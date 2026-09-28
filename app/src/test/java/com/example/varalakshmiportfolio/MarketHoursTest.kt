package com.example.varalakshmiportfolio

import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.ui.VaralakshmiViewModel
import com.example.varalakshmiportfolio.util.MarketHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MarketHoursTest {

    private fun createIstCalendar(
        year: Int,
        month: Int, // Calendar.MONTH (0-indexed: Calendar.SEPTEMBER = 8)
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int = 0
    ): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testPreMarketHoursOnWeekday() {
        // Monday 28 Sep 2026 at 09:14:00 IST -> Pre-market -> Should be FALSE
        val cal = createIstCalendar(2026, Calendar.SEPTEMBER, 28, 9, 14, 0)
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertFalse(MarketHours.isMarketHours(cal.time))
        assertTrue(MarketHours.isWeekday(cal.time))
        assertEquals("Market Closed", MarketHours.getMarketStatusText(cal.time))
    }

    @Test
    fun testMarketOpeningExactMinute() {
        // Monday 28 Sep 2026 at 09:15:00 IST -> Market Open -> Should be TRUE
        val cal = createIstCalendar(2026, Calendar.SEPTEMBER, 28, 9, 15, 0)
        assertTrue(MarketHours.isMarketHours(cal.time))
        assertEquals("Market Open (09:15 – 15:30 IST)", MarketHours.getMarketStatusText(cal.time))
    }

    @Test
    fun testMiddayTradingHours() {
        // Wednesday 30 Sep 2026 at 12:45:00 IST -> Mid-day -> Should be TRUE
        val cal = createIstCalendar(2026, Calendar.SEPTEMBER, 30, 12, 45, 0)
        assertEquals(Calendar.WEDNESDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertTrue(MarketHours.isMarketHours(cal.time))
    }

    @Test
    fun testMarketClosingMinute() {
        // Friday 02 Oct 2026 at 15:30:00 IST -> Closing minute -> Should be TRUE
        val cal = createIstCalendar(2026, Calendar.OCTOBER, 2, 15, 30, 0)
        assertEquals(Calendar.FRIDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertTrue(MarketHours.isMarketHours(cal.time))
    }

    @Test
    fun testPostMarketHoursOnWeekday() {
        // Friday 02 Oct 2026 at 15:31:00 IST -> Post-market -> Should be FALSE
        val cal = createIstCalendar(2026, Calendar.OCTOBER, 2, 15, 31, 0)
        assertFalse(MarketHours.isMarketHours(cal.time))
        assertEquals("Market Closed", MarketHours.getMarketStatusText(cal.time))
    }

    @Test
    fun testWeekendHoursAreClosed() {
        // Saturday 26 Sep 2026 at 11:30:00 IST -> Weekend -> Should be FALSE
        val satCal = createIstCalendar(2026, Calendar.SEPTEMBER, 26, 11, 30, 0)
        assertEquals(Calendar.SATURDAY, satCal.get(Calendar.DAY_OF_WEEK))
        assertFalse(MarketHours.isMarketHours(satCal.time))
        assertFalse(MarketHours.isWeekday(satCal.time))

        // Sunday 27 Sep 2026 at 14:00:00 IST -> Weekend -> Should be FALSE
        val sunCal = createIstCalendar(2026, Calendar.SEPTEMBER, 27, 14, 0, 0)
        assertEquals(Calendar.SUNDAY, sunCal.get(Calendar.DAY_OF_WEEK))
        assertFalse(MarketHours.isMarketHours(sunCal.time))
        assertFalse(MarketHours.isWeekday(sunCal.time))
    }

    @Test
    fun testTimeUntilNextMarketOpenCalculation() {
        // Sunday 27 Sep 2026 at 12:00:00 IST
        // Next open: Monday 28 Sep 2026 at 09:15:00 IST (21 hours 15 mins later)
        val cal = createIstCalendar(2026, Calendar.SEPTEMBER, 27, 12, 0, 0)
        val remaining = MarketHours.timeUntilNextMarketOpenMillis(cal.time)
        val expectedHours = (21 * 60 + 15) * 60 * 1000L
        assertEquals(expectedHours, remaining)
    }

    @Test
    fun testRepositoryAndViewModelAutoSyncState() {
        val repository = VaralakshmiRepository()
        assertTrue(repository.isAutoSyncEnabled())

        repository.setAutoSyncEnabled(false)
        assertFalse(repository.isAutoSyncEnabled())

        repository.setAutoSyncEnabled(true)
        assertTrue(repository.isAutoSyncEnabled())

        val viewModel = VaralakshmiViewModel(
            repository = repository,
            autoRefresh = false,
            enablePeriodicSync = false
        )

        assertTrue(viewModel.uiState.value.isAutoSyncEnabled)
        assertEquals(15 * 60 * 1000L, viewModel.syncIntervalMillis)

        viewModel.toggleAutoSync(false)
        assertFalse(viewModel.uiState.value.isAutoSyncEnabled)
        assertFalse(repository.isAutoSyncEnabled())

        viewModel.toggleAutoSync(true)
        assertTrue(viewModel.uiState.value.isAutoSyncEnabled)
        assertTrue(repository.isAutoSyncEnabled())
    }
}
