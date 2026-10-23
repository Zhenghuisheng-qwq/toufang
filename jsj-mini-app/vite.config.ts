import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const proxy = { '/api': { target: env.H5_API_TARGET || 'http://127.0.0.1:8881', changeOrigin: false } }
  const allowedHosts = (env.H5_ALLOWED_HOSTS || '').split(',').map(value => value.trim()).filter(Boolean)
  return {
    plugins: [vue()],
    server: { host: '127.0.0.1', port: 5174, strictPort: true, allowedHosts, proxy },
    preview: { host: '127.0.0.1', port: 4174, strictPort: true, allowedHosts, proxy }
  }
})
