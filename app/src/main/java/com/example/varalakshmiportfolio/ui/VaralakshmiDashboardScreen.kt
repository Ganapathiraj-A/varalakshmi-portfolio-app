package com.example.varalakshmiportfolio.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.varalakshmiportfolio.notification.IpoNotificationManager
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.components.HoldingsTable
import com.example.varalakshmiportfolio.ui.components.IpoActionBanner
import com.example.varalakshmiportfolio.ui.components.PortfolioHeaderCard
import com.example.varalakshmiportfolio.ui.components.TodayTickerChangesTable
import com.example.varalakshmiportfolio.ui.components.TransactionsTable
import com.example.varalakshmiportfolio.ui.components.StockRecommendationsTable
import com.example.varalakshmiportfolio.ui.components.StockChartDialog
import com.example.varalakshmiportfolio.ui.components.RecommendationHistoryDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaralakshmiDashboardScreen(
    viewModel: VaralakshmiViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Live clock updating every second
    val timeFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    var currentTimeString by remember { mutableStateOf(timeFormatter.format(Date())) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeString = timeFormatter.format(Date())
            delay(1000L)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(uiState.ipoNotifications) {
        val activeHighUrgency = uiState.ipoNotifications.filter { !it.isDismissed && it.urgency == "HIGH" }
        if (activeHighUrgency.isEmpty()) {
            IpoNotificationManager.cancelAllAlerts(context)
        } else {
            for (alert in activeHighUrgency) {
                IpoNotificationManager.postIpoAlert(context, alert)
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Varalakshmi Portfolio",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live Dual-Strategy Alpha Compounder",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isRefreshing
                    ) {
                        AnimatedRefreshIcon(isRefreshing = uiState.isRefreshing)
                    }
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Connection Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Current Time & Today's Nifty 50 Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Row 1: Current Time on left, Sync Status on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Current Time",
                                tint = AccentIndigoLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "CURRENT TIME",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currentTimeString,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isRefreshing) GoldAccent
                                            else if (uiState.isLiveSync) ProfitGreen
                                            else TextSecondary
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (uiState.isRefreshing) "Syncing..."
                                    else if (uiState.isLiveSync) "Live Cloudflare"
                                    else "Cached Snapshot",
                                    fontSize = 11.sp,
                                    color = if (uiState.isRefreshing) GoldAccent
                                    else if (uiState.isLiveSync) ProfitGreen
                                    else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Data: ${uiState.summary.lastUpdated}",
                                fontSize = 9.5.sp,
                                color = TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Today's Nifty 50 Benchmark Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentIndigoLight.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentIndigoLight.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = uiState.nifty.symbol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentIndigoLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.nifty.formattedLtp,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val isPos = uiState.nifty.isPositive
                            val changeColor = if (isPos) ProfitGreen else LossRed
                            Text(
                                text = uiState.nifty.formattedChange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = changeColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (uiState.nifty.high > 0 && uiState.nifty.low > 0) {
                                Text(
                                    text = "H: ${String.format(Locale.US, "%,.1f", uiState.nifty.high)}  L: ${String.format(Locale.US, "%,.1f", uiState.nifty.low)}",
                                    fontSize = 9.5.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // IPO Action Alerts Banner (Santhana Lakshmi)
            IpoActionBanner(
                notifications = uiState.ipoNotifications,
                onDismiss = { viewModel.dismissIpoNotification(it) }
            )

            // 1. Portfolio Header Card (NAV, Returns, Capital Metrics)
            PortfolioHeaderCard(summary = uiState.summary)

            // 2. Individual Tickers Table (Symbol, % up/down, Value, Target Toggle)
            HoldingsTable(
                positions = uiState.positions,
                onExitClick = { viewModel.requestExit(it) },
                onToggleProfitTarget = { viewModel.toggleProfitTarget(it) }
            )

            // 3. Today's Ticker Changes Table (Ticker, Price, Today Chg %, Today Value ₹)
            TodayTickerChangesTable(
                positions = uiState.positions,
                summaryTodayPnl = uiState.summary.todayPnl
            )

            // 4. Recent Transactions Table (Date, Buy/Sell, Ticker, Price, Profit/Loss Difference)
            TransactionsTable(
                transactions = uiState.transactions
            )

            // 5. Top 5 Stock Recommendations Table (Rank, Ticker, Price, Score, Action, History)
            StockRecommendationsTable(
                recommendations = uiState.recommendations,
                onRecommendationClick = { viewModel.selectRecommendation(it) },
                onViewHistoryClick = { viewModel.openRecommendationHistory() },
                historyWinRateSummary = "${uiState.recommendationHistorySummary.formattedWinRate} Win Rate"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive 2-Month Historical Price Chart Modal Dialog
    uiState.selectedRecommendationForChart?.let { recommendation ->
        StockChartDialog(
            recommendation = recommendation,
            onDismiss = { viewModel.dismissRecommendationChart() }
        )
    }

    // Past 3-Month Recommendation Track Record Modal Dialog
    if (uiState.showRecommendationHistory) {
        RecommendationHistoryDialog(
            history = uiState.recommendationHistory,
            summary = uiState.recommendationHistorySummary,
            selectedFilter = uiState.selectedHistoryFilter,
            onFilterChange = { viewModel.setHistoryFilter(it) },
            onDismiss = { viewModel.closeRecommendationHistory() }
        )
    }

    // Exit Position Confirmation Modal
    if (uiState.positionToExit != null) {
        val target = uiState.positionToExit!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissExit() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = LossRedLight
                )
            },
            title = {
                Text(
                    text = "Exit ${target.symbol} Position?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to trigger a market exit for ${target.symbol}?",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Quantity: ${target.quantity} shares", color = TextPrimary, fontSize = 12.sp)
                            Text("Current Price: ₹${String.format(Locale.US, "%.2f", target.currentPrice)}", color = TextPrimary, fontSize = 12.sp)
                            Text("Market Value: ₹${String.format(Locale.US, "%,.2f", target.marketValue)}", color = TextPrimary, fontSize = 12.sp)
                            Text(
                                "Unrealized P&L: ₹${String.format(Locale.US, "%,.2f", target.unrealizedPnl)} (${target.unrealizedPnlPct}%)",
                                color = if (target.unrealizedPnl >= 0) ProfitGreen else LossRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmExit() },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Confirm Exit (✕)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissExit() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Server Settings Dialog
    if (uiState.showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(uiState.serverUrl) }
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()

        fun safeOpenUri(uri: String) {
            try {
                uriHandler.openUri(uri)
            } catch (e: Throwable) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Unable to open browser: ${e.localizedMessage ?: "No browser application found"}")
                }
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.closeSettings() },
            title = {
                Text(
                    text = "Live Server Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Configure Unified UI server URL to sync live telemetry from trading cycles:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Server Base URL") },
                        placeholder = { Text("http://192.168.1.100:8088") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Cloudflare Tunnel (Anywhere / Mobile Data):\n  https://varalakshmi.ghostsoftwaresystems.com\n• Local LAN fallback: http://<LAN-IP>:8000\n• Android Emulator: http://10.0.2.2:8000",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    Text(
                        text = "App Updates & Releases (Current: v1.9.0)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download the latest APK release directly to update your installation:",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sUrl = tempUrl.trim().trimEnd('/')
                                val downloadUrl = if (sUrl.isNotBlank()) "$sUrl/download/varalakshmi-portfolio.apk"
                                else "https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/latest/download/varalakshmi-portfolio.apk"
                                safeOpenUri(downloadUrl)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download Latest APK",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download APK", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                safeOpenUri("https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/latest/download/varalakshmi-portfolio.apk")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "View GitHub Releases",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GitHub Mirror", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateServerUrl(tempUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Save & Sync", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeSettings() }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AnimatedRefreshIcon(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "RefreshSpinTransition")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RefreshSpinAngle"
        )
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refreshing",
            tint = AccentIndigoLight,
            modifier = modifier.graphicsLayer { rotationZ = rotation }
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh",
            tint = AccentIndigoLight,
            modifier = modifier
        )
    }
}
