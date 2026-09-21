package com.example.varalakshmiportfolio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.PortfolioSummary
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
                    Text(
                        text = "VARALAKSHMI ALPHA",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentIndigoLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "80%+ Fast Rotation Compounder",
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
                            text = summary.status,
                            color = ProfitGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Portfolio NAV
            Text(
                text = "PORTFOLIO VALUE (NAV)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹" + String.format(Locale.US, "%,.2f", summary.totalNav),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Side-by-side Return Badges (Total Return & Today's NAV Change)
            val isTotalPositive = summary.totalPnl >= 0
            val totalPnlColor = if (isTotalPositive) ProfitGreen else LossRed
            val totalPnlBg = if (isTotalPositive) ProfitGreenBg else LossRedBg

            val isTodayPositive = summary.todayPnl >= 0
            val todayColor = if (isTodayPositive) ProfitGreen else LossRed
            val todayBg = if (isTodayPositive) ProfitGreenBg else LossRedBg

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Total Return Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = totalPnlBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, totalPnlColor.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTotalPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.ArrowDownward,
                            contentDescription = "Total Return Direction",
                            tint = totalPnlColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "TOTAL RETURN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = (if (isTotalPositive) "+" else "") +
                                        String.format(Locale.US, "₹%,.2f (%.2f%%)", summary.totalPnl, summary.totalPnlPct),
                                color = totalPnlColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Today's NAV Value Change Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = todayBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, todayColor.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTodayPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.ArrowDownward,
                            contentDescription = "Today's Change Direction",
                            tint = todayColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "TODAY'S CHANGE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = (if (isTodayPositive) "+" else "") +
                                        String.format(Locale.US, "₹%,.2f (%.2f%%)", summary.todayPnl, summary.todayPnlPct),
                                color = todayColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Target: +35%",
                        fontSize = 11.sp,
                        color = ProfitGreenLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "🛡️ Cut Loss: -4%",
                        fontSize = 11.sp,
                        color = LossRedLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "📈 Trail: -8%",
                        fontSize = 11.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
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
        }
    }
}
