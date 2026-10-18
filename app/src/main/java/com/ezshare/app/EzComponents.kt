package com.ezshare.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import android.os.Environment
import android.os.StatFs
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class Category(val label:String,val icon:Int,val tint:Color)
val categories=listOf(Category("الصور",R.drawable.ic_images,EzTokens.CategoryPink),Category("الفيديوهات",R.drawable.ic_video_camera,EzTokens.CategoryPurple),Category("التطبيقات",R.drawable.ic_android_logo,EzTokens.CategoryBlue),Category("المستندات",R.drawable.ic_file_text,EzTokens.Peach))
@Composable fun HomeContent(onFiles:(String)->Unit,onReceive:()->Unit,onPrivacy:()->Unit,latest:HistoryEntry?,onHistory:()->Unit){
    val p=LocalEzPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min=230.dp).clip(RoundedCornerShape(28.dp)).background(p.lavender)){
        val compact=maxWidth<336.dp
        Image(painterResource(R.drawable.transfer_art),"هاتفان يتبادلان الصور والملفات لاسلكيًا",Modifier.align(Alignment.BottomEnd).offset(x=if(compact)20.dp else 17.dp,y=6.dp).width(if(compact)145.dp else 175.dp).height(205.dp).alpha(if(LocalDensity.current.fontScale>1.1f).15f else 1f),contentScale=ContentScale.Fit)
        Column(Modifier.fillMaxWidth(.68f).padding(20.dp)){
            EzTag("بدون إنترنت",R.drawable.ic_wifi_high)
            Text("ملفاتك توصل.\nبلمح البصر.",Modifier.fillMaxWidth().padding(top=10.dp,bottom=4.dp),fontSize=if(compact)25.sp else 28.sp,lineHeight=if(compact)36.sp else 40.sp,fontWeight=FontWeight.ExtraBold,color=EzTokens.Ink,letterSpacing=(-1.2).sp,softWrap=LocalDensity.current.fontScale>1.1f,maxLines=if(LocalDensity.current.fontScale>1.1f)6 else 2)
            Text("قرّب جهازك، وشارك اللي تحب.",fontSize=10.sp,lineHeight=16.sp,color=EzTokens.Ink,maxLines=1)
            EzButton("ابدأ المشاركة",{onFiles("الكل")},Modifier.padding(top=13.dp),icon=R.drawable.ic_arrow_up_left)
        }
    }
    EzSection("نشارك؟",caption="خطوة واحدة وتبدأ")
    // Keep the original asymmetry: 1.08 orange, .92 blue/pink; RTL places Send at the right.
    Row(Modifier.fillMaxWidth().height(223.dp*LocalDensity.current.fontScale.coerceAtLeast(1f)),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Box(Modifier.weight(1.08f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(p.orange).ezPress({onFiles("الكل")})){
            Column(Modifier.fillMaxSize().padding(16.dp)){
                EzTag("من جهازك")
                Spacer(Modifier.height(54.dp));Text("إرسال",fontSize=27.sp,lineHeight=38.sp,fontWeight=FontWeight.SemiBold,color=EzTokens.Ink)
                Text("شارك ملفاتك مع القريبين",fontSize=10.sp,lineHeight=16.sp,color=EzTokens.Ink)
                Spacer(Modifier.weight(1f));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("اختر ملفاتك",fontSize=10.sp,color=EzTokens.Ink);EzIcon(R.drawable.ic_arrow_up_left,size=20.dp,tint=EzTokens.Ink)}
            }
            EzIconAt(R.drawable.ic_arrow_up_left,Modifier.align(Alignment.TopEnd).padding(top=56.dp,start=16.dp,end=16.dp),37.dp)
        }
        Column(Modifier.weight(.92f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.blue).ezPress(onReceive).padding(16.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){EzTag("إلى جهازك");EzIcon(R.drawable.ic_arrow_down_right,size=32.dp,tint=EzTokens.Ink)}
                Spacer(Modifier.weight(1f));Text("استقبال",fontSize=27.sp,lineHeight=38.sp,fontWeight=FontWeight.SemiBold,color=EzTokens.Ink);Text("جاهز لشيء جديد؟",fontSize=10.sp,color=EzTokens.Ink,lineHeight=16.sp)
            }
            Row(Modifier.fillMaxWidth().height(68.dp*LocalDensity.current.fontScale.coerceAtLeast(1f)).clip(RoundedCornerShape(24.dp)).background(p.pink).ezPress(onPrivacy).padding(horizontal=8.dp,vertical=12.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){EzIcon(R.drawable.ic_shield_check,size=24.dp,tint=EzTokens.Ink);Spacer(Modifier.width(10.dp));Text("بينك وبينه.\nبخصوصية تامة.",fontSize=10.sp,lineHeight=16.sp,color=EzTokens.Ink)}
        }
    }
    EzSection("كل ملفاتك","عرض الكل",{onFiles("الكل")})
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){categories.forEach{c->Column(Modifier.weight(1f).heightIn(min=82.dp).ezPress({onFiles(c.label)}),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.fillMaxWidth().height(51.dp).clip(RoundedCornerShape(19.dp)).background(c.tint),contentAlignment=Alignment.Center){EzIcon(c.icon,size=25.dp,tint=EzTokens.Ink)};Text(c.label,Modifier.padding(top=7.dp),fontSize=10.sp,fontWeight=FontWeight.Medium)}}}
    EzSection("آخر مشاركة",icon=R.drawable.ic_clock_counter_clockwise)
    if(latest!=null)HistoryCard(latest,onHistory) else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.soft).ezPress({onFiles("الكل")}).heightIn(min=76.dp).padding(horizontal=12.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(p.surface),contentAlignment=Alignment.Center){EzIcon(R.drawable.ic_folder_open,size=27.dp)}
        Column(Modifier.weight(1f)){Text("أول مشاركة تبدأ منك",fontSize=11.sp,fontWeight=FontWeight.SemiBold);Text("أرسل صورة، فيديو، أو أي ملف تحبه.",Modifier.padding(top=2.dp),fontSize=9.sp,color=p.muted)};EzIcon(R.drawable.ic_arrow_up_left,size=21.dp)
    }
}
@Composable private fun EzIconAt(id:Int,modifier:Modifier,size:Dp){Box(modifier){EzIcon(id,size=size,tint=EzTokens.Ink)}}
@Composable fun EzNavigation(active:Int,onTab:(Int)->Unit,modifier:Modifier=Modifier){
    val quiet=LocalEzQuiet.current
    val labels=listOf("الرئيسية","الملفات","السجل","الإعدادات");val icons=listOf(R.drawable.ic_house,R.drawable.ic_squares_four,R.drawable.ic_clock_counter_clockwise,R.drawable.ic_gear_six)
    BoxWithConstraints(modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(60.dp)).background(Color(0xff101017)).padding(6.dp)){
        val item=64.dp;val step=(maxWidth-item)/3;val target=step*active
        val offset by animateDpAsState(target,if(quiet)snap() else spring(dampingRatio=EzTokens.SpringDamping,stiffness=EzTokens.SpringStiffness),label="nav-indicator")
        Box(Modifier.offset(x=offset).width(item).fillMaxHeight().clip(RoundedCornerShape(50)).background(Color.White))
        Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.SpaceBetween){labels.forEachIndexed{i,label->Column(Modifier.width(item).fillMaxHeight().ezPress({onTab(i)}),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){EzIcon(icons[i],label,24.dp,if(active==i)EzTokens.Ink else Color(0xffbcbcc4));Text(label,fontSize=8.sp,lineHeight=13.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=if(active==i)EzTokens.Ink else Color(0xffbcbcc4))}}}
    }
}
@Composable fun FilterChip(label:String,selected:Boolean,onClick:()->Unit){val p=LocalEzPalette.current;Box(Modifier.clip(RoundedCornerShape(30.dp)).background(if(selected)EzTokens.Ink else p.soft).ezPress(onClick).heightIn(min=44.dp).padding(horizontal=12.dp,vertical=10.dp),contentAlignment=Alignment.Center){Text(label,fontSize=11.sp,color=if(selected)Color.White else p.ink)}}
@Composable fun ChoiceMenu(value:String,options:List<String>,onChoice:(String)->Unit){var open by remember{mutableStateOf(false)};val p=LocalEzPalette.current;Box{Row(Modifier.clip(RoundedCornerShape(14.dp)).background(p.soft).border(1.dp,p.line,RoundedCornerShape(14.dp)).ezPress({open=true}).heightIn(min=44.dp).padding(9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(value,fontSize=11.sp);EzIcon(R.drawable.ic_caret_down,size=14.dp)};DropdownMenu(open,{open=false}){options.forEach{o->DropdownMenuItem(text={Text(o,fontSize=12.sp)},onClick={onChoice(o);open=false})}}}}
@Composable fun SearchField(value:String,onChange:(String)->Unit,label:String){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().padding(top=16.dp).clip(RoundedCornerShape(20.dp)).background(p.soft).border(1.dp,p.line,RoundedCornerShape(20.dp)).heightIn(min=49.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    EzIcon(R.drawable.ic_magnifying_glass,size=20.dp);BasicTextField(value,onChange,Modifier.weight(1f).padding(vertical=14.dp),singleLine=true,textStyle=LocalTextStyle.current.copy(fontSize=12.sp,color=p.ink),decorationBox={inner->if(value.isEmpty())Text(label,fontSize=12.sp,color=p.muted);inner()});if(value.isNotEmpty())IconButton(onClick={onChange("")},Modifier.size(40.dp)){EzIcon(R.drawable.ic_x,"مسح البحث",18.dp)}
}}
@Composable fun EzField(value:String,onChange:(String)->Unit,label:String,placeholder:String="",enabled:Boolean=true){val p=LocalEzPalette.current;Column(Modifier.fillMaxWidth()){Text(label,fontSize=12.sp);BasicTextField(value,onChange,Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(17.dp)).background(p.surface).border(1.dp,p.line,RoundedCornerShape(17.dp)).heightIn(min=48.dp).padding(12.dp),enabled=enabled,singleLine=true,textStyle=LocalTextStyle.current.copy(color=p.ink,fontSize=14.sp),decorationBox={inner->if(value.isEmpty())Text(placeholder,fontSize=12.sp,color=p.muted);inner()})}}
@Composable fun FileRow(name:String,subtitle:String,icon:Int,selected:Boolean,onClick:(()->Unit)?){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(18.dp)).background(if(selected)if(p.dark)Color(0xff393048) else Color(0xfff0eafd) else p.surface).then(if(onClick!=null)Modifier.ezPress(onClick) else Modifier).padding(horizontal=9.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    Box(Modifier.size(width=49.dp,height=54.dp).clip(RoundedCornerShape(17.dp)).background(Color(0xffe5dcfa)),contentAlignment=Alignment.Center){EzIcon(icon,size=26.dp,tint=EzTokens.Ink)}
    Column(Modifier.weight(1f)){Text(name,fontSize=12.sp,fontWeight=FontWeight.Medium,lineHeight=22.sp);Text(subtitle,fontSize=10.sp,color=p.muted)}
    Box(Modifier.size(23.dp).clip(RoundedCornerShape(50)).background(if(selected)EzTokens.Ink else Color.Transparent).border(1.dp,if(selected)EzTokens.Ink else p.line,RoundedCornerShape(50)),contentAlignment=Alignment.Center){if(selected)EzIcon(R.drawable.ic_check,size=16.dp,tint=Color.White)}
}}
@Composable fun StorageSummary(){val p=LocalEzPalette.current;val storage=remember{runCatching{StatFs(Environment.getDataDirectory().path).let{it.totalBytes to it.availableBytes}}.getOrNull()};Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(if(p.dark)Color(0xff293748) else Color(0xffeaf3ff)).padding(16.dp)){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){EzIcon(R.drawable.ic_hard_drives,size=23.dp);Text("مساحة جهازك",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("قسم بيانات الجهاز",fontSize=10.sp,color=p.muted)}
    if(storage!=null&&storage.first>0){val used=storage.first-storage.second;LinearProgressIndicator(progress={used.toFloat()/storage.first},modifier=Modifier.fillMaxWidth().padding(vertical=14.dp).height(7.dp).clip(RoundedCornerShape(20.dp)),color=EzTokens.Accent,trackColor=p.line,drawStopIndicator={});Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${humanSize(used)} مستخدم",fontSize=10.sp);Text("${humanSize(storage.second)} متاح",fontSize=10.sp)}}else Text("المساحة غير متاحة",fontSize=10.sp)
    Text("قد تختلف مساحة مجلد الاستقبال المختار.",Modifier.padding(top=8.dp),fontSize=9.sp,color=p.muted)
}}
@Composable fun SettingsRow(title:String,subtitle:String,icon:Int,onClick:()->Unit){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().ezPress(onClick).padding(vertical=22.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(subtitle,fontSize=10.sp,color=p.muted)};EzIcon(icon,size=20.dp)};HorizontalDivider(color=p.line)}
@Composable fun SheetOption(icon:Int,title:String,body:String,onClick:()->Unit){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(p.soft).ezPress(onClick).padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)){EzIcon(icon,size=25.dp,tint=EzTokens.Accent);Column(Modifier.weight(1f)){Text(title,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(body,fontSize=10.sp,color=p.muted,lineHeight=19.sp)}}}
@Composable fun HistoryCard(r:HistoryEntry,onClick:()->Unit){val p=LocalEzPalette.current;Row(Modifier.fillMaxWidth().padding(vertical=6.dp).clip(RoundedCornerShape(23.dp)).background(p.soft).ezPress(onClick).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(11.dp)){
    Box(Modifier.size(42.dp,48.dp).clip(RoundedCornerShape(16.dp)).background(if(r.incoming)EzTokens.CategoryBlue else EzTokens.Peach),contentAlignment=Alignment.Center){EzIcon(if(r.incoming)R.drawable.ic_arrow_down_right else R.drawable.ic_arrow_up_left,size=26.dp,tint=EzTokens.Ink)}
    Column(Modifier.weight(1f)){Text(r.name,fontSize=13.sp,fontWeight=FontWeight.SemiBold,lineHeight=22.sp);Text("${if(r.incoming)"استقبلت من" else "أرسلت إلى"} ${r.peer}",fontSize=10.sp,color=p.muted);Text(dateLabel(r.date),fontSize=10.sp,color=p.muted);Text("${humanSize(r.bytes)} · ${statusLabel(r.status)}",fontSize=10.sp,color=p.muted)};EzIcon(R.drawable.ic_arrow_up_left,size=18.dp)
}}
@Composable fun TransferContent(state:TransferUi,onCancel:()->Unit,onDetails:()->Unit,onRetry:()->Unit){
    val p=LocalEzPalette.current;val pr=state.progress;val done=state.result!=null;val failed=state.error!=null;val cancelled=state.status=="تم الإلغاء";val pct=if(done)100f else pr?.percent?.toFloat()
    val eased by animateFloatAsState(pct?:0f,if(LocalEzQuiet.current)snap() else tween(400,easing=LinearEasing),label="actual-progress")
    EzBadge("نقل فعلي · القيم من البايتات المنقولة")
    EzDevice(state.peer.ifBlank{"الجهاز الآخر"},if(state.incoming)"من" else "إلى",1)
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.padding(top=20.dp,bottom=20.dp).size(86.dp).clip(RoundedCornerShape(30.dp)).background(if(done)Color(0xffcfe9dd) else if(failed)EzTokens.Peach else EzTokens.CategoryPurple),contentAlignment=Alignment.Center){EzIcon(if(done)R.drawable.ic_check_circle else if(cancelled)R.drawable.ic_x else if(failed)R.drawable.ic_warning_circle else R.drawable.ic_broadcast,size=52.dp,tint=EzTokens.Ink)}
        Text(if(done)"وصلت، بكل سهولة." else if(cancelled)"تم إلغاء النقل" else if(failed)"تعذّر إكمال النقل" else if(state.phase=="verifying")"جارٍ التحقق من الحفظ" else if(pr!=null)"جارٍ النقل" else "جارٍ الاتصال",fontSize=25.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Text(if(pct!=null)"%.1f%%".format(Locale.US,eased) else "—",Modifier.padding(top=20.dp),fontSize=48.sp,lineHeight=72.sp,fontWeight=FontWeight.SemiBold)
        if(pr!=null&&pct!=null)CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){LinearProgressIndicator(progress={(eased/100).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp).height(10.dp).clip(RoundedCornerShape(20.dp)),color=EzTokens.Accent,trackColor=p.line,drawStopIndicator={})} else if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=12.dp),color=EzTokens.Accent)
        Text("${humanSize(pr?.bytes?:0)} من ${humanSize(pr?.total?:-1)}",fontSize=12.sp,color=p.muted)
        Text("${pr?.bytes?:0} / ${if((pr?.total?:-1)<0)"غير معروف" else pr?.total} بايت",Modifier.padding(top=8.dp),fontSize=10.sp,color=p.muted)
        Row(Modifier.fillMaxWidth().padding(vertical=20.dp).clip(RoundedCornerShape(22.dp)).background(p.soft).padding(vertical=15.dp,horizontal=4.dp),horizontalArrangement=Arrangement.SpaceEvenly){Metric("${if(state.files.isEmpty())if(done)1 else 0 else state.completedResults.size} / ${state.files.size.coerceAtLeast(1)}","ملفات مكتملة",Modifier.weight(1f));Metric(if(pr!=null)"%.2f MB/s".format(Locale.US,pr.currentMBps) else "—","السرعة الحالية",Modifier.weight(1f));Metric(if(done)"0 ث" else pr?.etaSeconds?.let{"%.0f ث".format(Locale.US,it)}?:"—","الوقت المتبقي",Modifier.weight(1f))}
    }
    if(state.filename.isNotBlank()){Text("الملف الحالي: ${state.filename}",Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).border(1.dp,p.line,RoundedCornerShape(17.dp)).padding(10.dp),fontSize=11.sp)}
    pr?.let{Text("المتوسط الفعلي: %.2f MB/s".format(Locale.US,it.averageMBps),Modifier.padding(top=12.dp),fontSize=12.sp,color=p.muted)}
    state.files.forEachIndexed{i,m->val verified=i<state.completedResults.size;Text("${if(verified)"✓" else if(i==state.fileIndex)"•" else "○"} ${m.name} · ${if(verified)"مكتمل ومتحقق" else if(failed||cancelled)"لم يكتمل" else "بانتظار الإكمال"}",Modifier.padding(top=8.dp),fontSize=10.sp,color=p.muted)}
    if(state.timings.isNotEmpty()){EzSection("تفاصيل الزمن الفعلي");state.timings.forEach{t->Text("${phaseLabel(t.phase)}${if(t.fileIndex>=0)" · ملف ${t.fileIndex+1}" else ""}: ${"%.3f".format(Locale.US,t.nanos/1e9)} ث",fontSize=10.sp,color=p.muted)}}
    if(state.busy)EzButton("إلغاء النقل",onCancel,Modifier.fillMaxWidth().padding(vertical=20.dp),outline=true)
    state.error?.let{Spacer(Modifier.height(16.dp));EzNotice(it,true);Spacer(Modifier.height(12.dp));EzButton("إعادة المحاولة",onRetry,Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));EzNotice("ستبدأ عملية جديدة. استكمال نقل منقطع غير متاح؛ لا تُعد الملفات الجزئية مكتملة.")}
    state.result?.let{r->Spacer(Modifier.height(16.dp));EzNotice("تمت كتابة الملف والتحقق من الحجم وبصمة SHA-256. ${if(state.incoming)"حُفظ في المجلد المحدد." else "وصل إيصال التحقق من المستقبل."}");EzButton("تفاصيل العملية",onDetails,Modifier.fillMaxWidth().padding(top=20.dp),outline=true)}
    if(!state.busy&&pr==null&&!done&&!failed)EzEmpty("أول مشاركة تبدأ منك","ابدأ إرسالًا أو استقبالًا لعرض تقدم حقيقي هنا.")
    Text("الإيقاف المؤقت والاستئناف قريبًا. لا تُعرض قياسات سرعة وهمية.",Modifier.fillMaxWidth().padding(top=20.dp),textAlign=TextAlign.Center,fontSize=9.sp,color=p.muted)
}
@Composable private fun Metric(value:String,label:String,modifier:Modifier=Modifier){Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){Text(value,fontSize=12.sp,fontWeight=FontWeight.SemiBold,style=LocalTextStyle.current.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr,textAlign=TextAlign.Center));Text(label,Modifier.padding(top=5.dp),fontSize=9.sp,color=LocalEzPalette.current.muted)}}
fun fileCategory(mime:String,name:String)=when{mime.startsWith("image/")->"الصور";mime.startsWith("video/")->"الفيديوهات";mime.startsWith("audio/")->"الصوتيات";name.endsWith(".apk",true)->"التطبيقات";mime.startsWith("text/")||mime=="application/pdf"||name.endsWith(".docx",true)->"المستندات";else->"ملفات أخرى"}
fun fileIcon(category:String)=categories.find{it.label==category}?.icon?:R.drawable.ic_file_text
fun statusLabel(status:String)=when(status){"completed"->"مكتمل";"cancelled"->"تم الإلغاء";else->"تعذّر إكمال النقل"}
fun dateLabel(millis:Long)=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,Locale("ar","SA")).format(Date(millis))

fun phaseLabel(phase:String)=when(phase){"discovery_first_peer"->"ظهور أول جهاز";"connection"->"الاتصال";"handshake"->"تهيئة التشفير";"approval"->"انتظار الموافقة";"first_wire_chunk"->"أول جزء من الملف";"streaming"->"نقل البيانات";"storage_write_within_streaming"->"الكتابة أثناء النقل";"storage_readback_and_publish"->"التحقق من الملف المحفوظ";"receipt_and_remote_verification"->"انتظار التحقق والإيصال";"total_session"->"الجلسة كاملة دون الاكتشاف";else->phase}
