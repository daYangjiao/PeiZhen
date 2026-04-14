<template>
  <view class="ai-page">
    <view class="topbar">
      <view class="topbar-left" @click="goBack">
        <text class="topbar-back">‹</text>
      </view>
      <text class="topbar-title">AI 帮我找</text>
      <view class="topbar-right"></view>
    </view>

    <view class="hero-card">
      <view class="hero-icon-wrap">
        <image class="hero-icon" :src="AIAvatar" mode="aspectFill" />
      </view>
      <view class="hero-texts">
        <text class="hero-title">智能匹配最懂您的陪诊师</text>
        <text class="hero-subtitle">先说需求，AI 会追问关键细节，再为您推荐更合适的人选</text>
      </view>
    </view>

    <view class="tip-card">
      <text class="tip-title">示例</text>
      <text class="tip-text">下周三上午，带80岁的爷爷去华西医院心内科复诊，需要一位有力气推轮椅、懂点急救知识的男陪诊师。</text>
    </view>

    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollIntoView"
      scroll-with-animation
    >
      <view class="chat-list">
        <view
          v-for="(msg, index) in messages"
          :key="msg.key"
          class="message-row"
          :class="msg.type === 'user' ? 'row-user' : 'row-ai'"
          :id="`msg-${index}`"
        >
          <image
            v-if="msg.type === 'ai'"
            class="avatar"
            :src="AIAvatar"
            mode="aspectFill"
          />

          <view class="bubble" :class="msg.type === 'user' ? 'bubble-user' : 'bubble-ai'">
            <template v-if="msg.type === 'ai'">
              <view class="bubble-head">
                <text class="bubble-name">AI 帮我找</text>
                <view v-if="msg.processingPhase && msg.processingPhase !== 'completed'" class="phase-badge" :class="`phase-${msg.processingPhase}`">
                  <text>{{ getPhaseLabel(msg.processingPhase) }}</text>
                </view>
              </view>
              <view v-if="msg.processingPhase && msg.processingPhase !== 'completed'" class="phase-text">
                <text>{{ msg.thinkingProcess || getPhaseLabel(msg.processingPhase) }}</text>
              </view>
              <view v-if="msg.waitingMatch" class="matching-indicator">
                <view class="matching-dots">
                  <text class="matching-dot"></text>
                  <text class="matching-dot"></text>
                  <text class="matching-dot"></text>
                </view>
                <text class="matching-caption">正在梳理需求并筛选可接单陪诊师</text>
              </view>
            </template>

            <view class="bubble-content" :class="{ loading: msg.type === 'ai' && msg.processingPhase && msg.processingPhase !== 'completed' }">
              <text>{{ msg.text }}</text>
            </view>
          </view>

          <image
            v-if="msg.type === 'user'"
            class="avatar"
            :src="getUserAvatar()"
            mode="aspectFill"
          />
        </view>
      </view>
    </scroll-view>

    <view class="composer" :style="{ paddingBottom: composerPaddingBottom + 'px' }">
      <view class="term-card">
        <text class="term-title">{{ activeOptions.length ? '快速补充' : '需求示例' }}</text>
        <scroll-view class="term-chips" scroll-x :show-scrollbar="false" enable-flex>
          <view class="term-chip-list">
            <view
              v-for="(tag, index) in activeOptions.length ? activeOptions : suggestions"
              :key="`${tag}-${index}`"
              class="term-chip"
              @click="handleChipClick(tag)"
            >
              <text>{{ tag }}</text>
            </view>
          </view>
        </scroll-view>
        <view v-if="shouldShowStructuredAction" class="structured-action" @click="openStructuredPicker(currentQuestionType)">
          <text class="structured-action-text">直接选择{{ getQuestionLabel(currentQuestionType) }}</text>
          <text class="structured-action-arrow">›</text>
        </view>
      </view>

      <view class="input-bar">
        <input
          v-model="userInput"
          class="input-box"
          placeholder="告诉 AI 您的预约需求..."
          confirm-type="send"
          @confirm="sendMessage"
          :disabled="sending || matchingInProgress || navigatingToResult"
        />
        <button class="send-btn" :disabled="sending || matchingInProgress || navigatingToResult || !userInput.trim()" @click="sendMessage">
          {{ sending ? '处理中' : '发送' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { brandAiAvatar, userPlaceholder } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { useUserStore } from '@/stores/user'
import {
  createAiAppointmentSession,
  getAiAppointmentSession,
  replyAiAppointmentSession,
  startAiAppointmentMatch
} from './api.js'

const AIAvatar = brandAiAvatar
const userStore = useUserStore()
const POLL_INTERVAL = 1500
const MATCH_WAIT_LIMIT = 30000

const suggestions = [
  '明天下午去华西医院，需要推轮椅',
  '下周一上午去省医院复诊，需要熟悉老人陪护',
  '周五晚上急诊陪同，希望反应快',
  '后天去协和医院检查，希望有护士经验'
]

const messages = ref([
  {
    key: 'welcome-ai-appointment',
    type: 'ai',
    text: '您好，我可以帮您先梳理预约需求，再推荐更合适的陪诊师。您直接描述时间、医院、患者情况和偏好就可以。',
    processingPhase: 'completed',
    thinkingProcess: ''
  }
])
const userInput = ref('')
const sending = ref(false)
const matchingInProgress = ref(false)
const scrollIntoView = ref('')
const composerPaddingBottom = ref(8)
const sessionId = ref('')
const currentQuestionType = ref('')
const activeOptions = ref([])
const followUpRound = ref(0)
const navigatingToResult = ref(false)
const activeAssistantKey = ref('')
const matchDeadlineTimer = ref(null)
let pollTimer = null

const getUserAvatar = () => {
  const avatar = userStore.avatar || uni.getStorageSync('userInfo')?.avatar || ''
  return resolveAvatarUrl(avatar, userPlaceholder)
}

const refreshSafeBottom = () => {
  try {
    const info = typeof uni.getWindowInfo === 'function' ? uni.getWindowInfo() : uni.getSystemInfoSync()
    const safeBottom = Number(info?.safeAreaInsets?.bottom || 0)
    composerPaddingBottom.value = Math.max(8, safeBottom)
  } catch (error) {
    composerPaddingBottom.value = 8
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoView.value = `msg-${messages.value.length - 1}`
  })
}

const getPhaseLabel = (phase = '') => {
  if (phase === 'thinking') return '思考中'
  if (phase === 'answering') return '回答中'
  if (phase === 'failed') return '失败'
  return '已完成'
}

const normalizeTimeChunk = (value = '') => {
  const text = String(value).trim()
  if (!text) return ''
  const [hours = '', minutes = ''] = text.split(':')
  if (!hours || !minutes) return ''
  const h = String(Number(hours))
  const m = String(Number(minutes))
  if (!Number.isFinite(Number(h)) || !Number.isFinite(Number(m))) return ''
  return `${h.padStart(2, '0')}:${m.padStart(2, '0')}`
}

const normalizeExactTimePeriod = (value = '') => {
  const cleaned = String(value)
    .trim()
    .replace(/[～~至到]/g, '-')
    .replace(/\s+/g, '')
  const match = cleaned.match(/^(\d{1,2}:\d{2})-(\d{1,2}:\d{2})$/)
  if (!match) return ''
  const start = normalizeTimeChunk(match[1])
  const end = normalizeTimeChunk(match[2])
  if (!start || !end) return ''
  return `${start}-${end}`
}

const getQuestionLabel = (field) => {
  if (field === 'serviceDate') return '日期'
  if (field === 'timePeriod') return '时段'
  if (field === 'hospital') return '医院'
  return '信息'
}

const shouldShowStructuredAction = computed(() => {
  return !!currentQuestionType.value && followUpRound.value >= 2
})

const appendUserMessage = (text) => {
  messages.value.push({
    key: `u-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type: 'user',
    text
  })
}

const appendAssistantPlaceholder = (text = '') => {
  const key = `a-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`
  activeAssistantKey.value = key
  messages.value.push({
    key,
    type: 'ai',
    text,
    processingPhase: 'thinking',
    thinkingProcess: '正在理解您的预约需求...',
    waitingMatch: false
  })
}

const upsertAssistantMessage = (payload = {}) => {
  const next = {
    key: activeAssistantKey.value || `a-${Date.now()}`,
    type: 'ai',
    text: payload.message || '',
    processingPhase: payload.processingPhase || 'completed',
    thinkingProcess: payload.thinkingProcess || '',
    waitingMatch: !!payload.waitingMatch
  }
  const index = messages.value.findIndex((item) => item.key === next.key)
  if (index >= 0) {
    messages.value.splice(index, 1, next)
  } else {
    messages.value.push(next)
  }
}

const stopPolling = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

const stopMatchDeadline = () => {
  if (matchDeadlineTimer.value) {
    clearTimeout(matchDeadlineTimer.value)
    matchDeadlineTimer.value = null
  }
}

const schedulePoll = (delay = POLL_INTERVAL) => {
  stopPolling()
  pollTimer = setTimeout(() => {
    pollSession()
  }, delay)
}

const showMatchingBubble = () => {
  matchingInProgress.value = true
  upsertAssistantMessage({
    message: '正在为您匹配合适的陪诊师',
    processingPhase: 'answering',
    thinkingProcess: '正在综合医院、时间与陪护偏好',
    waitingMatch: true
  })
  scrollToBottom()
}

const navigateToResultPage = async () => {
  if (navigatingToResult.value) return
  navigatingToResult.value = true
  stopPolling()
  stopMatchDeadline()
  matchingInProgress.value = false
  try {
    await uni.navigateTo({
      url: `/subpkg/ai-appointment/02-ai-match-result?sessionId=${encodeURIComponent(sessionId.value)}`
    })
  } catch (error) {
    navigatingToResult.value = false
    uni.showToast({ title: '跳转结果页失败，请稍后重试', icon: 'none' })
  }
}

const startMatchFlow = async () => {
  if (matchingInProgress.value || navigatingToResult.value || !sessionId.value) return
  showMatchingBubble()
  stopMatchDeadline()
  matchDeadlineTimer.value = setTimeout(async () => {
    try {
      const state = await getAiAppointmentSession(sessionId.value)
      if (state && Array.isArray(state.matchedList) && state.matchedList.length) {
        await applySessionState(state)
        return
      }
    } catch (error) {
      console.warn('匹配超时前最后一次拉取失败:', error)
    }
    await navigateToResultPage()
  }, MATCH_WAIT_LIMIT)

  try {
    await startAiAppointmentMatch(sessionId.value)
  } catch (error) {
    stopMatchDeadline()
    matchingInProgress.value = false
    sending.value = false
    upsertAssistantMessage({
      message: '启动匹配失败，请稍后重试。',
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试'
    })
    uni.showToast({ title: '启动匹配失败，请稍后重试', icon: 'none' })
    return
  }

  schedulePoll(800)
}

const applySessionState = async (state) => {
  if (!state) return
  upsertAssistantMessage({
    message: state.message || '',
    processingPhase: state.processingPhase,
    thinkingProcess: state.thinkingProcess,
    waitingMatch: matchingInProgress.value
  })
  currentQuestionType.value = state.questionType || ''
  activeOptions.value = Array.isArray(state.options) ? state.options : []
  followUpRound.value = Number(state.followUpRound || 0)
  scrollToBottom()

  if (state.processingPhase === 'completed') {
    sending.value = false
    if (Array.isArray(state.matchedList) && state.matchedList.length) {
      stopMatchDeadline()
      await navigateToResultPage()
    } else if (state.canMatch && !state.needMoreInfo && !matchingInProgress.value) {
      await startMatchFlow()
    } else if (state.needMoreInfo && followUpRound.value >= 2) {
      openStructuredPicker(currentQuestionType.value)
    } else if (matchingInProgress.value) {
      schedulePoll()
    }
    return
  }

  if (state.processingPhase === 'failed') {
    sending.value = false
    matchingInProgress.value = false
    stopMatchDeadline()
    return
  }

  schedulePoll()
}

const pollSession = async () => {
  stopPolling()
  if (!sessionId.value) return
  try {
    const state = await getAiAppointmentSession(sessionId.value)
    await applySessionState(state)
  } catch (error) {
    if (matchingInProgress.value) {
      upsertAssistantMessage({
        message: '正在为您匹配合适的陪诊师',
        processingPhase: 'answering',
        thinkingProcess: '网络波动中，正在继续为您匹配',
        waitingMatch: true
      })
      schedulePoll(2000)
      return
    }
    sending.value = false
    upsertAssistantMessage({
      message: '抱歉，当前 AI 预约服务暂不可用，请稍后重试。',
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试'
    })
  }
}

const sendMessage = async () => {
  const content = userInput.value.trim()
  if (!content || sending.value || navigatingToResult.value) return

  appendUserMessage(content)
  appendAssistantPlaceholder()
  userInput.value = ''
  sending.value = true
  currentQuestionType.value = ''
  activeOptions.value = []
  scrollToBottom()

  try {
    let response
    if (!sessionId.value) {
      response = await createAiAppointmentSession(content)
      sessionId.value = response?.sessionId || ''
    } else {
      response = await replyAiAppointmentSession(sessionId.value, { replyText: content })
    }
    await applySessionState(response)
  } catch (error) {
    sending.value = false
    upsertAssistantMessage({
      message: '抱歉，当前 AI 预约服务暂不可用，请稍后重试。',
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试'
    })
  }
}

const submitStructuredSelection = async (fieldKey, selectedValue, displayText = selectedValue) => {
  if (!sessionId.value || !fieldKey || !selectedValue) return
  appendUserMessage(displayText)
  appendAssistantPlaceholder()
  sending.value = true
  currentQuestionType.value = ''
  activeOptions.value = []
  scrollToBottom()

  try {
    const response = await replyAiAppointmentSession(sessionId.value, {
      fieldKey,
      selectedValue,
      replyText: displayText
    })
    await applySessionState(response)
  } catch (error) {
    sending.value = false
    matchingInProgress.value = false
    upsertAssistantMessage({
      message: '补充信息失败，请稍后重试。',
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试'
    })
  }
}

const handleChipClick = (tag) => {
  if (activeOptions.value.length && currentQuestionType.value) {
    if (currentQuestionType.value === 'timePeriod' && ['上午', '下午', '晚上', '具体时间'].includes(tag)) {
      const periodHint = tag === '具体时间' ? '' : tag
      const modalTitle = periodHint ? `补充具体时间段（${periodHint}）` : '请输入具体时间段'
      uni.showModal({
        title: modalTitle,
        editable: true,
        placeholderText: '例如 09:30-11:30',
        confirmText: '确认',
        success: ({ confirm, content }) => {
          const exactTime = normalizeExactTimePeriod(content || '')
          if (!confirm) return
          if (!exactTime) {
            uni.showToast({ title: '请输入完整时间段，例如 09:30-11:30', icon: 'none' })
            return
          }
          const displayText = periodHint ? `${periodHint}，${exactTime}` : exactTime
          submitStructuredSelection('timePeriod', exactTime, displayText)
        }
      })
      return
    }
    submitStructuredSelection(currentQuestionType.value, tag, tag)
    return
  }
  userInput.value = tag
}

const openStructuredPicker = (fieldKey) => {
  if (!fieldKey) return
  if (fieldKey === 'hospital') {
    uni.showModal({
      title: '补充就诊医院',
      editable: true,
      placeholderText: '请输入医院名称',
      success: ({ confirm, content }) => {
        if (confirm && content && content.trim()) {
          submitStructuredSelection('hospital', content.trim(), content.trim())
        }
      }
    })
    return
  }

  const itemList = activeOptions.value.length ? activeOptions.value : (fieldKey === 'timePeriod' ? ['上午', '下午', '晚上', '具体时间'] : [])
  if (!itemList.length) return
  uni.showActionSheet({
    itemList,
    success: ({ tapIndex }) => {
      const selected = itemList[tapIndex]
      handleChipClick(selected)
    }
  })
}

const goBack = () => {
  stopPolling()
  const pages = getCurrentPages()
  if (Array.isArray(pages) && pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/ai-triage/01-appointment-selection' })
}

onMounted(() => {
  refreshSafeBottom()
  scrollToBottom()
})

onUnmounted(() => {
  stopPolling()
  stopMatchDeadline()
})
</script>

<style lang="scss" scoped>
.ai-page {
  height: 100dvh;
  background: linear-gradient(180deg, #edf4ff 0%, #f8fbff 42%, #f6f8fc 100%);
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 28rpx 0;
}

.topbar-left,
.topbar-right {
  width: 72rpx;
  display: flex;
  align-items: center;
}

.topbar-back {
  font-size: 56rpx;
  color: #22324f;
  line-height: 1;
}

.topbar-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #22324f;
}

.hero-card,
.tip-card {
  margin: 0 24rpx 20rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 28rpx;
  box-shadow: 0 20rpx 48rpx rgba(65, 105, 180, 0.10);
}

.hero-card {
  display: flex;
  align-items: center;
  padding: 28rpx;
}

.hero-icon-wrap {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #d9eaff 0%, #f3f8ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 22rpx;
}

.hero-icon {
  width: 68rpx;
  height: 68rpx;
  border-radius: 22rpx;
}

.hero-texts {
  flex: 1;
}

.hero-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2f4d;
}

.hero-subtitle,
.tip-text {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6b7a96;
}

.tip-card {
  padding: 22rpx 24rpx;
}

.tip-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #4c6aa3;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 24rpx;
}

.chat-list {
  padding-bottom: 24rpx;
}

.message-row {
  display: flex;
  align-items: flex-end;
  margin-bottom: 20rpx;
}

.row-user {
  justify-content: flex-end;
}

.row-ai {
  justify-content: flex-start;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

.row-ai .avatar {
  margin-right: 16rpx;
}

.row-user .avatar {
  margin-left: 16rpx;
}

.bubble {
  max-width: 76%;
  border-radius: 26rpx;
  padding: 20rpx 22rpx;
}

.bubble-ai {
  background: #ffffff;
  box-shadow: 0 16rpx 30rpx rgba(40, 72, 132, 0.08);
}

.bubble-user {
  background: linear-gradient(135deg, #4d8fff 0%, #71b2ff 100%);
}

.bubble-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.bubble-name {
  font-size: 22rpx;
  font-weight: 700;
  color: #5872a3;
}

.phase-badge {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.phase-thinking {
  background: rgba(103, 134, 255, 0.14);
  color: #4e66d7;
}

.phase-answering {
  background: rgba(0, 168, 120, 0.14);
  color: #008761;
}

.phase-failed {
  background: rgba(255, 92, 92, 0.14);
  color: #d63d3d;
}

.phase-text {
  margin-bottom: 10rpx;
  font-size: 22rpx;
  color: #6b7a96;
}

.bubble-content {
  font-size: 28rpx;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-ai .bubble-content {
  color: #26344f;
}

.bubble-user .bubble-content {
  color: #ffffff;
}

.bubble-content.loading {
  min-height: 44rpx;
}

.matching-indicator {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.matching-dots {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
}

.matching-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #4b7fff;
  animation: aiAppointmentDot 1.1s infinite ease-in-out;
}

.matching-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.matching-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.matching-caption {
  font-size: 22rpx;
  color: #5e7398;
}

.composer {
  padding: 0 24rpx 12rpx;
}

.term-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 24rpx;
  padding: 20rpx 22rpx 18rpx;
  box-shadow: 0 18rpx 40rpx rgba(36, 71, 140, 0.08);
}

.term-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #415a86;
}

.term-chips {
  margin-top: 16rpx;
  white-space: nowrap;
}

.term-chip-list {
  display: inline-flex;
  gap: 14rpx;
  padding-right: 12rpx;
}

.term-chip {
  flex-shrink: 0;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #4766a0;
  font-size: 24rpx;
}

.structured-action {
  margin-top: 16rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #f0f7ff 0%, #edf2ff 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.structured-action-text,
.structured-action-arrow {
  color: #4365a9;
  font-size: 24rpx;
  font-weight: 600;
}

.input-bar {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.input-box {
  flex: 1;
  height: 92rpx;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 999rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  color: #22324f;
  box-shadow: 0 16rpx 32rpx rgba(48, 79, 143, 0.08);
}

.send-btn {
  height: 92rpx;
  min-width: 156rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #3f84ff 0%, #6cb0ff 100%);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn[disabled] {
  opacity: 0.56;
}

@keyframes aiAppointmentDot {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  40% {
    transform: translateY(-6rpx);
    opacity: 1;
  }
}
</style>
