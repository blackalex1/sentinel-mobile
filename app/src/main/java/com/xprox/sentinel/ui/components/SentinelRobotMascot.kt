package com.xprox.sentinel.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay

@Composable
fun SentinelRobotMascot(
    isRunning: Boolean,
    pulseGlow: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // Dynamic Eye Color & Alpha state with smooth transitions
    var currentEyeColor by remember { mutableStateOf(if (isRunning) Color(0xFF10B981) else Color(0xFF06B6D4)) }
    var currentEyeAlpha by remember { mutableStateOf(1.0f) }

    var previousRunningState by remember { mutableStateOf(isRunning) }

    // Smooth Activation Sequence: Red Breathing Pulses -> Smooth Transition to Neon Green
    LaunchedEffect(isRunning) {
        if (isRunning && !previousRunningState) {
            // Wave 1: Smooth Red Flare
            currentEyeColor = Color(0xFFFF1E44)
            currentEyeAlpha = 1.0f
            delay(240)

            // Smooth fade down
            currentEyeAlpha = 0.25f
            delay(130)

            // Wave 2: Smooth Red Flare
            currentEyeAlpha = 1.0f
            delay(260)

            // Smooth fade down
            currentEyeAlpha = 0.35f
            delay(130)

            // Smooth Ignite to Secure Emerald Green!
            currentEyeColor = Color(0xFF10B981)
            currentEyeAlpha = 1.0f
        } else if (!isRunning) {
            // Return to Idle Cyan
            currentEyeColor = Color(0xFF06B6D4)
            currentEyeAlpha = 1.0f
        } else {
            currentEyeColor = Color(0xFF10B981)
            currentEyeAlpha = 1.0f
        }
        previousRunningState = isRunning
    }

    val animatedEyeColor by animateColorAsState(
        targetValue = currentEyeColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "animatedEyeColor"
    )

    val animatedEyeAlpha by animateFloatAsState(
        targetValue = currentEyeAlpha,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "animatedEyeAlpha"
    )

    // Continuous Active Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "mascotActiveTransition")
    
    // 1. Crystal Heartbeat Scale Pulse (Vibrates dynamically when running)
    val crystalHeartbeat by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crystalHeartbeat"
    )

    // 2. Crystal Plasma Shockwave Rings (Expanding outward energy ripples)
    val crystalWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crystalWave1"
    )

    val crystalWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crystalWave2"
    )

    // 3. Eye Luminous Aura Breathing Glow
    val eyeBreathingGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyeBreathingGlow"
    )

    val helmetBorderColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF10B981) else Color(0xFF8B5CF6),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "helmetBorderColor"
    )

    val antennaColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF22D3EE) else Color(0xFF06B6D4),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "antennaColor"
    )

    val coreColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF10B981) else Color(0xFF8B5CF6),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "coreColor"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val s = size.minDimension / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f

        fun fx(v: Float) = ox + v * s
        fun fy(v: Float) = oy + v * s

        // 1. Android Bugdroid Antennas
        drawLine(
            color = antennaColor,
            start = Offset(fx(38f), fy(22f)),
            end = Offset(fx(26f), fy(8f)),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = antennaColor,
            start = Offset(fx(62f), fy(22f)),
            end = Offset(fx(74f), fy(8f)),
            strokeWidth = 5f * s,
            cap = StrokeCap.Round
        )

        // Antenna Spark Halo Emitters when running
        if (isRunning) {
            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.45f * (crystalHeartbeat - 0.85f) / 0.33f),
                radius = 4.5f * s * crystalHeartbeat,
                center = Offset(fx(26f), fy(8f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 1.8f * s,
                center = Offset(fx(26f), fy(8f))
            )

            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.45f * (crystalHeartbeat - 0.85f) / 0.33f),
                radius = 4.5f * s * crystalHeartbeat,
                center = Offset(fx(74f), fy(8f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 1.8f * s,
                center = Offset(fx(74f), fy(8f))
            )
        }

        // 2. Shield Helmet Body
        val helmetPath = Path().apply {
            moveTo(fx(50f), fy(16f))
            cubicTo(fx(76f), fy(16f), fx(82f), fy(26f), fx(82f), fy(42f))
            lineTo(fx(82f), fy(66f))
            cubicTo(fx(82f), fy(82f), fx(50f), fy(96f), fx(50f), fy(96f))
            cubicTo(fx(50f), fy(96f), fx(18f), fy(82f), fx(18f), fy(66f))
            lineTo(fx(18f), fy(42f))
            cubicTo(fx(18f), fy(26f), fx(24f), fy(16f), fx(50f), fy(16f))
            close()
        }

        // Helmet Fill (Dark Carbon Shield)
        drawPath(
            path = helmetPath,
            color = if (isRunning) Color(0xFF0A1911) else Color(0xFF0E0F1A),
            style = Fill
        )

        // Helmet Border with Living Pulse
        drawPath(
            path = helmetPath,
            color = helmetBorderColor,
            style = Stroke(width = 4.5f * s)
        )

        // 3. Side Cheek Wings
        val leftCheekPath = Path().apply {
            moveTo(fx(18f), fy(42f))
            lineTo(fx(34f), fy(52f))
            lineTo(fx(34f), fy(74f))
            lineTo(fx(18f), fy(66f))
            close()
        }
        val leftCheekColor = if (isRunning) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF8B5CF6).copy(alpha = 0.5f)
        drawPath(
            path = leftCheekPath,
            color = leftCheekColor,
            style = Fill
        )

        val rightCheekPath = Path().apply {
            moveTo(fx(82f), fy(42f))
            lineTo(fx(66f), fy(52f))
            lineTo(fx(66f), fy(74f))
            lineTo(fx(82f), fy(66f))
            close()
        }
        drawPath(
            path = rightCheekPath,
            color = Color(0xFF06B6D4).copy(alpha = if (isRunning) 0.55f else 0.5f),
            style = Fill
        )

        // 4. Robot Glowing Visor Eyes (With Radiant Eye Bloom / Aura!)
        val finalEyeColor = animatedEyeColor.copy(alpha = animatedEyeAlpha)

        // Soft Radial Eye Glow / Bloom (Сияние глаз)
        val eyeBloomRadius = 12f * s * (if (isRunning) eyeBreathingGlow else 0.85f)
        val eyeBloomColor = if (isRunning) animatedEyeColor else Color(0xFF06B6D4)
        val bloomAlpha = if (isRunning) (0.40f * eyeBreathingGlow * animatedEyeAlpha).coerceIn(0f, 1f) else (0.18f * animatedEyeAlpha)

        // Left Eye Bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(eyeBloomColor.copy(alpha = bloomAlpha), Color.Transparent),
                center = Offset(fx(35.5f), fy(37.5f)),
                radius = eyeBloomRadius
            ),
            radius = eyeBloomRadius,
            center = Offset(fx(35.5f), fy(37.5f))
        )

        // Right Eye Bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(eyeBloomColor.copy(alpha = bloomAlpha), Color.Transparent),
                center = Offset(fx(64.5f), fy(37.5f)),
                radius = eyeBloomRadius
            ),
            radius = eyeBloomRadius,
            center = Offset(fx(64.5f), fy(37.5f))
        )

        // Polygonal Eyes Geometry
        val leftEyePath = Path().apply {
            moveTo(fx(28f), fy(34f))
            lineTo(fx(43f), fy(34f))
            lineTo(fx(41f), fy(41f))
            lineTo(fx(30f), fy(41f))
            close()
        }
        drawPath(path = leftEyePath, color = finalEyeColor, style = Fill)

        val rightEyePath = Path().apply {
            moveTo(fx(57f), fy(34f))
            lineTo(fx(72f), fy(34f))
            lineTo(fx(70f), fy(41f))
            lineTo(fx(59f), fy(41f))
            close()
        }
        drawPath(path = rightEyePath, color = finalEyeColor, style = Fill)

        // Inner Core Laser Highlights inside each eye
        if (animatedEyeAlpha > 0.4f) {
            val sparkAlpha = if (isRunning) (0.85f * eyeBreathingGlow).coerceIn(0f, 1f) else 0.5f
            drawCircle(
                color = Color.White.copy(alpha = sparkAlpha * animatedEyeAlpha),
                radius = 1.4f * s,
                center = Offset(fx(35.5f), fy(37.5f))
            )
            drawCircle(
                color = Color.White.copy(alpha = sparkAlpha * animatedEyeAlpha),
                radius = 1.4f * s,
                center = Offset(fx(64.5f), fy(37.5f))
            )
        }

        // 5. Sentinel Diamond Plasma Core (With Dynamic Scaled Pulsation & Expanding Shockwaves!)
        val cScale = if (isRunning) crystalHeartbeat else 1.0f

        // Expanding Plasma Shockwave Ripples from Crystal Heart
        if (isRunning) {
            val sw1Radius = 8f * s + 18f * s * crystalWave1
            val sw1Alpha = (1f - crystalWave1) * 0.40f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF22D3EE).copy(alpha = sw1Alpha), Color.Transparent),
                    center = Offset(fx(50f), fy(60f)),
                    radius = sw1Radius
                ),
                radius = sw1Radius,
                center = Offset(fx(50f), fy(60f))
            )

            val sw2Radius = 8f * s + 18f * s * crystalWave2
            val sw2Alpha = (1f - crystalWave2) * 0.40f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = sw2Alpha), Color.Transparent),
                    center = Offset(fx(50f), fy(60f)),
                    radius = sw2Radius
                ),
                radius = sw2Radius,
                center = Offset(fx(50f), fy(60f))
            )

            // Dynamic Radiant Plasma Core Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF22D3EE).copy(alpha = 0.50f * (cScale - 0.85f) / 0.33f),
                        Color.Transparent
                    ),
                    center = Offset(fx(50f), fy(60f)),
                    radius = 22f * s * cScale
                ),
                radius = 22f * s * cScale,
                center = Offset(fx(50f), fy(60f))
            )
        }

        // Outer Diamond (Morphs and Scales with Heartbeat)
        val outerDiamondPath = Path().apply {
            moveTo(fx(50f), fy(60f - 16f * cScale))
            lineTo(fx(50f + 14f * cScale), fy(60f - 2f * cScale))
            lineTo(fx(50f), fy(60f + 20f * cScale))
            lineTo(fx(50f - 14f * cScale), fy(60f - 2f * cScale))
            close()
        }
        drawPath(path = outerDiamondPath, color = coreColor, style = Fill)
        drawPath(
            path = outerDiamondPath,
            color = if (isRunning) Color(0xFF34D399) else Color(0xFFA78BFA),
            style = Stroke(width = 1.2f * s)
        )

        // Inner Spark (Scales Dynamically)
        val innerScale = cScale * 0.75f
        val innerSparkPath = Path().apply {
            moveTo(fx(50f), fy(60f - 10f * innerScale))
            lineTo(fx(50f + 8f * innerScale), fy(60f - 2f * innerScale))
            lineTo(fx(50f), fy(60f + 13f * innerScale))
            lineTo(fx(50f - 8f * innerScale), fy(60f - 2f * innerScale))
            close()
        }
        drawPath(
            path = innerSparkPath,
            color = if (isRunning) Color(0xFF22D3EE) else Color(0xFF38BDF8),
            style = Fill
        )

        // Central White Spark Heart (Scales Dynamically)
        val heartScale = cScale * 0.45f
        val whiteHeartPath = Path().apply {
            moveTo(fx(50f), fy(60f - 6f * heartScale))
            lineTo(fx(50f + 4f * heartScale), fy(60f - 2f * heartScale))
            lineTo(fx(50f), fy(60f + 6f * heartScale))
            lineTo(fx(50f - 4f * heartScale), fy(60f - 2f * heartScale))
            close()
        }
        drawPath(path = whiteHeartPath, color = Color.White, style = Fill)
    }
}
