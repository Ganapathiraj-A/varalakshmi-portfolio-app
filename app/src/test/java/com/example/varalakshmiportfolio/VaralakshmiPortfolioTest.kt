package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.SyncResult
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.StockRecommendationItem
import com.example.varalakshmiportfolio.model.HistoricalPricePoint
import com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
import com.example.varalakshmiportfolio.model.RecommendationHistorySummary
import com.example.varalakshmiportfolio.model.isPositionBoughtToday
import com.example.varalakshmiportfolio.model.calculateReferencePrice
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

    @Test
    fun testRoundPaiseHandlesNaNAndInfinityGracefully() {
        assertEquals(0.0, VaralakshmiRepository.roundPaise(Double.NaN), 0.0001)
        assertEquals(0.0, VaralakshmiRepository.roundPaise(Double.POSITIVE_INFINITY), 0.0001)
        assertEquals(0.0, VaralakshmiRepository.roundPaise(Double.NEGATIVE_INFINITY), 0.0001)
        assertEquals(12.35, VaralakshmiRepository.roundPaise(12.3456), 0.0001)
        assertEquals(-12.35, VaralakshmiRepository.roundPaise(-12.3456), 0.0001)
    }

    @Test
    fun testPositionItemNaNAndZeroDivisionResilience() {
        val nanPos = PositionItem(
            positionId = "NAN_POS",
            symbol = "NAN_CORP",
            quantity = 10,
            entryPrice = Double.NaN,
            currentPrice = Double.NaN,
            marketValue = 0.0,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 0.0,
            entryDate = "2026-09-21 00:00:00",
            previousClose = Double.NaN
        )

        assertEquals(0.0, nanPos.todayPriceChange, 0.0001)
        assertEquals(0.0, nanPos.todayPriceChangePct, 0.0001)
        assertEquals(0.0, nanPos.todayValueChange, 0.0001)
        assertEquals(1.0, nanPos.returnMultiplier, 0.0001)
        assertEquals(0.0, nanPos.referencePrice, 0.0001)
    }

    @Test
    fun testPositionItemTodayValueChangeFormulaInvariant() {
        // Verify formula: todayValueChange strictly equals quantity * todayPriceChange
        val pos = PositionItem(
            positionId = "TEST_POS_FORMULA",
            symbol = "TEST",
            quantity = 350,
            entryPrice = 120.0,
            currentPrice = 123.45,
            marketValue = 43207.50,
            unrealizedPnl = 1207.50,
            unrealizedPnlPct = 2.88,
            peakPrice = 125.0,
            entryDate = "2026-09-21 10:00:00",
            previousClose = 121.15
        )

        val expectedPriceChange = VaralakshmiRepository.roundPaise(123.45 - 121.15) // 2.30
        assertEquals(expectedPriceChange, pos.todayPriceChange, 0.001)
        val expectedValueChange = VaralakshmiRepository.roundPaise(350 * expectedPriceChange) // 350 * 2.30 = 805.00
        assertEquals(expectedValueChange, pos.todayValueChange, 0.001)
        assertEquals(expectedValueChange, VaralakshmiRepository.roundPaise(pos.quantity * pos.todayPriceChange), 0.001)
    }

    @Test
    fun testLiveSyncHandlesEmptyPositionsArrayAsFullLiquidation() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """{"status":"OK","active":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshData("http://localhost:${server.address.port}")

            assertTrue("Empty active array must succeed as complete liquidation: ${(result as? SyncResult.OfflineCacheFallback)?.message}", result is SyncResult.Success)
            assertEquals(0, result.positions.size)
            assertEquals(0, result.summary.activeSlots)
            assertEquals(0.00, result.summary.todayPnl, 0.001)
            assertEquals(0.00, result.summary.todayPnlPct, 0.001)
            assertEquals(0.00, result.summary.deployedCapital, 0.001)
            // Available capital equals allocated (100,000) when deployed=0 and realized=0
            assertEquals(100000.00, result.summary.availableCapital, 0.001)
            assertEquals(100000.00, result.summary.totalNav, 0.001)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testDiskPersistenceAcrossAppRestarts() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_cache_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)

            // 1. Initial repository instance
            val repo1 = VaralakshmiRepository()
            val initialNav = repo1.getCachedSummary().totalNav
            assertEquals(109268.80, initialNav, 0.001)

            // 2. Perform an exit to mutate state and trigger saveToDisk
            val (updatedSummary, updatedPositions, updatedTransactions) = repo1.removePosition("STLNETWORK")
            assertEquals(2, updatedPositions.size)
            assertEquals(39015.18, updatedSummary.availableCapital, 0.001)

            // 3. Verify portfolio_cache.json was written to disk
            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            assertTrue("Cache file must exist on disk after trade exit or refresh", cacheFile.exists())

            // 4. Simulate process restart by creating a new repository instance
            val repo2 = VaralakshmiRepository()
            val reloadedSummary = repo2.getCachedSummary()
            val reloadedPositions = repo2.getCachedPositions()
            val reloadedTransactions = repo2.getCachedTransactions()

            // 5. Must NOT show the old seed data, must show the latest data from the last session!
            assertEquals(2, reloadedPositions.size)
            assertTrue("STLNETWORK must not be in reloaded positions", reloadedPositions.none { it.symbol == "STLNETWORK" })
            assertEquals(updatedSummary.totalNav, reloadedSummary.totalNav, 0.001)
            assertEquals(39015.18, reloadedSummary.availableCapital, 0.001)
            assertEquals(updatedSummary.realizedPnl, reloadedSummary.realizedPnl, 0.001)
            assertEquals(updatedTransactions.size, reloadedTransactions.size)
            assertEquals("SELL", reloadedTransactions.first().side)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLiveSyncDefensiveAgainstNullAndMalformedFields() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """
                    {
                        "status": "OK",
                        "active": [
                            {
                                "symbol": "NULL_TEST",
                                "quantity": 100,
                                "entry_price": 50.0,
                                "current_price": 55.0,
                                "today_price_change": null,
                                "change_pct": null,
                                "today_value_change": null,
                                "previous_close": null,
                                "market_value": null,
                                "unrealized_pnl": null
                            }
                        ]
                    }
                """.trimIndent()
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshData("http://localhost:${server.address.port}")

            assertTrue("Payload with null fields must parse safely", result is SyncResult.Success)
            assertEquals(1, result.positions.size)
            val pos = result.positions[0]
            assertEquals("NULL_TEST", pos.symbol)
            assertEquals(100, pos.quantity)
            assertEquals(55.0, pos.currentPrice, 0.001)
            // Fallback calculation: ref = entryPrice (50.0), todayChg = 55.0 - 50.0 = 5.0
            assertEquals(5.00, pos.todayPriceChange, 0.001)
            assertEquals(10.00, pos.todayPriceChangePct, 0.01)
            assertEquals(500.00, pos.todayValueChange, 0.001)
            assertEquals(500.00, result.summary.todayPnl, 0.001)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testPositionItemWithZeroOrUnknownReferencePriceDoesNotDrift() {
        val pos = PositionItem(
            positionId = "UNKNOWN_REF",
            symbol = "MYSTERY",
            quantity = 100,
            entryPrice = 0.0,
            currentPrice = 150.00,
            marketValue = 15000.00,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 150.00,
            entryDate = "2026-09-21 09:15:00",
            previousClose = 0.0
        )
        // With unknown reference price (0.0), today's price change must evaluate to 0.0, not currentPrice!
        assertEquals(0.00, pos.referencePrice, 0.001)
        assertEquals(0.00, pos.todayPriceChange, 0.001)
        assertEquals(0.00, pos.todayPriceChangePct, 0.001)
        assertEquals(0.00, pos.todayValueChange, 0.001)
    }

    @Test
    fun testLoadFromDiskGracefullyHandlesLegacyOrMissingTodayPriceChange() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_legacy_cache_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)

            // Write a legacy cache where positions do NOT have todayPriceChange or todayValueChange
            val legacyJson = """
                {
                    "summary": {
                        "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                        "totalNav": 105000.00,
                        "allocatedCapital": 100000.00,
                        "deployedCapital": 95000.00,
                        "availableCapital": 5000.00,
                        "realizedPnl": 0.0,
                        "unrealizedPnl": 5000.00,
                        "totalPnl": 5000.00,
                        "totalPnlPct": 5.0,
                        "activeSlots": 1,
                        "maxSlots": 3,
                        "lastUpdated": "2026-09-20 15:30:00"
                    },
                    "positions": [
                        {
                            "positionId": "P_LEGACY",
                            "symbol": "LEGACY_TICKER",
                            "quantity": 200,
                            "entryPrice": 50.00,
                            "currentPrice": 60.00,
                            "previousClose": 55.00,
                            "marketValue": 12000.00,
                            "unrealizedPnl": 2000.00,
                            "unrealizedPnlPct": 20.00
                        }
                    ],
                    "transactions": []
                }
            """.trimIndent()

            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            cacheFile.writeText(legacyJson, Charsets.UTF_8)

            val repo = VaralakshmiRepository()
            val positions = repo.getCachedPositions()
            val summary = repo.getCachedSummary()

            assertEquals(1, positions.size)
            val p = positions[0]
            assertEquals("LEGACY_TICKER", p.symbol)
            // refPrice = 55.00 -> todayPriceChange must be 60.00 - 55.00 = 5.00
            assertEquals(5.00, p.todayPriceChange, 0.001)
            assertEquals(9.09, p.todayPriceChangePct, 0.01)
            // todayValueChange = 200 * 5.00 = 1000.00
            assertEquals(1000.00, p.todayValueChange, 0.001)

            // Summary todayPnl must align with positions sum (1000.00), not default seed!
            assertEquals(1000.00, summary.todayPnl, 0.001)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testLateInitializationLoadsPersistedCacheUponAccess() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_late_init_test").toFile()
        try {
            // Write cache file directly
            val cacheJson = """
                {
                    "summary": {
                        "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                        "totalNav": 115000.00,
                        "allocatedCapital": 100000.00,
                        "deployedCapital": 100000.00,
                        "availableCapital": 0.00,
                        "realizedPnl": 0.0,
                        "unrealizedPnl": 15000.00,
                        "totalPnl": 15000.00,
                        "totalPnlPct": 15.0,
                        "todayPnl": 2500.00,
                        "todayPnlPct": 2.22,
                        "activeSlots": 1,
                        "maxSlots": 3,
                        "lastUpdated": "2026-09-21 11:30:00"
                    },
                    "positions": [
                        {
                            "positionId": "P_LATE",
                            "symbol": "LATE_STOCK",
                            "quantity": 100,
                            "entryPrice": 100.00,
                            "currentPrice": 125.00,
                            "previousClose": 100.00,
                            "todayPriceChange": 25.00,
                            "todayPriceChangePct": 25.00,
                            "todayValueChange": 2500.00,
                            "marketValue": 12500.00,
                            "unrealizedPnl": 2500.00,
                            "unrealizedPnlPct": 25.00
                        }
                    ],
                    "transactions": []
                }
            """.trimIndent()
            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            cacheFile.writeText(cacheJson, Charsets.UTF_8)

            // Instantiate repository BEFORE initialize() is called
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            val repo = VaralakshmiRepository()

            // Now initialize cacheDirectory
            VaralakshmiRepository.initialize(tempDir)

            // When accessing getCachedSummary(), it must lazily load from disk!
            val summary = repo.getCachedSummary()
            assertEquals(115000.00, summary.totalNav, 0.001)
            assertEquals(2500.00, summary.todayPnl, 0.001)

            val positions = repo.getCachedPositions()
            assertEquals(1, positions.size)
            assertEquals("LATE_STOCK", positions[0].symbol)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testServerUrlAndAuthTokenPersistence() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_server_config_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)

            val repo1 = VaralakshmiRepository()
            assertEquals(VaralakshmiRepository.DEFAULT_SERVER_URL, repo1.getServerUrl())

            // Update server configuration
            val customUrl = "http://192.168.1.100:8088"
            val customToken = "custom_jwt_token_12345"
            repo1.setServerConfig(customUrl, customToken)

            assertEquals(customUrl, repo1.getServerUrl())
            assertEquals(customToken, repo1.getAuthToken())

            // Create new repository instance simulating process restart
            val repo2 = VaralakshmiRepository()
            assertEquals(customUrl, repo2.getServerUrl())
            assertEquals(customToken, repo2.getAuthToken())
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDiskCacheCorruptJsonGracefulRecovery() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_corrupt_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)
            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            // Write malformed/truncated JSON directly to cache file
            cacheFile.writeText("{\"summary\": { \"totalNav\": 99999.0, \"positions\": [ broken json ...")

            // Repository instance must recover safely without throwing JSONException
            val repo = VaralakshmiRepository()
            val summary = repo.getCachedSummary()
            val positions = repo.getCachedPositions()

            // Invariant: Must fall back to clean seed baseline, not corrupted or partial state
            assertEquals(109268.80, summary.totalNav, 0.001)
            assertEquals(3, positions.size)
            assertEquals(4007.77, summary.todayPnl, 0.001)

            // The corrupt file should be quarantined or cleared
            val corruptBackup = java.io.File(tempDir, "${VaralakshmiRepository.CACHE_FILE_NAME}.corrupt")
            assertTrue("Corrupt file must be quarantined or deleted", corruptBackup.exists() || !cacheFile.exists())
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDiskCacheEmptyFileGracefulRecovery() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_empty_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)
            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            cacheFile.writeText("") // 0 bytes

            val repo = VaralakshmiRepository()
            val summary = repo.getCachedSummary()
            assertEquals(109268.80, summary.totalNav, 0.001)
            assertEquals(3, repo.getCachedPositions().size)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testAutoMakeCacheDirectoryIfMissing() {
        val baseTempDir = java.nio.file.Files.createTempDirectory("varalakshmi_nested_test").toFile()
        try {
            val nestedCacheDir = java.io.File(baseTempDir, "sub/deep/directory")
            assertFalse("Nested cache directory must not exist initially", nestedCacheDir.exists())

            VaralakshmiRepository.initialize(nestedCacheDir)

            val repo = VaralakshmiRepository()
            // Mutate state to trigger saveToDisk
            repo.removePosition("STLNETWORK")

            val cacheFile = java.io.File(nestedCacheDir, VaralakshmiRepository.CACHE_FILE_NAME)
            assertTrue("Nested cache directory must be automatically created", nestedCacheDir.exists())
            assertTrue("Cache file must be persisted successfully in nested directory", cacheFile.exists())
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            baseTempDir.deleteRecursively()
        }
    }

    @Test
    fun testZeroChangeNeutralFormattingInvariant() {
        val flatPosition = PositionItem(
            positionId = "FLAT_POS",
            symbol = "FLAT",
            quantity = 100,
            entryPrice = 250.00,
            currentPrice = 250.00,
            marketValue = 25000.00,
            unrealizedPnl = 0.00,
            unrealizedPnlPct = 0.00,
            peakPrice = 250.00,
            entryDate = "2026-09-21 10:00:00",
            previousClose = 250.00
        )

        assertEquals(0.00, flatPosition.todayPriceChange, 0.0001)
        assertEquals(0.00, flatPosition.todayPriceChangePct, 0.0001)
        assertEquals(0.00, flatPosition.todayValueChange, 0.0001)
        assertEquals(1.00, flatPosition.returnMultiplier, 0.0001)
    }

    @Test
    fun testClearDiskCacheRemovesAllArtifacts() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_clear_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)
            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            val corruptFile = java.io.File(tempDir, "${VaralakshmiRepository.CACHE_FILE_NAME}.corrupt")
            val tmpFile = java.io.File(tempDir, "${VaralakshmiRepository.CACHE_FILE_NAME}.tmp")

            cacheFile.writeText("{}")
            corruptFile.writeText("corrupt")
            tmpFile.writeText("tmp")

            val repo = VaralakshmiRepository()
            repo.clearDiskCache()

            assertFalse("Cache file must be removed", cacheFile.exists())
            assertFalse("Corrupt file must be removed", corruptFile.exists())
            assertFalse("Tmp file must be removed", tmpFile.exists())
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMarketIndexNiftyDataMappingAndFormatting() {
        val repository = VaralakshmiRepository()
        val nifty = repository.getCachedNifty()

        assertEquals("NIFTY 50", nifty.symbol)
        assertTrue(nifty.ltp > 20000.0)
        assertEquals(72.05, nifty.change, 0.001)
        assertEquals(0.31, nifty.changePct, 0.01)
        assertTrue(nifty.isPositive)
        assertEquals("▲ +72.05 (+0.31%)", nifty.formattedChange)
        assertEquals("23,401.05", nifty.formattedLtp)
    }

    @Test
    fun testNiftyDiskPersistenceAndRestoration() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_nifty_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)
            val repo1 = VaralakshmiRepository()
            // Force save to disk by updating server config
            repo1.setServerConfig(repo1.getServerUrl())

            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            assertTrue(cacheFile.exists())
            val content = cacheFile.readText()
            assertTrue(content.contains("NIFTY 50"))

            val repo2 = VaralakshmiRepository()
            val restoredNifty = repo2.getCachedNifty()
            assertEquals("NIFTY 50", restoredNifty.symbol)
            assertEquals(23401.05, restoredNifty.ltp, 0.01)
            assertEquals(72.05, restoredNifty.change, 0.01)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testTop5RecommendationsParsingAndStateHandling() {
        val repository = VaralakshmiRepository()
        val recs = repository.getCachedRecommendations()

        // 1. Verify Top 5 snapshot candidates count and order
        assertEquals(5, recs.size)
        val symbols = recs.map { it.symbol }
        assertEquals(listOf("RAYMOND", "XTRANET", "AAREYDRUGS", "GCSL", "GMMPFAUDLR"), symbols)

        // 2. Verify ranks 1 to 5
        assertEquals(listOf(1, 2, 3, 4, 5), recs.map { it.rank })

        // 3. Verify prices and scores
        val raymond = recs[0]
        assertEquals("RAYMOND", raymond.symbol)
        assertEquals(1, raymond.rank)
        assertEquals(1114.65, raymond.price, 0.001)
        assertEquals(10.02, raymond.score, 0.001)
        assertEquals(1390.00, raymond.targetPrice, 0.001)
        assertEquals(980.00, raymond.stopLossPrice, 0.001)
        assertEquals(45, raymond.historical2mPoints.size)

        val xtranet = recs[1]
        assertEquals("XTRANET", xtranet.symbol)
        assertEquals(2, xtranet.rank)
        assertEquals(317.65, xtranet.price, 0.001)
        assertEquals(8.91, xtranet.score, 0.001)
        assertEquals(45, xtranet.historical2mPoints.size)

        val aareydrugs = recs[2]
        assertEquals("AAREYDRUGS", aareydrugs.symbol)
        assertEquals(3, aareydrugs.rank)
        assertEquals(102.00, aareydrugs.price, 0.001)
        assertEquals(8.87, aareydrugs.score, 0.001)
        assertEquals(45, aareydrugs.historical2mPoints.size)

        val gcsl = recs[3]
        assertEquals("GCSL", gcsl.symbol)
        assertEquals(4, gcsl.rank)
        assertEquals(642.70, gcsl.price, 0.001)
        assertEquals(8.77, gcsl.score, 0.001)
        assertEquals(45, gcsl.historical2mPoints.size)

        val gmmpfaudlr = recs[4]
        assertEquals("GMMPFAUDLR", gmmpfaudlr.symbol)
        assertEquals(5, gmmpfaudlr.rank)
        assertEquals(1432.60, gmmpfaudlr.price, 0.001)
        assertEquals(8.76, gmmpfaudlr.score, 0.001)
        assertEquals(45, gmmpfaudlr.historical2mPoints.size)

        // 4. Test JSON parsing from live HTTPS payloads
        val testJson = """
            {
                "status": "OK",
                "recommendations": [
                    {
                        "rank": 1,
                        "symbol": "CUSTOM_TICKER",
                        "price": 500.0,
                        "score": 99.0,
                        "target_price": 650.0,
                        "stop_loss_price": 460.0,
                        "historical2m_points": [
                            {"date": "2026-08-01", "price": 400.0},
                            {"date": "2026-09-01", "price": 500.0}
                        ]
                    }
                ]
            }
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseRecommendationsJson(testJson)
        assertEquals(1, parsed.size)
        assertEquals("CUSTOM_TICKER", parsed[0].symbol)
        assertEquals(500.0, parsed[0].price, 0.001)
        assertEquals(99.0, parsed[0].score, 0.001)
        assertEquals(650.0, parsed[0].targetPrice, 0.001)
        assertEquals(460.0, parsed[0].stopLossPrice, 0.001)
        assertEquals(2, parsed[0].historical2mPoints.size)
        assertEquals(25.0, parsed[0].return2mPct, 0.01)

        // 4b. Test candidates JSON parsing matching web stage/status payload
        val candidatesJson = """
            {
                "success": true,
                "candidates": [
                    {
                        "symbol": "RAYMOND",
                        "alpha_score": 10.02,
                        "ltp": 1114.65,
                        "rank": 1
                    }
                ]
            }
        """.trimIndent()
        val parsedCand = VaralakshmiRepository.parseRecommendationsJson(candidatesJson)
        assertEquals(1, parsedCand.size)
        assertEquals("RAYMOND", parsedCand[0].symbol)
        assertEquals(1114.65, parsedCand[0].price, 0.001)
        assertEquals(10.02, parsedCand[0].score, 0.001)

        // 5. Test empty / malformed payload resilience
        assertEquals(0, VaralakshmiRepository.parseRecommendationsJson("").size)
        assertEquals(0, VaralakshmiRepository.parseRecommendationsJson("{ broken json").size)
        assertEquals(0, VaralakshmiRepository.parseRecommendationsJson("[]").size)

        // 6. Test ViewModel UI state initialization
        val viewModel = VaralakshmiViewModel(repository, autoRefresh = false)
        assertEquals(5, viewModel.uiState.value.recommendations.size)
        assertEquals("RAYMOND", viewModel.uiState.value.recommendations[0].symbol)
    }

    @Test
    fun testTwoMonthHistoricalDataPointSortingAndMinMaxCalculation() {
        val repository = VaralakshmiRepository()
        val recs = repository.getCachedRecommendations()
        val raymond = recs.first { it.symbol == "RAYMOND" }

        // 1. Verify chronological sorting across the 45 trading days
        for (i in 0 until raymond.historical2mPoints.size - 1) {
            val curr = raymond.historical2mPoints[i].date
            val next = raymond.historical2mPoints[i + 1].date
            assertTrue("Dates must be monotonically non-decreasing: $curr <= $next", curr <= next)
        }

        // 2. Verify min and max calculation
        val expectedMax = raymond.historical2mPoints.maxOf { it.price }
        val expectedMin = raymond.historical2mPoints.minOf { it.price }
        assertEquals(expectedMax, raymond.high2m, 0.001)
        assertEquals(expectedMin, raymond.low2m, 0.001)
        assertEquals(1116.95, raymond.high2m, 0.001)
        assertEquals(576.80, raymond.low2m, 0.001)

        // 3. Verify return2mPct formula: ((last - first) / first) * 100
        val firstPrice = raymond.historical2mPoints.first().price // 605.6
        val lastPrice = raymond.historical2mPoints.last().price   // 1114.65
        val expectedPct = ((lastPrice - firstPrice) / firstPrice) * 100.0
        assertEquals(expectedPct, raymond.return2mPct, 0.01)
        assertEquals(84.06, raymond.return2mPct, 0.01)

        // 4. Edge cases: Empty historical points
        val emptyItem = StockRecommendationItem(
            rank = 99,
            symbol = "EMPTY_PTS",
            price = 150.00,
            score = 80.0,
            historical2mPoints = emptyList()
        )
        assertEquals(150.00, emptyItem.high2m, 0.001)
        assertEquals(150.00, emptyItem.low2m, 0.001)
        assertEquals(0.00, emptyItem.return2mPct, 0.001)

        // 5. Edge cases: Single historical point
        val singleItem = StockRecommendationItem(
            rank = 99,
            symbol = "SINGLE_PT",
            price = 200.00,
            score = 85.0,
            historical2mPoints = listOf(HistoricalPricePoint("2026-09-01", 205.00))
        )
        assertEquals(205.00, singleItem.high2m, 0.001)
        assertEquals(205.00, singleItem.low2m, 0.001)
        assertEquals(0.00, singleItem.return2mPct, 0.001)

        // 6. Edge cases: Downward trending trajectory (negative return)
        val dropItem = StockRecommendationItem(
            rank = 10,
            symbol = "DOWN_STOCK",
            price = 80.00,
            score = 65.0,
            historical2mPoints = listOf(
                HistoricalPricePoint("2026-08-01", 100.00),
                HistoricalPricePoint("2026-08-15", 90.00),
                HistoricalPricePoint("2026-09-01", 80.00)
            )
        )
        assertEquals(100.00, dropItem.high2m, 0.001)
        assertEquals(80.00, dropItem.low2m, 0.001)
        assertEquals(-20.00, dropItem.return2mPct, 0.01)

        // 7. Verify explicit sorting logic
        val unsortedList = listOf(
            HistoricalPricePoint("2026-09-10", 120.0),
            HistoricalPricePoint("2026-07-25", 95.0),
            HistoricalPricePoint("2026-08-15", 110.0)
        )
        val sortedList = unsortedList.sortedBy { it.date }
        assertEquals("2026-07-25", sortedList[0].date)
        assertEquals("2026-08-15", sortedList[1].date)
        assertEquals("2026-09-10", sortedList[2].date)
    }

    @Test
    fun testRecommendationClickInteractionStateUpdates() {
        val repository = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository, autoRefresh = false)

        // 1. Initial state: no recommendation selected for chart
        assertNull(viewModel.uiState.value.selectedRecommendationForChart)
        val recs = viewModel.uiState.value.recommendations
        assertEquals(5, recs.size)

        // 2. Select Recommendation #1 (RAYMOND)
        viewModel.selectRecommendation(recs[0])
        val selected1 = viewModel.uiState.value.selectedRecommendationForChart
        assertNotNull(selected1)
        assertEquals("RAYMOND", selected1?.symbol)
        assertEquals(1, selected1?.rank)
        assertEquals(1114.65, selected1?.price ?: 0.0, 0.001)
        assertEquals(45, selected1?.historical2mPoints?.size)

        // 3. Switch to Recommendation #2 (XTRANET)
        viewModel.selectRecommendation(recs[1])
        val selected2 = viewModel.uiState.value.selectedRecommendationForChart
        assertNotNull(selected2)
        assertEquals("XTRANET", selected2?.symbol)
        assertEquals(2, selected2?.rank)
        assertEquals(317.65, selected2?.price ?: 0.0, 0.001)

        // 4. Dismiss modal chart
        viewModel.dismissRecommendationChart()
        assertNull(viewModel.uiState.value.selectedRecommendationForChart)
    }

    @Test
    fun testRecommendationDiskPersistenceAndRestoration() {
        val tempDir = java.nio.file.Files.createTempDirectory("varalakshmi_rec_test").toFile()
        try {
            VaralakshmiRepository.initialize(tempDir)
            val repo1 = VaralakshmiRepository()

            // Save to disk by updating server config
            repo1.setServerConfig(repo1.getServerUrl())

            val cacheFile = java.io.File(tempDir, VaralakshmiRepository.CACHE_FILE_NAME)
            assertTrue("Cache file must be persisted", cacheFile.exists())
            val content = cacheFile.readText()
            assertTrue("Cache file must contain recommendations", content.contains("recommendations"))
            assertTrue("Cache file must contain RAYMOND", content.contains("RAYMOND"))
            assertTrue("Cache file must contain XTRANET", content.contains("XTRANET"))

            // Instantiate second repo simulating process restart
            val repo2 = VaralakshmiRepository()
            val restoredRecs = repo2.getCachedRecommendations()
            assertEquals(5, restoredRecs.size)
            assertEquals("RAYMOND", restoredRecs[0].symbol)
            assertEquals(1, restoredRecs[0].rank)
            assertEquals(1114.65, restoredRecs[0].price, 0.001)
            assertEquals(45, restoredRecs[0].historical2mPoints.size)
            assertEquals(1116.95, restoredRecs[0].high2m, 0.001)
            assertEquals(576.80, restoredRecs[0].low2m, 0.001)
        } finally {
            VaralakshmiRepository.resetCacheDirectoryForTesting()
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testRecommendationItemNaNAndInfinityResilience() {
        // 1. First price is NaN
        val nanFirst = StockRecommendationItem(
            rank = 1,
            symbol = "NAN_FIRST",
            price = 100.0,
            score = 90.0,
            historical2mPoints = listOf(
                HistoricalPricePoint("2026-08-01", Double.NaN),
                HistoricalPricePoint("2026-09-01", 120.0)
            )
        )
        // Must NOT throw NumberFormatException: Infinite or NaN
        assertEquals(0.0, nanFirst.return2mPct, 0.001)
        assertEquals(120.0, nanFirst.high2m, 0.001)
        assertEquals(120.0, nanFirst.low2m, 0.001)

        // 2. Last price is Infinity
        val infLast = StockRecommendationItem(
            rank = 2,
            symbol = "INF_LAST",
            price = 200.0,
            score = 85.0,
            historical2mPoints = listOf(
                HistoricalPricePoint("2026-08-01", 150.0),
                HistoricalPricePoint("2026-09-01", Double.POSITIVE_INFINITY)
            )
        )
        assertEquals(0.0, infLast.return2mPct, 0.001)
        assertEquals(150.0, infLast.high2m, 0.001)
        assertEquals(150.0, infLast.low2m, 0.001)

        // 3. All points NaN - fall back to base price
        val allNan = StockRecommendationItem(
            rank = 3,
            symbol = "ALL_NAN",
            price = 350.0,
            score = 75.0,
            historical2mPoints = listOf(
                HistoricalPricePoint("2026-08-01", Double.NaN),
                HistoricalPricePoint("2026-09-01", Double.NaN)
            )
        )
        assertEquals(0.0, allNan.return2mPct, 0.001)
        assertEquals(350.0, allNan.high2m, 0.001)
        assertEquals(350.0, allNan.low2m, 0.001)

        // 4. First price <= 0.0
        val zeroFirst = StockRecommendationItem(
            rank = 4,
            symbol = "ZERO_FIRST",
            price = 50.0,
            score = 70.0,
            historical2mPoints = listOf(
                HistoricalPricePoint("2026-08-01", 0.0),
                HistoricalPricePoint("2026-09-01", 60.0)
            )
        )
        assertEquals(0.0, zeroFirst.return2mPct, 0.001)
    }

    @Test
    fun testRecommendationFlatPriceAndSinglePointTrajectory() {
        // Completely flat stock price across all 45 days
        val flatPoints = (1..45).map { HistoricalPricePoint("2026-08-$it", 500.0) }
        val flatItem = StockRecommendationItem(
            rank = 1,
            symbol = "FLAT_STOCK",
            price = 500.0,
            score = 88.0,
            historical2mPoints = flatPoints
        )
        assertEquals(0.0, flatItem.return2mPct, 0.001)
        assertEquals(500.0, flatItem.high2m, 0.001)
        assertEquals(500.0, flatItem.low2m, 0.001)

        // Single historical price point
        val singleItem = StockRecommendationItem(
            rank = 2,
            symbol = "SINGLE_STOCK",
            price = 250.0,
            score = 82.0,
            historical2mPoints = listOf(HistoricalPricePoint("2026-09-20", 255.0))
        )
        assertEquals(0.0, singleItem.return2mPct, 0.001)
        assertEquals(255.0, singleItem.high2m, 0.001)
        assertEquals(255.0, singleItem.low2m, 0.001)
    }

    @Test
    fun testOfflineSyncResultIncludesRecommendations() = runTest {
        val repo = VaralakshmiRepository()
        val result = repo.refreshData("http://127.0.0.1:59998")

        assertTrue("Must be offline fallback", result is SyncResult.OfflineCacheFallback)
        assertEquals(5, result.recommendations.size)
        assertEquals("RAYMOND", result.recommendations[0].symbol)
        assertEquals(1, result.recommendations[0].rank)
        assertEquals("GMMPFAUDLR", result.recommendations[4].symbol)
        assertEquals(5, result.recommendations[4].rank)
    }

    @Test
    fun testLiveSyncIncludesRecommendationsWhenAvailable() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """{"status":"OK","active":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/recommendations") { exchange ->
                val response = """
                    {
                        "status": "OK",
                        "recommendations": [
                            {
                                "rank": 1,
                                "symbol": "LIVE_WINNER",
                                "price": 777.0,
                                "score": 99.5,
                                "target_price": 950.0,
                                "stop_loss_price": 700.0,
                                "historical2m_points": [
                                    {"date": "2026-08-01", "price": 600.0},
                                    {"date": "2026-09-23", "price": 777.0}
                                ]
                            }
                        ]
                    }
                """.trimIndent()
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshData("http://localhost:${server.address.port}")

            assertTrue("Must succeed", result is SyncResult.Success)
            assertEquals(1, result.recommendations.size)
            assertEquals("LIVE_WINNER", result.recommendations[0].symbol)
            assertEquals(777.0, result.recommendations[0].price, 0.001)
            assertEquals(99.5, result.recommendations[0].score, 0.001)
            assertEquals(29.50, result.recommendations[0].return2mPct, 0.01)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testRecommendationReverseChronologicalHistoricalPointsCalculation() {
        // Backend returns descending (newest first) historical dates
        val reversePoints = listOf(
            HistoricalPricePoint("2026-09-23", 500.0), // Latest: 500
            HistoricalPricePoint("2026-08-15", 450.0),
            HistoricalPricePoint("2026-07-23", 400.0)  // Oldest: 400 (Gain: +25%)
        )
        val item = StockRecommendationItem(
            rank = 1,
            symbol = "REVERSE_PTS",
            price = 500.0,
            score = 91.0,
            historical2mPoints = reversePoints
        )

        // return2mPct must sort ascending first: (500 - 400) / 400 * 100 = +25.0% (NOT -20%)
        assertEquals(25.00, item.return2mPct, 0.01)
        assertEquals(500.00, item.high2m, 0.001)
        assertEquals(400.00, item.low2m, 0.001)
    }

    @Test
    fun testRecommendationBundledInPositionsPayload() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """
                    {
                        "status": "OK",
                        "active": [],
                        "recommendations": [
                            {
                                "rank": 1,
                                "symbol": "BUNDLED_CO",
                                "price": 1250.0,
                                "score": 98.2,
                                "target_price": 1500.0,
                                "stop_loss_price": 1150.0,
                                "historical2m_points": [
                                    {"date": "2026-07-23", "price": 1000.0},
                                    {"date": "2026-09-23", "price": 1250.0}
                                ]
                            }
                        ]
                    }
                """.trimIndent()
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/recommendations") { exchange ->
                // Simulate separate recommendations endpoint 404 or empty
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.close()
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshData("http://localhost:${server.address.port}")

            assertTrue("Must succeed with bundled payload", result is SyncResult.Success)
            assertEquals(1, result.recommendations.size)
            assertEquals("BUNDLED_CO", result.recommendations[0].symbol)
            assertEquals(1250.0, result.recommendations[0].price, 0.001)
            assertEquals(98.2, result.recommendations[0].score, 0.001)
            assertEquals(25.00, result.recommendations[0].return2mPct, 0.01)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testRecommendationFormattedPropertiesWithNaNAndInfiniteValues() {
        val abnormalItem = StockRecommendationItem(
            rank = 1,
            symbol = "BAD_NUMBERS",
            price = Double.NaN,
            score = Double.POSITIVE_INFINITY,
            targetPrice = Double.NaN,
            stopLossPrice = Double.NEGATIVE_INFINITY
        )

        assertEquals("₹0.00", abnormalItem.formattedPrice)
        assertEquals("0.0", abnormalItem.formattedScore)
        assertEquals("₹0.00", abnormalItem.formattedTarget)
        assertEquals("₹0.00", abnormalItem.formattedStopLoss)
    }

    @Test
    fun testRecommendationHighLowIgnoresZeroAndNegativePrices() {
        val corruptedPoints = listOf(
            HistoricalPricePoint("2026-08-01", 0.0),
            HistoricalPricePoint("2026-08-02", -50.0),
            HistoricalPricePoint("2026-08-03", 220.0),
            HistoricalPricePoint("2026-08-04", 260.0)
        )
        val item = StockRecommendationItem(
            rank = 1,
            symbol = "FILTER_TEST",
            price = 260.0,
            score = 80.0,
            historical2mPoints = corruptedPoints
        )

        assertEquals(260.0, item.high2m, 0.001)
        assertEquals(220.0, item.low2m, 0.001)
    }

    @Test
    fun testRecommendationHistoryDefaultSeedLoadsAndCalculatesSummary() {
        val repository = VaralakshmiRepository()
        val history = repository.getCachedRecommendationHistory()
        val summary = repository.getCachedRecommendationHistorySummary()

        assertTrue("Recommendation history should contain past 3 months signals", history.isNotEmpty())
        assertEquals(16, history.size)
        assertEquals(16, summary.totalTrades)
        assertTrue("Win rate should be positive", summary.winRatePercent > 50.0)
        assertEquals(12, summary.profitableTrades)
        assertEquals(4, summary.lossTrades)
        assertEquals(3, summary.activeTrades)
        assertTrue("Best trade return should be greater than 20%", summary.bestTradePercent >= 20.0)
        assertTrue("Max loss should be bounded", summary.maxLossPercent < 0.0)
    }

    @Test
    fun testRecommendationHistoryFiltering() {
        val repository = VaralakshmiRepository()
        val history = repository.getCachedRecommendationHistory()

        val profitable = history.filter { it.pnlPercent > 0.0 }
        val losses = history.filter { it.pnlPercent < 0.0 }
        val active = history.filter { it.isActive }

        assertEquals(12, profitable.size)
        assertEquals(4, losses.size)
        assertEquals(3, active.size)
        assertTrue(active.any { it.symbol == "TBZ" })
        assertTrue(active.any { it.symbol == "AHCL" })
        assertTrue(active.any { it.symbol == "STLNETWORK" })
    }

    @Test
    fun testRecommendationHistoryJsonParsing() {
        val sampleJson = """
            {
                "recommendation_history": [
                    {
                        "id": "REC-TEST-1",
                        "symbol": "INFY",
                        "sector": "Information Technology",
                        "entry_date": "2026-07-01",
                        "exit_date": "2026-07-20",
                        "entry_price": 1800.0,
                        "exit_price": 2070.0,
                        "pnl_percent": 15.0,
                        "holding_days": 14,
                        "status": "TRAILING_STOP",
                        "exit_reason": "Trailing Stop (-8%)",
                        "score": 95.0
                    },
                    {
                        "id": "REC-TEST-2",
                        "symbol": "TCS",
                        "sector": "Information Technology",
                        "entry_date": "2026-08-01",
                        "entry_price": 4200.0,
                        "exit_price": 4032.0,
                        "pnl_percent": -4.0,
                        "holding_days": 3,
                        "status": "CUT_LOSS",
                        "exit_reason": "Cut Loss (-4%)",
                        "score": 90.0
                    }
                ]
            }
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseRecommendationHistoryJson(sampleJson)
        assertEquals(2, parsed.size)
        assertEquals("INFY", parsed[0].symbol)
        assertEquals(15.0, parsed[0].pnlPercent, 0.001)
        assertTrue(parsed[0].isWin)
        assertFalse(parsed[0].isActive)

        assertEquals("TCS", parsed[1].symbol)
        assertEquals(-4.0, parsed[1].pnlPercent, 0.001)
        assertFalse(parsed[1].isWin)
        assertTrue(parsed[1].isActive) // exit_date is null/omitted

        val summary = VaralakshmiRepository.calculateRecommendationHistorySummary(parsed)
        assertEquals(2, summary.totalTrades)
        assertEquals(50.0, summary.winRatePercent, 0.001)
        assertEquals(5.5, summary.avgReturnPercent, 0.001)
        assertEquals(1, summary.profitableTrades)
        assertEquals(1, summary.lossTrades)
    }

    @Test
    fun testRecommendationHistoryViewModelStateTransitions() = runTest {
        val repository = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)

        assertFalse(viewModel.uiState.value.showRecommendationHistory)
        assertEquals("ALL", viewModel.uiState.value.selectedHistoryFilter)

        viewModel.openRecommendationHistory()
        assertTrue(viewModel.uiState.value.showRecommendationHistory)

        viewModel.setHistoryFilter("PROFITABLE")
        assertEquals("PROFITABLE", viewModel.uiState.value.selectedHistoryFilter)
        assertEquals(12, viewModel.uiState.value.filteredRecommendationHistory.size)

        viewModel.setHistoryFilter("LOSS")
        assertEquals(4, viewModel.uiState.value.filteredRecommendationHistory.size)

        viewModel.setHistoryFilter("ACTIVE")
        assertEquals(3, viewModel.uiState.value.filteredRecommendationHistory.size)

        viewModel.closeRecommendationHistory()
        assertFalse(viewModel.uiState.value.showRecommendationHistory)
    }

    @Test
    fun testHistoricalRecommendationItemFormatting() {
        val item = HistoricalRecommendationItem(
            id = "REC-FMT",
            symbol = "TEST",
            sector = "Test Sector",
            entryDate = "2026-06-01",
            exitDate = "2026-06-15",
            entryPrice = 1250.50,
            exitPrice = 1438.075,
            pnlPercent = 15.0,
            holdingDays = 10,
            status = "TRAILING_STOP",
            exitReason = "Trailing Stop"
        )

        assertEquals("₹1,250.50", item.formattedEntryPrice)
        assertEquals("₹1,438.08", item.formattedExitPrice)
        assertEquals("+15.00%", item.formattedPnlPercent)
        assertTrue(item.isWin)
        assertFalse(item.isActive)

        val lossItem = item.copy(pnlPercent = -4.25)
        assertEquals("-4.25%", lossItem.formattedPnlPercent)
        assertFalse(lossItem.isWin)
    }

    @Test
    fun testRepositoryExitPositionOfflineFallback() = runTest {
        val repository = VaralakshmiRepository()
        // Ensure AHCL exists initially
        assertTrue(repository.getCachedPositions().any { it.symbol == "AHCL" })
        
        // Calling exitPosition with unreachable server base URL should cleanly fallback to local removal
        val result = repository.exitPosition(
            strategyId = "VARALAKSHMI_ALPHA_SCALE_35",
            symbol = "AHCL",
            serverBaseUrl = "http://127.0.0.1:59999"
        )
        
        // Verify AHCL is removed from cached holdings
        assertFalse(repository.getCachedPositions().any { it.symbol == "AHCL" })
        assertEquals(2, repository.getCachedPositions().size)
        // Verify result is a failure indicating offline status
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Offline") == true)
    }

    @Test
    fun testPositionBoughtTodayUsesEntryExecutionPriceAsBaseline() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        // Position bought today (e.g. entryDate = today, entryPrice = 1183.36, currentPrice = 1182.55, previousClose = 1203.85)
        val posToday = PositionItem(
            positionId = "POS_TODAY_RAYMOND",
            symbol = "RAYMOND",
            quantity = 25,
            entryPrice = 1183.36,
            currentPrice = 1182.55,
            marketValue = 29563.75,
            unrealizedPnl = -20.25,
            unrealizedPnlPct = -0.07,
            peakPrice = 1183.36,
            entryDate = today,
            previousClose = 1203.85
        )

        // 1. Reference price must reflect entry execution price, not yesterday's close
        assertEquals(1183.36, posToday.referencePrice, 0.001)
        assertTrue(posToday.isBoughtToday)

        // 2. todayPriceChange == -0.81 (1182.55 - 1183.36)
        assertEquals(-0.81, posToday.todayPriceChange, 0.001)

        // 3. todayValueChange == roundPaise(quantity * -0.81), NOT based on 1203.85
        val expectedValChange = VaralakshmiRepository.roundPaise(posToday.quantity * -0.81) // 25 * -0.81 = -20.25
        assertEquals(-20.25, expectedValChange, 0.001)
        assertEquals(expectedValChange, posToday.todayValueChange, 0.001)
        assertNotEquals(VaralakshmiRepository.roundPaise(25 * (1182.55 - 1203.85)), posToday.todayValueChange, 0.001)

        // Also test with explicit "today" string
        val posLiteralToday = posToday.copy(entryDate = "today")
        assertEquals(1183.36, posLiteralToday.referencePrice, 0.001)
        assertEquals(-0.81, posLiteralToday.todayPriceChange, 0.001)
        assertEquals(expectedValChange, posLiteralToday.todayValueChange, 0.001)
    }

    @Test
    fun testPositionHeldPriorToTodayUsesPreviousCloseAsReferencePrice() {
        // Position held prior to today (e.g. entryDate = 2026-09-21, previousClose = 692.45, currentPrice = 674.85)
        val posPrior = PositionItem(
            positionId = "POS_PRIOR_HOLDING",
            symbol = "PRIOR_CORP",
            quantity = 40,
            entryPrice = 650.00,
            currentPrice = 674.85,
            marketValue = 26994.00,
            unrealizedPnl = 994.00,
            unrealizedPnlPct = 3.82,
            peakPrice = 680.00,
            entryDate = "2026-09-21 09:30:00",
            previousClose = 692.45
        )

        // 1. Reference price must use previousClose
        assertFalse(posPrior.isBoughtToday)
        assertEquals(692.45, posPrior.referencePrice, 0.001)

        // 2. todayPriceChange is computed against previousClose (674.85 - 692.45 = -17.60)
        assertEquals(-17.60, posPrior.todayPriceChange, 0.001)

        // 3. todayValueChange == roundPaise(quantity * todayPriceChange)
        val expectedValChange = VaralakshmiRepository.roundPaise(40 * -17.60) // -704.00
        assertEquals(-704.00, expectedValChange, 0.001)
        assertEquals(expectedValChange, posPrior.todayValueChange, 0.001)
    }

    @Test
    fun testSummaryTodayPnlStrictlyEqualsPositionsSumOfTodayValueChange() {
        val repository = VaralakshmiRepository()
        val summary = repository.getCachedSummary()
        val positions = repository.getCachedPositions()

        // Exact equivalence invariant: summary.todayPnl strictly equals positions.sumOf { it.todayValueChange }
        val sumTodayValueChange = VaralakshmiRepository.roundPaise(positions.sumOf { it.todayValueChange })
        assertEquals(summary.todayPnl, sumTodayValueChange, 0.0001)

        // todayPnlPct calculation against previous NAV
        val prevNav = summary.totalNav - summary.todayPnl
        val expectedPct = VaralakshmiRepository.roundPaise((summary.todayPnl / prevNav) * 100.0)
        assertEquals(expectedPct, summary.todayPnlPct, 0.01)
    }

    @Test
    fun testApplyCachedJsonStateRecomputesTodayPnlIgnoringStaleSummaryOverride() {
        val repository = VaralakshmiRepository()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        // JSON payload containing stale/bogus summary todayPnl (99999.00) and positions
        val jsonPayload = """
            {
                "summary": {
                    "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                    "totalNav": 100000.00,
                    "allocatedCapital": 100000.00,
                    "deployedCapital": 80000.00,
                    "availableCapital": 20000.00,
                    "realizedPnl": 0.00,
                    "unrealizedPnl": 500.00,
                    "totalPnl": 500.00,
                    "totalPnlPct": 0.50,
                    "todayPnl": 99999.00,
                    "todayPnlPct": 99.99
                },
                "positions": [
                    {
                        "positionId": "POS_1",
                        "symbol": "RAYMOND",
                        "quantity": 10,
                        "entryPrice": 1183.36,
                        "currentPrice": 1182.55,
                        "entryDate": "$today",
                        "previousClose": 1203.85,
                        "todayPriceChange": -21.30,
                        "todayValueChange": -213.00
                    },
                    {
                        "positionId": "POS_2",
                        "symbol": "TCS",
                        "quantity": 20,
                        "entryPrice": 3000.00,
                        "currentPrice": 3050.00,
                        "entryDate": "2026-09-20",
                        "previousClose": 3020.00,
                        "todayPriceChange": 30.00,
                        "todayValueChange": 600.00
                    }
                ],
                "transactions": []
            }
        """.trimIndent()

        val applied = repository.applyCachedJsonState(jsonPayload)
        assertTrue(applied)

        val positions = repository.getCachedPositions()
        val summary = repository.getCachedSummary()

        // Position 1 bought today uses entryPrice (1183.36) -> change = -0.81, value change = -8.10
        val raymond = positions.first { it.symbol == "RAYMOND" }
        assertEquals(1183.36, raymond.referencePrice, 0.001)
        assertEquals(-0.81, raymond.todayPriceChange, 0.001)
        assertEquals(-8.10, raymond.todayValueChange, 0.001)

        // Position 2 held prior uses previousClose (3020.00) -> change = 30.00, value change = 600.00
        val tcs = positions.first { it.symbol == "TCS" }
        assertEquals(3020.00, tcs.referencePrice, 0.001)
        assertEquals(30.00, tcs.todayPriceChange, 0.001)
        assertEquals(600.00, tcs.todayValueChange, 0.001)

        // Summary todayPnl MUST strictly equal sum of positions: -8.10 + 600.00 = 591.90
        val expectedTodayPnl = VaralakshmiRepository.roundPaise(positions.sumOf { it.todayValueChange })
        assertEquals(591.90, expectedTodayPnl, 0.001)
        assertEquals(expectedTodayPnl, summary.todayPnl, 0.001)
        assertNotEquals(99999.00, summary.todayPnl, 0.001)

        // todayPnlPct must be calculated against prevNav = totalNav - todayPnl
        val prevNav = summary.totalNav - summary.todayPnl
        val expectedPct = VaralakshmiRepository.roundPaise((summary.todayPnl / prevNav) * 100.0)
        assertEquals(expectedPct, summary.todayPnlPct, 0.01)
    }

    @Test
    fun testLiveSyncDynamicReferenceAndStrictDailyDeltaEquivalence() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """
                    {
                        "status": "OK",
                        "active": [
                            {
                                "position_id": "POS_LIVE_1",
                                "symbol": "RAYMOND",
                                "quantity": 10,
                                "entry_price": 1183.36,
                                "current_price": 1182.55,
                                "entry_date": "$today 10:15:00",
                                "previous_close": 1203.85,
                                "today_price_change": -21.30,
                                "today_value_change": -213.00
                            },
                            {
                                "position_id": "POS_LIVE_2",
                                "symbol": "INFY",
                                "quantity": 50,
                                "entry_price": 1800.00,
                                "current_price": 1820.00,
                                "entry_date": "2026-09-20 09:30:00",
                                "previous_close": 1810.00,
                                "today_price_change": 10.00,
                                "today_value_change": 500.00
                            }
                        ]
                    }
                """.trimIndent()
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.syncWithServer("http://localhost:${server.address.port}")

            assertTrue(result is SyncResult.Success)
            val positions = result.positions
            assertEquals(2, positions.size)

            val raymond = positions.first { it.symbol == "RAYMOND" }
            assertEquals(1183.36, raymond.referencePrice, 0.001)
            assertEquals(-0.81, raymond.todayPriceChange, 0.001)
            assertEquals(-8.10, raymond.todayValueChange, 0.001)

            val infy = positions.first { it.symbol == "INFY" }
            assertEquals(1810.00, infy.referencePrice, 0.001)
            assertEquals(10.00, infy.todayPriceChange, 0.001)
            assertEquals(500.00, infy.todayValueChange, 0.001)

            val expectedSum = VaralakshmiRepository.roundPaise(positions.sumOf { it.todayValueChange })
            assertEquals(491.90, expectedSum, 0.001)
            assertEquals(expectedSum, result.summary.todayPnl, 0.001)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testPositionBoughtTodayWithZeroOrNegativeEntryPriceFallsBackToPreviousClose() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val pos = PositionItem(
            positionId = "POS_ZERO_ENTRY",
            symbol = "BAD_ENTRY",
            quantity = 50,
            entryPrice = 0.0,
            currentPrice = 125.0,
            marketValue = 6250.0,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 125.0,
            entryDate = today,
            previousClose = 120.0
        )
        // Since entryPrice is 0.0, calculateReferencePrice falls back to previousClose (120.0)
        assertEquals(120.0, pos.referencePrice, 0.001)
        assertEquals(5.0, pos.todayPriceChange, 0.001)
        assertEquals(250.0, pos.todayValueChange, 0.001)
    }

    @Test
    fun testPositionWithBothZeroEntryPriceAndZeroPreviousCloseProducesZeroPriceAndValueChange() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val pos = PositionItem(
            positionId = "POS_BOTH_ZERO",
            symbol = "NO_REF",
            quantity = 100,
            entryPrice = 0.0,
            currentPrice = 200.0,
            marketValue = 20000.0,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 200.0,
            entryDate = today,
            previousClose = 0.0
        )
        // Both entryPrice and previousClose are 0.0 -> referencePrice must be 0.0 and today's changes 0.0
        assertEquals(0.0, pos.referencePrice, 0.001)
        assertEquals(0.0, pos.todayPriceChange, 0.001)
        assertEquals(0.0, pos.todayPriceChangePct, 0.001)
        assertEquals(0.0, pos.todayValueChange, 0.001)

        // Also test through applyCachedJsonState to ensure it does not drift to currentPrice (200.0)
        val jsonPayload = """
            {
                "summary": { "strategyId": "S", "totalNav": 100000.0, "todayPnl": 0.0 },
                "positions": [
                    {
                        "positionId": "POS_BOTH_ZERO",
                        "symbol": "NO_REF",
                        "quantity": 100,
                        "entryPrice": 0.0,
                        "currentPrice": 200.0,
                        "entryDate": "$today",
                        "previousClose": 0.0
                    }
                ]
            }
        """.trimIndent()
        val repo = VaralakshmiRepository()
        repo.applyCachedJsonState(jsonPayload)
        val parsed = repo.getCachedPositions().first { it.symbol == "NO_REF" }
        assertEquals(0.0, parsed.referencePrice, 0.001)
        assertEquals(0.0, parsed.todayPriceChange, 0.001)
        assertEquals(0.0, parsed.todayValueChange, 0.001)
        assertEquals(0.0, repo.getCachedSummary().todayPnl, 0.001)
    }

    @Test
    fun testIsPositionBoughtTodayHandlesIsoUtcTimestampNearMidnight() {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 5)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        // Format this morning's timestamp in UTC
        val utcFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        utcFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val utcTimestamp = utcFormat.format(cal.time)

        // Even if the UTC date might differ near midnight depending on timezone offset,
        // isPositionBoughtToday must correctly evaluate against local calendar date
        assertTrue(isPositionBoughtToday(utcTimestamp))
    }

    @Test
    fun testApplyCachedJsonStateRecalculatesPriorDayPositionsIgnoringStaleValueChange() {
        val repository = VaralakshmiRepository()
        val jsonPayload = """
            {
                "summary": {
                    "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                    "totalNav": 100000.00,
                    "allocatedCapital": 100000.00,
                    "deployedCapital": 80000.00,
                    "availableCapital": 20000.00,
                    "realizedPnl": 0.00,
                    "unrealizedPnl": 500.00,
                    "totalPnl": 500.00,
                    "totalPnlPct": 0.50,
                    "todayPnl": 9999.00,
                    "todayPnlPct": 9.99
                },
                "positions": [
                    {
                        "positionId": "POS_PRIOR_STALE",
                        "symbol": "HDFC",
                        "quantity": 10,
                        "entryPrice": 1500.00,
                        "currentPrice": 1550.00,
                        "entryDate": "2026-09-10",
                        "previousClose": 1500.00,
                        "todayPriceChange": 50.00,
                        "todayValueChange": 9999.00
                    }
                ],
                "transactions": []
            }
        """.trimIndent()

        val applied = repository.applyCachedJsonState(jsonPayload)
        assertTrue(applied)

        val positions = repository.getCachedPositions()
        val summary = repository.getCachedSummary()
        val hdfc = positions.first { it.symbol == "HDFC" }

        // Expected todayValueChange = 10 * 50.00 = 500.00, NOT stale 9999.00
        assertEquals(500.00, hdfc.todayValueChange, 0.001)
        assertEquals(500.00, summary.todayPnl, 0.001)
        assertNotEquals(9999.00, summary.todayPnl, 0.001)
    }

    @Test
    fun testLiveSyncRecalculatesPriorDayPositionsIgnoringStaleValueChange() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """
                    {
                        "status": "OK",
                        "active": [
                            {
                                "position_id": "POS_PRIOR_STALE_LIVE",
                                "symbol": "SBIN",
                                "quantity": 100,
                                "entry_price": 750.00,
                                "current_price": 760.00,
                                "entry_date": "2026-09-15 10:00:00",
                                "previous_close": 755.00,
                                "today_price_change": 5.00,
                                "today_value_change": 8888.00
                            }
                        ]
                    }
                """.trimIndent()
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.createContext("/api/live-trading/transactions") { exchange ->
                val response = """{"status":"OK","transactions":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.syncWithServer("http://localhost:${server.address.port}")

            assertTrue(result is SyncResult.Success)
            val sbin = result.positions.first { it.symbol == "SBIN" }
            // 100 * 5.00 = 500.00, NOT 8888.00
            assertEquals(5.00, sbin.todayPriceChange, 0.001)
            assertEquals(500.00, sbin.todayValueChange, 0.001)
            assertEquals(500.00, result.summary.todayPnl, 0.001)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testIsPositionBoughtTodayTimezoneAwareUtcEveningInUsTimezoneDoesNotFalselyMatchToday() {
        val origTz = java.util.TimeZone.getDefault()
        try {
            // Set timezone to America/New_York (UTC-4 in summer / EDT)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))

            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/New_York"))
            // Set to yesterday 21:00 EDT
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 21)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)

            // In UTC, 21:00 EDT yesterday is 01:00 UTC TODAY (starts with today's UTC date)
            val utcFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            utcFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val utcTimestamp = utcFormat.format(cal.time)

            // Because the trade occurred yesterday at 21:00 EDT in local time,
            // isPositionBoughtToday MUST return false despite the raw UTC string starting with today's date
            assertFalse(isPositionBoughtToday(utcTimestamp))
        } finally {
            java.util.TimeZone.setDefault(origTz)
        }
    }

    @Test
    fun testIsPositionBoughtTodaySupportsIndianAndAlternativeDateFormats() {
        val now = java.util.Date()
        val indianDash = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US).format(now)
        val indianSlash = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(now)
        val indianDashWithTime = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(now)
        val indianSlashWithTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.US).format(now)
        val yyyySlash = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(now)

        assertTrue(isPositionBoughtToday(indianDash))
        assertTrue(isPositionBoughtToday(indianSlash))
        assertTrue(isPositionBoughtToday(indianDashWithTime))
        assertTrue(isPositionBoughtToday(indianSlashWithTime))
        assertTrue(isPositionBoughtToday(yyyySlash))

        // Prior date in Indian format must return false
        assertFalse(isPositionBoughtToday("01-01-2020"))
        assertFalse(isPositionBoughtToday("01/01/2020"))
    }

    @Test
    fun testApplyCachedJsonStateWithNegativePrevNavDoesNotFlipPercentageSign() {
        val repository = VaralakshmiRepository()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val jsonPayload = """
            {
                "summary": {
                    "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                    "totalNav": 1000.00,
                    "allocatedCapital": 100000.00,
                    "todayPnl": 0.0,
                    "todayPnlPct": 0.0
                },
                "positions": [
                    {
                        "positionId": "POS_HIGH_PNL",
                        "symbol": "SUPER_GAINER",
                        "quantity": 100,
                        "entryPrice": 10.00,
                        "currentPrice": 25.00,
                        "entryDate": "$today",
                        "previousClose": 10.00,
                        "todayPriceChange": 15.00,
                        "todayValueChange": 1500.00
                    }
                ]
            }
        """.trimIndent()

        val applied = repository.applyCachedJsonState(jsonPayload)
        assertTrue(applied)

        val summary = repository.getCachedSummary()
        assertEquals(1500.00, summary.todayPnl, 0.001)
        // totalNav (1000) - todayPnl (1500) = prevNav (-500) <= 0.
        // It should fall back to allocatedCapital (100,000.00), yielding positive +1.50%, NOT -300.0%
        assertTrue(summary.todayPnlPct >= 0.0)
        assertEquals(1.50, summary.todayPnlPct, 0.01)
    }

    @Test
    fun testIsPositionBoughtTodaySupportsMicrosecondsAndRfc822Timezones() {
        val now = java.util.Date()
        val isoBase = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now)

        // 1. Python microsecond timestamps (.123456Z)
        val utcMicroFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        utcMicroFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val utcMicro = utcMicroFormat.format(now) + ".987654Z"
        // Evaluated against local calendar date
        val expectedUtcMatch = run {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val pDate = parser.parse(utcMicroFormat.format(now) + "Z")
            val localDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(pDate!!)
            localDateStr == isoBase
        }
        assertEquals(expectedUtcMatch, isPositionBoughtToday(utcMicro))

        // 2. RFC 822 timezone format without colon (+0530)
        val rfc822 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.US).format(now)
        assertTrue(isPositionBoughtToday(rfc822))

        // 3. ISO timestamps without seconds (HH:mm)
        val noSeconds = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US).format(now)
        assertTrue(isPositionBoughtToday(noSeconds))

        val noSecondsSpace = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(now)
        assertTrue(isPositionBoughtToday(noSecondsSpace))

        // 4. Invalid timezone string must return false and not falsely prefix match
        assertFalse(isPositionBoughtToday("invalid-timestamp+05:30"))
        assertFalse(isPositionBoughtToday("2026-09-25-malformed-time+05:30"))
    }

    @Test
    fun testPositionItemImmutableReferencePricePreservesConsistencyAcrossMidnight() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val pos = PositionItem(
            positionId = "POS_IMMUTABLE_TEST",
            symbol = "CONSISTENT_CORP",
            quantity = 10,
            entryPrice = 1183.36,
            currentPrice = 1182.55,
            marketValue = 11825.50,
            unrealizedPnl = -8.10,
            unrealizedPnlPct = -0.07,
            peakPrice = 1183.36,
            entryDate = today,
            previousClose = 1203.85
        )

        // Stored reference price matches entry execution price and does not depend on dynamic clock re-evaluation
        assertEquals(1183.36, pos.referencePrice, 0.001)
        assertTrue(pos.isBoughtToday)
        assertEquals(-0.81, pos.todayPriceChange, 0.001)
        assertEquals(-8.10, pos.todayValueChange, 0.001)

        // Mathematical invariant: todayPriceChange strictly equals roundPaise(currentPrice - referencePrice)
        val recomputedPriceChange = VaralakshmiRepository.roundPaise(pos.currentPrice - pos.referencePrice)
        assertEquals(recomputedPriceChange, pos.todayPriceChange, 0.0001)

        // Mathematical invariant: todayValueChange strictly equals roundPaise(quantity * todayPriceChange)
        val recomputedValueChange = VaralakshmiRepository.roundPaise(pos.quantity * pos.todayPriceChange)
        assertEquals(recomputedValueChange, pos.todayValueChange, 0.0001)
    }

    @Test
    fun testApplyCachedJsonStateFallbackAllocatedCapitalUsesParsedSummary() {
        val repository = VaralakshmiRepository()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val jsonPayload = """
            {
                "summary": {
                    "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                    "totalNav": 1000.00,
                    "allocatedCapital": 200000.00,
                    "todayPnl": 0.0,
                    "todayPnlPct": 0.0
                },
                "positions": [
                    {
                        "positionId": "POS_HIGH_PNL_2",
                        "symbol": "SUPER_GAINER_2",
                        "quantity": 100,
                        "entryPrice": 10.00,
                        "currentPrice": 25.00,
                        "entryDate": "$today",
                        "previousClose": 10.00,
                        "todayPriceChange": 15.00,
                        "todayValueChange": 1500.00
                    }
                ]
            }
        """.trimIndent()

        val applied = repository.applyCachedJsonState(jsonPayload)
        assertTrue(applied)

        val summary = repository.getCachedSummary()
        assertEquals(1500.00, summary.todayPnl, 0.001)
        // totalNav (1000) - todayPnl (1500) = prevNav (-500) <= 0.
        // It should fall back to parsedSummary.allocatedCapital (200,000.00), yielding positive +0.75%
        assertEquals(0.75, summary.todayPnlPct, 0.01)
    }

    @Test
    fun testReservedCapitalInclusionInNavAndJsonState() {
        val repository = VaralakshmiRepository()
        val jsonPayload = """
            {
                "summary": {
                    "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                    "totalNav": 618904.21,
                    "allocatedCapital": 600000.00,
                    "deployedCapital": 230604.50,
                    "availableCapital": 200504.37,
                    "reservedCapital": 182615.84,
                    "realizedPnl": 13724.71,
                    "unrealizedPnl": 5179.50,
                    "totalPnl": 18904.21,
                    "totalPnlPct": 3.15,
                    "todayPnl": 1200.00,
                    "todayPnlPct": 0.20,
                    "activeSlots": 2,
                    "maxSlots": 3
                },
                "positions": []
            }
        """.trimIndent()

        val applied = repository.applyCachedJsonState(jsonPayload)
        assertTrue(applied)

        val summary = repository.getCachedSummary()
        assertEquals(600000.00, summary.allocatedCapital, 0.001)
        assertEquals(230604.50, summary.deployedCapital, 0.001)
        assertEquals(200504.37, summary.availableCapital, 0.001)
        assertEquals(182615.84, summary.reservedCapital, 0.001)
        assertEquals(5179.50, summary.unrealizedPnl, 0.001)

        // Mathematical invariant: NAV = Deployed + Available + Reserved + Unrealized
        val expectedNav = VaralakshmiRepository.roundPaise(
            summary.deployedCapital + summary.availableCapital + summary.reservedCapital + summary.unrealizedPnl
        )
        assertEquals(618904.21, expectedNav, 0.001)
        assertEquals(expectedNav, summary.totalNav, 0.001)
    }

    @Test
    fun testIpoActionNotificationParsingAndDismissal() {
        val repository = VaralakshmiRepository()
        val defaultIpos = repository.getCachedIpoNotifications()
        assertTrue("Default seed IPO notifications should be empty until live data arrives", defaultIpos.isEmpty())

        // Test sample Tata Tech model
        val sampleAlert = VaralakshmiRepository.createSampleTataTechIpoNotification()
        assertEquals("TATATECH", sampleAlert.symbol)
        assertEquals(69.4, sampleAlert.qibMultiple, 0.001)
        assertEquals(15000.00, sampleAlert.lotPrice, 0.001)
        assertFalse(sampleAlert.isDismissed)

        // Dismissal test
        val dismissed = sampleAlert.copy(isDismissed = true)
        assertTrue(dismissed.isDismissed)

        // Custom JSON parsing test
        val customJson = """
            {
                "ipo_notifications": [
                    {
                        "id": "IPO-SWIGGY-001",
                        "symbol": "SWIGGY",
                        "company_name": "Swiggy Ltd",
                        "action_type": "APPLY_NOW",
                        "headline": "Action Required: Apply for Swiggy",
                        "message": "QIB Institutional Demand: 38.2x",
                        "lot_price": 14760.00,
                        "lot_quantity": 1,
                        "qib_multiple": 38.2,
                        "close_deadline": "15:30",
                        "urgency": "HIGH",
                        "timestamp": "2026-09-26 11:00:00"
                    }
                ]
            }
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseIpoNotificationsJson(customJson)
        assertEquals(1, parsed.size)
        val swiggy = parsed.first()
        assertEquals("SWIGGY", swiggy.symbol)
        assertEquals("Swiggy Ltd", swiggy.companyName)
        assertEquals(38.2, swiggy.qibMultiple, 0.001)
        assertEquals(14760.00, swiggy.lotPrice, 0.001)
        assertEquals("₹14,760.00", swiggy.formattedLotPrice)
        assertEquals(1, swiggy.lotQuantity)
        assertEquals(30, swiggy.lotShares)
        assertEquals(500.00, swiggy.cutoffPrice, 0.001)
        assertEquals("₹14,760.00", swiggy.formattedTotalAmount)
        assertEquals("38.2x", swiggy.formattedQibMultiple)
        assertFalse(swiggy.isDismissed)
    }
}

