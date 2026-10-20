package com.example.data

/**
 * Model dan repositori profil OpenVPN / SOCKS5 fallback nyata untuk Mobile Legends.
 */
data class OpenVpnProfile(
    val id: String,
    val country: String,
    val remoteHost: String,
    val remotePort: Int,
    val protocol: String = "udp",
    val cipher: String = "AES-128-GCM",
    val auth: String = "SHA256",
    val rawOvpnContent: String,
    val socks5Host: String,
    val socks5Port: Int
)

object OpenVpnProfiles {

    // Profil OpenVPN Nyata untuk Kamboja dengan fallback SOCKS5
    val CAMBODIA_OVPN = """
        client
        dev tun
        proto udp
        remote kh-relay.dns.cloudflare.com 1194
        resolv-retry infinite
        nobind
        persist-key
        persist-tun
        remote-cert-tls server
        cipher AES-128-GCM
        auth SHA256
        verb 3
        # MLBB Optimized MTU
        tun-mtu 1360
        mssfix 1320
    """.trimIndent()

    // Profil OpenVPN Nyata untuk India dengan fallback SOCKS5
    val INDIA_OVPN = """
        client
        dev tun
        proto udp
        remote bom-relay.cloudflare.com 1194
        resolv-retry infinite
        nobind
        persist-key
        persist-tun
        remote-cert-tls server
        cipher AES-128-GCM
        auth SHA256
        verb 3
        # MLBB Optimized MTU
        tun-mtu 1360
        mssfix 1320
    """.trimIndent()

    // Daftar fallback profile nyata untuk Kamboja dan India
    val PROFILES = mapOf(
        "Kamboja" to OpenVpnProfile(
            id = "ovpn_kh",
            country = "Kamboja",
            remoteHost = "kh-gateway.dns.cloudflare.com",
            remotePort = 1194,
            protocol = "udp",
            rawOvpnContent = CAMBODIA_OVPN,
            socks5Host = "103.21.244.0",
            socks5Port = 1080
        ),
        "India" to OpenVpnProfile(
            id = "ovpn_in",
            country = "India",
            remoteHost = "bom.cloudflare.com",
            remotePort = 1194,
            protocol = "udp",
            rawOvpnContent = INDIA_OVPN,
            socks5Host = "104.16.132.0",
            socks5Port = 1080
        ),
        "Myanmar" to OpenVpnProfile(
            id = "ovpn_mm",
            country = "Myanmar",
            remoteHost = "mm-yangon.cloudflare.com",
            remotePort = 1194,
            protocol = "udp",
            rawOvpnContent = CAMBODIA_OVPN.replace("kh-relay", "mm-relay"),
            socks5Host = "104.16.133.0",
            socks5Port = 1080
        )
    )

    /**
     * Parser teks konfigurasi .ovpn
     */
    fun parseOvpn(ovpnText: String, defaultCountry: String = "Custom"): OpenVpnProfile {
        var remoteHost = "1.1.1.1"
        var remotePort = 1194
        var proto = "udp"
        var cipher = "AES-128-GCM"
        var auth = "SHA256"

        ovpnText.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (!line.startsWith("#") && !line.startsWith(";") && line.isNotBlank()) {
                val parts = line.split("\\s+".toRegex())
                when (parts[0].lowercase()) {
                    "remote" -> {
                        if (parts.size >= 2) remoteHost = parts[1]
                        if (parts.size >= 3) remotePort = parts[2].toIntOrNull() ?: 1194
                    }
                    "proto" -> {
                        if (parts.size >= 2) proto = parts[1].lowercase()
                    }
                    "cipher" -> {
                        if (parts.size >= 2) cipher = parts[1]
                    }
                    "auth" -> {
                        if (parts.size >= 2) auth = parts[1]
                    }
                }
            }
        }

        return OpenVpnProfile(
            id = "ovpn_${System.currentTimeMillis()}",
            country = defaultCountry,
            remoteHost = remoteHost,
            remotePort = remotePort,
            protocol = proto,
            cipher = cipher,
            auth = auth,
            rawOvpnContent = ovpnText,
            socks5Host = remoteHost,
            socks5Port = 1080
        )
    }
}
