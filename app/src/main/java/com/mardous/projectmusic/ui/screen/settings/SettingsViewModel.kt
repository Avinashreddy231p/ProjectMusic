package com.mardous.projectmusic.ui.screen.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.theme.EraFont
import com.mardous.projectmusic.core.model.theme.EraShapeFamily
import com.mardous.projectmusic.core.model.theme.EraSurfaceMaterial
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.util.*
import com.mardous.projectmusic.core.model.lyrics.LyricsViewSettings
import com.mardous.projectmusic.core.model.action.*
import com.mardous.projectmusic.core.model.player.PlayerColorSchemeMode
import com.mardous.projectmusic.core.model.player.PlayerTransition
import com.mardous.projectmusic.playback.equalizer.EqualizerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: android.content.Context,
    private val preferences: SharedPreferences,
    private val equalizerManager: EqualizerManager,
    private val networkRepository: com.mardous.projectmusic.data.local.repository.NetworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val lastFmLoginState = networkRepository.getLoginState(com.mardous.projectmusic.data.model.network.ScrobblingService.Lastfm)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkRepository.getCurrentLoginState(com.mardous.projectmusic.data.model.network.ScrobblingService.Lastfm))

    val listenBrainzLoginState = networkRepository.getLoginState(com.mardous.projectmusic.data.model.network.ScrobblingService.ListenBrainz)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkRepository.getCurrentLoginState(com.mardous.projectmusic.data.model.network.ScrobblingService.ListenBrainz))

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        updateUiState(key)
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        loadInitialState()
        observeEqualizer()
    }

    private fun observeEqualizer() {
        viewModelScope.launch {
            combine(
                equalizerManager.audioOffload,
                equalizerManager.audioFloatOutput,
                equalizerManager.skipSilence
            ) { offload, float, skip ->
                Triple(offload, float, skip)
            }.collect { (offload, float, skip) ->
                _uiState.value = _uiState.value.copy(
                    audioOffload = offload,
                    audioFloatOutput = float,
                    skipSilence = skip
                )
            }
        }
    }

    private fun loadInitialState() {
        try {
            val currentState = _uiState.value
            _uiState.value = SettingsUiState(
                generalTheme = Preferences.generalTheme,
                blackTheme = Preferences.blackTheme,
                materialYou = Preferences.isMaterialYouTheme,
                uiTheme = Preferences.uiTheme,
                eraHarmonyMode = Preferences.eraHarmonyMode,
                eraShapeFamily = Preferences.eraShapeFamily,
                eraSurfaceMaterial = Preferences.eraSurfaceMaterial,
                eraMotionIntensity = Preferences.eraMotionIntensity,
                appBarMode = Preferences.appBarMode.name,
                lastfmScrobbling = Preferences.lastfmScrobbling,
                enableSyllableLyrics = preferences.safeBoolean(LyricsViewSettings.Key.ENABLE_SYLLABLE_LYRICS, false),
                showTranslation = preferences.safeBoolean(LyricsViewSettings.Key.SHOW_TRANSLATION, true),
                showTransliteration = preferences.safeBoolean(LyricsViewSettings.Key.SHOW_TRANSLITERATION, false),
                centerCurrentLine = preferences.safeBoolean(LyricsViewSettings.Key.CENTER_CURRENT_LINE, false),
                centerHorizontally = preferences.safeBoolean(LyricsViewSettings.Key.CENTER_HORIZONTALLY, false),
                useCustomFont = preferences.safeBoolean(LyricsViewSettings.Key.USE_CUSTOM_FONT, false),
                customFontPath = preferences.nullString(LyricsViewSettings.Key.SELECTED_CUSTOM_FONT),
                lineSpacing = preferences.safeInt(LyricsViewSettings.Key.LINE_SPACING, 40),
                resumeOnSeek = preferences.safeBoolean(LyricsViewSettings.Key.RESUME_ON_SEEK, false),
                syncedLyricsBold = preferences.safeBoolean(LyricsViewSettings.Key.SYNCED_BOLD_FONT, false),
                syncedFontSizePlayer = preferences.safeInt(LyricsViewSettings.Key.SYNCED_FONT_SIZE_PLAYER, 20),
                syncedFontSizeFull = preferences.safeInt(LyricsViewSettings.Key.SYNCED_FONT_SIZE_FULL, 24),
                unsyncedLyricsBold = preferences.safeBoolean(LyricsViewSettings.Key.UNSYNCED_BOLD_FONT, false),
                unsyncedFontSizePlayer = preferences.safeInt(LyricsViewSettings.Key.UNSYNCED_FONT_SIZE_PLAYER, 16),
                unsyncedFontSizeFull = preferences.safeInt(LyricsViewSettings.Key.UNSYNCED_FONT_SIZE_FULL, 20),
                lyricsAccentColor = preferences.safeInt(LYRICS_ACCENT_COLOR, 0),
                enableKaraokeStyle = preferences.safeBoolean(LyricsViewSettings.Key.ENABLE_KARAOKE_STYLE, false),
                progressiveColoring = preferences.safeBoolean(LyricsViewSettings.Key.PROGRESSIVE_COLORING, false),
                aaMetadataLyrics = preferences.safeBoolean("aa_metadata_lyrics", false),
                instrumentalTrackIdentifiers = preferences.requireString("instrumental_track_identifiers", context.getString(R.string.instrumental_identifiers)),
                markInstrumentalTracksByTitle = preferences.safeBoolean("mark_instrumental_tracks_by_title", false),
                ignoreBlankLinesInLyrics = preferences.safeBoolean("ignore_blank_lines_in_lyrics", false),
                preferredLyricsFileFormat = preferences.requireString("preferred_lyrics_file_format", "ttml"),
                forceUtf8EncodingForLyrics = preferences.safeBoolean("force_utf8_encoding_for_lyrics", true),
                lyricsBackgroundEffect = preferences.requireString(LyricsViewSettings.Key.BACKGROUND_EFFECT, "none"),
                lyricsShadowEffect = preferences.safeBoolean(LyricsViewSettings.Key.SHADOW_EFFECT, true),
                lyricsBlurEffect = preferences.safeBoolean(LyricsViewSettings.Key.BLUR_EFFECT, true),
                resumeOnConnect = Preferences.isResumeOnConnect(false),
                whitelistEnabled = Preferences.whitelistEnabled,
                blacklistEnabled = Preferences.blacklistEnabled,
                seekInterval = preferences.safeInt(SEEK_INTERVAL, 10),
                nowPlayingScreen = Preferences.nowPlayingScreen,
                vibrantBackgroundMode = Preferences.vibrantBackgroundMode,
                vibrantLyricsBackgroundMode = Preferences.vibrantLyricsBackgroundMode,
                vibrantBackgroundAnimations = Preferences.vibrantBackgroundAnimations,
                vibrantBackgroundHighQuality = Preferences.vibrantBackgroundHighQuality,
                vibrantBackgroundGlobal = Preferences.vibrantBackgroundGlobal,
                carouselEffect = Preferences.isCarouselEffect,
                nowPlayingSmallImage = Preferences.isSmallImage,
                nowPlayingCornerRadius = Preferences.getNowPlayingImageCornerRadius(context),
                lyricsCardCornerRadius = Preferences.nowPlayingLyricsCornerRadius,
                coverSingleTapAction = Preferences.coverSingleTapAction,
                coverDoubleTapAction = Preferences.coverDoubleTapAction,
                coverLeftDoubleTapAction = Preferences.coverLeftDoubleTapAction,
                coverRightDoubleTapAction = Preferences.coverRightDoubleTapAction,
                coverLongPressAction = Preferences.coverLongPressAction,
                swipeOnCover = Preferences.swipeOnCover,
                animatePlayerControl = Preferences.animateControls,
                addExtraControls = Preferences.extraControls,
                queueHeight = Preferences.queueHeight,
                enableScrollingText = Preferences.enableScrollingText,
                preferRemainingTime = Preferences.preferRemainingTime,
                preferAlbumArtistName = Preferences.preferAlbumArtistName,
                vibrantBackgroundNoiseLevel = Preferences.vibrantBackgroundNoiseLevel,
                playerBlurRadius = preferences.safeInt(PLAYER_BLUR_RADIUS, context.resources.getInteger(R.integer.max_player_blur)),
                openOnPlay = preferences.safeBoolean(OPEN_ON_PLAY, false),
                squigglySeekBar = Preferences.squigglySeekBar,
                progressBarStyle = Preferences.progressBarStyle,
                thumbStyle = Preferences.thumbStyle,
                thumbSize = Preferences.thumbSize,
                progressControlStyle = Preferences.progressControlStyle,
                adaptiveControls = Preferences.adaptiveControls,
                circlePlayButton = Preferences.circularPlayButton,
                displayAlbumTitle = Preferences.displayAlbumTitle,
                displayNextSong = Preferences.isShowNextSong,
                displayExtraInfo = Preferences.displayExtraInfo,
                miniPlayerSwipeToSkip = Preferences.miniPlayerSwipeToSkip,
                swipeDownToDismiss = Preferences.swipeDownToDismiss,
                swipeAnywhere = Preferences.isSwipeAnywhere,
                swipeUpQueue = Preferences.isSwipeUpQueue,
                eraShapeScale = Preferences.eraShapeScale,
                eraContrast = Preferences.eraContrast,
                eraTypeScale = Preferences.eraTypeScale,
                eraPrimarySeed = Preferences.eraPrimarySeed,
                eraSecondarySeed = Preferences.eraSecondarySeed,
                eraTertiarySeed = Preferences.eraTertiarySeed,
                eraErrorSeed = Preferences.eraErrorSeed,
                eraAsymmetricShapes = Preferences.eraAsymmetricShapes,
                eraVibrancy = Preferences.eraVibrancy,
                eraFontFamily = Preferences.eraFontFamily,
                rememberLastPage = preferences.safeBoolean(REMEMBER_LAST_PAGE, true),
                tabTitlesMode = preferences.requireString(TAB_TITLES_MODE, "selected"),
                holdTabToSearch = preferences.safeBoolean(HOLD_TAB_TO_SEARCH, true),
                largerHeaderImage = preferences.safeBoolean(LARGER_HEADER_IMAGE, false),
                widgetSmallLayoutStyle = preferences.requireString(WIDGET_SMALL_LAYOUT_STYLE, "simplified"),
                widgetDynamicColors = preferences.safeBoolean(WIDGET_DYNAMIC_COLORS, false),
                widgetImageCornerRadius = preferences.safeInt(WIDGET_IMAGE_CORNER_RADIUS, 8),
                widgetThirdLineContent = preferences.requireString(WIDGET_THIRD_LINE_CONTENT, ""),
                networkFeatures = preferences.safeBoolean(NetworkFeature.NETWORK_FEATURES_KEY, true),
                wifiOnlyNetwork = preferences.safeBoolean(NetworkFeature.ONLY_WIFI_NETWORK_KEY, true),
                onlineMusicProvider = preferences.requireString(NetworkFeature.ONLINE_MUSIC_PROVIDER_KEY, "ytmusic"),
                allowOnlineArtistImages = preferences.safeBoolean(NetworkFeature.ALLOW_ONLINE_ARTIST_IMAGES_KEY, true),
                allowOnlineAlbumCovers = preferences.safeBoolean(NetworkFeature.ALLOW_ONLINE_ALBUM_COVERS_KEY, false),
                preferredImageSize = preferences.requireString(PREFERRED_IMAGE_SIZE, "medium"),
                lrclibEnabled = preferences.safeBoolean(NetworkFeature.LRCLIB_ENABLED_KEY, true),
                betterLyricsEnabled = preferences.safeBoolean(NetworkFeature.BETTERLYRICS_ENABLED_KEY, false),
                lyricallyEnabled = preferences.safeBoolean(NetworkFeature.LYRICALLY_ENABLED_KEY, false),
                geniusEnabled = preferences.safeBoolean(NetworkFeature.GENIUS_ENABLED_KEY, false),
                lyricsPlusEnabled = preferences.safeBoolean(NetworkFeature.LYRICSPLUS_ENABLED_KEY, false),
                geniusApiKey = preferences.requireString(NetworkFeature.GENIUS_API_KEY_KEY, ""),
                lyricallyApiKey = preferences.requireString(NetworkFeature.LYRICALLY_API_KEY_KEY, ""),
                updateSearchMode = preferences.requireString(NetworkFeature.UPDATE_SEARCH_MODE_KEY, "weekly"),
                experimentalUpdates = preferences.safeBoolean(EXPERIMENTAL_UPDATES, false),
                lastfmNowPlaying = Preferences.lastfmNowPlaying,
                listenbrainzScrobbling = Preferences.listenbrainzScrobbling,
                listenbrainzNowPlaying = Preferences.listenbrainzNowPlaying,
                languageName = preferences.requireString(LANGUAGE_NAME, "auto"),
                rotationLock = preferences.safeBoolean(ENABLE_ROTATION_LOCK, false),
                pauseOnZeroVolume = preferences.safeBoolean(PAUSE_ON_ZERO_VOLUME, false),
                mp3IndexSeeking = preferences.safeBoolean(MP3_INDEX_SEEKING, false),
                stopWhenClosedFromRecents = preferences.safeBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, false),
                ignoreMediaStore = preferences.safeBoolean(IGNORE_MEDIA_STORE, false),
                useFolderArt = preferences.safeBoolean(USE_FOLDER_ART, false),
                trashMusicFiles = preferences.safeBoolean(TRASH_MUSIC_FILES, false),
                ignoreArticlesWhenSorting = preferences.safeBoolean("ignore_articles_when_sorting", false),
                enableHistoryPlaylist = preferences.safeBoolean(ENABLE_HISTORY, true),
                historyInterval = preferences.requireString(HISTORY_CUTOFF, "this_month"),
                lastAddedInterval = preferences.requireString(LAST_ADDED_CUTOFF, "this_month"),
                recursiveFolderActions = preferences.safeStringSet(RECURSIVE_FOLDER_ACTIONS, emptySet()),
                minimumSongDuration = preferences.safeInt(MINIMUM_SONG_DURATION, 15),
                artistMinimumSongs = preferences.safeInt(ARTIST_MINIMUM_SONGS, 1),
                albumMinimumSongs = preferences.safeInt(ALBUM_MINIMUM_SONGS, 1),
                playOnStartupMode = preferences.requireString(PLAY_ON_STARTUP_MODE, "never"),
                queueNextMode = preferences.requireString(QUEUE_NEXT_MODE, "1"),
                songClickAction = Preferences.songClickAction,
                clearQueueAction = Preferences.clearQueueAction,
                playOptionAlwaysVisible = preferences.safeBoolean(PLAY_OPTION_ALWAYS_VISIBLE, false),
                playOptionWholeList = preferences.safeBoolean(PLAY_OPTION_PLAYS_WHOLE_LIST, false),
                playAllSongsWhenSearching = preferences.safeBoolean(PLAY_ALL_SONGS_WHEN_SEARCHING, false),
                clearQueueOnCompletion = preferences.safeBoolean(CLEAR_QUEUE_ON_COMPLETION, false),
                rememberShuffleMode = preferences.safeBoolean(REMEMBER_SHUFFLE_MODE, true),
                albumShuffleMode = preferences.requireString(ALBUM_SHUFFLE_MODE, "shuffle_albums"),
                artistShuffleMode = preferences.requireString(ARTIST_SHUFFLE_MODE, "shuffle_all"),
                rewindWithBack = preferences.safeBoolean(REWIND_WITH_BACK, true),
                pauseOnDisconnect = Preferences.isPauseOnDisconnect(false),
                resumeOnBluetoothConnect = Preferences.isResumeOnConnect(true),
                pauseOnBluetoothDisconnect = Preferences.isPauseOnDisconnect(true),
                ignoreAudioFocus = preferences.safeBoolean(IGNORE_AUDIO_FOCUS, false),
                lockedPlaylists = Preferences.lockedPlaylists,
                horizontalArtistAlbums = Preferences.horizontalArtistAlbums,
                compactAlbumSongView = Preferences.compactAlbumSongView,
                compactArtistSongView = Preferences.compactArtistSongView,
                showLyricsOnCover = Preferences.showLyricsOnCover,
                isQueueLocked = Preferences.isQueueLocked,
                onlyAlbumArtists = Preferences.onlyAlbumArtists,
                ignoreSingles = Preferences.ignoreSingles,
                showAlbumDuration = Preferences.showAlbumDuration,
                hierarchyFolderView = Preferences.hierarchyFolderView,
                lastfmSyncFavorites = Preferences.lastfmSyncFavorites,
                lastfmOfflineScrobbling = Preferences.lastfmOfflineScrobbling,
                lastfmScrobblePercentage = Preferences.lastfmScrobblePercentage,
                updateOnlyWifi = preferences.safeBoolean(ONLY_WIFI, false),
                nowPlayingColorScheme = Preferences.getNowPlayingColorSchemeMode(Preferences.nowPlayingScreen),
                nowPlayingTransition = Preferences.getNowPlayingTransition(Preferences.nowPlayingScreen),
                lastfmInfoEnabled = preferences.safeBoolean(NetworkFeature.LASTFM_INFO_ENABLED_KEY, true),
                musicbrainzEnabled = Preferences.musicbrainzEnabled,
                isUpdaterEnabled = NetworkFeature.Updater.isEnabled,
                audioOffload = currentState.audioOffload,
                audioFloatOutput = currentState.audioFloatOutput,
                skipSilence = currentState.skipSilence,
                searchQuery = currentState.searchQuery
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUiState(key: String?) {
        viewModelScope.launch {
            loadInitialState()
        }
    }

    fun setGeneralTheme(theme: String) {
        Preferences.generalTheme = theme
        _uiState.update { it.copy(generalTheme = theme) }
    }

    fun setBlackTheme(enabled: Boolean) {
        preferences.edit().putBoolean(BLACK_THEME, enabled).apply()
        _uiState.update { it.copy(blackTheme = enabled) }
    }

    fun setUiTheme(theme: String) {
        Preferences.uiTheme = theme
        _uiState.update { it.copy(uiTheme = theme) }
    }

    fun setMaterialYou(enabled: Boolean) {
        preferences.edit().putBoolean(MATERIAL_YOU, enabled).apply()
        _uiState.update { it.copy(materialYou = enabled) }
    }

    fun setAppBarMode(mode: String) {
        preferences.edit().putString(APPBAR_MODE, mode).apply()
        _uiState.update { it.copy(appBarMode = mode) }
    }

    fun setEraHarmonyMode(enabled: Boolean) {
        Preferences.eraHarmonyMode = enabled
        _uiState.update { it.copy(eraHarmonyMode = enabled) }
    }

    fun setEraShapeFamily(family: EraShapeFamily) {
        Preferences.eraShapeFamily = family
        _uiState.update { it.copy(eraShapeFamily = family) }
    }

    fun setEraSurfaceMaterial(material: EraSurfaceMaterial) {
        Preferences.eraSurfaceMaterial = material
        _uiState.update { it.copy(eraSurfaceMaterial = material) }
    }

    fun setEraShapeScale(scale: Float) {
        Preferences.eraShapeScale = scale
        _uiState.update { it.copy(eraShapeScale = scale) }
    }

    fun setEraContrast(contrast: Float) {
        Preferences.eraContrast = contrast
        _uiState.update { it.copy(eraContrast = contrast) }
    }

    fun setEraTypeScale(scale: Float) {
        Preferences.eraTypeScale = scale
        _uiState.update { it.copy(eraTypeScale = scale) }
    }

    fun setEraMotionIntensity(intensity: Int) {
        Preferences.eraMotionIntensity = intensity
        _uiState.update { it.copy(eraMotionIntensity = intensity) }
    }

    fun setEraPrimarySeed(color: Int) {
        Preferences.eraPrimarySeed = color
        _uiState.update { it.copy(eraPrimarySeed = color) }
    }

    fun setEraSecondarySeed(color: Int) {
        Preferences.eraSecondarySeed = color
        _uiState.update { it.copy(eraSecondarySeed = color) }
    }

    fun setEraTertiarySeed(color: Int) {
        Preferences.eraTertiarySeed = color
        _uiState.update { it.copy(eraTertiarySeed = color) }
    }

    fun setEraErrorSeed(color: Int) {
        Preferences.eraErrorSeed = color
        _uiState.update { it.copy(eraErrorSeed = color) }
    }

    fun setEraAsymmetricShapes(enabled: Boolean) {
        Preferences.eraAsymmetricShapes = enabled
        _uiState.update { it.copy(eraAsymmetricShapes = enabled) }
    }

    fun setEraVibrancy(vibrancy: Float) {
        Preferences.eraVibrancy = vibrancy
        _uiState.update { it.copy(eraVibrancy = vibrancy) }
    }

    fun setEraFontFamily(font: EraFont) {
        Preferences.eraFontFamily = font
        _uiState.update { it.copy(eraFontFamily = font) }
    }

    fun setRememberLastPage(enabled: Boolean) {
        preferences.edit().putBoolean(REMEMBER_LAST_PAGE, enabled).apply()
        _uiState.update { it.copy(rememberLastPage = enabled) }
    }

    fun setTabTitlesMode(mode: String) {
        preferences.edit().putString(TAB_TITLES_MODE, mode).apply()
        _uiState.update { it.copy(tabTitlesMode = mode) }
    }

    fun setHoldTabToSearch(enabled: Boolean) {
        preferences.edit().putBoolean(HOLD_TAB_TO_SEARCH, enabled).apply()
        _uiState.update { it.copy(holdTabToSearch = enabled) }
    }

    fun setLargerHeaderImage(enabled: Boolean) {
        preferences.edit().putBoolean(LARGER_HEADER_IMAGE, enabled).apply()
        _uiState.update { it.copy(largerHeaderImage = enabled) }
    }

    fun setWidgetSmallLayoutStyle(style: String) {
        preferences.edit().putString(WIDGET_SMALL_LAYOUT_STYLE, style).apply()
        _uiState.update { it.copy(widgetSmallLayoutStyle = style) }
    }

    fun setWidgetDynamicColors(enabled: Boolean) {
        preferences.edit().putBoolean(WIDGET_DYNAMIC_COLORS, enabled).apply()
        _uiState.update { it.copy(widgetDynamicColors = enabled) }
    }

    fun setWidgetImageCornerRadius(radius: Int) {
        preferences.edit().putInt(WIDGET_IMAGE_CORNER_RADIUS, radius).apply()
        _uiState.update { it.copy(widgetImageCornerRadius = radius) }
    }

    fun setWidgetThirdLineContent(content: String) {
        preferences.edit().putString(WIDGET_THIRD_LINE_CONTENT, content).apply()
        _uiState.update { it.copy(widgetThirdLineContent = content) }
    }

    fun setNowPlayingScreen(screen: com.mardous.projectmusic.core.model.theme.NowPlayingScreen) {
        Preferences.nowPlayingScreen = screen
        _uiState.update { it.copy(nowPlayingScreen = screen) }
    }

    fun setVibrantBackgroundMode(mode: com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode) {
        Preferences.vibrantBackgroundMode = mode
        _uiState.update { it.copy(vibrantBackgroundMode = mode) }
    }

    fun setVibrantLyricsBackgroundMode(mode: com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode) {
        Preferences.vibrantLyricsBackgroundMode = mode
        _uiState.update { it.copy(vibrantLyricsBackgroundMode = mode) }
    }

    fun setVibrantBackgroundAnimations(enabled: Boolean) {
        preferences.edit().putBoolean(VIBRANT_BACKGROUND_ANIMATIONS, enabled).apply()
        _uiState.update { it.copy(vibrantBackgroundAnimations = enabled) }
    }

    fun setVibrantBackgroundHighQuality(enabled: Boolean) {
        preferences.edit().putBoolean(VIBRANT_BACKGROUND_HIGH_QUALITY, enabled).apply()
        _uiState.update { it.copy(vibrantBackgroundHighQuality = enabled) }
    }

    fun setVibrantBackgroundGlobal(enabled: Boolean) {
        preferences.edit().putBoolean(VIBRANT_BACKGROUND_GLOBAL, enabled).apply()
        _uiState.update { it.copy(vibrantBackgroundGlobal = enabled) }
    }

    fun setCarouselEffect(enabled: Boolean) {
        preferences.edit().putBoolean(CAROUSEL_EFFECT, enabled).apply()
        _uiState.update { it.copy(carouselEffect = enabled) }
    }

    fun setNowPlayingSmallImage(enabled: Boolean) {
        preferences.edit().putBoolean(NOW_PLAYING_SMALL_IMAGE, enabled).apply()
        _uiState.update { it.copy(nowPlayingSmallImage = enabled) }
    }

    fun setNowPlayingCornerRadius(radius: Int) {
        preferences.edit().putInt(NOW_PLAYING_IMAGE_CORNER_RADIUS, radius).apply()
        _uiState.update { it.copy(nowPlayingCornerRadius = radius) }
    }

    fun setLyricsCardCornerRadius(radius: Int) {
        Preferences.nowPlayingLyricsCornerRadius = radius
        _uiState.update { it.copy(lyricsCardCornerRadius = radius) }
    }

    fun setSquigglySeekBar(enabled: Boolean) {
        preferences.edit().putBoolean(SQUIGGLY_SEEK_BAR, enabled).apply()
        _uiState.update { 
            it.copy(
                squigglySeekBar = enabled,
                progressBarStyle = if (enabled) ProgressBarStyle.WAVY else if (it.progressBarStyle == ProgressBarStyle.WAVY) ProgressBarStyle.LINEAR else it.progressBarStyle
            )
        }
        // Backward compatibility sync
        if (enabled) {
            setProgressBarStyle(ProgressBarStyle.WAVY)
        } else if (Preferences.progressBarStyle == ProgressBarStyle.WAVY) {
            setProgressBarStyle(ProgressBarStyle.LINEAR)
        }
    }

    fun setProgressBarStyle(style: ProgressBarStyle) {
        Preferences.setProgressBarStyle(style)
        _uiState.update { it.copy(progressBarStyle = style, squigglySeekBar = style == ProgressBarStyle.WAVY) }
    }

    fun setThumbStyle(style: ThumbStyle) {
        Preferences.setThumbStyle(style)
        _uiState.update { it.copy(thumbStyle = style) }
    }

    fun setThumbSize(scale: Float) {
        Preferences.thumbSize = scale
        _uiState.update { it.copy(thumbSize = scale) }
    }

    fun setProgressControlStyle(style: ProgressControlStyle) {
        Preferences.setProgressControlStyle(style)
        _uiState.update { it.copy(progressControlStyle = style) }
    }

    fun setAdaptiveControls(enabled: Boolean) {
        preferences.edit().putBoolean(ADAPTIVE_CONTROLS, enabled).apply()
        _uiState.update { it.copy(adaptiveControls = enabled) }
    }

    fun setCirclePlayButton(enabled: Boolean) {
        preferences.edit().putBoolean(CIRCLE_PLAY_BUTTON, enabled).apply()
        _uiState.update { it.copy(circlePlayButton = enabled) }
    }

    fun setDisplayAlbumTitle(enabled: Boolean) {
        preferences.edit().putBoolean(DISPLAY_ALBUM_TITLE, enabled).apply()
        _uiState.update { it.copy(displayAlbumTitle = enabled) }
    }

    fun setDisplayNextSong(enabled: Boolean) {
        Preferences.isShowNextSong = enabled
        _uiState.update { it.copy(displayNextSong = enabled) }
    }

    fun setDisplayExtraInfo(enabled: Boolean) {
        preferences.edit().putBoolean(DISPLAY_EXTRA_INFO, enabled).apply()
        _uiState.update { it.copy(displayExtraInfo = enabled) }
    }

    fun setMiniPlayerSwipeToSkip(enabled: Boolean) {
        preferences.edit().putBoolean(MINI_PLAYER_SWIPE_TO_SKIP, enabled).apply()
        _uiState.update { it.copy(miniPlayerSwipeToSkip = enabled) }
    }

    fun setSwipeDownToDismiss(enabled: Boolean) {
        preferences.edit().putBoolean(SWIPE_DOWN_TO_DISMISS, enabled).apply()
        _uiState.update { it.copy(swipeDownToDismiss = enabled) }
    }

    fun setSwipeAnywhere(enabled: Boolean) {
        Preferences.isSwipeAnywhere = enabled
        _uiState.update { it.copy(swipeAnywhere = enabled) }
    }

    fun setSwipeUpQueue(enabled: Boolean) {
        Preferences.isSwipeUpQueue = enabled
        _uiState.update { it.copy(swipeUpQueue = enabled) }
    }

    fun setSwipeOnCover(enabled: Boolean) {
        preferences.edit().putBoolean(SWIPE_ON_COVER, enabled).apply()
        _uiState.update { it.copy(swipeOnCover = enabled) }
    }

    fun setAnimatePlayerControl(enabled: Boolean) {
        preferences.edit().putBoolean(ANIMATE_PLAYER_CONTROL, enabled).apply()
        _uiState.update { it.copy(animatePlayerControl = enabled) }
    }

    fun setAddExtraControls(enabled: Boolean) {
        preferences.edit().putBoolean(ADD_EXTRA_CONTROLS, enabled).apply()
        _uiState.update { it.copy(addExtraControls = enabled) }
    }

    fun setQueueHeight(enabled: Boolean) {
        Preferences.queueHeight = enabled
        _uiState.update { it.copy(queueHeight = enabled) }
    }

    fun setEnableScrollingText(enabled: Boolean) {
        preferences.edit().putBoolean(ENABLE_SCROLLING_TEXT, enabled).apply()
        _uiState.update { it.copy(enableScrollingText = enabled) }
    }

    fun setPreferRemainingTime(enabled: Boolean) {
        Preferences.preferRemainingTime = enabled
        _uiState.update { it.copy(preferRemainingTime = enabled) }
    }

    fun setPreferAlbumArtistName(enabled: Boolean) {
        Preferences.preferAlbumArtistName = enabled
        _uiState.update { it.copy(preferAlbumArtistName = enabled) }
    }

    fun setVibrantBackgroundNoiseLevel(level: Int) {
        Preferences.vibrantBackgroundNoiseLevel = level
        _uiState.update { it.copy(vibrantBackgroundNoiseLevel = level) }
    }

    fun setPlayerBlurRadius(radius: Int) {
        preferences.edit().putInt(PLAYER_BLUR_RADIUS, radius).apply()
        _uiState.update { it.copy(playerBlurRadius = radius) }
    }

    fun setOpenOnPlay(enabled: Boolean) {
        preferences.edit().putBoolean(OPEN_ON_PLAY, enabled).apply()
        _uiState.update { it.copy(openOnPlay = enabled) }
    }

    fun setCoverAction(key: String, action: NowPlayingAction) {
        preferences.edit().putString(key, action.name).apply()
        _uiState.update { 
            when (key) {
                COVER_SINGLE_TAP_ACTION -> it.copy(coverSingleTapAction = action)
                COVER_DOUBLE_TAP_ACTION -> it.copy(coverDoubleTapAction = action)
                COVER_LEFT_DOUBLE_TAP_ACTION -> it.copy(coverLeftDoubleTapAction = action)
                COVER_RIGHT_DOUBLE_TAP_ACTION -> it.copy(coverRightDoubleTapAction = action)
                COVER_LONG_PRESS_ACTION -> it.copy(coverLongPressAction = action)
                else -> it
            }
        }
    }

    fun setSeekInterval(interval: Int) {
        preferences.edit().putInt(SEEK_INTERVAL, interval).apply()
        _uiState.update { it.copy(seekInterval = interval) }
    }

    fun setPlayOnStartupMode(mode: String) {
        preferences.edit().putString(PLAY_ON_STARTUP_MODE, mode).apply()
        _uiState.update { it.copy(playOnStartupMode = mode) }
    }

    fun setQueueNextMode(mode: String) {
        preferences.edit().putString(QUEUE_NEXT_MODE, mode).apply()
        _uiState.update { it.copy(queueNextMode = mode) }
    }

    fun setSongClickAction(action: SongClickBehavior) {
        Preferences.songClickAction = action
        _uiState.update { it.copy(songClickAction = action) }
    }

    fun setClearQueueAction(action: QueueClearingBehavior) {
        Preferences.clearQueueAction = action
        _uiState.update { it.copy(clearQueueAction = action) }
    }

    fun setPlayOptionAlwaysVisible(enabled: Boolean) {
        preferences.edit().putBoolean(PLAY_OPTION_ALWAYS_VISIBLE, enabled).apply()
        _uiState.update { it.copy(playOptionAlwaysVisible = enabled) }
    }

    fun setPlayOptionWholeList(enabled: Boolean) {
        preferences.edit().putBoolean(PLAY_OPTION_PLAYS_WHOLE_LIST, enabled).apply()
        _uiState.update { it.copy(playOptionWholeList = enabled) }
    }

    fun setPlayAllSongsWhenSearching(enabled: Boolean) {
        preferences.edit().putBoolean(PLAY_ALL_SONGS_WHEN_SEARCHING, enabled).apply()
        _uiState.update { it.copy(playAllSongsWhenSearching = enabled) }
    }

    fun setClearQueueOnCompletion(enabled: Boolean) {
        preferences.edit().putBoolean(CLEAR_QUEUE_ON_COMPLETION, enabled).apply()
        _uiState.update { it.copy(clearQueueOnCompletion = enabled) }
    }

    fun setRememberShuffleMode(enabled: Boolean) {
        preferences.edit().putBoolean(REMEMBER_SHUFFLE_MODE, enabled).apply()
        _uiState.update { it.copy(rememberShuffleMode = enabled) }
    }

    fun setAlbumShuffleMode(mode: String) {
        preferences.edit().putString(ALBUM_SHUFFLE_MODE, mode).apply()
        _uiState.update { it.copy(albumShuffleMode = mode) }
    }

    fun setArtistShuffleMode(mode: String) {
        preferences.edit().putString(ARTIST_SHUFFLE_MODE, mode).apply()
        _uiState.update { it.copy(artistShuffleMode = mode) }
    }

    fun setRewindWithBack(enabled: Boolean) {
        preferences.edit().putBoolean(REWIND_WITH_BACK, enabled).apply()
        _uiState.update { it.copy(rewindWithBack = enabled) }
    }

    fun setResumeOnConnect(enabled: Boolean) {
        preferences.edit().putBoolean(RESUME_ON_CONNECT, enabled).apply()
        _uiState.update { it.copy(resumeOnConnect = enabled) }
    }

    fun setPauseOnDisconnect(enabled: Boolean) {
        preferences.edit().putBoolean(PAUSE_ON_DISCONNECT, enabled).apply()
        _uiState.update { it.copy(pauseOnDisconnect = enabled) }
    }

    fun setResumeOnBluetoothConnect(enabled: Boolean) {
        preferences.edit().putBoolean(RESUME_ON_BLUETOOTH_CONNECT, enabled).apply()
        _uiState.update { it.copy(resumeOnBluetoothConnect = enabled) }
    }

    fun setPauseOnBluetoothDisconnect(enabled: Boolean) {
        preferences.edit().putBoolean(PAUSE_ON_BLUETOOTH_DISCONNECT, enabled).apply()
        _uiState.update { it.copy(pauseOnBluetoothDisconnect = enabled) }
    }

    fun setIgnoreAudioFocus(enabled: Boolean) {
        preferences.edit().putBoolean(IGNORE_AUDIO_FOCUS, enabled).apply()
        _uiState.update { it.copy(ignoreAudioFocus = enabled) }
    }

    fun toggleLyricsShadow(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.SHADOW_EFFECT, enabled).apply()
        _uiState.update { it.copy(lyricsShadowEffect = enabled) }
    }

    fun setEnableSyllableLyrics(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.ENABLE_SYLLABLE_LYRICS, enabled).apply()
        _uiState.update { it.copy(enableSyllableLyrics = enabled) }
    }

    fun setShowTranslation(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.SHOW_TRANSLATION, enabled).apply()
        _uiState.update { it.copy(showTranslation = enabled) }
    }

    fun setShowTransliteration(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.SHOW_TRANSLITERATION, enabled).apply()
        _uiState.update { it.copy(showTransliteration = enabled) }
    }

    fun setCenterCurrentLine(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.CENTER_CURRENT_LINE, enabled).apply()
        _uiState.update { it.copy(centerCurrentLine = enabled) }
    }

    fun setCenterHorizontally(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.CENTER_HORIZONTALLY, enabled).apply()
        _uiState.update { it.copy(centerHorizontally = enabled) }
    }

    fun setUseCustomFont(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.USE_CUSTOM_FONT, enabled).apply()
        _uiState.update { it.copy(useCustomFont = enabled) }
    }

    fun setLineSpacing(spacing: Int) {
        preferences.edit().putInt(LyricsViewSettings.Key.LINE_SPACING, spacing).apply()
        _uiState.update { it.copy(lineSpacing = spacing) }
    }

    fun setResumeOnSeek(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.RESUME_ON_SEEK, enabled).apply()
        _uiState.update { it.copy(resumeOnSeek = enabled) }
    }

    fun setSyncedLyricsBold(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.SYNCED_BOLD_FONT, enabled).apply()
        _uiState.update { it.copy(syncedLyricsBold = enabled) }
    }

    fun setSyncedFontSizePlayer(size: Int) {
        preferences.edit().putInt(LyricsViewSettings.Key.SYNCED_FONT_SIZE_PLAYER, size).apply()
        _uiState.update { it.copy(syncedFontSizePlayer = size) }
    }

    fun setSyncedFontSizeFull(size: Int) {
        preferences.edit().putInt(LyricsViewSettings.Key.SYNCED_FONT_SIZE_FULL, size).apply()
        _uiState.update { it.copy(syncedFontSizeFull = size) }
    }

    fun setUnsyncedLyricsBold(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.UNSYNCED_BOLD_FONT, enabled).apply()
        _uiState.update { it.copy(unsyncedLyricsBold = enabled) }
    }

    fun setUnsyncedFontSizePlayer(size: Int) {
        preferences.edit().putInt(LyricsViewSettings.Key.UNSYNCED_FONT_SIZE_PLAYER, size).apply()
        _uiState.update { it.copy(unsyncedFontSizePlayer = size) }
    }

    fun setUnsyncedFontSizeFull(size: Int) {
        preferences.edit().putInt(LyricsViewSettings.Key.UNSYNCED_FONT_SIZE_FULL, size).apply()
        _uiState.update { it.copy(unsyncedFontSizeFull = size) }
    }

    fun setLyricsBackgroundEffect(effect: String) {
        preferences.edit().putString(LyricsViewSettings.Key.BACKGROUND_EFFECT, effect).apply()
        _uiState.update { it.copy(lyricsBackgroundEffect = effect) }
    }

    fun setEnableKaraokeStyle(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.ENABLE_KARAOKE_STYLE, enabled).apply()
        _uiState.update { it.copy(enableKaraokeStyle = enabled) }
    }

    fun setProgressiveColoring(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.PROGRESSIVE_COLORING, enabled).apply()
        _uiState.update { it.copy(progressiveColoring = enabled) }
    }

    fun setLyricsBlurEffect(enabled: Boolean) {
        preferences.edit().putBoolean(LyricsViewSettings.Key.BLUR_EFFECT, enabled).apply()
        _uiState.update { it.copy(lyricsBlurEffect = enabled) }
    }

    fun setLyricsAccentColor(color: Int) {
        preferences.edit().putInt(LYRICS_ACCENT_COLOR, color).apply()
        _uiState.update { it.copy(lyricsAccentColor = color) }
    }

    fun setAaMetadataLyrics(enabled: Boolean) {
        preferences.edit().putBoolean("aa_metadata_lyrics", enabled).apply()
        _uiState.update { it.copy(aaMetadataLyrics = enabled) }
    }

    fun setInstrumentalTrackIdentifiers(identifiers: String) {
        preferences.edit().putString("instrumental_track_identifiers", identifiers).apply()
        _uiState.update { it.copy(instrumentalTrackIdentifiers = identifiers) }
    }

    fun setMarkInstrumentalTracksByTitle(enabled: Boolean) {
        preferences.edit().putBoolean("mark_instrumental_tracks_by_title", enabled).apply()
        _uiState.update { it.copy(markInstrumentalTracksByTitle = enabled) }
    }

    fun setIgnoreBlankLinesInLyrics(enabled: Boolean) {
        preferences.edit().putBoolean("ignore_blank_lines_in_lyrics", enabled).apply()
        _uiState.update { it.copy(ignoreBlankLinesInLyrics = enabled) }
    }

    fun setPreferredLyricsFileFormat(format: String) {
        preferences.edit().putString("preferred_lyrics_file_format", format).apply()
        _uiState.update { it.copy(preferredLyricsFileFormat = format) }
    }

    fun setForceUtf8EncodingForLyrics(enabled: Boolean) {
        preferences.edit().putBoolean("force_utf8_encoding_for_lyrics", enabled).apply()
        _uiState.update { it.copy(forceUtf8EncodingForLyrics = enabled) }
    }

    fun toggleWhitelist(enabled: Boolean) {
        preferences.edit().putBoolean(WHITELIST_ENABLED, enabled).apply()
        _uiState.update { it.copy(whitelistEnabled = enabled) }
    }

    fun toggleBlacklist(enabled: Boolean) {
        preferences.edit().putBoolean(BLACKLIST_ENABLED, enabled).apply()
        _uiState.update { it.copy(blacklistEnabled = enabled) }
    }

    fun setNetworkFeatures(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.NETWORK_FEATURES_KEY, enabled).apply()
        _uiState.update { it.copy(networkFeatures = enabled) }
    }

    fun setWifiOnlyNetwork(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.ONLY_WIFI_NETWORK_KEY, enabled).apply()
        _uiState.update { it.copy(wifiOnlyNetwork = enabled) }
    }

    fun setOnlineMusicProvider(provider: String) {
        preferences.edit().putString(NetworkFeature.ONLINE_MUSIC_PROVIDER_KEY, provider).apply()
        _uiState.update { it.copy(onlineMusicProvider = provider) }
    }

    fun setAllowOnlineArtistImages(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.ALLOW_ONLINE_ARTIST_IMAGES_KEY, enabled).apply()
        _uiState.update { it.copy(allowOnlineArtistImages = enabled) }
    }

    fun setAllowOnlineAlbumCovers(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.ALLOW_ONLINE_ALBUM_COVERS_KEY, enabled).apply()
        _uiState.update { it.copy(allowOnlineAlbumCovers = enabled) }
    }

    fun setPreferredImageSize(size: String) {
        preferences.edit().putString(PREFERRED_IMAGE_SIZE, size).apply()
        _uiState.update { it.copy(preferredImageSize = size) }
    }

    fun setLrclibEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.LRCLIB_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(lrclibEnabled = enabled) }
    }

    fun setBetterLyricsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.BETTERLYRICS_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(betterLyricsEnabled = enabled) }
    }

    fun setLyricallyEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.LYRICALLY_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(lyricallyEnabled = enabled) }
    }

    fun setGeniusEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.GENIUS_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(geniusEnabled = enabled) }
    }

    fun setLyricsPlusEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.LYRICSPLUS_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(lyricsPlusEnabled = enabled) }
    }

    fun setGeniusApiKey(key: String) {
        preferences.edit().putString(NetworkFeature.GENIUS_API_KEY_KEY, key).apply()
        _uiState.update { it.copy(geniusApiKey = key) }
    }

    fun setLyricallyApiKey(key: String) {
        preferences.edit().putString(NetworkFeature.LYRICALLY_API_KEY_KEY, key).apply()
        _uiState.update { it.copy(lyricallyApiKey = key) }
    }

    fun setUpdateSearchMode(mode: String) {
        preferences.edit().putString(NetworkFeature.UPDATE_SEARCH_MODE_KEY, mode).apply()
        _uiState.update { it.copy(updateSearchMode = mode) }
    }

    fun setExperimentalUpdates(enabled: Boolean) {
        preferences.edit().putBoolean(EXPERIMENTAL_UPDATES, enabled).apply()
        _uiState.update { it.copy(experimentalUpdates = enabled) }
    }

    fun setLastfmScrobbling(enabled: Boolean) {
        Preferences.lastfmScrobbling = enabled
        _uiState.update { it.copy(lastfmScrobbling = enabled) }
    }

    fun setLastfmNowPlaying(enabled: Boolean) {
        Preferences.lastfmNowPlaying = enabled
        _uiState.update { it.copy(lastfmNowPlaying = enabled) }
    }

    fun setListenbrainzScrobbling(enabled: Boolean) {
        Preferences.listenbrainzScrobbling = enabled
        _uiState.update { it.copy(listenbrainzScrobbling = enabled) }
    }

    fun setListenbrainzNowPlaying(enabled: Boolean) {
        Preferences.listenbrainzNowPlaying = enabled
        _uiState.update { it.copy(listenbrainzNowPlaying = enabled) }
    }

    fun setLanguageName(language: String) {
        preferences.edit().putString(LANGUAGE_NAME, language).apply()
        _uiState.update { it.copy(languageName = language) }
        if (language == "auto") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }

    fun setRotationLock(enabled: Boolean) {
        preferences.edit().putBoolean(ENABLE_ROTATION_LOCK, enabled).apply()
        _uiState.update { it.copy(rotationLock = enabled) }
    }

    fun setPauseOnZeroVolume(enabled: Boolean) {
        preferences.edit().putBoolean(PAUSE_ON_ZERO_VOLUME, enabled).apply()
        _uiState.update { it.copy(pauseOnZeroVolume = enabled) }
    }

    fun setMp3IndexSeeking(enabled: Boolean) {
        preferences.edit().putBoolean(MP3_INDEX_SEEKING, enabled).apply()
        _uiState.update { it.copy(mp3IndexSeeking = enabled) }
    }

    fun setStopWhenClosedFromRecents(enabled: Boolean) {
        preferences.edit().putBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, enabled).apply()
        _uiState.update { it.copy(stopWhenClosedFromRecents = enabled) }
    }

    fun setIgnoreMediaStore(enabled: Boolean) {
        preferences.edit().putBoolean(IGNORE_MEDIA_STORE, enabled).apply()
        _uiState.update { it.copy(ignoreMediaStore = enabled) }
    }

    fun setUseFolderArt(enabled: Boolean) {
        preferences.edit().putBoolean(USE_FOLDER_ART, enabled).apply()
        _uiState.update { it.copy(useFolderArt = enabled) }
    }

    fun setTrashMusicFiles(enabled: Boolean) {
        preferences.edit().putBoolean(TRASH_MUSIC_FILES, enabled).apply()
        _uiState.update { it.copy(trashMusicFiles = enabled) }
    }

    fun setIgnoreArticlesWhenSorting(enabled: Boolean) {
        preferences.edit().putBoolean("ignore_articles_when_sorting", enabled).apply()
        _uiState.update { it.copy(ignoreArticlesWhenSorting = enabled) }
    }

    fun setEnableHistoryPlaylist(enabled: Boolean) {
        preferences.edit().putBoolean(ENABLE_HISTORY, enabled).apply()
        _uiState.update { it.copy(enableHistoryPlaylist = enabled) }
    }

    fun setHistoryInterval(interval: String) {
        preferences.edit().putString(HISTORY_CUTOFF, interval).apply()
        _uiState.update { it.copy(historyInterval = interval) }
    }

    fun setLastAddedInterval(interval: String) {
        preferences.edit().putString(LAST_ADDED_CUTOFF, interval).apply()
        _uiState.update { it.copy(lastAddedInterval = interval) }
    }

    fun setRecursiveFolderActions(actions: Set<String>) {
        preferences.edit().putStringSet(RECURSIVE_FOLDER_ACTIONS, actions).apply()
        _uiState.update { it.copy(recursiveFolderActions = actions) }
    }

    fun setMinimumSongDuration(duration: Int) {
        preferences.edit().putInt(MINIMUM_SONG_DURATION, duration).apply()
        _uiState.update { it.copy(minimumSongDuration = duration) }
    }

    fun setArtistMinimumSongs(count: Int) {
        preferences.edit().putInt(ARTIST_MINIMUM_SONGS, count).apply()
        _uiState.update { it.copy(artistMinimumSongs = count) }
    }

    fun setAlbumMinimumSongs(count: Int) {
        preferences.edit().putInt(ALBUM_MINIMUM_SONGS, count).apply()
        _uiState.update { it.copy(albumMinimumSongs = count) }
    }

    fun setLockedPlaylists(enabled: Boolean) {
        Preferences.lockedPlaylists = enabled
        _uiState.update { it.copy(lockedPlaylists = enabled) }
    }

    fun setHorizontalArtistAlbums(enabled: Boolean) {
        Preferences.horizontalArtistAlbums = enabled
        _uiState.update { it.copy(horizontalArtistAlbums = enabled) }
    }

    fun setCompactAlbumSongView(enabled: Boolean) {
        Preferences.compactAlbumSongView = enabled
        _uiState.update { it.copy(compactAlbumSongView = enabled) }
    }

    fun setCompactArtistSongView(enabled: Boolean) {
        Preferences.compactArtistSongView = enabled
        _uiState.update { it.copy(compactArtistSongView = enabled) }
    }

    fun setShowLyricsOnCover(enabled: Boolean) {
        Preferences.showLyricsOnCover = enabled
        _uiState.update { it.copy(showLyricsOnCover = enabled) }
    }

    fun setQueueLocked(enabled: Boolean) {
        Preferences.isQueueLocked = enabled
        _uiState.update { it.copy(isQueueLocked = enabled) }
    }

    fun setOnlyAlbumArtists(enabled: Boolean) {
        Preferences.onlyAlbumArtists = enabled
        _uiState.update { it.copy(onlyAlbumArtists = enabled) }
    }

    fun setIgnoreSingles(enabled: Boolean) {
        Preferences.ignoreSingles = enabled
        _uiState.update { it.copy(ignoreSingles = enabled) }
    }

    fun setShowAlbumDuration(enabled: Boolean) {
        Preferences.showAlbumDuration = enabled
        _uiState.update { it.copy(showAlbumDuration = enabled) }
    }

    fun setHierarchyFolderView(enabled: Boolean) {
        Preferences.hierarchyFolderView = enabled
        _uiState.update { it.copy(hierarchyFolderView = enabled) }
    }

    fun setLastfmSyncFavorites(enabled: Boolean) {
        Preferences.setLastfmSyncFavorites(enabled)
        _uiState.update { it.copy(lastfmSyncFavorites = enabled) }
    }

    fun setLastfmOfflineScrobbling(enabled: Boolean) {
        Preferences.setLastfmOfflineScrobbling(enabled)
        _uiState.update { it.copy(lastfmOfflineScrobbling = enabled) }
    }

    fun setLastfmScrobblePercentage(value: Int) {
        Preferences.setLastfmScrobblePercentage(value)
        _uiState.update { it.copy(lastfmScrobblePercentage = value) }
    }

    fun setUpdateOnlyWifi(enabled: Boolean) {
        preferences.edit().putBoolean(ONLY_WIFI, enabled).apply()
        _uiState.update { it.copy(updateOnlyWifi = enabled) }
    }

    fun setNowPlayingColorScheme(mode: PlayerColorSchemeMode) {
        Preferences.setNowPlayingColorSchemeMode(Preferences.nowPlayingScreen, mode)
        _uiState.update { it.copy(nowPlayingColorScheme = mode) }
    }

    fun setNowPlayingTransition(transition: PlayerTransition) {
        Preferences.setNowPlayingTransition(Preferences.nowPlayingScreen, transition)
        _uiState.update { it.copy(nowPlayingTransition = transition) }
    }

    fun setLastfmInfoEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(NetworkFeature.LASTFM_INFO_ENABLED_KEY, enabled).apply()
        _uiState.update { it.copy(lastfmInfoEnabled = enabled) }
    }

    fun setMusicbrainzEnabled(enabled: Boolean) {
        Preferences.musicbrainzEnabled = enabled
        _uiState.update { it.copy(musicbrainzEnabled = enabled) }
    }

    fun runMusicbrainzScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(musicbrainzScanning = true, musicbrainzScanResult = null, musicbrainzScanProgress = 0, musicbrainzScanTotal = 0, musicbrainzScanLabel = null) }
            try {
                val repo = org.koin.java.KoinJavaComponent.get<com.mardous.projectmusic.data.local.repository.MusicBrainzRepository>(com.mardous.projectmusic.data.local.repository.MusicBrainzRepository::class.java)
                val result = repo.scanAndWriteAll { current, total, label ->
                    _uiState.update { it.copy(musicbrainzScanProgress = current, musicbrainzScanTotal = total, musicbrainzScanLabel = label) }
                }
                _uiState.update {
                    it.copy(
                        musicbrainzScanning = false,
                        musicbrainzScanResult = "Scanned ${result.songsScanned}, updated ${result.songsUpdated}, wrote ${result.tagsWritten} tags, ${result.errors} errors"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        musicbrainzScanning = false,
                        musicbrainzScanResult = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMusicbrainzResult() {
        _uiState.update { it.copy(musicbrainzScanResult = null) }
    }

    fun runFileTagScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(fileTagScanning = true, fileTagScanResult = null, fileTagScanProgress = 0, fileTagScanTotal = 0, fileTagScanLabel = null) }
            try {
                val scanner = org.koin.java.KoinJavaComponent.get<com.mardous.projectmusic.data.local.repository.FileTagScanner>(com.mardous.projectmusic.data.local.repository.FileTagScanner::class.java)
                val result = scanner.scanAll { current, total, label ->
                    _uiState.update { it.copy(fileTagScanProgress = current, fileTagScanTotal = total, fileTagScanLabel = label) }
                }
                _uiState.update {
                    it.copy(
                        fileTagScanning = false,
                        fileTagScanResult = "Scanned ${result.songsScanned}, updated ${result.songsUpdated}, lyrics ${result.lyricsUpdated}, ${result.errors} errors"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        fileTagScanning = false,
                        fileTagScanResult = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearFileTagResult() {
        _uiState.update { it.copy(fileTagScanResult = null) }
    }

    fun runArtistScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(artistScanning = true, artistScanResult = null, artistScanProgress = 0, artistScanTotal = 0, artistScanLabel = null) }
            try {
                val repo = org.koin.java.KoinJavaComponent.get<com.mardous.projectmusic.data.local.repository.MusicBrainzRepository>(com.mardous.projectmusic.data.local.repository.MusicBrainzRepository::class.java)
                val result = repo.scanArtists { current, total, label ->
                    _uiState.update { it.copy(artistScanProgress = current, artistScanTotal = total, artistScanLabel = label) }
                }
                _uiState.update {
                    it.copy(
                        artistScanning = false,
                        artistScanResult = "Scanned ${result.artistsScanned}, updated ${result.artistsUpdated}, wrote ${result.tagsWritten} tags, ${result.errors} errors"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        artistScanning = false,
                        artistScanResult = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearArtistResult() {
        _uiState.update { it.copy(artistScanResult = null) }
    }

    fun setAudioOffload(enabled: Boolean) {
        _uiState.update { it.copy(audioOffload = enabled) }
        viewModelScope.launch { equalizerManager.setEnableAudioOffload(enabled) }
    }

    fun setAudioFloatOutput(enabled: Boolean) {
        _uiState.update { it.copy(audioFloatOutput = enabled) }
        viewModelScope.launch { equalizerManager.setEnableAudioFloatOutput(enabled) }
    }

    fun setSkipSilence(enabled: Boolean) {
        _uiState.update { it.copy(skipSilence = enabled) }
        viewModelScope.launch { equalizerManager.setEnableSkipSilence(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

data class SettingsUiState(
    val generalTheme: String = GeneralTheme.AUTO,
    val blackTheme: Boolean = false,
    val materialYou: Boolean = true,
    val uiTheme: String = UITheme.MATERIAL,
    val eraHarmonyMode: Boolean = true,
    val eraShapeFamily: EraShapeFamily = EraShapeFamily.ROUNDED,
    val eraSurfaceMaterial: EraSurfaceMaterial = EraSurfaceMaterial.SOLID,
    val eraMotionIntensity: Int = 1,
    val appBarMode: String = AppBarMode.COMPACT,
    val lastfmScrobbling: Boolean = false,
    val searchQuery: String = "",
    val enableSyllableLyrics: Boolean = false,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val centerCurrentLine: Boolean = false,
    val centerHorizontally: Boolean = false,
    val useCustomFont: Boolean = false,
    val customFontPath: String? = null,
    val lineSpacing: Int = 40,
    val resumeOnSeek: Boolean = false,
    val syncedLyricsBold: Boolean = false,
    val syncedFontSizePlayer: Int = 20,
    val syncedFontSizeFull: Int = 24,
    val unsyncedLyricsBold: Boolean = false,
    val unsyncedFontSizePlayer: Int = 16,
    val unsyncedFontSizeFull: Int = 20,
    val lyricsAccentColor: Int = 0,
    val enableKaraokeStyle: Boolean = false,
    val progressiveColoring: Boolean = false,
    val aaMetadataLyrics: Boolean = false,
    val instrumentalTrackIdentifiers: String = "",
    val markInstrumentalTracksByTitle: Boolean = false,
    val ignoreBlankLinesInLyrics: Boolean = false,
    val preferredLyricsFileFormat: String = "ttml",
    val forceUtf8EncodingForLyrics: Boolean = true,
    val lyricsBackgroundEffect: String = "none",
    val lyricsShadowEffect: Boolean = true,
    val lyricsBlurEffect: Boolean = true,
    val seekInterval: Int = 10,
    val resumeOnConnect: Boolean = false,
    val whitelistEnabled: Boolean = true,
    val blacklistEnabled: Boolean = true,
    val nowPlayingScreen: com.mardous.projectmusic.core.model.theme.NowPlayingScreen = com.mardous.projectmusic.core.model.theme.NowPlayingScreen.Default,
    val vibrantBackgroundMode: com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode = com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode.Gradient,
    val vibrantLyricsBackgroundMode: com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode = com.mardous.projectmusic.core.model.theme.VibrantBackgroundMode.Fluid,
    val vibrantBackgroundAnimations: Boolean = true,
    val vibrantBackgroundHighQuality: Boolean = true,
    val vibrantBackgroundGlobal: Boolean = false,
    val carouselEffect: Boolean = false,
    val nowPlayingSmallImage: Boolean = false,
    val nowPlayingCornerRadius: Int = 32,
    val lyricsCardCornerRadius: Int = 32,
    val squigglySeekBar: Boolean = false,
    val adaptiveControls: Boolean = false,
    val circlePlayButton: Boolean = false,
    val displayAlbumTitle: Boolean = true,
    val displayNextSong: Boolean = true,
    val displayExtraInfo: Boolean = false,
    val miniPlayerSwipeToSkip: Boolean = true,
    val swipeDownToDismiss: Boolean = true,
    val swipeAnywhere: Boolean = false,
    val swipeUpQueue: Boolean = false,
    val swipeOnCover: Boolean = true,
    val animatePlayerControl: Boolean = true,
    val addExtraControls: Boolean = false,
    val queueHeight: Boolean = false,
    val enableScrollingText: Boolean = false,
    val preferRemainingTime: Boolean = false,
    val preferAlbumArtistName: Boolean = false,
    val vibrantBackgroundNoiseLevel: Int = 20,
    val playerBlurRadius: Int = 25,
    val openOnPlay: Boolean = false,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.LINEAR,
    val thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    val thumbSize: Float = 1.0f,
    val progressControlStyle: ProgressControlStyle = ProgressControlStyle.EXPRESSIVE,
    val coverSingleTapAction: NowPlayingAction = NowPlayingAction.TogglePlayState,
    val coverDoubleTapAction: NowPlayingAction = NowPlayingAction.WebSearch,
    val coverLeftDoubleTapAction: NowPlayingAction = NowPlayingAction.SeekBackward,
    val coverRightDoubleTapAction: NowPlayingAction = NowPlayingAction.SeekForward,
    val coverLongPressAction: NowPlayingAction = NowPlayingAction.SleepTimer,
    val eraShapeScale: Float = 1.0f,
    val eraContrast: Float = 0.0f,
    val eraTypeScale: Float = 1.0f,
    val eraPrimarySeed: Int = 0xFF4D5C92.toInt(),
    val eraSecondarySeed: Int = 0xFF595D72.toInt(),
    val eraTertiarySeed: Int = 0xFF75546F.toInt(),
    val eraErrorSeed: Int = 0xFFBA1A1A.toInt(),
    val eraAsymmetricShapes: Boolean = false,
    val eraVibrancy: Float = 1.0f,
    val eraFontFamily: EraFont = EraFont.GOOGLE_SANS,
    val rememberLastPage: Boolean = true,
    val tabTitlesMode: String = "selected",
    val holdTabToSearch: Boolean = true,
    val largerHeaderImage: Boolean = false,
    val widgetSmallLayoutStyle: String = "simplified",
    val widgetDynamicColors: Boolean = false,
    val widgetImageCornerRadius: Int = 8,
    val widgetThirdLineContent: String = "",
    val networkFeatures: Boolean = true,
    val wifiOnlyNetwork: Boolean = true,
    val onlineMusicProvider: String = "ytmusic",
    val allowOnlineArtistImages: Boolean = true,
    val allowOnlineAlbumCovers: Boolean = false,
    val preferredImageSize: String = "medium",
    val lrclibEnabled: Boolean = true,
    val betterLyricsEnabled: Boolean = false,
    val lyricallyEnabled: Boolean = false,
    val geniusEnabled: Boolean = false,
    val lyricsPlusEnabled: Boolean = false,
    val geniusApiKey: String = "",
    val lyricallyApiKey: String = "",
    val updateSearchMode: String = "weekly",
    val experimentalUpdates: Boolean = false,
    val lastfmNowPlaying: Boolean = false,
    val listenbrainzScrobbling: Boolean = false,
    val listenbrainzNowPlaying: Boolean = false,
    val languageName: String = "auto",
    val rotationLock: Boolean = false,
    val pauseOnZeroVolume: Boolean = false,
    val mp3IndexSeeking: Boolean = false,
    val stopWhenClosedFromRecents: Boolean = false,
    val ignoreMediaStore: Boolean = false,
    val useFolderArt: Boolean = false,
    val trashMusicFiles: Boolean = false,
    val ignoreArticlesWhenSorting: Boolean = false,
    val enableHistoryPlaylist: Boolean = true,
    val historyInterval: String = "this_month",
    val lastAddedInterval: String = "this_month",
    val recursiveFolderActions: Set<String> = emptySet(),
    val minimumSongDuration: Int = 15,
    val artistMinimumSongs: Int = 1,
    val albumMinimumSongs: Int = 1,
    val playOnStartupMode: String = "never",
    val queueNextMode: String = "1",
    val songClickAction: SongClickBehavior = SongClickBehavior.PlayWholeList,
    val clearQueueAction: QueueClearingBehavior = QueueClearingBehavior.RemoveAllSongs,
    val playOptionAlwaysVisible: Boolean = false,
    val playOptionWholeList: Boolean = false,
    val playAllSongsWhenSearching: Boolean = false,
    val clearQueueOnCompletion: Boolean = false,
    val rememberShuffleMode: Boolean = true,
    val albumShuffleMode: String = "shuffle_albums",
    val artistShuffleMode: String = "shuffle_all",
    val rewindWithBack: Boolean = true,
    val pauseOnDisconnect: Boolean = true,
    val resumeOnBluetoothConnect: Boolean = false,
    val pauseOnBluetoothDisconnect: Boolean = true,
    val ignoreAudioFocus: Boolean = false,
    val lockedPlaylists: Boolean = false,
    val horizontalArtistAlbums: Boolean = true,
    val compactAlbumSongView: Boolean = false,
    val compactArtistSongView: Boolean = false,
    val showLyricsOnCover: Boolean = false,
    val isQueueLocked: Boolean = false,
    val onlyAlbumArtists: Boolean = true,
    val ignoreSingles: Boolean = false,
    val showAlbumDuration: Boolean = false,
    val hierarchyFolderView: Boolean = false,
    val lastfmSyncFavorites: Boolean = false,
    val lastfmOfflineScrobbling: Boolean = true,
    val lastfmScrobblePercentage: Int = 50,
    val updateOnlyWifi: Boolean = false,
    val nowPlayingColorScheme: com.mardous.projectmusic.core.model.player.PlayerColorScheme.Mode = com.mardous.projectmusic.core.model.player.PlayerColorScheme.Mode.VibrantColor,
    val nowPlayingTransition: com.mardous.projectmusic.core.model.player.PlayerTransition = com.mardous.projectmusic.core.model.player.PlayerTransition.Simple,
    val lastfmInfoEnabled: Boolean = true,
    val musicbrainzEnabled: Boolean = false,
    val isUpdaterEnabled: Boolean = true,
    val musicbrainzScanning: Boolean = false,
    val musicbrainzScanResult: String? = null,
    val musicbrainzScanProgress: Int = 0,
    val musicbrainzScanTotal: Int = 0,
    val musicbrainzScanLabel: String? = null,
    val fileTagScanning: Boolean = false,
    val fileTagScanResult: String? = null,
    val fileTagScanProgress: Int = 0,
    val fileTagScanTotal: Int = 0,
    val fileTagScanLabel: String? = null,
    val artistScanning: Boolean = false,
    val artistScanResult: String? = null,
    val artistScanProgress: Int = 0,
    val artistScanTotal: Int = 0,
    val artistScanLabel: String? = null,
    val audioOffload: Boolean = false,
    val audioFloatOutput: Boolean = false,
    val skipSilence: Boolean = false
)
