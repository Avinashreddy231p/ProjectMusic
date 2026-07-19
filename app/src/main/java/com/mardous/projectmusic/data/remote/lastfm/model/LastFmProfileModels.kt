package com.mardous.projectmusic.data.remote.lastfm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastFmTopArtistsResponse(
    val topartists: LastFmTopArtistsContainer
)

@Serializable
data class LastFmTopArtistsContainer(
    val artist: List<LastFmTopArtist>,
    @SerialName("@attr")
    val attr: LastFmPaginationAttr
)

@Serializable
data class LastFmTopArtist(
    val name: String,
    val playcount: String,
    val url: String,
    val image: List<LastFmImage>? = null
)

@Serializable
data class LastFmTopTracksResponse(
    val toptracks: LastFmTopTracksContainer
)

@Serializable
data class LastFmTopTracksContainer(
    val track: List<LastFmTopTrack>,
    @SerialName("@attr")
    val attr: LastFmPaginationAttr
)

@Serializable
data class LastFmTopTrack(
    val name: String,
    val playcount: String,
    val artist: LastFmTopTrackArtist,
    val url: String,
    val image: List<LastFmImage>? = null
)

@Serializable
data class LastFmTopTrackArtist(
    val name: String,
    val url: String
)

@Serializable
data class LastFmTopAlbumsResponse(
    val topalbums: LastFmTopAlbumsContainer
)

@Serializable
data class LastFmTopAlbumsContainer(
    val album: List<LastFmTopAlbum>,
    @SerialName("@attr")
    val attr: LastFmPaginationAttr
)

@Serializable
data class LastFmTopAlbum(
    val name: String,
    val playcount: String,
    val artist: LastFmTopTrackArtist,
    val url: String,
    val image: List<LastFmImage>? = null
)

@Serializable
data class LastFmRecentTracksResponse(
    val recenttracks: LastFmRecentTracksContainer
)

@Serializable
data class LastFmRecentTracksContainer(
    val track: List<LastFmRecentTrack>,
    @SerialName("@attr")
    val attr: LastFmPaginationAttr
)

@Serializable
data class LastFmRecentTrack(
    val name: String,
    val artist: LastFmRecentTrackField,
    val album: LastFmRecentTrackField,
    val url: String,
    val image: List<LastFmImage>? = null,
    val date: LastFmDate? = null,
    @SerialName("@attr")
    val attr: LastFmRecentTrackAttr? = null
)

@Serializable
data class LastFmRecentTrackField(
    @SerialName("#text")
    val text: String
)

@Serializable
data class LastFmRecentTrackAttr(
    val nowplaying: String
)

@Serializable
data class LastFmDate(
    val uts: String,
    @SerialName("#text")
    val text: String
)

@Serializable
data class LastFmImage(
    @SerialName("#text")
    val url: String,
    val size: String
)

@Serializable
data class LastFmPaginationAttr(
    val user: String,
    val page: String,
    val perPage: String,
    val totalPages: String,
    val total: String
)
