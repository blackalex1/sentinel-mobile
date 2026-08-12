package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                Icon(imageVector = Icons.Default.Info, contentDescription = "Warning", tint = WarningRose)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = string("no_profile_warning"),
                    fontSize = 11.sp,
                    color = WarningRose,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        DoppelrandCard(
            modifier = modifier.fillMaxWidth(),
            borderColor = if (isRunning) SecureGreen.copy(alpha = 0.4f) else ElectricViolet.copy(alpha = 0.35f),
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active Profile",
                            tint = if (isRunning) SecureGreen else ElectricViolet
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${string("active_profile")}: ${activeProfile.name}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = if (isDirect) {
                                    if (LanguageManager.currentLanguage.value.code == "ru") "DIRECT • Режим анализа трафика" else "DIRECT • Traffic Analysis Mode"
                                } else {
                                    "${activeProfile.type} • ${activeProfile.address}"
                                },
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
                
                if (isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!isConfirmed) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarningAmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, WarningAmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Warning",
                                        tint = WarningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = string("analytics_collection_warning"),
                                        fontSize = 10.sp,
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        XrayProfilePersistence.saveAnalyticsConfirmed(context, true)
                                        isConfirmed = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.End).height(26.dp)
                                ) {
                                    Text(
                                        text = string("analytics_confirm_btn"),
                                        color = VoidBg,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SecureGreen.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                .border(1.dp, SecureGreen.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Confirmed",
                                    tint = SecureGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
