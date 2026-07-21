/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.data.remote.lyrics

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.lyrics.RawLyrics
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.remote.lyrics.api.LyricsApi
import com.mardous.projectmusic.data.remote.lyrics.api.betterlyrics.BetterLyricsApi
import com.mardous.projectmusic.data.remote.lyrics.api.binilyrics.BiniLyricsApi
import com.mardous.projectmusic.data.remote.lyrics.api.genius.GeniusApi
import com.mardous.projectmusic.data.remote.lyrics.api.kugou.KugouApi
import com.mardous.projectmusic.data.remote.lyrics.api.lrclib.LrcLibApi
import com.mardous.projectmusic.data.remote.lyrics.api.lyrically.LyricallyApi
import com.mardous.projectmusic.data.remote.lyrics.api.lyricsplus.LyricsPlusApi
import com.mardous.projectmusic.data.remote.lyrics.api.netease.NetEaseApi
import com.mardous.projectmusic.data.remote.lyrics.model.LyricsSearchResult
import com.mardous.projectmusic.extensions.media.albumArtistName
import com.mardous.projectmusic.extensions.media.extractMainArtistName
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

class LyricsDownloadService(client: HttpClient) {

    private val lyricsApi = listOf(
        LrcLibApi(client),
        NetEaseApi(client),
        BiniLyricsApi(client),
        KugouApi(client),
        LyricallyApi(client),
        BetterLyricsApi(client),
        LyricsPlusApi(client),
        GeniusApi(client)
    )

    @Throws(IOException::class)
    suspend fun remoteLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        fromUser: Boolean = false
    ): RawLyrics.Remote {
        var result = RawLyrics.Remote()

        if (song == Song.emptySong || !NetworkFeature.isOnline(ignoreWifiSetting = fromUser))
            return result

        try {
            val cleanedTitle = cleanTitle(title)
            val cleanedArtist = artist.extractMainArtistName()
            for (api in lyricsApi) {
                if (!api.networkFeature.isEnabled)
                    continue

                val apiResult = runCatching { api.downloadLyrics(song, cleanedTitle, cleanedArtist) }
                if (apiResult.isFailure) {
                    Log.e(TAG, "Error during lyrics request", apiResult.exceptionOrNull())
                }

                val response = apiResult.getOrNull() ?: continue

                result = result.accept(response)
                if (result.hasBoth) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics download failed with error:", e)
        }

        return result
    }

    suspend fun searchRemoteLyrics(
        song: Song,
        title: String,
        artist: String
    ): List<LyricsSearchResult> = coroutineScope {
        if (song == Song.emptySong || !NetworkFeature.isOnline(ignoreWifiSetting = true))
            return@coroutineScope emptyList()

        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = artist.extractMainArtistName()
        Log.d(TAG, "Starting lyrics search for: $cleanedTitle by $cleanedArtist")

        val deferredResults = lyricsApi.map { api ->
            async {
                if (!api.networkFeature.isEnabled) {
                    Log.d(TAG, "${api.name} is disabled")
                    return@async emptyList()
                }
                Log.d(TAG, "Querying ${api.name}...")
                runCatching {
                    api.searchLyrics(song, cleanedTitle, cleanedArtist)
                }.onSuccess {
                    Log.d(TAG, "${api.name} returned ${it.size} results")
                }.onFailure {
                    Log.e(TAG, "Search failed for ${api.name}", it)
                }.getOrDefault(emptyList())
            }
        }

        val allResults = deferredResults.awaitAll().flatten().sortedByDescending { it.isSynced }
        Log.d(TAG, "Total results found: ${allResults.size}")
        allResults.forEach { Log.v(TAG, "Result: ${it.title} by ${it.artist} (${it.provider})") }
        allResults
    }

    /**
     * Taken from [Metrolist](https://github.com/MetrolistGroup/Metrolist).
     */
    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in TITLE_CLEANUP_PATTERNS) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    companion object {
        private const val TAG = "LyricsDownloadService"

        private val TITLE_CLEANUP_PATTERNS = listOf(
            Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
            Regex("""\s*【.*?】"""),
            Regex("""\s*\|.*$"""),
            Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
        )
    }
}