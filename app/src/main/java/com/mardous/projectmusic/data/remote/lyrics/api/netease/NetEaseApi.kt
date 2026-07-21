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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

            fetchLyricsBySongId(songs[0].id)
        } catch (e: Exception) {
            Log.e(TAG, "NetEase request failed", e)
            null
        }
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        try {
            Log.d(TAG, "Searching NetEase for: $title $artist")
            val searchResponse = client.get(SEARCH_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("s", "$title $artist")
                parameter("type", 1)
                parameter("limit", 5)
            }.body<NetEaseSearchResponse>()

            val songs = searchResponse.result?.songs ?: return@coroutineScope emptyList()
            
            // Fetch lyrics for the top 3 hits in parallel to find best sync quality
            val deferredResults = songs.take(3).map { s ->
                async {
                    val artistName = s.artists.joinToString(", ") { it.name }
                    val lyrics = fetchLyricsBySongId(s.id) ?: RawLyrics.Remote()
                    
                    Log.v(TAG, "Fetched lyrics for NetEase song ${s.id}: hasSynced=${lyrics.hasSynced}")
                    
                    LyricsSearchResult(
                        title = s.name,
                        artist = artistName,
                        album = s.album?.name,
                        duration = null,
                        instrumental = false,
                        provider = name,
                        lyrics = lyrics
                    )
                }
            }
            val results = deferredResults.awaitAll().filter { it.isSynced }
            Log.d(TAG, "NetEase found ${results.size} synced results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "NetEase search failed", e)
            emptyList()
        }
    }

    private suspend fun fetchLyricsBySongId(songId: Long): RawLyrics.Remote? {
        return try {
            val lyricsResponse = client.get(LYRIC_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("id", songId)
                parameter("lv", 1)
                parameter("kv", 1)
                parameter("tv", -1)
            }.body<NetEaseLyricsResponse>()

            val lrc = lyricsResponse.lrc?.lyric
            if (lrc.isNullOrBlank()) null
            else RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, lrc))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch NetEase lyrics for $songId", e)
            null
        }
    }

    companion object {
        private const val TAG = "NetEaseApi"
        private const val SEARCH_URL = "http://music.163.com/api/search/get"
        private const val LYRIC_URL = "http://music.163.com/api/song/lyric"
    }
}
