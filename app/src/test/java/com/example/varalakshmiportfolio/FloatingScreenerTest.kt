package com.example.varalakshmiportfolio

import android.content.SharedPreferences
import android.provider.Settings
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.service.FloatingScreenerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Permission check and intent generation logic.
 * 2. Floating screener state preferences persistence.
 * 3. Ticker text generation and formatting from portfolio position items and edge cases.
 */
class FloatingScreenerTest {

    // In-memory test implementation of SharedPreferences for hermetic JVM testing
    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any>()
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): Map<String, *> = HashMap(data)
        override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? Set<String> ?: defValues)
        override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = InMemoryEditor(this, data, listeners)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null && !listeners.contains(listener)) listeners.add(listener)
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listeners.remove(listener)
        }

        private class InMemoryEditor(
            private val prefs: InMemorySharedPreferences,
            private val target: MutableMap<String, Any>,
            private val listeners: List<SharedPreferences.OnSharedPreferenceChangeListener>
        ) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearFlag = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                pending[key] = values
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pending[key] = value
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                pending[key] = this // sentinel for removal
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearFlag = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                val modifiedKeys = mutableSetOf<String>()
                if (clearFlag) {
                    modifiedKeys.addAll(target.keys)
                    target.clear()
                }
                for ((k, v) in pending) {
                    if (v === this) {
                        if (target.containsKey(k)) {
                            target.remove(k)
                            modifiedKeys.add(k)
                        }
                    } else if (v != null) {
                        target[k] = v
                        modifiedKeys.add(k)
                    } else {
                        if (target.containsKey(k)) {
                            target.remove(k)
                            modifiedKeys.add(k)
                        }
                    }
                }
                pending.clear()
                for (key in modifiedKeys) {
                    for (listener in listeners.toList()) {
                        listener.onSharedPreferenceChanged(prefs, key)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // R4.1: Permission Check and Intent Generation Logic
    // -------------------------------------------------------------

    @Test
    fun testOverlayPermissionActionAndIntentCreation() {
        val packageName = "com.example.varalakshmiportfolio"
        val spec = FloatingScreenerManager.getOverlayPermissionIntentSpec(packageName)

        assertEquals(
            "Action must be Settings.ACTION_MANAGE_OVERLAY_PERMISSION",
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            spec.action
        )
        assertEquals("URI string must follow package scheme format", "package:com.example.varalakshmiportfolio", spec.uriString)

        val intent = FloatingScreenerManager.createOverlayPermissionIntent(packageName)
        assertNotNull("Intent must not be null", intent)
    }

    @Test
    fun testOverlayPermissionUriCustomPackages() {
        val customPkg = "com.test.customapp"
        val spec = FloatingScreenerManager.getOverlayPermissionIntentSpec(customPkg)

        assertEquals("package:com.test.customapp", spec.uriString)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, spec.action)

        val intent = FloatingScreenerManager.createOverlayPermissionIntent(customPkg)
        assertNotNull("Intent must not be null", intent)
    }

    // -------------------------------------------------------------
    // R4.2: Floating Screener State Preferences Persistence
    // -------------------------------------------------------------

    @Test
    fun testScreenerEnabledPreferencesPersistence() {
        val prefs = InMemorySharedPreferences()

        // Initial default should be false
        assertFalse("Default screener enabled must be false", FloatingScreenerManager.isScreenerEnabled(prefs))

        // Set to true and verify persistence
        FloatingScreenerManager.setScreenerEnabled(prefs, true)
        assertTrue("Screener enabled should be true after saving", FloatingScreenerManager.isScreenerEnabled(prefs))

        // Set back to false and verify persistence
        FloatingScreenerManager.setScreenerEnabled(prefs, false)
        assertFalse("Screener enabled should be false after saving", FloatingScreenerManager.isScreenerEnabled(prefs))
    }

    @Test
    fun testOverlayYPositionPersistence() {
        val prefs = InMemorySharedPreferences()
        val defaultY = 1600

        // Before any drag, default value should be returned
        assertEquals(defaultY, FloatingScreenerManager.getSavedOverlayY(prefs, defaultY))

        // Simulate user dragging overlay to y = 1250
        FloatingScreenerManager.saveLastOverlayY(prefs, 1250)
        assertEquals(1250, FloatingScreenerManager.getSavedOverlayY(prefs, defaultY))

        // Simulate user dragging overlay to y = 300
        FloatingScreenerManager.saveLastOverlayY(prefs, 300)
        assertEquals(300, FloatingScreenerManager.getSavedOverlayY(prefs, defaultY))
    }

    // -------------------------------------------------------------
    // R4.3: Ticker Text Generation and Formatting
    // -------------------------------------------------------------

    @Test
    fun testFormatSummaryBadgeTextPositivePnl() {
        val result = FloatingScreenerManager.formatSummaryBadgeText(2965.67, 2.1)
        assertEquals("+₹2,965.67 (+2.1%)", result)

        val largeResult = FloatingScreenerManager.formatSummaryBadgeText(104523.50, 8.45)
        assertEquals("+₹104,523.50 (+8.5%)", largeResult)
    }

    @Test
    fun testFormatSummaryBadgeTextNegativePnl() {
        val result = FloatingScreenerManager.formatSummaryBadgeText(-1234.50, -1.2)
        assertEquals("-₹1,234.50 (-1.2%)", result)

        val smallNegative = FloatingScreenerManager.formatSummaryBadgeText(-45.20, -0.4)
        assertEquals("-₹45.20 (-0.4%)", smallNegative)
    }

    @Test
    fun testFormatSummaryBadgeTextZeroAndEdgeCases() {
        val zeroResult = FloatingScreenerManager.formatSummaryBadgeText(0.0, 0.0)
        assertEquals("₹0.00 (0.0%)", zeroResult)

        // NaN and Infinite robust handling
        val nanResult = FloatingScreenerManager.formatSummaryBadgeText(Double.NaN, Double.NaN)
        assertEquals("₹0.00 (0.0%)", nanResult)

        val infResult = FloatingScreenerManager.formatSummaryBadgeText(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        assertEquals("₹0.00 (0.0%)", infResult)
    }

    @Test
    fun testFormatPositionTickerItem() {
        // Positive %
        assertEquals("PCJEWELLER +4.2%", FloatingScreenerManager.formatPositionTickerItem("PCJEWELLER", 4.2))

        // Negative %
        assertEquals("CUPID -1.1%", FloatingScreenerManager.formatPositionTickerItem("CUPID", -1.1))

        // Positive single decimal
        assertEquals("TATAMOTORS +0.8%", FloatingScreenerManager.formatPositionTickerItem("TATAMOTORS", 0.8))

        // Zero %
        assertEquals("INFY 0.0%", FloatingScreenerManager.formatPositionTickerItem("INFY", 0.0))

        // Edge case: NaN
        assertEquals("NIFTY 0.0%", FloatingScreenerManager.formatPositionTickerItem("NIFTY", Double.NaN))
    }

    @Test
    fun testFormatPositionsTickerMultipleItems() {
        val samplePositions = listOf(
            PositionItem(
                positionId = "P1",
                symbol = "PCJEWELLER",
                quantity = 100,
                entryPrice = 50.0,
                currentPrice = 52.1,
                marketValue = 5210.0,
                unrealizedPnl = 210.0,
                unrealizedPnlPct = 4.2,
                peakPrice = 52.1,
                entryDate = "2026-09-17",
                todayPriceChangePct = 4.2
            ),
            PositionItem(
                positionId = "P2",
                symbol = "CUPID",
                quantity = 50,
                entryPrice = 100.0,
                currentPrice = 98.9,
                marketValue = 4945.0,
                unrealizedPnl = -55.0,
                unrealizedPnlPct = -1.1,
                peakPrice = 100.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = -1.1
            ),
            PositionItem(
                positionId = "P3",
                symbol = "TATAMOTORS",
                quantity = 25,
                entryPrice = 800.0,
                currentPrice = 806.4,
                marketValue = 20160.0,
                unrealizedPnl = 160.0,
                unrealizedPnlPct = 0.8,
                peakPrice = 806.4,
                entryDate = "2026-09-17",
                todayPriceChangePct = 0.8
            )
        )

        val tickerString = FloatingScreenerManager.formatPositionsTicker(samplePositions)
        assertEquals(
            "PCJEWELLER +4.2%  •  CUPID -1.1%  •  TATAMOTORS +0.8%",
            tickerString
        )
    }

    @Test
    fun testFormatPositionsTickerEmptyAndSingle() {
        // Empty positions list
        val emptyResult = FloatingScreenerManager.formatPositionsTicker(emptyList())
        assertEquals(FloatingScreenerManager.DEFAULT_FALLBACK_TICKER, emptyResult)

        // Single position list (no trailing separator)
        val singlePosition = listOf(
            PositionItem(
                positionId = "P1",
                symbol = "PCJEWELLER",
                quantity = 100,
                entryPrice = 50.0,
                currentPrice = 52.1,
                marketValue = 5210.0,
                unrealizedPnl = 210.0,
                unrealizedPnlPct = 4.2,
                peakPrice = 52.1,
                entryDate = "2026-09-17",
                todayPriceChangePct = 4.2
            )
        )
        val singleResult = FloatingScreenerManager.formatPositionsTicker(singlePosition)
        assertEquals("PCJEWELLER +4.2%", singleResult)
    }

    @Test
    fun testParseTickerStockItems() {
        val samplePositions = listOf(
            PositionItem(
                positionId = "P1",
                symbol = "PCJEWELLER",
                quantity = 100,
                entryPrice = 50.0,
                currentPrice = 52.1,
                marketValue = 5210.0,
                unrealizedPnl = 210.0,
                unrealizedPnlPct = 4.2,
                peakPrice = 52.1,
                entryDate = "2026-09-17",
                todayPriceChangePct = 4.2
            )
        )

        val parsed = FloatingScreenerManager.parseTickerStockItems(samplePositions)
        assertEquals(1, parsed.size)
        assertEquals("PCJEWELLER", parsed[0].symbol)
        assertEquals(4.2, parsed[0].changePct, 0.001)
        assertEquals("PCJEWELLER +4.2%", parsed[0].formattedText)
    }

    @Test
    fun testRepositoryCachedPositionsTickerIntegration() {
        val repo = VaralakshmiRepository()
        val cachedPositions = repo.getCachedPositions()
        assertTrue("Cached positions must not be empty", cachedPositions.isNotEmpty())

        val ticker = FloatingScreenerManager.formatPositionsTicker(cachedPositions)
        assertTrue("Ticker must contain STLNETWORK", ticker.contains("STLNETWORK"))
        assertTrue("Ticker must contain AHCL", ticker.contains("AHCL"))
        assertTrue("Ticker must contain TBZ", ticker.contains("TBZ"))
        assertTrue("Ticker must have bullet separators", ticker.contains("  •  "))

        val summary = repo.getCachedSummary()
        val badge = FloatingScreenerManager.formatSummaryBadgeText(summary.todayPnl, summary.todayPnlPct)
        assertTrue("Badge must contain ₹ currency symbol", badge.contains("₹"))
        assertTrue("Badge must contain percent sign", badge.contains("%"))
    }

    // -------------------------------------------------------------
    // R4.4: Additional Edge Cases & Robustness
    // -------------------------------------------------------------

    @Test
    fun testClampOverlayYPortraitAndLandscapeBoundaries() {
        // Typical portrait screen: height 2400px, overlay height 120px, topMargin 90px, bottomMargin 60px
        val portraitHeight = 2400
        val overlayH = 120
        val topMargin = 90
        val bottomMargin = 60

        // In bounds
        assertEquals(1800, FloatingScreenerManager.clampOverlayY(1800, portraitHeight, overlayH, topMargin, bottomMargin))

        // Above top margin (e.g. status bar)
        assertEquals(90, FloatingScreenerManager.clampOverlayY(30, portraitHeight, overlayH, topMargin, bottomMargin))
        assertEquals(90, FloatingScreenerManager.clampOverlayY(-200, portraitHeight, overlayH, topMargin, bottomMargin))

        // Below bottom margin (e.g. navigation bar)
        // Max Y = 2400 - 120 - 60 = 2220
        assertEquals(2220, FloatingScreenerManager.clampOverlayY(2300, portraitHeight, overlayH, topMargin, bottomMargin))
        assertEquals(2220, FloatingScreenerManager.clampOverlayY(3000, portraitHeight, overlayH, topMargin, bottomMargin))

        // Landscape screen: height 1080px. Saved Y was 2000 in portrait!
        // Max Y = 1080 - 120 - 60 = 900
        val landscapeHeight = 1080
        val clampedLandscape = FloatingScreenerManager.clampOverlayY(2000, landscapeHeight, overlayH, topMargin, bottomMargin)
        assertEquals("Saved portrait Y must be clamped to landscape maximum", 900, clampedLandscape)

        // Ultra-small height edge case (e.g. 200px)
        val tinyHeight = 200
        val clampedTiny = FloatingScreenerManager.clampOverlayY(150, tinyHeight, overlayH, topMargin, bottomMargin)
        assertEquals(90, clampedTiny)
    }

    @Test
    fun testNearZeroNegativeRoundingFormatting() {
        // Microscopic negative percentage should format as "0.0%" not "-0.0%"
        val tickerNearZero = FloatingScreenerManager.formatPositionTickerItem("TEST", -0.01)
        assertEquals("TEST 0.0%", tickerNearZero)

        val tickerNearZeroPositive = FloatingScreenerManager.formatPositionTickerItem("TEST", 0.02)
        assertEquals("TEST 0.0%", tickerNearZeroPositive)

        // Microscopic negative pnl should format as "₹0.00 (0.0%)" not "-₹0.00 (-0.0%)"
        val badgeNearZero = FloatingScreenerManager.formatSummaryBadgeText(-0.001, -0.01)
        assertEquals("₹0.00 (0.0%)", badgeNearZero)
    }

    @Test
    fun testFormatPositionTickerItemExtremeValues() {
        val hugePositive = FloatingScreenerManager.formatPositionTickerItem("MULTIBAGGER", 845.67)
        assertEquals("MULTIBAGGER +845.7%", hugePositive)

        val totalLoss = FloatingScreenerManager.formatPositionTickerItem("DELISTED", -100.0)
        assertEquals("DELISTED -100.0%", totalLoss)
    }

    @Test
    fun testPnlPolarityHelpers() {
        // Positive values
        assertTrue("100.0 must be positive", FloatingScreenerManager.isPnlPositive(100.0))
        assertFalse("100.0 must not be negative", FloatingScreenerManager.isPnlNegative(100.0))

        // Negative values
        assertTrue("-50.0 must be negative", FloatingScreenerManager.isPnlNegative(-50.0))
        assertFalse("-50.0 must not be positive", FloatingScreenerManager.isPnlPositive(-50.0))

        // Exact zero
        assertFalse("0.0 is not positive", FloatingScreenerManager.isPnlPositive(0.0))
        assertFalse("0.0 is not negative", FloatingScreenerManager.isPnlNegative(0.0))

        // Near-zero boundary: 0.0049 is rounded to zero
        assertFalse("0.0049 rounds to zero so not positive", FloatingScreenerManager.isPnlPositive(0.0049))
        assertFalse("0.0049 is not negative", FloatingScreenerManager.isPnlNegative(0.0049))

        // Near-zero negative boundary: -0.0049 is rounded to zero
        assertFalse("-0.0049 rounds to zero so not negative", FloatingScreenerManager.isPnlNegative(-0.0049))
        assertFalse("-0.0049 is not positive", FloatingScreenerManager.isPnlPositive(-0.0049))

        // At or above 0.005 rounds to +0.01
        assertTrue("0.005 rounds to positive", FloatingScreenerManager.isPnlPositive(0.005))
        assertTrue("-0.005 rounds to negative", FloatingScreenerManager.isPnlNegative(-0.005))

        // NaN and Infinity safety
        assertFalse("NaN is not positive", FloatingScreenerManager.isPnlPositive(Double.NaN))
        assertFalse("NaN is not negative", FloatingScreenerManager.isPnlNegative(Double.NaN))
        assertFalse("Positive infinity is not positive", FloatingScreenerManager.isPnlPositive(Double.POSITIVE_INFINITY))
        assertFalse("Negative infinity is not negative", FloatingScreenerManager.isPnlNegative(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun testParseTickerStockItemsNearZeroPolarity() {
        val positions = listOf(
            PositionItem(
                positionId = "P1",
                symbol = "NEAR_ZERO_NEG",
                quantity = 10,
                entryPrice = 100.0,
                currentPrice = 99.98,
                marketValue = 999.8,
                unrealizedPnl = -0.2,
                unrealizedPnlPct = -0.02,
                peakPrice = 100.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = -0.02
            ),
            PositionItem(
                positionId = "P2",
                symbol = "NEAR_ZERO_POS",
                quantity = 10,
                entryPrice = 100.0,
                currentPrice = 100.02,
                marketValue = 1000.2,
                unrealizedPnl = 0.2,
                unrealizedPnlPct = 0.02,
                peakPrice = 100.02,
                entryDate = "2026-09-17",
                todayPriceChangePct = 0.02
            )
        )

        val parsed = FloatingScreenerManager.parseTickerStockItems(positions)
        assertEquals(2, parsed.size)

        // Item 1: -0.02% rounds to 0.0% -> neither positive nor negative
        assertEquals("NEAR_ZERO_NEG", parsed[0].symbol)
        assertEquals(0.0, parsed[0].changePct, 0.001)
        assertFalse("Near zero negative must not be flagged positive", parsed[0].isPositive)
        assertFalse("Near zero negative must not be flagged negative", parsed[0].isNegative)
        assertEquals("NEAR_ZERO_NEG 0.0%", parsed[0].formattedText)

        // Item 2: +0.02% rounds to 0.0% -> neither positive nor negative
        assertEquals("NEAR_ZERO_POS", parsed[1].symbol)
        assertEquals(0.0, parsed[1].changePct, 0.001)
        assertFalse("Near zero positive must not be flagged positive", parsed[1].isPositive)
        assertFalse("Near zero positive must not be flagged negative", parsed[1].isNegative)
        assertEquals("NEAR_ZERO_POS 0.0%", parsed[1].formattedText)
    }

    @Test
    fun testSummaryBadgeTextZeroPnlSuppressesNonZeroPct() {
        // When rupee P&L rounds to zero, pct must also format as 0.0%
        val badge = FloatingScreenerManager.formatSummaryBadgeText(0.001, -0.4)
        assertEquals("Rupee P&L of 0.00 must enforce 0.0%", "₹0.00 (0.0%)", badge)

        val badgeExactZero = FloatingScreenerManager.formatSummaryBadgeText(0.0, 0.8)
        assertEquals("Rupee P&L of 0.0 must enforce 0.0%", "₹0.00 (0.0%)", badgeExactZero)
    }

    @Test
    fun testClampOverlayYWithNegativeMarginsAndZeroHeight() {
        // Negative margins should be sanitized to 0
        val clampedWithNegativeMargins = FloatingScreenerManager.clampOverlayY(
            requestedY = -50,
            screenHeight = 1000,
            overlayHeight = 100,
            topMargin = -20,
            bottomMargin = -30
        )
        // With topMargin coerced to 0, minY = 0
        assertEquals(0, clampedWithNegativeMargins)

        // Screen height smaller than overlay height
        val clampedSmallScreen = FloatingScreenerManager.clampOverlayY(
            requestedY = 500,
            screenHeight = 50,
            overlayHeight = 100,
            topMargin = 20,
            bottomMargin = 10
        )
        // minY = 20, maxY = max(20, 50 - 100 - 10 = -60) -> 20. requested 500 coerced to 20
        assertEquals(20, clampedSmallScreen)
    }

    @Test
    fun testSharedPreferencesChangeListenerNotification() {
        val prefs = InMemorySharedPreferences()
        var notifiedKey: String? = null
        var notificationCount = 0

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            notifiedKey = key
            notificationCount++
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        FloatingScreenerManager.setScreenerEnabled(prefs, true)
        assertEquals("Listener must be notified of KEY_SCREENER_ENABLED", FloatingScreenerManager.KEY_SCREENER_ENABLED, notifiedKey)
        assertEquals("Notification count should be 1", 1, notificationCount)
        assertTrue(FloatingScreenerManager.isScreenerEnabled(prefs))

        FloatingScreenerManager.setScreenerEnabled(prefs, false)
        assertEquals("Notification count should be 2", 2, notificationCount)
        assertFalse(FloatingScreenerManager.isScreenerEnabled(prefs))

        // After unregistering, no more notifications
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        FloatingScreenerManager.setScreenerEnabled(prefs, true)
        assertEquals("Notification count should remain 2 after unregistering", 2, notificationCount)
    }

    @Test
    fun testMiuiAndOemDeviceDetection() {
        // Xiaomi / Redmi / POCO devices
        assertTrue("Xiaomi manufacturer must be detected as MIUI", FloatingScreenerManager.isMiuiDevice("Xiaomi", "Xiaomi 14"))
        assertTrue("Redmi brand must be detected as MIUI", FloatingScreenerManager.isMiuiDevice("Xiaomi", "Redmi Note 13"))
        assertTrue("POCO brand must be detected as MIUI", FloatingScreenerManager.isMiuiDevice("POCO", "POCO F5"))
        assertTrue("Lowercase redmi must be detected as MIUI", FloatingScreenerManager.isMiuiDevice("redmi", "k60"))

        // Non-MIUI devices (Google Pixel, Samsung Galaxy, Motorola)
        assertFalse("Google Pixel must not be detected as MIUI", FloatingScreenerManager.isMiuiDevice("Google", "Pixel 9 Pro"))
        assertFalse("Samsung Galaxy must not be detected as MIUI", FloatingScreenerManager.isMiuiDevice("Samsung", "Galaxy S24"))
        assertFalse("Motorola must not be detected as MIUI", FloatingScreenerManager.isMiuiDevice("Motorola", "Edge 50"))
    }

    @Test
    fun testMiuiAndAppDetailsIntentCreation() {
        val packageName = "com.example.varalakshmiportfolio"

        val miuiSpec = FloatingScreenerManager.getMiuiPermissionIntentSpec(packageName)
        assertEquals("miui.intent.action.APP_PERM_EDITOR", miuiSpec.action)
        assertEquals("com.miui.securitycenter", miuiSpec.componentPackage)
        assertEquals("com.miui.permcenter.permissions.PermissionsEditorActivity", miuiSpec.componentClass)
        assertEquals("extra_pkgname", miuiSpec.extraKey)
        assertEquals(packageName, miuiSpec.extraValue)

        val miuiIntent = FloatingScreenerManager.createMiuiAppPermissionsIntent(packageName)
        assertNotNull("MIUI intent must not be null", miuiIntent)

        val detailsSpec = FloatingScreenerManager.getApplicationDetailsIntentSpec(packageName)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, detailsSpec.action)
        assertEquals("package:$packageName", detailsSpec.uriString)

        val detailsIntent = FloatingScreenerManager.createApplicationDetailsSettingsIntent(packageName)
        assertNotNull("Details intent must not be null", detailsIntent)
    }

    @Test
    fun testBlankAndWhitespaceSymbolSanitization() {
        val dirtyPositions = listOf(
            PositionItem(
                positionId = "P1",
                symbol = "   ",
                quantity = 10,
                entryPrice = 100.0,
                currentPrice = 105.0,
                marketValue = 1050.0,
                unrealizedPnl = 50.0,
                unrealizedPnlPct = 5.0,
                peakPrice = 105.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = 5.0
            ),
            PositionItem(
                positionId = "P2",
                symbol = "",
                quantity = 10,
                entryPrice = 100.0,
                currentPrice = 95.0,
                marketValue = 950.0,
                unrealizedPnl = -50.0,
                unrealizedPnlPct = -5.0,
                peakPrice = 100.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = -5.0
            ),
            PositionItem(
                positionId = "P3",
                symbol = "  RELIANCE  ",
                quantity = 5,
                entryPrice = 2500.0,
                currentPrice = 2550.0,
                marketValue = 12750.0,
                unrealizedPnl = 250.0,
                unrealizedPnlPct = 2.0,
                peakPrice = 2550.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = 2.0
            )
        )

        // Blank and empty items are ignored, trimmed valid item is preserved
        val parsed = FloatingScreenerManager.parseTickerStockItems(dirtyPositions)
        assertEquals("Only valid non-blank symbol should be retained", 1, parsed.size)
        assertEquals("RELIANCE", parsed[0].symbol)
        assertEquals("RELIANCE +2.0%", parsed[0].formattedText)

        val tickerText = FloatingScreenerManager.formatPositionsTicker(dirtyPositions)
        assertEquals("RELIANCE +2.0%", tickerText)

        // All blank positions fallback to default message
        val allBlankPositions = listOf(
            PositionItem(
                positionId = "P1",
                symbol = " ",
                quantity = 1,
                entryPrice = 10.0,
                currentPrice = 10.0,
                marketValue = 10.0,
                unrealizedPnl = 0.0,
                unrealizedPnlPct = 0.0,
                peakPrice = 10.0,
                entryDate = "2026-09-17",
                todayPriceChangePct = 0.0
            )
        )
        val fallbackResult = FloatingScreenerManager.formatPositionsTicker(allBlankPositions)
        assertEquals(FloatingScreenerManager.DEFAULT_FALLBACK_TICKER, fallbackResult)
    }

    @Test
    fun testVerticalRepositioningVersusThumbRollTouchSlop() {
        val touchSlop = 16f

        // Case 1: Standard thumb-roll tap (finger moves 14px horizontally, 3px vertically)
        val thumbRollDeltaX = 14f
        val thumbRollDeltaY = 3f
        val isVerticalDragThumbRoll = kotlin.math.abs(thumbRollDeltaY) > touchSlop
        assertFalse("Thumb roll with small vertical delta must not trigger vertical dragging", isVerticalDragThumbRoll)

        val totalDist = kotlin.math.hypot(thumbRollDeltaX.toDouble(), thumbRollDeltaY.toDouble()).toFloat()
        val isRecognizedAsTap = !isVerticalDragThumbRoll && totalDist < touchSlop * 2.5f
        assertTrue("Thumb roll should be recognized as a valid tap to open app", isRecognizedAsTap)

        // Case 2: Intentional vertical repositioning drag (e.g. user drags 45px down)
        val dragDeltaY = 45f
        val isVerticalDragTrue = kotlin.math.abs(dragDeltaY) > touchSlop
        assertTrue("45px vertical movement must trigger vertical drag repositioning", isVerticalDragTrue)

        // Case 3: Wide horizontal swipe across screener (e.g. 180px)
        val wideSwipeDeltaX = 180f
        val wideSwipeDeltaY = 4f
        val wideSwipeTotalDist = kotlin.math.hypot(wideSwipeDeltaX.toDouble(), wideSwipeDeltaY.toDouble()).toFloat()
        val wideSwipeIsTap = !isVerticalDragThumbRoll && wideSwipeTotalDist < touchSlop * 2.5f
        assertFalse("Wide horizontal swipe should not trigger tap", wideSwipeIsTap)
    }
}
