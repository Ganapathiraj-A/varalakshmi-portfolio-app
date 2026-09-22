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
