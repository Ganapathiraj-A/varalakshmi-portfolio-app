package com.example.varalakshmiportfolio.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.notification.BadgeDisplayMode
import com.example.varalakshmiportfolio.notification.VaraBadgeManager
import com.example.varalakshmiportfolio.util.MarketHours
import com.example.varalakshmiportfolio.widget.VaraProfitWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

class VaraMarketService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var tickCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        VaraBadgeManager.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!VaraBadgeManager.isBadgeServiceEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                VaraBadgeManager.clearNotificationAndBadge(this)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_NOW, ACTION_CHANGE_MODE -> {
                scope.launch { performTickUpdate() }
                return START_STICKY
            }
        }

        // Start foreground immediately with default or cached state
        val repo = VaralakshmiRepository()
        val cachedSummary = repo.getCachedSummary()
        val todayPnl = cachedSummary.todayPnl
        val todayPnlPct = cachedSummary.todayPnlPct
        val totalNav = cachedSummary.totalNav
        val activeCount = repo.getCachedPositions().size
        val isMarketOpen = MarketHours.isMarketHours(Date())
        val mode = VaraBadgeManager.getSavedBadgeMode(this)

        val notification = VaraBadgeManager.buildNotification(
            context = this,
            todayPnl = todayPnl,
            todayPnlPct = todayPnlPct,
            totalNav = totalNav,
            activePositionsCount = activeCount,
            isMarketOpen = isMarketOpen,
            mode = mode,
            tickCount = tickCount
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    VaraBadgeManager.NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    }
                )
            } else {
                startForeground(VaraBadgeManager.NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Fallback standard startForeground
            startForeground(VaraBadgeManager.NOTIFICATION_ID, notification)
        }

        // Launch repeating 1-minute ticker
        startMarketPollingLoop()

        return START_STICKY
    }

    private fun startMarketPollingLoop() {
        scope.launch {
            while (isActive) {
                performTickUpdate()
                tickCount++

                val isMarketOpen = MarketHours.isMarketHours(Date())
                val waitMillis = if (isMarketOpen) {
                    60_000L // 1 minute in market hours
                } else {
                    // Outside market hours, sleep 5 minutes to conserve battery
                    300_000L
                }
                delay(waitMillis)
            }
        }
    }

    private fun performTickUpdate() {
        try {
            val repo = VaralakshmiRepository()
            val cachedSummary = repo.getCachedSummary()
            val cachedPositions = repo.getCachedPositions()

            val todayPnl = if (cachedPositions.isNotEmpty()) {
                cachedPositions.sumOf { it.todayValueChange }
            } else {
                cachedSummary.todayPnl
            }

            val totalNav = cachedSummary.totalNav
            val prevNav = totalNav - todayPnl
            val todayPnlPct = if (prevNav > 0.0) (todayPnl / prevNav) * 100.0 else cachedSummary.todayPnlPct
            val activeCount = cachedPositions.size
            val isMarketOpen = MarketHours.isMarketHours(Date())
            val mode = VaraBadgeManager.getSavedBadgeMode(this)

            VaraBadgeManager.postOrUpdateProfitNotification(
                context = this,
                todayPnl = todayPnl,
                todayPnlPct = todayPnlPct,
                totalNav = totalNav,
                activePositionsCount = activeCount,
                isMarketOpen = isMarketOpen,
                mode = mode,
                tickCount = tickCount
            )

            // Also update home screen widgets if placed
            VaraProfitWidgetProvider.updateAllWidgets(
                context = this,
                todayPnl = todayPnl,
                todayPnlPct = todayPnlPct,
                totalNav = totalNav,
                isMarketOpen = isMarketOpen
            )

            // Push dynamic updates to FloatingScreenerService overlay if active
            FloatingScreenerService.updateData(cachedSummary, cachedPositions)

            // Evaluate Arjun 5L Strategy triggers (Stop Loss & Profit Target auto-sells)
            try {
                com.example.varalakshmiportfolio.data.ArjunStockRepository().evaluateStrategyNow()
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.varalakshmiportfolio.action.START_BADGE_SERVICE"
        const val ACTION_STOP = "com.example.varalakshmiportfolio.action.STOP_BADGE_SERVICE"
        const val ACTION_UPDATE_NOW = "com.example.varalakshmiportfolio.action.UPDATE_BADGE_NOW"
        const val ACTION_CHANGE_MODE = "com.example.varalakshmiportfolio.action.CHANGE_BADGE_MODE"

        fun startService(context: Context) {
            val intent = Intent(context, VaraMarketService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateNow(context: Context) {
            val intent = Intent(context, VaraMarketService::class.java).apply {
                action = ACTION_UPDATE_NOW
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun notifyModeChanged(context: Context, newMode: BadgeDisplayMode) {
            VaraBadgeManager.saveBadgeMode(context, newMode)
            val intent = Intent(context, VaraMarketService::class.java).apply {
                action = ACTION_CHANGE_MODE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VaraMarketService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
