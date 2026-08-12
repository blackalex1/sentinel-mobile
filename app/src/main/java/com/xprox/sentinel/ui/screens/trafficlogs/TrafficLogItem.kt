package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isLineSensitive) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (isLineSensitive) "Alert" else "Info",
                tint = if (isLineSensitive) WarningRose else SecureGreen,
                modifier = Modifier
                    .size(13.dp)
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
        HorizontalDivider(color = CardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
    }
}

@Composable
fun XrayLogItem(line: String) {
    val highlightedText = renderHighlightedXrayLog(line)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
        }
        HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}

fun renderHighlightedXrayLog(line: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        try {
            // Match pattern: 2026/08/12 16:13:15.377226 [Info] [4049831113] proxy/freedom: ...
            val regex = Regex("""^(\d{4}/\d{2}/\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?)\s+\[([^\]]+)\](?:\s+\[([^\]]+)\])?\s*(.*)$""")
            val match = regex.find(line)

            if (match != null) {
                val timestamp = match.groupValues[1]
                val level = match.groupValues[2]
                val connId = match.groupValues[3]
                val rest = match.groupValues[4]

                // Timestamp
                withStyle(SpanStyle(color = TextGray)) {
                    append(timestamp)
                    append(" ")
                }

                // Level Tag [Info], [Warning], [Error]
                val levelColor = when (level.lowercase()) {
                    "info" -> CyberCyan
                    "warning", "warn" -> WarningAmber
                    "error", "fatal" -> WarningRose
                    else -> ElectricViolet
                }
                withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) {
                    append("[")
                    append(level)
                    append("] ")
                }

                // Conn ID [4049831113]
                if (connId.isNotEmpty()) {
                    withStyle(SpanStyle(color = ElectricViolet, fontWeight = FontWeight.Bold)) {
                        append("[")
                        append(connId)
                        append("] ")
                    }
                }

                // Rest of log body with keywords highlighted
                formatXrayBody(rest)
            } else {
                formatXrayBody(line)
            }
        } catch (e: Exception) {
            append(line)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.formatXrayBody(text: String) {
    var remaining = text

    // Highlight protocols e.g. proxy/freedom:, proxy/socks:, transport/internet/tcp:
    val protoRegex = Regex("""^(proxy/[a-z0-9_-]+:|transport/[a-z0-9_/]+:|app/[a-z0-9_-]+:)""")
    val protoMatch = protoRegex.find(remaining)
    if (protoMatch != null) {
        val proto = protoMatch.groupValues[1]
        withStyle(SpanStyle(color = SecureGreen, fontWeight = FontWeight.Bold)) {
            append(proto)
            append(" ")
        }
        remaining = remaining.substring(proto.length).trimStart()
    }

    // Highlight key verbs & endpoints
    val keywords = listOf(
        "connection opened to", "accepted", "taking detour", "dialing TCP to",
        "TCP Connect request to", "closed", "direct", "proxy", "block"
    )

    var cursor = 0
    while (cursor < remaining.length) {
        var foundMatch = false
        for (kw in keywords) {
            if (remaining.startsWith(kw, ignoreCase = true, startIndex = cursor)) {
                val kwColor = when (kw.lowercase()) {
                    "connection opened to", "accepted", "dialing tcp to" -> CyberCyan
                    "taking detour", "proxy" -> ElectricViolet
                    "direct" -> SecureGreen
                    "block", "closed" -> WarningRose
                    else -> TextWhite
                }
                withStyle(SpanStyle(color = kwColor, fontWeight = FontWeight.Bold)) {
                    append(remaining.substring(cursor, cursor + kw.length))
                }
                cursor += kw.length
                foundMatch = true
                break
            }
        }

        if (!foundMatch) {
            val char = remaining[cursor]
            if (char == 't' && remaining.startsWith("tcp:", cursor)) {
                withStyle(SpanStyle(color = SecureGreen)) {
                    append("tcp:")
                }
                cursor += 4
            } else {
                append(char)
                cursor++
            }
        }
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
                val color = if (isSensitive) WarningRose else CyberCyan
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
                
                withStyle(SpanStyle(color = ElectricViolet, fontWeight = FontWeight.Bold)) {
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
