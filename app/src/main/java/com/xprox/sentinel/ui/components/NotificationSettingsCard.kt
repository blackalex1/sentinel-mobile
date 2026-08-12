package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.config.XrayProfilePersistence
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun NotificationSettingsCard(context: Context) {
    var showSpeed by remember { mutableStateOf(XrayProfilePersistence.loadShowSpeedInNotification(context)) }

    SentinelSettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = string("show_speed_in_notification_title"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = string("show_speed_in_notification_desc"),
                    fontSize = 11.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }

            Switch(
                checked = showSpeed,
                onCheckedChange = { checked ->
                    showSpeed = checked
                    XrayProfilePersistence.saveShowSpeedInNotification(context, checked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricViolet,
                    checkedTrackColor = ElectricViolet.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = CardBorder
                )
            )
        }
    }
}
