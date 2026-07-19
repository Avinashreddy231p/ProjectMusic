package com.mardous.projectmusic.ui.screen.stats

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.swipe.SwipeContext
import com.mardous.projectmusic.core.sort.SongSortMode
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.ui.ISongCallback
import com.mardous.projectmusic.ui.adapters.song.SongAdapter
import com.mardous.projectmusic.ui.component.base.AbsRecyclerViewFragment
import com.mardous.projectmusic.ui.component.menu.onSongMenu
import com.mardous.projectmusic.ui.component.menu.onSongsMenu
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import android.view.Menu
import android.view.MenuInflater

class StatsDetailFragment : AbsRecyclerViewFragment<SongAdapter, LinearLayoutManager>(), ISongCallback {

    private val category: String by lazy { arguments?.getString("category") ?: "" }
    private val itemName: String by lazy { arguments?.getString("itemName") ?: "" }

    private val detailViewModel: StatsDetailViewModel by viewModel {
        parametersOf(category, itemName)
    }

    override val titleRes: Int = R.string.app_name
    override val isShuffleVisible: Boolean = true
    override val emptyMessageRes: Int = R.string.no_songs_label

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        toolbar.title = itemName

        toolbar.setNavigationIcon(R.drawable.ic_back_24dp)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        detailViewModel.getSongs().observe(viewLifecycleOwner) { songs ->
            adapter?.dataSet = songs
        }
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun createAdapter(): SongAdapter {
        val adapter = SongAdapter(
            activity = requireActivity(),
            dataSet = ArrayList(),
            itemLayoutRes = R.layout.item_list,
            sortMode = SongSortMode.AllSongs,
            swipeContext = SwipeContext.STATS,
            callback = this
        )
        adapter.attachToRecyclerView(recyclerView)
        return adapter
    }

    override fun onShuffleClicked() {
        super.onShuffleClicked()
        adapter?.dataSet?.let { songs ->
            playerViewModel.openAndShuffleQueue(songs)
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

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> false 
            else -> adapter?.dataSet?.onSongsMenu(this, item) ?: false
        }
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        detailViewModel.loadSongs()
    }
}
