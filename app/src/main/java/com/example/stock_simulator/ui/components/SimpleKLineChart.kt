package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.KLinePoint
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale
import kotlin.math.abs

@Composable
fun SimpleKLineChart(
    kLines: List<KLinePoint>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    if (kLines.isEmpty()) {
        Text(
            text = "暫無 K 線資料",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )
    } else {
        val minPrice = kLines.minOfOrNull { it.low } ?: 100.0
        val maxPrice = kLines.maxOfOrNull { it.high } ?: 100.0
        val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / kLines.size.coerceAtLeast(1)
            val candleWidth = maxOf(barWidth * 0.6f, 2f)

            // A. 背景參考線與價格刻度 (Y 軸 3 等分)
            val gridLines = 3
            val priceStep = priceRange / gridLines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            for (i in 0..gridLines) {
                val y = height - (i.toFloat() / gridLines) * height
                val priceLabel = minPrice + i * priceStep

                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    pathEffect = dashEffect,
                    strokeWidth = 1f
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = String.format(Locale.TAIWAN, "%.1f", priceLabel),
                    style = TextStyle(color = Color.Gray, fontSize = 9.sp),
                    topLeft = Offset(width - 40.dp.toPx(), y - 10.dp.toPx())
                )
            }

            // B. 繪製 K 線蠟燭圖 (Red/Green)
            kLines.forEachIndexed { index, kline ->
                val x = index * barWidth + barWidth / 2
                val isUp = kline.close >= kline.open
                val color = if (isUp) StockRed else StockGreen

                val highY = height - ((kline.high - minPrice) / priceRange * height).toFloat()
                val lowY = height - ((kline.low - minPrice) / priceRange * height).toFloat()
                val openY = height - ((kline.open - minPrice) / priceRange * height).toFloat()
                val closeY = height - ((kline.close - minPrice) / priceRange * height).toFloat()

                // 上下影線
                drawLine(
                    color = color,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = 1.5f
                )

                // 實體蠟燭棒
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
