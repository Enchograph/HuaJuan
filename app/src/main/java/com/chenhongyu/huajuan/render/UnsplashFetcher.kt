package com.chenhongyu.huajuan.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

object UnsplashFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch a random portrait image from Unsplash Source and decode into a Bitmap.
     * widthPx is used to request an appropriately sized image; height will be computed as width * 16/9 to ensure portrait.
     * This function will retry transient failures a few times with exponential backoff.
     */
    @Throws(Exception::class)
    fun fetchRandomPortraitBitmap(context: Context, widthPx: Int): Bitmap {
        val heightPx = (widthPx * 16) / 9 // portrait-ish tall image
        val url = "https://source.unsplash.com/random/${widthPx}x${heightPx}/?portrait"

        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 500L

        while (true) {
            attempt++
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body ?: throw Exception("Empty response body from Unsplash")
                    val inputStream: InputStream = body.byteStream()
                    try {
                        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                        if (bmp == null) throw Exception("Failed to decode image from Unsplash response")
                        return bmp
                    } finally {
                        try { inputStream.close() } catch (_: Exception) {}
                    }
                } else {
                    // transient HTTP errors may be retried
                    if (attempt >= maxAttempts) {
                        throw Exception("Unsplash request failed: ${resp.code}")
                    }
                    // else fall through to sleep+retry
                }
            }

            try {
                Thread.sleep(backoffMs)
            } catch (_: InterruptedException) {}
            backoffMs *= 2
        }
    }
}
