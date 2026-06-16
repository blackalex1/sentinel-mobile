package com.xprox.sentinel.ui.components.sensitiveports

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun AddPortRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (port: Int, service: String) -> Unit
) {
    val context = LocalContext.current
    var inputPort by remember { mutableStateOf("") }
    var inputService by remember { mutableStateOf("") }

    val isRu = com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru"

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            inputPort = ""
            inputService = ""
        },
        title = {
            Text(
                text = if (isRu) "ДОБАВИТЬ ПОРТ" else "ADD PORT",
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputPort,
                    onValueChange = { inputPort = it.filter { char -> char.isDigit() } },
                    label = { Text(if (isRu) "Номер порта" else "Port Number", color = TextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = CyberTeal,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputService,
                    onValueChange = { inputService = it },
                    label = { Text(if (isRu) "Описание / Сервис" else "Description / Service", color = TextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = CyberTeal,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portNum = inputPort.toIntOrNull()
                    if (portNum != null && portNum in 1..65535) {
                        val serviceName = inputService.trim().ifEmpty { "Custom Port" }
                        onConfirm(portNum, serviceName)
                    } else {
                        Toast.makeText(
                            context,
                            if (isRu) "Введите корректный порт (1-65535)" else "Enter a valid port (1-65535)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
            ) {
                Text(if (isRu) "Добавить" else "Add", color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    inputPort = ""
                    inputService = ""
                }
            ) {
                Text(if (isRu) "Отмена" else "Cancel", color = TextGray)
            }
        },
        containerColor = DarkCard,
        tonalElevation = 8.dp
    )
}
