package com.ezshare.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ezshare.core.Metadata
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Real MediaProvider and PackageManager on API29/API35, not a two-phone test. */
@RunWith(AndroidJUnit4::class)
class NativeStorageApkTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver get()=context.contentResolver
    private fun pendingFlag(p:MediaStorePending)=resolver.query(p.uri,arrayOf(MediaStore.Downloads.IS_PENDING),null,null,null)!!.use{assertTrue(it.moveToFirst());it.getInt(0)}
    private fun exists(p:MediaStorePending)=resolver.query(p.uri,arrayOf(MediaStore.Downloads._ID),null,null,null)!!.use{it.moveToFirst()}
    @Test fun downloadsPreserveAllTypesAndPublishOnlyAfterVerifiedReadback(){
        val fixtures=listOf("صورة.jpg" to "image/jpeg","فيديو.mp4" to "video/mp4","مستند.pdf" to "application/pdf","تطبيق.apk" to ApkSharing.APK_MIME,"بيانات.bin" to "application/octet-stream","فارغ.txt" to "text/plain")
        for((name,mime) in fixtures){val payload=if(name=="فارغ.txt") byteArrayOf() else ByteArray(131073){(it%251).toByte()};val expectedName=UUID.randomUUID().toString()+"-"+name
            val p=MediaStorePending(context,Metadata(expectedName,mime,payload.size.toLong()))
            try{assertEquals(1,pendingFlag(p));p.output.use{it.write(payload)};assertEquals(1,pendingFlag(p));assertEquals(p.uri.toString(),p.commit());assertEquals(0,pendingFlag(p));assertArrayEquals(payload,resolver.openInputStream(p.uri)!!.use{it.readBytes()});resolver.query(p.uri,arrayOf(MediaStore.Downloads.DISPLAY_NAME,MediaStore.Downloads.RELATIVE_PATH),null,null,null)!!.use{assertTrue(it.moveToFirst());assertEquals(expectedName,it.getString(0));assertEquals("Download/EZ/",it.getString(1))}
                val entry=HistoryEntry(id=expectedName,name=expectedName,bytes=payload.size.toLong(),total=payload.size.toLong(),duration=1.0,peer="fixture",incoming=true,status="completed",savedAt=p.uri.toString());val share=receivedIntent(context,entry,true);assertEquals(Intent.ACTION_SEND,share.action);assertEquals(p.uri,share.clipData!!.getItemAt(0).uri);assertEquals(0,share.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION);assertEquals(Intent.ACTION_VIEW,receivedIntent(context,entry,false).action)
            }finally{resolver.delete(p.uri,null,null)}
        }
    }
    @Test fun duplicatesNeverOverwriteExistingBytes(){val name=UUID.randomUUID().toString()+".apk";val first=MediaStorePending(context,Metadata(name,ApkSharing.APK_MIME,3));var second:MediaStorePending?=null
        try{first.output.use{it.write(byteArrayOf(1,2,3))};first.commit();second=MediaStorePending(context,Metadata(name,ApkSharing.APK_MIME,1));second.output.use{it.write(9)};second.commit();assertNotEquals(first.uri,second.uri);assertNotEquals(receivedName(context,first.uri.toString(),""),receivedName(context,second.uri.toString(),""));assertArrayEquals(byteArrayOf(1,2,3),resolver.openInputStream(first.uri)!!.use{it.readBytes()})}finally{resolver.delete(first.uri,null,null);second?.let{resolver.delete(it.uri,null,null)}}
    }
    @Test fun interruptedAndCancelledFilesAreDeleted(){val p=MediaStorePending(context,Metadata("cancel-${UUID.randomUUID()}.bin","application/octet-stream",999));p.output.write(byteArrayOf(1,2));assertEquals(1,pendingFlag(p));p.abort();assertFalse(exists(p));p.abort()}
    @Test fun storageCorruptionCannotPublish(){val p=MediaStorePending(context,Metadata("corrupt-${UUID.randomUUID()}.bin","application/octet-stream",3));try{p.output.use{it.write(byteArrayOf(1,2,3))};resolver.openOutputStream(p.uri,"wt")!!.use{it.write(7)};try{p.commit();fail("Corruption published")}catch(_:java.io.IOException){};assertEquals(1,pendingFlag(p));p.abort();assertFalse(exists(p))}finally{resolver.delete(p.uri,null,null)}}
    private fun hash(file:File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes())
    @Test fun installedOwnAndThirdPartyApksExportWithExactBytes(){for(pkg in listOf(context.packageName,"com.ezshare.fixture.single")){val installed=context.packageManager.getPackageInfo(pkg,PackageManager.GET_SIGNING_CERTIFICATES);val original=File(installed.applicationInfo!!.sourceDir);val exported=ApkSharing.export(context,pkg);assertArrayEquals(hash(original),hash(exported));assertTrue(exported.name.endsWith(".apk"));val uri=ApkSharing.shareIntent(context,exported).clipData!!.getItemAt(0).uri;assertArrayEquals(exported.readBytes(),resolver.openInputStream(uri)!!.use{it.readBytes()})}}
    @Test fun signedThirdPartyAndUnsignedApkPayloadsUseAuthenticatedTransport(){
        val thirdParty=ApkSharing.export(context,"com.ezshare.fixture.single").readBytes()
        for(payload in listOf(thirdParty,"ordinary unsigned file named .apk".toByteArray())){
            val listener=java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress());val pool=java.util.concurrent.Executors.newSingleThreadExecutor();val codes=java.util.concurrent.ArrayBlockingQueue<String>(1)
            val collected=java.io.ByteArrayOutputStream();var committed=false
            val task=pool.submit<com.ezshare.core.BatchResult>{listener.accept().use{socket->com.ezshare.core.Connection(socket,5000).use{connection->com.ezshare.core.BatchEngine.receive(connection,"receiver",approve={_,_,sas->codes.put(sas);true},create={object:com.ezshare.core.PendingFile{override val output=collected;override fun commit():String{committed=true;return "test-only"};override fun abort(){error("Unexpected aborted transfer")}}})}}}
            try{java.net.Socket(java.net.InetAddress.getLoopbackAddress(),listener.localPort).use{socket->com.ezshare.core.Connection(socket,5000).use{connection->val result=com.ezshare.core.BatchEngine.send(connection,"sender",com.ezshare.core.Manifest(listOf(Metadata("third-party.apk",ApkSharing.APK_MIME,payload.size.toLong()))),listOf({java.io.ByteArrayInputStream(payload)}),{_,_,sas->assertEquals(sas,codes.poll(3,java.util.concurrent.TimeUnit.SECONDS));true});assertEquals(payload.size.toLong(),result.files.single().bytes)}};task.get(10,java.util.concurrent.TimeUnit.SECONDS);assertTrue(committed);assertArrayEquals(payload,collected.toByteArray())}finally{listener.close();pool.shutdownNow()}
        }
    }
    @Test fun nativeSharesheetLaunchesForEzApk(){val apk=ApkSharing.export(context);InstrumentationRegistry.getInstrumentation().runOnMainSync{ApkSharing.share(context,apk)};Thread.sleep(700);val automation=InstrumentationRegistry.getInstrumentation().uiAutomation;val root=automation.rootInActiveWindow;assertNotNull(root);assertNotEquals(context.packageName,root.packageName?.toString());val capture=automation.takeScreenshot();assertNotNull(capture);File(context.getExternalFilesDir(null),"native-apk-sharesheet.png").outputStream().use{capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)}
}
