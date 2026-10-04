package com.example.stock_simulator.domain.model

data class IntradayTick(
    val time: String,       // 時間 (例: "09:00", "09:15", "10:30")
    val price: Double,      // 當前價格/指數
    val volume: Long,       // 成交量
    val change: Double      // 相較昨收之漲跌
)
