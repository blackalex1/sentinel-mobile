package com.xprox.sentinel.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.LanguageManager
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LanguageSelectorCard(context: Context) {
    val currentLang by LanguageManager.currentLanguage.collectAsState()

    SentinelSettingsCard {
        Text(
            text = string("lang_card_title"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricViolet,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = string("lang_card_desc"),
            fontSize = 11.sp,
            color = TextGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Segmented selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardElevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LanguageManager.Language.values().forEach { lang ->
                val isSelected = currentLang == lang

                val itemBg by animateColorAsState(
                    targetValue = if (isSelected) ElectricViolet else Color.Transparent,
                    label = "itemBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .clickable {
                            LanguageManager.setLanguage(context, lang)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lang.displayName,
                        color = if (isSelected) TextWhite else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
