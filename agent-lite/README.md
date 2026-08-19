# PCA-Lite Agent Runtime

PCA-Lite adalah MVP single-user browser agent yang dirancang untuk laptop atau VPS kecil. Ia memakai FastAPI, SQLite WAL, satu worker Playwright, provider model remote opsional, approval manusia, domain allowlist, dan event audit.

## Quick start

Dari directory ini:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m playwright install chromium
cp .env.example .env
uvicorn app.main:app --host 127.0.0.1 --port 8787
```

Buka `http://127.0.0.1:8787`.

Default `AGENT_MODEL_PROVIDER=demo` tidak memanggil model eksternal. Demo akan membuka URL yang diizinkan, mengambil screenshot, membaca `body`, lalu menyelesaikan task tanpa aksi write. Untuk planning bebas, isi `AGENT_MODEL_PROVIDER`, `AGENT_MODEL_API_KEY`, dan `AGENT_MODEL_BASE_URL` dengan endpoint OpenAI-compatible yang Anda pilih.

## Security defaults

Service hanya bind ke `127.0.0.1` secara default. Browser memakai context baru untuk setiap task. Navigation harus melewati domain allowlist. Aksi Enter, submit-like, send-like, delete-like, purchase-like, publish-like, login, consent, dan pola side effect lain akan meminta approval. SQLite, event log, dan workspace berada di directory `data/`.

Jangan membuka port service langsung ke internet tanpa reverse proxy TLS, authentication, firewall, dan pembatasan origin. Jangan memakai browser profile utama sebelum local desktop bridge dan explicit grant benar-benar diimplementasikan.

## Resource profile

MVP menargetkan satu task aktif, satu Chromium context, tanpa local LLM, tanpa PostgreSQL, dan tanpa Redis. Pada VPS 4 GB, gunakan headless browser, satu task, retention artifact, dan swap kecil sebagai safety net. Jangan menjalankan beberapa browser atau model lokal besar pada profile ini.

## Current scope

Yang sudah tersedia adalah task state machine, SQLite persistence, demo/model adapter, browser actions dasar, domain allowlist, approval state, cancellation, emergency stop, audit events, dan dashboard ringan. Desktop control melalui Cua Driver belum diaktifkan pada MVP ini; itu adalah tahap lanjutan setelah browser workflow stabil.
