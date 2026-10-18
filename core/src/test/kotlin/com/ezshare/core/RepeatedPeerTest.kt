package com.ezshare.core

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.concurrent.*

class RepeatedPeerTest {
    @Test fun bidirectionalRepeatedTcpSessionsRejectStaleQrThenRecover(){
        val pool=Executors.newCachedThreadPool();val data=ByteArray(262145){(it%251).toByte()}
        try{repeat(16){round->
            ServerSocket(0,8,InetAddress.getLoopbackAddress()).use{listener->
                val nonce=if(round%2==0)"a".repeat(32) else "b".repeat(32)
                val sas=ArrayBlockingQueue<String>(1);val stored=ByteArrayOutputStream();var committed=false
                val receive=pool.submit<BatchResult>{listener.accept().use{socket->Connection(socket,3000).use{connection->
                    BatchEngine.receive(connection,if(round%2==0)"B" else "A",nonce,{_,_,code->sas.put(code);true},{object:PendingFile{
                        override val output=stored
                        override fun commit():String{assertArrayEquals(data,stored.toByteArray());committed=true;return "verified-memory"}
                        override fun abort(){error("Unexpected partial")}
                    }})
                }}}
                Socket("127.0.0.1",listener.localPort).use{socket->Connection(socket,3000).use{connection->
                    val sent=BatchEngine.send(connection,if(round%2==0)"A" else "B",Manifest(listOf(Metadata("network.bin","application/octet-stream",data.size.toLong())),pairingNonce=nonce),listOf({ByteArrayInputStream(data)}),{_,_,code->assertEquals(code,sas.poll(2,TimeUnit.SECONDS));true})
                    assertEquals(data.size.toLong(),sent.files.single().bytes)
                    println("TCP round=$round sender=${if(round%2==0)"A" else "B"} local=${socket.localPort} remote=${listener.localPort} bytes=${data.size} authenticated=true")
                }}
                assertEquals(1,receive.get(5,TimeUnit.SECONDS).files.size);assertTrue(committed)
            }
        }
        ServerSocket(0).use{listener->
            val receiver=pool.submit<Boolean>{listener.accept().use{s->Connection(s,1000).use{c->runCatching{BatchEngine.receive(c,"B","b".repeat(32),{_,_,_->error("Stale QR reached approval")},{error("Stale QR reached storage")})}.isFailure}}}
            Socket("127.0.0.1",listener.localPort).use{s->Connection(s,1000).use{c->assertThrows(IOException::class.java){BatchEngine.send(c,"A",Manifest(listOf(Metadata("x","text/plain",0)),pairingNonce="a".repeat(32)),listOf({ByteArrayInputStream(byteArrayOf())}),{_,_,_->true})}}}
            assertTrue(receiver.get(3,TimeUnit.SECONDS));println("TCP stale receiver nonce rejected before approval/storage")
        }
        }finally{pool.shutdownNow()}
    }
}
