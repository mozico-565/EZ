package com.ezshare.core

import java.io.*
import java.net.Socket
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ProtocolException(message: String) : IOException(message)
data class Metadata(val name: String, val mime: String, val size: Long) {
    fun validate() {
        require(size >= -1 && size <= MAX_BYTES) { "Invalid file length" }
        require(name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= 1024) { "Invalid filename" }
        require(mime.length in 1..128 && mime.none { it.isISOControl() }) { "Invalid MIME type" }
    }
    companion object { const val MAX_BYTES = 16L * 1024 * 1024 * 1024 }
}
fun safeFilename(name: String): String = name.substringAfterLast('/').substringAfterLast('\\')
    .replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*\\u202A-\\u202E\\u2066-\\u2069]"), "_")
    .trim().trim('.').take(100).ifBlank { "received-file" }
fun uniqueFilename(name: String, exists: (String) -> Boolean): String {
    val clean = safeFilename(name)
    if (!exists(clean)) return clean
    val dot = clean.lastIndexOf('.').takeIf { it > 0 } ?: clean.length
    val base = clean.substring(0, dot); val ext = clean.substring(dot)
    for (i in 1..9999) { val candidate = "$base ($i)$ext"; if (!exists(candidate)) return candidate }
    throw IOException("Too many duplicate filenames")
}
data class Progress(val bytes: Long, val total: Long, val elapsedNanos: Long, val currentMBps: Double) {
    val averageMBps get() = if (elapsedNanos > 0) bytes / (elapsedNanos / 1e9) / 1e6 else 0.0
    val percent get() = if (total == 0L && bytes == 0L) 100.0 else if (total > 0) (bytes.toDouble() / total * 100).coerceIn(0.0, 100.0) else null
    val etaSeconds get() = if (total >= bytes && currentMBps > 0) (total - bytes) / (currentMBps * 1e6) else null
}
class ProgressMeter(private val total: Long, private val clock: () -> Long = System::nanoTime) {
    private val start = clock(); private var last = start; private var previous = 0L
    fun sample(bytes: Long): Progress { val now = clock(); val delta = now - last
        val speed = if (delta > 0) (bytes - previous).coerceAtLeast(0) / (delta / 1e9) / 1e6 else 0.0
        last = now; previous = bytes; return Progress(bytes, total, (now - start).coerceAtLeast(0), speed)
    }
}
interface PendingFile {
    val output: OutputStream
    /** Called only after the stream is closed and SHA-256 has matched. */
    fun commit(): String
    fun abort()
}
data class Result(val metadata: Metadata, val bytes: Long, val sha256: String, val elapsedNanos: Long, val savedAt: String = "")
class Connection(val socket: Socket, private val idleMillis: Long = 30_000) : Closeable {
    val cancelled = AtomicBoolean(false)
    @Volatile private var touched = System.nanoTime()
    @Volatile private var allowance = idleMillis
    private val watchdog = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "ez-timeout").apply { isDaemon = true } }
    init { socket.soTimeout = idleMillis.toInt(); socket.tcpNoDelay = true
        watchdog.scheduleAtFixedRate({ if ((System.nanoTime()-touched)/1_000_000 > allowance) close() }, 1, 1, TimeUnit.SECONDS)
    }
    fun touch() { touched = System.nanoTime() }
    fun approvalWindow() { allowance = 120_000; socket.soTimeout = 120_000; touch() }
    fun transferWindow() { allowance = idleMillis; socket.soTimeout = idleMillis.toInt(); touch() }
    override fun close() { cancelled.set(true); runCatching { socket.close() }; watchdog.shutdownNow() }
}
/** Authenticated-encryption records. Pair authenticity requires the users to compare SAS and approve on both devices. */
class SecureChannel(private val connection: Connection, sender: Boolean, deviceName: String, protocolVersion:Int=1) {
    private val input = DataInputStream(BufferedInputStream(connection.socket.getInputStream(), 64 * 1024))
    private val output = DataOutputStream(BufferedOutputStream(connection.socket.getOutputStream(), if(protocolVersion==2)256 * 1024 else 64 * 1024))
    private val txKey: SecretKeySpec; private val rxKey: SecretKeySpec
    private var txSeq = 0L; private var rxSeq = 0L
    val peerName: String; val sas: String
    init {
        val localName = deviceName.trim().take(48).ifBlank { "Android" }
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val pub = pair.public.encoded
        output.writeInt(0x455A0000 or protocolVersion); output.writeBoolean(sender); output.writeBounded(pub, 1024); output.writeBounded(nonce,32); output.writeText(localName,192); output.flush()
        requireProtocol(input.readInt()==(0x455A0000 or protocolVersion),"Unsupported protocol")
        requireProtocol(input.readBoolean()!=sender,"Roles conflict")
        val remotePub=input.readBounded(1024); val remoteNonce=input.readBounded(32); requireProtocol(remoteNonce.size==32,"Invalid nonce")
        peerName=input.readText(192).also { requireProtocol(it.isNotBlank() && it.none(Char::isISOControl),"Invalid device name") }
        val remote=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(remotePub)) as? ECPublicKey ?: throw ProtocolException("Invalid EC public key")
        val localParams=(pair.public as ECPublicKey).params
        requireProtocol(remote.params.order==localParams.order && remote.params.curve==localParams.curve,"Unsupported EC parameters")
        val agreement=KeyAgreement.getInstance("ECDH");agreement.init(pair.private);agreement.doPhase(remote,true)
        val transcript=ByteArrayOutputStream().also { b -> DataOutputStream(b).use { d ->
            d.writeText(if(protocolVersion==1)"EZ-ALPHA-1" else "EZ-BATCH-2",32)
            if(sender){d.writeBounded(pub,1024);d.writeBounded(nonce,32);d.writeText(localName,192);d.writeBounded(remotePub,1024);d.writeBounded(remoteNonce,32);d.writeText(peerName,192)}
            else {d.writeBounded(remotePub,1024);d.writeBounded(remoteNonce,32);d.writeText(peerName,192);d.writeBounded(pub,1024);d.writeBounded(nonce,32);d.writeText(localName,192)}
        }}.toByteArray()
        val hash=MessageDigest.getInstance("SHA-256").digest(transcript)
        val prk=hmac(hash,agreement.generateSecret())
        val forward=hmac(prk,"EZ sender to receiver\u0001".toByteArray()).copyOf(32)
        val backward=hmac(prk,"EZ receiver to sender\u0001".toByteArray()).copyOf(32)
        txKey=SecretKeySpec(if(sender)forward else backward,"AES");rxKey=SecretKeySpec(if(sender)backward else forward,"AES")
        val sasBytes=hmac(prk,"EZ SAS".toByteArray());val code=(java.nio.ByteBuffer.wrap(sasBytes).int.toLong() and 0xffffffffL)%1_000_000
        sas=String.format(java.util.Locale.ROOT,"%06d",code);connection.touch()
    }
    fun send(bytes: ByteArray, flush:Boolean=true) { requireProtocol(bytes.size<=65537,"Record too large");checkSequence(txSeq)
        val encrypted=crypt(Cipher.ENCRYPT_MODE,txKey,txSeq++,bytes)
        output.writeInt(encrypted.size);output.write(encrypted);if(flush)output.flush();connection.touch()
    }
    fun receive(): ByteArray { checkSequence(rxSeq);val length=input.readInt();requireProtocol(length in 17..65553,"Invalid record length")
        val bytes=ByteArray(length);input.readFully(bytes);val result=crypt(Cipher.DECRYPT_MODE,rxKey,rxSeq++,bytes);connection.touch();return result
    }
    private fun checkSequence(n:Long){requireProtocol(n>=0 && n<Long.MAX_VALUE,"Sequence exhausted")}
    private fun crypt(mode:Int,key:SecretKeySpec,n:Long,bytes:ByteArray):ByteArray {
        val seq=java.nio.ByteBuffer.allocate(12).putInt(0).putLong(n).array()
        return Cipher.getInstance("AES/GCM/NoPadding").run { init(mode,key,GCMParameterSpec(128,seq));updateAAD("EZ1".toByteArray()+seq);doFinal(bytes) }
    }
}
private fun hmac(key:ByteArray,data:ByteArray):ByteArray=Mac.getInstance("HmacSHA256").run{init(SecretKeySpec(key,"HmacSHA256"));doFinal(data)}
fun requireProtocol(ok:Boolean,message:String){if(!ok)throw ProtocolException(message)}
fun DataOutputStream.writeBounded(bytes:ByteArray,max:Int){requireProtocol(bytes.size<=max,"Value too long");writeInt(bytes.size);write(bytes)}
fun DataInputStream.readBounded(max:Int):ByteArray{val n=readInt();requireProtocol(n in 0..max,"Invalid value length");return ByteArray(n).also{readFully(it)}}
fun DataOutputStream.writeText(s:String,max:Int)=writeBounded(s.toByteArray(Charsets.UTF_8),max)
fun DataInputStream.readText(max:Int)=readBounded(max).toString(Charsets.UTF_8)
private fun packet(type:Int,block:(DataOutputStream)->Unit={}):ByteArray=ByteArrayOutputStream().also{b->DataOutputStream(b).use{it.writeByte(type);block(it)}}.toByteArray()
private fun metadataPacket(m:Metadata)=packet(1){it.writeText(m.name,1024);it.writeText(m.mime,128);it.writeLong(m.size)}
private fun parseMetadata(bytes:ByteArray):Metadata=DataInputStream(ByteArrayInputStream(bytes)).use { d ->
    requireProtocol(d.readUnsignedByte()==1,"Expected metadata");val m=Metadata(d.readText(1024),d.readText(128),d.readLong());requireProtocol(d.available()==0,"Trailing metadata");m.validate();m
}
private fun digestHex(d:ByteArray)=d.joinToString(""){"%02x".format(it.toInt() and 255)}
object TransferEngine {
    fun send(c:Connection,deviceName:String,m:Metadata,source:()->InputStream,confirm:(peer:String,sas:String)->Boolean,onProgress:(Progress)->Unit):Result {
        m.validate();val ch=SecureChannel(c,true,deviceName);c.approvalWindow()
        ch.send(metadataPacket(m))
        if(!confirm(ch.peerName,ch.sas))throw ProtocolException("Pairing rejected locally")
        requireProtocol(ch.receive().contentEquals(byteArrayOf(2)),"Receiver rejected the transfer")
        c.transferWindow();val meter=ProgressMeter(m.size);val digest=MessageDigest.getInstance("SHA-256");var bytes=0L;var last=0L
        source().use { stream -> val buffer=ByteArray(64*1024)
            while(true){if(c.cancelled.get())throw IOException("Cancelled");val n=stream.read(buffer);if(n<0)break;if(n==0)continue
                bytes+=n;requireProtocol(bytes<=Metadata.MAX_BYTES && (m.size<0||bytes<=m.size),"Source length changed")
                digest.update(buffer,0,n);ch.send(byteArrayOf(3)+buffer.copyOf(n));val now=System.nanoTime();if(now-last>=150_000_000){onProgress(meter.sample(bytes));last=now}
            }
        }
        requireProtocol(m.size<0||bytes==m.size,"Source truncated")
        val hash=digest.digest();ch.send(packet(4){it.writeLong(bytes);it.write(hash)});c.approvalWindow()
        val ack=DataInputStream(ByteArrayInputStream(ch.receive()));requireProtocol(ack.readUnsignedByte()==5,"No verified receipt")
        requireProtocol(ack.readLong()==bytes,"Receipt length mismatch");val remoteHash=ByteArray(32);ack.readFully(remoteHash);requireProtocol(ack.available()==0 && MessageDigest.isEqual(remoteHash,hash),"Receipt hash mismatch")
        val progress=meter.sample(bytes);onProgress(progress);return Result(m,bytes,digestHex(hash),progress.elapsedNanos)
    }
    fun receive(c:Connection,deviceName:String,approve:(Metadata,String,String)->Boolean,create:(Metadata)->PendingFile,onProgress:(Progress)->Unit):Result {
        val ch=SecureChannel(c,false,deviceName);c.approvalWindow();val m=parseMetadata(ch.receive())
        if(!approve(m,ch.peerName,ch.sas)){ch.send(byteArrayOf(9));throw ProtocolException("Receiver rejected")}
        val pending=create(m);var committed=false
        try {ch.send(byteArrayOf(2));c.transferWindow();val meter=ProgressMeter(m.size);val digest=MessageDigest.getInstance("SHA-256");var bytes=0L;var hash=ByteArray(0);var last=0L
            pending.output.use { out ->
                while(true){if(c.cancelled.get())throw IOException("Cancelled");val frame=ch.receive();requireProtocol(frame.isNotEmpty(),"Empty record")
                    when(frame[0].toInt()){
                        3->{val n=frame.size-1;requireProtocol(n>0,"Empty chunk");bytes+=n;requireProtocol(bytes<=Metadata.MAX_BYTES&&(m.size<0||bytes<=m.size),"Unexpected file size");out.write(frame,1,n);digest.update(frame,1,n);val now=System.nanoTime();if(now-last>=150_000_000){onProgress(meter.sample(bytes));last=now}}
                        4->{val end=DataInputStream(ByteArrayInputStream(frame,1,frame.size-1));requireProtocol(frame.size==41,"Invalid end record");val declared=end.readLong();val expected=ByteArray(32);end.readFully(expected);hash=digest.digest();requireProtocol(declared==bytes&&(m.size<0||m.size==bytes),"Truncated file");requireProtocol(MessageDigest.isEqual(expected,hash),"SHA-256 mismatch");out.flush();break}
                        else->throw ProtocolException("Unexpected frame")
                    }
                }
            }
            c.approvalWindow();val saved=pending.commit();committed=true
            ch.send(packet(5){it.writeLong(bytes);it.write(hash)});val progress=meter.sample(bytes);onProgress(progress);return Result(m,bytes,digestHex(hash),progress.elapsedNanos,saved)
        } finally { if(!committed){runCatching { pending.output.close() };runCatching { pending.abort() }} }
    }
}
