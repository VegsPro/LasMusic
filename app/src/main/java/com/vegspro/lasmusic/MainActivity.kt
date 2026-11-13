package com.vegspro.lasmusic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vegspro.lasmusic.player.PlayerViewModel
import com.vegspro.lasmusic.ui.LasMusicApp
import com.vegspro.lasmusic.ui.theme.LasMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MainActivity", "edgeToEdge", e)
        }
        setContent {
            LasMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    val vm: PlayerViewModel = viewModel()
                    LasMusicApp(playerViewModel = vm)
                }
            }
        }
    }
}
