package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xprox.sentinel.data.string
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.log.LogManager

@Composable
fun SessionSelectorDialog(
    showSessionSelector: Boolean,
    sessionHistoryList: List<LogManager.SessionInfo>,
    selectedSessionIndex: Int,
    onDismissRequest: () -> Unit,
    onSessionSelected: (index: Int, name: String) -> Unit
) {
    if (!showSessionSelector) return

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, CyberTeal),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = string("session_history_title"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal
                        )
                        Text(
                            text = string("session_history_desc"),
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // LazyColumn of sessions
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    items(sessionHistoryList) { session ->
                        val isSelected = selectedSessionIndex == session.index
                        val sizeFormatted = if (session.exists) {
                            when {
                                session.sizeBytes < 1024 -> "${session.sizeBytes} B"
                                session.sizeBytes < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", session.sizeBytes / 1024.0)
                                else -> String.format(java.util.Locale.getDefault(), "%.1f MB", session.sizeBytes / (1024.0 * 1024.0))
                            }
                        } else {
                            string("no_file")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val sessionName = if (session.index == 0) {
                                        LanguageManager.getString("active_session_name")
                                    } else {
                                        "${LanguageManager.getString("prev_session_name")} ${session.index}"
                                    }
                                    onSessionSelected(session.index, sessionName)
                                }
                                .background(
                                    if (isSelected) CyberTeal.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkCard, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (session.index == 0) "ACT" else "S${session.index}",
                                    color = if (session.index == 0) CyberTeal else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (session.index == 0) string("active_session_name") else "${string("prev_session_name")} ${session.index}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) CyberTeal else TextWhite
                                )
                                Text(
                                    text = "${string("size")}: $sizeFormatted | ${string("lines")}: ${session.logCount}",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                            if (session.index == 0) {
                                Box(
                                    modifier = Modifier
                                        .background(CyberTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = CyberTeal,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
