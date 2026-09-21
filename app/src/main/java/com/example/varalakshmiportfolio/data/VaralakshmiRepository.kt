package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaralakshmiRepository {

    // Default Seed Data from Live Trading Engine
    private var cachedSummary = PortfolioSummary(
        strategyId = "VARALAKSHMI_ALPHA_SCALE_35",
        strategyName = "VaraLakshmi Alpha (80%+ Fast Rotation Compounder)",
        formula = "AlphaZero_MCTS_Options_Gated_Agile_Exit(cut_loss=4%, trail_stop=8%, target=35%)",
        status = "ACTIVE",
        totalNav = 109268.80,
        allocatedCapital = 100000.00,
        deployedCapital = 97205.62,
        availableCapital = 2794.38,
        realizedPnl = 0.00,
        unrealizedPnl = 9268.80,
        totalPnl = 9268.80,
        totalPnlPct = 9.2688,
        activeSlots = 3,
        maxSlots = 3,
        lastUpdated = "2026-09-21 09:26:07"
    )

    private var cachedPositions = mutableListOf(
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

    private var cachedTransactions = mutableListOf(
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

    fun getCachedSummary(): PortfolioSummary = cachedSummary
    fun getCachedPositions(): List<PositionItem> = cachedPositions.toList()
    fun getCachedTransactions(): List<TransactionItem> = cachedTransactions.toList()

    suspend fun refreshData(serverBaseUrl: String): Triple<PortfolioSummary, List<PositionItem>, List<TransactionItem>> =
        withContext(Dispatchers.IO) {
            val cleanUrl = serverBaseUrl.trimEnd('/')

            try {
                // 1. Fetch positions
                val positionsUrl = "$cleanUrl/api/live-trading/positions?strategy=VARALAKSHMI_ALPHA_SCALE_35"
                val positionsJson = httpGet(positionsUrl)
                if (positionsJson != null) {
                    val root = JSONObject(positionsJson)
                    val activeArray = root.optJSONArray("active")
                    if (activeArray != null) {
                        val parsedPositions = mutableListOf<PositionItem>()
                        for (i in 0 until activeArray.length()) {
                            val obj = activeArray.getJSONObject(i)
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
                            cachedPositions = parsedPositions
                        }
                    }
                }
            } catch (e: Exception) {
                // Network failure - retain cache
            }

            try {
                // 2. Fetch transactions
                val txUrl = "$cleanUrl/api/live-trading/transactions?strategy=VARALAKSHMI_ALPHA_SCALE_35&limit=25"
                val txJson = httpGet(txUrl)
                if (txJson != null) {
                    val root = JSONObject(txJson)
                    val txArray = root.optJSONArray("transactions")
                    if (txArray != null) {
                        val parsedTx = mutableListOf<TransactionItem>()
                        for (i in 0 until txArray.length()) {
                            val obj = txArray.getJSONObject(i)
                            val realized = obj.optDouble("realized_pnl", 0.0)
                            val side = obj.optString("side", "BUY")
                            val symbol = obj.optString("symbol", "")
                            
                            // Find active pos diff if BUY entry
                            val matchedPos = cachedPositions.find { it.symbol == symbol }
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
                            cachedTransactions = parsedTx
                        }
                    }
                }
            } catch (e: Exception) {
                // Retain cache
            }

            // Recalculate summary metrics from positions
            val totalMarketValue = cachedPositions.sumOf { it.marketValue }
            val totalUnrealized = cachedPositions.sumOf { it.unrealizedPnl }
            val allocated = 100000.00
            val deployed = totalMarketValue - totalUnrealized
            val available = (allocated - deployed).coerceAtLeast(0.0)
            val nav = deployed + available + totalUnrealized
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

            cachedSummary = cachedSummary.copy(
                totalNav = nav,
                allocatedCapital = allocated,
                deployedCapital = deployed,
                availableCapital = available,
                unrealizedPnl = totalUnrealized,
                totalPnl = totalUnrealized,
                totalPnlPct = if (allocated > 0) (totalUnrealized / allocated) * 100.0 else 0.0,
                activeSlots = cachedPositions.size,
                lastUpdated = nowStr
            )

            Triple(cachedSummary, cachedPositions.toList(), cachedTransactions.toList())
        }

    fun removePosition(symbol: String): Pair<PortfolioSummary, List<PositionItem>> {
        cachedPositions.removeAll { it.symbol == symbol }
        val totalMarketValue = cachedPositions.sumOf { it.marketValue }
        val totalUnrealized = cachedPositions.sumOf { it.unrealizedPnl }
        val allocated = 100000.00
        val deployed = (totalMarketValue - totalUnrealized).coerceAtLeast(0.0)
        val available = (allocated - deployed).coerceAtLeast(0.0)
        val nav = deployed + available + totalUnrealized

        cachedSummary = cachedSummary.copy(
            totalNav = nav,
            deployedCapital = deployed,
            availableCapital = available,
            unrealizedPnl = totalUnrealized,
            totalPnl = totalUnrealized,
            totalPnlPct = if (allocated > 0) (totalUnrealized / allocated) * 100.0 else 0.0,
            activeSlots = cachedPositions.size
        )
        return Pair(cachedSummary, cachedPositions.toList())
    }

    private fun httpGet(urlStr: String, timeoutMs: Int = 4000): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
