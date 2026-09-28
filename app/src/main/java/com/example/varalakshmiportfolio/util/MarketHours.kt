package com.example.varalakshmiportfolio.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object MarketHours {
    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    // NSE/BSE Regular Market Trading Hours
    const val MARKET_OPEN_HOUR = 9
    const val MARKET_OPEN_MINUTE = 15
    const val MARKET_CLOSE_HOUR = 15
    const val MARKET_CLOSE_MINUTE = 30

    const val MARKET_OPEN_TOTAL_MINUTES = MARKET_OPEN_HOUR * 60 + MARKET_OPEN_MINUTE  // 555
    const val MARKET_CLOSE_TOTAL_MINUTES = MARKET_CLOSE_HOUR * 60 + MARKET_CLOSE_MINUTE // 930

    /**
     * Returns true if the given date falls on a weekday (Monday through Friday)
     * between 09:15 and 15:30 IST inclusive.
     */
    fun isMarketHours(date: Date = Date()): Boolean {
        val cal = Calendar.getInstance(IST_TIMEZONE).apply { time = date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        // Weekend check (Saturday = 7, Sunday = 1)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        return totalMinutes in MARKET_OPEN_TOTAL_MINUTES..MARKET_CLOSE_TOTAL_MINUTES
    }

    /**
     * Returns true if the given date is a weekday (Monday through Friday).
     */
    fun isWeekday(date: Date = Date()): Boolean {
        val cal = Calendar.getInstance(IST_TIMEZONE).apply { time = date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    /**
     * Returns a human-friendly string describing current market status in IST.
     */
    fun getMarketStatusText(date: Date = Date()): String {
        return if (isMarketHours(date)) {
            "Market Open (09:15 – 15:30 IST)"
        } else {
            "Market Closed"
        }
    }

    /**
     * Calculates the remaining milliseconds until the next 09:15 IST market open.
     */
    fun timeUntilNextMarketOpenMillis(date: Date = Date()): Long {
        val cal = Calendar.getInstance(IST_TIMEZONE).apply { time = date }
        val target = Calendar.getInstance(IST_TIMEZONE).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, MARKET_OPEN_HOUR)
            set(Calendar.MINUTE, MARKET_OPEN_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's 09:15 has already passed, advance by 1 day
        if (cal.timeInMillis >= target.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Skip Saturday and Sunday to find Monday
        while (target.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            target.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        ) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return (target.timeInMillis - cal.timeInMillis).coerceAtLeast(0L)
    }
}
