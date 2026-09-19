package com.example.vpn

enum class VpnStatus {
    DISCONNECTED,
    PREPARING,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    ERROR
}

data class VpnStats(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val packetsIn: Long = 0L,
    val packetsOut: Long = 0L,
    val connectedDurationSeconds: Long = 0L,
    val latencyMs: Long? = null,
    val assignedIp: String = "10.8.0.2",
    val serverHost: String = "",
    val serverPort: Int = 1194,
    val proto: String = "UDP",
    val routingScope: VpnRoutingScope = VpnRoutingScope.WHOLE_PHONE,
    val firewallType: FirewallType = FirewallType.OPENVPN,
    val activeRoutes: List<String> = emptyList(),
    val gatewayLatencies: Map<String, Long?> = emptyMap()
) {
    val formattedBytesIn: String
        get() = formatSize(bytesIn)

    val formattedBytesOut: String
        get() = formatSize(bytesOut)

    val formattedDownloadSpeed: String
        get() = "${formatSize(downloadSpeedBps)}/s"

    val formattedUploadSpeed: String
        get() = "${formatSize(uploadSpeedBps)}/s"

    val formattedDuration: String
        get() {
            val hrs = connectedDurationSeconds / 3600
            val mins = (connectedDurationSeconds % 3600) / 60
            val secs = connectedDurationSeconds % 60
            return if (hrs > 0) {
                "%02d:%02d:%02d".format(hrs, mins, secs)
            } else {
                "%02d:%02d".format(mins, secs)
            }
        }

    private fun formatSize(bytes: Long): String {
        val b = bytes.coerceAtLeast(0)
        return when {
            b >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f GB", b.toDouble() / (1024.0 * 1024.0 * 1024.0))
            b >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", b.toDouble() / (1024.0 * 1024.0))
            b >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", b.toDouble() / 1024.0)
            else -> "$b B"
        }
    }
}
