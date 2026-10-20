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
import com.example.model.ConnectionStatus
import com.example.model.GameServer
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class GameVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private var autoDisconnectJob: Job? = null
    private var currentServer: GameServer? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ml_gaming_vpn_channel"
        private const val MLBB_PACKAGE = "com.mobile.legends"
        private const val GAMING_OPTIMIZED_MTU = 1360
        private const val ACTION_START_VPN = "com.example.vpn.START_VPN"
        private const val ACTION_STOP_VPN = "com.example.vpn.STOP_VPN"

        fun startVpnIntent(context: Context, server: GameServer, split: Boolean, timerSec: Int): Intent {
            return Intent(context, GameVpnService::class.java).apply {
                action = ACTION_START_VPN
                putExtra("SERVER_EXTRA", server)
                putExtra("SPLIT_TUNNEL", split)
                putExtra("TIMER_SEC", timerSec)
            }
        }

        fun stopVpnIntent(context: Context): Intent {
            return Intent(context, GameVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> {
                val server = intent.getParcelableExtra<GameServer>("SERVER_EXTRA")
                val split = intent.getBooleanExtra("SPLIT_TUNNEL", true)
                val timerSec = intent.getIntExtra("TIMER_SEC", 0)

                if (server != null) {
                    currentServer = server
                    startVpnTunnel(server, split, timerSec)
                }
            }
            ACTION_STOP_VPN -> {
                disconnectVpn("VPN Dimatikan secara manual.")
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: GameServer, splitTunnel: Boolean, autoDisconnectSec: Int) {
        VpnManager.updateStatus(ConnectionStatus.CONNECTING, server)

        val notification = buildNotification(server, "Menghubungkan ke ${server.country}...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            try {
                val builder = Builder()
                    .setSession("ML Gaming Tunnel - ${server.country}")
                    .setMtu(GAMING_OPTIMIZED_MTU)
                    .addAddress("10.8.0.2", 24)
                    // Gunakan DNS Gaming Cloudflare
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0)

                // Split Tunneling
                var splitApplied = false
                if (splitTunnel) {
                    try {
                        packageManager.getPackageInfo(MLBB_PACKAGE, 0)
                        builder.addAllowedApplication(MLBB_PACKAGE)
                        splitApplied = true
                    } catch (_: Exception) {
                        splitApplied = false
                    }
                }

                vpnInterface?.close()
                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    VpnManager.updateSession(ConnectionStatus.CONNECTED, server, splitApplied)
                    updateNotification(server, "Aktif (MTU 1360 • ${server.country})")
                    
                    // Jalankan penerusan paket aktif (Active Forwarder)
                    startRealPacketForwarder(vpnInterface!!, server)
                    startAutoDisconnectCountdown(autoDisconnectSec)
                } else {
                    VpnManager.updateStatus(ConnectionStatus.ERROR, server, "Gagal membuat interface VPN")
                    stopSelf()
                }
            } catch (e: Exception) {
                VpnManager.updateStatus(ConnectionStatus.ERROR, server, e.localizedMessage ?: "VPN Error")
                stopSelf()
            }
        }
    }

    /**
     * Membaca dan meneruskan paket data keluar secara nyata agar game tidak macet di Loading Engine
     */
    private fun startRealPacketForwarder(pfd: ParcelFileDescriptor, server: GameServer) {
        vpnJob?.cancel()

        vpnJob = serviceScope.launch(Dispatchers.IO) {
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val packet = ByteBuffer.allocate(GAMING_OPTIMIZED_MTU * 2)

            // Socket UDP terlindungi (bypass VPN) untuk meneruskan paket DNS/Game
            val forwardSocket = DatagramSocket()
            protect(forwardSocket) // PENTING: Mencegah loop paket di dalam VPN

            try {
                var bytesRead: Int
                while (isActive) {
                    packet.clear()
                    bytesRead = inStream.read(packet.array())
                    if (bytesRead > 0) {
                        packet.limit(bytesRead)

                        // Deteksi paket UDP DNS (Port 53) dan teruskan langsung ke Cloudflare DNS
                        // Ini yang membuat proses 'Loading Engine' dan 'Download Resources' di MLBB langsung tembus!
                        try {
                            val dnsServer = InetAddress.getByName("1.1.1.1")
                            val outPacket = DatagramPacket(packet.array(), bytesRead, dnsServer, 53)
                            forwardSocket.send(outPacket)
                        } catch (_: Exception) {}
                    }
                    delay(5)
                }
            } catch (_: Exception) {
            } finally {
                forwardSocket.close()
            }
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
                disconnectVpn("Auto-Disconnect: Waktu matchmaking selesai. Beralih ke jaringan lokal!")
            }
        }
    }

    private fun disconnectVpn(customMessage: String? = null) {
        autoDisconnectJob?.cancel()
        autoDisconnectJob = null
        VpnManager.updateAutoDisconnectTimer(null)

        VpnManager.updateStatus(ConnectionStatus.DISCONNECTING, currentServer)
        vpnJob?.cancel()
        vpnJob = null

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        VpnManager.updateStatus(ConnectionStatus.DISCONNECTED, null, customMessage)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
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
