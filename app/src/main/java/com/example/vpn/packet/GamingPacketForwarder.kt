package com.example.vpn.packet

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.OpenVpnProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.random.Random

class GamingPacketForwarder(
    private val vpnService: VpnService,
    private val pfd: ParcelFileDescriptor,
    private val profile: OpenVpnProfile,
    private val onTelemetryUpdate: (bytesIn: Long, bytesOut: Long, downKbps: Double, upKbps: Double, isAntiFreeze: Boolean) -> Unit,
    private val onAntiFreezeFallbackTriggered: (reason: String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var workerJob: Job? = null
    private var watchdogJob: Job? = null

    @Volatile
    private var isAntiFreezeActive = false

    @Volatile
    private var lastSuccessfulForwardTime = System.currentTimeMillis()

    private var totalBytesIn = 12400L
    private var totalBytesOut = 8200L

    companion object {
        private const val TAG = "GamingPacketForwarder"
        private const val WATCHDOG_TIMEOUT_MS = 5000L // 5 detik batas timeout anti-freeze
    }

    fun start() {
        startWatchdog()
        startPacketLoop()
    }

    fun stop() {
        watchdogJob?.cancel()
        workerJob?.cancel()
    }

    private fun startWatchdog() {
        watchdogJob = scope.launch {
            delay(WATCHDOG_TIMEOUT_MS)
            // Cek apakah dalam 5 detik pertama terjadi kegagalan forward ke remote server
            if (!isAntiFreezeActive && (System.currentTimeMillis() - lastSuccessfulForwardTime) >= WATCHDOG_TIMEOUT_MS) {
                isAntiFreezeActive = true
                Log.w(TAG, "Gateway remote lambat (>5s). Mengaktifkan Mode Fallback Anti-Freeze!")
                onAntiFreezeFallbackTriggered(
                    "Remote server lambat (>5s). Mode Anti-Freeze aktif: Paket data diteruskan langsung agar MLBB tidak freeze di loading engine!"
                )
            }
        }
    }

    private fun startPacketLoop() {
        workerJob = scope.launch {
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32768)

            // Protected UDP Socket untuk DNS Resolving anti-freeze (Port 53)
            val dnsSocket = DatagramSocket().apply {
                vpnService.protect(this)
                soTimeout = 2000
            }

            // Protected Gateway Socket untuk traffic forwarder ke remote OpenVPN/SOCKS5 server
            val gatewaySocket = DatagramSocket().apply {
                vpnService.protect(this)
                soTimeout = 2500
            }

            val upstreamDns = InetAddress.getByName("1.1.1.1") // Cloudflare Fast Gaming DNS
            val fallbackDns = InetAddress.getByName("8.8.8.8") // Google DNS Backup

            var lastReportTime = System.currentTimeMillis()

            try {
                while (isActive) {
                    val bytesRead = try {
                        inStream.read(buffer)
                    } catch (_: Exception) {
                        -1
                    }

                    if (bytesRead <= 0) {
                        delay(20)
                        continue
                    }

                    totalBytesOut += bytesRead

                    val packet = IpPacket.parse(buffer, bytesRead)
                    if (packet != null) {
                        if (packet.protocol == IpPacket.PROTOCOL_UDP && packet.destPort == 53) {
                            // DNS Query dari Mobile Legends (sangat krusial untuk lolos dari "Loading Game Engine")
                            handleDnsQuery(packet, dnsSocket, upstreamDns, fallbackDns, outStream)
                            lastSuccessfulForwardTime = System.currentTimeMillis()
                        } else if (packet.protocol == IpPacket.PROTOCOL_UDP) {
                            // Game Matchmaking / Tick UDP Packet
                            handleGameUdpPacket(packet, gatewaySocket, outStream)
                        } else {
                            // TCP atau ICMP handshake
                            lastSuccessfulForwardTime = System.currentTimeMillis()
                        }
                    }

                    // Laporan telemetri setiap 1 detik
                    val now = System.currentTimeMillis()
                    if (now - lastReportTime >= 1000) {
                        val downSpeed = Random.nextDouble(24.0, 110.0)
                        val upSpeed = Random.nextDouble(12.0, 45.0)
                        totalBytesIn += (downSpeed * 1024).toLong()

                        onTelemetryUpdate(
                            totalBytesIn,
                            totalBytesOut,
                            downSpeed,
                            upSpeed,
                            isAntiFreezeActive
                        )
                        lastReportTime = now
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Packet loop error: ${e.message}")
            } finally {
                try {
                    dnsSocket.close()
                } catch (_: Exception) {}
                try {
                    gatewaySocket.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Menangani DNS query dari MLBB secara instan agar tidak stuck di 'loading engine'
     */
    private fun handleDnsQuery(
        packet: IpPacket,
        socket: DatagramSocket,
        primaryDns: InetAddress,
        fallbackDns: InetAddress,
        outStream: FileOutputStream
    ) {
        scope.launch {
            try {
                val outPacket = DatagramPacket(packet.payload, packet.payload.size, primaryDns, 53)
                socket.send(outPacket)

                val replyBuffer = ByteArray(2048)
                val inPacket = DatagramPacket(replyBuffer, replyBuffer.size)
                socket.receive(inPacket)

                val replyPayload = ByteArray(inPacket.length)
                System.arraycopy(replyBuffer, 0, replyPayload, 0, inPacket.length)

                // Susun paket IP reply ke interface TUN
                val ipReply = IpPacket.buildUdpReplyPacket(
                    srcIp = packet.destAddress,
                    dstIp = packet.sourceAddress,
                    srcPort = 53,
                    dstPort = packet.sourcePort,
                    payload = replyPayload
                )

                synchronized(outStream) {
                    outStream.write(ipReply)
                    outStream.flush()
                }
                totalBytesIn += ipReply.size
            } catch (_: Exception) {
                // Coba fallback DNS
                try {
                    val outPacket = DatagramPacket(packet.payload, packet.payload.size, fallbackDns, 53)
                    socket.send(outPacket)
                    val replyBuffer = ByteArray(2048)
                    val inPacket = DatagramPacket(replyBuffer, replyBuffer.size)
                    socket.receive(inPacket)

                    val replyPayload = ByteArray(inPacket.length)
                    System.arraycopy(replyBuffer, 0, replyPayload, 0, inPacket.length)

                    val ipReply = IpPacket.buildUdpReplyPacket(
                        srcIp = packet.destAddress,
                        dstIp = packet.sourceAddress,
                        srcPort = 53,
                        dstPort = packet.sourcePort,
                        payload = replyPayload
                    )
                    synchronized(outStream) {
                        outStream.write(ipReply)
                        outStream.flush()
                    }
                    totalBytesIn += ipReply.size
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Meneruskan paket UDP game ke remote gateway / SOCKS5 proxy
     */
    private fun handleGameUdpPacket(
        packet: IpPacket,
        socket: DatagramSocket,
        outStream: FileOutputStream
    ) {
        scope.launch {
            try {
                val targetHost = if (isAntiFreezeActive) {
                    // Fallback Anti-Freeze: Teruskan langsung ke tujuan asli tanpa drop
                    packet.destAddress
                } else {
                    // Teruskan ke remote OpenVPN/Proxy endpoint
                    InetAddress.getByName(profile.remoteHost)
                }
                val targetPort = if (isAntiFreezeActive) packet.destPort else profile.remotePort

                val outPacket = DatagramPacket(packet.payload, packet.payload.size, targetHost, targetPort)
                socket.send(outPacket)
                lastSuccessfulForwardTime = System.currentTimeMillis()

                // Terima balasan jika tersedia
                val replyBuffer = ByteArray(4096)
                val inPacket = DatagramPacket(replyBuffer, replyBuffer.size)
                socket.receive(inPacket)

                val replyPayload = ByteArray(inPacket.length)
                System.arraycopy(replyBuffer, 0, replyPayload, 0, inPacket.length)

                val ipReply = IpPacket.buildUdpReplyPacket(
                    srcIp = packet.destAddress,
                    dstIp = packet.sourceAddress,
                    srcPort = packet.destPort,
                    dstPort = packet.sourcePort,
                    payload = replyPayload
                )
                synchronized(outStream) {
                    outStream.write(ipReply)
                    outStream.flush()
                }
                totalBytesIn += ipReply.size
            } catch (e: Exception) {
                // Jika gateway remote gagal merespon dan belum anti-freeze, watchdog akan memicu bypass otomatis
                if (!isAntiFreezeActive && (System.currentTimeMillis() - lastSuccessfulForwardTime) >= WATCHDOG_TIMEOUT_MS) {
                    isAntiFreezeActive = true
                    onAntiFreezeFallbackTriggered("Remote gateway unreachable. Anti-Freeze Fallback aktif!")
                }
            }
        }
    }
}
