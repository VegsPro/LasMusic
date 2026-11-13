package com.vegspro.lasmusic.player

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.vegspro.lasmusic.youtube.YouTubeMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val musicPlayer = MusicPlayer(application, viewModelScope)
    private val ytm = YouTubeMusicRepository(application)
    private val prefs = application.getSharedPreferences("lasmusic_likes", Context.MODE_PRIVATE)

    val isPlaying = musicPlayer.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val currentItem = musicPlayer.currentItem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val queue = musicPlayer.queue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        musicPlayer.setInfiniteMode(true)
        loadLikesFromDisk()
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                val home = ytm.getHome()
                val charts = ytm.getCharts()
                _homeTracks.value = home
                _chartTracks.value = charts
            } catch (e: Exception) {
                Log.e("PlayerVM", "loadHome", e)
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
                    musicPlayer.setQueue(listOf(item), 0)
                    musicPlayer.play()
                    _statusMessage.value = null
                } else {
                    _statusMessage.value = "Поток недоступен. Попробуй другой трек."
                }
            } catch (e: Exception) {
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

    fun isLiked(videoId: String): Boolean =
        _likedTracks.value.any { it.videoId == videoId }

    private fun loadLikesFromDisk() {
        try {
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
        } catch (_: Exception) {}
    }

    private fun saveLikesToDisk(list: List<YouTubeMusicRepository.Track>) {
        try {
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
        } catch (_: Exception) {}
    }

    fun togglePlayPause() = try { musicPlayer.togglePlayPause() } catch (_: Exception) {}
    fun next() = try { musicPlayer.seekToNext() } catch (_: Exception) {}
    fun previous() = try { musicPlayer.seekToPrevious() } catch (_: Exception) {}

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        try {
            musicPlayer.setQueue(items, startIndex)
            musicPlayer.play()
        } catch (_: Exception) {}
    }

    fun playTrack(item: MediaItem) {
        val related = try { SimilarTrackProvider.getSimilar(item, limit = 6) } catch (_: Exception) { emptyList() }
        playQueue(listOf(item) + related)
    }

    override fun onCleared() {
        try { musicPlayer.release() } catch (_: Exception) {}
        try { ytm.close() } catch (_: Exception) {}
        super.onCleared()
    }
}
