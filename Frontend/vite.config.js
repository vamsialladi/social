import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import path from 'path'
import os from 'os'

// Store cache in system temp directory to prevent Windows OneDrive file-locking (EPERM)
export default defineConfig({
  plugins: [react()],
  cacheDir: path.join(os.tmpdir(), 'truehunt-vite-cache'),
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
