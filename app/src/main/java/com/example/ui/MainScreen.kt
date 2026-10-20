package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.components.ConfigGuideDialog
import com.example.ui.components.CustomServerDialog
import com.example.ui.components.PowerConnectButton
import com.example.ui.components.ServerCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepBackground
import com.example.ui.theme.DeepBorder
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.DeepSurfaceCard
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleDark
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.MainViewModel
import com.example.vpn.VpnManager
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val servers by viewModel.filteredServers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val isTestingPing by viewModel.isTestingPing.collectAsState()
    val isSplitTunnelEnabled by viewModel.isSplitTunnelEnabled.collectAsState()
    val autoDisconnectSec by viewModel.autoDisconnectSeconds.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
    val isMlbbInstalled by viewModel.isMlbbInstalled.collectAsState()

    var showConfigGuide by remember { mutableStateOf(false) }
    var showCustomServerDialog by remember { mutableStateOf(false) }

    // Launcher untuk izin SYSTEM_ALERT_WINDOW (Floating Widget)
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.toggleFloatingOverlay(Settings.canDrawOverlays(context))
    }

    LaunchedEffect(Unit) {
        viewModel.checkDeviceApps(context)
    }

    val rotationTransition = rememberInfiniteTransition(label = "refresh_rotate")
    val refreshAngle by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        ),
        label = "refreshAngle"
    )

    val isConnected = sessionState.status == ConnectionStatus.CONNECTED
    val formatDuration = remember(sessionState.durationSeconds) {
        val min = sessionState.durationSeconds / 60
        val sec = sessionState.durationSeconds % 60
        String.format("%02d:%02d", min, sec)
    }

    val categories = listOf("Semua", "Low Ping", "Easy Match", "Bot Lobbies")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepBackground,
                        Color(0xFF0F0B1E),
                        DeepBackground
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f))
                                    .border(1.dp, NeonPurpleGlow.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = NeonPurpleGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ML SERVER SWITCHER",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Anti-Lag UDP • MTU 1360 • MLBB Split Tunnel",
                            color = NeonCyanGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 44.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Refresh Pings Button
                        IconButton(
                            onClick = { viewModel.pingAllServers() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DeepSurfaceCard)
                                .border(1.dp, DeepBorder, CircleShape)
                                .testTag("refresh_pings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ping All Servers",
                                tint = if (isTestingPing) NeonCyan else TextPrimary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .then(if (isTestingPing) Modifier.rotate(refreshAngle) else Modifier)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Config Guide Button
                        IconButton(
                            onClick = { showConfigGuide = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DeepSurfaceCard)
                                .border(1.dp, DeepBorder, CircleShape)
                                .testTag("config_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Panduan Setup VPN",
                                tint = NeonPurpleGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Power Connect Hero Section
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    PowerConnectButton(
                        status = sessionState.status,
                        activeServerCountry = sessionState.activeServer?.country,
                        durationText = formatDuration,
                        onClick = {
                            viewModel.toggleConnection(context, onRequestVpnPermission)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected server status label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isConnected) "TERHUBUNG: " else "SERVER: ",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedServer.flagEmoji} ${selectedServer.country} (${selectedServer.city})",
                            color = if (isConnected) ElectricEmerald else NeonPurpleGlow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Auto-Disconnect Countdown Alert
                    if (isConnected && sessionState.autoDisconnectRemainingSeconds != null) {
                        val rem = sessionState.autoDisconnectRemainingSeconds ?: 0
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⏱ Auto-Disconnect dalam: ${rem}s (Matchmaking Fase Selesai)",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (sessionState.errorMessage != null) {
                        Text(
                            text = sessionState.errorMessage ?: "",
                            color = CrimsonAlert,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Real-Time Telemetry Bar (Visible when connected)
            item {
                AnimatedVisibility(visible = isConnected) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepSurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelemetryItem(
                                icon = Icons.Default.Speed,
                                iconColor = ElectricEmerald,
                                label = "PING",
                                value = "${selectedServer.pingMs ?: selectedServer.baselineMs} ms"
                            )
                            TelemetryItem(
                                icon = Icons.Default.ArrowDownward,
                                iconColor = NeonCyan,
                                label = "DOWN",
                                value = String.format("%.1f KB/s", sessionState.currentDownloadSpeedKbps)
                            )
                            TelemetryItem(
                                icon = Icons.Default.ArrowUpward,
                                iconColor = NeonPurpleGlow,
                                label = "UP",
                                value = String.format("%.1f KB/s", sessionState.currentUploadSpeedKbps)
                            )
                            TelemetryItem(
                                icon = Icons.Default.Timer,
                                iconColor = WarningAmber,
                                label = "DURASI",
                                value = formatDuration
                            )
                        }
                    }
                }
            }

            // Quick One-Tap "Connect & Launch MLBB" Hero Button
            item {
                Button(
                    onClick = {
                        viewModel.connectAndLaunchMlbb(context, onRequestVpnPermission)
                        if (!isMlbbInstalled) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Mengarahkan ke Google Play untuk mengunduh Mobile Legends...")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurpleDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(NeonPurpleGlow, NeonCyan)),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("connect_and_launch_mlbb_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "BUKA MOBILE LEGENDS SEKARANG" else "CONNECT & LAUNCH MLBB",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Fitur 3: Floating Overlay Widget Setting Card (SYSTEM_ALERT_WINDOW)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeepBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = NeonPurpleGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Floating Overlay In-Game",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tombol melayang di atas MLBB untuk 1-klik disconnect begitu match matchmaking dimulai.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = isOverlayEnabled,
                            onCheckedChange = { enable ->
                                if (enable && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                } else {
                                    viewModel.toggleFloatingOverlay(enable)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DeepSurface
                            ),
                            modifier = Modifier.testTag("floating_overlay_switch")
                        )
                    }
                }
            }

            // Fitur 4: Auto-Disconnect Timer Card (60s / 90s / 120s)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeepBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Disconnect Timer",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Otomatis matikan VPN setelah timer habis agar ping game kembali super stabil dengan ISP rumah.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val timerOptions = listOf(
                                0 to "Nonaktif",
                                60 to "60 Detik",
                                90 to "90 Detik",
                                120 to "120 Detik"
                            )

                            timerOptions.forEach { (seconds, label) ->
                                val isSelected = autoDisconnectSec == seconds
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) WarningAmber.copy(alpha = 0.2f) else Color(0xFF1E1736))
                                        .border(
                                            1.dp,
                                            if (isSelected) WarningAmber else DeepBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setAutoDisconnectSeconds(seconds) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) WarningAmber else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fitur 1 & 2: Split Tunneling & MTU 1360 Info
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeepBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Split Tunneling (com.mobile.legends)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "MTU 1360 UDP anti-fragmentasi. Hanya paket MLBB yang dialihkan ke server target.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = isSplitTunnelEnabled,
                            onCheckedChange = { viewModel.toggleSplitTunnel(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DeepSurface
                            ),
                            modifier = Modifier.testTag("split_tunnel_switch")
                        )
                    }
                }
            }

            // Fitur 5: Kategori Server Filter Tabs & Auto-Select Best Ping
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRESET SERVER MLBB",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        // Fitur Auto-Select Best Ping Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricEmerald.copy(alpha = 0.18f))
                                .border(1.dp, ElectricEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.autoSelectBestPing() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("auto_select_best_ping_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Auto-Select",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Auto Best Ping",
                                color = ElectricEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            val isCatSelected = selectedCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isCatSelected) NeonPurple else Color(0xFF1E1736))
                                    .border(
                                        1.dp,
                                        if (isCatSelected) NeonPurpleGlow else DeepBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.selectCategory(category) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = if (isCatSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Add Custom Server pill
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF261D45))
                                    .border(1.dp, NeonPurpleGlow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .clickable { showCustomServerDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("add_custom_server_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = NeonPurpleGlow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Kustom",
                                        color = NeonPurpleGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // List of Filtered Game Servers
            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    isSelected = selectedServer.id == server.id,
                    isConnected = isConnected && sessionState.activeServer?.id == server.id,
                    onSelect = {
                        viewModel.selectServer(server)
                    }
                )
            }

            // Bottom Spacing for clean scrolling
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showConfigGuide) {
        ConfigGuideDialog(onDismiss = { showConfigGuide = false })
    }

    if (showCustomServerDialog) {
        CustomServerDialog(
            onDismiss = { showCustomServerDialog = false },
            onAddServer = { country, code, emoji, city, host, port, note ->
                viewModel.addCustomServer(country, code, emoji, city, host, port, note)
            }
        )
    }
}

@Composable
private fun TelemetryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
