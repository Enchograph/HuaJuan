package com.chenhongyu.huajuan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, AICreationEntity::class],
    version = 4, // bump database version to include ai_creations
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun aiCreationDao(): AICreationDao

    companion object {
        // 添加数据库迁移逻辑
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为conversations表添加systemPrompt列
                database.execSQL("ALTER TABLE conversations ADD COLUMN systemPrompt TEXT NOT NULL DEFAULT '你是一个AI助手'")
            }
        }

        // migration to create ai_creations table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_creations` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `username` TEXT,
                      `userSignature` TEXT,
                      `aiRoleName` TEXT,
                      `aiModelName` TEXT,
                      `promptHtml` TEXT NOT NULL,
                      `promptJson` TEXT,
                      `imageFileName` TEXT,
                      `width` INTEGER NOT NULL,
                      `height` INTEGER NOT NULL,
                      `status` TEXT NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      `updatedAt` INTEGER NOT NULL,
                      `extraJson` TEXT
                    )
                """.trimIndent())
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4) // 添加迁移
                .fallbackToDestructiveMigration() // 允许破坏性迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}