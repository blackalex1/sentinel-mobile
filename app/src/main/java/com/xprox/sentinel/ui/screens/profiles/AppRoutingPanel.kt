package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.DoppelrandCard
import com.xprox.sentinel.ui.screens.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoutingPanel(
    context: Context,
    installedApps: List<AppInfo>,
    filteredApps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"
    val prefs = remember { context.getSharedPreferences("sentinel_app_routing_actions", Context.MODE_PRIVATE) }

    var appActions by remember {
        mutableStateOf(
            installedApps.associate { app ->
                val saved = prefs.getString(app.packageName, "PROXY") ?: "PROXY"
                app.packageName to AppRouteAction.valueOf(saved)
            }
        )
    }

    fun setAppAction(pkgName: String, action: AppRouteAction) {
        val next = appActions.toMutableMap()
        next[pkgName] = action
        appActions = next
        prefs.edit().putString(pkgName, action.name).apply()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (isRu) "РАЗДЕЛЬНОЕ ТУННЕЛИРОВАНИЕ ПО ПРИЛОЖЕНИЯМ" else "SPLIT TUNNELING BY APPLICATIONS",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricViolet,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Cyber search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(text = string("search_apps"), color = TextGray, fontSize = 12.sp) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextWhite, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = DarkCardElevated,
                unfocusedContainerColor = DarkCardElevated,
                cursorColor = CyberCyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(10.dp)
        )

        // Double-Bezel List Container
        DoppelrandCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            borderColor = ElectricViolet.copy(alpha = 0.35f),
            contentPadding = PaddingValues(4.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                if (installedApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberCyan)
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = string("nothing_found"), color = TextGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(filteredApps) { app ->
                        val action = appActions[app.packageName] ?: AppRouteAction.PROXY
                        AppRoutingRow(
                            app = app,
                            action = action,
                            onActionSelect = { selectedAction ->
                                setAppAction(app.packageName, selectedAction)
                            }
                        )
                    }
                }
            }
        }
    }
}
