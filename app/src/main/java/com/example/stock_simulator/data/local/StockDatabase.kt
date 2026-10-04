package com.example.stock_simulator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.stock_simulator.data.local.dao.AccountDao
import com.example.stock_simulator.data.local.dao.OrderDao
import com.example.stock_simulator.data.local.dao.PositionDao
import com.example.stock_simulator.data.local.dao.WatchlistDao
import com.example.stock_simulator.data.local.entity.AccountEntity
import com.example.stock_simulator.data.local.entity.OrderEntity
import com.example.stock_simulator.data.local.entity.PositionEntity
import com.example.stock_simulator.data.local.entity.WatchlistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        PositionEntity::class,
        OrderEntity::class,
        WatchlistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun positionDao(): PositionDao
    abstract fun orderDao(): OrderDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_simulator_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default initial account capital ($200,000 TWD)
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).accountDao().insertOrUpdateAccount(
                                AccountEntity(id = 1, cashBalance = 200000.0, initialCapital = 200000.0)
                            )
                            // Seed default watchlist
                            val watchlistDao = getDatabase(context).watchlistDao()
                            watchlistDao.addToWatchlist(WatchlistEntity("2330", "台積電", System.currentTimeMillis()))
                            watchlistDao.addToWatchlist(WatchlistEntity("2317", "鴻海", System.currentTimeMillis() - 1000))
                            watchlistDao.addToWatchlist(WatchlistEntity("2454", "聯發科", System.currentTimeMillis() - 2000))
                            watchlistDao.addToWatchlist(WatchlistEntity("0050", "元大台灣50", System.currentTimeMillis() - 3000))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
