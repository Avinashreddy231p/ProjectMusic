package com.mardous.projectmusic.ui.screen.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.data.local.MetadataReader
import com.mardous.projectmusic.data.local.database.analytics.PlayCountEntity
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.mapper.toPlayCount
import com.mardous.projectmusic.data.model.Album
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.files.asReadableFileSize
import com.mardous.projectmusic.extensions.files.formatFixed
import com.mardous.projectmusic.extensions.files.getHumanReadableSize
import com.mardous.projectmusic.extensions.files.getPrettyAbsolutePath
import com.mardous.projectmusic.extensions.files.toAudioFile
import com.mardous.projectmusic.extensions.media.asNumberOfTimes
import com.mardous.projectmusic.extensions.media.replayGainStr
import com.mardous.projectmusic.extensions.media.songDurationStr
import com.mardous.projectmusic.extensions.utilities.dateStr
import com.mardous.projectmusic.extensions.utilities.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioHeader
import java.io.File

class InfoViewModel(private val repository: Repository) : ViewModel() {

    private val _songInfoUiState = MutableStateFlow(
        SongInfoUiState(
            isLoading = true,
            isSuccess = false
        )
    )
    val songInfoUiState = _songInfoUiState.asStateFlow()

    fun loadAlbum(id: Long): LiveData<Album> = liveData(Dispatchers.IO) {
        if (id != -1L) {
            emit(repository.albumById(id))
        } else {
            emit(Album.empty)
        }
    }

    fun loadArtist(id: Long, name: String?): LiveData<Artist> = liveData(Dispatchers.IO) {
        if (name.isNullOrEmpty()) {
            emit(repository.artistById(id))
        } else if (id == -1L) {
            emit(repository.albumArtistByName(name))
        } else {
            emit(Artist.empty)
        }
    }

    fun playInfo(songs: List<Song>): LiveData<PlayInfoResult> = liveData(Dispatchers.IO) {
        val playCountEntities = repository.findSongsInPlayCount(songs).sortedByDescending { it.playCount }
        if (playCountEntities.isEmpty()) {
            emit(PlayInfoResult(-1, -1, -1, songs.map { it.toPlayCount() }))
        } else {
            val totalPlayCount = playCountEntities.sumOf { it.playCount }
            val totalSkipCount = playCountEntities.sumOf { it.skipCount }
            val lastPlayDate = playCountEntities.maxOf { it.timePlayed }
            emit(PlayInfoResult(totalPlayCount, totalSkipCount, lastPlayDate, playCountEntities))
        }
    }

    private fun formatDurationMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%dh %dm".format(hours, minutes)
        else if (minutes > 0) "%dm %ds".format(minutes, seconds)
        else "%ds".format(seconds)
    }

    private fun formatRelativeTime(timestamp: Long, context: Context): String {
        return if (timestamp > 0) {
            android.text.format.DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                android.text.format.DateUtils.SECOND_IN_MILLIS
            ).toString()
        } else "Never"
    }

    fun refreshSongInfo(context: Context, song: Song) = viewModelScope.launch(Dispatchers.IO) {
        val uiState = SongInfoUiState(isLoading = true, isSuccess = false)
        _songInfoUiState.value = uiState

        val songInfo = runCatching {
            val stats = repository.songStats(song.id)
            val rank = repository.songRank(song.id)

            val playCountRaw = (stats?.totalPlayCount ?: 0).toInt()
            val skipCountRaw = (stats?.totalSkipCount ?: 0).toInt()
            val totalDurationRaw = stats?.totalListeningDuration ?: 0L

            val playCountStr = playCountRaw.asNumberOfTimes(context)
            val skipCountStr = skipCountRaw.asNumberOfTimes(context)
            val lastPlayed = formatRelativeTime(stats?.lastPlayedTimestamp ?: 0L, context)
            val firstPlayed = formatRelativeTime(stats?.firstPlayedTimestamp ?: 0L, context)

            val overallScoreStr = if ((rank?.overallScore ?: 0.0) > 0.0) {
                val score = "%.2f".format(rank!!.overallScore)
                val rankText = if (rank.overallRank > 0) " (#${rank.overallRank})" else ""
                "$score$rankText"
            } else "—"

            val avgCompletion = if (stats != null) "%.1f%%".format(stats.avgCompletionPercentage) else "—"
            val totalPauses = stats?.totalPauseCount?.toString() ?: "—"
            val totalSeeks = stats?.totalSeekCount?.toString() ?: "—"
            val longestStreak = if ((stats?.longestListeningStreak ?: 0) > 0) formatDurationMs(stats!!.longestListeningStreak.toLong()) else "—"
            val effectiveDuration = if ((stats?.effectiveListeningDuration ?: 0L) > 0) {
                val duration = formatDurationMs(stats!!.effectiveListeningDuration)
                val rankText = if ((rank?.durationRank ?: 0) > 0) " (#${rank!!.durationRank})" else ""
                "$duration$rankText"
            } else "—"
            val monthlyPlays = stats?.monthlyPlayCount?.toString() ?: "—"
            val weeklyPlays = stats?.weeklyPlayCount?.toString() ?: "—"
            val dailyPlays = stats?.dailyPlayCount?.toString() ?: "—"

            val durationStr = if (totalDurationRaw > 0) formatDurationMs(totalDurationRaw) else "—"
            
            val dateModified = song.dateModified.format(context)
            val year = if (song.year > 0) song.year.toString() else null
            val trackLength = song.songDurationStr()
            val replayGain = song.replayGainStr(context)

            val metadataReader = MetadataReader(song.uri)
            val embeddedLyrics = metadataReader.value(MetadataReader.LYRICS)
            val hasEmbeddedLyrics = if (embeddedLyrics != null) "Yes" else "No"

            val commonFields = SongInfo(
                playCount = if (playCountRaw > 0) {
                    val rankText = if ((rank?.playRank ?: 0) > 0) " (#${rank!!.playRank})" else ""
                    "$playCountStr$rankText"
                } else "—",
                skipCount = skipCountStr,
                lastPlayedDate = lastPlayed,
                overallScore = overallScoreStr,
                playRank = if ((rank?.playRank ?: 0) > 0) "#${rank!!.playRank}" else "—",
                durationRank = if ((rank?.durationRank ?: 0) > 0) "#${rank!!.durationRank}" else "—",
                avgCompletion = avgCompletion,
                totalPauses = totalPauses,
                totalSeeks = totalSeeks,
                firstPlayedDate = firstPlayed,
                longestStreak = longestStreak,
                effectiveDuration = effectiveDuration,
                monthlyPlays = monthlyPlays,
                weeklyPlays = weeklyPlays,
                dailyPlays = dailyPlays,
                hasEmbeddedLyrics = hasEmbeddedLyrics,
                filePath = File(song.data).getPrettyAbsolutePath(),
                fileSize = song.size.asReadableFileSize(),
                trackLength = trackLength,
                dateModified = dateModified,
                title = song.title,
                albumYear = year,
                replayGain = replayGain
            )

            if (!metadataReader.hasMetadata) {
                commonFields
            } else {
                val file = File(song.data)
                val filePath = file.getPrettyAbsolutePath()
                val fileSize = file.getHumanReadableSize()

                val audioHeaderInfo = getAudioHeader(file.toAudioFile()?.audioHeader, metadataReader)

                val title = metadataReader.first(MetadataReader.TITLE)
                val album = metadataReader.first(MetadataReader.ALBUM)
                val artist = metadataReader.merge(MetadataReader.ARTIST)
                val albumArtist = metadataReader.first(MetadataReader.ALBUM_ARTIST)

                val trackNumber = getNumberAndTotal(
                    metadataReader.value(MetadataReader.TRACK_NUMBER),
                    metadataReader.value(MetadataReader.TRACK_TOTAL)
                )
                val discNumber = getNumberAndTotal(
                    metadataReader.value(MetadataReader.DISC_NUMBER),
                    metadataReader.value(MetadataReader.DISC_TOTAL)
                )

                val composer = metadataReader.merge(MetadataReader.COMPOSER)
                val conductor = metadataReader.merge(MetadataReader.PRODUCER)
                val publisher = metadataReader.merge(MetadataReader.COPYRIGHT)
                val lyricist = metadataReader.merge(MetadataReader.LYRICIST)
                val arranger = metadataReader.merge(MetadataReader.ARRANGER)
                val genre = metadataReader.merge(MetadataReader.GENRE)
                val comment = metadataReader.value(MetadataReader.COMMENT)

                commonFields.copy(
                    filePath = filePath,
                    fileSize = fileSize,
                    audioHeaderInfo = audioHeaderInfo,
                    title = title ?: song.title,
                    album = album,
                    artist = artist,
                    albumArtist = albumArtist,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    composer = composer,
                    conductor = conductor,
                    publisher = publisher,
                    lyricist = lyricist,
                    arranger = arranger,
                    genre = genre,
                    comment = comment
                )
            }
        }

        _songInfoUiState.value = uiState.copy(
            isLoading = false,
            isSuccess = songInfo.isSuccess,
            info = songInfo.getOrDefault(SongInfo.Empty)
        )
    }

    private fun getNumberAndTotal(number: String?, total: String?): String? {
        val numberInt = number?.toIntOrNull() ?: return null
        val totalInt = total?.toIntOrNull()
        return if (totalInt == null || totalInt == 0) {
            numberInt.toString().padStart(2, '0')
        } else {
            "%02d/%02d".format(numberInt, totalInt)
        }
    }

    private fun getAudioHeader(header: AudioHeader?, metadataReader: MetadataReader): AudioHeaderInfo {
        return AudioHeaderInfo(
            format = header?.formatFixed,
            bitrate = metadataReader.bitrate(),
            sampleRate = metadataReader.sampleRate(),
            channels = metadataReader.channelName(),
            variableBitrate = header?.isVariableBitRate == true,
            lossless = header?.isLossless == true
        )
    }
}