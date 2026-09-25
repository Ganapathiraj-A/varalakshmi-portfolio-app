package com.example.varalakshmiportfolio.data

// Typealiases for seamless access from both data and model packages
typealias PortfolioSummary = com.example.varalakshmiportfolio.model.PortfolioSummary
typealias PositionItem = com.example.varalakshmiportfolio.model.PositionItem
typealias TransactionItem = com.example.varalakshmiportfolio.model.TransactionItem
typealias MarketIndexItem = com.example.varalakshmiportfolio.model.MarketIndexItem
typealias StockRecommendationItem = com.example.varalakshmiportfolio.model.StockRecommendationItem
typealias HistoricalPricePoint = com.example.varalakshmiportfolio.model.HistoricalPricePoint
typealias HistoricalRecommendationItem = com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
typealias RecommendationHistorySummary = com.example.varalakshmiportfolio.model.RecommendationHistorySummary
 
fun isPositionBoughtToday(entryDate: String): Boolean =
    com.example.varalakshmiportfolio.model.isPositionBoughtToday(entryDate)

fun calculateReferencePrice(entryDate: String, entryPrice: Double, previousClose: Double): Double =
    com.example.varalakshmiportfolio.model.calculateReferencePrice(entryDate, entryPrice, previousClose)
