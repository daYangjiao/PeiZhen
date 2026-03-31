<template>
  <view class="ai-page">
    <view class="hero-card">
      <view class="hero-icon-wrap">
        <image class="hero-icon" :src="AIAvatar" mode="aspectFill" />
      </view>
      <view class="hero-texts">
        <text class="hero-title">智能医疗助手</text>
        <text class="hero-subtitle">仅提供健康科普参考，不能替代医生诊疗</text>
      </view>
    </view>

    <view class="disclaimer-bar">
      <text class="disclaimer-text">免责声明：AI 回复仅供参考，不构成诊断、治疗或用药建议；如有不适请及时就医。</text>
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
          :key="index"
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
            <text v-if="msg.type === 'ai'" class="bubble-name">医疗助手</text>
            <view class="bubble-content" :class="{ loading: msg.loading }">
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
        <text class="term-title">医疗术语联想</text>
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
          placeholder="请输入您的健康问题..."
          confirm-type="send"
          @confirm="sendMessage"
          :disabled="sending"
        />
        <button class="send-btn" :disabled="sending || !userInput.trim()" @click="sendMessage">
          {{ sending ? '发送中' : '发送' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { askMedicalQuestion } from './api.js'
import { brandAiAvatar, userPlaceholder } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 获取当前用户头像（优先使用 userStore，与 profile 页面保持一致）
const getUserAvatar = () => {
  const avatar = userStore.avatar || uni.getStorageSync('userInfo')?.avatar || ''
  return resolveAvatarUrl(avatar, userPlaceholder)
}

const AIAvatar = brandAiAvatar

const messages = ref([
  {
    type: 'ai',
    text: '您好，我是智能医疗助手。您可以描述症状或检查结果，我会提供科普参考信息，不能替代医生面诊。'
  }
])

const userInput = ref('')
const selectedTag = ref('')
const sending = ref(false)
const scrollIntoView = ref('')
const composerPaddingBottom = ref(8)

const suggestions = [
  '紧张性头痛',
  '过敏性咳嗽',
  '上呼吸道感染',
  '支气管炎',
  '胃炎',
  '鼻炎',
  '咽炎',
  '结膜炎'
]

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

const selectTag = (tag) => {
  selectedTag.value = tag
  userInput.value = tag
}

const sendMessage = async () => {
  const question = (userInput.value || '').trim()
  if (!question || sending.value) return

  sending.value = true
  messages.value.push({ type: 'user', text: question })
  userInput.value = ''
  scrollToBottom()

  const loadingIndex = messages.value.length
  messages.value.push({ type: 'ai', text: '正在分析您的问题，请稍候...', loading: true })
  scrollToBottom()

  try {
    const result = await askMedicalQuestion(question)
    const answer = result?.answer || '抱歉，当前无法回答您的问题，请稍后重试。'
    messages.value.splice(loadingIndex, 1, { type: 'ai', text: answer })
  } catch (error) {
    console.error('AI 回答失败:', error)
    messages.value.splice(loadingIndex, 1, { type: 'ai', text: '抱歉，当前无法回答您的问题，请稍后重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  refreshSafeBottom()
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';

.ai-page {
  min-height: 100vh;
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  height: calc(100dvh - var(--window-top, 0px) - var(--window-bottom, 0px));
  width: 100%;
  overflow: hidden;
  background: linear-gradient(180deg, #edf4ff 0%, #f4f7fb 180rpx, #f5f7fa 100%);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.hero-card {
  margin: 16rpx 24rpx 10rpx;
  border-radius: 20rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: linear-gradient(135deg, #007AFF, #2563EB);
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
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
}

.bubble-name {
  font-size: 22rpx;
  color: #6a778b;
  margin-bottom: 6rpx;
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
  color: #2563EB;
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
  background: #007AFF;
  border-color: #007AFF;
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
  background: linear-gradient(135deg, #007AFF, #2563EB);
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
  max-height: calc(100dvh - var(--window-top, 0px) - var(--window-bottom, 0px));
}

.composer {
  position: sticky;
  bottom: 0;
}
/* #endif */
</style>
