package com.example.varalakshmiportfolio.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.theme.*
import com.example.varalakshmiportfolio.ui.components.HoldingsTable
import com.example.varalakshmiportfolio.ui.components.PortfolioHeaderCard
import com.example.varalakshmiportfolio.ui.components.TransactionsTable
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaralakshmiDashboardScreen(
    repository: VaralakshmiRepository = remember { VaralakshmiRepository() },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var summary by remember { mutableStateOf(repository.getCachedSummary()) }
    var positions by remember { mutableStateOf(repository.getCachedPositions()) }
    var transactions by remember { mutableStateOf(repository.getCachedTransactions()) }

    var isRefreshing by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("http://10.0.2.2:8088") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var positionToExit by remember { mutableStateOf<PositionItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refresh() {
        if (isRefreshing) return
        coroutineScope.launch {
            isRefreshing = true
            try {
                val (newSummary, newPositions, newTransactions) = repository.refreshData(serverUrl)
                summary = newSummary
                positions = newPositions
                transactions = newTransactions
                snackbarHostState.showSnackbar("Synced with live trading engine")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Loaded cached portfolio data")
            } finally {
                isRefreshing = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

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
                        onClick = { refresh() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = AccentIndigoLight,
                            modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
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
            // Last Updated bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "As of ${summary.lastUpdated}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = if (isRefreshing) "Syncing..." else "Connected",
                    fontSize = 11.sp,
                    color = if (isRefreshing) GoldAccent else ProfitGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            // 1. Portfolio Header Card (NAV, Returns, Capital Metrics)
            PortfolioHeaderCard(summary = summary)

            // 2. Individual Tickers Table (Symbol, % up/down, Value, X)
            HoldingsTable(
                positions = positions,
                onExitClick = { positionToExit = it }
            )

            // 3. Recent Transactions Table (Date, Buy/Sell, Ticker, Price, Profit/Loss Difference)
            TransactionsTable(
                transactions = transactions
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Exit Position Confirmation Modal
    if (positionToExit != null) {
        val target = positionToExit!!
        AlertDialog(
            onDismissRequest = { positionToExit = null },
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
                            Text("Current Price: ₹${target.currentPrice}", color = TextPrimary, fontSize = 12.sp)
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
                    onClick = {
                        val (newSummary, newPositions) = repository.removePosition(target.symbol)
                        summary = newSummary
                        positions = newPositions
                        positionToExit = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Exited ${target.symbol} • Freed up ₹${String.format(Locale.US, "%,.0f", target.marketValue)}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Confirm Exit (✕)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { positionToExit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Server Settings Dialog
    if (showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(serverUrl) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Live Server Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
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
                        text = "• Android Emulator: http://10.0.2.2:8088\n• Physical Phone: http://<PC-LAN-IP>:8088",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        serverUrl = tempUrl
                        showSettingsDialog = false
                        refresh()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Save & Sync", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
