package com.vegspro.lasmusic.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Progressive streaming + infinite playlist player.
 */
class MusicPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentItem = MutableStateFlow<MediaItem?>(null)
    val currentItem: StateFlow<MediaItem?> = _currentItem.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private var infiniteMode = true
    private var recommendationJob: Job? = null

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,  // minBuffer
            50_000,  // maxBuffer
            1_500,   // bufferForPlayback
            3_000    // bufferForPlaybackAfterRebuffer
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(15_000)
        .setUserAgent("LasMusic/1.0.2")

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentItem.value = mediaItem
                    maybeRequestMoreTracks()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        maybeRequestMoreTracks(force = true)
                    }
                }
            })
        }

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        _queue.value = items
        // Important: do NOT seek next/previous — that was causing wrong track to start
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        _currentItem.value = items.getOrNull(startIndex)
    }

    fun addToQueue(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val newQueue = _queue.value + items
        _queue.value = newQueue
        player.addMediaItems(items)
        maybeRequestMoreTracks()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            maybeRequestMoreTracks(force = true)
        }
    }

    fun seekToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }

    fun setInfiniteMode(enabled: Boolean) {
        infiniteMode = enabled
        if (enabled) maybeRequestMoreTracks()
    }

    private fun maybeRequestMoreTracks(force: Boolean = false) {
        if (!infiniteMode) return
        val remaining = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (!force && remaining > 2) return

        recommendationJob?.cancel()
        recommendationJob = scope.launch(Dispatchers.IO) {
            delay(400)
            val current = player.currentMediaItem ?: return@launch
            val similar = SimilarTrackProvider.getSimilar(current, limit = 6)
            if (similar.isNotEmpty()) {
                launch(Dispatchers.Main) {
                    addToQueue(similar)
                }
            }
        }
    }

    fun release() {
        recommendationJob?.cancel()
        player.release()
    }
}
