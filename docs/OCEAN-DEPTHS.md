# Ocean Depths

Ocean Depths adalah pengalaman web scroll-driven bertema penyelaman dari permukaan laut menuju palung hadal. Proyek ini menggabungkan komposisi editorial, background plate, lingkungan hybrid Three.js, terrain prosedural, object PNG transparan, audio ambience, dan fallback berbasis gambar agar narasi tetap dapat dinikmati ketika WebGL atau audio tidak tersedia.

> Prinsip utama: **scroll bukan hanya navigasi halaman; scroll adalah kontrol perjalanan kamera melalui kedalaman laut.**

## 1. Ringkasan teknis

| Area | Implementasi |
|---|---|
| Frontend | React 19, Vite, TypeScript, Tailwind CSS 4 |
| Scene 3D | Three.js `0.185.1`, PerspectiveCamera, fog, lighting, procedural geometry |
| Narasi | Sembilan chapter dari surface hingga hadal edge |
| Scroll state | Satu nilai normalized progress yang dipakai editorial layer, depth rail, camera, audio, dan object reveal |
| Visual utama | Image plate hybrid sebagai textured planes di dunia Three.js |
| Object detail | PNG transparan kelp, trench rocks, coral, dan jellyfish |
| Audio | Tiga ambience crossfade dan cue per chapter dengan autoplay-safe unlock |
| Fallback | Image-based CSS environment, WebGL guard, reduced-motion path, texture-ready guard |
| Hosting | Manus WebDev static frontend |

## 2. Menjalankan proyek

Pastikan Node.js dan pnpm tersedia. Setelah repository di-clone, instal dependency lalu jalankan development server.

```bash
pnpm install
pnpm dev
```

Untuk validasi TypeScript dan production build, gunakan perintah berikut.

```bash
pnpm check
pnpm build
```

Build menghasilkan aplikasi frontend pada `dist/public` dan bundle server statis pada `dist/index.js`. Peringatan tentang chunk yang lebih besar dari 500 kB berasal dari bundle aplikasi dan chunk Three.js; Three.js sendiri tetap dipisahkan melalui dynamic import agar tidak memblokir first paint.

## 3. Struktur file penting

```text
client/
  index.html
  src/
    App.tsx
    index.css
    pages/Home.tsx
    components/ThreeOceanScene.tsx
    components/AudioDirector.tsx

docs/
  OCEAN-DEPTHS.md

ideas.md
package.json
vite.config.ts
server/index.ts
```

`Home.tsx` mengelola stage data, scroll progress, chapter navigation, idle-scroll creature reveal, lazy-loading scene, dan audio director. `ThreeOceanScene.tsx` mengelola seluruh dunia 3D, termasuk camera path, textured image plates, terrain prosedural, particle field, object PNG, fish school, jellyfish, fog, lighting, dan cleanup lifecycle. `index.css` mengatur visual editorial, fallback scene, typography, depth rail, atmosphere, responsive behavior, dan kontrol audio.

## 4. Model perjalanan dan normalized progress

Halaman menggunakan array stage dengan identifier, kedalaman, label, title, body, tone, image, dan catatan observasi. Posisi scroll dikonversi menjadi nilai `progress` antara `0` dan `1`. Nilai yang sama digunakan oleh seluruh subsistem agar teks, rail, background, kamera, terrain, audio, dan object reveal tetap sinkron.

Sembilan zona utama adalah:

| Zona | Karakter pengalaman |
|---|---|
| Surface | Cahaya permukaan, drift horizontal, dan air yang masih terbuka |
| Reef | Detail organisme dan gerak kamera yang lebih hidup |
| Thermocline | Perubahan warna, tekanan, dan transisi diagonal |
| Kelp Cathedral | Kamera weaving melewati kelp dan siluet foreground |
| Twilight | Cahaya berkurang, orbit lebih lebar, dan bioluminesensi mulai muncul |
| Midnight | Descent lebih vertikal dan ruang visual semakin kosong |
| Blackwater | Rasa melayang, jarak jauh, dan objek yang muncul perlahan |
| Hadal Edge | Plunge menuju palung, trench wall, dan fissure glow |
| Abyss | Gerakan sangat lambat, fog pekat, dan ruang gelap yang luas |

## 5. Arsitektur hybrid Three.js

Background tidak lagi hanya berupa `background-image` CSS. Image plate dimuat menggunakan `THREE.TextureLoader`, dipasang pada beberapa `PlaneGeometry`, dan ditempatkan di jarak `z` berbeda di dalam `world` group. Opacity plate dihitung dari jarak antara `chapterPosition` dan index plate. Posisi x, y, dan z setiap plate juga digeser berdasarkan progress lokal serta camera motion.

Arsitektur visualnya adalah sebagai berikut.

```text
PerspectiveCamera
  ├── Near: object PNG, kelp, rocks, coral, jellyfish
  ├── Mid: fish school, ridge batu, bubbles, trench debris
  ├── Far: textured image plates dan depth tunnel
  └── Atmosphere: fog, marine snow, plankton, light shafts
```

Saat WebGL aktif, image stack CSS direduksi menjadi veil tipis agar textured planes menjadi sumber lingkungan utama. Saat WebGL gagal, canvas tidak dibuat dan image stack CSS tetap menjadi pengalaman lengkap.

## 6. Camera path dan scroll-scrubbing

Setiap chapter mempunyai `CameraPath` sendiri yang mencakup posisi kamera, look-at target, roll, FOV, sway horizontal, dan sway vertikal. Path antar chapter diinterpolasi dengan progress lokal dan easing `smoothstep`. Transition pulse digunakan untuk memberi aksen kecil pada FOV, world offset, fog, dan depth tunnel saat berpindah zona.

Karakter geraknya sengaja berbeda. Surface menggunakan drift; Thermocline bergerak diagonal; Kelp Cathedral melakukan weaving; Twilight melakukan orbit; Blackwater menggunakan sway lateral; sedangkan Hadal Edge dan Abyss menekankan plunge yang lambat serta terarah.

Reduced motion menghilangkan sway, roll dinamis, particle drift, dan transition pulse. Posisi kamera masih mengikuti progress agar struktur narasi dan navigasi tetap berfungsi.

## 7. Terrain palung prosedural

Dinding palung dan dasar laut dibuat saat runtime tanpa model 3D eksternal. Seabed menggunakan `PlaneGeometry` dengan grid subdivision, kemudian vertex y dimodifikasi memakai deterministic noise, ridge function, dan basin function. Dinding kiri-kanan menggunakan plane ber-subdivision dengan displacement x yang bergantung pada posisi lokal dan kedalaman.

Terrain juga memiliki ridge batu berbasis `DodecahedronGeometry` serta garis retakan bioluminesen berbasis `THREE.Line`. Semua material memakai roughness tinggi dan flat shading ringan agar siluet geologis terbaca tanpa menambah texture besar.

## 8. Object PNG transparan

Object PNG dipakai ketika bentuk CSS tidak mampu memberikan detail organik yang cukup. Asset saat ini meliputi kelp, trench rocks, coral, dan jellyfish. Setiap object dimuat sebagai plane transparan dengan ukuran dan stage target sendiri. Object hanya diberi opacity setelah texture selesai dimuat sehingga placeholder generation tidak pernah muncul di halaman.

Asset dikompres dengan pipeline alpha-preserving. Resolusi maksimum dibatasi hingga 1536px pada sisi terpanjang, warna dikurangi menggunakan palette optimization, dan PNG disimpan dengan compression level tinggi. Ukuran hasil yang digunakan saat ini adalah sebagai berikut.

| Asset | Ukuran sebelum | Ukuran optimized |
|---|---:|---:|
| Kelp | 3,6 MB | 789 KB |
| Trench rocks | 5,4 MB | 2,0 MB |
| Coral | 6,7 MB | 2,6 MB |
| Jellyfish | 4,3 MB | 1,1 MB |

`TextureLoader` memakai `LinearFilter`, `generateMipmaps = false`, dan renderer membatasi pixel ratio menjadi sekitar `1.2` pada mobile dan `1.7` pada desktop.

## 9. Lazy-loading dan WebGL fallback

Three.js diimpor secara dinamis dari `Home.tsx` menggunakan `React.lazy`. Scene baru dimuat setelah first paint melalui scheduler idle dengan timeout fallback. `Suspense` menggunakan fallback kosong sehingga pengalaman image-based langsung dapat dirender.

Sebelum membuat renderer, scene melakukan probe terhadap `webgl2` dan `webgl`. Pembuatan `WebGLRenderer` dibungkus `try/catch`. Jika context tidak tersedia, scene keluar tanpa melempar error ke root aplikasi. Ini penting untuk browser dengan WebGL dinonaktifkan, perangkat low-power, atau environment preview yang tidak menyediakan GPU context.

## 10. Audio ambience dan cue

`AudioDirector.tsx` mengelola tiga ambience utama: surface–reef–kelp, twilight–midnight, dan blackwater–abyss. Volume ambience mengikuti progress kedalaman dan melakukan crossfade. Cue chapter dibuat lebih pendek dan dipicu ketika active chapter berubah.

Browser tidak boleh dipaksa memutar suara tanpa izin. Karena itu audio melakukan unlock setelah pointer atau keyboard interaction pertama. Tombol `SOUND ON/OFF` tetap terlihat, preferensi mute disimpan secara lokal, dan reduced-motion menonaktifkan cue tambahan yang tidak esensial.

## 11. Hidden creature reveal

Ketika pengguna berhenti scroll selama beberapa detik, makhluk laut yang sesuai dengan zona dapat muncul secara perlahan. State idle-scroll direset ketika scroll bergerak kembali. Reveal menggunakan opacity interpolation, posisi offset kecil, dan label observasi yang hanya diumumkan ketika state aktif. Pengalaman inti tidak bergantung pada interaksi tersembunyi tersebut.

## 12. Performa dan optimasi

Beberapa aturan utama untuk menjaga performa adalah memuat Three.js secara lazy, membatasi pixel ratio, menggunakan `Points` untuk partikel kecil, menghindari real-time post-processing berat, menggunakan material sederhana, menonaktifkan mipmap untuk PNG overlay, dan memuat object hanya melalui URL asset lifecycle-safe.

Asset besar harus disimpan di `/home/ubuntu/webdev-static-assets/` lalu diunggah menggunakan storage WebDev. Kode frontend hanya boleh memakai URL `/manus-storage/...` yang dikembalikan proses upload. Jangan menaruh gambar besar di `client/public` atau `client/src/assets` karena dapat memperlambat deployment.

## 13. Accessibility dan fallback

Konten utama harus tetap terbaca tanpa WebGL, audio, atau motion. Gunakan semantic landmarks, button nyata untuk navigasi chapter, visible focus state, label kontrol deskriptif, dan `prefers-reduced-motion`. Jangan menjadikan hover, custom cursor, audio, atau WebGL sebagai satu-satunya cara untuk memahami cerita.

Jika object PNG gagal dimuat, material object dibuat tidak terlihat. Jika image plate gagal dimuat, plane menggunakan fallback color per kedalaman. Jika WebGL gagal, scene CSS tetap ditampilkan. Jika audio autoplay diblokir, tombol audio tetap dapat digunakan setelah interaction.

## 14. Troubleshooting

### Halaman gagal digenerasi

Periksa error WebGL pada browser console. Pastikan scene memiliki probe WebGL, `try/catch` pada renderer, dan fallback image stack. Jangan melakukan inisialisasi Three.js secara statis di root halaman.

### Placeholder “Generating image” terlihat

Pastikan object memiliki state `loaded = false` dan opacity tetap `0` sampai callback `TextureLoader` berhasil. Jangan merender reserved generation URL secara visual sebelum texture siap.

### Background terasa datar

Pastikan image plate berada di dalam `world` Three.js, bukan hanya sebagai background CSS. Periksa perbedaan posisi `z`, camera travel, parallax, dan opacity plate. CSS scene stack sebaiknya menjadi veil ketika hybrid scene aktif.

### Asset terlalu berat

Gunakan asset PNG optimized, batasi sisi terpanjang, nonaktifkan mipmap jika tidak dibutuhkan, dan pertimbangkan lazy-load berbasis jarak chapter. Jangan mengulang asset resolusi penuh pada banyak mesh jika satu texture dapat dipakai bersama.

### Audio tidak terdengar

Browser biasanya menunggu interaction sebelum mengizinkan audio. Klik tombol `SOUND ON` atau lakukan pointer/keyboard interaction. Periksa apakah browser dalam mode mute dan pastikan volume tidak bernilai nol.

## 15. Validasi sebelum release

Jalankan `pnpm check` dan `pnpm build`. Periksa bahwa chunk Three.js terpisah dari bundle awal, lalu uji preview pada desktop dan viewport sekitar 390px. Periksa browser console dan network log untuk failed import, missing asset, WebGL exception, atau audio error. Pastikan pengalaman tetap utuh ketika reduced motion aktif dan ketika WebGL tidak tersedia.

## Referensi

[1]: https://threejs.org/docs/ "Three.js Documentation"
[2]: https://github.com/MengTo/kage "MengTo/Kage — interactive five-chapter Three.js experience"
[3]: https://developer.mozilla.org/en-US/docs/Web/API/Window/requestIdleCallback "MDN requestIdleCallback"
[4]: https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/play "MDN HTMLMediaElement.play and autoplay behavior"
