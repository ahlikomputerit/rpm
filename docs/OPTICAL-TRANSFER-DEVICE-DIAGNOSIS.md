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

Implementasi sebelumnya menambahkan `DecodeHintType.PURE_BARCODE = true` pada `QRCodeReader` yang diberi **seluruh frame kamera**. Hint tersebut cocok untuk bitmap yang sudah berisi QR saja atau telah dicrop menjadi barcode murni. CameraX receiver justru mengirim preview penuh dengan background, perspektif, dan QR berada di sebagian area gambar. Akibatnya ZXing gagal menemukan QR pada setiap image-analysis frame sebelum `FrameSerializer.parse()` dipanggil.

Perbaikan mengubah urutan decoding menjadi berikut:

1. Jalankan ZXing detector normal dengan `TRY_HARDER` dan `POSSIBLE_FORMATS = QR_CODE` pada full camera preview.
2. Gunakan `PURE_BARCODE` hanya sebagai fallback untuk fixture atau input yang memang sudah tercrop.
3. Tidak melakukan rotasi ulang terhadap seluruh buffer kamera. `rotationDegrees` adalah orientasi display; detector QR dapat menemukan finder pattern pada orientasi yang didukung, sementara rotasi global sebelumnya berpotensi mengubah relasi buffer yang sudah disediakan CameraX.
4. Pertahankan pembacaan channel CameraX dalam urutan `A, R, G, B`. Dokumentasi resmi CameraX menjelaskan bahwa `OUTPUT_IMAGE_FORMAT_RGBA_8888` menempatkan alpha, red, green, dan blue pada urutan byte tersebut.[1]

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
