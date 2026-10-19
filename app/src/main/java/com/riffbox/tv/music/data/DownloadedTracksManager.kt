package com.riffbox.tv.music.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.riffbox.tv.music.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DownloadedTracksManager {

    private const val PREFS_NAME = "riffbox_offline_tracks"
    private const val KEY_TRACKS = "downloaded_tracks_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDownloadedTracks(context: Context): MutableList<MusicTrack> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_TRACKS, null) ?: return mutableListOf()
        val list = mutableListOf<MusicTrack>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val path = obj.optString("localFilePath")
                val file = if (path.isNotEmpty()) File(path) else null
                // Verify the file still exists on disk
                if (file != null && file.exists()) {
                    list.add(
                        MusicTrack(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.optString("album", ""),
                            duration = obj.optString("duration", ""),
                            thumbnailUrl = obj.optString("thumbnailUrl", ""),
                            isDownloaded = true,
                            localFilePath = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun isTrackDownloaded(context: Context, trackId: String): Boolean {
        return getDownloadedTracks(context).any { it.id == trackId }
    }

    fun saveTrack(context: Context, track: MusicTrack, localPath: String) {
        val tracks = getDownloadedTracks(context)
        // Remove existing if any
        tracks.removeAll { it.id == track.id }
        track.isDownloaded = true
        track.localFilePath = localPath
        tracks.add(0, track) // newest first

        val array = JSONArray()
        for (t in tracks) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("artist", t.artist)
                put("album", t.album)
                put("duration", t.duration)
                put("thumbnailUrl", t.thumbnailUrl)
                put("localFilePath", t.localFilePath)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TRACKS, array.toString()).apply()
    }

    fun removeTrack(context: Context, trackId: String) {
        val tracks = getDownloadedTracks(context)
        val target = tracks.find { it.id == trackId }
        if (target != null && target.localFilePath != null) {
            try {
                File(target.localFilePath!!).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tracks.removeAll { it.id == trackId }

        val array = JSONArray()
        for (t in tracks) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("artist", t.artist)
                put("album", t.album)
                put("duration", t.duration)
                put("thumbnailUrl", t.thumbnailUrl)
                put("localFilePath", t.localFilePath)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_TRACKS, array.toString()).apply()
    }

    fun getOfflineDir(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "offline_music")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://loader.to/ajax/download.php?format=mp3&url=https://www.youtube.com/watch?v=$videoId",
            "https://en.loader.to/ajax/download.php?format=mp3&url=https://www.youtube.com/watch?v=$videoId"
        )

        for (ep in endpoints) {
            try {
                val conn = URL(ep).openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                conn.connect()
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(response)
                if (json.optBoolean("success", false)) {
                    val directUrl = json.optString("download_url")
                    if (directUrl.isNotEmpty() && directUrl != "null") {
                        return@withContext directUrl
                    }

                    var progressUrl = json.optString("progress_url")
                    if (progressUrl.isEmpty() || progressUrl == "null") {
                        val id = json.optString("id")
                        if (id.isNotEmpty()) {
                            progressUrl = "https://lto2.affadaffa.com/api/progress?id=$id"
                        }
                    }

                    if (progressUrl.isNotEmpty()) {
                        // Poll progress URL up to 30 times (every 1.5 seconds)
                        for (attempt in 1..30) {
                            kotlinx.coroutines.delay(1500)
                            try {
                                val pConn = URL(progressUrl).openConnection() as HttpURLConnection
                                pConn.connectTimeout = 8000
                                pConn.readTimeout = 8000
                                pConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                                val pResp = pConn.inputStream.bufferedReader().use { it.readText() }
                                pConn.disconnect()

                                val pJson = JSONObject(pResp)
                                val dl = pJson.optString("download_url")
                                val success = pJson.optInt("success", 0)
                                if (dl.isNotEmpty() && dl != "null" && (success == 1 || dl.startsWith("http"))) {
                                    return@withContext dl
                                }
                                val text = pJson.optString("text")
                                if (text.contains("error", ignoreCase = true)) {
                                    break
                                }
                            } catch (pe: Exception) {
                                // continue polling
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }

    suspend fun downloadAudioStream(
        context: Context,
        track: MusicTrack,
        audioStreamUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val safeName = "${track.artist} - ${track.title}"
            .replace(Regex("[^a-zA-Z0-9.-]"), "_")
            .take(60)
        val outputFile = File(getOfflineDir(context), "${safeName}_${track.id}.mp3")
        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }

            var currentUrl = audioStreamUrl
            var connection: HttpURLConnection
            var redirectCount = 0

            while (true) {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                val responseCode = connection.responseCode
                if ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == 307 || responseCode == 308) && redirectCount < 5
                ) {
                    val newLocation = connection.getHeaderField("Location")
                    if (!newLocation.isNullOrEmpty()) {
                        currentUrl = newLocation
                        redirectCount++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            val totalBytes = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                if (totalBytes > 0) {
                    val progress = (downloadedBytes * 100L / totalBytes).toInt()
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            saveTrack(context, track, outputFile.absolutePath)
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
