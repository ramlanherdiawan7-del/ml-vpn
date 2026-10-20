package com.example

import com.example.data.ServerPresets
import com.example.model.ConnectionStatus
import com.example.model.VpnSessionState
import com.example.vpn.GameVpnService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    }
}
