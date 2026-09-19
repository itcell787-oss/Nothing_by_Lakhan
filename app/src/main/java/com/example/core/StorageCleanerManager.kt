package com.example.core

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

data class StorageCategoryBreakdown(
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val apksBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val totalScannedBytes: Long = 0L
)

data class DuplicateFileGroup(
    val size: Long,
    val items: List<FileItem>
) {
    val formattedSize: String get() = FileItem.fromFile(File(items.firstOrNull()?.path ?: "")).formattedSize
}

class StorageCleanerManager {

    suspend fun scanJunkFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val junkItems = mutableListOf<FileItem>()

        val junkExtensions = setOf("tmp", "temp", "log", "bak", "thumb", "cache", "dmp")
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)

        while (queue.isNotEmpty() && junkItems.size < 400) {
            val (dir, depth) = queue.removeFirst()
            val files = dir.listFiles() ?: continue

            for (f in files) {
                if (f.name.startsWith(".") && f.name != ".cache") continue

                if (f.isDirectory) {
                    if (depth < 6) {
                        val path = f.absolutePath
                        if (!path.contains("/Android/data") && !path.contains("/Android/obb")) {
                            if (f.name.equals("cache", ignoreCase = true) || f.name.equals(".thumbnails", ignoreCase = true)) {
                                val nested = f.listFiles() ?: emptyArray()
                                nested.forEach { junkItems.add(FileItem.fromFile(it)) }
                            } else {
                                queue.add(f to depth + 1)
                            }
                        }
                    }
                } else {
                    val ext = f.extension.lowercase(Locale.US)
                    if (ext in junkExtensions || (f.length() == 0L && f.name.endsWith(".txt"))) {
                        junkItems.add(FileItem.fromFile(f))
                    }
                }
            }
        }
        junkItems
    }

    suspend fun cleanJunkFiles(items: List<FileItem>): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            for (item in items) {
                val f = File(item.path)
                val len = f.length()
                if (f.delete()) {
                    freedBytes += len
                }
            }
            Result.success(freedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanLargeFiles(minSizeBytes: Long = 10 * 1024 * 1024L): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val largeList = mutableListOf<FileItem>()
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)

        while (queue.isNotEmpty() && largeList.size < 200) {
            val (dir, depth) = queue.removeFirst()
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    if (depth < 6 && !f.absolutePath.contains("/Android/data")) {
                        queue.add(f to depth + 1)
                    }
                } else {
                    if (f.length() >= minSizeBytes) {
                        largeList.add(FileItem.fromFile(f))
                    }
                }
            }
        }
        largeList.sortedByDescending { it.size }
    }

    suspend fun scanDuplicateFiles(): List<DuplicateFileGroup> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)

        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    if (depth < 5 && !f.absolutePath.contains("/Android/data")) {
                        queue.add(f to depth + 1)
                    }
                } else {
                    // Only examine non-empty files > 50KB to make duplicate detection practical and fast
                    val size = f.length()
                    if (size > 50 * 1024L) {
                        sizeMap.getOrPut(size) { mutableListOf() }.add(f)
                    }
                }
            }
        }

        val potentialGroups = sizeMap.filter { it.value.size > 1 }
        val duplicateGroups = mutableListOf<DuplicateFileGroup>()

        for ((size, files) in potentialGroups) {
            if (files.size > 1) {
                // Group by MD5 or first 4KB prefix hash for speed
                val hashGroups = mutableMapOf<String, MutableList<File>>()
                for (file in files) {
                    val hash = calculateFastHash(file)
                    hashGroups.getOrPut(hash) { mutableListOf() }.add(file)
                }

                for ((_, matchingFiles) in hashGroups) {
                    if (matchingFiles.size > 1) {
                        duplicateGroups.add(
                            DuplicateFileGroup(
                                size = size,
                                items = matchingFiles.map { FileItem.fromFile(it) }
                            )
                        )
                    }
                }
            }
        }

        duplicateGroups.sortedByDescending { it.size * it.items.size }
    }

    suspend fun scanOldDownloads(daysThreshold: Int = 7): List<FileItem> = withContext(Dispatchers.IO) {
        val downloadDir = File(Environment.getExternalStorageDirectory(), "Download")
        if (!downloadDir.exists()) return@withContext emptyList()

        val thresholdMs = System.currentTimeMillis() - (daysThreshold * 24 * 60 * 60 * 1000L)
        val files = downloadDir.listFiles() ?: emptyArray()

        files.filter { !it.isDirectory && it.lastModified() < thresholdMs }
            .map { FileItem.fromFile(it) }
            .sortedByDescending { it.size }
    }

    suspend fun getCategoryBreakdown(): StorageCategoryBreakdown = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        var images = 0L
        var videos = 0L
        var audio = 0L
        var docs = 0L
        var apks = 0L
        var archives = 0L
        var other = 0L
        var total = 0L

        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)

        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    if (depth < 5 && !f.absolutePath.contains("/Android/data")) {
                        queue.add(f to depth + 1)
                    }
                } else {
                    val len = f.length()
                    total += len
                    val item = FileItem.fromFile(f)
                    when {
                        item.isImage -> images += len
                        item.isVideo -> videos += len
                        item.isAudio -> audio += len
                        item.isDocument -> docs += len
                        item.extension.equals("apk", ignoreCase = true) -> apks += len
                        item.isArchive -> archives += len
                        else -> other += len
                    }
                }
            }
        }

        StorageCategoryBreakdown(
            imagesBytes = images,
            videosBytes = videos,
            audioBytes = audio,
            documentsBytes = docs,
            apksBytes = apks,
            archivesBytes = archives,
            otherBytes = other,
            totalScannedBytes = total
        )
    }

    private fun calculateFastHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            val read = fis.read(buffer)
            fis.close()
            if (read > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "${file.name}_${file.length()}"
        }
    }
}
