package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.calculateLiveMilestones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMilestoneTest {

    @Test
    fun testRealWorld12TradesMilestoneCalculation() {
        // Mock of the exact 12 real-money trades executed since Sept 17, 2026
        val transactions = listOf(
            TransactionItem(transactionId = "1", timestamp = "2026-09-24", side = "SELL", symbol = "AHCL", quantity = 1472, fillPrice = 28.7, grossAmount = 42246.4, realizedPnl = 8781.36),
            TransactionItem(transactionId = "2", timestamp = "2026-09-25", side = "SELL", symbol = "AHCL", quantity = 6903, fillPrice = 27.97, grossAmount = 193110.82, realizedPnl = -7870.39),
            TransactionItem(transactionId = "3", timestamp = "2026-09-25", side = "SELL", symbol = "STLNETWORK", quantity = 855, fillPrice = 54.61, grossAmount = 46691.55, realizedPnl = 12215.46),
            TransactionItem(transactionId = "4", timestamp = "2026-09-30", side = "SELL", symbol = "TBZ", quantity = 53, fillPrice = 622.7, grossAmount = 33003.1, realizedPnl = 1115.27),
            TransactionItem(transactionId = "5", timestamp = "2026-10-01", side = "SELL", symbol = "GMMPFAUDLR", quantity = 121, fillPrice = 1435.32, grossAmount = 173673.5, realizedPnl = -9449.13),
            TransactionItem(transactionId = "6", timestamp = "2026-10-01", side = "SELL", symbol = "STLNETWORK", quantity = 4150, fillPrice = 47.17, grossAmount = 195764.75, realizedPnl = -8861.67),
            TransactionItem(transactionId = "7", timestamp = "2026-10-05", side = "SELL", symbol = "CUBEXTUB", quantity = 1155, fillPrice = 158.12, grossAmount = 182629.8, realizedPnl = -9235.70),
            TransactionItem(transactionId = "8", timestamp = "2026-10-06", side = "SELL", symbol = "AHCL", quantity = 6250, fillPrice = 29.62, grossAmount = 185112.68, realizedPnl = -5043.86),
            TransactionItem(transactionId = "9", timestamp = "2026-10-07", side = "SELL", symbol = "RAYMOND", quantity = 168, fillPrice = 1235.27, grossAmount = 207525.2, realizedPnl = 8464.88),
            TransactionItem(transactionId = "10", timestamp = "2026-10-08", side = "SELL", symbol = "INDSWFTLAB", quantity = 512, fillPrice = 366.6, grossAmount = 187700.35, realizedPnl = -11554.56),
            TransactionItem(transactionId = "11", timestamp = "2026-10-09", side = "SELL", symbol = "SIGMAADV", quantity = 160, fillPrice = 997.6, grossAmount = 159618.6, realizedPnl = -22867.59),
            TransactionItem(transactionId = "12", timestamp = "2026-10-13", side = "SELL", symbol = "SHILPAMED", quantity = 161, fillPrice = 974.1, grossAmount = 156830.1, realizedPnl = -6604.90)
        )

        val positions = listOf(
            PositionItem(
                positionId = "1",
                symbol = "TFCILTD",
                quantity = 1426,
                entryPrice = 139.48,
                currentPrice = 157.82,
                marketValue = 225051.32,
                unrealizedPnl = 26154.37,
                unrealizedPnlPct = 13.15,
                peakPrice = 161.95,
                entryDate = "2026-10-05",
                status = "OPEN"
            ),
            PositionItem(
                positionId = "2",
                symbol = "SBC",
                quantity = 3043,
                entryPrice = 62.84,
                currentPrice = 64.21,
                marketValue = 195391.03,
                unrealizedPnl = 4168.91,
                unrealizedPnlPct = 2.18,
                peakPrice = 66.35,
                entryDate = "2026-10-12",
                status = "OPEN"
            )
        )

        val state = calculateLiveMilestones(transactions, positions)

        assertEquals(12, state.totalClosedTrades)
        assertEquals(4, state.winsCount)
        assertEquals(8, state.lossesCount)
        assertEquals(33.33, state.winRatePct, 0.1)
        assertTrue("Average win should be around +17%", state.avgWinPct > 15.0)
        assertTrue("Average loss should be around -5.4%", state.avgLossPct < -4.0 && state.avgLossPct > -7.0)
        assertTrue("Payoff ratio should exceed 3.0x", state.payoffRatio >= 3.0)
        assertEquals("EDGE WORKING (PROVEN ASYMMETRY)", state.verdictTitle)
        assertEquals("PASS", state.verdictStatus)
        assertEquals(2, state.activeRunnersCount)
        assertTrue("Runners summary should mention TFCILTD", state.activeRunnersSummary.contains("TFCILTD"))
    }

    @Test
    fun testEmptyTransactionsGracefulDefault() {
        val state = calculateLiveMilestones(emptyList(), emptyList())
        assertEquals(0, state.totalClosedTrades)
        assertEquals(0.0, state.winRatePct, 0.01)
        assertEquals("ACCUMULATING SAMPLE", state.verdictTitle)
        assertEquals("ACCUMULATING", state.verdictStatus)
    }
}
