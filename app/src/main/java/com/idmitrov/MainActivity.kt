package com.idmitrov

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("news") }

    when (currentScreen) {
        "news" -> NewsScreen(
            onNavigateToAdmin = { currentScreen = "login" }
        )
        "login" -> LoginScreen(
            onLoginSuccess = { currentScreen = "admin" },
            onBack = { currentScreen = "news" }
        )
        "admin" -> AdminScreen(
            onBack = { currentScreen = "news" }
        )
    }
}
