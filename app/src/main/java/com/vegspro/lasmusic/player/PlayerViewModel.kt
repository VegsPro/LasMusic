package com.vegspro.lasmusic.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val musicPlayer = MusicPlayer(application, viewModelScope)

    val isPlaying = musicPlayer.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentItem = musicPlayer.currentItem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val queue = musicPlayer.queue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val initial = SimilarTrackProvider.getInitialQueue()
        musicPlayer.setQueue(initial)
        musicPlayer.setInfiniteMode(true)
        // Don't auto-play on start — let user press play
    }

    fun togglePlayPause() = musicPlayer.togglePlayPause()
    fun next() = musicPlayer.seekToNext()
    fun previous() = musicPlayer.seekToPrevious()
    fun setInfiniteMode(enabled: Boolean) = musicPlayer.setInfiniteMode(enabled)

    /** Start a new queue from the given items and begin playback */
    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        musicPlayer.setQueue(items, startIndex)
        musicPlayer.play()
    }

    /** Play a single track + related tracks after it */
    fun playTrack(item: MediaItem) {
        val related = SimilarTrackProvider.getSimilar(item, limit = 6)
        playQueue(listOf(item) + related)
    }

    override fun onCleared() {
        musicPlayer.release()
        super.onCleared()
    }
}
