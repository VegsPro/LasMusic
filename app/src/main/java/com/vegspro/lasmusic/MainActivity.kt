package com.vegspro.lasmusic

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vegspro.lasmusic.player.PlayerViewModel
import com.vegspro.lasmusic.youtube.YouTubeMusicRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("LasMusic", "MainActivity onCreate")
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFBB86FC),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    val vm: PlayerViewModel = viewModel()
                    AppRoot(vm)
                }
            }
        }
    }
}

@Composable
fun AppRoot(vm: PlayerViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val isPlaying by vm.isPlaying.collectAsState()
    val current by vm.currentItem.collectAsState()
    val status by vm.statusMessage.collectAsState()

    Box(Modifier.fillMaxSize().background(Color(0xFF121212))) {
        when (tab) {
            0 -> HomeTab(vm)
            1 -> SearchTab(vm)
            2 -> LibraryTab(vm)
        }

        // Mini player
        current?.let { item ->
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF2A2A30), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.mediaMetadata.title?.toString() ?: "—",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text(
                        item.mediaMetadata.artist?.toString() ?: "",
                        color = Color.White.copy(0.6f),
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { vm.togglePlayPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White)
                }
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .height(56.dp)
                .background(Color.White.copy(0.1f), RoundedCornerShape(28.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Icons.Default.Home to "Главная",
                Icons.Default.Search to "Поиск",
                Icons.Default.LibraryMusic to "Библиотека"
            ).forEachIndexed { i, (icon, label) ->
                val sel = tab == i
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { tab = i }
                        .padding(4.dp)
                ) {
                    Icon(icon, label, tint = if (sel) Color(0xFFBB86FC) else Color.White.copy(0.5f))
                    Text(label, color = if (sel) Color(0xFFBB86FC) else Color.White.copy(0.5f), fontSize = 10.sp)
                }
            }
        }

        status?.let { msg ->
            Text(
                msg,
                color = Color.White.copy(0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun HomeTab(vm: PlayerViewModel) {
    val home by vm.homeTracks.collectAsState()
    val charts by vm.chartTracks.collectAsState()
    val loading by vm.isLoadingHome.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 36.dp, bottom = 140.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("LasMusic", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.loadHome() }) { Text("Обновить") }
            }
        }
        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC))
                }
            }
        }
        if (home.isNotEmpty()) {
            item { Text("Рекомендуем", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
            items(home, key = { it.videoId }) { track ->
                TrackRow(track) { vm.playYtmTrack(track) }
            }
        }
        if (charts.isNotEmpty()) {
            item { Text("Чарты", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
            items(charts, key = { "c_${it.videoId}" }) { track ->
                TrackRow(track) { vm.playYtmTrack(track) }
            }
        }
        if (!loading && home.isEmpty() && charts.isEmpty()) {
            item {
                Text(
                    "Нет данных. Нажми Обновить или открой Поиск.",
                    color = Color.White.copy(0.6f),
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
fun SearchTab(vm: PlayerViewModel) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val searching by vm.isSearching.collectAsState()

    Column(Modifier.fillMaxSize().padding(top = 36.dp, bottom = 140.dp).padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск музыки…") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFBB86FC),
                unfocusedBorderColor = Color.White.copy(0.3f)
            )
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (query.isNotBlank()) vm.search(query) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            Text("Искать", color = Color.Black)
        }
        if (searching) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFBB86FC))
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(results, key = { it.videoId }) { track ->
                TrackRow(track) { vm.playYtmTrack(track) }
            }
        }
    }
}

@Composable
fun LibraryTab(vm: PlayerViewModel) {
    val liked by vm.likedTracks.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 36.dp, bottom = 140.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text("Любимое", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (liked.isEmpty()) {
            item { Text("Пока пусто — лайкай треки в поиске", color = Color.White.copy(0.5f)) }
        } else {
            items(liked, key = { it.videoId }) { track ->
                TrackRow(track, showLike = true, onLike = { vm.toggleLike(track) }) {
                    vm.playYtmTrack(track)
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    track: YouTubeMusicRepository.Track,
    showLike: Boolean = false,
    onLike: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.05f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF333333), RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(track.artist, color = Color.White.copy(0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
        if (showLike && onLike != null) {
            TextButton(onClick = onLike) { Text("♥", color = Color(0xFFBB86FC)) }
        }
    }
}
