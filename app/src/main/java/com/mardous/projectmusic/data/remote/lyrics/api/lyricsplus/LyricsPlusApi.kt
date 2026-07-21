package com.mardous.projectmusic.data.remote.lyrics.api.lyricsplus

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsPlusResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsPlusSearchResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsPlusSearchResultItem
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.extensions.media.isArtistNameUnknown
import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

class LyricsPlusApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "LyricsPlus"
    override val networkFeature = NetworkFeature.Lyrics.LyricsPlus

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return fetchLyricsInternal("https://lyricsplus.prjktla.workers.dev", title, artist, song.duration)
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        val mirrors = listOf(
            "https://lyricsplus.binimum.org",
            "https://lyricsplus.prjktla.workers.dev",
            "https://lyricsplus.atomix.one",
            "https://lyrics.geeked.wtf"
        )

        val searchQueries = if (artist.isArtistNameUnknown()) listOf(title) else listOf("$title $artist", title)

        for (query in searchQueries) {
            for (mirror in mirrors) {
                try {
                    Log.d(TAG, "Searching LyricsPlus ($mirror) for: $query")
                    val response = client.get("$mirror/v1/songlist/search") {
                        parameter("q", query)
                        timeout {
                            connectTimeoutMillis = 4000
                            socketTimeoutMillis = 6000
                        }
                    }

                    if (response.status == HttpStatusCode.OK) {
                        val responseText = response.bodyAsText()
                        Log.v(TAG, "LyricsPlus response from $mirror: ${responseText.take(100)}...")
                        
                        // Robust parsing: detect if direct array or wrapped results
                        val items = try {
                            val element = JSON.parseToJsonElement(responseText)
                            val jsonArray = if (element is kotlinx.serialization.json.JsonObject) {
                                element["results"] as? kotlinx.serialization.json.JsonArray
                            } else if (element is kotlinx.serialization.json.JsonArray) {
                                element
                            } else null
                            
                            jsonArray?.let {
                                JSON.decodeFromJsonElement<List<LyricsPlusSearchResultItem>>(it)
                            } ?: emptyList()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse LyricsPlus response from $mirror: ${e.message}")
                            emptyList()
                        }

                        if (items.isNotEmpty()) {
                            val deferredResults = items.take(10).map { item ->
                                async {
                                    val finalTitle = item.finalTitle
                                    val finalArtist = item.finalArtist
                                    
                                    val durationInSeconds = if ((item.duration ?: 0L) > 10000) (item.duration!! / 1000) else (item.duration ?: 0L)

                                    val lyrics = fetchLyricsInternal(mirror, finalTitle, finalArtist, durationInSeconds)
                                        ?: RawLyrics.Remote()

                                    if (lyrics.hasPlain || lyrics.hasSynced) {
                                        LyricsSearchResult(
                                            title = finalTitle,
                                            artist = finalArtist,
                                            album = item.finalAlbum,
                                            duration = item.duration,
                                            instrumental = false,
                                            provider = name,
                                            lyrics = lyrics
                                        )
                                    } else null
                                }
                            }
                            val results = deferredResults.awaitAll().filterNotNull()
                            if (results.isNotEmpty()) {
                                Log.d(TAG, "LyricsPlus found ${results.size} results via $mirror")
                                return@coroutineScope results
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "LyricsPlus mirror $mirror failed for '$query': ${e.message}")
                }
            }
        }
        emptyList()
    }

    private suspend fun fetchLyricsInternal(
        baseUrl: String,
        title: String,
        artist: String,
        duration: Long
    ): RawLyrics.Remote? {
        return try {
            val response = client.get("$baseUrl/v2/lyrics/get") {
                // User agent is now handled globally in headerInterceptor()
                parameter("title", title)
                parameter("artist", artist)
                if (duration > 0) {
                    parameter("duration", duration / 1000)
                }
                parameter("source", "apple,musixmatch,spotify")
                timeout {
                    connectTimeoutMillis = 4000
                    socketTimeoutMillis = 6000
                }
            }.body<LyricsPlusResponse>()

            if (response.lyrics.isEmpty()) return null

            val syncedLyrics = StringBuilder()
            for (line in response.lyrics) {
                syncedLyrics.append("[${line.time.toLrcTimestamp()}] ${line.text}\n")
            }

            RawLyrics.Remote(
                synced = RawLyrics.Remote.Content(name, syncedLyrics.toString().trim())
            )
        } catch (e: Exception) {
            Log.v(TAG, "LyricsPlus fetch failed for $title $artist via $baseUrl")
            null
        }
    }

    private fun Long.toLrcTimestamp(): String {
        val minutes = this / 60000
        val seconds = (this % 60000) / 1000
        val milliseconds = (this % 1000) / 10

        val leadingZeros: Array<String> = arrayOf(
            if (minutes < 10) "0" else "",
            if (seconds < 10) "0" else "",
            if (milliseconds < 10) "0" else ""
        )

        return "${leadingZeros[0]}$minutes:${leadingZeros[1]}$seconds.${leadingZeros[2]}$milliseconds"
    }

    companion object {
        private const val TAG = "LyricsPlusApi"
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowSpecialFloatingPointValues = true
        }
    }
}
