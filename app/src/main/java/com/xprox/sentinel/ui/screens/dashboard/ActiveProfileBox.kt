package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

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
        Card(
            colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.1f)),
            modifier = modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Warning", tint = WarningRed)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = string("no_profile_warning"),
                    fontSize = 11.sp,
                    color = WarningRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberTeal.copy(alpha = 0.05f)),
            modifier = modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active Profile", tint = CyberTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${string("active_profile")}: ${activeProfile.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = if (isDirect) {
                                    if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "DIRECT • Режим анализа трафика" else "DIRECT • Traffic Analysis Mode"
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
                                .background(WarningYellow.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .border(1.dp, WarningYellow.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Warning",
                                        tint = WarningYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = string("analytics_collection_warning"),
                                        fontSize = 10.sp,
                                        color = WarningYellow,
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
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.End).height(24.dp)
                                ) {
                                    Text(
                                        text = string("analytics_confirm_btn"),
                                        color = DarkBg,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberTeal.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .border(1.dp, CyberTeal.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Confirmed",
                                    tint = CyberTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = string("analytics_collection_approved"),
                                    fontSize = 10.sp,
                                    color = CyberTeal,
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
