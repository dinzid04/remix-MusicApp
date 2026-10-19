package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistProfileDetail
import com.example.data.model.ArtistReleaseTab
import com.example.data.model.MusicItem
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.theme.rememberDominantAccentColor

@Composable
fun ArtistProfileScreen(
    artistProfile: ArtistProfileDetail?,
    isLoading: Boolean,
    isFollowing: Boolean,
    currentPlayingTrackId: String?,
    favoriteIds: Set<String>,
    hasActiveMiniPlayer: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (String) -> Unit,
    onPlayTrack: (MusicItem, List<MusicItem>, String) -> Unit,
    onOpenReleaseOrPlaylist: (MusicItem) -> Unit,
    onPlayReleaseDirect: (MusicItem) -> Unit,
    onOpenRelatedArtist: (MusicItem) -> Unit,
    onToggleFavoriteTrack: (MusicItem) -> Unit,
    onOpenTrackMenu: (MusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedReleaseTab by remember { mutableStateOf(ArtistReleaseTab.ALL) }
    var showAllTopSongs by remember { mutableStateOf(false) }

    if (artistProfile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OledBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE94057))
        }
        return
    }

    val extractedAccent = rememberDominantAccentColor(
        imageUrl = artistProfile.avatarUrl.ifBlank { artistProfile.headerImageUrl },
        defaultColor = Color(0xFFE94057)
    )
    val dynamicAccent by animateColorAsState(
        targetValue = extractedAccent,
        animationSpec = tween(durationMillis = 550),
        label = "artist_dynamic_accent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("artist_profile_lazy_column"),
            contentPadding = PaddingValues(
                bottom = if (hasActiveMiniPlayer) 176.dp else 112.dp
            )
        ) {
            // 1. Immersive Dynamic-Color Hero Header
            item(key = "artist_hero_header") {
                ArtistHeroHeaderSection(
                    artist = artistProfile,
                    dynamicAccent = dynamicAccent,
                    isFollowing = isFollowing,
                    isLoading = isLoading,
                    onBack = onBack,
                    onToggleFollow = { onToggleFollow(artistProfile.browseId) },
                    onPlayAllTop = {
                        val first = artistProfile.topSongs.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                        }
                    },
                    onShuffleTop = {
                        val shuffled = artistProfile.topSongs.shuffled()
                        val first = shuffled.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, shuffled, "Shuffle • ${artistProfile.name}")
                        }
                    },
                    onStartRadio = {
                        val radioSongs = artistProfile.topSongs.shuffled()
                        val first = radioSongs.firstOrNull()
                        if (first != null) {
                            onPlayTrack(first, radioSongs, "Radio • ${artistProfile.name}")
                        }
                    }
                )
            }

            // 2. Highlight Rilis Terbaru (Featured Latest Release Spotlight with Vinyl Accent)
            artistProfile.latestRelease?.let { latest ->
                item(key = "artist_latest_release_spotlight") {
                    LatestReleaseSpotlightCard(
                        release = latest,
                        artistName = artistProfile.name,
                        accentColor = dynamicAccent,
                        onOpenDetail = { onOpenReleaseOrPlaylist(latest) },
                        onPlayNow = { onPlayReleaseDirect(latest) }
                    )
                }
            }

            // 3. Lagu Populer (Top Songs)
            if (artistProfile.topSongs.isNotEmpty()) {
                item(key = "artist_top_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lagu",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimaryWhite
                            )
                            Text(
                                text = "Karya terpopuler dari ${artistProfile.name}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = TextSecondaryGrey
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = dynamicAccent.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .border(1.dp, dynamicAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        val first = artistProfile.topSongs.firstOrNull()
                                        if (first != null) {
                                            onPlayTrack(first, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Putar semua",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                val visibleSongs = if (showAllTopSongs) {
                    artistProfile.topSongs
                } else {
                    artistProfile.topSongs.take(5)
                }

                itemsIndexed(
                    items = visibleSongs,
                    key = { idx, song -> "top_song_${song.id}_$idx" }
                ) { index, song ->
                    val isPlayingThis = currentPlayingTrackId == song.id ||
                        (song.videoId != null && currentPlayingTrackId == song.videoId)
                    val isFav = favoriteIds.contains(song.id) ||
                        (song.videoId != null && favoriteIds.contains(song.videoId))

                    ArtistTopSongRowItem(
                        rank = index + 1,
                        song = song,
                        isPlaying = isPlayingThis,
                        isFavorite = isFav,
                        accentColor = dynamicAccent,
                        onClick = {
                            onPlayTrack(song, artistProfile.topSongs, "Playing from ${artistProfile.name}")
                        },
                        onToggleFavorite = { onToggleFavoriteTrack(song) },
                        onMoreClick = { onOpenTrackMenu(song) }
                    )
                }

                if (artistProfile.topSongs.size > 5) {
                    item(key = "artist_top_songs_expand") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF1A1A20),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showAllTopSongs = !showAllTopSongs }
                                    .testTag("artist_toggle_all_songs_button")
                            ) {
                                Text(
                                    text = if (showAllTopSongs) "Tampilkan lebih sedikit" else "Tampilkan semua (${artistProfile.topSongs.size} lagu)",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Variasi Diskografi (Album & Single/EP dengan Filter Tab & Varied Cards)
            if (artistProfile.albums.isNotEmpty() || artistProfile.singlesAndEps.isNotEmpty()) {
                item(key = "artist_discography_section") {
                    ArtistDiscographySection(
                        albums = artistProfile.albums,
                        singlesAndEps = artistProfile.singlesAndEps,
                        selectedTab = selectedReleaseTab,
                        accentColor = dynamicAccent,
                        onSelectTab = { selectedReleaseTab = it },
                        onOpenRelease = onOpenReleaseOrPlaylist,
                        onPlayRelease = onPlayReleaseDirect
                    )
                }
            }

            // 5. Video Musik (16:9 Widescreen Cards)
            if (artistProfile.videos.isNotEmpty()) {
                item(key = "artist_videos_section") {
                    ArtistVideosSection(
                        videos = artistProfile.videos,
                        accentColor = dynamicAccent,
                        onVideoClick = { video ->
                            onPlayTrack(video, artistProfile.videos, "Video • ${artistProfile.name}")
                        }
                    )
                }
            }

            // 6. Menampilkan Artis / Playlist Pilihan (Featured Playlists)
            if (artistProfile.featuredPlaylists.isNotEmpty()) {
                item(key = "artist_featured_playlists_section") {
                    ArtistFeaturedPlaylistsSection(
                        artistName = artistProfile.name,
                        playlists = artistProfile.featuredPlaylists,
                        accentColor = dynamicAccent,
                        onPlaylistClick = onOpenReleaseOrPlaylist,
                        onPlayPlaylist = onPlayReleaseDirect
                    )
                }
            }

            // 7. "Penggemar mungkin juga suka" — Strictly Circular Artist Avatars!
            if (artistProfile.relatedArtists.isNotEmpty()) {
                item(key = "artist_fans_also_like_section") {
                    FansMightAlsoLikeCircularSection(
                        relatedArtists = artistProfile.relatedArtists,
                        accentColor = dynamicAccent,
                        onArtistClick = onOpenRelatedArtist
                    )
                }
            }

            // 8. Tentang Artis (About / Bio Card)
            if (!artistProfile.description.isNullOrBlank()) {
                item(key = "artist_about_card") {
                    ArtistAboutBioSection(
                        artist = artistProfile,
                        accentColor = dynamicAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistHeroHeaderSection(
    artist: ArtistProfileDetail,
    dynamicAccent: Color,
    isFollowing: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onPlayAllTop: () -> Unit,
    onShuffleTop: () -> Unit,
    onStartRadio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OledBlack)
    ) {
        // Full-bleed artist hero image with dynamic color atmospheric gradient
        AsyncImage(
            model = artist.headerImageUrl.ifBlank { artist.avatarUrl },
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
        )

        // Multi-stop dynamic color & OLED black gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to OledBlack.copy(alpha = 0.45f),
                        0.38f to dynamicAccent.copy(alpha = 0.36f),
                        0.76f to dynamicAccent.copy(alpha = 0.62f),
                        1.0f to OledBlack
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar: Back button + Verified Artist pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OledBlack.copy(alpha = 0.62f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("artist_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimaryWhite
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = TextPrimaryWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OledBlack.copy(alpha = 0.62f),
                        modifier = Modifier.border(1.dp, dynamicAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Artis Resmi",
                                tint = dynamicAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ARTIS RESMI",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.6.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))

            // Artist avatar + Name + Subscribers & Monthly Listeners
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = artist.avatarUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .border(2.5.dp, dynamicAccent, CircleShape)
                        .background(CardPlaceholderDark)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.8).sp,
                        color = TextPrimaryWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${artist.subscribersText} • ${artist.monthlyListenersText}",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Controls Bar: Subscribe/Follow + Radio + Shuffle + Primary Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subscribe / Ikuti Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isFollowing) dynamicAccent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .border(
                                width = 1.2.dp,
                                color = if (isFollowing) dynamicAccent else Color.White.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(50)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleFollow)
                            .testTag("artist_follow_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFollowing) "Disubscribe" else "Subscribe",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }

                    // Radio Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onStartRadio)
                            .testTag("artist_radio_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radio",
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Radio",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimaryWhite
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShuffleTop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .testTag("artist_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Acak",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayAllTop,
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(TextPrimaryWhite)
                            .testTag("artist_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar",
                            tint = OledBlack,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestReleaseSpotlightCard(
    release: MusicItem,
    artistName: String,
    accentColor: Color,
    onOpenDetail: () -> Unit,
    onPlayNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("artist_latest_release_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.24f),
                            SurfaceDark,
                            Color(0xFF121216)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Sleeve + Vinyl Disc Peek
            Box(
                modifier = Modifier
                    .width(106.dp)
                    .height(88.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Vinyl Disc peeking on the right
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .offset(x = 26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E12))
                        .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }

                AsyncImage(
                    model = release.highResThumbnail,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .shadow(10.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardPlaceholderDark)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "RILIS TERBARU",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp,
                        color = TextPrimaryWhite,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = release.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = TextPrimaryWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = release.subtitle.ifBlank { "Rilisan Unggulan • $artistName" },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = onPlayNow,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .testTag("latest_release_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar Rilis Terbaru",
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistTopSongRowItem(
    rank: Int,
    song: MusicItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("artist_top_song_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = if (isPlaying) accentColor else TextSecondaryGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(26.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        AsyncImage(
            model = song.smallThumbnail,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardPlaceholderDark)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                color = if (isPlaying) accentColor else TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val metaText = buildString {
                append(song.cleanArtist)
                song.playsText?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                    ?: song.duration?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            }
            Text(
                text = metaText,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorit",
                tint = if (isFavorite) Color(0xFFFF4D67) else TextSecondaryGrey,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi Lagu",
                tint = TextSecondaryGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ArtistDiscographySection(
    albums: List<MusicItem>,
    singlesAndEps: List<MusicItem>,
    selectedTab: ArtistReleaseTab,
    accentColor: Color,
    onSelectTab: (ArtistReleaseTab) -> Unit,
    onOpenRelease: (MusicItem) -> Unit,
    onPlayRelease: (MusicItem) -> Unit
) {
    val displayedReleases = remember(albums, singlesAndEps, selectedTab) {
        when (selectedTab) {
            ArtistReleaseTab.ALL -> (albums + singlesAndEps).distinctBy { it.title.lowercase() }
            ArtistReleaseTab.ALBUMS -> albums
            ArtistReleaseTab.SINGLES -> singlesAndEps
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Album & Single",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Diskografi resmi dengan variasi tata letak rilisan",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Tabs: Semua, Album, Single & EP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArtistReleaseTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) accentColor else Color(0xFF1A1A20),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color(0xFF2E2E38),
                            shape = RoundedCornerShape(50)
                        )
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectTab(tab) }
                        .testTag("artist_release_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = TextPrimaryWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Varied Release Cards Carousel (Wide Vinyl Sleeve Card + Editorial Discography Cards)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = displayedReleases,
                key = { idx, item -> "discog_${item.id}_$idx" }
            ) { index, release ->
                if (index == 0) {
                    FeaturedWideDiscographyCard(
                        release = release,
                        accentColor = accentColor,
                        onClick = { onOpenRelease(release) },
                        onPlayClick = { onPlayRelease(release) }
                    )
                } else {
                    EditorialVinylReleaseCard(
                        release = release,
                        accentColor = accentColor,
                        onClick = { onOpenRelease(release) },
                        onPlayClick = { onPlayRelease(release) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedWideDiscographyCard(
    release: MusicItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF17171D),
        modifier = Modifier
            .width(268.dp)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = release.highResThumbnail,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CardPlaceholderDark)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OledBlack.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = OledBlack.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (release.subtitle.contains("single", ignoreCase = true)) "SINGLE PILIHAN" else "ALBUM UNGGULAN",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = TextPrimaryWhite
                        )
                    }
                }

                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar ${release.title}",
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = release.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = release.subtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorialVinylReleaseCard(
    release: MusicItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF15151A),
        modifier = Modifier
            .width(172.dp)
            .border(1.dp, Color(0xFF282832), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = release.highResThumbnail,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardPlaceholderDark)
                )

                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OledBlack.copy(alpha = 0.78f))
                        .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar ${release.title}",
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = release.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = release.subtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TextSecondaryGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistVideosSection(
    videos: List<MusicItem>,
    accentColor: Color,
    onVideoClick: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Video",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimaryWhite
            )
            Text(
                text = "Video musik resmi & penampilan langsung",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(videos, key = { "vid_${it.id}" }) { video ->
                Column(
                    modifier = Modifier
                        .width(236.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onVideoClick(video) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardPlaceholderDark)
                    ) {
                        AsyncImage(
                            model = video.highResThumbnail,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(OledBlack.copy(alpha = 0.72f))
                                .border(1.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        video.duration?.takeIf { it.isNotBlank() }?.let { dur ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OledBlack.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = dur,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimaryWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = video.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = TextPrimaryWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.subtitle,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextSecondaryGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistFeaturedPlaylistsSection(
    artistName: String,
    playlists: List<MusicItem>,
    accentColor: Color,
    onPlaylistClick: (MusicItem) -> Unit,
    onPlayPlaylist: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Menampilkan $artistName",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = TextPrimaryWhite
            )
            Text(
                text = "Playlist kurasi & radio terbaik",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = TextSecondaryGrey
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists, key = { "feat_pl_${it.id}" }) { playlist ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF16161C),
                    modifier = Modifier
                        .width(240.dp)
                        .border(1.dp, Color(0xFF292934), RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onPlaylistClick(playlist) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = playlist.smallThumbnail,
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardPlaceholderDark)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = TextPrimaryWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = playlist.subtitle,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Penggemar mungkin juga suka" — Strictly Circular (`CircleShape`) Artist Avatars
 * as explicitly requested by the user ("itu image nya bulat ya bukan kotak!").
 */
@Composable
private fun FansMightAlsoLikeCircularSection(
    relatedArtists: List<MusicItem>,
    accentColor: Color,
    onArtistClick: (MusicItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Penggemar mungkin juga suka",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimaryWhite
                )
                Text(
                    text = "Artis serupa yang direkomendasikan untuk Anda",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextSecondaryGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(relatedArtists, key = { "rel_artist_${it.id}_${it.title}" }) { artistItem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onArtistClick(artistItem) }
                        .padding(vertical = 6.dp)
                        .testTag("related_artist_circular_${artistItem.title}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .border(2.dp, accentColor.copy(alpha = 0.55f), CircleShape)
                            .background(CardPlaceholderDark),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = artistItem.highResThumbnail,
                            contentDescription = artistItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = artistItem.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = TextPrimaryWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = artistItem.subtitle.ifBlank { "Artis" },
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextSecondaryGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAboutBioSection(
    artist: ArtistProfileDetail,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF15151B),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 26.dp)
            .border(1.dp, Color(0xFF282834), RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tentang ${artist.name}",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TextPrimaryWhite
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = artist.description.orEmpty(),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = TextSecondaryGrey,
                maxLines = if (expanded) 20 else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
