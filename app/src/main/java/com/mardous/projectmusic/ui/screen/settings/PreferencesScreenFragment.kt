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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.screen.settings

import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import coil3.SingletonImageLoader
import com.google.android.material.color.DynamicColors
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.R
import com.mardous.projectmusic.coil.CoverProvider
import com.mardous.projectmusic.core.model.lyrics.LyricsViewSettings
import com.mardous.projectmusic.data.local.database.dao.InclExclDao
import com.mardous.projectmusic.data.model.network.ScrobblingService
import com.mardous.projectmusic.extensions.files.getFormattedFileName
import com.mardous.projectmusic.extensions.hasR
import com.mardous.projectmusic.extensions.hasS
import com.mardous.projectmusic.extensions.isTablet
import com.mardous.projectmusic.extensions.materialSharedAxis
import com.mardous.projectmusic.extensions.navigation.findActivityNavController
import com.mardous.projectmusic.extensions.requestContext
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.extensions.utilities.dateStr
import com.mardous.projectmusic.data.local.database.dao.PendingScrobbleDao
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.model.network.LoginState
import com.mardous.projectmusic.ui.component.preferences.ServiceHubPreference
import com.mardous.projectmusic.extensions.utilities.toEnum
import com.mardous.projectmusic.ui.component.preferences.ProgressIndicatorPreference
import com.mardous.projectmusic.ui.component.preferences.SwitchWithButtonPreference
import com.mardous.projectmusic.ui.component.preferences.ThemePreference
import com.mardous.projectmusic.ui.component.preferences.dialog.ActionOnCoverPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.CategoriesPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.ClearQueueActionPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.ExtraInfoPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.NowPlayingScreenPreferenceDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.SingleSelectionDialog
import com.mardous.projectmusic.ui.component.preferences.dialog.SongClickActionPreferenceDialog
import com.mardous.projectmusic.ui.dialogs.MultiCheckDialog
import com.mardous.projectmusic.ui.dialogs.library.BlacklistWhitelistDialog
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.screen.library.ReloadType
import com.mardous.projectmusic.ui.screen.scrobbling.ScrobblingServiceLoginFragment
import com.mardous.projectmusic.ui.screen.lyrics.LyricsViewModel
import com.mardous.projectmusic.ui.screen.update.UpdateSearchResult
import com.mardous.projectmusic.ui.screen.update.UpdateViewModel
import com.mardous.projectmusic.util.ADD_EXTRA_CONTROLS
import com.mardous.projectmusic.util.ACCENT_COLOR
import com.mardous.projectmusic.util.LYRICS_ACCENT_COLOR
import com.mardous.projectmusic.util.ERA_PRIMARY_SEED
import com.mardous.projectmusic.util.ERA_SECONDARY_SEED
import com.mardous.projectmusic.util.ERA_TERTIARY_SEED
import com.mardous.projectmusic.util.ERA_ERROR_SEED
import com.mardous.projectmusic.util.ERA_ASYMMETRIC_SHAPES
import com.mardous.projectmusic.util.ERA_CONTRAST
import com.mardous.projectmusic.util.ERA_HARMONY_MODE
import com.mardous.projectmusic.util.ERA_MOTION_INTENSITY
import com.mardous.projectmusic.util.ERA_SHAPE_FAMILY
import com.mardous.projectmusic.util.ERA_SHAPE_SCALE
import com.mardous.projectmusic.util.ERA_SURFACE_MATERIAL
import com.mardous.projectmusic.util.ERA_TYPE_SCALE
import com.mardous.projectmusic.ui.component.preferences.dialog.AccentColorPreferenceDialog
import com.mardous.projectmusic.util.BACKUP_DATA
import com.mardous.projectmusic.util.BLACKLIST_ENABLED
import com.mardous.projectmusic.util.BLACK_THEME
import com.mardous.projectmusic.util.BackupContent
import com.mardous.projectmusic.util.BackupHelper
import com.mardous.projectmusic.util.COVER_DOUBLE_TAP_ACTION
import com.mardous.projectmusic.util.COVER_LEFT_DOUBLE_TAP_ACTION
import com.mardous.projectmusic.util.COVER_LONG_PRESS_ACTION
import com.mardous.projectmusic.util.COVER_RIGHT_DOUBLE_TAP_ACTION
import com.mardous.projectmusic.util.COVER_SINGLE_TAP_ACTION
import com.mardous.projectmusic.util.ENABLE_ROTATION_LOCK
import com.mardous.projectmusic.util.GENERAL_THEME
import com.mardous.projectmusic.util.IGNORE_MEDIA_STORE
import com.mardous.projectmusic.util.LANGUAGE_NAME
import com.mardous.projectmusic.util.UI_THEME
import com.mardous.projectmusic.util.LASTFM_LOGIN
import com.mardous.projectmusic.util.LASTFM_SCROBBLE_ENABLED
import com.mardous.projectmusic.util.LASTFM_NOW_PLAYING_ENABLED
import com.mardous.projectmusic.util.LASTFM_OFFLINE_SCROBBLING
import com.mardous.projectmusic.util.LASTFM_SCROBBLE_PERCENTAGE
import com.mardous.projectmusic.util.LASTFM_SYNC_FAVORITES
import com.mardous.projectmusic.util.LISTENBRAINZ_SCROBBLE_ENABLED
import com.mardous.projectmusic.util.LISTENBRAINZ_NOW_PLAYING_ENABLED
import com.mardous.projectmusic.util.LAST_ADDED_CUTOFF
import com.mardous.projectmusic.util.LIBRARY_CATEGORIES
import com.mardous.projectmusic.util.LISTENBRAINZ_LOGIN
import com.mardous.projectmusic.data.model.network.NetworkFeature.Companion.LASTFM_INFO_ENABLED_KEY
import com.mardous.projectmusic.util.MATERIAL_YOU
import com.mardous.projectmusic.util.NOW_PLAYING_EXTRA_INFO
import com.mardous.projectmusic.util.NOW_PLAYING_SCREEN
import com.mardous.projectmusic.util.ON_CLEAR_QUEUE_ACTION
import com.mardous.projectmusic.util.ON_SONG_CLICK_ACTION
import com.mardous.projectmusic.util.PREFERRED_IMAGE_SIZE
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.RESTORE_DATA
import com.mardous.projectmusic.util.TRASH_MUSIC_FILES
import com.mardous.projectmusic.util.USE_CUSTOM_FONT
import com.mardous.projectmusic.util.USE_FOLDER_ART
import com.mardous.projectmusic.util.WHITELIST_ENABLED
import com.mardous.projectmusic.util.WIDGET_IMAGE_CORNER_RADIUS
import com.mardous.projectmusic.util.WIDGET_THIRD_LINE_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsDashboardFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    SettingsComposeScreen(
                        viewModel = viewModel,
                        navController = findNavController(),
                        onBackClick = { findActivityNavController(R.id.fragment_container).popBackStack() },
                        onAboutClick = { findActivityNavController(R.id.fragment_container).navigate(R.id.nav_about) },
                        onStatsClick = { findActivityNavController(R.id.fragment_container).navigate(R.id.nav_stats) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class AppearancePreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    AppearanceSettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() },
                        onSwipeActionsClick = { 
                            findNavController().navigate(R.id.action_to_swipeActionsPreferences)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class SwipeActionsPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    SwipeActionsSettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}


class NowPlayingPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    NowPlayingSettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class LyricsPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    private val importFontLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lyricsViewModel.importCustomFont(requireContext(), uri)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            requireContext().showToast(R.string.font_imported_successfully)
                        } else {
                            requireContext().showToast(R.string.could_not_import_font)
                        }
                    }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    LyricsSettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() },
                        onImportFontClick = {
                            importFontLauncher.launch(
                                arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class PlaybackPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    PlaybackSettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() },
                        onEqualizerClick = { findActivityNavController(R.id.fragment_container).navigate(R.id.nav_equalizer) },
                        onSoundSettingsClick = { findActivityNavController(R.id.fragment_container).navigate(R.id.nav_sound_settings) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class LibraryPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    LibrarySettingsComposeScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().popBackStack() },
                        onReloadSuggestions = { libraryViewModel.forceReload(ReloadType.Suggestions) },
                        onStartDirectoryClick = {
                            val fragment = com.mardous.projectmusic.ui.dialogs.library.FolderChooserDialog()
                            fragment.setCallback(object : com.mardous.projectmusic.ui.dialogs.library.FolderChooserDialog.FolderCallback {
                                override fun onFolderSelection(dialog: com.mardous.projectmusic.ui.dialogs.library.FolderChooserDialog, folder: java.io.File) {
                                    com.mardous.projectmusic.util.Preferences.startDirectory = folder
                                }
                            })
                            fragment.show(childFragmentManager, "START_DIR")
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

class AdvancedPreferencesFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModel()
    private val updateViewModel: UpdateViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    AdvancedSettingsComposeScreen(
                        viewModel = viewModel,
                        updateViewModel = updateViewModel,
                        onBackClick = { findNavController().popBackStack() },
                        onCheckForUpdates = { updateViewModel.searchForUpdate(true) },
                        onClearCache = { clearImageLoaderCache() },
                        highlightKey = arguments?.getString("highlightKey")
                    )
                }
            }
        }
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            com.mardous.projectmusic.coil.CoverProvider.clearCache(requireContext())

            val imageLoader = coil3.SingletonImageLoader.get(requireContext())
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            android.util.Log.e("Settings", "Failed to clear image loader cache", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}

open class PreferenceScreenFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    protected val libraryViewModel: LibraryViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val updateViewModel: UpdateViewModel by activityViewModel()

    private val importFontLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lyricsViewModel.importCustomFont(requireContext(), uri)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            showToast(R.string.font_imported_successfully)
                        } else {
                            showToast(R.string.could_not_import_font)
                        }
                    }
            }
        }

    private val createBackupLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/*")) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    BackupHelper.createBackup(requireContext(), uri)
                }
            }
        }

    private val selectBackupLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { selection ->
            if (selection != null) {
                val items = BackupContent.entries.map {
                    getString(it.titleRes)
                }
                val multiCheckDialog = MultiCheckDialog.Builder(requireContext())
                    .title(R.string.select_content_to_restore)
                    .items(items)
                    .createDialog { _, whichPos, _ ->
                        val content = BackupContent.entries.filterIndexed { i, _ ->
                            whichPos.contains(i)
                        }
                        lifecycleScope.launch {
                            BackupHelper.restoreBackup(requireContext(), selection, content)
                        }
                        true
                    }
                multiCheckDialog.show(childFragmentManager, "RESTORE_DIALOG")
            }
        }

    protected val preferences: SharedPreferences by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences.registerOnSharedPreferenceChangeListener(this)
        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            listView.updatePadding(bottom = it.getWithSpace())
        }
        setDivider(Color.TRANSPARENT.toDrawable())
        materialSharedAxis(view)
        preparePreferences()

        arguments?.getString("highlightKey")?.let { key ->
            arguments?.remove("highlightKey")
            view.postDelayed({
                scrollToPreference(key)
            }, 100)
        }
    }

    fun preparePreferences() {
        findPreference<Preference>("about")?.summary =
            getString(R.string.about_summary, BuildConfig.VERSION_NAME)

        findPreference<ThemePreference>(GENERAL_THEME)?.apply {
            customCallback = object : ThemePreference.Callback {
                override fun onThemeSelected(themeName: String) {
                    Preferences.generalTheme = themeName
                    setDefaultNightMode(Preferences.getDayNightMode(themeName))
                    restartActivity()
                }
            }
        }

        findPreference<Preference>(BLACK_THEME)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val themeName = Preferences.getGeneralTheme((newValue as Boolean))
                setDefaultNightMode(Preferences.getDayNightMode(themeName))
                requireActivity().recreate()
                true
            }
        }

        findPreference<Preference>(MATERIAL_YOU)?.apply {
            isVisible = hasS()
            setOnPreferenceChangeListener { _, newValue ->
                val activity = requireActivity()
                if (newValue as Boolean) {
                    DynamicColors.applyToActivityIfAvailable(activity)
                }
                activity.recreate()
                true
            }
        }

        findPreference<Preference>(USE_CUSTOM_FONT)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }

        findPreference<Preference>(UI_THEME)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }

        findPreference<Preference>(WIDGET_IMAGE_CORNER_RADIUS)?.isVisible = hasS()
        findPreference<Preference>(ADD_EXTRA_CONTROLS)?.isVisible = !resources.isTablet

        findPreference<ListPreference>(LyricsViewSettings.Key.BACKGROUND_EFFECT)?.apply {
            if (!hasS()) {
                val indexOfBlur = entryValues.indexOf("blur")
                entries = entries.filterIndexed { index, _ -> index != indexOfBlur }
                    .toTypedArray()
                entryValues = entryValues.filterIndexed { index, _ -> index != indexOfBlur }
                    .toTypedArray()
            }
        }

        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isVisible = hasS()

        findPreference<Preference>(LyricsViewSettings.Key.SELECTED_CUSTOM_FONT)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            importFontLauncher.launch(
                arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")
            )
            true
        }

        findPreference<Preference>("clear_lyrics")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lyricsViewModel.deleteLyrics()
            showToast(R.string.lyrics_cleared)
            true
        }

        if (!hasR()) {
            findPreference<Preference>(TRASH_MUSIC_FILES)?.isVisible = false
        }

        findPreference<Preference>(LAST_ADDED_CUTOFF)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                libraryViewModel.forceReload(ReloadType.Suggestions)
                true
            }

        findPreference<SwitchWithButtonPreference>(WHITELIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.WHITELIST)
                }
            })
        }

        findPreference<SwitchWithButtonPreference>(BLACKLIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.BLACKLIST)
                }
            })
        }

        findPreference<Preference>(IGNORE_MEDIA_STORE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(PREFERRED_IMAGE_SIZE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(USE_FOLDER_ART)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(LANGUAGE_NAME)?.setOnPreferenceChangeListener { _, newValue ->
            val languageTag = (newValue as? String)
            if (languageTag == null || languageTag == "auto") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            }
            true
        }

        findPreference<Preference>(BACKUP_DATA)?.setOnPreferenceClickListener {
            createBackupLauncher.launch(
                getFormattedFileName(
                    "Backup",
                    BackupHelper.BACKUP_EXTENSION
                )
            )
            true
        }

        findPreference<Preference>(RESTORE_DATA)?.setOnPreferenceClickListener {
            selectBackupLauncher.launch(arrayOf("application/*"))
            true
        }

        findPreference<Preference>(ENABLE_ROTATION_LOCK)?.isVisible = !resources.isTablet

        val updateSearchPreference = findPreference<ProgressIndicatorPreference>("search_for_update")
        if (updateSearchPreference != null) {
            updateSearchPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                updateViewModel.searchForUpdate(true)
                true
            }

            updateViewModel.updateEventObservable.observe(viewLifecycleOwner) {
                val result = it.peekContent()
                when (result.state) {
                    UpdateSearchResult.State.Searching -> {
                        updateSearchPreference.showProgressIndicator()
                        updateSearchPreference.isEnabled = false
                        updateSearchPreference.summary = getString(R.string.checking_please_wait)
                    }

                    UpdateSearchResult.State.Completed -> {
                        updateSearchState(updateSearchPreference, result.executedAtMillis)
                    }

                    UpdateSearchResult.State.Failed -> {
                        updateSearchState(updateSearchPreference, result.executedAtMillis)
                        if (result.wasFromUser) {
                            val error = result.error
                            val message = when (error) {
                                is java.io.IOException -> getString(R.string.update_error_network)
                                is IllegalStateException -> {
                                    val msg = error.message ?: ""
                                    if (msg.contains("rate limit", true)) {
                                        getString(R.string.update_error_api_limit)
                                    } else if (msg.contains("not configured", true)) {
                                        msg
                                    } else if (msg.contains("No suitable", true)) {
                                        getString(R.string.update_error_not_found)
                                    } else if (msg.contains("GitHub", true)) {
                                        msg
                                    } else {
                                        getString(R.string.could_not_check_for_updates_detailed, msg.ifBlank { "Unknown error" })
                                    }
                                }
                                else -> {
                                    val msg = error?.message
                                    if (msg.isNullOrBlank()) {
                                        getString(R.string.could_not_check_for_updates_detailed, error?.javaClass?.simpleName ?: "Unknown error")
                                    } else {
                                        getString(R.string.could_not_check_for_updates_detailed, msg)
                                    }
                                }
                            }
                            showToast(message)
                        }
                    }

                    else -> {
                        updateSearchState(updateSearchPreference, Preferences.lastUpdateSearch)
                    }
                }
            }
        }

        onUpdateNowPlayingScreen()

        // New Era Customization Listeners
        val eraKeys = arrayOf(
            ERA_HARMONY_MODE,
            ERA_SHAPE_FAMILY,
            ERA_SHAPE_SCALE,
            ERA_ASYMMETRIC_SHAPES,
            ERA_TYPE_SCALE,
            ERA_CONTRAST,
            ERA_SURFACE_MATERIAL,
            ERA_MOTION_INTENSITY
        )
        eraKeys.forEach { key ->
            findPreference<Preference>(key)?.setOnPreferenceChangeListener { _, _ ->
                restartActivity()
                true
            }
        }

        onUpdateCoverActions()
        onUpdateLyricsPreferences()
        onUpdateQueuePreferences()
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment = SingleSelectionDialog.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            val dialogFragment: DialogFragment? = when (preference.key) {
                ACCENT_COLOR,
                LYRICS_ACCENT_COLOR,
                ERA_PRIMARY_SEED,
                ERA_SECONDARY_SEED,
                ERA_TERTIARY_SEED,
                ERA_ERROR_SEED -> AccentColorPreferenceDialog.newInstance(preference.key)
                LIBRARY_CATEGORIES -> CategoriesPreferenceDialog()
                NOW_PLAYING_SCREEN -> NowPlayingScreenPreferenceDialog()
                NOW_PLAYING_EXTRA_INFO -> ExtraInfoPreferenceDialog.nowPlaying(requireContext())
                WIDGET_THIRD_LINE_CONTENT -> ExtraInfoPreferenceDialog.appWidgets(requireContext())
                ON_SONG_CLICK_ACTION -> SongClickActionPreferenceDialog()
                ON_CLEAR_QUEUE_ACTION -> ClearQueueActionPreferenceDialog()
                COVER_DOUBLE_TAP_ACTION,
                COVER_SINGLE_TAP_ACTION,
                COVER_LONG_PRESS_ACTION,
                COVER_LEFT_DOUBLE_TAP_ACTION,
                COVER_RIGHT_DOUBLE_TAP_ACTION -> ActionOnCoverPreferenceDialog.newInstance(preference.key)
                LASTFM_LOGIN -> ScrobblingServiceLoginFragment.create(ScrobblingService.Lastfm)
                LISTENBRAINZ_LOGIN -> ScrobblingServiceLoginFragment.create(ScrobblingService.ListenBrainz)
                else -> {
                    if (preference.key.startsWith("swipe_action_")) {
                        com.mardous.projectmusic.ui.component.preferences.dialog.SwipeActionPreferenceDialog.newInstance(preference.key)
                    } else {
                        null
                    }
                }
            }
            if (dialogFragment != null) {
                dialogFragment.show(childFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val settingsScreen = preference.key.toEnum<SettingsScreen>()
        return when {
            settingsScreen != null -> {
                findNavController().navigate(settingsScreen.navAction)
                true
            }
            preference.key == "stats" -> {
                findActivityNavController(R.id.fragment_container).navigate(R.id.nav_stats)
                true
            }
            preference.key == "about" -> {
                findActivityNavController(R.id.fragment_container).navigate(R.id.nav_about)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            NOW_PLAYING_SCREEN -> onUpdateNowPlayingScreen()
            COVER_DOUBLE_TAP_ACTION,
            COVER_LEFT_DOUBLE_TAP_ACTION,
            COVER_RIGHT_DOUBLE_TAP_ACTION,
            COVER_LONG_PRESS_ACTION -> onUpdateCoverActions()
            ON_SONG_CLICK_ACTION,
            ON_CLEAR_QUEUE_ACTION -> onUpdateQueuePreferences()
            LyricsViewSettings.Key.BACKGROUND_EFFECT -> onUpdateLyricsPreferences()
        }
    }

    private fun onUpdateNowPlayingScreen() {
        findPreference<Preference>(NOW_PLAYING_SCREEN)?.summary =
            getString(Preferences.nowPlayingScreen.titleRes)
    }

    private fun onUpdateCoverActions() {
        findPreference<Preference>(COVER_SINGLE_TAP_ACTION)?.summary =
            getString(Preferences.coverSingleTapAction.titleRes)

        findPreference<Preference>(COVER_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LEFT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverLeftDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_RIGHT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverRightDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LONG_PRESS_ACTION)?.summary =
            getString(Preferences.coverLongPressAction.titleRes)
    }

    private fun onUpdateLyricsPreferences() {
        val hasBackgroundEffects =
            preferences.getString(LyricsViewSettings.Key.BACKGROUND_EFFECT, "none") != "none"
        findPreference<Preference>(LyricsViewSettings.Key.SHADOW_EFFECT)
            ?.isEnabled = hasBackgroundEffects
        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isEnabled = hasBackgroundEffects
    }

    private fun onUpdateQueuePreferences() {
        findPreference<Preference>(ON_SONG_CLICK_ACTION)
            ?.summary = getString(Preferences.songClickAction.titleRes)
        findPreference<Preference>(ON_CLEAR_QUEUE_ACTION)
            ?.summary = getString(Preferences.clearQueueAction.titleRes)
    }

    private fun showLibraryFolderSelector(type: Int) {
        BlacklistWhitelistDialog.newInstance(type).show(childFragmentManager, "LIBRARY_PATHS_PREFERENCE")
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            CoverProvider.clearCache(requireContext())

            val imageLoader = SingletonImageLoader.get(requireContext())
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            Log.e("Settings", "Failed to clear image loader cache", e)
        }
    }

    private fun updateSearchState(preference: ProgressIndicatorPreference?, lastUpdateSearch: Long) {
        requestContext {
            preference?.hideProgressIndicator()
            preference?.isEnabled = true
            preference?.summary = getString(R.string.last_update_search_x, it.dateStr(lastUpdateSearch))
        }
    }

    private fun restartActivity() {
        activity?.recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
