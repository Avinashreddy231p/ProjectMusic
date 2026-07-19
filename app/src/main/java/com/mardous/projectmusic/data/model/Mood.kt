package com.mardous.projectmusic.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Mood(
    val id: Long,
    val name: String,
    val description: String?,
    val emoji: String?,
    val songCount: Int,
    val playCount: Long,
    val listenedDurationMs: Long,
    val overallRank: Int,
    val favoriteCount: Int,
    val avgCompletion: Float,
    val createdAt: Long,
    val updatedAt: Long
) : Parcelable {
    companion object {
        val EmptyMood = Mood(-1, "", null, null, 0, 0, 0, 0, 0, 0f, 0, 0)
    }
}
