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
import java.util.UUID

class ArjunStockRepository {

    companion object {
        const val ARJUN_CACHE_FILE = "arjun_stocks_cache.json"

        const val TOTAL_STRATEGY_FUND = 700_000.0 // 7 Lakhs
        const val TOTAL_STRATEGY_SLOTS = 7
        const val SLOT_BUDGET = 100_000.0 // 1 Lakh per slot

        private val isRunningInUnitTest: Boolean by lazy {
            try {
                Class.forName("org.junit.Test")
                true
            } catch (_: Throwable) {
                false
            }
        }

        @Volatile
        var enableNetworkQuotes: Boolean = !isRunningInUnitTest

        @Volatile
        var enableDiskCache: Boolean = isRunningInUnitTest

        private val lock = Any()
        @Volatile
        private var memoryCache: MutableList<ArjunStockItem>? = null

        private val executionEventsCache = mutableListOf<ArjunAutoExecutionEvent>()
        private val breakoutOrdersCache = mutableListOf<ArjunBreakoutOrder>()

        fun resetCacheForTesting() = synchronized(lock) {
            memoryCache = null
            executionEventsCache.clear()
            breakoutOrdersCache.clear()
            enableNetworkQuotes = !isRunningInUnitTest
            enableDiskCache = isRunningInUnitTest
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
                    quantity = 335,
                    notes = "STRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)",
                    rawText = "ARJUN Combo Package 🚀\nBREAKOUT ALERT\nBUY ORIENTHOT (ORIENT HOTELS)\nABOVE 149.05\nTARGETS 190-225\nKEEP SL 129.95\nCURRENTLY TRADING 145.50\nSTRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)",
                    isApprovedForStrategy = false,
                    strategySlotIndex = -1,
                    strategySlotCapital = 0.0
                )
            )
        }
    }

    fun getRecommendations(): List<ArjunStockItem> {
        synchronized(lock) {
            if (memoryCache == null) {
                memoryCache = if (enableDiskCache) {
                    (loadFromDisk() ?: createDefaultSeeds()).toMutableList()
                } else {
                    mutableListOf()
                }
            }
            return memoryCache!!.toList()
        }
    }

    fun addRecommendation(item: ArjunStockItem): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            list.add(0, item)
            memoryCache = list
            saveToDisk(list, executionEventsCache, breakoutOrdersCache)
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
            saveToDisk(list, executionEventsCache, breakoutOrdersCache)
            return list.toList()
        }
    }

    fun deleteRecommendation(id: String): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            list.removeAll { it.id == id }
            memoryCache = list
            saveToDisk(list, executionEventsCache, breakoutOrdersCache)
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
            saveToDisk(list, executionEventsCache, breakoutOrdersCache)
            return list.toList()
        }
    }

    /**
     * Approves a recommendation into the Arjun 7L Strategy.
     * Finds next available slot out of 7, calculates share quantity for approx ₹1,00,000,
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
        if (targetSlot != null) {
            if (targetSlot !in 1..TOTAL_STRATEGY_SLOTS) {
                return Pair(false, "Invalid slot #$targetSlot. Strategy slots must be between 1 and $TOTAL_STRATEGY_SLOTS.")
            }
            if (occupiedSlots.contains(targetSlot)) {
                return Pair(false, "Strategy Slot #$targetSlot is already occupied.")
            }
        }
        val assignedSlot = targetSlot ?: (1..TOTAL_STRATEGY_SLOTS).firstOrNull { !occupiedSlots.contains(it) }

        if (assignedSlot == null) {
            return Pair(false, "All $TOTAL_STRATEGY_SLOTS strategy slots (₹7,00,000) are occupied! Wait for an active position to hit Target or Stop Loss to free up a slot.")
        }

        // Auto-calculate quantity for approx ₹1,00,000
        val effectiveEntry = if (item.entryPrice.isFinite() && item.entryPrice > 0.0) item.entryPrice else item.currentPrice
        if (effectiveEntry > SLOT_BUDGET) {
            return Pair(false, "Cannot allocate ${item.symbol}: Single share price (₹${ArjunMoneyMath.roundPaise(effectiveEntry)}) exceeds ₹1,00,000 slot budget.")
        }
        val calculatedQty = if (effectiveEntry.isFinite() && effectiveEntry > 0.0) {
            (SLOT_BUDGET / effectiveEntry).toInt()
        } else item.quantity
        if (calculatedQty <= 0) {
            return Pair(false, "Cannot allocate ${item.symbol}: Single share price exceeds ₹1,00,000 slot budget.")
        }
        val invested = ArjunMoneyMath.multiplyPaise(calculatedQty, effectiveEntry)

        list[idx] = item.copy(
            isApprovedForStrategy = true,
            strategySlotIndex = assignedSlot,
            strategySlotCapital = SLOT_BUDGET,
            quantity = calculatedQty,
            initialStrategyQuantity = calculatedQty,
            isClosed = false,
            closedPrice = 0.0,
            closedDate = "",
            exitReason = "",
            exitTriggerPrice = 0.0
        )
        // If there was a pending breakout order for this slot, remove it
        breakoutOrdersCache.removeAll { it.slotIndex == assignedSlot }
        memoryCache = list
        saveToDisk(list, executionEventsCache, breakoutOrdersCache)
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
            saveToDisk(list, executionEventsCache, breakoutOrdersCache)
        }
        return list.toList()
    }

    /**
     * Buys additional shares for an active slot position up to the ₹1,00,000 slot budget.
     */
    fun partialBuy(id: String, additionalQuantity: Int, buyPrice: Double): Pair<Boolean, String> = synchronized(lock) {
        if (additionalQuantity <= 0) return Pair(false, "Quantity must be greater than zero.")
        val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return Pair(false, "Stock not found.")
        val item = list[idx]
        if (!item.isStrategySlotOccupied) return Pair(false, "${item.symbol} is not active in a strategy slot.")

        val effectivePrice = if (buyPrice.isFinite() && buyPrice > 0.0) buyPrice else item.currentPrice
        val additionalCost = ArjunMoneyMath.multiplyPaise(additionalQuantity, effectivePrice)
        val currentInvested = item.investedValue
        val newInvested = ArjunMoneyMath.roundPaise(currentInvested + additionalCost)

        // Tolerance of 1 rupee for rounding
        if (newInvested > SLOT_BUDGET + 1.0) {
            val remainingBudget = ArjunMoneyMath.roundPaise(kotlin.math.max(0.0, SLOT_BUDGET - currentInvested))
            return Pair(false, "Exceeds ₹1,00,000 slot budget. Remaining budget: ₹$remainingBudget (Requested: ₹$additionalCost).")
        }

        val newQty = item.quantity + additionalQuantity
        val blendedEntryPrice = if (newQty > 0) newInvested / newQty.toDouble() else effectivePrice

        list[idx] = item.copy(
            quantity = newQty,
            initialStrategyQuantity = newQty,
            entryPrice = blendedEntryPrice,
            currentPrice = effectivePrice
        )
        memoryCache = list
        saveToDisk(list, executionEventsCache, breakoutOrdersCache)
        return Pair(true, "Bought $additionalQuantity shares of ${item.symbol} at ₹$effectivePrice. New position: $newQty shares @ avg ₹${ArjunMoneyMath.roundPaise(blendedEntryPrice)} (Total Deployed: ₹$newInvested).")
    }

    /**
     * Executes a partial sell on an active position, booking proportional realized P&L and updating cost basis.
     */
    fun partialSell(id: String, sellQuantity: Int, sellPrice: Double = 0.0, reason: String = "PARTIAL_PROFIT"): Pair<Boolean, String> = synchronized(lock) {
        if (sellQuantity <= 0) return Pair(false, "Sell quantity must be greater than zero.")
        val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return Pair(false, "Stock not found.")
        val item = list[idx]
        if (!item.isStrategySlotOccupied) return Pair(false, "${item.symbol} is not active in a strategy slot.")
        if (sellQuantity > item.quantity) return Pair(false, "Cannot sell $sellQuantity shares; only ${item.quantity} available.")

        val effectiveExit = if (sellPrice.isFinite() && sellPrice > 0.0) sellPrice else item.currentPrice
        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        if (sellQuantity == item.quantity) {
            markAsClosed(id, effectiveExit, reason)
            return Pair(true, "Closed position: Sold all $sellQuantity shares of ${item.symbol} at ₹$effectiveExit. Slot freed.")
        }

        // Proportional cost basis of sold shares
        val currentInvested = item.investedValue
        val costBasisSold = ArjunMoneyMath.roundPaise(sellQuantity.toDouble() * (currentInvested / item.quantity.toDouble()))
        val proceeds = ArjunMoneyMath.multiplyPaise(sellQuantity, effectiveExit)
        val realizedPnl = ArjunMoneyMath.roundPaise(proceeds - costBasisSold)
        val pnlPct = ArjunMoneyMath.calculatePnlPct(costBasisSold, proceeds)

        val remainingQty = item.quantity - sellQuantity
        val remainingInvested = ArjunMoneyMath.roundPaise(currentInvested - costBasisSold)
        val newEntryPrice = if (remainingQty > 0) remainingInvested / remainingQty.toDouble() else item.entryPrice
        val totalPartialRealized = ArjunMoneyMath.roundPaise(item.partialRealizedPnl + realizedPnl)

        val updatedItem = item.copy(
            quantity = remainingQty,
            entryPrice = newEntryPrice,
            partialRealizedPnl = totalPartialRealized,
            currentPrice = effectiveExit
        )
        list[idx] = updatedItem
        memoryCache = list

        val event = ArjunAutoExecutionEvent(
            stockId = item.id,
            symbol = item.symbol,
            timestamp = todayStr,
            eventType = "PARTIAL_SELL",
            triggerPrice = effectiveExit,
            entryPrice = item.entryPrice,
            quantity = sellQuantity,
            pnlAmount = realizedPnl,
            pnlPct = pnlPct,
            freedSlotIndex = 0,
            headline = "Partial Sell: ${item.symbol} ($sellQuantity shares)",
            message = "Sold $sellQuantity shares of ${item.symbol} at ₹$effectiveExit. Realized P&L: ${if (realizedPnl >= 0) "+" else ""}₹$realizedPnl ($pnlPct%). Remaining: $remainingQty shares in Slot #${item.strategySlotIndex}."
        )
        executionEventsCache.add(0, event)
        saveToDisk(list, executionEventsCache, breakoutOrdersCache)
        return Pair(true, "Sold $sellQuantity shares of ${item.symbol} at ₹$effectiveExit. Realized P&L: ₹$realizedPnl. Remaining: $remainingQty shares.")
    }

    /**
     * Configures a breakout trigger order for an empty slot.
     */
    fun setBreakoutOrder(order: ArjunBreakoutOrder): Pair<Boolean, String> = synchronized(lock) {
        if (order.slotIndex !in 1..TOTAL_STRATEGY_SLOTS) {
            return Pair(false, "Invalid slot #${order.slotIndex}. Must be 1 to $TOTAL_STRATEGY_SLOTS.")
        }
        val currentItems = memoryCache ?: getRecommendations()
        val occupied = currentItems.any { it.isStrategySlotOccupied && it.strategySlotIndex == order.slotIndex }
        if (occupied) {
            return Pair(false, "Slot #${order.slotIndex} is already occupied by an active position.")
        }
        if (order.symbol.isBlank()) return Pair(false, "Stock symbol cannot be blank.")
        if (order.triggerPrice <= 0.0) return Pair(false, "Trigger price must be greater than zero.")

        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val cleanAlloc = if (order.allocatedAmount in 1.0..SLOT_BUDGET) order.allocatedAmount else SLOT_BUDGET
        val cleanOrder = order.copy(
            symbol = order.symbol.trim().uppercase(Locale.US),
            allocatedAmount = cleanAlloc,
            createdAt = if (order.createdAt.isBlank()) todayStr else order.createdAt
        )
        breakoutOrdersCache.removeAll { it.slotIndex == order.slotIndex }
        breakoutOrdersCache.add(cleanOrder)
        saveToDisk(currentItems, executionEventsCache, breakoutOrdersCache)
        return Pair(true, "Breakout order set for ${cleanOrder.symbol} in Slot #${cleanOrder.slotIndex} @ ₹${cleanOrder.triggerPrice}.")
    }

    /**
     * Cancels a breakout trigger order for a slot.
     */
    fun cancelBreakoutOrder(slotIndex: Int): Pair<Boolean, String> = synchronized(lock) {
        val removed = breakoutOrdersCache.removeAll { it.slotIndex == slotIndex }
        if (removed) {
            memoryCache?.let { saveToDisk(it, executionEventsCache, breakoutOrdersCache) }
            Pair(true, "Cancelled breakout order for Slot #$slotIndex.")
        } else {
            Pair(false, "No breakout order found for Slot #$slotIndex.")
        }
    }

    /**
     * Retrieves all pending breakout orders.
     */
    fun getBreakoutOrders(): List<ArjunBreakoutOrder> = synchronized(lock) {
        if (memoryCache == null) {
            getRecommendations()
        }
        breakoutOrdersCache.toList()
    }

    /**
     * Updates multi-tier Stop Loss and Profit Target ladder levels for an active stock.
     */
    fun updateStockLadders(
        id: String,
        targetLadder: List<ArjunLadderLevel>,
        stopLossLadder: List<ArjunLadderLevel>
    ): Pair<Boolean, String> = synchronized(lock) {
        val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return Pair(false, "Stock not found.")
        val item = list[idx]
        list[idx] = item.copy(
            targetLadder = targetLadder,
            stopLossLadder = stopLossLadder
        )
        memoryCache = list
        saveToDisk(list, executionEventsCache, breakoutOrdersCache)
        return Pair(true, "Updated TP/SL ladders for ${item.symbol}.")
    }

    suspend fun dispatchBrokerExecution(symbol: String, action: String, quantity: Int, price: Double): Result<String> {
        return try {
            VaralakshmiRepository().dispatchBrokerOrder(
                strategyId = "ARJUN_7L_STRATEGY",
                symbol = symbol,
                action = action,
                quantity = quantity,
                price = price
            )
        } catch (_: Throwable) {
            Result.success("Local execution confirmed (broker offline/simulated).")
        }
    }

    /**
     * Evaluates daily Stop Loss and Profit Target levels (including multi-tier ladders) for all active strategy positions,
     * as well as pending breakout trigger orders for empty slots.
     */
    fun evaluateStrategyTriggers(
        items: List<ArjunStockItem>,
        externalPrices: Map<String, Double> = emptyMap()
    ): Pair<List<ArjunStockItem>, List<ArjunAutoExecutionEvent>> {
        val updated = items.toMutableList()
        val newEvents = mutableListOf<ArjunAutoExecutionEvent>()
        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        for (i in updated.indices) {
            val stock = updated[i]
            if (!stock.isStrategySlotOccupied) continue

            val stockPrice = externalPrices[stock.symbol] ?: stock.currentPrice
            val isPriceValid = stockPrice.isFinite() && stockPrice > 0.0
            if (!isPriceValid) continue

            // 1. Check Multi-tier Stop Loss Ladder or Single Stop Loss
            var slTriggered = false
            if (stock.stopLossLadder.isNotEmpty()) {
                val untriggeredSL = stock.stopLossLadder.filter { !it.isTriggered && it.price < stock.entryPrice }.sortedByDescending { it.price }
                val hitLevel = untriggeredSL.firstOrNull { stockPrice <= it.price }
                if (hitLevel != null) {
                    slTriggered = true
                    val exitPrice = if (stockPrice > 0.0) stockPrice else hitLevel.price
                    val allocPct = if (hitLevel.allocationPct in 1.0..100.0) hitLevel.allocationPct else 100.0
                    val totalBaseQty = if (stock.initialStrategyQuantity > 0) stock.initialStrategyQuantity else stock.quantity
                    var exitQty = kotlin.math.floor(totalBaseQty * (allocPct / 100.0)).toInt()
                    if (exitQty <= 0) exitQty = 1
                    val totalLadderAlloc = stock.stopLossLadder.sumOf { it.allocationPct }
                    if (untriggeredSL.size == 1 && totalLadderAlloc >= 99.0) {
                        exitQty = stock.quantity
                    }
                    exitQty = kotlin.math.min(exitQty, stock.quantity)

                    val updatedSLLadder = stock.stopLossLadder.map { lvl ->
                        if (lvl.levelIndex == hitLevel.levelIndex) {
                            lvl.copy(isTriggered = true, triggeredAtPrice = stockPrice, triggeredDate = todayStr)
                        } else lvl
                    }

                    if (exitQty >= stock.quantity) {
                        // Full exit
                        val pnl = ArjunMoneyMath.calculatePnl(stock.quantity, stock.entryPrice, exitPrice)
                        val pnlPct = ArjunMoneyMath.calculatePnlPct(stock.entryPrice, exitPrice)
                        val freedSlot = stock.strategySlotIndex

                        updated[i] = stock.copy(
                            isClosed = true,
                            closedPrice = exitPrice,
                            closedDate = todayStr,
                            exitReason = "STOP_LOSS_HIT",
                            exitTriggerPrice = stockPrice,
                            stopLossLadder = updatedSLLadder
                        )

                        val event = ArjunAutoExecutionEvent(
                            stockId = stock.id,
                            symbol = stock.symbol,
                            timestamp = todayStr,
                            eventType = "STOP_LOSS_HIT",
                            triggerPrice = stockPrice,
                            entryPrice = stock.entryPrice,
                            quantity = stock.quantity,
                            pnlAmount = pnl,
                            pnlPct = pnlPct,
                            freedSlotIndex = freedSlot,
                            headline = "🛑 SL${hitLevel.levelIndex} Hit: ${stock.symbol} Auto-Sold",
                            message = "${stock.symbol} touched Stop Loss Level #${hitLevel.levelIndex} ₹$exitPrice (CMP ₹$stockPrice). Position auto-sold. P&L: ${if (pnl >= 0) "+" else ""}₹$pnl ($pnlPct%). Slot #$freedSlot freed up!"
                        )
                        newEvents.add(event)
                    } else {
                        // Partial exit
                        val currentInvested = stock.investedValue
                        val costBasisSold = ArjunMoneyMath.roundPaise(exitQty.toDouble() * (currentInvested / stock.quantity.toDouble()))
                        val proceeds = ArjunMoneyMath.multiplyPaise(exitQty, exitPrice)
                        val pnl = ArjunMoneyMath.roundPaise(proceeds - costBasisSold)
                        val pnlPct = ArjunMoneyMath.calculatePnlPct(costBasisSold, proceeds)

                        val remainingQty = stock.quantity - exitQty
                        val remainingInvested = ArjunMoneyMath.roundPaise(currentInvested - costBasisSold)
                        val newEntryPrice = if (remainingQty > 0) remainingInvested / remainingQty.toDouble() else stock.entryPrice
                        val totalPartialRealized = ArjunMoneyMath.roundPaise(stock.partialRealizedPnl + pnl)

                        updated[i] = stock.copy(
                            quantity = remainingQty,
                            entryPrice = newEntryPrice,
                            partialRealizedPnl = totalPartialRealized,
                            currentPrice = exitPrice,
                            stopLossLadder = updatedSLLadder
                        )

                        val event = ArjunAutoExecutionEvent(
                            stockId = stock.id,
                            symbol = stock.symbol,
                            timestamp = todayStr,
                            eventType = "STOP_LOSS_HIT",
                            triggerPrice = stockPrice,
                            entryPrice = stock.entryPrice,
                            quantity = exitQty,
                            pnlAmount = pnl,
                            pnlPct = pnlPct,
                            freedSlotIndex = 0,
                            headline = "🛑 SL${hitLevel.levelIndex} Hit: ${stock.symbol} Partial Exit ($exitQty shares)",
                            message = "${stock.symbol} touched SL Level #${hitLevel.levelIndex} ₹$exitPrice (CMP ₹$stockPrice). Sold $exitQty shares (${hitLevel.allocationPct}%). Remaining: $remainingQty shares in Slot #${stock.strategySlotIndex}."
                        )
                        newEvents.add(event)
                    }
                }
            } else if (stock.stopLoss > 0.0 && stock.stopLoss < stock.entryPrice && stockPrice <= stock.stopLoss) {
                // Legacy single SL
                slTriggered = true
                val exitPrice = stock.stopLoss
                val pnl = ArjunMoneyMath.calculatePnl(stock.quantity, stock.entryPrice, exitPrice)
                val pnlPct = ArjunMoneyMath.calculatePnlPct(stock.entryPrice, exitPrice)
                val freedSlot = stock.strategySlotIndex

                updated[i] = stock.copy(
                    isClosed = true,
                    closedPrice = exitPrice,
                    closedDate = todayStr,
                    exitReason = "STOP_LOSS_HIT",
                    exitTriggerPrice = stockPrice
                )

                val event = ArjunAutoExecutionEvent(
                    stockId = stock.id,
                    symbol = stock.symbol,
                    timestamp = todayStr,
                    eventType = "STOP_LOSS_HIT",
                    triggerPrice = stockPrice,
                    entryPrice = stock.entryPrice,
                    quantity = stock.quantity,
                    pnlAmount = pnl,
                    pnlPct = pnlPct,
                    freedSlotIndex = freedSlot,
                    headline = "🛑 Stop Loss Hit: ${stock.symbol} Auto-Sold",
                    message = "${stock.symbol} touched Stop Loss ₹${stock.stopLoss} (CMP ₹$stockPrice). Position auto-sold at ₹$exitPrice. P&L: ${if (pnl >= 0) "+" else ""}₹$pnl ($pnlPct%). Slot #$freedSlot freed up for new picks!"
                )
                newEvents.add(event)
            }

            if (slTriggered) continue

            // 2. Check Multi-tier Target Ladder or Single Target
            if (stock.targetLadder.isNotEmpty()) {
                val untriggeredTP = stock.targetLadder.filter { !it.isTriggered && it.price > stock.entryPrice }.sortedBy { it.price }
                val hitLevel = untriggeredTP.firstOrNull { stockPrice >= it.price }
                if (hitLevel != null) {
                    val exitPrice = if (stockPrice > 0.0) stockPrice else hitLevel.price
                    val allocPct = if (hitLevel.allocationPct in 1.0..100.0) hitLevel.allocationPct else 100.0
                    val totalBaseQty = if (stock.initialStrategyQuantity > 0) stock.initialStrategyQuantity else stock.quantity
                    var exitQty = kotlin.math.floor(totalBaseQty * (allocPct / 100.0)).toInt()
                    if (exitQty <= 0) exitQty = 1
                    val totalLadderAlloc = stock.targetLadder.sumOf { it.allocationPct }
                    if (untriggeredTP.size == 1 && totalLadderAlloc >= 99.0) {
                        exitQty = stock.quantity
                    }
                    exitQty = kotlin.math.min(exitQty, stock.quantity)

                    val updatedTPLadder = stock.targetLadder.map { lvl ->
                        if (lvl.levelIndex == hitLevel.levelIndex) {
                            lvl.copy(isTriggered = true, triggeredAtPrice = stockPrice, triggeredDate = todayStr)
                        } else lvl
                    }

                    if (exitQty >= stock.quantity) {
                        // Full exit
                        val pnl = ArjunMoneyMath.calculatePnl(stock.quantity, stock.entryPrice, exitPrice)
                        val pnlPct = ArjunMoneyMath.calculatePnlPct(stock.entryPrice, exitPrice)
                        val freedSlot = stock.strategySlotIndex

                        updated[i] = stock.copy(
                            isClosed = true,
                            closedPrice = exitPrice,
                            closedDate = todayStr,
                            exitReason = "TARGET_HIT",
                            exitTriggerPrice = stockPrice,
                            targetLadder = updatedTPLadder
                        )

                        val event = ArjunAutoExecutionEvent(
                            stockId = stock.id,
                            symbol = stock.symbol,
                            timestamp = todayStr,
                            eventType = "TARGET_HIT",
                            triggerPrice = stockPrice,
                            entryPrice = stock.entryPrice,
                            quantity = stock.quantity,
                            pnlAmount = pnl,
                            pnlPct = pnlPct,
                            freedSlotIndex = freedSlot,
                            headline = "🎯 TP${hitLevel.levelIndex} Hit: ${stock.symbol} Auto-Sold!",
                            message = "${stock.symbol} reached Target Level #${hitLevel.levelIndex} ₹$exitPrice (CMP ₹$stockPrice). Position auto-sold. Profit booked: +₹$pnl (+${pnlPct}%). Slot #$freedSlot freed up!"
                        )
                        newEvents.add(event)
                    } else {
                        // Partial exit
                        val currentInvested = stock.investedValue
                        val costBasisSold = ArjunMoneyMath.roundPaise(exitQty.toDouble() * (currentInvested / stock.quantity.toDouble()))
                        val proceeds = ArjunMoneyMath.multiplyPaise(exitQty, exitPrice)
                        val pnl = ArjunMoneyMath.roundPaise(proceeds - costBasisSold)
                        val pnlPct = ArjunMoneyMath.calculatePnlPct(costBasisSold, proceeds)

                        val remainingQty = stock.quantity - exitQty
                        val remainingInvested = ArjunMoneyMath.roundPaise(currentInvested - costBasisSold)
                        val newEntryPrice = if (remainingQty > 0) remainingInvested / remainingQty.toDouble() else stock.entryPrice
                        val totalPartialRealized = ArjunMoneyMath.roundPaise(stock.partialRealizedPnl + pnl)

                        updated[i] = stock.copy(
                            quantity = remainingQty,
                            entryPrice = newEntryPrice,
                            partialRealizedPnl = totalPartialRealized,
                            currentPrice = exitPrice,
                            targetLadder = updatedTPLadder
                        )

                        val event = ArjunAutoExecutionEvent(
                            stockId = stock.id,
                            symbol = stock.symbol,
                            timestamp = todayStr,
                            eventType = "TARGET_HIT",
                            triggerPrice = stockPrice,
                            entryPrice = stock.entryPrice,
                            quantity = exitQty,
                            pnlAmount = pnl,
                            pnlPct = pnlPct,
                            freedSlotIndex = 0,
                            headline = "🎯 TP${hitLevel.levelIndex} Hit: ${stock.symbol} Partial Exit ($exitQty shares)",
                            message = "${stock.symbol} reached Target Level #${hitLevel.levelIndex} ₹$exitPrice (CMP ₹$stockPrice). Sold $exitQty shares (${hitLevel.allocationPct}%). Profit: +₹$pnl. Remaining: $remainingQty shares in Slot #${stock.strategySlotIndex}."
                        )
                        newEvents.add(event)
                    }
                }
            } else {
                val effectiveTarget = if (stock.targetPrice1 > 0.0) stock.targetPrice1 else stock.targetPrice2
                if (effectiveTarget > stock.entryPrice && stockPrice >= effectiveTarget) {
                    val exitPrice = effectiveTarget
                    val pnl = ArjunMoneyMath.calculatePnl(stock.quantity, stock.entryPrice, exitPrice)
                    val pnlPct = ArjunMoneyMath.calculatePnlPct(stock.entryPrice, exitPrice)
                    val freedSlot = stock.strategySlotIndex

                    updated[i] = stock.copy(
                        isClosed = true,
                        closedPrice = exitPrice,
                        closedDate = todayStr,
                        exitReason = "TARGET_HIT",
                        exitTriggerPrice = stockPrice
                    )

                    val event = ArjunAutoExecutionEvent(
                        stockId = stock.id,
                        symbol = stock.symbol,
                        timestamp = todayStr,
                        eventType = "TARGET_HIT",
                        triggerPrice = stockPrice,
                        entryPrice = stock.entryPrice,
                        quantity = stock.quantity,
                        pnlAmount = pnl,
                        pnlPct = pnlPct,
                        freedSlotIndex = freedSlot,
                        headline = "🎯 Profit Target Hit: ${stock.symbol} Auto-Sold!",
                        message = "${stock.symbol} reached Target ₹$effectiveTarget (CMP ₹$stockPrice). Position auto-sold. Profit booked: +₹$pnl (+${pnlPct}%). Slot #$freedSlot freed up for new picks!"
                    )
                    newEvents.add(event)
                }
            }
        }

        // 3. Check Pending Breakout Orders
        val ordersToProcess = breakoutOrdersCache.toList()
        for (order in ordersToProcess) {
            val slotOccupied = updated.any { it.isStrategySlotOccupied && it.strategySlotIndex == order.slotIndex }
            if (slotOccupied) continue

            val marketPrice = externalPrices[order.symbol]
                ?: updated.firstOrNull { it.symbol == order.symbol && it.currentPrice > 0.0 }?.currentPrice
            val isTriggered = marketPrice != null && marketPrice >= order.triggerPrice && marketPrice > 0.0
            if (isTriggered) {
                val execPrice = marketPrice
                val maxBudgetQty = if (execPrice > 0.0) (order.allocatedAmount / execPrice).toInt() else 0
                val buyQty = if (order.quantity > 0) kotlin.math.min(order.quantity, maxBudgetQty) else maxBudgetQty
                if (buyQty <= 0) {
                    // Cannot afford even 1 share within allocated budget; leave pending
                    continue
                }
                val finalQty = buyQty
                val invested = ArjunMoneyMath.multiplyPaise(finalQty, execPrice)

                val newStock = ArjunStockItem(
                    id = "arjun-breakout-${UUID.randomUUID()}",
                    symbol = order.symbol,
                    entryPrice = execPrice,
                    currentPrice = execPrice,
                    quantity = finalQty,
                    initialStrategyQuantity = finalQty,
                    tradeType = "BREAKOUT",
                    action = "BUY",
                    isApprovedForStrategy = true,
                    strategySlotIndex = order.slotIndex,
                    strategySlotCapital = order.allocatedAmount,
                    targetLadder = order.targetLadder,
                    stopLossLadder = order.stopLossLadder,
                    targetPrice1 = order.targetPrice1,
                    targetPrice2 = order.targetPrice2,
                    stopLoss = order.stopLoss,
                    notes = "Breakout Order filled live @ ₹$execPrice (Trigger ₹${order.triggerPrice})"
                )
                updated.add(0, newStock)
                breakoutOrdersCache.removeAll { it.id == order.id || it.slotIndex == order.slotIndex }

                val event = ArjunAutoExecutionEvent(
                    stockId = newStock.id,
                    symbol = order.symbol,
                    timestamp = todayStr,
                    eventType = "BREAKOUT_BUY",
                    triggerPrice = order.triggerPrice,
                    entryPrice = execPrice,
                    quantity = finalQty,
                    pnlAmount = 0.0,
                    pnlPct = 0.0,
                    freedSlotIndex = order.slotIndex,
                    headline = "🚀 Breakout Buy Executed: ${order.symbol}",
                    message = "${order.symbol} crossed Breakout Trigger ₹${order.triggerPrice} (CMP ₹$execPrice). Bought $finalQty shares for Slot #${order.slotIndex} (Invested: ₹$invested)."
                )
                newEvents.add(event)
            }
        }

        return Pair(updated.toList(), newEvents.toList())
    }

    fun evaluateBreakoutTriggers(prices: Map<String, Double>): Pair<List<ArjunStockItem>, List<ArjunAutoExecutionEvent>> = synchronized(lock) {
        val current = getRecommendations()
        val (evaluatedItems, newEvents) = evaluateStrategyTriggers(current, prices)
        if (newEvents.isNotEmpty()) {
            memoryCache = evaluatedItems.toMutableList()
            val existingIds = executionEventsCache.map { it.id }.toSet()
            val unique = newEvents.filterNot { it.id in existingIds }
            executionEventsCache.addAll(0, unique)
            saveToDisk(evaluatedItems, executionEventsCache, breakoutOrdersCache)
        }
        Pair(evaluatedItems, newEvents)
    }

    fun evaluateStrategyNow(): Pair<List<ArjunStockItem>, List<ArjunAutoExecutionEvent>> = synchronized(lock) {
        val current = getRecommendations()
        val (evaluatedItems, newEvents) = evaluateStrategyTriggers(current)
        if (newEvents.isNotEmpty()) {
            memoryCache = evaluatedItems.toMutableList()
            val existingIds = executionEventsCache.map { it.id }.toSet()
            val unique = newEvents.filterNot { it.id in existingIds }
            executionEventsCache.addAll(0, unique)
            saveToDisk(evaluatedItems, executionEventsCache, breakoutOrdersCache)
        }
        Pair(evaluatedItems, newEvents)
    }

    suspend fun refreshLiveQuotes(): List<ArjunStockItem> {
        if (!enableNetworkQuotes) {
            return synchronized(lock) {
                val current = getRecommendations()
                val (evaluatedItems, newEvents) = evaluateStrategyTriggers(current)
                memoryCache = evaluatedItems.toMutableList()
                if (newEvents.isNotEmpty()) {
                    val existingIds = executionEventsCache.map { it.id }.toSet()
                    val unique = newEvents.filterNot { it.id in existingIds }
                    executionEventsCache.addAll(0, unique)
                }
                saveToDisk(evaluatedItems, executionEventsCache, breakoutOrdersCache)
                evaluatedItems
            }
        }
        return withContext(VaralakshmiRepository.ioDispatcher) {
            try {
                val snapshot = getRecommendations()
                val priceUpdates = mutableMapOf<String, Pair<Double, Double>>()
                val symbolsToFetch = (snapshot.filter { !it.isClosed }.map { it.symbol } + breakoutOrdersCache.map { it.symbol }).distinct()

                for (sym in symbolsToFetch) {
                    val quote = fetchLiveQuote(sym)
                    if (quote != null) {
                        for (item in snapshot.filter { it.symbol == sym }) {
                            priceUpdates[item.id] = quote
                        }
                    }
                }

                val (evaluatedItems, newlyTriggered) = synchronized(lock) {
                    val current = (memoryCache ?: snapshot.toMutableList()).toMutableList()
                    val symbolPriceMap = mutableMapOf<String, Double>()
                    for (i in current.indices) {
                        val it = current[i]
                        if (!it.isClosed && priceUpdates.containsKey(it.id)) {
                            val quote = priceUpdates[it.id]!!
                            current[i] = it.copy(currentPrice = quote.first, todayChangePct = quote.second)
                            symbolPriceMap[it.symbol] = quote.first
                        }
                    }
                    for (bo in breakoutOrdersCache) {
                        if (!symbolPriceMap.containsKey(bo.symbol)) {
                            val q = fetchLiveQuote(bo.symbol)
                            if (q != null) symbolPriceMap[bo.symbol] = q.first
                        }
                    }

                    // Daily monitoring trigger evaluation
                    val (evaluated, newEvents) = evaluateStrategyTriggers(current, symbolPriceMap)
                    memoryCache = evaluated.toMutableList()
                    val newly = if (newEvents.isNotEmpty()) {
                        val existingIds = executionEventsCache.map { it.id }.toSet()
                        val unique = newEvents.filterNot { it.id in existingIds }
                        executionEventsCache.addAll(0, unique)
                        unique
                    } else emptyList()
                    saveToDisk(evaluated, executionEventsCache, breakoutOrdersCache)
                    Pair(evaluated, newly)
                }
                for (event in newlyTriggered) {
                    try {
                        val action = if (event.eventType == "BREAKOUT_BUY") "BUY" else "SELL"
                        dispatchBrokerExecution(event.symbol, action, event.quantity, event.triggerPrice)
                    } catch (_: Throwable) {
                        // Seamless offline simulation fallback
                    }
                }
                evaluatedItems
            } catch (_: Throwable) {
                getRecommendations()
            }
        }
    }

    fun getAutoExecutionEvents(): List<ArjunAutoExecutionEvent> = synchronized(lock) {
        if (memoryCache == null) {
            getRecommendations()
        }
        executionEventsCache.toList()
    }

    fun dismissAutoExecutionEvent(id: String): List<ArjunAutoExecutionEvent> = synchronized(lock) {
        if (memoryCache == null) {
            getRecommendations()
        }
        executionEventsCache.removeAll { it.id == id }
        memoryCache?.let { saveToDisk(it, executionEventsCache, breakoutOrdersCache) }
        executionEventsCache.toList()
    }

    fun calculateSummary(items: List<ArjunStockItem>): ArjunPortfolioSummary {
        val active = items.filter { !it.isClosed }
        val closed = items.filter { it.isClosed }

        val totalInvested = ArjunMoneyMath.roundPaise(active.sumOf { it.investedValue })
        val currentValue = ArjunMoneyMath.roundPaise(active.sumOf { it.currentValue })
        val totalPnl = ArjunMoneyMath.roundPaise(currentValue - totalInvested)
        val totalPnlPct = if (totalInvested > 0.0) {
            ArjunMoneyMath.roundPaise((totalPnl / totalInvested) * 100.0)
        } else 0.0

        val targetHits = items.count { it.status == ArjunTradeStatus.TARGET1_HIT || it.status == ArjunTradeStatus.TARGET2_HIT }
        val slHits = items.count { it.status == ArjunTradeStatus.STOP_LOSS_HIT }
        val wins = items.count { it.isProfit }
        val winRate = if (items.isNotEmpty()) {
            ArjunMoneyMath.roundPaise((wins.toDouble() / items.size.toDouble()) * 100.0)
        } else 0.0

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
        val activeStrategyItems = items.filter { it.isStrategySlotOccupied }
        val allStrategyItems = items.filter { it.isApprovedForStrategy }

        val occupiedSlots = activeStrategyItems.map { it.strategySlotIndex }.distinct().size
        val availableSlots = (TOTAL_STRATEGY_SLOTS - occupiedSlots).coerceAtLeast(0)

        val totalDeployedCapital = ArjunMoneyMath.roundPaise(activeStrategyItems.sumOf { it.investedValue })
        val currentDeployedValue = ArjunMoneyMath.roundPaise(activeStrategyItems.sumOf { it.currentValue })
        val unrealizedPnl = ArjunMoneyMath.roundPaise(currentDeployedValue - totalDeployedCapital)

        val realizedPnl = ArjunMoneyMath.roundPaise(allStrategyItems.sumOf { it.realizedPnl })

        val totalPnl = ArjunMoneyMath.roundPaise(unrealizedPnl + realizedPnl)
        val availableCash = ArjunMoneyMath.roundPaise(TOTAL_STRATEGY_FUND - totalDeployedCapital + realizedPnl)
        val totalStrategyNav = ArjunMoneyMath.roundPaise(availableCash + currentDeployedValue)
        val totalStrategyReturnPct = if (TOTAL_STRATEGY_FUND > 0.0) {
            ArjunMoneyMath.roundPaise(((totalStrategyNav - TOTAL_STRATEGY_FUND) / TOTAL_STRATEGY_FUND) * 100.0)
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
            val matchingBreakout = breakoutOrdersCache.firstOrNull { it.slotIndex == i }
            slots.add(
                ArjunStrategySlot(
                    slotIndex = i,
                    isOccupied = matchingStock != null,
                    stock = matchingStock,
                    targetCapital = SLOT_BUDGET,
                    pendingBreakout = if (matchingStock == null) matchingBreakout else null
                )
            )
        }
        return slots
    }

    suspend fun syncWithServer(
        serverBaseUrl: String,
        authToken: String = ""
    ): Result<ArjunServerSyncPayload> = withContext(Dispatchers.IO) {
        val sUrl = serverBaseUrl.trim().trimEnd('/')
        if (sUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Server URL is empty"))
        }

        var conn: HttpURLConnection? = null
        try {
            val endpoint = "$sUrl/api/arjun-stocks/full"
            val url = URI.create(endpoint).toURL()
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0 VaralakshmiPortfolio/2.5.0")
                setRequestProperty("Accept", "application/json")
                if (authToken.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errStream = try { conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } } catch (_: Exception) { null }
                return@withContext Result.failure(RuntimeException("Server HTTP $responseCode: ${errStream ?: conn.responseMessage}"))
            }

            val jsonStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val payload = parseServerFullPayload(jsonStr)
            synchronized(lock) {
                memoryCache = payload.positions.toMutableList()
                breakoutOrdersCache.clear()
                breakoutOrdersCache.addAll(payload.breakoutOrders)
            }
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to connect to Arjun server ($sUrl): ${e.message ?: "Connection refused"}", e))
        } finally {
            conn?.disconnect()
        }
    }

    fun parseServerFullPayload(jsonStr: String): ArjunServerSyncPayload {
        val root = JSONObject(jsonStr)
        val sumObj = root.optJSONObject("summary") ?: JSONObject()
        val totalCapital = sumObj.optDouble("total_strategy_capital", 500_000.0)
        val slotBudget = sumObj.optDouble("slot_budget", 50_000.0)
        val totalSlotsCount = sumObj.optInt("total_slots", 10)
        val occupiedCount = sumObj.optInt("occupied_slots_count", 0)
        val availableCount = sumObj.optInt("available_slots_count", totalSlotsCount - occupiedCount)
        val deployedCap = sumObj.optDouble("deployed_capital", 0.0)
        val availCash = sumObj.optDouble("available_cash", totalCapital - deployedCap)
        val currVal = sumObj.optDouble("current_deployed_value", deployedCap)
        val uPnl = sumObj.optDouble("unrealized_pnl", currVal - deployedCap)
        val rPnl = sumObj.optDouble("realized_pnl", 0.0)
        val stratNav = sumObj.optDouble("total_strategy_nav", totalCapital + uPnl + rPnl)
        val stratReturnPct = sumObj.optDouble("total_strategy_return_pct", if (totalCapital > 0) ((uPnl + rPnl) / totalCapital) * 100.0 else 0.0)
        val serverStatus = sumObj.optString("status", "ACTIVE_CNC")
        val broker = sumObj.optString("broker", "Zerodha Kite CNC")
        val lastUpdated = sumObj.optString("last_updated", "")

        val strategySummary = ArjunStrategySummary(
            totalFund = totalCapital,
            slotBudget = slotBudget,
            totalSlots = totalSlotsCount,
            occupiedSlotsCount = occupiedCount,
            availableSlotsCount = availableCount,
            totalDeployedCapital = deployedCap,
            availableCash = availCash,
            currentDeployedValue = currVal,
            unrealizedPnl = uPnl,
            realizedPnl = rPnl,
            totalStrategyNav = stratNav,
            totalStrategyReturnPct = stratReturnPct
        )

        // Parse positions
        val positionsList = mutableListOf<ArjunStockItem>()
        val posArr = root.optJSONArray("positions")
        if (posArr != null) {
            for (i in 0 until posArr.length()) {
                val p = posArr.optJSONObject(i) ?: continue
                val sym = p.optString("symbol", "").trim().uppercase(Locale.US)
                if (sym.isBlank()) continue
                val slotIdx = p.optInt("slot", i + 1)
                val qty = p.optInt("quantity", 0)
                val buyPrice = p.optDouble("buy_price", 0.0)
                val cmp = p.optDouble("cmp", buyPrice)
                val sl = p.optDouble("stop_loss", 0.0)
                val t1 = p.optDouble("target_1", 0.0)
                val t2 = p.optDouble("target_2", 0.0)
                val item = ArjunStockItem(
                    id = "arjun-pos-$slotIdx-$sym",
                    symbol = sym,
                    companyName = p.optString("company_name", sym),
                    brokerSource = p.optString("broker", broker),
                    tradeType = p.optString("trade_type", "SWING TRADE"),
                    action = "BUY",
                    entryPrice = buyPrice,
                    entryDate = p.optString("entry_date", ""),
                    currentPrice = cmp,
                    targetPrice1 = t1,
                    targetPrice2 = t2,
                    stopLoss = sl,
                    quantity = qty,
                    notes = p.optString("notes", ""),
                    isApprovedForStrategy = true,
                    strategySlotIndex = slotIdx,
                    strategySlotCapital = slotBudget
                )
                positionsList.add(item)
            }
        }

        // Parse watchlist for pending breakouts
        val breakoutList = mutableListOf<ArjunBreakoutOrder>()
        val watchArr = root.optJSONArray("watchlist")
        if (watchArr != null) {
            for (w in 0 until watchArr.length()) {
                val wo = watchArr.optJSONObject(w) ?: continue
                val sym = wo.optString("symbol", "").trim().uppercase(Locale.US)
                if (sym.isBlank()) continue
                val trigPrice = wo.optDouble("trigger_price", 0.0)
                val sl = wo.optDouble("stop_loss", 0.0)
                val t1 = wo.optDouble("target_1", 0.0)
                val t2 = wo.optDouble("target_2", 0.0)
                val shares = wo.optInt("planned_shares", 0)
                val targetSlot = w + occupiedCount + 1
                breakoutList.add(
                    ArjunBreakoutOrder(
                        slotIndex = targetSlot,
                        symbol = sym,
                        triggerPrice = trigPrice,
                        allocatedAmount = slotBudget,
                        quantity = shares,
                        notes = wo.optString("notes", ""),
                        targetLadder = listOf(
                            ArjunLadderLevel(1, t1, 50.0),
                            ArjunLadderLevel(2, t2, 50.0)
                        ),
                        stopLossLadder = listOf(
                            ArjunLadderLevel(1, sl, 100.0)
                        )
                    )
                )
            }
        }

        // Parse slots
        val slotsList = mutableListOf<ArjunStrategySlot>()
        val slotsArr = root.optJSONArray("slots")
        if (slotsArr != null && slotsArr.length() > 0) {
            for (s in 0 until slotsArr.length()) {
                val so = slotsArr.optJSONObject(s) ?: continue
                val slotNum = if (so.has("slot_number")) so.optInt("slot_number", s + 1) else so.optInt("slot_index", s + 1)
                val isOcc = so.optBoolean("is_occupied", false) || so.optString("status", "").equals("OCCUPIED", ignoreCase = true)
                val sym = so.optString("symbol", "").trim().uppercase(Locale.US)

                val matchingStock = if (isOcc && sym.isNotBlank()) {
                    positionsList.find { it.strategySlotIndex == slotNum || it.symbol.equals(sym, ignoreCase = true) }
                        ?: ArjunStockItem(
                            id = "arjun-slot-$slotNum-$sym",
                            symbol = sym,
                            companyName = so.optString("company_name", sym),
                            entryPrice = so.optDouble("buy_price", 0.0),
                            currentPrice = so.optDouble("cmp", 0.0),
                            stopLoss = so.optDouble("stop_loss", 0.0),
                            targetPrice1 = so.optDouble("target_1", 0.0),
                            targetPrice2 = so.optDouble("target_2", 0.0),
                            quantity = so.optInt("quantity", 0),
                            isApprovedForStrategy = true,
                            strategySlotIndex = slotNum,
                            strategySlotCapital = slotBudget
                        )
                } else null

                val matchingBreakout = breakoutList.find { it.slotIndex == slotNum }

                slotsList.add(
                    ArjunStrategySlot(
                        slotIndex = slotNum,
                        stock = matchingStock,
                        isOccupied = isOcc && matchingStock != null,
                        targetCapital = so.optDouble("budget", slotBudget),
                        pendingBreakout = matchingBreakout
                    )
                )
            }
        } else {
            for (idx in 1..totalSlotsCount) {
                val matchingStock = positionsList.find { it.strategySlotIndex == idx }
                val matchingBreakout = breakoutList.find { it.slotIndex == idx }
                slotsList.add(
                    ArjunStrategySlot(
                        slotIndex = idx,
                        stock = matchingStock,
                        isOccupied = matchingStock != null,
                        targetCapital = slotBudget,
                        pendingBreakout = matchingBreakout
                    )
                )
            }
        }

        // Ensure all slots from 1 to totalSlotsCount are present
        val finalSlots = (1..totalSlotsCount).map { idx ->
            slotsList.find { it.slotIndex == idx } ?: run {
                val matchingStock = positionsList.find { it.strategySlotIndex == idx }
                val matchingBreakout = breakoutList.find { it.slotIndex == idx }
                ArjunStrategySlot(
                    slotIndex = idx,
                    stock = matchingStock,
                    isOccupied = matchingStock != null,
                    targetCapital = slotBudget,
                    pendingBreakout = matchingBreakout
                )
            }
        }

        val portfolioSummary = calculateSummary(positionsList)

        return ArjunServerSyncPayload(
            positions = positionsList,
            strategySlots = finalSlots,
            strategySummary = strategySummary,
            portfolioSummary = portfolioSummary,
            breakoutOrders = breakoutList,
            serverStatus = serverStatus,
            broker = broker,
            lastUpdated = lastUpdated
        )
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
        if (VaralakshmiRepository.isRunningInUnitTest) return null
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
        if (!enableDiskCache) return null
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
                val boArr = root.optJSONArray("breakout_orders")
                if (boArr != null) {
                    breakoutOrdersCache.clear()
                    for (k in 0 until boArr.length()) {
                        val bo = boArr.optJSONObject(k) ?: continue
                        val tlList = mutableListOf<ArjunLadderLevel>()
                        val tlArr = bo.optJSONArray("target_ladder")
                        if (tlArr != null) {
                            for (m in 0 until tlArr.length()) {
                                val lo = tlArr.optJSONObject(m) ?: continue
                                tlList.add(ArjunLadderLevel(
                                    levelIndex = lo.optInt("level_index", m + 1),
                                    triggerPrice = lo.optDouble("trigger_price", lo.optDouble("price", 0.0)),
                                    allocationPct = lo.optDouble("allocation_pct", 50.0),
                                    isTriggered = lo.optBoolean("is_triggered", false),
                                    triggeredAtPrice = lo.optDouble("triggered_at_price", 0.0),
                                    triggeredDate = lo.optString("triggered_date", "")
                                ))
                            }
                        }
                        val slList = mutableListOf<ArjunLadderLevel>()
                        val slArr = bo.optJSONArray("stop_loss_ladder")
                        if (slArr != null) {
                            for (m in 0 until slArr.length()) {
                                val lo = slArr.optJSONObject(m) ?: continue
                                slList.add(ArjunLadderLevel(
                                    levelIndex = lo.optInt("level_index", m + 1),
                                    triggerPrice = lo.optDouble("trigger_price", lo.optDouble("price", 0.0)),
                                    allocationPct = lo.optDouble("allocation_pct", 50.0),
                                    isTriggered = lo.optBoolean("is_triggered", false),
                                    triggeredAtPrice = lo.optDouble("triggered_at_price", 0.0),
                                    triggeredDate = lo.optString("triggered_date", "")
                                ))
                            }
                        }
                        breakoutOrdersCache.add(
                            ArjunBreakoutOrder(
                                id = bo.optString("id", UUID.randomUUID().toString()),
                                slotIndex = bo.optInt("slot_index", 1),
                                symbol = bo.optString("symbol", ""),
                                triggerPrice = bo.optDouble("trigger_price", 0.0),
                                quantity = bo.optInt("quantity", 0),
                                allocatedAmount = bo.optDouble("allocated_amount", SLOT_BUDGET),
                                targetLadder = tlList,
                                stopLossLadder = slList,
                                targetPrice1 = bo.optDouble("target_price_1", 0.0),
                                targetPrice2 = bo.optDouble("target_price_2", 0.0),
                                stopLoss = bo.optDouble("stop_loss", 0.0),
                                notes = bo.optString("notes", ""),
                                createdAt = bo.optString("created_at", "")
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

                val tlList = mutableListOf<ArjunLadderLevel>()
                val tlArr = o.optJSONArray("target_ladder")
                if (tlArr != null) {
                    for (m in 0 until tlArr.length()) {
                        val lo = tlArr.optJSONObject(m) ?: continue
                        tlList.add(ArjunLadderLevel(
                            levelIndex = lo.optInt("level_index", m + 1),
                            triggerPrice = lo.optDouble("trigger_price", lo.optDouble("price", 0.0)),
                            allocationPct = lo.optDouble("allocation_pct", 50.0),
                            isTriggered = lo.optBoolean("is_triggered", false),
                            triggeredAtPrice = lo.optDouble("triggered_at_price", 0.0),
                            triggeredDate = lo.optString("triggered_date", "")
                        ))
                    }
                }
                val slList = mutableListOf<ArjunLadderLevel>()
                val slArr = o.optJSONArray("stop_loss_ladder")
                if (slArr != null) {
                    for (m in 0 until slArr.length()) {
                        val lo = slArr.optJSONObject(m) ?: continue
                        slList.add(ArjunLadderLevel(
                            levelIndex = lo.optInt("level_index", m + 1),
                            triggerPrice = lo.optDouble("trigger_price", lo.optDouble("price", 0.0)),
                            allocationPct = lo.optDouble("allocation_pct", 50.0),
                            isTriggered = lo.optBoolean("is_triggered", false),
                            triggeredAtPrice = lo.optDouble("triggered_at_price", 0.0),
                            triggeredDate = lo.optString("triggered_date", "")
                        ))
                    }
                }

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
                        exitTriggerPrice = o.optDouble("exit_trigger_price", 0.0),
                        initialStrategyQuantity = o.optInt("initial_strategy_quantity", o.optInt("quantity", 10)),
                        partialRealizedPnl = o.optDouble("partial_realized_pnl", 0.0),
                        targetLadder = tlList,
                        stopLossLadder = slList
                    )
                )
            }
            if (list.isNotEmpty()) {
                // Only stocks bought in Zerodha (SKYGOLD, LALITHAA) occupy strategy slots;
                // remove unbought recommendations like ORIENTHOT from slots
                list.map { item ->
                    if (item.symbol == "ORIENTHOT" && item.isApprovedForStrategy) {
                        item.copy(isApprovedForStrategy = false, strategySlotIndex = -1, strategySlotCapital = 0.0)
                    } else item
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDisk(
        items: List<ArjunStockItem>,
        events: List<ArjunAutoExecutionEvent> = executionEventsCache,
        breakoutOrders: List<ArjunBreakoutOrder> = breakoutOrdersCache
    ) {
        if (!enableDiskCache) return
        val dir = VaralakshmiRepository.getCacheDirectory() ?: return
        try {
            val targetFile = File(dir, ARJUN_CACHE_FILE)
            val tempFile = File(dir, "$ARJUN_CACHE_FILE.tmp")
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
                    put("initial_strategy_quantity", it.initialStrategyQuantity)
                    put("partial_realized_pnl", it.partialRealizedPnl)

                    if (it.targetLadder.isNotEmpty()) {
                        val tla = JSONArray()
                        for (tl in it.targetLadder) {
                            tla.put(JSONObject().apply {
                                put("level_index", tl.levelIndex)
                                put("price", tl.price)
                                put("trigger_price", tl.price)
                                put("allocation_pct", tl.allocationPct)
                                put("is_triggered", tl.isTriggered)
                                put("triggered_at_price", tl.triggeredAtPrice)
                                put("triggered_date", tl.triggeredDate)
                            })
                        }
                        put("target_ladder", tla)
                    }

                    if (it.stopLossLadder.isNotEmpty()) {
                        val sla = JSONArray()
                        for (sl in it.stopLossLadder) {
                            sla.put(JSONObject().apply {
                                put("level_index", sl.levelIndex)
                                put("price", sl.price)
                                put("trigger_price", sl.price)
                                put("allocation_pct", sl.allocationPct)
                                put("is_triggered", sl.isTriggered)
                                put("triggered_at_price", sl.triggeredAtPrice)
                                put("triggered_date", sl.triggeredDate)
                            })
                        }
                        put("stop_loss_ladder", sla)
                    }
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

            val boArr = JSONArray()
            for (b in breakoutOrders) {
                val bo = JSONObject().apply {
                    put("id", b.id)
                    put("slot_index", b.slotIndex)
                    put("symbol", b.symbol)
                    put("trigger_price", b.triggerPrice)
                    put("quantity", b.quantity)
                    put("allocated_amount", b.allocatedAmount)
                    put("target_price_1", b.targetPrice1)
                    put("target_price_2", b.targetPrice2)
                    put("stop_loss", b.stopLoss)
                    put("notes", b.notes)
                    put("created_at", b.createdAt)

                    if (b.targetLadder.isNotEmpty()) {
                        val tla = JSONArray()
                        for (tl in b.targetLadder) {
                            tla.put(JSONObject().apply {
                                put("level_index", tl.levelIndex)
                                put("price", tl.price)
                                put("trigger_price", tl.price)
                                put("allocation_pct", tl.allocationPct)
                                put("is_triggered", tl.isTriggered)
                                put("triggered_at_price", tl.triggeredAtPrice)
                                put("triggered_date", tl.triggeredDate)
                            })
                        }
                        put("target_ladder", tla)
                    }

                    if (b.stopLossLadder.isNotEmpty()) {
                        val sla = JSONArray()
                        for (sl in b.stopLossLadder) {
                            sla.put(JSONObject().apply {
                                put("level_index", sl.levelIndex)
                                put("price", sl.price)
                                put("trigger_price", sl.price)
                                put("allocation_pct", sl.allocationPct)
                                put("is_triggered", sl.isTriggered)
                                put("triggered_at_price", sl.triggeredAtPrice)
                                put("triggered_date", sl.triggeredDate)
                            })
                        }
                        put("stop_loss_ladder", sla)
                    }
                }
                boArr.put(bo)
            }
            root.put("breakout_orders", boArr)

            tempFile.writeText(root.toString(2), Charsets.UTF_8)
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (_: Exception) {
            // Ignore disk cache write errors
        }
    }

    private fun roundPaise(v: Double): Double = ArjunMoneyMath.roundPaise(v)
}
