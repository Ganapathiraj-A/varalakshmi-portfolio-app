package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArjunStockRepository {

    companion object {
        const val ARJUN_CACHE_FILE = "arjun_stocks_cache.json"

        const val TOTAL_STRATEGY_FUND = 500_000.0 // 5 Lakhs
        const val TOTAL_STRATEGY_SLOTS = 10
        const val SLOT_BUDGET = 50_000.0 // Approx 50k per slot

        @Volatile
        var enableNetworkQuotes: Boolean = true

        private val lock = Any()
        @Volatile
        private var memoryCache: MutableList<ArjunStockItem>? = null

        private val executionEventsCache = mutableListOf<ArjunAutoExecutionEvent>()

        fun resetCacheForTesting() = synchronized(lock) {
            memoryCache = null
            executionEventsCache.clear()
        }

        fun createDefaultSeeds(): List<ArjunStockItem> {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return listOf(
                ArjunStockItem(
                    id = "arjun-seed-skygold",
                    symbol = "SKYGOLD",
                    companyName = "Sky Gold Ltd",
                    brokerSource = "ARJUN Combo Package 🚀",
                    tradeType = "SWING TRADE",
                    action = "BUY",
                    entryPrice = 1058.64,
                    entryPriceRange = "1035 - 1045 (Filled: 1058.64)",
                    entryDate = todayStr,
                    currentPrice = 1059.45,
                    todayChangePct = 6.36,
                    targetPrice1 = 1175.0,
                    targetPrice2 = 1250.0,
                    stopLoss = 949.95,
                    quantity = 47, // 47 shares @ ₹1,058.64 = ₹49,756.05 for Slot 1
                    notes = "Filled live on Zerodha Kite CNC (Order #261016190744814). Executed at ₹1,058.64.",
                    rawText = "ARJUN Combo Package 🚀\nSWING TRADE\nBUY SKYGOLD AROUND 1035-1045\nTARGETS 1175-1250\nKEEP SL 949.95\nFILLED ON ZERODHA @ 1058.64",
                    isApprovedForStrategy = true,
                    strategySlotIndex = 1,
                    strategySlotCapital = SLOT_BUDGET
                ),
                ArjunStockItem(
                    id = "arjun-seed-lalitha",
                    symbol = "LALITHAA",
                    companyName = "Lalithaa Jewellery Mart Ltd",
                    brokerSource = "ARJUN Combo Package 🚀",
                    tradeType = "SWING TRADE",
                    action = "BUY",
                    entryPrice = 369.26,
                    entryPriceRange = "365 - 370 (Filled: 369.26)",
                    entryDate = todayStr,
                    currentPrice = 369.30,
                    todayChangePct = 3.33,
                    targetPrice1 = 425.0,
                    targetPrice2 = 450.0,
                    stopLoss = 324.95,
                    quantity = 136, // 136 shares @ ₹369.26 = ₹50,219.45 for Slot 2
                    notes = "Filled live on Zerodha Kite CNC (Order #261016190745570). Executed at ₹369.26.",
                    rawText = "ARJUN Combo Package 🚀\nSWING TRADE\nBUY LALITHA AROUND 365-370\nTARGETS 425 -450\nKEEP SL 324.95\nFILLED ON ZERODHA @ 369.26",
                    isApprovedForStrategy = true,
                    strategySlotIndex = 2,
                    strategySlotCapital = SLOT_BUDGET
                ),
                ArjunStockItem(
                    id = "arjun-seed-orienthot",
                    symbol = "ORIENTHOT",
                    companyName = "Orient Hotels Ltd",
                    brokerSource = "ARJUN Combo Package 🚀",
                    tradeType = "BREAKOUT ALERT",
                    action = "BUY",
                    entryPrice = 149.05,
                    entryPriceRange = "ABOVE 149.05",
                    entryDate = todayStr,
                    currentPrice = 147.20,
                    todayChangePct = 1.11,
                    targetPrice1 = 190.0,
                    targetPrice2 = 225.0,
                    stopLoss = 129.95,
                    quantity = 335, // approx ₹49,931.75 for Slot 3
                    notes = "STRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)",
                    rawText = "ARJUN Combo Package 🚀\nBREAKOUT ALERT\nBUY ORIENTHOT (ORIENT HOTELS)\nABOVE 149.05\nTARGETS 190-225\nKEEP SL 129.95\nCURRENTLY TRADING 145.50\nSTRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)",
                    isApprovedForStrategy = true,
                    strategySlotIndex = 3,
                    strategySlotCapital = SLOT_BUDGET
                )
            )
        }
    }

    fun getRecommendations(): List<ArjunStockItem> {
        synchronized(lock) {
            if (memoryCache == null) {
                memoryCache = (loadFromDisk() ?: createDefaultSeeds()).toMutableList()
            }
            return memoryCache!!.toList()
        }
    }

    fun addRecommendation(item: ArjunStockItem): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            list.add(0, item)
            memoryCache = list
            saveToDisk(list, executionEventsCache)
            return list.toList()
        }
    }

    fun updateRecommendation(item: ArjunStockItem): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            val idx = list.indexOfFirst { it.id == item.id }
            if (idx >= 0) {
                list[idx] = item
            } else {
                list.add(0, item)
            }
            memoryCache = list
            saveToDisk(list, executionEventsCache)
            return list.toList()
        }
    }

    fun deleteRecommendation(id: String): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            list.removeAll { it.id == id }
            memoryCache = list
            saveToDisk(list, executionEventsCache)
            return list.toList()
        }
    }

    fun markAsClosed(id: String, exitPrice: Double = 0.0, reason: String = "MANUAL_EXIT"): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val old = list[idx]
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val finalExit = if (exitPrice > 0) exitPrice else old.currentPrice
                list[idx] = old.copy(
                    isClosed = true,
                    closedPrice = finalExit,
                    closedDate = todayStr,
                    exitReason = reason,
                    exitTriggerPrice = old.currentPrice
                )
            }
            memoryCache = list
            saveToDisk(list, executionEventsCache)
            return list.toList()
        }
    }

    /**
     * Approves a recommendation into the Arjun 5L Strategy.
     * Finds next available slot out of 10, calculates share quantity for approx ₹50,000,
     * and activates automated SL and Target monitoring.
     */
    fun approveRecommendation(id: String, targetSlot: Int? = null): Pair<Boolean, String> = synchronized(lock) {
        val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return Pair(false, "Stock recommendation not found.")

        val item = list[idx]
        if (item.isStrategySlotOccupied) {
            return Pair(true, "${item.symbol} is already active in Strategy Slot #${item.strategySlotIndex}.")
        }

        // Find occupied slot indices among currently active strategy items
        val occupiedSlots = list.filter { it.isStrategySlotOccupied }.map { it.strategySlotIndex }.toSet()
        val assignedSlot = targetSlot?.takeIf { it in 1..TOTAL_STRATEGY_SLOTS && !occupiedSlots.contains(it) }
            ?: (1..TOTAL_STRATEGY_SLOTS).firstOrNull { !occupiedSlots.contains(it) }

        if (assignedSlot == null) {
            return Pair(false, "All $TOTAL_STRATEGY_SLOTS strategy slots (₹5,00,000) are occupied! Wait for an active position to hit Target or Stop Loss to free up a slot.")
        }

        // Auto-calculate quantity for approx ₹50,000
        val effectiveEntry = if (item.entryPrice > 0.0) item.entryPrice else item.currentPrice
        val calculatedQty = if (effectiveEntry > 0.0) {
            kotlin.math.max(1, (SLOT_BUDGET / effectiveEntry).toInt())
        } else item.quantity
        val invested = roundPaise(calculatedQty * effectiveEntry)

        list[idx] = item.copy(
            isApprovedForStrategy = true,
            strategySlotIndex = assignedSlot,
            strategySlotCapital = SLOT_BUDGET,
            quantity = calculatedQty,
            isClosed = false,
            closedPrice = 0.0,
            closedDate = "",
            exitReason = "",
            exitTriggerPrice = 0.0
        )
        memoryCache = list
        saveToDisk(list, executionEventsCache)
        return Pair(true, "Approved ${item.symbol} into Strategy Slot #$assignedSlot ($calculatedQty shares @ ₹$effectiveEntry ≈ ₹$invested).")
    }

    /**
     * Removes a stock from strategy slot allocation back to regular recommendation.
     */
    fun unapproveRecommendation(id: String): List<ArjunStockItem> = synchronized(lock) {
        val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(
                isApprovedForStrategy = false,
                strategySlotIndex = -1
            )
            memoryCache = list
            saveToDisk(list, executionEventsCache)
        }
        return list.toList()
    }

    /**
     * Evaluates daily Stop Loss and Profit Target levels for all active strategy positions.
     * When Stop Loss is hit (currentPrice <= stopLoss): auto-sells at stop loss price, freeing the slot.
     * When Target 1 is hit (currentPrice >= targetPrice1): auto-sells at target price, booking profit & freeing the slot.
     */
    fun evaluateStrategyTriggers(items: List<ArjunStockItem>): Pair<List<ArjunStockItem>, List<ArjunAutoExecutionEvent>> {
        val updated = items.toMutableList()
        val newEvents = mutableListOf<ArjunAutoExecutionEvent>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        for (i in updated.indices) {
            val stock = updated[i]
            if (!stock.isStrategySlotOccupied) continue

            // 1. Check Stop Loss Hit
            if (stock.stopLoss > 0.0 && stock.currentPrice <= stock.stopLoss) {
                val exitPrice = stock.stopLoss
                val pnl = roundPaise((exitPrice - stock.entryPrice) * stock.quantity)
                val pnlPct = if (stock.entryPrice > 0.0) roundPaise(((exitPrice - stock.entryPrice) / stock.entryPrice) * 100.0) else 0.0
                val freedSlot = stock.strategySlotIndex

                updated[i] = stock.copy(
                    isClosed = true,
                    closedPrice = exitPrice,
                    closedDate = todayStr,
                    exitReason = "STOP_LOSS_HIT",
                    exitTriggerPrice = stock.currentPrice
                )

                val event = ArjunAutoExecutionEvent(
                    stockId = stock.id,
                    symbol = stock.symbol,
                    timestamp = todayStr,
                    eventType = "STOP_LOSS_HIT",
                    triggerPrice = stock.currentPrice,
                    entryPrice = stock.entryPrice,
                    quantity = stock.quantity,
                    pnlAmount = pnl,
                    pnlPct = pnlPct,
                    freedSlotIndex = freedSlot,
                    headline = "🛑 Stop Loss Hit: ${stock.symbol} Auto-Sold",
                    message = "${stock.symbol} touched Stop Loss ₹${stock.stopLoss} (CMP ₹${stock.currentPrice}). Position auto-sold at ₹$exitPrice. P&L: ${if (pnl >= 0) "+" else ""}₹$pnl ($pnlPct%). Slot #$freedSlot freed up for new picks!"
                )
                newEvents.add(event)
            }
            // 2. Check Target Hit (Target 1)
            else if (stock.targetPrice1 > 0.0 && stock.currentPrice >= stock.targetPrice1) {
                val exitPrice = stock.targetPrice1
                val pnl = roundPaise((exitPrice - stock.entryPrice) * stock.quantity)
                val pnlPct = if (stock.entryPrice > 0.0) roundPaise(((exitPrice - stock.entryPrice) / stock.entryPrice) * 100.0) else 0.0
                val freedSlot = stock.strategySlotIndex

                updated[i] = stock.copy(
                    isClosed = true,
                    closedPrice = exitPrice,
                    closedDate = todayStr,
                    exitReason = "TARGET_HIT",
                    exitTriggerPrice = stock.currentPrice
                )

                val event = ArjunAutoExecutionEvent(
                    stockId = stock.id,
                    symbol = stock.symbol,
                    timestamp = todayStr,
                    eventType = "TARGET_HIT",
                    triggerPrice = stock.currentPrice,
                    entryPrice = stock.entryPrice,
                    quantity = stock.quantity,
                    pnlAmount = pnl,
                    pnlPct = pnlPct,
                    freedSlotIndex = freedSlot,
                    headline = "🎯 Profit Target Hit: ${stock.symbol} Auto-Sold!",
                    message = "${stock.symbol} reached Target 1 ₹${stock.targetPrice1} (CMP ₹${stock.currentPrice}). Position auto-sold. Profit booked: +₹$pnl (+${pnlPct}%). Slot #$freedSlot freed up for new picks!"
                )
                newEvents.add(event)
            }
        }
        return Pair(updated.toList(), newEvents.toList())
    }

    fun evaluateStrategyNow(): Pair<List<ArjunStockItem>, List<ArjunAutoExecutionEvent>> = synchronized(lock) {
        val current = getRecommendations()
        val (evaluatedItems, newEvents) = evaluateStrategyTriggers(current)
        if (newEvents.isNotEmpty()) {
            memoryCache = evaluatedItems.toMutableList()
            executionEventsCache.addAll(0, newEvents)
            saveToDisk(evaluatedItems, executionEventsCache)
        }
        Pair(evaluatedItems, newEvents)
    }

    suspend fun refreshLiveQuotes(): List<ArjunStockItem> = withContext(Dispatchers.IO) {
        try {
            val currentItems = getRecommendations()
            val updatedItems = mutableListOf<ArjunStockItem>()

            for (item in currentItems) {
                if (item.isClosed) {
                    updatedItems.add(item)
                    continue
                }

                val quote = fetchLiveQuote(item.symbol)
                if (quote != null) {
                    updatedItems.add(
                        item.copy(
                            currentPrice = quote.first,
                            todayChangePct = quote.second
                        )
                    )
                } else {
                    updatedItems.add(item)
                }
            }

            // Daily monitoring trigger evaluation
            val (evaluatedItems, newEvents) = evaluateStrategyTriggers(updatedItems)

            synchronized(lock) {
                memoryCache = evaluatedItems.toMutableList()
                if (newEvents.isNotEmpty()) {
                    executionEventsCache.addAll(0, newEvents)
                }
                saveToDisk(evaluatedItems, executionEventsCache)
            }
            evaluatedItems
        } catch (_: Throwable) {
            getRecommendations()
        }
    }

    fun getAutoExecutionEvents(): List<ArjunAutoExecutionEvent> = synchronized(lock) {
        executionEventsCache.toList()
    }

    fun dismissAutoExecutionEvent(id: String): List<ArjunAutoExecutionEvent> = synchronized(lock) {
        executionEventsCache.removeAll { it.id == id }
        memoryCache?.let { saveToDisk(it, executionEventsCache) }
        executionEventsCache.toList()
    }

    fun calculateSummary(items: List<ArjunStockItem>): ArjunPortfolioSummary {
        val active = items.filter { !it.isClosed }
        val closed = items.filter { it.isClosed }

        val totalInvested = roundPaise(active.sumOf { it.investedValue })
        val currentValue = roundPaise(active.sumOf { it.currentValue })
        val totalPnl = roundPaise(currentValue - totalInvested)
        val totalPnlPct = if (totalInvested > 0.0) roundPaise((totalPnl / totalInvested) * 100.0) else 0.0

        val targetHits = items.count { it.status == ArjunTradeStatus.TARGET1_HIT || it.status == ArjunTradeStatus.TARGET2_HIT }
        val slHits = items.count { it.status == ArjunTradeStatus.STOP_LOSS_HIT }
        val wins = items.count { it.isProfit }
        val winRate = if (items.isNotEmpty()) roundPaise((wins.toDouble() / items.size.toDouble()) * 100.0) else 0.0

        return ArjunPortfolioSummary(
            totalInvested = totalInvested,
            currentValue = currentValue,
            totalPnl = totalPnl,
            totalPnlPct = totalPnlPct,
            activeCount = active.size,
            closedCount = closed.size,
            targetHitCount = targetHits,
            slHitCount = slHits,
            winRatePct = winRate
        )
    }

    fun calculateStrategySummary(items: List<ArjunStockItem>): ArjunStrategySummary {
        val strategyItems = items.filter { it.isApprovedForStrategy }
        val activeStrategyItems = strategyItems.filter { !it.isClosed }
        val closedStrategyItems = strategyItems.filter { it.isClosed }

        val occupiedSlots = activeStrategyItems.size.coerceAtMost(TOTAL_STRATEGY_SLOTS)
        val availableSlots = (TOTAL_STRATEGY_SLOTS - occupiedSlots).coerceAtLeast(0)

        val totalDeployedCapital = roundPaise(activeStrategyItems.sumOf { it.investedValue })
        val currentDeployedValue = roundPaise(activeStrategyItems.sumOf { it.currentValue })
        val unrealizedPnl = roundPaise(currentDeployedValue - totalDeployedCapital)

        val realizedPnl = roundPaise(closedStrategyItems.sumOf {
            (it.effectivePrice - it.entryPrice) * it.quantity
        })

        val totalPnl = roundPaise(unrealizedPnl + realizedPnl)
        val availableCash = roundPaise(TOTAL_STRATEGY_FUND - totalDeployedCapital + realizedPnl)
        val totalStrategyNav = roundPaise(availableCash + currentDeployedValue)
        val totalStrategyReturnPct = if (TOTAL_STRATEGY_FUND > 0.0) {
            roundPaise(((totalStrategyNav - TOTAL_STRATEGY_FUND) / TOTAL_STRATEGY_FUND) * 100.0)
        } else 0.0

        return ArjunStrategySummary(
            totalFund = TOTAL_STRATEGY_FUND,
            slotBudget = SLOT_BUDGET,
            totalSlots = TOTAL_STRATEGY_SLOTS,
            occupiedSlotsCount = occupiedSlots,
            availableSlotsCount = availableSlots,
            totalDeployedCapital = totalDeployedCapital,
            availableCash = availableCash,
            currentDeployedValue = currentDeployedValue,
            unrealizedPnl = unrealizedPnl,
            realizedPnl = realizedPnl,
            totalStrategyNav = totalStrategyNav,
            totalStrategyReturnPct = totalStrategyReturnPct
        )
    }

    fun calculateStrategySlots(items: List<ArjunStockItem>): List<ArjunStrategySlot> {
        val activeStrategyItems = items.filter { it.isStrategySlotOccupied }
        val slots = mutableListOf<ArjunStrategySlot>()
        for (i in 1..TOTAL_STRATEGY_SLOTS) {
            val matchingStock = activeStrategyItems.firstOrNull { it.strategySlotIndex == i }
            slots.add(
                ArjunStrategySlot(
                    slotIndex = i,
                    isOccupied = matchingStock != null,
                    stock = matchingStock,
                    targetCapital = SLOT_BUDGET
                )
            )
        }
        return slots
    }

    /**
     * Fetches live quote from Yahoo Finance API for given symbol: Pair(price, changePct)
     */
    private fun fetchLiveQuote(symbol: String): Pair<Double, Double>? {
        if (!enableNetworkQuotes) return null
        val cleanSym = symbol.trim().uppercase(Locale.US)
        val nseUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$cleanSym.NS?interval=1d&range=1d"

        return try {
            val jsonStr = httpGetWithUa(nseUrl) ?: return null
            val root = JSONObject(jsonStr)
            val chart = root.optJSONObject("chart") ?: return null
            val resultArr = chart.optJSONArray("result") ?: return null
            if (resultArr.length() == 0) return null
            val result = resultArr.getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", 0.0)
            val changePct = meta.optDouble("regularMarketChangePercent", 0.0)
            if (price > 0.0) Pair(roundPaise(price), roundPaise(changePct)) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGetWithUa(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URI.create(urlStr).toURL()
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun loadFromDisk(): List<ArjunStockItem>? {
        val dir = VaralakshmiRepository.getCacheDirectory() ?: return null
        val file = File(dir, ARJUN_CACHE_FILE)
        if (!file.exists() || !file.isFile) return null

        return try {
            val text = file.readText(Charsets.UTF_8).trim()
            val list = mutableListOf<ArjunStockItem>()
            val itemsArr: JSONArray? = if (text.startsWith("{")) {
                val root = JSONObject(text)
                val evArr = root.optJSONArray("events")
                if (evArr != null) {
                    executionEventsCache.clear()
                    for (j in 0 until evArr.length()) {
                        val e = evArr.optJSONObject(j) ?: continue
                        executionEventsCache.add(
                            ArjunAutoExecutionEvent(
                                id = e.optString("id", ""),
                                stockId = e.optString("stock_id", ""),
                                symbol = e.optString("symbol", ""),
                                timestamp = e.optString("timestamp", ""),
                                eventType = e.optString("event_type", "TARGET_HIT"),
                                triggerPrice = e.optDouble("trigger_price", 0.0),
                                entryPrice = e.optDouble("entry_price", 0.0),
                                quantity = e.optInt("quantity", 0),
                                pnlAmount = e.optDouble("pnl_amount", 0.0),
                                pnlPct = e.optDouble("pnl_pct", 0.0),
                                freedSlotIndex = e.optInt("freed_slot_index", 1),
                                headline = e.optString("headline", ""),
                                message = e.optString("message", "")
                            )
                        )
                    }
                }
                root.optJSONArray("items")
            } else if (text.startsWith("[")) {
                JSONArray(text)
            } else null

            if (itemsArr == null) return null

            for (i in 0 until itemsArr.length()) {
                val o = itemsArr.optJSONObject(i) ?: continue
                list.add(
                    ArjunStockItem(
                        id = o.optString("id", ""),
                        symbol = o.optString("symbol", ""),
                        companyName = o.optString("company_name", ""),
                        brokerSource = o.optString("broker_source", "ARJUN Combo Package 🚀"),
                        tradeType = o.optString("trade_type", "SWING TRADE"),
                        action = o.optString("action", "BUY"),
                        entryPrice = o.optDouble("entry_price", 0.0),
                        entryPriceRange = o.optString("entry_price_range", ""),
                        entryDate = o.optString("entry_date", ""),
                        currentPrice = o.optDouble("current_price", 0.0),
                        todayChangePct = o.optDouble("today_change_pct", 0.0),
                        targetPrice1 = o.optDouble("target_price_1", 0.0),
                        targetPrice2 = o.optDouble("target_price_2", 0.0),
                        stopLoss = o.optDouble("stop_loss", 0.0),
                        quantity = o.optInt("quantity", 10),
                        notes = o.optString("notes", ""),
                        rawText = o.optString("raw_text", ""),
                        isClosed = o.optBoolean("is_closed", false),
                        closedPrice = o.optDouble("closed_price", 0.0),
                        closedDate = o.optString("closed_date", ""),
                        isApprovedForStrategy = o.optBoolean("is_approved_for_strategy", false),
                        strategySlotIndex = o.optInt("strategy_slot_index", -1),
                        strategySlotCapital = o.optDouble("strategy_slot_capital", SLOT_BUDGET),
                        exitReason = o.optString("exit_reason", ""),
                        exitTriggerPrice = o.optDouble("exit_trigger_price", 0.0)
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDisk(items: List<ArjunStockItem>, events: List<ArjunAutoExecutionEvent> = emptyList()) {
        val dir = VaralakshmiRepository.getCacheDirectory() ?: return
        try {
            val file = File(dir, ARJUN_CACHE_FILE)
            val root = JSONObject()
            val itemsArr = JSONArray()
            for (it in items) {
                val o = JSONObject().apply {
                    put("id", it.id)
                    put("symbol", it.symbol)
                    put("company_name", it.companyName)
                    put("broker_source", it.brokerSource)
                    put("trade_type", it.tradeType)
                    put("action", it.action)
                    put("entry_price", it.entryPrice)
                    put("entry_price_range", it.entryPriceRange)
                    put("entry_date", it.entryDate)
                    put("current_price", it.currentPrice)
                    put("today_change_pct", it.todayChangePct)
                    put("target_price_1", it.targetPrice1)
                    put("target_price_2", it.targetPrice2)
                    put("stop_loss", it.stopLoss)
                    put("quantity", it.quantity)
                    put("notes", it.notes)
                    put("raw_text", it.rawText)
                    put("is_closed", it.isClosed)
                    put("closed_price", it.closedPrice)
                    put("closed_date", it.closedDate)
                    put("is_approved_for_strategy", it.isApprovedForStrategy)
                    put("strategy_slot_index", it.strategySlotIndex)
                    put("strategy_slot_capital", it.strategySlotCapital)
                    put("exit_reason", it.exitReason)
                    put("exit_trigger_price", it.exitTriggerPrice)
                }
                itemsArr.put(o)
            }
            root.put("items", itemsArr)

            val evArr = JSONArray()
            for (e in events) {
                val eo = JSONObject().apply {
                    put("id", e.id)
                    put("stock_id", e.stockId)
                    put("symbol", e.symbol)
                    put("timestamp", e.timestamp)
                    put("event_type", e.eventType)
                    put("trigger_price", e.triggerPrice)
                    put("entry_price", e.entryPrice)
                    put("quantity", e.quantity)
                    put("pnl_amount", e.pnlAmount)
                    put("pnl_pct", e.pnlPct)
                    put("freed_slot_index", e.freedSlotIndex)
                    put("headline", e.headline)
                    put("message", e.message)
                }
                evArr.put(eo)
            }
            root.put("events", evArr)

            file.writeText(root.toString(2), Charsets.UTF_8)
        } catch (_: Exception) {
            // Ignore disk cache write errors
        }
    }

    private fun roundPaise(v: Double): Double =
        if (v.isNaN() || v.isInfinite()) 0.0
        else BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_EVEN).toDouble()
}
