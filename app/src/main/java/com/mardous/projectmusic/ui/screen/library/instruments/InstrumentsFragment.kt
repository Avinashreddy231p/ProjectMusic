package com.mardous.projectmusic.ui.screen.library.instruments

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

class InstrumentsFragment : Fragment() {

    private val instrumentsViewModel: InstrumentsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    InstrumentsScreen(
                        viewModel = instrumentsViewModel,
                        onInstrumentClick = { instrument ->
                            val bundle = Bundle().apply {
                                putLong("instrument_id", instrument.id)
                            }
                            findNavController().navigate(R.id.nav_instrument_detail, bundle)
                        },
                        onAssignSongs = {
                            val dialog = SongSelectionDialog.newInstance()
                            dialog.setOnConfirmListener { songIds ->
                                instrumentsViewModel.assignSongsToSelected(songIds)
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
