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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun NetworkRoutingPanel(
    context: Context,
    geoIpRules: Set<String>,
    geoSiteRules: Set<String>,
    customGeoIpInput: String,
    customGeoSiteInput: String,
    onCustomGeoIpInputChange: (String) -> Unit,
    onCustomGeoSiteInputChange: (String) -> Unit,
    onGeoIpRuleToggle: (preset: String, isChecked: Boolean) -> Unit,
    onGeoSiteRuleToggle: (preset: String, isChecked: Boolean) -> Unit,
    onCustomGeoIpAdd: () -> Unit,
    onCustomGeoSiteAdd: () -> Unit,
    onGeoIpRuleRemove: (customRule: String) -> Unit,
    onGeoSiteRuleRemove: (customRule: String) -> Unit
) {
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

    @Composable
    fun getGeoIpPresetLabel(preset: String): String {
        return when (preset) {
            "geoip:private" -> string("preset_local_ips")
            "geoip:ru" -> string("preset_ru_ips")
            "geoip:cn" -> string("preset_cn_ips")
            "geoip:us" -> string("preset_us_ips")
            else -> preset
        }
    }

    @Composable
    fun getGeoSitePresetLabel(preset: String): String {
        return when (preset) {
            "geosite:google" -> string("preset_google")
            "geosite:category-ads-all" -> string("preset_ads")
            "geosite:youtube" -> string("preset_youtube")
            "geosite:netflix" -> string("preset_netflix")
            "geosite:instagram" -> string("preset_instagram")
            "geosite:facebook" -> string("preset_facebook")
            "geosite:twitter" -> string("preset_twitter")
            else -> preset
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: GeoIP Routing rules
        item {
            Text(
                text = string("geoip_section_title"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Preset rules list
        items(geoIpPresets) { preset ->
            val isChecked = geoIpRules.contains(preset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGeoIpRuleToggle(preset, !isChecked)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = getGeoIpPresetLabel(preset), color = TextWhite, fontSize = 13.sp)
                Switch(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        onGeoIpRuleToggle(preset, checked)
                    },
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

        // Custom GeoIP Rules Chips
        val customGeoIpRules = geoIpRules.filter { !geoIpPresets.contains(it) }
        if (customGeoIpRules.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    customGeoIpRules.forEach { customRule ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBg),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = customRule, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                IconButton(
                                    onClick = {
                                        onGeoIpRuleRemove(customRule)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove", tint = WarningRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Custom GeoIP rule
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customGeoIpInput,
                    onValueChange = onCustomGeoIpInputChange,
                    placeholder = { Text(text = string("add_custom_rule"), color = TextGray, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        cursorColor = CyberTeal
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCustomGeoIpAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(text = string("btn_add"), color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 2: GeoSite Routing rules
        item {
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = string("geosite_section_title"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Preset geosite rules list
        items(geoSitePresets) { preset ->
            val isChecked = geoSiteRules.contains(preset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGeoSiteRuleToggle(preset, !isChecked)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = getGeoSitePresetLabel(preset), color = TextWhite, fontSize = 13.sp)
                Switch(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        onGeoSiteRuleToggle(preset, checked)
                    },
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

        // Custom GeoSite Rules Chips
        val customGeoSiteRules = geoSiteRules.filter { !geoSitePresets.contains(it) }
        if (customGeoSiteRules.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    customGeoSiteRules.forEach { customRule ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBg),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = customRule, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                IconButton(
                                    onClick = {
                                        onGeoSiteRuleRemove(customRule)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Remove", tint = WarningRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Custom GeoSite rule
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customGeoSiteInput,
                    onValueChange = onCustomGeoSiteInputChange,
                    placeholder = { Text(text = string("add_custom_rule"), color = TextGray, fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        cursorColor = CyberTeal
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onCustomGeoSiteAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(text = string("btn_add"), color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
