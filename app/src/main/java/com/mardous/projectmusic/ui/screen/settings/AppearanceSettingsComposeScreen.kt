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
import com.mardous.projectmusic.core.model.theme.EraFont
import com.mardous.projectmusic.core.model.theme.EraShapeFamily
import com.mardous.projectmusic.core.model.theme.EraSurfaceMaterial
import com.mardous.projectmusic.ui.component.compose.preferences.*
import com.mardous.projectmusic.util.*
import com.mardous.projectmusic.ui.component.preferences.dialog.AccentColorPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.CategoriesPreferenceDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsComposeScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onSwipeActionsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    
    // Bottom Sheet States
    var showDesignSystemSheet by remember { mutableStateOf(false) }
    var showWidgetSettingsSheet by remember { mutableStateOf(false) }
    var showTabModeDialog by remember { mutableStateOf(false) }

    if (showDesignSystemSheet) {
        DesignSystemBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showDesignSystemSheet = false }
        )
    }

    if (showWidgetSettingsSheet) {
        WidgetSettingsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showWidgetSettingsSheet = false }
        )
    }

    if (showTabModeDialog) {
        val tabEntries = context.resources.getStringArray(R.array.pref_tab_titles_mode_entries).toList()
        val tabValues = context.resources.getStringArray(R.array.pref_tab_titles_mode_values).toList()
        SingleChoiceDialog(
            title = stringResource(R.string.tab_mode_title),
            options = tabValues,
            selectedOption = uiState.tabTitlesMode,
            onOptionSelected = { viewModel.setTabTitlesMode(it) },
            onDismissRequest = { showTabModeDialog = false },
            optionTitle = { tabEntries[tabValues.indexOf(it)] }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.appearance_title)) },
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
            // --- PRIMARY THEME ---
            item {
                SegmentedPreferenceGroup {
                    val themeEntries = listOf(
                        stringResource(R.string.auto_theme_name),
                        stringResource(R.string.light_theme_name),
                        stringResource(R.string.dark_theme_name),
                        "Black"
                    )
                    val themeValues = listOf(GeneralTheme.AUTO, GeneralTheme.LIGHT, GeneralTheme.DARK, GeneralTheme.BLACK)
                    
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.general_theme_title),
                        options = themeEntries,
                        selectedIndex = themeValues.indexOf(uiState.generalTheme).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setGeneralTheme(themeValues[it])
                            (context as? FragmentActivity)?.recreate()
                        },
                        icon = R.drawable.ic_contrast_24dp
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.material_you_title),
                        summary = "Sync with system colors",
                        checked = uiState.materialYou,
                        onCheckedChange = { 
                            viewModel.setMaterialYou(it)
                            (context as? FragmentActivity)?.recreate()
                        },
                        icon = R.drawable.ic_palette_24dp
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.pure_black_theme_title),
                        summary = "Absolute black in dark mode",
                        checked = uiState.blackTheme,
                        onCheckedChange = {
                            viewModel.setBlackTheme(it)
                            (context as? FragmentActivity)?.recreate()
                        }
                    )
                }
            }

            // --- EXPRESSIVE ENGINE ---
            item { DashboardCategoryHeader("Visual Engine") }
            item {
                ExpressivePreferenceItem(
                    title = "Era Design System",
                    summary = "Advanced harmony, shapes, and motion",
                    icon = R.drawable.ic_tune_24dp,
                    onClick = { showDesignSystemSheet = true }
                )
            }

            // --- ACCENT & STYLE ---
            item { DashboardCategoryHeader("Accent & Style") }
            item {
                SegmentedPreferenceGroup {
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.accent_color_title),
                        summary = "Customize primary branding color",
                        icon = R.drawable.ic_palette_24dp,
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                AccentColorPreferenceDialog.newInstance(ACCENT_COLOR).show(it.supportFragmentManager, "ACCENT_COLOR")
                            }
                        }
                    )
                    
                    val uiStyleEntries = context.resources.getStringArray(R.array.pref_ui_theme_entries).toList()
                    val uiStyleValues = context.resources.getStringArray(R.array.pref_ui_theme_values).toList()

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.ui_theme_title),
                        options = uiStyleEntries,
                        selectedIndex = uiStyleValues.indexOf(uiState.uiTheme).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setUiTheme(uiStyleValues[it])
                            (context as? FragmentActivity)?.recreate()
                        }
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.use_custom_font_title),
                        checked = uiState.useCustomFont,
                        onCheckedChange = {
                            viewModel.setUseCustomFont(it)
                            (context as? FragmentActivity)?.recreate()
                        },
                        icon = R.drawable.ic_text_fields_24dp
                    )
                }
            }

            // --- LAYOUT ---
            item { DashboardCategoryHeader("Layout") }
            item {
                SegmentedPreferenceGroup {
                    val appBarEntries = context.resources.getStringArray(R.array.pref_appbar_mode_entries).toList()
                    val appBarValues = context.resources.getStringArray(R.array.pref_appbar_mode_values).toList()

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.appbar_mode_title),
                        options = appBarEntries,
                        selectedIndex = appBarValues.indexOf(uiState.appBarMode).coerceAtLeast(0),
                        onOptionSelected = { viewModel.setAppBarMode(appBarValues[it]) },
                        icon = R.drawable.ic_tab_24dp
                    )

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.tab_mode_title),
                        summary = uiState.tabTitlesMode.replaceFirstChar { it.uppercase() },
                        icon = R.drawable.ic_tab_24dp,
                        onClick = { showTabModeDialog = true }
                    )

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.library_categories_title),
                        summary = "Manage home screen tabs",
                        icon = R.drawable.ic_tune_24dp,
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                CategoriesPreferenceDialog().show(it.supportFragmentManager, "CATEGORIES")
                            }
                        }
                    )
                    ExpressiveSwitchItem(
                        title = "Horizontal Artist Albums",
                        checked = uiState.horizontalArtistAlbums,
                        onCheckedChange = { viewModel.setHorizontalArtistAlbums(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Compact Album Song View",
                        checked = uiState.compactAlbumSongView,
                        onCheckedChange = { viewModel.setCompactAlbumSongView(it) }
                    )
                    ExpressiveSwitchItem(
                        title = "Compact Artist Song View",
                        checked = uiState.compactArtistSongView,
                        onCheckedChange = { viewModel.setCompactArtistSongView(it) }
                    )
                }
            }

            // --- NAVIGATION & INTERACTION ---
            item { DashboardCategoryHeader("Interaction") }
            item {
                SegmentedPreferenceGroup {
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.swipe_actions_title),
                        summary = "Configure gestures for lists",
                        icon = R.drawable.ic_swipe_24dp,
                        onClick = onSwipeActionsClick
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.remember_last_page_title),
                        checked = uiState.rememberLastPage,
                        onCheckedChange = { viewModel.setRememberLastPage(it) }
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.hold_tab_to_search_title),
                        checked = uiState.holdTabToSearch,
                        onCheckedChange = { viewModel.setHoldTabToSearch(it) }
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.larger_header_image_title),
                        checked = uiState.largerHeaderImage,
                        onCheckedChange = { viewModel.setLargerHeaderImage(it) },
                        icon = R.drawable.ic_image_24dp
                    )
                }
            }

            // --- WIDGETS ---
            item { DashboardCategoryHeader(stringResource(R.string.widgets_header)) }
            item {
                ExpressivePreferenceItem(
                    title = "Widget Personalization",
                    summary = "Style and content for home screen widgets",
                    icon = R.drawable.ic_settings_applications_24dp,
                    onClick = { showWidgetSettingsSheet = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemBottomSheet(
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
                    "Design System Console",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                SegmentedPreferenceGroup {
                    ExpressiveSwitchItem(
                        title = stringResource(R.string.era_harmony_mode_title),
                        summary = stringResource(R.string.era_harmony_mode_summary),
                        checked = uiState.eraHarmonyMode,
                        onCheckedChange = { 
                            viewModel.setEraHarmonyMode(it)
                            (context as? FragmentActivity)?.recreate()
                        }
                    )
                    
                    DesignColorItem("Primary Seed", uiState.eraPrimarySeed) {
                        (context as? FragmentActivity)?.let {
                            AccentColorPreferenceDialog.newInstance(ERA_PRIMARY_SEED).show(it.supportFragmentManager, "PRIMARY_SEED")
                        }
                    }
                    
                    if (uiState.eraHarmonyMode) {
                        DesignColorItem("Secondary Seed", uiState.eraSecondarySeed) {
                            (context as? FragmentActivity)?.let {
                                AccentColorPreferenceDialog.newInstance(ERA_SECONDARY_SEED).show(it.supportFragmentManager, "SECONDARY_SEED")
                            }
                        }
                        DesignColorItem("Tertiary Seed", uiState.eraTertiarySeed) {
                            (context as? FragmentActivity)?.let {
                                AccentColorPreferenceDialog.newInstance(ERA_TERTIARY_SEED).show(it.supportFragmentManager, "TERTIARY_SEED")
                            }
                        }
                        DesignColorItem("Error Seed", uiState.eraErrorSeed) {
                            (context as? FragmentActivity)?.let {
                                AccentColorPreferenceDialog.newInstance(ERA_ERROR_SEED).show(it.supportFragmentManager, "ERROR_SEED")
                            }
                        }
                    }
                }
            }

            item {
                SegmentedPreferenceGroup {
                    val shapeEntries = context.resources.getStringArray(R.array.pref_era_shape_family_entries).toList()
                    val shapeValues = context.resources.getStringArray(R.array.pref_era_shape_family_values).toList()
                    
                    SegmentedPreferenceItem(
                        title = stringResource(R.string.era_shape_family_title),
                        options = shapeEntries,
                        selectedIndex = shapeValues.indexOf(uiState.eraShapeFamily.name).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setEraShapeFamily(EraShapeFamily.valueOf(shapeValues[it]))
                            (context as? FragmentActivity)?.recreate()
                        }
                    )

                    ExpressiveSliderItem(
                        title = stringResource(R.string.era_shape_scale_title),
                        value = uiState.eraShapeScale * 10f,
                        onValueChange = { viewModel.setEraShapeScale(it / 10f) },
                        valueRange = 0f..20f,
                        valueDisplay = { String.format("%.1f", it / 10f) }
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.era_asymmetric_shapes_title),
                        summary = stringResource(R.string.era_asymmetric_shapes_summary),
                        checked = uiState.eraAsymmetricShapes,
                        onCheckedChange = { 
                            viewModel.setEraAsymmetricShapes(it)
                            (context as? FragmentActivity)?.recreate()
                        }
                    )
                }
            }

            item {
                SegmentedPreferenceGroup {
                    ExpressiveSliderItem(
                        title = stringResource(R.string.era_type_scale_title),
                        value = uiState.eraTypeScale * 10f,
                        onValueChange = { viewModel.setEraTypeScale(it / 10f) },
                        valueRange = 5f..20f,
                        valueDisplay = { String.format("%.1f", it / 10f) }
                    )
                    
                    ExpressiveSliderItem(
                        title = stringResource(R.string.era_contrast_title),
                        value = (uiState.eraContrast * 10f) + 10f,
                        onValueChange = { viewModel.setEraContrast((it - 10f) / 10f) },
                        valueRange = 0f..20f,
                        valueDisplay = { String.format("%.1f", (it - 10f) / 10f) }
                    )

                    ExpressiveSliderItem(
                        title = "Vibrancy",
                        value = uiState.eraVibrancy * 10f,
                        onValueChange = { viewModel.setEraVibrancy(it / 10f) },
                        valueRange = 0f..20f,
                        valueDisplay = { String.format("%.1f", it / 10f) }
                    )
                }
            }

            item { DashboardCategoryHeader("Typography") }
            item {
                SegmentedPreferenceGroup {
                    val fontEntries = EraFont.entries.map { it.label }
                    val fontValues = EraFont.entries.map { it.name }
                    
                    SegmentedPreferenceItem(
                        title = "Font Family",
                        options = fontEntries,
                        selectedIndex = fontValues.indexOf(uiState.eraFontFamily.name).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setEraFontFamily(EraFont.valueOf(fontValues[it]))
                        },
                        icon = R.drawable.ic_text_fields_24dp
                    )
                    
                    ExpressiveSliderItem(
                        title = stringResource(R.string.era_type_scale_title),
                        value = uiState.eraTypeScale * 10f,
                        onValueChange = { viewModel.setEraTypeScale(it / 10f) },
                        valueRange = 5f..20f,
                        valueDisplay = { String.format("%.1f", it / 10f) }
                    )
                }
            }

            item {
                SegmentedPreferenceGroup {
                    val matEntries = context.resources.getStringArray(R.array.pref_era_surface_material_entries).toList()
                    val matValues = context.resources.getStringArray(R.array.pref_era_surface_material_values).toList()

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.era_surface_material_title),
                        options = matEntries,
                        selectedIndex = matValues.indexOf(uiState.eraSurfaceMaterial.name).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setEraSurfaceMaterial(EraSurfaceMaterial.valueOf(matValues[it]))
                            (context as? FragmentActivity)?.recreate()
                        }
                    )

                    val motionEntries = context.resources.getStringArray(R.array.pref_era_motion_intensity_entries).toList()
                    val motionValues = context.resources.getStringArray(R.array.pref_era_motion_intensity_values).toList()

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.era_motion_intensity_title),
                        options = motionEntries,
                        selectedIndex = motionValues.indexOf(uiState.eraMotionIntensity.toString()).coerceAtLeast(0),
                        onOptionSelected = { 
                            viewModel.setEraMotionIntensity(motionValues[it].toInt())
                            (context as? FragmentActivity)?.recreate()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsBottomSheet(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    "Widget Personalization",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                SegmentedPreferenceGroup {
                    val styleEntries = context.resources.getStringArray(R.array.widget_small_layout_style_entries).toList()
                    val styleValues = context.resources.getStringArray(R.array.widget_small_layout_style_values).toList()

                    SegmentedPreferenceItem(
                        title = stringResource(R.string.widget_small_layout_style_title),
                        options = styleEntries,
                        selectedIndex = styleValues.indexOf(uiState.widgetSmallLayoutStyle).coerceAtLeast(0),
                        onOptionSelected = { viewModel.setWidgetSmallLayoutStyle(styleValues[it]) }
                    )

                    ExpressiveSwitchItem(
                        title = stringResource(R.string.widget_dynamic_colors_title),
                        summary = stringResource(R.string.widget_dynamic_colors_summary),
                        checked = uiState.widgetDynamicColors,
                        onCheckedChange = { viewModel.setWidgetDynamicColors(it) }
                    )

                    ExpressiveSliderItem(
                        title = stringResource(R.string.widget_image_corner_radius_title),
                        value = uiState.widgetImageCornerRadius.toFloat(),
                        onValueChange = { viewModel.setWidgetImageCornerRadius(it.toInt()) },
                        valueRange = 8f..32f,
                        valueDisplay = { "${it.toInt()}dp" }
                    )
                    
                    ExpressivePreferenceItem(
                        title = stringResource(R.string.widget_third_line_title),
                        summary = uiState.widgetThirdLineContent.ifEmpty { "Select info line" },
                        onClick = {
                            (context as? FragmentActivity)?.let {
                                com.mardous.projectmusic.ui.component.preferences.dialog.ExtraInfoPreferenceDialog.appWidgets(it).show(it.supportFragmentManager, "WIDGET_INFO")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DesignColorItem(
    title: String,
    color: Int,
    onClick: () -> Unit
) {
    ExpressivePreferenceItem(
        title = title,
        trailingContent = {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = androidx.compose.ui.graphics.Color(color),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {}
        },
        onClick = onClick
    )
}
