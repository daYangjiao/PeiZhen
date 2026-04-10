import { get, post } from '@/utils/api.js'

export const getWechatConfigStatus = (role = 'user') => get('/api/users/wechat/config-status', { role })

export const loginByWechat = (code, role = 'user') => post('/api/users/wechat/login', { code, role })

export const bindWechatPhone = (payload = {}) => post('/api/users/wechat/bind-phone', payload)
