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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.abs

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
    val rawMin = prices.minOrNull() ?: previousClose
    val rawMax = prices.maxOrNull() ?: previousClose
    val maxDiff = maxOf(abs(rawMax - previousClose), abs(rawMin - previousClose), previousClose * 0.002).coerceAtLeast(0.01)

    // 以昨收為中心對稱擴展上下限，確保昨收橫線居中
    val displayMax = previousClose + maxDiff
    val displayMin = previousClose - maxDiff
    val priceRange = displayMax - displayMin

    // 全天 09:00 到 13:30 共 54 個 5 分鐘時間區隔
    val totalSlots = 54

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(ticks) {
                detectTapGestures { offset ->
                    val stepX = size.width / totalSlots.toFloat()
                    selectedIndex = (offset.x / stepX).toInt().coerceIn(0, ticks.size - 1)
                }
            }
            .pointerInput(ticks) {
                detectDragGestures { change, _ ->
                    val stepX = size.width / totalSlots.toFloat()
                    selectedIndex = (change.position.x / stepX).toInt().coerceIn(0, ticks.size - 1)
                }
            }
    ) {
        val width = size.width
        val height = size.height * 0.85f // 主走勢圖高度
        val stepX = width / totalSlots.toFloat()

        val baseLineY = height / 2f // 昨收線永遠置中

        // 1. 畫背景水平參考線與左側 Y 軸價格刻度
        val gridCount = 4
        val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (i in 0..gridCount) {
            val y = i.toFloat() / gridCount * height
            val priceVal = displayMax - (i.toFloat() / gridCount) * priceRange

            // 非中間線畫淡灰色細虛線
            if (i != 2) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    pathEffect = gridDashEffect,
                    strokeWidth = 1f
                )
            }

            // 左側 Y 軸價格文字
            val textColor = when {
                priceVal > previousClose + 0.001 -> StockRed
                priceVal < previousClose - 0.001 -> StockGreen
                else -> Color.LightGray
            }

            val textStr = if (i == 2) {
                "昨收 ${String.format(Locale.TAIWAN, "%,.2f", previousClose)}"
            } else {
                String.format(Locale.TAIWAN, "%,.2f", priceVal)
            }

            val textY = when (i) {
                0 -> 2.dp.toPx()
                gridCount -> height - 14.dp.toPx()
                else -> y - 6.dp.toPx()
            }

            drawText(
                textMeasurer = textMeasurer,
                text = textStr,
                style = TextStyle(
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = if (i == 2 || i == 0 || i == gridCount) FontWeight.Bold else FontWeight.Normal
                ),
                topLeft = Offset(4.dp.toPx(), textY)
            )
        }

        // 2. 畫昨收白色水平虛線
        val baseDashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(0f, baseLineY),
            end = Offset(width, baseLineY),
            pathEffect = baseDashEffect,
            strokeWidth = 1.5f
        )

        // 3. 畫時間 X 軸刻度 (09, 10, 11, 12, 13)
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
                topLeft = Offset((x - 8.dp.toPx()).coerceIn(0f, width - 20.dp.toPx()), size.height - 14.dp.toPx())
            )
        }

        // 4. 建立走勢 Line 與 Fill Path (根據真實時間累積之點位畫線)
        val trendPath = Path()
        val fillRedPath = Path()
        val fillGreenPath = Path()

        fillRedPath.moveTo(0f, baseLineY)
        fillGreenPath.moveTo(0f, baseLineY)

        ticks.forEachIndexed { i, tick ->
            val x = i * stepX
            val y = height / 2f - ((tick.price - previousClose) / maxDiff * (height / 2f)).toFloat()

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

        // 5. 上半部 Clip 畫紅色區，下半部 Clip 畫綠色區 (雙色走勢圖)
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
            val y = height / 2f - ((maVal - previousClose) / maxDiff * (height / 2f)).toFloat()

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

        // 7. 右側標註最高與最低數值 (避免與左側 Y 軸標籤重疊)
        drawText(
            textMeasurer = textMeasurer,
            text = "高 ${String.format(Locale.TAIWAN, "%,.2f", rawMax)}",
            style = TextStyle(color = StockRed, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(width - 70.dp.toPx(), 2.dp.toPx())
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "低 ${String.format(Locale.TAIWAN, "%,.2f", rawMin)}",
            style = TextStyle(color = StockGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            topLeft = Offset(width - 70.dp.toPx(), height - 14.dp.toPx())
        )

        // 8. 十字準星線與選取點數值標籤
        if (selectedIndex in ticks.indices) {
            val selTick = ticks[selectedIndex]
            val selX = selectedIndex * stepX
            val selY = height / 2f - ((selTick.price - previousClose) / maxDiff * (height / 2f)).toFloat()

            drawLine(
                color = Color.Yellow,
                start = Offset(selX, 0f),
                end = Offset(selX, size.height),
                strokeWidth = 1.5f
            )

            val selColor = if (selTick.price >= previousClose) StockRed else StockGreen
            val badgeWidth = 60.dp.toPx()
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
                topLeft = Offset(width - badgeWidth + 4.dp.toPx(), (selY - badgeHeight / 2).coerceIn(0f, height - badgeHeight) + 2.dp.toPx())
            )
        }
    }
}
