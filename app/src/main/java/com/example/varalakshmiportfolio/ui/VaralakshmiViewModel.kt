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
import com.example.varalakshmiportfolio.util.MarketHours
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class VaralakshmiUiState(
    val summary: PortfolioSummary,
    val positions: List<PositionItem>,
    val transactions: List<TransactionItem>,
    val nifty: MarketIndexItem = MarketIndexItem(),
    val recommendations: List<StockRecommendationItem> = emptyList(),
    val recommendationHistory: List<HistoricalRecommendationItem> = emptyList(),
    val recommendationHistorySummary: RecommendationHistorySummary = RecommendationHistorySummary(0, 0.0, 0.0, 0, 0),
    val ipoNotifications: List<IpoActionNotification> = emptyList(),
    val selectedRecommendationForChart: StockRecommendationItem? = null,
    val showRecommendationHistory: Boolean = false,
    val selectedHistoryFilter: String = "ALL",
    val isRefreshing: Boolean = false,
    val serverUrl: String = VaralakshmiRepository.DEFAULT_SERVER_URL,
    val authToken: String = VaralakshmiRepository.DEFAULT_AUTH_TOKEN,
    val showSettingsDialog: Boolean = false,
    val positionToExit: PositionItem? = null,
    val snackbarMessage: String? = null,
    val isLiveSync: Boolean = false,
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val isMarketOpen: Boolean = false,
    val marketStatusText: String = ""
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
    val syncIntervalMillis: Long = 15 * 60 * 1000L,
    private val checkIntervalMillis: Long = 30_000L
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VaralakshmiUiState(
            summary = repository.getCachedSummary(),
            positions = repository.getCachedPositions(),
            transactions = repository.getCachedTransactions(),
            nifty = repository.getCachedNifty(),
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

    private var periodicSyncJob: Job? = null
    private var fnoSyncJob: Job? = null

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

    fun refresh(isSilent: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            _veeraLakshmiState.update { it.copy(isLoading = true) }

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
                            transactions = syncResult.transactions,
                            nifty = syncResult.nifty,
                            recommendations = syncResult.recommendations,
                            recommendationHistory = syncResult.recommendationHistory,
                            recommendationHistorySummary = syncResult.recommendationHistorySummary,
                            ipoNotifications = syncResult.ipoNotifications,
                            isRefreshing = false,
                            isLiveSync = true,
                            lastSyncTimestamp = now,
                            isMarketOpen = isMarket,
                            marketStatusText = statusText,
                            snackbarMessage = if (isSilent) null else "Synced with live trading engine"
                        )
                    }
                }
                is SyncResult.OfflineCacheFallback -> {
                    _uiState.update {
                        it.copy(
                            summary = syncResult.summary,
                            positions = syncResult.positions,
                            transactions = syncResult.transactions,
                            nifty = syncResult.nifty,
                            recommendations = syncResult.recommendations,
                            recommendationHistory = syncResult.recommendationHistory,
                            recommendationHistorySummary = syncResult.recommendationHistorySummary,
                            ipoNotifications = syncResult.ipoNotifications,
                            isRefreshing = false,
                            isLiveSync = false,
                            lastSyncTimestamp = now,
                            isMarketOpen = isMarket,
                            marketStatusText = statusText,
                            snackbarMessage = if (isSilent) null else "Offline mode: showing cached snapshot"
                        )
                    }
                }
            }
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
                snackbarMessage = "Exited ${target.symbol} • Freed up ₹${String.format(Locale.US, "%,.2f", target.marketValue)}"
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
                                snackbarMessage = "Exited ${target.symbol} • $msg"
                            )
                        }
                        refresh()
                    },
                    onFailure = { err ->
                        _uiState.update { current ->
                            current.copy(
                                snackbarMessage = "Exit order issue for ${target.symbol}: ${err.message}"
                            )
                        }
                    }
                )
            }
        } catch (e: Throwable) {
            // Safeguard against unconfigured Main dispatcher in headless JVM tests
        }
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
            val targetLabel = if (newEnabled) "+35% Cap" else "Uncapped Runner"
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
    }

    internal fun setPositionsForTesting(positions: List<PositionItem>) {
        _uiState.update { it.copy(positions = positions) }
    }
}
