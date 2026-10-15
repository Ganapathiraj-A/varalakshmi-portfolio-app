package com.example.varalakshmiportfolio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.varalakshmiportfolio.MainActivity
import com.example.varalakshmiportfolio.R
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.util.MarketHours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Foreground service that renders an always-on horizontal floating stock screener
 * overlay above other applications.
 */
class FloatingScreenerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var pollingJob: Job? = null

    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var summaryBadgeContainer: LinearLayout? = null
    private var summaryIndicatorView: TextView? = null
    private var summaryTextView: TextView? = null
    private var tickerMarqueeView: TickerMarqueeView? = null

    private var isScreenOn = true
    private var isDragging = false
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchStartTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    tickerMarqueeView?.stopMarquee()
                    stopPeriodicDataPolling()
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    isScreenOn = true
                    tickerMarqueeView?.startMarquee()
                    startPeriodicDataPolling()
                    scope.launch { refreshDataFromRepository() }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()

        // Register screen on/off receiver to preserve battery
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            FloatingScreenerManager.setScreenerEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }

        // Check overlay permission
        if (!FloatingScreenerManager.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground immediately
        startInForeground()

        // Initialize or update overlay view
        if (overlayContainer == null || (overlayContainer?.isAttachedToWindow == false && overlayContainer?.parent == null)) {
            if (overlayContainer != null) {
                try {
                    windowManager?.removeView(overlayContainer)
                } catch (_: Throwable) {}
                overlayContainer = null
            }
            initOverlayView()
        }

        // Refresh initial data
        scope.launch {
            refreshDataFromRepository()
        }

        // Start background polling loop for live telemetry
        startPeriodicDataPolling()

        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingScreenerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Varalakshmi Live Screener")
            .setContentText("Always-on live stock ticker overlay active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(0, "Close Screener", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun initOverlayView() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels
        val defaultY = (screenHeight * 0.75f).toInt() - dpToPx(24)
        val initialOverlayY = FloatingScreenerManager.getSavedOverlayY(this, defaultY)
        val clampedY = FloatingScreenerManager.clampOverlayY(
            requestedY = initialOverlayY,
            screenHeight = screenHeight,
            overlayHeight = dpToPx(48),
            topMargin = dpToPx(32),
            bottomMargin = dpToPx(24)
        )

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = clampedY
        }
        windowLayoutParams = params

        // 1. Root Container
        val root = object : FrameLayout(this) {
            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                // Intercept all touches on the bar for unified drag and tap handling
                return true
            }
        }.apply {
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            isClickable = true
            isFocusable = true
            contentDescription = "Varalakshmi Live Stock Screener. Tap to open portfolio app."
        }

        // 2. Translucent OLED-Dark Horizontal Bar (#E60D1117)
        val horizontalBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(42)
            )
            background = GradientDrawable().apply {
                setColor(0xE60D1117.toInt()) // ~90% opacity OLED Dark #0D1117
                cornerRadius = 14f * density
                setStroke(dpToPx(1), 0x338B949E) // Subtle dark border
            }
            elevation = 6f * density
        }

        // 3. Portfolio Summary Badge (Left / Fixed)
        val summaryBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            val badgeParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(6)
            }
            layoutParams = badgeParams
            background = GradientDrawable().apply {
                setColor(0x2B21262D) // Dark compact pill background
                cornerRadius = 8f * density
                setStroke(dpToPx(1), 0x228B949E)
            }
        }
        summaryBadgeContainer = summaryBadge

        val indicatorView = TextView(this).apply {
            textSize = 10f
            text = "🟢 "
            setTextColor(0xFF3FB950.toInt())
        }
        summaryIndicatorView = indicatorView
        summaryBadge.addView(indicatorView)

        val summaryText = TextView(this).apply {
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF3FB950.toInt())
            maxLines = 1
            text = "+₹0.00 (+0.0%)"
        }
        summaryTextView = summaryText
        summaryBadge.addView(summaryText)
        horizontalBar.addView(summaryBadge)

        // 4. Subtle Vertical Divider
        val divider = View(this).apply {
            val dividerParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(20)).apply {
                marginStart = dpToPx(6)
                marginEnd = dpToPx(4)
            }
            layoutParams = dividerParams
            setBackgroundColor(0x338B949E)
        }
        horizontalBar.addView(divider)

        // 5. Continuous Auto-Scrolling Marquee (Right / Flex)
        val marquee = TickerMarqueeView(this).apply {
            val marqueeParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
            layoutParams = marqueeParams
        }
        tickerMarqueeView = marquee
        horizontalBar.addView(marquee)

        // 6. Subtle Drag Grip Cue on the Right
        val dragHandle = TextView(this).apply {
            val gripParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
            layoutParams = gripParams
            text = "⋮⋮"
            textSize = 12f
            setTextColor(0x668B949E)
        }
        horizontalBar.addView(dragHandle)

        root.addView(horizontalBar)

        // Attach Touch & Drag Listener with Multi-Touch Support & Thumb-Roll Tolerance
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var activePointerId = MotionEvent.INVALID_POINTER_ID

        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = event.getPointerId(0)
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = SystemClock.uptimeMillis()
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Multi-touch: Maintain current active pointer tracking without jumping
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    val currentRawY = if (pointerIndex != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        event.getRawY(pointerIndex)
                    } else {
                        event.rawY
                    }
                    val currentRawX = if (pointerIndex != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        event.getRawX(pointerIndex)
                    } else {
                        event.rawX
                    }

                    val deltaX = currentRawX - initialTouchX
                    val deltaY = currentRawY - initialTouchY

                    // Screener is a horizontal bar repositioned vertically:
                    // Only begin vertical dragging if vertical displacement strictly exceeds touchSlop
                    if (kotlin.math.abs(deltaY) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val currentScreenHeight = resources.displayMetrics.heightPixels
                        val viewH = root.height.takeIf { it > 0 } ?: dpToPx(48)
                        params.y = FloatingScreenerManager.clampOverlayY(
                            requestedY = initialY + deltaY.toInt(),
                            screenHeight = currentScreenHeight,
                            overlayHeight = viewH,
                            topMargin = dpToPx(32),
                            bottomMargin = dpToPx(24)
                        )
                        try {
                            windowManager?.updateViewLayout(root, params)
                        } catch (_: Throwable) {}
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    if (event.getPointerId(pointerIndex) == activePointerId) {
                        // The active dragging finger was lifted; smoothly switch to another down finger
                        val newIndex = if (pointerIndex == 0) 1 else 0
                        if (newIndex < event.pointerCount) {
                            activePointerId = event.getPointerId(newIndex)
                            initialTouchX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawX(newIndex) else event.rawX
                            initialTouchY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawY(newIndex) else event.rawY
                            initialY = params.y
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = SystemClock.uptimeMillis() - touchStartTime
                    val pointerIndex = event.findPointerIndex(activePointerId).takeIf { it != -1 } ?: 0
                    val finalRawX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawX(pointerIndex) else event.rawX
                    val finalRawY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.getRawY(pointerIndex) else event.rawY
                    val deltaX = kotlin.math.abs(finalRawX - initialTouchX)
                    val deltaY = kotlin.math.abs(finalRawY - initialTouchY)
                    val totalDist = kotlin.math.hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

                    val wasVerticalDrag = isDragging || deltaY > touchSlop
                    if (!wasVerticalDrag && totalDist < touchSlop * 2.5f && duration < 800L) {
                        // Tap to open portfolio application (tolerant to natural mobile thumb roll)
                        root.performClick()
                        launchMainActivity()
                    } else if (wasVerticalDrag) {
                        // Persist user's preferred Y coordinate
                        FloatingScreenerManager.saveLastOverlayY(this@FloatingScreenerService, params.y)
                    }
                    isDragging = false
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(root, params)
            overlayContainer = root
        } catch (_: Throwable) {
            overlayContainer = null
            stopSelf()
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Support Android 14+ (UPSIDE_DOWN_CAKE) background activity starts
        val optionsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.app.ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }.toBundle()
        } else {
            null
        }

        try {
            if (optionsBundle != null) {
                startActivity(intent, optionsBundle)
            } else {
                startActivity(intent)
            }
        } catch (_: Throwable) {
            try {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && optionsBundle != null) {
                    pendingIntent.send(this, 0, null, null, null, null, optionsBundle)
                } else {
                    pendingIntent.send()
                }
            } catch (_: Throwable) {}
        }
    }

    private suspend fun refreshDataFromRepository() {
        withContext(Dispatchers.IO) {
            try {
                val repo = VaralakshmiRepository()
                val summary = repo.getCachedSummary()
                val positions = repo.getCachedPositions()
                withContext(Dispatchers.Main) {
                    updateOverlayData(summary, positions)
                }
            } catch (_: Throwable) {}
        }
    }

    fun updateOverlayData(summary: PortfolioSummary, positions: List<PositionItem>) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                updateOverlayData(summary, positions)
            }
            return
        }

        val todayPnl = if (positions.isNotEmpty()) {
            positions.sumOf { it.todayValueChange }
        } else {
            summary.todayPnl
        }
        val totalNav = summary.totalNav
        val prevNav = totalNav - todayPnl
        val todayPnlPct = if (prevNav > 0.0) (todayPnl / prevNav) * 100.0 else summary.todayPnlPct

        val isPositive = FloatingScreenerManager.isPnlPositive(todayPnl)
        val isNegative = FloatingScreenerManager.isPnlNegative(todayPnl)

        val badgeText = FloatingScreenerManager.formatSummaryBadgeText(todayPnl, todayPnlPct)
        summaryTextView?.text = badgeText
        val color = when {
            isPositive -> 0xFF3FB950.toInt()
            isNegative -> 0xFFF85149.toInt()
            else -> 0xFFC9D1D9.toInt()
        }
        summaryTextView?.setTextColor(color)

        summaryIndicatorView?.text = when {
            isPositive -> "🟢 "
            isNegative -> "🔴 "
            else -> "⚪ "
        }
        summaryIndicatorView?.setTextColor(color)

        // Accessibility TalkBack announcement with live values
        overlayContainer?.contentDescription = "Varalakshmi Live Screener: $badgeText. Tap to open portfolio app."

        tickerMarqueeView?.setPositions(positions)
    }

    private fun startPeriodicDataPolling() {
        stopPeriodicDataPolling()
        if (!isScreenOn) return
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val isMarketOpen = MarketHours.isMarketHours(Date())
                val waitMillis = if (isMarketOpen) 60_000L else 300_000L
                delay(waitMillis)

                if (!FloatingScreenerManager.canDrawOverlays(this@FloatingScreenerService)) {
                    withContext(Dispatchers.Main) {
                        FloatingScreenerManager.setScreenerEnabled(this@FloatingScreenerService, false)
                        stopSelf()
                    }
                    break
                }

                if (isScreenOn) {
                    refreshDataFromRepository()
                }
            }
        }
    }

    private fun stopPeriodicDataPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Clamp overlay position on screen rotation
        val container = overlayContainer ?: return
        val params = windowLayoutParams ?: return
        val currentScreenHeight = resources.displayMetrics.heightPixels
        val viewH = container.height.takeIf { it > 0 } ?: dpToPx(48)
        params.y = FloatingScreenerManager.clampOverlayY(
            requestedY = params.y,
            screenHeight = currentScreenHeight,
            overlayHeight = viewH,
            topMargin = dpToPx(32),
            bottomMargin = dpToPx(24)
        )
        try {
            windowManager?.updateViewLayout(container, params)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance === this) {
            activeInstance = null
        }

        // Stop polling and coroutines
        stopPeriodicDataPolling()
        job.cancel()
        scope.cancel()

        // Unregister screen receiver
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {}

        // Stop marquee animations
        tickerMarqueeView?.stopMarquee()

        // Remove overlay from WindowManager
        overlayContainer?.let { root ->
            try {
                if (root.isAttachedToWindow || root.parent != null) {
                    windowManager?.removeView(root)
                }
            } catch (_: Throwable) {}
        }
        overlayContainer = null
        summaryBadgeContainer = null
        summaryIndicatorView = null
        summaryTextView = null
        tickerMarqueeView = null
        windowLayoutParams = null
        windowManager = null

        // Cancel ongoing notification and stop foreground
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val CHANNEL_ID = "vara_floating_screener_channel"
        const val CHANNEL_NAME = "Varalakshmi Live Screener"
        const val CHANNEL_DESC = "Always-on floating live stock screener overlay"
        const val NOTIFICATION_ID = 20261015

        const val ACTION_START = "com.example.varalakshmiportfolio.action.START_FLOATING_SCREENER"
        const val ACTION_STOP = "com.example.varalakshmiportfolio.action.STOP_FLOATING_SCREENER"

        @Volatile
        var activeInstance: FloatingScreenerService? = null
            private set

        fun startService(context: Context) {
            val intent = Intent(context, FloatingScreenerService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {}
        }

        fun stopService(context: Context) {
            FloatingScreenerManager.setScreenerEnabled(context, false)
            activeInstance?.let { instance ->
                try {
                    instance.stopSelf()
                    return
                } catch (_: Throwable) {}
            }
            val intent = Intent(context, FloatingScreenerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.stopService(intent)
            } catch (_: Throwable) {
                try {
                    context.startService(intent)
                } catch (_: Throwable) {}
            }
        }

        fun updateData(summary: PortfolioSummary, positions: List<PositionItem>) {
            val service = activeInstance ?: return
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                service.updateOverlayData(summary, positions)
            } else {
                service.overlayContainer?.post {
                    service.updateOverlayData(summary, positions)
                } ?: android.os.Handler(android.os.Looper.getMainLooper()).post {
                    service.updateOverlayData(summary, positions)
                }
            }
        }
    }
}
