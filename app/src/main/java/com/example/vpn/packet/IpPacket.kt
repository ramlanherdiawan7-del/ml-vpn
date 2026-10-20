package com.example.vpn.packet

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Utilitas parsing dan konstruksi paket IPv4 / UDP untuk interface TUN VpnService.
 */
class IpPacket private constructor(
    val version: Int,
    val headerLength: Int,
    val protocol: Int, // 17 = UDP, 6 = TCP, 1 = ICMP
    val sourceAddress: InetAddress,
    val destAddress: InetAddress,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray
) {
    companion object {
        const val PROTOCOL_ICMP = 1
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        fun parse(data: ByteArray, length: Int): IpPacket? {
            if (length < 20) return null

            val versionAndIhl = data[0].toInt() and 0xFF
            val version = versionAndIhl shr 4
            if (version != 4) return null // Hanya proses IPv4

            val ihl = (versionAndIhl and 0x0F) * 4
            if (length < ihl) return null

            val protocol = data[9].toInt() and 0xFF

            val srcIpBytes = ByteArray(4)
            System.arraycopy(data, 12, srcIpBytes, 0, 4)
            val dstIpBytes = ByteArray(4)
            System.arraycopy(data, 16, dstIpBytes, 0, 4)

            val srcAddress = InetAddress.getByAddress(srcIpBytes)
            val dstAddress = InetAddress.getByAddress(dstIpBytes)

            var srcPort = 0
            var dstPort = 0
            var payload = ByteArray(0)

            if (protocol == PROTOCOL_UDP && length >= ihl + 8) {
                srcPort = ((data[ihl].toInt() and 0xFF) shl 8) or (data[ihl + 1].toInt() and 0xFF)
                dstPort = ((data[ihl + 2].toInt() and 0xFF) shl 8) or (data[ihl + 3].toInt() and 0xFF)
                val udpLength = ((data[ihl + 4].toInt() and 0xFF) shl 8) or (data[ihl + 5].toInt() and 0xFF)
                val payloadLength = (udpLength - 8).coerceAtLeast(0)
                val availablePayload = (length - ihl - 8).coerceAtMost(payloadLength)

                if (availablePayload > 0) {
                    payload = ByteArray(availablePayload)
                    System.arraycopy(data, ihl + 8, payload, 0, availablePayload)
                }
            } else if (protocol == PROTOCOL_TCP && length >= ihl + 20) {
                srcPort = ((data[ihl].toInt() and 0xFF) shl 8) or (data[ihl + 1].toInt() and 0xFF)
                dstPort = ((data[ihl + 2].toInt() and 0xFF) shl 8) or (data[ihl + 3].toInt() and 0xFF)
                val dataOffset = ((data[ihl + 12].toInt() and 0xFF) shr 4) * 4
                val tcpPayloadOffset = ihl + dataOffset
                if (length > tcpPayloadOffset) {
                    val payloadSize = length - tcpPayloadOffset
                    payload = ByteArray(payloadSize)
                    System.arraycopy(data, tcpPayloadOffset, payload, 0, payloadSize)
                }
            }

            return IpPacket(
                version = version,
                headerLength = ihl,
                protocol = protocol,
                sourceAddress = srcAddress,
                destAddress = dstAddress,
                sourcePort = srcPort,
                destPort = dstPort,
                payload = payload
            )
        }

        /**
         * Membangun paket balasan IPv4 UDP sintetis untuk dituliskan kembali ke TUN interface.
         */
        fun buildUdpReplyPacket(
            srcIp: InetAddress,
            dstIp: InetAddress,
            srcPort: Int,
            dstPort: Int,
            payload: ByteArray
        ): ByteArray {
            val ipHeaderLength = 20
            val udpHeaderLength = 8
            val totalLength = ipHeaderLength + udpHeaderLength + payload.size
            val packet = ByteArray(totalLength)
            val buffer = ByteBuffer.wrap(packet)

            // IPv4 Header
            buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
            buffer.put(0x00.toByte()) // TOS
            buffer.putShort(totalLength.toShort()) // Total length
            buffer.putShort(0x0000.toShort()) // ID
            buffer.putShort(0x4000.toShort()) // Don't Fragment
            buffer.put(64.toByte()) // TTL
            buffer.put(PROTOCOL_UDP.toByte()) // Protocol UDP
            buffer.putShort(0.toShort()) // Checksum placeholder
            buffer.put(srcIp.address)
            buffer.put(dstIp.address)

            // Hitung checksum IPv4
            val ipChecksum = calculateChecksum(packet, 0, ipHeaderLength)
            buffer.putShort(10, ipChecksum.toShort())

            // UDP Header
            buffer.putShort(srcPort.toShort())
            buffer.putShort(dstPort.toShort())
            buffer.putShort((udpHeaderLength + payload.size).toShort())
            buffer.putShort(0.toShort()) // UDP Checksum (0 = diabaikan di IPv4)

            // Payload
            buffer.put(payload)

            return packet
        }

        private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
            var sum = 0
            var i = offset
            while (i < offset + length - 1) {
                val b1 = data[i].toInt() and 0xFF
                val b2 = data[i + 1].toInt() and 0xFF
                sum += (b1 shl 8) or b2
                i += 2
            }
            if (i < offset + length) {
                val b = data[i].toInt() and 0xFF
                sum += (b shl 8)
            }
            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            return (sum.inv()) and 0xFFFF
        }
    }
}
