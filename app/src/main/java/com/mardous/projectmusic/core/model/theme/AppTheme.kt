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

package com.mardous.projectmusic.core.model.theme

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.mardous.projectmusic.R
import com.mardous.projectmusic.util.GeneralTheme
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.UITheme

class AppTheme private constructor(
    val id: String,
    @StyleRes
    val themeRes: Int,
    val applyDynamicColors: Boolean,
    val seedColor: Int? = null
) {

    val isBlackTheme: Boolean
        get() = id == GeneralTheme.BLACK

    enum class Mode(@StyleRes val themeRes: Int) {
        Light(R.style.Theme_ProjectMusic_Light),
        Dark(R.style.Theme_ProjectMusic),
        Black(R.style.Theme_ProjectMusic_Black),
        FollowSystem(R.style.Theme_ProjectMusic_FollowSystem)
    }

    companion object {
        fun createAppTheme(context: Context): AppTheme {
            val generalTheme = Preferences.generalTheme
            val themeMode = Preferences.getThemeMode(generalTheme)
            val eraPrimarySeed = Preferences.eraPrimarySeed
            
            if (DynamicColors.isDynamicColorAvailable() || eraPrimarySeed != 0) {
                if (Preferences.isMaterialYouTheme && Preferences.accentColor == null && !Preferences.eraHarmonyMode) {
                    return AppTheme(
                        id = generalTheme,
                        themeRes = themeMode.themeRes,
                        applyDynamicColors = true
                    )
                }
                if (context is ContextThemeWrapper || eraPrimarySeed != 0) {
                    return AppTheme(
                        id = generalTheme,
                        themeRes = themeMode.themeRes,
                        applyDynamicColors = true,
                        seedColor = Preferences.accentColor ?: eraPrimarySeed
                    )
                }
            }
            return AppTheme(
                id = generalTheme,
                themeRes = themeMode.themeRes,
                applyDynamicColors = false
            )
        }
    }
}