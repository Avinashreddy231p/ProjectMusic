package com.mardous.projectmusic.data.remote.lyrics.api.lyricsplus

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsPlusResponse
import com.mardous.projectmusic.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

class LyricsPlusApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "LyricsPlus"
    override val networkFeature = NetworkFeature.Lyrics.LyricsPlus

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return try {
            val response = client.get(BASE_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("title", title)
                parameter("artist", artist)
                parameter("duration", song.duration / 1000)
                // Prefer apple and musixmatch sources
                parameter("source", "apple,musixmatch,spotify")
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
            Log.e(TAG, "LyricsPlus request failed", e)
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
        private const val BASE_URL = "https://lyricsplus.prjktla.my.id/v2/lyrics/get"
    }
}
