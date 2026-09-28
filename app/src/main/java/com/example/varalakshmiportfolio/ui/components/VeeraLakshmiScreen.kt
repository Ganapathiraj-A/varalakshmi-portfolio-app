package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FnoPositionCard(
    position: FnoPositionItem,
    modifier: Modifier = Modifier
) {
    val isShort = position.direction.equals("SHORT", ignoreCase = true)
    val isProfit = position.isProfit

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Symbol, Direction Pill, Engine, Lots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Row 3: Margin & Stop Loss
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

                Text(
                    text = if (position.stopLossPrice > 0.0) String.format(Locale.US, "SL: ₹%,.2f", position.stopLossPrice) else "SL: —",
                    fontSize = 9.5.sp,
                    color = if (position.stopLossPrice > 0.0) GoldAccent else TextMuted
                )
            }
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
