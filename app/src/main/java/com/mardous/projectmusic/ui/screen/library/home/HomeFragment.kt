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

package com.mardous.projectmusic.ui.screen.library.home

import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.UITheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mardous.projectmusic.R
import com.mardous.projectmusic.data.model.Album
import com.mardous.projectmusic.data.model.Artist
import com.mardous.projectmusic.data.model.ContentType
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.model.Suggestion
import com.mardous.projectmusic.databinding.FragmentHomeBinding
import com.mardous.projectmusic.extensions.dp
import com.mardous.projectmusic.extensions.isNullOrEmpty
import com.mardous.projectmusic.extensions.navigation.albumDetailArgs
import com.mardous.projectmusic.extensions.navigation.artistDetailArgs
import com.mardous.projectmusic.extensions.navigation.asFragmentExtras
import com.mardous.projectmusic.extensions.navigation.detailArgs
import com.mardous.projectmusic.extensions.navigation.playlistDetailArgs
import com.mardous.projectmusic.extensions.resources.addPaddingRelative
import com.mardous.projectmusic.extensions.resources.destroyOnDetach
import com.mardous.projectmusic.extensions.resources.primaryColor
import com.mardous.projectmusic.extensions.resources.setupStatusBarForeground
import com.mardous.projectmusic.extensions.setSupportActionBar
import com.mardous.projectmusic.extensions.toHtml
import com.mardous.projectmusic.extensions.topLevelTransition
import com.mardous.projectmusic.ui.IAlbumCallback
import com.mardous.projectmusic.ui.IArtistCallback
import com.mardous.projectmusic.ui.IHomeCallback
import com.mardous.projectmusic.ui.IScrollHelper
import com.mardous.projectmusic.ui.ISongCallback
import com.mardous.projectmusic.ui.adapters.HomeAdapter
import com.mardous.projectmusic.ui.adapters.album.AlbumAdapter
import com.mardous.projectmusic.ui.adapters.artist.ArtistAdapter
import com.mardous.projectmusic.ui.adapters.song.SongAdapter
import com.mardous.projectmusic.ui.component.base.AbsMainActivityFragment
import com.mardous.projectmusic.ui.component.menu.onAlbumMenu
import com.mardous.projectmusic.ui.component.menu.onAlbumsMenu
import com.mardous.projectmusic.ui.component.menu.onArtistMenu
import com.mardous.projectmusic.ui.component.menu.onArtistsMenu
import com.mardous.projectmusic.ui.component.menu.onSongMenu
import com.mardous.projectmusic.ui.component.menu.onSongsMenu
import com.mardous.projectmusic.ui.screen.stats.StatsViewModel
import com.mardous.projectmusic.ui.screen.library.ReloadType
import com.mardous.projectmusic.data.local.repository.StatsRepository
import com.mardous.projectmusic.data.local.repository.StatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * @author Christians M. A. (mardous)
 */
class HomeFragment : AbsMainActivityFragment(R.layout.fragment_home),
    View.OnClickListener,
    ISongCallback,
    IAlbumCallback,
    IArtistCallback,
    IHomeCallback,
    IScrollHelper {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private var homeAdapter: HomeAdapter? = null
    private val statsRepository: StatsRepository by inject()
    private var statsJob: kotlinx.coroutines.Job? = null

    private val currentContent: SuggestedResult
        get() = libraryViewModel.getSuggestions().value ?: SuggestedResult.Idle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeBinding = FragmentHomeBinding.bind(view)
        _binding = HomeBinding(homeBinding)
        binding.appBarLayout.setupStatusBarForeground()
        setSupportActionBar(binding.toolbar)
        topLevelTransition(view)

        setupTitle()
        setupListeners()
        setupStatsCard()
        checkForMargins()

        homeAdapter = HomeAdapter(arrayListOf(), this).also {
            it.registerAdapterDataObserver(adapterDataObserver)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = homeAdapter
            addPaddingRelative(bottom = 8.dp(resources))
            destroyOnDetach()
        }
        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(
                bottom = it.getWithSpace(16.dp(resources), includeInsets = false)
            )
        }
        libraryViewModel.getSuggestions().apply {
            observe(viewLifecycleOwner) { result ->
                if (result.isLoading && homeAdapter.isNullOrEmpty) {
                    binding.progressIndicator.show()
                } else {
                    binding.progressIndicator.hide()
                }
                homeAdapter?.dataSet = result.data
            }
        }.also { liveData ->
            if (liveData.value == SuggestedResult.Idle) {
                libraryViewModel.forceReload(ReloadType.Suggestions)
            }
        }

        applyWindowInsetsFromView(view)
    }

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            checkIsEmpty()
        }
    }

    private fun setupTitle() {
        binding.appBarLayout.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.nav_search)
        }
        
        val hexColor = String.format("#%06X", 0xFFFFFF and primaryColor())
        val appName = "Project <font color=$hexColor>Music</font>".toHtml()
        binding.appBarLayout.title = appName
    }

    private fun setupListeners() {
        binding.myTopTracks.setOnClickListener(this)
        binding.lastAdded.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.moods.setOnClickListener(this)
        binding.tags.setOnClickListener(this)
        binding.instruments.setOnClickListener(this)
        binding.statsCard.setOnClickListener {
            findNavController().navigate(R.id.nav_stats)
        }
    }

    private fun setupStatsCard() {
        statsJob?.cancel()
        statsJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) { statsRepository.getSummary() }
                binding.statsTime.text = StatsViewModel.formatDurationProgressive(summary.totalListeningMs)
                binding.statsPlays.text = summary.totalSessions.toString()
                binding.statsSongs.text = summary.uniqueSongs.toString()
                binding.statsArtists.text = summary.uniqueArtists.toString()
                binding.statsCard.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.statsCard.visibility = View.GONE
            }
        }
    }

    private fun checkIsEmpty() {
        _binding?.empty?.isVisible = !currentContent.isLoading && homeAdapter.isNullOrEmpty
    }

    private fun checkForMargins() {
        _binding?.let { checkForMargins(it.recyclerView) }
    }

    override fun onClick(view: View) {
        val binding = _binding ?: return
        when (view) {
            binding.myTopTracks -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.TopTracks))
            }

            binding.lastAdded -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.RecentSongs))
            }

            binding.history -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.History))
            }

            binding.shuffleButton -> {
                libraryViewModel.allSongs().observe(viewLifecycleOwner) {
                    playerViewModel.openAndShuffleQueue(it)
                }
            }

            binding.moods -> {
                findNavController().navigate(R.id.nav_moods)
            }

            binding.tags -> {
                findNavController().navigate(R.id.nav_tags)
            }

            binding.instruments -> {
                findNavController().navigate(R.id.nav_instruments)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForMargins()
    }

    override fun onPause() {
        super.onPause()
        _binding?.recyclerView?.stopScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        _binding?.recyclerView?.adapter = null
        _binding?.recyclerView?.layoutManager = null
        homeAdapter = null
        _binding = null
    }

    override fun onMediaContentChanged() {
        libraryViewModel.forceReload(ReloadType.Suggestions)
    }

    override fun onFavoriteContentChanged() {
        libraryViewModel.forceReload(ReloadType.Suggestions)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createSuggestionAdapter(suggestion: Suggestion): RecyclerView.Adapter<*> {
        return when (suggestion.type) {
            ContentType.TopArtists,
            ContentType.RecentArtists,
            ContentType.HistoryArtists -> ArtistAdapter(
                activity = mainActivity,
                dataSet = (suggestion.items as List<Artist>),
                itemLayoutRes = R.layout.item_artist,
                callback = this
            )

            ContentType.TopAlbums,
            ContentType.RecentAlbums,
            ContentType.HistoryAlbums -> AlbumAdapter(
                activity = mainActivity,
                dataSet = (suggestion.items as List<Album>),
                itemLayoutRes = R.layout.item_album_gradient,
                callback = this
            )

            ContentType.TopTracks,
            ContentType.History,
            ContentType.RecentSongs,
            ContentType.Favorites,
            ContentType.NotRecentlyPlayed -> SongAdapter(
                activity = mainActivity,
                dataSet = (suggestion.items as List<Song>),
                itemLayoutRes = R.layout.item_image,
                callback = this
            )
        }
    }

    override fun suggestionClick(suggestion: Suggestion) {
        when (suggestion.type) {
            ContentType.Favorites -> {
                libraryViewModel.favoritePlaylist().observe(viewLifecycleOwner) {
                    findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(it.playListId))
                }
            }

            else -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(suggestion.type))
            }
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_album_detail,
            albumDetailArgs(album.id),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun albumMenuItemClick(
        album: Album,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = album.onAlbumMenu(this, menuItem)

    override fun albumsMenuItemClick(albums: List<Album>, menuItem: MenuItem) {
        albums.onAlbumsMenu(this, menuItem)
    }

    override fun artistClick(artist: Artist, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_artist_detail,
            artistDetailArgs(artist),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun artistMenuItemClick(
        artist: Artist,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = artist.onArtistMenu(this, menuItem)

    override fun artistsMenuItemClick(artists: List<Artist>, menuItem: MenuItem) {
        artists.onArtistsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library, menu)
        menu.removeItem(R.id.action_scan)
        menu.removeItem(R.id.action_equalizer)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_view_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_settings) {
            findNavController().navigate(R.id.nav_settings)
            return true
        } else if (menuItem.itemId == R.id.action_library_stats) {
            findNavController().navigate(R.id.nav_library_stats)
            return true
        } else if (menuItem.itemId == R.id.action_moods) {
            findNavController().navigate(R.id.nav_moods)
            return true
        } else if (menuItem.itemId == R.id.action_tags) {
            findNavController().navigate(R.id.nav_tags)
            return true
        } else if (menuItem.itemId == R.id.action_instruments) {
            findNavController().navigate(R.id.nav_instruments)
            return true
        }
        return false
    }

    override fun scrollToTop() {
        _binding?.let {
            it.container.scrollTo(0, 0)
            it.appBarLayout.setExpanded(true)
        }
    }
}