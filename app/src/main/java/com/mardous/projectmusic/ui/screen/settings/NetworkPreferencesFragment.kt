package com.mardous.projectmusic.ui.screen.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.ui.component.base.AbsMainActivityFragment
import com.mardous.projectmusic.ui.screen.scrobbling.ScrobblingServiceLoginFragment
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class NetworkPreferencesFragment : AbsMainActivityFragment() {
    private val settingsViewModel: SettingsViewModel by activityViewModel()
    private val updateViewModel: com.mardous.projectmusic.ui.screen.update.UpdateViewModel by activityViewModel()
    private val lyricsViewModel: com.mardous.projectmusic.ui.screen.lyrics.LyricsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProjectMusicTheme {
                    NetworkSettingsComposeScreen(
                        viewModel = settingsViewModel,
                        updateViewModel = updateViewModel,
                        onBackClick = { findNavController().navigateUp() },
                        onCheckForUpdates = { updateViewModel.searchForUpdate(true) },
                        onScrobblingLogin = { service ->
                            ScrobblingServiceLoginFragment.create(service)
                                .show(childFragmentManager, "LOGIN_DIALOG")
                        },
                        onPendingScrobblesClick = { 
                            findNavController().navigate(com.mardous.projectmusic.R.id.action_to_pendingScrobbles) 
                        },
                        onClearLyricsClick = {
                            lyricsViewModel.deleteLyrics()
                            requireContext().showToast(com.mardous.projectmusic.R.string.lyrics_cleared)
                        },
                        highlightKey = arguments?.getString("highlightKey")
                    )
                }
            }
        }
    }

    override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {}
    override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean = false
}
