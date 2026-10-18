package com.ezshare.app

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

object ApkSharing {
    const val APK_MIME="application/vnd.android.package-archive"
    /** Only installed packages known to be single APK are exported; no lone base split. */
    fun export(context:Context,packageName:String=context.packageName):File{
        val pm=context.packageManager;val installed=pm.getPackageInfo(packageName,PackageManager.GET_SIGNING_CERTIFICATES)
        val app=installed.applicationInfo?:error("بيانات التطبيق غير متاحة")
        require(app.splitSourceDirs.isNullOrEmpty()&&installed.splitNames.isNullOrEmpty()){"هذا التطبيق حزمة مجزأة؛ لا يمكن مشاركة base.apk وحده. اختر APK كاملًا من مصدر موثوق."}
        val original=File(app.sourceDir);require(original.isFile&&original.canRead()){"APK غير متاح للقراءة"}
        val directory=File(context.cacheDir,"exports").apply{mkdirs()};val destination=File(directory,"${packageName}-${installed.longVersionCode}.apk")
        // Atomic copy on IO dispatcher. A bounded buffer handles large distributable packages.
        val temp=File.createTempFile("export-",".tmp",directory)
        try{original.inputStream().use{input->temp.outputStream().use{input.copyTo(it,256*1024)}}
            val archive=pm.getPackageArchiveInfo(temp.path,PackageManager.GET_SIGNING_CERTIFICATES)?:error("APK غير صالح")
            require(archive.packageName==packageName&&archive.longVersionCode==installed.longVersionCode){"إصدار أو حزمة APK غير مطابقة"}
            fun signatures(info:android.content.pm.PackageInfo)=info.signingInfo?.apkContentsSigners?.map{MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toList()}?.toSet().orEmpty()
            val expected=signatures(installed);require(expected.isNotEmpty()&&expected==signatures(archive)){"توقيع APK غير مطابق"}
            require(temp.length()==original.length()){"نسخة APK غير مكتملة"}
            if(destination.exists())require(destination.delete()){"تعذّر تحديث نسخة المشاركة"};require(temp.renameTo(destination)){"تعذّر تجهيز APK"};return destination
        }finally{temp.delete()}
    }
    fun shareIntent(context:Context,file:File):Intent {
        val uri=FileProvider.getUriForFile(context,context.packageName+".files",file)
        return Intent(Intent.ACTION_SEND).setType(APK_MIME).putExtra(Intent.EXTRA_STREAM,uri).setClipData(ClipData.newRawUri("APK",uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    fun share(context:Context,file:File){context.startActivity(Intent.createChooser(shareIntent(context,file),"مشاركة تطبيق EZ").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
    /** Do not claim a release URL exists. Obtain and verify it only on explicit user request. */
    fun officialLink(version:String):String{
        require(version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))){"لم يُنشر إصدار رسمي لهذا البناء"}
        val conn=URL("https://api.github.com/repos/mozico-565/EZ/releases/tags/v$version").openConnection() as HttpURLConnection
        conn.connectTimeout=6000;conn.readTimeout=6000;conn.setRequestProperty("Accept","application/vnd.github+json")
        try{require(conn.responseCode==200){"لا يوجد رابط إصدار رسمي متاح للعامة الآن. يمكنك مشاركة APK بدون إنترنت."};val body=conn.inputStream.bufferedReader().use{it.readText().also{t->require(t.length<=1_000_000)}};val release=JSONObject(body)
            require(!release.getBoolean("draft")&&!release.getBoolean("prerelease")&&release.getString("tag_name")=="v$version"){"الإصدار الرسمي لم يُنشر بعد"}
            val url=release.getString("html_url");require(url=="https://github.com/mozico-565/EZ/releases/tag/v$version"){"رابط إصدار غير موثوق"};return url
        }finally{conn.disconnect()}
    }
}
