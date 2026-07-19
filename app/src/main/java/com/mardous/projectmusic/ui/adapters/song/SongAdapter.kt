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

package com.mardous.projectmusic.ui.adapters.song

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mardous.projectmusic.R
import com.mardous.projectmusic.coil.DEFAULT_SONG_IMAGE
import com.mardous.projectmusic.core.model.action.SongClickBehavior
import com.mardous.projectmusic.core.model.sort.SortKey
import com.mardous.projectmusic.core.sort.SongSortMode
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.extensions.isActivated
import com.mardous.projectmusic.extensions.loadPaletteImage
import com.mardous.projectmusic.extensions.media.asSectionName
import com.mardous.projectmusic.extensions.media.displayArtistName
import com.mardous.projectmusic.extensions.media.songInfo
import com.mardous.projectmusic.extensions.resources.hide
import com.mardous.projectmusic.extensions.showToast
import com.mardous.projectmusic.extensions.files.getCanonicalPathSafe
import com.mardous.projectmusic.extensions.utilities.buildInfoString
import com.mardous.projectmusic.ui.ISongCallback
import com.mardous.projectmusic.ui.component.base.AbsMultiSelectAdapter
import com.mardous.projectmusic.ui.component.base.MediaEntryViewHolder
import com.mardous.projectmusic.ui.component.menu.OnClickMenu
import com.mardous.projectmusic.ui.screen.player.PlayerViewModel
import com.mardous.projectmusic.util.Preferences
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.koin.androidx.viewmodel.ext.android.getViewModel

@SuppressLint("NotifyDataSetChanged")
@Suppress("LeakingThis")
open class SongAdapter(
    protected val activity: FragmentActivity,
    dataSet: List<Song>,
    @LayoutRes protected val itemLayoutRes: Int = R.layout.item_list,
    protected val sortMode: SongSortMode? = null,
    protected val swipeContext: com.mardous.projectmusic.core.model.swipe.SwipeContext? = null,
    protected val callback: ISongCallback? = null,
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(activity, R.menu.menu_media_selection), PopupTextProvider,
    com.mardous.projectmusic.util.SwipeAndDragHelper.ActionCompletionContract {

    private val touchHelper = androidx.recyclerview.widget.ItemTouchHelper(
        com.mardous.projectmusic.util.SwipeAndDragHelper(this, swipeContext, false)
    )

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        touchHelper.attachToRecyclerView(recyclerView)
    }

    open var dataSet: List<Song> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    protected open fun createViewHolder(view: View, viewType: Int): ViewHolder {
        return ViewHolder(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song: Song = dataSet[position]
        val isChecked = isChecked(song)
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getSongTitle(song)
        holder.text?.text = getSongText(song)
        // Check if imageContainer exists, so we can have a smooth transition without
        // CardView clipping, if it doesn't exist in current layout set transition name to image instead.
        if (holder.imageContainer != null) {
            holder.imageContainer.transitionName = song.id.toString()
        } else {
            holder.image?.transitionName = song.id.toString()
        }
        holder.loadPaletteImage(song, DEFAULT_SONG_IMAGE)
    }

    private fun getSongTitle(song: Song): String {
        return when (sortMode?.selectedKey) {
            SortKey.FileName -> song.fileName
            else -> song.title
        }
    }

    protected open fun getSongText(song: Song): String? {
        return when (sortMode?.selectedKey) {
            SortKey.Year -> if (song.year > 0) {
                buildInfoString(song.displayArtistName(), song.year.toString())
            } else {
                song.displayArtistName()
            }

            SortKey.Album -> buildInfoString(song.displayArtistName(), song.albumName)

            else -> song.songInfo()
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getIdentifier(position: Int): Song? {
        return dataSet[position]
    }

    override fun getName(item: Song): String? {
        return item.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        callback?.songsMenuItemClick(selection, menuItem)
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        val song = dataSet.getOrNull(position) ?: return ""
        return when (sortMode?.selectedKey) {
            SortKey.Album -> song.albumName.asSectionName(sortMode)
            SortKey.Artist -> song.displayArtistName().asSectionName(sortMode)
            SortKey.AZ -> song.title.asSectionName(sortMode)
            SortKey.Year -> ""
            SortKey.FileName -> song.fileName.asSectionName(sortMode)
            else -> song.title.asSectionName(sortMode)
        }
    }

    override fun onViewSwiped(position: Int, direction: Int) {
        val song = dataSet.getOrNull(position) ?: return
        val action = if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            Preferences.getSwipeLeftAction(swipeContext!!)
        } else {
            Preferences.getSwipeRightAction(swipeContext!!)
        }
        
        if (action != com.mardous.projectmusic.core.model.swipe.SwipeAction.NONE) {
            val menuId = getMenuIdForSwipeAction(action)
            if (menuId != 0) {
                val menuItem = android.widget.PopupMenu(activity, null).menu.add(0, menuId, 0, "")
                onSongMenuItemClick(song, menuItem, position)
                activity.showToast(action.titleRes)
            } else {
                // Handle custom actions that do not map directly to a menu item
                when (action) {
                    com.mardous.projectmusic.core.model.swipe.SwipeAction.TOGGLE_FAVORITE -> {
                        if (activity is androidx.lifecycle.LifecycleOwner) {
                            val repository: com.mardous.projectmusic.data.local.repository.Repository by org.koin.java.KoinJavaComponent.inject(com.mardous.projectmusic.data.local.repository.Repository::class.java)
                            activity.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val isFav = repository.toggleFavorite(song)
                                val msgRes = if (isFav) R.string.added_to_favorites_label else R.string.removed_from_favorites_label
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    activity.showToast(msgRes)
                                }
                            }
                        }
                    }
                    com.mardous.projectmusic.core.model.swipe.SwipeAction.REMOVE_FROM_QUEUE -> {
                        val playerViewModel = activity.getViewModel<PlayerViewModel>()
                        playerViewModel.removePosition(position)
                        activity.showToast(action.titleRes)
                        return // Skip notifyItemChanged so the item stays swiped out until the queue updates
                    }
                    com.mardous.projectmusic.core.model.swipe.SwipeAction.REMOVE_FROM_PLAYLIST,
                    com.mardous.projectmusic.core.model.swipe.SwipeAction.HIDE_SONG,
                    com.mardous.projectmusic.core.model.swipe.SwipeAction.DELETE_FILE -> {
                        val menuId = when (action) {
                            com.mardous.projectmusic.core.model.swipe.SwipeAction.REMOVE_FROM_PLAYLIST -> R.id.action_remove_from_playlist
                            com.mardous.projectmusic.core.model.swipe.SwipeAction.DELETE_FILE -> R.id.action_delete_from_device
                            // HIDE_SONG has no XML action, so we can use a custom logic if implemented, or just 0
                            else -> 0 
                        }
                        
                        if (menuId != 0) {
                            val menuItem = android.widget.PopupMenu(activity, null).menu.add(0, menuId, 0, "")
                            onSongMenuItemClick(song, menuItem, position)
                        } else if (action == com.mardous.projectmusic.core.model.swipe.SwipeAction.HIDE_SONG) {
                            if (activity is androidx.lifecycle.LifecycleOwner) {
                                val inclExclDao: com.mardous.projectmusic.data.local.database.dao.InclExclDao by org.koin.java.KoinJavaComponent.inject(com.mardous.projectmusic.data.local.database.dao.InclExclDao::class.java)
                                activity.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val safePath = java.io.File(song.data).getCanonicalPathSafe()
                                    inclExclDao.insertPath(com.mardous.projectmusic.data.local.database.metadata.InclExclEntity(safePath, com.mardous.projectmusic.data.local.database.dao.InclExclDao.BLACKLIST))
                                }
                            }
                        }
                        activity.showToast(action.titleRes)
                        return // Skip notifyItemChanged
                    }
                    else -> {
                        activity.showToast(action.titleRes)
                    }
                }
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            notifyItemChanged(position)
        }
    }

    private fun onSongMenuItemClick(song: Song, item: MenuItem, position: Int): Boolean {
        if (item.itemId == R.id.action_play) {
            val playerViewModel = activity.getViewModel<PlayerViewModel>()
            val playOptionBehavior = Preferences.playOptionClickBehavior
            playerViewModel.openSongs(position, dataSet, playOptionBehavior)
            return true
        }
        return callback?.songMenuItemClick(song, item, null) ?: false
    }

    private fun getMenuIdForSwipeAction(action: com.mardous.projectmusic.core.model.swipe.SwipeAction): Int {
        return when (action) {
            com.mardous.projectmusic.core.model.swipe.SwipeAction.PLAY_NEXT -> R.id.action_play_next
            com.mardous.projectmusic.core.model.swipe.SwipeAction.ADD_TO_QUEUE -> R.id.action_add_to_playing_queue
            com.mardous.projectmusic.core.model.swipe.SwipeAction.ADD_TO_PLAYLIST -> R.id.action_add_to_playlist
            com.mardous.projectmusic.core.model.swipe.SwipeAction.TOGGLE_FAVORITE -> 0 // Handle custom if needed
            com.mardous.projectmusic.core.model.swipe.SwipeAction.REMOVE_FROM_PLAYLIST -> 0 // Handled in onViewSwiped
            com.mardous.projectmusic.core.model.swipe.SwipeAction.REMOVE_FROM_QUEUE -> 0 // Handled in onViewSwiped
            com.mardous.projectmusic.core.model.swipe.SwipeAction.DELETE_FILE -> 0 // Handled in onViewSwiped
            com.mardous.projectmusic.core.model.swipe.SwipeAction.SHARE -> R.id.action_share
            com.mardous.projectmusic.core.model.swipe.SwipeAction.SONG_INFO -> R.id.action_details
            com.mardous.projectmusic.core.model.swipe.SwipeAction.EDIT_TAGS -> R.id.action_tag_editor
            com.mardous.projectmusic.core.model.swipe.SwipeAction.OPEN_ALBUM -> R.id.action_go_to_album
            com.mardous.projectmusic.core.model.swipe.SwipeAction.OPEN_ARTIST -> R.id.action_go_to_artist
            com.mardous.projectmusic.core.model.swipe.SwipeAction.NONE,
            com.mardous.projectmusic.core.model.swipe.SwipeAction.HIDE_SONG,
            com.mardous.projectmusic.core.model.swipe.SwipeAction.COPY_FILE_PATH,
            com.mardous.projectmusic.core.model.swipe.SwipeAction.GO_TO_FOLDER -> 0
        }
    }

    open inner class ViewHolder(view: View) : MediaEntryViewHolder(view) {
        protected open val song: Song
            get() = dataSet[bindingAdapterPosition]

        @get:MenuRes
        protected open val songMenuRes: Int
            get() = R.menu.menu_item_song

        protected val sharedElements: Array<Pair<View, String>>?
            get() = if (image != null && image.isVisible) arrayOf(image to image.transitionName) else null

        protected val songClickBehavior: SongClickBehavior
            get() = Preferences.songClickAction

        @CallSuper
        protected open fun onPrepareSongMenu(menu: Menu) {
            menu.findItem(R.id.action_play)
                ?.isVisible = !songClickBehavior.isAbleToPlay || Preferences.playOptionAlwaysVisible
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.action_play) {
                val playerViewModel = activity.getViewModel<PlayerViewModel>()
                val playOptionBehavior = Preferences.playOptionClickBehavior
                playerViewModel.openSongs(bindingAdapterPosition, dataSet, playOptionBehavior)
                return true
            }
            return callback?.songMenuItemClick(song, item, sharedElements) ?: false
        }

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                val songClickBehavior = Preferences.songClickAction
                val playerViewModel = activity.getViewModel<PlayerViewModel>()
                playerViewModel.openSongs(bindingAdapterPosition, dataSet, songClickBehavior)
                if (!songClickBehavior.isAbleToPlay) {
                    activity.showToast(R.string.added_title_to_playing_queue)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            return toggleChecked(bindingAdapterPosition)
        }

        init {
            play?.hide()
            menu?.setOnClickListener(object : OnClickMenu() {
                override val popupMenuRes: Int
                    get() = songMenuRes

                override fun onPreparePopup(menu: Menu) {
                    onPrepareSongMenu(menu)
                }

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return onSongMenuItemClick(item)
                }
            })
        }
    }

    init {
        setHasStableIds(true)
    }

}