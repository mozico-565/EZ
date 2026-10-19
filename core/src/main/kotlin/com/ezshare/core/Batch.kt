package com.ezshare.core

import java.io.*
import java.security.MessageDigest
import java.util.UUID

/** Protocol 2 is incompatible with Alpha1, which fails closed at the handshake. */
data class Manifest(val files:List<Metadata>,val session:String=UUID.randomUUID().toString(),val pairingNonce:String="") {
    val total:Long get()=if(files.any{it.size<0})-1 else files.sumOf{it.size}
    fun validate(){requireProtocol(files.size in 1..256,"Select 1..256 files");files.forEach{it.validate()};requireProtocol(session.matches(Regex("[A-Za-z0-9-]{1,64}")),"Invalid session");requireProtocol(pairingNonce.isEmpty()||pairingNonce.matches(Regex("[a-f0-9]{32}")),"Invalid pairing nonce")}
}
data class BatchProgress(val overall:Progress,val index:Int,val count:Int,val name:String,val completed:Int,val stage:String)
data class PhaseTiming(val phase:String,val fileIndex:Int,val nanos:Long)
data class BatchResult(val session:String,val files:List<Result>,val timings:List<PhaseTiming>,val totalNanos:Long)
object BatchEngine {
    private fun encode(type:Int,block:(DataOutputStream)->Unit={}):ByteArray=ByteArrayOutputStream().also{b->DataOutputStream(b).use{it.writeByte(type);block(it)}}.toByteArray()
    private fun manifestBytes(m:Manifest):ByteArray {m.validate();return encode(20){d->d.writeText(m.session,64);d.writeText(m.pairingNonce,32);d.writeInt(m.files.size);m.files.forEach{d.writeText(it.name,1024);d.writeText(it.mime,128);d.writeLong(it.size)}}.also{requireProtocol(it.size<=65537,"Manifest too large")}}
    private fun manifest(bytes:ByteArray):Manifest=DataInputStream(ByteArrayInputStream(bytes)).use{d->
        requireProtocol(d.readUnsignedByte()==20,"Expected batch manifest");val id=d.readText(64);val nonce=d.readText(32);val n=d.readInt();requireProtocol(n in 1..256,"Invalid file count")
        Manifest(List(n){Metadata(d.readText(1024),d.readText(128),d.readLong())},id,nonce).also{it.validate();requireProtocol(d.available()==0,"Trailing manifest")}
    }
    fun send(c:Connection,name:String,m:Manifest,sources:List<()->InputStream>,confirm:(Manifest,String,String)->Boolean,onProgress:(BatchProgress)->Unit={},onComplete:(Result)->Unit={},onPairingFailure:(Exception)->Unit={}):BatchResult {
        requireProtocol(sources.size==m.files.size,"Source manifest mismatch");val encoded=manifestBytes(m);val start=System.nanoTime();val times=mutableListOf<PhaseTiming>();val results=mutableListOf<Result>()
        val hs=System.nanoTime();val ch=SecureChannel(c,true,name,2);times+=PhaseTiming("handshake",-1,System.nanoTime()-hs);c.approvalWindow();ch.send(encoded)
        val approveStart=System.nanoTime()
        // Read the peer decision while the local approval UI is open. Otherwise a
        // rejected/expired QR or disconnected peer leaves that UI blocked until timeout.
        // Exactly one reader owns rx here; join it before reading any file receipts.
        val remote=java.util.concurrent.CompletableFuture<Unit>()
        val approvalReader=Thread({
            try{requireProtocol(ch.receive().contentEquals(byteArrayOf(2)),"Receiver rejected the transfer");remote.complete(Unit)}
            catch(e:Exception){c.close();runCatching{onPairingFailure(e)};remote.completeExceptionally(e)}
        },"ez-pairing-decision").apply{isDaemon=true;start()}
        try{
            requireProtocol(confirm(m,ch.peerName,ch.sas),"Pairing rejected locally")
            try{remote.get()}catch(e:java.util.concurrent.ExecutionException){throw (e.cause as? Exception?:IOException("Peer pairing failed",e))}
        }catch(e:Exception){c.close();throw e}finally{
            if(!remote.isDone)c.close()
            approvalReader.join(1000)
        }
        times+=PhaseTiming("approval",-1,System.nanoTime()-approveStart)
        var completedBytes=0L;var streamingNanos=0L
        m.files.forEachIndexed{i,meta->
            onProgress(BatchProgress(Progress(completedBytes,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i,"preparing"))
            c.transferWindow();val meter=ProgressMeter(meta.size);val begin=System.nanoTime();val digest=MessageDigest.getInstance("SHA-256");var count=0L;var last=0L
            sources[i]().use{input->val buffer=ByteArray(64*1024+1);buffer[0]=3
                while(true){if(c.cancelled.get())throw IOException("Cancelled");val n=input.read(buffer,1,buffer.size-1);if(n<0)break;if(n==0)continue;count+=n;requireProtocol(count<=Metadata.MAX_BYTES&&(meta.size<0||count<=meta.size),"Source length changed");digest.update(buffer,1,n);ch.send(buffer.copyOf(n+1),count==n.toLong());if(count==n.toLong())times+=PhaseTiming("first_wire_chunk",i,System.nanoTime()-begin)
                    val now=System.nanoTime();if(now-last>=150_000_000){val p=meter.sample(count);onProgress(BatchProgress(p.copy(bytes=completedBytes+count,total=m.total,elapsedNanos=streamingNanos+p.elapsedNanos),i,m.files.size,meta.name,i,"streaming"));last=now}
                }
            }
            requireProtocol(meta.size<0||count==meta.size,"Source truncated");val hash=digest.digest();ch.send(encode(4){it.writeLong(count);it.write(hash)});val streamTime=System.nanoTime()-begin;streamingNanos+=streamTime;times+=PhaseTiming("streaming",i,streamTime);c.approvalWindow();onProgress(BatchProgress(Progress(completedBytes+count,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i,"verifying"))
            val receiptStart=System.nanoTime();val ack=DataInputStream(ByteArrayInputStream(ch.receive()));requireProtocol(ack.readUnsignedByte()==5&&ack.readLong()==count,"No verified receipt");val remote=ByteArray(32);ack.readFully(remote);requireProtocol(ack.available()==0&&MessageDigest.isEqual(hash,remote),"Receipt hash mismatch");times+=PhaseTiming("receipt_and_remote_verification",i,System.nanoTime()-receiptStart)
            val r=Result(meta,count,hash.hex(),streamTime);results+=r;onComplete(r);completedBytes+=count
            onProgress(BatchProgress(Progress(completedBytes,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i+1,"verified"))
        }
        return BatchResult(m.session,results,times+PhaseTiming("total_session",-1,System.nanoTime()-start),System.nanoTime()-start)
    }
    fun receive(c:Connection,name:String,expectedNonce:String="",approve:(Manifest,String,String)->Boolean,create:(Metadata)->PendingFile,onProgress:(BatchProgress)->Unit={},onComplete:(Result)->Unit={}):BatchResult {
        val start=System.nanoTime();val times=mutableListOf<PhaseTiming>();val results=mutableListOf<Result>();val hs=System.nanoTime();val ch=SecureChannel(c,false,name,2);times+=PhaseTiming("handshake",-1,System.nanoTime()-hs);c.approvalWindow();val m=manifest(ch.receive())
        requireProtocol(m.pairingNonce.isEmpty()||(expectedNonce.isNotBlank()&&MessageDigest.isEqual(m.pairingNonce.toByteArray(),expectedNonce.toByteArray())),"QR session expired or changed")
        val approvalStart=System.nanoTime();if(!approve(m,ch.peerName,ch.sas)){ch.send(byteArrayOf(9));throw ProtocolException("Receiver rejected")};times+=PhaseTiming("approval",-1,System.nanoTime()-approvalStart)
        var completedBytes=0L;var streamingNanos=0L
        m.files.forEachIndexed{i,meta->
            onProgress(BatchProgress(Progress(completedBytes,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i,"preparing"))
            val pending=create(meta);var committed=false
            try{
                if(i==0)ch.send(byteArrayOf(2));c.transferWindow();val meter=ProgressMeter(meta.size);val begin=System.nanoTime();val digest=MessageDigest.getInstance("SHA-256");var count=0L;var last=0L;var hash=ByteArray(0);var writeNanos=0L
                pending.output.use{out->while(true){if(c.cancelled.get())throw IOException("Cancelled");val frame=ch.receive();requireProtocol(frame.isNotEmpty(),"Empty record")
                    when(frame[0].toInt()){
                        3->{val n=frame.size-1;requireProtocol(n>0,"Empty chunk");count+=n;requireProtocol(count<=Metadata.MAX_BYTES&&(meta.size<0||count<=meta.size),"Unexpected file size");if(count==n.toLong())times+=PhaseTiming("first_wire_chunk",i,System.nanoTime()-begin);val w=System.nanoTime();out.write(frame,1,n);writeNanos+=System.nanoTime()-w;digest.update(frame,1,n);val now=System.nanoTime();if(now-last>=150_000_000){val p=meter.sample(count);onProgress(BatchProgress(p.copy(bytes=completedBytes+count,total=m.total,elapsedNanos=streamingNanos+p.elapsedNanos),i,m.files.size,meta.name,i,"streaming"));last=now}}
                        4->{requireProtocol(frame.size==41,"Invalid end record");val end=DataInputStream(ByteArrayInputStream(frame,1,40));val declared=end.readLong();val expected=ByteArray(32);end.readFully(expected);hash=digest.digest();requireProtocol(count==declared&&(meta.size<0||meta.size==count),"Truncated file");requireProtocol(MessageDigest.isEqual(hash,expected),"SHA-256 mismatch");out.flush();break}
                        else->throw ProtocolException("Unexpected frame")
                    }
                }}
                val streamTime=System.nanoTime()-begin;streamingNanos+=streamTime;times+=PhaseTiming("streaming",i,streamTime);times+=PhaseTiming("storage_write_within_streaming",i,writeNanos);c.approvalWindow()
                onProgress(BatchProgress(Progress(completedBytes+count,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i,"verifying"))
                val verifyStart=System.nanoTime();val saved=pending.commit();committed=true;times+=PhaseTiming("storage_readback_and_publish",i,System.nanoTime()-verifyStart)
                ch.send(encode(5){it.writeLong(count);it.write(hash)});val r=Result(meta,count,hash.hex(),streamTime,saved);results+=r;onComplete(r);completedBytes+=count
                onProgress(BatchProgress(Progress(completedBytes,m.total,streamingNanos,0.0),i,m.files.size,meta.name,i+1,"verified"))
            }finally{if(!committed){runCatching{pending.output.close()};runCatching{pending.abort()}.exceptionOrNull()?.let{throw IOException("Incomplete file cleanup failed; pending entry may remain",it)}}}
        }
        return BatchResult(m.session,results,times+PhaseTiming("total_session",-1,System.nanoTime()-start),System.nanoTime()-start)
    }
    private fun ByteArray.hex()=joinToString(""){"%02x".format(it.toInt() and 255)}
}
