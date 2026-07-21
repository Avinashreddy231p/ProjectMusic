package com.mardous.projectmusic.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.toBitmap
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mardous.projectmusic.R
import com.mardous.projectmusic.coil.CoilBitmapLoader
import com.mardous.projectmusic.core.appwidgets.ProjectMusicGlanceWidget
import com.mardous.projectmusic.core.appwidgets.CardWidget
import com.mardous.projectmusic.core.appwidgets.FullWidget
import com.mardous.projectmusic.core.appwidgets.WidgetTheme
import com.mardous.projectmusic.core.appwidgets.state.PlaybackState
import com.mardous.projectmusic.core.appwidgets.state.PlaybackStateDefinition
import com.mardous.projectmusic.core.audio.AudioOutputObserver
import com.mardous.projectmusic.core.model.player.MetadataField
import com.mardous.projectmusic.core.palette.PaletteProcessor
import com.mardous.projectmusic.data.local.MediaStoreObserver
import com.mardous.projectmusic.data.local.ReplayGainTagExtractor
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.network.NetworkFeature
import com.mardous.projectmusic.data.model.network.ScrobblingService
import com.mardous.projectmusic.extensions.isBluetoothA2dpConnected
import com.mardous.projectmusic.extensions.isBluetoothA2dpDisconnected
import com.mardous.projectmusic.extensions.media.displayArtistName
import com.mardous.projectmusic.extensions.media.songInfo
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.playback.equalizer.EqualizerManager
import com.mardous.projectmusic.playback.library.LibraryProvider
import com.mardous.projectmusic.playback.library.MediaIDs
import com.mardous.projectmusic.playback.processor.BalanceAudioProcessor
import com.mardous.projectmusic.playback.processor.ReplayGainAudioProcessor
import com.mardous.projectmusic.playback.renderer.AlacWorkaroundCodecSelector
import com.mardous.projectmusic.playback.renderer.ProjectMusicRenderersFactory
import com.mardous.projectmusic.playback.stats.DeviceContextCollector
import com.mardous.projectmusic.playback.stats.RealTimeStatsTracker
import com.mardous.projectmusic.playback.stats.SessionCoordinator
import com.mardous.projectmusic.playback.stats.StatsFlusher
import com.mardous.projectmusic.ui.screen.MainActivity
import com.mardous.projectmusic.util.CLEAR_QUEUE_ON_COMPLETION
import com.mardous.projectmusic.util.ENABLE_HISTORY
import com.mardous.projectmusic.util.IGNORE_AUDIO_FOCUS
import com.mardous.projectmusic.util.MP3_INDEX_SEEKING
import com.mardous.projectmusic.util.PAUSE_ON_ZERO_VOLUME
import com.mardous.projectmusic.util.PLAY_ON_STARTUP_MODE
import com.mardous.projectmusic.util.PlayOnStartupMode
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.requireString
import com.mardous.projectmusic.util.QUEUE_NEXT_MODE
import com.mardous.projectmusic.util.REWIND_WITH_BACK
import com.mardous.projectmusic.util.SEEK_INTERVAL
import com.mardous.projectmusic.util.STOP_WHEN_CLOSED_FROM_RECENTS
import com.mardous.projectmusic.util.SongPlayCountHelper
import com.mardous.projectmusic.util.WIDGET_DYNAMIC_COLORS
import com.mardous.projectmusic.util.WIDGET_IMAGE_CORNER_RADIUS
import com.mardous.projectmusic.util.WIDGET_SMALL_LAYOUT_STYLE
import com.mardous.projectmusic.util.WIDGET_THIRD_LINE_CONTENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import kotlin.random.Random

@OptIn(UnstableApi::class)
class PlaybackService :
    MediaLibraryService(),
    MediaLibrarySession.Callback,
    Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Main)
    private val uiHandler = Handler(Looper.getMainLooper())

    private val glanceManager by lazy { GlanceAppWidgetManager(applicationContext) }

    private val preferences: SharedPreferences by inject()
    private val sleepTimer: SleepTimer by inject()
    private val equalizerManager: EqualizerManager by inject()
    private val audioOutputObserver: AudioOutputObserver by inject()
    private val repository: Repository by inject()
    private val statsRepository: StatsRepository by inject()
    private val rankingEngine: com.mardous.projectmusic.data.local.database.sync.RankingEngine by inject()
    private val lyricsRepository: com.mardous.projectmusic.data.local.repository.LyricsRepository by inject()

    private val libraryProvider by lazy { LibraryProvider(repository, lyricsRepository) }
    private val songPlayCountHelper = SongPlayCountHelper()
    private val deviceContextCollector by lazy {
        DeviceContextCollector(applicationContext, audioOutputObserver, equalizerManager)
    }
    private val sessionCoordinator = SessionCoordinator()
    private val statsTracker by lazy {
        RealTimeStatsTracker(deviceContextCollector, sessionCoordinator)
    }
    private val statsFlusher by lazy { StatsFlusher(statsTracker, statsRepository, rankingEngine) }
    private val mediaStoreObserver = MediaStoreObserver(uiHandler) {
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_MEDIA_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private val playerThread = HandlerThread("ProjectMusic-ExoPlayer", Process.THREAD_PRIORITY_AUDIO)
    private val balanceProcessor: BalanceAudioProcessor by inject()
    private val replayGainProcessor: ReplayGainAudioProcessor by inject()
    private val beatProcessor: com.mardous.projectmusic.playback.processor.BeatAudioProcessor by inject()

    private lateinit var nm: NotificationManager
    private lateinit var persistentStorage: PersistentStorage
    private lateinit var customCommands: List<CommandButton>
    private lateinit var player: AdvancedForwardingPlayer
    private var mediaSession: MediaLibrarySession? = null

    private var isLyricsMetadataEnabled = false
    private var currentSyncedLyrics: com.mardous.projectmusic.data.model.lyrics.SyncedLyrics? = null
    private var lyricsMetadataUpdateJob: Job? = null
    private var lastLyricsLineIndex = -2

    private var eqStateHandler: Handler? = Handler(Looper.getMainLooper())

    private var errorRecoveryRetryCount = 0
    @Volatile
    private var pausedByZeroVolume = false
    private var hasSetUnshuffledOrder = false
    @Volatile
    private var isCurrentSongFavorite = false
    @Volatile
    private var stopIndex = -1

    private var lastTransitionMediaId: String? = null
    private var headsetClickCount = 0
    private val headsetClickRunnable = Runnable {
        if (!::player.isInitialized) return@Runnable
        val count = headsetClickCount
        headsetClickCount = 0
        when (count) {
            1 -> if (player.isPlaying) player.pause() else player.play()
            2 -> player.seekToNext()
            3 -> player.seekToPrevious()
        }
    }

    private var lastPlaybackState: PlaybackState? = null
    private var widgetUpdateJob: Job? = null
    private var fadeOutAnimator: ValueAnimator? = null

    val isInTransientFocusLoss: Boolean
        get() = if (::player.isInitialized) player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS else false

    val isPlaying: Boolean
        get() = if (::player.isInitialized) player.isPlaying else false

    private val shuffleCommand: CommandButton
        get() = if (::customCommands.isInitialized) {
            if (player.shuffleModeEnabled) customCommands[1] else customCommands[0]
        } else CommandButton.Builder(CommandButton.ICON_UNDEFINED).build()

    private val repeatCommand: CommandButton
        get() = if (::customCommands.isInitialized) {
            when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> customCommands[3]
                Player.REPEAT_MODE_ONE -> customCommands[4]
                else -> customCommands[2]
            }
        } else CommandButton.Builder(CommandButton.ICON_UNDEFINED).build()

    private val favoriteCommand: CommandButton
        get() = if (::customCommands.isInitialized) {
            if (isCurrentSongFavorite) customCommands[5] else customCommands[6]
        } else CommandButton.Builder(CommandButton.ICON_UNDEFINED).build()

    private val lyricsCommand: CommandButton
        get() = if (isLyricsMetadataEnabled) {
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(SessionCommand(Playback.SHOW_LYRICS, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_lyrics_24dp)
                .setDisplayName(getString(R.string.action_hide_lyrics))
                .build()
        } else {
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(SessionCommand(Playback.SHOW_LYRICS, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_lyrics_outline_24dp)
                .setDisplayName(getString(R.string.action_show_lyrics))
                .build()
        }

    private val pauseOnZeroVolume: Boolean
        get() = preferences.getBoolean(PAUSE_ON_ZERO_VOLUME, false)
    private val sequentialTimeline: Boolean
        get() = preferences.getString(QUEUE_NEXT_MODE, "1") == "1"
    private val handleAudioFocus: Boolean
        get() = preferences.getBoolean(IGNORE_AUDIO_FOCUS, false).not()
    private val maxSeekToPreviousMs: Long
        get() = if (preferences.getBoolean(REWIND_WITH_BACK, true)) REWIND_INSTEAD_PREVIOUS_MILLIS else 0
    private val seekInterval: Long
        get() = preferences.getInt(SEEK_INTERVAL, 10) * 1000L

    override fun onCreate() {
        super.onCreate()
        nm = requireNotNull(getSystemService<NotificationManager>())
        createNotificationChannel()

        customCommands = listOf(
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, true)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, false)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ALL)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ONE)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_OFF)
                .build(),
            CommandButton.Builder(CommandButton.ICON_HEART_FILLED)
                .setSessionCommand(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
                .setDisplayName(getString(R.string.action_remove_from_favorites))
                .build(),
            CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
                .setSessionCommand(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
                .setDisplayName(getString(R.string.action_add_to_favorites))
                .build(),
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(SessionCommand(Playback.SHOW_LYRICS, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_lyrics_24dp)
                .setDisplayName(getString(R.string.action_show_lyrics))
                .build()
        )

        playerThread.start()
        player = AdvancedForwardingPlayer(
            ExoPlayer.Builder(this)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), handleAudioFocus
                )
                .setRenderersFactory(
                    ProjectMusicRenderersFactory(this, balanceProcessor, replayGainProcessor, beatProcessor)
                        .setEnableAudioFloatOutput(equalizerManager.audioFloatOutput.value)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        .setMediaCodecSelector(AlacWorkaroundCodecSelector())
                        .setEnableDecoderFallback(true)
                )
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        this, DefaultExtractorsFactory()
                            .setConstantBitrateSeekingEnabled(true)
                            .also {
                                if (preferences.getBoolean(MP3_INDEX_SEEKING, false)) {
                                    it.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
                                }
                            }
                    )
                )
                .setSkipSilenceEnabled(equalizerManager.skipSilence.value)
                .setHandleAudioBecomingNoisy(true)
                .setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
                .setSeekBackIncrementMs(seekInterval)
                .setSeekForwardIncrementMs(seekInterval)
                .setPlaybackLooper(playerThread.looper)
                .build()
        )

        player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(0, 0, Random.nextLong())
        player.setSequentialTimelineEnabled(sequentialTimeline)
        player.addListener(this)

        mediaSession = with(MediaLibrarySession.Builder(this, player, this)) {
            setId(packageName)
            setSessionActivity(createSessionActivityIntent())
            setBitmapLoader(CacheBitmapLoader(CoilBitmapLoader(this@PlaybackService)))
            build()
        }

        setForegroundServiceTimeoutMs(FOREGROUND_SERVICE_TIMEOUT)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { _ -> NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.playing_notification_description
            ).apply {
                setSmallIcon(R.drawable.ic_stat_music_playback)
            }
        )

        mediaStoreObserver.init(this)

        persistentStorage = PersistentStorage(this, serviceScope, player)
        persistentStorage.restoreState { items, shuffleOrder ->
            player.setMediaItems(items.mediaItems, items.startIndex, items.startPositionMs)
            player.prepare()
            if (player.shuffleModeEnabled && shuffleOrder != null) {
                player.exoPlayer.shuffleOrder = shuffleOrder
            }
        }

        sleepTimer.addFinishListener { sleepParams ->
            if (player.playWhenReady && player.isPlaying) {
                statsFlusher.setEndReason("sleep_timer")
                if (sleepParams.pendingQuit) {
                    player.exoPlayer.pauseAtEndOfMediaItems = true
                } else {
                    if (sleepParams.fadeOut) {
                        launchMusicFadeOut(sleepParams.fadeDuration)
                    } else {
                        player.pause()
                    }
                }
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(this)
        audioOutputObserver.startObserver()

        statsFlusher.startPeriodicFlush()

        prepareEqualizerAndSoundSettings()
        registerReceivers()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        statsFlusher.flushAndStop("app_closed")
        if ((!isPlaybackOngoing && !isInTransientFocusLoss) ||
            preferences.getBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, false)) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statsFlusher.flushAndStop("service_destroyed")
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver)
            bluetoothConnectedRegistered = false
        }
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver)
            headsetReceiverRegistered = false
        }
        eqStateHandler?.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacks(headsetClickRunnable)
        serviceScope.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        audioOutputObserver.stopObserver()
        mediaStoreObserver.stop(this)
        mediaSession?.release()
        player.removeListener(this)
        player.release()
        playerThread.quitSafely()
        equalizerManager.release()
        sleepTimer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_FAVORITE -> {
                toggleFavorite()
                return START_STICKY
            }
            ACTION_TOGGLE_SHUFFLE -> {
                toggleShuffle()
                return START_STICKY
            }
            ACTION_CYCLE_REPEAT -> {
                cycleRepeat()
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        
        if (preferences.getBoolean("aa_metadata_lyrics", false)) {
            isLyricsMetadataEnabled = true
            startLyricsMetadataUpdate()
        }

        val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            .buildUpon()

        availableCommands.add(SessionCommand(Playback.CYCLE_REPEAT, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_SHUFFLE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SHOW_LYRICS, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.RESTORE_PLAYBACK, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_STOP_POSITION, Bundle.EMPTY))

        return MediaSession.ConnectionResult.accept(
            availableCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent
    ): Boolean {
        val ke = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        if (ke != null && (ke.keyCode == KeyEvent.KEYCODE_HEADSETHOOK || ke.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            if (ke.action == KeyEvent.ACTION_DOWN && ke.repeatCount == 0) {
                headsetClickCount++
                uiHandler.removeCallbacks(headsetClickRunnable)
                if (headsetClickCount >= 3) {
                    uiHandler.post(headsetClickRunnable)
                } else {
                    uiHandler.postDelayed(headsetClickRunnable, 300)
                }
            }
            return true
        }
        return super.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        equalizerManager.setSessionId(audioSessionId)
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val libraryParams = LibraryParams.Builder()
            .setOffline(true)
            .setRecent(true)
            .setSuggested(false)
            .build()
        val mediaItem = when {
            params?.isRecent == true -> {
                MediaItem.Builder()
                    .setMediaId(MediaIDs.RECENT_SONGS)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
            else -> {
                MediaItem.Builder()
                    .setMediaId(MediaIDs.ROOT)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
        }
        return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, libraryParams))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.future(IO) {
            val result = runCatching {
                libraryProvider.getChildren(this@PlaybackService, parentId, player.currentMediaItem)
            }
            if (result.isSuccess) {
                LibraryResult.ofItemList(result.getOrThrow(), params)
            } else {
                LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
            }
        }
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return serviceScope.future(IO) {
            val mediaItem = runCatching { libraryProvider.getItem(mediaId) }
                .getOrDefault(MediaItem.EMPTY)
            if (mediaItem != MediaItem.EMPTY) {
                LibraryResult.ofItem(mediaItem, null)
            } else {
                LibraryResult.ofError(SessionError.ERROR_IO)
            }
        }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        return serviceScope.future(IO) {
            runCatching { libraryProvider.search(query) }
                .onSuccess { session.notifySearchResultChanged(browser, query, it.size, params) }

            LibraryResult.ofVoid()
        }
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return Futures.immediateFuture(
            LibraryResult.ofItemList(libraryProvider.searchResult, params)
        )
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        return serviceScope.future(IO) {
            runCatching { libraryProvider.getMediaItemsForPlayback(mediaItems) }
                .getOrDefault(emptyList())
        }
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaItemsWithStartPosition> {
        player.exoPlayer.let { exoPlayer ->
            if (exoPlayer.shuffleOrder !is ImprovedShuffleOrder && !hasSetUnshuffledOrder) {
                exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                    firstIndex = player.currentMediaItemIndex,
                    length = player.mediaItemCount,
                    randomSeed = Random.nextLong()
                )
            }

            (exoPlayer.shuffleOrder as? ImprovedShuffleOrder)
                ?.playerIndex = startIndex

            hasSetUnshuffledOrder = false
        }
        return serviceScope.future(IO) {
            if (mediaSession.isAutomotiveController(controller) ||
                mediaSession.isAutoCompanionController(controller)) {
                runCatching { libraryProvider.getMediaItemsForAAOSPlayback(mediaItems) }
                    .getOrNull()
                    .let {
                        MediaItemsWithStartPosition(
                            it?.first ?: emptyList(),
                            it?.second ?: C.INDEX_UNSET,
                            startPositionMs
                        )
                    }
            } else {
                runCatching {
                    libraryProvider.getMediaItemsForPlayback(
                        mediaItems = mediaItems,
                        tryToResolveComplexPaths = true
                    )
                }.getOrDefault(emptyList()).let {
                    MediaItemsWithStartPosition(it, startIndex, startPositionMs)
                }
            }
        }.also { future ->
            future.addListener({
                val result = runCatching { future.get() }.getOrNull()
                if (result != null && result.mediaItems.isNotEmpty()) {
                    this.mediaSession?.broadcastCustomCommand(
                        SessionCommand(Playback.EVENT_PLAYBACK_STARTED, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            Playback.TOGGLE_SHUFFLE -> {
                toggleShuffle()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.CYCLE_REPEAT -> {
                cycleRepeat()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.TOGGLE_FAVORITE -> {
                toggleFavorite()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SHOW_LYRICS -> {
                isLyricsMetadataEnabled = !isLyricsMetadataEnabled
                if (isLyricsMetadataEnabled) {
                    startLyricsMetadataUpdate()
                } else {
                    stopLyricsMetadataUpdate()
                }
                
                // Persist the state if we want it to be remembered
                preferences.edit {
                    putBoolean("aa_metadata_lyrics", isLyricsMetadataEnabled)
                }
                
                refreshMediaButtonCustomLayout()
                mediaSession?.broadcastCustomCommand(
                    SessionCommand(Playback.EVENT_SHOW_LYRICS, Bundle.EMPTY),
                    Bundle.EMPTY
                )
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.RESTORE_PLAYBACK -> {
                val playOnStartupMode = preferences.requireString(PLAY_ON_STARTUP_MODE, PlayOnStartupMode.NEVER)
                if (playOnStartupMode != PlayOnStartupMode.NEVER) {
                    CallbackToFutureAdapter.getFuture { completer ->
                        persistentStorage.waitForRestoration {
                            if (!player.currentTimeline.isEmpty) {
                                mediaSession?.broadcastCustomCommand(
                                    SessionCommand(
                                        Playback.EVENT_PLAYBACK_RESTORED,
                                        Bundle.EMPTY
                                    ),
                                    Bundle.EMPTY
                                )
                                completer.set(SessionResult(SessionResult.RESULT_SUCCESS))
                            } else {
                                completer.setException(IllegalStateException("Timeline is empty"))
                            }
                        }
                    }
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                }
            }

            Playback.SET_UNSHUFFLED_ORDER -> {
                hasSetUnshuffledOrder = true
                player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(player.mediaItemCount)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SET_STOP_POSITION -> {
                val newStopIndex = customCommand.customExtras.getInt("index", -1)
                val canceled = newStopIndex > -1 && newStopIndex == stopIndex
                if (canceled) {
                    player.exoPlayer.pauseAtEndOfMediaItems = false
                    stopIndex = -1
                } else if (newStopIndex == player.currentMediaItemIndex) {
                    player.exoPlayer.pauseAtEndOfMediaItems = true
                    stopIndex = -1
                } else {
                    player.exoPlayer.pauseAtEndOfMediaItems = false
                    stopIndex = newStopIndex
                }
                Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply {
                        putBoolean("canceled", canceled)
                    })
                )
            }

            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean
    ): ListenableFuture<MediaItemsWithStartPosition> {
        if (persistentStorage.restorationState.isRestored) {
            return Futures.immediateFailedFuture(IllegalStateException("No MediaItems saved"))
        } else {
            val settableFuture = SettableFuture.create<MediaItemsWithStartPosition>()
            persistentStorage.waitForMediaItems { items, shuffleOrder ->
                if (items.mediaItems.isNotEmpty()) {
                    if (player.shuffleModeEnabled && shuffleOrder != null) {
                        player.exoPlayer.shuffleOrder = shuffleOrder
                    }
                    settableFuture.set(items)
                } else {
                    settableFuture.setException(IllegalStateException("No MediaItems saved"))
                }
            }
            return settableFuture
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (player.playbackState == Player.STATE_ENDED &&
            preferences.getBoolean(CLEAR_QUEUE_ON_COMPLETION, false)) {
            player.exoPlayer.clearMediaItems()
        }
        refreshMediaButtonCustomLayout()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        persistentStorage.saveState(true)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
            player.exoPlayer.pauseAtEndOfMediaItems = false
            sleepTimer.consumePendingQuit()
            if (stopIndex == player.currentMediaItemIndex) {
                stopIndex = -1
            }
        }
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
            statsFlusher.setEndReason("audio_focus_loss")
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            val currentDurationMs = player.mediaMetadata.durationMs ?: 0
            if (currentDurationMs > 0) {
                if (!player.currentTimeline.isEmpty) {
                    persistentStorage.saveState()
                }
            }
        }
        songPlayCountHelper.notifyPlayStateChanged(isPlaying)
        statsFlusher.onPlayPause(isPlaying)
        updateWidgets()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            statsFlusher.onSeek(oldPosition.positionMs, newPosition.positionMs)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateWidgets()
        refreshMediaButtonCustomLayout()
        persistentStorage.saveState()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateWidgets()
        refreshMediaButtonCustomLayout()
        persistentStorage.saveState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem?.mediaId == lastTransitionMediaId) return
        lastTransitionMediaId = mediaItem?.mediaId
        lastLyricsLineIndex = -2

        val isPlaying = player.isPlaying
        val currentPositionMs = player.currentPosition

        val repeatModeStr = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> "one"
            Player.REPEAT_MODE_ALL -> "all"
            else -> "off"
        }
        val shuffleEnabled = player.shuffleModeEnabled
        val queuePosition = player.currentMediaItemIndex

        serviceScope.launch(IO) {
            val newSong = repository.songByMediaItem(mediaItem)
            val previousSong = songPlayCountHelper.song

            val shouldBumpPlayCount = songPlayCountHelper.shouldBumpPlayCount()
            songPlayCountHelper.notifySongChanged(newSong, isPlaying)

            val isFavorite = if (newSong != Song.emptySong) {
                runCatching { repository.isSongFavorite(newSong.id) }.getOrDefault(false)
            } else false

            isCurrentSongFavorite = isFavorite

            // Pre-fetch synced lyrics for metadata if enabled
            launch {
                val rawLyrics = lyricsRepository.fileLyrics(newSong)
                    ?: lyricsRepository.embeddedLyrics(newSong)
                    ?: lyricsRepository.storedLyrics(newSong, false)
                currentSyncedLyrics = rawLyrics?.let { lyricsRepository.parseRawLyrics(newSong, it) }

                if (isLyricsMetadataEnabled) {
                    startLyricsMetadataUpdate()
                }
            }

            val extras = mediaItem?.mediaMetadata?.extras
            val queueSource = extras?.getString(Playback.EXTRA_QUEUE_SOURCE) ?: ""
            val playlistId = extras?.getString(Playback.EXTRA_PLAYLIST_ID) ?: ""
            val playlistName = extras?.getString(Playback.EXTRA_PLAYLIST_NAME) ?: ""
            val playbackOrigin = extras?.getString(Playback.EXTRA_PLAYBACK_ORIGIN) ?: "unknown"

            statsFlusher.setPlaybackContext(
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatModeStr,
                queuePosition = queuePosition,
                queueSource = queueSource,
                playbackOrigin = playbackOrigin,
                playlistId = playlistId,
                playlistName = playlistName,
                isFavorite = isFavorite
            )

            statsFlusher.onSongTransition(
                currentSong = previousSong,
                nextSong = newSong,
                currentPositionMs = currentPositionMs,
                transitionReason = transitionReasonToString(reason),
                maxDurationMs = previousSong.duration
            )

            if (newSong != Song.emptySong) {
                statsFlusher.onSongStarted(newSong, 0L)
                launch {
                    runCatching { rankingEngine.registerSong(newSong) }
                        .onFailure { Log.e(TAG, "Failed to register song in ranking engine", it) }
                }
                replayGainProcessor.currentGain = ReplayGainTagExtractor.getReplayGain(newSong)
                if (preferences.getBoolean(ENABLE_HISTORY, true)) {
                    repository.upsertSongInHistory(newSong)
                }
                if (NetworkFeature.Lastfm.NowPlaying.isAvailable) {
                    launch {
                        runCatching { repository.updateNowPlaying(ScrobblingService.Lastfm, newSong) }
                            .onFailure { Log.e(TAG, "Failed to update Last.fm now playing", it) }
                    }
                }
                if (NetworkFeature.ListenBrainz.NowPlaying.isAvailable) {
                    launch {
                        runCatching { repository.updateNowPlaying(ScrobblingService.ListenBrainz, newSong) }
                            .onFailure { Log.e(TAG, "Failed to update ListenBrainz now playing", it) }
                    }
                }
            }
            withContext(Main) {
                refreshMediaButtonCustomLayout()
            }
            if (previousSong != Song.emptySong) {
                val timestampMillis = System.currentTimeMillis()
                val timestampSeconds = (timestampMillis / 1000)
                if (shouldBumpPlayCount) {
                    repository.insertOrIncrementPlayCount(
                        song = previousSong,
                        timePlayed = timestampMillis
                    )
                    if (NetworkFeature.Lastfm.Scrobbling.isAvailable) {
                        launch {
                            runCatching { repository.scrobble(ScrobblingService.Lastfm, previousSong, timestampSeconds) }
                                .onFailure { Log.e(TAG, "Failed to scrobble to Last.fm", it) }
                        }
                    }
                    if (NetworkFeature.ListenBrainz.Scrobbling.isAvailable) {
                        launch {
                            runCatching { repository.scrobble(ScrobblingService.ListenBrainz, previousSong, timestampSeconds) }
                                .onFailure { Log.e(TAG, "Failed to scrobble to ListenBrainz", it) }
                        }
                    }
                } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    repository.insertOrIncrementSkipCount(previousSong)
                }
            }
        }

        if (player.currentMediaItemIndex == stopIndex) {
            player.exoPlayer.pauseAtEndOfMediaItems = true
        }

        persistentStorage.saveState()
        updateWidgets(force = true)
    }

    override fun onPlayerError(error: PlaybackException) {
        val nextMediaIndex = player.nextMediaItemIndex
        if (nextMediaIndex != C.INDEX_UNSET &&
            errorRecoveryRetryCount < MAX_RETRY_COUNT_AFTER_ERROR) {
            errorRecoveryRetryCount++
            player.seekToNextMediaItem()
            player.prepare()
        }
        showToast(getString(R.string.playback_error_code, error.errorCodeName))
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
            events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
            events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            if (player.isPlaying) errorRecoveryRetryCount = 0
            cancelSleepTimerFadeOut()
        }
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) &&
            !events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            updateEqualizerSessionState(player.isPlaying)
        }
        if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) &&
            !events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            if (player.shuffleModeEnabled && persistentStorage.restorationState.isRestored) {
                this.player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                    firstIndex = player.currentMediaItemIndex,
                    length = player.mediaItemCount,
                    randomSeed = Random.nextLong()
                )
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        var sampleRate = -1
        var channelCount = -1
        var bitrate = 0
        var format = ""
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val trackFormat = group.getTrackFormat(i)
                        sampleRate = trackFormat.sampleRate
                        channelCount = trackFormat.channelCount
                        bitrate = if (trackFormat.bitrate > 0) trackFormat.bitrate / 1000 else 0
                        format = trackFormat.sampleMimeType ?: ""
                        break
                    }
                }
            }
        }
        audioOutputObserver.updatePlaybackFormat(sampleRate, channelCount, bitrate, format)
    }


    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            QUEUE_NEXT_MODE -> {
                player.setSequentialTimelineEnabled(sequentialTimeline)
            }

            ENABLE_HISTORY -> {
                if (!preferences.getBoolean(key, true)) {
                    serviceScope.launch(IO) {
                        repository.clearSongHistory()
                    }
                }
            }

            IGNORE_AUDIO_FOCUS -> {
                player.setAudioAttributes(player.audioAttributes, handleAudioFocus)
            }

            REWIND_WITH_BACK -> {
                player.exoPlayer.setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
            }

            SEEK_INTERVAL -> {
                player.exoPlayer.setSeekBackIncrementMs(seekInterval)
                player.exoPlayer.setSeekForwardIncrementMs(seekInterval)
            }

            WIDGET_DYNAMIC_COLORS,
            WIDGET_SMALL_LAYOUT_STYLE,
            WIDGET_IMAGE_CORNER_RADIUS,
            WIDGET_THIRD_LINE_CONTENT -> {
                updateWidgets()
            }

            "aa_metadata_lyrics" -> {
                val enabled = preferences.getBoolean(key, false)
                isLyricsMetadataEnabled = enabled
                if (enabled) {
                    startLyricsMetadataUpdate()
                } else {
                    stopLyricsMetadataUpdate()
                }
                refreshMediaButtonCustomLayout()
            }
        }
    }

    private fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    private fun cycleRepeat() {
        val currentRepeatMode = player.repeatMode
        player.repeatMode = when (currentRepeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun toggleFavorite() = serviceScope.launch {
        val currentMediaItem = player.currentMediaItem
            ?: return@launch

        withContext(IO) {
            val song = repository.songByMediaItem(currentMediaItem)
            repository.toggleFavorite(song)
            isCurrentSongFavorite = repository.isSongFavorite(song.id)
        }

        updateWidgets()

        refreshMediaButtonCustomLayout()
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_FAVORITE_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private suspend fun buildPlaybackState(isForeground: Boolean): PlaybackState {
        val mediaItem = player.currentMediaItem
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (mediaItem == null || id == null) return PlaybackState.empty

        val isPlaying = player.isPlaying
        val isShuffleMode = player.shuffleModeEnabled
        val repeatMode = player.repeatMode
        return withContext(IO) {
            val song = repository.songById(id)
            val isFavorite = repository.isSongFavorite(song.id)
            val result = SingletonImageLoader.get(this@PlaybackService).execute(
                ImageRequest.Builder(this@PlaybackService)
                    .data(song)
                    .scale(Scale.FILL)
                    .size(300)
                    .build()
            )
            val bitmap = result.image?.toBitmap(300, 300)
            val artworkData = bitmap?.let {
                val stream = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
                } else {
                    it.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
                stream.toByteArray()
            }
            val widgetTheme = if (preferences.getBoolean(WIDGET_DYNAMIC_COLORS, false)) {
                val paletteColor = bitmap?.let {
                    PaletteProcessor.getPaletteColor(this@PlaybackService, bitmap)
                }
                if (paletteColor != null) {
                    WidgetTheme(paletteColor.primaryColor)
                } else null
            } else null
            val additionalInfo = MetadataField.getMetadataValue(
                song = song,
                fields = Preferences.getExtraInfoContent(
                    key = WIDGET_THIRD_LINE_CONTENT,
                    defaultContent = Preferences.getDefaultWidgetInfo()
                )
            )
            PlaybackState(
                isSimplifiedSmallLayout = preferences.getString(WIDGET_SMALL_LAYOUT_STYLE, null) == "simplified",
                isForeground = isForeground,
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                isShuffleMode = isShuffleMode,
                repeatMode = repeatMode,
                currentTitle = song.title,
                currentArtist = song.artistName,
                additionalInfo = additionalInfo,
                artworkData = artworkData,
                widgetTheme = widgetTheme,
                imageCornerRadius = preferences.getInt(WIDGET_IMAGE_CORNER_RADIUS, 8).toFloat()
            )
        }
    }

    private fun updateWidgets(force: Boolean = false, isForeground: Boolean = isPlaybackOngoing) {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            if (!force) delay(WIDGET_UPDATE_DEBOUNCE)

            val state = buildPlaybackState(isForeground)
            if (lastPlaybackState != state) {
                lastPlaybackState = state
                updateGlanceWidgets(state)
            }
        }
    }

    private suspend fun updateGlanceWidgets(playbackState: PlaybackState) = withContext(IO) {
        try {
            val projectMusicWidget = ProjectMusicGlanceWidget()
            val projectMusicWidgetIds = glanceManager.getGlanceIds(projectMusicWidget.javaClass)
            if (projectMusicWidgetIds.isNotEmpty()) {
                projectMusicWidgetIds.forEach { id ->
                    updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                        playbackState
                    }
                    projectMusicWidget.update(applicationContext, id)
                }
            }

            val cardWidget = CardWidget()
            val cardWidgetIds = glanceManager.getGlanceIds(cardWidget.javaClass)
            if (cardWidgetIds.isNotEmpty()) {
                cardWidgetIds.forEach { id ->
                    updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                        playbackState
                    }
                    cardWidget.update(applicationContext, id)
                }
            }

            val fullWidget = FullWidget()
            val fullWidgetIds = glanceManager.getGlanceIds(fullWidget.javaClass)
            if (fullWidgetIds.isNotEmpty()) {
                fullWidgetIds.forEach { id ->
                    updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                        playbackState
                    }
                    fullWidget.update(applicationContext, id)
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackService", "Couldn't update Glance widgets", e)
        }
    }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        var notificationChannel = nm.getNotificationChannel(CHANNEL_ID)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playing_notification_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playing_notification_description)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                    setShowBadge(false)
                }
            }
            nm.createNotificationChannel(notificationChannel)
        }
    }

    private fun refreshMediaButtonCustomLayout() {
        val hasTimeline = !player.currentTimeline.isEmpty
        val buttonLayout = mutableListOf<CommandButton>()
        if (hasTimeline) {
            buttonLayout.add(repeatCommand)
            buttonLayout.add(lyricsCommand)
        }
        buttonLayout.add(favoriteCommand)

        val immutableButtonLayout = ImmutableList.copyOf(buttonLayout)
        mediaSession?.setCustomLayout(immutableButtonLayout)

        mediaSession?.connectedControllers?.forEach { controllerInfo ->
            if (mediaSession?.isRemoteController(controllerInfo) == true) {
                mediaSession?.setMediaButtonPreferences(controllerInfo, immutableButtonLayout)
            }
        }
    }

    private fun launchMusicFadeOut(durationMs: Long = 1000) {
        cancelSleepTimerFadeOut()

        fadeOutAnimator = ValueAnimator.ofFloat(player.volume, 0f).apply {
            duration = durationMs
            addUpdateListener { animation ->
                player.volume = animation.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    restorePlayerVolume()
                }

                override fun onAnimationEnd(animation: Animator) {
                    player.pause()
                    restorePlayerVolume()
                }
            })
        }
        fadeOutAnimator?.start()
    }

    private fun cancelSleepTimerFadeOut() {
        fadeOutAnimator?.cancel()
        fadeOutAnimator = null

        restorePlayerVolume()
    }

    private fun restorePlayerVolume() {
        player.volume = equalizerManager.volumeState.value.currentVolume
    }

    private fun prepareEqualizerAndSoundSettings() {
        serviceScope.launch {
            equalizerManager.initializeEqualizer()
        }
        serviceScope.launch {
            equalizerManager.volumeState.collect { volume ->
                cancelSleepTimerFadeOut()
                player.volume = volume.currentVolume
            }
        }
        serviceScope.launch {
            equalizerManager.audioOffload.collect { audioOffloadingEnabled ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(
                        AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(
                                if (audioOffloadingEnabled)
                                    AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                                else AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                            )
                            .setIsSpeedChangeSupportRequired(true)
                            .build()
                    )
                    .build()
            }
        }
        serviceScope.launch {
            equalizerManager.skipSilence.collect {
                player.exoPlayer.skipSilenceEnabled = it
            }
        }
        serviceScope.launch {
            equalizerManager.tempoState.collect {
                player.playbackParameters = PlaybackParameters(it.speed, it.actualPitch)
            }
        }
        serviceScope.launch {
            audioOutputObserver.systemVolumeState.collect { systemVolume ->
                if (pauseOnZeroVolume && persistentStorage.restorationState.isRestored) {
                    // don't handle volume changes until our player is fully restored
                    if (isPlaying && systemVolume.currentVolume <= 0f) {
                        player.pause()
                        pausedByZeroVolume = true
                    } else if (pausedByZeroVolume && systemVolume.currentVolume >= 0.1f) {
                        player.play()
                        pausedByZeroVolume = false
                    }
                }
            }
        }
    }

    private fun updateEqualizerSessionState(isPlaying: Boolean) {
        eqStateHandler?.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacks(headsetClickRunnable)
        if (isPlaying) {
            equalizerManager.setSessionIsActive(true)
        } else {
            eqStateHandler?.postDelayed(500) {
                equalizerManager.setSessionIsActive(false)
            }
        }
    }

    private fun registerReceivers() {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_NOT_EXPORTED
        } else {
            ContextCompat.RECEIVER_EXPORTED
        }
        if (!bluetoothConnectedRegistered) {
            ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothConnectedIntentFilter, flags)
            bluetoothConnectedRegistered = true
        }

        if (!headsetReceiverRegistered) {
            ContextCompat.registerReceiver(this, headsetReceiver, headsetReceiverIntentFilter, flags)
            headsetReceiverRegistered = true
        }
    }

    private fun startLyricsMetadataUpdate() {
        lyricsMetadataUpdateJob?.cancel()
        lyricsMetadataUpdateJob = serviceScope.launch {
            while (isActive) {
                updateMetadataWithLyrics()
                delay(500) // Update frequency
            }
        }
    }

    private fun stopLyricsMetadataUpdate() {
        lyricsMetadataUpdateJob?.cancel()
        lyricsMetadataUpdateJob = null
        lastLyricsLineIndex = -2
        restoreOriginalMetadata()
    }

    private suspend fun updateMetadataWithLyrics() {
        val currentPosition = player.currentPosition
        val lyrics = currentSyncedLyrics
        val currentSong = player.currentSong
        
        val currentLine: String
        val nextLine: String
        val lineIndex: Int

        if (lyrics != null && lyrics.hasContent) {
            lineIndex = findLineIndexAt(currentPosition, lyrics)
            if (lineIndex == lastLyricsLineIndex) return
            
            val lineText = if (lineIndex in lyrics.lines.indices) {
                lyrics.lines[lineIndex].content.content
            } else {
                currentSong.title
            }

            currentLine = "▶ ${lineText.uppercase()}"

            nextLine = if (lineIndex + 1 in lyrics.lines.indices) {
                lyrics.lines[lineIndex + 1].content.content
            } else {
                currentSong.displayArtistName()
            }
        } else {
            lineIndex = -1
            if (lineIndex == lastLyricsLineIndex) return
            currentLine = currentSong.title
            nextLine = currentSong.displayArtistName()
        }

        lastLyricsLineIndex = lineIndex

        withContext(Main) {
            val currentItem = player.currentMediaItem
            if (currentItem != null && isLyricsMetadataEnabled) {
                val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                    .setTitle(currentLine)
                    .setArtist(nextLine)
                    .setSubtitle(nextLine)
                    .build()
                val updatedItem = currentItem.buildUpon()
                    .setMediaMetadata(updatedMetadata)
                    .build()
                if (player.currentMediaItemIndex != C.INDEX_UNSET) {
                    player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
                }
            }
        }
    }

    private fun restoreOriginalMetadata() {
        serviceScope.launch {
            val currentItem = player.currentMediaItem
            val currentSong = player.currentSong
            if (currentItem != null && currentSong != Song.emptySong) {
                val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                    .setTitle(currentSong.title)
                    .setArtist(currentSong.displayArtistName())
                    .setSubtitle(currentSong.songInfo())
                    .build()
                val updatedItem = currentItem.buildUpon()
                    .setMediaMetadata(updatedMetadata)
                    .build()
                withContext(Main) {
                    if (player.currentMediaItemIndex != C.INDEX_UNSET) {
                        player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
                    }
                }
            }
        }
    }

    private fun findLineIndexAt(position: Long, lyrics: com.mardous.projectmusic.data.model.lyrics.SyncedLyrics): Int {
        if (position < 0) return -1
        val lines = lyrics.lines
        for (i in lines.lastIndex downTo 0) {
            if (position >= lines[i].start) {
                return i
            }
        }
        return -1
    }

    private var bluetoothConnectedRegistered = false
    private val bluetoothConnectedIntentFilter = IntentFilter().apply {
        addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                        BluetoothA2dp.STATE_CONNECTED -> if (Preferences.isResumeOnConnect(true)) {
                            player.play()
                        }
                        BluetoothA2dp.STATE_DISCONNECTED -> {
                            statsFlusher.setEndReason("bluetooth_disconnect")
                            if (Preferences.isPauseOnDisconnect(true)) {
                                player.pause()
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED ->
                    if (context.isBluetoothA2dpConnected() && Preferences.isResumeOnConnect(true)) {
                        player.play()
                    }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    statsFlusher.setEndReason("bluetooth_disconnect")
                    if (context.isBluetoothA2dpDisconnected() && Preferences.isPauseOnDisconnect(true)) {
                        player.pause()
                    }
                }
            }
        }
    }

    private var receivedHeadsetConnected = false
    private var headsetReceiverRegistered = false
    private val headsetReceiverIntentFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_HEADSET_PLUG == intent.action && !isInitialStickyBroadcast) {
                when (intent.getIntExtra("state", -1)) {
                    0 -> if (Preferences.isPauseOnDisconnect(false)) {
                        player.pause()
                    }
                    // Check whether the current song is empty which means the playing queue hasn't restored yet
                    1 -> if (Preferences.isResumeOnConnect(false)) {
                        if (player.currentMediaItem != null) {
                            player.play()
                        } else {
                            receivedHeadsetConnected = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val PACKAGE_NAME = "com.mardous.projectmusic"

        const val ACTION_TOGGLE_SHUFFLE = "$PACKAGE_NAME.action.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "$PACKAGE_NAME.action.ACTION_CYCLE_REPEAT"
        const val ACTION_TOGGLE_FAVORITE = "$PACKAGE_NAME.action.ACTION_TOGGLE_FAVORITE"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "playing_notification"

        private const val MAX_RETRY_COUNT_AFTER_ERROR = 3
        private const val WIDGET_UPDATE_DEBOUNCE = 300L
        private const val REWIND_INSTEAD_PREVIOUS_MILLIS = 5000L

        private const val FOREGROUND_SERVICE_TIMEOUT = (60 * 1000) * 2L
    }
}

private fun transitionReasonToString(reason: Int): String {
    return when (reason) {
        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "seek"
        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "repeat"
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "playlist_changed"
        else -> "track_finished"
    }
}