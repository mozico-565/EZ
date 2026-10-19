package com.ezshare.app

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TransferService:Service(){
    override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("ez-transfer","نقل ملفات EZ",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val cancel=PendingIntent.getService(this,1,Intent(this,TransferService::class.java).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(this,"ez-transfer").setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle("EZ · مشاركة محلية").setContentText("طلب اتصال أو نقل ملفات جارٍ. افتح EZ للموافقة أو المتابعة.").setOngoing(true).setContentIntent(open).addAction(0,"إلغاء",cancel).build()
        startForeground(7,n)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {if(intent?.action=="cancel"){(application as EZApplication).transfers.cancel();stopSelf()};return START_NOT_STICKY}
    override fun onBind(intent:Intent?):IBinder?=null
    // Intentional stop/restart may destroy the old service after a new session starts.
    // Cancellation belongs to the explicit notification action / timeout, not stale teardown.
    override fun onDestroy(){super.onDestroy()}
    override fun onTimeout(startId:Int,fgsType:Int){(application as EZApplication).transfers.cancel();stopSelf()}
    companion object {fun start(context:Context){context.startForegroundService(Intent(context,TransferService::class.java))}}
}
