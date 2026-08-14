package com.xprox.sentinel.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import kotlin.math.cos
import kotlin.math.sin

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
    val sysClipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    fun triggerHapticFeedback(durationMs: Long = 16L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createOneShot(durationMs, 255)
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

    // Continuous Tactical Radar Sweep Animation (360 degrees)
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRotation"
    )

    // Sonar Wave Ripple 1
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarWave1"
    )

    // Sonar Wave Ripple 2 (Offset phase)
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarWave2"
    )

    // Ambient Breathing Glow Pulse
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 450f),
        label = "scale"
    )

    // Refresh button spin animation state
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshRotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        finishedListener = { isRefreshing = false },
        label = "refreshRotation"
    )

    // Smooth Disconnect & Connect Color and Glow transitions
    val animatedPrimaryColor by animateColorAsState(
        targetValue = if (isRunning) SecureGreen else ElectricViolet,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "animatedPrimaryColor"
    )

    val animatedAccentColor by animateColorAsState(
        targetValue = if (isRunning) CyberCyan else ElectricViolet,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "animatedAccentColor"
    )

    val runningGlowAlpha by animateFloatAsState(
        targetValue = if (isRunning) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "runningGlowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ====================================================================
        // 1. TACTICAL CYBER RADAR BUTTON CORE
        // ====================================================================
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Ambient Outer Glow Halo (Smoothly fades on disconnect)
            if (runningGlowAlpha > 0.01f) {
                Surface(
                    modifier = Modifier
                        .size(230.dp)
                        .graphicsLayer {
                            scaleX = pulseGlow
                            scaleY = pulseGlow
                            alpha = 0.18f * pulseGlow * runningGlowAlpha
                        },
                    shape = CircleShape,
                    color = SecureGreen
                ) {}
            }

            // Authentic High-Tech Radar Canvas Scope (Sector Sweep, Sonar Waves & Server Blips)
            Canvas(
                modifier = Modifier
                    .size(236.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f - 2.dp.toPx()
                val coreRadius = 68.dp.toPx()

                val primaryColor = animatedPrimaryColor
                val accentColor = animatedAccentColor

                // 1. Expanding Sonar Wave Ripples (Smoothly fade on disconnect)
                if (runningGlowAlpha > 0.01f) {
                    val r1 = coreRadius + (maxRadius - coreRadius) * sonarWave1
                    val a1 = (1f - sonarWave1) * 0.35f * runningGlowAlpha
                    drawCircle(
                        color = SecureGreen.copy(alpha = a1),
                        radius = r1,
                        center = centerOffset,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val r2 = coreRadius + (maxRadius - coreRadius) * sonarWave2
                    val a2 = (1f - sonarWave2) * 0.35f * runningGlowAlpha
                    drawCircle(
                        color = CyberCyan.copy(alpha = a2),
                        radius = r2,
                        center = centerOffset,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 2. Rotating Phosphor Radar Sector Sweep Cone & Leading Laser Line (Smoothly fade on disconnect)
                if (runningGlowAlpha > 0.01f) {
                    drawContext.canvas.save()
                    drawContext.transform.rotate(sweepRotation, centerOffset)

                    // Trailing 60-degree Sector Glow Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            0.833f to SecureGreen.copy(alpha = 0.04f * runningGlowAlpha),
                            0.94f to SecureGreen.copy(alpha = 0.20f * runningGlowAlpha),
                            1f to SecureGreen.copy(alpha = 0.45f * runningGlowAlpha)
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2),
                        topLeft = Offset(centerOffset.x - maxRadius, centerOffset.y - maxRadius)
                    )

                    // Leading Laser Sweep Line
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, SecureGreen.copy(alpha = runningGlowAlpha), CyberCyan.copy(alpha = runningGlowAlpha)),
                            start = centerOffset,
                            end = Offset(centerOffset.x + maxRadius, centerOffset.y)
                        ),
                        start = centerOffset,
                        end = Offset(centerOffset.x + maxRadius, centerOffset.y),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawContext.canvas.restore()
                }

                // 3. Concentric Range Grid Rings
                // Outer Tactical Rim
                drawCircle(
                    color = primaryColor.copy(alpha = if (isRunning) 0.6f else 0.35f),
                    radius = maxRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Mid Range Ring (Dashed)
                drawCircle(
                    color = primaryColor.copy(alpha = if (isRunning) 0.3f else 0.18f),
                    radius = coreRadius + (maxRadius - coreRadius) * 0.62f,
                    center = centerOffset,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    )
                )

                // Inner Boundary Ring (Dotted)
                drawCircle(
                    color = primaryColor.copy(alpha = if (isRunning) 0.25f else 0.15f),
                    radius = coreRadius + (maxRadius - coreRadius) * 0.30f,
                    center = centerOffset,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f)
                    )
                )

                // 4. Tactical Degree Tick Marks around outer bezel (36 ticks = every 10°)
                for (i in 0 until 36) {
                    val angleDeg = i * 10.0
                    val angleRad = Math.toRadians(angleDeg)
                    val isCardinal = i % 9 == 0
                    val isMajor = i % 3 == 0
                    val tickLen = when {
                        isCardinal -> 9.dp.toPx()
                        isMajor -> 6.dp.toPx()
                        else -> 3.dp.toPx()
                    }

                    val startX = (centerOffset.x + (maxRadius - tickLen) * cos(angleRad)).toFloat()
                    val startY = (centerOffset.y + (maxRadius - tickLen) * sin(angleRad)).toFloat()
                    val endX = (centerOffset.x + maxRadius * cos(angleRad)).toFloat()
                    val endY = (centerOffset.y + maxRadius * sin(angleRad)).toFloat()

                    val tickAlpha = when {
                        isCardinal -> if (isRunning) 0.9f else 0.6f
                        isMajor -> if (isRunning) 0.6f else 0.4f
                        else -> if (isRunning) 0.3f else 0.18f
                    }

                    drawLine(
                        color = if (isCardinal) accentColor.copy(alpha = tickAlpha) else primaryColor.copy(alpha = tickAlpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 5. Crosshair Reticles (0°, 90°, 180°, 270°)
                val crosshairAngles = listOf(0.0, 90.0, 180.0, 270.0)
                for (angleDeg in crosshairAngles) {
                    val angleRad = Math.toRadians(angleDeg)
                    val startX = (centerOffset.x + (coreRadius + 4.dp.toPx()) * cos(angleRad)).toFloat()
                    val startY = (centerOffset.y + (coreRadius + 4.dp.toPx()) * sin(angleRad)).toFloat()
                    val endX = (centerOffset.x + (maxRadius - 11.dp.toPx()) * cos(angleRad)).toFloat()
                    val endY = (centerOffset.y + (maxRadius - 11.dp.toPx()) * sin(angleRad)).toFloat()

                    drawLine(
                        color = primaryColor.copy(alpha = if (isRunning) 0.25f else 0.12f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    )
                }

                // 6. Detected Server Blips (Interactive Sonar Targets)
                val blipTargets = listOf(
                    Pair(35f, 0.78f),
                    Pair(125f, 0.86f),
                    Pair(215f, 0.65f),
                    Pair(305f, 0.80f)
                )

                for ((targetAngle, distFraction) in blipTargets) {
                    val targetRad = Math.toRadians(targetAngle.toDouble())
                    val blipRadius = coreRadius + (maxRadius - coreRadius) * distFraction
                    val blipX = (centerOffset.x + blipRadius * cos(targetRad)).toFloat()
                    val blipY = (centerOffset.y + blipRadius * sin(targetRad)).toFloat()

                    if (isRunning) {
                        val deltaAngle = ((sweepRotation - targetAngle + 360f) % 360f)
                        if (deltaAngle < 100f) {
                            val intensity = (1f - deltaAngle / 100f)
                            // Outer Ping Echo Ring
                            drawCircle(
                                color = CyberCyan.copy(alpha = intensity * 0.6f),
                                radius = (3f + intensity * 6f).dp.toPx(),
                                center = Offset(blipX, blipY),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            // Core Hotspot
                            drawCircle(
                                color = Color.White.copy(alpha = intensity),
                                radius = 2.2.dp.toPx(),
                                center = Offset(blipX, blipY)
                            )
                        } else {
                            // Dim Standby Target Dot
                            drawCircle(
                                color = SecureGreen.copy(alpha = 0.22f),
                                radius = 1.5.dp.toPx(),
                                center = Offset(blipX, blipY)
                            )
                        }
                    } else {
                        // Idle Stealth Target Dot
                        drawCircle(
                            color = ElectricViolet.copy(alpha = 0.25f),
                            radius = 1.5.dp.toPx(),
                            center = Offset(blipX, blipY)
                        )
                    }
                }
            }

            // Central Tactile Mascot Shield Node
            Surface(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            triggerHapticFeedback(24L)
                            onClick()
                        }
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    1.8.dp,
                    if (isRunning) SecureGreen.copy(alpha = 0.9f) else ElectricViolet.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isRunning) {
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF0D2818), Color(0xFF081810), Color(0xFF040A06))
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF19102E), Color(0xFF0F0A1C), Color(0xFF06040A))
                                )
                            }
                        )
                ) {
                    // Holographic Radial Ring behind Mascot
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = if (isRunning) {
                                    listOf(SecureGreen.copy(alpha = 0.35f), Color.Transparent)
                                } else {
                                    listOf(ElectricViolet.copy(alpha = 0.25f), Color.Transparent)
                                }
                            )
                        )
                    }

                    // Sentinel Dynamic Mascot Robot with Interactive Eye Blink Animation
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        SentinelRobotMascot(
                            isRunning = isRunning,
                            pulseGlow = pulseGlow,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = if (isRunning) 1.04f + 0.03f * pulseGlow else 0.98f
                                    scaleY = if (isRunning) 1.04f + 0.03f * pulseGlow else 0.98f
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Floating Tactical Status Pill under Radar
        Surface(
            color = (if (isRunning) SecureGreen else ElectricViolet).copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, (if (isRunning) SecureGreen else ElectricViolet).copy(alpha = 0.40f)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = if (isRunning) SecureGreen else ElectricViolet)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) string("shield_active") else string("disconnected"),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) SecureGreen else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ====================================================================
        // 2. MODULAR CYBER TELEMETRY HUD CARD
        // ====================================================================
        DoppelrandCard(
            shellPadding = 3.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            borderColor = if (isRunning) SecureGreen.copy(alpha = 0.45f) else DoppelrandShellBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Public IP Status with Copy & Refresh Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Pulsing Status Dot
                        Canvas(modifier = Modifier.size(7.dp)) {
                            drawCircle(
                                color = if (publicIp != null) SecureGreen else WarningAmber
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = string("public_ip"),
                            fontSize = 11.sp,
                            color = TextGray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = publicIp ?: string("checking_ip"),
                            fontSize = 13.sp,
                            color = if (publicIp != null) TextWhite else WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Action Icons: Copy IP & Fast Refresh
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (publicIp != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardElevated)
                                    .clickable {
                                        triggerHapticFeedback(12L)
                                        val clip = ClipData.newPlainText("Public IP", publicIp)
                                        sysClipboard?.setPrimaryClip(clip)
                                        Toast.makeText(context, LanguageManager.getString("ip_copied"), Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy IP",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        if (onPingClick != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardElevated)
                                    .clickable {
                                        triggerHapticFeedback(14L)
                                        isRefreshing = true
                                        onPingClick()
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh IP & Ping",
                                    tint = if (isRefreshing) SecureGreen else TextGray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .rotate(refreshRotation)
                                )
                            }
                        }
                    }
                }

                // Bottom Row: Telemetry Micro-Pill Chips (Ping Latency & Speed Rates)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Latency / Ping Chip
                    val pingColor = when {
                        pingMs == null -> TextGray
                        pingMs < 70 -> SecureGreen
                        pingMs < 150 -> CyberCyan
                        pingMs < 250 -> WarningAmber
                        else -> WarningRose
                    }

                    Surface(
                        color = pingColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, pingColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = hasProfile && onPingClick != null) {
                                triggerHapticFeedback(12L)
                                onPingClick?.invoke()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Ping",
                                tint = pingColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!hasProfile) {
                                    "${string("ping")}: ${string("ping_na")}"
                                } else if (pingMs != null) {
                                    "${string("ping")}: ${pingMs}ms"
                                } else {
                                    string("checking_ping")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pingColor,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Real-time Traffic Speed Rate Chip (when active)
                    if (isRunning && speedText.isNotEmpty()) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.30f)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = speedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
