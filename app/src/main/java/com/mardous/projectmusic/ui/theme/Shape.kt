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

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.mardous.projectmusic.core.model.theme.EraShapeFamily
import com.mardous.projectmusic.util.Preferences

fun eraShapes(): Shapes {
    val scale = Preferences.eraShapeScale
    val isRounded = Preferences.eraShapeFamily == EraShapeFamily.ROUNDED
    val isAsymmetric = Preferences.eraAsymmetricShapes

    fun createShape(baseSize: Float) = if (isRounded) {
        if (isAsymmetric) {
            RoundedCornerShape(
                topStart = (baseSize * scale * 2).dp,
                topEnd = (baseSize * scale).dp,
                bottomEnd = (baseSize * scale * 2).dp,
                bottomStart = (baseSize * scale).dp
            )
        } else {
            RoundedCornerShape((baseSize * scale).dp)
        }
    } else {
        if (isAsymmetric) {
            CutCornerShape(
                topStart = (baseSize * scale * 1.5f).dp,
                topEnd = (baseSize * scale).dp,
                bottomEnd = (baseSize * scale * 1.5f).dp,
                bottomStart = (baseSize * scale).dp
            )
        } else {
            CutCornerShape((baseSize * scale).dp)
        }
    }

    return Shapes(
        extraSmall = createShape(4f),
        small = createShape(8f),
        medium = createShape(12f),
        large = createShape(16f),
        extraLarge = createShape(28f)
    )
}
