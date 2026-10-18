package com.ezshare.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable fun NativeFiles(catalog:CatalogState,chosen:List<PickedFile>,initialCategory:String,onToggle:(LocalFile)->Unit,onMany:(List<LocalFile>,Boolean)->Unit,onImport:()->Unit,onFolder:()->Unit,onPermissions:()->Unit,onRefresh:()->Unit,onOpen:(LocalFile)->Unit,onUp:()->Unit,onDevice:()->Unit,onPreview:(LocalFile)->Unit,permissionNote:String){
    var query by rememberSaveable{mutableStateOf("")};var category by rememberSaveable(initialCategory){mutableStateOf(initialCategory)};var sort by rememberSaveable{mutableStateOf("الاسم")};var descending by rememberSaveable{mutableStateOf(false)};var grid by rememberSaveable{mutableStateOf(false)};var page by rememberSaveable{mutableIntStateOf(0)}
    val p=LocalEzPalette.current
    EzBadge("ملفات جهازك · وصول مصرح به");Text("أشياء تستحق\nالمشاركة.",style=MaterialTheme.typography.headlineLarge);Spacer(Modifier.height(20.dp));StorageSummary()
    SearchField(query,{query=it;page=0},"ابحث في الملفات")
    FlowRow(Modifier.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){browserCategories.forEach{c->FilterChip(c,c==category,{category=c;page=0})}}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ChoiceMenu(sort,listOf("الاسم","الحجم","التاريخ")){sort=it;page=0};TextButton(onClick={descending=!descending}){Text(if(descending)"تنازلي ↓" else "تصاعدي ↑",fontSize=11.sp)};TextButton(onClick={grid=!grid}){Text(if(grid)"قائمة" else "شبكة",fontSize=11.sp)}}
    Text(catalog.location,fontSize=11.sp,color=p.muted)
    if(catalog.canGoUp)Row{TextButton(onClick=onUp){Text("المجلد السابق")};TextButton(onClick=onDevice){Text("ملفات الجهاز")}}
    val source=(catalog.files+chosen.filter{f->catalog.files.none{it.uri==f.uri}}.map{LocalFile(it.uri,it.metadata)}).filter{f->(f.directory||category=="الكل"||f.category==category)&&f.metadata.name.contains(query.trim(),true)}
    val comparator=when(sort){"الحجم"->compareBy<LocalFile>{it.metadata.size};"التاريخ"->compareBy{it.modified};else->compareBy{it.metadata.name.lowercase()}}
    val filtered=source.sortedWith(if(descending)comparator.reversed() else comparator).sortedBy{!it.directory};val keys=chosen.map{it.uri.toString()}.toSet()+catalog.files.filter{f->f.packageName.isNotBlank()&&chosen.any{it.uri.lastPathSegment?.startsWith(f.packageName+"-")==true}}.map{it.key};val selectable=filtered.filter{!it.directory&&it.unavailable.isBlank()};val all=selectable.isNotEmpty()&&selectable.all{it.key in keys}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={onMany(selectable,!all)},enabled=selectable.isNotEmpty()){Text(if(all)"إلغاء تحديد الظاهر" else "تحديد كل الظاهر",fontSize=12.sp)};Text("${chosen.size} محدد · ${humanSize(if(chosen.any{it.metadata.size<0})-1 else chosen.sumOf{it.metadata.size})}",fontSize=11.sp)}
    if(catalog.loading){LinearProgressIndicator(Modifier.fillMaxWidth(),color=EzTokens.Accent);Text("جارٍ قراءة الملفات…",fontSize=12.sp)}
    if(catalog.error.isNotBlank())EzNotice(catalog.error,true)
    val pages=((filtered.size+39)/40).coerceAtLeast(1);val shown=filtered.drop(page.coerceAtMost(pages-1)*40).take(40)
    if(!catalog.loading&&shown.isEmpty())EzEmpty(if(query.isNotBlank())"لا توجد نتائج" else "هذه الفئة فارغة","قد تحتاج إذن الوسائط أو اختيار مجلد للمستندات.",action=if(query.isNotBlank())"مسح البحث" else "إذن الوسائط",onAction={if(query.isNotBlank())query="" else onPermissions()})
    if(grid){shown.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{f->Column(Modifier.weight(1f).padding(vertical=4.dp).clip(RoundedCornerShape(22.dp)).background(if(f.key in keys)p.lavender.copy(alpha=.25f) else p.soft).ezPress({if(f.directory)onOpen(f) else onToggle(f)}).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){FileThumbnail(f,72);Text(f.metadata.name,maxLines=2,overflow=TextOverflow.Ellipsis,fontSize=11.sp);Text(if(f.directory)"مجلد" else humanSize(f.metadata.size),fontSize=10.sp,color=p.muted);if(!f.directory)Checkbox(f.key in keys,{onToggle(f)},enabled=f.unavailable.isBlank())}};if(row.size==1)Spacer(Modifier.weight(1f))}}}
    else shown.forEach{f->Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(22.dp)).background(if(f.key in keys)p.lavender.copy(alpha=.25f) else p.soft).ezPress({if(f.directory)onOpen(f) else onToggle(f)}).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        FileThumbnail(f,44);Column(Modifier.weight(1f)){Text(f.metadata.name,fontSize=12.sp,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(f.directory)"مجلد" else humanSize(f.metadata.size)+(if(f.duration>0)" · ${f.duration/60000}:${"%02d".format(f.duration/1000%60)}" else ""),fontSize=10.sp,color=p.muted);if(f.modified>0)Text(dateLabel(f.modified),fontSize=9.sp,color=p.muted);if(f.unavailable.isNotBlank())Text(f.unavailable,fontSize=9.sp,color=p.muted)}
        if(f.directory)EzIcon(R.drawable.ic_arrow_right,size=20.dp) else{IconButton(onClick={onPreview(f)},modifier=Modifier.size(44.dp)){EzIcon(R.drawable.ic_file_text,"معاينة ${f.metadata.name}",20.dp)};Checkbox(f.key in keys,{onToggle(f)},enabled=f.unavailable.isBlank())}
    }}
    if(pages>1)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={page=(page-1).coerceAtLeast(0)},enabled=page>0){Text("السابق")};Text("${page+1} / $pages",fontSize=12.sp);TextButton(onClick={page++},enabled=page<pages-1){Text("التالي")}}
    Spacer(Modifier.height(16.dp));EzButton("اختيار مجلد للمستندات",onFolder,Modifier.fillMaxWidth(),outline=true,icon=R.drawable.ic_folder_open)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton(onClick=onImport){Text("إضافة ملفات أخرى",fontSize=11.sp)};TextButton(onClick=onPermissions){Text("أذونات الوسائط",fontSize=11.sp)};TextButton(onClick=onRefresh){Text("تحديث",fontSize=11.sp)}}
    EzNotice(permissionNote)
}
@Composable fun FileThumbnail(file:LocalFile,size:Int){val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,file.key,file.modified){value=withContext(Dispatchers.IO){runCatching{if(file.packageName.isNotBlank())context.packageManager.getApplicationIcon(file.packageName).toBitmap(96,96) else if(file.category in listOf("الصور","الفيديوهات"))context.contentResolver.loadThumbnail(file.uri,Size(96,96),null) else null}.getOrNull()}}
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(EzTokens.CategoryPurple),contentAlignment=Alignment.Center){if(bitmap!=null)Image(bitmap!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop) else EzIcon(if(file.directory)R.drawable.ic_folder_open else fileIcon(file.category),size=(size*.55f).dp,tint=EzTokens.Ink)}
}
