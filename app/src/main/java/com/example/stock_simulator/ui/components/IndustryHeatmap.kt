package com.example.stock_simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.domain.model.IndustrySector
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun IndustryHeatmap(
    sectors: List<IndustrySector>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 標題與分類標籤
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "產業動態熱圖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "依市值比重",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 塊狀熱圖 Treemap (左大塊, 右側分割小塊)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // 左側主熱區 (例: 半導體業 +2.41%)
                val mainSector = sectors.firstOrNull() ?: IndustrySector("半導體業", 2.41, 0.5f)

                HeatmapBlock(
                    sector = mainSector,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp)
                )

                // 右側次要熱區 (電子組件, 通信網路, 電腦週邊)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val sub1 = sectors.getOrNull(1) ?: IndustrySector("電子組件", 3.02, 0.25f)
                    val sub2 = sectors.getOrNull(2) ?: IndustrySector("通信網路", 5.62, 0.15f)

                    HeatmapBlock(
                        sector = sub1,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        HeatmapBlock(
                            sector = sub2,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 4.dp)
                        )

                        val sub3 = sectors.getOrNull(3) ?: IndustrySector("電腦週邊", 0.40, 0.1f)
                        HeatmapBlock(
                            sector = sub3,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapBlock(
    sector: IndustrySector,
    modifier: Modifier = Modifier
) {
    val isUp = sector.changePercent >= 0
    val bgColor = if (isUp) StockRed.copy(alpha = 0.85f) else StockGreen.copy(alpha = 0.85f)
    val sign = if (isUp) "+" else ""

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = sector.name,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.TAIWAN, "%s%.2f%%", sign, sector.changePercent),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
