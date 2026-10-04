package com.example.stock_simulator.domain.model

enum class OrderType { BUY, SELL }
enum class OrderPriceType { MARKET, LIMIT }
enum class OrderStatus { FILLED, PENDING, CANCELLED }

data class Order(
    val id: Long = 0,
    val symbol: String,             // 股票代號
    val name: String,               // 股票名稱
    val type: OrderType,            // 買進 / 賣出
    val priceType: OrderPriceType,  // 市價 / 限價
    val price: Double,              // 限價金額或成交單價
    val shares: Int,                // 股數
    val status: OrderStatus,        // 成交 / 委託中 / 已取消
    val timestamp: Long = System.currentTimeMillis()
)
