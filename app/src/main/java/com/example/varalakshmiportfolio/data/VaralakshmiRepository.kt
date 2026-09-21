package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

        fun roundPaise(value: Double): Double =
            BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN).toDouble()
    }

    private val lock = Any()

    // Default Seed Data: Aligned strictly to eliminate refresh drift
    // STL (cost 34,371.00) + AHCL (cost 33,365.77) + TBZ (cost 31,800.00) = 99,536.77
    // Allocated: 100,000.00 -> Available: 463.23
    // NAV = 99,536.77 + 463.23 + 9,268.80 = 109,268.80
    private var cachedSummary = PortfolioSummary(
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
        activeSlots = 3,
        maxSlots = 3,
        lastUpdated = "2026-09-21 09:26:07"
    )

    private val cachedPositions = mutableListOf(
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
            status = "OPEN"
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
            status = "OPEN"
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
            status = "OPEN"
        )
    )

    private val cachedTransactions = mutableListOf(
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

    fun getCachedSummary(): PortfolioSummary = synchronized(lock) { cachedSummary }
    fun getCachedPositions(): List<PositionItem> = synchronized(lock) { cachedPositions.toList() }
    fun getCachedTransactions(): List<TransactionItem> = synchronized(lock) { cachedTransactions.toList() }

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
                        parsedPositions.add(
                            PositionItem(
                                positionId = obj.optString("position_id", ""),
                                symbol = obj.optString("symbol", ""),
                                quantity = obj.optInt("quantity", 0),
                                entryPrice = obj.optDouble("entry_price", 0.0),
                                currentPrice = obj.optDouble("current_price", 0.0),
                                marketValue = obj.optDouble("market_value", 0.0),
                                unrealizedPnl = obj.optDouble("unrealized_pnl", 0.0),
                                unrealizedPnlPct = obj.optDouble("unrealized_pnl_pct", 0.0),
                                peakPrice = obj.optDouble("peak_price", 0.0),
                                entryDate = obj.optString("entry_date", ""),
                                status = obj.optString("status", "OPEN")
                            )
                        )
                    }
                    if (parsedPositions.isNotEmpty()) {
                        fetchedPositions = parsedPositions
                    }
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
                                    String.format(Locale.US, "%.2f (+%.1f%%)", kotlin.math.abs(matchedPos.unrealizedPnl), matchedPos.unrealizedPnlPct)
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
                    if (parsedTx.isNotEmpty()) {
                        fetchedTransactions = parsedTx
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep previous transactions if this fails
        }

        synchronized(lock) {
            if (fetchedPositions != null) {
                cachedPositions.clear()
                cachedPositions.addAll(fetchedPositions)
                if (fetchedTransactions != null) {
                    cachedTransactions.clear()
                    cachedTransactions.addAll(fetchedTransactions)
                }

                // Recalculate summary metrics from updated live positions
                val totalMarketValue = roundPaise(cachedPositions.sumOf { it.marketValue })
                val totalUnrealized = roundPaise(cachedPositions.sumOf { it.unrealizedPnl })
                val deployed = roundPaise(totalMarketValue - totalUnrealized)
                val allocated = cachedSummary.allocatedCapital
                val realized = cachedSummary.realizedPnl
                val available = roundPaise((allocated - deployed + realized).coerceAtLeast(0.0))
                val nav = roundPaise(deployed + available + totalUnrealized)
                val totalPnl = roundPaise(realized + totalUnrealized)
                val totalPnlPct = if (allocated > 0) (totalPnl / allocated) * 100.0 else 0.0
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
                    activeSlots = cachedPositions.size,
                    lastUpdated = nowStr
                )

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

                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                cachedSummary = cachedSummary.copy(
                    totalNav = nav,
                    deployedCapital = newDeployedCapital,
                    availableCapital = newAvailableCapital,
                    realizedPnl = newRealizedPnl,
                    unrealizedPnl = totalUnrealized,
                    totalPnl = totalPnl,
                    totalPnlPct = totalPnlPct,
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
            }

            Triple(cachedSummary, cachedPositions.toList(), cachedTransactions.toList())
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
}
