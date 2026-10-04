package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.IntradayTick
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun DualColorIntradayChart(
    ticks: List<IntradayTick>,
    previousClose: Double,
    modifier: Modifier = Modifier
) {
    if (ticks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("載入大盤走勢數據...", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var selectedIndex by remember(ticks) {
        mutableIntStateOf(if (ticks.isNotEmpty()) ticks.size - 1 else -1)
    }

    val textMeasurer = rememberTextMeasurer()

    val prices = ticks.map { it.price }
    val minPrice = minOf(prices.minOrNull() ?: previousClose, previousClose)
    val maxPrice = maxOf(prices.maxOrNull() ?: previousClose, previousClose)
    val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(ticks) {
                detectTapGestures { offset ->
                    val stepX = size.width / (ticks.size - 1).coerceAtLeast(1)
                    selectedIndex = (offset.x / stepX).toInt().coerceIn(0, ticks.size - 1)
                }
            }
            .pointerInput(ticks) {
                detectDragGestures { change, _ ->
                    val stepX = size.width / (ticks.size - 1).coerceAtLeast(1)
                    selectedIndex = (change.position.x / stepX).toInt().coerceIn(0, ticks.size - 1)
                }
            }
    ) {
        val width = size.width
        val height = size.height * 0.85f // 主走勢圖高
        val stepX = width / (ticks.size - 1).coerceAtLeast(1)

        val baseLineY = height - ((previousClose - minPrice) / priceRange * height).toFloat()

        // 1. 畫左側 Y 軸價格刻度 (上紅、中灰昨收、下綠)
        val p1 = previousClose + priceRange * 0.4
        val p2 = previousClose + priceRange * 0.2
        val p3 = previousClose
        val p4 = previousClose - priceRange * 0.2
        val p5 = previousClose - priceRange * 0.4

        drawText(textMeasurer, String.format(Locale.TAIWAN, "%.2f", p1), Offset(4.dp.toPx(), 4.dp.toPx()), TextStyle(color = StockRed, fontSize = 10.sp, fontWeight = FontWeight.Bold))
        drawText(textMeasurer, String.format(Locale.TAIWAN, "%.2f", p2), Offset(4.dp.toPx(), height * 0.25f - 6.dp.toPx()), TextStyle(color = StockRed, fontSize = 10.sp))
        drawText(textMeasurer, String.format(Locale.TAIWAN, "%.2f", p3), Offset(4.dp.toPx(), baseLineY - 6.dp.toPx()), TextStyle(color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold))
        drawText(textMeasurer, String.format(Locale.TAIWAN, "%.2f", p4), Offset(4.dp.toPx(), height * 0.75f - 6.dp.toPx()), TextStyle(color = StockGreen, fontSize = 10.sp))
        drawText(textMeasurer, String.format(Locale.TAIWAN, "%.2f", p5), Offset(4.dp.toPx(), height - 16.dp.toPx()), TextStyle(color = StockGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold))

        // 2. 畫昨收白色水平虛線與昨收標籤
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(0f, baseLineY),
            end = Offset(width, baseLineY),
            pathEffect = dashEffect,
            strokeWidth = 2f
        )

        // 昨收價標籤 (白色/黑底)
        drawText(
            textMeasurer = textMeasurer,
            text = "昨收 ${String.format(Locale.TAIWAN, "%,.2f", previousClose)}",
            style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
            topLeft = Offset(10.dp.toPx(), baseLineY - 14.dp.toPx())
        )

        // 3. 畫時間 X 軸 (09, 10, 11, 12, 13)
        val timeLabels = listOf("09", "10", "11", "12", "13")
        val timeStepWidth = width / (timeLabels.size - 1)

        timeLabels.forEachIndexed { i, label ->
            val x = i * timeStepWidth
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = TextStyle(color = Color.Gray, fontSize = 10.sp),
                topLeft = Offset((x - 6.dp.toPx()).coerceIn(0f, width - 20.dp.toPx()), size.height - 14.dp.toPx())
            )
        }

        // 4. 建立走勢 Line 與 Fill Path
        val trendPath = Path()
        val fillRedPath = Path()
        val fillGreenPath = Path()

        fillRedPath.moveTo(0f, baseLineY)
        fillGreenPath.moveTo(0f, baseLineY)

        ticks.forEachIndexed { i, tick ->
            val x = i * stepX
            val y = height - ((tick.price - minPrice) / priceRange * height).toFloat()

            if (i == 0) {
                trendPath.moveTo(x, y)
            } else {
                trendPath.lineTo(x, y)
            }

            fillRedPath.lineTo(x, y)
            fillGreenPath.lineTo(x, y)

            if (i == ticks.size - 1) {
                fillRedPath.lineTo(x, baseLineY)
                fillRedPath.close()

                fillGreenPath.lineTo(x, baseLineY)
                fillGreenPath.close()
            }
        }

        // 5. 上半部 Clip 畫紅色區，下半部 Clip 畫綠色區 (三竹股市經典雙色)
        clipRect(0f, 0f, width, baseLineY, clipOp = ClipOp.Intersect) {
            drawPath(
                path = fillRedPath,
                brush = Brush.verticalGradient(
                    colors = listOf(StockRed.copy(alpha = 0.45f), StockRed.copy(alpha = 0.05f)),
                    startY = 0f,
                    endY = baseLineY
                )
            )
            drawPath(
                path = trendPath,
                color = StockRed,
                style = Stroke(width = 2.5f)
            )
        }

        clipRect(0f, baseLineY, width, height, clipOp = ClipOp.Intersect) {
            drawPath(
                path = fillGreenPath,
                brush = Brush.verticalGradient(
                    colors = listOf(StockGreen.copy(alpha = 0.05f), StockGreen.copy(alpha = 0.45f)),
                    startY = baseLineY,
                    endY = height
                )
            )
            drawPath(
                path = trendPath,
                color = StockGreen,
                style = Stroke(width = 2.5f)
            )
        }

        // 6. 畫白色 MA 均線
        val maPath = Path()
        var maStarted = false
        var maSum = 0.0
        val window = 5

        ticks.forEachIndexed { i, tick ->
            maSum += tick.price
            if (i >= window) maSum -= ticks[i - window].price
            val count = minOf(i + 1, window)
            val maVal = maSum / count

            val x = i * stepX
            val y = height - ((maVal - minPrice) / priceRange * height).toFloat()

            if (!maStarted) {
                maPath.moveTo(x, y)
                maStarted = true
            } else {
                maPath.lineTo(x, y)
            }
        }

        drawPath(
            path = maPath,
            color = Color.White.copy(alpha = 0.8f),
            style = Stroke(width = 1.5f)
        )

        // 7. 標註最高與最低數值
        drawText(
            textMeasurer = textMeasurer,
            text = String.format(Locale.TAIWAN, "%,.2f", maxPrice),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(width - 60.dp.toPx(), 4.dp.toPx())
        )

        drawText(
            textMeasurer = textMeasurer,
            text = String.format(Locale.TAIWAN, "%,.2f", minPrice),
            style = TextStyle(color = StockGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(50.dp.toPx(), height - 20.dp.toPx())
        )

        // 8. 十字準星
        if (selectedIndex in ticks.indices) {
            val selTick = ticks[selectedIndex]
            val selX = selectedIndex * stepX
            val selY = height - ((selTick.price - minPrice) / priceRange * height).toFloat()

            drawLine(
                color = Color.Yellow,
                start = Offset(selX, 0f),
                end = Offset(selX, size.height),
                strokeWidth = 1.5f
            )

            val selColor = if (selTick.price >= previousClose) StockRed else StockGreen
            val badgeWidth = 55.dp.toPx()
            val badgeHeight = 18.dp.toPx()

            drawRect(
                color = selColor,
                topLeft = Offset(width - badgeWidth, (selY - badgeHeight / 2).coerceIn(0f, height - badgeHeight)),
                size = Size(badgeWidth, badgeHeight)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.TAIWAN, "%,.2f", selTick.price),
                style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                topLeft = Offset(width - badgeWidth + 2.dp.toPx(), (selY - badgeHeight / 2).coerceIn(0f, height - badgeHeight) + 2.dp.toPx())
            )
        }
    }
}
