# Perombakan 100% Desain Halaman Perpustakaan (Library) Sesuai Screenshot Referensi

Memperbaiki dan merombak total antarmuka halaman **Perpustakaan (Library)** agar **100% identik** dengan dua foto desain referensi yang Anda kirimkan (`Screenshot_20261019_205838_BitChord` dan `Screenshot_20261019_205256_BitChord`), baik dari segi proporsi kartu, warna, ikon, tipografi tebal, maupun alur pengelolaan Playlist, Artis, dan Podcasts yang ditambahkan ke Library pengguna.

## User Review & Critical Decisions

> [!IMPORTANT]
> Seluruh elemen pada halaman **Perpustakaan** akan disesuaikan persis mengikuti tata letak pada foto desain referensi Anda:
> - **Kartu Statistik Besar Horizontal (`PENGALAMAN MENDENGARKAN ANDA`)**: Dua kartu besar bergaya *dark metallic* yang dapat digeser ke samping, lengkap dengan logo 3 garis gelombang di kanan atas, angka besar `MINUTES LISTENED` / `TOP SONG`, serta footer `QIAA • Plays • 2026` dan `ANGGOTA SEJAK 10/26`.
> - **Baris Menu `Downloads` & `Local Music`**: Menggunakan baris daftar (*full-width list row*) dengan ikon di kiri dan tanda panah (`>`) di kanan (bukan kotak 2 kolom).
> - **Seksi `Playlist` (Karosel Kartu Kotak Besar + `Lihat semua`)**: Deretan kartu persegi besar (`148dp`) berurutan: **`Playlist baru` (+)**, **`Impor dari Spotify` (logo Spotify)**, **`Musik yang Disukai` (gradasi ungu-pink dengan ikon jempol/ThumbUp putih besar)**, diikuti kartu-kartu playlist milik pengguna.
> - **Bottom Sheet `New playlist`**: Mengikuti persis tampilan pada screenshot referensi (`New playlist`, `Saved to your YouTube Music account`, input `Playlist name`, pilihan chip `WHO CAN SEE IT`: `Private`, `Unlisted`, `Public`, dan tombol penuh `Create playlist`).
> - **Seksi `Artis` & `Podcasts` Berdasarkan Koleksi Library Sendiri**: Menampilkan kartu persegi foto artis dari lagu/artis yang disimpan pengguna ke Library (lengkap dengan jumlah `"X lagu"`), serta kartu **`Episode untuk Nanti`** (gradasi pink-ungu-biru dengan ikon mikrofon podcast) beserta podcast yang disimpan ke Library.

---

## 1. Overview & Core Concept

- **What It Does**: Menghadirkan halaman **Perpustakaan (Library)** dengan tata letak karosel kartu besar dan daftar yang persis seperti desain asli BitChord pada screenshot referensi Anda, sekaligus mempertahankan fitur manajemen playlist (Buat, Impor, Pin, Rename, Tambah Lagu, Delete) dan Download Offline.
- **Target Audience / Persona**: Pengguna BitChord yang menginginkan tampilan Perpustakaan gelap (*OLED Black*) yang rapi, proporsional, berfont Sans-Serif tebal, dan 100% sesuai desain referensi.
- **Key Value**: Tampilan visual yang presisi sesuai ekspektasi desain tanpa mengubah kemudahan pengelolaan playlist, artis, podcast, dan unduhan musik offline.

---

## 2. User Experience & Visual Design

### Tata Letak Vertikal Halaman `Perpustakaan` (100% Sesuai Screenshot)

1. **Header Atas & Kapsul Kanan Atas**:
   - Di pojok kanan atas terdapat kapsul elips gelap (`#161618` dengan border tipis `#2A2A2E`) yang memuat **Ikon Riwayat Jam (`History`)** di sebelah kiri dan **Avatar Kucing** di sebelah kanan.
   - Di bawahnya terdapat judul utama **`Perpustakaan`** berukuran besar (`34sp`, `ExtraBold`/`Black`, putih bersih `#FFFFFF`).

2. **Karosel Kartu Statistik `PENGALAMAN MENDENGARKAN ANDA`**:
   - Dua kartu horizontal berukuran besar (`lebar ~236dp`, `tinggi 168dp`, sudut membulat `24dp`, gradasi *dark charcoal* `#2B2B2E` ke `#18181A`):
     - **Kartu 1 (Minutes Listened)**:
       - Kiri atas: `PENGALAMAN MENDENGARKAN ANDA` (huruf kapital, `11sp`, abu-abu terang).
       - Kanan atas: Ikon 3 gelombang khas aplikasi.
       - Tengah: Angka besar (mis. `0` atau jumlah menit aktual, `32sp`, `ExtraBold`) + label `MINUTES LISTENED` di bawahnya.
       - Bawah kiri: `QIAA` dan `X Plays • 2026`.
       - Bawah kanan: `ANGGOTA SEJAK` dan `10/26`.
     - **Kartu 2 (Top Song)**:
       - Header yang sama (`PENGALAMAN MENDENGARKAN ANDA` + ikon gelombang).
       - Tengah: Judul lagu teratas (mis. `About You` atau lagu yang paling sering diputar, `26sp`, `ExtraBold`) + label `TOP SONG`.
       - Bawah kiri: `QIAA` dan `Nama Artis • X Plays`.

3. **Baris Menu `Downloads` dan `Local Music`**:
   - Ditampilkan sebagai 2 baris daftar memanjang (*full-width row*) di atas latar hitam pekat:
     - **`Downloads`**: Ikon unduh di kiri, teks tebal `Downloads` (beserta badge progres jika sedang mengunduh), dan ikon panah kanan `>` di ujung kanan, dipisahkan garis tipis halus.
     - **`Local Music`**: Ikon folder di kiri, teks tebal `Local Music`, dan ikon panah kanan `>` di ujung kanan.

4. **Seksi `Playlist` + Tombol `Lihat semua`**:
   - Baris judul: **`Playlist`** (`22sp`, `ExtraBold`) di kiri dan **`Lihat semua`** (`14sp`, `Bold`) di kanan (membuka tampilan daftar lengkap seluruh playlist untuk mengatur Pin/Rename/Delete dengan cepat).
   - Deretan kartu persegi besar (`148dp x 148dp`, `RoundedCornerShape(16.dp)`) yang dapat digeser ke kanan-kiri (`LazyRow`):
     1. **`Playlist baru`**: Kotak gelap `#18181B` dengan ikon `+` putih di tengah. Subtitle di bawahnya: `Disimpan ke YouTube ...`.
     2. **`Impor dari Spotify`**: Kotak gelap `#18181B` dengan logo lingkaran Spotify putih/abu-abu di tengah. Subtitle: `Dari tautan playlist`.
     3. **`Musik yang Disukai`**: Kotak bergradasi ungu-ke-pink (`#7059E6` ke `#D859B0`) dengan **ikon Jempol (`ThumbUp`) putih besar** di tengah. Subtitle: `Playlist otomatis`.
     4. **Kartu Playlist Pengguna & Hasil Impor**: Menampilkan sampul album/playlist, judul tebal di bawahnya, dan subtitle (`Qiaa` / jumlah lagu). Ketuk kartu untuk membuka isi playlist, atau tahan/klik menu untuk **Pin, Rename, Tambah Lagu, Download, dan Delete**.

5. **Bottom Sheet `New playlist` (Sesuai Screenshot 2)**:
   - Muncul dari bawah saat pengguna menekan kartu **`Playlist baru`**:
     - Handle bar kecil di tengah atas.
     - Judul **`New playlist`** (`22sp`, `ExtraBold`) dan subjudul **`Saved to your YouTube Music account`**.
     - Kotak input gelap membulat dengan placeholder **`Playlist name`**.
     - Label **`WHO CAN SEE IT`** diikuti 3 kapsul pilihan:
       - **`Private`** (ikon gembok, latar putih & teks hitam saat terpilih).
       - **`Unlisted`** (ikon rantai tautan).
       - **`Public`** (ikon bola dunia).
     - Tombol kapsul lebar di bagian bawah: **`Create playlist`**.

6. **Seksi `Artis` (Artis yang Ditambahkan / Disimpan ke Library Kita)**:
   - Judul seksi **`Artis`** (`22sp`, `ExtraBold`).
   - Deretan kartu persegi besar (`148dp x 148dp`, sudut `16dp`) berisi foto artis dari lagu-lagu yang ada di Library pengguna (lagu yang disukai, lagu di dalam playlist pengguna, atau artis yang diikuti), lengkap dengan nama artis tebal di bawahnya dan keterangan jumlah lagu di Library (mis. `Bravesboy • 1 lagu`, `Daun Jatuh • 1 lagu`, `Dido • 1 lagu`), plus kartu untuk menambahkan artis ke Library.

7. **Seksi `Podcasts` (Episode untuk Nanti & Podcast di Library Kita)**:
   - Judul seksi **`Podcasts`** (`22sp`, `ExtraBold`).
   - Kartu persegi besar (`148dp x 148dp`, sudut `16dp`) **`Episode untuk Nanti`** dengan gradasi pink-lavender-biru (`#F0A3B5` ke `#7B88FA`) dan ikon mikrofon podcast putih besar di tengah, subjudul `Episode dalam antrea...`, diikuti daftar podcast/episode yang disimpan pengguna ke Library.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Mengganti Layout List Vertikal menjadi Karosel Kotak Besar (`148dp`) Sesuai Referensi**
  - *Chosen Approach*: Menggunakan `LazyRow` untuk seksi `PENGALAMAN MENDENGARKAN ANDA`, `Playlist`, `Artis`, dan `Podcasts`, serta baris horizontal untuk `Downloads` dan `Local Music`.
  - *Why*: Menyamakan tampilan 100% dengan kedua screenshot referensi yang dikirimkan pengguna.
- **Decision 2: Tetap Menyediakan Semua Aksi Playlist (Pin, Rename, Delete, Add Music, Download) Melalui Kartu, Tombol `Lihat semua`, dan Detail Playlist**
  - *Chosen Approach*: Pengguna dapat mengetuk kartu playlist untuk membuka detailnya, menekan lama (*long-press*) atau menekan ikon opsi pada kartu playlist, maupun menekan **`Lihat semua`** di samping judul `Playlist` untuk mengelola seluruh playlist (Pin, Rename, Tambah Lagu, Download, Delete).
  - *Why*: Memastikan desain 100% bersih dan identik dengan foto referensi tanpa kehilangan satu pun fitur manajemen playlist yang diminta sebelumnya.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       LibraryScreen (OLED #000000)                      │
├─────────────────────────────────────────────────────────────────────────┤
│ 1. Top Pill Action Bar (History + Cat Avatar) + "Perpustakaan" Title    │
│ 2. Horizontal LazyRow: 2 Large Cards ("PENGALAMAN MENDENGARKAN ANDA")   │
│    ├── Card 1: MINUTES LISTENED + QIAA + 0 Plays•2026 + ANGGOTA 10/26   │
│    └── Card 2: TOP SONG ("About You") + QIAA + Artist • Plays           │
│ 3. Full-Width Rows: [Downloads >] & [Local Music >]                     │
│ 4. Section "Playlist" + "Lihat semua"                                   │
│    └── LazyRow (148dp Square Cards):                                    │
│        ├── [ + ] Playlist baru -> Opens "New playlist" Bottom Sheet     │
│        ├── [Spotify] Impor dari Spotify -> Opens Spotify Import Dialog  │
│        ├── [ThumbUp] Musik yang Disukai -> Opens Liked Songs            │
│        └── [Cover] User & Imported Playlists (Pin/Rename/Add/Delete)    │
│ 5. Section "Artis"                                                      │
│    └── LazyRow (148dp Square Artist Cards derived from Library tracks)  │
│ 6. Section "Podcasts"                                                   │
│    └── LazyRow (148dp "Episode untuk Nanti" Card + Saved Podcasts)      │
└─────────────────────────────────────────────────────────────────────────┘
```

- **State & Koleksi Library**:
  - **Artis di Library**: Dihitung secara reaktif dari lagu-lagu yang disimpan pengguna di Library (`favoriteTracks` + lagu di dalam `libraryPlaylists` + `followedArtists`), dikelompokkan berdasarkan nama artis untuk menampilkan foto artis dan jumlah `"X lagu"` milik artis tersebut di Library.
  - **Podcasts di Library**: Menyediakan playlist otomatis **`Episode untuk Nanti`** yang dapat dibuka, ditambahkan episode/audio dari pencarian atau menu lagu, serta diputar dan diunduh secara offline.
