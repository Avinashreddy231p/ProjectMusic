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

package com.mardous.projectmusic.ui.dialogs.metadata

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.EXTRA_SONGS
import com.mardous.projectmusic.extensions.extraNotNull
import com.mardous.projectmusic.extensions.media.songCountStr
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.extensions.withArgs
import com.mardous.projectmusic.ui.component.compose.BottomSheetDialogSurface
import com.mardous.projectmusic.ui.screen.library.AssignToMetadataUiState
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

enum class AssignMode {
    MOOD, TAG, INSTRUMENT
}

class AssignToMetadataDialog : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songs by extraNotNull<List<Song>>(EXTRA_SONGS)
    private val mode by lazy {
        arguments?.getSerializable(EXTRA_MODE) as? AssignMode ?: AssignMode.MOOD
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                ProjectMusicTheme {
                    AssignToMetadataScreen(
                        songs = songs,
                        mode = mode,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun AssignToMetadataScreen(
        songs: List<Song>,
        mode: AssignMode,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val uiState by libraryViewModel.assignToMetadataUiState.collectAsState()

        val checkedIds = rememberSaveable { mutableStateListOf<Long>() }
        var searchQuery by rememberSaveable { mutableStateOf("") }

        val listBottomPadding by animateDpAsState(
            targetValue = if (checkedIds.isNotEmpty()) 116.dp else 16.dp,
            animationSpec = tween(1000),
            label = "listBottomPadding"
        )

        LaunchedEffect(Unit) {
            libraryViewModel.prepareToAssignMetadata()
        }

        DisposableEffect(Unit) {
            onDispose {
                libraryViewModel.finishAssigningMetadata()
            }
        }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Box {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = listBottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = when (mode) {
                                        AssignMode.MOOD -> "Add to Mood"
                                        AssignMode.TAG -> "Add to Tag"
                                        AssignMode.INSTRUMENT -> "Add to Instrument"
                                    },
                                    style = MaterialTheme.typography.headlineSmallEmphasized,
                                    maxLines = 1
                                )

                                Text(
                                    text = songs.songCountStr(context),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }

                        item {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                label = {
                                    Text(
                                        when (mode) {
                                            AssignMode.MOOD -> "Search Moods"
                                            AssignMode.TAG -> "Search Tags"
                                            AssignMode.INSTRUMENT -> "Search Instruments"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_search_24dp),
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_cancel_24dp),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    errorIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item { Spacer(Modifier.height(16.dp)) }

                        when (val state = uiState) {
                            is AssignToMetadataUiState.Loading -> {
                                item {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 72.dp)
                                    ) {
                                        CircularWavyProgressIndicator()
                                    }
                                }
                            }

                            is AssignToMetadataUiState.Ready -> {
                                if (state.isLoading) {
                                    item {
                                        LinearWavyProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        )
                                    }
                                }

                                when (mode) {
                                    AssignMode.MOOD -> {
                                        val moods = state.moods.filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                        itemsIndexed(moods, { _, mood -> mood.id }) { index, mood ->
                                            MetadataItem(
                                                title = mood.name,
                                                subtitle = mood.description,
                                                iconText = mood.emoji,
                                                selected = checkedIds.contains(mood.id),
                                                index = index,
                                                count = moods.size,
                                                onClick = {
                                                    if (checkedIds.contains(mood.id)) checkedIds.remove(mood.id)
                                                    else checkedIds.add(mood.id)
                                                }
                                            )
                                        }
                                    }

                                    AssignMode.TAG -> {
                                        val tags = state.tags.values.flatten().filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                        itemsIndexed(tags, { _, tag -> tag.id }) { index, tag ->
                                            MetadataItem(
                                                title = tag.name,
                                                subtitle = tag.category.name,
                                                iconRes = R.drawable.ic_tag_24dp,
                                                selected = checkedIds.contains(tag.id),
                                                index = index,
                                                count = tags.size,
                                                onClick = {
                                                    if (checkedIds.contains(tag.id)) checkedIds.remove(tag.id)
                                                    else checkedIds.add(tag.id)
                                                }
                                            )
                                        }
                                    }

                                    AssignMode.INSTRUMENT -> {
                                        val instruments = state.instruments.values.flatten().filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }
                                        itemsIndexed(
                                            instruments,
                                            { _, instrument -> instrument.id }) { index, instrument ->
                                            MetadataItem(
                                                title = instrument.name,
                                                subtitle = instrument.family.name,
                                                iconRes = R.drawable.ic_piano_24dp,
                                                selected = checkedIds.contains(instrument.id),
                                                index = index,
                                                count = instruments.size,
                                                onClick = {
                                                    if (checkedIds.contains(instrument.id)) checkedIds.remove(instrument.id)
                                                    else checkedIds.add(instrument.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            is AssignToMetadataUiState.Completed -> {
                                if (state.isSuccess) {
                                    context.showToast("Metadata assigned successfully")
                                }
                                onDismiss()
                            }

                            else -> {}
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = checkedIds.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        ElevatedCard(shape = CircleShape) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "${checkedIds.size} selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                                Button(
                                    onClick = {
                                        when (mode) {
                                            AssignMode.MOOD -> libraryViewModel.assignToMoods(checkedIds, songs)
                                            AssignMode.TAG -> libraryViewModel.assignToTags(checkedIds, songs)
                                            AssignMode.INSTRUMENT -> libraryViewModel.assignToInstruments(
                                                checkedIds,
                                                songs
                                            )
                                        }
                                    },
                                    contentPadding = ButtonDefaults.MediumContentPadding
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24dp),
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun MetadataItem(
        title: String,
        subtitle: String?,
        selected: Boolean,
        index: Int,
        count: Int,
        onClick: () -> Unit,
        iconRes: Int? = null,
        iconText: String? = null
    ) {
        SegmentedListItem(
            checked = selected,
            onCheckedChange = { onClick() },
            shapes = ListItemDefaults.segmentedShapes(index, count),
            leadingContent = {
                if (iconText != null) {
                    Text(text = iconText, style = MaterialTheme.typography.headlineSmall)
                } else if (iconRes != null) {
                    Icon(painter = painterResource(iconRes), contentDescription = null)
                }
            },
            trailingContent = {
                Checkbox(checked = selected, onCheckedChange = null)
            },
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_MODE = "extra_mode"

        fun create(song: Song, mode: AssignMode) = create(listOf(song), mode)

        fun create(songs: List<Song>, mode: AssignMode): AssignToMetadataDialog {
            return AssignToMetadataDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
                putSerializable(EXTRA_MODE, mode)
            }
        }
    }
}
