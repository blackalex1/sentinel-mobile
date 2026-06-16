package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun DisconnectConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = string("disconnect_confirm_title"),
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
        },
        text = {
            Text(
                text = string("disconnect_confirm_desc"),
                color = TextWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
            ) {
                Text(string("btn_confirm_disconnect"), color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(string("cancel"), color = TextGray)
            }
        },
        containerColor = DarkCard,
        tonalElevation = 8.dp
    )
}
