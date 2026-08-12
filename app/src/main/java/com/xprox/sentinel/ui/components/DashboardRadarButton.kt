package com.xprox.sentinel.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun DashboardRadarButton(
    isRunning: Boolean,
    pingMs: Int? = null,
    publicIp: String? = null,
    speedText: String = "",
    hasProfile: Boolean = false,
    onPingClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createOneShot(12L, 255)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val attrs = android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_ALARM)
                        vibrator.vibrate(effect, attrs)
                    } else {
                        vibrator.vibrate(effect)
                    }
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (e: Throwable) {
            // Ignore hardware vibration exception
        }
    }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(230.dp)
        ) {
            // Outer Bezel Ring
            Surface(
                modifier = Modifier
                    .size(184.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        if (isRunning) {
                            listOf(SecureGreen, CyberCyan, SecureGreen)
                        } else {
                            listOf(ElectricViolet, CyberCyan, ElectricViolet)
                        }
                    )
                )
            ) {}

            // Inner Tactical Dashed Compass Ring
            Canvas(
                modifier = Modifier
                    .size(156.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                drawCircle(
                    color = if (isRunning) SecureGreen.copy(alpha = 0.35f) else ElectricViolet.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), 0f)
                    )
                )
            }

            // Central Interactive Core Node
            Surface(
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            triggerHapticFeedback()
                            onClick()
                        }
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    1.5.dp,
                    if (isRunning) SecureGreen.copy(alpha = 0.85f) else CardBorder
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isRunning) {
                                Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF0F766E)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF12121E), Color(0xFF0C0C14)))
                            }
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protection Lock Core",
                            tint = if (isRunning) TextWhite else ElectricViolet,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRunning) string("shield_active") else string("disconnected"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) TextWhite else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-End Doppelrand Glass Telemetry Surface Pill
        DoppelrandCard(
            shellPadding = 3.dp,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            borderColor = if (isRunning) SecureGreen.copy(alpha = 0.4f) else DoppelrandShellBorder,
            onClick = if (hasProfile && onPingClick != null) {
                {
                    triggerHapticFeedback()
                    onPingClick()
                }
            } else null
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Public IP Address Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(color = if (publicIp != null) SecureGreen else WarningRose)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (publicIp != null) "${string("public_ip")}: $publicIp" else string("checking_ip"),
                        fontSize = 13.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Ping & Speed Telemetry Line
                val pingText = if (!hasProfile) {
                    string("ping_na")
                } else if (pingMs != null) {
                    "${string("ping")}: ${pingMs}ms"
                } else {
                    string("checking_ping")
                }

                val telemetryLine = if (isRunning && speedText.isNotEmpty()) {
                    "$pingText  •  $speedText"
                } else {
                    pingText
                }

                val telemetryColor = if (pingMs != null) {
                    if (pingMs < 150) SecureGreen else if (pingMs < 300) CyberCyan else WarningRose
                } else {
                    TextGray
                }

                Text(
                    text = telemetryLine,
                    fontSize = 10.5.sp,
                    color = telemetryColor,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
