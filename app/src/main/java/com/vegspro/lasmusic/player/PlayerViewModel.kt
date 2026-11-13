package com.vegspro.lasmusic.player

import android.app.Application
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

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val musicPlayer: MusicPlayer = try {
        MusicPlayer(application, viewModelScope)
    } catch (e: Exception) {
        Log.e("PlayerVM", "MusicPlayer init failed", e)
        throw e
    }

    private val ytm = YouTubeMusicRepository(application)

    val isPlaying = musicPlayer.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentItem = musicPlayer.currentItem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val queue = musicPlayer.queue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<YouTubeMusicRepository.Track>>(emptyList())
    val searchResults: StateFlow<List<YouTubeMusicRepository.Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        try {
            musicPlayer.setInfiniteMode(true)
        } catch (e: Exception) {
            Log.e("PlayerVM", "init infinite mode", e)
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
                Log.e("PlayerVM", "Search failed", e)
                _statusMessage.value = "Ошибка поиска: ${e.message?.take(40)}"
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
                    _statusMessage.value = "Поток недоступен (cipher). Попробуй другой трек."
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "playYtmTrack", e)
                _statusMessage.value = "Ошибка: ${e.message?.take(40)}"
            }
        }
    }

    fun togglePlayPause() = try { musicPlayer.togglePlayPause() } catch (_: Exception) {}
    fun next() = try { musicPlayer.seekToNext() } catch (_: Exception) {}
    fun previous() = try { musicPlayer.seekToPrevious() } catch (_: Exception) {}
    fun setInfiniteMode(enabled: Boolean) = try { musicPlayer.setInfiniteMode(enabled) } catch (_: Exception) {}

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        try {
            musicPlayer.setQueue(items, startIndex)
            musicPlayer.play()
        } catch (e: Exception) {
            Log.e("PlayerVM", "playQueue", e)
        }
    }

    fun playTrack(item: MediaItem) {
        val related = try {
            SimilarTrackProvider.getSimilar(item, limit = 6)
        } catch (_: Exception) {
            emptyList()
        }
        playQueue(listOf(item) + related)
    }

    override fun onCleared() {
        try { musicPlayer.release() } catch (_: Exception) {}
        try { ytm.close() } catch (_: Exception) {}
        super.onCleared()
    }
}
