package com.huajuan.aispace.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    @ColumnInfo(name = "imageData", typeAffinity = ColumnInfo.BLOB) val imageData: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageEntity
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "ImageEntity(id=$id, sourceUrl=$sourceUrl, createdAt=$createdAt)"
    }
}
