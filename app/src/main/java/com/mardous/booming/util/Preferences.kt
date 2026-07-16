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

package com.mardous.booming.util

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.LabelVisibility
import com.mardous.booming.R
import com.mardous.booming.core.model.CategoryInfo
import com.mardous.booming.core.model.Cutoff
import com.mardous.booming.core.model.action.FolderAction
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.action.QueueClearingBehavior
import com.mardous.booming.core.model.action.SongClickBehavior
import com.mardous.booming.core.model.player.MetadataField
import com.mardous.booming.core.model.player.PlayerColorSchemeMode
import com.mardous.booming.core.model.player.PlayerTransition
import com.mardous.booming.core.model.shuffle.GroupShuffleMode
import com.mardous.booming.core.model.theme.AppTheme
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.core.model.theme.VibrantBackgroundMode
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.intRes
import com.mardous.booming.extensions.utilities.calendarSingleton
import com.mardous.booming.extensions.utilities.deserialize
import com.mardous.booming.extensions.utilities.getCutoffTimeMillis
import com.mardous.booming.extensions.utilities.serialize
import com.mardous.booming.extensions.utilities.toEnum
import com.mardous.booming.ui.component.views.TopAppBarLayout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
object Preferences : KoinComponent {

    private val preferences: SharedPreferences by inject()

    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun getGeneralTheme(isBlackMode: Boolean): String {
        return if (isBlackMode) {
            GeneralTheme.BLACK
        } else {
            preferences.requireString(GENERAL_THEME, GeneralTheme.AUTO)
        }
    }

    var generalTheme: String
        get() = getGeneralTheme(blackTheme)
        set(value) = preferences.edit { putString(GENERAL_THEME, value) }

    fun getThemeMode(themeName: String) = when (themeName) {
        GeneralTheme.LIGHT -> AppTheme.Mode.Light
        GeneralTheme.DARK -> AppTheme.Mode.Dark
        GeneralTheme.BLACK -> AppTheme.Mode.Black
        else -> AppTheme.Mode.FollowSystem
    }

    fun getDayNightMode(themeName: String = generalTheme) = when (themeName) {
        GeneralTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        GeneralTheme.DARK,
        GeneralTheme.BLACK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> if (hasQ()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
    }

    val blackTheme: Boolean
        get() = preferences.getBoolean(BLACK_THEME, false)

    var uiTheme: String
        get() = preferences.requireString(UI_THEME, UITheme.MATERIAL)
        set(value) = preferences.edit { putString(UI_THEME, value) }

    var accentColor: Int?
        get() = preferences.getInt(ACCENT_COLOR, 0).takeIf { it != 0 }
        set(value) = preferences.edit { 
            if (value != null) putInt(ACCENT_COLOR, value) else remove(ACCENT_COLOR) 
        }

    var lyricsAccentColor: Int?
        get() = preferences.getInt(LYRICS_ACCENT_COLOR, 0).takeIf { it != 0 }
        set(value) = preferences.edit { 
            if (value != null) putInt(LYRICS_ACCENT_COLOR, value) else remove(LYRICS_ACCENT_COLOR) 
        }

    val isMaterialYouTheme: Boolean
        get() = preferences.getBoolean(MATERIAL_YOU, hasS())

    val isCustomFont: Boolean
        get() = preferences.getBoolean(USE_CUSTOM_FONT, true)

    val appBarMode: TopAppBarLayout.AppBarMode
        get() = when (preferences.requireString(APPBAR_MODE, AppBarMode.COMPACT)) {
            AppBarMode.COMPACT -> TopAppBarLayout.AppBarMode.SIMPLE
            AppBarMode.EXPANDED -> TopAppBarLayout.AppBarMode.COLLAPSING
            else -> TopAppBarLayout.AppBarMode.SIMPLE
        }

    var libraryCategories: List<CategoryInfo>
        get() = preferences.nullString(LIBRARY_CATEGORIES).deserialize(
            getDefaultLibraryCategoryInfos()
        )
        set(categories) = preferences.edit { putString(LIBRARY_CATEGORIES, categories.serialize()) }

    fun getDefaultLibraryCategoryInfos() =
        CategoryInfo.Category.entries.mapIndexed { index, category ->
            CategoryInfo(category, index < CategoryInfo.MAX_VISIBLE_CATEGORIES)
        }

    val isRememberLastPage: Boolean
        get() = preferences.getBoolean(REMEMBER_LAST_PAGE, true)

    var lastPage: Int
        get() = preferences.getInt(LAST_PAGE, 0)
        set(value) = preferences.edit { putInt(LAST_PAGE, value) }

    @LabelVisibility
    val bottomTitlesMode: Int
        get() = when (preferences.nullString(TAB_TITLES_MODE)) {
            BottomTitlesMode.SELECTED -> NavigationBarView.LABEL_VISIBILITY_SELECTED
            BottomTitlesMode.LABELED -> NavigationBarView.LABEL_VISIBILITY_LABELED
            BottomTitlesMode.UNLABELED -> NavigationBarView.LABEL_VISIBILITY_UNLABELED
            else -> NavigationBarView.LABEL_VISIBILITY_SELECTED
        }

    val holdTabToSearch: Boolean
        get() = preferences.getBoolean(HOLD_TAB_TO_SEARCH, true)

    var lockedPlaylists: Boolean
        get() = preferences.getBoolean(LOCKED_PLAYLISTS, false)
        set(value) = preferences.edit { putBoolean(LOCKED_PLAYLISTS, value) }

    val lastfmSyncFavorites: Boolean
        get() = preferences.getBoolean(LASTFM_SYNC_FAVORITES, false)

    fun setLastfmSyncFavorites(value: Boolean) = preferences.edit {
        putBoolean(LASTFM_SYNC_FAVORITES, value)
    }

    val lastfmOfflineScrobbling: Boolean
        get() = preferences.getBoolean(LASTFM_OFFLINE_SCROBBLING, true)

    fun setLastfmOfflineScrobbling(value: Boolean) = preferences.edit {
        putBoolean(LASTFM_OFFLINE_SCROBBLING, value)
    }

    val lastfmScrobblePercentage: Int
        get() = preferences.getInt(LASTFM_SCROBBLE_PERCENTAGE, 50)

    fun setLastfmScrobblePercentage(value: Int) = preferences.edit {
        putInt(LASTFM_SCROBBLE_PERCENTAGE, value)
    }

    fun setLastfmScrobblingEnabled(value: Boolean) = preferences.edit {
        putBoolean(LASTFM_SCROBBLE_ENABLED, value)
    }

    fun setListenBrainzScrobblingEnabled(value: Boolean) = preferences.edit {
        putBoolean(LISTENBRAINZ_SCROBBLE_ENABLED, value)
    }

    var queueHeight: Boolean
        get() = preferences.getBoolean(QUEUE_HEIGHT, false)
        set(value) = preferences.edit { putBoolean(QUEUE_HEIGHT, value) }

    val largerHeaderImage: Boolean
        get() = preferences.getBoolean(LARGER_HEADER_IMAGE, false)

    var horizontalArtistAlbums: Boolean
        get() = preferences.getBoolean(HORIZONTAL_ARTIST_ALBUMS, true)
        set(value) = preferences.edit { putBoolean(HORIZONTAL_ARTIST_ALBUMS, value) }

    var compactAlbumSongView: Boolean
        get() = preferences.getBoolean(COMPACT_ALBUM_SONG_VIEW, false)
        set(value) = preferences.edit { putBoolean(COMPACT_ALBUM_SONG_VIEW, value) }

    var compactArtistSongView: Boolean
        get() = preferences.getBoolean(COMPACT_ARTIST_SONG_VIEW, false)
        set(value) = preferences.edit { putBoolean(COMPACT_ARTIST_SONG_VIEW, value) }


    // New Era Tokens
    var eraPrimarySeed: Int
        get() = preferences.getInt(ERA_PRIMARY_SEED, 0xFF4D5C92.toInt()) // primaryLight
        set(value) = preferences.edit { putInt(ERA_PRIMARY_SEED, value) }

    var eraSecondarySeed: Int
        get() = preferences.getInt(ERA_SECONDARY_SEED, 0xFF595D72.toInt()) // secondaryLight
        set(value) = preferences.edit { putInt(ERA_SECONDARY_SEED, value) }

    var eraTertiarySeed: Int
        get() = preferences.getInt(ERA_TERTIARY_SEED, 0xFF75546F.toInt()) // tertiaryLight
        set(value) = preferences.edit { putInt(ERA_TERTIARY_SEED, value) }

    var eraErrorSeed: Int
        get() = preferences.getInt(ERA_ERROR_SEED, 0xFFBA1A1A.toInt()) // errorLight
        set(value) = preferences.edit { putInt(ERA_ERROR_SEED, value) }

    var eraHarmonyMode: Boolean
        get() = preferences.getBoolean(ERA_HARMONY_MODE, true)
        set(value) = preferences.edit { putBoolean(ERA_HARMONY_MODE, value) }

    var eraVibrancy: Float
        get() = preferences.getFloat(ERA_VIBRANCY, 1.0f)
        set(value) = preferences.edit { putFloat(ERA_VIBRANCY, value) }

    var eraContrast: Float
        get() = (preferences.getInt(ERA_CONTRAST, 0) - 10) / 10.0f
        set(value) = preferences.edit { putInt(ERA_CONTRAST, (value * 10).toInt() + 10) }

    var eraShapeScale: Float
        get() = preferences.getInt(ERA_SHAPE_SCALE, 10) / 10.0f
        set(value) = preferences.edit { putInt(ERA_SHAPE_SCALE, (value * 10).toInt()) }

    var eraShapeFamily: EraShapeFamily
        get() = preferences.enumValue(ERA_SHAPE_FAMILY, EraShapeFamily.ROUNDED)
        set(value) = preferences.edit { putString(ERA_SHAPE_FAMILY, value.name) }

    var eraAsymmetricShapes: Boolean
        get() = preferences.getBoolean(ERA_ASYMMETRIC_SHAPES, false)
        set(value) = preferences.edit { putBoolean(ERA_ASYMMETRIC_SHAPES, value) }

    var eraTypeScale: Float
        get() = preferences.getInt(ERA_TYPE_SCALE, 10) / 10.0f
        set(value) = preferences.edit { putInt(ERA_TYPE_SCALE, (value * 10).toInt()) }

    var eraMotionIntensity: Int
        get() = preferences.getString(ERA_MOTION_INTENSITY, "1")?.toIntOrNull() ?: 1 // 0: None, 1: Fluid, 2: Expressive
        set(value) = preferences.edit { putString(ERA_MOTION_INTENSITY, value.toString()) }

    var eraSurfaceMaterial: EraSurfaceMaterial
        get() = preferences.enumValue(ERA_SURFACE_MATERIAL, EraSurfaceMaterial.SOLID)
        set(value) = preferences.edit { putString(ERA_SURFACE_MATERIAL, value.name) }

    var eraAdaptiveIconography: Boolean
        get() = preferences.getBoolean(ERA_ADAPTIVE_ICONOGRAPHY, true)
        set(value) = preferences.edit { putBoolean(ERA_ADAPTIVE_ICONOGRAPHY, value) }

    var nowPlayingScreen: NowPlayingScreen
        get() = preferences.enumValue(NOW_PLAYING_SCREEN, NowPlayingScreen.Default)
        set(value) = preferences.edit { putString(NOW_PLAYING_SCREEN, value.name) }

    var vibrantBackgroundMode: VibrantBackgroundMode
        get() = preferences.enumValue(VIBRANT_BACKGROUND_MODE, VibrantBackgroundMode.Gradient)
        set(value) = preferences.edit { putString(VIBRANT_BACKGROUND_MODE, value.name) }

    var vibrantBackgroundAnimations: Boolean
        get() = preferences.getBoolean(VIBRANT_BACKGROUND_ANIMATIONS, true)
        set(value) = preferences.edit { putBoolean(VIBRANT_BACKGROUND_ANIMATIONS, value) }

    var vibrantBackgroundHighQuality: Boolean
        get() = preferences.getBoolean(VIBRANT_BACKGROUND_HIGH_QUALITY, true)
        set(value) = preferences.edit { putBoolean(VIBRANT_BACKGROUND_HIGH_QUALITY, value) }

    var vibrantBackgroundGlobal: Boolean
        get() = preferences.getBoolean(VIBRANT_BACKGROUND_GLOBAL, false)
        set(value) = preferences.edit { putBoolean(VIBRANT_BACKGROUND_GLOBAL, value) }

    val extraControls: Boolean
        get() = preferences.getBoolean(ADD_EXTRA_CONTROLS, false)

    val adaptiveControls: Boolean
        get() = preferences.getBoolean(ADAPTIVE_CONTROLS, false)

    val squigglySeekBar: Boolean
        get() = preferences.getBoolean(SQUIGGLY_SEEK_BAR, false)

    val swipeDownToDismiss: Boolean
        get() = preferences.getBoolean(SWIPE_DOWN_TO_DISMISS, false)

    var showLyricsOnCover: Boolean
        get() = preferences.getBoolean(LYRICS_ON_COVER, false)
        set(value) = preferences.edit { putBoolean(LYRICS_ON_COVER, value) }

    val swipeOnCover: Boolean
        get() = preferences.getBoolean(SWIPE_ON_COVER, true)

    val miniPlayerSwipeToSkip: Boolean
        get() = preferences.getBoolean(MINI_PLAYER_SWIPE_TO_SKIP, true)

    var isQueueLocked: Boolean
        get() = preferences.getBoolean(LOCKED_QUEUE, false)
        set(value) = preferences.edit { putBoolean(LOCKED_QUEUE, value) }

    fun getNowPlayingColorSchemeKey(nps: NowPlayingScreen) =
        "player_${nps.name.lowercase()}_color_scheme"

    fun getNowPlayingColorSchemeMode(nps: NowPlayingScreen): PlayerColorSchemeMode {
        val defaultScheme = nps.defaultColorScheme
        val schemeName = preferences.nullString(getNowPlayingColorSchemeKey(nps))
            ?: defaultScheme.name
        if (nps.supportedColorSchemes.any { it.name == schemeName }) {
            return schemeName.toEnum<PlayerColorSchemeMode>() ?: defaultScheme
        }
        return defaultScheme
    }

    fun setNowPlayingColorSchemeMode(nps: NowPlayingScreen, schemeMode: PlayerColorSchemeMode) {
        val schemeName = schemeMode.name
        if (nps.supportedColorSchemes.any { it.name == schemeName }) {
            preferences.edit {
                putString(getNowPlayingColorSchemeKey(nps), schemeName)
            }
        }
    }

    val isSmallImage: Boolean
        get() = preferences.getBoolean(NOW_PLAYING_SMALL_IMAGE, false)

    fun getNowPlayingImageCornerRadius(context: Context): Int =
        preferences.getInt(NOW_PLAYING_IMAGE_CORNER_RADIUS, context.intRes(R.integer.now_playing_corner_radius))

    fun getNowPlayingLyricsCornerRadius(context: Context): Int =
        preferences.getInt(LYRICS_CARD_CORNER_RADIUS, 32)

    var nowPlayingLyricsCornerRadius: Int
        get() = preferences.getInt(LYRICS_CARD_CORNER_RADIUS, 32)
        set(value) = preferences.edit { putInt(LYRICS_CARD_CORNER_RADIUS, value) }

    val isCarouselEffect: Boolean
        get() = preferences.getBoolean(CAROUSEL_EFFECT, false)

    fun getNowPlayingTransitionKey(nps: NowPlayingScreen) =
        "player_${nps.name.lowercase()}_transition"

    fun getNowPlayingTransition(nps: NowPlayingScreen): PlayerTransition {
        val defaultTransition = nps.defaultTransition
        val transitionName = preferences.nullString(getNowPlayingTransitionKey(nps))
            ?: defaultTransition.name
        if (nps.supportedTransitions.any { it.name == transitionName }) {
            return transitionName.toEnum<PlayerTransition>() ?: defaultTransition
        }
        return defaultTransition
    }

    fun setNowPlayingTransition(nps: NowPlayingScreen, transition: PlayerTransition) {
        if (nps.supportedTransitions.contains(transition)) {
            preferences.edit {
                putString(getNowPlayingTransitionKey(nps), transition.name)
            }
        }
    }

    val coverSingleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_SINGLE_TAP_ACTION, NowPlayingAction.TogglePlayState)

    val coverDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_DOUBLE_TAP_ACTION, NowPlayingAction.WebSearch)

    val coverLeftDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_LEFT_DOUBLE_TAP_ACTION, NowPlayingAction.SeekBackward)

    val coverRightDoubleTapAction: NowPlayingAction
        get() = preferences.enumValue(COVER_RIGHT_DOUBLE_TAP_ACTION, NowPlayingAction.SeekForward)

    val coverLongPressAction: NowPlayingAction
        get() = preferences.enumValue(COVER_LONG_PRESS_ACTION, NowPlayingAction.SleepTimer)

    val animateControls: Boolean
        get() = preferences.getBoolean(ANIMATE_PLAYER_CONTROL, true)

    val circularPlayButton: Boolean
        get() = preferences.getBoolean(CIRCLE_PLAY_BUTTON, false)

    val enableScrollingText: Boolean
        get() = preferences.getBoolean(ENABLE_SCROLLING_TEXT, false)

    val displayAlbumTitle
        get() = preferences.getBoolean(DISPLAY_ALBUM_TITLE, true)

    val displayExtraInfo: Boolean
        get() = preferences.getBoolean(DISPLAY_EXTRA_INFO, false)

    fun getExtraInfoContent(key: String, defaultContent: List<MetadataField>) =
        preferences.nullString(key).deserialize(defaultContent)

    fun setExtraInfoContent(key: String, newContent: List<MetadataField>) {
        preferences.edit { putString(key, newContent.serialize()) }
    }

    fun getDefaultNowPlayingInfo(): List<MetadataField> =
        MetadataField.Content.entries.map { content ->
            MetadataField(
                content,
                content == MetadataField.Content.Format ||
                        content == MetadataField.Content.Bitrate ||
                        content == MetadataField.Content.SampleRate
            )
        }

    fun getDefaultWidgetInfo(): List<MetadataField> =
        MetadataField.Content.entries.map { tag ->
            MetadataField(tag, tag == MetadataField.Content.Album)
        }

    var preferRemainingTime: Boolean
        get() = preferences.getBoolean(PREFER_REMAINING_TIME, false)
        set(value) = preferences.edit { putBoolean(PREFER_REMAINING_TIME, value) }

    var preferAlbumArtistName: Boolean
        get() = preferences.getBoolean(PREFER_ALBUM_ARTIST_NAME, false)
        set(value) = preferences.edit { putBoolean(PREFER_ALBUM_ARTIST_NAME, value) }

    var clearQueueAction: QueueClearingBehavior
        get() = preferences.enumValueByOrdinal(ON_CLEAR_QUEUE_ACTION, QueueClearingBehavior.RemoveAllSongs)
        set(value) = preferences.edit { putInt(ON_CLEAR_QUEUE_ACTION, value.ordinal) }

    var songClickAction: SongClickBehavior
        get() = preferences.enumValueByOrdinal(ON_SONG_CLICK_ACTION, SongClickBehavior.PlayWholeList)
        set(value) = preferences.edit { putInt(ON_SONG_CLICK_ACTION, value.ordinal) }

    val playOptionAlwaysVisible: Boolean
        get() = preferences.getBoolean(PLAY_OPTION_ALWAYS_VISIBLE, false)

    val playOptionClickBehavior: SongClickBehavior
        get() = if (preferences.getBoolean(PLAY_OPTION_PLAYS_WHOLE_LIST, false)) {
            SongClickBehavior.PlayWholeList
        } else {
            SongClickBehavior.PlayOnlyThisSong
        }

    val playAllSongsWhenSearching: Boolean
        get() = preferences.getBoolean(PLAY_ALL_SONGS_WHEN_SEARCHING, false)

    val albumShuffleMode: GroupShuffleMode
        get() = getGroupShuffleMode(ALBUM_SHUFFLE_MODE, SelectedShuffleMode.SHUFFLE_ALBUMS)

    val artistShuffleMode: GroupShuffleMode
        get() = getGroupShuffleMode(ARTIST_SHUFFLE_MODE, SelectedShuffleMode.SHUFFLE_ALL)

    private fun getGroupShuffleMode(key: String, default: String) =
        when(preferences.requireString(key, default)) {
            SelectedShuffleMode.SHUFFLE_ARTISTS,
            SelectedShuffleMode.SHUFFLE_ALBUMS -> GroupShuffleMode.ByGroup
            SelectedShuffleMode.SHUFFLE_SONGS -> GroupShuffleMode.BySong
            else -> GroupShuffleMode.FullRandom
        }

    fun isResumeOnConnect(bluetooth: Boolean) = when {
        bluetooth -> preferences.getBoolean(RESUME_ON_BLUETOOTH_CONNECT, false)
        else -> preferences.getBoolean(RESUME_ON_CONNECT, false)
    }

    fun isPauseOnDisconnect(bluetooth: Boolean) = when {
        bluetooth -> preferences.getBoolean(PAUSE_ON_BLUETOOTH_DISCONNECT, false)
        else -> preferences.getBoolean(PAUSE_ON_DISCONNECT, false)
    }

    var onlyAlbumArtists: Boolean
        get() = preferences.getBoolean(ONLY_ALBUM_ARTISTS, true)
        set(value) = preferences.edit { putBoolean(ONLY_ALBUM_ARTISTS, value) }

    val trashMusicFiles: Boolean
        get() = preferences.getBoolean(TRASH_MUSIC_FILES, false)

    val recursiveFolderActions: Set<FolderAction>
        get() {
            val notNullSet = mutableSetOf<FolderAction>()
            preferences.getStringSet(RECURSIVE_FOLDER_ACTIONS, null)
                ?.mapNotNullTo(notNullSet) { string ->
                    FolderAction.entries.firstOrNull { it.preferenceValue == string }
                }
            return notNullSet
        }

    fun getLastAddedCutoff(context: Context): Cutoff =
        getCutoff(context, LAST_ADDED_CUTOFF, true)

    fun getHistoryCutoff(context: Context): Cutoff =
        getCutoff(context, HISTORY_CUTOFF)

    private fun getCutoff(
        context: Context,
        preferenceKey: String,
        asSeconds: Boolean = false
    ): Cutoff {
        val cutoff = preferences.requireString(preferenceKey, "")
        val description = when (cutoff) {
            PlaylistCutoff.TODAY -> context.getString(R.string.today)
            PlaylistCutoff.YESTERDAY -> context.getString(R.string.yesterday)
            PlaylistCutoff.THIS_WEEK -> context.getString(R.string.this_week)
            PlaylistCutoff.PAST_THREE_MONTHS -> context.getString(R.string.past_three_months)
            PlaylistCutoff.THIS_YEAR -> context.getString(R.string.this_year)
            PlaylistCutoff.THIS_MONTH -> context.getString(R.string.this_month)
            else -> context.getString(R.string.this_month)
        }
        val interval = calendarSingleton.getCutoffTimeMillis(cutoff).let { cutoffTimeMillis ->
            if (asSeconds) cutoffTimeMillis / 1000 else cutoffTimeMillis
        }
        return Cutoff(description, interval)
    }

    var ignoreSingles: Boolean
        get() = preferences.getBoolean(IGNORE_SINGLES, false)
        set(value) = preferences.edit { putBoolean(IGNORE_SINGLES, value) }

    var showAlbumDuration: Boolean
        get() = preferences.getBoolean(SHOW_TOTAL_DURATION, false)
        set(value) = preferences.edit { putBoolean(SHOW_TOTAL_DURATION, value) }

    val whitelistEnabled: Boolean
        get() = preferences.getBoolean(WHITELIST_ENABLED, true)

    var blacklistEnabled: Boolean
        get() = preferences.getBoolean(BLACKLIST_ENABLED, true)
        set(value) = preferences.edit { putBoolean(BLACKLIST_ENABLED, value) }

    val minimumSongCountForArtist: Int
        get() = preferences.getInt(ARTIST_MINIMUM_SONGS, 1)

    val minimumSongCountForAlbum: Int
        get() = preferences.getInt(ALBUM_MINIMUM_SONGS, 1)

    val minimumSongDuration: Int
        get() = preferences.getInt(MINIMUM_SONG_DURATION, 15)

    val rotationLockEnabled: Boolean
        get() = preferences.getBoolean(ENABLE_ROTATION_LOCK, false)

    val updateSearchMode: String
        get() = preferences.requireString(UPDATE_SEARCH_MODE, UpdateSearchMode.WEEKLY)

    val experimentalUpdates: Boolean
        get() = preferences.getBoolean(EXPERIMENTAL_UPDATES, false)

    var lastUpdateSearch: Long
        get() = preferences.getLong(LAST_UPDATE_SEARCH, -1)
        set(value) = preferences.edit { putLong(LAST_UPDATE_SEARCH, value) }

    var lastUpdateId: Long
        get() = preferences.getLong(LAST_UPDATE_ID, -1)
        set(value) = preferences.edit { putLong(LAST_UPDATE_ID, value) }

    var hierarchyFolderView: Boolean
        get() = preferences.getBoolean(HIERARCHY_FOLDER_VIEW, false)
        set(value) = preferences.edit { putBoolean(HIERARCHY_FOLDER_VIEW, value) }

    var startDirectory: File
        get() = File(preferences.requireString(START_DIRECTORY, FileUtil.getDefaultStartDirectory().path))
        set(file) = preferences.edit { putString(START_DIRECTORY, file.getCanonicalPathSafe()) }

    var savedArtworkCopyrightNoticeShown: Boolean
        get() = preferences.getBoolean(SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN, false)
        set(value) = preferences.edit { putBoolean(SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN, value) }

    var initializedBlacklist: Boolean
        get() = preferences.getBoolean(INITIALIZED_BLACKLIST, false)
        set(value) = preferences.edit { putBoolean(INITIALIZED_BLACKLIST, value) }

    var isSwipeAnywhere: Boolean
        get() = preferences.getBoolean(SWIPE_ANYWHERE, false)
        set(value) = preferences.edit { putBoolean(SWIPE_ANYWHERE, value) }

    var isSwipeUpQueue: Boolean
        get() = preferences.getBoolean(SWIPE_UP_QUEUE, false)
        set(value) = preferences.edit { putBoolean(SWIPE_UP_QUEUE, value) }

    var isShowNextSong: Boolean
        get() = preferences.getBoolean(DISPLAY_NEXT_SONG, true)
        set(value) = preferences.edit { putBoolean(DISPLAY_NEXT_SONG, value) }

    fun getSwipeLeftAction(context: com.mardous.booming.core.model.swipe.SwipeContext): com.mardous.booming.core.model.swipe.SwipeAction {
        val key = "${SWIPE_ACTION_LEFT_PREFIX}_${context.name.lowercase()}"
        return preferences.enumValue(key, com.mardous.booming.core.model.swipe.SwipeAction.NONE)
    }

    fun getSwipeRightAction(context: com.mardous.booming.core.model.swipe.SwipeContext): com.mardous.booming.core.model.swipe.SwipeAction {
        val key = "${SWIPE_ACTION_RIGHT_PREFIX}_${context.name.lowercase()}"
        return preferences.enumValue(key, com.mardous.booming.core.model.swipe.SwipeAction.NONE)
    }

    fun setSwipeLeftAction(context: com.mardous.booming.core.model.swipe.SwipeContext, action: com.mardous.booming.core.model.swipe.SwipeAction) {
        val key = "${SWIPE_ACTION_LEFT_PREFIX}_${context.name.lowercase()}"
        preferences.edit { putString(key, action.name) }
    }

    fun setSwipeRightAction(context: com.mardous.booming.core.model.swipe.SwipeContext, action: com.mardous.booming.core.model.swipe.SwipeAction) {
        val key = "${SWIPE_ACTION_RIGHT_PREFIX}_${context.name.lowercase()}"
        preferences.edit { putString(key, action.name) }
    }

    fun SharedPreferences.nullString(key: String): String? = getString(key, null)

    fun SharedPreferences.requireString(key: String, defaultValue: String): String =
        requireNotNull(getString(key, defaultValue))

    inline fun <reified T : Enum<T>> SharedPreferences.enumValue(key: String, defaultValue: T): T =
        nullString(key)?.toEnum<T>() ?: defaultValue

    inline fun <reified T : Enum<T>> SharedPreferences.enumValueByOrdinal(key: String, defaultValue: T): T =
        getInt(key, defaultValue.ordinal).toEnum<T>() ?: defaultValue
}

interface GeneralTheme {
    companion object {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val BLACK = "black"
        const val AUTO = "auto"
    }
}

interface UITheme {
    companion object {
        const val MATERIAL = "material"
        const val SPOTIFY = "spotify"
    }
}

interface BottomTitlesMode {
    companion object {
        const val SELECTED = "selected"
        const val LABELED = "labeled"
        const val UNLABELED = "unlabeled"
    }
}

interface AppBarMode {
    companion object {
        const val COMPACT = "compact"
        const val EXPANDED = "expanded"
    }
}

interface PlayOnStartupMode {
    companion object {
        const val NEVER = "never"
        const val WITH_MINIMIZED_PLAYER = "with_minimized_player"
        const val WITH_EXPANDED_PLAYER = "with_expanded_player"
    }
}

interface SelectedShuffleMode {
    companion object {
        const val SHUFFLE_ARTISTS = "shuffle_artists"
        const val SHUFFLE_ALBUMS = "shuffle_albums"
        const val SHUFFLE_SONGS = "shuffle_songs"
        const val SHUFFLE_ALL = "shuffle_all"
    }
}


interface PlaylistCutoff {
    companion object {
        const val TODAY = "today"
        const val YESTERDAY = "yesterday"
        const val THIS_WEEK = "this_week"
        const val THIS_MONTH = "this_month"
        const val PAST_THREE_MONTHS = "past_three_months"
        const val THIS_YEAR = "this_year"
    }
}

interface ImageSize {
    companion object {
        const val LARGE = "large"
        const val MEDIUM = "medium"
        const val SMALL = "small"
    }
}

interface UpdateSearchMode {
    companion object {
        const val EVERY_DAY = "every_day"
        const val WEEKLY = "weekly"
        const val EVERY_FIFTEEN_DAYS = "every_fifteen_days"
        const val MONTHLY = "monthly"
        const val NEVER = "never"
    }
}


enum class EraShapeFamily {
    ROUNDED, CUT
}

enum class EraSurfaceMaterial {
    SOLID, GLASS, VIBRANT
}

const val BLACK_THEME = "black_theme"
const val UI_THEME = "ui_theme"
const val MATERIAL_YOU = "material_you"
const val USE_CUSTOM_FONT = "use_custom_font"
const val APPBAR_MODE = "appbar_mode"
const val GENERAL_THEME = "general_theme"
const val ACCENT_COLOR = "accent_color"
const val LYRICS_ACCENT_COLOR = "lyrics_accent_color"
const val LIBRARY_CATEGORIES = "library_categories"
const val REMEMBER_LAST_PAGE = "remember_last_page"
const val TAB_TITLES_MODE = "tab_titles_mode"
const val HOLD_TAB_TO_SEARCH = "hold_tab_to_search"
const val LAST_PAGE = "last_page"
const val LARGER_HEADER_IMAGE = "larger_header_image"
const val HORIZONTAL_ARTIST_ALBUMS = "horizontal_artist_albums"
const val COMPACT_ALBUM_SONG_VIEW = "compact_album_song_view"
const val COMPACT_ARTIST_SONG_VIEW = "compact_artist_song_view"
const val NOW_PLAYING_SCREEN = "now_playing_screen"
const val OPEN_ON_PLAY = "open_on_play"
const val ADD_EXTRA_CONTROLS = "add_extra_controls"
const val ADAPTIVE_CONTROLS = "adaptive_controls"
const val SQUIGGLY_SEEK_BAR = "squiggly_seek_bar"
const val SWIPE_DOWN_TO_DISMISS = "swipe_down_to_dismiss"
const val LYRICS_ON_COVER = "lyrics_on_cover"
const val SWIPE_ON_COVER = "swipe_on_cover"
const val MINI_PLAYER_SWIPE_TO_SKIP = "mini_player_swipe_to_skip"
const val NOW_PLAYING_SMALL_IMAGE = "now_playing_small_image"
const val NOW_PLAYING_IMAGE_CORNER_RADIUS = "now_playing_corner_radius"
const val LYRICS_CARD_CORNER_RADIUS = "lyrics_card_corner_radius"
const val PLAYER_BLUR_RADIUS = "player_blur_radius"
const val CAROUSEL_EFFECT = "carousel_effect"
const val COVER_SINGLE_TAP_ACTION = "cover_single_tap_action"
const val COVER_DOUBLE_TAP_ACTION = "cover_double_tap_action"
const val COVER_LEFT_DOUBLE_TAP_ACTION = "cover_left_double_tap_action"
const val COVER_RIGHT_DOUBLE_TAP_ACTION = "cover_right_double_tap_action"
const val COVER_LONG_PRESS_ACTION = "cover_long_press_action"
const val ANIMATE_PLAYER_CONTROL = "animate_player_control"
const val CIRCLE_PLAY_BUTTON = "circle_play_button"
const val ENABLE_SCROLLING_TEXT = "enable_scrolling_text"
const val DISPLAY_ALBUM_TITLE = "display_album_title"
const val DISPLAY_EXTRA_INFO = "display_extra_info"
const val NOW_PLAYING_EXTRA_INFO = "now_playing_extra_info"
const val WIDGET_DYNAMIC_COLORS = "widget_dynamic_colors"
const val WIDGET_SMALL_LAYOUT_STYLE = "widget_small_layout_style"
const val WIDGET_IMAGE_CORNER_RADIUS = "widget_image_corner_radius"
const val WIDGET_THIRD_LINE_CONTENT = "widget_third_line_content"
const val PREFER_REMAINING_TIME = "prefer_remaining_time"
const val PREFER_ALBUM_ARTIST_NAME = "prefer_album_artist_name_on_np"
const val REWIND_WITH_BACK = "rewind_with_back"
const val SEEK_INTERVAL = "seek_interval"
const val QUEUE_NEXT_MODE = "queue_next_mode"
const val PLAY_ON_STARTUP_MODE = "play_on_startup_mode"
const val ON_SONG_CLICK_ACTION = "on_song_click_action"
const val ON_CLEAR_QUEUE_ACTION = "on_clear_queue_action"
const val PLAY_OPTION_ALWAYS_VISIBLE = "play_option_always_visible"
const val PLAY_OPTION_PLAYS_WHOLE_LIST = "play_option_whole_list"
const val PLAY_ALL_SONGS_WHEN_SEARCHING = "play_all_songs_when_searching"
const val CLEAR_QUEUE_ON_COMPLETION = "clear_queue_on_completion"
const val REMEMBER_SHUFFLE_MODE = "remember_shuffle_mode"
const val ALBUM_SHUFFLE_MODE = "album_shuffle_mode"
const val ARTIST_SHUFFLE_MODE = "artist_shuffle_mode"
const val RESUME_ON_CONNECT = "resume_on_connect"
const val PAUSE_ON_DISCONNECT = "pause_on_disconnect"
const val RESUME_ON_BLUETOOTH_CONNECT = "resume_on_bluetooth_connect"
const val PAUSE_ON_BLUETOOTH_DISCONNECT = "pause_on_bluetooth_disconnect"
const val IGNORE_AUDIO_FOCUS = "ignore_audio_focus"
const val PAUSE_ON_ZERO_VOLUME = "pause_on_zero_volume"
const val MP3_INDEX_SEEKING = "mp3_index_seeking"
const val IGNORE_MEDIA_STORE = "ignore_media_store"
const val USE_FOLDER_ART = "use_folder_art"
const val PREFERRED_IMAGE_SIZE = "preferred_image_size"
const val ONLY_ALBUM_ARTISTS = "only_album_artists"
const val TRASH_MUSIC_FILES = "trash_music_files"
const val RECURSIVE_FOLDER_ACTIONS = "recursive_folder_actions"
const val ENABLE_HISTORY = "enable_history_playlist"
const val HISTORY_CUTOFF = "history_interval"
const val LAST_ADDED_CUTOFF = "last_added_interval"
const val IGNORE_SINGLES = "ignore_singles"
const val SHOW_TOTAL_DURATION = "show_total_duration"
const val WHITELIST_ENABLED = "whitelist_enabled"
const val BLACKLIST_ENABLED = "blacklist_enabled"
const val ARTIST_MINIMUM_SONGS = "artist_minimum_songs"
const val ALBUM_MINIMUM_SONGS = "album_minimum_songs"
const val MINIMUM_SONG_DURATION = "minimum_song_duration"
const val ENABLE_ROTATION_LOCK = "enable_rotation_lock"
const val STOP_WHEN_CLOSED_FROM_RECENTS = "stop_when_closed_from_recents"
const val LANGUAGE_NAME = "language_name"
const val BACKUP_DATA = "backup_data"
const val RESTORE_DATA = "restore_data"
const val UPDATE_SEARCH_MODE = "update_search_mode"
const val ONLY_WIFI = "update_only_wifi"
const val LAST_UPDATE_SEARCH = "last_update_search"
const val LAST_UPDATE_ID = "last_update_id"
const val EXPERIMENTAL_UPDATES = "experimental_updates"
const val START_DIRECTORY = "start_directory"
const val SAVED_ARTWORK_COPYRIGHT_NOTICE_SHOWN = "saved_artwork_copyright_notice_shown"
const val INITIALIZED_BLACKLIST = "initialized_blacklist"
const val HIERARCHY_FOLDER_VIEW = "hierarchy_folder_view"
const val SWIPE_ANYWHERE = "swipe_anywhere"
const val SWIPE_UP_QUEUE = "swipe_up_queue"
const val DISPLAY_NEXT_SONG = "display_next_song"
const val LOCKED_QUEUE = "locked_queue"
const val LOCKED_PLAYLISTS = "locked_playlists"
const val QUEUE_HEIGHT = "queue_height"
const val LASTFM_LOGIN = "lastfm_login"
const val LASTFM_SYNC_FAVORITES = "lastfm_sync_favorites"
const val LASTFM_OFFLINE_SCROBBLING = "lastfm_offline_scrobble"
const val LASTFM_SCROBBLE_PERCENTAGE = "lastfm_scrobble_percentage"
const val LASTFM_SCROBBLE_ENABLED = "lastfm_scrobbling_enabled"
const val LISTENBRAINZ_LOGIN = "listenbrainz_login"
const val LISTENBRAINZ_SCROBBLE_ENABLED = "listenbrainz_scrobbling_enabled"


const val ERA_PRIMARY_SEED = "era_primary_seed"
const val ERA_SECONDARY_SEED = "era_secondary_seed"
const val ERA_TERTIARY_SEED = "era_tertiary_seed"
const val ERA_ERROR_SEED = "era_error_seed"
const val ERA_HARMONY_MODE = "era_harmony_mode"
const val ERA_VIBRANCY = "era_vibrancy"
const val ERA_CONTRAST = "era_contrast"
const val ERA_SHAPE_SCALE = "era_shape_scale"
const val ERA_SHAPE_FAMILY = "era_shape_family"
const val ERA_ASYMMETRIC_SHAPES = "era_asymmetric_shapes"
const val ERA_TYPE_SCALE = "era_type_scale"
const val ERA_MOTION_INTENSITY = "era_motion_intensity"
const val ERA_SURFACE_MATERIAL = "era_surface_material"
const val ERA_ADAPTIVE_ICONOGRAPHY = "era_adaptive_iconography"

const val VIBRANT_BACKGROUND_MODE = "vibrant_background_mode"
const val VIBRANT_BACKGROUND_ANIMATIONS = "vibrant_background_animations"
const val VIBRANT_BACKGROUND_HIGH_QUALITY = "vibrant_background_high_quality"
const val VIBRANT_BACKGROUND_GLOBAL = "vibrant_background_global"

const val SWIPE_ACTION_LEFT_PREFIX = "swipe_action_left"
const val SWIPE_ACTION_RIGHT_PREFIX = "swipe_action_right"
