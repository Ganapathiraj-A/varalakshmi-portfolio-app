package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VaralakshmiProfitTargetTest {

    private lateinit var tempCacheDir: File

    @Before
    fun setUp() {
        tempCacheDir = Files.createTempDirectory("varalakshmi_test_cache").toFile()
        VaralakshmiRepository.initialize(tempCacheDir)
    }

    @After
    fun tearDown() {
        VaralakshmiRepository.resetCacheDirectoryForTesting()
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun testPositionItemDefaultProfitTargetEnabled() {
        val position = PositionItem(
            positionId = "POS_1",
            symbol = "TEST",
            quantity = 100,
            entryPrice = 50.0,
            currentPrice = 60.0,
            marketValue = 6000.0,
            unrealizedPnl = 1000.0,
            unrealizedPnlPct = 20.0,
            peakPrice = 60.0,
            entryDate = "2026-09-22"
        )
        // Default requirement: ON (+35% Target Active)
        assertTrue("PositionItem profit target should default to true", position.isProfitTargetEnabled)

        // Toggle to Uncapped Runner
        val uncapped = position.copy(isProfitTargetEnabled = false)
        assertFalse("PositionItem can be toggled to uncapped runner", uncapped.isProfitTargetEnabled)
    }

    @Test
    fun testVaralakshmiRepositoryPersistenceOfToggleState() {
        val repository = VaralakshmiRepository()
        repository.resetToDefaultSeed()

        val initialPositions = repository.getCachedPositions()
        val stl = initialPositions.first { it.symbol == "STLNETWORK" }
        assertTrue("Initial default STLNETWORK must be profit target enabled", stl.isProfitTargetEnabled)

        // 1. Toggle OFF (Uncapped runner)
        val updatedPositions1 = repository.updateProfitTarget("STLNETWORK", false)
        val stlUpdated1 = updatedPositions1.first { it.symbol == "STLNETWORK" }
        assertFalse("STLNETWORK should now have profit target disabled", stlUpdated1.isProfitTargetEnabled)

        // Other positions must remain unaffected
        val ahcl1 = updatedPositions1.first { it.symbol == "AHCL" }
        assertTrue("AHCL must still have profit target enabled", ahcl1.isProfitTargetEnabled)

        // 2. Verify disk persistence across cache reload
        val newRepo = VaralakshmiRepository()
        val reloadedPositions1 = newRepo.getCachedPositions()
        val reloadedStl1 = reloadedPositions1.first { it.symbol == "STLNETWORK" }
        assertFalse("Disk cache reload must preserve disabled profit target for STLNETWORK", reloadedStl1.isProfitTargetEnabled)

        // 3. Toggle back ON (+35% Target)
        val updatedPositions2 = repository.updateProfitTarget("STLNETWORK", true)
        val stlUpdated2 = updatedPositions2.first { it.symbol == "STLNETWORK" }
        assertTrue("STLNETWORK should now be re-enabled", stlUpdated2.isProfitTargetEnabled)

        val newRepo2 = VaralakshmiRepository()
        val reloadedPositions2 = newRepo2.getCachedPositions()
        val reloadedStl2 = reloadedPositions2.first { it.symbol == "STLNETWORK" }
        assertTrue("Disk cache reload must preserve re-enabled profit target for STLNETWORK", reloadedStl2.isProfitTargetEnabled)
    }

    @Test
    fun testVaralakshmiViewModelStateFlowImmediateUpdateOnToggle() {
        val repository = VaralakshmiRepository()
        repository.resetToDefaultSeed()

        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        val initialPos = viewModel.uiState.value.positions.first { it.symbol == "STLNETWORK" }
        assertTrue("Initial state must be target enabled", initialPos.isProfitTargetEnabled)

        // 1. Toggle OFF
        viewModel.toggleProfitTarget(initialPos)
        val stateAfterToggle1 = viewModel.uiState.value
        val posAfterToggle1 = stateAfterToggle1.positions.first { it.symbol == "STLNETWORK" }
        assertFalse("StateFlow must immediately update isProfitTargetEnabled to false without jitter", posAfterToggle1.isProfitTargetEnabled)
        assertNotNull("Snackbar message should confirm toggle", stateAfterToggle1.snackbarMessage)
        assertTrue("Snackbar should indicate Uncapped Runner", stateAfterToggle1.snackbarMessage!!.contains("Uncapped Runner"))

        // 2. Toggle back ON using symbol overload
        viewModel.toggleProfitTarget("STLNETWORK")
        val stateAfterToggle2 = viewModel.uiState.value
        val posAfterToggle2 = stateAfterToggle2.positions.first { it.symbol == "STLNETWORK" }
        assertTrue("StateFlow must immediately update isProfitTargetEnabled to true", posAfterToggle2.isProfitTargetEnabled)
        assertTrue("Snackbar should indicate +35% Cap", stateAfterToggle2.snackbarMessage!!.contains("+35% Cap"))
    }

    @Test
    fun testToggleNonexistentSymbolIsNoOp() {
        val repository = VaralakshmiRepository()
        repository.resetToDefaultSeed()
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        val initialSize = viewModel.uiState.value.positions.size

        // Attempt to toggle unknown symbol
        viewModel.toggleProfitTarget("NONEXISTENT_STOCK")
        assertEquals("Positions list must remain unaffected", initialSize, viewModel.uiState.value.positions.size)
    }

    @Test
    fun testPositionItemEqualityAndCopyIntegrity() {
        val p1 = PositionItem(
            positionId = "POS_1",
            symbol = "TEST",
            quantity = 100,
            entryPrice = 50.0,
            currentPrice = 60.0,
            marketValue = 6000.0,
            unrealizedPnl = 1000.0,
            unrealizedPnlPct = 20.0,
            peakPrice = 60.0,
            entryDate = "2026-09-22",
            isProfitTargetEnabled = true
        )
        val p2 = p1.copy(isProfitTargetEnabled = false)
        assertFalse(p2.isProfitTargetEnabled)
        assertEquals(p1.symbol, p2.symbol)
        assertEquals(p1.entryPrice, p2.entryPrice, 0.001)
        assertEquals(p1.currentPrice, p2.currentPrice, 0.001)
    }

    @Test
    fun testDiskCacheDeserializationNullProfitTargetOverrideIsUncappedRunner() {
        // Write raw cache JSON with profit_target_override: null (simulating uncapped runner from backend)
        val cacheFile = File(tempCacheDir, VaralakshmiRepository.CACHE_FILE_NAME)
        val rawJson = """
        {
            "summary": {
                "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                "totalNav": 100000.0,
                "allocatedCapital": 100000.0,
                "deployedCapital": 50000.0,
                "availableCapital": 50000.0,
                "realizedPnl": 0.0,
                "unrealizedPnl": 0.0,
                "totalPnl": 0.0,
                "totalPnlPct": 0.0,
                "todayPnl": 0.0,
                "todayPnlPct": 0.0,
                "activeSlots": 2,
                "maxSlots": 3,
                "lastUpdated": "2026-09-22 12:00:00"
            },
            "serverUrl": "http://localhost:8080",
            "authToken": "test-token",
            "positions": [
                {
                    "positionId": "POS_1",
                    "symbol": "RUNNER_STOCK",
                    "quantity": 100,
                    "entryPrice": 50.0,
                    "currentPrice": 50.0,
                    "marketValue": 5000.0,
                    "unrealizedPnl": 0.0,
                    "unrealizedPnlPct": 0.0,
                    "peakPrice": 50.0,
                    "entryDate": "2026-09-22",
                    "status": "OPEN",
                    "profit_target_override": null
                },
                {
                    "positionId": "POS_2",
                    "symbol": "CAPPED_STOCK",
                    "quantity": 100,
                    "entryPrice": 50.0,
                    "currentPrice": 50.0,
                    "marketValue": 5000.0,
                    "unrealizedPnl": 0.0,
                    "unrealizedPnlPct": 0.0,
                    "peakPrice": 50.0,
                    "entryDate": "2026-09-22",
                    "status": "OPEN",
                    "profit_target_override": 0.35
                }
            ],
            "transactions": []
        }
        """.trimIndent()
        cacheFile.writeText(rawJson)

        val repo = VaralakshmiRepository()
        val loadedPositions = repo.getCachedPositions()
        assertEquals(2, loadedPositions.size)

        val runner = loadedPositions.first { it.symbol == "RUNNER_STOCK" }
        assertFalse("Position with profit_target_override: null must load as uncapped runner (false)", runner.isProfitTargetEnabled)

        val capped = loadedPositions.first { it.symbol == "CAPPED_STOCK" }
        assertTrue("Position with profit_target_override: 0.35 must load as capped (true)", capped.isProfitTargetEnabled)
    }

    @Test
    fun testRapidRepeatedTogglingIntegrity() {
        val repository = VaralakshmiRepository()
        repository.resetToDefaultSeed()
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)

        val initialPos = viewModel.uiState.value.positions.first { it.symbol == "STLNETWORK" }
        assertTrue(initialPos.isProfitTargetEnabled)

        // Rapid 10-toggle stress
        for (i in 1..10) {
            val currentPos = viewModel.uiState.value.positions.first { it.symbol == "STLNETWORK" }
            val expected = (i % 2 == 0) // even index -> true, odd index -> false
            viewModel.toggleProfitTarget(currentPos)
            val updated = viewModel.uiState.value.positions.first { it.symbol == "STLNETWORK" }
            assertEquals("Toggle $i should match expected state", expected, updated.isProfitTargetEnabled)
        }

        // Final state after 10 toggles should be true (even number of toggles)
        val finalPos = viewModel.uiState.value.positions.first { it.symbol == "STLNETWORK" }
        assertTrue(finalPos.isProfitTargetEnabled)
    }

    @Test
    fun testToggleProfitTargetWithBlankPositionIdDoesNotPolluteOtherPositions() {
        // Both positions have blank positionId: ""
        val pos1 = PositionItem(
            positionId = "",
            symbol = "STOCK_A",
            quantity = 100,
            entryPrice = 50.0,
            currentPrice = 50.0,
            marketValue = 5000.0,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 50.0,
            entryDate = "2026-09-22",
            isProfitTargetEnabled = true
        )
        val pos2 = PositionItem(
            positionId = "",
            symbol = "STOCK_B",
            quantity = 100,
            entryPrice = 50.0,
            currentPrice = 50.0,
            marketValue = 5000.0,
            unrealizedPnl = 0.0,
            unrealizedPnlPct = 0.0,
            peakPrice = 50.0,
            entryDate = "2026-09-22",
            isProfitTargetEnabled = true
        )

        val repository = VaralakshmiRepository()
        val viewModel = VaralakshmiViewModel(repository = repository, autoRefresh = false)
        // Inject positions into ViewModel state
        viewModel.setPositionsForTesting(listOf(pos1, pos2))

        // Toggle STOCK_A only
        viewModel.toggleProfitTarget(pos1)

        val updatedA = viewModel.uiState.value.positions.first { it.symbol == "STOCK_A" }
        val updatedB = viewModel.uiState.value.positions.first { it.symbol == "STOCK_B" }

        assertFalse("STOCK_A should be toggled to false", updatedA.isProfitTargetEnabled)
        assertTrue("STOCK_B must NOT be affected even though both had empty positionId", updatedB.isProfitTargetEnabled)
    }

    @Test
    fun testRepositoryUpdateProfitTargetCaseInsensitive() {
        val repository = VaralakshmiRepository()
        repository.resetToDefaultSeed()

        // Update using lowercase "stlnetwork"
        val updated = repository.updateProfitTarget("stlnetwork", false)
        val stl = updated.first { it.symbol.equals("STLNETWORK", ignoreCase = true) }
        assertFalse("Lowercase symbol query must update STLNETWORK to false", stl.isProfitTargetEnabled)

        // Reload from disk to verify persistence
        val reloaded = VaralakshmiRepository().getCachedPositions()
        val reloadedStl = reloaded.first { it.symbol.equals("STLNETWORK", ignoreCase = true) }
        assertFalse("Disk persistence must retain lowercase updated state", reloadedStl.isProfitTargetEnabled)
    }

    @Test
    fun testDiskCacheDeserializationExplicitNullBooleanFallsBackSafely() {
        val cacheFile = File(tempCacheDir, VaralakshmiRepository.CACHE_FILE_NAME)
        val rawJson = """
        {
            "summary": {
                "strategyId": "VARALAKSHMI_ALPHA_SCALE_35",
                "totalNav": 100000.0,
                "allocatedCapital": 100000.0,
                "deployedCapital": 50000.0,
                "availableCapital": 50000.0,
                "realizedPnl": 0.0,
                "unrealizedPnl": 0.0,
                "totalPnl": 0.0,
                "totalPnlPct": 0.0,
                "todayPnl": 0.0,
                "todayPnlPct": 0.0,
                "activeSlots": 1,
                "maxSlots": 3,
                "lastUpdated": "2026-09-22 12:00:00"
            },
            "serverUrl": "http://localhost:8080",
            "authToken": "test-token",
            "positions": [
                {
                    "positionId": "POS_NULL_FLAG",
                    "symbol": "NULLFLAG_STOCK",
                    "quantity": 100,
                    "entryPrice": 50.0,
                    "currentPrice": 50.0,
                    "marketValue": 5000.0,
                    "unrealizedPnl": 0.0,
                    "unrealizedPnlPct": 0.0,
                    "peakPrice": 50.0,
                    "entryDate": "2026-09-22",
                    "status": "OPEN",
                    "isProfitTargetEnabled": null,
                    "profit_target_override": null
                }
            ],
            "transactions": []
        }
        """.trimIndent()
        cacheFile.writeText(rawJson)

        val repo = VaralakshmiRepository()
        val loaded = repo.getCachedPositions()
        assertEquals(1, loaded.size)
        val pos = loaded.first()
        assertFalse("Position with isProfitTargetEnabled: null and profit_target_override: null must be false", pos.isProfitTargetEnabled)
    }
}
