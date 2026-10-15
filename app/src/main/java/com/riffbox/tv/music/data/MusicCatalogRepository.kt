package com.riffbox.tv.music.data

import com.riffbox.tv.music.model.MusicArtist
import com.riffbox.tv.music.model.MusicCategory
import com.riffbox.tv.music.model.MusicPlaylist
import com.riffbox.tv.music.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object MusicCatalogRepository {

    val categories: List<MusicCategory> = listOf(
        MusicCategory("rock_clasico", "Rock Clásico", "⚡", 0xFFFF5722, 0xFFFF9800, "Classic Rock 70s 80s greatest hits"),
        MusicCategory("metal", "Hard Rock & Metal", "🤘", 0xFFE91E63, 0xFF9C27B0, "Heavy Metal Hard Rock essentials"),
        MusicCategory("grunge", "Grunge & 90s", "🌧️", 0xFF00BCD4, 0xFF3F51B5, "Grunge 90s alternative rock"),
        MusicCategory("nacional", "Rock Nacional", "🇦🇷", 0xFF03A9F4, 0xFFE0E0E0, "Rock Nacional Argentino clasicos"),
        MusicCategory("punk", "Punk & Alternativo", "🧷", 0xFFFF9800, 0xFFF44336, "Punk Rock alternative anthems"),
        MusicCategory("blues", "Blues & Psicodelia", "🎷", 0xFF673AB7, 0xFF009688, "Blues Rock psychedelic rock"),
        MusicCategory("acustico", "Acústico & Baladas", "☕", 0xFF795548, 0xFFFFB74D, "Acoustic Rock unplugged ballads")
    )

    val artists: List<MusicArtist> = listOf(
        MusicArtist("soda", "Soda Stereo", "Rock en Español", "https://i.scdn.co/image/ab6761610000e5eb26d24a6873bc0c14b2169724", "Soda Stereo canciones"),
        MusicArtist("queen", "Queen", "Classic Rock", "https://i.scdn.co/image/ab6761610000e5ebe455c1ce6ecf582f3475220a", "Queen greatest hits"),
        MusicArtist("nirvana", "Nirvana", "Grunge", "https://i.scdn.co/image/ab6761610000e5ebb5b8d48a58145a8e0f6f075c", "Nirvana live and studio"),
        MusicArtist("acdc", "AC/DC", "Hard Rock", "https://i.scdn.co/image/ab6761610000e5eb0058b87455946cf9f5d37827", "AC DC full concert"),
        MusicArtist("metallica", "Metallica", "Heavy Metal", "https://i.scdn.co/image/ab6761610000e5eb793e20ec4d63a8326a271295", "Metallica best songs"),
        MusicArtist("charly", "Charly García", "Rock Nacional", "https://i.scdn.co/image/ab6761610000e5ebb77ca226d71b3e86fcfd586d", "Charly Garcia exitos"),
        MusicArtist("pinkfloyd", "Pink Floyd", "Progressive Rock", "https://i.scdn.co/image/ab6761610000e5ebea99d0840b33b00c5c4e7235", "Pink Floyd full album"),
        MusicArtist("gnr", "Guns N' Roses", "Hard Rock", "https://i.scdn.co/image/ab6761610000e5ebc1f93f5451fa2ea2e87c0e5a", "Guns N Roses top tracks"),
        MusicArtist("foofighters", "Foo Fighters", "Alternative Rock", "https://i.scdn.co/image/ab6761610000e5eb6747b01d2342ec6eb253b207", "Foo Fighters live"),
        MusicArtist("rhcp", "Red Hot Chili Peppers", "Funk Rock", "https://i.scdn.co/image/ab6761610000e5eb1e792b0c4eb5e7c8b0568019", "Red Hot Chili Peppers greatest hits")
    )

    val playlists: List<MusicPlaylist> = listOf(
        MusicPlaylist(
            id = "pl_riffs",
            title = "Los 100 Mejores Riffs de la Historia",
            subtitle = "Led Zeppelin, AC/DC, Deep Purple, Black Sabbath & Jimi Hendrix",
            badge = "ESENCIAL",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            tracks = listOf(
                MusicTrack("qKggnBh2vVU", "Smoke on the Water", "Deep Purple", "Machine Head", "5:40", "https://i.ytimg.com/vi/qKggnBh2vVU/hqdefault.jpg"),
                MusicTrack("pAgnJDJN4VA", "Back in Black", "AC/DC", "Back in Black", "4:15", "https://i.ytimg.com/vi/pAgnJDJN4VA/hqdefault.jpg"),
                MusicTrack("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "5:55", "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg"),
                MusicTrack("8SbUC-UaAxE", "November Rain", "Guns N' Roses", "Use Your Illusion I", "8:57", "https://i.ytimg.com/vi/8SbUC-UaAxE/hqdefault.jpg"),
                MusicTrack("CD-E-LDc384", "Enter Sandman", "Metallica", "Metallica (Black Album)", "5:31", "https://i.ytimg.com/vi/CD-E-LDc384/hqdefault.jpg"),
                MusicTrack("HQMm7l6XQ4E", "Whole Lotta Love", "Led Zeppelin", "Led Zeppelin II", "5:34", "https://i.ytimg.com/vi/HQMm7l6XQ4E/hqdefault.jpg"),
                MusicTrack("0qanF-91aJo", "Paranoid", "Black Sabbath", "Paranoid", "2:48", "https://i.ytimg.com/vi/0qanF-91aJo/hqdefault.jpg")
            )
        ),
        MusicPlaylist(
            id = "pl_grunge",
            title = "Himnos del Grunge & 90s",
            subtitle = "Nirvana, Pearl Jam, Soundgarden, Alice In Chains & Stone Temple Pilots",
            badge = "90s SEATTLE",
            coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80",
            tracks = listOf(
                MusicTrack("hTWKbfoikeg", "Smells Like Teen Spirit", "Nirvana", "Nevermind", "5:01", "https://i.ytimg.com/vi/hTWKbfoikeg/hqdefault.jpg"),
                MusicTrack("MS91knuzoOA", "Alive", "Pearl Jam", "Ten", "5:41", "https://i.ytimg.com/vi/MS91knuzoOA/hqdefault.jpg"),
                MusicTrack("3mbBbFH9fAg", "Black Hole Sun", "Soundgarden", "Superunknown", "5:18", "https://i.ytimg.com/vi/3mbBbFH9fAg/hqdefault.jpg"),
                MusicTrack("TAqZb52sgpU", "Man in the Box", "Alice In Chains", "Facelift", "4:44", "https://i.ytimg.com/vi/TAqZb52sgpU/hqdefault.jpg"),
                MusicTrack("vabnZ9-ex7o", "Come as You Are", "Nirvana", "Nevermind", "3:39", "https://i.ytimg.com/vi/vabnZ9-ex7o/hqdefault.jpg"),
                MusicTrack("V5UOC0C0x8Q", "Plush", "Stone Temple Pilots", "Core", "5:13", "https://i.ytimg.com/vi/V5UOC0C0x8Q/hqdefault.jpg"),
                MusicTrack("NCO_kZAbE4Q", "1979", "The Smashing Pumpkins", "Mellon Collie", "4:24", "https://i.ytimg.com/vi/NCO_kZAbE4Q/hqdefault.jpg")
            )
        ),
        MusicPlaylist(
            id = "pl_nacional",
            title = "Leyendas del Rock Argentino",
            subtitle = "Soda Stereo, Charly García, Los Redondos, Spinetta & Divididos",
            badge = "NACIONAL",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            tracks = listOf(
                MusicTrack("sWft4cMiwXQ", "De Música Ligera", "Soda Stereo", "Canción Animal", "3:32", "https://i.ytimg.com/vi/sWft4cMiwXQ/hqdefault.jpg"),
                MusicTrack("RdVc6LEEIL0", "Persiana Americana", "Soda Stereo", "Signos", "4:52", "https://i.ytimg.com/vi/RdVc6LEEIL0/hqdefault.jpg"),
                MusicTrack("y5X31wU9E9s", "Demoliendo Hoteles", "Charly García", "Piano Bar", "2:18", "https://i.ytimg.com/vi/y5X31wU9E9s/hqdefault.jpg"),
                MusicTrack("J_e9L2hB3s0", "Ji Ji Ji", "Patricio Rey y sus Redonditos de Ricota", "Oktubre", "5:32", "https://i.ytimg.com/vi/J_e9L2hB3s0/hqdefault.jpg"),
                MusicTrack("WjP7XkZ1111", "Ala Delta", "Divididos", "Acariciando lo Áspero", "4:12", "https://i.ytimg.com/vi/sWft4cMiwXQ/hqdefault.jpg"),
                MusicTrack("dZQur2gDGWk", "En la Ciudad de la Furia", "Soda Stereo", "Doble Vida", "5:45", "https://i.ytimg.com/vi/dZQur2gDGWk/hqdefault.jpg"),
                MusicTrack("X6m7gM4ZpQs", "Rezo por Vos", "Charly García & Luis Alberto Spinetta", "Privé", "4:30", "https://i.ytimg.com/vi/y5X31wU9E9s/hqdefault.jpg")
            )
        ),
        MusicPlaylist(
            id = "pl_metal",
            title = "Heavy Metal & Thrash Essentials",
            subtitle = "Iron Maiden, Metallica, Megadeth, Judas Priest & Pantera",
            badge = "HEAVY",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            tracks = listOf(
                MusicTrack("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12", "https://i.ytimg.com/vi/WxnN05vOuSM/hqdefault.jpg"),
                MusicTrack("2f1Ny74_ou0", "Master of Puppets", "Metallica", "Master of Puppets", "8:35", "https://i.ytimg.com/vi/2f1Ny74_ou0/hqdefault.jpg"),
                MusicTrack("9d4ui9q7eDM", "Holy Wars... The Punishment Due", "Megadeth", "Rust in Peace", "6:32", "https://i.ytimg.com/vi/9d4ui9q7eDM/hqdefault.jpg"),
                MusicTrack("L397TWLwrUU", "Painkiller", "Judas Priest", "Painkiller", "6:06", "https://i.ytimg.com/vi/L397TWLwrUU/hqdefault.jpg"),
                MusicTrack("AkFqg5wAuFk", "Walk", "Pantera", "Vulgar Display of Power", "5:15", "https://i.ytimg.com/vi/AkFqg5wAuFk/hqdefault.jpg")
            )
        ),
        MusicPlaylist(
            id = "pl_2000s",
            title = "MTV Generación 90s & 2000s",
            subtitle = "Foo Fighters, Green Day, Linkin Park, Blink-182 & The Offspring",
            badge = "MTV HITS",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
            tracks = listOf(
                MusicTrack("eVTXPUF4Oz4", "In the End", "Linkin Park", "Hybrid Theory", "3:36", "https://i.ytimg.com/vi/eVTXPUF4Oz4/hqdefault.jpg"),
                MusicTrack("SBjQ9tuuKSQ", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10", "https://i.ytimg.com/vi/SBjQ9tuuKSQ/hqdefault.jpg"),
                MusicTrack("NUTGr5t3Mo8", "Basket Case", "Green Day", "Dookie", "3:01", "https://i.ytimg.com/vi/NUTGr5t3Mo8/hqdefault.jpg"),
                MusicTrack("9jK-NcRmVcw", "The Pretender", "Foo Fighters", "Echoes, Silence, Patience & Grace", "4:30", "https://i.ytimg.com/vi/9jK-NcRmVcw/hqdefault.jpg"),
                MusicTrack("kXYiU_JCYtU", "Numb", "Linkin Park", "Meteora", "3:07", "https://i.ytimg.com/vi/kXYiU_JCYtU/hqdefault.jpg"),
                MusicTrack("98W9QuMq-2k", "All the Small Things", "Blink-182", "Enema of the State", "2:47", "https://i.ytimg.com/vi/98W9QuMq-2k/hqdefault.jpg")
            )
        )
    )

    /**
     * Real-time search query powered by YouTube Music Innertube API
     */
    suspend fun searchMusic(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()

        try {
            val url = URL("https://music.youtube.com/youtubei/v1/search")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "es")
                        put("gl", "US")
                    })
                })
                put("query", query)
                put("params", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D") // Filter for tracks only
            }

            connection.doOutput = true
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseText = reader.readText()
            reader.close()
            connection.disconnect()

            val responseJson = JSONObject(responseText)
            val parsedTracks = extractTracksFromYtmJson(responseJson)
            if (parsedTracks.isNotEmpty()) {
                return@withContext parsedTracks
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: match from local curated catalog if offline
        val lower = query.lowercase()
        val allLocalTracks = playlists.flatMap { it.tracks }.distinctBy { it.id }
        allLocalTracks.filter {
            it.title.lowercase().contains(lower) || it.artist.lowercase().contains(lower)
        }
    }

    private fun extractTracksFromYtmJson(json: JSONObject): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        try {
            // Find all musicResponsiveListItemRenderer elements recursively
            val items = mutableListOf<JSONObject>()
            findRenderers(json, "musicResponsiveListItemRenderer", items)

            for (item in items) {
                try {
                    val flexCols = item.optJSONArray("flexColumns") ?: continue
                    if (flexCols.length() < 2) continue

                    val col0 = flexCols.getJSONObject(0)
                        .getJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        .getJSONObject("text").getJSONArray("runs")
                    val title = col0.getJSONObject(0).getString("text")

                    val col1 = flexCols.getJSONObject(1)
                        .getJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        .getJSONObject("text").getJSONArray("runs")
                    val artist = if (col1.length() > 0) col1.getJSONObject(0).getString("text") else "Artista Desconocido"

                    var duration = ""
                    if (col1.length() > 4) {
                        duration = col1.getJSONObject(col1.length() - 1).optString("text", "")
                    }

                    // Video ID
                    var videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                    if (videoId.isNullOrEmpty()) {
                        videoId = item.optJSONObject("overlay")
                            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("musicPlayButtonRenderer")
                            ?.optJSONObject("playNavigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                    }

                    // Thumbnail
                    val thumbs = item.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                    val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.getJSONObject(thumbs.length() - 1).getString("url")
                    } else if (!videoId.isNullOrEmpty()) {
                        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    } else ""

                    if (!videoId.isNullOrEmpty() && title.isNotEmpty()) {
                        tracks.add(
                            MusicTrack(
                                id = videoId,
                                title = title,
                                artist = artist,
                                duration = duration,
                                thumbnailUrl = thumbUrl
                            )
                        )
                    }
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tracks
    }

    private fun findRenderers(obj: Any, targetKey: String, results: MutableList<JSONObject>) {
        if (obj is JSONObject) {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == targetKey) {
                    val target = obj.optJSONObject(key)
                    if (target != null) results.add(target)
                } else {
                    val child = obj.opt(key)
                    if (child != null) findRenderers(child, targetKey, results)
                }
            }
        } else if (obj is JSONArray) {
            for (i in 0 until obj.length()) {
                val item = obj.opt(i)
                if (item != null) findRenderers(item, targetKey, results)
            }
        }
    }
}
