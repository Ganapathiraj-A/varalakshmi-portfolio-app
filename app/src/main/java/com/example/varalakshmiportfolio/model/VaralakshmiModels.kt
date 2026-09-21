package com.example.varalakshmiportfolio.model

data class PortfolioSummary(
    val strategyId: String = "VARALAKSHMI_ALPHA_SCALE_35",
    val strategyName: String = "VaraLakshmi Alpha (80%+ Fast Rotation Compounder)",
    val formula: String = "AlphaZero_MCTS_Options_Gated_Agile_Exit(cut_loss=4%, trail_stop=8%, target=35%)",
    val status: String = "ACTIVE",
    val totalNav: Double = 109268.80,
    val allocatedCapital: Double = 100000.00,
    val deployedCapital: Double = 97205.62,
    val availableCapital: Double = 2794.38,
    val realizedPnl: Double = 0.00,
    val unrealizedPnl: Double = 9268.80,
    val totalPnl: Double = 9268.80,
    val totalPnlPct: Double = 9.2688,
    val activeSlots: Int = 3,
    val maxSlots: Int = 3,
    val lastUpdated: String = "2026-09-21 09:26:07"
)

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
    val status: String = "OPEN"
) {
    val returnMultiplier: Double
        get() = if (entryPrice > 0) currentPrice / entryPrice else 1.0

    val multiplierString: String
        get() = String.format("%.2fx", returnMultiplier)
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
