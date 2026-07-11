package com.mardous.booming.data.local.repository

import android.content.Context
import android.net.Uri
import com.mardous.booming.data.local.room.ListeningHistoryDao
import com.mardous.booming.data.local.room.ListeningSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.Writer
import kotlin.math.roundToLong

class StatsRepository(
    private val dao: ListeningHistoryDao,
    private val context: Context
) {
    suspend fun insertSession(session: ListeningSessionEntity): Long = dao.insertSession(session)

    suspend fun insertSessions(sessions: List<ListeningSessionEntity>) = dao.insertSessions(sessions)

    suspend fun getSessionsPaged(limit: Int = 50, offset: Int = 0) = dao.getSessionsPaged(limit, offset)

    fun getSessionsPagedFlow(limit: Int = 50, offset: Int = 0): Flow<List<ListeningSessionEntity>> =
        dao.getSessionsPagedFlow(limit, offset)

    suspend fun getSessionsForSong(songId: Long, limit: Int = 100) = dao.getSessionsForSong(songId, limit)

    suspend fun getSessionsInRange(from: Long, to: Long) = dao.getSessionsInRange(from, to)

    fun getAllSessionsFlow() = dao.getAllSessionsFlow()

    suspend fun getTotalSessionCount() = dao.getTotalSessionCount()

    suspend fun getTotalListeningTimeMs() = dao.getTotalListeningTimeMs()

    suspend fun getTotalListeningTimeMsForMonth(yearMonth: String) = dao.getTotalListeningTimeMsForMonth(yearMonth)

    suspend fun getTotalListeningTimeMsInRange(from: Long, to: Long) = dao.getTotalListeningTimeMsInRange(from, to)

    suspend fun getTopSongs(limit: Int = 50) = dao.getTopSongs(limit)

    suspend fun getTopArtists(limit: Int = 50) = dao.getTopArtists(limit)

    suspend fun getTopAlbums(limit: Int = 50) = dao.getTopAlbums(limit)

    suspend fun getMonthlyStats(limit: Int = 12) = dao.getMonthlyStats(limit)

    suspend fun getYearlyStats(limit: Int = 10) = dao.getYearlyStats(limit)

    suspend fun getTopGenres(limit: Int = 20) = dao.getTopGenres(limit)

    suspend fun getActiveMonthsCount() = dao.getActiveMonthsCount()

    suspend fun getFirstListeningDate() = dao.getFirstListeningDate()

    suspend fun getUniqueSongsCount() = dao.getUniqueSongsCount()

    suspend fun getUniqueArtistsCount() = dao.getUniqueArtistsCount()

    suspend fun getUniqueAlbumsCount() = dao.getUniqueAlbumsCount()

    suspend fun getLongestListeningSessionMs() = dao.getLongestListeningSessionMs()

    suspend fun getCompletedCount() = dao.getCompletedCount()

    suspend fun getSkippedCount() = dao.getSkippedCount()

    suspend fun getTotalPauses() = dao.getTotalPauses()

    suspend fun getTotalSeeks() = dao.getTotalSeeks()

    suspend fun clearAll() = dao.clearAll()

    suspend fun getAllSessionsAsc() = dao.getAllSessionsAsc()

    suspend fun getSessionsForMonthRange(fromMonth: String, toMonth: String) =
        dao.getSessionsForMonthRange(fromMonth, toMonth)

    suspend fun getSummary(): StatsSummary {
        val totalSessions = dao.getTotalSessionCount()
        val totalListeningMs = dao.getTotalListeningTimeMs()
        val uniqueSongs = dao.getUniqueSongsCount()
        val uniqueArtists = dao.getUniqueArtistsCount()
        val uniqueAlbums = dao.getUniqueAlbumsCount()
        val activeMonths = dao.getActiveMonthsCount()
        val completed = dao.getCompletedCount()
        val skipped = dao.getSkippedCount()
        val totalPauses = dao.getTotalPauses()
        val totalSeeks = dao.getTotalSeeks()
        val longestSessionMs = dao.getLongestListeningSessionMs()
        val firstDate = dao.getFirstListeningDate()

        return StatsSummary(
            totalSessions = totalSessions,
            totalListeningMs = totalListeningMs,
            uniqueSongs = uniqueSongs,
            uniqueArtists = uniqueArtists,
            uniqueAlbums = uniqueAlbums,
            activeMonths = activeMonths,
            completedCount = completed,
            skippedCount = skipped,
            totalPauses = totalPauses,
            totalSeeks = totalSeeks,
            longestSessionMs = longestSessionMs ?: 0L,
            firstListeningDate = firstDate ?: "N/A"
        )
    }

    suspend fun exportToCsv(
        uri: Uri,
        progressCallback: ((Int, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val allSessions = dao.getAllSessionsAsc()
        val total = allSessions.size
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                writer.write(CSV_HEADER)
                writer.newLine()
                allSessions.forEachIndexed { index, session ->
                    writer.write(session.toCsvLine())
                    writer.newLine()
                    if (index % 1000 == 0) {
                        progressCallback?.invoke(index + 1, total)
                    }
                }
            }
        }
        Unit
    }

    suspend fun exportToJson(
        uri: Uri,
        prettyPrint: Boolean = true,
        progressCallback: ((Int, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val allSessions = dao.getAllSessionsAsc()
        val total = allSessions.size
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                writer.write("[")
                writer.newLine()
                allSessions.forEachIndexed { index, session ->
                    writer.write(session.toJsonLine(prettyPrint))
                    if (index < total - 1) {
                        writer.write(",")
                    }
                    writer.newLine()
                    if (index % 500 == 0) {
                        progressCallback?.invoke(index + 1, total)
                    }
                }
                writer.write("]")
            }
        }
        Unit
    }

    suspend fun exportToCsvStreaming(
        uri: Uri,
        chunkSize: Int = 5000,
        progressCallback: ((Int, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val total = dao.getTotalSessionCount().toInt()
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                writer.write(CSV_HEADER)
                writer.newLine()
                var offset = 0
                var written = 0
                while (offset < total) {
                    val chunk = dao.getSessionsPaged(chunkSize, offset)
                    if (chunk.isEmpty()) break
                    chunk.forEach { session ->
                        writer.write(session.toCsvLine())
                        writer.newLine()
                    }
                    written += chunk.size
                    offset += chunkSize
                    progressCallback?.invoke(written, total)
                }
            }
        }
        Unit
    }

    suspend fun exportToJsonStreaming(
        uri: Uri,
        chunkSize: Int = 2000,
        prettyPrint: Boolean = true,
        progressCallback: ((Int, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val total = dao.getTotalSessionCount().toInt()
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                writer.write("[")
                writer.newLine()
                var offset = 0
                var isFirst = true
                while (offset < total) {
                    val chunk = dao.getSessionsPaged(chunkSize, offset)
                    if (chunk.isEmpty()) break
                    chunk.forEach { session ->
                        if (!isFirst) {
                            writer.write(",")
                            writer.newLine()
                        }
                        writer.write(session.toJsonLine(prettyPrint))
                        isFirst = false
                    }
                    offset += chunkSize
                    progressCallback?.invoke(offset, total)
                }
                writer.newLine()
                writer.write("]")
            }
        }
        Unit
    }

    suspend fun getAggregatedStats(): AggregatedStats = withContext(Dispatchers.IO) {
        val topSongs = dao.getTopSongs(50)
        val topArtists = dao.getTopArtists(50)
        val topAlbums = dao.getTopAlbums(50)
        val monthlyStats = dao.getMonthlyStats(12)
        val topGenres = dao.getTopGenres(20)
        val summary = getSummary()

        AggregatedStats(
            summary = summary,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            monthlyStats = monthlyStats,
            topGenres = topGenres
        )
    }

    companion object {
        private const val CSV_HEADER = "sessionId,songId,songTitle,artistName,albumArtist,albumName,albumId,composer,lyricist,publisher,genre,year,yearMonth,startTime,endTime,songDurationMs,durationPlayedMs,completed,skipped,pauseCount,seekCount,listenedSecondsMs,endReason"

        private fun ListeningSessionEntity.toCsvLine(): String {
            return buildString {
                append(sessionId); append(',')
                append(songId); append(',')
                append(escapeCsv(songTitle)); append(',')
                append(escapeCsv(artistName)); append(',')
                append(escapeCsv(albumArtist ?: "")); append(',')
                append(escapeCsv(albumName)); append(',')
                append(albumId); append(',')
                append(escapeCsv(composer ?: "")); append(',')
                append(escapeCsv(lyricist ?: "")); append(',')
                append(escapeCsv(publisher ?: "")); append(',')
                append(escapeCsv(genre ?: "")); append(',')
                append(year); append(',')
                append(yearMonth); append(',')
                append(startTime); append(',')
                append(endTime); append(',')
                append(songDurationMs); append(',')
                append(durationPlayedMs); append(',')
                append(if (completed) "1" else "0"); append(',')
                append(if (skipped) "1" else "0"); append(',')
                append(pauseCount); append(',')
                append(seekCount); append(',')
                append(listenedSecondsMs); append(',')
                append(endReason)
            }
        }

        private fun escapeCsv(value: String): String {
            return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }

        private fun ListeningSessionEntity.toJsonLine(pretty: Boolean): String {
            val indent = if (pretty) "  " else ""
            val nl = if (pretty) "\n" else ""
            val sep = if (pretty) " " else ""
            return buildString {
                append("$indent{$nl")
                append("$indent$sep\"sessionId\": $sessionId,\"$nl")
                append("$indent$sep\"songId\": $songId,\"$nl")
                append("$indent$sep\"songTitle\": ${jsonStr(songTitle)},\"$nl")
                append("$indent$sep\"artistName\": ${jsonStr(artistName)},\"$nl")
                append("$indent$sep\"albumArtist\": ${jsonStr(albumArtist ?: "")},\"$nl")
                append("$indent$sep\"albumName\": ${jsonStr(albumName)},\"$nl")
                append("$indent$sep\"albumId\": $albumId,\"$nl")
                append("$indent$sep\"composer\": ${jsonStr(composer ?: "")},\"$nl")
                append("$indent$sep\"lyricist\": ${jsonStr(lyricist ?: "")},\"$nl")
                append("$indent$sep\"publisher\": ${jsonStr(publisher ?: "")},\"$nl")
                append("$indent$sep\"genre\": ${jsonStr(genre ?: "")},\"$nl")
                append("$indent$sep\"year\": $year,\"$nl")
                append("$indent$sep\"yearMonth\": ${jsonStr(yearMonth)},\"$nl")
                append("$indent$sep\"startTime\": $startTime,\"$nl")
                append("$indent$sep\"endTime\": $endTime,\"$nl")
                append("$indent$sep\"songDurationMs\": $songDurationMs,\"$nl")
                append("$indent$sep\"durationPlayedMs\": $durationPlayedMs,\"$nl")
                append("$indent$sep\"completed\": $completed,\"$nl")
                append("$indent$sep\"skipped\": $skipped,\"$nl")
                append("$indent$sep\"pauseCount\": $pauseCount,\"$nl")
                append("$indent$sep\"seekCount\": $seekCount,\"$nl")
                append("$indent$sep\"listenedSecondsMs\": $listenedSecondsMs,\"$nl")
                append("$indent$sep\"endReason\": ${jsonStr(endReason)}\"$nl")
                append("$indent}")
            }
        }

        private fun jsonStr(value: String): String {
            return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
        }
    }
}

data class StatsSummary(
    val totalSessions: Long,
    val totalListeningMs: Long,
    val uniqueSongs: Long,
    val uniqueArtists: Long,
    val uniqueAlbums: Long,
    val activeMonths: Int,
    val completedCount: Long,
    val skippedCount: Long,
    val totalPauses: Long,
    val totalSeeks: Long,
    val longestSessionMs: Long,
    val firstListeningDate: String
)

data class AggregatedStats(
    val summary: StatsSummary,
    val topSongs: List<ListeningHistoryDao.SongStats>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val monthlyStats: List<ListeningHistoryDao.PeriodStats>,
    val topGenres: List<ListeningHistoryDao.GenreStats>
)
