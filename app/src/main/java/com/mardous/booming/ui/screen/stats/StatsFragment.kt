package com.mardous.booming.ui.screen.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

import androidx.navigation.fragment.findNavController

class StatsFragment : Fragment() {

    private val viewModel: StatsViewModel by viewModel()

    private val csvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsv(it) }
    }

    private val jsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportJson(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    StatsScreen(
                        viewModel = viewModel,
                        onBackClick = { getOnBackPressedDispatcher().onBackPressed() },
                        onCategoryClick = { category, itemName -> 
                            findNavController().navigate(
                                StatsFragmentDirections.actionNavStatsToNavStatsDetail(category, itemName)
                            )
                        },
                        onExportCsv = { csvLauncher.launch("listening_history.csv") },
                        onExportJson = { jsonLauncher.launch("listening_history.json") }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }

    private fun materialSharedAxis(view: View) {
        // transition animation handled by parent
    }
}
