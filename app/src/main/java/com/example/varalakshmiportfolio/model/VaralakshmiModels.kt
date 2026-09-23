package com.example.varalakshmiportfolio.model

import java.util.Locale

data class PortfolioSummary(
    val strategyId: String = "VARALAKSHMI_ALPHA_SCALE_35",
    val strategyName: String = "VaraLakshmi Alpha (80%+ Fast Rotation Compounder)",
    val formula: String = "AlphaZero_MCTS_Options_Gated_Agile_Exit(cut_loss=4%, trail_stop=8%, target=35%)",
    val status: String = "ACTIVE",
    val totalNav: Double = 109268.80,
    val allocatedCapital: Double = 100000.00,
    val deployedCapital: Double = 99536.77,
    val availableCapital: Double = 463.23,
    val realizedPnl: Double = 0.00,
    val unrealizedPnl: Double = 9268.80,
    val totalPnl: Double = 9268.80,
    val totalPnlPct: Double = 9.2688,
    val todayPnl: Double = 4007.77,
    val todayPnlPct: Double = 3.81,
    val activeSlots: Int = 3,
    val maxSlots: Int = 3,
    val lastUpdated: String = "2026-09-21 09:26:07"
) {
    val todayNavChange: Double get() = todayPnl
    val todayNavChangePct: Double get() = todayPnlPct
}

data class PositionItem(
    val positionId: String,
    val symbol: String,
    val quantity: Int,
    val entryPrice: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPct: Double,
    val peakPrice: Double,
    val entryDate: String,
    val status: String = "OPEN",
    val previousClose: Double = 0.0,
    val todayPriceChange: Double = run {
        if (currentPrice.isNaN() || currentPrice.isInfinite()) 0.0
        else {
            val ref = if (previousClose > 0.0 && !previousClose.isNaN() && !previousClose.isInfinite()) previousClose
            else if (entryPrice > 0.0 && !entryPrice.isNaN() && !entryPrice.isInfinite()) entryPrice
            else 0.0
            if (ref <= 0.0) 0.0
            else java.math.BigDecimal.valueOf(currentPrice - ref).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
        }
    },
    val todayPriceChangePct: Double = run {
        if (currentPrice.isNaN() || currentPrice.isInfinite()) 0.0
        else {
            val ref = if (previousClose > 0.0 && !previousClose.isNaN() && !previousClose.isInfinite()) previousClose
            else if (entryPrice > 0.0 && !entryPrice.isNaN() && !entryPrice.isInfinite()) entryPrice
            else 0.0
            if (ref > 0.0) {
                java.math.BigDecimal.valueOf(((currentPrice - ref) / ref) * 100.0).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
            } else 0.0
        }
    },
    val todayValueChange: Double = run {
        if (todayPriceChange.isNaN() || todayPriceChange.isInfinite()) 0.0
        else java.math.BigDecimal.valueOf(quantity * todayPriceChange).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
    },
    val isProfitTargetEnabled: Boolean = true
) {
    val returnMultiplier: Double
        get() = if (entryPrice > 0 && !currentPrice.isNaN() && !currentPrice.isInfinite() && !entryPrice.isNaN() && !entryPrice.isInfinite()) currentPrice / entryPrice else 1.0

    val multiplierString: String
        get() = String.format(Locale.US, "%.2fx", returnMultiplier)

    val referencePrice: Double
        get() = if (previousClose > 0.0) previousClose else if (entryPrice > 0.0) entryPrice else 0.0
}

data class TransactionItem(
    val transactionId: String,
    val timestamp: String,
    val side: String, // "BUY" or "SELL"
    val symbol: String,
    val quantity: Int,
    val fillPrice: Double,
    val grossAmount: Double,
    val fees: Double = 20.0,
    val realizedPnl: Double = 0.0,
    val pnlDifference: String = ""
)

data class MarketIndexItem(
    val symbol: String = "NIFTY 50",
    val ltp: Double = 23401.05,
    val change: Double = 72.05,
    val changePct: Double = 0.31,
    val open: Double = 23352.15,
    val high: Double = 23404.90,
    val low: Double = 23351.75,
    val previousClose: Double = 23329.00,
    val timestamp: String = "",
    val status: String = "LIVE"
) {
    val formattedLtp: String
        get() = String.format(Locale.US, "%,.2f", ltp)

    val formattedChange: String
        get() {
            val sign = if (change >= 0) "+" else ""
            val arrow = if (change >= 0) "▲ " else "▼ "
            return "$arrow$sign${String.format(Locale.US, "%.2f", change)} ($sign${String.format(Locale.US, "%.2f", changePct)}%)"
        }

    val isPositive: Boolean
        get() = change >= 0.0
}

data class HistoricalPricePoint(
    val date: String,
    val price: Double
)

data class StockRecommendationItem(
    val rank: Int,
    val symbol: String,
    val price: Double,
    val score: Double,
    val targetPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val historical2mPoints: List<HistoricalPricePoint> = emptyList()
) {
    val return2mPct: Double
        get() {
            if (historical2mPoints.size < 2) return 0.0
            val sorted = historical2mPoints.sortedBy { it.date }
            val first = sorted.first().price
            val last = sorted.last().price
            if (first <= 0.0 || first.isNaN() || first.isInfinite() || last.isNaN() || last.isInfinite()) return 0.0
            val pct = ((last - first) / first) * 100.0
            if (pct.isNaN() || pct.isInfinite()) return 0.0
            return java.math.BigDecimal.valueOf(pct)
                .setScale(2, java.math.RoundingMode.HALF_EVEN)
                .toDouble()
        }

    val high2m: Double
        get() {
            val fallback = if (price.isNaN() || price.isInfinite()) 0.0 else price
            if (historical2mPoints.isEmpty()) return fallback
            val valid = historical2mPoints.map { it.price }.filter { !it.isNaN() && !it.isInfinite() && it > 0.0 }
            return valid.maxOrNull() ?: fallback
        }

    val low2m: Double
        get() {
            val fallback = if (price.isNaN() || price.isInfinite()) 0.0 else price
            if (historical2mPoints.isEmpty()) return fallback
            val valid = historical2mPoints.map { it.price }.filter { !it.isNaN() && !it.isInfinite() && it > 0.0 }
            return valid.minOrNull() ?: fallback
        }

    val formattedPrice: String
        get() = if (price.isNaN() || price.isInfinite()) "₹0.00" else String.format(Locale.US, "₹%,.2f", price)

    val formattedScore: String
        get() = if (score.isNaN() || score.isInfinite()) "0.0" else String.format(Locale.US, "%.1f", score)

    val formattedTarget: String
        get() = if (targetPrice.isNaN() || targetPrice.isInfinite()) "₹0.00" else String.format(Locale.US, "₹%,.2f", targetPrice)

    val formattedStopLoss: String
        get() = if (stopLossPrice.isNaN() || stopLossPrice.isInfinite()) "₹0.00" else String.format(Locale.US, "₹%,.2f", stopLossPrice)
}

