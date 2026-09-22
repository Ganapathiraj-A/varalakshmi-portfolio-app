package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun HoldingsTable(
    positions: List<PositionItem>,
    onExitClick: (PositionItem) -> Unit,
    onToggleProfitTarget: (PositionItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "INDIVIDUAL TICKERS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = "${positions.size} Active",
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Tap row for details",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TICKER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.30f)
                    )
                    Text(
                        text = "% UP/DN",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.05f)
                    )
                    Text(
                        text = "VALUE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.15f)
                    )
                    Text(
                        text = "35% CAP",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.70f)
                    )
                    Text(
                        text = "EXIT",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.50f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (positions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No open positions", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                positions.forEachIndexed { index, position ->
                    key(position.positionId) {
                        HoldingRow(
                            position = position,
                            onExitClick = { onExitClick(position) },
                            onToggleProfitTarget = { onToggleProfitTarget(position) }
                        )
                        if (index < positions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = DarkCardBorder.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingRow(
    position: PositionItem,
    onExitClick: () -> Unit,
    onToggleProfitTarget: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isPositive = position.unrealizedPnl >= 0
    val pnlColor = if (isPositive) ProfitGreen else LossRed
    val pnlBg = if (isPositive) ProfitGreenBg else LossRedBg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Ticker Column
            Column(modifier = Modifier.weight(1.30f)) {
                Text(
                    text = position.symbol,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${position.quantity} shares",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }

            // 2. % UP/DOWN Column
            Box(
                modifier = Modifier.weight(1.05f),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = pnlBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = (if (isPositive) "+" else "") +
                                String.format(Locale.US, "%.2f%%", position.unrealizedPnlPct),
                        color = pnlColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // 3. VALUE Column
            Column(modifier = Modifier.weight(1.15f)) {
                Text(
                    text = "₹" + String.format(Locale.US, "%,.0f", position.marketValue),
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@ ₹" + String.format(Locale.US, "%.2f", position.currentPrice),
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. 35% CAP / TARGET Toggle Switch Column
            Box(
                modifier = Modifier.weight(0.70f),
                contentAlignment = Alignment.Center
            ) {
                TargetToggleSwitch(
                    checked = position.isProfitTargetEnabled,
                    onCheckedChange = { onToggleProfitTarget() }
                )
            }

            // 5. EXIT Action Column
            Box(
                modifier = Modifier.weight(0.50f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(LossRedBg)
                        .border(1.dp, LossRed.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onExitClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit / Close Position",
                        tint = LossRedLight,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Expanded Forensic Details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(DarkCard, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Entry Price", value = "₹" + String.format(Locale.US, "%.2f", position.entryPrice))
                    DetailItem(label = "Peak Price", value = "₹" + String.format(Locale.US, "%.2f", position.peakPrice))
                    DetailItem(
                        label = "Unrealized P&L",
                        value = (if (isPositive) "+₹" else "-₹") + String.format(Locale.US, "%,.2f", kotlin.math.abs(position.unrealizedPnl)),
                        valueColor = pnlColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Entry Timestamp", value = position.entryDate)
                    DetailItem(
                        label = "Trail Stop (-8%)",
                        value = "₹" + String.format(Locale.US, "%.2f", position.peakPrice * 0.92),
                        valueColor = GoldAccent
                    )
                    DetailItem(
                        label = "Profit Target",
                        value = if (position.isProfitTargetEnabled) "+35%" else "Uncapped Runner",
                        valueColor = if (position.isProfitTargetEnabled) ProfitGreenLight else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (position.isProfitTargetEnabled) ProfitGreenBg else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (position.isProfitTargetEnabled) ProfitGreen.copy(alpha = 0.4f) else DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (position.isProfitTargetEnabled) "Profit Target: +35%" else "Profit Target: Uncapped Runner",
                        color = if (position.isProfitTargetEnabled) ProfitGreenLight else TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TargetToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = ProfitGreen
    val activeBg = ProfitGreenBg
    val inactiveColor = TextMuted
    val inactiveBg = DarkCard

    Box(
        modifier = modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(
                role = Role.Switch,
                onClickLabel = if (checked) "Switch to Uncapped Runner" else "Switch to +35% Cap",
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (checked) activeBg else inactiveBg,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (checked) activeColor.copy(alpha = 0.5f) else DarkCardBorder
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (checked) {
                        Text(
                            text = "35%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ProfitGreenLight,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(inactiveColor)
                        )
                        Text(
                            text = "RUN",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = inactiveColor,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
