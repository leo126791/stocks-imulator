package com.example.stock_simulator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stock_simulator.ui.theme.StockFlat
import com.example.stock_simulator.ui.theme.StockGreen
import com.example.stock_simulator.ui.theme.StockRed
import java.util.Locale

@Composable
fun StockPriceText(
    price: Double,
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val color = when {
        change > 0 -> StockRed
        change < 0 -> StockGreen
        else -> StockFlat
    }

    val sign = when {
        change > 0 -> "▲ +"
        change < 0 -> "▼ "
        else -> ""
    }

    val priceText = String.format(Locale.TAIWAN, "%.2f", price)
    val changeText = String.format(Locale.TAIWAN, "%s%.2f (%.2f%%)", sign, change, changePercent)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = priceText,
            style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = changeText,
            style = if (isLarge) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
