package com.xprox.sentinel.ui.components.sensitiveports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SensitivePortItem(
    port: Int,
    serviceName: String,
    isActive: Boolean,
    isCustom: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portColor = getPortColor(port, isCustom)
    val portTag = getPortTag(port, isCustom)

    // Smooth status transitions
    val animBgColor by animateColorAsState(
        targetValue = if (isActive) portColor.copy(alpha = 0.06f) else DarkBg.copy(alpha = 0.5f),
        animationSpec = tween(250)
    )
    val animBorderColor by animateColorAsState(
        targetValue = if (isActive) portColor.copy(alpha = 0.5f) else CardBorder,
        animationSpec = tween(250)
    )
    val animStripeColor by animateColorAsState(
        targetValue = if (isActive) portColor else CardBorder.copy(alpha = 0.2f),
        animationSpec = tween(250)
    )
    val animPortColor by animateColorAsState(
        targetValue = if (isActive) portColor else TextWhite,
        animationSpec = tween(250)
    )
    val animToggleBgColor by animateColorAsState(
        targetValue = if (isActive) portColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(250)
    )

    val clickableModifier = if (isCustom) {
        Modifier.combinedClickable(
            onClick = onToggle,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(onClick = onToggle)
    }

    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(8.dp))
            .background(animBgColor)
            .border(
                width = 1.dp,
                color = animBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .then(clickableModifier)
    ) {
        // Left vertical industrial accent stripe
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .fillMaxHeight()
                .background(animStripeColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$port",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = animPortColor
                    )
                    
                    // Category classification tag
                    Text(
                        text = portTag,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isActive) portColor else TextGray,
                        modifier = Modifier
                            .background(
                                color = if (isActive) portColor.copy(alpha = 0.12f) else CardBorder.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = serviceName,
                    fontSize = 9.sp,
                    color = if (isActive) TextWhite else TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))

            // Cybernetic custom tick-box selector
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(animToggleBgColor)
                    .border(
                        width = 1.dp,
                        color = animBorderColor,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(portColor)
                    )
                }
            }
        }
    }
}

private fun getPortColor(port: Int, isCustom: Boolean = false): Color {
    if (isCustom) return CyberTeal
    return when (port) {
        21, 23, 25, 80, 110, 143, 445, 3389 -> WarningRed
        3306, 6379, 27017 -> CyberPurple
        22, 53, 443 -> CyberTeal
        else -> CyberBlue
    }
}

private fun getPortTag(port: Int, isCustom: Boolean = false): String {
    if (isCustom) return "CUSTOM"
    return when (port) {
        21, 23, 25, 80, 110, 143 -> "PLAIN"
        445, 3389 -> "EXPLOIT"
        3306, 6379, 27017 -> "DBMS"
        22, 443 -> "SECURE"
        53 -> "SYSTEM"
        else -> "AUDIT"
    }
}
