import { createApp } from 'vue'
import App from './App.vue'
import router from './router'           // ← 新增

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
app.use(router)                         // ← 新增
app.use(ElementPlus)
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
    app.component(name, comp)
}
app.mount('#app')
