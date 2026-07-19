package com.mardous.projectmusic.data.local.database.analytics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_session_groups",
    indices = [
        Index(value = ["start_time"], name = "idx_group_start_time")
    ]
)
data class ListeningSessionGroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "total_songs")
    val totalSongs: Int,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long
)
