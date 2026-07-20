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
import com.mardous.projectmusic.core.model.theme.NowPlayingScreen
import com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    // Bottom Sheet States
    var showStyleDialog by remember { mutableStateOf(false) }
    var showVibrantModeDialog by remember { mutableStateOf(false) }
    var editingVibrantTarget by remember { mutableStateOf(0) } // 0: Background, 1: Lyrics
    var showGesturesSheet by remember { mutableStateOf(false) }
    var showExtraInfoDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showTransitionDialog by remember { mutableStateOf(false) }
    var showSeekBarSheet by remember { mutableStateOf(false) }

    if (showSeekBarSheet) {
        SeekBarCustomizationBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showSeekBarSheet = false }
        )
    }

    if (showColorSchemeDialog) {
        val supportedSchemes = uiState.nowPlayingScreen.supportedColorSchemes
        SingleChoiceDialog(
            title = "Layout Color Scheme",
            options = supportedSchemes,
            selectedOption = uiState.nowPlayingColorScheme,
            onOptionSelected = { viewModel.setNowPlayingColorScheme(it) },
            onDismissRequest = { showColorSchemeDialog = false },
            optionTitle = { stringResource(it.titleRes) }
        )
    }

    if (showTransitionDialog) {
        val supportedTransitions = uiState.nowPlayingScreen.supportedTransitions
        SingleChoiceDialog(
            title = "Page Transition",
            options = supportedTransitions,
            selectedOption = uiState.nowPlayingTransition,
            onOptionSelected = { viewModel.setNowPlayingTransition(it) },
            onDismissRequest = { showTransitionDialog = false },
            optionTitle = { stringResource(it.nameRes) }
        )
    }

    if (showGesturesSheet) {
        NowPlayingGesturesBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showGesturesSheet = false }
        )
    }

    if (showExtraInfoDialog) {
        (context as? FragmentActivity)?.let {
            com.mardous.projectmusic.ui.component.preferences.dialog.ExtraInfoPreferenceDialog.nowPlaying(it).show(it.supportFragmentManager, "EXTRA_INFO")
        }
        showExtraInfoDialog = false
    }

    if (showStyleDialog) {
        SingleChoiceDialog(
            title = "Player Style",
            options = NowPlayingScreen.entries,
            selectedOption = uiState.nowPlayingScreen,
            onOptionSelected = { viewModel.setNowPlayingScreen(it) },
            onDismissRequest = { showStyleDialog = false },
            optionTitle = { stringResource(it.titleRes) }
        )
    }

    if (showVibrantModeDialog) {
        val title = if (editingVibrantTarget == 0) "Vibrant Background" else "Lyrics Background"
        val selected = if (editingVibrantTarget == 0) uiState.vibrantBackgroundMode else uiState.vibrantLyricsBackgroundMode
        
        SingleChoiceDialog(
            title = title,
            options = VibrantBackgroundMode.entries,
            selectedOption = selected,
            onOptionSelected = { 
                if (editingVibrantTarget == 0) viewModel.setVibrantBackgroundMode(it)
                else viewModel.setVibrantLyricsBackgroundMode(it)
            },
            onDismissRequest = { showVibrantModeDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.now_playing_title)) },
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
            // --- MAIN VISUALS ---
            item { DashboardCategoryHeader("Main Visuals") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = "Player Layout Style",
                        summary = stringResource(uiState.nowPlayingScreen.titleRes),
                        icon = R.drawable.ic_play_circle_24dp,
                        onClick = { showStyleDialog = true }
                    )

                    if (uiState.nowPlayingScreen.supportedColorSchemes.size > 1) {
                        SegmentedPreferenceItem(
                            title = "Layout Color Scheme",
                            summary = stringResource(uiState.nowPlayingColorScheme.titleRes),
                            icon = R.drawable.ic_palette_24dp,
                            onClick = { showColorSchemeDialog = true }
                        )
                    }

                    if (uiState.nowPlayingScreen.supportedTransitions.size > 1) {
                        SegmentedPreferenceItem(
                            title = "Page Transition",
                            summary = stringResource(uiState.nowPlayingTransition.nameRes),
                            icon = R.drawable.ic_view_carousel_24dp,
                            onClick = { showTransitionDialog = true }
                        )
                    }
                    
                    ExpressiveSwitchItem(
                        title = "Carousel Effect",
                        summary = "Animated album cover transition",
                        checked = uiState.carouselEffect,
                        onCheckedChange = { viewModel.setCarouselEffect(it) },
                        icon = R.drawable.ic_image_24dp
                    )

                    ExpressiveSwitchItem(
                        title = "Small Image Mode",
                        checked = uiState.nowPlayingSmallImage,
                        onCheckedChange = { viewModel.setNowPlayingSmallImage(it) }
                    )

                    ExpressiveSwitchItem(
                        title = "Show Lyrics on Cover",
                        checked = uiState.showLyricsOnCover,
                        onCheckedChange = { viewModel.setShowLyricsOnCover(it) }
                    )

                    ExpressiveSwitchItem(
                        title = "Lock Queue",
                        summary = "Disable queue reordering",
                        checked = uiState.isQueueLocked,
                        onCheckedChange = { viewModel.setQueueLocked(it) },
                        icon = R.drawable.ic_lock_24dp
                    )
                }
            }

            // --- GEOMETRY ---
            item { DashboardCategoryHeader("Shapes & Geometry") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSliderItem(
                        title = "Cover Corner Radius",
                        value = uiState.nowPlayingCornerRadius.toFloat(),
                        onValueChange = { viewModel.setNowPlayingCornerRadius(it.toInt()) },
                        valueRange = 0f..64f,
                        icon = R.drawable.ic_rounded_corner_24dp,
                        valueDisplay = { "${it.toInt()}dp" }
                    )
                    ExpressiveSliderItem(
                        title = "Lyrics Card Corner Radius",
                        value = uiState.lyricsCardCornerRadius.toFloat(),
                        onValueChange = { viewModel.setLyricsCardCornerRadius(it.toInt()) },
                        valueRange = 0f..64f,
                        icon = R.drawable.ic_rounded_corner_24dp,
                        valueDisplay = { "${it.toInt()}dp" }
                    )
                }
            }

            // --- BACKGROUND ---
            item { DashboardCategoryHeader("Background Effects") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = "Vibrant Background Mode",
                        summary = uiState.vibrantBackgroundMode.name,
                        icon = R.drawable.ic_palette_24dp,
                        onClick = { 
                            editingVibrantTarget = 0
                            showVibrantModeDialog = true 
                        }
                    )

                    SegmentedPreferenceItem(
                        title = "Lyrics Background Mode",
                        summary = uiState.vibrantLyricsBackgroundMode.name,
                        icon = R.drawable.ic_lyrics_outline_24dp,
                        onClick = { 
                            editingVibrantTarget = 1
                            showVibrantModeDialog = true 
                        }
                    )
                    
                    ExpressiveSwitchItem(
                        title = "Fluid Animations",
                        summary = "Enable background motion",
                        checked = uiState.vibrantBackgroundAnimations,
                        onCheckedChange = { viewModel.setVibrantBackgroundAnimations(it) }
                    )

                    ExpressiveSwitchItem(
                        title = "High Quality",
                        summary = "Prioritize visual fidelity",
                        checked = uiState.vibrantBackgroundHighQuality,
                        onCheckedChange = { viewModel.setVibrantBackgroundHighQuality(it) }
                    )

                    ExpressiveSliderItem(
                        title = "Noise Level",
                        value = uiState.vibrantBackgroundNoiseLevel.toFloat(),
                        onValueChange = { viewModel.setVibrantBackgroundNoiseLevel(it.toInt()) },
                        valueRange = 0f..100f
                    )

                    ExpressiveSwitchItem(
                        title = "Global Background",
                        summary = "Apply visuals to entire system (experimental)",
                        checked = uiState.vibrantBackgroundGlobal,
                        onCheckedChange = { viewModel.setVibrantBackgroundGlobal(it) }
                    )
                }
            }

            // --- GESTURES ---
            item { DashboardCategoryHeader("Gestures & Interaction") }
            item {
                ExpressivePreferenceItem(
                    title = "Cover & Player Gestures",
                    summary = "Tap, double-tap, and swipe configurations",
                    icon = R.drawable.ic_swipe_24dp,
                    onClick = { showGesturesSheet = true }
                )
            }

            // --- CONTROLS ---
            item { DashboardCategoryHeader("Player Controls") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.circle_button_title),
                        checked = uiState.circlePlayButton,
                        onCheckedChange = { viewModel.setCirclePlayButton(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.adaptive_controls_title),
                        checked = uiState.adaptiveControls,
                        onCheckedChange = { viewModel.setAdaptiveControls(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.animate_controls_title),
                        checked = uiState.animatePlayerControl,
                        onCheckedChange = { viewModel.setAnimatePlayerControl(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.extra_controls_title),
                        checked = uiState.addExtraControls,
                        onCheckedChange = { viewModel.setAddExtraControls(it) },
                        icon = R.drawable.ic_next_24dp
                    )
                }
            }

            // --- PROGRESS ---
            item { DashboardCategoryHeader("Progress Indicator") }
            item {
                ExpressivePreferenceItem(
                    title = "Seek Bar Customization",
                    summary = "Style, wavy effects, and thumb shapes",
                    icon = R.drawable.ic_squiggly_seekbar_24dp,
                    onClick = { showSeekBarSheet = true }
                )
            }

            // --- DISPLAY & METADATA ---
            item { DashboardCategoryHeader("Information & Display") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.squiggly_seek_bar_title),
                        checked = uiState.squigglySeekBar,
                        onCheckedChange = { viewModel.setSquigglySeekBar(it) },
                        icon = R.drawable.ic_squiggly_seekbar_24dp
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.queue_height_title),
                        checked = uiState.queueHeight,
                        onCheckedChange = { viewModel.setQueueHeight(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.scrolling_text_title),
                        checked = uiState.enableScrollingText,
                        onCheckedChange = { viewModel.setEnableScrollingText(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.prefer_remaining_time_title),
                        checked = uiState.preferRemainingTime,
                        onCheckedChange = { viewModel.setPreferRemainingTime(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.prefer_album_artist_name_title),
                        checked = uiState.preferAlbumArtistName,
                        onCheckedChange = { viewModel.setPreferAlbumArtistName(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.display_album_name_title),
                        checked = uiState.displayAlbumTitle,
                        onCheckedChange = { viewModel.setDisplayAlbumTitle(it) }
                    )
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.display_next_song_title),
                        checked = uiState.displayNextSong,
                        onCheckedChange = { viewModel.setDisplayNextSong(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Display Extra Info",
                        checked = uiState.displayExtraInfo,
                        onCheckedChange = { viewModel.setDisplayExtraInfo(it) },
                        icon = R.drawable.ic_info_24dp
                    )
                    ExpressivePreferenceItem(
                        title = "Select Metadata Tags",
                        summary = "Choose what technical details to show",
                        enabled = uiState.displayExtraInfo,
                        onClick = { showExtraInfoDialog = true }
                    )
                }
            }

            // --- ADVANCED ---
            item { DashboardCategoryHeader("Advanced Visuals") }
            item {
                SegmentedPreferenceGroup {
                    ExpressiveSliderItem(
                        title = "Player Blur Intensity",
                        value = uiState.playerBlurRadius.toFloat(),
                        onValueChange = { viewModel.setPlayerBlurRadius(it.toInt()) },
                        valueRange = 1f..25f,
                        icon = R.drawable.ic_blur_linear_24dp
                    )
                    ExpressiveSwitchItem(
                        title = "Open on Play",
                        summary = "Bring player to front when playback starts",
                        checked = uiState.openOnPlay,
                        onCheckedChange = { viewModel.setOpenOnPlay(it) },
                        icon = R.drawable.ic_keyboard_arrow_up_24dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingGesturesBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Cover & Player Gestures",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                Text("Cover Tap Actions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                SegmentedPreferenceGroup {
                    ActionPreferenceItem("Single Tap", uiState.coverSingleTapAction, COVER_SINGLE_TAP_ACTION)
                    ActionPreferenceItem("Double Tap", uiState.coverDoubleTapAction, COVER_DOUBLE_TAP_ACTION)
                    ActionPreferenceItem("Left Double Tap", uiState.coverLeftDoubleTapAction, COVER_LEFT_DOUBLE_TAP_ACTION)
                    ActionPreferenceItem("Right Double Tap", uiState.coverRightDoubleTapAction, COVER_RIGHT_DOUBLE_TAP_ACTION)
                    ActionPreferenceItem("Long Press", uiState.coverLongPressAction, COVER_LONG_PRESS_ACTION)
                }
            }

            item {
                Text("Swipe Gestures", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Swipe on Cover",
                        summary = "Horizontal swipe to skip tracks",
                        checked = uiState.swipeOnCover,
                        onCheckedChange = { viewModel.setSwipeOnCover(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Mini Player Swipe",
                        summary = "Swipe mini player to skip",
                        checked = uiState.miniPlayerSwipeToSkip,
                        onCheckedChange = { viewModel.setMiniPlayerSwipeToSkip(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Swipe Up for Queue",
                        checked = uiState.swipeUpQueue,
                        onCheckedChange = { viewModel.setSwipeUpQueue(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Swipe Anywhere to Dismiss",
                        checked = uiState.swipeAnywhere,
                        onCheckedChange = { viewModel.setSwipeAnywhere(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Swipe Down to Dismiss",
                        checked = uiState.swipeDownToDismiss,
                        onCheckedChange = { viewModel.setSwipeDownToDismiss(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarCustomizationBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Seek Bar Customization",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = "Squiggly Seek Bar",
                        checked = uiState.squigglySeekBar,
                        onCheckedChange = { viewModel.setSquigglySeekBar(it) }
                    )
                }
            }

            item {
                Text("Visual Style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedPreferenceGroup {
                    val styles = ProgressBarStyle.entries
                    styles.forEach { style ->
                        SegmentedPreferenceItem(
                            title = style.name.lowercase().replaceFirstChar { it.uppercase() },
                            trailingContent = {
                                RadioButton(selected = uiState.progressBarStyle == style, onClick = null)
                            },
                            onClick = { viewModel.setProgressBarStyle(style) }
                        )
                    }
                }
            }

            item {
                Text("Thumb Style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedPreferenceGroup {
                    val thumbs = ThumbStyle.entries
                    thumbs.forEach { thumb ->
                        SegmentedPreferenceItem(
                            title = thumb.name.lowercase().replaceFirstChar { it.uppercase() },
                            trailingContent = {
                                RadioButton(selected = uiState.thumbStyle == thumb, onClick = null)
                            },
                            onClick = { viewModel.setThumbStyle(thumb) }
                        )
                    }
                }
            }

            item {
                Text("Thumb Size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedPreferenceGroup {
                    ExpressiveSliderItem(
                        title = "Scale Factor",
                        value = uiState.thumbSize,
                        onValueChange = { viewModel.setThumbSize(it) },
                        valueRange = 0.5f..2.0f,
                        valueDisplay = { String.format("%.1fx", it) }
                    )
                }
            }

            item {
                Text("Control Mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                SegmentedPreferenceGroup {
                    val modes = ProgressControlStyle.entries
                    modes.forEach { mode ->
                        SegmentedPreferenceItem(
                            title = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            trailingContent = {
                                RadioButton(selected = uiState.progressControlStyle == mode, onClick = null)
                            },
                            onClick = { viewModel.setProgressControlStyle(mode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionPreferenceItem(title: String, currentAction: com.mardous.projectmusic.core.model.action.NowPlayingAction, prefKey: String) {
    val context = LocalContext.current
    ExpressivePreferenceItem(
        title = title,
        summary = stringResource(currentAction.titleRes),
        onClick = {
            (context as? FragmentActivity)?.let {
                com.mardous.projectmusic.ui.component.preferences.dialog.ActionOnCoverPreferenceDialog.newInstance(prefKey).show(it.supportFragmentManager, "ACTION_DIALOG_$prefKey")
            }
        }
    )
}
