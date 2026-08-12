package com.xprox.sentinel.ui.screens.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.AppInfo

enum class AppRouteAction(val label: String, val color: Color) {
    PROXY("PROXY", ElectricViolet),
    DIRECT("DIRECT", SecureGreen),
    BLOCK("BLOCK", WarningRose)
}

@Composable
fun AppRoutingRow(
    app: AppInfo,
    action: AppRouteAction,
    onActionSelect: (AppRouteAction) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkCardElevated, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = app.appName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 10.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Dropdown Action Badge Button (PROXY / DIRECT / BLOCK)
            Box {
                Surface(
                    color = action.color.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, action.color.copy(alpha = 0.45f)),
                    modifier = Modifier.clickable { menuExpanded = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = action.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = action.color,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Action",
                            tint = action.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkCardElevated)
                ) {
                    AppRouteAction.values().forEach { act ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = act.label,
                                    color = act.color,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = {
                                onActionSelect(act)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
    }
}
