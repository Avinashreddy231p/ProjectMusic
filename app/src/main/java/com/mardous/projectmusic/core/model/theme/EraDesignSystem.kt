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

enum class EraFont(val label: String) {
    GOOGLE_SANS("Google Sans"),
    INTER("Inter"),
    MONTSERRAT("Montserrat"),
    OUTFIT("Outfit"),
    MANROPE("Manrope")
}

enum class EraShapeFamily {
    ROUNDED, CUT
}

enum class EraSurfaceMaterial {
    SOLID, GLASS, VIBRANT
}
