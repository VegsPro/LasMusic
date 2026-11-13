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

    @Volatile private var musicPlayer: MusicPlayer? = null
    private val ytm by lazy { YouTubeMusicRepository(app) }
    private val prefs by lazy { app.getSharedPreferences("lasmusic_likes", Context.MODE_PRIVATE) }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentItem = MutableStateFlow<MediaItem?>(null)
    val currentItem: StateFlow<MediaItem?> = _currentItem.asStateFlow()

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
        Log.i(tag, "init")
        try { loadLikesFromDisk() } catch (e: Exception) { Log.e(tag, "likes", e) }
        viewModelScope.launch {
            try { loadHome() } catch (e: Exception) { Log.e(tag, "home", e) }
        }
    }

    private fun ensurePlayer(): MusicPlayer {
        musicPlayer?.let { return it }
        synchronized(this) {
            musicPlayer?.let { return it }
            Log.i(tag, "create player")
            val p = MusicPlayer(app, viewModelScope)
            viewModelScope.launch { p.isPlaying.collect { _isPlaying.value = it } }
            viewModelScope.launch { p.currentItem.collect { _currentItem.value = it } }
            musicPlayer = p
            return p
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            _statusMessage.value = "Загрузка…"
            try {
                val h = ytm.getHome()
                val c = ytm.getCharts()
                _homeTracks.value = h
                _chartTracks.value = c
                _statusMessage.value = if (h.isEmpty() && c.isEmpty()) "Нет данных (сеть?)" else null
            } catch (e: Exception) {
                Log.e(tag, "loadHome", e)
                _statusMessage.value = "Ошибка сети"
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
                val r = ytm.search(query)
                _searchResults.value = r
                _statusMessage.value = if (r.isEmpty()) "Ничего не найдено" else "Найдено: ${r.size}"
            } catch (e: Exception) {
                Log.e(tag, "search", e)
                _statusMessage.value = "Ошибка поиска"
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
                    // auto-like optional skip
                    _statusMessage.value = null
                } else {
                    _statusMessage.value = "Поток недоступен"
                }
            } catch (e: Exception) {
                Log.e(tag, "play", e)
                _statusMessage.value = "Ошибка: ${e.message?.take(40)}"
            }
        }
    }

    fun toggleLike(track: YouTubeMusicRepository.Track) {
        val list = _likedTracks.value.toMutableList()
        val idx = list.indexOfFirst { it.videoId == track.videoId }
        if (idx >= 0) list.removeAt(idx) else list.add(0, track)
        _likedTracks.value = list
        saveLikesToDisk(list)
    }

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

    override fun onCleared() {
        try { musicPlayer?.release() } catch (_: Exception) {}
        super.onCleared()
    }
}
