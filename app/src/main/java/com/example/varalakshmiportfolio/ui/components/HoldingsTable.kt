package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun HoldingsTable(
    positions: List<PositionItem>,
    onExitClick: (PositionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "INDIVIDUAL TICKERS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = "${positions.size} Active",
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Tap row for details",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TICKER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.3f)
                    )
                    Text(
                        text = "% UP/DN",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "VALUE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "X",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (positions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No open positions", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                positions.forEachIndexed { index, position ->
                    HoldingRow(
                        position = position,
                        onExitClick = { onExitClick(position) }
                    )
                    if (index < positions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = DarkCardBorder.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingRow(
    position: PositionItem,
    onExitClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isPositive = position.unrealizedPnl >= 0
    val pnlColor = if (isPositive) ProfitGreen else LossRed
    val pnlBg = if (isPositive) ProfitGreenBg else LossRedBg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Ticker Column
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = position.symbol,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${position.quantity} shares",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // 2. % UP/DOWN Column
            Box(
                modifier = Modifier.weight(1.1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pnlBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = (if (isPositive) "+" else "") +
                                String.format(Locale.US, "%.2f%%", position.unrealizedPnlPct),
                        color = pnlColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // 3. VALUE Column
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "₹" + String.format(Locale.US, "%,.0f", position.marketValue),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@ ₹" + String.format(Locale.US, "%.1f", position.currentPrice),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // 4. X Column (Multiplier + Exit Button 'X')
            Row(
                modifier = Modifier.weight(0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Multiplier Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentIndigoBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = position.multiplierString,
                        color = AccentIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Interactive 'X' Exit Button
                IconButton(
                    onClick = onExitClick,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(LossRedBg)
                        .border(1.dp, LossRed.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit / Close Position",
                        tint = LossRedLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Expanded Forensic Details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(DarkCard, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Entry Price", value = "₹" + String.format(Locale.US, "%.2f", position.entryPrice))
                    DetailItem(label = "Peak Price", value = "₹" + String.format(Locale.US, "%.2f", position.peakPrice))
                    DetailItem(
                        label = "Unrealized P&L",
                        value = (if (isPositive) "+₹" else "-₹") + String.format(Locale.US, "%,.2f", kotlin.math.abs(position.unrealizedPnl)),
                        valueColor = pnlColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Entry Timestamp", value = position.entryDate)
                    DetailItem(
                        label = "Trail Stop (-8%)",
                        value = "₹" + String.format(Locale.US, "%.2f", position.peakPrice * 0.92),
                        valueColor = GoldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
