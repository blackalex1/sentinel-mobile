package com.xprox.sentinel.ui.components.localproxy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.theme.CyberTeal
import com.xprox.sentinel.theme.CardBorder
import com.xprox.sentinel.theme.TextWhite
import com.xprox.sentinel.theme.TextGray

@Composable
fun LocalProxyRandomizeRow(
    isRandomizeEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRu = com.xprox.sentinel.data.LanguageManager.currentLanguage.value.code == "ru"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isRu) "Рандомизировать при каждом запуске" else "Randomize on every launch",
                fontSize = 12.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isRu) {
                    "Генерировать новый случайный порт и пароль при каждом старте VPN"
                } else {
                    "Generate a new random port and password every time the VPN starts"
                },
                fontSize = 9.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = isRandomizeEnabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberTeal,
                checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = CardBorder
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}
