package com.xprox.sentinel.ui.components.localproxy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LocalProxyCredentialsBox(
    isRandomizeEnabled: Boolean,
    isVpnActive: Boolean,
    displayUsername: String,
    displayPassword: String,
    displayPort: Int,
    onRegenerate: () -> Unit,
    onCopy: (text: String, successMsg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isRu = com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru"

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!isRandomizeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (isRu) "🔄 СГЕНЕРИРОВАТЬ НОВЫЕ ДАННЫЕ" else "🔄 REGENERATE NEW DATA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        modifier = Modifier.clickable {
                            onRegenerate()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // SOCKS5 Host & Port Block
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Адрес прокси (SOCKS5)" else "Proxy Host & Port (SOCKS5)",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                        if (displayPort > 0) {
                            Text(
                                text = string("btn_copy"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    onCopy(
                                        "127.0.0.1:$displayPort",
                                        if (isRu) "Адрес скопирован" else "Address copied"
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (displayPort > 0) "127.0.0.1:$displayPort" else "127.0.0.1: [Случайный порт]",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Username Block
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = string("lan_username"),
                            fontSize = 10.sp,
                            color = TextGray
                        )
                        if (!isRandomizeEnabled || isVpnActive) {
                            Text(
                                text = string("btn_copy"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    onCopy(
                                        displayUsername,
                                        if (isRu) "Логин скопирован" else "Username copied"
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayUsername,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Password Block
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = string("lan_password"),
                            fontSize = 10.sp,
                            color = TextGray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRandomizeEnabled || isVpnActive) {
                                Text(
                                    text = if (isPasswordVisible) string("btn_hide") else string("btn_show"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTeal,
                                    modifier = Modifier.clickable {
                                        isPasswordVisible = !isPasswordVisible
                                    }
                                )
                                Text(
                                    text = string("btn_copy"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTeal,
                                    modifier = Modifier.clickable {
                                        onCopy(
                                            displayPassword,
                                            if (isRu) "Пароль скопирован" else "Password copied"
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val displayPassText = if (isPasswordVisible || (isRandomizeEnabled && !isVpnActive)) {
                        displayPassword
                    } else {
                        "•".repeat(displayPassword.length)
                    }
                    Text(
                        text = displayPassText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }
        }
    }
}
