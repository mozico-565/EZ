package com.ezshare.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezshare.core.*
import androidx.activity.compose.setContent

private val Purple=Color(0xffbaa3f6);private val Orange=Color(0xffffc568);private val Blue=Color(0xffbbd8ff);private val Pink=Color(0xfff3b4eb)
@Composable fun LegacyAlphaScreen(vm:EZViewModel=viewModel()){
    val controller=vm.controller;val state by vm.ui.collectAsStateWithLifecycle();var address by rememberSaveable{mutableStateOf("")};var tab by rememberSaveable{mutableIntStateOf(0)}
    val context=androidx.compose.ui.platform.LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(controller::pick)}
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(!granted)Toast.makeText(context,"يمكن متابعة النقل؛ قد يُخفي النظام الإشعار",Toast.LENGTH_LONG).show()}
    fun notifyPermission(){if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)notifications.launch(Manifest.permission.POST_NOTIFICATIONS)}
    fun openSend(){tab=1;controller.page("send")}
    Scaffold(containerColor=Color.White,contentWindowInsets=WindowInsets.safeDrawing,bottomBar={
        Surface(color=Color(0xff101017),shape=RoundedCornerShape(50.dp),modifier=Modifier.navigationBarsPadding().padding(horizontal=24.dp,vertical=12.dp)){
            Row(Modifier.fillMaxWidth().padding(6.dp),horizontalArrangement=Arrangement.SpaceAround){listOf("الرئيسية","الملفات","النتيجة","حول EZ").forEachIndexed{i,label->
                val active=tab==i
                Surface(onClick={tab=i;controller.page(if(i==1)"send" else if(i==2)"result" else "home")},color=if(active)Color.White else Color.Transparent,shape=RoundedCornerShape(40.dp)){
                    Column(Modifier.widthIn(min=60.dp).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(listOf(Icons.Outlined.Home,Icons.Outlined.Folder,Icons.Outlined.CheckCircle,Icons.Outlined.Info)[i],null,tint=if(active)Color.Black else Color.LightGray);Text(label,fontSize=10.sp,color=if(active)Color.Black else Color.LightGray)}
                }
            }}
        }
    }){padding->Column(Modifier.fillMaxSize().padding(padding)){
        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
            Image(painterResource(R.drawable.ez_logo),"شعار EZ",Modifier.width(56.dp).height(28.dp));Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text("أهلًا، خلّنا نشارك!",fontWeight=FontWeight.Bold);Text("Alpha 0.1 · شبكة Wi-Fi محلية",fontSize=10.sp,color=Color.Gray)}
            if(state.stage!="home")IconButton(onClick={tab=0;controller.page("home")}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"العودة للرئيسية")}
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if(state.error!=null)Surface(color=Color(0xffffeddf),shape=RoundedCornerShape(20.dp)){Text(state.error!!,Modifier.padding(16.dp),fontSize=12.sp)}
            if(state.busy&&state.progress==null) {LinearProgressIndicator(Modifier.fillMaxWidth());Text(state.status,fontSize=13.sp)}
            AnimatedContent(targetState=if(tab==3)"about" else state.stage,label="screen"){page->Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
                when(page){
                    "home"->{
                        Surface(color=Purple,shape=RoundedCornerShape(28.dp)){Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("ملفاتك توصل.\nبلمح البصر.",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("شارك ملفًا حقيقيًا عبر شبكتك المحلية.",fontSize=12.sp);Spacer(Modifier.height(12.dp));Button(onClick={openSend()},colors=ButtonDefaults.buttonColors(containerColor=Color.Black)){Text("ابدأ المشاركة")}};Image(painterResource(R.drawable.transfer_art),null,Modifier.width(120.dp).height(170.dp))}}
                        Text("نشارك؟",fontSize=22.sp,fontWeight=FontWeight.Bold)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Surface(onClick={openSend()},color=Orange,shape=RoundedCornerShape(28.dp),modifier=Modifier.weight(1f)){Column(Modifier.padding(20.dp).heightIn(min=155.dp),verticalArrangement=Arrangement.SpaceBetween){Icon(Icons.AutoMirrored.Outlined.Send,null);Text("إرسال",fontSize=30.sp);Text("اختر ملفًا من جهازك",fontSize=12.sp)}}
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){Surface(onClick={tab=0;controller.page("receive")},color=Blue,shape=RoundedCornerShape(28.dp)){Column(Modifier.fillMaxWidth().padding(20.dp)){Icon(Icons.Outlined.Download,null);Text("استقبال",fontSize=28.sp);Text("وافق، ثم استقبل",fontSize=12.sp)}};Surface(color=Pink,shape=RoundedCornerShape(24.dp)){Text("قارن الرمز على الهاتفين\nقبل الموافقة.",Modifier.fillMaxWidth().padding(14.dp),fontSize=12.sp)}}
                        }
                        Note("يتطلب هاتفين على شبكة Wi-Fi نفسها، أو نقطة اتصال أحد الهاتفين. لا يحتاج النقل إلى الإنترنت. Wi-Fi Direct والاستكمال لاحقًا.")
                        if(state.busy||state.progress!=null)OutlinedButton(onClick={tab=2;controller.page("result")},modifier=Modifier.fillMaxWidth()){Text("عرض حالة النقل")}
                    }
                    "send"->{
                        Text("اختر ما تريد مشاركته",fontSize=24.sp,fontWeight=FontWeight.Bold)
                        Button(onClick={picker.launch(arrayOf("*/*"))},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.AttachFile,null);Text(" اختيار ملف حقيقي")}
                        state.picked?.let{file->Surface(color=Color(0xfff4f0fc),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(file.metadata.name,fontWeight=FontWeight.Bold);Text(if(file.metadata.size<0)"الحجم غير معروف؛ سيُقاس أثناء النقل" else legacyHumanSize(file.metadata.size),fontSize=12.sp)}}}
                        Button(onClick={controller.startDiscovery()},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("البحث عن مستقبلين")}
                        Text(state.discoveryMessage.ifBlank{"فعّل الاستقبال على الهاتف الآخر."},fontSize=12.sp,color=Color.Gray)
                        state.peers.forEach{peer->OutlinedButton(onClick={notifyPermission();controller.send(peer.address)},enabled=state.picked!=null&&!state.busy,modifier=Modifier.fillMaxWidth()){Text("${peer.name}\n${peer.address}")}}
                        OutlinedTextField(value=address,onValueChange={address=it},label={Text("العنوان اليدوي IP:PORT")},placeholder={Text("192.168.1.8:34567")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!state.busy)
                        Button(onClick={notifyPermission();controller.send(address)},enabled=state.picked!=null&&!state.busy&&address.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("اتصال وطلب مشاركة الملف")}
                        if(state.busy)OutlinedButton(onClick={controller.cancel()},modifier=Modifier.fillMaxWidth()){Text("إلغاء الاتصال أو النقل")}
                        Note("اسم الجهاز وعنوانه لا يثبتان هويته. قارن رمز الأرقام الستة قبل الموافقة على الهاتفين.")
                    }
                    "receive"->{
                        Text("جاهز للاستقبال",fontSize=26.sp,fontWeight=FontWeight.Bold)
                        Text("جهازك: ${controller.identity}")
                        Button(onClick={notifyPermission();controller.receive()},enabled=!state.listening&&!state.busy,modifier=Modifier.fillMaxWidth()){Text("تفعيل الاستقبال")}
                        if(state.listening){Surface(color=Blue,shape=RoundedCornerShape(25.dp)){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("العنوان اليدوي للمُرسل",fontWeight=FontWeight.Bold);controller.localAddresses().forEach{ip->SelectionContainerCompat("$ip:${state.port}")};if(controller.localAddresses().isEmpty())Text("لا يظهر عنوان Wi-Fi. اتصل بشبكة محلية أولًا.");Text("ستُطلب موافقتك عند وصول ملف.",fontSize=12.sp)}}
                            OutlinedButton(onClick={controller.cancel()},modifier=Modifier.fillMaxWidth()){Text("إيقاف الاستقبال والنقل")}
                        }
                        Note("اختر مجلدًا محليًا فرعيًا مثل Documents/EZ. بعض إصدارات Android لا تسمح باختيار جذر Downloads. الملف الجزئي يحمل امتداد ezpart حتى ينجح التحقق؛ قد تبقى بقايا إذا رفض مزود التخزين حذفها.")
                    }
                    "result"->{
                        Text(state.status,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        state.progress?.let{p->ProgressCard(p,state.filename)}
                        if(state.busy)OutlinedButton(onClick={controller.cancel()},modifier=Modifier.fillMaxWidth()){Text("إلغاء النقل")}
                        state.result?.let{r->Surface(color=Color(0xffe9f4ef),shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(r.metadata.name,fontWeight=FontWeight.Bold);Text("${legacyHumanSize(r.bytes)} · ${"%.2f".format(r.elapsedNanos/1e9)} ثانية");Text("متوسط فعلي: ${"%.2f".format(if(r.elapsedNanos>0)r.bytes/(r.elapsedNanos/1e9)/1e6 else 0.0)} MB/s");Text("SHA-256: ${r.sha256}",fontSize=10.sp);Text("تمت الكتابة والتحقق. ${if(r.savedAt.isNotEmpty())"تم حفظ الملف في المجلد المحدد." else "وصل إيصال التحقق من المستقبل."}",fontSize=12.sp)}}}
                        if(state.result==null&&!state.busy)Note("لا توجد عملية ناجحة بعد. افتح إرسال أو استقبال للبدء؛ لا توجد أرقام أو ملفات وهمية.")
                        Button(onClick={tab=0;controller.page("home")},modifier=Modifier.fillMaxWidth()){Text("العودة للرئيسية")}
                    }
                    "about"->{Text("EZ Alpha 0.1",fontSize=28.sp,fontWeight=FontWeight.Bold);Note("تطبيق Android أصلي لنقل ملف واحد على شبكة محلية. تشفير AES-GCM مع ECDH مؤقت ورمز مقارنة يدوي. البروتوكول جديد ويحتاج مراجعة أمنية واختبار هاتفين؛ ليست نسخة إصدار نهائي.");Note("Wi-Fi Direct وQR وتعدد الملفات والاستكمال والثيم الداكن ليست منفذة في Alpha. نموذج التصميم السابق يحتفظ برؤيتها فقط.");Text("لا حسابات ولا خادم سحابي. لا تُجمع ملفاتك أو تُرفع إلى الإنترنت.");OutlinedButton(onClick={controller.cancel();controller.stopDiscovery()}){Text("إيقاف جميع عمليات الشبكة")}}
                }
            }}
            if(state.progress!=null&&state.stage!="result") {ProgressCard(state.progress!!,state.filename);TextButton(onClick={tab=2;controller.page("result")}){Text("عرض التفاصيل")}}
            Spacer(Modifier.height(16.dp))
        }
    }}
    state.request?.let{request->AlertDialog(onDismissRequest={controller.decide(false)},title={Text(if(request.incoming)"طلب استقبال ملف" else "تحقق من الهاتف الآخر")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("الجهاز: ${request.peer}");request.metadata?.let{Text(safeFilename(it.name));Text(if(it.size<0)"الحجم غير معروف · راقب مساحة التخزين" else legacyHumanSize(it.size))};Text("قارن الرمز نفسه على الهاتفين، ثم وافق فقط إذا كان متطابقًا.");Text(request.sas,fontSize=38.sp,fontWeight=FontWeight.Bold);Text("لا توافق اعتمادًا على الاسم أو IP وحدهما.",fontSize=12.sp)}},confirmButton={Button(onClick={controller.decide(true)}){Text(if(request.incoming)"الرمزان متطابقان · قبول الملف" else "الرمزان متطابقان · متابعة")}},dismissButton={TextButton(onClick={controller.decide(false)}){Text("رفض")}})}
}
@Composable private fun Note(text:String){Surface(color=Color(0xfff6f6f8),shape=RoundedCornerShape(22.dp)){Text(text,Modifier.fillMaxWidth().padding(16.dp),fontSize=12.sp,lineHeight=22.sp)}}
@Composable private fun SelectionContainerCompat(text:String){androidx.compose.foundation.text.selection.SelectionContainer{Text(text,fontSize=20.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun ProgressCard(p:Progress,name:String){Surface(color=Color(0xfff3edff),shape=RoundedCornerShape(26.dp)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(name.isNotBlank())Text(name,fontWeight=FontWeight.Bold);if(p.percent==null)LinearProgressIndicator(Modifier.fillMaxWidth())else LinearProgressIndicator(progress={((p.percent?:0.0)/100).toFloat()},modifier=Modifier.fillMaxWidth());Text("${legacyHumanSize(p.bytes)} / ${if(p.total<0)"غير معروف" else legacyHumanSize(p.total)}");Text("${p.percent?.let{"%.1f%%".format(it)}?:"حجم غير معروف"} · ${"%.2f".format(p.currentMBps)} MB/s");Text("المتوسط: ${"%.2f".format(p.averageMBps)} MB/s · المتبقي: ${p.etaSeconds?.let{"%.0f ث".format(it)}?:"غير معروف"}",fontSize=12.sp);Text("السرعة محسوبة من بايتات فعلية وزمن monotonic، وليست وعد أداء.",fontSize=10.sp)}}}
fun legacyHumanSize(bytes:Long):String=if(bytes>=1_000_000_000)"%.2f GB".format(bytes/1e9) else "%.2f MB".format(bytes/1e6)
