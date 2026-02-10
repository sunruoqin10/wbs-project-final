import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    host: '0.0.0.0',  // 监听所有网络接口，支持局域网访问
    port: 3000,
    open: true,
    proxy: {
      // 代理 /api 请求到后端服务器
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 不重写路径，保留 /api 前缀
      }
    }
  }
})
