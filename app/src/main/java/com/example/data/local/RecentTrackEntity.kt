package com.example.data.local

import com.example.data.model.MusicItem

data class RecentTrackEntity(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val videoId: String?,
    val browseId: String?,
    val type: String,
    val duration: String?,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toMusicItem(): MusicItem = MusicItem(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
        type = type,
        duration = duration
    )

    companion object {
        fun fromMusicItem(item: MusicItem): RecentTrackEntity {
            val effectiveId = item.videoId ?: item.browseId ?: item.id
            return RecentTrackEntity(
                id = effectiveId,
                title = item.title,
                subtitle = item.subtitle,
                thumbnailUrl = item.highResThumbnail,
                videoId = item.videoId,
                browseId = item.browseId,
                type = item.type,
                duration = item.duration,
                playedAt = System.currentTimeMillis()
            )
        }
    }
}
