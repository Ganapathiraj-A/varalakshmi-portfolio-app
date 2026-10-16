package com.example.varalakshmiportfolio.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.varalakshmiportfolio.model.PositionItem
import java.util.Locale

/**
 * Data model for an individual stock item rendered in the live screener marquee.
 */
data class TickerStockItem(
    val symbol: String,
    val changePct: Double,
    val formattedText: String,
    val isPositive: Boolean = false,
    val isNegative: Boolean = false
)

/**
 * Central manager and helper for the Floating Live Screener.
 * Handles overlay permissions, preference persistence, and ticker text formatting.
 */
object FloatingScreenerManager {
    const val PREFS_NAME = "vara_floating_screener_prefs"
    const val KEY_SCREENER_ENABLED = "screener_enabled"
    const val KEY_OVERLAY_Y = "overlay_y"
    const val ACTION_MANAGE_OVERLAY_PERMISSION = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    const val DEFAULT_FALLBACK_TICKER = "VARALAKSHMI LIVE  •  NO ACTIVE POSITIONS"

    data class OverlayPermissionIntentSpec(
        val action: String,
        val uriString: String
    )

    fun getOverlayPermissionIntentSpec(packageName: String): OverlayPermissionIntentSpec {
        return OverlayPermissionIntentSpec(
            action = ACTION_MANAGE_OVERLAY_PERMISSION,
            uriString = getOverlayPermissionUriString(packageName)
        )
    }

    /**
     * Checks if overlay drawing permission is granted by the system/user.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.canDrawOverlays(context)
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }
    }

    /**
     * Builds the Intent to navigate the user to system settings to grant overlay permission.
     */
    fun createOverlayPermissionIntent(context: Context): Intent {
        return createOverlayPermissionIntent(context.packageName)
    }

    fun createOverlayPermissionIntent(packageName: String): Intent {
        val spec = getOverlayPermissionIntentSpec(packageName)
        return Intent(spec.action).apply {
            data = Uri.parse(spec.uriString)
        }
    }

    fun getOverlayPermissionUriString(packageName: String): String = "package:$packageName"

    /**
     * Checks if the device is running MIUI / HyperOS (Xiaomi / Redmi / POCO).
     * On these devices, background activity starts often require OEM permission.
     */
    fun isMiuiDevice(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND): Boolean {
        val m = manufacturer.lowercase(Locale.US)
        val b = brand.lowercase(Locale.US)
        return m.contains("xiaomi") || m.contains("redmi") ||
            b.contains("xiaomi") || b.contains("redmi") || b.contains("poco")
    }

    fun isMiui(): Boolean = isMiuiDevice()

    data class MiuiPermissionIntentSpec(
        val action: String,
        val componentPackage: String,
        val componentClass: String,
        val extraKey: String,
        val extraValue: String
    )

    fun getMiuiPermissionIntentSpec(packageName: String): MiuiPermissionIntentSpec {
        return MiuiPermissionIntentSpec(
            action = "miui.intent.action.APP_PERM_EDITOR",
            componentPackage = "com.miui.securitycenter",
            componentClass = "com.miui.permcenter.permissions.PermissionsEditorActivity",
            extraKey = "extra_pkgname",
            extraValue = packageName
        )
    }

    fun createMiuiAppPermissionsIntent(packageName: String): Intent {
        val spec = getMiuiPermissionIntentSpec(packageName)
        return Intent(spec.action).apply {
            setClassName(spec.componentPackage, spec.componentClass)
            putExtra(spec.extraKey, spec.extraValue)
        }
    }

    data class ApplicationDetailsIntentSpec(
        val action: String,
        val uriString: String
    )

    fun getApplicationDetailsIntentSpec(packageName: String): ApplicationDetailsIntentSpec {
        return ApplicationDetailsIntentSpec(
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            uriString = "package:$packageName"
        )
    }

    fun createApplicationDetailsSettingsIntent(packageName: String): Intent {
        val spec = getApplicationDetailsIntentSpec(packageName)
        return Intent(spec.action).apply {
            data = Uri.parse(spec.uriString)
        }
    }

    /**
     * Preferences persistence for screener toggle state.
     */
    fun isScreenerEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return isScreenerEnabled(prefs)
    }

    fun isScreenerEnabled(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(KEY_SCREENER_ENABLED, false)
    }

    fun setScreenerEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setScreenerEnabled(prefs, enabled)
    }

    fun setScreenerEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENER_ENABLED, enabled).apply()
    }

    /**
     * Y-coordinate persistence for vertical drag positioning.
     */
    fun getSavedOverlayY(context: Context, defaultY: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return getSavedOverlayY(prefs, defaultY)
    }

    fun getSavedOverlayY(prefs: SharedPreferences, defaultY: Int): Int {
        return prefs.getInt(KEY_OVERLAY_Y, defaultY)
    }

    fun saveLastOverlayY(context: Context, y: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        saveLastOverlayY(prefs, y)
    }

    fun saveLastOverlayY(prefs: SharedPreferences, y: Int) {
        prefs.edit().putInt(KEY_OVERLAY_Y, y).apply()
    }

    /**
     * Clamps vertical overlay Y-position within safe screen boundaries.
     * Prevents overlapping top status bar / camera cutout and bottom gesture navigation pill.
     */
    fun clampOverlayY(
        requestedY: Int,
        screenHeight: Int,
        overlayHeight: Int = 48,
        topMargin: Int = 32,
        bottomMargin: Int = 24
    ): Int {
        val safeTop = topMargin.coerceAtLeast(0)
        val safeBottom = bottomMargin.coerceAtLeast(0)
        val safeOverlayH = overlayHeight.coerceAtLeast(0)
        val minY = safeTop
        val maxY = (screenHeight - safeOverlayH - safeBottom).coerceAtLeast(minY)
        return requestedY.coerceIn(minY, maxY)
    }

    /**
     * Formats the Portfolio Summary Badge text on the left of the overlay.
     * Example: "+₹2,965.67 (+2.1%)" or "-₹1,234.50 (-1.2%)"
     */
    fun formatSummaryBadgeText(todayPnl: Double, todayPnlPct: Double): String {
        val safePnl = if (todayPnl.isNaN() || todayPnl.isInfinite()) 0.0 else todayPnl
        val safePct = if (todayPnlPct.isNaN() || todayPnlPct.isInfinite()) 0.0 else todayPnlPct

        val roundedPnl = if (kotlin.math.abs(safePnl) < 0.005) 0.0 else safePnl
        val roundedPct = if (roundedPnl == 0.0) 0.0 else if (kotlin.math.abs(safePct) < 0.05) 0.0 else safePct

        val pnlText = when {
            roundedPnl > 0.0 -> "+₹" + String.format(Locale.US, "%,.2f", roundedPnl)
            roundedPnl < 0.0 -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(roundedPnl))
            else -> "₹0.00"
        }

        val pctSign = if (roundedPct > 0.0) "+" else ""
        val pctText = "($pctSign${String.format(Locale.US, "%.1f", roundedPct)}%)"
        return "$pnlText $pctText"
    }

    /**
     * Checks if today's P&L is strictly positive after rounding paise.
     */
    fun isPnlPositive(todayPnl: Double): Boolean {
        val safePnl = if (todayPnl.isNaN() || todayPnl.isInfinite()) 0.0 else todayPnl
        return safePnl >= 0.005
    }

    /**
     * Checks if today's P&L is strictly negative after rounding paise.
     */
    fun isPnlNegative(todayPnl: Double): Boolean {
        val safePnl = if (todayPnl.isNaN() || todayPnl.isInfinite()) 0.0 else todayPnl
        return safePnl <= -0.005
    }

    /**
     * Formats a single stock ticker item for the scrolling marquee.
     * Example: "PCJEWELLER +4.2%" or "CUPID -1.1%"
     */
    fun formatPositionTickerItem(symbol: String, todayPriceChangePct: Double): String {
        val safePct = if (todayPriceChangePct.isNaN() || todayPriceChangePct.isInfinite()) 0.0 else todayPriceChangePct
        val roundedPct = if (kotlin.math.abs(safePct) < 0.05) 0.0 else safePct
        val sign = if (roundedPct > 0.0) "+" else ""
        val pctStr = "$sign${String.format(Locale.US, "%.1f", roundedPct)}%"
        return "$symbol $pctStr"
    }

    /**
     * Formats a list of position items into a joined continuous ticker string.
     * Example: "PCJEWELLER +4.2%  •  CUPID -1.1%  •  TATAMOTORS +0.8%"
     */
    fun formatPositionsTicker(positions: List<PositionItem>, separator: String = "  •  "): String {
        val validPositions = positions.filter { it.symbol.isNotBlank() }
        if (validPositions.isEmpty()) return DEFAULT_FALLBACK_TICKER
        return validPositions.joinToString(separator) {
            formatPositionTickerItem(it.symbol.trim(), it.todayPriceChangePct)
        }
    }

    /**
     * Parses position items into structured ticker items for custom rendering.
     */
    fun parseTickerStockItems(positions: List<PositionItem>): List<TickerStockItem> {
        return positions.filter { it.symbol.isNotBlank() }.map {
            val trimmedSymbol = it.symbol.trim()
            val safePct = if (it.todayPriceChangePct.isNaN() || it.todayPriceChangePct.isInfinite()) 0.0 else it.todayPriceChangePct
            val roundedPct = if (kotlin.math.abs(safePct) < 0.05) 0.0 else safePct
            TickerStockItem(
                symbol = trimmedSymbol,
                changePct = roundedPct,
                formattedText = formatPositionTickerItem(trimmedSymbol, roundedPct),
                isPositive = roundedPct > 0.0,
                isNegative = roundedPct < 0.0
            )
        }
    }

    /**
     * Parses Arjun recommendation stock items into structured ticker items for custom rendering.
     */
    fun parseArjunTickerStockItems(stocks: List<com.example.varalakshmiportfolio.model.ArjunStockItem>): List<TickerStockItem> {
        val activeStocks = stocks.filter { !it.isClosed }
        val targetList = if (activeStocks.isNotEmpty()) activeStocks else stocks
        if (targetList.isEmpty()) {
            return listOf(
                TickerStockItem(
                    symbol = "ARJUN",
                    changePct = 0.0,
                    formattedText = "ARJUN 5L STRATEGY READY",
                    isPositive = true,
                    isNegative = false
                )
            )
        }
        return targetList.filter { it.symbol.isNotBlank() }.map {
            val trimmedSymbol = it.symbol.trim()
            val safePct = if (it.todayChangePct.isNaN() || it.todayChangePct.isInfinite()) 0.0 else it.todayChangePct
            val roundedPct = if (kotlin.math.abs(safePct) < 0.05) 0.0 else safePct
            TickerStockItem(
                symbol = trimmedSymbol,
                changePct = roundedPct,
                formattedText = formatPositionTickerItem(trimmedSymbol, roundedPct),
                isPositive = roundedPct > 0.0,
                isNegative = roundedPct < 0.0
            )
        }
    }
}
