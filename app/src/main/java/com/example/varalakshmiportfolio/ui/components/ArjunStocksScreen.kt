package com.example.varalakshmiportfolio.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.model.ArjunBreakoutOrder
import com.example.varalakshmiportfolio.model.ArjunLadderLevel
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunSlotStatus
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunStocksUiState
import com.example.varalakshmiportfolio.model.ArjunStrategySlot
import com.example.varalakshmiportfolio.model.ArjunStrategySummary
import com.example.varalakshmiportfolio.model.ArjunTelegramMessage
import com.example.varalakshmiportfolio.model.ArjunTradeStatus
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.util.StockLinkHelper
import java.util.Locale

@Composable
fun ArjunStocksScreen(
    state: ArjunStocksUiState,
    onAddRecommendation: (String) -> Unit,
    onUpdateStock: (ArjunStockItem) -> Unit,
    onDeleteStock: (String) -> Unit,
    onCloseStock: (id: String, exitPrice: Double) -> Unit,
    onSelectFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onApproveStock: (String) -> Unit = {},
    onUnapproveStock: (String) -> Unit = {},
    onDismissExecutionEvent: (String) -> Unit = {},
    onSelectSlot: (Int?) -> Unit = {},
    onPartialBuy: (id: String, quantity: Int, price: Double) -> Unit = { _, _, _ -> },
    onPartialSell: (id: String, quantity: Int, price: Double) -> Unit = { _, _, _ -> },
    onSetBreakoutOrder: (ArjunBreakoutOrder) -> Unit = {},
    onCancelBreakoutOrder: (slotIndex: Int) -> Unit = {},
    onUpdateStockLadders: (id: String, targetLadder: List<ArjunLadderLevel>, stopLossLadder: List<ArjunLadderLevel>) -> Unit = { _, _, _ -> },
    onRefreshTelegram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Auto-Execution Alert Banner (Triggered automated sells)
        if (state.autoExecutionEvents.isNotEmpty()) {
            ArjunAutoExecutionBanner(
                events = state.autoExecutionEvents,
                onDismiss = onDismissExecutionEvent
            )
        }

        // Real Server Connection Error Banner (if connection failed)
        if (state.errorMessage != null) {
            ArjunServerErrorBanner(
                errorMessage = state.errorMessage,
                onRetry = onRefresh
            )
        }

        // Real Server Status & Sync Header (Live Server Connection, Broker, Timestamp, Refresh)
        ArjunServerStatusHeader(
            isLive = state.errorMessage == null && state.isLiveSync,
            serverStatus = state.serverStatus,
            broker = state.broker,
            lastUpdated = state.lastRefreshedTime,
            isRefreshing = state.isRefreshingQuotes,
            onRefresh = onRefresh
        )

        // 1. Arjun Strategy Allocation Matrix Card (Dynamic 7 or 10-Slot Matrix)
        ArjunStrategySlotMatrixCard(
            summary = state.strategySummary,
            slots = state.strategySlots,
            selectedSlotIndex = state.selectedSlotIndex,
            onSelectSlot = { slotIndex ->
                onSelectSlot(if (state.selectedSlotIndex == slotIndex) null else slotIndex)
            }
        )

        // 1b. Dynamic Slot Configuration & Action Card (Rendered directly below the matrix)
        val selectedSlotIdx = state.selectedSlotIndex
        if (selectedSlotIdx != null) {
            val selectedSlot = state.strategySlots.firstOrNull { it.slotIndex == selectedSlotIdx }
                ?: ArjunStrategySlot(slotIndex = selectedSlotIdx, targetCapital = state.strategySummary.slotBudget)

            ArjunSlotConfigurationActionCard(
                slotIndex = selectedSlotIdx,
                slot = selectedSlot,
                onCloseSlotCard = { onSelectSlot(null) },
                onSetBreakoutOrder = onSetBreakoutOrder,
                onCancelBreakoutOrder = { onCancelBreakoutOrder(selectedSlotIdx) },
                onPartialBuy = onPartialBuy,
                onPartialSell = onPartialSell,
                onFullSell = { stock -> onCloseStock(stock.id, stock.currentPrice) },
                onUpdateLadders = onUpdateStockLadders
            )
        }

        // 2. Today's Telegram Messages Feed (Dynamically updated, latest first)
        ArjunTelegramChannelCard(
            messages = state.telegramMessages,
            todayCount = state.telegramTodayCount,
            channel = state.telegramChannel,
            isSyncing = state.isTelegramSyncing,
            onRefresh = onRefreshTelegram
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 1. Header & Summary KPI Card
// ─────────────────────────────────────────────────────────────
@Composable
private fun ArjunHeaderSummaryCard(
    summary: ArjunPortfolioSummary,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenPasteDialog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentIndigoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏹", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ARJUN STOCKS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "ARJUN Combo Package 🚀 & Broker Picks",
                            fontSize = 10.sp,
                            color = AccentIndigoLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isRefreshing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "arjun_spin")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spin_angle"
                        )
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refreshing Quotes",
                            tint = GoldAccent,
                            modifier = Modifier.graphicsLayer { rotationZ = angle }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Quotes",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Primary Metrics Grid: Invested, Value, Total P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Invested
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "INVESTED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = summary.formattedTotalInvested,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Current Value
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "LIVE VALUE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = summary.formattedCurrentValue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Overall P&L
                val pnlColor = if (summary.isProfit) ProfitGreen else LossRed
                val pnlBg = if (summary.isProfit) ProfitGreenBg else LossRedBg
                Surface(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    color = pnlBg,
                    border = BorderStroke(1.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "OVERALL P&L",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = pnlColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = summary.formattedTotalPnl,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = pnlColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = pnlColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = summary.formattedTotalPnlPct,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pnlColor,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Secondary Quick Badges Row: Active, Closed, Target Hits, Win Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "${summary.activeCount} Active",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ProfitGreenBg
                    ) {
                        Text(
                            text = "🎯 ${summary.targetHitCount} Target Hits",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (summary.slHitCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LossRedBg
                        ) {
                            Text(
                                text = "🛑 ${summary.slHitCount} SL Hits",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = LossRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Win Rate: ${String.format(Locale.US, "%.1f%%", summary.winRatePct)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.winRatePct >= 50.0) ProfitGreen else GoldAccent
                )
            }

            // Big "+ Paste Broker Recommendation" Button
            Button(
                onClick = onOpenPasteDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentIndigo
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Paste Broker Recommendation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 2. Filter Chips
// ─────────────────────────────────────────────────────────────
@Composable
private fun ArjunFilterChips(
    selectedFilter: String,
    recommendations: List<ArjunStockItem>,
    onSelectFilter: (String) -> Unit
) {
    val strategyCount = recommendations.count { it.isApprovedForStrategy }
    val activeCount = recommendations.count { !it.isClosed }
    val winCount = recommendations.count { it.isProfit }
    val tgtCount = recommendations.count { it.status == ArjunTradeStatus.TARGET1_HIT || it.status == ArjunTradeStatus.TARGET2_HIT }
    val closedCount = recommendations.count { it.isClosed }

    val filters = listOf(
        "ALL" to recommendations.size,
        "STRATEGY" to strategyCount,
        "ACTIVE" to activeCount,
        "WINS" to winCount,
        "TARGET" to tgtCount,
        "CLOSED" to closedCount
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filters.forEach { (label, count) ->
            key(label) {
                val isSelected = selectedFilter.equals(label, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFilter(label) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AccentIndigoBg else DarkCard,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentIndigo else DarkCardBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentIndigoLight else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "$count",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AccentIndigo else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 3. Stock Card
// ─────────────────────────────────────────────────────────────
@Composable
private fun ArjunStockCard(
    stock: ArjunStockItem,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onOpenChart: () -> Unit,
    onApprove: () -> Unit = {},
    onUnapprove: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, if (stock.status == ArjunTradeStatus.TARGET1_HIT || stock.status == ArjunTradeStatus.TARGET2_HIT) ProfitGreen.copy(alpha = 0.5f) else DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Symbol, Company & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (stock.action == "BUY") ProfitGreenBg else LossRedBg
                        ) {
                            Text(
                                text = stock.action,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stock.action == "BUY") ProfitGreen else LossRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AccentIndigoBg
                        ) {
                            Text(
                                text = stock.tradeType,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentIndigoLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (stock.companyName.isNotBlank()) {
                        Text(
                            text = stock.companyName,
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge
                ArjunStatusBadge(status = stock.status)
            }

            // Strategy Slot Badge if Approved
            if (stock.isApprovedForStrategy) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (stock.isClosed) DarkCard else Color(0xFF0F3020),
                    border = BorderStroke(0.8.dp, if (stock.isClosed) DarkCardBorder else ProfitGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (stock.isClosed) Icons.Filled.Info else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (stock.isClosed) TextMuted else ProfitGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    stock.isClosed && stock.strategySlotIndex in 1..7 -> "STRATEGY POSITION (CLOSED - FREED SLOT #${stock.strategySlotIndex})"
                                    stock.isClosed -> "STRATEGY POSITION (CLOSED)"
                                    stock.strategySlotIndex in 1..7 -> "STRATEGY SLOT #${stock.strategySlotIndex} ACTIVE (~₹1,00,000)"
                                    else -> "STRATEGY POSITION"
                                },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stock.isClosed) TextSecondary else ProfitGreen
                            )
                        }
                        if (!stock.isClosed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🤖 Auto-Sell Active",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentIndigoLight
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Free Slot",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier
                                        .clickable { onUnapprove() }
                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Closed Exit Reason Badge
            if (stock.isClosed && stock.exitReason.isNotBlank()) {
                val (exitText, exitColor, exitBg) = when (stock.exitReason) {
                    "TARGET_HIT" -> Triple("🎯 AUTO-SOLD: Target 1 Hit at ₹${stock.closedPrice}. Profit Booked & Slot Freed!", ProfitGreen, ProfitGreenBg)
                    "STOP_LOSS_HIT" -> Triple("🛑 AUTO-SOLD: Stop Loss Hit at ₹${stock.closedPrice}. Slot Freed!", LossRed, LossRedBg)
                    else -> Triple("🔒 Position Exited at ₹${stock.closedPrice}", GoldAccent, Color(0xFF2A2210))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = exitBg,
                    border = BorderStroke(0.8.dp, exitColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = exitText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = exitColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

            // Row 2: Price Grid (Entry vs Current) & P&L Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Entry & Current Price
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stock.formattedCurrentPrice,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val isDayPos = stock.todayChangePct >= 0.0
                        val dayColor = if (isDayPos) ProfitGreen else LossRed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDayPos) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = dayColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%s%.2f%%", if (isDayPos) "+" else "", stock.todayChangePct),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = dayColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Entry: ${stock.formattedEntryPrice}" + if (stock.entryPriceRange.isNotBlank()) " (${stock.entryPriceRange})" else "",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // P&L Badge
                Column(horizontalAlignment = Alignment.End) {
                    val pnlColor = if (stock.isProfit) ProfitGreen else LossRed
                    val pnlBg = if (stock.isProfit) ProfitGreenBg else LossRedBg
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pnlBg,
                        border = BorderStroke(0.8.dp, pnlColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = stock.formattedPnl,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = pnlColor,
                                maxLines = 1
                            )
                            Text(
                                text = stock.formattedPnlPct,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        }
                    }
                    Text(
                        text = "${stock.quantity} shares • ${String.format(Locale.US, "₹%,.2f", stock.currentValue)}",
                        fontSize = 9.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Row 3: Target & SL Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stop Loss Level
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "STOP LOSS",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stock.formattedStopLoss,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (stock.stopLossLossPct != 0.0) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", stock.stopLossLossPct),
                                fontSize = 8.sp,
                                color = LossRed
                            )
                        }
                    }
                }

                // Target 1
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "TARGET 1",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stock.formattedTarget1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (stock.target1GainPct != 0.0) {
                            Text(
                                text = String.format(Locale.US, "+%.1f%%", stock.target1GainPct),
                                fontSize = 8.sp,
                                color = ProfitGreen
                            )
                        }
                    }
                }

                // Target 2
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "TARGET 2",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stock.formattedTarget2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (stock.target2GainPct != 0.0) {
                            Text(
                                text = String.format(Locale.US, "+%.1f%%", stock.target2GainPct),
                                fontSize = 8.sp,
                                color = GoldAccent
                            )
                        }
                    }
                }
            }

            // Visual Progress to Target 1
            if (stock.stopLoss > 0 && stock.targetPrice1 > stock.stopLoss && !stock.isClosed) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SL ${stock.formattedStopLoss}", fontSize = 8.sp, color = LossRed)
                        Text(text = "TGT1 ${stock.formattedTarget1}", fontSize = 8.sp, color = ProfitGreen)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { stock.progressToTarget1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (stock.isProfit) ProfitGreen else GoldAccent,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }

            // Notes / Advisory
            if (stock.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2433),
                    border = BorderStroke(0.6.dp, GoldAccent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stock.notes,
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Expandable Original Raw Broker Alert
            if (stock.rawText.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide Broker Alert" else "View Original Broker Alert",
                        fontSize = 9.sp,
                        color = AccentIndigoLight,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AccentIndigoLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F1420),
                        border = BorderStroke(0.6.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = stock.rawText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Approve into Strategy Button if not yet approved
            if (!stock.isApprovedForStrategy && !stock.isClosed) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Approve & Buy into Strategy (₹1L Slot)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Actions: Chart, Edit, Close, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart button
                OutlinedButton(
                    onClick = onOpenChart,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "NSE Chart",
                        tint = AccentIndigoLight,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NSE Chart", fontSize = 10.sp, color = AccentIndigoLight)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Stock",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Close Position
                    if (!stock.isClosed) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Exit / Close", fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = LossRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Status Badge Helper
// ─────────────────────────────────────────────────────────────
@Composable
private fun ArjunStatusBadge(status: ArjunTradeStatus) {
    val (text, color, bg) = when (status) {
        ArjunTradeStatus.TARGET2_HIT -> Triple("🎯 TARGET 2 HIT", ProfitGreen, ProfitGreenBg)
        ArjunTradeStatus.TARGET1_HIT -> Triple("🎯 TARGET 1 HIT", ProfitGreen, ProfitGreenBg)
        ArjunTradeStatus.STOP_LOSS_HIT -> Triple("🛑 STOP LOSS HIT", LossRed, LossRedBg)
        ArjunTradeStatus.WAITING_TRIGGER -> Triple("⏳ WAITING TRIGGER", GoldAccent, GoldAccentBg)
        ArjunTradeStatus.PENDING_BREAKOUT -> Triple("🚀 BREAKOUT PENDING", GoldAccent, GoldAccentBg)
        ArjunTradeStatus.CLOSED -> Triple("🔒 CLOSED", TextMuted, Color(0xFF1E293B))
        ArjunTradeStatus.ACTIVE -> Triple("🟢 ACTIVE", AccentIndigoLight, AccentIndigoBg)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg,
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 4. Paste Recommendation Modal Dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun PasteRecommendationDialog(
    onDismiss: () -> Unit,
    onSave: (rawText: String, customQty: Int) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("10") }

    val parsedPreview = remember(rawText) {
        ArjunRecommendationParser.parse(rawText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = null,
                    tint = AccentIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Paste Broker Recommendation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste alerts directly from Telegram, WhatsApp, or broker SMS. The intelligent parser will extract symbol, entry, targets, and stop loss.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                        Text(
                            text = "Example:\nARJUN Combo Package 🚀\nSWING TRADE\nBUY SKYGOLD AROUND 1035-1045\nTARGETS 1175-1250\nKEEP SL 949.95",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentIndigo,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick Sample Fill Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            rawText = "ARJUN Combo Package 🚀\nSWING TRADE\nBUY SKYGOLD AROUND 1035-1045\nTARGETS 1175-1250\nKEEP SL 949.95\nCURRENTLY TRADING 1043"
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Fill Demo Alert", fontSize = 10.sp, color = AccentIndigoLight)
                    }
                }

                // Parser Live Preview Card
                if (parsedPreview != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PARSED: ${parsedPreview.symbol}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreen
                                )
                                Text(
                                    text = "${parsedPreview.action} • ${parsedPreview.tradeType}",
                                    fontSize = 9.5.sp,
                                    color = AccentIndigoLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Entry: ₹${parsedPreview.entryPrice} (${parsedPreview.entryPriceRange})",
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Targets: ₹${parsedPreview.targetPrice1} / ₹${parsedPreview.targetPrice2} | SL: ₹${parsedPreview.stopLoss}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            if (parsedPreview.notes.isNotBlank()) {
                                Text(
                                    text = "Note: ${parsedPreview.notes}",
                                    fontSize = 9.sp,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                } else if (rawText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Parsing in progress... Ensure message mentions BUY <SYMBOL> and TARGETS / SL.",
                            fontSize = 10.sp,
                            color = GoldAccent,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Quantity Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Initial Quantity:", fontSize = 11.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 10
                    onSave(rawText, qty)
                },
                enabled = rawText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Add Recommendation", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// 5. Edit Stock Modal Dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun EditStockDialog(
    stock: ArjunStockItem,
    onDismiss: () -> Unit,
    onSave: (ArjunStockItem) -> Unit
) {
    var symbol by remember { mutableStateOf(stock.symbol) }
    var entryPrice by remember { mutableStateOf(stock.entryPrice.toString()) }
    var target1 by remember { mutableStateOf(if (stock.targetPrice1 > 0) stock.targetPrice1.toString() else "") }
    var target2 by remember { mutableStateOf(if (stock.targetPrice2 > 0) stock.targetPrice2.toString() else "") }
    var stopLoss by remember { mutableStateOf(if (stock.stopLoss > 0) stock.stopLoss.toString() else "") }
    var quantity by remember { mutableStateOf(stock.quantity.toString()) }
    var notes by remember { mutableStateOf(stock.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(text = "Edit ${stock.symbol}", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase(Locale.US) },
                        label = { Text("Symbol", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryPrice,
                        onValueChange = { entryPrice = it },
                        label = { Text("Entry Price (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stopLoss,
                        onValueChange = { stopLoss = it },
                        label = { Text("Stop Loss (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target1,
                        onValueChange = { target1 = it },
                        label = { Text("Target 1 (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target2,
                        onValueChange = { target2 = it },
                        label = { Text("Target 2 (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Advisory Notes", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = stock.copy(
                        symbol = symbol.trim().uppercase(Locale.US),
                        quantity = quantity.toIntOrNull() ?: stock.quantity,
                        entryPrice = entryPrice.toDoubleOrNull() ?: stock.entryPrice,
                        targetPrice1 = target1.toDoubleOrNull() ?: stock.targetPrice1,
                        targetPrice2 = target2.toDoubleOrNull() ?: stock.targetPrice2,
                        stopLoss = stopLoss.toDoubleOrNull() ?: stock.stopLoss,
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// 6. Close Stock Modal Dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun CloseStockDialog(
    stock: ArjunStockItem,
    onDismiss: () -> Unit,
    onConfirmClose: (exitPrice: Double) -> Unit
) {
    var exitPriceText by remember { mutableStateOf(stock.currentPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Close ${stock.symbol} Position",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Record final exit price to close this trade and lock in realized P&L:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = exitPriceText,
                    onValueChange = { exitPriceText = it },
                    label = { Text("Exit Price (₹)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = exitPriceText.toDoubleOrNull() ?: stock.currentPrice
                    onConfirmClose(price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Lock Exit & Close", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// 7. Empty State Card
// ─────────────────────────────────────────────────────────────
@Composable
private fun ArjunEmptyStateCard(
    filter: String,
    onOpenPasteDialog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentIndigoBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏹", fontSize = 24.sp)
            }

            Text(
                text = if (filter == "ALL") "No Arjun Stocks Stored Yet" else "No $filter Recommendations",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Paste broker alerts from Telegram, WhatsApp, or SMS to start tracking their live profit & loss.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onOpenPasteDialog,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste First Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 8. Arjun Telegram Channel Live Feed Card (Today's Messages First)
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunTelegramChannelCard(
    messages: List<ArjunTelegramMessage>,
    todayCount: Int,
    channel: String = "@FlyingcallsArjun",
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("TODAY") }

    val todayMessages = remember(messages) { messages.filter { it.isToday } }
    val displayMessages = if (selectedTab == "TODAY") todayMessages else messages

    val infiniteTransition = rememberInfiniteTransition(label = "tg_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tg_rotation"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Telegram Icon + Title + Live Pulse + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF229ED9).copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, Color(0xFF229ED9).copy(alpha = 0.5f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "✈️", fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TELEGRAM CHANNEL",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF229ED9).copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, Color(0xFF229ED9).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = channel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF229ED9),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (todayCount > 0) "$todayCount alert${if (todayCount > 1) "s" else ""} posted today • Latest first"
                            else "Live Analyst Alerts • Auto-Updating",
                            fontSize = 8.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pulsing LIVE dot
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ProfitGreenBg,
                        border = BorderStroke(0.6.dp, ProfitGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ProfitGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isSyncing,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Telegram",
                            tint = if (isSyncing) AccentBlue else TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    if (isSyncing) rotationZ = rotation
                                }
                        )
                    }
                }
            }

            // Filter Tabs: TODAY'S MESSAGES (N) | ALL RECENT (N)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Today Tab
                val isTodayTabSelected = selectedTab == "TODAY"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isTodayTabSelected) AccentIndigo.copy(alpha = 0.25f) else DarkCard,
                    border = BorderStroke(
                        if (isTodayTabSelected) 1.dp else 0.6.dp,
                        if (isTodayTabSelected) AccentIndigo else DarkCardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "TODAY" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S MESSAGES ($todayCount)",
                            fontSize = 8.5.sp,
                            fontWeight = if (isTodayTabSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isTodayTabSelected) AccentIndigoLight else TextSecondary
                        )
                    }
                }

                // Recent Tab
                val isRecentTabSelected = selectedTab == "RECENT"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRecentTabSelected) AccentIndigo.copy(alpha = 0.25f) else DarkCard,
                    border = BorderStroke(
                        if (isRecentTabSelected) 1.dp else 0.6.dp,
                        if (isRecentTabSelected) AccentIndigo else DarkCardBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = "RECENT" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL RECENT (${messages.size})",
                            fontSize = 8.5.sp,
                            fontWeight = if (isRecentTabSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isRecentTabSelected) AccentIndigoLight else TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Message Cards List or Empty Notice
            if (displayMessages.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCard,
                    border = BorderStroke(0.6.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (selectedTab == "TODAY") "No messages posted today yet" else "No recent messages found",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = if (selectedTab == "TODAY") "The channel has not posted any alerts so far today. New updates will dynamically appear here the moment they are posted."
                            else "Connecting to Telegram channel feed...",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        if (selectedTab == "TODAY" && messages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = { selectedTab = "RECENT" },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("View Recent Posts (${messages.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayMessages.forEach { msg ->
                        key(msg.postId) {
                            ArjunTelegramMessageItemCard(
                                message = msg,
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Could not open link: $url", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArjunTelegramMessageItemCard(
    message: ArjunTelegramMessage,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = BorderStroke(
            if (message.isToday) 1.dp else 0.6.dp,
            if (message.isToday) ProfitGreen.copy(alpha = 0.5f) else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Meta Row: Post ID, Timestamp, Views, Open Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = AccentIndigoBg,
                        border = BorderStroke(0.5.dp, AccentIndigo.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "#${message.postId}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentIndigoLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (message.isToday) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = ProfitGreenBg,
                            border = BorderStroke(0.5.dp, ProfitGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "TODAY",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = message.displayTime,
                        fontSize = 8.sp,
                        color = if (message.isToday) ProfitGreen else TextMuted,
                        fontWeight = if (message.isToday) FontWeight.Bold else FontWeight.Normal
                    )

                    if (message.views.isNotBlank()) {
                        Text(
                            text = "👁 ${message.views}",
                            fontSize = 7.5.sp,
                            color = TextMuted
                        )
                    }
                }

                if (message.rawUrl.isNotBlank()) {
                    IconButton(
                        onClick = { onOpenUrl(message.rawUrl) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Telegram",
                            tint = Color(0xFF229ED9),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Message Body Text
            Text(
                text = message.text,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = TextPrimary,
                fontFamily = FontFamily.Default
            )

            if (message.hasMedia) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DarkSurface,
                    border = BorderStroke(0.5.dp, DarkCardBorder)
                ) {
                    Text(
                        text = "📷 Media / Photo attached",
                        fontSize = 7.5.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 8b. Server Status Header & Error Banner
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunServerErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LossRedBg,
        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = LossRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LIVE SERVER DISCONNECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LossRed,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("RETRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ArjunServerStatusHeader(
    isLive: Boolean,
    serverStatus: String,
    broker: String,
    lastUpdated: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arjun_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(0.8.dp, if (isLive) DarkCardBorder else LossRed.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Live Indicator Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isLive) ProfitGreen else LossRed)
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isLive) "SERVER LIVE ($serverStatus)" else "SERVER: $serverStatus",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLive) ProfitGreen else LossRed
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = AccentIndigoBg,
                            border = BorderStroke(0.5.dp, AccentIndigo.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = broker,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentIndigoLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (lastUpdated.isNotBlank()) {
                        Text(
                            text = "Sync: $lastUpdated (No Local Cache)",
                            fontSize = 8.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = if (isRefreshing) AccentBlue else TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            if (isRefreshing) rotationZ = rotation
                        }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 8b. Arjun Strategy Slot Matrix Card & Dynamic Configuration
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunStrategySlotMatrixCard(
    summary: ArjunStrategySummary,
    slots: List<ArjunStrategySlot>,
    selectedSlotIndex: Int? = null,
    onSelectSlot: (Int) -> Unit = {}
) {
    val totalSlots = if (summary.totalSlots > 0) summary.totalSlots else if (slots.isNotEmpty()) slots.size else 7
    val totalCapitalLakhs = (summary.totalCapital / 100_000).toInt()
    val strategyTitle = if (totalCapitalLakhs > 0) "ARJUN ${totalCapitalLakhs}L STRATEGY" else "ARJUN STRATEGY"
    val slotBudgetLabel = if (summary.slotBudget >= 100_000.0) "₹${(summary.slotBudget / 100_000).toInt()}L" else "₹${(summary.slotBudget / 1000).toInt()}k"
    val subtitle = "$totalSlots Slots × $slotBudgetLabel • Multi-Tier SL/TP Ladders & Breakout Triggers"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Allocation Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏹", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strategyTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = subtitle,
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AccentIndigoBg,
                    border = BorderStroke(0.8.dp, AccentIndigo.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${summary.occupiedSlotsCount}/$totalSlots SLOTS USED",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentIndigoLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )
                }
            }

            // Metric Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Deployed Capital
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = DarkCard,
                    border = BorderStroke(0.6.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("DEPLOYED", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(summary.formattedTotalDeployed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                // Available Cash
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = DarkCard,
                    border = BorderStroke(0.6.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("AVAILABLE CASH", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(summary.formattedAvailableCash, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                }
                // Strategy P&L
                val pnlColor = if (summary.totalPnl >= 0.0) ProfitGreen else LossRed
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = if (summary.totalPnl >= 0.0) ProfitGreenBg else LossRedBg,
                    border = BorderStroke(0.6.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("STRATEGY P&L", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = pnlColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${summary.formattedTotalPnl} (${summary.formattedTotalPnlPct})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = pnlColor)
                    }
                }
            }

            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Dynamic Slot Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLOT ALLOCATION MATRIX ($totalSlots SLOTS)",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap slot to manage / configure ↗",
                    fontSize = 7.5.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            val slotsPerRow = if (totalSlots <= 8) 4 else 5
            val displaySlots = (1..totalSlots).map { i ->
                slots.firstOrNull { it.slotIndex == i } ?: ArjunStrategySlot(slotIndex = i, targetCapital = summary.slotBudget)
            }
            val chunkedRows = displaySlots.chunked(slotsPerRow)

            chunkedRows.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowSlots.forEach { slot ->
                        key(slot.slotIndex) {
                            ArjunSlotPill(
                                slotIndex = slot.slotIndex,
                                slot = slot,
                                isSelected = selectedSlotIndex == slot.slotIndex,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectSlot(slot.slotIndex) }
                            )
                        }
                    }
                    if (rowSlots.size < slotsPerRow) {
                        repeat(slotsPerRow - rowSlots.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArjunSlotPill(
    slotIndex: Int,
    slot: ArjunStrategySlot?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val status = slot?.status ?: ArjunSlotStatus.EMPTY
    val stock = slot?.stock
    val breakout = slot?.pendingBreakout

    val borderColor = when {
        isSelected -> AccentIndigo
        status == ArjunSlotStatus.OCCUPIED -> ProfitGreen.copy(alpha = 0.7f)
        status == ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccent.copy(alpha = 0.8f)
        status == ArjunSlotStatus.CLOSED -> TextMuted.copy(alpha = 0.5f)
        else -> DarkCardBorder
    }

    val bgColor = when {
        isSelected -> DarkCard.copy(alpha = 0.95f)
        status == ArjunSlotStatus.OCCUPIED -> Color(0xFF132A1C)
        status == ArjunSlotStatus.PENDING_BREAKOUT -> Color(0xFF2D240E)
        status == ArjunSlotStatus.CLOSED -> DarkSurface
        else -> DarkCard
    }

    val budgetLabel = when {
        (slot?.targetCapital ?: 0.0) >= 100_000.0 -> "₹${((slot?.targetCapital ?: 100_000.0) / 100_000).toInt()}L"
        (slot?.targetCapital ?: 0.0) > 0.0 -> "₹${((slot?.targetCapital ?: 50_000.0) / 1000).toInt()}k"
        else -> "READY"
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "#$slotIndex",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> AccentIndigoLight
                    status == ArjunSlotStatus.OCCUPIED -> ProfitGreen
                    status == ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccent
                    else -> TextMuted
                }
            )
            Spacer(modifier = Modifier.height(1.dp))
            when (status) {
                ArjunSlotStatus.OCCUPIED -> {
                    if (stock != null) {
                        Text(
                            text = stock.symbol,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stock.formattedCurrentPrice,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC9D1D9),
                            maxLines = 1
                        )
                        Text(
                            text = stock.formattedPnlPct,
                            fontSize = 6.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stock.isProfit) ProfitGreen else LossRed,
                            maxLines = 1
                        )
                    }
                }
                ArjunSlotStatus.PENDING_BREAKOUT -> {
                    Text(
                        text = breakout?.symbol ?: "TRIG",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "₹${breakout?.triggerPrice?.let { String.format(Locale.US, "%.1f", it) } ?: ""}",
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent,
                        maxLines = 1
                    )
                    Text(
                        text = "BREAKOUT",
                        fontSize = 5.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
                ArjunSlotStatus.CLOSED -> {
                    Text(
                        text = stock?.symbol ?: "CLOSED",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        maxLines = 1
                    )
                    Text(
                        text = "EXITED",
                        fontSize = 6.5.sp,
                        color = TextMuted
                    )
                }
                ArjunSlotStatus.EMPTY -> {
                    Text(
                        text = "READY",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    Text(
                        text = budgetLabel,
                        fontSize = 7.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ArjunSlotConfigurationActionCard(
    slotIndex: Int,
    slot: ArjunStrategySlot,
    onCloseSlotCard: () -> Unit,
    onSetBreakoutOrder: (ArjunBreakoutOrder) -> Unit,
    onCancelBreakoutOrder: () -> Unit,
    onPartialBuy: (id: String, quantity: Int, price: Double) -> Unit,
    onPartialSell: (id: String, quantity: Int, price: Double) -> Unit,
    onFullSell: (stock: ArjunStockItem) -> Unit,
    onUpdateLadders: (id: String, targetLadder: List<ArjunLadderLevel>, stopLossLadder: List<ArjunLadderLevel>) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = slot.status

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(
            1.5.dp,
            when (status) {
                ArjunSlotStatus.EMPTY -> GoldAccent.copy(alpha = 0.6f)
                ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccent
                ArjunSlotStatus.OCCUPIED -> ProfitGreen.copy(alpha = 0.8f)
                ArjunSlotStatus.CLOSED -> TextMuted
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Slot # and Status Badge and Close (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SLOT #$slotIndex",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (status) {
                            ArjunSlotStatus.EMPTY -> DarkCard
                            ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccentBg
                            ArjunSlotStatus.OCCUPIED -> ProfitGreenBg
                            ArjunSlotStatus.CLOSED -> DarkCard
                        },
                        border = BorderStroke(
                            0.8.dp,
                            when (status) {
                                ArjunSlotStatus.EMPTY -> TextMuted
                                ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccent
                                ArjunSlotStatus.OCCUPIED -> ProfitGreen
                                ArjunSlotStatus.CLOSED -> TextMuted
                            }
                        )
                    ) {
                        Text(
                            text = slot.statusLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                ArjunSlotStatus.EMPTY -> TextSecondary
                                ArjunSlotStatus.PENDING_BREAKOUT -> GoldAccent
                                ArjunSlotStatus.OCCUPIED -> ProfitGreen
                                ArjunSlotStatus.CLOSED -> TextMuted
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCloseSlotCard,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Slot Card",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            when (status) {
                ArjunSlotStatus.EMPTY -> {
                    ArjunEmptySlotConfigSection(
                        slotIndex = slotIndex,
                        onSetBreakoutOrder = onSetBreakoutOrder
                    )
                }
                ArjunSlotStatus.PENDING_BREAKOUT -> {
                    slot.pendingBreakout?.let { breakout ->
                        ArjunPendingBreakoutSection(
                            slotIndex = slotIndex,
                            breakout = breakout,
                            onCancelOrder = onCancelBreakoutOrder
                        )
                    }
                }
                ArjunSlotStatus.OCCUPIED -> {
                    slot.stock?.let { stock ->
                        ArjunOccupiedSlotSection(
                            slotIndex = slotIndex,
                            stock = stock,
                            onPartialBuy = onPartialBuy,
                            onPartialSell = onPartialSell,
                            onFullSell = { onFullSell(stock) },
                            onUpdateLadders = onUpdateLadders
                        )
                    }
                }
                ArjunSlotStatus.CLOSED -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Position closed for ${slot.stock?.symbol ?: "this slot"}.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Realized P&L: ${slot.stock?.formattedRealizedPnl ?: "₹0.00"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if ((slot.stock?.realizedPnl ?: 0.0) >= 0.0) ProfitGreen else LossRed
                        )
                        Button(
                            onClick = onCloseSlotCard,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("Dismiss", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArjunEmptySlotConfigSection(
    slotIndex: Int,
    onSetBreakoutOrder: (ArjunBreakoutOrder) -> Unit
) {
    val context = LocalContext.current
    var symbol by remember { mutableStateOf("") }
    var triggerPrice by remember { mutableStateOf("") }
    var allocationAmount by remember { mutableStateOf("100000") }
    var shareCount by remember { mutableStateOf("") }
    var tp1Price by remember { mutableStateOf("") }
    var tp1Pct by remember { mutableStateOf("50") }
    var tp2Price by remember { mutableStateOf("") }
    var tp2Pct by remember { mutableStateOf("50") }
    var slPrice by remember { mutableStateOf("") }
    var slPct by remember { mutableStateOf("100") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Configure Breakout Trigger Buy Order for Slot #$slotIndex",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentIndigoLight
        )

        // Symbol & Trigger Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase(Locale.US) },
                label = { Text("Ticker Symbol", fontSize = 11.sp) },
                placeholder = { Text("e.g. TATAPOWER", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = triggerPrice,
                onValueChange = { triggerPrice = it },
                label = { Text("Trigger Price (₹)", fontSize = 11.sp) },
                placeholder = { Text("e.g. 420.50", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Allocation Amount (₹) & Share Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = allocationAmount,
                onValueChange = {
                    allocationAmount = it
                    val amt = it.toDoubleOrNull() ?: 0.0
                    val trig = triggerPrice.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && trig > 0.0) {
                        shareCount = (amt / trig).toInt().toString()
                    }
                },
                label = { Text("Alloc ₹ (Max ₹1L)", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = shareCount,
                onValueChange = {
                    shareCount = it
                    val cnt = it.toIntOrNull() ?: 0
                    val trig = triggerPrice.toDoubleOrNull() ?: 0.0
                    if (cnt > 0 && trig > 0.0) {
                        allocationAmount = (cnt * trig).toInt().toString()
                    }
                },
                label = { Text("Est. Shares", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Multi-tier Target & Stop Loss Ladders
        Text(
            text = "Multi-Tier Target & Stop Loss Ladder (% Allocation)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = tp1Price,
                onValueChange = { tp1Price = it },
                label = { Text("TP1 Price (₹)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfitGreen,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = tp1Pct,
                onValueChange = { tp1Pct = it },
                label = { Text("TP1 %", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.7f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfitGreen,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = tp2Price,
                onValueChange = { tp2Price = it },
                label = { Text("TP2 Price (₹)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfitGreen,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = tp2Pct,
                onValueChange = { tp2Pct = it },
                label = { Text("TP2 %", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.7f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProfitGreen,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = slPrice,
                onValueChange = { slPrice = it },
                label = { Text("Stop Loss Price (₹)", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LossRed,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = slPct,
                onValueChange = { slPct = it },
                label = { Text("SL %", fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.7f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LossRed,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        Button(
            onClick = {
                val cleanSymbol = symbol.trim().uppercase(Locale.US)
                val trig = triggerPrice.toDoubleOrNull() ?: 0.0
                val alloc = (allocationAmount.toDoubleOrNull() ?: 100_000.0).coerceIn(100.0, 100_000.0)
                if (cleanSymbol.isBlank() || trig <= 0.0) {
                    Toast.makeText(context, "Enter a valid ticker and trigger price", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val calcQty = shareCount.toIntOrNull() ?: if (trig > 0.0) (alloc / trig).toInt() else 1
                val targetLevels = mutableListOf<ArjunLadderLevel>()
                val t1 = tp1Price.toDoubleOrNull()
                val t1p = tp1Pct.toDoubleOrNull() ?: 50.0
                if (t1 != null && t1 > 0.0) {
                    targetLevels.add(ArjunLadderLevel(levelIndex = 1, triggerPrice = t1, allocationPct = t1p))
                }
                val t2 = tp2Price.toDoubleOrNull()
                val t2p = tp2Pct.toDoubleOrNull() ?: 50.0
                if (t2 != null && t2 > 0.0) {
                    targetLevels.add(ArjunLadderLevel(levelIndex = 2, triggerPrice = t2, allocationPct = t2p))
                }
                val slLevels = mutableListOf<ArjunLadderLevel>()
                val s1 = slPrice.toDoubleOrNull()
                val s1p = slPct.toDoubleOrNull() ?: 100.0
                if (s1 != null && s1 > 0.0) {
                    slLevels.add(ArjunLadderLevel(levelIndex = 1, triggerPrice = s1, allocationPct = s1p))
                }

                val order = ArjunBreakoutOrder(
                    symbol = cleanSymbol,
                    triggerPrice = trig,
                    slotIndex = slotIndex,
                    allocationAmount = alloc,
                    quantity = calcQty.coerceAtLeast(1),
                    targetLadder = targetLevels,
                    stopLossLadder = slLevels
                )
                onSetBreakoutOrder(order)
                Toast.makeText(context, "Breakout order set for $cleanSymbol on Slot #$slotIndex", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
        ) {
            Text("Set Breakout Trigger Order", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
private fun ArjunPendingBreakoutSection(
    slotIndex: Int,
    breakout: ArjunBreakoutOrder,
    onCancelOrder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Symbol:", fontSize = 11.5.sp, color = TextMuted)
                    Text(breakout.symbol, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Breakout Trigger Price:", fontSize = 11.5.sp, color = TextMuted)
                    Text("₹${String.format(Locale.US, "%.2f", breakout.triggerPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Allocation:", fontSize = 11.5.sp, color = TextMuted)
                    Text("₹${String.format(Locale.US, "%,.2f", breakout.allocationAmount)} (~${breakout.quantity} shares)", fontSize = 11.5.sp, color = TextPrimary)
                }
                if (breakout.targetLadder.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TP Ladder:", fontSize = 11.sp, color = ProfitGreen)
                        Text(
                            breakout.targetLadder.joinToString(", ") { "TP${it.levelIndex}: ₹${it.triggerPrice} (${it.allocationPct.toInt()}%)" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ProfitGreen
                        )
                    }
                }
                if (breakout.stopLossLadder.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SL Ladder:", fontSize = 11.sp, color = LossRed)
                        Text(
                            breakout.stopLossLadder.joinToString(", ") { "SL${it.levelIndex}: ₹${it.triggerPrice} (${it.allocationPct.toInt()}%)" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LossRed
                        )
                    }
                }
            }
        }

        Text(
            text = "⏳ Awaiting Live Market Trigger: System will automatically execute buy and occupy Slot #$slotIndex when live price reaches or exceeds ₹${breakout.triggerPrice}.",
            fontSize = 10.sp,
            color = GoldAccent
        )

        Button(
            onClick = onCancelOrder,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = LossRed)
        ) {
            Text("Cancel Breakout Order (Free Slot)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArjunOccupiedSlotSection(
    slotIndex: Int,
    stock: ArjunStockItem,
    onPartialBuy: (id: String, quantity: Int, price: Double) -> Unit,
    onPartialSell: (id: String, quantity: Int, price: Double) -> Unit,
    onFullSell: () -> Unit,
    onUpdateLadders: (id: String, targetLadder: List<ArjunLadderLevel>, stopLossLadder: List<ArjunLadderLevel>) -> Unit
) {
    val context = LocalContext.current
    var buyQtyText by remember(stock.id) { mutableStateOf("5") }
    var sellQtyText by remember(stock.id, stock.quantity) { mutableStateOf(kotlin.math.max(1, stock.quantity / 2).toString()) }

    var tp1PriceText by remember(stock.id) {
        val tp1 = stock.targetLadder.firstOrNull { it.levelIndex == 1 }?.triggerPrice ?: stock.target1
        mutableStateOf(if (tp1 > 0.0) tp1.toString() else "")
    }
    var tp1PctText by remember(stock.id) {
        val pct = stock.targetLadder.firstOrNull { it.levelIndex == 1 }?.allocationPct ?: 50.0
        mutableStateOf(pct.toInt().toString())
    }
    var tp2PriceText by remember(stock.id) {
        val tp2 = stock.targetLadder.firstOrNull { it.levelIndex == 2 }?.triggerPrice ?: stock.target2
        mutableStateOf(if (tp2 > 0.0) tp2.toString() else "")
    }
    var tp2PctText by remember(stock.id) {
        val pct = stock.targetLadder.firstOrNull { it.levelIndex == 2 }?.allocationPct ?: 50.0
        mutableStateOf(pct.toInt().toString())
    }
    var slPriceText by remember(stock.id) {
        val sl = stock.stopLossLadder.firstOrNull { it.levelIndex == 1 }?.triggerPrice ?: stock.stopLoss
        mutableStateOf(if (sl > 0.0) sl.toString() else "")
    }
    var slPctText by remember(stock.id) {
        val pct = stock.stopLossLadder.firstOrNull { it.levelIndex == 1 }?.allocationPct ?: 100.0
        mutableStateOf(pct.toInt().toString())
    }

    val maxBudget = 100_000.0
    val invested = stock.investedValue
    val remainingBudget = (maxBudget - invested).coerceAtLeast(0.0)
    val cmp = if (stock.currentPrice > 0.0) stock.currentPrice else stock.entryPrice
    val maxBuyQty = if (cmp > 0.0) (remainingBudget / cmp).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Position Financial Overview Card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = BorderStroke(0.8.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stock.symbol, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Qty: ${stock.quantity} shares", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigoLight)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Invested: ${stock.formattedInvestedValue}", fontSize = 11.sp, color = TextSecondary)
                    Text("CMP: ${stock.formattedCurrentPrice}", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Value: ${stock.formattedCurrentValue}", fontSize = 11.sp, color = TextSecondary)
                    val pnlCol = if (stock.isProfit) ProfitGreen else LossRed
                    Text("P&L: ${stock.formattedPnl} (${stock.formattedPnlPct})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = pnlCol)
                }

                // Slot utilization progress bar
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val progress = (invested / maxBudget).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progress >= 0.99f) GoldAccent else AccentIndigo,
                        trackColor = DarkCardBorder
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Slot Used: ₹${invested.toInt()}", fontSize = 9.sp, color = TextMuted)
                        Text("Headroom: ₹${remainingBudget.toInt()} (Max ₹1L)", fontSize = 9.sp, color = TextMuted)
                    }
                }
            }
        }

        // Action 1: Partial Buy
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = BorderStroke(0.8.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PARTIAL BUY (Add to Position)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentIndigoLight
                )
                if (maxBuyQty > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = buyQtyText,
                            onValueChange = { buyQtyText = it },
                            label = { Text("Shares (Max $maxBuyQty)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentIndigo,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Button(
                            onClick = {
                                val bQty = buyQtyText.toIntOrNull() ?: 0
                                if (bQty <= 0 || bQty > maxBuyQty) {
                                    Toast.makeText(context, "Enter shares between 1 and $maxBuyQty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onPartialBuy(stock.id, bQty, cmp)
                                Toast.makeText(context, "Bought $bQty shares of ${stock.symbol}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            Text("+ Buy", fontWeight = FontWeight.Bold)
                        }
                    }
                    // Quick add buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 5, 10, maxBuyQty).distinct().filter { it <= maxBuyQty }.forEach { q ->
                            AssistChip(
                                onClick = { buyQtyText = q.toString() },
                                label = { Text("+$q", fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = TextSecondary)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "₹1,00,000 slot budget fully reached. Cannot add more shares.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Action 2: Partial Sell & Full Sell
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = BorderStroke(0.8.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PARTIAL SELL & FULL EXIT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(25, 50, 75).forEach { pct ->
                        val calc = kotlin.math.max(1, (stock.quantity * pct / 100.0).toInt())
                        AssistChip(
                            onClick = { sellQtyText = calc.toString() },
                            label = { Text("$pct% ($calc)", fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = TextSecondary)
                        )
                    }
                    AssistChip(
                        onClick = { sellQtyText = stock.quantity.toString() },
                        label = { Text("100% (${stock.quantity})", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = LossRed)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sellQtyText,
                        onValueChange = { sellQtyText = it },
                        label = { Text("Sell Shares (1..${stock.quantity})", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Button(
                        onClick = {
                            val sQty = sellQtyText.toIntOrNull() ?: 0
                            if (sQty <= 0 || sQty > stock.quantity) {
                                Toast.makeText(context, "Enter shares between 1 and ${stock.quantity}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (sQty == stock.quantity) {
                                onFullSell()
                            } else {
                                onPartialSell(stock.id, sQty, cmp)
                            }
                            Toast.makeText(context, "Executed sell for $sQty shares", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Sell", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Button(
                    onClick = {
                        onFullSell()
                        Toast.makeText(context, "Exited position for ${stock.symbol}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Exit 100% Position (Liberate Slot)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action 3: Target & SL Ladder Edit
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            border = BorderStroke(0.8.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "MULTI-TIER TARGET & STOP LOSS LADDERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfitGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = tp1PriceText,
                        onValueChange = { tp1PriceText = it },
                        label = { Text("TP1 ₹", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = tp1PctText,
                        onValueChange = { tp1PctText = it },
                        label = { Text("TP1 %", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = tp2PriceText,
                        onValueChange = { tp2PriceText = it },
                        label = { Text("TP2 ₹", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = tp2PctText,
                        onValueChange = { tp2PctText = it },
                        label = { Text("TP2 %", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProfitGreen,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = slPriceText,
                        onValueChange = { slPriceText = it },
                        label = { Text("SL ₹", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LossRed,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = slPctText,
                        onValueChange = { slPctText = it },
                        label = { Text("SL %", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LossRed,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Button(
                    onClick = {
                        val tpLevels = mutableListOf<ArjunLadderLevel>()
                        val t1 = tp1PriceText.toDoubleOrNull()
                        val t1p = tp1PctText.toDoubleOrNull() ?: 50.0
                        if (t1 != null && t1 > 0.0) {
                            tpLevels.add(ArjunLadderLevel(levelIndex = 1, triggerPrice = t1, allocationPct = t1p))
                        }
                        val t2 = tp2PriceText.toDoubleOrNull()
                        val t2p = tp2PctText.toDoubleOrNull() ?: 50.0
                        if (t2 != null && t2 > 0.0) {
                            tpLevels.add(ArjunLadderLevel(levelIndex = 2, triggerPrice = t2, allocationPct = t2p))
                        }
                        val slLevels = mutableListOf<ArjunLadderLevel>()
                        val s1 = slPriceText.toDoubleOrNull()
                        val s1p = slPctText.toDoubleOrNull() ?: 100.0
                        if (s1 != null && s1 > 0.0) {
                            slLevels.add(ArjunLadderLevel(levelIndex = 1, triggerPrice = s1, allocationPct = s1p))
                        }
                        onUpdateLadders(stock.id, tpLevels, slLevels)
                        Toast.makeText(context, "Updated ladders for ${stock.symbol}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                ) {
                    Text("Update Ladders", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 9. Auto-Execution Alert Banner
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunAutoExecutionBanner(
    events: List<com.example.varalakshmiportfolio.model.ArjunAutoExecutionEvent>,
    onDismiss: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        events.take(3).forEach { event ->
            key(event.id) {
                val isProfit = event.isProfit
                val borderColor = if (isProfit) ProfitGreen else LossRed
                val bgColor = if (isProfit) ProfitGreenBg else LossRedBg

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isProfit) "🎯" else "🛑", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = event.headline,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) ProfitGreen else LossRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.message,
                                fontSize = 9.5.sp,
                                color = TextPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = { onDismiss(event.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
    }
}

// 10. Arjun 7L Strategy Approval Confirmation Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunApprovalDialog(
    stock: ArjunStockItem,
    availableSlots: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveEntry = if (stock.entryPrice > 0.0) stock.entryPrice else stock.currentPrice
    val canAfford = effectiveEntry <= 100_000.0 && effectiveEntry > 0.0
    val qty = if (canAfford) (100_000.0 / effectiveEntry).toInt() else 0
    val approxTotal = if (canAfford) com.example.varalakshmiportfolio.model.ArjunMoneyMath.multiplyPaise(qty, effectiveEntry) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏹", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Approve for 7L Strategy",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (canAfford) "Deploy approx ₹1,00,000 into next available slot for ${stock.symbol}."
                    else "Cannot allocate ${stock.symbol}: Single share price exceeds ₹1,00,000 slot budget.",
                    fontSize = 12.sp,
                    color = if (canAfford) TextSecondary else LossRed
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stock:", fontSize = 11.sp, color = TextMuted)
                            Text(stock.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Price:", fontSize = 11.sp, color = TextMuted)
                            Text(stock.formattedEntryPrice, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Auto-Sized Quantity:", fontSize = 11.sp, color = TextMuted)
                            Text(if (canAfford) "$qty shares" else "0 shares (exceeds budget)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (canAfford) AccentIndigoLight else LossRed)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Approx Slot Investment:", fontSize = 11.sp, color = TextMuted)
                            Text(String.format(Locale.US, "₹%,.2f", approxTotal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.6.dp, modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Profit Target 1:", fontSize = 11.sp, color = ProfitGreen)
                            Text("${stock.formattedTarget1} (+${stock.target1GainPct}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stop Loss:", fontSize = 11.sp, color = LossRed)
                            Text("${stock.formattedStopLoss} (${stock.stopLossLossPct}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentIndigoBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = AccentIndigoLight, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily monitoring active: System sells immediately when Target 1 or Stop Loss is reached, freeing up the slot.",
                            fontSize = 9.5.sp,
                            color = AccentIndigoLight
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Confirm & Buy (₹1L Slot)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

