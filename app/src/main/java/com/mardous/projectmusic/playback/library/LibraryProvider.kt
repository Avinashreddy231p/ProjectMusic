package com.mardous.projectmusic.playback.library

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mardous.projectmusic.R
import com.mardous.projectmusic.coil.CoverProvider.Companion.ALBUM_ARTIST_COVER_PATH
import com.mardous.projectmusic.coil.CoverProvider.Companion.ALBUM_COVER_PATH
import com.mardous.projectmusic.coil.CoverProvider.Companion.ARTIST_COVER_PATH
import com.mardous.projectmusic.coil.CoverProvider.Companion.GENRE_COVER_PATH
import com.mardous.projectmusic.coil.CoverProvider.Companion.PLAYLIST_COVER_PATH
import com.mardous.projectmusic.coil.CoverProvider.Companion.getImageUri
import com.mardous.projectmusic.core.model.CategoryInfo
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.mapper.toSongs
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.media.albumInfo
import com.mardous.projectmusic.extensions.media.artistInfo
import com.mardous.projectmusic.extensions.media.asNumberOfAlbums
import com.mardous.projectmusic.extensions.media.asNumberOfArtists
import com.mardous.projectmusic.extensions.media.asNumberOfGenres
import com.mardous.projectmusic.extensions.media.asNumberOfPlaylists
import com.mardous.projectmusic.extensions.media.asNumberOfSongs
import com.mardous.projectmusic.playback.toMediaItems
import com.mardous.projectmusic.util.Preferences

class LibraryProvider(
    private val repository: Repository,
    private val lyricsRepository: com.mardous.projectmusic.data.local.repository.LyricsRepository
) {

    private val _searchResult = mutableListOf<MediaItem>()
    val searchResult: List<MediaItem> get() = _searchResult

    suspend fun getMediaItemsForPlayback(
        mediaItems: List<MediaItem>,
        tryToResolveComplexPaths: Boolean = false
    ): List<MediaItem> {
        val resolvedMediaItems = mutableListOf<MediaItem>()
        val localItems = mutableListOf<MediaItem>()
        val onlineItems = mutableListOf<MediaItem>()

        for (item in mediaItems) {
            val uri = item.localConfiguration?.uri
            if (uri != null && (uri.scheme == "http" || uri.scheme == "https")) {
                onlineItems.add(item)
            } else if (item.localConfiguration != null) {
                localItems.add(item)
            }
        }

        resolvedMediaItems.addAll(onlineItems)
        resolvedMediaItems.addAll(localItems)

        if (resolvedMediaItems.size == mediaItems.size) {
            return resolvedMediaItems
        }

        val unresolved = mediaItems.filter { item ->
            val uri = item.localConfiguration?.uri
            uri == null || (uri.scheme != "http" && uri.scheme != "https")
        }.filter { item -> item.localConfiguration == null }

        if (unresolved.isNotEmpty()) {
            val (songs, missingMediaItems) = repository.songsByMediaItems(unresolved)
            if (songs.isNotEmpty()) {
                resolvedMediaItems.addAll(songs.toMediaItems())
            }
            if (missingMediaItems.isNotEmpty()) {
                val complexMediaItems = if (tryToResolveComplexPaths) {
                    missingMediaItems.filter { item -> item.mediaId.contains(":") }
                } else {
                    emptyList()
                }
                if (complexMediaItems.isNotEmpty()) {
                    getMediaItemsForAAOSPlayback(complexMediaItems)?.first?.forEach {
                        resolvedMediaItems.add(it)
                    }
                } else {
                    missingMediaItems.forEach {
                        getPlayableSongs(it.mediaId).let { playableSongs ->
                            if (playableSongs.isNotEmpty()) {
                                resolvedMediaItems.addAll(playableSongs.toMediaItems())
                            }
                        }
                    }
                }
            }
        }
        return resolvedMediaItems
    }

    suspend fun getMediaItemsForAAOSPlayback(
        mediaItems: List<MediaItem>
    ): Pair<List<MediaItem>, Int>? {
        val single = mediaItems.singleOrNull()
        return if (single != null) {
            val path = MediaIDs.splitPath(single.mediaId)
            when (path.firstOrNull()) {
                SEARCH -> {
                    val id = path.getOrNull(1)
                    if (id == null || searchResult.isEmpty()) return null
                    val transformedMediaItems = searchResult.map { it.buildUpon().setMediaId(id).build() }
                    Pair(
                        transformedMediaItems,
                        transformedMediaItems.indexOfFirst { it.mediaId == id }.coerceAtLeast(0)
                    )
                }

                MediaIDs.SONGS -> {
                    val id = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val allSongs = repository.allSongs()
                    Pair(
                        allSongs.map { it.toAutoMediaItem() },
                        allSongs.indexOfFirst { it.id == id }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ALBUMS -> {
                    val albumId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val album = repository.albumById(albumId)
                    Pair(
                        album.songs.map { it.toAutoMediaItem() },
                        album.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val artistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val artistSongs = repository.artistById(artistId).sortedSongs
                    Pair(
                        artistSongs.map { it.toAutoMediaItem() },
                        artistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ALBUM_ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val albumArtistName = path.getOrNull(1) ?: return null
                    val albumArtistSongs = repository.albumArtistByName(albumArtistName).sortedSongs
                    Pair(
                        albumArtistSongs.map { it.toAutoMediaItem() },
                        albumArtistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.PLAYLISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val playlistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playlist = repository.playlistWithSongs(playlistId)
                    Pair(
                        playlist.songs.toSongs().map { it.toAutoMediaItem() },
                        playlist.songs.indexOfFirst { it.mediaStoreId == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.GENRES -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val genreId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songsByGenre = repository.songsByGenre(genreId)
                    Pair(
                        songsByGenre.map { it.toAutoMediaItem() },
                        songsByGenre.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.TOP_TRACKS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playCountSongs = repository.playCountSongs()
                    Pair(
                        playCountSongs.map { it.toAutoMediaItem() },
                        playCountSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.RECENT_SONGS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val historySongs = repository.historySongs()
                    Pair(
                        historySongs.map { it.toAutoMediaItem() },
                        historySongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                else -> null
            }
        } else null
    }

    suspend fun getChildren(
        context: Context,
        parentId: String,
        currentMediaItem: MediaItem? = null
    ): List<MediaItem> {
        return if (MediaIDs.isPath(parentId)) {
            val parts = MediaIDs.splitPath(parentId)
            if (parts.size < 2) {
                listOf(MediaItem.EMPTY)
            } else {
                getPlayableMediaItems(parts[0], parts[1])
            }
        } else when (parentId) {
            MediaIDs.ROOT -> {
                getRootChildren(context, currentMediaItem != null)
            }

            MediaIDs.DISCOVERY -> {
                getDiscoveryChildren(context)
            }

            MediaIDs.LIBRARY -> {
                getLibraryChildren(context)
            }

            MediaIDs.LYRICS -> {
                getLyricsChildren(currentMediaItem)
            }

            MediaIDs.ALBUMS -> {
                repository.allAlbums().map { album ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, album.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                .setArtworkUri(getImageUri(ALBUM_COVER_PATH, album.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(album.name)
                                .setSubtitle(album.albumInfo())
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.ALBUM_ARTISTS -> {
                repository.allAlbumArtists().map { albumArtist ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, albumArtist.name))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                .setArtworkUri(getImageUri(ALBUM_ARTIST_COVER_PATH, albumArtist.name))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(albumArtist.name)
                                .setSubtitle(albumArtist.artistInfo(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.ARTISTS -> {
                repository.allArtists().map { artist ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, artist.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                .setArtworkUri(getImageUri(ARTIST_COVER_PATH, artist.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(artist.name)
                                .setSubtitle(artist.artistInfo(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.PLAYLISTS -> {
                repository.playlistsWithSongs(sorted = true).map { playlistWithSongs ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, playlistWithSongs.playlistEntity.playListId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .setArtworkUri(getImageUri(PLAYLIST_COVER_PATH, playlistWithSongs.playlistEntity.playListId))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(playlistWithSongs.playlistEntity.playlistName)
                                .setSubtitle(playlistWithSongs.songCount.asNumberOfSongs(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.GENRES -> {
                repository.allGenres().map { genre ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, genre.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_GENRE)
                                .setArtworkUri(getImageUri(GENRE_COVER_PATH, genre.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(genre.name)
                                .setSubtitle(genre.songCount.asNumberOfSongs(context))
                                .build()
                        )
                        .build()
                }
            }

            // SONGS, TOP_TRACKS, RECENT_SONGS
            else -> getPlayableMediaItems(parentId)
        }
    }

    fun getItem(itemId: String): MediaItem {
        val songId = itemId.toLongOrNull() ?: return MediaItem.EMPTY
        return repository.songById(songId).toAutoMediaItem()
    }

    suspend fun search(query: String): List<MediaItem> {
        _searchResult.clear()
        _searchResult.addAll(repository.searchSongs(query).map { it.toAutoMediaItem(SEARCH) })
        return _searchResult
    }

    private suspend fun getRootChildren(context: Context, hasCurrentSong: Boolean): List<MediaItem> {
        val resources = context.resources
        val children = mutableListOf<MediaItem>()
        
        if (hasCurrentSong) {
            children.add(
                MediaItem.Builder()
                    .setMediaId(MediaIDs.LYRICS)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle(resources.getString(R.string.lyrics_label))
                            .setSubtitle(resources.getString(R.string.action_show_lyrics))
                            .build()
                    )
                    .build()
            )
        }

        children.add(
            MediaItem.Builder()
                .setMediaId(MediaIDs.DISCOVERY)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.discovery_label))
                        .setSubtitle(resources.getString(R.string.online_recommendations))
                        .setExtras(Bundle().apply {
                            // Hint for Android Auto to display this folder as a grid
                            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
                        })
                        .build()
                )
                .build()
        )

        children.add(
            MediaItem.Builder()
                .setMediaId(MediaIDs.LIBRARY)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.library_title))
                        .setSubtitle(repository.allSongs().size.asNumberOfSongs(context))
                        .setExtras(Bundle().apply {
                            // Hint for Android Auto to display this folder as a list
                            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                        })
                        .build()
                )
                .build()
        )
        
        return children
    }

    private suspend fun getLyricsChildren(currentMediaItem: MediaItem?): List<MediaItem> {
        if (currentMediaItem == null) return emptyList()

        val song = repository.songByMediaItem(currentMediaItem)
        if (song == Song.emptySong) return emptyList()

        val rawLyrics = lyricsRepository.fileLyrics(song) 
            ?: lyricsRepository.embeddedLyrics(song)
            ?: lyricsRepository.storedLyrics(song, false)

        val syncedLyrics = rawLyrics?.let { lyricsRepository.parseRawLyrics(song, it) }
        val lines = syncedLyrics?.lines?.map { it.content.content }
            ?: rawLyrics?.lyrics?.split("\n")
            ?: emptyList()

        return lines.filter { it.isNotBlank() }.mapIndexed { index, line ->
            MediaItem.Builder()
                .setMediaId("LYRIC_LINE_$index")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        // Using TITLE for the lyrics text ensures it uses the largest font 
                        // available in the car UI list template.
                        .setTitle(line.trim())
                        .build()
                )
                .build()
        }
    }

    private suspend fun getDiscoveryChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        return listOf(
            MediaItem.Builder()
                .setMediaId(MediaIDs.RECENT_SONGS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.history_label))
                        .setSubtitle(repository.historySongs().size.asNumberOfSongs(context))
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(MediaIDs.TOP_TRACKS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.top_tracks_label))
                        .setSubtitle(repository.playCountSongs().size.asNumberOfSongs(context))
                        .build()
                )
                .build()
        )
    }

    private suspend fun getLibraryChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        val mediaItems = mutableListOf<MediaItem>()

        // 1. Quick Start
        mediaItems.add(
            MediaItem.Builder()
                .setMediaId(MediaIDs.SONGS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(true)
                        .setTitle(resources.getString(R.string.shuffle_all_label))
                        .setSubtitle(repository.allSongs().size.asNumberOfSongs(context))
                        .build()
                )
                .build()
        )

        mediaItems.add(
            MediaItem.Builder()
                .setMediaId(MediaIDs.FAVORITES)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(true)
                        .setTitle(resources.getString(R.string.favorites_label))
                        .setSubtitle(repository.favoriteSongs().size.asNumberOfSongs(context))
                        .setArtworkUri(getImageUri(PLAYLIST_COVER_PATH, -1))
                        .build()
                )
                .build()
        )

        // 2. Categories
        val libraryCategories = Preferences.libraryCategories
        libraryCategories.forEach { categoryInfo ->
            if (categoryInfo.visible) {
                when (categoryInfo.category) {
                    CategoryInfo.Category.Songs -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.SONGS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .setSubtitle(repository.allSongs().size.asNumberOfSongs(context))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Albums -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.ALBUMS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .setSubtitle(repository.allAlbums().size.asNumberOfAlbums(context))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Artists -> {
                        if (Preferences.onlyAlbumArtists) {
                            mediaItems.add(
                                MediaItem.Builder()
                                    .setMediaId(MediaIDs.ALBUM_ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.album_artists_label))
                                            .setSubtitle(repository.allAlbumArtists().size.asNumberOfArtists(context))
                                            .build()
                                    )
                                    .build()
                            )
                        } else {
                            mediaItems.add(
                                MediaItem.Builder()
                                    .setMediaId(MediaIDs.ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.artists_label))
                                            .setSubtitle(repository.allArtists().size.asNumberOfArtists(context))
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }

                    CategoryInfo.Category.Genres -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.GENRES)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_GENRES)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .setSubtitle(repository.allGenres().size.asNumberOfGenres(context))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Playlists -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.PLAYLISTS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .setSubtitle(repository.playlistsWithSongs().size.asNumberOfPlaylists(context))
                                        .build()
                                )
                                .build()
                        )
                    }

                    else -> { /*no-op*/ }
                }
            }
        }
        return mediaItems
    }

    private suspend fun getPlayableSongs(
        parentId: String,
        childId: String? = null
    ): List<Song> {
        return if (childId == null) {
            when (parentId) {
                MediaIDs.SONGS -> repository.allSongs()
                MediaIDs.TOP_TRACKS -> repository.playCountSongs()
                MediaIDs.LAST_ADDED -> repository.recentSongs()
                MediaIDs.RECENT_SONGS -> repository.historySongs()
                MediaIDs.FAVORITES -> repository.favoriteSongs()
                else -> emptyList()
            }
        } else {
            val childIdLong = childId.toLongOrNull()
            if (childIdLong == null) {
                if (parentId == MediaIDs.ALBUM_ARTISTS) {
                    repository.albumArtistByName(childId).sortedSongs
                } else {
                    emptyList()
                }
            } else when (parentId) {
                MediaIDs.ALBUMS -> repository.albumById(childIdLong).songs
                MediaIDs.ARTISTS -> repository.artistById(childIdLong).sortedSongs
                MediaIDs.PLAYLISTS -> repository.playlistWithSongs(childIdLong).songs.toSongs()
                MediaIDs.GENRES -> repository.songsByGenre(childIdLong)
                else -> emptyList()
            }
        }
    }

    private suspend fun getPlayableMediaItems(parentId: String, childId: String? = null) =
        getPlayableSongs(parentId, childId)
            .filterNot { it == Song.emptySong }
            .map { song ->
                song.toAutoMediaItem(
                    if (childId.isNullOrEmpty()) parentId else MediaIDs.getPathId(parentId, childId)
                )
            }

    private fun Song.toAutoMediaItem(parent: String? = null): MediaItem =
        toMediaItem(if (parent.isNullOrEmpty()) id.toString() else MediaIDs.getPathId(parent, id))

    companion object {
        // Internal ID for search requests
        private const val SEARCH = "SEARCH"
    }
}
