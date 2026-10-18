package com.ezshare.core

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class BatchTest {
    private fun transfer(data:List<ByteArray>,sizes:List<Long> = data.map{it.size.toLong()},accept:Boolean=true,nonce:String="",expectedNonce:String=""):Triple<BatchResult?,List<ByteArray>,Pair<Int,Int>>{
        val server=ServerSocket(0,1,InetAddress.getLoopbackAddress());val pool=Executors.newSingleThreadExecutor();val outputs=mutableListOf<ByteArrayOutputStream>();var commits=0;var aborts=0;val sas=ArrayBlockingQueue<String>(1)
        val task=pool.submit<BatchResult?>{server.accept().use{socket->Connection(socket,3000).use{c->runCatching{BatchEngine.receive(c,"receiver",expectedNonce,{m,_,code->assertEquals(data.size,m.files.size);sas.put(code);accept},{_->object:PendingFile{override val output=ByteArrayOutputStream().also{outputs+=it};override fun commit():String{commits++;return "verified"};override fun abort(){aborts++}}})}.getOrNull()}}}
        var result:BatchResult?=null
        try{Socket(InetAddress.getLoopbackAddress(),server.localPort).use{socket->Connection(socket,3000).use{c->result=runCatching{BatchEngine.send(c,"sender",Manifest(data.indices.map{Metadata(if(it%2==0)"صورة.png" else "مستند.txt","application/octet-stream",sizes[it])},pairingNonce=nonce),data.map{bytes->{ByteArrayInputStream(bytes)}},{_,_,code->assertEquals(code,sas.poll(2,TimeUnit.SECONDS));true})}.getOrNull()}};task.get(8,TimeUnit.SECONDS)}finally{server.close();pool.shutdownNow()}
        return Triple(result,outputs.map{it.toByteArray()},commits to aborts)
    }
    @Test fun multipleFilesAreVerifiedInOneApprovedSession(){val data=listOf(ByteArray(4_000_000){(it%251).toByte()},byteArrayOf(),ByteArray(99000){4});val(r,output,counts)=transfer(data);assertEquals(3,r!!.files.size);assertEquals(3,counts.first);assertEquals(0,counts.second);data.zip(output).forEach{assertArrayEquals(it.first,it.second)};assertEquals(4_099_000L,r.files.sumOf{it.bytes});assertTrue(r.timings.any{it.phase=="handshake"});assertTrue(r.timings.any{it.phase=="receipt_and_remote_verification"})}
    @Test fun interruptedLaterFileKeepsEarlierVerifiedFileOnly(){val(r,_,counts)=transfer(listOf(ByteArray(1000),ByteArray(100)),listOf(1000,101));assertNull(r);assertEquals(1,counts.first);assertEquals(1,counts.second)}
    @Test fun rejectedBatchCreatesNoDestination(){val(r,output,counts)=transfer(listOf(ByteArray(100)),accept=false);assertNull(r);assertTrue(output.isEmpty());assertEquals(0,counts.first)}
    @Test fun wrongQrNonceCannotCreateAnyFile(){val(r,output,_)=transfer(listOf(ByteArray(10)),nonce="a".repeat(32),expectedNonce="b".repeat(32));assertNull(r);assertTrue(output.isEmpty())}
    @Test fun qrNonceMatchesLiveReceiverSession(){assertNotNull(transfer(listOf(ByteArray(70)),nonce="a".repeat(32),expectedNonce="a".repeat(32)).first)}
    @Test fun unknownSizeAndDuplicateNamesStillVerifyEachFile(){val(r,_,counts)=transfer(listOf(ByteArray(70000),ByteArray(1),byteArrayOf()),listOf(-1,-1,0));assertEquals(3,r!!.files.size);assertEquals(3,counts.first)}
    @Test fun manifestBoundsRejectBeforeOpeningSocket(){assertThrows(ProtocolException::class.java){Manifest(emptyList()).validate()};assertThrows(ProtocolException::class.java){Manifest(List(257){Metadata("x","text/plain",0)}).validate()};assertThrows(ProtocolException::class.java){Manifest(listOf(Metadata("x","text/plain",1)),pairingNonce="bad").validate()}}
    @Test fun alphaAndBatchProtocolFailClosed(){val listener=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();val task=pool.submit<Boolean>{listener.accept().use{s->Connection(s,1000).use{runCatching{SecureChannel(it,false,"receiver",2)}.isFailure}}};try{Socket("127.0.0.1",listener.localPort).use{s->Connection(s,1000).use{assertThrows(ProtocolException::class.java){SecureChannel(it,true,"sender",1)}}};assertTrue(task.get(3,TimeUnit.SECONDS))}finally{listener.close();pool.shutdownNow()}}
    @Test fun batchDigestMismatchAbortsWithoutPublishing(){
        val server=ServerSocket(0);val pool=Executors.newSingleThreadExecutor();var committed=false;var aborted=false
        val task=pool.submit<Boolean>{server.accept().use{s->Connection(s,2000).use{c->runCatching{BatchEngine.receive(c,"receiver",approve={_,_,_->true},create={object:PendingFile{override val output=ByteArrayOutputStream();override fun commit():String{committed=true;return "bad"};override fun abort(){aborted=true}}})}.isFailure}}}
        try{Socket("127.0.0.1",server.localPort).use{s->Connection(s,2000).use{c->val ch=SecureChannel(c,true,"sender",2);val bytes=ByteArrayOutputStream();DataOutputStream(bytes).apply{writeByte(20);writeText("test-session",64);writeText("",32);writeInt(1);writeText("a.bin",1024);writeText("application/octet-stream",128);writeLong(3)};ch.send(bytes.toByteArray());assertArrayEquals(byteArrayOf(2),ch.receive());ch.send(byteArrayOf(3,1,2,3));val end=ByteArrayOutputStream();DataOutputStream(end).apply{writeByte(4);writeLong(3);write(ByteArray(32))};ch.send(end.toByteArray());assertTrue(task.get(3,TimeUnit.SECONDS))}};assertFalse(committed);assertTrue(aborted)}finally{server.close();pool.shutdownNow()}
    }

}
