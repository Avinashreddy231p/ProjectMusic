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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class AppIcons(
    val home: ImageVector,
    val songs: ImageVector,
    val albums: ImageVector,
    val artists: ImageVector,
    val playlists: ImageVector,
    val genres: ImageVector,
    val years: ImageVector,
    val folders: ImageVector,
    val play: ImageVector,
    val pause: ImageVector,
    val next: ImageVector,
    val previous: ImageVector,
    val shuffle: ImageVector,
    val repeat: ImageVector,
    val favorite: ImageVector,
    val favoriteBorder: ImageVector,
    val search: ImageVector,
    val settings: ImageVector,
    val more: ImageVector,
    val add: ImageVector,
    val delete: ImageVector,
    val edit: ImageVector,
    val share: ImageVector,
    val info: ImageVector,
    val back: ImageVector,
    val close: ImageVector,
    val check: ImageVector,
    val sort: ImageVector,
    val filter: ImageVector,
    val musicNote: ImageVector,
    val microphone: ImageVector,
    val history: ImageVector,
    val palette: ImageVector,
    val tune: ImageVector
)

object IconProvider {
    @Composable
    @ReadOnlyComposable
    fun getIcons(): AppIcons = getMaterialIcons(true)

    private fun getMaterialIcons(isFilled: Boolean): AppIcons {
        return AppIcons(
            home = if (isFilled) Icons.Filled.Home else Icons.Outlined.Home,
            songs = if (isFilled) Icons.Filled.MusicNote else Icons.Outlined.MusicNote,
            albums = if (isFilled) Icons.Filled.Album else Icons.Outlined.Album,
            artists = if (isFilled) Icons.Filled.Person else Icons.Outlined.Person,
            playlists = if (isFilled) Icons.AutoMirrored.Filled.PlaylistPlay else Icons.AutoMirrored.Outlined.PlaylistPlay,
            genres = if (isFilled) Icons.Filled.Radio else Icons.Outlined.Radio,
            years = if (isFilled) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
            folders = if (isFilled) Icons.Filled.Folder else Icons.Outlined.Folder,
            play = Icons.Filled.PlayArrow,
            pause = Icons.Filled.Pause,
            next = Icons.Filled.SkipNext,
            previous = Icons.Filled.SkipPrevious,
            shuffle = Icons.Filled.Shuffle,
            repeat = Icons.Filled.Repeat,
            favorite = Icons.Filled.Favorite,
            favoriteBorder = Icons.Filled.FavoriteBorder,
            search = Icons.Filled.Search,
            settings = if (isFilled) Icons.Filled.Settings else Icons.Outlined.Settings,
            more = Icons.Default.MoreVert,
            add = Icons.Default.Add,
            delete = if (isFilled) Icons.Filled.Delete else Icons.Outlined.Delete,
            edit = if (isFilled) Icons.Filled.Edit else Icons.Outlined.Edit,
            share = if (isFilled) Icons.Filled.Share else Icons.Outlined.Share,
            info = if (isFilled) Icons.Filled.Info else Icons.Outlined.Info,
            back = Icons.AutoMirrored.Filled.ArrowBack,
            close = Icons.Default.Close,
            check = Icons.Default.Check,
            sort = Icons.AutoMirrored.Filled.Sort,
            filter = Icons.Default.FilterList,
            musicNote = Icons.Default.MusicNote,
            microphone = if (isFilled) Icons.Filled.Mic else Icons.Outlined.Mic,
            history = Icons.Default.History,
            palette = if (isFilled) Icons.Filled.Palette else Icons.Outlined.Palette,
            tune = Icons.Default.Tune
        )
    }
}

val LocalAppIcons = staticCompositionLocalOf<AppIcons> {
    error("No AppIcons provided")
}
