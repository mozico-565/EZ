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
    @Test fun generatedQrIsReadableByIndependentZxingDecoder(){val payload=PairingQr("192.168.49.1",33045,"a".repeat(32),System.currentTimeMillis()/1000+600).encode();val bitmap=pairingBitmap(payload);val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);val result=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))));assertEquals(payload,result.text);assertEquals(payload,PairingQr.decode(result.text).encode())}
    @Test fun sharingGrantsOnlyReadAccessToControlledContentUri(){val c=RuntimeEnvironment.getApplication();val file=File(c.cacheDir,"exports/sample.apk").apply{parentFile!!.mkdirs();writeBytes(byteArrayOf(1,2,3))};val intent=ApkSharing.shareIntent(c,file);assertEquals(Intent.ACTION_SEND,intent.action);assertEquals(ApkSharing.APK_MIME,intent.type);assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0);assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION);val uri=intent.clipData!!.getItemAt(0).uri;assertEquals("content",uri.scheme);assertEquals(c.packageName+".files",uri.authority);assertArrayEquals(byteArrayOf(1,2,3),c.contentResolver.openInputStream(uri)!!.use{it.readBytes()});assertEquals(Intent.ACTION_CHOOSER,Intent.createChooser(intent,"share").action)}
    @Test fun privateFilesAreNotExposedByProvider(){val c=RuntimeEnvironment.getApplication();val file=File(c.filesDir,"private.txt").apply{writeText("private")};assertThrows(IllegalArgumentException::class.java){FileProvider.getUriForFile(c,c.packageName+".files",file)}}
    @Test fun mediaPermissionsAreVersionSpecificAndNotBroadStorage(){val c=RuntimeEnvironment.getApplication();val permissions=FileCatalog(c).permissions().toSet();assertTrue("android.permission.READ_MEDIA_IMAGES" in permissions);assertTrue("android.permission.READ_MEDIA_VISUAL_USER_SELECTED" in permissions);assertFalse("android.permission.MANAGE_EXTERNAL_STORAGE" in permissions);assertFalse("android.permission.READ_EXTERNAL_STORAGE" in permissions)}
    @Test @Config(sdk=[29]) fun android10UsesScopedLegacyMediaReadOnly(){val permissions=FileCatalog(RuntimeEnvironment.getApplication()).permissions().toList();assertEquals(listOf("android.permission.READ_EXTERNAL_STORAGE"),permissions)}
}
