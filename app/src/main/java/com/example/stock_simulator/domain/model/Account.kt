package com.example.stock_simulator.domain.model

data class Account(
    val id: Int = 1,
    val cashBalance: Double,        // 現金餘額
    val initialCapital: Double      // 初始資金 (如: 1,000,000)
)
