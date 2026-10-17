package com.example.varalakshmiportfolio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.varalakshmiportfolio.model.DailyReturnItem
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.varalakshmiportfolio.model.formatVaralakshmiAllStatementsForClipboard
import com.example.varalakshmiportfolio.ui.components.formatFullHistoryForClipboard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.varalakshmiportfolio.notification.IpoNotificationManager
import com.example.varalakshmiportfolio.service.FloatingScreenerManager
import com.example.varalakshmiportfolio.service.FloatingScreenerService
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.components.HoldingsTable
import com.example.varalakshmiportfolio.ui.components.PortfolioHeaderCard
import com.example.varalakshmiportfolio.ui.components.TodayTickerChangesTable
import androidx.compose.foundation.clickable
import com.example.varalakshmiportfolio.ui.components.TransactionsTable
import com.example.varalakshmiportfolio.ui.components.StockRecommendationsTable
import com.example.varalakshmiportfolio.ui.components.PastRecommendationsSection
import com.example.varalakshmiportfolio.ui.components.LiveMilestoneCard
import com.example.varalakshmiportfolio.ui.components.StockChartDialog
import com.example.varalakshmiportfolio.ui.components.RecommendationHistoryDialog
import com.example.varalakshmiportfolio.ui.components.DailyReturnDetailDialog
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.varalakshmiportfolio.ui.components.VeeraLakshmiScreen
import com.example.varalakshmiportfolio.ui.components.ArjunStocksScreen
import com.example.varalakshmiportfolio.model.formatArjunStocksForClipboard
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
    val veeraLakshmiState by viewModel.veeraLakshmiState.collectAsStateWithLifecycle()
    val arjunStocksState by viewModel.arjunStocksState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val fnoScrollState = rememberScrollState()
    val arjunScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
    val clipboardManager = LocalClipboardManager.current
    var isTopCopied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        IpoNotificationManager.cancelAllAlerts(context)
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
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (pagerState.currentPage) {
                                    1 -> "VeeraLakshmi F&O"
                                    2 -> "Arjun Stocks"
                                    else -> "Vara"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (pagerState.currentPage) {
                                    1 -> "Multi-Asset Derivatives Paper Trading"
                                    2 -> "Broker Recommendations & Live P&L Tracker"
                                    else -> "Live Dual-Strategy Alpha Compounder"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val textToCopy = when (pagerState.currentPage) {
                                    1 -> {
                                        val dailyPnlList = veeraLakshmiState.dailyPnlHistory
                                        val totalDays = dailyPnlList.size
                                        val winDays = dailyPnlList.count { it.isProfit }
                                        val lossDays = totalDays - winDays
                                        val winRatePct = if (totalDays > 0) (winDays.toDouble() / totalDays.toDouble()) * 100.0 else 0.0
                                        val netPnl = dailyPnlList.sumOf { it.dailyPnl }
                                        val avgDailyPnl = if (totalDays > 0) netPnl / totalDays else 0.0
                                        val bestDay = dailyPnlList.maxOfOrNull { it.dailyPnl } ?: 0.0

                                        formatFullHistoryForClipboard(
                                            items = dailyPnlList,
                                            winRatePct = winRatePct,
                                            netPnl = netPnl,
                                            winDays = winDays,
                                            lossDays = lossDays,
                                            avgDailyPnl = avgDailyPnl,
                                            bestDay = bestDay
                                        )
                                    }
                                    2 -> {
                                        formatArjunStocksForClipboard(
                                            summary = arjunStocksState.summary,
                                            stocks = arjunStocksState.recommendations
                                        )
                                    }
                                    else -> {
                                        formatVaralakshmiAllStatementsForClipboard(
                                            summary = uiState.summary,
                                            positions = uiState.positions,
                                            dailyReturns = uiState.dailyReturns,
                                            transactions = uiState.transactions
                                        )
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "All statements copied to clipboard!", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    isTopCopied = true
                                    delay(2000L)
                                    isTopCopied = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isTopCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy All Statements",
                                tint = if (isTopCopied) ProfitGreen else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !uiState.isRefreshing && !veeraLakshmiState.isLoading
                        ) {
                            AnimatedRefreshIcon(isRefreshing = uiState.isRefreshing || veeraLakshmiState.isLoading)
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

                // Swipeable Navigation Tab Row
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = DarkSurface,
                    contentColor = AccentIndigo,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                            color = AccentIndigo
                        )
                    },
                    divider = { HorizontalDivider(color = DarkCardBorder) }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            val todayPnl = uiState.summary.todayPnl
                            val absPnl = kotlin.math.abs(todayPnl)
                            val badgeText = when {
                                absPnl >= 100_000.0 -> String.format(Locale.US, "%s₹%.1fL", if (todayPnl >= 0.0) "+" else "-", absPnl / 100_000.0)
                                absPnl >= 1_000.0 -> String.format(Locale.US, "%s₹%.1fk", if (todayPnl >= 0.0) "+" else "-", absPnl / 1000.0)
                                else -> String.format(Locale.US, "%s₹%.0f", if (todayPnl >= 0.0) "+" else "-", absPnl)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📈 Vara",
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = if (pagerState.currentPage == 0) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (todayPnl >= 0.0) ProfitGreenBg else LossRedBg
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (todayPnl >= 0.0) ProfitGreen else LossRed,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            val fnoPnl = veeraLakshmiState.currentInstance.unrealizedPnl
                            val absPnl = kotlin.math.abs(fnoPnl)
                            val formattedBadge = when {
                                absPnl >= 100_000.0 -> String.format(Locale.US, "%s₹%.1fL", if (fnoPnl >= 0.0) "+" else "-", absPnl / 100_000.0)
                                absPnl >= 1_000.0 -> String.format(Locale.US, "%s₹%.1fk", if (fnoPnl >= 0.0) "+" else "-", absPnl / 1000.0)
                                else -> String.format(Locale.US, "%s₹%.0f", if (fnoPnl >= 0.0) "+" else "-", absPnl)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚡ Veera",
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = if (pagerState.currentPage == 1) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (fnoPnl >= 0.0) ProfitGreenBg else LossRedBg
                                ) {
                                    Text(
                                        text = formattedBadge,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fnoPnl >= 0.0) ProfitGreen else LossRed,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = {
                            val arjunPnl = arjunStocksState.summary.totalPnl
                            val absPnl = kotlin.math.abs(arjunPnl)
                            val formattedBadge = when {
                                absPnl >= 100_000.0 -> String.format(Locale.US, "%s₹%.1fL", if (arjunPnl >= 0.0) "+" else "-", absPnl / 100_000.0)
                                absPnl >= 1_000.0 -> String.format(Locale.US, "%s₹%.1fk", if (arjunPnl >= 0.0) "+" else "-", absPnl / 1000.0)
                                else -> String.format(Locale.US, "%s₹%.0f", if (arjunPnl >= 0.0) "+" else "-", absPnl)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🏹 Arjun",
                                    fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = if (pagerState.currentPage == 2) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (arjunPnl >= 0.0) ProfitGreenBg else LossRedBg
                                ) {
                                    Text(
                                        text = formattedBadge,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (arjunPnl >= 0.0) ProfitGreen else LossRed,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (!uiState.isAutoSyncEnabled) TextMuted
                                            else if (uiState.isMarketOpen) ProfitGreen
                                            else GoldAccent
                                        )
                                )
                                Spacer(modifier = Modifier.width(3.5.dp))
                                Text(
                                    text = if (!uiState.isAutoSyncEnabled) "Auto-sync Off"
                                    else if (uiState.isMarketOpen) "Auto-sync: 1m"
                                    else "Auto-sync: Paused",
                                    fontSize = 9.sp,
                                    color = if (!uiState.isAutoSyncEnabled) TextMuted
                                    else if (uiState.isMarketOpen) ProfitGreenLight
                                    else GoldAccent,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Live Portfolio NAV & Cash Invested under Date Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PORTFOLIO VALUE (NAV)",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "₹" + String.format(Locale.US, "%,.2f", uiState.summary.totalNav),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val isTodayPos = uiState.summary.todayPnl >= 0.0
                                val todayPnlColor = if (isTodayPos) ProfitGreen else LossRed
                                val todayPnlSign = if (isTodayPos) "+" else ""
                                Text(
                                    text = String.format(Locale.US, "%s₹%,.0f (%s%.2f%%)", todayPnlSign, uiState.summary.todayPnl, todayPnlSign, uiState.summary.todayPnlPct),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = todayPnlColor,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCard,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "TOTAL GAIN",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                val isTotalPos = uiState.summary.totalPnl >= 0.0
                                val totalColor = if (isTotalPos) ProfitGreen else LossRed
                                val totalSign = if (isTotalPos) "+" else ""
                                Text(
                                    text = String.format(Locale.US, "%s%.2f%%", totalSign, uiState.summary.totalPnlPct),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = totalColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Market Benchmark Indices (Nifty 50, Smlcap 250, Midcap 150, 500 Mom 50) - Change % Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val indicesToDisplay = if (uiState.marketIndices.isNotEmpty()) uiState.marketIndices else listOf(uiState.nifty)
                        indicesToDisplay.take(4).forEach { item ->
                            val isPos = item.changePct >= 0.0
                            val changeColor = if (isPos) ProfitGreen else LossRed
                            val badgeBg = if (isPos) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                            val badgeBorder = if (isPos) ProfitGreen.copy(alpha = 0.35f) else LossRed.copy(alpha = 0.35f)

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF161B22),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.shortDisplayName,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = badgeBg,
                                        border = androidx.compose.foundation.BorderStroke(0.6.dp, badgeBorder)
                                    ) {
                                        Text(
                                            text = item.formattedChangePct,
                                            fontSize = 9.8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = changeColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // 1. Individual Tickers Table (Symbol, % up/down, Value, Target Toggle)
            HoldingsTable(
                positions = uiState.positions,
                onExitClick = { viewModel.requestExit(it) },
                onToggleProfitTarget = { viewModel.toggleProfitTarget(it) }
            )

            // 2. Portfolio Header Card (VARA ALPHA, NAV, Returns, Capital Metrics)
            PortfolioHeaderCard(summary = uiState.summary)

            // 3. Today's Ticker Changes Table (Ticker, Price, Today Chg %, Today Value ₹)
            TodayTickerChangesTable(
                positions = uiState.positions,
                otherPositions = uiState.otherPositions,
                summaryTodayPnl = uiState.summary.todayPnl
            )

            // 4. Dedicated Daily Returns Section
            DailyReturnsTable(
                dailyReturns = uiState.dailyReturns,
                onDailyReturnClick = { viewModel.selectDailyReturn(it) }
            )

            // 5. Recent Transactions Table (Date, Buy/Sell, Ticker, Price, Profit/Loss Difference)
            TransactionsTable(
                transactions = uiState.transactions
            )

            // 6. Top 5 Stock Recommendations Table (Rank, Ticker, Price, Score, Action, History)
            StockRecommendationsTable(
                recommendations = uiState.recommendations,
                onRecommendationClick = { viewModel.selectRecommendation(it) },
                onViewHistoryClick = { viewModel.openRecommendationHistory() },
                historyWinRateSummary = "${uiState.recommendationHistorySummary.formattedWinRate} Win Rate"
            )

            // 7. Past Recommendations Section (Interactive 3-Month Track Record with Google Finance link)
            PastRecommendationsSection(
                history = uiState.recommendationHistory,
                summary = uiState.recommendationHistorySummary
            )

            // 8. Real-World Live Execution Milestone Card (Daily Recalculated Health & Edge Verdict)
            LiveMilestoneCard(
                milestoneState = uiState.liveMilestoneState
            )

            Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(fnoScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        VeeraLakshmiScreen(
                            state = veeraLakshmiState,
                            onSelectInstance = { viewModel.selectFnoInstance(it) },
                            onRefresh = { viewModel.refreshFno() }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(arjunScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ArjunStocksScreen(
                            state = arjunStocksState,
                            onAddRecommendation = remember(viewModel) { { text: String -> viewModel.addArjunRecommendation(text) } },
                            onUpdateStock = remember(viewModel) { { item: com.example.varalakshmiportfolio.model.ArjunStockItem -> viewModel.updateArjunStock(item) } },
                            onDeleteStock = remember(viewModel) { { id: String -> viewModel.deleteArjunStock(id) } },
                            onCloseStock = remember(viewModel) { { id: String, exitPrice: Double -> viewModel.closeArjunStock(id, exitPrice) } },
                            onSelectFilter = remember(viewModel) { { filter: String -> viewModel.selectArjunFilter(filter) } },
                            onRefresh = remember(viewModel) { { viewModel.refreshArjunQuotes() } },
                            onApproveStock = remember(viewModel) { { id: String -> viewModel.approveArjunStock(id) } },
                            onUnapproveStock = remember(viewModel) { { id: String -> viewModel.unapproveArjunStock(id) } },
                            onDismissExecutionEvent = remember(viewModel) { { id: String -> viewModel.dismissAutoExecutionEvent(id) } }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
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

    // Daily Return Session Trade Breakdown Modal Dialog
    uiState.selectedDailyReturnForDetail?.let { dailyReturn ->
        DailyReturnDetailDialog(
            dailyReturn = dailyReturn,
            onDismiss = { viewModel.dismissDailyReturnDetail() }
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
                        text = "Are you sure you want to trigger a live market exit for ${target.symbol}?",
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Execution: Zerodha Kite CNC Market Sell", color = TextSecondary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmExit() },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    enabled = !uiState.isExitingPosition
                ) {
                    Text(
                        if (uiState.isExitingPosition) "Submitting..." else "Confirm Exit (✕)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissExit() },
                    enabled = !uiState.isExitingPosition
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Broker / Exit Order Failure Alert Dialog
    uiState.exitErrorDialog?.let { exitError ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { viewModel.dismissExitError() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = LossRed
                )
            },
            title = {
                Text(
                    text = "Zerodha Order Not Issued",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "The exit order for ${exitError.symbol} (${exitError.quantity} shares) could not be placed on Zerodha Kite.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LossRed.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Rejection / Failure Reason:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                exitError.errorMessage,
                                color = LossRedLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Status: This position remains OPEN in your Zerodha portfolio. You can exit directly on Zerodha Kite.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(exitError.kiteUrl)
                        } catch (e: Throwable) {
                            // UriHandler fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05338))
                ) {
                    Text("Open Zerodha Kite ↗", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissExitError() }) {
                    Text("Dismiss", color = TextSecondary)
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

                    // Auto-Sync Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync (1 Min)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically syncs every 1 minute during market hours (09:15 – 15:30 IST). Pauses outside market hours.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentIndigo,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!uiState.isAutoSyncEnabled) DarkCard
                        else if (uiState.isMarketOpen) ProfitGreenBg
                        else DarkCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!uiState.isAutoSyncEnabled) DarkCardBorder
                            else if (uiState.isMarketOpen) ProfitGreen.copy(alpha = 0.4f)
                            else DarkCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!uiState.isAutoSyncEnabled) TextMuted
                                        else if (uiState.isMarketOpen) ProfitGreen
                                        else GoldAccent
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!uiState.isAutoSyncEnabled) "Auto-sync disabled"
                                else if (uiState.isMarketOpen) "🟢 Market Open: Active (Syncs every 15m)"
                                else "⏸️ Market Closed: Paused until 09:15 IST",
                                fontSize = 10.5.sp,
                                color = if (!uiState.isAutoSyncEnabled) TextMuted
                                else if (uiState.isMarketOpen) ProfitGreen
                                else GoldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Always-On Live Screener Settings Row
                    var isScreenerEnabled by remember { mutableStateOf(FloatingScreenerManager.isScreenerEnabled(context)) }
                    var hasOverlayPermission by remember { mutableStateOf(FloatingScreenerManager.canDrawOverlays(context)) }
                    var showScreenerPermissionDialog by remember { mutableStateOf(false) }

                    val screenerLifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(screenerLifecycleOwner, context) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                hasOverlayPermission = FloatingScreenerManager.canDrawOverlays(context)
                                val enabled = FloatingScreenerManager.isScreenerEnabled(context)
                                isScreenerEnabled = enabled
                                if (enabled && hasOverlayPermission) {
                                    FloatingScreenerService.startService(context)
                                }
                            }
                        }
                        screenerLifecycleOwner.lifecycle.addObserver(observer)

                        val prefs = context.getSharedPreferences(FloatingScreenerManager.PREFS_NAME, Context.MODE_PRIVATE)
                        val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                            if (key == FloatingScreenerManager.KEY_SCREENER_ENABLED) {
                                isScreenerEnabled = FloatingScreenerManager.isScreenerEnabled(prefs)
                                hasOverlayPermission = FloatingScreenerManager.canDrawOverlays(context)
                            }
                        }
                        prefs.registerOnSharedPreferenceChangeListener(prefListener)

                        onDispose {
                            screenerLifecycleOwner.lifecycle.removeObserver(observer)
                            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Always-On Live Screener",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isScreenerEnabled && hasOverlayPermission) ProfitGreenBg else DarkCardBorder.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = if (isScreenerEnabled && hasOverlayPermission) "ACTIVE" else "OFF",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isScreenerEnabled && hasOverlayPermission) ProfitGreen else TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Displays an always-on floating horizontal stock ticker overlay with live P&L and % changes above other apps.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isScreenerEnabled && hasOverlayPermission,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    FloatingScreenerManager.setScreenerEnabled(context, true)
                                    isScreenerEnabled = true
                                    if (FloatingScreenerManager.canDrawOverlays(context)) {
                                        hasOverlayPermission = true
                                        FloatingScreenerService.startService(context)
                                    } else {
                                        hasOverlayPermission = false
                                        showScreenerPermissionDialog = true
                                    }
                                } else {
                                    FloatingScreenerManager.setScreenerEnabled(context, false)
                                    isScreenerEnabled = false
                                    FloatingScreenerService.stopService(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentIndigo,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCard
                            )
                        )
                    }

                    if (showScreenerPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showScreenerPermissionDialog = false
                                if (!FloatingScreenerManager.canDrawOverlays(context)) {
                                    FloatingScreenerManager.setScreenerEnabled(context, false)
                                    isScreenerEnabled = false
                                }
                            },
                            title = {
                                Text(
                                    text = "Overlay Permission Required",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                val miuiHint = if (FloatingScreenerManager.isMiui()) {
                                    "\n\nNote for Xiaomi / HyperOS / MIUI: Also ensure 'Display pop-up windows while running in background' is enabled in App Permissions."
                                } else ""
                                Text(
                                    text = "To display the always-on floating live stock screener above other apps, Varalakshmi Portfolio requires the 'Display over other apps' system permission.\n\nTap 'Grant Permission' to open Settings and enable it.$miuiHint",
                                    color = TextSecondary,
                                    fontSize = 12.5.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showScreenerPermissionDialog = false
                                        try {
                                            context.startActivity(FloatingScreenerManager.createOverlayPermissionIntent(context))
                                        } catch (_: Throwable) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                                ) {
                                    Text("Grant Permission", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showScreenerPermissionDialog = false
                                    FloatingScreenerManager.setScreenerEnabled(context, false)
                                    isScreenerEnabled = false
                                }) {
                                    Text("Cancel", color = TextMuted)
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    Text(
                        text = "App Updates & Releases (Current: v2.4.1)",
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
                                safeOpenUri("https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/latest/download/varalakshmi-portfolio.apk")
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
                            Text("Download APK (GitHub)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                val sUrl = tempUrl.trim().trimEnd('/')
                                val downloadUrl = if (sUrl.isNotBlank()) "$sUrl/download/latest.apk?t=${System.currentTimeMillis()}"
                                else "https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/latest/download/varalakshmi-portfolio.apk"
                                safeOpenUri(downloadUrl)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Download from Live Server",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Server Direct", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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

@Composable
fun DailyReturnsTable(
    dailyReturns: List<DailyReturnItem>,
    onDailyReturnClick: (DailyReturnItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

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
            // Section Header (Clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Daily Returns",
                        tint = AccentIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DAILY RETURNS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val sessionBadgeText = if (dailyReturns.size == 1) "1 Session" else "${dailyReturns.size} Sessions"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text(
                            text = sessionBadgeText,
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var isTableCopied by remember { mutableStateOf(false) }
                    val tableClipboard = LocalClipboardManager.current
                    val tableContext = LocalContext.current
                    val tableScope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            val textToCopy = buildString {
                                appendLine("================================================================================")
                                appendLine("VARALAKSHMI EQUITY - DAILY RETURNS STATEMENT (${dailyReturns.size} Sessions)")
                                appendLine("================================================================================")
                                appendLine(String.format(Locale.US, "%-12s | %-16s | %-14s | %-8s | %-6s | %-6s",
                                    "DATE", "FUND BALANCE", "DAILY P&L", "RETURN %", "STATUS", "TRADES"))
                                appendLine("--------------------------------------------------------------------------------")
                                for (d in dailyReturns) {
                                    appendLine(String.format(Locale.US, "%-12s | %-16s | %-14s | %-8s | %-6s | %-6d",
                                        d.date, String.format(Locale.US, "₹%,.2f", d.totalNav), d.formattedPnl, d.formattedReturnPct, d.status, d.tradeCount))
                                }
                                appendLine("================================================================================")
                            }
                            tableClipboard.setText(AnnotatedString(textToCopy))
                            Toast.makeText(tableContext, "Daily returns statement copied!", Toast.LENGTH_SHORT).show()
                            tableScope.launch {
                                isTableCopied = true
                                delay(2000L)
                                isTableCopied = false
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isTableCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copy Daily Returns",
                            tint = if (isTableCopied) ProfitGreen else AccentIndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
                                modifier = Modifier.weight(1.05f)
                            )
                            Text(
                                text = "FUND BAL",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.25f)
                            )
                            Text(
                                text = "DAILY P&L",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.20f)
                            )
                            Text(
                                text = "RETURN %",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.95f)
                            )
                            Text(
                                text = "STATUS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.65f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (dailyReturns.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No historical daily returns recorded yet",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dailyReturns.forEachIndexed { index, item ->
                                key("${item.date}_$index") {
                                    DailyReturnRow(
                                        item = item,
                                        onRowClick = onDailyReturnClick
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
private fun DailyReturnRow(
    item: DailyReturnItem,
    onRowClick: (DailyReturnItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable { onRowClick(item) },
        shape = RoundedCornerShape(8.dp),
        color = DarkCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DATE column
            Column(modifier = Modifier.weight(1.05f)) {
                Text(
                    text = item.date.ifBlank { "—" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.hasCapitalInflow) {
                    Text(
                        text = if (item.capitalInflow > 0) "+TOP-UP" else "WITHDRAWAL",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.capitalInflow > 0) AccentIndigoLight else LossRed,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (item.tradeCount > 0) {
                    Text(
                        text = "${item.tradeCount} trade${if (item.tradeCount > 1) "s" else ""}",
                        fontSize = 9.sp,
                        color = TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // FUND BAL column
            Text(
                text = item.formattedNav,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.25f)
            )

            // DAILY P&L column
            val pnlColor = when {
                item.dailyPnl >= 0.005 -> ProfitGreen
                item.dailyPnl <= -0.005 -> LossRed
                else -> TextSecondary
            }
            Text(
                text = item.formattedPnl,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = pnlColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.20f)
            )

            // RETURN % column
            val returnColor = when {
                item.returnPct >= 0.005 -> ProfitGreen
                item.returnPct <= -0.005 -> LossRed
                else -> TextSecondary
            }
            Text(
                text = item.formattedReturnPct,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = returnColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.95f)
            )

            // STATUS badge column
            val statusUpper = item.status.uppercase(Locale.US)
            val displayStatus = when (statusUpper) {
                "WIN", "PROFIT", "GAIN" -> "WIN"
                "LOSS", "DEFICIT" -> "LOSS"
                "FLAT", "NEUTRAL", "EVEN", "PRE_INCEPTION", "BREAKEVEN" -> "FLAT"
                else -> if (statusUpper.length > 5) statusUpper.take(4) else statusUpper
            }
            val (badgeBg, badgeTextColor) = when (displayStatus) {
                "WIN" -> Pair(ProfitGreenBg, ProfitGreen)
                "LOSS" -> Pair(LossRedBg, LossRed)
                else -> Pair(DarkCardBorder.copy(alpha = 0.5f), TextSecondary)
            }
            Box(
                modifier = Modifier.weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = displayStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
