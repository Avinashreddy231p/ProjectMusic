/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.ui.screen.library.playlists

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.GridViewType
import com.mardous.projectmusic.core.sort.PlaylistSortMode
import com.mardous.projectmusic.data.local.database.metadata.PlaylistWithSongs
import com.mardous.projectmusic.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.projectmusic.extensions.navigation.playlistDetailArgs
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.ui.IPlaylistCallback
import com.mardous.projectmusic.ui.adapters.PlaylistAdapter
import com.mardous.projectmusic.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.projectmusic.ui.component.menu.onPlaylistMenu
import com.mardous.projectmusic.ui.component.menu.onPlaylistsMenu
import com.mardous.projectmusic.ui.dialogs.playlists.CreatePlaylistDialog
import com.mardous.projectmusic.ui.dialogs.playlists.ImportPlaylistDialog
import com.mardous.projectmusic.ui.screen.library.PlaylistImportState
import com.mardous.projectmusic.ui.screen.library.ReloadType
import kotlinx.coroutines.flow.collectLatest

/**
 * @author Christians M. A. (mardous)
 */
class PlaylistListFragment : AbsRecyclerViewCustomGridSizeFragment<PlaylistAdapter, GridLayoutManager>(),
    IPlaylistCallback {

    private var playlistToExport: PlaylistWithSongs? = null
    private var selectedFormat = "m3u"
    
    private val createPlaylistFile = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        uri?.let {
            playlistToExport?.let { p ->
                libraryViewModel.exportPlaylistToFile(requireContext(), p, it, selectedFormat)
                showToast(getString(R.string.saved_playlist_x, p.playlistEntity.playlistName))
            }
        }
    }

    private fun showExportDialog(playlist: PlaylistWithSongs) {
        playlistToExport = playlist
        val formats = arrayOf("JSON", "M3U", "PLS")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_export_playlist)
            .setItems(formats) { _, which ->
                selectedFormat = formats[which].lowercase()
                val mimeType = when (selectedFormat) {
                    "json" -> "application/json"
                    "pls" -> "audio/scpls"
                    else -> "audio/mpegurl"
                }
                val fileName = "${playlist.playlistEntity.playlistName}.$selectedFormat"
                createPlaylistFile.launch(fileName)
            }
            .show()
    }

    private val pickPlaylistFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { 
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(it, flags)
            }
            libraryViewModel.importPlaylistFromFile(requireContext(), it) 
        }
    }

    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private var progressTextView: android.widget.TextView? = null

    private fun toggleProgressDialog(show: Boolean) {
        if (show) {
            if (progressDialog == null) {
                val view = layoutInflater.inflate(R.layout.dialog_loading, null)
                progressTextView = view.findViewById(R.id.progress_text)
                progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.preparing_playback_title)
                    .setView(view)
                    .setCancelable(false)
                    .create()
            }
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
            progressDialog = null
            progressTextView = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getPlaylists().observe(viewLifecycleOwner) { playlists ->
            adapter?.dataSet = playlists
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            libraryViewModel.playlistImportState.collectLatest { state ->
                when (state) {
                    PlaylistImportState.Loading -> {
                        toggleProgressDialog(true)
                        progressTextView?.visibility = View.GONE
                    }
                    is PlaylistImportState.Progress -> {
                        toggleProgressDialog(true)
                        progressTextView?.apply {
                            visibility = View.VISIBLE
                            text = getString(R.string.progress_x_y, state.current, state.total)
                        }
                    }
                    is PlaylistImportState.Success -> {
                        toggleProgressDialog(false)
                        showToast(state.message)
                        libraryViewModel.resetPlaylistImportState()
                    }
                    is PlaylistImportState.Error -> {
                        toggleProgressDialog(false)
                        showToast(state.message)
                        libraryViewModel.resetPlaylistImportState()
                    }
                    PlaylistImportState.Idle -> {
                        toggleProgressDialog(false)
                    }
                }
            }
        }
    }

    override val titleRes: Int = R.string.playlists_label
    override val isShuffleVisible: Boolean = false
    override val emptyMessageRes: Int
        get() = R.string.no_device_playlists

    override val maxGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.max_playlist_columns_land)
        else resources.getInteger(R.integer.max_playlist_columns)

    override val itemLayoutRes: Int
        get() = if (isGridMode) R.layout.item_playlist
        else R.layout.item_list

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireContext(), gridSize)
    }

    override fun createAdapter(): PlaylistAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return PlaylistAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun playlistClick(playlist: PlaylistWithSongs) {
        findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(playlist.playlistEntity.playListId))
    }

    override fun playlistMenuItemClick(playlist: PlaylistWithSongs, menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_export_playlist) {
            showExportDialog(playlist)
            return true
        }
        return playlist.onPlaylistMenu(this, menuItem)
    }

    override fun playlistsMenuItemClick(playlists: List<PlaylistWithSongs>, menuItem: MenuItem) {
        if (menuItem.itemId == R.id.action_export_playlist && playlists.size == 1) {
            showExportDialog(playlists.first())
        } else {
            playlists.onPlaylistsMenu(this, menuItem)
        }
    }

    override fun onMediaContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onFavoriteContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        menu.removeItem(R.id.action_view_type)
        menu.add(0, R.id.action_new_playlist, 0, R.string.new_playlist_title)
        menu.add(0, R.id.action_import_playlist, 0, R.string.action_import_playlist)
        menu.add(0, R.id.action_import_from_file, 0, R.string.action_import_from_file)
        PlaylistSortMode.AllPlaylists.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (PlaylistSortMode.AllPlaylists.sortItemSelected(item)) {
            libraryViewModel.forceReload(ReloadType.Playlists)
            return true
        }
        return when (item.itemId) {
            R.id.action_new_playlist -> {
                CreatePlaylistDialog()
                    .show(childFragmentManager, "NEW_PLAYLIST")
                true
            }
            R.id.action_import_playlist -> {
                ImportPlaylistDialog().show(childFragmentManager, "IMPORT_PLAYLIST")
                true
            }
            R.id.action_import_from_file -> {
                pickPlaylistFile.launch(arrayOf("application/json", "audio/mpegurl", "audio/x-mpegurl", "audio/scpls", "application/pls+xml", "*/*"))
                true
            }
            else -> super.onMenuItemSelected(item)
        }
    }

    override fun getSavedViewType(): GridViewType {
        return GridViewType.Normal
    }

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(GRID_SIZE, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val GRID_SIZE = "playlists_grid_size"
    }
}