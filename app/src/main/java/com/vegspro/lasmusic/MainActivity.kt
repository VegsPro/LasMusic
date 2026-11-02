package com.vegspro.lasmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vegspro.lasmusic.ui.LasMusicApp
import com.vegspro.lasmusic.ui.theme.LasMusicTheme
import com.vegspro.lasmusic.player.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LasMusicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val playerViewModel: PlayerViewModel = viewModel()
                    LasMusicApp(playerViewModel = playerViewModel)
                }
            }
        }
    }
}
