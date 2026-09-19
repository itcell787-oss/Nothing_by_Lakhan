package com.example.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileShareHelper {

    fun getMimeType(file: FileItem): String {
        if (file.isDirectory) {
            return "resource/folder"
        }
        return getMimeType(file.extension)
    }

    fun getMimeType(extension: String): String {
        val ext = extension.lowercase(Locale.ROOT)
        return when (ext) {
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pdf" -> "application/pdf"
            "txt", "log" -> "text/plain"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "csv" -> "text/csv"
            "tsv" -> "text/tab-separated-values"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "apk" -> "application/vnd.android.package-archive"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> {
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                mime ?: "*/*"
            }
        }
    }

    private fun getFileUri(context: Context, localFile: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )
        } catch (_: Exception) {
            // Fallback to Uri.fromFile if FileProvider fails
            Uri.fromFile(localFile)
        }
    }

    private fun zipFolderToCache(context: Context, folder: File): File? {
        return try {
            val shareCacheDir = File(context.cacheDir, "shared_archives").apply { mkdirs() }
            val zipFile = File(shareCacheDir, "${folder.name}.zip")
            if (zipFile.exists()) zipFile.delete()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                folder.walkTopDown().maxDepth(10).forEach { file ->
                    val relativePath = file.relativeTo(folder).path
                    if (file.isDirectory) {
                        if (relativePath.isNotEmpty()) {
                            val entry = ZipEntry("$relativePath/")
                            zos.putNextEntry(entry)
                            zos.closeEntry()
                        }
                    } else {
                        val entry = ZipEntry(relativePath)
                        zos.putNextEntry(entry)
                        file.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            zipFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Shares a single file or directory using Android's native share sheet.
     * If sharing a folder, compresses it to a temporary zip archive first.
     */
    fun shareFile(context: Context, fileItem: FileItem) {
        try {
            val localFile = File(fileItem.path)
            if (!localFile.exists()) {
                Toast.makeText(context, "Item does not exist: ${fileItem.name}", Toast.LENGTH_SHORT).show()
                return
            }

            if (fileItem.isDirectory) {
                Toast.makeText(context, "Preparing folder archive for sharing...", Toast.LENGTH_SHORT).show()
                val zipArchive = zipFolderToCache(context, localFile)
                if (zipArchive != null && zipArchive.exists()) {
                    val uri = getFileUri(context, zipArchive)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "${fileItem.name}.zip")
                        putExtra(Intent.EXTRA_TEXT, "Shared folder archive: ${fileItem.name}.zip")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Folder \"${fileItem.name}\" via...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return
                }
            }

            val uri = getFileUri(context, localFile)
            val mimeType = getMimeType(fileItem)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileItem.name)
                putExtra(Intent.EXTRA_TEXT, "Shared from Nothing Explorer: ${fileItem.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share \"${fileItem.name}\" via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share item: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares multiple files simultaneously using Android's native share sheet.
     */
    fun shareMultipleFiles(context: Context, fileItems: List<FileItem>) {
        if (fileItems.isEmpty()) return
        if (fileItems.size == 1) {
            shareFile(context, fileItems.first())
            return
        }

        try {
            val uris = ArrayList<Uri>()
            for (item in fileItems) {
                val f = File(item.path)
                if (f.exists()) {
                    if (item.isDirectory) {
                        val zipped = zipFolderToCache(context, f)
                        if (zipped != null) uris.add(getFileUri(context, zipped))
                    } else {
                        uris.add(getFileUri(context, f))
                    }
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(context, "No valid items selected to share", Toast.LENGTH_SHORT).show()
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "${uris.size} Items from Nothing Explorer")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share ${uris.size} items via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens a file or directory with an external application chosen by the user.
     */
    fun openFileWithExternalApp(context: Context, fileItem: FileItem) {
        try {
            val localFile = File(fileItem.path)
            if (!localFile.exists()) {
                Toast.makeText(context, "Item does not exist: ${fileItem.name}", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = getFileUri(context, localFile)

            if (fileItem.isDirectory) {
                // Folder open with
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "resource/folder")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    val chooser = Intent.createChooser(viewIntent, "Open Folder \"${fileItem.name}\" with...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return
                } catch (_: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(fallbackIntent, "Open Folder \"${fileItem.name}\" with...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return
                }
            }

            val mimeType = getMimeType(fileItem)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open \"${fileItem.name}\" with...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No application found to open this item", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open item: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
