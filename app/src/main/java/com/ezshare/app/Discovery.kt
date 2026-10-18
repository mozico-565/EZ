package com.ezshare.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.ArrayDeque

data class Peer(val name:String,val address:String)
@Suppress("DEPRECATION")
class Discovery(context:Context,private val peers:(List<Peer>)->Unit,private val status:(String)->Unit) {
    private val nsd=context.getSystemService(NsdManager::class.java)
    private var registration:NsdManager.RegistrationListener?=null
    private var discovery:NsdManager.DiscoveryListener?=null
    private val found=linkedMapOf<String,Peer>()
    private val revisions=mutableMapOf<String,Long>()
    private val pending=ArrayDeque<Pair<NsdServiceInfo,Long>>()
    private var generation=0L
    private var resolving=false
    @Synchronized fun advertise(name:String,port:Int){stopAdvertising();val listener=object:NsdManager.RegistrationListener{
        override fun onServiceRegistered(info:NsdServiceInfo){}
        override fun onRegistrationFailed(info:NsdServiceInfo,error:Int){synchronized(this@Discovery){if(registration===this)status("تعذّر الإعلان؛ استخدم عنوان الاتصال اليدوي")}}
        override fun onServiceUnregistered(info:NsdServiceInfo){}
        override fun onUnregistrationFailed(info:NsdServiceInfo,error:Int){}
    };registration=listener
        try{nsd.registerService(NsdServiceInfo().apply{serviceName="EZ-${name.take(24)}";serviceType="_ezshare._tcp.";setPort(port);setAttribute("version","2")},NsdManager.PROTOCOL_DNS_SD,listener)}catch(_:Exception){status("الإعلان غير متاح؛ الاتصال اليدوي يعمل")}
    }
    @Synchronized fun discover(){stopDiscovery();val epoch=generation
        status("جارٍ البحث على شبكة Wi-Fi المحلية…")
        val listener=object:NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type:String){synchronized(this@Discovery){if(epoch==generation)status("اختر جهازًا أو أدخل عنوانه يدويًا")}}
            override fun onDiscoveryStopped(type:String){}
            override fun onStartDiscoveryFailed(type:String,error:Int){synchronized(this@Discovery){if(epoch==generation)status("تعذّر البحث؛ أدخل العنوان اليدوي")}}
            override fun onStopDiscoveryFailed(type:String,error:Int){}
            override fun onServiceLost(info:NsdServiceInfo){synchronized(this@Discovery){if(epoch!=generation)return
                revisions[info.serviceName]=(revisions[info.serviceName]?:0)+1
                found.remove(info.serviceName);peers(found.values.toList())
            }}
            override fun onServiceFound(info:NsdServiceInfo){synchronized(this@Discovery){if(epoch!=generation||info.serviceType!="_ezshare._tcp.")return
                val revision=(revisions[info.serviceName]?:0)+1;revisions[info.serviceName]=revision
                pending.add(info to revision);resolveNext(epoch)
            }}
        };discovery=listener;try{nsd.discoverServices("_ezshare._tcp.",NsdManager.PROTOCOL_DNS_SD,listener)}catch(_:Exception){status("البحث غير متاح؛ الاتصال اليدوي متاح")}
    }
    // Legacy Android permits only one resolve request at a time. Do not let an old
    // callback repopulate a stopped search or resurrect an obsolete IP:PORT.
    @Synchronized private fun resolveNext(epoch:Long){
        if(epoch!=generation||resolving)return
        val item=pending.poll()?:return
        val (service,revision)=item
        if(revisions[service.serviceName]!=revision){resolveNext(epoch);return}
        resolving=true
        val callback=object:NsdManager.ResolveListener{
            override fun onResolveFailed(info:NsdServiceInfo,error:Int){synchronized(this@Discovery){if(epoch!=generation)return
                resolving=false;status("تعذّر حل جهاز؛ استخدم الاتصال اليدوي أو أعد البحث");resolveNext(epoch)
            }}
            override fun onServiceResolved(info:NsdServiceInfo){synchronized(this@Discovery){if(epoch!=generation)return
                resolving=false
                val host=info.host?.hostAddress
                if(revisions[service.serviceName]==revision&&host!=null&&!host.contains(':')&&info.port in 1024..65535){
                    found[service.serviceName]=Peer(service.serviceName,"$host:${info.port}");peers(found.values.toList())
                }
                resolveNext(epoch)
            }}
        }
        try{nsd.resolveService(service,callback)}catch(_:Exception){resolving=false;status("تعذّر حل جهاز؛ أعد البحث");resolveNext(epoch)}
    }
    @Synchronized fun stopDiscovery(){generation++;pending.clear();resolving=false;revisions.clear();found.clear()
        val old=discovery;discovery=null;old?.let{runCatching{nsd.stopServiceDiscovery(it)}};peers(emptyList())
    }
    @Synchronized fun stopAdvertising(){val old=registration;registration=null;old?.let{runCatching{nsd.unregisterService(it)}}}
}
