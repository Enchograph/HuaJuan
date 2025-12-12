package com.chenhongyu.huajuan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageStorage {
    private const val DIR = "ai_creations"
    private const val IMAGES = "images"
    private const val THUMBS = "thumbs"

    private fun baseDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun imagesDir(context: Context): File {
        val d = File(baseDir(context), IMAGES)
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun thumbsDir(context: Context): File {
        val d = File(baseDir(context), THUMBS)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String = "img"): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${'$'}{prefix}_$timestamp.png"
        val file = File(imagesDir(context), filename)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            fos.flush()
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        } finally {
            try {
                fos?.close()
            } catch (ignored: Exception) {}
        }
    }

    fun saveThumbnail(context: Context, bitmap: Bitmap, sourceFilename: String, maxDim: Int = 320): String {
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val thumb = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val filename = "thumb_${File(sourceFilename).nameWithoutExtension}.webp"
        val file = File(thumbsDir(context), filename)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            thumb.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos)
            fos.flush()
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        } finally {
            try { fos?.close() } catch (ignored: Exception) {}
            if (!thumb.isRecycled) thumb.recycle()
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
