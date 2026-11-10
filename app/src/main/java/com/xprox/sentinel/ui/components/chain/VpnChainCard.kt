package com.xprox.sentinel.ui.components.chain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.theme.*

@Composable
fun VpnChainCard(
    isChainEnabled: Boolean,
    chainProfiles: List<XrayConfigManager.ServerProfile>,
    subscriptionNames: Map<String, String> = emptyMap(),
    onToggleChain: (Boolean) -> Unit,
    onAddNodeClick: () -> Unit,
    onChangeNodeClick: (index: Int) -> Unit,
    onMoveUp: (index: Int) -> Unit,
    onMoveDown: (index: Int) -> Unit,
    onDeleteNode: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChainEnabled) DarkCard else DarkCard.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.2.dp,
            if (isChainEnabled) ElectricViolet.copy(alpha = 0.6f) else CardBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isChainEnabled) ElectricViolet.copy(alpha = 0.2f) else Color(0x10FFFFFF),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "VPN Chain",
                            tint = if (isChainEnabled) ElectricViolet else TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "VPN-ЦЕПОЧКА (MULTI-HOP)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isChainEnabled) TextWhite else TextGray,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            if (isChainEnabled) {
                                Surface(
                                    color = SecureGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.6.dp, SecureGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${chainProfiles.size} ХОПА",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isChainEnabled) "Трафик дважды зашифрован через каскад узлов" else "Выключено: подключение через один узел",
                            fontSize = 10.5.sp,
                            color = TextGray
                        )
                    }
                }

                Switch(
                    checked = isChainEnabled,
                    onCheckedChange = onToggleChain,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = ElectricViolet,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = Color(0x18FFFFFF),
                        uncheckedBorderColor = CardBorder
                    )
                )
            }

            // Chain Details when Enabled
            AnimatedVisibility(
                visible = isChainEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.8.dp)

                    // Minimal nodes warning
                    if (chainProfiles.size < 2) {
                        Surface(
                            color = Color(0x20FF9800),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, Color(0x55FF9800)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Для работы цепочки выберите минимум 2 сервера (Входной и Выходной).",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFE0B2)
                                )
                            }
                        }
                    }

                    // Chain visual progression ribbon
                    if (chainProfiles.size >= 2) {
                        ChainRibbonFlow(chainProfiles = chainProfiles)
                    }

                    // Node Items List
                    chainProfiles.forEachIndexed { index, profile ->
                        val isEntry = index == 0
                        val isExit = index == chainProfiles.size - 1
                        val roleBadge = when {
                            isEntry -> "1. ВХОД (ENTRY)"
                            isExit -> "${index + 1}. ВЫХОД (EXIT)"
                            else -> "${index + 1}. РЕЛЕ (HOP ${index + 1})"
                        }
                        val roleColor = when {
                            isEntry -> CyberCyan
                            isExit -> SecureGreen
                            else -> ElectricViolet
                        }

                        val subTitle = profile.groupId?.let { subscriptionNames[it] }

                        ChainNodeRow(
                            index = index,
                            totalCount = chainProfiles.size,
                            profile = profile,
                            roleBadge = roleBadge,
                            roleColor = roleColor,
                            subscriptionName = subTitle,
                            onMoveUp = { onMoveUp(index) },
                            onMoveDown = { onMoveDown(index) },
                            onChange = { onChangeNodeClick(index) },
                            onDelete = { onDeleteNode(index) }
                        )
                    }

                    // Add node button
                    OutlinedButton(
                        onClick = onAddNodeClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add node",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (chainProfiles.isEmpty()) "ВЫБРАТЬ ВХОДНОЙ СЕРВЕР" else "ДОБАВИТЬ УЗЕЛ В ЦЕПОЧКУ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainRibbonFlow(chainProfiles: List<XrayConfigManager.ServerProfile>) {
    Surface(
        color = Color(0x08FFFFFF),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.6.dp, Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            chainProfiles.forEachIndexed { i, p ->
                val color = when {
                    i == 0 -> CyberCyan
                    i == chainProfiles.size - 1 -> SecureGreen
                    else -> ElectricViolet
                }
                Text(
                    text = p.name.take(12),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                if (i < chainProfiles.size - 1) {
                    Text(
                        text = "➔",
                        fontSize = 11.sp,
                        color = TextGray,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChainNodeRow(
    index: Int,
    totalCount: Int,
    profile: XrayConfigManager.ServerProfile,
    roleBadge: String,
    roleColor: Color,
    subscriptionName: String?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onChange: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0x0CFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left content: Role pill + Name & Type
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChange() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = roleColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.6.dp, roleColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = roleBadge,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (!subscriptionName.isNullOrEmpty()) {
                        Surface(
                            color = ElectricViolet.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, ElectricViolet.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = subscriptionName,
                                fontSize = 8.5.sp,
                                color = ElectricViolet,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.name.ifEmpty { "Без названия" },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    maxLines = 1
                )
                Text(
                    text = "${profile.type.uppercase()} • ${profile.address}:${profile.port}",
                    fontSize = 10.sp,
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (index > 0) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (index < totalCount - 1) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (totalCount > 2) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
