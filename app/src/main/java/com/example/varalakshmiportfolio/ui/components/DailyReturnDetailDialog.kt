package com.example.varalakshmiportfolio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.varalakshmiportfolio.model.DailyReturnItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.formatDailyReturnStatementForClipboard
import com.example.varalakshmiportfolio.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable

fun DailyReturnDetailDialog(
    dailyReturn: DailyReturnItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val pnlColor = when {
        dailyReturn.dailyPnl >= 0.005 -> ProfitGreen
        dailyReturn.dailyPnl <= -0.005 -> LossRed
        else -> TextSecondary
    }

    val returnColor = when {
        dailyReturn.returnPct >= 0.005 -> ProfitGreen
        dailyReturn.returnPct <= -0.005 -> LossRed
        else -> TextSecondary
    }

    val statusUpper = dailyReturn.status.uppercase(Locale.US)
    val displayStatus = when (statusUpper) {
        "WIN", "PROFIT", "GAIN" -> "WIN"
        "LOSS", "DEFICIT" -> "LOSS"
        "FLAT", "NEUTRAL", "EVEN", "PRE_INCEPTION", "BREAKEVEN" -> "FLAT"
        else -> if (statusUpper.length > 5) statusUpper.take(4) else statusUpper
    }
    val (statusBg, statusTextColor) = when (displayStatus) {
        "WIN" -> Pair(ProfitGreenBg, ProfitGreen)
        "LOSS" -> Pair(LossRedBg, LossRed)
        else -> Pair(DarkCardBorder.copy(alpha = 0.5f), TextSecondary)
    }

    val totalVolume = dailyReturn.trades.sumOf { it.grossAmount }
    val totalFees = dailyReturn.trades.sumOf { it.fees }
    val buyTradesCount = dailyReturn.trades.count { it.side.equals("BUY", ignoreCase = true) }
    val sellTradesCount = dailyReturn.trades.count { it.side.equals("SELL", ignoreCase = true) }

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
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkCard)
                                .border(1.dp, DarkCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (dailyReturn.dailyPnl >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = "Day Return",
                                tint = pnlColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dailyReturn.date.ifBlank { "Daily Session" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusBg,
                                    border = BorderStroke(0.8.dp, statusTextColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = displayStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTextColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Daily Session Splitup & Executions",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val textToCopy = formatDailyReturnStatementForClipboard(dailyReturn)
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Session statement for ${dailyReturn.date} copied!", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    isCopied = true
                                    delay(2000L)
                                    isCopied = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkCard)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy Daily Statement",
                                tint = if (isCopied) ProfitGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkCard)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // KPI Metrics Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCard,
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NET REALIZED P&L",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dailyReturn.formattedPnl,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = pnlColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SESSION RETURN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dailyReturn.formattedReturnPct,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = returnColor
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkCardBorder.copy(alpha = 0.6f),
                            thickness = 0.8.dp
                        )

                        // Secondary Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "TOTAL FUND BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentIndigoLight)
                                Text(
                                    text = dailyReturn.formattedNav,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "EXECUTIONS", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = if (dailyReturn.tradeCount > 0) "${dailyReturn.tradeCount} Trades" else "0 Trades",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "TOTAL VOLUME", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = if (totalVolume > 0) String.format(Locale.US, "₹%,.2f", totalVolume) else "₹0.00",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                if (dailyReturn.hasCapitalInflow) {
                    val inflowColor: Color = if (dailyReturn.capitalInflow > 0) AccentIndigoLight else LossRed
                    val inflowBgColor: Color = if (dailyReturn.capitalInflow > 0) AccentIndigoBg else LossRedBg
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = inflowBgColor,
                        border = BorderStroke(1.dp, inflowColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = inflowColor.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (dailyReturn.capitalInflow > 0) "CAPITAL TOP-UP" else "CAPITAL WITHDRAWAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = inflowColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dailyReturn.formattedCapitalInflow,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = inflowColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Strategy capital scaled up from ₹1,00,000 to ₹6,00,000. Balance identity holds exactly: Opening Balance + Daily P&L + Capital Top-Up = Closing Balance.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Trades Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Trades",
                            tint = AccentIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRADE BREAKDOWN (${dailyReturn.trades.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (dailyReturn.trades.isNotEmpty()) {
                        Text(
                            text = "$buyTradesCount BUY • $sellTradesCount SELL",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Trades List or Empty State
                if (dailyReturn.trades.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCard,
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No trade executions recorded for this day",
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            if (dailyReturn.tradeCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dailyReturn.tradeCount} aggregate transactions reported in platform history",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dailyReturn.trades.forEach { trade ->
                            TradeDetailCard(trade = trade)
                        }
                    }
                }

                if (totalFees > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Brokerage & Regulatory Taxes:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = String.format(Locale.US, "₹%,.2f", totalFees),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCard,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text(
                        text = "Close",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeDetailCard(
    trade: TransactionItem,
    modifier: Modifier = Modifier
) {
    val isBuy = trade.side.equals("BUY", ignoreCase = true)
    val sideColor = if (isBuy) ProfitGreen else LossRed
    val sideBg = if (isBuy) ProfitGreenBg else LossRedBg

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCardBg,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Symbol, Side badge, and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = sideBg,
                        border = BorderStroke(0.6.dp, sideColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = trade.side.uppercase(Locale.US),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = sideColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trade.symbol.ifBlank { "—" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time
                val timeStr = if (trade.timestamp.length >= 16) {
                    val parts = trade.timestamp.split(" ")
                    if (parts.size >= 2) parts[1].take(8) else trade.timestamp
                } else {
                    trade.timestamp
                }
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Quantity @ Fill Price and Realized P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${trade.quantity} shares @ " + String.format(Locale.US, "₹%,.2f", trade.fillPrice),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Gross: " + String.format(Locale.US, "₹%,.2f", trade.grossAmount),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (!isBuy || trade.realizedPnl != 0.0) {
                        val pnlDiffColor = when {
                            trade.realizedPnl >= 0.005 -> ProfitGreen
                            trade.realizedPnl <= -0.005 -> LossRed
                            else -> TextSecondary
                        }
                        val diffText = if (trade.pnlDifference.isNotBlank()) {
                            trade.pnlDifference
                        } else {
                            (if (trade.realizedPnl >= 0) "+₹" else "-₹") +
                                String.format(Locale.US, "%,.2f", kotlin.math.abs(trade.realizedPnl))
                        }
                        Text(
                            text = diffText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = pnlDiffColor
                        )
                        Text(
                            text = "Realized P&L",
                            fontSize = 9.5.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Position Entry",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentIndigoLight
                        )
                        Text(
                            text = "₹0.00 Realized",
                            fontSize = 9.5.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
