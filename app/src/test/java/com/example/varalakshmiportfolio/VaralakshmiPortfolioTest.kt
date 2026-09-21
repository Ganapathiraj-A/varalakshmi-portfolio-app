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
}
