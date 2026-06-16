package com.xprox.sentinel.ui.components.sensitiveports

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun DeletePortRuleDialog(
    port: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isRu = com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRu) "Удалить порт?" else "Delete Port?",
                fontWeight = FontWeight.Bold,
                color = WarningRed
            )
        },
        text = {
            Text(
                text = if (isRu) {
                    "Вы действительно хотите удалить пользовательский порт $port?"
                } else {
                    "Are you sure you want to delete custom port $port?"
                },
                color = TextWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
            ) {
                Text(if (isRu) "Удалить" else "Delete", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(if (isRu) "Отмена" else "Cancel", color = TextGray)
            }
        },
        containerColor = DarkCard,
        tonalElevation = 8.dp
    )
}
