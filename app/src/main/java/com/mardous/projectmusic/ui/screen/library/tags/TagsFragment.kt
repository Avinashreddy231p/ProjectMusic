package com.mardous.projectmusic.ui.screen.library.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.dialogs.songs.SongSelectionDialog
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class TagsFragment : Fragment() {

    private val tagsViewModel: TagsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    TagsScreen(
                        viewModel = tagsViewModel,
                        onTagClick = { tag ->
                            val bundle = Bundle().apply {
                                putLong("tag_id", tag.id)
                            }
                            findNavController().navigate(R.id.nav_tag_detail, bundle)
                        },
                        onAssignSongs = {
                            val dialog = SongSelectionDialog.newInstance()
                            dialog.setOnConfirmListener { songIds ->
                                tagsViewModel.assignSongsToSelected(songIds)
                            }
                            dialog.show(childFragmentManager, "SONG_SELECTION")
                        },
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
