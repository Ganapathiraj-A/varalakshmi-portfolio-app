package com.example.varalakshmiportfolio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.varalakshmiportfolio.MainActivity
import com.example.varalakshmiportfolio.R
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.util.MarketHours
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaraProfitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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
        val isMarketOpen = MarketHours.isMarketHours(Date())

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, todayPnl, todayPnlPct, totalNav, isMarketOpen)
        }
    }

    companion object {
        private const val COLOR_PROFIT = 0xFF3FB950.toInt() // #3FB950
        private const val COLOR_LOSS = 0xFFF85149.toInt()   // #F85149
        private const val COLOR_MUTED = 0xFF8B949E.toInt()  // #8B949E

        fun updateAllWidgets(
            context: Context,
            todayPnl: Double,
            todayPnlPct: Double,
            totalNav: Double,
            isMarketOpen: Boolean
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, VaraProfitWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds == null || appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, todayPnl, todayPnlPct, totalNav, isMarketOpen)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            todayPnl: Double,
            todayPnlPct: Double,
            totalNav: Double,
            isMarketOpen: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_vara_profit)

            val isPos = todayPnl >= 0.0
            val pnlSign = if (isPos) "+" else "-"
            val pnlPctSign = if (todayPnlPct >= 0.0) "+" else "-"
            val absPnl = kotlin.math.abs(todayPnl)
            val absPct = kotlin.math.abs(todayPnlPct)
            val color = if (isPos) COLOR_PROFIT else COLOR_LOSS

            val pnlText = String.format(Locale.US, "%s₹%,.0f", pnlSign, absPnl)
            val pctText = String.format(Locale.US, "(%s%.2f%%)", pnlPctSign, absPct)

            views.setTextViewText(R.id.widget_profit_val, pnlText)
            views.setTextColor(R.id.widget_profit_val, color)

            views.setTextViewText(R.id.widget_profit_pct, pctText)
            views.setTextColor(R.id.widget_profit_pct, color)

            val timeStr = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = MarketHours.IST_TIMEZONE
            }.format(Date())

            val statusText = if (isMarketOpen) "LIVE" else "CLOSED"
            val statusColor = if (isMarketOpen) COLOR_PROFIT else COLOR_MUTED
            views.setTextViewText(R.id.widget_status, statusText)
            views.setTextColor(R.id.widget_status, statusColor)

            val footerText = String.format(
                Locale.US,
                "NAV: ₹%,.0f • %s IST",
                totalNav, timeStr
            )
            views.setTextViewText(R.id.widget_footer, footerText)

            // Click to launch app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
