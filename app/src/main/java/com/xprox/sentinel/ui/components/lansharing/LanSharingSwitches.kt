package com.xprox.sentinel.ui.components.lansharing

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LanSharingSwitches(
    context: Context,
    isHttpEnabled: Boolean,
    onHttpChanged: (Boolean) -> Unit,
    isSocksEnabled: Boolean,
    onSocksChanged: (Boolean) -> Unit,
    isAuthEnabled: Boolean,
    onAuthChanged: (Boolean) -> Unit,
    isRandomizeEnabled: Boolean,
    onRandomizeChanged: (Boolean) -> Unit
) {
    // Switch 1: Start HTTP Server (Port 10809)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = string("lan_enable_http"),
                fontSize = 12.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = string("lan_enable_http_desc"),
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isHttpEnabled,
            onCheckedChange = { checked ->
                if (!checked && !isSocksEnabled) {
                    Toast.makeText(
                        context,
                        if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                            "Пожалуйста, оставьте активным хотя бы один протокол"
                        } else {
                            "Please keep at least one protocol active"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onHttpChanged(checked)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberTeal,
                checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.scale(0.85f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Switch 2: Start SOCKS5 Server (Port 10808)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = string("lan_enable_socks"),
                fontSize = 12.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = string("lan_enable_socks_desc"),
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isSocksEnabled,
            onCheckedChange = { checked ->
                if (!checked && !isHttpEnabled) {
                    Toast.makeText(
                        context,
                        if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                            "Пожалуйста, оставьте активным хотя бы один протокол"
                        } else {
                            "Please keep at least one protocol active"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onSocksChanged(checked)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberTeal,
                checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.scale(0.85f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(12.dp))

    // Secondary Switch: Enable Authentication
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = string("lan_sharing_auth_title"),
                fontSize = 12.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = string("lan_sharing_auth_desc"),
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isAuthEnabled,
            onCheckedChange = { checked ->
                onAuthChanged(checked)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberTeal,
                checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.scale(0.85f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Tertiary Switch: Randomize Ports & Password
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                    "Рандомизировать при каждом запуске"
                } else {
                    "Randomize on every launch"
                },
                fontSize = 12.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                    "Генерировать новый случайный порт и пароль при каждом старте VPN"
                } else {
                    "Generate a new random port and password every time the VPN starts"
                },
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isRandomizeEnabled,
            onCheckedChange = { checked ->
                onRandomizeChanged(checked)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberTeal,
                checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}
