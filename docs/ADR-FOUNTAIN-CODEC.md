# ADR — Fountain Codec untuk Optical Transfer

- **Status:** Accepted for MVP reliability checkpoint
- **Tanggal:** 2026-08-16
- **Scope:** Sender `FountainFrameSource` dan receiver `FountainDecoder`/`FountainReconstructor`

## Keputusan

Lumen Transfer menggunakan **systematic frames** untuk setiap source block dan menambahkan **repair frames** yang merupakan XOR dari subset source block. Subset dipilih secara deterministik dari `seed`, sehingga sender dan receiver tidak perlu mengirim daftar indeks block di dalam payload. Receiver membentuk persamaan biner dan melakukan pivot reduction di GF(2) dengan `BitSet`.

Frame systematic menggunakan `kind=SYSTEMATIC_DATA`, `seed=sourceBlockIndex`, `degree=1`, dan payload berukuran `blockSize`. Frame repair menggunakan `kind=REPAIR_DATA`, `seed=repairIndex`, `degree` dari distribusi deterministic sederhana, dan payload XOR berukuran `blockSize`. Metadata serta `END` tetap menjadi frame protocol terpisah.

## Alasan

Systematic frames mempertahankan kompatibilitas dan recovery cepat ketika tidak ada frame yang hilang. Repair frames membuat dropped, duplicate, dan out-of-order frame tidak langsung menggagalkan transfer. Receiver tidak perlu menahan seluruh frame yang diterima sebagai daftar mentah; ia menyimpan maksimal satu equation per pivot dan melakukan elimination saat equation baru datang.

Pada checkpoint throughput, repair budget ditetapkan sebesar `max(4, ceil(sourceBlockCount × 0.50))`. Nilai ini adalah baseline engineering, bukan jaminan probabilistik untuk semua pola kehilangan frame. Rasio 50% mengurangi overhead 75% yang membuat file sekitar 1 MB terlalu lama pada transfer optical; physical-device loss-rate benchmark akan menentukan apakah ratio ini cukup atau perlu dinaikkan. SHA-256 dan CRC32 tetap menjaga integritas dan tidak dikurangi.

## Batasan memori

Sender memuat file ke block store bounded dengan batas global `MAX_FILE_BYTES` sebesar 10 MiB. Receiver menyimpan temporary file berukuran sumber dan pivot equations selama proses decode; ukuran praktis bergantung pada jumlah source block, block size, dan jumlah equation yang belum tereliminasi. Setiap payload dibatasi oleh `ProtocolConstants.MAX_PAYLOAD_BYTES`.

## Risiko dan mitigasi

Decoder GF(2) saat ini dirancang untuk sequential block size yang sama dan belum merupakan implementasi Raptor/Luby Transform formal. Jika physical benchmark menunjukkan overhead equation terlalu tinggi atau recovery rate tidak memadai, interface protocol tetap memungkinkan penggantian distribution dan decoder tanpa mengubah UI, storage, atau camera pipeline.

Frame yang sama dapat diterima lebih dari sekali dari kamera. Deduplication key pada ReceiveViewModel tetap digunakan sebelum frame diteruskan ke reconstructor. Integrity akhir tetap ditentukan oleh SHA-256 setelah semua block pulih; CRC32 hanya menangani kerusakan frame pada boundary protocol.
