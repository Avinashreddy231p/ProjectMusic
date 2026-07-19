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

package com.mardous.projectmusic.ui.component.compose.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.local.database.metadata.TagCategory
import com.mardous.projectmusic.ui.component.compose.MaterialSwitch

/**
 * State class for Library Dimension filters.
 */
data class LibraryFilterState(
    val favoritesOnly: Boolean = false,
    val showHidden: Boolean = false,
    val dataSourceAI: Boolean = true,
    val dataSourceManual: Boolean = true,
    val hasSongs: Boolean = false,
    val minSongCount: Int = 0,
    val selectedTagCategories: Set<TagCategory> = emptySet(),
    val selectedInstrumentFamilies: Set<InstrumentFamily> = emptySet()
)

/**
 * A modal bottom sheet for filtering the library dimensions.
 *
 * @param state The current filter state.
 * @param onStateChange Callback when the filter state changes.
 * @param onReset Callback to reset filters to default.
 * @param onDismissRequest Callback to close the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpressiveFilterSheet(
    state: LibraryFilterState,
    onStateChange: (LibraryFilterState) -> Unit,
    onReset: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Reset button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_restart_alt_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Filters")
                }
            }

            // Visibility Section
            FilterSectionHeader(title = "Visibility")
            MaterialSwitch(
                title = "Favorites only",
                isChecked = state.favoritesOnly,
                onClick = { onStateChange(state.copy(favoritesOnly = !state.favoritesOnly)) },
                icon = painterResource(R.drawable.ic_favorite_24dp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            MaterialSwitch(
                title = "Show Hidden",
                isChecked = state.showHidden,
                onClick = { onStateChange(state.copy(showHidden = !state.showHidden)) },
                icon = painterResource(R.drawable.ic_visibility_24dp),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Data Source Section
            FilterSectionHeader(title = "Data Source")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.dataSourceAI,
                    onClick = { onStateChange(state.copy(dataSourceAI = !state.dataSourceAI)) },
                    label = { Text("AI Generated") },
                    leadingIcon = if (state.dataSourceAI) {
                        { Icon(painterResource(R.drawable.ic_check_24dp), null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(48.dp)
                )
                FilterChip(
                    selected = state.dataSourceManual,
                    onClick = { onStateChange(state.copy(dataSourceManual = !state.dataSourceManual)) },
                    label = { Text("Manual") },
                    leadingIcon = if (state.dataSourceManual) {
                        { Icon(painterResource(R.drawable.ic_check_24dp), null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.height(48.dp)
                )
            }

            // Content Section
            FilterSectionHeader(title = "Content")
            MaterialSwitch(
                title = "Has Songs",
                isChecked = state.hasSongs,
                onClick = { onStateChange(state.copy(hasSongs = !state.hasSongs)) },
                icon = painterResource(R.drawable.ic_music_note_24dp),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Minimum Song Count: ${state.minSongCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.minSongCount.toFloat(),
                    onValueChange = { onStateChange(state.copy(minSongCount = it.toInt())) },
                    valueRange = 0f..50f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Categories/Families Section
            FilterSectionHeader(title = "Tag Categories")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagCategory.entries.forEach { category ->
                    val isSelected = state.selectedTagCategories.contains(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelected = if (isSelected) {
                                state.selectedTagCategories - category
                            } else {
                                state.selectedTagCategories + category
                            }
                            onStateChange(state.copy(selectedTagCategories = newSelected))
                        },
                        label = { Text(category.name) },
                        shape = MaterialTheme.shapes.large
                    )
                }
            }

            FilterSectionHeader(title = "Instrument Families")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InstrumentFamily.entries.forEach { family ->
                    val isSelected = state.selectedInstrumentFamilies.contains(family)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelected = if (isSelected) {
                                state.selectedInstrumentFamilies - family
                            } else {
                                state.selectedInstrumentFamilies + family
                            }
                            onStateChange(state.copy(selectedInstrumentFamilies = newSelected))
                        },
                        label = { Text(family.name) },
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
