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

    private fun track(id: String, title: String, artist: String, album: String = "", duration: String = "") =
        MusicTrack(id, title, artist, album, duration, "https://i.ytimg.com/vi/$id/hqdefault.jpg")

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
                track("qKggnBh2vVU", "Smoke on the Water", "Deep Purple", "Machine Head", "5:40"),
                track("pAgnJDJN4VA", "Back in Black", "AC/DC", "Back in Black", "4:15"),
                track("l482T0yNkeo", "Highway to Hell", "AC/DC", "Highway to Hell", "3:28"),
                track("v2AC41dglnM", "Thunderstruck", "AC/DC", "The Razors Edge", "4:52"),
                track("ZakzPmvT5WU", "You Shook Me All Night Long", "AC/DC", "Back in Black", "3:30"),
                track("HQMm7l6XQ4E", "Whole Lotta Love", "Led Zeppelin", "Led Zeppelin II", "5:34"),
                track("QkF3oxziUI4", "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "8:02"),
                track("6tlSx0jKUlm", "Black Dog", "Led Zeppelin", "Led Zeppelin IV", "4:56"),
                track("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "5:55"),
                track("-tJYN-eG1zk", "We Will Rock You", "Queen", "News of the World", "2:01"),
                track("rY0WxgSXdEE", "Another One Bites the Dust", "Queen", "The Game", "3:35"),
                track("1w7OgIMMRc4", "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", "5:03"),
                track("o1tj2zJ2Wvg", "Welcome to the Jungle", "Guns N' Roses", "Appetite for Destruction", "4:33"),
                track("8SbUC-UaAxE", "November Rain", "Guns N' Roses", "Use Your Illusion I", "8:57"),
                track("Rbm6GX4P650", "Paradise City", "Guns N' Roses", "Appetite for Destruction", "6:49"),
                track("CD-E-LDc384", "Enter Sandman", "Metallica", "Metallica", "5:31"),
                track("0qanF-91aJo", "Paranoid", "Black Sabbath", "Paranoid", "2:48"),
                track("5s7_WbiR640", "Iron Man", "Black Sabbath", "Paranoid", "5:56"),
                track("LQUXuQ6Zd9w", "War Pigs", "Black Sabbath", "Paranoid", "7:55"),
                track("WGoDaYjdfSg", "Purple Haze", "Jimi Hendrix", "Are You Experienced", "2:51"),
                track("qFfnlHbBn6A", "Voodoo Child", "Jimi Hendrix", "Electric Ladyland", "5:12"),
                track("TLV4_xx4URk", "All Along the Watchtower", "Jimi Hendrix", "Electric Ladyland", "4:00"),
                track("nrIPxlFzDi0", "(I Can't Get No) Satisfaction", "The Rolling Stones", "Out of Our Heads", "3:43"),
                track("O4irXQhgMqg", "Paint It, Black", "The Rolling Stones", "Aftermath", "3:22"),
                track("SGyOaCXr8Lw", "Start Me Up", "The Rolling Stones", "Tattoo You", "3:33"),
                track("SHhrZgojY1Q", "Won't Get Fooled Again", "The Who", "Who's Next", "8:32"),
                track("x2KRpRMSu4g", "Baba O'Riley", "The Who", "Who's Next", "5:00"),
                track("zt51r9GYwy4", "Sunshine of Your Love", "Cream", "Disraeli Gears", "4:10"),
                track("uSquiIVLhrQ", "Layla", "Derek and the Dominos", "Layla", "7:05"),
                track("4B_UYYPb-Gk", "Walk This Way", "Aerosmith", "Toys in the Attic", "3:40"),
                track("sZfZ8uWaKgk", "Dream On", "Aerosmith", "Aerosmith", "4:28"),
                track("SwYN7mTi6HM", "Jump", "Van Halen", "1984", "4:04"),
                track("fuKDBPw8wQA", "Panama", "Van Halen", "1984", "3:32"),
                track("Y-IUB62zSuA", "Ain't Talkin' 'Bout Love", "Van Halen", "Van Halen", "3:50"),
                track("Wr9ie2J2690", "Highway Star", "Deep Purple", "Machine Head", "6:05"),
                track("LCnebZnysmI", "Burn", "Deep Purple", "Burn", "6:00"),
                track("RMR5zf558Wo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
                track("L397TWLwrUU", "Breaking the Law", "Judas Priest", "British Steel", "2:35"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("rG6b94o_6lI", "La Grange", "ZZ Top", "Tres Hombres", "3:52"),
                track("7wRHBLwpASw", "Sharp Dressed Man", "ZZ Top", "Eliminator", "4:14"),
                track("0vo23H9J8o8", "Pride and Joy", "Stevie Ray Vaughan", "Texas Flood", "3:40"),
                track("0fAQH8ArMzs", "Sultans of Swing", "Dire Straits", "Dire Straits", "5:47"),
                track("wTP2RUD_cL0", "Money for Nothing", "Dire Straits", "Brothers in Arms", "4:38"),
                track("_FrOQC-zEog", "Comfortably Numb", "Pink Floyd", "The Wall", "6:23"),
                track("IXdNnw99-Ic", "Wish You Were Here", "Pink Floyd", "Wish You Were Here", "5:34"),
                track("t4QK8RxCAwo", "More Than a Feeling", "Boston", "Boston", "4:45"),
                track("2X_2Idop9pc", "Carry On Wayward Son", "Kansas", "Leftoverture", "5:23"),
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
                track("AhcttcXc5-0", "About a Girl", "Nirvana", "Bleach", "2:48"),
                track("MS91knuzoOA", "Alive", "Pearl Jam", "Ten", "5:41"),
                track("CxKWTzr-k6s", "Even Flow", "Pearl Jam", "Ten", "4:53"),
                track("xM-sB0m23wI", "Jeremy", "Pearl Jam", "Ten", "5:18"),
                track("4q9UafsiQ6k", "Black", "Pearl Jam", "Ten", "5:43"),
                track("a-2sZ1u4Z4Y", "Better Man", "Pearl Jam", "Vitalogy", "4:28"),
                track("3mbBbFH9fAg", "Black Hole Sun", "Soundgarden", "Superunknown", "5:18"),
                track("T0_zzCllLkE", "Spoonman", "Soundgarden", "Superunknown", "4:06"),
                track("ySzrJ4GRF7s", "Fell on Black Days", "Soundgarden", "Superunknown", "4:42"),
                track("sNh-iw7gsuI", "Outshined", "Soundgarden", "Badmotorfinger", "5:11"),
                track("pBZs_Py-1_0", "Rusty Cage", "Soundgarden", "Badmotorfinger", "4:26"),
                track("TAqZb52sgpU", "Man in the Box", "Alice In Chains", "Facelift", "4:44"),
                track("Nco_kh8xJjc", "Would?", "Alice In Chains", "Dirt", "3:27"),
                track("uAE6Il6OTcs", "Rooster", "Alice In Chains", "Dirt", "6:15"),
                track("zTuD8k3JvxQ", "Them Bones", "Alice In Chains", "Dirt", "2:30"),
                track("_9p_3qfE6F4", "Nutshell", "Alice In Chains", "Jar of Flies", "4:19"),
                track("f8hT3oDDf6c", "Down in a Hole", "Alice In Chains", "Dirt", "5:38"),
                track("V5UOC0C0x8Q", "Plush", "Stone Temple Pilots", "Core", "5:13"),
                track("sT1LovQ_S3U", "Creep", "Stone Temple Pilots", "Core", "5:33"),
                track("yjJL9DGU7Gg", "Interstate Love Song", "Stone Temple Pilots", "Purple", "3:14"),
                track("ht672-wYelc", "Vasoline", "Stone Temple Pilots", "Purple", "2:56"),
                track("sH_mHZZsZ3M", "Wicked Garden", "Stone Temple Pilots", "Core", "4:05"),
                track("NCO_kZAbE4Q", "1979", "The Smashing Pumpkins", "Mellon Collie", "4:24"),
                track("8-r-V0uK4S0", "Bullet with Butterfly Wings", "The Smashing Pumpkins", "Mellon Collie", "4:18"),
                track("q-KE9lvU810", "Cherub Rock", "The Smashing Pumpkins", "Siamese Dream", "4:58"),
                track("NOG3eus4ZSo", "Tonight, Tonight", "The Smashing Pumpkins", "Mellon Collie", "4:15"),
                track("d1acEVmnVhI", "Disarm", "The Smashing Pumpkins", "Siamese Dream", "3:17"),
                track("RZD982yrmx4", "Tomorrow", "Silverchair", "Frogstomp", "4:26"),
                track("zRJcxF05t38", "Freak", "Silverchair", "Freak Show", "3:49"),
                track("3N3d735jTz8", "Pure Massacre", "Silverchair", "Frogstomp", "4:58"),
                track("VUb450Alpps", "Hunger Strike", "Temple of the Dog", "Temple of the Dog", "4:03"),
                track("5qi8h3RUHvs", "Say Hello 2 Heaven", "Temple of the Dog", "Temple of the Dog", "6:22"),
                track("Fm72DPJCX58", "River of Deceit", "Mad Season", "Above", "5:04"),
                track("dE6_Wb_o220", "Nearly Lost You", "Screaming Trees", "Sweet Oblivion", "4:07"),
                track("pU6KhxCvRaU", "Glycerine", "Bush", "Sixteen Stone", "4:26"),
                track("5WPbqYoz9HA", "Machinehead", "Bush", "Sixteen Stone", "4:16"),
                track("xS3GiUq5Oj8", "Comedown", "Bush", "Sixteen Stone", "5:26"),
                track("SBjQ9tuuKSQ", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("I7rCNiiNPxA", "Monkey Wrench", "Foo Fighters", "The Colour and the Shape", "3:51"),
                track("EqWRaAFb_WY", "My Hero", "Foo Fighters", "The Colour and the Shape", "4:20"),
                track("O3dWBLoU--E", "Celebrity Skin", "Hole", "Celebrity Skin", "2:42"),
                track("g_483j82ZpM", "Jane Says", "Jane's Addiction", "Nothing's Shocking", "4:52"),
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
                track("F0iWc9Lb6t4", "Cuando Pase el Temblor", "Soda Stereo", "Nada Personal", "3:49"),
                track("x2w7P2W8xoc", "Prófugos", "Soda Stereo", "Signos", "5:20"),
                track("WjFf_b4RrhM", "Trátame Suavemente", "Soda Stereo", "Soda Stereo", "3:22"),
                track("I6eC2e_L4f8", "Ella Usó Mi Cabeza Como un Revólver", "Soda Stereo", "Sueño Stereo", "4:31"),
                track("a4q77o9J-5M", "Nada Personal", "Soda Stereo", "Nada Personal", "4:52"),
                track("K4cZ78p0q0c", "Signos", "Soda Stereo", "Signos", "5:15"),
                track("x4f7m98zHk8", "Un Misil en Mi Placard", "Soda Stereo", "Comfort y Música Para Volar", "4:34"),
                track("y5X31wU9E9s", "Demoliendo Hoteles", "Charly García", "Piano Bar", "2:18"),
                track("q4f7V311-hY", "Nos Siguen Pegando Abajo", "Charly García", "Clics Modernos", "3:30"),
                track("k9H7347v78g", "Promesas Sobre el Bidet", "Charly García", "Piano Bar", "2:46"),
                track("k3L7p_t0m6M", "Yendo de la Cama al Living", "Charly García", "Yendo de la Cama al Living", "4:42"),
                track("46c5mG-k8pY", "Cerca de la Revolución", "Charly García", "Piano Bar", "4:42"),
                track("a2v751Z03w8", "Hablando a tu Corazón", "Charly García", "Tango", "2:14"),
                track("X6m7gM4ZpQs", "Rezo por Vos", "Charly García & Luis Alberto Spinetta", "Privé", "4:30"),
                track("f7v44243k10", "No Voy en Tren", "Charly García", "Parte de la Religión", "3:03"),
                track("a2741mK-76w", "Fanky", "Charly García", "Cómo Conseguir Chicas", "4:45"),
                track("J_e9L2hB3s0", "Ji Ji Ji", "Patricio Rey y sus Redonditos de Ricota", "Oktubre", "5:32"),
                track("m9V73Zk0184", "La Bestia Pop", "Patricio Rey y sus Redonditos de Ricota", "Gulp!", "3:42"),
                track("d68841Z0p1Y", "Un Poco de Amor Francés", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "3:27"),
                track("f67384p010w", "Juguetes Perdidos", "Patricio Rey y sus Redonditos de Ricota", "Luzbelito", "7:10"),
                track("k38741Z-m0Y", "Queso Ruso", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "4:52"),
                track("f738419Z-9w", "Mi Perro Dinamita", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "2:35"),
                track("k7384p-1111", "El Pibe de los Astilleros", "Patricio Rey y sus Redonditos de Ricota", "La Mosca y la Sopa", "3:32"),
                track("G8c3_00k_6s", "Ala Delta", "Divididos", "Acariciando lo Áspero", "4:12"),
                track("Wp8723m018k", "El 38", "Divididos", "Acariciando lo Áspero", "2:31"),
                track("c83711m001w", "Spaghetti del Rock", "Divididos", "Narigón del Siglo", "3:32"),
                track("f7344199-kw", "¿Qué Ves?", "Divididos", "La Era de la Boludez", "5:14"),
                track("d74883Z-k18", "Salir a Asustar", "Divididos", "La Era de la Boludez", "3:21"),
                track("k78341Z010w", "La Rubia Tarada", "Sumo", "Divididos por la Felicidad", "3:40"),
                track("m74831Z018w", "Mejor No Hablar de Ciertas Cosas", "Sumo", "Divididos por la Felicidad", "4:42"),
                track("k4834199-8w", "Los Viejos Vinagres", "Sumo", "Llegando los Monos", "3:15"),
                track("d73841m-99w", "Mañana en el Abasto", "Sumo", "After Chabón", "4:08"),
                track("m98411Z-90k", "Será", "Las Pelotas", "Esperando el Milagro", "3:02"),
                track("f83741Z001w", "Bombachitas Rosas", "Las Pelotas", "Máscaras de Sal", "3:15"),
                track("k7488311-0w", "La Balada del Diablo y la Muerte", "La Renga", "Despedazado por Mil Partes", "5:36"),
                track("m7384199-kw", "El Revelde", "La Renga", "La Renga", "3:32"),
                track("d7488311-kw", "Panic Show", "La Renga", "La Esquina del Infinito", "3:01"),
                track("m74831Z001w", "Verano del 92", "Los Piojos", "3er Arco", "4:40"),
                track("d73841Z011k", "El Farolito", "Los Piojos", "3er Arco", "4:14"),
                track("f73841Z002w", "Bicho de Ciudad", "Los Piojos", "Civilización", "5:12"),
                track("k74883Z010w", "Tan Solo", "Los Piojos", "Chactuchac", "4:00"),
                track("m98411Z018w", "Muchacha Ojos de Papel", "Almendra", "Almendra", "3:07"),
                track("f83741Z011w", "Bajan", "Pescado Rabioso", "Artaud", "3:26"),
                track("d73841Z012k", "Seguir Viviendo Sin Tu Amor", "Luis Alberto Spinetta", "Pelusón of Milk", "2:40"),
                track("m74831Z003w", "Seminare", "Serú Girán", "Serú Girán", "3:27"),
                track("f73841Z004w", "Peperina", "Serú Girán", "Peperina", "3:40"),
                track("k74883Z011w", "Mariposa Tecknicolor", "Fito Páez", "Circo Beat", "3:42"),
                track("d73841Z013k", "11 y 6", "Fito Páez", "Giros", "3:15"),
                track("m74831Z004w", "El Amor Después del Amor", "Fito Páez", "El Amor Después del Amor", "5:10"),
                track("k74883Z012w", "Flaca", "Andrés Calamaro", "Alta Suciedad", "4:47"),
                track("f73841Z005w", "Mil Horas", "Los Abuelos de la Nada", "Vasos y Besos", "2:50"),
                track("m98411Z019w", "Lamento Boliviano", "Los Enanitos Verdes", "Big Bang", "3:42")
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
                track("B_HSa1dEL4U", "For Whom the Bell Tolls", "Metallica", "Ride the Lightning", "5:10"),
                track("W_bYh8vQYQo", "Seek & Destroy", "Metallica", "Kill 'Em All", "6:55"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("qebw_1F5L_E", "Fear of the Dark", "Iron Maiden", "Fear of the Dark", "7:18"),
                track("7-iRf9AWoyE", "The Number of the Beast", "Iron Maiden", "The Number of the Beast", "4:50"),
                track("BBtLPEYXVeM", "Hallowed Be Thy Name", "Iron Maiden", "The Number of the Beast", "7:12"),
                track("86URGgqONvA", "Run to the Hills", "Iron Maiden", "The Number of the Beast", "3:53"),
                track("Ij99dud8-0A", "Wasted Years", "Iron Maiden", "Somewhere in Time", "5:08"),
                track("9d4ui9q7eDM", "Holy Wars... The Punishment Due", "Megadeth", "Rust in Peace", "6:32"),
                track("Wpdfl70Yx-k", "Symphony of Destruction", "Megadeth", "Countdown to Extinction", "4:02"),
                track("67bTy696enc", "Peace Sells", "Megadeth", "Peace Sells... But Who's Buying?", "4:02"),
                track("B-oU2BlA8bi", "Hangar 18", "Megadeth", "Rust in Peace", "5:14"),
                track("L397TWLwrUU", "Painkiller", "Judas Priest", "Painkiller", "6:06"),
                track("L397TWLwrUU", "Breaking the Law", "Judas Priest", "British Steel", "2:35"),
                track("b1B_pZC8aWU", "Electric Eye", "Judas Priest", "Screaming for Vengeance", "3:39"),
                track("WTuqP39121U", "You've Got Another Thing Comin'", "Judas Priest", "Screaming for Vengeance", "5:09"),
                track("AkFqg5wAuFk", "Walk", "Pantera", "Vulgar Display of Power", "5:15"),
                track("i9kJ9bTwt60", "Cowboys from Hell", "Pantera", "Cowboys from Hell", "4:06"),
                track("WyczwqVISnA", "Cemetery Gates", "Pantera", "Cowboys from Hell", "7:03"),
                track("7m7njvwB-Ks", "5 Minutes Alone", "Pantera", "Far Beyond Driven", "5:50"),
                track("0qanF-91aJo", "Paranoid", "Black Sabbath", "Paranoid", "2:48"),
                track("5s7_WbiR640", "Iron Man", "Black Sabbath", "Paranoid", "5:56"),
                track("LQUXuQ6Zd9w", "War Pigs", "Black Sabbath", "Paranoid", "7:55"),
                track("RMR5zf558Wo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
                track("G3LvhdFEOqs", "Mr. Crowley", "Ozzy Osbourne", "Blizzard of Ozz", "5:02"),
                track("bnNWUUZ7cEA", "Bark at the Moon", "Ozzy Osbourne", "Bark at the Moon", "4:17"),
                track("z8ZqFlw6hYg", "Raining Blood", "Slayer", "Reign in Blood", "4:14"),
                track("K6_zsJ8KPP0", "Angel of Death", "Slayer", "Reign in Blood", "4:51"),
                track("Xtly_Q49K3k", "South of Heaven", "Slayer", "South of Heaven", "4:59"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("ueeEWRz1F54", "Overkill", "Motörhead", "Overkill", "5:12"),
                track("uGHsxMqpL0c", "Madhouse", "Anthrax", "Spreading the Disease", "4:20"),
                track("7T_E3Z7c3-g", "Caught in a Mosh", "Anthrax", "Among the Living", "4:59"),
                track("bkysjsc570c", "Holy Diver", "Dio", "Holy Diver", "4:16"),
                track("LmSt1PvovvU", "Rainbow in the Dark", "Dio", "Holy Diver", "4:15"),
                track("B_3TlrZLp0c", "Balls to the Wall", "Accept", "Balls to the Wall", "5:43"),
                track("F_6IjepnG9o", "Roots Bloody Roots", "Sepultura", "Roots", "3:32"),
                track("6ODNxy3YOPU", "Refuse/Resist", "Sepultura", "Chaos A.D.", "3:19"),
                track("6yP1tcy9a10", "Rock You Like a Hurricane", "Scorpions", "Love at First Sting", "4:12"),
                track("FjV8443QSCo", "I Want Out", "Helloween", "Keeper of the Seven Keys Part II", "4:40"),
                track("CSvFpBO48dQ", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("iywaBOMvYLI", "Toxicity", "System of a Down", "Toxicity", "3:38"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("6fVE8k50JaU", "Duality", "Slipknot", "Vol. 3: (The Subliminal Verses)", "4:12"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("09LTT0sfCRg", "Down with the Sickness", "Disturbed", "The Sickness", "4:38")
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
                track("Gd9OhYroKA0", "Crawling", "Linkin Park", "Hybrid Theory", "3:29"),
                track("SBjQ9tuuKSQ", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("9jK-NcRmVcw", "The Pretender", "Foo Fighters", "Echoes, Silence, Patience & Grace", "4:30"),
                track("h_L44Bi805g", "Best of You", "Foo Fighters", "In Your Honor", "4:16"),
                track("1VQ_3sBZRS0", "Learn to Fly", "Foo Fighters", "There Is Nothing Left to Lose", "3:55"),
                track("NUTGr5t3Mo8", "Basket Case", "Green Day", "Dookie", "3:01"),
                track("Soa3gO7tL-c", "Boulevard of Broken Dreams", "Green Day", "American Idiot", "4:21"),
                track("Ee_uGuOrUO8", "American Idiot", "Green Day", "American Idiot", "2:54"),
                track("NU9JoFK5PZZ", "Wake Me Up When September Ends", "Green Day", "American Idiot", "4:45"),
                track("98W9QuMq-2k", "All the Small Things", "Blink-182", "Enema of the State", "2:47"),
                track("K7l5ZeVVoCA", "What's My Age Again?", "Blink-182", "Enema of the State", "2:28"),
                track("s1tAYmMjLdY", "I Miss You", "Blink-182", "Blink-182", "3:47"),
                track("vVy9Lgpg1m8", "First Date", "Blink-182", "Take Off Your Pants and Jacket", "2:51"),
                track("VrZ4sMRYimw", "The Kids Aren't Alright", "The Offspring", "Americana", "3:00"),
                track("eeWjzBH8Dtk", "Self Esteem", "The Offspring", "Smash", "4:17"),
                track("AltFeOba504", "Pretty Fly (For a White Guy)", "The Offspring", "Americana", "3:08"),
                track("5_LxyhCJpsM", "You're Gonna Go Far, Kid", "The Offspring", "Rise and Fall, Rage and Grace", "2:57"),
                track("YlUKcNNmywk", "Californication", "Red Hot Chili Peppers", "Californication", "5:21"),
                track("rn_YodiJO6k", "Otherside", "Red Hot Chili Peppers", "Californication", "4:15"),
                track("8DyziWtkfBw", "Can't Stop", "Red Hot Chili Peppers", "By the Way", "4:29"),
                track("mzJj5-lubeM", "Scar Tissue", "Red Hot Chili Peppers", "Californication", "3:37"),
                track("gGdGFtwCNBE", "Mr. Brightside", "The Killers", "Hot Fuss", "3:42"),
                track("Y5fBdpreJi4", "Somebody Told Me", "The Killers", "Hot Fuss", "3:17"),
                track("w8KQmps-Sog", "Uprising", "Muse", "The Resistance", "5:03"),
                track("Xsp3_a-PMTw", "Supermassive Black Hole", "Muse", "Black Holes and Revelations", "3:29"),
                track("3dm_5qWWDV8", "Hysteria", "Muse", "Absolution", "3:47"),
                track("bpOSxM0rNPM", "Do I Wanna Know?", "Arctic Monkeys", "AM", "4:32"),
                track("pK7egVaTviE", "I Bet You Look Good on the Dancefloor", "Arctic Monkeys", "Whatever People Say I Am...", "2:53"),
                track("7QU1nvuxaMA", "Like a Stone", "Audioslave", "Audioslave", "4:54"),
                track("KblA1jD-N_E", "Cochise", "Audioslave", "Audioslave", "3:42"),
                track("3YxaaGgTQYM", "Bring Me to Life", "Evanescence", "Fallen", "3:57"),
                track("j0lSpNQQ9-8", "Last Resort", "Papa Roach", "Infest", "3:19"),
                track("RYnFIRc0k6E", "Rollin'", "Limp Bizkit", "Chocolate Starfish...", "3:33"),
                track("ZpUYjpKg9KY", "Break Stuff", "Limp Bizkit", "Significant Other", "2:46"),
                track("CMX2lPum_pg", "Fat Lip", "Sum 41", "All Killer No Filler", "2:58"),
                track("emGri7i8Y2Y", "In Too Deep", "Sum 41", "All Killer No Filler", "3:27"),
                track("desJKYvdq9A", "The Anthem", "Good Charlotte", "The Young and the Hopeless", "2:55"),
                track("RRKJiM9Njr8", "Welcome to the Black Parade", "My Chemical Romance", "The Black Parade", "5:11"),
                track("UCCyoocDxWI", "Helena", "My Chemical Romance", "Three Cheers for Sweet Revenge", "3:22"),
                track("toYZFeH8gwg", "Last Nite", "The Strokes", "Is This It", "3:13"),
                track("b8-t4LD22zs", "Reptilia", "The Strokes", "Room on Fire", "3:39"),
                track("GhCXAiNz9Ao", "Take Me Out", "Franz Ferdinand", "Franz Ferdinand", "3:57"),
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
                track("zguCFjHyVeM", "Crystal Mountain", "Death", "Symbolic", "5:06"),
                track("gT37p_s3v4A", "The Philosopher", "Death", "Individual Thought Patterns", "4:13"),
                track("SnTL1L8N6Yg", "Blow Your Trumpets Gabriel", "Behemoth", "The Satanist", "4:25"),
                track("qZXs_V4v4c8", "O Father O Satan O Sun!", "Behemoth", "The Satanist", "7:13"),
                track("edBYB1VCV0k", "Twilight of the Thunder God", "Amon Amarth", "Twilight of the Thunder God", "4:09"),
                track("bLq8k-6r4qE", "The Way of Vikings", "Amon Amarth", "Jomsviking", "5:11"),
                track("ZPRt6Tt6RyM", "The Pursuit of Vikings", "Amon Amarth", "Fate of Norns", "4:30"),
                track("joEAb_mxEyw", "Progenies of the Great Apocalypse", "Dimmu Borgir", "Death Cult Armageddon", "5:17"),
                track("4b0WwB_q0kU", "Mourning Palace", "Dimmu Borgir", "Enthrone Darkness Triumphant", "5:13"),
                track("n9AcG03R120", "Nemesis", "Arch Enemy", "Doomsday Machine", "4:12"),
                track("mjF1rmSV1dM", "The Eagle Flies Alone", "Arch Enemy", "Will to Power", "4:59"),
                track("InRMwptcgAo", "War Eternal", "Arch Enemy", "War Eternal", "4:16"),
                track("0-P_h2pG6hE", "Needled 24/7", "Children of Bodom", "Hate Crew Deathroll", "4:08"),
                track("yLAjKmdwUNQ", "In Your Face", "Children of Bodom", "Are You Dead Yet?", "4:16"),
                track("f0aI3fBv51A", "Are You Dead Yet?", "Children of Bodom", "Are You Dead Yet?", "3:54"),
                track("6AP567t_c3c", "Nymphetamine Fix", "Cradle of Filth", "Nymphetamine", "5:02"),
                track("qRNfeMaBR3U", "Her Ghost in the Fog", "Cradle of Filth", "Midian", "6:24"),
                track("28rE4ji44s4", "Ghost of Perdition", "Opeth", "Ghost Reveries", "10:29"),
                track("FNdC_3LR2AI", "Stranded", "Gojira", "Magma", "4:29"),
                track("oqdZpxkzNvc", "Redneck", "Lamb of God", "Sacrament", "3:41"),
                track("HL9kaJZLD84", "Laid to Rest", "Lamb of God", "Ashes of the Wake", "3:50"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("CSvFpBO48dQ", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("6ODNxy3YOPU", "Refuse/Resist", "Sepultura", "Chaos A.D.", "3:19"),
                track("F_6IjepnG9o", "Roots Bloody Roots", "Sepultura", "Roots", "3:32"),
                track("K6_zsJ8KPP0", "Angel of Death", "Slayer", "Reign in Blood", "4:51"),
                track("z8ZqFlw6hYg", "Raining Blood", "Slayer", "Reign in Blood", "4:14"),
                track("Xtly_Q49K3k", "South of Heaven", "Slayer", "South of Heaven", "4:59"),
                track("L397TWLwrUU", "Painkiller", "Judas Priest", "Painkiller", "6:06"),
                track("AkFqg5wAuFk", "Walk", "Pantera", "Vulgar Display of Power", "5:15"),
                track("i9kJ9bTwt60", "Cowboys from Hell", "Pantera", "Cowboys from Hell", "4:06"),
                track("9d4ui9q7eDM", "Holy Wars... The Punishment Due", "Megadeth", "Rust in Peace", "6:32"),
                track("2f1Ny74_ou0", "Master of Puppets", "Metallica", "Master of Puppets", "8:35"),
                track("B_3TlrZLp0c", "Balls to the Wall", "Accept", "Balls to the Wall", "5:43"),
                track("FjV8443QSCo", "I Want Out", "Helloween", "Keeper of the Seven Keys Part II", "4:40"),
                track("bkysjsc570c", "Holy Diver", "Dio", "Holy Diver", "4:16"),
                track("LmSt1PvovvU", "Rainbow in the Dark", "Dio", "Holy Diver", "4:15"),
                track("3mbvWn1EY6g", "Ace of Spades", "Motörhead", "Ace of Spades", "2:48"),
                track("ueeEWRz1F54", "Overkill", "Motörhead", "Overkill", "5:12"),
                track("uGHsxMqpL0c", "Madhouse", "Anthrax", "Spreading the Disease", "4:20"),
                track("7T_E3Z7c3-g", "Caught in a Mosh", "Anthrax", "Among the Living", "4:59"),
                track("09LTT0sfCRg", "Down with the Sickness", "Disturbed", "The Sickness", "4:38"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("6yP1tcy9a10", "Rock You Like a Hurricane", "Scorpions", "Love at First Sting", "4:12"),
                track("WxnN05vOuSM", "The Trooper", "Iron Maiden", "Piece of Mind", "4:12"),
                track("qebw_1F5L_E", "Fear of the Dark", "Iron Maiden", "Fear of the Dark", "7:18"),
                track("7-iRf9AWoyE", "The Number of the Beast", "Iron Maiden", "The Number of the Beast", "4:50"),
                track("BBtLPEYXVeM", "Hallowed Be Thy Name", "Iron Maiden", "The Number of the Beast", "7:12")
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
                track("q7488311-0w", "Tu Eres Su Seguridad", "Hermética", "Hermética", "5:04"),
                track("m7384199-kw", "Evitando el Ablande", "Hermética", "Ácido Argentino", "2:54"),
                track("d7488311-kw", "Soy de la Esquina", "Hermética", "Víctimas del Vaciamiento", "3:06"),
                track("k74883Z010w", "Del Camionero", "Hermética", "Ácido Argentino", "4:31"),
                track("m74831Z001w", "Memoria de Siglos", "Hermética", "Ácido Argentino", "5:45"),
                track("d73841Z011k", "Gil Trabajador", "Hermética", "Ácido Argentino", "3:10"),
                track("f73841Z002w", "Atravesando Todo Límite", "Hermética", "Víctimas del Vaciamiento", "4:00"),
                track("k74883Z011w", "Masa Anestesiada", "Hermética", "Hermética", "4:43"),
                track("d73841Z013k", "Sé Vos", "Almafuerte", "Mundo Guanaco", "4:03"),
                track("m74831Z004w", "El Pibe Tigre", "Almafuerte", "Mundo Guanaco", "4:28"),
                track("k74883Z012w", "A Vos Amigo", "Almafuerte", "Del Entorno", "4:50"),
                track("f73841Z005w", "Toro y Pampa", "Almafuerte", "Toro y Pampa", "3:40"),
                track("m98411Z019w", "Almafuerte", "Almafuerte", "Almafuerte", "3:55"),
                track("d7488311-1w", "Destrucción", "V8", "Luchando por el Metal", "2:01"),
                track("k74883Z013w", "Brigadas Metálicas", "V8", "Luchando por el Metal", "3:03"),
                track("m74831Z005w", "Luchando por el Metal", "V8", "Luchando por el Metal", "2:58"),
                track("d73841Z014k", "Cautivos del Sistema", "V8", "Luchando por el Metal", "3:40"),
                track("f73841Z006w", "Síntoma de la Infección", "Malón", "Espíritu Combativo", "4:05"),
                track("k74883Z014w", "Castigador por Herencia", "Malón", "Espíritu Combativo", "3:35"),
                track("d73841Z015k", "Gato Negro", "Malón", "Espíritu Combativo", "3:20"),
                track("wQ4Y1a1Z03w", "Mujer Amante", "Rata Blanca", "Magos, Espadas y Rosas", "6:05"),
                track("k94Y1a1Z03w", "La Leyenda del Hada y el Mago", "Rata Blanca", "Magos, Espadas y Rosas", "5:24"),
                track("m84Y1a1Z03w", "Guerrero del Arco Iris", "Rata Blanca", "Guerrero del Arco Iris", "5:03"),
                track("d74Y1a1Z03w", "Volviendo a Casa", "Rata Blanca", "El Camino del Fuego", "5:27"),
                track("f64Y1a1Z03w", "Esperanza", "Horcas", "Vence", "4:15"),
                track("k54Y1a1Z03w", "Vencer", "Horcas", "Vence", "3:45"),
                track("m44Y1a1Z03w", "Fuego", "Horcas", "Eternos", "4:00"),
                track("q24Y1a1Z03w", "Sacate la Mierda", "Carajo", "Carajo", "3:25"),
                track("m14Y1a1Z03w", "El Mar de las Almas", "Carajo", "El Mar de las Almas", "4:37"),
                track("d04Y1a1Z03w", "Luna Hueca", "Carajo", "Frente a Frente", "4:12"),
                track("k93Y1a1Z03w", "Triste", "Carajo", "Atrapasueños", "4:00"),
                track("m73Y1a1Z03w", "El Nuevo Camino del Hombre", "A.N.I.M.A.L.", "El Nuevo Camino del Hombre", "3:40"),
                track("d63Y1a1Z03w", "Sólo por Ser Indios", "A.N.I.M.A.L.", "Poder Latino", "3:30"),
                track("k53Y1a1Z03w", "Combatiente del Metal", "A.N.I.M.A.L.", "A.N.I.M.A.L. 6", "3:15"),
                track("m33Y1a1Z03w", "Tierra Negra", "Tren Loco", "Tierra Negra", "4:30"),
                track("d23Y1a1Z03w", "Clase Trabajadora", "Tren Loco", "No Me Importa Morir", "3:40"),
                track("k13Y1a1Z03w", "Se Extraña el Dolor", "O'Connor", "Hay un Lugar", "4:12"),
                track("f03Y1a1Z03w", "Caníbal", "O'Connor", "Dolor", "3:50"),
                track("m92Y1a1Z03w", "Como Relámpago en la Oscuridad", "Logos", "La Industria del Poder", "4:20"),
                track("d82Y1a1Z03w", "No Te Rindas", "Logos", "Generación Mutable", "3:45"),
                track("G8c3_00k_6s", "Ala Delta", "Divididos", "Acariciando lo Áspero", "4:12"),
                track("Wp8723m018k", "El 38", "Divididos", "Acariciando lo Áspero", "2:31"),
                track("k7488311-0w", "La Balada del Diablo y la Muerte", "La Renga", "Despedazado por Mil Partes", "5:36"),
                track("m7384199-kw", "El Revelde", "La Renga", "La Renga", "3:32"),
                track("d7488311-kw", "Panic Show", "La Renga", "La Esquina del Infinito", "3:01"),
                track("J_e9L2hB3s0", "Ji Ji Ji", "Patricio Rey y sus Redonditos de Ricota", "Oktubre", "5:32"),
                track("f67384p010w", "Juguetes Perdidos", "Patricio Rey y sus Redonditos de Ricota", "Luzbelito", "7:10"),
                track("sWft4cMiwXQ", "De Música Ligera", "Soda Stereo", "Canción Animal", "3:32"),
                track("y5X31wU9E9s", "Demoliendo Hoteles", "Charly García", "Piano Bar", "2:18"),
                track("k78341Z010w", "La Rubia Tarada", "Sumo", "Divididos por la Felicidad", "3:40")
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
                track("h_D3VFkatNY", "Smooth Criminal", "Michael Jackson", "Bad", "4:17"),
                track("79fzeNUqQbQ", "Like a Prayer", "Madonna", "Like a Prayer", "5:41"),
                track("GuJQEYlkvgk", "Vogue", "Madonna", "I'm Breathless", "4:50"),
                track("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "After Hours", "3:20"),
                track("XXYlFuWEuKI", "Save Your Tears", "The Weeknd", "After Hours", "3:36"),
                track("oygrmJFKYZY", "Don't Start Now", "Dua Lipa", "Future Nostalgia", "3:03"),
                track("TUVcZfQe-Kw", "Levitating", "Dua Lipa", "Future Nostalgia", "3:23"),
                track("OPf0YbXqDm0", "Uptown Funk", "Bruno Mars", "Uptown Special", "4:30"),
                track("UqyT8IEBkvY", "24K Magic", "Bruno Mars", "24K Magic", "3:46"),
                track("5NV6Rdv1a3I", "Get Lucky", "Daft Punk", "Random Access Memories", "4:08"),
                track("FGBhQbmMxH8", "One More Time", "Daft Punk", "Discovery", "5:20"),
                track("xFrGuyw1V8s", "Dancing Queen", "ABBA", "Arrival", "3:51"),
                track("XEjLoHdbVeE", "Gimme! Gimme! Gimme!", "ABBA", "Voulez-Vous", "4:49"),
                track("izGwDsrQ1eQ", "Careless Whisper", "George Michael", "Make It Big", "5:00"),
                track("eH3GIF69TW8", "I Wanna Dance with Somebody", "Whitney Houston", "Whitney", "4:52"),
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
                track("YkADjO5gApw", "In the Air Tonight", "Phil Collins", "Face Value", "5:30"),
                track("6M6samPEMpM", "Everybody", "Backstreet Boys", "Backstreet's Back", "4:45"),
                track("C-u5WLJ9nkU", "...Baby One More Time", "Britney Spears", "...Baby One More Time", "3:30"),
                track("ru0K8uYEZWw", "Can't Stop the Feeling!", "Justin Timberlake", "Trolls", "3:56"),
                track("CevxZvSJLk8", "Roar", "Katy Perry", "Prism", "3:43"),
                track("qrO4YZeyl0I", "Bad Romance", "Lady Gaga", "The Fame Monster", "4:54"),
                track("nfWlot6h_JM", "Shake It Off", "Taylor Swift", "1989", "3:39"),
                track("2vjPBrBU-TM", "Chandelier", "Sia", "1000 Forms of Fear", "3:36"),
                track("pB-5XG-DbAA", "Stay with Me", "Sam Smith", "In the Lonely Hour", "2:52"),
                track("H5v3k2nn9pc", "As It Was", "Harry Styles", "Harry's House", "2:47"),
                track("G7KNmW9a75Y", "Flowers", "Miley Cyrus", "Endless Summer Vacation", "3:20"),
                track("dyRsYkfflGU", "Bad Guy", "Billie Eilish", "When We All Fall Asleep...", "3:14"),
                track("wXhTHyIgQ_U", "Circles", "Post Malone", "Hollywood's Bleeding", "3:35"),
                track("7wtfhZwyrcc", "Believer", "Imagine Dragons", "Evolve", "3:24"),
                track("hT_nvWreIhg", "Counting Stars", "OneRepublic", "Native", "4:17"),
                track("pw8PpY42usc", "Feel Good Inc.", "Gorillaz", "Demon Days", "3:41"),
                track("PWgvGjAhvIw", "Hey Ya!", "Outkast", "Speakerboxxx/The Love Below", "3:55"),
                track("WZzcY7ASRno", "Hips Don't Lie", "Shakira", "Oral Fixation Vol. 2", "3:38"),
                track("p47fEXGABAo", "Livin' La Vida Loca", "Ricky Martin", "Ricky Martin", "4:03")
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
                track("q482310184w", "El Bombón Asesino", "Los Palmeras", "Un Poco Más", "3:12"),
                track("m74831Z001w", "Olvídala", "Los Palmeras", "Grandes Éxitos", "3:40"),
                track("d73841Z011k", "Soy Sabalero", "Los Palmeras", "En Vivo Colón", "3:30"),
                track("f73841Z002w", "Doble Vida", "Los Palmeras", "Fiesta", "3:15"),
                track("k74883Z010w", "Beso a Beso", "La Mona Jiménez", "Beso a Beso", "4:10"),
                track("d73841Z012k", "Quién Se Ha Tomado Todo el Vino", "La Mona Jiménez", "En Vivo", "4:20"),
                track("m74831Z003w", "El Federal", "La Mona Jiménez", "Raza Negra", "3:55"),
                track("f73841Z004w", "La Mano de Dios", "Rodrigo", "A 2000", "5:30"),
                track("k74883Z011w", "Ocho Cuarenta", "Rodrigo", "Cuarteteando", "3:45"),
                track("d73841Z013k", "Lo Mejor del Amor", "Rodrigo", "Lo Mejor del Amor", "3:50"),
                track("m74831Z004w", "Soy Cordobés", "Rodrigo", "A 2000", "3:45"),
                track("k74883Z012w", "No Te Creas Tan Importante", "Damas Gratis", "Somos Nosotros", "3:35"),
                track("f73841Z005w", "Me Vas a Extrañar", "Damas Gratis", "En Vivo", "3:15"),
                track("m98411Z019w", "Se Te Ve la Tanga", "Damas Gratis", "Para los Pibes", "3:05"),
                track("q7488311-1w", "Los Dueños del Pabellón", "Damas Gratis", "Operación Damas Gratis", "3:40"),
                track("m7384199-1w", "Un Finde", "Ke Personajes", "Crossover #2", "2:45"),
                track("d7488311-1w", "Pobre Corazón", "Ke Personajes", "En Vivo", "3:20"),
                track("k74883Z013w", "Adiós Amor", "Ke Personajes", "Grandes Éxitos", "3:10"),
                track("m74831Z005w", "Loco (Tu Forma de Ser)", "Los Auténticos Decadentes", "El Milagro Argentino", "3:35"),
                track("d73841Z014k", "El Murguero", "Los Auténticos Decadentes", "Mi Vida Loca", "4:40"),
                track("f73841Z006w", "Corazón", "Los Auténticos Decadentes", "Mi Vida Loca", "4:15"),
                track("k74883Z014w", "La Guitarra", "Los Auténticos Decadentes", "Mi Vida Loca", "3:45"),
                track("d73841Z015k", "No Me Arrepiento de Este Amor", "Gilda", "Corazón Valiente", "3:30"),
                track("m74831Z006w", "Fuiste", "Gilda", "Corazón Valiente", "3:00"),
                track("wQ4Y1a1Z03w", "Se Me Ha Perdido un Corazón", "Gilda", "Pasito a Pasito", "3:12"),
                track("k94Y1a1Z03w", "Una Cerveza", "Ráfaga", "Una Cerveza", "3:25"),
                track("m84Y1a1Z03w", "Mentirosa", "Ráfaga", "Ráfaga en Vivo", "3:15"),
                track("d74Y1a1Z03w", "Agüita", "Ráfaga", "Ráfaga de Amor", "3:20"),
                track("f64Y1a1Z03w", "Matador", "Los Fabulosos Cadillacs", "Vasos Vacíos", "4:35"),
                track("k54Y1a1Z03w", "Vasos Vacíos", "Los Fabulosos Cadillacs", "Vasos Vacíos", "4:40"),
                track("m44Y1a1Z03w", "Mal Bicho", "Los Fabulosos Cadillacs", "Rey Azúcar", "4:05"),
                track("d34Y1a1Z03w", "La Cumbia de los Trapos", "Yerba Brava", "Cumbia Villera", "3:20"),
                track("q24Y1a1Z03w", "Pibe Cantina", "Yerba Brava", "100% Yerba Brava", "3:10"),
                track("m14Y1a1Z03w", "Por Lo Que Yo Te Quiero", "Walter Olmos", "A Pura Sangre", "3:45"),
                track("d04Y1a1Z03w", "Amor Fugitivo", "Walter Olmos", "De Madrid al Cielo", "3:30"),
                track("k93Y1a1Z03w", "Intento", "Ulises Bueno", "En la Piel", "4:05"),
                track("f83Y1a1Z03w", "Dale Vieja Dale", "Ulises Bueno", "Creo", "3:30"),
                track("m73Y1a1Z03w", "Universo Paralelo", "La K'onga", "En Vivo", "3:25"),
                track("d63Y1a1Z03w", "Ya No Más", "La K'onga", "Sin Límites", "3:15"),
                track("k53Y1a1Z03w", "Yo Tomo Licor", "Amar Azul", "Dime Tú", "3:15"),
                track("f43Y1a1Z03w", "El Polvito del Amor", "Amar Azul", "Insuperable", "3:10"),
                track("m33Y1a1Z03w", "El Campanero", "Tambó Tambó", "Tambó Tambó", "3:30"),
                track("d23Y1a1Z03w", "La Cumbia de los Vagos", "Tambó Tambó", "El Auténtico", "3:15"),
                track("k13Y1a1Z03w", "La Marca de la Gorra", "Mala Fama", "Ritmo Sustancia", "3:20"),
                track("f03Y1a1Z03w", "Guampa Chata", "Mala Fama", "Para Vos", "3:05"),
                track("m92Y1a1Z03w", "Amores Como el Nuestro", "Los Charros", "Corazón de Papel", "3:35"),
                track("d82Y1a1Z03w", "Jurabas Tú", "Los del Fuego", "En Vivo", "3:10"),
                track("k72Y1a1Z03w", "Una Calle Nos Separa", "Los Leales", "Grandes Éxitos", "3:40"),
                track("f62Y1a1Z03w", "Le Pido a Dios", "Leo Mattioli", "Un Homenaje al Cielo", "3:50"),
                track("m52Y1a1Z03w", "Tramposa y Mentirosa", "Leo Mattioli", "Ese Soy Yo", "3:40")
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
                track("pKwQlm-wldA", "Layla (Acoustic)", "Eric Clapton", "Unplugged", "4:49"),
                track("fregObNcHC8", "The Man Who Sold the World", "Nirvana", "MTV Unplugged in New York", "4:20"),
                track("2YdoIumDaD4", "Where Did You Sleep Last Night", "Nirvana", "MTV Unplugged in New York", "5:08"),
                track("ErvgV4P6FSE", "Patience", "Guns N' Roses", "G N' R Lies", "5:56"),
                track("n4RjJKxkeG4", "Wind of Change", "Scorpions", "Crazy World", "5:12"),
                track("Cvm2O06LqZ0", "Still Loving You", "Scorpions", "Love at First Sting", "6:28"),
                track("NvryolGa19A", "Bed of Roses", "Bon Jovi", "Keep the Faith", "6:34"),
                track("9BMwcO6_hyA", "Always", "Bon Jovi", "Cross Road", "5:53"),
                track("JkK8g6FMEXE", "I Don't Want to Miss a Thing", "Aerosmith", "Armageddon", "4:59"),
                track("UrIILvgEEeE", "More Than Words", "Extreme", "Pornograffitti", "5:34"),
                track("Y0pdQU87dc8", "(Everything I Do) I Do It for You", "Bryan Adams", "Waking Up the Neighbours", "6:34"),
                track("9EHAo6rHAa8", "Please Forgive Me", "Bryan Adams", "So Far So Good", "5:55"),
                track("L6-uNLkeNYc", "To Be with You", "Mr. Big", "Lean into It", "3:27"),
                track("ivFYVAntpw0", "I Remember You", "Skid Row", "Skid Row", "5:14"),
                track("4jpf-eC-Xlk", "18 and Life", "Skid Row", "Skid Row", "3:50"),
                track("GOJk0XX154A", "Is This Love", "Whitesnake", "Whitesnake", "4:43"),
                track("j2r2nDhTzC4", "Every Rose Has Its Thorn", "Poison", "Open Up and Say... Ahh!", "4:20"),
                track("IXdNnw99-Ic", "Wish You Were Here", "Pink Floyd", "Wish You Were Here", "5:34"),
                track("-cfc3rCQOuU", "Going to California", "Led Zeppelin", "Led Zeppelin IV", "3:32"),
                track("RcZn2-b9978", "Angie", "The Rolling Stones", "Goats Head Soup", "4:33"),
                track("NrgmdOz227I", "Yesterday", "The Beatles", "Help!", "2:05"),
                track("QDYfEBY9NM4", "Let It Be", "The Beatles", "Let It Be", "4:03"),
                track("sUJkCXE4444", "Love of My Life", "Queen", "A Night at the Opera", "3:38"),
                track("6hzrDeceEKc", "Wonderwall", "Oasis", "(What's the Story) Morning Glory?", "4:18"),
                track("cmpRLQZt488", "Don't Look Back in Anger", "Oasis", "(What's the Story) Morning Glory?", "4:48"),
                track("1uYWYWPc9HU", "Karma Police", "Radiohead", "OK Computer", "4:24"),
                track("n5h0qCG62XA", "Fake Plastic Trees", "Radiohead", "The Bends", "4:50"),
                track("xwtdhWltSIg", "Losing My Religion", "R.E.M.", "Out of Time", "4:28"),
                track("5rOiW_xY-kc", "Everybody Hurts", "R.E.M.", "Automatic for the People", "5:17"),
                track("sC2GenzSs18", "Blow Up the Outside World", "Soundgarden", "Down on the Upside", "5:46"),
                track("f8hT3oDDf6c", "Down in a Hole (Unplugged)", "Alice In Chains", "MTV Unplugged", "5:40"),
                track("kuq7RYQ8Wa0", "Just Breathe", "Pearl Jam", "Backspacer", "3:35"),
                track("GLvohMXgcBo", "Under the Bridge", "Red Hot Chili Peppers", "Blood Sugar Sex Magik", "4:24"),
                track("CnQ8N1KacJc", "Good Riddance (Time of Your Life)", "Green Day", "Nimrod", "2:34"),
                track("NdYWuo9OFAw", "Iris", "Goo Goo Dolls", "Dizzy Up the Girl", "4:49"),
                track("fgT9zGkiLig", "Drive", "Incubus", "Make Yourself", "3:52"),
                track("kPBzTxZQG5Q", "Here Without You", "3 Doors Down", "Away from the Sun", "3:58"),
                track("tH2w6Oxx0kQ", "Dust in the Wind", "Kansas", "Point of Know Return", "3:26"),
                track("zSAJ0l4OBHM", "A Horse with No Name", "America", "America", "4:12"),
                track("P6zaCV42Erk", "Father and Son", "Cat Stevens", "Tea for the Tillerman", "3:40"),
                track("rm9coQLGltQ", "Knockin' on Heaven's Door", "Bob Dylan", "Pat Garrett & Billy the Kid", "2:32"),
                track("m98411Z018w", "Barro Tal Vez", "Luis Alberto Spinetta", "Kamikaze", "3:20"),
                track("f83741Z011w", "Canción Para Mi Muerte", "Sui Generis", "Vida", "3:36"),
                track("d73841Z012k", "Desarma y Sangra", "Serú Girán", "Bicicleta", "3:40"),
                track("m74831Z003w", "Ojos de Video Tape", "Charly García", "Clics Modernos", "3:50"),
                track("f73841Z004w", "Té Para Tres", "Soda Stereo", "Canción Animal", "2:26"),
                track("k74883Z011w", "Pétalo de Sal", "Fito Páez", "El Amor Después del Amor", "2:43"),
                track("c83711m001w", "Spaghetti del Rock", "Divididos", "Narigón del Siglo", "3:32"),
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
                track("qFfnlHbBn6A", "Voodoo Child", "Jimi Hendrix", "Electric Ladyland", "5:12"),
                track("4O_YMLDvvnw", "Still Got the Blues", "Gary Moore", "Still Got the Blues", "6:11"),
                track("vkUpfw4Hf3w", "Parisienne Walkways", "Gary Moore", "Back on the Streets", "3:18"),
                track("oica5jG7FpU", "The Thrill Is Gone", "B.B. King", "Completely Well", "5:24"),
                track("qYS732zyYfU", "Cocaine", "Eric Clapton", "Slowhand", "3:38"),
                track("uSquiIVLhrQ", "Layla", "Derek and the Dominos", "Layla", "7:05"),
                track("IPo9Gd2zUTw", "Black Magic Woman", "Carlos Santana", "Abraxas", "5:24"),
                track("6Whgn_XVNAN", "Smooth", "Carlos Santana", "Supernatural", "4:56"),
                track("7VBex8zbPew", "Gravity", "John Mayer", "Continuum", "4:05"),
                track("32GZ3suxRn4", "Slow Dancing in a Burning Room", "John Mayer", "Continuum", "4:02"),
                track("VI57QHL6w0o", "Always with Me, Always with You", "Joe Satriani", "Surfing with the Alien", "3:22"),
                track("okLDnZ717N0", "For the Love of God", "Steve Vai", "Passion and Warfare", "6:02"),
                track("sI7XiJgt0vY", "Eruption", "Van Halen", "Van Halen", "1:42"),
                track("55nAwmVLQSk", "Cliffs of Dover", "Eric Johnson", "Ah Via Musicom", "4:07"),
                track("0c3d7nNmdfw", "Stranglehold", "Ted Nugent", "Ted Nugent", "8:22"),
                track("yRDivUb5EeA", "Jessica", "The Allman Brothers Band", "Brothers and Sisters", "7:30"),
                track("0fAQH8ArMzs", "Sultans of Swing", "Dire Straits", "Dire Straits", "5:47"),
                track("wTP2RUD_cL0", "Money for Nothing", "Dire Straits", "Brothers in Arms", "4:38"),
                track("_FrOQC-zEog", "Comfortably Numb", "Pink Floyd", "The Wall", "6:23"),
                track("f03Y1a1Z03w", "El Hombre Suburbano", "Pappo's Blues", "Pappo's Blues Vol. 1", "2:18"),
                track("m92Y1a1Z03w", "Sucio y Desprolijo", "Pappo's Blues", "Pappo's Blues Vol. 3", "4:33"),
                track("d82Y1a1Z03w", "Desconfío de la Vida", "Pappo's Blues", "Pappo's Blues Vol. 2", "2:46"),
                track("k72Y1a1Z03w", "Juntos a la Par", "Pappo", "Buscando un Amor", "3:20"),
                track("HQMm7l6XQ4E", "Whole Lotta Love", "Led Zeppelin", "Led Zeppelin II", "5:34"),
                track("QkF3oxziUI4", "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "8:02"),
                track("6tlSx0jKUlm", "Black Dog", "Led Zeppelin", "Led Zeppelin IV", "4:56"),
                track("zt51r9GYwy4", "Sunshine of Your Love", "Cream", "Disraeli Gears", "4:10"),
                track("rG6b94o_6lI", "La Grange", "ZZ Top", "Tres Hombres", "3:52"),
                track("7wRHBLwpASw", "Sharp Dressed Man", "ZZ Top", "Eliminator", "4:14"),
                track("Wr9ie2J2690", "Highway Star", "Deep Purple", "Machine Head", "6:05"),
                track("LCnebZnysmI", "Burn", "Deep Purple", "Burn", "6:00"),
                track("qKggnBh2vVU", "Smoke on the Water", "Deep Purple", "Machine Head", "5:40"),
                track("pAgnJDJN4VA", "Back in Black", "AC/DC", "Back in Black", "4:15"),
                track("l482T0yNkeo", "Highway to Hell", "AC/DC", "Highway to Hell", "3:28"),
                track("v2AC41dglnM", "Thunderstruck", "AC/DC", "The Razors Edge", "4:52"),
                track("1w7OgIMMRc4", "Sweet Child O' Mine", "Guns N' Roses", "Appetite for Destruction", "5:03"),
                track("8SbUC-UaAxE", "November Rain", "Guns N' Roses", "Use Your Illusion I", "8:57"),
                track("SwYN7mTi6HM", "Jump", "Van Halen", "1984", "4:04"),
                track("fuKDBPw8wQA", "Panama", "Van Halen", "1984", "3:32"),
                track("RMR5zf558Wo", "Crazy Train", "Ozzy Osbourne", "Blizzard of Ozz", "4:56"),
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
                track("VqoyKDAkWyk", "Square Hammer", "Ghost", "Popestar", "3:58"),
                track("_u-7rWKnVVo", "Lux Æterna", "Metallica", "72 Seasons", "3:25"),
                track("_mEC54eTuGw", "Angry", "The Rolling Stones", "Hackney Diamonds", "3:46"),
                track("id6l62G641k", "Beggin'", "Måneskin", "Chosen", "3:31"),
                track("yOb9Xaug35M", "I WANNA BE YOUR SLAVE", "Måneskin", "Teatro d'ira - Vol. I", "2:53"),
                track("XrsbfrFPATs", "THE LONELIEST", "Måneskin", "RUSH!", "4:07"),
                track("aJg4OJxp-co", "Highway Tune", "Greta Van Fleet", "From the Fires", "3:01"),
                track("FYH8DsU2WCk", "There'd Better Be a Mirrorball", "Arctic Monkeys", "The Car", "4:48"),
                track("xIYJ_m49480", "This Is Why", "Paramore", "This Is Why", "3:27"),
                track("eVTXPUF4Oz4", "In the End", "Linkin Park", "Hybrid Theory", "3:36"),
                track("kXYiU_JCYtU", "Numb", "Linkin Park", "Meteora", "3:07"),
                track("LYU-8IFcDPw", "Faint", "Linkin Park", "Meteora", "2:42"),
                track("SBjQ9tuuKSQ", "Everlong", "Foo Fighters", "The Colour and the Shape", "4:10"),
                track("9jK-NcRmVcw", "The Pretender", "Foo Fighters", "Echoes, Silence, Patience & Grace", "4:30"),
                track("NUTGr5t3Mo8", "Basket Case", "Green Day", "Dookie", "3:01"),
                track("Soa3gO7tL-c", "Boulevard of Broken Dreams", "Green Day", "American Idiot", "4:21"),
                track("98W9QuMq-2k", "All the Small Things", "Blink-182", "Enema of the State", "2:47"),
                track("s1tAYmMjLdY", "I Miss You", "Blink-182", "Blink-182", "3:47"),
                track("VrZ4sMRYimw", "The Kids Aren't Alright", "The Offspring", "Americana", "3:00"),
                track("YlUKcNNmywk", "Californication", "Red Hot Chili Peppers", "Californication", "5:21"),
                track("rn_YodiJO6k", "Otherside", "Red Hot Chili Peppers", "Californication", "4:15"),
                track("gGdGFtwCNBE", "Mr. Brightside", "The Killers", "Hot Fuss", "3:42"),
                track("w8KQmps-Sog", "Uprising", "Muse", "The Resistance", "5:03"),
                track("bpOSxM0rNPM", "Do I Wanna Know?", "Arctic Monkeys", "AM", "4:32"),
                track("7QU1nvuxaMA", "Like a Stone", "Audioslave", "Audioslave", "4:54"),
                track("3YxaaGgTQYM", "Bring Me to Life", "Evanescence", "Fallen", "3:57"),
                track("j0lSpNQQ9-8", "Last Resort", "Papa Roach", "Infest", "3:19"),
                track("RYnFIRc0k6E", "Rollin'", "Limp Bizkit", "Chocolate Starfish...", "3:33"),
                track("CMX2lPum_pg", "Fat Lip", "Sum 41", "All Killer No Filler", "2:58"),
                track("RRKJiM9Njr8", "Welcome to the Black Parade", "My Chemical Romance", "The Black Parade", "5:11"),
                track("toYZFeH8gwg", "Last Nite", "The Strokes", "Is This It", "3:13"),
                track("GhCXAiNz9Ao", "Take Me Out", "Franz Ferdinand", "Franz Ferdinand", "3:57"),
                track("s88r_q7oufE", "No One Knows", "Queens of the Stone Age", "Songs for the Deaf", "4:38"),
                track("CSvFpBO48dQ", "Chop Suey!", "System of a Down", "Toxicity", "3:30"),
                track("5abamRO41fE", "Psychosocial", "Slipknot", "All Hope Is Gone", "4:44"),
                track("W3q8Od5qJio", "Du Hast", "Rammstein", "Sehnsucht", "3:54"),
                track("DelhLppPSxY", "Hail to the King", "Avenged Sevenfold", "Hail to the King", "5:05"),
                track("09LTT0sfCRg", "Down with the Sickness", "Disturbed", "The Sickness", "4:38"),
                track("4NRXx6U8ABQ", "Blinding Lights", "The Weeknd", "After Hours", "3:20"),
                track("oygrmJFKYZY", "Don't Start Now", "Dua Lipa", "Future Nostalgia", "3:03"),
                track("OPf0YbXqDm0", "Uptown Funk", "Bruno Mars", "Uptown Special", "4:30"),
                track("5NV6Rdv1a3I", "Get Lucky", "Daft Punk", "Random Access Memories", "4:08"),
                track("dvgZkm1xWPE", "Viva La Vida", "Coldplay", "Viva la Vida", "4:02"),
                track("7wtfhZwyrcc", "Believer", "Imagine Dragons", "Evolve", "3:24"),
                track("hT_nvWreIhg", "Counting Stars", "OneRepublic", "Native", "4:17"),
                track("pw8PpY42usc", "Feel Good Inc.", "Gorillaz", "Demon Days", "3:41"),
                track("H5v3k2nn9pc", "As It Was", "Harry Styles", "Harry's House", "2:47"),
                track("G7KNmW9a75Y", "Flowers", "Miley Cyrus", "Endless Summer Vacation", "3:20"),
                track("dyRsYkfflGU", "Bad Guy", "Billie Eilish", "When We All Fall Asleep...", "3:14")
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
     * Builds an intelligent Spotify-style radio queue centered around an initial track.
     * Takes the initial track, other tracks by the same artist, and similar tracks from matching genre playlists.
     */
    fun buildRadioQueue(initialTrack: MusicTrack): List<MusicTrack> {
        val artistLower = initialTrack.artist.lowercase()
        val allCatalogTracks = playlists.flatMap { it.tracks }.distinctBy { it.id }

        // Find tracks by the exact same artist
        val sameArtistTracks = allCatalogTracks.filter {
            it.id != initialTrack.id && (it.artist.lowercase().contains(artistLower) || artistLower.contains(it.artist.lowercase()))
        }.shuffled()

        // Find which playlist contains this track or similar artists
        val matchingPlaylist = playlists.firstOrNull { pl ->
            pl.tracks.any { it.id == initialTrack.id || it.artist.lowercase().contains(artistLower) }
        }

        val similarTracks = if (matchingPlaylist != null) {
            matchingPlaylist.tracks.filter { it.id != initialTrack.id && it.artist.lowercase() != artistLower }.shuffled()
        } else {
            // Fallback: pick general rock/pop/metal tracks from catalog
            allCatalogTracks.filter { it.id != initialTrack.id }.shuffled()
        }

        // Combine: Initial Track -> up to 5 tracks from same artist -> up to 40 similar tracks
        val radioList = mutableListOf<MusicTrack>()
        radioList.add(initialTrack)
        radioList.addAll(sameArtistTracks.take(6))
        radioList.addAll(similarTracks.take(45))

        return radioList.distinctBy { it.id }
    }

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
