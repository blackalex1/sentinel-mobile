package com.xprox.sentinel.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Official Android Sentinel Bugdroid Capsule Droid Mascot (x-prox).
 * Features:
 * - Iconic Rounded Bugdroid Capsule Head & Dome Silhouette
 * - 30° Rod Antennae with Spherical Glow Tips & Phased 5G/WiFi Radio Waves
 * - Curved Dark Visor Screen with Rectangular Cyber-Optic Eyes & Sleep Slits
 * - Solid Quantum Diamond Arc Reactor with Titanium Clamps & Singularity Heart
 * - Cellular 4-Bar Signal Strength Glyphs on Cheeks
 * - Luxury Dark Titanium Armor with Ambient Glow Halo
 * - Power-On Electric Shock Ignition & Sleep Transition Sequences
 */
@Composable
fun SentinelRobotMascot(
    isRunning: Boolean,
    pulseGlow: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // Official Android Sentinel Palette
    val androidGreen = Color(0xFF3DDC84)
    val accentCyan = Color(0xFF22D3EE)
    val standbyViolet = Color(0xFF8B5CF6)
    val standbyMuted = Color(0xFFA78BFA)
    val titanium = Color(0xFF1E293B)
    val titaniumLight = Color(0xFF334155)

    var currentEyeColor by remember { mutableStateOf(if (isRunning) accentCyan else standbyViolet) }
    var currentArmorColor by remember { mutableStateOf(if (isRunning) androidGreen else standbyViolet) }
    var isShocking by remember { mutableStateOf(false) }
    var isFallingAsleep by remember { mutableStateOf(false) }
    var previousRunningState by remember { mutableStateOf(isRunning) }

    // Shock on Connect & Falling Asleep on Disconnect
    LaunchedEffect(isRunning) {
        if (isRunning && !previousRunningState) {
            isFallingAsleep = false
            isShocking = true
            currentEyeColor = Color(0xFF00FFFF)
            currentArmorColor = Color(0xFF00FFFF)
            delay(120)

            currentEyeColor = Color(0xFFFF1E44)
            currentArmorColor = Color(0xFFFF1E44)
            delay(100)

            currentEyeColor = Color(0xFFFACC15)
            currentArmorColor = Color(0xFFFACC15)
            delay(160)

            currentEyeColor = accentCyan
            currentArmorColor = androidGreen
            delay(200)
            isShocking = false
        } else if (!isRunning && previousRunningState) {
            isShocking = false
            isFallingAsleep = true

            currentEyeColor = Color(0xFFF59E0B)
            currentArmorColor = Color(0xFFF59E0B)
            delay(200)

            currentEyeColor = standbyMuted
            currentArmorColor = standbyMuted
            delay(220)

            currentEyeColor = standbyViolet
            currentArmorColor = standbyViolet
            delay(180)
            isFallingAsleep = false
        } else if (!isRunning) {
            isShocking = false
            isFallingAsleep = false
            currentEyeColor = standbyViolet
            currentArmorColor = standbyViolet
        } else {
            isShocking = false
            isFallingAsleep = false
            currentEyeColor = accentCyan
            currentArmorColor = androidGreen
        }
        previousRunningState = isRunning
    }

    val eyeOpenness by animateFloatAsState(
        targetValue = if (isRunning) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "eyeOpenness"
    )

    val sleepProgress by animateFloatAsState(
        targetValue = if (!isRunning) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "sleepProgress"
    )

    val shockFadeAlpha by animateFloatAsState(
        targetValue = if (isShocking) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "shockFadeAlpha"
    )

    val animatedEyeColor by animateColorAsState(
        targetValue = currentEyeColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "animatedEyeColor"
    )

    val animatedArmorColor by animateColorAsState(
        targetValue = currentArmorColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "animatedArmorColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bugdroidMascotTransition")

    // Standby Sleep Floating & Active Breathing
    val breatheY by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 1800 else 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheY"
    )

    // Quantum Reactor Heartbeat & Singularity Pulse
    val reactorGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorGlow"
    )

    // Phased 5G/WiFi Radio Waves from Antenna Tips
    val radioWaveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radioWaveProgress"
    )

    // Jitter on Electric Shock
    val electricShakeXRaw by infiniteTransition.animateFloat(
        initialValue = -2.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "electricShakeXRaw"
    )
    val electricShakeYRaw by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 65, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "electricShakeYRaw"
    )
    val shakeX = electricShakeXRaw * shockFadeAlpha
    val shakeY = electricShakeYRaw * shockFadeAlpha

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) / 100f

        fun fx(x: Float): Float = (x + shakeX) * s + (w - 100f * s) / 2f
        fun fy(y: Float): Float = (y + shakeY + breatheY) * s + (h - 100f * s) / 2f

        val isAwake = 1.0f - sleepProgress

        // 1. Radial Ambient Halo
        val haloAlpha = (0.24f * isAwake + 0.08f * sleepProgress) * pulseGlow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedArmorColor.copy(alpha = haloAlpha),
                    accentCyan.copy(alpha = 0.08f * isAwake),
                    Color.Transparent
                ),
                center = Offset(fx(50f), fy(50f)),
                radius = 48f * s
            ),
            radius = 48f * s,
            center = Offset(fx(50f), fy(50f))
        )

        // 2. 30° Bugdroid Rod Antennae
        val antennaColor = animatedArmorColor
        val antennaStrokeWidth = 4.0f * s

        // Left Antenna Rod: (32, 21) -> (18, 5)
        drawLine(
            color = antennaColor,
            start = Offset(fx(32f), fy(21f)),
            end = Offset(fx(18f), fy(5f)),
            strokeWidth = antennaStrokeWidth,
            cap = StrokeCap.Round
        )

        // Right Antenna Rod: (68, 21) -> (82, 5)
        drawLine(
            color = antennaColor,
            start = Offset(fx(68f), fy(21f)),
            end = Offset(fx(82f), fy(5f)),
            strokeWidth = antennaStrokeWidth,
            cap = StrokeCap.Round
        )

        // Antenna Spherical Glow Tips
        drawCircle(
            color = if (isRunning) Color.White else Color(0xFFC4B5FD),
            radius = 3.2f * s,
            center = Offset(fx(18f), fy(5f))
        )
        drawCircle(
            color = if (isRunning) Color.White else Color(0xFFC4B5FD),
            radius = 3.2f * s,
            center = Offset(fx(82f), fy(5f))
        )

        // Radiating 5G/WiFi Radio Waves from Antenna Tips
        if (isAwake > 0.05f) {
            val waveAlpha = (0.35f * (1f - radioWaveProgress)) * isAwake
            val waveRadius = (3.0f + radioWaveProgress * 7.5f) * s
            drawCircle(
                color = animatedArmorColor.copy(alpha = waveAlpha),
                radius = waveRadius,
                center = Offset(fx(18f), fy(5f)),
                style = Stroke(width = 1.3f * s)
            )
            drawCircle(
                color = animatedArmorColor.copy(alpha = waveAlpha),
                radius = waveRadius,
                center = Offset(fx(82f), fy(5f)),
                style = Stroke(width = 1.3f * s)
            )
        }

        // 3. Iconic Bugdroid Capsule Chassis Silhouette & Armor
        val chassisPath = Path().apply {
            // Start top-left dome arc
            arcTo(
                rect = Rect(
                    left = fx(50f) - 34f * s,
                    top = fy(48f) - 34f * s,
                    right = fx(50f) + 34f * s,
                    bottom = fy(48f) + 34f * s
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            // Straight right side to lower jaw
            lineTo(fx(84f), fy(68f))
            // Rounded bottom transition to chin
            quadraticTo(fx(84f), fy(84f), fx(68f), fy(88f))
            lineTo(fx(50f), fy(92f))
            lineTo(fx(32f), fy(88f))
            quadraticTo(fx(16f), fy(84f), fx(16f), fy(68f))
            lineTo(fx(16f), fy(48f))
            close()
        }

        // Armor Base Gradient
        val armorGradient = Brush.verticalGradient(
            colors = if (isRunning) {
                listOf(Color(0xFF0C1F14), Color(0xFF07160D), Color(0xFF030A06))
            } else {
                listOf(Color(0xFF160D24), Color(0xFF0F0819), Color(0xFF06030A))
            },
            startY = fy(14f),
            endY = fy(92f)
        )
        drawPath(path = chassisPath, brush = armorGradient, style = Fill)

        // Signature Armor Contour Glow Border
        drawPath(
            path = chassisPath,
            color = animatedArmorColor,
            style = Stroke(width = 4.2f * s)
        )

        // Lateral Side Cowlings with Intake Slits
        val leftCowlingPath = Path().apply {
            moveTo(fx(16f), fy(48f))
            lineTo(fx(28f), fy(52f))
            lineTo(fx(28f), fy(76f))
            lineTo(fx(16f), fy(72f))
            close()
        }
        val rightCowlingPath = Path().apply {
            moveTo(fx(84f), fy(48f))
            lineTo(fx(72f), fy(52f))
            lineTo(fx(72f), fy(76f))
            lineTo(fx(84f), fy(72f))
            close()
        }
        val cowlingColor = animatedArmorColor.copy(alpha = 0.28f * isAwake + 0.12f * sleepProgress)
        drawPath(path = leftCowlingPath, color = cowlingColor, style = Fill)
        drawPath(path = rightCowlingPath, color = cowlingColor, style = Fill)

        // Intake Micro-Grids
        val gridColor = Color.White.copy(alpha = 0.20f)
        val gridStroke = Stroke(width = 0.8f * s)
        drawLine(gridColor, Offset(fx(19f), fy(56f)), Offset(fx(25f), fy(58f)), strokeWidth = 0.8f * s)
        drawLine(gridColor, Offset(fx(19f), fy(62f)), Offset(fx(25f), fy(64f)), strokeWidth = 0.8f * s)
        drawLine(gridColor, Offset(fx(81f), fy(56f)), Offset(fx(75f), fy(58f)), strokeWidth = 0.8f * s)
        drawLine(gridColor, Offset(fx(81f), fy(62f)), Offset(fx(75f), fy(64f)), strokeWidth = 0.8f * s)

        // 4. Cellular 4-Bar Signal Strength Glyphs on Cheeks
        for (b in 0 until 4) {
            val isActive = isRunning && isAwake > 0.3f
            val barAlpha = if (isActive) 0.95f else 0.18f
            val barColor = if (isActive) androidGreen else Color.White.copy(alpha = barAlpha)
            val barH = (1.6f + b * 0.9f) * s

            // Left Cheek
            drawRect(
                color = barColor,
                topLeft = Offset(fx(19f + b * 2.0f), fy(50f + (3 - b) * 1.8f)),
                size = Size(1.5f * s, barH)
            )
            // Right Cheek
            drawRect(
                color = barColor,
                topLeft = Offset(fx(79f - b * 2.0f), fy(50f + (3 - b) * 1.8f)),
                size = Size(1.5f * s, barH)
            )
        }

        // Forehead Panel Seam Arc & Titanium Rivets
        val foreheadPath = Path().apply {
            arcTo(
                rect = Rect(
                    left = fx(50f) - 26f * s,
                    top = fy(48f) - 26f * s,
                    right = fx(50f) + 26f * s,
                    bottom = fy(48f) + 26f * s
                ),
                startAngleDegrees = 205f,
                sweepAngleDegrees = 130f,
                forceMoveTo = true
            )
        }
        drawPath(path = foreheadPath, color = Color.White.copy(alpha = 0.16f), style = Stroke(width = 0.9f * s))

        fun drawRivet(rx: Float, ry: Float) {
            drawCircle(color = titaniumLight, radius = 1.0f * s, center = Offset(fx(rx), fy(ry)))
            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 1.0f * s, center = Offset(fx(rx), fy(ry)), style = Stroke(width = 0.6f * s))
        }
        drawRivet(28f, 28f)
        drawRivet(72f, 28f)

        // 5. Curved Dark Visor Screen Inset
        val visorPath = Path().apply {
            moveTo(fx(26f), fy(33f))
            lineTo(fx(74f), fy(33f))
            lineTo(fx(74f), fy(49f))
            lineTo(fx(50f), fy(54f))
            lineTo(fx(26f), fy(49f))
            close()
        }
        drawPath(path = visorPath, color = Color(0xE0020617), style = Fill)
        drawPath(path = visorPath, color = Color.White.copy(alpha = 0.12f), style = Stroke(width = 0.8f * s))

        // 6. Rectangular Cyber-Optic Eyes with Smooth Aperture
        val currentAperture = maxOf(0.08f, eyeOpenness * isAwake)
        if (currentAperture > 0.18f) {
            val eyeW = 12f * s
            val eyeH = 7.0f * s * currentAperture
            val eyeCorner = 1.5f * s

            // Left Cyber Eye
            drawRoundRect(
                color = animatedEyeColor,
                topLeft = Offset(fx(31f), fy(37.5f) - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeCorner, eyeCorner)
            )
            // Right Cyber Eye
            drawRoundRect(
                color = animatedEyeColor,
                topLeft = Offset(fx(57f), fy(37.5f) - eyeH / 2f),
                size = Size(eyeW, eyeH),
                cornerRadius = CornerRadius(eyeCorner, eyeCorner)
            )
        } else {
            // Sleep Standby Slits
            val slitColor = if (isRunning) accentCyan else standbyViolet
            val slitWidth = (2.2f * sleepProgress + 1.2f * isAwake) * s
            drawLine(
                color = slitColor,
                start = Offset(fx(30f), fy(37.5f)),
                end = Offset(fx(44f), fy(37.5f)),
                strokeWidth = slitWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = slitColor,
                start = Offset(fx(56f), fy(37.5f)),
                end = Offset(fx(70f), fy(37.5f)),
                strokeWidth = slitWidth,
                cap = StrokeCap.Round
            )
        }

        // 7. Quantum Diamond Arc Reactor
        // Titanium Clamps
        drawRect(
            color = titanium,
            topLeft = Offset(fx(46f), fy(48f)),
            size = Size(8f * s, 3.2f * s)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(fx(46f), fy(48f)),
            size = Size(8f * s, 3.2f * s),
            style = Stroke(width = 0.8f * s)
        )

        drawRect(
            color = titanium,
            topLeft = Offset(fx(46f), fy(84f)),
            size = Size(8f * s, 3.2f * s)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(fx(46f), fy(84f)),
            size = Size(8f * s, 3.2f * s),
            style = Stroke(width = 0.8f * s)
        )

        // Outer Diamond Shell
        val outerDiamondPath = Path().apply {
            moveTo(fx(50f), fy(51f))
            lineTo(fx(63f), fy(64f))
            lineTo(fx(50f), fy(84f))
            lineTo(fx(37f), fy(64f))
            close()
        }
        drawPath(
            path = outerDiamondPath,
            color = animatedArmorColor,
            style = Fill
        )

        // Inner Quantum Prism
        val innerPrismPath = Path().apply {
            moveTo(fx(50f), fy(56f))
            lineTo(fx(57f), fy(64f))
            lineTo(fx(50f), fy(78f))
            lineTo(fx(43f), fy(64f))
            close()
        }
        drawPath(
            path = innerPrismPath,
            color = if (isRunning) accentCyan else standbyMuted,
            style = Fill
        )

        // Radiant Singularity Heart with Breathing Glow
        val singularityPath = Path().apply {
            moveTo(fx(50f), fy(60f))
            lineTo(fx(54f), fy(64f))
            lineTo(fx(50f), fy(70f))
            lineTo(fx(46f), fy(64f))
            close()
        }
        val heartAlpha = (0.75f + (reactorGlow - 1.0f) * 0.4f * isAwake).coerceIn(0f, 1f)
        drawPath(
            path = singularityPath,
            color = Color.White.copy(alpha = heartAlpha),
            style = Fill
        )
    }
}
