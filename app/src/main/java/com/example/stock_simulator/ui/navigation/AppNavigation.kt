package com.example.stock_simulator.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stock_simulator.data.local.StockDatabase
import com.example.stock_simulator.ui.screens.detail.StockDetailScreen
import com.example.stock_simulator.ui.screens.detail.StockDetailViewModel
import com.example.stock_simulator.ui.screens.market.MarketScreen
import com.example.stock_simulator.ui.screens.portfolio.PortfolioScreen
import com.example.stock_simulator.ui.screens.portfolio.PortfolioViewModel
import com.example.stock_simulator.ui.screens.trade.TradeScreen
import com.example.stock_simulator.ui.screens.trade.TradeViewModel
import com.example.stock_simulator.ui.screens.watchlist.WatchlistScreen
import com.example.stock_simulator.ui.screens.watchlist.WatchlistViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = StockDatabase.getDatabase(context)

    val bottomNavItems = listOf(
        BottomNavScreen.Market,
        BottomNavScreen.Watchlist,
        BottomNavScreen.Trade,
        BottomNavScreen.Portfolio
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 只在四大分頁時顯示底部導覽列，進入內頁明細時隱藏
            if (bottomNavItems.any { it.route == currentRoute }) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Market.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Market.route) {
                MarketScreen(
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }

            composable(BottomNavScreen.Watchlist.route) {
                val viewModel: WatchlistViewModel = viewModel(factory = SimpleViewModelFactory {
                    WatchlistViewModel(database)
                })
                WatchlistScreen(
                    viewModel = viewModel,
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }

            composable(BottomNavScreen.Trade.route) {
                val viewModel: TradeViewModel = viewModel(factory = SimpleViewModelFactory {
                    TradeViewModel("2330", database)
                })
                TradeScreen(viewModel = viewModel)
            }

            composable("trade/{symbol}") { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: "2330"
                val viewModel: TradeViewModel = viewModel(factory = SimpleViewModelFactory {
                    TradeViewModel(symbol, database)
                })
                TradeScreen(viewModel = viewModel)
            }

            composable(BottomNavScreen.Portfolio.route) {
                val viewModel: PortfolioViewModel = viewModel(factory = SimpleViewModelFactory {
                    PortfolioViewModel(database)
                })
                PortfolioScreen(
                    viewModel = viewModel,
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }

            composable(
                route = "detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: "2330"
                val viewModel: StockDetailViewModel = viewModel(factory = SimpleViewModelFactory {
                    StockDetailViewModel(symbol, database)
                })
                StockDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onTradeClick = { targetSymbol ->
                        navController.navigate("trade/$targetSymbol")
                    }
                )
            }
        }
    }
}

class SimpleViewModelFactory<T : androidx.lifecycle.ViewModel>(
    private val creator: () -> T
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
