package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DeepBorder
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.DeepSurfaceCard
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConfigGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, DeepBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = NeonPurpleGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Panduan & Konfigurasi VPN",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Cara Kerja VpnService & Split Tunneling
                GuideSection(
                    icon = Icons.Default.Shield,
                    iconTint = ElectricEmerald,
                    title = "1. Cara Kerja VpnService (Split Tunneling)",
                    description = "Aplikasi ini mengaktifkan interface virtual TUN lokal di Android. Fitur Split Tunneling menggunakan perintah builder.addAllowedApplication(\"com.mobile.legends\") sehingga HANYA paket game Mobile Legends yang dialihkan ke IP server target. Aplikasi lain (WhatsApp, YouTube, Spotify) tetap memakai jaringan ISP lokal tanpa terganggu."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Memasukkan IP / Server Asli
                GuideSection(
                    icon = Icons.Default.Code,
                    iconTint = NeonCyan,
                    title = "2. Memasukkan IP Server Pribadi",
                    description = "Tekan tombol '+ Kustom Server' di bawah untuk memasukkan IP VPS/Proxy Anda. Format yang didukung: IPv4 (contoh: 103.28.12.55) atau FQDN (contoh: vpn-kh.yourdomain.com) dengan port 443, 1194, atau 51820."
                )

                // Code snippet view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0713))
                        .border(1.dp, Color(0xFF221A38), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "// GameVpnService.kt snippet:\nval builder = Builder()\nbuilder.addAddress(\"10.8.0.2\", 24)\nbuilder.addAllowedApplication(\"com.mobile.legends\")\nbuilder.addRoute(\"0.0.0.0\", 0)\nval tunFd = builder.establish()",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Integrasi WireGuard / OpenVPN Core
                GuideSection(
                    icon = Icons.Default.Security,
                    iconTint = NeonPurpleGlow,
                    title = "3. Integrasi WireGuard / OpenVPN SDK",
                    description = "Untuk produksi penuh dengan enkripsi ChaCha20/AES-GCM, Anda dapat menghubungkan library com.wireguard.android:tunnel atau ICS-OpenVPN core di dalam GameVpnService.kt dengan menaruh berkas konfigurasi .conf / .ovpn."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Tips Aman Anti Banned
                GuideSection(
                    icon = Icons.Default.Shield,
                    iconTint = ElectricEmerald,
                    title = "4. Keamanan Akun Moonton",
                    description = "VPN ini tidak memodifikasi data game atau file obb (.apk). Ini hanya mengarahkan IP gateway saat matchmaking mencari lobi negara tertentu. Selalu gunakan ping <120ms agar performa match tetap mulus."
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saya Mengerti",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
