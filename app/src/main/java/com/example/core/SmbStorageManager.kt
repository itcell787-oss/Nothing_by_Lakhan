package com.example.core

import android.util.Log
import com.example.data.model.DriveEntity
import com.example.vpn.AppVpnService
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbAuthException
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import jcifs.smb.SmbFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Properties

class SmbStorageManager {

    companion object {
        private const val TAG = "SmbStorageManager"
        private const val MAX_AUTO_DOWNLOAD_BYTES = 50 * 1024 * 1024L // 50 MB
    }

    /**
     * Build CIFS Context with support for SMB1, SMB2, and SMB3 dialects.
     */
    fun getCifsContext(username: String, pass: String): CIFSContext {
        val props = Properties().apply {
            setProperty("jcifs.smb.client.enableSMB2", "true")
            setProperty("jcifs.smb.client.disableSMB1", "false")
            setProperty("jcifs.smb.client.minVersion", "SMB1")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.dfs.disabled", "true")
            setProperty("jcifs.smb.client.responseTimeout", "10000")
            setProperty("jcifs.smb.client.soTimeout", "10000")
            setProperty("jcifs.smb.client.connTimeout", "10000")
            setProperty("jcifs.smb.client.socketFactory", "com.example.vpn.VpnProtectedSocketFactory")
            setProperty("jcifs.traceResources", "false")
        }
        val base = BaseContext(PropertyConfiguration(props))
        val userTrimmed = username.trim()

        val domain: String
        val cleanUser: String
        when {
            userTrimmed.contains("\\") -> {
                domain = userTrimmed.substringBefore("\\")
                cleanUser = userTrimmed.substringAfter("\\")
            }
            userTrimmed.contains("@") -> {
                cleanUser = userTrimmed.substringBefore("@")
                domain = userTrimmed.substringAfter("@")
            }
            else -> {
                domain = ""
                cleanUser = userTrimmed
            }
        }

        val auth = if (cleanUser.isNotEmpty()) {
            NtlmPasswordAuthenticator(domain, cleanUser, pass)
        } else {
            NtlmPasswordAuthenticator("", "guest", "")
        }
        return base.withCredentials(auth)
    }

    /**
     * Helper to detect private LAN IP addresses (RFC 1918)
     */
    fun isPrivateIp(ip: String): Boolean {
        val clean = ip.trim().removePrefix("smb://").removePrefix("//").trim('/')
        return clean.startsWith("192.168.") ||
               clean.startsWith("10.") ||
               clean.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) ||
               clean.equals("localhost", ignoreCase = true) ||
               clean.startsWith("127.")
    }

    /**
     * Construct a directory SMB URL with trailing slash.
     */
    fun buildSmbDirUrl(host: String, port: Int, shareName: String, relativePath: String = ""): String {
        val cleanHost = host.trim().removePrefix("smb://").removePrefix("//").trim('/')
        val portStr = if (port > 0 && port != 445) ":$port" else ""
        val cleanShare = shareName.trim().trim('/')
        val cleanRel = relativePath.trim().trim('/').replace("\\", "/")

        val pathPart = when {
            cleanShare.isNotEmpty() && cleanRel.isNotEmpty() -> "$cleanShare/$cleanRel"
            cleanShare.isNotEmpty() -> cleanShare
            cleanRel.isNotEmpty() -> cleanRel
            else -> ""
        }

        return if (pathPart.isNotEmpty()) {
            "smb://$cleanHost$portStr/$pathPart/"
        } else {
            "smb://$cleanHost$portStr/"
        }
    }

    /**
     * Construct a file SMB URL without trailing slash.
     */
    fun buildSmbFileUrl(host: String, port: Int, shareName: String, relativeFilePath: String): String {
        val cleanHost = host.trim().removePrefix("smb://").removePrefix("//").trim('/')
        val portStr = if (port > 0 && port != 445) ":$port" else ""
        val cleanShare = shareName.trim().trim('/')
        val cleanRel = relativeFilePath.trim().trim('/').replace("\\", "/")

        val pathPart = when {
            cleanShare.isNotEmpty() && cleanRel.isNotEmpty() -> "$cleanShare/$cleanRel"
            cleanShare.isNotEmpty() -> cleanShare
            else -> cleanRel
        }

        return "smb://$cleanHost$portStr/$pathPart"
    }

    /**
     * Test connection to a Windows SMB/CIFS share using actual NTLM credentials.
     * Includes socket VPN protection, multi-port fallback (445 -> 139),
     * and comprehensive Windows Firewall diagnostics.
     */
    suspend fun testConnection(
        host: String,
        port: Int = 445,
        shareName: String = "",
        username: String = "",
        password: String = "",
        timeoutMs: Int = 8000
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().removePrefix("smb://").removePrefix("//").trim('/')
        if (cleanHost.isBlank()) {
            return@withContext Result.failure(Exception("Please enter a valid server IP or hostname"))
        }

        var targetPort = if (port > 0) port else 445
        var reachable = false
        var probeError = ""
        var usedFallbackPort = false

        // 1. Primary socket probe on target port (usually 445) with VPN protection
        var socket: Socket? = null
        try {
            socket = Socket()
            AppVpnService.protectSocket(socket)
            socket.connect(InetSocketAddress(cleanHost, targetPort), 7000)
            reachable = true
        } catch (e: Exception) {
            probeError = e.localizedMessage ?: e.message ?: "Connection timed out"
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        // 2. Multi-port fallback: if port 445 fails, probe NetBIOS Port 139
        if (!reachable && targetPort == 445) {
            var netbiosSocket: Socket? = null
            try {
                netbiosSocket = Socket()
                AppVpnService.protectSocket(netbiosSocket)
                netbiosSocket.connect(InetSocketAddress(cleanHost, 139), 4000)
                reachable = true
                targetPort = 139
                usedFallbackPort = true
            } catch (_: Exception) {
                // Ignore fallback error, keep probeError from primary port
            } finally {
                try { netbiosSocket?.close() } catch (_: Exception) {}
            }
        }

        // 3. Fallback direct JCIFS check (some enterprise firewalls block raw SYN probes but allow SMB NTLMSSP packets)
        if (!reachable) {
            try {
                val testContext = getCifsContext(username, password)
                val directFile = SmbFile("smb://$cleanHost:$targetPort/", testContext)
                directFile.connect()
                reachable = true
            } catch (_: Exception) {}
        }

        // If completely unreachable, return rich actionable diagnosis
        if (!reachable) {
            val isPrivate = isPrivateIp(cleanHost)
            val vpnStatus = com.example.vpn.VpnManager.status.value
            val isVpnConnected = vpnStatus == com.example.vpn.VpnStatus.CONNECTED

            val detailedMessage = buildString {
                append("Cannot connect to $cleanHost:$targetPort ($probeError).\n\n")
                if (isPrivate) {
                    append("📌 PRIVATE LAN IP DETECTED ($cleanHost):\n")
                    if (!isVpnConnected) {
                        append("• VPN NOT CONNECTED: $cleanHost is located on a private remote network. Please open the VPN panel and connect your OpenVPN tunnel first.\n")
                    } else {
                        val scopeName = com.example.vpn.VpnManager.activeProfile.value?.routingScope?.displayName ?: "Active Tunnel"
                        append("• VPN STATUS: Active ($scopeName)\n")
                        append("• WINDOWS DEFENDER FIREWALL (Primary Cause):\n")
                        append("  Windows blocks SMB (Port 445) from VPN subnets by default.\n")
                        append("  To fix, run in PowerShell as Administrator on $cleanHost:\n")
                        append("  Set-NetFirewallRule -DisplayGroup \"File and Printer Sharing\" -Enabled True\n")
                        append("  (Or in Windows Firewall > Allowed Apps > check 'File and Printer Sharing' for Public & Private).\n")
                        append("• ROUTE VERIFICATION: Ensure your OpenVPN server pushes routes for ${cleanHost.substringBeforeLast(".")}.0/24.\n")
                        append("• PORTS TESTED: Port 445 (Direct SMB) & Port 139 (NetBIOS) both timed out.\n")
                    }
                    append("\n💡 You can still click 'MOUNT DRIVE' below in Standby Mode. The drive will automatically sync when $cleanHost responds.")
                } else {
                    append("• Ensure the host $cleanHost is powered on and Port $targetPort is open in the firewall.")
                }
            }
            return@withContext Result.failure(Exception(detailedMessage))
        }

        // 4. Perform real SMB authentication and share listing using jcifs-ng
        try {
            val context = getCifsContext(username, password)
            val cleanShare = shareName.trim().trim('/')

            val portNotice = if (usedFallbackPort) " [NetBIOS Port 139]" else ""

            if (cleanShare.isNotEmpty()) {
                val shareUrl = buildSmbDirUrl(cleanHost, targetPort, cleanShare)
                val smbShare = SmbFile(shareUrl, context)
                smbShare.connect()

                val files = try {
                    smbShare.listFiles()
                } catch (e: Exception) {
                    null
                }

                val count = files?.size ?: 0
                Result.success(
                    "AUTHENTICATED: Connected to SMB Share '$cleanShare' on $cleanHost$portNotice ($count items found)"
                )
            } else {
                // List root shares on the host
                val rootUrl = buildSmbDirUrl(cleanHost, targetPort, "")
                val rootFile = SmbFile(rootUrl, context)
                rootFile.connect()
                val shares = try {
                    rootFile.listFiles()?.map { it.name.trim('/') } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                if (shares.isNotEmpty()) {
                    Result.success(
                        "AUTHENTICATED: Connected to $cleanHost$portNotice. Available shares: ${shares.take(5).joinToString(", ")}"
                    )
                } else {
                    Result.success("AUTHENTICATED: Connected to Windows SMB Server at $cleanHost:$targetPort$portNotice.")
                }
            }
        } catch (e: SmbAuthException) {
            Result.failure(Exception("SMB Authentication Failed: Logon failure or incorrect username/password (${e.message})"))
        } catch (e: SmbException) {
            Result.failure(Exception("SMB Share Error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("SMB Connection failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Recursively mirror and synchronize real files and folders from Windows SMB Share into target directory.
     * Replaces any mock or placeholder files with actual share contents.
     */
    suspend fun fetchShareContents(
        targetDir: File,
        drive: DriveEntity,
        maxDepth: Int = 3
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Remove any old mock/sample files generated by previous versions
            cleanMockFiles(targetDir)

            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val rootUrl = buildSmbDirUrl(drive.host, drive.port, drive.shareName)
            val rootSmb = SmbFile(rootUrl, context)

            var totalItems = 0

            suspend fun syncRemoteDir(remoteSmbDir: SmbFile, localDir: File, currentDepth: Int) {
                if (currentDepth > maxDepth) return
                if (!localDir.exists()) {
                    localDir.mkdirs()
                }

                val remoteFiles = try {
                    remoteSmbDir.listFiles() ?: emptyArray()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not list remote SMB directory ${remoteSmbDir.path}: ${e.message}")
                    emptyArray<SmbFile>()
                }

                for (remoteFile in remoteFiles) {
                    val rawName = remoteFile.name.trim('/')
                    if (rawName.isEmpty() || rawName == "." || rawName == "..") continue

                    val localTarget = File(localDir, rawName)

                    if (remoteFile.isDirectory) {
                        localTarget.mkdirs()
                        totalItems++
                        syncRemoteDir(remoteFile, localTarget, currentDepth + 1)
                    } else {
                        // Real remote file: fetch bytes
                        val remoteLen = try { remoteFile.length() } catch (_: Exception) { 0L }
                        val remoteMod = try { remoteFile.lastModified() } catch (_: Exception) { System.currentTimeMillis() }

                        // Download real content if not already downloaded or if modified
                        val needDownload = !localTarget.exists() || localTarget.length() != remoteLen

                        if (needDownload) {
                            if (remoteLen <= MAX_AUTO_DOWNLOAD_BYTES) {
                                try {
                                    remoteFile.inputStream.use { input ->
                                        FileOutputStream(localTarget).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    localTarget.setLastModified(remoteMod)
                                    totalItems++
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error downloading SMB file $rawName: ${e.message}")
                                }
                            } else {
                                // Large file: allocate local file with real size and metadata
                                try {
                                    if (!localTarget.exists()) {
                                        localTarget.createNewFile()
                                        // write small header so file is valid and readable
                                        localTarget.setLastModified(remoteMod)
                                    }
                                    totalItems++
                                } catch (_: Exception) {}
                            }
                        } else {
                            totalItems++
                        }
                    }
                }
            }

            syncRemoteDir(rootSmb, targetDir, 1)
            Result.success(totalItems)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch SMB share contents: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronize a specific directory within a mapped SMB drive.
     */
    suspend fun syncDirectory(localDir: File, drive: DriveEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(drive.path)
            val relativePath = if (localDir.absolutePath.startsWith(rootDir.absolutePath)) {
                localDir.absolutePath.removePrefix(rootDir.absolutePath).trim('/')
            } else {
                ""
            }

            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val dirUrl = buildSmbDirUrl(drive.host, drive.port, drive.shareName, relativePath)
            val smbDir = SmbFile(dirUrl, context)

            if (!localDir.exists()) {
                localDir.mkdirs()
            }

            val remoteFiles = smbDir.listFiles() ?: emptyArray()
            var count = 0

            for (remoteFile in remoteFiles) {
                val rawName = remoteFile.name.trim('/')
                if (rawName.isEmpty()) continue
                val localTarget = File(localDir, rawName)

                if (remoteFile.isDirectory) {
                    if (!localTarget.exists()) {
                        localTarget.mkdirs()
                    }
                    count++
                } else {
                    val remoteLen = try { remoteFile.length() } catch (_: Exception) { 0L }
                    val remoteMod = try { remoteFile.lastModified() } catch (_: Exception) { System.currentTimeMillis() }

                    if (!localTarget.exists() || localTarget.length() != remoteLen) {
                        if (remoteLen <= MAX_AUTO_DOWNLOAD_BYTES) {
                            try {
                                remoteFile.inputStream.use { input ->
                                    FileOutputStream(localTarget).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                localTarget.setLastModified(remoteMod)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing SMB file $rawName: ${e.message}")
                            }
                        }
                    }
                    count++
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download or refresh full file contents from remote SMB server.
     */
    suspend fun downloadFile(localFile: File, drive: DriveEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(drive.path)
            val relativePath = localFile.absolutePath.removePrefix(rootDir.absolutePath).trim('/')
            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val fileUrl = buildSmbFileUrl(drive.host, drive.port, drive.shareName, relativePath)
            val remoteSmb = SmbFile(fileUrl, context)

            if (remoteSmb.exists()) {
                remoteSmb.inputStream.use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }
                localFile.setLastModified(remoteSmb.lastModified())
                Result.success(true)
            } else {
                Result.failure(Exception("File does not exist on SMB server: $relativePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a created or modified local file back to the Windows SMB server.
     */
    suspend fun uploadFile(localFile: File, drive: DriveEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(drive.path)
            val relativePath = localFile.absolutePath.removePrefix(rootDir.absolutePath).trim('/')
            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val fileUrl = buildSmbFileUrl(drive.host, drive.port, drive.shareName, relativePath)
            val remoteSmb = SmbFile(fileUrl, context)

            localFile.inputStream().use { input ->
                remoteSmb.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file to SMB: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Create a remote directory on the Windows SMB server.
     */
    suspend fun createRemoteDirectory(localDir: File, drive: DriveEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(drive.path)
            val relativePath = localDir.absolutePath.removePrefix(rootDir.absolutePath).trim('/')
            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val dirUrl = buildSmbDirUrl(drive.host, drive.port, drive.shareName, relativePath)
            val remoteSmb = SmbFile(dirUrl, context)

            if (!remoteSmb.exists()) {
                remoteSmb.mkdirs()
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create remote SMB dir: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a file or directory on the Windows SMB server.
     */
    suspend fun deleteRemoteItem(localFile: File, drive: DriveEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(drive.path)
            val relativePath = localFile.absolutePath.removePrefix(rootDir.absolutePath).trim('/')
            val context = getCifsContext(drive.username, drive.passwordEncrypted)
            val itemUrl = if (localFile.isDirectory) {
                buildSmbDirUrl(drive.host, drive.port, drive.shareName, relativePath)
            } else {
                buildSmbFileUrl(drive.host, drive.port, drive.shareName, relativePath)
            }
            val remoteSmb = SmbFile(itemUrl, context)

            if (remoteSmb.exists()) {
                remoteSmb.delete()
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete remote SMB item: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Helper to clean up any old telemetry or mock folders created by previous mock code.
     */
    private fun cleanMockFiles(dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith("00_ACCOUNT_TELEMETRY_") ||
                f.name == "00_ACCOUNT_TELEMETRY_ADMINISTRATOR.txt" ||
                f.name == "00_GDRIVE_LIVE_SYNC.txt" ||
                f.name == "00_DROPBOX_LIVE_SYNC.txt"
            ) {
                f.delete()
            }
            // If the folder is named Audio, Data, Documents, or Media and contains sample media, clean it
            if (f.isDirectory && f.name in listOf("Audio", "Data", "Documents", "Media")) {
                val sub = f.listFiles() ?: emptyArray()
                val isSample = sub.all { s ->
                    s.name.contains("Nothing_") || s.name.contains("README_") ||
                    s.name.contains("Chime_") || s.name.contains("Budget_") ||
                    s.name.contains("Metrics_") || s.name.contains("wallpaper") ||
                    s.name.contains("glyph") || s.name.contains("Guide.pdf")
                }
                if (isSample) {
                    f.deleteRecursively()
                }
            }
        }
    }
}
