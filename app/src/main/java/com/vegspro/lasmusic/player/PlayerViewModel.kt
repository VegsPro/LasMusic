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
        // Start with a demo queue so the user can immediately experience
        // progressive loading + infinite mode
        val initial = SimilarTrackProvider.getInitialQueue()
        musicPlayer.setQueue(initial)
        musicPlayer.setInfiniteMode(true)
        musicPlayer.play()
    }

    fun togglePlayPause() = musicPlayer.togglePlayPause()
    fun next() = musicPlayer.seekToNext()
    fun previous() = musicPlayer.seekToPrevious()
    fun setInfiniteMode(enabled: Boolean) = musicPlayer.setInfiniteMode(enabled)

    override fun onCleared() {
        musicPlayer.release()
        super.onCleared()
    }
}
