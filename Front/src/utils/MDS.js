// src/utils/MDS.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/utils/stores/token.js'
import router from '@/router/index.js'
import { ENVI_DEV_SERVER_ADDRESS, ENVI_PROD_SERVER_ADDRESS } from '@/utils/globalCfg'

const ENVIRONMENT = import.meta.env
const URL = ENVIRONMENT.PROD ? ENVI_PROD_SERVER_ADDRESS : ENVI_DEV_SERVER_ADDRESS

const axiosInstance = axios.create({
    baseURL: URL,
    timeout: 3000,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }
})

// === 请求拦截器：携带 token ===
axiosInstance.interceptors.request.use(
    (config) => {
        // 1) 优先从 Pinia 取
        let token
        try {
            const store = useTokenStore()    // 若 Pinia 未初始化会抛错
            token = store?.token
        } catch (_) {
            // 2) 兜底：从本地存储取（和你登录时写入的 key 对齐）
            token = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
        }

        if (token) {
            // 统一 Bearer 格式（后端拦截器已兼容的话也可以直接用纯 token）
            config.headers.Authorization = `Bearer ${token}`
        }

        return config
    },
    (err) => Promise.reject(err)
)

// === 响应拦截器：处理 401 等 ===
axiosInstance.interceptors.response.use(
    // 成功：直接返回 data（保持原有行为）
    (res) => res.data,
    // 失败分支
    (err) => {
        const cfg = err?.config || {}
        const status = err?.response?.status

        // ===== 关键新增：仅超时静默 =====
        // 客户端超时（Axios 的 code/message）、以及常见服务侧超时状态
        const isClientTimeout =
            err?.code === 'ECONNABORTED' ||
            /timeout of \d+ms exceeded/i.test(err?.message || '') ||
            /ETIMEDOUT/i.test(err?.code || '')
        const isServerTimeout = status === 408 || status === 504

        // 若该请求显式要求对“超时”静默，则不弹错误提示，直接把错误交给调用方处理
        if ((isClientTimeout || isServerTimeout) && cfg.silenceTimeout) {
            return Promise.reject(err)
        }
        // ===== 关键新增结束 =====

        if (err.response) {
            if (status === 401) {
                ElMessage.warning('请先登录')
                try {
                    const store = useTokenStore()
                    store.removeToken?.()
                } catch (_) {
                    // 兜底清理
                    localStorage.removeItem('auth_token')
                    sessionStorage.removeItem('auth_token')
                }
                router.push('/login')
            } else {
                ElMessage.error('服务器状态错误')
            }
        } else {
            ElMessage.error('网络错误')
        }
        return Promise.reject(err)
    }
)

// 应用启动时，从本地把 token 写进默认头（避免首个请求漏带）
{
    const bootToken = localStorage.getItem('auth_token') || sessionStorage.getItem('auth_token')
    if (bootToken) {
        axiosInstance.defaults.headers.common.Authorization = `Bearer ${bootToken}`
    }
}

// 便捷方法：登录/登出时设置/清空默认头
export function setAuthToken(token) {
    if (token) {
        axiosInstance.defaults.headers.common.Authorization = `Bearer ${token}`
    } else {
        delete axiosInstance.defaults.headers.common.Authorization
    }
}

export default axiosInstance
