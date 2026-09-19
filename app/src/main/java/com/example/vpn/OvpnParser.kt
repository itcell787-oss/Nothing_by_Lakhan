package com.example.vpn

import java.util.Locale

data class ParsedOvpnConfig(
    val serverHost: String = "",
    val serverPort: Int = 1194,
    val proto: String = "UDP",
    val routingScope: VpnRoutingScope = VpnRoutingScope.WHOLE_PHONE,
    val firewallType: FirewallType = FirewallType.OPENVPN,
    val requiresUserAuth: Boolean = true,
    val cipher: String = "",
    val authDigest: String = "",
    val mtu: Int = 1500,
    val hasCaCert: Boolean = false,
    val hasClientCert: Boolean = false,
    val hasClientKey: Boolean = false,
    val hasTlsAuth: Boolean = false,
    val dnsServers: List<String> = emptyList(),
    val routes: List<String> = emptyList(),
    val isRedirectGateway: Boolean = false,
    val suggestedName: String = "OpenVPN Profile"
)

object OvpnParser {

    /**
     * Parses an .ovpn configuration file string and extracts parameters.
     */
    fun parse(content: String, originalFileName: String = ""): ParsedOvpnConfig {
        var serverHost = ""
        var serverPort = 1194
        var proto = "UDP"
        var requiresUserAuth = false
        var cipher = ""
        var authDigest = ""
        var mtu = 1500
        var isRedirectGateway = false
        val dnsServers = mutableListOf<String>()
        val routes = mutableListOf<String>()

        val lowerContent = content.lowercase(Locale.ROOT)
        val hasCaCert = content.contains("<ca>") && content.contains("</ca>")
        val hasClientCert = content.contains("<cert>") && content.contains("</cert>")
        val hasClientKey = content.contains("<key>") && content.contains("</key>")
        val hasTlsAuth = (content.contains("<tls-auth>") && content.contains("</tls-auth>")) ||
                (content.contains("<tls-crypt>") && content.contains("</tls-crypt>"))

        // Detect firewall brand
        val firewallType = detectFirewall(content, originalFileName)

        val lines = content.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue

            val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (tokens.isEmpty()) continue

            when (tokens[0].lowercase(Locale.ROOT)) {
                "remote" -> {
                    if (tokens.size >= 2 && serverHost.isEmpty()) {
                        serverHost = tokens[1]
                    }
                    if (tokens.size >= 3) {
                        tokens[2].toIntOrNull()?.let { serverPort = it }
                    }
                    if (tokens.size >= 4) {
                        val p = tokens[3].lowercase(Locale.ROOT)
                        proto = if (p.contains("tcp")) "TCP" else "UDP"
                    }
                }
                "port" -> {
                    if (tokens.size >= 2) {
                        tokens[1].toIntOrNull()?.let { serverPort = it }
                    }
                }
                "proto" -> {
                    if (tokens.size >= 2) {
                        val p = tokens[1].lowercase(Locale.ROOT)
                        proto = if (p.contains("tcp")) "TCP" else "UDP"
                    }
                }
                "tun-mtu", "link-mtu" -> {
                    if (tokens.size >= 2) {
                        tokens[1].toIntOrNull()?.let { mtu = it }
                    }
                }
                "auth-user-pass" -> {
                    requiresUserAuth = true
                }
                "cipher" -> {
                    if (tokens.size >= 2) cipher = tokens[1]
                }
                "data-ciphers" -> {
                    if (tokens.size >= 2) cipher = tokens[1]
                }
                "auth" -> {
                    if (tokens.size >= 2) authDigest = tokens[1]
                }
                "redirect-gateway" -> {
                    isRedirectGateway = true
                }
                "route" -> {
                    if (tokens.size >= 3) {
                        val net = tokens[1]
                        val mask = tokens[2]
                        val cidr = netmaskToCidr(mask)
                        if (cidr != null) {
                            routes.add("$net/$cidr")
                        } else {
                            routes.add("$net/24")
                        }
                    }
                }
                "dhcp-option" -> {
                    if (tokens.size >= 3 && tokens[1].equals("DNS", ignoreCase = true)) {
                        dnsServers.add(tokens[2])
                    }
                }
            }
        }

        // Suggested profile name
        val suggestedName = when {
            originalFileName.isNotBlank() -> originalFileName.removeSuffix(".ovpn").replace("_", " ").uppercase(Locale.ROOT)
            serverHost.isNotBlank() -> "${firewallType.displayName.substringBefore(" ")} ($serverHost)"
            else -> "${firewallType.displayName} VPN"
        }

        val effectiveRoutes = if (routes.isNotEmpty()) {
            routes
        } else {
            listOf("0.0.0.0/0")
        }

        val effectiveDns = if (dnsServers.isNotEmpty()) {
            dnsServers
        } else {
            listOf("1.1.1.1", "8.8.8.8")
        }

        val detectedScope = if (isRedirectGateway || routes.isEmpty() || routes.contains("0.0.0.0/0")) {
            VpnRoutingScope.WHOLE_PHONE
        } else {
            VpnRoutingScope.DRIVE_ONLY
        }

        return ParsedOvpnConfig(
            serverHost = serverHost,
            serverPort = serverPort,
            proto = proto,
            routingScope = detectedScope,
            firewallType = firewallType,
            requiresUserAuth = requiresUserAuth,
            cipher = cipher.ifEmpty { "AES-256-GCM" },
            authDigest = authDigest.ifEmpty { "SHA256" },
            mtu = mtu,
            hasCaCert = hasCaCert,
            hasClientCert = hasClientCert,
            hasClientKey = hasClientKey,
            hasTlsAuth = hasTlsAuth,
            dnsServers = effectiveDns,
            routes = effectiveRoutes,
            isRedirectGateway = isRedirectGateway,
            suggestedName = suggestedName
        )
    }

    private fun detectFirewall(content: String, fileName: String): FirewallType {
        val combined = (content + " " + fileName).lowercase(Locale.ROOT)
        return when {
            combined.contains("sophos") || combined.contains("cyberoam") || combined.contains("xg_firewall") -> FirewallType.SOPHOS
            combined.contains("pfsense") || combined.contains("opnsense") -> FirewallType.PFSENSE
            combined.contains("fortinet") || combined.contains("fortigate") -> FirewallType.FORTINET
            combined.contains("mikrotik") || combined.contains("routeros") -> FirewallType.MIKROTIK
            combined.contains("openvpn") || combined.contains("as_") -> FirewallType.OPENVPN
            else -> FirewallType.OPENVPN
        }
    }

    private fun netmaskToCidr(mask: String): Int? {
        val parts = mask.split(".")
        if (parts.size != 4) return null
        return try {
            var cidr = 0
            for (p in parts) {
                val byteVal = p.toInt()
                cidr += Integer.bitCount(byteVal)
            }
            cidr
        } catch (_: Exception) {
            null
        }
    }
}
