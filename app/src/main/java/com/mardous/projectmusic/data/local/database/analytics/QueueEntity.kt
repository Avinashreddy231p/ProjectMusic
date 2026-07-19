package com.mardous.projectmusic.data.local.database.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
class QueueEntity(
    @PrimaryKey
    val id: String,
    val order: Int
)
