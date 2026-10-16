package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.ArjunStockItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ArjunRecommendationParser {

    private val SYMBOL_ALIASES = mapOf(
        "LALITHA" to "LALITHAA",
        "M&M" to "M_M",
        "MCDOWELL" to "MCDOWELL-N",
        "TATAMOTOR" to "TATAMOTORS",
        "BAJAJ-AUTO" to "BAJAJ_AUTO"
    )

    /**
     * Parses free-form text pasted from broker alerts or Telegram channels (such as ARJUN Combo Package)
     * into a structured ArjunStockItem.
     */
    fun parse(rawText: String): ArjunStockItem? {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return null

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // 1. Detect Channel / Broker Name
        var brokerSource = "ARJUN Combo Package 🚀"
        for (line in lines.take(3)) {
            if (line.contains("ARJUN", ignoreCase = true)) {
                brokerSource = line
                break
            } else if (line.contains("BROKER", ignoreCase = true) || line.contains("ADVISORY", ignoreCase = true)) {
                brokerSource = line
                break
            }
        }

        // 2. Detect Strategy / Trade Type
        var tradeType = "SWING TRADE"
        for (line in lines) {
            val upper = line.uppercase(Locale.US)
            when {
                upper.contains("BREAKOUT ALERT") || upper.contains("BREAKOUT") -> tradeType = "BREAKOUT ALERT"
                upper.contains("SWING TRADE") || upper.contains("SWING") -> tradeType = "SWING TRADE"
                upper.contains("INTRADAY") -> tradeType = "INTRADAY"
                upper.contains("SHORT TERM") -> tradeType = "SHORT TERM"
                upper.contains("LONG TERM") -> tradeType = "LONG TERM"
                upper.contains("MOMENTUM") -> tradeType = "MOMENTUM"
            }
        }

        // 3. Extract Symbol, Action, and Company Name
        // Pattern: BUY/SELL <SYMBOL> [ (COMPANY NAME) ] [AROUND/ABOVE/AT/@ ...]
        var action = "BUY"
        var symbol = ""
        var companyName = ""
        var entryPriceRange = ""
        var entryPrice = 0.0

        val buyLineRegex = Regex("""(?i)\b(BUY|SELL|ACCUMULATE)\b\s+([A-Z0-9&_\.\-]+)(?:\s*\(([^)]+)\))?(.*)""")
        for (line in lines) {
            val match = buyLineRegex.find(line)
            if (match != null) {
                action = match.groupValues[1].uppercase(Locale.US)
                val rawSym = match.groupValues[2].uppercase(Locale.US).trim()
                companyName = match.groupValues[3].trim()
                symbol = SYMBOL_ALIASES[rawSym] ?: rawSym

                val remainder = match.groupValues[4].trim()
                if (remainder.isNotBlank()) {
                    entryPriceRange = remainder
                }
                break
            }
        }

        // If no explicit BUY line, search for symbol standalone
        if (symbol.isBlank()) {
            val standaloneRegex = Regex("""\b([A-Z]{3,15})\b""")
            for (line in lines) {
                if (line.contains("ARJUN", ignoreCase = true) || 
                    line.contains("TRADE", ignoreCase = true) || 
                    line.contains("TARGET", ignoreCase = true) ||
                    line.contains("SL", ignoreCase = true)
                ) continue
                val match = standaloneRegex.find(line)
                if (match != null) {
                    val rawSym = match.groupValues[1].uppercase(Locale.US)
                    symbol = SYMBOL_ALIASES[rawSym] ?: rawSym
                    break
                }
            }
        }

        if (symbol.isBlank()) {
            // Cannot identify stock ticker
            return null
        }

        // 4. Parse Entry Price / Range
        // Check for "AROUND 1035-1045", "ABOVE 149.05", "AT 365", "CMP 1043", "CURRENTLY TRADING 1043"
        val aroundRangeRegex = Regex("""(?i)AROUND\s+([0-9]+(?:\.[0-9]+)?)\s*[-–]\s*([0-9]+(?:\.[0-9]+)?)""")
        val aboveRegex = Regex("""(?i)ABOVE\s+([0-9]+(?:\.[0-9]+)?)""")
        val cmpRegex = Regex("""(?i)(?:CURRENTLY\s+TRADING|CMP|AT|@)\s*[:\s]?\s*([0-9]+(?:\.[0-9]+)?)""")

        var cmpPrice = 0.0
        for (line in lines) {
            val aroundMatch = aroundRangeRegex.find(line)
            if (aroundMatch != null) {
                val p1 = aroundMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val p2 = aroundMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                entryPriceRange = "${aroundMatch.groupValues[1]} - ${aroundMatch.groupValues[2]}"
                entryPrice = if (p1 > 0 && p2 > 0) (p1 + p2) / 2.0 else maxOf(p1, p2)
            }

            val aboveMatch = aboveRegex.find(line)
            if (aboveMatch != null && entryPrice == 0.0) {
                val p = aboveMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                entryPrice = p
                entryPriceRange = "ABOVE ${aboveMatch.groupValues[1]}"
            }

            val cmpMatch = cmpRegex.find(line)
            if (cmpMatch != null) {
                cmpPrice = cmpMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }
        }

        if (entryPrice == 0.0 && cmpPrice > 0.0) {
            entryPrice = cmpPrice
            if (entryPriceRange.isBlank()) entryPriceRange = "CMP $cmpPrice"
        }

        // 5. Parse Targets (e.g. "TARGETS 1175-1250", "TARGET 425 -450", "TGT 190, 225")
        var target1 = 0.0
        var target2 = 0.0
        val targetRangeRegex = Regex("""(?i)(?:TARGETS?|TGT)\s*[:\s]?\s*([0-9]+(?:\.[0-9]+)?)\s*[-–,]\s*([0-9]+(?:\.[0-9]+)?)""")
        val targetSingleRegex = Regex("""(?i)(?:TARGETS?|TGT)\s*[:\s]?\s*([0-9]+(?:\.[0-9]+)?)""")

        for (line in lines) {
            val rangeMatch = targetRangeRegex.find(line)
            if (rangeMatch != null) {
                target1 = rangeMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                target2 = rangeMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                break
            } else {
                val singleMatch = targetSingleRegex.find(line)
                if (singleMatch != null && target1 == 0.0) {
                    target1 = singleMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                }
            }
        }

        // 6. Parse Stop Loss (e.g. "KEEP SL 949.95", "SL 324.95", "STOP LOSS 129.95")
        var stopLoss = 0.0
        val slRegex = Regex("""(?i)(?:KEEP\s+SL|SL|STOP\s*LOSS)\s*[:\s]?\s*([0-9]+(?:\.[0-9]+)?)""")
        for (line in lines) {
            val slMatch = slRegex.find(line)
            if (slMatch != null) {
                stopLoss = slMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                break
            }
        }

        // 7. Extract Specific Notes (e.g. "STRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)")
        var notes = ""
        for (line in lines) {
            val upper = line.uppercase(Locale.US)
            if (upper.contains("STRICTLY") || upper.contains("DON'T BUY") || upper.contains("NOTE") || upper.contains("RULES")) {
                notes = line
                break
            }
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return ArjunStockItem(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            companyName = companyName,
            brokerSource = brokerSource,
            tradeType = tradeType,
            action = action,
            entryPrice = if (entryPrice > 0) entryPrice else (if (cmpPrice > 0) cmpPrice else 100.0),
            entryPriceRange = entryPriceRange,
            entryDate = todayDate,
            currentPrice = if (cmpPrice > 0) cmpPrice else (if (entryPrice > 0) entryPrice else 100.0),
            targetPrice1 = target1,
            targetPrice2 = target2,
            stopLoss = stopLoss,
            quantity = 10,
            notes = notes,
            rawText = trimmed
        )
    }
}
