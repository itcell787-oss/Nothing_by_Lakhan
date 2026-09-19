package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DriveEntity
import com.example.data.model.DriveType
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {
    @Query("SELECT * FROM mounted_drives ORDER BY createdAt ASC")
    fun getAllDrives(): Flow<List<DriveEntity>>

    @Query("SELECT * FROM mounted_drives ORDER BY createdAt ASC")
    suspend fun getAllDrivesSnapshot(): List<DriveEntity>

    @Query("DELETE FROM mounted_drives WHERE type = :type")
    suspend fun deleteDrivesByType(type: DriveType)

    @Query("SELECT * FROM mounted_drives WHERE id = :id LIMIT 1")
    suspend fun getDriveById(id: Long): DriveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrive(drive: DriveEntity): Long

    @Update
    suspend fun updateDrive(drive: DriveEntity)

    @Delete
    suspend fun deleteDrive(drive: DriveEntity)

    @Query("UPDATE mounted_drives SET isMounted = :mounted WHERE id = :id")
    suspend fun setMounted(id: Long, mounted: Boolean)
}
