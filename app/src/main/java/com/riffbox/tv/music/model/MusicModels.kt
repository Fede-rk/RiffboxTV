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
    var localFilePath: String? = null
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
