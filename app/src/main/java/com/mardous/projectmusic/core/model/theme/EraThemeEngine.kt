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

package com.mardous.projectmusic.core.model.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeExpressive
import com.kyant.m3color.scheme.SchemeTonalSpot
import com.mardous.projectmusic.extensions.resources.ensureContrastAgainst
import com.mardous.projectmusic.util.Preferences

object EraThemeEngine {

    fun generateColorScheme(isDark: Boolean): ColorScheme {
        val contrast = Preferences.eraContrast.toDouble()
        
        return if (Preferences.eraHarmonyMode) {
            // Harmonized: Use Primary seed to generate everything
            val primaryHct = Hct.fromInt(Preferences.eraPrimarySeed)
            val scheme = SchemeExpressive(primaryHct, isDark, contrast)
            createColorSchemeFromScheme(scheme)
        } else {
            // Absolute Freedom: Pick roles from different seeds
            val primaryHct = Hct.fromInt(Preferences.eraPrimarySeed)
            val secondaryHct = Hct.fromInt(Preferences.eraSecondarySeed)
            val tertiaryHct = Hct.fromInt(Preferences.eraTertiarySeed)
            val errorHct = Hct.fromInt(Preferences.eraErrorSeed)

            val pScheme = SchemeTonalSpot(primaryHct, isDark, contrast)
            val sScheme = SchemeTonalSpot(secondaryHct, isDark, contrast)
            val tScheme = SchemeTonalSpot(tertiaryHct, isDark, contrast)
            val eScheme = SchemeTonalSpot(errorHct, isDark, contrast)

            // Construct a hybrid scheme
            createColorSchemeFromScheme(pScheme).copy(
                primary = pScheme.primary.toColor(),
                onPrimary = pScheme.onPrimary.toColor(),
                primaryContainer = pScheme.primaryContainer.toColor(),
                onPrimaryContainer = pScheme.onPrimaryContainer.toColor(),
                inversePrimary = pScheme.inversePrimary.toColor(),
                
                secondary = sScheme.primary.toColor(), // Use 'primary' of secondary seed as secondary
                onSecondary = sScheme.onPrimary.toColor(),
                secondaryContainer = sScheme.primaryContainer.toColor(),
                onSecondaryContainer = sScheme.onPrimaryContainer.toColor(),
                
                tertiary = tScheme.primary.toColor(),
                onTertiary = tScheme.onPrimary.toColor(),
                tertiaryContainer = tScheme.primaryContainer.toColor(),
                onTertiaryContainer = tScheme.onPrimaryContainer.toColor(),
                
                error = eScheme.primary.toColor(),
                onError = eScheme.onPrimary.toColor(),
                errorContainer = eScheme.errorContainer.toColor(),
                onErrorContainer = eScheme.onErrorContainer.toColor()
            )
        }
    }

    private fun createColorSchemeFromScheme(scheme: com.kyant.m3color.dynamiccolor.DynamicScheme): ColorScheme {
        val surface = scheme.surface.toColor().run {
            if (this == Color.Transparent) Color.Black else this
        }
        
        return ColorScheme(
            primary = scheme.primary.toColor().ensureContrastAgainst(surface),
            onPrimary = scheme.onPrimary.toColor(),
            primaryContainer = scheme.primaryContainer.toColor(),
            onPrimaryContainer = scheme.onPrimaryContainer.toColor(),
            inversePrimary = scheme.inversePrimary.toColor(),
            secondary = scheme.secondary.toColor().ensureContrastAgainst(surface),
            onSecondary = scheme.onSecondary.toColor(),
            secondaryContainer = scheme.secondaryContainer.toColor(),
            onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
            tertiary = scheme.tertiary.toColor().ensureContrastAgainst(surface),
            onTertiary = scheme.onTertiary.toColor(),
            tertiaryContainer = scheme.tertiaryContainer.toColor(),
            onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
            background = scheme.background.toColor(),
            onBackground = scheme.onBackground.toColor(),
            surface = surface,
            onSurface = scheme.onSurface.toColor(),
            surfaceVariant = scheme.surfaceVariant.toColor(),
            onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
            surfaceTint = scheme.surfaceTint.toColor(),
            inverseSurface = scheme.inverseSurface.toColor(),
            inverseOnSurface = scheme.inverseOnSurface.toColor(),
            error = scheme.error.toColor().ensureContrastAgainst(surface),
            onError = scheme.onError.toColor(),
            errorContainer = scheme.errorContainer.toColor(),
            onErrorContainer = scheme.onErrorContainer.toColor(),
            outline = scheme.outline.toColor(),
            outlineVariant = scheme.outlineVariant.toColor(),
            scrim = scheme.scrim.toColor(),
            surfaceBright = scheme.surfaceBright.toColor(),
            surfaceDim = scheme.surfaceDim.toColor(),
            surfaceContainer = scheme.surfaceContainer.toColor(),
            surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
            surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
            surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
            surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
            primaryFixed = scheme.primaryFixed.toColor(),
            primaryFixedDim = scheme.primaryFixedDim.toColor(),
            onPrimaryFixed = scheme.onPrimaryFixed.toColor(),
            onPrimaryFixedVariant = scheme.onPrimaryFixedVariant.toColor(),
            secondaryFixed = scheme.secondaryFixed.toColor(),
            secondaryFixedDim = scheme.secondaryFixedDim.toColor(),
            onSecondaryFixed = scheme.onSecondaryFixed.toColor(),
            onSecondaryFixedVariant = scheme.onSecondaryFixedVariant.toColor(),
            tertiaryFixed = scheme.tertiaryFixed.toColor(),
            tertiaryFixedDim = scheme.tertiaryFixedDim.toColor(),
            onTertiaryFixed = scheme.onTertiaryFixed.toColor(),
            onTertiaryFixedVariant = scheme.onTertiaryFixedVariant.toColor()
        )
    }

    private fun Int.toColor(): Color = Color(this)
}
