package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.LiveMilestoneState
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun LiveMilestoneCard(
    milestoneState: LiveMilestoneState,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (milestoneState.verdictStatus) {
        "PASS" -> ProfitGreen
        "WARNING" -> LossRed
        else -> GoldAccent
    }

    val statusBg = when (milestoneState.verdictStatus) {
        "PASS" -> ProfitGreen.copy(alpha = 0.12f)
        "WARNING" -> LossRed.copy(alpha = 0.12f)
        else -> GoldAccent.copy(alpha = 0.12f)
    }

    val statusBorder = when (milestoneState.verdictStatus) {
        "PASS" -> ProfitGreen.copy(alpha = 0.40f)
        "WARNING" -> LossRed.copy(alpha = 0.40f)
        else -> GoldAccent.copy(alpha = 0.40f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title, Live Recalculated Badge & Toggle Arrow (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Milestone Verification",
                        tint = AccentIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE EXECUTION MILESTONES",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF161F33),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Daily Recalculated",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Verdict Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = when (milestoneState.verdictStatus) {
                                        "PASS" -> Icons.Filled.CheckCircle
                                        "WARNING" -> Icons.Filled.Warning
                                        else -> Icons.Filled.Info
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = milestoneState.verdictTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = milestoneState.verdictSubtext,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            if (milestoneState.formattedCalculationTimestamp.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Recalculated: ${milestoneState.formattedCalculationTimestamp}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(start = 30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Metric Health Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric 1: Win Rate
                        MetricBox(
                            modifier = Modifier.weight(1f),
                            title = "WIN RATE",
                            value = String.format(Locale.US, "%.1f%%", milestoneState.winRatePct),
                            target = "Target: 35-38%",
                            subDetail = "${milestoneState.winsCount}W / ${milestoneState.lossesCount}L",
                            isGood = milestoneState.winRatePct >= 30.0
                        )

                        // Metric 2: Loss Truncation
                        MetricBox(
                            modifier = Modifier.weight(1f),
                            title = "AVG LOSS",
                            value = String.format(Locale.US, "%.2f%%", milestoneState.avgLossPct),
                            target = "Target: ~ -4.2%",
                            subDetail = "Strict Truncation",
                            isGood = milestoneState.avgLossPct >= -6.5
                        )

                        // Metric 3: Payoff Ratio
                        MetricBox(
                            modifier = Modifier.weight(1f),
                            title = "PAYOFF RATIO",
                            value = String.format(Locale.US, "%.2fx", milestoneState.payoffRatio),
                            target = "Target: > 3.0x",
                            subDetail = "Win/Loss Asymmetry",
                            isGood = milestoneState.payoffRatio >= 2.5
                        )
                    }

                    // In-flight active runners banner if available
                    if (milestoneState.activeRunnersSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = ProfitGreenLight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Active Runners In-Flight:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = milestoneState.activeRunnersSummary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreenLight
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
private fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    target: String,
    subDetail: String,
    isGood: Boolean
) {
    val highlightColor = if (isGood) ProfitGreen else GoldAccent

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131A2B),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = highlightColor,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = target,
                fontSize = 8.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subDetail,
                fontSize = 8.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
