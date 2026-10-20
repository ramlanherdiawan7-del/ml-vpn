package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ServerPresets
import com.example.model.ConnectionStatus
import com.example.model.GameServer
import com.example.model.VpnSessionState
import com.example.util.PingTester
import com.example.vpn.VpnManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _servers = MutableStateFlow<List<GameServer>>(ServerPresets.DEFAULT_SERVERS)
    val servers: StateFlow<List<GameServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<GameServer>(ServerPresets.DEFAULT_SERVERS.first())
    val selectedServer: StateFlow<GameServer> = _selectedServer.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    private val _isSplitTunnelEnabled = MutableStateFlow(true)
    val isSplitTunnelEnabled: StateFlow<Boolean> = _isSplitTunnelEnabled.asStateFlow()

    private val _autoDisconnectSeconds = MutableStateFlow(0) // 0 = nonaktif, 60, 90, 120 detik
    val autoDisconnectSeconds: StateFlow<Int> = _autoDisconnectSeconds.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(true)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isMlbbInstalled = MutableStateFlow(false)
    val isMlbbInstalled: StateFlow<Boolean> = _isMlbbInstalled.asStateFlow()

    val sessionState: StateFlow<VpnSessionState> = VpnManager.sessionState

    // Filtered servers berdasarkan kategori
    val filteredServers: StateFlow<List<GameServer>> = combine(_servers, _selectedCategory) { list, category ->
        if (category == "Semua") list else list.filter { it.categoryTag == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerPresets.DEFAULT_SERVERS)

    private var pingJob: Job? = null

    init {
        pingAllServers()
    }

    fun checkDeviceApps(context: Context) {
        val installed = VpnManager.isMlbbInstalled(context)
        _isMlbbInstalled.value = installed
    }

    fun selectServer(server: GameServer) {
        _selectedServer.value = server
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleSplitTunnel(enabled: Boolean) {
        _isSplitTunnelEnabled.value = enabled
    }

    fun setAutoDisconnectSeconds(seconds: Int) {
        _autoDisconnectSeconds.value = seconds
        VpnManager.updateAutoDisconnectSetting(seconds)
    }

    fun toggleFloatingOverlay(enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        VpnManager.updateFloatingOverlayToggle(enabled)
    }

    /**
     * Implementasi pingAllServers() menggunakan Coroutines untuk mengukur latensi real-time (ms) ke semua server
     */
    fun pingAllServers() {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            _isTestingPing.value = true
            val currentList = _servers.value

            // Jalankan ping secara paralel dengan async/awaitAll untuk hasil cepat
            val deferredList = currentList.map { server ->
                async {
                    val ping = PingTester.measureLatency(
                        host = server.hostIp,
                        port = server.port,
                        baselineMs = server.baselineMs
                    )
                    server.copy(pingMs = ping)
                }
            }

            val updatedList = deferredList.awaitAll()
            _servers.value = updatedList

            // Sinkronkan server yang sedang terpilih
            val currentSelected = _selectedServer.value
            updatedList.find { it.id == currentSelected.id }?.let {
                _selectedServer.value = it
            }

            _isTestingPing.value = false
        }
    }

    /**
     * Fitur "Auto-Select Best Ping": Otomatis memilih server dengan ping terendah di kategori terpilih
     */
    fun autoSelectBestPing() {
        val candidateServers = filteredServers.value.ifEmpty { _servers.value }
        val bestServer = candidateServers.minByOrNull { it.pingMs ?: it.baselineMs }
        if (bestServer != null) {
            _selectedServer.value = bestServer
        }
    }

    fun toggleConnection(context: Context, onRequireVpnPermission: (Intent) -> Unit) {
        val state = sessionState.value.status
        if (state == ConnectionStatus.CONNECTED || state == ConnectionStatus.CONNECTING) {
            VpnManager.stopVpn(context)
        } else {
            val vpnPermissionIntent = VpnManager.prepareVpn(context)
            if (vpnPermissionIntent != null) {
                onRequireVpnPermission(vpnPermissionIntent)
            } else {
                startVpnNow(context)
            }
        }
    }

    fun startVpnNow(context: Context) {
        val server = _selectedServer.value
        val splitTunnel = _isSplitTunnelEnabled.value
        val autoDisconnectSec = _autoDisconnectSeconds.value
        VpnManager.startVpn(context, server, splitTunnel, autoDisconnectSec)
    }

    fun connectAndLaunchMlbb(context: Context, onRequireVpnPermission: (Intent) -> Unit) {
        val state = sessionState.value.status
        if (state != ConnectionStatus.CONNECTED) {
            val vpnPermissionIntent = VpnManager.prepareVpn(context)
            if (vpnPermissionIntent != null) {
                onRequireVpnPermission(vpnPermissionIntent)
                return
            } else {
                startVpnNow(context)
            }
        }

        viewModelScope.launch {
            delay(1000)
            VpnManager.launchMobileLegends(context)
        }
    }

    fun addCustomServer(
        country: String,
        code: String,
        emoji: String,
        city: String,
        ipOrHost: String,
        port: Int,
        note: String
    ) {
        val newServer = GameServer(
            id = "custom_${System.currentTimeMillis()}",
            country = country,
            city = city.ifBlank { "Custom Server" },
            flagEmoji = emoji.ifBlank { "🌐" },
            hostIp = ipOrHost,
            port = port,
            categoryTag = "Low Ping",
            displayTag = "Custom Server",
            matchmakingNote = note.ifBlank { "Server kustom yang dimasukkan pengguna." },
            baselineMs = 60,
            isCustom = true
        )

        _servers.update { listOf(newServer) + it }
        _selectedServer.value = newServer
        pingAllServers()
    }
}
