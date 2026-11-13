package com.vegspro.lasmusic.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves playable stream URLs via public Piped API instances
 * (they handle YouTube cipher server-side).
 */
object StreamResolver {
    private const val TAG = "StreamResolver"

    private val instances = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.leptons.xyz",
        "https://api-piped.mha.fi"
    )

    /**
     * Returns a direct media URL for ExoPlayer, or null.
     */
    suspend fun resolve(videoId: String): String? = withContext(Dispatchers.IO) {
        for (base in instances) {
            try {
                val url = resolveFrom(base, videoId)
                if (url != null) {
                    Log.i(TAG, "Resolved via $base")
                    return@withContext url
                }
            } catch (e: Exception) {
                Log.w(TAG, "Instance $base failed: ${e.message}")
            }
        }
        null
    }

    private fun resolveFrom(base: String, videoId: String): String? {
        val api = "$base/streams/$videoId"
        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LasMusic/1.3")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10000
            readTimeout = 12000
        }
        if (conn.responseCode !in 200..299) {
            Log.w(TAG, "$base HTTP ${conn.responseCode}")
            return null
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(text)

        // Prefer audio streams
        val audio = json.optJSONArray("audioStreams")
        if (audio != null && audio.length() > 0) {
            var bestUrl: String? = null
            var bestBr = -1
            for (i in 0 until audio.length()) {
                val s = audio.optJSONObject(i) ?: continue
                val u = s.optString("url").takeIf { it.startsWith("http") } ?: continue
                val br = s.optInt("bitrate", 0)
                if (br >= bestBr) {
                    bestBr = br
                    bestUrl = u
                }
            }
            if (bestUrl != null) return bestUrl
        }

        // Fallback: video streams (ExoPlayer will play audio)
        val video = json.optJSONArray("videoStreams")
        if (video != null && video.length() > 0) {
            // Prefer progressive / lower quality for faster start
            var bestUrl: String? = null
            var bestScore = Int.MAX_VALUE
            for (i in 0 until video.length()) {
                val s = video.optJSONObject(i) ?: continue
                if (s.optBoolean("videoOnly", false)) continue
                val u = s.optString("url").takeIf { it.startsWith("http") } ?: continue
                val height = s.optInt("height", 720)
                val score = height // lower is better for audio-focused playback
                if (score < bestScore) {
                    bestScore = score
                    bestUrl = u
                }
            }
            if (bestUrl != null) return bestUrl
            // last resort any video url
            for (i in 0 until video.length()) {
                val u = video.optJSONObject(i)?.optString("url")
                if (!u.isNullOrBlank() && u.startsWith("http")) return u
            }
        }

        // HLS
        val hls = json.optString("hls").takeIf { it.startsWith("http") }
        if (hls != null) return hls

        return null
    }
}
