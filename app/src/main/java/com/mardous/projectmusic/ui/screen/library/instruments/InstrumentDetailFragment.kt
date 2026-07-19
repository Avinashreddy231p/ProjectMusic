package com.mardous.projectmusic.ui.screen.library.instruments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mardous.projectmusic.R
import com.mardous.projectmusic.ui.screen.player.PlayerViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class InstrumentDetailFragment : Fragment() {

    private val args: InstrumentDetailFragmentArgs by navArgs()
    private val instrumentDetailViewModel: InstrumentDetailViewModel by viewModel { parametersOf(args.instrumentId) }
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    InstrumentDetailScreen(
                        viewModel = instrumentDetailViewModel,
                        onSongClick = { song ->
                            playerViewModel.openQueue(listOf(song), 0, true)
                        },
                        onArtistClick = { artistName ->
                            val bundle = Bundle().apply {
                                putString("artist_name", artistName)
                                putLong("artist_id", 0L)
                            }
                            findNavController().navigate(R.id.nav_artist_detail, bundle)
                        },
                        onAlbumClick = { albumId ->
                            val bundle = Bundle().apply {
                                putLong("album_id", albumId)
                            }
                            findNavController().navigate(R.id.nav_album_detail, bundle)
                        },
                        onInstrumentClick = { instrument ->
                            val bundle = Bundle().apply {
                                putLong("instrument_id", instrument.id)
                            }
                            findNavController().navigate(R.id.nav_instrument_detail, bundle)
                        },
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
