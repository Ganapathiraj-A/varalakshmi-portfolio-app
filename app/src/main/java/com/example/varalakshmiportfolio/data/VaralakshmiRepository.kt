package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.StockRecommendationItem
import com.example.varalakshmiportfolio.model.HistoricalPricePoint
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Typed result representing synchronization state with the live trading engine.
 */
sealed class SyncResult {
    abstract val summary: PortfolioSummary
    abstract val positions: List<PositionItem>
    abstract val transactions: List<TransactionItem>
    abstract val nifty: MarketIndexItem
    abstract val recommendations: List<StockRecommendationItem>
    abstract val recommendationHistory: List<HistoricalRecommendationItem>
    abstract val recommendationHistorySummary: RecommendationHistorySummary

    data class Success(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>,
        override val nifty: MarketIndexItem = MarketIndexItem(),
        override val recommendations: List<StockRecommendationItem> = emptyList(),
        override val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
        override val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0)
    ) : SyncResult()

    data class OfflineCacheFallback(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>,
        val message: String,
        override val nifty: MarketIndexItem = MarketIndexItem(),
        override val recommendations: List<StockRecommendationItem> = emptyList(),
        override val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
        override val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0)
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

        fun createDefaultNifty() = MarketIndexItem(
            symbol = "NIFTY 50",
            ltp = 23401.05,
            change = 72.05,
            changePct = 0.31,
            open = 23352.15,
            high = 23404.90,
            low = 23351.75,
            previousClose = 23329.00,
            timestamp = "2026-09-23 09:20:00",
            status = "LIVE"
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

        fun generateTradingDates(count: Int = 45): List<String> {
            val dates = mutableListOf<String>()
            val cal = Calendar.getInstance(Locale.US).apply {
                set(2026, Calendar.SEPTEMBER, 23, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            while (dates.size < count) {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                    dates.add(sdf.format(cal.time))
                }
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            return dates.reversed()
        }

        fun createDefaultRecommendations(): List<StockRecommendationItem> {
            val dates = generateTradingDates(45)

            val cupidPrices = doubleArrayOf(
                330.0, 327.5, 324.0, 321.0, 318.5, 322.0, 326.5, 325.0, 329.0, 334.0,
                332.5, 338.0, 345.0, 342.0, 348.5, 355.0, 352.0, 359.0, 366.5, 364.0,
                370.0, 377.5, 375.0, 382.0, 389.5, 386.0, 394.0, 401.5, 398.0, 406.0,
                412.0, 409.0, 416.5, 423.0, 420.5, 425.0, 428.0, 424.5, 419.0, 422.5,
                418.0, 415.0, 417.5, 414.0, 412.5
            )

            val adaniPrices = doubleArrayOf(
                588.0, 584.5, 580.0, 576.0, 579.5, 585.0, 591.0, 588.5, 594.0, 600.5,
                597.0, 604.0, 611.5, 608.0, 615.0, 622.5, 619.0, 626.0, 633.5, 630.0,
                638.0, 645.5, 642.0, 649.0, 657.0, 653.5, 661.0, 668.5, 665.0, 672.0,
                679.5, 676.0, 683.0, 690.5, 687.0, 693.0, 698.0, 694.5, 689.0, 692.5,
                688.0, 685.0, 687.5, 685.5, 684.2
            )

            val yashoPrices = doubleArrayOf(
                1530.0, 1522.0, 1515.0, 1508.0, 1514.0, 1525.0, 1538.0, 1532.0, 1545.0, 1560.0,
                1552.0, 1568.0, 1584.0, 1576.0, 1592.0, 1610.0, 1602.0, 1620.0, 1638.0, 1630.0,
                1648.0, 1666.0, 1658.0, 1675.0, 1695.0, 1686.0, 1705.0, 1724.0, 1715.0, 1735.0,
                1755.0, 1746.0, 1768.0, 1790.0, 1780.0, 1805.0, 1830.0, 1855.0, 1892.0, 1880.0,
                1865.0, 1852.0, 1860.0, 1848.0, 1845.0
            )

            val deedevPrices = doubleArrayOf(
                275.0, 272.5, 270.0, 268.0, 271.0, 275.5, 280.0, 277.5, 282.0, 287.5,
                285.0, 290.0, 295.5, 293.0, 298.0, 303.5, 301.0, 306.0, 312.0, 309.5,
                315.0, 320.5, 318.0, 323.5, 329.0, 326.5, 331.0, 336.5, 334.0, 338.0,
                341.0, 342.0, 339.5, 336.0, 338.5, 335.0, 332.0, 334.5, 331.0, 333.5,
                330.0, 328.0, 330.5, 329.0, 328.75
            )

            val arihantPrices = doubleArrayOf(
                79.5, 78.4, 77.5, 76.8, 77.6, 78.8, 80.0, 79.2, 80.5, 82.0,
                81.2, 82.6, 84.0, 83.2, 84.5, 86.0, 85.2, 86.8, 88.2, 87.4,
                88.8, 90.2, 89.5, 91.0, 92.5, 91.8, 93.0, 94.4, 93.6, 95.0,
                96.2, 95.4, 94.5, 93.8, 94.6, 93.8, 93.0, 94.0, 93.2, 92.8,
                93.5, 92.6, 93.0, 92.5, 92.4
            )

            fun toPoints(prices: DoubleArray): List<HistoricalPricePoint> {
                val list = mutableListOf<HistoricalPricePoint>()
                for (i in prices.indices) {
                    val d = if (i < dates.size) dates[i] else "2026-09-23"
                    list.add(HistoricalPricePoint(date = d, price = roundPaise(prices[i])))
                }
                return list
            }

            return listOf(
                StockRecommendationItem(
                    rank = 1,
                    symbol = "CUPID",
                    price = 412.50,
                    score = 96.8,
                    targetPrice = 515.00,
                    stopLossPrice = 375.00,
                    historical2mPoints = toPoints(cupidPrices)
                ),
                StockRecommendationItem(
                    rank = 2,
                    symbol = "ADANIPOWER",
                    price = 684.20,
                    score = 94.5,
                    targetPrice = 820.00,
                    stopLossPrice = 625.00,
                    historical2mPoints = toPoints(adaniPrices)
                ),
                StockRecommendationItem(
                    rank = 3,
                    symbol = "YASHO",
                    price = 1845.00,
                    score = 92.3,
                    targetPrice = 2280.00,
                    stopLossPrice = 1690.00,
                    historical2mPoints = toPoints(yashoPrices)
                ),
                StockRecommendationItem(
                    rank = 4,
                    symbol = "DEEDEV",
                    price = 328.75,
                    score = 89.7,
                    targetPrice = 410.00,
                    stopLossPrice = 298.00,
                    historical2mPoints = toPoints(deedevPrices)
                ),
                StockRecommendationItem(
                    rank = 5,
                    symbol = "ARIHANT",
                    price = 92.40,
                    score = 87.5,
                    targetPrice = 118.00,
                    stopLossPrice = 82.00,
                    historical2mPoints = toPoints(arihantPrices)
                )
            )
        }

        fun parseRecommendationsJson(jsonStr: String): List<StockRecommendationItem> {
            return try {
                val trimmed = jsonStr.trim()
                val recArray = when {
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("recommendations") ?: root.optJSONArray("data")
                    }
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> null
                } ?: return emptyList()

                val defaultRecs by lazy { createDefaultRecommendations() }
                val parsed = mutableListOf<StockRecommendationItem>()
                for (i in 0 until recArray.length()) {
                    val obj = recArray.optJSONObject(i) ?: continue
                    val rank = obj.optInt("rank", i + 1)
                    val symbol = obj.optString("symbol", "").trim().uppercase(Locale.US)
                    if (symbol.isBlank()) continue
                    val price = optSafeDouble(obj, "price", 0.0)
                    val score = optSafeDouble(obj, "score", if (obj.has("alpha_score")) optSafeDouble(obj, "alpha_score", 0.0) else 0.0)
                    val targetPrice = optSafeDouble(obj, "target_price", if (obj.has("targetPrice")) optSafeDouble(obj, "targetPrice", 0.0) else 0.0)
                    val stopLossPrice = optSafeDouble(obj, "stop_loss_price", if (obj.has("stopLossPrice")) optSafeDouble(obj, "stopLossPrice", 0.0) else 0.0)

                    val ptsArray = when {
                        obj.has("historical2m_points") -> obj.optJSONArray("historical2m_points")
                        obj.has("historical2mPoints") -> obj.optJSONArray("historical2mPoints")
                        obj.has("history") -> obj.optJSONArray("history")
                        obj.has("chart") -> obj.optJSONArray("chart")
                        else -> null
                    }
                    val pointsList = mutableListOf<HistoricalPricePoint>()
                    if (ptsArray != null) {
                        for (j in 0 until ptsArray.length()) {
                            val ptObj = ptsArray.optJSONObject(j) ?: continue
                            val d = ptObj.optString("date", if (ptObj.has("timestamp")) ptObj.optString("timestamp", "") else "")
                            val p = optSafeDouble(ptObj, "price", if (ptObj.has("close")) optSafeDouble(ptObj, "close", 0.0) else 0.0)
                            if (d.isNotBlank() && p > 0.0) {
                                pointsList.add(HistoricalPricePoint(date = d, price = p))
                            }
                        }
                    }

                    val effectivePoints = if (pointsList.isNotEmpty()) {
                        pointsList.sortedBy { it.date }
                    } else {
                        defaultRecs.find { it.symbol.equals(symbol, ignoreCase = true) }?.historical2mPoints ?: emptyList()
                    }

                    parsed.add(
                        StockRecommendationItem(
                            rank = rank,
                            symbol = symbol,
                            price = price,
                            score = score,
                            targetPrice = targetPrice,
                            stopLossPrice = stopLossPrice,
                            historical2mPoints = effectivePoints
                        )
                    )
                }
                parsed
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun calculateRecommendationHistorySummary(items: List<HistoricalRecommendationItem>): RecommendationHistorySummary {
            if (items.isEmpty()) {
                return RecommendationHistorySummary(
                    totalTrades = 0,
                    winRatePercent = 0.0,
                    avgReturnPercent = 0.0,
                    profitableTrades = 0,
                    lossTrades = 0,
                    activeTrades = 0,
                    bestTradePercent = 0.0,
                    maxLossPercent = 0.0
                )
            }
            val total = items.size
            val wins = items.count { it.pnlPercent > 0.0 }
            val losses = items.count { it.pnlPercent < 0.0 }
            val active = items.count { it.isActive }
            val winRate = if (total > 0) (wins.toDouble() / total) * 100.0 else 0.0
            val avgReturn = items.map { it.pnlPercent }.average()
            val best = items.maxOfOrNull { it.pnlPercent } ?: 0.0
            val worst = items.minOfOrNull { it.pnlPercent } ?: 0.0
            return RecommendationHistorySummary(
                totalTrades = total,
                winRatePercent = roundPaise(winRate),
                avgReturnPercent = roundPaise(avgReturn),
                profitableTrades = wins,
                lossTrades = losses,
                activeTrades = active,
                bestTradePercent = roundPaise(best),
                maxLossPercent = roundPaise(worst)
            )
        }

        fun createDefaultRecommendationHistory(): List<HistoricalRecommendationItem> {
            return listOf(
                HistoricalRecommendationItem(
                    id = "REC-2026-0908",
                    symbol = "YASHO",
                    sector = "Specialty Chemicals",
                    entryDate = "2026-09-08",
                    exitDate = null,
                    entryPrice = 1690.00,
                    exitPrice = 1845.00,
                    pnlPercent = 9.17,
                    holdingDays = 11,
                    status = "ACTIVE",
                    exitReason = "Trailing Stop Armed at ₹1,697.40",
                    score = 92.3
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0902",
                    symbol = "ADANIPOWER",
                    sector = "Power & Infrastructure",
                    entryDate = "2026-09-02",
                    exitDate = null,
                    entryPrice = 615.00,
                    exitPrice = 684.20,
                    pnlPercent = 11.25,
                    holdingDays = 15,
                    status = "ACTIVE",
                    exitReason = "Trailing Stop Armed at ₹629.46",
                    score = 94.5
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0817",
                    symbol = "INDSWFTLAB",
                    sector = "Pharmaceuticals",
                    entryDate = "2026-08-17",
                    exitDate = "2026-08-20",
                    entryPrice = 112.50,
                    exitPrice = 117.25,
                    pnlPercent = 4.21,
                    holdingDays = 4,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 88.4
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0806",
                    symbol = "CUPID",
                    sector = "Healthcare & Consumer",
                    entryDate = "2026-08-06",
                    exitDate = "2026-08-18",
                    entryPrice = 330.00,
                    exitPrice = 353.75,
                    pnlPercent = 7.20,
                    holdingDays = 8,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 96.8
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0805",
                    symbol = "ARIHANT",
                    sector = "Services & Media",
                    entryDate = "2026-08-05",
                    exitDate = "2026-08-07",
                    entryPrice = 82.50,
                    exitPrice = 78.75,
                    pnlPercent = -4.55,
                    holdingDays = 2,
                    status = "CUT_LOSS",
                    exitReason = "Cut Loss (-4% from Entry)",
                    score = 86.2
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0731",
                    symbol = "YASHO",
                    sector = "Specialty Chemicals",
                    entryDate = "2026-07-31",
                    exitDate = "2026-08-04",
                    entryPrice = 1530.00,
                    exitPrice = 1873.30,
                    pnlPercent = 22.44,
                    holdingDays = 4,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 94.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0724",
                    symbol = "ARIHANT",
                    sector = "Services & Media",
                    entryDate = "2026-07-24",
                    exitDate = "2026-07-31",
                    entryPrice = 930.10,
                    exitPrice = 1087.05,
                    pnlPercent = 16.58,
                    holdingDays = 5,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 91.5
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0722",
                    symbol = "CEMPRO",
                    sector = "Building Materials",
                    entryDate = "2026-07-22",
                    exitDate = "2026-07-24",
                    entryPrice = 280.00,
                    exitPrice = 248.30,
                    pnlPercent = -11.31,
                    holdingDays = 2,
                    status = "CUT_LOSS",
                    exitReason = "Gap Down / Cut Loss",
                    score = 85.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0717",
                    symbol = "HFCL",
                    sector = "Telecom & Infrastructure",
                    entryDate = "2026-07-17",
                    exitDate = "2026-07-21",
                    entryPrice = 128.00,
                    exitPrice = 122.80,
                    pnlPercent = -4.06,
                    holdingDays = 2,
                    status = "CUT_LOSS",
                    exitReason = "Cut Loss (-4% from Entry)",
                    score = 87.1
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0713",
                    symbol = "NOVARTIND",
                    sector = "Healthcare & Pharma",
                    entryDate = "2026-07-13",
                    exitDate = "2026-07-16",
                    entryPrice = 1120.00,
                    exitPrice = 1139.15,
                    pnlPercent = 1.71,
                    holdingDays = 3,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 89.2
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0710",
                    symbol = "NINSYS",
                    sector = "Technology",
                    entryDate = "2026-07-10",
                    exitDate = "2026-07-27",
                    entryPrice = 410.00,
                    exitPrice = 382.30,
                    pnlPercent = -6.76,
                    holdingDays = 11,
                    status = "CUT_LOSS",
                    exitReason = "Cut Loss (-4% from Entry)",
                    score = 84.8
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0610",
                    symbol = "KOVAI",
                    sector = "Healthcare",
                    entryDate = "2026-06-10",
                    exitDate = "2026-07-23",
                    entryPrice = 4500.00,
                    exitPrice = 4503.60,
                    pnlPercent = 0.08,
                    holdingDays = 30,
                    status = "TRAILING_STOP",
                    exitReason = "Breakeven Trailing Stop",
                    score = 88.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0612",
                    symbol = "CUPID",
                    sector = "Healthcare & Consumer",
                    entryDate = "2026-06-12",
                    exitDate = "2026-07-09",
                    entryPrice = 245.00,
                    exitPrice = 303.80,
                    pnlPercent = 24.01,
                    holdingDays = 19,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 97.2
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0522",
                    symbol = "DEEDEV",
                    sector = "Capital Goods",
                    entryDate = "2026-05-22",
                    exitDate = "2026-06-12",
                    entryPrice = 220.00,
                    exitPrice = 266.05,
                    pnlPercent = 20.93,
                    holdingDays = 14,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 93.4
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0519",
                    symbol = "BBOX",
                    sector = "IT Services",
                    entryDate = "2026-05-19",
                    exitDate = "2026-06-09",
                    entryPrice = 315.00,
                    exitPrice = 337.00,
                    pnlPercent = 6.98,
                    holdingDays = 14,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 90.1
                )
            )
        }

        fun parseRecommendationHistoryJson(jsonStr: String): List<HistoricalRecommendationItem> {
            return try {
                val trimmed = jsonStr.trim()
                val array = when {
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("recommendation_history")
                            ?: root.optJSONArray("recommendations_history")
                            ?: root.optJSONArray("history")
                            ?: root.optJSONArray("trades")
                            ?: root.optJSONArray("data")
                    }
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> null
                } ?: return emptyList()

                val list = mutableListOf<HistoricalRecommendationItem>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val sym = obj.optString("symbol", "").trim().uppercase(Locale.US)
                    if (sym.isBlank()) continue
                    val id = obj.optString("id", "REC-${i + 1}")
                    val sector = obj.optString("sector", "General")
                    val entryDate = obj.optString("entry_date", if (obj.has("buy_date")) obj.optString("buy_date", "2026-06-01") else "2026-06-01")
                    val exitDate = when {
                        obj.has("exit_date") && !obj.isNull("exit_date") -> obj.optString("exit_date").takeIf { it.isNotBlank() }
                        obj.has("sell_date") && !obj.isNull("sell_date") -> obj.optString("sell_date").takeIf { it.isNotBlank() }
                        else -> null
                    }
                    val entryPx = optSafeDouble(obj, "entry_price", if (obj.has("buy_px")) optSafeDouble(obj, "buy_px", 0.0) else optSafeDouble(obj, "buy_price", 0.0))
                    val exitPx = optSafeDouble(obj, "exit_price", if (obj.has("sell_px")) optSafeDouble(obj, "sell_px", entryPx) else optSafeDouble(obj, "sell_price", entryPx))
                    val pnlPct = optSafeDouble(obj, "pnl_percent", if (obj.has("ret_pct")) optSafeDouble(obj, "ret_pct", 0.0) else 0.0)
                    val holdDays = obj.optInt("holding_days", if (obj.has("hold_days")) obj.optInt("hold_days", 5) else 5)
                    val status = obj.optString("status", if (exitDate == null) "ACTIVE" else "CLOSED")
                    val exitReason = obj.optString("exit_reason", if (obj.has("decision")) obj.optString("decision", "") else "")
                    val score = optSafeDouble(obj, "score", 90.0)

                    list.add(
                        HistoricalRecommendationItem(
                            id = id,
                            symbol = sym,
                            sector = sector,
                            entryDate = entryDate,
                            exitDate = exitDate,
                            entryPrice = roundPaise(entryPx),
                            exitPrice = roundPaise(exitPx),
                            pnlPercent = roundPaise(pnlPct),
                            holdingDays = holdDays,
                            status = status,
                            exitReason = exitReason,
                            score = roundPaise(score)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private val lock = Any()

    // Default Seed Data: Aligned strictly to eliminate refresh drift
    // STL (cost 34,371.00) + AHCL (cost 33,365.77) + TBZ (cost 31,800.00) = 99,536.77
    // Allocated: 100,000.00 -> Available: 463.23
    // NAV = 99,536.77 + 463.23 + 9,268.80 = 109,268.80
    private var cachedSummary = createDefaultSummary()
    private val cachedPositions = createDefaultPositions().toMutableList()
    private val cachedTransactions = createDefaultTransactions().toMutableList()
    private var cachedNifty = createDefaultNifty()
    private val cachedRecommendations = createDefaultRecommendations().toMutableList()
    private val cachedRecommendationHistory = createDefaultRecommendationHistory().toMutableList()
    private var cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)

    fun resetToDefaultSeed() = synchronized(lock) {
        cachedSummary = createDefaultSummary()
        cachedPositions.clear()
        cachedPositions.addAll(createDefaultPositions())
        cachedTransactions.clear()
        cachedTransactions.addAll(createDefaultTransactions())
        cachedNifty = createDefaultNifty()
        cachedRecommendations.clear()
        cachedRecommendations.addAll(createDefaultRecommendations())
        cachedRecommendationHistory.clear()
        cachedRecommendationHistory.addAll(createDefaultRecommendationHistory())
        cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
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
    fun getCachedNifty(): MarketIndexItem = synchronized(lock) { ensureLoaded(); cachedNifty }
    fun getCachedRecommendations(): List<StockRecommendationItem> = synchronized(lock) { ensureLoaded(); cachedRecommendations.toList() }
    fun getCachedRecommendationHistory(): List<HistoricalRecommendationItem> = synchronized(lock) { ensureLoaded(); cachedRecommendationHistory.toList() }
    fun getCachedRecommendationHistorySummary(): RecommendationHistorySummary = synchronized(lock) { ensureLoaded(); cachedRecommendationHistorySummary }

    suspend fun refreshData(serverBaseUrl: String, authToken: String = DEFAULT_AUTH_TOKEN): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = serverBaseUrl.trimEnd('/')

        var fetchedPositions: List<PositionItem>? = null
        var fetchedTransactions: List<TransactionItem>? = null
        var fetchedNifty: MarketIndexItem? = null
        var fetchedRecommendations: List<StockRecommendationItem>? = null
        var fetchedRecommendationHistory: List<HistoricalRecommendationItem>? = null
        var networkError: String? = null

        val tokenQuery = if (authToken.isNotBlank()) "&token=$authToken" else ""

        try {
            // 1. Fetch positions
            val positionsUrl = "$cleanUrl/api/live-trading/positions?strategy=VARALAKSHMI_ALPHA_SCALE_35$tokenQuery"
            val positionsJson = httpGet(positionsUrl, authToken)
            if (positionsJson != null) {
                val root = JSONObject(positionsJson)

                val niftyObj = root.optJSONObject("nifty")
                if (niftyObj != null) {
                    val ltp = optSafeDouble(niftyObj, "ltp", cachedNifty.ltp)
                    val change = optSafeDouble(niftyObj, "change", cachedNifty.change)
                    val changePct = optSafeDouble(niftyObj, "change_pct", cachedNifty.changePct)
                    val open = optSafeDouble(niftyObj, "open", cachedNifty.open)
                    val high = optSafeDouble(niftyObj, "high", cachedNifty.high)
                    val low = optSafeDouble(niftyObj, "low", cachedNifty.low)
                    val prev = optSafeDouble(niftyObj, "previous_close", cachedNifty.previousClose)
                    val ts = niftyObj.optString("timestamp", cachedNifty.timestamp)
                    val stat = niftyObj.optString("status", "LIVE")
                    fetchedNifty = MarketIndexItem(
                        symbol = niftyObj.optString("symbol", "NIFTY 50"),
                        ltp = ltp,
                        change = change,
                        changePct = changePct,
                        open = open,
                        high = high,
                        low = low,
                        previousClose = prev,
                        timestamp = ts,
                        status = stat
                    )
                }

                val recArray = root.optJSONArray("recommendations")
                if (recArray != null && recArray.length() > 0) {
                    val parsedRecs = parseRecommendationsJson(root.toString())
                    if (parsedRecs.isNotEmpty()) {
                        fetchedRecommendations = parsedRecs
                    }
                }

                val histArray = root.optJSONArray("recommendation_history")
                    ?: root.optJSONArray("recommendations_history")
                    ?: root.optJSONArray("history")
                if (histArray != null && histArray.length() > 0) {
                    val parsedHist = parseRecommendationHistoryJson(root.toString())
                    if (parsedHist.isNotEmpty()) {
                        fetchedRecommendationHistory = parsedHist
                    }
                }

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
            val stratTokenQuery = if (authToken.isNotBlank()) "?token=$authToken" else ""
            val strategiesUrl = "$cleanUrl/api/live-trading/strategies$stratTokenQuery"
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

        if (fetchedNifty == null) {
            try {
                val stratTokenQuery = if (authToken.isNotBlank()) "?token=$authToken" else ""
                val niftyUrl = "$cleanUrl/api/live-trading/nifty$stratTokenQuery"
                val niftyJson = httpGet(niftyUrl, authToken)
                if (niftyJson != null) {
                    val nObj = JSONObject(niftyJson)
                    val ltp = optSafeDouble(nObj, "ltp", cachedNifty.ltp)
                    val change = optSafeDouble(nObj, "change", cachedNifty.change)
                    val changePct = optSafeDouble(nObj, "change_pct", cachedNifty.changePct)
                    val open = optSafeDouble(nObj, "open", cachedNifty.open)
                    val high = optSafeDouble(nObj, "high", cachedNifty.high)
                    val low = optSafeDouble(nObj, "low", cachedNifty.low)
                    val prev = optSafeDouble(nObj, "previous_close", cachedNifty.previousClose)
                    val ts = nObj.optString("timestamp", cachedNifty.timestamp)
                    val stat = nObj.optString("status", "LIVE")
                    fetchedNifty = MarketIndexItem(
                        symbol = nObj.optString("symbol", "NIFTY 50"),
                        ltp = ltp,
                        change = change,
                        changePct = changePct,
                        open = open,
                        high = high,
                        low = low,
                        previousClose = prev,
                        timestamp = ts,
                        status = stat
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Non-fatal, keep cached nifty
            }
        }

        if (fetchedRecommendations == null) {
            try {
                val recUrl = "$cleanUrl/api/live-trading/recommendations?limit=5$tokenQuery"
                val recJson = httpGet(recUrl, authToken)
                if (recJson != null) {
                    val parsed = parseRecommendationsJson(recJson)
                    if (parsed.isNotEmpty()) {
                        fetchedRecommendations = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep cached recommendations
            }
        }

        if (fetchedRecommendationHistory == null) {
            try {
                val histUrl = "$cleanUrl/api/live-trading/recommendations/history?months=3$tokenQuery"
                val histJson = httpGet(histUrl, authToken)
                if (histJson != null) {
                    val parsed = parseRecommendationHistoryJson(histJson)
                    if (parsed.isNotEmpty()) {
                        fetchedRecommendationHistory = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep cached recommendation history
            }
        }

        synchronized(lock) {
            if (fetchedNifty != null) {
                cachedNifty = fetchedNifty
            }

            if (fetchedRecommendations != null) {
                cachedRecommendations.clear()
                cachedRecommendations.addAll(fetchedRecommendations)
            }

            if (fetchedRecommendationHistory != null) {
                cachedRecommendationHistory.clear()
                cachedRecommendationHistory.addAll(fetchedRecommendationHistory)
                cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
            }

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

                SyncResult.Success(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    nifty = cachedNifty,
                    recommendations = cachedRecommendations.toList(),
                    recommendationHistory = cachedRecommendationHistory.toList(),
                    recommendationHistorySummary = cachedRecommendationHistorySummary
                )
            } else {
                // Offline fallback - preserve existing cache and timestamp
                SyncResult.OfflineCacheFallback(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    message = networkError ?: "Offline mode: server unreachable",
                    nifty = cachedNifty,
                    recommendations = cachedRecommendations.toList(),
                    recommendationHistory = cachedRecommendationHistory.toList(),
                    recommendationHistorySummary = cachedRecommendationHistorySummary
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

            val niftyObj = JSONObject().apply {
                put("symbol", cachedNifty.symbol)
                put("ltp", cachedNifty.ltp)
                put("change", cachedNifty.change)
                put("changePct", cachedNifty.changePct)
                put("open", cachedNifty.open)
                put("high", cachedNifty.high)
                put("low", cachedNifty.low)
                put("previousClose", cachedNifty.previousClose)
                put("timestamp", cachedNifty.timestamp)
                put("status", cachedNifty.status)
            }
            root.put("nifty", niftyObj)

            val recArray = JSONArray()
            for (r in cachedRecommendations) {
                val rObj = JSONObject().apply {
                    put("rank", r.rank)
                    put("symbol", r.symbol)
                    put("price", r.price)
                    put("score", r.score)
                    put("targetPrice", r.targetPrice)
                    put("stopLossPrice", r.stopLossPrice)
                    val ptsArray = JSONArray()
                    for (pt in r.historical2mPoints) {
                        val ptObj = JSONObject().apply {
                            put("date", pt.date)
                            put("price", pt.price)
                        }
                        ptsArray.put(ptObj)
                    }
                    put("historical2mPoints", ptsArray)
                }
                recArray.put(rObj)
            }
            root.put("recommendations", recArray)

            val histArray = JSONArray()
            for (h in cachedRecommendationHistory) {
                val hObj = JSONObject().apply {
                    put("id", h.id)
                    put("symbol", h.symbol)
                    put("sector", h.sector)
                    put("entry_date", h.entryDate)
                    if (h.exitDate != null) put("exit_date", h.exitDate)
                    put("entry_price", h.entryPrice)
                    put("exit_price", h.exitPrice)
                    put("pnl_percent", h.pnlPercent)
                    put("holding_days", h.holdingDays)
                    put("status", h.status)
                    put("exit_reason", h.exitReason)
                    put("score", h.score)
                }
                histArray.put(hObj)
            }
            root.put("recommendation_history", histArray)

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

            val niftyObj = root.optJSONObject("nifty")
            val parsedNifty = if (niftyObj != null) {
                MarketIndexItem(
                    symbol = niftyObj.optString("symbol", cachedNifty.symbol),
                    ltp = optSafeDouble(niftyObj, "ltp", cachedNifty.ltp),
                    change = optSafeDouble(niftyObj, "change", cachedNifty.change),
                    changePct = optSafeDouble(niftyObj, "changePct", optSafeDouble(niftyObj, "change_pct", cachedNifty.changePct)),
                    open = optSafeDouble(niftyObj, "open", cachedNifty.open),
                    high = optSafeDouble(niftyObj, "high", cachedNifty.high),
                    low = optSafeDouble(niftyObj, "low", cachedNifty.low),
                    previousClose = optSafeDouble(niftyObj, "previousClose", optSafeDouble(niftyObj, "previous_close", cachedNifty.previousClose)),
                    timestamp = niftyObj.optString("timestamp", cachedNifty.timestamp),
                    status = niftyObj.optString("status", cachedNifty.status)
                )
            } else null

            val recArray = root.optJSONArray("recommendations")
            val parsedRecommendations = if (recArray != null && recArray.length() > 0) {
                parseRecommendationsJson(recArray.toString())
            } else null

            val histArray = root.optJSONArray("recommendation_history")
            val parsedHistory = if (histArray != null && histArray.length() > 0) {
                parseRecommendationHistoryJson(histArray.toString())
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
            if (parsedNifty != null) {
                cachedNifty = parsedNifty
            }
            if (parsedRecommendations != null && parsedRecommendations.isNotEmpty()) {
                cachedRecommendations.clear()
                cachedRecommendations.addAll(parsedRecommendations)
            }
            if (parsedHistory != null && parsedHistory.isNotEmpty()) {
                cachedRecommendationHistory.clear()
                cachedRecommendationHistory.addAll(parsedHistory)
                cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
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
