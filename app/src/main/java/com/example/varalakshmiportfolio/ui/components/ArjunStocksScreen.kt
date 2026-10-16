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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunStocksUiState
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }
    var stockToEdit by remember { mutableStateOf<ArjunStockItem?>(null) }
    var stockToClose by remember { mutableStateOf<ArjunStockItem?>(null) }
    var stockToDelete by remember { mutableStateOf<ArjunStockItem?>(null) }
    var stockToApprove by remember { mutableStateOf<ArjunStockItem?>(null) }

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

        // 1. Arjun 5L Strategy 10-Slot Allocation Matrix Card
        ArjunStrategySlotMatrixCard(
            summary = state.strategySummary,
            slots = state.strategySlots,
            onSelectStock = { onSelectFilter("STRATEGY") }
        )

        // 2. Header & Summary KPI Card
        ArjunHeaderSummaryCard(
            summary = state.summary,
            isRefreshing = state.isRefreshingQuotes,
            onRefresh = onRefresh,
            onOpenPasteDialog = { showPasteDialog = true }
        )

        // 3. Filter Chips Row
        ArjunFilterChips(
            selectedFilter = state.selectedFilter,
            recommendations = state.recommendations,
            onSelectFilter = onSelectFilter
        )

        // 4. Filtered Recommendations List
        val filteredList = remember(state.recommendations, state.selectedFilter) {
            when (state.selectedFilter.uppercase(Locale.US)) {
                "STRATEGY", "5L STRATEGY" -> state.recommendations.filter { it.isApprovedForStrategy }
                "ACTIVE" -> state.recommendations.filter { !it.isClosed }
                "PROFITABLE", "WINS" -> state.recommendations.filter { it.isProfit }
                "TARGET HIT", "TARGETS", "TARGET" -> state.recommendations.filter {
                    it.status == ArjunTradeStatus.TARGET1_HIT || it.status == ArjunTradeStatus.TARGET2_HIT
                }
                "SL HIT" -> state.recommendations.filter { it.status == ArjunTradeStatus.STOP_LOSS_HIT }
                "CLOSED" -> state.recommendations.filter { it.isClosed }
                else -> state.recommendations
            }
        }

        if (filteredList.isEmpty()) {
            ArjunEmptyStateCard(
                filter = state.selectedFilter,
                onOpenPasteDialog = { showPasteDialog = true }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredList.forEach { stock ->
                    ArjunStockCard(
                        stock = stock,
                        onEdit = { stockToEdit = stock },
                        onClose = { stockToClose = stock },
                        onDelete = { stockToDelete = stock },
                        onApprove = { stockToApprove = stock },
                        onUnapprove = { onUnapproveStock(stock.id) },
                        onOpenChart = {
                            val cleanSym = stock.symbol.trim().uppercase(Locale.US)
                            val url = "https://www.google.com/finance/quote/$cleanSym:NSE"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal: Paste Recommendation Dialog
    if (showPasteDialog) {
        PasteRecommendationDialog(
            onDismiss = { showPasteDialog = false },
            onSave = { rawText, customQty ->
                val parsed = ArjunRecommendationParser.parse(rawText)
                if (parsed != null) {
                    val finalItem = if (customQty > 0) parsed.copy(quantity = customQty) else parsed
                    onUpdateStock(finalItem)
                    Toast.makeText(context, "Added ${finalItem.symbol}!", Toast.LENGTH_SHORT).show()
                } else {
                    onAddRecommendation(rawText)
                }
                showPasteDialog = false
            }
        )
    }

    // Modal: Edit Stock Dialog
    stockToEdit?.let { stock ->
        EditStockDialog(
            stock = stock,
            onDismiss = { stockToEdit = null },
            onSave = { updated ->
                onUpdateStock(updated)
                stockToEdit = null
                Toast.makeText(context, "Updated ${updated.symbol}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Close Position Dialog
    stockToClose?.let { stock ->
        CloseStockDialog(
            stock = stock,
            onDismiss = { stockToClose = null },
            onConfirmClose = { exitPrice ->
                onCloseStock(stock.id, exitPrice)
                stockToClose = null
                Toast.makeText(context, "Closed ${stock.symbol} position", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Delete Confirmation Dialog
    stockToDelete?.let { stock ->
        AlertDialog(
            onDismissRequest = { stockToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Delete ${stock.symbol}?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this recommendation? This cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStock(stock.id)
                        stockToDelete = null
                        Toast.makeText(context, "Deleted ${stock.symbol}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LossRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stockToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Modal: Approve Recommendation for 5L Strategy Dialog
    stockToApprove?.let { stock ->
        ArjunApprovalDialog(
            stock = stock,
            availableSlots = state.strategySummary.availableSlotsCount,
            onConfirm = {
                onApproveStock(stock.id)
                stockToApprove = null
            },
            onDismiss = { stockToApprove = null }
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
                            modifier = Modifier.rotate(angle)
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
                                text = if (stock.strategySlotIndex in 1..10) "STRATEGY SLOT #${stock.strategySlotIndex} ACTIVE (~₹50k)" else "STRATEGY POSITION",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stock.isClosed) TextSecondary else ProfitGreen
                            )
                        }
                        if (!stock.isClosed) {
                            Text(
                                text = "🤖 Daily Auto-Sell Active",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentIndigoLight
                            )
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
                                imageVector = if (isDayPos) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.TrendingDown,
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
                        text = "Approve & Buy into Strategy (₹50k Slot)",
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
// 8. Arjun 5L Strategy 10-Slot Matrix Card
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunStrategySlotMatrixCard(
    summary: com.example.varalakshmiportfolio.model.ArjunStrategySummary,
    slots: List<com.example.varalakshmiportfolio.model.ArjunStrategySlot>,
    onSelectStock: (String) -> Unit
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
                            text = "ARJUN 5L STRATEGY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "10 Slots × ₹50,000 • Daily Auto-Sell on SL & Target",
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
                        text = "${summary.occupiedSlotsCount}/10 SLOTS USED",
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

            // 10-Slot Grid Layout (2 rows of 5 slots)
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLOT ALLOCATION MATRIX (10 SLOTS)",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap slot for Google Finance ↗",
                    fontSize = 7.5.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            // Row 1: Slots 1 to 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    val slot = slots.firstOrNull { it.slotIndex == i }
                    ArjunSlotPill(
                        slotIndex = i,
                        slot = slot,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            slot?.stock?.let { s ->
                                onSelectStock(s.symbol)
                                StockLinkHelper.openGoogleFinance(context, s.symbol)
                            }
                        }
                    )
                }
            }

            // Row 2: Slots 6 to 10
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 6..10) {
                    val slot = slots.firstOrNull { it.slotIndex == i }
                    ArjunSlotPill(
                        slotIndex = i,
                        slot = slot,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            slot?.stock?.let { s ->
                                onSelectStock(s.symbol)
                                StockLinkHelper.openGoogleFinance(context, s.symbol)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArjunSlotPill(
    slotIndex: Int,
    slot: com.example.varalakshmiportfolio.model.ArjunStrategySlot?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOccupied = slot?.isOccupied == true && slot.stock != null
    val stock = slot?.stock

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = isOccupied, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (isOccupied) Color(0xFF132A1C) else DarkCard,
        border = BorderStroke(
            0.8.dp,
            if (isOccupied) ProfitGreen.copy(alpha = 0.6f) else DarkCardBorder
        )
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
                color = if (isOccupied) ProfitGreen else TextMuted
            )
            Spacer(modifier = Modifier.height(1.dp))
            if (isOccupied && stock != null) {
                Text(
                    text = stock.symbol,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stock.formattedEntryPrice,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC9D1D9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stock.formattedPnlPct,
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stock.isProfit) ProfitGreen else LossRed,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "READY",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Text(
                    text = "₹50k",
                    fontSize = 7.sp,
                    color = TextMuted
                )
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

// ─────────────────────────────────────────────────────────────
// 10. Arjun 5L Strategy Approval Confirmation Dialog
// ─────────────────────────────────────────────────────────────
@Composable
fun ArjunApprovalDialog(
    stock: ArjunStockItem,
    availableSlots: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveEntry = if (stock.entryPrice > 0.0) stock.entryPrice else stock.currentPrice
    val qty = if (effectiveEntry > 0.0) kotlin.math.max(1, (50_000.0 / effectiveEntry).toInt()) else 10
    val approxTotal = qty * effectiveEntry

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏹", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Approve for 5L Strategy",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Deploy approx ₹50,000 into next available slot for ${stock.symbol}.",
                    fontSize = 12.sp,
                    color = TextSecondary
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
                            Text("$qty shares", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentIndigoLight)
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Confirm & Buy (₹50k Slot)", fontWeight = FontWeight.Bold)
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

