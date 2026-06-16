package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LanguageSelectorCard(context: Context) {
    val currentLang by LanguageManager.currentLanguage.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = string("lang_card_title"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                letterSpacing = 1.sp
            )
            Text(
                text = string("lang_card_desc"),
                fontSize = 10.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Segmented selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LanguageManager.Language.values().forEach { lang ->
                    val isSelected = currentLang == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberTeal.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                LanguageManager.setLanguage(context, lang)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.displayName,
                            color = if (isSelected) CyberTeal else TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
