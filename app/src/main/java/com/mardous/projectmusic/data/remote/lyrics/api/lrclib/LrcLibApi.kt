package com.mardous.projectmusic.data.remote.lyrics.api.lrclib

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.LRCLibResponse
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent

class LrcLibApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "LRCLib"
    override val networkFeature = NetworkFeature.Lyrics.LRCLib

    override suspend fun downloadLyrics(song: Song, title: String, artist: String): RawLyrics.Remote? {
        return try {
            val lyrics = client.get(LRCLIB_API_URL) {
                userAgent(USER_AGENT)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
                url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
                url.encodedParameters.append("album_name", song.albumName.encodeURLParameter())
            }.body<List<LRCLibResponse>>()
            if (lyrics.isEmpty()) {
                null
            } else {
                val songDurationInSeconds = (song.duration / 1000).toDouble()
                val matchingLyrics = lyrics.firstOrNull {
                    val maxValue = maxOf(songDurationInSeconds, it.durationInSeconds)
                    val minValue = minOf(songDurationInSeconds, it.durationInSeconds)
                    ((maxValue - minValue) < 2)
                } ?: lyrics.firstOrNull { !it.plainLyrics.isNullOrEmpty() }
                    ?: null
                RawLyrics.Remote(
                    plain = RawLyrics.Remote.Content(name, matchingLyrics?.plainLyrics),
                    synced = RawLyrics.Remote.Content(name, matchingLyrics?.syncedLyrics),
                    instrumental = matchingLyrics?.instrumental ?: false
                )
            }
        } catch (e: Exception) {
            Log.e("LrcLibApi", "LRCLib request failed", e)
            null
        }
    }

    override suspend fun searchLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> {
        return try {
            val results = client.get(LRCLIB_API_URL) {
                userAgent(USER_AGENT)
                timeout {
                    connectTimeoutMillis = 5000
                    socketTimeoutMillis = 10000
                    requestTimeoutMillis = 15000
                }
                url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
            }.body<List<LRCLibResponse>>()

            results.map {
                LyricsSearchResult(
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    duration = (it.durationInSeconds * 1000).toLong(),
                    instrumental = it.instrumental,
                    provider = name,
                    lyrics = RawLyrics.Remote(
                        plain = RawLyrics.Remote.Content(name, it.plainLyrics),
                        synced = RawLyrics.Remote.Content(name, it.syncedLyrics),
                        instrumental = it.instrumental
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("LrcLibApi", "LRCLib search failed", e)
            emptyList()
        }
    }

    companion object {
        private const val LRCLIB_API_URL = "https://lrclib.net/api/search"
    }
}