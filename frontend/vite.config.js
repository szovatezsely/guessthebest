import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Proxy /api calls to the Ktor backend during development.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
