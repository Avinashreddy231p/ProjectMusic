package com.mardous.booming.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.mardous.booming.R
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.UITheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.mardous.booming.core.model.theme.EraThemeEngine
import androidx.compose.ui.unit.sp
import com.mardous.booming.util.EraSurfaceMaterial

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
fun BoomingMusicTheme(
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

    val eraTypography = eraTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = eraTypography,
        shapes = eraShapes(),
        content = content
    )
}

@Composable
fun eraTypography(): Typography {
    val scale = Preferences.eraTypeScale
    
    fun scaleStyle(style: TextStyle) = style.copy(
        fontSize = (style.fontSize.value * scale).sp,
        lineHeight = (style.lineHeight.value * scale).sp
    )

    return Typography(
        displayLarge = scaleStyle(customTypography.displayLarge),
        displayMedium = scaleStyle(customTypography.displayMedium),
        displaySmall = scaleStyle(customTypography.displaySmall),
        headlineLarge = scaleStyle(customTypography.headlineLarge),
        headlineMedium = scaleStyle(customTypography.headlineMedium),
        headlineSmall = scaleStyle(customTypography.headlineSmall),
        titleLarge = scaleStyle(customTypography.titleLarge),
        titleMedium = scaleStyle(customTypography.titleMedium),
        titleSmall = scaleStyle(customTypography.titleSmall),
        bodyLarge = scaleStyle(customTypography.bodyLarge),
        bodyMedium = scaleStyle(customTypography.bodyMedium),
        bodySmall = scaleStyle(customTypography.bodySmall),
        labelLarge = scaleStyle(customTypography.labelLarge),
        labelMedium = scaleStyle(customTypography.labelMedium),
        labelSmall = scaleStyle(customTypography.labelSmall)
    )
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