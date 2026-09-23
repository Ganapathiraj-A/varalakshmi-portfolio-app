package com.example.varalakshmiportfolio.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.varalakshmiportfolio.model.StockRecommendationItem
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StockChartDialog(
    recommendation: StockRecommendationItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember(recommendation) {
        val raw = recommendation.historical2mPoints
        val validPoints = raw.filter { !it.price.isNaN() && !it.price.isInfinite() && it.price > 0.0 }
        when {
            validPoints.size >= 2 -> validPoints.sortedBy { it.date }
            validPoints.size == 1 -> {
                val pt = validPoints[0]
                listOf(
                    com.example.varalakshmiportfolio.model.HistoricalPricePoint("2026-07-23", pt.price),
                    pt
                )
            }
            else -> {
                val fallbackPrice = if (recommendation.price.isNaN() || recommendation.price.isInfinite() || recommendation.price <= 0.0) 100.0 else recommendation.price
                listOf(
                    com.example.varalakshmiportfolio.model.HistoricalPricePoint("2026-07-23", fallbackPrice),
                    com.example.varalakshmiportfolio.model.HistoricalPricePoint("2026-09-23", fallbackPrice)
                )
            }
        }
    }

    val isPositive = recommendation.return2mPct >= 0
    val trendColor = if (isPositive) ProfitGreen else LossRed
    val trendBg = if (isPositive) ProfitGreenBg else LossRedBg

    var scrubbedIndex by remember(recommendation.symbol) { mutableStateOf<Int?>(null) }
    var scrubbedPositionX by remember(recommendation.symbol) { mutableStateOf<Float?>(null) }

    val activePoint = remember(scrubbedIndex, points) {
        if (scrubbedIndex != null && scrubbedIndex in points.indices) {
            points[scrubbedIndex!!]
        } else {
            points.lastOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Row 1: Header Ticker, Rank & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentIndigoBg,
                            border = BorderStroke(1.dp, AccentIndigoLight.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "RANK #${recommendation.rank}",
                                color = AccentIndigoLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recommendation.symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Current Price & 2-Month Net Return Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LTP / ENTRY PRICE",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = recommendation.formattedPrice,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = trendBg,
                            border = BorderStroke(1.dp, trendColor.copy(alpha = 0.4f))
                        ) {
                            val sign = if (isPositive) "+" else ""
                            Text(
                                text = "$sign${String.format(Locale.US, "%.1f", recommendation.return2mPct)}% (2M)",
                                color = trendColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldAccentBg,
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Score: ${recommendation.formattedScore}",
                                color = GoldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 3: High/Low Range & Strategy Target/SL Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricTag(
                        label = "2M High",
                        value = "₹" + String.format(Locale.US, "%.1f", recommendation.high2m),
                        color = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTag(
                        label = "2M Low",
                        value = "₹" + String.format(Locale.US, "%.1f", recommendation.low2m),
                        color = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    if (recommendation.targetPrice > 0.0) {
                        MetricTag(
                            label = "Target",
                            value = "₹" + String.format(Locale.US, "%.0f", recommendation.targetPrice),
                            color = AccentIndigoLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (recommendation.stopLossPrice > 0.0) {
                        MetricTag(
                            label = "Stop Loss",
                            value = "₹" + String.format(Locale.US, "%.0f", recommendation.stopLossPrice),
                            color = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrub / Tooltip Status Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = scrubbedIndex != null) {
                            scrubbedIndex = null
                            scrubbedPositionX = null
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = DarkCard,
                    border = BorderStroke(0.8.dp, if (scrubbedIndex != null) AccentIndigoLight.copy(alpha = 0.5f) else DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (scrubbedIndex != null) Icons.AutoMirrored.Filled.ShowChart else Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = if (scrubbedIndex != null) AccentIndigoLight else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scrubbedIndex != null) "INSPECTING:" else "INTERACTIVE SCRUB:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activePoint?.date ?: "N/A",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (scrubbedIndex != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✕",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentIndigoLight
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CLOSE:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activePoint != null) "₹" + String.format(Locale.US, "%.2f", activePoint.price) else "₹0.00",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentIndigoLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Jetpack Compose Canvas 2-Month Line Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0D1117))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                ) {
                    HistoricalPriceCanvas(
                        points = points,
                        currentPrice = recommendation.price,
                        isPositive = isPositive,
                        scrubbedIndex = scrubbedIndex,
                        onScrub = { index, x ->
                            scrubbedIndex = index
                            scrubbedPositionX = x
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Timeline Range Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = points.firstOrNull()?.date ?: "",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Past 2 Months (~45 Trading Days)",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = points.lastOrNull()?.date ?: "",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Prominent Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Close Chart (✕)",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTag(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = BorderStroke(0.8.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoricalPriceCanvas(
    points: List<com.example.varalakshmiportfolio.model.HistoricalPricePoint>,
    currentPrice: Double,
    isPositive: Boolean,
    scrubbedIndex: Int?,
    onScrub: (Int?, Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val curveColor = if (isPositive) ProfitGreen else LossRed

    val minPrice = remember(points) {
        val valid = points.map { it.price }.filter { !it.isNaN() && !it.isInfinite() }
        valid.minOrNull() ?: 0.0
    }
    val maxPrice = remember(points) {
        val valid = points.map { it.price }.filter { !it.isNaN() && !it.isInfinite() }
        valid.maxOrNull() ?: 1.0
    }

    Canvas(
        modifier = modifier
            .pointerInput(points) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val width = size.width
                    if (width > 0 && points.size > 1) {
                        val idx = ((down.position.x / width) * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                        onScrub(idx, down.position.x)
                    }
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        change.consume()
                        if (width > 0 && points.size > 1) {
                            val idx = ((change.position.x / width) * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
                            onScrub(idx, change.position.x.coerceIn(0f, width.toFloat()))
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        if (points.size < 2 || width <= 0f || height <= 0f) return@Canvas

        val topPadding = 28.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val chartHeight = height - topPadding - bottomPadding
        val isFlat = (maxPrice - minPrice) < 0.001
        val priceRange = if (isFlat) 1.0 else (maxPrice - minPrice)

        fun priceToY(p: Double): Float {
            if (p.isNaN() || p.isInfinite()) return topPadding + chartHeight * 0.5f
            if (isFlat) return topPadding + chartHeight * 0.5f
            val ratio = ((p - minPrice) / priceRange).coerceIn(0.0, 1.0).toFloat()
            return topPadding + chartHeight * (1f - ratio)
        }

        // Horizontal dashed guidelines: Peak High, Trough Low, Current Price
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
        val textPaint = Paint().apply {
            color = android.graphics.Color.argb(170, 148, 163, 184)
            textSize = 10.sp.toPx()
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val highY = if (isFlat) topPadding + chartHeight * 0.35f else priceToY(maxPrice)
        val lowY = if (isFlat) topPadding + chartHeight * 0.65f else priceToY(minPrice)
        val curY = if (isFlat) topPadding + chartHeight * 0.5f else priceToY(currentPrice)

        val textMargin = 12.dp.toPx()

        // 1. Peak High Line
        drawLine(
            color = ProfitGreen.copy(alpha = 0.35f),
            start = Offset(0f, highY),
            end = Offset(width, highY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )
        drawContext.canvas.nativeCanvas.drawText(
            "HIGH ₹" + String.format(Locale.US, "%.1f", maxPrice),
            textMargin,
            highY - 4.dp.toPx(),
            textPaint
        )

        // 2. Trough Low Line
        drawLine(
            color = LossRed.copy(alpha = 0.35f),
            start = Offset(0f, lowY),
            end = Offset(width, lowY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )
        drawContext.canvas.nativeCanvas.drawText(
            "LOW ₹" + String.format(Locale.US, "%.1f", minPrice),
            textMargin,
            lowY + 14.dp.toPx(),
            textPaint
        )

        // 3. Current Price Line (only when distinctly positioned from bounds)
        val minGap = 16.dp.toPx()
        if (!isFlat && curY > highY + minGap && curY < lowY - minGap) {
            drawLine(
                color = AccentIndigoLight.copy(alpha = 0.30f),
                start = Offset(0f, curY),
                end = Offset(width, curY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            val nowText = "NOW ₹" + String.format(Locale.US, "%.1f", currentPrice)
            val textW = textPaint.measureText(nowText)
            val textX = (width - textW - textMargin).coerceAtLeast(textMargin)
            drawContext.canvas.nativeCanvas.drawText(
                nowText,
                textX,
                curY - 4.dp.toPx(),
                textPaint
            )
        }

        // Calculate (x, y) coordinates for all points
        val xCoords = FloatArray(points.size)
        val yCoords = FloatArray(points.size)
        val dx = width / (points.size - 1)
        for (i in points.indices) {
            xCoords[i] = i * dx
            yCoords[i] = priceToY(points[i].price)
        }

        // Construct smooth curve path
        val linePath = Path().apply {
            moveTo(xCoords[0], yCoords[0])
            for (i in 1 until points.size) {
                val prevX = xCoords[i - 1]
                val prevY = yCoords[i - 1]
                val currX = xCoords[i]
                val currY = yCoords[i]
                val cX = (prevX + currX) / 2f
                cubicTo(cX, prevY, cX, currY, currX, currY)
            }
        }

        // Smooth gradient fill below curve
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(width, height - 2.dp.toPx())
            lineTo(0f, height - 2.dp.toPx())
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    curveColor.copy(alpha = 0.32f),
                    curveColor.copy(alpha = 0.02f)
                ),
                startY = topPadding,
                endY = height
            )
        )

        // Price curve stroke
        drawPath(
            path = linePath,
            color = curveColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Interactive touch / scrub indicator and floating tooltip
        val activeIdx = scrubbedIndex?.coerceIn(0, points.size - 1)
        if (activeIdx != null) {
            val sx = xCoords[activeIdx]
            val sy = yCoords[activeIdx]

            // Vertical scrub guideline
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(sx, topPadding),
                end = Offset(sx, height - bottomPadding),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // Outer glow circle
            drawCircle(
                color = curveColor.copy(alpha = 0.35f),
                radius = 6.dp.toPx(),
                center = Offset(sx, sy)
            )

            // Inner focus dot
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(sx, sy)
            )

            // Floating dynamic tooltip bubble at touched point
            val activePt = points[activeIdx]
            val tooltipText = "${activePt.date}  •  ₹${String.format(Locale.US, "%.2f", activePt.price)}"
            val tooltipPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11.5.sp.toPx()
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            val tooltipTextWidth = tooltipPaint.measureText(tooltipText)
            val tipPaddingH = 12.dp.toPx()
            val tipBoxW = (tooltipTextWidth + tipPaddingH * 2f).coerceAtMost(width - 16.dp.toPx())
            val tipBoxH = 34.dp.toPx()
            val minLeft = 8.dp.toPx()
            val maxLeft = (width - tipBoxW - minLeft).coerceAtLeast(minLeft)
            val tipBoxLeft = (sx - tipBoxW / 2f).coerceIn(minLeft, maxLeft)
            val offsetAbove = 12.dp.toPx()
            val tipBoxTop = if (sy - tipBoxH - offsetAbove >= 8.dp.toPx()) {
                sy - tipBoxH - offsetAbove
            } else {
                (sy + offsetAbove).coerceAtMost(height - tipBoxH - 8.dp.toPx())
            }

            // Tooltip background pill
            drawRoundRect(
                color = Color(0xFF161B22),
                topLeft = Offset(tipBoxLeft, tipBoxTop),
                size = androidx.compose.ui.geometry.Size(tipBoxW, tipBoxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Tooltip border matching trend color
            drawRoundRect(
                color = curveColor.copy(alpha = 0.85f),
                topLeft = Offset(tipBoxLeft, tipBoxTop),
                size = androidx.compose.ui.geometry.Size(tipBoxW, tipBoxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Tooltip text centered vertically and horizontally
            val textBaselineY = tipBoxTop + (tipBoxH / 2f) - ((tooltipPaint.descent() + tooltipPaint.ascent()) / 2f)
            val textLeft = tipBoxLeft + (tipBoxW - tooltipTextWidth) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                tooltipText,
                textLeft,
                textBaselineY,
                tooltipPaint
            )
        }
    }
}
