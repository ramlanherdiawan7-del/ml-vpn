package com.example

import com.example.data.OpenVpnProfiles
import com.example.data.ServerPresets
import com.example.model.ConnectionStatus
import com.example.model.VpnSessionState
import com.example.vpn.GameVpnService
import com.example.vpn.packet.IpPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class ExampleUnitTest {

    @Test
    fun testServerPresets_containsAllRequiredRegions() {
        val servers = ServerPresets.DEFAULT_SERVERS
        assertTrue(servers.isNotEmpty())

        val countries = servers.map { it.country }
        assertTrue("Harus mencakup server Kamboja", countries.contains("Kamboja"))
        assertTrue("Harus mencakup server Myanmar", countries.contains("Myanmar"))
        assertTrue("Harus mencakup server Laos", countries.contains("Laos"))
        assertTrue("Harus mencakup server India", countries.contains("India"))
        assertTrue("Harus mencakup server Turki", countries.contains("Turki"))

        val kamboja = servers.first { it.country == "Kamboja" }
        assertEquals("Recommended (Lowest Ping)", kamboja.displayTag)
        assertEquals("Low Ping", kamboja.categoryTag)

        val laos = servers.first { it.country == "Laos" }
        assertEquals("Low Playerbase", laos.displayTag)
    }

    @Test
    fun testServerPresets_pingColorThresholds() {
        val server = ServerPresets.DEFAULT_SERVERS.first()

        // Hijau (< 80 ms)
        val lowPing = server.copy(pingMs = 50)
        assertEquals(0xFF10B981, lowPing.getPingColor())

        // Kuning (80 - 150 ms)
        val midPing = server.copy(pingMs = 110)
        assertEquals(0xFFF59E0B, midPing.getPingColor())

        // Merah (> 150 ms)
        val highPing = server.copy(pingMs = 175)
        assertEquals(0xFFEF4444, highPing.getPingColor())
    }

    @Test
    fun testMtu_isLowLatency1360() {
        assertEquals(1360, GameVpnService.GAMING_OPTIMIZED_MTU)
    }

    @Test
    fun testVpnSessionState_defaultAntiLagValues() {
        val state = VpnSessionState()
        assertEquals(ConnectionStatus.DISCONNECTED, state.status)
        assertTrue(state.isSplitTunnelActive)
        assertEquals(0, state.autoDisconnectSeconds)
        assertTrue(state.isFloatingOverlayEnabled)
        assertEquals(false, state.isAntiFreezeActive)
    }

    @Test
    fun testOpenVpnProfiles_cambodiaAndIndiaExist() {
        val cambodiaProfile = OpenVpnProfiles.PROFILES["Kamboja"]
        assertNotNull("Profil OpenVPN Kamboja harus tersedia", cambodiaProfile)
        assertTrue(cambodiaProfile!!.rawOvpnContent.contains("client"))
        assertTrue(cambodiaProfile.rawOvpnContent.contains("dev tun"))

        val indiaProfile = OpenVpnProfiles.PROFILES["India"]
        assertNotNull("Profil OpenVPN India harus tersedia", indiaProfile)
        assertEquals(1194, indiaProfile!!.remotePort)
    }

    @Test
    fun testOpenVpnProfiles_parser() {
        val sampleOvpn = """
            client
            dev tun
            proto udp
            remote my-vpn.gaming.com 443
            cipher AES-256-GCM
        """.trimIndent()

        val parsed = OpenVpnProfiles.parseOvpn(sampleOvpn, "TestServer")
        assertEquals("my-vpn.gaming.com", parsed.remoteHost)
        assertEquals(443, parsed.remotePort)
        assertEquals("udp", parsed.protocol)
    }

    @Test
    fun testIpPacket_buildUdpReply() {
        val src = InetAddress.getByName("1.1.1.1")
        val dst = InetAddress.getByName("10.8.0.2")
        val payload = "DNS_PONG".toByteArray()

        val replyBytes = IpPacket.buildUdpReplyPacket(
            srcIp = src,
            dstIp = dst,
            srcPort = 53,
            dstPort = 45678,
            payload = payload
        )

        assertTrue(replyBytes.size >= 28 + payload.size)
        val parsed = IpPacket.parse(replyBytes, replyBytes.size)
        assertNotNull(parsed)
        assertEquals(IpPacket.PROTOCOL_UDP, parsed!!.protocol)
        assertEquals(53, parsed.sourcePort)
        assertEquals(45678, parsed.destPort)
    }
}
