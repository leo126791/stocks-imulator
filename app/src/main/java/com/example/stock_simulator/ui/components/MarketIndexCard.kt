package com.example.stock_simulator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.MarketIndex
import com.example.stock_simulator.ui.theme.StockFlat
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun MarketIndexCard(
    indexItem: MarketIndex,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUp = indexItem.change >= 0
    val themeColor = if (isUp) StockRed else StockGreen
    val sign = if (isUp) "▲ " else "▼ "

    Card(
        onClick = onClick,
        modifier = modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 指數名稱 (如: 上市)
            Text(
                text = indexItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 當前指數價格 (如: 48,475.74)
            Text(
                text = String.format(Locale.TAIWAN, "%,.2f", indexItem.currentPrice),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = themeColor,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // 漲跌金額與幅度 (如: ▲ 122.25 (0.25%))
            Text(
                text = String.format(Locale.TAIWAN, "%s%.2f (%.2f%%)", sign, indexItem.change, indexItem.changePercent),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )

            // 成交金額 (如: 成交 8,997.10 億)
            Text(
                text = "成交 ${indexItem.turnoverAmount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 即時走勢微型趨勢圖 (Sparkline Area Chart)
            MiniTrendChart(
                points = indexItem.trendPoints,
                previousClose = indexItem.previousClose,
                themeColor = themeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 開盤/最高/昨收/最低 統計網格
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("開盤 ${String.format(Locale.TAIWAN, "%,.2f", indexItem.openPrice)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("最高 ${String.format(Locale.TAIWAN, "%,.2f", indexItem.highPrice)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("昨收 ${String.format(Locale.TAIWAN, "%,.2f", indexItem.previousClose)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("最低 ${String.format(Locale.TAIWAN, "%,.2f", indexItem.lowPrice)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MiniTrendChart(
    points: List<Double>,
    previousClose: Double,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val minVal = minOf(points.minOrNull() ?: previousClose, previousClose)
    val maxVal = maxOf(points.maxOrNull() ?: previousClose, previousClose)
    val valRange = if (maxVal > minVal) maxVal - minVal else 1.0

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val stepX = width / (points.size - 1).coerceAtLeast(1)

        // 1. 畫昨收虛線基準線
        val baseLineY = height - ((previousClose - minVal) / valRange * height).toFloat()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        drawLine(
            color = StockFlat.copy(alpha = 0.5f),
            start = Offset(0f, baseLineY),
            end = Offset(width, baseLineY),
            pathEffect = dashEffect,
            strokeWidth = 1.5f
        )

        // 2. 建立走勢 Line 與 Area 漸層填充 Path
        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, price ->
            val x = index * stepX
            val y = height - ((price - minVal) / valRange * height).toFloat()

            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // 3. 畫漸層區域
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    themeColor.copy(alpha = 0.35f),
                    themeColor.copy(alpha = 0.05f)
                )
            )
        )

        // 4. 畫走勢折線
        drawPath(
            path = strokePath,
            color = themeColor,
            style = Stroke(width = 2.5f)
        )
    }
}
