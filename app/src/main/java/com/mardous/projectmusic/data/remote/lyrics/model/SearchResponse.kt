/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.data.remote.lyrics.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ITunesSearchResponse(
    @SerialName("resultCount")
    val size: Int,
    val results: List<ITunesSearchResult>
)

@Serializable
data class ITunesSearchResult(
    @SerialName("trackId")
    val id: Long,
    @SerialName("wrapperType")
    val type: String,
    @SerialName("trackName")
    val name: String,
    @SerialName("artistName")
    val artist: String,
    @SerialName("collectionName")
    val album: String,
    @SerialName("trackTimeMillis")
    val durationInMillis: Long? = null
)

@Serializable
data class GeniusSearchResponse(
    val response: GeniusResponseContent
)

@Serializable
data class GeniusResponseContent(
    val hits: List<GeniusHit>
)

@Serializable
data class GeniusHit(
    val result: GeniusSongResult
)

@Serializable
data class GeniusSongResult(
    val id: Long,
    val title: String,
    val url: String,
    @SerialName("primary_artist")
    val primaryArtist: GeniusArtist
)

@Serializable
data class GeniusArtist(
    val name: String
)

@Serializable
data class LyricsPlusSearchResponse(
    val results: List<LyricsPlusSearchResultItem> = emptyList()
)

@Serializable
data class LyricsPlusSearchResultItem(
    val name: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artistName: String? = null,
    val album: String? = null,
    val albumName: String? = null,
    val duration: Long? = null,
    val isrc: String? = null,
    val source: String? = null
) {
    val finalTitle: String get() = title ?: name ?: "Unknown"
    val finalArtist: String get() = artist ?: artistName ?: "Unknown Artist"
    val finalAlbum: String? get() = album ?: albumName
}

@Serializable
data class BetterLyricsSuggestion(
    val song: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artistName: String? = null,
    val album: String? = null,
    val albumName: String? = null,
    val videoId: String? = null
) {
    val finalTitle: String get() = title ?: song ?: "Unknown"
    val finalArtist: String get() = artist ?: artistName ?: "Unknown Artist"
    val finalAlbum: String? get() = album ?: albumName
}

@Serializable
data class UnisonSearchResponse(
    val success: Boolean = false,
    val data: List<UnisonLyricsItem> = emptyList()
)

@Serializable
data class UnisonLyricsItem(
    val videoId: String? = null,
    val song: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artistName: String? = null,
    val album: String? = null,
    val albumName: String? = null,
    val lyrics: String? = null,
    val duration: Long? = null,
    val format: String? = null
) {
    val finalTitle: String get() = title ?: song ?: "Unknown"
    val finalArtist: String get() = artist ?: artistName ?: "Unknown Artist"
    val finalAlbum: String? get() = album ?: albumName
}
