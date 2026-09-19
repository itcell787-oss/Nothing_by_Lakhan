package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BookmarkDao
import com.example.data.dao.DriveDao
import com.example.data.dao.TransferDao
import com.example.data.model.BookmarkEntity
import com.example.data.model.DriveEntity
import com.example.data.model.TransferEntity

@Database(
    entities = [
        DriveEntity::class,
        BookmarkEntity::class,
        TransferEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driveDao(): DriveDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nothing_explorer.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
