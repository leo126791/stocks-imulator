package com.example.stock_simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stock_simulator.domain.model.Position

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val shares: Int,
    val averagePrice: Double
) {
    fun toDomainModel(currentPrice: Double = averagePrice): Position = Position(
        symbol = symbol,
        name = name,
        shares = shares,
        averagePrice = averagePrice,
        currentPrice = currentPrice
    )

    companion object {
        fun fromDomainModel(position: Position): PositionEntity = PositionEntity(
            symbol = position.symbol,
            name = position.name,
            shares = position.shares,
            averagePrice = position.averagePrice
        )
    }
}
