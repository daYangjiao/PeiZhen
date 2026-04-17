<template>
  <view class="ai-page">
    <view class="topbar">
      <view class="topbar-left" @click="goBack">
        <text class="topbar-back">‹</text>
      </view>
      <text class="topbar-title">AI 帮我找</text>
      <view class="topbar-right" @click="startNewSession">
        <text class="new-chat-text">新会话</text>
      </view>
    </view>

    <view v-if="showIntroCards" class="intro-stack">
      <view class="hero-card" :class="{ compact: hasStartedConversation }">
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
        <text class="tip-text">明天 09:00-11:00，我去华西医院复诊，需要陪诊帮我取号和陪同检查。</text>
      </view>
    </view>

    <view v-else class="intro-hint" @click="toggleIntroCards">
      <text class="intro-hint-text">继续补充需求，或点我查看示例</text>
      <text class="intro-hint-arrow">{{ showIntroCards ? '⌃' : '⌄' }}</text>
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
          :class="msg.type === 'system' ? 'row-system' : (msg.type === 'user' ? 'row-user' : 'row-ai')"
          :id="`msg-${index}`"
        >
          <template v-if="msg.type === 'system'">
            <view class="history-divider">
              <text class="history-divider-text">{{ msg.text }}</text>
            </view>
          </template>
          <template v-else>
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
          </template>
        </view>

        <view v-if="showInlineConfirmCard" id="confirm-card" class="inline-card confirm-card">
          <view class="inline-card-head">
            <view>
              <text class="inline-card-title">确认预约信息</text>
              <text class="inline-card-subtitle">根据您的要求，我先整理出这些预约信息，您看是否正确。</text>
            </view>
            <view class="summary-badge">
              <text>待确认</text>
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

          <view class="inline-card-actions">
            <button class="summary-secondary-btn" :disabled="sending || matchingInProgress || navigatingToResult" @click="continueEditing">
              继续修改
            </button>
            <button class="summary-primary-btn" :disabled="sending || matchingInProgress || navigatingToResult" @click="confirmAndStartMatch">
              确认并开始匹配
            </button>
          </view>
        </view>

        <view v-if="showMatchingInfoCard" id="matching-info-card" class="inline-card matching-card">
          <view class="inline-card-head">
            <view>
              <text class="inline-card-title">已确认的信息</text>
              <text class="inline-card-subtitle">{{ matchingStageText }}</text>
            </view>
            <view class="summary-badge active">
              <text>匹配中</text>
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

          <view class="summary-loading">
            <view class="matching-dots">
              <text class="matching-dot"></text>
              <text class="matching-dot"></text>
              <text class="matching-dot"></text>
            </view>
            <text class="summary-loading-text">{{ matchingStageText }}</text>
          </view>
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

    <view v-if="showTimePickerSheet" class="sheet-overlay" @click="cancelTimePicker">
      <view class="sheet-panel time-sheet-panel" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-title">补充就诊时间</text>
            <text class="sheet-subtitle">请选择和普通预约一致的开始、结束时间</text>
          </view>
        </view>

        <scroll-view class="sheet-scroll" scroll-y>
          <appointment-time-range-picker
            v-model:start-time="pickerStartTime"
            v-model:end-time="pickerEndTime"
            :selected-date="structuredDemand.serviceDate"
            compact
            hide-header
          />
        </scroll-view>

        <view class="sheet-actions">
          <button class="summary-secondary-btn" :disabled="sending" @click="cancelTimePicker">
            稍后再选
          </button>
          <button class="summary-primary-btn" :disabled="!canSubmitPickedTime || sending" @click="submitPickedTimeRange">
            确认这个时间段
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { brandAiAvatar, userPlaceholder } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { useUserStore } from '@/stores/user'
import { post as apiPost } from '@/utils/api.js'
import AppointmentTimeRangePicker from '@/components/appointment-time-range-picker.vue'
import {
  getAiAppointmentSession,
} from './api.js'

const AIAvatar = brandAiAvatar
const userStore = useUserStore()
const POLL_INTERVAL = 1500
const MATCH_WAIT_LIMIT = 30000
const AI_APPOINTMENT_DRAFT_KEY = 'ai_appointment_draft_pending'
const AI_APPOINTMENT_SESSION_KEY = 'ai_appointment_current_session_id'

const suggestions = [
  '明天 09:00-11:00，我去华西医院复诊，需要陪诊帮我取号',
  '下周一 14:00-16:00，我去省医院检查，希望熟悉医院流程',
  '周五 19:00-21:00，我去急诊复查，希望反应快一点',
  '后天 08:30-10:30，我去协和医院取药，希望有护士经验'
]

const messages = ref([createWelcomeMessage()])
const userInput = ref('')
const sending = ref(false)
const matchingInProgress = ref(false)
const scrollIntoView = ref('')
const composerPaddingBottom = ref(8)
const sessionId = ref('')
const currentQuestionType = ref('')
const currentAssistantIntent = ref('')
const activeOptions = ref([])
const followUpRound = ref(0)
const activeFollowUpType = ref('')
const navigatingToResult = ref(false)
const activeAssistantKey = ref('')
const matchDeadlineTimer = ref(null)
const structuredDemand = ref(createEmptyStructuredDemand())
const readyToMatch = ref(false)
const introExpanded = ref(true)
const showTimePickerSheet = ref(false)
const pickerStartTime = ref('')
const pickerEndTime = ref('')
const currentTimeProposal = ref(null)
const matchingStageIndex = ref(0)
const restoringSession = ref(false)
const showHistoryDivider = ref(false)
const loadedFromQuerySession = ref(false)
const manualNewSessionStarted = ref(false)
let pollTimer = null
let matchingStageTimer = null

function createWelcomeMessage() {
  return {
    key: 'welcome-ai-appointment',
    type: 'ai',
    text: '您好，我可以帮您先梳理预约需求，再推荐更合适的陪诊师。您直接描述时间段、医院和就诊情况就可以。',
    processingPhase: 'completed',
    thinkingProcess: ''
  }
}

function createSystemMessage(text = '') {
  return {
    key: `sys-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type: 'system',
    text
  }
}

function createChatMessage(role = 'assistant', content = '', processingPhase = 'completed', thinkingProcess = '') {
  return {
    key: `${role}-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type: role === 'user' ? 'user' : 'ai',
    text: content,
    processingPhase,
    thinkingProcess,
    waitingMatch: false
  }
}

function resetToWelcomeMessage() {
  messages.value = [createWelcomeMessage()]
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
    composerPaddingBottom.value = 8
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoView.value = `msg-${messages.value.length - 1}`
  })
}

const scrollToAnchor = (anchorId) => {
  nextTick(() => {
    scrollIntoView.value = anchorId
  })
}

const getPhaseLabel = (phase = '') => {
  if (phase === 'thinking') return '思考中'
  if (phase === 'answering') return '回答中'
  if (phase === 'failed') return '失败'
  return '已完成'
}

function getCurrentUserName() {
  return userStore.displayName || userStore.userInfo?.name || userStore.userInfo?.nickName || userStore.userInfo?.phone || '本人'
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

const normalizeString = (value = '') => String(value || '').trim()

const buildMessagesFromHistory = (historyMessages = [], includeHistoryDivider = false) => {
  const restored = Array.isArray(historyMessages)
    ? historyMessages
      .filter((item) => normalizeString(item?.role) && normalizeString(item?.content))
      .map((item) => createChatMessage(
        item.role === 'user' ? 'user' : 'assistant',
        normalizeString(item.content),
        item.role === 'assistant' ? (item.processingPhase || 'completed') : 'completed',
        item.role === 'assistant' ? (item.thinkingProcess || '') : ''
      ))
    : []

  if (!restored.length) {
    return [createWelcomeMessage()]
  }
  if (includeHistoryDivider) {
    restored.push(createSystemMessage('以上为之前的聊天记录'))
  }
  return restored
}

const getLastConversationMessage = () => {
  return [...messages.value].reverse().find((item) => item.type === 'ai' || item.type === 'user') || null
}

const shouldUpsertAssistantMessage = (state = {}) => {
  if (!state) return false
  if (state.processingPhase && state.processingPhase !== 'completed') {
    return true
  }
  const assistantReply = normalizeString(state.assistantReply || state.message || '')
  if (!assistantReply) {
    return false
  }
  const lastMessage = getLastConversationMessage()
  if (!lastMessage || lastMessage.type !== 'ai') {
    return true
  }
  return normalizeString(lastMessage.text) !== assistantReply
}

const uniqueList = (items = []) => {
  return Array.from(new Set((Array.isArray(items) ? items : [items]).map((item) => normalizeString(item)).filter(Boolean)))
}

const normalizeClockToken = (value = '') => {
  const text = normalizeString(value)
  if (!text) return ''
  const match = text.match(/^(\d{1,2})[:：.](\d{1,2})$/)
  if (!match) return ''
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return ''
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return ''
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

const normalizeFlexibleClockToken = (value = '') => {
  const text = normalizeString(value).replace(/\s+/g, '').replace(/：/g, ':').replace(/时/g, '点')
  if (!text) return ''
  if (/^\d{1,2}$/.test(text)) {
    return `${text.padStart(2, '0')}:00`
  }
  let match = text.match(/^(\d{1,2})[:.](\d{1,2})$/)
  if (match) {
    return normalizeClockToken(`${match[1]}:${match[2]}`) || ''
  }
  match = text.match(/^(\d{1,2})点半$/)
  if (match) {
    return normalizeClockToken(`${match[1]}:30`) || ''
  }
  match = text.match(/^(\d{1,2})点(?:(\d{1,2})分?)?$/)
  if (match) {
    return normalizeClockToken(`${match[1]}:${match[2] || '0'}`) || ''
  }
  return ''
}

const normalizeExactTimePeriod = (value = '') => {
  const text = normalizeString(value)
    .replace(/[～~至到—]/g, '-')
    .replace(/：/g, ':')
    .replace(/\s+/g, '')
  if (!text) return ''
  const match = text.match(/(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)\s*-\s*(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)/)
  if (!match) return ''
  const start = normalizeFlexibleClockToken(match[1])
  const end = normalizeFlexibleClockToken(match[2])
  if (!start || !end) return ''
  return `${start}-${end}`
}

const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addDays = (date, days) => {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

const formatDateLabel = (dateKey = '') => {
  if (!dateKey) return ''
  const safe = String(dateKey).trim().replace(/\//g, '-')
  const match = safe.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (!match) return safe
  return `${Number(match[1])}年${Number(match[2])}月${Number(match[3])}日`
}

const extractServiceDate = (text = '') => {
  const normalized = normalizeString(text).replace(/\s+/g, '')
  if (!normalized) return ''
  const now = new Date()
  now.setHours(0, 0, 0, 0)

  const explicit = normalized.match(/(\d{4})[年./-](\d{1,2})[月./-](\d{1,2})(?:日|号)?/)
  if (explicit) {
    const date = new Date(Number(explicit[1]), Number(explicit[2]) - 1, Number(explicit[3]))
    if (!Number.isNaN(date.getTime())) return formatDateKey(date)
  }

  const relativeMap = { 今天: 0, 明天: 1, 后天: 2, 大后天: 3 }
  for (const [keyword, offset] of Object.entries(relativeMap)) {
    if (normalized.includes(keyword)) {
      return formatDateKey(addDays(now, offset))
    }
  }

  const weekdayMatch = normalized.match(/(本周|这周|下周|周|星期)([一二三四五六日天])/)
  if (weekdayMatch) {
    const weekdayIndexMap = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 日: 0, 天: 0 }
    const prefix = weekdayMatch[1]
    const target = weekdayIndexMap[weekdayMatch[2]]
    const current = now.getDay()
    let diff = target - current
    if (prefix === '下周') {
      diff += 7
    } else if (prefix === '本周' || prefix === '这周') {
      if (diff < 0) diff += 7
    } else {
      if (diff <= 0) diff += 7
    }
    return formatDateKey(addDays(now, diff))
  }

  return ''
}

const extractTimeWindow = (text = '') => {
  const normalized = normalizeString(text).replace(/\s+/g, '').replace(/：/g, ':').replace(/时/g, '点')
  if (!normalized) return { serviceStartTime: '', serviceEndTime: '' }
  const patterns = [
    /(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)\s*[-~–—至到]\s*(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)/,
    /(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)\s*到\s*(\d{1,2}(?:[:.]\d{1,2}|点半|点\d{1,2}分?|点)?)/
  ]
  for (const pattern of patterns) {
    const match = normalized.match(pattern)
    if (!match) continue
    const start = normalizeFlexibleClockToken(match[1])
    const end = normalizeFlexibleClockToken(match[2])
    if (start && end) {
      return { serviceStartTime: start, serviceEndTime: end }
    }
  }
  return { serviceStartTime: '', serviceEndTime: '' }
}

const extractPatientProfile = (text = '') => {
  const normalized = normalizeString(text)
  const profileRules = [
    { pattern: /(老爷子|爷爷|老爷|爸爸|父亲|大伯|叔叔|舅舅)/, value: '老人' },
    { pattern: /(奶奶|外婆|妈妈|母亲|阿姨|大娘|婶婶)/, value: '女性长辈' },
    { pattern: /(孩子|小孩|宝宝|儿童|小朋友)/, value: '儿童' },
    { pattern: /(本人|我自己|我)/, value: '本人' }
  ]
  for (const rule of profileRules) {
    if (rule.pattern.test(normalized)) return rule.value
  }
  return ''
}

const extractHospital = (text = '') => {
  const normalized = normalizeString(text)
  if (!normalized) return ''
  const match = normalized.match(/([\u4e00-\u9fa5A-Za-z0-9·（）()\-]{2,24}?医院)/)
  return match ? normalizeString(match[1]) : ''
}

const HOSPITAL_STOP_WORDS = ['需要', '希望', '找', '带', '陪', '复诊', '检查', '咨询', '预约', '去', '到', '前往', '就诊', '看病']
const DEPARTMENT_KEYWORDS = [
  '心内科', '心血管内科', '消化内科', '呼吸内科', '神经内科', '内科', '外科', '骨科', '儿科', '妇科', '产科',
  '急诊科', '眼科', '耳鼻喉科', '口腔科', '皮肤科', '肿瘤科', '康复科', '老年科', '内分泌科', '风湿免疫科',
  '感染科', '精神科', '中医科', '体检科', '泌尿外科', '普外科', '神经外科', '胸外科', '心外科', '胸外科'
]
const SYMPTOM_KEYWORDS = [
  '复诊', '复查', '检查', '拍片', 'CT', '核磁', 'MRI', 'B超', '抽血', '化验', '住院', '出院', '手术', '术后',
  '换药', '取药', '领药', '疼痛', '胃痛', '头痛', '头晕', '发烧', '咳嗽', '胸闷', '胸痛', '腹痛', '呕吐',
  '乏力', '骨折', '炎症', '随访', '体检', '开药', '复查结果', '拿结果', '转诊', '问诊'
]
const PREFERENCE_KEYWORDS = [
  '男陪诊师', '女陪诊师', '男陪诊', '女陪诊', '推轮椅', '轮椅', '有力气', '力气大', '懂急救', '急救',
  '护士经验', '护士', '熟悉医院', '熟悉流程', '熟悉', '耐心', '会沟通', '沟通好', '会英语', '老人陪护',
  '照顾老人', '陪老人', '跑腿快', '细心', '经验丰富', '熟路', '会开车'
]
const EXPLICIT_MEDICAL_SCENE_KEYWORDS = ['复诊', '复查', '检查', '取药', '拿药', '拿结果', '取结果', '体检', '开药', '问诊', '术后', '换药']

const extractDepartment = (text = '') => {
  const normalized = normalizeString(text)
  if (!normalized) return ''
  const hit = DEPARTMENT_KEYWORDS.find((keyword) => normalized.includes(keyword))
  return hit || ''
}

const extractGenderPreference = (text = '') => {
  const normalized = normalizeString(text)
  if (!normalized) return ''
  if (/(男陪诊师|男陪诊|男性|男的|男)/.test(normalized)) return '男'
  if (/(女陪诊师|女陪诊|女性|女的|女)/.test(normalized)) return '女'
  return ''
}

const extractMedicalClauses = (text = '') => {
  const normalized = normalizeString(text)
  if (!normalized) return []
  const clauses = normalized
    .split(/[，。！？；、,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
  return clauses.filter((clause) => SYMPTOM_KEYWORDS.some((keyword) => clause.includes(keyword)) || /(复诊|检查|术后|体检|住院|出院|开药|取药|拿结果)/.test(clause))
}

const extractPreferenceTags = (text = '') => {
  const normalized = normalizeString(text)
  if (!normalized) return []
  return PREFERENCE_KEYWORDS.filter((keyword) => normalized.includes(keyword))
}

function classifyDemandText(text = '') {
  const rawDemandText = normalizeString(text)
  const hospital = extractHospital(rawDemandText)
  const date = extractServiceDate(rawDemandText)
  const { serviceStartTime, serviceEndTime } = extractTimeWindow(rawDemandText)
  const patientProfile = extractPatientProfile(rawDemandText)
  const attendantGender = extractGenderPreference(rawDemandText)
  const symptomClauses = extractMedicalClauses(rawDemandText)
  const symptomTags = uniqueList(
    symptomClauses
      .flatMap((clause) => SYMPTOM_KEYWORDS.filter((keyword) => clause.includes(keyword)))
  )
  const preferenceTags = uniqueList(extractPreferenceTags(rawDemandText))

  let symptomDescription = symptomClauses.join('，')
  if (!symptomDescription) {
    const scene = EXPLICIT_MEDICAL_SCENE_KEYWORDS.find((keyword) => rawDemandText.includes(keyword))
    if (scene) {
      symptomDescription = scene
    }
  }

  let otherRequirement = preferenceTags.join('、')
  if (!otherRequirement && attendantGender) {
    otherRequirement = `${attendantGender}陪诊师`
  }

  return sanitizeStructuredDemand({
    patientName: getCurrentUserName(),
    patientProfile,
    hospital,
    serviceDate: date,
    serviceStartTime,
    serviceEndTime,
    symptomDescription: symptomDescription === rawDemandText ? '' : symptomDescription,
    symptomTags,
    otherRequirement,
    preferenceTags,
    attendantGender,
    rawDemandText
  })
}

function sanitizeStructuredDemand(payload = {}) {
  return {
    patientName: normalizeString(payload.patientName) || getCurrentUserName(),
    patientProfile: normalizeString(payload.patientProfile),
    hospital: normalizeString(payload.hospital),
    serviceDate: normalizeString(payload.serviceDate),
    serviceStartTime: normalizeFlexibleClockToken(payload.serviceStartTime),
    serviceEndTime: normalizeFlexibleClockToken(payload.serviceEndTime),
    symptomDescription: normalizeString(payload.symptomDescription),
    symptomTags: uniqueList(payload.symptomTags),
    otherRequirement: normalizeString(payload.otherRequirement),
    preferenceTags: uniqueList(payload.preferenceTags),
    attendantGender: normalizeString(payload.attendantGender),
    rawDemandText: normalizeString(payload.rawDemandText)
  }
}

const persistStructuredDemand = () => {
  try {
    uni.setStorageSync(AI_APPOINTMENT_DRAFT_KEY, structuredDemand.value)
    if (sessionId.value) {
      uni.setStorageSync(`ai_appointment_draft_${sessionId.value}`, structuredDemand.value)
    }
  } catch (error) {
    console.warn('保存 AI 预约草稿失败:', error)
  }
}

const persistCurrentSessionId = (id = '') => {
  try {
    const normalizedId = normalizeString(id)
    if (normalizedId) {
      uni.setStorageSync(AI_APPOINTMENT_SESSION_KEY, normalizedId)
    }
  } catch (error) {
    console.warn('保存 AI 预约会话失败:', error)
  }
}

const readCurrentSessionId = () => {
  try {
    return normalizeString(uni.getStorageSync(AI_APPOINTMENT_SESSION_KEY))
  } catch (error) {
    return ''
  }
}

const clearCurrentSessionCache = (targetSessionId = '') => {
  const normalizedSessionId = normalizeString(targetSessionId || sessionId.value)
  try {
    uni.removeStorageSync(AI_APPOINTMENT_SESSION_KEY)
    uni.removeStorageSync(AI_APPOINTMENT_DRAFT_KEY)
    if (normalizedSessionId) {
      uni.removeStorageSync(getStructuredDemandStorageKey(normalizedSessionId))
    }
  } catch (error) {
    console.warn('清理 AI 预约会话缓存失败:', error)
  }
}

const startNewSession = () => {
  const previousSessionId = sessionId.value
  stopPolling()
  stopMatchDeadline()
  stopMatchingStageRotation()
  cancelTimePicker()
  clearCurrentSessionCache(previousSessionId)

  sessionId.value = ''
  userInput.value = ''
  sending.value = false
  matchingInProgress.value = false
  currentQuestionType.value = ''
  currentAssistantIntent.value = ''
  activeOptions.value = []
  followUpRound.value = 0
  activeFollowUpType.value = ''
  navigatingToResult.value = false
  activeAssistantKey.value = ''
  structuredDemand.value = createEmptyStructuredDemand()
  readyToMatch.value = false
  introExpanded.value = true
  currentTimeProposal.value = null
  matchingStageIndex.value = 0
  showHistoryDivider.value = false
  loadedFromQuerySession.value = false
  manualNewSessionStarted.value = true
  resetToWelcomeMessage()
  scrollToBottom()
}

const restoreStructuredDemand = () => {
  try {
    const cached = uni.getStorageSync(AI_APPOINTMENT_DRAFT_KEY)
    if (cached && typeof cached === 'object') {
      structuredDemand.value = sanitizeStructuredDemand({ ...createEmptyStructuredDemand(), ...cached })
    }
  } catch (error) {
    console.warn('恢复 AI 预约草稿失败:', error)
  }
}

const refreshStructuredDemandFromText = (text = '') => {
  const parsed = classifyDemandText(text)
  structuredDemand.value = sanitizeStructuredDemand({
    ...structuredDemand.value,
    ...parsed,
    patientName: getCurrentUserName(),
    rawDemandText: text || structuredDemand.value.rawDemandText
  })
  persistStructuredDemand()
  return structuredDemand.value
}

const applyStructuredSelectionPatch = (fieldKey, selectedValue, displayText = selectedValue) => {
  const patch = { ...structuredDemand.value }
  const normalizedValue = normalizeString(selectedValue)
  if (!normalizedValue) return false

  if (fieldKey === 'timePeriod' || fieldKey === 'timeRange' || fieldKey === 'timeRangeConfirmed') {
    const exact = normalizeExactTimePeriod(normalizedValue)
    if (exact) {
      const [startTime, endTime] = exact.split('-')
      patch.serviceStartTime = startTime
      patch.serviceEndTime = endTime
      patch.timePeriod = '具体时间'
    } else {
      patch.timePeriod = normalizedValue
    }
  } else if (fieldKey === 'serviceDate') {
    patch.serviceDate = normalizedValue
  } else if (fieldKey === 'hospital') {
    patch.hospital = normalizedValue
  } else if (fieldKey === 'patientProfile') {
    patch.patientProfile = normalizedValue
  } else if (fieldKey === 'symptomDescription') {
    patch.symptomDescription = normalizedValue
  } else if (fieldKey === 'otherRequirement') {
    patch.otherRequirement = normalizedValue
  } else if (fieldKey === 'attendantGender') {
    patch.attendantGender = normalizedValue
  } else {
    patch[fieldKey] = normalizedValue
  }

  patch.rawDemandText = [patch.rawDemandText, displayText].filter(Boolean).join(' ').trim()
  structuredDemand.value = sanitizeStructuredDemand({ ...patch, patientName: getCurrentUserName() })
  persistStructuredDemand()
  return true
}

const formatDateDisplay = (dateKey = '') => {
  return formatDateLabel(dateKey)
}

const formatTimeDisplay = (startTime = '', endTime = '') => {
  if (!startTime && !endTime) return ''
  if (startTime && endTime) return `${startTime}-${endTime}`
  return startTime || endTime || ''
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
    value: [formatDateDisplay(structuredDemand.value.serviceDate), formatTimeDisplay(structuredDemand.value.serviceStartTime, structuredDemand.value.serviceEndTime)].filter(Boolean).join(' '),
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

const hasStartedConversation = computed(() => messages.value.some((item) => item.type === 'user'))
const showIntroCards = computed(() => !hasStartedConversation.value || introExpanded.value)
const showMatchingInfoCard = computed(() => matchingInProgress.value)
const canSubmitPickedTime = computed(() => !!structuredDemand.value.serviceDate && !!pickerStartTime.value && !!pickerEndTime.value)
const showInlineConfirmCard = computed(() => readyToMatch.value && !matchingInProgress.value && !navigatingToResult.value)
const MATCHING_STAGE_COPY = [
  '正在整理就诊信息',
  '正在筛选可接单陪诊师',
  '正在生成推荐理由'
]
const matchingStageText = computed(() => MATCHING_STAGE_COPY[matchingStageIndex.value] || MATCHING_STAGE_COPY[0])

const getStructuredDemandStorageKey = (id = '') => {
  return id ? `ai_appointment_draft_${id}` : AI_APPOINTMENT_DRAFT_KEY
}

const getQuestionLabel = (field) => {
  if (field === 'serviceDate') return '日期'
  if (field === 'timePeriod') return '时段'
  if (field === 'hospital') return '医院'
  if (field === 'symptomDescription') return '就诊情况'
  return '信息'
}

const shouldShowStructuredAction = computed(() => {
  if (!activeFollowUpType.value) return false
  if (activeFollowUpType.value === 'time_picker') return !!structuredDemand.value.serviceDate
  if (activeFollowUpType.value === 'date_picker') return true
  return false
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

const stopMatchingStageRotation = () => {
  if (matchingStageTimer) {
    clearInterval(matchingStageTimer)
    matchingStageTimer = null
  }
}

const startMatchingStageRotation = () => {
  stopMatchingStageRotation()
  matchingStageIndex.value = 0
  matchingStageTimer = setInterval(() => {
    matchingStageIndex.value = (matchingStageIndex.value + 1) % MATCHING_STAGE_COPY.length
  }, 1800)
}

watch(matchingStageIndex, () => {
  if (!matchingInProgress.value) return
  upsertAssistantMessage({
    message: '正在为您匹配合适的陪诊师',
    processingPhase: 'answering',
    thinkingProcess: matchingStageText.value,
    waitingMatch: true
  })
})

const schedulePoll = (delay = POLL_INTERVAL) => {
  stopPolling()
  pollTimer = setTimeout(() => {
    pollSession()
  }, delay)
}

const showMatchingBubble = () => {
  matchingInProgress.value = true
  readyToMatch.value = false
  showTimePickerSheet.value = false
  currentTimeProposal.value = null
  startMatchingStageRotation()
  upsertAssistantMessage({
    message: '正在为您匹配合适的陪诊师',
    processingPhase: 'answering',
    thinkingProcess: matchingStageText.value,
    waitingMatch: true
  })
  scrollToAnchor('matching-info-card')
}

const navigateToResultPage = async () => {
  if (navigatingToResult.value) return
  navigatingToResult.value = true
  stopPolling()
  stopMatchDeadline()
  stopMatchingStageRotation()
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
    const response = await apiPost(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId.value)}/match`, {
      structuredDemand: structuredDemand.value
    })
    await applySessionState(response?.data || null)
    if (!navigatingToResult.value && matchingInProgress.value) {
      schedulePoll(800)
    }
    return
  } catch (error) {
    stopMatchDeadline()
    stopMatchingStageRotation()
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
}

const confirmAndStartMatch = async () => {
  if (!readyToMatch.value || matchingInProgress.value || navigatingToResult.value) return
  await startMatchFlow()
}

const continueEditing = () => {
  readyToMatch.value = false
  upsertAssistantMessage({
    message: '好的，您可以继续补充或修改需求，确认后我再开始智能匹配。',
    processingPhase: 'completed',
    thinkingProcess: ''
  })
  scrollToBottom()
}

const toggleIntroCards = () => {
  if (!hasStartedConversation.value) return
  introExpanded.value = !introExpanded.value
}

const resetTimePicker = () => {
  pickerStartTime.value = ''
  pickerEndTime.value = ''
}

const cancelTimePicker = () => {
  showTimePickerSheet.value = false
  resetTimePicker()
}

const submitPickedTimeRange = async () => {
  if (!canSubmitPickedTime.value) return
  const exactRange = `${pickerStartTime.value}-${pickerEndTime.value}`
  const displayText = structuredDemand.value.timePeriod
    ? `${structuredDemand.value.timePeriod}，${exactRange}`
    : exactRange
  showTimePickerSheet.value = false
  resetTimePicker()
  await submitStructuredSelection('timeRangeConfirmed', exactRange, displayText)
}

const applySessionStructuredDemand = (state = {}) => {
  const source = state.structuredDemand || state.demandData || state.appointmentData || {}
  const next = sanitizeStructuredDemand({
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
  structuredDemand.value = next
  persistStructuredDemand()
}

const restoreSessionConversation = async (targetSessionId = '', options = {}) => {
  const normalizedSessionId = normalizeString(targetSessionId || readCurrentSessionId())
  if (!normalizedSessionId || restoringSession.value || manualNewSessionStarted.value) return
  restoringSession.value = true
  try {
    const state = await getAiAppointmentSession(normalizedSessionId)
    if (!state) return
    const hasMatchedResult = (Array.isArray(state.matchedList) && state.matchedList.length > 0)
      || normalizeString(state.status) === 'MATCHED'
      || !!normalizeString(state.appointmentNo)
    if (!options.allowMatchedRestore && hasMatchedResult) {
      clearCurrentSessionCache(normalizedSessionId)
      sessionId.value = ''
      showHistoryDivider.value = false
      matchingInProgress.value = false
      readyToMatch.value = false
      activeAssistantKey.value = ''
      structuredDemand.value = createEmptyStructuredDemand()
      resetToWelcomeMessage()
      return
    }
    sessionId.value = normalizedSessionId
    manualNewSessionStarted.value = false
    persistCurrentSessionId(normalizedSessionId)
    showHistoryDivider.value = Array.isArray(state.messages) && state.messages.length > 0
    messages.value = buildMessagesFromHistory(state.messages, showHistoryDivider.value)
    introExpanded.value = false
    await applySessionState(state, { keepHistoryDivider: showHistoryDivider.value })
  } catch (error) {
    console.warn('恢复 AI 预约历史聊天失败:', error)
  } finally {
    restoringSession.value = false
  }
}

const applySessionState = async (state, options = {}) => {
  if (!state) return
  applySessionStructuredDemand(state)
  if (Array.isArray(state.messages) && state.messages.length) {
    const shouldKeepHistoryDivider = options.keepHistoryDivider ?? showHistoryDivider.value
    messages.value = buildMessagesFromHistory(state.messages, shouldKeepHistoryDivider)
  } else if (!messages.value.some((item) => item.type === 'user' || item.type === 'ai')) {
    resetToWelcomeMessage()
  }
  if (hasStartedConversation.value) {
    introExpanded.value = false
  }
  const assistantReply = state.assistantReply || state.message || ''
  if (shouldUpsertAssistantMessage(state)) {
    upsertAssistantMessage({
      message: assistantReply,
      processingPhase: state.processingPhase,
      thinkingProcess: matchingInProgress.value ? matchingStageText.value : state.thinkingProcess,
      waitingMatch: matchingInProgress.value
    })
  }
  currentAssistantIntent.value = state.assistantIntent || ''
  currentQuestionType.value = state.questionKey || state.questionType || ''
  activeFollowUpType.value = state.followUpType || ''
  activeOptions.value = Array.isArray(state.options) ? state.options : []
  followUpRound.value = Number(state.followUpRound || 0)
  currentTimeProposal.value = null
  scrollToBottom()

  if (state.processingPhase === 'completed') {
    sending.value = false
    if (Array.isArray(state.matchedList) && state.matchedList.length) {
      stopMatchDeadline()
      stopMatchingStageRotation()
      await navigateToResultPage()
    } else if ((state.readyForConfirm || (state.canMatch && !state.needMoreInfo)) && !matchingInProgress.value) {
      readyToMatch.value = true
      showTimePickerSheet.value = false
      currentTimeProposal.value = null
      scrollToAnchor('confirm-card')
    } else if (state.needMoreInfo && activeFollowUpType.value === 'time_picker' && !!structuredDemand.value.serviceDate) {
      readyToMatch.value = false
      resetTimePicker()
      showTimePickerSheet.value = false
    } else if (state.needMoreInfo && activeFollowUpType.value === 'date_picker') {
      readyToMatch.value = false
      showTimePickerSheet.value = false
      resetTimePicker()
    } else if (matchingInProgress.value) {
      schedulePoll()
    } else {
      readyToMatch.value = false
      showTimePickerSheet.value = false
    }
    return
  }

  if (state.processingPhase === 'failed') {
    sending.value = false
    matchingInProgress.value = false
    readyToMatch.value = false
    showTimePickerSheet.value = false
    currentTimeProposal.value = null
    stopMatchDeadline()
    stopMatchingStageRotation()
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
        thinkingProcess: matchingStageText.value,
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

  readyToMatch.value = false
  showTimePickerSheet.value = false
  currentTimeProposal.value = null
  resetTimePicker()
  introExpanded.value = false
  appendUserMessage(content)
  appendAssistantPlaceholder()
  userInput.value = ''
  sending.value = true
  currentQuestionType.value = ''
  activeFollowUpType.value = ''
  activeOptions.value = []
  scrollToBottom()

  try {
    let response
    if (!sessionId.value) {
      showHistoryDivider.value = false
      response = await apiPost('/ai/guide/ai-appointment/session', {
        demandText: content,
        structuredDemand: structuredDemand.value
      })
      response = response?.data || null
      sessionId.value = response?.sessionId || ''
      manualNewSessionStarted.value = false
      persistCurrentSessionId(sessionId.value)
      persistStructuredDemand()
    } else {
      response = await apiPost(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId.value)}/reply`, {
        replyText: content,
        structuredDemand: structuredDemand.value
      })
      response = response?.data || null
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
  readyToMatch.value = false
  currentTimeProposal.value = null
  if (fieldKey === 'timePeriod' || fieldKey === 'timeRangeConfirmed') {
    showTimePickerSheet.value = false
    resetTimePicker()
  }
  appendUserMessage(displayText)
  appendAssistantPlaceholder()
  sending.value = true
  currentQuestionType.value = ''
  activeFollowUpType.value = ''
  activeOptions.value = []
  applyStructuredSelectionPatch(fieldKey, selectedValue, displayText)
  scrollToBottom()

  try {
    const response = await apiPost(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId.value)}/reply`, {
      fieldKey,
      selectedValue,
      replyText: displayText,
      structuredDemand: structuredDemand.value
    })
    await applySessionState(response?.data || null)
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
    if (activeFollowUpType.value === 'time_picker' && ['上午', '下午', '晚上', '选择时间', '具体时间'].includes(tag)) {
      if (tag === '选择时间' || tag === '具体时间') {
        openStructuredPicker('timePeriod')
        return
      }
      submitStructuredSelection('timePeriod', tag, tag)
      return
    }
    submitStructuredSelection(currentQuestionType.value, tag, tag)
    return
  }
  userInput.value = tag
}

const openStructuredPicker = (fieldKey) => {
  if (!fieldKey) return
  if (fieldKey === 'timePeriod' || activeFollowUpType.value === 'time_picker') {
    if (!structuredDemand.value.serviceDate) {
      uni.showToast({ title: '请先确认就诊日期', icon: 'none' })
      return
    }
    showTimePickerSheet.value = true
    resetTimePicker()
    return
  }
  if (fieldKey === 'serviceDate' || activeFollowUpType.value === 'date_picker') {
    const itemList = [
      formatDateKey(new Date()),
      formatDateKey(addDays(new Date(), 1)),
      formatDateKey(addDays(new Date(), 2))
    ]
    uni.showActionSheet({
      itemList,
      success: ({ tapIndex }) => {
        const selected = itemList[tapIndex]
        submitStructuredSelection('serviceDate', selected, selected)
      }
    })
    return
  }

  const itemList = activeOptions.value.length ? activeOptions.value : []
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
  restoreStructuredDemand()
  scrollToBottom()
})

onLoad((options) => {
  const routeSessionId = normalizeString(options?.sessionId)
  loadedFromQuerySession.value = !!routeSessionId
  sessionId.value = routeSessionId || readCurrentSessionId()
})

onShow(() => {
  if (manualNewSessionStarted.value) {
    return
  }
  const restoredId = sessionId.value || readCurrentSessionId()
  if (restoredId) {
    restoreSessionConversation(restoredId, { allowMatchedRestore: loadedFromQuerySession.value })
  }
})

onUnmounted(() => {
  stopPolling()
  stopMatchDeadline()
  stopMatchingStageRotation()
})
</script>

<style lang="scss" scoped>
.ai-page {
  height: 100dvh;
  width: 100%;
  max-width: 100%;
  background: linear-gradient(180deg, #edf4ff 0%, #f8fbff 42%, #f6f8fc 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.topbar {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 28rpx 0;
  box-sizing: border-box;
  width: 100%;
}

.topbar-left,
.topbar-right {
  width: 128rpx;
  display: flex;
  align-items: center;
}

.topbar-right {
  justify-content: flex-end;
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

.new-chat-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #2563eb;
}

.intro-stack {
  display: flex;
  flex-direction: column;
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

.hero-card.compact {
  margin-bottom: 14rpx;
  padding: 24rpx;
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

.intro-hint {
  margin: 0 24rpx 18rpx;
  padding: 18rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14rpx 30rpx rgba(48, 79, 143, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.intro-hint-text,
.intro-hint-arrow {
  font-size: 24rpx;
  color: #5672a5;
  font-weight: 600;
}

.summary-card {
  margin: 0 24rpx 18rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 28rpx;
  box-shadow: 0 20rpx 46rpx rgba(48, 79, 143, 0.08);
}

.summary-card.matching {
  background: linear-gradient(180deg, rgba(245, 249, 255, 0.98) 0%, rgba(255, 255, 255, 0.96) 100%);
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
  color: #6a7a94;
}

.summary-badge {
  flex-shrink: 0;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: #edf4ff;
  color: #4b6daa;
  font-size: 22rpx;
  font-weight: 700;
}

.summary-badge.active {
  background: linear-gradient(135deg, #d8e9ff 0%, #edf5ff 100%);
  color: #335aa4;
}

.inline-card {
  margin: 0 0 20rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20rpx 46rpx rgba(48, 79, 143, 0.08);
}

.inline-card.confirm-card {
  border: 2rpx solid rgba(80, 121, 205, 0.12);
}

.inline-card.matching-card {
  background: linear-gradient(180deg, rgba(245, 249, 255, 0.98) 0%, rgba(255, 255, 255, 0.96) 100%);
}

.inline-card.proposal-card {
  border: 2rpx solid rgba(80, 121, 205, 0.12);
}

.inline-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.inline-card-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #23344f;
}

.inline-card-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: #6a7a94;
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

.proposal-time-pill {
  margin-top: 18rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 64rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #edf4ff 0%, #f6f9ff 100%);
  color: #365b9a;
  font-size: 26rpx;
  font-weight: 700;
}

.summary-tag {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #eef4ff;
  color: #49679d;
  font-size: 22rpx;
}

.summary-loading {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.summary-loading-text {
  font-size: 22rpx;
  color: #5e7396;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 24rpx;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.chat-list {
  padding-bottom: 40rpx;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
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

.row-system {
  justify-content: center;
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
  min-width: 0;
  border-radius: 26rpx;
  padding: 20rpx 22rpx;
  box-sizing: border-box;
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

.history-divider {
  max-width: 100%;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: rgba(112, 136, 180, 0.12);
  box-sizing: border-box;
}

.history-divider-text {
  font-size: 24rpx;
  color: #7d8daa;
  line-height: 1.5;
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
  z-index: 20;
  flex-shrink: 0;
  padding: 16rpx 24rpx 12rpx;
  background: linear-gradient(180deg, rgba(246, 248, 252, 0) 0%, rgba(246, 248, 252, 0.92) 18%, rgba(246, 248, 252, 1) 100%);
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.term-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 24rpx;
  padding: 20rpx 22rpx 18rpx;
  box-shadow: 0 18rpx 40rpx rgba(36, 71, 140, 0.08);
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
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

.inline-card-actions {
  margin-top: 18rpx;
  display: flex;
  gap: 16rpx;
}

.sheet-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 24rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.sheet-panel {
  width: 100%;
  max-width: 720rpx;
  max-height: calc(100dvh - 48rpx - env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 34rpx 34rpx 28rpx 28rpx;
  box-shadow: 0 -12rpx 40rpx rgba(15, 23, 42, 0.12), 0 16rpx 48rpx rgba(15, 23, 42, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.time-sheet-panel {
  max-width: 760rpx;
}

.sheet-handle {
  width: 88rpx;
  height: 10rpx;
  border-radius: 999rpx;
  background: rgba(148, 163, 184, 0.35);
  margin: 16rpx auto 0;
  flex-shrink: 0;
}

.sheet-head {
  padding: 28rpx 30rpx 24rpx;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  border-bottom: 1rpx solid #eef3f8;
}

.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #23344f;
}

.sheet-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6a7a94;
}

.sheet-scroll {
  flex: 1;
  min-height: 0;
  padding: 22rpx 24rpx 12rpx;
  box-sizing: border-box;
}

.sheet-actions {
  display: flex;
  gap: 16rpx;
  padding: 22rpx 24rpx calc(28rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eef3f8;
  background: rgba(255, 255, 255, 0.98);
  flex-shrink: 0;
}

.input-bar {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.input-box {
  flex: 1;
  min-width: 0;
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
  flex-shrink: 0;
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

.summary-secondary-btn,
.summary-primary-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-secondary-btn {
  background: #eef4ff;
  color: #4263a1;
  border: none;
}

.summary-primary-btn {
  background: linear-gradient(135deg, #3f84ff 0%, #6cb0ff 100%);
  color: #ffffff;
  border: none;
}

.summary-secondary-btn[disabled],
.summary-primary-btn[disabled] {
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
