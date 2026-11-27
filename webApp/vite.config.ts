import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

// Librairie Kotlin/JS produite par `npm run kotlin` (module shared:umbrella).
const sharedLibrary = path.resolve(
  __dirname,
  '../shared/umbrella/build/dist/js/productionLibrary/ytblight-shared-umbrella.mjs',
)

export default defineConfig({
  // Site publié sur GitHub Pages dans /ytblight/ (jalon 6) ; HashRouter, donc aucune règle serveur.
  base: process.env.YTBLIGHT_BASE ?? '/',
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      'ytblight-shared': sharedLibrary,
      ws: path.resolve(__dirname, 'src/stubs/empty.ts'),
    },
  },
  server: { fs: { allow: ['..'] } },
})
