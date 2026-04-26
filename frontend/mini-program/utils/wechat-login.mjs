export const isWechatBrowser = (userAgent = '') => /MicroMessenger/i.test(userAgent || '')

export const resolveWechatLoginPlatform = ({ isMiniProgram = false, isApp = false, userAgent = '' } = {}) => {
  if (isMiniProgram) return 'MINI_PROGRAM'
  if (isApp) return 'APP'
  if (isWechatBrowser(userAgent)) return 'WECHAT_H5'
  return ''
}

export const parseWechatOAuthPayload = (hash = '') => {
  const raw = String(hash || '').replace(/^#/, '')
  const queryLike = raw.includes('?') ? raw.slice(raw.indexOf('?') + 1) : raw
  const params = new URLSearchParams(queryLike.replace(/^&/, ''))
  return {
    token: params.get('wechatToken') || '',
    bindToken: params.get('wechatBindToken') || '',
  }
}
