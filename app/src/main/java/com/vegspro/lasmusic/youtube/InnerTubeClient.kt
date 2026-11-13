package com.vegspro.lasmusic.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.GZIPInputStream

/**
 * Lightweight InnerTube client for YouTube Music.
 * Implements anonymous search + basic player endpoint.
 * Login / personalized features can be layered on top later.
 */
class InnerTubeClient {

    private val tag = "InnerTubeClient"
    private val base = "https://music.youtube.com/youtubei/v1"
    private val apiKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30" // WEB client key (public)

    @Volatile
    private var visitorData: String? = null

    private fun defaultContext(): JSONObject {
        return JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", "WEB_REMIX")
                put("clientVersion", "1.20241030.01.00")
                put("hl", "ru")
                put("gl", "RU")
                visitorData?.let { put("visitorData", it) }
            })
            put("user", JSONObject().apply {
                put("lockedSafetyMode", false)
            })
        }
    }

    private fun post(endpoint: String, body: JSONObject): JSONObject? {
        return try {
            val url = URL("$base/$endpoint?prettyPrint=false&key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("X-Goog-Visitor-Id", visitorData ?: "")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) {
                if (conn.contentEncoding == "gzip") GZIPInputStream(conn.inputStream) else conn.inputStream
            } else {
                if (conn.contentEncoding == "gzip") GZIPInputStream(conn.errorStream) else conn.errorStream
            }
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                Log.w(tag, "HTTP $code for $endpoint: ${text.take(300)}")
                return null
            }
            JSONObject(text)
        } catch (e: Exception) {
            Log.e(tag, "POST $endpoint failed: ${e.message}", e)
            null
        }
    }

    suspend fun ensureVisitorData() = withContext(Dispatchers.IO) {
        if (visitorData != null) return@withContext
        val body = JSONObject().put("context", defaultContext())
        val resp = post("visitor_id", body) // may not exist; fallback
        // Alternative: get from browse
        val browseBody = JSONObject().apply {
            put("context", defaultContext())
            put("browseId", "FEmusic_home")
        }
        val browse = post("browse", browseBody)
        // Extract visitorData from responseContext if present
        browse?.optJSONObject("responseContext")?.optString("visitorData")?.takeIf { it.isNotBlank() }?.let {
            visitorData = it
            Log.i(tag, "Got visitorData")
        }
        if (visitorData == null) {
            visitorData = "Cgt" + UUID.randomUUID().toString().replace("-", "").take(20)
        }
    }

    data class SearchResult(
        val videoId: String,
        val title: String,
        val artist: String,
        val thumbnail: String?
    )

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        ensureVisitorData()
        val body = JSONObject().apply {
            put("context", defaultContext())
            put("query", query)
            put("params", "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D") // songs filter
        }
        val resp = post("search", body) ?: return@withContext emptyList()
        parseSearchResults(resp)
    }

    private fun parseSearchResults(json: JSONObject): List<SearchResult> {
        val out = mutableListOf<SearchResult>()
        try {
            val contents = json
                .optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
                ?: return emptyList()

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i) ?: continue
                val musicShelf = section.optJSONObject("musicShelfRenderer") ?: continue
                val items = musicShelf.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val videoId = item
                        .optJSONObject("playlistItemData")
                        ?.optString("videoId")
                        ?.takeIf { it.isNotBlank() }
                        ?: item.optJSONObject("overlay")
                            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("musicPlayButtonRenderer")
                            ?.optJSONObject("playNavigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                    if (videoId.isNullOrBlank()) continue

                    val flex = item.optJSONArray("flexColumns")
                    val title = flex?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "Unknown"

                    val artist = flex?.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""

                    val thumb = item.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        ?.let { arr ->
                            arr.optJSONObject(arr.length() - 1)?.optString("url")
                        }

                    out.add(SearchResult(videoId, title, artist, thumb))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Parse search failed", e)
        }
        return out
    }

    /**
     * Get player response. Stream URLs may be ciphered — for full playback
     * a signature decipher step is still required (InnerTubeX extractor / yt-dlp style).
     * Returns adaptive formats when available (some are direct).
     */
    suspend fun getPlayer(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        ensureVisitorData()
        val body = JSONObject().apply {
            put("context", defaultContext())
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        post("player", body)
    }

    /**
     * Try to pick a direct (non-ciphered) audio URL from player response.
     */
    suspend fun getDirectAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val player = getPlayer(videoId) ?: return@withContext null
        val formats = player.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
            ?: player.optJSONObject("streamingData")?.optJSONArray("formats")
            ?: return@withContext null

        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val mime = f.optString("mimeType")
            if (!mime.startsWith("audio/")) continue
            val url = f.optString("url").takeIf { it.isNotBlank() } ?: continue // skip ciphered
            val br = f.optInt("bitrate", 0)
            if (br > bestBitrate) {
                bestBitrate = br
                bestUrl = url
            }
        }
        bestUrl
    }
}
