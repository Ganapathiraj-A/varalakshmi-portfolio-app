package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
    otherPositions: List<PositionItem> = emptyList(),
    modifier: Modifier = Modifier,
    summaryTodayPnl: Double? = null
) {
    val allPositions = remember(positions, otherPositions) {
        val seen = mutableSetOf<String>()
        val combined = mutableListOf<PositionItem>()
        for (p in positions) {
            val key = p.symbol.trim().uppercase(Locale.US)
            if (key.isNotEmpty() && seen.add(key)) {
                combined.add(p)
            }
        }
        for (p in otherPositions) {
            val key = p.symbol.trim().uppercase(Locale.US)
            if (key.isNotEmpty() && seen.add(key)) {
                combined.add(p)
            }
        }
        combined
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "TODAY'S TICKER MOVEMENTS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = "${allPositions.size} Active",
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = if (otherPositions.isNotEmpty()) "Strategy + Zerodha" else "Daily Delta",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.30f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "PRICE",
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.95f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "TODAY CHG (%)",
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.25f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "DAY & TOT P&L",
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.20f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (allPositions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active ticker movements", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                allPositions.forEachIndexed { index, position ->
                    key(position.positionId) {
                        TodayTickerRow(position = position)
                        if (index < allPositions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = DarkCardBorder.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }

                // Strategy Delta calculation
                val stratTodayValChg = java.math.BigDecimal.valueOf(positions.sumOf { it.todayValueChange })
                    .setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
                val isStratValPositive = stratTodayValChg > 0
                val isStratValNegative = stratTodayValChg < 0

                val stratValColor = when {
                    isStratValPositive -> ProfitGreen
                    isStratValNegative -> LossRed
                    else -> TextSecondary
                }

                val stratValChgStr = when {
                    isStratValPositive -> "+₹" + String.format(Locale.US, "%,.2f", stratTodayValChg)
                    isStratValNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(stratTodayValChg))
                    else -> "₹0.00"
                }

                val isHeaderMatched = summaryTodayPnl == null || kotlin.math.abs(stratTodayValChg - summaryTodayPnl) < 0.005

                // Total Zerodha (all positions combined) Delta calculation
                val totalTodayValChg = java.math.BigDecimal.valueOf(allPositions.sumOf { it.todayValueChange })
                    .setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
                val isTotalValPositive = totalTodayValChg > 0
                val isTotalValNegative = totalTodayValChg < 0

                val totalValColor = when {
                    isTotalValPositive -> ProfitGreen
                    isTotalValNegative -> LossRed
                    else -> TextSecondary
                }

                val totalValChgStr = when {
                    isTotalValPositive -> "+₹" + String.format(Locale.US, "%,.2f", totalTodayValChg)
                    isTotalValNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(totalTodayValChg))
                    else -> "₹0.00"
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = DarkCardBorder.copy(alpha = 0.8f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Strategy Delta Row (strictly matching the header card)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STRATEGY TODAY'S DELTA",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isHeaderMatched) ProfitGreenBg else LossRedBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isHeaderMatched) ProfitGreen.copy(alpha = 0.4f) else LossRed.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = if (isHeaderMatched) "Header Match ✓" else "Delta Mismatch",
                                        color = if (isHeaderMatched) ProfitGreen else LossRed,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = stratValChgStr,
                                color = stratValColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // 2. Total Zerodha Delta (if non-strategy positions exist)
                    if (otherPositions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TOTAL ZERODHA ACCOUNT DELTA",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${allPositions.size} holdings)",
                                        color = TextMuted,
                                        fontSize = 9.5.sp
                                    )
                                }

                                Text(
                                    text = totalValChgStr,
                                    color = totalValColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // 3. Total Account Overall Profit / Loss (All Positions Combined)
                    val totalAccountUnrealizedPnl = java.math.BigDecimal.valueOf(allPositions.sumOf { it.unrealizedPnl })
                        .setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
                    val totalAccountInvested = java.math.BigDecimal.valueOf(allPositions.sumOf { it.quantity * it.entryPrice })
                        .setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
                    val totalAccountPnlPct = if (totalAccountInvested > 0.0) {
                        java.math.BigDecimal.valueOf((totalAccountUnrealizedPnl / totalAccountInvested) * 100.0)
                            .setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
                    } else 0.0

                    val isAcctPnlPos = totalAccountUnrealizedPnl > 0
                    val isAcctPnlNeg = totalAccountUnrealizedPnl < 0
                    val acctPnlColor = when {
                        isAcctPnlPos -> ProfitGreen
                        isAcctPnlNeg -> LossRed
                        else -> TextSecondary
                    }
                    val acctPnlBg = when {
                        isAcctPnlPos -> ProfitGreenBg
                        isAcctPnlNeg -> LossRedBg
                        else -> DarkCard
                    }
                    val acctPnlStr = when {
                        isAcctPnlPos -> "+₹" + String.format(Locale.US, "%,.2f", totalAccountUnrealizedPnl)
                        isAcctPnlNeg -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(totalAccountUnrealizedPnl))
                        else -> "₹0.00"
                    }
                    val acctPnlPctStr = when {
                        isAcctPnlPos || isAcctPnlNeg -> String.format(Locale.US, "(%+.2f%%)", totalAccountPnlPct)
                        else -> "(0.00%)"
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = acctPnlBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, acctPnlColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL ACCOUNT PROFIT / LOSS",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Invested: ₹" + String.format(Locale.US, "%,.2f", totalAccountInvested) +
                                           " • Current: ₹" + String.format(Locale.US, "%,.2f", allPositions.sumOf { it.marketValue }),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = acctPnlStr,
                                    color = acctPnlColor,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = acctPnlPctStr,
                                    color = acctPnlColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
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
    val isPositive = position.todayPriceChange > 0
    val isNegative = position.todayPriceChange < 0
    val pnlColor = when {
        isPositive -> ProfitGreen
        isNegative -> LossRed
        else -> TextSecondary
    }
    val pnlBg = when {
        isPositive -> ProfitGreenBg
        isNegative -> LossRedBg
        else -> DarkCard
    }

    val isValPositive = position.todayValueChange > 0
    val isValNegative = position.todayValueChange < 0
    val valColor = when {
        isValPositive -> ProfitGreen
        isValNegative -> LossRed
        else -> TextSecondary
    }

    val priceChgStr = when {
        isPositive -> "+₹" + String.format(Locale.US, "%.2f", position.todayPriceChange)
        isNegative -> "-₹" + String.format(Locale.US, "%.2f", kotlin.math.abs(position.todayPriceChange))
        else -> "₹0.00"
    }
    val priceChgPctStr = when {
        isPositive || isNegative -> String.format(Locale.US, "(%+.2f%%)", position.todayPriceChangePct)
        else -> "(0.00%)"
    }

    val valChgStr = when {
        isValPositive -> "+₹" + String.format(Locale.US, "%,.2f", position.todayValueChange)
        isValNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(position.todayValueChange))
        else -> "₹0.00"
    }

    val isTotalPositive = position.unrealizedPnl > 0
    val isTotalNegative = position.unrealizedPnl < 0
    val totalPnlColor = when {
        isTotalPositive -> ProfitGreen
        isTotalNegative -> LossRed
        else -> TextSecondary
    }
    val totalPnlBg = when {
        isTotalPositive -> ProfitGreenBg
        isTotalNegative -> LossRedBg
        else -> DarkCard
    }
    val totalPnlStr = when {
        isTotalPositive -> "+₹" + String.format(Locale.US, "%,.2f", position.unrealizedPnl)
        isTotalNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(position.unrealizedPnl))
        else -> "₹0.00"
    }
    val totalPnlPctStr = when {
        isTotalPositive || isTotalNegative -> String.format(Locale.US, "(%+.2f%%)", position.unrealizedPnlPct)
        else -> "(0.00%)"
    }

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
            // 1. TICKER Column (Click to open Google Finance Chart in Chrome)
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(
                modifier = Modifier
                    .weight(1.30f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        com.example.varalakshmiportfolio.ui.util.StockLinkHelper.openGoogleFinance(
                            context = context,
                            symbol = position.symbol
                        )
                    }
                    .padding(vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = position.symbol,
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "View Chart on Google Finance",
                        tint = AccentIndigoLight.copy(alpha = 0.7f),
                        modifier = Modifier.size(10.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${position.quantity} shs",
                        color = TextMuted,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!position.isStrategy) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleGrey40.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, PurpleGrey40.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "OTHER",
                                color = Purple80,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
            }

            // 2. PRICE Column (LTP & reference price)
            Column(modifier = Modifier.weight(0.95f)) {
                Text(
                    text = "₹" + String.format(Locale.US, "%.2f", position.currentPrice),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ref: ₹" + String.format(Locale.US, "%.2f", position.referencePrice),
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
                        Text(
                            text = priceChgStr,
                            color = pnlColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = priceChgPctStr,
                            color = pnlColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. DAY & TOTAL P&L Column
            Column(
                modifier = Modifier.weight(1.20f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = valChgStr,
                    color = valColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$totalPnlStr $totalPnlPctStr",
                    color = totalPnlColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
                        label = if (position.isBoughtToday) "Baseline Source" else "Previous Close",
                        value = if (position.isBoughtToday) "Entry Execution (Bought Today)"
                                else if (position.previousClose > 0.0) "₹" + String.format(Locale.US, "%.2f", position.previousClose)
                                else "N/A (Entry ref)"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(
                        label = "Position Type",
                        value = if (position.isStrategy) "VaraLakshmi Strategy" else "Other Zerodha Holding",
                        valueColor = if (position.isStrategy) AccentIndigoLight else Purple80
                    )
                    DetailItem(
                        label = "Total Market Value",
                        value = "₹" + String.format(Locale.US, "%,.2f", position.marketValue)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated Total Profit / Loss Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = totalPnlBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        totalPnlColor.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL PROFIT / LOSS",
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Avg Buy: ₹" + String.format(Locale.US, "%.2f", position.entryPrice) +
                                       " • Invested: ₹" + String.format(Locale.US, "%,.2f", position.quantity * position.entryPrice),
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = totalPnlStr,
                                color = totalPnlColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = totalPnlPctStr,
                                color = totalPnlColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                        }
                    }
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
