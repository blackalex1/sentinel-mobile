package com.xprox.sentinel.ui.screens.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.xprox.sentinel.theme.*
import com.xprox.sentinel.ui.screens.AppInfo

@Composable
fun AppRoutingRow(
    app: AppInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onCheckedChange(!isChecked)
                }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text(text = app.packageName, fontSize = 10.sp, color = TextGray)
                }
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                    onCheckedChange(checked)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberTeal,
                    uncheckedColor = CardBorder
                )
            )
        }
        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
    }
}
