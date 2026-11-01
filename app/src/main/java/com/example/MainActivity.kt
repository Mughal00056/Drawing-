package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.ScreenDrawSettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppDestination {
    CANVAS,
    SCREEN_DRAW_SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppDestination.CANVAS) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            AppDestination.CANVAS -> {
                                DrawingScreen(
                                    onOpenSettings = { currentScreen = AppDestination.SCREEN_DRAW_SETTINGS }
                                )
                            }
                            AppDestination.SCREEN_DRAW_SETTINGS -> {
                                BackHandler {
                                    currentScreen = AppDestination.CANVAS
                                }
                                ScreenDrawSettingsScreen(
                                    onNavigateBack = { currentScreen = AppDestination.CANVAS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
