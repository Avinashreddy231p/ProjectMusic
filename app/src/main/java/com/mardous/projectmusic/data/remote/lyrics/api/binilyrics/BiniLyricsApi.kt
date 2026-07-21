package com.mardous.projectmusic.data.remote.lyrics.api.binilyrics

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.BiniLyricsResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode

class BiniLyricsApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "BiniLyrics"
    override val networkFeature = NetworkFeature.Lyrics.BiniLyrics

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return try {
            val response = client.get(BASE_URL) {
                parameter("s", title)
                parameter("a", artist)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val result = response.body<BiniLyricsResponse>()
                if (result.success && !result.lyrics.isNullOrBlank()) {
                    RawLyrics.Remote(
                        synced = RawLyrics.Remote.Content(name, result.lyrics)
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.v(TAG, "BiniLyrics fetch failed: ${e.message}")
            null
        }
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> {
        val lyrics = downloadLyrics(song, title, artist)
        return if (lyrics != null) {
            listOf(
                LyricsSearchResult(
                    title = title,
                    artist = artist,
                    album = null,
                    duration = null,
                    instrumental = false,
                    provider = name,
                    lyrics = lyrics
                )
            )
        } else emptyList()
    }

    companion object {
        private const val TAG = "BiniLyricsApi"
        private const val BASE_URL = "https://api.better-lyrics.xyz/getLyrics"
    }
}
