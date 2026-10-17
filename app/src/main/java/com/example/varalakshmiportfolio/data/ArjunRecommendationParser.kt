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
        "TATA MOTORS" to "TATAMOTORS",
        "TATA MOTOR" to "TATAMOTORS",
        "BAJAJ-AUTO" to "BAJAJ_AUTO",
        "BAJAJ AUTO" to "BAJAJ_AUTO",
        "HDFC BANK" to "HDFCBANK",
        "ICICI BANK" to "ICICIBANK",
        "AXIS BANK" to "AXISBANK",
        "KOTAK BANK" to "KOTAKBANK",
        "MARUTI SUZUKI" to "MARUTI",
        "ASHOK LEYLAND" to "ASHOKLEY",
        "L&T" to "LT",
        "L & T" to "LT"
    )

    private fun cleanSymbol(raw: String): String {
        var s = raw.uppercase(Locale.US).trim()
        s = s.trim('"', '\'', '[', ']', '(', ')')
        s = s.removeSuffix(".NS")
            .removeSuffix(".NSE")
            .removeSuffix(":NSE")
            .removeSuffix("-EQ")
            .removeSuffix(":EQ")
            .removeSuffix(".BO")
            .removeSuffix(":BSE")
            .trim()
        val aliased = SYMBOL_ALIASES[s] ?: s
        return if (aliased.contains(" ")) aliased.replace(" ", "") else aliased
    }

    private fun cleanPrice(str: String): Double? {
        val clean = str.replace("₹", "")
            .replace(Regex("(?i)RS\\.?"), "")
            .replace(Regex("(?i)INR"), "")
            .replace("/-", "")
            .replace(",", "")
            .trimEnd { !it.isDigit() && it != '.' }
            .trim()
        val p = clean.toDoubleOrNull() ?: return null
        if (p <= 0.0 || p > 500_000.0 || p.isNaN() || p.isInfinite()) return null
        return p
    }

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
        // Pattern: BUY/SELL/ACCUMULATE <SYMBOL> [ (COMPANY NAME) ] [AROUND/ABOVE/AT/@ ...]
        var action = "BUY"
        var symbol = ""
        var companyName = ""
        var entryPriceRange = ""
        var entryPrice = 0.0

        val buyLineRegex = Regex("""(?i)\b(BUY|SELL|ACCUMULATE|LONG)\b[\s/:\-]+["'\[\(]?([A-Z0-9&_:\.\-]+(?:\s+[A-Z0-9&_:\.\-]+)*?)["'\]\)]?(?:\s*\(([^)]+)\))?(?:\s+(?=\b(?:AROUND|RANGE|BETWEEN|ENTRY|CMP|CURRENTLY|AT|ABOVE|NEAR|TARGETS?|TGT|KEEP|STOP|SL)\b|[@₹]|\d)|$)(.*)""")
        var buyRemainder = ""
        for (line in lines) {
            val match = buyLineRegex.find(line)
            if (match != null) {
                action = match.groupValues[1].uppercase(Locale.US)
                if (action == "LONG") action = "BUY"
                val rawSym = match.groupValues[2].trim()
                companyName = match.groupValues[3].trim()
                symbol = cleanSymbol(rawSym)

                buyRemainder = match.groupValues[4].trim()
                break
            }
        }

        // If no explicit BUY line, search for symbol standalone
        if (symbol.isBlank()) {
            val standaloneRegex = Regex("""\b([A-Z0-9&_\.\-]{3,15})\b""")
            val ignoredWords = setOf(
                "ARJUN", "TRADE", "TARGET", "TARGETS", "STOP", "LOSS", "KEEP", "ALERT",
                "RECOMMENDATION", "PACKAGE", "COMBO", "SWING", "BREAKOUT", "INTRADAY",
                "SHORT", "LONG", "ABOVE", "AROUND", "CURRENTLY", "TRADING", "ENTRY", "PRICE",
                "BUY", "SELL", "ACCUMULATE"
            )
            for (line in lines) {
                val match = standaloneRegex.find(line)
                if (match != null) {
                    val rawSym = match.groupValues[1].uppercase(Locale.US)
                    if (rawSym !in ignoredWords && !rawSym.startsWith("TGT") && !rawSym.startsWith("SL")) {
                        symbol = cleanSymbol(rawSym)
                        break
                    }
                }
            }
        }

        if (symbol.isBlank()) {
            // Cannot identify stock ticker
            return null
        }

        // 4. Parse Entry Price / Range
        val aroundRangeRegex = Regex("""(?i)\b(?:AROUND|RANGE|BETWEEN|ENTRY|BUY\s+RANGE)\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(?:/-)?\s*(?:[-–/]|(?i:\s+(?:TO|AND)\s+))\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val directRangeRegex = Regex("""(?i)(?:@|AT)?\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(?:/-)?\s*(?:[-–/]|(?i:\s+(?:TO|AND)\s+))\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val aboveRegex = Regex("""(?i)\bABOVE\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val cmpRegex = Regex("""(?i)(?:\b(?:CURRENTLY\s+TRADING|CMP|AT|ENTRY\s*PRICE|ENTRY)\b|@)\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val dateLineRegex = Regex("""(?i)\b(?:DATE|TIME|POSTED|UPDATED?)\b|\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""")

        // Parse from buyRemainder first if present to avoid cross-line timestamp/date confusion
        if (buyRemainder.isNotBlank()) {
            val aroundMatch = aroundRangeRegex.find(buyRemainder)
            if (aroundMatch != null) {
                val p1 = cleanPrice(aroundMatch.groupValues[1]) ?: 0.0
                val p2 = cleanPrice(aroundMatch.groupValues[2]) ?: 0.0
                entryPriceRange = "${aroundMatch.groupValues[1]} - ${aroundMatch.groupValues[2]}"
                entryPrice = if (p1 > 0 && p2 > 0) com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise((p1 + p2) / 2.0) else maxOf(p1, p2)
            }
            if (entryPrice == 0.0) {
                val directMatch = directRangeRegex.find(buyRemainder)
                if (directMatch != null) {
                    val p1 = cleanPrice(directMatch.groupValues[1]) ?: 0.0
                    val p2 = cleanPrice(directMatch.groupValues[2]) ?: 0.0
                    if (p1 > 0 && p2 > 0) {
                        entryPriceRange = "${directMatch.groupValues[1]} - ${directMatch.groupValues[2]}"
                        entryPrice = com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise((p1 + p2) / 2.0)
                    }
                }
            }
            if (entryPrice == 0.0) {
                val aboveMatch = aboveRegex.find(buyRemainder)
                if (aboveMatch != null) {
                    val p = cleanPrice(aboveMatch.groupValues[1]) ?: 0.0
                    entryPrice = p
                    entryPriceRange = "ABOVE ${aboveMatch.groupValues[1]}"
                }
            }
            if (entryPrice == 0.0) {
                val cmpMatch = cmpRegex.find(buyRemainder)
                if (cmpMatch != null) {
                    val p = cleanPrice(cmpMatch.groupValues[1]) ?: 0.0
                    if (p > 0.0) {
                        entryPrice = p
                        val prefix = if (cmpMatch.value.contains("CMP", ignoreCase = true)) "CMP" else "AT"
                        entryPriceRange = "$prefix ${cmpMatch.groupValues[1]}"
                    }
                }
            }
        }

        var cmpPrice = 0.0
        for (line in lines) {
            val isDateLine = dateLineRegex.containsMatchIn(line) && !line.contains("CMP", ignoreCase = true)
            if (isDateLine) continue

            val aroundMatch = aroundRangeRegex.find(line)
            if (aroundMatch != null && entryPrice == 0.0) {
                val p1 = cleanPrice(aroundMatch.groupValues[1]) ?: 0.0
                val p2 = cleanPrice(aroundMatch.groupValues[2]) ?: 0.0
                entryPriceRange = "${aroundMatch.groupValues[1]} - ${aroundMatch.groupValues[2]}"
                entryPrice = if (p1 > 0 && p2 > 0) com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise((p1 + p2) / 2.0) else maxOf(p1, p2)
            }

            val aboveMatch = aboveRegex.find(line)
            if (aboveMatch != null && entryPrice == 0.0) {
                val p = cleanPrice(aboveMatch.groupValues[1]) ?: 0.0
                entryPrice = p
                entryPriceRange = "ABOVE ${aboveMatch.groupValues[1]}"
            }

            val cmpMatch = cmpRegex.find(line)
            if (cmpMatch != null && cmpPrice == 0.0) {
                cmpPrice = cleanPrice(cmpMatch.groupValues[1]) ?: 0.0
            }
        }

        if (entryPrice == 0.0 && cmpPrice > 0.0) {
            entryPrice = cmpPrice
            if (entryPriceRange.isBlank()) entryPriceRange = "CMP $cmpPrice"
        }

        // 5. Parse Targets (e.g. "TARGETS 1175-1250", "TARGET 1: 1175", "TARGET 2: 1250", "TGT 190, 225")
        var target1 = 0.0
        var target2 = 0.0
        val targetRangeRegex = Regex("""(?i)\b(?:TARGETS?|TGT)\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(?:/-)?\s*(?:[-–,/&]|(?i:\s+(?:TO|AND)\s+))\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val target1ExplicitRegex = Regex("""(?i)\b(?:TARGET\s*1|TGT\s*1|T1)\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val target2ExplicitRegex = Regex("""(?i)\b(?:TARGET\s*2|TGT\s*2|T2)\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        val targetSingleRegex = Regex("""(?i)\b(?:TARGETS?|TGT)\b\s*[:\s\-]?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")

        for (line in lines) {
            val t1Match = target1ExplicitRegex.find(line)
            if (t1Match != null && target1 == 0.0) {
                target1 = cleanPrice(t1Match.groupValues[1]) ?: 0.0
            }
            val t2Match = target2ExplicitRegex.find(line)
            if (t2Match != null && target2 == 0.0) {
                target2 = cleanPrice(t2Match.groupValues[1]) ?: 0.0
            }

            val rangeMatch = targetRangeRegex.find(line)
            if (rangeMatch != null && target1 == 0.0) {
                target1 = cleanPrice(rangeMatch.groupValues[1]) ?: 0.0
                target2 = cleanPrice(rangeMatch.groupValues[2]) ?: 0.0
            } else if (target1 == 0.0) {
                val singleMatch = targetSingleRegex.find(line)
                if (singleMatch != null) {
                    val p = cleanPrice(singleMatch.groupValues[1]) ?: 0.0
                    // Avoid picking up literal "1" from "TARGET 1" if explicit didn't catch it
                    if (p > 5.0) {
                        target1 = p
                    }
                }
            }
        }

        // 6. Parse Stop Loss (e.g. "KEEP SL 949.95", "SL: ₹324.95", "STOP LOSS: 129.95", "STRICT SL @ 949.95")
        var stopLoss = 0.0
        val slRegex = Regex("""(?i)(?:KEEP\s+SL|STRICT\s+SL|STOP\s*[-/]?\s*LOSS|\bSL\b)\s*[:\s@\-]?\s*(?:BELOW|UNDER)?\s*(?:₹|RS\.?)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)(?!\s*[:\d])""")
        for (line in lines) {
            val slMatch = slRegex.find(line)
            if (slMatch != null) {
                stopLoss = cleanPrice(slMatch.groupValues[1]) ?: 0.0
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
