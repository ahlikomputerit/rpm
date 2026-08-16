# Lumen Transfer — Product Requirements Document

**Status:** Draft siap dipecah menjadi issue  
**Versi:** 1.0  
**Bahasa:** Bahasa Indonesia  
**Working title:** Lumen Transfer  
**Platform target:** Android phone dan tablet  
**Target implementasi awal:** Kotlin + Jetpack Compose melalui Google AI Studio Build mode  
**Tanggal:** 2026-08-16  

## 1. Ringkasan produk

Lumen Transfer adalah aplikasi Android standalone untuk memindahkan satu file dari perangkat pengirim ke perangkat penerima menggunakan hanya **layar pengirim dan kamera penerima**. Pengirim menampilkan aliran QR code animasi yang berisi metadata serta potongan file. Penerima mengarahkan kamera ke layar tersebut, membaca frame secara berulang, merekonstruksi file, memverifikasi integritasnya, lalu menyimpannya ke perangkat.

Transfer payload tidak memakai Wi-Fi, Bluetooth, jaringan seluler, kabel, server, akun, atau pairing. Kedua perangkat cukup memasang aplikasi terlebih dahulu. Setelah aplikasi tersedia, sesi transfer dapat berlangsung sepenuhnya secara lokal dan offline. Keputusan ini mengikuti karakteristik platform Android di Google AI Studio: aplikasi Android yang dihasilkan bersifat client-side, berbasis Kotlin dan Jetpack Compose, dan tidak membutuhkan server-side runtime untuk fitur lokal.[1]

Produk ini bukan pengganti sistem berbagi file berkecepatan tinggi. Nilai utamanya adalah **air-gapped optical transfer** yang mudah dipahami, tidak meninggalkan data di server, dan dapat tetap berfungsi ketika jalur konektivitas biasa tidak tersedia atau sengaja tidak digunakan.

## 2. Masalah yang ingin diselesaikan

Berbagi file antarp ponsel biasanya mengandalkan jaringan, Bluetooth, kabel, akun cloud, atau aplikasi pihak ketiga. Setiap pilihan tersebut menambah prasyarat berupa pairing, penemuan perangkat, izin jaringan, akses internet, atau kepercayaan terhadap layanan eksternal. Dalam situasi tertentu, pengguna hanya memiliki dua perangkat dengan layar dan kamera, tetapi tidak ingin atau tidak dapat menghubungkannya melalui jalur radio.

Solusi berbasis QR biasa sering memakai chunk berurutan. Jika satu frame gagal terbaca, penerima harus menunggu pengulangan atau transfer dapat rusak. Lumen Transfer menggunakan aliran frame berulang dengan redundansi fountain-code sebagai target protokol, sehingga frame yang hilang menambah waktu tetapi tidak seharusnya merusak kebenaran file.

## 3. Visi dan prinsip produk

Visinya adalah menjadikan transfer file optik terasa seperti tindakan sederhana: **pilih file, tampilkan layar, arahkan kamera, simpan hasil**. Pengguna tidak perlu memahami QR, chunk, checksum, atau fountain code.

Prinsip produk yang tidak boleh dikompromikan adalah sebagai berikut. Pertama, tidak ada data file yang dikirim ke server. Kedua, kegagalan sebagian frame tidak boleh langsung menggagalkan transfer. Ketiga, file tidak boleh ditawarkan sebagai hasil valid sebelum checksum cocok. Keempat, kamera dan file hanya diminta ketika fitur tersebut dipakai. Kelima, aplikasi harus tetap jujur tentang batasan keamanan: layar yang sedang menampilkan QR dapat direkam atau dibaca oleh kamera lain.

## 4. Tujuan dan indikator keberhasilan

| Tujuan | Indikator keberhasilan versi MVP |
|---|---|
| Transfer lokal tanpa jaringan | File dapat dipindahkan ketika Wi-Fi, Bluetooth, dan data seluler dimatikan setelah aplikasi tersedia di kedua perangkat. |
| Alur pengguna sederhana | Pengguna baru dapat memulai pengiriman dan penerimaan tanpa dokumentasi teknis tambahan. |
| Tahan terhadap frame yang hilang | Transfer tetap selesai setelah sebagian frame QR sengaja dilewati atau tidak terbaca. |
| Integritas file terjamin | SHA-256 hasil rekonstruksi sama dengan checksum file asal sebelum tombol simpan aktif. |
| Native Android yang dapat dipasang | APK debug dapat dibangun dan dipasang pada minimal dua perangkat Android nyata. |
| Dapat diuji di Google AI Studio | Project mengikuti batasan Kotlin, Jetpack Compose, single activity, dan single module yang didukung platform.[1] |
| Privasi dapat dijelaskan | UI menyatakan bahwa transfer tidak terenkripsi pada MVP dan data tidak melewati server. |

Target performa awal bukan angka absolut yang sama untuk semua perangkat. Benchmark harus melaporkan ukuran file, pasangan perangkat, resolusi layar, kecepatan frame, durasi, goodput, serta frame loss. Angka contoh dari proyek web rujukan tidak boleh dianggap sebagai SLA untuk aplikasi native.[2]

## 5. Target pengguna

Target utama adalah pengguna Android yang membutuhkan cara memindahkan file kecil sampai menengah tanpa akun, koneksi jaringan, atau aplikasi cloud. Target sekunder adalah teknisi, pendidik, peneliti lapangan, atau pengguna perangkat terisolasi yang membutuhkan saluran transfer visual sederhana.

MVP hanya menargetkan perangkat Android phone dan tablet dengan kamera yang dapat digunakan untuk preview video. Wear OS, Android TV, dan perangkat tanpa kamera berada di luar cakupan. Google AI Studio sendiri mendukung phone dan tablet, bukan Wear OS atau Android TV.[1]

## 6. User journey

Pengirim membuka aplikasi dan memilih **Kirim**. Aplikasi meminta pemilihan file melalui system file picker, menghitung metadata serta checksum, lalu menampilkan layar transfer. Pengguna menempatkan perangkat pengirim di depan perangkat penerima.

Penerima membuka aplikasi dan memilih **Terima**. Aplikasi meminta izin kamera, menampilkan preview, dan mulai mendeteksi frame. Ketika metadata transfer telah terbaca, UI menampilkan nama file, ukuran, estimasi progres, frame yang diterima, dan status decoding.

Setelah cukup banyak frame valid terkumpul, aplikasi merekonstruksi byte file dan menghitung checksum. Jika checksum cocok, pengguna dapat menyimpan file melalui system document picker. Jika checksum gagal, aplikasi tidak menawarkan hasil sebagai file valid dan menjelaskan langkah pemulihan.

## 7. Ruang lingkup versi pertama

### In scope

| Area | Cakupan MVP |
|---|---|
| Platform | Android phone/tablet, Kotlin, Jetpack Compose, satu activity, satu module. |
| Transfer | Satu file per sesi, satu pengirim, satu penerima, tanpa back-channel. |
| Format | Semua file sebagai byte stream; pengujian wajib mencakup gambar, audio, video kecil, PDF, teks, dan file biner. |
| Ukuran | Batas konfigurasi awal 10 MB; arsitektur harus dapat dinaikkan ke 64 MB tanpa perubahan protokol mendasar. |
| Sender | File picker, metadata, checksum, QR animation, pause/resume, cancel, dan indikator status. |
| Receiver | Camera preview, QR decoding, deduplikasi, progress, pause/resume scanning, reconstruction, checksum, save result. |
| Protokol | Envelope berversi, metadata frame, frame payload, deduplikasi, retry-by-repetition, dan fountain-code adapter. |
| Penyimpanan | Memori untuk file kecil dan cache lokal sementara untuk file lebih besar; tidak ada upload cloud. |
| Quality | Unit test codec, golden vectors, checksum test, error-state test, dan device-to-device test. |
| Distribusi | Debug APK untuk uji internal; release bundle atau APK signed dibahas setelah MVP stabil. |

### Out of scope

Versi pertama tidak mencakup akun pengguna, server, database, cloud storage, transfer dua arah bersamaan, multi-file batch, folder, link sharing, background transfer, Bluetooth fallback, Wi-Fi Direct fallback, desktop client, iOS client, Wear OS, Android TV, monetisasi, iklan, analytics pihak ketiga, serta sinkronisasi otomatis.

Enkripsi end-to-end juga bukan bagian P0 karena dua perangkat tidak memiliki back-channel untuk menegosiasikan kunci secara otomatis. Enkripsi passphrase manual dapat dibuat sebagai P1 setelah protokol dasar stabil.

## 8. Requirement fungsional

| ID | Requirement | Prioritas |
|---|---|---|
| FR-01 | Pengguna dapat memilih mode Kirim atau Terima dari layar utama. | P0 |
| FR-02 | Pengirim dapat memilih tepat satu file melalui Android system file picker. | P0 |
| FR-03 | Pengirim menghitung nama file, MIME type, ukuran, dan SHA-256 sebelum transfer. | P0 |
| FR-04 | Pengirim menampilkan frame QR secara berulang dengan interval yang dapat diatur. | P0 |
| FR-05 | Setiap frame memiliki protocol version, transfer ID, tipe frame, metadata atau payload, sequence/seed, dan checksum yang sesuai. | P0 |
| FR-06 | Receiver meminta izin kamera hanya ketika mode Terima dipilih dan menampilkan alasan izin secara jelas. | P0 |
| FR-07 | Receiver membaca QR dari live camera preview dan mengabaikan QR yang rusak atau tidak sesuai protocol version. | P0 |
| FR-08 | Receiver mengabaikan frame duplikat tanpa menaikkan progres efektif. | P0 |
| FR-09 | Receiver dapat mengumpulkan frame dalam urutan berbeda dari urutan pengirimannya. | P0 |
| FR-10 | Receiver merekonstruksi file tanpa server atau komunikasi balik. | P0 |
| FR-11 | Aplikasi memvalidasi SHA-256 sebelum file dianggap berhasil. | P0 |
| FR-12 | Pengguna dapat membatalkan transfer dari kedua sisi tanpa membuat aplikasi crash. | P0 |
| FR-13 | UI menampilkan status idle, preparing, sending, scanning, decoding, verifying, completed, cancelled, dan error. | P0 |
| FR-14 | File hasil dapat disimpan dengan nama dan MIME type yang dipulihkan dari metadata. | P0 |
| FR-15 | Aplikasi memberi panduan posisi, jarak, orientasi, dan kecerahan layar. | P1 |
| FR-16 | Pengirim dapat mengatur ukuran QR, correction level, dan kecepatan frame melalui advanced settings. | P1 |
| FR-17 | Aplikasi mendukung pause/resume scanning tanpa menghapus data yang sudah terkumpul. | P1 |
| FR-18 | Aplikasi mendukung passphrase encryption dengan AES-GCM dan key derivation yang terdokumentasi. | P1 |
| FR-19 | Aplikasi menyediakan diagnostics screen untuk frame rate, decode rate, duplicate rate, loss estimate, goodput, dan error code. | P1 |
| FR-20 | Aplikasi menyediakan dark theme dan layout portrait serta landscape. | P1 |

## 9. Requirement nonfungsional

### 9.1 Offline dan privasi

Setelah aplikasi dan resource lokal tersedia, transfer tidak boleh membuat request jaringan. MVP tidak menggunakan server-side runtime, database remote, analytics, login, atau cloud storage. Android app di Google AI Studio bersifat client-side only; fitur server-side seperti Firebase integration, secrets management, Workspace API, dan multiplayer tidak tersedia untuk Android app tersebut.[1]

Aplikasi harus menampilkan pernyataan privasi singkat sebelum sesi pertama: **“File berpindah melalui cahaya dari layar ke kamera. Tidak ada server yang menerima file. QR pada layar dapat dibaca oleh kamera lain.”**

### 9.2 Keamanan

Checksum wajib digunakan untuk mendeteksi korupsi. Checksum bukan enkripsi. Pada MVP, threat model menganggap siapa pun yang dapat melihat layar dapat memperoleh isi file. P1 dapat menambahkan enkripsi passphrase, tetapi password tidak boleh ditulis ke log, disimpan permanen, atau dikirim melalui QR tanpa perlindungan tambahan.

### 9.3 Performa

UI tidak boleh freeze ketika file sedang dipecah, frame sedang diencode, atau file sedang direkonstruksi. Pekerjaan CPU harus menggunakan coroutine atau dispatcher yang sesuai. Preview kamera dan QR renderer harus memiliki lifecycle cleanup untuk menghindari camera leak, coroutine leak, atau memory leak.

Batas awal yang disarankan adalah file 10 MB, payload QR konservatif, dan satu frame aktif pada satu waktu. Ukuran payload, correction level, frame interval, dan decoder harus dapat diubah tanpa mengubah kontrak data utama.

### 9.4 Compatibility

MVP harus memiliki minimum SDK yang realistis untuk perangkat uji dan target Android yang tersedia pada project. Versi SDK final harus ditetapkan ketika project AI Studio dibuat karena template dan dependency dapat berubah. Pengujian minimum meliputi dua perangkat Android dengan perbedaan kamera dan layar, serta satu perangkat dengan orientasi landscape.

### 9.5 Accessibility dan UX

Semua kontrol utama wajib memiliki label yang dapat dibaca screen reader, touch target yang memadai, focus state yang terlihat, kontras yang cukup, dan pesan error yang dapat dipahami tanpa melihat detail teknis. Animasi QR tidak boleh menjadi satu-satunya sumber informasi status; nama file, ukuran, dan status harus juga tersedia sebagai teks.

## 10. Arsitektur produk

```text
Single-Activity Compose App
├── HomeScreen
│   ├── Send route
│   └── Receive route
├── Send flow
│   ├── Android file picker
│   ├── FileMetadataReader
│   ├── Sha256Hasher
│   ├── Chunk/Fountain Encoder
│   ├── FrameEnvelopeSerializer
│   └── AnimatedQrRenderer
├── Receive flow
│   ├── CameraX Preview
│   ├── QrFrameDecoder
│   ├── FrameValidator
│   ├── DeduplicationStore
│   ├── Fountain Decoder
│   ├── FileReconstructor
│   └── Sha256Verifier
├── Persistence
│   ├── SessionStateStore
│   └── TemporaryFileStore
├── Shared domain
│   ├── TransferState
│   ├── ProtocolVersion
│   ├── TransferError
│   └── DiagnosticsEvent
└── Presentation
    ├── ViewModels
    ├── Compose screens
    ├── Permission states
    └── Theme and accessibility
```

AI Studio dibatasi pada project Kotlin + Jetpack Compose, single activity, single module, tanpa Java/XML layout dan tanpa NDK/native C/C++.[1] Karena itu, decoder awal harus dipilih dari library Kotlin/Java yang dapat dipasang melalui Maven atau dari implementasi QR yang tidak membutuhkan native code. Jika library terbaik ternyata membutuhkan ZXing-C++ atau NDK, keputusan tersebut harus ditunda dan dibuktikan ulang di luar batas MVP AI Studio.

## 11. Kontrak protokol tingkat tinggi

Frame wire format harus berversi dan memiliki magic prefix agar QR acak tidak dianggap sebagai frame Lumen Transfer. Semua integer perlu menggunakan endianness yang ditentukan. Serializer dan decoder wajib memiliki golden vector sehingga perubahan format dapat terdeteksi oleh test.

| Field | Deskripsi |
|---|---|
| `magic` | Identitas protokol. |
| `version` | Versi wire format. |
| `flags` | Fitur yang aktif, misalnya compressed atau encrypted. |
| `transferId` | ID sesi acak yang tidak mengandung informasi pribadi. |
| `frameKind` | Header, metadata, data, parity, atau end marker. |
| `objectId` | Identitas file dalam sesi. |
| `sequenceOrSeed` | Sequence untuk frame sederhana atau seed untuk fountain frame. |
| `blockCount` | Parameter decoder. |
| `payloadLength` | Panjang payload aktual. |
| `payload` | Data metadata atau potongan file. |
| `frameChecksum` | Deteksi kerusakan frame. |

MVP dapat dimulai dengan **sequential chunk fallback** untuk memvalidasi end-to-end flow, tetapi production-ready milestone harus menggunakan fountain-code adapter atau secara eksplisit menandai mode sequential sebagai eksperimental. Receiver tidak boleh menganggap keberhasilan hanya berdasarkan jumlah frame; ia harus mencapai kondisi decoder complete dan checksum file cocok.

## 12. Risiko dan mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Emulator AI Studio tidak mendukung kamera | Camera flow tidak dapat divalidasi di emulator. | Uji penerima pada perangkat Android nyata sejak issue kamera pertama. AI Studio menyatakan browser emulator tidak mendukung camera/photo capture.[1] |
| QR terlalu padat untuk kamera ponsel | Decode rate rendah atau transfer tidak selesai. | Mulai dengan payload kecil, correction level konservatif, lalu benchmark per pasangan perangkat. |
| Library QR membutuhkan native code | Tidak sesuai batasan AI Studio. | Pilih library JVM/Kotlin untuk MVP; buat spike teknis sebelum mengunci dependency. |
| Frame hilang atau duplikat | Progres lambat atau file gagal direkonstruksi. | Fountain-code target, deduplication, frame repetition, dan checksum. |
| Layar mati atau brightness rendah | Transfer terhenti. | Tampilkan screen-on flag selama send flow dan panduan menaikkan brightness. |
| File besar menyebabkan memory pressure | Crash atau UI freeze. | Temporary file store, streaming reconstruction, dan batas 10 MB untuk MVP. |
| Pengguna mengira checksum adalah keamanan | Data sensitif dianggap aman padahal terbaca kamera lain. | Privacy notice wajib dan encryption dipisahkan sebagai P1. |
| Export project terbatas | Workflow GitHub tidak langsung tersedia dari AI Studio Android. | Simpan PRD, issue, dan TDD di repositori; gunakan ZIP export AI Studio lalu impor ke Android Studio. AI Studio mendokumentasikan ZIP sebagai jalur ekspor Android.[1] |

## 13. Definition of Done

Sebuah issue dianggap selesai jika kode berhasil dikompilasi, acceptance criteria issue terpenuhi, unit test terkait lulus, tidak ada crash pada state normal maupun error utama, dan dokumentasi kontrak yang berubah telah diperbarui.

MVP dianggap siap untuk uji internal apabila dua perangkat Android nyata dapat melakukan transfer gambar, PDF, audio, video kecil, dan file biner ketika koneksi radio dimatikan. Hasil akhir harus memiliki checksum yang sama dengan sumber, dan sekurang-kurangnya satu uji harus membuktikan bahwa frame yang hilang atau rusak tidak langsung menggagalkan transfer.

## 14. Tahapan rilis

| Milestone | Hasil |
|---|---|
| M0 — Product foundation | PRD, TDD, project skeleton, theme, navigation, dan keputusan dependency. |
| M1 — Protocol laboratory | Envelope, serializer, validator, checksum, golden vectors, dan sequential chunk prototype. |
| M2 — Sender | File picker, metadata, QR rendering, lifecycle, screen-on, dan cancel. |
| M3 — Receiver | Camera permission, preview, QR decode, validation, deduplication, dan progress. |
| M4 — Reconstruction | Decoder, temporary file store, checksum verification, save result, dan recovery state. |
| M5 — Fountain reliability | Fountain encoder/decoder, dropped-frame test, duplicate test, dan benchmark harness. |
| M6 — Device QA | Dua perangkat nyata, orientation, brightness, camera distance, performance, accessibility, dan privacy review. |
| M7 — Internal release | Signed debug/release candidate APK, known limitations, installation guide, dan test report. |

## 15. Keputusan produk yang ditunda

Keputusan berikut tidak boleh menghambat P0: format fountain code final, penggunaan kompresi, AES-GCM, dukungan multi-file, background service, resume setelah aplikasi ditutup, animated color QR, adaptive frame rate, iOS port, dan Play Store publishing. Setiap keputusan tersebut harus dibuat berdasarkan hasil benchmark atau kebutuhan pengguna, bukan asumsi.

## 16. Referensi

[1]: https://ai.google.dev/gemini-api/docs/aistudio-android "Google AI Studio — Build Android Apps"
[2]: https://github.com/bashalarmistalt/decimen-optical-transfer/ "Decimen Optical Transfer — fountain-coded QR file transfer"
[3]: https://developer.android.com/develop/ui/compose "Android Developers — Jetpack Compose"
[4]: https://developer.android.com/training/permissions/requesting "Android Developers — Request app permissions"
