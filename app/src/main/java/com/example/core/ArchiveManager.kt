package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveManager {

    suspend fun createZip(
        sourcePaths: List<String>,
        outputZip: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (outputZip.exists()) {
                outputZip.delete()
            }
            outputZip.parentFile?.mkdirs()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
                for (path in sourcePaths) {
                    val file = File(path)
                    if (!file.exists()) continue
                    addFileToZip(file, "", zos)
                }
            }
            Result.success(outputZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, parentFolder: String, zos: ZipOutputStream) {
        val entryName = if (parentFolder.isEmpty()) file.name else "$parentFolder/${file.name}"
        if (file.isDirectory) {
            val dirEntry = ZipEntry("$entryName/")
            zos.putNextEntry(dirEntry)
            zos.closeEntry()
            val children = file.listFiles() ?: return
            for (child in children) {
                addFileToZip(child, entryName, zos)
            }
        } else {
            val entry = ZipEntry(entryName)
            entry.time = file.lastModified()
            zos.putNextEntry(entry)
            BufferedInputStream(FileInputStream(file)).use { bis ->
                val buffer = ByteArray(8192)
                var count: Int
                while (bis.read(buffer).also { count = it } != -1) {
                    zos.write(buffer, 0, count)
                }
            }
            zos.closeEntry()
        }
    }

    suspend fun extractZip(
        zipFile: File,
        targetDirectory: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || !zipFile.canRead()) {
                return@withContext Result.failure(Exception("Archive file does not exist or cannot be read"))
            }

            targetDirectory.mkdirs()
            val canonicalDestDirPath = targetDirectory.canonicalPath
            var extractedFilesCount = 0

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val newFile = File(targetDirectory, entry.name)
                    val canonicalDestPath = newFile.canonicalPath
                    if (!canonicalDestPath.startsWith(canonicalDestDirPath + File.separator) &&
                        canonicalDestPath != canonicalDestDirPath
                    ) {
                        throw SecurityException("Zip Slip Vulnerability Detected: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                fos.write(buffer, 0, count)
                            }
                        }
                        extractedFilesCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            Result.success(extractedFilesCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
