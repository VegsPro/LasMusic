package com.vegspro.lasmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vegspro.lasmusic.player.PlayerViewModel

data class AlbumCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val imageUrl: String,
    val mediaItem: MediaItem
)

private fun track(
    id: String,
    title: String,
    artist: String,
    uri: String,
    art: String? = null
): MediaItem {
    val meta = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
    if (art != null) meta.setArtworkUri(android.net.Uri.parse(art))
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(meta.build())
        .build()
}

private val recommended = listOf(
    AlbumCard("1", "SoundHelix 1", "Demo Track • Progressive", Color(0xFF8B4513),
        "https://picsum.photos/seed/las1/400",
        track("1", "SoundHelix 1", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://picsum.photos/seed/las1/400")),
    AlbumCard("2", "SoundHelix 2", "Demo Track • Progressive", Color(0xFFB71C1C),
        "https://picsum.photos/seed/las2/400",
        track("2", "SoundHelix 2", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://picsum.photos/seed/las2/400")),
    AlbumCard("3", "SoundHelix 3", "Demo Track • Progressive", Color(0xFF1A237E),
        "https://picsum.photos/seed/las3/400",
        track("3", "SoundHelix 3", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://picsum.photos/seed/las3/400")),
    AlbumCard("4", "SoundHelix 4", "Demo Track • Progressive", Color(0xFF4A148C),
        "https://picsum.photos/seed/las4/400",
        track("4", "SoundHelix 4", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://picsum.photos/seed/las4/400"))
)

private val newReleases = listOf(
    AlbumCard("5", "SoundHelix 5", "Demo Track • Progressive", Color(0xFF212121),
        "https://picsum.photos/seed/las5/400",
        track("5", "SoundHelix 5", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://picsum.photos/seed/las5/400")),
    AlbumCard("6", "SoundHelix 6", "Demo Track • Progressive", Color(0xFFAD1457),
        "https://picsum.photos/seed/las6/400",
        track("6", "SoundHelix 6", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://picsum.photos/seed/las6/400")),
    AlbumCard("7", "SoundHelix 7", "Demo Track • Progressive", Color(0xFF004D40),
        "https://picsum.photos/seed/las7/400",
        track("7", "SoundHelix 7", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "https://picsum.photos/seed/las7/400")),
    AlbumCard("8", "SoundHelix 8", "Demo Track • Progressive", Color(0xFF01579B),
        "https://picsum.photos/seed/las8/400",
        track("8", "SoundHelix 8", "Demo Track", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://picsum.photos/seed/las8/400"))
)

private val artists = listOf(
    "The Weeknd" to Color(0xFF6A1B9A),
    "Playboi Carti" to Color(0xFFC62828),
    "Travis Scott" to Color(0xFF283593),
    "Billie Eilish" to Color(0xFF2E7D32)
)

private val libraryItems = listOf(
    "Любимое" to Icons.Default.Favorite,
    "Недавние" to Icons.Default.History,
    "Скачанное" to Icons.Default.Download,
    "Мои плейлисты" to Icons.Default.PlaylistPlay,
    "Альбомы" to Icons.Default.Album,
    "Исполнители" to Icons.Default.Person
)

@Composable
fun LasMusicApp(playerViewModel: PlayerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val current by playerViewModel.currentItem.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF121212), Color(0xFF0D0D12))
                )
            )
            .statusBarsPadding()
    ) {
        when (selectedTab) {
            0 -> HomeScreen(onPlayAlbum = { album -> playerViewModel.playTrack(album.mediaItem) })
            1 -> MixScreen()
            2 -> LibraryScreen()
            3 -> SearchScreen(playerViewModel)
        }

        if (current != null) {
            MiniPlayer(
                title = current?.mediaMetadata?.title?.toString() ?: "Unknown",
                artist = current?.mediaMetadata?.artist?.toString() ?: "",
                isPlaying = isPlaying,
                onPlayPause = { playerViewModel.togglePlayPause() },
                onNext = { playerViewModel.next() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 78.dp)
                    .padding(horizontal = 12.dp)
            )
        }

        GlassBottomBar(
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun HomeScreen(onPlayAlbum: (AlbumCard) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = 160.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = true, onClick = {},
                        label = { Text("Все") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFBB86FC).copy(0.3f),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = false, onClick = {},
                        label = { Text("Настроение") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(0.8f)
                        )
                    )
                }
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, null, tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                }
            }
        }

        item {
            SectionHeader("Рекомендуем")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recommended) { album ->
                    AlbumCardView(album = album, onClick = { onPlayAlbum(album) })
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            SectionHeader("Новые релизы", showMore = true)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(newReleases) { album ->
                    AlbumCardView(album = album, onClick = { onPlayAlbum(album) })
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            SectionHeader("Популярные исполнители", showMore = true)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(artists) { (name, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            SectionHeader("Чарты")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recommended + newReleases) { album ->
                    AlbumCardView(album = album, onClick = { onPlayAlbum(album) })
                }
            }
        }
    }
}

@Composable
fun MixScreen() {
    Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Микс", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Персональные подборки скоро появятся", color = Color.White.copy(0.6f))
        }
    }
}

@Composable
fun LibraryScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = 160.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Библиотека", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp))
        }
        items(libraryItems) { (title, icon) ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = Color.White, fontSize = 17.sp)
            }
        }
    }
}


@Composable
fun SearchScreen(playerViewModel: PlayerViewModel) {
    var query by remember { mutableStateOf("") }
    val results by playerViewModel.searchResults.collectAsState()
    val isSearching by playerViewModel.isSearching.collectAsState()
    val status by playerViewModel.statusMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 160.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Треки, альбомы, исполнители...", color = Color.White.copy(0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.7f)) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { playerViewModel.search(query) }) {
                        Icon(Icons.Default.PlayArrow, "Искать", tint = Color(0xFFBB86FC))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(0.1f),
                unfocusedContainerColor = Color.White.copy(0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFBB86FC)
            ),
            singleLine = true
        )

        if (isSearching) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color(0xFFBB86FC)
            )
        }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color.White.copy(0.7f), fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))

        if (results.isEmpty() && !isSearching) {
            Text("Популярные запросы", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            listOf("The Weeknd", "Phonk", "Lo-fi", "Billie Eilish", "Hardtekk").forEach { q ->
                Text(
                    q,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            query = q
                            playerViewModel.search(q)
                        }
                        .padding(vertical = 12.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.videoId }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { playerViewModel.playYtmTrack(track) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(0.08f))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                color = Color.White.copy(0.6f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFBB86FC))
                    }
                }
            }
        }
    }
}


@Composable
fun SectionHeader(title: String, showMore: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (showMore) Text("Ещё", color = Color.White.copy(0.6f), fontSize = 14.sp)
    }
}

@Composable
fun AlbumCardView(album: AlbumCard, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(album.color)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.subtitle, color = Color.White.copy(0.55f), fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MiniPlayer(
    title: String, artist: String, isPlaying: Boolean,
    onPlayPause: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth().height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E24).copy(alpha = 0.95f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFBB86FC).copy(0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color.White.copy(0.6f), fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.FavoriteBorder, null, tint = Color.White.copy(0.8f))
        }
        IconButton(onClick = onPlayPause) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
        }
    }
}

@Composable
fun GlassBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Icons.Default.Home to "Главная",
        Icons.Default.AutoAwesome to "Микс",
        Icons.Default.LibraryMusic to "Библиотека",
        Icons.Default.Search to "Поиск"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, (icon, label) ->
            val selected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable { onSelect(index) }.padding(vertical = 6.dp)
            ) {
                Icon(icon, label, tint = if (selected) Color(0xFFBB86FC) else Color.White.copy(0.55f), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, color = if (selected) Color(0xFFBB86FC) else Color.White.copy(0.55f), fontSize = 10.sp)
            }
        }
    }
}
