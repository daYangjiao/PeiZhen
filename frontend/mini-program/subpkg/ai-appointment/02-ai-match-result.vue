<template>
  <view class="match-page">
    <view class="hero-card">
      <text class="hero-title">AI 推荐结果</text>
      <text class="hero-subtitle">根据您的需求，优先推荐更匹配的陪诊师</text>
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
import { ref } from 'vue'
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
let hydrateTimer = null
const MAX_HYDRATE_RETRY = 3

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
