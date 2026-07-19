package com.mardous.projectmusic.ui.screen.librarystats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

import android.view.MenuItem
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.ui.ISongCallback
import com.mardous.projectmusic.ui.component.menu.onSongMenu

class LibraryStatsFragment : Fragment(), ISongCallback {

    private val viewModel by viewModel<LibraryStatsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    LibraryStatsScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().navigateUp() },
                        onCategoryClick = { category, itemName -> 
                            val bundle = Bundle().apply {
                                putString("category", category)
                                putString("itemName", itemName)
                            }
                            findNavController().navigate(com.mardous.projectmusic.R.id.nav_library_stats_detail, bundle)
                        },
                        songCallback = this@LibraryStatsFragment,
                        activity = requireActivity()
                    )
                }
            }
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        // Multi-select menu action handling (optional/if needed)
    }
}
