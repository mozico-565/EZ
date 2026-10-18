package com.ezshare.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ezshare.core.PairingQr
import com.google.zxing.BarcodeFormat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

fun pairingBitmap(payload:String):Bitmap{val matrix=QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,600,600);return Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888).apply{setPixels(IntArray(600*600){i->if(matrix[i%600,i/600])android.graphics.Color.BLACK else android.graphics.Color.WHITE},0,600,0,0,600,600)}}
@Composable fun ReceiverQr(payload:String){val bitmap=remember(payload){pairingBitmap(payload)};Image(bitmap.asImageBitmap(),"رمز اقتران محلي لجلسة الاستقبال الحالية",Modifier.sizeIn(maxWidth=220.dp,maxHeight=220.dp).aspectRatio(1f).background(Color.White).padding(8.dp));Text("امسح الرمز ثم قارن رمز التحقق على الهاتفين.",fontSize=11.sp)}
@Composable fun PairingScanner(onConnect:(PairingQr)->Unit){
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
    var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    var message by remember{mutableStateOf("وجّه الكاميرا إلى رمز EZ على هاتف الاستقبال")};var scanning by remember{mutableStateOf(true)}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it;message=if(it)"وجّه الكاميرا إلى رمز EZ" else "إذن الكاميرا غير متاح. يمكنك اختيار جهاز قريب أو إدخال عنوانه."}
    Text(message,fontSize=12.sp,color=LocalEzPalette.current.muted)
    if(!granted){EzButton("السماح بالكاميرا",{request.launch(Manifest.permission.CAMERA)},Modifier.fillMaxWidth().padding(top=20.dp));return}
    val view=remember{PreviewView(context).apply{implementationMode=PreviewView.ImplementationMode.COMPATIBLE}}
    val latestConnect by rememberUpdatedState(onConnect)
    AndroidView(factory={view},modifier=Modifier.fillMaxWidth().height(300.dp).padding(top=20.dp))
    DisposableEffect(granted,lifecycle,scanning){
        val executor=Executors.newSingleThreadExecutor();val closed=AtomicBoolean(false);val delivered=AtomicBoolean(false)
        val reader=MultiFormatReader().apply{setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true))}
        val future=ProcessCameraProvider.getInstance(context)
        val analysis=ImageAnalysis.Builder().setTargetResolution(android.util.Size(1280,720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();var provider:ProcessCameraProvider?=null
        val main=ContextCompat.getMainExecutor(context);var previousFrame=0L;var luminance=ByteArray(0)
        future.addListener({if(!closed.get())runCatching{val p=future.get();provider=p;val preview=Preview.Builder().build();preview.setSurfaceProvider(view.surfaceProvider)
            analysis.setAnalyzer(executor){proxy->try{
                val time=System.nanoTime()
                if(!closed.get()&&scanning&&!delivered.get()&&time-previousFrame>200_000_000){
                    previousFrame=time
                    val width=proxy.width;val height=proxy.height
                    if(width.toLong()*height<=4_000_000){
                        if(luminance.size!=width*height)luminance=ByteArray(width*height)
                        val plane=proxy.planes[0];copyLuminance(plane.buffer,width,height,plane.rowStride,plane.pixelStride,luminance)
                        val result=reader.decodeWithState(BinaryBitmap(HybridBinarizer(PlanarYUVLuminanceSource(luminance,width,height,0,0,width,height,false)))).text
                        runCatching{PairingQr.decode(result)}.onSuccess{qr->if(delivered.compareAndSet(false,true))main.execute{if(!closed.get()){scanning=false;latestConnect(qr)}}}.onFailure{main.execute{if(!closed.get())message="رمز غير صالح أو منتهي. امسح رمز EZ حديثًا."}}
                    }
                }
            }catch(_:NotFoundException){}catch(_:Exception){main.execute{if(!closed.get())message="تعذّر قراءة الرمز؛ حاول مجددًا."}}finally{reader.reset();proxy.close()}}
            p.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
        }.onFailure{message="تعذّر فتح الكاميرا. استخدم الاتصال اليدوي."}},main)
        onDispose{closed.set(true);analysis.clearAnalyzer();provider?.unbindAll();executor.shutdown()}

    }
    EzButton("إعادة المسح",{scanning=true;message="وجّه الكاميرا إلى رمز EZ"},Modifier.fillMaxWidth().padding(top=16.dp),outline=true)
}

/** Respect row/pixel strides; never assume tightly packed camera Y planes. */
fun copyLuminance(buffer:java.nio.ByteBuffer,width:Int,height:Int,rowStride:Int,pixelStride:Int,out:ByteArray){
    require(width>0&&height>0&&width.toLong()*height<=4_000_000&&out.size==width*height)
    require(rowStride>0&&pixelStride>0);val start=buffer.position()
    for(y in 0 until height)for(x in 0 until width)out[y*width+x]=buffer.get(start+y*rowStride+x*pixelStride)
}
