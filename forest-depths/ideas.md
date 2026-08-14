# Forest Depths — Arah Desain

## Tiga pendekatan awal

### Pendekatan 1 — Biophilic Editorial

**Very Brief Intro:** Pengalaman editorial sinematik yang terasa seperti buku foto hutan tua: tenang, lembap, taktil, dan disusun sebagai perjalanan dari cahaya tepi hutan menuju heartwood yang gelap.

**Probability:** 0.07

### Pendekatan 2 — Field Station Archive

**Very Brief Intro:** Ekspedisi botani yang lebih ilmiah dengan label spesimen, koordinat, catatan cuaca, dan struktur arsip lapangan yang presisi.

**Probability:** 0.04

### Pendekatan 3 — Nocturne Understory

**Very Brief Intro:** Interpretasi malam yang lebih misterius, berpusat pada kabut, jamur bercahaya, firefly trails, dan batas tipis antara pengamatan dan imajinasi.

**Probability:** 0.02

## Pendekatan terpilih — Biophilic Editorial

### Design Movement

Neo-editorial environmental storytelling: perpaduan art direction majalah fotografi alam, film dokumenter hutan hujan beriklim sedang, dan jurnal perjalanan yang lambat.

### Core Principles

1. **Depth as narrative:** setiap chapter mengubah kualitas cahaya, kelembapan, tekstur, dan kepadatan vegetasi sehingga scroll terasa seperti masuk lebih dalam, bukan berpindah section.
2. **Quiet scale:** headline besar yang asimetris berhadapan dengan detail kecil seperti lumut, jamur, dan partikel debu untuk memberi rasa luas dan usia.
3. **Layered reality:** plate, PNG foreground, kabut, akar, objek midground, dan Three.js bergerak pada kedalaman berbeda.
4. **Scientific restraint:** label chapter dan observasi singkat memberi konteks tanpa mengubah pengalaman menjadi dashboard.

### Color Philosophy

Palet dimulai dari lichen cream dan fern green di tepi hutan, turun ke cedar brown dan fog blue di jalur lembap, lalu berakhir pada heartwood black dengan aksen firefly amber. Warna berubah mengikuti berkurangnya cahaya dan bertambahnya kepadatan, bukan sebagai gradient dekoratif.

### Layout Paradigm

Viewport lingkungan bersifat tetap, sedangkan editorial content mengalir secara vertikal. Headline berganti sisi kiri dan kanan, depth rail tetap hadir sebagai meter perjalanan, dan asset visual boleh menembus batas frame agar terasa seperti kamera yang berjalan di antara batang dan akar.

### Signature Elements

1. **Depth rail:** garis vertikal dengan penanda chapter, elevasi relatif, dan indikator progress yang selalu terlihat.
2. **Lichen trace:** garis atau titik kecil berwarna amber/lichen yang bergerak pelan seperti firefly atau serbuk sari.
3. **Canopy veil:** lapisan kabut, grain, dan shadow canopy yang makin rapat ketika masuk ke heartwood.

### Interaction Philosophy

Scroll terasa seperti berjalan perlahan dan memberi waktu bagi mata untuk menyesuaikan diri. Chapter navigation memungkinkan lompatan yang terkontrol, sedangkan idle state membuka observasi tersembunyi tanpa mengganggu teks utama. Hover dan focus hanya memberi respons kecil seperti perubahan cahaya, garis, atau label.

### Animation

Progress scroll menjadi sumber koordinasi tunggal untuk plate crossfade, camera path, opacity object, tone, dan depth rail. Background bergerak paling lambat, akar serta trunk bergerak pada mid-depth, daun dan dust drift lebih dekat ke viewport, dan teks masuk melalui reveal vertikal singkat. `prefers-reduced-motion` mempertahankan urutan narasi tanpa camera sway, particle drift, atau transition burst.

### Typography System

Display menggunakan **Cormorant Garamond** untuk headline yang terasa tua, observasional, dan editorial. Body serta metadata menggunakan **DM Sans** dengan tracking lebar untuk field labels. Headline memakai skala besar dan alignment asimetris; label kecil menggunakan uppercase, angka tabular, dan warna lichen.

### Brand Essence

Forest Depths adalah perjalanan scroll sinematik dari tepi hutan menuju heartwood, untuk pembaca yang ingin merasakan perubahan ruang, cahaya, dan kehidupan mikro melalui kombinasi fotografi, asset PNG detail, dan kamera 3D.

**Personality:** contemplative, tactile, mysterious.

### Brand Voice

Headline berbicara pendek, puitis, dan observasional. CTA bersifat instruktif tetapi tidak agresif. Microcopy terdengar seperti catatan lapangan yang ditulis ketika suara hutan mulai menutup jarak.

- “The canopy closes before the path does.”
- “Walk slowly. The smallest life is closest to the ground.”

### Wordmark & Logo

Logo berupa simbol abstrak tanpa teks: satu batang vertikal yang bercabang menjadi tiga akar pendek, dengan satu titik amber kecil di sisi bawah. Simbol ini mewakili trunk, canopy, dan kehidupan kecil di lantai hutan. Wordmark ditulis terpisah dalam small caps dengan tracking lebar.

### Signature Brand Color

**Lichen Amber — `#D7A45A`**. Warna ini menjadi sinyal observasi dan kehidupan tersembunyi, digunakan hemat untuk depth rail, firefly cue, focus state, dan garis CTA.

## Forest stage contract

Setiap stage wajib memiliki `id`, `index`, `depth`, `meters`, `eyebrow`, `title`, `body`, `image`, `tone`, `note`, `transitionCue`, dan `cameraCharacter`. Sembilan id stabilnya adalah `edge`, `fern-passage`, `moss-creek`, `fog-basin`, `cathedral-grove`, `thorn-hollow`, `night-marsh`, `ancient-boundary`, dan `heartwood`.

## Style Decisions

- Gunakan komposisi editorial asimetris, bukan layout terpusat yang seragam.
- Gunakan lichen green, cedar brown, fog blue, dan heartwood black sebagai tone perjalanan; Lichen Amber adalah satu-satunya signature accent.
- Prioritaskan transform dan opacity untuk animasi agar tetap ringan.
- Semua object utama harus berupa asset PNG/WebP nyata; CSS hanya menangani atmosphere, typography, veil, dan detail kecil.
- Depth rail harus menjadi meter observasi vertikal yang terus hadir.
- Komposisi chapter berganti secara nyata agar perjalanan terasa seperti rangkaian editorial spread.
- Hindari purple gradients, rounded cards berlebihan, glassmorphism generik, dan dekorasi tanpa fungsi naratif.
