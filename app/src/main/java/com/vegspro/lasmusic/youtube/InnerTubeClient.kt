package com.vegspro.lasmusic.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.GZIPInputStream

/**
 * Lightweight InnerTube client for YouTube Music.
 * Tries WEB_REMIX and ANDROID clients for better stream availability.
 */
class InnerTubeClient {

    private val tag = "InnerTubeClient"
    private val base = "https://music.youtube.com/youtubei/v1"
    // Public client keys used by official clients
    private val webKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    private val androidKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI"

    @Volatile
    private var visitorData: String? = null

    private fun webContext(): JSONObject = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", "1.20241030.01.00")
            put("hl", "ru")
            put("gl", "RU")
            visitorData?.let { put("visitorData", it) }
        })
        put("user", JSONObject().put("lockedSafetyMode", false))
    }

    private fun androidContext(): JSONObject = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "ANDROID_MUSIC")
            put("clientVersion", "7.27.52")
            put("androidSdkVersion", 34)
            put("hl", "ru")
            put("gl", "RU")
            visitorData?.let { put("visitorData", it) }
        })
        put("user", JSONObject().put("lockedSafetyMode", false))
    }

    private fun post(endpoint: String, body: JSONObject, key: String, origin: String): JSONObject? {
        return try {
            val url = URL("$base/$endpoint?prettyPrint=false&key=$key")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent",
                    if (key == androidKey)
                        "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; rv:34) gzip"
                    else
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", origin)
                setRequestProperty("Referer", "$origin/")
                setRequestProperty("Accept-Encoding", "gzip")
                visitorData?.let { setRequestProperty("X-Goog-Visitor-Id", it) }
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = if (code in 200..299) conn.inputStream else conn.errorStream
            val stream = if (conn.contentEncoding == "gzip") GZIPInputStream(raw) else raw
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                Log.w(tag, "HTTP $code $endpoint: ${text.take(200)}")
                return null
            }
            JSONObject(text)
        } catch (e: Exception) {
            Log.e(tag, "POST $endpoint: ${e.message}")
            null
        }
    }

    suspend fun ensureVisitorData() = withContext(Dispatchers.IO) {
        if (visitorData != null) return@withContext
        val body = JSONObject().apply {
            put("context", webContext())
            put("browseId", "FEmusic_home")
        }
        val browse = post("browse", body, webKey, "https://music.youtube.com")
        browse?.optJSONObject("responseContext")?.optString("visitorData")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                visitorData = it
                Log.i(tag, "visitorData ok")
            }
        if (visitorData == null) {
            visitorData = "Cgt" + UUID.randomUUID().toString().replace("-", "").take(22)
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
            put("context", webContext())
            put("query", query)
            put("params", "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D") // filter: songs
        }
        val resp = post("search", body, webKey, "https://music.youtube.com")
            ?: return@withContext emptyList()
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
                val musicShelf = contents.optJSONObject(i)?.optJSONObject("musicShelfRenderer") ?: continue
                val items = musicShelf.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
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
                        ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }

                    out.add(SearchResult(videoId, title, artist, thumb))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "parse search", e)
        }
        return out
    }

    /**
     * Try ANDROID_MUSIC then WEB for a direct audio URL (no cipher).
     */
    suspend fun getDirectAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureVisitorData()
        // Prefer Android Music client — more often returns plain URLs
        extractUrl(
            post(
                "player",
                JSONObject().apply {
                    put("context", androidContext())
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                },
                androidKey,
                "https://music.youtube.com"
            )
        ) ?: extractUrl(
            post(
                "player",
                JSONObject().apply {
                    put("context", webContext())
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                },
                webKey,
                "https://music.youtube.com"
            )
        )
    }

    private fun extractUrl(player: JSONObject?): String? {
        if (player == null) return null
        val status = player.optJSONObject("playabilityStatus")?.optString("status")
        if (status != null && status != "OK") {
            Log.w(tag, "playability: $status")
            return null
        }
        val streaming = player.optJSONObject("streamingData") ?: return null
        val arrays = listOfNotNull(
            streaming.optJSONArray("adaptiveFormats"),
            streaming.optJSONArray("formats")
        )
        var bestUrl: String? = null
        var bestBr = -1
        for (arr in arrays) {
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                val mime = f.optString("mimeType")
                if (!mime.contains("audio")) continue
                val url = f.optString("url").takeIf { it.isNotBlank() } ?: continue
                val br = f.optInt("bitrate", 0)
                if (br > bestBr) {
                    bestBr = br
                    bestUrl = url
                }
            }
        }
        return bestUrl
    }
}
