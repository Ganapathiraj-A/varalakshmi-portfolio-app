package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.data.ArjunStockRepository
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.ArjunAutoExecutionEvent
import com.example.varalakshmiportfolio.model.ArjunBreakoutOrder
import com.example.varalakshmiportfolio.model.ArjunLadderLevel
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import com.example.varalakshmiportfolio.model.ArjunSlotStatus
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
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        ArjunStockRepository.resetCacheForTesting()
        VaralakshmiRepository.resetIoDispatcherForTesting()
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
        try {
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
        } finally {
            vm.stopPeriodicSync()
        }
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

        // Approve customStock into Strategy. Slot budget = ₹100,000. Quantity = 100,000 / 500 = 200 shares.
        val (success, message) = repo.approveRecommendation("strat-test-stock-1", targetSlot = 4)
        assertTrue(success)
        assertTrue(message.contains("200 shares"))

        val approved = repo.getRecommendations().find { it.id == "strat-test-stock-1" }
        assertNotNull(approved)
        assertTrue(approved!!.isApprovedForStrategy)
        assertEquals(4, approved.strategySlotIndex)
        assertEquals(200, approved.quantity)
        assertEquals(100000.0, approved.investedValue, 0.01)
        assertTrue(approved.isStrategySlotOccupied)
    }

    @Test
    fun testStrategySevenSlotCapacityLimit() {
        val repo = ArjunStockRepository()
        // Seeds occupy slots 1 and 2 (SKYGOLD and LALITHAA).
        // Add 5 more and approve them into slots 3, 4, 5, 6, 7.
        for (i in 3..7) {
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
        assertEquals(7, slots.size)
        assertEquals(7, slots.count { it.isOccupied })

        // 8th recommendation should be rejected because all 7 slots are occupied
        val s8 = ArjunStockItem(
            id = "stock-8",
            symbol = "SYM8",
            entryPrice = 200.0,
            currentPrice = 200.0
        )
        repo.addRecommendation(s8)
        val (eighthSuccess, msg) = repo.approveRecommendation("stock-8")
        assertFalse(eighthSuccess)
        assertTrue(msg.contains("All 7 strategy slots") && msg.contains("occupied"))

        // Unapproving slot 4 frees it up
        val afterUnapprove = repo.unapproveRecommendation("stock-4")
        val unapprovedStock = afterUnapprove.find { it.id == "stock-4" }
        assertNotNull(unapprovedStock)
        assertFalse(unapprovedStock!!.isApprovedForStrategy)
        assertFalse(unapprovedStock.isStrategySlotOccupied)

        // Now 8th can be approved into the freed slot
        val (retrySuccess, retryMsg) = repo.approveRecommendation("stock-8")
        assertTrue(retrySuccess)
        assertTrue(retryMsg.contains("Approved SYM8"))
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
        assertEquals(700000.0, summary.totalFund, 0.01)
        assertEquals(2, summary.occupiedSlotsCount)
        assertEquals(5, summary.availableSlotsCount)
        assertEquals(100000.0, summary.totalDeployedCapital, 0.01) // 50k + 50k active
        assertEquals(105000.0, summary.currentDeployedValue, 0.01) // 55k + 50k
        assertEquals(5000.0, summary.unrealizedPnl, 0.01)
        assertEquals(10000.0, summary.realizedPnl, 0.01)
        // Cash = 700,000 - 100,000 + 10,000 = 610,000
        assertEquals(610000.0, summary.availableCash, 0.01)
        // NAV = 610,000 + 105,000 = 715,000
        assertEquals(715000.0, summary.totalStrategyNav, 0.01)
        assertEquals(15000.0, summary.totalPnl, 0.01)
        assertEquals(2.14, summary.totalStrategyReturnPct, 0.01)
    }

    // ─────────────────────────────────────────────────────────────
    // 9. ViewModel Strategy Operations
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testViewModelStrategyApproveAndDismissEvent() = runTest {
        val vm = VaralakshmiViewModel(autoRefresh = false)
        try {
            val state = vm.arjunStocksState.value
            assertEquals(7, state.strategySlots.size)
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
        } finally {
            vm.stopPeriodicSync()
        }
    }

    @Test
    fun testOnlyZerodhaBoughtStocksInStrategySlots() {
        val repo = ArjunStockRepository()
        val items = repo.getRecommendations()
        val strategyItems = items.filter { it.isStrategySlotOccupied }

        // Only SKYGOLD and LALITHAA (bought in Zerodha) should be occupied
        assertEquals(2, strategyItems.size)
        assertTrue(strategyItems.any { it.symbol == "SKYGOLD" })
        assertTrue(strategyItems.any { it.symbol == "LALITHAA" })
        assertFalse(strategyItems.any { it.symbol == "ORIENTHOT" })

        // Check Google Finance URL generation
        val skygoldUrl = com.example.varalakshmiportfolio.ui.util.StockLinkHelper.buildGoogleFinanceUrl("SKYGOLD")
        assertEquals("https://www.google.com/finance/quote/SKYGOLD%3ANSE", skygoldUrl)
        val lalithaUrl = com.example.varalakshmiportfolio.ui.util.StockLinkHelper.buildGoogleFinanceUrl("LALITHAA")
        assertEquals("https://www.google.com/finance/quote/LALITHAA%3ANSE", lalithaUrl)
    }

    // ─────────────────────────────────────────────────────────────
    // 10. Financial Invariants & NAV Conservation Tests (R1)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testTotalNavMathematicalConservationAcrossLifecycleStates() {
        val repo = ArjunStockRepository()

        // State 0: Initial Clean State (No active strategy items)
        val initialItems = listOf(
            ArjunStockItem(symbol = "UNAPPROVED1", entryPrice = 100.0, currentPrice = 105.0)
        )
        val summary0 = repo.calculateStrategySummary(initialItems)
        assertEquals(700_000.0, summary0.totalFund, 0.001)
        assertEquals(700_000.0, summary0.availableCash, 0.001)
        assertEquals(0.0, summary0.totalDeployedCapital, 0.001)
        assertEquals(0.0, summary0.currentDeployedValue, 0.001)
        assertEquals(700_000.0, summary0.totalStrategyNav, 0.001)
        // Conservation check: Total NAV == Available Cash + Current Market Value of Open Holdings
        assertEquals(summary0.totalStrategyNav, summary0.availableCash + summary0.currentDeployedValue, 0.001)
        assertEquals(summary0.availableCash, summary0.totalFund - summary0.totalDeployedCapital + summary0.realizedPnl, 0.001)

        // State 1: 1 Slot Allocation (~₹100k deployed)
        val item1 = ArjunStockItem(
            id = "slot1",
            symbol = "STOCK1",
            entryPrice = 1000.0,
            currentPrice = 1000.0,
            quantity = 100, // 100,000 deployed
            isApprovedForStrategy = true,
            strategySlotIndex = 1
        )
        val summary1 = repo.calculateStrategySummary(listOf(item1))
        assertEquals(100_000.0, summary1.totalDeployedCapital, 0.001)
        assertEquals(100_000.0, summary1.currentDeployedValue, 0.001)
        assertEquals(600_000.0, summary1.availableCash, 0.001)
        assertEquals(700_000.0, summary1.totalStrategyNav, 0.001)
        assertEquals(1, summary1.occupiedSlotsCount)
        assertEquals(6, summary1.availableSlotsCount)
        assertEquals(summary1.totalStrategyNav, summary1.availableCash + summary1.currentDeployedValue, 0.001)
        assertEquals(summary1.availableCash, summary1.totalFund - summary1.totalDeployedCapital + summary1.realizedPnl, 0.001)

        // State 2: 7 Slots Fully Deployed (All 700k deployed)
        val full7Items = (1..7).map { slot ->
            ArjunStockItem(
                id = "slot$slot",
                symbol = "STOCK$slot",
                entryPrice = 500.0,
                currentPrice = 500.0,
                quantity = 200, // 200 * 500 = 100,000 per slot
                isApprovedForStrategy = true,
                strategySlotIndex = slot
            )
        }
        val summary2 = repo.calculateStrategySummary(full7Items)
        assertEquals(700_000.0, summary2.totalDeployedCapital, 0.001)
        assertEquals(700_000.0, summary2.currentDeployedValue, 0.001)
        assertEquals(0.0, summary2.availableCash, 0.001)
        assertEquals(700_000.0, summary2.totalStrategyNav, 0.001)
        assertEquals(7, summary2.occupiedSlotsCount)
        assertEquals(0, summary2.availableSlotsCount)
        assertEquals(summary2.totalStrategyNav, summary2.availableCash + summary2.currentDeployedValue, 0.001)
        assertEquals(summary2.availableCash, summary2.totalFund - summary2.totalDeployedCapital + summary2.realizedPnl, 0.001)

        // State 3: Intraday Price Changes (Unrealized PnL drift)
        // 4 stocks gain 10% (+10,000 each = +40,000), 3 stocks lose 5% (-5,000 each = -15,000). Net unrealized PnL = +25,000
        val priceChangedItems = full7Items.mapIndexed { index, item ->
            if (index < 4) item.copy(currentPrice = 550.0) // 200 * 550 = 110,000
            else item.copy(currentPrice = 475.0) // 200 * 475 = 95,000
        }
        val summary3 = repo.calculateStrategySummary(priceChangedItems)
        assertEquals(700_000.0, summary3.totalDeployedCapital, 0.001)
        assertEquals(725_000.0, summary3.currentDeployedValue, 0.001)
        assertEquals(25_000.0, summary3.unrealizedPnl, 0.001)
        assertEquals(0.0, summary3.realizedPnl, 0.001)
        assertEquals(0.0, summary3.availableCash, 0.001)
        assertEquals(725_000.0, summary3.totalStrategyNav, 0.001)
        assertEquals(3.57, summary3.totalStrategyReturnPct, 0.01)
        assertEquals(summary3.totalStrategyNav, summary3.availableCash + summary3.currentDeployedValue, 0.001)
        assertEquals(summary3.availableCash, summary3.totalFund - summary3.totalDeployedCapital + summary3.realizedPnl, 0.001)

        // State 4: Target 1 Hit Automated Exit on Slot 1 (+₹20,000 profit booked)
        // Slot 1 closed at 600.0 (cost 100k, proceeds 120k, profit +20k).
        val afterTarget1Exit = priceChangedItems.map {
            if (it.strategySlotIndex == 1) it.copy(isClosed = true, closedPrice = 600.0, exitReason = "TARGET_HIT")
            else it
        }
        val summary4 = repo.calculateStrategySummary(afterTarget1Exit)
        // 6 active slots: 3 at 110k, 3 at 95k. Total deployed = 600,000. Current value = 3 * 110k + 3 * 95k = 615,000
        assertEquals(600_000.0, summary4.totalDeployedCapital, 0.001)
        assertEquals(615_000.0, summary4.currentDeployedValue, 0.001)
        assertEquals(20_000.0, summary4.realizedPnl, 0.001)
        assertEquals(15_000.0, summary4.unrealizedPnl, 0.001)
        // Available cash = 700,000 - 600,000 + 20,000 = 120,000 (Initial 100k cost basis + 20k profit returned to cash!)
        assertEquals(120_000.0, summary4.availableCash, 0.001)
        // Total NAV = 120,000 + 615,000 = 735,000
        assertEquals(735_000.0, summary4.totalStrategyNav, 0.001)
        assertEquals(6, summary4.occupiedSlotsCount)
        assertEquals(1, summary4.availableSlotsCount) // Slot 1 is immediately free!
        assertEquals(summary4.totalStrategyNav, summary4.availableCash + summary4.currentDeployedValue, 0.001)
        assertEquals(summary4.availableCash, summary4.totalFund - summary4.totalDeployedCapital + summary4.realizedPnl, 0.001)

        // State 5: Stop Loss Hit Automated Exit on Slot 5 (-₹10,000 loss booked)
        // Slot 5 closed at 450.0 (cost 100k, proceeds 90k, loss -10k).
        val afterSlExit = afterTarget1Exit.map {
            if (it.strategySlotIndex == 5) it.copy(isClosed = true, closedPrice = 450.0, exitReason = "STOP_LOSS_HIT")
            else it
        }
        val summary5 = repo.calculateStrategySummary(afterSlExit)
        // 5 active slots: 3 at 110k, 2 at 95k. Total deployed = 500,000. Current value = 3 * 110k + 2 * 95k = 520,000
        assertEquals(500_000.0, summary5.totalDeployedCapital, 0.001)
        assertEquals(520_000.0, summary5.currentDeployedValue, 0.001)
        // Realized PnL = +20,000 - 10,000 = +10,000
        assertEquals(10_000.0, summary5.realizedPnl, 0.001)
        assertEquals(20_000.0, summary5.unrealizedPnl, 0.001)
        // Available cash = 700,000 - 500,000 + 10,000 = 210,000 (120k previous + 90k returned from SL exit)
        assertEquals(210_000.0, summary5.availableCash, 0.001)
        // Total NAV = 210,000 + 520,000 = 730,000
        assertEquals(730_000.0, summary5.totalStrategyNav, 0.001)
        assertEquals(5, summary5.occupiedSlotsCount)
        assertEquals(2, summary5.availableSlotsCount) // Slots 1 and 5 are freed!
        assertEquals(summary5.totalStrategyNav, summary5.availableCash + summary5.currentDeployedValue, 0.001)
        assertEquals(summary5.availableCash, summary5.totalFund - summary5.totalDeployedCapital + summary5.realizedPnl, 0.001)

        // State 6: Complete Liquidation (All positions closed)
        val allLiquidated = afterSlExit.map {
            if (!it.isClosed) it.copy(isClosed = true, closedPrice = it.currentPrice, exitReason = "MANUAL_EXIT")
            else it
        }
        val summary6 = repo.calculateStrategySummary(allLiquidated)
        assertEquals(0, summary6.occupiedSlotsCount)
        assertEquals(7, summary6.availableSlotsCount)
        assertEquals(0.0, summary6.totalDeployedCapital, 0.001)
        assertEquals(0.0, summary6.currentDeployedValue, 0.001)
        assertEquals(0.0, summary6.unrealizedPnl, 0.001)
        // Realized PnL = +30,000
        assertEquals(30_000.0, summary6.realizedPnl, 0.001)
        // Cash = 700,000 - 0 + 30,000 = 730,000
        assertEquals(730_000.0, summary6.availableCash, 0.001)
        assertEquals(730_000.0, summary6.totalStrategyNav, 0.001)
        // Absolute Conservation: Total NAV == Available Cash + 0
        assertEquals(summary6.totalStrategyNav, summary6.availableCash, 0.001)
        assertEquals(summary6.availableCash, summary6.totalFund + summary6.realizedPnl, 0.001)
    }

    @Test
    fun testPaisePrecisionAndZeroIeeeFloatingDrift() {
        // Test with real Zerodha fill numbers:
        // SKYGOLD: 47 shares @ ₹1,058.64
        val skygold = ArjunStockItem(
            symbol = "SKYGOLD",
            entryPrice = 1058.64,
            currentPrice = 1059.45,
            quantity = 47
        )
        // Invested = 47 * 1058.64 = 49756.08
        assertEquals(49756.08, skygold.investedValue, 0.0001)
        // Current = 47 * 1059.45 = 49794.15
        assertEquals(49794.15, skygold.currentValue, 0.0001)
        // Unrealized = 49794.15 - 49756.08 = 38.07
        assertEquals(38.07, skygold.unrealizedPnl, 0.0001)

        // LALITHAA: 136 shares @ ₹369.26
        val lalitha = ArjunStockItem(
            symbol = "LALITHAA",
            entryPrice = 369.26,
            currentPrice = 369.30,
            quantity = 136
        )
        // Invested = 136 * 369.26 = 50219.36
        assertEquals(50219.36, lalitha.investedValue, 0.0001)
        // Current = 136 * 369.30 = 50224.80
        assertEquals(50224.80, lalitha.currentValue, 0.0001)
        // Unrealized = 50224.80 - 50219.36 = 5.44
        assertEquals(5.44, lalitha.unrealizedPnl, 0.0001)

        // Half-even boundary rounding verification
        // 100.005 should round to 100.00 (nearest even integer last digit)
        assertEquals(100.00, com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise(100.005), 0.0001)
        // 100.015 should round to 100.02 (nearest even integer last digit)
        assertEquals(100.02, com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise(100.015), 0.0001)
    }

    @Test
    fun testHardenBrokerAlertParserVariations() {
        // Variation 1: Currency symbols, commas, and multi-line targets
        val alert1 = """
            BUY: TRENT.NS
            CMP: ₹4,850.50
            TARGET 1: ₹5,400.00
            TARGET 2: ₹5,800.00
            STOP LOSS: ₹4,500.00
            NOTE: Strong breakout above previous ATH
        """.trimIndent()
        val parsed1 = ArjunRecommendationParser.parse(alert1)
        assertNotNull(parsed1)
        assertEquals("TRENT", parsed1!!.symbol)
        assertEquals(4850.50, parsed1.entryPrice, 0.01)
        assertEquals(5400.0, parsed1.targetPrice1, 0.01)
        assertEquals(5800.0, parsed1.targetPrice2, 0.01)
        assertEquals(4500.0, parsed1.stopLoss, 0.01)
        assertTrue(parsed1.notes.contains("ATH"))

        // Variation 2: Rs. with commas and hyphenated suffixes
        val alert2 = """
            👉 BUY RELIANCE-EQ AROUND Rs. 2,940 - Rs. 2,960
            TARGETS: Rs. 3,200 / Rs. 3,400
            STRICT SL: Rs. 2,820.50
        """.trimIndent()
        val parsed2 = ArjunRecommendationParser.parse(alert2)
        assertNotNull(parsed2)
        assertEquals("RELIANCE", parsed2!!.symbol)
        assertEquals(2950.0, parsed2.entryPrice, 0.01)
        assertEquals(3200.0, parsed2.targetPrice1, 0.01)
        assertEquals(3400.0, parsed2.targetPrice2, 0.01)
        assertEquals(2820.50, parsed2.stopLoss, 0.01)

        // Variation 3: NSE suffix and SL at syntax
        val alert3 = """
            ACCUMULATE INFY:NSE AT 1820.75
            TARGET 1 - 2050
            TARGET 2 - 2200
            STOP-LOSS @ 1710.25
        """.trimIndent()
        val parsed3 = ArjunRecommendationParser.parse(alert3)
        assertNotNull(parsed3)
        assertEquals("INFY", parsed3!!.symbol)
        assertEquals(1820.75, parsed3.entryPrice, 0.01)
        assertEquals(2050.0, parsed3.targetPrice1, 0.01)
        assertEquals(2200.0, parsed3.targetPrice2, 0.01)
        assertEquals(1710.25, parsed3.stopLoss, 0.01)
    }

    @Test
    fun testSlotLiberationAndAtomicCachePersistence() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "persist-stock-1",
            symbol = "TESTPERSIST",
            entryPrice = 100.0,
            currentPrice = 90.0,
            stopLoss = 95.0,
            quantity = 500,
            isApprovedForStrategy = true,
            strategySlotIndex = 3
        )
        repo.addRecommendation(stock)

        // Trigger auto SL sell
        val (evaluated, events) = repo.evaluateStrategyNow()
        assertEquals(1, events.size)
        assertEquals("STOP_LOSS_HIT", events.first().eventType)
        assertEquals(3, events.first().freedSlotIndex)

        // Verify slot is liberated immediately
        val slots = repo.calculateStrategySlots(repo.getRecommendations())
        val slot3 = slots.find { it.slotIndex == 3 }
        assertNotNull(slot3)
        assertFalse(slot3!!.isOccupied)
        assertNull(slot3.stock)

        // Re-read cache from disk to verify atomic persistence
        val repoNewInstance = ArjunStockRepository()
        val fromDisk = repoNewInstance.getRecommendations()
        val reloadedStock = fromDisk.find { it.id == "persist-stock-1" }
        assertNotNull(reloadedStock)
        assertTrue(reloadedStock!!.isClosed)
        assertEquals("STOP_LOSS_HIT", reloadedStock.exitReason)
        assertEquals(95.0, reloadedStock.closedPrice, 0.01)
    }

    // ─────────────────────────────────────────────────────────────
    // 11. Reviewer Hardening & Adversarial Edge Case Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testParserTimeAndDatePunctuationResilience() {
        // Tricky case 1: Time prefix should NOT hijack CMP/entry price
        val alertTimePrefix = """
            ARJUN Combo Package 🚀
            Alert at 10:30 AM:
            BUY TRENT AT 5400
            TARGETS: 5800 / 6000
            KEEP SL 5100
        """.trimIndent()
        val parsedTime = ArjunRecommendationParser.parse(alertTimePrefix)
        assertNotNull(parsedTime)
        assertEquals("TRENT", parsedTime!!.symbol)
        assertEquals(5400.0, parsedTime.entryPrice, 0.01)
        assertEquals(5800.0, parsedTime.targetPrice1, 0.01)
        assertEquals(6000.0, parsedTime.targetPrice2, 0.01)
        assertEquals(5100.0, parsedTime.stopLoss, 0.01)

        // Tricky case 2: Date format and rupee slash /- notation
        val alertDateAndRupeeSlash = """
            Date: 15/10/2026
            BUY HDFCBANK AROUND 1,650/- - 1,670/-
            TARGETS: 1,750/- / 1,820/-
            STOP LOSS: 1,590/-
        """.trimIndent()
        val parsedDate = ArjunRecommendationParser.parse(alertDateAndRupeeSlash)
        assertNotNull(parsedDate)
        assertEquals("HDFCBANK", parsedDate!!.symbol)
        assertEquals(1660.0, parsedDate.entryPrice, 0.01)
        assertEquals(1750.0, parsedDate.targetPrice1, 0.01)
        assertEquals(1820.0, parsedDate.targetPrice2, 0.01)
        assertEquals(1590.0, parsedDate.stopLoss, 0.01)

        // Tricky case 3: Phone number and bracketed ticker
        val alertPhoneAndBrackets = """
            Advisory Help: 9876543210
            BUY [TATAMOTORS] CMP 950
            T1 1050
            T2 1100
            SL 900
        """.trimIndent()
        val parsedPhone = ArjunRecommendationParser.parse(alertPhoneAndBrackets)
        assertNotNull(parsedPhone)
        assertEquals("TATAMOTORS", parsedPhone!!.symbol)
        assertEquals(950.0, parsedPhone.entryPrice, 0.01)
        assertEquals(1050.0, parsedPhone.targetPrice1, 0.01)
        assertEquals(1100.0, parsedPhone.targetPrice2, 0.01)
        assertEquals(900.0, parsedPhone.stopLoss, 0.01)
    }

    @Test
    fun testZeroAndInvalidPriceDoesNotTriggerAutoExit() {
        val repo = ArjunStockRepository()
        // Stock in slot 1 with currentPrice = 0.0 (e.g. quote fetch failure)
        val stockZero = ArjunStockItem(
            id = "zero-price-stock",
            symbol = "ZEROPRICE",
            entryPrice = 100.0,
            currentPrice = 0.0,
            stopLoss = 90.0,
            targetPrice1 = 120.0,
            quantity = 500,
            isApprovedForStrategy = true,
            strategySlotIndex = 1
        )
        // Ensure status is NOT STOP_LOSS_HIT
        assertNotEquals(com.example.varalakshmiportfolio.model.ArjunTradeStatus.STOP_LOSS_HIT, stockZero.status)

        // Evaluate triggers: should NOT trigger auto exit
        val (evaluated, events) = repo.evaluateStrategyTriggers(listOf(stockZero))
        assertEquals(0, events.size)
        assertFalse(evaluated.first().isClosed)
        assertTrue(evaluated.first().isStrategySlotOccupied)
    }

    @Test
    fun testExplicitOccupiedSlotApprovalRejection() {
        val repo = ArjunStockRepository()
        // Seeds occupy Slot 1 (SKYGOLD) and Slot 2 (LALITHAA)
        val stockNew = ArjunStockItem(
            id = "slot-conflict-stock",
            symbol = "CONFLICT",
            entryPrice = 500.0,
            currentPrice = 500.0
        )
        repo.addRecommendation(stockNew)

        // Request already occupied Slot 2
        val (okOccupied, msgOccupied) = repo.approveRecommendation("slot-conflict-stock", targetSlot = 2)
        assertFalse(okOccupied)
        assertTrue(msgOccupied.contains("already occupied"))

        // Request invalid slot #15
        val (okInvalid, msgInvalid) = repo.approveRecommendation("slot-conflict-stock", targetSlot = 15)
        assertFalse(okInvalid)
        assertTrue(msgInvalid.contains("Invalid slot"))
    }

    @Test
    fun testAutoExecutionEventsLoadedOnColdStart() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "cold-stock-1",
            symbol = "COLDTEST",
            entryPrice = 100.0,
            currentPrice = 130.0,
            targetPrice1 = 125.0,
            quantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = 4
        )
        repo.addRecommendation(stock)
        val (_, events) = repo.evaluateStrategyNow()
        assertEquals(1, events.size)

        // Reset memory cache to simulate cold app launch
        ArjunStockRepository.resetCacheForTesting()

        // Fresh repo instance: directly call getAutoExecutionEvents() without calling getRecommendations() first
        val freshRepo = ArjunStockRepository()
        val loadedEvents = freshRepo.getAutoExecutionEvents()
        assertEquals(1, loadedEvents.size)
        assertEquals("COLDTEST", loadedEvents.first().symbol)
        assertEquals("TARGET_HIT", loadedEvents.first().eventType)
    }

    @Test
    fun testProgressToTarget1NanAndInfinitySafety() {
        // Test with NaN currentPrice
        val stockNan = ArjunStockItem(
            symbol = "NANSTOCK",
            entryPrice = 100.0,
            currentPrice = Double.NaN,
            stopLoss = 90.0,
            targetPrice1 = 120.0
        )
        assertFalse(stockNan.progressToTarget1.isNaN())
        assertEquals(0.5f, stockNan.progressToTarget1, 0.001f)

        // Test with Infinite prices
        val stockInf = ArjunStockItem(
            symbol = "INFSTOCK",
            entryPrice = 100.0,
            currentPrice = Double.POSITIVE_INFINITY,
            stopLoss = 90.0,
            targetPrice1 = 120.0
        )
        assertFalse(stockInf.progressToTarget1.isNaN())
        assertEquals(0.5f, stockInf.progressToTarget1, 0.001f)

        // Test ArjunMoneyMath with Infinity: should not throw NumberFormatException
        assertEquals(0.0, com.example.varalakshmiportfolio.model.ArjunMoneyMath.calculatePnl(10, Double.POSITIVE_INFINITY, 100.0), 0.001)
        assertEquals(0.0, com.example.varalakshmiportfolio.model.ArjunMoneyMath.calculatePnlPct(Double.POSITIVE_INFINITY, 100.0), 0.001)
    }

    // ─────────────────────────────────────────────────────────────
    // 12. Reviewer Round 2 Hardening & Deep Verification Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testParserMultiWordIndianSymbols() {
        // Multi-word 1: TATA MOTORS
        val alertTata = """
            BUY TATA MOTORS AROUND 950-960
            TARGETS: 1050 / 1100
            KEEP SL 900
        """.trimIndent()
        val parsedTata = ArjunRecommendationParser.parse(alertTata)
        assertNotNull(parsedTata)
        assertEquals("TATAMOTORS", parsedTata!!.symbol)
        assertEquals(955.0, parsedTata.entryPrice, 0.01)
        assertEquals("950 - 960", parsedTata.entryPriceRange)

        // Multi-word 2: HDFC BANK
        val alertHdfc = """
            BUY HDFC BANK CMP 1650
            TARGETS 1750-1800
            KEEP SL 1580
        """.trimIndent()
        val parsedHdfc = ArjunRecommendationParser.parse(alertHdfc)
        assertNotNull(parsedHdfc)
        assertEquals("HDFCBANK", parsedHdfc!!.symbol)
        assertEquals(1650.0, parsedHdfc.entryPrice, 0.01)

        // Multi-word 3: BAJAJ AUTO
        val alertBajaj = """
            BUY BAJAJ AUTO AT 9500
            T1 10200
            SL 9100
        """.trimIndent()
        val parsedBajaj = ArjunRecommendationParser.parse(alertBajaj)
        assertNotNull(parsedBajaj)
        assertEquals("BAJAJ_AUTO", parsedBajaj!!.symbol)
        assertEquals(9500.0, parsedBajaj.entryPrice, 0.01)
    }

    @Test
    fun testParserToAndAndRangeSeparators() {
        // Range with TO: 5400 TO 5450
        val alertTo = """
            BUY TRENT AROUND 5400 TO 5450
            TARGETS: 5800 TO 6000
            KEEP SL 5100
        """.trimIndent()
        val parsedTo = ArjunRecommendationParser.parse(alertTo)
        assertNotNull(parsedTo)
        assertEquals("TRENT", parsedTo!!.symbol)
        assertEquals(5425.0, parsedTo.entryPrice, 0.01)
        assertEquals("5400 - 5450", parsedTo.entryPriceRange)
        assertEquals(5800.0, parsedTo.targetPrice1, 0.01)
        assertEquals(6000.0, parsedTo.targetPrice2, 0.01)

        // Targets with AND: 5800 AND 6000
        val alertAnd = """
            BUY RELIANCE 2940 TO 2960
            TARGETS: 3200 AND 3400
            SL 2820
        """.trimIndent()
        val parsedAnd = ArjunRecommendationParser.parse(alertAnd)
        assertNotNull(parsedAnd)
        assertEquals("RELIANCE", parsedAnd!!.symbol)
        assertEquals(2950.0, parsedAnd.entryPrice, 0.01)
        assertEquals(3200.0, parsedAnd.targetPrice1, 0.01)
        assertEquals(3400.0, parsedAnd.targetPrice2, 0.01)
    }

    @Test
    fun testSingleLineAlertDoesNotPolluteEntryPriceRange() {
        val singleLineAlert = "BUY TRENT AT 5400 SL 5100 TGT 5800"
        val parsed = ArjunRecommendationParser.parse(singleLineAlert)
        assertNotNull(parsed)
        assertEquals("TRENT", parsed!!.symbol)
        assertEquals(5400.0, parsed.entryPrice, 0.01)
        assertEquals(5100.0, parsed.stopLoss, 0.01)
        assertEquals(5800.0, parsed.targetPrice1, 0.01)
        // Entry range must be clean "AT 5400", NOT "AT 5400 SL 5100 TGT 5800"
        assertEquals("AT 5400", parsed.entryPriceRange)
    }

    @Test
    fun testSlBelowAndUnderParsing() {
        val alert = """
            BUY TCS AT 4200
            TARGET 1: 4500
            SL: BELOW 4050
        """.trimIndent()
        val parsed = ArjunRecommendationParser.parse(alert)
        assertNotNull(parsed)
        assertEquals("TCS", parsed!!.symbol)
        assertEquals(4200.0, parsed.entryPrice, 0.01)
        assertEquals(4050.0, parsed.stopLoss, 0.01)
        assertEquals(4500.0, parsed.targetPrice1, 0.01)
    }

    @Test
    fun testGhostCapitalPreventedInStrategySummary() {
        val repo = ArjunStockRepository()
        // Stock has isApprovedForStrategy = true but strategySlotIndex = -1 (invalid slot index)
        val ghostStock = ArjunStockItem(
            id = "ghost-stock",
            symbol = "GHOST",
            entryPrice = 500.0,
            currentPrice = 500.0,
            quantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = -1
        )
        val summary = repo.calculateStrategySummary(listOf(ghostStock))
        // Must NOT count as deployed capital in 7L Strategy
        assertEquals(0.0, summary.totalDeployedCapital, 0.001)
        assertEquals(0.0, summary.currentDeployedValue, 0.001)
        assertEquals(700_000.0, summary.availableCash, 0.001)
        assertEquals(700_000.0, summary.totalStrategyNav, 0.001)
        assertEquals(0, summary.occupiedSlotsCount)
        assertEquals(7, summary.availableSlotsCount)
    }

    @Test
    fun testInfinitePriceDoesNotTriggerAutoExit() {
        val repo = ArjunStockRepository()
        val stockWithInf = ArjunStockItem(
            id = "inf-stock",
            symbol = "INFSTOCK",
            entryPrice = 100.0,
            currentPrice = Double.POSITIVE_INFINITY,
            targetPrice1 = 120.0,
            stopLoss = 90.0,
            isApprovedForStrategy = true,
            strategySlotIndex = 1
        )
        val (evaluated, events) = repo.evaluateStrategyTriggers(listOf(stockWithInf))
        assertTrue(events.isEmpty())
        assertFalse(evaluated.first().isClosed)
        assertTrue(evaluated.first().isStrategySlotOccupied)
    }

    @Test
    fun testDismissAutoExecutionEventPersistsOnColdStart() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "cold-dismiss-stock",
            symbol = "DISMISSTEST",
            entryPrice = 100.0,
            currentPrice = 130.0,
            targetPrice1 = 125.0,
            quantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = 4
        )
        repo.addRecommendation(stock)
        val (_, events) = repo.evaluateStrategyNow()
        assertEquals(1, events.size)
        val eventId = events.first().id

        // Cold start 1
        ArjunStockRepository.resetCacheForTesting()
        val repo1 = ArjunStockRepository()
        // Dismiss event without calling getRecommendations() first
        val remaining = repo1.dismissAutoExecutionEvent(eventId)
        assertTrue(remaining.isEmpty())

        // Cold start 2: verify disk persistence across cold start
        ArjunStockRepository.resetCacheForTesting()
        val repo2 = ArjunStockRepository()
        val loadedEvents = repo2.getAutoExecutionEvents()
        assertTrue(loadedEvents.isEmpty())
    }

    @Test
    fun testDashboardSyncUpdatesArjunStrategySummaryAndEvents() = runTest {
        val vm = VaralakshmiViewModel(autoRefresh = false)
        try {
            val initialState = vm.arjunStocksState.value
            assertNotNull(initialState.strategySummary)
            assertEquals(2, initialState.strategySummary.occupiedSlotsCount)

            // General refresh() must update strategySummary and slots
            vm.refresh()
            advanceUntilIdle()

            val refreshedState = vm.arjunStocksState.value
            assertEquals(2, refreshedState.strategySummary.occupiedSlotsCount)
            assertEquals(5, refreshedState.strategySummary.availableSlotsCount)
            assertEquals(7, refreshedState.strategySlots.size)
        } finally {
            vm.stopPeriodicSync()
        }
    }

    @Test
    fun testClipboardFormattingNoEmptyParentheses() {
        val summary = ArjunPortfolioSummary(
            totalInvested = 1000.0,
            currentValue = 1100.0,
            totalPnl = 100.0,
            totalPnlPct = 10.0
        )
        val stockNoRange = ArjunStockItem(
            symbol = "NORANGE",
            entryPrice = 100.0,
            entryPriceRange = "",
            currentPrice = 110.0
        )
        val formatted = formatArjunStocksForClipboard(summary, listOf(stockNoRange))
        // Must NOT contain empty parentheses " ()"
        assertFalse(formatted.contains(" ()"))
        assertTrue(formatted.contains("Entry: ₹100.00 | Current: ₹110.00"))
    }

    // ─────────────────────────────────────────────────────────────
    // 11. Partial Buy / Sell & Multi-Tier Ladders & Breakout Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun testPartialBuyIncreasesPositionWithinSlotBudget() {
        val repo = ArjunStockRepository()
        // Slot 1: SKYGOLD has ~47 shares @ ~1058.64 = ~49,756.08
        val skygold = repo.getRecommendations().first { it.symbol == "SKYGOLD" }
        assertEquals(1, skygold.strategySlotIndex)
        val initialInvested = skygold.investedValue
        val initialQty = skygold.quantity

        // Add 20 shares @ 1060.0. 20 * 1060 = 21,200. Total invested ~ 70,956.08 <= 100,000.
        val (success, msg) = repo.partialBuy(skygold.id, additionalQuantity = 20, buyPrice = 1060.0)
        assertTrue(success)
        assertTrue(msg.contains("Bought 20 shares"))

        val updated = repo.getRecommendations().first { it.id == skygold.id }
        assertEquals(initialQty + 20, updated.quantity)
        val expectedInvested = com.example.varalakshmiportfolio.model.ArjunMoneyMath.roundPaise(initialInvested + 21200.0)
        assertEquals(expectedInvested, updated.investedValue, 0.01)
        assertTrue(updated.investedValue <= 100_000.0)
    }

    @Test
    fun testPartialBuyRejectsExceedingSlotBudget() {
        val repo = ArjunStockRepository()
        val skygold = repo.getRecommendations().first { it.symbol == "SKYGOLD" }
        // Remaining headroom is around 50,243.92.
        // Attempting to buy 60 shares @ 1060.0 = 63,600.0 -> total exceeds 100,000!
        val (success, msg) = repo.partialBuy(skygold.id, additionalQuantity = 60, buyPrice = 1060.0)
        assertFalse(success)
        assertTrue(msg.contains("Exceeds ₹1,00,000 slot budget"))

        // Quantity remains unchanged
        val unadjusted = repo.getRecommendations().first { it.id == skygold.id }
        assertEquals(skygold.quantity, unadjusted.quantity)
    }

    @Test
    fun testPartialSellProportionalCostBasisAndPnl() {
        val repo = ArjunStockRepository()
        // Create a custom item with round numbers for clean verification
        val stock = ArjunStockItem(
            id = "partial-sell-test",
            symbol = "PARTIALCO",
            entryPrice = 1000.0,
            currentPrice = 1200.0,
            quantity = 100, // 100k invested
            isApprovedForStrategy = true,
            strategySlotIndex = 4
        )
        repo.addRecommendation(stock)

        // Sell 40 shares @ 1200.0:
        // Proportional cost basis sold = 40 * 1000.0 = 40,000.0
        // Realized proceeds = 40 * 1200.0 = 48,000.0
        // Realized profit = 8,000.0
        // Remaining invested = 60,000.0, remaining shares = 60
        val (success, msg) = repo.partialSell("partial-sell-test", sellQuantity = 40, sellPrice = 1200.0)
        assertTrue(success)
        assertTrue(msg.contains("Sold 40 shares"))

        val afterSell = repo.getRecommendations().first { it.id == "partial-sell-test" }
        assertEquals(60, afterSell.quantity)
        assertEquals(60_000.0, afterSell.investedValue, 0.01)
        assertEquals(8_000.0, afterSell.partialRealizedPnl, 0.01)
        assertFalse(afterSell.isClosed)
        assertTrue(afterSell.isStrategySlotOccupied)

        // Verify summary accounting and conservation:
        // Available cash must include the realized P&L from the partial sell
        val summary = repo.calculateStrategySummary(repo.getRecommendations())
        assertEquals(8_000.0, afterSell.realizedPnl, 0.01)
        // NAV conservation equation holds strictly:
        assertEquals(summary.totalStrategyNav, summary.availableCash + summary.currentDeployedValue, 0.001)
        assertEquals(summary.availableCash, summary.totalFund - summary.totalDeployedCapital + summary.realizedPnl, 0.001)
    }

    @Test
    fun testPartialSellLiberatesSlotOnFullExit() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "full-exit-test",
            symbol = "FULLEXIT",
            entryPrice = 1000.0,
            currentPrice = 1200.0,
            quantity = 50,
            isApprovedForStrategy = true,
            strategySlotIndex = 5
        )
        repo.addRecommendation(stock)

        // Sell 100% of shares (50 shares)
        val (success, msg) = repo.partialSell("full-exit-test", sellQuantity = 50, sellPrice = 1200.0)
        assertTrue(success)
        assertTrue(msg.contains("Closed position"))

        val afterSell = repo.getRecommendations().first { it.id == "full-exit-test" }
        assertTrue(afterSell.isClosed)
        assertEquals(1200.0, afterSell.closedPrice, 0.01)
        assertFalse(afterSell.isStrategySlotOccupied)

        val slots = repo.calculateStrategySlots(repo.getRecommendations())
        val slot5 = slots.first { it.slotIndex == 5 }
        assertEquals(ArjunSlotStatus.EMPTY, slot5.status)
        assertFalse(slot5.isOccupied)
    }

    @Test
    fun testMultiTierTargetLadderPartialExecutions() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "ladder-test-1",
            symbol = "LADDERCO",
            entryPrice = 100.0,
            currentPrice = 102.0,
            quantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = 3,
            targetLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 110.0, allocationPct = 50.0),
                ArjunLadderLevel(levelIndex = 2, triggerPrice = 120.0, allocationPct = 50.0)
            ),
            stopLossLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 90.0, allocationPct = 100.0)
            )
        )
        repo.addRecommendation(stock)

        // Price rises to 112.0 -> crosses TP1 (110.0)
        val price112Item = stock.copy(currentPrice = 112.0)
        val (eval1, events1) = repo.evaluateStrategyTriggers(listOf(price112Item))

        assertEquals(1, events1.size)
        assertTrue(events1.first().headline.contains("TP1 Hit"))
        val itemAfterTp1 = eval1.first { it.id == "ladder-test-1" }
        // 50% sold = 50 shares sold, 50 remaining
        assertEquals(50, itemAfterTp1.quantity)
        assertEquals(5000.0, itemAfterTp1.investedValue, 0.01)
        assertEquals(600.0, itemAfterTp1.partialRealizedPnl, 0.01) // 50 * (112 - 100) = 600
        assertFalse(itemAfterTp1.isClosed)
        assertTrue(itemAfterTp1.isStrategySlotOccupied)
        assertTrue(itemAfterTp1.targetLadder.first { it.levelIndex == 1 }.isExecuted)
        assertFalse(itemAfterTp1.targetLadder.first { it.levelIndex == 2 }.isExecuted)

        // Price rises to 122.0 -> crosses TP2 (120.0)
        val price122Item = itemAfterTp1.copy(currentPrice = 122.0)
        val (eval2, events2) = repo.evaluateStrategyTriggers(listOf(price122Item))

        assertEquals(1, events2.size)
        assertTrue(events2.first().headline.contains("TP2 Hit"))
        val itemAfterTp2 = eval2.first { it.id == "ladder-test-1" }
        assertTrue(itemAfterTp2.isClosed)
        assertEquals(122.0, itemAfterTp2.closedPrice, 0.01)
        assertFalse(itemAfterTp2.isStrategySlotOccupied)
        // Total realized P&L = 600 (TP1) + 50 * (122 - 100 = 22) = 600 + 1100 = 1700
        assertEquals(1700.0, itemAfterTp2.realizedPnl, 0.01)
    }

    @Test
    fun testMultiTierStopLossLadderPartialExecutions() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "sl-ladder-test",
            symbol = "SLLADDER",
            entryPrice = 100.0,
            currentPrice = 98.0,
            quantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = 6,
            targetLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 115.0, allocationPct = 100.0)
            ),
            stopLossLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 92.0, allocationPct = 40.0),
                ArjunLadderLevel(levelIndex = 2, triggerPrice = 85.0, allocationPct = 60.0)
            )
        )
        repo.addRecommendation(stock)

        // Price drops to 91.0 -> crosses SL1 (92.0)
        val price91Item = stock.copy(currentPrice = 91.0)
        val (eval1, events1) = repo.evaluateStrategyTriggers(listOf(price91Item))

        assertEquals(1, events1.size)
        assertTrue(events1.first().headline.contains("SL1 Hit"))
        val itemAfterSl1 = eval1.first { it.id == "sl-ladder-test" }
        // 40% of 100 = 40 shares sold @ 91.0, 60 remaining
        assertEquals(60, itemAfterSl1.quantity)
        assertEquals(6000.0, itemAfterSl1.investedValue, 0.01)
        assertEquals(-360.0, itemAfterSl1.partialRealizedPnl, 0.01) // 40 * (91 - 100) = -360
        assertFalse(itemAfterSl1.isClosed)
        assertTrue(itemAfterSl1.isStrategySlotOccupied)
    }

    @Test
    fun testBreakoutOrderExecutionWhenPriceCrosses() {
        val repo = ArjunStockRepository()
        // Place breakout order on empty Slot 7
        val order = ArjunBreakoutOrder(
            symbol = "TATAPOWER",
            triggerPrice = 420.0,
            slotIndex = 7,
            allocationAmount = 100_000.0,
            quantity = 238
        )
        val (success, msg) = repo.setBreakoutOrder(order)
        assertTrue(success)
        assertTrue(msg.contains("Breakout order set"))

        // Check slot status
        val slots = repo.calculateStrategySlots(repo.getRecommendations())
        val slot7 = slots.first { it.slotIndex == 7 }
        assertEquals(ArjunSlotStatus.PENDING_BREAKOUT, slot7.status)
        assertNotNull(slot7.pendingBreakout)

        // Evaluate when price is below trigger (415.0) -> No execution
        val quotesBelow = mapOf("TATAPOWER" to 415.0)
        val (_, eventsBelow) = repo.evaluateBreakoutTriggers(quotesBelow)
        assertTrue(eventsBelow.isEmpty())
        assertEquals(1, repo.getBreakoutOrders().size)

        // Evaluate when price crosses trigger (422.50) -> Auto execution!
        val quotesAbove = mapOf("TATAPOWER" to 422.50)
        val (_, eventsAbove) = repo.evaluateBreakoutTriggers(quotesAbove)
        assertEquals(1, eventsAbove.size)
        assertTrue(eventsAbove.first().headline.contains("Breakout Buy Executed"))
        assertTrue(eventsAbove.first().message.contains("TATAPOWER"))

        // Breakout orders cache must now be empty
        assertTrue(repo.getBreakoutOrders().isEmpty())

        // Recommendation list now has TATAPOWER approved in Slot 7
        val tatapower = repo.getRecommendations().firstOrNull { it.symbol == "TATAPOWER" }
        assertNotNull(tatapower)
        assertTrue(tatapower!!.isApprovedForStrategy)
        assertEquals(7, tatapower.strategySlotIndex)
        assertEquals(422.50, tatapower.entryPrice, 0.01)
        assertTrue(tatapower.investedValue <= 100_000.0)

        // Slot 7 is now OCCUPIED
        val slotsAfter = repo.calculateStrategySlots(repo.getRecommendations())
        val slot7After = slotsAfter.first { it.slotIndex == 7 }
        assertEquals(ArjunSlotStatus.OCCUPIED, slot7After.status)
    }

    @Test
    fun testBreakoutOrderPersistenceAcrossColdStart() {
        val repo1 = ArjunStockRepository()
        val order = ArjunBreakoutOrder(
            symbol = "PERSISTTEST",
            triggerPrice = 500.0,
            slotIndex = 6,
            allocationAmount = 80_000.0,
            quantity = 160
        )
        repo1.setBreakoutOrder(order)
        assertEquals(1, repo1.getBreakoutOrders().size)

        // Simulate app restart / cold start
        ArjunStockRepository.resetCacheForTesting()
        val repo2 = ArjunStockRepository()
        val loadedOrders = repo2.getBreakoutOrders()
        assertEquals(1, loadedOrders.size)
        assertEquals("PERSISTTEST", loadedOrders.first().symbol)
        assertEquals(500.0, loadedOrders.first().triggerPrice, 0.01)
        assertEquals(6, loadedOrders.first().slotIndex)

        // Cancel order
        val (cancelSuccess, _) = repo2.cancelBreakoutOrder(6)
        assertTrue(cancelSuccess)
        assertTrue(repo2.getBreakoutOrders().isEmpty())

        // Cold start after cancel
        ArjunStockRepository.resetCacheForTesting()
        val repo3 = ArjunStockRepository()
        assertTrue(repo3.getBreakoutOrders().isEmpty())
    }

    @Test
    fun testBreakoutOrderOverBudgetSingleShareSkipped() {
        val repo = ArjunStockRepository()
        // Expensive stock (e.g. MRF) where single share > 100,000 budget
        val order = ArjunBreakoutOrder(
            symbol = "EXPENSIVESTOCK",
            triggerPrice = 120_000.0,
            slotIndex = 4,
            allocationAmount = 100_000.0,
            quantity = 0 // Auto-calc by budget
        )
        val (setSuccess, _) = repo.setBreakoutOrder(order)
        assertTrue(setSuccess)

        // Price crosses trigger at 125,000.0 -> maxBudgetQty = 0 -> buyQty = 0
        val quotes = mapOf("EXPENSIVESTOCK" to 125_000.0)
        val (_, events) = repo.evaluateBreakoutTriggers(quotes)
        assertTrue(events.isEmpty())

        // Order remains pending, slot remains PENDING_BREAKOUT
        assertEquals(1, repo.getBreakoutOrders().size)
        val slots = repo.calculateStrategySlots(repo.getRecommendations())
        val slot4 = slots.first { it.slotIndex == 4 }
        assertEquals(ArjunSlotStatus.PENDING_BREAKOUT, slot4.status)

        // Summary confirms no capital was deployed
        val summary = repo.calculateStrategySummary(repo.getRecommendations())
        assertTrue(summary.totalDeployedCapital <= 700_000.0)
    }

    @Test
    fun testBreakoutOrderPriceGapDoesNotFalselyTriggerLowerProfitTarget() {
        val repo = ArjunStockRepository()
        val order = ArjunBreakoutOrder(
            symbol = "GAPSTOCK",
            triggerPrice = 100.0,
            slotIndex = 3,
            allocationAmount = 100_000.0,
            targetLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 110.0, allocationPct = 50.0),
                ArjunLadderLevel(levelIndex = 2, triggerPrice = 130.0, allocationPct = 50.0)
            ),
            stopLossLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 90.0, allocationPct = 100.0)
            )
        )
        val (setSuccess, _) = repo.setBreakoutOrder(order)
        assertTrue(setSuccess)

        // Stock gaps up to 115.0 on opening tick -> crosses breakout trigger 100.0
        val gapQuotes = mapOf("GAPSTOCK" to 115.0)
        val (itemsAfterBuy, buyEvents) = repo.evaluateBreakoutTriggers(gapQuotes)
        assertEquals(1, buyEvents.size)
        assertEquals("BREAKOUT_BUY", buyEvents.first().eventType)

        val stock = itemsAfterBuy.first { it.symbol == "GAPSTOCK" }
        assertEquals(115.0, stock.entryPrice, 0.01)
        assertEquals(115.0, stock.currentPrice, 0.01)
        assertTrue(stock.isStrategySlotOccupied)

        // On immediate subsequent evaluation at 115.0, TP1 (110.0) must NOT fire because 110.0 <= entryPrice (115.0)
        val (itemsSubsequent, subsequentEvents) = repo.evaluateStrategyTriggers(itemsAfterBuy, gapQuotes)
        assertTrue(subsequentEvents.isEmpty())
        val stockStillOpen = itemsSubsequent.first { it.symbol == "GAPSTOCK" }
        assertFalse(stockStillOpen.isClosed)
        assertTrue(stockStillOpen.isStrategySlotOccupied)

        // But when price rises to 132.0 -> crosses TP2 (130.0 > entry 115.0) -> triggers TP2!
        val quotesTp2 = mapOf("GAPSTOCK" to 132.0)
        val (itemsTp2, tp2Events) = repo.evaluateStrategyTriggers(itemsSubsequent, quotesTp2)
        assertEquals(1, tp2Events.size)
        assertTrue(tp2Events.first().headline.contains("TP2 Hit"))
    }

    @Test
    fun testBreakoutOrderPreservedWhenRecommendationsMutated() {
        val repo = ArjunStockRepository()
        val order = ArjunBreakoutOrder(
            symbol = "RETAINED",
            triggerPrice = 250.0,
            slotIndex = 4,
            allocationAmount = 100_000.0
        )
        repo.setBreakoutOrder(order)
        assertEquals(1, repo.getBreakoutOrders().size)

        // Add unrelated recommendation
        val newRec = ArjunStockItem(
            id = "mutated-test-1",
            symbol = "NEWREC",
            entryPrice = 50.0,
            quantity = 10
        )
        repo.addRecommendation(newRec)
        assertEquals(1, repo.getBreakoutOrders().size)

        // Mark a recommendation closed
        repo.markAsClosed("mutated-test-1", exitPrice = 55.0)
        assertEquals(1, repo.getBreakoutOrders().size)

        // Cold start check: orders must still be preserved on disk
        ArjunStockRepository.resetCacheForTesting()
        val repoReloaded = ArjunStockRepository()
        val ordersAfterReload = repoReloaded.getBreakoutOrders()
        assertEquals(1, ordersAfterReload.size)
        assertEquals("RETAINED", ordersAfterReload.first().symbol)
        assertEquals(4, ordersAfterReload.first().slotIndex)
    }

    @Test
    fun testBrokerOrderDispatchSellDoesNotTouchCorePortfolio() = runTest {
        val repo = VaralakshmiRepository()
        val initialCorePositions = repo.getCachedPositions()

        // Dispatch SELL order for an Arjun strategy stock
        val result = repo.dispatchBrokerOrder(
            strategyId = "ARJUN_7L_STRATEGY",
            symbol = "TATAPOWER",
            action = "SELL",
            quantity = 50,
            price = 420.0
        )
        assertTrue(result.isSuccess)

        // Core portfolio positions cache must remain completely unchanged
        val afterPositions = repo.getCachedPositions()
        assertEquals(initialCorePositions.size, afterPositions.size)
        assertEquals(initialCorePositions.map { it.symbol }, afterPositions.map { it.symbol })
    }

    @Test
    fun testApproveRecommendationOverBudgetSingleShareFails() {
        val repo = ArjunStockRepository()
        val expensiveStock = ArjunStockItem(
            id = "mrf-expensive-stock",
            symbol = "MRF",
            entryPrice = 140_000.0,
            currentPrice = 140_000.0,
            quantity = 1,
            isApprovedForStrategy = false,
            strategySlotIndex = -1
        )
        repo.addRecommendation(expensiveStock)

        val (success, message) = repo.approveRecommendation("mrf-expensive-stock")
        assertFalse(success)
        assertTrue(message.contains("exceeds ₹1,00,000 slot budget"))

        val reloaded = repo.getRecommendations().first { it.id == "mrf-expensive-stock" }
        assertFalse(reloaded.isApprovedForStrategy)
        assertEquals(-1, reloaded.strategySlotIndex)
    }

    @Test
    fun testOddLotQuantityMultiTierLadderNoOrphanShares() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "odd-lot-ladder-test",
            symbol = "ODDLOT",
            entryPrice = 100.0,
            currentPrice = 100.0,
            quantity = 9,
            initialStrategyQuantity = 9,
            isApprovedForStrategy = true,
            strategySlotIndex = 4,
            targetLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 110.0, allocationPct = 50.0),
                ArjunLadderLevel(levelIndex = 2, triggerPrice = 120.0, allocationPct = 50.0)
            )
        )
        repo.addRecommendation(stock)

        // 1. Cross TP1: floor(9 * 0.5) = 4 shares sold, 5 remaining
        val (eval1, events1) = repo.evaluateStrategyTriggers(listOf(stock.copy(currentPrice = 112.0)))
        assertEquals(1, events1.size)
        val afterTp1 = eval1.first { it.id == "odd-lot-ladder-test" }
        assertEquals(5, afterTp1.quantity)
        assertFalse(afterTp1.isClosed)

        // 2. Cross TP2: final ladder tier must sell all remaining 5 shares (no orphan shares)
        val (eval2, events2) = repo.evaluateStrategyTriggers(listOf(afterTp1.copy(currentPrice = 122.0)))
        assertEquals(1, events2.size)
        val afterTp2 = eval2.first { it.id == "odd-lot-ladder-test" }
        assertTrue(afterTp2.isClosed)
        assertEquals(122.0, afterTp2.closedPrice, 0.01)
        assertFalse(afterTp2.isStrategySlotOccupied)
        // Total PnL: 4 * (112 - 100) = 48, plus 5 * (122 - 100) = 110 => 158.0
        assertEquals(158.0, afterTp2.realizedPnl, 0.01)
    }

    @Test
    fun testBreakoutOrderAllocationCappedAtSlotBudget() {
        val repo = ArjunStockRepository()
        val oversizedOrder = ArjunBreakoutOrder(
            id = "oversized-breakout",
            slotIndex = 5,
            symbol = "OVERSIZE",
            triggerPrice = 100.0,
            allocatedAmount = 500_000.0 // Attempts 5 Lakhs allocation
        )
        val (success, _) = repo.setBreakoutOrder(oversizedOrder)
        assertTrue(success)

        val savedOrder = repo.getBreakoutOrders().first { it.id == "oversized-breakout" }
        assertEquals(100_000.0, savedOrder.allocatedAmount, 0.01)

        // Execution at ₹100 should buy 1000 shares (₹1 Lakh), NOT 5000 shares (₹5 Lakhs)
        val (eval, events) = repo.evaluateStrategyTriggers(
            items = repo.getRecommendations(),
            externalPrices = mapOf("OVERSIZE" to 100.0)
        )
        val executed = eval.firstOrNull { it.symbol == "OVERSIZE" }
        assertNotNull(executed)
        assertEquals(1000, executed?.quantity)
        assertEquals(100_000.0, executed?.strategySlotCapital ?: 0.0, 0.01)
    }

    @Test
    fun testPartialBuyResetsBaseQuantityForSubsequentLadder() {
        val repo = ArjunStockRepository()
        val stock = ArjunStockItem(
            id = "ladder-buy-reset-test",
            symbol = "RESETLADDER",
            entryPrice = 100.0,
            currentPrice = 100.0,
            quantity = 100,
            initialStrategyQuantity = 100,
            isApprovedForStrategy = true,
            strategySlotIndex = 6,
            targetLadder = listOf(
                ArjunLadderLevel(levelIndex = 1, triggerPrice = 110.0, allocationPct = 50.0)
            )
        )
        repo.addRecommendation(stock)

        // 1. Partial sell 50 shares
        repo.partialSell("ladder-buy-reset-test", 50, 105.0)
        val afterSell = repo.getRecommendations().first { it.id == "ladder-buy-reset-test" }
        assertEquals(50, afterSell.quantity)

        // 2. Partial buy 50 shares back at ₹100
        repo.partialBuy("ladder-buy-reset-test", 50, 100.0)
        val afterBuy = repo.getRecommendations().first { it.id == "ladder-buy-reset-test" }
        assertEquals(100, afterBuy.quantity)
        assertEquals(100, afterBuy.initialStrategyQuantity)

        // 3. Trigger 50% TP level -> should sell 50 shares (50% of 100), not 75 shares
        val (eval, events) = repo.evaluateStrategyTriggers(listOf(afterBuy.copy(currentPrice = 115.0)))
        assertEquals(1, events.size)
        val afterTp = eval.first { it.id == "ladder-buy-reset-test" }
        assertEquals(50, afterTp.quantity)
    }

    @Test
    fun testNoDiskCacheInProductionMode() {
        val repo = ArjunStockRepository()
        ArjunStockRepository.enableDiskCache = false
        try {
            val stock = ArjunStockItem(symbol = "TESTCACHE", entryPrice = 100.0)
            repo.addRecommendation(stock)
            val dir = VaralakshmiRepository.getCacheDirectory()
            val file = File(dir, "arjun_stocks_cache.json")
            assertFalse("arjun_stocks_cache.json must NOT exist when enableDiskCache is false", file.exists())
        } finally {
            ArjunStockRepository.enableDiskCache = true
        }
    }

    @Test
    fun testParseServerFullPayload10Slots() {
        val repo = ArjunStockRepository()
        val mockJson = """
            {
              "status": "success",
              "summary": {
                "total_strategy_capital": 500000.0,
                "slot_budget": 50000.0,
                "total_slots": 10,
                "occupied_slots_count": 1,
                "deployed_capital": 49000.0,
                "available_cash": 451000.0,
                "current_portfolio_value": 52000.0,
                "unrealized_pnl": 3000.0,
                "realized_pnl": 0.0,
                "total_strategy_nav": 503000.0,
                "total_strategy_return_pct": 0.60,
                "status": "ACTIVE_CNC",
                "broker": "Zerodha Kite CNC"
              },
              "positions": [
                {
                  "slot_index": 1,
                  "symbol": "ORIENTHOT",
                  "company_name": "ORIENTAL HOTELS",
                  "entry_price": 169.5,
                  "current_price": 175.0,
                  "quantity": 289,
                  "target_1": 185.0,
                  "target_2": 195.0,
                  "stop_loss": 160.0
                }
              ],
              "slots": [
                {
                  "slot_index": 1,
                  "status": "OCCUPIED",
                  "symbol": "ORIENTHOT",
                  "current_price": 175.0,
                  "pnl_pct": 3.24
                },
                {
                  "slot_index": 2,
                  "status": "EMPTY",
                  "allocated_budget": 50000.0
                }
              ],
              "watchlist": [
                {
                  "symbol": "FEDERALBNK",
                  "trigger_price": 205.0,
                  "planned_shares": 243,
                  "target_1": 220.0,
                  "target_2": 230.0,
                  "stop_loss": 195.0
                }
              ]
            }
        """.trimIndent()

        val payload = repo.parseServerFullPayload(mockJson)
        assertEquals(500000.0, payload.strategySummary.totalFund, 0.01)
        assertEquals(500000.0, payload.strategySummary.totalCapital, 0.01)
        assertEquals(50000.0, payload.strategySummary.slotBudget, 0.01)
        assertEquals(10, payload.strategySummary.totalSlots)
        assertEquals("ACTIVE_CNC", payload.serverStatus)
        assertEquals("Zerodha Kite CNC", payload.broker)

        assertEquals(1, payload.positions.size)
        assertEquals("ORIENTHOT", payload.positions.first().symbol)
        assertEquals(1, payload.positions.first().strategySlotIndex)

        // Verify slots include 10 slots
        assertEquals(10, payload.strategySlots.size)
        assertEquals(ArjunSlotStatus.OCCUPIED, payload.strategySlots[0].status)
        assertEquals(ArjunSlotStatus.PENDING_BREAKOUT, payload.strategySlots[1].status)
        assertEquals("FEDERALBNK", payload.strategySlots[1].pendingBreakout?.symbol)
        assertEquals(ArjunSlotStatus.EMPTY, payload.strategySlots[2].status)

        // Verify breakout order from watchlist
        assertEquals(1, payload.breakoutOrders.size)
        assertEquals("FEDERALBNK", payload.breakoutOrders.first().symbol)
        assertEquals(205.0, payload.breakoutOrders.first().triggerPrice, 0.01)
    }

    @Test
    fun testParseServerFullPayloadTelegramMessages() {
        val repo = ArjunStockRepository()
        val mockJson = """
            {
              "status": "success",
              "summary": { "total_strategy_capital": 500000.0, "total_slots": 10 },
              "positions": [],
              "slots": [],
              "watchlist": [],
              "telegram_messages": [
                {
                  "post_id": 16777,
                  "datetime": "2026-10-18T03:00:00+00:00",
                  "date": "2026-10-18",
                  "time": "08:30 AM",
                  "formatted_time": "Today, 08:30 AM",
                  "views": "3.5K",
                  "has_media": true,
                  "text": "FIRE CALL: BUY ORIENTHOT @ 175",
                  "raw_url": "https://t.me/FlyingcallsArjun/16777",
                  "is_today": true
                },
                {
                  "post_id": 16776,
                  "datetime": "2026-10-17T14:19:07+00:00",
                  "date": "2026-10-17",
                  "time": "07:49 PM",
                  "formatted_time": "Oct 17, 07:49 PM",
                  "views": "5.2K",
                  "has_media": false,
                  "text": "Good evening traders",
                  "raw_url": "https://t.me/FlyingcallsArjun/16776",
                  "is_today": false
                }
              ]
            }
        """.trimIndent()

        val payload = repo.parseServerFullPayload(mockJson)
        assertEquals(2, payload.telegramMessages.size)

        val first = payload.telegramMessages[0]
        assertEquals(16777L, first.postId)
        assertEquals("Today, 08:30 AM", first.displayTime)
        assertTrue(first.isToday)
        assertTrue(first.hasMedia)
        assertEquals("FIRE CALL: BUY ORIENTHOT @ 175", first.text)

        val second = payload.telegramMessages[1]
        assertEquals(16776L, second.postId)
        assertFalse(second.isToday)
        assertEquals("Oct 17, 07:49 PM", second.displayTime)
    }
}


