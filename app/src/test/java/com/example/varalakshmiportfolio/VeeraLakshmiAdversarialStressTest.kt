package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.FnoDailyPnlItem
import com.example.varalakshmiportfolio.model.FnoHistoricalPosition
import com.example.varalakshmiportfolio.model.FnoHistoricalTrade
import com.example.varalakshmiportfolio.model.FnoInstanceSummary
import com.example.varalakshmiportfolio.model.FnoPositionItem
import com.example.varalakshmiportfolio.model.FnoStatusInfo
import com.example.varalakshmiportfolio.model.VeeraLakshmiUiState
import com.example.varalakshmiportfolio.ui.components.formatFullHistoryForClipboard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Empirical Adversarial Stress Test Suite for VeeraLakshmi V2 Android Integration.
 * Stress tests:
 * 1. Model deserialization resilience under empty, malformed, or missing payloads
 * 2. Strict zero-dummy state verification (complete absence of synthetic seeds)
 * 3. Math invariants and division-by-zero protection across all models
 * 4. Disk cache persistence and corrupted cache resilience
 * 5. Formatting safety across date, currency, and clipboard renders
 */
class VeeraLakshmiAdversarialStressTest {

    private lateinit var tempDir: File
    private lateinit var repository: VaralakshmiRepository

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("veeralakshmi_adversarial_test").toFile()
        VaralakshmiRepository.initialize(tempDir)
        repository = VaralakshmiRepository()
    }

    @After
    fun tearDown() {
        VaralakshmiRepository.resetIoDispatcherForTesting()
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        tempDir.deleteRecursively()
    }

    @Test
    fun testEmptyPositionsDeserializationResilience() {
        // Case 1: Standard empty positions array
        val emptyPositionsJson = """{"instance_id": "3L", "positions": []}"""
        val parsed1 = repository.parseFnoPositions(emptyPositionsJson)
        assertNotNull(parsed1)
        assertTrue("Empty positions array must return emptyList", parsed1.isEmpty())

        // Case 2: Missing positions field completely
        val missingPositionsJson = """{"instance_id": "3L", "status": "NO TRADE"}"""
        val parsed2 = repository.parseFnoPositions(missingPositionsJson)
        assertNotNull(parsed2)
        assertTrue("Missing positions key must return emptyList", parsed2.isEmpty())

        // Case 3: Empty JSON object
        val emptyObjectJson = "{}"
        val parsed3 = repository.parseFnoPositions(emptyObjectJson)
        assertNotNull(parsed3)
        assertTrue("Empty JSON object must return emptyList", parsed3.isEmpty())

        // Case 4: Array containing null elements
        val nullElementJson = """{"positions": [null]}"""
        val parsed4 = repository.parseFnoPositions(nullElementJson)
        assertNotNull(parsed4)
        assertTrue("Array with null elements must be skipped safely", parsed4.isEmpty())

        // Case 5: Empty inner position object should parse with safe defaults without crashing
        val emptyInnerObjectJson = """{"positions": [{}]}"""
        val parsed5 = repository.parseFnoPositions(emptyInnerObjectJson)
        assertEquals("Single empty object should produce 1 default position", 1, parsed5.size)
        val pos = parsed5[0]
        assertEquals("pos_0", pos.positionId)
        assertEquals("NIFTY_FUT", pos.symbol)
        assertEquals(0.0, pos.entryPrice, 0.001)
        assertEquals(0.0, pos.currentPrice, 0.001)
        assertEquals(0.0, pos.unrealizedPnl, 0.001)
        assertEquals(0.0, pos.pnlPct, 0.001) // Division by zero guard
    }

    @Test
    fun testEmptyTradeHistoryAndRecordsDeserializationResilience() {
        // Case 1: Empty records array
        val emptyRecordsJson = """{"records": []}"""
        val parsed1 = repository.parseFnoDailyPnl(emptyRecordsJson)
        assertNotNull(parsed1)
        assertTrue("Empty records must return empty list", parsed1.isEmpty())

        // Case 2: Fallback key 'daily_pnl' with empty array
        val fallbackEmptyJson = """{"daily_pnl": []}"""
        val parsed2 = repository.parseFnoDailyPnl(fallbackEmptyJson)
        assertNotNull(parsed2)
        assertTrue("Empty daily_pnl fallback must return empty list", parsed2.isEmpty())

        // Case 3: Completely empty object
        val emptyObjJson = "{}"
        val parsed3 = repository.parseFnoDailyPnl(emptyObjJson)
        assertNotNull(parsed3)
        assertTrue("Empty root must return empty list", parsed3.isEmpty())

        // Case 4: Record with empty trades and empty positions arrays
        val recordEmptyTradesJson = """
            {
              "records": [
                {
                  "date": "2026-10-12",
                  "daily_pnl": 0.0,
                  "daily_return_pct": 0.0,
                  "total_equity": 300000.0,
                  "trades": [],
                  "positions": []
                }
              ]
            }
        """.trimIndent()
        val parsed4 = repository.parseFnoDailyPnl(recordEmptyTradesJson)
        assertEquals(1, parsed4.size)
        val rec = parsed4[0]
        assertEquals("2026-10-12", rec.date)
        assertEquals(0.0, rec.dailyPnl, 0.001)
        assertEquals(300000.0, rec.totalEquity, 0.001)
        assertTrue("Trades must be empty list", rec.trades.isEmpty())
        assertTrue("Positions must be empty list", rec.positions.isEmpty())

        // Case 5: Trades array with null element
        val nullTradeJson = """
            {
              "records": [
                {
                  "date": "2026-10-12",
                  "daily_pnl": 0.0,
                  "trades": [null]
                }
              ]
            }
        """.trimIndent()
        val parsed5 = repository.parseFnoDailyPnl(nullTradeJson)
        assertEquals(1, parsed5.size)
        assertTrue("Null trade element should be skipped safely", parsed5[0].trades.isEmpty())
    }

    @Test
    fun testStatusInfoNoTradeAndEdgeCases() {
        // Case 1: Clean Ready state without skip reason
        val readyJson = """{"subsystem": "fno", "status": "READY", "no_trade_reason": null}"""
        val status1 = repository.parseFnoStatus(readyJson)
        assertEquals("READY", status1.status)
        assertNull(status1.noTradeReason)
        assertEquals(0.0, status1.vixLevel, 0.001)

        // Case 2: Explicit NO TRADE status with holiday reason
        val holidayJson = """
            {
              "subsystem": "fno",
              "status": "NO TRADE — Weekend / Holiday (Markets Closed)",
              "no_trade_reason": "NSE Trading Holiday",
              "vix_level": 11.45,
              "vix_regime": "LOW"
            }
        """.trimIndent()
        val status2 = repository.parseFnoStatus(holidayJson)
        assertTrue(status2.status.startsWith("NO TRADE"))
        assertEquals("NSE Trading Holiday", status2.noTradeReason)
        assertEquals(11.45, status2.vixLevel, 0.001)

        // Case 3: NO TRADE status with gap skip reason
        val gapSkipJson = """
            {
              "subsystem": "fno",
              "status": "NO TRADE — Opening gap 1.25% exceeds 1.0% limit",
              "no_trade_reason": "Opening gap > 1.0% (Gap: 1.25%)"
            }
        """.trimIndent()
        val status3 = repository.parseFnoStatus(gapSkipJson)
        assertTrue(status3.status.startsWith("NO TRADE"))
        assertEquals("Opening gap > 1.0% (Gap: 1.25%)", status3.noTradeReason)

        // Case 4: Completely empty status JSON should yield safe default
        val emptyStatus = repository.parseFnoStatus("{}")
        assertEquals("READY", emptyStatus.status)
        assertNull(emptyStatus.noTradeReason)
        assertEquals(0.0, emptyStatus.cagrPct, 0.001)
    }

    @Test
    fun testMathematicalInvariantsAndDivisionByZeroDefenses() {
        // FnoInstanceSummary with 0 capital and 0 margin
        val zeroSummary = FnoInstanceSummary(
            instanceId = "3L",
            allocatedCapital = 0.0,
            marginBlocked = 0.0,
            unrealizedPnl = 0.0
        )
        assertEquals(0.0, zeroSummary.returnOnMarginPct, 0.001)
        assertEquals(0.0, zeroSummary.returnOnCapitalPct, 0.001)
        assertEquals(0.0, zeroSummary.totalNav, 0.001)
        assertEquals(0.0, zeroSummary.totalPnlPct, 0.001)

        // FnoPositionItem with entryPrice = 0.0
        val zeroEntryPos = FnoPositionItem(
            positionId = "pos_zero",
            symbol = "NIFTY 25000 CE",
            entryPrice = 0.0,
            currentPrice = 125.0,
            direction = "SHORT",
            stopLossPrice = 0.0
        )
        assertEquals(0.0, zeroEntryPos.pnlPct, 0.001)
        assertEquals(0.0, zeroEntryPos.stopLossDistancePct, 0.001)
        assertEquals(0.0, zeroEntryPos.capitalAtRisk, 0.001)
        assertEquals("—", zeroEntryPos.formattedStopLoss)

        // FnoPositionItem short position PnL computation
        val shortPos = FnoPositionItem(
            positionId = "pos_short",
            symbol = "NIFTY 25000 PE",
            entryPrice = 100.0,
            currentPrice = 80.0,
            direction = "SHORT",
            stopLossPrice = 125.0,
            quantity = 75,
            unrealizedPnl = 1500.0
        )
        assertEquals(20.0, shortPos.pnlPct, 0.001) // ((100 - 80) / 100) * 100 = 20%
        assertEquals(25.0, shortPos.stopLossDistancePct, 0.001) // 25% stop
        assertEquals(1875.0, shortPos.capitalAtRisk, 0.001) // 25 * 75 = 1875
        assertEquals("+₹1,500.00", shortPos.formattedUnrealizedPnl)
    }

    @Test
    fun testAntiFabricationStrictStateAudit() {
        val defaultState = repository.createDefaultVeeraLakshmiState()

        // 1. Instance ID strictly "10L"
        assertEquals("10L", defaultState.selectedInstanceId)

        // 2. Only single instance "10L" exists
        assertEquals(1, defaultState.instances.size)
        assertTrue("Instances map must only contain 10L", defaultState.instances.containsKey("10L"))
        assertFalse("Retired 50L must not exist", defaultState.instances.containsKey("50L"))
        assertFalse("Retired 20L must not exist", defaultState.instances.containsKey("20L"))
        assertFalse("Retired 15L must not exist", defaultState.instances.containsKey("15L"))

        // 3. Active positions strictly empty
        assertTrue("Default active positions must be empty", defaultState.activePositions.isEmpty())

        // 4. Daily PnL history strictly empty
        assertTrue("Default daily PnL history must be empty", defaultState.dailyPnlHistory.isEmpty())

        // 5. Today trades strictly empty
        assertTrue("Default today trades must be empty", defaultState.todayTrades.isEmpty())

        // 6. Zero dummy symbols
        val bannedSymbols = setOf("BANKNIFTY", "RELIANCE", "MARUTI", "SBIN")
        for (pos in defaultState.activePositions) {
            assertFalse("Banned synthetic symbol in position: ${pos.symbol}", bannedSymbols.any { pos.symbol.contains(it) })
        }
    }

    @Test
    fun testDiskCacheCleanRoundTripAndCorruptionTolerance() {
        VaralakshmiRepository.initialize(tempDir)
        // 1. Save default clean state
        val cleanState = repository.createDefaultVeeraLakshmiState()
        repository.saveFnoToDisk(cleanState)

        // 2. Load back from disk: Must remain clean with 0 positions and 0 trades
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals("10L", loaded!!.selectedInstanceId)
        assertTrue("Loaded positions must be empty", loaded.activePositions.isEmpty())
        assertTrue("Loaded daily PnL must be empty", loaded.dailyPnlHistory.isEmpty())
        assertTrue("Loaded today trades must be empty", loaded.todayTrades.isEmpty())

        // 3. Corrupt cache test: write invalid JSON to disk
        val cacheFile = File(tempDir, "veeralakshmi_fno_cache.json")
        cacheFile.writeText("INVALID_CORRUPTED_JSON_DATA{{{", Charsets.UTF_8)

        // 4. Loading corrupted cache must safely return null without throwing
        val corruptedLoaded = repository.loadFnoFromDisk()
        assertNull("Corrupted cache must safely return null", corruptedLoaded)

        // 5. Repository fallback with corrupted cache must provide clean default state
        val safeState = repository.loadFnoFromDisk() ?: repository.createDefaultVeeraLakshmiState()
        assertEquals("10L", safeState.selectedInstanceId)
        assertTrue(safeState.activePositions.isEmpty())
    }

    @Test
    fun testFormattedExpiryAdversarialInputs() {
        fun makeItem(expiry: String) = FnoPositionItem(
            positionId = "pos_exp",
            symbol = "NIFTY 25000 CE",
            expiryDate = expiry
        )

        // Standard valid date
        assertEquals("29 Oct 2026", makeItem("2026-10-29").formattedExpiry)
        assertEquals("12 Oct 2026", makeItem("2026-10-12").formattedExpiry)

        // Blank or whitespace
        assertEquals("—", makeItem("").formattedExpiry)
        assertEquals("—", makeItem("   ").formattedExpiry)

        // Irregular date formats must not crash
        assertEquals("29/10/2026", makeItem("29/10/2026").formattedExpiry)
        assertEquals("99 99 2026", makeItem("2026-99-99").formattedExpiry)
        assertEquals("EXPIRY_OCT_2026", makeItem("EXPIRY_OCT_2026").formattedExpiry)
    }

    @Test
    fun testFormatFullHistoryForClipboardStress() {
        // 1. Completely empty history
        val emptyResult = formatFullHistoryForClipboard(
            items = emptyList(),
            winRatePct = 0.0,
            netPnl = 0.0,
            winDays = 0,
            lossDays = 0,
            avgDailyPnl = 0.0,
            bestDay = 0.0
        )
        assertNotNull(emptyResult)
        assertTrue(emptyResult.contains("FORWARD PAPER TEST LEDGER"))
        assertTrue(emptyResult.contains("Awaiting forward paper test sessions starting Monday 2026-10-12"))

        // 2. Single session with zero trades
        val zeroTradeItem = FnoDailyPnlItem(
            date = "2026-10-12",
            dailyPnl = 0.0,
            dailyReturnPct = 0.0,
            totalEquity = 300000.0,
            dayStatus = "WIN",
            trades = emptyList(),
            positions = emptyList()
        )
        val singleResult = formatFullHistoryForClipboard(
            items = listOf(zeroTradeItem),
            winRatePct = 100.0,
            netPnl = 0.0,
            winDays = 1,
            lossDays = 0,
            avgDailyPnl = 0.0,
            bestDay = 0.0
        )
        assertNotNull(singleResult)
        assertTrue(singleResult.contains("Total Sessions : 1"))
        assertTrue(singleResult.contains("2026-10-12"))
    }
}
