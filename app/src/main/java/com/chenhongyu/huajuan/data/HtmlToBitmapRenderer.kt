package com.chenhongyu.huajuan.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object HtmlToBitmapRenderer {
    /**
     * Render HTML to a bitmap of given width and height. Must be called from a coroutine; this will
     * switch to main thread internally to create an offscreen WebView and capture its drawing.
     */
    suspend fun render(context: Context, html: String, width: Int, height: Int): Bitmap =
        suspendCancellableCoroutine { cont ->
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                try {
                    val webView = WebView(context)
                    webView.settings.apply {
                        javaScriptEnabled = false // templates shouldn't need JS; safer to disable
                        // other settings can be tuned if needed
                    }
                    webView.layoutParams = ViewGroup.LayoutParams(width, height)
                    // measure & layout
                    webView.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    webView.layout(0, 0, width, height)

                    val client = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            try {
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                webView.draw(canvas)
                                // cleanup
                                try { webView.stopLoading() } catch (_: Exception) {}
                                try { webView.destroy() } catch (_: Exception) {}
                                if (!cont.isCompleted) cont.resume(bitmap)
                            } catch (e: Exception) {
                                if (!cont.isCompleted) cont.resumeWithException(e)
                            }
                        }
                    }

                    webView.webViewClient = client

                    // load HTML
                    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)

                    // handle cancellation
                    cont.invokeOnCancellation {
                        try {
                            webView.stopLoading()
                            try { webView.destroy() } catch (_: Exception) {}
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }
}
