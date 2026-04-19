<template>
  <view class="ai-page">
    <view class="topbar">
      <view class="topbar-left" @click="goBack">
        <text class="topbar-back">‹</text>
      </view>
      <text class="topbar-title">AI 导诊助手</text>
      <view class="topbar-right" @click="startNewConversation">
        <text class="new-chat-text">新对话</text>
      </view>
    </view>

    <view class="hero-card">
      <view class="hero-icon-wrap">
        <image class="hero-icon" :src="AIAvatar" mode="aspectFill" />
      </view>
      <view class="hero-texts">
        <text class="hero-title">AI 导诊助手</text>
        <text class="hero-subtitle">帮您梳理症状方向与就诊科室，仅供参考</text>
      </view>
    </view>

    <view class="disclaimer-bar">
      <text class="disclaimer-text">免责声明：AI 回复仅供参考，不构成诊断、治疗或用药建议；如有不适请及时前往正规医疗机构就诊。</text>
    </view>

    <view class="platform-referral" @click="goToAppointmentPage">
      <view class="platform-referral-copy">
        <text class="platform-referral-title">需要陪诊师协助？</text>
        <text class="platform-referral-desc">前往预约页使用 AI导诊，填写医院、时间和需求后由平台匹配陪诊师。</text>
      </view>
      <text class="platform-referral-action">去预约</text>
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
                <text class="bubble-name">AI 导诊助手</text>
                <view v-if="msg.processingPhase && msg.processingPhase !== 'completed'" class="phase-badge" :class="`phase-${msg.processingPhase}`">
                  <text>{{ getPhaseLabel(msg.processingPhase) }}</text>
                </view>
              </view>
              <view v-if="msg.processingPhase && msg.processingPhase !== 'completed'" class="phase-text">
                <text>{{ msg.thinkingProcess || getPhaseLabel(msg.processingPhase) }}</text>
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
        <text class="term-title">导诊快捷提问</text>
        <scroll-view class="term-chips" scroll-x :show-scrollbar="false" enable-flex>
          <view class="term-chip-list">
            <view
              v-for="(tag, index) in suggestions"
              :key="index"
              class="term-chip"
              :class="{ selected: selectedTag === tag }"
              @click="selectTag(tag)"
            >
              <text>{{ tag }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="input-bar">
        <input
          v-model="userInput"
          class="input-box"
          placeholder="请输入症状、持续时间或就诊疑问..."
          confirm-type="send"
          @confirm="sendMessage"
          :disabled="sending"
        />
        <button class="send-btn" :disabled="sending || !userInput.trim()" @click="sendMessage">
          {{ sending ? '处理中' : '发送' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { brandAiAvatar, userPlaceholder } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { useUserStore } from '@/stores/user'
import { getLatestMedicalConversation, getMedicalConversation, getMedicalQaRecord, submitMedicalQuestion } from './api.js'
import { getMedicalConversation, getMedicalQaRecord, submitMedicalQuestion } from './api.js'

const AIAvatar = brandAiAvatar
const userStore = useUserStore()
const STORAGE_KEY = 'ai_medical_current_conversation_id'
const POLL_INTERVAL = 1500
const WELCOME_TEXT = '您好，我是 AI 导诊助手。您可以描述当前症状、持续时间、年龄或担心的问题，我会帮您梳理可能的就诊科室与注意事项。'

const createWelcomeMessage = () => ({
    key: 'welcome-ai',
    type: 'ai',
    text: WELCOME_TEXT,
    processingPhase: 'completed',
    thinkingProcess: ''
})

const messages = ref([createWelcomeMessage()])
const userInput = ref('')
const selectedTag = ref('')
const sending = ref(false)
const scrollIntoView = ref('')
const composerPaddingBottom = ref(8)
const currentConversationId = ref('')
const manualNewConversationStarted = ref(false)
const restoringConversation = ref(false)
const pollTimers = new Map()

const suggestions = [
  '反复胃痛挂什么科',
  '孩子发烧三天怎么办',
  '胸闷气短要不要急诊',
  '头晕恶心应该看哪个科',
  '老人腿脚无力怎么检查',
  '咳嗽两周还没好怎么办'
]

const getCurrentUserId = () => {
  const cached = uni.getStorageSync('userInfo') || {}
  return userStore.userInfo?.id || cached.id || ''
}

const getConversationStorageKey = () => {
  const userId = getCurrentUserId()
  return userId ? `${STORAGE_KEY}_${userId}` : STORAGE_KEY
}

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
    console.warn('读取安全区失败，使用默认底部间距', error)
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

const selectTag = (tag) => {
  selectedTag.value = tag
  userInput.value = tag
}

const goBack = () => {
  const pages = getCurrentPages()
  if (Array.isArray(pages) && pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({
    url: '/pages/role-user/home'
  })
}

const goToAppointmentPage = () => {
  uni.switchTab({
    url: '/pages/ai-triage/01-appointment-selection'
  })
}

const startNewConversation = () => {
  Array.from(pollTimers.keys()).forEach((recordId) => stopPolling(recordId))
  currentConversationId.value = ''
  userInput.value = ''
  selectedTag.value = ''
  messages.value = [createWelcomeMessage()]
  manualNewConversationStarted.value = true
  uni.removeStorageSync(getConversationStorageKey())
  uni.removeStorageSync(STORAGE_KEY)
  scrollToBottom()
}

const upsertAiMessage = (record) => {
  const key = `a-${record.recordId}`
  const targetIndex = messages.value.findIndex((item) => item.key === key)
  const nextMessage = {
    key,
    type: 'ai',
    text: record.processingPhase === 'completed'
      ? (record.answer || '暂无回答')
      : (record.answer || ''),
    processingPhase: record.processingPhase || 'thinking',
    thinkingProcess: record.thinkingProcess || '',
    recordId: record.recordId
  }

  if (targetIndex >= 0) {
    messages.value.splice(targetIndex, 1, nextMessage)
  } else {
    messages.value.push(nextMessage)
  }
}

const appendConversationMessages = (records = []) => {
  const restored = [createWelcomeMessage()]

  records.forEach((record) => {
    restored.push({
      key: `q-${record.recordId}`,
      type: 'user',
      text: record.question || ''
    })
    restored.push({
      key: `a-${record.recordId}`,
      type: 'ai',
      text: record.processingPhase === 'completed'
        ? (record.answer || '暂无回答')
        : (record.answer || ''),
      processingPhase: record.processingPhase || 'thinking',
      thinkingProcess: record.thinkingProcess || '',
      recordId: record.recordId
    })
  })

  messages.value = restored
  const conversationId = records.find((record) => record?.conversationId)?.conversationId || ''
  if (conversationId) {
    currentConversationId.value = conversationId
    uni.setStorageSync(getConversationStorageKey(), conversationId)
  }
}

const showWelcomeOnly = () => {
  currentConversationId.value = ''
  messages.value = [createWelcomeMessage()]
  scrollToBottom()
}

const stopPolling = (recordId) => {
  const timer = pollTimers.get(recordId)
  if (timer) {
    clearTimeout(timer)
    pollTimers.delete(recordId)
  }
}

const pollRecord = async (recordId) => {
  stopPolling(recordId)
  try {
    const record = await getMedicalQaRecord(recordId)
    if (!record) {
      throw new Error('问答记录不存在')
    }

    upsertAiMessage(record)
    scrollToBottom()

    if (record.processingPhase === 'completed' || record.processingPhase === 'failed') {
      stopPolling(recordId)
      return
    }

    const timer = setTimeout(() => {
      pollRecord(recordId)
    }, POLL_INTERVAL)
    pollTimers.set(recordId, timer)
  } catch (error) {
    console.error('轮询 AI 问答状态失败:', error)
    upsertAiMessage({
      recordId,
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试',
      answer: '抱歉，当前 AI 导诊服务暂不可用，请稍后重试。'
    })
    scrollToBottom()
  }
}

const restoreConversation = async () => {
  if (manualNewConversationStarted.value || restoringConversation.value) {
    messages.value = [createWelcomeMessage()]
    scrollToBottom()
    return
  }
  restoringConversation.value = true
  const storageKey = getConversationStorageKey()
  const savedConversationId = uni.getStorageSync(storageKey) || ''
  try {
    let records = []
    if (!savedConversationId) {
      records = await getLatestMedicalConversation()
      if (!records.length) {
        uni.removeStorageSync(storageKey)
        showWelcomeOnly()
        return
      }
    } else {
      currentConversationId.value = savedConversationId
      records = await getMedicalConversation(savedConversationId)
    }

    if (!records.length) {
      uni.removeStorageSync(storageKey)
      showWelcomeOnly()
      return
    }

    appendConversationMessages(records)
    records.forEach((record) => {
      if (record.processingPhase === 'thinking' || record.processingPhase === 'answering') {
        pollRecord(record.recordId)
      }
    })
    scrollToBottom()
  } catch (error) {
    console.error('恢复 AI 会话失败:', error)
    showWelcomeOnly()
  } finally {
    restoringConversation.value = false
  }
}

const sendMessage = async () => {
  const question = (userInput.value || '').trim()
  if (!question || sending.value) return

  sending.value = true
  selectedTag.value = ''

  messages.value.push({
    key: `q-temp-${Date.now()}`,
    type: 'user',
    text: question
  })
  userInput.value = ''
  scrollToBottom()

  try {
    const record = await submitMedicalQuestion(question, currentConversationId.value)
    if (!record?.recordId) {
      throw new Error('提交 AI 导诊问题失败')
    }

    currentConversationId.value = record.conversationId || currentConversationId.value
    if (currentConversationId.value) {
      uni.setStorageSync(getConversationStorageKey(), currentConversationId.value)
    }
    manualNewConversationStarted.value = false

    upsertAiMessage(record)
    scrollToBottom()
    pollRecord(record.recordId)
  } catch (error) {
    console.error('AI 导诊请求失败:', error)
    messages.value.push({
      key: `a-fail-${Date.now()}`,
      type: 'ai',
      text: '抱歉，当前 AI 导诊服务暂不可用，请稍后重试。',
      processingPhase: 'failed',
      thinkingProcess: '服务暂不可用，请稍后重试'
    })
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  refreshSafeBottom()
  await restoreConversation()
})

onShow(() => {
  if (!manualNewConversationStarted.value) {
    restoreConversation()
  }
})

onUnmounted(() => {
  Array.from(pollTimers.keys()).forEach((recordId) => stopPolling(recordId))
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';

.ai-page {
  min-height: 100vh;
  min-height: 100dvh;
  height: 100vh;
  height: 100dvh;
  width: 100%;
  overflow: hidden;
  background: linear-gradient(180deg, #edf4ff 0%, #f4f7fb 180rpx, #f5f7fa 100%);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.topbar {
  min-height: 88rpx;
  padding: env(safe-area-inset-top) 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: transparent;
  box-sizing: border-box;
}

.topbar-left,
.topbar-right {
  width: 116rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.topbar-left {
  justify-content: flex-start;
}

.topbar-right {
  justify-content: flex-end;
}

.topbar-back {
  font-size: 48rpx;
  line-height: 1;
  color: #1f2937;
  transform: translateY(-2rpx);
}

.topbar-title {
  flex: 1;
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.new-chat-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #2563eb;
}

.hero-card {
  margin: 16rpx 24rpx 10rpx;
  border-radius: 20rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: linear-gradient(135deg, #007aff, #2563eb);
  box-shadow: 0 12rpx 28rpx rgba(79, 149, 240, 0.28);
}

.hero-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 12rpx;
}

.hero-texts {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.hero-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #fff;
}

.hero-subtitle {
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.88);
}

.disclaimer-bar {
  margin: 0 24rpx 10rpx;
  padding: 10rpx 14rpx;
  border-radius: 14rpx;
  background: #fff6e9;
  border: 1rpx solid #ffe2b8;
}

.disclaimer-text {
  font-size: 22rpx;
  line-height: 1.55;
  color: #9a6514;
}

.platform-referral {
  margin: 0 24rpx 10rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  background: #ffffff;
  border: 1rpx solid #dce8fa;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  box-shadow: 0 8rpx 22rpx rgba(31, 41, 55, 0.06);
}

.platform-referral-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.platform-referral-title {
  font-size: 25rpx;
  line-height: 1.35;
  font-weight: 700;
  color: #1f3f6d;
}

.platform-referral-desc {
  font-size: 22rpx;
  line-height: 1.5;
  color: #65758b;
}

.platform-referral-action {
  flex-shrink: 0;
  min-width: 104rpx;
  height: 54rpx;
  line-height: 54rpx;
  text-align: center;
  border-radius: 8rpx;
  background: #1d75f0;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}

.chat-scroll {
  flex: 1;
  height: 0;
  min-height: 0;
  padding: 0 20rpx;
  box-sizing: border-box;
}

.chat-list {
  padding: 8rpx 0 16rpx;
}

.message-row {
  display: flex;
  margin-bottom: 18rpx;
  align-items: flex-start;
}

.row-ai {
  justify-content: flex-start;
}

.row-user {
  justify-content: flex-end;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  border: 1rpx solid #e4ebf5;
  background: #fff;
  flex-shrink: 0;
}

.bubble {
  max-width: 72%;
  border-radius: 20rpx;
  padding: 16rpx 18rpx;
  margin: 0 12rpx;
  line-height: 1.55;
  font-size: 27rpx;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.08);
}

.bubble-ai {
  background: #fff;
  border: 1rpx solid #e6edf6;
  color: #1f2937;
}

.bubble-user {
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #fff;
}

.bubble-head {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 8rpx;
}

.bubble-name {
  font-size: 22rpx;
  color: #6a778b;
}

.phase-badge {
  min-height: 38rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.phase-thinking {
  background: #e9f2ff;
  color: #2563eb;
}

.phase-answering {
  background: #eef8ea;
  color: #2f8f47;
}

.phase-failed {
  background: #fff0f0;
  color: #d14343;
}

.phase-text {
  margin-bottom: 8rpx;
  font-size: 22rpx;
  color: #6a778b;
}

.bubble-content.loading {
  color: #5f6b7b;
}

.composer {
  flex-shrink: 0;
  position: relative;
  z-index: 2;
  background: #fff;
  border-top: 1rpx solid #e7edf5;
  padding: 16rpx 18rpx 8rpx;
  box-sizing: border-box;
  box-shadow: 0 -8rpx 24rpx rgba(31, 41, 55, 0.05);
}

.term-card {
  background: #f4f8ff;
  border: 1rpx solid #dbe8ff;
  border-radius: 16rpx;
  padding: 12rpx;
}

.term-title {
  font-size: 24rpx;
  color: #43648f;
  font-weight: 600;
}

.term-chips {
  width: 100%;
  margin-top: 10rpx;
  white-space: nowrap;
}

.term-chip-list {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  white-space: nowrap;
  gap: 10rpx;
  padding-right: 6rpx;
}

.term-chip {
  flex-shrink: 0;
  min-height: 52rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid #b8d2ff;
  background: #fff;
  color: #2563eb;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
  box-sizing: border-box;
}

.term-chip text {
  display: inline-block;
  white-space: nowrap;
  writing-mode: horizontal-tb;
}

.term-chip.selected {
  background: #007aff;
  border-color: #007aff;
  color: #fff;
}

.input-bar {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.input-box {
  flex: 1;
  height: 76rpx;
  border-radius: 18rpx;
  border: 1rpx solid #d9e4f3;
  background: #f8fafd;
  padding: 0 18rpx;
  font-size: 28rpx;
  color: #1f2937;
  box-sizing: border-box;
}

.send-btn {
  min-width: 128rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #007aff, #2563eb);
  border: none;
  padding: 0 22rpx;
  box-shadow: 0 8rpx 18rpx rgba(79, 149, 240, 0.28);
  transition: all 0.2s ease;
}

.send-btn::after {
  border: none;
}

.send-btn[disabled] {
  opacity: 1;
  color: #6f87ad;
  background: linear-gradient(135deg, #dbe8ff, #cfddf5);
  border: none;
  box-shadow: none;
}

/* #ifdef H5 */
.ai-page {
  max-height: 100dvh;
}

.composer {
  position: sticky;
  bottom: 0;
}
/* #endif */
</style>
