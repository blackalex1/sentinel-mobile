package com.xprox.sentinel.ui.components.lansharing

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun LanSharingActiveIpsBox(
    context: Context,
    isVpnActive: Boolean,
    activeTetherIps: List<String>
) {
    if (!isVpnActive || activeTetherIps.isEmpty()) return

    val clipboardManager = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = string("active_hotspot_ips"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTeal
            )
            Spacer(modifier = Modifier.height(4.dp))
            activeTetherIps.forEach { ip ->
                Text(
                    text = "•  $ip",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(ip))
                        Toast.makeText(context, ip, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
