// src/api/User.js
import Request from '@/utils/MDS.js'
import qs from 'qs'

export const apiLogin = (p) => Request.post('/api/user/login', qs.stringify(p))

export const apiRegister = (p) => Request.post('/api/user/register', qs.stringify(p))


export const apiLoginByEmail = (p) =>
     Request.post('/api/user/login-email', qs.stringify(p), {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  }).then(r => (r?.data && r?.data?.reply !== undefined) ? r.data : r)


export const apiSendEmailCode = ({ email }) =>
    Request.post(
        '/api/user/send-email-code',
        qs.stringify({ email: String(email ?? '').trim() }),
        {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            timeout: 15000,        // 放宽超时
            silenceTimeout: true,  // 仅超时静默（配合 MDS.js 拦截器）
        }
    ).then(r => (r?.data && r?.data?.reply !== undefined) ? r.data : r)

export const getUserId = () => Request.get('/api/user/getUserId')