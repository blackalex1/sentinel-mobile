package com.xprox.sentinel.ui.components
 
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string
 
@Composable
fun DashboardRadarButton(
    isRunning: Boolean,
    pingMs: Int? = null,
    publicIp: String? = null,
    hasProfile: Boolean = false,
    onPingClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRunning) 5000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            if (isRunning) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                ) {
                    drawCircle(
                        color = CyberTeal,
                        radius = (size.minDimension / 2) * pulseRadius,
                        style = Stroke(width = 1.5.dp.toPx()),
                        alpha = pulseAlpha
                    )
                }
            }

            // Outer Rotating Sweep Gradient Bezel
            Surface(
                modifier = Modifier
                    .size(175.dp)
                    .graphicsLayer(
                        rotationZ = rotation,
                        scaleX = scale,
                        scaleY = scale
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(CyberTeal, CyberPurple, CyberTeal)
                    )
                )
            ) {}

            // Inner Dashed Compass/Radar Details Ring
            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer(
                        rotationZ = -rotation * 0.5f,
                        scaleX = scale,
                        scaleY = scale
                    )
            ) {
                drawCircle(
                    color = if (isRunning) CyberTeal.copy(alpha = 0.25f) else CyberPurple.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 15f), 0f
                        )
                    )
                )
            }

            // Central Interactive Command Core Node
            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
                    .shadow(
                        elevation = if (isPressed) 6.dp else 18.dp,
                        shape = CircleShape,
                        ambientColor = if (isRunning) CyberTeal else CyberPurple,
                        spotColor = if (isRunning) CyberTeal else CyberPurple
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ),
                shape = CircleShape,
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isRunning) SecureGreen else WarningRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRunning) string("shield_active") else string("disconnected"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) SecureGreen else WarningRed,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
            modifier = if (hasProfile && onPingClick != null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPingClick() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            } else {
                Modifier
            }
        ) {
            Text(
                text = if (publicIp != null) "${string("public_ip")}: $publicIp" else string("checking_ip"),
                fontSize = 14.sp,
                color = CyberTeal,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (!hasProfile) {
                    string("ping_na")
                } else if (pingMs != null) {
                    "${string("ping")}: ${pingMs}ms"
                } else {
                    string("checking_ping")
                },
                fontSize = 12.sp,
                color = if (pingMs != null) {
                    if (pingMs < 150) SecureGreen else if (pingMs < 300) CyberTeal else WarningRed
                } else {
                    TextGray
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}
