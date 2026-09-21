package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.data.VaralakshmiRepository.Companion.roundPaise
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.concurrent.thread

/**
 * Adversarial and Stress Verification Test Suite for Varalakshmi Portfolio.
 * Validates financial precision, invariant conservation under sequential and arbitrary
 * trade exits, concurrency safety, edge cases, and ViewModel state transitions.
 */
class FinancialAdversarialStressTest {

    private val baselineNav = 109268.80
    private val baselineDeployed = 99536.77
    private val baselineAvailable = 463.23
    private val baselineUnrealized = 9268.80
    private val baselineTotalPnl = 9268.80

    // Position specs:
    // STLNETWORK: Q=855, Entry=40.20 (Cost=34371.00), Current=45.09 (MV=38551.95), Gain=+4180.95
    // AHCL:       Q=1472, Cost=33365.77, Current=24.91 (MV=36667.52), Gain=+3301.75
    // TBZ:        Q=53, Entry=600.00 (Cost=31800.00), Current=633.70 (MV=33586.10), Gain=+1786.10

    @Test
    fun testBaselineInvariant() {
        val repository = VaralakshmiRepository()
        val summary = repository.getCachedSummary()
        val positions = repository.getCachedPositions()
        val transactions = repository.getCachedTransactions()

        assertEquals(baselineNav, summary.totalNav, 0.0001)
        assertEquals(baselineDeployed, summary.deployedCapital, 0.0001)
        assertEquals(baselineAvailable, summary.availableCapital, 0.0001)
        assertEquals(baselineUnrealized, summary.unrealizedPnl, 0.0001)
        assertEquals(0.00, summary.realizedPnl, 0.0001)
        assertEquals(baselineTotalPnl, summary.totalPnl, 0.0001)
        assertEquals(9.2688, summary.totalPnlPct, 0.0001)
        assertEquals(3, summary.activeSlots)
        assertEquals(3, positions.size)
        assertEquals(4, transactions.size)

        // Fundamental Invariant: NAV == Deployed + Available + Unrealized
        val computedNav = roundPaise(summary.deployedCapital + summary.availableCapital + summary.unrealizedPnl)
        assertEquals(baselineNav, computedNav, 0.0001)
    }

    @Test
    fun testExactSequentialTradeExitsConservesNavAtEveryStep() {
        val repository = VaralakshmiRepository()

        // --- Step 0: Baseline verification ---
        var summary = repository.getCachedSummary()
        var available = summary.availableCapital
        var deployed = summary.deployedCapital
        var realized = summary.realizedPnl
        var txList = repository.getCachedTransactions()
        assertEquals(4, txList.size)

        // --- Step 1: Exit STLNETWORK ---
        val (s1, p1, tx1) = repository.removePosition("STLNETWORK")
        val stlMv = 38551.95
        val stlCost = 34371.00
        val stlGain = 4180.95

        // Invariant: NAV remains strictly conserved at 109,268.80
        assertEquals(baselineNav, s1.totalNav, 0.0001)
        // Available cash increases by the exact market value of the closed position
        val expectedCash1 = roundPaise(available + stlMv)
        assertEquals(39015.18, expectedCash1, 0.0001)
        assertEquals(expectedCash1, s1.availableCapital, 0.0001)
        // Deployed capital decreases by the exact cost basis
        val expectedDeployed1 = roundPaise(deployed - stlCost)
        assertEquals(65165.77, expectedDeployed1, 0.0001)
        assertEquals(expectedDeployed1, s1.deployedCapital, 0.0001)
        // Realized PnL accumulates the closed position's unrealized gain
        val expectedRealized1 = roundPaise(realized + stlGain)
        assertEquals(4180.95, expectedRealized1, 0.0001)
        assertEquals(expectedRealized1, s1.realizedPnl, 0.0001)
        // Total PnL (unrealized + realized) remains ₹9,268.80 (+9.27%)
        val expectedUnrealized1 = roundPaise(3301.75 + 1786.10)
        assertEquals(expectedUnrealized1, s1.unrealizedPnl, 0.0001)
        assertEquals(5087.85, s1.unrealizedPnl, 0.0001)
        assertEquals(baselineTotalPnl, s1.totalPnl, 0.0001)
        assertEquals(9.2688, s1.totalPnlPct, 0.001)
        // Invariant check: NAV == Deployed + Available + Unrealized
        assertEquals(s1.totalNav, roundPaise(s1.deployedCapital + s1.availableCapital + s1.unrealizedPnl), 0.0001)
        // Position check
        assertEquals(2, p1.size)
        assertTrue(p1.none { it.symbol == "STLNETWORK" })
        // SELL transaction appended
        assertEquals(5, tx1.size)
        val sellTx1 = tx1.first()
        assertEquals("SELL", sellTx1.side)
        assertEquals("STLNETWORK", sellTx1.symbol)
        assertEquals(855, sellTx1.quantity)
        assertEquals(45.09, sellTx1.fillPrice, 0.0001)
        assertEquals(stlMv, sellTx1.grossAmount, 0.0001)
        assertEquals(stlGain, sellTx1.realizedPnl, 0.0001)

        // --- Step 2: Exit AHCL ---
        val (s2, p2, tx2) = repository.removePosition("AHCL")
        val ahclMv = 36667.52
        val ahclCost = 33365.77
        val ahclGain = 3301.75

        // Invariant: NAV remains strictly conserved at 109,268.80
        assertEquals(baselineNav, s2.totalNav, 0.0001)
        // Available cash increases by exact market value
        val expectedCash2 = roundPaise(expectedCash1 + ahclMv)
        assertEquals(75682.70, expectedCash2, 0.0001)
        assertEquals(expectedCash2, s2.availableCapital, 0.0001)
        // Deployed capital decreases by exact cost basis
        val expectedDeployed2 = roundPaise(expectedDeployed1 - ahclCost)
        assertEquals(31800.00, expectedDeployed2, 0.0001)
        assertEquals(expectedDeployed2, s2.deployedCapital, 0.0001)
        // Realized PnL accumulates closed position's unrealized gain
        val expectedRealized2 = roundPaise(expectedRealized1 + ahclGain)
        assertEquals(7482.70, expectedRealized2, 0.0001)
        assertEquals(expectedRealized2, s2.realizedPnl, 0.0001)
        // Total PnL remains ₹9,268.80 (+9.27%)
        val expectedUnrealized2 = 1786.10
        assertEquals(expectedUnrealized2, s2.unrealizedPnl, 0.0001)
        assertEquals(baselineTotalPnl, s2.totalPnl, 0.0001)
        assertEquals(9.2688, s2.totalPnlPct, 0.001)
        // Invariant check: NAV == Deployed + Available + Unrealized
        assertEquals(s2.totalNav, roundPaise(s2.deployedCapital + s2.availableCapital + s2.unrealizedPnl), 0.0001)
        // Position check
        assertEquals(1, p2.size)
        assertEquals("TBZ", p2.first().symbol)
        // SELL transaction appended
        assertEquals(6, tx2.size)
        val sellTx2 = tx2.first()
        assertEquals("SELL", sellTx2.side)
        assertEquals("AHCL", sellTx2.symbol)
        assertEquals(1472, sellTx2.quantity)
        assertEquals(24.91, sellTx2.fillPrice, 0.0001)
        assertEquals(ahclMv, sellTx2.grossAmount, 0.0001)
        assertEquals(ahclGain, sellTx2.realizedPnl, 0.0001)

        // --- Step 3: Exit TBZ ---
        val (s3, p3, tx3) = repository.removePosition("TBZ")
        val tbzMv = 33586.10
        val tbzCost = 31800.00
        val tbzGain = 1786.10

        // Invariant: NAV remains strictly conserved at 109,268.80
        assertEquals(baselineNav, s3.totalNav, 0.0001)
        // Available cash increases by exact market value
        val expectedCash3 = roundPaise(expectedCash2 + tbzMv)
        assertEquals(109268.80, expectedCash3, 0.0001)
        assertEquals(expectedCash3, s3.availableCapital, 0.0001)
        // Deployed capital decreases by exact cost basis (down to 0.00)
        val expectedDeployed3 = roundPaise(expectedDeployed2 - tbzCost)
        assertEquals(0.00, expectedDeployed3, 0.0001)
        assertEquals(0.00, s3.deployedCapital, 0.0001)
        // Realized PnL accumulates closed position's unrealized gain
        val expectedRealized3 = roundPaise(expectedRealized2 + tbzGain)
        assertEquals(9268.80, expectedRealized3, 0.0001)
        assertEquals(expectedRealized3, s3.realizedPnl, 0.0001)
        // Unrealized is 0.00, total PnL remains ₹9,268.80 (+9.27%)
        assertEquals(0.00, s3.unrealizedPnl, 0.0001)
        assertEquals(baselineTotalPnl, s3.totalPnl, 0.0001)
        assertEquals(9.2688, s3.totalPnlPct, 0.001)
        // Invariant check: NAV == Deployed + Available + Unrealized
        assertEquals(s3.totalNav, roundPaise(s3.deployedCapital + s3.availableCapital + s3.unrealizedPnl), 0.0001)
        // Position check
        assertEquals(0, p3.size)
        assertEquals(0, s3.activeSlots)
        // SELL transaction appended
        assertEquals(7, tx3.size)
        val sellTx3 = tx3.first()
        assertEquals("SELL", sellTx3.side)
        assertEquals("TBZ", sellTx3.symbol)
        assertEquals(53, sellTx3.quantity)
        assertEquals(633.70, sellTx3.fillPrice, 0.0001)
        assertEquals(tbzMv, sellTx3.grossAmount, 0.0001)
        assertEquals(tbzGain, sellTx3.realizedPnl, 0.0001)
    }

    @Test
    fun testAllPermutationsOfLiquidationConserveNav() {
        val symbols = listOf("STLNETWORK", "AHCL", "TBZ")
        val permutations = listOf(
            listOf("STLNETWORK", "AHCL", "TBZ"),
            listOf("STLNETWORK", "TBZ", "AHCL"),
            listOf("AHCL", "STLNETWORK", "TBZ"),
            listOf("AHCL", "TBZ", "STLNETWORK"),
            listOf("TBZ", "STLNETWORK", "AHCL"),
            listOf("TBZ", "AHCL", "STLNETWORK")
        )

        for (perm in permutations) {
            val repo = VaralakshmiRepository()
            for (sym in perm) {
                val (sum, pos, tx) = repo.removePosition(sym)
                assertEquals("NAV broken during permutation $perm at $sym", baselineNav, sum.totalNav, 0.0001)
                val expectedNav = roundPaise(sum.deployedCapital + sum.availableCapital + sum.unrealizedPnl)
                assertEquals("Invariant broken during permutation $perm at $sym", sum.totalNav, expectedNav, 0.0001)
                assertEquals("Total PnL broken during permutation $perm at $sym", baselineTotalPnl, sum.totalPnl, 0.0001)
            }
            val finalSum = repo.getCachedSummary()
            assertEquals(109268.80, finalSum.availableCapital, 0.0001)
            assertEquals(0.00, finalSum.deployedCapital, 0.0001)
            assertEquals(0.00, finalSum.unrealizedPnl, 0.0001)
            assertEquals(9268.80, finalSum.realizedPnl, 0.0001)
            assertEquals(0, repo.getCachedPositions().size)
            assertEquals(7, repo.getCachedTransactions().size)
        }
    }

    @Test
    fun testIdempotentAndNonExistentExits() {
        val repo = VaralakshmiRepository()
        val initialSummary = repo.getCachedSummary()
        val initialTxCount = repo.getCachedTransactions().size

        // Attempt to remove non-existent symbol
        val (s1, p1, tx1) = repo.removePosition("NON_EXISTENT_SYMBOL")
        assertEquals(initialSummary.totalNav, s1.totalNav, 0.0001)
        assertEquals(initialSummary.availableCapital, s1.availableCapital, 0.0001)
        assertEquals(initialSummary.deployedCapital, s1.deployedCapital, 0.0001)
        assertEquals(initialSummary.realizedPnl, s1.realizedPnl, 0.0001)
        assertEquals(3, p1.size)
        assertEquals(initialTxCount, tx1.size)

        // Remove STLNETWORK once
        repo.removePosition("STLNETWORK")
        val postFirstSummary = repo.getCachedSummary()
        val postFirstTxCount = repo.getCachedTransactions().size

        // Attempt to remove STLNETWORK again (duplicate exit)
        val (s2, p2, tx2) = repo.removePosition("STLNETWORK")
        assertEquals(postFirstSummary.totalNav, s2.totalNav, 0.0001)
        assertEquals(postFirstSummary.availableCapital, s2.availableCapital, 0.0001)
        assertEquals(postFirstSummary.deployedCapital, s2.deployedCapital, 0.0001)
        assertEquals(postFirstSummary.realizedPnl, s2.realizedPnl, 0.0001)
        assertEquals(2, p2.size)
        assertEquals(postFirstTxCount, tx2.size)
    }

    @Test
    fun testMultithreadedConcurrentExits() {
        val repo = VaralakshmiRepository()
        val symbols = listOf("STLNETWORK", "AHCL", "TBZ")

        val threads = symbols.map { sym ->
            thread {
                repo.removePosition(sym)
            }
        }
        threads.forEach { it.join() }

        val finalSummary = repo.getCachedSummary()
        val finalPositions = repo.getCachedPositions()
        val finalTransactions = repo.getCachedTransactions()

        assertEquals(baselineNav, finalSummary.totalNav, 0.0001)
        assertEquals(109268.80, finalSummary.availableCapital, 0.0001)
        assertEquals(0.00, finalSummary.deployedCapital, 0.0001)
        assertEquals(0.00, finalSummary.unrealizedPnl, 0.0001)
        assertEquals(9268.80, finalSummary.realizedPnl, 0.0001)
        assertEquals(9268.80, finalSummary.totalPnl, 0.0001)
        assertEquals(0, finalPositions.size)
        assertEquals(7, finalTransactions.size)
        assertEquals(3, finalTransactions.count { it.side == "SELL" })
    }

    @Test
    fun testViewModelSequentialExitsEndToEnd() = runTest {
        val repo = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repo)

        val symbolsToExit = listOf("STLNETWORK", "AHCL", "TBZ")
        for (sym in symbolsToExit) {
            val stateBefore = viewModel.uiState.value
            val target = stateBefore.positions.firstOrNull { it.symbol == sym }
            assertNotNull("Position $sym should exist before exit", target)

            viewModel.requestExit(target!!)
            assertEquals(target, viewModel.uiState.value.positionToExit)

            viewModel.confirmExit()
            val stateAfter = viewModel.uiState.value
            assertNull(stateAfter.positionToExit)
            assertEquals(baselineNav, stateAfter.summary.totalNav, 0.0001)
            assertTrue(stateAfter.snackbarMessage?.contains("Exited $sym") == true)
        }

        val finalState = viewModel.uiState.value
        assertEquals(0, finalState.positions.size)
        assertEquals(109268.80, finalState.summary.availableCapital, 0.0001)
        assertEquals(0.00, finalState.summary.deployedCapital, 0.0001)
        assertEquals(9268.80, finalState.summary.realizedPnl, 0.0001)
        assertEquals(baselineNav, finalState.summary.totalNav, 0.0001)
    }
}
