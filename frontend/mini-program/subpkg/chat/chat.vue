<template>
  <view class="container" :class="roleClass">
    <view class="chat-header">
      <view class="header-content">
        <view class="header-back" @click="navigateBack">
          <image class="back-icon" src="/static/back.svg" mode="aspectFit"></image>
        </view>
        <view class="header-title">
          <view class="title-row">
            <text class="title-text">{{ targetName }}</text>
            <view class="online-dot"></view>
          </view>
          <text class="subtitle-text">{{ headerSubtitle }}</text>
        </view>
        <view class="header-placeholder-right"></view>
      </view>
    </view>
    <view class="header-placeholder"></view>

    <scroll-view 
      class="chat-list" 
      scroll-y 
      :scroll-top="scrollTop" 
      :scroll-into-view="scrollIntoView" 
      @scrolltolower="loadMoreHistory"
      upper-threshold="50"
      @click="closePanel"
    >
      <view v-if="loadingMore" class="loading-more"><text>加载中...</text></view>

      <view v-for="(msg, index) in messages" :key="msg.id || index" :id="'msg-' + index">
        <view v-if="shouldShowTime(index)" class="time-divider">
          <text>{{ formatTimeCenter(msg.createTime) }}</text>
        </view>

        <view class="message-item" :class="{ 'self': msg.senderId === currentUserId }">
          <image class="avatar" :src="getAvatar(msg)" @error="handleAvatarError" mode="aspectFill"></image>
          <view class="message-content">
            <text v-if="msg.senderId !== currentUserId" class="sender-name">{{ msg.senderName || targetName }}</text>
            <view class="bubble-container">
              <text v-if="msg.senderId === currentUserId && msg.status !== 'sending' && msg.status !== 'failed'" 
                class="read-status" :class="{ 'read': msg.isRead }">
                {{ msg.isRead ? '已读' : '未读' }}
              </text>
              <view v-if="msg.senderId === currentUserId && msg.status === 'sending'" class="loading-spinner"></view>
              <view v-if="msg.senderId === currentUserId && msg.status === 'failed'" class="fail-icon">!</view>
              <view class="content-bubble" :class="{'voice-bubble': msg.msgType === 3}">
                <text v-if="msg.msgType === 1" class="text">{{ msg.content }}</text>
                <image v-else-if="msg.msgType === 2" class="image" :src="getImageUrl(msg.content)" mode="widthFix" @click="previewImage(getImageUrl(msg.content))"></image>
                <view v-else-if="msg.msgType === 3" class="voice-content" @click="playVoice(msg.content)">
                  <image class="voice-icon-img" src="/static/icons/voice.png" mode="aspectFit"></image>
                  <text class="voice-text">语音消息</text>
                </view>
                <view v-else-if="msg.msgType === 4" class="location-content" @click="openLocation(msg.content)">
                  <image class="location-icon-img" src="/static/icons/location.png" mode="aspectFit"></image>
                  <view class="location-text-wrap">
                    <text class="location-name">{{ parseLocation(msg.content).name || '位置信息' }}</text>
                  </view>
                </view>
                <text v-else class="text">[未知消息类型]</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="bottom-placeholder" :style="{ height: (showPanel ? 500 : 0) + 140 + 'rpx' }"></view>
    </scroll-view>

    <view class="footer-area" :style="{ bottom: keyboardHeight + 'px' }">
      <view class="input-toolbar">
        <view class="icon-btn" @click="switchVoiceMode">
          <image class="icon-img" :src="isVoiceMode ? '/static/icons/keyboard.png' : '/static/icons/voice.png'" mode="aspectFit"></image>
        </view>
        <view class="input-wrapper">
          <view v-if="isVoiceMode" class="voice-record-btn" :class="{ 'recording': recording }"
            @touchstart="startRecord" @touchend="stopRecord" @touchcancel="cancelRecord">
            <text>{{ recording ? '松开 结束' : '按住 说话' }}</text>
          </view>
          <input v-else class="input" v-model="inputText" confirm-type="send" @confirm="sendText"
            :adjust-position="false" @focus="onInputFocus" @blur="onInputBlur" cursor-spacing="20" />
        </view>
        <view class="icon-btn" @click="toggleEmoji">
          <image class="icon-img" src="/static/icons/emoji.png" mode="aspectFit"></image>
        </view>
        <view class="action-btn">
          <view v-if="inputText.trim() && !isVoiceMode" class="send-btn" @click="sendText">
            <text>发送</text>
          </view>
          <view v-else class="icon-btn" @click="toggleMore">
            <image class="icon-img" src="/static/icons/plus.png" mode="aspectFit"></image>
          </view>
        </view>
      </view>

      <view class="panel-area" v-if="showPanel">
        <scroll-view scroll-y v-if="panelType === 'emoji'" class="emoji-panel">
          <view class="emoji-grid">
            <view v-for="(emoji, index) in emojiList" :key="index" class="emoji-item" @click="addEmoji(emoji)">
              {{ emoji }}
            </view>
          </view>
        </scroll-view>
        <view v-if="panelType === 'more'" class="more-panel">
          <view class="more-item" @click="chooseImage('album')">
            <view class="more-icon-box">
              <image class="more-icon-img" src="/static/icons/album.png" mode="aspectFit"></image>
            </view>
            <text class="more-text">相册</text>
          </view>
          <view class="more-item" @click="chooseImage('camera')">
            <view class="more-icon-box">
              <image class="more-icon-img" src="/static/icons/camera.png" mode="aspectFit"></image>
            </view>
            <text class="more-text">拍摄</text>
          </view>
          <view class="more-item" @click="chooseLocation">
            <view class="more-icon-box">
              <image class="more-icon-img" src="/static/icons/location.png" mode="aspectFit"></image>
            </view>
            <text class="more-text">位置</text>
          </view>
          <view class="more-item" @click="sendEmergency">
            <view class="more-icon-box">
              <image class="more-icon-img" src="/static/icons/emergency.png" mode="aspectFit"></image>
            </view>
            <text class="more-text">紧急</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post, upload, config } from '@/utils/api.js'
import { addChatListener, removeChatListener, connectChatSocket } from '@/utils/chat-websocket.js'

const currentUserId = ref(uni.getStorageSync('userInfo')?.id || 0)
const targetUserId = ref(null)
const role = ref(uni.getStorageSync('role') || '')
const targetName = ref(role.value === 'escort' ? '用户' : '陪诊师')
const targetAvatar = ref('/static/user-placeholder.png')
const messages = ref([])
const inputText = ref('')
const scrollTop = ref(0)
const scrollIntoView = ref('')
const loadingMore = ref(false)
const hasMoreHistory = ref(true)
const pageSize = 20
const currentPage = ref(1)

const isVoiceMode = ref(false)
const showPanel = ref(false)
const panelType = ref('')
const keyboardHeight = ref(0)
const recording = ref(false)
const recorderManager = uni.getRecorderManager()
const innerAudioContext = uni.createInnerAudioContext()
const processedMessages = new Set()

const emojiList = ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','😘','🥰','😗','😙','😚','🙂','🤗','🤩','🤔','🤨','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','☹️','🙁','😖','😞','😟','😤','😢','😭','😦','😧','😨','😩','🤯','😬','😰','😱','🥵','🥶','😳','🤪','😵','😡','😠','🤬','😷','🤒','🤕','🤢','🤮','🤧','😇','🤠','🤡','🥳','🥴','🥺','🤥','🤫','🤭','🧐','🤓','😈','👿']

const headerSubtitle = computed(() => (role.value === 'escort' ? '患者 · 在线沟通中' : '陪诊师 · 在线沟通中'))
const roleClass = computed(() => (role.value === 'escort' ? 'role-escort' : 'role-user'))

onLoad((options) => {
  uni.stopPullDownRefresh()
  if (!options.userId && !options.attendantId) { uni.navigateBack(); return }
  targetUserId.value = parseInt(options.userId || options.attendantId)
  targetName.value = options.name || (role.value === 'escort' ? '用户' : '陪诊师')
  if (options.avatar) targetAvatar.value = getImageUrl(options.avatar)
  connectChatSocket()
  loadHistory()
  uni.onKeyboardHeightChange(res => {
    if (res.height > 0) {
      keyboardHeight.value = res.height
      showPanel.value = false
      scrollToBottom()
    } else {
      keyboardHeight.value = 0
    }
  })
  recorderManager.onStop((res) => {
    if (recording.value) {
      sendVoice(res.tempFilePath)
      recording.value = false
    }
  })
})

onMounted(() => {
  addChatListener(handleNewMessage)
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
})

const shouldShowTime = (index) => {
  if (index === 0) return true
  const prevTime = new Date(messages.value[index - 1].createTime).getTime()
  const currTime = new Date(messages.value[index].createTime).getTime()
  return (currTime - prevTime) > 5 * 60 * 1000
}

const loadHistory = async () => {
  try {
    const res = await get(`/api/chat/history?targetUserId=${targetUserId.value}&page=1&pageSize=${pageSize}`)
    if (res.code === 200) {
      messages.value = res.data
      hasMoreHistory.value = res.data.length === pageSize
      setTimeout(() => scrollToBottom(), 100)
    }
  } catch {}
}

const loadMoreHistory = async () => {
  if (loadingMore.value || !hasMoreHistory.value) return
  loadingMore.value = true
  try {
    currentPage.value++
    const res = await get(`/api/chat/history?targetUserId=${targetUserId.value}&page=${currentPage.value}&pageSize=${pageSize}`)
    if (res.code === 200 && res.data.length > 0) {
      messages.value = [...res.data.reverse(), ...messages.value]
      hasMoreHistory.value = res.data.length === pageSize
    } else {
      hasMoreHistory.value = false
    }
  } catch {
    currentPage.value--
  } finally {
    loadingMore.value = false
  }
}

const handleNewMessage = (msg) => {
  const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime}-${msg.content}`
  if (processedMessages.has(msgKey)) return
  processedMessages.add(msgKey)
  const isNormalChatMsg = [1, 2, 3, 4].includes(Number(msg.msgType)) && 
    msg.content !== 'READ_RECEIPT' && 
    msg.type !== 'READ_RECEIPT' && 
    msg.type !== 'MESSAGE_STATUS_UPDATE'
  if (isNormalChatMsg && (msg.senderId == targetUserId.value || msg.receiverId == targetUserId.value)) {
    if (msg.senderId == targetUserId.value) {
      const newMsg = { ...msg }
      if (!newMsg.senderAvatar) newMsg.senderAvatar = targetAvatar.value || '/static/user-placeholder.png'
      if (!messages.value.some(m => m.id == newMsg.id)) {
        messages.value.push(newMsg)
        scrollToBottom()
        markAsRead()
      }
    } else if (msg.receiverId == targetUserId.value) {
      if (!messages.value.some(m => m.id == msg.id)) {
        messages.value.push(msg)
        scrollToBottom()
      }
    }
  }
}

const sendMessage = async (content, type) => {
  const userInfo = uni.getStorageSync('userInfo')
  const tempMsg = {
    id: 'temp-' + Date.now(),
    senderId: currentUserId.value,
    receiverId: targetUserId.value,
    content,
    msgType: type,
    senderName: userInfo?.name || '我',
    senderAvatar: userInfo?.avatar,
    createTime: new Date(),
    status: 'sending',
    isRead: 0
  }
  messages.value.push(tempMsg)
  scrollToBottom()
  const tempIndex = messages.value.length - 1
  try {
    const res = await post('/api/chat/send', { receiverId: targetUserId.value, content, msgType: type })
    if (res.code === 200) messages.value[tempIndex] = { ...res.data, status: 'sent' }
    else throw new Error('Failed')
  } catch {
    messages.value[tempIndex].status = 'failed'
  }
}

const sendingText = ref(false)
const sendText = () => {
  const text = (inputText.value || '').trim()
  if (!text) return
  if (sendingText.value) return
  sendingText.value = true
  inputText.value = ''
  sendMessage(text, 1)
  setTimeout(() => { sendingText.value = false }, 500)
}

const chooseImage = (sourceType) => {
  uni.chooseImage({
    count: 1,
    sourceType: sourceType ? [sourceType] : ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths[0]
      try {
        const uploadRes = await upload('/api/common/upload', path)
        if (uploadRes.code === 200) sendMessage(uploadRes.url, 2)
      } catch {}
    }
  })
}

const sendVoice = async (path) => {
  try {
    const uploadRes = await upload('/api/common/upload', path)
    if (uploadRes.code === 200) sendMessage(uploadRes.url, 3)
  } catch {}
}

const chooseLocation = () => {
  uni.chooseLocation({
    success: (res) => {
      const locationData = JSON.stringify({
        name: res.name,
        address: res.address,
        latitude: res.latitude,
        longitude: res.longitude
      })
      sendMessage(locationData, 4)
    }
  })
}

const sendEmergency = () => {
  sendMessage('【紧急求助】请立即联系我！', 1)
}

const switchVoiceMode = () => {
  isVoiceMode.value = !isVoiceMode.value
  if (isVoiceMode.value) {
    showPanel.value = false
    uni.hideKeyboard()
  }
}

const toggleEmoji = () => {
  if (panelType.value === 'emoji' && showPanel.value) {
    showPanel.value = false
  } else {
    panelType.value = 'emoji'
    showPanel.value = true
    isVoiceMode.value = false
    uni.hideKeyboard()
    scrollToBottom()
  }
}

const toggleMore = () => {
  if (panelType.value === 'more' && showPanel.value) {
    showPanel.value = false
  } else {
    panelType.value = 'more'
    showPanel.value = true
    isVoiceMode.value = false
    uni.hideKeyboard()
    scrollToBottom()
  }
}

const closePanel = () => {
  showPanel.value = false
  uni.hideKeyboard()
}

const onInputFocus = (e) => {
  showPanel.value = false
  keyboardHeight.value = e.detail.height
  scrollToBottom()
}

const onInputBlur = () => {
  keyboardHeight.value = 0
}

const addEmoji = (emoji) => {
  inputText.value += emoji
}

const startRecord = () => {
  recording.value = true
  recorderManager.start()
}

const stopRecord = () => {
  recorderManager.stop()
}

const cancelRecord = () => {
  recording.value = false
  recorderManager.stop()
}

const playVoice = (url) => {
  innerAudioContext.src = getImageUrl(url)
  innerAudioContext.play()
}

const openLocation = (content) => {
  try {
    const loc = JSON.parse(content)
    uni.openLocation({
      latitude: loc.latitude,
      longitude: loc.longitude,
      name: loc.name,
      address: loc.address
    })
  } catch {}
}

const parseLocation = (content) => {
  try { return JSON.parse(content) } catch { return {} }
}

const markAsRead = () => {
  post(`/api/chat/read?senderId=${targetUserId.value}`)
}

const navigateBack = () => {
  uni.$emit('chat:return', { targetUserId: targetUserId.value })
  const pages = getCurrentPages()
  if (pages && pages.length > 1) {
    uni.navigateBack({
      fail: () => {
        if (role.value === 'escort') {
          uni.reLaunch({ url: '/pages/role-escort/message' })
        } else {
          uni.switchTab({ url: '/pages/role-user/message' })
        }
      }
    })
    return
  }
  if (role.value === 'escort') {
    uni.reLaunch({ url: '/pages/role-escort/message' })
  } else {
    uni.switchTab({ url: '/pages/role-user/message' })
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoView.value = 'msg-' + (messages.value.length - 1)
  })
}

const getAvatar = (msg) => {
  if (msg.senderId === currentUserId.value) {
    const currentUserAvatar = uni.getStorageSync('userInfo')?.avatar
    const avatarUrl = currentUserAvatar || '/static/user-placeholder.png'
    return getImageUrl(avatarUrl)
  }
  if (msg.senderAvatar) return getImageUrl(msg.senderAvatar)
  if (targetAvatar.value) return targetAvatar.value
  return '/static/user-placeholder.png'
}

const getImageUrl = (url) => {
  if (!url) return '/static/user-placeholder.png'
  if (url.startsWith('http') || url.startsWith('wxfile')) return url
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL
  return baseUrl + (url.startsWith('/') ? url : '/' + url)
}

const previewImage = (url) => uni.previewImage({ urls: [url], current: url })

const handleAvatarError = () => {}

const formatTimeCenter = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return `${d.getMonth()+1}月${d.getDate()}日 ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
@import '@/styles/escort-ui.scss';
$bg-color: #F5F7FA;
$text-main: #333;
$bubble-other: #FFF;
$bubble-self: var(--chat-primary);

.container {
  --chat-primary: #{$user-color-primary};
  --chat-primary-deep: #{$user-color-primary-deep};
  height: 100vh;
  background-color: $bg-color;
  display: flex;
  flex-direction: column;
}
.container.role-escort {
  --chat-primary: #{$escort-color-primary};
  --chat-primary-deep: #{$escort-color-primary-deep};
}
.chat-header { position: fixed; top: 0; left: 0; width: 100%; z-index: 100; background-color: #fff; padding-top: var(--status-bar-height); box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.header-content { height: 88rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx; }
.header-back { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; }
.back-icon { width: 40rpx; height: 40rpx; }
.header-title { display: flex; flex-direction: column; align-items: flex-start; gap: 4rpx;
  .title-row { display: flex; align-items: center; gap: 10rpx; }
  .title-text { font-size: 32rpx; font-weight: 600; color: $text-main; }
  .online-dot { width: 12rpx; height: 12rpx; background: #07c160; border-radius: 50%; }
  .subtitle-text { font-size: 22rpx; color: #999; }
}
.header-placeholder-right { width: 60rpx; }
.header-placeholder { width: 100%; height: calc(88rpx + var(--status-bar-height)); flex-shrink: 0; }

.chat-list { flex: 1; width: 100%; box-sizing: border-box; padding: 24rpx; overflow-y: scroll; -webkit-overflow-scrolling: touch; }
.time-divider { display: flex; justify-content: center; margin: 32rpx 0; text { font-size: 22rpx; color: #999; background: rgba(0,0,0,0.05); padding: 4rpx 16rpx; border-radius: 8rpx; } }
.loading-more { text-align: center; padding: 10rpx; font-size: 22rpx; color: #999; }

.message-item { display: flex; margin-bottom: 30rpx; align-items: flex-start; &.self { flex-direction: row-reverse; } }
.avatar { width: 80rpx; height: 80rpx; border-radius: 12rpx; flex-shrink: 0; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05); background: #fff; }
.message-content { display: flex; flex-direction: column; max-width: 70%; margin: 0 20rpx; .self & { align-items: flex-end; } }
.sender-name { font-size: 22rpx; color: #999; margin-bottom: 4rpx; margin-left: 8rpx; }

.bubble-container { display: flex; align-items: flex-end; gap: 10rpx; .self & { flex-direction: row-reverse; } }
.read-status { font-size: 20rpx; color: #999; margin-bottom: 10rpx; white-space: nowrap; &.read { color: #999; } }
.loading-spinner { width: 24rpx; height: 24rpx; border: 2rpx solid #ccc; border-top-color: var(--chat-primary); border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 10rpx; }
.fail-icon { width: 30rpx; height: 30rpx; background: #ff4d4f; color: #fff; border-radius: 50%; font-size: 20rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 10rpx; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.content-bubble {
  position: relative; padding: 20rpx 24rpx; border-radius: 12rpx; font-size: 30rpx; line-height: 1.5; box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.04);
  background: $bubble-other; color: $text-main;
  .self & { background: $bubble-self; color: #fff; }
  .image { max-width: 300rpx; border-radius: 8rpx; display: block; }
}

.voice-content { display: flex; align-items: center; gap: 12rpx; }
.voice-icon-img { width: 32rpx; height: 32rpx; }
.location-content { display: flex; align-items: center; gap: 12rpx; }
.location-icon-img { width: 32rpx; height: 32rpx; }
.location-name { font-size: 28rpx; color: $text-main; }

.bottom-placeholder { height: 140rpx; transition: height 0.3s; }

/* 底部区域样式 */
.footer-area {
  position: fixed; left: 0; width: 100%; z-index: 100;
  background: #F7F7F7; border-top: 1rpx solid #E5E5E5;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  transition: bottom 0.1s;
}

.input-toolbar { display: flex; align-items: center; padding: 16rpx 20rpx; min-height: 100rpx; box-sizing: border-box; }
.input-wrapper { flex: 1; margin: 0 20rpx; }
.input { width: 100%; height: 72rpx; background: #FFFFFF; border-radius: 12rpx; padding: 0 20rpx; font-size: 30rpx; box-sizing: border-box; }
.voice-record-btn {
  width: 100%; height: 72rpx; background: #FFFFFF; border-radius: 12rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 30rpx; color: #333; font-weight: 500;
  &.recording { background: #E5E5E5; }
}
.icon-btn { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; }
.icon-img { width: 56rpx; height: 56rpx; }
.action-btn { width: 100rpx; display: flex; align-items: center; justify-content: center; margin-left: 10rpx; }
.send-btn { background: #07C160; color: #fff; padding: 10rpx 20rpx; border-radius: 8rpx; font-size: 26rpx; }

/* 面板区域 */
.panel-area { height: 500rpx; background: #F7F7F7; border-top: 1rpx solid #E5E5E5; overflow: hidden; }
.emoji-panel { height: 100%; padding: 20rpx; box-sizing: border-box; }
.emoji-grid { display: flex; flex-wrap: wrap; }
.emoji-item { width: 12.5%; height: 80rpx; display: flex; align-items: center; justify-content: center; font-size: 40rpx; }
.more-panel { height: 100%; display: flex; flex-wrap: wrap; padding: 40rpx 30rpx; box-sizing: border-box; }
.more-item { width: 25%; display: flex; flex-direction: column; align-items: center; margin-bottom: 40rpx; }
.more-icon-box { width: 110rpx; height: 110rpx; background: #FFFFFF; border-radius: 24rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 10rpx; }
.more-icon-img { width: 64rpx; height: 64rpx; }
.more-text { font-size: 24rpx; color: #666; }
.voice-content { display: flex; align-items: center; gap: 10rpx; min-width: 120rpx; }
.location-content { display: flex; align-items: center; gap: 10rpx; }
</style>
