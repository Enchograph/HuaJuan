package com.huajuan.aispace.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE id = :id")
    fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE sourceUrl = :sourceUrl LIMIT 1")
    fun getImageBySourceUrl(sourceUrl: String): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(image: ImageEntity): Long

    @Query("DELETE FROM images WHERE id = :id")
    fun deleteImageById(id: String): Int

    @Query("DELETE FROM images WHERE createdAt < :timestamp")
    fun deleteOldImages(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM images")
    fun getImageCount(): Int
}
