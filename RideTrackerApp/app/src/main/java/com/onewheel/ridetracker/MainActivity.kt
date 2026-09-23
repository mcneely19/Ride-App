package com.onewheel.ridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onewheel.ridetracker.ui.HomeScreen
import com.onewheel.ridetracker.ui.theme.RideTelemetryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RideTelemetryTheme {
                HomeScreen()
            }
        }
    }
}
