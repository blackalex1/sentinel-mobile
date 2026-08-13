package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun ProfileActionsHeader(
    onImportClipboard: () -> Unit,
    onAddProfile: () -> Unit,
    onAddDirectProfile: () -> Unit,
    onFeedClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFeed: Boolean = false,
    showAddDirect: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Title with Monospace Accent
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = string("connection_profiles"),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Tactile Action Pill Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Import from Clipboard
            ActionPillButton(
                icon = Icons.Default.ContentPaste,
                label = string("import_clipboard"),
                accentColor = CyberCyan,
                modifier = Modifier.weight(1f),
                onClick = onImportClipboard
            )

            // 2. Feeds / Subscriptions (if enabled)
            if (showFeed) {
                ActionPillButton(
                    icon = Icons.Default.RssFeed,
                    label = string("feed_btn"),
                    accentColor = ElectricViolet,
                    modifier = Modifier.weight(1f),
                    onClick = onFeedClick
                )
            }

            // 3. Add Direct Analysis Profile (if not yet added)
            if (showAddDirect) {
                ActionPillButton(
                    icon = Icons.Default.Shield,
                    label = string("add_direct_profile_btn"),
                    accentColor = SecureGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onAddDirectProfile
                )
            }

            // 4. Manual Create Profile
            ActionPillButton(
                icon = Icons.Default.Add,
                label = string("add_profile_btn"),
                accentColor = ElectricViolet,
                modifier = Modifier.weight(1f),
                onClick = onAddProfile
            )
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = DarkCardElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
