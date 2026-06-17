import mqtt, { type MqttClient } from 'mqtt'
import type { Module } from '../types.js'

const BROKER_URL         = 'wss://blynk.cloud:443/mqtt'
const HEARTBEAT_INTERVAL = 30_000   // 30 detik
const MAX_RECONNECT_DEL  = 30_000   // max backoff 30 detik
const MAX_ATTEMPTS       = 15

export interface MQTTCallbacks {
  onConnect    : (id: string) => void
  onDisconnect : (id: string) => void
  onConnecting : (id: string) => void
  onLampState  : (id: string, state: boolean) => void
  onMessage    : (id: string, topic: string, payload: string) => void
  getModule    : (id: string) => Module | undefined
}

export class MQTTManager {
  private clients    : Record<string, MqttClient | null> = {}
  private connState  : Record<string, boolean>  = {}
  private retryTOs   : Record<string, ReturnType<typeof setTimeout>  | null> = {}
  private heartbeats : Record<string, ReturnType<typeof setInterval> | null> = {}
  private attempts   : Record<string, number>   = {}
  private manualDC   : Record<string, boolean>  = {}

  constructor(private cb: MQTTCallbacks) {}

  // ── public API ─────────────────────────────────────────────────────────────

  isConnected(id: string): boolean { return this.connState[id] ?? false }

  connect(id: string): void {
    const mod = this.cb.getModule(id)
    if (!mod?.authToken.trim()) throw new Error('Auth Token kosong')

    this.attempts[id] = 0
    this.manualDC[id] = false
    this.cb.onConnecting(id)
    this._connect(id)
  }

  disconnect(id: string): void {
    this.manualDC[id]  = true
    this.connState[id] = false
    this._clearTimers(id)

    const client = this.clients[id]
    if (client) {
      client.end(true, undefined, () => {
        this.clients[id] = null
        this.cb.onDisconnect(id)
        this.cb.onMessage(id, 'system', '🛑 Disconnected by user')
      })
    } else {
      this.cb.onDisconnect(id)
    }
  }

  publish(id: string, state: boolean, isSchedule = false): void {
    const mod    = this.cb.getModule(id)
    const client = this.clients[id]
    if (!client || !this.connState[id] || !mod) return

    client.publish(
      `ds/${mod.virtualPin}`,
      state ? '1' : '0',
      { qos: 1 },
      (err) => {
        if (err) {
          this.cb.onMessage(id, 'system', `❌ Publish error: ${err}`)
        } else {
          this.cb.onLampState(id, state)
          this.cb.onMessage(
            id,
            `uplink/ds/${mod.virtualPin}`,
            `${state ? 'ON' : 'OFF'}${isSchedule ? ' (jadwal otomatis)' : ''}`,
          )
        }
      },
    )
  }

  destroy(): void {
    Object.keys(this.clients).forEach((id) => {
      this.manualDC[id] = true
      this._clearTimers(id)
      this.clients[id]?.end(true)
    })
  }

  // ── private ────────────────────────────────────────────────────────────────

  private _clearTimers(id: string): void {
    if (this.retryTOs[id])   { clearTimeout(this.retryTOs[id]!);   this.retryTOs[id]   = null }
    if (this.heartbeats[id]) { clearInterval(this.heartbeats[id]!); this.heartbeats[id] = null }
  }

  private _backoff(attempt: number): number {
    return Math.min(1000 * 2 ** attempt, MAX_RECONNECT_DEL) + Math.random() * 1000
  }

  private _connect(id: string): void {
    const mod = this.cb.getModule(id)
    if (!mod) return

    const client = mqtt.connect(BROKER_URL, {
      username        : 'device',
      password        : mod.authToken.trim(),
      keepalive       : 30,
      clean           : true,
      connectTimeout  : 10_000,
      reconnectPeriod : 0,           // manual reconnect
      protocolVersion : 4,
      clientId        : `blynk_web_${id}_${Date.now()}`,
    })

    this.clients[id] = client

    // ── connect ──────────────────────────────────────────────────────────────
    client.on('connect', () => {
      this.connState[id] = true
      this.attempts[id]  = 0
      this.cb.onConnect(id)
      this.cb.onMessage(id, 'system', '✅ Terhubung ke Blynk MQTT Broker')

      // Heartbeat ping agar koneksi tidak di-drop server
      this.heartbeats[id] = setInterval(() => {
        try { this.clients[id]?.ping?.() } catch (_) {}
      }, HEARTBEAT_INTERVAL)

      // Subscribe semua downlink
      client.subscribe('downlink/#', { qos: 1 }, (err) => {
        this.cb.onMessage(
          id, 'system',
          err ? `⚠️ Subscribe error: ${err.message}` : '📡 Subscribed ke downlink/#',
        )
      })
    })

    // ── message ───────────────────────────────────────────────────────────────
    client.on('message', (topic, payload) => {
      const str = payload.toString()
      // Re-read modul setiap kali — cb.getModule membaca Svelte $state proxy (selalu fresh)
      const m = this.cb.getModule(id)
      if (!m) return

      if (topic === 'downlink/redirect') {
        this.cb.onMessage(id, 'system', `📍 Server redirect: ${str}`)
      } else if (topic === `downlink/ds/${m.virtualPin}`) {
        const state = parseInt(str) !== 0
        this.cb.onLampState(id, state)
        this.cb.onMessage(id, topic, state ? 'ON' : 'OFF')
      } else {
        this.cb.onMessage(id, topic, str)
      }
    })

    // ── error ─────────────────────────────────────────────────────────────────
    client.on('error', (err) => {
      const msg = (err.message || String(err)).toLowerCase()
      if (msg.includes('password') || msg.includes('unauthorized') || msg.includes('not authorized')) {
        this.cb.onMessage(id, 'system', '❌ Auth Token tidak valid — periksa kembali')
        this.connState[id] = false
        this._clearTimers(id)
        this.cb.onDisconnect(id)
      } else {
        this.cb.onMessage(id, 'system', `⚠️ Error: ${err.message}`)
      }
    })

    // ── close (reconnect logic) ───────────────────────────────────────────────
    client.on('close', () => {
      this._clearTimers(id)

      // connState dipakai (bukan React state) → tidak ada stale closure
      if (!this.manualDC[id] && this.connState[id]) {
        this.connState[id] = false
        this.cb.onDisconnect(id)
        this.cb.onMessage(id, 'system', '⚠️ Koneksi terputus, mencoba reconnect...')

        const attempt = this.attempts[id] ?? 0
        if (attempt < MAX_ATTEMPTS) {
          const delay = this._backoff(attempt)
          this.attempts[id] = attempt + 1
          this.cb.onMessage(
            id, 'system',
            `🔄 Reconnect #${attempt + 1} dalam ${Math.round(delay / 1000)}s`,
          )
          this.retryTOs[id] = setTimeout(() => {
            if (!this.manualDC[id]) this._connect(id)
          }, delay)
        } else {
          this.cb.onMessage(id, 'system', '❌ Gagal reconnect: melebihi batas percobaan')
        }
      }
    })

    client.on('offline', () =>
      this.cb.onMessage(id, 'system', '📴 Client offline'))
  }
}
