package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprox.sentinel.data.string
import com.xprox.sentinel.theme.*

@Composable
fun LogsActionControls(
    filterSensitiveOnly: Boolean,
    onFilterSensitiveChanged: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onClearClick: () -> Unit
) {
    // Sensitive Ports Filter Checkbox
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = filterSensitiveOnly,
            onCheckedChange = onFilterSensitiveChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = WarningRose,
                uncheckedColor = TextGray,
                checkmarkColor = TextWhite
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = string("sensitive_only"),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (filterSensitiveOnly) WarningRose else TextWhite,
            fontFamily = FontFamily.Monospace
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Export Logs Button
        Button(
            onClick = onExportClick,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = string("export_logs"),
                fontSize = 11.5.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Clear Logs Button
        OutlinedButton(
            onClick = onClearClick,
            border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRose),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = WarningRose,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = string("clear_logs"),
                fontSize = 11.5.sp,
                color = WarningRose,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
