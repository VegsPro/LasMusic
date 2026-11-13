package com.vegspro.lasmusic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vegspro.lasmusic.player.PlayerViewModel
import com.vegspro.lasmusic.ui.LasMusicApp
import com.vegspro.lasmusic.ui.theme.LasMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate start")
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }
        setContent {
            LasMusicTheme {
                var error by remember { mutableStateOf<String?>(null) }
                if (error != null) {
                    ErrorScreen(error!!)
                } else {
                    try {
                        val vm: PlayerViewModel = viewModel()
                        LasMusicApp(playerViewModel = vm)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Compose content failed", e)
                        error = e.message ?: e.javaClass.simpleName
                        ErrorScreen(error!!)
                    }
                }
            }
        }
        Log.i("MainActivity", "onCreate end")
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ошибка запуска:\n$message",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
