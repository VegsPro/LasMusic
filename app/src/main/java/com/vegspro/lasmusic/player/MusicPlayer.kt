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
 * LasMusic Player — fixes the two main pain points of many YT Music clients:
 *
 * 1. Progressive / streaming loading:
 *    - Uses Media3 ExoPlayer with tuned DefaultLoadControl
 *    - Starts playback as soon as enough buffer is available (does NOT wait for full download)
 *    - Prepares the next track in the background while current is playing
 *
 * 2. Infinite continuous playlist:
 *    - When remaining items in queue drop below threshold, automatically
 *      appends similar / related tracks so playback never stops.
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

    // Tuned load control for smooth progressive streaming
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 15_000,          // keep at least 15s
            /* maxBufferMs = */ 50_000,          // up to 50s
            /* bufferForPlaybackMs = */ 1_500,   // start after 1.5s
            /* bufferForPlaybackAfterRebufferMs = */ 3_000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(15_000)
        .setUserAgent("LasMusic/1.0")

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
                    // When we move to a new item, check if we need more recommendations
                    maybeRequestMoreTracks()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Safety net: if somehow we reach real end, request more
                        maybeRequestMoreTracks(force = true)
                    }
                }
            })
        }

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        _queue.value = items
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        // Immediately start preparing next items for seamless transition
        player.seekToNextMediaItem() // internal prepare
        player.seekToPreviousMediaItem()
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
        player.seekToPreviousMediaItem()
    }

    fun setInfiniteMode(enabled: Boolean) {
        infiniteMode = enabled
        if (enabled) maybeRequestMoreTracks()
    }

    /**
     * Core of the "never stop" feature.
     * When remaining tracks are low, ask the recommender for similar tracks
     * and append them to the live queue. Playback continues without interruption.
     */
    private fun maybeRequestMoreTracks(force: Boolean = false) {
        if (!infiniteMode) return

        val remaining = player.mediaItemCount - player.currentMediaItemIndex - 1
        if (!force && remaining > 2) return

        recommendationJob?.cancel()
        recommendationJob = scope.launch(Dispatchers.IO) {
            // Avoid spamming
            delay(300)
            val current = player.currentMediaItem ?: return@launch
            val similar = SimilarTrackProvider.getSimilar(current, limit = 8)
            if (similar.isNotEmpty()) {
                // Switch to main thread for player mutation
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
