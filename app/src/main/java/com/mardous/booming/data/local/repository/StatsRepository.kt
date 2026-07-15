package com.mardous.booming.data.local.repository

import android.content.Context
import android.net.Uri
import com.mardous.booming.data.local.room.ListeningHistoryDao
import com.mardous.booming.data.local.room.ListeningSessionEntity
import com.mardous.booming.data.local.room.ListeningSessionGroupDao
import com.mardous.booming.data.local.room.ListeningSessionGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatsRepository(
    private val dao: ListeningHistoryDao,
    private val groupDao: ListeningSessionGroupDao,
    private val context: Context
) {
    suspend fun insertSession(session: ListeningSessionEntity): Long = dao.insertSession(session)

    suspend fun deleteSession(sessionId: Long) = dao.deleteSessionById(sessionId)

    suspend fun insertSessions(sessions: List<ListeningSessionEntity>) = dao.insertSessions(sessions)

    suspend fun insertGroup(group: ListeningSessionGroupEntity) = groupDao.insertGroup(group)

    suspend fun insertGroups(groups: List<ListeningSessionGroupEntity>) = groupDao.insertGroups(groups)

    suspend fun getSessionsPaged(limit: Int = 50, offset: Int = 0) = dao.getSessionsPaged(limit, offset)

    suspend fun getTotalListeningTimeForSong(songId: Long): Long {
        return dao.getTotalListeningTimeForSong(songId) ?: 0L
    }

    fun getSessionsPagedFlow(limit: Int = 50, offset: Int = 0): Flow<List<ListeningSessionEntity>> =
        dao.getSessionsPagedFlow(limit, offset)

    suspend fun getSessionsForSong(songId: Long, limit: Int = 100) = dao.getSessionsForSong(songId, limit)

    suspend fun getSessionsInRange(from: Long, to: Long) = dao.getSessionsInRange(from, to)

    fun getAllSessionsFlow() = dao.getAllSessionsFlow()

    suspend fun getTotalSessionCount() = dao.getTotalSessionCount()

    suspend fun getTotalListeningTimeMs() = dao.getTotalListeningTimeMs() ?: 0L

    suspend fun getSummary(): StatsSummary {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        val monthEnd = System.currentTimeMillis()

        val totalSessions = dao.getTotalSessionCount()
        val totalListeningMs = dao.getTotalListeningTimeMs() ?: 0L
        val uniqueSongs = dao.getUniqueSongsCount()
        val uniqueArtists = dao.getUniqueArtistsCount()
        val uniqueAlbums = dao.getUniqueAlbumsCount()
        val activeMonths = dao.getActiveMonthsCount()
        val completed = dao.getCompletedCount()
        val skipped = dao.getSkippedCount()
        val totalPauses = dao.getTotalPauses() ?: 0L
        val totalSeeks = dao.getTotalSeeks() ?: 0L
        val longestSessionMs = dao.getLongestListeningSessionMs() ?: 0L
        val firstDate = dao.getFirstListeningDate()
        val totalGroups = groupDao.totalSessionGroups()
        val avgSessionDuration = groupDao.avgSessionDurationMs() ?: 0L
        val avgCompletion = dao.getAvgCompletionPercent() ?: 0.0

        // Extra stats for Spotify Home
        val dailyActivity = dao.getDailyActivity(monthStart, monthEnd)
        val streak = computeStreak(dailyActivity).currentStreak

        val monthSessions = dao.getSessionsInRange(monthStart, monthEnd)
        val monthTotalCount = monthSessions.size.coerceAtLeast(1)

        val shuffleCount = dao.getShuffleEnabledCount(monthStart, monthEnd) ?: 0L
        val shuffleRate = shuffleCount.toDouble() / monthTotalCount

        val nightSessions = monthSessions.count { it.hour in 0..5 || it.hour in 22..23 }
        val nightOwlRate = nightSessions.toDouble() / monthTotalCount

        val discoveryCount = dao.getNewDiscoveries(monthStart, monthEnd).size
        val discoveryRate = discoveryCount.toDouble() / monthTotalCount
        val repeatRate = 1.0 - discoveryRate

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
            longestSessionMs = longestSessionMs,
            firstListeningDate = firstDate ?: "N/A",
            totalSessionGroups = totalGroups,
            avgSessionDurationMs = avgSessionDuration,
            avgCompletionPercent = avgCompletion,
            currentStreak = streak,
            repeatRate = repeatRate,
            discoveryRate = discoveryRate,
            shuffleRate = shuffleRate,
            nightOwlRate = nightOwlRate
        )
    }

    suspend fun getTopSongs(limit: Int = 50) = dao.getTopSongs(limit)

    suspend fun getTopArtists(limit: Int = 50) = dao.getTopArtists(limit)

    suspend fun getTopAlbums(limit: Int = 50) = dao.getTopAlbums(limit)

    suspend fun getMonthlyStats(limit: Int = 12) = dao.getMonthlyStats(limit)

    suspend fun getYearlyStats(limit: Int = 10) = dao.getYearlyStats(limit)

    suspend fun getTopGenres(limit: Int = 20) = dao.getTopGenres(limit)

    suspend fun clearAll() {
        dao.clearAll()
        groupDao.clearAll()
    }

    suspend fun getAllSessionsAsc() = dao.getAllSessionsAsc()

    suspend fun getTopSongsInRange(from: Long, to: Long, limit: Int = 50) =
        dao.getTopSongsInRange(from, to, limit)

    suspend fun getTopArtistsInRange(from: Long, to: Long, limit: Int = 50) =
        dao.getTopArtistsInRange(from, to, limit)

    suspend fun getTopAlbumsInRange(from: Long, to: Long, limit: Int = 50) =
        dao.getTopAlbumsInRange(from, to, limit)

    suspend fun getTopGenresInRange(from: Long, to: Long, limit: Int = 50) =
        dao.getTopGenresInRange(from, to, limit)

    suspend fun getSummaryInRange(from: Long, to: Long) = dao.getSummaryInRange(from, to)

    suspend fun getDailyActivity(from: Long, to: Long) = dao.getDailyActivity(from, to)

    suspend fun getHourlyDistribution(from: Long, to: Long) = dao.getHourlyDistribution(from, to)

    suspend fun getDayOfWeekDistribution(from: Long, to: Long) = dao.getDayOfWeekDistribution(from, to)

    suspend fun getEndReasonDistribution(from: Long, to: Long) = dao.getEndReasonDistribution(from, to)

    suspend fun getQueueSourceDistribution(from: Long, to: Long) = dao.getQueueSourceDistribution(from, to)

    suspend fun getPlaybackOriginDistribution(from: Long, to: Long) = dao.getPlaybackOriginDistribution(from, to)

    suspend fun getAudioFormatDistribution(from: Long, to: Long) = dao.getAudioFormatDistribution(from, to)

    suspend fun getDeviceDistribution(from: Long, to: Long) = dao.getDeviceDistribution(from, to)

    suspend fun getRepeatModeDistribution(from: Long, to: Long) = dao.getRepeatModeDistribution(from, to)

    suspend fun getAvgPauseCount(from: Long, to: Long) = dao.getAvgPauseCount(from, to) ?: 0.0

    suspend fun getAvgSeekCount(from: Long, to: Long) = dao.getAvgSeekCount(from, to) ?: 0.0

    suspend fun getShuffleEnabledCount(from: Long, to: Long) = dao.getShuffleEnabledCount(from, to) ?: 0L

    suspend fun getFavoriteCount(from: Long, to: Long) = dao.getFavoriteCount(from, to) ?: 0L

    suspend fun getWeekendCount(from: Long, to: Long) = dao.getWeekendCount(from, to) ?: 0L

    suspend fun getWeekdayCount(from: Long, to: Long) = dao.getWeekdayCount(from, to) ?: 0L

    suspend fun getScreenOnCount(from: Long, to: Long) = dao.getScreenOnCount(from, to) ?: 0L

    suspend fun getChargingCount(from: Long, to: Long) = dao.getChargingCount(from, to) ?: 0L

    suspend fun getAvgVolumeStart(from: Long, to: Long) = dao.getAvgVolumeStart(from, to) ?: 0.0

    suspend fun getMonthlyTrends(from: Long, to: Long) = dao.getMonthlyTrends(from, to)

    suspend fun getTimezonesInRange(from: Long, to: Long) = dao.getTimezonesInRange(from, to)

    suspend fun getSessionGroupStats() = SessionGroupStats(
        totalGroups = groupDao.totalSessionGroups(),
        avgSongsPerSession = groupDao.avgSongsPerSession() ?: 0f,
        avgSessionDurationMs = groupDao.avgSessionDurationMs() ?: 0L,
        maxSongsInSession = groupDao.maxSongsInSession() ?: 0,
        maxSessionDurationMs = groupDao.maxSessionDurationMs() ?: 0L,
        singleSongSessions = groupDao.singleSongSessionCount(),
        longSessions = groupDao.longSessionCount()
    )

    suspend fun getPeriodStats(from: Long, to: Long): PeriodStatsResult = withContext(Dispatchers.IO) {
        val summary = dao.getSummaryInRange(from, to)
        val topSongs = dao.getTopSongsInRange(from, to, 10)
        val topArtists = dao.getTopArtistsInRange(from, to, 10)
        val topAlbums = dao.getTopAlbumsInRange(from, to, 10)
        val topGenres = dao.getTopGenresInRange(from, to, 10)
        val dailyActivity = dao.getDailyActivity(from, to)
        val hourlyDistribution = dao.getHourlyDistribution(from, to)
        val dayOfWeekDistribution = dao.getDayOfWeekDistribution(from, to)
        val endReasons = dao.getEndReasonDistribution(from, to)
        val deviceDistribution = dao.getDeviceDistribution(from, to)
        val audioFormats = dao.getAudioFormatDistribution(from, to)
        val queueSources = dao.getQueueSourceDistribution(from, to)
        val playbackOrigins = dao.getPlaybackOriginDistribution(from, to)
        val repeatModes = dao.getRepeatModeDistribution(from, to)

        val completionRate = if (summary.totalSessions > 0) {
            summary.completedCount.toFloat() / summary.totalSessions.toFloat()
        } else 0f

        val avgSessionMs = if (summary.totalSessions > 0) {
            summary.totalListeningMs / summary.totalSessions
        } else 0L

        val totalHours = TimeUnit.MILLISECONDS.toHours(summary.totalListeningMs)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(summary.totalListeningMs) % 60

        val streak = computeStreak(dailyActivity)

        val peakHour = hourlyDistribution.maxByOrNull { it.playCount }?.hourOfDay

        val insights = buildInsights(
            summary = summary,
            completionRate = completionRate,
            totalHours = totalHours,
            totalMinutes = totalMinutes,
            topGenre = topGenres.firstOrNull()?.genre,
            topArtist = topArtists.firstOrNull()?.artistName,
            topSong = topSongs.firstOrNull()?.songTitle,
            peakHour = peakHour,
            streak = streak
        )

        PeriodStatsResult(
            totalSessions = summary.totalSessions,
            totalListeningMs = summary.totalListeningMs,
            uniqueSongs = summary.uniqueSongs,
            uniqueArtists = summary.uniqueArtists,
            uniqueAlbums = summary.uniqueAlbums,
            completionRate = completionRate,
            skippedCount = summary.skippedCount,
            totalPauses = summary.totalPauses,
            totalSeeks = summary.totalSeeks,
            avgSessionMs = avgSessionMs,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            dailyActivity = dailyActivity,
            hourlyDistribution = hourlyDistribution,
            dayOfWeekDistribution = dayOfWeekDistribution,
            endReasons = endReasons,
            deviceDistribution = deviceDistribution,
            audioFormats = audioFormats,
            queueSources = queueSources,
            playbackOrigins = playbackOrigins,
            repeatModes = repeatModes,
            currentStreak = streak.currentStreak,
            longestStreak = streak.longestStreak,
            insights = insights
        )
    }

    private fun computeStreak(dailyActivity: List<ListeningHistoryDao.DailyActivity>): StreakResult {
        if (dailyActivity.isEmpty()) return StreakResult(0, 0)

        val daySet = dailyActivity.map { it.dayEpoch }.toSortedSet()
        val days = daySet.toList()

        var longestStreak = 0
        var tempStreak = 1

        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        val todayEpoch = (System.currentTimeMillis() / 86400000).toInt()
        val lastDay = days.last()
        val currentStreak = if (todayEpoch - lastDay <= 1) {
            var count = 1
            for (i in days.size - 2 downTo 0) {
                if (days[i + 1] == days[i] + 1) count++ else break
            }
            count
        } else {
            0
        }

        return StreakResult(currentStreak, longestStreak)
    }

    private fun buildInsights(
        summary: ListeningHistoryDao.RangeSummaryResult,
        completionRate: Float,
        totalHours: Long,
        totalMinutes: Long,
        topGenre: String?,
        topArtist: String?,
        topSong: String?,
        peakHour: Int?,
        streak: StreakResult
    ): List<InsightCard> {
        val insights = mutableListOf<InsightCard>()

        if (totalHours > 0 || totalMinutes > 0) {
            insights.add(InsightCard(InsightIcon.LISTENING_TIME, "You listened for $totalHours hours and $totalMinutes minutes"))
        }
        if (summary.totalSessions > 0) {
            insights.add(InsightCard(InsightIcon.PLAYS, "You played ${summary.totalSessions} songs"))
        }
        if (summary.uniqueSongs > 0) {
            insights.add(InsightCard(InsightIcon.UNIQUE_SONGS, "You explored ${summary.uniqueSongs} unique songs"))
        }
        if (topArtist != null) {
            insights.add(InsightCard(InsightIcon.ARTIST, "Your top artist was $topArtist"))
        }
        if (topSong != null) {
            insights.add(InsightCard(InsightIcon.SONG, "Your most played song was $topSong"))
        }
        if (topGenre != null) {
            insights.add(InsightCard(InsightIcon.GENRE, "Your favorite genre was $topGenre"))
        }
        if (peakHour != null) {
            val label = when (peakHour) {
                in 5..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..20 -> "evening"
                else -> "night"
            }
            insights.add(InsightCard(InsightIcon.TIME, "You listen most in the $label"))
        }
        if (streak.currentStreak > 1) {
            insights.add(InsightCard(InsightIcon.STREAK, "You're on a ${streak.currentStreak}-day listening streak"))
        }
        if (streak.longestStreak > streak.currentStreak && streak.longestStreak > 1) {
            insights.add(InsightCard(InsightIcon.STREAK, "Your longest streak was ${streak.longestStreak} days"))
        }
        if (summary.skippedCount > 0) {
            val skipRate = summary.skippedCount.toFloat() / summary.totalSessions.coerceAtLeast(1)
            insights.add(InsightCard(InsightIcon.SKIP, "You skipped ${summary.skippedCount} songs (${(skipRate * 100).toInt()}%)"))
        }

        return insights
    }

    // --- Export ---

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

    companion object {
        private const val CSV_HEADER = "session_id,session_group_id,song_id,artist_id,song_title,artist_name,album_artist,album_id,album_name,genre,release_year,composer,lyricist,publisher,is_lyrics,song_duration_ms,audio_format,audio_sample_rate,audio_channel_count,bitrate_kbps,playback_speed,equalizer_active,start_time,end_time,time_standard,timezone_id,timezone_offset_minutes,start_date,start_time_only,day_of_week,day_of_month,day_of_year,week_of_year,month,month_name,quarter,year,year_month,year_week,hour,minute,second,time_period,is_weekend,playback_duration_ms,effective_listened_ms,completion_percent,end_reason,pause_count,pause_duration_ms,seek_count,seek_forward_count,seek_backward_count,shuffle_enabled,repeat_mode,queue_position,queue_source,playback_origin,playlist_id,playlist_name,is_favorite,output_device,volume_start,volume_end,battery_level,charging,screen_on,app_version"

        private fun ListeningSessionEntity.toCsvLine(): String {
            return buildString {
                append(sessionId); append(',')
                append(sessionGroupId); append(',')
                append(songId); append(',')
                append(artistId); append(',')
                append(escapeCsv(songTitle)); append(',')
                append(escapeCsv(artistName)); append(',')
                append(escapeCsv(albumArtist ?: "")); append(',')
                append(albumId); append(',')
                append(escapeCsv(albumName)); append(',')
                append(escapeCsv(genre ?: "")); append(',')
                append(releaseYear); append(',')
                append(escapeCsv(composer ?: "")); append(',')
                append(escapeCsv(lyricist ?: "")); append(',')
                append(escapeCsv(publisher ?: "")); append(',')
                append(if (isLyrics) 1 else 0); append(',')
                append(songDurationMs); append(',')
                append(escapeCsv(audioFormat)); append(',')
                append(audioSampleRate); append(',')
                append(audioChannelCount); append(',')
                append(bitrateKbps); append(',')
                append(playbackSpeed); append(',')
                append(if (equalizerActive) "1" else "0"); append(',')
                append(startTime); append(',')
                append(endTime); append(',')
                append(timeStandard); append(',')
                append(escapeCsv(timezoneId)); append(',')
                append(timezoneOffsetMinutes); append(',')
                append(startDate); append(',')
                append(startTimeOnly); append(',')
                append(dayOfWeek); append(',')
                append(dayOfMonth); append(',')
                append(dayOfYear); append(',')
                append(weekOfYear); append(',')
                append(month); append(',')
                append(monthName); append(',')
                append(quarter); append(',')
                append(year); append(',')
                append(yearMonth); append(',')
                append(yearWeek); append(',')
                append(hour); append(',')
                append(minute); append(',')
                append(second); append(',')
                append(timePeriod); append(',')
                append(if (isWeekend) "1" else "0"); append(',')
                append(playbackDurationMs); append(',')
                append(effectiveListenedMs); append(',')
                append(completionPercent); append(',')
                append(endReason); append(',')
                append(pauseCount); append(',')
                append(pauseDurationMs); append(',')
                append(seekCount); append(',')
                append(seekForwardCount); append(',')
                append(seekBackwardCount); append(',')
                append(if (shuffleEnabled) "1" else "0"); append(',')
                append(repeatMode); append(',')
                append(queuePosition); append(',')
                append(queueSource); append(',')
                append(playbackOrigin); append(',')
                append(playlistId); append(',')
                append(playlistName); append(',')
                append(if (isFavorite) "1" else "0"); append(',')
                append(outputDevice); append(',')
                append(volumeStart); append(',')
                append(volumeEnd); append(',')
                append(batteryLevel); append(',')
                append(if (charging) "1" else "0"); append(',')
                append(if (screenOn) "1" else "0"); append(',')
                append(appVersion)
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
                append("$indent$sep\"sessionId\": $sessionId,$nl")
                append("$indent$sep\"sessionGroupId\": ${jsonStr(sessionGroupId)},$nl")
                append("$indent$sep\"songId\": $songId,$nl")
                append("$indent$sep\"artistId\": $artistId,$nl")
                append("$indent$sep\"songTitle\": ${jsonStr(songTitle)},$nl")
                append("$indent$sep\"artistName\": ${jsonStr(artistName)},$nl")
                append("$indent$sep\"albumArtist\": ${jsonStr(albumArtist ?: "")},$nl")
                append("$indent$sep\"albumId\": $albumId,$nl")
                append("$indent$sep\"albumName\": ${jsonStr(albumName)},$nl")
                append("$indent$sep\"genre\": ${jsonStr(genre ?: "")},$nl")
                append("$indent$sep\"releaseYear\": $releaseYear,$nl")
                append("$indent$sep\"composer\": ${jsonStr(composer ?: "")},$nl")
                append("$indent$sep\"lyricist\": ${jsonStr(lyricist ?: "")},$nl")
                append("$indent$sep\"publisher\": ${jsonStr(publisher ?: "")},$nl")
                append("$indent$sep\"isLyrics\": $isLyrics,$nl")
                append("$indent$sep\"songDurationMs\": $songDurationMs,$nl")
                append("$indent$sep\"audioFormat\": ${jsonStr(audioFormat)},$nl")
                append("$indent$sep\"audioSampleRate\": $audioSampleRate,$nl")
                append("$indent$sep\"audioChannelCount\": $audioChannelCount,$nl")
                append("$indent$sep\"bitrateKbps\": $bitrateKbps,$nl")
                append("$indent$sep\"playbackSpeed\": $playbackSpeed,$nl")
                append("$indent$sep\"equalizerActive\": $equalizerActive,$nl")
                append("$indent$sep\"startTime\": $startTime,$nl")
                append("$indent$sep\"endTime\": $endTime,$nl")
                append("$indent$sep\"timeStandard\": ${jsonStr(timeStandard)},$nl")
                append("$indent$sep\"timezoneId\": ${jsonStr(timezoneId)},$nl")
                append("$indent$sep\"timezoneOffsetMinutes\": $timezoneOffsetMinutes,$nl")
                append("$indent$sep\"startDate\": ${jsonStr(startDate)},$nl")
                append("$indent$sep\"startTimeOnly\": ${jsonStr(startTimeOnly)},$nl")
                append("$indent$sep\"dayOfWeek\": ${jsonStr(dayOfWeek)},$nl")
                append("$indent$sep\"dayOfMonth\": $dayOfMonth,$nl")
                append("$indent$sep\"dayOfYear\": $dayOfYear,$nl")
                append("$indent$sep\"weekOfYear\": $weekOfYear,$nl")
                append("$indent$sep\"month\": $month,$nl")
                append("$indent$sep\"monthName\": ${jsonStr(monthName)},$nl")
                append("$indent$sep\"quarter\": $quarter,$nl")
                append("$indent$sep\"year\": $year,$nl")
                append("$indent$sep\"yearMonth\": ${jsonStr(yearMonth)},$nl")
                append("$indent$sep\"yearWeek\": ${jsonStr(yearWeek)},$nl")
                append("$indent$sep\"hour\": $hour,$nl")
                append("$indent$sep\"minute\": $minute,$nl")
                append("$indent$sep\"second\": $second,$nl")
                append("$indent$sep\"timePeriod\": ${jsonStr(timePeriod)},$nl")
                append("$indent$sep\"isWeekend\": $isWeekend,$nl")
                append("$indent$sep\"playbackDurationMs\": $playbackDurationMs,$nl")
                append("$indent$sep\"effectiveListenedMs\": $effectiveListenedMs,$nl")
                append("$indent$sep\"completionPercent\": $completionPercent,$nl")
                append("$indent$sep\"endReason\": ${jsonStr(endReason)},$nl")
                append("$indent$sep\"pauseCount\": $pauseCount,$nl")
                append("$indent$sep\"pauseDurationMs\": $pauseDurationMs,$nl")
                append("$indent$sep\"seekCount\": $seekCount,$nl")
                append("$indent$sep\"seekForwardCount\": $seekForwardCount,$nl")
                append("$indent$sep\"seekBackwardCount\": $seekBackwardCount,$nl")
                append("$indent$sep\"shuffleEnabled\": $shuffleEnabled,$nl")
                append("$indent$sep\"repeatMode\": ${jsonStr(repeatMode)},$nl")
                append("$indent$sep\"queuePosition\": $queuePosition,$nl")
                append("$indent$sep\"queueSource\": ${jsonStr(queueSource)},$nl")
                append("$indent$sep\"playbackOrigin\": ${jsonStr(playbackOrigin)},$nl")
                append("$indent$sep\"playlistId\": ${jsonStr(playlistId)},$nl")
                append("$indent$sep\"playlistName\": ${jsonStr(playlistName)},$nl")
                append("$indent$sep\"isFavorite\": $isFavorite,$nl")
                append("$indent$sep\"outputDevice\": ${jsonStr(outputDevice)},$nl")
                append("$indent$sep\"volumeStart\": $volumeStart,$nl")
                append("$indent$sep\"volumeEnd\": $volumeEnd,$nl")
                append("$indent$sep\"batteryLevel\": $batteryLevel,$nl")
                append("$indent$sep\"charging\": $charging,$nl")
                append("$indent$sep\"screenOn\": $screenOn,$nl")
                append("$indent$sep\"appVersion\": ${jsonStr(appVersion)}$nl")
                append("$indent}$nl")
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
    val firstListeningDate: String,
    val totalSessionGroups: Long,
    val avgSessionDurationMs: Long,
    val avgCompletionPercent: Double,
    val currentStreak: Int = 0,
    val repeatRate: Double = 0.0,
    val discoveryRate: Double = 0.0,
    val shuffleRate: Double = 0.0,
    val nightOwlRate: Double = 0.0
)

data class SessionGroupStats(
    val totalGroups: Long,
    val avgSongsPerSession: Float,
    val avgSessionDurationMs: Long,
    val maxSongsInSession: Int,
    val maxSessionDurationMs: Long,
    val singleSongSessions: Long,
    val longSessions: Long
)

data class AggregatedStats(
    val summary: StatsSummary,
    val topSongs: List<ListeningHistoryDao.SongStats>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val monthlyStats: List<ListeningHistoryDao.PeriodStats>,
    val topGenres: List<ListeningHistoryDao.GenreStats>
)

data class PeriodStatsResult(
    val totalSessions: Long,
    val totalListeningMs: Long,
    val uniqueSongs: Long,
    val uniqueArtists: Long,
    val uniqueAlbums: Long,
    val completionRate: Float,
    val skippedCount: Long,
    val totalPauses: Long,
    val totalSeeks: Long,
    val avgSessionMs: Long,
    val topSongs: List<ListeningHistoryDao.SongStats>,
    val topArtists: List<ListeningHistoryDao.ArtistStats>,
    val topAlbums: List<ListeningHistoryDao.AlbumStats>,
    val topGenres: List<ListeningHistoryDao.GenreStats>,
    val dailyActivity: List<ListeningHistoryDao.DailyActivity>,
    val hourlyDistribution: List<ListeningHistoryDao.HourlyCount>,
    val dayOfWeekDistribution: List<ListeningHistoryDao.DayOfWeekCount>,
    val endReasons: List<ListeningHistoryDao.EndReasonStats>,
    val deviceDistribution: List<ListeningHistoryDao.DeviceStats>,
    val audioFormats: List<ListeningHistoryDao.AudioFormatStats>,
    val queueSources: List<ListeningHistoryDao.QueueSourceStats>,
    val playbackOrigins: List<ListeningHistoryDao.PlaybackOriginStats>,
    val repeatModes: List<ListeningHistoryDao.RepeatModeStats>,
    val currentStreak: Int,
    val longestStreak: Int,
    val insights: List<InsightCard>
)

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)

data class InsightCard(
    val icon: InsightIcon,
    val title: String
)

enum class InsightIcon {
    LISTENING_TIME, PLAYS, UNIQUE_SONGS, ARTIST, SONG, GENRE, TIME, STREAK, SKIP
}
