package com.mardous.booming.data.remote.lyrics.api.genius

import android.util.Log
import com.mardous.booming.BuildConfig
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.GeniusSearchResponse
import com.mardous.booming.util.Constants.USER_AGENT
import com.mardous.booming.util.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

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
            }.body<GeniusSearchResponse>()
        } catch (e: Exception) {
            Log.e(TAG, "Genius search failed", e)
            return null
        }

        val hits = searchResponse.response.hits
        if (hits.isEmpty()) return null

        // Try first 3 hits for a match
        for (hit in hits.take(3)) {
            val geniusUrl = hit.result.url
            val lyricsText = scrapeLyrics(geniusUrl)
            if (!lyricsText.isNullOrBlank()) {
                return RawLyrics.Remote(
                    plain = RawLyrics.Remote.Content(name, lyricsText)
                )
            }
        }

        return null
    }

    private suspend fun scrapeLyrics(url: String): String? {
        return try {
            val responseBody = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
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
