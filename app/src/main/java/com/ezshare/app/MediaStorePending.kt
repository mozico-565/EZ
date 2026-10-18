package com.ezshare.app

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import com.ezshare.core.Metadata
import com.ezshare.core.PendingFile
import com.ezshare.core.uniqueFilename
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

/** Android 10+: app-owned Downloads need no storage write permission or tree grant.
 * BatchEngine calls commit only after closing the stream and verifying wire SHA-256.
 * MediaProvider allocates a new row/file (never an existing file for truncation).
 */
class MediaStorePending(context: Context, metadata: Metadata,
    private val onOpen: (Closeable) -> Unit = {}): PendingFile {
    private val resolver = context.contentResolver
    private val digest = MessageDigest.getInstance("SHA-256")
    val uri: Uri
    override val output: OutputStream
    private var committed = false
    private var aborted = false

    init {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val relative = Environment.DIRECTORY_DOWNLOADS + "/EZ/"
        val name = uniqueFilename(metadata.name) { candidate ->
            resolver.query(collection, arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(relative, candidate), null)?.use { it.moveToFirst() }
                ?: throw IOException("تعذّر فحص أسماء التنزيلات")
        }
        uri = resolver.insert(collection, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, metadata.mime.ifBlank { "application/octet-stream" })
            put(MediaStore.Downloads.RELATIVE_PATH, relative)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }) ?: throw IOException("تعذّر إنشاء ملف في Download/EZ")
        try {
            output = DigestOutputStream(resolver.openOutputStream(uri, "w")
                ?: throw IOException("تعذّرت الكتابة في التنزيلات"), digest)
            onOpen(output)
        } catch(e: Exception) {
            runCatching { resolver.delete(uri, null, null) }.onFailure { e.addSuppressed(it) }
            throw e
        }
    }

    override fun commit(): String {
        check(!aborted && !committed)
        val expected = digest.digest()
        val actual = MessageDigest.getInstance("SHA-256")
        (resolver.openInputStream(uri) ?: throw IOException("تعذّر قراءة الملف للتحقق")).use { input ->
            onOpen(input)
            val buffer = ByteArray(64 * 1024)
            while(true) { val n = input.read(buffer); if(n < 0) break; if(n > 0) actual.update(buffer, 0, n) }
        }
        if(!MessageDigest.isEqual(expected, actual.digest())) throw IOException("فشل التحقق من الملف المحفوظ")
        if(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) != 1)
            throw IOException("تعذّر نشر الملف المتحقق منه")
        committed = true
        return uri.toString()
    }

    override fun abort() {
        if(committed || aborted) return
        runCatching { output.close() }
        if(resolver.delete(uri, null, null) != 1) throw IOException("Incomplete file cleanup failed; entry remains pending")
        aborted = true
    }
}

fun receivedName(context: Context, savedAt: String, fallback: String): String = runCatching {
    context.contentResolver.query(Uri.parse(savedAt), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if(it.moveToFirst()) it.getString(0) else null } ?: fallback
}.getOrDefault(fallback)

fun receivedIntent(context: Context, entry: HistoryEntry, share: Boolean): Intent {
    require(entry.incoming && entry.status == "completed" && entry.savedAt.isNotBlank())
    val uri = Uri.parse(entry.savedAt)
    require(uri.scheme == "content") { "لا يوجد رابط محلي صالح لهذا الملف" }
    // Ensure an expired/revoked or deleted URI produces a truthful error before launching.
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {} ?: throw IOException("الملف غير متاح")
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    return (if(share) Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
        else Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)).apply {
        clipData = ClipData.newRawUri(entry.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun openReceived(context: Context, entry: HistoryEntry, share: Boolean) {
    runCatching {
        val intent = receivedIntent(context, entry, share)
        context.startActivity(if(share) Intent.createChooser(intent, "مشاركة الملف") else intent)
    }.onFailure { Toast.makeText(context, "تعذّر فتح الملف؛ ربما حُذف أو لا يوجد تطبيق مناسب.", Toast.LENGTH_LONG).show() }
}
