package com.mardous.projectmusic.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.core.SongMetadataEntity
import com.mardous.projectmusic.data.local.database.core.SongRankingEntity
import com.mardous.projectmusic.data.local.database.core.SongStatsEntity
import com.mardous.projectmusic.data.local.database.dao.RankingDao
import com.mardous.projectmusic.data.local.database.metadata.AlbumEntity
import com.mardous.projectmusic.data.local.database.metadata.AlbumArtistEntity
import com.mardous.projectmusic.data.local.database.metadata.ArtistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SongExportHelper(private val context: Context, private val rankingDao: RankingDao) {

    data class ExportResult(val csvUri: Uri, val jsonUri: Uri)

    suspend fun export(): ExportResult = withContext(Dispatchers.IO) {
        val songs = rankingDao.getAllSongs()
        val stats = rankingDao.getAllSongStats().associateBy { it.songKey }
        val rankings = rankingDao.getAllSongRankings().groupBy { it.songKey }
        val metadata = rankingDao.getAllSongMetadata().associateBy { it.songKey }
        val lyrics = rankingDao.getAllLyrics().associateBy { it.id }
        val albums = rankingDao.getAllAlbums()
        val artists = rankingDao.getAllArtists()
        val albumArtists = rankingDao.getAllAlbumArtists()

        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(context.filesDir, "exports").also { it.mkdirs() }

        val csvFile = File(dir, "songs_$dateStamp.csv")
        val jsonFile = File(dir, "songs_$dateStamp.json")

        csvFile.bufferedWriter().use { writer ->
            writer.write(buildCsvHeader())
            writer.newLine()
            for (song in songs) {
                writer.write(toCsvRow(song, stats[song.songKey], rankings[song.songKey], metadata[song.songKey], lyrics[song.mediaStoreId]))
                writer.newLine()
            }
            writer.newLine()
            writer.write(buildAlbumCsv(albums))
            writer.newLine()
            writer.write(buildArtistCsv(artists, albumArtists))
        }

        jsonFile.bufferedWriter().use { writer ->
            writer.write(buildJson(songs, stats, rankings, metadata, lyrics, albums, artists, albumArtists))
        }

        val csvUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", csvFile)
        val jsonUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", jsonFile)
        ExportResult(csvUri, jsonUri)
    }

    private fun buildCsvHeader(): String = listOf(
        "song_key", "media_store_id", "data", "title", "duration", "album_id", "album_name",
        "artist_id", "artist_name", "album_artist", "genre_name", "volume_name", "bitrate",
        "total_play_count", "total_skip_count", "total_listening_duration", "effective_listening_duration",
        "avg_completion_percentage", "last_played_timestamp", "first_played_timestamp",
        "total_pause_count", "total_seek_count", "longest_listening_streak",
        "monthly_play_count", "weekly_play_count", "daily_play_count", "last_synced",
        "ranking_type", "play_rank", "duration_rank", "overall_rank", "overall_score",
        "ranking_last_updated", "ranking_version",
        "composer", "lyricist", "conductor", "publisher", "copyright", "genre_metadata",
        "year", "track_number", "disc_number", "file_size", "date_added", "date_modified",
        "audio_format", "sample_rate", "channel_count", "bit_depth", "mime_type",
        "replay_gain", "bpm", "key_signature", "isrc",
        "musicbrainz_track_id", "musicbrainz_album_id", "musicbrainz_artist_id",
        "musicbrainz_work_id", "iswc", "label", "catalog_number", "arranger", "producer", "engineer",
        "language", "artwork_uri", "checksum",
        "is_explicit", "is_compilation", "is_favorite", "rating",
        "has_embedded_lyrics", "embedded_lyrics"
    ).joinToString(",") { escapeCsv(it) }

    private fun toCsvRow(
        song: SongEntity,
        st: SongStatsEntity?,
        ranks: List<SongRankingEntity>?,
        meta: SongMetadataEntity?,
        lyr: com.mardous.projectmusic.data.local.database.intel.LyricsEntity?
    ): String {
        val overallRank = ranks?.find { it.rankingType == "overall" }
        return listOf(
            song.songKey, song.mediaStoreId, song.data, song.title, song.duration,
            song.albumId, song.albumName, song.artistId, song.artistName,
            song.albumArtist, song.genreName, song.volumeName, song.bitrate,
            st?.totalPlayCount, st?.totalSkipCount, st?.totalListeningDuration, st?.effectiveListeningDuration,
            st?.avgCompletionPercentage, st?.lastPlayedTimestamp, st?.firstPlayedTimestamp,
            st?.totalPauseCount, st?.totalSeekCount, st?.longestListeningStreak,
            st?.monthlyPlayCount, st?.weeklyPlayCount, st?.dailyPlayCount, st?.lastSynced,
            "overall", overallRank?.playRank, overallRank?.durationRank, overallRank?.overallRank,
            overallRank?.overallScore, overallRank?.lastUpdated, overallRank?.rankingVersion,
            meta?.composer, meta?.lyricist, meta?.conductor, meta?.publisher, meta?.copyright, meta?.genre,
            meta?.year, meta?.trackNumber, meta?.discNumber, meta?.fileSize, meta?.dateAdded, meta?.dateModified,
            meta?.audioFormat, meta?.sampleRate, meta?.channelCount, meta?.bitDepth, meta?.mimeType,
            meta?.replayGain, meta?.bpm, meta?.keySignature, meta?.isrc,
            meta?.musicbrainzTrackId, meta?.musicbrainzAlbumId, meta?.musicbrainzArtistId,
            meta?.musicbrainzWorkId, meta?.iswc, meta?.label, meta?.catalogNumber,
            meta?.arranger, meta?.producer, meta?.engineer,
            meta?.language, meta?.artworkUri, meta?.checksum,
            meta?.isExplicit, meta?.isCompilation, meta?.isFavorite, meta?.rating,
            if (lyr?.hasEmbeddedLyrics == true) "Yes" else "No",
            lyr?.embeddedLyrics
        ).joinToString(",") { escapeCsv(it?.toString()) }
    }

    private fun buildArtistCsv(artists: List<ArtistEntity>, albumArtists: List<AlbumArtistEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("# Artists")
        sb.appendLine(listOf(
            "artist_id", "name", "song_count", "total_duration", "play_count",
            "listening_duration", "overall_score", "play_rank", "duration_rank", "overall_rank", "about",
            "musicbrainz_id", "type", "gender", "country", "disambiguation", "begin_date", "end_date"
        ).joinToString(",") { escapeCsv(it) })
        for (a in artists) {
            sb.appendLine(listOf(
                a.id, a.name, a.songCount, a.totalDuration, a.playCount,
                a.listeningDuration, a.overallScore, a.playRank, a.durationRank, a.overallRank, null,
                a.musicbrainzId, a.type, a.gender, a.country, a.disambiguation, a.beginDate, a.endDate
            ).joinToString(",") { escapeCsv(it?.toString()) })
        }
        sb.appendLine("# Album Artists")
        sb.appendLine(listOf(
            "album_artist_id", "name", "album_count", "song_count", "total_duration", "play_count",
            "listening_duration", "overall_score", "play_rank", "duration_rank", "overall_rank", "about",
            "musicbrainz_id", "type", "gender", "country", "disambiguation", "begin_date", "end_date"
        ).joinToString(",") { escapeCsv(it) })
        for (a in albumArtists) {
            sb.appendLine(listOf(
                a.id, a.name, a.albumCount, a.songCount, a.totalDuration, a.playCount,
                a.listeningDuration, a.overallScore, a.playRank, a.durationRank, a.overallRank, null,
                a.musicbrainzId, a.type, a.gender, a.country, a.disambiguation, a.beginDate, a.endDate
            ).joinToString(",") { escapeCsv(it?.toString()) })
        }
        return sb.toString()
    }

    private fun buildAlbumCsv(albums: List<AlbumEntity>): String {
        val header = listOf(
            "album_id", "name", "album_artist", "release_year", "is_favorite",
            "song_count", "total_duration", "play_count", "listening_duration",
            "overall_score", "play_rank", "duration_rank", "overall_rank", "about",
            "musicbrainz_release_id", "release_country", "label", "catalog_number"
        ).joinToString(",") { escapeCsv(it) }
        val sb = StringBuilder()
        sb.appendLine("# Albums")
        sb.appendLine(header)
        for (a in albums) {
            sb.appendLine(listOf(
                a.id, a.name, a.albumArtist, a.releaseYear, a.isFavorite,
                a.songCount, a.totalDuration, a.playCount, a.listeningDuration,
                a.overallScore, a.playRank, a.durationRank, a.overallRank, null,
                a.musicbrainzReleaseId, a.releaseCountry, a.label, a.catalogNumber
            ).joinToString(",") { escapeCsv(it?.toString()) })
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    private fun buildJson(
        songs: List<SongEntity>,
        stats: Map<Long, SongStatsEntity>,
        rankings: Map<Long, List<SongRankingEntity>>,
        metadata: Map<Long, SongMetadataEntity>,
        lyrics: Map<Long, com.mardous.projectmusic.data.local.database.intel.LyricsEntity>,
        albums: List<AlbumEntity>,
        artists: List<ArtistEntity>,
        albumArtists: List<AlbumArtistEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"songs\": [")
        for ((i, song) in songs.withIndex()) {
            val st = stats[song.songKey]
            val ranks = rankings[song.songKey]
            val overallRank = ranks?.find { it.rankingType == "overall" }
            val meta = metadata[song.songKey]
            val lyr = lyrics[song.mediaStoreId]
            sb.appendLine("  {")
            sb.appendLine("    \"song_key\": ${song.songKey},")
            sb.appendLine("    \"media_store_id\": ${song.mediaStoreId},")
            sb.appendLine("    \"title\": ${jsonStr(song.title)},")
            sb.appendLine("    \"data\": ${jsonStr(song.data)},")
            sb.appendLine("    \"duration\": ${song.duration},")
            sb.appendLine("    \"album_id\": ${song.albumId},")
            sb.appendLine("    \"album_name\": ${jsonStr(song.albumName)},")
            sb.appendLine("    \"artist_id\": ${song.artistId},")
            sb.appendLine("    \"artist_name\": ${jsonStr(song.artistName)},")
            sb.appendLine("    \"album_artist\": ${jsonStr(song.albumArtist)},")
            sb.appendLine("    \"genre_name\": ${jsonStr(song.genreName)},")
            sb.appendLine("    \"volume_name\": ${jsonStr(song.volumeName)},")
            sb.appendLine("    \"bitrate\": ${song.bitrate},")
            appendJsonStats(sb, st)
            appendJsonRankings(sb, overallRank)
            appendJsonMetadata(sb, meta)
            sb.appendLine("    \"play_rank\": ${overallRank?.playRank ?: 0},")
            sb.appendLine("    \"duration_rank\": ${overallRank?.durationRank ?: 0},")
            sb.appendLine("    \"overall_rank\": ${overallRank?.overallRank ?: 0},")
            sb.appendLine("    \"overall_score\": ${overallRank?.overallScore ?: 0.0},")
            sb.appendLine("    \"has_embedded_lyrics\": ${lyr?.hasEmbeddedLyrics ?: false},")
            sb.appendLine("    \"embedded_lyrics\": ${jsonStr(lyr?.embeddedLyrics)}")
            sb.append(if (i < songs.lastIndex) "  },\n" else "  }\n")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"albums\": [")
        for ((i, a) in albums.withIndex()) {
            sb.appendLine("  {")
            sb.appendLine("    \"album_id\": ${a.id},")
            sb.appendLine("    \"name\": ${jsonStr(a.name)},")
            sb.appendLine("    \"album_artist\": ${jsonStr(a.albumArtist)},")
            sb.appendLine("    \"release_year\": ${a.releaseYear},")
            sb.appendLine("    \"is_favorite\": ${a.isFavorite},")
            sb.appendLine("    \"song_count\": ${a.songCount},")
            sb.appendLine("    \"total_duration\": ${a.totalDuration},")
            sb.appendLine("    \"play_count\": ${a.playCount},")
            sb.appendLine("    \"listening_duration\": ${a.listeningDuration},")
            sb.appendLine("    \"overall_score\": ${a.overallScore},")
            sb.appendLine("    \"play_rank\": ${a.playRank},")
            sb.appendLine("    \"duration_rank\": ${a.durationRank},")
            sb.appendLine("    \"overall_rank\": ${a.overallRank},")
            sb.appendLine("    \"about\": null,")
            sb.appendLine("    \"musicbrainz_release_id\": ${jsonStr(a.musicbrainzReleaseId)},")
            sb.appendLine("    \"release_country\": ${jsonStr(a.releaseCountry)},")
            sb.appendLine("    \"label\": ${jsonStr(a.label)},")
            sb.appendLine("    \"catalog_number\": ${jsonStr(a.catalogNumber)}")
            sb.append(if (i < albums.lastIndex) "  },\n" else "  }\n")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"artists\": [")
        for ((i, a) in artists.withIndex()) {
            sb.appendLine("  {")
            sb.appendLine("    \"artist_id\": ${a.id},")
            sb.appendLine("    \"name\": ${jsonStr(a.name)},")
            sb.appendLine("    \"song_count\": ${a.songCount},")
            sb.appendLine("    \"total_duration\": ${a.totalDuration},")
            sb.appendLine("    \"play_count\": ${a.playCount},")
            sb.appendLine("    \"listening_duration\": ${a.listeningDuration},")
            sb.appendLine("    \"overall_score\": ${a.overallScore},")
            sb.appendLine("    \"play_rank\": ${a.playRank},")
            sb.appendLine("    \"duration_rank\": ${a.durationRank},")
            sb.appendLine("    \"overall_rank\": ${a.overallRank},")
            sb.appendLine("    \"about\": null,")
            sb.appendLine("    \"musicbrainz_id\": ${jsonStr(a.musicbrainzId)},")
            sb.appendLine("    \"type\": ${jsonStr(a.type)},")
            sb.appendLine("    \"gender\": ${jsonStr(a.gender)},")
            sb.appendLine("    \"country\": ${jsonStr(a.country)},")
            sb.appendLine("    \"disambiguation\": ${jsonStr(a.disambiguation)},")
            sb.appendLine("    \"begin_date\": ${jsonStr(a.beginDate)},")
            sb.appendLine("    \"end_date\": ${jsonStr(a.endDate)}")
            sb.append(if (i < artists.lastIndex) "  },\n" else "  }\n")
        }
        sb.appendLine("  ],")
        sb.appendLine("  \"album_artists\": [")
        for ((i, a) in albumArtists.withIndex()) {
            sb.appendLine("  {")
            sb.appendLine("    \"album_artist_id\": ${a.id},")
            sb.appendLine("    \"name\": ${jsonStr(a.name)},")
            sb.appendLine("    \"album_count\": ${a.albumCount},")
            sb.appendLine("    \"song_count\": ${a.songCount},")
            sb.appendLine("    \"total_duration\": ${a.totalDuration},")
            sb.appendLine("    \"play_count\": ${a.playCount},")
            sb.appendLine("    \"listening_duration\": ${a.listeningDuration},")
            sb.appendLine("    \"overall_score\": ${a.overallScore},")
            sb.appendLine("    \"play_rank\": ${a.playRank},")
            sb.appendLine("    \"duration_rank\": ${a.durationRank},")
            sb.appendLine("    \"overall_rank\": ${a.overallRank},")
            sb.appendLine("    \"about\": null,")
            sb.appendLine("    \"musicbrainz_id\": ${jsonStr(a.musicbrainzId)},")
            sb.appendLine("    \"type\": ${jsonStr(a.type)},")
            sb.appendLine("    \"gender\": ${jsonStr(a.gender)},")
            sb.appendLine("    \"country\": ${jsonStr(a.country)},")
            sb.appendLine("    \"disambiguation\": ${jsonStr(a.disambiguation)},")
            sb.appendLine("    \"begin_date\": ${jsonStr(a.beginDate)},")
            sb.appendLine("    \"end_date\": ${jsonStr(a.endDate)}")
            sb.append(if (i < albumArtists.lastIndex) "  },\n" else "  }\n")
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    private fun appendJsonStats(sb: StringBuilder, st: SongStatsEntity?) {
        if (st == null) {
            sb.appendLine("    \"play_count\": 0,")
            sb.appendLine("    \"skip_count\": 0,")
            sb.appendLine("    \"listening_duration\": 0,")
            sb.appendLine("    \"avg_completion\": 0.0,")
            return
        }
        sb.appendLine("    \"total_play_count\": ${st.totalPlayCount},")
        sb.appendLine("    \"total_skip_count\": ${st.totalSkipCount},")
        sb.appendLine("    \"total_listening_duration\": ${st.totalListeningDuration},")
        sb.appendLine("    \"effective_listening_duration\": ${st.effectiveListeningDuration},")
        sb.appendLine("    \"avg_completion_percentage\": ${st.avgCompletionPercentage},")
        sb.appendLine("    \"last_played_timestamp\": ${st.lastPlayedTimestamp},")
        sb.appendLine("    \"first_played_timestamp\": ${st.firstPlayedTimestamp},")
        sb.appendLine("    \"total_pause_count\": ${st.totalPauseCount},")
        sb.appendLine("    \"total_seek_count\": ${st.totalSeekCount},")
        sb.appendLine("    \"longest_listening_streak\": ${st.longestListeningStreak},")
        sb.appendLine("    \"monthly_play_count\": ${st.monthlyPlayCount},")
        sb.appendLine("    \"weekly_play_count\": ${st.weeklyPlayCount},")
        sb.appendLine("    \"daily_play_count\": ${st.dailyPlayCount},")
    }

    private fun appendJsonRankings(sb: StringBuilder, rank: SongRankingEntity?) {
        if (rank == null) {
            sb.appendLine("    \"overall_rank\": 0,")
            sb.appendLine("    \"overall_score\": 0.0,")
            sb.appendLine("    \"ranking_version\": 0,")
            return
        }
        sb.appendLine("    \"play_rank\": ${rank.playRank},")
        sb.appendLine("    \"duration_rank\": ${rank.durationRank},")
        sb.appendLine("    \"overall_rank\": ${rank.overallRank},")
        sb.appendLine("    \"overall_score\": ${rank.overallScore},")
        sb.appendLine("    \"ranking_last_updated\": ${rank.lastUpdated},")
        sb.appendLine("    \"ranking_version\": ${rank.rankingVersion},")
    }

    private fun appendJsonMetadata(sb: StringBuilder, meta: SongMetadataEntity?) {
        if (meta == null) {
            sb.appendLine("    \"year\": 0,")
            sb.appendLine("    \"track_number\": 0,")
            sb.appendLine("    \"is_favorite\": false,")
            return
        }
        sb.appendLine("    \"composer\": ${jsonStr(meta.composer)},")
        sb.appendLine("    \"lyricist\": ${jsonStr(meta.lyricist)},")
        sb.appendLine("    \"conductor\": ${jsonStr(meta.conductor)},")
        sb.appendLine("    \"publisher\": ${jsonStr(meta.publisher)},")
        sb.appendLine("    \"copyright\": ${jsonStr(meta.copyright)},")
        sb.appendLine("    \"genre_metadata\": ${jsonStr(meta.genre)},")
        sb.appendLine("    \"year\": ${meta.year},")
        sb.appendLine("    \"track_number\": ${meta.trackNumber},")
        sb.appendLine("    \"disc_number\": ${meta.discNumber},")
        sb.appendLine("    \"file_size\": ${meta.fileSize},")
        sb.appendLine("    \"date_added\": ${meta.dateAdded},")
        sb.appendLine("    \"date_modified\": ${meta.dateModified},")
        sb.appendLine("    \"audio_format\": ${jsonStr(meta.audioFormat)},")
        sb.appendLine("    \"sample_rate\": ${meta.sampleRate},")
        sb.appendLine("    \"channel_count\": ${meta.channelCount},")
        sb.appendLine("    \"bit_depth\": ${meta.bitDepth},")
        sb.appendLine("    \"mime_type\": ${jsonStr(meta.mimeType)},")
        sb.appendLine("    \"replay_gain\": ${meta.replayGain},")
        sb.appendLine("    \"bpm\": ${meta.bpm},")
        sb.appendLine("    \"key_signature\": ${jsonStr(meta.keySignature)},")
        sb.appendLine("    \"isrc\": ${jsonStr(meta.isrc)},")
        sb.appendLine("    \"musicbrainz_track_id\": ${jsonStr(meta.musicbrainzTrackId)},")
        sb.appendLine("    \"musicbrainz_album_id\": ${jsonStr(meta.musicbrainzAlbumId)},")
        sb.appendLine("    \"musicbrainz_artist_id\": ${jsonStr(meta.musicbrainzArtistId)},")
        sb.appendLine("    \"musicbrainz_work_id\": ${jsonStr(meta.musicbrainzWorkId)},")
        sb.appendLine("    \"iswc\": ${jsonStr(meta.iswc)},")
        sb.appendLine("    \"label\": ${jsonStr(meta.label)},")
        sb.appendLine("    \"catalog_number\": ${jsonStr(meta.catalogNumber)},")
        sb.appendLine("    \"arranger\": ${jsonStr(meta.arranger)},")
        sb.appendLine("    \"producer\": ${jsonStr(meta.producer)},")
        sb.appendLine("    \"engineer\": ${jsonStr(meta.engineer)},")
        sb.appendLine("    \"language\": ${jsonStr(meta.language)},")
        sb.appendLine("    \"artwork_uri\": ${jsonStr(meta.artworkUri)},")
        sb.appendLine("    \"checksum\": ${jsonStr(meta.checksum)},")
        sb.appendLine("    \"is_explicit\": ${meta.isExplicit},")
        sb.appendLine("    \"is_compilation\": ${meta.isCompilation},")
        sb.appendLine("    \"is_favorite\": ${meta.isFavorite},")
        sb.appendLine("    \"rating\": ${meta.rating},")
    }

    private fun jsonStr(value: String?): String = when {
        value == null -> "null"
        else -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
    }

    companion object {
        const val FILE_PROVIDER_AUTHORITY = ".fileprovider"
    }
}
