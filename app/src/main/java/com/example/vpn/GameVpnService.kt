package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.OpenVpnProfile
import com.example.data.OpenVpnProfiles
import com.example.data.ServerPresets
import com.example.model.ConnectionStatus
import com.example.model.GameServer
import com.example.vpn.packet.GamingPacketForwarder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetForwarder: GamingPacketForwarder? = null
    private var autoDisconnectJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var currentServer: GameServer? = null
    private var isSplitTunnel: Boolean = true
    private var autoDisconnectSeconds: Int = 0
    private var sessionSeconds: Long = 0L

    companion object {
        const val ACTION_START = "com.example.vpn.ACTION_START"
        const val ACTION_STOP = "com.example.vpn.ACTION_STOP"
        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SPLIT_TUNNEL = "extra_split_tunnel"
        const val EXTRA_AUTO_DISCONNECT_SECONDS = "extra_auto_disconnect_seconds"
        const val CHANNEL_ID = "ml_gaming_vpn_channel"
        const val NOTIFICATION_ID = 7711
        const val MLBB_PACKAGE = "com.mobile.legends"

        // MTU 1360 dioptimasi untuk mencegah fragmentasi paket UDP Mobile Legends pada jaringan seluler 4G/5G
        const val GAMING_OPTIMIZED_MTU = 1360

        fun startVpnIntent(context: Context, serverId: String, splitTunnel: Boolean, autoDisconnectSec: Int = 0): Intent {
            return Intent(context, GameVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_ID, serverId)
                putExtra(EXTRA_SPLIT_TUNNEL, splitTunnel)
                putExtra(EXTRA_AUTO_DISCONNECT_SECONDS, autoDisconnectSec)
            }
        }

        fun stopVpnIntent(context: Context): Intent {
            return Intent(context, GameVpnService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            disconnectVpn()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val serverId = intent.getStringExtra(EXTRA_SERVER_ID)
            isSplitTunnel = intent.getBooleanExtra(EXTRA_SPLIT_TUNNEL, true)
            autoDisconnectSeconds = intent.getIntExtra(EXTRA_AUTO_DISCONNECT_SECONDS, 0)
            val server = ServerPresets.DEFAULT_SERVERS.find { it.id == serverId }
                ?: ServerPresets.DEFAULT_SERVERS.first()
            currentServer = server

            startVpnTunnel(server, isSplitTunnel, autoDisconnectSeconds)
            return START_STICKY
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: GameServer, splitTunnel: Boolean, autoDisconnectSec: Int) {
        VpnManager.updateStatus(ConnectionStatus.CONNECTING, server)
        VpnManager.updateAntiFreezeStatus(false, null)

        val notification = buildNotification(server, "Menghubungkan ke ${server.country}...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            try {
                // 1. Dapatkan profil OpenVPN / SOCKS5 nyata untuk server target
                val profile = OpenVpnProfiles.PROFILES[server.country] ?: OpenVpnProfile(
                    id = "ovpn_${server.id}",
                    country = server.country,
                    remoteHost = server.hostIp,
                    remotePort = server.port,
                    rawOvpnContent = OpenVpnProfiles.CAMBODIA_OVPN,
                    socks5Host = server.hostIp,
                    socks5Port = 1080
                )

                // 2. Konfigurasi Interface VpnService Android dengan MTU 1360
                val builder = Builder()
                    .setSession("MLBB OpenVPN/UDP Tunnel - ${server.country}")
                    .setMtu(GAMING_OPTIMIZED_MTU)
                    .addAddress("10.8.0.2", 24)
                    .addDnsServer("1.1.1.1") // Cloudflare Fast Gaming DNS
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0)

                // 3. Split Tunneling otomatis menggunakan builder.addAllowedApplication("com.mobile.legends")
                var splitApplied = false
                if (splitTunnel) {
                    try {
                        packageManager.getPackageInfo(MLBB_PACKAGE, 0)
                        builder.addAllowedApplication(MLBB_PACKAGE)
                        splitApplied = true
                    } catch (_: PackageManager.NameNotFoundException) {
                        splitApplied = false
                    }
                }

                vpnInterface?.close()
                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    VpnManager.updateSession(
                        status = ConnectionStatus.CONNECTED,
                        server = server,
                        splitActive = splitApplied
                    )
                    updateNotification(server, "Aktif (MTU 1360 • Forwarder Aktif)")

                    // 4. Mulai Packet Forwarder nyata dengan watchdog anti-freeze 5 detik
                    startPacketForwarderEngine(vpnInterface!!, profile, server)

                    // 5. Timer Auto-Disconnect jika diaktifkan (60s / 90s / 120s)
                    startAutoDisconnectCountdown(autoDisconnectSec)
                } else {
                    VpnManager.updateStatus(ConnectionStatus.ERROR, server, "Gagal menginisialisasi interface TUN VPN.")
                    stopSelf()
                }
            } catch (e: Exception) {
                VpnManager.updateStatus(ConnectionStatus.ERROR, server, e.localizedMessage ?: "VPN Error")
                stopSelf()
            }
        }
    }

    private fun startPacketForwarderEngine(pfd: ParcelFileDescriptor, profile: OpenVpnProfile, server: GameServer) {
        packetForwarder?.stop()
        sessionSeconds = 0L

        packetForwarder = GamingPacketForwarder(
            vpnService = this,
            pfd = pfd,
            profile = profile,
            onTelemetryUpdate = { bytesIn, bytesOut, downKbps, upKbps, isAntiFreeze ->
                sessionSeconds++
                VpnManager.updateTelemetry(
                    durationSec = sessionSeconds,
                    bytesIn = bytesIn,
                    bytesOut = bytesOut,
                    downKbps = downKbps,
                    upKbps = upKbps,
                    isAntiFreeze = isAntiFreeze
                )

                if (sessionSeconds % 10 == 0L) {
                    val minutes = sessionSeconds / 60
                    val seconds = sessionSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)
                    val statusText = if (isAntiFreeze) "Anti-Freeze Aktif • Game Lancar" else "Forwarder Aktif • MTU 1360"
                    updateNotification(server, "$timeStr • $statusText")
                }
            },
            onAntiFreezeFallbackTriggered = { reason ->
                VpnManager.updateAntiFreezeStatus(true, reason)
                updateNotification(server, "⚠️ Anti-Freeze Aktif: Loading MLBB Aman!")
            }
        ).apply {
            start()
        }
    }

    private fun startAutoDisconnectCountdown(durationSec: Int) {
        autoDisconnectJob?.cancel()
        if (durationSec <= 0) {
            VpnManager.updateAutoDisconnectTimer(null)
            return
        }

        autoDisconnectJob = serviceScope.launch {
            var remaining = durationSec
            while (isActive && remaining > 0) {
                VpnManager.updateAutoDisconnectTimer(remaining)
                delay(1000)
                remaining--
            }

            if (isActive && remaining <= 0) {
                VpnManager.updateAutoDisconnectTimer(0)
                disconnectVpn("Auto-Disconnect: Waktu matchmaking selesai (${durationSec}s). Beralih ke jaringan lokal!")
            }
        }
    }

    private fun disconnectVpn(customMessage: String? = null) {
        autoDisconnectJob?.cancel()
        autoDisconnectJob = null
        VpnManager.updateAutoDisconnectTimer(null)

        packetForwarder?.stop()
        packetForwarder = null

        VpnManager.updateStatus(ConnectionStatus.DISCONNECTING, currentServer)

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        VpnManager.updateStatus(ConnectionStatus.DISCONNECTED, null, customMessage)
        VpnManager.updateAntiFreezeStatus(false, null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status koneksi server MLBB Gaming VPN"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(server: GameServer, statusDetail: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = stopVpnIntent(this)
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ML Server VPN: ${server.country} ${server.flagEmoji}")
            .setContentText(statusDetail)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.vpn_disconnect),
                disconnectPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(server: GameServer, statusDetail: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(server, statusDetail))
    }

    override fun onDestroy() {
        disconnectVpn()
        super.onDestroy()
    }
}
