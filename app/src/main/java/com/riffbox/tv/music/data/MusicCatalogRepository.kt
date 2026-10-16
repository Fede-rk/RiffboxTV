package com.riffbox.tv.music.data

import com.riffbox.tv.music.model.ArtistProfile
import com.riffbox.tv.music.model.InfiniteGenreStation
import com.riffbox.tv.music.model.MusicAlbum
import com.riffbox.tv.music.model.MusicArtist
import com.riffbox.tv.music.model.MusicArtistSummary
import com.riffbox.tv.music.model.MusicCategory
import com.riffbox.tv.music.model.MusicPlaylist
import com.riffbox.tv.music.model.MusicTrack
import com.riffbox.tv.music.model.SearchMusicResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object MusicCatalogRepository {

    private fun track(id: String, title: String, artist: String, album: String = "", duration: String = "") =
        MusicTrack(id, title, artist, album, duration, "https://i.ytimg.com/vi/$id/hqdefault.jpg")

    val infiniteStations: List<InfiniteGenreStation> = listOf(
        InfiniteGenreStation(
            id = "station_lucky",
            title = "Voy a tener suerte",
            subtitle = "Ruleta musical 100% aleatoria",
            icon = "🎲",
            gradientStart = 0xFF8E2DE2,
            gradientEnd = 0xFF4A00E0,
            isLucky = true
        ),
        InfiniteGenreStation(
            id = "station_metal",
            title = "Metal & Pesado",
            subtitle = "Heavy, Thrash, Nu-Metal & Groove",
            icon = "🤘",
            gradientStart = 0xFFB71C1C,
            gradientEnd = 0xFFFF5722,
            localPlaylistIds = listOf("pl_metal_nacional", "pl_metal", "pl_metal_extremo"),
            seedArtists = listOf(
                "Metallica", "Iron Maiden", "Megadeth", "Pantera", "Horcas", "Carajo",
                "Hermética", "Malón", "Almafuerte", "V8", "Slipknot", "Slayer", "Judas Priest",
                "Tren Loco", "Plan 4", "Rammstein", "Sepultura", "Gojira", "Avenged Sevenfold",
                "O'Connor", "Logos", "Asspera", "Arde la Sangre", "Anthrax", "Black Sabbath"
            )
        ),
        InfiniteGenreStation(
            id = "station_rock",
            title = "Puro Rock",
            subtitle = "Rock Clásico, Nacional & Alternativo",
            icon = "🎸",
            gradientStart = 0xFFC62828,
            gradientEnd = 0xFFF57C00,
            localPlaylistIds = listOf("pl_riffs", "pl_nacional", "pl_grunge"),
            seedArtists = listOf(
                "AC/DC", "Guns N' Roses", "Led Zeppelin", "La Renga", "Los Redondos",
                "Divididos", "Charly García", "Soda Stereo", "Queen", "Foo Fighters",
                "Aerosmith", "The Rolling Stones", "Deep Purple", "Pearl Jam", "Callejeros",
                "Los Piojos", "Las Pastillas del Abuelo", "Guasones", "Don Osvaldo", "Ciro y los Persas"
            )
        ),
        InfiniteGenreStation(
            id = "station_punk",
            title = "Punk Rock",
            subtitle = "Punk, Skate Punk & Hardcore",
            icon = "⚡",
            gradientStart = 0xFF880E4F,
            gradientEnd = 0xFF00E676,
            localPlaylistIds = listOf("pl_2000s"),
            seedArtists = listOf(
                "Green Day", "Blink-182", "The Offspring", "Attaque 77", "Bad Religion",
                "2 Minutos", "Cadena Perpetua", "Flema", "Ramones", "Sum 41", "NOFX",
                "Pennywise", "Millencolin", "The Clash", "Sex Pistols", "Eterna Inocencia"
            )
        ),
        InfiniteGenreStation(
            id = "station_cumbia",
            title = "Cumbia & Fiesta",
            subtitle = "Tropical, Villera, Cuarteto & Fiesta",
            icon = "🌴",
            gradientStart = 0xFF00695C,
            gradientEnd = 0xFFFFB300,
            localPlaylistIds = listOf("pl_fiesta"),
            seedArtists = listOf(
                "Los Palmeras", "Damas Gratis", "Rodrigo", "La Mona Jiménez", "Ke Personajes",
                "Gilda", "Ráfaga", "Yerba Brava", "Walter Olmos", "Ulises Bueno", "Amar Azul",
                "La K'onga", "Tambó Tambó", "Mala Fama", "Los Charros", "Los del Fuego", "Leo Mattioli"
            )
        ),
        InfiniteGenreStation(
            id = "station_80s90s",
            title = "Clásicos 80s & 90s",
            subtitle = "Synthpop, New Wave & Hits Retro",
            icon = "📻",
            gradientStart = 0xFF4A148C,
            gradientEnd = 0xFF00B0FF,
            localPlaylistIds = listOf("pl_riffs", "pl_pop_hits"),
            seedArtists = listOf(
                "Michael Jackson", "Madonna", "Queen", "a-ha", "Depeche Mode", "The Cure",
                "Tears for Fears", "Phil Collins", "Cyndi Lauper", "Whitney Houston",
                "George Michael", "ABBA", "Duran Duran", "Bon Jovi", "Guns N' Roses"
            )
        ),
        InfiniteGenreStation(
            id = "station_2000s",
            title = "Éxitos 2000s",
            subtitle = "Nu-Metal, Post-Grunge & Pop 2000",
            icon = "🎧",
            gradientStart = 0xFF0D47A1,
            gradientEnd = 0xFF00E5FF,
            localPlaylistIds = listOf("pl_2000s", "pl_grunge"),
            seedArtists = listOf(
                "Linkin Park", "Evanescence", "Papa Roach", "The Killers", "Coldplay",
                "Muse", "Arctic Monkeys", "System of a Down", "Limp Bizkit", "Avril Lavigne",
                "Audioslave", "Gorillaz", "Franz Ferdinand", "My Chemical Romance", "Paramore"
            )
        ),
        InfiniteGenreStation(
            id = "station_pop",
            title = "Pop Global",
            subtitle = "Hits Mundiales & Dance Pop",
            icon = "🌟",
            gradientStart = 0xFFAD1457,
            gradientEnd = 0xFFFF4081,
            localPlaylistIds = listOf("pl_pop_hits"),
            seedArtists = listOf(
                "Dua Lipa", "The Weeknd", "Bruno Mars", "Coldplay", "Taylor Swift",
                "Ed Sheeran", "Miley Cyrus", "Harry Styles", "Lady Gaga", "Katy Perry",
                "Billie Eilish", "Imagine Dragons", "OneRepublic", "Maroon 5", "Shakira"
            )
        ),
        InfiniteGenreStation(
            id = "station_blues",
            title = "Blues & Jazz",
            subtitle = "Guitar Heroes, Blues Rock & Soul",
            icon = "🎷",
            gradientStart = 0xFF3E2723,
            gradientEnd = 0xFFFF6D00,
            localPlaylistIds = listOf("pl_guitar_heroes"),
            seedArtists = listOf(
                "Jimi Hendrix", "Stevie Ray Vaughan", "Eric Clapton", "BB King", "Gary Moore",
                "Pappo's Blues", "Pappo", "Buddy Guy", "John Mayer", "Joe Bonamassa",
                "Muddy Waters", "Carlos Santana", "Albert King", "Rory Gallagher"
            )
        ),
        InfiniteGenreStation(
            id = "station_electro",
            title = "Electrónica",
            subtitle = "House, EDM, Synthwave & Techno",
            icon = "🎛️",
            gradientStart = 0xFF01579B,
            gradientEnd = 0xFF76FF03,
            localPlaylistIds = listOf("pl_pop_hits"),
            seedArtists = listOf(
                "Daft Punk", "Avicii", "Calvin Harris", "David Guetta", "Tiësto",
                "Swedish House Mafia", "The Chemical Brothers", "Fatboy Slim", "Martin Garrix", "Kavinsky"
            )
        ),
        InfiniteGenreStation(
            id = "station_rap",
            title = "Hip Hop & Rap",
            subtitle = "Old School, 90s Boom Bap & Trap",
            icon = "🎤",
            gradientStart = 0xFF212121,
            gradientEnd = 0xFFFFD600,
            localPlaylistIds = listOf("pl_2000s"),
            seedArtists = listOf(
                "Eminem", "Tupac", "Notorious BIG", "Snoop Dogg", "Dr Dre",
                "50 Cent", "Kendrick Lamar", "Kanye West", "Outkast", "Wu-Tang Clan", "Cypress Hill"
            )
        ),
        InfiniteGenreStation(
            id = "station_acustico",
            title = "Acústicos & Relax",
            subtitle = "MTV Unplugged, Baladas Rock & Chill",
            icon = "🌧️",
            gradientStart = 0xFF263238,
            gradientEnd = 0xFF80CBC4,
            localPlaylistIds = listOf("pl_acustico"),
            seedArtists = listOf(
                "Nirvana Unplugged", "Alice in Chains Unplugged", "Eric Clapton Unplugged",
                "Soda Stereo Comfort", "Spinetta", "Charly García", "Divididos Acústico",
                "Catupecu Machu Acústico", "Pearl Jam", "Oasis", "Radiohead"
            )
        )
    )

    val categories: List<MusicCategory> = listOf(
        MusicCategory("rock_clasico", "Rock Clásico", "⚡", 0xFFFF5722, 0xFFFF9800, "Classic Rock 70s 80s greatest hits"),
        MusicCategory("metal", "Hard Rock & Metal", "🤘", 0xFFE91E63, 0xFF9C27B0, "Heavy Metal Hard Rock essentials"),
        MusicCategory("grunge", "Grunge & 90s", "🌧️", 0xFF00BCD4, 0xFF3F51B5, "Grunge 90s alternative rock"),
        MusicCategory("nacional", "Rock Nacional", "🇦🇷", 0xFF03A9F4, 0xFFE0E0E0, "Rock Nacional Argentino clasicos"),
        MusicCategory("metal_extremo", "Metal Extremo & Pesado", "💀", 0xFF795548, 0xFF212121, "Death Metal Black Metal Argentine Metal"),
        MusicCategory("pop", "Pop & Hits", "🌟", 0xFFFF4081, 0xFF7C4DFF, "Pop Hits 80s 90s 2000s top charts"),
        MusicCategory("fiesta", "Fiesta & Cumbia", "🌴", 0xFFFFC107, 0xFFFF5722, "Cumbia Cuarteto Fiesta Argentina"),
        MusicCategory("punk", "Punk & 2000s", "🧷", 0xFFFF9800, 0xFFF44336, "Punk Rock 2000s alternative anthems"),
        MusicCategory("acustico", "Acústico & Baladas", "☕", 0xFF8D6E63, 0xFFFFB74D, "Acoustic Rock unplugged ballads"),
        MusicCategory("blues", "Guitar Heroes & Blues", "🎷", 0xFF673AB7, 0xFF009688, "Blues Rock electric guitar legends")
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
        MusicArtist("rhcp", "Red Hot Chili Peppers", "Funk Rock", "https://i.scdn.co/image/ab6761610000e5eb1e792b0c4eb5e7c8b0568019", "Red Hot Chili Peppers greatest hits"),
        MusicArtist("redondos", "Los Redondos", "Rock Nacional", "https://i.scdn.co/image/ab6761610000e5eb981610582093e034da465e92", "Patricio Rey Redonditos de Ricota exitos"),
        MusicArtist("maiden", "Iron Maiden", "Heavy Metal", "https://i.scdn.co/image/ab6761610000e5eb04d2e5bf590fb05fa4f971c2", "Iron Maiden best tracks"),
        MusicArtist("hermetica", "Hermética & Almafuerte", "Metal Pesado Argentino", "https://i.scdn.co/image/ab6761610000e5eb27a2ea3295c6c06a644c3561", "Hermetica Almafuerte canciones"),
        MusicArtist("palmeras", "Los Palmeras", "Cumbia Santafesina", "https://i.scdn.co/image/ab6761610000e5eb9d04f26042ce92da07a7e80d", "Los Palmeras grandes exitos"),
        MusicArtist("mj", "Michael Jackson", "Pop King", "https://i.scdn.co/image/ab6761610000e5eb0a980753a8cb7194625b5971", "Michael Jackson hits"),
        MusicArtist("pearljam", "Pearl Jam", "Grunge", "https://i.scdn.co/image/ab6761610000e5ebd4b3d87212450b29efb61ec0", "Pearl Jam live")
    )

    val playlists: List<MusicPlaylist> = listOf(
        // 1. LOS MEJORES RIFFS DE LA HISTORIA (52 Canciones)
        MusicPlaylist(
            id = "pl_riffs",
            title = "Los Mejores Riffs de la Historia",
            subtitle = "Led Zeppelin, AC/DC, Deep Purple, Hendrix, Floyd & más (52 temas)",
            badge = "52 TEMAS",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            tracks = listOf(
                track("qK7j_EERgT4", "Smoke on the Water", "Deep Purple", "Machine Head", "5:40"),
                track("pAgnJDJN4VA", "Back in Black", "AC/DC", "Back in Black", "4:15"),
                track("l482T0yNkeo", "Highway to Hell", "AC/DC", "Highway to Hell", "3:28"),
                track("v2AC41dglnM", "Thunderstruck", "AC/DC", "The Razors Edge", "4:52"),
                track("3G6oLyWohUg", "You Shook Me All Night Long", "AC/DC", "Back in Black", "3:30"),
                track("HibBnC6SVk8", "Whole Lotta Love", "Led Zeppelin", "Led Zeppelin II", "5:34"),
                track("QkF3oxziUI4", "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "8:02"),
                track("2KPEHohJMuw", "Black Dog", "Led Zeppelin", "Led Zeppelin IV", "4:56"),
                track("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "5:55"),
                track("-tJYN-eG1zk", "We Will Rock You", "Queen", "News of the World", "2:01"),
                track("rY0WxgSXdEE", "Another One Bites the Dust", "Queen", "The Game", "3:35"),
                track("1w7OgIMMRc4", "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", "5:03"),
                track("o1tj2zJ2Wvg", "Welcome to the Jungle", "Guns N' Roses", "Appetite for Destruction", "4:33"),
                track("8SbUC-UaAxE", "November Rain", "Guns N' Roses", "Use Your Illusion I", "8:57"),
                track("LwfMqgpZe5U", "Paradise City", "Guns N' Roses", "Appetite for Destruction", "6:49"),
                track("CD-E-LDc384", "Enter Sandman", "Metallica", "Metallica", "5:31"),
                track("0qanF-91aJo", "Paranoid", "Black Sabbath", "Paranoid", "2:48"),
                track("b3-QqGVt-tM", "Iron Man", "Black Sabbath", "Paranoid", "5:56"),
                track("LQUXuQ6Zd9w", "War Pigs", "Black Sabbath", "Paranoid", "7:55"),
                track("WGoDaYjdfSg", "Purple Haze", "Jimi Hendrix", "Are You Experienced", "2:51"),
                track("L7UMubmfbH0", "Voodoo Child", "Jimi Hendrix", "Electric Ladyland", "5:12"),
                track("TLV4_xaYynY", "All Along the Watchtower", "Jimi Hendrix", "Electric Ladyland", "4:00"),
                track("nrIPxlFzDi0", "(I Can't Get No) Satisfaction", "The Rolling Stones", "Out of Our Heads", "3:43"),
                track("O4irXQhgMqg", "Paint It, Black", "The Rolling Stones", "Aftermath", "3:22"),
                track("SGyOaCXr8Lw", "Start Me Up", "The Rolling Stones", "Tattoo You", "3:33"),
                track("SHhrZgojY1Q", "Won't Get Fooled Again", "The Who", "Who's Next", "8:32"),
                track("x2KRpRMSu4g", "Baba O'Riley", "The Who", "Who's Next", "5:00"),
                track("f3y8jf01UY8", "Sunshine of Your Love", "Cream", "Disraeli Gears", "4:10"),
                track("uSquiIVLhrQ", "Layla", "Derek and the Dominos", "Layla", "7:05"),
                track("4B_UYYPb-Gk", "Walk This Way", "Aerosmith", "Toys in the Attic", "3:40"),
                track("89dGC8de0CA", "Dream On", "Aerosmith", "Aerosmith", "4:28"),
                track("SwYN7mTi6HM", "Jump", "Van Halen", "1984", "4:04"),
                track("fuKDBPw8wQA", "Panama", "Van Halen", "1984", "3:32"),
                track("qtwBFz6lfrY", "Ain't Talkin' 'Bout Love", "Van Halen", "Van Halen", "3:50"),
                track("Wr9ie2J2690", "Highway Star", "Deep Purple", "Machine Head", "6:05"),
                track("LCnebZnysmI", "Burn", "Deep Purple", "Burn", "6:00"),
                track("Djrl6fu8myo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
                track("L397TWLwrUU", "Breaking the Law", "Judas Priest", "British Steel", "2:35"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("rG6b8gjMEkw", "La Grange", "ZZ Top", "Tres Hombres", "3:52"),
                track("7wRHBLwpASw", "Sharp Dressed Man", "ZZ Top", "Eliminator", "4:14"),
                track("0vo23H9J8o8", "Pride and Joy", "Stevie Ray Vaughan", "Texas Flood", "3:40"),
                track("89Qg_gYqkys", "Sultans of Swing", "Dire Straits", "Dire Straits", "5:47"),
                track("wTP2RUD_cL0", "Money for Nothing", "Dire Straits", "Brothers in Arms", "4:38"),
                track("_FrOQC-zEog", "Comfortably Numb", "Pink Floyd", "The Wall", "6:23"),
                track("IXdNnw99-Ic", "Wish You Were Here", "Pink Floyd", "Wish You Were Here", "5:34"),
                track("t4QK8RxCAwo", "More Than a Feeling", "Boston", "Boston", "4:45"),
                track("2X_2IdybTV0", "Carry On Wayward Son", "Kansas", "Leftoverture", "5:23"),
                track("ClQcUyhoxTg", "(Don't Fear) The Reaper", "Blue Öyster Cult", "Agents of Fortune", "5:08"),
                track("ye5BuYf8q4o", "Sweet Home Alabama", "Lynyrd Skynyrd", "Second Helping", "4:44"),
                track("auLBLk4ibAk", "Tom Sawyer", "Rush", "Moving Pictures", "4:34")
            )
        ),

        // 2. HIMNOS DEL GRUNGE & 90s (50 Canciones)
        MusicPlaylist(
            id = "pl_grunge",
            title = "Himnos del Grunge & 90s",
            subtitle = "Nirvana, Pearl Jam, Soundgarden, Alice In Chains & más (50 temas)",
            badge = "50 TEMAS",
            coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80",
            tracks = listOf(
                track("hTWKbfoikeg", "Smells Like Teen Spirit", "Nirvana", "Nevermind", "5:01"),
                track("vabnZ9-ex7o", "Come as You Are", "Nirvana", "Nevermind", "3:39"),
                track("n6P0SitRwy8", "Heart-Shaped Box", "Nirvana", "In Utero", "4:41"),
                track("pkcJEvMcnEg", "Lithium", "Nirvana", "Nevermind", "4:17"),
                track("PbgKEjNBHqM", "In Bloom", "Nirvana", "Nevermind", "4:15"),
                track("JIx2H-plXdU", "About a Girl", "Nirvana", "Bleach", "2:48"),
                track("wGiTPgvKktM", "Alive", "Pearl Jam", "Ten", "5:41"),
                track("CxKWTzr-k6s", "Even Flow", "Pearl Jam", "Ten", "4:53"),
                track("bQtPzo-7AHs", "Jeremy", "Pearl Jam", "Ten", "5:18"),
                track("4q9UafsiQ6k", "Black", "Pearl Jam", "Ten", "5:43"),
                track("nDK-wsdEhNE", "Better Man", "Pearl Jam", "Vitalogy", "4:28"),
                track("3mbBbFH9fAg", "Black Hole Sun", "Soundgarden", "Superunknown", "5:18"),
                track("5O14N7PQAsM", "Spoonman", "Soundgarden", "Superunknown", "4:06"),
                track("ySzrJ4GRF7s", "Fell on Black Days", "Soundgarden", "Superunknown", "4:42"),
                track("sNh-iw7gsuI", "Outshined", "Soundgarden", "Badmotorfinger", "5:11"),
                track("pBZs_Py-1_0", "Rusty Cage", "Soundgarden", "Badmotorfinger", "4:26"),
                track("TAqZb52sgpU", "Man in the Box", "Alice In Chains", "Facelift", "4:44"),
                track("EiZR4xyFK6Q", "Would?", "Alice In Chains", "Dirt", "3:27"),
                track("uAE6Il6OTcs", "Rooster", "Alice In Chains", "Dirt", "6:15"),
                track("zTuD8k3JvxQ", "Them Bones", "Alice In Chains", "Dirt", "2:30"),
                track("G23iLGhh9lo", "Nutshell", "Alice In Chains", "Jar of Flies", "4:19"),
                track("f8hT3oDDf6c", "Down in a Hole", "Alice In Chains", "Dirt", "5:38"),
                track("V5UOC0C0x8Q", "Plush", "Stone Temple Pilots", "Core", "5:13"),
                track("FKTjssfkL4I", "Creep", "Stone Temple Pilots", "Core", "5:33"),
                track("yjJL9DGU7Gg", "Interstate Love Song", "Stone Temple Pilots", "Purple", "3:14"),
                track("ht672-wYelc", "Vasoline", "Stone Temple Pilots", "Purple", "2:56"),
                track("lnLuvaSXfig", "Wicked Garden", "Stone Temple Pilots", "Core", "4:05"),
                track("Lr58WHo2ndM", "1979", "The Smashing Pumpkins", "Mellon Collie", "4:24"),
                track("ZYRbPcjVtJg", "Bullet with Butterfly Wings", "The Smashing Pumpkins", "Mellon Collie", "4:18"),
                track("q-KE9lvU810", "Cherub Rock", "The Smashing Pumpkins", "Siamese Dream", "4:58"),
                track("NOG3eus4ZSo", "Tonight, Tonight", "The Smashing Pumpkins", "Mellon Collie", "4:15"),
                track("d1acEVmnVhI", "Disarm", "The Smashing Pumpkins", "Siamese Dream", "3:17"),
                track("RZD982yrmx4", "Tomorrow", "Silverchair", "Frogstomp", "4:26"),
                track("nzf4I0taEp4", "Freak", "Silverchair", "Freak Show", "3:49"),
                track("uMSTIePQri0", "Pure Massacre", "Silverchair", "Frogstomp", "4:58"),
                track("VUb450Alpps", "Hunger Strike", "Temple of the Dog", "Temple of the Dog", "4:03"),
                track("dwXBfbCfbro", "Say Hello 2 Heaven", "Temple of the Dog", "Temple of the Dog", "6:22"),
                track("Fm72DPJCX58", "River of Deceit", "Mad Season", "Above", "5:04"),
                track("rWoG7jShUck", "Nearly Lost You", "Screaming Trees", "Sweet Oblivion", "4:07"),
                track("iY7LLYCmkpU", "Glycerine", "Bush", "Sixteen Stone", "4:26"),
                track("5WPbqYoz9HA", "Machinehead", "Bush", "Sixteen Stone", "4:16"),
                track("cPlgoNY2xqw", "Comedown", "Bush", "Sixteen Stone", "5:26"),
                track("hq0rZ3IiyWw", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("I7rCNiiNPxA", "Monkey Wrench", "Foo Fighters", "The Colour and the Shape", "3:51"),
                track("c7RDwh2pIEY", "My Hero", "Foo Fighters", "The Colour and the Shape", "4:20"),
                track("O3dWBLoU--E", "Celebrity Skin", "Hole", "Celebrity Skin", "2:42"),
                track("yik_Lkuz-fo", "Jane Says", "Jane's Addiction", "Nothing's Shocking", "4:52"),
                track("3qVPNONdF58", "No Rain", "Blind Melon", "Blind Melon", "3:37"),
                track("_m0bI82Rz_k", "Shine", "Collective Soul", "Hints Allegations and Things Left Unsaid", "5:05"),
                track("6Ejga4kJUts", "Zombie", "The Cranberries", "No Need to Argue", "5:07")
            )
        ),

        // 3. LEYENDAS DEL ROCK ARGENTINO (55 Canciones)
        MusicPlaylist(
            id = "pl_nacional",
            title = "Leyendas del Rock Argentino",
            subtitle = "Soda Stereo, Charly García, Los Redondos, Spinetta & más (55 temas)",
            badge = "55 TEMAS",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            tracks = listOf(
                track("sWft4cMiwXQ", "De Música Ligera", "Soda Stereo", "Canción Animal", "3:32"),
                track("RdVc6LEEIL0", "Persiana Americana", "Soda Stereo", "Signos", "4:52"),
                track("dZQur2gDGWk", "En la Ciudad de la Furia", "Soda Stereo", "Doble Vida", "5:45"),
                track("UEchfRz0D8Q", "Cuando Pase el Temblor", "Soda Stereo", "Nada Personal", "3:49"),
                track("lGptyREttJ4", "Prófugos", "Soda Stereo", "Signos", "5:20"),
                track("UPVfQKUHTSc", "Trátame Suavemente", "Soda Stereo", "Soda Stereo", "3:22"),
                track("WOYxCRfneRI", "Ella Usó Mi Cabeza Como un Revólver", "Soda Stereo", "Sueño Stereo", "4:31"),
                track("ko24BZXdK8c", "Nada Personal", "Soda Stereo", "Nada Personal", "4:52"),
                track("CEb99v79yRM", "Signos", "Soda Stereo", "Signos", "5:15"),
                track("P4WfOLmrFxA", "Un Misil en Mi Placard", "Soda Stereo", "Comfort y Música Para Volar", "4:34"),
                track("DeMCz0O7-FM", "Demoliendo Hoteles", "Charly García", "Piano Bar", "2:18"),
                track("jzO8x1pnbCk", "Nos Siguen Pegando Abajo", "Charly García", "Clics Modernos", "3:30"),
                track("WXIfT-dQtCo", "Promesas Sobre el Bidet", "Charly García", "Piano Bar", "2:46"),
                track("ldgUatPszek", "Yendo de la Cama al Living", "Charly García", "Yendo de la Cama al Living", "4:42"),
                track("0o2WK0Q_ooQ", "Cerca de la Revolución", "Charly García", "Piano Bar", "4:42"),
                track("Z7AERldCALc", "Hablando a tu Corazón", "Charly García", "Tango", "2:14"),
                track("iTA75Z6E4eA", "Rezo por Vos", "Charly García & Luis Alberto Spinetta", "Privé", "4:30"),
                track("P3L10KNMptw", "No Voy en Tren", "Charly García", "Parte de la Religión", "3:03"),
                track("u4viftk6vBw", "Fanky", "Charly García", "Cómo Conseguir Chicas", "4:45"),
                track("CnJqYsSOgfg", "Ji Ji Ji", "Patricio Rey y sus Redonditos de Ricota", "Oktubre", "5:32"),
                track("PAq7xAqXTxk", "La Bestia Pop", "Patricio Rey y sus Redonditos de Ricota", "Gulp!", "3:42"),
                track("WlFLpKaC4_c", "Un Poco de Amor Francés", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "3:27"),
                track("U6lI8FkG8UI", "Juguetes Perdidos", "Patricio Rey y sus Redonditos de Ricota", "Luzbelito", "7:10"),
                track("uEWQmm3qYt0", "Queso Ruso", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "4:52"),
                track("84DBTr67OnY", "Mi Perro Dinamita", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "2:35"),
                track("DRf5Nzgq3Lk", "El Pibe de los Astilleros", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "3:32"),
                track("e-Vi5uugYtY", "Ala Delta", "Divididos", "Acariciando lo Áspero", "4:12"),
                track("obswq_in8Z4", "El 38", "Divididos", "Acariciando lo Áspero", "2:31"),
                track("Ma-r9hq-dsU", "Spaghetti del Rock", "Divididos", "Narigón del Siglo", "3:32"),
                track("n_t8_OO58GM", "¿Qué Ves?", "Divididos", "La Era de la Boludez", "5:14"),
                track("XqT0lCJzUV8", "Salir a Asustar", "Divididos", "La Era de la Boludez", "3:21"),
                track("nWvA8ThFH8M", "La Rubia Tarada", "Sumo", "Divididos por la Felicidad", "3:40"),
                track("P7M9U0D06iI", "Mejor No Hablar de Ciertas Cosas", "Sumo", "Divididos por la Felicidad", "4:42"),
                track("v-uUfI8IVyw", "Los Viejos Vinagres", "Sumo", "Llegando los Monos", "3:15"),
                track("zrXYrql9qyY", "Mañana en el Abasto", "Sumo", "After Chabón", "4:08"),
                track("dfukoPRpQzA", "Será", "Las Pelotas", "Esperando el Milagro", "3:02"),
                track("JjOUCDICjUY", "Bombachitas Rosas", "Las Pelotas", "Máscaras de Sal", "3:15"),
                track("VC5NaNsR7-8", "La Balada del Diablo y la Muerte", "La Renga", "Despedazado por Mil Partes", "5:36"),
                track("_FTW5MdKw98", "El Revelde", "La Renga", "La Renga", "3:32"),
                track("3wsTxNusM4g", "Panic Show", "La Renga", "La Esquina del Infinito", "3:01"),
                track("AO5ZjpPReTs", "Verano del 92", "Los Piojos", "3er Arco", "4:40"),
                track("QmYBmoEVqbk", "El Farolito", "Los Piojos", "3er Arco", "4:14"),
                track("Z2KyHzgFJsk", "Bicho de Ciudad", "Los Piojos", "Civilización", "5:12"),
                track("Xv2vXP7R-KI", "Tan Solo", "Los Piojos", "Chactuchac", "4:00"),
                track("W47hOuQjDag", "Muchacha Ojos de Papel", "Almendra", "Almendra", "3:07"),
                track("1Zjm0uh8oeA", "Bajan", "Pescado Rabioso", "Artaud", "3:26"),
                track("BxucLDUu4bg", "Seguir Viviendo Sin Tu Amor", "Luis Alberto Spinetta", "Pelusón of Milk", "2:40"),
                track("h9AHQV3pPn8", "Seminare", "Serú Girán", "Serú Girán", "3:27"),
                track("qt11aXRwMiA", "Peperina", "Serú Girán", "Peperina", "3:40"),
                track("NAcDZfLiwss", "Mariposa Tecknicolor", "Fito Páez", "Circo Beat", "3:42"),
                track("glZA97qtUD8", "11 y 6", "Fito Páez", "Giros", "3:15"),
                track("EZaO0mgpy6I", "El Amor Después del Amor", "Fito Páez", "El Amor Después del Amor", "5:10"),
                track("j6CUfl4aX9s", "Flaca", "Andrés Calamaro", "Alta Suciedad", "4:47"),
                track("2lVHb_ypdKI", "Mil Horas", "Los Abuelos de la Nada", "Vasos y Besos", "2:50"),
                track("pK0qzW17EXw", "Lamento Boliviano", "Los Enanitos Verdes", "Big Bang", "3:42")
            )
        ),

        // 4. HEAVY METAL & THRASH ESSENTIALS (52 Canciones)
        MusicPlaylist(
            id = "pl_metal",
            title = "Heavy Metal & Thrash Essentials",
            subtitle = "Metallica, Maiden, Megadeth, Pantera, Ozzy & más (52 temas)",
            badge = "52 TEMAS",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            tracks = listOf(
                track("2f1Ny74_ou0", "Master of Puppets", "Metallica", "Master of Puppets", "8:35"),
                track("CD-E-LDc384", "Enter Sandman", "Metallica", "Metallica", "5:31"),
                track("WM8bTdBs-cw", "One", "Metallica", "...And Justice for All", "7:24"),
                track("WEQnzs8wl6E", "Fade to Black", "Metallica", "Ride the Lightning", "6:56"),
                track("tAGnKpE4NCI", "Nothing Else Matters", "Metallica", "Metallica", "6:28"),
                track("B_HSa1dEL9s", "For Whom the Bell Tolls", "Metallica", "Ride the Lightning", "5:10"),
                track("FLTchCiC0T0", "Seek & Destroy", "Metallica", "Kill 'Em All", "6:55"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("p32b5nNq1zw", "Fear of the Dark", "Iron Maiden", "Fear of the Dark", "7:18"),
                track("fyr79sB6nGg", "The Number of the Beast", "Iron Maiden", "The Number of the Beast", "4:50"),
                track("hCtTN4J_OeM", "Hallowed Be Thy Name", "Iron Maiden", "The Number of the Beast", "7:12"),
                track("86URGgqONvA", "Run to the Hills", "Iron Maiden", "The Number of the Beast", "3:53"),
                track("Ij99dud8-0A", "Wasted Years", "Iron Maiden", "Somewhere in Time", "5:08"),
                track("9d4ui9q7eDM", "Holy Wars... The Punishment Due", "Megadeth", "Rust in Peace", "6:32"),
                track("wEPKIulCEhQ", "Symphony of Destruction", "Megadeth", "Countdown to Extinction", "4:02"),
                track("LVhJy-CR64Q", "Peace Sells", "Megadeth", "Peace Sells... But Who's Buying?", "4:02"),
                track("x5A9Aa-WU5E", "Hangar 18", "Megadeth", "Rust in Peace", "5:14"),
                track("L397TWLwrUU", "Painkiller", "Judas Priest", "Painkiller", "6:06"),
                track("L397TWLwrUU", "Breaking the Law", "Judas Priest", "British Steel", "2:35"),
                track("eGtwgYt_QnA", "Electric Eye", "Judas Priest", "Screaming for Vengeance", "3:39"),
                track("XJMgveYdO-M", "You've Got Another Thing Comin'", "Judas Priest", "Screaming for Vengeance", "5:09"),
                track("AkFqg5wAuFk", "Walk", "Pantera", "Vulgar Display of Power", "5:15"),
                track("Um6wZvfjZcM", "Cowboys from Hell", "Pantera", "Cowboys from Hell", "4:06"),
                track("cSZf8ZYHV7o", "Cemetery Gates", "Pantera", "Cowboys from Hell", "7:03"),
                track("7m7njvwB-Ks", "5 Minutes Alone", "Pantera", "Far Beyond Driven", "5:50"),
                track("0qanF-91aJo", "Paranoid", "Black Sabbath", "Paranoid", "2:48"),
                track("b3-QqGVt-tM", "Iron Man", "Black Sabbath", "Paranoid", "5:56"),
                track("LQUXuQ6Zd9w", "War Pigs", "Black Sabbath", "Paranoid", "7:55"),
                track("Djrl6fu8myo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
                track("G3LvhdFEOqs", "Mr. Crowley", "Ozzy Osbourne", "Blizzard of Ozz", "5:02"),
                track("45RSG9oqt6I", "Bark at the Moon", "Ozzy Osbourne", "Bark at the Moon", "4:17"),
                track("z8ZqFlw6hYg", "Raining Blood", "Slayer", "Reign in Blood", "4:14"),
                track("K6_zsJ8KPP0", "Angel of Death", "Slayer", "Reign in Blood", "4:51"),
                track("74nTzbgDGWM", "South of Heaven", "Slayer", "South of Heaven", "4:59"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("NRzuex6I6lI", "Overkill", "Motörhead", "Overkill", "5:12"),
                track("uGHsxMqpL0c", "Madhouse", "Anthrax", "Spreading the Disease", "4:20"),
                track("XphUURIAx5g", "Caught in a Mosh", "Anthrax", "Among the Living", "4:59"),
                track("EhGEGIBGLu8", "Holy Diver", "Dio", "Holy Diver", "4:16"),
                track("nNDGsDwUaXo", "Rainbow in the Dark", "Dio", "Holy Diver", "4:15"),
                track("8Sqh8Twcp2g", "Balls to the Wall", "Accept", "Balls to the Wall", "5:43"),
                track("nUCodt4zVw4", "Roots Bloody Roots", "Sepultura", "Roots", "3:32"),
                track("6ODNxy3YOPU", "Refuse/Resist", "Sepultura", "Chaos A.D.", "3:19"),
                track("6yP1tcy9a10", "Rock You Like a Hurricane", "Scorpions", "Love at First Sting", "4:12"),
                track("KMhL9NZ0Kdw", "I Want Out", "Helloween", "Keeper of the Seven Keys Part II", "4:40"),
                track("GK-DkHHAMIU", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("iywaBOMvYLI", "Toxicity", "System of a Down", "Toxicity", "3:38"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("3GWwohI5h1w", "Duality", "Slipknot", "Vol. 3: (The Subliminal Verses)", "4:12"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("aEx8XyOvmnM", "Down with the Sickness", "Disturbed", "The Sickness", "4:38")
            )
        ),

        // 5. MTV GENERACIÓN 90s & 2000s (50 Canciones)
        MusicPlaylist(
            id = "pl_2000s",
            title = "MTV Generación 90s & 2000s",
            subtitle = "Linkin Park, Green Day, Blink-182, Foo Fighters & más (50 temas)",
            badge = "50 TEMAS",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
            tracks = listOf(
                track("eVTXPUF4Oz4", "In the End", "Linkin Park", "Hybrid Theory", "3:36"),
                track("kXYiU_JCYtU", "Numb", "Linkin Park", "Meteora", "3:07"),
                track("LYU-8IFcDPw", "Faint", "Linkin Park", "Meteora", "2:42"),
                track("vZ7nTHxWyOk", "Crawling", "Linkin Park", "Hybrid Theory", "3:29"),
                track("hq0rZ3IiyWw", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("9jK-NcRmVcw", "The Pretender", "Foo Fighters", "Echoes, Silence, Patience & Grace", "4:30"),
                track("-q2zI0J9zj8", "Best of You", "Foo Fighters", "In Your Honor", "4:16"),
                track("MOQV__Z55eE", "Learn to Fly", "Foo Fighters", "There Is Nothing Left to Lose", "3:55"),
                track("AJDiYxKSAqQ", "Basket Case", "Green Day", "Dookie", "3:01"),
                track("Soa3gO7tL-c", "Boulevard of Broken Dreams", "Green Day", "American Idiot", "4:21"),
                track("kqkGY9jsBOw", "American Idiot", "Green Day", "American Idiot", "2:54"),
                track("pGhwBFYtn1s", "Wake Me Up When September Ends", "Green Day", "American Idiot", "4:45"),
                track("98W9QuMq-2k", "All the Small Things", "Blink-182", "Enema of the State", "2:47"),
                track("K7l5ZeVVoCA", "What's My Age Again?", "Blink-182", "Enema of the State", "2:28"),
                track("s1tAYmMjLdY", "I Miss You", "Blink-182", "Blink-182", "3:47"),
                track("vVy9Lgpg1m8", "First Date", "Blink-182", "Take Off Your Pants and Jacket", "2:51"),
                track("1KP_nPltW9s", "The Kids Aren't Alright", "The Offspring", "Americana", "3:00"),
                track("EtNZnhxWLHo", "Self Esteem", "The Offspring", "Smash", "4:17"),
                track("cFnAt_I9STw", "Pretty Fly (For a White Guy)", "The Offspring", "Americana", "3:08"),
                track("_pmZKkb_UAg", "You're Gonna Go Far, Kid", "The Offspring", "Rise and Fall, Rage and Grace", "2:57"),
                track("YlUKcNNmywk", "Californication", "Red Hot Chili Peppers", "Californication", "5:21"),
                track("rn_YodiJO6k", "Otherside", "Red Hot Chili Peppers", "Californication", "4:15"),
                track("8DyziWtkfBw", "Can't Stop", "Red Hot Chili Peppers", "By the Way", "4:29"),
                track("mzJj5-lubeM", "Scar Tissue", "Red Hot Chili Peppers", "Californication", "3:37"),
                track("gGdGFtwCNBE", "Mr. Brightside", "The Killers", "Hot Fuss", "3:42"),
                track("dxpKi9G_kxU", "Somebody Told Me", "The Killers", "Hot Fuss", "3:17"),
                track("w8KQmps-Sog", "Uprising", "Muse", "The Resistance", "5:03"),
                track("Xsp3_a-PMTw", "Supermassive Black Hole", "Muse", "Black Holes and Revelations", "3:29"),
                track("3dm_5qWWDV8", "Hysteria", "Muse", "Absolution", "3:47"),
                track("bpOSxM0rNPM", "Do I Wanna Know?", "Arctic Monkeys", "AM", "4:32"),
                track("CYpn8yUnX_c", "I Bet You Look Good on the Dancefloor", "Arctic Monkeys", "Whatever People Say I Am...", "2:53"),
                track("7QU1nvuxaMA", "Like a Stone", "Audioslave", "Audioslave", "4:54"),
                track("YymwGlbqzIc", "Cochise", "Audioslave", "Audioslave", "3:42"),
                track("3YxaaGgTQYM", "Bring Me to Life", "Evanescence", "Fallen", "3:57"),
                track("Hm7vnOC4hoY", "Last Resort", "Papa Roach", "Infest", "3:19"),
                track("RYnFIRc0k6E", "Rollin'", "Limp Bizkit", "Chocolate Starfish...", "3:33"),
                track("ZpUYjpKg9KY", "Break Stuff", "Limp Bizkit", "Significant Other", "2:46"),
                track("CMX2lPum_pg", "Fat Lip", "Sum 41", "All Killer No Filler", "2:58"),
                track("emGri7i8Y2Y", "In Too Deep", "Sum 41", "All Killer No Filler", "3:27"),
                track("desJKYvdq9A", "The Anthem", "Good Charlotte", "The Young and the Hopeless", "2:55"),
                track("RRKJiM9Njr8", "Welcome to the Black Parade", "My Chemical Romance", "The Black Parade", "5:11"),
                track("7SM5laxokpg", "Helena", "My Chemical Romance", "Three Cheers for Sweet Revenge", "3:22"),
                track("6rkLF0zxp7E", "Last Nite", "The Strokes", "Is This It", "3:13"),
                track("SsXQoDiQC-o", "Reptilia", "The Strokes", "Room on Fire", "3:39"),
                track("7dfBmjrN5W0", "Take Me Out", "Franz Ferdinand", "Franz Ferdinand", "3:57"),
                track("s88r_q7oufE", "No One Knows", "Queens of the Stone Age", "Songs for the Deaf", "4:38"),
                track("fgT9zGkiLig", "Drive", "Incubus", "Make Yourself", "3:52"),
                track("erG5rgNYSdk", "Island in the Sun", "Weezer", "Weezer (Green Album)", "3:20"),
                track("xPU8OAjjS4k", "Kryptonite", "3 Doors Down", "The Better Life", "3:53"),
                track("1cQh1ccqu8M", "How You Remind Me", "Nickelback", "Silver Side Up", "3:43")
            )
        ),

        // 6. METAL EXTREMO: DEATH, BLACK & MELODIC (50 Canciones)
        MusicPlaylist(
            id = "pl_metal_extremo",
            title = "Metal Extremo: Death & Black",
            subtitle = "Cannibal Corpse, Death, Behemoth, Amon Amarth, Dimmu Borgir & más (50 temas)",
            badge = "EXTREMO",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80",
            tracks = listOf(
                track("gNhN6lT-y5U", "Hammer Smashed Face", "Cannibal Corpse", "Tomb of the Mutilated", "4:04"),
                track("I1-EOJ_b0-Y", "Crystal Mountain", "Death", "Symbolic", "5:06"),
                track("PhQ1Fe0oKPM", "The Philosopher", "Death", "Individual Thought Patterns", "4:13"),
                track("0RK9jLlYvvo", "Blow Your Trumpets Gabriel", "Behemoth", "The Satanist", "4:25"),
                track("Wss3FegsHb8", "O Father O Satan O Sun!", "Behemoth", "The Satanist", "7:13"),
                track("edBYB1VCV0k", "Twilight of the Thunder God", "Amon Amarth", "Twilight of the Thunder God", "4:09"),
                track("55OJ17cHeJA", "The Way of Vikings", "Amon Amarth", "Jomsviking", "5:11"),
                track("mw0pylT0Tg0", "The Pursuit of Vikings", "Amon Amarth", "Fate of Norns", "4:30"),
                track("QB68Gt52BY4", "Progenies of the Great Apocalypse", "Dimmu Borgir", "Death Cult Armageddon", "5:17"),
                track("U9pLV9kG3-s", "Mourning Palace", "Dimmu Borgir", "Enthrone Darkness Triumphant", "5:13"),
                track("zWgYk3QQAQA", "Nemesis", "Arch Enemy", "Doomsday Machine", "4:12"),
                track("mjF1rmSV1dM", "The Eagle Flies Alone", "Arch Enemy", "Will to Power", "4:59"),
                track("InRMwptcgAo", "War Eternal", "Arch Enemy", "War Eternal", "4:16"),
                track("0nx9WwMskgs", "Needled 24/7", "Children of Bodom", "Hate Crew Deathroll", "4:08"),
                track("77kTMdl5zrQ", "In Your Face", "Children of Bodom", "Are You Dead Yet?", "4:16"),
                track("8DxpYENeBII", "Are You Dead Yet?", "Children of Bodom", "Are You Dead Yet?", "3:54"),
                track("JW5vsXIezvI", "Nymphetamine Fix", "Cradle of Filth", "Nymphetamine", "5:02"),
                track("lVlkl2kadoE", "Her Ghost in the Fog", "Cradle of Filth", "Midian", "6:24"),
                track("MDBykpSXsSE", "Ghost of Perdition", "Opeth", "Ghost Reveries", "10:29"),
                track("FNdC_3LR2AI", "Stranded", "Gojira", "Magma", "4:29"),
                track("oqdZpxkzNvc", "Redneck", "Lamb of God", "Sacrament", "3:41"),
                track("-oVW8gcse4g", "Laid to Rest", "Lamb of God", "Ashes of the Wake", "3:50"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("GK-DkHHAMIU", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("6ODNxy3YOPU", "Refuse/Resist", "Sepultura", "Chaos A.D.", "3:19"),
                track("nUCodt4zVw4", "Roots Bloody Roots", "Sepultura", "Roots", "3:32"),
                track("K6_zsJ8KPP0", "Angel of Death", "Slayer", "Reign in Blood", "4:51"),
                track("z8ZqFlw6hYg", "Raining Blood", "Slayer", "Reign in Blood", "4:14"),
                track("74nTzbgDGWM", "South of Heaven", "Slayer", "South of Heaven", "4:59"),
                track("L397TWLwrUU", "Painkiller", "Judas Priest", "Painkiller", "6:06"),
                track("AkFqg5wAuFk", "Walk", "Pantera", "Vulgar Display of Power", "5:15"),
                track("Um6wZvfjZcM", "Cowboys from Hell", "Pantera", "Cowboys from Hell", "4:06"),
                track("9d4ui9q7eDM", "Holy Wars... The Punishment Due", "Megadeth", "Rust in Peace", "6:32"),
                track("2f1Ny74_ou0", "Master of Puppets", "Metallica", "Master of Puppets", "8:35"),
                track("8Sqh8Twcp2g", "Balls to the Wall", "Accept", "Balls to the Wall", "5:43"),
                track("KMhL9NZ0Kdw", "I Want Out", "Helloween", "Keeper of the Seven Keys Part II", "4:40"),
                track("EhGEGIBGLu8", "Holy Diver", "Dio", "Holy Diver", "4:16"),
                track("nNDGsDwUaXo", "Rainbow in the Dark", "Dio", "Holy Diver", "4:15"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("NRzuex6I6lI", "Overkill", "Motörhead", "Overkill", "5:12"),
                track("uGHsxMqpL0c", "Madhouse", "Anthrax", "Spreading the Disease", "4:20"),
                track("XphUURIAx5g", "Caught in a Mosh", "Anthrax", "Among the Living", "4:59"),
                track("aEx8XyOvmnM", "Down with the Sickness", "Disturbed", "The Sickness", "4:38"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("6yP1tcy9a10", "Rock You Like a Hurricane", "Scorpions", "Love at First Sting", "4:12"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("p32b5nNq1zw", "Fear of the Dark", "Iron Maiden", "Fear of the Dark", "7:18"),
                track("fyr79sB6nGg", "The Number of the Beast", "Iron Maiden", "The Number of the Beast", "4:50"),
                track("hCtTN4J_OeM", "Hallowed Be Thy Name", "Iron Maiden", "The Number of the Beast", "7:12")
            )
        ),

        // 7. METAL PESADO ARGENTINO (50 Canciones)
        MusicPlaylist(
            id = "pl_metal_nacional",
            title = "Metal Pesado Argentino",
            subtitle = "Hermética, Almafuerte, V8, Malón, Rata Blanca & más (50 temas)",
            badge = "METAL ARGENTO",
            coverUrl = "https://images.unsplash.com/photo-1549834185-bd9f078a5dfe?w=500&q=80",
            tracks = listOf(
                track("EuUfDUkWmRo", "Tu Eres Su Seguridad", "Hermética", "Hermética", "5:04"),
                track("_FTW5MdKw98", "Evitando el Ablande", "Hermética", "Ácido Argentino", "2:54"),
                track("3wsTxNusM4g", "Soy de la Esquina", "Hermética", "Víctimas del Vaciamiento", "3:06"),
                track("Xv2vXP7R-KI", "Del Camionero", "Hermética", "Ácido Argentino", "4:31"),
                track("AO5ZjpPReTs", "Memoria de Siglos", "Hermética", "Ácido Argentino", "5:45"),
                track("QmYBmoEVqbk", "Gil Trabajador", "Hermética", "Ácido Argentino", "3:10"),
                track("Z2KyHzgFJsk", "Atravesando Todo Límite", "Hermética", "Víctimas del Vaciamiento", "4:00"),
                track("NAcDZfLiwss", "Masa Anestesiada", "Hermética", "Hermética", "4:43"),
                track("glZA97qtUD8", "Sé Vos", "Almafuerte", "Mundo Guanaco", "4:03"),
                track("EZaO0mgpy6I", "El Pibe Tigre", "Almafuerte", "Mundo Guanaco", "4:28"),
                track("j6CUfl4aX9s", "A Vos Amigo", "Almafuerte", "Del Entorno", "4:50"),
                track("2lVHb_ypdKI", "Toro y Pampa", "Almafuerte", "Toro y Pampa", "3:40"),
                track("pK0qzW17EXw", "Almafuerte", "Almafuerte", "Almafuerte", "3:55"),
                track("Dcow-Jp3Ak4", "Destrucción", "V8", "Luchando por el Metal", "2:01"),
                track("th6QSI8PU7s", "Brigadas Metálicas", "V8", "Luchando por el Metal", "3:03"),
                track("fgXDHQm5eq4", "Luchando por el Metal", "V8", "Luchando por el Metal", "2:58"),
                track("XW4X7kHmhyU", "Cautivos del Sistema", "V8", "Luchando por el Metal", "3:40"),
                track("D_DAj1Dt3Ek", "Síntoma de la Infección", "Malón", "Espíritu Combativo", "4:05"),
                track("Btp4vN7xFzo", "Castigador por Herencia", "Malón", "Espíritu Combativo", "3:35"),
                track("pw4EXhVHKQ8", "Gato Negro", "Malón", "Espíritu Combativo", "3:20"),
                track("X72XPwSKcJ4", "Mujer Amante", "Rata Blanca", "Magos, Espadas y Rosas", "6:05"),
                track("by4EHmvME1c", "La Leyenda del Hada y el Mago", "Rata Blanca", "Magos, Espadas y Rosas", "5:24"),
                track("OsFfjr20tm4", "Guerrero del Arco Iris", "Rata Blanca", "Guerrero del Arco Iris", "5:03"),
                track("oaWRxm_5rEA", "Volviendo a Casa", "Rata Blanca", "El Camino del Fuego", "5:27"),
                track("rv8G30H5hzI", "Esperanza", "Horcas", "Vence", "4:15"),
                track("8Zdhan166z0", "Vencer", "Horcas", "Vence", "3:45"),
                track("9xua8KKPZME", "Fuego", "Horcas", "Eternos", "4:00"),
                track("RvZNGjAcme8", "Sacate la Mierda", "Carajo", "Carajo", "3:25"),
                track("u2Vvxngbn9U", "El Mar de las Almas", "Carajo", "El Mar de las Almas", "4:37"),
                track("B8LTbjnKC5w", "Luna Hueca", "Carajo", "Frente a Frente", "4:12"),
                track("taR5ENb3Q9w", "Triste", "Carajo", "Atrapasueños", "4:00"),
                track("T6Es4yNHEIM", "El Nuevo Camino del Hombre", "A.N.I.M.A.L.", "El Nuevo Camino del Hombre", "3:40"),
                track("YKITrSHdBwk", "Sólo por Ser Indios", "A.N.I.M.A.L.", "Poder Latino", "3:30"),
                track("I9eqN3N66eA", "Combatiente del Metal", "A.N.I.M.A.L.", "A.N.I.M.A.L. 6", "3:15"),
                track("oRI1MOo0C1A", "Tierra Negra", "Tren Loco", "Tierra Negra", "4:30"),
                track("gvDuXIb87j8", "Clase Trabajadora", "Tren Loco", "No Me Importa Morir", "3:40"),
                track("2FXqNNjqY84", "Se Extraña el Dolor", "O'Connor", "Hay un Lugar", "4:12"),
                track("yjecye6ydlI", "Caníbal", "O'Connor", "Dolor", "3:50"),
                track("mAr_QQ9uSFc", "Como Relámpago en la Oscuridad", "Logos", "La Industria del Poder", "4:20"),
                track("hYgmT91EXlY", "No Te Rindas", "Logos", "Generación Mutable", "3:45"),
                track("e-Vi5uugYtY", "Ala Delta", "Divididos", "Acariciando lo Áspero", "4:12"),
                track("obswq_in8Z4", "El 38", "Divididos", "Acariciando lo Áspero", "2:31"),
                track("VC5NaNsR7-8", "La Balada del Diablo y la Muerte", "La Renga", "Despedazado por Mil Partes", "5:36"),
                track("_FTW5MdKw98", "El Revelde", "La Renga", "La Renga", "3:32"),
                track("3wsTxNusM4g", "Panic Show", "La Renga", "La Esquina del Infinito", "3:01"),
                track("CnJqYsSOgfg", "Ji Ji Ji", "Patricio Rey y sus Redonditos de Ricota", "Oktubre", "5:32"),
                track("U6lI8FkG8UI", "Juguetes Perdidos", "Patricio Rey y sus Redonditos de Ricota", "Luzbelito", "7:10"),
                track("sWft4cMiwXQ", "De Música Ligera", "Soda Stereo", "Canción Animal", "3:32"),
                track("DeMCz0O7-FM", "Demoliendo Hoteles", "Charly García", "Piano Bar", "2:18"),
                track("nWvA8ThFH8M", "La Rubia Tarada", "Sumo", "Divididos por la Felicidad", "3:40")
            )
        ),

        // 8. POP GLOBAL & GRANDES HITS (50 Canciones)
        MusicPlaylist(
            id = "pl_pop_hits",
            title = "Pop Global & Grandes Hits",
            subtitle = "Michael Jackson, Madonna, Dua Lipa, The Weeknd, Bruno Mars & más (50 temas)",
            badge = "POP HITS",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            tracks = listOf(
                track("Zi_XLOBDo_Y", "Billie Jean", "Michael Jackson", "Thriller", "4:54"),
                track("oRdxUFDoQe0", "Beat It", "Michael Jackson", "Thriller", "4:18"),
                track("sOnqjkJTMaA", "Thriller", "Michael Jackson", "Thriller", "5:58"),
                track("tOgftEf7UDg", "Smooth Criminal", "Michael Jackson", "Bad", "4:17"),
                track("79fzeNUqQbQ", "Like a Prayer", "Madonna", "Like a Prayer", "5:41"),
                track("QJ1LOC_4n0g", "Vogue", "Madonna", "I'm Breathless", "4:50"),
                track("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "After Hours", "3:20"),
                track("XXYlFuWEuKI", "Save Your Tears", "The Weeknd", "After Hours", "3:36"),
                track("oygrmJFKYZY", "Don't Start Now", "Dua Lipa", "Future Nostalgia", "3:03"),
                track("TUVcZfQe-Kw", "Levitating", "Dua Lipa", "Future Nostalgia", "3:23"),
                track("OPf0YbXqDm0", "Uptown Funk", "Bruno Mars", "Uptown Special", "4:30"),
                track("UqyT8IEBkvY", "24K Magic", "Bruno Mars", "24K Magic", "3:46"),
                track("5NV6Rdv1a3I", "Get Lucky", "Daft Punk", "Random Access Memories", "4:08"),
                track("A2VpR8HahKc", "One More Time", "Daft Punk", "Discovery", "5:20"),
                track("xFrGuyw1V8s", "Dancing Queen", "ABBA", "Arrival", "3:51"),
                track("XEjLoHdbVeE", "Gimme! Gimme! Gimme!", "ABBA", "Voulez-Vous", "4:49"),
                track("izGwDsrQ1eQ", "Careless Whisper", "George Michael", "Make It Big", "5:00"),
                track("id2-K3daNRQ", "I Wanna Dance with Somebody", "Whitney Houston", "Whitney", "4:52"),
                track("djV11Xbc914", "Take On Me", "a-ha", "Hunting High and Low", "3:45"),
                track("HgzGwKwLmgM", "Don't Stop Me Now", "Queen", "Jazz", "3:29"),
                track("PIb6AZdTr-A", "Girls Just Want to Have Fun", "Cyndi Lauper", "She's So Unusual", "3:58"),
                track("aGCdLKXNF3w", "Everybody Wants to Rule the World", "Tears for Fears", "Songs from the Big Chair", "4:11"),
                track("OMOGaugKpzs", "Every Breath You Take", "The Police", "Synchronicity", "4:13"),
                track("rYEDA3JcQqw", "Rolling in the Deep", "Adele", "21", "3:48"),
                track("dvgZkm1xWPE", "Viva La Vida", "Coldplay", "Viva la Vida", "4:02"),
                track("yKNxeF4KMsY", "Yellow", "Coldplay", "Parachutes", "4:29"),
                track("JGwWNGJdvx8", "Shape of You", "Ed Sheeran", "÷", "3:53"),
                track("09R8_2nJtjg", "Sugar", "Maroon 5", "V", "3:55"),
                track("DtVBCG6ThDk", "Rocket Man", "Elton John", "Honky Château", "4:41"),
                track("lXgkuM2NhYI", "Heroes", "David Bowie", "Heroes", "3:36"),
                track("TvnYmWpD_T8", "Purple Rain", "Prince", "Purple Rain", "8:41"),
                track("mpYzO6bZMTs", "In the Air Tonight", "Phil Collins", "Face Value", "5:30"),
                track("6M6samPEMpM", "Everybody", "Backstreet Boys", "Backstreet's Back", "4:45"),
                track("fEfvTTJNeAY", "...Baby One More Time", "Britney Spears", "...Baby One More Time", "3:30"),
                track("ru0K8uYEZWw", "Can't Stop the Feeling!", "Justin Timberlake", "Trolls", "3:56"),
                track("CevxZvSJLk8", "Roar", "Katy Perry", "Prism", "3:43"),
                track("qrO4YZeyl0I", "Bad Romance", "Lady Gaga", "The Fame Monster", "4:54"),
                track("nfWlot6h_JM", "Shake It Off", "Taylor Swift", "1989", "3:39"),
                track("2vjPBrBU-TM", "Chandelier", "Sia", "1000 Forms of Fear", "3:36"),
                track("pB-5XG-DbAA", "Stay with Me", "Sam Smith", "In the Lonely Hour", "2:52"),
                track("V1Z586zoeeE", "As It Was", "Harry Styles", "Harry's House", "2:47"),
                track("G7KNmW9a75Y", "Flowers", "Miley Cyrus", "Endless Summer Vacation", "3:20"),
                track("YD4utuFiexw", "Bad Guy", "Billie Eilish", "When We All Fall Asleep...", "3:14"),
                track("wXhTHyIgQ_U", "Circles", "Post Malone", "Hollywood's Bleeding", "3:35"),
                track("7wtfhZwyrcc", "Believer", "Imagine Dragons", "Evolve", "3:24"),
                track("hT_nvWreIhg", "Counting Stars", "OneRepublic", "Native", "4:17"),
                track("-Fp0aTedjwI", "Feel Good Inc.", "Gorillaz", "Demon Days", "3:41"),
                track("PWgvGjAhvIw", "Hey Ya!", "Outkast", "Speakerboxxx/The Love Below", "3:55"),
                track("rONPuz9xMdo", "Hips Don't Lie", "Shakira", "Oral Fixation Vol. 2", "3:38"),
                track("CN5hQOI__10", "Livin' La Vida Loca", "Ricky Martin", "Ricky Martin", "4:03")
            )
        ),

        // 9. FIESTA, CUMBIA & CUARTETO (50 Canciones)
        MusicPlaylist(
            id = "pl_fiesta",
            title = "Fiesta, Cumbia & Cuarteto",
            subtitle = "Los Palmeras, La Mona, Rodrigo, Damas Gratis, Ke Personajes & más (50 temas)",
            badge = "FIESTA TOTAL",
            coverUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&q=80",
            tracks = listOf(
                track("Ij_7b2t_AUs", "El Bombón Asesino", "Los Palmeras", "Un Poco Más", "3:12"),
                track("NabLI7HdoLQ", "Olvídala", "Los Palmeras", "Grandes Éxitos", "3:40"),
                track("mpaXJfGmfq0", "Soy Sabalero", "Los Palmeras", "En Vivo Colón", "3:30"),
                track("z13SHypucdQ", "Doble Vida", "Los Palmeras", "Fiesta", "3:15"),
                track("wGCs-2ql2Lw", "Beso a Beso", "La Mona Jiménez", "Beso a Beso", "4:10"),
                track("giX01Mu34p0", "Quién Se Ha Tomado Todo el Vino", "La Mona Jiménez", "En Vivo", "4:20"),
                track("1fx6aNitmOs", "El Federal", "La Mona Jiménez", "Raza Negra", "3:55"),
                track("fCZLaLzbhrI", "La Mano de Dios", "Rodrigo", "A 2000", "5:30"),
                track("ZC0wMHhAKOc", "Ocho Cuarenta", "Rodrigo", "Cuarteteando", "3:45"),
                track("zbucRj_Dm3Y", "Lo Mejor del Amor", "Rodrigo", "Lo Mejor del Amor", "3:50"),
                track("JsOtkliXCo8", "Soy Cordobés", "Rodrigo", "A 2000", "3:45"),
                track("tim_8lORz1c", "No Te Creas Tan Importante", "Damas Gratis", "Somos Nosotros", "3:35"),
                track("slvEqmq8WsY", "Me Vas a Extrañar", "Damas Gratis", "En Vivo", "3:15"),
                track("kaxxrKIzU_8", "Se Te Ve la Tanga", "Damas Gratis", "Para los Pibes", "3:05"),
                track("bscRMgViJzY", "Los Dueños del Pabellón", "Damas Gratis", "Operación Damas Gratis", "3:40"),
                track("XkLRR9PXQSk", "Un Finde", "Ke Personajes", "Crossover #2", "2:45"),
                track("LahR993X1PI", "Pobre Corazón", "Ke Personajes", "En Vivo", "3:20"),
                track("9FTx-xaflZY", "Adiós Amor", "Ke Personajes", "Grandes Éxitos", "3:10"),
                track("pHPTDX_vS-4", "Loco (Tu Forma de Ser)", "Los Auténticos Decadentes", "El Milagro Argentino", "3:35"),
                track("w_4hUZtHMCg", "El Murguero", "Los Auténticos Decadentes", "Mi Vida Loca", "4:40"),
                track("vzwolTO2kE4", "Corazón", "Los Auténticos Decadentes", "Mi Vida Loca", "4:15"),
                track("d0TkrT3sguM", "La Guitarra", "Los Auténticos Decadentes", "Mi Vida Loca", "3:45"),
                track("9dtqJ5SHEeM", "No Me Arrepiento de Este Amor", "Gilda", "Corazón Valiente", "3:30"),
                track("JHZq0NqI2TU", "Fuiste", "Gilda", "Corazón Valiente", "3:00"),
                track("H3GG5CyaAA8", "Se Me Ha Perdido un Corazón", "Gilda", "Pasito a Pasito", "3:12"),
                track("2NeZ8Z1ubj4", "Una Cerveza", "Ráfaga", "Una Cerveza", "3:25"),
                track("RPyG8YJBjp4", "Mentirosa", "Ráfaga", "Ráfaga en Vivo", "3:15"),
                track("Qt8n8QWV-FI", "Agüita", "Ráfaga", "Ráfaga de Amor", "3:20"),
                track("Nh26cDCPlKo", "Matador", "Los Fabulosos Cadillacs", "Vasos Vacíos", "4:35"),
                track("3fWjLmt8xzI", "Vasos Vacíos", "Los Fabulosos Cadillacs", "Vasos Vacíos", "4:40"),
                track("8hklYKTmaiU", "Mal Bicho", "Los Fabulosos Cadillacs", "Rey Azúcar", "4:05"),
                track("-ki-3oLJOIE", "La Cumbia de los Trapos", "Yerba Brava", "Cumbia Villera", "3:20"),
                track("x5PGbRt9m3U", "Pibe Cantina", "Yerba Brava", "100% Yerba Brava", "3:10"),
                track("VsV1PClw7nw", "Por Lo Que Yo Te Quiero", "Walter Olmos", "A Pura Sangre", "3:45"),
                track("a2mst3KR0_E", "Amor Fugitivo", "Walter Olmos", "De Madrid al Cielo", "3:30"),
                track("yCTMFmlgWU8", "Intento", "Ulises Bueno", "En la Piel", "4:05"),
                track("tv8mLdi60DQ", "Dale Vieja Dale", "Ulises Bueno", "Creo", "3:30"),
                track("LUaqy_b2QA4", "Universo Paralelo", "La K'onga", "En Vivo", "3:25"),
                track("WXygjN3YZZ0", "Ya No Más", "La K'onga", "Sin Límites", "3:15"),
                track("dZWBHnK_SqY", "Yo Tomo Licor", "Amar Azul", "Dime Tú", "3:15"),
                track("-95hS_UD1II", "El Polvito del Amor", "Amar Azul", "Insuperable", "3:10"),
                track("D4hnLxYWvB4", "El Campanero", "Tambó Tambó", "Tambó Tambó", "3:30"),
                track("-23VoCagjDU", "La Cumbia de los Vagos", "Tambó Tambó", "El Auténtico", "3:15"),
                track("8XyooPB03qI", "La Marca de la Gorra", "Mala Fama", "Ritmo Sustancia", "3:20"),
                track("h4qsDwpjuss", "Guampa Chata", "Mala Fama", "Para Vos", "3:05"),
                track("J5iFix-1FJw", "Amores Como el Nuestro", "Los Charros", "Corazón de Papel", "3:35"),
                track("LmpAu1RqDrU", "Jurabas Tú", "Los del Fuego", "En Vivo", "3:10"),
                track("MGQhcjK7RBw", "Una Calle Nos Separa", "Los Leales", "Grandes Éxitos", "3:40"),
                track("mMcKOAEUGro", "Le Pido a Dios", "Leo Mattioli", "Un Homenaje al Cielo", "3:50"),
                track("lhM_aMimHN8", "Tramposa y Mentirosa", "Leo Mattioli", "Ese Soy Yo", "3:40")
            )
        ),

        // 10. ACÚSTICOS & BALADAS DEL ROCK (50 Canciones)
        MusicPlaylist(
            id = "pl_acustico",
            title = "Acústicos & Baladas del Rock",
            subtitle = "Clapton, Nirvana Unplugged, Guns N' Roses, Scorpions & más (50 temas)",
            badge = "BALADAS",
            coverUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=500&q=80",
            tracks = listOf(
                track("JxPj3GAYYZ0", "Tears in Heaven", "Eric Clapton", "Rush", "4:32"),
                track("EOs0qeiJyIg", "Layla (Acoustic)", "Eric Clapton", "Unplugged", "4:49"),
                track("fregObNcHC8", "The Man Who Sold the World", "Nirvana", "MTV Unplugged in New York", "4:20"),
                track("Nq62vcKxYcU", "Where Did You Sleep Last Night", "Nirvana", "MTV Unplugged in New York", "5:08"),
                track("ErvgV4P6Fzc", "Patience", "Guns N' Roses", "G N' R Lies", "5:56"),
                track("F_-ZuVy76yg", "Wind of Change", "Scorpions", "Crazy World", "5:12"),
                track("O5Kw41JAfG4", "Still Loving You", "Scorpions", "Love at First Sting", "6:28"),
                track("NvryolGa19A", "Bed of Roses", "Bon Jovi", "Keep the Faith", "6:34"),
                track("9BMwcO6_hyA", "Always", "Bon Jovi", "Cross Road", "5:53"),
                track("JkK8g6FMEXE", "I Don't Want to Miss a Thing", "Aerosmith", "Armageddon", "4:59"),
                track("NeU4GwYBlLI", "More Than Words", "Extreme", "Pornograffitti", "5:34"),
                track("Y0pdQU87dc8", "(Everything I Do) I Do It for You", "Bryan Adams", "Waking Up the Neighbours", "6:34"),
                track("mxVbPEIkfqw", "Please Forgive Me", "Bryan Adams", "So Far So Good", "5:55"),
                track("ivtSA0-fbcg", "To Be with You", "Mr. Big", "Lean into It", "3:27"),
                track("LiQdV69sINo", "I Remember You", "Skid Row", "Skid Row", "5:14"),
                track("ZLwwIe_PjhE", "18 and Life", "Skid Row", "Skid Row", "3:50"),
                track("yziKcd4YAkU", "Is This Love", "Whitesnake", "Whitesnake", "4:43"),
                track("jYx4Y52xAts", "Every Rose Has Its Thorn", "Poison", "Open Up and Say... Ahh!", "4:20"),
                track("IXdNnw99-Ic", "Wish You Were Here", "Pink Floyd", "Wish You Were Here", "5:34"),
                track("NrUIJY_Xu2s", "Going to California", "Led Zeppelin", "Led Zeppelin IV", "3:32"),
                track("oWRr03VcA-0", "Angie", "The Rolling Stones", "Goats Head Soup", "4:33"),
                track("NrgmdOz227I", "Yesterday", "The Beatles", "Help!", "2:05"),
                track("QDYfEBY9NM4", "Let It Be", "The Beatles", "Let It Be", "4:03"),
                track("_IL77qFKgRw", "Love of My Life", "Queen", "A Night at the Opera", "3:38"),
                track("6hzrDeceEKc", "Wonderwall", "Oasis", "(What's the Story) Morning Glory?", "4:18"),
                track("oplra1FJxWI", "Don't Look Back in Anger", "Oasis", "(What's the Story) Morning Glory?", "4:48"),
                track("1uYWYWPc9HU", "Karma Police", "Radiohead", "OK Computer", "4:24"),
                track("n5h0qHwNrHk", "Fake Plastic Trees", "Radiohead", "The Bends", "4:50"),
                track("xwtdhWltSIg", "Losing My Religion", "R.E.M.", "Out of Time", "4:28"),
                track("5rOiW_xY-kc", "Everybody Hurts", "R.E.M.", "Automatic for the People", "5:17"),
                track("sC2GjXMk7i4", "Blow Up the Outside World", "Soundgarden", "Down on the Upside", "5:46"),
                track("f8hT3oDDf6c", "Down in a Hole (Unplugged)", "Alice In Chains", "MTV Unplugged", "5:40"),
                track("_7ioE4wRil8", "Just Breathe", "Pearl Jam", "Backspacer", "3:35"),
                track("GLvohMXgcBo", "Under the Bridge", "Red Hot Chili Peppers", "Blood Sugar Sex Magik", "4:24"),
                track("CnQ8N1KacJc", "Good Riddance (Time of Your Life)", "Green Day", "Nimrod", "2:34"),
                track("NdYWuo9OFAw", "Iris", "Goo Goo Dolls", "Dizzy Up the Girl", "4:49"),
                track("fgT9zGkiLig", "Drive", "Incubus", "Make Yourself", "3:52"),
                track("kPBzTxZQG5Q", "Here Without You", "3 Doors Down", "Away from the Sun", "3:58"),
                track("tH2w6Oxx0kQ", "Dust in the Wind", "Kansas", "Point of Know Return", "3:26"),
                track("zSAJ0l4OBHM", "A Horse with No Name", "America", "America", "4:12"),
                track("ZxjTC0bmKls", "Father and Son", "Cat Stevens", "Tea for the Tillerman", "3:40"),
                track("rm9coqlk8fY", "Knockin' on Heaven's Door", "Bob Dylan", "Pat Garrett & Billy the Kid", "2:32"),
                track("W47hOuQjDag", "Barro Tal Vez", "Luis Alberto Spinetta", "Kamikaze", "3:20"),
                track("1Zjm0uh8oeA", "Canción Para Mi Muerte", "Sui Generis", "Vida", "3:36"),
                track("BxucLDUu4bg", "Desarma y Sangra", "Serú Girán", "Bicicleta", "3:40"),
                track("h9AHQV3pPn8", "Ojos de Video Tape", "Charly García", "Clics Modernos", "3:50"),
                track("qt11aXRwMiA", "Té Para Tres", "Soda Stereo", "Canción Animal", "2:26"),
                track("NAcDZfLiwss", "Pétalo de Sal", "Fito Páez", "El Amor Después del Amor", "2:43"),
                track("Ma-r9hq-dsU", "Spaghetti del Rock", "Divididos", "Narigón del Siglo", "3:32"),
                track("tAGnKpE4NCI", "Nothing Else Matters", "Metallica", "Metallica", "6:28")
            )
        ),

        // 11. GUITAR HEROES & BLUES CLÁSICO (50 Canciones)
        MusicPlaylist(
            id = "pl_guitar_heroes",
            title = "Guitar Heroes & Blues Clásico",
            subtitle = "SRV, Hendrix, Gary Moore, B.B. King, Clapton, Santana & más (50 temas)",
            badge = "GUITAR HEROES",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            tracks = listOf(
                track("0vo23H9J8o8", "Pride and Joy", "Stevie Ray Vaughan", "Texas Flood", "3:40"),
                track("wVjdMLAMbM0", "Texas Flood", "Stevie Ray Vaughan", "Texas Flood", "5:21"),
                track("An4uDegHB8s", "Little Wing", "Stevie Ray Vaughan", "The Sky Is Crying", "6:48"),
                track("WGoDaYjdfSg", "Purple Haze", "Jimi Hendrix", "Are You Experienced", "2:51"),
                track("L7UMubmfbH0", "Voodoo Child", "Jimi Hendrix", "Electric Ladyland", "5:12"),
                track("4O_YMLDvvnw", "Still Got the Blues", "Gary Moore", "Still Got the Blues", "6:11"),
                track("vkUpfw4Hf3w", "Parisienne Walkways", "Gary Moore", "Back on the Streets", "3:18"),
                track("oica5jG7FpU", "The Thrill Is Gone", "B.B. King", "Completely Well", "5:24"),
                track("qYS732zyYfU", "Cocaine", "Eric Clapton", "Slowhand", "3:38"),
                track("uSquiIVLhrQ", "Layla", "Derek and the Dominos", "Layla", "7:05"),
                track("9wT1s96JIb0", "Black Magic Woman", "Carlos Santana", "Abraxas", "5:24"),
                track("QcUxrpIwuQ4", "Smooth", "Carlos Santana", "Supernatural", "4:56"),
                track("53pweVfVwV8", "Gravity", "John Mayer", "Continuum", "4:05"),
                track("32GZ3suxRn4", "Slow Dancing in a Burning Room", "John Mayer", "Continuum", "4:02"),
                track("VI57QHL6ge0", "Always with Me, Always with You", "Joe Satriani", "Surfing with the Alien", "3:22"),
                track("tCEXHA93Tnw", "For the Love of God", "Steve Vai", "Passion and Warfare", "6:02"),
                track("M4Czx8EWXb0", "Eruption", "Van Halen", "Van Halen", "1:42"),
                track("55nAwmVLQSk", "Cliffs of Dover", "Eric Johnson", "Ah Via Musicom", "4:07"),
                track("hzFpiW5vHrc", "Stranglehold", "Ted Nugent", "Ted Nugent", "8:22"),
                track("vTOozRAJ8dU", "Jessica", "The Allman Brothers Band", "Brothers and Sisters", "7:30"),
                track("89Qg_gYqkys", "Sultans of Swing", "Dire Straits", "Dire Straits", "5:47"),
                track("wTP2RUD_cL0", "Money for Nothing", "Dire Straits", "Brothers in Arms", "4:38"),
                track("_FrOQC-zEog", "Comfortably Numb", "Pink Floyd", "The Wall", "6:23"),
                track("yjecye6ydlI", "El Hombre Suburbano", "Pappo's Blues", "Pappo's Blues Vol. 1", "2:18"),
                track("mAr_QQ9uSFc", "Sucio y Desprolijo", "Pappo's Blues", "Pappo's Blues Vol. 3", "4:33"),
                track("hYgmT91EXlY", "Desconfío de la Vida", "Pappo's Blues", "Pappo's Blues Vol. 2", "2:46"),
                track("82oqLCiGQvA", "Juntos a la Par", "Pappo", "Buscando un Amor", "3:20"),
                track("HibBnC6SVk8", "Whole Lotta Love", "Led Zeppelin", "Led Zeppelin II", "5:34"),
                track("QkF3oxziUI4", "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "8:02"),
                track("2KPEHohJMuw", "Black Dog", "Led Zeppelin", "Led Zeppelin IV", "4:56"),
                track("f3y8jf01UY8", "Sunshine of Your Love", "Cream", "Disraeli Gears", "4:10"),
                track("rG6b8gjMEkw", "La Grange", "ZZ Top", "Tres Hombres", "3:52"),
                track("7wRHBLwpASw", "Sharp Dressed Man", "ZZ Top", "Eliminator", "4:14"),
                track("Wr9ie2J2690", "Highway Star", "Deep Purple", "Machine Head", "6:05"),
                track("LCnebZnysmI", "Burn", "Deep Purple", "Burn", "6:00"),
                track("qK7j_EERgT4", "Smoke on the Water", "Deep Purple", "Machine Head", "5:40"),
                track("pAgnJDJN4VA", "Back in Black", "AC/DC", "Back in Black", "4:15"),
                track("l482T0yNkeo", "Highway to Hell", "AC/DC", "Highway to Hell", "3:28"),
                track("v2AC41dglnM", "Thunderstruck", "AC/DC", "The Razors Edge", "4:52"),
                track("1w7OgIMMRc4", "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", "5:03"),
                track("8SbUC-UaAxE", "November Rain", "Guns N' Roses", "Use Your Illusion I", "8:57"),
                track("SwYN7mTi6HM", "Jump", "Van Halen", "1984", "4:04"),
                track("fuKDBPw8wQA", "Panama", "Van Halen", "1984", "3:32"),
                track("Djrl6fu8myo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("CD-E-LDc384", "Enter Sandman", "Metallica", "Metallica", "5:31"),
                track("2f1Ny74_ou0", "Master of Puppets", "Metallica", "Master of Puppets", "8:35"),
                track("ye5BuYf8q4o", "Sweet Home Alabama", "Lynyrd Skynyrd", "Second Helping", "4:44"),
                track("auLBLk4ibAk", "Tom Sawyer", "Rush", "Moving Pictures", "4:34"),
                track("t4QK8RxCAwo", "More Than a Feeling", "Boston", "Boston", "4:45")
            )
        ),

        // 12. NOVEDADES & ÉXITOS RECIENTES (50 Canciones)
        MusicPlaylist(
            id = "pl_lanzamientos",
            title = "Novedades & Éxitos Recientes",
            subtitle = "Linkin Park, Ghost, Stones, Blink-182, Green Day, Måneskin & más (50 temas)",
            badge = "LANZAMIENTOS",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            tracks = listOf(
                track("SRXH9AbT280", "The Emptiness Machine", "Linkin Park", "From Zero", "3:10"),
                track("Wn_3VX9HuKU", "Square Hammer", "Ghost", "Popestar", "3:58"),
                track("_u-7rWKnVVo", "Lux Æterna", "Metallica", "72 Seasons", "3:25"),
                track("_mEC54eTuGw", "Angry", "The Rolling Stones", "Hackney Diamonds", "3:46"),
                track("cIuYFCAJ2a8", "Beggin'", "Måneskin", "Chosen", "3:31"),
                track("yOb9Xaug35M", "I WANNA BE YOUR SLAVE", "Måneskin", "Teatro d'ira - Vol. I", "2:53"),
                track("XrsbfrFPATs", "THE LONELIEST", "Måneskin", "RUSH!", "4:07"),
                track("aJg4OJxp-co", "Highway Tune", "Greta Van Fleet", "From the Fires", "3:01"),
                track("FYH8DsU2WCk", "There'd Better Be a Mirrorball", "Arctic Monkeys", "The Car", "4:48"),
                track("mxnksi-G0T0", "This Is Why", "Paramore", "This Is Why", "3:27"),
                track("eVTXPUF4Oz4", "In the End", "Linkin Park", "Hybrid Theory", "3:36"),
                track("kXYiU_JCYtU", "Numb", "Linkin Park", "Meteora", "3:07"),
                track("LYU-8IFcDPw", "Faint", "Linkin Park", "Meteora", "2:42"),
                track("hq0rZ3IiyWw", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("9jK-NcRmVcw", "The Pretender", "Foo Fighters", "Echoes, Silence, Patience & Grace", "4:30"),
                track("AJDiYxKSAqQ", "Basket Case", "Green Day", "Dookie", "3:01"),
                track("Soa3gO7tL-c", "Boulevard of Broken Dreams", "Green Day", "American Idiot", "4:21"),
                track("98W9QuMq-2k", "All the Small Things", "Blink-182", "Enema of the State", "2:47"),
                track("s1tAYmMjLdY", "I Miss You", "Blink-182", "Blink-182", "3:47"),
                track("1KP_nPltW9s", "The Kids Aren't Alright", "The Offspring", "Americana", "3:00"),
                track("YlUKcNNmywk", "Californication", "Red Hot Chili Peppers", "Californication", "5:21"),
                track("rn_YodiJO6k", "Otherside", "Red Hot Chili Peppers", "Californication", "4:15"),
                track("gGdGFtwCNBE", "Mr. Brightside", "The Killers", "Hot Fuss", "3:42"),
                track("w8KQmps-Sog", "Uprising", "Muse", "The Resistance", "5:03"),
                track("bpOSxM0rNPM", "Do I Wanna Know?", "Arctic Monkeys", "AM", "4:32"),
                track("7QU1nvuxaMA", "Like a Stone", "Audioslave", "Audioslave", "4:54"),
                track("3YxaaGgTQYM", "Bring Me to Life", "Evanescence", "Fallen", "3:57"),
                track("Hm7vnOC4hoY", "Last Resort", "Papa Roach", "Infest", "3:19"),
                track("RYnFIRc0k6E", "Rollin'", "Limp Bizkit", "Chocolate Starfish...", "3:33"),
                track("CMX2lPum_pg", "Fat Lip", "Sum 41", "All Killer No Filler", "2:58"),
                track("RRKJiM9Njr8", "Welcome to the Black Parade", "My Chemical Romance", "The Black Parade", "5:11"),
                track("6rkLF0zxp7E", "Last Nite", "The Strokes", "Is This It", "3:13"),
                track("7dfBmjrN5W0", "Take Me Out", "Franz Ferdinand", "Franz Ferdinand", "3:57"),
                track("s88r_q7oufE", "No One Knows", "Queens of the Stone Age", "Songs for the Deaf", "4:38"),
                track("GK-DkHHAMIU", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("aEx8XyOvmnM", "Down with the Sickness", "Disturbed", "The Sickness", "4:38"),
                track("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "After Hours", "3:20"),
                track("oygrmJFKYZY", "Don't Start Now", "Dua Lipa", "Future Nostalgia", "3:03"),
                track("OPf0YbXqDm0", "Uptown Funk", "Bruno Mars", "Uptown Special", "4:30"),
                track("5NV6Rdv1a3I", "Get Lucky", "Daft Punk", "Random Access Memories", "4:08"),
                track("dvgZkm1xWPE", "Viva La Vida", "Coldplay", "Viva la Vida", "4:02"),
                track("7wtfhZwyrcc", "Believer", "Imagine Dragons", "Evolve", "3:24"),
                track("hT_nvWreIhg", "Counting Stars", "OneRepublic", "Native", "4:17"),
                track("-Fp0aTedjwI", "Feel Good Inc.", "Gorillaz", "Demon Days", "3:41"),
                track("V1Z586zoeeE", "As It Was", "Harry Styles", "Harry's House", "2:47"),
                track("G7KNmW9a75Y", "Flowers", "Miley Cyrus", "Endless Summer Vacation", "3:20"),
                track("YD4utuFiexw", "Bad Guy", "Billie Eilish", "When We All Fall Asleep...", "3:14")
            )
        )
    )

    fun getTracksForCategory(category: MusicCategory, searchResults: List<MusicTrack>): List<MusicTrack> {
        val matchingLocal = when (category.id) {
            "rock_clasico" -> playlists.find { it.id == "pl_riffs" }?.tracks ?: emptyList()
            "metal" -> playlists.find { it.id == "pl_metal" }?.tracks ?: emptyList()
            "grunge" -> playlists.find { it.id == "pl_grunge" }?.tracks ?: emptyList()
            "nacional" -> playlists.find { it.id == "pl_nacional" }?.tracks ?: emptyList()
            "metal_extremo" -> (playlists.find { it.id == "pl_metal_extremo" }?.tracks ?: emptyList()) + (playlists.find { it.id == "pl_metal_nacional" }?.tracks ?: emptyList())
            "pop" -> playlists.find { it.id == "pl_pop_hits" }?.tracks ?: emptyList()
            "fiesta" -> playlists.find { it.id == "pl_fiesta" }?.tracks ?: emptyList()
            "punk" -> playlists.find { it.id == "pl_2000s" }?.tracks ?: emptyList()
            "acustico" -> playlists.find { it.id == "pl_acustico" }?.tracks ?: emptyList()
            "blues" -> playlists.find { it.id == "pl_guitar_heroes" }?.tracks ?: emptyList()
            else -> playlists.flatMap { it.tracks }
        }
        return (searchResults + matchingLocal).distinctBy { it.id }
    }

    fun getTracksForArtist(artist: MusicArtist, searchResults: List<MusicTrack>): List<MusicTrack> {
        val localMatches = playlists.flatMap { it.tracks }
            .filter { it.artist.contains(artist.name, ignoreCase = true) || it.title.contains(artist.name, ignoreCase = true) }
        return (searchResults + localMatches).distinctBy { it.id }
    }

    /**
     * Maps an artist to their precise genre family playlist IDs in the local catalog.
     * Prevents cross-genre contamination (metal jumping to cumbia/cuarteto or rock barrial).
     */
    fun getGenreFamilyPlaylistIds(artistName: String): List<String> {
        val lower = artistName.lowercase().trim()
        val metalArtists = listOf(
            "horcas", "malon", "malón", "hermetica", "hermética", "almafuerte", "v8",
            "tren loco", "o'connor", "oconnor", "logos", "rata blanca", "plan 4", "plan4",
            "carajo", "a.n.i.m.a.l.", "animal", "against", "lethal", "serpentor", "mastifal",
            "arde la sangre", "deni", "deny", "cabezones", "asspera", "d-mente", "helker",
            "cannibal corpse", "death", "behemoth", "amon amarth", "dimmu borgir", "arch enemy",
            "children of bodom", "cradle of filth", "opeth", "gojira", "lamb of god", "rammstein",
            "slipknot", "system of a down", "sepultura", "slayer", "judas priest", "pantera",
            "megadeth", "metallica", "accept", "helloween", "dio", "motörhead", "motorhead",
            "anthrax", "disturbed", "avenged sevenfold", "scorpions", "iron maiden", "black sabbath",
            "ozzy", "ghost", "deftones", "korn"
        )
        val fiestaArtists = listOf(
            "los palmeras", "la mona", "rodrigo", "damas gratis", "ke personajes",
            "autenticos decadentes", "auténticos decadentes", "gilda", "rafaga", "ráfaga",
            "yerba brava", "walter olmos", "ulises bueno", "amar azul", "la re pandilla",
            "los leales", "el polaco", "karina", "nestor en bloque", "meta guacha", "la base",
            "repiola", "pala ancha", "supermerk2", "tambo tambo", "tambó tambó", "mala fama",
            "los charros", "los del fuego", "leo mattioli", "la k'onga", "la konga"
        )
        val punkOr2000sArtists = listOf(
            "attaque 77", "2 minutos", "dos minutos", "flema", "cadena perpetua", "eterna inocencia",
            "bad religion", "offspring", "the offspring", "blink-182", "blink 182", "green day", "sum 41",
            "nofx", "pennywise", "millencolin", "lagwagon", "ramones", "the clash",
            "linkin park", "papa roach", "limp bizkit", "my chemical romance", "paramore",
            "evanescence", "foo fighters", "audioslave", "nirvana", "pearl jam", "soundgarden",
            "alice in chains", "stone temple pilots", "silverchair"
        )
        val rockNacionalArtists = listOf(
            "la renga", "los redondos", "patricio rey", "indio solari", "skay", "divididos",
            "sumo", "charly garcia", "charly garcía", "spinetta", "soda stereo", "cerati",
            "callejeros", "don osvaldo", "los piojos", "ciro", "guasones", "las pastillas",
            "salta la banca", "viejas locas", "intoxicados", "la 25", "gardelitos", "los gardelitos",
            "el bordo", "sueño de pescado", "la vela puerca", "no te va gustar", "ntvg",
            "bersuit", "fito paez", "fito páez", "babasonicos", "babasónicos", "estelares",
            "ratones paranoicos", "mancha de rolando", "los tipitos"
        )
        val popArtists = listOf(
            "michael jackson", "madonna", "the weeknd", "dua lipa", "bruno mars", "daft punk",
            "abba", "george michael", "whitney houston", "a-ha", "cyndi lauper", "tears for fears",
            "adele", "coldplay", "ed sheeran", "maroon 5", "elton john", "david bowie", "prince",
            "phil collins", "backstreet boys", "britney spears", "justin timberlake", "katy perry",
            "lady gaga", "taylor swift", "sia", "sam smith", "harry styles", "miley cyrus",
            "billie eilish", "post malone", "imagine dragons", "onerepublic", "gorillaz", "shakira"
        )

        return when {
            metalArtists.any { lower.contains(it) || it.contains(lower) } -> listOf("pl_metal_nacional", "pl_metal", "pl_metal_extremo")
            fiestaArtists.any { lower.contains(it) || it.contains(lower) } -> listOf("pl_fiesta")
            punkOr2000sArtists.any { lower.contains(it) || it.contains(lower) } -> listOf("pl_2000s", "pl_grunge")
            rockNacionalArtists.any { lower.contains(it) || it.contains(lower) } -> listOf("pl_nacional")
            popArtists.any { lower.contains(it) || it.contains(lower) } -> listOf("pl_pop_hits")
            else -> {
                val matchedPl = playlists.firstOrNull { pl -> pl.tracks.any { it.artist.lowercase().contains(lower) } }
                if (matchedPl != null) listOf(matchedPl.id) else listOf("pl_riffs", "pl_nacional")
            }
        }
    }

    /**
     * Builds an intelligent Spotify-style radio queue centered around an initial track.
     * Takes the initial track, other tracks by the same artist, and similar tracks from matching genre playlists.
     */
    fun buildRadioQueue(initialTrack: MusicTrack): List<MusicTrack> {
        val artistLower = initialTrack.artist.lowercase().trim()
        val allCatalogTracks = playlists.flatMap { it.tracks }.distinctBy { it.id }

        // Find tracks by the exact same artist
        val sameArtistTracks = allCatalogTracks.filter {
            it.id != initialTrack.id && (it.artist.lowercase().contains(artistLower) || artistLower.contains(it.artist.lowercase()))
        }.shuffled()

        // Find safe matching genre playlists
        val familyPlaylistIds = getGenreFamilyPlaylistIds(initialTrack.artist)
        val genrePool = playlists.filter { it.id in familyPlaylistIds }.flatMap { it.tracks }
            .filter { it.id != initialTrack.id && !it.artist.lowercase().contains(artistLower) }
            .shuffled()

        val radioList = mutableListOf<MusicTrack>()
        radioList.add(initialTrack)
        radioList.addAll(sameArtistTracks.take(5))
        radioList.addAll(genrePool.take(40))

        return radioList.distinctBy { it.id }
    }

    /**
     * Executes an Innertube HTTP POST request with proper headers and JSON payload.
     */
    private fun executeInnertubePost(urlStr: String, payload: JSONObject): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseText = reader.readText()
                reader.close()
                JSONObject(responseText)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Search YouTube Music returning both an optional artist profile summary and song tracks.
     */
    suspend fun searchMusicWithArtist(query: String): SearchMusicResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext SearchMusicResult()

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "es")
                        put("gl", "AR")
                    })
                })
                put("query", trimmed)
            }

            val responseJson = executeInnertubePost("https://music.youtube.com/youtubei/v1/search", payload)
            if (responseJson != null) {
                val artistSummary = extractArtistSummaryFromJson(responseJson)
                val ytmTracks = extractYtmTracksFromJson(responseJson)
                if (ytmTracks.isNotEmpty()) {
                    return@withContext SearchMusicResult(artist = artistSummary, tracks = ytmTracks)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to standard YouTube Web search
        try {
            val webPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "es")
                        put("gl", "US")
                    })
                })
                put("query", trimmed)
            }
            val webJson = executeInnertubePost("https://www.youtube.com/youtubei/v1/search", webPayload)
            if (webJson != null) {
                val webTracks = extractTracksFromJson(webJson)
                if (webTracks.isNotEmpty()) {
                    return@withContext SearchMusicResult(tracks = webTracks)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: match from local curated catalog if offline
        val lower = trimmed.lowercase()
        val allLocalTracks = playlists.flatMap { it.tracks }.distinctBy { it.id }
        val matchingLocal = allLocalTracks.filter {
            it.title.lowercase().contains(lower) || it.artist.lowercase().contains(lower)
        }
        SearchMusicResult(tracks = matchingLocal)
    }

    suspend fun searchMusic(query: String): List<MusicTrack> {
        return searchMusicWithArtist(query).tracks
    }

    /**
     * Loads a complete artist profile by browseId, including top songs, albums, and singles.
     */
    suspend fun getArtistProfile(browseId: String, artistName: String = "", fallbackImage: String = ""): ArtistProfile = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "es")
                    put("gl", "AR")
                })
            })
            put("browseId", browseId)
        }

        val json = executeInnertubePost("https://music.youtube.com/youtubei/v1/browse", payload)
        if (json != null) {
            val topTracks = mutableListOf<MusicTrack>()
            val albums = mutableListOf<MusicAlbum>()
            val singles = mutableListOf<MusicAlbum>()
            val similarArtists = mutableListOf<MusicArtistSummary>()
            var parsedName = artistName
            var parsedSubtitle = ""
            var parsedImage = fallbackImage

            fun parseArtistSections(obj: Any) {
                when (obj) {
                    is JSONObject -> {
                        // Extract Header if present
                        if (obj.has("musicImmersiveHeaderRenderer")) {
                            val header = obj.optJSONObject("musicImmersiveHeaderRenderer")
                            if (header != null) {
                                val t = header.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                if (!t.isNullOrEmpty()) parsedName = t
                                val sub = header.optJSONObject("description")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                    ?: header.optJSONObject("monthlyListeners")?.optString("text") ?: ""
                                if (sub.isNotEmpty()) parsedSubtitle = sub
                                val thumbs = header.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                if (thumbs != null && thumbs.length() > 0) {
                                    parsedImage = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: parsedImage
                                }
                            }
                        }

                        // Top Songs Shelf
                        if (obj.has("musicShelfRenderer")) {
                            val shelf = obj.optJSONObject("musicShelfRenderer")
                            val shelfTitle = shelf?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            val lowerTitle = shelfTitle.lowercase()
                            if (lowerTitle.contains("canci") || lowerTitle.contains("song") || lowerTitle.contains("popular") || lowerTitle.contains("mencionad") || lowerTitle.contains("éxito")) {
                                val contents = shelf?.optJSONArray("contents")
                                if (contents != null) {
                                    for (i in 0 until contents.length()) {
                                        val item = contents.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer")
                                        if (item != null) {
                                            val track = parseYtmItemToTrack(item, parsedName)
                                            if (track != null) topTracks.add(track)
                                        }
                                    }
                                }
                            }
                        }

                        // Carousel Shelves: Albums, Singles, and Similar Artists (Fans also like)
                        if (obj.has("musicCarouselShelfRenderer")) {
                            val car = obj.optJSONObject("musicCarouselShelfRenderer")
                            val headerTitle = car?.optJSONObject("header")
                                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                                ?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            val lowerHeader = headerTitle.lowercase()
                            val isAlbum = lowerHeader.contains("lbum")
                            val isSingle = lowerHeader.contains("single") || lowerHeader.contains("sencillo") || lowerHeader.contains("ep")
                            val isSimilar = lowerHeader.contains("similar") || lowerHeader.contains("fan") ||
                                    lowerHeader.contains("parecid") || lowerHeader.contains("relacionad") ||
                                    lowerHeader.contains("gust") || lowerHeader.contains("oyente")

                            if (isAlbum || isSingle) {
                                val contents = car?.optJSONArray("contents")
                                if (contents != null) {
                                    for (i in 0 until contents.length()) {
                                        val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer")
                                        if (item != null) {
                                            val aTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                            val aBrowseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""
                                            val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
                                            val sb = StringBuilder()
                                            if (subtitleRuns != null) {
                                                for (r in 0 until subtitleRuns.length()) {
                                                    sb.append(subtitleRuns.optJSONObject(r)?.optString("text", ""))
                                                }
                                            }
                                            val aYear = sb.toString()
                                            val thumbs = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                            val aThumb = if (thumbs != null && thumbs.length() > 0) {
                                                thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                                            } else ""

                                            if (aTitle.isNotEmpty() && aBrowseId.isNotEmpty()) {
                                                val albumObj = MusicAlbum(
                                                    browseId = aBrowseId,
                                                    title = aTitle,
                                                    year = aYear,
                                                    coverUrl = aThumb,
                                                    artist = parsedName
                                                )
                                                if (isAlbum) albums.add(albumObj) else singles.add(albumObj)
                                            }
                                        }
                                    }
                                }
                            } else if (isSimilar) {
                                val contents = car?.optJSONArray("contents")
                                if (contents != null) {
                                    for (i in 0 until contents.length()) {
                                        val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer")
                                        if (item != null) {
                                            val sTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                            val sBrowseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""
                                            val thumbs = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                            val sThumb = if (thumbs != null && thumbs.length() > 0) {
                                                thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                                            } else ""

                                            if (sTitle.isNotEmpty()) {
                                                similarArtists.add(MusicArtistSummary(browseId = sBrowseId, name = sTitle, imageUrl = sThumb))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val child = obj.opt(keys.next())
                            if (child != null) parseArtistSections(child)
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until obj.length()) {
                            val it = obj.opt(i)
                            if (it != null) parseArtistSections(it)
                        }
                    }
                }
            }

            parseArtistSections(json)

            // Extract official YouTube Music Radio Playlist ID (RDEM...) from JSON
            val rdemMatch = Regex(""""playlistId":\s*"(RDEM[^"]+)"""").find(json.toString())
            val parsedRadioPlaylistId = rdemMatch?.groupValues?.get(1) ?: ""

            // If artist has recent singles/releases, prepend them to popular tracks!
            val recentTracks = mutableListOf<MusicTrack>()
            val latestSingle = singles.firstOrNull()
            if (latestSingle != null && latestSingle.browseId.isNotEmpty()) {
                try {
                    val singleTracks = getAlbumTracks(latestSingle)
                    for (t in singleTracks.take(2)) {
                        recentTracks.add(t.copy(album = "✨ NUEVO LANZAMIENTO", isNewRelease = true))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val orderedTracks = (recentTracks + topTracks).distinctBy { it.id }

            return@withContext ArtistProfile(
                browseId = browseId,
                name = parsedName.ifEmpty { "Artista" },
                subtitle = parsedSubtitle,
                imageUrl = parsedImage,
                radioPlaylistId = parsedRadioPlaylistId,
                topTracks = orderedTracks,
                albums = albums.distinctBy { it.browseId },
                singles = singles.distinctBy { it.browseId },
                similarArtists = similarArtists.distinctBy { it.name.lowercase() }
            )
        }

        // Fallback
        ArtistProfile(
            browseId = browseId,
            name = artistName.ifEmpty { "Artista" },
            imageUrl = fallbackImage
        )
    }

    /**
     * Resolves an artist by name via search, then fetches full profile and discography.
     */
    suspend fun getArtistProfileByName(artistName: String): ArtistProfile? = withContext(Dispatchers.IO) {
        val searchRes = searchMusicWithArtist(artistName)
        val summary = searchRes.artist
        if (summary != null && summary.browseId.isNotEmpty()) {
            return@withContext getArtistProfile(summary.browseId, summary.name, summary.imageUrl)
        }
        if (searchRes.tracks.isNotEmpty()) {
            return@withContext ArtistProfile(
                browseId = "",
                name = artistName,
                subtitle = "Canciones encontradas",
                imageUrl = searchRes.tracks.first().thumbnailUrl,
                topTracks = searchRes.tracks
            )
        }
        null
    }

    /**
     * Retrieves tracks from YouTube Music Radio / WatchNext endpoint given a playlistId (e.g. RDEM... or RDAMVM...).
     * Returns ~25-50 musically cohesive tracks of the artist and genre-matched peers.
     */
    suspend fun getRadioTracks(radioPlaylistId: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MusicTrack>()
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "es")
                        put("gl", "AR")
                    })
                })
                put("playlistId", radioPlaylistId)
            }

            val json = executeInnertubePost("https://music.youtube.com/youtubei/v1/next", payload)
            if (json != null) {
                fun parseVideoRenderers(obj: Any) {
                    when (obj) {
                        is JSONObject -> {
                            if (obj.has("playlistPanelVideoRenderer")) {
                                val r = obj.optJSONObject("playlistPanelVideoRenderer")
                                val videoId = r?.optString("videoId", "") ?: ""
                                val title = r?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                val artist = r?.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                    ?: r?.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                val duration = r?.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                val thumbs = r?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                                } else if (videoId.isNotEmpty()) {
                                    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                } else ""

                                if (videoId.isNotEmpty() && title.isNotEmpty()) {
                                    tracks.add(
                                        MusicTrack(
                                            id = videoId,
                                            title = title,
                                            artist = artist.ifEmpty { "Artista" },
                                            album = "Radio Mix",
                                            duration = duration,
                                            thumbnailUrl = thumbUrl
                                        )
                                    )
                                }
                            }
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val child = obj.opt(keys.next())
                                if (child != null) parseVideoRenderers(child)
                            }
                        }
                        is JSONArray -> {
                            for (i in 0 until obj.length()) {
                                val it = obj.opt(i)
                                if (it != null) parseVideoRenderers(it)
                            }
                        }
                    }
                }
                parseVideoRenderers(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks.distinctBy { it.id }
    }

    /**
     * Fetches top songs directly from an artist's YouTube Music browse page (browseId).
     */
    suspend fun getArtistTopTracks(browseId: String, artistName: String = ""): List<MusicTrack> = withContext(Dispatchers.IO) {
        if (browseId.isEmpty()) return@withContext emptyList()
        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "es")
                    put("gl", "AR")
                })
            })
            put("browseId", browseId)
        }

        val json = executeInnertubePost("https://music.youtube.com/youtubei/v1/browse", payload) ?: return@withContext emptyList()
        val tracks = mutableListOf<MusicTrack>()

        fun extractTracks(obj: Any) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicShelfRenderer")) {
                        val shelf = obj.optJSONObject("musicShelfRenderer")
                        val contents = shelf?.optJSONArray("contents")
                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val item = contents.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer")
                                if (item != null) {
                                    val t = parseYtmItemToTrack(item, artistName)
                                    if (t != null) tracks.add(t)
                                }
                            }
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val child = obj.opt(keys.next())
                        if (child != null) extractTracks(child)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        val it = obj.opt(i)
                        if (it != null) extractTracks(it)
                    }
                }
            }
        }

        extractTracks(json)
        return@withContext tracks.distinctBy { it.id }
    }

    /**
     * Builds an on-the-fly, completely dynamic and random mix of tracks
     * featuring the target artist and curated similar artists in the exact same musical genre.
     * Uses YouTube Music's curated similar artists ("Puede que también te guste"),
     * strictly bound to the artist's genre family, with zero cross-genre pollution.
     */
    suspend fun buildSimilarArtistsRadioQueue(profile: ArtistProfile): List<MusicTrack> = withContext(Dispatchers.IO) {
        val artistLower = profile.name.lowercase().trim()
        val allCatalogTracks = playlists.flatMap { it.tracks }
        val similarTracksPool = mutableListOf<MusicTrack>()

        // 1. PRIMARY & CURATED: Fetch tracks from YouTube Music's "Puede que también te guste" (Fans Also Like)
        if (profile.similarArtists.isNotEmpty()) {
            val chosenSimilar = profile.similarArtists.shuffled().take(6)
            coroutineScope {
                val deferredTracks = chosenSimilar.map { sim ->
                    async {
                        // Check local catalog first (instant zero-latency tracks)
                        val localSimTracks = allCatalogTracks.filter {
                            it.artist.contains(sim.name, ignoreCase = true) || sim.name.contains(it.artist, ignoreCase = true)
                        }
                        if (localSimTracks.isNotEmpty()) {
                            return@async localSimTracks.shuffled().take(3)
                        }

                        // Query artist top tracks via browseId
                        if (sim.browseId.isNotEmpty()) {
                            try {
                                val top = getArtistTopTracks(sim.browseId, sim.name)
                                if (top.isNotEmpty()) {
                                    return@async top.shuffled().take(3)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Fallback to search query
                        try {
                            val searchRes = searchMusic(sim.name).filter { track ->
                                track.artist.contains(sim.name, ignoreCase = true) || sim.name.contains(track.artist, ignoreCase = true)
                            }
                            if (searchRes.isNotEmpty()) {
                                return@async searchRes.shuffled().take(3)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        emptyList<MusicTrack>()
                    }
                }
                similarTracksPool.addAll(deferredTracks.awaitAll().flatten())
            }
        }

        // 2. SAFETY FALLBACK: If online similar artists was empty, use STRICT genre family playlists
        if (similarTracksPool.size < 6) {
            val familyPlaylistIds = getGenreFamilyPlaylistIds(profile.name)
            val genrePool = playlists.filter { it.id in familyPlaylistIds }.flatMap { it.tracks }
                .filter { !it.artist.lowercase().contains(artistLower) }
                .shuffled()
            similarTracksPool.addAll(genrePool.take(25))
        }

        // 3. Assemble dynamic random mix:
        val myArtistTracks = profile.topTracks.shuffled()
        val opener = myArtistTracks.firstOrNull()

        val remainingMy = if (opener != null) myArtistTracks.filter { it.id != opener.id }.take(5) else myArtistTracks.take(5)
        val mixedPool = (remainingMy + similarTracksPool).distinctBy { it.id }.shuffled()

        val resultList = mutableListOf<MusicTrack>()
        if (opener != null) {
            resultList.add(opener)
        }
        resultList.addAll(mixedPool)

        return@withContext resultList.distinctBy { it.id }
    }

    /**
     * Starts an infinite station with instant zero-delay tracks.
     * Takes an initial batch of 12-15 tracks from the station's genre or lucky pool,
     * and shuffles them so playback begins immediately.
     */
    suspend fun startInfiniteStation(station: InfiniteGenreStation): List<MusicTrack> = withContext(Dispatchers.IO) {
        val initialTracks = mutableListOf<MusicTrack>()

        if (station.isLucky) {
            // Lucky mode: completely random cross-genre lottery
            val allTracks = playlists.flatMap { it.tracks }.shuffled()
            initialTracks.addAll(allTracks.take(15))
        } else {
            // Genre station: pull from local playlists matching the genre
            val genreTracks = playlists.filter { it.id in station.localPlaylistIds }
                .flatMap { it.tracks }
                .shuffled()
            if (genreTracks.isNotEmpty()) {
                initialTracks.addAll(genreTracks.take(15))
            } else {
                initialTracks.addAll(playlists.flatMap { it.tracks }.shuffled().take(15))
            }
        }

        return@withContext initialTracks.distinctBy { it.id }.shuffled()
    }

    /**
     * Dynamically fetches more tracks on the fly for the active infinite station
     * by querying seed artists or subgenres in YouTube Music, keeping the station endless.
     */
    suspend fun fetchMoreTracksForStation(station: InfiniteGenreStation): List<MusicTrack> = withContext(Dispatchers.IO) {
        val moreTracks = mutableListOf<MusicTrack>()

        try {
            if (station.isLucky) {
                // Pick 2 random non-lucky stations and grab an artist from each
                val otherStations = infiniteStations.filterNot { it.isLucky }
                val randomStation1 = otherStations.randomOrNull()
                val randomStation2 = otherStations.randomOrNull()

                val query1 = randomStation1?.seedArtists?.randomOrNull() ?: "hits"
                val query2 = randomStation2?.seedArtists?.randomOrNull() ?: "music"

                coroutineScope {
                    val d1 = async { searchMusic(query1).shuffled().take(5) }
                    val d2 = async { searchMusic(query2).shuffled().take(5) }
                    moreTracks.addAll(d1.await())
                    moreTracks.addAll(d2.await())
                }
            } else {
                // Genre station: query 2 random seed artists of this genre
                val seeds = station.seedArtists.shuffled().take(2)
                coroutineScope {
                    val deferred = seeds.map { artist ->
                        async {
                            try {
                                searchMusic(artist).filter { track ->
                                    track.artist.contains(artist, ignoreCase = true) || artist.contains(track.artist, ignoreCase = true)
                                }.shuffled().take(4)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    moreTracks.addAll(deferred.awaitAll().flatten())
                }

                // If online search was sparse, supplement from local playlist for that genre
                if (moreTracks.size < 4) {
                    val localGenre = playlists.filter { it.id in station.localPlaylistIds }
                        .flatMap { it.tracks }
                        .shuffled()
                    moreTracks.addAll(localGenre.take(10))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: local tracks
            val localTracks = if (station.isLucky) {
                playlists.flatMap { it.tracks }.shuffled().take(10)
            } else {
                playlists.filter { it.id in station.localPlaylistIds }.flatMap { it.tracks }.shuffled().take(10)
            }
            moreTracks.addAll(localTracks)
        }

        return@withContext moreTracks.distinctBy { it.id }
    }

    /**
     * Loads the complete tracklist for an album or single by its browseId.
     */
    suspend fun getAlbumTracks(album: MusicAlbum): List<MusicTrack> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "es")
                    put("gl", "AR")
                })
            })
            put("browseId", album.browseId)
        }

        val json = executeInnertubePost("https://music.youtube.com/youtubei/v1/browse", payload) ?: return@withContext emptyList()
        val tracks = mutableListOf<MusicTrack>()

        fun parseAlbumTracks(obj: Any) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val item = obj.optJSONObject("musicResponsiveListItemRenderer")
                        if (item != null) {
                            val track = parseYtmItemToTrack(item, album.artist)
                            if (track != null) {
                                tracks.add(track.copy(
                                    album = album.title,
                                    thumbnailUrl = if (track.thumbnailUrl.isEmpty() || track.thumbnailUrl.contains("hqdefault")) album.coverUrl else track.thumbnailUrl
                                ))
                            }
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val child = obj.opt(keys.next())
                        if (child != null) parseAlbumTracks(child)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        val it = obj.opt(i)
                        if (it != null) parseAlbumTracks(it)
                    }
                }
            }
        }

        parseAlbumTracks(json)
        tracks.distinctBy { it.id }
    }

    /**
     * Fetches plain lyrics for a track using lrclib API with automatic title cleaning.
     */
    suspend fun getTrackLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title
                .replace(Regex("""\([^)]*(remaster|live|official|video|audio|mix)[^)]*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\[[^]]*]"""), "")
                .replace(Regex("""-\s*remaster.*""", RegexOption.IGNORE_CASE), "")
                .trim()

            val cleanArtist = artist
                .replace(Regex("""-\s*Topic""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""VEVO""", RegexOption.IGNORE_CASE), "")
                .trim()

            val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(cleanTitle, "UTF-8")
            val urlStr = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "RiffboxTV/1.4.5")

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val text = reader.readText()
                reader.close()
                val json = JSONObject(text)
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotEmpty()) {
                    return@withContext plainLyrics
                }
            }
        } catch (e: Exception) {
            // Fallback search on lrclib
            try {
                val cleanTitle = title.replace(Regex("""\([^)]*\)"""), "").trim()
                val encodedQ = java.net.URLEncoder.encode("$artist $cleanTitle", "UTF-8")
                val searchUrl = URL("https://lrclib.net/api/search?q=$encodedQ")
                val conn = searchUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "RiffboxTV/1.4.5")
                if (conn.responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                    val text = reader.readText()
                    reader.close()
                    val arr = JSONArray(text)
                    if (arr.length() > 0) {
                        val first = arr.optJSONObject(0)
                        val plain = first?.optString("plainLyrics", "") ?: ""
                        if (plain.isNotEmpty()) return@withContext plain
                    }
                }
            } catch (ex: Exception) {
                // Ignore
            }
        }
        null
    }


    private fun extractArtistSummaryFromJson(json: JSONObject): MusicArtistSummary? {
        var summary: MusicArtistSummary? = null
        fun find(obj: Any) {
            if (summary != null) return
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicCardShelfRenderer")) {
                        val card = obj.optJSONObject("musicCardShelfRenderer")
                        if (card != null) {
                            val title = card.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            val subRuns = card.optJSONObject("subtitle")?.optJSONArray("runs")
                            val sb = StringBuilder()
                            if (subRuns != null) {
                                for (i in 0 until subRuns.length()) sb.append(subRuns.optJSONObject(i)?.optString("text", ""))
                            }
                            val sub = sb.toString()
                            val browseId = card.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)
                                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""
                            val thumbs = card.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val thumb = if (thumbs != null && thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: "" else ""

                            if (browseId.isNotEmpty() && title.isNotEmpty()) {
                                summary = MusicArtistSummary(browseId, title, sub, thumb)
                                return
                            }
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val child = obj.opt(keys.next())
                        if (child != null) find(child)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        val it = obj.opt(i)
                        if (it != null) find(it)
                    }
                }
            }
        }
        find(json)
        return summary
    }

    private fun extractYtmTracksFromJson(json: JSONObject): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        fun find(obj: Any) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val item = obj.optJSONObject("musicResponsiveListItemRenderer")
                        if (item != null) {
                            val track = parseYtmItemToTrack(item)
                            if (track != null) tracks.add(track)
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val child = obj.opt(keys.next())
                        if (child != null) find(child)
                    }
                }
                is JSONArray -> {
                    for (i in 0 until obj.length()) {
                        val it = obj.opt(i)
                        if (it != null) find(it)
                    }
                }
            }
        }
        find(json)
        return tracks.distinctBy { it.id }
    }

    private fun parseYtmItemToTrack(item: JSONObject, fallbackArtist: String = "Artista"): MusicTrack? {
        try {
            var vid = item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""

            val flexCols = item.optJSONArray("flexColumns")
            var title = ""
            var artist = fallbackArtist

            if (flexCols != null) {
                val col0 = flexCols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val tRun = col0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)
                title = tRun?.optString("text", "") ?: ""
                if (vid.isEmpty()) {
                    vid = tRun?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""
                }

                if (flexCols.length() > 1) {
                    val col1 = flexCols.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    val aRun = col1?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)
                    val aText = aRun?.optString("text", "") ?: ""
                    if (aText.isNotEmpty()) artist = aText
                }
            }

            if (vid.isEmpty() || title.isEmpty()) return null

            var duration = ""
            val fixedCols = item.optJSONArray("fixedColumns")
            if (fixedCols != null && fixedCols.length() > 0) {
                duration = fixedCols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
            }

            val thumbs = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val thumb = if (thumbs != null && thumbs.length() > 0) {
                thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
            } else {
                "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
            }

            return MusicTrack(id = vid, title = title, artist = artist, duration = duration, thumbnailUrl = thumb)
        } catch (e: Exception) {
            return null
        }
    }

    private fun extractTracksFromJson(json: JSONObject): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        try {
            fun parse(obj: Any) {
                if (obj is JSONObject) {
                    if (obj.has("videoRenderer")) {
                        val vr = obj.optJSONObject("videoRenderer")
                        if (vr != null) {
                            val vid = vr.optString("videoId")
                            val title = vr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                            val artist = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Artista"
                            val duration = vr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                            val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val thumb = if (thumbs != null && thumbs.length() > 0) {
                                thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            } else {
                                "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            }
                            if (vid.isNotEmpty() && title.isNotEmpty()) {
                                tracks.add(MusicTrack(id = vid, title = title, artist = artist, duration = duration, thumbnailUrl = thumb))
                            }
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val child = obj.opt(keys.next())
                        if (child != null) parse(child)
                    }
                } else if (obj is JSONArray) {
                    for (i in 0 until obj.length()) {
                        val item = obj.opt(i)
                        if (item != null) parse(item)
                    }
                }
            }
            parse(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tracks
    }
}

