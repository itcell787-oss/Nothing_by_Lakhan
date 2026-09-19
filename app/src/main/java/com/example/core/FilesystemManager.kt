package com.example.core

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

data class PartitionInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val availableBytes: Long,
    val isReadOnly: Boolean,
    val fsType: String,
    val isRemovable: Boolean = false
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Int
        get() = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0
    val usedPercent: Int get() = usedPercentage

    val formattedTotal: String get() = formatBytes(totalBytes)
    val formattedUsed: String get() = formatBytes(usedBytes)
    val formattedFree: String get() = formatBytes(availableBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        if (digitGroups >= units.size) digitGroups = units.size - 1
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
}

class FilesystemManager {

    suspend fun listDirectory(
        path: String,
        showHidden: Boolean = true
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext Result.failure(NoSuchFileException(directory, reason = "Directory does not exist"))
            }
            if (!directory.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Path is not a directory: $path"))
            }
            if (!directory.canRead()) {
                return@withContext Result.success(emptyList())
            }

            val files = directory.listFiles() ?: emptyArray()
            val items = files
                .filter { showHidden || !it.name.startsWith(".") }
                .map { FileItem.fromFile(it) }
                .sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase(Locale.getDefault()) }
                )

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPartitions(): List<PartitionInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PartitionInfo>()
        // 1. Primary Internal Storage Partition
        try {
            val extDir = Environment.getExternalStorageDirectory()
            val dataDir = Environment.getDataDirectory()
            
            // Query StatFs on External Storage Directory (user accessible primary flash storage)
            var stat = try { StatFs(extDir.path) } catch (_: Throwable) { null }
            if (stat == null || stat.blockCountLong == 0L) {
                stat = try { StatFs(dataDir.path) } catch (_: Throwable) { null }
            }

            if (stat != null && stat.blockCountLong > 0L) {
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                val freeBlocks = stat.freeBlocksLong

                val total = blockSize * totalBlocks
                val free = blockSize * freeBlocks
                val available = blockSize * availableBlocks

                list.add(
                    PartitionInfo(
                        name = "Internal Storage",
                        path = extDir.absolutePath,
                        totalBytes = total,
                        freeBytes = free,
                        availableBytes = available,
                        isReadOnly = false,
                        fsType = "f2fs/ext4"
                    )
                )
            }
        } catch (_: Throwable) {}

        // Detect Physical Removable SD Cards / USB OTG
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.canRead()) {
                val entries = storageDir.listFiles() ?: emptyArray()
                for (sub in entries) {
                    if (sub.isDirectory && sub.canRead() && !sub.name.equals("emulated", ignoreCase = true) && !sub.name.equals("self", ignoreCase = true)) {
                        try {
                            val stat = StatFs(sub.path)
                            val total = stat.blockSizeLong * stat.blockCountLong
                            if (total > 0L) {
                                val free = stat.blockSizeLong * stat.freeBlocksLong
                                val available = stat.blockSizeLong * stat.availableBlocksLong
                                val isUsb = sub.name.contains("usb", ignoreCase = true)
                                list.add(
                                    PartitionInfo(
                                        name = if (isUsb) "USB OTG Storage (${sub.name})" else "SD Card (${sub.name})",
                                        path = sub.absolutePath,
                                        totalBytes = total,
                                        freeBytes = free,
                                        availableBytes = available,
                                        isReadOnly = !sub.canWrite(),
                                        fsType = "exfat/vfat"
                                    )
                                )
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}

        try {
            val rootDir = Environment.getRootDirectory()
            if (rootDir.exists() && rootDir.canRead()) {
                val stat = StatFs(rootDir.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                val freeBlocks = stat.freeBlocksLong

                val total = blockSize * totalBlocks
                val free = blockSize * freeBlocks
                val available = blockSize * availableBlocks

                list.add(
                    PartitionInfo(
                        name = "System Partition",
                        path = rootDir.absolutePath,
                        totalBytes = total,
                        freeBytes = free,
                        availableBytes = available,
                        isReadOnly = true,
                        fsType = "erofs"
                    )
                )
            }
        } catch (_: Throwable) {}

        list
    }

    fun isDeviceRooted(): Boolean {
        return try {
            val buildTags = android.os.Build.TAGS
            buildTags != null && buildTags.contains("test-keys")
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun createFolder(parentPath: String, name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parentPath, name)
            if (newDir.exists()) {
                return@withContext Result.failure(Exception("Directory already exists"))
            }
            val created = newDir.mkdirs()
            if (created) Result.success(true) else Result.failure(Exception("Permission denied to create folder in $parentPath"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentPath: String, name: String, content: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(parentPath, name)
            if (file.exists()) {
                return@withContext Result.failure(Exception("File already exists"))
            }
            file.writeText(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(oldPath: String, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return@withContext Result.failure(Exception("Source does not exist"))
            val targetFile = File(oldFile.parentFile, newName)
            if (targetFile.exists()) return@withContext Result.failure(Exception("Target name already exists"))
            val renamed = oldFile.renameTo(targetFile)
            if (renamed) Result.success(true) else Result.failure(Exception("Failed to rename $oldPath"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(Exception("File does not exist"))
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (deleted) Result.success(true) else Result.failure(Exception("Permission denied to delete $path"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copy(sourcePath: String, destDir: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val src = File(sourcePath)
            val dest = File(destDir, src.name)
            if (!src.exists()) return@withContext Result.failure(Exception("Source does not exist"))
            if (src.isDirectory) {
                src.copyRecursively(dest, overwrite = true)
            } else {
                src.copyTo(dest, overwrite = true)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun move(sourcePath: String, destDir: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val copyResult = copy(sourcePath, destDir)
            if (copyResult.isSuccess) {
                delete(sourcePath)
                Result.success(true)
            } else {
                copyResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readFileContent(path: String, maxChars: Int = 10000): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))
            if (!file.canRead()) return@withContext Result.failure(Exception("File read permission denied"))

            val reader = file.bufferedReader()
            val buffer = CharArray(maxChars)
            val charsRead = reader.read(buffer, 0, maxChars)
            reader.close()

            val text = if (charsRead > 0) String(buffer, 0, charsRead) else ""
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readHexDump(path: String, maxBytes: Int = 512): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))
            val bytes = ByteArray(maxBytes)
            val stream = FileInputStream(file)
            val bytesRead = stream.read(bytes)
            stream.close()

            if (bytesRead <= 0) return@withContext Result.success("Empty file (0 bytes)")

            val sb = StringBuilder()
            var offset = 0
            while (offset < bytesRead) {
                sb.append(String.format(Locale.US, "%08X: ", offset))
                val lineLength = Math.min(16, bytesRead - offset)
                for (i in 0 until 16) {
                    if (i < lineLength) {
                        sb.append(String.format(Locale.US, "%02X ", bytes[offset + i]))
                    } else {
                        sb.append("   ")
                    }
                    if (i == 7) sb.append(" ")
                }
                sb.append(" |")
                for (i in 0 until lineLength) {
                    val b = bytes[offset + i].toInt() and 0xFF
                    val char = if (b in 32..126) b.toChar() else '.'
                    sb.append(char)
                }
                sb.append("|\n")
                offset += 16
            }

            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateChecksum(path: String, algorithm: String = "MD5"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || file.isDirectory) return@withContext Result.failure(Exception("Invalid file"))
            val digest = MessageDigest.getInstance(algorithm)
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var n: Int
            while (fis.read(buffer).also { n = it } != -1) {
                digest.update(buffer, 0, n)
            }
            fis.close()
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deep search recursively inside directories, folders, and subfolders.
     */
    suspend fun searchRecursive(
        rootPath: String,
        query: String,
        maxDepth: Int = 8,
        maxResults: Int = 250,
        includeHidden: Boolean = false
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val root = File(rootPath)
            if (!root.exists() || !root.canRead()) {
                return@withContext Result.failure(Exception("Directory not accessible"))
            }

            val cleanQuery = query.trim().lowercase(Locale.getDefault())
            if (cleanQuery.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val results = mutableListOf<FileItem>()
            val queue = ArrayDeque<Pair<File, Int>>()
            queue.add(root to 0)

            while (queue.isNotEmpty() && results.size < maxResults) {
                val (dir, depth) = queue.removeFirst()
                val children = dir.listFiles() ?: continue

                for (child in children) {
                    if (!includeHidden && child.name.startsWith(".")) continue

                    if (child.name.lowercase(Locale.getDefault()).contains(cleanQuery)) {
                        results.add(FileItem.fromFile(child))
                        if (results.size >= maxResults) break
                    }

                    if (child.isDirectory && depth < maxDepth && child.canRead()) {
                        val childPath = child.absolutePath
                        if (!childPath.startsWith("/proc") && !childPath.startsWith("/sys") && !childPath.startsWith("/dev")) {
                            queue.add(child to (depth + 1))
                        }
                    }
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

