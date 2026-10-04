package com.example.stock_simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stock_simulator.domain.model.WatchlistItem

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val addedAt: Long
) {
    fun toDomainModel(): WatchlistItem = WatchlistItem(
        symbol = symbol,
        name = name,
        addedAt = addedAt
    )

    companion object {
        fun fromDomainModel(item: WatchlistItem): WatchlistEntity = WatchlistEntity(
            symbol = item.symbol,
            name = item.name,
            addedAt = item.addedAt
        )
    }
}
