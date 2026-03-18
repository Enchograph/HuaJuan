package com.huajuan.aispace.utils

import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import android.util.Log
import com.huajuan.aispace.R
import com.huajuan.aispace.data.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ImageListUtils"

/**
 * 获取图片列表的工具函数
 */
suspend fun getImageList(context: Context, maxItems: Int = 300): List<ImageItem> =
    withContext(Dispatchers.IO) {
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

                while (cursor.moveToNext() && imageList.size < maxItems) {
                    val id = cursor.getLong(idColumn).toString()
                    val name = cursor.getString(nameColumn) ?: context.getString(R.string.image_unknown_name)
                    val dateAdded = cursor.getLong(dateColumn)

                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    imageList.add(
                        ImageItem(
                            id = id,
                            uri = contentUri.toString(),
                            displayName = name,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getImageList failed", e)
        }

        imageList
    }
