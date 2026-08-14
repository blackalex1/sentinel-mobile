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
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SentinelRobotMascot(
    isRunning: Boolean,
    pulseGlow: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // Dynamic Eye Color & Alpha state with smooth transitions
    var currentEyeColor by remember { mutableStateOf(if (isRunning) Color(0xFF10B981) else Color(0xFF8B5CF6)) }
    var currentEyeAlpha by remember { mutableStateOf(1.0f) }
    var isShocking by remember { mutableStateOf(false) }
    var isFallingAsleep by remember { mutableStateOf(false) }

    var previousRunningState by remember { mutableStateOf(isRunning) }

    // Shock Sequence on Connect & Falling Asleep Sequence on Disconnect
    LaunchedEffect(isRunning) {
        if (isRunning && !previousRunningState) {
            // Power-On Electric Shock Sequence
            isFallingAsleep = false
            isShocking = true

            currentEyeColor = Color(0xFF00FFFF) // Cyan Shock Flash
            currentEyeAlpha = 1.0f
            delay(120)

            currentEyeColor = Color(0xFFFF1E44) // Red Flare
            currentEyeAlpha = 0.4f
            delay(100)

            currentEyeColor = Color(0xFFFACC15) // Yellow Lightning Flash
            currentEyeAlpha = 1.0f
            delay(180)

            currentEyeColor = Color(0xFF10B981) // Secure Emerald Green!
            currentEyeAlpha = 1.0f
            delay(200)

            isShocking = false
        } else if (!isRunning && previousRunningState) {
            // Smooth Falling Asleep Sequence on Disconnect!
            isShocking = false
            isFallingAsleep = true

            // Step 1: Sleepy Amber Glow
            currentEyeColor = Color(0xFFF59E0B)
            currentEyeAlpha = 0.9f
            delay(200)

            // Step 2: Soft Lavender Yawn
            currentEyeColor = Color(0xFFA78BFA)
            currentEyeAlpha = 0.7f
            delay(220)

            // Step 3: Deep Sleep Purple
            currentEyeColor = Color(0xFF8B5CF6)
            currentEyeAlpha = 0.8f
            delay(180)

            isFallingAsleep = false
        } else if (!isRunning) {
            isShocking = false
            isFallingAsleep = false
            currentEyeColor = Color(0xFF8B5CF6)
            currentEyeAlpha = 0.8f
        } else {
            isShocking = false
            isFallingAsleep = false
            currentEyeColor = Color(0xFF10B981)
            currentEyeAlpha = 1.0f
        }
        previousRunningState = isRunning
    }

    // --- SMOOTH TRANSITION ANIMATED STATES ---
    val eyeOpenness by animateFloatAsState(
        targetValue = if (isRunning) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "eyeOpenness"
    )

    val zFadeAlpha by animateFloatAsState(
        targetValue = if (!isRunning) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "zFadeAlpha"
    )

    val shockFadeAlpha by animateFloatAsState(
        targetValue = if (isShocking) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "shockFadeAlpha"
    )

    val headDipY by animateFloatAsState(
        targetValue = if (isFallingAsleep) 3.5f else 0.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "headDipY"
    )

    val animatedEyeColor by animateColorAsState(
        targetValue = currentEyeColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animatedEyeColor"
    )

    val animatedEyeAlpha by animateFloatAsState(
        targetValue = currentEyeAlpha,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "animatedEyeAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mascotActiveTransition")

    // 1. Sleeping Slow Breathing Motion Y-offset
    val sleepBreathingYRaw by infiniteTransition.animateFloat(
        initialValue = -2.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleepBreathingYRaw"
    )
    val sleepBreathingY = sleepBreathingYRaw * (1.0f - eyeOpenness)

    // 2. Animated Floating "Z Z Z Z" Sleeping Effect
    val zProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zProgress1"
    )

    val zProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zProgress2"
    )

    val zProgress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zProgress3"
    )

    // 3. Electric Micro-shake / Jitter ONLY during shock
    val electricShakeXRaw by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "electricShakeXRaw"
    )

    val electricShakeYRaw by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 65, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "electricShakeYRaw"
    )
    val electricShakeX = electricShakeXRaw * shockFadeAlpha
    val electricShakeY = electricShakeYRaw * shockFadeAlpha

    // 4. Electric Lightning Flash Alpha
    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightningFlash"
    )

    // 5. Crystal Heartbeat Scale Pulse when connected
    val crystalHeartbeat by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crystalHeartbeat"
    )

    // 6. Crystal Plasma Shockwave Rings
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

    // 7. Eye Luminous Aura Breathing Glow
    val eyeBreathingGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyeBreathingGlow"
    )

    val helmetBorderColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF10B981) else Color(0xFF8B5CF6).copy(alpha = 0.6f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "helmetBorderColor"
    )

    val antennaColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF22D3EE) else Color(0xFF8B5CF6).copy(alpha = 0.7f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "antennaColor"
    )

    val coreColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF10B981) else Color(0xFF6D28D9),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "coreColor"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val s = size.minDimension / 100f

        val ox = (size.width - 100f * s) / 2f + electricShakeX * s
        val oy = (size.height - 100f * s) / 2f + (electricShakeY + sleepBreathingY + headDipY) * s

        fun fx(v: Float) = ox + v * s
        fun fy(v: Float) = oy + v * s

        // --- DRAW FLOATING SLEEPING "Z Z Z" (WITH SMOOTH FADE IN ON FALLING ASLEEP) ---
        if (zFadeAlpha > 0.01f) {
            fun drawFloatingZ(progress: Float) {
                if (progress <= 0f || progress >= 1f) return
                val zX = fx(62f + 13f * progress)
                val zY = fy(26f - 16f * progress)
                val zScale = (0.35f + 0.40f * progress) * s
                val alpha = (sin(progress * PI)).toFloat().coerceIn(0f, 1f) * 0.80f * zFadeAlpha
                val zColor = Color(0xFFA78BFA)

                val zPath = Path().apply {
                    moveTo(zX - 3.5f * zScale, zY - 4.5f * zScale)
                    lineTo(zX + 3.5f * zScale, zY - 4.5f * zScale)
                    lineTo(zX - 3.5f * zScale, zY + 4.5f * zScale)
                    lineTo(zX + 3.5f * zScale, zY + 4.5f * zScale)
                }
                drawPath(
                    path = zPath,
                    color = zColor.copy(alpha = alpha),
                    style = Stroke(width = 2.0f * zScale, cap = StrokeCap.Round)
                )
            }

            drawFloatingZ(zProgress1)
            drawFloatingZ(zProgress2)
            drawFloatingZ(zProgress3)
        }

        // --- DRAW ELECTRIC LIGHTNING SPARKS (WITH SMOOTH FADE OUT) ---
        if (shockFadeAlpha > 0.01f && lightningFlash > 0.25f) {
            val sparkAlpha = (lightningFlash * 0.95f * shockFadeAlpha).coerceIn(0f, 1f)

            // Spark 1: Left Antenna Bolt
            val bolt1 = Path().apply {
                moveTo(fx(26f), fy(8f))
                lineTo(fx(21f), fy(18f))
                lineTo(fx(27f), fy(18f))
                lineTo(fx(23f), fy(30f))
            }
            drawPath(
                path = bolt1,
                color = Color(0xFF00FFFF).copy(alpha = sparkAlpha),
                style = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
            )

            // Spark 2: Right Antenna Bolt
            val bolt2 = Path().apply {
                moveTo(fx(74f), fy(8f))
                lineTo(fx(79f), fy(18f))
                lineTo(fx(73f), fy(18f))
                lineTo(fx(77f), fy(30f))
            }
            drawPath(
                path = bolt2,
                color = Color(0xFFFACC15).copy(alpha = sparkAlpha),
                style = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
            )

            // Spark 3: Top Crown Arc Bolt
            val bolt3 = Path().apply {
                moveTo(fx(36f), fy(14f))
                lineTo(fx(50f), fy(7f))
                lineTo(fx(47f), fy(14f))
                lineTo(fx(64f), fy(14f))
            }
            drawPath(
                path = bolt3,
                color = Color.White.copy(alpha = sparkAlpha),
                style = Stroke(width = 2.0f * s, cap = StrokeCap.Round)
            )
        }

        // 1. Android Antennas
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

        // Antenna Spark Halos (Smooth Alpha Interpolation)
        val haloAlpha = (1.0f - zFadeAlpha)
        if (haloAlpha > 0.01f) {
            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.55f * haloAlpha * (crystalHeartbeat - 0.85f) / 0.30f),
                radius = 5.0f * s * crystalHeartbeat,
                center = Offset(fx(26f), fy(8f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * haloAlpha),
                radius = 2.0f * s,
                center = Offset(fx(26f), fy(8f))
            )

            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.55f * haloAlpha * (crystalHeartbeat - 0.85f) / 0.30f),
                radius = 5.0f * s * crystalHeartbeat,
                center = Offset(fx(74f), fy(8f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * haloAlpha),
                radius = 2.0f * s,
                center = Offset(fx(74f), fy(8f))
            )
        }

        val sleepHaloAlpha = zFadeAlpha
        if (sleepHaloAlpha > 0.01f) {
            drawCircle(
                color = Color(0xFF8B5CF6).copy(alpha = 0.4f * sleepHaloAlpha),
                radius = 3.0f * s,
                center = Offset(fx(26f), fy(8f))
            )
            drawCircle(
                color = Color(0xFF8B5CF6).copy(alpha = 0.4f * sleepHaloAlpha),
                radius = 3.0f * s,
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

        val helmetFillColor = Color(
            red = (15 + (10 - 15) * eyeOpenness) / 255f,
            green = (11 + (25 - 11) * eyeOpenness) / 255f,
            blue = (30 + (17 - 30) * eyeOpenness) / 255f,
            alpha = 1.0f
        )

        drawPath(
            path = helmetPath,
            color = helmetFillColor,
            style = Fill
        )

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
        val leftCheekColor = if (isRunning) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF8B5CF6).copy(alpha = 0.35f)
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
        val rightCheekColor = if (isRunning) Color(0xFF06B6D4).copy(alpha = 0.55f) else Color(0xFF8B5CF6).copy(alpha = 0.35f)
        drawPath(
            path = rightCheekPath,
            color = rightCheekColor,
            style = Fill
        )

        // 4. Robot Eyes: SILKY SMOOTH CROSS-FADE MORPH BETWEEN CLOSED SLEEPING EYES & OPEN AWAKE EYES
        val finalEyeColor = animatedEyeColor.copy(alpha = animatedEyeAlpha)

        // --- CLOSED SLEEPING EYE COMPONENT (Fades out smoothly as eyeOpenness goes 0 -> 1) ---
        val closedEyeAlpha = (1.0f - eyeOpenness).coerceIn(0f, 1f)
        if (closedEyeAlpha > 0.01f) {
            val leftClosedEyePath = Path().apply {
                moveTo(fx(27f), fy(38f))
                quadraticTo(fx(35.5f), fy(43f), fx(44f), fy(38f))
            }
            drawPath(
                path = leftClosedEyePath,
                color = animatedEyeColor.copy(alpha = 0.85f * closedEyeAlpha),
                style = Stroke(width = 3.5f * s, cap = StrokeCap.Round)
            )

            val rightClosedEyePath = Path().apply {
                moveTo(fx(56f), fy(38f))
                quadraticTo(fx(64.5f), fy(43f), fx(73f), fy(38f))
            }
            drawPath(
                path = rightClosedEyePath,
                color = animatedEyeColor.copy(alpha = 0.85f * closedEyeAlpha),
                style = Stroke(width = 3.5f * s, cap = StrokeCap.Round)
            )
        }

        // --- OPEN AWAKE EYE COMPONENT (Fades in & opens smoothly as eyeOpenness goes 0 -> 1) ---
        val openEyeAlpha = eyeOpenness.coerceIn(0f, 1f)
        if (openEyeAlpha > 0.01f) {
            val eyeBloomRadius = 13f * s * eyeBreathingGlow
            val eyeBloomColor = animatedEyeColor
            val bloomAlpha = (0.45f * eyeBreathingGlow * animatedEyeAlpha * openEyeAlpha).coerceIn(0f, 1f)

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

            // Polygonal Eyes Geometry with Smooth Vertical Opening Interpolation
            val eyeTopY = 38f - 5f * eyeOpenness
            val eyeBottomY = 38f + 4f * eyeOpenness

            val leftEyePath = Path().apply {
                moveTo(fx(27f), fy(eyeTopY))
                lineTo(fx(44f), fy(eyeTopY))
                lineTo(fx(42f), fy(eyeBottomY))
                lineTo(fx(29f), fy(eyeBottomY))
                close()
            }
            drawPath(path = leftEyePath, color = finalEyeColor.copy(alpha = finalEyeColor.alpha * openEyeAlpha), style = Fill)

            val rightEyePath = Path().apply {
                moveTo(fx(56f), fy(eyeTopY))
                lineTo(fx(73f), fy(eyeTopY))
                lineTo(fx(71f), fy(eyeBottomY))
                lineTo(fx(58f), fy(eyeBottomY))
                close()
            }
            drawPath(path = rightEyePath, color = finalEyeColor.copy(alpha = finalEyeColor.alpha * openEyeAlpha), style = Fill)

            // Inner Core Laser Highlights inside each eye
            val sparkAlpha = (0.90f * eyeBreathingGlow * openEyeAlpha).coerceIn(0f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = sparkAlpha * animatedEyeAlpha),
                radius = 1.6f * s * eyeOpenness,
                center = Offset(fx(35.5f), fy(37.5f))
            )
            drawCircle(
                color = Color.White.copy(alpha = sparkAlpha * animatedEyeAlpha),
                radius = 1.6f * s * eyeOpenness,
                center = Offset(fx(64.5f), fy(37.5f))
            )
        }

        // 5. Sentinel Diamond Plasma Core
        val cScale = 0.85f + (crystalHeartbeat - 0.85f) * eyeOpenness

        // Expanding Plasma Shockwave Ripples from Crystal Heart (when running)
        if (openEyeAlpha > 0.01f) {
            val sw1Radius = 8f * s + 18f * s * crystalWave1
            val sw1Alpha = (1f - crystalWave1) * 0.40f * openEyeAlpha
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
            val sw2Alpha = (1f - crystalWave2) * 0.40f * openEyeAlpha
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
                        Color(0xFF22D3EE).copy(alpha = (0.50f * (cScale - 0.85f) / 0.33f * openEyeAlpha).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = Offset(fx(50f), fy(60f)),
                    radius = 22f * s * cScale
                ),
                radius = 22f * s * cScale,
                center = Offset(fx(50f), fy(60f))
            )
        }

        // Outer Diamond
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
            color = if (isRunning) Color(0xFF34D399) else Color(0xFFA78BFA).copy(alpha = 0.7f),
            style = Stroke(width = 1.2f * s)
        )

        // Inner Spark
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
            color = if (isRunning) Color(0xFF22D3EE) else Color(0xFF8B5CF6).copy(alpha = 0.6f),
            style = Fill
        )

        // Central White Spark Heart
        val heartScale = cScale * 0.45f
        val whiteHeartPath = Path().apply {
            moveTo(fx(50f), fy(60f - 6f * heartScale))
            lineTo(fx(50f + 4f * heartScale), fy(60f - 2f * heartScale))
            lineTo(fx(50f), fy(60f + 6f * heartScale))
            lineTo(fx(50f - 4f * heartScale), fy(60f - 2f * heartScale))
            close()
        }
        drawPath(path = whiteHeartPath, color = Color.White.copy(alpha = 0.5f + 0.5f * openEyeAlpha), style = Fill)
    }
}
