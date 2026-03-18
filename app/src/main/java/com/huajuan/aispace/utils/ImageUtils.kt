package com.huajuan.aispace.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.huajuan.aispace.utils.debugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_SIDE = 1280
    
    /**
     * 将图片文件转换为base64编码的data URL
     */
    fun convertImageToDataUrl(context: Context, imagePath: String): String? {
        return try {
            val bitmap = if (imagePath.startsWith("content://")) {
                // 如果是内容URI，使用ContentResolver获取
                val uri = Uri.parse(imagePath)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                // 如果是文件路径，直接加载
                BitmapFactory.decodeFile(imagePath)
            }

            if (bitmap != null) {
                val scaled = downscale(bitmap, MAX_IMAGE_SIDE)
                val outputStream = ByteArrayOutputStream()
                // 压缩图片以减少大小
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                
                // 关闭流
                outputStream.close()
                
                // 返回data URL格式
                "data:image/jpeg;base64,$base64String"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "convertImageToDataUrl failed", e)
            null
        }
    }
    
    /**
     * 将图片URI转换为base64编码的data URL
     */
    fun convertUriToDataUrl(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap != null) {
                val scaled = downscale(bitmap, MAX_IMAGE_SIDE)
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                
                // 关闭流
                inputStream?.close()
                outputStream.close()
                
                // 返回data URL格式
                "data:image/jpeg;base64,$base64String"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "convertUriToDataUrl failed", e)
            null
        }
    }

    fun prepareImageUrl(context: Context, imageUri: String): String? {
        return try {
            if (imageUri.startsWith("http://") || imageUri.startsWith("https://") || imageUri.startsWith("data:")) {
                imageUri
            } else if (imageUri.startsWith("content://")) {
                convertUriToDataUrl(context, Uri.parse(imageUri))
            } else {
                convertImageToDataUrl(context, imageUri)
            }
        } catch (e: Exception) {
            Log.w(TAG, "prepareImageUrl failed", e)
            null
        }
    }

    private fun downscale(bitmap: Bitmap, maxSide: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest.toFloat()
        val newW = (width * scale).toInt().coerceAtLeast(1)
        val newH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
    
    /**
     * 从URI获取真实的文件路径
     */
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex >= 0) {
                        return@use it.getString(columnIndex)
                    }
                }
            }
            // 如果无法通过MediaStore获取路径，复制文件到应用私有目录
            copyFileToInternalStorage(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "copyFileToInternalStorage failed", e)
            null
        }
    }
    
    /**
     * 复制文件到内部存储
     */
    private fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(outputFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "getRealPathFromUri failed", e)
            null
        }
    }

    // 图片URL正则表达式模式 - 支持完整的presigned URL（包含URL编码的查询参数）
    private val IMAGE_URL_PATTERN = Pattern.compile(
        "(https?://[^\\s]+?\\.(jpg|jpeg|png|gif|webp|bmp|svg)(\\?[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )
    
    // 需要跳过自定义headers的域名（OSS/CDN等）
    private val SKIP_HEADERS_DOMAINS = listOf(
        "aliyuncs.com",
        "oss-cn-",
        "cos.ap-",
        "myqcloud.com",
        "amazonaws.com",
        "cloudfront.net",
        "cdn.",
        "bizyair"
    )

    /**
     * 从文本中提取图片URL列表
     */
    fun extractImageUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = IMAGE_URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val url = matcher.group(1)
            if (url != null) {
                urls.add(url)
            }
        }
        return urls
    }

    /**
     * 从文本中提取并移除图片URL，返回清理后的文本
     */
    fun extractAndCleanImageUrls(text: String): Pair<List<String>, String> {
        val urls = extractImageUrls(text)
        var cleanedText = text
        for (url in urls) {
            cleanedText = cleanedText.replace(url, "")
        }
        // 清理多余的空白字符
        cleanedText = cleanedText.trim().replace(Regex("\\n{3,}"), "\n\n")
        return Pair(urls, cleanedText)
    }

    /**
     * 检查文本是否只包含图片URL（没有其他内容）
     */
    fun isOnlyImageUrls(text: String): Boolean {
        val cleaned = text.trim().replace(Regex("\\s"), "")
        if (cleaned.isEmpty()) return false
        
        val urls = extractImageUrls(text)
        if (urls.isEmpty()) return false
        
        // 检查所有非空白字符是否都是URL
        val nonUrlText = text.replace(Regex("https?://[^\\s]+"), "").trim()
        return nonUrlText.isEmpty()
    }

    /**
     * 下载图片并返回图片数据（字节数组）
     * @param imageUrl 图片URL
     * @return 图片字节数组，失败返回null
     */
    suspend fun downloadImageBytes(imageUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val urlObj = java.net.URL(imageUrl)
                val host = urlObj.host.lowercase()
                
                val shouldSkipHeaders = SKIP_HEADERS_DOMAINS.any { domain -> 
                    host.contains(domain.lowercase()) 
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBuilder = Request.Builder().url(imageUrl)
                
                if (!shouldSkipHeaders) {
                    val referer = "https://$host/"
                    requestBuilder
                        .header("Referer", referer)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to download image: HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body ?: run {
                    Log.w(TAG, "Response body is null")
                    return@withContext null
                }

                body.byteStream().use { input ->
                    input.readBytes()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image", e)
                null
            }
        }
    }

    suspend fun downloadImage(context: Context, imageUrl: String, prefix: String = "ai_img"): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bytes = downloadImageBytes(imageUrl)
                if (bytes == null) {
                    return@withContext null
                }
                
                val timestamp = System.currentTimeMillis()
                val filename = "${prefix}_${timestamp}.png"
                val imagesDir = File(context.getExternalFilesDir(null), "ai_creations/images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val outputFile = File(imagesDir, filename)

                FileOutputStream(outputFile).use { fos ->
                    fos.write(bytes)
                }

                debugLog(TAG) { "Image saved to ${outputFile.absolutePath}" }
                outputFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image", e)
                null
            }
        }
    }

    /**
     * 批量下载图片
     * @param context Android上下文
     * @param imageUrls 图片URL列表
     * @param prefix 文件名前缀
     * @return 成功下载的本地文件路径列表
     */
    suspend fun downloadImages(context: Context, imageUrls: List<String>, prefix: String = "ai_img"): List<String> {
        val downloadedPaths = mutableListOf<String>()
        for ((index, url) in imageUrls.withIndex()) {
            val path = downloadImage(context, url, "${prefix}_$index")
            if (path != null) {
                downloadedPaths.add(path)
            }
        }
        return downloadedPaths
    }
}
