package com.xprox.sentinel.ui.components.lansharing

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun LanSharingCredentialsBox(
    context: Context,
    isAuthEnabled: Boolean,
    isRandomizeEnabled: Boolean,
    displayUsername: String,
    displayPassword: String,
    onRegenerateClick: () -> Unit
) {
    if (!isAuthEnabled) return

    var isPasswordVisible by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
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
                        text = if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") {
                            "🔄 СГЕНЕРИРОВАТЬ НОВЫЕ ДАННЫЕ"
                        } else {
                            "🔄 REGENERATE NEW DATA"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTeal,
                        modifier = Modifier.clickable {
                            onRegenerateClick()
                            Toast.makeText(
                                context,
                                if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Новые данные сгенерированы!" else "New credentials generated!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Username Block (Stacked)
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
                        Text(
                            text = string("btn_copy"),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(displayUsername))
                                Toast.makeText(context, if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Логин скопирован" else "Username copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayUsername,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(displayUsername))
                            Toast.makeText(context, if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Логин скопирован" else "Username copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                
                // Password Block (Stacked)
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
                                    clipboardManager.setText(AnnotatedString(displayPassword))
                                    Toast.makeText(context, if (com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru") "Пароль скопирован" else "Password copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val displayPassText = if (isPasswordVisible) {
                        displayPassword
                    } else {
                        "•".repeat(displayPassword.length)
                    }
                    
                    Text(
                        text = displayPassText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.clickable {
                            isPasswordVisible = !isPasswordVisible
                        }
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}
