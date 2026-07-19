package com.mardous.projectmusic.ui.dialogs.songs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.local.repository.SongRepository
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.ui.component.compose.BottomSheetDialogSurface
import com.mardous.projectmusic.ui.component.compose.MediaImage
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.android.ext.android.inject

class SongSelectionDialog : BottomSheetDialogFragment() {

    private val songRepository: SongRepository by inject()
    private var onConfirm: ((List<Long>) -> Unit)? = null

    fun setOnConfirmListener(listener: (List<Long>) -> Unit) {
        onConfirm = listener
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
                    SongSelectionScreen(
                        onConfirm = {
                            onConfirm?.invoke(it)
                            dismiss()
                        },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SongSelectionScreen(
        onConfirm: (List<Long>) -> Unit,
        onDismiss: () -> Unit
    ) {
        var searchQuery by rememberSaveable { mutableStateOf("") }
        val allSongs = remember { songRepository.songs() }
        val filteredSongs = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                allSongs
            } else {
                allSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artistName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
        val selectedIds = rememberSaveable { mutableStateListOf<Long>() }

        BottomSheetDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Songs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    placeholder = { Text("Search songs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isSelected = selectedIds.contains(song.id)
                            SongSelectionItem(
                                song = song,
                                isSelected = isSelected,
                                onToggle = {
                                    if (isSelected) selectedIds.remove(song.id)
                                    else selectedIds.add(song.id)
                                }
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedIds.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { onConfirm(selectedIds.toList()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Selection (${selectedIds.size})")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SongSelectionItem(
        song: Song,
        isSelected: Boolean,
        onToggle: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaImage(
                model = song,
                modifier = Modifier
                    .size(56.dp)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }

    companion object {
        fun newInstance(): SongSelectionDialog {
            return SongSelectionDialog()
        }
    }
}
