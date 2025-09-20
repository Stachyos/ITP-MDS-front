// src/api/user.js
import Request from '@/utils/MDS.js'
import qs from 'qs'

export const apiLogin = (p) => Request.post('/api/user/login', qs.stringify(p))
export const apiLoginByEmail = (p) => Request.post('/api/user/login-email', qs.stringify(p))
export const apiSendEmailCode = ({ email }) => Request.post('/api/user/send-email-code', qs.stringify({ email }))
export const apiRegister = (p) => Request.post('/api/user/register', qs.stringify(p))
