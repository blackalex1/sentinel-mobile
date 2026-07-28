package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.VisualLogEntry

@Composable
fun TrafficLogItemRow(
    entry: VisualLogEntry,
    onClick: () -> Unit
) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (entry.isSensitive) WarningRed.copy(alpha = 0.4f) else CardBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.appName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isSensitive) WarningRed else CyberTeal
                    )
                    if (entry.port != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ":${entry.port}",
                            fontSize = 10.sp,
                            color = TextGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.line,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite
                )
            }

            if (entry.isSensitive) {
                Surface(
                    color = WarningRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SECURITY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
