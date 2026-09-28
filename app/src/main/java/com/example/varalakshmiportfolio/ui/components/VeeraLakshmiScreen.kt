package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.FnoDailyPnlItem
import com.example.varalakshmiportfolio.model.FnoHistoricalPosition
import com.example.varalakshmiportfolio.model.FnoHistoricalTrade
import com.example.varalakshmiportfolio.model.FnoInstanceSummary
import com.example.varalakshmiportfolio.model.FnoPositionItem
import com.example.varalakshmiportfolio.model.VeeraLakshmiUiState
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun VeeraLakshmiScreen(
    state: VeeraLakshmiUiState,
    onSelectInstance: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentInst = state.currentInstance

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner with Live Pulse & Instant Switcher
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldAccentBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "VeeraLakshmi F&O",
                                tint = GoldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "VEERALAKSHMI F&O",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Multi-Asset Derivatives Quant Engine",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Live / Offline Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.isLiveSync) ProfitGreenBg else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.isLiveSync) ProfitGreen.copy(alpha = 0.5f) else DarkCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isLiveSync) ProfitGreen else GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (state.isLoading) "Syncing..."
                                else if (state.isLiveSync) "Live Server"
                                else "Cached MTM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.isLiveSync) ProfitGreen else GoldAccent
                            )
                        }
                    }
                }

                // Instance Selector Pills (50L, 20L, 15L)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val instances = listOf(
                        Triple("50L", "50L", "Quad-Engine"),
                        Triple("20L", "20L", "Balanced"),
                        Triple("15L", "15L", "Lean & Agile")
                    )
                    instances.forEach { (id, tierName, engineDesc) ->
                        val isSelected = state.selectedInstanceId == id
                        val instSummary = state.instances[id]
                        val isProfit = (instSummary?.unrealizedPnl ?: 0.0) >= 0.0
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectInstance(id) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AccentIndigo.copy(alpha = 0.2f) else DarkCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AccentIndigo else DarkCardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$tierName ($engineDesc)",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (instSummary != null) {
                                        val sign = if (isProfit) "+" else "-"
                                        val absPnl = kotlin.math.abs(instSummary.unrealizedPnl)
                                        String.format(Locale.US, "%s₹%,.0f", sign, absPnl)
                                    } else "—",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Capital & Margin Utilization Dashboard Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: Unrealized P&L Spotlight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UNREALIZED P&L (${currentInst.instanceId})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentInst.formattedUnrealizedPnl,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (currentInst.unrealizedPnl >= 0.0) ProfitGreen else LossRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentInst.unrealizedPnl >= 0.0) ProfitGreenBg else LossRedBg
                            ) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%s%.2f%% margin",
                                        if (currentInst.returnOnMarginPct >= 0.0) "+" else "",
                                        currentInst.returnOnMarginPct
                                    ),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentInst.unrealizedPnl >= 0.0) ProfitGreen else LossRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Open positions count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "POSITIONS",
                                fontSize = 8.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.activePositions.size} Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIndigoLight
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkCardBorder)

                // Margin Utilization Meter
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Margin Blocked: ${currentInst.formattedMarginBlocked}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%% Utilized", currentInst.marginUtilizationPct),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentInst.marginUtilizationPct > 50.0) GoldAccent else ProfitGreen
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (currentInst.marginUtilizationPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (currentInst.marginUtilizationPct > 50.0) GoldAccent else AccentIndigo,
                        trackColor = DarkCard
                    )
                }

                // 3-Metric Mini Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FnoMiniMetric(
                        label = "ALLOCATED",
                        value = currentInst.formattedAllocatedCapital,
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "FREE CASH BUFFER",
                        value = currentInst.formattedCashBuffer,
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "CAPITAL RETURN",
                        value = String.format(Locale.US, "%s%.2f%%", if (currentInst.returnOnCapitalPct >= 0.0) "+" else "", currentInst.returnOnCapitalPct),
                        valueColor = if (currentInst.returnOnCapitalPct >= 0.0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Macro Risk & Circuit Breaker Telemetry Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circuit Breaker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Risk Guard",
                        tint = ProfitGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "CIRCUIT BREAKER",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = state.statusInfo.circuitBreakerTier,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProfitGreen
                        )
                    }
                }

                // India VIX
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "INDIA VIX",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f (%s)", state.statusInfo.vixLevel, state.statusInfo.vixRegime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Daily Step As-of
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DAILY STEP MTM",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "${currentInst.asOfDate} 15:40",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }

        // 4. Active F&O Positions Table / Cards
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ACTIVE F&O POSITIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentIndigoBg
                        ) {
                            Text(
                                text = "${state.activePositions.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIndigoLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "MTM Close",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }

                HorizontalDivider(color = DarkCardBorder)

                if (state.activePositions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active positions for ${currentInst.name}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    state.activePositions.forEach { position ->
                        FnoPositionCard(position = position)
                    }
                }
            }
        }

        // 5. 10-Year Quant Backtest Baseline Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10-YEAR BENCHMARK BASELINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAccentBg
                    ) {
                        Text(
                            text = "Audited 2016-2026",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FnoMiniMetric(
                        label = "10Y CAGR",
                        value = "${state.statusInfo.cagrPct}%",
                        valueColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "SHARPE",
                        value = "${state.statusInfo.sharpeRatio}",
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "WIN RATE",
                        value = "${state.statusInfo.winRatePct}%",
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "2026 YTD",
                        value = "+${state.statusInfo.ytdPct}%",
                        valueColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Daily Profit & Loss History Table
        FnoDailyPnlHistorySection(dailyPnlList = state.dailyPnlHistory)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FnoPositionCard(
    position: FnoPositionItem,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val isShort = position.direction.equals("SHORT", ignoreCase = true)
    val isProfit = position.isProfit

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(8.dp),
        color = if (isExpanded) DarkSurface else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) AccentIndigo.copy(alpha = 0.6f) else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Row 1: Symbol, Direction Pill, Engine, Lots, Unrealized PnL badge, Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Direction badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isShort) LossRedBg else ProfitGreenBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isShort) LossRed.copy(alpha = 0.5f) else ProfitGreen.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = position.direction.uppercase(Locale.US),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isShort) LossRed else ProfitGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = position.symbol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${position.lots} ${if (position.lots == 1) "lot" else "lots"} (${position.quantity} qty)",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Unrealized PnL badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isProfit) ProfitGreenBg else LossRedBg
                    ) {
                        Text(
                            text = position.formattedUnrealizedPnl,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isProfit) ProfitGreen else LossRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Chevron indicator
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse reason" else "Expand reason",
                        tint = if (isExpanded) AccentIndigoLight else TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Row 2: Strategy Engine & Price Trajectory
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = position.strategyEngine,
                    fontSize = 10.sp,
                    color = AccentIndigoLight
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Entry: ${position.formattedEntryPrice}",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = " → ",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "LTP: ${position.formattedCurrentPrice}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            // Row 3: Margin, Stop Loss & Tap prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Margin: ${position.formattedMargin}",
                    fontSize = 9.5.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (position.stopLossPrice > 0.0) String.format(Locale.US, "SL: ₹%,.2f", position.stopLossPrice) else "SL: —",
                        fontSize = 9.5.sp,
                        color = if (position.stopLossPrice > 0.0) GoldAccent else TextMuted
                    )
                    if (!isExpanded) {
                        Text(
                            text = "• Reason ▾",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentIndigoLight.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Expanded Section: Detailed Reason on Why It Was Bought & Trade Mechanics
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = DarkCardBorder)

                    // 1. Detailed Reason Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = AccentIndigoBg.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Lightbulb,
                                        contentDescription = "Why it was entered",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "WHY THIS POSITION WAS ENTERED",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent,
                                        letterSpacing = 0.4.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DarkCard
                                ) {
                                    Text(
                                        text = if (isShort) "SHORT SETUP" else "LONG SETUP",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isShort) LossRed else ProfitGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = position.detailedReason,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // 2. Quantitative Trade Parameters 3-Row Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FnoParamPill(
                            label = "SIGNAL TRIGGER",
                            value = position.effectiveSignalTrigger,
                            modifier = Modifier.weight(1f)
                        )
                        FnoParamPill(
                            label = "TARGET & RR",
                            value = "${position.formattedTargetPrice} (${position.effectiveRiskReward})",
                            valueColor = ProfitGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FnoParamPill(
                            label = "STOP LOSS BUFFER",
                            value = "${position.formattedStopLoss} (${String.format(Locale.US, "%.2f%%", position.stopLossDistancePct)})",
                            valueColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        FnoParamPill(
                            label = "CAPITAL AT RISK",
                            value = "${position.formattedCapitalAtRisk} (Max)",
                            valueColor = LossRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FnoParamPill(
                            label = "EXPIRY / CONTRACT",
                            value = "${position.formattedExpiry} • ${position.instrumentType}",
                            modifier = Modifier.weight(1f)
                        )
                        FnoParamPill(
                            label = "LOT SIZE / QTY",
                            value = "${position.lots} lot(s) (${position.quantity} units)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Execution Tag Footer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚡ Systematic Quant Rule-Based Execution",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "Risk Guard Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FnoParamPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FnoMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = valueColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FnoDailyPnlHistorySection(
    dailyPnlList: List<FnoDailyPnlItem>,
    modifier: Modifier = Modifier
) {
    if (dailyPnlList.isEmpty()) return

    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredList = remember(dailyPnlList, selectedFilter) {
        when (selectedFilter) {
            "WIN" -> dailyPnlList.filter { it.isProfit }
            "LOSS" -> dailyPnlList.filter { !it.isProfit }
            else -> dailyPnlList
        }
    }

    val totalDays = dailyPnlList.size
    val winDays = dailyPnlList.count { it.isProfit }
    val lossDays = totalDays - winDays
    val winRatePct = if (totalDays > 0) (winDays.toDouble() / totalDays.toDouble()) * 100.0 else 0.0
    val netPnl = dailyPnlList.sumOf { it.dailyPnl }
    val avgDailyPnl = if (totalDays > 0) netPnl / totalDays else 0.0
    val bestDay = dailyPnlList.maxOfOrNull { it.dailyPnl } ?: 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "P&L History",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY PROFIT & LOSS HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (netPnl >= 0.0) ProfitGreenBg else LossRedBg
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%% Win Rate", winRatePct),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netPnl >= 0.0) ProfitGreen else LossRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Summary 4-Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FnoMiniMetric(
                    label = "NET P&L",
                    value = String.format(Locale.US, "%s₹%,.0f", if (netPnl >= 0) "+" else "", netPnl),
                    valueColor = if (netPnl >= 0) ProfitGreen else LossRed,
                    modifier = Modifier.weight(1f)
                )
                FnoMiniMetric(
                    label = "WIN / LOSS",
                    value = "${winDays}W / ${lossDays}L",
                    valueColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                FnoMiniMetric(
                    label = "AVG DAY",
                    value = String.format(Locale.US, "%s₹%,.0f", if (avgDailyPnl >= 0) "+" else "", avgDailyPnl),
                    valueColor = if (avgDailyPnl >= 0) ProfitGreen else LossRed,
                    modifier = Modifier.weight(1f)
                )
                FnoMiniMetric(
                    label = "BEST DAY",
                    value = String.format(Locale.US, "+₹%,.0f", bestDay),
                    valueColor = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Tabs (All, Green Days, Red Days)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("ALL", "All Days ($totalDays)", AccentIndigo),
                    Triple("WIN", "Green Days ($winDays)", ProfitGreen),
                    Triple("LOSS", "Red Days ($lossDays)", LossRed)
                ).forEach { (filterKey, label, activeColor) ->
                    val isSelected = selectedFilter == filterKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedFilter = filterKey },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) activeColor.copy(alpha = 0.18f) else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) activeColor else DarkCardBorder
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activeColor else TextSecondary
                            )
                        }
                    }
                }
            }

            // Table Column Headers
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = DarkCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "DAILY P&L",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(1.3f)
                    )
                    Text(
                        text = "RETURN",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "EQUITY",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            // History Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredList.forEach { item ->
                    FnoDailyPnlRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun FnoDailyPnlRow(
    item: FnoDailyPnlItem,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "pnl_row_chevron"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(8.dp),
        color = if (isExpanded) DarkSurface else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) AccentIndigo.copy(alpha = 0.7f) else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Summary Row (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Date & Today Pill / Trades & Positions count
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.formattedDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (item.isToday) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = GoldAccentBg
                            ) {
                                Text(
                                    text = "TODAY",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Text(
                        text = "${item.tradeCount} trades • ${item.positionsCount} pos",
                        fontSize = 8.5.sp,
                        color = TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Column 2: Daily P&L & Status tag
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.formattedDailyPnl,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (item.isProfit) ProfitGreen else LossRed,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (item.isProfit) ProfitGreenBg else LossRedBg
                    ) {
                        Text(
                            text = item.dayStatus,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isProfit) ProfitGreen else LossRed,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1
                        )
                    }
                }

                // Column 3: Return %
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.formattedDailyReturn,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (item.isProfit) ProfitGreen else LossRed,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Return",
                        fontSize = 7.5.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                }

                // Column 4: Total Equity & Details toggle
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = item.formattedTotalEquity,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "Details",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isExpanded) AccentIndigoLight else TextMuted,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse Details" else "Expand Details",
                            tint = if (isExpanded) AccentIndigoLight else TextMuted,
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            // Expanded Breakdown: Trades & Positions
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = DarkCardBorder)

                    // 1. Session Telemetry Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MARGIN BLOCKED: ${String.format(Locale.US, "₹%,.0f", item.marginBlocked)}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = if (item.isProfit) "SESSION PROFIT" else "SESSION DRAWDOWN",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isProfit) ProfitGreen else LossRed
                            )
                        }
                    }

                    // 2. Positions Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POSITIONS HELD ON ${item.formattedDate.uppercase()}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 0.4.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldAccentBg
                            ) {
                                Text(
                                    text = "${item.positions.size} open",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (item.positions.isEmpty()) {
                            Text(
                                text = "No open positions recorded for this session.",
                                fontSize = 9.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            item.positions.forEach { pos ->
                                HistoricalPositionSubCard(pos = pos)
                            }
                        }
                    }

                    // 3. Executed Trades Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXECUTED TRADES & ORDERS (${item.trades.size})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIndigoLight,
                                letterSpacing = 0.4.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentIndigoBg
                            ) {
                                Text(
                                    text = "${item.trades.size} executed",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentIndigoLight,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (item.trades.isEmpty()) {
                            Text(
                                text = "No order fills executed (Positions carried forward).",
                                fontSize = 9.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            item.trades.forEach { trd ->
                                HistoricalTradeSubCard(trade = trd)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalPositionSubCard(
    pos: FnoHistoricalPosition,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Symbol, Direction pill, Engine, Day PnL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (pos.isShort) LossRedBg else ProfitGreenBg
                    ) {
                        Text(
                            text = pos.direction,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pos.isShort) LossRed else ProfitGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = pos.symbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${pos.strategyEngine}",
                        fontSize = 8.5.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = pos.formattedDayPnl,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (pos.isProfit) ProfitGreen else LossRed
                )
            }

            // Row 2: Lots & Qty, Entry Price, Close Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pos.lots} lot (${pos.quantity} units)",
                    fontSize = 8.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Entry: ${pos.formattedEntryPrice}  →  Close: ${pos.formattedClosePrice}",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun HistoricalTradeSubCard(
    trade: FnoHistoricalTrade,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Time, Action Pill, Symbol, Qty, Realized PnL or Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = AccentIndigoBg
                    ) {
                        Text(
                            text = trade.time,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentIndigoLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (trade.isSell) LossRedBg else ProfitGreenBg
                    ) {
                        Text(
                            text = trade.action,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.isSell) LossRed else ProfitGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = trade.symbol,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${trade.quantity} @ ${trade.formattedPrice})",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }

                if (trade.realizedPnl != 0.0) {
                    Text(
                        text = trade.formattedRealizedPnl,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (trade.isProfit) ProfitGreen else LossRed
                    )
                }
            }

            // Row 2: Execution Reason / Trigger
            if (trade.executionReason.isNotBlank()) {
                Text(
                    text = "⚡ ${trade.executionReason}",
                    fontSize = 8.5.sp,
                    color = TextMuted,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

