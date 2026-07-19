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

package com.mardous.projectmusic.data.local.room

enum class MetadataSource(val value: Int) {
    Manual(0),
    AI(1),
    Embedded(2),
    Imported(3),
    Lastfm(4),
    MusicBrainz(5)
}

enum class TagCategory(val value: Int) {
    Activity(0),
    Theme(1),
    Era(2),
    Production(3),
    Language(4),
    Occasion(5),
    Weather(6),
    Vocal(7)
}

enum class InstrumentFamily(val value: Int) {
    Strings(0),
    Percussion(1),
    Brass(2),
    Woodwind(3),
    Keyboard(4),
    Electronic(5)
}
