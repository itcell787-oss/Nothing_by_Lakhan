package com.example.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TrashItem(
    val id: String,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val size: Long,
    val deletedAt: Long,
    val isDirectory: Boolean
) {
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var d = size.toDouble()
            var idx = 0
            while (d >= 1024.0 && idx < units.size - 1) {
                d /= 1024.0
                idx++
            }
            return String.format(java.util.Locale.US, "%.1f %s", d, units[idx])
        }
}

class TrashManager(private val context: Context) {

    private val trashDir: File by lazy {
        File(context.filesDir, ".trash_bin").apply { mkdirs() }
    }

    private val metaFile: File by lazy {
        File(trashDir, "trash_meta.json")
    }

    suspend fun moveToTrash(path: String): Result<TrashItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(path)
            if (!source.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }

            val id = "trash_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val target = File(trashDir, id)

            val moved = if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true) && source.deleteRecursively()
            } else {
                source.copyTo(target, overwrite = true)
                source.delete()
            }

            if (!moved && !target.exists()) {
                return@withContext Result.failure(Exception("Failed to move to trash"))
            }

            val item = TrashItem(
                id = id,
                originalPath = source.absolutePath,
                trashPath = target.absolutePath,
                name = source.name,
                size = if (target.isDirectory) getFolderSize(target) else target.length(),
                deletedAt = System.currentTimeMillis(),
                isDirectory = target.isDirectory
            )

            saveItemMeta(item)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTrash(): List<TrashItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrashItem>()
        if (!metaFile.exists()) return@withContext emptyList()
        try {
            val jsonStr = metaFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val trashPath = obj.getString("trashPath")
                if (File(trashPath).exists()) {
                    list.add(
                        TrashItem(
                            id = obj.getString("id"),
                            originalPath = obj.getString("originalPath"),
                            trashPath = trashPath,
                            name = obj.getString("name"),
                            size = obj.getLong("size"),
                            deletedAt = obj.getLong("deletedAt"),
                            isDirectory = obj.getBoolean("isDirectory")
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list.sortedByDescending { it.deletedAt }
    }

    suspend fun restore(item: TrashItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) {
                removeItemMeta(item.id)
                return@withContext Result.failure(Exception("Item no longer in trash"))
            }

            val dest = File(item.originalPath)
            dest.parentFile?.mkdirs()

            val restored = if (trashFile.isDirectory) {
                trashFile.copyRecursively(dest, overwrite = true) && trashFile.deleteRecursively()
            } else {
                trashFile.copyTo(dest, overwrite = true)
                trashFile.delete()
            }

            if (restored || dest.exists()) {
                removeItemMeta(item.id)
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to restore item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(item: TrashItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (trashFile.exists()) {
                if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
            }
            removeItemMeta(item.id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val items = listTrash()
            for (item in items) {
                val f = File(item.trashPath)
                if (f.exists()) {
                    if (f.isDirectory) f.deleteRecursively() else f.delete()
                }
            }
            metaFile.delete()
            Result.success(items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    private fun saveItemMeta(item: TrashItem) {
        try {
            val array = if (metaFile.exists()) {
                JSONArray(metaFile.readText())
            } else {
                JSONArray()
            }
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalPath", item.originalPath)
                put("trashPath", item.trashPath)
                put("name", item.name)
                put("size", item.size)
                put("deletedAt", item.deletedAt)
                put("isDirectory", item.isDirectory)
            }
            array.put(obj)
            metaFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    private fun removeItemMeta(id: String) {
        try {
            if (!metaFile.exists()) return
            val array = JSONArray(metaFile.readText())
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") != id) {
                    newArray.put(obj)
                }
            }
            metaFile.writeText(newArray.toString())
        } catch (_: Exception) {}
    }
}
