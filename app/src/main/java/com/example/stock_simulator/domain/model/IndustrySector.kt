package com.example.stock_simulator.domain.model

data class IndustrySector(
    val name: String,           // 產業名稱 (例: 半導體業, 電子組件, 通信網路)
    val changePercent: Double,  // 漲跌幅 (%)
    val weight: Float           // 比重 (用於塊狀熱圖面積大小)
)
