package com.ezshare.app

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.ezshare.core.Metadata
import com.ezshare.core.PairingQr
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.security.MessageDigest

/** Test-APK-only command port. Never shipped in the application APK. No automatic approval. */
class NetworkPeerHarnessTest {
    @Test fun controlledPeer(){
        org.junit.Assume.assumeTrue("Dedicated dual-emulator driver required",InstrumentationRegistry.getArguments().getString("ez_peer_harness")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as EZApplication
        val controller=app.transfers
        val activity=instrumentation.startActivitySync(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val server=ServerSocket(39441,8,java.net.InetAddress.getLoopbackAddress())
        server.soTimeout=900000
        Log.i("EZNetworkTest","READY test-only command server")
        var running=true
        try{while(running){server.accept().use{socket->socket.soTimeout=30000
            val input=socket.getInputStream().bufferedReader();val output=socket.getOutputStream().bufferedWriter()
            val reply=try{
                val command=JSONObject(input.readLine());val op=command.getString("op")
                when(op){
                    "receive"->instrumentation.runOnMainSync{controller.receive()}
                    "stop"->instrumentation.runOnMainSync{controller.stopReceive()}
                    "cancel"->instrumentation.runOnMainSync{controller.cancel()}
                    "discover"->instrumentation.runOnMainSync{controller.startDiscovery()}
                    "decide"->controller.decide(command.getBoolean("accept"))
                    "send"->{val size=command.optInt("size",131073);val f=File(app.cacheDir,"network-اختبار.bin");f.outputStream().use{out->val block=ByteArray(65536){(it%251).toByte()};var left=size;while(left>0){val n=minOf(left,block.size);out.write(block,0,n);left-=n}}
                        controller.select(listOf(PickedFile(Uri.fromFile(f),Metadata(f.name,"application/octet-stream",f.length()))))
                        instrumentation.runOnMainSync{if(command.has("qr"))controller.scan(PairingQr.decode(command.getString("qr"))) else controller.send(command.getString("address"),command.optString("nonce",""))}
                    }
                    "expiredScan"->{val qr=PairingQr("10.0.2.2",34567,"a".repeat(32),System.currentTimeMillis()/1000-1);instrumentation.runOnMainSync{controller.scan(qr)}}
                    "qr"->{val state=controller.ui.value;val payload=PairingQr(command.getString("host"),command.optInt("port",state.port),state.pairingNonce,state.pairingExpiry).encode()
                        val bmp=pairingBitmap(payload);val pixels=IntArray(bmp.width*bmp.height);bmp.getPixels(pixels,0,bmp.width,0,0,bmp.width,bmp.height)
                        val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bmp.width,bmp.height,pixels)))).text
                        check(payload==decoded);PairingQr.decode(decoded)
                        output.write(JSONObject().put("ok",true).put("qr",decoded).toString()+"\n");output.flush();return@use
                    }
                    "shutdown"->{controller.cancel();running=false}
                    "state"->{}
                    else->error("Unknown command")
                }
                val s=controller.ui.value
                val history=EzLocalStore(app).history()
                JSONObject().put("ok",true).put("busy",s.busy).put("listening",s.listening).put("port",s.port).put("nonce",s.pairingNonce).put("expiry",s.pairingExpiry)
                    .put("stage",s.stage).put("status",s.status).put("error",s.error).put("sas",s.request?.sas).put("request",s.request!=null)
                    .put("operation",s.operation).put("results",s.completedResults.size)
                    .put("peers",JSONArray(s.peers.map{JSONObject().put("name",it.name).put("address",it.address)}))
                    .put("history",JSONArray(history.filter{it.name.startsWith("network-")}.map{entry->
                        val row=JSONObject().put("status",entry.status).put("incoming",entry.incoming).put("sha",entry.sha).put("bytes",entry.bytes)
                        if(entry.incoming&&entry.status=="completed"){
                            val digest=MessageDigest.getInstance("SHA-256");app.contentResolver.openInputStream(Uri.parse(entry.savedAt))!!.use{stream->val buf=ByteArray(65536);while(true){val n=stream.read(buf);if(n<0)break;digest.update(buf,0,n)}}
                            row.put("storedSha",digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)})
                        };row
                    }))
            }catch(e:Exception){JSONObject().put("ok",false).put("error",e.toString())}
            output.write(reply.toString()+"\n");output.flush()
        }}}finally{server.close();controller.cancel();instrumentation.runOnMainSync{activity.finish()}}
    }
}
