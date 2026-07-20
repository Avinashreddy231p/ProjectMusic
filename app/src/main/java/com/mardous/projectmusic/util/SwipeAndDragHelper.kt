package com.mardous.projectmusic.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.swipe.SwipeAction
import com.mardous.projectmusic.core.model.swipe.SwipeContext
import kotlin.math.abs

class SwipeAndDragHelper(
    private val contract: ActionCompletionContract,
    private val swipeContext: SwipeContext? = null,
    private val dragEnabled: Boolean = true
) : ItemTouchHelper.Callback() {

    private val bgPaint = Paint()

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = if (dragEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        var swipeFlags = 0

        if (swipeContext != null) {
            val leftAction = Preferences.getSwipeLeftAction(swipeContext)
            if (leftAction != SwipeAction.NONE) {
                swipeFlags = swipeFlags or ItemTouchHelper.LEFT
            }

            val rightAction = Preferences.getSwipeRightAction(swipeContext)
            if (rightAction != SwipeAction.NONE) {
                swipeFlags = swipeFlags or ItemTouchHelper.RIGHT
            }
        }

        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        contract.onViewMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        contract.onViewSwiped(viewHolder.bindingAdapterPosition, direction)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return dragEnabled
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val context = recyclerView.context
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && swipeContext != null) {
            val isLeftSwipe = dX < 0
            val action = if (isLeftSwipe) {
                Preferences.getSwipeLeftAction(swipeContext)
            } else {
                Preferences.getSwipeRightAction(swipeContext)
            }

            if (action != SwipeAction.NONE) {
                // Background
                bgPaint.color = ContextCompat.getColor(context, R.color.md_theme_primaryContainer)
                
                val iconRes = getActionIcon(action)
                val icon = if (iconRes != 0) ContextCompat.getDrawable(context, iconRes) else null
                
                if (isLeftSwipe) {
                    val bgRect = Rect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    c.drawRect(bgRect, bgPaint)
                    
                    if (icon != null) {
                        icon.setTint(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer))
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                } else {
                    val bgRect = Rect(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    c.drawRect(bgRect, bgPaint)

                    if (icon != null) {
                        icon.setTint(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer))
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }
            }
            
            // Still fade the item
            val alpha = 1 - (abs(dX) / recyclerView.width.toFloat())
            itemView.alpha = alpha
            itemView.translationX = dX
            
        } else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val alpha = 1 - (abs(dX) / recyclerView.width.toFloat())
            itemView.alpha = alpha
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
    
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1f
        viewHolder.itemView.translationX = 0f
    }

    
    private fun getActionIcon(action: SwipeAction): Int {
        return when (action) {
            SwipeAction.PLAY_NEXT -> R.drawable.ic_queue_play_next_24dp
            SwipeAction.ADD_TO_QUEUE -> R.drawable.ic_queue_music_24dp
            SwipeAction.ADD_TO_PLAYLIST -> R.drawable.ic_playlist_add_24dp
            SwipeAction.TOGGLE_FAVORITE -> R.drawable.ic_favorite_24dp
            SwipeAction.HIDE_SONG -> R.drawable.ic_visibility_off_24dp
            SwipeAction.REMOVE_FROM_PLAYLIST -> R.drawable.ic_delete_24dp
            SwipeAction.REMOVE_FROM_QUEUE -> R.drawable.ic_delete_24dp
            SwipeAction.DELETE_FILE -> R.drawable.ic_delete_24dp
            SwipeAction.SHARE -> R.drawable.ic_share_24dp
            SwipeAction.COPY_FILE_PATH -> R.drawable.ic_content_paste_24dp
            SwipeAction.SONG_INFO -> R.drawable.ic_info_24dp
            SwipeAction.EDIT_TAGS -> R.drawable.ic_edit_24dp
            SwipeAction.OPEN_ALBUM -> R.drawable.ic_album_24dp
            SwipeAction.OPEN_ARTIST -> R.drawable.ic_artist_24dp
            SwipeAction.GO_TO_FOLDER -> R.drawable.ic_folder_24dp
            SwipeAction.NONE -> 0
        }
    }

    interface ActionCompletionContract {
        fun onViewMoved(oldPosition: Int, newPosition: Int) {}
        fun onViewSwiped(position: Int, direction: Int) {}
    }
}
