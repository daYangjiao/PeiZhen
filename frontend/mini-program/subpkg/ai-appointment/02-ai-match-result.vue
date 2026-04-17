<template>
  <view class="match-page">
    <view class="hero-card">
      <text class="hero-title">AI 推荐结果</text>
      <text class="hero-subtitle">根据您的需求，优先推荐更匹配的陪诊师</text>
    </view>

    <view v-if="summaryVisible" class="summary-card">
      <view class="summary-head">
        <view class="summary-head-copy">
          <text class="summary-title">已整理的预约信息</text>
          <text class="summary-subtitle">{{ summaryStatusText }}</text>
        </view>
        <view class="summary-badge">
          <text>已确认</text>
        </view>
      </view>

      <view class="summary-grid">
        <view v-for="item in summaryItems" :key="item.key" class="summary-item" :class="{ empty: !item.value }">
          <text class="summary-label">{{ item.label }}</text>
          <text class="summary-value">{{ item.value || item.placeholder }}</text>
        </view>
      </view>

      <view v-if="summaryTags.length" class="summary-tags">
        <view v-for="tag in summaryTags" :key="tag" class="summary-tag">
          <text>{{ tag }}</text>
        </view>
      </view>
    </view>

    <view v-if="loading" class="state-card loading-card">
      <text class="loading-title">{{ phaseLabel }}</text>
      <text class="loading-desc">{{ phaseText }}</text>
    </view>

    <view v-else-if="errorText" class="state-card">
      <text class="error-title">暂时还没生成推荐</text>
      <text class="error-desc">{{ errorText }}</text>
      <button class="secondary-btn" @click="retryPoll">重新获取</button>
    </view>

    <template v-else>
      <view v-if="degraded" class="notice-card">
        <text class="notice-title">快速推荐</text>
        <text class="notice-text">当前已先为您返回高分优质陪诊师，您也可以继续直接下单。</text>
      </view>

      <view class="list-wrap">
        <view v-for="item in matchedList" :key="item.attendantId" class="match-card">
          <view class="card-top">
            <image class="avatar" :src="resolveAvatar(item.avatar)" mode="aspectFill" />
            <view class="meta">
              <view class="name-row">
                <text class="name">{{ item.attendantName }}</text>
                <text v-if="item.gender" class="gender">{{ item.gender }}</text>
              </view>
              <text class="field">{{ item.professionalField || item.specialty || '综合陪诊' }}</text>
              <text class="intro">{{ item.introduction || item.experience || '资料完善中' }}</text>
            </view>
            <view class="score-pill">
              <text class="score">{{ item.matchScore || 90 }}</text>
              <text class="score-label">匹配分</text>
            </view>
          </view>

          <view class="stats-row">
            <text class="stat">评分 {{ formatScore(item.score) }}</text>
            <text class="stat">{{ item.experienceYears || 0 }}年经验</text>
            <text class="stat">已服务 {{ item.completedOrders || 0 }} 单</text>
          </view>

          <view class="reason-box">
            <text class="reason-badge">AI 推荐理由</text>
            <text class="reason-text">{{ item.reason }}</text>
          </view>

          <button class="primary-btn" :disabled="actionLoading" @click="createOrder(item.attendantId)">
            {{ actionLoading ? '处理中...' : '就选 TA 并下单' }}
          </button>
        </view>
      </view>

      <view class="footer-actions">
        <button class="ghost-btn" :disabled="actionLoading" @click="goToNormalFlow">跳过推荐，走普通预约</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { defaultAvatar } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { createAiAppointmentOrder, getAiAppointmentSession } from './api.js'

const sessionId = ref('')
const loading = ref(true)
const actionLoading = ref(false)
const matchedList = ref([])
const appointmentNo = ref('')
const degraded = ref(false)
const errorText = ref('')
const phaseLabel = ref('AI 正在为您寻优匹配中...')
const phaseText = ref('请稍候，系统正在结合需求与可用陪诊师做筛选')
const structuredDemand = ref(createEmptyStructuredDemand())
const AI_APPOINTMENT_DRAFT_KEY = 'ai_appointment_draft_pending'
const AI_APPOINTMENT_SESSION_KEY = 'ai_appointment_current_session_id'
let hydrateTimer = null
const MAX_HYDRATE_RETRY = 3

const normalizeString = (value = '') => String(value || '').trim()

function getCurrentUserName() {
  const userInfo = uni.getStorageSync('userInfo') || {}
  return userInfo.nickName || userInfo.name || userInfo.phone || '本人'
}

function createEmptyStructuredDemand() {
  return {
    patientName: getCurrentUserName(),
    patientProfile: '',
    hospital: '',
    serviceDate: '',
    serviceStartTime: '',
    serviceEndTime: '',
    symptomDescription: '',
    symptomTags: [],
    otherRequirement: '',
    preferenceTags: [],
    attendantGender: '',
    rawDemandText: ''
  }
}

const uniqueList = (items = []) => Array.from(new Set((Array.isArray(items) ? items : [items]).map((item) => normalizeString(item)).filter(Boolean)))

const formatDateLabel = (dateKey = '') => {
  if (!dateKey) return ''
  const safe = String(dateKey).trim().replace(/\//g, '-')
  const match = safe.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (!match) return safe
  return `${Number(match[1])}年${Number(match[2])}月${Number(match[3])}日`
}

const normalizeTimeValue = (value = '') => {
  const text = normalizeString(value)
    .replace(/[～~至到—]/g, '-')
    .replace(/\s+/g, '')
    .replace(/：/g, ':')
    .replace(/时/g, '点')
  if (!text) return ''
  if (/^\d{1,2}$/.test(text)) return `${text.padStart(2, '0')}:00`
  let match = text.match(/^(\d{1,2})[:.](\d{1,2})$/)
  if (match) {
    const hour = Number(match[1])
    const minute = Number(match[2])
    if (Number.isFinite(hour) && Number.isFinite(minute) && hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
      return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
    }
  }
  match = text.match(/^(\d{1,2})点半$/)
  if (match) return `${String(Number(match[1])).padStart(2, '0')}:30`
  match = text.match(/^(\d{1,2})点(?:(\d{1,2})分?)?$/)
  if (match) return `${String(Number(match[1])).padStart(2, '0')}:${String(Number(match[2] || 0)).padStart(2, '0')}`
  return ''
}

const sanitizeStructuredDemand = (payload = {}) => ({
  patientName: normalizeString(payload.patientName) || getCurrentUserName(),
  patientProfile: normalizeString(payload.patientProfile),
  hospital: normalizeString(payload.hospital),
  serviceDate: normalizeString(payload.serviceDate),
  serviceStartTime: normalizeTimeValue(payload.serviceStartTime),
  serviceEndTime: normalizeTimeValue(payload.serviceEndTime),
  symptomDescription: normalizeString(payload.symptomDescription),
  symptomTags: uniqueList(payload.symptomTags),
  otherRequirement: normalizeString(payload.otherRequirement),
  preferenceTags: uniqueList(payload.preferenceTags),
  attendantGender: normalizeString(payload.attendantGender),
  rawDemandText: normalizeString(payload.rawDemandText)
})

const getStructuredDemandStorageKey = (id = '') => {
  return id ? `ai_appointment_draft_${id}` : AI_APPOINTMENT_DRAFT_KEY
}

const clearAiAppointmentCache = () => {
  try {
    uni.removeStorageSync(AI_APPOINTMENT_SESSION_KEY)
    uni.removeStorageSync(AI_APPOINTMENT_DRAFT_KEY)
    if (sessionId.value) {
      uni.removeStorageSync(getStructuredDemandStorageKey(sessionId.value))
    }
  } catch (error) {
    console.warn('清理 AI 预约缓存失败:', error)
  }
}

const formatTimeRange = (startTime = '', endTime = '') => {
  if (!startTime && !endTime) return ''
  if (startTime && endTime) return `${startTime}-${endTime}`
  return startTime || endTime || ''
}

const mergeStructuredDemand = (state = {}) => {
  const source = state.structuredDemand || state.demandData || state.appointmentData || {}
  structuredDemand.value = sanitizeStructuredDemand({
    ...structuredDemand.value,
    ...source,
    patientName: state.patientName || source.patientName || structuredDemand.value.patientName || getCurrentUserName(),
    patientProfile: state.patientProfile || source.patientProfile || structuredDemand.value.patientProfile,
    hospital: state.hospital || source.hospital || structuredDemand.value.hospital,
    serviceDate: state.serviceDate || source.serviceDate || structuredDemand.value.serviceDate,
    serviceStartTime: state.serviceStartTime || source.serviceStartTime || structuredDemand.value.serviceStartTime,
    serviceEndTime: state.serviceEndTime || source.serviceEndTime || structuredDemand.value.serviceEndTime,
    symptomDescription: state.symptomDescription || source.symptomDescription || structuredDemand.value.symptomDescription,
    symptomTags: state.symptomTags || source.symptomTags || structuredDemand.value.symptomTags,
    otherRequirement: state.otherRequirement || source.otherRequirement || structuredDemand.value.otherRequirement,
    preferenceTags: state.preferenceTags || source.preferenceTags || structuredDemand.value.preferenceTags,
    attendantGender: state.attendantGender || source.attendantGender || structuredDemand.value.attendantGender,
    rawDemandText: state.rawDemandText || source.rawDemandText || structuredDemand.value.rawDemandText
  })
}

const persistStructuredDemand = () => {
  try {
    uni.setStorageSync(AI_APPOINTMENT_DRAFT_KEY, structuredDemand.value)
  } catch (error) {
    console.warn('保存 AI 预约草稿失败:', error)
  }
}

const restoreStructuredDemand = () => {
  try {
    const cached = sessionId.value
      ? uni.getStorageSync(getStructuredDemandStorageKey(sessionId.value))
      : uni.getStorageSync(AI_APPOINTMENT_DRAFT_KEY)
    if (cached && typeof cached === 'object') {
      structuredDemand.value = sanitizeStructuredDemand({ ...createEmptyStructuredDemand(), ...cached })
    }
  } catch (error) {
    console.warn('恢复 AI 预约草稿失败:', error)
  }
}

const summaryItems = computed(() => ([
  {
    key: 'hospital',
    label: '就诊医院',
    value: structuredDemand.value.hospital,
    placeholder: '等待补充医院'
  },
  {
    key: 'time',
    label: '就诊时间',
    value: [formatDateLabel(structuredDemand.value.serviceDate), formatTimeRange(structuredDemand.value.serviceStartTime, structuredDemand.value.serviceEndTime)].filter(Boolean).join(' '),
    placeholder: '等待补充时间'
  },
  {
    key: 'patient',
    label: '就诊人',
    value: structuredDemand.value.patientName ? `${structuredDemand.value.patientName}${structuredDemand.value.patientProfile ? `（${structuredDemand.value.patientProfile}）` : ''}` : '',
    placeholder: '等待补充就诊人'
  },
  {
    key: 'symptom',
    label: '症状描述',
    value: structuredDemand.value.symptomDescription || uniqueList(structuredDemand.value.symptomTags).join('、'),
    placeholder: '等待补充症状'
  },
  {
    key: 'other',
    label: '其他需求',
    value: structuredDemand.value.otherRequirement || uniqueList(structuredDemand.value.preferenceTags).join('、'),
    placeholder: '等待补充其他需求'
  }
]))

const summaryTags = computed(() => {
  const tags = []
  tags.push(...uniqueList(structuredDemand.value.symptomTags))
  tags.push(...uniqueList(structuredDemand.value.preferenceTags))
  return uniqueList(tags)
})

const summaryVisible = computed(() => summaryItems.value.some((item) => !!item.value) || !!sessionId.value || degraded.value || matchedList.value.length > 0)

const summaryStatusText = computed(() => {
  if (loading.value) return '等待 AI 匹配完成，您可以先查看已整理的信息'
  if (degraded.value) return '已先返回高分优质陪诊师'
  return '系统将基于这份信息为您继续下单'
})

const stopPolling = () => {
  if (hydrateTimer) {
    clearTimeout(hydrateTimer)
    hydrateTimer = null
  }
}

const resolveAvatar = (url) => resolveAvatarUrl(url, defaultAvatar)
const formatScore = (score) => Number(score || 5).toFixed(1)

const hydrateSession = async (attempt = 0) => {
  stopPolling()
  if (!sessionId.value) return
  try {
    const state = await getAiAppointmentSession(sessionId.value)
    if (!state) {
      throw new Error('匹配会话不存在')
    }
    phaseLabel.value = state.processingPhase === 'answering' ? 'AI 正在输出推荐结果...' : 'AI 正在为您寻优匹配中...'
    phaseText.value = state.thinkingProcess || phaseText.value
    mergeStructuredDemand(state)
    persistStructuredDemand()

    if (Array.isArray(state.matchedList) && state.matchedList.length) {
      matchedList.value = state.matchedList
      appointmentNo.value = state.appointmentNo || ''
      degraded.value = !!state.degraded
      loading.value = false
      return
    }

    if (state.processingPhase === 'failed') {
      loading.value = false
      errorText.value = state.message || '推荐失败，请稍后重试'
      return
    }
    if (attempt < MAX_HYDRATE_RETRY - 1) {
      hydrateTimer = setTimeout(() => {
        hydrateSession(attempt + 1)
      }, 800)
      return
    }

    loading.value = false
    errorText.value = '结果仍在生成中，请返回上一页稍后重试'
  } catch (error) {
    if (attempt < MAX_HYDRATE_RETRY - 1) {
      hydrateTimer = setTimeout(() => {
        hydrateSession(attempt + 1)
      }, 800)
      return
    }
    loading.value = false
    errorText.value = '获取推荐结果失败，请稍后重试'
  }
}

const createOrder = async (designatedAttendantId) => {
  if (!appointmentNo.value || actionLoading.value) return
  actionLoading.value = true
  try {
    const data = await createAiAppointmentOrder(appointmentNo.value, designatedAttendantId)
    const orderNo = data?.orderNo
    if (!orderNo) {
      throw new Error('订单创建失败')
    }
    clearAiAppointmentCache()
    uni.navigateTo({
      url: `/subpkg/appointment-flow/04-order-confirm-page?orderNo=${encodeURIComponent(orderNo)}`
    })
  } catch (error) {
    uni.showToast({ title: '创建订单失败，请稍后重试', icon: 'none' })
  } finally {
    actionLoading.value = false
  }
}

const goToNormalFlow = () => {
  clearAiAppointmentCache()
  uni.switchTab({ url: '/pages/ai-triage/01-appointment-selection' })
}

const retryPoll = () => {
  matchedList.value = []
  appointmentNo.value = ''
  degraded.value = false
  errorText.value = ''
  loading.value = true
  hydrateSession(0)
}

onLoad((options) => {
  sessionId.value = options?.sessionId || ''
  restoreStructuredDemand()
  if (!sessionId.value) {
    loading.value = false
    errorText.value = '缺少匹配会话，请重新发起 AI 预约'
    return
  }
  hydrateSession(0)
})

onUnload(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
.match-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef5ff 0%, #f7f9fd 100%);
  padding: 24rpx 24rpx 40rpx;
  width: 100%;
  max-width: 100%;
  overflow-x: clip;
  box-sizing: border-box;
}

.hero-card,
.state-card,
.notice-card,
.match-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 28rpx;
  box-shadow: 0 18rpx 44rpx rgba(48, 79, 143, 0.08);
}

.hero-card {
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.hero-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #20324f;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #6b7a96;
}

.summary-card {
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  box-shadow: 0 18rpx 44rpx rgba(48, 79, 143, 0.08);
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.summary-head-copy {
  flex: 1;
  min-width: 0;
}

.summary-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #23344f;
}

.summary-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: #6a7c9a;
}

.summary-badge {
  flex-shrink: 0;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: #edf4ff;
  color: #4a6ba3;
  font-size: 22rpx;
  font-weight: 700;
}

.summary-grid {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.summary-item {
  padding: 16rpx 18rpx;
  border-radius: 20rpx;
  background: #f7faff;
}

.summary-item.empty {
  background: #f5f7fb;
}

.summary-label {
  display: block;
  font-size: 22rpx;
  color: #6a7c9a;
}

.summary-value {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #22324f;
  word-break: break-word;
}

.summary-tags {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.summary-tag {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #49679d;
  font-size: 22rpx;
}

.state-card,
.notice-card {
  padding: 32rpx 28rpx;
  margin-bottom: 20rpx;
}

.loading-title,
.error-title,
.notice-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #24375a;
}

.loading-desc,
.error-desc,
.notice-text {
  display: block;
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #667892;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.match-card {
  padding: 28rpx;
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 28rpx;
  flex-shrink: 0;
}

.meta {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  font-size: 32rpx;
  font-weight: 700;
  color: #21324f;
}

.gender {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #edf3ff;
  color: #5570a5;
  font-size: 22rpx;
}

.field,
.intro {
  display: block;
  margin-top: 10rpx;
}

.field {
  font-size: 24rpx;
  color: #4a6ba3;
}

.intro {
  font-size: 24rpx;
  line-height: 1.6;
  color: #6c7b95;
}

.score-pill {
  min-width: 120rpx;
  padding: 14rpx 12rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #ebf4ff 0%, #f4f8ff 100%);
  text-align: center;
}

.score {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #2f5ebc;
}

.score-label {
  font-size: 20rpx;
  color: #6983b2;
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 20rpx;
}

.stat {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #f3f6fb;
  color: #5d6d89;
  font-size: 22rpx;
}

.reason-box {
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #f0f7ff 0%, #eef3ff 100%);
}

.reason-badge {
  display: inline-flex;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #dceaff;
  color: #4369b0;
  font-size: 22rpx;
  font-weight: 700;
}

.reason-text {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #29406d;
}

.primary-btn,
.ghost-btn,
.secondary-btn {
  height: 92rpx;
  border-radius: 999rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.primary-btn {
  margin-top: 24rpx;
  background: linear-gradient(135deg, #3f84ff 0%, #6cb0ff 100%);
  color: #ffffff;
}

.ghost-btn,
.secondary-btn {
  background: #ffffff;
  color: #3f6fca;
  border: 2rpx solid #d6e6ff;
}

.footer-actions {
  margin-top: 24rpx;
}
</style>
