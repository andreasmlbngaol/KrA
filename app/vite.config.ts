import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Minimal declaration so tsc accepts process.env without @types/node.
declare const process: { env: Record<string, string | undefined> }

// The dev server proxies /api to the Spring server to avoid CORS. Locally this is
// localhost:8083; inside the Docker dev setup it is overridden to the "server"
// service via VITE_PROXY_TARGET.
const proxyTarget = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8083'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
      },
    },
  },
})
