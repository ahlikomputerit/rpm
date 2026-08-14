# PRD — Forest Depths

**Status:** Draft siap dipecah menjadi issue

**Versi:** 1.0

**Bahasa:** Bahasa Indonesia

**Working title:** Forest Depths

**Referensi pengalaman:** [MengTo/Kage](https://github.com/MengTo/kage), khususnya pendekatan interactive chapter walk, scene Three.js yang dirender live, cinematic generated imagery, foreground cutouts, camera choreography, fog, particles, dan scroll sebagai perjalanan ruang.

## 1. Ringkasan produk

Forest Depths adalah pengalaman web scroll-driven sinematik yang membawa pengguna berjalan semakin jauh ke dalam hutan tua. Perjalanan dimulai dari tepi hutan yang masih terang, melewati jalur pakis, sungai berkabut, cathedral of trees, rawa malam, dan berakhir di jantung hutan purba yang hampir tidak tersentuh manusia.

Produk ini bukan halaman landing page konvensional. Scroll diperlakukan sebagai kontrol kamera dan ritme narasi. Setiap chapter mengubah kedalaman ruang, kepadatan vegetasi, sumber cahaya, suara, cuaca, dan posisi kamera. Tujuan utamanya adalah menciptakan rasa penasaran: pengguna terus menggulir bukan karena dipaksa, melainkan karena ingin mengetahui apa yang berada di balik lapisan hutan berikutnya.

## 2. Masalah yang ingin diselesaikan

Banyak pengalaman web bertema alam berhenti pada satu hero image, parallax ringan, dan beberapa section teks yang terpisah. Pola tersebut menyampaikan tema tetapi tidak menciptakan perjalanan yang terasa hidup. Forest Depths harus menyatukan narasi, gambar, geometri 3D, audio, dan interaksi menjadi satu sistem lingkungan yang koheren.

Masalah teknis yang harus dihindari sejak awal adalah background yang tetap datar, 3D layer yang hanya menjadi dekorasi, asset generation yang gagal tetapi meninggalkan area kosong, audio yang diputar tanpa izin, bundle awal yang terlalu berat, serta pengalaman mobile yang kehilangan hierarchy.

## 3. Tujuan dan indikator keberhasilan

| Tujuan | Indikator keberhasilan |
|---|---|
| Membuat perjalanan hutan terasa nyata | Pengguna dapat merasakan perubahan jarak, cahaya, dan kepadatan lingkungan melalui scroll tanpa perlu membaca seluruh copy |
| Meniru prinsip Kage tanpa menyalin identitasnya | Scene live menggunakan Three.js, chapter memiliki camera path, dan foreground/background memiliki depth berbeda; branding, copy, dan visual Forest Depths harus orisinal |
| Mempertahankan first paint yang cepat | Image fallback tampil segera; Three.js dimuat lazy setelah first paint; WebGL tidak menjadi dependency untuk konten utama |
| Menjaga aksesibilitas | Navigasi keyboard, focus state, reduced-motion, mute audio, dan fallback non-WebGL tersedia sejak awal |
| Menjaga performa | Pixel ratio dibatasi, object asset dikompres, geometry modest, dan tidak ada error fatal di console |
| Membangun rasa penasaran | Setiap chapter memiliki curiosity beat, hidden detail, atau perubahan atmosfer yang mendorong pengguna melanjutkan scroll |

## 4. Target pengguna

Target utama adalah pengunjung desktop dan mobile yang menyukai pengalaman digital sinematik, interactive storytelling, creative coding, game environment, dan desain editorial. Pengguna tidak harus memahami Three.js. Pengalaman harus tetap bermakna bagi pengguna yang mematikan audio, menggunakan reduced motion, atau tidak memiliki WebGL.

## 5. User journey

Perjalanan menggunakan sembilan chapter agar durasi scroll cukup panjang tetapi tetap memiliki struktur yang mudah dipahami.

| Chapter | Zona | Narasi dan visual | Karakter kamera | Curiosity beat |
|---|---|---|---|---|
| 01 | Forest Edge | Cahaya pagi, jalan tanah, rumput basah, dan garis pepohonan pertama | Drift lateral yang tenang | Jejak kaki berakhir di batas kabut |
| 02 | Fern Passage | Pakis tinggi, serangga kecil, dan cahaya terpecah | Weaving rendah di antara pakis | Sesuatu bergerak di luar fokus foreground |
| 03 | Moss Creek | Sungai dangkal, batu licin, akar terbuka, dan suara air | Follow path mengikuti aliran sungai | Riak air muncul tanpa sumber yang terlihat |
| 04 | Fog Basin | Cekungan lembap dengan kabut setinggi dada | Slow orbit dan visibility reduction | Siluet pohon terlihat lebih besar dari perkiraan |
| 05 | Cathedral Grove | Pohon raksasa, kanopi tinggi, dan shaft of light | Vertical crane-up lalu descent perlahan | Cahaya di kanopi membentuk pola yang tidak natural |
| 06 | Thorn Hollow | Jalur sempit, ranting berduri, dan warna hijau yang menggelap | Constrained forward push | Ada suara langkah kedua yang tidak sinkron |
| 07 | Night Marsh | Rawa malam, fireflies, air hitam, dan akar tenggelam | Sway lateral dengan fokus dangkal | Fireflies berkumpul membentuk arah tertentu |
| 08 | Ancient Boundary | Batu penanda, lumut tua, kabut dingin, dan silence break | Slow arc mengitari landmark | Simbol pada batu berubah ketika pengguna berhenti |
| 09 | Heartwood | Ruang hutan purba, akar monumental, dan cahaya bioluminesen | Descent/approach menuju pusat ruang | Pengguna melihat sesuatu hidup di balik heartwood |

## 6. Prinsip desain

Arah visual yang dipilih adalah **Biophilic Editorial**. Hutan harus terasa organik, tak sepenuhnya terpetakan, dan lebih besar daripada viewport. Layout memakai komposisi asimetris, label observasi kecil, headline serif dengan ruang napas, serta depth rail vertikal yang berubah dari warna pagi ke hijau malam dan biru hitam.

Signature visual terdiri dari tiga hal. Pertama, **canopy veil**, yaitu lapisan daun dan cabang transparan yang sesekali melewati foreground. Kedua, **moss signal**, yaitu aksen hijau lumut yang menjadi indikator aktif, focus ring, dan glow kecil pada detail interaktif. Ketiga, **field notes**, yaitu metadata chapter yang terdengar seperti catatan ekspedisi, bukan copy marketing generik.

Typography harus menggunakan display serif yang elegan untuk headline dan sans humanist yang terbaca untuk body, metadata, tombol, dan label kedalaman. Copy harus tenang, observasional, dan sedikit misterius. Hindari klaim hiperbolik serta filler seperti “Welcome to our website”.

## 7. Scope versi pertama

### In scope

Versi pertama mencakup satu halaman scroll-driven, sembilan chapter, depth rail, crossfade image plates, hybrid Three.js environment, camera path unik per chapter, procedural terrain sederhana, PNG cutouts untuk foreground, particles untuk dust/fireflies/rain, audio ambience per kelompok zona, hidden creature/detail reveal saat idle-scroll, lazy-loading Three.js, WebGL fallback, reduced-motion path, responsive desktop-mobile, dan dokumentasi teknis.

### Out of scope

Versi pertama tidak mencakup akun pengguna, database, CMS, multiplayer, peta lokasi nyata, komentar, commerce, personalisasi berbasis profil, procedural world tanpa batas, atau model AI yang berjalan di browser.

## 8. Requirement fungsional

| ID | Requirement | Prioritas |
|---|---|---|
| FR-01 | Sistem stage memiliki identifier stabil, title, body, image, tone, depth label, note, dan camera path | P0 |
| FR-02 | Scroll progress ternormalisasi ke `0..1` dan dipakai bersama oleh text, rail, background, camera, audio, serta detail reveal | P0 |
| FR-03 | Setiap chapter memiliki path kamera unik dengan posisi, look target, FOV, roll, easing, dan motion intensity | P0 |
| FR-04 | Background plate berada dalam environment hybrid Three.js sebagai textured plane atau layer 3D yang memiliki kedalaman | P0 |
| FR-05 | Scene memiliki background, midground, foreground, fog, lighting, particles, dan procedural forest terrain | P0 |
| FR-06 | Asset PNG transparan dapat dimuat sebagai cutout berparallax dan hanya tampil setelah texture siap | P0 |
| FR-07 | Audio ambience melakukan crossfade berdasarkan zona dan cue dapat dipicu saat chapter berubah | P1 |
| FR-08 | Audio tidak autoplay tanpa interaksi; tombol mute/sound selalu terlihat dan dapat diakses keyboard | P0 |
| FR-09 | Detail tersembunyi muncul perlahan setelah pengguna berhenti scroll beberapa detik | P1 |
| FR-10 | Reduced-motion menonaktifkan sway, camera pulse, particle drift, dan cue non-esensial | P0 |
| FR-11 | Kegagalan WebGL, asset, atau audio tidak boleh membuat root page crash | P0 |
| FR-12 | Navigasi chapter dapat digunakan melalui tombol, keyboard, dan anchor yang jelas | P0 |

## 9. Requirement nonfungsional

Performa harus menjadi acceptance criterion, bukan pekerjaan akhir. Three.js harus lazy-loaded, pixel ratio dibatasi terutama pada mobile, dan asset PNG dioptimalkan sebelum upload. Target awal adalah first paint image fallback yang cepat dan tidak ada fatal runtime error. Bundle warning boleh ada selama Three.js tetap berada pada chunk terpisah dan penyebabnya terdokumentasi.

Aksesibilitas mensyaratkan semantic landmarks, focus state terlihat, tombol dengan label, kontras teks yang cukup terhadap background aktual, audio opt-in, serta pengalaman yang dapat dipahami tanpa WebGL atau motion. Layout mobile sekitar 390px harus diuji secara khusus.

Reliability mensyaratkan cleanup `requestAnimationFrame`, resize listener, renderer, geometry, material, texture, audio element, dan event listener. Texture generation placeholder tidak boleh ditampilkan kepada pengguna.

## 10. Asset plan

Asset dibuat sebelum implementasi visual. Minimal dibutuhkan satu reference hero, satu image plate per zona utama, logo/mark tanpa teks, object PNG transparent untuk branch, fern, rock, moss, firefly cluster, fog veil, dan landmark stone. Object yang sama tidak boleh digunakan secara identik di semua chapter.

Asset besar disimpan di luar project directory, dikompres, diunggah melalui lifecycle-safe WebDev storage, dan di-referensikan dengan URL storage. Untuk object foreground, gunakan PNG alpha jika detail tepi dan transparansi penting; gunakan WebP jika asset tidak membutuhkan alpha penuh.

## 11. Arsitektur teknis

```text
React page
  ├── Stage model and normalized scroll progress
  ├── Editorial chapter sections and depth rail
  ├── CSS image fallback and atmospheric veil
  ├── Lazy-loaded ForestScene.tsx
  │     ├── PerspectiveCamera and chapter camera paths
  │     ├── Hybrid image plates in 3D space
  │     ├── Procedural forest floor, roots, rocks, and terrain
  │     ├── PNG foreground/midground cutouts
  │     ├── Fog, shafts, dust, fireflies, leaves, and rain
  │     └── WebGL guard and cleanup lifecycle
  ├── AudioDirector.tsx
  └── Reduced-motion and accessibility layer
```

The scene is an enhancement. The image-based page must remain complete when the scene is not loaded. The same normalized progress must drive all systems so there is no mismatch where copy indicates one chapter while the camera shows another.

## 12. Quality gates dan definition of done

Sebuah issue tidak boleh ditutup hanya karena kode berhasil dikompilasi. Issue dianggap selesai jika acceptance criteria-nya terpenuhi, `pnpm check` dan `pnpm build` berhasil, tidak ada fatal browser console error, dan perubahan yang relevan telah diuji pada desktop serta mobile.

Versi Forest Depths siap untuk checkpoint ketika seluruh requirement P0 selesai, fallback WebGL dapat diverifikasi, reduced-motion dapat diverifikasi, audio tidak memaksa autoplay, semua asset memiliki URL valid atau fallback, dan PRD serta issue tetap sinkron dengan implementasi aktual.

## 13. Risiko dan mitigasi

Risiko visual terbesar adalah scene terasa seperti kumpulan layer datar. Mitigasinya adalah menempatkan plate dan cutout pada jarak z berbeda, memberi camera path nyata, dan membuat foreground bergerak lebih cepat daripada background.

Risiko performa terbesar adalah texture dan geometry terlalu berat. Mitigasinya adalah kompresi asset, lazy-load, batas pixel ratio, geometry modest, dan pemisahan optional effects.

Risiko aksesibilitas terbesar adalah pengalaman bergantung pada motion dan audio. Mitigasinya adalah menyimpan narasi dalam HTML, menyediakan reduced-motion, tombol mute, dan fallback statis lengkap.

Risiko produksi terbesar adalah generation placeholder atau asset yang hilang. Mitigasinya adalah texture-ready guard, fallback color/image, validasi network, dan tidak menampilkan object sebelum texture berhasil dimuat.

## 14. Referensi

[1]: https://github.com/MengTo/kage "MengTo/Kage — interactive five-chapter Three.js experience"
[2]: https://threejs.org/docs/ "Three.js Documentation"
[3]: https://developer.mozilla.org/en-US/docs/Web/API/Window/requestIdleCallback "MDN requestIdleCallback"
[4]: https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices "MDN Media APIs"
