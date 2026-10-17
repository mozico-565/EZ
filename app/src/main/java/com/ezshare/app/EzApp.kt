package com.ezshare.app

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.ezshare.core.Progress
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun EzApp(
    state:TransferUi,prefs:EzPreferences,history:List<HistoryEntry>,addresses:List<String>,
    onPick:()->Unit={},onFolder:()->Unit={},onDiscover:()->Unit={},onSend:(String)->Unit={},onReceive:()->Unit={},onCancel:()->Unit={},onDecide:(Boolean)->Unit={},onSave:(EzPreferences)->Unit={},onClearHistory:()->Unit={},
    initialDestination:String="home",initialSheet:String=""
){
    val p=LocalEzPalette.current;val quiet=LocalEzQuiet.current
    var screen by rememberSaveable{mutableStateOf(initialDestination)};var sheet by rememberSaveable{mutableStateOf(initialSheet)}
    var category by rememberSaveable{mutableStateOf("الكل")};var query by rememberSaveable{mutableStateOf("")};var selected by rememberSaveable{mutableStateOf(true)}
    var sort by rememberSaveable{mutableStateOf("الاسم")};var descending by rememberSaveable{mutableStateOf(false)};var address by rememberSaveable{mutableStateOf("")}
    var historyQuery by rememberSaveable{mutableStateOf("")};var direction by rememberSaveable{mutableStateOf("الكل")};var historyStatus by rememberSaveable{mutableStateOf("كل الحالات")}
    var detail by remember{mutableStateOf<HistoryEntry?>(null)};var draftName by rememberSaveable(prefs.name){mutableStateOf(prefs.name)};var draftAvatar by rememberSaveable(prefs.avatar){mutableIntStateOf(prefs.avatar)}
    var feedback by remember{mutableStateOf("")}
    fun route(s:String){screen=s;feedback=""}
    fun openFiles(c:String="الكل"){category=c;route("files")}
    fun back(){if(state.request!=null)onDecide(false);route(when(screen){"discover","scanner","confirm"->if(state.incoming)"receive" else "files";"detail"->"history";else->"home"})}
    BackHandler(screen!="home"||sheet.isNotEmpty()){if(sheet.isNotEmpty())sheet="" else back()}
    LaunchedEffect(state.picked){if(state.picked!=null){selected=true;if(screen!="home")screen="files"}}
    LaunchedEffect(state.request){if(state.request!=null)screen="confirm"}
    LaunchedEffect(state.stage){if(state.stage=="transfer"||state.stage=="result")screen="transfer"}
    val tab=when(screen){"files","discover","scanner"->1;"history","detail","transfer"->2;"settings"->3;else->0}
    val pageTitle=when(screen){"files"->"اختر ما تريد مشاركته";"receive"->"جاهز للاستقبال";"discover","scanner","confirm"->"اتصال قريب وآمن";"transfer"->"مشاركة، خطوة بخطوة";"detail"->"تفاصيل المشاركة";"history"->"كل مشاركة، حكاية";"settings"->"على طريقتك";else->"أهلًا، خلّنا نشارك!"}
    val density=LocalDensity.current;val top=with(density){WindowInsets.safeDrawing.getTop(this).toDp()};val bottom=WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(p.background).testTag("ez-screen")){
        Column(Modifier.fillMaxSize().padding(top=maxOf(64.dp,top+8.dp))){
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp).height(60.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Image(painterResource(R.drawable.ez_logo),"شعار EZ",Modifier.width(52.dp).height(28.dp).clip(RoundedCornerShape(5.dp)).background(Color.White),contentScale=ContentScale.Fit)
                Column(Modifier.weight(1f)){Text("قريبين، حتى بدون إنترنت",fontSize=9.sp,color=p.muted,lineHeight=16.sp,maxLines=1);Text(pageTitle,fontSize=12.sp,fontWeight=FontWeight.SemiBold,lineHeight=20.sp,maxLines=2)}
                if(screen=="home")EzRound(R.drawable.ic_share_network,"مشاركة تطبيق EZ",{sheet="share"},true)
                EzRound(if(screen in listOf("home","history","settings"))R.drawable.ic_gear_six else R.drawable.ic_arrow_right,if(screen in listOf("home","history","settings"))"الإعدادات" else "رجوع",{if(screen in listOf("home","history","settings"))route("settings") else back()})
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(screen,transitionSpec={if(quiet)(fadeIn(snap()) togetherWith fadeOut(snap())) else ((fadeIn(tween(280))+slideInHorizontally(tween(280,easing=CubicBezierEasing(.22f,1f,.36f,1f))){-18}) togetherWith (fadeOut(tween(140))+slideOutHorizontally(tween(280)){20}))},label="page",modifier=Modifier.weight(1f)) { page->
                key(page){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=8.dp,bottom=bottom+if(page=="files")240.dp else 130.dp)){
                    if(state.error!=null&&page!="transfer"&&page!="home"){EzNotice(state.error,true);Spacer(Modifier.height(16.dp))}
                    when(page){
                        "home"->{
                            HomeContent(onFiles={openFiles(it)},onReceive={route("receive")},onPrivacy={sheet="privacy"},latest=history.firstOrNull(),onHistory={route("history")})
                            Text("Alpha 0.1.1 · نقل ملف حقيقي عبر شبكة محلية",Modifier.fillMaxWidth().padding(top=18.dp),textAlign=TextAlign.Center,fontSize=9.sp,color=p.muted)
                        }
                        "files"->{
                            EzBadge("ملفاتك المختارة · من جهازك")
                            Text("أشياء تستحق\nالمشاركة.",style=MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(20.dp));StorageSummary()
                            SearchField(query,{query=it},"ابحث في الملفات المختارة")
                            FlowRow(Modifier.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){(listOf("الكل")+categories.map{it.label}+"ملفات أخرى").forEach{c->FilterChip(c,c==category,{category=c})}}
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Row(verticalAlignment=Alignment.CenterVertically){Text("ترتيب حسب",fontSize=11.sp);Spacer(Modifier.width(6.dp));ChoiceMenu(sort,listOf("الاسم","الحجم","التاريخ")){sort=it}};TextButton(onClick={descending=!descending}){Text(if(descending)"تنازلي ↓" else "تصاعدي ↑",fontSize=11.sp)}}
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){TextButton(onClick={selected=!selected},enabled=state.picked!=null){Text(if(selected&&state.picked!=null)"إلغاء تحديد الظاهر" else "تحديد كل الظاهر",fontSize=12.sp)};Text("${if(selected&&state.picked!=null)1 else 0} محدد · ${if(selected)humanSize(state.picked?.metadata?.size?:0) else "0 B"}",fontSize=11.sp)}
                            val file=state.picked
                            if(file!=null&&file.metadata.name.contains(query.trim(),true)&&(category=="الكل"||fileCategory(file.metadata.mime,file.metadata.name)==category)){
                                FileRow(file.metadata.name,humanSize(file.metadata.size),fileIcon(fileCategory(file.metadata.mime,file.metadata.name)),selected,{selected=!selected})
                            }else EzEmpty(if(query.isNotBlank())"لا توجد نتائج" else if(file!=null)"هذه الفئة فارغة" else "اختر ما يستحق المشاركة",if(query.isNotBlank())"جرّب كلمة أخرى أو امسح البحث." else "اختر ملفًا من جهازك باستخدام منتقي Android.",action=if(query.isNotBlank())"مسح البحث" else "اختيار ملف",onAction={if(query.isNotBlank())query="" else onPick()})
                            EzButton(if(file==null)"اختيار ملف حقيقي" else "تغيير الملف",onPick,Modifier.fillMaxWidth().padding(top=16.dp),enabled=!state.busy,outline=true,icon=R.drawable.ic_folder_open)
                            Spacer(Modifier.height(16.dp));EzNotice("تدعم Alpha ملفًا واحدًا في كل عملية. التحديد المتعدد لاحقًا؛ لا تُعرض ملفات وهمية أو تُطلب صلاحية وصول شاملة.")
                            if(category=="التطبيقات"){Spacer(Modifier.height(12.dp));EzNotice("يمكن اختيار APK قابل للتوزيع من الملفات. استخراج التطبيقات المثبتة والحزم المجزأة غير منفّذ.")}
                        }
                        "discover"->{
                            EzBadge("اقتران محلي · شبكة Wi-Fi نفسها")
                            Text("مين نشارك معه؟",style=MaterialTheme.typography.headlineLarge)
                            Text("ملف واحد · ${humanSize(state.picked?.metadata?.size?:0)}",fontSize=12.sp,color=p.muted)
                            Box(Modifier.padding(top=24.dp,bottom=12.dp).clip(RoundedCornerShape(25.dp)).background(EzTokens.CategoryPurple).ezPress({route("scanner")})){
                                Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){EzIcon(R.drawable.ic_scan,size=30.dp,tint=EzTokens.Ink);Column(Modifier.weight(1f)){Text("مسح رمز QR",fontWeight=FontWeight.SemiBold,color=EzTokens.Ink);Text("قريبًا · الاتصال اليدوي متاح الآن",fontSize=10.sp,color=EzTokens.Ink)};EzIcon(R.drawable.ic_arrow_up_left,size=20.dp,tint=EzTokens.Ink)}
                            }
                            EzSection("أجهزة قريبة","إعادة البحث",onDiscover)
                            Text(state.discoveryMessage.ifBlank{"فعّل الاستقبال على الهاتف الآخر."},fontSize=11.sp,color=p.muted)
                            if(state.busy){EzEmpty("جارٍ الاتصال…","قارن الرمز على الهاتفين عند ظهوره.",R.drawable.ic_broadcast);LinearProgressIndicator(Modifier.fillMaxWidth(),color=EzTokens.Accent)}
                            else if(state.peers.isEmpty())EzEmpty("لا توجد أجهزة قريبة","قرّب الجهاز الآخر وفعّل الاستقبال، أو استخدم العنوان اليدوي.",R.drawable.ic_devices, "إعادة المحاولة",onDiscover)
                            else state.peers.forEach{peer->Row(Modifier.fillMaxWidth().padding(vertical=5.dp).clip(RoundedCornerShape(23.dp)).background(p.soft).ezPress({onSend(peer.address)},state.picked!=null&&!state.busy).padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){EzAvatar(1);Column(Modifier.weight(1f)){Text(peer.name,fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text(peer.address,fontSize=10.sp,color=p.muted)};EzIcon(R.drawable.ic_arrow_up_left,size=20.dp)}}
                            Spacer(Modifier.height(16.dp));EzField(address,{address=it},"العنوان اليدوي IP:PORT","192.168.1.8:34567",enabled=!state.busy)
                            EzButton("اتصال وطلب مشاركة الملف",{onSend(address)},Modifier.fillMaxWidth().padding(vertical=16.dp),enabled=state.picked!=null&&address.isNotBlank()&&!state.busy)
                            if(state.busy)EzButton("إلغاء الاتصال",onCancel,Modifier.fillMaxWidth(),outline=true)
                            EzNotice("الاسم والعنوان لا يثبتان هوية الجهاز. يجب مقارنة رمز التحقق قبل قبول النقل.")
                        }
                        "receive"->{
                            EzBadge("استقبال محلي · موافقتك أولًا")
                            EzDevice(prefs.name,"تظهر للآخرين باسم",prefs.avatar)
                            Text("خلّ جهازك قريب.",Modifier.fillMaxWidth().padding(top=20.dp,bottom=8.dp),textAlign=TextAlign.Center,fontSize=28.sp,lineHeight=40.sp,fontWeight=FontWeight.ExtraBold)
                            Text("يستطيع المرسل اختيار جهازك من الأجهزة القريبة.",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=12.sp,color=p.muted)
                            Column(Modifier.fillMaxWidth().padding(top=24.dp,bottom=12.dp).clip(RoundedCornerShape(28.dp)).background(p.surface).border(1.dp,p.line,RoundedCornerShape(28.dp)).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                                Box(Modifier.size(132.dp).clip(RoundedCornerShape(38.dp)).background(EzTokens.CategoryBlue),contentAlignment=Alignment.Center){EzIcon(if(state.listening)R.drawable.ic_broadcast else R.drawable.ic_devices,size=68.dp,tint=EzTokens.Ink)}
                                Text(if(state.listening)"بانتظار طلب مشاركة" else "جاهز للاستقبال",fontSize=16.sp,fontWeight=FontWeight.SemiBold)
                                Text("QR قريبًا · الاتصال اليدوي متاح",fontSize=10.sp,color=p.muted)
                            }
                            EzButton(if(state.folder==null)"اختيار مجلد الحفظ" else "تغيير مجلد الحفظ",onFolder,Modifier.fillMaxWidth().padding(top=12.dp),enabled=!state.busy&&!state.listening,outline=true)
                            if(state.folder!=null)Text("مجلد محلي محدد · لا يُحفظ ملف قبل موافقتك",Modifier.padding(top=8.dp),fontSize=10.sp,color=p.muted)
                            if(!state.listening)EzButton("تفعيل الاستقبال",onReceive,Modifier.fillMaxWidth().padding(top=16.dp,bottom=16.dp),enabled=state.folder!=null&&!state.busy)
                            else{
                                Column(Modifier.fillMaxWidth().padding(vertical=16.dp).clip(RoundedCornerShape(18.dp)).background(p.soft).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("العنوان اليدوي للمُرسل",fontSize=12.sp);SelectionContainer{Column{addresses.forEach{Text("$it:${state.port}",fontWeight=FontWeight.SemiBold,fontSize=18.sp)}}};if(addresses.isEmpty())Text("لا يظهر عنوان Wi-Fi. اتصل بشبكة محلية أولًا.",fontSize=12.sp);Text("سيظهر طلب القبول ورمز المقارنة هنا.",fontSize=10.sp,color=p.muted)}
                                EzButton("إيقاف الاستقبال",onCancel,Modifier.fillMaxWidth().padding(bottom=16.dp),outline=true)
                            }
                            EzNotice("اختر مجلدًا محليًا فرعيًا مثل Documents/EZ. النقل يحتاج هاتفين على الشبكة نفسها؛ لا يحتاج إنترنت.")
                        }
                        "scanner"->{EzBadge("QR · قريبًا");Text("قرّب الرمز.",style=MaterialTheme.typography.headlineLarge);Text("المسح غير مفعّل في هذه النسخة. لا نطلب إذن الكاميرا.",fontSize=12.sp,color=p.muted);Column(Modifier.fillMaxWidth().padding(top=24.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xff161b25)).height(270.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){EzIcon(R.drawable.ic_scan,size=100.dp,tint=Color(0xffeae5ff));Text("ماسح QR — قريبًا",Modifier.padding(top=22.dp),color=Color.White,fontSize=12.sp)};EzButton("اختيار جهاز قريب",{route("discover")},Modifier.fillMaxWidth().padding(top=24.dp));Spacer(Modifier.height(16.dp));EzNotice("عرض رمز لا ينشئ اتصالًا لاسلكيًا بمفرده. استخدم اكتشاف الأجهزة أو عنوان IP والمنفذ الآن.")}
                        "confirm"->{
                            val r=state.request
                            EzBadge("تأكيد الاتصال · مقارنة على الهاتفين")
                            if(r!=null){Column(Modifier.fillMaxWidth().padding(vertical=20.dp).clip(RoundedCornerShape(28.dp)).background(p.soft).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){EzAvatar(1);Text(if(r.incoming)"تقبل المشاركة؟" else "هذا هو الجهاز؟",fontSize=28.sp,fontWeight=FontWeight.Bold);Text(r.peer,fontSize=19.sp,fontWeight=FontWeight.SemiBold);Text("رمز التحقق على الجهازين",fontSize=12.sp,color=p.muted);SelectionContainer{Text(r.sas,fontSize=38.sp,fontWeight=FontWeight.Bold,letterSpacing=4.sp)};r.metadata?.let{Text(it.name,fontSize=12.sp);Text(humanSize(it.size),fontSize=11.sp,color=p.muted)};Text("قارن الرمز نفسه على الهاتفين. وافق فقط إذا تطابق وكان الجهاز معروفًا لك.",fontSize=12.sp,textAlign=TextAlign.Center,color=p.muted)}
                                EzButton(if(r.incoming)"قبول الاتصال والاستقبال" else "تأكيد وإرسال الملف",{onDecide(true);route("transfer")},Modifier.fillMaxWidth().padding(top=12.dp));EzButton("إلغاء الاتصال",{onDecide(false);back()},Modifier.fillMaxWidth().padding(top=12.dp),outline=true)
                            }else EzEmpty("انتهى طلب الاتصال","ارجع إلى الأجهزة القريبة وأعد المحاولة.",action="رجوع",onAction={back()})
                        }
                        "transfer"->{TransferContent(state,onCancel={sheet="cancel-transfer"},onDetails={detail=history.firstOrNull();route("detail")},onRetry={if(state.incoming)route("receive") else {route("discover");onDiscover()}})}
                        "history"->{
                            EzBadge("سجل المشاركات الفعلية")
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("كل مشاركة،\nحكاية.",Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge);IconButton(onClick={sheet="clear-history"}){EzIcon(R.drawable.ic_trash,"مسح السجل",21.dp)}}
                            SearchField(historyQuery,{historyQuery=it},"ابحث في السجل")
                            FlowRow(Modifier.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("الكل","مرسل","مستقبل").forEach{d->FilterChip(d,direction==d,{direction=d})};ChoiceMenu(historyStatus,listOf("كل الحالات","مكتمل","فشل / انقطع")){historyStatus=it}}
                            val list=history.filter{r->(direction=="الكل"||r.incoming==(direction=="مستقبل"))&&(historyStatus=="كل الحالات"||(r.status=="completed")== (historyStatus=="مكتمل"))&&(r.name+" "+r.peer).contains(historyQuery.trim(),true)}
                            if(list.isEmpty())EzEmpty(if(history.isEmpty())"أول مشاركة تبدأ منك" else "لا توجد نتائج","مسح السجل لا يحذف الملفات المنقولة.",action=if(history.isEmpty())"اختيار ملفات" else "مسح عوامل التصفية",onAction={if(history.isEmpty())openFiles() else {historyQuery="";direction="الكل";historyStatus="كل الحالات"}})
                            list.forEach{r->HistoryCard(r,{detail=r;route("detail")})}
                        }
                        "detail"->{
                            val r=detail?:history.firstOrNull()
                            EzBadge("تفاصيل نقل فعلي");Text("تفاصيل المشاركة",style=MaterialTheme.typography.headlineLarge)
                            if(r==null)EzEmpty("لا توجد عملية بعد","ابدأ مشاركة جديدة للعثور على تفاصيلها هنا.") else{
                                EzDevice(r.peer,if(r.incoming)"استقبلت من" else "أرسلت إلى",1)
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(p.soft).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(statusLabel(r.status),fontSize=17.sp,fontWeight=FontWeight.SemiBold);Text(dateLabel(r.date),fontSize=12.sp,color=p.muted);Text("ملف واحد · ${humanSize(r.total)}",fontSize=12.sp);Text("المدة النشطة: ${"%.2f".format(Locale.US,r.duration)} ث",fontSize=12.sp);Text("متوسط السرعة: ${if(r.duration>0)"%.2f MB/s".format(Locale.US,r.bytes/r.duration/1e6) else "غير متاح"}",fontSize=12.sp);if(r.sha.isNotBlank())SelectionContainer{Text("SHA-256\n${r.sha}",fontSize=10.sp,lineHeight=18.sp)}}
                                FileRow(r.name,"${humanSize(r.bytes)} · ${statusLabel(r.status)}",R.drawable.ic_file_text,r.status=="completed",null)
                                EzButton("مشاركة الملف مرة أخرى",{openFiles();onPick()},Modifier.fillMaxWidth().padding(top=24.dp),outline=true)
                                if(r.status!="completed"){Spacer(Modifier.height(16.dp));EzNotice("إعادة المحاولة تبدأ نقلًا جديدًا. الاستئناف من إزاحة سابقة غير متاح في Alpha.")}
                            }
                        }
                        "settings"->{
                            EzBadge("تفضيلات EZ");Text("بسيط، مثلك.",style=MaterialTheme.typography.headlineLarge)
                            EzSection("جهازي","استعادة المحفوظ",{draftName=prefs.name;draftAvatar=prefs.avatar})
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp)).background(p.soft).padding(18.dp)){
                                EzDevice(draftName.ifBlank{"اسم الجهاز"},"كما يظهر للآخرين",draftAvatar)
                                EzField(draftName,{draftName=it.take(28)},"اسم الجهاز")
                                Row(Modifier.padding(vertical=18.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){(0..3).forEach{i->Box(Modifier.clip(RoundedCornerShape(22.dp)).border(2.dp,if(draftAvatar==i)EzTokens.Accent else Color.Transparent,RoundedCornerShape(22.dp)).ezPress({draftAvatar=i}).padding(3.dp)){EzAvatar(i,43.dp)}}}
                                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){EzButton("حفظ",{onSave(prefs.copy(name=draftName.trim(),avatar=draftAvatar));feedback="تم حفظ هوية جهازك"},Modifier.weight(1f),enabled=draftName.isNotBlank()&&!state.busy&&!state.listening);EzButton("إلغاء",{draftName=prefs.name;draftAvatar=prefs.avatar;feedback="تم إلغاء التعديلات"},Modifier.weight(1f),outline=true)}
                                if(state.busy||state.listening)Text("أوقف الاستقبال أو النقل قبل تغيير الاسم.",Modifier.padding(top=10.dp),fontSize=10.sp,color=p.muted)
                            }
                            EzSection("المظهر")
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(p.soft).padding(5.dp)){listOf("light" to "فاتح","dark" to "داكن","system" to "حسب النظام").forEach{(id,label)->Box(Modifier.weight(1f).clip(RoundedCornerShape(19.dp)).background(if(prefs.theme==id)EzTokens.Ink else Color.Transparent).ezPress({onSave(prefs.copy(theme=id))}).heightIn(min=48.dp),contentAlignment=Alignment.Center){Text(label,fontSize=12.sp,color=if(prefs.theme==id)Color.White else p.ink)}}}
                            SettingsRow("العربية","واجهة من اليمين إلى اليسار",R.drawable.ic_check,{feedback="العربية هي اللغة الحالية"})
                            Row(Modifier.fillMaxWidth().ezPress({onSave(prefs.copy(quiet=!prefs.quiet))}).padding(vertical=22.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("تقليل الحركة",fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("يحترم أيضًا تفضيل النظام",fontSize=10.sp,color=p.muted)};Switch(checked=prefs.quiet,onCheckedChange={onSave(prefs.copy(quiet=it))},colors=SwitchDefaults.colors(checkedTrackColor=EzTokens.Accent))}
                            HorizontalDivider(color=p.line);Text("يُحفظ الاسم والصورة والمظهر محليًا على جهازك. لا تُرسل معلوماتك إلى أي خدمة.",Modifier.padding(top=18.dp),fontSize=12.sp,color=p.muted,lineHeight=23.sp)
                            EzButton("حول الخصوصية وEZ",{sheet="privacy"},Modifier.fillMaxWidth().padding(top=24.dp),outline=true)
                            if(feedback.isNotBlank()){Spacer(Modifier.height(16.dp));EzNotice(feedback)}
                        }
                    }
                }}
            }
        }
        if(screen=="files")Row(Modifier.align(Alignment.BottomCenter).padding(start=24.dp,end=24.dp,bottom=bottom+107.dp).fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(p.surface).border(1.dp,p.line,RoundedCornerShape(25.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Column(Modifier.weight(1f)){Text("${if(selected&&state.picked!=null)1 else 0} ملفات محددة",fontSize=11.sp);Text(humanSize(if(selected)state.picked?.metadata?.size?:0 else 0),fontSize=10.sp,color=p.muted)}
            EzButton("إرسال الملفات",{route("discover");onDiscover()},enabled=selected&&state.picked!=null&&!state.busy,icon=R.drawable.ic_arrow_up_left)
        }
        if((state.busy||state.listening)&&screen !in listOf("transfer","confirm","receive","discover")){
            Box(Modifier.align(Alignment.BottomCenter).padding(horizontal=24.dp).padding(bottom=bottom+if(screen=="files")195.dp else 100.dp).clip(RoundedCornerShape(18.dp)).background(EzTokens.CategoryPurple).ezPress({route(if(state.listening&&!state.busy)"receive" else "transfer")}).padding(horizontal=12.dp,vertical=8.dp)){Text(if(state.listening&&!state.busy)"العودة للاستقبال · بانتظار طلب" else "العودة للنقل · ${state.status}",fontSize=10.sp,color=EzTokens.Ink)}
        }
        EzNavigation(tab,{i->route(listOf("home","files","history","settings")[i])},Modifier.align(Alignment.BottomCenter).padding(horizontal=28.dp).padding(bottom=bottom+12.dp))
    }
    if(sheet.isNotEmpty())ModalBottomSheet(onDismissRequest={sheet=""},containerColor=p.surface,shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp)){
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text(when(sheet){"share"->"مشاركة تطبيق EZ";"clear-history"->"مسح السجل؟";"cancel-transfer"->"إلغاء النقل؟";else->"بسيطة. وقريبة. وخاصة."},fontSize=23.sp,fontWeight=FontWeight.Bold)
            when(sheet){
                "share"->{Text("شارك التطبيق مع أصدقائك",fontSize=13.sp,color=p.muted);SheetOption(R.drawable.ic_link,"مشاركة رابط التطبيق","قريبًا · لم يُنشر رابط توزيع بعد"){feedback="لا يوجد رابط منشور موثوق للتطبيق حتى الآن."};SheetOption(R.drawable.ic_android_logo,"مشاركة التطبيق بدون إنترنت","قريبًا · مشاركة APK موثوق قابل للتوزيع"){feedback="مشاركة APK غير منفّذة في هذه النسخة. إرسال ملفاتك مستقل عن مشاركة EZ."};EzNotice("لا يوجد رابط Play Store مفترض، ولا مشاركة APK فعلية من هذا الزر.");if(feedback.isNotBlank())EzNotice(feedback)}
                "clear-history"->{Text("سيُحذف سجل العمليات فقط. لن تُحذف أي ملفات من الجهاز.",fontSize=13.sp);EzButton("مسح السجل فقط",{onClearHistory();sheet=""},Modifier.fillMaxWidth());EzButton("الاحتفاظ بالسجل",{sheet=""},Modifier.fillMaxWidth(),outline=true)}
                "cancel-transfer"->{Text("سيتوقف النقل وتبقى الملفات غير المكتملة غير ناجحة. الاستئناف غير متاح في Alpha.",fontSize=13.sp);EzButton("تأكيد إلغاء النقل",{onCancel();sheet=""},Modifier.fillMaxWidth());EzButton("متابعة النقل",{sheet=""},Modifier.fillMaxWidth(),outline=true)}
                else->{EzIcon(R.drawable.ic_shield_check,size=40.dp);Text("يُنقل ملفك محليًا بين الهاتفين، دون حساب أو خادم سحابي. قارن رمز التحقق يدويًا قبل الموافقة على الطرفين.",fontSize=13.sp);EzNotice("Alpha تجريبية. بروتوكول الاقتران جديد ويحتاج مراجعة أمنية مستقلة واختبارًا على هاتفين.");Text("Wi-Fi Direct وQR والنقل المتعدد والإيقاف المؤقت والاستئناف ومشاركة التطبيق لاحقًا.",fontSize=12.sp,color=p.muted);EzButton("فهمت",{sheet=""},Modifier.fillMaxWidth())}
            }
            TextButton(onClick={sheet=""},modifier=Modifier.fillMaxWidth()){Text("إغلاق",color=EzTokens.Accent)}
        }
    }
}
