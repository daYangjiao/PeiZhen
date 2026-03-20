const isBrowser = () => typeof window !== 'undefined' && typeof document !== 'undefined'

const isSecureBrowserContext = () => {
  if (!isBrowser()) return true
  if (window.isSecureContext) return true

  const hostname = window.location?.hostname || ''
  const protocol = window.location?.protocol || ''
  return protocol === 'https:' || hostname === 'localhost' || hostname === '127.0.0.1'
}

export const showUnsupportedFeature = (feature, detail) => {
  uni.showToast({
    title: detail || `${feature}当前环境不可用`,
    icon: 'none',
    duration: 2500
  })
}

export const createRecorderManager = () => {
  if (typeof uni.getRecorderManager !== 'function') return null
  if (isBrowser() && (!isSecureBrowserContext() || typeof navigator === 'undefined' || !navigator.mediaDevices)) return null
  return uni.getRecorderManager()
}

export const createInnerAudioContext = () => {
  if (typeof uni.createInnerAudioContext !== 'function') return null
  return uni.createInnerAudioContext()
}

export const chooseLocationWithGuard = ({ success, fail } = {}) => {
  if (typeof uni.chooseLocation !== 'function' || (isBrowser() && !isSecureBrowserContext())) {
    showUnsupportedFeature('位置选择', '公网 IP 的 HTTP 页面不支持位置选择，请改用小程序或 HTTPS')
    if (typeof fail === 'function') fail(new Error('unsupported'))
    return false
  }

  uni.chooseLocation({ success, fail })
  return true
}

export const openLocationWithGuard = ({ latitude, longitude, name, address } = {}) => {
  if (typeof uni.openLocation === 'function' && (!isBrowser() || isSecureBrowserContext())) {
    uni.openLocation({ latitude, longitude, name, address })
    return true
  }

  if (isBrowser() && latitude && longitude) {
    const encodedName = encodeURIComponent(name || '目的地')
    const mapUrl = `https://uri.amap.com/marker?position=${longitude},${latitude}&name=${encodedName}&src=陪诊服务&coordinate=gaode&callnative=0`
    window.open(mapUrl, '_blank')
    return true
  }

  showUnsupportedFeature('地图导航', '当前环境不支持打开地图，请改用小程序或 HTTPS')
  return false
}

export const makePhoneCallWithGuard = (phoneNumber) => {
  if (!phoneNumber) {
    showUnsupportedFeature('拨号', '暂无可用联系电话')
    return false
  }

  if (typeof uni.makePhoneCall === 'function' && !isBrowser()) {
    uni.makePhoneCall({
      phoneNumber,
      fail: () => showUnsupportedFeature('拨号', '拨号失败，请稍后再试')
    })
    return true
  }

  if (isBrowser()) {
    window.location.href = `tel:${phoneNumber}`
    return true
  }

  showUnsupportedFeature('拨号', '当前环境不支持拨号')
  return false
}

export const scanCodeWithGuard = ({ success, fail } = {}) => {
  if (typeof uni.scanCode === 'function' && (!isBrowser() || isSecureBrowserContext())) {
    uni.scanCode({ success, fail })
    return true
  }

  showUnsupportedFeature('扫码', '公网 IP 的 HTTP 页面不支持扫码，请改用小程序或 HTTPS')
  if (typeof fail === 'function') fail(new Error('unsupported'))
  return false
}
