package com.chenhongyu.huajuan.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeoutException

object HtmlRenderer {
    private const val TAG = "HtmlRenderer"

    // Safety limits to avoid OOM. Adjust to your needs / device memory.
    private const val MAX_WIDTH_PX = 2048
    private const val MAX_HEIGHT_PX = 8192
    private const val BYTES_PER_PIXEL = 4 // ARGB_8888
    private const val RENDER_TIMEOUT_MS = 15_000L

    /**
     * Render the provided HTML string into a Bitmap. Must be called from a coroutine.
     * This function will switch to the Main dispatcher for WebView operations.
     * Returns the Bitmap on success or throws an exception on failure.
     */
    suspend fun renderHtmlToBitmap(
        context: Context,
        html: String,
        targetWidthPx: Int,
        backgroundColor: Int = 0xFFFFFFFF.toInt(),
        scaleDownIfTooLarge: Boolean = true
    ): Bitmap = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Bitmap>()

        try {
            val webView = WebView(context)
            // Basic settings
            webView.settings.apply {
                javaScriptEnabled = true // enable JS: template uses marked.js and we use evaluateJavascript
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                // allow file access for local assets if needed
                allowFileAccess = true
                // allowMixedContent for loading remote fonts/resources if needed
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
            }

            webView.isVerticalScrollBarEnabled = false
            webView.isHorizontalScrollBarEnabled = false

            // Fallback handler: timeout
            val mainHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(TimeoutException("WebView render timeout"))
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.removeAllViews()
                        webView.destroy()
                    } catch (_: Throwable) {
                    }
                }
            }
            mainHandler.postDelayed(timeoutRunnable, RENDER_TIMEOUT_MS)

            webView.webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    // WebView's renderer process crashed; fail fast and cleanup
                    try {
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(RuntimeException("WebView renderer process gone: ${detail?.didCrash()}") )
                        }
                    } catch (_: Throwable) {}
                    try {
                        view?.removeAllViews()
                        view?.destroy()
                    } catch (_: Throwable) {}
                    // Returning true means we handled it and WebView is cleaned up
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        // get content height (in CSS px) and devicePixelRatio
                        view?.evaluateJavascript(
                            "(function(){return {h: Math.max(document.body.scrollHeight, document.documentElement.scrollHeight), d: window.devicePixelRatio||1};})()"
                        ) { result ->
                            try {
                                if (result == null) {
                                    deferred.completeExceptionally(RuntimeException("Empty JS result"))
                                } else {
                                    val cleaned = result.trim()
                                    // parse json result
                                    val obj = JSONObject(cleaned)
                                    val cssHeight = obj.optInt("h", 0)
                                    val dpr = obj.optDouble("d", 1.0).toFloat()

                                    val contentHeightPx = (cssHeight * dpr).toInt()

                                    var widthPx = targetWidthPx.coerceAtMost(MAX_WIDTH_PX)
                                    var heightPx = contentHeightPx.coerceAtMost(MAX_HEIGHT_PX)

                                    if (scaleDownIfTooLarge) {
                                        // if content is taller than max, scale down both dimensions proportionally
                                        if (contentHeightPx > MAX_HEIGHT_PX) {
                                            val scale = MAX_HEIGHT_PX.toFloat() / contentHeightPx.toFloat()
                                            widthPx = (widthPx * scale).toInt().coerceAtLeast(1)
                                            heightPx = MAX_HEIGHT_PX
                                        }
                                    } else {
                                        if (contentHeightPx > MAX_HEIGHT_PX) {
                                            deferred.completeExceptionally(RuntimeException("Rendered content too tall: $contentHeightPx px"))
                                            return@evaluateJavascript
                                        }
                                    }

                                    try {
                                        // measure & layout
                                        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                                        val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
                                        view.measure(widthSpec, heightSpec)
                                        view.layout(0, 0, widthPx, heightPx)

                                        // create bitmap and draw
                                        val estimatedBytes = widthPx.toLong() * heightPx.toLong() * BYTES_PER_PIXEL
                                        // crude safety check: avoid allocating beyond a large threshold (~200MB)
                                        if (estimatedBytes > 200L * 1024 * 1024) {
                                            deferred.completeExceptionally(RuntimeException("Bitmap too large to allocate: bytes=$estimatedBytes"))
                                            return@evaluateJavascript
                                        }

                                        // Force software layer to avoid GPU-related renderer crashes on some devices
                                        try {
                                            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                        } catch (_: Throwable) {}

                                        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bitmap)
                                        canvas.drawColor(backgroundColor)
                                        view.draw(canvas)

                                        // finish
                                        deferred.complete(bitmap)
                                    } catch (e: Throwable) {
                                        deferred.completeExceptionally(e)
                                    }
                                }
                            } catch (e: Throwable) {
                                deferred.completeExceptionally(e)
                            } finally {
                                // cleanup WebView to avoid leak
                                try {
                                    mainHandler.removeCallbacks(timeoutRunnable)
                                    view?.stopLoading()
                                    view?.loadUrl("about:blank")
                                    view?.removeAllViews()
                                    view?.destroy()
                                } catch (t: Throwable) {
                                    Log.w(TAG, "Error destroying WebView", t)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        deferred.completeExceptionally(e)
                    }
                }
            }

            // load HTML. Use baseUrl null; callers can embed asset references using file:///android_asset/
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)

        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
        }

        deferred.await()
    }

    /**
     * Java/callback friendly API. Runs on Main thread internally but returns on caller thread via callback.
     */
    fun renderHtmlToBitmapAsync(
        context: Context,
        html: String,
        targetWidthPx: Int,
        onResult: (result: Result<Bitmap>) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val bmp = renderHtmlToBitmap(context, html, targetWidthPx)
                onResult(Result.success(bmp))
            } catch (e: Throwable) {
                onResult(Result.failure(e))
            }
        }
    }
}
