package com.xprox.sentinel.ui.components.serverprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun VlessSpecificFields(
    flow: String,
    onFlowChange: (String) -> Unit,
    encryption: String,
    onEncryptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = flow,
            onValueChange = onFlowChange,
            label = { Text(string("vless_flow"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = encryption,
            onValueChange = onEncryptionChange,
            label = { Text(string("vless_encryption"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
