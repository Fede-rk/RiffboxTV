package com.riffbox.tv.music.model

import java.io.Serializable

data class MusicTrack(
    val id: String, // YouTube Video ID or local unique ID
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String = "",
    val thumbnailUrl: String = "",
    var isDownloaded: Boolean = false,
    var localFilePath: String? = null,
    val isNewRelease: Boolean = false
) : Serializable


data class MusicCategory(
    val id: String,
    val title: String,
    val icon: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val query: String
) : Serializable

data class MusicPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val coverUrl: String,
    val tracks: List<MusicTrack> = emptyList()
) : Serializable

data class MusicArtist(
    val id: String,
    val name: String,
    val genre: String,
    val imageUrl: String,
    val query: String
) : Serializable

data class MusicAlbum(
    val browseId: String,
    val title: String,
    val year: String = "",
    val coverUrl: String = "",
    val artist: String = ""
) : Serializable

data class MusicArtistSummary(
    val browseId: String,
    val name: String,
    val subtitle: String = "",
    val imageUrl: String = ""
) : Serializable

data class ArtistProfile(
    val browseId: String,
    val name: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val radioPlaylistId: String = "",
    val topTracks: List<MusicTrack> = emptyList(),
    val albums: List<MusicAlbum> = emptyList(),
    val singles: List<MusicAlbum> = emptyList(),
    val similarArtists: List<MusicArtistSummary> = emptyList()
) : Serializable

data class SearchMusicResult(
    val artist: MusicArtistSummary? = null,
    val tracks: List<MusicTrack> = emptyList()
) : Serializable

data class InfiniteGenreStation(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val isLucky: Boolean = false,
    val localPlaylistIds: List<String> = emptyList(),
    val seedArtists: List<String> = emptyList()
) : Serializable

