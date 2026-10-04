package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.IntradayTick
import com.example.stock_simulator.ui.theme.StockFlat
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun IntradayTrendChart(
    ticks: List<IntradayTick>,
    previousClose: Double,
    modifier: Modifier = Modifier
) {
    // 雙指縮放與平移狀態
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetRatio by remember { mutableFloatStateOf(1f) } // 預設靠右最新數據

    val visibleCount = remember(ticks, zoomScale) {
        if (ticks.isEmpty()) 1
        else (ticks.size / zoomScale).toInt().coerceIn(10, ticks.size)
    }

    val maxStartIdx = remember(ticks, visibleCount) {
        maxOf(0, ticks.size - visibleCount)
    }

    val startIdx = remember(panOffsetRatio, maxStartIdx) {
        (panOffsetRatio * maxStartIdx).toInt().coerceIn(0, maxStartIdx)
    }

    val visibleTicks = remember(ticks, startIdx, visibleCount) {
        if (ticks.isEmpty()) emptyList()
        else ticks.subList(startIdx, (startIdx + visibleCount).coerceAtMost(ticks.size))
    }

    var selectedIndex by remember(visibleTicks) {
        mutableIntStateOf(if (visibleTicks.isNotEmpty()) visibleTicks.size - 1 else -1)
    }

    val selectedTick = visibleTicks.getOrNull(selectedIndex) ?: visibleTicks.lastOrNull()
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1. 頂部資訊列 (時間, 價, 最高價, 最低價, 漲跌)
            selectedTick?.let { tick ->
                val change = tick.price - previousClose
                val color = when {
                    change > 0 -> StockRed
                    change < 0 -> StockGreen
                    else -> StockFlat
                }
                val sign = if (change > 0) "+" else ""

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "時間: ${tick.time}",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        Row {
                            Text("價 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                            Text(
                                text = String.format(Locale.TAIWAN, "%,.2f", tick.price),
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                            )
                        }
                        Row {
                            Text("漲跌 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                            Text(
                                text = String.format(Locale.TAIWAN, "%s%.2f", sign, change),
                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val maxPrice = ticks.maxOfOrNull { it.price } ?: previousClose
                        val minPrice = ticks.minOfOrNull { it.price } ?: previousClose

                        Text("最高 ${String.format(Locale.TAIWAN, "%,.2f", maxPrice)}", style = TextStyle(fontSize = 11.sp, color = StockRed))
                        Text("最低 ${String.format(Locale.TAIWAN, "%,.2f", minPrice)}", style = TextStyle(fontSize = 11.sp, color = StockGreen))
                        Text(
                            text = if (zoomScale > 1.05f) "雙指縮放中 (${String.format(Locale.TAIWAN, "%.1fx", zoomScale)})" else "雙指可縮放查看",
                            style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 分時走勢圖 Canvas 繪圖區 (支援雙指縮放與拖曳平移)
            if (visibleTicks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("載入盤中走勢資料...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val prices = visibleTicks.map { it.price }
                val minPrice = minOf(prices.minOrNull() ?: previousClose, previousClose)
                val maxPrice = maxOf(prices.maxOrNull() ?: previousClose, previousClose)
                val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .pointerInput(visibleTicks) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1.0f, 5.0f)
                                if (maxStartIdx > 0) {
                                    val deltaRatio = -pan.x / (size.width * 0.5f)
                                    panOffsetRatio = (panOffsetRatio + deltaRatio).coerceIn(0f, 1f)
                                }
                            }
                        }
                        .pointerInput(visibleTicks) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (visibleTicks.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / stepX).toInt().coerceIn(0, visibleTicks.size - 1)
                            }
                        }
                        .pointerInput(visibleTicks) {
                            detectDragGestures { change, _ ->
                                val stepX = size.width / (visibleTicks.size - 1).coerceAtLeast(1)
                                selectedIndex = (change.position.x / stepX).toInt().coerceIn(0, visibleTicks.size - 1)
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height * 0.75f // 主圖高佔 75%
                    val volHeight = size.height * 0.20f // 下方成交量高佔 20%

                    val stepX = width / (visibleTicks.size - 1).coerceAtLeast(1)

                    // A. 畫 5 條水平虛線與價格刻度 (Y 軸)
                    val gridCount = 4
                    val priceStep = priceRange / gridCount
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    for (i in 0..gridCount) {
                        val y = height - (i.toFloat() / gridCount) * height
                        val priceLabel = minPrice + i * priceStep

                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            pathEffect = dashEffect,
                            strokeWidth = 1f
                        )

                        // 刻度文字
                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format(Locale.TAIWAN, "%,.1f", priceLabel),
                            style = TextStyle(color = Color.Gray, fontSize = 9.sp),
                            topLeft = Offset(width - 45.dp.toPx(), y - 10.dp.toPx())
                        )
                    }

                    // B. 畫 昨收 基準線 (白色虛線與昨收價標籤)
                    val baseLineY = height - ((previousClose - minPrice) / priceRange * height).toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(0f, baseLineY),
                        end = Offset(width, baseLineY),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        strokeWidth = 2f
                    )

                    // 昨收價黑底框與文字
                    val baseBadgeWidth = 62.dp.toPx()
                    val baseBadgeHeight = 18.dp.toPx()
                    drawRect(
                        color = Color.DarkGray,
                        topLeft = Offset(width - baseBadgeWidth, baseLineY - baseBadgeHeight / 2),
                        size = Size(baseBadgeWidth, baseBadgeHeight)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "昨收 ${String.format(Locale.TAIWAN, "%,.1f", previousClose)}",
                        style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        topLeft = Offset(width - baseBadgeWidth + 2.dp.toPx(), baseLineY - baseBadgeHeight / 2 + 2.dp.toPx())
                    )

                    // C. 畫時間刻度線與文字
                    val timeLabels = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "13:30")
                    val timeStepWidth = width / (timeLabels.size - 1)

                    timeLabels.forEachIndexed { i, label ->
                        val x = i * timeStepWidth
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            style = TextStyle(color = Color.Gray, fontSize = 9.sp),
                            topLeft = Offset((x - 12.dp.toPx()).coerceIn(0f, width - 25.dp.toPx()), size.height - 14.dp.toPx())
                        )
                    }

                    // D. 畫藍色/紅色走勢折線與區域漸層填充
                    val linePath = Path()
                    val fillPath = Path()

                    val lineTrendColor = Color(0xFF1E88E5) // 經典分時走勢藍線

                    visibleTicks.forEachIndexed { i, tick ->
                        val x = i * stepX
                        val y = height - ((tick.price - minPrice) / priceRange * height).toFloat()

                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, baseLineY)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (i == visibleTicks.size - 1) {
                            fillPath.lineTo(x, baseLineY)
                            fillPath.close()
                        }
                    }

                    // 畫區域漸層
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineTrendColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // 畫走勢線
                    drawPath(
                        path = linePath,
                        color = lineTrendColor,
                        style = Stroke(width = 2.5f)
                    )

                    // E. 下方成交量柱圖 (Red/Green)
                    val maxVol = visibleTicks.maxOf { it.volume }.toDouble().coerceAtLeast(1.0)
                    visibleTicks.forEachIndexed { i, tick ->
                        val x = i * stepX
                        val vHeight = (tick.volume / maxVol * volHeight).toFloat()
                        val volColor = if (tick.price >= previousClose) StockRed else StockGreen

                        drawLine(
                            color = volColor.copy(alpha = 0.7f),
                            start = Offset(x, size.height - 16.dp.toPx()),
                            end = Offset(x, size.height - 16.dp.toPx() - vHeight),
                            strokeWidth = maxOf(stepX * 0.7f, 1.5f)
                        )
                    }

                    // F. 十字準星線與選取價格卡牌 (Crosshair)
                    if (selectedIndex in visibleTicks.indices) {
                        val selTick = visibleTicks[selectedIndex]
                        val selX = selectedIndex * stepX
                        val selY = height - ((selTick.price - minPrice) / priceRange * height).toFloat()

                        // 垂直準星
                        drawLine(
                            color = Color.Black,
                            start = Offset(selX, 0f),
                            end = Offset(selX, size.height - 16.dp.toPx()),
                            strokeWidth = 2f
                        )

                        // 水平準星
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, selY),
                            end = Offset(width, selY),
                            strokeWidth = 1.5f
                        )

                        // 選取價格紅/綠色標籤卡
                        val badgeColor = if (selTick.price >= previousClose) StockRed else StockGreen
                        val badgeWidth = 52.dp.toPx()
                        val badgeHeight = 20.dp.toPx()
                        val badgeTop = (selY - badgeHeight / 2).coerceIn(0f, height - badgeHeight)

                        drawRect(
                            color = badgeColor,
                            topLeft = Offset(width - badgeWidth, badgeTop),
                            size = Size(badgeWidth, badgeHeight)
                        )

                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format(Locale.TAIWAN, "%,.1f", selTick.price),
                            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            topLeft = Offset(width - badgeWidth + 4.dp.toPx(), badgeTop + 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
