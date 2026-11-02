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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vegspro.lasmusic.player.PlayerViewModel
import com.vegspro.lasmusic.player.SimilarTrackProvider

data class AlbumCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String
)

data class ArtistCard(
    val id: String,
    val name: String,
    val imageUrl: String
)

private val recommended = listOf(
    AlbumCard("1", "ДИНАСТИЯ", "VILLIAN, madk1d • Красивое 3", "https://picsum.photos/seed/dynasty/300"),
    AlbumCard("2", "DEMON'S SHOT", "Aiobahn +81, DEMONDICE • DE", "https://picsum.photos/seed/demon/300"),
    AlbumCard("3", "Night Drive", "Synthwave Dreams", "https://picsum.photos/seed/night/300"),
    AlbumCard("4", "Neon Dreams", "Retro Wave", "https://picsum.photos/seed/neon/300")
)

private val newReleases = listOf(
    AlbumCard("5", "да пошло всё", "dabbackwood, volhey", "https://picsum.photos/seed/snake/300"),
    AlbumCard("6", "SHAPE OF YOU (HARDTEKK)", "ANDONIS, prodByLone", "https://picsum.photos/seed/pink/300"),
    AlbumCard("7", "Midnight Jazz", "Cool Cats Trio", "https://picsum.photos/seed/jazz/300"),
    AlbumCard("8", "Ocean Waves", "Ambient Collective", "https://picsum.photos/seed/ocean/300")
)

private val artists = listOf(
    ArtistCard("a1", "The Weeknd", "https://picsum.photos/seed/weeknd/200"),
    ArtistCard("a2", "Playboi Carti", "https://picsum.photos/seed/carti/200"),
    ArtistCard("a3", "Travis Scott", "https://picsum.photos/seed/travis/200"),
    ArtistCard("a4", "Billie Eilish", "https://picsum.photos/seed/billie/200")
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
    ) {
        when (selectedTab) {
            0 -> HomeScreen(onPlayAlbum = { playerViewModel.togglePlayPause() })
            1 -> MixScreen()
            2 -> LibraryScreen()
            3 -> SearchScreen()
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
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun HomeScreen(onPlayAlbum: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 160.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Все") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Настроение") })
                }
                Row {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                    }
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
                    AlbumCardView(album = album, onClick = onPlayAlbum)
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
                    AlbumCardView(album = album, onClick = onPlayAlbum)
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
                items(artists) { artist ->
                    ArtistCardView(artist)
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
                items(recommended.shuffled()) { album ->
                    AlbumCardView(album = album, onClick = onPlayAlbum)
                }
            }
        }
    }
}

@Composable
fun MixScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Микс", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Персональные подборки скоро появятся", color = Color.White.copy(0.6f))
        }
    }
}

@Composable
fun LibraryScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 160.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                "Библиотека",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(libraryItems) { (title, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = Color.White, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun SearchScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 160.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Треки, альбомы, исполнители...", color = Color.White.copy(0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.7f)) },
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
        Spacer(Modifier.height(32.dp))
        Text("Популярные запросы", color = Color.White.copy(0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        listOf("The Weeknd", "Hardtekk", "Phonk", "Lo-fi", "Synthwave").forEach { query ->
            Text(
                query,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, showMore: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (showMore) {
            Text("Ещё", color = Color.White.copy(0.6f), fontSize = 14.sp)
        }
    }
}

@Composable
fun AlbumCardView(album: AlbumCard, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = album.imageUrl,
            contentDescription = album.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(0.08f))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            album.subtitle,
            color = Color.White.copy(0.55f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCardView(artist: ArtistCard) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E24).copy(alpha = 0.95f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
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
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun GlassBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color(0xFFBB86FC) else Color.White.copy(0.55f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = if (selected) Color(0xFFBB86FC) else Color.White.copy(0.55f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
