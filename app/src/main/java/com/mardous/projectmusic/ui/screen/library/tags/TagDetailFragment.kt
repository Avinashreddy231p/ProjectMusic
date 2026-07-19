package com.mardous.projectmusic.ui.screen.library.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.screen.player.PlayerViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TagDetailFragment : Fragment() {

    private val viewModel: TagDetailViewModel by viewModel {
        parametersOf(arguments?.getLong("tag_id") ?: -1L)
    }
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    TagDetailScreen(
                        viewModel = viewModel,
                        onSongClick = { song ->
                            playerViewModel.openQueue(listOf(song), 0, true)
                        },
                        onAddSongsClick = { findNavController().navigate(R.id.nav_search) },
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
