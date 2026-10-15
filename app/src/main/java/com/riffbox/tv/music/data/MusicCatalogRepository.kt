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
        MusicCategory("punk", "Punk & 2000s", "🧷", 0xFFFF9800, 0xFFF44336, "Punk Rock alternative anthems"),
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
        // ========================================================
        // 1. LOS MEJORES RIFFS DE LA HISTORIA (52 Canciones)
        // ========================================================
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

        // ========================================================
        // 2. HIMNOS DEL GRUNGE & 90s (50 Canciones)
        // ========================================================
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

        // ========================================================
        // 3. LEYENDAS DEL ROCK ARGENTINO (55 Canciones)
        // ========================================================
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

        // ========================================================
        // 4. HEAVY METAL & THRASH ESSENTIALS (52 Canciones)
        // ========================================================
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

        // ========================================================
        // 5. MTV GENERACIÓN 90s & 2000s (50 Canciones)
        // ========================================================
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
        )
    )

    fun getTracksForCategory(category: MusicCategory, searchResults: List<MusicTrack>): List<MusicTrack> {
        val matchingLocal = when (category.id) {
            "rock_clasico" -> playlists.find { it.id == "pl_riffs" }?.tracks ?: emptyList()
            "metal" -> playlists.find { it.id == "pl_metal" }?.tracks ?: emptyList()
            "grunge" -> playlists.find { it.id == "pl_grunge" }?.tracks ?: emptyList()
            "nacional" -> playlists.find { it.id == "pl_nacional" }?.tracks ?: emptyList()
            "punk" -> playlists.find { it.id == "pl_2000s" }?.tracks ?: emptyList()
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
