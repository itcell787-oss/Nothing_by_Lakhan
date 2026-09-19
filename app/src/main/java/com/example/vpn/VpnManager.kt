package com.example.vpn

import android.content.Context
import android.content.SharedPreferences
import android.net.VpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

object VpnManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(VpnStats())
    val stats: StateFlow<VpnStats> = _stats.asStateFlow()

    private val _activeProfile = MutableStateFlow<VpnProfile?>(null)
    val activeProfile: StateFlow<VpnProfile?> = _activeProfile.asStateFlow()

    private val _savedProfiles = MutableStateFlow<List<VpnProfile>>(emptyList())
    val savedProfiles: StateFlow<List<VpnProfile>> = _savedProfiles.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("vpn_manager_prefs", Context.MODE_PRIVATE)
        loadProfiles()
        if (_savedProfiles.value.isEmpty()) {
            val defaults = listOf(
                VpnProfile.createDefaultHomelabProfile(),
                VpnProfile.createDefaultDriveOnlyProfile(),
                VpnProfile.createDefaultWholePhoneProfile()
            )
            _savedProfiles.value = defaults
            persistProfiles()
        }
        _activeProfile.value = _savedProfiles.value.firstOrNull()
    }

    private fun loadProfiles() {
        val jsonStr = prefs?.getString(KEY_PROFILES, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<VpnProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(VpnProfile.fromJson(obj))
            }
            _savedProfiles.value = list
        } catch (_: Exception) {}
    }

    private fun persistProfiles() {
        val arr = JSONArray()
        for (p in _savedProfiles.value) {
            arr.put(p.toJson())
        }
        prefs?.edit()?.putString(KEY_PROFILES, arr.toString())?.apply()
    }

    fun saveProfile(profile: VpnProfile) {
        val current = _savedProfiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(0, profile)
        }
        _savedProfiles.value = current
        _activeProfile.value = profile
        persistProfiles()
    }

    fun setRoutingScope(scope: VpnRoutingScope) {
        val current = _activeProfile.value ?: return
        val updated = current.copy(routingScope = scope)
        saveProfile(updated)
        addLog("Scope switch: ${scope.displayName} (${scope.badge})")
    }

    fun addRouteToActiveProfile(route: String) {
        val current = _activeProfile.value ?: return
        val trimmed = route.trim()
        if (trimmed.isEmpty()) return
        val existing = current.routes.toMutableList()
        if (!existing.contains(trimmed)) {
            existing.add(trimmed)
            val updated = current.copy(routes = existing)
            saveProfile(updated)
            addLog("Route added: $trimmed")
        }
    }

    fun removeRouteFromActiveProfile(route: String) {
        val current = _activeProfile.value ?: return
        val existing = current.routes.toMutableList()
        if (existing.remove(route)) {
            val updated = current.copy(routes = existing)
            saveProfile(updated)
            addLog("Route removed: $route")
        }
    }

    fun deleteProfile(id: String) {
        val current = _savedProfiles.value.toMutableList()
        current.removeAll { it.id == id }
        _savedProfiles.value = current
        if (_activeProfile.value?.id == id) {
            _activeProfile.value = current.firstOrNull()
        }
        persistProfiles()
    }

    fun selectProfile(profile: VpnProfile) {
        _activeProfile.value = profile
    }

    fun importFromOvpn(content: String, fileName: String, overrideScope: VpnRoutingScope? = null): VpnProfile {
        val parsed = OvpnParser.parse(content, fileName)
        val finalScope = overrideScope ?: parsed.routingScope
        val profile = VpnProfile(
            id = UUID.randomUUID().toString(),
            name = parsed.suggestedName,
            serverHost = parsed.serverHost,
            serverPort = parsed.serverPort,
            proto = parsed.proto,
            routingScope = finalScope,
            firewallType = parsed.firewallType,
            authType = if (parsed.requiresUserAuth) VpnAuthType.USER_PASS else VpnAuthType.CERTIFICATE,
            username = "",
            password = "",
            saveCredentials = true,
            ovpnContent = content,
            fileName = fileName,
            dnsServers = parsed.dnsServers,
            routes = parsed.routes,
            splitTunnel = finalScope == VpnRoutingScope.DRIVE_ONLY,
            assignedIp = "10.8.0.2",
            mtu = parsed.mtu,
            cipher = parsed.cipher,
            authDigest = parsed.authDigest,
            createdAt = System.currentTimeMillis()
        )
        saveProfile(profile)
        addLog("Imported OVPN configuration from '$fileName'")
        addLog("Detected Firewall: ${parsed.firewallType.displayName}")
        addLog("Remote Endpoint: ${parsed.serverHost}:${parsed.serverPort} (${parsed.proto})")
        addLog("Routing Scope: ${finalScope.displayName}")
        return profile
    }

    fun setStatus(newStatus: VpnStatus) {
        _status.value = newStatus
    }

    fun updateStats(newStats: VpnStats) {
        _stats.value = newStats
    }

    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$timestamp] $message"
        val current = _logs.value.toMutableList()
        if (current.size > 100) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun isVpnConnected(): Boolean {
        return _status.value == VpnStatus.CONNECTED
    }

    suspend fun testServerReachability(host: String, port: Int, proto: String): Result<Long> = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext Result.failure(Exception("Host is empty"))
        val start = System.currentTimeMillis()
        try {
            if (proto.equals("TCP", ignoreCase = true)) {
                val socket = Socket()
                AppVpnService.protectSocket(socket)
                socket.connect(InetSocketAddress(host, port), 4000)
                socket.close()
                val duration = System.currentTimeMillis() - start
                Result.success(duration)
            } else {
                val socket = DatagramSocket()
                AppVpnService.protectSocket(socket)
                socket.soTimeout = 4000
                val address = InetAddress.getByName(host)
                val testData = byteArrayOf(0x38, 0x00, 0x00, 0x00, 0x00)
                val packet = DatagramPacket(testData, testData.size, address, port)
                socket.send(packet)
                socket.close()
                val duration = System.currentTimeMillis() - start
                Result.success(duration)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Firewall port unreachable: ${e.localizedMessage ?: e.message}"))
        }
    }

    fun duplicateProfile(profile: VpnProfile) {
        val copy = profile.copy(
            id = UUID.randomUUID().toString(),
            name = "${profile.name} (Copy)",
            createdAt = System.currentTimeMillis()
        )
        saveProfile(copy)
    }

    fun exportLogsToString(): String {
        val sb = StringBuilder()
        sb.append("=== OpenVPN Client Session Log ===\n")
        sb.append("Export Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        sb.append("Active Profile: ${_activeProfile.value?.name ?: "None"}\n")
        sb.append("Server Endpoint: ${_activeProfile.value?.serverHost}:${_activeProfile.value?.serverPort} (${_activeProfile.value?.proto})\n")
        sb.append("----------------------------------\n\n")
        for (line in _logs.value) {
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    fun connect(context: Context, profile: VpnProfile) {
        _activeProfile.value = profile
        _status.value = VpnStatus.CONNECTING
        addLog("Initiating connection to '${profile.name}' [${profile.serverHost}:${profile.serverPort}]")
        AppVpnService.start(context)
    }

    fun disconnect(context: Context) {
        _status.value = VpnStatus.DISCONNECTING
        addLog("User requested disconnect")
        AppVpnService.stop(context)
    }

    /**
     * Checks if an IP or hostname is on a private network (RFC 1918),
     * which typically requires a VPN when connecting remotely.
     */
    fun isPrivateNetworkIp(host: String): Boolean {
        val cleanHost = host.trim().lowercase(java.util.Locale.ROOT)
        if (cleanHost.startsWith("192.168.") ||
            cleanHost.startsWith("10.") ||
            cleanHost.startsWith("127.") ||
            cleanHost == "localhost"
        ) {
            return true
        }
        if (cleanHost.startsWith("172.")) {
            val secondOctet = cleanHost.substringAfter("172.").substringBefore(".").toIntOrNull()
            if (secondOctet != null && secondOctet in 16..31) {
                return true
            }
        }
        return false
    }

    private const val KEY_PROFILES = "saved_vpn_profiles_json"
}
