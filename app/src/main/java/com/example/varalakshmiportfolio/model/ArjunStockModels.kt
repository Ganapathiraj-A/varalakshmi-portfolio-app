package com.example.varalakshmiportfolio.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID

enum class ArjunTradeStatus {
    ACTIVE,
    WAITING_TRIGGER,
    TARGET1_HIT,
    TARGET2_HIT,
    STOP_LOSS_HIT,
    CLOSED
}

data class ArjunStockItem(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val companyName: String = "",
    val brokerSource: String = "ARJUN Combo Package 🚀",
    val tradeType: String = "SWING TRADE",
    val action: String = "BUY",
    val entryPrice: Double,
    val entryPriceRange: String = "",
    val entryDate: String = "",
    val currentPrice: Double = entryPrice,
    val todayChangePct: Double = 0.0,
    val targetPrice1: Double = 0.0,
    val targetPrice2: Double = 0.0,
    val stopLoss: Double = 0.0,
    val quantity: Int = 10,
    val notes: String = "",
    val rawText: String = "",
    val isClosed: Boolean = false,
    val closedPrice: Double = 0.0,
    val closedDate: String = "",
    val isApprovedForStrategy: Boolean = false,
    val strategySlotIndex: Int = -1,
    val strategySlotCapital: Double = 50_000.0,
    val exitReason: String = "",
    val exitTriggerPrice: Double = 0.0
) {
    val isStrategySlotOccupied: Boolean
        get() = isApprovedForStrategy && !isClosed && strategySlotIndex in 1..10

    val effectivePrice: Double get() = if (isClosed) closedPrice else currentPrice

    val isProfit: Boolean get() = effectivePrice >= entryPrice

    val priceChange: Double get() = roundPaise(effectivePrice - entryPrice)

    val pnlPct: Double get() = if (entryPrice > 0.0) {
        roundPaise(((effectivePrice - entryPrice) / entryPrice) * 100.0)
    } else 0.0

    val investedValue: Double get() = roundPaise(quantity * entryPrice)

    val currentValue: Double get() = roundPaise(quantity * effectivePrice)

    val unrealizedPnl: Double get() = roundPaise(currentValue - investedValue)

    val status: ArjunTradeStatus
        get() {
            if (isClosed) return ArjunTradeStatus.CLOSED
            if (tradeType.contains("BREAKOUT", ignoreCase = true) && 
                entryPriceRange.contains("ABOVE", ignoreCase = true) && 
                currentPrice < entryPrice
            ) {
                return ArjunTradeStatus.WAITING_TRIGGER
            }
            if (stopLoss > 0.0 && currentPrice <= stopLoss) return ArjunTradeStatus.STOP_LOSS_HIT
            if (targetPrice2 > 0.0 && currentPrice >= targetPrice2) return ArjunTradeStatus.TARGET2_HIT
            if (targetPrice1 > 0.0 && currentPrice >= targetPrice1) return ArjunTradeStatus.TARGET1_HIT
            return ArjunTradeStatus.ACTIVE
        }

    val formattedEntryPrice: String get() = String.format(Locale.US, "₹%,.2f", entryPrice)
    val formattedCurrentPrice: String get() = String.format(Locale.US, "₹%,.2f", currentPrice)
    val formattedTarget1: String get() = if (targetPrice1 > 0) String.format(Locale.US, "₹%,.2f", targetPrice1) else "—"
    val formattedTarget2: String get() = if (targetPrice2 > 0) String.format(Locale.US, "₹%,.2f", targetPrice2) else "—"
    val formattedStopLoss: String get() = if (stopLoss > 0) String.format(Locale.US, "₹%,.2f", stopLoss) else "—"

    val formattedPnl: String get() {
        val sign = if (unrealizedPnl >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(unrealizedPnl))
    }

    val formattedPnlPct: String get() {
        val sign = if (pnlPct >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s%.2f%%", sign, kotlin.math.abs(pnlPct))
    }

    val target1GainPct: Double get() = if (entryPrice > 0 && targetPrice1 > 0) roundPaise(((targetPrice1 - entryPrice) / entryPrice) * 100.0) else 0.0
    val target2GainPct: Double get() = if (entryPrice > 0 && targetPrice2 > 0) roundPaise(((targetPrice2 - entryPrice) / entryPrice) * 100.0) else 0.0
    val stopLossLossPct: Double get() = if (entryPrice > 0 && stopLoss > 0) roundPaise(((stopLoss - entryPrice) / entryPrice) * 100.0) else 0.0

    /**
     * Calculates position of current price between Stop Loss and Target 1 (0.0 = SL, 1.0 = Tgt1)
     */
    val progressToTarget1: Float get() {
        if (targetPrice1 <= stopLoss || stopLoss <= 0.0) return 0.5f
        val fraction = (currentPrice - stopLoss) / (targetPrice1 - stopLoss)
        return fraction.coerceIn(0.0, 1.0).toFloat()
    }

    private fun roundPaise(v: Double): Double =
        if (v.isNaN() || v.isInfinite()) 0.0
        else BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_EVEN).toDouble()
}

data class ArjunPortfolioSummary(
    val totalInvested: Double = 0.0,
    val currentValue: Double = 0.0,
    val totalPnl: Double = 0.0,
    val totalPnlPct: Double = 0.0,
    val activeCount: Int = 0,
    val closedCount: Int = 0,
    val targetHitCount: Int = 0,
    val slHitCount: Int = 0,
    val winRatePct: Double = 0.0
) {
    val isProfit: Boolean get() = totalPnl >= 0.0
    val formattedTotalInvested: String get() = String.format(Locale.US, "₹%,.2f", totalInvested)
    val formattedCurrentValue: String get() = String.format(Locale.US, "₹%,.2f", currentValue)
    val formattedTotalPnl: String get() {
        val sign = if (totalPnl >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(totalPnl))
    }
    val formattedTotalPnlPct: String get() {
        val sign = if (totalPnlPct >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s%.2f%%", sign, kotlin.math.abs(totalPnlPct))
    }
}

data class ArjunStrategySlot(
    val slotIndex: Int, // 1 to 10
    val isOccupied: Boolean,
    val stock: ArjunStockItem? = null,
    val targetCapital: Double = 50_000.0
) {
    val statusLabel: String
        get() = if (isOccupied && stock != null) stock.symbol else "Slot $slotIndex Available"
}

data class ArjunStrategySummary(
    val totalFund: Double = 500_000.0,
    val slotBudget: Double = 50_000.0,
    val totalSlots: Int = 10,
    val occupiedSlotsCount: Int = 0,
    val availableSlotsCount: Int = 10,
    val totalDeployedCapital: Double = 0.0,
    val availableCash: Double = 500_000.0,
    val currentDeployedValue: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val totalStrategyNav: Double = 500_000.0,
    val totalStrategyReturnPct: Double = 0.0
) {
    val formattedTotalFund: String get() = String.format(Locale.US, "₹%,.2f", totalFund)
    val formattedTotalDeployed: String get() = String.format(Locale.US, "₹%,.2f", totalDeployedCapital)
    val formattedAvailableCash: String get() = String.format(Locale.US, "₹%,.2f", availableCash)
    val formattedCurrentValue: String get() = String.format(Locale.US, "₹%,.2f", currentDeployedValue)
    val formattedTotalNav: String get() = String.format(Locale.US, "₹%,.2f", totalStrategyNav)
    val totalPnl: Double get() = unrealizedPnl + realizedPnl
    val formattedTotalPnl: String get() {
        val sign = if (totalPnl >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(totalPnl))
    }
    val formattedTotalPnlPct: String get() {
        val sign = if (totalStrategyReturnPct >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s%.2f%%", sign, kotlin.math.abs(totalStrategyReturnPct))
    }
}

data class ArjunAutoExecutionEvent(
    val id: String = UUID.randomUUID().toString(),
    val stockId: String,
    val symbol: String,
    val timestamp: String,
    val eventType: String, // "TARGET_HIT" or "STOP_LOSS_HIT"
    val triggerPrice: Double,
    val entryPrice: Double,
    val quantity: Int,
    val pnlAmount: Double,
    val pnlPct: Double,
    val freedSlotIndex: Int,
    val headline: String,
    val message: String
) {
    val isProfit: Boolean get() = pnlAmount >= 0.0
    val formattedPnl: String get() {
        val sign = if (pnlAmount >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(pnlAmount))
    }
    val formattedPnlPct: String get() {
        val sign = if (pnlPct >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s%.2f%%", sign, kotlin.math.abs(pnlPct))
    }
}

data class ArjunStocksUiState(
    val recommendations: List<ArjunStockItem> = emptyList(),
    val summary: ArjunPortfolioSummary = ArjunPortfolioSummary(),
    val strategySummary: ArjunStrategySummary = ArjunStrategySummary(),
    val strategySlots: List<ArjunStrategySlot> = emptyList(),
    val autoExecutionEvents: List<ArjunAutoExecutionEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshingQuotes: Boolean = false,
    val selectedFilter: String = "ALL",
    val lastRefreshedTime: String = "",
    val errorMessage: String? = null
)

fun formatArjunStocksForClipboard(
    summary: ArjunPortfolioSummary,
    stocks: List<ArjunStockItem>
): String {
    val sb = StringBuilder()
    sb.appendLine("════════════════════════════════════════════════════════════")
    sb.appendLine("🏹 ARJUN STOCKS - BROKER RECOMMENDATIONS & LIVE P&L")
    sb.appendLine("════════════════════════════════════════════════════════════")
    sb.appendLine("• Total Invested: ${summary.formattedTotalInvested}")
    sb.appendLine("• Current Value:  ${summary.formattedCurrentValue}")
    sb.appendLine("• Overall P&L:    ${summary.formattedTotalPnl} (${summary.formattedTotalPnlPct})")
    sb.appendLine("• Active Trades:  ${summary.activeCount} | Closed: ${summary.closedCount}")
    sb.appendLine("• Target Hits:    ${summary.targetHitCount} | SL Hits: ${summary.slHitCount}")
    sb.appendLine("• Win Rate:       ${String.format(Locale.US, "%.1f%%", summary.winRatePct)}")
    sb.appendLine("────────────────────────────────────────────────────────────")
    sb.appendLine("RECOMMENDATIONS:")
    for (s in stocks) {
        val statusLabel = when (s.status) {
            ArjunTradeStatus.TARGET2_HIT -> "🎯 TARGET 2 HIT"
            ArjunTradeStatus.TARGET1_HIT -> "🎯 TARGET 1 HIT"
            ArjunTradeStatus.STOP_LOSS_HIT -> "🛑 SL HIT"
            ArjunTradeStatus.WAITING_TRIGGER -> "⏳ WAITING TRIGGER"
            ArjunTradeStatus.CLOSED -> "🔒 CLOSED"
            ArjunTradeStatus.ACTIVE -> "🟢 ACTIVE"
        }
        sb.appendLine("• ${s.symbol} [${s.tradeType}] - $statusLabel")
        sb.appendLine("  Entry: ${s.formattedEntryPrice} (${s.entryPriceRange}) | Current: ${s.formattedCurrentPrice}")
        sb.appendLine("  P&L: ${s.formattedPnl} (${s.formattedPnlPct}) | Qty: ${s.quantity} | Val: ${String.format(Locale.US, "₹%,.2f", s.currentValue)}")
        sb.appendLine("  Targets: T1=${s.formattedTarget1}, T2=${s.formattedTarget2} | SL=${s.formattedStopLoss}")
        if (s.notes.isNotBlank()) {
            sb.appendLine("  Notes: ${s.notes}")
        }
        sb.appendLine()
    }
    sb.appendLine("════════════════════════════════════════════════════════════")
    return sb.toString()
}
