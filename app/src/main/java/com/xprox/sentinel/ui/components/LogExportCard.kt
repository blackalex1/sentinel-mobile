package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string

@Composable
fun LogExportCard(
    context: Context
) {
    val prefs = remember { context.getSharedPreferences("x_prox_sensitive_ports_prefs", Context.MODE_PRIVATE) }
    var saveAllLogs by remember { mutableStateOf(prefs.getBoolean("save_all_logs_to_disk", false)) }
    val isRu = com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru"

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = string("log_path"),
                fontSize = 13.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = LogManager.getLogFilePath(context),
                fontSize = 11.sp,
                color = TextGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = string("session_history_title") + ":",
                fontSize = 13.sp,
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
                        fontSize = 13.sp,
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
                        checkedThumbColor = CyberTeal,
                        checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CardBorder
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }
        }
    }
}
