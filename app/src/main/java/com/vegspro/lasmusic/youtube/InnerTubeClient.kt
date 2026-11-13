package com.vegspro.lasmusic.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import java.util.UUID
import java.util.zip.GZIPInputStream

class InnerTubeClient {

    private val tag = "InnerTubeClient"
    private val base = "https://music.youtube.com/youtubei/v1"
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

    private fun post(endpoint: String, body: JSONObject, key: String): JSONObject? {
        return try {
            val url = URL("$base/$endpoint?prettyPrint=false&key=$key")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "User-Agent",
                    if (key == androidKey)
                        "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14) gzip"
                    else
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                )
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                visitorData?.let { setRequestProperty("X-Goog-Visitor-Id", it) }
                doOutput = true
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val raw = if (code in 200..299) conn.inputStream else conn.errorStream
            if (raw == null) {
                Log.w(tag, "HTTP $code no body for $endpoint")
                return null
            }
            val stream = if (conn.contentEncoding == "gzip") GZIPInputStream(raw) else raw
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                Log.w(tag, "HTTP $code $endpoint: ${text.take(180)}")
                return null
            }
            JSONObject(text)
        } catch (e: Exception) {
            Log.e(tag, "POST $endpoint: ${e.message}", e)
            null
        }
    }

    suspend fun ensureVisitorData() = withContext(Dispatchers.IO) {
        if (visitorData != null) return@withContext
        val body = JSONObject().apply {
            put("context", webContext())
            put("browseId", "FEmusic_home")
        }
        val browse = post("browse", body, webKey)
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
        // Do NOT use songs-only filter — it often returns empty for many queries
        val body = JSONObject().apply {
            put("context", webContext())
            put("query", query)
        }
        val resp = post("search", body, webKey) ?: return@withContext emptyList()
        val results = extractTracks(resp)
        Log.i(tag, "search '$query' -> ${results.size}")
        results
    }

    suspend fun getHome(): List<SearchResult> = withContext(Dispatchers.IO) {
        ensureVisitorData()
        val out = mutableListOf<SearchResult>()
        // New releases usually have direct videoIds
        post(
            "browse",
            JSONObject().apply {
                put("context", webContext())
                put("browseId", "FEmusic_new_releases")
            },
            webKey
        )?.let { out.addAll(extractTracks(it)) }
        if (out.size < 8) {
            post(
                "browse",
                JSONObject().apply {
                    put("context", webContext())
                    put("browseId", "FEmusic_home")
                },
                webKey
            )?.let { out.addAll(extractTracks(it)) }
        }
        Log.i(tag, "home -> ${out.size}")
        out.distinctBy { it.videoId }
    }

    suspend fun getCharts(): List<SearchResult> = withContext(Dispatchers.IO) {
        ensureVisitorData()
        val resp = post(
            "browse",
            JSONObject().apply {
                put("context", webContext())
                put("browseId", "FEmusic_charts")
            },
            webKey
        ) ?: return@withContext emptyList()
        val results = extractTracks(resp)
        Log.i(tag, "charts -> ${results.size}")
        results
    }

    /**
     * Walk entire JSON tree and collect any playable tracks (11-char videoId).
     */
    private fun extractTracks(json: JSONObject): List<SearchResult> {
        val out = mutableListOf<SearchResult>()
        val seen = HashSet<String>()
        val queue: ArrayDeque<Any> = ArrayDeque()
        queue.add(json)
        var steps = 0
        while (queue.isNotEmpty() && steps < 800 && out.size < 50) {
            steps++
            when (val node = queue.removeFirst()) {
                is JSONObject -> {
                    // musicResponsiveListItemRenderer
                    node.optJSONObject("musicResponsiveListItemRenderer")?.let { item ->
                        parseResponsive(item)?.let { r ->
                            if (seen.add(r.videoId)) out.add(r)
                        }
                    }
                    // musicTwoRowItemRenderer
                    node.optJSONObject("musicTwoRowItemRenderer")?.let { item ->
                        parseTwoRow(item)?.let { r ->
                            if (seen.add(r.videoId)) out.add(r)
                        }
                    }
                    // musicCardShelfRenderer (top search result)
                    node.optJSONObject("musicCardShelfRenderer")?.let { card ->
                        val videoId = card.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                        if (!videoId.isNullOrBlank() && videoId.length == 11 && seen.add(videoId)) {
                            val title = card.optJSONObject("title")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: "Unknown"
                            val subtitle = card.optJSONObject("subtitle")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: ""
                            val thumb = card.optJSONObject("thumbnail")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                                ?.let { a -> a.optJSONObject(a.length() - 1)?.optString("url") }
                            out.add(SearchResult(videoId, title, subtitle, thumb))
                        }
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        val v = node.get(keys.next())
                        if (v is JSONObject || v is JSONArray) queue.add(v)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        val v = node.opt(i)
                        if (v is JSONObject || v is JSONArray) queue.add(v)
                    }
                }
            }
        }
        return out
    }

    private fun parseResponsive(item: JSONObject): SearchResult? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
            ?.takeIf { it.length == 11 }
            ?: item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.length == 11 }
            ?: return null

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
            ?.let { a -> a.optJSONObject(a.length() - 1)?.optString("url") }
        return SearchResult(videoId, title, artist, thumb)
    }

    private fun parseTwoRow(item: JSONObject): SearchResult? {
        val videoId = item.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.takeIf { it.length == 11 }
            ?: item.optJSONObject("thumbnailOverlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?.takeIf { it.length == 11 }
            ?: return null

        val title = item.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text") ?: "Unknown"
        val artist = item.optJSONObject("subtitle")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text") ?: ""
        val thumb = item.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { a -> a.optJSONObject(a.length() - 1)?.optString("url") }
        return SearchResult(videoId, title, artist, thumb)
    }

    suspend fun getDirectAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureVisitorData()
        extractUrl(
            post(
                "player",
                JSONObject().apply {
                    put("context", androidContext())
                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                },
                androidKey
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
                webKey
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
        var bestUrl: String? = null
        var bestBr = -1
        for (key in listOf("adaptiveFormats", "formats")) {
            val arr = streaming.optJSONArray(key) ?: continue
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                if (!f.optString("mimeType").contains("audio")) continue
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
