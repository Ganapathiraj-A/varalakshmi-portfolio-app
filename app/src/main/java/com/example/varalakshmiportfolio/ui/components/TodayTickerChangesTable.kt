package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun TodayTickerChangesTable(
    positions: List<PositionItem>,
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
                        text = "TODAY'S TICKER MOVEMENTS",
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
                    text = "Daily Delta",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Bar: TICKER | PRICE | TODAY CHG (%) | TODAY VALUE (₹)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TICKER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.30f)
                    )
                    Text(
                        text = "PRICE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = "TODAY CHG (%)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.25f)
                    )
                    Text(
                        text = "TODAY VALUE (₹)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.20f)
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
                    Text("No active ticker movements", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                positions.forEachIndexed { index, position ->
                    key(position.positionId) {
                        TodayTickerRow(position = position)
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
}

@Composable
private fun TodayTickerRow(
    position: PositionItem
) {
    var expanded by remember { mutableStateOf(false) }
    val isPositive = position.todayPriceChange >= 0
    val pnlColor = if (isPositive) ProfitGreen else LossRed
    val pnlBg = if (isPositive) ProfitGreenBg else LossRedBg

    val isValPositive = position.todayValueChange >= 0
    val valColor = if (isValPositive) ProfitGreen else LossRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. TICKER Column
            Column(modifier = Modifier.weight(1.30f)) {
                Text(
                    text = position.symbol,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${position.quantity} shares",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }

            // 2. PRICE Column (LTP & reference price)
            Column(modifier = Modifier.weight(0.95f)) {
                Text(
                    text = "₹" + String.format(Locale.US, "%.2f", position.currentPrice),
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Ref: ₹" + String.format(Locale.US, "%.2f", position.referencePrice),
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // 3. TODAY CHG (%) Column (₹ diff and % change in badge)
            Box(
                modifier = Modifier.weight(1.25f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = pnlBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val priceChgStr = (if (isPositive) "+₹" else "-₹") +
                                String.format(Locale.US, "%.2f", kotlin.math.abs(position.todayPriceChange))
                        Text(
                            text = priceChgStr,
                            color = pnlColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = String.format(Locale.US, "(%+.2f%%)", position.todayPriceChangePct),
                            color = pnlColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. TODAY VALUE (₹) Column (Quantity × Today's Price Change)
            Column(
                modifier = Modifier.weight(1.20f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = (if (isValPositive) "+₹" else "-₹") +
                            String.format(Locale.US, "%,.2f", kotlin.math.abs(position.todayValueChange)),
                    color = valColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                Text(
                    text = "Day P&L",
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }

        // Expanded Forensic Calculation Breakdown
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
                    DetailItem(
                        label = "Reference Price",
                        value = "₹" + String.format(Locale.US, "%.2f", position.referencePrice)
                    )
                    DetailItem(
                        label = "Price Change",
                        value = (if (isPositive) "+₹" else "-₹") + String.format(Locale.US, "%.2f", kotlin.math.abs(position.todayPriceChange)),
                        valueColor = pnlColor
                    )
                    DetailItem(
                        label = "Today's Value Δ",
                        value = (if (isValPositive) "+₹" else "-₹") + String.format(Locale.US, "%,.2f", kotlin.math.abs(position.todayValueChange)),
                        valueColor = valColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formulaPriceStr = (if (isPositive) "₹" else "-₹") +
                            String.format(Locale.US, "%.2f", kotlin.math.abs(position.todayPriceChange))
                    DetailItem(
                        label = "Calculation Formula",
                        value = "${position.quantity} shs × $formulaPriceStr"
                    )
                    DetailItem(
                        label = "Previous Close",
                        value = if (position.previousClose > 0.0) "₹" + String.format(Locale.US, "%.2f", position.previousClose) else "N/A (Entry ref)"
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
