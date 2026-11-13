package com.vegspro.lasmusic.youtube

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeMusicRepository(private val context: Context) {

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
            Log.e(tag, "Search error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Resolve stream URL and build MediaItem ready for ExoPlayer.
     * Prefers direct (non-ciphered) audio formats.
     */
    suspend fun resolvePlayable(track: Track): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val url = client.getDirectAudioUrl(track.videoId)
            if (url != null) {
                Log.i(tag, "Got direct stream for ${track.videoId}")
                track.toMediaItem(url)
            } else {
                Log.w(tag, "No direct stream for ${track.videoId} (ciphered?)")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Resolve failed", e)
            null
        }
    }

    suspend fun resolvePlayable(videoId: String, title: String, artist: String, thumb: String?): MediaItem? {
        return resolvePlayable(Track(videoId, title, artist, thumb))
    }

    fun close() {
        // nothing to close for HttpURLConnection client
    }
}
