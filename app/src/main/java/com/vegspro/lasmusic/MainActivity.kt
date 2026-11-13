package com.vegspro.lasmusic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
        Log.i("MainActivity", "onCreate")
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("CRASH", "Uncaught: ${e.message}", e)
        }
        setContent {
            LasMusicTheme {
                var phase by remember { mutableStateOf(0) }

                if (phase == 1) {
                    val vm: PlayerViewModel = viewModel()
                    LasMusicApp(playerViewModel = vm)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LasMusic", color = Color.White, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Базовый экран работает", color = Color(0xFFBB86FC))
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { phase = 1 }) {
                                Text("Открыть приложение")
                            }
                        }
                    }
                }
            }
        }
    }
}
