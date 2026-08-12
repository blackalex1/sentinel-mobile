package com.xprox.sentinel.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.*

@Composable
fun AboutAppCard() {
    val context = LocalContext.current
    val repoUrl = "https://github.com/blackalex1/sentinel-mobile"

    DoppelrandCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ElectricViolet.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = ElectricViolet,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Sentinel Mobile v1.1.0",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Loopback Hardened Core • API 29+",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                            context.startActivity(intent)
                        }
                ) {
                    Text(
                        text = "repo: ",
                        fontSize = 11.sp,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "blackalex1/sentinel-mobile",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}
