package com.example.stock_simulator.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.data.repository.SimulatorRepository
import com.example.stock_simulator.data.repository.StockRepository
import com.example.stock_simulator.domain.model.StockQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val isLoading: Boolean = true,
    val watchlistQuotes: List<StockQuote> = emptyList()
)

class WatchlistViewModel(
    private val database: StockDatabase,
    private val stockRepository: StockRepository = StockRepository(),
    private val simulatorRepository: SimulatorRepository = SimulatorRepository(database)
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        observeWatchlist()
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            simulatorRepository.watchlistFlow.collect { list ->
                if (list.isEmpty()) {
                    _uiState.value = WatchlistUiState(isLoading = false, watchlistQuotes = emptyList())
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    val symbols = list.map { it.symbol }
                    val quotes = stockRepository.getStockQuotes(symbols)
                    _uiState.value = WatchlistUiState(isLoading = false, watchlistQuotes = quotes)
                }
            }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            simulatorRepository.removeFromWatchlist(symbol)
        }
    }
}
