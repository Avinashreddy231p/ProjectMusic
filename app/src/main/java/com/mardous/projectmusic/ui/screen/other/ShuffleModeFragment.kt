package com.mardous.projectmusic.ui.screen.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.projectmusic.ui.screen.library.LibraryViewModel
import com.mardous.projectmusic.ui.screen.player.PlayerViewModel
import com.mardous.projectmusic.ui.theme.ProjectMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ShuffleModeFragment : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMusicTheme {
                    ShuffleModeBottomSheet(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}