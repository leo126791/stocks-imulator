package com.example.stock_simulator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun InOutVolumeBar(
    inVolume: Long,
    outVolume: Long,
    modifier: Modifier = Modifier
) {
    val totalVol = (inVolume + outVolume).coerceAtLeast(1L)
    val inPct = (inVolume.toDouble() / totalVol) * 100
    val outPct = (outVolume.toDouble() / totalVol) * 100

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. 文字列： 內盤  159,357(38.35%)  256,229(61.65%)  外盤
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "內盤",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format(Locale.TAIWAN, "%,d(%.2f%%)", inVolume, inPct),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StockGreen)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = String.format(Locale.TAIWAN, "%,d(%.2f%%)", outVolume, outPct),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StockRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "外盤",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. 雙色對比比率條 (左綠內盤 右紅外盤)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            val inWeight = (inPct / 100.0).toFloat().coerceIn(0.01f, 0.99f)
            val outWeight = 1.0f - inWeight

            Box(
                modifier = Modifier
                    .weight(inWeight)
                    .fillMaxHeight()
                    .background(StockGreen)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .weight(outWeight)
                    .fillMaxHeight()
                    .background(StockRed)
            )
        }
    }
}
