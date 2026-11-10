import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import path from 'path'
import os from 'os'

export default defineConfig({
  plugins: [react()],
  cacheDir: path.join(os.tmpdir(), 'truehunt-vite-cache'),
  define: {
    global: 'window',
  },

  server: {
    host: 'localhost',
    port: 5173,

    warmup: {
      clientFiles: [
        './src/main.jsx',
        './src/App.jsx',
      ],
    },

    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },

  optimizeDeps: {
    include: ['react', 'react-dom'],
  },

  build: {
    target: 'es2020',
    minify: 'esbuild',
    sourcemap: false,
    cssCodeSplit: true,
  },
});
