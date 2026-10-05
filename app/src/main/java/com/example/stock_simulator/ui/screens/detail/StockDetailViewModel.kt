package com.example.stock_simulator.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.data.repository.SimulatorRepository
import com.example.stock_simulator.data.repository.StockRepository
import com.example.stock_simulator.domain.model.IntradayTick
import com.example.stock_simulator.domain.model.KLinePoint
import com.example.stock_simulator.domain.model.StockQuote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockDetailUiState(
    val isLoading: Boolean = true,
    val quote: StockQuote? = null,
    val intradayTicks: List<IntradayTick> = emptyList(),
    val kLines: List<KLinePoint> = emptyList(),
    val isInWatchlist: Boolean = false,
    val chartTab: Int = 0, // 0: 分時走勢圖, 1: K線圖
    val errorMessage: String? = null
)

class StockDetailViewModel(
    private val symbol: String,
    private val database: StockDatabase,
    private val stockRepository: StockRepository = StockRepository(),
    private val simulatorRepository: SimulatorRepository = SimulatorRepository(database)
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetailData()
        startRealTimeQuoteRefresh()
    }

    fun loadDetailData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val quote = stockRepository.getStockQuote(symbol)
                val intradayTicks = stockRepository.getIntradayTicks(symbol, quote.previousClose)
                val kLines = stockRepository.getKLineData(symbol)
                val inWatchlist = simulatorRepository.isInWatchlist(symbol)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quote = quote,
                    intradayTicks = intradayTicks,
                    kLines = kLines,
                    isInWatchlist = inWatchlist
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加載股票明細失敗: ${e.message}"
                )
            }
        }
    }

    fun onChartTabSelected(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(chartTab = tabIndex)
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val currentQuote = _uiState.value.quote ?: return@launch
            val currentlyIn = _uiState.value.isInWatchlist
            if (currentlyIn) {
                simulatorRepository.removeFromWatchlist(symbol)
            } else {
                simulatorRepository.addToWatchlist(symbol, currentQuote.name)
            }
            _uiState.value = _uiState.value.copy(isInWatchlist = !currentlyIn)
        }
    }

    private fun startRealTimeQuoteRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // 每 5 秒即時刷新個股報價與分時走勢
                try {
                    val updatedQuote = stockRepository.getStockQuote(symbol)
                    val updatedTicks = stockRepository.getIntradayTicks(symbol, updatedQuote.previousClose)
                    _uiState.value = _uiState.value.copy(
                        quote = updatedQuote,
                        intradayTicks = updatedTicks
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
