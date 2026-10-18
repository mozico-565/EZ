package com.ezshare.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EZViewModel(app:Application):AndroidViewModel(app){
    val controller=(app as EZApplication).transfers;val ui=controller.ui
    val catalog=FileCatalog(app)
    init{catalog.refresh()}
    private val store=EzLocalStore(app)
    private val _preferences=MutableStateFlow(store.read());val preferences=_preferences.asStateFlow()
    private val _history=MutableStateFlow(store.history());val history=_history.asStateFlow()
    private val historyListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener{_,key->if(key=="history")_history.value=store.history()}
    init{store.observeHistory(historyListener)}
    fun save(p:EzPreferences){store.save(p);_preferences.value=p}
    override fun onCleared(){store.stopObservingHistory(historyListener);catalog.close();super.onCleared()}
    fun clearHistory(){_history.value=emptyList();store.saveHistory(emptyList())}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();setContent{EZScreen()}}}
@Composable fun EZScreen(vm:EZViewModel=viewModel()){
    val state by vm.ui.collectAsStateWithLifecycle();val prefs by vm.preferences.collectAsStateWithLifecycle();val history by vm.history.collectAsStateWithLifecycle()
    val controller=vm.controller;val context=LocalContext.current
    var networkTick by remember{mutableIntStateOf(0)}
    LaunchedEffect(state.listening){while(state.listening){kotlinx.coroutines.delay(2000);networkTick++}}
    val addresses=remember(networkTick,state.port){controller.localAddresses()}
    val catalog by vm.catalog.state.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    fun message(s:String){Toast.makeText(context,s,Toast.LENGTH_LONG).show()}
    var sharing by remember{mutableStateOf(false)}
    fun launchShare(action:suspend ()->Unit){if(sharing){message("جارٍ تجهيز المشاركة؛ انتظر قليلًا");return};sharing=true;scope.launch{try{action()}catch(e:Exception){message(e.message?:"تعذّرت المشاركة")}finally{sharing=false}}}
    fun toggle(file:LocalFile){if(controller.ui.value.busy)return;if(file.unavailable.isNotBlank()){message(file.unavailable);return};scope.launch{
        try{val current=controller.ui.value.selected;val existing=current.firstOrNull{it.uri==file.uri||file.packageName.isNotBlank()&&it.uri.lastPathSegment?.startsWith(file.packageName+"-")==true}
            if(existing!=null)controller.select(current-existing) else {val picked=if(file.packageName.isNotBlank())kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){val apk=ApkSharing.export(context,file.packageName);PickedFile(android.net.Uri.fromFile(apk),file.metadata.copy(size=apk.length()))} else PickedFile(file.uri,file.metadata);controller.select(controller.ui.value.selected+picked)}
        }catch(e:Exception){message(e.message?:"تعذّر اختيار الملف")}
    }}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){vm.catalog.import(it)}
    val browseFolders=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){it?.let(vm.catalog::openTree)}
    val mediaPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){vm.catalog.refresh()}
    val folders=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){it?.let(controller::folder)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(!it)Toast.makeText(context,"يمكن متابعة النقل؛ قد يُخفي النظام الإشعار",Toast.LENGTH_LONG).show()}
    fun notifyPermission(){if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}
    val dark=prefs.theme=="dark"||prefs.theme=="system"&&isSystemInDarkTheme()
    DisposableEffect(dark){
        (context as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),navigationBarStyle=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT))
        onDispose{}
    }
    val systemQuiet=runCatching{Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f}.getOrDefault(false)
    EzTheme(dark,prefs.quiet||systemQuiet){EzApp(state,prefs,history,addresses,
        onPick={picker.launch(arrayOf("*/*"))},onFolder={folders.launch(null)},onDiscover=controller::startDiscovery,
        onSend={notifyPermission();controller.send(it)},onReceive={notifyPermission();controller.receive()},onCancel=controller::cancel,onDecide=controller::decide,onSave=vm::save,onClearHistory=vm::clearHistory,
        catalog=catalog,onToggle=::toggle,onMany={files,select->
            if(select){val combined=(state.selected+files.map{PickedFile(it.uri,it.metadata)}).distinctBy{it.uri};if(combined.size>256)message("حد العملية 256 ملفًا؛ قلّل التحديد") else scope.launch{try{val prepared=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){files.map{f->if(f.packageName.isBlank())PickedFile(f.uri,f.metadata) else {val apk=ApkSharing.export(context,f.packageName);PickedFile(android.net.Uri.fromFile(apk),f.metadata.copy(size=apk.length()))}}};controller.select((controller.ui.value.selected+prepared).distinctBy{it.uri})}catch(e:Exception){message(e.message?:"تعذّر تجهيز التطبيقات")}}}else controller.select(state.selected.filter{f->files.none{it.uri==f.uri||it.packageName.isNotBlank()&&f.uri.lastPathSegment?.startsWith(it.packageName+"-")==true}})
        },onBrowseFolder={browseFolders.launch(null)},onMediaPermission={mediaPermission.launch(vm.catalog.permissions())},onCatalogRefresh=vm.catalog::refresh,onOpenDirectory=vm.catalog::open,onCatalogUp=vm.catalog::up,onDeviceFiles=vm.catalog::device,
        permissionNote=vm.catalog.permissionNote(),onScan={notifyPermission();controller.scan(it)},
        onPreview={f->runCatching{if(f.packageName.isNotBlank())message(f.unavailable.ifBlank{f.metadata.name+" · APK كامل قابل للمشاركة"}) else context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(f.uri,f.metadata.mime).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{message("لا يوجد تطبيق مناسب للمعاينة")}},
        onShareApk={launchShare{message("جارٍ تجهيز APK كامل للمشاركة…");val apk=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){ApkSharing.export(context)};ApkSharing.share(context,apk)}},
        onShareLink={launchShare{message("جارٍ التحقق من رابط الإصدار الرسمي…");val link=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){ApkSharing.officialLink(BuildConfig.VERSION_NAME)};context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,link),"مشاركة رابط EZ"))}})}
}
fun humanSize(bytes:Long):String { val text=when{bytes<0->"غير معروف";bytes>=1_000_000_000->"%.2f GB".format(java.util.Locale.US,bytes/1e9);bytes>=1_000_000->"%.1f MB".format(java.util.Locale.US,bytes/1e6);bytes>=1000->"%.0f KB".format(java.util.Locale.US,bytes/1e3);else->"$bytes B"};return if(bytes<0)text else "\u2066$text\u2069"}
