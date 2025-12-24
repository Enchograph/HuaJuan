package com.chenhongyu.huajuan.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object ImageUtils {
    
    /**
     * 将图片文件转换为base64编码的data URL
     */
    fun convertImageToDataUrl(context: Context, imagePath: String): String? {
        return try {
            val bitmap = if (imagePath.startsWith("content://")) {
                // 如果是内容URI，使用ContentResolver获取
                val uri = Uri.parse(imagePath)
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                // 如果是文件路径，直接加载
                BitmapFactory.decodeFile(imagePath)
            }
            
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                // 压缩图片以减少大小
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
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
            e.printStackTrace()
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
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
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
            e.printStackTrace()
            null
        }
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
            e.printStackTrace()
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
            e.printStackTrace()
            null
        }
    }
}