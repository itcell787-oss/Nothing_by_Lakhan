package com.example.data

import android.os.Environment
import com.example.data.dao.BookmarkDao
import com.example.data.dao.DriveDao
import com.example.data.dao.TransferDao
import com.example.data.model.BookmarkEntity
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import com.example.data.model.TransferEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class ExplorerRepository(
    private val driveDao: DriveDao,
    private val bookmarkDao: BookmarkDao,
    private val transferDao: TransferDao
) {
    val allDrives: Flow<List<DriveEntity>> = driveDao.getAllDrives()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()

    suspend fun initializeDefaultDrivesIfEmpty(sampleMediaPath: String? = null) {
        val internalPath = try {
            Environment.getExternalStorageDirectory().absolutePath
        } catch (_: Exception) {
            "/storage/emulated/0"
        }

        // Clean up legacy System Root drive if present so default drives are strictly Internal Storage & System Partition
        driveDao.deleteDrivesByType(DriveType.LOCAL_SYSTEM_ROOT)

        val existingDrives = driveDao.getAllDrivesSnapshot()
        val hasInternal = existingDrives.any { it.type == DriveType.LOCAL_INTERNAL }
        val hasSystemPartition = existingDrives.any { it.type == DriveType.LOCAL_PARTITION }

        // Default Drive 1: Internal Storage
        if (!hasInternal) {
            driveDao.insertDrive(
                DriveEntity(
                    name = "Internal Storage",
                    type = DriveType.LOCAL_INTERNAL,
                    path = internalPath,
                    isMounted = true,
                    fsType = "f2fs/ext4"
                )
            )
        }

        // Default Drive 2: System Partition
        if (!hasSystemPartition) {
            driveDao.insertDrive(
                DriveEntity(
                    name = "System Partition",
                    type = DriveType.LOCAL_PARTITION,
                    path = if (File("/system").exists()) "/system" else "/",
                    isMounted = true,
                    fsType = "erofs/ext4"
                )
            )
        }

        // Add standard bookmarks
        bookmarkDao.insertBookmark(
            BookmarkEntity(name = "Downloads", path = "$internalPath/Download", isSystem = false)
        )
        bookmarkDao.insertBookmark(
            BookmarkEntity(name = "DCIM", path = "$internalPath/DCIM", isSystem = false)
        )
        if (sampleMediaPath != null) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(name = "Inspection Suite", path = sampleMediaPath, isSystem = false)
            )
        }
        bookmarkDao.insertBookmark(
            BookmarkEntity(name = "System Bin", path = "/system/bin", isSystem = true)
        )
        bookmarkDao.insertBookmark(
            BookmarkEntity(name = "System Etc", path = "/system/etc", isSystem = true)
        )
    }

    suspend fun addDrive(drive: DriveEntity): Long = driveDao.insertDrive(drive)

    suspend fun updateDrive(drive: DriveEntity) = driveDao.updateDrive(drive)

    suspend fun deleteDrive(drive: DriveEntity) = driveDao.deleteDrive(drive)

    suspend fun setMounted(id: Long, mounted: Boolean) = driveDao.setMounted(id, mounted)

    suspend fun addBookmark(name: String, path: String, isSystem: Boolean): Long {
        return bookmarkDao.insertBookmark(BookmarkEntity(name = name, path = path, isSystem = isSystem))
    }

    suspend fun removeBookmark(path: String) = bookmarkDao.deleteByPath(path)

    suspend fun logTransfer(task: TransferEntity): Long = transferDao.insertTransfer(task)

    suspend fun updateTransfer(task: TransferEntity) = transferDao.updateTransfer(task)

    suspend fun clearTransferHistory() = transferDao.clearHistory()
}
