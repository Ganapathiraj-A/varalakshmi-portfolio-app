package com.example.varalakshmiportfolio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.notification.BadgeDisplayMode
import com.example.varalakshmiportfolio.notification.VaraBadgeManager
import com.example.varalakshmiportfolio.service.VaraMarketService
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun PortfolioHeaderCard(
    summary: PortfolioSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Strategy Title & Status
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
                            text = "VARA ALPHA",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentIndigoLight,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkCard,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "DAY ${summary.daysActive}",
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
                        text = "80%+ Fast Rotation Compounder • ${summary.formattedDaysActive}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ProfitGreenBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ProfitGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${summary.status} • ${summary.daysActive}D",
                            color = ProfitGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Portfolio NAV & Cash Invested
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
                        text = "₹" + String.format(Locale.US, "%,.2f", summary.totalNav),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
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
                            text = "CASH INVESTED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹" + String.format(Locale.US, "%,.2f", summary.allocatedCapital),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Side-by-side Return Badges (Total Return & Today's NAV Change)
            val isTotalPositive = summary.totalPnl > 0
            val isTotalNegative = summary.totalPnl < 0
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

            val isTodayPositive = summary.todayPnl > 0
            val isTodayNegative = summary.todayPnl < 0
            val todayColor = when {
                isTodayPositive -> ProfitGreen
                isTodayNegative -> LossRed
                else -> TextSecondary
            }
            val todayBg = when {
                isTodayPositive -> ProfitGreenBg
                isTodayNegative -> LossRedBg
                else -> DarkCard
            }
            val todayIcon = when {
                isTodayPositive -> Icons.AutoMirrored.Filled.TrendingUp
                isTodayNegative -> Icons.Filled.ArrowDownward
                else -> Icons.Filled.CheckCircle
            }

            val totalValStr = when {
                isTotalPositive -> "+₹" + String.format(Locale.US, "%,.2f", summary.totalPnl)
                isTotalNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(summary.totalPnl))
                else -> "₹0.00"
            }
            val totalPctStr = when {
                isTotalPositive || isTotalNegative -> String.format(Locale.US, "(%+.2f%%)", summary.totalPnlPct)
                else -> "(0.00%)"
            }

            val cagrVal = summary.effectiveCagr
            val isCagrPositive = cagrVal > 0
            val isCagrNegative = cagrVal < 0
            val cagrStr = when {
                isCagrPositive || isCagrNegative -> String.format(Locale.US, "CAGR: %+.1f%% (%dd)", cagrVal, summary.daysActive)
                else -> String.format(Locale.US, "CAGR: 0.0%% (%dd)", summary.daysActive)
            }

            val isUnrealizedPositive = summary.unrealizedPnl > 0
            val isUnrealizedNegative = summary.unrealizedPnl < 0
            val unrealizedValStr = when {
                isUnrealizedPositive -> "+₹" + String.format(Locale.US, "%,.2f", summary.unrealizedPnl)
                isUnrealizedNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(summary.unrealizedPnl))
                else -> "₹0.00"
            }
            val unrealizedColor = when {
                isUnrealizedPositive -> ProfitGreenLight
                isUnrealizedNegative -> LossRedLight
                else -> TextSecondary
            }

            val todayValStr = when {
                isTodayPositive -> "+₹" + String.format(Locale.US, "%,.2f", summary.todayPnl)
                isTodayNegative -> "-₹" + String.format(Locale.US, "%,.2f", kotlin.math.abs(summary.todayPnl))
                else -> "₹0.00"
            }
            val todayPctStr = when {
                isTodayPositive || isTodayNegative -> String.format(Locale.US, "(%+.2f%%)", summary.todayPnlPct)
                else -> "(0.00%)"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Total Return Pill (with Unrealized P/L & CAGR)
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
                                text = "TOTAL RETURN",
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
                                text = "Unrealized: $unrealizedValStr",
                                color = unrealizedColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = cagrStr,
                                color = totalPnlColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Today's NAV Value Change Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = todayBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, todayColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = todayIcon,
                            contentDescription = "Today's Change Direction",
                            tint = todayColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "TODAY'S CHANGE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = todayValStr,
                                color = todayColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = todayPctStr,
                                color = todayColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "1D RETURN",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metric Grid (Deployed, Available, Realized, Slots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricMiniCard(
                    title = "DEPLOYED",
                    value = "₹" + String.format(Locale.US, "%,.0f", summary.deployedCapital),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "AVAILABLE CASH",
                    value = "₹" + String.format(Locale.US, "%,.0f", summary.availableCapital),
                    subtitle = if (summary.reservedCapital > 0.0) "+₹" + String.format(Locale.US, "%,.0f", summary.reservedCapital) + " res (AMO)" else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricMiniCard(
                    title = "REALIZED P&L",
                    value = "₹" + String.format(Locale.US, "%.2f", summary.realizedPnl),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "ACTIVE SLOTS",
                    value = "${summary.activeSlots} / ${summary.maxSlots} Used",
                    accent = AccentIndigoLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Strategy Parameters Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Target: +35%",
                        fontSize = 10.5.sp,
                        color = ProfitGreenLight,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "🛡️ Cut Loss: -4%",
                        fontSize = 10.5.sp,
                        color = LossRedLight,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "📈 Trail: -8%",
                        fontSize = 10.5.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // App Icon Badge & 1-Minute Live Market Sync Control
            val context = LocalContext.current
            var currentBadgeMode by remember { mutableStateOf(VaraBadgeManager.getSavedBadgeMode(context)) }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val nextMode = when (currentBadgeMode) {
                            BadgeDisplayMode.PROFIT_THOUSANDS -> BadgeDisplayMode.PERCENTAGE_GAIN
                            BadgeDisplayMode.PERCENTAGE_GAIN -> BadgeDisplayMode.ALTERNATING
                            BadgeDisplayMode.ALTERNATING -> BadgeDisplayMode.PROFIT_THOUSANDS
                        }
                        currentBadgeMode = nextMode
                        VaraMarketService.notifyModeChanged(context, nextMode)
                    },
                shape = RoundedCornerShape(10.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔔 ICON BADGE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ProfitGreenBg
                        ) {
                            Text(
                                text = "1M LIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProfitGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentBadgeMode.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentIndigoLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⇄ Tap to switch",
                            fontSize = 8.5.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Strategy Inception & Elapsed Days Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ Strategy Inception: ${summary.inceptionDate}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${summary.daysActive} Days Active",
                    fontSize = 10.sp,
                    color = AccentIndigoLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accent: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = AccentIndigoLight,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
