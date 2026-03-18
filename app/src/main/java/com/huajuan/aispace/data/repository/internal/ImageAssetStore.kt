package com.huajuan.aispace.data.repository.internal

import android.content.Context
import android.util.Base64
import android.util.Log
import com.huajuan.aispace.data.AppDatabase
import com.huajuan.aispace.data.ImageEntity
import com.huajuan.aispace.data.ImageGenerationResponsePayload
import com.huajuan.aispace.utils.ImageUtils
import com.huajuan.aispace.utils.debugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ImageAssetStore(
    private val context: Context,
    private val database: AppDatabase
) {
    private companion object {
        const val TAG = "ImageAssetStore"
    }

    suspend fun processAIImageResponse(responseText: String): Pair<List<String>, String> {
        val imageUrls = ImageUtils.extractImageUrls(responseText)
        if (imageUrls.isEmpty()) return Pair(emptyList(), responseText)

        val savedImageIds = downloadAndStoreImages(imageUrls)
        val cleanedText = ImageUtils.extractAndCleanImageUrls(responseText).second
        return Pair(savedImageIds, cleanedText)
    }

    suspend fun getImageData(imageId: String): ByteArray? = withContext(Dispatchers.IO) {
        database.imageDao().getImageById(imageId)?.imageData
    }

    suspend fun getImageUri(imageId: String): String? = withContext(Dispatchers.IO) {
        val imageData = database.imageDao().getImageById(imageId) ?: return@withContext null
        val tempFile = java.io.File(context.cacheDir, "image_$imageId.png")
        java.io.FileOutputStream(tempFile).use { fos ->
            fos.write(imageData.imageData)
        }
        tempFile.absolutePath
    }

    suspend fun getImageUris(imageIds: List<String>): List<String> =
        imageIds.mapNotNull { id -> getImageUri(id) }

    fun extractImageUrlsFromResponse(responseText: String): List<String> =
        ImageUtils.extractImageUrls(responseText)

    fun extractAndCleanImageUrls(responseText: String): Pair<List<String>, String> =
        ImageUtils.extractAndCleanImageUrls(responseText)

    suspend fun deleteImageById(imageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            database.imageDao().deleteImageById(imageId) > 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveImageGenerationPayload(payload: ImageGenerationResponsePayload): Pair<List<String>, String> {
        val urlIds = downloadAndStoreImages(payload.remoteUrls)
        val base64Ids = saveBase64Images(payload.base64Images)
        val imageIds = urlIds + base64Ids
        val text = payload.displayText.ifBlank {
            if (imageIds.isEmpty()) "" else payload.remoteUrls.joinToString("\n")
        }
        return Pair(imageIds, text)
    }

    private suspend fun downloadAndStoreImages(imageUrls: List<String>): List<String> {
        return withContext(Dispatchers.IO) {
            val imageDao = database.imageDao()
            val savedIds = mutableListOf<String>()
            for (url in imageUrls) {
                try {
                    val existingImage = imageDao.getImageBySourceUrl(url)
                    if (existingImage != null) {
                        savedIds.add(existingImage.id)
                        continue
                    }

                    val bytes = ImageUtils.downloadImageBytes(url)
                    if (bytes != null) {
                        val imageId = java.util.UUID.randomUUID().toString()
                        val imageEntity = ImageEntity(
                            id = imageId,
                            sourceUrl = url,
                            imageData = bytes,
                            createdAt = System.currentTimeMillis()
                        )
                        imageDao.insertImage(imageEntity)
                        savedIds.add(imageId)
                        debugLog(TAG) { "Image saved to database with id=$imageId" }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to download/store image from $url: ${e.message}")
                }
            }
            savedIds
        }
    }

    private suspend fun saveBase64Images(base64Images: List<String>): List<String> {
        return withContext(Dispatchers.IO) {
            val imageDao = database.imageDao()
            val savedIds = mutableListOf<String>()
            for (base64Image in base64Images) {
                try {
                    val normalized = base64Image.substringAfter("base64,", base64Image)
                    val bytes = Base64.decode(normalized, Base64.DEFAULT)
                    val imageId = java.util.UUID.randomUUID().toString()
                    imageDao.insertImage(
                        ImageEntity(
                            id = imageId,
                            sourceUrl = "inline:$imageId",
                            imageData = bytes,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    savedIds.add(imageId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to store base64 image: ${e.message}")
                }
            }
            savedIds
        }
    }
}
