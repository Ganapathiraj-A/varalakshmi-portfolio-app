package com.example.varalakshmiportfolio.data

import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunTradeStatus
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

        @Volatile
        var enableNetworkQuotes: Boolean = true

        private val lock = Any()
        @Volatile
        private var memoryCache: MutableList<ArjunStockItem>? = null

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
                    entryPrice = 1040.0,
                    entryPriceRange = "1035 - 1045",
                    entryDate = todayStr,
                    currentPrice = 1065.15,
                    todayChangePct = 6.36,
                    targetPrice1 = 1175.0,
                    targetPrice2 = 1250.0,
                    stopLoss = 949.95,
                    quantity = 25,
                    notes = "Swing trade alert. Currently trading 1043.",
                    rawText = "ARJUN Combo Package 🚀\nSWING TRADE\nBUY SKYGOLD AROUND 1035-1045\nTARGETS 1175-1250\nKEEP SL 949.95\nCURRENTLY TRADING 1043"
                ),
                ArjunStockItem(
                    id = "arjun-seed-lalitha",
                    symbol = "LALITHAA",
                    companyName = "Lalithaa Jewellery Mart Ltd",
                    brokerSource = "ARJUN Combo Package 🚀",
                    tradeType = "SWING TRADE",
                    action = "BUY",
                    entryPrice = 367.50,
                    entryPriceRange = "365 - 370",
                    entryDate = todayStr,
                    currentPrice = 365.65,
                    todayChangePct = 3.33,
                    targetPrice1 = 425.0,
                    targetPrice2 = 450.0,
                    stopLoss = 324.95,
                    quantity = 50,
                    notes = "Swing trade alert. Currently trading 368.50.",
                    rawText = "ARJUN Combo Package 🚀\nSWING TRADE\nBUY LALITHA AROUND 365-370\nTARGETS 425 -450\nKEEP SL 324.95\nCURRENTLY TRADING 368.50"
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
                    quantity = 100,
                    notes = "STRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)",
                    rawText = "ARJUN Combo Package 🚀\nBREAKOUT ALERT\nBUY ORIENTHOT (ORIENT HOTELS)\nABOVE 149.05\nTARGETS 190-225\nKEEP SL 129.95\nCURRENTLY TRADING 145.50\nSTRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)"
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
            // Add at top
            list.add(0, item)
            memoryCache = list
            saveToDisk(list)
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
            saveToDisk(list)
            return list.toList()
        }
    }

    fun deleteRecommendation(id: String): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            list.removeAll { it.id == id }
            memoryCache = list
            saveToDisk(list)
            return list.toList()
        }
    }

    fun markAsClosed(id: String, exitPrice: Double = 0.0): List<ArjunStockItem> {
        synchronized(lock) {
            val list = (memoryCache ?: getRecommendations().toMutableList()).toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val old = list[idx]
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val finalExit = if (exitPrice > 0) exitPrice else old.currentPrice
                list[idx] = old.copy(isClosed = true, closedPrice = finalExit, closedDate = todayStr)
            }
            memoryCache = list
            saveToDisk(list)
            return list.toList()
        }
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

            synchronized(lock) {
                memoryCache = updatedItems.toMutableList()
                saveToDisk(updatedItems)
            }
            updatedItems
        } catch (_: Throwable) {
            getRecommendations()
        }
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
            val text = file.readText(Charsets.UTF_8)
            val arr = JSONArray(text)
            val list = mutableListOf<ArjunStockItem>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
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
                        closedDate = o.optString("closed_date", "")
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDisk(items: List<ArjunStockItem>) {
        val dir = VaralakshmiRepository.getCacheDirectory() ?: return
        try {
            val file = File(dir, ARJUN_CACHE_FILE)
            val arr = JSONArray()
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
                }
                arr.put(o)
            }
            file.writeText(arr.toString(2), Charsets.UTF_8)
        } catch (_: Exception) {
            // Ignore disk cache write errors
        }
    }

    private fun roundPaise(v: Double): Double =
        if (v.isNaN() || v.isInfinite()) 0.0
        else BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_EVEN).toDouble()
}
