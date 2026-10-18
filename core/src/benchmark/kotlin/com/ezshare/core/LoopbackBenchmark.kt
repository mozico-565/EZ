package com.ezshare.core

import java.io.*
import java.net.*
import java.util.concurrent.*

/** Synthetic JVM loopback only; not Android, radio, SAF, or SHAREit performance. */
object LoopbackBenchmark{
    @JvmStatic fun main(args:Array<String>){println("protocol,bytes,trial,total_ms,stream_MBps");for(size in listOf(4_000_000L,100_000_000L,1_000_000_000L))for(trial in 1..2)for(version in 1..2){
        val server=ServerSocket(0,1,InetAddress.getLoopbackAddress());val pool=Executors.newSingleThreadExecutor();val start=System.nanoTime()
        fun pending()=object:PendingFile{override val output:OutputStream=object:OutputStream(){override fun write(b:Int){};override fun write(b:ByteArray,o:Int,n:Int){}};override fun commit()="synthetic-null-sink";override fun abort(){}}
        val task=pool.submit<Unit>{server.accept().use{s->Connection(s).use{c->if(version==1)TransferEngine.receive(c,"benchmark-receiver",{_,_,_->true},{pending()},{}) else BatchEngine.receive(c,"benchmark-receiver",approve={_,_,_->true},create={pending()})}}}
        try{Socket(InetAddress.getLoopbackAddress(),server.localPort).use{s->Connection(s).use{c->val meta=Metadata("synthetic.bin","application/octet-stream",size)
            val source={object:InputStream(){var left=size;val template=ByteArray(65536){(it%251).toByte()};override fun read():Int{if(left<=0)return -1;left--;return 1};override fun read(b:ByteArray,o:Int,n:Int):Int{if(left==0L)return -1;val count=minOf(n.toLong(),left,65536).toInt();System.arraycopy(template,0,b,o,count);left-=count;return count}} as InputStream}
            val result=if(version==1)TransferEngine.send(c,"benchmark-sender",meta,source,{_,_->true},{}) else BatchEngine.send(c,"benchmark-sender",Manifest(listOf(meta)),listOf(source),{_,_,_->true}).files.single()
            task.get(180,TimeUnit.SECONDS);println("$version,$size,$trial,${(System.nanoTime()-start)/1e6},${result.bytes/(result.elapsedNanos/1e9)/1e6}")
        }}}finally{server.close();pool.shutdownNow()}
    }}
}
