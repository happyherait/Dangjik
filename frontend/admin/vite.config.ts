import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: './',
  server: {
    proxy: {
      '/api': 'http://localhost:18082',
    },
  },
  test: {
    environment: 'jsdom',
  },
})
