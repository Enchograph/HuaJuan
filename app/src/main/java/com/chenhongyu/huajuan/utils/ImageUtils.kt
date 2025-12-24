package com.chenhongyu.huajuan.utils

import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.chenhongyu.huajuan.ImageItem

/**
 * 获取相册图片列表的工具函数
 */
suspend fun getImageList(context: Context): List<ImageItem> {
    val imageList = mutableListOf<ImageItem>()
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    
    try {
        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                
                imageList.add(ImageItem(id.toString(), contentUri.toString(), name, date))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return imageList.take(10) // 只取最新的10张图片
}

/**
 * 检查是否有读取媒体图片的权限
 */
fun hasReadStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}