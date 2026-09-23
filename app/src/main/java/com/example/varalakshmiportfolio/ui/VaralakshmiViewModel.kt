package com.example.varalakshmiportfolio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.varalakshmiportfolio.data.SyncResult
import com.example.varalakshmiportfolio.data.VaralakshmiRepository
import com.example.varalakshmiportfolio.model.PortfolioSummary
import com.example.varalakshmiportfolio.model.PositionItem
import com.example.varalakshmiportfolio.model.TransactionItem
import com.example.varalakshmiportfolio.model.MarketIndexItem
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
    val isRefreshing: Boolean = false,
    val serverUrl: String = VaralakshmiRepository.DEFAULT_SERVER_URL,
    val authToken: String = VaralakshmiRepository.DEFAULT_AUTH_TOKEN,
    val showSettingsDialog: Boolean = false,
    val positionToExit: PositionItem? = null,
    val snackbarMessage: String? = null,
    val isLiveSync: Boolean = false
)

class VaralakshmiViewModel(
    private val repository: VaralakshmiRepository = VaralakshmiRepository(),
    autoRefresh: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VaralakshmiUiState(
            summary = repository.getCachedSummary(),
            positions = repository.getCachedPositions(),
            transactions = repository.getCachedTransactions(),
            nifty = repository.getCachedNifty(),
            serverUrl = repository.getServerUrl(),
            authToken = repository.getAuthToken()
        )
    )
    val uiState: StateFlow<VaralakshmiUiState> = _uiState.asStateFlow()

    init {
        if (autoRefresh) {
            try {
                refresh()
            } catch (e: Throwable) {
                // Safeguard against unconfigured Main dispatcher in headless JVM tests
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val syncResult = repository.refreshData(_uiState.value.serverUrl, _uiState.value.authToken)
            when (syncResult) {
                is SyncResult.Success -> {
                    _uiState.update {
                        it.copy(
                            summary = syncResult.summary,
                            positions = syncResult.positions,
                            transactions = syncResult.transactions,
                            nifty = syncResult.nifty,
                            isRefreshing = false,
                            isLiveSync = true,
                            snackbarMessage = "Synced with live trading engine"
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
                            isRefreshing = false,
                            isLiveSync = false,
                            snackbarMessage = "Offline mode: showing cached snapshot"
                        )
                    }
                }
            }
        }
    }

    fun requestExit(position: PositionItem) {
        _uiState.update { it.copy(positionToExit = position) }
    }

    fun dismissExit() {
        _uiState.update { it.copy(positionToExit = null) }
    }

    fun confirmExit() {
        val target = _uiState.value.positionToExit ?: return
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

    internal fun setPositionsForTesting(positions: List<PositionItem>) {
        _uiState.update { it.copy(positions = positions) }
    }
}
