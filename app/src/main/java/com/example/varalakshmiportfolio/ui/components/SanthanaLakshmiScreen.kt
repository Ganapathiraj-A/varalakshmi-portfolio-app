package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
import com.example.varalakshmiportfolio.model.RecommendationHistorySummary
import com.example.varalakshmiportfolio.model.ActiveIpoApplication
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.IpoActionType
import com.example.varalakshmiportfolio.model.PastIpoApplication
import com.example.varalakshmiportfolio.model.SanthanaLakshmiSummary
import com.example.varalakshmiportfolio.model.SanthanaLakshmiTradeItem
import com.example.varalakshmiportfolio.model.SanthanaLakshmiUiState
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.util.StockLinkHelper
import java.util.Locale

@Composable
fun SanthanaLakshmiScreen(
    state: SanthanaLakshmiUiState,
    onSelectFilter: (String) -> Unit,
    onSelectTradeDetail: (SanthanaLakshmiTradeItem?) -> Unit,
    onRefresh: () -> Unit,
    onConfirmMandate: (String) -> Unit = {},
    onDismissAlert: (String) -> Unit = {},
    onUpdateAllotmentStatus: (String, String) -> Unit = { _, _ -> },
    pastRecommendations: List<HistoricalRecommendationItem> = emptyList(),
    recommendationSummary: RecommendationHistorySummary = RecommendationHistorySummary(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Strategy & Capital Card
        SanthanaHeroCard(
            summary = state.summary,
            isLoading = state.isLoading,
            onRefresh = onRefresh
        )

        // 2. Regime Gating & Market Climate Engine Card
        SanthanaRegimeCard(summary = state.summary)

        // 3. Active IPO Action Signals Section
        val appliedSymbols = remember(state.activeApplications) {
            state.activeApplications.map { it.symbol.uppercase().trim() }.toSet()
        }
        SanthanaActiveSignalsSection(
            activeAlerts = state.activeAlerts,
            appliedSymbols = appliedSymbols,
            onDismiss = onDismissAlert
        )

        // 4. Active IPO Applications & Auto-Sell Pipeline
        if (state.activeApplications.isNotEmpty()) {
            SanthanaActiveApplicationsSection(
                applications = state.activeApplications,
                onConfirmMandate = onConfirmMandate,
                onUpdateAllotmentStatus = onUpdateAllotmentStatus
            )
        }

        // 5. Audited Historical IPO Trade Ledger Table
        SanthanaTradeLedgerSection(
            trades = state.filteredTrades,
            totalAll = state.realHistoricalTrades.size,
            totalWins = state.totalWins,
            totalDefense = state.totalDefense,
            selectedFilter = state.selectedFilter,
            onSelectFilter = onSelectFilter,
            onTradeClick = { onSelectTradeDetail(it) }
        )

        // 6. Past IPO Applications & Allotment Outcomes Section
        SanthanaPastApplicationsSection(
            pastApplications = state.realPastApplications,
            totalAllotted = state.pastAllottedCount,
            totalNotAllotted = state.pastNotAllottedCount,
            totalUnblockedCapital = state.pastTotalUnblockedCapital,
            totalRealizedProfit = state.pastTotalRealizedListingProfit
        )

        // 7. Past Recommendations Section (Interactive Track Record with Google Finance link)
        if (pastRecommendations.isNotEmpty()) {
            PastRecommendationsSection(
                history = pastRecommendations,
                summary = recommendationSummary,
                title = "PAST RECOMMENDATIONS",
                subtitle = "Past 3 Months Track Record • Tap ticker for Google Finance"
            )
        }

        // 8. Strategy 3-Pillar Architecture Card
        SanthanaArchitectureAccordion()
    }

    // Trade Detail Inspection Dialog
    state.selectedTradeDetail?.let { trade ->
        SanthanaTradeDetailDialog(
            trade = trade,
            onDismiss = { onSelectTradeDetail(null) }
        )
    }
}

@Composable
private fun SanthanaHeroCard(
    summary: SanthanaLakshmiSummary,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
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
            // Header Row: Logo, Title, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B1E48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌸", fontSize = 18.sp)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SanthanaLakshmi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ProfitGreenBg
                            ) {
                                Text(
                                    text = "ACTIVE • ${summary.daysActive}D",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Regime-Gated IPO Application • ${summary.formattedDaysActive}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh SanthanaLakshmi",
                        tint = if (isLoading) AccentIndigo else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = DarkCardBorder)

            // Capital Breakdown Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2D3A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TOTAL ALLOCATED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.formattedAllocatedCapital,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DEPLOYED (RISK)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.formattedDeployedCapital,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SWEPT (+6.5%)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.formattedSweptCashBalance,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Key Quant Performance Metrics (6-card Grid)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "LIFETIME CAGR",
                        value = summary.formattedLifetimeCagr,
                        valueColor = ProfitGreen,
                        subtitle = "Audited Net",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "WIN RATE",
                        value = summary.formattedWinRate,
                        valueColor = ProfitGreen,
                        subtitle = "${summary.totalOfferingsApplied} Applied",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "AVG LISTING GAIN",
                        value = summary.formattedAvgListingGain,
                        valueColor = ProfitGreen,
                        subtitle = "Listing Day",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "PROFIT FACTOR",
                        value = summary.formattedProfitFactor,
                        valueColor = AccentIndigo,
                        subtitle = "Gross Gain/Loss",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "MAX DRAWDOWN",
                        value = summary.formattedMaxDrawdown,
                        valueColor = TextPrimary,
                        subtitle = "Peak-to-Trough",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "SHARPE RATIO",
                        value = String.format(Locale.US, "%.2f", summary.sharpeRatio),
                        valueColor = GoldAccent,
                        subtitle = "Annualized",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    valueColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DarkBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SanthanaRegimeCard(summary: SanthanaLakshmiSummary) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Regime Gating",
                        tint = AccentIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "REGIME GATING TELEMETRY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ProfitGreenBg
                ) {
                    Text(
                        text = "🟢 ${summary.currentMarketRegime} REGIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = summary.regimeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = DarkBackground
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Rule",
                        tint = AccentIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Active Rule: ${summary.gatingRuleActive}",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SanthanaActiveSignalsSection(
    activeAlerts: List<IpoActionNotification>,
    appliedSymbols: Set<String> = emptySet(),
    onDismiss: (String) -> Unit = {}
) {
    val validAlerts = remember(activeAlerts, appliedSymbols) {
        activeAlerts
            .filterNot { it.isDismissed }
            .filter { it.isDeadlineTodayOrFuture }
            .filterNot { appliedSymbols.contains(it.symbol.uppercase().trim()) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE IPO ACTION SIGNALS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (validAlerts.isNotEmpty()) ProfitGreenBg else DarkCardBorder
            ) {
                Text(
                    text = if (validAlerts.isNotEmpty()) "${validAlerts.size} ACTIONABLE" else "IDLE PIPELINE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (validAlerts.isNotEmpty()) ProfitGreen else TextMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        if (validAlerts.isNotEmpty()) {
            validAlerts.forEach { alert ->
                ActiveIpoAlertCard(alert = alert, onDismiss = onDismiss)
            }
        } else {
            // Idle Pipeline Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldAccentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Savings,
                            contentDescription = "Cash Swept",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛡️ 100% Capital Swept into Liquid ETFs (+6.5% p.a.)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No open IPOs currently meet institutional QIB thresholds. ₹1,00,000 cash balance is safely generating daily liquid yield with zero market exposure.",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveIpoAlertCard(
    alert: IpoActionNotification,
    onDismiss: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentIndigoBg.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, AccentIndigoLight.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                StockLinkHelper.openGoogleFinance(
                                    context = context,
                                    symbol = alert.symbol
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = alert.symbol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "View on Google Finance",
                                tint = AccentIndigoLight,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ProfitGreenBg
                    ) {
                        Text(
                            text = alert.actionType.name.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Deadline: ${alert.closeDeadline}",
                        fontSize = 10.sp,
                        color = LossRed,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onDismiss(alert.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Alert",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = alert.headline,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Text(
                text = alert.message,
                fontSize = 10.5.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onDismiss(alert.id) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.32f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dismiss",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = { uriHandler.openUri(alert.deepLinkUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.68f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply on Zerodha (${alert.formattedTotalAmount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SanthanaTradeLedgerSection(
    trades: List<SanthanaLakshmiTradeItem>,
    totalAll: Int,
    totalWins: Int,
    totalDefense: Int,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onTradeClick: (SanthanaLakshmiTradeItem) -> Unit
) {
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
            // Header & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL IPO TRADE LEDGER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "${trades.size} TRADES",
                    fontSize = 9.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { onSelectFilter("ALL") },
                    label = { Text("All ($totalAll)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentIndigo,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = selectedFilter == "WINS",
                    onClick = { onSelectFilter("WINS") },
                    label = { Text("Wins ($totalWins)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ProfitGreen,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.height(28.dp)
                )
                FilterChip(
                    selected = selectedFilter == "DEFENSE",
                    onClick = { onSelectFilter("DEFENSE") },
                    label = { Text("Defense ($totalDefense)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6E7681),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }

            HorizontalDivider(color = DarkCardBorder)

            if (trades.isNotEmpty()) {
                // Table Column Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "OFFERING / DATES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.weight(1.3f)
                    )
                    Text(
                        text = "ISSUE → EXIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "GAIN %",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = "NET P&L",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.0f)
                    )
                }

                HorizontalDivider(color = DarkCardBorder)

                // Rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    trades.forEach { trade ->
                        SanthanaTradeRow(trade = trade, onClick = { onTradeClick(trade) })
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2430)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = AccentIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Completed IPO Trades Yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only real IPOs allotted and exited via Zerodha will appear here. Currently 4 applications (PARASYNTEX, ACMERAIL, TNA, RKFAL) are active in Zerodha Kite.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SanthanaTradeRow(
    trade: SanthanaLakshmiTradeItem,
    onClick: () -> Unit
) {
    val gainColor = if (trade.netGainPct >= 0.0) ProfitGreen else LossRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = DarkBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Col 1: Symbol & Dates (Clickable for Google Finance)
            val context = LocalContext.current
            Column(modifier = Modifier.weight(1.3f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            StockLinkHelper.openGoogleFinance(
                                context = context,
                                symbol = trade.symbol
                            )
                        }
                ) {
                    Text(
                        text = trade.symbol,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "View on Google Finance",
                        tint = AccentIndigoLight.copy(alpha = 0.7f),
                        modifier = Modifier.size(9.dp)
                    )
                }
                Text(
                    text = trade.listingDate,
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }

            // Col 2: Issue -> Exit
            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = trade.formattedIssuePrice,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "→ ${trade.formattedExitPrice}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Col 3: Gain %
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = trade.formattedNetGainPct,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = gainColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = trade.exitReason.replace("REGIME_DEFENSE_", "DEF:"),
                    fontSize = 8.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Col 4: Net PnL
            Column(
                modifier = Modifier.weight(1.0f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = trade.formattedNetPnl,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = gainColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Tap ℹ️",
                    fontSize = 8.5.sp,
                    color = AccentIndigo
                )
            }
        }
    }
}

@Composable
private fun SanthanaTradeDetailDialog(
    trade: SanthanaLakshmiTradeItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = trade.symbol,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (trade.isWin) ProfitGreenBg else LossRedBg
                            ) {
                                Text(
                                    text = if (trade.isWin) "PROFITABLE" else "DEFENSE EXIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trade.isWin) ProfitGreen else LossRed,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = trade.companyName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text(text = "✕", fontSize = 16.sp, color = TextSecondary)
                    }
                }

                HorizontalDivider(color = DarkCardBorder)

                // Audit Breakdown Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Application Amount", value = trade.formattedAppliedAmount)
                    DetailRow(label = "Issue / Cut-off Price", value = trade.formattedIssuePrice)
                    DetailRow(label = "Listing Exit Price", value = trade.formattedExitPrice)
                    DetailRow(label = "Decision Date", value = trade.decisionDate)
                    DetailRow(label = "Listing Date", value = trade.listingDate)
                    DetailRow(label = "Market Regime at Decision", value = trade.regime)
                    DetailRow(label = "QIB Institutional Multiple", value = "${trade.qibMultiple}x")
                    DetailRow(label = "Exit Policy Trigger", value = trade.exitReason)
                    DetailRow(label = "Statutory Taxes & DP Fees", value = String.format(Locale.US, "₹%.2f", trade.totalCost))
                    DetailRow(
                        label = "Alpha vs Nifty 50",
                        value = String.format(Locale.US, "%s%.2f%%", if (trade.alphaPct >= 0) "+" else "", trade.alphaPct),
                        valueColor = if (trade.alphaPct >= 0) ProfitGreen else LossRed
                    )
                }

                HorizontalDivider(color = DarkCardBorder)

                // Bottom Net PnL Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (trade.isWin) ProfitGreen else DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "NET REALIZED P&L", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(text = "Gain: ${trade.formattedNetGainPct}", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text(
                            text = trade.formattedNetPnl,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.isWin) ProfitGreen else LossRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close Audit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SanthanaArchitectureAccordion() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotation")

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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Architecture",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "THE 3 PILLARS OF SANTHANALAKSHMI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.rotate(rotationState)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillarItem(
                        number = "1",
                        title = "Regime Gating & Macro Defense",
                        description = "Bids selectively when Nifty trend is positive and institutional QIB demand is high (>= 1.5x in Bull, >= 5.0x in Neutral). Completely skips Bearish regimes to avoid catastrophic listing discounts (-16.2% avg)."
                    )
                    PillarItem(
                        number = "2",
                        title = "1-Lot Retail & sHNI Allocation",
                        description = "Enforces mathematical 1-lot retail ASBA bidding (highest lottery allotment probability per rupee blocked) or sHNI category when capital scales."
                    )
                    PillarItem(
                        number = "3",
                        title = "Idle Capital Liquid Sweep (+6.5% p.a.)",
                        description = "Unallocated capital does not sit idle. 100% of cash is swept daily into liquid exchange-traded funds (Nippon Liquid BeES) generating +6.50% annualized yield with zero market volatility."
                    )
                }
            }
        }
    }
}

@Composable
private fun PillarItem(
    number: String,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AccentIndigoBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentIndigo
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SanthanaActiveApplicationsSection(
    applications: List<ActiveIpoApplication>,
    onConfirmMandate: (String) -> Unit = {},
    onUpdateAllotmentStatus: (String, String) -> Unit = { _, _ -> },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF238636).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "ACTIVE IPO APPLICATIONS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF238636).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3FB950).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${applications.size} Bids Active",
                        color = Color(0xFF3FB950),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Tracking active UPI mandates, registrar allotment verification, and listing-day automated CNC exit execution.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            // Cards for each application
            applications.forEach { app ->
                SanthanaApplicationCard(
                    app = app,
                    onConfirmMandate = onConfirmMandate,
                    onUpdateAllotmentStatus = onUpdateAllotmentStatus
                )
            }
        }
    }
}

@Composable
private fun SanthanaApplicationCard(
    app: ActiveIpoApplication,
    onConfirmMandate: (String) -> Unit = {},
    onUpdateAllotmentStatus: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAllotmentToday = app.isAllotmentToday

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0D1117),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Symbol, Company, Mandate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentIndigoBg.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, AccentIndigoLight.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    StockLinkHelper.openGoogleFinance(
                                        context = context,
                                        symbol = app.symbol
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = app.symbol,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "View on Google Finance",
                                    tint = Color(0xFF58A6FF),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF161B22),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Text(
                                text = app.issueType,
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = app.companyName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Mandate status badge
                if (app.isMandateApproved) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF238636).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3FB950))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF3FB950),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "MANDATE APPROVED",
                                color = Color(0xFF3FB950),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD29922).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD29922)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onConfirmMandate(app.symbol) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⏳ UPI PENDING (TAP TO CONFIRM)",
                                color = Color(0xFFE3B341),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Capital & Lot Details Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "BLOCKED CAPITAL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = app.formattedTotalAmount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "LOT SIZE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "${app.lotSize} Shares", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CUT-OFF PRICE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = app.formattedCutOffPrice, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Lifecycle Milestones Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MilestonePill(label = "CLOSES", value = app.closingDate, isAlert = app.closingDate.contains("06"))
                MilestonePill(label = "ALLOTMENT", value = app.allotmentDate, isAlert = isAllotmentToday)
                MilestonePill(label = "LISTING", value = app.listingDate, isAlert = false)
            }

            // Auto-Sell Armed Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1F242C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF388BFD).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "⚡", fontSize = 10.sp)
                        Text(
                            text = "AUTO-SELL ARMED",
                            color = Color(0xFF58A6FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 10:00 AM CNC Exit",
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }

                    Text(
                        text = "Target: +${String.format(Locale.US, "%.0f", app.targetListingGainPct)}%",
                        color = Color(0xFF3FB950),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Allotment Result & Registrar Status Row
            when {
                app.isAllotted -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF238636).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3FB950))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🎉", fontSize = 12.sp)
                                Column {
                                    Text(
                                        text = "ALLOTMENT: APPROVED (ALLOTTED)",
                                        color = Color(0xFF3FB950),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${app.lotSize} Shares Allocated • Demat Credit Scheduled",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF238636)
                            ) {
                                Text(
                                    text = "DEBITED",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                app.isNotAllotted -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDA3633).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF85149).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "❌", fontSize = 12.sp)
                                Column {
                                    Text(
                                        text = "ALLOTMENT: NOT ALLOTTED (REJECTED)",
                                        color = Color(0xFFF85149),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Mandate Revoked • ${app.formattedTotalAmount} Unblocked to Bank",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDA3633)
                            ) {
                                Text(
                                    text = "UNBLOCKED",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Pending / In progress
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAllotmentToday) Color(0xFF1F2A38) else Color(0xFF161B22),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAllotmentToday) Color(0xFF58A6FF).copy(alpha = 0.6f) else Color(0xFF30363D)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = if (isAllotmentToday) "⏳" else "📅", fontSize = 11.sp)
                                    Column {
                                        Text(
                                            text = if (isAllotmentToday) "ALLOTMENT RESULT: ANNOUNCEMENT DUE TODAY" else "ALLOTMENT STATUS: SCHEDULED FOR ${app.allotmentDate}",
                                            color = if (isAllotmentToday) Color(0xFF58A6FF) else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isAllotmentToday) "Registrar basis of allotment pending confirmation" else "UPI mandate active • Capital blocked via ASBA",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isAllotmentToday) Color(0xFF388BFD).copy(alpha = 0.2f) else Color(0xFF21262D),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isAllotmentToday) Color(0xFF58A6FF) else Color(0xFF30363D)
                                    )
                                ) {
                                    Text(
                                        text = if (isAllotmentToday) "IN PROGRESS" else "BLOCKED",
                                        color = if (isAllotmentToday) Color(0xFF58A6FF) else TextMuted,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Registrar Allotment Check Button (Full Width, Centered, Elegant)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF21262D),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF388BFD).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        StockLinkHelper.openRegistrarAllotmentCheck(
                                            context = context,
                                            symbol = app.symbol,
                                            registrarName = app.registrarName
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF58A6FF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check Registrar (${app.registrarName})",
                                        color = Color(0xFF58A6FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }

                            // Manual Outcome Actions (Balanced 50/50 Row)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF238636).copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3FB950).copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onUpdateAllotmentStatus(app.symbol, "ALLOTTED") }
                                ) {
                                    Text(
                                        text = "Mark Allotted ✓",
                                        color = Color(0xFF3FB950),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDA3633).copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF85149).copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onUpdateAllotmentStatus(app.symbol, "NOT_ALLOTTED") }
                                ) {
                                    Text(
                                        text = "Not Allotted ✕",
                                        color = Color(0xFFF85149),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SanthanaPastApplicationsSection(
    pastApplications: List<PastIpoApplication>,
    totalAllotted: Int,
    totalNotAllotted: Int,
    totalUnblockedCapital: Double,
    totalRealizedProfit: Double,
    modifier: Modifier = Modifier
) {
    var pastFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredList = remember(pastFilter, pastApplications) {
        when (pastFilter) {
            "ALLOTTED" -> pastApplications.filter { it.isAllotted }
            "NOT_ALLOTTED" -> pastApplications.filter { !it.isAllotted }
            else -> pastApplications
        }
    }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF388BFD).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "PAST IPO APPLICATIONS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF388BFD).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF58A6FF).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${pastApplications.size} Concluded",
                        color = Color(0xFF58A6FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Full audit history of past applications, registrar allotment outcomes (Allotted vs Rejected), and bank funds resolution (Debited vs Unblocked).",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            if (pastApplications.isNotEmpty()) {
                // Summary Metrics Cards (Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "ALLOTMENT RATE", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            val ratePct = if (pastApplications.isNotEmpty()) totalAllotted * 100.0 / pastApplications.size else 0.0
                            Text(
                                text = "${String.format(Locale.US, "%.1f", ratePct)}%",
                                color = Color(0xFF3FB950),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "$totalAllotted Allotted / ${pastApplications.size}", color = TextSecondary, fontSize = 8.5.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "FUNDS UNBLOCKED", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹" + String.format(Locale.US, "%,.0f", totalUnblockedCapital),
                                color = Color(0xFFE3B341),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "$totalNotAllotted Rejected (0 Loss)", color = TextSecondary, fontSize = 8.5.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "LISTING PROFIT", color = TextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "+₹" + String.format(Locale.US, "%,.0f", totalRealizedProfit),
                                color = Color(0xFF3FB950),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Auto-Sold @ CNC", color = TextSecondary, fontSize = 8.5.sp)
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = pastFilter == "ALL",
                        onClick = { pastFilter = "ALL" },
                        label = { Text("All (${pastApplications.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentIndigo,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = pastFilter == "ALLOTTED",
                        onClick = { pastFilter = "ALLOTTED" },
                        label = { Text("🎉 Allotted ($totalAllotted)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF238636),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = pastFilter == "NOT_ALLOTTED",
                        onClick = { pastFilter = "NOT_ALLOTTED" },
                        label = { Text("❌ Not Allotted ($totalNotAllotted)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDA3633),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // List of Past Application Cards
                filteredList.forEach { pastApp ->
                    PastApplicationCard(pastApp = pastApp)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF21262D))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F2430)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF58A6FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Past Concluded Applications Yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only real IPO applications concluded via Zerodha will appear here. As active applications (ACMERAIL, RKFAL, PARASYNTEX, TNA) announce registrar allotments, their outcomes and unblocked capital will appear here automatically.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastApplicationCard(
    pastApp: PastIpoApplication,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0D1117),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Symbol, Company, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentIndigoBg.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, AccentIndigoLight.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    StockLinkHelper.openGoogleFinance(
                                        context = context,
                                        symbol = pastApp.symbol
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = pastApp.symbol,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "View on Google Finance",
                                    tint = Color(0xFF58A6FF),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF161B22),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Text(
                                text = pastApp.issueType,
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = pastApp.companyName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Outcome Pill
                if (pastApp.isAllotted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF238636).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3FB950))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF3FB950),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "ALLOTTED (APPROVED)",
                                color = Color(0xFF3FB950),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDA3633).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF85149).copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFF85149),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "NOT ALLOTTED (REJECTED)",
                                color = Color(0xFFF85149),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Financial & Allotment Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "APPLIED AMOUNT", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = pastApp.formattedTotalAmount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${pastApp.lotSize} sh @ ₹${String.format(Locale.US, "%.0f", pastApp.cutOffPrice)}", color = TextMuted, fontSize = 8.5.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "BANK RESOLUTION", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    if (pastApp.isAllotted) {
                        Text(text = "${pastApp.formattedTotalAmount} Debited", color = Color(0xFF58A6FF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Demat Credited", color = TextMuted, fontSize = 8.5.sp)
                    } else {
                        Text(text = "${pastApp.formattedTotalAmount} Unblocked", color = Color(0xFFE3B341), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Mandate Released (100%)", color = Color(0xFF3FB950), fontSize = 8.5.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "LISTING OUTCOME", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    if (pastApp.isAllotted) {
                        Text(
                            text = pastApp.formattedRealizedPnl,
                            color = Color(0xFF3FB950),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gain: ${pastApp.formattedListingGainPct}",
                            color = Color(0xFF3FB950),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "₹0.00 Loss",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ASBA Protected",
                            color = TextMuted,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }

            // Timeline Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MilestonePill(label = "APPLIED", value = pastApp.appliedDate, isAlert = false)
                MilestonePill(label = "ALLOTMENT", value = pastApp.allotmentDate, isAlert = false)
                MilestonePill(label = "LISTING", value = pastApp.listingDate, isAlert = false)
            }

            // Audit Explanation Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = if (pastApp.isAllotted) Color(0xFF238636).copy(alpha = 0.12f) else Color(0xFF161B22),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (pastApp.isAllotted) Color(0xFF3FB950).copy(alpha = 0.4f) else Color(0xFF30363D)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = pastApp.outcomeReason,
                        color = if (pastApp.isAllotted) Color(0xFFE6EDF3) else TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registrar: ${pastApp.registrarName}",
                            color = TextMuted,
                            fontSize = 9.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF21262D),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF30363D)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    StockLinkHelper.openRegistrarAllotmentCheck(
                                        context = context,
                                        symbol = pastApp.symbol,
                                        registrarName = pastApp.registrarName
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFF58A6FF),
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "Registrar Portal ↗",
                                    color = Color(0xFF58A6FF),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonePill(
    label: String,
    value: String,
    isAlert: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = if (isAlert) Color(0xFFF85149) else TextMuted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = if (isAlert) Color(0xFFFFA198) else TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

