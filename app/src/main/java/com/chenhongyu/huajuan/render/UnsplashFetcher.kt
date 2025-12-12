package com.chenhongyu.huajuan.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

object UnsplashFetcher {
    private const val UNSPLASH_ACCESS_KEY = "66nq83i5qCueyRl_LR1yjoMEwk4m-EUDRXgoNqgGlE4"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Global caps to avoid oversized bitmaps (more aggressive)
    private const val MAX_REQUEST_WIDTH = 720            // pixels
    private const val MAX_DECODE_WIDTH = 1280            // pixels
    private const val MAX_DECODE_HEIGHT = 1280           // pixels
    private const val MAX_DECODE_AREA = 1280 * 1280      // ~1.6MP

    /**
     * Fetch a random portrait image from Unsplash API and decode into a Bitmap safely.
     * Decoding uses sampling and post-scale to ensure the final bitmap fits under platform limits.
     */
    @Throws(Exception::class)
    fun fetchRandomPortraitBitmap(widthPx: Int): Bitmap {
        val targetWidth = min(widthPx, MAX_REQUEST_WIDTH)
        val targetHeight = (targetWidth * 16) / 9

        val url = "https://api.unsplash.com/photos/random?" +
                "query=portrait,landscape,abstract,nature,art,scenery,mountain,forest,ocean&orientation=portrait&w=$targetWidth&h=$targetHeight"

        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 500L

        while (true) {
            attempt++
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Client-ID $UNSPLASH_ACCESS_KEY")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body ?: throw Exception("Empty response body from Unsplash")
                    val responseBodyString = body.string()

                    val jsonObject = JSONObject(responseBodyString)
                    val urlsObject = jsonObject.getJSONObject("urls")
                    val imageUrl = when {
                        urlsObject.has("regular") -> urlsObject.getString("regular")
                        urlsObject.has("small") -> urlsObject.getString("small")
                        else -> urlsObject.getString("thumb")
                    }

                    val sizedUrl = if (imageUrl.contains("?")) {
                        "$imageUrl&w=$targetWidth&h=$targetHeight&fit=max"
                    } else {
                        "$imageUrl?w=$targetWidth&h=$targetHeight&fit=max"
                    }

                    val imageReq = Request.Builder().url(sizedUrl).get().build()
                    client.newCall(imageReq).execute().use { imageResp ->
                        if (imageResp.isSuccessful) {
                            val imageBody = imageResp.body ?: throw Exception("Empty image response body from Unsplash")
                            val inputStream: InputStream = imageBody.byteStream()
                            try {
                                val bytes = inputStream.readBytesSafe()

                                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                                val cappedTargetW = min(targetWidth, MAX_DECODE_WIDTH)
                                val cappedTargetH = min(targetHeight, MAX_DECODE_HEIGHT)
                                val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, cappedTargetW, cappedTargetH)

                                val decodeOptions = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                    inSampleSize = sampleSize
                                }
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                                    ?: throw Exception("Failed to decode image from Unsplash response")

                                val safeBmp1 = ensureMaxBitmapSize(bmp, maxWidth = MAX_DECODE_WIDTH, maxHeight = MAX_DECODE_HEIGHT)
                                val safeBmp2 = ensureMaxArea(safeBmp1, MAX_DECODE_AREA)
                                return safeBmp2
                            } finally {
                                try { inputStream.close() } catch (_: Exception) {}
                            }
                        } else {
                            throw Exception("Unsplash image download failed: ${imageResp.code}")
                        }
                    }
                } else {
                    if (attempt >= maxAttempts) {
                        throw Exception("Unsplash API request failed: ${resp.code} - ${resp.message}")
                    }
                }
            }

            try { Thread.sleep(backoffMs) } catch (_: InterruptedException) {}
            backoffMs *= 2
        }
    }

    // --- Helpers ---

    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > targetHeight || srcWidth > targetWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) > targetHeight || (halfWidth / inSampleSize) > targetWidth) {
                inSampleSize *= 2
            }
        }
        return if (inSampleSize < 1) 1 else inSampleSize
    }

    private fun InputStream.readBytesSafe(): ByteArray {
        val buffer = ByteArray(8 * 1024)
        val output = ByteArrayOutputStream()
        var read: Int
        while (true) {
            read = this.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun ensureMaxBitmapSize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxWidth && h <= maxHeight) return bitmap

        val widthRatio = w.toFloat() / maxWidth.toFloat()
        val heightRatio = h.toFloat() / maxHeight.toFloat()
        val scale = 1f / maxOf(widthRatio, heightRatio)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun ensureMaxArea(bitmap: Bitmap, maxArea: Int): Bitmap {
        val area = bitmap.width * bitmap.height
        if (area <= maxArea) return bitmap
        val scale = kotlin.math.sqrt(maxArea.toFloat() / area.toFloat())
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}