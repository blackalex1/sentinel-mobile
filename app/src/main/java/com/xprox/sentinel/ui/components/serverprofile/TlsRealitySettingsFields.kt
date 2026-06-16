package com.xprox.sentinel.ui.components.serverprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun TlsRealitySettingsFields(
    alpn: String,
    onAlpnChange: (String) -> Unit,
    allowInsecure: Boolean,
    onAllowInsecureChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = alpn,
            onValueChange = onAlpnChange,
            label = { Text(string("alpn_label"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = string("allow_insecure_label"),
                color = TextWhite,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = allowInsecure,
                onCheckedChange = onAllowInsecureChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberTeal,
                    checkedTrackColor = CyberTeal.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = CardBorder
                )
            )
        }
    }
}
