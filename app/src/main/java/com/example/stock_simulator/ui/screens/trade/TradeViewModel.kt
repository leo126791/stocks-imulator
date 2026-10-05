package com.example.stock_simulator.ui.screens.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.data.repository.SimulatorRepository
import com.example.stock_simulator.data.repository.StockRepository
import com.example.stock_simulator.data.repository.TradeResult
import com.example.stock_simulator.domain.model.Account
import com.example.stock_simulator.domain.model.OrderPriceType
import com.example.stock_simulator.domain.model.OrderType
import com.example.stock_simulator.domain.model.StockQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TradeUiState(
    val symbol: String = "2330",
    val quote: StockQuote? = null,
    val orderType: OrderType = OrderType.BUY,
    val priceType: OrderPriceType = OrderPriceType.MARKET,
    val customPrice: String = "",
    val shares: String = "1000", // 預設 1 張 (1,000 股)
    val account: Account? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class TradeViewModel(
    private val initialSymbol: String = "2330",
    private val database: StockDatabase,
    private val stockRepository: StockRepository = StockRepository(),
    private val simulatorRepository: SimulatorRepository = SimulatorRepository(database)
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradeUiState(symbol = initialSymbol))
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    init {
        loadStockAndAccount(initialSymbol)
    }

    fun loadStockAndAccount(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(symbol = symbol, isLoading = true)
            val quote = stockRepository.getStockQuote(symbol)
            _uiState.value = _uiState.value.copy(
                quote = quote,
                customPrice = quote.currentPrice.toString(),
                isLoading = false
            )
        }

        viewModelScope.launch {
            simulatorRepository.accountFlow.collect { account ->
                _uiState.value = _uiState.value.copy(account = account)
            }
        }
    }

    fun onSymbolChanged(symbol: String) {
        _uiState.value = _uiState.value.copy(symbol = symbol)
        loadStockAndAccount(symbol)
    }

    fun onOrderTypeChanged(type: OrderType) {
        _uiState.value = _uiState.value.copy(orderType = type)
    }

    fun onPriceTypeChanged(priceType: OrderPriceType) {
        _uiState.value = _uiState.value.copy(priceType = priceType)
    }

    fun onCustomPriceChanged(price: String) {
        _uiState.value = _uiState.value.copy(customPrice = price)
    }

    fun onSharesChanged(shares: String) {
        _uiState.value = _uiState.value.copy(shares = shares)
    }

    fun submitTrade() {
        val current = _uiState.value
        val quote = current.quote ?: return

        // 檢查是否為大盤與台指期指數，指數類別禁止購買下單
        val isIndex = current.symbol.startsWith("tse_") ||
                current.symbol.startsWith("otc_") ||
                current.symbol in listOf("t00", "o00", "TX", "N225", "DJI", "IXIC", "GSPC", "SOX", "HSI", "KS11", "FTSE", "GDAXI") ||
                quote.name.contains("台指") ||
                quote.name.contains("加權") ||
                quote.name.contains("櫃買") ||
                quote.name.contains("指數")

        if (isIndex) {
            _uiState.value = current.copy(
                message = "${quote.name} (${current.symbol}) 為指數類別，無法進行買賣下單，請輸入個股代號 (例: 2330)",
                isError = true
            )
            return
        }

        val targetPrice = current.customPrice.toDoubleOrNull() ?: quote.currentPrice
        val sharesInt = current.shares.toIntOrNull() ?: 0

        if (sharesInt <= 0) {
            _uiState.value = current.copy(message = "請輸入有效的交易股數", isError = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true)
            val result = simulatorRepository.executeTrade(
                symbol = current.symbol,
                name = quote.name,
                type = current.orderType,
                priceType = current.priceType,
                targetPrice = targetPrice,
                shares = sharesInt,
                currentMarketPrice = quote.currentPrice
            )

            when (result) {
                is TradeResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = false
                    )
                }
                is TradeResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
