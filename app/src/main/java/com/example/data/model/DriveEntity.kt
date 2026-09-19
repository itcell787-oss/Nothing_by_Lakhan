package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DriveType {
    LOCAL_INTERNAL,
    LOCAL_SYSTEM_ROOT,
    LOCAL_PARTITION,
    NETWORK_SMB,
    NETWORK_FTP,
    CLOUD_GDRIVE,
    CLOUD_DROPBOX,
    CLOUD_ONEDRIVE;

    val isNetworkDrive: Boolean
        get() = this == NETWORK_SMB || this == NETWORK_FTP

    val isCloudDrive: Boolean
        get() = this == CLOUD_GDRIVE || this == CLOUD_DROPBOX || this == CLOUD_ONEDRIVE

    val displayName: String
        get() = when (this) {
            LOCAL_INTERNAL -> "Internal Storage"
            LOCAL_SYSTEM_ROOT -> "System Root"
            LOCAL_PARTITION -> "Storage Partition"
            NETWORK_SMB -> "Windows SMB"
            NETWORK_FTP -> "FTP Server"
            CLOUD_GDRIVE -> "Google Drive"
            CLOUD_DROPBOX -> "Dropbox"
            CLOUD_ONEDRIVE -> "OneDrive"
        }
}

@Entity(tableName = "mounted_drives")
data class DriveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: DriveType,
    val path: String, // local root path or remote base path
    val host: String = "",
    val port: Int = 0,
    val shareName: String = "",
    val username: String = "",
    val passwordEncrypted: String = "",
    val isMounted: Boolean = true,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val fsType: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
