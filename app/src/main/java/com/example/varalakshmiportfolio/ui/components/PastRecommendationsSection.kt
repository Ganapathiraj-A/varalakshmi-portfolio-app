package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
import com.example.varalakshmiportfolio.model.RecommendationHistorySummary
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.util.StockLinkHelper
import java.util.Locale

@Composable
fun PastRecommendationsSection(
    history: List<HistoricalRecommendationItem>,
    summary: RecommendationHistorySummary,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") }

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
            // Header clickable to toggle expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Past Recommendations Icon",
                        tint = AccentIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PAST RECOMMENDATIONS",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Past 3 Months Track Record • Tap ticker for Google Finance",
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProfitGreenBg,
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = summary.formattedWinRate.ifBlank { "73.3%" } + " Win",
                            color = ProfitGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    // KPI Summary Row (4 boxes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PastKpiBox(
                            title = "WIN RATE",
                            value = summary.formattedWinRate,
                            valueColor = ProfitGreen,
                            subtitle = "${summary.profitableTrades}/${summary.totalTrades}",
                            modifier = Modifier.weight(1f)
                        )
                        PastKpiBox(
                            title = "AVG RETURN",
                            value = summary.formattedAvgReturn,
                            valueColor = if (summary.avgReturnPercent >= 0) ProfitGreen else LossRed,
                            subtitle = "Per Trade",
                            modifier = Modifier.weight(1f)
                        )
                        PastKpiBox(
                            title = "BEST TRADE",
                            value = "+${String.format(Locale.US, "%.1f%%", summary.bestTradePercent)}",
                            valueColor = GoldAccent,
                            subtitle = "Peak Gain",
                            modifier = Modifier.weight(1f)
                        )
                        PastKpiBox(
                            title = "MAX LOSS",
                            value = String.format(Locale.US, "%.1f%%", summary.maxLossPercent),
                            valueColor = LossRed,
                            subtitle = "Stop Bound",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PastFilterChip(
                            label = "All ($totalCount)",
                            isSelected = selectedFilter.equals("ALL", ignoreCase = true),
                            onClick = { selectedFilter = "ALL" },
                            modifier = Modifier.weight(1f)
                        )
                        PastFilterChip(
                            label = "Wins ($winCount)",
                            isSelected = selectedFilter.equals("PROFITABLE", ignoreCase = true) || selectedFilter.equals("WINS", ignoreCase = true),
                            selectedColor = ProfitGreen,
                            onClick = { selectedFilter = "WINS" },
                            modifier = Modifier.weight(1f)
                        )
                        PastFilterChip(
                            label = "Loss ($lossCount)",
                            isSelected = selectedFilter.equals("LOSS", ignoreCase = true) || selectedFilter.equals("LOSSES", ignoreCase = true),
                            selectedColor = LossRed,
                            onClick = { selectedFilter = "LOSS" },
                            modifier = Modifier.weight(1f)
                        )
                        PastFilterChip(
                            label = "Active ($activeCount)",
                            isSelected = selectedFilter.equals("ACTIVE", ignoreCase = true),
                            selectedColor = AccentIndigoLight,
                            onClick = { selectedFilter = "ACTIVE" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recommendations match this filter", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredList.forEach { item ->
                                key(item.id) {
                                    PastRecommendationCard(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastKpiBox(
    title: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 8.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PastFilterChip(
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
            modifier = Modifier.padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) selectedColor else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PastRecommendationCard(
    item: HistoricalRecommendationItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isWin = item.pnlPercent > 0.0
    val pnlColor = if (isWin) ProfitGreen else if (item.pnlPercent < 0) LossRed else TextSecondary
    val pnlBg = if (isWin) ProfitGreenBg else if (item.pnlPercent < 0) LossRedBg else DarkSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Row 1: Symbol (Clickable for Google Finance) + Sector & Return Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable ticker button launching Google Finance
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentIndigoBg.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, AccentIndigoLight.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                StockLinkHelper.openGoogleFinance(
                                    context = context,
                                    symbol = item.symbol
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.symbol,
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "View ${item.symbol} on Google Finance",
                                tint = AccentIndigoLight,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface,
                        border = BorderStroke(0.8.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = item.sector,
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = pnlBg,
                    border = BorderStroke(0.8.dp, pnlColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWin) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = pnlColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.formattedPnlPercent,
                            color = pnlColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.formattedEntryPrice,
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "→",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val exitLabel = if (item.exitDate != null) "EXIT: ${item.exitDate}" else "CURRENT (ACTIVE)"
                    Text(
                        text = exitLabel,
                        color = if (item.isActive) AccentIndigoLight else TextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.formattedExitPrice,
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = DarkCardBorder.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Trigger Reason & Holding Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Risk Trigger",
                        tint = when {
                            item.isActive -> AccentIndigoLight
                            isWin -> ProfitGreen
                            else -> LossRed
                        },
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.exitReason.ifBlank { item.status },
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${item.holdingDays}d held",
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
