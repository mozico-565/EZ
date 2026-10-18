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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

fun pairingBitmap(payload:String):Bitmap{val matrix=QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,600,600);return Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888).apply{setPixels(IntArray(600*600){i->if(matrix[i%600,i/600])android.graphics.Color.BLACK else android.graphics.Color.WHITE},0,600,0,0,600,600)}}
@Composable fun ReceiverQr(payload:String){val bitmap=remember(payload){pairingBitmap(payload)};Image(bitmap.asImageBitmap(),"رمز اقتران محلي لجلسة الاستقبال الحالية",Modifier.sizeIn(maxWidth=250.dp,maxHeight=250.dp).aspectRatio(1f).background(Color.White).padding(8.dp));Text("امسح الرمز ثم قارن رمز التحقق على الهاتفين.",fontSize=11.sp)}
@androidx.annotation.OptIn(ExperimentalGetImage::class)
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
        val executor=Executors.newSingleThreadExecutor();val closed=AtomicBoolean(false);val working=AtomicBoolean(false);val delivered=AtomicBoolean(false)
        val scanner=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build());val future=ProcessCameraProvider.getInstance(context)
        val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();var provider:ProcessCameraProvider?=null
        future.addListener({if(!closed.get())runCatching{val p=future.get();provider=p;val preview=Preview.Builder().build();preview.setSurfaceProvider(view.surfaceProvider)
            analysis.setAnalyzer(executor){proxy->
                val media=proxy.image
                if(closed.get()||!scanning||media==null||!working.compareAndSet(false,true)){proxy.close()}else{
                    scanner.process(InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)).addOnSuccessListener{codes->
                        if(!closed.get()&&!delivered.get()){codes.firstOrNull()?.rawValue?.let{raw->runCatching{PairingQr.decode(raw)}.onSuccess{qr->if(delivered.compareAndSet(false,true)){scanning=false;latestConnect(qr)}}.onFailure{message="رمز غير صالح أو منتهي. امسح رمز EZ حديثًا على الشبكة المحلية."}}}
                    }.addOnFailureListener{if(!closed.get())message="تعذّر قراءة الرمز؛ حاول مجددًا."}.addOnCompleteListener{working.set(false);proxy.close()}
                }
            };p.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
        }.onFailure{message="تعذّر فتح الكاميرا. استخدم الاتصال اليدوي."}},ContextCompat.getMainExecutor(context))
        onDispose{closed.set(true);analysis.clearAnalyzer();provider?.unbind(analysis);provider?.unbindAll();scanner.close();executor.shutdown()}
    }
    EzButton("إعادة المسح",{scanning=true;message="وجّه الكاميرا إلى رمز EZ"},Modifier.fillMaxWidth().padding(top=16.dp),outline=true)
}
