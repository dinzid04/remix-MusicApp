package com.example.ui.home

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ActiveDownloadProgress
import com.example.data.model.ArtistProfileDetail
import com.example.data.model.ConnectedAccountState
import com.example.data.model.DownloadedTrackItem
import com.example.data.model.HomeSection
import com.example.data.model.HomeThemeMode
import com.example.data.model.IntegrationPlatform
import com.example.data.model.ListeningStatsState
import com.example.data.model.LyricsData
import com.example.data.model.LyricsProvider
import com.example.data.model.MusicItem
import com.example.data.model.PlaylistDetail
import com.example.data.model.PlaylistPrivacy
import com.example.data.model.PlaylistSourceType
import com.example.data.model.SearchCategorySection
import com.example.data.model.SearchFilter
import com.example.data.model.UserLibraryPlaylist
import com.example.data.remote.CuratedHomeFallback
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerController
import com.example.player.MusicDownloadManager
import com.example.player.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlayerSubView {
    MAIN,
    LYRICS,
    QUEUE
}

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val pintasanCepatItems: List<MusicItem> = emptyList(),
    val topShelfTitle: String = "Pilihan cepat",
    val topShelfItems: List<MusicItem> = emptyList(),
    val carouselSections: List<HomeSection> = emptyList(),
    val importedPlaylists: List<MusicItem> = emptyList(),
    val hasListenedToMusic: Boolean = false,
    val selectedTrackForMenu: MusicItem? = null,
    val homeThemeMode: HomeThemeMode = HomeThemeMode.DEFAULT_APP,
    val savedPlaylistIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@Immutable
data class PlaylistScreenState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val playlist: PlaylistDetail? = null,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val sortAscending: Boolean = true
)

@Immutable
data class ArtistScreenState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val artistProfile: ArtistProfileDetail? = null,
    val followedArtistIds: Set<String> = emptySet()
)

@Immutable
data class SearchUiState(
    val query: String = "",
    val isSubmitted: Boolean = false,
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val isLoadingSuggestions: Boolean = false,
    val isLoadingResults: Boolean = false,
    val querySuggestions: List<String> = emptyList(),
    val directSuggestionItems: List<MusicItem> = emptyList(),
    val allCategorySections: List<SearchCategorySection> = emptyList(),
    val filteredItems: List<MusicItem> = emptyList(),
    val recentSearches: List<String> = listOf(
        "Teh Hijau Tulus",
        "Sakura Rossa",
        "Nadin Amizah",
        "Mahalini Sial",
        "Budi Doremi Mesin Waktu",
        "Sheila On 7",
        "Bernadya Satu Bulan"
    )
)

class MusicHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = MusicRepository(database.recentTrackDao())
    private val playerController = AudioPlayerController.getInstance(application)
    private val downloadManager = MusicDownloadManager.getInstance(application)
    private val prefs = application.getSharedPreferences("bitchord_experimental_prefs", Context.MODE_PRIVATE)

    private val _scrapedSections = MutableStateFlow<List<HomeSection>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _selectedTrackForMenu = MutableStateFlow<MusicItem?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun showUiMessage(message: String) {
        _uiMessage.value = message
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    private val _homeThemeMode = MutableStateFlow(loadSavedHomeThemeMode())
    val homeThemeMode: StateFlow<HomeThemeMode> = _homeThemeMode.asStateFlow()

    val libraryPlaylists: StateFlow<List<UserLibraryPlaylist>> = repository.libraryPlaylistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedPlaylistIds: StateFlow<Set<String>> = libraryPlaylists
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val favoriteTracks: StateFlow<List<MusicItem>> = repository.favoriteTracksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedTracks: StateFlow<List<DownloadedTrackItem>> = repository.downloadedTracksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedTrackIds: StateFlow<Set<String>> = repository.downloadedTrackIdsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val activeDownloads: StateFlow<Map<String, ActiveDownloadProgress>> = downloadManager.activeDownloads

    private val _localDeviceTracks = MutableStateFlow<List<MusicItem>>(emptyList())
    val localDeviceTracks: StateFlow<List<MusicItem>> = _localDeviceTracks.asStateFlow()

    private val _playlistScreenState = MutableStateFlow(PlaylistScreenState())
    val playlistScreenState: StateFlow<PlaylistScreenState> = _playlistScreenState.asStateFlow()

    private val _artistScreenState = MutableStateFlow(ArtistScreenState())
    val artistScreenState: StateFlow<ArtistScreenState> = _artistScreenState.asStateFlow()
    private val artistBackStack = ArrayDeque<ArtistProfileDetail>()

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible.asStateFlow()

    private val _isAccountIntegrationsVisible = MutableStateFlow(false)
    val isAccountIntegrationsVisible: StateFlow<Boolean> = _isAccountIntegrationsVisible.asStateFlow()

    private val _spotifyAccount = MutableStateFlow(ConnectedAccountState(platform = IntegrationPlatform.SPOTIFY))
    val spotifyAccount: StateFlow<ConnectedAccountState> = _spotifyAccount.asStateFlow()

    private val _youtubeMusicAccount = MutableStateFlow(ConnectedAccountState(platform = IntegrationPlatform.YOUTUBE_MUSIC))
    val youtubeMusicAccount: StateFlow<ConnectedAccountState> = _youtubeMusicAccount.asStateFlow()

    private val _importedPlaylists = MutableStateFlow<List<MusicItem>>(emptyList())
    val importedPlaylists: StateFlow<List<MusicItem>> = _importedPlaylists.asStateFlow()

    private val _isIntegrationSyncing = MutableStateFlow(false)
    val isIntegrationSyncing: StateFlow<Boolean> = _isIntegrationSyncing.asStateFlow()

    private val _integrationStatusMessage = MutableStateFlow<String?>(null)
    val integrationStatusMessage: StateFlow<String?> = _integrationStatusMessage.asStateFlow()

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _activePlayerSubView = MutableStateFlow(PlayerSubView.MAIN)
    val activePlayerSubView: StateFlow<PlayerSubView> = _activePlayerSubView.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _selectedLyricsProvider = MutableStateFlow(LyricsProvider.LRC_RED)
    val selectedLyricsProvider: StateFlow<LyricsProvider> = _selectedLyricsProvider.asStateFlow()

    private val _customCoverOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val customCoverOverrides: StateFlow<Map<String, String>> = _customCoverOverrides.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun clearIntegrationStatusMessage() {
        _integrationStatusMessage.value = null
    }

    val playerState: StateFlow<PlayerUiState> = playerController.playerState

    val hasActiveTrack: StateFlow<Boolean> = playerController.playerState
        .map { it.currentTrack != null }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentPlayingTrackId: StateFlow<String?> = playerController.playerState
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPlayingAudio: StateFlow<Boolean> = playerController.playerState
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIdsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val recentTracks: StateFlow<List<MusicItem>> = repository.recentTracksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val listeningStats: StateFlow<ListeningStatsState> = combine(
        recentTracks,
        favoriteTracks
    ) { recents, favs ->
        val baseTracks = recents.size + favs.size
        val minutes = if (baseTracks == 0) 0 else (recents.size * 4) + (favs.size * 3)
        val topTrack = recents.firstOrNull() ?: favs.firstOrNull()
        ListeningStatsState(
            minutesListened = minutes,
            topTrack = topTrack,
            totalTracksPlayed = recents.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListeningStatsState()
    )

    private var trackEnrichmentJob: Job? = null
    private var liveSuggestionJob: Job? = null
    private var fullSearchJob: Job? = null
    private var artistLoadJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        repository.recentTracksFlow,
        _scrapedSections,
        _isLoading,
        _selectedTrackForMenu,
        _homeThemeMode
    ) { recents, sections, loading, menuTrack, themeMode ->
        val quickPicksSection = sections.firstOrNull { sec ->
            val low = sec.title.lowercase()
            low.contains("pilihan cepat") || low.contains("quick picks")
        }

        val topShelfItems = quickPicksSection?.items?.takeIf { it.isNotEmpty() }
            ?: repository.getQuickPicksDefault()

        val remainingSections = sections.filterNot { sec ->
            sec.id == quickPicksSection?.id
        }

        val pintasanItems = if (recents.isNotEmpty()) {
            recents.take(27)
        } else {
            emptyList()
        }

        HomeUiState(
            isLoading = loading,
            pintasanCepatItems = pintasanItems,
            topShelfTitle = "Pilihan cepat",
            topShelfItems = topShelfItems,
            carouselSections = remainingSections,
            importedPlaylists = emptyList(), // Imported playlists live in Library screen per user request
            hasListenedToMusic = recents.isNotEmpty(),
            selectedTrackForMenu = menuTrack,
            homeThemeMode = themeMode,
            savedPlaylistIds = savedPlaylistIds.value,
            errorMessage = _errorMessage.value
        )
    }.combine(savedPlaylistIds) { baseState, savedIds ->
        baseState.copy(savedPlaylistIds = savedIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            isLoading = true,
            pintasanCepatItems = emptyList(),
            topShelfTitle = "Pilihan cepat",
            topShelfItems = repository.getQuickPicksDefault(),
            carouselSections = CuratedHomeFallback.getRequiredHomeSectionsFallback().drop(1),
            homeThemeMode = _homeThemeMode.value
        )
    )

    init {
        playerController.setFavoriteCallback { track ->
            toggleFavoriteTrack(track)
        }
        viewModelScope.launch {
            combine(
                playerController.playerState.map { it.currentTrack }.distinctUntilChanged(),
                favoriteIds
            ) { track, favs ->
                if (track == null) false
                else favs.contains(track.id) || (track.videoId != null && favs.contains(track.videoId))
            }.collect { isFav ->
                playerController.updateCurrentTrackFavoriteStatus(isFav)
            }
        }
        // Keep open special playlists (Liked Songs, Downloads, User Playlists) synced in real time
        viewModelScope.launch {
            favoriteTracks.collect { favs ->
                val current = _playlistScreenState.value
                if (current.isVisible && current.playlist?.id == LIKED_SONGS_PLAYLIST_ID) {
                    _playlistScreenState.update {
                        it.copy(
                            playlist = it.playlist?.copy(
                                subtitle = "Playlist Otomatis • ${favs.size} lagu",
                                thumbnailUrl = favs.firstOrNull()?.thumbnailUrl ?: "",
                                tracks = favs
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            downloadedTracks.collect { downloads ->
                val current = _playlistScreenState.value
                if (current.isVisible && current.playlist?.id == DOWNLOADS_PLAYLIST_ID) {
                    val items = downloads.map { it.toMusicItem() }
                    _playlistScreenState.update {
                        it.copy(
                            playlist = it.playlist?.copy(
                                subtitle = "Musik Offline • ${items.size} lagu",
                                thumbnailUrl = items.firstOrNull()?.thumbnailUrl ?: "",
                                tracks = items
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            libraryPlaylists.collect { playlists ->
                val current = _playlistScreenState.value
                val openId = current.playlist?.id
                if (current.isVisible && openId != null) {
                    val match = playlists.firstOrNull { it.id == openId }
                    if (match != null) {
                        _playlistScreenState.update {
                            it.copy(
                                playlist = it.playlist?.copy(
                                    title = match.title,
                                    subtitle = match.subtitle,
                                    thumbnailUrl = match.thumbnailUrl.ifBlank {
                                        match.tracks.firstOrNull()?.thumbnailUrl ?: ""
                                    },
                                    tracks = if (match.tracks.isNotEmpty() || match.sourceType == PlaylistSourceType.USER_CREATED) {
                                        match.tracks
                                    } else {
                                        it.playlist.tracks
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        loadHomeData()
        refreshLocalDeviceMusic()
    }

    private fun loadSavedHomeThemeMode(): HomeThemeMode {
        val raw = prefs.getString("home_theme_mode", HomeThemeMode.DEFAULT_APP.name)
        return try {
            HomeThemeMode.valueOf(raw ?: HomeThemeMode.DEFAULT_APP.name)
        } catch (_: Exception) {
            HomeThemeMode.DEFAULT_APP
        }
    }

    fun setHomeThemeMode(mode: HomeThemeMode) {
        _homeThemeMode.value = mode
        prefs.edit().putString("home_theme_mode", mode.name).apply()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            if (_scrapedSections.value.isEmpty()) {
                _scrapedSections.value = CuratedHomeFallback.getRequiredHomeSectionsFallback()
                _isLoading.value = false
            }
            _errorMessage.value = null
            val sections = repository.fetchHomeSections()
            if (sections.isNotEmpty()) {
                _scrapedSections.value = sections
            }
            _isLoading.value = false
        }
    }

    // ========================================================================
    // LIBRARY & PLAYLIST MANAGEMENT (CREATE, RENAME, DELETE, PIN, ADD TRACK, SAVE)
    // ========================================================================
    fun createCustomPlaylist(title: String, privacy: PlaylistPrivacy = PlaylistPrivacy.PUBLIC) {
        val clean = title.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.createCustomPlaylist(clean, privacy)
            _uiMessage.value = "Playlist \"$clean\" berhasil dibuat di Library"
        }
    }

    fun createCustomPlaylistAndAddTrack(
        title: String,
        privacy: PlaylistPrivacy = PlaylistPrivacy.PUBLIC,
        track: MusicItem
    ) {
        val clean = title.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val created = repository.createCustomPlaylist(clean, privacy)
            repository.addTrackToLibraryPlaylist(created.id, track)
            _uiMessage.value = "\"${track.title}\" disimpan ke playlist baru \"$clean\""
        }
    }

    fun renameLibraryPlaylist(playlistId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.renameLibraryPlaylist(playlistId, clean)
            _uiMessage.value = "Nama playlist diubah menjadi \"$clean\""
        }
    }

    fun deleteLibraryPlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deleteLibraryPlaylist(playlistId)
            if (_playlistScreenState.value.playlist?.id == playlistId) {
                closePlaylistDetail()
            }
            _uiMessage.value = "Playlist dihapus dari Library"
        }
    }

    fun togglePinLibraryPlaylist(playlistId: String) {
        viewModelScope.launch {
            val target = libraryPlaylists.value.firstOrNull { it.id == playlistId } ?: return@launch
            val nextPinned = !target.isPinned
            repository.setPlaylistPinned(playlistId, nextPinned)
            _uiMessage.value = if (nextPinned) {
                "\"${target.title}\" disematkan (Pin) di atas Library"
            } else {
                "Pin \"${target.title}\" dilepas"
            }
        }
    }

    fun addTrackToLibraryPlaylist(playlistId: String, track: MusicItem) {
        viewModelScope.launch {
            val target = libraryPlaylists.value.firstOrNull { it.id == playlistId }
            repository.addTrackToLibraryPlaylist(playlistId, track)
            _uiMessage.value = "Ditambahkan ke \"${target?.title ?: "Playlist"}\""
        }
    }

    fun removeTrackFromLibraryPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromLibraryPlaylist(playlistId, trackId)
            _uiMessage.value = "Lagu dihapus dari playlist"
        }
    }

    fun toggleSavePlaylistDetailToLibrary(detail: PlaylistDetail) {
        viewModelScope.launch {
            val isAlreadySaved = savedPlaylistIds.value.contains(detail.id)
            if (isAlreadySaved) {
                repository.deleteLibraryPlaylist(detail.id)
                _uiMessage.value = "\"${detail.title}\" dihapus dari Library"
            } else {
                repository.savePlaylistToLibrary(
                    playlistDetail = detail,
                    sourceType = PlaylistSourceType.SAVED_COMMUNITY
                )
                _uiMessage.value = "\"${detail.title}\" disimpan ke Library Anda"
            }
        }
    }

    fun openLikedSongsPlaylist() {
        val favs = favoriteTracks.value
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = false,
            playlist = PlaylistDetail(
                id = LIKED_SONGS_PLAYLIST_ID,
                title = "Musik yang Disukai",
                subtitle = "Playlist Otomatis • ${favs.size} lagu",
                description = "Semua lagu yang Anda tandai suka tersimpan di sini.",
                thumbnailUrl = favs.firstOrNull()?.thumbnailUrl ?: "",
                isAlbum = false,
                tracks = favs
            )
        )
    }

    fun openDownloadsPlaylist() {
        val items = downloadedTracks.value.map { it.toMusicItem() }
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = false,
            playlist = PlaylistDetail(
                id = DOWNLOADS_PLAYLIST_ID,
                title = "Downloads",
                subtitle = "Musik Offline • ${items.size} lagu",
                description = "Lagu yang telah diunduh ke penyimpanan perangkat & siap diputar offline.",
                thumbnailUrl = items.firstOrNull()?.thumbnailUrl ?: "",
                isAlbum = false,
                tracks = items
            )
        )
    }

    fun refreshLocalDeviceMusic() {
        viewModelScope.launch {
            val scanned = downloadManager.scanLocalDeviceMusic()
            val downloadedItems = downloadedTracks.value.map { it.toMusicItem() }
            val combined = (downloadedItems + scanned).distinctBy { it.id }
            _localDeviceTracks.value = combined
        }
    }

    fun openLocalMusicPlaylist() {
        viewModelScope.launch {
            val scanned = downloadManager.scanLocalDeviceMusic()
            val downloadedItems = downloadedTracks.value.map { it.toMusicItem() }
            val combined = (downloadedItems + scanned).distinctBy { it.id }
            _localDeviceTracks.value = combined
            _playlistScreenState.value = PlaylistScreenState(
                isVisible = true,
                isLoading = false,
                playlist = PlaylistDetail(
                    id = LOCAL_MUSIC_PLAYLIST_ID,
                    title = "Local Music",
                    subtitle = "Penyimpanan Perangkat • ${combined.size} lagu",
                    description = "File audio dari penyimpanan lokal & folder Music/BitChord perangkat Anda.",
                    thumbnailUrl = combined.firstOrNull()?.thumbnailUrl ?: "",
                    isAlbum = false,
                    tracks = combined
                )
            )
        }
    }

    fun openLibraryPlaylist(userPlaylist: UserLibraryPlaylist) {
        val initialDetail = PlaylistDetail(
            id = userPlaylist.id,
            title = userPlaylist.title,
            subtitle = userPlaylist.subtitle,
            description = userPlaylist.description,
            thumbnailUrl = userPlaylist.thumbnailUrl.ifBlank {
                userPlaylist.tracks.firstOrNull()?.thumbnailUrl ?: ""
            },
            isAlbum = false,
            tracks = userPlaylist.tracks
        )

        val needsRemoteFetch = userPlaylist.tracks.isEmpty() &&
            userPlaylist.sourceType != PlaylistSourceType.USER_CREATED

        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = needsRemoteFetch,
            playlist = initialDetail
        )

        if (needsRemoteFetch) {
            viewModelScope.launch {
                val seed = userPlaylist.toMusicItem()
                val fetched = repository.fetchPlaylistOrAlbumDetail(seed)
                if (fetched.tracks.isNotEmpty()) {
                    repository.savePlaylistToLibrary(
                        playlistDetail = fetched.copy(id = userPlaylist.id, title = userPlaylist.title),
                        sourceType = userPlaylist.sourceType,
                        platformBadge = userPlaylist.platformBadge
                    )
                    _playlistScreenState.update {
                        it.copy(
                            isLoading = false,
                            playlist = fetched.copy(id = userPlaylist.id, title = userPlaylist.title)
                        )
                    }
                } else {
                    _playlistScreenState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ========================================================================
    // OFFLINE MUSIC DOWNLOADER (SINGLE TRACK & FULL PLAYLIST/ALBUM)
    // ========================================================================
    fun downloadSingleTrack(track: MusicItem) {
        downloadManager.downloadTrack(
            track = track,
            onCompleted = { item ->
                if (item != null) {
                    _uiMessage.value = "\"${track.title}\" selesai diunduh untuk offline"
                    refreshLocalDeviceMusic()
                } else {
                    _uiMessage.value = "Gagal mengunduh \"${track.title}\""
                }
            }
        )
        _uiMessage.value = "Mengunduh \"${track.title}\" (lihat progres di Notifikasi)..."
    }

    fun downloadPlaylistOrAlbum(detail: PlaylistDetail) {
        if (detail.tracks.isEmpty()) {
            _uiMessage.value = "Tidak ada lagu untuk diunduh di daftar ini"
            return
        }
        downloadManager.downloadTracksBatch(
            collectionTitle = detail.title,
            tracks = detail.tracks,
            onBatchCompleted = { count ->
                _uiMessage.value = "$count lagu dari \"${detail.title}\" selesai diunduh"
                refreshLocalDeviceMusic()
            }
        )
        _uiMessage.value = "Mengunduh ${detail.tracks.size} lagu dari \"${detail.title}\"..."
    }

    fun downloadMusicItemCollection(collectionItem: MusicItem) {
        viewModelScope.launch {
            val existingLib = libraryPlaylists.value.firstOrNull { it.id == collectionItem.id }
            if (existingLib != null && existingLib.tracks.isNotEmpty()) {
                downloadManager.downloadTracksBatch(
                    collectionTitle = existingLib.title,
                    tracks = existingLib.tracks,
                    onBatchCompleted = { count ->
                        _uiMessage.value = "$count lagu dari \"${existingLib.title}\" selesai diunduh"
                        refreshLocalDeviceMusic()
                    }
                )
                _uiMessage.value = "Mengunduh ${existingLib.tracks.size} lagu dari \"${existingLib.title}\"..."
                return@launch
            }
            val detail = repository.fetchPlaylistOrAlbumDetail(collectionItem)
            downloadPlaylistOrAlbum(detail)
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownloadedTrack(trackId)
            refreshLocalDeviceMusic()
            _uiMessage.value = "Unduhan offline dihapus"
        }
    }

    // ========================================================================
    // ARTIST PROFILE SCREEN NAVIGATION & ACTIONS
    // ========================================================================
    fun openArtistProfile(seedItem: MusicItem) {
        val current = _artistScreenState.value.artistProfile
        if (_artistScreenState.value.isVisible && current != null &&
            !current.name.equals(seedItem.title, ignoreCase = true)
        ) {
            artistBackStack.addLast(current)
        }

        val rawName = if (seedItem.type == "artist" || seedItem.browseId?.startsWith("UC") == true) {
            seedItem.title
        } else {
            seedItem.cleanArtist.ifBlank { seedItem.title }
        }
        val instantFallback = CuratedHomeFallback.getArtistProfileFallback(rawName, seedItem)

        _artistScreenState.update {
            it.copy(
                isVisible = true,
                isLoading = true,
                artistProfile = instantFallback
            )
        }

        artistLoadJob?.cancel()
        artistLoadJob = viewModelScope.launch {
            val detail = repository.fetchArtistProfile(seedItem)
            _artistScreenState.update {
                it.copy(
                    isLoading = false,
                    artistProfile = detail
                )
            }
        }
    }

    fun openArtistProfileByName(artistName: String, thumbnailUrl: String = "") {
        val cleanName = MusicItem.sanitizeArtistName(artistName, isCollection = false)
            .substringBefore(",")
            .substringBefore("&")
            .trim()
            .ifBlank { artistName }
        val seed = MusicItem(
            id = "artist_${cleanName.lowercase()}",
            title = cleanName,
            subtitle = "Artis",
            thumbnailUrl = thumbnailUrl,
            browseId = "UC_${cleanName.lowercase()}",
            type = "artist"
        )
        openArtistProfile(seed)
    }

    fun closeArtistProfile() {
        if (artistBackStack.isNotEmpty()) {
            val previous = artistBackStack.removeLast()
            _artistScreenState.update {
                it.copy(
                    isVisible = true,
                    isLoading = false,
                    artistProfile = previous
                )
            }
        } else {
            _artistScreenState.update {
                it.copy(isVisible = false, isLoading = false)
            }
        }
    }

    fun toggleFollowArtist(artistIdOrName: String) {
        _artistScreenState.update { state ->
            val nextSet = if (state.followedArtistIds.contains(artistIdOrName)) {
                state.followedArtistIds - artistIdOrName
            } else {
                state.followedArtistIds + artistIdOrName
            }
            state.copy(followedArtistIds = nextSet)
        }
    }

    // ========================================================================
    // SETTINGS & ACCOUNT INTEGRATIONS (YOUTUBE MUSIC GOOGLE & SPOTIFY)
    // ========================================================================
    fun openSettingsScreen() {
        _isSettingsVisible.value = true
    }

    fun closeSettingsScreen() {
        _isSettingsVisible.value = false
    }

    fun openAccountIntegrationsScreen() {
        _isAccountIntegrationsVisible.value = true
    }

    fun closeAccountIntegrationsScreen() {
        _isAccountIntegrationsVisible.value = false
    }

    private suspend fun persistImportedPlaylistsToLibrary(
        importedItems: List<MusicItem>,
        platformBadge: String
    ) {
        for (item in importedItems) {
            val tracks = if (item.previewTracks.isNotEmpty()) {
                item.previewTracks
            } else {
                repository.fetchPlaylistOrAlbumDetail(item).tracks
            }
            val detail = PlaylistDetail(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle.ifBlank { "Impor $platformBadge • ${tracks.size} lagu" },
                description = "Diimpor dari $platformBadge ke Library Anda",
                thumbnailUrl = item.thumbnailUrl.ifBlank { tracks.firstOrNull()?.thumbnailUrl ?: "" },
                isAlbum = false,
                tracks = tracks
            )
            repository.savePlaylistToLibrary(
                playlistDetail = detail,
                sourceType = PlaylistSourceType.IMPORTED,
                platformBadge = platformBadge
            )
        }
    }

    fun connectSpotifyAccount(accountInput: String, tokenOrUrl: String) {
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            val result = repository.connectSpotifyAndImport(accountInput, tokenOrUrl)
            _spotifyAccount.value = result.account
            _importedPlaylists.update { current ->
                (result.importedPlaylists + current).distinctBy { it.id }
            }
            persistImportedPlaylistsToLibrary(result.importedPlaylists, "Spotify")
            _isIntegrationSyncing.value = false
            _integrationStatusMessage.value = "${result.statusMessage} Tersimpan di halaman Library."
        }
    }

    fun connectYouTubeMusicAccount(googleEmail: String, tokenOrUrl: String) {
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            val result = repository.connectYouTubeMusicAndImport(googleEmail, tokenOrUrl)
            _youtubeMusicAccount.value = result.account
            _importedPlaylists.update { current ->
                (result.importedPlaylists + current).distinctBy { it.id }
            }
            persistImportedPlaylistsToLibrary(result.importedPlaylists, "YouTube Music")
            _isIntegrationSyncing.value = false
            _integrationStatusMessage.value = "${result.statusMessage} Tersimpan di halaman Library."
        }
    }

    fun importPlaylistByUrl(platform: IntegrationPlatform, urlOrId: String) {
        val clean = urlOrId.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _isIntegrationSyncing.value = true
            _integrationStatusMessage.value = null
            val imported = repository.importPlaylistByUrl(platform, clean)
            if (imported != null) {
                _importedPlaylists.update { current ->
                    (listOf(imported) + current).distinctBy { it.id }
                }
                persistImportedPlaylistsToLibrary(listOf(imported), platform.displayName)
                val msg = "Playlist \"${imported.title}\" berhasil diimpor ke Library!"
                _integrationStatusMessage.value = msg
                _uiMessage.value = msg
            } else {
                val errMsg = "Gagal mengimpor playlist dari tautan tersebut."
                _integrationStatusMessage.value = errMsg
                _uiMessage.value = errMsg
            }
            _isIntegrationSyncing.value = false
        }
    }

    fun disconnectIntegration(platform: IntegrationPlatform) {
        when (platform) {
            IntegrationPlatform.SPOTIFY -> {
                _spotifyAccount.value = ConnectedAccountState(platform = IntegrationPlatform.SPOTIFY)
                _integrationStatusMessage.value = "Koneksi akun Spotify telah diputuskan."
            }
            IntegrationPlatform.YOUTUBE_MUSIC -> {
                _youtubeMusicAccount.value = ConnectedAccountState(platform = IntegrationPlatform.YOUTUBE_MUSIC)
                _integrationStatusMessage.value = "Koneksi akun Google (YouTube Music) telah diputuskan."
            }
            else -> Unit
        }
    }

    // ========================================================================
    // SEARCH LOGIC (Live Suggestions + Full Categorized & Filtered Search)
    // ========================================================================
    fun onSearchQueryChange(newQuery: String) {
        _searchUiState.update {
            it.copy(
                query = newQuery,
                isSubmitted = false,
                isLoadingSuggestions = newQuery.isNotBlank()
            )
        }
        liveSuggestionJob?.cancel()
        if (newQuery.isBlank()) {
            _searchUiState.update {
                it.copy(
                    isLoadingSuggestions = false,
                    querySuggestions = emptyList(),
                    directSuggestionItems = emptyList()
                )
            }
            return
        }

        liveSuggestionJob = viewModelScope.launch {
            delay(220L)
            val payload = repository.fetchSearchSuggestions(newQuery)
            _searchUiState.update { current ->
                if (current.query == newQuery && !current.isSubmitted) {
                    current.copy(
                        isLoadingSuggestions = false,
                        querySuggestions = payload.querySuggestions,
                        directSuggestionItems = payload.directItems
                    )
                } else {
                    current
                }
            }
        }
    }

    fun onFillQueryFromSuggestion(suggestionText: String) {
        onSearchQueryChange(suggestionText)
    }

    fun onClearSearchQuery() {
        liveSuggestionJob?.cancel()
        fullSearchJob?.cancel()
        _searchUiState.update {
            it.copy(
                query = "",
                isSubmitted = false,
                selectedFilter = SearchFilter.ALL,
                isLoadingSuggestions = false,
                isLoadingResults = false,
                querySuggestions = emptyList(),
                directSuggestionItems = emptyList(),
                allCategorySections = emptyList(),
                filteredItems = emptyList()
            )
        }
    }

    fun onSubmitSearch(submittedQuery: String = _searchUiState.value.query) {
        val clean = submittedQuery.trim()
        if (clean.isBlank()) return

        liveSuggestionJob?.cancel()
        fullSearchJob?.cancel()

        _searchUiState.update { state ->
            val updatedRecents = (listOf(clean) + state.recentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            state.copy(
                query = clean,
                isSubmitted = true,
                selectedFilter = SearchFilter.ALL,
                isLoadingSuggestions = false,
                isLoadingResults = true,
                recentSearches = updatedRecents
            )
        }

        fullSearchJob = viewModelScope.launch {
            val sections = repository.searchAllCategories(clean)
            _searchUiState.update { current ->
                current.copy(
                    isLoadingResults = false,
                    allCategorySections = sections
                )
            }
        }
    }

    fun onSelectSearchFilter(filter: SearchFilter) {
        val currentQuery = _searchUiState.value.query.trim()
        if (currentQuery.isBlank()) return

        _searchUiState.update {
            it.copy(
                selectedFilter = filter,
                isSubmitted = true,
                isLoadingResults = true
            )
        }

        fullSearchJob?.cancel()
        fullSearchJob = viewModelScope.launch {
            if (filter == SearchFilter.ALL) {
                val sections = repository.searchAllCategories(currentQuery)
                _searchUiState.update {
                    it.copy(
                        isLoadingResults = false,
                        allCategorySections = sections
                    )
                }
            } else {
                val apiKey = filter.apiFilterKey ?: "songs"
                val items = repository.searchByFilter(currentQuery, filter = apiKey)
                _searchUiState.update {
                    it.copy(
                        isLoadingResults = false,
                        filteredItems = items
                    )
                }
            }
        }
    }

    fun onSearchItemClicked(item: MusicItem, contextItems: List<MusicItem> = emptyList()) {
        if (item.type == "artist" || item.browseId?.startsWith("UC") == true) {
            openArtistProfile(item)
        } else if (item.type == "playlist" || item.type == "album") {
            openPlaylistOrAlbumDetail(item)
        } else {
            val playableQueue = contextItems.filter { !it.isCollection && it.type != "artist" }
                .ifEmpty { listOf(item) }
            playSingleOrQueueTrack(
                item = item,
                contextQueue = playableQueue,
                sourceLabel = "Playing from Search"
            )
        }
    }

    // ========================================================================
    // COMMUNITY PLAYLIST CARD ("Dari komunitas") ACTIONS
    // ========================================================================
    fun onPlayCommunityPlaylistFirstTrack(playlistItem: MusicItem) {
        val preview = playlistItem.previewTracks
        if (preview.isNotEmpty()) {
            playSingleOrQueueTrack(
                item = preview.first(),
                contextQueue = preview,
                sourceLabel = "Playing from ${playlistItem.title}"
            )
        }
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(playlistItem)
            if (detail.tracks.isNotEmpty()) {
                val firstTrack = detail.tracks.first()
                playSingleOrQueueTrack(
                    item = firstTrack,
                    contextQueue = detail.tracks,
                    sourceLabel = "Playing from ${detail.title}"
                )
            }
        }
    }

    fun onStartCommunityPlaylistRadio(playlistItem: MusicItem) {
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(playlistItem)
            val tracks = detail.tracks.ifEmpty { playlistItem.previewTracks }
            if (tracks.isNotEmpty()) {
                val shuffled = tracks.shuffled()
                playSingleOrQueueTrack(
                    item = shuffled.first(),
                    contextQueue = shuffled,
                    sourceLabel = "Radio • ${detail.title}"
                )
            }
        }
    }

    fun onToggleSaveCommunityPlaylist(playlistItem: MusicItem) {
        viewModelScope.launch {
            val isSaved = savedPlaylistIds.value.contains(playlistItem.id)
            if (isSaved) {
                repository.deleteLibraryPlaylist(playlistItem.id)
                _uiMessage.value = "\"${playlistItem.title}\" dihapus dari Library"
            } else {
                val tracks = if (playlistItem.previewTracks.isNotEmpty()) {
                    playlistItem.previewTracks
                } else {
                    repository.fetchPlaylistOrAlbumDetail(playlistItem).tracks
                }
                val detail = PlaylistDetail(
                    id = playlistItem.id,
                    title = playlistItem.title,
                    subtitle = playlistItem.subtitle.ifBlank { "Playlist • ${tracks.size} lagu" },
                    description = "Disimpan ke Library Anda",
                    thumbnailUrl = playlistItem.thumbnailUrl,
                    isAlbum = playlistItem.type == "album",
                    tracks = tracks
                )
                repository.savePlaylistToLibrary(
                    playlistDetail = detail,
                    sourceType = PlaylistSourceType.SAVED_COMMUNITY
                )
                _uiMessage.value = "\"${playlistItem.title}\" disimpan ke Library"
            }
        }
    }

    // ========================================================================
    // PLAYER & LYRICS PROVIDER LOGIC
    // ========================================================================
    fun openFullPlayer(initialSubView: PlayerSubView = PlayerSubView.MAIN) {
        _activePlayerSubView.value = initialSubView
        _isPlayerExpanded.value = true
        playerController.setFullPlayerExpanded(true)
    }

    fun closeFullPlayer() {
        _isPlayerExpanded.value = false
        _activePlayerSubView.value = PlayerSubView.MAIN
        playerController.setFullPlayerExpanded(false)
    }

    fun setPlayerSubView(subView: PlayerSubView) {
        if (_activePlayerSubView.value == subView && subView != PlayerSubView.MAIN) {
            _activePlayerSubView.value = PlayerSubView.MAIN
        } else {
            _activePlayerSubView.value = subView
        }
    }

    fun selectLyricsProvider(provider: LyricsProvider) {
        _selectedLyricsProvider.value = provider
        val activeTrack = playerController.playerState.value.currentTrack ?: return
        viewModelScope.launch {
            val updatedLyrics = repository.getTrackLyrics(activeTrack, preferredProvider = provider)
            _currentLyrics.value = updatedLyrics
        }
    }

    fun applyLrcHdCover(track: MusicItem, hdCoverUrl: String) {
        if (hdCoverUrl.isBlank()) return
        _customCoverOverrides.update { current ->
            current + (track.id to hdCoverUrl)
        }
    }

    fun restoreDefaultCover(track: MusicItem) {
        _customCoverOverrides.update { current ->
            current - track.id
        }
    }

    fun onHomeItemClicked(item: MusicItem) {
        if (item.type == "artist" || item.browseId?.startsWith("UC") == true) {
            openArtistProfile(item)
            return
        }
        if (item.type == "daily_recommendation_track") {
            val dailySection = _scrapedSections.value.firstOrNull {
                it.title.contains("rekomendasi harian", ignoreCase = true)
            }
            val queue = dailySection?.items?.takeIf { it.isNotEmpty() } ?: listOf(item)
            playSingleOrQueueTrack(
                item = item,
                contextQueue = queue,
                sourceLabel = "Playing from Rekomendasi harian Anda"
            )
            return
        }

        if (item.isCollection) {
            openPlaylistOrAlbumDetail(item)
        } else {
            val parentSection = _scrapedSections.value.firstOrNull { sec ->
                sec.items.any { it.id == item.id }
            }
            val sourceName = parentSection?.title ?: "Pilihan cepat"
            val sectionPlayableQueue = parentSection?.items
                ?.filter { !it.isCollection }
                ?.takeIf { it.isNotEmpty() }
                ?: uiState.value.topShelfItems
            playSingleOrQueueTrack(
                item = item,
                contextQueue = sectionPlayableQueue,
                sourceLabel = "Playing from $sourceName"
            )
        }
    }

    fun onPlaySectionAll(sectionTitle: String, items: List<MusicItem>) {
        val playable = items.filter { !it.isCollection }
        val firstTrack = playable.firstOrNull() ?: items.firstOrNull() ?: return
        if (firstTrack.isCollection && playable.isEmpty()) {
            openPlaylistOrAlbumDetail(firstTrack)
        } else {
            playSingleOrQueueTrack(
                item = firstTrack,
                contextQueue = playable.ifEmpty { listOf(firstTrack) },
                sourceLabel = "Playing from $sectionTitle"
            )
        }
    }

    fun openPlaylistOrAlbumDetail(seedItem: MusicItem) {
        val existingUserPlaylist = libraryPlaylists.value.firstOrNull { it.id == seedItem.id }
        if (existingUserPlaylist != null) {
            openLibraryPlaylist(existingUserPlaylist)
            return
        }

        val fallbackDetail = CuratedHomeFallback.getPlaylistFallback(seedItem)
        _playlistScreenState.value = PlaylistScreenState(
            isVisible = true,
            isLoading = true,
            playlist = fallbackDetail
        )
        viewModelScope.launch {
            val detail = repository.fetchPlaylistOrAlbumDetail(seedItem)
            _playlistScreenState.update { state ->
                state.copy(
                    isLoading = false,
                    playlist = detail
                )
            }
        }
    }

    fun openPlaylistByCustomBrowseId(browseIdInput: String) {
        val cleanId = browseIdInput.trim()
        if (cleanId.isBlank()) return
        val normalizedId = if (cleanId.startsWith("PL")) "VL$cleanId" else cleanId
        val seed = MusicItem(
            id = normalizedId,
            title = "Memuat Playlist...",
            subtitle = "YouTube Music",
            thumbnailUrl = "",
            browseId = normalizedId,
            type = if (normalizedId.startsWith("MPREb_")) "album" else "playlist"
        )
        openPlaylistOrAlbumDetail(seed)
    }

    fun closePlaylistDetail() {
        _playlistScreenState.update { it.copy(isVisible = false, isSearchVisible = false, searchQuery = "") }
    }

    fun onTogglePlaylistSearch() {
        _playlistScreenState.update {
            val nextVisible = !it.isSearchVisible
            it.copy(
                isSearchVisible = nextVisible,
                searchQuery = if (nextVisible) it.searchQuery else ""
            )
        }
    }

    fun onUpdatePlaylistSearchQuery(query: String) {
        _playlistScreenState.update { it.copy(searchQuery = query) }
    }

    fun onTogglePlaylistSort() {
        _playlistScreenState.update { it.copy(sortAscending = !it.sortAscending) }
    }

    fun onPlayAllFromPlaylist() {
        val playlist = _playlistScreenState.value.playlist ?: return
        val firstTrack = playlist.tracks.firstOrNull() ?: return
        playSingleOrQueueTrack(
            item = firstTrack,
            contextQueue = playlist.tracks,
            sourceLabel = "Playing from ${playlist.title}"
        )
    }

    fun onShufflePlayPlaylist() {
        val playlist = _playlistScreenState.value.playlist ?: return
        if (playlist.tracks.isEmpty()) return
        val shuffled = playlist.tracks.shuffled()
        playSingleOrQueueTrack(
            item = shuffled.first(),
            contextQueue = shuffled,
            sourceLabel = "Playing from ${playlist.title}"
        )
    }

    fun playSingleOrQueueTrack(
        item: MusicItem,
        contextQueue: List<MusicItem> = emptyList(),
        sourceLabel: String? = null
    ) {
        val effectiveSource = sourceLabel ?: playerController.playerState.value.playingFromSource
        val effectiveQueue = if (contextQueue.isNotEmpty()) contextQueue else listOf(item)

        playerController.playTrackWithQueue(
            track = item,
            queue = effectiveQueue,
            sourceLabel = effectiveSource,
            resolveStream = { track -> repository.resolveAudioStream(track) },
            onTrackStarted = { startedTrack ->
                enrichTrackPlaybackContext(startedTrack)
            }
        )
    }

    private fun enrichTrackPlaybackContext(item: MusicItem) {
        val preferred = _selectedLyricsProvider.value
        _currentLyrics.value = null

        trackEnrichmentJob?.cancel()
        trackEnrichmentJob = viewModelScope.launch {
            launch {
                repository.recordPlayedTrack(item)
            }

            val lyricsDeferred = async { repository.getTrackLyrics(item, preferredProvider = preferred) }
            val queueDeferred = async { repository.getTrackQueue(item) }

            val fetchedLyrics = lyricsDeferred.await()
            _currentLyrics.value = fetchedLyrics

            val fetchedQueue = queueDeferred.await()
            if (fetchedQueue.isNotEmpty()) {
                playerController.setAutoPlayQueue(fetchedQueue)
            }
        }
    }

    fun onSelectTrackFromQueue(item: MusicItem) {
        playerController.playTrackFromQueueOrAutoPlay(item)
    }

    fun onRemoveTrackFromQueue(item: MusicItem) {
        playerController.removeTrackFromQueue(item.id)
    }

    fun toggleFavoriteTrack(item: MusicItem) {
        viewModelScope.launch {
            val wasFav = favoriteIds.value.contains(item.id) ||
                (item.videoId != null && favoriteIds.value.contains(item.videoId))
            repository.toggleFavoriteTrack(item)
            _uiMessage.value = if (wasFav) {
                "Dihapus dari Musik yang Disukai"
            } else {
                "Ditambahkan ke Musik yang Disukai"
            }
        }
    }

    fun onOpenTrackMenu(item: MusicItem) {
        _selectedTrackForMenu.value = item
    }

    fun onDismissTrackMenu() {
        _selectedTrackForMenu.value = null
    }

    fun onRemoveFromRecents(item: MusicItem) {
        viewModelScope.launch {
            repository.removeRecentTrack(item.id)
            _selectedTrackForMenu.value = null
        }
    }

    fun onClearAllRecents() {
        viewModelScope.launch {
            repository.clearAllRecents()
        }
    }

    fun onTogglePlayPause() {
        playerController.togglePlayPause()
    }

    fun onNextTrack() {
        playerController.playNext()
    }

    fun onPreviousTrack() {
        playerController.playPrevious()
    }

    fun onSeekTo(progress: Float) {
        playerController.seekToFraction(progress)
    }

    fun onVolumeChange(volume: Float) {
        playerController.setVolume(volume)
    }

    fun onVolumeStep(raise: Boolean) {
        playerController.adjustDeviceVolumeStep(raise)
    }

    fun onSyncDeviceVolume() {
        playerController.syncVolumeFromDevice()
    }

    fun onToggleShuffle() {
        playerController.toggleShuffle()
    }

    fun onToggleRepeat() {
        playerController.toggleRepeat()
    }

    fun onToggleAutoPlay() {
        playerController.toggleAutoPlay()
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        const val LIKED_SONGS_PLAYLIST_ID = "liked_songs_playlist"
        const val DOWNLOADS_PLAYLIST_ID = "offline_downloads_playlist"
        const val LOCAL_MUSIC_PLAYLIST_ID = "local_device_music"
    }
}
