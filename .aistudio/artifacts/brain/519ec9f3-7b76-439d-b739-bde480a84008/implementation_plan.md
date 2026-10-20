# Rencana Perbaikan & Redesign Profil Artis, Perpustakaan (Library), Detail Playlist, Video Player, & Impor Spotify

Merombak dan memperbaiki seluruh poin yang Anda sampaikan beserta kesesuaian penuh dengan 5 foto referensi yang Anda lampirkan (`Screenshot_20261020_163310`, `164343`, `165415`, `165435`, dan `165442`).

## User Review & Critical Decisions

> [!IMPORTANT]
> **Cakupan Pembaruan Utama:**
> 1. **Halaman Profil Artis (`ArtistProfileScreen`)**:
>    - **Tentang Artis**: Menghilangkan kotak ber-border dan *overlay* bertumpuk yang kaku; diganti dengan tata letak editorial bersih menyatu dengan latar halaman.
>    - **Menampilkan [Artis] (Playlist Kurasi & Radio)**: Memperbaiki aksi klik agar setiap kartu playlist kurasi langsung membuka halaman detail playlist beserta daftar lagunya.
>    - **Video Musik & Mode Video di Music Player**: Memperbaiki *thumbnail* video yang kosong pada profil artis. Saat diputar di Music Player, tampilannya menggunakan gambar sampul terlebih dahulu dan dapat diubah ke **Mode Video Full** melalui **menu titik 3 (`⋮`) di Music Player** (video langsung terputar penuh di area sampul).
>    - **Album & Single**: Menghapus garis batas kotak luar dan menghapus tombol *Play* di atas sampul; gambar sampul tampil *full* dengan *ambient blur/opacity* halus yang menyatu di bawahnya (cukup klik kartu untuk membuka/memutar).
>    - **Subscribe Artis Masuk ke Library**: Artis yang di-*subscribe* otomatis masuk ke daftar **Artis** di halaman **Perpustakaan (Library)** lengkap dengan foto profil aslinya.
> 2. **Halaman Perpustakaan (`LibraryScreen`)**:
>    - **Username Default `"user"` (Bisa Diklik & Diganti)**: Mengganti `"QIAA"` menjadi `"user"` secara default. Jika diklik pada kartu statistik, muncul dialog untuk mengganti nama pengguna.
>    - **Total Plays & Akumulasi Putaran Lagu Teratas (`X Plays`)**: Menghitung total berapa kali Anda mendengarkan musik (`X Plays • 2026`) dan menghitung akumulasi berapa kali lagu teratas (*Top Song*, mis. *About You*) telah diputar (`Artis • X Plays`).
>    - **Anggota Sejak (`ANGGOTA SEJAK`)**: Menggunakan tanggal asli pertama kali aplikasi di-*install* dan dibuka di perangkat.
>    - **Seksi `Artis` Bulat dengan Foto Profil Asli & Hapus `Podcasts`**: Menampilkan avatar bulat (*circular*) foto profil asli artis (bukan gambar kolase playlist) dan **menghapus seksi `Podcasts` sepenuhnya**.
> 3. **Notifikasi Download di Status Bar HP**:
>    - Memastikan izin notifikasi Android aktif dan menampilkan *progress bar* beserta persentase (`%`) unduhan secara langsung di panel **Notifikasi / Status Bar** atas HP.
> 4. **Detail Playlist & Menu Titik 3 (`...`) Sesuai Screenshot Referensi**:
>    - **Tampilan Detail Playlist (`Screenshot_20261020_165435`)**: Gambar sampul playlist tampil penuh di bagian atas dengan efek *blur* dan *opacity fade* tanpa garis batas yang menyatu ke warna latar belakang adaptif, disertai 4 tombol aksi bulat (`Shuffle`, `Play` putih besar, `Search`, dan `...`).
>    - **Menu Titik 3 (`...`) Playlist (`Screenshot_20261020_165442`)**: Menyediakan seluruh menu persis sesuai foto referensi dan semuanya berfungsi:
>      - **`Play next`**
>      - **`Add to queue`**
>      - **`Download all`**
>      - **`Share`**
>      - **`Pin`**
>      - **`Rename`**
>      - **`Reorder songs`** (ubah urutan lagu di dalam playlist)
>      - **`Delete playlist`**
> 5. **Perbaikan Impor Playlist Spotify (Semua Lagu + Progress Bar Berhasil/Gagal + Gambar Asli per Lagu)**:
>    - Memindai **seluruh lagu** dalam playlist Spotify (tanpa batas 25 lagu) menggunakan *parser* HTML/JSON + API embed & halaman publik.
>    - Menampilkan **Progress Bar & Persentase (`%`) Pemindaian** secara *real-time* yang menampilkan: total lagu terdeteksi, jumlah lagu **Berhasil (`✓`)**, jumlah lagu **Gagal (`✕`)**, dan lagu yang sedang diproses.
>    - Mencari setiap lagu ke YouTube Music saat proses impor agar masing-masing lagu memiliki **gambar sampul musik aslinya sendiri** (bukan memakai gambar sampul playlist).

---

## 1. Arsitektur & Alur Pembaruan

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   1. ArtistProfileScreen & Video Player                    │
├────────────────────────────────────────────────────────────────────────────┤
│ • Album & Single: Full-bleed cover + soft opacity/blur fade, tanpa tombol  │
│ • Video Section: High-res YouTube thumbnail + Switch to Video di Player ⋮  │
│ • Menampilkan Artis: Klik langsung buka & resolve PlaylistDetailScreen     │
│ • Tentang Artis: Editorial typography bersih tanpa box overlay kaku        │
│ • Subscribe: Simpan foto profil & data artis ke tabel Library Artists      │
└────────────────────────────────────────────────────────────────────────────┘
                                      │
┌────────────────────────────────────────────────────────────────────────────┐
│              2. LibraryScreen & Statistik Mendengarkan Akurat              │
├────────────────────────────────────────────────────────────────────────────┤
│ • Username: Default "user", klik untuk ubah nama (tersimpan permanen)      │
│ • Play Counter: Hitung total plays semua lagu & akumulasi plays Top Song   │
│ • Anggota Sejak: Tanggal pertama kali install / buka aplikasi              │
│ • Artis Library: Avatar bulat foto profil asli artis (Subscribed + Library)│
│ • Podcasts: Dihapus sepenuhnya dari halaman Library                        │
└────────────────────────────────────────────────────────────────────────────┘
                                      │
┌────────────────────────────────────────────────────────────────────────────┐
│         3. PlaylistDetailScreen, Menu Titik 3 & Spotify Importer           │
├────────────────────────────────────────────────────────────────────────────┤
│ • Hero Cover: Full-width atas + seamless blur/opacity gradient ke warna UI │
│ • Menu (...): Play next, Add to queue, Download all, Share, Pin, Rename,   │
│               Reorder songs (geser naik/turun), Delete playlist            │
│ • Spotify Import: Scan semua lagu + Progress Bar % (Berhasil vs Gagal)     │
│                   + Resolve cover image asli untuk setiap lagu             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Rincian Perubahan per Komponen

1. **`ArtistProfileScreen.kt` & `YtMusicScraper.kt`**:
   - Merombak `FeaturedWideDiscographyCard` dan `EditorialVinylReleaseCard` pada seksi **Album & Single** agar gambar sampul memenuhi kartu dengan gradasi *blur/opacity* menyatu di bagian bawah dan tanpa ikon tombol *Play*.
   - Memastikan seksi **Video** selalu menghasilkan URL thumbnail YouTube valid (`https://i.ytimg.com/vi/{videoId}/hqdefault.jpg`) dari `videoId`.
   - Memperbaiki handler `onOpenReleaseOrPlaylist` untuk item di seksi **Menampilkan [Artis]** agar apabila playlist kurasi diklik, `MusicHomeViewModel` memuat detail playlist (atau mencarikan daftar lagu kurasi artis tersebut secara otomatis) dan menampilkan `PlaylistDetailScreen`.
   - Merombak `ArtistAboutBioSection` (**Tentang [Artis]**) menjadi bagian tipografi editorial terbuka tanpa kartu ber-border kaku.

2. **`FullMusicPlayerScreen.kt` & `UniversalTrackMenuBottomSheet`**:
   - Menambahkan opsi **"Tampilkan Video Musik" / "Kembali ke Sampul Audio"** pada menu titik 3 (`⋮`) di pemutar musik.
   - Saat mode video diaktifkan, area *thumbnail* pemutar musik berubah menjadi *full-bleed video player* yang langsung memutar video musik lagu tersebut secara otomatis.

3. **`LibraryScreen.kt`, `AppDatabase.kt`, & `MusicHomeViewModel.kt`**:
   - Mengatur default username menjadi `"user"` dan menambahkan dialog **Ubah Username** saat teks username pada kartu *Pengalaman Mendengarkan Anda* diketuk.
   - Menyimpan penghitung `playCount` per lagu dan total putaran keseluruhan di database lokal, sehingga setiap kali lagu diputar angka `X Plays • 2026` dan `Artis • X Plays` pada kartu *Top Song* bertambah secara akurat.
   - Mengambil tanggal instalasi aplikasi (`PackageManager.getPackageInfo(...).firstInstallTime`) untuk ditampilkan pada **`ANGGOTA SEJAK`**.
   - Menyimpan artis yang di-*subscribe* beserta URL foto profil aslinya, dan menampilkan seksi **`Artis`** di Library dalam bentuk **avatar bulat (*circle*)** dengan foto profil asli artis.
   - Menghapus seksi **`Podcasts`** dari `LibraryScreen.kt`.

4. **`PlaylistDetailScreen.kt` (Tampilan Blur Menyatu & Menu Titik 3 Lengkap)**:
   - Menyamakan tampilan seluruh playlist (termasuk playlist di Library) dengan desain `Screenshot_20261020_165435`: gambar sampul besar di bagian atas (`fillMaxWidth`) yang menyatu secara halus menggunakan gradasi *opacity* & *blur* ke warna latar adaptif, judul tebal di tengah, subjudul `user, X lagu` berwarna aksen, `PLAYLIST • X TRACKS`, dan 4 tombol bulat (`Shuffle`, `Play`, `Search`, `...`).
   - Mengimplementasikan *bottom sheet* menu titik 3 (`...`) persis seperti `Screenshot_20261020_165442`:
     - `Play next`, `Add to queue`, `Download all`, `Share`, `Pin`, `Rename`, `Reorder songs` (mode urutkan lagu naik/turun), dan `Delete playlist`.

5. **`PlaylistIntegrationService.kt` & `SettingsAndIntegrationsScreen.kt` (Impor Spotify + Progress Bar + Gambar Lagu Asli)**:
   - Menghapus batas 25 lagu pada `scrapeSpotifyPlaylistUrl` dan `fetchSpotifyPlaylistTracksWithToken` serta menambahkan ekstraksi daftar lagu lengkap dari halaman Spotify.
   - Mencocokkan setiap lagu hasil ekstraksi ke YouTube Music (`scraper.searchSongs`) untuk mendapatkan **gambar sampul asli masing-masing lagu** dan `videoId` audionya, sambil memperbarui `SpotifyImportProgressState` (Total lagu, Progress `%`, jumlah **Berhasil**, jumlah **Gagal**, dan judul lagu saat ini) yang ditampilkan dengan *progress bar* interaktif di layar.

6. **`MusicDownloadManager.kt` & `MainActivity.kt` (Notifikasi Bar Sistem HP)**:
   - Meminta izin runtime `POST_NOTIFICATIONS` (untuk Android 13+) dan memastikan `NotificationManagerCompat` menampilkan *progress bar* persentase (`%`) unduhan langsung di **Notification Bar / Status Bar** atas perangkat Android.
