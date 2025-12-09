package com.chenhongyu.huajuan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 3, // 更新数据库版本
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        // 添加数据库迁移逻辑
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为conversations表添加systemPrompt列
                database.execSQL("ALTER TABLE conversations ADD COLUMN systemPrompt TEXT NOT NULL DEFAULT '你是一个AI助手'")
            }
        }
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_2_3) // 添加迁移
                .fallbackToDestructiveMigration() // 允许破坏性迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}