package com.example.stock_simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockGreenLight
import com.example.stock_simulator.ui.theme.StockRed
import com.example.stock_simulator.ui.theme.StockRedLight
import java.util.Locale

@Composable
fun OrderBookView(
    buyPrices: List<Double>,
    buyVolumes: List<Int>,
    sellPrices: List<Double>,
    sellVolumes: List<Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "最佳五檔 (買賣委託價量)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("買量", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("買價", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                Text("賣價", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                Text("賣量", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
            }

            val maxRows = maxOf(buyPrices.size, sellPrices.size, 5)
            for (i in 0 until maxRows) {
                val buyPrice = buyPrices.getOrNull(i)
                val buyVol = buyVolumes.getOrNull(i)
                val sellPrice = sellPrices.getOrNull(i)
                val sellVol = sellVolumes.getOrNull(i)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = buyVol?.let { String.format(Locale.TAIWAN, "%,d", it) } ?: "-",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = buyPrice?.let { String.format(Locale.TAIWAN, "%.2f", it) } ?: "-",
                        modifier = Modifier
                            .weight(1f)
                            .background(StockRedLight, shape = RoundedCornerShape(4.dp)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = StockRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = sellPrice?.let { String.format(Locale.TAIWAN, "%.2f", it) } ?: "-",
                        modifier = Modifier
                            .weight(1f)
                            .background(StockGreenLight, shape = RoundedCornerShape(4.dp)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = StockGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = sellVol?.let { String.format(Locale.TAIWAN, "%,d", it) } ?: "-",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
