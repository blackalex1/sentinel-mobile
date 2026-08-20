package com.xprox.sentinel.ui.screens.profiles

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.xprox.sentinel.core.SentinelCore
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.service.VpnManagerService
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.DoppelrandCard

data class RoutingTableRule(
    val id: String,
    val name: String,
    val conditions: String,
    var action: String, // BLOCKED, DIRECT, VPN
    var enabled: Boolean
)

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
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"
    var selectedTarget by remember { mutableStateOf("Direct") } // Direct, Proxy, Block
    var customInputText by remember { mutableStateOf("") }

    val prefs = remember { context.getSharedPreferences("sentinel_routing_priority_prefs", Context.MODE_PRIVATE) }

    fun getCleanRuleName(id: String, name: String): String {
        if (id == "1" || name.contains("BitTorrent", ignoreCase = true)) {
            return if (isRu) "BitTorrent трафик" else "BitTorrent Traffic"
        }
        if (id == "2" || name.contains("определения IP", ignoreCase = true) || name.contains("IP Services", ignoreCase = true) || name.contains("IP Checkers", ignoreCase = true)) {
            return if (isRu) "Определение IP" else "IP Checkers"
        }
        if (id == "3" || name.contains("RU Sites", ignoreCase = true) || name.contains("России", ignoreCase = true)) {
            return if (isRu) "Сайты России (RU)" else "RU Sites"
        }
        if (id == "4" || name.contains("Local", ignoreCase = true) || name.contains("Локальн", ignoreCase = true)) {
            return if (isRu) "Локальная сеть (LAN)" else "Local Private IPs"
        }
        return name
    }

    fun loadSavedTableRules(): List<RoutingTableRule> {
        val defaultList = listOf(
            RoutingTableRule("1", if (isRu) "BitTorrent трафик" else "BitTorrent Traffic", "BitTorrent", "BLOCKED", true),
            RoutingTableRule("2", if (isRu) "Определение IP" else "IP Checkers", "IP Checkers", "DIRECT", true),
            RoutingTableRule("3", if (isRu) "Сайты России (RU)" else "RU Sites", "RU Sites", "DIRECT", true),
            RoutingTableRule("4", if (isRu) "Локальная сеть (LAN)" else "Local Private IPs", "Local IPs", "DIRECT", true)
        )
        val jsonStr = prefs.getString("table_rules_json", null) ?: return defaultList
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<RoutingTableRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "$i")
                val name = obj.optString("name", "")
                val cond = obj.optString("conditions", "")
                val action = obj.optString("action", "DIRECT")
                val enabled = obj.optBoolean("enabled", true)
                list.add(
                    RoutingTableRule(
                        id = id,
                        name = getCleanRuleName(id, name),
                        conditions = cond,
                        action = action,
                        enabled = enabled
                    )
                )
            }
            if (list.isEmpty()) defaultList else list
        } catch (e: Exception) {
            defaultList
        }
    }

    var tableRules by remember { mutableStateOf(loadSavedTableRules()) }

    fun saveTableRules(rules: List<RoutingTableRule>) {
        tableRules = rules
        val jsonArray = org.json.JSONArray()
        rules.forEach { rule ->
            val obj = org.json.JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("conditions", rule.conditions)
                put("action", rule.action)
                put("enabled", rule.enabled)
            }
            jsonArray.put(obj)

            if (rule.id == "2" || rule.name.contains("определения IP", ignoreCase = true) || rule.name.contains("IP Services", ignoreCase = true)) {
                context.getSharedPreferences("sentinel_quick_actions_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("enabled_ip_service", rule.enabled)
                    .putString("action_ip_service", rule.action)
                    .apply()
            }
        }
        prefs.edit().putString("table_rules_json", jsonArray.toString()).apply()
        val intent = android.content.Intent(context, VpnManagerService::class.java).apply {
            action = VpnManagerService.ACTION_RELOAD_CONFIG
        }
        context.startService(intent)
    }

    // Interactive Smooth Drag and Drop State
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun moveRuleUp(index: Int) {
        if (index > 0) {
            val list = tableRules.toMutableList()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            saveTableRules(list)
        }
    }

    fun moveRuleDown(index: Int) {
        if (index < tableRules.size - 1) {
            val list = tableRules.toMutableList()
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            saveTableRules(list)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Routing Priority Table Cards with Elevated Drag & Drop
        item {
            DoppelrandCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ElectricViolet.copy(alpha = 0.35f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = if (isRu) "🛡️ ТАБЛИЦА МАРШРУТИЗАЦИИ (ПРИОРИТЕТ СВЕРХУ ВНИЗ)" else "🛡️ ROUTING TABLE (TOP-DOWN PRIORITY)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isRu) "Зажмите :: для плавного перетаскивания или используйте ▲ ▼" else "Press & hold :: to drag smoothly or use ▲ ▼",
                    fontSize = 11.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Cards Stack with Animated Lift & Smooth Drag
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tableRules.forEachIndexed { index, rule ->
                        var ruleMenuExpanded by remember { mutableStateOf(false) }

                        val isDraggingThis = (draggingIndex == index)
                        val scale by animateFloatAsState(if (isDraggingThis) 1.04f else 1.0f, label = "scale")
                        val elevation by animateDpAsState(if (isDraggingThis) 12.dp else 0.dp, label = "elevation")

                        val actionColor = when (rule.action) {
                            "BLOCKED" -> WarningRose
                            "DIRECT" -> SecureGreen
                            else -> ElectricViolet
                        }

                        Surface(
                            color = if (isDraggingThis) DarkCardElevated.copy(alpha = 0.95f) else DarkCardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isDraggingThis) 2.dp else 1.dp,
                                color = if (isDraggingThis) ElectricViolet else if (rule.enabled) actionColor.copy(alpha = 0.35f) else CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDraggingThis) 10f else 1f)
                                .graphicsLayer {
                                    translationY = if (isDraggingThis) dragOffsetY else 0f
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation.toPx()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag Handle :: Icon
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Drag Handle",
                                    tint = if (isDraggingThis) ElectricViolet else TextWhite.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .padding(end = 4.dp)
                                        .pointerInput(tableRules.size) {
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    draggingIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val currentIdx = draggingIndex ?: return@detectVerticalDragGestures
                                                    dragOffsetY += dragAmount

                                                    val thresholdPx = 140f
                                                    if (dragOffsetY < -thresholdPx && currentIdx > 0) {
                                                        moveRuleUp(currentIdx)
                                                        draggingIndex = currentIdx - 1
                                                        dragOffsetY += thresholdPx
                                                    } else if (dragOffsetY > thresholdPx && currentIdx < tableRules.size - 1) {
                                                        moveRuleDown(currentIdx)
                                                        draggingIndex = currentIdx + 1
                                                        dragOffsetY -= thresholdPx
                                                    }
                                                }
                                            )
                                        }
                                )

                                // Priority Index (#1)
                                Text(
                                    text = "#${index + 1}",
                                    color = ElectricViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 6.dp)
                                )

                                // Up / Down Reorder Arrows
                                Column(
                                    modifier = Modifier.padding(end = 8.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) TextWhite else TextGray.copy(alpha = 0.25f),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable(enabled = index > 0) { moveRuleUp(index) }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (index < tableRules.size - 1) TextWhite else TextGray.copy(alpha = 0.25f),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable(enabled = index < tableRules.size - 1) { moveRuleDown(index) }
                                    )
                                }

                                // Full Clean Rule Title
                                Text(
                                    text = rule.name,
                                    color = TextWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Action Dropdown Selector (BLOCKED / DIRECT / VPN)
                                Box(modifier = Modifier.padding(end = 6.dp)) {
                                    Surface(
                                        color = actionColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, actionColor.copy(alpha = 0.45f)),
                                        modifier = Modifier.clickable { ruleMenuExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = rule.action,
                                                color = actionColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Action",
                                                tint = actionColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = ruleMenuExpanded,
                                        onDismissRequest = { ruleMenuExpanded = false },
                                        modifier = Modifier.background(DarkCardElevated)
                                    ) {
                                        listOf("BLOCKED", "DIRECT", "VPN").forEach { act ->
                                            val actColor = when (act) {
                                                "BLOCKED" -> WarningRose
                                                "DIRECT" -> SecureGreen
                                                else -> ElectricViolet
                                            }
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = act,
                                                        color = actColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    val nextList = tableRules.mapIndexed { i, r ->
                                                        if (i == index) r.copy(action = act) else r
                                                    }
                                                    saveTableRules(nextList)
                                                    ruleMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Enable Switch
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { chk ->
                                        val nextList = tableRules.mapIndexed { i, r ->
                                            if (i == index) r.copy(enabled = chk) else r
                                        }
                                        saveTableRules(nextList)
                                    },
                                    modifier = Modifier.scale(0.85f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = actionColor,
                                        checkedTrackColor = actionColor.copy(alpha = 0.35f),
                                        uncheckedThumbColor = TextGray,
                                        uncheckedTrackColor = CardBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Add Custom Domain/IP Rule
        item {
            DoppelrandCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyberCyan.copy(alpha = 0.35f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = "ПОЛЬЗОВАТЕЛЬСКИЕ ПРАВИЛА (DOMAINS / IPS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Action Segmented Selector (Direct / Proxy / Block)
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        listOf(
                            "Direct" to "DIRECT (Напрямую)",
                            "Proxy" to "PROXY (через VPN)",
                            "Block" to "BLOCK (Блок)"
                        ).forEach { (targetKey, targetLabel) ->
                            val isSelected = selectedTarget == targetKey
                            val targetColor = when (targetKey) {
                                "Direct" -> SecureGreen
                                "Proxy" -> ElectricViolet
                                else -> WarningRose
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
                                    color = if (isSelected) TextWhite else TextWhite.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 2.dp)
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
                        placeholder = { Text(text = string("add_custom_domain_or_ip"), color = TextGray, fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextWhite, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = DarkCardElevated,
                            unfocusedContainerColor = DarkCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val rule = customInputText.trim()
                            if (rule.isNotBlank()) {
                                val (isValid, errorMsg) = SentinelCore.validateRuleOrConfig(rule)
                                if (isValid) {
                                    onAddCustomRule(selectedTarget, rule)
                                    customInputText = ""
                                } else {
                                    Toast.makeText(context, "Ошибка валидации правила: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Added Rules Lists
                CustomRulesChipGroup(
                    title = "${string("custom_target_direct")} (${customDirectRules.size})",
                    rules = customDirectRules,
                    chipColor = SecureGreen,
                    onRemove = { onRemoveCustomRule("Direct", it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomRulesChipGroup(
                    title = "${string("custom_target_proxy")} (${customProxyRules.size})",
                    rules = customProxyRules,
                    chipColor = ElectricViolet,
                    onRemove = { onRemoveCustomRule("Proxy", it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CustomRulesChipGroup(
                    title = "${string("custom_target_block")} (${customBlockRules.size})",
                    rules = customBlockRules,
                    chipColor = WarningRose,
                    onRemove = { onRemoveCustomRule("Block", it) }
                )
            }
        }
    }
}

@Composable
private fun CustomRulesChipGroup(
    title: String,
    rules: List<String>,
    chipColor: Color,
    onRemove: (String) -> Unit
) {
    if (rules.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = chipColor,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        OptInFlowRow(
            horizontalGap = 6.dp,
            verticalGap = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            rules.forEach { rule ->
                Surface(
                    color = chipColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, chipColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rule,
                            color = TextWhite,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Remove",
                            tint = chipColor,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemove(rule) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptInFlowRow(
    horizontalGap: androidx.compose.ui.unit.Dp,
    verticalGap: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(verticalGap)) {
        content()
    }
}
