package com.xprox.sentinel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayConfigManager
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.data.string
import com.xprox.sentinel.ui.components.serverprofile.*
import java.util.UUID

@Composable
fun ServerProfileDialog(
    profile: XrayConfigManager.ServerProfile?,
    onDismiss: () -> Unit,
    onConfirm: (XrayConfigManager.ServerProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var address by remember { mutableStateOf(profile?.address ?: "") }
    var portStr by remember { mutableStateOf(profile?.port?.toString() ?: "443") }
    var uuid by remember { mutableStateOf(profile?.uuid ?: "") }
    var type by remember { mutableStateOf(profile?.type ?: "VLESS") }
    var security by remember { mutableStateOf(profile?.security ?: "none") }
    var path by remember { mutableStateOf(profile?.path ?: "") }
    var sni by remember { mutableStateOf(profile?.sni ?: "") }
    var pbk by remember { mutableStateOf(profile?.pbk ?: "") }
    var sid by remember { mutableStateOf(profile?.sid ?: "") }
    var fp by remember { mutableStateOf(profile?.fp ?: "chrome") }
    var network by remember { mutableStateOf(profile?.network ?: "tcp") }
    var flow by remember { mutableStateOf(profile?.flow ?: "") }
    var encryption by remember { mutableStateOf(profile?.encryption ?: "none") }
    var spx by remember { mutableStateOf(profile?.spx ?: "") }
    var host by remember { mutableStateOf(profile?.host ?: "") }
    var allowInsecure by remember { mutableStateOf(profile?.allowInsecure ?: false) }
    var alpn by remember { mutableStateOf(profile?.alpn ?: "") }
    var headerType by remember { mutableStateOf(profile?.headerType ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        tonalElevation = 6.dp,
        title = {
            Text(
                text = if (profile == null) string("profile_dialog_add_title") else string("profile_dialog_edit_title"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(string("profile_name"), color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                val isDirect = type.uppercase() == "DIRECT"

                Text(
                    text = string("protocol_label"),
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val row1 = listOf("VLESS", "VMESS", "SHADOWSOCKS")
                    val row2 = listOf("TROJAN", "SOCKS", "DIRECT")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row1.forEach { proto ->
                            val selected = type.uppercase() == proto
                            Button(
                                onClick = { type = proto },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) CyberTeal else DarkCard,
                                    contentColor = if (selected) DarkBg else TextWhite
                                ),
                                border = BorderStroke(1.dp, if (selected) CyberTeal else CardBorder),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(proto, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row2.forEach { proto ->
                            val selected = type.uppercase() == proto
                            Button(
                                onClick = { type = proto },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) CyberTeal else DarkCard,
                                    contentColor = if (selected) DarkBg else TextWhite
                                ),
                                border = BorderStroke(1.dp, if (selected) CyberTeal else CardBorder),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(proto, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!isDirect) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(string("server_host"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text(string("server_port"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text(string("vless_uuid"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = security,
                        onValueChange = { security = it },
                        label = { Text(string("security_label"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = network,
                        onValueChange = { network = it },
                        label = { Text(string("transport_label"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = path,
                        onValueChange = { path = it },
                        label = { Text(string("path_label"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sni,
                        onValueChange = { sni = it },
                        label = { Text(string("sni_label"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(string("host_header"), color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (network.lowercase() == "tcp") {
                        OutlinedTextField(
                            value = headerType,
                            onValueChange = { headerType = it },
                            label = { Text(string("tcp_obfuscation"), color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberTeal,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (security.lowercase() == "tls" || security.lowercase() == "reality") {
                        TlsRealitySettingsFields(
                            alpn = alpn,
                            onAlpnChange = { alpn = it },
                            allowInsecure = allowInsecure,
                            onAllowInsecureChange = { allowInsecure = it }
                        )
                    }
                    
                    if (security.lowercase() == "reality") {
                        RealitySpecificFields(
                            pbk = pbk,
                            onPbkChange = { pbk = it },
                            sid = sid,
                            onSidChange = { sid = it },
                            fp = fp,
                            onFpChange = { fp = it },
                            spx = spx,
                            onSpxChange = { spx = it }
                        )
                    }

                    if (type.lowercase() == "vless") {
                        VlessSpecificFields(
                            flow = flow,
                            onFlowChange = { flow = it },
                            encryption = encryption,
                            onEncryptionChange = { encryption = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portStr.toIntOrNull() ?: 443
                    val isDirectMode = type.uppercase() == "DIRECT"
                    val newProfile = XrayConfigManager.ServerProfile(
                        id = profile?.id ?: UUID.randomUUID().toString(),
                        name = name.ifEmpty { 
                            if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Анализ трафика (Без VPN)" else "Direct Traffic Analysis (No VPN)"
                        },
                        address = if (isDirectMode) "" else address,
                        port = if (isDirectMode) 0 else port,
                        type = type,
                        uuid = if (isDirectMode) "" else uuid,
                        security = if (isDirectMode) "none" else security,
                        path = if (isDirectMode) "" else path,
                        sni = if (isDirectMode) "" else sni,
                        pbk = if (isDirectMode) "" else pbk,
                        sid = if (isDirectMode) "" else sid,
                        fp = if (isDirectMode) "chrome" else fp,
                        network = if (isDirectMode) "tcp" else network,
                        flow = if (isDirectMode) "" else flow,
                        encryption = if (isDirectMode) "none" else encryption,
                        spx = if (isDirectMode) "" else spx,
                        host = if (isDirectMode) "" else host,
                        allowInsecure = if (isDirectMode) false else allowInsecure,
                        alpn = if (isDirectMode) "" else alpn,
                        headerType = if (isDirectMode) "" else headerType,
                        pinnedPeerCertSha256 = profile?.pinnedPeerCertSha256 ?: "",
                        groupId = profile?.groupId
                    )
                    onConfirm(newProfile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
            ) {
                Text(string("save_changes"), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(string("cancel"), color = TextGray)
            }
        }
    )
}
