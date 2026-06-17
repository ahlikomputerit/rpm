import type { Module, SavedModule } from '../types.js'
import { makeModule } from './utils.js'

const KEY = 'blynk-modules-v2'

export function saveModules(modules: Module[]): void {
  try {
    const data: SavedModule[] = modules.map(
      ({ id, name, authToken, virtualPin, manualMode,
         pagiHour, pagiDuration, soreHour, soreDuration }) =>
        ({ id, name, authToken, virtualPin, manualMode,
           pagiHour, pagiDuration, soreHour, soreDuration }),
    )
    localStorage.setItem(KEY, JSON.stringify(data))
  } catch (_) {}
}

export function loadModules(): Module[] {
  try {
    const raw = localStorage.getItem(KEY)
    if (raw) {
      const data: SavedModule[] = JSON.parse(raw)
      return data.map((d) => ({
        ...makeModule(d.name, d.authToken, d.virtualPin),
        id: d.id,
        manualMode: d.manualMode ?? false,
        pagiHour: d.pagiHour ?? 8,
        pagiDuration: d.pagiDuration ?? 5,
        soreHour: d.soreHour ?? 16,
        soreDuration: d.soreDuration ?? 5,
      }))
    }
  } catch (_) {}
  // Default: 1 modul kosong
  return [makeModule('Lampu Utama')]
}
