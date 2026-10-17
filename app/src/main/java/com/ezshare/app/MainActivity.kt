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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EZViewModel(app:Application):AndroidViewModel(app){
    val controller=(app as EZApplication).transfers;val ui=controller.ui
    private val store=EzLocalStore(app)
    private val _preferences=MutableStateFlow(store.read());val preferences=_preferences.asStateFlow()
    private val _history=MutableStateFlow(store.history());val history=_history.asStateFlow()
    private var lastResult:com.ezshare.core.Result?=null;private var lastError:String?=null
    fun save(p:EzPreferences){store.save(p);_preferences.value=p}
    fun observe(s:TransferUi){
        if(s.error==null)lastError=null
        s.result?.let{r->if(r!==lastResult){lastResult=r;lastError=null;add(HistoryEntry(name=r.metadata.name,bytes=r.bytes,total=r.metadata.size,duration=r.elapsedNanos/1e9,sha=r.sha256,peer=s.peer,incoming=s.incoming,status="completed",savedAt=r.savedAt))}}
        if(s.error!=null&&s.error!=lastError&&s.filename.isNotBlank()&&s.progress!=null){lastError=s.error;val p=s.progress;add(HistoryEntry(name=s.filename,bytes=p.bytes,total=p.total,duration=p.elapsedNanos/1e9,peer=s.peer,incoming=s.incoming,status=if(s.status=="تم الإلغاء")"cancelled" else "failed"))}
    }
    private fun add(r:HistoryEntry){_history.value=(listOf(r)+_history.value).take(100);store.saveHistory(_history.value)}
    fun clearHistory(){_history.value=emptyList();store.saveHistory(emptyList())}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();setContent{EZScreen()}}}
@Composable fun EZScreen(vm:EZViewModel=viewModel()){
    val state by vm.ui.collectAsStateWithLifecycle();val prefs by vm.preferences.collectAsStateWithLifecycle();val history by vm.history.collectAsStateWithLifecycle()
    val controller=vm.controller;val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(controller::pick)}
    val folders=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){it?.let(controller::folder)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(!it)Toast.makeText(context,"يمكن متابعة النقل؛ قد يُخفي النظام الإشعار",Toast.LENGTH_LONG).show()}
    fun notifyPermission(){if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}
    val dark=prefs.theme=="dark"||prefs.theme=="system"&&isSystemInDarkTheme()
    DisposableEffect(dark){
        (context as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),navigationBarStyle=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT))
        onDispose{}
    }
    val systemQuiet=runCatching{Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f}.getOrDefault(false)
    LaunchedEffect(state.result,state.error){vm.observe(state)}
    EzTheme(dark,prefs.quiet||systemQuiet){EzApp(state,prefs,history,controller.localAddresses(),
        onPick={picker.launch(arrayOf("*/*"))},onFolder={folders.launch(null)},onDiscover=controller::startDiscovery,
        onSend={notifyPermission();controller.send(it)},onReceive={notifyPermission();controller.receive()},onCancel=controller::cancel,onDecide=controller::decide,onSave=vm::save,onClearHistory=vm::clearHistory)}
}
fun humanSize(bytes:Long):String { val text=when{bytes<0->"غير معروف";bytes>=1_000_000_000->"%.2f GB".format(java.util.Locale.US,bytes/1e9);bytes>=1_000_000->"%.1f MB".format(java.util.Locale.US,bytes/1e6);bytes>=1000->"%.0f KB".format(java.util.Locale.US,bytes/1e3);else->"$bytes B"};return if(bytes<0)text else "\u2066$text\u2069"}
