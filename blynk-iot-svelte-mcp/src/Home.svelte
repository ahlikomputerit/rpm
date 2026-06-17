<script lang="ts">
  import { onMount, onDestroy } from 'svelte'
  import type { Module } from './types.js'
  import { makeModule, formatEndTime, isInWindow } from './lib/utils.js'
  import { loadModules, saveModules } from './lib/storage.js'
  import { MQTTManager } from './lib/mqtt.js'

  // ─── Init ──────────────────────────────────────────────────────────────────
  const _initial = loadModules()

  // ─── State ─────────────────────────────────────────────────────────────────
  let modules      = $state<Module[]>(_initial)
  let selectedId   = $state(_initial[0]?.id ?? '')
  let currentTime  = $state('')
  let sidebarOpen  = $state(false)          // mobile sidebar toggle
  let activeTab    = $state<'log' | 'info'>('log')
  let showAdd      = $state(false)
  let showDelCfm   = $state(false)
  let editingName  = $state(false)
  let tempName     = $state('')

  // form
  let newName  = $state('')
  let newToken = $state('')
  let newPin   = $state('Power')

  // ─── Toast ─────────────────────────────────────────────────────────────────
  let toasts = $state<{ id: number; msg: string; type: 'ok' | 'err' }[]>([])
  let _tid = 0
  function toast(msg: string, type: 'ok' | 'err' = 'ok') {
    const id = ++_tid
    toasts = [...toasts, { id, msg, type }]
    setTimeout(() => { toasts = toasts.filter(t => t.id !== id) }, 3000)
  }

  // ─── Derived ───────────────────────────────────────────────────────────────
  let sel    = $derived(modules.find(m => m.id === selectedId) ?? modules[0])
  let online = $derived(modules.filter(m => m.isConnected).length)

  // ─── MQTT Manager ──────────────────────────────────────────────────────────
  const mqttMgr = new MQTTManager({
    getModule   : (id) => modules.find(m => m.id === id),
    onConnect   : (id) => {
      const i = modules.findIndex(m => m.id === id)
      if (i < 0) return
      modules[i].isConnected  = true
      modules[i].isConnecting = false
      toast(`✅ ${modules[i].name} terhubung`)
    },
    onDisconnect: (id) => {
      const i = modules.findIndex(m => m.id === id)
      if (i >= 0) { modules[i].isConnected = false; modules[i].isConnecting = false }
    },
    onConnecting: (id) => {
      const i = modules.findIndex(m => m.id === id)
      if (i >= 0) modules[i].isConnecting = true
    },
    onLampState : (id, state) => {
      const i = modules.findIndex(m => m.id === id)
      if (i >= 0) modules[i].lampState = state
    },
    onMessage   : (id, topic, payload) => {
      const i = modules.findIndex(m => m.id === id)
      if (i < 0) return
      modules[i].messages = [
        ...modules[i].messages,
        { topic, payload, timestamp: new Date() },
      ].slice(-100)
    },
  })

  // ─── Jadwal Otomatis ───────────────────────────────────────────────────────
  function checkSchedule() {
    const now    = new Date()
    const nowMin = now.getHours() * 60 + now.getMinutes()
    modules.forEach((mod) => {
      if (!mqttMgr.isConnected(mod.id) || mod.manualMode) return
      const shouldOn = isInWindow(nowMin, mod.pagiHour, mod.pagiDuration) ||
                       isInWindow(nowMin, mod.soreHour, mod.soreDuration)
      if (shouldOn !== mod.lampState) mqttMgr.publish(mod.id, shouldOn, true)
    })
  }

  // ─── Lifecycle ─────────────────────────────────────────────────────────────
  onMount(() => {
    const clockId    = setInterval(() => {
      currentTime = new Date().toLocaleTimeString('id-ID', {
        hour: '2-digit', minute: '2-digit', second: '2-digit',
      })
    }, 1000)
    currentTime = new Date().toLocaleTimeString('id-ID', {
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    })

    checkSchedule()
    const scheduleId = setInterval(checkSchedule, 30_000)

    // Escape key menutup semua modal
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { showAdd = false; showDelCfm = false; sidebarOpen = false }
    }
    window.addEventListener('keydown', onKey)

    return () => {
      clearInterval(clockId)
      clearInterval(scheduleId)
      window.removeEventListener('keydown', onKey)
    }
  })

  onDestroy(() => mqttMgr.destroy())

  $effect(() => { saveModules(modules) })

  // ─── Aksi ──────────────────────────────────────────────────────────────────
  function connectModule(id: string) {
    try { mqttMgr.connect(id) }
    catch (e: any) { toast(e.message ?? 'Gagal connect', 'err') }
  }

  function controlLamp(id: string, state: boolean) {
    if (!mqttMgr.isConnected(id)) { toast('Tidak terhubung ke Blynk', 'err'); return }
    const i = modules.findIndex(m => m.id === id)
    if (i >= 0) modules[i].manualMode = true
    mqttMgr.publish(id, state)
  }

  function addModule() {
    if (!newName.trim())  { toast('Nama modul wajib diisi', 'err'); return }
    if (!newToken.trim()) { toast('Auth Token wajib diisi', 'err'); return }
    const mod  = makeModule(newName.trim(), newToken.trim(), newPin.trim() || 'Power')
    modules    = [...modules, mod]
    selectedId = mod.id
    showAdd    = false; sidebarOpen = false
    newName    = ''; newToken = ''; newPin = 'Power'
    toast(`Modul "${mod.name}" ditambahkan`)
    setTimeout(() => connectModule(mod.id), 150)
  }

  function deleteModule(id: string) {
    if (mqttMgr.isConnected(id)) mqttMgr.disconnect(id)
    modules    = modules.filter(m => m.id !== id)
    if (selectedId === id) selectedId = modules[0]?.id ?? ''
    showDelCfm = false
    toast('Modul dihapus')
  }

  function selectModule(id: string) {
    selectedId  = id
    sidebarOpen = false   // tutup sidebar di mobile setelah pilih
    activeTab   = 'log'
  }

  function setField<K extends keyof Module>(id: string, key: K, val: Module[K]) {
    const i = modules.findIndex(m => m.id === id)
    if (i >= 0) modules[i][key] = val
  }

  function saveName() {
    if (tempName.trim()) setField(sel.id, 'name', tempName.trim())
    editingName = false
  }

  function resetToSchedule(id: string) {
    const i = modules.findIndex(m => m.id === id)
    if (i >= 0) { modules[i].manualMode = false; checkSchedule() }
  }
</script>

<!-- ─── Toast ─────────────────────────────────────────────────────────────── -->
<div class="fixed top-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
  {#each toasts as t (t.id)}
    <div class="px-4 py-2.5 rounded-xl text-sm font-semibold shadow-2xl
      animate-[fadeSlide_0.2s_ease]
      {t.type === 'ok' ? 'bg-cyan-500 text-navy-950' : 'bg-red-500 text-white'}">
      {t.msg}
    </div>
  {/each}
</div>

<!-- ─── Root ──────────────────────────────────────────────────────────────── -->
<div class="h-screen flex flex-col bg-navy-950 overflow-hidden">

  <!-- ── Header ─────────────────────────────────────────────────────────── -->
  <header class="flex-shrink-0 border-b border-navy-700 bg-navy-900 shadow-lg
                 px-4 lg:px-6 py-3 flex items-center justify-between gap-3">
    <!-- Hamburger (mobile only) -->
    <button
      onclick={() => sidebarOpen = !sidebarOpen}
      aria-label="Toggle sidebar"
      class="lg:hidden w-9 h-9 rounded-lg bg-navy-800 border border-navy-700
             flex items-center justify-center text-navy-400 hover:text-white
             hover:bg-navy-700 transition-colors flex-shrink-0"
    >
      {#if sidebarOpen}
        <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      {:else}
        <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      {/if}
    </button>

    <!-- Title -->
    <div class="flex-1 min-w-0">
      <h1 class="text-base lg:text-lg font-bold text-white leading-tight truncate">
        Blynk IoT Dashboard
      </h1>
      <p class="text-[11px] text-cyan-400 hidden sm:block">
        {online}/{modules.length} modul online
      </p>
    </div>

    <!-- Right controls -->
    <div class="flex items-center gap-2 flex-shrink-0">
      <!-- Status badge -->
      <div class="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium
        {online > 0
          ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
          : 'bg-red-500/10 text-red-400 border border-red-500/30'}">
        <span class="w-1.5 h-1.5 rounded-full
          {online > 0 ? 'bg-cyan-400 animate-pulse' : 'bg-red-400'}">
        </span>
        <span class="hidden sm:inline">
          {online > 0 ? `${online} Online` : 'Offline'}
        </span>
        <span class="sm:hidden">{online}</span>
      </div>
      <!-- Clock -->
      <div class="font-mono text-xs text-navy-400 bg-navy-800 px-2.5 py-1.5 rounded-lg
                  border border-navy-700 hidden sm:block">
        {currentTime}
      </div>
      <!-- Add button (always visible) -->
      <button
        onclick={() => showAdd = true}
        aria-label="Tambah modul"
        class="w-8 h-8 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-navy-950
               flex items-center justify-center transition-colors shadow-sm"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
        </svg>
      </button>
    </div>
  </header>

  <!-- ── Body ───────────────────────────────────────────────────────────── -->
  <div class="flex flex-1 overflow-hidden relative">

    <!-- Mobile overlay -->
    {#if sidebarOpen}
      <div
        role="presentation"
        class="fixed inset-0 bg-black/50 z-20 lg:hidden"
        onclick={() => sidebarOpen = false}
        onkeydown={(e) => e.key === 'Escape' && (sidebarOpen = false)}
      ></div>
    {/if}

    <!-- ── Sidebar ──────────────────────────────────────────────────────── -->
    <aside class="
      absolute lg:relative inset-y-0 left-0 z-30 lg:z-auto
      w-64 lg:w-56 xl:w-64 flex-shrink-0
      bg-navy-900 border-r border-navy-700 flex flex-col
      transform transition-transform duration-300 ease-in-out
      {sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
    ">
      <!-- Sidebar header -->
      <div class="px-4 py-3 border-b border-navy-700 flex items-center justify-between">
        <span class="text-xs font-semibold text-navy-500 uppercase tracking-wider">
          Modul ({modules.length})
        </span>
        <button
          onclick={() => showAdd = true}
          aria-label="Tambah modul baru"
          class="w-6 h-6 rounded-md bg-cyan-500/10 text-cyan-400 border border-cyan-500/30
                 hover:bg-cyan-500/20 flex items-center justify-center transition-colors"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
          </svg>
        </button>
      </div>

      <!-- Module list -->
      <div class="flex-1 overflow-y-auto py-2 px-2 space-y-1">
        {#if modules.length === 0}
          <div class="flex items-center justify-center py-12 text-navy-600 text-xs text-center px-4">
            Belum ada modul.<br/>Klik + untuk menambah.
          </div>
        {:else}
          {#each modules as mod (mod.id)}
            <button
              onclick={() => selectModule(mod.id)}
              class="w-full text-left px-3 py-2.5 rounded-xl transition-all border
                {selectedId === mod.id
                  ? 'bg-cyan-500/10 border-cyan-500/25 shadow-sm'
                  : 'hover:bg-navy-800 border-transparent'}"
            >
              <div class="flex items-center gap-2 mb-1">
                <!-- Status dot -->
                <span class="w-2 h-2 rounded-full flex-shrink-0
                  {mod.isConnected ? 'bg-cyan-400'
                    : mod.isConnecting ? 'bg-yellow-400 animate-pulse'
                    : 'bg-navy-600'}">
                </span>
                <span class="font-medium text-sm truncate flex-1
                  {selectedId === mod.id ? 'text-cyan-300' : 'text-white'}">
                  {mod.name}
                </span>
                <!-- Lamp indicator -->
                {#if mod.isConnected}
                  <span class="text-[10px] flex-shrink-0
                    {mod.lampState ? 'text-yellow-400' : 'text-navy-600'}">
                    {mod.lampState ? '●' : '○'}
                  </span>
                {/if}
              </div>
              <div class="flex items-center gap-1.5 ml-4">
                <span class="text-[10px] font-medium px-1.5 py-0.5 rounded-md
                  {mod.isConnected ? 'bg-cyan-500/15 text-cyan-500'
                    : mod.isConnecting ? 'bg-yellow-500/15 text-yellow-500'
                    : 'bg-navy-800 text-navy-600'}">
                  {mod.isConnected ? 'Online' : mod.isConnecting ? 'Connecting…' : 'Offline'}
                </span>
                <span class="text-[10px] text-navy-600 truncate font-mono">
                  {mod.authToken ? `/${mod.virtualPin}` : 'no token'}
                </span>
              </div>
            </button>
          {/each}
        {/if}
      </div>

      <!-- Sidebar footer clock (mobile) -->
      <div class="lg:hidden px-4 py-3 border-t border-navy-700">
        <p class="text-center font-mono text-sm text-navy-400">{currentTime}</p>
      </div>
    </aside>

    <!-- ── Main Panel ──────────────────────────────────────────────────── -->
    <main class="flex-1 overflow-y-auto min-w-0">
      {#if !sel}
        <!-- Empty state -->
        <div class="flex flex-col items-center justify-center h-full gap-4 p-8 text-center">
          <div class="text-6xl">📡</div>
          <div>
            <p class="text-lg font-semibold text-white mb-1">Belum ada modul</p>
            <p class="text-sm text-navy-500">Tambah modul pertama untuk mulai mengontrol perangkat IoT</p>
          </div>
          <button
            onclick={() => showAdd = true}
            class="mt-2 px-5 py-2.5 bg-cyan-500 hover:bg-cyan-400 text-navy-950
                   font-semibold text-sm rounded-xl transition-colors flex items-center gap-2"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
            </svg>
            Tambah Modul Pertama
          </button>
        </div>
      {:else}
        <div class="p-4 lg:p-6 max-w-5xl mx-auto">

          <!-- Modul header -->
          <div class="flex items-center justify-between mb-5 gap-3">
            <div class="flex items-center gap-2 min-w-0">
              {#if editingName}
                <input
                  id="rename-input"
                  bind:value={tempName}
                  onkeydown={(e) => { if (e.key === 'Enter') saveName(); if (e.key === 'Escape') editingName = false }}
                  class="bg-navy-800 border border-cyan-500/50 text-white text-lg font-bold
                         rounded-lg px-3 py-1 w-44 outline-none focus:ring-2 focus:ring-cyan-500/40"
                />
                <button onclick={saveName} aria-label="Simpan nama"
                  class="px-2 py-1 text-xs rounded-lg border border-cyan-500/40 text-cyan-400
                         hover:bg-cyan-500/10">✓</button>
                <button onclick={() => editingName = false} aria-label="Batal"
                  class="px-2 py-1 text-xs rounded-lg border border-navy-600 text-navy-500
                         hover:bg-navy-800">✕</button>
              {:else}
                <h2 class="text-lg font-bold text-white truncate">{sel.name}</h2>
                <button
                  onclick={() => { tempName = sel.name; editingName = true }}
                  aria-label="Rename modul"
                  class="text-navy-600 hover:text-cyan-400 transition-colors flex-shrink-0"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5
                         m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                </button>
              {/if}
            </div>
            <button
              onclick={() => showDelCfm = true}
              class="text-red-400 hover:text-red-300 text-xs flex items-center gap-1.5 px-2.5 py-1.5
                     rounded-lg border border-red-500/20 hover:bg-red-500/10 transition-colors flex-shrink-0"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7
                     m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
              <span class="hidden sm:inline">Hapus</span>
            </button>
          </div>

          <!-- 3-column grid -->
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 lg:gap-5">

            <!-- ── Kolom Kiri ─────────────────────────────────────────── -->
            <div class="space-y-4">

              <!-- Card: Koneksi -->
              <div class="bg-navy-900 rounded-xl border border-navy-700 p-4">
                <p class="text-[10px] font-bold text-navy-500 uppercase tracking-widest mb-3">Koneksi</p>
                <div class="space-y-3">
                  <div>
                    <label for="tok-{sel.id}" class="block text-xs text-navy-500 mb-1">Auth Token</label>
                    <input
                      id="tok-{sel.id}"
                      type="password"
                      class="w-full bg-navy-800 border border-navy-700 text-white text-xs
                             rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-cyan-500/30
                             disabled:opacity-40 font-mono placeholder-navy-600"
                      value={sel.authToken}
                      oninput={(e) => setField(sel.id, 'authToken', (e.target as HTMLInputElement).value)}
                      placeholder="Paste Auth Token Blynk"
                      disabled={sel.isConnected}
                    />
                  </div>
                  <div>
                    <label for="pin-{sel.id}" class="block text-xs text-navy-500 mb-1">Virtual Pin</label>
                    <input
                      id="pin-{sel.id}"
                      type="text"
                      class="w-full bg-navy-800 border border-navy-700 text-white text-xs
                             rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-cyan-500/30
                             disabled:opacity-40 font-mono placeholder-navy-600"
                      value={sel.virtualPin}
                      oninput={(e) => setField(sel.id, 'virtualPin', (e.target as HTMLInputElement).value)}
                      placeholder="Power"
                      disabled={sel.isConnected}
                    />
                    <p class="text-[10px] text-navy-600 mt-1">
                      <code class="text-cyan-500/80">ds/{sel.virtualPin}</code>
                    </p>
                  </div>
                  {#if !sel.isConnected}
                    <button
                      onclick={() => connectModule(sel.id)}
                      disabled={sel.isConnecting || !sel.authToken.trim()}
                      class="w-full py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-navy-950
                             font-semibold text-sm transition-colors
                             disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      {sel.isConnecting ? 'Menghubungkan…' : 'Hubungkan'}
                    </button>
                  {:else}
                    <button
                      onclick={() => mqttMgr.disconnect(sel.id)}
                      class="w-full py-2 rounded-lg border border-navy-600 text-navy-400
                             hover:bg-navy-800 text-sm transition-colors"
                    >Putuskan</button>
                  {/if}
                </div>
              </div>

              <!-- Card: Lampu -->
              <div class="bg-navy-900 rounded-xl border border-cyan-500/20 p-4">
                <p class="text-[10px] font-bold text-navy-500 uppercase tracking-widest mb-3">Kontrol Lampu</p>

                <!-- Lamp visual -->
                <div class="flex justify-center mb-3">
                  <div class="p-4 rounded-full transition-all duration-500
                    {sel.lampState
                      ? 'bg-yellow-400/10 shadow-[0_0_40px_8px_rgba(250,204,21,0.12)]'
                      : 'bg-navy-800'}">
                    <svg class="w-12 h-12 transition-all duration-500
                      {sel.lampState ? 'text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.8)]' : 'text-navy-600'}"
                      fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M9 21h6v-1H9v1zm3-19C8.134 2 5 5.134 5 9c0 2.386 1.154 4.5
                               2.924 5.838C8.575 15.398 9 16.162 9 17v.5h6V17c0-.838.425-1.602
                               1.076-2.162C17.846 13.5 19 11.386 19 9c0-3.866-3.134-7-7-7zm0 2a5 5
                               0 015 5c0 1.645-.803 3.106-2.032 4.014-.638.46-1.143 1.13-1.337
                               1.986H11.37c-.194-.856-.7-1.526-1.337-1.986C8.803 12.106 8 10.645
                               8 9a5 5 0 015-5z"/>
                    </svg>
                  </div>
                </div>

                <div class="text-center mb-3">
                  <p class="text-xl font-bold {sel.lampState ? 'text-yellow-400' : 'text-navy-600'}">
                    {sel.lampState ? 'MENYALA' : 'MATI'}
                  </p>
                  <div class="flex items-center justify-center gap-1.5 mt-1.5 flex-wrap">
                    <span class="text-[10px] px-2 py-0.5 rounded-full border
                      {sel.manualMode
                        ? 'border-purple-500/40 text-purple-400 bg-purple-500/10'
                        : 'border-cyan-500/30 text-cyan-400 bg-cyan-500/10'}">
                      {sel.manualMode ? '🎮 Manual' : '⏰ Jadwal'}
                    </span>
                    {#if sel.manualMode}
                      <button
                        onclick={() => resetToSchedule(sel.id)}
                        class="text-[10px] text-cyan-400 hover:text-cyan-300 underline"
                      >Kembali ke Jadwal</button>
                    {/if}
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                  <button
                    onclick={() => controlLamp(sel.id, true)}
                    disabled={!sel.isConnected}
                    class="py-2.5 rounded-lg text-sm font-bold transition-all
                      {sel.lampState
                        ? 'bg-yellow-400 text-navy-950 shadow-[0_0_20px_rgba(250,204,21,0.25)]'
                        : 'bg-navy-800 text-navy-500 border border-navy-700'}
                      disabled:opacity-30 disabled:cursor-not-allowed"
                  >ON</button>
                  <button
                    onclick={() => controlLamp(sel.id, false)}
                    disabled={!sel.isConnected}
                    class="py-2.5 rounded-lg text-sm font-bold transition-all
                      {!sel.lampState && sel.isConnected
                        ? 'bg-slate-300 text-navy-950'
                        : 'bg-navy-800 text-navy-500 border border-navy-700'}
                      disabled:opacity-30 disabled:cursor-not-allowed"
                  >OFF</button>
                </div>
              </div>

              <!-- Card: Jadwal -->
              <div class="bg-navy-900 rounded-xl border border-navy-700 p-4">
                <p class="text-[10px] font-bold text-navy-500 uppercase tracking-widest mb-1">Jadwal Otomatis</p>
                <p class="text-[10px] text-navy-600 mb-3">Dicek setiap 30 detik</p>

                {#each [
                  { label:'🌅 Pagi', hId:`ph-${sel.id}`, dId:`pd-${sel.id}`,
                    hour: sel.pagiHour, dur: sel.pagiDuration,
                    setH:(v:number)=>setField(sel.id,'pagiHour',v),
                    setD:(v:number)=>setField(sel.id,'pagiDuration',v) },
                  { label:'🌆 Sore', hId:`sh-${sel.id}`, dId:`sd-${sel.id}`,
                    hour: sel.soreHour, dur: sel.soreDuration,
                    setH:(v:number)=>setField(sel.id,'soreHour',v),
                    setD:(v:number)=>setField(sel.id,'soreDuration',v) },
                ] as s}
                  <div class="bg-navy-800 rounded-lg p-3 mb-2 last:mb-0">
                    <p class="text-xs font-medium text-white mb-2">{s.label}</p>
                    <div class="grid grid-cols-2 gap-2">
                      <div>
                        <label for={s.hId} class="block text-[10px] text-navy-500 mb-1">Jam</label>
                        <input id={s.hId} type="number" min="0" max="23"
                          value={s.hour}
                          oninput={(e)=>s.setH(parseInt((e.target as HTMLInputElement).value)||0)}
                          class="w-full text-center bg-navy-700 border border-navy-600 text-white
                                 rounded px-1 py-1 text-xs outline-none focus:ring-1 focus:ring-cyan-500/40"/>
                      </div>
                      <div>
                        <label for={s.dId} class="block text-[10px] text-navy-500 mb-1">Durasi (mnt)</label>
                        <input id={s.dId} type="number" min="1" max="180"
                          value={s.dur}
                          oninput={(e)=>s.setD(parseInt((e.target as HTMLInputElement).value)||1)}
                          class="w-full text-center bg-navy-700 border border-navy-600 text-white
                                 rounded px-1 py-1 text-xs outline-none focus:ring-1 focus:ring-cyan-500/40"/>
                      </div>
                    </div>
                    <p class="text-[10px] text-cyan-500 font-mono mt-1.5">
                      {String(s.hour).padStart(2,'0')}:00 → {formatEndTime(s.hour, s.dur)}
                    </p>
                  </div>
                {/each}
              </div>
            </div>

            <!-- ── Kolom Kanan (2 col) ─────────────────────────────────── -->
            <div class="lg:col-span-2 bg-navy-900 rounded-xl border border-navy-700 flex flex-col
                        min-h-0 overflow-hidden">

              <!-- Tab bar -->
              <div class="flex border-b border-navy-700 flex-shrink-0">
                {#each [
                  { id: 'log',  label: 'Log Aktivitas', badge: sel.messages.length },
                  { id: 'info', label: 'Informasi',     badge: null },
                ] as tab}
                  <button
                    onclick={() => activeTab = tab.id as 'log' | 'info'}
                    class="relative flex items-center gap-2 px-5 py-3.5 text-sm font-medium
                           transition-colors border-b-2 -mb-px
                      {activeTab === tab.id
                        ? 'text-cyan-400 border-cyan-500'
                        : 'text-navy-500 border-transparent hover:text-navy-300'}"
                  >
                    {tab.label}
                    {#if tab.badge !== null && tab.badge > 0}
                      <span class="text-[10px] px-1.5 py-0.5 rounded-full bg-cyan-500/15 text-cyan-400 font-mono">
                        {tab.badge}
                      </span>
                    {/if}
                  </button>
                {/each}
              </div>

              <!-- Tab content -->
              <div class="flex-1 overflow-hidden p-4 lg:p-5">

                <!-- LOG TAB -->
                {#if activeTab === 'log'}
                  <div class="flex flex-col h-full gap-3">
                    <!-- Messages -->
                    <div class="flex-1 bg-navy-950 rounded-xl p-3 overflow-y-auto
                                font-mono text-xs min-h-0">
                      {#if sel.messages.length === 0}
                        <div class="flex flex-col items-center justify-center h-full gap-3 text-navy-700">
                          <svg class="w-10 h-10 opacity-30" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round"
                              d="M8.288 15.038a5.25 5.25 0 017.424 0M5.106 11.856c3.807-3.808 9.98-3.808
                                 13.788 0M1.924 8.674c5.565-5.565 14.587-5.565 20.152 0M12.53
                                 18.22l-.53.53-.53-.53a.75.75 0 011.06 0z"/>
                          </svg>
                          <div class="text-center">
                            <p class="font-medium">Belum ada log</p>
                            <p class="text-[10px] mt-0.5 opacity-60">Hubungkan modul untuk mulai</p>
                          </div>
                        </div>
                      {:else}
                        <div class="space-y-0.5">
                          {#each [...sel.messages].reverse() as msg (msg.timestamp.getTime() + msg.topic)}
                            <div class="flex gap-2 py-1 border-b border-navy-800/40 last:border-0">
                              <span class="text-navy-600 flex-shrink-0 tabular-nums text-[10px] pt-px">
                                {msg.timestamp.toLocaleTimeString('id-ID')}
                              </span>
                              {#if msg.topic === 'system'}
                                <span class="text-cyan-400 break-all">{msg.payload}</span>
                              {:else}
                                <span class="text-navy-500 flex-shrink-0 max-w-[35%] truncate"
                                  title={msg.topic}>{msg.topic}</span>
                                <span class="text-navy-700 mx-0.5">→</span>
                                <span class="text-white break-all">{msg.payload}</span>
                              {/if}
                            </div>
                          {/each}
                        </div>
                      {/if}
                    </div>
                    <!-- Hapus log -->
                    <button
                      onclick={() => setField(sel.id, 'messages', [])}
                      disabled={sel.messages.length === 0}
                      class="py-2 text-xs rounded-lg border border-navy-700 text-navy-500
                             hover:border-red-500/30 hover:text-red-400 transition-colors
                             disabled:opacity-30 disabled:cursor-not-allowed"
                    >Hapus Log</button>
                  </div>

                <!-- INFO TAB -->
                {:else}
                  <div class="space-y-4 overflow-y-auto h-full">

                    <!-- Status grid -->
                    <div class="grid grid-cols-2 gap-3">
                      <div class="bg-navy-800 rounded-xl p-4">
                        <p class="text-[10px] font-bold text-navy-500 uppercase tracking-wider mb-3">Status</p>
                        <div class="space-y-2.5">
                          {#each [
                            ['Koneksi', sel.isConnected ? '🟢 Online'  : '🔴 Offline'],
                            ['Lampu',   sel.lampState   ? '💡 Menyala' : '⚫ Mati'],
                            ['Mode',    sel.manualMode  ? '🎮 Manual'  : '⏰ Jadwal'],
                            ['Waktu',   currentTime],
                          ] as [k, v]}
                            <div class="flex justify-between items-center text-xs gap-2">
                              <span class="text-navy-500 flex-shrink-0">{k}</span>
                              <span class="text-white font-medium text-right">{v}</span>
                            </div>
                          {/each}
                        </div>
                      </div>

                      <div class="bg-navy-800 rounded-xl p-4">
                        <p class="text-[10px] font-bold text-navy-500 uppercase tracking-wider mb-3">MQTT</p>
                        <div class="space-y-2">
                          {#each [
                            ['Broker',   'blynk.cloud:443'],
                            ['Uplink',   `ds/${sel.virtualPin}`],
                            ['Downlink', `downlink/ds/${sel.virtualPin}`],
                            ['Protocol', 'MQTTv4 / WSS'],
                          ] as [k, v]}
                            <div class="text-xs">
                              <span class="text-navy-500">{k}: </span>
                              <code class="text-cyan-400 font-mono break-all">{v}</code>
                            </div>
                          {/each}
                        </div>
                      </div>
                    </div>

                    <!-- Jadwal aktif -->
                    <div class="bg-navy-800 rounded-xl p-4">
                      <p class="text-[10px] font-bold text-navy-500 uppercase tracking-wider mb-3">Jadwal Aktif</p>
                      <div class="grid grid-cols-2 gap-3 text-xs">
                        {#each [
                          { label:'🌅 Pagi', h: sel.pagiHour, d: sel.pagiDuration },
                          { label:'🌆 Sore', h: sel.soreHour, d: sel.soreDuration },
                        ] as j}
                          <div class="bg-navy-750 rounded-lg p-3 border border-navy-700">
                            <p class="font-medium text-white mb-1">{j.label}</p>
                            <p class="text-cyan-400 font-mono">
                              {String(j.h).padStart(2,'0')}:00 – {formatEndTime(j.h, j.d)}
                            </p>
                            <p class="text-navy-500 mt-0.5">{j.d} menit</p>
                          </div>
                        {/each}
                      </div>
                    </div>

                    <!-- Feature list -->
                    <div class="bg-cyan-500/5 border border-cyan-500/20 rounded-xl p-4">
                      <p class="text-[10px] font-bold text-cyan-400 uppercase tracking-wider mb-3">
                        Stack & Fitur
                      </p>
                      <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-[10px] text-navy-400">
                        {#each [
                          'Svelte 5 + TypeScript',
                          'Tailwind CSS 4 (purged 5KB)',
                          'mqtt.js v5 (cached 112KB)',
                          'Vite 6 (code splitting)',
                          'Multi-modul MQTT terpisah',
                          'Jadwal otomatis setiap 30s',
                          'Exponential backoff reconnect',
                          'LocalStorage persistence',
                          'Svelte $state — no stale closure',
                          'Bundle app: 24KB gzip',
                        ] as f}
                          <p>• {f}</p>
                        {/each}
                      </div>
                    </div>
                  </div>
                {/if}
              </div>
            </div>

          </div>
        </div>
      {/if}
    </main>
  </div>
</div>

<!-- ─── Modal: Tambah Modul ───────────────────────────────────────────────── -->
{#if showAdd}
  <div
    role="presentation"
    class="fixed inset-0 bg-black/70 backdrop-blur-sm z-40 flex items-center justify-center p-4"
    onclick={(e) => { if (e.target === e.currentTarget) showAdd = false }}
    onkeydown={(e) => { if (e.key === 'Escape') showAdd = false }}
  >
    <div class="bg-navy-900 border border-navy-700 rounded-2xl p-5 lg:p-6
                w-full max-w-md shadow-2xl">
      <div class="flex items-center justify-between mb-5">
        <h3 class="text-base font-bold text-white flex items-center gap-2">
          <span class="w-7 h-7 rounded-lg bg-cyan-500/10 text-cyan-400 border border-cyan-500/30
                       flex items-center justify-center text-base">+</span>
          Tambah Modul Baru
        </h3>
        <button onclick={() => showAdd = false} aria-label="Tutup"
          class="text-navy-500 hover:text-white w-7 h-7 rounded-lg hover:bg-navy-800
                 flex items-center justify-center transition-colors">✕</button>
      </div>

      <div class="space-y-3">
        <div>
          <label for="new-name" class="block text-xs text-navy-400 mb-1.5">
            Nama Modul <span class="text-red-400">*</span>
          </label>
          <input id="new-name" bind:value={newName}
            placeholder="Contoh: Lampu Taman, Pompa Air"
            onkeydown={(e) => e.key === 'Enter' && addModule()}
            class="w-full bg-navy-800 border border-navy-700 text-white text-sm rounded-xl
                   px-3 py-2.5 outline-none focus:ring-2 focus:ring-cyan-500/30 placeholder-navy-600"/>
        </div>
        <div>
          <label for="new-token" class="block text-xs text-navy-400 mb-1.5">
            Auth Token Blynk <span class="text-red-400">*</span>
          </label>
          <input id="new-token" bind:value={newToken} type="password"
            placeholder="Dari: Blynk Console → Device → Device Info"
            onkeydown={(e) => e.key === 'Enter' && addModule()}
            class="w-full bg-navy-800 border border-navy-700 text-white text-sm rounded-xl
                   px-3 py-2.5 outline-none focus:ring-2 focus:ring-cyan-500/30 font-mono
                   placeholder-navy-600"/>
        </div>
        <div>
          <label for="new-pin" class="block text-xs text-navy-400 mb-1.5">
            Virtual Pin / Datastream Name
          </label>
          <input id="new-pin" bind:value={newPin} placeholder="Power"
            class="w-full bg-navy-800 border border-navy-700 text-white text-sm rounded-xl
                   px-3 py-2.5 outline-none focus:ring-2 focus:ring-cyan-500/30 font-mono
                   placeholder-navy-600"/>
          <p class="text-[10px] text-navy-600 mt-1">
            Uplink topic: <code class="text-cyan-500">ds/{newPin || 'Power'}</code>
          </p>
        </div>
        <div class="bg-cyan-500/5 border border-cyan-500/15 rounded-xl p-3 text-xs text-cyan-400/80">
          💡 Modul akan otomatis terhubung setelah ditambahkan
        </div>
      </div>

      <div class="flex gap-2 mt-5">
        <button onclick={() => showAdd = false}
          class="flex-1 py-2.5 rounded-xl border border-navy-700 text-navy-400
                 hover:bg-navy-800 text-sm transition-colors">
          Batal
        </button>
        <button onclick={addModule}
          disabled={!newName.trim() || !newToken.trim()}
          class="flex-1 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-navy-950
                 font-semibold text-sm transition-colors
                 disabled:opacity-40 disabled:cursor-not-allowed">
          Tambah & Hubungkan
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- ─── Modal: Konfirmasi Hapus ──────────────────────────────────────────── -->
{#if showDelCfm && sel}
  <div
    role="presentation"
    class="fixed inset-0 bg-black/70 backdrop-blur-sm z-40 flex items-center justify-center p-4"
    onclick={(e) => { if (e.target === e.currentTarget) showDelCfm = false }}
    onkeydown={(e) => { if (e.key === 'Escape') showDelCfm = false }}
  >
    <div class="bg-navy-900 border border-red-500/30 rounded-2xl p-5 lg:p-6
                w-full max-w-sm shadow-2xl">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-9 h-9 rounded-xl bg-red-500/10 border border-red-500/20
                    flex items-center justify-center flex-shrink-0">
          <svg class="w-4.5 h-4.5 text-red-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874
                 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/>
          </svg>
        </div>
        <h3 class="text-base font-bold text-red-400">Hapus Modul</h3>
      </div>
      <p class="text-sm text-navy-400 mb-5 ml-12">
        Hapus <strong class="text-white">"{sel.name}"</strong>?
        Koneksi diputus dan semua konfigurasi hilang permanen.
      </p>
      <div class="flex gap-2">
        <button onclick={() => showDelCfm = false}
          class="flex-1 py-2.5 rounded-xl border border-navy-700 text-navy-400
                 hover:bg-navy-800 text-sm transition-colors">
          Batal
        </button>
        <button onclick={() => deleteModule(sel.id)}
          class="flex-1 py-2.5 rounded-xl bg-red-500 hover:bg-red-400 text-white
                 font-semibold text-sm transition-colors">
          Hapus
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  @keyframes fadeSlide {
    from { opacity: 0; transform: translateX(12px); }
    to   { opacity: 1; transform: translateX(0); }
  }
</style>
