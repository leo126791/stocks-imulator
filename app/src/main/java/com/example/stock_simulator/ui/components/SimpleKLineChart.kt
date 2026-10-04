package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stock_simulator.domain.model.KLinePoint
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed

@Composable
fun SimpleKLineChart(
    kLines: List<KLinePoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "技術分析 (近 30 日 K 線趨勢)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (kLines.isEmpty()) {
                Text(
                    text = "暫無 K 線資料",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                val minPrice = kLines.minOf { it.low }
                val maxPrice = kLines.maxOf { it.high }
                val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barWidth = width / kLines.size
                    val candleWidth = barWidth * 0.6f

                    kLines.forEachIndexed { index, kline ->
                        val isUp = kline.close >= kline.open
                        val color = if (isUp) StockRed else StockGreen

                        val x = index * barWidth + barWidth / 2

                        // Y 座標計算 (Canvas 頂端為 0)
                        val highY = height - ((kline.high - minPrice) / priceRange * height).toFloat()
                        val lowY = height - ((kline.low - minPrice) / priceRange * height).toFloat()
                        val openY = height - ((kline.open - minPrice) / priceRange * height).toFloat()
                        val closeY = height - ((kline.close - minPrice) / priceRange * height).toFloat()

                        // 畫影線 (High - Low)
                        drawLine(
                            color = color,
                            start = Offset(x, highY),
                            end = Offset(x, lowY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // 畫實體 (Open - Close)
                        val bodyTop = minOf(openY, closeY)
                        val bodyHeight = maxOf(abs(openY - closeY), 2.dp.toPx())

                        drawRect(
                            color = color,
                            topLeft = Offset(x - candleWidth / 2, bodyTop),
                            size = Size(candleWidth, bodyHeight)
                        )
                    }
                }
            }
        }
    }
}

private fun abs(value: Float): Float = if (value < 0) -value else value
