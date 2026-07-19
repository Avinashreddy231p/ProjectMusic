/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  See the GNU General Public License
 * for more details.
 *
 */

package com.mardous.projectmusic.data.local.database.sync

object RankingWeights {
    const val DEFAULT_PLAY_COUNT_WEIGHT = 2.0
    const val DEFAULT_DURATION_WEIGHT = 0.0001 // Weight per ms
    const val DEFAULT_FAVORITE_BONUS = 5.0

    var playCountWeight: Double = DEFAULT_PLAY_COUNT_WEIGHT
    var durationWeight: Double = DEFAULT_DURATION_WEIGHT
    var favoriteBonus: Double = DEFAULT_FAVORITE_BONUS
}
