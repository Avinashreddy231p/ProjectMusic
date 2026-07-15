package com.mardous.booming.ui.screen.scrobbling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class PendingScrobblesFragment : Fragment() {

    private val viewModel by viewModel<PendingScrobblesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    PendingScrobblesScreen(
                        viewModel = viewModel,
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
