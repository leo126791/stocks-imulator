package com.example.stock_simulator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TwseOpenApiStockDto(
    @SerializedName("Code") val code: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("ClosingPrice") val closingPrice: String? = null,
    @SerializedName("Change") val change: String? = null,
    @SerializedName("OpeningPrice") val openingPrice: String? = null,
    @SerializedName("HighestPrice") val highestPrice: String? = null,
    @SerializedName("LowestPrice") val lowestPrice: String? = null,
    @SerializedName("TradeVolume") val tradeVolume: String? = null
)

data class TpexOtcStockDto(
    @SerializedName("SecuritiesCompanyCode") val code: String? = null,
    @SerializedName("CompanyName") val name: String? = null,
    @SerializedName("Close") val closingPrice: String? = null,
    @SerializedName("Change") val change: String? = null,
    @SerializedName("Open") val openingPrice: String? = null,
    @SerializedName("High") val highestPrice: String? = null,
    @SerializedName("Low") val lowestPrice: String? = null,
    @SerializedName("TradingShares") val tradeVolume: String? = null
)
