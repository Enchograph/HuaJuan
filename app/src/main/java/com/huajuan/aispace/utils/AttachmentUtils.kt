package com.huajuan.aispace.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.huajuan.aispace.R
import com.huajuan.aispace.data.FileAttachment
import com.huajuan.aispace.i18n.LocalizedResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object AttachmentUtils {
    private const val TAG = "AttachmentUtils"
    private const val MAX_TEXT_BYTES = 200 * 1024

    fun buildAttachmentFromUri(context: Context, uri: Uri): FileAttachment? {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)
            val meta = queryMeta(resolver, uri)
            val displayName = resolveDisplayName(context, uri, mimeType, meta.first)
            val sizeBytes = meta.second
            val localPath = copyToCache(context, uri, displayName)
            FileAttachment(
                uri = uri.toString(),
                localPath = localPath,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                isImage = mimeType?.startsWith("image/") == true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build attachment from uri", e)
            null
        }
    }

    fun formatFileSize(context: Context?, sizeBytes: Long?): String {
        if (sizeBytes == null || sizeBytes < 0) {
            return context?.let { LocalizedResources.getString(it, R.string.unknown_size) }
                ?: "Unknown size"
        }
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.1f GB", gb)
    }

    suspend fun buildAttachmentPrompt(context: Context, attachment: FileAttachment): String = withContext(Dispatchers.IO) {
        val sizeLabel = formatFileSize(context, attachment.sizeBytes)
        val typeLabel = attachment.mimeType ?: "application/octet-stream"
        val header = "${LocalizedResources.getString(context, R.string.chat_attachment)}: ${attachment.displayName} ($typeLabel, $sizeLabel)"
        val text = readTextIfSupported(context, attachment)
        if (text.isNullOrBlank()) header else "$header\n\n$text"
    }

    suspend fun saveAttachmentToDownloads(context: Context, attachment: FileAttachment): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = attachment.displayName.ifBlank { "attachment_${System.currentTimeMillis()}" }
        val mimeType = attachment.mimeType ?: "application/octet-stream"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/HuaJuan")
        }

        val target = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val outUri = resolver.insert(target, values) ?: return@withContext null

        try {
            resolver.openOutputStream(outUri)?.use { out ->
                openAttachmentInputStream(context, attachment)?.use { input ->
                    input.copyTo(out)
                } ?: run {
                    resolver.delete(outUri, null, null)
                    return@withContext null
                }
            } ?: run {
                resolver.delete(outUri, null, null)
                return@withContext null
            }
        } catch (e: Exception) {
            resolver.delete(outUri, null, null)
            Log.w(TAG, "saveAttachmentToDownloads failed", e)
            return@withContext null
        }

        outUri
    }

    private fun queryMeta(resolver: ContentResolver, uri: Uri): Pair<String?, Long?> {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else null
                    return@use Pair(name, size)
                }
            }
            Pair(null, null)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    private fun resolveDisplayName(
        context: Context,
        uri: Uri,
        mimeType: String?,
        queriedName: String?
    ): String {
        val candidates = listOfNotNull(
            queriedName,
            runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull(),
            decodeUriFileName(uri)
        )
        val resolved = candidates
            .mapNotNull { sanitizeDisplayName(it) }
            .firstOrNull()

        if (resolved != null) {
            return ensureExtension(resolved, mimeType)
        }

        val fallbackExt = extensionFromMimeType(mimeType)?.let { ".$it" }.orEmpty()
        return "attachment_${System.currentTimeMillis()}$fallbackExt"
    }

    private fun decodeUriFileName(uri: Uri): String? {
        val rawCandidates = buildList {
            uri.lastPathSegment?.let(::add)
            uri.pathSegments.lastOrNull()?.let(::add)
            uri.path?.substringAfterLast('/')?.let(::add)
            uri.toString().substringBefore('?').substringBefore('#').substringAfterLast('/').let(::add)
        }
        return rawCandidates
            .asSequence()
            .map { candidate ->
                runCatching { URLDecoder.decode(candidate, StandardCharsets.UTF_8.name()) }.getOrDefault(candidate)
            }
            .mapNotNull { sanitizeDisplayName(it) }
            .firstOrNull()
    }

    private fun sanitizeDisplayName(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .trim('.')
        if (trimmed.isBlank() || trimmed == ":" || trimmed.contains("://")) return null
        if (trimmed.startsWith("attachment_") && !trimmed.contains('.')) return null
        return trimmed.takeIf { candidate ->
            candidate.any { !it.isDigit() } && candidate.any { it.isLetterOrDigit() }
        }
    }

    private fun ensureExtension(fileName: String, mimeType: String?): String {
        if (fileName.substringAfterLast('.', "").isNotBlank()) return fileName
        val ext = extensionFromMimeType(mimeType) ?: return fileName
        return "$fileName.$ext"
    }

    private fun extensionFromMimeType(mimeType: String?): String? {
        val normalized = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(normalized)
            ?.takeIf { it.isNotBlank() }
    }

    private fun copyToCache(context: Context, uri: Uri, displayName: String): String? {
        return try {
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val outFile = File(context.cacheDir, "attachment_${System.currentTimeMillis()}_$safeName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "copyToCache failed", e)
            null
        }
    }

    private fun openAttachmentInputStream(context: Context, attachment: FileAttachment): InputStream? {
        return try {
            if (!attachment.localPath.isNullOrBlank()) {
                File(attachment.localPath).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(attachment.uri))
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun readTextIfSupported(context: Context, attachment: FileAttachment): String? {
        val mime = attachment.mimeType ?: return null
        if (!mime.startsWith("text/")) return null
        val size = attachment.sizeBytes
        if (size != null && size > MAX_TEXT_BYTES) return null
        return withContext(Dispatchers.IO) {
            try {
                openAttachmentInputStream(context, attachment)?.use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size > MAX_TEXT_BYTES) return@withContext null
                    String(bytes)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
