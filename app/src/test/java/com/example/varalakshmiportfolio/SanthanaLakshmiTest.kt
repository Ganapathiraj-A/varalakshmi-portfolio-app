package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.IpoActionType
import com.example.varalakshmiportfolio.model.PastIpoApplication
import com.example.varalakshmiportfolio.model.SanthanaLakshmiSummary
import com.example.varalakshmiportfolio.model.SanthanaLakshmiTradeItem
import com.example.varalakshmiportfolio.model.SanthanaLakshmiUiState
import com.example.varalakshmiportfolio.model.formatSanthanaLakshmiForClipboard
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SanthanaLakshmiTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cacheDir = tempFolder.newFolder("santhana_test_cache")
        VaralakshmiRepository.initialize(cacheDir)
    }

    @After
    fun tearDown() {
        VaralakshmiRepository.resetIoDispatcherForTesting()
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        VaralakshmiRepository.resetCacheDirectoryForTesting()
    }

    @Test
    fun testDefaultSanthanaSummaryValues() {
        val repo = VaralakshmiRepository()
        val state = repo.createDefaultSanthanaState()

        assertEquals("SanthanaLakshmi (Regime-Gated IPO Compounder)", state.summary.strategyName)
        assertEquals(100000.0, state.summary.allocatedCapital, 0.001)
        assertEquals(0.0, state.summary.deployedCapital, 0.001)
        assertEquals(100000.0, state.summary.sweptCashBalance, 0.001)
        assertEquals(6.50, state.summary.sweepYieldPct, 0.001)
        assertEquals(21.55, state.summary.lifetimeCagrPct, 0.001)
        assertEquals(82.14, state.summary.winRatePct, 0.001)
        assertEquals(36.18, state.summary.avgListingGainPct, 0.001)
        assertEquals(-0.69, state.summary.maxDrawdownPct, 0.001)
        assertEquals(11.69, state.summary.profitFactor, 0.001)
        assertEquals("BULLISH", state.summary.currentMarketRegime)
        assertEquals("+21.55%", state.summary.formattedLifetimeCagr)
        assertEquals("82.1%", state.summary.formattedWinRate)
    }

    @Test
    fun testDefaultTradesCountAndIntegrity() {
        val repo = VaralakshmiRepository()
        // Default trades must be empty so no fake or simulated trades appear in real accounts
        val defaultTrades = repo.createDefaultSanthanaTrades()
        assertTrue(defaultTrades.isEmpty())

        // Sample trades available for model verification
        val sampleTrades = repo.createSampleSanthanaTrades()
        assertEquals(8, sampleTrades.size)

        val swiggy = sampleTrades.first { it.symbol == "SWIGGY" }
        assertEquals(390.00, swiggy.issuePrice, 0.001)
        assertEquals(456.00, swiggy.exitPrice, 0.001)
        assertTrue(swiggy.isWin)
        assertTrue(swiggy.isSample)
        assertEquals(2139.38, swiggy.netPnl, 0.001)
        assertEquals("+16.92%", swiggy.formattedNetGainPct)

        val western = sampleTrades.first { it.symbol == "WESTERN" }
        assertTrue(western.isRegimeDefense)
        assertFalse(western.isWin)
        assertTrue(western.isSample)
        assertEquals(-23.90, western.netPnl, 0.001)
        assertEquals("REGIME_DEFENSE_OPEN", western.exitReason)
    }

    @Test
    fun testFilteredTradesLogic() {
        val realTrades = listOf(
            SanthanaLakshmiTradeItem(
                tradeId = "REAL-1",
                symbol = "REALWIN",
                companyName = "Real Win Ltd",
                decisionDate = "2026-10-01",
                listingDate = "2026-10-05",
                issuePrice = 100.0,
                exitPrice = 150.0,
                netGainPct = 50.0,
                netPnl = 5000.0,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 10.0,
                isRealTrade = true
            ),
            SanthanaLakshmiTradeItem(
                tradeId = "REAL-2",
                symbol = "REALDEF",
                companyName = "Real Def Ltd",
                decisionDate = "2026-10-01",
                listingDate = "2026-10-05",
                issuePrice = 100.0,
                exitPrice = 90.0,
                netGainPct = -10.0,
                netPnl = -1000.0,
                regime = "BEARISH",
                exitReason = "REGIME_DEFENSE",
                qibMultiple = 1.0,
                isRealTrade = true
            )
        )
        val state = SanthanaLakshmiUiState(historicalTrades = realTrades)

        val allTrades = state.filteredTrades
        assertEquals(2, allTrades.size)

        val winsState = state.copy(selectedFilter = "WINS")
        val winTrades = winsState.filteredTrades
        assertEquals(1, winTrades.size)
        assertEquals("REALWIN", winTrades.first().symbol)

        val defenseState = state.copy(selectedFilter = "DEFENSE")
        val defenseTrades = defenseState.filteredTrades
        assertEquals(1, defenseTrades.size)
        assertEquals("REALDEF", defenseTrades.first().symbol)
    }

    @Test
    fun testDiskCacheSerialization() {
        val repo = VaralakshmiRepository()
        val realTrades = listOf(
            SanthanaLakshmiTradeItem(
                tradeId = "REAL-TX-101",
                symbol = "CUSTOMIPO",
                companyName = "Custom IPO Ltd",
                decisionDate = "2026-10-01",
                listingDate = "2026-10-05",
                issuePrice = 120.0,
                exitPrice = 180.0,
                netGainPct = 50.0,
                netPnl = 6000.0,
                regime = "BULLISH",
                exitReason = "PROFIT_TARGET",
                qibMultiple = 15.0,
                isRealTrade = true
            )
        )
        val originalState = repo.createDefaultSanthanaState().copy(historicalTrades = realTrades)

        repo.saveSanthanaToDisk(originalState)

        val loadedState = repo.loadSanthanaFromDisk()
        assertNotNull(loadedState)
        assertEquals(originalState.summary.strategyName, loadedState!!.summary.strategyName)
        assertEquals(originalState.summary.allocatedCapital, loadedState.summary.allocatedCapital, 0.001)
        assertEquals(originalState.summary.lifetimeCagrPct, loadedState.summary.lifetimeCagrPct, 0.001)
        assertEquals(1, loadedState.historicalTrades.size)
        assertEquals("CUSTOMIPO", loadedState.historicalTrades.first().symbol)
        assertEquals(4, loadedState.activeApplications.size)
    }

    @Test
    fun testActiveApplicationsIntegrityAndMandateStatus() {
        val repo = VaralakshmiRepository()
        val state = repo.createDefaultSanthanaState()

        assertEquals(4, state.activeApplications.size)

        val rkfal = state.activeApplications.first { it.symbol == "RKFAL" }
        assertEquals("APPROVED", rkfal.mandateStatus)
        assertTrue(rkfal.isMandateApproved)
        assertEquals(1600, rkfal.lotSize)
        assertEquals(82.0, rkfal.cutOffPrice, 0.001)
        assertEquals(131200.0, rkfal.totalAmount, 0.001)
        assertTrue(rkfal.autoSellEnabled)

        val acme = state.activeApplications.first { it.symbol == "ACMERAIL" }
        assertEquals("APPROVED", acme.mandateStatus)
        assertTrue(acme.isMandateApproved)
        assertEquals(600, acme.lotSize)
        assertEquals(186.0, acme.cutOffPrice, 0.001)
        assertEquals(111600.0, acme.totalAmount, 0.001)
        assertEquals("2026-10-06", acme.closingDate)

        val par = state.activeApplications.first { it.symbol == "PARASYNTEX" }
        assertEquals("APPROVED", par.mandateStatus)
        assertTrue(par.isMandateApproved)
        assertEquals(1000, par.lotSize)
        assertEquals(127.0, par.cutOffPrice, 0.001)

        val tna = state.activeApplications.first { it.symbol == "TNA" }
        assertEquals("APPROVED", tna.mandateStatus)
        assertTrue(tna.isMandateApproved)
        assertEquals(2000, tna.lotSize)
        assertEquals(70.0, tna.cutOffPrice, 0.001)
    }

    @Test
    fun testUpdateIpoMandateStatus() {
        val repo = VaralakshmiRepository()
        val updated = repo.updateIpoMandateStatus("TNA", "APPROVED")
        val tna = updated.activeApplications.first { it.symbol == "TNA" }
        assertEquals("APPROVED", tna.mandateStatus)
        assertTrue(tna.isMandateApproved)
    }

    @Test
    fun testDismissIpoAlertPersistence() {
        val repo = VaralakshmiRepository()
        val notif = IpoActionNotification(
            id = "IPO-TEST-DISMISS",
            symbol = "TESTIPO",
            companyName = "Test IPO Ltd",
            actionType = IpoActionType.APPLY_NOW,
            headline = "Apply for Test IPO",
            message = "Test notification message",
            closeDeadline = "Today 16:30"
        )
        repo.setIpoNotificationsForTesting(listOf(notif))
        assertFalse(repo.getCachedIpoNotifications().first { it.id == "IPO-TEST-DISMISS" }.isDismissed)

        repo.dismissIpoNotification("IPO-TEST-DISMISS")
        assertTrue(repo.getCachedIpoNotifications().first { it.id == "IPO-TEST-DISMISS" }.isDismissed)
    }

    @Test
    fun testClipboardFormatting() {
        val summary = SanthanaLakshmiSummary()
        val repo = VaralakshmiRepository()

        // When no real trades exist
        val emptyText = formatSanthanaLakshmiForClipboard(summary, emptyList(), emptyList(), emptyList())
        assertTrue(emptyText.contains("SANTHANALAKSHMI - REGIME-GATED IPO COMPOUNDER"))
        assertTrue(emptyText.contains("No completed real IPO trades yet"))

        // When active applications exist
        val apps = repo.createDefaultActiveApplications()
        val appText = formatSanthanaLakshmiForClipboard(summary, emptyList(), emptyList(), apps)
        assertTrue(appText.contains("ACTIVE IPO APPLICATIONS & ALLOTMENT PIPELINE (4)"))
        assertTrue(appText.contains("ACMERAIL"))
        assertTrue(appText.contains("RKFAL"))
        assertTrue(appText.contains("APPROVED"))

        // When real trades exist
        val realTrades = listOf(
            SanthanaLakshmiTradeItem(
                tradeId = "REAL-1",
                symbol = "REALCORP",
                companyName = "Real Corp Ltd",
                decisionDate = "2026-10-01",
                listingDate = "2026-10-05",
                issuePrice = 200.0,
                exitPrice = 260.0,
                netGainPct = 30.0,
                netPnl = 6000.0,
                regime = "BULLISH",
                exitReason = "CLOSE",
                qibMultiple = 12.0,
                isRealTrade = true
            )
        )
        val filledText = formatSanthanaLakshmiForClipboard(summary, realTrades, emptyList(), apps)
        assertTrue(filledText.contains("REALCORP"))
    }

    @Test
    fun testViewModelStateInitialization() = runTest {
        val repo = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(
            repository = repo,
            autoRefresh = false,
            enablePeriodicSync = false
        )

        val initialState = viewModel.santhanaLakshmiState.value
        assertNotNull(initialState)
        assertEquals(100000.0, initialState.summary.allocatedCapital, 0.001)
        assertEquals(0, initialState.historicalTrades.size)

        viewModel.selectSanthanaFilter("WINS")
        assertEquals("WINS", viewModel.santhanaLakshmiState.value.selectedFilter)

        val sampleTrade = repo.createSampleSanthanaTrades().first()
        viewModel.selectSanthanaTradeDetail(sampleTrade)
        assertEquals("SWIGGY", viewModel.santhanaLakshmiState.value.selectedTradeDetail?.symbol)

        viewModel.selectSanthanaTradeDetail(null)
        assertEquals(null, viewModel.santhanaLakshmiState.value.selectedTradeDetail)
        viewModel.stopPeriodicSync()
    }

    @Test
    fun testRefreshSanthanaWithMockServer() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """{"status":"OK","ipo_notifications":[]}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshSanthana("http://localhost:${server.address.port}", "dummy-token")
            assertNotNull(result)
            assertFalse(result.isLoading)
            assertEquals(100000.0, result.summary.allocatedCapital, 0.001)
            assertEquals(0, result.historicalTrades.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testIsDeadlineTodayOrFutureExcludesPastDates() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val fixedNow = sdf.parse("2026-10-06")!!

        // Past deadlines from user screenshot (September & early October 2026)
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("23-09-2026", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("1-10-2026", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("01-10-2026", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("20-09-2026", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("2026-09-23", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("2026-10-01", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("yesterday", fixedNow))
        assertFalse(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("Deadline: 23-09-2026", fixedNow))

        // Today deadlines
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("06-10-2026", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("6-10-2026", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("2026-10-06", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("today", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("15:30", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("Deadline: 06-10-2026", fixedNow))

        // Future deadlines
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("07-10-2026", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("15-10-2026", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("2026-10-15", fixedNow))
        assertTrue(com.example.varalakshmiportfolio.model.isDeadlineTodayOrFuture("tomorrow", fixedNow))
    }

    @Test
    fun testRefreshSanthanaFiltersOutPastAlerts() = runTest {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(0), 0)
        try {
            server.createContext("/api/live-trading/positions") { exchange ->
                val response = """{
                    "status":"OK",
                    "ipo_notifications":[
                        {
                            "id":"IPO-ROBOKIDZEDUV",
                            "symbol":"ROBOKIDZEDUV",
                            "action_type":"APPLY_NOW",
                            "deadline":"23-09-2026",
                            "qib":306.7
                        },
                        {
                            "id":"IPO-VANSELECTROE",
                            "symbol":"VANSELECTROE",
                            "action_type":"APPLY_NOW",
                            "deadline":"1-10-2026",
                            "qib":281.6
                        },
                        {
                            "id":"IPO-FUTURE-CORP",
                            "symbol":"FUTURECORP",
                            "action_type":"APPLY_NOW",
                            "deadline":"28-10-2026",
                            "qib":55.0
                        }
                    ]
                }"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }
            server.start()

            val repo = VaralakshmiRepository()
            val result = repo.refreshSanthana("http://localhost:${server.address.port}", "dummy-token")
            assertNotNull(result)
            // Past alerts (23-09-2026 and 1-10-2026) must be excluded
            assertEquals(1, result.activeAlerts.size)
            assertEquals("FUTURECORP", result.activeAlerts.first().symbol)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun testAppliedIposExcludedFromActionSignals() {
        val repo = VaralakshmiRepository()
        val defaultState = repo.createDefaultSanthanaState()

        val alerts = listOf(
            com.example.varalakshmiportfolio.model.IpoActionNotification(
                id = "ALERT-RKFAL",
                symbol = "RKFAL",
                companyName = "R.K. Fashion Accessories SME",
                actionType = com.example.varalakshmiportfolio.model.IpoActionType.APPLY_NOW,
                headline = "Apply for RKFAL",
                message = "High subscription",
                closeDeadline = "07-10-2026"
            ),
            com.example.varalakshmiportfolio.model.IpoActionNotification(
                id = "ALERT-ACMERAIL",
                symbol = "ACMERAIL",
                companyName = "Acme India Industries SME",
                actionType = com.example.varalakshmiportfolio.model.IpoActionType.APPLY_NOW,
                headline = "Apply for ACMERAIL",
                message = "Closes today",
                closeDeadline = "06-10-2026"
            ),
            com.example.varalakshmiportfolio.model.IpoActionNotification(
                id = "ALERT-NEWIPO",
                symbol = "NEWIPO",
                companyName = "New IPO Corp",
                actionType = com.example.varalakshmiportfolio.model.IpoActionType.APPLY_NOW,
                headline = "Apply for NEWIPO",
                message = "New candidate",
                closeDeadline = "31-12-2026"
            )
        )

        val stateWithAlerts = defaultState.copy(activeAlerts = alerts)
        val visibleSignals = stateWithAlerts.activeTodayOrFutureAlerts

        // RKFAL and ACMERAIL are in activeApplications, so they must be excluded
        assertEquals(1, visibleSignals.size)
        assertEquals("NEWIPO", visibleSignals.first().symbol)
    }

    @Test
    fun testPastApplicationsDefaultAndMetrics() {
        val repo = VaralakshmiRepository()
        val state = repo.createDefaultSanthanaState()

        // Default past applications must be empty so no dummy data appears in real accounts
        assertTrue(state.pastApplications.isEmpty())
        assertTrue(state.realPastApplications.isEmpty())
        assertEquals(0, state.pastAllottedCount)
        assertEquals(0, state.pastNotAllottedCount)
        assertEquals(0.0, state.pastTotalUnblockedCapital, 0.001)
        assertEquals(0.0, state.pastTotalRealizedListingProfit, 0.001)

        // Verify Sample Past Applications for model validation
        val samplePast = repo.createSamplePastApplications()
        assertEquals(6, samplePast.size)

        // Verify Allotted trade details
        val swiggy = samplePast.first { it.symbol == "SWIGGY" }
        assertTrue(swiggy.isAllotted)
        assertTrue(swiggy.isSample)
        assertEquals("DEBITED", swiggy.refundStatus)
        assertEquals(2139.38, swiggy.realizedPnl!!, 0.01)

        // Verify Rejected / Not Allotted details
        val ntpc = samplePast.first { it.symbol == "NTPCGREEN" }
        assertFalse(ntpc.isAllotted)
        assertTrue(ntpc.isSample)
        assertEquals("UNBLOCKED_TO_BANK", ntpc.refundStatus)
        assertEquals(0.00, ntpc.realizedPnl!!, 0.001)
        assertEquals(14904.00, ntpc.totalAmount, 0.001)
    }

    @Test
    fun testUpdateIpoAllotmentStatusActiveApplications() {
        val repo = VaralakshmiRepository()
        val defaultState = repo.createDefaultSanthanaState()

        val acmeInitial = defaultState.activeApplications.first { it.symbol == "ACMERAIL" }
        assertEquals("APPLIED", acmeInitial.status)
        assertFalse(acmeInitial.isAllotted)
        assertFalse(acmeInitial.isNotAllotted)

        // Mark ALLOTTED (Approved)
        val stateAllotted = repo.updateIpoAllotmentStatus("ACMERAIL", "ALLOTTED")
        val acmeAllotted = stateAllotted.activeApplications.first { it.symbol == "ACMERAIL" }
        assertEquals("ALLOTTED", acmeAllotted.status)
        assertTrue(acmeAllotted.isAllotted)
        assertFalse(acmeAllotted.isNotAllotted)

        // Mark NOT_ALLOTTED (Rejected)
        val stateRejected = repo.updateIpoAllotmentStatus("ACMERAIL", "NOT_ALLOTTED")
        val acmeRejected = stateRejected.activeApplications.first { it.symbol == "ACMERAIL" }
        assertEquals("NOT_ALLOTTED", acmeRejected.status)
        assertFalse(acmeRejected.isAllotted)
        assertTrue(acmeRejected.isNotAllotted)
    }

    @Test
    fun testDiskPersistencePastApplications() {
        val repo = VaralakshmiRepository()
        val realPast = listOf(
            PastIpoApplication(
                id = "REAL-PAST-001",
                symbol = "REALCO",
                companyName = "Real Co Ltd",
                issueType = "MAINBOARD",
                appliedDate = "2026-10-01",
                allotmentDate = "2026-10-03",
                listingDate = "2026-10-05",
                lotSize = 100,
                cutOffPrice = 150.0,
                totalAmount = 15000.0,
                allotmentStatus = "ALLOTTED",
                refundStatus = "DEBITED",
                listingPrice = 200.0,
                listingGainPct = 33.33,
                realizedPnl = 5000.0,
                outcomeReason = "1 Lot allotted",
                registrarName = "Link Intime India",
                isRealApplication = true
            )
        )
        val initial = repo.createDefaultSanthanaState().copy(pastApplications = realPast)
        repo.saveSanthanaToDisk(initial)

        val reloaded = repo.loadSanthanaFromDisk()
        assertNotNull(reloaded)
        assertEquals(1, reloaded!!.pastApplications.size)
        assertEquals(1, reloaded.pastAllottedCount)
        assertEquals(0, reloaded.pastNotAllottedCount)
        assertEquals("REALCO", reloaded.pastApplications.first().symbol)
    }

    @Test
    fun testDummyPastApplicationsFilteredFromOldCache() {
        val repo = VaralakshmiRepository()
        // Simulate old disk cache containing dummy items
        val oldDummyState = repo.createDefaultSanthanaState().copy(
            pastApplications = repo.createSamplePastApplications()
        )
        repo.saveSanthanaToDisk(oldDummyState)

        // Load must automatically sanitize and strip out all sample dummy past applications
        val loaded = repo.loadSanthanaFromDisk()
        assertNotNull(loaded)
        assertTrue(loaded!!.pastApplications.isEmpty())
        assertTrue(loaded.realPastApplications.isEmpty())
        assertEquals(0, loaded.pastAllottedCount)
        assertEquals(0, loaded.pastNotAllottedCount)
    }

    @Test
    fun testPastApplicationsFormatting() {
        val repo = VaralakshmiRepository()
        val samplePast = repo.createSamplePastApplications()
        val sampleState = SanthanaLakshmiUiState(pastApplications = samplePast)
        assertEquals(6, sampleState.pastApplications.size)
        // Since all 6 in sample are marked isSample, realPastApplications is 0
        assertEquals(0, sampleState.realPastApplications.size)

        for (pastApp in samplePast) {
            val pnlStr = pastApp.formattedRealizedPnl
            assertNotNull(pnlStr)
            assertTrue(pnlStr.startsWith("+₹") || pnlStr.startsWith("-₹") || pnlStr == "₹0.00")
            val gainStr = pastApp.formattedListingGainPct
            assertNotNull(gainStr)
            assertNotNull(pastApp.formattedCutOffPrice)
            assertNotNull(pastApp.formattedTotalAmount)
        }

        val swiggy = samplePast.first { it.symbol == "SWIGGY" }
        assertTrue(swiggy.isAllotted)
        assertEquals("+₹2,139.38", swiggy.formattedRealizedPnl)
        assertEquals("+16.92%", swiggy.formattedListingGainPct)

        val ntpc = samplePast.first { it.symbol == "NTPCGREEN" }
        assertFalse(ntpc.isAllotted)
        assertEquals("₹0.00", ntpc.formattedRealizedPnl)
    }
}

