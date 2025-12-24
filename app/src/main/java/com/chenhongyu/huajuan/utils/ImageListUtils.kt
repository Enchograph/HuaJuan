package com.chenhongyu.huajuan.utils

import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import com.chenhongyu.huajuan.ImageItem

/**
 * 获取图片列表的工具函数
 */
fun getImageList(context: Context): List<ImageItem> {
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
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn).toString()
                val name = cursor.getString(nameColumn) ?: "未知图片"
                val dateAdded = cursor.getLong(dateColumn)
                
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                imageList.add(
                    ImageItem(
                        id = id,
                        uri = uri.toString(),
                        displayName = name,
                        dateAdded = dateAdded
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return imageList
}