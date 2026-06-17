<script lang="ts">
  import { onMount } from 'svelte'
  import type { Device, Template } from './lib/mcp.js'
  import { MCPManager } from './lib/mcp.js'

  // ─── State ─────────────────────────────────────────────────────────────────
  let mcpManager: MCPManager | null = null
  let accessToken = $state('')
  let isAuthenticated = $state(false)
  let isLoading = $state(false)
  let error = $state<string | null>(null)

  let devices = $state<Device[]>([])
  let templates = $state<Template[]>([])
  let activeTab = $state<'devices' | 'templates' | 'create'>('devices')

  // Form states
  let searchQuery = $state('')
  let createDeviceName = $state('')
  let createDeviceTemplate = $state('')
  let createTemplateName = $state('')
  let createTemplateConnectionType = $state('WiFi')

  // ─── Toast ─────────────────────────────────────────────────────────────────
  let toasts = $state<{ id: number; msg: string; type: 'ok' | 'err' | 'info' }[]>([])
  let _tid = 0
  function toast(msg: string, type: 'ok' | 'err' | 'info' = 'info') {
    const id = ++_tid
    toasts = [...toasts, { id, msg, type }]
    setTimeout(() => { toasts = toasts.filter(t => t.id !== id) }, 3000)
  }

  // ─── Functions ──────────────────────────────────────────────────────────────
  function handleLogin() {
    if (!accessToken.trim()) {
      toast('Access token diperlukan', 'err')
      return
    }
    mcpManager = new MCPManager(accessToken.trim())
    isAuthenticated = true
    toast('✅ Terhubung ke Blynk MCP Server', 'ok')
    loadDevices()
    loadTemplates()
  }

  function handleLogout() {
    mcpManager = null
    isAuthenticated = false
    accessToken = ''
    devices = []
    templates = []
    error = null
    toast('Logout berhasil', 'ok')
  }

  async function loadDevices() {
    if (!mcpManager) return
    isLoading = true
    error = null
    try {
      const result = await mcpManager.searchDevices(
        searchQuery ? { name: searchQuery } : undefined,
      )
      devices = result || []
      toast(`✅ Loaded ${devices.length} device(s)`, 'ok')
    } catch (err: any) {
      error = err.message || 'Gagal memuat devices'
      toast(error, 'err')
    } finally {
      isLoading = false
    }
  }

  async function loadTemplates() {
    if (!mcpManager) return
    isLoading = true
    error = null
    try {
      const result = await mcpManager.getAllTemplates()
      templates = result || []
      toast(`✅ Loaded ${templates.length} template(s)`, 'ok')
    } catch (err: any) {
      error = err.message || 'Gagal memuat templates'
      toast(error, 'err')
    } finally {
      isLoading = false
    }
  }

  async function handleCreateDevice() {
    if (!mcpManager) return
    if (!createDeviceName.trim()) {
      toast('Nama device diperlukan', 'err')
      return
    }
    if (!createDeviceTemplate) {
      toast('Template diperlukan', 'err')
      return
    }

    isLoading = true
    error = null
    try {
      const result = await mcpManager.createDevice({
        name: createDeviceName.trim(),
        template: createDeviceTemplate,
      })
      toast(`✅ Device "${createDeviceName}" berhasil dibuat`, 'ok')
      createDeviceName = ''
      createDeviceTemplate = ''
      await loadDevices()
    } catch (err: any) {
      error = err.message || 'Gagal membuat device'
      toast(error, 'err')
    } finally {
      isLoading = false
    }
  }

  async function handleCreateTemplate() {
    if (!mcpManager) return
    if (!createTemplateName.trim()) {
      toast('Nama template diperlukan', 'err')
      return
    }

    isLoading = true
    error = null
    try {
      const result = await mcpManager.createTemplate({
        name: createTemplateName.trim(),
        connectionType: createTemplateConnectionType as any,
      })
      toast(`✅ Template "${createTemplateName}" berhasil dibuat`, 'ok')
      createTemplateName = ''
      await loadTemplates()
    } catch (err: any) {
      error = err.message || 'Gagal membuat template'
      toast(error, 'err')
    } finally {
      isLoading = false
    }
  }

  async function handleGetDeviceDetails(deviceId: string) {
    if (!mcpManager) return
    isLoading = true
    error = null
    try {
      const device = await mcpManager.getDevice(deviceId)
      toast(`✅ Device details loaded`, 'ok')
      console.log('Device details:', device)
    } catch (err: any) {
      error = err.message || 'Gagal memuat device details'
      toast(error, 'err')
    } finally {
      isLoading = false
    }
  }

  onMount(() => {
    // Load from localStorage if available
    const savedToken = localStorage.getItem('blynk_mcp_token')
    if (savedToken) {
      accessToken = savedToken
      handleLogin()
    }
  })
</script>

<!-- ─── Toast ─────────────────────────────────────────────────────────────── -->
<div class="fixed top-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
  {#each toasts as t (t.id)}
    <div class="px-4 py-2.5 rounded-xl text-sm font-semibold shadow-2xl
      animate-[fadeSlide_0.2s_ease]
      {t.type === 'ok' ? 'bg-cyan-500 text-navy-950' : t.type === 'err' ? 'bg-red-500 text-white' : 'bg-blue-500 text-white'}">
      {t.msg}
    </div>
  {/each}
</div>

<!-- ─── Main Container ─────────────────────────────────────────────────────── -->
<div class="min-h-screen bg-navy-950 p-4 lg:p-6">
  <div class="max-w-6xl mx-auto">
    <!-- Header -->
    <header class="mb-8">
      <h1 class="text-3xl font-bold text-white mb-2">Blynk MCP Integration</h1>
      <p class="text-navy-400">Manage your Blynk IoT devices and templates via MCP API</p>
    </header>

    <!-- Error Banner -->
    {#if error}
      <div class="bg-red-500/10 border border-red-500/30 text-red-400 px-4 py-3 rounded-lg mb-6">
        {error}
      </div>
    {/if}

    <!-- Authentication Section -->
    {#if !isAuthenticated}
      <div class="bg-navy-900 border border-navy-700 rounded-xl p-6 mb-6">
        <h2 class="text-lg font-bold text-white mb-4">Blynk MCP Authentication</h2>
        <div class="space-y-4">
          <div>
            <label for="token" class="block text-sm text-navy-400 mb-2">
              Access Token
            </label>
            <input
              id="token"
              type="password"
              bind:value={accessToken}
              placeholder="Paste your Blynk access token here"
              class="w-full px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                     text-white placeholder-navy-600 focus:outline-none focus:ring-2
                     focus:ring-cyan-500/40"
            />
            <p class="text-xs text-navy-600 mt-2">
              Get your token from Blynk Console → Settings → API Tokens
            </p>
          </div>
          <div class="flex gap-3">
            <button
              onclick={handleLogin}
              disabled={isLoading || !accessToken.trim()}
              class="px-6 py-2 bg-cyan-500 hover:bg-cyan-400 text-navy-950 font-bold
                     rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Connecting...' : 'Login'}
            </button>
          </div>
        </div>
      </div>
    {:else}
      <!-- Authenticated Content -->
      <div class="mb-6 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-3 h-3 rounded-full bg-cyan-400 animate-pulse"></div>
          <span class="text-white font-medium">Connected to Blynk MCP</span>
        </div>
        <button
          onclick={handleLogout}
          class="px-4 py-2 bg-navy-800 hover:bg-navy-700 text-navy-400 hover:text-white
                 border border-navy-700 rounded-lg transition-colors text-sm"
        >
          Logout
        </button>
      </div>

      <!-- Tab Navigation -->
      <div class="flex gap-2 mb-6 border-b border-navy-700">
        {#each [
          { id: 'devices', label: 'Devices' },
          { id: 'templates', label: 'Templates' },
          { id: 'create', label: 'Create' },
        ] as tab}
          <button
            onclick={() => activeTab = tab.id as any}
            class="px-4 py-3 font-medium transition-colors border-b-2 -mb-px
              {activeTab === tab.id
                ? 'text-cyan-400 border-cyan-500'
                : 'text-navy-500 border-transparent hover:text-navy-300'}"
          >
            {tab.label}
          </button>
        {/each}
      </div>

      <!-- Tab Content -->
      <div class="space-y-6">
        <!-- Devices Tab -->
        {#if activeTab === 'devices'}
          <div class="bg-navy-900 border border-navy-700 rounded-xl p-6">
            <div class="flex items-center gap-3 mb-6">
              <h2 class="text-xl font-bold text-white flex-1">Devices</h2>
              <input
                type="text"
                bind:value={searchQuery}
                placeholder="Search devices..."
                class="px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                       text-white placeholder-navy-600 focus:outline-none focus:ring-2
                       focus:ring-cyan-500/40 text-sm"
              />
              <button
                onclick={loadDevices}
                disabled={isLoading}
                class="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-navy-950 font-bold
                       rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Loading...' : 'Refresh'}
              </button>
            </div>

            {#if devices.length === 0}
              <div class="text-center py-12 text-navy-600">
                <p>No devices found</p>
              </div>
            {:else}
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {#each devices as device (device.id)}
                  <div class="bg-navy-800 border border-navy-700 rounded-lg p-4">
                    <h3 class="font-bold text-white mb-2">{device.name}</h3>
                    <p class="text-xs text-navy-500 mb-3">ID: {device.id}</p>
                    {#if device.status}
                      <p class="text-xs text-cyan-400 mb-3">Status: {device.status}</p>
                    {/if}
                    <button
                      onclick={() => handleGetDeviceDetails(device.id)}
                      disabled={isLoading}
                      class="w-full px-3 py-2 bg-navy-700 hover:bg-navy-600 text-navy-300
                             hover:text-white rounded text-xs transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      View Details
                    </button>
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/if}

        <!-- Templates Tab -->
        {#if activeTab === 'templates'}
          <div class="bg-navy-900 border border-navy-700 rounded-xl p-6">
            <div class="flex items-center gap-3 mb-6">
              <h2 class="text-xl font-bold text-white flex-1">Templates</h2>
              <button
                onclick={loadTemplates}
                disabled={isLoading}
                class="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-navy-950 font-bold
                       rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Loading...' : 'Refresh'}
              </button>
            </div>

            {#if templates.length === 0}
              <div class="text-center py-12 text-navy-600">
                <p>No templates found</p>
              </div>
            {:else}
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                {#each templates as template (template.id)}
                  <div class="bg-navy-800 border border-navy-700 rounded-lg p-4">
                    <h3 class="font-bold text-white mb-2">{template.name}</h3>
                    <p class="text-xs text-navy-500 mb-3">ID: {template.id}</p>
                    {#if template.description}
                      <p class="text-xs text-navy-400 mb-3">{template.description}</p>
                    {/if}
                    {#if template.datastreams}
                      <p class="text-xs text-cyan-400">
                        Datastreams: {template.datastreams.length}
                      </p>
                    {/if}
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/if}

        <!-- Create Tab -->
        {#if activeTab === 'create'}
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- Create Device -->
            <div class="bg-navy-900 border border-navy-700 rounded-xl p-6">
              <h2 class="text-lg font-bold text-white mb-4">Create Device</h2>
              <div class="space-y-4">
                <div>
                  <label for="dev-name" class="block text-sm text-navy-400 mb-2">
                    Device Name
                  </label>
                  <input
                    id="dev-name"
                    type="text"
                    bind:value={createDeviceName}
                    placeholder="My Device"
                    class="w-full px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                           text-white placeholder-navy-600 focus:outline-none focus:ring-2
                           focus:ring-cyan-500/40"
                  />
                </div>
                <div>
                  <label for="dev-template" class="block text-sm text-navy-400 mb-2">
                    Template
                  </label>
                  <select
                    id="dev-template"
                    bind:value={createDeviceTemplate}
                    class="w-full px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                           text-white focus:outline-none focus:ring-2 focus:ring-cyan-500/40"
                  >
                    <option value="">Select a template...</option>
                    {#each templates as template (template.id)}
                      <option value={template.id}>{template.name}</option>
                    {/each}
                  </select>
                </div>
                <button
                  onclick={handleCreateDevice}
                  disabled={isLoading || !createDeviceName.trim() || !createDeviceTemplate}
                  class="w-full px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-navy-950 font-bold
                         rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLoading ? 'Creating...' : 'Create Device'}
                </button>
              </div>
            </div>

            <!-- Create Template -->
            <div class="bg-navy-900 border border-navy-700 rounded-xl p-6">
              <h2 class="text-lg font-bold text-white mb-4">Create Template</h2>
              <div class="space-y-4">
                <div>
                  <label for="tmpl-name" class="block text-sm text-navy-400 mb-2">
                    Template Name
                  </label>
                  <input
                    id="tmpl-name"
                    type="text"
                    bind:value={createTemplateName}
                    placeholder="My Template"
                    class="w-full px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                           text-white placeholder-navy-600 focus:outline-none focus:ring-2
                           focus:ring-cyan-500/40"
                  />
                </div>
                <div>
                  <label for="conn-type" class="block text-sm text-navy-400 mb-2">
                    Connection Type
                  </label>
                  <select
                    id="conn-type"
                    bind:value={createTemplateConnectionType}
                    class="w-full px-4 py-2 bg-navy-800 border border-navy-700 rounded-lg
                           text-white focus:outline-none focus:ring-2 focus:ring-cyan-500/40"
                  >
                    <option value="WiFi">WiFi</option>
                    <option value="Cellular">Cellular</option>
                    <option value="Ethernet">Ethernet</option>
                    <option value="BLE">BLE</option>
                  </select>
                </div>
                <button
                  onclick={handleCreateTemplate}
                  disabled={isLoading || !createTemplateName.trim()}
                  class="w-full px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-navy-950 font-bold
                         rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLoading ? 'Creating...' : 'Create Template'}
                </button>
              </div>
            </div>
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  @keyframes fadeSlide {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
</style>
