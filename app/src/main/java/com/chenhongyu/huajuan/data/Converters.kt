package com.chenhongyu.huajuan.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromImageUrisJson(value: String?): List<String> {
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun toImageUrisJson(uris: List<String>?): String {
        return Gson().toJson(uris ?: emptyList<String>())
    }
}