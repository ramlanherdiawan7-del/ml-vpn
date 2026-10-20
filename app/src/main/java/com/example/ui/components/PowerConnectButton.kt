package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun PowerConnectButton(
    status: ConnectionStatus,
    activeServerCountry: String?,
    durationText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == ConnectionStatus.CONNECTED) 1.08f else if (status == ConnectionStatus.CONNECTING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowColor = when (status) {
        ConnectionStatus.CONNECTED -> ElectricEmerald
        ConnectionStatus.CONNECTING -> NeonPurpleGlow
        ConnectionStatus.DISCONNECTING -> CrimsonAlert
        ConnectionStatus.ERROR -> CrimsonAlert
        ConnectionStatus.DISCONNECTED -> NeonPurple
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(190.dp)
    ) {
        // Outer pulsing glow ring
        if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING) {
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.18f))
                    .border(1.5.dp, glowColor.copy(alpha = 0.4f), CircleShape)
            )
        }

        // Second outer ring
        Box(
            modifier = Modifier
                .size(155.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.25f),
                            DeepSurface.copy(alpha = 0.9f)
                        )
                    )
                )
                .border(2.dp, glowColor.copy(alpha = 0.65f), CircleShape)
        )

        // Main clickable core button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = glowColor, spotColor = glowColor)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF261D45),
                            Color(0xFF130E24)
                        )
                    )
                )
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(glowColor, glowColor.copy(alpha = 0.4f))
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 65.dp),
                    onClick = onClick
                )
                .testTag("power_connect_button")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (status == ConnectionStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = NeonPurpleGlow,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (status == ConnectionStatus.CONNECTED) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = "Power Connect",
                        tint = glowColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (status) {
                        ConnectionStatus.CONNECTED -> "CONNECTED"
                        ConnectionStatus.CONNECTING -> "CONNECTING"
                        ConnectionStatus.DISCONNECTING -> "STOPPING"
                        ConnectionStatus.ERROR -> "RETRY"
                        ConnectionStatus.DISCONNECTED -> "TAP TO CONNECT"
                    },
                    color = if (status == ConnectionStatus.CONNECTED) ElectricEmerald else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                if (status == ConnectionStatus.CONNECTED) {
                    Text(
                        text = durationText,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
