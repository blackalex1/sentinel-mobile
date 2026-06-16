package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun DownloadRequiredDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = string("xray_required"),
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
        },
        text = {
            Text(
                text = string("xray_required_desc"),
                color = TextWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
            ) {
                Text(string("go_to_settings"), color = DarkBg, fontWeight = FontWeight.Bold)
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
