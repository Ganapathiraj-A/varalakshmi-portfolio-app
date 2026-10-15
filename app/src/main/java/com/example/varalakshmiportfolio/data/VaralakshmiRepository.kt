package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.calculateCurrentCagr
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.DailyReturnItem
import com.example.varalakshmiportfolio.model.StockRecommendationItem
import com.example.varalakshmiportfolio.model.HistoricalPricePoint
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.IpoActionType
import com.example.varalakshmiportfolio.model.FnoInstanceSummary
import com.example.varalakshmiportfolio.model.FnoPositionItem
import com.example.varalakshmiportfolio.model.FnoStatusInfo
import com.example.varalakshmiportfolio.model.VeeraLakshmiUiState
import com.example.varalakshmiportfolio.model.SanthanaLakshmiTradeItem
import com.example.varalakshmiportfolio.model.SanthanaLakshmiSummary
import com.example.varalakshmiportfolio.model.SanthanaLakshmiUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import java.util.TimeZone
import java.util.Locale

/**
 * Typed result representing synchronization state with the live trading engine.
 */
sealed class SyncResult {
    abstract val summary: PortfolioSummary
    abstract val positions: List<PositionItem>
    abstract val transactions: List<TransactionItem>
    abstract val dailyReturns: List<DailyReturnItem>
    abstract val nifty: MarketIndexItem
    abstract val marketIndices: List<MarketIndexItem>
    abstract val recommendations: List<StockRecommendationItem>
    abstract val recommendationHistory: List<HistoricalRecommendationItem>
    abstract val recommendationHistorySummary: RecommendationHistorySummary
    abstract val ipoNotifications: List<IpoActionNotification>
    abstract val otherPositions: List<PositionItem>

    data class Success(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>,
        override val dailyReturns: List<DailyReturnItem> = emptyList(),
        override val nifty: MarketIndexItem = MarketIndexItem(),
        override val marketIndices: List<MarketIndexItem> = emptyList(),
        override val recommendations: List<StockRecommendationItem> = emptyList(),
        override val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
        override val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0),
        override val ipoNotifications: List<IpoActionNotification> = emptyList(),
        override val otherPositions: List<PositionItem> = emptyList()
    ) : SyncResult()

    data class OfflineCacheFallback(
        override val summary: PortfolioSummary,
        override val positions: List<PositionItem>,
        override val transactions: List<TransactionItem>,
        val message: String,
        override val dailyReturns: List<DailyReturnItem> = emptyList(),
        override val nifty: MarketIndexItem = MarketIndexItem(),
        override val marketIndices: List<MarketIndexItem> = emptyList(),
        override val recommendations: List<StockRecommendationItem> = emptyList(),
        override val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
        override val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0),
        override val ipoNotifications: List<IpoActionNotification> = emptyList(),
        override val otherPositions: List<PositionItem> = emptyList()
    ) : SyncResult()
}

class VaralakshmiRepository {

    companion object {
        const val DEFAULT_SERVER_URL = "https://varalakshmi.ghostsoftwaresystems.com"
        const val DEFAULT_AUTH_TOKEN = "eyJlbWFpbCI6ImdhbmFwYXRoaXJhakBnbWFpbC5jb20iLCJleHAiOjIxMDUzNDA5NzgsIm5vbmNlIjoiYTFkYWE3NTlmMWU2ZmU1MjgxMDFlZTRmZDRjYTIzODQifQ.uEdogGmmZ7NeYDuwiWdv416rE7P5Im1s8CPByRwzgR0"
        const val CACHE_FILE_NAME = "portfolio_cache.json"
        const val FNO_CACHE_FILE_NAME = "veeralakshmi_fno_cache.json"
        const val SANTHANA_CACHE_FILE_NAME = "santhanalakshmi_cache.json"

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
            reservedCapital = 0.00,
            realizedPnl = 0.00,
            unrealizedPnl = 9268.80,
            totalPnl = 9268.80,
            totalPnlPct = 9.2688,
            todayPnl = 4007.77,
            todayPnlPct = 3.81,
            activeSlots = 3,
            maxSlots = 3,
            lastUpdated = "2026-09-21 09:26:07",
            cagrPct = 74.20,
            inceptionDate = "2026-09-17"
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

        fun createDefaultMarketIndices(): List<MarketIndexItem> =
            com.example.varalakshmiportfolio.model.createDefaultMarketIndices()

        fun parseMarketIndexItem(niftyObj: JSONObject, defaultItem: MarketIndexItem = MarketIndexItem()): MarketIndexItem {
            val sym = niftyObj.optString("symbol", defaultItem.symbol)
            val ltp = optSafeDouble(niftyObj, "ltp", defaultItem.ltp)
            val change = optSafeDouble(niftyObj, "change", defaultItem.change)
            val changePct = optSafeDouble(niftyObj, "changePct", optSafeDouble(niftyObj, "change_pct", defaultItem.changePct))
            val open = optSafeDouble(niftyObj, "open", defaultItem.open)
            val high = optSafeDouble(niftyObj, "high", defaultItem.high)
            val low = optSafeDouble(niftyObj, "low", defaultItem.low)
            val prev = optSafeDouble(niftyObj, "previousClose", optSafeDouble(niftyObj, "previous_close", defaultItem.previousClose))
            val ts = niftyObj.optString("timestamp", defaultItem.timestamp)
            val stat = niftyObj.optString("status", defaultItem.status)
            return MarketIndexItem(
                symbol = sym,
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

        fun createDefaultOtherPositions() = listOf(
            PositionItem(
                positionId = "ZERODHA_HOLDING_LIQUIDCASE",
                symbol = "LIQUIDCASE",
                quantity = 863,
                entryPrice = 115.82,
                currentPrice = 116.37,
                marketValue = 100427.31,
                unrealizedPnl = 474.65,
                unrealizedPnlPct = 0.47,
                peakPrice = 116.37,
                entryDate = "",
                status = "OPEN",
                previousClose = 116.34,
                referencePrice = 116.34,
                todayPriceChange = 0.03,
                todayPriceChangePct = 0.03,
                todayValueChange = 25.89,
                isProfitTargetEnabled = false,
                isStrategy = false,
                category = "ZERODHA"
            ),
            PositionItem(
                positionId = "ZERODHA_HOLDING_PARASYNTEX",
                symbol = "PARASYNTEX",
                quantity = 2000,
                entryPrice = 127.00,
                currentPrice = 93.75,
                marketValue = 187500.00,
                unrealizedPnl = -66500.00,
                unrealizedPnlPct = -26.18,
                peakPrice = 127.00,
                entryDate = "",
                status = "OPEN",
                previousClose = 98.65,
                referencePrice = 98.65,
                todayPriceChange = -4.90,
                todayPriceChangePct = -4.97,
                todayValueChange = -9800.00,
                isProfitTargetEnabled = false,
                isStrategy = false,
                category = "ZERODHA"
            ),
            PositionItem(
                positionId = "ZERODHA_DAY_PCJEWELLER",
                symbol = "PCJEWELLER",
                quantity = 7000,
                entryPrice = 14.32,
                currentPrice = 14.47,
                marketValue = 101290.00,
                unrealizedPnl = 1050.00,
                unrealizedPnlPct = 1.05,
                peakPrice = 14.47,
                entryDate = "today",
                status = "OPEN",
                previousClose = 14.32,
                referencePrice = 14.32,
                isBoughtToday = true,
                todayPriceChange = 0.15,
                todayPriceChangePct = 1.05,
                todayValueChange = 1050.00,
                isProfitTargetEnabled = false,
                isStrategy = false,
                category = "ZERODHA"
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
                    historical2mPoints = toPoints(raymondPrices),
                    changePct = 2.45,
                    change = 26.70
                ),
                StockRecommendationItem(
                    rank = 2,
                    symbol = "XTRANET",
                    price = 317.65,
                    score = 8.91,
                    targetPrice = 395.00,
                    stopLossPrice = 275.00,
                    historical2mPoints = toPoints(xtranetPrices),
                    changePct = 3.18,
                    change = 9.80
                ),
                StockRecommendationItem(
                    rank = 3,
                    symbol = "AAREYDRUGS",
                    price = 102.00,
                    score = 8.87,
                    targetPrice = 128.00,
                    stopLossPrice = 89.00,
                    historical2mPoints = toPoints(aareydrugsPrices),
                    changePct = 1.95,
                    change = 1.95
                ),
                StockRecommendationItem(
                    rank = 4,
                    symbol = "GCSL",
                    price = 642.70,
                    score = 8.77,
                    targetPrice = 800.00,
                    stopLossPrice = 560.00,
                    historical2mPoints = toPoints(gcslPrices),
                    changePct = 0.55,
                    change = 3.50
                ),
                StockRecommendationItem(
                    rank = 5,
                    symbol = "GMMPFAUDLR",
                    price = 1432.60,
                    score = 8.76,
                    targetPrice = 1790.00,
                    stopLossPrice = 1250.00,
                    historical2mPoints = toPoints(gmmpfaudlrPrices),
                    changePct = 1.82,
                    change = 25.60
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
                    val changePct = optSafeDouble(
                        obj, "change_pct",
                        if (obj.has("changePct")) optSafeDouble(obj, "changePct", 0.0)
                        else if (obj.has("pnl_pct")) optSafeDouble(obj, "pnl_pct", 0.0)
                        else if (obj.has("today_change_pct")) optSafeDouble(obj, "today_change_pct", 0.0)
                        else 0.0
                    )
                    val change = optSafeDouble(
                        obj, "change",
                        if (obj.has("today_change")) optSafeDouble(obj, "today_change", 0.0)
                        else 0.0
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
                            historical2mPoints = effectivePoints,
                            changePct = changePct,
                            change = change
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

        fun createDefaultIpoNotifications(): List<IpoActionNotification> = emptyList()

        fun createSampleTataTechIpoNotification(): IpoActionNotification =
            IpoActionNotification(
                id = "IPO-ALERT-SANTHANA-001",
                symbol = "TATATECH",
                companyName = "Tata Technologies Ltd",
                actionType = IpoActionType.APPLY_NOW,
                headline = "Santhana Lakshmi Alert: Apply for Tata Technologies",
                message = "Apply strictly 1 Lot (30 shares @ ₹500 Cut-off). QIB Institutional Demand: 69.4x (High Conviction). Regime: BULLISH.",
                lotPrice = 15000.00,
                lotQuantity = 1,
                lotShares = 30,
                cutoffPrice = 500.00,
                qibMultiple = 69.4,
                closeDeadline = "15:30",
                urgency = "HIGH",
                timestamp = "2026-09-26 10:00:00",
                deepLinkUrl = "https://kite.zerodha.com/ipo"
            )

        fun parseIpoNotificationsJson(jsonStr: String): List<IpoActionNotification> {
            return try {
                val trimmed = jsonStr.trim()
                val array = when {
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("ipo_notifications")
                            ?: root.optJSONArray("ipo_alerts")
                            ?: root.optJSONArray("ipo_signals")
                            ?: root.optJSONArray("notifications")
                    }
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> null
                } ?: return emptyList()

                val list = mutableListOf<IpoActionNotification>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "IPO-${i + 1}")
                    val symbol = obj.optString("symbol", "").trim().uppercase(Locale.US)
                    if (symbol.isBlank()) continue
                    val companyName = obj.optString("company_name", symbol)
                    val actionStr = obj.optString("action_type", obj.optString("action", "APPLY_NOW")).uppercase(Locale.US)
                    val actionType = try {
                        IpoActionType.valueOf(actionStr)
                    } catch (_: Exception) {
                        when {
                            actionStr.contains("MANDATE") -> IpoActionType.MANDATE_PENDING
                            actionStr.contains("WON") || actionStr.contains("ALLOTTED") -> IpoActionType.ALLOTMENT_WON
                            actionStr.contains("MISSED") || actionStr.contains("REFUND") -> IpoActionType.ALLOTMENT_MISSED
                            actionStr.contains("EXIT") || actionStr.contains("LIST") -> IpoActionType.LISTING_EXIT
                            else -> IpoActionType.APPLY_NOW
                        }
                    }
                    val headline = obj.optString("headline", "IPO Action Required: $symbol")
                    val message = obj.optString("message", "Action required for $symbol IPO.")
                    val lotPrice = optSafeDouble(obj, "lot_price", if (obj.has("price")) optSafeDouble(obj, "price", 14500.0) else 14500.0)
                    val lotQty = obj.optInt("lot_quantity", 1)
                    val lotShares = obj.optInt("lot_shares", if (obj.has("shares_per_lot")) obj.optInt("shares_per_lot", 30) else 30)
                    val cutoffPrice = optSafeDouble(obj, "cutoff_price", if (obj.has("cut_off_price")) optSafeDouble(obj, "cut_off_price", 500.0) else 500.0)
                    val qib = optSafeDouble(obj, "qib_multiple", if (obj.has("qib")) optSafeDouble(obj, "qib", 0.0) else 0.0)
                    val deadline = obj.optString("close_deadline", obj.optString("deadline", "15:30"))
                    val urgency = obj.optString("urgency", "HIGH")
                    val ts = obj.optString("timestamp", "")
                    val deepLink = obj.optString("deep_link_url", "https://kite.zerodha.com/ipo")
                    val isDismissed = obj.optBoolean("is_dismissed", false)

                    list.add(
                        IpoActionNotification(
                            id = id,
                            symbol = symbol,
                            companyName = companyName,
                            actionType = actionType,
                            headline = headline,
                            message = message,
                            lotPrice = lotPrice,
                            lotQuantity = lotQty,
                            lotShares = lotShares,
                            cutoffPrice = cutoffPrice,
                            qibMultiple = qib,
                            closeDeadline = deadline,
                            urgency = urgency,
                            timestamp = ts,
                            deepLinkUrl = deepLink,
                            isDismissed = isDismissed
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun createDefaultDailyReturns(): List<DailyReturnItem> = listOf(
            DailyReturnItem(
                date = "2026-10-01",
                dailyPnl = -18310.80,
                returnPct = -2.98,
                totalNav = 598256.00,
                tradeCount = 3,
                status = "LOSS",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK_20261001_SELL",
                        timestamp = "2026-10-01 15:15:18",
                        side = "SELL",
                        symbol = "STLNETWORK",
                        quantity = 4150,
                        fillPrice = 47.17,
                        grossAmount = 195764.75,
                        fees = 218.78,
                        realizedPnl = -8861.67,
                        pnlDifference = "-₹8,861.67"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_CUBEXTUB_20261001_BUY",
                        timestamp = "2026-10-01 15:15:18",
                        side = "BUY",
                        symbol = "CUBEXTUB",
                        quantity = 1155,
                        fillPrice = 166.10,
                        grossAmount = 191845.50,
                        fees = 227.58,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_GMMPFAUDLR_20261001_SELL",
                        timestamp = "2026-10-01 11:10:50",
                        side = "SELL",
                        symbol = "GMMPFAUDLR",
                        quantity = 121,
                        fillPrice = 1435.32,
                        grossAmount = 173673.50,
                        fees = 195.89,
                        realizedPnl = -9449.13,
                        pnlDifference = "-₹9,449.13"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-30",
                dailyPnl = 1115.27,
                returnPct = 0.18,
                totalNav = 614241.70,
                tradeCount = 2,
                status = "WIN",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK_20260930_BUY",
                        timestamp = "2026-09-30 15:11:12",
                        side = "BUY",
                        symbol = "STLNETWORK",
                        quantity = 4150,
                        fillPrice = 49.20,
                        grossAmount = 204165.46,
                        fees = 242.18,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_TBZ_20260930_SELL",
                        timestamp = "2026-09-30 09:59:22",
                        side = "SELL",
                        symbol = "TBZ",
                        quantity = 53,
                        fillPrice = 622.70,
                        grossAmount = 33003.10,
                        fees = 50.12,
                        realizedPnl = 1115.27,
                        pnlDifference = "+₹1,115.27"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-28",
                dailyPnl = 0.00,
                returnPct = 0.00,
                totalNav = 613126.43,
                tradeCount = 1,
                status = "FLAT",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_GMMPFAUDLR_20260926_BUY",
                        timestamp = "2026-09-28 09:16:05",
                        side = "BUY",
                        symbol = "GMMPFAUDLR",
                        quantity = 121,
                        fillPrice = 1510.00,
                        grossAmount = 182710.00,
                        fees = 216.74,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-25",
                dailyPnl = 4345.07,
                returnPct = 0.71,
                totalNav = 613126.43,
                tradeCount = 3,
                status = "WIN",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK_20260925_SELL",
                        timestamp = "2026-09-25 15:15:20",
                        side = "SELL",
                        symbol = "STLNETWORK",
                        quantity = 855,
                        fillPrice = 54.61,
                        grossAmount = 46691.55,
                        fees = 64.32,
                        realizedPnl = 12215.46,
                        pnlDifference = "+₹12,215.46"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_AHCL_20260925_SELL",
                        timestamp = "2026-09-25 11:36:52",
                        side = "SELL",
                        symbol = "AHCL",
                        quantity = 6903,
                        fillPrice = 27.97,
                        grossAmount = 193110.82,
                        fees = 216.04,
                        realizedPnl = -7870.39,
                        pnlDifference = "-₹7,870.39"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_RAYMOND_20260925_BUY",
                        timestamp = "2026-09-25 11:36:52",
                        side = "BUY",
                        symbol = "RAYMOND",
                        quantity = 168,
                        fillPrice = 1183.36,
                        grossAmount = 198804.50,
                        fees = 235.82,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-24",
                dailyPnl = 8781.36,
                returnPct = 1.46,
                totalNav = 608781.36,
                tradeCount = 2,
                status = "WIN",
                capitalInflow = 490731.20,
                realizedPnl = 8781.36,
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_AHCL_20260924_BUY",
                        timestamp = "2026-09-24 15:27:49",
                        side = "BUY",
                        symbol = "AHCL",
                        quantity = 6903,
                        fillPrice = 29.05,
                        grossAmount = 200527.29,
                        fees = 237.88,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_AHCL_20260924_SELL",
                        timestamp = "2026-09-24 14:38:50",
                        side = "SELL",
                        symbol = "AHCL",
                        quantity = 1472,
                        fillPrice = 28.70,
                        grossAmount = 42246.40,
                        fees = 59.70,
                        realizedPnl = 8781.36,
                        pnlDifference = "+₹8,781.36"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-21",
                dailyPnl = 4007.77,
                returnPct = 3.81,
                totalNav = 109268.80,
                tradeCount = 1,
                status = "WIN",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_TBZ_20260919_BUY_53",
                        timestamp = "2026-09-21 09:26:07",
                        side = "BUY",
                        symbol = "TBZ",
                        quantity = 53,
                        fillPrice = 600.00,
                        grossAmount = 31800.00,
                        fees = 37.71,
                        realizedPnl = 0.00,
                        pnlDifference = "+₹4,007.77"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-18",
                dailyPnl = 2154.20,
                returnPct = 2.09,
                totalNav = 105261.03,
                tradeCount = 2,
                status = "WIN",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_AHCL_20260918_BUY_1472",
                        timestamp = "2026-09-18 09:23:45",
                        side = "BUY",
                        symbol = "AHCL",
                        quantity = 1472,
                        fillPrice = 22.67,
                        grossAmount = 33365.77,
                        fees = 39.57,
                        realizedPnl = 0.00,
                        pnlDifference = "₹0.00"
                    ),
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_TBZ_20260918_BUY_58",
                        timestamp = "2026-09-18 09:11:58",
                        side = "BUY",
                        symbol = "TBZ",
                        quantity = 58,
                        fillPrice = 574.85,
                        grossAmount = 33341.30,
                        fees = 39.54,
                        realizedPnl = 0.00,
                        pnlDifference = "+₹2,154.20"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-17",
                dailyPnl = 3106.83,
                returnPct = 3.10,
                totalNav = 103106.83,
                tradeCount = 2,
                status = "WIN",
                trades = listOf(
                    TransactionItem(
                        transactionId = "TX_ORD_VARALAKSHMI_ALPHA_SCALE_35_STLNETWORK_20260917_BUY_855",
                        timestamp = "2026-09-17 08:41:42",
                        side = "BUY",
                        symbol = "STLNETWORK",
                        quantity = 855,
                        fillPrice = 40.20,
                        grossAmount = 34371.00,
                        fees = 40.77,
                        realizedPnl = 0.00,
                        pnlDifference = "+₹3,106.83"
                    )
                )
            ),
            DailyReturnItem(
                date = "2026-09-16",
                dailyPnl = -450.50,
                returnPct = -0.45,
                totalNav = 100000.00,
                tradeCount = 1,
                status = "LOSS"
            ),
            DailyReturnItem(
                date = "2026-09-15",
                dailyPnl = 1280.00,
                returnPct = 1.29,
                totalNav = 100450.50,
                tradeCount = 2,
                status = "WIN"
            ),
            DailyReturnItem(
                date = "2026-09-14",
                dailyPnl = -820.00,
                returnPct = -0.82,
                totalNav = 99170.50,
                tradeCount = 0,
                status = "LOSS"
            ),
            DailyReturnItem(
                date = "2026-09-11",
                dailyPnl = 0.00,
                returnPct = 0.00,
                totalNav = 99990.50,
                tradeCount = 0,
                status = "FLAT"
            )
        )

        fun normalizeDate(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val epoch = trimmed.toLongOrNull()
            if (epoch != null && epoch > 1_000_000_000L) {
                val millis = if (epoch < 100_000_000_000L) epoch * 1000L else epoch
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                return sdf.format(Date(millis))
            }
            return trimmed.substringBefore("T").substringBefore(" ")
        }

        fun parseDailyReturnsJson(array: JSONArray): List<DailyReturnItem> {
            val list = mutableListOf<DailyReturnItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawDate = obj.optString("date", "")
                    .ifBlank { obj.optString("session_date", "") }
                    .ifBlank { obj.optString("sessionDate", "") }
                    .ifBlank { obj.optString("timestamp", "") }
                    .ifBlank { obj.optString("day", "") }
                    .ifBlank { obj.optString("created_at", "") }
                val date = normalizeDate(rawDate)

                val dailyPnl = when {
                    obj.has("daily_pnl") -> optSafeDouble(obj, "daily_pnl", 0.0)
                    obj.has("dailyPnl") -> optSafeDouble(obj, "dailyPnl", 0.0)
                    obj.has("net_pnl") -> optSafeDouble(obj, "net_pnl", 0.0)
                    obj.has("netPnl") -> optSafeDouble(obj, "netPnl", 0.0)
                    obj.has("pnl") -> optSafeDouble(obj, "pnl", 0.0)
                    else -> 0.0
                }
                val returnPct = when {
                    obj.has("return_pct") -> optSafeDouble(obj, "return_pct", 0.0)
                    obj.has("returnPct") -> optSafeDouble(obj, "returnPct", 0.0)
                    obj.has("pnl_pct") -> optSafeDouble(obj, "pnl_pct", 0.0)
                    obj.has("pnlPct") -> optSafeDouble(obj, "pnlPct", 0.0)
                    obj.has("daily_return_pct") -> optSafeDouble(obj, "daily_return_pct", 0.0)
                    else -> 0.0
                }
                val totalNav = when {
                    obj.has("total_nav") -> optSafeDouble(obj, "total_nav", 0.0)
                    obj.has("totalNav") -> optSafeDouble(obj, "totalNav", 0.0)
                    obj.has("nav") -> optSafeDouble(obj, "nav", 0.0)
                    obj.has("closing_nav") -> optSafeDouble(obj, "closing_nav", 0.0)
                    obj.has("closingNav") -> optSafeDouble(obj, "closingNav", 0.0)
                    else -> 0.0
                }
                val prevNav = totalNav - dailyPnl
                val safeReturnPct = if (returnPct != 0.0) {
                    returnPct
                } else if (dailyPnl != 0.0 && prevNav > 0.0) {
                    roundPaise((dailyPnl / prevNav) * 100.0)
                } else {
                    0.0
                }

                val tradeCount = when {
                    obj.has("trade_count") -> obj.optInt("trade_count", 0)
                    obj.has("tradeCount") -> obj.optInt("tradeCount", 0)
                    obj.has("trades_count") -> obj.optInt("trades_count", 0)
                    obj.has("trades") && obj.optJSONArray("trades") != null -> obj.optJSONArray("trades")!!.length()
                    obj.has("trades") -> obj.optInt("trades", 0)
                    obj.has("tradesCount") -> obj.optInt("tradesCount", 0)
                    obj.has("trade_qty") -> obj.optInt("trade_qty", 0)
                    else -> 0
                }
                val safeTradeCount = if (tradeCount < 0) 0 else tradeCount

                val defaultStatus = when {
                    dailyPnl >= 0.005 -> "WIN"
                    dailyPnl <= -0.005 -> "LOSS"
                    else -> "FLAT"
                }
                val rawStatus = obj.optString("status", "")
                    .ifBlank { obj.optString("day_status", "") }
                    .ifBlank { obj.optString("dayStatus", "") }
                    .uppercase(Locale.US)
                    .trim()
                val status = when (rawStatus) {
                    "WIN", "PROFIT", "GAIN" -> "WIN"
                    "LOSS", "DEFICIT" -> "LOSS"
                    "FLAT", "NEUTRAL", "EVEN", "PRE_INCEPTION", "BREAKEVEN" -> "FLAT"
                    else -> defaultStatus
                }

                val parsedTrades = mutableListOf<TransactionItem>()
                val tradesArray = obj.optJSONArray("trades")
                if (tradesArray != null) {
                    for (j in 0 until tradesArray.length()) {
                        val tObj = tradesArray.optJSONObject(j) ?: continue
                        val tSide = tObj.optString("side", "BUY")
                        val tRealized = optSafeDouble(tObj, "realized_pnl", optSafeDouble(tObj, "realizedPnl", 0.0))
                        val tGross = optSafeDouble(tObj, "gross_amount", optSafeDouble(tObj, "grossAmount", 0.0))
                        val rawFees = optSafeDouble(tObj, "fees", 0.0)
                        val tFees = if (rawFees <= 0.0 || rawFees == 20.0) calculateZerodhaDeliveryFees(tGross, tSide) else rawFees
                        val tFillPrice = optSafeDouble(tObj, "fill_price", optSafeDouble(tObj, "fillPrice", 0.0))
                        val tQuantity = tObj.optInt("quantity", 0)
                        val tDiffStr = tObj.optString("pnlDifference", tObj.optString("pnl_difference", "")).ifBlank {
                            if (tSide.equals("SELL", ignoreCase = true) || tRealized != 0.0) {
                                (if (tRealized >= 0) "+₹" else "-₹") + String.format(Locale.US, "%,.2f", kotlin.math.abs(tRealized))
                            } else {
                                "₹0.00"
                            }
                        }
                        parsedTrades.add(
                            TransactionItem(
                                transactionId = tObj.optString("transaction_id", tObj.optString("transactionId", "")),
                                timestamp = tObj.optString("timestamp", ""),
                                side = tSide,
                                symbol = tObj.optString("symbol", ""),
                                quantity = tQuantity,
                                fillPrice = tFillPrice,
                                grossAmount = tGross,
                                fees = tFees,
                                realizedPnl = tRealized,
                                pnlDifference = tDiffStr
                            )
                        )
                    }
                }

                val capitalInflow = when {
                    obj.has("capital_inflow") -> optSafeDouble(obj, "capital_inflow", 0.0)
                    obj.has("capitalInflow") -> optSafeDouble(obj, "capitalInflow", 0.0)
                    obj.has("inflow") -> optSafeDouble(obj, "inflow", 0.0)
                    obj.has("deposit") -> optSafeDouble(obj, "deposit", 0.0)
                    obj.has("topup") -> optSafeDouble(obj, "topup", 0.0)
                    obj.has("top_up") -> optSafeDouble(obj, "top_up", 0.0)
                    obj.has("withdrawal") -> -optSafeDouble(obj, "withdrawal", 0.0)
                    else -> 0.0
                }
                val realizedPnl = when {
                    obj.has("realized_pnl") -> optSafeDouble(obj, "realized_pnl", dailyPnl)
                    obj.has("realizedPnl") -> optSafeDouble(obj, "realizedPnl", dailyPnl)
                    else -> dailyPnl
                }
                val unrealizedPnlChange = when {
                    obj.has("unrealized_pnl_change") -> optSafeDouble(obj, "unrealized_pnl_change", 0.0)
                    obj.has("unrealizedPnlChange") -> optSafeDouble(obj, "unrealizedPnlChange", 0.0)
                    obj.has("mtm_adjustment") -> optSafeDouble(obj, "mtm_adjustment", 0.0)
                    else -> 0.0
                }

                list.add(
                    DailyReturnItem(
                        date = date,
                        dailyPnl = dailyPnl,
                        returnPct = safeReturnPct,
                        totalNav = totalNav,
                        tradeCount = if (parsedTrades.isNotEmpty()) parsedTrades.size else safeTradeCount,
                        status = status,
                        trades = parsedTrades,
                        capitalInflow = capitalInflow,
                        realizedPnl = realizedPnl,
                        unrealizedPnlChange = unrealizedPnlChange
                    )
                )
            }
            return list
        }

        fun parseDailyReturnsJson(jsonStr: String): List<DailyReturnItem> {
            return try {
                val trimmed = jsonStr.trim()
                val array = when {
                    trimmed.startsWith("{") -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("daily_returns")
                            ?: root.optJSONArray("dailyReturns")
                            ?: root.optJSONArray("daily_pnl")
                            ?: root.optJSONArray("dailyPnl")
                            ?: root.optJSONArray("sessions")
                            ?: root.optJSONArray("history")
                    }
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> null
                } ?: return emptyList()
                parseDailyReturnsJson(array)
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
    private val cachedDailyReturns = createDefaultDailyReturns().toMutableList()
    private var cachedNifty = createDefaultNifty()
    private val cachedMarketIndices = createDefaultMarketIndices().toMutableList()
    private val cachedRecommendations = createDefaultRecommendations().toMutableList()
    private val cachedRecommendationHistory = createDefaultRecommendationHistory().toMutableList()
    private var cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
    private val cachedIpoNotifications = createDefaultIpoNotifications().toMutableList()
    private val cachedOtherPositions = createDefaultOtherPositions().toMutableList()

    fun resetToDefaultSeed() = synchronized(lock) {
        cachedSummary = createDefaultSummary()
        cachedPositions.clear()
        cachedPositions.addAll(createDefaultPositions())
        cachedOtherPositions.clear()
        cachedOtherPositions.addAll(createDefaultOtherPositions())
        cachedTransactions.clear()
        cachedTransactions.addAll(createDefaultTransactions())
        cachedDailyReturns.clear()
        cachedDailyReturns.addAll(createDefaultDailyReturns())
        cachedNifty = createDefaultNifty()
        cachedMarketIndices.clear()
        cachedMarketIndices.addAll(createDefaultMarketIndices())
        cachedRecommendations.clear()
        cachedRecommendations.addAll(createDefaultRecommendations())
        cachedRecommendationHistory.clear()
        cachedRecommendationHistory.addAll(createDefaultRecommendationHistory())
        cachedRecommendationHistorySummary = calculateRecommendationHistorySummary(cachedRecommendationHistory)
        cachedIpoNotifications.clear()
        cachedIpoNotifications.addAll(createDefaultIpoNotifications())
    }

    private var isLoadedFromDisk = false
    private var cachedServerUrl = DEFAULT_SERVER_URL
    private var cachedAuthToken = DEFAULT_AUTH_TOKEN
    private var cachedAutoSyncEnabled = true

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

    fun isAutoSyncEnabled(): Boolean = synchronized(lock) { ensureLoaded(); cachedAutoSyncEnabled }
    fun setAutoSyncEnabled(enabled: Boolean) {
        synchronized(lock) {
            cachedAutoSyncEnabled = enabled
            saveToDisk()
        }
    }

    fun getCachedSummary(): PortfolioSummary = synchronized(lock) { ensureLoaded(); cachedSummary }
    fun getCachedPositions(): List<PositionItem> = synchronized(lock) { ensureLoaded(); cachedPositions.toList() }
    fun getCachedOtherPositions(): List<PositionItem> = synchronized(lock) { ensureLoaded(); cachedOtherPositions.toList() }
    fun getCachedTransactions(): List<TransactionItem> = synchronized(lock) { ensureLoaded(); cachedTransactions.toList() }
    fun getCachedDailyReturns(): List<DailyReturnItem> = synchronized(lock) { ensureLoaded(); cachedDailyReturns.toList() }
    fun getCachedNifty(): MarketIndexItem = synchronized(lock) { ensureLoaded(); cachedNifty }
    fun getCachedMarketIndices(): List<MarketIndexItem> = synchronized(lock) { ensureLoaded(); cachedMarketIndices.toList() }
    fun getCachedRecommendations(): List<StockRecommendationItem> = synchronized(lock) {
        ensureLoaded()
        val heldSymbols = cachedPositions.map { it.symbol.trim().uppercase(Locale.US) }.toSet()
        cachedRecommendations
            .filterNot { it.symbol.trim().uppercase(Locale.US) in heldSymbols }
            .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }
    fun getCachedRecommendationHistory(): List<HistoricalRecommendationItem> = synchronized(lock) { ensureLoaded(); cachedRecommendationHistory.toList() }
    fun getCachedRecommendationHistorySummary(): RecommendationHistorySummary = synchronized(lock) { ensureLoaded(); cachedRecommendationHistorySummary }
    fun getCachedIpoNotifications(): List<IpoActionNotification> = synchronized(lock) {
        ensureLoaded()
        cachedIpoNotifications.filter { it.isDeadlineTodayOrFuture }.toList()
    }

    fun dismissIpoNotification(id: String): List<IpoActionNotification> = synchronized(lock) {
        ensureLoaded()
        val index = cachedIpoNotifications.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = cachedIpoNotifications[index]
            cachedIpoNotifications[index] = old.copy(isDismissed = true)
            saveToDisk()
        }
        val sState = cachedSanthanaState ?: loadSanthanaFromDisk()
        if (sState != null) {
            val updatedAlerts = sState.activeAlerts.map {
                if (it.id == id || it.symbol.equals(id, ignoreCase = true)) it.copy(isDismissed = true) else it
            }
            val newSState = sState.copy(activeAlerts = updatedAlerts)
            cachedSanthanaState = newSState
            saveSanthanaToDisk(newSState)
        }
        cachedIpoNotifications.toList()
    }

    fun setIpoNotificationsForTesting(notifications: List<IpoActionNotification>) = synchronized(lock) {
        cachedIpoNotifications.clear()
        cachedIpoNotifications.addAll(notifications)
    }

    suspend fun syncWithServer(serverBaseUrl: String = getServerUrl(), authToken: String = getAuthToken()): SyncResult =
        refreshData(serverBaseUrl, authToken)

    suspend fun refreshData(serverBaseUrl: String, authToken: String = DEFAULT_AUTH_TOKEN): SyncResult = withContext(Dispatchers.IO) {
        val cleanUrl = serverBaseUrl.trimEnd('/')

        var fetchedPositions: List<PositionItem>? = null
        var fetchedOtherPositions: List<PositionItem>? = null
        var fetchedTransactions: List<TransactionItem>? = null
        var fetchedNifty: MarketIndexItem? = null
        var fetchedMarketIndices: List<MarketIndexItem>? = null
        var fetchedRecommendations: List<StockRecommendationItem>? = null
        var fetchedRecommendationHistory: List<HistoricalRecommendationItem>? = null
        var fetchedIpoNotifications: List<IpoActionNotification>? = null
        var fetchedDailyReturns: List<DailyReturnItem>? = null
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

                val indicesArray = root.optJSONArray("market_indices") ?: root.optJSONArray("indices")
                if (indicesArray != null && indicesArray.length() > 0) {
                    val list = mutableListOf<MarketIndexItem>()
                    for (i in 0 until indicesArray.length()) {
                        val obj = indicesArray.optJSONObject(i) ?: continue
                        list.add(parseMarketIndexItem(obj))
                    }
                    if (list.isNotEmpty()) {
                        fetchedMarketIndices = list
                    }
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

                val ipoArray = root.optJSONArray("ipo_notifications")
                    ?: root.optJSONArray("ipo_alerts")
                    ?: root.optJSONArray("ipo_signals")
                if (ipoArray != null && ipoArray.length() > 0) {
                    val parsedIpos = parseIpoNotificationsJson(root.toString())
                    if (parsedIpos.isNotEmpty()) {
                        fetchedIpoNotifications = parsedIpos
                    }
                }

                val drArray = root.optJSONArray("daily_returns")
                    ?: root.optJSONArray("dailyReturns")
                    ?: root.optJSONArray("daily_pnl")
                    ?: root.optJSONArray("dailyPnl")
                if (drArray != null && drArray.length() > 0) {
                    val parsedDr = parseDailyReturnsJson(drArray)
                    if (parsedDr.isNotEmpty()) {
                        fetchedDailyReturns = parsedDr
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

                val otherArray = root.optJSONArray("other_positions") ?: root.optJSONArray("zerodha_positions")
                if (otherArray != null) {
                    val parsedOthers = mutableListOf<PositionItem>()
                    for (i in 0 until otherArray.length()) {
                        val obj = otherArray.optJSONObject(i) ?: continue
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
                        val prevClose = if (obj.has("previous_close") && !obj.isNull("previous_close")) {
                            obj.optDouble("previous_close", 0.0)
                        } else if (obj.has("prev_close") && !obj.isNull("prev_close")) {
                            obj.optDouble("prev_close", 0.0)
                        } else 0.0

                        val effectivePrevClose = if (prevClose > 0.0) prevClose else entryPrice
                        val isBoughtToday = if (obj.has("is_bought_today")) obj.optBoolean("is_bought_today") else isPositionBoughtToday(entryDate)
                        val refPrice = if (isBoughtToday && entryPrice > 0.0) entryPrice else calculateReferencePrice(entryDate, entryPrice, effectivePrevClose)

                        val todayChg = if (refPrice > 0.0 && kotlin.math.abs(currentPrice - refPrice) > 0.0001) {
                            roundPaise(currentPrice - refPrice)
                        } else if (obj.has("today_price_change") && !obj.isNull("today_price_change")) {
                            roundPaise(obj.optDouble("today_price_change", 0.0))
                        } else {
                            0.0
                        }

                        val todayChgPct = if (refPrice > 0.0 && kotlin.math.abs(currentPrice - refPrice) > 0.0001) {
                            roundPaise(((currentPrice - refPrice) / refPrice) * 100.0)
                        } else if (obj.has("today_price_change_pct") && !obj.isNull("today_price_change_pct")) {
                            roundPaise(obj.optDouble("today_price_change_pct", 0.0))
                        } else {
                            0.0
                        }

                        val todayValChg = if (obj.has("today_value_change") && !obj.isNull("today_value_change") && kotlin.math.abs(obj.optDouble("today_value_change", 0.0)) > 0.0001) {
                            roundPaise(obj.optDouble("today_value_change", 0.0))
                        } else {
                            roundPaise(quantity * todayChg)
                        }

                        parsedOthers.add(
                            PositionItem(
                                positionId = obj.optString("position_id", "ZERODHA_${symbol}"),
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
                                isProfitTargetEnabled = false,
                                isStrategy = false,
                                category = obj.optString("category", "ZERODHA")
                            )
                        )
                    }
                    fetchedOtherPositions = parsedOthers
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
                                fees = run {
                                    val gross = obj.optDouble("gross_amount", 0.0)
                                    val raw = obj.optDouble("fees", 0.0)
                                    if (raw <= 0.0 || raw == 20.0) calculateZerodhaDeliveryFees(gross, side) else raw
                                },
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
        var fetchedReservedCapital: Double? = null
        var fetchedRealizedPnl: Double? = null
        var fetchedCagrPct: Double? = null
        var fetchedInceptionDate: String? = null

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
                            if (obj.has("reserved_capital") && !obj.isNull("reserved_capital")) {
                                fetchedReservedCapital = roundPaise(obj.optDouble("reserved_capital", 0.0))
                            }
                            if (obj.has("realized_pnl") && !obj.isNull("realized_pnl")) {
                                fetchedRealizedPnl = roundPaise(obj.optDouble("realized_pnl", 0.0))
                            }
                            if (obj.has("current_cagr") && !obj.isNull("current_cagr")) {
                                fetchedCagrPct = roundPaise(obj.optDouble("current_cagr", 0.0))
                            } else if (obj.has("cagr_pct") && !obj.isNull("cagr_pct")) {
                                fetchedCagrPct = roundPaise(obj.optDouble("cagr_pct", 0.0))
                            }
                            if (obj.has("created_at") && !obj.isNull("created_at")) {
                                fetchedInceptionDate = obj.optString("created_at", "2026-09-17")
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

        if (fetchedDailyReturns == null) {
            try {
                val drUrl = "$cleanUrl/api/live-trading/daily-returns?strategy=VARALAKSHMI_ALPHA_SCALE_35$tokenQuery"
                val drJson = httpGet(drUrl, authToken)
                if (drJson != null) {
                    val parsed = parseDailyReturnsJson(drJson)
                    if (parsed.isNotEmpty()) {
                        fetchedDailyReturns = parsed
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep cached daily returns
            }
        }

        synchronized(lock) {
            if (fetchedNifty != null) {
                cachedNifty = fetchedNifty
            }

            if (fetchedMarketIndices != null && fetchedMarketIndices.isNotEmpty()) {
                cachedMarketIndices.clear()
                cachedMarketIndices.addAll(fetchedMarketIndices)
                cachedNifty = fetchedMarketIndices[0]
            } else if (fetchedNifty != null) {
                if (cachedMarketIndices.isNotEmpty()) {
                    cachedMarketIndices[0] = fetchedNifty
                } else {
                    cachedMarketIndices.add(fetchedNifty)
                }
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

            if (fetchedIpoNotifications != null) {
                // Filter out legacy sample alert and only keep today or future deadlines
                val validFetched = fetchedIpoNotifications
                    .filterNot { it.id == "IPO-ALERT-SANTHANA-001" }
                    .filter { it.isDeadlineTodayOrFuture }
                val dismissedIds = cachedIpoNotifications.filter { it.isDismissed }.map { it.id }.toSet()
                cachedIpoNotifications.clear()
                cachedIpoNotifications.addAll(
                    validFetched.map {
                        if (it.id in dismissedIds) it.copy(isDismissed = true) else it
                    }
                )
            }

            if (fetchedDailyReturns != null && fetchedDailyReturns.isNotEmpty()) {
                cachedDailyReturns.clear()
                cachedDailyReturns.addAll(fetchedDailyReturns)
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
                val reserved = fetchedReservedCapital ?: cachedSummary.reservedCapital
                val available = fetchedAvailableCapital ?: roundPaise((allocated - deployed - reserved + realized).coerceAtLeast(0.0))
                val nav = roundPaise(deployed + available + reserved + totalUnrealized)
                val totalPnl = roundPaise(realized + totalUnrealized)
                val totalPnlPct = if (allocated > 0) (totalPnl / allocated) * 100.0 else 0.0

                val todayPnl = roundPaise(cachedPositions.sumOf { it.todayValueChange })
                val prevNav = nav - todayPnl
                val todayPnlPct = if (prevNav > 0.0) roundPaise((todayPnl / prevNav) * 100.0) else if (allocated > 0.0) roundPaise((todayPnl / allocated) * 100.0) else 0.0
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val inceptDate = fetchedInceptionDate ?: cachedSummary.inceptionDate
                val computedCagr = fetchedCagrPct ?: calculateCurrentCagr(nav, allocated, inceptDate, nowStr)

                cachedSummary = cachedSummary.copy(
                    totalNav = nav,
                    allocatedCapital = allocated,
                    deployedCapital = deployed,
                    availableCapital = available,
                    reservedCapital = reserved,
                    realizedPnl = realized,
                    unrealizedPnl = totalUnrealized,
                    totalPnl = totalPnl,
                    totalPnlPct = totalPnlPct,
                    todayPnl = todayPnl,
                    todayPnlPct = todayPnlPct,
                    activeSlots = cachedPositions.size,
                    lastUpdated = nowStr,
                    cagrPct = computedCagr,
                    inceptionDate = inceptDate
                )
                if (fetchedOtherPositions != null) {
                    cachedOtherPositions.clear()
                    cachedOtherPositions.addAll(fetchedOtherPositions)
                }

                saveToDisk()

                val heldSymbols = cachedPositions.map { it.symbol.trim().uppercase(Locale.US) }.toSet()
                val cleanRecommendations = cachedRecommendations
                    .filterNot { it.symbol.trim().uppercase(Locale.US) in heldSymbols }
                    .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

                SyncResult.Success(
                    summary = cachedSummary,
                    positions = cachedPositions.toList(),
                    transactions = cachedTransactions.toList(),
                    dailyReturns = cachedDailyReturns.toList(),
                    nifty = cachedNifty,
                    marketIndices = cachedMarketIndices.toList(),
                    recommendations = cleanRecommendations,
                    recommendationHistory = cachedRecommendationHistory.toList(),
                    recommendationHistorySummary = cachedRecommendationHistorySummary,
                    ipoNotifications = cachedIpoNotifications.toList(),
                    otherPositions = cachedOtherPositions.toList()
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
                    dailyReturns = cachedDailyReturns.toList(),
                    nifty = cachedNifty,
                    marketIndices = cachedMarketIndices.toList(),
                    recommendations = cleanRecommendations,
                    recommendationHistory = cachedRecommendationHistory.toList(),
                    recommendationHistorySummary = cachedRecommendationHistorySummary,
                    ipoNotifications = cachedIpoNotifications.toList(),
                    otherPositions = cachedOtherPositions.toList()
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
                val reserved = cachedSummary.reservedCapital
                // Invariant: NAV = Deployed + Available + Reserved + TotalUnrealized
                val nav = roundPaise(newDeployedCapital + newAvailableCapital + reserved + totalUnrealized)
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
                    reservedCapital = reserved,
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

    fun restorePosition(position: PositionItem): Triple<PortfolioSummary, List<PositionItem>, List<TransactionItem>> = synchronized(lock) {
        ensureLoaded()
        if (cachedPositions.none { it.symbol.equals(position.symbol, ignoreCase = true) }) {
            cachedPositions.add(position)
            cachedTransactions.removeAll { it.side == "SELL" && it.symbol.equals(position.symbol, ignoreCase = true) }

            val proceeds = roundPaise(position.marketValue)
            val realizedGain = roundPaise(position.unrealizedPnl)
            val costBasis = roundPaise(position.marketValue - position.unrealizedPnl)

            val newRealizedPnl = roundPaise(cachedSummary.realizedPnl - realizedGain)
            val newAvailableCapital = roundPaise((cachedSummary.availableCapital - proceeds).coerceAtLeast(0.0))
            val newDeployedCapital = roundPaise(cachedSummary.deployedCapital + costBasis)
            val totalUnrealized = roundPaise(cachedPositions.sumOf { it.unrealizedPnl })
            val reserved = cachedSummary.reservedCapital
            val nav = roundPaise(newDeployedCapital + newAvailableCapital + reserved + totalUnrealized)
            val allocated = cachedSummary.allocatedCapital
            val totalPnl = roundPaise(newRealizedPnl + totalUnrealized)
            val totalPnlPct = if (allocated > 0) (totalPnl / allocated) * 100.0 else 0.0
            val totalTodayPnl = roundPaise(cachedPositions.sumOf { it.todayValueChange })
            val prevNav = nav - totalTodayPnl
            val todayPnlPct = if (prevNav > 0.0) roundPaise((totalTodayPnl / prevNav) * 100.0) else 0.0

            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

            cachedSummary = cachedSummary.copy(
                totalNav = nav,
                deployedCapital = newDeployedCapital,
                availableCapital = newAvailableCapital,
                reservedCapital = reserved,
                realizedPnl = newRealizedPnl,
                unrealizedPnl = totalUnrealized,
                totalPnl = totalPnl,
                totalPnlPct = totalPnlPct,
                todayPnl = totalTodayPnl,
                todayPnlPct = todayPnlPct,
                activeSlots = cachedPositions.size,
                lastUpdated = nowStr
            )
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
                put("confirmation_token", "CONFIRMED_BY_USER")
            }
            val res = httpPost(endpoint, payload.toString(), authToken)
            if (res != null) {
                val json = JSONObject(res)
                val success = json.optBoolean("success", false)
                val zerodhaId = json.optString("zerodha_order_id", "")
                val clientOrderId = json.optString("client_order_id", "")
                val orderLabel = when {
                    zerodhaId.isNotBlank() -> "Zerodha Order ID: $zerodhaId"
                    clientOrderId.isNotBlank() -> "Client Order: $clientOrderId"
                    else -> ""
                }
                val defaultSuccessMsg = if (orderLabel.isNotBlank()) {
                    "Live exit order submitted to Zerodha Kite ($orderLabel)"
                } else {
                    "Live exit order submitted to Zerodha Kite"
                }
                val msg = json.optString("message", defaultSuccessMsg)
                if (success) {
                    removePosition(symbol)
                    Result.success(msg)
                } else {
                    val err = json.optString("reason", json.optString("error", json.optString("message", "Exit order rejected by broker / risk gateway")))
                    val status = json.optString("status")
                    if (status == "CLOSED" || status == "NOT_FOUND" || err.contains("already closed", ignoreCase = true) || err.contains("No open position", ignoreCase = true)) {
                        removePosition(symbol)
                        Result.success("Position already closed on server")
                    } else {
                        // Strict safety: DO NOT remove position locally when broker order fails or is rejected
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
                val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!errText.isNullOrBlank()) {
                    errText
                } else {
                    null
                }
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
            root.put("autoSyncEnabled", cachedAutoSyncEnabled)
            val summaryObj = JSONObject().apply {
                put("strategyId", cachedSummary.strategyId)
                put("strategyName", cachedSummary.strategyName)
                put("formula", cachedSummary.formula)
                put("status", cachedSummary.status)
                put("totalNav", cachedSummary.totalNav)
                put("allocatedCapital", cachedSummary.allocatedCapital)
                put("deployedCapital", cachedSummary.deployedCapital)
                put("availableCapital", cachedSummary.availableCapital)
                put("reservedCapital", cachedSummary.reservedCapital)
                put("realizedPnl", cachedSummary.realizedPnl)
                put("unrealizedPnl", cachedSummary.unrealizedPnl)
                put("totalPnl", cachedSummary.totalPnl)
                put("totalPnlPct", cachedSummary.totalPnlPct)
                put("todayPnl", cachedSummary.todayPnl)
                put("todayPnlPct", cachedSummary.todayPnlPct)
                put("activeSlots", cachedSummary.activeSlots)
                put("maxSlots", cachedSummary.maxSlots)
                put("lastUpdated", cachedSummary.lastUpdated)
                put("cagrPct", cachedSummary.cagrPct)
                put("inceptionDate", cachedSummary.inceptionDate)
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

            val drArray = JSONArray()
            val dailyReturnsSnapshot = synchronized(lock) { cachedDailyReturns.toList() }
            for (dr in dailyReturnsSnapshot) {
                val drObj = JSONObject().apply {
                    put("date", dr.date)
                    put("daily_pnl", dr.dailyPnl)
                    put("return_pct", dr.returnPct)
                    put("total_nav", dr.totalNav)
                    put("trade_count", dr.tradeCount)
                    put("status", dr.status)
                    val tradesArray = JSONArray()
                    for (t in dr.trades) {
                        val tObj = JSONObject().apply {
                            put("transactionId", t.transactionId)
                            put("transaction_id", t.transactionId)
                            put("timestamp", t.timestamp)
                            put("side", t.side)
                            put("symbol", t.symbol)
                            put("quantity", t.quantity)
                            put("fillPrice", t.fillPrice)
                            put("fill_price", t.fillPrice)
                            put("grossAmount", t.grossAmount)
                            put("gross_amount", t.grossAmount)
                            put("fees", t.fees)
                            put("realizedPnl", t.realizedPnl)
                            put("realized_pnl", t.realizedPnl)
                            put("pnlDifference", t.pnlDifference)
                        }
                        tradesArray.put(tObj)
                    }
                    put("trades", tradesArray)
                }
                drArray.put(drObj)
            }
            root.put("daily_returns", drArray)

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

            val indicesArray = JSONArray()
            for (idx in cachedMarketIndices) {
                val idxObj = JSONObject().apply {
                    put("symbol", idx.symbol)
                    put("ltp", idx.ltp)
                    put("change", idx.change)
                    put("changePct", idx.changePct)
                    put("open", idx.open)
                    put("high", idx.high)
                    put("low", idx.low)
                    put("previousClose", idx.previousClose)
                    put("timestamp", idx.timestamp)
                    put("status", idx.status)
                }
                indicesArray.put(idxObj)
            }
            root.put("market_indices", indicesArray)

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

            val ipoArray = JSONArray()
            for (ipo in cachedIpoNotifications) {
                val ipoObj = JSONObject().apply {
                    put("id", ipo.id)
                    put("symbol", ipo.symbol)
                    put("company_name", ipo.companyName)
                    put("action_type", ipo.actionType.name)
                    put("headline", ipo.headline)
                    put("message", ipo.message)
                    put("lot_price", ipo.lotPrice)
                    put("lot_quantity", ipo.lotQuantity)
                    put("lot_shares", ipo.lotShares)
                    put("cutoff_price", ipo.cutoffPrice)
                    put("qib_multiple", ipo.qibMultiple)
                    put("close_deadline", ipo.closeDeadline)
                    put("urgency", ipo.urgency)
                    put("timestamp", ipo.timestamp)
                    put("deep_link_url", ipo.deepLinkUrl)
                    put("is_dismissed", ipo.isDismissed)
                }
                ipoArray.put(ipoObj)
            }
            root.put("ipo_notifications", ipoArray)

            val otherArray = JSONArray()
            val otherPositionsSnapshot = synchronized(lock) { cachedOtherPositions.toList() }
            for (p in otherPositionsSnapshot) {
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
                    put("isStrategy", p.isStrategy)
                    put("category", p.category)
                }
                otherArray.put(pObj)
            }
            root.put("other_positions", otherArray)

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
                    reservedCapital = optSafeDouble(summaryObj, "reservedCapital", cachedSummary.reservedCapital),
                    realizedPnl = optSafeDouble(summaryObj, "realizedPnl", cachedSummary.realizedPnl),
                    unrealizedPnl = optSafeDouble(summaryObj, "unrealizedPnl", cachedSummary.unrealizedPnl),
                    totalPnl = optSafeDouble(summaryObj, "totalPnl", cachedSummary.totalPnl),
                    totalPnlPct = optSafeDouble(summaryObj, "totalPnlPct", cachedSummary.totalPnlPct),
                    todayPnl = optSafeDouble(summaryObj, "todayPnl", cachedSummary.todayPnl),
                    todayPnlPct = optSafeDouble(summaryObj, "todayPnlPct", cachedSummary.todayPnlPct),
                    activeSlots = summaryObj.optInt("activeSlots", cachedSummary.activeSlots),
                    maxSlots = summaryObj.optInt("maxSlots", cachedSummary.maxSlots),
                    lastUpdated = summaryObj.optString("lastUpdated", cachedSummary.lastUpdated),
                    cagrPct = optSafeDouble(
                        summaryObj, "cagrPct",
                        optSafeDouble(summaryObj, "cagr_pct", optSafeDouble(summaryObj, "current_cagr", cachedSummary.cagrPct))
                    ),
                    inceptionDate = summaryObj.optString("inceptionDate", summaryObj.optString("inception_date", cachedSummary.inceptionDate))
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
                            fees = run {
                                val gross = optSafeDouble(tx, "grossAmount", if (tx.has("gross_amount")) optSafeDouble(tx, "gross_amount", 0.0) else 0.0)
                                val raw = optSafeDouble(tx, "fees", 0.0)
                                if (raw <= 0.0 || raw == 20.0) calculateZerodhaDeliveryFees(gross, side) else raw
                            },
                            realizedPnl = realized,
                            pnlDifference = pnlDiff
                        )
                    )
                }
                list
            } else null

            val niftyObj = root.optJSONObject("nifty")
            val parsedNifty = if (niftyObj != null) {
                parseMarketIndexItem(niftyObj, cachedNifty)
            } else null

            val indicesArray = root.optJSONArray("market_indices")
                ?: root.optJSONArray("indices")
            val parsedIndices = if (indicesArray != null && indicesArray.length() > 0) {
                val list = mutableListOf<MarketIndexItem>()
                for (i in 0 until indicesArray.length()) {
                    val obj = indicesArray.optJSONObject(i) ?: continue
                    list.add(parseMarketIndexItem(obj))
                }
                list
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

            val ipoArray = root.optJSONArray("ipo_notifications")
                ?: root.optJSONArray("ipo_alerts")
                ?: root.optJSONArray("ipo_signals")
            val parsedIpos = if (ipoArray != null && ipoArray.length() > 0) {
                parseIpoNotificationsJson(root.toString())
            } else null

            val drArray = root.optJSONArray("daily_returns")
                ?: root.optJSONArray("dailyReturns")
                ?: root.optJSONArray("daily_pnl")
                ?: root.optJSONArray("dailyPnl")
            val parsedDailyReturns = if (drArray != null && drArray.length() > 0) {
                parseDailyReturnsJson(drArray)
            } else null

            if (root.has("serverUrl") && !root.isNull("serverUrl")) {
                val url = root.optString("serverUrl", "").trim()
                if (url.isNotBlank() && !url.contains("10.0.2.2") && !url.contains("localhost") && !url.contains("127.0.0.1")) {
                    cachedServerUrl = url
                } else {
                    cachedServerUrl = DEFAULT_SERVER_URL
                }
            }
            if (root.has("authToken") && !root.isNull("authToken")) {
                val token = root.optString("authToken", "").trim()
                if (token.isNotBlank()) cachedAuthToken = token
            }
            if (root.has("autoSyncEnabled")) {
                cachedAutoSyncEnabled = root.optBoolean("autoSyncEnabled", true)
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
            if (parsedIndices != null && parsedIndices.isNotEmpty()) {
                cachedMarketIndices.clear()
                cachedMarketIndices.addAll(parsedIndices)
                cachedNifty = parsedIndices[0]
            } else if (parsedNifty != null) {
                cachedNifty = parsedNifty
                if (cachedMarketIndices.isNotEmpty()) {
                    cachedMarketIndices[0] = parsedNifty
                }
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
            if (parsedIpos != null) {
                // Filter out legacy sample alert and only keep today or future deadlines
                val cleanIpos = parsedIpos
                    .filterNot { it.id == "IPO-ALERT-SANTHANA-001" }
                    .filter { it.isDeadlineTodayOrFuture }
                cachedIpoNotifications.clear()
                cachedIpoNotifications.addAll(cleanIpos)
            }
            if (parsedDailyReturns != null && parsedDailyReturns.isNotEmpty()) {
                val isLegacyReturnsCache = parsedDailyReturns.size == 7 &&
                    parsedDailyReturns.firstOrNull()?.date == "2026-09-21" &&
                    parsedDailyReturns.all { it.trades.isEmpty() }
                cachedDailyReturns.clear()
                if (!isLegacyReturnsCache) {
                    cachedDailyReturns.addAll(parsedDailyReturns)
                } else {
                    cachedDailyReturns.addAll(createDefaultDailyReturns())
                }
            }

            val cachedOthersArray = root.optJSONArray("other_positions") ?: root.optJSONArray("zerodha_positions")
            if (cachedOthersArray != null) {
                val parsedOthersList = mutableListOf<PositionItem>()
                for (i in 0 until cachedOthersArray.length()) {
                    val p = cachedOthersArray.optJSONObject(i) ?: continue
                    val symbol = p.optString("symbol", "").trim().uppercase(Locale.US)
                    if (symbol.isEmpty()) continue
                    val quantity = p.optInt("quantity", 0)
                    val entryPrice = optSafeDouble(p, "entryPrice", optSafeDouble(p, "entry_price", 0.0))
                    val currentPrice = optSafeDouble(p, "currentPrice", optSafeDouble(p, "current_price", 0.0))
                    val entryDate = p.optString("entryDate", p.optString("entry_date", ""))
                    val prevClose = optSafeDouble(p, "previousClose", optSafeDouble(p, "previous_close", optSafeDouble(p, "prev_close", 0.0)))
                    val effectivePrevClose = if (prevClose > 0.0) prevClose else entryPrice
                    val isBoughtToday = if (p.has("isBoughtToday")) p.optBoolean("isBoughtToday") else (if (p.has("is_bought_today")) p.optBoolean("is_bought_today") else isPositionBoughtToday(entryDate))
                    val refPrice = if (isBoughtToday && entryPrice > 0.0) entryPrice else if (p.has("referencePrice")) optSafeDouble(p, "referencePrice", calculateReferencePrice(entryDate, entryPrice, effectivePrevClose)) else calculateReferencePrice(entryDate, entryPrice, effectivePrevClose)
                    val todayChg = if (p.has("todayPriceChange")) optSafeDouble(p, "todayPriceChange", 0.0) else (if (refPrice > 0.0 && kotlin.math.abs(currentPrice - refPrice) > 0.0001) roundPaise(currentPrice - refPrice) else 0.0)
                    val todayChgPct = if (p.has("todayPriceChangePct")) optSafeDouble(p, "todayPriceChangePct", 0.0) else (if (refPrice > 0.0 && kotlin.math.abs(currentPrice - refPrice) > 0.0001) roundPaise(((currentPrice - refPrice) / refPrice) * 100.0) else 0.0)
                    val todayValChg = if (p.has("todayValueChange")) optSafeDouble(p, "todayValueChange", 0.0) else roundPaise(quantity * todayChg)
                    val marketValue = optSafeDouble(p, "marketValue", optSafeDouble(p, "market_value", roundPaise(quantity * currentPrice)))
                    val unrealizedPnl = optSafeDouble(p, "unrealizedPnl", optSafeDouble(p, "unrealized_pnl", roundPaise(marketValue - (quantity * entryPrice))))
                    val unrealizedPnlPct = optSafeDouble(p, "unrealizedPnlPct", optSafeDouble(p, "unrealized_pnl_pct", if (entryPrice > 0.0) roundPaise(((currentPrice - entryPrice) / entryPrice) * 100.0) else 0.0))
                    val peakPrice = optSafeDouble(p, "peakPrice", optSafeDouble(p, "peak_price", currentPrice))

                    parsedOthersList.add(
                        PositionItem(
                            positionId = p.optString("positionId", p.optString("position_id", "ZERODHA_$symbol")),
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
                            isProfitTargetEnabled = false,
                            isStrategy = false,
                            category = p.optString("category", "ZERODHA")
                        )
                    )
                }
                if (parsedOthersList.isNotEmpty()) {
                    cachedOtherPositions.clear()
                    cachedOtherPositions.addAll(parsedOthersList)
                }
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

    // =========================================================================
    // VEERALAKSHMI F&O PAPER TRADING REPOSITORY
    // =========================================================================

    private var cachedFnoState: VeeraLakshmiUiState? = null

    fun getCachedFnoState(): VeeraLakshmiUiState {
        synchronized(lock) {
            if (cachedFnoState != null) return cachedFnoState!!
            val fromDisk = loadFnoFromDisk()
            if (fromDisk != null) {
                cachedFnoState = fromDisk
                return fromDisk
            }
            val defaultState = createDefaultVeeraLakshmiState()
            cachedFnoState = defaultState
            return defaultState
        }
    }

    fun selectFnoInstance(instanceId: String): VeeraLakshmiUiState {
        synchronized(lock) {
            val current = getCachedFnoState()
            val updated = current.copy(
                selectedInstanceId = instanceId
            )
            cachedFnoState = updated
            return updated
        }
    }

    suspend fun syncFno(
        serverUrl: String,
        selectedInstanceId: String = "10L",
        authToken: String? = null
    ): VeeraLakshmiUiState = withContext(Dispatchers.IO) {
        var sUrl = serverUrl.trim().trimEnd('/')
        if (sUrl.isBlank()) {
            sUrl = DEFAULT_SERVER_URL
        }
        var instancesMap: Map<String, FnoInstanceSummary> = emptyMap()
        var positionsList: List<FnoPositionItem> = emptyList()
        var dailyPnlList: List<FnoDailyPnlItem> = emptyList()
        var statusInfo = FnoStatusInfo()
        var isLive = false
        var errorMsg: String? = null
        var positionsJson: String? = null
        var instancesJson: String? = null
        var statusJson: String? = null
        var dailyPnlJson: String? = null

        val fetchEndpoints: suspend (String) -> Boolean = { url ->
            try {
                coroutineScope {
                    val instancesDeferred = async { httpGet("$url/api/fno/instances", authToken) }
                    val positionsDeferred = async { httpGet("$url/api/fno/positions?instance=$selectedInstanceId", authToken) }
                    val statusDeferred = async { httpGet("$url/api/fno/status", authToken) }
                    val dailyPnlDeferred = async { httpGet("$url/api/fno/daily-pnl?instance=$selectedInstanceId", authToken) }

                    val instJ = instancesDeferred.await()
                    val posJ = positionsDeferred.await()
                    val statJ = statusDeferred.await()
                    val dailyJ = dailyPnlDeferred.await()

                    var ok = false
                    if (!instJ.isNullOrBlank()) {
                        instancesMap = parseFnoInstances(instJ)
                        instancesJson = instJ
                        ok = true
                    }
                    if (!posJ.isNullOrBlank()) {
                        positionsList = parseFnoPositions(posJ)
                        positionsJson = posJ
                        ok = true
                    }
                    if (!statJ.isNullOrBlank()) {
                        statusInfo = parseFnoStatus(statJ)
                        statusJson = statJ
                    }
                    if (!dailyJ.isNullOrBlank()) {
                        dailyPnlList = parseFnoDailyPnl(dailyJ)
                        dailyPnlJson = dailyJ
                    }
                    ok
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                false
            }
        }

        try {
            isLive = fetchEndpoints(sUrl)
            if (!isLive && sUrl.contains("10.0.2.2")) {
                isLive = fetchEndpoints(DEFAULT_SERVER_URL)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            errorMsg = e.message ?: "Failed to sync F&O data"
        }

        val newState = if (isLive && instancesMap.isNotEmpty()) {
            val posJson = positionsJson
            val closedFromPositions = if (!posJson.isNullOrBlank()) parseFnoClosedTrades(posJson) else emptyList()
            val tradesFromDaily = dailyPnlList.firstOrNull { it.isToday }?.trades?.takeIf { it.isNotEmpty() }
            val latestDailyTrades = dailyPnlList.filter { it.trades.isNotEmpty() }.maxByOrNull { it.date }?.trades
            val todayTrades = tradesFromDaily
                ?: closedFromPositions.takeIf { it.isNotEmpty() }
                ?: latestDailyTrades
                ?: emptyList()

            val state = VeeraLakshmiUiState(
                selectedInstanceId = selectedInstanceId,
                instances = instancesMap,
                activePositions = positionsList,
                dailyPnlHistory = dailyPnlList,
                statusInfo = statusInfo,
                isLoading = false,
                isLiveSync = true,
                errorMessage = null,
                lastSyncTimestamp = System.currentTimeMillis(),
                todayTrades = todayTrades
            )
            saveFnoToDisk(state)
            state
        } else {
            val loaded = loadFnoFromDisk() ?: createDefaultVeeraLakshmiState()
            val tradesFromDaily = loaded.dailyPnlHistory.firstOrNull { it.isToday }?.trades?.takeIf { it.isNotEmpty() }
            val latestDailyTrades = loaded.dailyPnlHistory.filter { it.trades.isNotEmpty() }.maxByOrNull { it.date }?.trades
            val fallbackTrades = when {
                loaded.todayTrades.isNotEmpty() -> loaded.todayTrades
                tradesFromDaily != null -> tradesFromDaily
                latestDailyTrades != null -> latestDailyTrades
                else -> emptyList()
            }
            loaded.copy(
                selectedInstanceId = selectedInstanceId,
                isLoading = false,
                isLiveSync = false,
                errorMessage = errorMsg,
                todayTrades = fallbackTrades
            )
        }

        synchronized(lock) {
            cachedFnoState = newState
        }
        newState
    }

    fun parseFnoInstances(jsonStr: String): Map<String, FnoInstanceSummary> {
        val result = mutableMapOf<String, FnoInstanceSummary>()
        try {
            val root = JSONObject(jsonStr)
            val instancesObj = root.optJSONObject("instances") ?: return result
            val keys = instancesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = instancesObj.optJSONObject(key) ?: continue
                val allocated = optSafeDouble(obj, "allocated_capital", 1000000.0)
                val unrealized = optSafeDouble(obj, "unrealized_pnl", 0.0)
                val realized = optSafeDouble(obj, "realized_pnl", optSafeDouble(obj, "realized_pnl_today", 0.0))
                val totalPnl = if (obj.has("total_pnl")) optSafeDouble(obj, "total_pnl", unrealized + realized) else (unrealized + realized)
                val effectivePnl = if (totalPnl != 0.0) totalPnl else unrealized
                val summary = FnoInstanceSummary(
                    instanceId = obj.optString("instance_id", key),
                    name = obj.optString("name", "VeeraLakshmi $key"),
                    allocatedCapital = allocated,
                    marginBlocked = optSafeDouble(obj, "margin_blocked", 0.0),
                    unrealizedPnl = effectivePnl,
                    cashBuffer = optSafeDouble(obj, "cash_buffer", 0.0),
                    marginUtilizationPct = optSafeDouble(obj, "margin_utilization_pct", 0.0),
                    positionsCount = obj.optInt("positions_count", 0),
                    asOfDate = obj.optString("as_of_date", "2026-09-28")
                )
                result[key] = summary
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseFnoClosedTrades(jsonStr: String): List<FnoHistoricalTrade> {
        val result = mutableListOf<FnoHistoricalTrade>()
        try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("closed_trades") ?: root.optJSONArray("trades") ?: return result
            for (i in 0 until arr.length()) {
                val tObj = arr.optJSONObject(i) ?: continue
                val exitTimestamp = tObj.optString("exit_timestamp", "")
                val exitDate = tObj.optString("exit_date", if (exitTimestamp.length >= 10) exitTimestamp.substring(0, 10) else root.optString("as_of_date", ""))
                val exitTime = if (exitTimestamp.length >= 16) exitTimestamp.substring(11, 16) else tObj.optString("exit_time", tObj.optString("time", "15:15"))
                val symbol = tObj.optString("tradingsymbol", tObj.optString("symbol", ""))
                val direction = tObj.optString("direction", "SHORT")
                val action = if (tObj.has("action")) tObj.optString("action") else (if (direction.equals("SHORT", ignoreCase = true)) "BUY" else "SELL")
                val instType = tObj.optString("instrument_type", if (symbol.endsWith("PE") || symbol.endsWith("CE")) "OPT" else "FUT")

                result.add(
                    FnoHistoricalTrade(
                        tradeId = tObj.optString("trade_id", "trd_$i"),
                        time = exitTime,
                        symbol = symbol,
                        action = action,
                        instrumentType = instType,
                        lots = tObj.optInt("lots", 1),
                        quantity = if (tObj.has("closed_quantity")) tObj.optInt("closed_quantity") else tObj.optInt("quantity", 0),
                        executionPrice = optSafeDouble(tObj, "exit_price", optSafeDouble(tObj, "execution_price", 0.0)),
                        realizedPnl = optSafeDouble(tObj, "net_pnl", optSafeDouble(tObj, "realized_pnl", 0.0)),
                        executionReason = tObj.optString("exit_reason", tObj.optString("execution_reason", "")),
                        costs = optSafeDouble(tObj, "total_friction", optSafeDouble(tObj, "costs", 0.0)),
                        date = exitDate
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseFnoPositions(jsonStr: String): List<FnoPositionItem> {
        val result = mutableListOf<FnoPositionItem>()
        try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("positions") ?: return result
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val item = FnoPositionItem(
                    positionId = obj.optString("position_id", "pos_$i"),
                    strategyEngine = obj.optString("strategy_engine", "IntradayStraddleEngine"),
                    symbol = obj.optString("symbol", obj.optString("tradingsymbol", "NIFTY_FUT")),
                    instrumentType = obj.optString("instrument_type", "OPT"),
                    direction = obj.optString("direction", "SHORT"),
                    quantity = obj.optInt("quantity", 1),
                    lots = obj.optInt("lots", 1),
                    entryPrice = optSafeDouble(obj, "entry_price", 0.0),
                    currentPrice = optSafeDouble(obj, "current_price", 0.0),
                    marginRequired = optSafeDouble(obj, "margin_required", optSafeDouble(obj, "margin_blocked", 0.0)),
                    stopLossPrice = optSafeDouble(obj, "stop_loss_price", 0.0),
                    unrealizedPnl = optSafeDouble(obj, "unrealized_pnl", optSafeDouble(obj, "pnl", 0.0)),
                    expiryDate = obj.optString("expiry_date", obj.optString("expiry", "")),
                    strikePrice = optSafeDouble(obj, "strike_price", 0.0),
                    optionType = if (obj.has("option_type") && !obj.isNull("option_type")) obj.optString("option_type") else null,
                    entryReason = obj.optString("entry_reason", obj.optString("reason", obj.optString("rationale", ""))),
                    targetPrice = optSafeDouble(obj, "target_price", 0.0),
                    signalTrigger = obj.optString("signal_trigger", obj.optString("trigger", "")),
                    riskReward = obj.optString("risk_reward", "")
                )
                result.add(item)
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseFnoStatus(jsonStr: String): FnoStatusInfo {
        try {
            val root = JSONObject(jsonStr)
            val summaryMetrics = root.optJSONObject("summary_metrics")
            return FnoStatusInfo(
                subsystem = root.optString("subsystem", "fno"),
                status = root.optString("status", root.optString("today_status", "READY")),
                vixLevel = optSafeDouble(root, "vix_level", 0.0),
                vixRegime = root.optString("vix_regime", "NORMAL"),
                circuitBreakerTier = root.optString("circuit_breaker_tier", "Tier 0 (Normal)"),
                cagrPct = if (summaryMetrics != null) optSafeDouble(summaryMetrics, "cagr_pct", 0.0) else 0.0,
                ytdPct = if (summaryMetrics != null) optSafeDouble(summaryMetrics, "ytd_2026_pct", 0.0) else 0.0,
                maxDrawdownPct = if (summaryMetrics != null) optSafeDouble(summaryMetrics, "max_drawdown_pct", 0.0) else 0.0,
                sharpeRatio = if (summaryMetrics != null) optSafeDouble(summaryMetrics, "sharpe_ratio", 0.0) else 0.0,
                winRatePct = if (summaryMetrics != null) optSafeDouble(summaryMetrics, "win_rate_pct", 0.0) else 0.0,
                lastDate = root.optString("last_date", "2026-10-09"),
                noTradeReason = if (root.has("no_trade_reason") && !root.isNull("no_trade_reason")) root.optString("no_trade_reason") else null
            )
        } catch (_: Exception) {
            return FnoStatusInfo()
        }
    }

    fun parseFnoDailyPnl(jsonStr: String): List<FnoDailyPnlItem> {
        val result = mutableListOf<FnoDailyPnlItem>()
        try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("records") ?: (if (root.has("daily_pnl")) root.optJSONArray("daily_pnl") else null) ?: return result
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue

                val posList = mutableListOf<FnoHistoricalPosition>()
                val posArr = obj.optJSONArray("positions")
                if (posArr != null) {
                    for (p in 0 until posArr.length()) {
                        val pObj = posArr.optJSONObject(p) ?: continue
                        posList.add(
                            FnoHistoricalPosition(
                                symbol = pObj.optString("symbol", ""),
                                direction = pObj.optString("direction", "SHORT"),
                                instrumentType = pObj.optString("instrument_type", "FUT"),
                                lots = pObj.optInt("lots", 1),
                                quantity = pObj.optInt("quantity", 0),
                                entryPrice = optSafeDouble(pObj, "entry_price", 0.0),
                                closePrice = optSafeDouble(pObj, "close_price", 0.0),
                                dayPnl = optSafeDouble(pObj, "day_pnl", 0.0),
                                strategyEngine = pObj.optString("strategy_engine", "IndexTrendEngine")
                            )
                        )
                    }
                }

                val tradeList = mutableListOf<FnoHistoricalTrade>()
                val tradeArr = obj.optJSONArray("trades")
                val dayDate = obj.optString("date", "2026-10-09")
                if (tradeArr != null) {
                    for (t in 0 until tradeArr.length()) {
                        val tObj = tradeArr.optJSONObject(t) ?: continue
                        val exitTimestamp = tObj.optString("exit_timestamp", "")
                        val exitDate = tObj.optString("exit_date", if (exitTimestamp.length >= 10) exitTimestamp.substring(0, 10) else dayDate)
                        val exitTime = if (exitTimestamp.length >= 16) exitTimestamp.substring(11, 16) else tObj.optString("exit_time", tObj.optString("time", "15:15"))
                        val symbol = tObj.optString("tradingsymbol", tObj.optString("symbol", ""))
                        val direction = tObj.optString("direction", "SHORT")
                        val action = if (tObj.has("action")) tObj.optString("action") else (if (direction.equals("SHORT", ignoreCase = true)) "BUY" else "SELL")
                        val instType = tObj.optString("instrument_type", if (symbol.endsWith("PE") || symbol.endsWith("CE")) "OPT" else "FUT")

                        tradeList.add(
                            FnoHistoricalTrade(
                                tradeId = tObj.optString("trade_id", "trd_$t"),
                                time = exitTime,
                                symbol = symbol,
                                action = action,
                                instrumentType = instType,
                                lots = tObj.optInt("lots", 1),
                                quantity = if (tObj.has("closed_quantity")) tObj.optInt("closed_quantity") else tObj.optInt("quantity", 0),
                                executionPrice = optSafeDouble(tObj, "exit_price", optSafeDouble(tObj, "execution_price", 0.0)),
                                realizedPnl = optSafeDouble(tObj, "net_pnl", optSafeDouble(tObj, "realized_pnl", 0.0)),
                                executionReason = tObj.optString("exit_reason", tObj.optString("execution_reason", "")),
                                costs = optSafeDouble(tObj, "total_friction", optSafeDouble(tObj, "costs", 0.0)),
                                date = exitDate
                            )
                        )
                    }
                }

                val item = FnoDailyPnlItem(
                    date = obj.optString("date", "2026-10-09"),
                    dailyPnl = optSafeDouble(obj, "daily_pnl", 0.0),
                    dailyReturnPct = optSafeDouble(obj, "daily_return_pct", 0.0),
                    totalEquity = optSafeDouble(obj, "total_equity", 1000000.0),
                    marginBlocked = optSafeDouble(obj, "margin_blocked", 0.0),
                    positionsCount = obj.optInt("positions_count", if (posList.isNotEmpty()) posList.size else 0),
                    tradeCount = obj.optInt("trade_count", if (tradeList.isNotEmpty()) tradeList.size else 0),
                    dayStatus = obj.optString("day_status", if (optSafeDouble(obj, "daily_pnl", 0.0) >= 0.0) "WIN" else "LOSS"),
                    costs = optSafeDouble(obj, "costs", 0.0),
                    positions = posList,
                    trades = tradeList
                )
                result.add(item)
            }
        } catch (_: Exception) {}
        return result
    }

    internal fun loadFnoFromDisk(): VeeraLakshmiUiState? {
        val dir = cacheDirectory ?: return null
        val file = File(dir, FNO_CACHE_FILE_NAME)
        if (!file.exists() || !file.isFile) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            val root = JSONObject(text)
            val selectedId = root.optString("selected_instance_id", "10L")
            val instancesJson = root.optString("instances_json", "")
            val positionsJson = root.optString("positions_json", "")
            val dailyPnlJson = root.optString("daily_pnl_json", "")
            val statusJson = root.optString("status_json", "")
            val lastSync = root.optLong("last_sync_timestamp", 0L)

            val instances = if (instancesJson.isNotBlank()) parseFnoInstances(instancesJson) else emptyMap()
            val positions = if (positionsJson.isNotBlank()) parseFnoPositions(positionsJson) else emptyList()
            val dailyPnl = if (dailyPnlJson.isNotBlank()) parseFnoDailyPnl(dailyPnlJson) else emptyList()
            val status = if (statusJson.isNotBlank()) parseFnoStatus(statusJson) else FnoStatusInfo()

            if (instances.isNotEmpty()) {
                val todayTradesJson = root.optString("today_trades_json", "")
                val savedTodayTrades = if (todayTradesJson.isNotBlank()) parseFnoClosedTrades(todayTradesJson) else emptyList()
                val tradesFromDaily = dailyPnl.firstOrNull { it.isToday }?.trades?.takeIf { it.isNotEmpty() }
                val latestDailyTrades = dailyPnl.filter { it.trades.isNotEmpty() }.maxByOrNull { it.date }?.trades
                val todayTrades = savedTodayTrades.takeIf { it.isNotEmpty() }
                    ?: tradesFromDaily
                    ?: latestDailyTrades
                    ?: emptyList()

                VeeraLakshmiUiState(
                    selectedInstanceId = selectedId,
                    instances = instances,
                    activePositions = positions,
                    dailyPnlHistory = dailyPnl,
                    statusInfo = status,
                    isLoading = false,
                    isLiveSync = false,
                    errorMessage = null,
                    lastSyncTimestamp = lastSync,
                    todayTrades = todayTrades
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    internal fun saveFnoToDisk(state: VeeraLakshmiUiState) {
        val dir = cacheDirectory ?: return
        try {
            val root = JSONObject()
            root.put("selected_instance_id", state.selectedInstanceId)
            root.put("last_sync_timestamp", state.lastSyncTimestamp)

            val instancesObj = JSONObject()
            val instancesMap = JSONObject()
            for ((k, v) in state.instances) {
                val inst = JSONObject()
                inst.put("instance_id", v.instanceId)
                inst.put("name", v.name)
                inst.put("allocated_capital", v.allocatedCapital)
                inst.put("margin_blocked", v.marginBlocked)
                inst.put("unrealized_pnl", v.unrealizedPnl)
                inst.put("cash_buffer", v.cashBuffer)
                inst.put("margin_utilization_pct", v.marginUtilizationPct)
                inst.put("positions_count", v.positionsCount)
                inst.put("as_of_date", v.asOfDate)
                instancesMap.put(k, inst)
            }
            instancesObj.put("instances", instancesMap)
            root.put("instances_json", instancesObj.toString())

            val posArr = JSONArray()
            for (p in state.activePositions) {
                val obj = JSONObject()
                obj.put("position_id", p.positionId)
                obj.put("strategy_engine", p.strategyEngine)
                obj.put("symbol", p.symbol)
                obj.put("instrument_type", p.instrumentType)
                obj.put("direction", p.direction)
                obj.put("quantity", p.quantity)
                obj.put("lots", p.lots)
                obj.put("entry_price", p.entryPrice)
                obj.put("current_price", p.currentPrice)
                obj.put("margin_required", p.marginRequired)
                obj.put("stop_loss_price", p.stopLossPrice)
                obj.put("unrealized_pnl", p.unrealizedPnl)
                obj.put("expiry_date", p.expiryDate)
                obj.put("strike_price", p.strikePrice)
                if (p.optionType != null) obj.put("option_type", p.optionType)
                if (p.entryReason.isNotBlank()) obj.put("entry_reason", p.entryReason)
                if (p.targetPrice > 0.0) obj.put("target_price", p.targetPrice)
                if (p.signalTrigger.isNotBlank()) obj.put("signal_trigger", p.signalTrigger)
                if (p.riskReward.isNotBlank()) obj.put("risk_reward", p.riskReward)
                posArr.put(obj)
            }
            val posRoot = JSONObject()
            posRoot.put("positions", posArr)
            root.put("positions_json", posRoot.toString())

            val statusObj = JSONObject()
            statusObj.put("subsystem", state.statusInfo.subsystem)
            statusObj.put("status", state.statusInfo.status)
            statusObj.put("vix_level", state.statusInfo.vixLevel)
            statusObj.put("vix_regime", state.statusInfo.vixRegime)
            statusObj.put("circuit_breaker_tier", state.statusInfo.circuitBreakerTier)
            val summaryMetrics = JSONObject()
            summaryMetrics.put("cagr_pct", state.statusInfo.cagrPct)
            summaryMetrics.put("ytd_2026_pct", state.statusInfo.ytdPct)
            summaryMetrics.put("max_drawdown_pct", state.statusInfo.maxDrawdownPct)
            summaryMetrics.put("sharpe_ratio", state.statusInfo.sharpeRatio)
            summaryMetrics.put("win_rate_pct", state.statusInfo.winRatePct)
            statusObj.put("summary_metrics", summaryMetrics)
            statusObj.put("last_date", state.statusInfo.lastDate)
            if (state.statusInfo.noTradeReason != null) {
                statusObj.put("no_trade_reason", state.statusInfo.noTradeReason)
            }
            root.put("status_json", statusObj.toString())

            val pnlArr = JSONArray()
            for (d in state.dailyPnlHistory) {
                val obj = JSONObject()
                obj.put("date", d.date)
                obj.put("daily_pnl", d.dailyPnl)
                obj.put("daily_return_pct", d.dailyReturnPct)
                obj.put("total_equity", d.totalEquity)
                obj.put("margin_blocked", d.marginBlocked)
                obj.put("positions_count", d.positionsCount)
                obj.put("trade_count", d.tradeCount)
                obj.put("day_status", d.dayStatus)
                obj.put("costs", d.costs)

                val dayPosArr = JSONArray()
                for (p in d.positions) {
                    val pObj = JSONObject()
                    pObj.put("symbol", p.symbol)
                    pObj.put("direction", p.direction)
                    pObj.put("instrument_type", p.instrumentType)
                    pObj.put("lots", p.lots)
                    pObj.put("quantity", p.quantity)
                    pObj.put("entry_price", p.entryPrice)
                    pObj.put("close_price", p.closePrice)
                    pObj.put("day_pnl", p.dayPnl)
                    pObj.put("strategy_engine", p.strategyEngine)
                    dayPosArr.put(pObj)
                }
                obj.put("positions", dayPosArr)

                val dayTrdArr = JSONArray()
                for (t in d.trades) {
                    val tObj = JSONObject()
                    tObj.put("trade_id", t.tradeId)
                    tObj.put("time", t.time)
                    tObj.put("symbol", t.symbol)
                    tObj.put("action", t.action)
                    tObj.put("instrument_type", t.instrumentType)
                    tObj.put("lots", t.lots)
                    tObj.put("quantity", t.quantity)
                    tObj.put("execution_price", t.executionPrice)
                    tObj.put("realized_pnl", t.realizedPnl)
                    tObj.put("execution_reason", t.executionReason)
                    tObj.put("costs", t.costs)
                    dayTrdArr.put(tObj)
                }
                obj.put("trades", dayTrdArr)

                pnlArr.put(obj)
            }
            val pnlRoot = JSONObject()
            pnlRoot.put("records", pnlArr)
            root.put("daily_pnl_json", pnlRoot.toString())

            val todayTrdArr = JSONArray()
            for (t in state.todayTrades) {
                val tObj = JSONObject()
                tObj.put("trade_id", t.tradeId)
                tObj.put("time", t.time)
                tObj.put("tradingsymbol", t.symbol)
                tObj.put("symbol", t.symbol)
                tObj.put("action", t.action)
                tObj.put("instrument_type", t.instrumentType)
                tObj.put("lots", t.lots)
                tObj.put("closed_quantity", t.quantity)
                tObj.put("quantity", t.quantity)
                tObj.put("exit_price", t.executionPrice)
                tObj.put("execution_price", t.executionPrice)
                tObj.put("net_pnl", t.realizedPnl)
                tObj.put("realized_pnl", t.realizedPnl)
                tObj.put("exit_reason", t.executionReason)
                tObj.put("execution_reason", t.executionReason)
                tObj.put("total_friction", t.costs)
                tObj.put("costs", t.costs)
                tObj.put("exit_date", t.date)
                todayTrdArr.put(tObj)
            }
            val todayTrdRoot = JSONObject()
            todayTrdRoot.put("closed_trades", todayTrdArr)
            root.put("today_trades_json", todayTrdRoot.toString())

            val tmpFile = File(dir, "$FNO_CACHE_FILE_NAME.tmp")
            val targetFile = File(dir, FNO_CACHE_FILE_NAME)
            tmpFile.writeText(root.toString(), Charsets.UTF_8)
            if (targetFile.exists()) targetFile.delete()
            tmpFile.renameTo(targetFile)
        } catch (_: Exception) {}
    }

    fun createDefaultVeeraLakshmiState(): VeeraLakshmiUiState {
        val instances = mapOf(
            "10L" to FnoInstanceSummary(
                instanceId = "10L",
                name = "VeeraLakshmi V3 Flagship (₹10L Paper Trading)",
                allocatedCapital = 1000000.0,
                marginBlocked = 0.0,
                unrealizedPnl = 0.0,
                cashBuffer = 1000000.0,
                marginUtilizationPct = 0.0,
                positionsCount = 0,
                asOfDate = "2026-10-09"
            )
        )

        return VeeraLakshmiUiState(
            selectedInstanceId = "10L",
            instances = instances,
            activePositions = emptyList(),
            dailyPnlHistory = emptyList(),
            statusInfo = FnoStatusInfo(),
            isLoading = false,
            isLiveSync = false,
            errorMessage = null,
            lastSyncTimestamp = 0L,
            todayTrades = emptyList()
        )
    }

    // =========================================================================
    // SANTHANALAKSHMI REGIME-GATED IPO COMPOUNDER REPOSITORY
    // =========================================================================
    private var cachedSanthanaState: SanthanaLakshmiUiState? = null

    fun getCachedSanthanaState(): SanthanaLakshmiUiState {
        val current = cachedSanthanaState
        if (current != null) {
            val sanitized = current.copy(
                pastApplications = current.realPastApplications,
                historicalTrades = current.realHistoricalTrades
            )
            cachedSanthanaState = sanitized
            return sanitized
        }
        val loaded = loadSanthanaFromDisk()
        if (loaded != null) {
            val sanitized = loaded.copy(
                pastApplications = loaded.realPastApplications,
                historicalTrades = loaded.realHistoricalTrades
            )
            cachedSanthanaState = sanitized
            return sanitized
        }
        val defaultState = createDefaultSanthanaState()
        cachedSanthanaState = defaultState
        return defaultState
    }

    suspend fun refreshSanthana(
        serverBaseUrl: String = getServerUrl(),
        authToken: String = getAuthToken()
    ): SanthanaLakshmiUiState = withContext(Dispatchers.IO) {
        val cleanUrl = serverBaseUrl.trimEnd('/')
        val tokenQuery = if (authToken.isNotBlank()) "&token=$authToken" else ""

        try {
            val endpoint = "$cleanUrl/api/live-trading/positions?strategy=SANTHANALAKSHMI$tokenQuery"
            val jsonStr = httpGet(endpoint, authToken)
            val currentState = cachedSanthanaState ?: loadSanthanaFromDisk() ?: createDefaultSanthanaState()

            var updatedAlerts = currentState.activeAlerts
            var updatedApplications = currentState.activeApplications
            var updatedPastApplications = currentState.pastApplications
            if (jsonStr != null && jsonStr.isNotBlank()) {
                val parsedAlerts = parseIpoNotificationsJson(jsonStr)
                if (parsedAlerts.isNotEmpty()) {
                    updatedAlerts = parsedAlerts.filterNot { it.id == "IPO-ALERT-SANTHANA-001" }
                }

                try {
                    val root = JSONObject(jsonStr)
                    val appsArr = root.optJSONArray("applications")
                        ?: root.optJSONArray("active_applications")
                        ?: root.optJSONArray("ipo_applications")
                    if (appsArr != null && appsArr.length() > 0) {
                        val parsedApps = mutableListOf<ActiveIpoApplication>()
                        for (i in 0 until appsArr.length()) {
                            val aObj = appsArr.getJSONObject(i)
                            parsedApps.add(
                                ActiveIpoApplication(
                                    symbol = aObj.optString("symbol"),
                                    companyName = aObj.optString("company_name"),
                                    issueType = aObj.optString("issue_type", "SME"),
                                    appliedDate = aObj.optString("applied_date"),
                                    closingDate = aObj.optString("closing_date"),
                                    lotSize = aObj.optInt("lot_size"),
                                    cutOffPrice = aObj.optDouble("cut_off_price"),
                                    totalAmount = aObj.optDouble("total_amount"),
                                    mandateStatus = aObj.optString("mandate_status", "PENDING_UPI"),
                                    allotmentDate = aObj.optString("allotment_date"),
                                    listingDate = aObj.optString("listing_date"),
                                    status = aObj.optString("status", "APPLIED"),
                                    autoSellEnabled = aObj.optBoolean("auto_sell_enabled", true),
                                    targetListingGainPct = aObj.optDouble("target_listing_gain_pct", 35.0),
                                    executionChannel = aObj.optString("execution_channel", "ZERODHA_CNC"),
                                    registrarName = aObj.optString("registrar_name", "Bigshare Services")
                                )
                            )
                        }
                        if (parsedApps.isNotEmpty()) {
                            updatedApplications = parsedApps
                        }
                    }

                    val pastArr = root.optJSONArray("past_applications")
                        ?: root.optJSONArray("past_ipo_applications")
                    if (pastArr != null && pastArr.length() > 0) {
                        val parsedPast = mutableListOf<PastIpoApplication>()
                        for (i in 0 until pastArr.length()) {
                            val pObj = pastArr.getJSONObject(i)
                            val id = pObj.optString("id", "PAST-$i")
                            val symbol = pObj.optString("symbol").uppercase(Locale.US)
                            if (id.startsWith("PAST-IPO-") || id in SAMPLE_PAST_IPO_IDS || symbol in SAMPLE_PAST_IPO_SYMBOLS) {
                                continue
                            }
                            parsedPast.add(
                                PastIpoApplication(
                                    id = id,
                                    symbol = pObj.optString("symbol"),
                                    companyName = pObj.optString("company_name"),
                                    issueType = pObj.optString("issue_type", "MAINBOARD"),
                                    appliedDate = pObj.optString("applied_date"),
                                    allotmentDate = pObj.optString("allotment_date"),
                                    listingDate = pObj.optString("listing_date"),
                                    lotSize = pObj.optInt("lot_size"),
                                    cutOffPrice = pObj.optDouble("cut_off_price"),
                                    totalAmount = pObj.optDouble("total_amount"),
                                    allotmentStatus = pObj.optString("allotment_status", "NOT_ALLOTTED"),
                                    refundStatus = pObj.optString("refund_status", "UNBLOCKED_TO_BANK"),
                                    listingPrice = if (pObj.has("listing_price")) pObj.optDouble("listing_price") else null,
                                    listingGainPct = if (pObj.has("listing_gain_pct")) pObj.optDouble("listing_gain_pct") else null,
                                    realizedPnl = if (pObj.has("realized_pnl")) pObj.optDouble("realized_pnl") else null,
                                    outcomeReason = pObj.optString("outcome_reason", ""),
                                    registrarName = pObj.optString("registrar_name", "Link Intime India"),
                                    subscriptionMultiple = if (pObj.has("subscription_multiple")) pObj.optDouble("subscription_multiple") else null
                                )
                            )
                        }
                        updatedPastApplications = parsedPast
                    }
                } catch (_: Exception) {}
            }

            if (cachedIpoNotifications.isNotEmpty()) {
                val liveActive = cachedIpoNotifications.filterNot { it.id == "IPO-ALERT-SANTHANA-001" }
                if (liveActive.isNotEmpty()) {
                    updatedAlerts = liveActive
                }
            }

            val allDismissedIds = (
                cachedIpoNotifications.filter { it.isDismissed }.map { it.id } +
                currentState.activeAlerts.filter { it.isDismissed }.map { it.id }
            ).toSet()

            val validAlerts = updatedAlerts
                .map { if (it.id in allDismissedIds || it.symbol.uppercase() in allDismissedIds) it.copy(isDismissed = true) else it }
                .filter { it.isDeadlineTodayOrFuture }

            val updatedState = currentState.copy(
                activeAlerts = validAlerts,
                activeApplications = updatedApplications,
                pastApplications = updatedPastApplications,
                isLoading = false,
                errorMessage = null
            )
            cachedSanthanaState = updatedState
            saveSanthanaToDisk(updatedState)
            updatedState
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val fallback = cachedSanthanaState ?: loadSanthanaFromDisk() ?: createDefaultSanthanaState()
            fallback.copy(isLoading = false, errorMessage = e.message)
        }
    }

    internal fun loadSanthanaFromDisk(): SanthanaLakshmiUiState? {
        val dir = cacheDirectory ?: return null
        val file = File(dir, SANTHANA_CACHE_FILE_NAME)
        if (!file.exists() || !file.isFile) return null
        return try {
            val text = file.readText(Charsets.UTF_8)
            val root = JSONObject(text)
            val summaryObj = root.optJSONObject("summary")
            val summary = if (summaryObj != null) {
                SanthanaLakshmiSummary(
                    strategyName = summaryObj.optString("strategy_name", "SanthanaLakshmi (Regime-Gated IPO Compounder)"),
                    allocatedCapital = summaryObj.optDouble("allocated_capital", 100000.0),
                    deployedCapital = summaryObj.optDouble("deployed_capital", 0.0),
                    sweptCashBalance = summaryObj.optDouble("swept_cash_balance", 100000.0),
                    sweepYieldPct = summaryObj.optDouble("sweep_yield_pct", 6.50),
                    lifetimeCagrPct = summaryObj.optDouble("lifetime_cagr_pct", 21.55),
                    winRatePct = summaryObj.optDouble("win_rate_pct", 82.14),
                    avgListingGainPct = summaryObj.optDouble("avg_listing_gain_pct", 36.18),
                    maxDrawdownPct = summaryObj.optDouble("max_drawdown_pct", -0.69),
                    profitFactor = summaryObj.optDouble("profit_factor", 11.69),
                    sharpeRatio = summaryObj.optDouble("sharpe_ratio", 7.63),
                    totalOfferingsApplied = summaryObj.optInt("total_offerings_applied", 168),
                    totalOfferingsEvaluated = summaryObj.optInt("total_offerings_evaluated", 216),
                    currentMarketRegime = summaryObj.optString("current_market_regime", "BULLISH"),
                    regimeDescription = summaryObj.optString("regime_description", "Nifty 50 above SMA50/SMA200, 20-day realized volatility 12.8%, market breadth positive (62% advancing)."),
                    gatingRuleActive = summaryObj.optString("gating_rule_active", "Apply on QIB >= 1.5x or Total >= 2.0x (Regime Adaptive Exit)")
                )
            } else SanthanaLakshmiSummary()

            val tradesArray = root.optJSONArray("trades")
            val trades = mutableListOf<SanthanaLakshmiTradeItem>()
            if (tradesArray != null && tradesArray.length() > 0) {
                for (i in 0 until tradesArray.length()) {
                    val tObj = tradesArray.optJSONObject(i) ?: continue
                    val isReal = tObj.optBoolean("is_real_trade", tObj.optBoolean("is_real", false))
                    val sym = tObj.optString("symbol", "").trim().uppercase(Locale.US)
                    val tradeId = tObj.optString("trade_id", "")
                    if (!isReal && (sym in SAMPLE_IPO_SYMBOLS || tradeId.startsWith("IPO-TRD-"))) {
                        continue
                    }
                    trades.add(
                        SanthanaLakshmiTradeItem(
                            tradeId = tradeId.ifBlank { "IPO-$i" },
                            symbol = sym,
                            companyName = tObj.optString("company_name", ""),
                            decisionDate = tObj.optString("decision_date", ""),
                            listingDate = tObj.optString("listing_date", ""),
                            issuePrice = tObj.optDouble("issue_price", 0.0),
                            exitPrice = tObj.optDouble("exit_price", 0.0),
                            netGainPct = tObj.optDouble("net_gain_pct", 0.0),
                            netPnl = tObj.optDouble("net_pnl", 0.0),
                            regime = tObj.optString("regime", "BULLISH"),
                            exitReason = tObj.optString("exit_reason", "CLOSE"),
                            qibMultiple = tObj.optDouble("qib_multiple", 0.0),
                            appliedLots = tObj.optInt("applied_lots", 1),
                            allottedLots = tObj.optInt("allotted_lots", 1),
                            appliedAmount = tObj.optDouble("applied_amount", 15000.0),
                            totalCost = tObj.optDouble("total_cost", 25.50),
                            benchmarkRetPct = tObj.optDouble("benchmark_ret_pct", 0.0),
                            alphaPct = tObj.optDouble("alpha_pct", 0.0),
                            status = tObj.optString("status", "CLOSED"),
                            isRealTrade = isReal
                        )
                    )
                }
            } else {
                trades.addAll(createDefaultSanthanaTrades())
            }

            val appsArr = root.optJSONArray("applications")
            val applications = mutableListOf<ActiveIpoApplication>()
            if (appsArr != null && appsArr.length() > 0) {
                for (i in 0 until appsArr.length()) {
                    val aObj = appsArr.getJSONObject(i)
                    applications.add(
                        ActiveIpoApplication(
                            symbol = aObj.optString("symbol"),
                            companyName = aObj.optString("company_name"),
                            issueType = aObj.optString("issue_type", "SME"),
                            appliedDate = aObj.optString("applied_date"),
                            closingDate = aObj.optString("closing_date"),
                            lotSize = aObj.optInt("lot_size"),
                            cutOffPrice = aObj.optDouble("cut_off_price"),
                            totalAmount = aObj.optDouble("total_amount"),
                            mandateStatus = aObj.optString("mandate_status", "PENDING_UPI"),
                            allotmentDate = aObj.optString("allotment_date"),
                            listingDate = aObj.optString("listing_date"),
                            status = aObj.optString("status", "APPLIED"),
                            autoSellEnabled = aObj.optBoolean("auto_sell_enabled", true),
                            targetListingGainPct = aObj.optDouble("target_listing_gain_pct", 35.0),
                            executionChannel = aObj.optString("execution_channel", "ZERODHA_CNC")
                        )
                    )
                }
            } else {
                applications.addAll(createDefaultActiveApplications())
            }

            val pastAppsArr = root.optJSONArray("past_applications")
            val pastApps = mutableListOf<PastIpoApplication>()
            if (pastAppsArr != null && pastAppsArr.length() > 0) {
                for (i in 0 until pastAppsArr.length()) {
                    val pObj = pastAppsArr.getJSONObject(i)
                    val id = pObj.optString("id", "PAST-$i")
                    val symbol = pObj.optString("symbol").uppercase(Locale.US)
                    if (id.startsWith("PAST-IPO-") || id in SAMPLE_PAST_IPO_IDS || symbol in SAMPLE_PAST_IPO_SYMBOLS) {
                        continue
                    }
                    pastApps.add(
                        PastIpoApplication(
                            id = id,
                            symbol = pObj.optString("symbol"),
                            companyName = pObj.optString("company_name"),
                            issueType = pObj.optString("issue_type", "MAINBOARD"),
                            appliedDate = pObj.optString("applied_date"),
                            allotmentDate = pObj.optString("allotment_date"),
                            listingDate = pObj.optString("listing_date"),
                            lotSize = pObj.optInt("lot_size"),
                            cutOffPrice = pObj.optDouble("cut_off_price"),
                            totalAmount = pObj.optDouble("total_amount"),
                            allotmentStatus = pObj.optString("allotment_status", "NOT_ALLOTTED"),
                            refundStatus = pObj.optString("refund_status", "UNBLOCKED_TO_BANK"),
                            listingPrice = if (pObj.has("listing_price")) pObj.optDouble("listing_price") else null,
                            listingGainPct = if (pObj.has("listing_gain_pct")) pObj.optDouble("listing_gain_pct") else null,
                            realizedPnl = if (pObj.has("realized_pnl")) pObj.optDouble("realized_pnl") else null,
                            outcomeReason = pObj.optString("outcome_reason", ""),
                            registrarName = pObj.optString("registrar_name", "Link Intime India"),
                            subscriptionMultiple = if (pObj.has("subscription_multiple")) pObj.optDouble("subscription_multiple") else null
                        )
                    )
                }
            } else {
                pastApps.addAll(createDefaultPastApplications())
            }

            val alertsJson = root.optString("alerts_json", "")
            val alerts = if (alertsJson.isNotBlank()) parseIpoNotificationsJson(alertsJson) else emptyList()

            SanthanaLakshmiUiState(
                summary = summary,
                activeAlerts = alerts.filterNot { it.id == "IPO-ALERT-SANTHANA-001" }.filter { it.isDeadlineTodayOrFuture },
                activeApplications = applications,
                pastApplications = pastApps,
                historicalTrades = trades,
                isLoading = false,
                errorMessage = null
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun saveSanthanaToDisk(state: SanthanaLakshmiUiState) {
        val dir = cacheDirectory ?: return
        try {
            val root = JSONObject()
            val sum = state.summary
            val sumObj = JSONObject()
            sumObj.put("strategy_name", sum.strategyName)
            sumObj.put("allocated_capital", sum.allocatedCapital)
            sumObj.put("deployed_capital", sum.deployedCapital)
            sumObj.put("swept_cash_balance", sum.sweptCashBalance)
            sumObj.put("sweep_yield_pct", sum.sweepYieldPct)
            sumObj.put("lifetime_cagr_pct", sum.lifetimeCagrPct)
            sumObj.put("win_rate_pct", sum.winRatePct)
            sumObj.put("avg_listing_gain_pct", sum.avgListingGainPct)
            sumObj.put("max_drawdown_pct", sum.maxDrawdownPct)
            sumObj.put("profit_factor", sum.profitFactor)
            sumObj.put("sharpe_ratio", sum.sharpeRatio)
            sumObj.put("total_offerings_applied", sum.totalOfferingsApplied)
            sumObj.put("total_offerings_evaluated", sum.totalOfferingsEvaluated)
            sumObj.put("current_market_regime", sum.currentMarketRegime)
            sumObj.put("regime_description", sum.regimeDescription)
            sumObj.put("gating_rule_active", sum.gatingRuleActive)
            root.put("summary", sumObj)

            val appsArr = JSONArray()
            for (app in state.activeApplications) {
                val aObj = JSONObject()
                aObj.put("symbol", app.symbol)
                aObj.put("company_name", app.companyName)
                aObj.put("issue_type", app.issueType)
                aObj.put("applied_date", app.appliedDate)
                aObj.put("closing_date", app.closingDate)
                aObj.put("lot_size", app.lotSize)
                aObj.put("cut_off_price", app.cutOffPrice)
                aObj.put("total_amount", app.totalAmount)
                aObj.put("mandate_status", app.mandateStatus)
                aObj.put("allotment_date", app.allotmentDate)
                aObj.put("listing_date", app.listingDate)
                aObj.put("status", app.status)
                aObj.put("auto_sell_enabled", app.autoSellEnabled)
                aObj.put("target_listing_gain_pct", app.targetListingGainPct)
                aObj.put("execution_channel", app.executionChannel)
                aObj.put("registrar_name", app.registrarName)
                appsArr.put(aObj)
            }
            root.put("applications", appsArr)

            val pastAppsArr = JSONArray()
            for (p in state.pastApplications) {
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("symbol", p.symbol)
                pObj.put("company_name", p.companyName)
                pObj.put("issue_type", p.issueType)
                pObj.put("applied_date", p.appliedDate)
                pObj.put("allotment_date", p.allotmentDate)
                pObj.put("listing_date", p.listingDate)
                pObj.put("lot_size", p.lotSize)
                pObj.put("cut_off_price", p.cutOffPrice)
                pObj.put("total_amount", p.totalAmount)
                pObj.put("allotment_status", p.allotmentStatus)
                pObj.put("refund_status", p.refundStatus)
                p.listingPrice?.let { pObj.put("listing_price", it) }
                p.listingGainPct?.let { pObj.put("listing_gain_pct", it) }
                p.realizedPnl?.let { pObj.put("realized_pnl", it) }
                pObj.put("outcome_reason", p.outcomeReason)
                pObj.put("registrar_name", p.registrarName)
                p.subscriptionMultiple?.let { pObj.put("subscription_multiple", it) }
                pastAppsArr.put(pObj)
            }
            root.put("past_applications", pastAppsArr)

            val tradesArr = JSONArray()
            for (t in state.historicalTrades) {
                val tObj = JSONObject()
                tObj.put("trade_id", t.tradeId)
                tObj.put("symbol", t.symbol)
                tObj.put("company_name", t.companyName)
                tObj.put("decision_date", t.decisionDate)
                tObj.put("listing_date", t.listingDate)
                tObj.put("issue_price", t.issuePrice)
                tObj.put("exit_price", t.exitPrice)
                tObj.put("net_gain_pct", t.netGainPct)
                tObj.put("net_pnl", t.netPnl)
                tObj.put("regime", t.regime)
                tObj.put("exit_reason", t.exitReason)
                tObj.put("qib_multiple", t.qibMultiple)
                tObj.put("applied_lots", t.appliedLots)
                tObj.put("allotted_lots", t.allottedLots)
                tObj.put("applied_amount", t.appliedAmount)
                tObj.put("total_cost", t.totalCost)
                tObj.put("benchmark_ret_pct", t.benchmarkRetPct)
                tObj.put("alpha_pct", t.alphaPct)
                tObj.put("status", t.status)
                tradesArr.put(tObj)
            }
            root.put("trades", tradesArr)

            val file = File(dir, SANTHANA_CACHE_FILE_NAME)
            file.writeText(root.toString(), Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    fun createDefaultSanthanaState(): SanthanaLakshmiUiState {
        return SanthanaLakshmiUiState(
            summary = SanthanaLakshmiSummary(),
            activeAlerts = emptyList(),
            activeApplications = createDefaultActiveApplications(),
            pastApplications = createDefaultPastApplications(),
            historicalTrades = createDefaultSanthanaTrades(),
            selectedFilter = "ALL",
            selectedTradeDetail = null,
            isLoading = false,
            errorMessage = null
        )
    }

    fun createDefaultActiveApplications(): List<ActiveIpoApplication> {
        return listOf(
            ActiveIpoApplication(
                symbol = "ACMERAIL",
                companyName = "Acme India Industries SME",
                issueType = "SME",
                appliedDate = "2026-10-06",
                closingDate = "2026-10-06",
                lotSize = 600,
                cutOffPrice = 186.00,
                totalAmount = 111600.00,
                mandateStatus = "APPROVED",
                allotmentDate = "2026-10-07",
                listingDate = "2026-10-09",
                status = "APPLIED",
                autoSellEnabled = true,
                targetListingGainPct = 35.0,
                executionChannel = "ZERODHA_CNC",
                registrarName = "Bigshare Services"
            ),
            ActiveIpoApplication(
                symbol = "RKFAL",
                companyName = "R.K. Fashion Accessories SME",
                issueType = "SME",
                appliedDate = "2026-10-06",
                closingDate = "2026-10-07",
                lotSize = 1600,
                cutOffPrice = 82.00,
                totalAmount = 131200.00,
                mandateStatus = "APPROVED",
                allotmentDate = "2026-10-08",
                listingDate = "2026-10-10",
                status = "APPLIED",
                autoSellEnabled = true,
                targetListingGainPct = 35.0,
                executionChannel = "ZERODHA_CNC",
                registrarName = "Bigshare Services"
            ),
            ActiveIpoApplication(
                symbol = "PARASYNTEX",
                companyName = "Paramount Syntex SME",
                issueType = "SME",
                appliedDate = "2026-10-06",
                closingDate = "2026-10-08",
                lotSize = 1000,
                cutOffPrice = 127.00,
                totalAmount = 127000.00,
                mandateStatus = "APPROVED",
                allotmentDate = "2026-10-09",
                listingDate = "2026-10-13",
                status = "APPLIED",
                autoSellEnabled = true,
                targetListingGainPct = 35.0,
                executionChannel = "ZERODHA_CNC",
                registrarName = "Link Intime India"
            ),
            ActiveIpoApplication(
                symbol = "TNA",
                companyName = "TNA Solutions SME",
                issueType = "SME",
                appliedDate = "2026-10-06",
                closingDate = "2026-10-08",
                lotSize = 2000,
                cutOffPrice = 70.00,
                totalAmount = 140000.00,
                mandateStatus = "APPROVED",
                allotmentDate = "2026-10-09",
                listingDate = "2026-10-13",
                status = "APPLIED",
                autoSellEnabled = true,
                targetListingGainPct = 35.0,
                executionChannel = "ZERODHA_CNC",
                registrarName = "Bigshare Services"
            )
        )
    }

    fun createDefaultPastApplications(): List<PastIpoApplication> {
        return emptyList()
    }

    fun createSamplePastApplications(): List<PastIpoApplication> {
        return listOf(
            PastIpoApplication(
                id = "PAST-IPO-001",
                symbol = "NTPCGREEN",
                companyName = "NTPC Green Energy Ltd",
                issueType = "MAINBOARD",
                appliedDate = "2026-09-18",
                allotmentDate = "2026-09-22",
                listingDate = "2026-09-24",
                lotSize = 138,
                cutOffPrice = 108.00,
                totalAmount = 14904.00,
                allotmentStatus = "NOT_ALLOTTED",
                refundStatus = "UNBLOCKED_TO_BANK",
                listingPrice = 112.50,
                listingGainPct = 4.17,
                realizedPnl = 0.00,
                outcomeReason = "Retail quota 15.8x oversubscribed. Not selected in registrar lottery. UPI mandate revoked automatically and full ₹14,904.00 unblocked to bank.",
                registrarName = "KFin Technologies",
                subscriptionMultiple = 15.8
            ),
            PastIpoApplication(
                id = "PAST-IPO-002",
                symbol = "SWIGGY",
                companyName = "Swiggy Limited",
                issueType = "MAINBOARD",
                appliedDate = "2026-11-06",
                allotmentDate = "2026-11-08",
                listingDate = "2026-11-13",
                lotSize = 38,
                cutOffPrice = 390.00,
                totalAmount = 14820.00,
                allotmentStatus = "ALLOTTED",
                refundStatus = "DEBITED",
                listingPrice = 456.00,
                listingGainPct = 16.92,
                realizedPnl = 2139.38,
                outcomeReason = "1 Lot (38 shares) successfully allotted. Deposited into Zerodha demat and auto-sold at 10:00 AM CNC listing exit (+16.92% net profit).",
                registrarName = "Link Intime India",
                subscriptionMultiple = 6.02
            ),
            PastIpoApplication(
                id = "PAST-IPO-003",
                symbol = "WAAREEENER",
                companyName = "Waaree Energies Ltd",
                issueType = "MAINBOARD",
                appliedDate = "2026-10-21",
                allotmentDate = "2026-10-23",
                listingDate = "2026-10-28",
                lotSize = 9,
                cutOffPrice = 1503.00,
                totalAmount = 13527.00,
                allotmentStatus = "ALLOTTED",
                refundStatus = "DEBITED",
                listingPrice = 2341.00,
                listingGainPct = 55.76,
                realizedPnl = 8070.00,
                outcomeReason = "1 Lot (9 shares) successfully allotted. Deposited into Zerodha demat and auto-sold at 10:00 AM CNC listing exit (+55.76% net profit).",
                registrarName = "Link Intime India",
                subscriptionMultiple = 208.63
            ),
            PastIpoApplication(
                id = "PAST-IPO-004",
                symbol = "BAJAJHFL",
                companyName = "Bajaj Housing Finance Ltd",
                issueType = "MAINBOARD",
                appliedDate = "2026-09-09",
                allotmentDate = "2026-09-12",
                listingDate = "2026-09-16",
                lotSize = 204,
                cutOffPrice = 70.00,
                totalAmount = 14280.00,
                allotmentStatus = "ALLOTTED",
                refundStatus = "DEBITED",
                listingPrice = 165.00,
                listingGainPct = 135.71,
                realizedPnl = 19380.00,
                outcomeReason = "1 Lot (204 shares) successfully allotted. Multibagger listing surge (+135.71%) captured on listing day CNC exit.",
                registrarName = "KFin Technologies",
                subscriptionMultiple = 67.43
            ),
            PastIpoApplication(
                id = "PAST-IPO-005",
                symbol = "KRNHEAT",
                companyName = "KRN Heat Exchanger and Refrigeration",
                issueType = "MAINBOARD",
                appliedDate = "2026-09-25",
                allotmentDate = "2026-09-28",
                listingDate = "2026-10-01",
                lotSize = 65,
                cutOffPrice = 220.00,
                totalAmount = 14300.00,
                allotmentStatus = "NOT_ALLOTTED",
                refundStatus = "UNBLOCKED_TO_BANK",
                listingPrice = 478.00,
                listingGainPct = 117.27,
                realizedPnl = 0.00,
                outcomeReason = "Retail quota heavily oversubscribed (96.7x). Not selected in lottery. Mandate revoked and ₹14,300.00 unblocked to bank (0 loss).",
                registrarName = "Bigshare Services",
                subscriptionMultiple = 96.7
            ),
            PastIpoApplication(
                id = "PAST-IPO-006",
                symbol = "PREMIEREN",
                companyName = "Premier Energies Ltd",
                issueType = "MAINBOARD",
                appliedDate = "2026-08-27",
                allotmentDate = "2026-08-30",
                listingDate = "2026-09-03",
                lotSize = 33,
                cutOffPrice = 450.00,
                totalAmount = 14850.00,
                allotmentStatus = "NOT_ALLOTTED",
                refundStatus = "UNBLOCKED_TO_BANK",
                listingPrice = 991.00,
                listingGainPct = 120.22,
                realizedPnl = 0.00,
                outcomeReason = "Retail quota 74.3x oversubscribed. Not selected in lottery. Mandate revoked and ₹14,850.00 unblocked to bank (0 loss).",
                registrarName = "KFin Technologies",
                subscriptionMultiple = 74.3
            )
        )
    }

    fun updateIpoMandateStatus(symbol: String, newStatus: String = "APPROVED"): SanthanaLakshmiUiState {
        val current = cachedSanthanaState ?: loadSanthanaFromDisk() ?: createDefaultSanthanaState()
        val updatedApplications = current.activeApplications.map { app ->
            if (app.symbol.equals(symbol, ignoreCase = true)) {
                app.copy(mandateStatus = newStatus)
            } else {
                app
            }
        }
        val newState = current.copy(activeApplications = updatedApplications)
        cachedSanthanaState = newState
        saveSanthanaToDisk(newState)
        return newState
    }

    fun updateIpoAllotmentStatus(symbol: String, newStatus: String): SanthanaLakshmiUiState {
        val current = cachedSanthanaState ?: loadSanthanaFromDisk() ?: createDefaultSanthanaState()
        val updatedApplications = current.activeApplications.map { app ->
            if (app.symbol.equals(symbol, ignoreCase = true)) {
                app.copy(status = newStatus)
            } else {
                app
            }
        }
        val newState = current.copy(activeApplications = updatedApplications)
        cachedSanthanaState = newState
        saveSanthanaToDisk(newState)
        return newState
    }

    fun createDefaultSanthanaTrades(): List<SanthanaLakshmiTradeItem> {
        return emptyList()
    }

    fun createSampleSanthanaTrades(): List<SanthanaLakshmiTradeItem> {
        return listOf(
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-215",
                symbol = "SWIGGY",
                companyName = "Swiggy Limited",
                decisionDate = "2024-11-08",
                listingDate = "2024-11-13",
                issuePrice = 390.00,
                exitPrice = 456.00,
                netGainPct = 16.92,
                netPnl = 2139.38,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 6.02,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14820.00,
                totalCost = 28.62,
                benchmarkRetPct = -1.90,
                alphaPct = 18.82
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-209",
                symbol = "WAAREEENER",
                companyName = "Waaree Energies Ltd",
                decisionDate = "2024-10-23",
                listingDate = "2024-10-28",
                issuePrice = 1503.00,
                exitPrice = 2341.00,
                netGainPct = 55.76,
                netPnl = 807.01,
                regime = "BULLISH",
                exitReason = "STOP_LOSS",
                qibMultiple = 208.63,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 13527.00,
                totalCost = 24.18,
                benchmarkRetPct = -0.65,
                alphaPct = 56.41
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-206",
                symbol = "KRN",
                companyName = "KRN Heat Exchanger and Refrigeration",
                decisionDate = "2024-09-27",
                listingDate = "2024-10-03",
                issuePrice = 220.00,
                exitPrice = 478.00,
                netGainPct = 117.27,
                netPnl = 157.70,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 253.90,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14300.00,
                totalCost = 22.10,
                benchmarkRetPct = -2.12,
                alphaPct = 119.39
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-204",
                symbol = "NORTHARC",
                companyName = "Northern Arc Capital Ltd",
                decisionDate = "2024-09-19",
                listingDate = "2024-09-24",
                issuePrice = 349.00,
                exitPrice = 430.00,
                netGainPct = 23.21,
                netPnl = 141.26,
                regime = "BULLISH",
                exitReason = "STOP_LOSS",
                qibMultiple = 42.48,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14658.00,
                totalCost = 23.44,
                benchmarkRetPct = 0.25,
                alphaPct = 22.96
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-203",
                symbol = "ARKADE",
                companyName = "Arkade Developers Ltd",
                decisionDate = "2024-09-19",
                listingDate = "2024-09-24",
                issuePrice = 128.00,
                exitPrice = 175.90,
                netGainPct = 37.42,
                netPnl = 86.72,
                regime = "BULLISH",
                exitReason = "STOP_LOSS",
                qibMultiple = 51.33,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14080.00,
                totalCost = 22.80,
                benchmarkRetPct = 0.25,
                alphaPct = 37.17
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-202",
                symbol = "WESTERN",
                companyName = "Western Carriers India Ltd",
                decisionDate = "2024-09-19",
                listingDate = "2024-09-24",
                issuePrice = 172.00,
                exitPrice = 168.00,
                netGainPct = -2.33,
                netPnl = -23.90,
                regime = "NEUTRAL",
                exitReason = "REGIME_DEFENSE_OPEN",
                qibMultiple = 21.83,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14964.00,
                totalCost = 21.50,
                benchmarkRetPct = 0.25,
                alphaPct = -2.58
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-198",
                symbol = "BAJAJHFL",
                companyName = "Bajaj Housing Finance Ltd",
                decisionDate = "2024-09-11",
                listingDate = "2024-09-16",
                issuePrice = 70.00,
                exitPrice = 165.00,
                netGainPct = 135.71,
                netPnl = 20328.00,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 222.16,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 14980.00,
                totalCost = 22.00,
                benchmarkRetPct = 0.35,
                alphaPct = 135.36
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "IPO-TRD-140",
                symbol = "TATATECH",
                companyName = "Tata Technologies Ltd",
                decisionDate = "2023-11-24",
                listingDate = "2023-11-30",
                issuePrice = 500.00,
                exitPrice = 1200.00,
                netGainPct = 140.00,
                netPnl = 20974.50,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 203.41,
                appliedLots = 1,
                allottedLots = 1,
                appliedAmount = 15000.00,
                totalCost = 25.50,
                benchmarkRetPct = 1.05,
                alphaPct = 138.95
            )
        )
    }
}

