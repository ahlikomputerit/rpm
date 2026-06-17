import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [tailwindcss(), svelte()],
  build: {
    target: 'es2020',
    chunkSizeWarningLimit: 200,
    rollupOptions: {
      output: {
        // Pisahkan mqtt.js ke chunk sendiri agar bisa di-cache browser
        manualChunks: { mqtt: ['mqtt'] },
      },
    },
  },
})
