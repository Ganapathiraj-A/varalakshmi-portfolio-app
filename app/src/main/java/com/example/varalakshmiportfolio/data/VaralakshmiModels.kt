package com.example.varalakshmiportfolio.data

// Typealiases for seamless access from both data and model packages
typealias PortfolioSummary = com.example.varalakshmiportfolio.model.PortfolioSummary
typealias PositionItem = com.example.varalakshmiportfolio.model.PositionItem
typealias TransactionItem = com.example.varalakshmiportfolio.model.TransactionItem
typealias DailyReturnItem = com.example.varalakshmiportfolio.model.DailyReturnItem
typealias MarketIndexItem = com.example.varalakshmiportfolio.model.MarketIndexItem
typealias StockRecommendationItem = com.example.varalakshmiportfolio.model.StockRecommendationItem
typealias HistoricalPricePoint = com.example.varalakshmiportfolio.model.HistoricalPricePoint
typealias HistoricalRecommendationItem = com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
typealias RecommendationHistorySummary = com.example.varalakshmiportfolio.model.RecommendationHistorySummary
typealias FnoInstanceSummary = com.example.varalakshmiportfolio.model.FnoInstanceSummary
typealias FnoPositionItem = com.example.varalakshmiportfolio.model.FnoPositionItem
typealias FnoStatusInfo = com.example.varalakshmiportfolio.model.FnoStatusInfo
typealias VeeraLakshmiUiState = com.example.varalakshmiportfolio.model.VeeraLakshmiUiState
typealias FnoDailyPnlItem = com.example.varalakshmiportfolio.model.FnoDailyPnlItem
typealias FnoHistoricalPosition = com.example.varalakshmiportfolio.model.FnoHistoricalPosition
typealias FnoHistoricalTrade = com.example.varalakshmiportfolio.model.FnoHistoricalTrade
typealias SanthanaLakshmiTradeItem = com.example.varalakshmiportfolio.model.SanthanaLakshmiTradeItem
typealias SanthanaLakshmiSummary = com.example.varalakshmiportfolio.model.SanthanaLakshmiSummary
typealias SanthanaLakshmiUiState = com.example.varalakshmiportfolio.model.SanthanaLakshmiUiState
typealias ActiveIpoApplication = com.example.varalakshmiportfolio.model.ActiveIpoApplication
typealias PastIpoApplication = com.example.varalakshmiportfolio.model.PastIpoApplication

val SAMPLE_IPO_SYMBOLS = com.example.varalakshmiportfolio.model.SAMPLE_IPO_SYMBOLS
val SAMPLE_PAST_IPO_SYMBOLS = com.example.varalakshmiportfolio.model.SAMPLE_PAST_IPO_SYMBOLS
val SAMPLE_PAST_IPO_IDS = com.example.varalakshmiportfolio.model.SAMPLE_PAST_IPO_IDS
 
fun isPositionBoughtToday(entryDate: String): Boolean =
    com.example.varalakshmiportfolio.model.isPositionBoughtToday(entryDate)

fun isDeadlineTodayOrFuture(deadlineStr: String, now: java.util.Date = java.util.Date()): Boolean =
    com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture(deadlineStr, now)

fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double =
    com.example.varalakshmiportfolio.model.calculateReferencePrice(entryDate, entryPrice, previousClose)

fun calculateStrategyDaysActive(
    inceptionDateStr: String = "2026-09-17",
    asOfDateStr: String? = null
): Int = com.example.varalakshmiportfolio.model.calculateStrategyDaysActive(inceptionDateStr, asOfDateStr)

fun calculateDaysSinceBuy(
    entryDateStr: String,
    asOfDateStr: String? = null
): Int = com.example.varalakshmiportfolio.model.calculateDaysSinceBuy(entryDateStr, asOfDateStr)

fun formatVaralakshmiAllStatementsForClipboard(
    summary: PortfolioSummary,
    positions: List<PositionItem>,
    dailyReturns: List<DailyReturnItem>,
    transactions: List<TransactionItem>
): String = com.example.varalakshmiportfolio.model.formatVaralakshmiAllStatementsForClipboard(summary, positions, dailyReturns, transactions)

fun formatDailyReturnStatementForClipboard(item: DailyReturnItem): String =
    com.example.varalakshmiportfolio.model.formatDailyReturnStatementForClipboard(item)

fun formatSanthanaLakshmiForClipboard(
    summary: SanthanaLakshmiSummary,
    trades: List<SanthanaLakshmiTradeItem>,
    activeAlerts: List<com.example.varalakshmiportfolio.model.IpoActionNotification>,
    applications: List<ActiveIpoApplication> = emptyList()
): String = com.example.varalakshmiportfolio.model.formatSanthanaLakshmiForClipboard(summary, trades, activeAlerts, applications)

fun calculateZerodhaDeliveryFees(grossAmount: Double, side: String): Double =
    com.example.varalakshmiportfolio.model.calculateZerodhaDeliveryFees(grossAmount, side)


