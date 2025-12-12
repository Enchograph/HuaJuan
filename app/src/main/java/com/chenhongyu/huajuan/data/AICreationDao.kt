package com.chenhongyu.huajuan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AICreationDao {
    @Query("SELECT * FROM ai_creations ORDER BY createdAt DESC")
    fun getAllCreations(): Flow<List<AICreationEntity>>

    @Query("SELECT * FROM ai_creations WHERE id = :id")
    fun getCreationById(id: String): AICreationEntity?

    @Insert
    fun insertCreation(entity: AICreationEntity): Long

    @Update
    fun updateCreation(entity: AICreationEntity): Int

    @Query("DELETE FROM ai_creations WHERE id = :id")
    fun deleteCreationById(id: String): Int
}

