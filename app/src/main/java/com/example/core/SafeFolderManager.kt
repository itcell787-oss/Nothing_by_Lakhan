package com.example.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class SafeFolderManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("safe_folder_prefs", Context.MODE_PRIVATE)
    }

    private val safeDir: File by lazy {
        File(context.filesDir, ".safe_vault").apply { mkdirs() }
    }

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hash = hashPin(pin)
        return prefs.edit().putString(KEY_PIN_HASH, hash).commit()
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == savedHash
    }

    fun changePin(currentPin: String, newPin: String): Result<Boolean> {
        if (!verifyPin(currentPin)) {
            return Result.failure(Exception("Current PIN is incorrect"))
        }
        if (newPin.length != 4) {
            return Result.failure(Exception("New PIN must be exactly 4 digits"))
        }
        val success = setPin(newPin)
        return if (success) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to save new PIN"))
        }
    }

    fun getOriginalLocation(fileName: String): String? {
        return prefs.getString(KEY_ORIG_PREFIX + fileName, null)?.takeIf { it.isNotBlank() }
    }

    suspend fun moveToSafeFolder(filePath: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(filePath)
            if (!source.exists()) return@withContext Result.failure(Exception("File does not exist"))

            // Store original parent directory path for "Move to previous location"
            val parentPath = source.parent ?: ""
            prefs.edit().putString(KEY_ORIG_PREFIX + source.name, parentPath).apply()

            val safeDest = File(safeDir, source.name)
            val moved = if (source.isDirectory) {
                source.copyRecursively(safeDest, overwrite = true) && source.deleteRecursively()
            } else {
                source.copyTo(safeDest, overwrite = true)
                source.delete()
            }

            if (moved || safeDest.exists()) {
                Result.success(FileItem.fromFile(safeDest))
            } else {
                Result.failure(Exception("Failed to move file to Safe Folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreToOriginalLocation(fileName: String, fallbackFolder: String): Result<FileItem> {
        val orig = getOriginalLocation(fileName) ?: fallbackFolder
        val target = if (orig.isNotBlank() && (File(orig).exists() || File(orig).mkdirs())) orig else fallbackFolder
        return moveOutOfSafeFolder(fileName, target)
    }

    suspend fun moveOutOfSafeFolder(fileName: String, targetFolder: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(safeDir, fileName)
            if (!source.exists()) return@withContext Result.failure(Exception("Vault file not found"))

            val targetDir = File(targetFolder).apply { mkdirs() }
            val dest = File(targetDir, fileName)

            val moved = if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true) && source.deleteRecursively()
            } else {
                source.copyTo(dest, overwrite = true)
                source.delete()
            }

            if (moved || dest.exists()) {
                prefs.edit().remove(KEY_ORIG_PREFIX + fileName).apply()
                Result.success(FileItem.fromFile(dest))
            } else {
                Result.failure(Exception("Failed to restore file from Safe Folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSafeItems(): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val files = safeDir.listFiles() ?: emptyArray()
            files.map { FileItem.fromFile(it) }.sortedByDescending { it.lastModified }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteSafeItem(fileName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(safeDir, fileName)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (deleted) {
                prefs.edit().remove(KEY_ORIG_PREFIX + fileName).apply()
                Result.success(true)
            } else {
                Result.failure(Exception("Delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "safe_folder_pin_hash"
        private const val KEY_ORIG_PREFIX = "vault_orig_path_"
    }
}
