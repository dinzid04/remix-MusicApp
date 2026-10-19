package com.example.data.local

import kotlinx.coroutines.flow.Flow

interface RecentTrackDao {
    fun observeRecentTracks(): Flow<List<RecentTrackEntity>>

    suspend fun upsertRecentTrack(track: RecentTrackEntity)

    suspend fun deleteRecentById(id: String)

    suspend fun clearAllRecents()

    fun observeFavoriteIds(): Flow<List<String>>

    fun observeFavoriteTracks(): Flow<List<FavoriteTrackEntity>>

    suspend fun insertFavorite(track: FavoriteTrackEntity)

    suspend fun deleteFavoriteById(id: String)

    // Library Playlists
    fun observeLibraryPlaylists(): Flow<List<LibraryPlaylistEntity>>

    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity>

    suspend fun upsertLibraryPlaylist(playlist: LibraryPlaylistEntity)

    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>)

    suspend fun updatePlaylistPin(playlistId: String, isPinned: Boolean)

    suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String)

    suspend fun updatePlaylistThumbnail(playlistId: String, thumbUrl: String)

    suspend fun deleteLibraryPlaylist(playlistId: String)

    suspend fun deleteAllTracksFromPlaylist(playlistId: String)

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    // Offline Downloaded Tracks
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    suspend fun getDownloadedTrackById(id: String): DownloadedTrackEntity?

    suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity)

    suspend fun deleteDownloadedTrackById(id: String)
}
