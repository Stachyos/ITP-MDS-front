import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 系统 token（用于你自己的后端）
 */
export const useTokenStore = defineStore(
    'userToken',
    () => {
        const token = ref('')

        /**
         * 设置 token
         * @param {string} newToken
         * @param {boolean} remember  记住我：true -> localStorage; false -> sessionStorage
         */
        function setToken(newToken, remember = true) {
            token.value = newToken || ''
            const store = remember ? localStorage : sessionStorage
            store.setItem('auth_token', token.value)
            // 同步清掉另一个存储，避免两个位置都有
            ;(remember ? sessionStorage : localStorage).removeItem('auth_token')
        }

        /** 清除 token（两个存储都清） */
        function removeToken() {
            token.value = ''
            localStorage.removeItem('auth_token')
            sessionStorage.removeItem('auth_token')
        }

        /** 方便 axios 直接用 */
        const authHeader = () => (token.value ? { Authorization: token.value } : {})

        return { token, setToken, removeToken, authHeader }
    },
    {
        // 持久化 Pinia 状态（提高刷新恢复速度）；真正权威仍以 storage 为准
        persist: {
            key: 'userToken',
            storage: localStorage, // 这里只是保存 Pinia 状态；真正落盘用 setToken 控制 local/session
            paths: ['token']
        }
    }
)

/**
 * 门户 token（如果你还有另一套网关/单点）
 */
export const usePortalTokenStore = defineStore(
    'portalToken',
    () => {
        const token = ref('')

        function setToken(newToken, remember = true) {
            token.value = newToken || ''
            const store = remember ? localStorage : sessionStorage
            store.setItem('portal_auth_token', token.value)
            ;(remember ? sessionStorage : localStorage).removeItem('portal_auth_token')
        }

        function removeToken() {
            token.value = ''
            localStorage.removeItem('portal_auth_token')
            sessionStorage.removeItem('portal_auth_token')
        }

        const authHeader = () => (token.value ? { Authorization: token.value } : {})

        return { token, setToken, removeToken, authHeader }
    },
    {
        persist: {
            key: 'portalToken',
            storage: localStorage,
            paths: ['token']
        }
    }
)
