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

package com.mardous.projectmusic.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.ACCENT_COLOR
import com.mardous.projectmusic.util.LYRICS_ACCENT_COLOR
import com.mardous.projectmusic.util.ERA_PRIMARY_SEED
import com.mardous.projectmusic.util.ERA_SECONDARY_SEED
import com.mardous.projectmusic.util.ERA_TERTIARY_SEED
import com.mardous.projectmusic.util.ERA_ERROR_SEED

class AccentColorPreferenceDialog : DialogFragment() {

    companion object {
        private const val ARG_PREF_KEY = "pref_key"

        fun newInstance(prefKey: String): AccentColorPreferenceDialog {
            val args = Bundle()
            args.putString(ARG_PREF_KEY, prefKey)
            val fragment = AccentColorPreferenceDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private val prefKey by lazy {
        arguments?.getString(ARG_PREF_KEY) ?: ACCENT_COLOR
    }

    private val presetColors = listOf(
        Color(0xFFF44336), // Red
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
    )

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val initialColor = when (prefKey) {
            LYRICS_ACCENT_COLOR -> Preferences.lyricsAccentColor
            ERA_PRIMARY_SEED -> Preferences.eraPrimarySeed
            ERA_SECONDARY_SEED -> Preferences.eraSecondarySeed
            ERA_TERTIARY_SEED -> Preferences.eraTertiarySeed
            ERA_ERROR_SEED -> Preferences.eraErrorSeed
            else -> Preferences.accentColor
        }?.let { Color(it) }

        val titleRes = when (prefKey) {
            LYRICS_ACCENT_COLOR -> R.string.lyrics_accent_color_title
            ERA_PRIMARY_SEED -> R.string.era_primary_seed_title
            ERA_SECONDARY_SEED -> R.string.era_secondary_seed_title
            ERA_TERTIARY_SEED -> R.string.era_tertiary_seed_title
            ERA_ERROR_SEED -> R.string.era_error_seed_title
            else -> R.string.accent_color_title
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        ProjectMusicTheme {
                            DialogScreen(
                                initialColor = initialColor,
                                onColorSelected = { color ->
                                    val colorArgb = color?.toArgb()
                                    when (prefKey) {
                                        LYRICS_ACCENT_COLOR -> Preferences.lyricsAccentColor = colorArgb
                                        ERA_PRIMARY_SEED -> Preferences.eraPrimarySeed = colorArgb ?: 0
                                        ERA_SECONDARY_SEED -> Preferences.eraSecondarySeed = colorArgb ?: 0
                                        ERA_TERTIARY_SEED -> Preferences.eraTertiarySeed = colorArgb ?: 0
                                        ERA_ERROR_SEED -> Preferences.eraErrorSeed = colorArgb ?: 0
                                        else -> Preferences.accentColor = colorArgb
                                    }
                                    dialog?.dismiss()
                                    if (isAdded) requireActivity().recreate()
                                }
                            )
                        }
                    }
                }
            )
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    @OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Composable
    private fun DialogScreen(
        initialColor: Color?,
        onColorSelected: (Color?) -> Unit
    ) {
        var hexText by remember { 
            mutableStateOf(
                initialColor?.let { 
                    String.format("#%06X", 0xFFFFFF and it.toArgb()) 
                } ?: ""
            ) 
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { hexText = it },
                    label = { Text("Custom Hex Color (e.g. #FF0000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onColorSelected(null) }) {
                        Text("Default")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val parsed = android.graphics.Color.parseColor(hexText)
                                onColorSelected(Color(parsed))
                            } catch (e: Exception) {
                            }
                        }
                    ) {
                        Text("Apply Hex")
                    }
                }
            }
        }
    }
}
