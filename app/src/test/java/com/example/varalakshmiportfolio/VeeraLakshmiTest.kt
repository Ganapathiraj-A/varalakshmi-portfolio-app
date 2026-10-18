package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.FnoDailyPnlItem
import com.example.varalakshmiportfolio.model.FnoHistoricalTrade
import com.example.varalakshmiportfolio.model.FnoInstanceSummary
import com.example.varalakshmiportfolio.model.FnoPositionItem
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import com.example.varalakshmiportfolio.ui.components.formatFullHistoryForClipboard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VeeraLakshmiTest {

    private lateinit var tempDir: File
    private lateinit var repository: VaralakshmiRepository

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("veeralakshmi_test").toFile()
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
    fun testDefaultVeeraLakshmiStateHasSingleV2Instance() {
        val state = repository.createDefaultVeeraLakshmiState()

        assertEquals("10L", state.selectedInstanceId)
        assertEquals(1, state.instances.size)
        assertTrue(state.instances.containsKey("10L"))

        val inst10L = state.instances["10L"]!!
        assertEquals(1000000.0, inst10L.allocatedCapital, 0.01)
        assertEquals(0.0, inst10L.marginBlocked, 0.01)
        assertEquals(0.0, inst10L.unrealizedPnl, 0.01)
        assertEquals(0.0, inst10L.marginUtilizationPct, 0.01)
        assertEquals(0, inst10L.positionsCount)
        assertTrue("Active positions must be empty on initialization", state.activePositions.isEmpty())
        assertTrue("Daily PnL history must be empty on initialization", state.dailyPnlHistory.isEmpty())
    }

    @Test
    fun testDefaultPositionsForInstances() {
        // Anti-fabrication check: repository default state and positions are completely clean
        val defaultState = repository.createDefaultVeeraLakshmiState()
        assertTrue("V2 default positions must be empty on initialization", defaultState.activePositions.isEmpty())
        assertTrue("V2 default daily PnL history must be empty on initialization", defaultState.dailyPnlHistory.isEmpty())
    }

    @Test
    fun testParseFnoInstancesJson() {
        val jsonPayload = """
            {
              "instances": {
                "3L": {
                  "instance_id": "3L",
                  "name": "VeeraLakshmi 3L (Agile Micro-Quad)",
                  "allocated_capital": 300000.0,
                  "margin_blocked": 65000.0,
                  "unrealized_pnl": 2450.0,
                  "cash_buffer": 235000.0,
                  "margin_utilization_pct": 21.67,
                  "positions_count": 2,
                  "as_of_date": "2026-10-12"
                }
              }
            }
        """.trimIndent()

        val parsed = repository.parseFnoInstances(jsonPayload)
        assertEquals(1, parsed.size)
        assertTrue(parsed.containsKey("3L"))

        val inst3L = parsed["3L"]!!
        assertEquals("3L", inst3L.instanceId)
        assertEquals("VeeraLakshmi 3L (Agile Micro-Quad)", inst3L.name)
        assertEquals(300000.0, inst3L.allocatedCapital, 0.01)
        assertEquals(65000.0, inst3L.marginBlocked, 0.01)
        assertEquals(2450.0, inst3L.unrealizedPnl, 0.01)
        assertEquals(235000.0, inst3L.cashBuffer, 0.01)
        assertEquals(21.67, inst3L.marginUtilizationPct, 0.01)
        assertEquals(2, inst3L.positionsCount)
        assertEquals("2026-10-12", inst3L.asOfDate)

        // Verify computed properties
        assertTrue(inst3L.returnOnMarginPct > 3.0)
        assertTrue(inst3L.returnOnCapitalPct > 0.8)
    }

    @Test
    fun testParseFnoPositionsJson() {
        val jsonPayload = """
            {
              "instance_id": "50L",
              "positions": [
                {
                  "position_id": "IndexTrendEngine_BANKNIFTY_FUT",
                  "strategy_engine": "IndexTrendEngine",
                  "symbol": "BANKNIFTY_FUT",
                  "instrument_type": "FUT",
                  "direction": "SHORT",
                  "quantity": 30,
                  "lots": 1,
                  "entry_price": 58381.1,
                  "current_price": 57591.0,
                  "margin_required": 262714.95,
                  "stop_loss_price": 58900.0,
                  "unrealized_pnl": 23703.0,
                  "expiry_date": "2026-09-24",
                  "strike_price": 0.0,
                  "option_type": null
                }
              ]
            }
        """.trimIndent()

        val positions = repository.parseFnoPositions(jsonPayload)
        assertEquals(1, positions.size)
        val pos = positions[0]
        assertEquals("IndexTrendEngine_BANKNIFTY_FUT", pos.positionId)
        assertEquals("IndexTrendEngine", pos.strategyEngine)
        assertEquals("BANKNIFTY_FUT", pos.symbol)
        assertEquals("SHORT", pos.direction)
        assertEquals(30, pos.quantity)
        assertEquals(1, pos.lots)
        assertEquals(58381.10, pos.entryPrice, 0.01)
        assertEquals(57591.00, pos.currentPrice, 0.01)
        assertEquals(262714.95, pos.marginRequired, 0.01)
        assertEquals(58900.00, pos.stopLossPrice, 0.01)
        assertEquals(23703.00, pos.unrealizedPnl, 0.01)
        assertTrue(pos.isProfit)
        assertTrue(pos.pnlPct > 1.3)
    }

    @Test
    fun testParseFnoStatusJson() {
        val jsonPayload = """
            {
              "subsystem": "fno",
              "status": "READY",
              "circuit_breaker_tier": "Tier 0 (Normal)",
              "vix_level": 8.81,
              "vix_regime": "NORMAL",
              "summary_metrics": {
                "cagr_pct": 34.85,
                "ytd_2026_pct": 27.66,
                "max_drawdown_pct": -14.31,
                "sharpe_ratio": 1.63,
                "win_rate_pct": 64.75
              },
              "last_date": "2026-09-28"
            }
        """.trimIndent()

        val status = repository.parseFnoStatus(jsonPayload)
        assertEquals("fno", status.subsystem)
        assertEquals("READY", status.status)
        assertEquals("Tier 0 (Normal)", status.circuitBreakerTier)
        assertEquals(8.81, status.vixLevel, 0.01)
        assertEquals("NORMAL", status.vixRegime)
        assertEquals(34.85, status.cagrPct, 0.01)
        assertEquals(27.66, status.ytdPct, 0.01)
        assertEquals(-14.31, status.maxDrawdownPct, 0.01)
        assertEquals(1.63, status.sharpeRatio, 0.01)
        assertEquals(64.75, status.winRatePct, 0.01)
        assertEquals("2026-09-28", status.lastDate)
    }

    @Test
    fun testInstanceSelectionUpdatesState() {
        val initial = repository.getCachedFnoState()
        assertEquals("10L", initial.selectedInstanceId)
        assertTrue(initial.activePositions.isEmpty())

        val updated = repository.selectFnoInstance("10L")
        assertEquals("10L", updated.selectedInstanceId)
        assertTrue(updated.activePositions.isEmpty())
    }

    @Test
    fun testViewModelVeeraLakshmiIntegration() {
        val vm = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        val fnoState = vm.veeraLakshmiState.value

        assertEquals("10L", fnoState.selectedInstanceId)
        assertEquals(1, fnoState.instances.size)
        assertTrue(fnoState.activePositions.isEmpty())

        vm.selectFnoInstance("10L")
        assertEquals("10L", vm.veeraLakshmiState.value.selectedInstanceId)
        assertTrue(vm.veeraLakshmiState.value.activePositions.isEmpty())
        vm.stopPeriodicSync()
    }

    @Test
    fun testMarginCalculationsAndFinancialInvariants() {
        val defaultState = repository.createDefaultVeeraLakshmiState()

        for ((id, inst) in defaultState.instances) {
            // Invariant 1: Allocated Capital = Margin Blocked + Free Cash Buffer
            assertEquals(
                "Allocated capital invariant violated for $id",
                inst.allocatedCapital,
                inst.marginBlocked + inst.cashBuffer,
                0.01
            )

            // Invariant 2: Margin utilization percentage = (marginBlocked / allocatedCapital) * 100
            val expectedUtil = (inst.marginBlocked / inst.allocatedCapital) * 100.0
            assertEquals(
                "Margin utilization formula mismatch for $id",
                expectedUtil,
                inst.marginUtilizationPct,
                0.05
            )

            // Invariant 3: Return on margin = (unrealizedPnl / marginBlocked) * 100
            val expectedRom = if (inst.marginBlocked > 0.0) (inst.unrealizedPnl / inst.marginBlocked) * 100.0 else 0.0
            assertEquals(
                "Return on margin formula mismatch for $id",
                expectedRom,
                inst.returnOnMarginPct,
                0.01
            )

            // Invariant 4: Return on capital = (unrealizedPnl / allocatedCapital) * 100
            val expectedRoc = (inst.unrealizedPnl / inst.allocatedCapital) * 100.0
            assertEquals(
                "Return on capital formula mismatch for $id",
                expectedRoc,
                inst.returnOnCapitalPct,
                0.01
            )
        }

        // Test position-level PnL calculations for SHORT and LONG
        val shortPos = FnoPositionItem(
            positionId = "test_short",
            symbol = "NIFTY_FUT",
            direction = "SHORT",
            entryPrice = 25000.0,
            currentPrice = 24500.0,
            marginRequired = 200000.0,
            unrealizedPnl = 500.0
        )
        // SHORT profit when price drops: (25000 - 24500) / 25000 * 100 = +2.0%
        assertEquals(2.0, shortPos.pnlPct, 0.001)
        assertTrue(shortPos.isProfit)
        assertTrue(shortPos.formattedMargin.startsWith("₹"))
        assertFalse(shortPos.formattedMargin.startsWith("₹₹"))

        val longPos = FnoPositionItem(
            positionId = "test_long",
            symbol = "BANKNIFTY_FUT",
            direction = "LONG",
            entryPrice = 50000.0,
            currentPrice = 51000.0,
            marginRequired = 250000.0,
            unrealizedPnl = 1000.0
        )
        // LONG profit when price rises: (51000 - 50000) / 50000 * 100 = +2.0%
        assertEquals(2.0, longPos.pnlPct, 0.001)
        assertTrue(longPos.isProfit)

        // Zero entry price edge case safety
        val zeroEntryPos = FnoPositionItem(
            positionId = "test_zero",
            symbol = "ZERO_FUT",
            entryPrice = 0.0,
            currentPrice = 100.0
        )
        assertEquals(0.0, zeroEntryPos.pnlPct, 0.001)
    }

    @Test
    fun testDiskCacheSerializationAndRestoration() {
        val samplePosition = FnoPositionItem(
            positionId = "pos_v2_1",
            strategyEngine = "IntradayStraddleEngine",
            symbol = "NIFTY 25000 CE",
            instrumentType = "OPT",
            direction = "SHORT",
            quantity = 75,
            lots = 1,
            entryPrice = 120.0,
            currentPrice = 95.0,
            marginRequired = 65000.0,
            stopLossPrice = 150.0,
            unrealizedPnl = 1875.0,
            expiryDate = "2026-10-15"
        )
        val originalState = repository.createDefaultVeeraLakshmiState().copy(
            activePositions = listOf(samplePosition)
        )

        // 1. Save to disk cache
        repository.saveFnoToDisk(originalState)

        val cacheFile = File(tempDir, VaralakshmiRepository.FNO_CACHE_FILE_NAME)
        assertTrue("Cache file should exist on disk", cacheFile.exists())
        assertTrue("Cache file should not be empty", cacheFile.length() > 0)

        // 2. Load back from disk
        val restoredState = repository.loadFnoFromDisk()
        assertNotNull("Restored state should not be null", restoredState)
        assertEquals(originalState.selectedInstanceId, restoredState!!.selectedInstanceId)
        assertEquals(originalState.instances.size, restoredState.instances.size)
        assertEquals(originalState.activePositions.size, restoredState.activePositions.size)

        // Verify instance summaries match exactly
        for ((key, expected) in originalState.instances) {
            val actual = restoredState.instances[key]
            assertNotNull("Instance $key should be present in restored state", actual)
            assertEquals(expected.instanceId, actual!!.instanceId)
            assertEquals(expected.allocatedCapital, actual.allocatedCapital, 0.01)
            assertEquals(expected.marginBlocked, actual.marginBlocked, 0.01)
            assertEquals(expected.unrealizedPnl, actual.unrealizedPnl, 0.01)
            assertEquals(expected.cashBuffer, actual.cashBuffer, 0.01)
            assertEquals(expected.marginUtilizationPct, actual.marginUtilizationPct, 0.01)
            assertEquals(expected.positionsCount, actual.positionsCount)
        }

        // Verify active positions match
        assertEquals(originalState.activePositions.size, restoredState.activePositions.size)
        val posOriginal = originalState.activePositions.first()
        val posRestored = restoredState.activePositions.first()
        assertEquals(posOriginal.positionId, posRestored.positionId)
        assertEquals(posOriginal.symbol, posRestored.symbol)
        assertEquals(posOriginal.direction, posRestored.direction)
        assertEquals(posOriginal.entryPrice, posRestored.entryPrice, 0.01)
        assertEquals(posOriginal.currentPrice, posRestored.currentPrice, 0.01)
        assertEquals(posOriginal.marginRequired, posRestored.marginRequired, 0.01)

        // Verify status info matches
        assertEquals(originalState.statusInfo.subsystem, restoredState.statusInfo.subsystem)
        assertEquals(originalState.statusInfo.circuitBreakerTier, restoredState.statusInfo.circuitBreakerTier)
        assertEquals(originalState.statusInfo.vixLevel, restoredState.statusInfo.vixLevel, 0.01)
        assertEquals(originalState.statusInfo.cagrPct, restoredState.statusInfo.cagrPct, 0.01)
        assertEquals(originalState.statusInfo.sharpeRatio, restoredState.statusInfo.sharpeRatio, 0.01)
    }

    @Test
    fun testDiskCachePreservesCleanEmptyPositionsWithoutSeedFallback() {
        val emptyPosState = repository.createDefaultVeeraLakshmiState().copy(
            selectedInstanceId = "3L",
            activePositions = emptyList()
        )
        repository.saveFnoToDisk(emptyPosState)

        // Loading from disk must preserve clean empty positions without resurrecting seeds
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals("3L", loaded!!.selectedInstanceId)
        assertTrue("Disk cache must not resurrect synthetic positions", loaded.activePositions.isEmpty())
    }

    @Test
    fun testOfflineFallbackLoadsSeamlesslyWithoutBlankScreens() {
        // Without any network sync, getCachedFnoState returns authentic clean V2 default state
        val state = repository.getCachedFnoState()
        assertEquals("10L", state.selectedInstanceId)
        assertFalse(state.isLoading)
        assertFalse(state.isLiveSync)
        assertEquals(1, state.instances.size)
        assertTrue(state.activePositions.isEmpty())
        assertEquals("Tier 0 (Normal)", state.statusInfo.circuitBreakerTier)
    }

    @Test
    fun testNegativePnlFormattingCleanCurrencySymbol() {
        // Verify negative PnL puts the minus sign before the rupee symbol (-₹), never ₹-
        val lossInstance = FnoInstanceSummary(
            instanceId = "15L",
            unrealizedPnl = -2522.00
        )
        assertEquals("-₹2,522.00", lossInstance.formattedUnrealizedPnl)
        assertFalse("Must not format as ₹-", lossInstance.formattedUnrealizedPnl.startsWith("₹-"))

        val profitInstance = FnoInstanceSummary(
            instanceId = "50L",
            unrealizedPnl = 50670.50
        )
        assertEquals("+₹50,670.50", profitInstance.formattedUnrealizedPnl)

        val lossPosition = FnoPositionItem(
            positionId = "loss_pos",
            symbol = "SBIN_FUT",
            unrealizedPnl = -5362.50
        )
        assertEquals("-₹5,362.50", lossPosition.formattedUnrealizedPnl)
        assertFalse("Must not format as ₹-", lossPosition.formattedUnrealizedPnl.startsWith("₹-"))

        val profitPosition = FnoPositionItem(
            positionId = "profit_pos",
            symbol = "NIFTY_FUT",
            unrealizedPnl = 2840.50
        )
        assertEquals("+₹2,840.50", profitPosition.formattedUnrealizedPnl)
    }

    @Test
    fun testFinancialMathZeroAndBoundarySafety() {
        val zeroInst = FnoInstanceSummary(
            allocatedCapital = 0.0,
            marginBlocked = 0.0,
            unrealizedPnl = 0.0
        )
        assertEquals(0.0, zeroInst.returnOnMarginPct, 0.001)
        assertEquals(0.0, zeroInst.returnOnCapitalPct, 0.001)
        assertFalse(zeroInst.returnOnMarginPct.isNaN())
        assertFalse(zeroInst.returnOnCapitalPct.isNaN())

        val zeroPos = FnoPositionItem(
            positionId = "zero",
            symbol = "TEST",
            entryPrice = 0.0,
            currentPrice = 0.0
        )
        assertEquals(0.0, zeroPos.pnlPct, 0.001)
        assertFalse(zeroPos.pnlPct.isNaN())
    }

    @Test
    fun testFnoNavAndPnlPctCalculationsAndFormatting() {
        // Clean slate 3L
        val inst3L = FnoInstanceSummary(
            instanceId = "3L",
            allocatedCapital = 300000.0,
            unrealizedPnl = 0.0,
            marginBlocked = 0.0,
            cashBuffer = 300000.0
        )
        assertEquals(300000.0, inst3L.totalNav, 0.001)
        assertEquals(0.0, inst3L.totalPnlPct, 0.001)
        assertEquals("₹300,000.00", inst3L.formattedTotalNav)
        assertEquals("(+0.00%)", inst3L.formattedTotalPnlPct)

        // Profit instance 50L
        val inst50 = FnoInstanceSummary(
            instanceId = "50L",
            allocatedCapital = 5000000.0,
            unrealizedPnl = 50670.50
        )
        assertEquals(5050670.50, inst50.totalNav, 0.001)
        assertEquals(1.01341, inst50.totalPnlPct, 0.001)
        assertEquals("₹5,050,670.50", inst50.formattedTotalNav)
        assertEquals("(+1.01%)", inst50.formattedTotalPnlPct)

        // Loss instance 15L
        val inst15 = FnoInstanceSummary(
            instanceId = "15L",
            allocatedCapital = 1500000.0,
            unrealizedPnl = -2522.0
        )
        assertEquals(1497478.0, inst15.totalNav, 0.001)
        assertEquals(-0.16813, inst15.totalPnlPct, 0.001)
        assertEquals("₹1,497,478.00", inst15.formattedTotalNav)
        assertEquals("(-0.17%)", inst15.formattedTotalPnlPct)
    }

    @Test
    fun testMalformedAndEmptyJsonResilience() {
        // parseFnoInstances resilience
        val emptyInstances = repository.parseFnoInstances("")
        assertTrue(emptyInstances.isEmpty())
        val corruptInstances = repository.parseFnoInstances("{ invalid json }")
        assertTrue(corruptInstances.isEmpty())
        val emptyObjInstances = repository.parseFnoInstances("{}")
        assertTrue(emptyObjInstances.isEmpty())

        // parseFnoPositions resilience
        val emptyPositions = repository.parseFnoPositions("")
        assertTrue(emptyPositions.isEmpty())
        val corruptPositions = repository.parseFnoPositions("not json")
        assertTrue(corruptPositions.isEmpty())
        val emptyObjPositions = repository.parseFnoPositions("{}")
        assertTrue(emptyObjPositions.isEmpty())

        // parseFnoStatus resilience
        val defaultStatus1 = repository.parseFnoStatus("")
        assertEquals("fno", defaultStatus1.subsystem)
        assertEquals("READY", defaultStatus1.status)
        val defaultStatus2 = repository.parseFnoStatus("{ invalid }")
        assertEquals(0.0, defaultStatus2.cagrPct, 0.01)
    }

    @Test
    fun testRapidInstanceSwitchingIntegrity() {
        val vm = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        vm.selectFnoInstance("3L")
        assertEquals("3L", vm.veeraLakshmiState.value.selectedInstanceId)
        assertTrue(vm.veeraLakshmiState.value.activePositions.isEmpty())
        vm.stopPeriodicSync()
    }

    @Test
    fun testDetailedEntryReasonAndSignalParameters() {
        val testPositions = listOf(
            FnoPositionItem(
                positionId = "straddle_ce_test",
                strategyEngine = "IntradayStraddleEngine",
                symbol = "NIFTY 25000 CE",
                instrumentType = "OPT",
                direction = "SHORT",
                quantity = 75,
                lots = 1,
                entryPrice = 120.0,
                currentPrice = 95.0,
                marginRequired = 65000.0,
                stopLossPrice = 150.0,
                unrealizedPnl = 1875.0,
                expiryDate = "2026-10-15",
                strikePrice = 25000.0,
                optionType = "CE",
                entryReason = "9:20 AM ATM straddle short entry with 25% stop-loss guard",
                targetPrice = 10.0,
                signalTrigger = "09:20 IST ATM Straddle Deployment",
                riskReward = "1 : 2.5 RR"
            ),
            FnoPositionItem(
                positionId = "decoupling_ce_test",
                strategyEngine = "DecouplingBullCallSpreadEngine",
                symbol = "BANKNIFTY 54000 CE",
                instrumentType = "OPT",
                direction = "LONG",
                quantity = 30,
                lots = 1,
                entryPrice = 220.0,
                currentPrice = 260.0,
                marginRequired = 45000.0,
                stopLossPrice = 160.0,
                unrealizedPnl = 1200.0,
                expiryDate = "2026-10-15",
                strikePrice = 54000.0,
                optionType = "CE",
                entryReason = "US-India decoupled relative strength breakout above 5-day high",
                targetPrice = 340.0,
                signalTrigger = "Decoupling RS Breakout > 5-Day High",
                riskReward = "1 : 2.0 RR"
            )
        )

        for (pos in testPositions) {
            // Verify every position has a rich, non-empty detailed entry rationale
            assertTrue("Detailed reason should not be blank for ${pos.symbol}", pos.detailedReason.isNotBlank())
            assertTrue("Detailed reason should be descriptive (> 30 chars)", pos.detailedReason.length > 30)

            // Verify signal trigger is present
            assertTrue("Signal trigger should not be blank for ${pos.symbol}", pos.effectiveSignalTrigger.isNotBlank())

            // Verify stop loss metrics
            assertTrue("Stop loss should be positive for ${pos.symbol}", pos.stopLossPrice > 0.0)
            assertTrue("Stop loss distance pct should be positive", pos.stopLossDistancePct > 0.0)
            assertTrue("Capital at risk should be positive", pos.capitalAtRisk > 0.0)
            assertTrue("Effective target price should be positive", pos.effectiveTargetPrice > 0.0)
            assertTrue("Risk reward should be non-empty", pos.effectiveRiskReward.contains("RR"))
        }

        // Test fallback dynamic reason for custom position with blank entryReason
        val customPos = FnoPositionItem(
            positionId = "custom_test",
            strategyEngine = "IndexTrendEngine",
            symbol = "NIFTY_FUT",
            instrumentType = "FUT",
            direction = "SHORT",
            quantity = 65,
            lots = 1,
            entryPrice = 26000.0,
            currentPrice = 25950.0,
            marginRequired = 250000.0,
            stopLossPrice = 26300.0,
            unrealizedPnl = 3250.0,
            entryReason = "" // blank to trigger dynamic fallback
        )
        assertTrue(customPos.detailedReason.contains("Systematic short derivative position"))
        assertTrue(customPos.effectiveSignalTrigger.contains("Systematic Quantitative Rules Trigger"))
        assertEquals("₹25,340.00", customPos.formattedTargetPrice)
        assertEquals(19500.0, customPos.capitalAtRisk, 0.01)
        assertEquals(1.15, customPos.stopLossDistancePct, 0.01)
    }

    @Test
    fun testParseFnoPositionsJsonWithEntryReason() {
        val jsonPayload = """
            {
              "instance_id": "50L",
              "positions": [
                {
                  "position_id": "test_opt_ce",
                  "strategy_engine": "OptionsCreditSpread",
                  "symbol": "BANKNIFTY 58500 CE",
                  "instrument_type": "OPT",
                  "option_type": "CE",
                  "direction": "SHORT",
                  "quantity": 30,
                  "lots": 1,
                  "entry_price": 420.0,
                  "current_price": 310.0,
                  "margin_required": 145000.0,
                  "stop_loss_price": 550.0,
                  "unrealized_pnl": 3300.0,
                  "expiry_date": "2026-09-24",
                  "strike_price": 58500.0,
                  "entry_reason": "Out of the money call credit spread entered to harvest theta decay outside 1.8 SD expected move.",
                  "target_price": 100.0,
                  "signal_trigger": "IV Percentile > 75 + Overhead Resistance",
                  "risk_reward": "1 : 2.5 RR"
                }
              ]
            }
        """.trimIndent()

        val parsed = repository.parseFnoPositions(jsonPayload)
        assertEquals(1, parsed.size)
        val pos = parsed[0]
        assertEquals("test_opt_ce", pos.positionId)
        assertEquals("BANKNIFTY 58500 CE", pos.symbol)
        assertEquals("OPT", pos.instrumentType)
        assertEquals("CE", pos.optionType)
        assertEquals("SHORT", pos.direction)
        assertEquals(30, pos.quantity)
        assertEquals("Out of the money call credit spread entered to harvest theta decay outside 1.8 SD expected move.", pos.entryReason)
        assertEquals("Out of the money call credit spread entered to harvest theta decay outside 1.8 SD expected move.", pos.detailedReason)
        assertEquals(100.0, pos.targetPrice, 0.01)
        assertEquals("IV Percentile > 75 + Overhead Resistance", pos.signalTrigger)
        assertEquals("IV Percentile > 75 + Overhead Resistance", pos.effectiveSignalTrigger)
        assertEquals("1 : 2.5 RR", pos.riskReward)
        assertEquals("1 : 2.5 RR", pos.effectiveRiskReward)
    }

    @Test
    fun testDiskCachePreservesEntryReason() {
        val customPos = FnoPositionItem(
            positionId = "custom_1",
            strategyEngine = "StockMomentumEngine",
            symbol = "INFY_FUT",
            instrumentType = "FUT",
            direction = "LONG",
            quantity = 400,
            lots = 1,
            entryPrice = 1920.0,
            currentPrice = 1955.0,
            marginRequired = 180000.0,
            stopLossPrice = 1880.0,
            unrealizedPnl = 14000.0,
            entryReason = "IT sector momentum revival and bullish cup-and-handle pattern breakout.",
            targetPrice = 2020.0,
            signalTrigger = "Cup-and-Handle Breakout on Above Average Volume",
            riskReward = "1 : 2.5 RR"
        )

        val defaultState = repository.createDefaultVeeraLakshmiState()
        val customState = defaultState.copy(activePositions = listOf(customPos))

        repository.saveFnoToDisk(customState)
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals(1, loaded!!.activePositions.size)

        val loadedPos = loaded.activePositions[0]
        assertEquals("custom_1", loadedPos.positionId)
        assertEquals("INFY_FUT", loadedPos.symbol)
        assertEquals("IT sector momentum revival and bullish cup-and-handle pattern breakout.", loadedPos.entryReason)
        assertEquals("Cup-and-Handle Breakout on Above Average Volume", loadedPos.signalTrigger)
        assertEquals(2020.0, loadedPos.targetPrice, 0.01)
        assertEquals("1 : 2.5 RR", loadedPos.riskReward)
    }

    @Test
    fun testFormattedExpiryRollsForwardPastDates() {
        // Formatted date from September expiry displays authentic parsed date
        val pastPos = FnoPositionItem(
            positionId = "past_fut",
            symbol = "NIFTY_FUT",
            expiryDate = "2026-09-24"
        )
        assertEquals("24 Sep 2026", pastPos.formattedExpiry)

        // Blank expiry should display clean dash
        val blankPos = FnoPositionItem(
            positionId = "blank_fut",
            symbol = "BANKNIFTY_FUT",
            expiryDate = ""
        )
        assertEquals("—", blankPos.formattedExpiry)

        // Valid active future expiry
        val futurePos = FnoPositionItem(
            positionId = "future_fut",
            symbol = "RELIANCE_FUT",
            expiryDate = "2026-10-29"
        )
        assertEquals("29 Oct 2026", futurePos.formattedExpiry)

        val novPos = FnoPositionItem(
            positionId = "nov_fut",
            symbol = "MARUTI_FUT",
            expiryDate = "2026-11-26"
        )
        assertEquals("26 Nov 2026", novPos.formattedExpiry)
    }

    @Test
    fun testParseFnoDailyPnl() {
        val jsonPayload = """
            {
              "instance_id": "50L",
              "records": [
                {
                  "date": "2026-09-28",
                  "daily_pnl": 21180.0,
                  "daily_return_pct": 0.42,
                  "total_equity": 5050670.5,
                  "margin_blocked": 1184886.09,
                  "positions_count": 1,
                  "trade_count": 1,
                  "day_status": "WIN",
                  "positions": [
                    {
                      "symbol": "BANKNIFTY_FUT",
                      "direction": "SHORT",
                      "instrument_type": "FUT",
                      "lots": 1,
                      "quantity": 30,
                      "entry_price": 58381.1,
                      "close_price": 57591.0,
                      "day_pnl": 23703.0,
                      "strategy_engine": "IndexTrendEngine"
                    }
                  ],
                  "trades": [
                    {
                      "trade_id": "TRD_28_01",
                      "time": "09:20",
                      "symbol": "BANKNIFTY_FUT",
                      "action": "SELL",
                      "instrument_type": "FUT",
                      "lots": 1,
                      "quantity": 30,
                      "execution_price": 58381.1,
                      "realized_pnl": 0.0,
                      "execution_reason": "Index breakdown trigger"
                    }
                  ]
                },
                {
                  "date": "2026-09-24",
                  "daily_pnl": -12400.0,
                  "daily_return_pct": -0.25,
                  "total_equity": 4995240.5,
                  "margin_blocked": 1150000.0,
                  "positions_count": 0,
                  "trade_count": 0,
                  "day_status": "LOSS"
                }
              ]
            }
        """.trimIndent()

        val parsed = repository.parseFnoDailyPnl(jsonPayload)
        assertEquals(2, parsed.size)

        val day1 = parsed[0]
        assertEquals("2026-09-28", day1.date)
        assertEquals(21180.0, day1.dailyPnl, 0.01)
        assertEquals(0.42, day1.dailyReturnPct, 0.01)
        assertEquals(5050670.5, day1.totalEquity, 0.01)
        assertEquals(1, day1.tradeCount)
        assertEquals(1, day1.positionsCount)
        assertEquals("WIN", day1.dayStatus)
        assertTrue(day1.isProfit)
        assertTrue(day1.formattedDailyPnl.contains("21,180"))
        assertEquals("+0.42%", day1.formattedDailyReturn)

        // Verify parsed positions & trades
        assertEquals(1, day1.positions.size)
        val pos = day1.positions[0]
        assertEquals("BANKNIFTY_FUT", pos.symbol)
        assertEquals("SHORT", pos.direction)
        assertTrue(pos.isShort)
        assertEquals(58381.10, pos.entryPrice, 0.01)
        assertEquals(57591.00, pos.closePrice, 0.01)
        assertEquals(23703.00, pos.dayPnl, 0.01)
        assertTrue(pos.isProfit)

        assertEquals(1, day1.trades.size)
        val trd = day1.trades[0]
        assertEquals("TRD_28_01", trd.tradeId)
        assertEquals("09:20", trd.time)
        assertEquals("SELL", trd.action)
        assertTrue(trd.isSell)
        assertEquals("Index breakdown trigger", trd.executionReason)

        val day2 = parsed[1]
        assertEquals("2026-09-24", day2.date)
        assertEquals(-12400.0, day2.dailyPnl, 0.01)
        assertEquals(-0.25, day2.dailyReturnPct, 0.01)
        assertEquals("LOSS", day2.dayStatus)
        assertFalse(day2.isProfit)
        assertTrue(day2.formattedDailyPnl.contains("12,400"))
        assertEquals("-0.25%", day2.formattedDailyReturn)
        assertTrue(day2.positions.isEmpty())
        assertTrue(day2.trades.isEmpty())
    }

    @Test
    fun testParseFnoDailyPnlAuthenticV2Payload() {
        val jsonPayload = """
            {
              "instance_id": "3L",
              "records": [
                {
                  "date": "2026-10-12",
                  "daily_pnl": 2450.0,
                  "daily_return_pct": 0.81,
                  "total_equity": 302450.0,
                  "margin_blocked": 65000.0,
                  "positions_count": 0,
                  "trade_count": 2,
                  "day_status": "WIN",
                  "costs": 142.50,
                  "positions": [],
                  "trades": [
                    {
                      "trade_id": "TRD_V2_01",
                      "time": "09:20",
                      "symbol": "NIFTY 25000 CE",
                      "action": "SELL",
                      "instrument_type": "OPT",
                      "lots": 1,
                      "quantity": 75,
                      "execution_price": 120.0,
                      "realized_pnl": 0.0,
                      "execution_reason": "09:20 IST ATM straddle leg",
                      "costs": 71.25
                    },
                    {
                      "trade_id": "TRD_V2_02",
                      "time": "15:15",
                      "symbol": "NIFTY 25000 CE",
                      "action": "BUY",
                      "instrument_type": "OPT",
                      "lots": 1,
                      "quantity": 75,
                      "execution_price": 87.33,
                      "realized_pnl": 2450.0,
                      "execution_reason": "15:15 IST intraday time square-off",
                      "costs": 71.25
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val records = repository.parseFnoDailyPnl(jsonPayload)
        assertEquals(1, records.size)
        val rec = records[0]
        assertEquals("2026-10-12", rec.date)
        assertEquals(2450.0, rec.dailyPnl, 0.01)
        assertEquals(142.50, rec.costs, 0.01)
        assertEquals(2, rec.trades.size)
        assertEquals("TRD_V2_01", rec.trades[0].tradeId)
        assertEquals(71.25, rec.trades[0].costs, 0.01)
        assertEquals("TRD_V2_02", rec.trades[1].tradeId)
        assertEquals(2450.0, rec.trades[1].realizedPnl, 0.01)
    }

    @Test
    fun testDiskCachePreservesDailyPnlHistory() {
        val sampleRecord = FnoDailyPnlItem(
            date = "2026-10-12",
            dailyPnl = 2450.0,
            dailyReturnPct = 0.81,
            totalEquity = 302450.0,
            marginBlocked = 65000.0,
            positionsCount = 0,
            tradeCount = 2,
            dayStatus = "WIN",
            costs = 142.50,
            positions = emptyList(),
            trades = listOf(
                FnoHistoricalTrade(
                    tradeId = "TRD_V2_01",
                    time = "09:20",
                    symbol = "NIFTY 25000 CE",
                    action = "SELL",
                    instrumentType = "OPT",
                    lots = 1,
                    quantity = 75,
                    executionPrice = 120.0,
                    realizedPnl = 0.0,
                    executionReason = "09:20 IST ATM straddle leg",
                    costs = 71.25
                )
            )
        )
        val defaultState = repository.createDefaultVeeraLakshmiState().copy(
            dailyPnlHistory = listOf(sampleRecord)
        )
        assertEquals(1, defaultState.dailyPnlHistory.size)

        repository.saveFnoToDisk(defaultState)
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals(1, loaded!!.dailyPnlHistory.size)

        val first = loaded.dailyPnlHistory[0]
        assertEquals("2026-10-12", first.date)
        assertEquals(2450.0, first.dailyPnl, 0.01)
        assertEquals("WIN", first.dayStatus)
        assertEquals(1, first.trades.size)
        assertEquals("TRD_V2_01", first.trades[0].tradeId)
        assertEquals(71.25, first.trades[0].costs, 0.01)
    }

    @Test
    fun testAuthoritativeV2StraddleTradeLifecycleAndCostReconciliation() {
        val entryTrade = FnoHistoricalTrade(
            tradeId = "TRD_01",
            time = "09:20",
            symbol = "NIFTY 25000 CE",
            action = "SELL",
            instrumentType = "OPT",
            lots = 1,
            quantity = 75,
            executionPrice = 120.0,
            realizedPnl = 0.0,
            executionReason = "09:20 ATM straddle short entry",
            costs = 72.10
        )
        val exitTrade = FnoHistoricalTrade(
            tradeId = "TRD_02",
            time = "15:15",
            symbol = "NIFTY 25000 CE",
            action = "BUY",
            instrumentType = "OPT",
            lots = 1,
            quantity = 75,
            executionPrice = 85.0,
            realizedPnl = 2625.0,
            executionReason = "15:15 intraday square-off",
            costs = 72.10
        )

        val dailyItem = FnoDailyPnlItem(
            date = "2026-10-12",
            dailyPnl = 2625.0,
            dailyReturnPct = 0.875,
            totalEquity = 302625.0,
            marginBlocked = 65000.0,
            positionsCount = 0,
            tradeCount = 2,
            dayStatus = "WIN",
            costs = 144.20,
            positions = emptyList(),
            trades = listOf(entryTrade, exitTrade)
        )

        assertEquals("WIN", dailyItem.dayStatus)
        assertEquals(2, dailyItem.trades.size)
        val totalCosts = dailyItem.trades.sumOf { it.costs }
        assertEquals(144.20, totalCosts, 0.01)
        val netPnl = dailyItem.dailyPnl - totalCosts
        assertEquals(2480.80, netPnl, 0.01)
    }

    @Test
    fun testFormatFullHistoryForClipboard() {
        val emptyFormatted = formatFullHistoryForClipboard(
            items = emptyList(),
            winRatePct = 0.0,
            netPnl = 0.0,
            winDays = 0,
            lossDays = 0,
            avgDailyPnl = 0.0,
            bestDay = 0.0
        )
        assertNotNull(emptyFormatted)
        assertTrue(emptyFormatted.contains("VEERALAKSHMI V2 (₹3,00,000)"))
        assertTrue(emptyFormatted.contains("Awaiting forward paper test sessions starting Monday 2026-10-12"))

        // Also test non-empty formatted history
        val testItem = FnoDailyPnlItem(
            date = "2026-10-12",
            dailyPnl = 2500.0,
            dailyReturnPct = 0.83,
            totalEquity = 302500.0,
            marginBlocked = 65000.0,
            positionsCount = 0,
            tradeCount = 2,
            dayStatus = "WIN",
            costs = 140.0
        )
        val nonEmptyFormatted = formatFullHistoryForClipboard(
            items = listOf(testItem),
            winRatePct = 100.0,
            netPnl = 2500.0,
            winDays = 1,
            lossDays = 0,
            avgDailyPnl = 2500.0,
            bestDay = 2500.0
        )
        assertNotNull(nonEmptyFormatted)
        assertTrue(nonEmptyFormatted.contains("Total Sessions : 1"))
        assertTrue(nonEmptyFormatted.contains("100.0%"))
    }

    @Test
    fun testParseFnoClosedTrades() {
        val jsonPayload = """
            {
              "instance_id": "10L",
              "closed_trades": [
                {
                  "trade_id": "TRD_20261014_0001",
                  "tradingsymbol": "NIFTY26O1922700PE",
                  "direction": "SHORT",
                  "instrument_type": "PE",
                  "lots": 1,
                  "closed_quantity": 65,
                  "entry_price": 150.95,
                  "exit_price": 195.55,
                  "exit_time": "13:03",
                  "net_pnl": -2965.67,
                  "exit_reason": "STOP_LOSS",
                  "total_friction": 66.67
                },
                {
                  "trade_id": "TRD_20261014_0002",
                  "tradingsymbol": "NIFTY26O1922700CE",
                  "direction": "SHORT",
                  "instrument_type": "CE",
                  "lots": 1,
                  "closed_quantity": 65,
                  "entry_price": 150.75,
                  "exit_price": 97.30,
                  "exit_time": "15:15",
                  "net_pnl": 3410.27,
                  "exit_reason": "SQUARE_OFF",
                  "total_friction": 63.98
                }
              ]
            }
        """.trimIndent()

        val trades = repository.parseFnoClosedTrades(jsonPayload)
        assertEquals(2, trades.size)

        val t1 = trades[0]
        assertEquals("TRD_20261014_0001", t1.tradeId)
        assertEquals("NIFTY26O1922700PE", t1.symbol)
        assertEquals("13:03", t1.time)
        assertEquals("BUY", t1.action)
        assertEquals(65, t1.quantity)
        assertEquals(195.55, t1.executionPrice, 0.01)
        assertEquals(-2965.67, t1.realizedPnl, 0.01)
        assertEquals("STOP_LOSS", t1.executionReason)
        assertEquals(66.67, t1.costs, 0.01)
        assertFalse(t1.isProfit)

        val t2 = trades[1]
        assertEquals("TRD_20261014_0002", t2.tradeId)
        assertEquals("NIFTY26O1922700CE", t2.symbol)
        assertEquals("15:15", t2.time)
        assertEquals(3410.27, t2.realizedPnl, 0.01)
        assertEquals("SQUARE_OFF", t2.executionReason)
        assertTrue(t2.isProfit)

        val totalNetPnl = trades.sumOf { it.realizedPnl }
        assertEquals(444.60, totalNetPnl, 0.01)
    }

    @Test
    fun testParseFnoInstancesWithRealizedPnl() {
        val jsonPayload = """
            {
              "instances": {
                "10L": {
                  "instance_id": "10L",
                  "name": "VeeraLakshmi V3 Flagship (₹10L Paper Trading)",
                  "allocated_capital": 1000000.0,
                  "margin_blocked": 0.0,
                  "unrealized_pnl": 0.0,
                  "realized_pnl": 444.60,
                  "total_pnl": 444.60,
                  "total_equity": 1000444.60,
                  "cash_buffer": 1000444.60,
                  "margin_utilization_pct": 0.0,
                  "positions_count": 0,
                  "as_of_date": "2026-10-14"
                }
              }
            }
        """.trimIndent()

        val parsed = repository.parseFnoInstances(jsonPayload)
        val inst10L = parsed["10L"]
        assertNotNull(inst10L)
        assertEquals(1000000.0, inst10L!!.allocatedCapital, 0.01)
        assertEquals(444.60, inst10L.unrealizedPnl, 0.01)
        assertEquals(1000444.60, inst10L.totalNav, 0.01)
        assertEquals(0.04446, inst10L.returnOnCapitalPct, 0.001)
    }

    @Test
    fun testParseFnoClosedTradesLiveServerSchema() {
        val jsonPayload = """
            {
              "instance_id": "10L",
              "as_of_date": "2026-10-14",
              "closed_trades": [
                {
                  "trade_id": "TRD_20261014_0001",
                  "tradingsymbol": "NIFTY26O1922700PE",
                  "direction": "SHORT",
                  "closed_quantity": 65,
                  "exit_price": 195.55,
                  "net_pnl": -2965.67,
                  "exit_reason": "STOP_LOSS",
                  "total_friction": 66.67,
                  "exit_timestamp": "2026-10-14T13:03:04.878941+05:30"
                },
                {
                  "trade_id": "TRD_20261014_0002",
                  "tradingsymbol": "NIFTY26O1922700CE",
                  "direction": "SHORT",
                  "closed_quantity": 65,
                  "exit_price": 97.30,
                  "net_pnl": 3410.27,
                  "exit_reason": "SQUARE_OFF",
                  "total_friction": 63.98,
                  "exit_timestamp": "2026-10-14T15:15:05.045107+05:30"
                }
              ]
            }
        """.trimIndent()

        val trades = repository.parseFnoClosedTrades(jsonPayload)
        assertEquals(2, trades.size)

        val t1 = trades[0]
        assertEquals("TRD_20261014_0001", t1.tradeId)
        assertEquals("NIFTY26O1922700PE", t1.symbol)
        assertEquals("BUY", t1.action)
        assertEquals("OPT", t1.instrumentType)
        assertEquals(65, t1.quantity)
        assertEquals(195.55, t1.executionPrice, 0.01)
        assertEquals(-2965.67, t1.realizedPnl, 0.01)
        assertEquals("STOP_LOSS", t1.executionReason)
        assertEquals(66.67, t1.costs, 0.01)
        assertEquals("13:03", t1.time)
        assertEquals("2026-10-14", t1.date)

        val t2 = trades[1]
        assertEquals("TRD_20261014_0002", t2.tradeId)
        assertEquals("NIFTY26O1922700CE", t2.symbol)
        assertEquals(3410.27, t2.realizedPnl, 0.01)
        assertEquals("15:15", t2.time)
        assertEquals("2026-10-14", t2.date)

        val netSum = trades.sumOf { it.realizedPnl }
        assertEquals(444.60, netSum, 0.01)
    }

    @Test
    fun testParseFnoDailyPnlLiveServerTrades() {
        val jsonPayload = """
            {
              "instance_id": "10L",
              "records": [
                {
                  "date": "2026-10-14",
                  "daily_pnl": 444.60,
                  "daily_return_pct": 0.044,
                  "total_equity": 1000444.60,
                  "trade_count": 2,
                  "day_status": "WIN",
                  "trades": [
                    {
                      "trade_id": "TRD_20261014_0001",
                      "tradingsymbol": "NIFTY26O1922700PE",
                      "direction": "SHORT",
                      "closed_quantity": 65,
                      "exit_price": 195.55,
                      "net_pnl": -2965.67,
                      "exit_reason": "STOP_LOSS",
                      "total_friction": 66.67,
                      "exit_timestamp": "2026-10-14T13:03:04.878941+05:30"
                    },
                    {
                      "trade_id": "TRD_20261014_0002",
                      "tradingsymbol": "NIFTY26O1922700CE",
                      "direction": "SHORT",
                      "closed_quantity": 65,
                      "exit_price": 97.30,
                      "net_pnl": 3410.27,
                      "exit_reason": "SQUARE_OFF",
                      "total_friction": 63.98,
                      "exit_timestamp": "2026-10-14T15:15:05.045107+05:30"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val records = repository.parseFnoDailyPnl(jsonPayload)
        assertEquals(1, records.size)
        val rec = records[0]
        assertEquals("2026-10-14", rec.date)
        assertEquals(2, rec.trades.size)
        assertEquals("NIFTY26O1922700PE", rec.trades[0].symbol)
        assertEquals(65, rec.trades[0].quantity)
        assertEquals(-2965.67, rec.trades[0].realizedPnl, 0.01)
        assertEquals(3410.27, rec.trades[1].realizedPnl, 0.01)
    }

    @Test
    fun testLoadFnoFromDiskPreservesLatestTradesAcrossDays() {
        val trd1 = FnoHistoricalTrade(
            tradeId = "TRD_1",
            time = "13:03",
            symbol = "NIFTY26O1922700PE",
            action = "BUY",
            instrumentType = "OPT",
            quantity = 65,
            executionPrice = 195.55,
            realizedPnl = -2965.67,
            executionReason = "STOP_LOSS",
            costs = 66.67,
            date = "2026-10-14"
        )
        val trd2 = FnoHistoricalTrade(
            tradeId = "TRD_2",
            time = "15:15",
            symbol = "NIFTY26O1922700CE",
            action = "BUY",
            instrumentType = "OPT",
            quantity = 65,
            executionPrice = 97.30,
            realizedPnl = 3410.27,
            executionReason = "SQUARE_OFF",
            costs = 63.98,
            date = "2026-10-14"
        )

        val dayPnl = FnoDailyPnlItem(
            date = "2026-10-14",
            dailyPnl = 444.60,
            dailyReturnPct = 0.044,
            totalEquity = 1000444.60,
            tradeCount = 2,
            trades = listOf(trd1, trd2)
        )

        val stateToSave = com.example.varalakshmiportfolio.model.VeeraLakshmiUiState(
            selectedInstanceId = "10L",
            instances = mapOf("10L" to FnoInstanceSummary(
                instanceId = "10L",
                name = "VeeraLakshmi V3",
                allocatedCapital = 1000000.0,
                marginBlocked = 0.0,
                unrealizedPnl = 444.60,
                cashBuffer = 1000444.60,
                marginUtilizationPct = 0.0,
                positionsCount = 0,
                asOfDate = "2026-10-14"
            )),
            dailyPnlHistory = listOf(dayPnl),
            todayTrades = listOf(trd1, trd2)
        )

        repository.saveFnoToDisk(stateToSave)
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals(2, loaded!!.todayTrades.size)
        assertEquals("NIFTY26O1922700PE", loaded.todayTrades[0].symbol)
        assertEquals("NIFTY26O1922700CE", loaded.todayTrades[1].symbol)
    }

    @Test
    fun testParseFnoPositionsWithLiveBackendPayloadAndNegativeShortQuantity() {
        // Simulates real live payload from /api/fno/positions?instance=10L
        val jsonPayload = """
            {
              "instance_id": "10L",
              "name": "VeeraLakshmi V3 Flagship (₹10L Paper Trading)",
              "as_of_date": "2026-10-15",
              "allocated_capital": 1000000.0,
              "margin_blocked": 76500.0,
              "unrealized_pnl": 3474.25,
              "cash_buffer": 923500.0,
              "margin_utilization_pct": 7.65,
              "positions": [
                {
                  "position_id": "POS_NIFTY26O1922950PE_20261015_2",
                  "symbol": "NIFTY26O1922950PE",
                  "tradingsymbol": "NIFTY26O1922950PE",
                  "instrument_token": 12072962,
                  "instrument_type": "PE",
                  "strike": 22950.0,
                  "expiry": "2026-10-19",
                  "side": "SELL",
                  "direction": "SHORT",
                  "quantity": -65,
                  "entry_price": 142.0,
                  "current_price": 88.55,
                  "stop_loss_price": 177.5,
                  "stop_loss": 177.5,
                  "margin_required": 76500.0,
                  "status": "ACTIVE",
                  "strategy": "VeeraLakshmi_V2_Straddle",
                  "pnl": 0.0,
                  "unrealized_pnl": 3474.25
                }
              ]
            }
        """.trimIndent()

        val positions = repository.parseFnoPositions(jsonPayload)
        assertEquals(1, positions.size)
        val position = positions[0]

        assertEquals("POS_NIFTY26O1922950PE_20261015_2", position.positionId)
        assertEquals("VeeraLakshmi_V2_Straddle", position.strategyEngine)
        assertEquals("NIFTY26O1922950PE", position.symbol)
        assertEquals("PE", position.instrumentType)
        assertEquals("PE", position.optionType)
        assertEquals("SHORT", position.direction)
        assertEquals(65, position.quantity)
        assertEquals(142.0, position.entryPrice, 0.01)
        assertEquals(88.55, position.currentPrice, 0.01)
        assertEquals(177.5, position.stopLossPrice, 0.01)
        assertEquals(22950.0, position.strikePrice, 0.01)
        assertEquals(3474.25, position.unrealizedPnl, 0.01)
        assertTrue("Capital at risk must be strictly positive", position.capitalAtRisk > 0.0)
        assertEquals(2307.50, position.capitalAtRisk, 0.01)
        assertTrue(position.isProfit)
    }

    @Test
    fun testCapitalAtRiskNeverNegativeEvenWithExplicitNegativeQuantity() {
        val position = FnoPositionItem(
            positionId = "test_neg_qty",
            symbol = "NIFTY26O1922950PE",
            quantity = -65,
            entryPrice = 142.0,
            stopLossPrice = 177.5
        )

        assertTrue("Capital at risk must be positive even if quantity is negative", position.capitalAtRisk > 0.0)
        assertEquals(2307.50, position.capitalAtRisk, 0.01)
        assertFalse("Formatted capital at risk should not contain minus", position.formattedCapitalAtRisk.contains("-"))
    }

    @Test
    fun testParseFnoDailyPnlWithNestedHistoricalPositionsFromLiveBackend() {
        val jsonPayload = """
            {
              "instance_id": "10L",
              "daily_pnl": [
                {
                  "date": "2026-10-15",
                  "daily_pnl": 1843.94,
                  "daily_return_pct": 0.18,
                  "total_equity": 1001843.94,
                  "margin_blocked": 76500.0,
                  "positions_count": 1,
                  "trade_count": 1,
                  "day_status": "WIN",
                  "positions": [
                    {
                      "tradingsymbol": "NIFTY26O1922950PE",
                      "symbol": "NIFTY",
                      "direction": "SHORT",
                      "instrument_type": "PE",
                      "lots": 1,
                      "quantity": -65,
                      "entry_price": 142.0,
                      "current_price": 88.55,
                      "unrealized_net_pnl": 3437.77,
                      "strategy": "VeeraLakshmi_V2_Straddle"
                    }
                  ],
                  "trades": []
                }
              ]
            }
        """.trimIndent()

        val records = repository.parseFnoDailyPnl(jsonPayload)
        assertEquals(1, records.size)
        val day = records[0]
        assertEquals("2026-10-15", day.date)
        assertEquals(1, day.positions.size)

        val pos = day.positions[0]
        assertEquals("NIFTY26O1922950PE", pos.symbol)
        assertEquals("SHORT", pos.direction)
        assertEquals(65, pos.quantity)
        assertEquals(142.0, pos.entryPrice, 0.01)
        assertEquals(88.55, pos.closePrice, 0.01)
        assertEquals(3437.77, pos.dayPnl, 0.01)
        assertEquals("VeeraLakshmi_V2_Straddle", pos.strategyEngine)
        assertTrue(pos.isProfit)
        assertEquals("₹88.55", pos.formattedClosePrice)
        assertTrue(pos.formattedDayPnl.startsWith("+₹3,437.77"))
    }
}
