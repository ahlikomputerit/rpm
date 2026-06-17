import type { Module } from '../types.js'

export function genId(): string {
  return `mod_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

export function makeModule(
  name = 'Modul Baru',
  authToken = '',
  virtualPin = 'Power',
): Module {
  return {
    id: genId(),
    name,
    authToken,
    virtualPin,
    lampState: false,
    manualMode: false,
    pagiHour: 8,
    pagiDuration: 5,
    soreHour: 16,
    soreDuration: 5,
    messages: [],
    isConnected: false,
    isConnecting: false,
  }
}

/** "08:00" + 65 menit → "09:05" */
export function formatEndTime(startHour: number, durationMinutes: number): string {
  const total = startHour * 60 + durationMinutes
  const h = Math.floor(total / 60) % 24
  const m = total % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** Cek apakah nowMinutes berada di dalam window jadwal */
export function isInWindow(
  nowMinutes: number,
  startHour: number,
  durationMinutes: number,
): boolean {
  const start = startHour * 60
  return nowMinutes >= start && nowMinutes < start + durationMinutes
}
