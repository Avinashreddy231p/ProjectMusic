package com.mardous.projectmusic.data.remote.musicbrainz

import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MusicBrainzArtistSearch(
    val artists: List<MusicBrainzArtist> = emptyList()
)

@Serializable
data class MusicBrainzArtist(
    val id: String = "",
    val name: String = "",
    @kotlinx.serialization.SerialName("sort-name") val sortName: String? = null,
    val disambiguation: String? = null,
    val type: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val area: MusicBrainzArea? = null,
    @kotlinx.serialization.SerialName("life-span") val lifeSpan: MusicBrainzLifeSpan? = null,
    val aliases: List<MusicBrainzAlias>? = null,
    val tags: List<MusicBrainzTag>? = null,
    val relations: List<MusicBrainzRelation>? = null
)

@Serializable
data class MusicBrainzAlias(
    val name: String? = null,
    @kotlinx.serialization.SerialName("sort-name") val sortName: String? = null,
    val type: String? = null,
    val locale: String? = null
)

@Serializable
data class MusicBrainzTag(
    val name: String? = null,
    val count: Int? = null
)

@Serializable
data class MusicBrainzLifeSpan(
    val begin: String? = null,
    val end: String? = null,
    val ended: Boolean? = null
)

@Serializable
data class MusicBrainzReleaseSearch(
    val releases: List<MusicBrainzRelease> = emptyList()
)

@Serializable
data class MusicBrainzRelease(
    val id: String = "",
    val title: String = "",
    val date: String? = null,
    val country: String? = null,
    val status: String? = null,
    @kotlinx.serialization.SerialName("release-events") val releaseEvents: List<MusicBrainzReleaseEvent>? = null,
    @kotlinx.serialization.SerialName("label-info") val labelInfo: List<MusicBrainzLabelInfo>? = null,
    @kotlinx.serialization.SerialName("artist-credit") val artistCredit: List<MusicBrainzArtistCredit>? = null
)

@Serializable
data class MusicBrainzReleaseEvent(
    val date: String? = null,
    val area: MusicBrainzArea? = null
)

@Serializable
data class MusicBrainzArea(
    val name: String? = null
)

@Serializable
data class MusicBrainzLabelInfo(
    @kotlinx.serialization.SerialName("catalog-number") val catalogNumber: String? = null,
    val label: MusicBrainzLabel? = null
)

@Serializable
data class MusicBrainzLabel(
    val name: String? = null
)

@Serializable
data class MusicBrainzArtistCredit(
    val name: String? = null,
    val artist: MusicBrainzArtist? = null
)

@Serializable
data class MusicBrainzRecording(
    val id: String = "",
    val title: String = "",
    val length: Int? = null,
    val isrcs: List<String>? = null,
    val disambiguation: String? = null,
    val relations: List<MusicBrainzRelation>? = null,
    @kotlinx.serialization.SerialName("artist-credit") val artistCredit: List<MusicBrainzArtistCredit>? = null
)

@Serializable
data class MusicBrainzRecordingResponse(
    val recordings: List<MusicBrainzRecording>? = null,
    val id: String? = null,
    val title: String? = null,
    val isrcs: List<String>? = null,
    val relations: List<MusicBrainzRelation>? = null,
    @kotlinx.serialization.SerialName("artist-credit") val artistCredit: List<MusicBrainzArtistCredit>? = null
)

@Serializable
data class MusicBrainzRelation(
    val type: String = "",
    val direction: String? = null,
    val artist: MusicBrainzRelationArtist? = null,
    val work: MusicBrainzRelationWork? = null,
    val attributes: List<String>? = null
)

@Serializable
data class MusicBrainzRelationArtist(
    val id: String? = null,
    val name: String? = null,
    val disambiguation: String? = null,
    @kotlinx.serialization.SerialName("sort-name") val sortName: String? = null
)

@Serializable
data class MusicBrainzRelationWork(
    val id: String? = null,
    val title: String? = null,
    val disambiguation: String? = null,
    val type: String? = null,
    val iswc: String? = null,
    val attributes: List<MusicBrainzWorkAttribute>? = null,
    val language: String? = null
)

@Serializable
data class MusicBrainzWorkAttribute(
    val type: String? = null,
    @kotlinx.serialization.SerialName("type-id") val typeId: String? = null,
    val value: String? = null
)

class MusicBrainzService(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchArtist(query: String): MusicBrainzArtistSearch =
        client.get("$BASE_URL/artist") {
            userAgent(USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            url { parameters.append("query", "artist:$query") }
            url { parameters.append("fmt", "json") }
            url { parameters.append("limit", "5") }
        }.body()

    suspend fun searchRelease(albumQuery: String, artistQuery: String? = null): MusicBrainzReleaseSearch {
        val query = buildString {
            append("release:\"$albumQuery\"")
            if (artistQuery != null) append(" AND artist:\"$artistQuery\"")
        }
        return client.get("$BASE_URL/release") {
            userAgent(USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            url { parameters.append("query", query) }
            url { parameters.append("fmt", "json") }
            url { parameters.append("limit", "5") }
        }.body()
    }

    suspend fun getRecording(mbid: String): MusicBrainzRecordingResponse =
        client.get("$BASE_URL/recording/$mbid") {
            userAgent(USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            url { parameters.append("inc", "artists+artist-rels+isrcs+work-rels+releases") }
            url { parameters.append("fmt", "json") }
        }.body()

    suspend fun getWork(mbid: String): MusicBrainzRelationWork =
        client.get("$BASE_URL/work/$mbid") {
            userAgent(USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            url { parameters.append("inc", "artist-rels+attributes") }
            url { parameters.append("fmt", "json") }
        }.body()

    suspend fun getArtist(mbid: String): MusicBrainzArtist =
        client.get("$BASE_URL/artist/$mbid") {
            userAgent(USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
            url { parameters.append("inc", "aliases+tags+artist-rels") }
            url { parameters.append("fmt", "json") }
        }.body()

    companion object {
        private const val BASE_URL = "https://musicbrainz.org/ws/2"
    }
}
