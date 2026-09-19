package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_tasks")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val sourceUri: String,
    val destinationUri: String,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val status: String, // QUEUED, IN_PROGRESS, COMPLETED, FAILED
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
