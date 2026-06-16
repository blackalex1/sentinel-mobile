package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoutingPanel(
    context: Context,
    installedApps: List<AppInfo>,
    filteredApps: List<AppInfo>,
    allowedApps: Set<String>,
    isBypassMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBypassModeChange: (Boolean) -> Unit,
    onAllowedAppToggle: (pkgName: String, isChecked: Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = string("routing_settings"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Cyber Segmented Toggle Bar for App Bypass Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBypassMode) CyberTeal.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable {
                        onBypassModeChange(true)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = string("bypass_mode"),
                    color = if (isBypassMode) CyberTeal else TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isBypassMode) CyberTeal.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable {
                        onBypassModeChange(false)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = string("selection_mode"),
                    color = if (!isBypassMode) CyberTeal else TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Cyber search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(text = string("search_apps"), color = TextGray, fontSize = 14.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                cursorColor = CyberTeal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(10.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(8.dp)
        ) {
            if (installedApps.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberTeal)
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = string("nothing_found"), color = TextGray, fontSize = 14.sp)
                    }
                }
            } else {
                items(filteredApps) { app ->
                    val isChecked = allowedApps.contains(app.packageName)
                    AppRoutingRow(
                        app = app,
                        isChecked = isChecked,
                        onCheckedChange = { checked ->
                            onAllowedAppToggle(app.packageName, checked)
                        }
                    )
                }
            }
        }
    }
}
