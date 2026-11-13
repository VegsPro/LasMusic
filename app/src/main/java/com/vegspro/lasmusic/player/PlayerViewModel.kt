package com.vegspro.lasmusic.player

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.vegspro.lasmusic.youtube.YouTubeMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "PlayerVM"
    private val app = application

    private var musicPlayer: MusicPlayer? = null
    private val ytm by lazy { YouTubeMusicRepository(app) }
    private val prefs by lazy { app.getSharedPreferences("lasmusic_likes", Context.MODE_PRIVATE) }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentItem = MutableStateFlow<MediaItem?>(null)
    val currentItem: StateFlow<MediaItem?> = _currentItem.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeMusicRepository.Track>>(emptyList())
    val searchResults: StateFlow<List<YouTubeMusicRepository.Track>> = _searchResults.asStateFlow()

    private val _homeTracks = MutableStateFlow<List<YouTubeMusicRepository.Track>>(emptyList())
    val homeTracks: StateFlow<List<YouTubeMusicRepository.Track>> = _homeTracks.asStateFlow()

    private val _chartTracks = MutableStateFlow<List<YouTubeMusicRepository.Track>>(emptyList())
    val chartTracks: StateFlow<List<YouTubeMusicRepository.Track>> = _chartTracks.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<YouTubeMusicRepository.Track>>(emptyList())
    val likedTracks: StateFlow<List<YouTubeMusicRepository.Track>> = _likedTracks.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        Log.i(tag, "ViewModel init")
        try {
            loadLikesFromDisk()
        } catch (e: Exception) {
            Log.e(tag, "loadLikes", e)
        }
        // Defer network + player to after first frame
        viewModelScope.launch {
            try {
                ensurePlayer()
            } catch (e: Exception) {
                Log.e(tag, "player init", e)
            }
            try {
                loadHome()
            } catch (e: Exception) {
                Log.e(tag, "loadHome", e)
            }
        }
    }

    private fun ensurePlayer(): MusicPlayer {
        val existing = musicPlayer
        if (existing != null) return existing
        synchronized(this) {
            val again = musicPlayer
            if (again != null) return again
            Log.i(tag, "Creating MusicPlayer")
            val p = MusicPlayer(app, viewModelScope)
            // Mirror state into VM flows
            viewModelScope.launch {
                p.isPlaying.collect { _isPlaying.value = it }
            }
            viewModelScope.launch {
                p.currentItem.collect { _currentItem.value = it }
            }
            viewModelScope.launch {
                p.queue.collect { _queue.value = it }
            }
            p.setInfiniteMode(true)
            musicPlayer = p
            return p
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                _homeTracks.value = ytm.getHome()
                _chartTracks.value = ytm.getCharts()
            } catch (e: Exception) {
                Log.e(tag, "loadHome", e)
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _statusMessage.value = "Поиск…"
            try {
                val results = ytm.search(query)
                _searchResults.value = results
                _statusMessage.value = if (results.isEmpty()) "Ничего не найдено" else "Найдено: ${results.size}"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка поиска"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playYtmTrack(track: YouTubeMusicRepository.Track) {
        viewModelScope.launch {
            _statusMessage.value = "Загрузка потока…"
            try {
                val item = ytm.resolvePlayable(track)
                if (item != null) {
                    val p = ensurePlayer()
                    p.setQueue(listOf(item), 0)
                    p.play()
                    _statusMessage.value = null
                } else {
                    _statusMessage.value = "Поток недоступен. Попробуй другой трек."
                }
            } catch (e: Exception) {
                Log.e(tag, "playYtmTrack", e)
                _statusMessage.value = "Ошибка воспроизведения"
            }
        }
    }

    fun toggleLike(track: YouTubeMusicRepository.Track) {
        val current = _likedTracks.value.toMutableList()
        val idx = current.indexOfFirst { it.videoId == track.videoId }
        if (idx >= 0) current.removeAt(idx) else current.add(0, track)
        _likedTracks.value = current
        saveLikesToDisk(current)
    }

    fun isLiked(videoId: String): Boolean = _likedTracks.value.any { it.videoId == videoId }

    private fun loadLikesFromDisk() {
        val raw = prefs.getString("likes_json", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<YouTubeMusicRepository.Track>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                YouTubeMusicRepository.Track(
                    videoId = o.getString("videoId"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    thumbnailUrl = o.optString("thumb").takeIf { it.isNotBlank() }
                )
            )
        }
        _likedTracks.value = list
    }

    private fun saveLikesToDisk(list: List<YouTubeMusicRepository.Track>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("videoId", t.videoId)
                put("title", t.title)
                put("artist", t.artist)
                put("thumb", t.thumbnailUrl ?: "")
            })
        }
        prefs.edit().putString("likes_json", arr.toString()).apply()
    }

    fun togglePlayPause() = try { ensurePlayer().togglePlayPause() } catch (e: Exception) { Log.e(tag, "toggle", e) }
    fun next() = try { ensurePlayer().seekToNext() } catch (e: Exception) { Log.e(tag, "next", e) }
    fun previous() = try { ensurePlayer().seekToPrevious() } catch (e: Exception) { Log.e(tag, "prev", e) }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        try {
            val p = ensurePlayer()
            p.setQueue(items, startIndex)
            p.play()
        } catch (e: Exception) {
            Log.e(tag, "playQueue", e)
        }
    }

    fun playTrack(item: MediaItem) {
        val related = try { SimilarTrackProvider.getSimilar(item, limit = 6) } catch (_: Exception) { emptyList() }
        playQueue(listOf(item) + related)
    }

    override fun onCleared() {
        try { musicPlayer?.release() } catch (_: Exception) {}
        try { ytm.close() } catch (_: Exception) {}
        super.onCleared()
    }
}
