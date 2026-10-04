package com.example.stock_simulator.ui.screens.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.Order
import com.example.stock_simulator.domain.model.OrderPriceType
import com.example.stock_simulator.domain.model.OrderStatus
import com.example.stock_simulator.domain.model.OrderType
import com.example.stock_simulator.domain.model.Position
import com.example.stock_simulator.ui.theme.StockFlat
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    onStockClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 持有庫存, 1: 委託成交紀錄

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("資產與庫存", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 資產總覽卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "總資產 (TWD)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "NT$ ${String.format(Locale.TAIWAN, "%,.0f", uiState.totalAsset)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("可用現金", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "NT$ ${String.format(Locale.TAIWAN, "%,.0f", uiState.account.cashBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("股票市值", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "NT$ ${String.format(Locale.TAIWAN, "%,.0f", uiState.stockValue)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val profitColor = when {
                        uiState.totalProfitLoss > 0 -> StockRed
                        uiState.totalProfitLoss < 0 -> StockGreen
                        else -> StockFlat
                    }
                    val profitSign = if (uiState.totalProfitLoss > 0) "+" else ""

                    Text(
                        text = "總累計損益: $profitSign${String.format(Locale.TAIWAN, "%,.0f", uiState.totalProfitLoss)} ($profitSign${String.format(Locale.TAIWAN, "%.2f", uiState.totalProfitLossPercent)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            // 分頁切換 (持有庫存 vs 委託成交紀錄)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("持有庫存 (${uiState.positions.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("交易歷史 (${uiState.orders.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                if (uiState.positions.isEmpty()) {
                    BoxEmptyState("目前無任何持股，前往「大盤行情」或「模擬交易」開始買進！")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.positions, key = { it.symbol }) { position ->
                            PositionCard(position = position, onClick = { onStockClick(position.symbol) })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                if (uiState.orders.isEmpty()) {
                    BoxEmptyState("尚無歷史下單紀錄")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onCancelClick = { viewModel.cancelOrder(order.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionCard(position: Position, onClick: () -> Unit) {
    val profitColor = when {
        position.profitLoss > 0 -> StockRed
        position.profitLoss < 0 -> StockGreen
        else -> StockFlat
    }
    val profitSign = if (position.profitLoss > 0) "+" else ""

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = position.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${position.symbol} • ${position.shares} 股", style = MaterialTheme.typography.bodySmall)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NT$ ${String.format(Locale.TAIWAN, "%,.0f", position.currentValue)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$profitSign${String.format(Locale.TAIWAN, "%,.0f", position.profitLoss)} ($profitSign${String.format(Locale.TAIWAN, "%.2f", position.profitLossPercent)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onCancelClick: (() -> Unit)? = null
) {
    val typeColor = if (order.type == OrderType.BUY) StockRed else StockGreen
    val typeText = if (order.type == OrderType.BUY) "買進" else "賣出"
    val priceTypeText = if (order.priceType == OrderPriceType.MARKET) "市價" else "限價"
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN).format(Date(order.timestamp))

    val statusText = when (order.status) {
        OrderStatus.FILLED -> "已成交"
        OrderStatus.PENDING -> "委託中"
        OrderStatus.CANCELLED -> "已取消"
    }

    val statusBgColor = when (order.status) {
        OrderStatus.FILLED -> StockGreen.copy(alpha = 0.2f)
        OrderStatus.PENDING -> Color(0xFFFF9800).copy(alpha = 0.25f)
        OrderStatus.CANCELLED -> Color.Gray.copy(alpha = 0.2f)
    }

    val statusTextColor = when (order.status) {
        OrderStatus.FILLED -> StockGreen
        OrderStatus.PENDING -> Color(0xFFE65100)
        OrderStatus.CANCELLED -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${order.name} ($typeText $priceTypeText)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = typeColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(statusBgColor, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = statusTextColor, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "NT$ ${String.format(Locale.TAIWAN, "%,.0f", order.price * order.shares)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "${order.symbol} • ${order.shares} 股 @ NT$ ${order.price}", style = MaterialTheme.typography.bodySmall)
                    Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (order.status == OrderStatus.PENDING && onCancelClick != null) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("取消委託", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BoxEmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
