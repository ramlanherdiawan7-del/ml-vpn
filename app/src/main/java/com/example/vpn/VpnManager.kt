package com.example.vpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import com.example.model.ConnectionStatus
import com.example.model.GameServer
import com.example.model.VpnSessionState
import com.example.overlay.FloatingVpnOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnManager {

    const val MLBB_PACKAGE_NAME = "com.mobile.legends"

    private val _sessionState = MutableStateFlow(VpnSessionState())
    val sessionState: StateFlow<VpnSessionState> = _sessionState.asStateFlow()

    fun isMlbbInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    MLBB_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(MLBB_PACKAGE_NAME, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun prepareVpn(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun startVpn(
        context: Context,
        server: GameServer,
        splitTunnel: Boolean,
        autoDisconnectSeconds: Int = 0
    ) {
        val intent = GameVpnService.startVpnIntent(
            context = context,
            serverId = server.id,
            splitTunnel = splitTunnel,
            autoDisconnectSec = autoDisconnectSeconds
        )
        context.startForegroundService(intent)

        if (_sessionState.value.isFloatingOverlayEnabled && hasOverlayPermission(context)) {
            FloatingVpnOverlayService.start(context)
        }
    }

    fun stopVpn(context: Context) {
        val intent = GameVpnService.stopVpnIntent(context)
        context.startService(intent)
        FloatingVpnOverlayService.stop(context)
    }

    fun launchMobileLegends(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(MLBB_PACKAGE_NAME)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            // Jika MLBB belum terpasang, buka link Google Play Store
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$MLBB_PACKAGE_NAME")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            } catch (_: Exception) {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$MLBB_PACKAGE_NAME")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            false
        }
    }

    fun updateFloatingOverlayToggle(enabled: Boolean) {
        _sessionState.update { it.copy(isFloatingOverlayEnabled = enabled) }
    }

    fun updateAutoDisconnectSetting(seconds: Int) {
        _sessionState.update { it.copy(autoDisconnectSeconds = seconds) }
    }

    fun updateAutoDisconnectTimer(remainingSeconds: Int?) {
        _sessionState.update { it.copy(autoDisconnectRemainingSeconds = remainingSeconds) }
    }

    fun updateStatus(status: ConnectionStatus, server: GameServer?, error: String? = null) {
        _sessionState.update {
            it.copy(
                status = status,
                activeServer = if (status == ConnectionStatus.DISCONNECTED) null else (server ?: it.activeServer),
                errorMessage = error,
                durationSeconds = if (status == ConnectionStatus.DISCONNECTED) 0L else it.durationSeconds
            )
        }
    }

    fun updateSession(status: ConnectionStatus, server: GameServer, splitActive: Boolean) {
        _sessionState.update {
            it.copy(
                status = status,
                activeServer = server,
                isSplitTunnelActive = splitActive,
                errorMessage = null
            )
        }
    }

    fun updateAntiFreezeStatus(isActive: Boolean, message: String? = null) {
        _sessionState.update {
            it.copy(
                isAntiFreezeActive = isActive,
                antiFreezeMessage = message
            )
        }
    }

    fun updateTelemetry(
        durationSec: Long,
        bytesIn: Long,
        bytesOut: Long,
        downKbps: Double,
        upKbps: Double,
        isAntiFreeze: Boolean = false
    ) {
        _sessionState.update {
            it.copy(
                durationSeconds = durationSec,
                bytesIn = bytesIn,
                bytesOut = bytesOut,
                currentDownloadSpeedKbps = downKbps,
                currentUploadSpeedKbps = upKbps,
                isAntiFreezeActive = isAntiFreeze || it.isAntiFreezeActive
            )
        }
    }
}
