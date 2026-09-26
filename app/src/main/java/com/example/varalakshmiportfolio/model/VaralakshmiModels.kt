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
    val reservedCapital: Double = 0.00,
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

fun isPositionBoughtToday(entryDate: String): Boolean {
    if (entryDate.isBlank()) return false
    val trimmed = entryDate.trim()
    if (trimmed.equals("today", ignoreCase = true)) return true

    val todayLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    // If string has ISO timestamp indicators ('T', 'Z', '+', or an offset like '-' after date),
    // we must parse with timezone awareness FIRST so that dates from other timezones (e.g. UTC near midnight)
    // are converted to the local calendar day rather than falsely matching a raw string prefix.
    val hasTimezoneOrIso = trimmed.contains('T') || trimmed.contains('Z') || trimmed.contains('+') ||
            (trimmed.length > 10 && trimmed.substring(10).contains('-'))

    if (hasTimezoneOrIso) {
        // Normalize fractional seconds beyond milliseconds (e.g. .123456 -> .123) for SimpleDateFormat compatibility
        val normalized = trimmed.replace(Regex("""\.(\d{3})\d+"""), ".$1")

        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mmXXX",
            "yyyy-MM-dd'T'HH:mmXX",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXX",
            "yyyy-MM-dd HH:mm:ss.SSSZ",
            "yyyy-MM-dd HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ssXX",
            "yyyy-MM-dd HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy'T'HH:mm:ssXXX",
            "dd-MM-yyyy'T'HH:mm:ssXX",
            "dd-MM-yyyy'T'HH:mm:ssZ",
            "dd-MM-yyyy'T'HH:mm:ss'Z'",
            "dd-MM-yyyy'T'HH:mm:ss",
            "dd-MM-yyyy HH:mm:ssXXX",
            "dd-MM-yyyy HH:mm:ssXX",
            "dd-MM-yyyy HH:mm:ssZ",
            "dd-MM-yyyy HH:mm:ss'Z'",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm:ssXXX",
            "dd/MM/yyyy HH:mm:ssXX",
            "dd/MM/yyyy HH:mm:ssZ",
            "dd/MM/yyyy HH:mm:ss'Z'",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm"
        )
        for (pattern in patterns) {
            try {
                val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val parsedDate = parser.parse(normalized)
                if (parsedDate != null) {
                    val localFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val dateStr = localFormat.format(parsedDate)
                    return dateStr == todayLocal
                }
            } catch (_: Exception) {
                // Continue trying remaining formats
            }
        }
        // Timezone/ISO format was indicated but could not be parsed; do NOT fall back to naive prefix match
        return false
    }

    // Direct prefix match against local ISO date yyyy-MM-dd
    if (trimmed.startsWith(todayLocal)) return true

    // Support Indian / European date conventions (dd-MM-yyyy, dd/MM/yyyy, yyyy/MM/dd)
    val altPatterns = arrayOf("dd-MM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd")
    for (altPattern in altPatterns) {
        val altToday = java.text.SimpleDateFormat(altPattern, java.util.Locale.US).format(java.util.Date())
        if (trimmed.startsWith(altToday)) return true
    }

    return false
}

fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double {
    val validEntry = if (entryPrice > 0.0 && !entryPrice.isNaN() && !entryPrice.isInfinite()) entryPrice else 0.0
    val validPrevClose = if (previousClose > 0.0 && !previousClose.isNaN() && !previousClose.isInfinite()) previousClose else 0.0

    return if (isPositionBoughtToday(entryDate)) {
        if (validEntry > 0.0) validEntry else validPrevClose
    } else {
        if (validPrevClose > 0.0) validPrevClose else validEntry
    }
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
    val referencePrice: Double = calculateReferencePrice(entryDate, entryPrice, previousClose),
    val isBoughtToday: Boolean = isPositionBoughtToday(entryDate),
    val todayPriceChange: Double = run {
        if (currentPrice.isNaN() || currentPrice.isInfinite()) 0.0
        else {
            if (referencePrice <= 0.0) 0.0
            else java.math.BigDecimal.valueOf(currentPrice - referencePrice).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
        }
    },
    val todayPriceChangePct: Double = run {
        if (currentPrice.isNaN() || currentPrice.isInfinite()) 0.0
        else {
            if (referencePrice > 0.0) {
                java.math.BigDecimal.valueOf(((currentPrice - referencePrice) / referencePrice) * 100.0).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
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

    companion object {
        fun isBoughtToday(entryDate: String): Boolean = isPositionBoughtToday(entryDate)
        fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double =
            com.example.varalakshmiportfolio.model.calculateReferencePrice(entryDate, entryPrice, previousClose)
    }
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

data class HistoricalRecommendationItem(
    val id: String,
    val symbol: String,
    val sector: String = "General",
    val entryDate: String,
    val exitDate: String? = null,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnlPercent: Double,
    val holdingDays: Int,
    val status: String,
    val exitReason: String,
    val score: Double = 90.0
) {
    val isWin: Boolean get() = pnlPercent > 0.0
    val isActive: Boolean get() = status.equals("ACTIVE", ignoreCase = true) || exitDate == null

    val formattedEntryPrice: String
        get() = if (entryPrice.isNaN() || entryPrice.isInfinite()) "₹0.00" else String.format(Locale.US, "₹%,.2f", entryPrice)

    val formattedExitPrice: String
        get() = if (exitPrice.isNaN() || exitPrice.isInfinite()) "₹0.00" else String.format(Locale.US, "₹%,.2f", exitPrice)

    val formattedPnlPercent: String
        get() {
            val sign = if (pnlPercent >= 0.0) "+" else ""
            return String.format(Locale.US, "%s%.2f%%", sign, pnlPercent)
        }
}

data class RecommendationHistorySummary(
    val totalTrades: Int,
    val winRatePercent: Double,
    val avgReturnPercent: Double,
    val profitableTrades: Int,
    val lossTrades: Int,
    val activeTrades: Int = 0,
    val bestTradePercent: Double = 0.0,
    val maxLossPercent: Double = 0.0
) {
    val formattedWinRate: String
        get() = String.format(Locale.US, "%.1f%%", winRatePercent)

    val formattedAvgReturn: String
        get() {
            val sign = if (avgReturnPercent >= 0.0) "+" else ""
            return String.format(Locale.US, "%s%.2f%%", sign, avgReturnPercent)
        }
}

