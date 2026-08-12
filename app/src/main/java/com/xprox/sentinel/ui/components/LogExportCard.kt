package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.*

@Composable
fun LogExportCard(
    context: Context
) {
    val prefs = remember { context.getSharedPreferences("x_prox_sensitive_ports_prefs", Context.MODE_PRIVATE) }
    var saveAllLogs by remember { mutableStateOf(prefs.getBoolean("save_all_logs_to_disk", false)) }
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    DoppelrandCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ElectricViolet.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = string("log_path"),
            fontSize = 12.sp,
            color = ElectricViolet,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = LogManager.getLogFilePath(context),
            fontSize = 11.sp,
            color = TextGray,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = string("session_history_title") + ":",
            fontSize = 12.sp,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = string("backups_desc"),
            fontSize = 11.sp,
            color = TextGray
        )

        HorizontalDivider(
            color = CardBorder,
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRu) "Записывать весь трафик на диск" else "Log All Traffic to Disk",
                    fontSize = 12.5.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRu) {
                        "Сохранять даже обычные HTTPS/HTTP запросы (может занимать память)"
                    } else {
                        "Logs even standard HTTPS/HTTP requests (can use more storage)"
                    },
                    fontSize = 11.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Switch(
                checked = saveAllLogs,
                onCheckedChange = { checked ->
                    saveAllLogs = checked
                    prefs.edit().putBoolean("save_all_logs_to_disk", checked).apply()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricViolet,
                    checkedTrackColor = ElectricViolet.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = CardBorder
                ),
                modifier = Modifier.scale(0.85f)
            )
        }

        HorizontalDivider(
            color = CardBorder,
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        var xrayLogLevel by remember { mutableStateOf(prefs.getString("xray_log_level", "info") ?: "info") }
        val levels = listOf("debug", "info", "warning", "error", "none")

        Text(
            text = if (isRu) "Уровень логов Xray-core" else "Xray-core Log Level",
            fontSize = 12.sp,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isRu) "Выберите детализацию системного ядра Xray" else "Select verbosity for Xray native process",
            fontSize = 11.sp,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = xrayLogLevel.equals(level, ignoreCase = true)
                    val chipColor = when (level) {
                        "debug" -> CyberCyan
                        "info" -> SecureGreen
                        "warning" -> WarningAmber
                        "error" -> WarningRose
                        else -> TextGray
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) chipColor else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable {
                                xrayLogLevel = level
                                prefs.edit().putString("xray_log_level", level).apply()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) chipColor else TextGray,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
