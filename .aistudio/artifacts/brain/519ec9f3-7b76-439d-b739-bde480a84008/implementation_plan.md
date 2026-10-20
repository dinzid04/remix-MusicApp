# BitChord — Artist Profile Thumbnail Fixes, Player 3-Dot Menu & Sleep Timer Redesign, and Library Artist Portraits

This update resolves missing thumbnails across the Artist Profile (Video section, "Penggemar mungkin juga suka", and tracks inside "Menampilkan" curated playlists), overhauls the Music Player 3-dot (`•••`) options sheet and Sleep Timer overlay to match your reference screenshots 1:1 with complete functional actions, and ensures every artist in the Library **Artis** section displays their real scraped YouTube Music circular portrait instead of a shared default fallback image.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions incorporate your confirmed answers from our design & scope alignment alongside your reference screenshots and `ytm.mjs` scraper specification:

- **Confirmed Decision 1 — Menu & Sleep Timer Language (`English labels matching reference screenshots`)**: Both the Music Player 3-dot (`•••`) overlay menu and the Sleep Timer modal will use the exact English labels and layout structure from your reference screenshots (`Like`, `Add to playlist`, `Play next`, `Add to queue`, `Open artist`, `Sleep timer`, and `10m`, `15m`, `30m`, `1h`, `End of track`, `Cancel`).
- **Confirmed Decision 2 — Library Artis Section Scope & Real Scraped Avatars (`Subscribed artists plus played and saved artists`)**: The Library **Artis** carousel will include your subscribed artists as well as artists from your played history and saved playlists, and will dynamically resolve and persist each artist's real circular profile portrait directly from YouTube Music (`UC...` channel/artist thumbnails) rather than falling back to a single hardcoded image.
- **Confirmed Decision 3 — Zero Double-Dimming on Player Overlays**: Because the Music Player already renders a rich full-screen blurred artwork atmosphere, opening the 3-dot menu or transforming it into the Sleep Timer view will **not** stack an extra dark bottom-sheet backdrop on top of the screen; instead, it renders a clean floating translucent glass panel directly over the blurred player background.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Fixes Blank / Black Video & Artist Thumbnails on Artist Profile**: Aligns the YouTube Music InnerTube parser with `ytm.mjs` (`extractThumbnail` & `parseCarouselItem`) so music videos (`musicResponsiveListItemRenderer` and `musicTwoRowItemRenderer` with `VIDEO` / `OMV` / `UGV`), related artists ("Penggemar mungkin juga suka"), and curated playlists ("Menampilkan...") extract every nested thumbnail path and never corrupt non-square `i.ytimg.com/vi/...` URLs with invalid `=w544-h544` suffixes that return HTTP 404.
  2. **Fixes Missing Track Thumbnails Inside Artist Featured Playlists**: When opening a playlist or album from "Menampilkan [Artist]", tracks whose individual renderer omits a thumbnail or uses a signed `i.ytimg.com` query string now preserve valid URLs, fall back to `https://i.ytimg.com/vi/{videoId}/hqdefault.jpg` when `videoId` is present, or inherit the parent playlist/album artwork so zero tracks render blank black boxes.
  3. **1:1 Music Player 3-Dot (`•••`) Floating Glass Menu & Sleep Timer**: Replaces the old bottom sheet with the exact floating frosted-glass layout from your screenshots, featuring the current song header (`Title`, `Artist • Album`) and interactive rows for **Like**, **Add to playlist**, **Play next**, **Add to queue**, **Open artist**, and **Sleep timer** (plus quick access to Video Mode / Offline Download / HD Cover). Tapping **Sleep timer** smoothly transitions to the centered **Sleep timer** glass card (`10m`, `15m`, `30m`, `1h`, `End of track`, and `Cancel`) without adding a second dark overlay.
  4. **Authentic Per-Artist Profile Photos in Library**: Eliminates the shared fallback avatar bug where multiple played/saved artists displayed the same photo, by caching and resolving each artist's real YouTube Music channel portrait (`UC...` / `lh3.googleusercontent.com` / `yt3.ggpht.com`) in the background and persisting it across sessions.
- **Target Audience / Persona**: Music listeners who want a seamless, authentic YouTube Music-powered experience with zero broken thumbnails, responsive queue/timer controls, and accurate artist imagery across their personal Library.
- **Key Value**: Eliminates visual bugs (black thumbnail boxes and duplicate artist avatars) and delivers a cohesive, human-crafted glassmorphic player menu and sleep timer experience.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Viewing an Artist Profile (`Video`, `Penggemar mungkin juga suka`, `Menampilkan...`)**:
   - Scrolling to **Video** displays crisp 16:9 video thumbnails for every music video (using extracted `thumbnailRenderer` or deterministic `https://i.ytimg.com/vi/{videoId}/hqdefault.jpg`).
   - Scrolling to **Penggemar mungkin juga suka** displays each related artist's real circular avatar (`lh3.googleusercontent.com` / `yt3.ggpht.com`). If a shelf item lacks a direct thumbnail in the initial browse payload, an asynchronous background resolver fetches and caches the artist's real avatar by their `UC...` browseId.
   - Tapping any playlist or radio in **Menampilkan [Artist]** opens the Playlist Detail view with 100% populated track thumbnails—every track displays its real song cover or video frame without any dark empty squares.

2. **Using the Music Player 3-Dot (`•••`) Menu & Sleep Timer**:
   - Tapping the `•••` button on the Full Music Player opens a floating translucent glass overlay over the player's existing blurred artwork background (no extra dark scrim layer).
   - **Top Header**: Displays the 52dp rounded album cover on the left, bold white track title, and `Artist • Album` subtitle on the right, followed by a subtle 1dp translucent divider.
   - **Action List**:
     - **Like** (Heart icon — toggles favorite status immediately with active state feedback)
     - **Add to playlist** (Plus icon — opens the Add to Playlist selector)
     - **Play next** (`PlaylistPlay` icon — inserts the track immediately after the currently playing song in the active queue)
     - **Add to queue** (`QueueMusic` icon — appends the track to the end of the active playback queue)
     - **Open artist** (Artist icon — closes the player overlay and navigates directly to the artist's profile page)
     - **Sleep timer** (Moon icon — shows active countdown badge if running, and switches the glass card into the Sleep Timer picker)
     - **Switch to Video / Audio & Offline Download** (Clean secondary actions below a subtle divider so existing video/download capabilities remain accessible)
   - **Sleep Timer View**:
     - Replaces the menu content in-place on the same blurred backdrop (zero double-overlay dimming) with a centered **Sleep timer** card.
     - Displays a 2-column grid of rounded glass pill buttons: **10m**, **15m** (row 1), **30m**, **1h** (row 2), a full-width **End of track** pill button (row 3), and a centered **Cancel** action below.
     - Selecting any timer option starts a live countdown in the playback controller that automatically pauses playback when the timer expires (or when the current track finishes if **End of track** is selected).

3. **Browsing the Library `Artis` Section**:
   - Whenever you subscribe to an artist, play a track by an artist, or save tracks to a playlist, the artist appears in the **Artis** section of the Library.
   - Each artist card in the Library resolves and caches the artist's genuine YouTube Music profile portrait (matching the portrait shown when opening their Artist Profile page) instead of reusing a single static fallback image or a song's album cover.

### Visual Identity & Theme
- **Aesthetic Direction**: Refined dark glassmorphism harmonized with dynamic album artwork color extraction.
- **Color Palette & Mood**:
  - Floating Menu & Sleep Timer Card Surface: Translucent frosted glass (`Color(0xFF221E24).copy(alpha = 0.78f)` blended with the active track's ambient palette) with a `1.dp` subtle specular border (`Color.White.copy(alpha = 0.14f)`).
  - Pill Buttons (Sleep Timer): `Color.White.copy(alpha = 0.12f)` fill with `16.dp` corner radius, crisp white `PlusJakartaSans` Bold typography (`15.sp`), and highlighted border when active.
  - Primary Text: `#FFFFFF` (`FontWeight.Bold` / `ExtraBold`); Secondary Subtitle Text: `Color.White.copy(alpha = 0.68f)`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Preserving Valid `i.ytimg.com` & Google Content Thumbnail URLs**
  - *Chosen Approach*: Update `upgradeThumbnailUrl` and `extractThumbnails` so that Google user content (`lh3.googleusercontent.com`, `yt3.ggpht.com`, `yt3.googleusercontent.com`) is upgraded cleanly to `=w544-h544-l90-rj`, while `i.ytimg.com/vi/...` URLs strip signed query strings (`?sqp=...`) that expire or fail on resize and use reliable `/hqdefault.jpg` or `/maxresdefault.jpg` paths. Furthermore, any item with a `videoId` automatically falls back to `https://i.ytimg.com/vi/{videoId}/hqdefault.jpg` if its renderer thumbnail array is empty.
  - *Why*: YouTube Music returns signed `i.ytimg.com/vi/.../sddefault.jpg?sqp=...` URLs for videos and playlist tracks. Appending or mutating those URLs or leaving empty thumbnail arrays untouched caused Coil requests to fail and render black boxes.
  - *Alternatives Considered*: Using static placeholder icons on failure — rejected because real thumbnails are available via the video ID and InnerTube thumbnail renderers.

- **Decision 2: In-Place Glass Overlay Transition for 3-Dot Menu & Sleep Timer**
  - *Chosen Approach*: Render the 3-dot menu and Sleep Timer as custom animated glass overlays inside `FullMusicPlayerScreen` rather than stacking standard Material `ModalBottomSheet` scrims.
  - *Why*: `ModalBottomSheet` adds its own dark black scrim over the entire screen, which dulls the player's blurred artwork background and creates a double-overlay look when switching to the Sleep Timer.

- **Decision 3: Live Artist Avatar Resolution & Caching for Library Artists**
  - *Chosen Approach*: Maintain a persistent `artistAvatarCache: Map<String, String>` in preferences/state. When an artist is added via playback or library actions without a dedicated `UC...` channel portrait, a lightweight background lookup queries YouTube Music (`searchArtists` / `getArtistDetails` from `ytm.mjs` logic) by the artist's name/browseId, extracts their authentic circular portrait, and updates the Library **Artis** carousel automatically.
  - *Why*: Track objects only carry their song/album cover URL, not the artist's circular channel portrait. Resolving and caching the artist's real channel avatar guarantees every artist in the Library displays their own distinct profile photo.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        YouTube Music InnerTube Layer                       │
│  ┌──────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │ YtMusicScraper (ytm.mjs aligned) │  │ Thumbnail URL Normalizer       │  │
│  │ • extractThumbnails (deep paths) │  │ • lh3/yt3 -> =w544-h544-l90-rj │  │
│  │ • Video / Artist / Playlist fix  │  │ • i.ytimg -> /vi/{id}/hqdefault│  │
│  └───────────────┬──────────────────┘  └───────────────┬────────────────┘  │
└──────────────────┼─────────────────────────────────────┼───────────────────┘
                   ▼                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   MusicHomeViewModel & AudioPlayerController               │
│  ┌─────────────────────────┐  ┌────────────────────┐  ┌─────────────────┐  │
│  │ Queue & Track Actions   │  │ Sleep Timer Engine │  │ Artist Avatar   │  │
│  │ • toggleFavorite(track) │  │ • 10m/15m/30m/1h   │  │ Resolver &      │  │
│  │ • playNextInQueue(track)│  │ • End of track     │  │ Persistent Cache│  │
│  │ • addToQueue(track)     │  │ • Cancel timer     │  │ (per artist ID) │  │
│  └───────────┬─────────────┘  └─────────┬──────────┘  └────────┬────────┘  │
└──────────────┼──────────────────────────┼──────────────────────┼───────────┘
               ▼                          ▼                      ▼
┌────────────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐
│   FullMusicPlayerScreen    │  │ ArtistProfile &     │  │   LibraryScreen   │
│ • Floating 3-Dot Glass Menu│  │ PlaylistDetail      │  │ • Artis Carousel  │
│ • In-Place Sleep Timer Card│  │ • 16:9 Video Thumbs │  │   (Subscribed +   │
│   (No extra scrim overlay) │  │ • Related Avatars   │  │    Played/Saved   │
│                            │  │ • Full Track Covers │  │    Real Portraits)│
└────────────────────────────┘  └─────────────────────┘  └───────────────────┘
```

### Data Model & State
- **Sleep Timer State (`SleepTimerMode` & `sleepTimerRemainingMs`)**:
  - Added to `PlayerUiState`: `sleepTimerLabel: String?` (e.g., `"15m"`, `"End of track"`) and `sleepTimerEndOfTrack: Boolean`.
  - Managed in `MusicHomeViewModel` via a coroutine ticker job that decrements remaining time every second and invokes `audioController.pause()` when reaching zero, or pauses automatically inside the track-completion listener when `sleepTimerEndOfTrack == true`.
- **Artist Avatar Cache (`artistAvatarMap: Map<String, String>`)**:
  - Keyed by normalized lowercase artist name (`artistName.trim().lowercase()`).
  - Populated immediately whenever:
    1. An artist profile page is viewed (`ArtistProfileData.avatarUrl`).
    2. Related artists ("Penggemar mungkin juga suka") or search results are parsed.
    3. A background coroutine resolves any artist in `libraryArtists` whose avatar is missing or not yet cached.

### Interactive Component & State Mapping
- **3-Dot Menu Overlay (`PlayerMoreOptionsGlassOverlay`)**:
  - `Like` -> Calls `onToggleFavorite(track)` and updates heart icon tint/fill in real time.
  - `Add to playlist` -> Dismisses menu and opens `AddToPlaylistBottomSheet`.
  - `Play next` -> Calls `onPlayNext(track)` to insert `track` at `currentIndex + 1` in `AudioPlayerController`'s queue and shows confirmation feedback.
  - `Add to queue` -> Calls `onAddToQueue(track)` to append `track` to the end of `AudioPlayerController`'s queue and shows confirmation feedback.
  - `Open artist` -> Closes player and triggers `onOpenArtistProfile(track.cleanArtist, "")` (passing empty fallback cover so the artist profile uses real scraped artist imagery).
  - `Sleep timer` -> Transitions `overlayStep` from `MENU` to `SLEEP_TIMER` in-place on the same blurred backdrop.
- **Sleep Timer Overlay (`SleepTimerGlassCard`)**:
  - `10m`, `15m`, `30m`, `1h` -> Starts countdown for `10 / 15 / 30 / 60` minutes and closes the overlay.
  - `End of track` -> Arms end-of-track auto-pause and closes the overlay.
  - `Cancel` -> Cancels any active sleep timer (if running) and closes the overlay.
