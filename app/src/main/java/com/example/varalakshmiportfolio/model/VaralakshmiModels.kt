package com.example.varalakshmiportfolio.model

import java.util.Locale
import kotlin.math.pow

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
    val lastUpdated: String = "2026-09-21 09:26:07",
    val cagrPct: Double = 0.0,
    val inceptionDate: String = "2026-09-17"
) {
    val todayNavChange: Double get() = todayPnl
    val todayNavChangePct: Double get() = todayPnlPct
    val daysActive: Int get() = calculateStrategyDaysActive(inceptionDate)
    val formattedDaysActive: String get() = if (daysActive == 1) "1 Day Active" else "$daysActive Days Active"
    val effectiveCagr: Double get() = if (cagrPct != 0.0) cagrPct else calculateCurrentCagr(totalNav, allocatedCapital, inceptionDate)
}

fun calculateStrategyDaysActive(
    inceptionDateStr: String = "2026-09-17",
    asOfDateStr: String? = null
): Int {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val inception = try {
        val clean = if (inceptionDateStr.length >= 10) inceptionDateStr.substring(0, 10) else inceptionDateStr
        sdf.parse(clean)
    } catch (_: Exception) {
        null
    } ?: return 25

    val current = try {
        if (!asOfDateStr.isNullOrBlank() && asOfDateStr.length >= 10) {
            sdf.parse(asOfDateStr.substring(0, 10)) ?: java.util.Date()
        } else {
            java.util.Date()
        }
    } catch (_: Exception) {
        java.util.Date()
    }

    val diffMillis = current.time - inception.time
    return (diffMillis / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
}

fun calculateCurrentCagr(
    totalNav: Double,
    allocatedCapital: Double,
    inceptionDateStr: String = "2026-09-17",
    asOfDateStr: String? = null
): Double {
    if (allocatedCapital <= 0.0 || totalNav <= 0.0) return 0.0
    val totalReturnRatio = totalNav / allocatedCapital
    val days = calculateStrategyDaysActive(inceptionDateStr, asOfDateStr)
    return try {
        val annualized = Math.pow(totalReturnRatio, 365.0 / days) - 1.0
        val pct = annualized * 100.0
        if (pct.isFinite()) kotlin.math.round(pct * 100.0) / 100.0 else 0.0
    } catch (_: Exception) {
        0.0
    }
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

/**
 * Validates whether an IPO deadline is today or in the future.
 * Excludes past deadlines (e.g. dates prior to today's start of day).
 * Pure time strings (e.g. "15:30") without dates default to today.
 */
fun isDeadlineTodayOrFuture(deadlineStr: String, now: java.util.Date = java.util.Date()): Boolean {
    if (deadlineStr.isBlank()) return true
    var trimmed = deadlineStr.trim()

    // Strip common labels if present
    if (trimmed.startsWith("Deadline:", ignoreCase = true)) {
        trimmed = trimmed.substringAfter("Deadline:").trim()
    } else if (trimmed.startsWith("Closes:", ignoreCase = true)) {
        trimmed = trimmed.substringAfter("Closes:").trim()
    }

    if (trimmed.equals("today", ignoreCase = true)) return true
    if (trimmed.equals("tomorrow", ignoreCase = true)) return true
    if (trimmed.equals("yesterday", ignoreCase = true)) return false

    // Pure time without date: e.g. "15:30", "15:30:00", "5:00 PM"
    if (trimmed.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?(\s*[AaPp][Mm])?$"""))) {
        return true
    }

    val calNow = java.util.Calendar.getInstance().apply {
        time = now
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val todayStartMillis = calNow.timeInMillis

    // First try standard patterns (Day-Month-Year first for Indian/UK convention, then ISO Year-Month-Day)
    val patterns = arrayOf(
        "d-M-yyyy HH:mm:ss",
        "d-M-yyyy HH:mm",
        "d-M-yyyy",
        "d/M/yyyy HH:mm:ss",
        "d/M/yyyy HH:mm",
        "d/M/yyyy",
        "yyyy-M-d'T'HH:mm:ss",
        "yyyy-M-d'T'HH:mm:ss.SSS",
        "yyyy-M-d HH:mm:ss",
        "yyyy-M-d HH:mm",
        "yyyy-M-d",
        "yyyy/M/d HH:mm:ss",
        "yyyy/M/d HH:mm",
        "yyyy/M/d"
    )

    val cleanDateStr = trimmed.replace("Z", "").replace("T", " ")

    for (pattern in patterns) {
        try {
            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            sdf.isLenient = false
            val parsed = try { sdf.parse(cleanDateStr) } catch (_: Exception) { sdf.parse(trimmed) }
            if (parsed != null) {
                val calDeadline = java.util.Calendar.getInstance().apply {
                    time = parsed
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                return calDeadline.timeInMillis >= todayStartMillis
            }
        } catch (_: Exception) {}
    }

    // Regex extraction fallback for d-M-yyyy or d/M/yyyy (e.g. 23-09-2026, 1-10-2026)
    val dmyMatch = Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(trimmed)
    if (dmyMatch != null) {
        try {
            val (dayStr, monthStr, yearStr) = dmyMatch.destructured
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, yearStr.toInt())
                set(java.util.Calendar.MONTH, monthStr.toInt() - 1)
                set(java.util.Calendar.DAY_OF_MONTH, dayStr.toInt())
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis >= todayStartMillis
        } catch (_: Exception) {}
    }

    // Regex extraction fallback for yyyy-M-d or yyyy/M/d (e.g. 2026-10-06)
    val ymdMatch = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""").find(trimmed)
    if (ymdMatch != null) {
        try {
            val (yearStr, monthStr, dayStr) = ymdMatch.destructured
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, yearStr.toInt())
                set(java.util.Calendar.MONTH, monthStr.toInt() - 1)
                set(java.util.Calendar.DAY_OF_MONTH, dayStr.toInt())
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis >= todayStartMillis
        } catch (_: Exception) {}
    }

    return true
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

    val profitTargetPrice: Double
        get() = if (entryPrice > 0 && !entryPrice.isNaN() && !entryPrice.isInfinite()) {
            java.math.BigDecimal.valueOf(entryPrice * 1.35).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
        } else 0.0

    val formattedProfitTargetPrice: String
        get() = "₹" + String.format(Locale.US, "%.2f", profitTargetPrice)

    val additionalGainToTarget: Double
        get() {
            if (!isProfitTargetEnabled || profitTargetPrice <= 0.0 || currentPrice <= 0.0 || quantity <= 0) return 0.0
            val diffPerShare = (profitTargetPrice - currentPrice).coerceAtLeast(0.0)
            return java.math.BigDecimal.valueOf(quantity * diffPerShare)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toDouble()
        }

    val formattedAdditionalGainToTarget: String
        get() = "₹" + String.format(Locale.US, "%,.2f", additionalGainToTarget)

    companion object {
        fun isBoughtToday(entryDate: String): Boolean = isPositionBoughtToday(entryDate)
        fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double =
            com.example.varalakshmiportfolio.model.calculateReferencePrice(entryDate, entryPrice, previousClose)
    }
}

/**
 * Calculates exact statutory charges and taxes for Zerodha Equity Delivery (CNC):
 * - Brokerage: ₹0.00
 * - STT: 0.1% on Buy and 0.1% on Sell
 * - Exchange Txn (NSE): 0.00297%
 * - SEBI Turnover: ₹10 / crore (0.0001%)
 * - Stamp Duty: 0.015% on Buy only
 * - GST: 18% on (Exchange + SEBI)
 * - DP Charges: ₹15.93 flat on Sell only
 */
fun calculateZerodhaDeliveryFees(grossAmount: Double, side: String): Double {
    if (grossAmount <= 0.0 || grossAmount.isNaN() || grossAmount.isInfinite()) return 0.0
    val stt = java.math.BigDecimal.valueOf(0.001 * grossAmount).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    val exchange = java.math.BigDecimal.valueOf(0.0000297 * grossAmount).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    val sebi = java.math.BigDecimal.valueOf(0.000001 * grossAmount).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    val stamp = if (side.equals("BUY", ignoreCase = true)) {
        java.math.BigDecimal.valueOf(0.00015 * grossAmount).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    } else 0.0
    val gst = java.math.BigDecimal.valueOf(0.18 * (exchange + sebi)).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
    val dp = if (side.equals("SELL", ignoreCase = true)) 15.93 else 0.0
    return java.math.BigDecimal.valueOf(stt + exchange + sebi + stamp + gst + dp).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()
}

data class TransactionItem(
    val transactionId: String,
    val timestamp: String,
    val side: String, // "BUY" or "SELL"
    val symbol: String,
    val quantity: Int,
    val fillPrice: Double,
    val grossAmount: Double,
    val fees: Double = calculateZerodhaDeliveryFees(grossAmount, side),
    val realizedPnl: Double = 0.0,
    val pnlDifference: String = ""
)

/**
 * Daily Return Record for Varalakshmi Equity Portfolio.
 * Displays session-by-session performance, net P&L, percentage change, and status.
 */
data class DailyReturnItem(
    val date: String,
    val dailyPnl: Double,
    val returnPct: Double,
    val totalNav: Double,
    val tradeCount: Int = 0,
    val status: String = when {
        !dailyPnl.isNaN() && !dailyPnl.isInfinite() && dailyPnl >= 0.005 -> "WIN"
        !dailyPnl.isNaN() && !dailyPnl.isInfinite() && dailyPnl <= -0.005 -> "LOSS"
        else -> "FLAT"
    },
    val trades: List<TransactionItem> = emptyList(),
    val capitalInflow: Double = 0.0,
    val realizedPnl: Double = dailyPnl,
    val unrealizedPnlChange: Double = 0.0
) {
    val isProfit: Boolean
        get() {
            val s = status.trim().uppercase(Locale.US)
            return when (s) {
                "WIN", "PROFIT", "GAIN" -> true
                "LOSS", "DEFICIT", "FLAT", "NEUTRAL", "EVEN", "BREAKEVEN" -> false
                else -> !dailyPnl.isNaN() && !dailyPnl.isInfinite() && dailyPnl >= 0.005
            }
        }

    val formattedPnl: String
        get() = when {
            dailyPnl.isNaN() || dailyPnl.isInfinite() -> "₹0.00"
            dailyPnl >= 0.005 -> String.format(Locale.US, "+₹%,.2f", dailyPnl)
            dailyPnl <= -0.005 -> String.format(Locale.US, "-₹%,.2f", kotlin.math.abs(dailyPnl))
            else -> "₹0.00"
        }

    val formattedReturnPct: String
        get() = when {
            returnPct.isNaN() || returnPct.isInfinite() -> "0.00%"
            returnPct >= 0.005 -> String.format(Locale.US, "+%.2f%%", returnPct)
            returnPct <= -0.005 -> String.format(Locale.US, "-%.2f%%", kotlin.math.abs(returnPct))
            else -> "0.00%"
        }

    val formattedNav: String
        get() = when {
            totalNav.isNaN() || totalNav.isInfinite() -> "₹0.00"
            else -> String.format(Locale.US, "₹%,.2f", totalNav)
        }

    val formattedCapitalInflow: String
        get() = when {
            capitalInflow.isNaN() || capitalInflow.isInfinite() || kotlin.math.abs(capitalInflow) < 0.005 -> "₹0.00"
            capitalInflow >= 0.005 -> String.format(Locale.US, "+₹%,.2f", capitalInflow)
            else -> String.format(Locale.US, "-₹%,.2f", kotlin.math.abs(capitalInflow))
        }

    val hasCapitalInflow: Boolean
        get() = !capitalInflow.isNaN() && !capitalInflow.isInfinite() && kotlin.math.abs(capitalInflow) >= 0.01
}

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

    val formattedChangePct: String
        get() {
            val sign = if (changePct >= 0) "+" else ""
            val arrow = if (changePct >= 0) "▲ " else "▼ "
            return "$arrow$sign${String.format(Locale.US, "%.2f", changePct)}%"
        }

    val shortDisplayName: String
        get() = when {
            symbol.contains("MOM", ignoreCase = true) -> "500 MOM 50"
            symbol.contains("50", ignoreCase = true) && !symbol.contains("500", ignoreCase = true) && !symbol.contains("MID", ignoreCase = true) && !symbol.contains("SML", ignoreCase = true) -> "NIFTY 50"
            symbol.contains("SML", ignoreCase = true) || symbol.contains("SMALL", ignoreCase = true) -> "SMLCAP 250"
            symbol.contains("MID", ignoreCase = true) -> "MIDCAP 150"
            else -> symbol
        }

    val isPositive: Boolean
        get() = changePct >= 0.0
}

fun createDefaultMarketIndices(): List<MarketIndexItem> = listOf(
    MarketIndexItem(
        symbol = "NIFTY 50",
        ltp = 23401.05,
        change = 72.05,
        changePct = 0.31,
        open = 23352.15,
        high = 23404.90,
        low = 23351.75,
        previousClose = 23329.00,
        status = "LIVE"
    ),
    MarketIndexItem(
        symbol = "NIFTY SMLCAP 250",
        ltp = 17709.25,
        change = 148.50,
        changePct = 0.84,
        open = 17560.75,
        high = 17725.10,
        low = 17550.20,
        previousClose = 17560.75,
        status = "LIVE"
    ),
    MarketIndexItem(
        symbol = "NIFTY MIDCAP 150",
        ltp = 21636.35,
        change = 112.30,
        changePct = 0.52,
        open = 21524.05,
        high = 21660.80,
        low = 21510.40,
        previousClose = 21524.05,
        status = "LIVE"
    ),
    MarketIndexItem(
        symbol = "NIFTY 500 MOMENTUM 50",
        ltp = 53124.40,
        change = -533.00,
        changePct = -0.99,
        open = 53635.45,
        high = 53636.15,
        low = 52966.60,
        previousClose = 53657.40,
        status = "LIVE"
    )
)

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
    val historical2mPoints: List<HistoricalPricePoint> = emptyList(),
    val changePct: Double = 0.0,
    val change: Double = 0.0
) {
    val todayChangePct: Double
        get() {
            if (changePct != 0.0) return changePct
            if (historical2mPoints.size >= 2) {
                val sorted = historical2mPoints.sortedBy { it.date }
                val today = sorted.last().price
                val yesterday = sorted[sorted.size - 2].price
                if (yesterday > 0.0 && !today.isNaN() && !yesterday.isNaN()) {
                    val pct = ((today - yesterday) / yesterday) * 100.0
                    if (pct.isFinite()) {
                        return java.math.BigDecimal.valueOf(pct)
                            .setScale(2, java.math.RoundingMode.HALF_EVEN)
                            .toDouble()
                    }
                }
            }
            return 0.0
        }

    val formattedTodayChangePct: String
        get() {
            val pct = todayChangePct
            val sign = if (pct > 0) "+" else ""
            return String.format(Locale.US, "%s%.2f%%", sign, pct)
        }

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
        get() = when {
            score.isNaN() || score.isInfinite() -> "0.0"
            score > 0.0 && score < 1.0 -> String.format(Locale.US, "%.2f", score)
            else -> String.format(Locale.US, "%.1f", score)
        }

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
    val totalTrades: Int = 0,
    val winRatePercent: Double = 0.0,
    val avgReturnPercent: Double = 0.0,
    val profitableTrades: Int = 0,
    val lossTrades: Int = 0,
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

enum class IpoActionType {
    APPLY_NOW,
    MANDATE_PENDING,
    ALLOTMENT_WON,
    ALLOTMENT_MISSED,
    LISTING_EXIT
}

data class IpoActionNotification(
    val id: String,
    val symbol: String,
    val companyName: String,
    val actionType: IpoActionType = IpoActionType.APPLY_NOW,
    val headline: String,
    val message: String,
    val lotPrice: Double = 14500.0,
    val lotQuantity: Int = 1,          // Recommended Lots to apply (Strictly 1 Lot in Retail)
    val lotShares: Int = 30,           // Shares per lot (e.g. 30 shares @ ₹500 = ₹15,000)
    val cutoffPrice: Double = 500.0,   // Cutoff / Upper band bidding price (₹)
    val qibMultiple: Double = 0.0,
    val closeDeadline: String = "15:30",
    val urgency: String = "HIGH", // HIGH, MEDIUM, INFO
    val timestamp: String = "",
    val deepLinkUrl: String = "https://kite.zerodha.com/ipo",
    val isDismissed: Boolean = false
) {
    val totalApplicationAmount: Double
        get() = if (lotPrice > 0.0) lotPrice * lotQuantity else cutoffPrice * lotShares * lotQuantity

    val formattedLotPrice: String
        get() = if (lotPrice.isNaN() || lotPrice.isInfinite()) "₹14,500.00" else String.format(Locale.US, "₹%,.2f", lotPrice)

    val formattedTotalAmount: String
        get() = String.format(Locale.US, "₹%,.2f", totalApplicationAmount)

    val formattedCutoffPrice: String
        get() = if (cutoffPrice.isNaN() || cutoffPrice.isInfinite()) "Cut-off" else String.format(Locale.US, "₹%,.2f", cutoffPrice)

    val formattedQibMultiple: String
        get() = if (qibMultiple.isNaN() || qibMultiple.isInfinite()) "—" else String.format(Locale.US, "%.1fx", qibMultiple)

    val isDeadlineTodayOrFuture: Boolean
        get() = com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture(closeDeadline)
}

/**
 * VeeraLakshmi F&O Paper-Trading Instance Summary (10L Flagship).
 */
data class FnoInstanceSummary(
    val instanceId: String = "10L",
    val name: String = "VeeraLakshmi V3 Flagship (₹10L Paper Trading)",
    val allocatedCapital: Double = 1000000.0,
    val marginBlocked: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val cashBuffer: Double = 1000000.0,
    val marginUtilizationPct: Double = 0.0,
    val positionsCount: Int = 0,
    val asOfDate: String = "",
    val inceptionDate: String = "2026-10-09"
) {
    val daysActive: Int
        get() = calculateStrategyDaysActive(inceptionDate, asOfDate.takeIf { it.isNotBlank() })

    val formattedDaysActive: String
        get() = if (daysActive == 1) "1 Day Active" else "$daysActive Days Active"

    val returnOnMarginPct: Double
        get() = if (marginBlocked > 0.0) (unrealizedPnl / marginBlocked) * 100.0 else 0.0

    val returnOnCapitalPct: Double
        get() = if (allocatedCapital > 0.0) (unrealizedPnl / allocatedCapital) * 100.0 else 0.0

    val totalNav: Double
        get() = allocatedCapital + unrealizedPnl

    val totalPnlPct: Double
        get() = returnOnCapitalPct

    val formattedAllocatedCapital: String
        get() = String.format(Locale.US, "₹%,.0f", allocatedCapital)

    val formattedMarginBlocked: String
        get() = String.format(Locale.US, "₹%,.2f", marginBlocked)

    val formattedCashBuffer: String
        get() = String.format(Locale.US, "₹%,.2f", cashBuffer)

    val formattedUnrealizedPnl: String
        get() {
            val sign = if (unrealizedPnl >= 0.0) "+" else "-"
            val absPnl = kotlin.math.abs(unrealizedPnl)
            return String.format(Locale.US, "%s₹%,.2f", sign, absPnl)
        }

    val formattedTotalNav: String
        get() = String.format(Locale.US, "₹%,.2f", totalNav)

    val formattedTotalPnlPct: String
        get() = String.format(Locale.US, "(%s%.2f%%)", if (totalPnlPct >= 0.0) "+" else "", totalPnlPct)
}

/**
 * Individual F&O Position in VeeraLakshmi Paper Trading.
 */
data class FnoPositionItem(
    val positionId: String,
    val strategyEngine: String = "IndexTrendEngine",
    val symbol: String,
    val instrumentType: String = "FUT",
    val direction: String = "SHORT", // "LONG" or "SHORT"
    val quantity: Int = 1,
    val lots: Int = 1,
    val entryPrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val marginRequired: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val expiryDate: String = "",
    val strikePrice: Double = 0.0,
    val optionType: String? = null,
    val entryReason: String = "",
    val targetPrice: Double = 0.0,
    val signalTrigger: String = "",
    val riskReward: String = ""
) {
    val isProfit: Boolean get() = unrealizedPnl >= 0.0

    val pnlPct: Double
        get() = if (entryPrice > 0.0) {
            if (direction.equals("SHORT", ignoreCase = true)) {
                ((entryPrice - currentPrice) / entryPrice) * 100.0
            } else {
                ((currentPrice - entryPrice) / entryPrice) * 100.0
            }
        } else 0.0

    val formattedUnrealizedPnl: String
        get() {
            val sign = if (unrealizedPnl >= 0.0) "+" else "-"
            val absPnl = kotlin.math.abs(unrealizedPnl)
            return String.format(Locale.US, "%s₹%,.2f", sign, absPnl)
        }

    val formattedCurrentPrice: String
        get() = String.format(Locale.US, "₹%,.2f", currentPrice)

    val formattedEntryPrice: String
        get() = String.format(Locale.US, "₹%,.2f", entryPrice)

    val formattedMargin: String
        get() = String.format(Locale.US, "₹%,.0f", marginRequired)

    val detailedReason: String
        get() {
            if (entryReason.isNotBlank()) return entryReason
            return when {
                strategyEngine.contains("Theta", ignoreCase = true) || strategyEngine.contains("Straddle", ignoreCase = true) ->
                    "Intraday NIFTY ATM Straddle deployed at 09:20 IST to harvest intraday theta decay with a 25% stop loss per leg and strict 15:15 IST square-off."

                strategyEngine.contains("Decoupling", ignoreCase = true) || strategyEngine.contains("Spread", ignoreCase = true) ->
                    "Selective Bull Call Spread deployed on red-day decoupling signal targeting 2.2R upside with strictly defined risk and margin capped within RMS limits."

                instrumentType.equals("OPT", ignoreCase = true) || optionType != null -> {
                    val optKind = optionType?.uppercase(Locale.US) ?: if (symbol.contains("PE")) "PUT" else "CALL"
                    if (direction.equals("LONG", ignoreCase = true) || direction.equals("BUY", ignoreCase = true)) {
                        "Long $optKind option position entered with strictly defined risk."
                    } else {
                        "Short $optKind option credit position entered with disciplined risk controls."
                    }
                }

                direction.equals("SHORT", ignoreCase = true) ->
                    "Systematic short derivative position executed by $strategyEngine on $symbol with protective stop-loss."

                else ->
                    "Systematic long derivative position executed by $strategyEngine on $symbol with protective stop-loss."
            }
        }

    val effectiveSignalTrigger: String
        get() {
            if (signalTrigger.isNotBlank()) return signalTrigger
            return when {
                strategyEngine.contains("Theta", ignoreCase = true) || strategyEngine.contains("Straddle", ignoreCase = true) ->
                    "09:20 IST Session Open Rule (Gap < 1.0% Gate Verified)"
                strategyEngine.contains("Decoupling", ignoreCase = true) || strategyEngine.contains("Spread", ignoreCase = true) ->
                    "Red-Day Decoupling Alpha Trigger"
                else -> "Systematic Quantitative Rules Trigger"
            }
        }

    val effectiveTargetPrice: Double
        get() {
            if (targetPrice > 0.0) return targetPrice
            if (stopLossPrice > 0.0 && entryPrice > 0.0) {
                val risk = kotlin.math.abs(entryPrice - stopLossPrice)
                return if (direction.equals("SHORT", ignoreCase = true)) {
                    kotlin.math.max(0.0, entryPrice - (risk * 2.2))
                } else {
                    entryPrice + (risk * 2.2)
                }
            }
            return 0.0
        }

    val formattedTargetPrice: String
        get() = if (effectiveTargetPrice > 0.0) String.format(Locale.US, "₹%,.2f", effectiveTargetPrice) else "Dynamic ATR"

    val effectiveRiskReward: String
        get() {
            if (riskReward.isNotBlank()) return riskReward
            if (stopLossPrice > 0.0 && entryPrice > 0.0) {
                val riskPerUnit = kotlin.math.abs(entryPrice - stopLossPrice)
                val estTarget = effectiveTargetPrice
                if (riskPerUnit > 0.0 && estTarget > 0.0) {
                    val rewardPerUnit = kotlin.math.abs(estTarget - entryPrice)
                    val ratio = rewardPerUnit / riskPerUnit
                    return String.format(Locale.US, "1 : %.1f RR", ratio)
                }
            }
            return "1 : 2.2 RR"
        }

    val stopLossDistancePct: Double
        get() = if (entryPrice > 0.0 && stopLossPrice > 0.0) {
            (kotlin.math.abs(entryPrice - stopLossPrice) / entryPrice) * 100.0
        } else 0.0

    val capitalAtRisk: Double
        get() = if (entryPrice > 0.0 && stopLossPrice > 0.0) {
            kotlin.math.abs(entryPrice - stopLossPrice) * quantity
        } else 0.0

    val formattedCapitalAtRisk: String
        get() = String.format(Locale.US, "₹%,.0f", capitalAtRisk)

    val formattedStopLoss: String
        get() = if (stopLossPrice > 0.0) String.format(Locale.US, "₹%,.2f", stopLossPrice) else "—"

    val formattedExpiry: String
        get() {
            val raw = expiryDate.trim()
            if (raw.isBlank()) return "—"
            return try {
                val parts = raw.split("-")
                if (parts.size == 3) {
                    val y = parts[0]
                    val m = when (parts[1]) {
                        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
                        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
                        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
                        else -> parts[1]
                    }
                    val d = parts[2]
                    "$d $m $y"
                } else raw
            } catch (_: Exception) {
                raw
            }
        }
}

/**
 * Historical Position held on a specific day in VeeraLakshmi.
 */
data class FnoHistoricalPosition(
    val symbol: String = "",
    val direction: String = "SHORT",
    val instrumentType: String = "FUT",
    val lots: Int = 1,
    val quantity: Int = 0,
    val entryPrice: Double = 0.0,
    val closePrice: Double = 0.0,
    val dayPnl: Double = 0.0,
    val strategyEngine: String = "IndexTrendEngine"
) {
    val isProfit: Boolean get() = dayPnl >= 0.0
    val isShort: Boolean get() = direction.equals("SHORT", ignoreCase = true)
    val formattedEntryPrice: String get() = String.format(Locale.US, "₹%,.2f", entryPrice)
    val formattedClosePrice: String get() = String.format(Locale.US, "₹%,.2f", closePrice)
    val formattedDayPnl: String get() {
        val sign = if (dayPnl >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(dayPnl))
    }
}

/**
 * Historical Executed Trade on a specific day in VeeraLakshmi.
 */
data class FnoHistoricalTrade(
    val tradeId: String = "",
    val time: String = "",
    val symbol: String = "",
    val action: String = "SELL",
    val instrumentType: String = "OPT",
    val lots: Int = 1,
    val quantity: Int = 0,
    val executionPrice: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val costs: Double = 0.0,
    val executionReason: String = ""
) {
    val isProfit: Boolean get() = realizedPnl >= 0.0
    val isSell: Boolean get() = action.contains("SELL", ignoreCase = true)
    val formattedPrice: String get() = String.format(Locale.US, "₹%,.2f", executionPrice)
    val formattedRealizedPnl: String get() {
        val sign = if (realizedPnl >= 0.0) "+" else "-"
        return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(realizedPnl))
    }
}

/**
 * Daily Profit and Loss Record for VeeraLakshmi F&O Paper Trading.
 */
data class FnoDailyPnlItem(
    val date: String,
    val dailyPnl: Double,
    val dailyReturnPct: Double,
    val totalEquity: Double,
    val marginBlocked: Double = 0.0,
    val costs: Double = 0.0,
    val positionsCount: Int = 0,
    val tradeCount: Int = 0,
    val dayStatus: String = if (dailyPnl >= 0.0) "WIN" else "LOSS",
    val positions: List<FnoHistoricalPosition> = emptyList(),
    val trades: List<FnoHistoricalTrade> = emptyList()
) {
    val isProfit: Boolean get() = dailyPnl >= 0.0

    val formattedDailyPnl: String
        get() {
            val sign = if (dailyPnl >= 0.0) "+" else "-"
            return String.format(Locale.US, "%s₹%,.2f", sign, kotlin.math.abs(dailyPnl))
        }

    val formattedDailyReturn: String
        get() {
            val sign = if (dailyReturnPct >= 0.0) "+" else ""
            return String.format(Locale.US, "%s%.2f%%", sign, dailyReturnPct)
        }

    val formattedTotalEquity: String
        get() = String.format(Locale.US, "₹%,.0f", totalEquity)

    val formattedDate: String
        get() {
            return try {
                val parts = date.split("-")
                if (parts.size == 3) {
                    val m = when (parts[1]) {
                        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
                        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
                        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
                        else -> parts[1]
                    }
                    "${parts[2]} $m"
                } else date
            } catch (_: Exception) {
                date
            }
        }

    val isToday: Boolean
        get() {
            val todayStr = try {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
            } catch (_: Exception) { "" }
            return date.isNotBlank() && date == todayStr
        }
}

/**
 * Macro Risk, VIX, and Benchmark Telemetry for VeeraLakshmi.
 */
data class FnoStatusInfo(
    val subsystem: String = "fno",
    val status: String = "READY",
    val noTradeReason: String? = null,
    val vixLevel: Double = 0.0,
    val vixRegime: String = "NORMAL",
    val circuitBreakerTier: String = "Tier 0 (Normal)",
    val cagrPct: Double = 0.0,
    val ytdPct: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val sharpeRatio: Double = 0.0,
    val winRatePct: Double = 0.0,
    val lastDate: String = ""
)

/**
 * Full UI State for VeeraLakshmi F&O Paper Trading.
 */
data class VeeraLakshmiUiState(
    val selectedInstanceId: String = "10L",
    val instances: Map<String, FnoInstanceSummary> = emptyMap(),
    val activePositions: List<FnoPositionItem> = emptyList(),
    val dailyPnlHistory: List<FnoDailyPnlItem> = emptyList(),
    val todayTrades: List<FnoHistoricalTrade> = emptyList(),
    val statusInfo: FnoStatusInfo = FnoStatusInfo(),
    val isLoading: Boolean = false,
    val isLiveSync: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncTimestamp: Long = 0L
) {
    val currentInstance: FnoInstanceSummary
        get() = instances[selectedInstanceId] ?: FnoInstanceSummary()
}

/**
 * Formats full portfolio and daily trading statements into a comprehensive monospace ASCII ledger
 * suitable for clipboard copying.
 */
fun formatVaralakshmiAllStatementsForClipboard(
    summary: PortfolioSummary,
    positions: List<PositionItem>,
    dailyReturns: List<DailyReturnItem>,
    transactions: List<TransactionItem>
): String {
    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("VARALAKSHMI ALPHA COMPOUNDER - FULL PORTFOLIO & TRADING STATEMENTS")
    sb.appendLine("================================================================================")
    sb.appendLine("Strategy   : ${summary.strategyName}")
    sb.appendLine("Formula    : ${summary.formula}")
    sb.appendLine("Status     : ${summary.status} | Last Updated: ${summary.lastUpdated}")
    sb.appendLine("Total NAV  : ${String.format(Locale.US, "₹%,.2f", summary.totalNav)}")
    sb.appendLine("Capital    : Allocated: ${String.format(Locale.US, "₹%,.2f", summary.allocatedCapital)} | Deployed: ${String.format(Locale.US, "₹%,.2f", summary.deployedCapital)} | Available: ${String.format(Locale.US, "₹%,.2f", summary.availableCapital)}")
    sb.appendLine("Total P&L  : ${if (summary.totalPnl >= 0) "+" else ""}${String.format(Locale.US, "₹%,.2f", summary.totalPnl)} (${String.format(Locale.US, "%s%.2f%%", if (summary.totalPnlPct >= 0) "+" else "", summary.totalPnlPct)})")
    sb.appendLine("Today P&L  : ${if (summary.todayPnl >= 0) "+" else ""}${String.format(Locale.US, "₹%,.2f", summary.todayPnl)} (${String.format(Locale.US, "%s%.2f%%", if (summary.todayPnlPct >= 0) "+" else "", summary.todayPnlPct)})")
    sb.appendLine("Realized   : ${String.format(Locale.US, "₹%,.2f", summary.realizedPnl)} | Unrealized: ${String.format(Locale.US, "₹%,.2f", summary.unrealizedPnl)}")
    sb.appendLine("--------------------------------------------------------------------------------")

    // 1. Active Holdings
    sb.appendLine()
    sb.appendLine("================================================================================")
    sb.appendLine("1. ACTIVE PORTFOLIO HOLDINGS (${positions.size})")
    sb.appendLine("================================================================================")
    if (positions.isEmpty()) {
        sb.appendLine("No active open positions.")
    } else {
        sb.appendLine(String.format(Locale.US, "%-12s | %-6s | %-12s | %-12s | %-14s | %-14s | %-8s",
            "TICKER", "QTY", "ENTRY", "CMP", "VALUE", "UNREALIZED P&L", "CHG %"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (p in positions) {
            val pnlSign = if (p.unrealizedPnl >= 0) "+" else ""
            val pnlPctSign = if (p.unrealizedPnlPct >= 0) "+" else ""
            sb.appendLine(String.format(Locale.US, "%-12s | %-6d | %-12s | %-12s | %-14s | %-14s | %-8s",
                p.symbol,
                p.quantity,
                String.format(Locale.US, "₹%,.2f", p.entryPrice),
                String.format(Locale.US, "₹%,.2f", p.currentPrice),
                String.format(Locale.US, "₹%,.2f", p.marketValue),
                String.format(Locale.US, "%s₹%,.2f", pnlSign, p.unrealizedPnl),
                String.format(Locale.US, "%s%.2f%%", pnlPctSign, p.unrealizedPnlPct)
            ))
        }
    }

    // 2. Daily Session Returns History
    sb.appendLine()
    sb.appendLine("================================================================================")
    sb.appendLine("2. DAILY SESSION RETURNS HISTORY (${dailyReturns.size} Sessions)")
    sb.appendLine("================================================================================")
    if (dailyReturns.isEmpty()) {
        sb.appendLine("No daily return sessions recorded.")
    } else {
        sb.appendLine(String.format(Locale.US, "%-12s | %-16s | %-14s | %-8s | %-6s | %-6s",
            "DATE", "FUND BALANCE", "DAILY P&L", "RETURN %", "STATUS", "TRADES"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (d in dailyReturns) {
            sb.appendLine(String.format(Locale.US, "%-12s | %-16s | %-14s | %-8s | %-6s | %-6d",
                d.date,
                String.format(Locale.US, "₹%,.2f", d.totalNav),
                d.formattedPnl,
                d.formattedReturnPct,
                d.status,
                d.tradeCount
            ))
        }
    }

    // 3. Daily Sessions Breakdown & Trades
    sb.appendLine()
    sb.appendLine("================================================================================")
    sb.appendLine("3. DAILY SESSIONS BREAKDOWN & TRADES")
    sb.appendLine("================================================================================")
    for (d in dailyReturns) {
        val inflowInfo = if (d.hasCapitalInflow) " | Capital Inflow: ${d.formattedCapitalInflow}" else ""
        sb.appendLine("[${d.date}] Fund Balance: ${String.format(Locale.US, "₹%,.2f", d.totalNav)} | Daily P&L: ${d.formattedPnl} (${d.formattedReturnPct})$inflowInfo | Status: ${d.status}")
        if (d.trades.isNotEmpty()) {
            sb.appendLine("  • Trades Executed (${d.trades.size}):")
            for (t in d.trades) {
                val pnlStr = if (t.realizedPnl != 0.0) " | Realized P&L: ${if (t.realizedPnl >= 0) "+" else ""}${String.format(Locale.US, "₹%,.2f", t.realizedPnl)}" else ""
                sb.appendLine("    - ${t.timestamp} ${t.side} ${t.quantity} ${t.symbol} @ ${String.format(Locale.US, "₹%,.2f", t.fillPrice)} | Gross: ${String.format(Locale.US, "₹%,.2f", t.grossAmount)} | Fees: ${String.format(Locale.US, "₹%,.2f", t.fees)}$pnlStr")
            }
        } else {
            sb.appendLine("  • Trades: None (holding/market-move session)")
        }
    }

    // 4. Complete Executed Transactions / Fill Ledger
    sb.appendLine()
    sb.appendLine("================================================================================")
    sb.appendLine("4. COMPLETE EXECUTED TRANSACTIONS / FILL LEDGER (${transactions.size} Records)")
    sb.appendLine("================================================================================")
    if (transactions.isEmpty()) {
        sb.appendLine("No executed transactions.")
    } else {
        sb.appendLine(String.format(Locale.US, "%-19s | %-4s | %-12s | %-6s | %-12s | %-14s | %-8s | %-14s",
            "TIMESTAMP", "SIDE", "SYMBOL", "QTY", "PRICE", "GROSS", "FEES", "REALIZED P&L"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (t in transactions) {
            val rPnlStr = if (t.realizedPnl != 0.0) String.format(Locale.US, "%s₹%,.2f", if (t.realizedPnl >= 0) "+" else "", t.realizedPnl) else "-"
            sb.appendLine(String.format(Locale.US, "%-19s | %-4s | %-12s | %-6d | %-12s | %-14s | %-8s | %-14s",
                t.timestamp,
                t.side,
                t.symbol,
                t.quantity,
                String.format(Locale.US, "₹%,.2f", t.fillPrice),
                String.format(Locale.US, "₹%,.2f", t.grossAmount),
                String.format(Locale.US, "₹%,.2f", t.fees),
                rPnlStr
            ))
        }
    }
    sb.appendLine("================================================================================")
    return sb.toString()
}

/**
 * Formats a single day's session return, metrics, and trades breakdown for clipboard copying.
 */
fun formatDailyReturnStatementForClipboard(item: DailyReturnItem): String {
    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("VARALAKSHMI EQUITY - DAILY SESSION STATEMENT [${item.date}]")
    sb.appendLine("================================================================================")
    sb.appendLine("Session Date  : ${item.date}")
    sb.appendLine("Fund Balance  : ${String.format(Locale.US, "₹%,.2f", item.totalNav)}")
    sb.appendLine("Daily P&L     : ${item.formattedPnl} (${item.formattedReturnPct})")
    if (item.hasCapitalInflow) {
        val inflowLabel = if (item.capitalInflow >= 0) "Capital Deposit/Inflow" else "Capital Withdrawal"
        sb.appendLine("$inflowLabel: ${item.formattedCapitalInflow}")
    }
    sb.appendLine("Session Status: ${item.status}")
    sb.appendLine("Trades Count  : ${item.tradeCount}")
    val totalVolume = item.trades.sumOf { it.grossAmount }
    val totalFees = item.trades.sumOf { it.fees }
    sb.appendLine("Total Volume  : ${String.format(Locale.US, "₹%,.2f", totalVolume)}")
    sb.appendLine("Total Fees    : ${String.format(Locale.US, "₹%,.2f", totalFees)}")
    sb.appendLine("--------------------------------------------------------------------------------")
    if (item.trades.isNotEmpty()) {
        sb.appendLine("EXECUTED TRADES & ORDER FILLS (${item.trades.size}):")
        sb.appendLine(String.format(Locale.US, "%-19s | %-4s | %-12s | %-6s | %-12s | %-14s | %-8s | %-14s",
            "TIMESTAMP", "SIDE", "SYMBOL", "QTY", "PRICE", "GROSS", "FEES", "REALIZED P&L"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (t in item.trades) {
            val rPnlStr = if (t.realizedPnl != 0.0) String.format(Locale.US, "%s₹%,.2f", if (t.realizedPnl >= 0) "+" else "", t.realizedPnl) else "-"
            sb.appendLine(String.format(Locale.US, "%-19s | %-4s | %-12s | %-6d | %-12s | %-14s | %-8s | %-14s",
                t.timestamp,
                t.side,
                t.symbol,
                t.quantity,
                String.format(Locale.US, "₹%,.2f", t.fillPrice),
                String.format(Locale.US, "₹%,.2f", t.grossAmount),
                String.format(Locale.US, "₹%,.2f", t.fees),
                rPnlStr
            ))
        }
    } else {
        sb.appendLine("No trade executions recorded for this session (holding/market-move session).")
    }
    sb.appendLine("================================================================================")
    return sb.toString()
}

val SAMPLE_IPO_SYMBOLS = setOf("SWIGGY", "WAAREEENER", "KRN", "NORTHARC", "ARKADE", "TATATECH", "BAJAJHFL", "WESTERN")

/**
 * Individual IPO Trade Record in SanthanaLakshmi.
 */
data class SanthanaLakshmiTradeItem(
    val tradeId: String,
    val symbol: String,
    val companyName: String,
    val decisionDate: String,
    val listingDate: String,
    val issuePrice: Double,
    val exitPrice: Double,
    val netGainPct: Double,
    val netPnl: Double,
    val regime: String,
    val exitReason: String,
    val qibMultiple: Double,
    val appliedLots: Int = 1,
    val allottedLots: Int = 1,
    val appliedAmount: Double = 15000.0,
    val totalCost: Double = 25.50,
    val benchmarkRetPct: Double = 0.0,
    val alphaPct: Double = 0.0,
    val status: String = "CLOSED",
    val isRealTrade: Boolean = false
) {
    val isSample: Boolean get() = !isRealTrade && (symbol.uppercase(Locale.US) in SAMPLE_IPO_SYMBOLS || tradeId.startsWith("IPO-TRD-"))
    val isWin: Boolean get() = netPnl > 0.0
    val isRegimeDefense: Boolean get() = exitReason.contains("REGIME", ignoreCase = true) || exitReason.contains("DEFENSE", ignoreCase = true)
    val formattedIssuePrice: String get() = String.format(Locale.US, "₹%,.2f", issuePrice)
    val formattedExitPrice: String get() = String.format(Locale.US, "₹%,.2f", exitPrice)
    val formattedNetPnl: String get() = String.format(Locale.US, "%s₹%,.2f", if (netPnl >= 0.0) "+" else "-", kotlin.math.abs(netPnl))
    val formattedNetGainPct: String get() = String.format(Locale.US, "%s%.2f%%", if (netGainPct >= 0.0) "+" else "", netGainPct)
    val formattedAppliedAmount: String get() = String.format(Locale.US, "₹%,.2f", appliedAmount)
}

/**
 * SanthanaLakshmi Portfolio & Strategy Summary.
 */
data class SanthanaLakshmiSummary(
    val strategyName: String = "SanthanaLakshmi (Regime-Gated IPO Compounder)",
    val allocatedCapital: Double = 100000.0,
    val deployedCapital: Double = 0.0,
    val sweptCashBalance: Double = 100000.0,
    val sweepYieldPct: Double = 6.50,
    val lifetimeCagrPct: Double = 21.55,
    val winRatePct: Double = 82.14,
    val avgListingGainPct: Double = 36.18,
    val maxDrawdownPct: Double = -0.69,
    val profitFactor: Double = 11.69,
    val sharpeRatio: Double = 7.63,
    val totalOfferingsApplied: Int = 168,
    val totalOfferingsEvaluated: Int = 216,
    val currentMarketRegime: String = "BULLISH",
    val regimeDescription: String = "Nifty 50 above SMA50/SMA200, 20-day realized volatility 12.8%, market breadth positive (62% advancing).",
    val gatingRuleActive: String = "Apply on QIB >= 1.5x or Total >= 2.0x (Regime Adaptive Exit)",
    val inceptionDate: String = "2026-09-01"
) {
    val daysActive: Int
        get() = calculateStrategyDaysActive(inceptionDate)

    val formattedDaysActive: String
        get() = if (daysActive == 1) "1 Day Active" else "$daysActive Days Active"

    val formattedAllocatedCapital: String get() = String.format(Locale.US, "₹%,.2f", allocatedCapital)
    val formattedDeployedCapital: String get() = String.format(Locale.US, "₹%,.2f", deployedCapital)
    val formattedSweptCashBalance: String get() = String.format(Locale.US, "₹%,.2f", sweptCashBalance)
    val formattedLifetimeCagr: String get() = String.format(Locale.US, "+%.2f%%", lifetimeCagrPct)
    val formattedWinRate: String get() = String.format(Locale.US, "%.1f%%", winRatePct)
    val formattedAvgListingGain: String get() = String.format(Locale.US, "+%.2f%%", avgListingGainPct)
    val formattedMaxDrawdown: String get() = String.format(Locale.US, "%.2f%%", maxDrawdownPct)
    val formattedProfitFactor: String get() = String.format(Locale.US, "%.2fx", profitFactor)
}

/**
 * Active IPO Application tracked in SanthanaLakshmi.
 */
data class ActiveIpoApplication(
    val symbol: String,
    val companyName: String,
    val issueType: String = "SME", // "SME" or "MAINBOARD"
    val appliedDate: String, // e.g. "2026-10-06"
    val closingDate: String, // e.g. "2026-10-06"
    val lotSize: Int, // e.g. 600, 1600, 1000, 2000
    val cutOffPrice: Double, // e.g. 186.0
    val totalAmount: Double = lotSize * cutOffPrice, // e.g. 111600.0
    val mandateStatus: String = "PENDING_UPI", // "APPROVED", "PENDING_UPI", "EXPIRED", "REJECTED"
    val allotmentDate: String, // e.g. "2026-10-07"
    val listingDate: String, // e.g. "2026-10-09"
    val status: String = "APPLIED", // "APPLIED", "AWAITING_ALLOTMENT", "ALLOTTED", "NOT_ALLOTTED", "AUTO_SOLD"
    val autoSellEnabled: Boolean = true,
    val targetListingGainPct: Double = 35.0,
    val executionChannel: String = "ZERODHA_CNC",
    val registrarName: String = "Bigshare Services"
) {
    val formattedCutOffPrice: String get() = String.format(Locale.US, "₹%,.2f", cutOffPrice)
    val formattedTotalAmount: String get() = String.format(Locale.US, "₹%,.2f", totalAmount)
    val isMandateApproved: Boolean get() = mandateStatus.equals("APPROVED", ignoreCase = true)
    val isAllotted: Boolean get() = status.equals("ALLOTTED", ignoreCase = true)
    val isNotAllotted: Boolean get() = status.equals("NOT_ALLOTTED", ignoreCase = true) || status.equals("REJECTED", ignoreCase = true)
    val isAllotmentToday: Boolean get() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        return allotmentDate.trim() == today
    }
}

val SAMPLE_PAST_IPO_SYMBOLS = setOf("NTPCGREEN", "SWIGGY", "WAAREEENER", "BAJAJHFL", "KRNHEAT", "PREMIEREN")
val SAMPLE_PAST_IPO_IDS = setOf("PAST-IPO-001", "PAST-IPO-002", "PAST-IPO-003", "PAST-IPO-004", "PAST-IPO-005", "PAST-IPO-006")

/**
 * Past Concluded IPO Application record in SanthanaLakshmi.
 */
data class PastIpoApplication(
    val id: String,
    val symbol: String,
    val companyName: String,
    val issueType: String = "MAINBOARD", // "MAINBOARD" or "SME"
    val appliedDate: String,
    val allotmentDate: String,
    val listingDate: String,
    val lotSize: Int,
    val cutOffPrice: Double,
    val totalAmount: Double = lotSize * cutOffPrice,
    val allotmentStatus: String = "NOT_ALLOTTED", // "ALLOTTED", "NOT_ALLOTTED", "REJECTED"
    val refundStatus: String = if (allotmentStatus.equals("ALLOTTED", ignoreCase = true)) "DEBITED" else "UNBLOCKED_TO_BANK",
    val listingPrice: Double? = null,
    val listingGainPct: Double? = null,
    val realizedPnl: Double? = null,
    val outcomeReason: String = "",
    val registrarName: String = "Link Intime India",
    val subscriptionMultiple: Double? = null,
    val isRealApplication: Boolean = false
) {
    val isSample: Boolean get() = !isRealApplication && (id in SAMPLE_PAST_IPO_IDS || id.startsWith("PAST-IPO-") || symbol.uppercase(Locale.US) in SAMPLE_PAST_IPO_SYMBOLS)
    val formattedCutOffPrice: String get() = String.format(Locale.US, "₹%,.2f", cutOffPrice)
    val formattedTotalAmount: String get() = String.format(Locale.US, "₹%,.2f", totalAmount)
    val isAllotted: Boolean get() = allotmentStatus.equals("ALLOTTED", ignoreCase = true)
    val formattedListingGainPct: String get() = if (listingGainPct != null) String.format(Locale.US, "%+.2f%%", listingGainPct) else "N/A"
    val formattedRealizedPnl: String get() = when {
        realizedPnl == null || kotlin.math.abs(realizedPnl) < 0.005 -> "₹0.00"
        realizedPnl > 0.0 -> "+₹" + String.format(Locale.US, "%,.2f", realizedPnl)
        else -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(realizedPnl))
    }
}

/**
 * Full UI State for SanthanaLakshmi.
 */
data class SanthanaLakshmiUiState(
    val summary: SanthanaLakshmiSummary = SanthanaLakshmiSummary(),
    val activeAlerts: List<IpoActionNotification> = emptyList(),
    val activeApplications: List<ActiveIpoApplication> = emptyList(),
    val pastApplications: List<PastIpoApplication> = emptyList(),
    val historicalTrades: List<SanthanaLakshmiTradeItem> = emptyList(),
    val selectedFilter: String = "ALL", // "ALL", "WINS", "DEFENSE"
    val selectedTradeDetail: SanthanaLakshmiTradeItem? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val realPastApplications: List<PastIpoApplication>
        get() = pastApplications.filterNot { it.isSample }

    val pastAllottedCount: Int get() = realPastApplications.count { it.isAllotted }
    val pastNotAllottedCount: Int get() = realPastApplications.count { !it.isAllotted }
    val pastTotalUnblockedCapital: Double get() = realPastApplications.filter { !it.isAllotted }.sumOf { it.totalAmount }
    val pastTotalRealizedListingProfit: Double get() = realPastApplications.filter { it.isAllotted }.sumOf { it.realizedPnl ?: 0.0 }

    // Only real IPO trades that were bought and sold
    val realHistoricalTrades: List<SanthanaLakshmiTradeItem>
        get() = historicalTrades.filterNot { it.isSample }

    val filteredTrades: List<SanthanaLakshmiTradeItem>
        get() = when (selectedFilter) {
            "WINS" -> realHistoricalTrades.filter { it.isWin }
            "DEFENSE" -> realHistoricalTrades.filter { it.isRegimeDefense || !it.isWin }
            else -> realHistoricalTrades
        }

    val totalWins: Int get() = realHistoricalTrades.count { it.isWin }
    val totalDefense: Int get() = realHistoricalTrades.count { it.isRegimeDefense || !it.isWin }

    val activeTodayOrFutureAlerts: List<IpoActionNotification>
        get() {
            val appliedSymbols = activeApplications.map { it.symbol.uppercase().trim() }.toSet()
            return activeAlerts
                .filter { it.isDeadlineTodayOrFuture }
                .filterNot { appliedSymbols.contains(it.symbol.uppercase().trim()) }
        }
}

fun formatSanthanaLakshmiForClipboard(
    summary: SanthanaLakshmiSummary,
    trades: List<SanthanaLakshmiTradeItem>,
    activeAlerts: List<IpoActionNotification>,
    applications: List<ActiveIpoApplication> = emptyList()
): String {
    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("🌸 SANTHANALAKSHMI - REGIME-GATED IPO COMPOUNDER AUDIT STATEMENT")
    sb.appendLine("================================================================================")
    sb.appendLine("Strategy Name : ${summary.strategyName}")
    sb.appendLine("Allocated Cap : ${summary.formattedAllocatedCapital}")
    sb.appendLine("Deployed Cap  : ${summary.formattedDeployedCapital} (Zero Risk when Idle)")
    sb.appendLine("Swept Balance : ${summary.formattedSweptCashBalance} (Liquid Fund Yield: +${summary.sweepYieldPct}% p.a.)")
    sb.appendLine("Lifetime CAGR : ${summary.formattedLifetimeCagr}")
    sb.appendLine("Win Rate      : ${summary.formattedWinRate} (${summary.totalOfferingsApplied} Applied / ${summary.totalOfferingsEvaluated} Evaluated)")
    sb.appendLine("Avg Gain      : ${summary.formattedAvgListingGain}")
    sb.appendLine("Max Drawdown  : ${summary.formattedMaxDrawdown}")
    sb.appendLine("Profit Factor : ${summary.formattedProfitFactor}")
    sb.appendLine("Sharpe Ratio  : ${String.format(Locale.US, "%.2f", summary.sharpeRatio)}")
    sb.appendLine("Active Regime : ${summary.currentMarketRegime}")
    sb.appendLine("Regime Telemetry: ${summary.regimeDescription}")
    sb.appendLine("Active Gating : ${summary.gatingRuleActive}")
    sb.appendLine("--------------------------------------------------------------------------------")
    if (applications.isNotEmpty()) {
        sb.appendLine("ACTIVE IPO APPLICATIONS & ALLOTMENT PIPELINE (${applications.size}):")
        sb.appendLine(String.format(Locale.US, "%-10s | %-6s | %-12s | %-12s | %-10s | %-10s | %-14s",
            "SYMBOL", "LOT", "AMOUNT", "MANDATE", "ALLOTMENT", "LISTING", "AUTO-SELL"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (app in applications) {
            sb.appendLine(String.format(Locale.US, "%-10s | %-6d | %-12s | %-12s | %-10s | %-10s | %-14s",
                app.symbol,
                app.lotSize,
                app.formattedTotalAmount,
                app.mandateStatus,
                app.allotmentDate,
                app.listingDate,
                if (app.autoSellEnabled) "ARMED (CNC)" else "DISABLED"
            ))
        }
        sb.appendLine("--------------------------------------------------------------------------------")
    }
    val appliedSymbols = applications.map { it.symbol.uppercase().trim() }.toSet()
    val validAlerts = activeAlerts
        .filter { it.isDeadlineTodayOrFuture }
        .filterNot { appliedSymbols.contains(it.symbol.uppercase().trim()) }
    if (validAlerts.isNotEmpty()) {
        sb.appendLine("ACTIVE IPO ACTION SIGNALS (${validAlerts.size}):")
        for (a in validAlerts) {
            sb.appendLine("- [${a.actionType}] ${a.symbol} (${a.companyName}) Cut-off: ${a.formattedCutoffPrice}, QIB: ${a.formattedQibMultiple}x, Deadline: ${a.closeDeadline}")
        }
        sb.appendLine("--------------------------------------------------------------------------------")
    } else {
        sb.appendLine("ACTIVE SIGNALS: None currently active. 100% of capital is deployed in Liquid ETF yield sweep.")
        sb.appendLine("--------------------------------------------------------------------------------")
    }
    val realTrades = trades.filterNot { it.isSample }
    if (realTrades.isNotEmpty()) {
        sb.appendLine("AUDITED HISTORICAL IPO EXECUTIONS (${realTrades.size}):")
        sb.appendLine(String.format(Locale.US, "%-10s | %-10s | %-10s | %-9s | %-9s | %-9s | %-12s | %-14s",
            "SYMBOL", "DECISION", "LISTING", "ISSUE", "EXIT", "GAIN %", "NET PNL", "EXIT REASON"))
        sb.appendLine("--------------------------------------------------------------------------------")
        for (t in realTrades) {
            sb.appendLine(String.format(Locale.US, "%-10s | %-10s | %-10s | %-9s | %-9s | %-9s | %-12s | %-14s",
                t.symbol,
                t.decisionDate,
                t.listingDate,
                t.formattedIssuePrice,
                t.formattedExitPrice,
                t.formattedNetGainPct,
                t.formattedNetPnl,
                t.exitReason
            ))
        }
    } else {
        sb.appendLine("AUDITED HISTORICAL IPO EXECUTIONS: No completed real IPO trades yet.")
    }
    sb.appendLine("================================================================================")
    return sb.toString()
}

