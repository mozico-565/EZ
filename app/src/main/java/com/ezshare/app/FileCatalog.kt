package com.ezshare.app

import android.Manifest
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.ezshare.core.Metadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

val browserCategories=listOf("الكل","الصور","الفيديوهات","التطبيقات","المستندات","الصوتيات","ملفات أخرى")
data class LocalFile(val uri:Uri,val metadata:Metadata,val modified:Long=0,val duration:Long=0,val packageName:String="",val unavailable:String="",val directory:Boolean=false){val key get()=uri.toString();val category get()=if(packageName.isNotBlank())"التطبيقات" else fileCategory(metadata.mime,metadata.name)}
data class CatalogState(val files:List<LocalFile> = emptyList(),val loading:Boolean=false,val error:String="",val location:String="ملفات الجهاز المتاحة",val canGoUp:Boolean=false)
class FileCatalog(private val app:Application){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO);private val _state=MutableStateFlow(CatalogState());val state=_state.asStateFlow()
    private var task:Job?=null;private var tree:Uri?=null;private val parents=mutableListOf<Pair<Uri,String>>();private var extras=listOf<LocalFile>()
    fun import(uris:List<Uri>){scope.launch{extras=(extras+uris.mapNotNull{u->runCatching{app.contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION)};runCatching{app.contentResolver.query(u,arrayOf("_display_name","_size"),null,null,null)?.use{c->if(!c.moveToFirst())return@use null;LocalFile(u,Metadata(c.getString(0)?:"file",app.contentResolver.getType(u)?:"application/octet-stream",if(c.isNull(1))-1 else c.getLong(1)))}}.getOrNull()}).distinctBy{it.key};refresh()}}
    fun openTree(uri:Uri){runCatching{app.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};tree=uri;parents.clear();parents+=DocumentsContract.buildDocumentUriUsingTree(uri,DocumentsContract.getTreeDocumentId(uri)) to "مجلد مصرح به";refresh()}
    fun open(file:LocalFile){if(file.directory){parents+=file.uri to file.metadata.name;refresh()}}
    fun up(){if(parents.isNotEmpty())parents.removeAt(parents.lastIndex);refresh()}
    fun device(){parents.clear();refresh()}
    fun refresh(){task?.cancel();task=scope.launch{_state.update{it.copy(loading=true,error="")};try{val files=if(parents.isNotEmpty())documents() else media()+apps()+extras;ensureActive();_state.value=CatalogState(files.distinctBy{it.key},location=parents.lastOrNull()?.second?:"ملفات الجهاز المتاحة",canGoUp=parents.isNotEmpty())}catch(e:Exception){if(e is CancellationException)throw e;_state.update{it.copy(loading=false,error="تعذّر قراءة بعض الملفات. تحقق من الأذونات أو أعد اختيار المجلد.")}}}}
    private fun documents():List<LocalFile>{val root=tree?:return emptyList();val parent=parents.last().first;val children=DocumentsContract.buildChildDocumentsUriUsingTree(root,DocumentsContract.getDocumentId(parent));return app.contentResolver.query(children,arrayOf("document_id","_display_name","mime_type","_size","last_modified"),null,null,null)?.use{c->buildList{while(c.moveToNext()){val mime=c.getString(2)?:"application/octet-stream";add(LocalFile(DocumentsContract.buildDocumentUriUsingTree(root,c.getString(0)),Metadata(c.getString(1)?:"file",mime,if(c.isNull(3))-1 else c.getLong(3)),c.getLong(4),directory=mime==DocumentsContract.Document.MIME_TYPE_DIR))}}}?:emptyList()}
    private fun media():List<LocalFile>{val result=mutableListOf<LocalFile>();val sources=listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,MediaStore.Video.Media.EXTERNAL_CONTENT_URI,MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        sources.forEachIndexed{index,uri->try{val projection=mutableListOf("_id","_display_name","mime_type","_size","date_modified");if(index>0)projection+="duration";app.contentResolver.query(uri,projection.toTypedArray(),null,null,"date_modified DESC")?.use{c->while(c.moveToNext()){val name=c.getString(1)?:"file";result+=LocalFile(ContentUris.withAppendedId(uri,c.getLong(0)),Metadata(name,c.getString(2)?:"application/octet-stream",c.getLong(3)),c.getLong(4)*1000,if(index>0)c.getLong(5) else 0)}}}catch(_:SecurityException){/* Narrow permissions may be partial or denied. Authorized sources still work. */}}
        return result
    }
    private fun apps():List<LocalFile>{val pm=app.packageManager;val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return (pm.queryIntentActivities(intent,0).map{it.activityInfo.packageName}+app.packageName).distinct().mapNotNull{pkg->runCatching{val info=pm.getPackageInfo(pkg,0);val a=info.applicationInfo?:return@runCatching null;val f=File(a.sourceDir);LocalFile(Uri.fromFile(f),Metadata(a.loadLabel(pm).toString()+".apk",ApkSharing.APK_MIME,f.length()),f.lastModified(),packageName=pkg,unavailable=if(!a.splitSourceDirs.isNullOrEmpty()||!info.splitNames.isNullOrEmpty())"حزمة مجزأة؛ اختر APK كاملًا من مصدر موثوق" else "")}.getOrNull()}}
    fun permissions():Array<String> = if(Build.VERSION.SDK_INT>=33)buildList{add(Manifest.permission.READ_MEDIA_IMAGES);add(Manifest.permission.READ_MEDIA_VIDEO);add(Manifest.permission.READ_MEDIA_AUDIO);if(Build.VERSION.SDK_INT>=34)add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)}.toTypedArray() else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun permissionNote():String {val permissions=permissions();return if(permissions.any{app.checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED})"يعرض EZ الملفات المسموح بها فقط. امنح إذن الوسائط، أو اختر مجلدًا للمستندات. التطبيقات الظاهرة تخضع لقيود Android؛ الحزم المجزأة لا تُصدّر كملف واحد." else "المستندات والملفات غير المفهرسة تحتاج مجلدًا تختاره. لا يطلب EZ وصولًا شاملًا للتخزين."}
}
