package com.mardous.projectmusic.data.model

import android.os.Parcelable
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import kotlinx.parcelize.Parcelize

@Parcelize
data class Instrument(
    val id: Long,
    val name: String,
    val family: InstrumentFamily,
    val songCount: Int,
    val playCount: Long,
    val listenedDurationMs: Long,
    val overallRank: Int,
    val avgConfidence: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String?,
    val icon: String?,
    val color: Int?
) : Parcelable
