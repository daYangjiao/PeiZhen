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

    <view v-if="summaryVisible" class="summary-card" :class="{ matching: matchingInProgress }">
      <view class="summary-head">
        <view class="summary-head-copy">
          <text class="summary-title">已整理的预约信息</text>
          <text class="summary-subtitle">{{ summaryStatusText }}</text>
        </view>
        <view class="summary-badge" :class="{ active: matchingInProgress }">
          <text>{{ matchingInProgress ? '等待 AI 返回' : '已确认' }}</text>
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

      <view v-if="showSummaryAction" class="summary-actions">
        <button class="summary-primary-btn" :disabled="sending || matchingInProgress || navigatingToResult" @click="confirmAndStartMatch">
          确认信息并开始匹配
        </button>
      </view>

      <view v-if="matchingInProgress" class="summary-loading">
        <view class="matching-dots">
          <text class="matching-dot"></text>
          <text class="matching-dot"></text>
          <text class="matching-dot"></text>
        </view>
        <text class="summary-loading-text">正在为您匹配合适的陪诊师</text>
      </view>
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
import { post as apiPost } from '@/utils/api.js'
import {
  getAiAppointmentSession,
} from './api.js'

const AIAvatar = brandAiAvatar
const userStore = useUserStore()
const POLL_INTERVAL = 1500
const MATCH_WAIT_LIMIT = 30000
const AI_APPOINTMENT_DRAFT_KEY = 'ai_appointment_draft_pending'

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
const structuredDemand = ref(createEmptyStructuredDemand())
const showSummaryCard = ref(false)
const readyToMatch = ref(false)
const matchConfirmationOpened = ref(false)
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

function getCurrentUserName() {
  return userStore.displayName || userStore.userInfo?.name || userStore.userInfo?.nickName || userStore.userInfo?.phone || '本人'
}

function createEmptyStructuredDemand() {
  return {
    patientName: getCurrentUserName(),
    patientProfile: '',
    hospital: '',
    department: '',
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
  const department = extractDepartment(rawDemandText)
  const date = extractServiceDate(rawDemandText)
  const { serviceStartTime, serviceEndTime } = extractTimeWindow(rawDemandText)
  const patientProfile = extractPatientProfile(rawDemandText)
  const attendantGender = extractGenderPreference(rawDemandText)
  const symptomClauses = extractMedicalClauses(rawDemandText)
  const symptomTags = uniqueList(
    symptomClauses
      .flatMap((clause) => SYMPTOM_KEYWORDS.filter((keyword) => clause.includes(keyword)))
      .concat(department ? [department] : [])
  )
  const preferenceTags = uniqueList(extractPreferenceTags(rawDemandText))

  let symptomDescription = symptomClauses.join('，')
  if (!symptomDescription && department) {
    symptomDescription = `${department}相关需求`
  }
  if (!symptomDescription && rawDemandText) {
    symptomDescription = rawDemandText
      .replace(hospital, '')
      .replace(date, '')
      .replace(serviceStartTime, '')
      .replace(serviceEndTime, '')
      .trim()
  }

  let otherRequirement = preferenceTags.join('、')
  if (!otherRequirement && attendantGender) {
    otherRequirement = `${attendantGender}陪诊师`
  }

  return sanitizeStructuredDemand({
    patientName: getCurrentUserName(),
    patientProfile,
    hospital,
    department,
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
    department: normalizeString(payload.department),
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

  if (fieldKey === 'timePeriod') {
    const exact = normalizeExactTimePeriod(normalizedValue)
    if (!exact) return false
    const [startTime, endTime] = exact.split('-')
    patch.serviceStartTime = startTime
    patch.serviceEndTime = endTime
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
  if (structuredDemand.value.department) tags.push(structuredDemand.value.department)
  tags.push(...uniqueList(structuredDemand.value.symptomTags))
  tags.push(...uniqueList(structuredDemand.value.preferenceTags))
  return uniqueList(tags)
})

const summaryVisible = computed(() => showSummaryCard.value || matchingInProgress.value)

const showSummaryAction = computed(() => showSummaryCard.value && readyToMatch.value && !matchingInProgress.value)

const summaryStatusText = computed(() => {
  if (matchingInProgress.value) return '正在梳理医院、时间、症状与陪护偏好'
  if (readyToMatch.value) return '信息已整理完成，请确认后开始智能匹配'
  if (currentQuestionType.value) return `正在补齐${getQuestionLabel(currentQuestionType.value)}`
  return 'AI 已帮您整理出可用于匹配的关键信息'
})

const getStructuredDemandStorageKey = (id = '') => {
  return id ? `ai_appointment_draft_${id}` : AI_APPOINTMENT_DRAFT_KEY
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
  readyToMatch.value = false
  showSummaryCard.value = true
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

const buildSummaryConfirmText = () => {
  return summaryItems.value
    .map((item) => `${item.label}：${item.value || item.placeholder}`)
    .join('\n')
}

const promptMatchConfirmation = () => {
  if (matchConfirmationOpened.value || matchingInProgress.value || navigatingToResult.value) return
  matchConfirmationOpened.value = true
  showSummaryCard.value = true
  uni.showModal({
    title: '确认预约信息',
    content: `${buildSummaryConfirmText()}\n\n确认无误后开始 AI 智能匹配陪诊师。`,
    confirmText: '开始匹配',
    cancelText: '继续修改',
    success: async ({ confirm }) => {
      matchConfirmationOpened.value = false
      if (confirm) {
        await confirmAndStartMatch()
        return
      }
      upsertAssistantMessage({
        message: '好的，您可以继续补充或修改需求，确认后再开始智能匹配。',
        processingPhase: 'completed',
        thinkingProcess: ''
      })
      scrollToBottom()
    },
    fail: () => {
      matchConfirmationOpened.value = false
    }
  })
}

const confirmAndStartMatch = async () => {
  if (!readyToMatch.value || matchingInProgress.value || navigatingToResult.value) return
  await startMatchFlow()
}

const applySessionStructuredDemand = (state = {}) => {
  const source = state.structuredDemand || state.demandData || state.appointmentData || {}
  const next = sanitizeStructuredDemand({
    ...structuredDemand.value,
    ...source,
    patientName: state.patientName || source.patientName || structuredDemand.value.patientName || getCurrentUserName(),
    patientProfile: state.patientProfile || source.patientProfile || structuredDemand.value.patientProfile,
    hospital: state.hospital || source.hospital || structuredDemand.value.hospital,
    department: state.department || source.department || structuredDemand.value.department,
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

const applySessionState = async (state) => {
  if (!state) return
  applySessionStructuredDemand(state)
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
      readyToMatch.value = true
      showSummaryCard.value = true
      if (!matchConfirmationOpened.value) {
        upsertAssistantMessage({
          message: '我已经帮您整理好预约信息。您确认后，我就开始智能匹配陪诊师。',
          processingPhase: 'completed',
          thinkingProcess: ''
        })
        scrollToBottom()
        promptMatchConfirmation()
      }
    } else if (state.needMoreInfo && followUpRound.value >= 2) {
      readyToMatch.value = false
      showSummaryCard.value = false
      openStructuredPicker(currentQuestionType.value)
    } else if (matchingInProgress.value) {
      schedulePoll()
    } else {
      readyToMatch.value = false
      showSummaryCard.value = false
    }
    return
  }

  if (state.processingPhase === 'failed') {
    sending.value = false
    matchingInProgress.value = false
    readyToMatch.value = false
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

  showSummaryCard.value = false
  readyToMatch.value = false
  matchConfirmationOpened.value = false
  refreshStructuredDemandFromText(content)
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
      response = await apiPost('/ai/guide/ai-appointment/session', {
        demandText: content,
        structuredDemand: structuredDemand.value
      })
      response = response?.data || null
      sessionId.value = response?.sessionId || ''
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
  showSummaryCard.value = false
  readyToMatch.value = false
  matchConfirmationOpened.value = false
  appendUserMessage(displayText)
  appendAssistantPlaceholder()
  sending.value = true
  currentQuestionType.value = ''
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
    if (currentQuestionType.value === 'timePeriod' && ['上午', '下午', '晚上', '具体时间'].includes(tag)) {
      const periodHint = tag === '具体时间' ? '' : tag
      const modalTitle = periodHint ? `补充具体时间段（${periodHint}）` : '请输入具体时间段'
      uni.showModal({
        title: modalTitle,
        editable: true,
        placeholderText: '例如 9:00-11:00、9.00-11.00、9点到11点',
        confirmText: '确认',
        success: ({ confirm, content }) => {
          const exactTime = normalizeExactTimePeriod(content || '')
          if (!confirm) return
          if (!exactTime) {
            uni.showToast({ title: '请输入完整时间段，例如 9:00-11:00', icon: 'none' })
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
  restoreStructuredDemand()
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
