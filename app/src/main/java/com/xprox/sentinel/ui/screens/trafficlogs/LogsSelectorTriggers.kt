package com.xprox.sentinel.ui.screens.trafficlogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
fun LogsSelectorTriggers(
    showSessionSelector: Boolean,
    selectedSessionName: String,
    onSessionClick: () -> Unit,
    showAppSelector: Boolean,
    selectedAppName: String,
    onAppClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cyber Session Selector Trigger
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = DarkCard,
                contentColor = TextWhite
            ),
            border = BorderStroke(1.dp, if (showSessionSelector) CyberTeal else CardBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .clickable { onSessionClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = string("filter_session"),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedSessionName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "▼",
                    color = CyberTeal,
                    fontSize = 10.sp
                )
            }
        }

        // Cyber Application Selector Trigger
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = DarkCard,
                contentColor = TextWhite
            ),
            border = BorderStroke(1.dp, if (showAppSelector) CyberTeal else CardBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .clickable { onAppClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = string("filter_app"),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedAppName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "▼",
                    color = CyberTeal,
                    fontSize = 10.sp
                )
            }
        }
    }
}
