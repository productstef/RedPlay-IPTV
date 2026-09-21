package com.redplay.iptv.data

data class Provider(
    val id: String,
    val name: String,
    val url: String,
    val epgUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val builtIn: Boolean = false,
)

data class Channel(
    val id: String,
    val name: String,
    val tvgId: String = "",
    val tvgName: String = "",
    val logo: String = "",
    val group: String = "Other",
    val language: String = "",
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

data class Playlist(
    val channels: List<Channel> = emptyList(),
    val epgUrl: String = "",
)

data class Programme(
    val channelId: String,
    val startMillis: Long,
    val stopMillis: Long,
    val title: String,
    val subTitle: String = "",
    val description: String = "",
    val category: String = "",
)

data class EpgData(
    val displayNames: Map<String, List<String>> = emptyMap(),
    val programmes: Map<String, List<Programme>> = emptyMap(),
    val nameToIds: Map<String, List<String>> = emptyMap(),
)

data class LibraryMovie(
    val title: String,
    val displayTitle: String,
    val year: String,
    val description: String,
    val candidateUrls: List<String>,
    val subtitleUrl: String,
    val durationMillis: Long,
)

data class ChannelLibrary(
    val title: String,
    val channel: Channel,
    val movies: List<LibraryMovie>,
)

data class PlaybackRequest(
    val key: Long = System.nanoTime(),
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val localSubtitlePath: String? = null,
    val isLive: Boolean,
)

data class TrackOption(val id: Int, val title: String)

data class PlayerSnapshot(
    val playing: Boolean = false,
    val loading: Boolean = false,
    val error: String = "",
    val timeMillis: Long = 0,
    val lengthMillis: Long = 0,
    val volume: Int = 80,
    val muted: Boolean = false,
    val audioTracks: List<TrackOption> = emptyList(),
    val selectedAudio: Int = -1,
    val subtitleTracks: List<TrackOption> = emptyList(),
    val selectedSubtitle: Int = -1,
)

fun builtInProviders(): List<Provider> = listOf(
    Provider(
        id = "builtin-western",
        name = "Western",
        url = "http://92.5.59.81/western/playlist.m3u",
        builtIn = true,
    ),
    Provider(
        id = "builtin-starwars",
        name = "Star Wars",
        url = "http://92.5.59.81/starwars.m3u",
        builtIn = true,
    ),
)
