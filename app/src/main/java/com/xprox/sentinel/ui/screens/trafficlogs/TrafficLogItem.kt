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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
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
    val highlightedText = renderHighlightedLog(displayLine, isLineSensitive)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isLineSensitive) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (isLineSensitive) "Alert" else "Info",
                tint = if (isLineSensitive) WarningRed else CyberTeal,
                modifier = Modifier
                    .size(14.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
        }
        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
    }
}

fun renderHighlightedLog(line: String, isSensitive: Boolean): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        try {
            val timestampRegex = Regex("""^\[([^\]]+)\]""")
            val timestampMatch = timestampRegex.find(line)
            var currentIdx = 0
            
            if (timestampMatch != null) {
                val ts = timestampMatch.groupValues[1]
                withStyle(SpanStyle(color = TextGray)) {
                    append("[")
                    append(ts)
                    append("] ")
                }
                currentIdx = timestampMatch.range.last + 2
            }
            
            val tagRegex = Regex("""^\[([^\]]+)\]""")
            val tagMatch = tagRegex.find(line, currentIdx)
            if (tagMatch != null) {
                val tagContent = tagMatch.groupValues[1]
                val color = if (isSensitive) WarningRed else CyberTeal
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append("[")
                    append(tagContent)
                    append("] ")
                }
                currentIdx = tagMatch.range.last + 2
            }
            
            val remaining = line.substring(currentIdx)
            val appLabelIdx = remaining.indexOf("App:")
            val destLabelIdx = remaining.indexOf("Dest:")
            val arrowIdx = remaining.indexOf("->")
            
            if (appLabelIdx != -1 && destLabelIdx != -1 && arrowIdx != -1) {
                withStyle(SpanStyle(color = TextGray)) {
                    append("App: ")
                }
                
                val appSection = remaining.substring(appLabelIdx + 4, arrowIdx).trim()
                val pkgStartIdx = appSection.indexOf("(")
                val pkgEndIdx = appSection.indexOf(")")
                if (pkgStartIdx != -1 && pkgEndIdx != -1) {
                    val appName = appSection.substring(0, pkgStartIdx).trim()
                    val pkg = appSection.substring(pkgStartIdx, pkgEndIdx + 1)
                    
                    withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.Bold)) {
                        append(appName)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = TextGray)) {
                        append(pkg)
                    }
                } else {
                    withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.Bold)) {
                        append(appSection)
                    }
                }
                
                withStyle(SpanStyle(color = CyberBlue, fontWeight = FontWeight.Bold)) {
                    append(" -> ")
                }
                
                withStyle(SpanStyle(color = TextGray)) {
                    append("Dest: ")
                }
                val destContent = remaining.substring(destLabelIdx + 5).trim()
                withStyle(SpanStyle(color = TextWhite)) {
                    append(destContent)
                }
            } else {
                withStyle(SpanStyle(color = TextWhite)) {
                    append(remaining)
                }
            }
        } catch (e: Exception) {
            append(line)
        }
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
