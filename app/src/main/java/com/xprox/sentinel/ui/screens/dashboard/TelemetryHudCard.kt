package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun TelemetryHudCard(
    isRunning: Boolean,
    speedText: String,
    isRu: Boolean
) {
    AnimatedVisibility(
        visible = isRunning && speedText.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // Parse speeds dynamically from the single flow string to build an ultra-premium split column HUD
        val speeds = speedText.split("|")
        val downSpeed = speeds.getOrNull(0)?.trim()?.removePrefix("↓")?.trim() ?: "0.0 B/s"
        val upSpeed = speeds.getOrNull(1)?.trim()?.removePrefix("↑")?.trim() ?: "0.0 B/s"

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Text(
                    text = if (isRu) "АКТИВНАЯ ТЕЛЕМЕТРИЯ СЕТИ" else "LIVE NETWORK TELEMETRY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTeal,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Download Panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        border = BorderStroke(0.5.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isRu) "ЗАГРУЗКА" else "DOWNLOAD",
                                fontSize = 9.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Down",
                                    tint = SecureGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = downSpeed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }
                    }

                    // Upload Panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBg),
                        border = BorderStroke(0.5.dp, CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isRu) "ОТДАЧА" else "UPLOAD",
                                fontSize = 9.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Up",
                                    tint = CyberBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = upSpeed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
