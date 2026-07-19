package com.mardous.projectmusic.ui.screen.info

import com.mardous.projectmusic.data.local.database.analytics.PlayCountEntity

data class PlayInfoResult(
    val playCount: Int,
    val skipCount: Int,
    val lastPlayDate: Long,
    val mostPlayedTracks: List<PlayCountEntity>
)

data class AudioHeaderInfo(
    val format: String? = null,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val channels: String? = null,
    val variableBitrate: Boolean,
    val lossless: Boolean
)

data class SongInfo(
    val playCount: String? = null,
    val skipCount: String? = null,
    val lastPlayedDate: String? = null,
    val overallScore: String? = null,
    val overallRank: String? = null,
    val playRank: String? = null,
    val durationRank: String? = null,
    val avgCompletion: String? = null,
    val totalPauses: String? = null,
    val totalSeeks: String? = null,
    val firstPlayedDate: String? = null,
    val longestStreak: String? = null,
    val effectiveDuration: String? = null,
    val monthlyPlays: String? = null,
    val weeklyPlays: String? = null,
    val dailyPlays: String? = null,
    val hasEmbeddedLyrics: String? = null,
    val hasSynchronizedLyrics: String? = null,
    val filePath: String? = null,
    val fileSize: String? = null,
    val trackLength: String? = null,
    val dateModified: String? = null,
    val audioHeaderInfo: AudioHeaderInfo? = null,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val albumYear: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val composer: String? = null,
    val conductor: String? = null,
    val publisher: String? = null,
    val lyricist: String? = null,
    val arranger: String? = null,
    val genre: String? = null,
    val replayGain: String? = null,
    val comment: String? = null
) {
    // True only when every optional metadata field is absent; gates the metadata section in the UI.
    // lyricist and arranger are included alongside composer/conductor/publisher for consistency:
    // the section should appear whenever any credit or descriptive tag has content.
    val isMissingMetadata: Boolean = album.isNullOrEmpty() && albumArtist.isNullOrEmpty() &&
            albumYear.isNullOrEmpty() && trackNumber.isNullOrEmpty() && discNumber.isNullOrEmpty() &&
            composer.isNullOrEmpty() && conductor.isNullOrEmpty() && publisher.isNullOrEmpty() &&
            lyricist.isNullOrEmpty() && arranger.isNullOrEmpty() && genre.isNullOrEmpty()

    companion object {
        val Empty = SongInfo()
    }
}