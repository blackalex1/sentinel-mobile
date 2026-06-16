package com.xprox.sentinel.ui.components.blockedapps

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.service.ThreatDetectionManager
import com.xprox.sentinel.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun UserThreatRow(
    pkgName: String,
    context: Context,
    onReportClick: () -> Unit,
    onPcapClick: () -> Unit,
    onUnblockClick: () -> Unit
) {
    val appLabel = remember(pkgName) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBg)
            .padding(12.dp)
    ) {
        // Top Row: App Label/Package on the left, "ИЗОЛИРОВАН" premium neon warning tag on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appLabel,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pkgName,
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Premium blinking/neon status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(WarningRed.copy(alpha = 0.15f))
                    .border(1.dp, WarningRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ИЗОЛИРОВАН",
                    color = WarningRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Bottom Row: Action buttons horizontally arranged
        val triggerTime = remember(pkgName) { ThreatDetectionManager.getTriggerTime(pkgName) }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Text Forensics Report sharing button
            Button(
                onClick = onReportClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarningRed.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Отчет",
                    color = WarningRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Wireshark PCAP countdown or download button
            if (triggerTime != null) {
                val currentTime = remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(pkgName) {
                    while (true) {
                        currentTime.value = System.currentTimeMillis()
                        delay(1000L)
                    }
                }

                val isCapturing = (currentTime.value - triggerTime) < 300000L
                val remainingSec = if (isCapturing) {
                    (300000L - (currentTime.value - triggerTime)) / 1000L
                } else 0L

                if (isCapturing) {
                    // Blinking active sniffer button
                    val min = remainingSec / 60
                    val sec = remainingSec % 60
                    val timerText = String.format(Locale.US, "%d:%02d", min, sec)
                    
                    Button(
                        onClick = onPcapClick,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningYellow.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔴 Сбор ($timerText)",
                            color = WarningYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Finished stable download button
                    Button(
                        onClick = onPcapClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "💾 PCAP дамп",
                            color = CyberTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Unblock Restoration Button
            Button(
                onClick = onUnblockClick,
                colors = ButtonDefaults.buttonColors(containerColor = TextWhite.copy(alpha = 0.10f)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Разблокировать",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
