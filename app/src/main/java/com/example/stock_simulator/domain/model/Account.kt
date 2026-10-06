package com.example.stock_simulator.domain.model

data class Account(
    val id: Int = 1,
    val cashBalance: Double,        // 現金餘額
    val initialCapital: Double,     // 初始資金 (如: 200,000)
    val isVip: Boolean = false      // VIP 尊榮會員權限 (預設 false)
)
