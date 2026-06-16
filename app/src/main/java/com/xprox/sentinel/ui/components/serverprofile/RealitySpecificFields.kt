package com.xprox.sentinel.ui.components.serverprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun RealitySpecificFields(
    pbk: String,
    onPbkChange: (String) -> Unit,
    sid: String,
    onSidChange: (String) -> Unit,
    fp: String,
    onFpChange: (String) -> Unit,
    spx: String,
    onSpxChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = pbk,
            onValueChange = onPbkChange,
            label = { Text(string("reality_pubkey"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = sid,
            onValueChange = onSidChange,
            label = { Text(string("reality_shortid"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fp,
            onValueChange = onFpChange,
            label = { Text(string("tls_fingerprint"), color = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = spx,
            onValueChange = onSpxChange,
            label = { Text(string("reality_spiderx"), color = TextGray) },
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
