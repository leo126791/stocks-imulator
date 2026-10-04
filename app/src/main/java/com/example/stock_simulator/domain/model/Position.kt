package com.example.stock_simulator.domain.model

data class Position(
    val symbol: String,             // 股票代號
    val name: String,               // 股票名稱
    val shares: Int,                // 持有股數
    val averagePrice: Double,       // 平均成本價
    val currentPrice: Double = 0.0  // 現價 (即時更新)
) {
    val totalCost: Double get() = shares * averagePrice
    val currentValue: Double get() = shares * currentPrice
    val profitLoss: Double get() = currentValue - totalCost
    val profitLossPercent: Double get() = if (totalCost > 0) (profitLoss / totalCost) * 100 else 0.0
}
