package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.DoppelrandCard

@Composable
fun ActiveProfileBox(
    activeProfile: XrayConfigManager.ServerProfile,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isConfirmed by remember(isRunning) { mutableStateOf(XrayProfilePersistence.loadAnalyticsConfirmed(context)) }
    val isDirect = activeProfile.type.uppercase() == "DIRECT"

    if (!isDirect && (activeProfile.address.isEmpty() || activeProfile.uuid.isEmpty())) {
        DoppelrandCard(
            modifier = modifier.fillMaxWidth(),
            borderColor = WarningRose.copy(alpha = 0.5f),
            contentPadding = PaddingValues(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Warning",
                    tint = WarningRose,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = string("no_profile_warning"),
                    fontSize = 11.5.sp,
                    color = WarningRose,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        DoppelrandCard(
            modifier = modifier.fillMaxWidth(),
            borderColor = if (isRunning) SecureGreen.copy(alpha = 0.45f) else ElectricViolet.copy(alpha = 0.35f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Server Profile Info Row with Protocol Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Node Icon with Glowing Bezel
                        Surface(
                            color = if (isRunning) SecureGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isRunning) SecureGreen.copy(alpha = 0.4f) else ElectricViolet.copy(alpha = 0.4f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDirect) Icons.Default.Shield else Icons.Default.Dns,
                                    contentDescription = "Active Server Node",
                                    tint = if (isRunning) SecureGreen else ElectricViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProfile.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDirect) {
                                    if (LanguageManager.currentLanguage.value.code == "ru") "Прямое подключение (Аудит)" else "Direct connection (Audit mode)"
                                } else {
                                    activeProfile.address
                                },
                                fontSize = 11.sp,
                                color = TextGray,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    // Protocol Badge Pill
                    Surface(
                        color = (if (isRunning) SecureGreen else CyberCyan).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, (if (isRunning) SecureGreen else CyberCyan).copy(alpha = 0.4f)),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (isDirect) "DIRECT" else activeProfile.type.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRunning) SecureGreen else CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Inline Telemetry / Audit Compliance Chip (when running)
                if (isRunning) {
                    if (!isConfirmed) {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Warning",
                                        tint = WarningAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = string("analytics_collection_warning"),
                                        fontSize = 10.sp,
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Button(
                                    onClick = {
                                        XrayProfilePersistence.saveAnalyticsConfirmed(context, true)
                                        isConfirmed = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(24.dp).padding(start = 6.dp)
                                ) {
                                    Text(
                                        text = string("analytics_confirm_btn"),
                                        color = VoidBg,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = SecureGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SecureGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Confirmed",
                                    tint = SecureGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = string("analytics_collection_approved"),
                                    fontSize = 10.sp,
                                    color = SecureGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
