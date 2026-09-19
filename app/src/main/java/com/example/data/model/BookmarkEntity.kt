package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val isSystem: Boolean = false,
    val isDirectory: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
