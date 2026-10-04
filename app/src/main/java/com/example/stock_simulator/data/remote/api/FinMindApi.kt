package com.example.stock_simulator.data.remote.api

import com.example.stock_simulator.data.remote.dto.FinMindResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FinMindApi {
    @GET("api/v4/data")
    suspend fun getKLineData(
        @Query("dataset") dataset: String = "TaiwanStockPrice",
        @Query("data_id") dataId: String,
        @Query("start_date") startDate: String
    ): FinMindResponseDto
}
