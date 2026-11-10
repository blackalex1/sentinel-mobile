package com.xprox.sentinel.ui.components.chain

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.theme.*

@Composable
fun ChainServerPickerDialog(
    title: String,
    profiles: List<XrayConfigManager.ServerProfile>,
    subscriptionNames: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onSelectProfile: (XrayConfigManager.ServerProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isBlank()) {
            profiles.filter { it.type.uppercase() != "DIRECT" && it.address.isNotEmpty() }
        } else {
            val q = searchQuery.trim().lowercase()
            profiles.filter {
                it.type.uppercase() != "DIRECT" &&
                it.address.isNotEmpty() &&
                (it.name.lowercase().contains(q) ||
                 it.address.lowercase().contains(q) ||
                 it.type.lowercase().contains(q))
            }
        }
    }

    // Group profiles by subscription groupId (null means manual)
    val manualProfiles = remember(filteredProfiles) {
        filteredProfiles.filter { it.groupId == null }
    }
    val subscriptionGroups = remember(filteredProfiles) {
        filteredProfiles.filter { it.groupId != null }.groupBy { it.groupId!! }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Поиск сервера...",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Servers list grouped
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Subscription Groups
                    subscriptionGroups.forEach { (groupId, groupProfiles) ->
                        val subTitle = subscriptionNames[groupId] ?: "Подписка"
                        item(key = "sub_header_$groupId") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = subTitle.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    color = ElectricViolet.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.6.dp, ElectricViolet.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "${groupProfiles.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        items(groupProfiles, key = { "sub_${groupId}_${it.id}" }) { profile ->
                            ServerPickerItem(
                                profile = profile,
                                badgeText = subTitle,
                                badgeColor = ElectricViolet,
                                onClick = { onSelectProfile(profile) }
                            )
                        }
                    }

                    // 2. Manual Connections
                    if (manualProfiles.isNotEmpty()) {
                        item(key = "manual_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "РУЧНЫЕ СОЕДИНЕНИЯ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        items(manualProfiles, key = { "manual_${it.id}" }) { profile ->
                            ServerPickerItem(
                                profile = profile,
                                badgeText = "Ручное",
                                badgeColor = CyberCyan,
                                onClick = { onSelectProfile(profile) }
                            )
                        }
                    }

                    if (filteredProfiles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Серверы не найдены",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerPickerItem(
    profile: XrayConfigManager.ServerProfile,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0x0CFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.name.ifEmpty { "Без названия" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${profile.type.uppercase()} • ${profile.address}:${profile.port}",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Surface(
                color = badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
