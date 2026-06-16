package com.xprox.sentinel.ui.components.blockedapps

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun UnblockConfirmationDialog(
    targetPkg: String,
    context: Context,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val appLabel = remember(targetPkg) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(targetPkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            targetPkg
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚠️ СНЯТИЕ ИЗОЛЯЦИИ",
                color = WarningRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Text(
                text = "Вы действительно хотите полностью снять изоляцию и восстановить доступ к сети для приложения '$appLabel' ($targetPkg)? Это отключит Zero Trust блокировку и позволит приложению свободно передавать сетевые данные.",
                color = TextWhite,
                fontSize = 12.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WarningRed.copy(alpha = 0.20f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningRed),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Снять блок",
                    color = WarningRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TextWhite.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Отмена",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = DarkCard,
        modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    )
}
