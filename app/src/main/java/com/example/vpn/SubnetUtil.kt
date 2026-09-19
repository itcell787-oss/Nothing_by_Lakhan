package com.example.vpn

import java.net.InetAddress

/**
 * Utility for parsing, normalizing, and calculating IPv4 subnets and CIDR blocks.
 * Specifically supports /22 subnets (e.g., 192.168.4.1/22, 10.10.0.1/22, 192.168.0.1/22)
 * and ensures Android's VpnService.Builder.addRoute() receives valid network addresses with host bits zeroed.
 */
data class SubnetInfo(
    val originalInput: String,
    val gatewayOrHostIp: String,
    val networkAddress: String,
    val prefixLength: Int,
    val netmask: String,
    val broadcastAddress: String,
    val firstUsableIp: String,
    val lastUsableIp: String,
    val totalHosts: Int,
    val cidrNotation: String // e.g. "192.168.4.0/22"
) {
    fun contains(ip: String): Boolean {
        return SubnetUtil.isIpInSubnet(ip, cidrNotation)
    }

    val displayLabel: String
        get() = "$cidrNotation (GW: $gatewayOrHostIp | $firstUsableIp - $lastUsableIp)"
}

object SubnetUtil {

    // Default user gateway presets
    val DEFAULT_USER_GATEWAYS = listOf(
        "192.168.4.1/22",  // Share drives & LAN devices (192.168.4.0 - 192.168.7.255)
        "10.10.0.1/22",    // Internal homelab & infrastructure (10.10.0.0 - 10.10.3.255)
        "192.168.0.1/22"   // Local devices & self-hosted web (192.168.0.0 - 192.168.3.255)
    )

    /**
     * Parses an IP with CIDR or netmask (e.g. "192.168.4.1/22", "10.10.0.1/255.255.252.0", "192.168.0.1 22")
     * into structured [SubnetInfo].
     */
    fun parseSubnet(input: String): SubnetInfo? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val parts = if (trimmed.contains("/")) {
            trimmed.split("/")
        } else if (trimmed.contains(" ")) {
            trimmed.split(" ")
        } else {
            listOf(trimmed, "24")
        }

        val ipStr = parts.getOrNull(0)?.trim() ?: return null
        val maskPart = parts.getOrNull(1)?.trim() ?: "24"

        val prefixLength: Int = if (maskPart.contains(".")) {
            netmaskToPrefix(maskPart) ?: 24
        } else {
            maskPart.toIntOrNull()?.coerceIn(0, 32) ?: 24
        }

        val ipBytes = try {
            InetAddress.getByName(ipStr).address
        } catch (_: Exception) {
            return null
        }

        if (ipBytes.size != 4) return null // Only IPv4 supported for subnet mask arithmetic

        val ipInt = bytesToInt(ipBytes)
        val maskInt = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
        val networkInt = ipInt and maskInt
        val broadcastInt = networkInt or maskInt.inv()

        val netmaskStr = intToIp(maskInt)
        val networkStr = intToIp(networkInt)
        val broadcastStr = intToIp(broadcastInt)

        val firstUsableInt = if (prefixLength >= 31) networkInt else networkInt + 1
        val lastUsableInt = if (prefixLength >= 31) broadcastInt else broadcastInt - 1

        val totalHosts = if (prefixLength >= 31) 2 else (1 shl (32 - prefixLength)) - 2

        return SubnetInfo(
            originalInput = trimmed,
            gatewayOrHostIp = ipStr,
            networkAddress = networkStr,
            prefixLength = prefixLength,
            netmask = netmaskStr,
            broadcastAddress = broadcastStr,
            firstUsableIp = intToIp(firstUsableInt),
            lastUsableIp = intToIp(lastUsableInt),
            totalHosts = totalHosts.coerceAtLeast(1),
            cidrNotation = "$networkStr/$prefixLength"
        )
    }

    /**
     * Normalizes a route string (e.g. "192.168.4.1/22") to a pair of
     * (NetworkBaseAddress, PrefixLength) suitable for Android's VpnService.Builder.addRoute.
     * This eliminates IllegalArgumentException when host bits are non-zero.
     */
    fun normalizeToSubnetBase(routeInput: String): Pair<String, Int>? {
        val info = parseSubnet(routeInput) ?: return null
        return Pair(info.networkAddress, info.prefixLength)
    }

    /**
     * Tests if an IP address belongs to the specified CIDR subnet.
     */
    fun isIpInSubnet(ip: String, cidr: String): Boolean {
        return try {
            val targetBytes = InetAddress.getByName(ip.trim()).address
            if (targetBytes.size != 4) return false
            val targetInt = bytesToInt(targetBytes)

            val subnet = parseSubnet(cidr) ?: return false
            val maskInt = if (subnet.prefixLength == 0) 0 else (-1 shl (32 - subnet.prefixLength))
            val netInt = bytesToInt(InetAddress.getByName(subnet.networkAddress).address)

            (targetInt and maskInt) == netInt
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Finds which configured subnet (if any) contains the given IP.
     */
    fun findMatchingSubnet(ip: String, subnets: List<String>): SubnetInfo? {
        for (s in subnets) {
            val parsed = parseSubnet(s) ?: continue
            if (isIpInSubnet(ip, parsed.cidrNotation)) {
                return parsed
            }
        }
        return null
    }

    fun netmaskToPrefix(mask: String): Int? {
        val parts = mask.split(".")
        if (parts.size != 4) return null
        return try {
            var count = 0
            for (p in parts) {
                val b = p.toInt()
                count += Integer.bitCount(b)
            }
            count
        } catch (_: Exception) {
            null
        }
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        return ((bytes[0].toInt() and 0xFF) shl 24) or
               ((bytes[1].toInt() and 0xFF) shl 16) or
               ((bytes[2].toInt() and 0xFF) shl 8) or
               (bytes[3].toInt() and 0xFF)
    }

    private fun intToIp(value: Int): String {
        return "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}.${(value ushr 8) and 0xFF}.${value and 0xFF}"
    }
}
