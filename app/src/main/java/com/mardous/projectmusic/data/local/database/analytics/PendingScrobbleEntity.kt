package com.mardous.projectmusic.data.local.database.analytics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_scrobbles")
data class PendingScrobbleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "track") val track: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
