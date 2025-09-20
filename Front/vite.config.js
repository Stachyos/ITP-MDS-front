import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
    plugins: [vue(), vueDevTools()],
    resolve: {
        alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
        port: 7488,       // ← 改这里
        host: '0.0.0.0',  // 可选：允许局域网访问
        open: true        // 可选：启动自动打开浏览器
        // proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } // 如需反向代理
    }
})
