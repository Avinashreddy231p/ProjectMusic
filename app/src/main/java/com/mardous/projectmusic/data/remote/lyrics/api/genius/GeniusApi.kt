package com.mardous.projectmusic.data.remote.lyrics.api.genius

import android.util.Log
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.GeniusSearchResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.util.Constants.USER_AGENT
import com.mardous.projectmusic.util.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GeniusApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "Genius"
    override val networkFeature = NetworkFeature.Lyrics.Genius

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        val userApiKey = Preferences.geniusApiKey
        val apiKey = userApiKey.ifBlank { BuildConfig.GENIUS_API_KEY }
        
        val searchResponse = try {
            client.get("https://api.genius.com/search") {
                header("Authorization", "Bearer $apiKey")
                parameter("q", "$title $artist")
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }.body<GeniusSearchResponse>()
        } catch (e: Exception) {
            Log.e(TAG, "Genius search failed", e)
            return null
        }

        val hits = searchResponse.response.hits
        if (hits.isEmpty()) return null

        // Try first 3 hits for a match
        for (hit in hits.take(3)) {
            val lyricsText = scrapeLyrics(hit.result.url)
            if (!lyricsText.isNullOrBlank()) {
                return RawLyrics.Remote(
                    plain = RawLyrics.Remote.Content(name, lyricsText)
                )
            }
        }

        return null
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        val userApiKey = Preferences.geniusApiKey
        val apiKey = userApiKey.ifBlank { BuildConfig.GENIUS_API_KEY }

        if (apiKey.isBlank()) {
            Log.w(TAG, "Genius API key is empty, search will likely fail")
        }

        try {
            Log.d(TAG, "Searching Genius for: $title $artist")
            val searchResponse = client.get("https://api.genius.com/search") {
                header("Authorization", "Bearer $apiKey")
                parameter("q", "$title $artist")
            }.body<GeniusSearchResponse>()

            val deferredResults = searchResponse.response.hits.take(5).map { hit ->
                async {
                    val lyricsText = scrapeLyrics(hit.result.url)
                    if (!lyricsText.isNullOrBlank()) {
                        LyricsSearchResult(
                            title = hit.result.title,
                            artist = hit.result.primaryArtist.name,
                            album = null,
                            duration = null,
                            instrumental = false,
                            provider = name,
                            lyrics = RawLyrics.Remote(plain = RawLyrics.Remote.Content(name, lyricsText))
                        )
                    } else {
                        Log.v(TAG, "No lyrics scraped for ${hit.result.url}")
                        null
                    }
                }
            }
            val results = deferredResults.awaitAll().filterNotNull()
            Log.d(TAG, "Genius found ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Genius search failed", e)
            emptyList()
        }
    }

    private suspend fun scrapeLyrics(url: String): String? {
        return try {
            val responseBody = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }.bodyAsText()

            // Genius lyrics are inside divs with class starting with "Lyrics__Container"
            val lyricsRegex = Regex("""<div[^>]*class="[^"]*Lyrics__Container[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val matches = lyricsRegex.findAll(responseBody)
            if (matches.any()) {
                val combinedLyrics = matches.joinToString("\n") { it.groupValues[1] }
                return cleanHtml(combinedLyrics)
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Scraping failed for $url", e)
            null
        }
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<br/?>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    companion object {
        private const val TAG = "GeniusApi"
    }
}
