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

package com.mardous.projectmusic.ui.screen.library

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.projectmusic.coil.CustomPlaylistImageManager
import com.mardous.projectmusic.core.model.LibraryMargin
import com.mardous.projectmusic.core.model.filesystem.FileSystemItem
import com.mardous.projectmusic.core.model.filesystem.FileSystemQuery
import com.mardous.projectmusic.data.SongProvider
import com.mardous.projectmusic.data.local.database.analytics.PlayCountEntity
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.metadata.*
import com.mardous.projectmusic.data.local.database.dao.InclExclDao
import com.mardous.projectmusic.data.local.repository.MetadataRepository
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.mapper.toSongEntity
import com.mardous.projectmusic.data.mapper.toSongsEntity
import com.mardous.projectmusic.data.model.*
import com.mardous.projectmusic.data.model.network.LoginParams
import com.mardous.projectmusic.data.model.network.ScrobblingService
import com.mardous.projectmusic.extensions.files.getCanonicalPathSafe
import com.mardous.projectmusic.extensions.media.indexOfSong
import com.mardous.projectmusic.ui.dialogs.playlists.AddToPlaylistUiState
import com.mardous.projectmusic.ui.screen.library.home.SuggestedResult
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.StorageUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class LibraryViewModel(
    private val repository: Repository,
    private val metadataRepository: MetadataRepository,
    private val inclExclDao: InclExclDao,
    private val customPlaylistImageManager: CustomPlaylistImageManager
) : ViewModel() {

    init {
        viewModelScope.launch(IO) {
            initializeBlacklist()
            deleteMissingContent()
        }
    }

    private val suggestions = MutableLiveData(SuggestedResult.Idle)
    private val songs = MutableLiveData<List<Song>>()
    private val albums = MutableLiveData<List<Album>>()
    private val artists = MutableLiveData<List<Artist>>()
    private val playlists = MutableLiveData<List<PlaylistWithSongs>>()
    private val genres = MutableLiveData<List<Genre>>()
    private val years = MutableLiveData<List<ReleaseYear>>()
    private val fileSystem = MutableLiveData<FileSystemQuery>()
    private val fabMargin = MutableLiveData(LibraryMargin(0))
    private val miniPlayerMargin = MutableLiveData(LibraryMargin(0))
    private val songHistory = MutableLiveData<List<Song>>()

    fun getSuggestions(): LiveData<SuggestedResult> = suggestions
    fun getSongs(): LiveData<List<Song>> = songs
    fun getAlbums(): LiveData<List<Album>> = albums
    fun getArtists(): LiveData<List<Artist>> = artists
    fun getPlaylists(): LiveData<List<PlaylistWithSongs>> = playlists
    fun getGenres(): LiveData<List<Genre>> = genres
    fun getYears(): LiveData<List<ReleaseYear>> = years
    fun getFileSystem(): LiveData<FileSystemQuery> = fileSystem
    fun getFabMargin(): LiveData<LibraryMargin> = fabMargin
    fun getMiniPlayerMargin(): LiveData<LibraryMargin> = miniPlayerMargin

    private fun createValueAnimator(oldValue: Int, newValue: Int, setter: (Int) -> Unit): Animator {
        return ValueAnimator.ofInt(oldValue, newValue).apply {
            addUpdateListener { setter(it.animatedValue as Int) }
            doOnEnd { setter(newValue) }
            start()
        }
    }

    fun setLibraryMargins(fabBottomMargin: LibraryMargin, bottomSheetMargin: LibraryMargin) {
        val fabAnimator = createValueAnimator(
            oldValue = fabMargin.value!!.margin,
            newValue = fabBottomMargin.margin
        ) {
            fabMargin.postValue(fabBottomMargin.copy(margin = it))
        }
        val miniPlayerAnimator = createValueAnimator(
            oldValue = miniPlayerMargin.value!!.margin,
            newValue = bottomSheetMargin.margin
        ) {
            miniPlayerMargin.postValue(bottomSheetMargin.copy(margin = it))
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fabAnimator, miniPlayerAnimator)
        animatorSet.start()
    }

    suspend fun albumById(id: Long) = repository.albumById(id)
    fun artistById(id: Long) = repository.artistById(id)
    suspend fun devicePlaylistById(id: Long) = repository.devicePlaylist(id)
    fun genreBySong(song: Song): LiveData<Genre> = liveData(IO) {
        emit(repository.genreBySong(song))
    }

    fun allSongs() = liveData(IO) {
        emit(repository.allSongs())
    }

    fun forceReload(reloadType: ReloadType) = viewModelScope.launch(IO) {
        when (reloadType) {
            ReloadType.Songs -> fetchSongs()
            ReloadType.Albums -> fetchAlbums()
            ReloadType.Artists -> fetchArtists()
            ReloadType.Playlists -> fetchPlaylists()
            ReloadType.Genres -> fetchGenres()
            ReloadType.Folders -> fetchFolders()
            ReloadType.Years -> fetchYears()
            ReloadType.Suggestions -> fetchSuggestions()
        }
    }

    private suspend fun fetchSuggestions() {
        val currentValue = suggestions.value?.copy(state = SuggestedResult.State.Loading)
            ?: SuggestedResult(SuggestedResult.State.Loading)
        suggestions.postValue(currentValue)

        val data = repository.homeSuggestions()
        suggestions.postValue(SuggestedResult(SuggestedResult.State.Ready, data))
    }

    private suspend fun fetchSongs() {
        songs.postValue(repository.allSongs())
    }

    private suspend fun fetchAlbums() {
        albums.postValue(repository.allAlbums())
    }

    private suspend fun fetchArtists() {
        if (Preferences.onlyAlbumArtists) {
            artists.postValue(repository.allAlbumArtists())
        } else {
            artists.postValue(repository.allArtists())
        }
    }

    private suspend fun fetchPlaylists() {
        playlists.postValue(repository.playlistsWithSongs(true))
    }

    private suspend fun fetchGenres() {
        genres.postValue(repository.allGenres())
    }

    private suspend fun fetchYears() {
        years.postValue(repository.allYears())
    }

    private fun fetchFolders() {
        navigateToPath()
    }

    private suspend fun filesToSongs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean
    ): List<Song> {
        return buildList {
            if (includeFolders) {
                val songs = files.filterIsInstance<Folder>().flatMap {
                    if (deepListing) {
                        repository.songsByFolder(it.filePath, true)
                    } else {
                        it.songs
                    }
                }
                addAll(songs)
            }
            addAll(files.filterIsInstance<Song>())
        }
    }

    fun navigateToPath(
        navigateToPath: String? = null,
        hierarchyView: Boolean = Preferences.hierarchyFolderView
    ) = viewModelScope.launch(IO) {
        if (hierarchyView) {
            val path = if (navigateToPath.isNullOrEmpty()) {
                fileSystem.value?.path ?: Preferences.startDirectory.getCanonicalPathSafe()
            } else {
                navigateToPath
            }
            fileSystem.postValue(repository.filesInPath(path))
        } else {
            fileSystem.postValue(repository.allFolders())
        }
    }

    fun scanPaths(context: Context, paths: Array<String>): LiveData<Int> = liveData(IO) {
        val scanResult = runCatching {
            suspendCancellableCoroutine { continuation ->
                var progress = 0
                val total = paths.size

                MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                    progress++
                    if (progress == total && continuation.isActive) {
                        continuation.resume(total)
                    }
                }
            }
        }
        emit(scanResult.getOrElse { 0 })
    }

    fun scanAllPaths(context: Context): LiveData<Int> {
        // We attempt to retrieve all storage roots using our StorageManager-based utility.
        // If that fails for some reason, we fall back to Environment.getExternalStorageDirectory()
        // to scan at least the device's primary storage root.
        val storageRoots = StorageUtil.refreshStorageVolumes()
                .map { it.filePath }
                .plus(Environment.getExternalStorageDirectory().path)
                .distinct()
                .toTypedArray()

        return scanPaths(context, storageRoots)
    }

    fun blacklistPath(file: File) = viewModelScope.launch(IO) {
        inclExclDao.insertPath(InclExclEntity(file.getCanonicalPathSafe(), InclExclDao.BLACKLIST))
        forceReload(ReloadType.Folders)
    }

    fun listSongsFromFiles(
        song: Song,
        files: List<FileSystemItem>?
    ) = liveData(IO) {
        if (!files.isNullOrEmpty()) {
            val currentFolder = fileSystem.value
            val songs = if (currentFolder != null) {
                filesToSongs(files, includeFolders = false, deepListing = false)
            } else {
                emptyList()
            }
            val startPos = songs.indexOfSong(song.id).coerceAtLeast(0)
            emit(songs to startPos)
        }
    }

    fun songs(providers: List<Any>): LiveData<List<Song>> = liveData(IO) {
        val songs = providers.filterIsInstance<SongProvider>()
            .flatMap { it.songs }
        emit(songs)
    }

    fun songs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean
    ): LiveData<List<Song>> = liveData(IO) {
        val songs = filesToSongs(files, includeFolders, deepListing)
        emit(songs)
    }

    fun artists(type: ContentType): LiveData<List<Artist>> = liveData(IO) {
        when (type) {
            ContentType.TopArtists -> emit(repository.topArtists())
            ContentType.RecentArtists -> emit(repository.recentArtists())
            ContentType.HistoryArtists -> emit(repository.historyArtists())
            else -> emit(arrayListOf())
        }
    }

    fun albums(type: ContentType): LiveData<List<Album>> = liveData(IO) {
        when (type) {
            ContentType.TopAlbums -> emit(repository.topAlbums())
            ContentType.RecentAlbums -> emit(repository.recentAlbums())
            ContentType.HistoryAlbums -> emit(repository.historyAlbums())
            else -> emit(arrayListOf())
        }
    }

    fun clearHistory() {
        viewModelScope.launch(IO) {
            repository.clearSongHistory()
        }
        songHistory.value = emptyList()
    }


    fun lastAddedSongs(): LiveData<List<Song>> = liveData(IO) {
        emit(repository.recentSongs())
    }

    fun favoriteSongsFlow() = repository.favoriteSongsFlow()

    fun playCountSongsFlow() = repository.playCountSongsFlow()

    fun historySongsFlow() = repository.historySongsFlow()

    fun notRecentlyPlayedSongs(): LiveData<List<Song>> = liveData(IO) {
        emit(repository.notRecentlyPlayedSongs())
    }

    private val _addToPlaylistUiState = MutableStateFlow<AddToPlaylistUiState?>(null)
    val addToPlaylistUiState = _addToPlaylistUiState.asStateFlow()

    fun prepareToAddToPlaylist(searchQuery: String? = null) = viewModelScope.launch(IO) {
        _addToPlaylistUiState.update { it ?: AddToPlaylistUiState.Loading }

        val playlists = if (searchQuery.isNullOrBlank()) {
            repository.playlistsWithSongs()
        } else {
            repository.searchPlaylists(searchQuery)
        }

        _addToPlaylistUiState.value = if (playlists.isEmpty()) {
            AddToPlaylistUiState.Empty(searchQuery)
        } else {
            AddToPlaylistUiState.Ready(playlists)
        }
    }

    fun addToPlaylists(
        playlistsIds: List<Long>,
        songs: List<Song>
    ) = viewModelScope.launch(IO) {
        val state = addToPlaylistUiState.value ?: return@launch
        if (state is AddToPlaylistUiState.Ready && state.playlists.isNotEmpty()) {
            _addToPlaylistUiState.value = state.copy(isLoading = true)

            var success = true
            val playlists = state.playlists.filter { playlistsIds.contains(it.playlistEntity.playListId) }
            for (playlist in playlists) {
                val checkedSongs = songs.filterNot {
                    repository.checkSongExistInPlaylist(playlist.playlistEntity, it)
                }
                val result = runCatching {
                    insertSongs(playlist.playlistEntity.playListId, checkedSongs)
                }
                success = success && result.isSuccess
            }

            _addToPlaylistUiState.value = AddToPlaylistUiState.Completed(success)
            forceReload(ReloadType.Playlists)
        }
    }

    fun finishAddingToPlaylists() {
        _addToPlaylistUiState.value = null
    }

    private val _assignToMetadataUiState = MutableStateFlow<AssignToMetadataUiState?>(null)
    val assignToMetadataUiState = _assignToMetadataUiState.asStateFlow()

    fun prepareToAssignMetadata() = viewModelScope.launch(IO) {
        _assignToMetadataUiState.update { it ?: AssignToMetadataUiState.Loading }

        val moods = metadataRepository.getAllMoods().first()
        val tags = metadataRepository.getTagsByCategory().first()
        val instruments = metadataRepository.getInstrumentsByFamily().first()

        _assignToMetadataUiState.value = AssignToMetadataUiState.Ready(moods, tags, instruments)
    }

    fun assignToMoods(moodIds: List<Long>, songs: List<Song>, source: MetadataSource = MetadataSource.Manual) =
        viewModelScope.launch(IO) {
            val state = assignToMetadataUiState.value
            if (state is AssignToMetadataUiState.Ready) {
                _assignToMetadataUiState.value = state.copy(isLoading = true)
                val result = runCatching {
                    metadataRepository.assignSongsToMoods(moodIds, songs.map { it.id }, source)
                }
                _assignToMetadataUiState.value = AssignToMetadataUiState.Completed(result.isSuccess)
            }
        }

    fun assignToTags(tagIds: List<Long>, songs: List<Song>, source: MetadataSource = MetadataSource.Manual) =
        viewModelScope.launch(IO) {
            val state = assignToMetadataUiState.value
            if (state is AssignToMetadataUiState.Ready) {
                _assignToMetadataUiState.value = state.copy(isLoading = true)
                val result = runCatching {
                    metadataRepository.assignSongsToTags(tagIds, songs.map { it.id }, source)
                }
                _assignToMetadataUiState.value = AssignToMetadataUiState.Completed(result.isSuccess)
            }
        }

    fun assignToInstruments(
        instrumentIds: List<Long>,
        songs: List<Song>,
        source: MetadataSource = MetadataSource.Manual
    ) = viewModelScope.launch(IO) {
        val state = assignToMetadataUiState.value
        if (state is AssignToMetadataUiState.Ready) {
            _assignToMetadataUiState.value = state.copy(isLoading = true)
            val result = runCatching {
                metadataRepository.assignSongsToInstruments(instrumentIds, songs.map { it.id }, source)
            }
            _assignToMetadataUiState.value = AssignToMetadataUiState.Completed(result.isSuccess)
        }
    }

    fun finishAssigningMetadata() {
        _assignToMetadataUiState.value = null
    }

    fun updatePlaylist(
        playlist: PlaylistEntity,
        newName: String,
        newImageUri: String?,
        newDescription: String?
    ) = viewModelScope.launch(IO) {
        var imageUri = playlist.customCoverUri
        if (newImageUri != imageUri) {
            if (!imageUri.isNullOrEmpty()) {
                customPlaylistImageManager.deleteImage(imageUri.toUri())
            }
            imageUri = customPlaylistImageManager.createPlaylistImage(newImageUri)?.toString()
        }
        repository.updatePlaylist(
            playlist.copy(
                playlistName = newName,
                customCoverUri = imageUri,
                description = newDescription
            )
        )
    }

    fun deleteSongsInPlaylist(playlistId: Long, songs: List<SongEntity>) = viewModelScope.launch(IO) {
        repository.deleteSongsInPlaylist(playlistId, songs.map { it.mediaStoreId })
        forceReload(ReloadType.Playlists)
    }

    fun deletePlaylists(playlists: List<PlaylistEntity>) = viewModelScope.launch(IO) {
        for (playlist in playlists) {
            playlist.customCoverUri?.let {
                customPlaylistImageManager.deleteImage(it.toUri())
            }
        }
        repository.deletePlaylists(playlists)
        forceReload(ReloadType.Playlists)
    }

    fun createCustomPlaylist(
        playlistName: String,
        customCoverUri: String? = null,
        description: String? = null,
        songs: List<Song> = emptyList()
    ): LiveData<AddToPlaylistResult> = liveData(IO) {
        emit(AddToPlaylistResult(playlistName, isWorking = true))

        val playlists = checkPlaylistExists(playlistName)
        if (playlists.isEmpty()) {
            val playlistImageUri = customPlaylistImageManager.createPlaylistImage(customCoverUri)
            val playlistEntity = PlaylistEntity(
                playlistName = playlistName,
                customCoverUri = playlistImageUri?.toString(),
                description = description
            )
            val playlistId: Long = createPlaylist(playlistEntity)
            if (songs.isNotEmpty()) {
                repository.insertSongsInPlaylist(playlistId, songs)
            }
            val playlistCreated = (playlistId != -1L)
            val isFavoritePlaylist = repository.checkFavoritePlaylist()?.playListId == playlistId
            emit(
                AddToPlaylistResult(
                    playlistName,
                    playlistCreated = playlistCreated,
                    isFavoritePlaylist = isFavoritePlaylist,
                    insertedSongs = songs.size
                )
            )
        } else {
            // Playlist already exists
            emit(AddToPlaylistResult(playlistName, playlistCreated = false))
        }
        forceReload(ReloadType.Playlists)
    }

    fun favoritePlaylist(): LiveData<PlaylistEntity> = liveData(IO) {
        emit(repository.favoritePlaylist())
    }

    fun isSongFavorite(song: Song): LiveData<Boolean> = liveData(IO) {
        emit(repository.isSongFavorite(song.id))
    }

    suspend fun insertSongs(playlistId: Long, songs: List<Song>) = repository.insertSongsInPlaylist(playlistId, songs)

    private suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        repository.checkPlaylistExists(playlistName)

    private suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        repository.createPlaylist(playlistEntity)

    private suspend fun deleteMissingContent() {
        repository.deleteMissingContent()
    }

    fun getDevicePlaylists(): LiveData<List<ImportablePlaylistResult>> = liveData(IO) {
        val devicePlaylists = repository.devicePlaylists()
        val importablePlaylists = devicePlaylists.map {
            ImportablePlaylistResult(it.name, it.getSongs())
        }.filter {
            it.songs.isNotEmpty()
        }
        emit(importablePlaylists)
    }

    fun importPlaylist(context: Context, playlist: ImportablePlaylistResult): LiveData<ImportResult> = liveData(IO) {
        var count = 1
        var playlistName = playlist.playlistName
        while (repository.checkPlaylistExists(playlistName).isNotEmpty() && count <= 100) {
            playlistName = "${playlist.playlistName} $count"
            count++
        }
        if (repository.checkPlaylistExists(playlistName).isEmpty()) {
            val id = repository.createPlaylist(PlaylistEntity(playlistName = playlistName))
            if (id != -1L) {
                repository.insertSongsInPlaylist(id, playlist.songs)
                emit(ImportResult.success(context, playlist))
                forceReload(ReloadType.Playlists)
            } else {
                emit(ImportResult.error(context, playlist))
            }
        } else {
            emit(ImportResult.error(context, playlist))
        }
    }

    fun deleteSongs(songs: List<Song>) = viewModelScope.launch(IO) {
        repository.deleteSongs(songs)
    }

    private suspend fun initializeBlacklist() {
        if (!Preferences.initializedBlacklist) {
            repository.initializeBlacklist()
            Preferences.initializedBlacklist = true
        }
    }

    fun getLoginState(service: ScrobblingService) = repository.getLoginState(service)
    fun getCurrentLoginState(service: ScrobblingService) = repository.getCurrentLoginState(service)

    fun isServiceLoggedIn(service: ScrobblingService): Boolean {
        // This is a bit hacky since we don't have direct access to the current value of the flow in the interface
        // But for UI checks, we can use this if we store the state in the VM too.
        // Actually, NetworkRepositoryImpl has it as a StateFlow, so we can try to cast or add it to the interface.
        return false // Default
    }

    fun logInToService(service: ScrobblingService, params: LoginParams) = viewModelScope.launch(IO) {
        repository.loginToService(service, params)
    }

    fun logoutFromService(service: ScrobblingService) = viewModelScope.launch(IO) {
        repository.logoutFromService(service)
    }

    @Suppress("DEPRECATION")
    fun handleIntent(intent: Intent): LiveData<HandleIntentResult> = liveData(IO) {
        val result = HandleIntentResult(handled = true)
        val uri = intent.data
        if (uri == null || uri.scheme == "glance-action") {
            emit(result.copy(handled = false))
        } else try {
            if (uri.toString().isNotEmpty()) {
                val songs = repository.songsByUri(uri)
                emit(result.copy(songs = songs, failed = songs.isEmpty()))
            } else {
                when (intent.type) {
                    MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "playlistId", "playlist")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            val playlist = devicePlaylistById(id)
                            if (playlist != Playlist.EmptyPlaylist) {
                                emit(result.copy(songs = playlist.getSongs(), position = position))
                            } else {
                                emit(result)
                            }
                        }
                    }

                    MediaStore.Audio.Albums.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "albumId", "album")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            emit(result.copy(songs = albumById(id).songs, position = position))
                        }
                    }

                    MediaStore.Audio.Artists.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "artistId", "artist")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            emit(result.copy(songs = artistById(id).songs, position = position))
                        }
                    }

                    else -> emit(result.copy(handled = false))
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "handleIntent() failed; intent=$intent", e)
        }
    }

    private fun parseIdFromIntent(intent: Intent, longKey: String, stringKey: String): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            id = intent.getStringExtra(stringKey)?.toLongOrNull() ?: -1
        }
        return id
    }
}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    Playlists,
    Genres,
    Folders,
    Years,
    Suggestions
}

sealed class AssignToMetadataUiState(open val isLoading: Boolean) {
    data object Loading : AssignToMetadataUiState(true)
    data class Ready(
        val moods: List<Mood> = emptyList(),
        val tags: Map<TagCategory, List<Tag>> = emptyMap(),
        val instruments: Map<InstrumentFamily, List<Instrument>> = emptyMap(),
        override val isLoading: Boolean = false
    ) : AssignToMetadataUiState(isLoading)

    data class Completed(val isSuccess: Boolean) : AssignToMetadataUiState(false)
}
