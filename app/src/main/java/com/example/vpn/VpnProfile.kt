package com.example.vpn

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class VpnRoutingScope(val displayName: String, val badge: String, val description: String) {
    DIRECT_LAN_BRIDGE("Direct LAN / Homelab", "BRIDGE", "Direct LAN access: connect directly to gateways 192.168.4.1/22, 10.10.0.1/22, 192.168.0.1/22 without VPN interference"),
    DRIVE_ONLY("Drive Only (Subnets)", "DRIVE", "Storage tunnel: only file explorer & configured private subnets route through VPN"),
    WHOLE_PHONE("Whole Phone (Subnet-Safe)", "DEVICE", "Device tunnel: routes homelab subnets without blackholing internet/websites")
}

enum class VpnAuthType {
    USER_PASS,
    CERTIFICATE,
    PKCS12,
    NONE
}

enum class FirewallType(val displayName: String) {
    OPENVPN("OpenVPN Access Server"),
    SOPHOS("Sophos UTM / XG"),
    PFSENSE("pfSense / OPNsense"),
    FORTINET("Fortinet FortiGate"),
    MIKROTIK("MikroTik RouterOS"),
    CUSTOM("Custom OpenVPN Server")
}

data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "OpenVPN Tunnel",
    val serverHost: String = "",
    val serverPort: Int = 1194,
    val proto: String = "UDP", // "UDP" or "TCP"
    val routingScope: VpnRoutingScope = VpnRoutingScope.WHOLE_PHONE,
    val firewallType: FirewallType = FirewallType.OPENVPN,
    val authType: VpnAuthType = VpnAuthType.USER_PASS,
    val username: String = "",
    val password: String = "",
    val saveCredentials: Boolean = true,
    val ovpnContent: String = "",
    val fileName: String = "",
    val dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val routes: List<String> = listOf("192.168.4.1/22", "10.10.0.1/22", "192.168.0.1/22"),
    val splitTunnel: Boolean = true,
    val assignedIp: String = "10.8.0.2",
    val mtu: Int = 1500,
    val cipher: String = "AES-256-GCM",
    val authDigest: String = "SHA256",
    val killSwitch: Boolean = false,
    val autoReconnect: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long = 0L
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("serverHost", serverHost)
        json.put("serverPort", serverPort)
        json.put("proto", proto)
        json.put("routingScope", routingScope.name)
        json.put("firewallType", firewallType.name)
        json.put("authType", authType.name)
        json.put("username", username)
        if (saveCredentials) {
            json.put("password", password)
        }
        json.put("saveCredentials", saveCredentials)
        json.put("ovpnContent", ovpnContent)
        json.put("fileName", fileName)
        json.put("dnsServers", JSONArray(dnsServers))
        json.put("routes", JSONArray(routes))
        json.put("splitTunnel", splitTunnel)
        json.put("assignedIp", assignedIp)
        json.put("mtu", mtu)
        json.put("cipher", cipher)
        json.put("authDigest", authDigest)
        json.put("killSwitch", killSwitch)
        json.put("autoReconnect", autoReconnect)
        json.put("createdAt", createdAt)
        json.put("lastConnectedAt", lastConnectedAt)
        return json
    }

    companion object {
        fun createDefaultHomelabProfile(): VpnProfile {
            return VpnProfile(
                id = "preset-homelab-bridge",
                name = "Direct LAN Gateway / Homelab",
                serverHost = "192.168.4.1",
                serverPort = 1194,
                proto = "UDP",
                routingScope = VpnRoutingScope.DIRECT_LAN_BRIDGE,
                firewallType = FirewallType.CUSTOM,
                authType = VpnAuthType.NONE,
                username = "",
                password = "",
                dnsServers = listOf("192.168.4.1", "1.1.1.1"),
                routes = listOf("192.168.4.1/22", "10.10.0.1/22", "192.168.0.1/22"),
                splitTunnel = true,
                assignedIp = "192.168.4.50",
                mtu = 1500,
                cipher = "AES-256-GCM"
            )
        }

        fun createDefaultWholePhoneProfile(): VpnProfile {
            return VpnProfile(
                id = "preset-whole-phone",
                name = "Secure Whole Phone Gateway",
                serverHost = "vpn.secure-tunnel.net",
                serverPort = 1194,
                proto = "UDP",
                routingScope = VpnRoutingScope.WHOLE_PHONE,
                firewallType = FirewallType.OPENVPN,
                authType = VpnAuthType.USER_PASS,
                username = "client_mobile",
                password = "",
                dnsServers = listOf("1.1.1.1", "8.8.8.8"),
                routes = listOf("0.0.0.0/0"),
                splitTunnel = false,
                assignedIp = "10.8.0.2",
                mtu = 1500,
                cipher = "AES-256-GCM"
            )
        }

        fun createDefaultDriveOnlyProfile(): VpnProfile {
            return VpnProfile(
                id = "preset-drive-only",
                name = "Remote Drive & SMB Gateway",
                serverHost = "storage.corp-gateway.com",
                serverPort = 1194,
                proto = "UDP",
                routingScope = VpnRoutingScope.DRIVE_ONLY,
                firewallType = FirewallType.SOPHOS,
                authType = VpnAuthType.USER_PASS,
                username = "admin_storage",
                password = "",
                dnsServers = listOf("1.1.1.1", "10.0.0.1"),
                routes = listOf("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12"),
                splitTunnel = true,
                assignedIp = "10.8.0.5",
                mtu = 1500,
                cipher = "AES-256-GCM"
            )
        }

        fun fromJson(json: JSONObject): VpnProfile {
            val dnsList = mutableListOf<String>()
            val dnsArr = json.optJSONArray("dnsServers")
            if (dnsArr != null) {
                for (i in 0 until dnsArr.length()) {
                    dnsList.add(dnsArr.getString(i))
                }
            } else {
                dnsList.addAll(listOf("1.1.1.1", "8.8.8.8"))
            }

            val routeList = mutableListOf<String>()
            val routeArr = json.optJSONArray("routes")
            if (routeArr != null) {
                for (i in 0 until routeArr.length()) {
                    routeList.add(routeArr.getString(i))
                }
            } else {
                routeList.addAll(listOf("0.0.0.0/0"))
            }

            val fwType = try {
                FirewallType.valueOf(json.optString("firewallType", FirewallType.OPENVPN.name))
            } catch (_: Exception) {
                FirewallType.OPENVPN
            }

            val authType = try {
                VpnAuthType.valueOf(json.optString("authType", VpnAuthType.USER_PASS.name))
            } catch (_: Exception) {
                VpnAuthType.USER_PASS
            }

            val routingScope = try {
                VpnRoutingScope.valueOf(json.optString("routingScope", VpnRoutingScope.WHOLE_PHONE.name))
            } catch (_: Exception) {
                VpnRoutingScope.WHOLE_PHONE
            }

            return VpnProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "OpenVPN Profile"),
                serverHost = json.optString("serverHost", ""),
                serverPort = json.optInt("serverPort", 1194),
                proto = json.optString("proto", "UDP"),
                routingScope = routingScope,
                firewallType = fwType,
                authType = authType,
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                saveCredentials = json.optBoolean("saveCredentials", true),
                ovpnContent = json.optString("ovpnContent", ""),
                fileName = json.optString("fileName", ""),
                dnsServers = dnsList,
                routes = routeList,
                splitTunnel = json.optBoolean("splitTunnel", false),
                assignedIp = json.optString("assignedIp", "10.8.0.2"),
                mtu = json.optInt("mtu", 1500),
                cipher = json.optString("cipher", "AES-256-GCM"),
                authDigest = json.optString("authDigest", "SHA256"),
                killSwitch = json.optBoolean("killSwitch", false),
                autoReconnect = json.optBoolean("autoReconnect", true),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                lastConnectedAt = json.optLong("lastConnectedAt", 0L)
            )
        }
    }
}
