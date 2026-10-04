package com.example.stock_simulator.data.repository

import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.data.local.entity.AccountEntity
import com.example.stock_simulator.data.local.entity.OrderEntity
import com.example.stock_simulator.data.local.entity.PositionEntity
import com.example.stock_simulator.data.local.entity.WatchlistEntity
import com.example.stock_simulator.domain.model.Account
import com.example.stock_simulator.domain.model.Order
import com.example.stock_simulator.domain.model.OrderPriceType
import com.example.stock_simulator.domain.model.OrderStatus
import com.example.stock_simulator.domain.model.OrderType
import com.example.stock_simulator.domain.model.Position
import com.example.stock_simulator.domain.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class TradeResult {
    data class Success(val message: String) : TradeResult()
    data class Error(val message: String) : TradeResult()
}

class SimulatorRepository(private val database: StockDatabase) {

    private val accountDao = database.accountDao()
    private val positionDao = database.positionDao()
    private val orderDao = database.orderDao()
    private val watchlistDao = database.watchlistDao()

    val accountFlow: Flow<Account> = accountDao.getAccountFlow().map { entity ->
        if (entity == null) {
            val newAcc = AccountEntity(id = 1, cashBalance = 200000.0, initialCapital = 200000.0)
            accountDao.insertOrUpdateAccount(newAcc)
            newAcc.toDomainModel()
        } else if (entity.initialCapital == 1000000.0) {
            // 自動校正舊的 1,000,000 為 200,000
            val updated = entity.copy(cashBalance = 200000.0, initialCapital = 200000.0)
            accountDao.insertOrUpdateAccount(updated)
            updated.toDomainModel()
        } else {
            entity.toDomainModel()
        }
    }

    val positionsFlow: Flow<List<Position>> = positionDao.getAllPositionsFlow().map { list ->
        list.map { it.toDomainModel() }
    }

    val ordersFlow: Flow<List<Order>> = orderDao.getAllOrdersFlow().map { list ->
        list.map { it.toDomainModel() }
    }

    val watchlistFlow: Flow<List<WatchlistItem>> = watchlistDao.getWatchlistFlow().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun executeTrade(
        symbol: String,
        name: String,
        type: OrderType,
        priceType: OrderPriceType,
        targetPrice: Double,
        shares: Int,
        currentMarketPrice: Double
    ): TradeResult = withContext(Dispatchers.IO) {
        val tradePrice = if (priceType == OrderPriceType.MARKET) currentMarketPrice else targetPrice
        if (tradePrice <= 0 || shares <= 0) {
            return@withContext TradeResult.Error("價格與股數必須大於 0")
        }

        var currentAccount = accountDao.getAccount()
            ?: AccountEntity(id = 1, cashBalance = 200000.0, initialCapital = 200000.0)

        if (currentAccount.initialCapital == 1000000.0) {
            currentAccount = currentAccount.copy(cashBalance = 200000.0, initialCapital = 200000.0)
            accountDao.insertOrUpdateAccount(currentAccount)
        }

        when (type) {
            OrderType.BUY -> {
                val totalAmount = tradePrice * shares

                if (priceType == OrderPriceType.LIMIT && currentMarketPrice > targetPrice) {
                    // 限價買進：當前現價高於限價，暫無法成交 -> 預扣現金掛單委託中 (PENDING)
                    if (currentAccount.cashBalance < totalAmount) {
                        return@withContext TradeResult.Error("現金餘額不足！限價掛單需要預扣現金 NT$ ${totalAmount.toLong()}")
                    }

                    // 預扣現金
                    accountDao.insertOrUpdateAccount(currentAccount.copy(cashBalance = currentAccount.cashBalance - totalAmount))

                    orderDao.insertOrder(
                        OrderEntity(
                            symbol = symbol,
                            name = name,
                            type = OrderType.BUY.name,
                            priceType = priceType.name,
                            price = targetPrice,
                            shares = shares,
                            status = OrderStatus.PENDING.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    TradeResult.Success("限價買進已掛單 (委託中)！現價 NT$ $currentMarketPrice 高於限價 NT$ $targetPrice，待價格下跌至 NT$ $targetPrice 即自動成交")
                } else {
                    // 市價買進 或 限價買進已達標 -> 立即成交 (FILLED)
                    val fillPrice = if (priceType == OrderPriceType.MARKET) currentMarketPrice else targetPrice
                    val fillAmount = fillPrice * shares

                    if (currentAccount.cashBalance < fillAmount) {
                        return@withContext TradeResult.Error("現金餘額不足！需要 NT$ ${fillAmount.toLong()}")
                    }

                    accountDao.insertOrUpdateAccount(currentAccount.copy(cashBalance = currentAccount.cashBalance - fillAmount))

                    val existingPos = positionDao.getPositionBySymbol(symbol)
                    if (existingPos == null) {
                        positionDao.insertOrUpdatePosition(
                            PositionEntity(
                                symbol = symbol,
                                name = name,
                                shares = shares,
                                averagePrice = fillPrice
                            )
                        )
                    } else {
                        val totalShares = existingPos.shares + shares
                        val newAvgPrice = ((existingPos.shares * existingPos.averagePrice) + fillAmount) / totalShares
                        positionDao.insertOrUpdatePosition(
                            existingPos.copy(
                                shares = totalShares,
                                averagePrice = newAvgPrice
                            )
                        )
                    }

                    orderDao.insertOrder(
                        OrderEntity(
                            symbol = symbol,
                            name = name,
                            type = OrderType.BUY.name,
                            priceType = priceType.name,
                            price = fillPrice,
                            shares = shares,
                            status = OrderStatus.FILLED.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    TradeResult.Success("買進成功！以 NT$ $fillPrice 成交 $shares 股")
                }
            }

            OrderType.SELL -> {
                val existingPos = positionDao.getPositionBySymbol(symbol)
                if (existingPos == null || existingPos.shares < shares) {
                    return@withContext TradeResult.Error("庫存不足！現有庫存: ${existingPos?.shares ?: 0} 股")
                }

                if (priceType == OrderPriceType.LIMIT && currentMarketPrice < targetPrice) {
                    // 限價賣出：當前現價低於限價，暫無法成交 -> 圈存庫存掛單委託中 (PENDING)
                    val remainingShares = existingPos.shares - shares
                    if (remainingShares > 0) {
                        positionDao.insertOrUpdatePosition(existingPos.copy(shares = remainingShares))
                    } else {
                        positionDao.deletePosition(symbol)
                    }

                    orderDao.insertOrder(
                        OrderEntity(
                            symbol = symbol,
                            name = name,
                            type = OrderType.SELL.name,
                            priceType = priceType.name,
                            price = targetPrice,
                            shares = shares,
                            status = OrderStatus.PENDING.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    TradeResult.Success("限價賣出已掛單 (委託中)！現價 NT$ $currentMarketPrice 低於限價 NT$ $targetPrice，待價格上漲至 NT$ $targetPrice 即自動成交")
                } else {
                    // 市價賣出 或 限價賣出已達標 -> 立即成交 (FILLED)
                    val fillPrice = if (priceType == OrderPriceType.MARKET) currentMarketPrice else targetPrice
                    val fillAmount = fillPrice * shares

                    accountDao.insertOrUpdateAccount(currentAccount.copy(cashBalance = currentAccount.cashBalance + fillAmount))

                    val remainingShares = existingPos.shares - shares
                    if (remainingShares > 0) {
                        positionDao.insertOrUpdatePosition(existingPos.copy(shares = remainingShares))
                    } else {
                        positionDao.deletePosition(symbol)
                    }

                    orderDao.insertOrder(
                        OrderEntity(
                            symbol = symbol,
                            name = name,
                            type = OrderType.SELL.name,
                            priceType = priceType.name,
                            price = fillPrice,
                            shares = shares,
                            status = OrderStatus.FILLED.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    TradeResult.Success("賣出成功！以 NT$ $fillPrice 成交 $shares 股")
                }
            }
        }
    }

    /**
     * 檢查並自動撮合委託中的限價單 (PENDING Orders Matcher)
     */
    suspend fun checkAndMatchPendingOrders(latestPriceMap: Map<String, Double>) = withContext(Dispatchers.IO) {
        val pendingOrders = orderDao.getPendingOrders()
        if (pendingOrders.isEmpty()) return@withContext

        val currentAccount = accountDao.getAccount() ?: return@withContext

        pendingOrders.forEach { order ->
            val marketPrice = latestPriceMap[order.symbol] ?: return@forEach

            if (order.type == OrderType.BUY.name) {
                // 限價買進撮合條件：當前現價 <= 限價
                if (marketPrice <= order.price) {
                    val fillPrice = order.price
                    val totalCost = fillPrice * order.shares

                    // 更新庫存
                    val existingPos = positionDao.getPositionBySymbol(order.symbol)
                    if (existingPos == null) {
                        positionDao.insertOrUpdatePosition(
                            PositionEntity(
                                symbol = order.symbol,
                                name = order.name,
                                shares = order.shares,
                                averagePrice = fillPrice
                            )
                        )
                    } else {
                        val newTotalShares = existingPos.shares + order.shares
                        val newAvgPrice = ((existingPos.shares * existingPos.averagePrice) + totalCost) / newTotalShares
                        positionDao.insertOrUpdatePosition(
                            existingPos.copy(
                                shares = newTotalShares,
                                averagePrice = newAvgPrice
                            )
                        )
                    }

                    // 更新委託狀態為 已成交
                    orderDao.updateOrderStatus(order.id, OrderStatus.FILLED.name)
                }
            } else if (order.type == OrderType.SELL.name) {
                // 限價賣出撮合條件：當前現價 >= 限價
                if (marketPrice >= order.price) {
                    val fillPrice = order.price
                    val fillAmount = fillPrice * order.shares

                    // 解存金額入帳
                    accountDao.insertOrUpdateAccount(currentAccount.copy(cashBalance = currentAccount.cashBalance + fillAmount))

                    // 更新委託狀態為 已成交
                    orderDao.updateOrderStatus(order.id, OrderStatus.FILLED.name)
                }
            }
        }
    }

    /**
     * 取消委託中的限價單 (CANCEL Order)
     */
    suspend fun cancelOrder(orderId: Long): Boolean = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext false
        if (order.status != OrderStatus.PENDING.name) return@withContext false

        val currentAccount = accountDao.getAccount() ?: return@withContext false

        if (order.type == OrderType.BUY.name) {
            // 退還預扣現金
            val refundAmount = order.price * order.shares
            accountDao.insertOrUpdateAccount(currentAccount.copy(cashBalance = currentAccount.cashBalance + refundAmount))
        } else if (order.type == OrderType.SELL.name) {
            // 退還圈存庫存
            val existingPos = positionDao.getPositionBySymbol(order.symbol)
            if (existingPos == null) {
                positionDao.insertOrUpdatePosition(
                    PositionEntity(
                        symbol = order.symbol,
                        name = order.name,
                        shares = order.shares,
                        averagePrice = order.price
                    )
                )
            } else {
                positionDao.insertOrUpdatePosition(
                    existingPos.copy(shares = existingPos.shares + order.shares)
                )
            }
        }

        orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED.name)
        true
    }

    suspend fun addToWatchlist(symbol: String, name: String) = withContext(Dispatchers.IO) {
        watchlistDao.addToWatchlist(WatchlistEntity(symbol, name, System.currentTimeMillis()))
    }

    suspend fun removeFromWatchlist(symbol: String) = withContext(Dispatchers.IO) {
        watchlistDao.removeFromWatchlist(symbol)
    }

    suspend fun isInWatchlist(symbol: String): Boolean = withContext(Dispatchers.IO) {
        watchlistDao.isInWatchlist(symbol)
    }

    suspend fun resetAccount() = withContext(Dispatchers.IO) {
        accountDao.insertOrUpdateAccount(AccountEntity(id = 1, cashBalance = 200000.0, initialCapital = 200000.0))
        database.clearAllTables()
        accountDao.insertOrUpdateAccount(AccountEntity(id = 1, cashBalance = 200000.0, initialCapital = 200000.0))
    }
}
