package com.example.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory {
    FOLDER,
    DOCUMENT,
    CODE,
    IMAGE,
    AUDIO,
    VIDEO,
    ARCHIVE,
    BINARY,
    SYSTEM_CONFIG,
    OTHER
}

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val permissions: String,
    val isSymlink: Boolean = false,
    val symlinkTarget: String? = null,
    val isReadable: Boolean = true,
    val isWritable: Boolean = false,
    val isHidden: Boolean = false,
    val category: FileCategory = FileCategory.OTHER,
    val childCount: Int? = null,
    val isRemote: Boolean = false,
    val remoteDriveType: String? = null
) {
    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (childCount != null) "$childCount items" else "Folder"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }

    val formattedDate: String
        get() {
            if (lastModified <= 0) return "--"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isPhoto: Boolean
        get() = category == FileCategory.IMAGE

    val isImage: Boolean
        get() = category == FileCategory.IMAGE

    val isVideo: Boolean
        get() = category == FileCategory.VIDEO

    val isAudio: Boolean
        get() = category == FileCategory.AUDIO

    val isDocument: Boolean
        get() = category == FileCategory.DOCUMENT

    val isArchive: Boolean
        get() = category == FileCategory.ARCHIVE

    val parentPath: String
        get() = path.substringBeforeLast('/', "/")

    companion object {
        fun determineCategory(name: String, isDirectory: Boolean): FileCategory {
            if (isDirectory) return FileCategory.FOLDER
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "txt", "md", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "log" -> FileCategory.DOCUMENT
                "kt", "java", "xml", "json", "html", "css", "js", "ts", "py", "c", "cpp", "h", "sh", "rc", "prop" -> FileCategory.CODE
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic" -> FileCategory.IMAGE
                "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus" -> FileCategory.AUDIO
                "mp4", "mkv", "avi", "mov", "webm", "flv" -> FileCategory.VIDEO
                "zip", "tar", "gz", "tgz", "bz2", "7z", "rar", "apk", "jar" -> FileCategory.ARCHIVE
                "so", "bin", "elf", "o", "a", "dex" -> FileCategory.BINARY
                "conf", "ini", "cfg", "yaml", "yml", "toml", "te" -> FileCategory.SYSTEM_CONFIG
                else -> FileCategory.OTHER
            }
        }

        fun fromFile(file: File): FileItem {
            val isDir = file.isDirectory
            val readable = file.canRead()
            val writable = file.canWrite()

            val permBuilder = StringBuilder(7)
            permBuilder.append(if (isDir) "d" else "-")
            permBuilder.append(if (readable) "r" else "-")
            permBuilder.append(if (writable) "w" else "-")
            permBuilder.append(if (isDir) "x" else "-")
            permBuilder.append("r-x")

            return FileItem(
                name = file.name.ifEmpty { file.absolutePath },
                path = file.absolutePath,
                size = if (isDir) 0L else file.length(),
                isDirectory = isDir,
                lastModified = file.lastModified(),
                permissions = permBuilder.toString(),
                isSymlink = false,
                symlinkTarget = null,
                isReadable = readable,
                isWritable = writable,
                isHidden = file.name.startsWith("."),
                category = determineCategory(file.name, isDir),
                childCount = null
            )
        }
    }
}
