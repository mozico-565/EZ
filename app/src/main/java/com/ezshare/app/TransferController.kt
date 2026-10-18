package com.ezshare.app

import android.app.Application
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.ezshare.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.security.DigestOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class EZApplication : Application() { val transfers by lazy { TransferController(this) } }
data class PickedFile(val uri:Uri,val metadata:Metadata)
data class PairRequest(val peer:String,val sas:String,val metadata:Metadata?,val incoming:Boolean,val files:List<Metadata> = emptyList())
data class TransferUi(
    val stage:String="home", val status:String="جاهز", val picked:PickedFile?=null,val selected:List<PickedFile> = emptyList(),
    val filename:String="", val folder:Uri?=null,val request:PairRequest?=null,val progress:Progress?=null,
    val result:Result?=null,val error:String?=null,val listening:Boolean=false,val port:Int=0,
    val incoming:Boolean=false,val peer:String="",val operation:String="",val files:List<Metadata> = emptyList(),val completedResults:List<Result> = emptyList(),val fileIndex:Int=0,val phase:String="",val timings:List<PhaseTiming> = emptyList(),val pairingNonce:String="",val pairingExpiry:Long=0,val peers:List<Peer> = emptyList(),val discoveryMessage:String="", val busy:Boolean=false
)
class TransferController(private val app:Application) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val _ui=MutableStateFlow(TransferUi());val ui=_ui.asStateFlow()
    private val inUse=AtomicBoolean(false)
    private val receiving=AtomicBoolean(false)
    @Volatile private var cancellationRequested=false
    @Volatile private var wantsReceive=false
    @Volatile private var connection:Connection?=null
    @Volatile private var server:ServerSocket?=null
    @Volatile private var approval:CompletableDeferred<Boolean>?=null
    @Volatile private var activeStream:Closeable?=null
    private val discovery=Discovery(app,{ peers->_ui.update { it.copy(peers=peers) } },{ message->_ui.update { it.copy(discoveryMessage=message) } })
    val identity:String get()=EzLocalStore(app).read().name.ifBlank{Build.MODEL}.take(48)
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
        try{require(files.size<=256){"يمكن تحديد 256 ملفًا كحد أقصى"};files.forEach{it.metadata.validate()};_ui.update{it.copy(selected=files.distinctBy{f->f.uri},picked=files.firstOrNull(),error=null)}}catch(e:Exception){fail(e)}
    }
    fun scan(qr:PairingQr){if(System.currentTimeMillis()/1000>=qr.expires){status("رمز QR منتهي؛ أعد تفعيل الاستقبال");return};send(qr.address,qr.nonce)}
    fun folder(uri:Uri){try{
        app.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        _ui.update{it.copy(folder=uri,error=null)}
    }catch(e:Exception){fail(e)}}
    fun startDiscovery(){discovery.discover()}
    fun stopDiscovery(){discovery.stopDiscovery()}
    fun decide(accepted:Boolean){approval?.complete(accepted)}
    private fun ask(peer:String,sas:String,m:Metadata?,incoming:Boolean,files:List<Metadata> = emptyList()):Boolean=runBlocking {
        val decision=CompletableDeferred<Boolean>();approval=decision
        _ui.update{it.copy(filename=m?.name?:it.picked?.metadata?.name.orEmpty(),request=PairRequest(peer,sas,m,incoming,files),files=files,peer=peer,incoming=incoming,status="قارن الرمز على الهاتفين ثم وافق",stage=if(incoming)"receive" else "send")}
        try {withTimeout(115_000){decision.await()}}finally{approval=null;_ui.update{it.copy(request=null)}}
    }
    fun receive(){if(_ui.value.folder==null){status("اختر مجلد الحفظ أولًا داخل المستندات");return}
        if(inUse.get()||!receiving.compareAndSet(false,true))return
        cancellationRequested=false;wantsReceive=true
        runForeground()
        scope.launch {try{
            val listener=synchronized(this@TransferController){if(!wantsReceive)null else ServerSocket(0).also{server=it}}?:return@launch;discovery.advertise(identity,listener.localPort)
            val nonce=UUID.randomUUID().toString().replace("-","");val expiry=System.currentTimeMillis()/1000+600
            _ui.update{it.copy(pairingNonce=nonce,pairingExpiry=expiry,listening=true,port=listener.localPort,status="بانتظار طلب · قارن الرمز قبل القبول",error=null,stage="receive",result=null)}
            while(!listener.isClosed){val socket=listener.accept();if(!inUse.compareAndSet(false,true)){socket.close();continue}
                scope.launch { val c=Connection(socket);connection=c
                    try{if(cancellationRequested)throw IOException("Cancelled");_ui.update{it.copy(operation=UUID.randomUUID().toString(),completedResults=emptyList(),timings=emptyList(),files=emptyList(),fileIndex=0,busy=true,progress=null,result=null,error=null,incoming=true,peer="")}
                        val result=BatchEngine.receive(c,identity,if(System.currentTimeMillis()/1000<expiry)nonce else "",{m,peer,sas->
                            if(m.pairingNonce.isNotBlank()&&System.currentTimeMillis()/1000>=expiry)throw ProtocolException("QR session expired")
                            ask(peer,sas,m.files.firstOrNull(),true,m.files)
                        },{m->SafPending(app,_ui.value.folder?:throw IOException("اختر مجلدًا"),m){activeStream=it}},::batchProgress,::completed)
                        finishBatch(result)
                    }catch(e:Exception){fail(e)}finally{activeStream=null;c.close();connection=null;inUse.set(false);_ui.update{it.copy(busy=false)}}
                }
            }
        }catch(e:Exception){if(server!=null)fail(e)}finally{server=null;receiving.set(false);discovery.stopAdvertising();_ui.update{it.copy(listening=false,port=0)};if(!inUse.get())stopForeground()}}
    }
    fun send(address:String,nonce:String=""){val files=_ui.value.selected.ifEmpty{listOfNotNull(_ui.value.picked)};if(files.isEmpty())return
        if(server!=null){status("أوقف وضع الاستقبال أولًا");return};if(!inUse.compareAndSet(false,true))return
        cancellationRequested=false;runForeground();val started=System.nanoTime()
        _ui.update{it.copy(operation=UUID.randomUUID().toString(),completedResults=emptyList(),timings=emptyList(),files=files.map{f->f.metadata},fileIndex=0,busy=true,error=null,result=null,progress=null,incoming=false,peer=address,filename=files.first().metadata.name,status="جارٍ الاتصال…",stage="transfer")}
        scope.launch {try{
            val (host,port)=parseLocalAddress(address);val socket=Socket();val c=Connection(socket);connection=c
            if(cancellationRequested)throw IOException("Cancelled");socket.connect(InetSocketAddress(host,port),10_000);c.touch();val connectNanos=System.nanoTime()-started
            val result=BatchEngine.send(c,identity,Manifest(files.map{it.metadata},pairingNonce=nonce),files.map{file->{(app.contentResolver.openInputStream(file.uri)?:throw IOException("لا يمكن فتح الملف")).also{activeStream=it}}},
                {m,peer,sas->ask(peer,sas,m.files.firstOrNull(),false,m.files)},::batchProgress,::completed)
            finishBatch(result.copy(timings=listOf(PhaseTiming("connection",-1,connectNanos))+result.timings))
        }catch(e:Exception){fail(e)}finally{runCatching{activeStream?.close()};activeStream=null;connection?.close();connection=null;inUse.set(false);_ui.update{it.copy(busy=false)};stopForeground()}}
    }
    private fun batchProgress(p:BatchProgress){_ui.update{it.copy(progress=p.overall,fileIndex=p.index,phase=p.stage,filename=p.name,status=if(p.stage=="verifying")"جارٍ التحقق من الملف المحفوظ…" else "جارٍ النقل الفعلي…",stage="transfer")}}
    private fun completed(r:Result){_ui.update{it.copy(completedResults=it.completedResults+r)}}
    private fun finishBatch(r:BatchResult){_ui.update{it.copy(result=r.files.lastOrNull(),timings=r.timings,progress=it.progress?.copy(currentMBps=0.0),status="اكتمل النقل والتحقق من جميع الملفات",request=null,error=null,stage="result")}}
    fun cancel(){cancellationRequested=true;approval?.complete(false);runCatching{activeStream?.close()};connection?.close();stopReceive();_ui.update{it.copy(status="تم الإلغاء",error=if(it.busy)"أُلغي النقل؛ لا يُعد الملف الجزئي ناجحًا." else null,request=null)} }
    fun stopReceive(){val old=synchronized(this){wantsReceive=false;server.also{server=null}};runCatching{old?.close()};discovery.stopAdvertising();_ui.update{it.copy(listening=false,port=0)};if(!inUse.get())stopForeground()}
    private fun progress(p:Progress){_ui.update{it.copy(progress=p,status="جارٍ نقل الملف الفعلي…",stage="transfer")}}
    private fun finish(result:Result){_ui.update{it.copy(progress=it.progress?.copy(total=result.bytes,currentMBps=0.0),result=result,status="اكتمل النقل والتحقق من SHA-256",request=null,error=null,stage="result")}}
    private fun fail(e:Exception){if(cancellationRequested){_ui.update{it.copy(error="أُلغي النقل؛ لا يُعد الملف الجزئي ناجحًا.",status="تم الإلغاء",request=null)};return};_ui.update{it.copy(error=when(e){is SocketTimeoutException->"انتهت مهلة الاتصال. قرّب الهاتفين وافحص الشبكة ثم أعد المحاولة.";is ProtocolException->if(e.message?.contains("Unsupported protocol")==true)"إصدار غير متوافق. حدّث EZ على الهاتفين إلى الإصدار نفسه." else "تعذّر اعتماد النقل: ${e.message}";is TimeoutCancellationException->"انتهت مهلة موافقة المستخدم.";else->"تعذّرت العملية: ${e.message?:e.javaClass.simpleName}. لم يُعلن نجاح أي ملف جزئي."},status="توقفت العملية",request=null)}}
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
class SafPending(private val app:Application,tree:Uri,m:Metadata,private val onOpen:(Closeable)->Unit):PendingFile {
    private val resolver=app.contentResolver
    private val parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
    private var uri:Uri
    private val storageDigest=MessageDigest.getInstance("SHA-256")
    private val finalName=uniqueFilename(m.name){name->exists(tree,name)}
    override val output:OutputStream
    init {
        uri=DocumentsContract.createDocument(resolver,parent,m.mime,"${finalName}.ezpart-${UUID.randomUUID().toString().take(8)}")?:throw IOException("المجلد لا يدعم إنشاء الملفات")
        try{output=DigestOutputStream(resolver.openOutputStream(uri,"w")?:throw IOException("تعذّرت الكتابة"),storageDigest);onOpen(output)}catch(e:Exception){runCatching{DocumentsContract.deleteDocument(resolver,uri)};throw e}
    }
    private fun exists(tree:Uri,name:String):Boolean {
        val children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
        return resolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use { c->var found=false;while(c.moveToNext())if(c.getString(0)==name)found=true;found }?:throw IOException("تعذّر فحص أسماء الملفات")
    }
    override fun commit():String {
        val expected=storageDigest.digest();val actual=MessageDigest.getInstance("SHA-256")
        (resolver.openInputStream(uri)?:throw IOException("تعذّر إعادة قراءة الملف للتحقق")).use{input->onOpen(input);val buffer=ByteArray(64*1024);while(true){val n=input.read(buffer);if(n<0)break;if(n>0)actual.update(buffer,0,n)}}
        if(!MessageDigest.isEqual(expected,actual.digest()))throw IOException("فشل التحقق من الملف المحفوظ")
        val renamed=DocumentsContract.renameDocument(resolver,uri,finalName)?:throw IOException("المجلد لا يدعم نشر الملف بعد التحقق؛ اختر مجلدًا محليًا آخر")
        uri=renamed;return renamed.toString()
    }
    override fun abort(){if(!DocumentsContract.deleteDocument(resolver,uri))throw IOException("تعذّر حذف الملف الجزئي")}
}
