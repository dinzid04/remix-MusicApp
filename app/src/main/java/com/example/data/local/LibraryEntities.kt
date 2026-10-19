package com.example.data.local

import com.example.data.model.DownloadedTrackItem
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.PlaylistSourceType
import com.example.data.model.UserLibraryPlaylist

data class LibraryPlaylistEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val privacy: String = PlaylistPrivacy.PRIVATE.name,
    val isPinned: Boolean = false,
    val sourceType: String = PlaylistSourceType.CUSTOM.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(tracks: List<MusicItem>): UserLibraryPlaylist {
        val parsedPrivacy = try {
            PlaylistPrivacy.valueOf(privacy)
        } catch (_: Exception) {
            PlaylistPrivacy.PRIVATE
        }
        val parsedSource = try {
            PlaylistSourceType.valueOf(sourceType)
        } catch (_: Exception) {
            PlaylistSourceType.CUSTOM
        }
        return UserLibraryPlaylist(
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            privacy = parsedPrivacy,
            isPinned = isPinned,
            sourceType = parsedSource,
            createdAt = createdAt,
            tracks = tracks
        )
    }
}

data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String?,
    val duration: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = trackId,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId ?: trackId,
        type = "song",
        duration = duration
    )

    companion object {
        fun fromMusicItem(playlistId: String, item: MusicItem, orderOffset: Long = 0L): PlaylistTrackEntity {
            val effectiveId = item.videoId ?: item.id
            return PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = effectiveId,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.highResThumbnail,
                videoId = item.videoId ?: item.id,
                duration = item.duration,
                addedAt = System.currentTimeMillis() + orderOffset
            )
        }
    }
}

data class DownloadedTrackEntity(
    val id: String,
    val videoId: String?,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val duration: String?,
    val localFilePath: String,
    val mediaStoreUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DownloadedTrackItem = DownloadedTrackItem(
        id = id,
        videoId = videoId,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        localFilePath = localFilePath,
        mediaStoreUri = mediaStoreUri,
        fileSizeBytes = fileSizeBytes,
        downloadedAt = downloadedAt
    )
}
