package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.theme.*
import java.util.Locale

@Composable
fun TransactionsTable(
    transactions: List<TransactionItem>,
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
                        text = "RECENT TRANSACTIONS",
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
                            text = "${transactions.size} Logs",
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Fill History",
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
                        text = "DATE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        text = "SIDE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        text = "TICKER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "PRICE",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.0f)
                    )
                    Text(
                        text = "DIFF / P&L",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                transactions.forEachIndexed { index, tx ->
                    key(tx.transactionId) {
                        TransactionRow(transaction = tx)
                        if (index < transactions.size - 1) {
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
private fun TransactionRow(
    transaction: TransactionItem
) {
    var expanded by remember { mutableStateOf(false) }
    val isBuy = transaction.side.uppercase() == "BUY"
    val sideColor = if (isBuy) ProfitGreen else LossRed
    val sideBg = if (isBuy) ProfitGreenBg else LossRedBg

    // Format readable date
    val displayDate = if (transaction.timestamp.length >= 16) {
        val parts = transaction.timestamp.split(" ")
        if (parts.size >= 2) {
            val datePart = parts[0] // 2026-09-21
            val timePart = parts[1].take(5) // 09:26
            val sub = datePart.split("-")
            if (sub.size == 3) "${sub[2]}/${sub[1]} $timePart" else "$datePart $timePart"
        } else {
            transaction.timestamp.take(16)
        }
    } else {
        transaction.timestamp
    }

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
            // 1. DATE
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = displayDate,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. SIDE (BUY / SELL)
            Box(
                modifier = Modifier.weight(0.7f),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = sideBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, sideColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = transaction.side.uppercase(),
                        color = sideColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // 3. TICKER
            Column(modifier = Modifier.weight(1.1f)) {
                Text(
                    text = transaction.symbol,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.quantity} qty",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            // 4. PRICE
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "₹" + String.format(Locale.US, "%.2f", transaction.fillPrice),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 5. PROFIT / LOSS DIFFERENCE
            Column(modifier = Modifier.weight(1.3f)) {
                val isProfit = transaction.pnlDifference.startsWith("+")
                val diffColor = when {
                    transaction.pnlDifference.startsWith("+") -> ProfitGreen
                    transaction.pnlDifference.startsWith("-") -> LossRed
                    else -> TextSecondary
                }

                Text(
                    text = transaction.pnlDifference.ifEmpty { "₹0.00" },
                    color = diffColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expanded details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(DarkCard, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailText(label = "Gross Amount", value = "₹" + String.format(Locale.US, "%,.2f", transaction.grossAmount))
                    DetailText(label = "Brokerage & Fees", value = "₹" + String.format(Locale.US, "%.2f", transaction.fees))
                    DetailText(label = "Realized P&L", value = "₹" + String.format(Locale.US, "%.2f", transaction.realizedPnl))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TxID: ${transaction.transactionId}",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
