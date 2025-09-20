import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    // 之后可继续加：{ path: '/register', component: () => import('@/views/Register.vue') },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router
