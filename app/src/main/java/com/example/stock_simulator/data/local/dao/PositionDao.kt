package com.example.stock_simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stock_simulator.data.local.entity.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions")
    fun getAllPositionsFlow(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    suspend fun getPositionBySymbol(symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePosition(position: PositionEntity)

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)
}
