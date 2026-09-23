package com.example.varalakshmiportfolio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
import com.example.varalakshmiportfolio.model.RecommendationHistorySummary
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun RecommendationHistoryDialog(
    history: List<HistoricalRecommendationItem>,
    summary: RecommendationHistorySummary,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = history.size
    val winCount = history.count { it.pnlPercent > 0.0 }
    val lossCount = history.count { it.pnlPercent < 0.0 }
    val activeCount = history.count { it.isActive }

    val filteredList = when (selectedFilter.uppercase(Locale.US)) {
        "PROFITABLE", "WINS" -> history.filter { it.pnlPercent > 0.0 }
        "LOSS", "LOSSES" -> history.filter { it.pnlPercent < 0.0 }
        "ACTIVE" -> history.filter { it.isActive }
        else -> history
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentIndigoBg,
                            border = BorderStroke(1.dp, AccentIndigoLight.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "History",
                                tint = AccentIndigoLight,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RECOMMENDATION TRACK RECORD",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                text = "Past 3 Months • Varalakshmi Alpha Engine",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkCard, CircleShape)
                            .border(1.dp, DarkCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KPI Metric Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiSummaryBox(
                        title = "WIN RATE",
                        value = summary.formattedWinRate,
                        valueColor = ProfitGreen,
                        subtitle = "${summary.profitableTrades} of ${summary.totalTrades}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiSummaryBox(
                        title = "AVG RETURN",
                        value = summary.formattedAvgReturn,
                        valueColor = if (summary.avgReturnPercent >= 0) ProfitGreen else LossRed,
                        subtitle = "Per Signal",
                        modifier = Modifier.weight(1f)
                    )
                    KpiSummaryBox(
                        title = "BEST TRADE",
                        value = "+${String.format(Locale.US, "%.1f%%", summary.bestTradePercent)}",
                        valueColor = GoldAccent,
                        subtitle = "Max Upside",
                        modifier = Modifier.weight(1f)
                    )
                    KpiSummaryBox(
                        title = "MAX LOSS",
                        value = "${String.format(Locale.US, "%.1f%%", summary.maxLossPercent)}",
                        valueColor = LossRed,
                        subtitle = "Stop Bound",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChipItem(
                        label = "All ($totalCount)",
                        isSelected = selectedFilter.equals("ALL", ignoreCase = true),
                        onClick = { onFilterChange("ALL") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "Wins ($winCount)",
                        isSelected = selectedFilter.equals("PROFITABLE", ignoreCase = true) || selectedFilter.equals("WINS", ignoreCase = true),
                        selectedColor = ProfitGreen,
                        onClick = { onFilterChange("PROFITABLE") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "Loss ($lossCount)",
                        isSelected = selectedFilter.equals("LOSS", ignoreCase = true) || selectedFilter.equals("LOSSES", ignoreCase = true),
                        selectedColor = LossRed,
                        onClick = { onFilterChange("LOSS") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipItem(
                        label = "Active ($activeCount)",
                        isSelected = selectedFilter.equals("ACTIVE", ignoreCase = true),
                        selectedColor = AccentIndigoLight,
                        onClick = { onFilterChange("ACTIVE") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Historical Recommendations
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recommendations match the filter",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            HistoricalRecommendationCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryBox(
    title: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = AccentIndigoLight,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.2f) else DarkCard,
        border = BorderStroke(
            1.dp,
            if (isSelected) selectedColor.copy(alpha = 0.6f) else DarkCardBorder
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) selectedColor else TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoricalRecommendationCard(
    item: HistoricalRecommendationItem,
    modifier: Modifier = Modifier
) {
    val isWin = item.pnlPercent > 0.0
    val pnlColor = if (isWin) ProfitGreen else if (item.pnlPercent < 0) LossRed else TextSecondary
    val pnlBg = if (isWin) ProfitGreenBg else if (item.pnlPercent < 0) LossRedBg else DarkSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Symbol, Sector & Return Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.symbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface,
                        border = BorderStroke(0.8.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = item.sector,
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pnlBg,
                    border = BorderStroke(1.dp, pnlColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWin) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = pnlColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.formattedPnlPercent,
                            color = pnlColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Entry Date & Price -> Exit Date & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENTRY: ${item.entryDate}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.formattedEntryPrice,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "→",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val exitLabel = if (item.exitDate != null) "EXIT: ${item.exitDate}" else "CURRENT (ACTIVE)"
                    Text(
                        text = exitLabel,
                        color = if (item.isActive) AccentIndigoLight else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.formattedExitPrice,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = DarkCardBorder.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Holding Duration & Trigger Reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Risk Trigger",
                        tint = when {
                            item.isActive -> AccentIndigoLight
                            isWin -> ProfitGreen
                            else -> LossRed
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = item.exitReason.ifBlank { item.status },
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${item.holdingDays}d held",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
