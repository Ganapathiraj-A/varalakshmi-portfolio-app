package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.SyncResult
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class VaralakshmiPortfolioTest {

    @Test
    fun testBaselineSnapshotAndInvariant() {
        val repository = VaralakshmiRepository()
        val summary = repository.getCachedSummary()
        val positions = repository.getCachedPositions()
        val transactions = repository.getCachedTransactions()

        // 1. Acceptance Criteria: Baseline NAV = ₹109,268.80
        assertEquals(109268.80, summary.totalNav, 0.001)
        assertEquals(99536.77, summary.deployedCapital, 0.001)
        assertEquals(463.23, summary.availableCapital, 0.001)
        assertEquals(9268.80, summary.unrealizedPnl, 0.001)
        assertEquals(0.00, summary.realizedPnl, 0.001)
        assertEquals(9268.80, summary.totalPnl, 0.001)
        assertEquals(4007.77, summary.todayPnl, 0.001)
        assertEquals(3.81, summary.todayPnlPct, 0.01)
        assertEquals(4007.77, summary.todayNavChange, 0.001)
        assertEquals(3.81, summary.todayNavChangePct, 0.01)
        assertEquals(3, summary.activeSlots)
        assertEquals(3, positions.size)
        assertEquals(4, transactions.size)

        // 2. Core Invariant: NAV = Deployed + Available + Unrealized
        val calculatedNav = summary.deployedCapital + summary.availableCapital + summary.unrealizedPnl
        assertEquals(summary.totalNav, calculatedNav, 0.001)

        // 3. Daily Change Invariant: sum of todayValueChange across all positions equals summary.todayPnl
        val sumTodayValueChange = VaralakshmiRepository.roundPaise(positions.sumOf { it.todayValueChange })
        assertEquals(summary.todayPnl, sumTodayValueChange, 0.001)
    }

    @Test
    fun testTodayTickerPositionsDataMappingAndCalculations() {
        val repository = VaralakshmiRepository()
        val positions = repository.getCachedPositions()

        val stl = positions.first { it.symbol == "STLNETWORK" }
        assertEquals(855, stl.quantity)
        assertEquals(40.20, stl.entryPrice, 0.001)
        assertEquals(45.09, stl.currentPrice, 0.001)
        assertEquals(43.80, stl.previousClose, 0.001)
        assertEquals(43.80, stl.referencePrice, 0.001)
        assertEquals(1.29, stl.todayPriceChange, 0.001)
        assertEquals(2.95, stl.todayPriceChangePct, 0.01)
        assertEquals(1102.95, stl.todayValueChange, 0.001)
        assertEquals(1102.95, VaralakshmiRepository.roundPaise(stl.quantity * stl.todayPriceChange), 0.001)

        val ahcl = positions.first { it.symbol == "AHCL" }
        assertEquals(1472, ahcl.quantity)
        assertEquals(22.67, ahcl.entryPrice, 0.001)
        assertEquals(24.91, ahcl.currentPrice, 0.001)
        assertEquals(24.15, ahcl.previousClose, 0.001)
        assertEquals(24.15, ahcl.referencePrice, 0.001)
        assertEquals(0.76, ahcl.todayPriceChange, 0.001)
        assertEquals(3.15, ahcl.todayPriceChangePct, 0.01)
        assertEquals(1118.72, ahcl.todayValueChange, 0.001)
        assertEquals(1118.72, VaralakshmiRepository.roundPaise(ahcl.quantity * ahcl.todayPriceChange), 0.001)

        val tbz = positions.first { it.symbol == "TBZ" }
        assertEquals(53, tbz.quantity)
        assertEquals(600.00, tbz.entryPrice, 0.001)
        assertEquals(633.70, tbz.currentPrice, 0.001)
        assertEquals(600.00, tbz.previousClose, 0.001)
        assertEquals(600.00, tbz.referencePrice, 0.001)
        assertEquals(33.70, tbz.todayPriceChange, 0.001)
        assertEquals(5.62, tbz.todayPriceChangePct, 0.01)
        assertEquals(1786.10, tbz.todayValueChange, 0.001)
        assertEquals(1786.10, VaralakshmiRepository.roundPaise(tbz.quantity * tbz.todayPriceChange), 0.001)
    }

    @Test
    fun testSequentialExitsRecalculateTodayPnlWithoutDrift() {
        val repository = VaralakshmiRepository()

        // 1. Initial: todayPnl = 4007.77
        assertEquals(4007.77, repository.getCachedSummary().todayPnl, 0.001)

        // 2. Remove STLNETWORK (todayValueChange = 1102.95)
        val (s1, p1, _) = repository.removePosition("STLNETWORK")
        val expectedTodayPnl1 = VaralakshmiRepository.roundPaise(1118.72 + 1786.10)
        assertEquals(2904.82, expectedTodayPnl1, 0.001)
        assertEquals(expectedTodayPnl1, s1.todayPnl, 0.001)
        assertEquals(s1.todayPnl, VaralakshmiRepository.roundPaise(p1.sumOf { it.todayValueChange }), 0.001)

        // 3. Remove AHCL (todayValueChange = 1118.72)
        val (s2, p2, _) = repository.removePosition("AHCL")
        assertEquals(1786.10, s2.todayPnl, 0.001)
        assertEquals(s2.todayPnl, VaralakshmiRepository.roundPaise(p2.sumOf { it.todayValueChange }), 0.001)

        // 4. Remove TBZ -> Complete liquidation: todayPnl = 0.00, todayPnlPct = 0.00
        val (s3, p3, _) = repository.removePosition("TBZ")
        assertEquals(0.00, s3.todayPnl, 0.001)
        assertEquals(0.00, s3.todayPnlPct, 0.001)
        assertEquals(0, p3.size)
    }

    @Test
    fun testPositionItemPreviousCloseFallbackToEntryPrice() {
        // Position entered today without previousClose (previousClose = 0.0)
        val todayNewPosition = PositionItem(
            positionId = "NEW_POS_1",
            symbol = "INFY",
            quantity = 50,
            entryPrice = 1800.00,
            currentPrice = 1845.00,
            marketValue = 92250.00,
            unrealizedPnl = 2250.00,
            unrealizedPnlPct = 2.50,
            peakPrice = 1845.00,
            entryDate = "2026-09-21 10:00:00"
            // previousClose defaults to 0.0 -> falls back to entryPrice
        )

        assertEquals(1800.00, todayNewPosition.referencePrice, 0.001)
        assertEquals(45.00, todayNewPosition.todayPriceChange, 0.001)
        assertEquals(2.50, todayNewPosition.todayPriceChangePct, 0.01)
        assertEquals(2250.00, todayNewPosition.todayValueChange, 0.001)

        // Position with negative day movement
        val downPosition = PositionItem(
            positionId = "DOWN_POS",
            symbol = "TCS",
            quantity = 20,
            entryPrice = 3500.00,
            currentPrice = 3450.00,
            marketValue = 69000.00,
            unrealizedPnl = -1000.00,
            unrealizedPnlPct = -1.43,
            peakPrice = 3550.00,
            entryDate = "2026-09-20 10:00:00",
            previousClose = 3500.00
        )

        assertEquals(-50.00, downPosition.todayPriceChange, 0.001)
        assertEquals(-1.43, downPosition.todayPriceChangePct, 0.01)
        assertEquals(-1000.00, downPosition.todayValueChange, 0.001)
    }

    @Test
    fun testTradeExitConservesNavAndRecordsSell() {
        val repository = VaralakshmiRepository()
        val initialNav = repository.getCachedSummary().totalNav
        val initialCash = repository.getCachedSummary().availableCapital

        // Exit STLNETWORK (Market Value = ₹38,551.95, Unrealized Gain = ₹4,180.95)
        val (newSummary, newPositions, newTransactions) = repository.removePosition("STLNETWORK")

        // 1. NAV must be strictly conserved
        assertEquals(initialNav, newSummary.totalNav, 0.001)
        assertEquals(109268.80, newSummary.totalNav, 0.001)

        // 2. Available cash credited by full market value proceeds
        val expectedCash = initialCash + 38551.95
        assertEquals(expectedCash, newSummary.availableCapital, 0.001)
        assertEquals(39015.18, newSummary.availableCapital, 0.001)

        // 3. Realized PnL accumulates the closed gain
        assertEquals(4180.95, newSummary.realizedPnl, 0.001)

        // 4. Remaining holdings
        assertEquals(2, newPositions.size)
        assertTrue(newPositions.none { it.symbol == "STLNETWORK" })

        // 5. Invariant holds post-exit
        val calculatedNav = newSummary.deployedCapital + newSummary.availableCapital + newSummary.unrealizedPnl
        assertEquals(newSummary.totalNav, calculatedNav, 0.001)

        // 6. SELL transaction recorded in ledger
        val latestTx = newTransactions.first()
        assertEquals("SELL", latestTx.side)
        assertEquals("STLNETWORK", latestTx.symbol)
        assertEquals(855, latestTx.quantity)
        assertEquals(45.09, latestTx.fillPrice, 0.001)
        assertEquals(38551.95, latestTx.grossAmount, 0.001)
        assertEquals(4180.95, latestTx.realizedPnl, 0.001)
        assertTrue(latestTx.pnlDifference.contains("+₹4,180.95"))
    }

    @Test
    fun testCompleteLiquidationPreservesNavAndConvertsAllToCash() {
        val repository = VaralakshmiRepository()

        // Sequentially liquidate all 3 open holdings
        repository.removePosition("STLNETWORK")
        repository.removePosition("AHCL")
        val (finalSummary, finalPositions, finalTransactions) = repository.removePosition("TBZ")

        // 1. NAV conserved exactly at ₹109,268.80
        assertEquals(109268.80, finalSummary.totalNav, 0.001)

        // 2. 100% Cash allocation: available cash equals total NAV
        assertEquals(109268.80, finalSummary.availableCapital, 0.001)
        assertEquals(0.00, finalSummary.deployedCapital, 0.001)
        assertEquals(0.00, finalSummary.unrealizedPnl, 0.001)

        // 3. Cumulative realized PnL captured in full
        assertEquals(9268.80, finalSummary.realizedPnl, 0.001)
        assertEquals(9268.80, finalSummary.totalPnl, 0.001)
        assertEquals(9.2688, finalSummary.totalPnlPct, 0.001)

        // 4. Zero open holdings
        assertEquals(0, finalPositions.size)
        assertEquals(0, finalSummary.activeSlots)

        // 5. Invariant strictly preserved: NAV = 0 + 109268.80 + 0
        val calculatedNav = finalSummary.deployedCapital + finalSummary.availableCapital + finalSummary.unrealizedPnl
        assertEquals(finalSummary.totalNav, calculatedNav, 0.001)

        // 6. 3 SELL transactions prepended to initial 4 BUYs = 7 logs
        assertEquals(7, finalTransactions.size)
        val sellTxCount = finalTransactions.count { it.side == "SELL" }
        assertEquals(3, sellTxCount)
    }

    @Test
    fun testOfflineNetworkFailurePreservesCacheWithoutCrash() = runTest {
        val repository = VaralakshmiRepository()

        // Attempt refresh against an offline / unreachable port
        val result = repository.refreshData("http://127.0.0.1:59999")

        // Must return typed fallback without throwing or corrupting cache
        assertTrue(result is SyncResult.OfflineCacheFallback)
        assertEquals(109268.80, result.summary.totalNav, 0.001)
        assertEquals(99536.77, result.summary.deployedCapital, 0.001)
        assertEquals(463.23, result.summary.availableCapital, 0.001)
        assertEquals(3, result.positions.size)
        assertEquals(4, result.transactions.size)
    }

    @Test
    fun testViewModelStateManagementAndExitFlow() = runTest {
        val repository = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository)

        // Initial state
        val initialState = viewModel.uiState.value
        assertEquals(109268.80, initialState.summary.totalNav, 0.001)
        assertEquals(3, initialState.positions.size)
        assertNull(initialState.positionToExit)

        // Request exit for AHCL
        val ahcl = initialState.positions.first { it.symbol == "AHCL" }
        viewModel.requestExit(ahcl)
        assertEquals(ahcl, viewModel.uiState.value.positionToExit)

        // Dismiss exit
        viewModel.dismissExit()
        assertNull(viewModel.uiState.value.positionToExit)

        // Request and confirm exit
        viewModel.requestExit(ahcl)
        viewModel.confirmExit()
        val postExitState = viewModel.uiState.value
        assertNull(postExitState.positionToExit)
        assertEquals(2, postExitState.positions.size)
        assertEquals(109268.80, postExitState.summary.totalNav, 0.001)
        assertEquals(3301.75, postExitState.summary.realizedPnl, 0.001)
        assertNotNull(postExitState.snackbarMessage)
        assertTrue(postExitState.snackbarMessage!!.contains("Exited AHCL"))
    }

    @Test
    fun testMultiplierStringLocaleIndependence() {
        val position = PositionItem(
            positionId = "TEST_POS",
            symbol = "TEST",
            quantity = 100,
            entryPrice = 100.0,
            currentPrice = 112.5,
            marketValue = 11250.0,
            unrealizedPnl = 1250.0,
            unrealizedPnlPct = 12.5,
            peakPrice = 115.0,
            entryDate = "2026-09-21 00:00:00"
        )
        // Must use US dot decimal separator
        assertEquals("1.13x", position.multiplierString)
    }
}
