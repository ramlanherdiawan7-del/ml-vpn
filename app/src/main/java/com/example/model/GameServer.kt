package com.example.model

/**
 * Data Model GameServer sesuai spesifikasi ML Server Switcher
 */
data class GameServer(
    val id: String,
    val country: String,
    val city: String,
    val flagEmoji: String,
    val hostIp: String,
    val port: Int = 443,
    val categoryTag: String, // "Low Ping", "Easy Match", "Bot Lobbies"
    val displayTag: String,  // e.g. "Recommended (Lowest Ping)", "Casual Lobby"
    val matchmakingNote: String,
    val baselineMs: Int,
    val pingMs: Int? = null,
    val isCustom: Boolean = false
) {
    // Getter untuk kompatibilitas tampilan
    val countryName: String get() = country
    val gatewayHost: String get() = hostIp
    val gatewayPort: Int get() = port
    val tag: String get() = displayTag

    /**
     * Indikator warna sesuai spesifikasi:
     * - Hijau (< 80 ms)
     * - Kuning (80 - 150 ms)
     * - Merah (> 150 ms)
     */
    fun getPingColor(): Long {
        val ping = pingMs ?: baselineMs
        return when {
            ping < 80 -> 0xFF10B981   // Hijau
            ping <= 150 -> 0xFFF59E0B // Kuning
            else -> 0xFFEF4444        // Merah
        }
    }

    fun getPingCategoryText(): String {
        val ping = pingMs ?: baselineMs
        return when {
            ping < 80 -> "Sangat Cepat"
            ping <= 150 -> "Stabil"
            else -> "Tinggi"
        }
    }
}
