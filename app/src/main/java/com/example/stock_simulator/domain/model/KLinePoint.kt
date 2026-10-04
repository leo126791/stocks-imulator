package com.example.stock_simulator.domain.model

data class KLinePoint(
    val date: String,       // 日期 (YYYY-MM-DD)
    val open: Double,       // 開盤價
    val high: Double,       // 最高價
    val low: Double,        // 最低價
    val close: Double,      // 收盤價
    val volume: Long        // 成交量
)
