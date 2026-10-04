package com.example.stock_simulator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FinMindResponseDto(
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("data") val data: List<FinMindKLineDto>? = null
)

data class FinMindKLineDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("stock_id") val stockId: String? = null,
    @SerializedName("open") val open: Double? = null,
    @SerializedName("max") val max: Double? = null,
    @SerializedName("min") val min: Double? = null,
    @SerializedName("close") val close: Double? = null,
    @SerializedName("Trading_Volume") val tradingVolume: Long? = null
)
