package com.example.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class VpnSessionState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activeServer: GameServer? = null,
    val durationSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val currentDownloadSpeedKbps: Double = 0.0,
    val currentUploadSpeedKbps: Double = 0.0,
    val isSplitTunnelActive: Boolean = true,
    val isMlbbInstalled: Boolean = false,
    val autoDisconnectSeconds: Int = 0, // 0 = disabled, 60, 90, 120
    val autoDisconnectRemainingSeconds: Int? = null,
    val isFloatingOverlayEnabled: Boolean = true,
    val errorMessage: String? = null
)
