package com.xprox.sentinel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xprox.sentinel.theme.*

/**
 * Doppelrand (Double Bezel) Card Architecture Component
 * Aligned with x-pc (.double-bezel-shell & .double-bezel-core) & panel (--bg-card)
 *
 * Outer Shell: 22dp corner radius, 5dp padding, subtle translucent border
 * Inner Core: 16dp corner radius, DarkCard (#0D0D15) surface
 */
@Composable
fun DoppelrandCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DoppelrandShellBorder,
    glowColor: Color? = null,
    shellPadding: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val outerShape = RoundedCornerShape(22.dp)
    val innerShape = RoundedCornerShape(16.dp)
    val finalBorderColor = glowColor?.copy(alpha = 0.4f) ?: borderColor

    Surface(
        color = DoppelrandShellBg,
        shape = outerShape,
        border = BorderStroke(1.dp, finalBorderColor),
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(outerShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        )
    ) {
        Box(
            modifier = Modifier
                .padding(shellPadding)
                .background(DarkCard, shape = innerShape)
                .border(1.dp, CardBorder, shape = innerShape)
                .clip(innerShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}
