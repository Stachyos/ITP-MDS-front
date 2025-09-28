import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import HealthRecordShow from '@/views/HealthRecordShow.vue'
import Analysis from '@/views/AnalysisPage.vue'
import Visual from "@/views/Visual.vue";
import PermissionManagement from "@/views/PermissionManagementPage.vue";
import AuditLogTable from "@/views/AuditLogTable.vue";
import {getPermissionByUserId} from "@/api/Permission.js";
import {getUserId} from "@/api/User.js";

// 路由 + 权限键（与后端 PermissionVo 一致）
const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/register', component: () => import('@/views/Register.vue') },

    { path: '/healthRecordShow', component: HealthRecordShow},
    { path: '/analysis',          component: Analysis,        meta: { perm: 'accessVisualPage' } },
    { path: '/visual',            component: Visual,          meta: { perm: 'accessDisplayPage' } },
    { path: '/permissionManagement', component: PermissionManagement, meta: { perm: 'permissionManagement' } },
    { path: '/auditLogs',         component: AuditLogTable,   meta: { perm: 'accessLogPage' } },

    { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 白名单：无需登录即可直达（通常只登录/注册）
const PUBLIC_PAGES = new Set(['/login', '/register'])

// 统一取后端权限（每次都打接口）
async function fetchPermissions() {
    const r1 = await getUserId()
    const uidStr = (typeof r1 === 'string') ? r1 : (r1?.data ?? r1)
    const userId = Number(uidStr)
    if (!Number.isFinite(userId)) throw new Error('Invalid userId')

    const r2 = await getPermissionByUserId(userId)
    // 兼容 { data: vo } 或直接返回 vo
    return (r2 && r2.data !== undefined) ? r2.data : r2
}

router.beforeEach(async (to, from, next) => {
    try {
        const authed = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')

        // 未登录：仅放行白名单
        if (!authed && !PUBLIC_PAGES.has(to.path)) {
            ElMessage.warning('请先登录')
            return next('/login')
        }

        // 已登录：每次导航都向后端询问权限
        if (authed) {
            const need = to.meta?.perm
            if (need) {
                const perm = await fetchPermissions()
                const ok = !!perm?.[need]
                if (!ok) {
                    ElMessage.error('没有访问该页面的权限')
                    // 停留在当前页。如果首跳来自空白（from 无值），回到一个兜底有权限/公开页
                    if (from.matched.length > 0) return next(false)
                    // 首次直达无权限页：尝试跳到一个可能有权限的页面，否则回登录
                    if (perm?.accessLogPage) return next('/healthRecordShow')
                    if (perm?.accessVisualPage) return next('/analysis')
                    if (perm?.accessDisplayPage) return next('/visual')
                    if (perm?.permissionManagement) return next('/permissionManagement')
                    if (perm?.optionEdit) return next('/auditLogs')
                    return next('/login')
                }
            }
        }

        next()
    } catch (e) {
        // 任何异常（接口失败/解析失败）统一回登录
        console.warn('[router guard] error:', e)
        ElMessage.error('会话校验失败，请重新登录')
        localStorage.removeItem('auth_token')
        sessionStorage.removeItem('auth_token')
        next('/login')
    }
})

export default router
