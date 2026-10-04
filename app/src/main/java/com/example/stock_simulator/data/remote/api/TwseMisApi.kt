package com.example.stock_simulator.data.remote.api

import com.example.stock_simulator.data.remote.dto.TwseMisResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TwseMisApi {
    /**
     * @param exCh 格式例如: "tse_2330.tw|tse_2317.tw|otc_6547.tw"
     */
    @GET("stock/api/getStockInfo.jsp")
    suspend fun getStockInfo(
        @Query("ex_ch") exCh: String,
        @Query("json") json: Int = 1,
        @Query("delay") delay: Int = 0
    ): TwseMisResponseDto
}
