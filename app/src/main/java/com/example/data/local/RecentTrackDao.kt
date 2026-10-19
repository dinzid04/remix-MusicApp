package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentTrackDao {
    @Query("SELECT * FROM recent_tracks ORDER BY playedAt DESC LIMIT 50")
    fun observeRecentTracks(): Flow<List<RecentTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentTrack(track: RecentTrackEntity)

    @Query("DELETE FROM recent_tracks WHERE id = :id")
    suspend fun deleteRecentById(id: String)

    @Query("DELETE FROM recent_tracks")
    suspend fun clearAllRecents()

    @Query("SELECT id FROM favorite_tracks")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT * FROM favorite_tracks ORDER BY likedAt DESC")
    fun observeFavoriteTracks(): Flow<List<FavoriteTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    // Library Playlists
    @Query("SELECT * FROM library_playlists ORDER BY isPinned DESC, createdAt DESC")
    fun observeLibraryPlaylists(): Flow<List<LibraryPlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY addedAt ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryPlaylist(playlist: LibraryPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    @Query("UPDATE library_playlists SET isPinned = :isPinned WHERE id = :playlistId")
    suspend fun updatePlaylistPin(playlistId: String, isPinned: Boolean)

    @Query("UPDATE library_playlists SET title = :newTitle WHERE id = :playlistId")
    suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String)

    @Query("UPDATE library_playlists SET thumbnailUrl = :thumbUrl WHERE id = :playlistId")
    suspend fun updatePlaylistThumbnail(playlistId: String, thumbUrl: String)

    @Query("DELETE FROM library_playlists WHERE id = :playlistId")
    suspend fun deleteLibraryPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteAllTracksFromPlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    // Offline Downloaded Tracks
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id OR videoId = :id LIMIT 1")
    suspend fun getDownloadedTrackById(id: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id OR videoId = :id")
    suspend fun deleteDownloadedTrackById(id: String)
}

