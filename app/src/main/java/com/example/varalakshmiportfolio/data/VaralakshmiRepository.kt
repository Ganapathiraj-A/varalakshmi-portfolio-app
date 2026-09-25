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

        fun isBoughtToday(entryDate: String): Boolean = isPositionBoughtToday(entryDate)

        fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double =
            com.example.varalakshmiportfolio.model.calculateReferencePrice(entryDate, entryPrice, previousClose)

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

            val raymondPrices = doubleArrayOf(
                605.6, 588.4, 595.0, 576.8, 588.5, 582.2, 580.1, 602.0, 605.1, 629.4,
                627.6, 614.95, 627.0, 624.3, 618.95, 642.0, 635.45, 647.85, 625.0, 630.5,
                656.65, 643.15, 632.2, 627.2, 621.05, 614.0, 626.85, 631.85, 635.3, 667.25,
                757.9, 740.85, 772.4, 857.8, 808.45, 853.85, 1002.8, 1002.8, 995.95, 993.9,
                978.2, 978.0, 1084.95, 1116.95, 1114.65
            )

            val xtranetPrices = doubleArrayOf(
                129.2, 129.2, 129.2, 129.2, 129.2, 129.2, 126.53, 124.01, 127.94, 128.01,
                129.73, 136.21, 143.02, 150.17, 156.18, 150.4, 172.5, 158.7, 168.97, 169.11,
                160.18, 167.03, 167.69, 163.09, 159.83, 164.26, 197.11, 194.82, 187.34, 193.68,
                195.2, 234.24, 279.83, 266.7, 293.37, 285.59, 271.34, 271.34, 276.09, 283.03,
                302.42, 312.16, 341.43, 334.36, 317.65
            )

            val aareydrugsPrices = doubleArrayOf(
                75.98, 77.2, 77.5, 75.95, 77.15, 75.61, 77.12, 77.86, 81.53, 83.9,
                84.31, 81.8, 81.47, 83.81, 85.51, 83.1, 84.49, 83.87, 85.58, 82.16,
                82.59, 80.59, 81.2, 78.1, 76.91, 80.75, 84.78, 89.01, 88.21, 92.49,
                91.72, 94.44, 95.79, 95.88, 98.03, 99.98, 95.71, 98.89, 98.82, 99.94,
                101.72, 102.13, 102.37, 102.99, 102.0
            )

            val gcslPrices = doubleArrayOf(
                523.55, 521.05, 531.0, 531.1, 519.6, 517.9, 522.3, 520.55, 508.75, 508.15,
                523.05, 529.65, 523.95, 517.35, 465.8, 502.2, 515.95, 517.75, 492.4, 512.3,
                521.7, 482.05, 488.9, 512.15, 523.1, 538.0, 540.55, 541.3, 556.55, 562.9,
                571.85, 586.7, 595.15, 601.85, 598.25, 601.9, 614.8, 600.7, 601.55, 594.75,
                594.35, 618.55, 635.1, 639.2, 642.7
            )

            val gmmpfaudlrPrices = doubleArrayOf(
                795.54, 807.08, 829.75, 805.8, 803.65, 814.3, 864.4, 858.45, 847.7, 857.1,
                979.85, 987.3, 993.6, 1003.8, 997.3, 1012.9, 1048.25, 1056.55, 1031.25, 1019.85,
                1018.2, 1022.35, 989.3, 1004.9, 1017.4, 1025.15, 1026.7, 1042.45, 1083.8, 1070.0,
                1073.5, 1147.9, 1235.7, 1285.2, 1305.4, 1319.2, 1307.9, 1307.9, 1348.0, 1335.0,
                1363.4, 1467.0, 1507.2, 1462.3, 1432.6
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
                    symbol = "RAYMOND",
                    price = 1114.65,
                    score = 10.02,
                    targetPrice = 1390.00,
                    stopLossPrice = 980.00,
                    historical2mPoints = toPoints(raymondPrices)
                ),
                StockRecommendationItem(
                    rank = 2,
                    symbol = "XTRANET",
                    price = 317.65,
                    score = 8.91,
                    targetPrice = 395.00,
                    stopLossPrice = 275.00,
                    historical2mPoints = toPoints(xtranetPrices)
                ),
                StockRecommendationItem(
                    rank = 3,
                    symbol = "AAREYDRUGS",
                    price = 102.00,
                    score = 8.87,
                    targetPrice = 128.00,
                    stopLossPrice = 89.00,
                    historical2mPoints = toPoints(aareydrugsPrices)
                ),
                StockRecommendationItem(
                    rank = 4,
                    symbol = "GCSL",
                    price = 642.70,
                    score = 8.77,
                    targetPrice = 800.00,
                    stopLossPrice = 560.00,
                    historical2mPoints = toPoints(gcslPrices)
                ),
                StockRecommendationItem(
                    rank = 5,
                    symbol = "GMMPFAUDLR",
                    price = 1432.60,
                    score = 8.76,
                    targetPrice = 1790.00,
                    stopLossPrice = 1250.00,
                    historical2mPoints = toPoints(gmmpfaudlrPrices)
                )
            )
        }

        fun parseRecommendationsJson(jsonStr: String): List<StockRecommendationItem> {
            return try {
                val trimmed = jsonStr.trim()
                val recArray = when {
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("candidates")
                            ?: root.optJSONArray("recommendations")
                            ?: root.optJSONArray("data")
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
                    val price = optSafeDouble(
                        obj, "price",
                        if (obj.has("ltp")) optSafeDouble(obj, "ltp", 0.0)
                        else if (obj.has("close")) optSafeDouble(obj, "close", 0.0)
                        else 0.0
                    )
                    val score = optSafeDouble(
                        obj, "score",
                        if (obj.has("alpha_score")) optSafeDouble(obj, "alpha_score", 0.0)
                        else if (obj.has("ml_score")) optSafeDouble(obj, "ml_score", 0.0)
                        else 0.0
                    )
                    val targetPrice = optSafeDouble(
                        obj, "target_price",
                        if (obj.has("targetPrice")) optSafeDouble(obj, "targetPrice", 0.0)
                        else roundPaise(price * 1.25)
                    )
                    val stopLossPrice = optSafeDouble(
                        obj, "stop_loss_price",
                        if (obj.has("stopLossPrice")) optSafeDouble(obj, "stopLossPrice", 0.0)
                        else roundPaise(price * 0.88)
                    )

                    val ptsArray = when {
                        obj.has("historical_2m") -> obj.optJSONArray("historical_2m")
                        obj.has("historical2m_points") -> obj.optJSONArray("historical2m_points")
                        obj.has("historical2mPoints") -> obj.optJSONArray("historical2mPoints")
                        obj.has("price_history") -> obj.optJSONArray("price_history")
                        obj.has("historical2m") -> obj.optJSONArray("historical2m")
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
                    id = "REC-LIVE-TBZ-20260921",
                    symbol = "TBZ",
                    sector = "Gems & Jewellery",
                    entryDate = "2026-09-21",
                    exitDate = null,
                    entryPrice = 600.00,
                    exitPrice = 692.45,
                    pnlPercent = 15.41,
                    holdingDays = 2,
                    status = "ACTIVE",
                    exitReason = "Active Momentum Runner (Target: ₹810.00)",
                    score = 96.5
                ),
                HistoricalRecommendationItem(
                    id = "REC-LIVE-AHCL-20260918",
                    symbol = "AHCL",
                    sector = "Real Estate & Construction",
                    entryDate = "2026-09-18",
                    exitDate = null,
                    entryPrice = 22.67,
                    exitPrice = 29.68,
                    pnlPercent = 30.94,
                    holdingDays = 5,
                    status = "ACTIVE",
                    exitReason = "Active Momentum Runner (Target: ₹30.60)",
                    score = 95.8
                ),
                HistoricalRecommendationItem(
                    id = "REC-LIVE-STLNETWORK-20260917",
                    symbol = "STLNETWORK",
                    sector = "Telecommunication Equipment",
                    entryDate = "2026-09-17",
                    exitDate = null,
                    entryPrice = 40.20,
                    exitPrice = 49.70,
                    pnlPercent = 23.63,
                    holdingDays = 6,
                    status = "ACTIVE",
                    exitReason = "Active Momentum Runner (Target: ₹54.27)",
                    score = 94.2
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0817",
                    symbol = "INDSWFTLAB",
                    sector = "Pharmaceuticals",
                    entryDate = "2026-08-17",
                    exitDate = "2026-08-20",
                    entryPrice = 312.00,
                    exitPrice = 326.00,
                    pnlPercent = 4.21,
                    holdingDays = 3,
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
                    entryPrice = 254.00,
                    exitPrice = 273.00,
                    pnlPercent = 7.20,
                    holdingDays = 8,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 96.8
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0805",
                    symbol = "ARIHANT",
                    sector = "Financial Services",
                    entryDate = "2026-08-05",
                    exitDate = "2026-08-07",
                    entryPrice = 1180.00,
                    exitPrice = 1129.50,
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
                    entryPrice = 3258.90,
                    exitPrice = 4000.00,
                    pnlPercent = 22.44,
                    holdingDays = 2,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 94.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0724",
                    symbol = "ARIHANT",
                    sector = "Financial Services",
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
                    entryPrice = 1620.10,
                    exitPrice = 1441.00,
                    pnlPercent = -11.31,
                    holdingDays = 2,
                    status = "CUT_LOSS",
                    exitReason = "Cut Loss (-4% from Entry)",
                    score = 85.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0717",
                    symbol = "HFCL",
                    sector = "Telecom Equipment",
                    entryDate = "2026-07-17",
                    exitDate = "2026-07-21",
                    entryPrice = 217.24,
                    exitPrice = 209.00,
                    pnlPercent = -4.06,
                    holdingDays = 2,
                    status = "CUT_LOSS",
                    exitReason = "Cut Loss (-4% from Entry)",
                    score = 87.1
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0713",
                    symbol = "NOVARTIND",
                    sector = "Pharmaceuticals",
                    entryDate = "2026-07-13",
                    exitDate = "2026-07-16",
                    entryPrice = 1509.00,
                    exitPrice = 1539.00,
                    pnlPercent = 1.71,
                    holdingDays = 3,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 89.2
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0710",
                    symbol = "NINSYS",
                    sector = "IT & Software",
                    entryDate = "2026-07-10",
                    exitDate = "2026-07-27",
                    entryPrice = 904.00,
                    exitPrice = 845.30,
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
                    entryPrice = 5736.65,
                    exitPrice = 5756.60,
                    pnlPercent = 0.08,
                    holdingDays = 31,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 88.0
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0612",
                    symbol = "CUPID",
                    sector = "Healthcare & Consumer",
                    entryDate = "2026-06-12",
                    exitDate = "2026-07-09",
                    entryPrice = 157.66,
                    exitPrice = 196.00,
                    pnlPercent = 24.01,
                    holdingDays = 19,
                    status = "TRAILING_STOP",
                    exitReason = "Trailing Stop (-8% from Peak)",
                    score = 95.5
                ),
                HistoricalRecommendationItem(
                    id = "REC-2026-0522",
                    symbol = "DEEDEV",
                    sector = "Capital Goods",
                    entryDate = "2026-05-22",
                    exitDate = "2026-06-12",
                    entryPrice = 523.80,
                    exitPrice = 635.00,
                    pnlPercent = 20.93,
                    holdingDays = 15,
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
                    entryPrice = 940.00,
                    exitPrice = 1008.30,
                    pnlPercent = 6.98,
                    holdingDays = 15,
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
    fun getCachedRecommendations(): List<StockRecommendationItem> = synchronized(lock) {
        ensureLoaded()
        val heldSymbols = cachedPositions.map { it.symbol.trim().uppercase(Locale.US) }.toSet()
        cachedRecommendations
            .filterNot { it.symbol.trim().uppercase(Locale.US) in heldSymbols }
            .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }
    fun getCachedRecommendationHistory(): List<HistoricalRecommendationItem> = synchronized(lock) { ensureLoaded(); cachedRecommendationHistory.toList() }
    fun getCachedRecommendationHistorySummary(): RecommendationHistorySummary = synchronized(lock) { ensureLoaded(); cachedRecommendationHistorySummary }

    suspend fun syncWithServer(serverBaseUrl: String = getServerUrl(), authToken: String = getAuthToken()): SyncResult =
        refreshData(serverBaseUrl, authToken)

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

                val recArray = root.optJSONArray("candidates")
                    ?: root.optJSONArray("recommendations")
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

                        val isBoughtToday = isPositionBoughtToday(entryDate)
                        val refPrice = calculateReferencePrice(entryDate, entryPrice, effectivePrevClose)

                        val todayChg = if (refPrice > 0.0) {
                            roundPaise(currentPrice - refPrice)
                        } else if (obj.has("today_price_change") && !obj.isNull("today_price_change")) {
                            roundPaise(obj.optDouble("today_price_change", 0.0))
                        } else if (obj.has("change") && !obj.isNull("change")) {
                            roundPaise(obj.optDouble("change", 0.0))
                        } else {
                            0.0
                        }

                        val todayChgPct = if (refPrice > 0.0) {
                            roundPaise(((currentPrice - refPrice) / refPrice) * 100.0)
                        } else if (obj.has("today_price_change_pct") && !obj.isNull("today_price_change_pct")) {
                            roundPaise(obj.optDouble("today_price_change_pct", 0.0))
                        } else if (obj.has("change_pct") && !obj.isNull("change_pct")) {
                            roundPaise(obj.optDouble("change_pct", 0.0))
                        } else {
                            0.0
                        }

                        val calculatedValChg = roundPaise(quantity * todayChg)
                        val todayValChg = calculatedValChg

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
                                referencePrice = refPrice,
                                isBoughtToday = isBoughtToday,
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
        var fetchedRealizedPnl: Double? = null

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
                            if (obj.has("realized_pnl") && !obj.isNull("realized_pnl")) {
                                fetchedRealizedPnl = roundPaise(obj.optDouble("realized_pnl", 0.0))
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
                val stageUrl = "$cleanUrl/api/live-trading/stage/status?strategy=VARALAKSHMI_ALPHA_SCALE_35$tokenQuery"
                val stageJson = httpGet(stageUrl, authToken)
                if (stageJson != null) {
                    val parsed = parseRecommendationsJson(stageJson)
                    if (parsed.isNotEmpty()) {
                        fetchedRecommendations = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Fallback to recommendations endpoint
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
                val realized = fetchedRealizedPnl ?: cachedSummary.realizedPnl
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

                val heldSymbols = cachedPositions.map { it.symbol.trim().uppercase(Locale.US) }.toSet()
                val cleanRecommendations = cachedRecommendations
                    .filterNot { it.symbol.trim().uppercase(Locale.US) in heldSymbols }
                    .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

                SyncResult.Success(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    nifty = cachedNifty,
                    recommendations = cleanRecommendations,
                    recommendationHistory = cachedRecommendationHistory.toList(),
                    recommendationHistorySummary = cachedRecommendationHistorySummary
                )
            } else {
                val heldSymbols = cachedPositions.map { it.symbol.trim().uppercase(Locale.US) }.toSet()
                val cleanRecommendations = cachedRecommendations
                    .filterNot { it.symbol.trim().uppercase(Locale.US) in heldSymbols }
                    .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

                // Offline fallback - preserve existing cache and timestamp
                SyncResult.OfflineCacheFallback(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    message = networkError ?: "Offline mode: server unreachable",
                    nifty = cachedNifty,
                    recommendations = cleanRecommendations,
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

    suspend fun exitPosition(
        strategyId: String,
        symbol: String,
        serverBaseUrl: String = cachedServerUrl,
        authToken: String = cachedAuthToken
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverBaseUrl.trimEnd('/')
            val tokenQuery = if (authToken.isNotBlank()) "?token=$authToken" else ""
            val endpoint = "$cleanUrl/api/live-trading/positions/exit$tokenQuery"
            val payload = JSONObject().apply {
                put("strategy_id", strategyId)
                put("symbol", symbol)
            }
            val res = httpPost(endpoint, payload.toString(), authToken)
            if (res != null) {
                val json = JSONObject(res)
                val success = json.optBoolean("success", false)
                val msg = json.optString("message", if (success) "Exit order submitted to broker" else "Exit order rejected")
                if (success) {
                    removePosition(symbol)
                    Result.success(msg)
                } else {
                    val err = json.optString("error", msg)
                    if (json.optString("status") == "NOT_FOUND" || err.contains("No open position", ignoreCase = true)) {
                        removePosition(symbol)
                        Result.success("Position already closed on server")
                    } else {
                        Result.failure(Exception(err))
                    }
                }
            } else {
                removePosition(symbol)
                Result.failure(Exception("Offline: Position removed locally. Verify Zerodha broker order."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            removePosition(symbol)
            Result.failure(e)
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
                    put("referencePrice", p.referencePrice)
                    put("isBoughtToday", p.isBoughtToday)
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

    fun applyCachedJsonState(jsonStr: String): Boolean = synchronized(lock) {
        try {
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
                    val entryDate = p.optString("entryDate", p.optString("entry_date", ""))
                    val isBoughtToday = isPositionBoughtToday(entryDate)
                    val prevClose = if (p.has("previousClose") && !p.isNull("previousClose")) {
                        optSafeDouble(p, "previousClose", 0.0)
                    } else if (p.has("previous_close") && !p.isNull("previous_close")) {
                        optSafeDouble(p, "previous_close", 0.0)
                    } else 0.0
                    val effectivePrevClose = if (prevClose > 0.0) prevClose else entryPrice
                    val refPrice = calculateReferencePrice(entryDate, entryPrice, effectivePrevClose)

                    val todayChg = if (refPrice > 0.0) {
                        roundPaise(currentPrice - refPrice)
                    } else if (p.has("todayPriceChange") && !p.isNull("todayPriceChange")) {
                        optSafeDouble(p, "todayPriceChange", 0.0)
                    } else if (p.has("today_price_change") && !p.isNull("today_price_change")) {
                        optSafeDouble(p, "today_price_change", 0.0)
                    } else {
                        0.0
                    }

                    val todayChgPct = if (refPrice > 0.0) {
                        roundPaise(((currentPrice - refPrice) / refPrice) * 100.0)
                    } else if (p.has("todayPriceChangePct") && !p.isNull("todayPriceChangePct")) {
                        optSafeDouble(p, "todayPriceChangePct", 0.0)
                    } else if (p.has("today_price_change_pct") && !p.isNull("today_price_change_pct")) {
                        optSafeDouble(p, "today_price_change_pct", 0.0)
                    } else {
                        0.0
                    }

                    val calculatedValChg = roundPaise(quantity * todayChg)
                    val todayValChg = calculatedValChg

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
                            entryDate = entryDate,
                            status = p.optString("status", "OPEN"),
                            previousClose = effectivePrevClose,
                            referencePrice = refPrice,
                            isBoughtToday = isBoughtToday,
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

            val recArray = root.optJSONArray("candidates")
                ?: root.optJSONArray("recommendations")
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
            // Ensure todayPnl strictly matches active positions to prevent calculation drift
            val effectiveTodayPnl = if (cachedPositions.isNotEmpty()) {
                roundPaise(cachedPositions.sumOf { it.todayValueChange })
            } else if (parsedSummary != null) {
                parsedSummary.todayPnl
            } else {
                0.0
            }
            val baseNav = parsedSummary?.totalNav ?: cachedSummary.totalNav
            val baseAllocated = parsedSummary?.allocatedCapital ?: cachedSummary.allocatedCapital
            val prevNav = baseNav - effectiveTodayPnl
            val effectiveTodayPnlPct = if (prevNav > 0.0) roundPaise((effectiveTodayPnl / prevNav) * 100.0)
            else if (baseAllocated > 0.0) roundPaise((effectiveTodayPnl / baseAllocated) * 100.0)
            else 0.0

            if (parsedSummary != null) {
                cachedSummary = parsedSummary.copy(
                    todayPnl = effectiveTodayPnl,
                    todayPnlPct = effectiveTodayPnlPct
                )
            } else if (cachedPositions.isNotEmpty()) {
                cachedSummary = cachedSummary.copy(
                    todayPnl = effectiveTodayPnl,
                    todayPnlPct = effectiveTodayPnlPct
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
                val isOldPlaceholder = parsedRecommendations.any { it.symbol == "CUPID" }
                cachedRecommendations.clear()
                if (!isOldPlaceholder) {
                    cachedRecommendations.addAll(parsedRecommendations)
                } else {
                    cachedRecommendations.addAll(createDefaultRecommendations())
                }
            }
            if (parsedHistory != null && parsedHistory.isNotEmpty()) {
                val isOldHistory = parsedHistory.any { it.symbol == "ADANIPOWER" || it.id.startsWith("REC-2026-0908") || it.id.startsWith("REC-2026-0902") }
                cachedRecommendationHistory.clear()
                if (!isOldHistory) {
                    cachedRecommendationHistory.addAll(parsedHistory)
                } else {
                    cachedRecommendationHistory.addAll(createDefaultRecommendationHistory())
                }
                cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
            }
            isLoadedFromDisk = true
            true
        } catch (e: Exception) {
            false
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
            val success = applyCachedJsonState(jsonStr)
            if (!success) {
                throw IOException("Corrupt cache payload")
            }
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
