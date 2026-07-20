/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.screen.library

import com.mardous.projectmusic.data.model.Instrument
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.model.Mood
import com.mardous.projectmusic.data.model.Tag
import com.mardous.projectmusic.data.local.database.metadata.TagCategory

sealed class AssignToMetadataUiState(open val isLoading: Boolean) {
    data object Loading : AssignToMetadataUiState(true)
    data class Ready(
        val moods: List<Mood> = emptyList(),
        val tags: Map<TagCategory, List<Tag>> = emptyMap(),
        val instruments: Map<InstrumentFamily, List<Instrument>> = emptyMap(),
        override val isLoading: Boolean = false
    ) : AssignToMetadataUiState(isLoading)

    data class Completed(val isSuccess: Boolean) : AssignToMetadataUiState(false)
}

data class MusicBrainzScanState(
    val isScanning: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val label: String? = null,
    val result: String? = null
)

data class LyricsScanState(
    val isScanning: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val label: String? = null,
    val result: String? = null
)
