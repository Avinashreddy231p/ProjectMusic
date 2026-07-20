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

package com.mardous.projectmusic.ui.dialogs.lyrics

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LyricsProgressDialog : DialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
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
                    LyricsProgressScreen(
                        onDismiss = {
                            libraryViewModel.finishLyricsScan()
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun LyricsProgressScreen(
        onDismiss: () -> Unit
    ) {
        val scanState by libraryViewModel.lyricsScanState.collectAsStateWithLifecycle()

        if (scanState == null) {
            LaunchedEffect(Unit) { onDismiss() }
            return
        }

        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            confirmButton = {
                if (scanState?.isScanning == false) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close_action))
                    }
                }
            },
            title = {
                Text(
                    text = if (scanState?.isScanning == true) 
                        stringResource(R.string.lyrics_lookup_progress_title)
                    else stringResource(R.string.lyrics_lookup_finished_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (scanState?.isScanning == true) {
                        LinearProgressIndicator(
                            progress = {
                                if (scanState!!.total > 0)
                                    scanState!!.progress.toFloat() / scanState!!.total
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        
                        Text(
                            text = "${scanState?.progress} / ${scanState?.total}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = scanState?.label ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = scanState?.result ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
