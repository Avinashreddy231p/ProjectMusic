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

package com.mardous.projectmusic

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.mardous.projectmusic.coil.CustomArtistImageManager
import com.mardous.projectmusic.coil.CustomPlaylistImageManager
import com.mardous.projectmusic.core.ProjectMusicDatabase
import com.mardous.projectmusic.core.audio.AudioOutputObserver
import com.mardous.projectmusic.data.local.EditTarget
import com.mardous.projectmusic.data.local.MediaStoreWriter
import com.mardous.projectmusic.data.local.repository.AlbumRepository
import com.mardous.projectmusic.data.local.repository.ArtistRepository
import com.mardous.projectmusic.data.local.repository.GenreRepository
import com.mardous.projectmusic.data.local.repository.LyricsRepository
import com.mardous.projectmusic.data.local.repository.NetworkRepository
import com.mardous.projectmusic.data.local.repository.NetworkRepositoryImpl
import com.mardous.projectmusic.data.local.repository.PlaylistRepository
import com.mardous.projectmusic.data.local.repository.RealAlbumRepository
import com.mardous.projectmusic.data.local.repository.RealArtistRepository
import com.mardous.projectmusic.data.local.repository.RealGenreRepository
import com.mardous.projectmusic.data.local.repository.RealLyricsRepository
import com.mardous.projectmusic.data.local.repository.RealPlaylistRepository
import com.mardous.projectmusic.data.local.repository.RealRepository
import com.mardous.projectmusic.data.local.repository.RealSearchRepository
import com.mardous.projectmusic.data.local.repository.RealSmartRepository
import com.mardous.projectmusic.data.local.repository.RealSongRepository
import com.mardous.projectmusic.data.local.repository.RealSpecialRepository
import com.mardous.projectmusic.data.local.repository.Repository
import com.mardous.projectmusic.data.local.repository.SearchRepository
import com.mardous.projectmusic.data.local.repository.SmartRepository
import com.mardous.projectmusic.data.local.repository.SongRepository
import com.mardous.projectmusic.data.local.repository.SpecialRepository
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.local.repository.RealStatsRepository
import com.mardous.projectmusic.data.model.Genre
import com.mardous.projectmusic.data.remote.deezer.DeezerService
import com.mardous.projectmusic.data.remote.github.GitHubService
import com.mardous.projectmusic.data.remote.jsonHttpClient
import com.mardous.projectmusic.data.remote.lastfm.LastFmService
import com.mardous.projectmusic.data.remote.listenbrainz.ListenBrainzService
import com.mardous.projectmusic.data.remote.lyrics.LyricsDownloadService
import com.mardous.projectmusic.data.remote.lyrics.UnisonSubmissionService
import com.mardous.projectmusic.data.remote.musicbrainz.MusicBrainzService
import com.mardous.projectmusic.data.remote.provideOkHttp
import com.mardous.projectmusic.playback.SleepTimer
import com.mardous.projectmusic.playback.equalizer.EqualizerManager
import com.mardous.projectmusic.playback.processor.BalanceAudioProcessor
import com.mardous.projectmusic.playback.processor.BeatAudioProcessor
import com.mardous.projectmusic.playback.processor.ReplayGainAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import com.mardous.projectmusic.ui.screen.equalizer.EqualizerViewModel
import com.mardous.projectmusic.ui.screen.info.InfoViewModel
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.screen.library.albums.AlbumDetailViewModel
import com.mardous.projectmusic.ui.screen.library.artists.ArtistDetailViewModel
import com.mardous.projectmusic.ui.screen.library.folders.FolderDetailViewModel
import com.mardous.projectmusic.ui.screen.library.genres.GenreDetailViewModel
import com.mardous.projectmusic.ui.screen.library.instruments.InstrumentsViewModel
import com.mardous.projectmusic.ui.screen.librarystats.LibraryStatsViewModel
import com.mardous.projectmusic.ui.screen.scrobbling.LastFmProfileViewModel
import com.mardous.projectmusic.ui.screen.scrobbling.PendingScrobblesViewModel
import com.mardous.projectmusic.ui.screen.library.moods.MoodDetailViewModel
import com.mardous.projectmusic.ui.screen.library.moods.MoodsViewModel
import com.mardous.projectmusic.ui.screen.library.tags.TagDetailViewModel
import com.mardous.projectmusic.ui.screen.library.tags.TagsViewModel
import com.mardous.projectmusic.ui.screen.library.playlists.PlaylistDetailViewModel
import com.mardous.projectmusic.ui.screen.library.search.SearchViewModel
import com.mardous.projectmusic.ui.screen.library.years.YearDetailViewModel
import com.mardous.projectmusic.ui.screen.lyrics.LyricsViewModel
import com.mardous.projectmusic.ui.screen.player.PlayerViewModel
import com.mardous.projectmusic.ui.screen.sleeptimer.SleepTimerViewModel
import com.mardous.projectmusic.ui.screen.settings.SettingsViewModel
import com.mardous.projectmusic.ui.screen.stats.StatsViewModel
import com.mardous.projectmusic.ui.screen.tageditor.TagEditorViewModel
import com.mardous.projectmusic.ui.screen.update.UpdateViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    factory {
        jsonHttpClient(okHttpClient = get())
    }
    factory {
        provideOkHttp(context = get())
    }
    single {
        GitHubService(context = androidContext(), client = get())
    }
    single {
        DeezerService(client = get())
    }
    single {
        LastFmService(client = get())
    }
    single {
        ListenBrainzService(client = get())
    }
    single {
        LyricsDownloadService(client = get())
    }
    single {
        UnisonSubmissionService(client = get())
    }
    single {
        MusicBrainzService(client = get())
    }
}

private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        PreferenceManager.getDefaultSharedPreferences(androidContext())
    }
    single {
        SleepTimer(context = androidContext())
    }
    single {
        BalanceAudioProcessor()
    }
    single {
        ReplayGainAudioProcessor()
    }
    single {
        com.mardous.projectmusic.playback.processor.BeatAudioProcessor()
    }
    single {
        EqualizerManager(
            context = androidContext(),
            balanceProcessor = get(),
            replayGainProcessor = get(),
            audioOutputObserver = get()
        )
    }
    single {
        MediaStoreWriter(context = androidContext(), contentResolver = get())
    }
    single {
        CustomArtistImageManager(context = androidContext())
    }
    single {
        CustomPlaylistImageManager(context = androidContext())
    }
    single {
        AudioOutputObserver(context = androidContext())
    }
    factory {
        com.mardous.projectmusic.playback.shuffle.ShuffleManager()
    }
    single(named("applicationScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

private val roomModule = module {
    single {
        Room.databaseBuilder(androidContext(), ProjectMusicDatabase::class.java, "music_database.db")
            .addMigrations(
                ProjectMusicDatabase.MIGRATION_1_2,
                ProjectMusicDatabase.MIGRATION_2_3,
                ProjectMusicDatabase.MIGRATION_3_4,
                ProjectMusicDatabase.MIGRATION_4_5,
                ProjectMusicDatabase.MIGRATION_5_6,
                ProjectMusicDatabase.MIGRATION_6_7,
                ProjectMusicDatabase.MIGRATION_7_8,
                ProjectMusicDatabase.MIGRATION_8_9,
                ProjectMusicDatabase.MIGRATION_9_10,
                ProjectMusicDatabase.MIGRATION_10_11,
                ProjectMusicDatabase.MIGRATION_11_12,
                ProjectMusicDatabase.MIGRATION_12_13,
                ProjectMusicDatabase.MIGRATION_13_14,
                ProjectMusicDatabase.MIGRATION_14_15,
                ProjectMusicDatabase.MIGRATION_15_16
            )
            .build()
    }

    single {
        get<ProjectMusicDatabase>().playlistDao()
    }

    single {
        get<ProjectMusicDatabase>().playCountDao()
    }

    single {
        get<ProjectMusicDatabase>().historyDao()
    }

    single {
        get<ProjectMusicDatabase>().queueDao()
    }

    single {
        get<ProjectMusicDatabase>().inclExclDao()
    }

    single {
        get<ProjectMusicDatabase>().lyricsDao()
    }

    single {
        get<ProjectMusicDatabase>().listeningHistoryDao()
    }

    single {
        get<ProjectMusicDatabase>().listeningSessionGroupDao()
    }

    single {
        get<ProjectMusicDatabase>().pendingScrobbleDao()
    }

    single {
        get<ProjectMusicDatabase>().metadataDao()
    }

    single {
        get<ProjectMusicDatabase>().rankingDao()
    }

    single {
        get<ProjectMusicDatabase>().workDao()
    }

    single {
        get<ProjectMusicDatabase>().analyticsDao()
    }

    single {
        get<ProjectMusicDatabase>().healthDao()
    }

    single {
        RealStatsRepository(dao = get(), groupDao = get(), context = androidContext())
    } bind StatsRepository::class

    single {
        com.mardous.projectmusic.data.local.repository.StatsCache(listeningHistoryDao = get(), applicationScope = get(named("applicationScope")))
    }
}

private val dataModule = module {
    single {
        RealRepository(
            context = androidContext(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            genreRepository = get(),
            smartRepository = get(),
            specialRepository = get(),
            playlistRepository = get(),
            searchRepository = get(),
            networkRepository = get()
        )
    } bind Repository::class

    single {
        RealSongRepository(context = get(), inclExclDao = get())
    } bind SongRepository::class

    single {
        RealAlbumRepository(songRepository = get(), metadataDao = get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(songRepository = get(), albumRepository = get(), metadataDao = get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(
            context = androidContext(),
            songRepository = get(),
            playlistDao = get(),
            rankingEngine = get()
        )
    } bind PlaylistRepository::class

    single {
        RealGenreRepository(contentResolver = get(), songRepository = get())
    } bind GenreRepository::class

    single {
        RealSearchRepository(
            albumRepository = get(),
            songRepository = get(),
            artistRepository = get(),
            playlistRepository = get(),
            genreRepository = get(),
            specialRepository = get()
        )
    } bind SearchRepository::class

    single {
        RealSmartRepository(
            context = androidContext(),
            songRepository = get(),
            albumRepository = get(),
            artistRepository = get(),
            historyDao = get(),
            playCountDao = get(),
            listeningHistoryDao = get(),
            rankingDao = get()
        )
    } bind SmartRepository::class

    single {
        RealSpecialRepository(songRepository = get())
    } bind SpecialRepository::class

    single {
        RealLyricsRepository(
            context = androidContext(),
            preferences = get(),
            lyricsDownloadService = get(),
            lyricsDao = get(),
            songRepository = get()
        )
    } bind LyricsRepository::class

    single {
        NetworkRepositoryImpl(
            context = androidContext(),
            preferences = get(),
            lastFmService = get(),
            listenBrainzService = get(),
            deezerService = get(),
            pendingScrobbleDao = get()
        )
    } bind NetworkRepository::class

    single {
        com.mardous.projectmusic.data.local.repository.RealMetadataRepository(
            metadataDao = get(),
            rankingDao = get(),
            songRepository = get()
        )
    } bind com.mardous.projectmusic.data.local.repository.MetadataRepository::class

    single {
        com.mardous.projectmusic.data.local.database.sync.RankingEngine(
            rankingDao = get(),
            metadataDao = get(),
            healthDao = get(),
            lyricsDao = get(),
            songRepository = get()
        )
    }

    single {
        com.mardous.projectmusic.data.local.repository.MusicBrainzRepository(
            musicBrainzService = get(),
            metadataDao = get(),
            rankingDao = get()
        )
    }

    single {
        com.mardous.projectmusic.data.local.repository.FileTagScanner(
            rankingDao = get(),
            metadataDao = get(),
            lyricsDao = get(),
            contentResolver = get<android.content.Context>().contentResolver
        )
    }
}

private val viewModule = module {
    viewModelOf(::LibraryViewModel)
    viewModelOf(::PlayerViewModel)
    viewModelOf(::EqualizerViewModel)
    viewModelOf(::SleepTimerViewModel)

    viewModel { (albumId: Long) ->
        AlbumDetailViewModel(
            application = androidApplication(),
            repository = get(),
            albumId = albumId
        )
    }

    viewModel { (artistId: Long, artistName: String?) ->
        ArtistDetailViewModel(
            application = androidApplication(),
            repository = get(),
            artistId = artistId,
            artistName = artistName
        )
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailViewModel(playlistRepository = get(), playlistId = playlistId)
    }

    viewModel { (genre: Genre) ->
        GenreDetailViewModel(repository = get(), genre = genre)
    }

    viewModel { (year: Int) ->
        YearDetailViewModel(repository = get(), year = year)
    }

    viewModel { (path: String) ->
        FolderDetailViewModel(repository = get(), folderPath = path)
    }

    viewModel { (moodId: Long) ->
        MoodDetailViewModel(metadataRepository = get(), moodId = moodId)
    }

    viewModel { (tagId: Long) ->
        TagDetailViewModel(metadataRepository = get(), tagId = tagId)
    }

    viewModelOf(::SearchViewModel)
    viewModelOf(::LyricsViewModel)
    viewModelOf(::InfoViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::MoodsViewModel)
    viewModelOf(::TagsViewModel)
    viewModelOf(::PendingScrobblesViewModel)
    viewModelOf(::LastFmProfileViewModel)
    viewModelOf(::LibraryStatsViewModel)
    viewModelOf(::InstrumentsViewModel)

    viewModel { (target: EditTarget) ->
        TagEditorViewModel(
            repository = get(),
            customArtistImageManager = get(),
            target = target
        )
    }

    viewModel { (instrumentId: Long) ->
        com.mardous.projectmusic.ui.screen.library.instruments.InstrumentDetailViewModel(
            metadataRepository = get(),
            instrumentId = instrumentId
        )
    }
}

val appModules = listOf(networkModule, mainModule, roomModule, dataModule, viewModule)
