package com.mardous.projectmusic.data.remote.lyrics.api.betterlyrics

import android.net.Uri
import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.BetterLyricsResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.data.remote.lyrics.model.UnisonSearchResponse
import com.mardous.projectmusic.extensions.media.isArtistNameUnknown
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.apache.commons.text.similarity.JaroWinklerSimilarity

class BetterLyricsApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "BetterLyrics"
    override val networkFeature = NetworkFeature.Lyrics.BetterLyrics

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return fetchLyricsInternal(title, artist, song.duration)
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        val endpoint = "https://unison.boidu.dev/lyrics/search"
        val isUnknownArtist = artist.isArtistNameUnknown()
        val normalizedSearchArtist = artist.lowercase().trim()
        val normalizedSearchTitle = title.lowercase().trim()

        val searchAttempts = mutableListOf<Map<String, String>>()
        if (!isUnknownArtist) {
            searchAttempts.add(mapOf("song" to title, "artist" to artist))
            searchAttempts.add(mapOf("q" to "$title $artist"))
        }
        searchAttempts.add(mapOf("q" to title))

        val allRelevantResults = mutableListOf<LyricsSearchResult>()

        for (params in searchAttempts) {
            try {
                Log.d(TAG, "Querying BetterLyrics with params: $params")
                val response = client.get(endpoint) {
                    params.forEach { (key, value) -> parameter(key, value) }
                    timeout {
                        connectTimeoutMillis = 5000
                        socketTimeoutMillis = 8000
                    }
                }

                if (response.status == HttpStatusCode.OK) {
                    val responseText = response.bodyAsText()
                    val element = JSON.parseToJsonElement(responseText) as JsonObject
                    val data = element["data"] as JsonArray
                    
                    val deferredResults = data.take(30).map { item ->
                        async {
                            val obj = item.jsonObject
                            val resTitle = obj["title"]?.jsonPrimitive?.content ?: obj["song"]?.jsonPrimitive?.content ?: "Unknown"
                            val resArtist = obj["artist"]?.jsonPrimitive?.content ?: obj["artistName"]?.jsonPrimitive?.content ?: "Unknown Artist"
                            val albumName = obj["album"]?.jsonPrimitive?.content ?: obj["albumName"]?.jsonPrimitive?.content
                            val durationVal = obj["duration"]?.jsonPrimitive?.longOrNull
                            val videoId = obj["videoId"]?.jsonPrimitive?.content
                            
                            val normalizedResArtist = resArtist.lowercase().trim()
                            val normalizedResTitle = resTitle.lowercase().trim()
                            
                            // Advanced Filtering: Check both Title and Artist
                            val titleSimilarity = JW_SIMILARITY.apply(normalizedSearchTitle, normalizedResTitle)
                            val titleMatches = titleSimilarity > 0.8 || normalizedResTitle.contains(normalizedSearchTitle) || normalizedSearchTitle.contains(normalizedResTitle)
                            
                            if (!titleMatches) {
                                Log.v(TAG, "Filtering out BetterLyrics hit due to title mismatch: $resTitle (Similarity: $titleSimilarity)")
                                return@async null
                            }

                            if (!isUnknownArtist) {
                                val artistSimilarity = JW_SIMILARITY.apply(normalizedSearchArtist, normalizedResArtist)
                                val artistMatches = artistSimilarity > 0.75 || normalizedResArtist.contains(normalizedSearchArtist) || normalizedSearchArtist.contains(normalizedResArtist)
                                
                                if (!artistMatches) {
                                    Log.v(TAG, "Filtering out BetterLyrics hit due to artist mismatch: $resArtist (Similarity: $artistSimilarity)")
                                    return@async null
                                }
                            }

                            Log.v(TAG, "Processing BetterLyrics hit: $resTitle by $resArtist (videoId: $videoId)")
                            
                            var lyricsContent = obj["lyrics"]?.jsonPrimitive?.content
                            if (lyricsContent.isNullOrBlank()) {
                                lyricsContent = fetchLyricsByIdInternal(videoId, resTitle, resArtist)?.lyrics
                            }

                            if (!lyricsContent.isNullOrBlank()) {
                                LyricsSearchResult(
                                    title = resTitle,
                                    artist = resArtist,
                                    album = albumName,
                                    duration = durationVal,
                                    instrumental = false,
                                    provider = name,
                                    lyrics = RawLyrics.Remote(
                                        synced = RawLyrics.Remote.Content(name, lyricsContent)
                                    )
                                )
                            } else null
                        }
                    }
                    val currentHits = deferredResults.awaitAll().filterNotNull()
                    allRelevantResults.addAll(currentHits)
                    
                    if (allRelevantResults.any { it.artist.contains(artist, ignoreCase = true) && it.title.contains(title, ignoreCase = true) }) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.v(TAG, "BetterLyrics search attempt failed: ${e.message}")
            }
        }
        
        allRelevantResults.distinctBy { "${it.title}${it.artist}" }
            .sortedWith(compareByDescending<LyricsSearchResult> { it.artist.contains(artist, ignoreCase = true) }
                .thenByDescending { it.title.contains(title, ignoreCase = true) })
    }

    private suspend fun fetchLyricsByIdInternal(
        videoId: String?,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return try {
            val url = if (!videoId.isNullOrBlank()) {
                "https://unison.boidu.dev/lyrics?v=$videoId"
            } else {
                "https://unison.boidu.dev/lyrics?song=${Uri.encode(title)}&artist=${Uri.encode(artist)}"
            }
            
            val response = client.get(url) {
                timeout {
                    connectTimeoutMillis = 4000
                    socketTimeoutMillis = 6000
                }
            }
            
            if (response.status == HttpStatusCode.OK) {
                val responseText = response.bodyAsText()
                val element = JSON.parseToJsonElement(responseText) as JsonObject
                val data = element["data"]?.jsonObject
                val lyrics = data?.get("lyrics")?.jsonPrimitive?.content
                if (!lyrics.isNullOrBlank()) {
                    RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, lyrics))
                } else null
            } else null
        } catch (e: Exception) {
            Log.v(TAG, "BetterLyrics fetch by ID failed: ${e.message}")
            fetchLyricsInternal(title, artist, 0L) // Final fallback to old API
        }
    }

    private suspend fun fetchLyricsInternal(
        title: String,
        artist: String,
        duration: Long
    ): RawLyrics.Remote? {
        return try {
            // First try the new Unison API for lyrics fetching if search query has videoId or ID
            // But we already have the lyrics in the search results mapping above if they exist.
            // This fallback uses the classic BetterLyrics API which is more reliable for direct title/artist lookup.
            val response = client.get(GET_LYRICS_URL) {
                parameter("s", title)
                parameter("a", artist)
                if (duration > 0) {
                    parameter("d", (duration / 1000))
                }
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val result = response.body<BetterLyricsResponse>()
                if (!result.ttml.isNullOrEmpty()) {
                    RawLyrics.Remote(
                        synced = RawLyrics.Remote.Content(name, result.ttml)
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.v(TAG, "BetterLyrics fetch fallback failed for $title $artist: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "BetterLyricsApi"
        private const val GET_LYRICS_URL = "https://lyrics-api.boidu.dev/getLyrics"
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowSpecialFloatingPointValues = true
        }
        private val JW_SIMILARITY = JaroWinklerSimilarity()
    }
}
