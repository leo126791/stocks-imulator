package com.example.stock_simulator.ui.screens.trade

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.OrderPriceType
import com.example.stock_simulator.domain.model.OrderType
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    viewModel: TradeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // 👑 VIP 升級開通視窗 (當試圖啟用盤後交易且未開通 VIP 時跳出，引導至 Google Play Store 訂閱)
    if (uiState.showVipDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVipDialog() },
            icon = {
                Text("👑", fontSize = 36.sp)
            },
            title = {
                Text("訂閱 VIP 尊榮會員", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "「盤後模擬交易」為 VIP 會員專屬功能！",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("前往 Google Play 商店訂閱 VIP 即可解鎖：", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 24 小時全天候盤後模擬下單交易", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 零時差即時行情與 K 線專業圖表", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• 無限次資產模擬與投資組合試算", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.requestGooglePlayPurchase() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("🛒 前往 Google Play 商店訂閱", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissVipDialog() }) {
                    Text("稍後再說")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模擬下單交易", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. 帳戶可用資金卡片 (含 VIP 勳章標籤)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "可用現金餘額",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (uiState.account?.isVip == true) {
                            Surface(
                                color = Color(0xFFFFD700), // 尊榮金色
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "👑 VIP 尊榮會員",
                                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "NT$ ${String.format(Locale.TAIWAN, "%,.0f", uiState.account?.cashBalance ?: 200000.0)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 盤中常規交易時間狀態卡片 (台股 09:00 ~ 13:30)
            val marketStatusBg = if (uiState.isMarketOpen) StockGreen.copy(alpha = 0.15f) else StockRed.copy(alpha = 0.15f)
            val marketStatusText = if (uiState.isMarketOpen) "🟢 台股常規交易中 (開盤時間 09:00 ~ 13:30)" else "🔴 台股休市中 (開盤時間為週一至週五 09:00 ~ 13:30)"
            val marketStatusColor = if (uiState.isMarketOpen) StockGreen else StockRed

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = marketStatusBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = marketStatusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = marketStatusColor,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.enforceTradingHours,
                        onClick = { viewModel.onEnforceTradingHoursChanged(!uiState.enforceTradingHours) },
                        label = { Text(if (uiState.enforceTradingHours) "限開盤交易" else "👑 盤後模擬 (VIP)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 選擇股票代號
            OutlinedTextField(
                value = uiState.symbol,
                onValueChange = { viewModel.onSymbolChanged(it) },
                label = { Text("股票代號 (例: 2330)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            uiState.quote?.let { quote ->
                Text(
                    text = "${quote.name} 現價: NT$ ${quote.currentPrice}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 買進 / 賣出 切換
            Text("交易類型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = { viewModel.onOrderTypeChanged(OrderType.BUY) },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.orderType == OrderType.BUY)
                        ButtonDefaults.buttonColors(containerColor = StockRed)
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("買進", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { viewModel.onOrderTypeChanged(OrderType.SELL) },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.orderType == OrderType.SELL)
                        ButtonDefaults.buttonColors(containerColor = StockGreen)
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("賣出", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 市價 / 限價 切換
            Text("價格類型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = uiState.priceType == OrderPriceType.MARKET,
                    onClick = { viewModel.onPriceTypeChanged(OrderPriceType.MARKET) },
                    label = { Text("市價成交") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = uiState.priceType == OrderPriceType.LIMIT,
                    onClick = { viewModel.onPriceTypeChanged(OrderPriceType.LIMIT) },
                    label = { Text("限價委託") }
                )
            }

            if (uiState.priceType == OrderPriceType.LIMIT) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.customPrice,
                    onValueChange = { viewModel.onCustomPriceChanged(it) },
                    label = { Text("委託單價 (NT$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. 委託股數
            OutlinedTextField(
                value = uiState.shares,
                onValueChange = { viewModel.onSharesChanged(it) },
                label = { Text("委託股數 (例: 1000 股 = 1 張)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // 試算預估總金額
            val quotePrice = uiState.quote?.currentPrice ?: 0.0
            val unitPrice = if (uiState.priceType == OrderPriceType.LIMIT) {
                uiState.customPrice.toDoubleOrNull() ?: quotePrice
            } else quotePrice
            val shares = uiState.shares.toIntOrNull() ?: 0
            val estimatedTotal = unitPrice * shares

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("預估交易總金額: ", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "NT$ ${String.format(Locale.TAIWAN, "%,.0f", estimatedTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.orderType == OrderType.BUY) StockRed else StockGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. 送出下單按鈕
            Button(
                onClick = { viewModel.submitTrade() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.orderType == OrderType.BUY) StockRed else StockGreen
                )
            ) {
                Text(
                    text = if (uiState.orderType == OrderType.BUY) "確認買進模擬下單" else "確認賣出模擬下單",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
