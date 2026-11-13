package com.vegspro.lasmusic.youtube

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeMusicRepository(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val tag = "YTMRepo"
    private val client = InnerTubeClient()

    data class Track(
        val videoId: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val durationSeconds: Int? = null
    ) {
        fun toMediaItem(streamUrl: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(videoId)
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setArtworkUri(thumbnailUrl?.let { android.net.Uri.parse(it) })
                        .setExtras(android.os.Bundle().apply {
                            putString("videoId", videoId)
                        })
                        .build()
                )
                .build()
        }
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            client.search(query).map {
                Track(it.videoId, it.title, it.artist, it.thumbnail)
            }
        } catch (e: Exception) {
            Log.e(tag, "Search error", e)
            emptyList()
        }
    }

    suspend fun getHome(): List<Track> = withContext(Dispatchers.IO) {
        try {
            client.getHome().map { Track(it.videoId, it.title, it.artist, it.thumbnail) }
        } catch (e: Exception) {
            Log.e(tag, "Home error", e)
            emptyList()
        }
    }

    suspend fun getCharts(): List<Track> = withContext(Dispatchers.IO) {
        try {
            client.getCharts().map { Track(it.videoId, it.title, it.artist, it.thumbnail) }
        } catch (e: Exception) {
            Log.e(tag, "Charts error", e)
            emptyList()
        }
    }

    suspend fun resolvePlayable(track: Track): MediaItem? = withContext(Dispatchers.IO) {
        try {
            // 1) Piped / public stream APIs (most reliable for playback)
            val piped = StreamResolver.resolve(track.videoId)
            if (piped != null) {
                Log.i(tag, "Stream via Piped for ${track.videoId}")
                return@withContext track.toMediaItem(piped)
            }
            // 2) Direct InnerTube (rare plain URLs)
            val direct = client.getDirectAudioUrl(track.videoId)
            if (direct != null) {
                Log.i(tag, "Stream via InnerTube for ${track.videoId}")
                return@withContext track.toMediaItem(direct)
            }
            Log.w(tag, "No stream for ${track.videoId}")
            null
        } catch (e: Exception) {
            Log.e(tag, "Resolve failed", e)
            null
        }
    }

    fun close() {}
}
