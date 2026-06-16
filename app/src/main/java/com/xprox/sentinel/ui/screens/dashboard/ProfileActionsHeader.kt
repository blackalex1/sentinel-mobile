package com.xprox.sentinel.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.CyberTeal
import com.xprox.sentinel.theme.TextWhite

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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = string("connection_profiles"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onImportClipboard,
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import", tint = CyberTeal, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(string("import_clipboard"), fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
            }
 
            if (showFeed) {
                TextButton(
                    onClick = onFeedClick,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(string("feed_btn"), fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                }
            }
 
            if (showAddDirect) {
                TextButton(
                    onClick = onAddDirectProfile,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Direct", tint = CyberTeal, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(string("add_direct_profile_btn"), fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onAddProfile,
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = CyberTeal, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(string("add_profile_btn"), fontSize = 10.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}
