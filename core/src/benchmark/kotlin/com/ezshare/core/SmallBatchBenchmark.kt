package com.ezshare.core
import java.net.*
import java.io.*
import java.util.concurrent.*
object SmallBatchBenchmark{
 @JvmStatic fun main(args:Array<String>){println("protocol,files,bytes,trial,total_ms");for(trial in 1..2)for(version in 1..2){val count=64;val data=ByteArray(65536){(it%251).toByte()};val m=Metadata("small.bin","application/octet-stream",data.size.toLong());val start=System.nanoTime()
  fun pending()=object:PendingFile{override val output:OutputStream=OutputStream.nullOutputStream();override fun commit()="synthetic-null-sink";override fun abort(){}}
  val server=ServerSocket(0,1,InetAddress.getLoopbackAddress());val pool=Executors.newSingleThreadExecutor();val task=pool.submit<Unit>{repeat(if(version==1)count else 1){server.accept().use{s->Connection(s).use{c->if(version==1)TransferEngine.receive(c,"receiver",{_,_,_->true},{pending()},{}) else BatchEngine.receive(c,"receiver",approve={_,_,_->true},create={pending()})}}}}
  try{repeat(if(version==1)count else 1){Socket(InetAddress.getLoopbackAddress(),server.localPort).use{s->Connection(s).use{c->if(version==1)TransferEngine.send(c,"sender",m,{ByteArrayInputStream(data)},{_,_->true},{}) else BatchEngine.send(c,"sender",Manifest(List(count){m}),List(count){{ByteArrayInputStream(data)}},{_,_,_->true})}}};task.get(120,TimeUnit.SECONDS);println("$version,$count,${data.size.toLong()*count},$trial,${(System.nanoTime()-start)/1e6}")}finally{server.close();pool.shutdownNow()}
 }}
}
