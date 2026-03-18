package com.huajuan.aispace

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import com.huajuan.aispace.data.repository.internal.AppPreferenceStore
import com.huajuan.aispace.i18n.AppLocaleManager
import java.util.concurrent.TimeUnit

class HuaJuanApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLocaleManager.applyAppLanguage(this, AppPreferenceStore(this).getLanguage())
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .connectTimeout(30, TimeUnit.SECONDS)
                                .readTimeout(60, TimeUnit.SECONDS)
                                .addNetworkInterceptor(OssRefererInterceptor())
                                .build()
                        }
                    )
                )
            }
            .crossfade(true)
            .build()
    }

    class OssRefererInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            
            val newRequest = if (isOssUrl(url)) {
                val host = request.url.host
                request.newBuilder()
                    .header("Referer", "https://$host/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build()
            } else {
                request
            }
            
            return chain.proceed(newRequest)
        }

        private fun isOssUrl(url: String): Boolean {
            return url.contains("oss-") || 
                   url.contains("aliyuncs.com") || 
                   url.contains("amazonaws.com") ||
                   url.contains("cloudfront.net") ||
                   url.contains("myqcloud.com") ||
                   url.contains("cdn.bcebos.com")
        }
    }

    companion object {
        lateinit var instance: HuaJuanApplication
            private set
    }
}
