package com.example.stock_simulator.ui.screens.market

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stock_simulator.ui.components.DualColorIntradayChart
import com.example.stock_simulator.ui.components.MarketOverviewHeader
import com.example.stock_simulator.ui.components.StockCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onStockClick: (String) -> Unit,
    viewModel: MarketViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("台灣股市大盤", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadMarketQuotes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新行情")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. 最頂部的即時搜尋框 (專為全台股 2000+ 檔設計)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜尋台股代號或名稱 (例: 2330, 台積電, 長榮)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜尋")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2. 搜尋狀態：若輸入關鍵字，立即於最上方呈現相近台股清單
            if (uiState.searchQuery.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "相近台股搜尋結果 (${uiState.filteredQuotes.size} 檔)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (uiState.filteredQuotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "查無與「${uiState.searchQuery}」相符的台股代號或名稱",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.filteredQuotes, key = { it.symbol }) { quote ->
                                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                    StockCard(
                                        quote = quote,
                                        onClick = { onStockClick(quote.symbol) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 3. 常規狀態：呈現台股三大指數卡片、大盤雙色走勢圖與熱門精選股票
                if (uiState.isLoading && uiState.quotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.errorMessage != null && uiState.quotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 頂部三大指數概覽卡片列 (加權指 / 櫃買指 / 台指近)
                        item {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                MarketOverviewHeader(
                                    indices = uiState.indices,
                                    selectedSymbol = uiState.selectedOverviewSymbol,
                                    onSelectIndex = { symbol ->
                                        viewModel.onOverviewSymbolSelected(symbol)
                                    }
                                )
                            }
                        }

                        // 大盤雙色分時走勢主圖
                        item {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                DualColorIntradayChart(
                                    ticks = uiState.mainIntradayTicks,
                                    previousClose = uiState.mainPrevClose
                                )
                            }
                        }

                        // 全台股精選行情標題
                        item {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "全台股精選行情",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 個股列表
                        items(uiState.filteredQuotes, key = { it.symbol }) { quote ->
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                StockCard(
                                    quote = quote,
                                    onClick = { onStockClick(quote.symbol) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
