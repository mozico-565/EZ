package com.ezshare.app

import android.app.Application
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.ezshare.core.*
import com.ezshare.core.ProtocolException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class EZApplication : Application() { val transfers by lazy { TransferController(this) } }
data class PickedFile(val uri:Uri,val metadata:Metadata)
data class PairRequest(val peer:String,val sas:String,val metadata:Metadata?,val incoming:Boolean,val files:List<Metadata> = emptyList())
data class TransferUi(
    val stage:String="home", val status:String="جاهز", val picked:PickedFile?=null,val selected:List<PickedFile> = emptyList(),
    val filename:String="", val request:PairRequest?=null,val progress:Progress?=null,
    val result:Result?=null,val error:String?=null,val listening:Boolean=false,val port:Int=0,
    val incoming:Boolean=false,val peer:String="",val operation:String="",val files:List<Metadata> = emptyList(),val completedResults:List<Result> = emptyList(),val fileIndex:Int=0,val phase:String="",val timings:List<PhaseTiming> = emptyList(),val pairingNonce:String="",val pairingExpiry:Long=0,val peers:List<Peer> = emptyList(),val discoveryMessage:String="", val busy:Boolean=false
)
class TransferController(private val app:Application) {
    private val store=EzLocalStore(app)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val _ui=MutableStateFlow(TransferUi());val ui=_ui.asStateFlow()
    private val inUse=AtomicBoolean(false)
    @Volatile private var cancellationRequested=false
    @Volatile private var connection:Connection?=null
    @Volatile private var server:ServerSocket?=null
    @Volatile private var approval:CompletableDeferred<Boolean>?=null
    @Volatile private var activeStream:Closeable?=null
    @Volatile private var discoveryStart=0L
    @Volatile private var discoveryLatency=0L
    private val discovery=Discovery(app,{ peers->if(peers.isNotEmpty()&&discoveryStart>0&&discoveryLatency==0L)discoveryLatency=System.nanoTime()-discoveryStart;_ui.update { it.copy(peers=peers) } },{ message->_ui.update { it.copy(discoveryMessage=message) } })
    val identity:String get()=store.read().name.ifBlank{Build.MODEL}.take(48)
    fun status(s:String){_ui.update{it.copy(status=s)}}
    fun page(s:String){_ui.update{it.copy(stage=s)}}
    fun pick(uri:Uri){try{
        app.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }catch(_:SecurityException){ /* Current provider grant can still be used for this process. */ }
        try {val cursor=app.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)
            val meta=cursor?.use { if(!it.moveToFirst())throw IOException("تعذّر قراءة الملف")
                Metadata(it.getString(0)?:"file",app.contentResolver.getType(uri)?:"application/octet-stream",if(it.isNull(1))-1 else it.getLong(1))
            }?:throw IOException("تعذّر قراءة بيانات الملف")
            meta.validate();_ui.update{it.copy(picked=PickedFile(uri,meta),selected=listOf(PickedFile(uri,meta)),error=null,stage="send")}
        }catch(e:Exception){fail(e)}
    }
    fun select(files:List<PickedFile>){if(inUse.get())return
        try{require(files.size<=256){"يمكن تحديد 256 ملفًا كحد أقصى"};files.forEach{it.metadata.validate()};_ui.update{it.copy(selected=files.distinctBy{f->f.uri},picked=files.firstOrNull(),error=null)}}catch(e:Exception){_ui.update{it.copy(error="تعذّر الاختيار: الحد 256 ملفًا و16GiB للملف الواحد. ${e.message.orEmpty()}",status="تعذّر اختيار الملفات")}}
    }
    fun scan(qr:PairingQr){
        if(inUse.get())return
        try{PairingQr.decode(qr.encode())}catch(e:Exception){_ui.update{it.copy(error="رمز QR غير صالح أو منتهي؛ أعد تفعيل الاستقبال وامسح الرمز الجديد.",status="تعذّر الاقتران")};return}
        send(qr.address,qr.nonce)
    }
    @Synchronized fun startDiscovery(){
        if(inUse.get())return
        // Choosing files/discovery switches an idle receiver to sender mode.
        stopReceive();discoveryStart=System.nanoTime();discoveryLatency=0;discovery.discover()
    }
    fun stopDiscovery(){discovery.stopDiscovery()}
    fun decide(accepted:Boolean){approval?.complete(accepted)}
    private fun ask(peer:String,sas:String,m:Metadata?,incoming:Boolean,files:List<Metadata> = emptyList()):Boolean=runBlocking {
        val current=connection?:throw IOException("Connection closed before approval")
        if(current.cancelled.get())throw IOException("Peer disconnected before approval")
        val decision=CompletableDeferred<Boolean>();approval=decision
        if(current.cancelled.get())decision.completeExceptionally(IOException("Peer disconnected before approval"))
        _ui.update{it.copy(filename=m?.name?:it.picked?.metadata?.name.orEmpty(),request=PairRequest(peer,sas,m,incoming,files),files=files,peer=peer,incoming=incoming,status="قارن الرمز على الهاتفين ثم وافق",stage=if(incoming)"receive" else "send")}
        try {withTimeout(115_000){decision.await()}}finally{approval=null;_ui.update{it.copy(request=null)}}
    }
    @Synchronized fun receive(){
        if(inUse.get()||server!=null)return
        cancellationRequested=false
        val listener=try{ServerSocket(0)}catch(e:Exception){fail(e);return}
        server=listener
        val nonce=UUID.randomUUID().toString().replace("-","");val expiry=System.currentTimeMillis()/1000+600
        try{runForeground()}catch(e:Exception){server=null;listener.close();fail(e);return}
        stopDiscovery()
        _ui.update{it.copy(pairingNonce=nonce,pairingExpiry=expiry,listening=true,port=listener.localPort,status="بانتظار طلب · قارن الرمز قبل القبول",error=null,stage="receive",result=null)}
        discovery.advertise(identity,listener.localPort)
        android.util.Log.i("EZSession","listen port=${listener.localPort}")
        scope.launch {try{
            while(!listener.isClosed){val socket=listener.accept()
                synchronized(this@TransferController){
                    // A detached listener must never claim a new transfer after restart.
                    if(server!==listener||!inUse.compareAndSet(false,true)){socket.close();return@synchronized}
                    val c=Connection(socket);connection=c
                    _ui.update{it.copy(operation=UUID.randomUUID().toString(),completedResults=emptyList(),timings=emptyList(),files=emptyList(),filename="",fileIndex=0,busy=true,progress=null,result=null,error=null,incoming=true,peer="")}
                    scope.launch {
                        try{if(cancellationRequested)throw IOException("Cancelled")
                            val result=BatchEngine.receive(c,identity,if(System.currentTimeMillis()/1000<expiry)nonce else "",{m,peer,sas->
                                if(m.pairingNonce.isNotBlank()&&System.currentTimeMillis()/1000>=expiry)throw ProtocolException("QR session expired")
                                ask(peer,sas,m.files.firstOrNull(),true,m.files)
                            },{m->MediaStorePending(app,m){activeStream=it}},::batchProgress,::completed)
                            finishBatch(result)
                        }catch(e:Exception){fail(e)}finally{synchronized(this@TransferController){
                            runCatching{activeStream?.close()};activeStream=null;c.close();if(connection===c)connection=null
                            _ui.update{it.copy(busy=false)};inUse.set(false)
                            if(server==null)stopForeground()
                        }}
                    }
                }
            }
        }catch(e:Exception){synchronized(this@TransferController){if(server===listener&&!listener.isClosed)fail(e)}}finally{
            synchronized(this@TransferController){
                // Old accept-loop cleanup cannot clear the replacement socket/QR/NSD.
                if(server===listener){server=null;listener.close();discovery.stopAdvertising();_ui.update{it.copy(listening=false,port=0,pairingNonce="",pairingExpiry=0)};if(!inUse.get())stopForeground()}
            }
        }}
    }
    @Synchronized fun send(address:String,nonce:String=""){val files=_ui.value.selected.ifEmpty{listOfNotNull(_ui.value.picked)};if(files.isEmpty())return
        if(!inUse.compareAndSet(false,true))return
        stopReceive();stopDiscovery()
        cancellationRequested=false;runForeground();val started=System.nanoTime()
        _ui.update{it.copy(operation=UUID.randomUUID().toString(),completedResults=emptyList(),timings=emptyList(),files=files.map{f->f.metadata},fileIndex=0,busy=true,error=null,result=null,progress=null,incoming=false,peer=address,filename=files.first().metadata.name,status="جارٍ الاتصال…",stage="transfer")}
        scope.launch {try{
            val (host,port)=parseLocalAddress(address);val socket=Socket();val c=Connection(socket);connection=c
            if(cancellationRequested)throw IOException("Cancelled");socket.connect(InetSocketAddress(host,port),10_000);c.touch();val connectNanos=System.nanoTime()-started
            val result=BatchEngine.send(c,identity,Manifest(files.map{it.metadata},pairingNonce=nonce),files.map{file->{(app.contentResolver.openInputStream(file.uri)?:throw IOException("لا يمكن فتح الملف")).also{activeStream=it}}},
                {m,peer,sas->ask(peer,sas,m.files.firstOrNull(),false,m.files)},::batchProgress,::completed,onPairingFailure={error->approval?.completeExceptionally(error)})
            finishBatch(result.copy(timings=(if(discoveryLatency>0)listOf(PhaseTiming("discovery_first_peer",-1,discoveryLatency)) else emptyList())+PhaseTiming("connection",-1,connectNanos)+result.timings))
        }catch(e:Exception){fail(e)}finally{synchronized(this@TransferController){runCatching{activeStream?.close()};activeStream=null;connection?.close();connection=null;_ui.update{it.copy(busy=false)};inUse.set(false);if(server==null)stopForeground()}}}
    }
    private fun batchProgress(p:BatchProgress){_ui.update{it.copy(progress=p.overall,fileIndex=p.index,phase=p.stage,filename=p.name,status=when(p.stage){"verifying"->"جارٍ التحقق من الملف المحفوظ…";"preparing"->"جارٍ تجهيز الملف…";else->"جارٍ النقل الفعلي…"},stage="transfer")}}
    private fun completed(r:Result){val s=_ui.value;store.addHistory(HistoryEntry(id=s.operation+":"+s.completedResults.size+":completed",name=if(s.incoming)receivedName(app,r.savedAt,r.metadata.name) else r.metadata.name,bytes=r.bytes,total=r.metadata.size,duration=r.elapsedNanos/1e9,sha=r.sha256,peer=s.peer,incoming=s.incoming,status="completed",savedAt=r.savedAt));_ui.update{it.copy(completedResults=it.completedResults+r)}}
    private fun recordFailure(){val s=_ui.value;if(!s.busy||s.filename.isBlank())return;val completedBytes=s.completedResults.sumOf{it.bytes};val completedTime=s.completedResults.sumOf{it.elapsedNanos};store.addHistory(HistoryEntry(id=s.operation+":"+s.fileIndex+":failed",name=s.filename,bytes=((s.progress?.bytes?:0)-completedBytes).coerceAtLeast(0),total=s.files.getOrNull(s.fileIndex)?.size?:-1,duration=((s.progress?.elapsedNanos?:0)-completedTime).coerceAtLeast(0)/1e9,peer=s.peer,incoming=s.incoming,status=if(cancellationRequested)"cancelled" else "failed"))}

    private fun finishBatch(r:BatchResult){_ui.update{it.copy(result=r.files.lastOrNull(),timings=r.timings,progress=it.progress?.copy(currentMBps=0.0),status="اكتمل النقل والتحقق من جميع الملفات",request=null,error=null,stage="result")}}
    fun cancel(){cancellationRequested=true;approval?.complete(false);runCatching{activeStream?.close()};connection?.close();stopReceive();_ui.update{it.copy(status="تم الإلغاء",error=if(it.busy)"أُلغي النقل؛ لا يُعد الملف الجزئي ناجحًا." else null,request=null)} }
    @Synchronized fun stopReceive(){val old=server;server=null
        runCatching{old?.close()};discovery.stopAdvertising()
        _ui.update{it.copy(listening=false,port=0,pairingNonce="",pairingExpiry=0)}
        if(!inUse.get())stopForeground()
    }
    private fun progress(p:Progress){_ui.update{it.copy(progress=p,status="جارٍ نقل الملف الفعلي…",stage="transfer")}}
    private fun finish(result:Result){_ui.update{it.copy(progress=it.progress?.copy(total=result.bytes,currentMBps=0.0),result=result,status="اكتمل النقل والتحقق من SHA-256",request=null,error=null,stage="result")}}
    private fun fail(e:Exception){android.util.Log.w("EZSession","operation failed: ${e.javaClass.simpleName}");recordFailure();if(cancellationRequested){_ui.update{it.copy(error="أُلغي النقل؛ لا يُعد الملف الجزئي ناجحًا.",status="تم الإلغاء",request=null)};return};_ui.update{it.copy(error=when(e){is SocketTimeoutException->"انتهت مهلة الاتصال. قرّب الهاتفين وافحص الشبكة ثم أعد المحاولة.";is ProtocolException->if(e.message?.contains("Unsupported protocol")==true)"إصدار غير متوافق. حدّث EZ على الهاتفين إلى الإصدار نفسه." else "تعذّر اعتماد النقل: ${e.message}";is TimeoutCancellationException->"انتهت مهلة موافقة المستخدم.";else->if(e.message?.contains("Incomplete file cleanup failed")==true)"تعذّر تنظيف الإدخال الجزئي؛ يظل قيد الكتابة ولا يُعرض كملف مكتمل. لم يُعلن نجاحه." else "تعذّرت العملية: ${e.message?:e.javaClass.simpleName}. لم يُعلن نجاح أي ملف جزئي."},status="توقفت العملية",request=null)}}
    private fun runForeground(){TransferService.start(app)}
    private fun stopForeground(){app.stopService(Intent(app,TransferService::class.java))}
    fun serviceStopped(){if(inUse.get()||server!=null)cancel()}
    fun localAddresses():List<String> = runCatching { NetworkInterface.getNetworkInterfaces().toList().flatMap{it.inetAddresses.toList()}.filter{it is Inet4Address&&!it.isLoopbackAddress&&it.isSiteLocalAddress}.map{it.hostAddress.orEmpty()} }.getOrDefault(emptyList())
}
fun parseLocalAddress(value:String):Pair<InetAddress,Int>{val parts=value.trim().split(':');require(parts.size==2){"اكتب عنوانًا محليًا مثل 192.168.1.8:34567"}
    require(parts[0].matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))){"عنوان IPv4 محلي مطلوب"}
    require(parts[0].split('.').all{it.toInt() in 0..255}){"عنوان غير صالح"}
    val host=InetAddress.getByName(parts[0]);require(host.isSiteLocalAddress||host.isLinkLocalAddress){"استخدم عنوان الهاتف على الشبكة المحلية"};val port=parts[1].toIntOrNull()?:0;require(port in 1024..65535){"منفذ غير صالح"};return host to port
}
