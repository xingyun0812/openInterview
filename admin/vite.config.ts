import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backend = process.env.VITE_DEV_BACKEND ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true,
      },
    },
  },
})
