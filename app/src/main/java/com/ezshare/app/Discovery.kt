package com.ezshare.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.concurrent.CopyOnWriteArrayList

data class Peer(val name:String,val address:String)
@Suppress("DEPRECATION")
class Discovery(context:Context,private val peers:(List<Peer>)->Unit,private val status:(String)->Unit) {
    private val nsd=context.getSystemService(NsdManager::class.java)
    private var registration:NsdManager.RegistrationListener?=null
    private var discovery:NsdManager.DiscoveryListener?=null
    private val found=CopyOnWriteArrayList<Peer>()
    fun advertise(name:String,port:Int){stopAdvertising();val listener=object:NsdManager.RegistrationListener{
        override fun onServiceRegistered(info:NsdServiceInfo){}
        override fun onRegistrationFailed(info:NsdServiceInfo,error:Int){status("تعذّر الإعلان؛ استخدم عنوان الاتصال اليدوي")}
        override fun onServiceUnregistered(info:NsdServiceInfo){}
        override fun onUnregistrationFailed(info:NsdServiceInfo,error:Int){}
    };registration=listener
        try{nsd.registerService(NsdServiceInfo().apply{serviceName="EZ-${name.take(24)}";serviceType="_ezshare._tcp.";setPort(port);setAttribute("version","2")},NsdManager.PROTOCOL_DNS_SD,listener)}catch(_:Exception){status("الإعلان غير متاح؛ الاتصال اليدوي يعمل")}
    }
    fun discover(){stopDiscovery();found.clear();peers(emptyList());status("جارٍ البحث على شبكة Wi-Fi المحلية…")
        val listener=object:NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type:String){status("اختر جهازًا أو أدخل عنوانه يدويًا")}
            override fun onDiscoveryStopped(type:String){}
            override fun onStartDiscoveryFailed(type:String,error:Int){status("تعذّر البحث؛ أدخل العنوان اليدوي")}
            override fun onStopDiscoveryFailed(type:String,error:Int){}
            override fun onServiceLost(info:NsdServiceInfo){found.removeAll{it.name==info.serviceName};peers(found.toList())}
            override fun onServiceFound(info:NsdServiceInfo){if(info.serviceType!="_ezshare._tcp.")return
                nsd.resolveService(info,object:NsdManager.ResolveListener {
                    override fun onResolveFailed(info:NsdServiceInfo,error:Int){status("تعذّر حل جهاز؛ استخدم الاتصال اليدوي أو أعد البحث")}
                    override fun onServiceResolved(info:NsdServiceInfo){val host=info.host?.hostAddress?:return
                        if(host.contains(':'))return
                        val peer=Peer(info.serviceName,"$host:${info.port}");found.removeAll{it.name==peer.name};found.add(peer);peers(found.toList())
                    }
                })
            }
        };discovery=listener;try{nsd.discoverServices("_ezshare._tcp.",NsdManager.PROTOCOL_DNS_SD,listener)}catch(_:Exception){status("البحث غير متاح؛ الاتصال اليدوي متاح")}
    }
    fun stopDiscovery(){discovery?.let{runCatching{nsd.stopServiceDiscovery(it)}};discovery=null}
    fun stopAdvertising(){registration?.let{runCatching{nsd.unregisterService(it)}};registration=null}
}
