package com.example.stock_simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stock_simulator.domain.model.Order
import com.example.stock_simulator.domain.model.OrderPriceType
import com.example.stock_simulator.domain.model.OrderStatus
import com.example.stock_simulator.domain.model.OrderType

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val type: String,        // "BUY" or "SELL"
    val priceType: String,   // "MARKET" or "LIMIT"
    val price: Double,
    val shares: Int,
    val status: String,      // "FILLED", "PENDING", "CANCELLED"
    val timestamp: Long
) {
    fun toDomainModel(): Order = Order(
        id = id,
        symbol = symbol,
        name = name,
        type = OrderType.valueOf(type),
        priceType = OrderPriceType.valueOf(priceType),
        price = price,
        shares = shares,
        status = OrderStatus.valueOf(status),
        timestamp = timestamp
    )

    companion object {
        fun fromDomainModel(order: Order): OrderEntity = OrderEntity(
            id = order.id,
            symbol = order.symbol,
            name = order.name,
            type = order.type.name,
            priceType = order.priceType.name,
            price = order.price,
            shares = order.shares,
            status = order.status.name,
            timestamp = order.timestamp
        )
    }
}
