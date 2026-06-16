package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // Filters and Controls
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = filterSensitiveOnly,
            onCheckedChange = { isChecked ->
                onFilterSensitiveChanged(isChecked)
            },
            colors = CheckboxDefaults.colors(checkedColor = CyberTeal)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = string("sensitive_only"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Export Button
        Button(
            onClick = { onExportClick() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(
                text = string("export_logs"),
                fontSize = 12.sp,
                color = CyberTeal,
                fontWeight = FontWeight.Bold
            )
        }

        // Clear Logs Button
        Button(
            onClick = { onClearClick() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(
                text = string("clear_logs"),
                fontSize = 12.sp,
                color = WarningRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
