package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun NetworkRoutingPanel(
    context: Context,
    customDirectRules: List<String>,
    customProxyRules: List<String>,
    customBlockRules: List<String>,
    geoIpRules: Set<String>,
    geoSiteRules: Set<String>,
    onAddCustomRule: (target: String, rule: String) -> Unit,
    onRemoveCustomRule: (target: String, rule: String) -> Unit,
    onGeoIpRuleToggle: (preset: String, isChecked: Boolean) -> Unit,
    onGeoSiteRuleToggle: (preset: String, isChecked: Boolean) -> Unit,
    onGeoIpRuleRemove: (rule: String) -> Unit,
    onGeoSiteRuleRemove: (rule: String) -> Unit
) {
    var selectedTarget by remember { mutableStateOf("Direct") } // Direct, Proxy, Block
    var customInputText by remember { mutableStateOf("") }
    var customGeoTagInput by remember { mutableStateOf("") }

    val geoIpPresets = listOf("geoip:private", "geoip:ru", "geoip:cn", "geoip:us")
    val geoSitePresets = listOf(
        "geosite:google",
        "geosite:category-ads-all",
        "geosite:youtube",
        "geosite:netflix",
        "geosite:instagram",
        "geosite:facebook",
        "geosite:twitter"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Add Custom Domain/IP Rule
        item {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(CyberTeal.copy(alpha = 0.35f), CyberPurple.copy(alpha = 0.05f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ПОЛЬЗОВАТЕЛЬСКИЕ ПРАВИЛА (DOMAINS / IPS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Action Segmented Selector (Direct / Proxy / Block)
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                listOf(
                                    "Direct" to string("custom_target_direct"),
                                    "Proxy" to string("custom_target_proxy"),
                                    "Block" to string("custom_target_block")
                                ).forEach { (targetKey, targetLabel) ->
                                    val isSelected = selectedTarget == targetKey
                                    val targetColor = when (targetKey) {
                                        "Direct" -> CyberTeal
                                        "Proxy" -> CyberBlue
                                        else -> WarningRed
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) targetColor else Color.Transparent)
                                            .clickable { selectedTarget = targetKey }
                                    ) {
                                        Text(
                                            text = targetLabel,
                                            color = if (isSelected) DarkBg else TextWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input field + Add button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customInputText,
                                onValueChange = { customInputText = it },
                                placeholder = { Text(text = string("add_custom_domain_or_ip"), color = TextGray, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = CyberTeal,
                                    unfocusedBorderColor = CardBorder,
                                    focusedContainerColor = DarkCard,
                                    unfocusedContainerColor = DarkCard,
                                    cursorColor = CyberTeal
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (customInputText.isNotBlank()) {
                                        onAddCustomRule(selectedTarget, customInputText.trim())
                                        customInputText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = DarkBg)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active Custom Rules Lists
                        CustomRuleGroupList(
                            title = "Напрямую (Direct)",
                            rules = customDirectRules,
                            badgeColor = CyberTeal,
                            onRemove = { rule -> onRemoveCustomRule("Direct", rule) }
                        )

                        CustomRuleGroupList(
                            title = "Через VPN (Proxy)",
                            rules = customProxyRules,
                            badgeColor = CyberBlue,
                            onRemove = { rule -> onRemoveCustomRule("Proxy", rule) }
                        )

                        CustomRuleGroupList(
                            title = "Заблокировано (Block)",
                            rules = customBlockRules,
                            badgeColor = WarningRed,
                            onRemove = { rule -> onRemoveCustomRule("Block", rule) }
                        )

                    }
                }
            }
        }

        // Section 2: GeoIP Presets & Tags
        item {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = string("geoip_section_title"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        geoIpPresets.forEach { preset ->
                            val isChecked = geoIpRules.contains(preset)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGeoIpRuleToggle(preset, !isChecked) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = preset, color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked -> onGeoIpRuleToggle(preset, checked) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberTeal,
                                        checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                                        uncheckedThumbColor = TextGray,
                                        uncheckedTrackColor = CardBorder
                                    )
                                )
                            }
                            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Section 3: GeoSite Presets & Tags
        item {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = string("geosite_section_title"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        geoSitePresets.forEach { preset ->
                            val isChecked = geoSiteRules.contains(preset)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGeoSiteRuleToggle(preset, !isChecked) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = preset, color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked -> onGeoSiteRuleToggle(preset, checked) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberTeal,
                                        checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                                        uncheckedThumbColor = TextGray,
                                        uncheckedTrackColor = CardBorder
                                    )
                                )
                            }
                            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRuleGroupList(
    title: String,
    rules: List<String>,
    badgeColor: Color,
    onRemove: (String) -> Unit
) {
    if (rules.isEmpty()) return

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextGray)
        }

        Spacer(modifier = Modifier.height(6.dp))

        rules.forEach { rule ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(0.5.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = rule, color = TextWhite, fontSize = 12.sp)
                    IconButton(onClick = { onRemove(rule) }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove", tint = WarningRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
