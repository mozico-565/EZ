package com.ezshare.core

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.*
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class EngineTest {
    @Test fun longArabicNamesKeepApkExtension(){val clean=safeFilename("ملف".repeat(70)+".apk");assertTrue(clean.endsWith(".apk"));assertTrue(clean.length<=100);assertTrue(uniqueFilename(clean){it==clean}.endsWith(" (1).apk"))}
    @Test fun sanitizesNamesAndDuplicates(){assertEquals("a.txt",safeFilename("../../a.txt"));assertEquals("evil.txt",safeFilename("C:\\private\\evil.txt"));assertEquals("received-file",safeFilename("..."));assertFalse(safeFilename("ab\u202ecd").contains('\u202e'));assertEquals("a (1).txt",uniqueFilename("a.txt"){it=="a.txt"})}
    @Test fun rejectsInvalidMetadata(){for(size in listOf(-2L,Metadata.MAX_BYTES+1)){assertThrows(IllegalArgumentException::class.java){Metadata("a","text/plain",size).validate()}};assertThrows(IllegalArgumentException::class.java){Metadata("","text/plain",1).validate()};assertThrows(IllegalArgumentException::class.java){Metadata("a","bad\n",1).validate()}}
    @Test fun progressUsesMonotonicActualByteDeltas(){var n=0L;val meter=ProgressMeter(10_000_000){n};n=1_000_000_000;val p=meter.sample(2_000_000);assertEquals(2.0,p.currentMBps,0.0001);assertEquals(20.0,p.percent!!,0.001);assertEquals(4.0,p.etaSeconds!!,0.001);n=3_000_000_000;val q=meter.sample(4_000_000);assertEquals(1.0,q.currentMBps,0.001);assertEquals(4.0/3,q.averageMBps,0.001);assertNull(Progress(5,-1,1,1.0).percent)}
    private fun runTransfer(data:ByteArray,declared:Long=data.size.toLong(),accept:Boolean=true,source:()->InputStream={ByteArrayInputStream(data)}):Triple<Result?,ByteArray,Boolean>{
        val listener=ServerSocket(0,1,InetAddress.getLoopbackAddress());val executor=Executors.newSingleThreadExecutor();val output=ByteArrayOutputStream();val aborted=AtomicBoolean(false);val receiverSas=ArrayBlockingQueue<String>(1)
        val task=executor.submit<Result?> { listener.accept().use { socket->Connection(socket,3000).use { c->runCatching {TransferEngine.receive(c,"receiver",{_,_,sas->receiverSas.put(sas);accept},{_->object:PendingFile{override val output:OutputStream=output;override fun commit()="memory";override fun abort(){aborted.set(true)}}},{})}.getOrNull() } } }
        var result:Result?=null
        try{Socket(InetAddress.getLoopbackAddress(),listener.localPort).use{socket->Connection(socket,3000).use{c->result=runCatching{TransferEngine.send(c,"sender",Metadata("../فيديو.bin","application/octet-stream",declared),source,{_,sas->assertEquals(sas,receiverSas.poll(2,TimeUnit.SECONDS));true},{})}.getOrNull()}};val received=task.get(5,TimeUnit.SECONDS);assertEquals(result!=null,received!=null)
        }finally{listener.close();executor.shutdownNow()}
        return Triple(result,output.toByteArray(),aborted.get())
    }
    @Test fun actualEncryptedLoopbackVerifiesSHA256AndStreamsMultipleFrames(){val data=ByteArray(2*1024*1024+17){(it%251).toByte()};val (r,received,aborted)=runTransfer(data);assertNotNull(r);assertArrayEquals(data,received);assertEquals(data.size.toLong(),r!!.bytes);val hash=MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it.toInt() and 255)};assertEquals(hash,r.sha256);assertFalse(aborted)}
    @Test fun unknownLengthAndEmptyFile(){assertNotNull(runTransfer(ByteArray(70_000){7},-1).first);assertNotNull(runTransfer(byteArrayOf()).first)}
    @Test fun explicitRejectionSendsNoFileBytes(){val (r,bytes,_)=runTransfer(ByteArray(100),accept=false);assertNull(r);assertTrue(bytes.isEmpty())}
    @Test fun truncatedStreamNeverCommitsAndAborts(){val (r,_,aborted)=runTransfer(ByteArray(100),declared=101);assertNull(r);assertTrue(aborted)}
    @Test fun sourceErrorAbortsReceiverPartial(){val data=ByteArray(100_000);val (r,_,aborted)=runTransfer(data,source={object:InputStream(){var count=0;override fun read():Int=throw IOException("interrupted");override fun read(b:ByteArray,off:Int,len:Int):Int{if(count++>0)throw IOException("network/source interruption");return 50_000}}});assertNull(r);assertTrue(aborted)}
    @Test fun malformedHandshakeRejectedWithoutAllocation(){val server=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();val t=pool.submit<Boolean>{server.accept().use{s->Connection(s,500).use{c->runCatching{SecureChannel(c,false,"receiver")}.isFailure}}};Socket("127.0.0.1",server.localPort).use{s->val d=DataOutputStream(s.getOutputStream());d.writeInt(0);d.flush();assertTrue(t.get(3,TimeUnit.SECONDS))};server.close();pool.shutdownNow()}
    @Test fun stalledPeerTimesOut(){val server=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();val t=pool.submit<Boolean>{server.accept().use{s->Connection(s,250).use{c->runCatching{SecureChannel(c,false,"receiver")}.isFailure}}};Socket("127.0.0.1",server.localPort).use{assertTrue(t.get(3,TimeUnit.SECONDS))};server.close();pool.shutdownNow()}
    @Test fun closeCancelsBlockedSocket(){val server=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();val entered=CountDownLatch(1);val t=pool.submit<Boolean>{server.accept().use{s->val c=Connection(s,3000);entered.countDown();pool.hashCode();c.close();runCatching{s.getInputStream().read()}.isFailure}};Socket("127.0.0.1",server.localPort).use{assertTrue(entered.await(1,TimeUnit.SECONDS));assertTrue(t.get(2,TimeUnit.SECONDS))};server.close();pool.shutdownNow()}
    @Test fun corruptTerminalDigestAbortsWithoutPublishing(){
        val listener=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();val aborted=AtomicBoolean(false);val committed=AtomicBoolean(false)
        val task=pool.submit<Boolean>{listener.accept().use{s->Connection(s,2000).use{c->runCatching{TransferEngine.receive(c,"receiver",{_,_,_->true},{_->object:PendingFile{override val output=ByteArrayOutputStream();override fun commit():String{committed.set(true);return "bad"};override fun abort(){aborted.set(true)}}},{})}.isFailure}}}
        Socket("127.0.0.1",listener.localPort).use{s->Connection(s,2000).use{c->val ch=SecureChannel(c,true,"sender");val meta=ByteArrayOutputStream();DataOutputStream(meta).apply{writeByte(1);writeText("a.bin",1024);writeText("application/octet-stream",128);writeLong(3);flush()};ch.send(meta.toByteArray());assertArrayEquals(byteArrayOf(2),ch.receive());ch.send(byteArrayOf(3,1,2,3));val end=ByteArrayOutputStream();DataOutputStream(end).apply{writeByte(4);writeLong(3);write(ByteArray(32));flush()};ch.send(end.toByteArray());assertTrue(task.get(3,TimeUnit.SECONDS))}}
        assertTrue(aborted.get());assertFalse(committed.get());listener.close();pool.shutdownNow()
    }
}
