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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

                // VeeraLakshmi V3 (₹10L Flagship) Spotlight Card at Top
                val inst10L = state.instances["10L"]
                val isSelected10L = state.selectedInstanceId == "10L"
                val pnl10L = inst10L?.unrealizedPnl ?: 0.0
                val cap10L = inst10L?.allocatedCapital ?: 1000000.0
                val nav10L = cap10L + pnl10L
                val pnlPct10L = if (cap10L > 0.0) (pnl10L / cap10L) * 100.0 else 0.0
                val isProfit10L = pnl10L >= 0.0
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectInstance("10L") },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected10L) AccentIndigo.copy(alpha = 0.25f) else DarkCard,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected10L) 1.5.dp else 1.dp,
                        if (isSelected10L) AccentIndigo else GoldAccent.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected10L) AccentIndigo.copy(alpha = 0.3f) else GoldAccentBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "VeeraLakshmi V3",
                                    tint = if (isSelected10L) AccentIndigoLight else GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "VEERALAKSHMI V3",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected10L) AccentIndigoLight else TextPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ProfitGreenBg
                                    ) {
                                        Text(
                                            text = "₹10L FLAGSHIP",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ProfitGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Text(
                                    text = "Multi-Engine Quant Flagship",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = "NAV ₹" + String.format(Locale.US, "%,.0f", nav10L),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%s₹%,.0f (%s%.2f%%)",
                                    if (isProfit10L) "+" else "-",
                                    kotlin.math.abs(pnl10L),
                                    if (pnlPct10L >= 0) "+" else "",
                                    pnlPct10L
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isProfit10L) ProfitGreen else LossRed,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (isSelected10L) "ACTIVE VIEW" else "TAP TO VIEW",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected10L) AccentIndigoLight else TextMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Capital & Margin Utilization Dashboard Card (Aligned with Vara PortfolioHeaderCard)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Strategy Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "VEERALAKSHMI QUANT (${currentInst.instanceId})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentIndigoLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkCard,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                            ) {
                                Text(
                                    text = "DAY ${currentInst.daysActive}",
                                    color = GoldAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentInst.name} • ${currentInst.formattedDaysActive}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Open positions count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.activePositions.isNotEmpty()) ProfitGreen else GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${state.activePositions.size} Active • ${currentInst.daysActive}D",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Large Portfolio NAV & Cash Invested (Exact same layout as Vara)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "PORTFOLIO VALUE (NAV)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹" + String.format(Locale.US, "%,.2f", currentInst.totalNav),
                            fontSize = if (currentInst.totalNav >= 1000000.0) 22.sp else 28.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "ALLOCATED CAPITAL",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentInst.formattedAllocatedCapital,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Side-by-side Return Badges (Total Return & Available Cash Buffer - Just like Vara!)
                val isTotalPositive = currentInst.unrealizedPnl > 0.0
                val isTotalNegative = currentInst.unrealizedPnl < 0.0
                val totalPnlColor = when {
                    isTotalPositive -> ProfitGreen
                    isTotalNegative -> LossRed
                    else -> TextSecondary
                }
                val totalPnlBg = when {
                    isTotalPositive -> ProfitGreenBg
                    isTotalNegative -> LossRedBg
                    else -> DarkCard
                }
                val totalIcon = when {
                    isTotalPositive -> Icons.AutoMirrored.Filled.TrendingUp
                    isTotalNegative -> Icons.Filled.ArrowDownward
                    else -> Icons.Filled.CheckCircle
                }

                val totalValStr = when {
                    isTotalPositive -> "+₹" + String.format(Locale.US, "%,.2f", currentInst.unrealizedPnl)
                    isTotalNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(currentInst.unrealizedPnl))
                    else -> "₹0.00"
                }
                val totalPctStr = when {
                    isTotalPositive || isTotalNegative -> String.format(Locale.US, "(%+.2f%%)", currentInst.totalPnlPct)
                    else -> "(0.00%)"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Total Return Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = totalPnlBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, totalPnlColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = totalIcon,
                                contentDescription = "Total Return Direction",
                                tint = totalPnlColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "TOTAL RETURN (P&L)",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = totalValStr,
                                    color = totalPnlColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = totalPctStr,
                                    color = totalPnlColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "RoM: " + String.format(Locale.US, "%s%.2f%%", if (currentInst.returnOnMarginPct >= 0) "+" else "", currentInst.returnOnMarginPct),
                                    color = if (currentInst.returnOnMarginPct >= 0) ProfitGreenLight else LossRedLight,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 2. Free Cash Buffer Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Cash Buffer Protection",
                                tint = AccentIndigoLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "AVAILABLE CASH",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "₹" + String.format(Locale.US, "%,.2f", currentInst.cashBuffer),
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%% Utilized", currentInst.marginUtilizationPct),
                                    color = if (currentInst.marginUtilizationPct > 50.0) GoldAccent else TextSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "Margin: ${currentInst.formattedMarginBlocked}",
                                    color = TextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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

                // 4-Metric Mini Grid (Aligned with Vara)
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
                        label = "MARGIN BLOCKED",
                        value = currentInst.formattedMarginBlocked,
                        modifier = Modifier.weight(1f)
                    )
                    FnoMiniMetric(
                        label = "FREE CASH",
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
                    val noTradeReason = state.statusInfo.noTradeReason
                    val isNoTrade = !noTradeReason.isNullOrBlank() || state.statusInfo.status.startsWith("NO TRADE")
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNoTrade) GoldAccentBg else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isNoTrade) GoldAccent.copy(alpha = 0.5f) else DarkCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isNoTrade) Icons.Filled.Info else Icons.Filled.CheckCircle,
                                contentDescription = if (isNoTrade) "No Trade" else "Positions Flat",
                                tint = if (isNoTrade) GoldAccent else AccentIndigoLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isNoTrade) "SESSION STATUS: NO TRADE" else "NO OPEN POSITIONS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNoTrade) GoldAccent else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isNoTrade) {
                                    noTradeReason ?: state.statusInfo.status
                                } else {
                                    "No open positions. Intraday straddles square off daily by 15:15 IST."
                                },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    state.activePositions.forEach { position ->
                        FnoPositionCard(position = position)
                    }
                }
            }
        }

        // Today's Executed Trades Section
        FnoTodayTradesSection(trades = state.todayTrades)

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
            // Row 1: Symbol & Direction Pill on Left, Unrealized PnL & Chevron on Right
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                com.example.varalakshmiportfolio.ui.util.StockLinkHelper.openGoogleFinance(
                                    context = context,
                                    symbol = position.symbol
                                )
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = position.symbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "View Chart on Google Finance",
                            tint = AccentIndigoLight.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Unrealized PnL & % P/L badge (like in Vara)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isProfit) ProfitGreenBg else LossRedBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            (if (isProfit) ProfitGreen else LossRed).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = position.formattedUnrealizedPnl,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isProfit) ProfitGreen else LossRed
                            )
                            Text(
                                text = String.format(Locale.US, "(%s%.2f%%)", if (position.pnlPct >= 0) "+" else "", position.pnlPct),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isProfit) ProfitGreen else LossRed
                            )
                        }
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

            // Row 2: Strategy Engine & Lots on Left, Price Trajectory on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${position.strategyEngine} • ${position.lots} ${if (position.lots == 1) "lot" else "lots"} (${position.quantity} qty)",
                    fontSize = 10.sp,
                    color = AccentIndigoLight,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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

private fun formatPnlCurrency(amount: Double): String {
    return if (amount >= 0) {
        String.format(Locale.US, "+₹%,.2f", amount)
    } else {
        String.format(Locale.US, "-₹%,.2f", Math.abs(amount))
    }
}

internal fun formatFullHistoryForClipboard(
    items: List<FnoDailyPnlItem>,
    winRatePct: Double,
    netPnl: Double,
    winDays: Int,
    lossDays: Int,
    avgDailyPnl: Double,
    bestDay: Double
): String {
    if (items.isEmpty()) {
        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("VEERALAKSHMI V2 (₹3,00,000) - FORWARD PAPER TEST LEDGER")
        sb.appendLine("================================================================================")
        sb.appendLine("Total Sessions : 0 | Awaiting forward paper test sessions starting Monday 2026-10-12")
        sb.appendLine("Allocated Cap  : ₹3,00,000.00")
        sb.appendLine("================================================================================")
        return sb.toString()
    }

    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("VEERALAKSHMI V2 (₹3,00,000) - FORWARD PERFORMANCE HISTORY & LEDGER")
    sb.appendLine("================================================================================")
    sb.appendLine("Total Sessions : ${items.size} | Win Rate: ${String.format(Locale.US, "%.1f%%", winRatePct)} (${winDays}W / ${lossDays}L)")
    sb.appendLine("Net P&L        : ${formatPnlCurrency(netPnl)}")
    sb.appendLine("Avg Daily      : ${formatPnlCurrency(avgDailyPnl)}")
    sb.appendLine("Best Day       : ${String.format(Locale.US, "+₹%,.2f", bestDay)}")
    sb.appendLine("--------------------------------------------------------------------------------")
    sb.appendLine(String.format(Locale.US, "%-12s | %-14s | %-8s | %-6s | %-16s | %-6s | %-9s",
        "DATE", "DAILY P&L", "RETURN", "STATUS", "TOTAL EQUITY", "TRADES", "POSITIONS"))
    sb.appendLine("--------------------------------------------------------------------------------")
    for (item in items) {
        val pnlStr = formatPnlCurrency(item.dailyPnl)
        val retStr = String.format(Locale.US, "%s%.2f%%", if (item.dailyReturnPct >= 0) "+" else "", item.dailyReturnPct)
        val eqStr = String.format(Locale.US, "₹%,.2f", item.totalEquity)
        sb.appendLine(String.format(Locale.US, "%-12s | %-14s | %-8s | %-6s | %-16s | %-6d | %-9d",
            item.date, pnlStr, retStr, item.dayStatus, eqStr, item.tradeCount, item.positionsCount))
    }
    sb.appendLine("================================================================================")
    sb.appendLine("DETAILED DAILY LEDGER (EXECUTED TRADES & POSITIONS HELD)")
    sb.appendLine("================================================================================")
    for (item in items) {
        val pnlStr = formatPnlCurrency(item.dailyPnl)
        val retStr = String.format(Locale.US, "%s%.2f%%", if (item.dailyReturnPct >= 0) "+" else "", item.dailyReturnPct)
        sb.appendLine()
        sb.appendLine("[${item.date}] Net P&L: $pnlStr ($retStr) | Status: ${item.dayStatus} | Total Equity: ${String.format(Locale.US, "₹%,.2f", item.totalEquity)}")

        if (item.trades.isNotEmpty()) {
            sb.appendLine("  • Executed Trades (${item.trades.size}):")
            for (t in item.trades) {
                val trdPnlStr = formatPnlCurrency(t.realizedPnl)
                sb.appendLine("    - ${t.time} ${t.action} ${t.symbol} ${t.lots} lot (${t.quantity} qty) @ ${String.format(Locale.US, "₹%,.2f", t.executionPrice)} | Realized P&L: $trdPnlStr")
                if (t.executionReason.isNotBlank()) {
                    sb.appendLine("      Reason: ${t.executionReason}")
                }
            }
        } else {
            sb.appendLine("  • Executed Trades: None (holding session)")
        }

        if (item.positions.isNotEmpty()) {
            sb.appendLine("  • Positions Held (${item.positions.size}):")
            for (p in item.positions) {
                val pPnlStr = formatPnlCurrency(p.dayPnl)
                sb.appendLine("    - ${p.symbol} ${p.direction} ${p.quantity} units | Entry: ${String.format(Locale.US, "₹%,.2f", p.entryPrice)} | Close: ${String.format(Locale.US, "₹%,.2f", p.closePrice)} | MtM P&L: $pPnlStr (${p.strategyEngine})")
            }
        }
    }
    sb.appendLine()
    sb.appendLine("================================================================================")
    return sb.toString()
}

@Composable
private fun FnoDailyPnlHistorySection(
    dailyPnlList: List<FnoDailyPnlItem>,
    modifier: Modifier = Modifier
) {
    if (dailyPnlList.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Forward Paper Test Ledger",
                    tint = AccentIndigoLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FORWARD PAPER TEST LEDGER (90 SESSIONS)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Awaiting forward test sessions starting Monday 2026-10-12. Real paper fills will be recorded here.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "P&L History",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY PROFIT & LOSS HISTORY",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy Full History Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val fullText = formatFullHistoryForClipboard(
                                    items = dailyPnlList,
                                    winRatePct = winRatePct,
                                    netPnl = netPnl,
                                    winDays = winDays,
                                    lossDays = lossDays,
                                    avgDailyPnl = avgDailyPnl,
                                    bestDay = bestDay
                                )
                                clipboardManager.setText(AnnotatedString(fullText))
                                Toast.makeText(context, "Full history copied to clipboard!", Toast.LENGTH_SHORT).show()
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCopied) ProfitGreenBg else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCopied) ProfitGreen else DarkCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = if (isCopied) "Copied" else "Copy Full History",
                                tint = if (isCopied) ProfitGreen else TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isCopied) "Copied!" else "Copy",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCopied) ProfitGreen else TextSecondary
                            )
                        }
                    }

                    // Win Rate Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (netPnl >= 0.0) ProfitGreenBg else LossRedBg
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%% Win", winRatePct),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netPnl >= 0.0) ProfitGreen else LossRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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

@Composable
private fun FnoTodayTradesSection(
    trades: List<FnoHistoricalTrade>,
    modifier: Modifier = Modifier
) {
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
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AccentIndigoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Today's Trades",
                            tint = AccentIndigoLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S EXECUTED TRADES",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkCard
                ) {
                    Text(
                        text = "${trades.size} TRADES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (trades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trades executed today.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            } else {
                trades.forEach { trade ->
                    HistoricalTradeSubCard(trade = trade)
                }
            }
        }
    }
}

