package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.*

@Composable
fun SecurityPolicyCard(context: Context) {
    val isRu = LanguageManager.currentLanguage.collectAsState().value.code == "ru"

    var shieldMode by remember { mutableStateOf(LogManager.loadShieldMode(context)) }
    var blockThreshold by remember { mutableStateOf(LogManager.loadBlockThreshold(context)) }
    var autoPcap by remember { mutableStateOf(LogManager.loadAutoPcap(context)) }

    var showCustomThresholdDialog by remember { mutableStateOf(false) }
    var customThresholdText by remember { mutableStateOf(blockThreshold.toString()) }

    if (showCustomThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showCustomThresholdDialog = false },
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (isRu) "Порог попыток до блока" else "Block Threshold",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isRu)
                            "Укажите количество попыток (1 - 100) до автоматической блокировки:"
                        else
                            "Specify number of attempts (1 - 100) before dropping connection:",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val parsed = customThresholdText.toIntOrNull()
                    val isValid = parsed != null && parsed in 1..100
                    val isError = customThresholdText.isNotEmpty() && !isValid

                    OutlinedTextField(
                        value = customThresholdText,
                        onValueChange = { input ->
                            customThresholdText = input.filter { it.isDigit() }.take(3)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isError,
                        supportingText = {
                            if (isError || customThresholdText.isEmpty()) {
                                Text(
                                    text = if (isRu) "Введите число от 1 до 100" else "Enter number 1 - 100",
                                    color = WarningRed,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = CardBorder,
                            errorBorderColor = WarningRed,
                            focusedContainerColor = DarkCardElevated,
                            unfocusedContainerColor = DarkCardElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val parsed = customThresholdText.toIntOrNull()
                val isValid = parsed != null && parsed in 1..100
                TextButton(
                    enabled = isValid,
                    onClick = {
                        if (parsed != null && parsed in 1..100) {
                            blockThreshold = parsed
                            LogManager.saveBlockThreshold(context, parsed)
                        }
                        showCustomThresholdDialog = false
                    }
                ) {
                    Text(
                        text = if (isRu) "Применить" else "Apply",
                        color = if (isValid) ElectricViolet else TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomThresholdDialog = false }) {
                    Text(text = if (isRu) "Отмена" else "Cancel", color = TextGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRu) "ПОЛИТИКА ЗАЩИТЫ И ПОРОГИ" else "SECURITY POLICY & THRESHOLDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRu)
                "Настройка алгоритма реагирования ядра Zero Trust на несанкционированные сетевые запросы"
            else
                "Configure zero trust core reaction policy to unauthorized network probes",
            fontSize = 10.sp,
            color = TextGray,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0x14FFFFFF))
        Spacer(modifier = Modifier.height(12.dp))

        // Shield Mode Selector
        Text(
            text = if (isRu) "Режим экранирования портов:" else "Port Shielding Mode:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))

        val modes = listOf(
            Triple(
                "threshold_block",
                if (isRu) "🛡️ Пороговый ($blockThreshold ${if (blockThreshold == 1) "попытка" else if (blockThreshold in 2..4) "попытки" else "попыток"})"
                else "🛡️ Threshold ($blockThreshold attempts)",
                if (isRu) "Предупреждение, блок при превышении лимита" else "Alert first, block upon exceeding limit"
            ),
            Triple(
                "strict_block",
                if (isRu) "⚡ Строгий Zero Trust" else "⚡ Strict Zero Trust",
                if (isRu) "Мгновенная блокировка с 1-й попытки" else "Immediate block on 1st attempt"
            ),
            Triple(
                "alert_only",
                if (isRu) "👁️ Только аудит" else "👁️ Alert Only",
                if (isRu) "Только мониторинг и логирование без блокировок" else "Audit and logging without drops"
            )
        )

        modes.forEach { (modeKey, modeTitle, modeDesc) ->
            val isSelected = shieldMode == modeKey
            Surface(
                color = if (isSelected) ElectricViolet.copy(alpha = 0.15f) else DarkCardElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isSelected) ElectricViolet else CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable {
                        shieldMode = modeKey
                        LogManager.saveShieldMode(context, modeKey)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            shieldMode = modeKey
                            LogManager.saveShieldMode(context, modeKey)
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricViolet)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = modeTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextWhite else TextGray
                        )
                        Text(
                            text = modeDesc,
                            fontSize = 9.5.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }

        // Threshold Selector: ONLY shown for threshold_block mode!
        if (shieldMode == "threshold_block") {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Порог попыток до блока:" else "Block Threshold:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = if (isRu) "Количество попыток до блокировки" else "Probes before dropping connection",
                        fontSize = 9.5.sp,
                        color = TextGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 3, 5, 10).forEach { limit ->
                        val isSel = blockThreshold == limit
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) ElectricViolet else DarkCardElevated)
                                .border(1.dp, if (isSel) ElectricViolet else CardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    blockThreshold = limit
                                    LogManager.saveBlockThreshold(context, limit)
                                }
                        ) {
                            Text(
                                text = limit.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) TextWhite else TextGray
                            )
                        }
                    }

                    // Custom Number Chip
                    val isCustom = !listOf(1, 2, 3, 5, 10).contains(blockThreshold)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCustom) ElectricViolet else DarkCardElevated)
                            .border(1.dp, if (isCustom) ElectricViolet else CardBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                customThresholdText = blockThreshold.toString()
                                showCustomThresholdDialog = true
                            }
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isCustom) TextWhite else TextGray,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isCustom) blockThreshold.toString() else (if (isRu) "Свой" else "Custom"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustom) TextWhite else TextGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0x14FFFFFF))
        Spacer(modifier = Modifier.height(10.dp))

        // PCAP Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRu) "Автосбор дампов PCAP" else "Auto PCAP Capture",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = if (isRu) "Запись сетевых пакетов при атаках и сканировании" else "Record packets upon threat detection & port scans",
                    fontSize = 9.5.sp,
                    color = TextGray
                )
            }

            Switch(
                checked = autoPcap,
                onCheckedChange = {
                    autoPcap = it
                    LogManager.saveAutoPcap(context, it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = ElectricViolet
                )
            )
        }
    }
}
