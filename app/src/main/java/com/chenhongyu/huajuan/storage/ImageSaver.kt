package com.chenhongyu.huajuan.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageSaver {
    private const val TAG = "ImageSaver"

    suspend fun saveBitmapToFilesDir(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): String {
        return try {
            val dir = File(context.filesDir, "ai_creations")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                fos.flush()
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to filesDir", e)
            throw e
        }
    }

    /**
     * Save to MediaStore (shared photos). Returns content Uri on success.
     */
    suspend fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        mimeType: String = "image/png",
        relativePath: String? = "Pictures/HuaJuan"
    ): Uri? {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
            }

            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, contentValues) ?: return null

            resolver.openOutputStream(uri).use { out ->
                if (out == null) return null
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                out.flush()
                if (!success) {
                    // cleanup
                    resolver.delete(uri, null, null)
                    return null
                }
            }

            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to MediaStore", e)
            return null
        }
    }

    /**
     * Save a bitmap to an explicit OutputStream. Helper.
     */
    fun saveBitmapToStream(bitmap: Bitmap, out: OutputStream, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 90): Boolean {
        return try {
            bitmap.compress(format, quality, out)
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapToStream failed", e)
            false
        }
    }
}

