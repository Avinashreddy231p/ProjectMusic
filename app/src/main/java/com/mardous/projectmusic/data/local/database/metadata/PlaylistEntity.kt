/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.mardous.projectmusic.data.local.database.metadata

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "playlists")
@Parcelize
@Serializable
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "playlist_id")
    val playListId: Long = 0,
    @ColumnInfo(name = "playlist_name")
    val playlistName: String,
    @ColumnInfo(name = "custom_cover_uri")
    val customCoverUri: String? = null,
    val description: String? = null,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "total_duration") val totalDuration: Long = 0,
    @ColumnInfo(name = "play_count") val playCount: Long = 0,
    @ColumnInfo(name = "listening_duration") val listeningDuration: Long = 0,
    @ColumnInfo(name = "overall_score") val overallScore: Double = 0.0,
    @ColumnInfo(name = "play_rank") val playRank: Int = 0,
    @ColumnInfo(name = "duration_rank") val durationRank: Int = 0,
    @ColumnInfo(name = "overall_rank") val overallRank: Int = 0,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable {

    companion object {
        val Empty = PlaylistEntity(-1, "", null, null)
    }
}
