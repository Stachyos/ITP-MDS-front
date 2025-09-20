// vite.config.js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// ↓ 选配：Element Plus 自动引入
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
    plugins: [
        vue(),
        vueDevTools(),
        // 选配：自动引入 Element Plus 组件/指令/样式
        AutoImport({ resolvers: [ElementPlusResolver()] }),
        Components({ resolvers: [ElementPlusResolver()] }),
    ],
    resolve: {
        alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
        host: '0.0.0.0',
        port: 7488,
        open: true,
        // 反向代理配置：把 /api 的请求转发到后端
        proxy: {
            '/api': {
                target: 'http://localhost:7486', // ← 改成你的后端地址
                changeOrigin: true,
                // 如果后端不是以 /api 开头，可用 rewrite 去掉前缀：
                // rewrite: path => path.replace(/^\/api/, ''),
                // 如果是 https 自签名证书：
                // secure: false,
            },
        },
    },
})
