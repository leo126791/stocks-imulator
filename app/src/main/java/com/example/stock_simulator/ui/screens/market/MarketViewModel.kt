package com.example.stock_simulator.ui.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.repository.StockRepository
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
                var indices = stockRepository.getMarketIndices("台股")

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

                // 調用 API 載入全台股所有股票 (~2000+ 檔)
                var allTaiwanStocks = stockRepository.getAllTaiwanStocks()
                if (allTaiwanStocks.isEmpty()) {
                    allTaiwanStocks = stockRepository.getFallbackStockList()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    indices = indices,
                    mainIntradayTicks = ticks,
                    mainPrevClose = prevClose,
                    quotes = allTaiwanStocks,
                    filteredQuotes = filterQuotes(allTaiwanStocks, _uiState.value.searchQuery)
                )
            } catch (e: Exception) {
                val fallbackStocks = stockRepository.getFallbackStockList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    quotes = fallbackStocks,
                    filteredQuotes = filterQuotes(fallbackStocks, _uiState.value.searchQuery)
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

    fun onSearchQueryChanged(query: String) {
        val currentQuotes = if (_uiState.value.quotes.isNotEmpty()) {
            _uiState.value.quotes
        } else {
            stockRepository.getFallbackStockList()
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredQuotes = filterQuotes(currentQuotes, query)
        )
    }

    private fun filterQuotes(quotes: List<StockQuote>, query: String): List<StockQuote> {
        val sourceQuotes = if (quotes.isNotEmpty()) quotes else stockRepository.getFallbackStockList()
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            return sourceQuotes.distinctBy { it.symbol }.take(30)
        }

        // 搜尋比對代號與名稱（不區分大小寫），並根據相符程度排序：
        // 1. 完全相同
        // 2. 代號或名稱以關鍵字開頭 (例如輸入 "23" -> "2330", "2317" 優先)
        // 3. 代號或名稱包含關鍵字
        return sourceQuotes
            .filter { quote ->
                quote.symbol.contains(cleanQuery, ignoreCase = true) ||
                quote.name.contains(cleanQuery, ignoreCase = true)
            }
            .distinctBy { it.symbol }
            .sortedWith(
                compareByDescending<StockQuote> { quote ->
                    if (quote.symbol.equals(cleanQuery, ignoreCase = true) || quote.name.equals(cleanQuery, ignoreCase = true)) 3
                    else if (quote.symbol.startsWith(cleanQuery, ignoreCase = true) || quote.name.startsWith(cleanQuery, ignoreCase = true)) 2
                    else 1
                }.thenBy { it.symbol }
            )
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(3000) // 每 3 秒即時自動輪詢更新大盤與全台股行情
                try {
                    val indices = stockRepository.getMarketIndices("台股")
                    val currentSymbol = _uiState.value.selectedOverviewSymbol
                    val selectedIdxItem = indices.find { it.symbol == currentSymbol }
                    val prevClose = selectedIdxItem?.previousClose ?: 48353.49
                    val ticks = stockRepository.getIntradayTicks(currentSymbol, prevClose)

                    var allTaiwanStocks = stockRepository.getAllTaiwanStocks()
                    if (allTaiwanStocks.isEmpty()) {
                        allTaiwanStocks = stockRepository.getFallbackStockList()
                    }

                    _uiState.value = _uiState.value.copy(
                        indices = indices,
                        mainIntradayTicks = ticks,
                        mainPrevClose = prevClose,
                        quotes = allTaiwanStocks,
                        filteredQuotes = filterQuotes(allTaiwanStocks, _uiState.value.searchQuery)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
