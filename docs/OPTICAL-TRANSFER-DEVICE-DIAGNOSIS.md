# Lumen Transfer — Diagnosis Kegagalan Transfer Perangkat

**Tanggal diagnosis:** 2026-08-16
**Sumber:** `lumen-diagnostics-send.json` dan `lumen-diagnostics-receive.json` hasil uji pengguna
**Status:** Root cause pada live QR decoder diperbaiki; re-test dua perangkat masih diperlukan

## Ringkasan

Uji nyata gagal sebelum satu pun frame protocol diterima receiver. Sender memang memancarkan frame secara aktif, sedangkan receiver membaca ribuan image-analysis frames tetapi menolak seluruhnya sampai watchdog 30 detik berakhir. Pola ini menempatkan kegagalan pada tahap **QR image decoding**, bukan pada CRC, transfer ID, fountain decoder, SHA-256, atau Storage Access Framework.

| Sisi | Bukti dari diagnostics | Interpretasi |
|---|---:|---|
| Sender | 794 frame emitted; 843,083 emitted bytes; 793 systematic; 0 repair | Loop sender aktif dan layar terus diperbarui. |
| Receiver | 2,218 rejected; 0 accepted; 0 accepted bytes | Tidak ada frame yang mencapai parser/envelope acceptance. |
| Receiver terminal | 30,002 ms; `DECODER_STALLED` | Watchdog berakhir karena tidak ada frame valid, bukan karena checksum hasil gagal. |
| Receiver reconstruction | 0 source blocks; 0 recovered blocks; 0 equations | Fountain reconstruction tidak pernah dimulai. |

## Akar masalah

Bukti baru dari pengguna mempersempit akar masalah: QR yang sama dapat dibaca QR reader bawaan HP, sehingga QR sender valid. Regression fixture lama ternyata juga memakai urutan `A, R, G, B`, mengikuti asumsi salah pada decoder. CameraX `OUTPUT_IMAGE_FORMAT_RGBA_8888` menggunakan `R, G, B, A`. Decoder live sebelumnya membaca byte pertama sebagai alpha dan byte keempat sebagai blue; akibatnya nilai alpha konstan masuk ke luminance blue channel dan pola hitam-putih dapat menjadi hampir seragam bagi ZXing. Perbaikan channel mapping ini adalah kandidat akar masalah utama dan sudah diuji dengan fixture RGBA yang benar.

Implementasi sebelumnya menambahkan `DecodeHintType.PURE_BARCODE = true` pada `QRCodeReader` yang diberi **seluruh frame kamera**. Hint tersebut cocok untuk bitmap yang sudah berisi QR saja atau telah dicrop menjadi barcode murni. CameraX receiver justru mengirim preview penuh dengan background, perspektif, dan QR berada di sebagian area gambar. Akibatnya ZXing gagal menemukan QR pada setiap image-analysis frame sebelum `FrameSerializer.parse()` dipanggil.

Perbaikan mengubah urutan decoding menjadi berikut:

1. Jalankan ZXing detector normal dengan `TRY_HARDER` dan `POSSIBLE_FORMATS = QR_CODE` pada full camera preview.
2. Gunakan `PURE_BARCODE` hanya sebagai fallback untuk fixture atau input yang memang sudah tercrop.
3. Tidak melakukan rotasi ulang terhadap seluruh buffer kamera. `rotationDegrees` adalah orientasi display; detector QR dapat menemukan finder pattern pada orientasi yang didukung, sementara rotasi global sebelumnya berpotensi mengubah relasi buffer yang sudah disediakan CameraX.
4. Pertahankan pembacaan channel CameraX dalam urutan `R, G, B, A`. Dokumentasi resmi CameraX menjelaskan bahwa `OUTPUT_IMAGE_FORMAT_RGBA_8888` menggunakan urutan red, green, blue, lalu alpha pada indeks byte yang meningkat.[1]

## Perubahan diagnosis

Diagnostics sebelumnya hanya memiliki satu counter `rejectedFrames`, sehingga 2,218 rejection tidak dapat dibedakan antara QR tidak ditemukan dan protocol invalid. Snapshot berikutnya sekarang menambahkan counter payload-free berikut:

| Counter | Makna |
|---|---|
| `qrNotFoundFrames` | Image-analysis frame tidak menghasilkan QR text. |
| `invalidProtocolFrames` | QR text terbaca, tetapi Base64/envelope tidak valid. |
| `transferIdMismatchFrames` | Envelope valid, tetapi berasal dari sesi transfer berbeda. |

Counter ini tidak menyimpan payload, QR text, nama file, atau isi file.

## Validasi otomatis

Regression test baru menjalankan decoder terhadap QR yang ditempatkan di dalam frame RGBA yang lebih besar, bukan hanya terhadap bitmap QR yang memenuhi seluruh buffer. Fixture juga menggunakan payload 1,024 byte untuk mendekati kepadatan frame transfer aktual. Unit test QR dan seluruh `testDebugUnitTest` berhasil setelah perbaikan.

Validasi perangkat nyata belum dapat dilakukan ulang dari sandbox. Karena itu, perbaikan ini adalah **diagnosis berbasis bukti + regression test**, bukan klaim bahwa transfer fisik sudah terverifikasi end-to-end.

## Prosedur uji ulang

Install APK debug terbaru pada dua perangkat. Pada sender, pilih file kecil terlebih dahulu, idealnya 10–100 KB. Pada receiver, izinkan kamera, naikkan brightness sender, tampilkan seluruh QR, dan mulai dari jarak sekitar 15–30 cm. Setelah 30–60 detik, export diagnostics dari kedua perangkat.

Sesi dianggap melewati tahap decoder apabila receiver menunjukkan `acceptedFrames > 0` dan `qrNotFoundFrames` tidak lagi mendominasi. Jika `acceptedFrames` tetap nol, bandingkan `qrNotFoundFrames` dengan `invalidProtocolFrames`: QR-not-found berarti masalah fokus, ukuran QR di kamera, framing, atau brightness masih tersisa; invalid-protocol berarti QR sudah terbaca tetapi transformasi Base64/envelope perlu ditelusuri berikutnya.

## Referensi

[1]: https://developer.android.com/media/camera/camerax/analyze "Android Developers — Image analysis"

## Bukti uji kedua

Diagnostics kedua kembali menunjukkan kegagalan sebelum protocol parser, tetapi kini kategorinya lebih informatif: receiver mencatat `rejectedFrames = 1210`, seluruhnya `qrNotFoundFrames = 1210`, dengan `invalidProtocolFrames = 0` dan `transferIdMismatchFrames = 0`. Sender memancarkan 677 frame dengan 676 systematic frame. Ini mengesampingkan CRC, Base64, envelope, dan transfer-ID sebagai titik pertama kegagalan.

Sesi receiver kedua berakhir dengan `CAMERA_UNAVAILABLE` setelah sekitar 98 detik. Audit lifecycle menemukan receiver dapat memanggil `bindCamera()` melalui event `ON_START` dan sekali lagi melalui pemeriksaan `currentState.isAtLeast(STARTED)` pada efek yang sama. Guard `cameraBound` sekarang mencegah double-bind dan mengurangi risiko konflik CameraX pada re-entry.

Selain guard lifecycle, ukuran block sender diturunkan dari 1.024 menjadi 256 byte. Dengan Base64 URL-safe dan ZXing error correction M, estimasi matrix untuk payload wire sekitar 1.024, 512, dan 256 byte masing-masing adalah 145, 109, dan 81 modul sebelum margin layar. Frame 256-byte memberi lebih banyak piksel per modul pada jarak kamera yang sama, sehingga lebih sesuai untuk QR yang dipindai dari layar ponsel. Quiet zone encoder juga dinaikkan dari margin 2 menjadi 4 modul.

Perubahan kedua ini belum dapat dinyatakan berhasil secara physical-device sampai diagnostics berikutnya menunjukkan `acceptedFrames > 0`.

## Bukti uji ketiga

Percobaan ketiga tetap menunjukkan pola yang sama: sesi receiver dengan 912 `qrNotFoundFrames` dari 912 rejection, 0 accepted frame, dan `CAMERA_UNAVAILABLE` setelah sekitar 113 detik. Sesi tambahan menunjukkan 321 `qrNotFoundFrames` dari 321 rejection, 0 accepted frame, dan error yang sama. Sender memancarkan 786 frame pada satu sesi dan 181 frame pada sesi lain, sehingga sender loop tetap aktif tetapi belum membuktikan bahwa QR terlihat oleh sensor receiver.

Karena perbaikan decoder, block size, QR margin, dan bind guard belum mengubah `qrNotFoundFrames`, diagnosis tidak lagi boleh bergantung pada asumsi ukuran QR saja. Build berikutnya menambahkan telemetry aman untuk:

| Field | Tujuan |
|---|---|
| `lastQrModules` | Membuktikan ukuran matrix QR yang benar-benar dirender sender. |
| `cameraFramesAnalyzed` | Membuktikan analyzer menerima frame CameraX. |
| `lastCameraWidth`, `lastCameraHeight` | Memeriksa resolusi actual image analysis. |
| `lastCameraRowStride`, `lastCameraPixelStride`, `lastCameraBytes` | Memeriksa pemetaan buffer RGBA. |
| `lastCameraRotationDegrees` | Memeriksa orientasi frame yang dianalisis. |
| `lastCameraLumaMin`, `lastCameraLumaMax`, `lastCameraLumaMean` | Mengukur kontras global camera frame tanpa menyimpan gambar. |

Decoder juga sekarang mencoba full frame, center crop 80%, center crop 60%, HybridBinarizer, GlobalHistogramBinarizer, dan pure-barcode fallback. Sender renderer memakai batas pixel integer per QR module untuk mengurangi blur pada ukuran matrix non-integer.

Jika diagnostics berikutnya memiliki `cameraFramesAnalyzed = 0`, masalahnya berada pada camera binding/analyzer lifecycle. Jika `cameraFramesAnalyzed > 0` tetapi luma hampir seragam, receiver tidak melihat layar sender atau exposure/preview bermasalah. Jika luma memiliki rentang lebar dan `lastQrModules` rendah tetapi `qrNotFoundFrames` tetap tinggi, fokus berikutnya adalah format/crop/rotation image input dan bukan fountain protocol.
