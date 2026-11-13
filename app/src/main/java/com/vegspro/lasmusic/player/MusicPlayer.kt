package com.vegspro.lasmusic.player

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayer(
    context: Context,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
) {
    private val tag = "MusicPlayer"

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentItem = MutableStateFlow<MediaItem?>(null)
    val currentItem: StateFlow<MediaItem?> = _currentItem.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val httpFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(12_000)
        .setReadTimeoutMs(20_000)
        .setUserAgent("LasMusic/1.4")

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(10_000, 40_000, 1_000, 2_500)
        .build()

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
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(tag, "Player error: ${error.message}")
                }
            })
        }

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        try {
            player.stop()
            player.clearMediaItems()
            player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            player.prepare()
            _queue.value = items
            _currentItem.value = items.getOrNull(startIndex.coerceIn(0, items.lastIndex))
        } catch (e: Exception) {
            Log.e(tag, "setQueue", e)
        }
    }

    fun play() {
        try { player.play() } catch (e: Exception) { Log.e(tag, "play", e) }
    }

    fun pause() {
        try { player.pause() } catch (e: Exception) { Log.e(tag, "pause", e) }
    }

    fun togglePlayPause() {
        try {
            if (player.isPlaying) player.pause() else player.play()
        } catch (e: Exception) {
            Log.e(tag, "toggle", e)
        }
    }

    fun seekToNext() {
        try {
            if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        } catch (e: Exception) {
            Log.e(tag, "next", e)
        }
    }

    fun seekToPrevious() {
        try {
            if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
        } catch (e: Exception) {
            Log.e(tag, "prev", e)
        }
    }

    fun setInfiniteMode(@Suppress("UNUSED_PARAMETER") enabled: Boolean) {}

    fun release() {
        try { player.release() } catch (_: Exception) {}
    }
}
