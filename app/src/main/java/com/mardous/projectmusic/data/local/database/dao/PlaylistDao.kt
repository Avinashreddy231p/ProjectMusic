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

package com.mardous.projectmusic.data.local.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mardous.projectmusic.data.local.database.core.SongEntity
import com.mardous.projectmusic.data.local.database.metadata.PlaylistEntity
import com.mardous.projectmusic.data.local.database.metadata.PlaylistWithSongs
import com.mardous.projectmusic.data.local.database.metadata.SongPlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE playlist_name = :name")
    fun playlist(name: String): List<PlaylistEntity>

    @Query("SELECT * FROM playlists")
    suspend fun playlists(): List<PlaylistEntity>

    @Insert
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET playlist_name = :name WHERE playlist_id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Transaction
    @Query("SELECT * FROM playlists")
    suspend fun playlistsWithSongs(): List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlist_id = :playlistId")
    fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlist_id = :playlistId")
    fun playlistWithSongs(playlistId: Long): PlaylistWithSongs?

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlist_name LIKE :playlistName")
    fun searchPlaylists(playlistName: String): List<PlaylistWithSongs>

    @Transaction
    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id 
        WHERE spr.playlist_id = :playlistId AND s.title LIKE :songName
    """)
    fun searchSongs(playlistId: Long, songName: String): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongsToPlaylist(relationships: List<SongPlaylistEntity>)

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id 
        WHERE spr.playlist_id = :playlistId AND s.media_store_id = :songId LIMIT 1
    """)
    suspend fun findSongInPlaylist(playlistId: Long, songId: Long): SongEntity?

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id 
        WHERE spr.playlist_id = :playlistId AND s.media_store_id IN(:songIds)
    """)
    suspend fun findSongsInPlaylist(playlistId: Long, songIds: List<Long>): List<SongEntity>

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id 
        WHERE spr.playlist_id = :playlistId ORDER BY spr.order_in_playlist ASC
    """)
    fun songsFromPlaylistObservable(playlistId: Long): LiveData<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id 
        WHERE spr.playlist_id = :playlistId ORDER BY spr.order_in_playlist ASC
    """)
    suspend fun songsFromPlaylist(playlistId: Long): List<SongEntity>

    @Transaction
    suspend fun removeSongsAndDeletePlaylists(playlistIds: List<Long>) {
        deleteAllSongsFromPlaylists(playlistIds)
        deletePlaylists(playlistIds)
    }

    @Query("DELETE FROM playlists WHERE playlist_id IN (:playlistIds)")
    suspend fun deletePlaylists(playlistIds: List<Long>)

    @Query("DELETE FROM song_playlist_relationship WHERE playlist_id = :playlistId AND song_id IN (SELECT song_key FROM songs WHERE media_store_id IN (:songIds))")
    suspend fun deleteSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    @Delete
    suspend fun deleteSongsFromPlaylists(relationships: List<SongPlaylistEntity>)

    @Query("DELETE FROM song_playlist_relationship WHERE playlist_id IN(:playlistIds)")
    suspend fun deleteAllSongsFromPlaylists(playlistIds: List<Long>)

    @Query("DELETE FROM song_playlist_relationship WHERE song_id IN (SELECT song_key FROM songs WHERE media_store_id IN (:songIds))")
    suspend fun deleteSongsFromAllPlaylists(songIds: List<Long>)

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id
        INNER JOIN playlists p ON spr.playlist_id = p.playlist_id
        WHERE p.playlist_name = :playlistName
    """)
    fun favoritesSongsFlow(playlistName: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN song_playlist_relationship spr ON s.song_key = spr.song_id
        WHERE spr.playlist_id = :playlistId
    """)
    fun favoritesSongs(playlistId: Long): List<SongEntity>

    @Query("SELECT EXISTS(SELECT * FROM playlists WHERE playlist_id = :playlistId)")
    fun checkPlaylistExists(playlistId: Long): LiveData<Boolean>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM song_playlist_relationship spr
            INNER JOIN songs s ON spr.song_id = s.song_key
            WHERE s.media_store_id = :songId AND spr.playlist_id = :playlistId
        )
    """)
    fun checkSongExistInPlaylist(playlistId: Long, songId: Long): Boolean
}
