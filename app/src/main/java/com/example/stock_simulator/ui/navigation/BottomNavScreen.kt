package com.example.stock_simulator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Market : BottomNavScreen("market", "大盤行情", Icons.Default.ShowChart)
    object Watchlist : BottomNavScreen("watchlist", "自選股", Icons.Default.Star)
    object Trade : BottomNavScreen("trade", "模擬交易", Icons.Default.SwapHoriz)
    object Portfolio : BottomNavScreen("portfolio", "資產庫存", Icons.Default.AccountBalanceWallet)
}
