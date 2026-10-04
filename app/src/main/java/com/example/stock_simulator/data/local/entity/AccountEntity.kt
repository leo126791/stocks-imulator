package com.example.stock_simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stock_simulator.domain.model.Account

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: Int = 1,
    val cashBalance: Double,
    val initialCapital: Double
) {
    fun toDomainModel(): Account = Account(
        id = id,
        cashBalance = cashBalance,
        initialCapital = initialCapital
    )

    companion object {
        fun fromDomainModel(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            cashBalance = account.cashBalance,
            initialCapital = account.initialCapital
        )
    }
}
