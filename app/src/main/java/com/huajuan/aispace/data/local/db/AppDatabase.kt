package com.huajuan.aispace.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        ImageEntity::class,
        KnowledgeBaseEntity::class,
        KnowledgeItemEntity::class,
        KnowledgeChunkEntity::class,
        KnowledgeEmbeddingEntity::class,
        ConversationKbSelectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun imageDao(): ImageDao
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "huajuan_public_app.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
