package com.xprox.sentinel.ui.components.lansharing

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LanSharingProxyUrlsBox(
    context: Context,
    isVpnActive: Boolean,
    activeTetherIps: List<String>,
    isHttpEnabled: Boolean,
    isSocksEnabled: Boolean,
    isAuthEnabled: Boolean,
    displayUsername: String,
    displayPassword: String,
    displayHttpPort: Int,
    displaySocksPort: Int
) {
    if (!isVpnActive || activeTetherIps.isEmpty() || (!isHttpEnabled && !isSocksEnabled)) return

    val clipboardManager = LocalClipboardManager.current
    val firstIp = activeTetherIps.firstOrNull() ?: "192.168.43.1"
    
    val socksProxyUrl = if (isAuthEnabled) {
        "socks5://${displayUsername}:${displayPassword}@$firstIp:$displaySocksPort#Sentinel-Hotspot"
    } else {
        "socks5://$firstIp:$displaySocksPort#Sentinel-Hotspot"
    }
    
    val httpProxyUrl = if (isAuthEnabled) {
        "http://${displayUsername}:${displayPassword}@$firstIp:$displayHttpPort"
    } else {
        "http://$firstIp:$displayHttpPort"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. SOCKS5 Proxy Card (For v2rayNG / Nekobox)
        if (isSocksEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = string("lan_socks_title"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal
                            )
                            Text(
                                text = string("lan_proxy_url_desc"),
                                fontSize = 8.sp,
                                color = TextGray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = string("btn_share"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    try {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, socksProxyUrl)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "Error sharing", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Text(
                                text = string("btn_copy"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(socksProxyUrl))
                                    Toast.makeText(
                                        context,
                                        if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "SOCKS5 ссылка скопирована!" else "SOCKS5 URL copied!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = socksProxyUrl,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(socksProxyUrl))
                            Toast.makeText(
                                context,
                                if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "SOCKS5 ссылка скопирована!" else "SOCKS5 URL copied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }

        if (isSocksEnabled && isHttpEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. HTTP Proxy Card (For Wi-Fi settings / TV / Telegram)
        if (isHttpEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = string("lan_http_title"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = string("lan_proxy_url_desc"),
                                fontSize = 8.sp,
                                color = TextGray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = string("btn_share"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    try {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, httpProxyUrl)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "Error sharing", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Text(
                                text = string("btn_copy"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTeal,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(httpProxyUrl))
                                    Toast.makeText(
                                        context,
                                        if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "HTTP ссылка скопирована!" else "HTTP URL copied!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = httpProxyUrl,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(httpProxyUrl))
                            Toast.makeText(
                                context,
                                if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "HTTP ссылка скопирована!" else "HTTP URL copied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
