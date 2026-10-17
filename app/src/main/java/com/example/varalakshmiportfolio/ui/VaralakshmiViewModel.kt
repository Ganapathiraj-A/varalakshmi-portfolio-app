package com.example.varalakshmiportfolio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.varalakshmiportfolio.data.SyncResult
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.MarketIndexItem
import com.example.varalakshmiportfolio.model.StockRecommendationItem
import com.example.varalakshmiportfolio.model.HistoricalRecommendationItem
import com.example.varalakshmiportfolio.model.RecommendationHistorySummary
import com.example.varalakshmiportfolio.model.IpoActionNotification
import com.example.varalakshmiportfolio.model.VeeraLakshmiUiState
import com.example.varalakshmiportfolio.model.SanthanaLakshmiUiState
import com.example.varalakshmiportfolio.model.SanthanaLakshmiTradeItem
import com.example.varalakshmiportfolio.service.FloatingScreenerService
import com.example.varalakshmiportfolio.util.MarketHours
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.varalakshmiportfolio.model.DailyReturnItem
import com.example.varalakshmiportfolio.data.ArjunRecommendationParser
import com.example.varalakshmiportfolio.data.ArjunStockRepository
import com.example.varalakshmiportfolio.model.ArjunStockItem
import com.example.varalakshmiportfolio.model.ArjunStocksUiState
import com.example.varalakshmiportfolio.model.ArjunPortfolioSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExitOrderError(
    val symbol: String,
    val quantity: Int,
    val errorMessage: String,
    val kiteUrl: String = "https://kite.zerodha.com/holdings"
)

data class VaralakshmiUiState(
    val summary: PortfolioSummary,
    val positions: List<PositionItem>,
    val otherPositions: List<PositionItem> = emptyList(),
    val transactions: List<TransactionItem>,
    val dailyReturns: List<DailyReturnItem> = emptyList(),
    val nifty: MarketIndexItem = MarketIndexItem(),
    val marketIndices: List<MarketIndexItem> = com.example.varalakshmiportfolio.model.createDefaultMarketIndices(),
    val recommendations: List<StockRecommendationItem> = emptyList(),
    val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
    val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0),
    val ipoNotifications: List<IpoActionNotification> = emptyList(),
    val selectedRecommendationForChart: StockRecommendationItem? = null,
    val selectedDailyReturnForDetail: DailyReturnItem? = null,
    val showRecommendationHistory: Boolean = false,
    val selectedHistoryFilter: String = "ALL",
    val isRefreshing: Boolean = false,
    val serverUrl: String = VaralakshmiRepository.DEFAULT_SERVER_URL,
    val authToken: String = VaralakshmiRepository.DEFAULT_AUTH_TOKEN,
    val showSettingsDialog: Boolean = false,
    val positionToExit: PositionItem? = null,
    val exitErrorDialog: ExitOrderError? = null,
    val isExitingPosition: Boolean = false,
    val snackbarMessage: String? = null,
    val isLiveSync: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val isMarketOpen: Boolean = false,
    val marketStatusText: String = "",
    val liveMilestoneState: com.example.varalakshmiportfolio.model.LiveMilestoneState = com.example.varalakshmiportfolio.model.calculateLiveMilestones(transactions, positions)
) {
    val filteredRecommendationHistory: List<HistoricalRecommendationItem>
        get() = when (selectedHistoryFilter.uppercase(Locale.US)) {
            "PROFITABLE", "WINS" -> recommendationHistory.filter { it.pnlPercent > 0.0 }
            "LOSS", "LOSSES" -> recommendationHistory.filter { it.pnlPercent < 0.0 }
            "ACTIVE" -> recommendationHistory.filter { it.isActive }
            else -> recommendationHistory
        }
}

class VaralakshmiViewModel(
    private val repository: VaralakshmiRepository = VaralakshmiRepository(),
    autoRefresh: Boolean = true,
    private val enablePeriodicSync: Boolean = autoRefresh,
    val syncIntervalMillis: Long = 1 * 60 * 1000L,
    private val checkIntervalMillis: Long = 10_000L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VaralakshmiUiState(
            summary = repository.getCachedSummary(),
            positions = repository.getCachedPositions(),
            otherPositions = repository.getCachedOtherPositions(),
            transactions = repository.getCachedTransactions(),
            dailyReturns = repository.getCachedDailyReturns(),
            nifty = repository.getCachedNifty(),
            marketIndices = repository.getCachedMarketIndices(),
            recommendations = repository.getCachedRecommendations(),
            recommendationHistory = repository.getCachedRecommendationHistory(),
            recommendationHistorySummary = repository.getCachedRecommendationHistorySummary(),
            ipoNotifications = repository.getCachedIpoNotifications(),
            serverUrl = repository.getServerUrl(),
            authToken = repository.getAuthToken(),
            isAutoSyncEnabled = repository.isAutoSyncEnabled(),
            lastSyncTimestamp = System.currentTimeMillis(),
            isMarketOpen = MarketHours.isMarketHours(),
            marketStatusText = MarketHours.getMarketStatusText()
        )
    )
    val uiState: StateFlow<VaralakshmiUiState> = _uiState.asStateFlow()

    private val _veeraLakshmiState = MutableStateFlow(repository.getCachedFnoState())
    val veeraLakshmiState: StateFlow<VeeraLakshmiUiState> = _veeraLakshmiState.asStateFlow()

    private val _santhanaLakshmiState = MutableStateFlow(repository.getCachedSanthanaState())
    val santhanaLakshmiState: StateFlow<SanthanaLakshmiUiState> = _santhanaLakshmiState.asStateFlow()

    private val arjunRepository: ArjunStockRepository = ArjunStockRepository()
    private val _arjunStocksState = MutableStateFlow(
        run {
            val initial = arjunRepository.getRecommendations()
            ArjunStocksUiState(
                recommendations = initial,
                summary = arjunRepository.calculateSummary(initial),
                strategySummary = arjunRepository.calculateStrategySummary(initial),
                strategySlots = arjunRepository.calculateStrategySlots(initial),
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents()
            )
        }
    )
    val arjunStocksState: StateFlow<ArjunStocksUiState> = _arjunStocksState.asStateFlow()

    private var periodicSyncJob: Job? = null
    private var fnoSyncJob: Job? = null
    private var santhanaSyncJob: Job? = null
    private var arjunSyncJob: Job? = null

    init {
        if (autoRefresh) {
            try {
                refresh()
            } catch (e: Throwable) {
                // Safeguard against unconfigured Main dispatcher in headless JVM tests
            }
        }
        if (enablePeriodicSync) {
            try {
                startPeriodicSync()
            } catch (e: Throwable) {
                // Safeguard against unconfigured Main dispatcher in headless JVM tests
            }
        }
    }

    fun selectFnoInstance(instanceId: String) {
        val updated = repository.selectFnoInstance(instanceId)
        _veeraLakshmiState.value = updated.copy(isLoading = true)
        fnoSyncJob?.cancel()
        fnoSyncJob = viewModelScope.launch {
            try {
                val synced = repository.syncFno(_uiState.value.serverUrl, instanceId, _uiState.value.authToken)
                if (_veeraLakshmiState.value.selectedInstanceId == instanceId) {
                    _veeraLakshmiState.value = synced
                }
            } catch (_: Exception) {
                if (_veeraLakshmiState.value.selectedInstanceId == instanceId) {
                    _veeraLakshmiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun refreshFno() {
        fnoSyncJob?.cancel()
        val currentId = _veeraLakshmiState.value.selectedInstanceId
        fnoSyncJob = viewModelScope.launch {
            _veeraLakshmiState.update { it.copy(isLoading = true) }
            try {
                val synced = repository.syncFno(_uiState.value.serverUrl, currentId, _uiState.value.authToken)
                if (_veeraLakshmiState.value.selectedInstanceId == currentId) {
                    _veeraLakshmiState.value = synced
                }
            } catch (e: Exception) {
                if (_veeraLakshmiState.value.selectedInstanceId == currentId) {
                    _veeraLakshmiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            }
        }
    }

    fun selectSanthanaFilter(filter: String) {
        _santhanaLakshmiState.update { it.copy(selectedFilter = filter) }
    }

    fun selectSanthanaTradeDetail(trade: SanthanaLakshmiTradeItem?) {
        _santhanaLakshmiState.update { it.copy(selectedTradeDetail = trade) }
    }

    fun refreshSanthana() {
        santhanaSyncJob?.cancel()
        santhanaSyncJob = viewModelScope.launch {
            _santhanaLakshmiState.update { it.copy(isLoading = true) }
            try {
                val synced = repository.refreshSanthana(_uiState.value.serverUrl, _uiState.value.authToken)
                _santhanaLakshmiState.value = synced
            } catch (e: Exception) {
                _santhanaLakshmiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun confirmIpoMandate(symbol: String, newStatus: String = "APPROVED") {
        val updatedState = repository.updateIpoMandateStatus(symbol, newStatus)
        _santhanaLakshmiState.value = updatedState
    }

    fun updateIpoAllotmentStatus(symbol: String, newStatus: String) {
        val updatedState = repository.updateIpoAllotmentStatus(symbol, newStatus)
        _santhanaLakshmiState.value = updatedState
    }

    fun refresh(isSilent: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            _veeraLakshmiState.update { it.copy(isLoading = true) }
            _santhanaLakshmiState.update { it.copy(isLoading = true) }

            // Concurrent F&O sync
            fnoSyncJob?.cancel()
            val targetInstanceId = _veeraLakshmiState.value.selectedInstanceId
            fnoSyncJob = launch {
                try {
                    val fnoSynced = repository.syncFno(_uiState.value.serverUrl, targetInstanceId, _uiState.value.authToken)
                    if (_veeraLakshmiState.value.selectedInstanceId == targetInstanceId) {
                        _veeraLakshmiState.value = fnoSynced
                    }
                } catch (_: Exception) {
                    _veeraLakshmiState.update { it.copy(isLoading = false) }
                }
            }

            // Concurrent SanthanaLakshmi sync
            santhanaSyncJob?.cancel()
            santhanaSyncJob = launch {
                try {
                    val santhanaSynced = repository.refreshSanthana(_uiState.value.serverUrl, _uiState.value.authToken)
                    _santhanaLakshmiState.value = santhanaSynced
                } catch (_: Exception) {
                    _santhanaLakshmiState.update { it.copy(isLoading = false) }
                }
            }

            // Concurrent Arjun live quotes sync
            arjunSyncJob?.cancel()
            if (!ArjunStockRepository.enableNetworkQuotes) {
                val current = arjunRepository.getRecommendations()
                val summary = arjunRepository.calculateSummary(current)
                val strategySummary = arjunRepository.calculateStrategySummary(current)
                val strategySlots = arjunRepository.calculateStrategySlots(current)
                val events = arjunRepository.getAutoExecutionEvents()
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _arjunStocksState.update {
                    it.copy(
                        recommendations = current,
                        summary = summary,
                        strategySummary = strategySummary,
                        strategySlots = strategySlots,
                        autoExecutionEvents = events,
                        isRefreshingQuotes = false,
                        lastRefreshedTime = timeStr,
                        errorMessage = null
                    )
                }
            } else {
                arjunSyncJob = launch {
                    try {
                        _arjunStocksState.update { it.copy(isRefreshingQuotes = true) }
                        val updated = arjunRepository.refreshLiveQuotes()
                        val summary = arjunRepository.calculateSummary(updated)
                        val strategySummary = arjunRepository.calculateStrategySummary(updated)
                        val strategySlots = arjunRepository.calculateStrategySlots(updated)
                        val events = arjunRepository.getAutoExecutionEvents()
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        _arjunStocksState.update {
                            it.copy(
                                recommendations = updated,
                                summary = summary,
                                strategySummary = strategySummary,
                                strategySlots = strategySlots,
                                autoExecutionEvents = events,
                                isRefreshingQuotes = false,
                                lastRefreshedTime = timeStr,
                                errorMessage = null
                            )
                        }
                    } catch (e: Throwable) {
                        _arjunStocksState.update {
                            it.copy(
                                isRefreshingQuotes = false,
                                errorMessage = e.message
                            )
                        }
                    }
                }
            }

            val syncResult = repository.refreshData(_uiState.value.serverUrl, _uiState.value.authToken)
            val now = System.currentTimeMillis()
            val isMarket = MarketHours.isMarketHours()
            val statusText = MarketHours.getMarketStatusText()
            when (syncResult) {
                is SyncResult.Success -> {
                    _uiState.update {
                        it.copy(
                            summary = syncResult.summary,
                            positions = syncResult.positions,
                            otherPositions = syncResult.otherPositions,
                            transactions = syncResult.transactions,
                            dailyReturns = syncResult.dailyReturns,
                            nifty = syncResult.nifty,
                            marketIndices = syncResult.marketIndices.ifEmpty { it.marketIndices },
                            recommendations = syncResult.recommendations,
                            recommendationHistory = syncResult.recommendationHistory,
                            recommendationHistorySummary = syncResult.recommendationHistorySummary,
                            ipoNotifications = syncResult.ipoNotifications,
                            isRefreshing = false,
                            isLiveSync = true,
                            lastSyncTimestamp = now,
                            isMarketOpen = isMarket,
                            marketStatusText = statusText,
                            snackbarMessage = if (isSilent) null else "Synced with live trading engine",
                            liveMilestoneState = com.example.varalakshmiportfolio.model.calculateLiveMilestones(syncResult.transactions, syncResult.positions, timestampMillis = now)
                        )
                    }
                }
                is SyncResult.OfflineCacheFallback -> {
                    _uiState.update {
                        it.copy(
                            summary = syncResult.summary,
                            positions = syncResult.positions,
                            otherPositions = syncResult.otherPositions,
                            transactions = syncResult.transactions,
                            dailyReturns = syncResult.dailyReturns,
                            nifty = syncResult.nifty,
                            marketIndices = syncResult.marketIndices.ifEmpty { it.marketIndices },
                            recommendations = syncResult.recommendations,
                            recommendationHistory = syncResult.recommendationHistory,
                            recommendationHistorySummary = syncResult.recommendationHistorySummary,
                            ipoNotifications = syncResult.ipoNotifications,
                            isRefreshing = false,
                            isLiveSync = false,
                            lastSyncTimestamp = now,
                            isMarketOpen = isMarket,
                            marketStatusText = statusText,
                            snackbarMessage = if (isSilent) null else "Offline mode: showing cached snapshot",
                            liveMilestoneState = com.example.varalakshmiportfolio.model.calculateLiveMilestones(syncResult.transactions, syncResult.positions, timestampMillis = now)
                        )
                    }
                }
            }
            try {
                FloatingScreenerService.updateData(syncResult.summary, syncResult.positions)
            } catch (_: Throwable) {}
        }
    }

    fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(checkIntervalMillis)
                val now = System.currentTimeMillis()
                val isMarket = MarketHours.isMarketHours()
                val statusText = MarketHours.getMarketStatusText()

                _uiState.update {
                    it.copy(
                        isMarketOpen = isMarket,
                        marketStatusText = statusText
                    )
                }

                if (_uiState.value.isAutoSyncEnabled && isMarket) {
                    val lastSync = _uiState.value.lastSyncTimestamp
                    if (now - lastSync >= syncIntervalMillis) {
                        refresh(isSilent = true)
                    }
                }
            }
        }
    }

    fun stopPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }

    fun onAppResume() {
        val isMarket = MarketHours.isMarketHours()
        val now = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isMarketOpen = isMarket,
                marketStatusText = MarketHours.getMarketStatusText()
            )
        }
        if (_uiState.value.isAutoSyncEnabled && isMarket) {
            val lastSync = _uiState.value.lastSyncTimestamp
            if (now - lastSync >= syncIntervalMillis) {
                refresh(isSilent = true)
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        repository.setAutoSyncEnabled(enabled)
        _uiState.update { it.copy(isAutoSyncEnabled = enabled) }
        if (enabled && MarketHours.isMarketHours()) {
            val now = System.currentTimeMillis()
            if (now - _uiState.value.lastSyncTimestamp >= syncIntervalMillis) {
                refresh(isSilent = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicSync()
        fnoSyncJob?.cancel()
        santhanaSyncJob?.cancel()
        arjunSyncJob?.cancel()
    }

    fun openRecommendationHistory() {
        _uiState.update { it.copy(showRecommendationHistory = true) }
    }

    fun closeRecommendationHistory() {
        _uiState.update { it.copy(showRecommendationHistory = false) }
    }

    fun setHistoryFilter(filter: String) {
        _uiState.update { it.copy(selectedHistoryFilter = filter) }
    }

    fun selectRecommendation(recommendation: StockRecommendationItem) {
        _uiState.update { it.copy(selectedRecommendationForChart = recommendation) }
    }

    fun dismissRecommendationChart() {
        _uiState.update { it.copy(selectedRecommendationForChart = null) }
    }

    fun selectDailyReturn(dailyReturn: DailyReturnItem) {
        _uiState.update { it.copy(selectedDailyReturnForDetail = dailyReturn) }
    }

    fun dismissDailyReturnDetail() {
        _uiState.update { it.copy(selectedDailyReturnForDetail = null) }
    }

    fun requestExit(position: PositionItem) {
        _uiState.update { it.copy(positionToExit = position) }
    }

    fun dismissExit() {
        _uiState.update { it.copy(positionToExit = null) }
    }

    fun confirmExit() {
        val target = _uiState.value.positionToExit ?: return
        val stratId = _uiState.value.summary.strategyId.ifBlank { "VARALAKSHMI_ALPHA_SCALE_35" }
        val serverUrl = _uiState.value.serverUrl
        val authToken = _uiState.value.authToken

        val (newSummary, newPositions, newTransactions) = repository.removePosition(target.symbol)
        _uiState.update {
            it.copy(
                summary = newSummary,
                positions = newPositions,
                transactions = newTransactions,
                positionToExit = null,
                isExitingPosition = true,
                snackbarMessage = "Exited ${target.symbol} • Submitting live exit order to Zerodha Kite..."
            )
        }

        try {
            viewModelScope.launch {
                val res = repository.exitPosition(
                    strategyId = stratId,
                    symbol = target.symbol,
                    serverBaseUrl = serverUrl,
                    authToken = authToken
                )
                res.fold(
                    onSuccess = { msg ->
                        _uiState.update { current ->
                            current.copy(
                                isExitingPosition = false,
                                snackbarMessage = "Exited ${target.symbol} • $msg"
                            )
                        }
                        refresh()
                    },
                    onFailure = { err ->
                        val errMsg = err.message ?: "Broker order failed or was rejected"
                        val isOffline = errMsg.contains("Offline", ignoreCase = true)
                        if (!isOffline) {
                            // Online execution failed: Roll back and restore position to prevent misleading user
                            val (restoredSummary, restoredPositions, restoredTransactions) = repository.restorePosition(target)
                            _uiState.update { current ->
                                current.copy(
                                    summary = restoredSummary,
                                    positions = restoredPositions,
                                    transactions = restoredTransactions,
                                    isExitingPosition = false,
                                    exitErrorDialog = ExitOrderError(
                                        symbol = target.symbol,
                                        quantity = target.quantity,
                                        errorMessage = errMsg,
                                        kiteUrl = "https://kite.zerodha.com/holdings"
                                    ),
                                    snackbarMessage = "Zerodha exit failed for ${target.symbol}: $errMsg"
                                )
                            }
                        } else {
                            _uiState.update { current ->
                                current.copy(
                                    isExitingPosition = false,
                                    snackbarMessage = "Exited ${target.symbol} locally (Offline mode)"
                                )
                            }
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            // Safeguard against unconfigured Main dispatcher in headless JVM tests
        }
    }

    fun dismissExitError() {
        _uiState.update { it.copy(exitErrorDialog = null) }
    }

    fun toggleProfitTarget(position: PositionItem) {
        val newEnabled = !position.isProfitTargetEnabled
        // 1. Immediately update StateFlow (no UI jitter, no full list rebuild)
        _uiState.update { current ->
            val updatedPositions = current.positions.map {
                val matches = (position.positionId.isNotBlank() && it.positionId == position.positionId) ||
                        it.symbol.equals(position.symbol, ignoreCase = true)
                if (matches) {
                    it.copy(isProfitTargetEnabled = newEnabled)
                } else it
            }
            val targetLabel = if (newEnabled) "+35% Cap (${position.formattedProfitTargetPrice})" else "Uncapped Runner"
            current.copy(
                positions = updatedPositions,
                snackbarMessage = "${position.symbol}: Profit Target set to $targetLabel"
            )
        }

        // 2. Persist to repository (disk cache)
        repository.updateProfitTarget(position.symbol, newEnabled)

        // 3. Asynchronously update backend endpoint
        try {
            viewModelScope.launch {
                val ok = repository.syncProfitTargetToBackend(
                    strategyId = _uiState.value.summary.strategyId,
                    symbol = position.symbol,
                    enabled = newEnabled,
                    serverBaseUrl = _uiState.value.serverUrl,
                    authToken = _uiState.value.authToken
                )
                if (!ok) {
                    _uiState.update { current ->
                        current.copy(
                            snackbarMessage = "Warning: Failed to sync ${position.symbol} profit target to backend (saved offline)"
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            // Safeguard against unconfigured Main dispatcher in headless JVM tests
        }
    }

    fun toggleProfitTarget(symbol: String) {
        val target = _uiState.value.positions.find { it.symbol.equals(symbol, ignoreCase = true) } ?: return
        toggleProfitTarget(target)
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun updateServerUrl(newUrl: String, newToken: String = _uiState.value.authToken) {
        repository.setServerConfig(newUrl, newToken)
        _uiState.update { it.copy(serverUrl = newUrl, authToken = newToken, showSettingsDialog = false) }
        refresh()
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun dismissIpoNotification(id: String) {
        val updated = repository.dismissIpoNotification(id)
        _uiState.update { it.copy(ipoNotifications = updated) }
        _santhanaLakshmiState.update { current ->
            current.copy(
                activeAlerts = current.activeAlerts.map {
                    if (it.id == id || it.symbol.equals(id, ignoreCase = true)) it.copy(isDismissed = true) else it
                }
            )
        }
    }

    fun addArjunRecommendation(rawText: String) {
        val parsed = ArjunRecommendationParser.parse(rawText)
        if (parsed != null) {
            val updatedList = arjunRepository.addRecommendation(parsed)
            val updatedSummary = arjunRepository.calculateSummary(updatedList)
            val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
            val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
            _arjunStocksState.update {
                it.copy(
                    recommendations = updatedList,
                    summary = updatedSummary,
                    strategySummary = strategySummary,
                    strategySlots = strategySlots,
                    autoExecutionEvents = arjunRepository.getAutoExecutionEvents(),
                    errorMessage = null
                )
            }
            refreshArjunQuotes()
        } else {
            _arjunStocksState.update {
                it.copy(errorMessage = "Could not parse recommendation. Ensure it mentions BUY <SYMBOL>.")
            }
        }
    }

    fun updateArjunStock(item: ArjunStockItem) {
        val updatedList = arjunRepository.updateRecommendation(item)
        val updatedSummary = arjunRepository.calculateSummary(updatedList)
        val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
        val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
        _arjunStocksState.update {
            it.copy(
                recommendations = updatedList,
                summary = updatedSummary,
                strategySummary = strategySummary,
                strategySlots = strategySlots,
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents()
            )
        }
    }

    fun deleteArjunStock(id: String) {
        val updatedList = arjunRepository.deleteRecommendation(id)
        val updatedSummary = arjunRepository.calculateSummary(updatedList)
        val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
        val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
        _arjunStocksState.update {
            it.copy(
                recommendations = updatedList,
                summary = updatedSummary,
                strategySummary = strategySummary,
                strategySlots = strategySlots,
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents()
            )
        }
    }

    fun closeArjunStock(id: String, exitPrice: Double = 0.0) {
        val updatedList = arjunRepository.markAsClosed(id, exitPrice, "MANUAL_EXIT")
        val updatedSummary = arjunRepository.calculateSummary(updatedList)
        val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
        val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
        _arjunStocksState.update {
            it.copy(
                recommendations = updatedList,
                summary = updatedSummary,
                strategySummary = strategySummary,
                strategySlots = strategySlots,
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents()
            )
        }
    }

    fun approveArjunStock(id: String, targetSlot: Int? = null) {
        val (success, message) = arjunRepository.approveRecommendation(id, targetSlot)
        val updatedList = arjunRepository.getRecommendations()
        val updatedSummary = arjunRepository.calculateSummary(updatedList)
        val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
        val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
        _arjunStocksState.update {
            it.copy(
                recommendations = updatedList,
                summary = updatedSummary,
                strategySummary = strategySummary,
                strategySlots = strategySlots,
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents(),
                errorMessage = if (!success) message else null
            )
        }
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun unapproveArjunStock(id: String) {
        val updatedList = arjunRepository.unapproveRecommendation(id)
        val updatedSummary = arjunRepository.calculateSummary(updatedList)
        val strategySummary = arjunRepository.calculateStrategySummary(updatedList)
        val strategySlots = arjunRepository.calculateStrategySlots(updatedList)
        _arjunStocksState.update {
            it.copy(
                recommendations = updatedList,
                summary = updatedSummary,
                strategySummary = strategySummary,
                strategySlots = strategySlots,
                autoExecutionEvents = arjunRepository.getAutoExecutionEvents()
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Removed from Arjun Strategy slots.") }
    }

    fun dismissAutoExecutionEvent(id: String) {
        val updatedEvents = arjunRepository.dismissAutoExecutionEvent(id)
        _arjunStocksState.update { it.copy(autoExecutionEvents = updatedEvents) }
    }

    fun selectArjunFilter(filter: String) {
        _arjunStocksState.update { it.copy(selectedFilter = filter) }
    }

    fun refreshArjunQuotes() {
        refreshArjunQuotesInternal()
    }

    private fun refreshArjunQuotesInternal() {
        arjunSyncJob?.cancel()
        if (!ArjunStockRepository.enableNetworkQuotes) {
            val current = arjunRepository.getRecommendations()
            val summary = arjunRepository.calculateSummary(current)
            val strategySummary = arjunRepository.calculateStrategySummary(current)
            val strategySlots = arjunRepository.calculateStrategySlots(current)
            val events = arjunRepository.getAutoExecutionEvents()
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _arjunStocksState.update {
                it.copy(
                    recommendations = current,
                    summary = summary,
                    strategySummary = strategySummary,
                    strategySlots = strategySlots,
                    autoExecutionEvents = events,
                    isRefreshingQuotes = false,
                    lastRefreshedTime = timeStr,
                    errorMessage = null
                )
            }
            return
        }
        try {
            arjunSyncJob = viewModelScope.launch {
                try {
                    _arjunStocksState.update { it.copy(isRefreshingQuotes = true) }
                    val updated = arjunRepository.refreshLiveQuotes()
                    val summary = arjunRepository.calculateSummary(updated)
                    val strategySummary = arjunRepository.calculateStrategySummary(updated)
                    val strategySlots = arjunRepository.calculateStrategySlots(updated)
                    val events = arjunRepository.getAutoExecutionEvents()
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    _arjunStocksState.update {
                        it.copy(
                            recommendations = updated,
                            summary = summary,
                            strategySummary = strategySummary,
                            strategySlots = strategySlots,
                            autoExecutionEvents = events,
                            isRefreshingQuotes = false,
                            lastRefreshedTime = timeStr,
                            errorMessage = null
                        )
                    }
                } catch (e: Exception) {
                    _arjunStocksState.update {
                        it.copy(
                            isRefreshingQuotes = false,
                            errorMessage = e.message
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            // JVM test fallback
        }
    }

    internal fun setPositionsForTesting(positions: List<PositionItem>) {
        _uiState.update { it.copy(positions = positions) }
    }
}
