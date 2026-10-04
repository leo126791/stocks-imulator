package com.example.stock_simulator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TwseMisResponseDto(
    @SerializedName("msgArray") val msgArray: List<StockMsgDto>? = null,
    @SerializedName("userDelay") val userDelay: Long? = null,
    @SerializedName("rtmessage") val rtmessage: String? = null
)

data class StockMsgDto(
    @SerializedName("c") val code: String? = null,          // 股票代號 (如 2330)
    @SerializedName("n") val name: String? = null,          // 股票名稱 (如 台積電)
    @SerializedName("z") val currentPrice: String? = null,   // 當前成交價
    @SerializedName("y") val yesterdayClose: String? = null,// 昨收價
    @SerializedName("o") val openPrice: String? = null,      // 開盤價
    @SerializedName("h") val highPrice: String? = null,      // 最高價
    @SerializedName("l") val lowPrice: String? = null,       // 最低價
    @SerializedName("v") val volume: String? = null,         // 累積成交量 (張)
    @SerializedName("b") val buyPrices: String? = null,      // 買價清單 (用 _ 分隔)
    @SerializedName("g") val buyVolumes: String? = null,     // 買量清單 (用 _ 分隔)
    @SerializedName("a") val sellPrices: String? = null,     // 賣價清單 (用 _ 分隔)
    @SerializedName("f") val sellVolumes: String? = null,    // 賣量清單 (用 _ 分隔)
    @SerializedName("tlong") val timestamp: String? = null   // 搓合時間 (毫秒)
)
