package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.data.ArjunStockRepository
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunTradeStatus
import com.example.varalakshmiportfolio.model.formatArjunStocksForClipboard
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ArjunStockTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cacheDir = tempFolder.newFolder("arjun_test_cache")
        VaralakshmiRepository.initialize(cacheDir)
        ArjunStockRepository.enableNetworkQuotes = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        ArjunStockRepository.enableNetworkQuotes = true
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Intelligent Parser Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testParseSkyGoldAlert() {
        val raw = """
            ARJUN Combo Package 🚀
            SWING TRADE
            BUY SKYGOLD AROUND 1035-1045
            TARGETS 1175-1250
            KEEP SL 949.95
            CURRENTLY TRADING 1043
        """.trimIndent()

        val parsed = ArjunRecommendationParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("SKYGOLD", parsed!!.symbol)
        assertEquals("BUY", parsed.action)
        assertEquals("SWING TRADE", parsed.tradeType)
        assertEquals(1040.0, parsed.entryPrice, 0.01)
        assertEquals("1035 - 1045", parsed.entryPriceRange)
        assertEquals(1175.0, parsed.targetPrice1, 0.01)
        assertEquals(1250.0, parsed.targetPrice2, 0.01)
        assertEquals(949.95, parsed.stopLoss, 0.01)
    }

    @Test
    fun testParseLalithaWithAliasMapping() {
        val raw = """
            ARJUN Combo Package 🚀
            SWING TRADE
            BUY LALITHA AROUND 365-370
            TARGETS 425 -450
            KEEP SL 324.95
            CURRENTLY TRADING 368.50
        """.trimIndent()

        val parsed = ArjunRecommendationParser.parse(raw)
        assertNotNull(parsed)
        // Alias maps LALITHA -> LALITHAA (correct NSE ticker)
        assertEquals("LALITHAA", parsed!!.symbol)
        assertEquals(367.50, parsed.entryPrice, 0.01)
        assertEquals(425.0, parsed.targetPrice1, 0.01)
        assertEquals(450.0, parsed.targetPrice2, 0.01)
        assertEquals(324.95, parsed.stopLoss, 0.01)
    }

    @Test
    fun testParseOrientHotBreakoutWithNotes() {
        val raw = """
            ARJUN Combo Package 🚀
            BREAKOUT ALERT
            BUY ORIENTHOT (ORIENT HOTELS)
            ABOVE 149.05
            TARGETS 190-225
            KEEP SL 129.95
            CURRENTLY TRADING 145.50
            STRICTLY BUY ABOVE 149.05 (DON'T BUY BEFORE THE LEVEL)
        """.trimIndent()

        val parsed = ArjunRecommendationParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("ORIENTHOT", parsed!!.symbol)
        assertEquals("ORIENT HOTELS", parsed.companyName)
        assertEquals("BREAKOUT ALERT", parsed.tradeType)
        assertEquals(149.05, parsed.entryPrice, 0.01)
        assertEquals(190.0, parsed.targetPrice1, 0.01)
        assertEquals(225.0, parsed.targetPrice2, 0.01)
        assertEquals(129.95, parsed.stopLoss, 0.01)
        assertTrue(parsed.notes.contains("STRICTLY BUY ABOVE 149.05"))
    }

    @Test
    fun testParseInvalidOrEmptyText() {
        assertNull(ArjunRecommendationParser.parse(""))
        assertNull(ArjunRecommendationParser.parse("   "))
        assertNull(ArjunRecommendationParser.parse("Hello Good Morning! Have a great day"))
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Model & Mathematical Calculations
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testPnlAndValueCalculations() {
        val item = ArjunStockItem(
            symbol = "SKYGOLD",
            entryPrice = 1040.0,
            currentPrice = 1065.15,
            quantity = 25
        )

        assertEquals(26000.0, item.investedValue, 0.01)
        assertEquals(26628.75, item.currentValue, 0.01)
        assertEquals(628.75, item.unrealizedPnl, 0.01)
        assertEquals(2.42, item.pnlPct, 0.01)
        assertTrue(item.isProfit)
        assertEquals("+₹628.75", item.formattedPnl)
        assertEquals("+2.42%", item.formattedPnlPct)
    }

    @Test
    fun testTradeStatusTransitions() {
        val base = ArjunStockItem(
            symbol = "TEST",
            entryPrice = 100.0,
            targetPrice1 = 120.0,
            targetPrice2 = 140.0,
            stopLoss = 90.0
        )

        // 1. Active inside range
        val active = base.copy(currentPrice = 105.0)
        assertEquals(ArjunTradeStatus.ACTIVE, active.status)

        // 2. Target 1 Hit
        val t1Hit = base.copy(currentPrice = 125.0)
        assertEquals(ArjunTradeStatus.TARGET1_HIT, t1Hit.status)

        // 3. Target 2 Hit
        val t2Hit = base.copy(currentPrice = 142.0)
        assertEquals(ArjunTradeStatus.TARGET2_HIT, t2Hit.status)

        // 4. Stop Loss Hit
        val slHit = base.copy(currentPrice = 88.0)
        assertEquals(ArjunTradeStatus.STOP_LOSS_HIT, slHit.status)

        // 5. Waiting Trigger for breakout
        val breakout = ArjunStockItem(
            symbol = "ORIENTHOT",
            tradeType = "BREAKOUT ALERT",
            entryPrice = 150.0,
            entryPriceRange = "ABOVE 150.00",
            currentPrice = 145.0,
            targetPrice1 = 180.0,
            stopLoss = 130.0
        )
        assertEquals(ArjunTradeStatus.WAITING_TRIGGER, breakout.status)

        // 6. Closed position
        val closed = base.copy(isClosed = true, closedPrice = 115.0)
        assertEquals(ArjunTradeStatus.CLOSED, closed.status)
        assertEquals(115.0, closed.effectivePrice, 0.01)
    }

    @Test
    fun testProgressToTarget1() {
        val item = ArjunStockItem(
            symbol = "TEST",
            entryPrice = 100.0,
            currentPrice = 105.0,
            stopLoss = 90.0,
            targetPrice1 = 120.0
        )
        // Range = 120 - 90 = 30. Distance from SL = 105 - 90 = 15. Fraction = 15/30 = 0.5f
        assertEquals(0.5f, item.progressToTarget1, 0.01f)
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Repository CRUD & Persistence
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testRepositoryDefaultSeedsAndCrud() {
        val repo = ArjunStockRepository()
        val initial = repo.getRecommendations()
        assertEquals(3, initial.size)
        assertTrue(initial.any { it.symbol == "SKYGOLD" })
        assertTrue(initial.any { it.symbol == "LALITHAA" })
        assertTrue(initial.any { it.symbol == "ORIENTHOT" })

        // Add
        val newItem = ArjunStockItem(
            id = "custom-test-1",
            symbol = "TATAMOTORS",
            entryPrice = 950.0,
            currentPrice = 980.0,
            quantity = 10
        )
        val afterAdd = repo.addRecommendation(newItem)
        assertEquals(4, afterAdd.size)
        assertEquals("TATAMOTORS", afterAdd.first().symbol)

        // Update
        val updated = newItem.copy(quantity = 20)
        val afterUpdate = repo.updateRecommendation(updated)
        assertEquals(20, afterUpdate.find { it.id == "custom-test-1" }?.quantity)

        // Close
        val afterClose = repo.markAsClosed("custom-test-1", exitPrice = 1000.0)
        val closedItem = afterClose.find { it.id == "custom-test-1" }
        assertNotNull(closedItem)
        assertTrue(closedItem!!.isClosed)
        assertEquals(1000.0, closedItem.closedPrice, 0.01)

        // Delete
        val afterDelete = repo.deleteRecommendation("custom-test-1")
        assertEquals(3, afterDelete.size)
        assertNull(afterDelete.find { it.id == "custom-test-1" })
    }

    @Test
    fun testSummaryCalculation() {
        val repo = ArjunStockRepository()
        val items = listOf(
            ArjunStockItem(symbol = "A", entryPrice = 100.0, currentPrice = 110.0, quantity = 10, targetPrice1 = 120.0), // Profit
            ArjunStockItem(symbol = "B", entryPrice = 200.0, currentPrice = 180.0, quantity = 5, stopLoss = 170.0),      // Loss
            ArjunStockItem(symbol = "C", entryPrice = 50.0, currentPrice = 60.0, quantity = 20, isClosed = true, closedPrice = 60.0) // Closed
        )

        val summary = repo.calculateSummary(items)
        // Active: A (1000 inv, 1100 val), B (1000 inv, 900 val) => totalInvested = 2000, currentValue = 2000
        assertEquals(2000.0, summary.totalInvested, 0.01)
        assertEquals(2000.0, summary.currentValue, 0.01)
        assertEquals(0.0, summary.totalPnl, 0.01)
        assertEquals(2, summary.activeCount)
        assertEquals(1, summary.closedCount)
        // 2 wins out of 3 total items = 66.67%
        assertEquals(66.67, summary.winRatePct, 0.1)
    }

    // ─────────────────────────────────────────────────────────────
    // 4. ViewModel Integration
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testViewModelArjunStockOperations() = runTest {
        val vm = VaralakshmiViewModel(autoRefresh = false)
        val initialState = vm.arjunStocksState.value
        assertEquals(3, initialState.recommendations.size)

        // Add via raw alert
        val raw = """
            ARJUN Combo Package 🚀
            SWING TRADE
            BUY ADANIPOWER AROUND 650-660
            TARGETS 750-800
            KEEP SL 610
        """.trimIndent()

        vm.addArjunRecommendation(raw)
        advanceUntilIdle()

        val afterAdd = vm.arjunStocksState.value
        assertEquals(4, afterAdd.recommendations.size)
        assertEquals("ADANIPOWER", afterAdd.recommendations.first().symbol)

        // Close position
        val addedId = afterAdd.recommendations.first().id
        vm.closeArjunStock(addedId, exitPrice = 720.0)
        val afterClose = vm.arjunStocksState.value
        val closed = afterClose.recommendations.find { it.id == addedId }
        assertNotNull(closed)
        assertTrue(closed!!.isClosed)
        assertEquals(720.0, closed.closedPrice, 0.01)

        // Filter selection
        vm.selectArjunFilter("CLOSED")
        assertEquals("CLOSED", vm.arjunStocksState.value.selectedFilter)

        // Delete
        vm.deleteArjunStock(addedId)
        assertEquals(3, vm.arjunStocksState.value.recommendations.size)
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Clipboard Export Formatting
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testClipboardExportFormatting() {
        val summary = ArjunPortfolioSummary(
            totalInvested = 50000.0,
            currentValue = 54000.0,
            totalPnl = 4000.0,
            totalPnlPct = 8.0,
            activeCount = 2,
            closedCount = 1,
            targetHitCount = 1,
            winRatePct = 66.7
        )
        val stocks = listOf(
            ArjunStockItem(
                symbol = "SKYGOLD",
                tradeType = "SWING TRADE",
                entryPrice = 1040.0,
                currentPrice = 1065.15,
                targetPrice1 = 1175.0,
                targetPrice2 = 1250.0,
                stopLoss = 949.95,
                quantity = 25
            )
        )

        val text = formatArjunStocksForClipboard(summary, stocks)
        assertTrue(text.contains("🏹 ARJUN STOCKS"))
        assertTrue(text.contains("Total Invested: ₹50,000.00"))
        assertTrue(text.contains("Overall P&L:    +₹4,000.00 (+8.00%)"))
        assertTrue(text.contains("SKYGOLD"))
        assertTrue(text.contains("Targets: T1=₹1,175.00, T2=₹1,250.00 | SL=₹949.95"))
    }
}
