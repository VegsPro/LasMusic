package com.vegspro.lasmusic.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Provides "similar" tracks for the infinite playlist feature.
 *
 * Current implementation is a smart local/mock provider so the app works offline
 * and demonstrates the continuous playback behaviour immediately.
 *
 * Later this can be replaced / extended with:
 * - YouTube Music "related" / radio endpoint (via extractor)
 * - Last.fm similar artists
 * - Local audio fingerprint / metadata similarity
 */
object SimilarTrackProvider {

    // Demo progressive streams (public domain / creative commons friendly samples)
    // In a real app these would come from YT Music or other sources.
    private val demoPool = listOf(
        createItem(
            id = "demo1",
            title = "Night City Drive",
            artist = "Synthwave Dreams",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            art = null
        ),
        createItem(
            id = "demo2",
            title = "Ocean Waves",
            artist = "Ambient Collective",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            art = null
        ),
        createItem(
            id = "demo3",
            title = "Midnight Jazz",
            artist = "Cool Cats Trio",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            art = null
        ),
        createItem(
            id = "demo4",
            title = "Forest Rain",
            artist = "Nature Sounds",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            art = null
        ),
        createItem(
            id = "demo5",
            title = "Electric Pulse",
            artist = "Neon Pulse",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            art = null
        ),
        createItem(
            id = "demo6",
            title = "Sunset Boulevard",
            artist = "Retro Wave",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            art = null
        ),
        createItem(
            id = "demo7",
            title = "Deep Focus",
            artist = "Study Beats",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            art = null
        ),
        createItem(
            id = "demo8",
            title = "Cosmic Journey",
            artist = "Space Ambient",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            art = null
        )
    )

    fun getSimilar(current: MediaItem, limit: Int = 6): List<MediaItem> {
        // Simple rotation + shuffle so it never ends and feels continuous
        val shuffled = demoPool.shuffled()
        return shuffled.take(limit).mapIndexed { index, item ->
            // Give unique IDs so Media3 treats them as new items
            item.buildUpon()
                .setMediaId("${item.mediaId}_${System.currentTimeMillis()}_$index")
                .build()
        }
    }

    fun getInitialQueue(): List<MediaItem> = demoPool.shuffled().take(5)

    private fun createItem(
        id: String,
        title: String,
        artist: String,
        uri: String,
        art: String?
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(art?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
}
