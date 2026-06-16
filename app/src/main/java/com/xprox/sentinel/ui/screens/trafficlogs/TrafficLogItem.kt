package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun TrafficLogItem(
    logLine: com.xprox.sentinel.ui.screens.VisualLogEntry,
    activePorts: Set<Int>
) {
    val isLineSensitive = logLine.port != null && activePorts.contains(logLine.port)
    val displayLine = formatLogLineDynamically(logLine.line, activePorts)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isLineSensitive) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (isLineSensitive) "Alert" else "Info",
                tint = if (isLineSensitive) WarningRed else CyberTeal,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayLine,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextWhite,
                lineHeight = 16.sp
            )
        }
        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
    }
}

fun formatLogLineDynamically(line: String, activePorts: Set<Int>): String {
    val portMatch = Regex("""Port\s+(\d+)""").find(line)
    val port = portMatch?.groupValues?.get(1)?.toIntOrNull() ?: return line
    val isCurrentlySensitive = activePorts.contains(port)
    
    val hasAlert = line.contains("[ALERT:")
    val hasInfo = line.contains("[INFO:")
    
    if (isCurrentlySensitive && hasInfo) {
        return line.replace("[INFO: Port $port]", "[ALERT: Port $port]")
    } else if (!isCurrentlySensitive && hasAlert) {
        val alertPattern = Regex("""\[ALERT:[^\]]+Port\s+\d+\)\]""")
        return line.replace(alertPattern, "[INFO: Port $port]")
    }
    
    return line
}
