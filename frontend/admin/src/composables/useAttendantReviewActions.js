import { ref } from 'vue'
import { reviewAttendant, updateAttendantStatus } from '../utils/admin-api'

export const ATTENDANT_REASON_CHIPS = {
  reject: [
    '身份证照片不清晰',
    '执业证书信息不完整',
    '健康证已过期',
    '实名信息与证件不一致'
  ],
  ban: [
    '服务投诉成立',
    '多次爽约',
    '资质复核未通过',
    '违规私下交易'
  ]
}

export const ATTENDANT_ACTION_META = {
  approve: {
    title: '通过审核',
    description: '将该陪诊师审核通过并恢复为正常状态。',
    confirmText: '确认通过',
    confirmTone: 'button-primary',
    needReason: false
  },
  reject: {
    title: '驳回审核',
    description: '驳回会把陪诊师状态改为审核失败，并记录原因。',
    confirmText: '确认驳回',
    confirmTone: 'button-danger',
    needReason: true,
    reasonLabel: '驳回原因',
    reasonPlaceholder: '请输入审核驳回原因'
  },
  ban: {
    title: '封禁陪诊师',
    description: '封禁后该陪诊师无法以正常状态参与服务。',
    confirmText: '确认封禁',
    confirmTone: 'button-danger',
    needReason: true,
    reasonLabel: '封禁原因',
    reasonPlaceholder: '请输入封禁原因'
  },
  'restore-status': {
    title: '恢复陪诊师',
    description: '将封禁状态恢复为正常。',
    confirmText: '确认恢复',
    confirmTone: 'button-primary',
    needReason: false
  },
  'restore-review': {
    title: '重新通过审核',
    description: '将审核失败的陪诊师重新恢复为通过状态。',
    confirmText: '确认通过',
    confirmTone: 'button-primary',
    needReason: false
  }
}

const normalizeError = (error, fallback) => error?.message || fallback

export const useAttendantReviewActions = (uiStore) => {
  const actionLoading = ref(false)

  const runAttendantAction = async ({ attendantId, actionType, reason = '' }) => {
    if (!attendantId) return { ok: false, errorMessage: '陪诊师 ID 缺失' }

    const meta = ATTENDANT_ACTION_META[actionType]
    if (!meta) return { ok: false, errorMessage: '不支持的处理动作' }
    if (meta.needReason && !reason.trim()) return { ok: false, errorMessage: '请填写处理原因' }

    actionLoading.value = true
    try {
      if (actionType === 'approve') {
        await reviewAttendant(attendantId, { action: 'approve' })
      } else if (actionType === 'reject') {
        await reviewAttendant(attendantId, { action: 'reject', reason: reason.trim() })
      } else if (actionType === 'ban') {
        await updateAttendantStatus(attendantId, { status: 2, reason: reason.trim() })
      } else if (actionType === 'restore-status') {
        await updateAttendantStatus(attendantId, { status: 1, reason: '' })
      } else if (actionType === 'restore-review') {
        await reviewAttendant(attendantId, { action: 'restore' })
      }
      return { ok: true }
    } catch (error) {
      const message = normalizeError(error, '处理失败')
      uiStore?.toast?.(message, 'error')
      return { ok: false, errorMessage: message }
    } finally {
      actionLoading.value = false
    }
  }

  const getReasonChips = (actionType) => ATTENDANT_REASON_CHIPS[actionType] || []
  const getActionMeta = (actionType) => ATTENDANT_ACTION_META[actionType] || ATTENDANT_ACTION_META.approve

  return {
    actionLoading,
    getActionMeta,
    getReasonChips,
    runAttendantAction
  }
}
