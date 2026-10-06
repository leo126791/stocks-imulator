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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class TradeUiState(
    val symbol: String = "2330",
    val quote: StockQuote? = null,
    val orderType: OrderType = OrderType.BUY,
    val priceType: OrderPriceType = OrderPriceType.MARKET,
    val customPrice: String = "",
    val shares: String = "1000", // 預設 1 張 (1,000 股)
    val account: Account? = null,
    val isMarketOpen: Boolean = false,
    val enforceTradingHours: Boolean = true, // 預設依規定僅限開盤時間交易
    val showVipDialog: Boolean = false,      // VIP 開通彈窗開關
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
        checkMarketStatus()
        loadStockAndAccount(initialSymbol)
        startRealTimeQuoteRefresh()
    }

    fun checkMarketStatus() {
        val open = isTaiwanMarketOpen()
        _uiState.value = _uiState.value.copy(isMarketOpen = open)
    }

    fun onEnforceTradingHoursChanged(enforce: Boolean) {
        val currentAccount = _uiState.value.account
        val isVip = currentAccount?.isVip ?: false

        // 若使用者欲取消開盤限制 (允許盤後模擬)，且目前非 VIP 會員 -> 阻擋並彈出 VIP 升級視窗
        if (!enforce && !isVip) {
            _uiState.value = _uiState.value.copy(
                showVipDialog = true,
                message = "「盤後模擬交易」為 VIP 尊榮會員專屬功能！請前往 Google Play 商店訂閱 VIP。"
            )
            return
        }

        _uiState.value = _uiState.value.copy(enforceTradingHours = enforce)
    }

    fun dismissVipDialog() {
        _uiState.value = _uiState.value.copy(showVipDialog = false)
    }

    fun requestGooglePlayPurchase() {
        _uiState.value = _uiState.value.copy(
            showVipDialog = false,
            message = "🛒 即將連結 Google Play 商店完成 VIP 訂閱流程！(預留 Google Play 內購金流接入點)",
            isError = false
        )
    }

    fun loadStockAndAccount(symbol: String) {
        checkMarketStatus()
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
        checkMarketStatus()
        val current = _uiState.value
        val quote = current.quote ?: return
        val isVip = current.account?.isVip ?: false

        // 1. 交易時間與 VIP 限制檢查
        if (current.enforceTradingHours && !isTaiwanMarketOpen()) {
            if (!isVip) {
                // 非開盤時間 + 非 VIP -> 阻擋並跳出 VIP 升級視窗
                _uiState.value = current.copy(
                    showVipDialog = true,
                    message = "非盤中交易時間！「盤後模擬交易」為 VIP 會員專屬權限，請訂閱 VIP 尊榮會員以開通盤後交易。",
                    isError = true
                )
                return
            }
        }

        // 2. 檢查是否為大盤與台指期指數，指數類別禁止購買下單
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

    private fun startRealTimeQuoteRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(3000) // 每 3 秒即時刷新下單頁面的個股最新價格
                try {
                    val currentSym = _uiState.value.symbol
                    val updatedQuote = stockRepository.getStockQuote(currentSym)
                    val isMarket = _uiState.value.priceType == OrderPriceType.MARKET

                    _uiState.value = _uiState.value.copy(
                        quote = updatedQuote,
                        customPrice = if (isMarket) updatedQuote.currentPrice.toString() else _uiState.value.customPrice
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        /**
         * 檢查目前是否位於台灣股市常規交易時間 (週一至週五 09:00 ~ 13:30)
         */
        fun isTaiwanMarketOpen(): Boolean {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"))
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            // 週六 (Calendar.SATURDAY = 7) 與週日 (Calendar.SUNDAY = 1) 為休市日
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                return false
            }

            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val currentMinuteOfDay = hour * 60 + minute

            val openMinute = 9 * 60          // 09:00 (540 分鐘)
            val closeMinute = 13 * 60 + 30   // 13:30 (810 分鐘)

            return currentMinuteOfDay in openMinute..closeMinute
        }
    }
}
