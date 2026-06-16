package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.AppSelectorItem
import com.xprox.sentinel.ui.screens.VisualLogEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorDialog(
    showAppSelector: Boolean,
    activeSelectorApps: List<AppSelectorItem>,
    selectedAppPackage: String,
    allAppsText: String,
    logsList: List<VisualLogEntry>,
    logCounts: Map<String, Int>,
    onDismissRequest: () -> Unit,
    onAppSelected: (pkg: String, name: String) -> Unit
) {
    if (!showAppSelector) return

    Dialog(onDismissRequest = onDismissRequest) {
        var searchAppQuery by remember { mutableStateOf("") }
        
        val filteredSelectorApps = remember(activeSelectorApps, searchAppQuery) {
            if (searchAppQuery.isEmpty()) {
                activeSelectorApps
            } else {
                activeSelectorApps.filter {
                    it.appName.contains(searchAppQuery, ignoreCase = true) ||
                    it.packageName.contains(searchAppQuery, ignoreCase = true)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, CyberTeal),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            text = string("app_selector_title"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal
                        )
                        Text(
                            text = string("app_selector_desc"),
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

                // Sleek Cyber Search Input inside Dialog
                OutlinedTextField(
                    value = searchAppQuery,
                    onValueChange = { searchAppQuery = it },
                    placeholder = { Text(text = string("search_app_placeholder"), color = TextGray, fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        cursorColor = CyberTeal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // LazyColumn of applications
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    // "Все приложения" Option
                    item {
                        val totalLogs = logsList.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppSelected("all", allAppsText)
                                }
                                .background(
                                    if (selectedAppPackage == "all") CyberTeal.copy(alpha = 0.15f) else Color.Transparent
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
                                Text(text = "ALL", color = CyberTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = string("all_apps"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = string("all_traffic_desc"),
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                            if (totalLogs > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(CyberTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$totalLogs log",
                                        color = CyberTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    }

                    // Installed Apps & System Services
                    items(filteredSelectorApps) { app ->
                        val appLogsCount = logCounts[app.packageName] ?: 0
                        val isSelected = selectedAppPackage == app.packageName
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppSelected(app.packageName, app.appName)
                                }
                                .background(
                                    if (isSelected) CyberTeal.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App Icon
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DarkCard, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (app.isSystem) "SYS" else "APP",
                                        color = if (app.isSystem) WarningRed else CyberTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // App Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (appLogsCount > 0) CyberTeal else TextWhite
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }

                            // Glowing count badge if has active logs!
                            if (appLogsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(CyberTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+$appLogsCount",
                                        color = CyberTeal,
                                        fontSize = 10.sp,
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
