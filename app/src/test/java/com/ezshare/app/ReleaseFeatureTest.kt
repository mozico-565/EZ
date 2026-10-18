package com.ezshare.app

import android.content.Intent
import android.graphics.Color
import androidx.core.content.FileProvider
import com.ezshare.core.PairingQr
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReleaseFeatureTest{
    @Test fun absentArchiveSigningMetadataIsNotACertificateMismatch(){val installed=setOf(listOf(1.toByte(),2.toByte()));ApkSharing.verifyExportIdentity(installed,emptySet());ApkSharing.verifyExportIdentity(installed,installed);assertThrows(IllegalArgumentException::class.java){ApkSharing.verifyExportIdentity(installed,setOf(listOf(3.toByte())))};assertThrows(IllegalArgumentException::class.java){ApkSharing.verifyExportIdentity(emptySet(),emptySet())}}
    @Test fun generatedQrIsReadableByIndependentZxingDecoder(){val payload=PairingQr("192.168.49.1",33045,"a".repeat(32),System.currentTimeMillis()/1000+600).encode();val bitmap=pairingBitmap(payload);val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);val result=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))));assertEquals(payload,result.text);assertEquals(payload,PairingQr.decode(result.text).encode())}
    @Test fun sharingGrantsOnlyReadAccessToControlledContentUri(){val c=RuntimeEnvironment.getApplication();val file=File(c.cacheDir,"exports/sample.apk").apply{parentFile!!.mkdirs();writeBytes(byteArrayOf(1,2,3))};val intent=ApkSharing.shareIntent(c,file);assertEquals(Intent.ACTION_SEND,intent.action);assertEquals(ApkSharing.APK_MIME,intent.type);assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0);assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION);val uri=intent.clipData!!.getItemAt(0).uri;assertEquals("content",uri.scheme);assertEquals(c.packageName+".files",uri.authority);assertArrayEquals(byteArrayOf(1,2,3),c.contentResolver.openInputStream(uri)!!.use{it.readBytes()});assertEquals(Intent.ACTION_CHOOSER,Intent.createChooser(intent,"share").action)}
    @Test fun privateFilesAreNotExposedByProvider(){val c=RuntimeEnvironment.getApplication();val file=File(c.filesDir,"private.txt").apply{writeText("private")};assertThrows(IllegalArgumentException::class.java){FileProvider.getUriForFile(c,c.packageName+".files",file)}}
    @Test fun mediaPermissionsAreVersionSpecificAndNotBroadStorage(){val c=RuntimeEnvironment.getApplication();val permissions=FileCatalog(c).permissions().toSet();assertTrue("android.permission.READ_MEDIA_IMAGES" in permissions);assertTrue("android.permission.READ_MEDIA_VISUAL_USER_SELECTED" in permissions);assertFalse("android.permission.MANAGE_EXTERNAL_STORAGE" in permissions);assertFalse("android.permission.READ_EXTERNAL_STORAGE" in permissions)}
    @Test @Config(sdk=[29]) fun android10UsesScopedLegacyMediaReadOnly(){val permissions=FileCatalog(RuntimeEnvironment.getApplication()).permissions().toList();assertEquals(listOf("android.permission.READ_EXTERNAL_STORAGE"),permissions)}
    @Test fun cameraRowAndPixelStridePaddingCannotCorruptQr(){val payload=PairingQr("192.168.49.1",33045,"b".repeat(32),System.currentTimeMillis()/1000+600).encode();val bitmap=pairingBitmap(payload);val w=bitmap.width;val h=bitmap.height;val stride=w*2+16;val bytes=java.nio.ByteBuffer.allocate(5+stride*h);bytes.position(5);for(y in 0 until h)for(x in 0 until w)bytes.put(5+y*stride+x*2,if(bitmap.getPixel(x,y)==Color.BLACK)0.toByte() else 255.toByte());val y=ByteArray(w*h);copyLuminance(bytes,w,h,stride,2,y);val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(PlanarYUVLuminanceSource(y,w,h,0,0,w,h,false))));assertEquals(payload,decoded.text)}

    @Test fun historyPersistsWithoutComposeAndClearingDoesNotDeleteFiles(){val c=RuntimeEnvironment.getApplication();val file=File(c.filesDir,"received-document.txt").apply{writeText("kept")};val store=EzLocalStore(c);store.saveHistory(emptyList());store.addHistory(HistoryEntry(id="fixture:0:completed",name="document.txt",bytes=4,total=4,duration=1.0,peer="fixture",incoming=true,status="completed",savedAt=file.toURI().toString()));assertEquals("document.txt",EzLocalStore(c).history().single().name);store.addHistory(store.history().single());assertEquals(1,store.history().size);store.saveHistory(emptyList());assertTrue(file.exists());assertTrue(EzLocalStore(c).history().isEmpty())}

}
