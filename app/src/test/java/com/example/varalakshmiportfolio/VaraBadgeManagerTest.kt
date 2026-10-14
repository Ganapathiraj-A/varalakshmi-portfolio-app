package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.notification.BadgeDisplayMode
import com.example.varalakshmiportfolio.notification.VaraBadgeManager
import com.example.varalakshmiportfolio.util.MarketHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class VaraBadgeManagerTest {

    @Test
    fun testComputeBadgeNumberProfitThousands() {
        // Zero and negligible profit
        assertEquals(0, VaraBadgeManager.computeBadgeNumber(0.0, 0.0, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(0, VaraBadgeManager.computeBadgeNumber(40.0, 0.05, BadgeDisplayMode.PROFIT_THOUSANDS))

        // Small positive profit (< 1,000) rounds to 1
        assertEquals(1, VaraBadgeManager.computeBadgeNumber(250.0, 0.25, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(1, VaraBadgeManager.computeBadgeNumber(750.0, 0.75, BadgeDisplayMode.PROFIT_THOUSANDS))

        // Normal profits in thousands
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(2000.0, 1.5, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(2450.0, 1.85, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(15, VaraBadgeManager.computeBadgeNumber(14800.0, 10.2, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(14, VaraBadgeManager.computeBadgeNumber(14000.0, 9.8, BadgeDisplayMode.PROFIT_THOUSANDS))

        // Negative profits (integer badge displays absolute magnitude count)
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(-2450.0, -1.85, BadgeDisplayMode.PROFIT_THOUSANDS))
        assertEquals(14, VaraBadgeManager.computeBadgeNumber(-14000.0, -9.8, BadgeDisplayMode.PROFIT_THOUSANDS))
    }

    @Test
    fun testComputeBadgeNumberPercentageGain() {
        // Zero and negligible %
        assertEquals(0, VaraBadgeManager.computeBadgeNumber(0.0, 0.0, BadgeDisplayMode.PERCENTAGE_GAIN))
        assertEquals(0, VaraBadgeManager.computeBadgeNumber(10.0, 0.05, BadgeDisplayMode.PERCENTAGE_GAIN))

        // Small % (< 1.0%) rounds to 1
        assertEquals(1, VaraBadgeManager.computeBadgeNumber(450.0, 0.45, BadgeDisplayMode.PERCENTAGE_GAIN))
        assertEquals(1, VaraBadgeManager.computeBadgeNumber(850.0, 0.85, BadgeDisplayMode.PERCENTAGE_GAIN))

        // Higher percentages
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(2500.0, 2.14, BadgeDisplayMode.PERCENTAGE_GAIN))
        assertEquals(5, VaraBadgeManager.computeBadgeNumber(6000.0, 5.2, BadgeDisplayMode.PERCENTAGE_GAIN))
        assertEquals(6, VaraBadgeManager.computeBadgeNumber(7500.0, 5.8, BadgeDisplayMode.PERCENTAGE_GAIN))

        // Negative percentages
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(-2500.0, -2.14, BadgeDisplayMode.PERCENTAGE_GAIN))
        assertEquals(4, VaraBadgeManager.computeBadgeNumber(-5000.0, -4.0, BadgeDisplayMode.PERCENTAGE_GAIN))
    }

    @Test
    fun testComputeBadgeNumberAlternatingMode() {
        val todayPnl = 14200.0 // 14k
        val todayPnlPct = 2.45  // 2%

        // Even ticks (0, 2, 4) -> PROFIT_THOUSANDS (14)
        assertEquals(14, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 0))
        assertEquals(14, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 2))
        assertEquals(14, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 4))

        // Odd ticks (1, 3, 5) -> PERCENTAGE_GAIN (2)
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 1))
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 3))
        assertEquals(2, VaraBadgeManager.computeBadgeNumber(todayPnl, todayPnlPct, BadgeDisplayMode.ALTERNATING, tickCount = 5))
    }

    @Test
    fun testMarketHoursEvaluation() {
        // Monday 11:30 IST is market hours
        val cal = Calendar.getInstance(MarketHours.IST_TIMEZONE).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 12) // Monday
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        assertTrue("Monday 11:30 IST should be market hours", MarketHours.isMarketHours(cal.time))

        // Saturday 11:30 IST is not market hours
        cal.set(Calendar.DAY_OF_MONTH, 10) // Saturday
        assertFalse("Saturday should not be market hours", MarketHours.isMarketHours(cal.time))

        // Monday 08:30 IST is not market hours (before 09:15)
        cal.set(Calendar.DAY_OF_MONTH, 12)
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 30)
        assertFalse("08:30 IST should not be market hours", MarketHours.isMarketHours(cal.time))

        // Monday 16:00 IST is not market hours (after 15:30)
        cal.set(Calendar.HOUR_OF_DAY, 16)
        cal.set(Calendar.MINUTE, 0)
        assertFalse("16:00 IST should not be market hours", MarketHours.isMarketHours(cal.time))
    }
}
