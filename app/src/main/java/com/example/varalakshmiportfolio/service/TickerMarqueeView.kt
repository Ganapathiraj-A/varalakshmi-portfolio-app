package com.example.varalakshmiportfolio.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.example.varalakshmiportfolio.model.PositionItem
import java.util.Locale

/**
 * Custom auto-scrolling ticker marquee view that renders an infinite, seamless loop
 * of stock symbols and their percentage changes with custom colors.
 */
class TickerMarqueeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class RenderItem(
        val symbol: String,
        val changeText: String,
        val isPositive: Boolean,
        val isNegative: Boolean,
        val symbolWidth: Float,
        val spaceWidth: Float,
        val changeWidth: Float,
        val bulletWidth: Float,
        val totalWidth: Float
    )

    private val density = context.resources.displayMetrics.density
    private val baseTextSize = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP,
        11.5f,
        context.resources.displayMetrics
    )

    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF0F6FC.toInt() // OLED off-white
        textSize = baseTextSize
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3FB950.toInt() // Profit green
        textSize = baseTextSize
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF85149.toInt() // Loss red
        textSize = baseTextSize
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B949E.toInt() // Muted text
        textSize = baseTextSize
    }

    private val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6E7681.toInt() // Bullet separator
        textSize = baseTextSize
    }

    private val fontMetrics = Paint.FontMetrics()
    private var cachedBaselineY: Float = 0f

    private var renderItems: List<RenderItem> = emptyList()
    private var totalMarqueeWidth: Float = 0f
    private var scrollXOffset: Float = 0f
    private var speedPxPerSec: Float = 42f * density // Smooth reading speed
    private var lastFrameTime: Long = 0L
    private var isRunning: Boolean = false

    init {
        // Prevent view clipping when drawing loop
        clipToOutline = false
    }

    fun setPositions(positions: List<PositionItem>) {
        val tickerItems = FloatingScreenerManager.parseTickerStockItems(positions)
        val items = mutableListOf<RenderItem>()
        val bulletStr = "  •  "
        val spaceStr = " "
        val bulletWidth = bulletPaint.measureText(bulletStr)
        val spaceWidth = symbolPaint.measureText(spaceStr)

        for (item in tickerItems) {
            val isPos = item.isPositive
            val isNeg = item.isNegative
            val sign = if (isPos) "+" else ""
            val changeText = "$sign${String.format(Locale.US, "%.1f", item.changePct)}%"

            val symWidth = symbolPaint.measureText(item.symbol)
            val paintForChange = when {
                isPos -> greenPaint
                isNeg -> redPaint
                else -> mutedPaint
            }
            val chgWidth = paintForChange.measureText(changeText)
            val totalW = symWidth + spaceWidth + chgWidth + bulletWidth

            items.add(
                RenderItem(
                    symbol = item.symbol,
                    changeText = changeText,
                    isPositive = isPos,
                    isNegative = isNeg,
                    symbolWidth = symWidth,
                    spaceWidth = spaceWidth,
                    changeWidth = chgWidth,
                    bulletWidth = bulletWidth,
                    totalWidth = totalW
                )
            )
        }

        renderItems = items
        val newTotal = items.sumOf { it.totalWidth.toDouble() }.toFloat()
        totalMarqueeWidth = newTotal
        if (newTotal > 0f && scrollXOffset < 0f) {
            scrollXOffset = scrollXOffset % newTotal
        } else {
            scrollXOffset = 0f
        }
        postInvalidate()
    }

    fun startMarquee() {
        if (!isRunning) {
            isRunning = true
            lastFrameTime = SystemClock.uptimeMillis()
            postInvalidateOnAnimation()
        }
    }

    fun stopMarquee() {
        isRunning = false
        lastFrameTime = 0L
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startMarquee()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMarquee()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startMarquee()
        } else {
            stopMarquee()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        symbolPaint.getFontMetrics(fontMetrics)
        cachedBaselineY = (h.toFloat() - (fontMetrics.descent + fontMetrics.ascent)) / 2f
        if (w > 0 && h > 0 && isRunning) {
            lastFrameTime = SystemClock.uptimeMillis()
            postInvalidateOnAnimation()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startMarquee()
        } else {
            stopMarquee()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val baselineY = if (cachedBaselineY > 0f) {
            cachedBaselineY
        } else {
            symbolPaint.getFontMetrics(fontMetrics)
            (viewHeight - (fontMetrics.descent + fontMetrics.ascent)) / 2f
        }

        if (renderItems.isEmpty() || totalMarqueeWidth <= 1f) {
            val fallback = FloatingScreenerManager.DEFAULT_FALLBACK_TICKER
            canvas.drawText(fallback, 8f * density, baselineY, mutedPaint)
            return
        }

        // Advance animation
        val now = SystemClock.uptimeMillis()
        if (isRunning) {
            if (lastFrameTime > 0L) {
                val dt = (now - lastFrameTime) / 1000f
                scrollXOffset -= speedPxPerSec * dt.coerceIn(0f, 0.1f)
                if (totalMarqueeWidth > 0f) {
                    while (scrollXOffset <= -totalMarqueeWidth) {
                        scrollXOffset += totalMarqueeWidth
                    }
                }
            }
            lastFrameTime = now
        }

        // Draw seamless looping sequence, skipping off-screen items to save CPU
        var currentX = scrollXOffset
        val bulletStr = BULLET_SEPARATOR
        while (currentX < viewWidth) {
            for (item in renderItems) {
                if (item.totalWidth <= 0f) continue
                val itemEndX = currentX + item.totalWidth
                if (itemEndX > 0f && currentX < viewWidth) {
                    // 1. Symbol
                    canvas.drawText(item.symbol, currentX, baselineY, symbolPaint)
                    val symRight = currentX + item.symbolWidth + item.spaceWidth

                    // 2. Change %
                    val paint = when {
                        item.isPositive -> greenPaint
                        item.isNegative -> redPaint
                        else -> mutedPaint
                    }
                    canvas.drawText(item.changeText, symRight, baselineY, paint)
                    val chgRight = symRight + item.changeWidth

                    // 3. Bullet Separator
                    canvas.drawText(bulletStr, chgRight, baselineY, bulletPaint)
                }
                currentX += item.totalWidth
            }
        }

        if (isRunning) {
            postInvalidateOnAnimation()
        }
    }

    companion object {
        const val BULLET_SEPARATOR = "  •  "
    }
}
