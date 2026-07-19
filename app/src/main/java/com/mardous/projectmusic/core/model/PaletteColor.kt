package com.mardous.projectmusic.core.model

import android.content.Context
import com.mardous.projectmusic.extensions.resolveColor
import com.mardous.projectmusic.extensions.resources.withAlpha
import com.mardous.projectmusic.ui.component.views.PlaceholderDrawable

class PaletteColor(
    val backgroundColor: Int,
    val primaryColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int
) {
    companion object {
        fun errorColor(context: Context): PaletteColor {
            val backgroundColor = context.resolveColor(PlaceholderDrawable.BACKGROUND_COLOR)
            val foregroundColor = context.resolveColor(PlaceholderDrawable.FOREGROUND_COLOR)
            return PaletteColor(
                backgroundColor = backgroundColor,
                primaryColor = foregroundColor,
                primaryTextColor = foregroundColor,
                secondaryTextColor = foregroundColor.withAlpha(0.75f)
            )
        }
    }
}