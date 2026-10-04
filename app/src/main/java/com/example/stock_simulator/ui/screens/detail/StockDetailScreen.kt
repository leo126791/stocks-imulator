package com.example.stock_simulator.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stock_simulator.ui.components.InteractiveKLineChart
import com.example.stock_simulator.ui.components.IntradayTrendChart
import com.example.stock_simulator.ui.components.OrderBookView
import com.example.stock_simulator.ui.components.StockPriceText
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel,
    onBackClick: () -> Unit,
    onTradeClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val quote = uiState.quote

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quote?.let { "${it.name} (${it.symbol})" } ?: "行情明細") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (uiState.isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "加入自選",
                            tint = if (uiState.isInWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && quote == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (quote != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 價格與統計區
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quote.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "更新時間: ${quote.updateTime}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    StockPriceText(
                        price = quote.currentPrice,
                        change = quote.change,
                        changePercent = quote.changePercent,
                        isLarge = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 開/高/低/量 行情概覽
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("開盤: ${quote.openPrice}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("最高: ${quote.highPrice}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("最低: ${quote.lowPrice}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("成交: ${quote.volume}張", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 圖表頁籤切換 (0: 盤中分時圖, 1: K線圖)
                TabRow(selectedTabIndex = uiState.chartTab) {
                    Tab(
                        selected = uiState.chartTab == 0,
                        onClick = { viewModel.onChartTabSelected(0) },
                        text = { Text("分時走勢", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = uiState.chartTab == 1,
                        onClick = { viewModel.onChartTabSelected(1) },
                        text = { Text("K線技術圖", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.chartTab == 0) {
                    // Yahoo! 股市風格 盤中即時分時圖 (09:00 - 13:30 趨勢折線 + 昨收線)
                    IntradayTrendChart(
                        ticks = uiState.intradayTicks,
                        previousClose = quote.previousClose
                    )
                } else {
                    // Yahoo! 股市風格 專業 K 線圖 (MA均線 + 十字準星 + 極值標註)
                    InteractiveKLineChart(allKLines = uiState.kLines)
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isIndex = quote.symbol.startsWith("tse_") ||
                        quote.symbol.startsWith("otc_") ||
                        quote.symbol in listOf("N225", "DJI", "IXIC", "GSPC", "SOX", "HSI", "KS11", "FTSE", "GDAXI")

                // 最佳五檔 (若非大盤指數才顯示買賣五檔)
                if (!isIndex) {
                    OrderBookView(
                        buyPrices = quote.buyFivePrices,
                        buyVolumes = quote.buyFiveVolumes,
                        sellPrices = quote.sellFivePrices,
                        sellVolumes = quote.sellFiveVolumes
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 下單按鈕區 (若非大盤指數才顯示買賣下單按鈕)
                if (!isIndex) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onTradeClick(quote.symbol) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StockRed)
                        ) {
                            Text("買進模擬", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { onTradeClick(quote.symbol) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StockGreen)
                        ) {
                            Text("賣出模擬", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
