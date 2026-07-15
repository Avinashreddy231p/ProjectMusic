package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PendingScrobbleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "track") val track: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
