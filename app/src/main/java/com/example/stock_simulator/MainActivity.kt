package com.example.stock_simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.stock_simulator.ui.navigation.AppNavigation
import com.example.stock_simulator.ui.theme.StocksimulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StocksimulatorTheme {
                AppNavigation()
            }
        }
    }
}
