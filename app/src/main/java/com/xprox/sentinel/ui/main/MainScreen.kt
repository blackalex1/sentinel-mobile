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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.CosmicBackground
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

    CosmicBackground {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        color = DarkCardElevated.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(64.dp)
                            .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
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
                                    targetValue = if (isSelected) ElectricViolet.copy(alpha = 0.15f) else Color.Transparent,
                                    label = "bgPillColor"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
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
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = string(tab.key),
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = iconTint
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
    }
}
