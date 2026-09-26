package com.example.varalakshmiportfolio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.IpoActionType
import com.example.varalakshmiportfolio.theme.*

@Composable
fun IpoActionBanner(
    notifications: List<IpoActionNotification>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotifications = notifications.filterNot { it.isDismissed }
    if (activeNotifications.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        activeNotifications.forEach { alert ->
            IpoAlertCard(alert = alert, onDismiss = { onDismiss(alert.id) })
        }
    }
}

@Composable
private fun IpoAlertCard(
    alert: IpoActionNotification,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val (badgeBg, badgeBorder, badgeText, badgeColor) = when (alert.actionType) {
        IpoActionType.APPLY_NOW -> Quadruple(
            GoldAccent.copy(alpha = 0.15f),
            GoldAccent.copy(alpha = 0.5f),
            "ACTION REQUIRED: APPLY NOW",
            GoldAccent
        )
        IpoActionType.MANDATE_PENDING -> Quadruple(
            AccentIndigoLight.copy(alpha = 0.15f),
            AccentIndigoLight.copy(alpha = 0.5f),
            "MANDATE PENDING (CHECK GPAY)",
            AccentIndigoLight
        )
        IpoActionType.ALLOTMENT_WON -> Quadruple(
            ProfitGreen.copy(alpha = 0.15f),
            ProfitGreen.copy(alpha = 0.5f),
            "🎉 ALLOTMENT WON (1 LOT)",
            ProfitGreen
        )
        IpoActionType.ALLOTMENT_MISSED -> Quadruple(
            TextMuted.copy(alpha = 0.15f),
            TextMuted.copy(alpha = 0.3f),
            "REFUNDED / LIEN RELEASED",
            TextMuted
        )
        IpoActionType.LISTING_EXIT -> Quadruple(
            ProfitGreen.copy(alpha = 0.2f),
            ProfitGreen.copy(alpha = 0.6f),
            "LISTING TODAY (EXIT AUTOMATED)",
            ProfitGreen
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.2.dp, if (alert.urgency == "HIGH") badgeBorder.copy(alpha = alphaAnim) else DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Action Tag + Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = alphaAnim))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg,
                        border = BorderStroke(0.8.dp, badgeBorder)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss Alert",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = alert.headline,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body message
            Text(
                text = alert.message,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Application Bidding Details Card (Lots, Shares, Cutoff, Total ₹)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DarkBackground,
                border = BorderStroke(0.8.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "BIDDING SPECIFICATIONS (RETAIL MANDATE)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lots to Apply:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${alert.lotQuantity} Lot (${alert.lotShares} Shares)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bid Price:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${alert.formattedCutoffPrice} (Cut-off)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Blocked:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = alert.formattedTotalAmount,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric pills: QIB Multiple, Window Deadline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (alert.qibMultiple > 0) {
                    MetricPill(
                        label = "QIB INSTITUTIONAL DEMAND",
                        value = "${alert.formattedQibMultiple} (Rule >15x: PASS)",
                        valueColor = if (alert.qibMultiple >= 15.0) ProfitGreen else GoldAccent,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                MetricPill(
                    label = "WINDOW CLOSES",
                    value = alert.closeDeadline,
                    valueColor = LossRedLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(alert.deepLinkUrl)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (alert.actionType == IpoActionType.APPLY_NOW) AccentIndigo else ProfitGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (alert.actionType) {
                            IpoActionType.APPLY_NOW -> "Apply on Zerodha / CUB"
                            IpoActionType.MANDATE_PENDING -> "Approve UPI Mandate"
                            IpoActionType.ALLOTMENT_WON -> "View in Demat"
                            else -> "View IPO Details"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Done", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = DarkBackground,
        border = BorderStroke(0.8.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
