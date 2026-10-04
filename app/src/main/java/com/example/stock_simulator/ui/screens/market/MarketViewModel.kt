package com.example.stock_simulator.ui.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.repository.StockRepository
import com.example.stock_simulator.domain.model.IndustrySector
import com.example.stock_simulator.domain.model.IntradayTick
import com.example.stock_simulator.domain.model.MarketIndex
import com.example.stock_simulator.domain.model.StockQuote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "台股",
    val selectedOverviewSymbol: String = "tse_t00",
    val indices: List<MarketIndex> = emptyList(),
    val mainIntradayTicks: List<IntradayTick> = emptyList(),
    val mainPrevClose: Double = 48353.49,
    val quotes: List<StockQuote> = emptyList(),
    val filteredQuotes: List<StockQuote> = emptyList(),
    val errorMessage: String? = null
)

class MarketViewModel(
    private val stockRepository: StockRepository = StockRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        loadMarketQuotes()
        startAutoRefresh()
    }

    fun loadMarketQuotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val category = _uiState.value.selectedCategory
                var indices = stockRepository.getMarketIndices(category)

                // 確保台股有 加權指, 櫃買指, 台指近 三大指標卡片
                if (indices.none { it.symbol == "TX" }) {
                    val txIndex = MarketIndex(
                        symbol = "TX",
                        name = "台指近",
                        category = "台股",
                        currentPrice = 48671.0,
                        change = -27.0,
                        changePercent = -0.06,
                        turnoverAmount = "3,210 億",
                        openPrice = 48698.0,
                        highPrice = 48750.0,
                        lowPrice = 48620.0,
                        previousClose = 48698.0,
                        trendPoints = listOf(48698.0, 48750.0, 48620.0, 48671.0)
                    )
                    indices = indices + txIndex
                }

                val currentSymbol = _uiState.value.selectedOverviewSymbol
                val selectedIdxItem = indices.find { it.symbol == currentSymbol } ?: indices.firstOrNull()
                val prevClose = selectedIdxItem?.previousClose ?: 48353.49
                val ticks = stockRepository.getIntradayTicks(currentSymbol, prevClose)

                // 調用 TWSE/TPEX 官方開放 API 載入全台股所有股票 (2000+ 檔)
                val allTaiwanStocks = stockRepository.getAllTaiwanStocks()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    indices = indices,
                    mainIntradayTicks = ticks,
                    mainPrevClose = prevClose,
                    quotes = allTaiwanStocks,
                    filteredQuotes = filterQuotes(allTaiwanStocks, _uiState.value.searchQuery)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加載行情失敗: ${e.message}"
                )
            }
        }
    }

    fun onOverviewSymbolSelected(symbol: String) {
        viewModelScope.launch {
            val selectedItem = _uiState.value.indices.find { it.symbol == symbol }
            val prevClose = selectedItem?.previousClose ?: 48353.49
            val ticks = stockRepository.getIntradayTicks(symbol, prevClose)

            _uiState.value = _uiState.value.copy(
                selectedOverviewSymbol = symbol,
                mainIntradayTicks = ticks,
                mainPrevClose = prevClose
            )
        }
    }

    fun onCategorySelected(category: String) {
        viewModelScope.launch {
            val indices = stockRepository.getMarketIndices(category)
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                indices = indices
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentQuotes = _uiState.value.quotes
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredQuotes = filterQuotes(currentQuotes, query)
        )
    }

    private fun filterQuotes(quotes: List<StockQuote>, query: String): List<StockQuote> {
        if (query.isBlank()) {
            // 預設展示前 30 檔熱門股票與權值股
            return quotes.take(30)
        }
        return quotes.filter { quote ->
            quote.symbol.contains(query, ignoreCase = true) || quote.name.contains(query, ignoreCase = true)
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(10000)
                if (_uiState.value.searchQuery.isBlank()) {
                    val category = _uiState.value.selectedCategory
                    val indices = stockRepository.getMarketIndices(category)
                    _uiState.value = _uiState.value.copy(
                        indices = indices
                    )
                }
            }
        }
    }
}
