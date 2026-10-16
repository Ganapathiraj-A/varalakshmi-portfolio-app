package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.data.ArjunStockRepository
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.ArjunAutoExecutionEvent
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunStrategySlot
import com.example.varalakshmiportfolio.model.ArjunStrategySummary
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
        ArjunStockRepository.resetCacheForTesting()
        ArjunStockRepository.enableNetworkQuotes = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        ArjunStockRepository.resetCacheForTesting()
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

    // ─────────────────────────────────────────────────────────────
    // 6. Strategy 10-Slot Allocation & Auto-Sizing Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testStrategyApprovalAndQuantitySizing() {
        val repo = ArjunStockRepository()
        val customStock = ArjunStockItem(
            id = "strat-test-stock-1",
            symbol = "TESTSTOCK",
            entryPrice = 500.0,
            currentPrice = 505.0,
            targetPrice1 = 580.0,
            stopLoss = 460.0
        )
        repo.addRecommendation(customStock)

        // Approve customStock into Strategy. Slot budget = ₹50,000. Quantity = 50,000 / 500 = 100 shares.
        val (success, message) = repo.approveRecommendation("strat-test-stock-1", targetSlot = 4)
        assertTrue(success)
        assertTrue(message.contains("100 shares"))

        val approved = repo.getRecommendations().find { it.id == "strat-test-stock-1" }
        assertNotNull(approved)
        assertTrue(approved!!.isApprovedForStrategy)
        assertEquals(4, approved.strategySlotIndex)
        assertEquals(100, approved.quantity)
        assertEquals(50000.0, approved.investedValue, 0.01)
        assertTrue(approved.isStrategySlotOccupied)
    }

    @Test
    fun testStrategyTenSlotCapacityLimit() {
        val repo = ArjunStockRepository()
        // Seeds occupy slots 1, 2, 3.
        // Add 7 more and approve them into slots 4, 5, 6, 7, 8, 9, 10.
        for (i in 4..10) {
            val s = ArjunStockItem(
                id = "stock-$i",
                symbol = "SYM$i",
                entryPrice = 100.0,
                currentPrice = 100.0
            )
            repo.addRecommendation(s)
            val (ok, _) = repo.approveRecommendation("stock-$i", targetSlot = i)
            assertTrue(ok)
        }

        val slots = repo.calculateStrategySlots(repo.getRecommendations())
        assertEquals(10, slots.size)
        assertEquals(10, slots.count { it.isOccupied })

        // 11th recommendation should be rejected because all 10 slots are occupied
        val s11 = ArjunStockItem(
            id = "stock-11",
            symbol = "SYM11",
            entryPrice = 200.0,
            currentPrice = 200.0
        )
        repo.addRecommendation(s11)
        val (eleventhSuccess, msg) = repo.approveRecommendation("stock-11")
        assertFalse(eleventhSuccess)
        assertTrue(msg.contains("All 10 strategy slots") && msg.contains("occupied"))

        // Unapproving slot 4 frees it up
        val afterUnapprove = repo.unapproveRecommendation("stock-4")
        val unapprovedStock = afterUnapprove.find { it.id == "stock-4" }
        assertNotNull(unapprovedStock)
        assertFalse(unapprovedStock!!.isApprovedForStrategy)
        assertFalse(unapprovedStock.isStrategySlotOccupied)

        // Now 11th can be approved into the freed slot
        val (retrySuccess, retryMsg) = repo.approveRecommendation("stock-11")
        assertTrue(retrySuccess)
        assertTrue(retryMsg.contains("Approved SYM11"))
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Automated Exit Triggers (Stop Loss & Target 1 Hit)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testAutomatedExitOnProfitTargetHit() {
        val repo = ArjunStockRepository()
        val item = ArjunStockItem(
            id = "target-hit-item",
            symbol = "MULTIBAGGER",
            entryPrice = 1000.0,
            currentPrice = 1205.0, // Above Target 1 (1200.0)
            targetPrice1 = 1200.0,
            stopLoss = 920.0,
            isApprovedForStrategy = true,
            strategySlotIndex = 4,
            quantity = 50 // 50 * 1000 = 50,000
        )

        val (updatedItems, events) = repo.evaluateStrategyTriggers(listOf(item))
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("TARGET_HIT", event.eventType)
        assertEquals("MULTIBAGGER", event.symbol)
        assertEquals(1205.0, event.triggerPrice, 0.01)
        assertEquals(4, event.freedSlotIndex)
        // Profit = (1200 - 1000) * 50 = +10,000
        assertEquals(10000.0, event.pnlAmount, 0.01)
        assertEquals(20.0, event.pnlPct, 0.01)

        val updatedStock = updatedItems.first()
        assertTrue(updatedStock.isClosed)
        assertEquals("TARGET_HIT", updatedStock.exitReason)
        assertEquals(1200.0, updatedStock.closedPrice, 0.01)
        assertFalse(updatedStock.isStrategySlotOccupied) // Slot is freed!
    }

    @Test
    fun testAutomatedExitOnStopLossHit() {
        val repo = ArjunStockRepository()
        val item = ArjunStockItem(
            id = "sl-hit-item",
            symbol = "LOSER",
            entryPrice = 500.0,
            currentPrice = 445.0, // Below Stop Loss (450.0)
            targetPrice1 = 600.0,
            stopLoss = 450.0,
            isApprovedForStrategy = true,
            strategySlotIndex = 5,
            quantity = 100 // 100 * 500 = 50,000
        )

        val (updatedItems, events) = repo.evaluateStrategyTriggers(listOf(item))
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("STOP_LOSS_HIT", event.eventType)
        assertEquals("LOSER", event.symbol)
        assertEquals(445.0, event.triggerPrice, 0.01)
        assertEquals(5, event.freedSlotIndex)
        // Loss = (450 - 500) * 100 = -5,000
        assertEquals(-5000.0, event.pnlAmount, 0.01)
        assertEquals(-10.0, event.pnlPct, 0.01)

        val updatedStock = updatedItems.first()
        assertTrue(updatedStock.isClosed)
        assertEquals("STOP_LOSS_HIT", updatedStock.exitReason)
        assertEquals(450.0, updatedStock.closedPrice, 0.01)
        assertFalse(updatedStock.isStrategySlotOccupied) // Slot is freed!
    }

    @Test
    fun testNoTriggerWhenPriceIsWithinBounds() {
        val repo = ArjunStockRepository()
        val item = ArjunStockItem(
            id = "safe-item",
            symbol = "STEADY",
            entryPrice = 100.0,
            currentPrice = 105.0, // Safe between SL 90 and T1 120
            targetPrice1 = 120.0,
            stopLoss = 90.0,
            isApprovedForStrategy = true,
            strategySlotIndex = 2,
            quantity = 500
        )

        val (updatedItems, events) = repo.evaluateStrategyTriggers(listOf(item))
        assertTrue(events.isEmpty())
        assertFalse(updatedItems.first().isClosed)
        assertTrue(updatedItems.first().isStrategySlotOccupied)
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Strategy Portfolio Financial Calculations
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testStrategySummaryFinancialAccounting() {
        val repo = ArjunStockRepository()
        // 2 active slots (~50k each) + 1 closed slot (+5k profit)
        val items = listOf(
            ArjunStockItem(
                id = "1",
                symbol = "WIN1",
                entryPrice = 1000.0,
                currentPrice = 1100.0, // +5k unrealized
                quantity = 50, // 50k deployed
                isApprovedForStrategy = true,
                strategySlotIndex = 1
            ),
            ArjunStockItem(
                id = "2",
                symbol = "FLAT2",
                entryPrice = 500.0,
                currentPrice = 500.0,
                quantity = 100, // 50k deployed
                isApprovedForStrategy = true,
                strategySlotIndex = 2
            ),
            ArjunStockItem(
                id = "3",
                symbol = "CLOSED3",
                entryPrice = 250.0,
                currentPrice = 300.0,
                quantity = 200, // was 50k deployed
                isApprovedForStrategy = true,
                strategySlotIndex = 3,
                isClosed = true,
                closedPrice = 300.0, // +10,000 realized profit
                exitReason = "TARGET_HIT"
            )
        )

        val summary = repo.calculateStrategySummary(items)
        assertEquals(500000.0, summary.totalFund, 0.01)
        assertEquals(2, summary.occupiedSlotsCount)
        assertEquals(8, summary.availableSlotsCount)
        assertEquals(100000.0, summary.totalDeployedCapital, 0.01) // 50k + 50k active
        assertEquals(105000.0, summary.currentDeployedValue, 0.01) // 55k + 50k
        assertEquals(5000.0, summary.unrealizedPnl, 0.01)
        assertEquals(10000.0, summary.realizedPnl, 0.01)
        // Cash = 500,000 - 100,000 + 10,000 = 410,000
        assertEquals(410000.0, summary.availableCash, 0.01)
        // NAV = 410,000 + 105,000 = 515,000
        assertEquals(515000.0, summary.totalStrategyNav, 0.01)
        assertEquals(15000.0, summary.totalPnl, 0.01)
        assertEquals(3.0, summary.totalStrategyReturnPct, 0.01)
    }

    // ─────────────────────────────────────────────────────────────
    // 9. ViewModel Strategy Operations
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testViewModelStrategyApproveAndDismissEvent() = runTest {
        val vm = VaralakshmiViewModel(autoRefresh = false)
        val state = vm.arjunStocksState.value
        assertEquals(10, state.strategySlots.size)
        assertNotNull(state.strategySummary)

        // Unapprove first, then approve into Slot 6
        val targetId = state.recommendations.first().id
        vm.unapproveArjunStock(targetId)
        advanceUntilIdle()

        vm.approveArjunStock(targetId, targetSlot = 6)
        advanceUntilIdle()

        val afterApprove = vm.arjunStocksState.value
        val item = afterApprove.recommendations.find { it.id == targetId }
        assertNotNull(item)
        assertTrue(item!!.isApprovedForStrategy)
        assertEquals(6, item.strategySlotIndex)

        // Dismiss execution event
        vm.dismissAutoExecutionEvent("evt-123")
        advanceUntilIdle()
        assertFalse(vm.arjunStocksState.value.autoExecutionEvents.any { it.id == "evt-123" })
    }
}

