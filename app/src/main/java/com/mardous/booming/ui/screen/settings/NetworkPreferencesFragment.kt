package com.mardous.booming.ui.screen.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import com.mardous.booming.R
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.screen.scrobbling.ScrobblingServiceLoginFragment
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class NetworkPreferencesFragment : AbsMainActivityFragment() {
    private val settingsViewModel: SettingsViewModel by activityViewModel()
    private val lyricsViewModel: com.mardous.booming.ui.screen.lyrics.LyricsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BoomingMusicTheme {
                    NetworkSettingsComposeScreen(
                        viewModel = settingsViewModel,
                        libraryViewModel = libraryViewModel,
                        onBackClick = { findNavController().navigateUp() },
                        onLastFmProfileClick = { findNavController().navigate(R.id.action_to_lastfmProfile) },
                        onPendingScrobblesClick = { findNavController().navigate(R.id.action_to_pendingScrobbles) },
                        onLoginClick = { service ->
                            ScrobblingServiceLoginFragment.create(service)
                                .show(childFragmentManager, "${service.name}_LOGIN_DIALOG")
                        },
                        onClearLyricsClick = {
                            lyricsViewModel.deleteLyrics()
                            requireContext().showToast(R.string.lyrics_cleared)
                        }
                    )
                }
            }
        }
    }

    override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {}
    override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean = false
}
