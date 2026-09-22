package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Typed result representing synchronization state with the live trading engine.
 */
sealed class SyncResult {
    abstract val summary: PortfolioSummary
    abstract val positions: List<PositionItem>
    abstract val transactions: List<TransactionItem>

    data class Success(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>
    ) : SyncResult()

    data class OfflineCacheFallback(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>,
        val message: String
    ) : SyncResult()
}

class VaralakshmiRepository {

    companion object {
        const val DEFAULT_SERVER_URL = "https://varalakshmi.ghostsoftwaresystems.com"
        const val DEFAULT_AUTH_TOKEN = "eyJlbWFpbCI6ImdhbmFwYXRoaXJhakBnbWFpbC5jb20iLCJleHAiOjIxMDUzNDA5NzgsIm5vbmNlIjoiYTFkYWE3NTlmMWU2ZmU1MjgxMDFlZTRmZDRjYTIzODQifQ.uEdogGmmZ7NeYDuwiWdv416rE7P5Im1s8CPByRwzgR0"
        const val CACHE_FILE_NAME = "portfolio_cache.json"

        @Volatile
        private var cacheDirectory: File? = null

        fun initialize(cacheDir: File) {
            cacheDirectory = cacheDir
        }

        fun getCacheDirectory(): File? = cacheDirectory

        fun resetCacheDirectoryForTesting() {
            cacheDirectory = null
        }

        fun roundPaise(value: Double): Double =
            if (value.isNaN() || value.isInfinite()) 0.0
            else BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).toDouble()

        fun optSafeDouble(obj: JSONObject, key: String, default: Double): Double {
            if (!obj.has(key) || obj.isNull(key)) return default
            val v = obj.optDouble(key, default)
            return if (v.isNaN() || v.isInfinite()) default else roundPaise(v)
        }

        fun createDefaultSummary() = PortfolioSummary(
            strategyId = "VARALAKSHMI_ALPHA_SCALE_35",
            strategyName = "VaraLakshmi Alpha (80%+ Fast Rotation Compounder)",
            formula = "AlphaZero_MCTS_Options_Gated_Agile_Exit(cut_loss=4%, trail_stop=8%, target=35%)",
            status = "ACTIVE",
            totalNav = 109268.80,
            allocatedCapital = 100000.00,
            deployedCapital = 99536.77,
            availableCapital = 463.23,
            realizedPnl = 0.00,
            unrealizedPnl = 9268.80,
            totalPnl = 9268.80,
            totalPnlPct = 9.2688,
            todayPnl = 4007.77,
            todayPnlPct = 3.81,
            activeSlots = 3,
            maxSlots = 3,
            lastUpdated = "2026-09-21 09:26:07"
        )

        fun createDefaultPositions() = listOf(
            PositionItem(
                positionId = "VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK",
                symbol = "STLNETWORK",
                quantity = 855,
                entryPrice = 40.20,
                currentPrice = 45.09,
                marketValue = 38551.95,
                unrealizedPnl = 4180.95,
                unrealizedPnlPct = 12.16,
                peakPrice = 45.09,
                entryDate = "2026-09-17 08:41:42",
                status = "OPEN",
                previousClose = 43.80,
                todayPriceChange = 1.29,
                todayPriceChangePct = 2.95,
                todayValueChange = 1102.95,
                isProfitTargetEnabled = true
            ),
            PositionItem(
                positionId = "VARALAKSHMI_ALPHA_SCALE_35_AHCL",
                symbol = "AHCL",
                quantity = 1472,
                entryPrice = 22.67,
                currentPrice = 24.91,
                marketValue = 36667.52,
                unrealizedPnl = 3301.75,
                unrealizedPnlPct = 9.90,
                peakPrice = 25.30,
                entryDate = "2026-09-18 09:23:45",
                status = "OPEN",
                previousClose = 24.15,
                todayPriceChange = 0.76,
                todayPriceChangePct = 3.15,
                todayValueChange = 1118.72,
                isProfitTargetEnabled = true
            ),
            PositionItem(
                positionId = "VARALAKSHMI_ALPHA_SCALE_35_TBZ",
                symbol = "TBZ",
                quantity = 53,
                entryPrice = 600.00,
                currentPrice = 633.70,
                marketValue = 33586.10,
                unrealizedPnl = 1786.10,
                unrealizedPnlPct = 5.62,
                peakPrice = 633.70,
                entryDate = "2026-09-21 09:26:07",
                status = "OPEN",
                previousClose = 600.00,
                todayPriceChange = 33.70,
                todayPriceChangePct = 5.62,
                todayValueChange = 1786.10,
                isProfitTargetEnabled = true
            )
        )

        fun createDefaultTransactions() = listOf(
            TransactionItem(
                transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_TBZ_20260919_BUY_53",
                timestamp = "2026-09-21 09:26:07",
                side = "BUY",
                symbol = "TBZ",
                quantity = 53,
                fillPrice = 600.00,
                grossAmount = 31800.00,
                fees = 20.0,
                realizedPnl = 0.0,
                pnlDifference = "+₹1,786.10 (+5.6%)"
            ),
            TransactionItem(
                transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_AHCL_20260918_BUY_1472",
                timestamp = "2026-09-18 09:23:45",
                side = "BUY",
                symbol = "AHCL",
                quantity = 1472,
                fillPrice = 22.67,
                grossAmount = 33365.77,
                fees = 20.0,
                realizedPnl = 0.0,
                pnlDifference = "+₹3,301.75 (+9.9%)"
            ),
            TransactionItem(
                transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_TBZ_20260918_BUY_58",
                timestamp = "2026-09-18 09:11:58",
                side = "BUY",
                symbol = "TBZ",
                quantity = 58,
                fillPrice = 574.85,
                grossAmount = 33341.30,
                fees = 20.0,
                realizedPnl = 0.0,
                pnlDifference = "Pre-fill Entry"
            ),
            TransactionItem(
                transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK_20260917_BUY_855",
                timestamp = "2026-09-17 08:41:42",
                side = "BUY",
                symbol = "STLNETWORK",
                quantity = 855,
                fillPrice = 40.20,
                grossAmount = 34371.00,
                fees = 20.0,
                realizedPnl = 0.0,
                pnlDifference = "+₹4,180.95 (+12.2%)"
            )
        )
    }

    private val lock = Any()

    // Default Seed Data: Aligned strictly to eliminate refresh drift
    // STL (cost 34,371.00) + AHCL (cost 33,365.77) + TBZ (cost 31,800.00) = 99,536.77
    // Allocated: 100,000.00 -> Available: 463.23
    // NAV = 99,536.77 + 463.23 + 9,268.80 = 109,268.80
    private var cachedSummary = createDefaultSummary()
    private val cachedPositions = createDefaultPositions().toMutableList()
    private val cachedTransactions = createDefaultTransactions().toMutableList()

    fun resetToDefaultSeed() = synchronized(lock) {
        cachedSummary = createDefaultSummary()
        cachedPositions.clear()
        cachedPositions.addAll(createDefaultPositions())
        cachedTransactions.clear()
        cachedTransactions.addAll(createDefaultTransactions())
    }

    private var isLoadedFromDisk = false
    private var cachedServerUrl = DEFAULT_SERVER_URL
    private var cachedAuthToken = DEFAULT_AUTH_TOKEN

    init {
        synchronized(lock) {
            if (loadFromDisk()) {
                isLoadedFromDisk = true
            }
        }
    }

    private fun ensureLoaded() {
        if (!isLoadedFromDisk && cacheDirectory != null) {
            if (loadFromDisk()) {
                isLoadedFromDisk = true
            }
        }
    }

    fun reloadCache(): Boolean = synchronized(lock) {
        val result = loadFromDisk()
        if (result) isLoadedFromDisk = true
        result
    }

    fun getServerUrl(): String = synchronized(lock) { ensureLoaded(); cachedServerUrl }
    fun getAuthToken(): String = synchronized(lock) { ensureLoaded(); cachedAuthToken }
    fun setServerConfig(url: String, token: String = cachedAuthToken) {
        synchronized(lock) {
            cachedServerUrl = url
            cachedAuthToken = token
            saveToDisk()
        }
    }

    fun getCachedSummary(): PortfolioSummary = synchronized(lock) { ensureLoaded(); cachedSummary }
    fun getCachedPositions(): List<PositionItem> = synchronized(lock) { ensureLoaded(); cachedPositions.toList() }
    fun getCachedTransactions(): List<TransactionItem> = synchronized(lock) { ensureLoaded(); cachedTransactions.toList() }

    suspend fun refreshData(serverBaseUrl: String, authToken: String = DEFAULT_AUTH_TOKEN): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = serverBaseUrl.trimEnd('/')

        var fetchedPositions: List<PositionItem>? = null
        var fetchedTransactions: List<TransactionItem>? = null
        var networkError: String? = null

        val tokenQuery = if (authToken.isNotBlank()) "&token=$authToken" else ""

        try {
            // 1. Fetch positions
            val positionsUrl = "$cleanUrl/api/live-trading/positions?strategy=VARALAKSHMI_ALPHA_SCALE_35$tokenQuery"
            val positionsJson = httpGet(positionsUrl, authToken)
            if (positionsJson != null) {
                val root = JSONObject(positionsJson)
                val activeArray = root.optJSONArray("active")
                if (activeArray != null) {
                    val parsedPositions = mutableListOf<PositionItem>()
                    for (i in 0 until activeArray.length()) {
                        val obj = activeArray.optJSONObject(i) ?: continue
                        val symbol = obj.optString("symbol", "")
                        val quantity = obj.optInt("quantity", 0)
                        val entryPrice = obj.optDouble("entry_price", 0.0)
                        val currentPrice = obj.optDouble("current_price", 0.0)
                        val marketValue = if (obj.has("market_value") && !obj.isNull("market_value")) {
                            roundPaise(obj.optDouble("market_value", 0.0))
                        } else roundPaise(quantity * currentPrice)
                        val unrealizedPnl = if (obj.has("unrealized_pnl") && !obj.isNull("unrealized_pnl")) {
                            roundPaise(obj.optDouble("unrealized_pnl", 0.0))
                        } else roundPaise(marketValue - (quantity * entryPrice))
                        val unrealizedPnlPct = if (obj.has("unrealized_pnl_pct") && !obj.isNull("unrealized_pnl_pct")) {
                            roundPaise(obj.optDouble("unrealized_pnl_pct", 0.0))
                        } else {
                            if (entryPrice > 0.0) roundPaise(((currentPrice - entryPrice) / entryPrice) * 100.0) else 0.0
                        }
                        val peakPrice = if (obj.has("peak_price") && !obj.isNull("peak_price")) {
                            obj.optDouble("peak_price", currentPrice)
                        } else currentPrice
                        val entryDate = obj.optString("entry_date", "")
                        val status = obj.optString("status", "OPEN")

                        // Look up previous close from JSON, or fall back to cached position, or entryPrice
                        val jsonPrevClose = if (obj.has("previous_close") && !obj.isNull("previous_close")) {
                            obj.optDouble("previous_close", 0.0)
                        } else if (obj.has("prev_close") && !obj.isNull("prev_close")) {
                            obj.optDouble("prev_close", 0.0)
                        } else 0.0

                        val cachedPrevClose = synchronized(lock) {
                            cachedPositions.find { it.symbol == symbol }?.previousClose
                        } ?: 0.0
                        val effectivePrevClose = if (jsonPrevClose > 0.0) {
                            jsonPrevClose
                        } else if (cachedPrevClose > 0.0) {
                            cachedPrevClose
                        } else {
                            entryPrice
                        }

                        val refPrice = if (effectivePrevClose > 0.0) effectivePrevClose else entryPrice

                        val todayChg = if (obj.has("today_price_change") && !obj.isNull("today_price_change")) {
                            roundPaise(obj.optDouble("today_price_change", 0.0))
                        } else if (obj.has("change") && !obj.isNull("change")) {
                            roundPaise(obj.optDouble("change", 0.0))
                        } else {
                            roundPaise(currentPrice - refPrice)
                        }

                        val todayChgPct = if (obj.has("today_price_change_pct") && !obj.isNull("today_price_change_pct")) {
                            roundPaise(obj.optDouble("today_price_change_pct", 0.0))
                        } else if (obj.has("change_pct") && !obj.isNull("change_pct")) {
                            roundPaise(obj.optDouble("change_pct", 0.0))
                        } else {
                            if (refPrice > 0.0) roundPaise(((currentPrice - refPrice) / refPrice) * 100.0) else 0.0
                        }

                        val calculatedValChg = roundPaise(quantity * todayChg)
                        val todayValChg = if (obj.has("today_value_change") && !obj.isNull("today_value_change") && obj.optDouble("today_value_change", 0.0) != 0.0) {
                            roundPaise(obj.optDouble("today_value_change", 0.0))
                        } else {
                            calculatedValChg
                        }

                        val targetEnabled = if (obj.has("target_enabled") && !obj.isNull("target_enabled")) {
                            obj.optBoolean("target_enabled", true)
                        } else if (obj.has("is_profit_target_enabled") && !obj.isNull("is_profit_target_enabled")) {
                            obj.optBoolean("is_profit_target_enabled", true)
                        } else if (obj.has("profit_target_override")) {
                            if (obj.isNull("profit_target_override")) {
                                false
                            } else {
                                val override = obj.optDouble("profit_target_override", 0.35)
                                override > 0.0
                            }
                        } else {
                            val cached = synchronized(lock) { cachedPositions.find { it.symbol == symbol } }
                            cached?.isProfitTargetEnabled ?: true
                        }

                        parsedPositions.add(
                            PositionItem(
                                positionId = obj.optString("position_id", ""),
                                symbol = symbol,
                                quantity = quantity,
                                entryPrice = entryPrice,
                                currentPrice = currentPrice,
                                marketValue = marketValue,
                                unrealizedPnl = unrealizedPnl,
                                unrealizedPnlPct = unrealizedPnlPct,
                                peakPrice = peakPrice,
                                entryDate = entryDate,
                                status = status,
                                previousClose = effectivePrevClose,
                                todayPriceChange = todayChg,
                                todayPriceChangePct = todayChgPct,
                                todayValueChange = todayValChg,
                                isProfitTargetEnabled = targetEnabled
                            )
                        )
                    }
                    fetchedPositions = parsedPositions
                }
            } else {
                networkError = "Positions endpoint returned empty response"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            networkError = e.message ?: "Failed to fetch positions"
        }

        try {
            // 2. Fetch transactions
            val txUrl = "$cleanUrl/api/live-trading/transactions?strategy=VARALAKSHMI_ALPHA_SCALE_35&limit=25$tokenQuery"
            val txJson = httpGet(txUrl, authToken)
            if (txJson != null) {
                val root = JSONObject(txJson)
                val txArray = root.optJSONArray("transactions")
                if (txArray != null) {
                    val parsedTx = mutableListOf<TransactionItem>()
                    val currentPosList = fetchedPositions ?: synchronized(lock) { cachedPositions.toList() }

                    for (i in 0 until txArray.length()) {
                        val obj = txArray.optJSONObject(i) ?: continue
                        val realized = obj.optDouble("realized_pnl", 0.0)
                        val side = obj.optString("side", "BUY")
                        val symbol = obj.optString("symbol", "")

                        val matchedPos = currentPosList.find { it.symbol == symbol }
                        val diffStr = if (side == "SELL" || realized != 0.0) {
                            (if (realized >= 0) "+₹" else "-₹") + String.format(Locale.US, "%.2f", kotlin.math.abs(realized))
                        } else if (matchedPos != null) {
                            (if (matchedPos.unrealizedPnl >= 0) "+₹" else "-₹") +
                                    String.format(Locale.US, "%.2f (%+.1f%%)", kotlin.math.abs(matchedPos.unrealizedPnl), matchedPos.unrealizedPnlPct)
                        } else {
                            "₹0.00"
                        }

                        parsedTx.add(
                            TransactionItem(
                                transactionId = obj.optString("transaction_id", ""),
                                timestamp = obj.optString("timestamp", ""),
                                side = side,
                                symbol = symbol,
                                quantity = obj.optInt("quantity", 0),
                                fillPrice = obj.optDouble("fill_price", 0.0),
                                grossAmount = obj.optDouble("gross_amount", 0.0),
                                fees = obj.optDouble("fees", 20.0),
                                realizedPnl = realized,
                                pnlDifference = diffStr
                            )
                        )
                    }
                    fetchedTransactions = parsedTx
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep previous transactions if this fails
        }

        var fetchedAllocatedCapital: Double? = null
        var fetchedAvailableCapital: Double? = null

        try {
            // 3. Fetch strategies to sync dynamic capital allocation
            val strategiesUrl = "$cleanUrl/api/live-trading/strategies$tokenQuery"
            val stratJson = httpGet(strategiesUrl, authToken)
            if (stratJson != null) {
                val root = JSONObject(stratJson)
                val stratArray = root.optJSONArray("strategies")
                if (stratArray != null) {
                    for (i in 0 until stratArray.length()) {
                        val obj = stratArray.optJSONObject(i) ?: continue
                        if (obj.optString("strategy_id") == "VARALAKSHMI_ALPHA_SCALE_35") {
                            val alloc = obj.optDouble("allocated_capital", 0.0)
                            val avail = obj.optDouble("available_capital", 0.0)
                            if (alloc > 0.0) {
                                fetchedAllocatedCapital = roundPaise(alloc)
                            }
                            if (obj.has("available_capital") && !obj.isNull("available_capital")) {
                                fetchedAvailableCapital = roundPaise(avail)
                            }
                            break
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Non-fatal, fallback to existing or calculated allocation
        }

        synchronized(lock) {
            if (fetchedPositions != null) {
                cachedPositions.clear()
                cachedPositions.addAll(fetchedPositions)
                if (fetchedTransactions != null) {
                    cachedTransactions.clear()
                    cachedTransactions.addAll(fetchedTransactions)
                }

                // Recalculate summary metrics from updated live positions and dynamic capital allocation
                val totalMarketValue = roundPaise(cachedPositions.sumOf { it.marketValue })
                val totalUnrealized = roundPaise(cachedPositions.sumOf { it.unrealizedPnl })
                val deployed = roundPaise(totalMarketValue - totalUnrealized)
                val allocated = fetchedAllocatedCapital ?: cachedSummary.allocatedCapital
                val realized = cachedSummary.realizedPnl
                val available = fetchedAvailableCapital ?: roundPaise((allocated - deployed + realized).coerceAtLeast(0.0))
                val nav = roundPaise(deployed + available + totalUnrealized)
                val totalPnl = roundPaise(realized + totalUnrealized)
                val totalPnlPct = if (allocated > 0) (totalPnl / allocated) * 100.0 else 0.0

                val todayPnl = roundPaise(cachedPositions.sumOf { it.todayValueChange })
                val prevNav = nav - todayPnl
                val todayPnlPct = if (prevNav > 0.0) roundPaise((todayPnl / prevNav) * 100.0) else if (allocated > 0.0) roundPaise((todayPnl / allocated) * 100.0) else 0.0
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                cachedSummary = cachedSummary.copy(
                    totalNav = nav,
                    allocatedCapital = allocated,
                    deployedCapital = deployed,
                    availableCapital = available,
                    realizedPnl = realized,
                    unrealizedPnl = totalUnrealized,
                    totalPnl = totalPnl,
                    totalPnlPct = totalPnlPct,
                    todayPnl = todayPnl,
                    todayPnlPct = todayPnlPct,
                    activeSlots = cachedPositions.size,
                    lastUpdated = nowStr
                )
                saveToDisk()

                SyncResult.Success(cachedSummary, cachedPositions.toList(), cachedTransactions.toList())
            } else {
                // Offline fallback - preserve existing cache and timestamp
                SyncResult.OfflineCacheFallback(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    message = networkError ?: "Offline mode: server unreachable"
                )
            }
        }
    }

    /**
     * Removes an active position, credits proceeds to available cash, adjusts deployed capital,
     * records realized PnL, appends a SELL transaction, and strictly preserves the NAV invariant.
     */
    fun removePosition(symbol: String): Triple<PortfolioSummary, List<PositionItem>, List<TransactionItem>> =
        synchronized(lock) {
            val targetIndex = cachedPositions.indexOfFirst { it.symbol == symbol }
            if (targetIndex != -1) {
                val target = cachedPositions.removeAt(targetIndex)

                val proceeds = roundPaise(target.marketValue)
                val realizedGain = roundPaise(target.unrealizedPnl)
                val costBasis = roundPaise(target.marketValue - target.unrealizedPnl)

                val newRealizedPnl = roundPaise(cachedSummary.realizedPnl + realizedGain)
                val newAvailableCapital = roundPaise(cachedSummary.availableCapital + proceeds)
                val newDeployedCapital = roundPaise((cachedSummary.deployedCapital - costBasis).coerceAtLeast(0.0))

                val totalUnrealized = roundPaise(cachedPositions.sumOf { it.unrealizedPnl })
                // Invariant: NAV = Deployed + Available + TotalUnrealized
                val nav = roundPaise(newDeployedCapital + newAvailableCapital + totalUnrealized)
                val allocated = cachedSummary.allocatedCapital
                val totalPnl = roundPaise(newRealizedPnl + totalUnrealized)
                val totalPnlPct = if (allocated > 0) (totalPnl / allocated) * 100.0 else 0.0

                val totalTodayPnl = roundPaise(cachedPositions.sumOf { it.todayValueChange })
                val prevNav = nav - totalTodayPnl
                val todayPnlPct = if (prevNav > 0.0) roundPaise((totalTodayPnl / prevNav) * 100.0) else if (allocated > 0.0) roundPaise((totalTodayPnl / allocated) * 100.0) else 0.0

                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                cachedSummary = cachedSummary.copy(
                    totalNav = nav,
                    deployedCapital = newDeployedCapital,
                    availableCapital = newAvailableCapital,
                    realizedPnl = newRealizedPnl,
                    unrealizedPnl = totalUnrealized,
                    totalPnl = totalPnl,
                    totalPnlPct = totalPnlPct,
                    todayPnl = totalTodayPnl,
                    todayPnlPct = todayPnlPct,
                    activeSlots = cachedPositions.size,
                    lastUpdated = nowStr
                )

                // Append SELL TransactionItem to transaction ledger
                val diffPrefix = if (realizedGain >= 0) "+₹" else "-₹"
                val diffStr = String.format(Locale.US, "%s%,.2f (%+.1f%%)", diffPrefix, kotlin.math.abs(realizedGain), target.unrealizedPnlPct)
                val dateCompact = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val exitTx = TransactionItem(
                    transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_${symbol}_${dateCompact}_SELL_${target.quantity}",
                    timestamp = nowStr,
                    side = "SELL",
                    symbol = target.symbol,
                    quantity = target.quantity,
                    fillPrice = target.currentPrice,
                    grossAmount = target.marketValue,
                    fees = 20.0,
                    realizedPnl = realizedGain,
                    pnlDifference = diffStr
                )
                cachedTransactions.add(0, exitTx)
                saveToDisk()
            }

            Triple(cachedSummary, cachedPositions.toList(), cachedTransactions.toList())
        }

    fun updateProfitTarget(symbol: String, enabled: Boolean): List<PositionItem> = synchronized(lock) {
        ensureLoaded()
        val index = cachedPositions.indexOfFirst { it.symbol.equals(symbol, ignoreCase = true) }
        if (index != -1) {
            val old = cachedPositions[index]
            cachedPositions[index] = old.copy(isProfitTargetEnabled = enabled)
            saveToDisk()
        }
        cachedPositions.toList()
    }

    suspend fun syncProfitTargetToBackend(
        strategyId: String,
        symbol: String,
        enabled: Boolean,
        serverBaseUrl: String = cachedServerUrl,
        authToken: String = cachedAuthToken
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverBaseUrl.trimEnd('/')
            val tokenQuery = if (authToken.isNotBlank()) "?token=$authToken" else ""
            val endpoint = "$cleanUrl/api/live-trading/positions/toggle-target$tokenQuery"
            val payload = JSONObject().apply {
                put("strategy_id", strategyId)
                put("symbol", symbol)
                put("target_enabled", enabled)
                put("target_rate", if (enabled) 0.35 else JSONObject.NULL)
            }
            val res = httpPost(endpoint, payload.toString(), authToken)
            if (res != null) {
                try {
                    val json = JSONObject(res)
                    json.optBoolean("success", true)
                } catch (e: Exception) {
                    true
                }
            } else false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private fun httpGet(urlStr: String, authToken: String? = null, timeoutMs: Int = 5000): String? {
        var conn: HttpURLConnection? = null
        return try {
            val uri = URI(urlStr)
            val scheme = uri.scheme?.lowercase(Locale.US)
            if (scheme != "http" && scheme != "https") {
                return null
            }
            conn = uri.toURL().openConnection() as? HttpURLConnection ?: return null
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "VaralakshmiPortfolio/1.0")
            if (!authToken.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $authToken")
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPost(urlStr: String, jsonBody: String, authToken: String? = null, timeoutMs: Int = 5000): String? {
        var conn: HttpURLConnection? = null
        return try {
            val uri = URI(urlStr)
            val scheme = uri.scheme?.lowercase(Locale.US)
            if (scheme != "http" && scheme != "https") {
                return null
            }
            conn = uri.toURL().openConnection() as? HttpURLConnection ?: return null
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "VaralakshmiPortfolio/1.0")
            if (!authToken.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $authToken")
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun saveToDisk() {
        val dir = cacheDirectory ?: return
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val root = JSONObject()
            root.put("serverUrl", cachedServerUrl)
            root.put("authToken", cachedAuthToken)
            val summaryObj = JSONObject().apply {
                put("strategyId", cachedSummary.strategyId)
                put("strategyName", cachedSummary.strategyName)
                put("formula", cachedSummary.formula)
                put("status", cachedSummary.status)
                put("totalNav", cachedSummary.totalNav)
                put("allocatedCapital", cachedSummary.allocatedCapital)
                put("deployedCapital", cachedSummary.deployedCapital)
                put("availableCapital", cachedSummary.availableCapital)
                put("realizedPnl", cachedSummary.realizedPnl)
                put("unrealizedPnl", cachedSummary.unrealizedPnl)
                put("totalPnl", cachedSummary.totalPnl)
                put("totalPnlPct", cachedSummary.totalPnlPct)
                put("todayPnl", cachedSummary.todayPnl)
                put("todayPnlPct", cachedSummary.todayPnlPct)
                put("activeSlots", cachedSummary.activeSlots)
                put("maxSlots", cachedSummary.maxSlots)
                put("lastUpdated", cachedSummary.lastUpdated)
            }
            root.put("summary", summaryObj)
            root.put("serverUrl", cachedServerUrl)
            root.put("authToken", cachedAuthToken)

            val posArray = JSONArray()
            for (p in cachedPositions) {
                val pObj = JSONObject().apply {
                    put("positionId", p.positionId)
                    put("symbol", p.symbol)
                    put("quantity", p.quantity)
                    put("entryPrice", p.entryPrice)
                    put("currentPrice", p.currentPrice)
                    put("marketValue", p.marketValue)
                    put("unrealizedPnl", p.unrealizedPnl)
                    put("unrealizedPnlPct", p.unrealizedPnlPct)
                    put("peakPrice", p.peakPrice)
                    put("entryDate", p.entryDate)
                    put("status", p.status)
                    put("previousClose", p.previousClose)
                    put("todayPriceChange", p.todayPriceChange)
                    put("todayPriceChangePct", p.todayPriceChangePct)
                    put("todayValueChange", p.todayValueChange)
                    put("isProfitTargetEnabled", p.isProfitTargetEnabled)
                }
                posArray.put(pObj)
            }
            root.put("positions", posArray)

            val txArray = JSONArray()
            for (tx in cachedTransactions) {
                val txObj = JSONObject().apply {
                    put("transactionId", tx.transactionId)
                    put("timestamp", tx.timestamp)
                    put("side", tx.side)
                    put("symbol", tx.symbol)
                    put("quantity", tx.quantity)
                    put("fillPrice", tx.fillPrice)
                    put("grossAmount", tx.grossAmount)
                    put("fees", tx.fees)
                    put("realizedPnl", tx.realizedPnl)
                    put("pnlDifference", tx.pnlDifference)
                }
                txArray.put(txObj)
            }
            root.put("transactions", txArray)

            val targetFile = File(dir, CACHE_FILE_NAME)
            val tempFile = File(dir, "$CACHE_FILE_NAME.tmp")
            tempFile.writeText(root.toString(2), Charsets.UTF_8)
            if (tempFile.renameTo(targetFile).not()) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            // Disk caching failure must not interrupt normal operations
        }
    }

    private fun loadFromDisk(): Boolean {
        val dir = cacheDirectory ?: return false
        val cacheFile = File(dir, CACHE_FILE_NAME)
        if (!cacheFile.exists() || !cacheFile.isFile) return false

        return try {
            val jsonStr = cacheFile.readText(Charsets.UTF_8)
            if (jsonStr.isBlank()) {
                cacheFile.delete()
                return false
            }
            val root = JSONObject(jsonStr)

            val summaryObj = root.optJSONObject("summary")
            val parsedSummary = if (summaryObj != null) {
                PortfolioSummary(
                    strategyId = summaryObj.optString("strategyId", cachedSummary.strategyId),
                    strategyName = summaryObj.optString("strategyName", cachedSummary.strategyName),
                    formula = summaryObj.optString("formula", cachedSummary.formula),
                    status = summaryObj.optString("status", cachedSummary.status),
                    totalNav = optSafeDouble(summaryObj, "totalNav", cachedSummary.totalNav),
                    allocatedCapital = optSafeDouble(summaryObj, "allocatedCapital", cachedSummary.allocatedCapital),
                    deployedCapital = optSafeDouble(summaryObj, "deployedCapital", cachedSummary.deployedCapital),
                    availableCapital = optSafeDouble(summaryObj, "availableCapital", cachedSummary.availableCapital),
                    realizedPnl = optSafeDouble(summaryObj, "realizedPnl", cachedSummary.realizedPnl),
                    unrealizedPnl = optSafeDouble(summaryObj, "unrealizedPnl", cachedSummary.unrealizedPnl),
                    totalPnl = optSafeDouble(summaryObj, "totalPnl", cachedSummary.totalPnl),
                    totalPnlPct = optSafeDouble(summaryObj, "totalPnlPct", cachedSummary.totalPnlPct),
                    todayPnl = optSafeDouble(summaryObj, "todayPnl", cachedSummary.todayPnl),
                    todayPnlPct = optSafeDouble(summaryObj, "todayPnlPct", cachedSummary.todayPnlPct),
                    activeSlots = summaryObj.optInt("activeSlots", cachedSummary.activeSlots),
                    maxSlots = summaryObj.optInt("maxSlots", cachedSummary.maxSlots),
                    lastUpdated = summaryObj.optString("lastUpdated", cachedSummary.lastUpdated)
                )
            } else null

            val posArray = root.optJSONArray("positions")
            val parsedPositions = if (posArray != null) {
                val list = mutableListOf<PositionItem>()
                for (i in 0 until posArray.length()) {
                    val p = posArray.optJSONObject(i) ?: continue
                    val symbol = p.optString("symbol", "")
                    val quantity = p.optInt("quantity", 0)
                    val entryPrice = optSafeDouble(p, "entryPrice", if (p.has("entry_price")) optSafeDouble(p, "entry_price", 0.0) else 0.0)
                    val currentPrice = optSafeDouble(p, "currentPrice", if (p.has("current_price")) optSafeDouble(p, "current_price", 0.0) else 0.0)
                    val prevClose = if (p.has("previousClose") && !p.isNull("previousClose")) {
                        optSafeDouble(p, "previousClose", 0.0)
                    } else if (p.has("previous_close") && !p.isNull("previous_close")) {
                        optSafeDouble(p, "previous_close", 0.0)
                    } else 0.0
                    val effectivePrevClose = if (prevClose > 0.0) prevClose else entryPrice
                    val refPrice = if (effectivePrevClose > 0.0) effectivePrevClose else entryPrice

                    val todayChg = if (p.has("todayPriceChange") && !p.isNull("todayPriceChange")) {
                        optSafeDouble(p, "todayPriceChange", 0.0)
                    } else if (p.has("today_price_change") && !p.isNull("today_price_change")) {
                        optSafeDouble(p, "today_price_change", 0.0)
                    } else {
                        roundPaise(currentPrice - refPrice)
                    }

                    val todayChgPct = if (p.has("todayPriceChangePct") && !p.isNull("todayPriceChangePct")) {
                        optSafeDouble(p, "todayPriceChangePct", 0.0)
                    } else if (p.has("today_price_change_pct") && !p.isNull("today_price_change_pct")) {
                        optSafeDouble(p, "today_price_change_pct", 0.0)
                    } else {
                        if (refPrice > 0.0) roundPaise(((currentPrice - refPrice) / refPrice) * 100.0) else 0.0
                    }

                    val calculatedValChg = roundPaise(quantity * todayChg)
                    val todayValChg = if (p.has("todayValueChange") && !p.isNull("todayValueChange") && p.optDouble("todayValueChange", 0.0) != 0.0) {
                        optSafeDouble(p, "todayValueChange", calculatedValChg)
                    } else if (p.has("today_value_change") && !p.isNull("today_value_change") && p.optDouble("today_value_change", 0.0) != 0.0) {
                        optSafeDouble(p, "today_value_change", calculatedValChg)
                    } else {
                        calculatedValChg
                    }

                    val marketValue = if (p.has("marketValue") && !p.isNull("marketValue")) {
                        optSafeDouble(p, "marketValue", roundPaise(quantity * currentPrice))
                    } else if (p.has("market_value") && !p.isNull("market_value")) {
                        optSafeDouble(p, "market_value", roundPaise(quantity * currentPrice))
                    } else roundPaise(quantity * currentPrice)

                    val unrealizedPnl = if (p.has("unrealizedPnl") && !p.isNull("unrealizedPnl")) {
                        optSafeDouble(p, "unrealizedPnl", roundPaise(marketValue - (quantity * entryPrice)))
                    } else if (p.has("unrealized_pnl") && !p.isNull("unrealized_pnl")) {
                        optSafeDouble(p, "unrealized_pnl", roundPaise(marketValue - (quantity * entryPrice)))
                    } else roundPaise(marketValue - (quantity * entryPrice))

                    val unrealizedPnlPct = if (p.has("unrealizedPnlPct") && !p.isNull("unrealizedPnlPct")) {
                        optSafeDouble(p, "unrealizedPnlPct", 0.0)
                    } else if (p.has("unrealized_pnl_pct") && !p.isNull("unrealized_pnl_pct")) {
                        optSafeDouble(p, "unrealized_pnl_pct", 0.0)
                    } else {
                        if (entryPrice > 0.0) roundPaise(((currentPrice - entryPrice) / entryPrice) * 100.0) else 0.0
                    }

                    val peakPrice = if (p.has("peakPrice") && !p.isNull("peakPrice")) {
                        optSafeDouble(p, "peakPrice", currentPrice)
                    } else if (p.has("peak_price") && !p.isNull("peak_price")) {
                        optSafeDouble(p, "peak_price", currentPrice)
                    } else currentPrice

                    val isProfitTargetEnabled = if (p.has("isProfitTargetEnabled") && !p.isNull("isProfitTargetEnabled")) {
                        p.optBoolean("isProfitTargetEnabled", true)
                    } else if (p.has("is_profit_target_enabled") && !p.isNull("is_profit_target_enabled")) {
                        p.optBoolean("is_profit_target_enabled", true)
                    } else if (p.has("target_enabled") && !p.isNull("target_enabled")) {
                        p.optBoolean("target_enabled", true)
                    } else if (p.has("profit_target_override")) {
                        if (p.isNull("profit_target_override")) {
                            false
                        } else {
                            val override = p.optDouble("profit_target_override", 0.35)
                            override > 0.0
                        }
                    } else {
                        true
                    }

                    list.add(
                        PositionItem(
                            positionId = p.optString("positionId", if (p.has("position_id")) p.optString("position_id", "") else ""),
                            symbol = symbol,
                            quantity = quantity,
                            entryPrice = entryPrice,
                            currentPrice = currentPrice,
                            marketValue = marketValue,
                            unrealizedPnl = unrealizedPnl,
                            unrealizedPnlPct = unrealizedPnlPct,
                            peakPrice = peakPrice,
                            entryDate = p.optString("entryDate", p.optString("entry_date", "")),
                            status = p.optString("status", "OPEN"),
                            previousClose = effectivePrevClose,
                            todayPriceChange = todayChg,
                            todayPriceChangePct = todayChgPct,
                            todayValueChange = todayValChg,
                            isProfitTargetEnabled = isProfitTargetEnabled
                        )
                    )
                }
                list
            } else null

            val txArray = root.optJSONArray("transactions")
            val parsedTransactions = if (txArray != null) {
                val list = mutableListOf<TransactionItem>()
                for (i in 0 until txArray.length()) {
                    val tx = txArray.optJSONObject(i) ?: continue
                    val side = tx.optString("side", "BUY")
                    val realized = optSafeDouble(tx, "realizedPnl", if (tx.has("realized_pnl")) optSafeDouble(tx, "realized_pnl", 0.0) else 0.0)
                    val pnlDiff = tx.optString("pnlDifference", tx.optString("pnl_difference", ""))

                    list.add(
                        TransactionItem(
                            transactionId = tx.optString("transactionId", tx.optString("transaction_id", "")),
                            timestamp = tx.optString("timestamp", ""),
                            side = side,
                            symbol = tx.optString("symbol", ""),
                            quantity = tx.optInt("quantity", 0),
                            fillPrice = optSafeDouble(tx, "fillPrice", if (tx.has("fill_price")) optSafeDouble(tx, "fill_price", 0.0) else 0.0),
                            grossAmount = optSafeDouble(tx, "grossAmount", if (tx.has("gross_amount")) optSafeDouble(tx, "gross_amount", 0.0) else 0.0),
                            fees = optSafeDouble(tx, "fees", 20.0),
                            realizedPnl = realized,
                            pnlDifference = pnlDiff
                        )
                    )
                }
                list
            } else null

            if (root.has("serverUrl") && !root.isNull("serverUrl")) {
                val url = root.optString("serverUrl", "").trim()
                if (url.isNotBlank()) cachedServerUrl = url
            }
            if (root.has("authToken") && !root.isNull("authToken")) {
                val token = root.optString("authToken", "").trim()
                if (token.isNotBlank()) cachedAuthToken = token
            }

            // Transactional commit to memory: all-or-nothing
            if (parsedPositions != null) {
                cachedPositions.clear()
                cachedPositions.addAll(parsedPositions)
            }
            if (parsedSummary != null) {
                // Ensure todayPnl strictly matches parsedPositions to prevent calculation drift
                val effectiveTodayPnl = if (parsedPositions != null) {
                    if (summaryObj != null && summaryObj.has("todayPnl") && !summaryObj.isNull("todayPnl")) {
                        optSafeDouble(summaryObj, "todayPnl", roundPaise(parsedPositions.sumOf { it.todayValueChange }))
                    } else {
                        roundPaise(parsedPositions.sumOf { it.todayValueChange })
                    }
                } else {
                    parsedSummary.todayPnl
                }
                val prevNav = parsedSummary.totalNav - effectiveTodayPnl
                val effectiveTodayPnlPct = if (prevNav > 0.0) roundPaise((effectiveTodayPnl / prevNav) * 100.0)
                else if (parsedSummary.allocatedCapital > 0.0) roundPaise((effectiveTodayPnl / parsedSummary.allocatedCapital) * 100.0)
                else 0.0

                cachedSummary = parsedSummary.copy(
                    todayPnl = effectiveTodayPnl,
                    todayPnlPct = if (summaryObj != null && summaryObj.has("todayPnlPct") && !summaryObj.isNull("todayPnlPct")) parsedSummary.todayPnlPct else effectiveTodayPnlPct
                )
            }
            if (parsedTransactions != null) {
                cachedTransactions.clear()
                cachedTransactions.addAll(parsedTransactions)
            }
            isLoadedFromDisk = true
            true
        } catch (e: Exception) {
            // Corrupt file recovery: quarantine or delete corrupt file so app continues safely
            try {
                val corruptBackup = File(dir, "$CACHE_FILE_NAME.corrupt")
                if (cacheFile.exists()) {
                    if (corruptBackup.exists()) corruptBackup.delete()
                    cacheFile.renameTo(corruptBackup)
                }
            } catch (_: Exception) {
                try { cacheFile.delete() } catch (_: Exception) {}
            }
            false
        }
    }

    fun clearDiskCache() {
        synchronized(lock) {
            val dir = cacheDirectory ?: return
            val cacheFile = File(dir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            val corruptFile = File(dir, "$CACHE_FILE_NAME.corrupt")
            if (corruptFile.exists()) {
                corruptFile.delete()
            }
            val tmpFile = File(dir, "$CACHE_FILE_NAME.tmp")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            isLoadedFromDisk = false
        }
    }
}
