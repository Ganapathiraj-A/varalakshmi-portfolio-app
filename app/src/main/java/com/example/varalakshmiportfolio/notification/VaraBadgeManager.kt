package com.example.varalakshmiportfolio.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.varalakshmiportfolio.MainActivity
import com.example.varalakshmiportfolio.R
import java.util.Locale

enum class BadgeDisplayMode(val label: String, val shortDesc: String) {
    PROFIT_THOUSANDS("Profit in Thousands (₹k)", "e.g. 2 for ₹2,000, 14 for ₹14,000"),
    PERCENTAGE_GAIN("Percentage Gain (%)", "e.g. 2 for +2.1%, 5 for +5.4%"),
    ALTERNATING("Both (Alternating)", "Rotates between ₹k and % every minute")
}

object VaraBadgeManager {
    const val CHANNEL_ID = "vara_live_profit_badge"
    const val CHANNEL_NAME = "Vara Live Profit & Icon Badge"
    const val CHANNEL_DESC = "Real-time market hours profit updates and dynamic home screen icon badge"
    const val NOTIFICATION_ID = 20261014

    private const val PREFS_NAME = "vara_badge_prefs"
    private const val KEY_BADGE_MODE = "badge_mode"
    private const val KEY_SERVICE_ENABLED = "service_enabled"

    fun getSavedBadgeMode(context: Context): BadgeDisplayMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_BADGE_MODE, BadgeDisplayMode.PROFIT_THOUSANDS.name)
        return try {
            BadgeDisplayMode.valueOf(name ?: BadgeDisplayMode.PROFIT_THOUSANDS.name)
        } catch (_: Exception) {
            BadgeDisplayMode.PROFIT_THOUSANDS
        }
    }

    fun saveBadgeMode(context: Context, mode: BadgeDisplayMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BADGE_MODE, mode.name).apply()
    }

    fun isBadgeServiceEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setBadgeServiceEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    /**
     * Computes the positive integer count for the launcher app icon badge.
     */
    fun computeBadgeNumber(
        todayPnl: Double,
        todayPnlPct: Double,
        mode: BadgeDisplayMode,
        tickCount: Int = 0
    ): Int {
        val effectiveMode = if (mode == BadgeDisplayMode.ALTERNATING) {
            if ((tickCount % 2) == 0) BadgeDisplayMode.PROFIT_THOUSANDS else BadgeDisplayMode.PERCENTAGE_GAIN
        } else {
            mode
        }

        return when (effectiveMode) {
            BadgeDisplayMode.PROFIT_THOUSANDS -> {
                val absPnl = kotlin.math.abs(todayPnl)
                when {
                    absPnl < 50.0 -> 0
                    absPnl < 1000.0 -> 1
                    else -> kotlin.math.round(absPnl / 1000.0).toInt().coerceAtLeast(1)
                }
            }
            BadgeDisplayMode.PERCENTAGE_GAIN -> {
                val absPct = kotlin.math.abs(todayPnlPct)
                when {
                    absPct < 0.1 -> 0
                    absPct < 1.0 -> 1
                    else -> kotlin.math.round(absPct).toInt().coerceAtLeast(1)
                }
            }
            BadgeDisplayMode.ALTERNATING -> 0
        }
    }

    /**
     * Creates or updates the silent notification channel with badge support enabled.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the persistent notification for foreground service & app icon badge.
     */
    fun buildNotification(
        context: Context,
        todayPnl: Double,
        todayPnlPct: Double,
        totalNav: Double,
        activePositionsCount: Int,
        isMarketOpen: Boolean,
        mode: BadgeDisplayMode,
        tickCount: Int = 0
    ): Notification {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val badgeNumber = computeBadgeNumber(todayPnl, todayPnlPct, mode, tickCount)
        val isPos = todayPnl >= 0.0
        val pnlSign = if (isPos) "+" else "-"
        val pnlPctSign = if (todayPnlPct >= 0.0) "+" else "-"
        val absPnl = kotlin.math.abs(todayPnl)
        val absPct = kotlin.math.abs(todayPnlPct)

        val modeLabel = when (mode) {
            BadgeDisplayMode.PROFIT_THOUSANDS -> "Badge: ${badgeNumber}k"
            BadgeDisplayMode.PERCENTAGE_GAIN -> "Badge: ${badgeNumber}%"
            BadgeDisplayMode.ALTERNATING -> if ((tickCount % 2) == 0) "Badge: ${badgeNumber}k" else "Badge: ${badgeNumber}%"
        }

        val title = String.format(
            Locale.US,
            "📈 Vara: %s₹%,.0f (%s%.2f%%)",
            pnlSign, absPnl, pnlPctSign, absPct
        )

        val statusText = if (isMarketOpen) {
            String.format(
                Locale.US,
                "Market Open • %s • %d Active • NAV ₹%,.0f",
                modeLabel, activePositionsCount, totalNav
            )
        } else {
            String.format(
                Locale.US,
                "Market Closed • Day Final • %s • NAV ₹%,.0f",
                modeLabel, totalNav
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\nUpdates every minute in market hours (09:15–15:30 IST)."))
            .setContentIntent(pendingIntent)
            .setNumber(badgeNumber)
            .setOngoing(isMarketOpen)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Posts or updates the live notification and badge count.
     */
    fun postOrUpdateProfitNotification(
        context: Context,
        todayPnl: Double,
        todayPnlPct: Double,
        totalNav: Double,
        activePositionsCount: Int,
        isMarketOpen: Boolean,
        mode: BadgeDisplayMode,
        tickCount: Int = 0
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification = buildNotification(
            context = context,
            todayPnl = todayPnl,
            todayPnlPct = todayPnlPct,
            totalNav = totalNav,
            activePositionsCount = activePositionsCount,
            isMarketOpen = isMarketOpen,
            mode = mode,
            tickCount = tickCount
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun clearNotificationAndBadge(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
