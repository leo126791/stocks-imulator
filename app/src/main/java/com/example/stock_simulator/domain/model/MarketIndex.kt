package com.example.stock_simulator.domain.model

data class MarketIndex(
    val symbol: String,             // 指數代號 (例: tse_t00)
    val name: String,               // 指數名稱 (例: 上市, 上櫃, 電子, 金融)
    val category: String,           // 分類 (例: 台股, 亞股, 歐股, 美股)
    val currentPrice: Double,       // 當前指數
    val change: Double,             // 漲跌點數
    val changePercent: Double,      // 漲跌幅 (%)
    val turnoverAmount: String,     // 成交金額 (例: 8,997.10 億)
    val openPrice: Double,          // 開盤
    val highPrice: Double,          // 最高
    val lowPrice: Double,           // 最低
    val previousClose: Double,      // 昨收
    val trendPoints: List<Double>   // 即時走勢趨勢點 (用於微型走勢圖)
)
