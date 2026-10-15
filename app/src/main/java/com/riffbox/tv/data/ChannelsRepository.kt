package com.riffbox.tv.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.riffbox.tv.model.Channel
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object ChannelsRepository {

    private const val TAG = "ChannelsRepository"
    private const val CACHE_FILE = "channels_cache.json"
    private const val PREFS_NAME = "riffbox_remote_prefs"
    private const val KEY_REMOTE_URL = "remote_channels_url"

    const val DEFAULT_REMOTE_URL = "https://raw.githubusercontent.com/Fede-rk/RiffboxTV/main/channels.json"

    val defaultCategories = listOf(
        "Todas",
        "🎸 Conciertos",
        "🤘 Rock & Metal",
        "📻 Clásicos 70s, 80s & 90s",
        "🎷 Jazz & Blues",
        "🎧 Electrónica",
        "🌟 Pop & Hits",
        "🌴 Latino & Cumbia",
        "📻 Radios en Vivo"
    )

    val defaultChannels = listOf(
        Channel(1, "Stingray iConcerts HD", "🎸 Conciertos", "https://dash4.antik.sk/live/test_stingray_iconcerts_hd_hevc50/playlist.m3u8", "1080p", isRadio = false),
        Channel(2, "Qello Concerts by Stingray", "🎸 Conciertos", "https://lotus.stingray.com/manifest/qello-qello001-montreal/samsungtvplus/master.m3u8", "1080p", isRadio = false),
        Channel(3, "MTV Live", "🎸 Conciertos", "http://23.237.104.106:8080/USA_MTV_LIVE/index.m3u8", "720p", isRadio = false),
        Channel(4, "Concert Channel", "🎸 Conciertos", "https://jmp2.uk/plu-6868152657543d1f8be4ece3.m3u8", "720p", isRadio = false),
        Channel(5, "Totalmusic Concerts", "🎸 Conciertos", "https://cdn.global.elektamedia.com/live/c7eds/Totalmusic_Concerts/SA_LIVE_hls_enc/master.m3u8", "720p", isRadio = false),
        Channel(6, "XITE Rock x Metal", "🤘 Rock & Metal", "https://d198ro05q94rc4.cloudfront.net/XITE_Rock_On.m3u8", "1080p", isRadio = false),
        Channel(7, "NOW Rock", "🤘 Rock & Metal", "https://lightning-now90s-samsungnz.amagi.tv/playlist.m3u8", "1080p", isRadio = false),
        Channel(8, "Eska Rock TV", "🤘 Rock & Metal", "http://92.114.85.87:8016/play/a005/index.m3u8", "1080p", isRadio = false),
        Channel(9, "Stingray Classic Rock", "🤘 Rock & Metal", "https://lotus.stingray.com/manifest/ose-101ads-montreal/samsungtvplus/master.m3u8", "1080p", isRadio = false),
        Channel(10, "Stingray Rock Alternative", "🤘 Rock & Metal", "https://lotus.stingray.com/manifest/ose-102ads-montreal/samsungtvplus/master.m3u8", "1080p", isRadio = false),
        Channel(11, "MTV Rocks LatAm", "🤘 Rock & Metal", "https://jmp2.uk/plu-66a01b52a4ee27000808ea36.m3u8", "720p", isRadio = false),
        Channel(12, "Alterna TV (Argentina)", "🤘 Rock & Metal", "https://tv.alterna.ar/stream/hls/live.m3u8", "720p", isRadio = false),
        Channel(13, "BRIDGE Rock", "🤘 Rock & Metal", "http://178.134.1.158:8081/BRIDGEROCK/index.m3u8", "720p", isRadio = false),
        Channel(14, "Rock TV Europa", "🤘 Rock & Metal", "https://tv.broadcasting.ro/rocktv/85c83a80-4f71-4f2d-a8d6-43f676896bcb.m3u8", "720p", isRadio = false),
        Channel(15, "Metaleitor TV", "🤘 Rock & Metal", "https://vs20.live.opencaster.com/metaleitortv_423d3342/index.m3u8", "720p", isRadio = false),
        Channel(16, "Lamore Rock Show", "🤘 Rock & Metal", "https://a.cdni.live/rodrigo2364/rodrigo2364/playlist.m3u8", "720p", isRadio = false),
        Channel(17, "NOW 80s Hits", "📻 Clásicos 70s, 80s & 90s", "https://lightning-now80s-samsungnz.amagi.tv/playlist.m3u8", "1080p", isRadio = false),
        Channel(18, "NOW 70s Classic", "📻 Clásicos 70s, 80s & 90s", "https://lightning-now70s-samsungnz.amagi.tv/playlist.m3u8", "1080p", isRadio = false),
        Channel(19, "Stingray Retro", "📻 Clásicos 70s, 80s & 90s", "https://lotus.stingray.com/manifest/ose-104ads-montreal/samsungtvplus/master.m3u8", "1080p", isRadio = false),
        Channel(20, "Totalmusic 80s", "📻 Clásicos 70s, 80s & 90s", "https://cdn.global.elektamedia.com/live/c7eds/Totalmusic_80s/SA_LIVE_hls_enc/master.m3u8", "720p", isRadio = false),
        Channel(21, "MTV Classic LatAm", "📻 Clásicos 70s, 80s & 90s", "https://jmp2.uk/plu-66a11a21a79dea0008aa90ca.m3u8", "720p", isRadio = false),
        Channel(22, "Stingray DJAZZ", "🎷 Jazz & Blues", "https://lotus.stingray.com/manifest/djazz-djaads-montreal/samsungtvplus/master.m3u8", "1080p", isRadio = false),
        Channel(23, "Qwest TV Jazz & Beyond", "🎷 Jazz & Blues", "https://samsungau-qwestjazz-samsungtv-zw2jc.amagi.tv/playlist/samsungau-qwestjazz-samsungtv/playlist.m3u8", "1080p", isRadio = false),
        Channel(24, "Clubbing TV", "🎧 Electrónica", "https://a-cdn.klowdtv.com/live3/clubbingtv_720p/playlist.m3u8", "720p", isRadio = false),
        Channel(25, "Dance Television", "🎧 Electrónica", "https://m1b2.worldcast.tv/dancetelevisionone/2/dancetelevisionone.m3u8", "1080p", isRadio = false),
        Channel(26, "Deluxe Dance", "🎧 Electrónica", "https://sdn-global-live-streaming-packager-cache.3qsdn.com/64733/64733_264_live.m3u8", "1080p", isRadio = false),
        Channel(27, "DanceTV Deep House", "🎧 Electrónica", "https://m1b2.worldcast.tv/dancetelevisiontwo/dancetelevisiontwo.m3u8", "1080p", isRadio = false),
        Channel(28, "Beats Radio TV", "🎧 Electrónica", "https://videostream.shockmedia.com.ar:19360/beatsradio/beatsradio.m3u8", "720p", isRadio = false),
        Channel(29, "Deluxe Music", "🌟 Pop & Hits", "https://sdn-global-live-streaming-packager-cache.3qsdn.com/13456/13456_264_live.m3u8", "720p", isRadio = false),
        Channel(30, "4Fun TV", "🌟 Pop & Hits", "https://stream.4fun.tv:8888/sd/4fun_sd_high.m3u8", "1080p", isRadio = false),
        Channel(31, "Deluxe Rap", "🌟 Pop & Hits", "https://sdn-global-live-streaming-packager-cache.3qsdn.com/65183/65183_264_live.m3u8", "1080p", isRadio = false),
        Channel(32, "Urbano TV", "🌴 Latino & Cumbia", "https://59ef525c24caa.streamlock.net/tvurbano/tvurbano/playlist.m3u8", "720p", isRadio = false),
        Channel(33, "Cumbia Mix TV", "🌴 Latino & Cumbia", "https://cloud.tvomix.com/CUMBIAMIX/index.m3u8", "720p", isRadio = false),
        Channel(34, "FM La Redonda 100.3", "📻 Radios en Vivo", "https://01.solumedia.com.ar:8290/stream?icy=http", "HQ", isRadio = true),
        Channel(35, "Red 92 (FM 92.1)", "📻 Radios en Vivo", "https://radios.solumedia.com/6426/stream", "HQ", isRadio = true),
        Channel(36, "La Cielo (FM 103.5)", "📻 Radios en Vivo", "http://66.128.32.105:8006/;", "HQ", isRadio = true),
        Channel(37, "221 Radio (FM 103.1)", "📻 Radios en Vivo", "https://01.solumedia.com.ar:8612/stream", "HQ", isRadio = true),
        Channel(38, "Radio La Plata (FM 90.9)", "📻 Radios en Vivo", "https://radios.solumedia.com/6348/stream", "HQ", isRadio = true),
        Channel(39, "Rock & Pop 95.9 FM", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/ROCKANDPOPAAC.aac", "AAC", isRadio = true),
        Channel(40, "Radio Nacional Rock 93.7", "📻 Radios en Vivo", "http://sa.mp3.icecast.magma.edge-access.net:7200/sc_rad39", "HQ", isRadio = true),
        Channel(41, "Blue 100.7 FM", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/BLUE_FM_100_7AAC.aac", "AAC", isRadio = true),
        Channel(42, "La 100 (99.9 FM)", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/FM999_56.mp3", "HQ", isRadio = true),
        Channel(43, "Metro 95.1 FM", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/METRO.mp3", "HQ", isRadio = true),
        Channel(44, "Urbana Play 104.3 FM", "📻 Radios en Vivo", "https://vmf.edge-apps.net/playlist/urbana.m3u8/play.m3u8", "HLS", isRadio = true),
        Channel(45, "Pop Radio 101.5 FM", "📻 Radios en Vivo", "https://popradio.stweb.tv/popradio/live/playlist.m3u8", "HLS", isRadio = true),
        Channel(46, "Radio Mitre AM 790", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/AM790_56AAC.aac", "AAC", isRadio = true),
        Channel(47, "Cadena 3 Argentina", "📻 Radios en Vivo", "https://liveradio.mediainbox.net/radio3.mp3", "HQ", isRadio = true),
        Channel(48, "Radio Rivadavia AM 630", "📻 Radios en Vivo", "https://playerservices.streamtheworld.com/api/livestream-redirect/RIVADAVIAAAC.aac", "AAC", isRadio = true),
        Channel(49, "Radio 10 AM 710", "📻 Radios en Vivo", "https://radio10.stweb.tv/radio10/live/playlist.m3u8", "HLS", isRadio = true),
        Channel(50, "Radio Nacional Folklórica", "📻 Radios en Vivo", "http://sa.mp3.icecast.magma.edge-access.net:7200/sc_rad38", "HQ", isRadio = true)
    )

    var categories: List<String> = defaultCategories
        private set

    var channels: List<Channel> = defaultChannels
        private set

    fun getChannelsByCategory(category: String): List<Channel> {
        return if (category == "Todas") channels else channels.filter { it.category == category }
    }

    fun init(context: Context, onSyncComplete: ((Boolean, AppUpdateInfo?) -> Unit)? = null) {
        loadFromCache(context)

        val remoteUrl = getRemoteUrl(context)
        Thread {
            val (success, updateInfo) = fetchRemote(context, remoteUrl)
            Handler(Looper.getMainLooper()).post {
                onSyncComplete?.invoke(success, updateInfo)
            }
        }.start()
    }

    private fun loadFromCache(context: Context) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            if (file.exists()) {
                val jsonStr = file.readText(Charsets.UTF_8)
                parseJson(jsonStr)
                Log.d(TAG, "Loaded channels from local cache.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache fallback: ${e.message}")
        }
    }

    private fun fetchRemote(context: Context, urlStr: String): Pair<Boolean, AppUpdateInfo?> {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "RiffboxTV-Android")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val updateInfo = parseJson(jsonStr)
                File(context.filesDir, CACHE_FILE).writeText(jsonStr, Charsets.UTF_8)
                return Pair(true, updateInfo)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Remote sync skipped: ${e.message}")
        }
        return Pair(false, null)
    }

    private fun parseJson(jsonStr: String): AppUpdateInfo? {
        val root = JSONObject(jsonStr)

        if (root.has("channels")) {
            val arr = root.getJSONArray("channels")
            val parsedList = mutableListOf<Channel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsedList.add(
                    Channel(
                        id = obj.optInt("id", i + 1),
                        name = obj.getString("name"),
                        category = obj.getString("category"),
                        streamUrl = obj.getString("streamUrl"),
                        badge = obj.optString("badge", "HD"),
                        isRadio = obj.optBoolean("isRadio", false)
                    )
                )
            }
            if (parsedList.isNotEmpty()) {
                channels = parsedList
            }
        }

        if (root.has("categories")) {
            val catArr = root.getJSONArray("categories")
            val catList = mutableListOf<String>()
            for (i in 0 until catArr.length()) {
                catList.add(catArr.getString(i))
            }
            if (catList.isNotEmpty()) {
                categories = catList
            }
        }

        if (root.has("latest_app_version_code")) {
            val vCode = root.optInt("latest_app_version_code", 1)
            val vName = root.optString("latest_app_version_name", "1.1.0")
            val notes = root.optString("release_notes", "")
            val dlUrl = root.optString("download_url", "")
            return AppUpdateInfo(vCode, vName, notes, dlUrl)
        }

        return null
    }

    fun getRemoteUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_REMOTE_URL, null)
        if (saved == null || saved.contains("federico-riffbox") || !saved.startsWith("http")) {
            prefs.edit().putString(KEY_REMOTE_URL, DEFAULT_REMOTE_URL).apply()
            return DEFAULT_REMOTE_URL
        }
        return saved
    }

    fun setRemoteUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REMOTE_URL, url).apply()
    }
}