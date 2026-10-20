package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DeepBorder
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.DeepSurfaceVariant
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun CustomServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (country: String, code: String, emoji: String, city: String, host: String, port: Int, note: String) -> Unit
) {
    var countryName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var flagEmoji by remember { mutableStateOf("🌐") }
    var city by remember { mutableStateOf("") }
    var hostOrIp by remember { mutableStateOf("") }
    var portStr by remember { mutableStateOf("443") }
    var note by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

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
                Text(
                    text = "Tambah Server Kustom",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Masukkan IP Gateway VPS / Node Proxy VPN Anda",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = flagEmoji,
                        onValueChange = { flagEmoji = it },
                        label = { Text("Emoji", color = TextMuted) },
                        modifier = Modifier.width(85.dp),
                        colors = customFieldColors()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = countryName,
                        onValueChange = {
                            countryName = it
                            hasError = false
                        },
                        label = { Text("Nama Negara", color = TextMuted) },
                        isError = hasError && countryName.isBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_country_input"),
                        colors = customFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it },
                        label = { Text("Kode (ID/KH/TR)", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Kota / Region", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = hostOrIp,
                        onValueChange = {
                            hostOrIp = it
                            hasError = false
                        },
                        label = { Text("Host / IP Gateway", color = TextMuted) },
                        placeholder = { Text("103.45.xx.xx / domain.com", color = TextMuted) },
                        isError = hasError && hostOrIp.isBlank(),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("custom_host_input"),
                        colors = customFieldColors()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Matchmaking (Opsional)", color = TextMuted) },
                    placeholder = { Text("Lobi bot jam malam / matchmaking cepat", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customFieldColors()
                )

                if (hasError) {
                    Text(
                        text = "Nama negara dan IP gateway wajib diisi!",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (countryName.isBlank() || hostOrIp.isBlank()) {
                                hasError = true
                            } else {
                                val port = portStr.toIntOrNull() ?: 443
                                onAddServer(
                                    countryName.trim(),
                                    countryCode.trim().ifBlank { "XX" },
                                    flagEmoji.trim().ifBlank { "🌐" },
                                    city.trim().ifBlank { "Custom Server" },
                                    hostOrIp.trim(),
                                    port,
                                    note.trim()
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.testTag("submit_custom_server_button")
                    ) {
                        Text("Simpan Server", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DeepSurfaceVariant,
    unfocusedContainerColor = DeepSurfaceVariant,
    focusedBorderColor = NeonPurpleGlow,
    unfocusedBorderColor = DeepBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
