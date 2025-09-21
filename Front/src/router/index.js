import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import HealthRecordShow from '@/views/HealthRecordShow.vue'
import Analysis from '@/views/AnalysisPage.vue'
import Visual from "@/views/Visual.vue";
import PermissionManagement from "@/views/PermissionManagementPage.vue";

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/register', component: () => import('@/views/Register.vue') }, // ← 已添加
    { path: '/healthRecordShow', component: HealthRecordShow},
    // 示例：登录后首页
    // { path: '/home', component: () => import('@/views/Home.vue') },
    // 404
    { path: '/:pathMatch(.*)*', redirect: '/login' },
    { path: '/analysis', component: Analysis},
    { path: '/visual', component: Visual},
    { path: '/permissionManagement', component: PermissionManagement}

]

const router = createRouter({
    history: createWebHistory(),
    routes
})


router.beforeEach((to, _from, next) => {
    const publicPages = ['/login', '/register','/healthRecordShow', '/homeTable', '/analysis', '/display', '/permissionManagement']
    const authed =
        localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
    if (!authed && !publicPages.includes(to.path)) return next('/login')
    next()
})

export default router
