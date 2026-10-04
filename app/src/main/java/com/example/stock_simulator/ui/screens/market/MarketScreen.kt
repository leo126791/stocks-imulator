package com.example.stock_simulator.ui.screens.market

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
    val categories = listOf("台灣市場", "美國市場", "陸港市場", "全球市場")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大盤動態", fontWeight = FontWeight.Bold) },
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
                    // 1. 市場分類標籤 (台灣市場, 美國市場, 陸港市場, 全球市場)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = uiState.selectedCategory == category || (category == "台灣市場" && uiState.selectedCategory == "台股"),
                                    onClick = { viewModel.onCategorySelected(if (category == "台灣市場") "台股" else category) },
                                    label = { Text(category, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }

                    // 2. 頂部三大指數概覽卡片列 (加權指 / 櫃買指 / 台指近)
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

                    // 3. 大盤雙色分時走勢主圖 (三竹股市風格：上紅下綠 + 昨收虛線)
                    item {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            DualColorIntradayChart(
                                ticks = uiState.mainIntradayTicks,
                                previousClose = uiState.mainPrevClose
                            )
                        }
                    }

                    // 4. 搜尋列與全台股檢索列表 (含熱門與全部 2,000+ 檔個股)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                placeholder = { Text("搜尋全台股 2000+ 檔代號或名稱 (例: 2330, 長榮)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true
                            )

                            Text(
                                text = if (uiState.searchQuery.isBlank()) "全台股精選行情 (${uiState.quotes.size} 檔)" else "搜尋結果 (${uiState.filteredQuotes.size} 檔)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                            )
                        }
                    }

                    // 5. 股票個股列表
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
