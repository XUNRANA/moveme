import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    watch: {
      ignored: ['**/*.err', '**/*.log', '**/*.tmp.*'],
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // SSE 流式响应不能缓冲，必须直接转发
          proxy.on('proxyReq', (_proxyReq, req) => {
            if (req.url?.includes('/recommend/chat')) {
              // 确保不缓冲
            }
          })
          proxy.on('proxyRes', (proxyRes, req) => {
            // 对 SSE 请求禁用缓冲
            if (req.url?.includes('/recommend/chat')) {
              proxyRes.headers['x-accel-buffering'] = 'no'
              proxyRes.headers['cache-control'] = 'no-cache'
            }
          })
        },
      },
      '/static': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
