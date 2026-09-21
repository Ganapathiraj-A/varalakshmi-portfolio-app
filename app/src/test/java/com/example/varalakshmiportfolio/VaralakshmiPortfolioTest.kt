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
        assertEquals(3, summary.activeSlots)
        assertEquals(3, positions.size)
        assertEquals(4, transactions.size)

        // 2. Core Invariant: NAV = Deployed + Available + Unrealized
        val calculatedNav = summary.deployedCapital + summary.availableCapital + summary.unrealizedPnl
        assertEquals(summary.totalNav, calculatedNav, 0.001)
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
