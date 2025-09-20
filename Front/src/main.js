import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from '@/utils/stores'            // ← 引入同一个 pinia 实例

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as Icons from '@element-plus/icons-vue'

const app = createApp(App)
app.use(router)
app.use(pinia)                              // ← 挂载
app.use(ElementPlus)
for (const [k, c] of Object.entries(Icons)) app.component(k, c)
app.mount('#app')
