package com.xprox.sentinel.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = if (isSelected) ElectricViolet else TextGray.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = profile.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    val isDirect = profile.type.uppercase() == "DIRECT"
                    Text(
                        text = if (isDirect) {
                            if (LanguageManager.currentLanguage.value.code == "ru") "DIRECT • Режим анализа трафика" else "DIRECT • Traffic Analysis Mode"
                        } else {
                            "${profile.type} • ${profile.address}"
                        },
                        fontSize = 11.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDirect = profile.type.uppercase() == "DIRECT"
                if (isDirect) {
                    // No ping badge for direct
                } else if (isPinging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = ElectricViolet,
                        strokeWidth = 2.dp
                    )
                } else {
                    val pingColor = when {
                        pingMs == null -> TextGray.copy(alpha = 0.5f)
                        pingMs < 100 -> SecureGreen
                        pingMs < 300 -> WarningAmber
                        else -> WarningRose
                    }
                    val pingText = when {
                        pingMs == null -> "Ping"
                        else -> "$pingMs ms"
                    }
                    Text(
                        text = pingText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = pingColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPingClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
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
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp)) },
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
    }
}
