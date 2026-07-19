package com.mardous.projectmusic.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onImportFontClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    var showVibrantLyricsModeDialog by remember { mutableStateOf(false) }
    var showBackgroundEffectDialog by remember { mutableStateOf(false) }
    var showAdvancedSheet by remember { mutableStateOf(false) }

    if (showAdvancedSheet) {
        LyricsAdvancedBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showAdvancedSheet = false }
        )
    }

    if (showVibrantLyricsModeDialog) {
        SingleChoiceDialog(
            title = "Vibrant Background",
            options = VibrantBackgroundMode.entries,
            selectedOption = uiState.vibrantLyricsBackgroundMode,
            onOptionSelected = { viewModel.setVibrantLyricsBackgroundMode(it) },
            onDismissRequest = { showVibrantLyricsModeDialog = false }
        )
    }

    if (showBackgroundEffectDialog) {
        val entries = context.resources.getStringArray(R.array.pref_lyrics_background_effect_entries)
        val values = context.resources.getStringArray(R.array.pref_lyrics_background_effect_values)
        SingleChoiceDialog(
            title = stringResource(R.string.lyrics_background_effect_title),
            options = values.toList(),
            selectedOption = uiState.lyricsBackgroundEffect,
            onOptionSelected = { viewModel.setLyricsBackgroundEffect(it) },
            onDismissRequest = { showBackgroundEffectDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.lyrics_preferences_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painter = painterResource(R.drawable.ic_back_24dp), contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- SYNCED LYRICS ---
            item { DashboardCategoryHeader("Synced Experience") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_syllable_mode_title),
                        summary = stringResource(R.string.lyrics_syllable_mode_summary),
                        checked = uiState.enableSyllableLyrics,
                        onCheckedChange = { viewModel.setEnableSyllableLyrics(it) },
                        icon = R.drawable.ic_match_word_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_karaoke_style_title),
                        checked = uiState.enableKaraokeStyle,
                        onCheckedChange = { viewModel.setEnableKaraokeStyle(it) },
                        enabled = uiState.enableSyllableLyrics
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_resume_on_seek_title),
                        checked = uiState.resumeOnSeek,
                        onCheckedChange = { viewModel.setResumeOnSeek(it) }
                    )
                }
            }

            // --- TRANSLATION ---
            item { DashboardCategoryHeader("Content & Display") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_show_translation_title),
                        checked = uiState.showTranslation,
                        onCheckedChange = { viewModel.setShowTranslation(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_show_transliteration_title),
                        checked = uiState.showTransliteration,
                        onCheckedChange = { viewModel.setShowTransliteration(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_center_current_line_title),
                        checked = uiState.centerCurrentLine,
                        onCheckedChange = { viewModel.setCenterCurrentLine(it) },
                        icon = R.drawable.ic_align_center_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_center_horizontally_title),
                        checked = uiState.centerHorizontally,
                        onCheckedChange = { viewModel.setCenterHorizontally(it) },
                        icon = R.drawable.ic_align_horizontal_center_24dp
                    )
                }
            }

            // --- VISUAL STYLING ---
            item { DashboardCategoryHeader("Visual Styling") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.lyrics_background_effect_title),
                        summary = uiState.lyricsBackgroundEffect.replaceFirstChar { it.uppercase() },
                        onClick = { showBackgroundEffectDialog = true }
                    )
                    
                    if (uiState.lyricsBackgroundEffect != "none") {
                        SegmentedPreferenceItem(
                            title = "Vibrant Mode",
                            summary = uiState.vibrantLyricsBackgroundMode.name,
                            onClick = { showVibrantLyricsModeDialog = true }
                        )
                    }

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_blur_effect_title),
                        checked = uiState.lyricsBlurEffect,
                        onCheckedChange = { viewModel.setLyricsBlurEffect(it) },
                        icon = R.drawable.ic_blur_on_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_shadow_effect_title),
                        checked = uiState.lyricsShadowEffect,
                        onCheckedChange = { viewModel.toggleLyricsShadow(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_progressive_coloring_title),
                        checked = uiState.progressiveColoring,
                        onCheckedChange = { viewModel.setProgressiveColoring(it) },
                        icon = R.drawable.ic_blur_linear_24dp
                    )
                    
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.lyrics_accent_color_title),
                        trailingContent = {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = androidx.compose.ui.graphics.Color(uiState.lyricsAccentColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {}
                        },
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                com.mardous.projectmusic.ui.component.preferences.dialog.AccentColorPreferenceDialog.newInstance(LYRICS_ACCENT_COLOR).show(it.supportFragmentManager, "LYRICS_ACCENT")
                            }
                        }
                    )
                }
            }

            // --- TYPOGRAPHY ---
            item { DashboardCategoryHeader("Typography") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.lyrics_custom_font_title),
                        checked = uiState.useCustomFont,
                        onCheckedChange = { viewModel.setUseCustomFont(it) },
                        icon = R.drawable.ic_custom_typography_24dp
                    )
                    
                    if (uiState.useCustomFont) {
                        ExpressivePreferenceItem(
                            title = "Import Custom Font",
                            summary = uiState.customFontPath ?: "Select .ttf or .otf file",
                            icon = R.drawable.ic_file_open_24dp,
                            onClick = onImportFontClick
                        )
                    }

                    ExpressiveSliderItem(
                        title = stringResource(R.string.lyrics_line_spacing_title),
                        value = uiState.lineSpacing.toFloat(),
                        onValueChange = { viewModel.setLineSpacing(it.toInt()) },
                        valueRange = 20f..80f,
                        icon = R.drawable.ic_format_line_spacing_24dp
                    )
                }
            }

            item {
                Text("Synced Typography", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Bold Font",
                        checked = uiState.syncedLyricsBold,
                        onCheckedChange = { viewModel.setSyncedLyricsBold(it) }
                    )
                    ExpressiveSliderItem(
                        title = "Player Font Size",
                        value = uiState.syncedFontSizePlayer.toFloat(),
                        onValueChange = { viewModel.setSyncedFontSizePlayer(it.toInt()) },
                        valueRange = 12f..32f,
                        valueDisplay = { "${it.toInt()} sp" }
                    )
                    ExpressiveSliderItem(
                        title = "Full Font Size",
                        value = uiState.syncedFontSizeFull.toFloat(),
                        onValueChange = { viewModel.setSyncedFontSizeFull(it.toInt()) },
                        valueRange = 16f..48f,
                        valueDisplay = { "${it.toInt()} sp" }
                    )
                }
            }

            item {
                Text("Plain Typography", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Bold Font",
                        checked = uiState.unsyncedLyricsBold,
                        onCheckedChange = { viewModel.setUnsyncedLyricsBold(it) }
                    )
                    ExpressiveSliderItem(
                        title = "Player Font Size",
                        value = uiState.unsyncedFontSizePlayer.toFloat(),
                        onValueChange = { viewModel.setUnsyncedFontSizePlayer(it.toInt()) },
                        valueRange = 12f..32f,
                        valueDisplay = { "${it.toInt()} sp" }
                    )
                    ExpressiveSliderItem(
                        title = "Full Font Size",
                        value = uiState.unsyncedFontSizeFull.toFloat(),
                        onValueChange = { viewModel.setUnsyncedFontSizeFull(it.toInt()) },
                        valueRange = 16f..48f,
                        valueDisplay = { "${it.toInt()} sp" }
                    )
                }
            }

            // --- AA METADATA ---
            item { DashboardCategoryHeader("Automotive & External") }
            item {
                ExpressiveSwitchItem(
                    title = stringResource(R.string.aa_metadata_lyrics_title),
                    summary = stringResource(R.string.aa_metadata_lyrics_summary),
                    checked = uiState.aaMetadataLyrics,
                    onCheckedChange = { viewModel.setAaMetadataLyrics(it) },
                    icon = R.drawable.ic_lyrics_24dp
                )
            }

            // --- ADVANCED ---
            item { DashboardCategoryHeader("Processing") }
            item {
                ExpressivePreferenceItem(
                    title = "Advanced Processing",
                    summary = "Instrumental identifiers and file formats",
                    icon = R.drawable.ic_tune_24dp,
                    onClick = { showAdvancedSheet = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsAdvancedBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showInstrumentalDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }

    if (showFormatDialog) {
        val entries = context.resources.getStringArray(R.array.pref_lyrics_file_format_entries)
        val values = context.resources.getStringArray(R.array.pref_lyrics_file_format_values)
        
        SingleChoiceDialog(
            title = "Preferred Lyrics Format",
            options = values.toList(),
            selectedOption = uiState.preferredLyricsFileFormat,
            onOptionSelected = { viewModel.setPreferredLyricsFileFormat(it) },
            onDismissRequest = { showFormatDialog = false },
            optionTitle = { entries[values.indexOf(it)] }
        )
    }

    if (showInstrumentalDialog) {
        TextEditDialog(
            title = "Instrumental Identifiers",
            initialValue = uiState.instrumentalTrackIdentifiers,
            onConfirm = { viewModel.setInstrumentalTrackIdentifiers(it) },
            onDismissRequest = { showInstrumentalDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Advanced Processing",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = "Instrumental Identifiers",
                        summary = uiState.instrumentalTrackIdentifiers,
                        onClick = { showInstrumentalDialog = true }
                    )
                    ExpressiveSwitchItem(
                        title = "Mark by Title",
                        summary = "Identify instrumentals via track title",
                        checked = uiState.markInstrumentalTracksByTitle,
                        onCheckedChange = { viewModel.setMarkInstrumentalTracksByTitle(it) }
                    )
                }
            }

            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Ignore Blank Lines",
                        checked = uiState.ignoreBlankLinesInLyrics,
                        onCheckedChange = { viewModel.setIgnoreBlankLinesInLyrics(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Enforce UTF-8",
                        summary = "Treat all files as UTF-8 encoded",
                        checked = uiState.forceUtf8EncodingForLyrics,
                        onCheckedChange = { viewModel.setForceUtf8EncodingForLyrics(it) }
                    )
                    SegmentedPreferenceItem(
                        title = "Preferred Format",
                        summary = uiState.preferredLyricsFileFormat.uppercase(),
                        onClick = { showFormatDialog = true }
                    )
                }
            }
        }
    }
}
