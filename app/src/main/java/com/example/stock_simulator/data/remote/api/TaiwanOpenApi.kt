package com.example.stock_simulator.data.remote.api

import com.example.stock_simulator.data.remote.dto.TpexOtcStockDto
import com.example.stock_simulator.data.remote.dto.TwseOpenApiStockDto
import retrofit2.http.GET

interface TwseOpenApi {
    @GET("v1/exchangeReport/STOCK_DAY_ALL")
    suspend fun getAllListedStocks(): List<TwseOpenApiStockDto>
}

interface TpexOpenApi {
    @GET("openapi/v1/tpex_mainboard_daily_close_quotes")
    suspend fun getAllOtcStocks(): List<TpexOtcStockDto>
}
