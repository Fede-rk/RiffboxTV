package com.riffbox.tv.music.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Resolves a YouTube videoId to a direct audio stream URL using the
 * YouTube InnerTube API (the same API used by YouTube's own clients).
 *
 * This bypasses WebView entirely — ExoPlayer streams the audio directly.
 */
object YoutubeStreamResolver {

    private const val TAG = "YTStreamResolver"

    // Android YouTube client credentials (public, used by YouTube Music app)
    private const val ANDROID_CLIENT_NAME = "ANDROID"
    private const val ANDROID_CLIENT_VERSION = "18.11.34"
    private const val ANDROID_SDK_VERSION = 30
    private const val INNERTUBE_API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39Q"

    // iOS client as secondary fallback
    private const val IOS_CLIENT_NAME = "IOS"
    private const val IOS_CLIENT_VERSION = "19.09.3"

    /**
     * Returns a direct audio stream URL for the given YouTube videoId.
     * Returns null if resolution fails.
     */
    suspend fun resolve(videoId: String): String? = withContext(Dispatchers.IO) {
        // Try Android client first (most reliable for audio extraction)
        tryAndroidClient(videoId)
            ?: tryIosClient(videoId)
            ?: tryWebClient(videoId)
    }

    private fun tryAndroidClient(videoId: String): String? {
        return try {
            val url = URL("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "com.google.android.youtube/$ANDROID_CLIENT_VERSION (Linux; U; Android $ANDROID_SDK_VERSION) gzip")
            conn.setRequestProperty("X-YouTube-Client-Name", "3")
            conn.setRequestProperty("X-YouTube-Client-Version", ANDROID_CLIENT_VERSION)
            conn.doOutput = true

            val payload = buildPlayerPayload(
                videoId = videoId,
                clientName = ANDROID_CLIENT_NAME,
                clientVersion = ANDROID_CLIENT_VERSION,
                sdkVersion = ANDROID_SDK_VERSION
            )

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(payload)
            writer.flush()
            writer.close()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Android client HTTP ${conn.responseCode} for $videoId")
                conn.disconnect()
                return null
            }

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()

            val audioUrl = extractBestAudioUrl(JSONObject(response))
            if (audioUrl != null) {
                Log.i(TAG, "Android client resolved $videoId → ${audioUrl.take(80)}...")
            }
            audioUrl
        } catch (e: Exception) {
            Log.e(TAG, "Android client failed for $videoId: ${e.message}")
            null
        }
    }

    private fun tryIosClient(videoId: String): String? {
        return try {
            val url = URL("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "com.google.ios.youtube/$IOS_CLIENT_VERSION (iPhone; CPU iPhone OS 16_7 like Mac OS X)")
            conn.setRequestProperty("X-YouTube-Client-Name", "5")
            conn.setRequestProperty("X-YouTube-Client-Version", IOS_CLIENT_VERSION)
            conn.doOutput = true

            val payload = buildPlayerPayload(
                videoId = videoId,
                clientName = IOS_CLIENT_NAME,
                clientVersion = IOS_CLIENT_VERSION,
                sdkVersion = null
            )

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(payload)
            writer.flush()
            writer.close()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "iOS client HTTP ${conn.responseCode} for $videoId")
                conn.disconnect()
                return null
            }

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()

            val audioUrl = extractBestAudioUrl(JSONObject(response))
            if (audioUrl != null) {
                Log.i(TAG, "iOS client resolved $videoId → ${audioUrl.take(80)}...")
            }
            audioUrl
        } catch (e: Exception) {
            Log.e(TAG, "iOS client failed for $videoId: ${e.message}")
            null
        }
    }

    private fun tryWebClient(videoId: String): String? {
        return try {
            val url = URL("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            conn.doOutput = true

            val payload = buildPlayerPayload(
                videoId = videoId,
                clientName = "WEB",
                clientVersion = "2.20240101.01.00",
                sdkVersion = null
            )

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(payload)
            writer.flush()
            writer.close()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()

            val audioUrl = extractBestAudioUrl(JSONObject(response))
            if (audioUrl != null) {
                Log.i(TAG, "WEB client resolved $videoId → ${audioUrl.take(80)}...")
            }
            audioUrl
        } catch (e: Exception) {
            Log.e(TAG, "WEB client failed for $videoId: ${e.message}")
            null
        }
    }

    private fun buildPlayerPayload(
        videoId: String,
        clientName: String,
        clientVersion: String,
        sdkVersion: Int?
    ): String {
        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", "es")
            put("gl", "US")
            if (sdkVersion != null) {
                put("androidSdkVersion", sdkVersion)
            }
        }

        return JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", clientObj)
            })
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put("params", "CgIIAQ==") // This param requests audio-only formats
        }.toString()
    }

    /**
     * Extracts the best audio stream URL from an InnerTube player response.
     * Priority: opus/webm > m4a/mp4 audio adaptive > any audio format
     */
    private fun extractBestAudioUrl(response: JSONObject): String? {
        try {
            val streamingData = response.optJSONObject("streamingData") ?: return null

            // Check if this video can be played
            val playabilityStatus = response.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != null && status != "OK" && status != "LIVE_STREAM_OFFLINE") {
                Log.w(TAG, "Video not playable: $status - ${playabilityStatus.optString("reason")}")
                return null
            }

            // 1. Try adaptive formats (audio-only, best quality)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                val audioFormats = mutableListOf<Pair<Int, String>>() // (bitrate, url)

                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mimeType = format.optString("mimeType")

                    // Only audio-only formats
                    if (!mimeType.startsWith("audio/")) continue

                    val url = resolveUrl(format) ?: continue
                    val bitrate = format.optInt("bitrate", 0)
                    audioFormats.add(Pair(bitrate, url))
                }

                if (audioFormats.isNotEmpty()) {
                    // Pick highest bitrate audio stream
                    val bestAudio = audioFormats.maxByOrNull { it.first }
                    if (bestAudio != null) {
                        return bestAudio.second
                    }
                }
            }

            // 2. Try muxed formats (audio+video combined, lower quality but works)
            val formats = streamingData.optJSONArray("formats")
            if (formats != null && formats.length() > 0) {
                // Pick lowest resolution (smallest file = mostly audio) muxed format
                for (i in 0 until formats.length()) {
                    val format = formats.optJSONObject(i) ?: continue
                    val url = resolveUrl(format) ?: continue
                    return url
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "extractBestAudioUrl error: ${e.message}")
        }
        return null
    }

    /**
     * Resolves either a direct "url" field or a "signatureCipher"/"cipher" field.
     * For cipher URLs, we only handle the simple n-param case (unsignedUrl).
     */
    private fun resolveUrl(format: JSONObject): String? {
        // Direct URL (most common for Android/iOS clients)
        val directUrl = format.optString("url")
        if (directUrl.isNotEmpty() && directUrl.startsWith("http")) {
            return directUrl
        }

        // Try signatureCipher (used by WEB client)
        val cipher = format.optString("signatureCipher").ifEmpty {
            format.optString("cipher")
        }

        if (cipher.isNotEmpty()) {
            return decodeCipherUrl(cipher)
        }

        return null
    }

    /**
     * Basic cipher URL decoder. Extracts the url param from the cipher string.
     * Note: This only works for videos that don't require JS signature transforms.
     * The Android/iOS clients typically return direct URLs without cipher.
     */
    private fun decodeCipherUrl(cipher: String): String? {
        return try {
            val params = cipher.split("&").associate { part ->
                val idx = part.indexOf('=')
                if (idx >= 0) {
                    URLDecoder.decode(part.substring(0, idx), "UTF-8") to
                    URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                } else {
                    part to ""
                }
            }
            params["url"]
        } catch (e: Exception) {
            null
        }
    }
}
