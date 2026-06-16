package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.log.LogManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.components.sensitiveports.*

@Composable
fun SensitivePortsCard(context: Context) {
    var activePorts by remember { mutableStateOf(LogManager.loadActivePorts(context)) }
    var customPorts by remember { mutableStateOf(LogManager.loadCustomPorts(context)) }
    
    val allPorts = (LogManager.ALL_AVAILABLE_SENSITIVE_PORTS + customPorts).toList().sortedBy { it.first }

    var showAddDialog by remember { mutableStateOf(false) }
    var portToDelete by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        // High-tech terminal header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = string("ports_selector_title").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = string("ports_selector_desc"),
            fontSize = 10.sp,
            color = TextGray,
            lineHeight = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Render rows of 2 items each
        val chunkedPorts = allPorts.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunkedPorts.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (port, serviceName) ->
                        val isActive = activePorts.contains(port)
                        val isCustom = customPorts.containsKey(port)

                        SensitivePortItem(
                            port = port,
                            serviceName = serviceName,
                            isActive = isActive,
                            isCustom = isCustom,
                            onToggle = {
                                val nextSet = activePorts.toMutableSet()
                                if (isActive) {
                                    nextSet.remove(port)
                                } else {
                                    nextSet.add(port)
                                }
                                activePorts = nextSet
                                LogManager.saveActivePorts(context, nextSet)
                            },
                            onLongClick = {
                                portToDelete = port
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill row with an empty space if odd item
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cyber Outlined Add Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.4f)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = CyberTeal
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add custom port",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "ДОБАВИТЬ СВОЙ ПОРТ" else "ADD CUSTOM PORT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }

    // Add Custom Port Dialog
    if (showAddDialog) {
        AddPortRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { portNum, serviceName ->
                LogManager.addCustomPort(context, portNum, serviceName)
                customPorts = LogManager.loadCustomPorts(context)
                activePorts = LogManager.loadActivePorts(context)
                showAddDialog = false
            }
        )
    }

    // Delete Custom Port Dialog
    if (portToDelete != null) {
        DeletePortRuleDialog(
            port = portToDelete!!,
            onDismiss = { portToDelete = null },
            onConfirm = {
                val port = portToDelete
                if (port != null) {
                    LogManager.deleteCustomPort(context, port)
                    customPorts = LogManager.loadCustomPorts(context)
                    activePorts = LogManager.loadActivePorts(context)
                }
                portToDelete = null
            }
        )
    }
}
