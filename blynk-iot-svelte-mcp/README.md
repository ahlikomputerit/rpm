# Blynk IoT Dashboard — Svelte Edition

## Stack

| Layer       | Teknologi          | Alasan                                         |
|-------------|--------------------|-------------------------------------------------|
| UI Framework | **Svelte 5**      | Compiler, zero runtime overhead                 |
| Bahasa      | **TypeScript**     | Type safety untuk state multi-modul yang kompleks |
| Styling     | **Tailwind CSS 4** | Purged ~5KB gzip, no config file needed         |
| IoT Protocol| **mqtt.js**        | Satu-satunya dependensi runtime yang wajib      |
| Build       | **Vite 6**         | Bundling + code splitting mqtt ke chunk terpisah |

**Total dependensi: 7** (vs 50+ pada versi React sebelumnya)

## Bundle Size

```
dist/assets/index.css      →   5.32 KB gzip   (Tailwind CSS)
dist/assets/index.js       →  24.01 KB gzip   (Svelte app)
dist/assets/mqtt.js        → 111.86 KB gzip   (mqtt.js, di-cache terpisah)
─────────────────────────────────────────────
First load total           → ~141 KB gzip
Kunjungan berikutnya       →  ~30 KB gzip     (mqtt.js sudah di-cache browser)
```

**Vs versi React + shadcn/ui sebelumnya: ~300–500 KB gzip (5x lebih berat)**

## Kenapa Svelte — Bukan Vue / Preact / Vanilla

- **Bukan Vue**: runtime ~33KB + butuh wrapper library untuk reaktivitas real-time MQTT
- **Bukan Preact**: masih perlu `useRef` tricks untuk stale closure, hanya 3KB lebih kecil dari React
- **Bukan Vanilla**: state multi-modul tanpa reaktivitas = DOM manual = rawan bug
- **Svelte**: dikompilasi ke pure JS → nol virtual DOM → reaktivitas native di level compiler

### Keunggulan Svelte untuk project ini

**1. Tidak ada stale closure** — Svelte `$state` adalah JavaScript Proxy.
Membaca `modules` di dalam `setInterval` atau MQTT callback SELALU mendapat nilai terkini.
Tidak perlu `useRef`, `modulesRef.current`, atau workaround lain seperti di React.

```typescript
// React (perlu workaround)
const modulesRef = useRef(modules)
useEffect(() => { modulesRef.current = modules }, [modules])
setInterval(() => modulesRef.current.forEach(...), 30000) // harus pakai ref!

// Svelte (langsung benar)
let modules = $state([...])
setInterval(() => modules.forEach(...), 30000) // selalu fresh! ✅
```

**2. Mutasi langsung** — tidak perlu spread/map untuk update state nested.
```typescript
// React
setModules(prev => prev.map(m => m.id === id ? {...m, lampState: true} : m))

// Svelte
modules[i].lampState = true  // ✅ UI langsung update
```

**3. Reactivity tanpa re-render** — hanya DOM node yang berubah yang di-update,
bukan seluruh component tree.

## Cara Menjalankan

```bash
npm install
npm run dev       # Development server → http://localhost:5173
npm run build     # Production build → ./dist/
npm run preview   # Preview production build
```

## Cara Menambah Modul Baru

1. Klik tombol **"+"** di sidebar (atau tombol "Tambah Modul" di bawah)
2. Isi **Nama Modul** — bebas, untuk identifikasi
3. Paste **Auth Token** dari Blynk Console → Device → Device Info
4. Atur **Virtual Pin** jika berbeda dari default `Power`
   - Uplink topic: `ds/Power`
   - Downlink topic: `downlink/ds/Power`
5. Klik **"Tambah & Hubungkan"** → auto-connect

## Fitur

- ✅ **Multi-modul** — tiap modul punya MQTT connection terpisah
- ✅ **Jadwal otomatis** — dicek setiap 30 detik, benar-benar berfungsi
- ✅ **Kembali ke Jadwal** — setelah kontrol manual bisa kembali ke mode otomatis
- ✅ **LocalStorage persistence** — konfigurasi tetap ada setelah refresh
- ✅ **Virtual pin configurable** — tidak hardcoded
- ✅ **End-time kalkulasi benar** — 08:00 + 65 menit = 09:05 (bukan "08:65")
- ✅ **Exponential backoff** — reconnect dengan jeda yang makin panjang
- ✅ **Heartbeat ping** — koneksi dijaga aktif setiap 30 detik
- ✅ **Rename modul** — edit nama inline
- ✅ **Hapus modul** — dengan konfirmasi, auto-disconnect

## Struktur File

```
src/
├── types.ts          Interface TypeScript (Module, MQTTMessage, SavedModule)
├── lib/
│   ├── utils.ts      Helper: genId, makeModule, formatEndTime, isInWindow
│   ├── storage.ts    localStorage: saveModules, loadModules
│   └── mqtt.ts       MQTTManager class — semua logika koneksi
├── App.svelte        Root component
├── Home.svelte       Dashboard utama (state, UI, lifecycle)
├── main.ts           Entry point
└── app.css           Tailwind + custom CSS variables (dark theme)
```
