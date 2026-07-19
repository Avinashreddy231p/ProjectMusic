package com.mardous.projectmusic.data.model

import android.os.Parcelable
import com.mardous.projectmusic.data.local.database.metadata.TagCategory
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: Long,
    val name: String,
    val category: TagCategory,
    val songCount: Int,
    val playCount: Long,
    val listenedDurationMs: Long,
    val overallRank: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String?,
    val icon: String?,
    val color: Int?
) : Parcelable
