package com.example.stock_simulator.ui.screens.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.data.repository.SimulatorRepository
import com.example.stock_simulator.data.repository.StockRepository
import com.example.stock_simulator.domain.model.Account
import com.example.stock_simulator.domain.model.Order
import com.example.stock_simulator.domain.model.OrderStatus
import com.example.stock_simulator.domain.model.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val account: Account = Account(cashBalance = 200000.0, initialCapital = 200000.0),
    val positions: List<Position> = emptyList(),
    val orders: List<Order> = emptyList(),
    val stockValue: Double = 0.0,
    val totalAsset: Double = 200000.0,
    val totalProfitLoss: Double = 0.0,
    val totalProfitLossPercent: Double = 0.0,
    val isLoading: Boolean = false
)

class PortfolioViewModel(
    private val database: StockDatabase,
    private val stockRepository: StockRepository = StockRepository(),
    private val simulatorRepository: SimulatorRepository = SimulatorRepository(database)
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        observeData()
        startOrderMatcherLoop()
    }

    private fun observeData() {
        viewModelScope.launch {
            simulatorRepository.accountFlow.collect { account ->
                updatePortfolioState(account = account)
            }
        }

        viewModelScope.launch {
            simulatorRepository.positionsFlow.collect { rawPositions ->
                // 即時更新庫存現價
                val symbols = rawPositions.map { it.symbol }
                val quotes = if (symbols.isNotEmpty()) stockRepository.getStockQuotes(symbols) else emptyList()
                val updatedPositions = rawPositions.map { pos ->
                    val currentPrice = quotes.find { it.symbol == pos.symbol }?.currentPrice ?: pos.averagePrice
                    pos.copy(currentPrice = currentPrice)
                }
                updatePortfolioState(positions = updatedPositions)
            }
        }

        viewModelScope.launch {
            simulatorRepository.ordersFlow.collect { orders ->
                _uiState.value = _uiState.value.copy(orders = orders)
            }
        }
    }

    private fun updatePortfolioState(
        account: Account = _uiState.value.account,
        positions: List<Position> = _uiState.value.positions
    ) {
        val stockVal = positions.sumOf { it.currentValue }
        val totalAssets = account.cashBalance + stockVal
        val profitLoss = totalAssets - account.initialCapital
        val profitLossPercent = if (account.initialCapital > 0) (profitLoss / account.initialCapital) * 100 else 0.0

        _uiState.value = _uiState.value.copy(
            account = account,
            positions = positions,
            stockValue = stockVal,
            totalAsset = totalAssets,
            totalProfitLoss = profitLoss,
            totalProfitLossPercent = profitLossPercent
        )
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            simulatorRepository.cancelOrder(orderId)
        }
    }

    private fun startOrderMatcherLoop() {
        viewModelScope.launch {
            while (true) {
                delay(3000) // 每 3 秒檢查一次委託中的限價單並撮合
                try {
                    val pendingOrders = _uiState.value.orders.filter { it.status == OrderStatus.PENDING }
                    if (pendingOrders.isNotEmpty()) {
                        val symbols = pendingOrders.map { it.symbol }.distinct()
                        val quotes = stockRepository.getStockQuotes(symbols)
                        val priceMap = quotes.associate { it.symbol to it.currentPrice }
                        simulatorRepository.checkAndMatchPendingOrders(priceMap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun resetAccount() {
        viewModelScope.launch {
            simulatorRepository.resetAccount()
        }
    }
}
