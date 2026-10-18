package com.ezshare.core

import java.net.URI
import java.util.Locale

data class PairingQr(val host:String,val port:Int,val nonce:String,val expires:Long){
    val address get()="$host:$port"
    fun encode()="ezpair://v2?host=$host&port=$port&nonce=$nonce&expires=$expires"
    companion object{
        fun decode(raw:String,now:Long=System.currentTimeMillis()/1000):PairingQr{
            requireProtocol(raw.length in 1..512,"Invalid QR size");val u=try{URI(raw)}catch(e:Exception){throw ProtocolException("Invalid QR")}
            requireProtocol(u.scheme=="ezpair"&&u.rawAuthority=="v2"&&u.rawPath.isNullOrEmpty()&&u.rawFragment==null,"Unsupported QR scheme or protocol")
            val pairs=u.rawQuery?.split('&')?.map{it.split('=',limit=2)}?:emptyList();requireProtocol(pairs.size==4&&pairs.all{it.size==2},"Invalid QR fields")
            val fields=pairs.associate{it[0] to it[1]};requireProtocol(fields.keys==setOf("host","port","nonce","expires"),"Duplicate or unknown QR fields")
            val host=fields.getValue("host");requireProtocol(host.matches(Regex("[0-9]{1,3}(\\.[0-9]{1,3}){3}")),"Numeric local IPv4 required");val octets=host.split('.').map{it.toInt()};requireProtocol(octets.all{it in 0..255},"Invalid address")
            requireProtocol(octets[0]==10||octets[0]==192&&octets[1]==168||octets[0]==172&&octets[1] in 16..31||octets[0]==169&&octets[1]==254,"Local address required")
            val port=fields["port"]?.toIntOrNull()?:0;requireProtocol(port in 1024..65535,"Invalid port");val nonce=fields.getValue("nonce");requireProtocol(nonce.matches(Regex("[a-f0-9]{32}")),"Invalid nonce")
            val expiry=fields["expires"]?.toLongOrNull()?:0;requireProtocol(expiry>now&&expiry-now<=600,"QR expired or invalid lifetime");return PairingQr(host,port,nonce,expiry)
        }
    }
}
