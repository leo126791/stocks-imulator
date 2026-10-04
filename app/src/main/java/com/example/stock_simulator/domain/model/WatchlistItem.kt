package com.example.stock_simulator.domain.model

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
