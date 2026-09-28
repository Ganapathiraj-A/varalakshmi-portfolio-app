package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.FnoInstanceSummary
import com.example.varalakshmiportfolio.model.FnoPositionItem
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
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
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        tempDir.deleteRecursively()
    }

    @Test
    fun testDefaultVeeraLakshmiStateHasAllThreeInstances() {
        val state = repository.createDefaultVeeraLakshmiState()

        assertEquals("50L", state.selectedInstanceId)
        assertEquals(3, state.instances.size)
        assertTrue(state.instances.containsKey("50L"))
        assertTrue(state.instances.containsKey("20L"))
        assertTrue(state.instances.containsKey("15L"))

        val inst50 = state.instances["50L"]!!
        assertEquals(5000000.0, inst50.allocatedCapital, 0.01)
        assertEquals(1184886.09, inst50.marginBlocked, 0.01)
        assertEquals(50670.50, inst50.unrealizedPnl, 0.01)
        assertEquals(23.70, inst50.marginUtilizationPct, 0.01)
        assertEquals(6, inst50.positionsCount)

        val inst20 = state.instances["20L"]!!
        assertEquals(2000000.0, inst20.allocatedCapital, 0.01)
        assertEquals(554435.18, inst20.marginBlocked, 0.01)
        assertEquals(29489.50, inst20.unrealizedPnl, 0.01)
        assertEquals(27.72, inst20.marginUtilizationPct, 0.01)
        assertEquals(3, inst20.positionsCount)

        val inst15 = state.instances["15L"]!!
        assertEquals(1500000.0, inst15.allocatedCapital, 0.01)
        assertEquals(367735.96, inst15.marginBlocked, 0.01)
        assertEquals(-2522.0, inst15.unrealizedPnl, 0.01)
        assertEquals(24.52, inst15.marginUtilizationPct, 0.01)
        assertEquals(2, inst15.positionsCount)
    }

    @Test
    fun testDefaultPositionsForInstances() {
        val pos50 = repository.getSeedPositionsForInstance("50L")
        assertEquals(6, pos50.size)
        assertTrue(pos50.any { it.symbol == "BANKNIFTY_FUT" && it.direction == "SHORT" })
        assertTrue(pos50.any { it.symbol == "RELIANCE_FUT" && it.direction == "SHORT" })
        assertTrue(pos50.any { it.symbol == "MARUTI_FUT" && it.direction == "SHORT" })
        assertTrue(pos50.any { it.symbol == "NIFTY_FUT" && it.direction == "SHORT" })
        assertTrue(pos50.any { it.symbol == "SBIN_FUT" && it.direction == "SHORT" })

        val pos20 = repository.getSeedPositionsForInstance("20L")
        assertEquals(3, pos20.size)
        assertTrue(pos20.any { it.symbol == "RELIANCE_FUT" })
        assertTrue(pos20.any { it.symbol == "MARUTI_FUT" })
        assertTrue(pos20.any { it.symbol == "NIFTY_FUT" })

        val pos15 = repository.getSeedPositionsForInstance("15L")
        assertEquals(2, pos15.size)
        assertTrue(pos15.any { it.symbol == "NIFTY_FUT" })
        assertTrue(pos15.any { it.symbol == "SBIN_FUT" })
    }

    @Test
    fun testParseFnoInstancesJson() {
        val jsonPayload = """
            {
              "instances": {
                "50L": {
                  "instance_id": "50L",
                  "name": "VeeraLakshmi 50L (Full Quad-Engine)",
                  "allocated_capital": 5000000.0,
                  "margin_blocked": 1184886.09,
                  "unrealized_pnl": 50670.5,
                  "cash_buffer": 3815113.91,
                  "margin_utilization_pct": 23.7,
                  "positions_count": 6,
                  "as_of_date": "2026-09-28"
                },
                "15L": {
                  "instance_id": "15L",
                  "name": "VeeraLakshmi 15L (Lean & Agile)",
                  "allocated_capital": 1500000.0,
                  "margin_blocked": 367735.96,
                  "unrealized_pnl": -2522.0,
                  "cash_buffer": 1132264.04,
                  "margin_utilization_pct": 24.52,
                  "positions_count": 2,
                  "as_of_date": "2026-09-28"
                }
              }
            }
        """.trimIndent()

        val parsed = repository.parseFnoInstances(jsonPayload)
        assertEquals(2, parsed.size)
        assertTrue(parsed.containsKey("50L"))
        assertTrue(parsed.containsKey("15L"))

        val inst50 = parsed["50L"]!!
        assertEquals("50L", inst50.instanceId)
        assertEquals("VeeraLakshmi 50L (Full Quad-Engine)", inst50.name)
        assertEquals(5000000.0, inst50.allocatedCapital, 0.01)
        assertEquals(1184886.09, inst50.marginBlocked, 0.01)
        assertEquals(50670.50, inst50.unrealizedPnl, 0.01)
        assertEquals(3815113.91, inst50.cashBuffer, 0.01)
        assertEquals(23.70, inst50.marginUtilizationPct, 0.01)
        assertEquals(6, inst50.positionsCount)
        assertEquals("2026-09-28", inst50.asOfDate)

        // Verify computed properties
        assertTrue(inst50.returnOnMarginPct > 4.2)
        assertTrue(inst50.returnOnCapitalPct > 1.0)
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
        assertEquals("50L", initial.selectedInstanceId)
        assertEquals(6, initial.activePositions.size)

        val updatedTo20 = repository.selectFnoInstance("20L")
        assertEquals("20L", updatedTo20.selectedInstanceId)
        assertEquals(3, updatedTo20.activePositions.size)

        val updatedTo15 = repository.selectFnoInstance("15L")
        assertEquals("15L", updatedTo15.selectedInstanceId)
        assertEquals(2, updatedTo15.activePositions.size)
    }

    @Test
    fun testViewModelVeeraLakshmiIntegration() {
        val vm = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        val fnoState = vm.veeraLakshmiState.value

        assertEquals("50L", fnoState.selectedInstanceId)
        assertEquals(3, fnoState.instances.size)
        assertEquals(6, fnoState.activePositions.size)

        vm.selectFnoInstance("20L")
        assertEquals("20L", vm.veeraLakshmiState.value.selectedInstanceId)
        assertEquals(3, vm.veeraLakshmiState.value.activePositions.size)
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
            val expectedRom = (inst.unrealizedPnl / inst.marginBlocked) * 100.0
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
        val originalState = repository.createDefaultVeeraLakshmiState()

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
    fun testDiskCacheFallsBackToSeedPositionsOnEmptyCache() {
        val emptyPosState = repository.createDefaultVeeraLakshmiState().copy(
            selectedInstanceId = "20L",
            activePositions = emptyList()
        )
        repository.saveFnoToDisk(emptyPosState)

        // Loading from disk must resolve seed positions for 20L without returning empty list
        val loaded = repository.loadFnoFromDisk()
        assertNotNull(loaded)
        assertEquals("20L", loaded!!.selectedInstanceId)
        assertEquals(3, loaded.activePositions.size)
    }

    @Test
    fun testOfflineFallbackLoadsSeamlesslyWithoutBlankScreens() {
        // Without any network sync, getCachedFnoState returns authentic seed state
        val state = repository.getCachedFnoState()
        assertEquals("50L", state.selectedInstanceId)
        assertFalse(state.isLoading)
        assertFalse(state.isLiveSync)
        assertEquals(3, state.instances.size)
        assertEquals(6, state.activePositions.size)
        assertEquals("Tier 0 (Normal)", state.statusInfo.circuitBreakerTier)
        assertEquals(34.85, state.statusInfo.cagrPct, 0.01)
        assertEquals(1.63, state.statusInfo.sharpeRatio, 0.01)
        assertEquals(64.75, state.statusInfo.winRatePct, 0.01)
        assertEquals(27.66, state.statusInfo.ytdPct, 0.01)
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
        assertEquals(34.85, defaultStatus2.cagrPct, 0.01)
    }

    @Test
    fun testRapidInstanceSwitchingIntegrity() {
        val vm = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        // Rapid successive clicks: 50L -> 20L -> 15L
        vm.selectFnoInstance("20L")
        assertEquals("20L", vm.veeraLakshmiState.value.selectedInstanceId)

        vm.selectFnoInstance("15L")
        assertEquals("15L", vm.veeraLakshmiState.value.selectedInstanceId)
        assertEquals(2, vm.veeraLakshmiState.value.activePositions.size)
    }

    @Test
    fun testDetailedEntryReasonAndSignalParameters() {
        val seed50 = repository.getSeedPositionsForInstance("50L")
        assertTrue(seed50.isNotEmpty())

        for (pos in seed50) {
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
        assertTrue(customPos.detailedReason.contains("NIFTY crossed below its 20-day ATR"))
        assertTrue(customPos.effectiveSignalTrigger.contains("ATR Channel Breakdown"))
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
}
