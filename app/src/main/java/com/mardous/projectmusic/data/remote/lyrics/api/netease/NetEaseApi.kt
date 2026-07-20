package com.mardous.projectmusic.data.remote.lyrics.api.netease

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.data.remote.lyrics.model.NetEaseLyricsResponse
import com.mardous.projectmusic.data.remote.lyrics.model.NetEaseSearchResponse
import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

class NetEaseApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "NetEase"
    override val networkFeature = NetworkFeature.Lyrics.NetEase

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return try {
            val searchResponse = client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("s", "$title $artist")
                parameter("type", 1)
                parameter("limit", 5)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }.body<NetEaseSearchResponse>()

            val songs = searchResponse.result?.songs ?: return null
            if (songs.isEmpty()) return null

            // Try first 2 songs for a match (or just the first one for now)
            val songId = songs[0].id
            
            val lyricsResponse = client.get(LYRIC_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("id", songId)
                parameter("lv", 1)
                parameter("kv", 1)
                parameter("tv", -1)
            }.body<NetEaseLyricsResponse>()

            val lrc = lyricsResponse.lrc?.lyric
            if (lrc.isNullOrBlank()) return null

            RawLyrics.Remote(
                synced = RawLyrics.Remote.Content(name, lrc)
            )
        } catch (e: Exception) {
            Log.e(TAG, "NetEase request failed", e)
            null
        }
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> {
        return try {
            val searchResponse = client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("s", "$title $artist")
                parameter("type", 1)
                parameter("limit", 10)
            }.body<NetEaseSearchResponse>()

            val songs = searchResponse.result?.songs ?: return emptyList()
            songs.map { s ->
                val artistName = s.artists.joinToString(", ") { it.name }
                LyricsSearchResult(
                    title = s.name,
                    artist = artistName,
                    album = s.album?.name,
                    duration = null, // NetEase search doesn't easily give duration in this basic API
                    instrumental = false,
                    provider = name,
                    lyrics = RawLyrics.Remote(
                        plain = RawLyrics.Remote.Content(name, null), // We only know ID here
                        synced = RawLyrics.Remote.Content(name, null)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "NetEase search failed", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "NetEaseApi"
        private const val SEARCH_URL = "http://music.163.com/api/search/get"
        private const val LYRIC_URL = "http://music.163.com/api/song/lyric"
    }
}
