package com.example.stock_simulator.domain.model

data class StockQuote(
    val symbol: String,             // 代號 (例: 2330)
    val name: String,               // 名稱 (例: 台積電)
    val currentPrice: Double,       // 當前/成交價
    val change: Double,             // 漲跌點數
    val changePercent: Double,      // 漲跌幅 (%)
    val openPrice: Double,          // 開盤價
    val highPrice: Double,          // 最高價
    val lowPrice: Double,           // 最低價
    val previousClose: Double,      // 昨收價
    val volume: Long,               // 當日成交總量 (張)
    val buyFivePrices: List<Double> = emptyList(),   // 買一 ~ 買五
    val buyFiveVolumes: List<Int> = emptyList(),    // 買一 ~ 買五 量
    val sellFivePrices: List<Double> = emptyList(),  // 賣一 ~ 賣五
    val sellFiveVolumes: List<Int> = emptyList(),   // 賣一 ~ 賣五 量
    val updateTime: String = ""     // 更新時間
)
