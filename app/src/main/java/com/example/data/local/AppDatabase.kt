package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "bitchord_music.db", null, 3) {

    private val daoImpl = SqliteRecentTrackDao(this)

    fun recentTrackDao(): RecentTrackDao = daoImpl

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createTables(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                browseId TEXT,
                type TEXT NOT NULL,
                duration TEXT,
                playedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                browseId TEXT,
                duration TEXT,
                likedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS library_playlists (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                privacy TEXT NOT NULL,
                isPinned INTEGER NOT NULL DEFAULT 0,
                sourceType TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                playlistId TEXT NOT NULL,
                trackId TEXT NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                videoId TEXT,
                duration TEXT,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY (playlistId, trackId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS downloaded_tracks (
                id TEXT PRIMARY KEY NOT NULL,
                videoId TEXT,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                duration TEXT,
                localFilePath TEXT NOT NULL,
                mediaStoreUri TEXT,
                fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                downloadedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

private class SqliteRecentTrackDao(
    private val helper: SQLiteOpenHelper
) : RecentTrackDao {

    private val lock = Any()

    private val recentTracksState = MutableStateFlow<List<RecentTrackEntity>>(emptyList())
    private val favoriteTracksState = MutableStateFlow<List<FavoriteTrackEntity>>(emptyList())
    private val libraryPlaylistsState = MutableStateFlow<List<LibraryPlaylistEntity>>(emptyList())
    private val playlistTracksState = MutableStateFlow<List<PlaylistTrackEntity>>(emptyList())
    private val downloadedTracksState = MutableStateFlow<List<DownloadedTrackEntity>>(emptyList())

    init {
        refreshAllStatesSync()
    }

    private fun refreshAllStatesSync() {
        synchronized(lock) {
            runCatching {
                val db = helper.writableDatabase
                recentTracksState.value = queryRecentTracks(db)
                favoriteTracksState.value = queryFavoriteTracks(db)
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
                playlistTracksState.value = queryAllPlaylistTracks(db)
                downloadedTracksState.value = queryDownloadedTracks(db)
            }
        }
    }

    override fun observeRecentTracks(): Flow<List<RecentTrackEntity>> =
        recentTracksState.asStateFlow()

    override suspend fun upsertRecentTrack(track: RecentTrackEntity) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", track.id)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("videoId", track.videoId)
                put("browseId", track.browseId)
                put("type", track.type)
                put("duration", track.duration)
                put("playedAt", track.playedAt)
            }
            db.insertWithOnConflict("recent_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            recentTracksState.value = queryRecentTracks(db)
        }
    }

    override suspend fun deleteRecentById(id: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val db = helper.writableDatabase
            db.delete("recent_tracks", "id = ?", arrayOf(id))
            recentTracksState.value = queryRecentTracks(db)
        }
    }

    override suspend fun clearAllRecents() = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val db = helper.writableDatabase
            db.delete("recent_tracks", null, null)
            recentTracksState.value = emptyList()
        }
    }

    override fun observeFavoriteIds(): Flow<List<String>> =
        favoriteTracksState.map { list -> list.map { it.id } }

    override fun observeFavoriteTracks(): Flow<List<FavoriteTrackEntity>> =
        favoriteTracksState.asStateFlow()

    override suspend fun insertFavorite(track: FavoriteTrackEntity) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val db = helper.writableDatabase
            val cv = ContentValues().apply {
                put("id", track.id)
                put("title", track.title)
                put("subtitle", track.subtitle)
                put("thumbnailUrl", track.thumbnailUrl)
                put("videoId", track.videoId)
                put("browseId", track.browseId)
                put("duration", track.duration)
                put("likedAt", track.likedAt)
            }
            db.insertWithOnConflict("favorite_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            favoriteTracksState.value = queryFavoriteTracks(db)
        }
    }

    override suspend fun deleteFavoriteById(id: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val db = helper.writableDatabase
            db.delete("favorite_tracks", "id = ?", arrayOf(id))
            favoriteTracksState.value = queryFavoriteTracks(db)
        }
    }

    override fun observeLibraryPlaylists(): Flow<List<LibraryPlaylistEntity>> =
        libraryPlaylistsState.asStateFlow()

    override fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>> =
        playlistTracksState.asStateFlow()

    override suspend fun getTracksForPlaylist(playlistId: String): List<PlaylistTrackEntity> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                playlistTracksState.value.filter { it.playlistId == playlistId }
            }
        }

    override suspend fun upsertLibraryPlaylist(playlist: LibraryPlaylistEntity) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("id", playlist.id)
                    put("title", playlist.title)
                    put("subtitle", playlist.subtitle)
                    put("thumbnailUrl", playlist.thumbnailUrl)
                    put("privacy", playlist.privacy)
                    put("isPinned", if (playlist.isPinned) 1 else 0)
                    put("sourceType", playlist.sourceType)
                    put("createdAt", playlist.createdAt)
                }
                db.insertWithOnConflict("library_playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun insertPlaylistTrack(track: PlaylistTrackEntity) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("playlistId", track.playlistId)
                    put("trackId", track.trackId)
                    put("title", track.title)
                    put("subtitle", track.subtitle)
                    put("thumbnailUrl", track.thumbnailUrl)
                    put("videoId", track.videoId)
                    put("duration", track.duration)
                    put("addedAt", track.addedAt)
                }
                db.insertWithOnConflict("playlist_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override suspend fun insertPlaylistTracks(tracks: List<PlaylistTrackEntity>) =
        withContext(Dispatchers.IO) {
            if (tracks.isEmpty()) return@withContext
            synchronized(lock) {
                val db = helper.writableDatabase
                db.beginTransaction()
                try {
                    for (track in tracks) {
                        val cv = ContentValues().apply {
                            put("playlistId", track.playlistId)
                            put("trackId", track.trackId)
                            put("title", track.title)
                            put("subtitle", track.subtitle)
                            put("thumbnailUrl", track.thumbnailUrl)
                            put("videoId", track.videoId)
                            put("duration", track.duration)
                            put("addedAt", track.addedAt)
                        }
                        db.insertWithOnConflict(
                            "playlist_tracks",
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_REPLACE
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override suspend fun updatePlaylistPin(playlistId: String, isPinned: Boolean) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("isPinned", if (isPinned) 1 else 0)
                }
                db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun renameLibraryPlaylist(playlistId: String, newTitle: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("title", newTitle)
                }
                db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun updatePlaylistThumbnail(playlistId: String, thumbUrl: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("thumbnailUrl", thumbUrl)
                }
                db.update("library_playlists", cv, "id = ?", arrayOf(playlistId))
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun deleteLibraryPlaylist(playlistId: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                db.delete("library_playlists", "id = ?", arrayOf(playlistId))
                libraryPlaylistsState.value = queryLibraryPlaylists(db)
            }
        }

    override suspend fun deleteAllTracksFromPlaylist(playlistId: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                db.delete("playlist_tracks", "playlistId = ?", arrayOf(playlistId))
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                db.delete(
                    "playlist_tracks",
                    "playlistId = ? AND trackId = ?",
                    arrayOf(playlistId, trackId)
                )
                playlistTracksState.value = queryAllPlaylistTracks(db)
            }
        }

    override fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>> =
        downloadedTracksState.asStateFlow()

    override suspend fun getDownloadedTrackById(id: String): DownloadedTrackEntity? =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                downloadedTracksState.value.firstOrNull { it.id == id || it.videoId == id }
            }
        }

    override suspend fun upsertDownloadedTrack(track: DownloadedTrackEntity) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                val cv = ContentValues().apply {
                    put("id", track.id)
                    put("videoId", track.videoId)
                    put("title", track.title)
                    put("subtitle", track.subtitle)
                    put("thumbnailUrl", track.thumbnailUrl)
                    put("duration", track.duration)
                    put("localFilePath", track.localFilePath)
                    put("mediaStoreUri", track.mediaStoreUri)
                    put("fileSizeBytes", track.fileSizeBytes)
                    put("downloadedAt", track.downloadedAt)
                }
                db.insertWithOnConflict("downloaded_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                downloadedTracksState.value = queryDownloadedTracks(db)
            }
        }

    override suspend fun deleteDownloadedTrackById(id: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val db = helper.writableDatabase
                db.delete("downloaded_tracks", "id = ? OR videoId = ?", arrayOf(id, id))
                downloadedTracksState.value = queryDownloadedTracks(db)
            }
        }

    private fun queryRecentTracks(db: SQLiteDatabase): List<RecentTrackEntity> {
        val list = mutableListOf<RecentTrackEntity>()
        db.rawQuery("SELECT id, title, subtitle, thumbnailUrl, videoId, browseId, type, duration, playedAt FROM recent_tracks ORDER BY playedAt DESC LIMIT 50", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    RecentTrackEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        videoId = if (c.isNull(4)) null else c.getString(4),
                        browseId = if (c.isNull(5)) null else c.getString(5),
                        type = c.getString(6),
                        duration = if (c.isNull(7)) null else c.getString(7),
                        playedAt = c.getLong(8)
                    )
                )
            }
        }
        return list
    }

    private fun queryFavoriteTracks(db: SQLiteDatabase): List<FavoriteTrackEntity> {
        val list = mutableListOf<FavoriteTrackEntity>()
        db.rawQuery("SELECT id, title, subtitle, thumbnailUrl, videoId, browseId, duration, likedAt FROM favorite_tracks ORDER BY likedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    FavoriteTrackEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        videoId = if (c.isNull(4)) null else c.getString(4),
                        browseId = if (c.isNull(5)) null else c.getString(5),
                        duration = if (c.isNull(6)) null else c.getString(6),
                        likedAt = c.getLong(7)
                    )
                )
            }
        }
        return list
    }

    private fun queryLibraryPlaylists(db: SQLiteDatabase): List<LibraryPlaylistEntity> {
        val list = mutableListOf<LibraryPlaylistEntity>()
        db.rawQuery("SELECT id, title, subtitle, thumbnailUrl, privacy, isPinned, sourceType, createdAt FROM library_playlists ORDER BY isPinned DESC, createdAt DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LibraryPlaylistEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        subtitle = c.getString(2),
                        thumbnailUrl = c.getString(3),
                        privacy = c.getString(4),
                        isPinned = c.getInt(5) != 0,
                        sourceType = c.getString(6),
                        createdAt = c.getLong(7)
                    )
                )
            }
        }
        return list
    }

    private fun queryAllPlaylistTracks(db: SQLiteDatabase): List<PlaylistTrackEntity> {
        val list = mutableListOf<PlaylistTrackEntity>()
        db.rawQuery("SELECT playlistId, trackId, title, subtitle, thumbnailUrl, videoId, duration, addedAt FROM playlist_tracks ORDER BY addedAt ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PlaylistTrackEntity(
                        playlistId = c.getString(0),
                        trackId = c.getString(1),
                        title = c.getString(2),
                        subtitle = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        videoId = if (c.isNull(5)) null else c.getString(5),
                        duration = if (c.isNull(6)) null else c.getString(6),
                        addedAt = c.getLong(7)
                    )
                )
            }
        }
        return list
    }

    private fun queryDownloadedTracks(db: SQLiteDatabase): List<DownloadedTrackEntity> {
        val list = mutableListOf<DownloadedTrackEntity>()
        db.rawQuery("SELECT id, videoId, title, subtitle, thumbnailUrl, duration, localFilePath, mediaStoreUri, fileSizeBytes, downloadedAt FROM downloaded_tracks ORDER BY downloadedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    DownloadedTrackEntity(
                        id = c.getString(0),
                        videoId = if (c.isNull(1)) null else c.getString(1),
                        title = c.getString(2),
                        subtitle = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        duration = if (c.isNull(5)) null else c.getString(5),
                        localFilePath = c.getString(6),
                        mediaStoreUri = if (c.isNull(7)) null else c.getString(7),
                        fileSizeBytes = c.getLong(8),
                        downloadedAt = c.getLong(9)
                    )
                )
            }
        }
        return list
    }
}
