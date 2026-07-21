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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

            fetchLyricsByHash(songs[0].hash, (songs[0].duration * 1000).toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Kugou request failed", e)
            null
        }
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        try {
            val searchResponse = client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("keyword", "$title $artist")
                parameter("page", 1)
                parameter("pagesize", 5)
            }.body<KugouSearchResponse>()

            val songs = searchResponse.data?.info ?: return@coroutineScope emptyList()
            
            // Fetch lyrics for top 3 results in parallel
            val deferredResults = songs.take(3).map { s ->
                async {
                    val duration = (s.duration * 1000).toLong()
                    val lyrics = fetchLyricsByHash(s.hash, duration) ?: RawLyrics.Remote()

                    LyricsSearchResult(
                        title = s.songname,
                        artist = s.singername,
                        album = s.album_name,
                        duration = duration,
                        instrumental = false,
                        provider = name,
                        lyrics = lyrics
                    )
                }
            }
            deferredResults.awaitAll().filter { it.isSynced }
        } catch (e: Exception) {
            Log.e(TAG, "Kugou search failed", e)
            emptyList()
        }
    }

    private suspend fun fetchLyricsByHash(hash: String, duration: Long): RawLyrics.Remote? {
        return try {
            val lyricsResponse = client.get(LYRIC_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("ver", 1)
                parameter("man", "yes")
                parameter("client", "mobi")
                parameter("hash", hash)
                parameter("timelength", duration)
            }.body<KugouLyricsResponse>()

            val content = lyricsResponse.content
            if (content.isNullOrBlank()) null
            else RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, content))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Kugou lyrics for $hash", e)
            null
        }
    }

    companion object {
        private const val TAG = "KugouApi"
        private const val SEARCH_URL = "http://mobilecdn.kugou.com/api/v3/search/song"
        private const val LYRIC_URL = "http://krcs.kugou.com/search"
    }
}
