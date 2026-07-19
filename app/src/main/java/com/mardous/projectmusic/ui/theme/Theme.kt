/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.projectmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.player.PlayerColorScheme
import com.mardous.projectmusic.core.model.theme.EraThemeEngine
import com.mardous.projectmusic.util.Preferences
import com.mardous.projectmusic.util.UITheme

private val spotifyScheme = darkColorScheme(
    primary = spotifyGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1DB954).copy(alpha = 0.3f),
    onPrimaryContainer = Color.White,
    secondary = spotifyGrey,
    onSecondary = Color.White,
    secondaryContainer = spotifyDarkGrey,
    onSecondaryContainer = spotifyLightGrey,
    background = spotifyBlack,
    onBackground = Color.White,
    surface = spotifyBlack,
    onSurface = Color.White,
    surfaceVariant = spotifyGrey,
    onSurfaceVariant = spotifyLightGrey,
    outline = spotifyLightGrey,
    inverseOnSurface = spotifyBlack,
    inverseSurface = Color.White,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = spotifyDarkGrey,
    surfaceContainer = spotifyGrey,
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF3E3E3E)
)

private val spotifyLightScheme = lightColorScheme(
    primary = spotifyGreen,
    onPrimary = Color.White,
    primaryContainer = spotifyGreen.copy(alpha = 0.3f),
    onPrimaryContainer = Color.Black,
    secondary = spotifyGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBEBEB),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF6A6A6A),
    outline = Color(0xFF797979),
    inverseOnSurface = Color.White,
    inverseSurface = spotifyBlack,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE7E7E7),
    surfaceContainerHighest = Color(0xFFDFDFDF)
)

@Composable
fun ProjectMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    blackTheme: Boolean = Preferences.blackTheme,
    dynamicColor: Boolean = Preferences.isMaterialYouTheme,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val eraColorScheme = EraThemeEngine.generateColorScheme(darkTheme)
    
    var colorScheme = when {
        Preferences.uiTheme == UITheme.SPOTIFY -> if (darkTheme) spotifyScheme else spotifyLightScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> eraColorScheme
    }

    if (Preferences.uiTheme != UITheme.SPOTIFY && darkTheme && blackTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = colorResource(R.color.surfaceContainerBlack),
            surfaceContainerLow = colorResource(R.color.surfaceContainerLowBlack),
            surfaceContainerLowest = colorResource(R.color.surfaceContainerLowestBlack)
        )
    }

    val eraTypography = getEraTypography(resolveFontFamily(Preferences.eraFontFamily))
    val eraIcons = IconProvider.getIcons()

    CompositionLocalProvider(
        LocalAppIcons provides eraIcons
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = eraTypography,
            shapes = eraShapes(),
            content = content
        )
    }
}

@Composable
fun PlayerTheme(
    playerColorScheme: PlayerColorScheme,
    content: @Composable () -> Unit
) {
    val base = MaterialTheme.colorScheme

    val scheme = remember(playerColorScheme) {
        if (playerColorScheme.mode == PlayerColorScheme.Mode.AppTheme) {
            base
        } else {
            base.copy(
                surface = if (playerColorScheme.surface == Color.Transparent) base.surface else playerColorScheme.surface,
                primary = if (playerColorScheme.primary == Color.Transparent) base.primary else playerColorScheme.primary,
                onPrimary = if (playerColorScheme.onPrimary == Color.Transparent) base.onPrimary else playerColorScheme.onPrimary,
                onSurface = if (playerColorScheme.onSurface == Color.Transparent) base.onSurface else playerColorScheme.onSurface,
                onSurfaceVariant = if (playerColorScheme.onSurfaceVariant == Color.Transparent) base.onSurfaceVariant else playerColorScheme.onSurfaceVariant
            )
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = customTypography,
        content = content
    )
}
