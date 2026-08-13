package com.xprox.sentinel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerProfileCard(
    profile: XrayConfigManager.ServerProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onExport: () -> Unit,
    pingMs: Int? = null,
    isPinging: Boolean = false,
    onPingClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = if (LanguageManager.currentLanguage.value.code == "ru") "Удалить подключение?" else "Delete Connection?",
                    fontWeight = FontWeight.Bold,
                    color = WarningRose
                )
            },
            text = {
                Text(
                    text = if (LanguageManager.currentLanguage.value.code == "ru") {
                        "Вы действительно хотите навсегда удалить подключение \"${profile.name}\"?"
                    } else {
                        "Are you sure you want to permanently delete connection \"${profile.name}\"?"
                    },
                    color = TextWhite,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRose)
                ) {
                    Text(
                        text = if (LanguageManager.currentLanguage.value.code == "ru") "Удалить" else "Delete",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(
                        text = if (LanguageManager.currentLanguage.value.code == "ru") "Отмена" else "Cancel",
                        color = TextGray
                    )
                }
            },
            containerColor = DarkCardElevated,
            tonalElevation = 8.dp
        )
    }

    DoppelrandCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect() },
                onLongClick = { menuExpanded = true }
            ),
        borderColor = if (isSelected) ElectricViolet else DoppelrandShellBorder,
        glowColor = if (isSelected) ElectricViolet else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1 (Top Level): [CheckIcon] [Server Name] --------- [Protocol Badge] [Ping Pill] [⋮]
            // Protocol badge and Ping pill are on the EXACT SAME HORIZONTAL LEVEL!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Selected Check Icon + Profile Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = if (isSelected) ElectricViolet else TextGray.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextWhite else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: [Protocol Badge] + [Ping Pill] + [More Options]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Protocol Badge Pill
                    val isDirect = profile.type.uppercase() == "DIRECT"
                    Surface(
                        color = (if (isSelected) ElectricViolet else TextGray).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.8.dp, (if (isSelected) ElectricViolet else TextGray).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isDirect) "DIRECT" else profile.type.uppercase(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) CyberCyan else TextGray,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Latency / Ping Pill (when not direct)
                    if (!isDirect) {
                        if (isPinging) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .height(24.dp)
                                    .padding(horizontal = 6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = CyberCyan,
                                    strokeWidth = 1.8.dp
                                )
                            }
                        } else {
                            val pingColor = when {
                                pingMs == null -> TextGray
                                pingMs < 80 -> SecureGreen
                                pingMs < 180 -> CyberCyan
                                pingMs < 300 -> WarningAmber
                                else -> WarningRose
                            }

                            Surface(
                                color = pingColor.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, pingColor.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .clickable { onPingClick() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (pingMs != null) {
                                        Icon(
                                            imageVector = Icons.Default.SignalCellularAlt,
                                            contentDescription = "Signal",
                                            tint = pingColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = if (pingMs == null) "Ping" else "${pingMs}ms",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pingColor,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // 3-Dots Options Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextGray,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.sizeIn(minWidth = 160.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (LanguageManager.currentLanguage.value.code == "ru") "Поделиться" else "Share", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onExport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (LanguageManager.currentLanguage.value.code == "ru") "Дублировать" else "Clone", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onClone()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (LanguageManager.currentLanguage.value.code == "ru") "Редактировать" else "Edit", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (LanguageManager.currentLanguage.value.code == "ru") "Удалить" else "Delete", color = WarningRose) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WarningRose, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmation = true
                                }
                            )
                        }
                    }
                }
            }

            // Row 2 (Bottom Level): [Indent] [Host / IP Address (Port hidden for cleaner stealth UI)]
            val isDirect = profile.type.uppercase() == "DIRECT"
            val cleanAddr = profile.address.trim()
            val rawHost = when {
                cleanAddr.isNotEmpty() -> cleanAddr
                profile.sni.isNotEmpty() -> profile.sni.trim()
                profile.host.isNotEmpty() -> profile.host.trim()
                else -> ""
            }
            val hostDisplay = when {
                isDirect -> if (LanguageManager.currentLanguage.value.code == "ru") "DIRECT • Режим анализа трафика" else "DIRECT • Traffic Analysis Mode"
                rawHost.isNotEmpty() -> {
                    val colonIdx = rawHost.lastIndexOf(':')
                    if (colonIdx != -1 && colonIdx > rawHost.lastIndexOf(']')) {
                        rawHost.substring(0, colonIdx)
                    } else {
                        rawHost
                    }
                }
                else -> "—"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(26.dp))
                Text(
                    text = hostDisplay,
                    fontSize = 11.sp,
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}
