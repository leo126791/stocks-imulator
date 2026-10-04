package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.stock_simulator.domain.model.KLinePoint
import com.example.stock_simulator.ui.theme.MA10Color
import com.example.stock_simulator.ui.theme.MA20Color
import com.example.stock_simulator.ui.theme.MA5Color
import com.example.stock_simulator.ui.theme.MA60Color
import com.example.stock_simulator.ui.theme.StockFlat
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun InteractiveKLineChart(
    allKLines: List<KLinePoint>,
    modifier: Modifier = Modifier
) {
    var rangeDays by remember { mutableIntStateOf(60) } // 預設 60 日
    var showMA5 by remember { mutableStateOf(true) }
    var showMA10 by remember { mutableStateOf(true) }
    var showMA20 by remember { mutableStateOf(true) }
    var showMA60 by remember { mutableStateOf(true) }

    // 雙指縮放與平移狀態
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetRatio by remember { mutableFloatStateOf(1f) } // 預設靠右最新數據

    // 依據時間範圍取 K 線基礎資料
    val baseKLines = remember(allKLines, rangeDays) {
        if (allKLines.isEmpty()) emptyList()
        else allKLines.takeLast(rangeDays)
    }

    val visibleCount = remember(baseKLines, zoomScale) {
        if (baseKLines.isEmpty()) 1
        else (baseKLines.size / zoomScale).toInt().coerceIn(10, baseKLines.size)
    }

    val maxStartIdx = remember(baseKLines, visibleCount) {
        maxOf(0, baseKLines.size - visibleCount)
    }

    val startIdx = remember(panOffsetRatio, maxStartIdx) {
        (panOffsetRatio * maxStartIdx).toInt().coerceIn(0, maxStartIdx)
    }

    val kLines = remember(baseKLines, startIdx, visibleCount) {
        if (baseKLines.isEmpty()) emptyList()
        else baseKLines.subList(startIdx, (startIdx + visibleCount).coerceAtMost(baseKLines.size))
    }

    var selectedIndex by remember(kLines) {
        mutableIntStateOf(if (kLines.isNotEmpty()) kLines.size - 1 else -1)
    }

    // 計算全圖均線數據
    val ma5List = remember(allKLines) { calculateMA(allKLines, 5) }
    val ma10List = remember(allKLines) { calculateMA(allKLines, 10) }
    val ma20List = remember(allKLines) { calculateMA(allKLines, 20) }
    val ma60List = remember(allKLines) { calculateMA(allKLines, 60) }

    // 對齊當前可見視窗的均線數據
    val offset = maxOf(0, allKLines.size - baseKLines.size) + startIdx
    val currentMA5 = remember(kLines, ma5List, offset) { ma5List.drop(offset).take(kLines.size) }
    val currentMA10 = remember(kLines, ma10List, offset) { ma10List.drop(offset).take(kLines.size) }
    val currentMA20 = remember(kLines, ma20List, offset) { ma20List.drop(offset).take(kLines.size) }
    val currentMA60 = remember(kLines, ma60List, offset) { ma60List.drop(offset).take(kLines.size) }

    val selectedPoint = kLines.getOrNull(selectedIndex) ?: kLines.lastOrNull()
    val prevPoint = selectedIndex.let { if (it > 0) kLines.getOrNull(it - 1) else null }
    val prevClose = prevPoint?.close ?: selectedPoint?.open ?: 0.0

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 範圍切換按鈕 (1個月, 3個月, 半年, 1年)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "K線與移動平均線 (MA)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    listOf(30 to "1月", 60 to "3月", 120 to "半年", 240 to "1年").forEach { (days, label) ->
                        FilterChip(
                            selected = rangeDays == days,
                            onClick = {
                                rangeDays = days
                                zoomScale = 1f
                                panOffsetRatio = 1f
                            },
                            label = { Text(label, style = TextStyle(fontSize = 11.sp)) },
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // 1. 頂部 K 線報價明細資料列 (日期/開/高/低/收/量/漲跌)
            selectedPoint?.let { point ->
                val change = point.close - prevClose
                val priceColor = when {
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = point.date,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        Text("開 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                        Text(
                            text = String.format(Locale.TAIWAN, "%.2f", point.open),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = priceColor)
                        )
                        Text("高 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                        Text(
                            text = String.format(Locale.TAIWAN, "%.2f", point.high),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockRed)
                        )
                        Text("低 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                        Text(
                            text = String.format(Locale.TAIWAN, "%.2f", point.low),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                        )
                        Text("收 ", style = TextStyle(fontSize = 12.sp, color = StockFlat))
                        Text(
                            text = String.format(Locale.TAIWAN, "%.2f", point.close),
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = priceColor)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("量(張): ${point.volume}", style = TextStyle(fontSize = 11.sp))
                        Text(
                            text = "漲跌: $sign${String.format(Locale.TAIWAN, "%.2f", change)}",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = priceColor)
                        )
                    }

                    // 2. 均線指示列 (點擊可開關顯示)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeIdx = if (selectedIndex in currentMA5.indices) selectedIndex else currentMA5.size - 1

                        MATag(
                            label = "MA5",
                            value = currentMA5.getOrNull(activeIdx),
                            color = MA5Color,
                            isChecked = showMA5,
                            onClick = { showMA5 = !showMA5 }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MATag(
                            label = "MA10",
                            value = currentMA10.getOrNull(activeIdx),
                            color = MA10Color,
                            isChecked = showMA10,
                            onClick = { showMA10 = !showMA10 }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MATag(
                            label = "MA20",
                            value = currentMA20.getOrNull(activeIdx),
                            color = MA20Color,
                            isChecked = showMA20,
                            onClick = { showMA20 = !showMA20 }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MATag(
                            label = "MA60",
                            value = currentMA60.getOrNull(activeIdx),
                            color = MA60Color,
                            isChecked = showMA60,
                            onClick = { showMA60 = !showMA60 }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 畫布 Canvas：繪製 K 線、均線、刻度網格、極值標註與十字準星觸控 (支援雙指縮放)
            if (kLines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("載入 K 線資料中...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val minPrice = kLines.minOf { it.low }
                val maxPrice = kLines.maxOf { it.high }
                val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .pointerInput(kLines) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1.0f, 5.0f)
                                if (maxStartIdx > 0) {
                                    val deltaRatio = -pan.x / (size.width * 0.5f)
                                    panOffsetRatio = (panOffsetRatio + deltaRatio).coerceIn(0f, 1f)
                                }
                            }
                        }
                        .pointerInput(kLines) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / kLines.size
                                selectedIndex = (offset.x / barWidth).toInt().coerceIn(0, kLines.size - 1)
                            }
                        }
                        .pointerInput(kLines) {
                            detectDragGestures { change, _ ->
                                val barWidth = size.width / kLines.size
                                selectedIndex = (change.position.x / barWidth).toInt().coerceIn(0, kLines.size - 1)
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height * 0.75f // 主 K 線圖佔上方 75%
                    val volHeight = size.height * 0.20f // 下方成交量佔 20%

                    val barWidth = width / kLines.size
                    val candleWidth = maxOf(barWidth * 0.65f, 3f)

                    // A. 繪製背景虛線網格 (Y 軸 4 等分)
                    val gridLines = 4
                    val priceStep = priceRange / gridLines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    for (i in 0..gridLines) {
                        val y = height - (i.toFloat() / gridLines) * height
                        val priceLabel = minPrice + i * priceStep

                        // 畫橫向虛線
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            pathEffect = dashEffect,
                            strokeWidth = 1f
                        )

                        // 畫右側價格刻度
                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format(Locale.TAIWAN, "%.1f", priceLabel),
                            style = TextStyle(color = Color.Gray, fontSize = 9.sp),
                            topLeft = Offset(width - 45.dp.toPx(), y - 12.dp.toPx())
                        )
                    }

                    // 找出全圖極值 (Highest / Lowest) 索引與座標，以便標註文字標籤
                    val maxIndex = kLines.indexOfFirst { it.high == maxPrice }
                    val minIndex = kLines.indexOfFirst { it.low == minPrice }

                    // B. 繪製 K 線柱體與影線，以及成交量柱狀圖
                    val maxVol = kLines.maxOf { it.volume }.toDouble().coerceAtLeast(1.0)

                    kLines.forEachIndexed { index, kline ->
                        val x = index * barWidth + barWidth / 2
                        val isUp = kline.close >= kline.open
                        val color = if (isUp) StockRed else StockGreen

                        val highY = height - ((kline.high - minPrice) / priceRange * height).toFloat()
                        val lowY = height - ((kline.low - minPrice) / priceRange * height).toFloat()
                        val openY = height - ((kline.open - minPrice) / priceRange * height).toFloat()
                        val closeY = height - ((kline.close - minPrice) / priceRange * height).toFloat()

                        // 畫上下影線
                        drawLine(
                            color = color,
                            start = Offset(x, highY),
                            end = Offset(x, lowY),
                            strokeWidth = 2f
                        )

                        // 畫實體棒
                        val bodyTop = minOf(openY, closeY)
                        val bodyHeight = maxOf(kotlin.math.abs(openY - closeY), 2f)

                        drawRect(
                            color = color,
                            topLeft = Offset(x - candleWidth / 2, bodyTop),
                            size = Size(candleWidth, bodyHeight)
                        )

                        // 畫下方成交量柱圖
                        val vHeight = (kline.volume / maxVol * volHeight).toFloat()
                        drawRect(
                            color = color.copy(alpha = 0.6f),
                            topLeft = Offset(x - candleWidth / 2, size.height - vHeight),
                            size = Size(candleWidth, vHeight)
                        )

                        // 標註高點數值標籤 (Red)
                        if (index == maxIndex) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = String.format(Locale.TAIWAN, "%.1f", maxPrice),
                                style = TextStyle(color = StockRed, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                topLeft = Offset((x - 15.dp.toPx()).coerceIn(0f, width - 40.dp.toPx()), maxOf(highY - 18.dp.toPx(), 0f))
                            )
                        }

                        // 標註低點數值標籤 (Green)
                        if (index == minIndex) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = String.format(Locale.TAIWAN, "%.1f", minPrice),
                                style = TextStyle(color = StockGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                topLeft = Offset((x - 15.dp.toPx()).coerceIn(0f, width - 40.dp.toPx()), minOf(lowY + 2.dp.toPx(), height - 15.dp.toPx()))
                            )
                        }
                    }

                    // C. 繪製均線 (MA5, MA10, MA20, MA60)
                    fun drawMALine(maList: List<Double?>, color: Color) {
                        val path = Path()
                        var started = false

                        maList.forEachIndexed { index, value ->
                            if (value != null) {
                                val x = index * barWidth + barWidth / 2
                                val y = height - ((value - minPrice) / priceRange * height).toFloat()
                                if (!started) {
                                    path.moveTo(x, y)
                                    started = true
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                        }
                        if (started) {
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }

                    if (showMA5) drawMALine(currentMA5, MA5Color)
                    if (showMA10) drawMALine(currentMA10, MA10Color)
                    if (showMA20) drawMALine(currentMA20, MA20Color)
                    if (showMA60) drawMALine(currentMA60, MA60Color)

                    // D. 繪製十字準星 (Crosshair) 與選取價格浮籤
                    if (selectedIndex in kLines.indices) {
                        val selected = kLines[selectedIndex]
                        val selX = selectedIndex * barWidth + barWidth / 2
                        val selCloseY = height - ((selected.close - minPrice) / priceRange * height).toFloat()

                        // 垂直準星線
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(selX, 0f),
                            end = Offset(selX, size.height),
                            strokeWidth = 1.5f
                        )

                        // 水平準星線
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(0f, selCloseY),
                            end = Offset(width, selCloseY),
                            strokeWidth = 1.5f
                        )

                        // 右側選取價格黑底白字卡
                        val badgeWidth = 45.dp.toPx()
                        val badgeHeight = 18.dp.toPx()
                        val badgeTop = (selCloseY - badgeHeight / 2).coerceIn(0f, height - badgeHeight)

                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(width - badgeWidth, badgeTop),
                            size = Size(badgeWidth, badgeHeight)
                        )

                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format(Locale.TAIWAN, "%.1f", selected.close),
                            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            topLeft = Offset(width - badgeWidth + 4.dp.toPx(), badgeTop + 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MATag(
    label: String,
    value: Double?,
    color: Color,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val textVal = value?.let { String.format(Locale.TAIWAN, "%.2f", it) } ?: "--"
    val alpha = if (isChecked) 1.0f else 0.4f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(color.copy(alpha = alpha), shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$label $textVal",
            style = TextStyle(
                color = color.copy(alpha = alpha),
                fontSize = 11.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

/**
 * 移動平均線 (MA) 計算函數
 */
private fun calculateMA(kLines: List<KLinePoint>, period: Int): List<Double?> {
    if (kLines.isEmpty()) return emptyList()
    val maList = mutableListOf<Double?>()

    for (i in kLines.indices) {
        if (i < period - 1) {
            maList.add(null)
        } else {
            var sum = 0.0
            for (j in (i - period + 1)..i) {
                sum += kLines[j].close
            }
            maList.add(sum / period)
        }
    }
    return maList
}
