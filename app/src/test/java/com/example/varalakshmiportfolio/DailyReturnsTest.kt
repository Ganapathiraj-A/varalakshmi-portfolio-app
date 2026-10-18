package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.SyncResult
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.DailyReturnItem
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.formatVaralakshmiAllStatementsForClipboard
import com.example.varalakshmiportfolio.model.formatDailyReturnStatementForClipboard
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReturnsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        VaralakshmiRepository.resetIoDispatcherForTesting()
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Test
    fun testPositiveReturnItemFormattingAndStatus() {
        val item = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = 4007.77,
            returnPct = 3.81,
            totalNav = 109268.80,
            tradeCount = 1
        )

        assertEquals("2026-09-21", item.date)
        assertEquals(4007.77, item.dailyPnl, 0.001)
        assertEquals(3.81, item.returnPct, 0.001)
        assertEquals(109268.80, item.totalNav, 0.001)
        assertEquals(1, item.tradeCount)
        assertEquals("WIN", item.status)
        assertTrue("Positive daily return should have isProfit = true", item.isProfit)
        assertEquals("+₹4,007.77", item.formattedPnl)
        assertEquals("+3.81%", item.formattedReturnPct)
    }

    @Test
    fun testNegativeReturnItemFormattingAndStatus() {
        val item = DailyReturnItem(
            date = "2026-09-16",
            dailyPnl = -450.50,
            returnPct = -0.45,
            totalNav = 100000.00,
            tradeCount = 2
        )

        assertEquals("2026-09-16", item.date)
        assertEquals(-450.50, item.dailyPnl, 0.001)
        assertEquals(-0.45, item.returnPct, 0.001)
        assertEquals(100000.00, item.totalNav, 0.001)
        assertEquals(2, item.tradeCount)
        assertEquals("LOSS", item.status)
        assertFalse("Negative daily return should have isProfit = false", item.isProfit)
        assertEquals("-₹450.50", item.formattedPnl)
        assertEquals("-0.45%", item.formattedReturnPct)
    }

    @Test
    fun testZeroReturnItemFormattingAndStatus() {
        val item = DailyReturnItem(
            date = "2026-09-11",
            dailyPnl = 0.00,
            returnPct = 0.00,
            totalNav = 99990.50,
            tradeCount = 0
        )

        assertEquals("2026-09-11", item.date)
        assertEquals(0.00, item.dailyPnl, 0.001)
        assertEquals(0.00, item.returnPct, 0.001)
        assertEquals(99990.50, item.totalNav, 0.001)
        assertEquals(0, item.tradeCount)
        assertEquals("FLAT", item.status)
        assertFalse("Zero daily return should not be profit", item.isProfit)
        assertEquals("₹0.00", item.formattedPnl)
        assertEquals("0.00%", item.formattedReturnPct)
    }

    @Test
    fun testExplicitStatusAttribution() {
        val customWin = DailyReturnItem(
            date = "2026-09-20",
            dailyPnl = 50.0,
            returnPct = 0.05,
            totalNav = 101000.0,
            status = "WIN"
        )
        assertEquals("WIN", customWin.status)

        val customLoss = DailyReturnItem(
            date = "2026-09-19",
            dailyPnl = -20.0,
            returnPct = -0.02,
            totalNav = 100950.0,
            status = "LOSS"
        )
        assertEquals("LOSS", customLoss.status)

        val customFlat = DailyReturnItem(
            date = "2026-09-18",
            dailyPnl = 0.0,
            returnPct = 0.0,
            totalNav = 100970.0,
            status = "FLAT"
        )
        assertEquals("FLAT", customFlat.status)
    }

    @Test
    fun testJsonParsingSnakeCaseArray() {
        val json = """
            [
                {
                    "date": "2026-09-21",
                    "daily_pnl": 4007.77,
                    "return_pct": 3.81,
                    "total_nav": 109268.80,
                    "trade_count": 1
                },
                {
                    "date": "2026-09-16",
                    "daily_pnl": -450.50,
                    "return_pct": -0.45,
                    "total_nav": 100000.00,
                    "trade_count": 1,
                    "status": "LOSS"
                },
                {
                    "date": "2026-09-11",
                    "daily_pnl": 0.00,
                    "return_pct": 0.00,
                    "total_nav": 99990.50,
                    "trade_count": 0
                }
            ]
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(3, parsed.size)

        val win = parsed[0]
        assertEquals("2026-09-21", win.date)
        assertEquals(4007.77, win.dailyPnl, 0.001)
        assertEquals(3.81, win.returnPct, 0.001)
        assertEquals(109268.80, win.totalNav, 0.001)
        assertEquals(1, win.tradeCount)
        assertEquals("WIN", win.status)
        assertEquals("+₹4,007.77", win.formattedPnl)
        assertEquals("+3.81%", win.formattedReturnPct)

        val loss = parsed[1]
        assertEquals("2026-09-16", loss.date)
        assertEquals(-450.50, loss.dailyPnl, 0.001)
        assertEquals(-0.45, loss.returnPct, 0.001)
        assertEquals(100000.00, loss.totalNav, 0.001)
        assertEquals(1, loss.tradeCount)
        assertEquals("LOSS", loss.status)
        assertEquals("-₹450.50", loss.formattedPnl)
        assertEquals("-0.45%", loss.formattedReturnPct)

        val flat = parsed[2]
        assertEquals("2026-09-11", flat.date)
        assertEquals(0.00, flat.dailyPnl, 0.001)
        assertEquals(0.00, flat.returnPct, 0.001)
        assertEquals(99990.50, flat.totalNav, 0.001)
        assertEquals(0, flat.tradeCount)
        assertEquals("FLAT", flat.status)
        assertEquals("₹0.00", flat.formattedPnl)
        assertEquals("0.00%", flat.formattedReturnPct)
    }

    @Test
    fun testJsonParsingCamelCaseRootObject() {
        val json = """
            {
                "daily_returns": [
                    {
                        "date": "2026-09-21",
                        "dailyPnl": 1250.00,
                        "returnPct": 1.25,
                        "totalNav": 101250.00,
                        "tradeCount": 3,
                        "status": "WIN"
                    }
                ]
            }
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(1, parsed.size)
        assertEquals("2026-09-21", parsed[0].date)
        assertEquals(1250.00, parsed[0].dailyPnl, 0.001)
        assertEquals(1.25, parsed[0].returnPct, 0.001)
        assertEquals(101250.00, parsed[0].totalNav, 0.001)
        assertEquals(3, parsed[0].tradeCount)
        assertEquals("WIN", parsed[0].status)
    }

    @Test
    fun testJsonParsingMalformedJsonReturnsEmptyList() {
        val parsed = VaralakshmiRepository.parseDailyReturnsJson("{ invalid json ")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun testRepositoryProvidesOfflineFallbackDailyReturns() {
        val repository = VaralakshmiRepository()
        val dailyReturns = repository.getCachedDailyReturns()

        assertTrue("Fallback daily returns must not be empty", dailyReturns.isNotEmpty())
        assertEquals(12, dailyReturns.size)

        // Latest session: 2026-10-01 with 3 executed trades
        val latestSession = dailyReturns.first()
        assertEquals("2026-10-01", latestSession.date)
        assertEquals(-18310.80, latestSession.dailyPnl, 0.01)
        assertEquals(-2.98, latestSession.returnPct, 0.01)
        assertEquals(598256.00, latestSession.totalNav, 0.01)
        assertEquals(3, latestSession.tradeCount)
        assertEquals(3, latestSession.trades.size)
        assertEquals("LOSS", latestSession.status)
        assertFalse(latestSession.isProfit)

        // Session for 2026-09-21 matches summary baseline
        val sep21Session = dailyReturns.first { it.date == "2026-09-21" }
        assertEquals("2026-09-21", sep21Session.date)
        assertEquals(4007.77, sep21Session.dailyPnl, 0.001)
        assertEquals(3.81, sep21Session.returnPct, 0.001)
        assertEquals(109268.80, sep21Session.totalNav, 0.001)
        assertEquals("WIN", sep21Session.status)
        assertTrue(sep21Session.isProfit)
        assertEquals(1, sep21Session.trades.size)

        // Verify presence of WIN, LOSS, and FLAT sessions
        assertTrue("Must contain at least one WIN session", dailyReturns.any { it.status == "WIN" })
        assertTrue("Must contain at least one LOSS session", dailyReturns.any { it.status == "LOSS" })
        assertTrue("Must contain at least one FLAT session", dailyReturns.any { it.status == "FLAT" })

        // Check summary match against 2026-09-21 baseline
        val summary = repository.getCachedSummary()
        assertEquals(summary.todayPnl, sep21Session.dailyPnl, 0.001)
        assertEquals(summary.todayPnlPct, sep21Session.returnPct, 0.01)
        assertEquals(summary.totalNav, sep21Session.totalNav, 0.001)
    }

    @Test
    fun testVaralakshmiUiStateInclusionUponRepositoryLoad() {
        val repository = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        try {
            val uiState = viewModel.uiState.value

            assertNotNull("UiState dailyReturns must not be null", uiState.dailyReturns)
            assertTrue("UiState dailyReturns must be populated from repository", uiState.dailyReturns.isNotEmpty())
            assertEquals(repository.getCachedDailyReturns().size, uiState.dailyReturns.size)
            assertEquals(repository.getCachedDailyReturns(), uiState.dailyReturns)
        } finally {
            viewModel.stopPeriodicSync()
        }
    }

    @Test
    fun testRepositoryResetRestoresDefaultDailyReturns() {
        val repository = VaralakshmiRepository()
        val original = repository.getCachedDailyReturns()

        repository.resetToDefaultSeed()
        val resetReturns = repository.getCachedDailyReturns()

        assertEquals(original.size, resetReturns.size)
        assertEquals(original, resetReturns)
    }

    @Test
    fun testApplyCachedJsonStateParsesDailyReturns() {
        val repository = VaralakshmiRepository()
        val jsonPayload = """
            {
                "summary": {
                    "totalNav": 110000.0,
                    "allocatedCapital": 100000.0,
                    "todayPnl": 2000.0,
                    "todayPnlPct": 1.85
                },
                "positions": [],
                "transactions": [],
                "daily_returns": [
                    {
                        "date": "2026-09-22",
                        "daily_pnl": 2000.0,
                        "return_pct": 1.85,
                        "total_nav": 110000.0,
                        "trade_count": 2,
                        "status": "WIN"
                    },
                    {
                        "date": "2026-09-21",
                        "daily_pnl": -500.0,
                        "return_pct": -0.46,
                        "total_nav": 108000.0,
                        "trade_count": 1,
                        "status": "LOSS"
                    }
                ]
            }
        """.trimIndent()

        val success = repository.applyCachedJsonState(jsonPayload)
        assertTrue("applyCachedJsonState should return true for valid payload", success)

        val updatedReturns = repository.getCachedDailyReturns()
        assertEquals(2, updatedReturns.size)
        assertEquals("2026-09-22", updatedReturns[0].date)
        assertEquals(2000.0, updatedReturns[0].dailyPnl, 0.001)
        assertEquals("WIN", updatedReturns[0].status)
        assertEquals("2026-09-21", updatedReturns[1].date)
        assertEquals(-500.0, updatedReturns[1].dailyPnl, 0.001)
        assertEquals("LOSS", updatedReturns[1].status)
    }

    @Test
    fun testSubCentRoundingAndBoundaryFormatting() {
        // Values < 0.005 should round to 0.00 and NEVER display negative or positive zero signs
        val subCentPos = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = 0.004,
            returnPct = 0.004,
            totalNav = 100000.00
        )
        assertEquals("₹0.00", subCentPos.formattedPnl)
        assertEquals("0.00%", subCentPos.formattedReturnPct)
        assertEquals("FLAT", subCentPos.status)
        assertFalse(subCentPos.isProfit)

        val subCentNeg = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = -0.004,
            returnPct = -0.004,
            totalNav = 100000.00
        )
        assertEquals("₹0.00", subCentNeg.formattedPnl)
        assertEquals("0.00%", subCentNeg.formattedReturnPct)
        assertEquals("FLAT", subCentNeg.status)
        assertFalse(subCentNeg.isProfit)

        // Half-cent threshold (0.005) formats to ₹0.01 / 0.01%
        val boundaryPos = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = 0.005,
            returnPct = 0.005,
            totalNav = 100000.00
        )
        assertEquals("+₹0.01", boundaryPos.formattedPnl)
        assertEquals("+0.01%", boundaryPos.formattedReturnPct)
        assertEquals("WIN", boundaryPos.status)
        assertTrue(boundaryPos.isProfit)

        val boundaryNeg = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = -0.005,
            returnPct = -0.005,
            totalNav = 100000.00
        )
        assertEquals("-₹0.01", boundaryNeg.formattedPnl)
        assertEquals("-0.01%", boundaryNeg.formattedReturnPct)
        assertEquals("LOSS", boundaryNeg.status)
        assertFalse(boundaryNeg.isProfit)
    }

    @Test
    fun testNaNAndInfiniteHandling() {
        val nanItem = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = Double.NaN,
            returnPct = Double.NaN,
            totalNav = Double.NaN
        )
        assertEquals("₹0.00", nanItem.formattedPnl)
        assertEquals("0.00%", nanItem.formattedReturnPct)
        assertEquals("FLAT", nanItem.status)
        assertFalse(nanItem.isProfit)

        val infItem = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = Double.POSITIVE_INFINITY,
            returnPct = Double.POSITIVE_INFINITY,
            totalNav = Double.POSITIVE_INFINITY
        )
        assertEquals("₹0.00", infItem.formattedPnl)
        assertEquals("0.00%", infItem.formattedReturnPct)
        assertEquals("FLAT", infItem.status)
        assertFalse(infItem.isProfit)

        val negInfItem = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = Double.NEGATIVE_INFINITY,
            returnPct = Double.NEGATIVE_INFINITY,
            totalNav = Double.NEGATIVE_INFINITY
        )
        assertEquals("₹0.00", negInfItem.formattedPnl)
        assertEquals("0.00%", negInfItem.formattedReturnPct)
        assertEquals("FLAT", negInfItem.status)
        assertFalse(negInfItem.isProfit)
    }

    @Test
    fun testDateNormalizationAndAlternativeKeys() {
        val json = """
            [
                {
                    "session_date": "2026-09-25T15:30:00.000Z",
                    "net_pnl": 5200.50,
                    "pnl_pct": 4.75,
                    "closing_nav": 114469.30,
                    "trades": 4,
                    "day_status": "PROFIT"
                },
                {
                    "timestamp": "2026-09-24 15:30:00",
                    "pnl": -1200.00,
                    "daily_return_pct": -1.10,
                    "nav": 109268.80,
                    "trades_count": 2,
                    "status": "DEFICIT"
                }
            ]
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(2, parsed.size)

        val item1 = parsed[0]
        assertEquals("2026-09-25", item1.date)
        assertEquals(5200.50, item1.dailyPnl, 0.001)
        assertEquals(4.75, item1.returnPct, 0.001)
        assertEquals(114469.30, item1.totalNav, 0.001)
        assertEquals(4, item1.tradeCount)
        assertEquals("WIN", item1.status)

        val item2 = parsed[1]
        assertEquals("2026-09-24", item2.date)
        assertEquals(-1200.00, item2.dailyPnl, 0.001)
        assertEquals(-1.10, item2.returnPct, 0.001)
        assertEquals(109268.80, item2.totalNav, 0.001)
        assertEquals(2, item2.tradeCount)
        assertEquals("LOSS", item2.status)
    }

    @Test
    fun testViewModelRefreshSyncsDailyReturns() = runTest {
        val repository = VaralakshmiRepository()
        repository.setServerConfig("http://127.0.0.1:59999")
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false, enablePeriodicSync = false)
        try {
            val initialReturns = viewModel.uiState.value.dailyReturns
            assertEquals(12, initialReturns.size)

            val updatedJson = """
                {
                    "daily_returns": [
                        {
                            "date": "2026-09-23",
                            "daily_pnl": 1800.00,
                            "return_pct": 1.65,
                            "total_nav": 111068.80,
                            "trade_count": 2,
                            "status": "WIN"
                        }
                    ]
                }
            """.trimIndent()
            repository.applyCachedJsonState(updatedJson)

            viewModel.refresh()
            val startTime = System.currentTimeMillis()
            while (viewModel.uiState.value.dailyReturns.size != 1 && (System.currentTimeMillis() - startTime) < 3000) {
                testScheduler.runCurrent()
                Thread.sleep(20)
            }
            testScheduler.runCurrent()

            val syncedReturns = viewModel.uiState.value.dailyReturns
            assertEquals(1, syncedReturns.size)
            assertEquals("2026-09-23", syncedReturns[0].date)
            assertEquals(1800.00, syncedReturns[0].dailyPnl, 0.001)

            // Also verify directly that refreshData returns dailyReturns in OfflineCacheFallback
            val directResult = repository.refreshData("http://127.0.0.1:59999")
            assertTrue(directResult is SyncResult.OfflineCacheFallback)
            assertEquals(1, directResult.dailyReturns.size)
            assertEquals("2026-09-23", directResult.dailyReturns[0].date)
        } finally {
            viewModel.stopPeriodicSync()
            testScheduler.advanceUntilIdle()
        }
    }

    @Test
    fun testConcurrentRepositoryAccessThreadSafety() {
        val repository = VaralakshmiRepository()
        val threads = mutableListOf<Thread>()
        val exceptions = java.util.concurrent.CopyOnWriteArrayList<Throwable>()

        for (i in 0 until 10) {
            val t = Thread {
                try {
                    for (j in 0 until 50) {
                        repository.getCachedDailyReturns()
                        if (j % 5 == 0) {
                            repository.resetToDefaultSeed()
                        }
                    }
                } catch (e: Throwable) {
                    exceptions.add(e)
                }
            }
            threads.add(t)
            t.start()
        }

        for (t in threads) {
            t.join(3000)
        }

        assertTrue("No concurrency exceptions expected, but got: $exceptions", exceptions.isEmpty())
    }

    @Test
    fun testExplicitStatusCaseInsensitiveAndProfitAttribution() {
        val winUpper = DailyReturnItem(date = "2026-09-21", dailyPnl = 0.0, returnPct = 0.0, totalNav = 100000.0, status = "WIN")
        assertTrue("WIN status should produce isProfit = true", winUpper.isProfit)

        val winLower = DailyReturnItem(date = "2026-09-21", dailyPnl = 0.0, returnPct = 0.0, totalNav = 100000.0, status = " win ")
        assertTrue("Trimmed lowercase win status should produce isProfit = true", winLower.isProfit)

        val lossUpper = DailyReturnItem(date = "2026-09-21", dailyPnl = 10.0, returnPct = 0.01, totalNav = 100000.0, status = "LOSS")
        assertFalse("Explicit LOSS status should produce isProfit = false even if PnL > 0", lossUpper.isProfit)

        val flatExplicit = DailyReturnItem(date = "2026-09-21", dailyPnl = 10.0, returnPct = 0.01, totalNav = 100000.0, status = "FLAT")
        assertFalse("Explicit FLAT status should produce isProfit = false even if PnL > 0", flatExplicit.isProfit)
    }

    @Test
    fun testAuthenticWeekdayTradingDatesInDefaultSeeds() {
        val repo = VaralakshmiRepository()
        val returns = repo.getCachedDailyReturns()
        assertEquals(12, returns.size)

        // Verify October 1 and Monday 2026-09-14 are present and weekend dates are NOT present
        assertTrue("Must contain October 1 session", returns.any { it.date == "2026-10-01" })
        assertTrue("Must contain Monday 2026-09-14 session", returns.any { it.date == "2026-09-14" })
        assertFalse("Must NOT contain weekend Saturday 2026-09-12 session", returns.any { it.date == "2026-09-12" })
        assertFalse("Must NOT contain weekend Sunday 2026-09-13 session", returns.any { it.date == "2026-09-13" })
        assertFalse("Must NOT contain weekend Saturday 2026-09-19 session", returns.any { it.date == "2026-09-19" })
        assertFalse("Must NOT contain weekend Sunday 2026-09-20 session", returns.any { it.date == "2026-09-20" })
        assertFalse("Must NOT contain weekend Saturday 2026-09-26 session", returns.any { it.date == "2026-09-26" })
        assertFalse("Must NOT contain weekend Sunday 2026-09-27 session", returns.any { it.date == "2026-09-27" })
    }

    @Test
    fun testBlankAndMissingDateNormalization() {
        val json = """
            [
                {
                    "daily_pnl": 500.0,
                    "return_pct": 0.5,
                    "total_nav": 100500.0
                }
            ]
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(1, parsed.size)
        assertEquals("", parsed[0].date)
        assertEquals("WIN", parsed[0].status)
        assertTrue(parsed[0].isProfit)
    }

    @Test
    fun testEpochTimestampDateNormalization() {
        // 2026-09-21 10:00:00 IST = 1789965000 seconds / 1789965000000 ms
        val json = """
            [
                {
                    "date": "1789965000",
                    "daily_pnl": 100.0,
                    "return_pct": 0.1,
                    "total_nav": 100100.0
                },
                {
                    "date": "1789965000000",
                    "daily_pnl": 200.0,
                    "return_pct": 0.2,
                    "total_nav": 100300.0
                }
            ]
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(2, parsed.size)
        assertEquals("2026-09-21", parsed[0].date)
        assertEquals("2026-09-21", parsed[1].date)
    }

    @Test
    fun testAutomaticReturnPctDerivationWhenMissing() {
        val json = """
            [
                {
                    "date": "2026-09-21",
                    "daily_pnl": 4007.77,
                    "total_nav": 109268.80
                },
                {
                    "date": "2026-09-16",
                    "daily_pnl": -450.50,
                    "total_nav": 100000.00
                }
            ]
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(2, parsed.size)

        // Previous NAV = 109268.80 - 4007.77 = 105261.03
        // Return % = (4007.77 / 105261.03) * 100.0 = 3.81%
        assertEquals(3.81, parsed[0].returnPct, 0.01)
        assertEquals("+3.81%", parsed[0].formattedReturnPct)

        // Previous NAV = 100000.00 - (-450.50) = 100450.50
        // Return % = (-450.50 / 100450.50) * 100.0 = -0.45%
        assertEquals(-0.45, parsed[1].returnPct, 0.01)
        assertEquals("-0.45%", parsed[1].formattedReturnPct)
    }

    @Test
    fun testStatusNormalizationOfSynonyms() {
        val json = """
            [
                { "date": "2026-09-21", "daily_pnl": 100.0, "return_pct": 0.1, "total_nav": 100100.0, "status": "PROFIT" },
                { "date": "2026-09-20", "daily_pnl": 200.0, "return_pct": 0.2, "total_nav": 100300.0, "status": "GAIN" },
                { "date": "2026-09-19", "daily_pnl": -50.0, "return_pct": -0.05, "total_nav": 100250.0, "status": "DEFICIT" },
                { "date": "2026-09-18", "daily_pnl": 0.0, "return_pct": 0.0, "total_nav": 100250.0, "status": "NEUTRAL" },
                { "date": "2026-09-17", "daily_pnl": 0.0, "return_pct": 0.0, "total_nav": 100250.0, "status": "PRE_INCEPTION" },
                { "date": "2026-09-16", "daily_pnl": 0.0, "return_pct": 0.0, "total_nav": 100250.0, "status": "BREAKEVEN" }
            ]
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(6, parsed.size)
        assertEquals("WIN", parsed[0].status)
        assertTrue(parsed[0].isProfit)
        assertEquals("WIN", parsed[1].status)
        assertTrue(parsed[1].isProfit)
        assertEquals("LOSS", parsed[2].status)
        assertFalse(parsed[2].isProfit)
        assertEquals("FLAT", parsed[3].status)
        assertFalse(parsed[3].isProfit)
        assertEquals("FLAT", parsed[4].status)
        assertFalse(parsed[4].isProfit)
        assertEquals("FLAT", parsed[5].status)
        assertFalse(parsed[5].isProfit)
    }

    @Test
    fun testNegativeTradeCountProtection() {
        val json = """
            [
                {
                    "date": "2026-09-21",
                    "daily_pnl": 100.0,
                    "return_pct": 0.1,
                    "total_nav": 100100.0,
                    "trade_count": -5
                }
            ]
        """.trimIndent()
        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(1, parsed.size)
        assertEquals(0, parsed[0].tradeCount)
    }

    @Test
    fun testNegativeZeroFloatFormatting() {
        val item = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = -0.0,
            returnPct = -0.0,
            totalNav = 100000.0
        )
        assertEquals("₹0.00", item.formattedPnl)
        assertEquals("0.00%", item.formattedReturnPct)
        assertEquals("FLAT", item.status)
        assertFalse(item.isProfit)
    }

    @Test
    fun testExtremePnlValuesFormatting() {
        val hugeProfit = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = 1500000.00,
            returnPct = 15.00,
            totalNav = 11500000.00
        )
        assertEquals("+₹1,500,000.00", hugeProfit.formattedPnl)
        assertEquals("+15.00%", hugeProfit.formattedReturnPct)
        assertEquals("WIN", hugeProfit.status)
        assertTrue(hugeProfit.isProfit)

        val hugeLoss = DailyReturnItem(
            date = "2026-09-21",
            dailyPnl = -550000.50,
            returnPct = -5.50,
            totalNav = 9450000.00
        )
        assertEquals("-₹550,000.50", hugeLoss.formattedPnl)
        assertEquals("-5.50%", hugeLoss.formattedReturnPct)
        assertEquals("LOSS", hugeLoss.status)
        assertFalse(hugeLoss.isProfit)
    }

    @Test
    fun testDailyReturnItemTradesParsingAndFormatting() {
        val json = """
            [
                {
                    "date": "2026-10-01",
                    "daily_pnl": -17477.21,
                    "return_pct": -2.91,
                    "total_nav": 600000.00,
                    "trade_count": 2,
                    "status": "LOSS",
                    "trades": [
                        {
                            "transaction_id": "TX_TEST_001",
                            "timestamp": "2026-10-01 11:10:50",
                            "side": "SELL",
                            "symbol": "GMMPFAUDLR",
                            "quantity": 121,
                            "fill_price": 1435.32,
                            "gross_amount": 173673.50,
                            "fees": 20.00,
                            "realized_pnl": -9056.50
                        },
                        {
                            "transaction_id": "TX_TEST_002",
                            "timestamp": "2026-10-01 15:15:18",
                            "side": "BUY",
                            "symbol": "CUBEXTUB",
                            "quantity": 1155,
                            "fill_price": 166.10,
                            "gross_amount": 191845.50,
                            "fees": 20.00,
                            "realized_pnl": 0.00
                        }
                    ]
                }
            ]
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(1, parsed.size)
        val day = parsed[0]
        assertEquals("2026-10-01", day.date)
        assertEquals(2, day.trades.size)

        val sellTrade = day.trades[0]
        assertEquals("TX_TEST_001", sellTrade.transactionId)
        assertEquals("SELL", sellTrade.side)
        assertEquals("GMMPFAUDLR", sellTrade.symbol)
        assertEquals(121, sellTrade.quantity)
        assertEquals(1435.32, sellTrade.fillPrice, 0.001)
        assertEquals(173673.50, sellTrade.grossAmount, 0.001)
        assertEquals(195.89, sellTrade.fees, 0.01)
        assertEquals(-9056.50, sellTrade.realizedPnl, 0.001)
        assertEquals("-₹9,056.50", sellTrade.pnlDifference)

        val buyTrade = day.trades[1]
        assertEquals("TX_TEST_002", buyTrade.transactionId)
        assertEquals("BUY", buyTrade.side)
        assertEquals("CUBEXTUB", buyTrade.symbol)
        assertEquals(1155, buyTrade.quantity)
        assertEquals(166.10, buyTrade.fillPrice, 0.001)
        assertEquals(227.58, buyTrade.fees, 0.01)
        assertEquals("₹0.00", buyTrade.pnlDifference)
    }

    @Test
    fun testCalculateZerodhaDeliveryFees() {
        // Buy CUBEXTUB: gross 191,845.50 -> 227.58
        val buyFee = com.example.varalakshmiportfolio.model.calculateZerodhaDeliveryFees(191845.50, "BUY")
        assertEquals(227.58, buyFee, 0.01)

        // Sell GMMPFAUDLR: gross 173,673.50 -> 195.89
        val sellFee = com.example.varalakshmiportfolio.model.calculateZerodhaDeliveryFees(173673.50, "SELL")
        assertEquals(195.89, sellFee, 0.01)

        // Sell STLNETWORK: gross 195,764.75 -> 218.78
        val sellFee2 = com.example.varalakshmiportfolio.model.calculateZerodhaDeliveryFees(195764.75, "SELL")
        assertEquals(218.78, sellFee2, 0.01)

        // Zero turnover
        val zeroFee = com.example.varalakshmiportfolio.model.calculateZerodhaDeliveryFees(0.0, "BUY")
        assertEquals(0.0, zeroFee, 0.001)
    }

    @Test
    fun testViewModelDailyReturnSelectionAndDismissal() {
        val repo = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository = repo, autoRefresh = false)
        try {
            assertNull(viewModel.uiState.value.selectedDailyReturnForDetail)

            val target = viewModel.uiState.value.dailyReturns.first()
            viewModel.selectDailyReturn(target)
            assertEquals(target, viewModel.uiState.value.selectedDailyReturnForDetail)

            viewModel.dismissDailyReturnDetail()
            assertNull(viewModel.uiState.value.selectedDailyReturnForDetail)
        } finally {
            viewModel.stopPeriodicSync()
        }
    }

    @Test
    fun testLegacy7ItemCacheMigrationReseeds12Sessions() {
        val repo = VaralakshmiRepository()
        // Simulate legacy 7-day cache ending on 2026-09-21 with no trades
        val legacyJson = """
            {
                "daily_returns": [
                    { "date": "2026-09-21", "daily_pnl": 4007.77, "return_pct": 3.81, "total_nav": 109268.80, "trade_count": 1 },
                    { "date": "2026-09-18", "daily_pnl": 2154.20, "return_pct": 2.09, "total_nav": 105261.03, "trade_count": 2 },
                    { "date": "2026-09-17", "daily_pnl": 3106.83, "return_pct": 3.10, "total_nav": 103106.83, "trade_count": 2 },
                    { "date": "2026-09-16", "daily_pnl": -450.50, "return_pct": -0.45, "total_nav": 100000.00, "trade_count": 1 },
                    { "date": "2026-09-15", "daily_pnl": 1280.00, "return_pct": 1.29, "total_nav": 100450.50, "trade_count": 2 },
                    { "date": "2026-09-14", "daily_pnl": -820.00, "return_pct": -0.82, "total_nav": 99170.50, "trade_count": 0 },
                    { "date": "2026-09-11", "daily_pnl": 0.00, "return_pct": 0.00, "total_nav": 99990.50, "trade_count": 0 }
                ]
            }
        """.trimIndent()

        val result = repo.applyCachedJsonState(legacyJson)
        assertTrue(result)
        val migratedReturns = repo.getCachedDailyReturns()
        // Must be migrated to 12 sessions including October 1 and trades
        assertEquals(12, migratedReturns.size)
        assertEquals("2026-10-01", migratedReturns.first().date)
        assertTrue(migratedReturns.first().trades.isNotEmpty())
    }

    @Test
    fun testFormatVaralakshmiAllStatementsForClipboard() {
        val summary = PortfolioSummary(
            totalNav = 112500.0,
            allocatedCapital = 100000.0,
            deployedCapital = 95000.0,
            availableCapital = 17500.0,
            realizedPnl = 5000.0,
            unrealizedPnl = 7500.0,
            totalPnl = 12500.0,
            totalPnlPct = 12.5,
            todayPnl = 1500.0,
            todayPnlPct = 1.35
        )
        val positions = listOf(
            PositionItem(
                positionId = "POS_1",
                symbol = "TESTCO",
                quantity = 100,
                entryPrice = 200.0,
                currentPrice = 220.0,
                marketValue = 22000.0,
                unrealizedPnl = 2000.0,
                unrealizedPnlPct = 10.0,
                peakPrice = 225.0,
                entryDate = "2026-09-20"
            )
        )
        val trades = listOf(
            TransactionItem(
                transactionId = "TX_1",
                timestamp = "2026-10-01 10:15:00",
                side = "BUY",
                symbol = "TESTCO",
                quantity = 100,
                fillPrice = 200.0,
                grossAmount = 20000.0,
                fees = 20.0,
                realizedPnl = 0.0
            )
        )
        val dailyReturns = listOf(
            DailyReturnItem(
                date = "2026-10-01",
                dailyPnl = 1500.0,
                returnPct = 1.35,
                totalNav = 112500.0,
                tradeCount = 1,
                trades = trades
            )
        )

        val output = formatVaralakshmiAllStatementsForClipboard(
            summary = summary,
            positions = positions,
            dailyReturns = dailyReturns,
            transactions = trades
        )

        assertTrue(output.contains("VARALAKSHMI ALPHA COMPOUNDER - FULL PORTFOLIO & TRADING STATEMENTS"))
        assertTrue(output.contains("Total NAV  : ₹112,500.00"))
        assertTrue(output.contains("Total P&L  : +₹12,500.00 (+12.50%)"))
        assertTrue(output.contains("1. ACTIVE PORTFOLIO HOLDINGS (1)"))
        assertTrue(output.contains("TESTCO"))
        assertTrue(output.contains("2. DAILY SESSION RETURNS HISTORY (1 Sessions)"))
        assertTrue(output.contains("2026-10-01"))
        assertTrue(output.contains("+₹1,500.00"))
        assertTrue(output.contains("3. DAILY SESSIONS BREAKDOWN & TRADES"))
        assertTrue(output.contains("4. COMPLETE EXECUTED TRANSACTIONS / FILL LEDGER (1 Records)"))
        assertTrue(output.contains("TX_1") || output.contains("TESTCO"))
    }

    @Test
    fun testFormatDailyReturnStatementForClipboard() {
        val trades = listOf(
            TransactionItem(
                transactionId = "TX_2",
                timestamp = "2026-09-29 11:30:00",
                side = "SELL",
                symbol = "OLDCO",
                quantity = 50,
                fillPrice = 300.0,
                grossAmount = 15000.0,
                fees = 20.0,
                realizedPnl = 1200.0
            )
        )
        val dailyReturn = DailyReturnItem(
            date = "2026-09-29",
            dailyPnl = 1200.0,
            returnPct = 1.08,
            totalNav = 111000.0,
            tradeCount = 1,
            trades = trades
        )

        val output = formatDailyReturnStatementForClipboard(dailyReturn)

        assertTrue(output.contains("VARALAKSHMI EQUITY - DAILY SESSION STATEMENT [2026-09-29]"))
        assertTrue(output.contains("Fund Balance  : ₹111,000.00"))
        assertTrue(output.contains("Daily P&L     : +₹1,200.00 (+1.08%)"))
        assertTrue(output.contains("Trades Count  : 1"))
        assertTrue(output.contains("Total Volume  : ₹15,000.00"))
        assertTrue(output.contains("Total Fees    : ₹20.00"))
        assertTrue(output.contains("EXECUTED TRADES & ORDER FILLS (1)"))
        assertTrue(output.contains("OLDCO"))
        assertTrue(output.contains("SELL"))
        assertTrue(output.contains("+₹1,200.00"))
    }

    @Test
    fun testCapitalInflowFormattingAndClipboardOutput() {
        val depositItem = DailyReturnItem(
            date = "2026-09-24",
            dailyPnl = 8781.36,
            returnPct = 1.46,
            totalNav = 608781.36,
            tradeCount = 2,
            status = "WIN",
            capitalInflow = 490731.20,
            realizedPnl = 8781.36
        )

        assertTrue(depositItem.hasCapitalInflow)
        assertEquals("+₹490,731.20", depositItem.formattedCapitalInflow)

        val clipboardText = formatDailyReturnStatementForClipboard(depositItem)
        assertTrue(clipboardText.contains("Capital Deposit/Inflow: +₹490,731.20"))
        assertTrue(clipboardText.contains("Fund Balance  : ₹608,781.36"))
        assertTrue(clipboardText.contains("Daily P&L     : +₹8,781.36 (+1.46%)"))

        // Test Mathematical Accounting Invariant: Opening Balance + Daily P&L + Inflow = Closing Balance
        val openingBalance = 109268.80
        val closingBalance = openingBalance + depositItem.dailyPnl + depositItem.capitalInflow
        assertEquals(depositItem.totalNav, closingBalance, 0.01)
    }

    @Test
    fun testParseDailyReturnsJsonWithCapitalInflow() {
        val json = """
            [
                {
                    "date": "2026-09-24",
                    "daily_pnl": 8781.36,
                    "return_pct": 1.46,
                    "total_nav": 608781.36,
                    "trade_count": 2,
                    "status": "WIN",
                    "capital_inflow": 490731.20,
                    "realized_pnl": 8781.36,
                    "trades": []
                }
            ]
        """.trimIndent()

        val parsed = VaralakshmiRepository.parseDailyReturnsJson(json)
        assertEquals(1, parsed.size)
        val item = parsed[0]
        assertEquals("2026-09-24", item.date)
        assertEquals(8781.36, item.dailyPnl, 0.001)
        assertEquals(608781.36, item.totalNav, 0.001)
        assertEquals(490731.20, item.capitalInflow, 0.001)
        assertTrue(item.hasCapitalInflow)
        assertEquals("+₹490,731.20", item.formattedCapitalInflow)
    }
}
