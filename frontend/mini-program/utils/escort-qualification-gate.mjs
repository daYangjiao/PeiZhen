const hasText = (value) => typeof value === 'string' && value.trim().length > 0

const numericStatus = (value) => {
  const next = Number(value)
  return Number.isFinite(next) ? next : 3
}

const isAccountBlocked = (profile = {}) => {
  const reason = String(profile.qualificationBlockReason || '')
  return Number(profile.userStatus) === 0 || Number(profile.accountStatus) === 0 || reason.includes('账号已被禁用') || reason.includes('账号禁用')
}

const isIncomplete = (profile = {}) => {
  const completeness = Number(profile.qualificationCompleteness || 0)
  return completeness < 100
}

export const qualificationPromptKey = ({ userId, state } = {}) =>
  `escort_qualification_gate_prompted:${userId || 'anonymous'}:${state || 'unknown'}`

export const createQualificationPromptTracker = () => {
  const promptedKeys = new Set()
  return {
    has(key) {
      return promptedKeys.has(key)
    },
    mark(key) {
      if (key) promptedKeys.add(key)
    },
    clear() {
      promptedKeys.clear()
    },
  }
}

export const resolveQualificationGate = (profile = {}) => {
  const status = numericStatus(profile.qualificationStatusCode)
  const reason = profile.qualificationBlockReason || profile.qualificationFailReason || ''

  if (profile.canAcceptOrders === true) {
    return { allowed: true, state: 'passed', title: '资质已通过', message: '', popupRequired: false }
  }

  if (isAccountBlocked(profile)) {
    return {
      allowed: false,
      state: 'blocked',
      title: '账号暂不可用',
      message: reason || '账号已被禁用，请联系平台客服处理。',
      popupRequired: false,
    }
  }

  if (profile.practiceCertExpired || profile.healthCertExpired) {
    return {
      allowed: false,
      state: 'expired',
      title: '证件已过期',
      message: reason || '证件已过期，请更新资质后重新提交审核。',
      popupRequired: true,
    }
  }

  if (status === 2) {
    return {
      allowed: false,
      state: 'rejected',
      title: '资质审核未通过',
      message: reason || '资质审核未通过，请修改后重新提交。',
      popupRequired: true,
    }
  }

  if (isIncomplete(profile)) {
    return {
      allowed: false,
      state: 'incomplete',
      title: '资质待补充',
      message: '请补全身份证正反面、执业证书、健康证和证件有效期后提交审核。',
      popupRequired: true,
    }
  }

  if (status === 0) {
    return {
      allowed: false,
      state: 'pending',
      title: '资质审核中',
      message: reason || '平台正在审核你的入驻资料，审核通过后即可查看接单大厅。',
      popupRequired: false,
    }
  }

  return {
    allowed: false,
    state: 'incomplete',
    title: '资质待补充',
    message: reason || '请补全身份证、执业证书、健康证和证件有效期后提交审核。',
    popupRequired: true,
  }
}

export const shouldPromptQualificationGate = ({ userId, gate, tracker } = {}) => {
  if (!gate || gate.allowed || !gate.popupRequired) return false
  const activeTracker = tracker || createQualificationPromptTracker()
  const key = qualificationPromptKey({ userId, state: gate.state })
  if (activeTracker.has(key)) return false
  activeTracker.mark(key)
  return true
}
