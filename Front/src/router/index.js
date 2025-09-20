import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/register', component: () => import('@/views/Register.vue') }, // ← 已添加
    { path: '/visual', component: () => import('@/views/visual.vue') },

    // 示例：登录后首页
    // { path: '/home', component: () => import('@/views/Home.vue') },
    // 404
    { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})


router.beforeEach((to, _from, next) => {
    const publicPages = ['/login', '/register','/visual']
    const authed =
        localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
    if (!authed && !publicPages.includes(to.path)) return next('/login')
    next()
})

export default router
