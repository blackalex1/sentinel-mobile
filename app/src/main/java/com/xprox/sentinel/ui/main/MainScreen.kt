package com.xprox.sentinel.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.xprox.sentinel.MainActivity
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.SentinelPairingManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.CosmicBackground
import com.xprox.sentinel.ui.components.DoppelrandCard
import com.xprox.sentinel.ui.screens.DashboardScreen
import com.xprox.sentinel.ui.screens.ProfilesScreen
import com.xprox.sentinel.ui.screens.SettingsScreen
import com.xprox.sentinel.ui.screens.TrafficLogsScreen

sealed class Tab(val key: String, val icon: ImageVector) {
    object Dashboard : Tab("tab_dashboard", Icons.Default.Home)
    object Profiles : Tab("tab_profiles", Icons.Default.Info)
    object Logs : Tab("tab_logs", Icons.AutoMirrored.Filled.List)
    object Settings : Tab("tab_settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf<Tab>(Tab.Dashboard) }

    val activePairingRequest by SentinelPairingManager.activePairingRequest.collectAsState()
    val showDisconnectConfirm by MainActivity.showDisconnectConfirmFlow.collectAsState()

    CosmicBackground {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 2.dp)
                ) {
                    Surface(
                        color = Color(0xF00D0D18),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(Tab.Dashboard, Tab.Profiles, Tab.Logs, Tab.Settings)
                            tabs.forEach { tab ->
                                val isSelected = selectedTab == tab

                                val iconTint by animateColorAsState(
                                    targetValue = if (isSelected) ElectricViolet else TextGray,
                                    label = "iconTint"
                                )

                                val bgPillColor by animateColorAsState(
                                    targetValue = if (isSelected) ElectricViolet.copy(alpha = 0.16f) else Color.Transparent,
                                    label = "bgPillColor"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 2.dp, horizontal = 2.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bgPillColor)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { selectedTab = tab }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = string(tab.key),
                                            tint = iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = string(tab.key),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = iconTint,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    Tab.Dashboard -> DashboardScreen(onNavigateToSettings = { selectedTab = Tab.Settings })
                    Tab.Profiles -> ProfilesScreen()
                    Tab.Logs -> TrafficLogsScreen()
                    Tab.Settings -> SettingsScreen()
                }
            }
        }

        // Interactive PC Pairing Confirmation Modal Dialog
        activePairingRequest?.let { req ->
            AlertDialog(
                onDismissRequest = { SentinelPairingManager.rejectCurrent() },
                containerColor = DarkCardElevated,
                title = {
                    Text(
                        text = "🖥️ ЗАПРОС НА СОПРЯЖЕНИЕ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Компьютер '${req.clientName}' запрашивает защищенное подключение к VPN и передачу настроек прокси.",
                            fontSize = 13.sp,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "КОД ПОДТВЕРЖДЕНИЯ",
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = req.pinCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { SentinelPairingManager.approveCurrent() },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureGreen)
                    ) {
                        Text(
                            text = "РАЗРЕШИТЬ",
                            color = DarkBg,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { SentinelPairingManager.rejectCurrent() }) {
                        Text(
                            text = "ОТКЛОНИТЬ",
                            color = WarningRose,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }
    }
}
