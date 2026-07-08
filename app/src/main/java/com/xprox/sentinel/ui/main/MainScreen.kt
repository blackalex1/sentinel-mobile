package com.xprox.sentinel.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.xprox.sentinel.ui.screens.DashboardScreen
import com.xprox.sentinel.ui.screens.ProfilesScreen
import com.xprox.sentinel.ui.screens.SettingsScreen
import com.xprox.sentinel.ui.screens.TrafficLogsScreen
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string

sealed class Tab(val key: String, val icon: ImageVector) {
    object Dashboard : Tab("tab_dashboard", Icons.Default.Home)
    object Profiles : Tab("tab_profiles", Icons.Default.Info)
    object Logs : Tab("tab_logs", Icons.Default.List)
    object Settings : Tab("tab_settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf<Tab>(Tab.Dashboard) }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
            ) {
                Surface(
                    color = DarkCard.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(64.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(Tab.Dashboard, Tab.Profiles, Tab.Logs, Tab.Settings)
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
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
                                        tint = if (isSelected) CyberTeal else TextGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = string(tab.key),
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                        color = if (isSelected) CyberTeal else TextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
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
