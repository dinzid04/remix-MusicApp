package com.example.ui.playlist

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.UserLibraryPlaylist
import com.example.ui.home.MusicHomeViewModel
import com.example.ui.home.PlaylistScreenState
import com.example.ui.library.AddToPlaylistBottomSheet
import com.example.ui.library.RenamePlaylistDialog
import com.example.ui.library.SheetActionRow
import com.example.ui.library.UniversalTrackMenuBottomSheet
import com.example.ui.theme.CardPlaceholderDark
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SurfaceObsidian
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dominantColorCache = LruCache<String, Color>(40)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    state: PlaylistScreenState,
    currentPlayingTrackId: String?,
    isPlayingAudio: Boolean,
    favoriteIds: Set<String> = emptySet(),
    downloadedTrackIds: Set<String> = emptySet(),
    activeDownloads: Map<String, ActiveDownloadProgress> = emptyMap(),
    libraryPlaylists: List<UserLibraryPlaylist> = emptyList(),
    savedPlaylistIds: Set<String> = emptySet(),
    onBackClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onShufflePlayClick: () -> Unit,
    onToggleSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortClick: () -> Unit,
    onTrackClick: (MusicItem, List<MusicItem>) -> Unit,
    onTrackMenuClick: (MusicItem) -> Unit,
    onToggleFavoriteTrack: (MusicItem) -> Unit = {},
    onAddTrackToPlaylist: (playlistId: String, track: MusicItem) -> Unit = { _, _ -> },
    onCreatePlaylistAndAddTrack: (title: String, privacy: PlaylistPrivacy, track: MusicItem) -> Unit = { _, _, _ -> },
    onRemoveTrackFromCurrentPlaylist: ((trackId: String) -> Unit)? = null,
    onDownloadSingleTrack: (MusicItem) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onDownloadWholePlaylist: (PlaylistDetail) -> Unit = {},
    onToggleSavePlaylistToLibrary: (PlaylistDetail) -> Unit = {},
    onRenamePlaylist: ((newTitle: String) -> Unit)? = null,
    onTogglePinPlaylist: (() -> Unit)? = null,
    onDeletePlaylist: (() -> Unit)? = null,
    onOpenArtistByName: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state.isVisible) {
        if (state.isSearchVisible) {
            onToggleSearchClick()
        } else {
            onBackClick()
        }
    }

    val playlist = state.playlist ?: return
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showPlaylistMoreSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTrackForSheet by remember { mutableStateOf<MusicItem?>(null) }
    var trackForAddToPlaylist by remember { mutableStateOf<MusicItem?>(null) }

    var extractedDominantColor by remember(playlist.id) {
        mutableStateOf(dominantColorCache.get(playlist.highResThumbnail) ?: Color(0xFF42322B))
    }

    LaunchedEffect(playlist.highResThumbnail) {
        val url = playlist.highResThumbnail
        if (url.isNotBlank()) {
            val cached = dominantColorCache.get(url)
            if (cached != null) {
                extractedDominantColor = cached
            } else {
                val color = extractDominantColorFromUrl(context, url)
                if (color != null) {
                    dominantColorCache.put(url, color)
                    extractedDominantColor = color
                }
            }
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = extractedDominantColor,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "playlist_dominant_bg"
    )

    val displayedTracks by remember(playlist.tracks, state.searchQuery, state.sortAscending) {
        derivedStateOf {
            val filtered = if (state.searchQuery.isBlank()) {
                playlist.tracks
            } else {
                val q = state.searchQuery.trim().lowercase()
                playlist.tracks.filter {
                    it.title.lowercase().contains(q) || it.cleanArtist.lowercase().contains(q)
                }
            }
            if (state.sortAscending) filtered else filtered.reversed()
        }
    }

    val showCompactTopBarTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 340
        }
    }

    val isSpecialBuiltinPlaylist = playlist.id == MusicHomeViewModel.LIKED_SONGS_PLAYLIST_ID ||
        playlist.id == MusicHomeViewModel.DOWNLOADS_PLAYLIST_ID ||
        playlist.id == MusicHomeViewModel.LOCAL_MUSIC_PLAYLIST_ID

    val isSavedInLibrary = savedPlaylistIds.contains(playlist.id)
    val userLibraryEntry = libraryPlaylists.firstOrNull { it.id == playlist.id }

    if (showRenameDialog && onRenamePlaylist != null) {
        RenamePlaylistDialog(
            currentTitle = playlist.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newTitle ->
                onRenamePlaylist(newTitle)
                showRenameDialog = false
            }
        )
    }

    if (showPlaylistMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPlaylistMoreSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF16161D),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = playlist.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.tracks.size} lagu • Opsi Playlist / Album",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 1. Download Seluruh Playlist / Album
                SheetActionRow(
                    icon = Icons.Rounded.Download,
                    iconTint = Color(0xFF38EF7D),
                    title = "Download Semua Lagu (Offline)",
                    subtitle = "Unduh ${playlist.tracks.size} lagu dengan bar % di Notifikasi",
                    onClick = {
                        showPlaylistMoreSheet = false
                        onDownloadWholePlaylist(playlist)
                    },
                    testTag = "playlist_detail_download_all_menu"
                )

                // 2. Simpan / Hapus dari Library
                if (!isSpecialBuiltinPlaylist) {
                    SheetActionRow(
                        icon = if (isSavedInLibrary) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        iconTint = Color(0xFFB4C5FF),
                        title = if (isSavedInLibrary) "Tersimpan di Library (Hapus)" else "Simpan Playlist ke Library",
                        subtitle = if (isSavedInLibrary) "Keluarkan dari koleksi Library Anda" else "Simpan agar selalu muncul di halaman Library",
                        onClick = {
                            showPlaylistMoreSheet = false
                            onToggleSavePlaylistToLibrary(playlist)
                        },
                        testTag = "playlist_detail_save_library_menu"
                    )
                }

                // 3. Pin / Rename / Delete jika playlist ada di Library
                if (userLibraryEntry != null) {
                    if (onTogglePinPlaylist != null) {
                        SheetActionRow(
                            icon = Icons.Rounded.PushPin,
                            iconTint = Color(0xFF38EF7D),
                            title = if (userLibraryEntry.isPinned) "Lepas Pin Playlist" else "Pin Playlist di Library",
                            subtitle = "Sematkan di urutan teratas halaman Library",
                            onClick = {
                                showPlaylistMoreSheet = false
                                onTogglePinPlaylist()
                            }
                        )
                    }
                    if (onRenamePlaylist != null) {
                        SheetActionRow(
                            icon = Icons.Rounded.Edit,
                            iconTint = Color(0xFFD0BCFF),
                            title = "Rename Nama Playlist",
                            subtitle = "Ubah judul playlist ini",
                            onClick = {
                                showPlaylistMoreSheet = false
                                showRenameDialog = true
                            }
                        )
                    }
                    if (onDeletePlaylist != null) {
                        SheetActionRow(
                            icon = Icons.Rounded.DeleteOutline,
                            iconTint = Color(0xFFFF6B6B),
                            title = "Delete Playlist",
                            subtitle = "Hapus playlist dari Library",
                            onClick = {
                                showPlaylistMoreSheet = false
                                onDeletePlaylist()
                            }
                        )
                    }
                }
            }
        }
    }

    selectedTrackForSheet?.let { selectedTrack ->
        val isFav = favoriteIds.contains(selectedTrack.id) ||
            (selectedTrack.videoId != null && favoriteIds.contains(selectedTrack.videoId))
        val isDownloaded = downloadedTrackIds.contains(selectedTrack.id)
        val activeDl = activeDownloads[selectedTrack.id]

        UniversalTrackMenuBottomSheet(
            track = selectedTrack,
            isFavorite = isFav,
            isDownloaded = isDownloaded,
            activeDownloadProgress = activeDl,
            onDismiss = { selectedTrackForSheet = null },
            onToggleFavorite = { onToggleFavoriteTrack(selectedTrack) },
            onAddToPlaylist = { trackForAddToPlaylist = selectedTrack },
            onDownloadTrack = { onDownloadSingleTrack(selectedTrack) },
            onRemoveDownload = { onRemoveDownload(selectedTrack.id) },
            onOpenArtist = { onOpenArtistByName(selectedTrack.cleanArtist) },
            onRemoveFromCurrentPlaylist = if (userLibraryEntry != null && onRemoveTrackFromCurrentPlaylist != null) {
                { onRemoveTrackFromCurrentPlaylist(selectedTrack.id) }
            } else null
        )
    }

    trackForAddToPlaylist?.let { targetTrack ->
        AddToPlaylistBottomSheet(
            track = targetTrack,
            userPlaylists = libraryPlaylists,
            onDismiss = { trackForAddToPlaylist = null },
            onSelectPlaylist = { targetPlaylist ->
                onAddTrackToPlaylist(targetPlaylist.id, targetTrack)
                trackForAddToPlaylist = null
            },
            onCreateNewPlaylistAndAdd = { title, privacy ->
                onCreatePlaylistAndAddTrack(title, privacy, targetTrack)
                trackForAddToPlaylist = null
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceObsidian)
            .testTag("playlist_detail_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedDominantColor.copy(alpha = 0.88f),
                            animatedDominantColor.copy(alpha = 0.48f),
                            SurfaceObsidian
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlist_tracks_lazy_column"),
            contentPadding = PaddingValues(top = 68.dp, bottom = 185.dp)
        ) {
            item(
                key = "playlist_hero_header",
                contentType = "hero_header"
            ) {
                PlaylistHeroHeader(
                    playlist = playlist,
                    isLoading = state.isLoading,
                    isSearchVisible = state.isSearchVisible,
                    searchQuery = state.searchQuery,
                    sortAscending = state.sortAscending,
                    isSavedInLibrary = isSavedInLibrary,
                    isSpecialBuiltinPlaylist = isSpecialBuiltinPlaylist,
                    onPlayAllClick = onPlayAllClick,
                    onShufflePlayClick = onShufflePlayClick,
                    onToggleSearchClick = onToggleSearchClick,
                    onSearchQueryChange = onSearchQueryChange,
                    onToggleSortClick = onToggleSortClick,
                    onDownloadPlaylistClick = { onDownloadWholePlaylist(playlist) },
                    onToggleSavePlaylistClick = { onToggleSavePlaylistToLibrary(playlist) }
                )
            }

            itemsIndexed(
                items = displayedTracks,
                key = { idx, item -> "pl_track_${item.id}_$idx" },
                contentType = { _, _ -> "playlist_track_row" }
            ) { _, track ->
                val isCurrentlyPlaying = currentPlayingTrackId != null &&
                    (track.id == currentPlayingTrackId || track.videoId == currentPlayingTrackId)
                val isDownloaded = downloadedTrackIds.contains(track.id)
                val activeDl = activeDownloads[track.id]

                PlaylistTrackRow(
                    track = track,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    isPlayingAudio = isPlayingAudio,
                    isDownloaded = isDownloaded,
                    activeDownloadProgress = activeDl,
                    onClick = { onTrackClick(track, displayedTracks) },
                    onMenuClick = { selectedTrackForSheet = track }
                )
            }
        }

        // Top Bar with Back Button + Title + 3-Dot Playlist Options Menu (Download All, Pin, Rename, Save, Delete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
                    .testTag("playlist_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCompactTopBarTitle,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    Text(
                        text = playlist.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = { showPlaylistMoreSheet = true },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
                    .testTag("playlist_top_more_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu Playlist & Download",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlaylistHeroHeader(
    playlist: PlaylistDetail,
    isLoading: Boolean,
    isSearchVisible: Boolean,
    searchQuery: String,
    sortAscending: Boolean,
    isSavedInLibrary: Boolean,
    isSpecialBuiltinPlaylist: Boolean,
    onPlayAllClick: () -> Unit,
    onShufflePlayClick: () -> Unit,
    onToggleSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortClick: () -> Unit,
    onDownloadPlaylistClick: () -> Unit,
    onToggleSavePlaylistClick: () -> Unit
) {
    val context = LocalContext.current
    val coverRequest = remember(context, playlist.highResThumbnail) {
        ImageRequest.Builder(context)
            .data(playlist.highResThumbnail)
            .size(540, 540)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        AsyncImage(
            model = coverRequest,
            contentDescription = playlist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(228.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color.Black.copy(alpha = 0.75f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(CardPlaceholderDark)
                .testTag("playlist_hero_cover")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = playlist.title,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimaryWhite,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("playlist_hero_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = playlist.subtitle,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isLoading && playlist.tracks.isEmpty()) {
                "Memuat daftar lagu..."
            } else {
                "${playlist.tracks.size} lagu • ${playlist.description}"
            },
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = Color.White.copy(alpha = 0.56f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row: Putar, Acak, Download Offline, Save to Library, Search, Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Putar Button (Filled White Pill)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable(onClick = onPlayAllClick)
                    .testTag("playlist_play_all_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Putar",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = Color.Black
                )
            }

            // Acak Button (Translucent Glass Pill)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.13f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .clickable(onClick = onShufflePlayClick)
                    .testTag("playlist_shuffle_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Acak",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = Color.White
                )
            }

            // Download Entire Playlist Offline Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF143828))
                    .border(1.dp, Color(0xFF38EF7D).copy(alpha = 0.45f), CircleShape)
                    .clickable(onClick = onDownloadPlaylistClick)
                    .testTag("playlist_download_all_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Download Semua Lagu di Playlist",
                    tint = Color(0xFF38EF7D),
                    modifier = Modifier.size(21.dp)
                )
            }

            // Save to Library Button (for non-builtin playlists)
            if (!isSpecialBuiltinPlaylist) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSavedInLibrary) Color(0xFF3B465E)
                            else Color.White.copy(alpha = 0.13f)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(onClick = onToggleSavePlaylistClick)
                        .testTag("playlist_save_to_library_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSavedInLibrary) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Simpan ke Library",
                        tint = if (isSavedInLibrary) Color(0xFFB4C5FF) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search In Playlist Circular Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSearchVisible) Color.White else Color.White.copy(alpha = 0.13f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onToggleSearchClick)
                    .testTag("playlist_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari di dalam playlist",
                    tint = if (isSearchVisible) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Sort Tracks Circular Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (!sortAscending) Color.White else Color.White.copy(alpha = 0.13f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onToggleSortClick)
                    .testTag("playlist_sort_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Urutkan daftar lagu",
                    tint = if (!sortAscending) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn(tween(180)) + expandVertically(),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari judul lagu atau artis...",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.48f)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun PlaylistTrackRow(
    track: MusicItem,
    isCurrentlyPlaying: Boolean,
    isPlayingAudio: Boolean,
    isDownloaded: Boolean,
    activeDownloadProgress: ActiveDownloadProgress?,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbRequest = remember(context, track.smallThumbnail) {
        ImageRequest.Builder(context)
            .data(track.smallThumbnail)
            .size(144, 144)
            .allowHardware(true)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val rowBgColor = if (isCurrentlyPlaying) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("playlist_track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardPlaceholderDark),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbRequest,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedPlayingEqualizerBars(isPlaying = isPlayingAudio)
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.5.sp,
                letterSpacing = (-0.2).sp,
                color = if (isCurrentlyPlaying) Color(0xFF90CAF9) else TextPrimaryWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeDownloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { activeDownloadProgress.progressPercent / 100f },
                        modifier = Modifier.size(12.dp),
                        color = Color(0xFF38EF7D),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${activeDownloadProgress.progressPercent}% • ",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF38EF7D)
                    )
                } else if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = "Tersimpan Offline",
                        tint = Color(0xFF38EF7D),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = track.artistWithDuration,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextSecondaryGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(44.dp)
                .testTag("playlist_track_more_${track.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi lagu ${track.title}",
                tint = TextSecondaryGrey,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun AnimatedPlayingEqualizerBars(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = if (isPlaying) 0.92f else 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 0.25f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = if (isPlaying) 0.96f else 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_3"
    )

    Canvas(modifier = Modifier.size(18.dp)) {
        val barWidth = size.width * 0.20f
        val gap = size.width * 0.18f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

        val h1 = size.height * bar1
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(0f, size.height - h1),
            size = Size(barWidth, h1),
            cornerRadius = corner
        )

        val h2 = size.height * bar2
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(barWidth + gap, size.height - h2),
            size = Size(barWidth, h2),
            cornerRadius = corner
        )

        val h3 = size.height * bar3
        drawRoundRect(
            color = Color.White,
            topLeft = Offset((barWidth + gap) * 2f, size.height - h3),
            size = Size(barWidth, h3),
            cornerRadius = corner
        )
    }
}

private suspend fun extractDominantColorFromUrl(
    context: android.content.Context,
    imageUrl: String
): Color? = withContext(Dispatchers.IO) {
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(64, 64)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable?.let { drawable ->
            (drawable as? BitmapDrawable)?.bitmap
        } ?: return@withContext null

        sampleDominantRichColor(bitmap)
    } catch (_: Exception) {
        null
    }
}

private fun sampleDominantRichColor(bitmap: Bitmap): Color {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var weightSum = 0L

    val width = bitmap.width
    val height = bitmap.height
    val step = maxOf(1, minOf(width, height) / 12)

    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            val saturation = if (maxC == 0) 0 else ((maxC - minC) * 100) / maxC
            val brightness = (r + g + b) / 3

            val weight = if (brightness in 28..225) (saturation + 25).toLong() else 4L
            rSum += r * weight
            gSum += g * weight
            bSum += b * weight
            weightSum += weight
        }
    }

    if (weightSum <= 0L) return Color(0xFF3E2D27)

    val avgR = (rSum / weightSum).toInt().coerceIn(24, 185)
    val avgG = (gSum / weightSum).toInt().coerceIn(20, 165)
    val avgB = (bSum / weightSum).toInt().coerceIn(24, 185)

    val candidate = Color(avgR, avgG, avgB)
    return if (candidate.luminance() > 0.42f) {
        Color(
            red = (avgR * 0.62f).toInt().coerceIn(20, 255),
            green = (avgG * 0.62f).toInt().coerceIn(20, 255),
            blue = (avgB * 0.62f).toInt().coerceIn(20, 255)
        )
    } else {
        candidate
    }
}
