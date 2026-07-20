package com.mardous.projectmusic.data.remote.lyrics.api.kugou

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.KugouLyricsResponse
import com.mardous.projectmusic.data.remote.lyrics.model.KugouSearchResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

class KugouApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "Kugou"
    override val networkFeature = NetworkFeature.Lyrics.Kugou

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return try {
            val searchResponse = client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("keyword", "$title $artist")
                parameter("page", 1)
                parameter("pagesize", 5)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }.body<KugouSearchResponse>()

            val songs = searchResponse.data?.info ?: return null
            if (songs.isEmpty()) return null

            val hash = songs[0].hash
            val duration = songs[0].duration * 1000
            
            val lyricsResponse = client.get(LYRIC_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("ver", 1)
                parameter("man", "yes")
                parameter("client", "mobi")
                parameter("hash", hash)
                parameter("timelength", duration)
            }.body<KugouLyricsResponse>()

            val content = lyricsResponse.content
            if (content.isNullOrBlank()) return null

            RawLyrics.Remote(
                synced = RawLyrics.Remote.Content(name, content)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Kugou request failed", e)
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
                parameter("keyword", "$title $artist")
                parameter("page", 1)
                parameter("pagesize", 10)
            }.body<KugouSearchResponse>()

            val songs = searchResponse.data?.info ?: return emptyList()
            songs.map { s ->
                LyricsSearchResult(
                    title = s.songname,
                    artist = s.singername,
                    album = s.album_name,
                    duration = (s.duration * 1000).toLong(),
                    instrumental = false,
                    provider = name,
                    lyrics = RawLyrics.Remote(
                        plain = RawLyrics.Remote.Content(name, null),
                        synced = RawLyrics.Remote.Content(name, null)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kugou search failed", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "KugouApi"
        private const val SEARCH_URL = "http://mobilecdn.kugou.com/api/v3/search/song"
        private const val LYRIC_URL = "http://krcs.kugou.com/search"
    }
}
