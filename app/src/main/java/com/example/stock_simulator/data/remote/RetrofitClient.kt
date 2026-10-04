package com.example.stock_simulator.data.remote

import com.example.stock_simulator.data.remote.api.FinMindApi
import com.example.stock_simulator.data.remote.api.TpexOpenApi
import com.example.stock_simulator.data.remote.api.TwseMisApi
import com.example.stock_simulator.data.remote.api.TwseOpenApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Referer", "https://mis.twse.com.tw/stock/fibest.jsp")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val twseMisApi: TwseMisApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://mis.twse.com.tw/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwseMisApi::class.java)
    }

    val twseOpenApi: TwseOpenApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://openapi.twse.com.tw/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwseOpenApi::class.java)
    }

    val tpexOpenApi: TpexOpenApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.tpex.org.tw/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TpexOpenApi::class.java)
    }

    val finMindApi: FinMindApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.finmindtrade.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinMindApi::class.java)
    }
}
