package com.xprox.sentinel.ui.screens.profiles
 
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 
        // Segmented Pill Toggle Control for Bypass Mode
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(42.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    true to string("bypass_mode"),
                    false to string("selection_mode")
                ).forEach { (mode, label) ->
                    val isSelected = isBypassMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyberTeal else Color.Transparent)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onBypassModeChange(mode) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBg else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
 
        // Double-Bezel List Container
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(CyberTeal.copy(alpha = 0.35f), CyberBlue.copy(alpha = 0.05f))
                )
            )
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp)),
                border = BorderStroke(0.5.dp, CardBorder)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    if (installedApps.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CyberTeal)
                            }
                        }
                    } else if (filteredApps.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
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
    }
}
