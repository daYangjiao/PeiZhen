<template>
  <view class="page">
    <view v-if="loading" class="state-card">
      <text class="state-title">正在加载陪诊师资料...</text>
      <text class="state-desc">请稍候</text>
    </view>

    <view v-else-if="loadError" class="state-card">
      <text class="state-title">{{ loadError }}</text>
      <text class="state-link" @click="loadAttendantDetail">重新加载</text>
    </view>

    <template v-else>
      <view class="hero-card slide-up">
        <view class="avatar-shell">
          <image class="avatar" :src="avatarUrl" mode="aspectFill"></image>
        </view>
        <text class="hero-name">{{ profile.name || '陪诊师' }}</text>
        <view class="rating-row">
          <view class="stars">
            <text v-for="n in 5" :key="n" class="star" :class="{ active: n <= roundedScore }">★</text>
          </view>
          <text class="rating-score">{{ displayScore }}</text>
          <text class="rating-count" v-if="profile.evaluationCount">（{{ profile.evaluationCount }}条评价）</text>
        </view>
        <view class="tag-row" v-if="specialtyTags.length">
          <text class="tag-chip" v-for="tag in specialtyTags" :key="tag">{{ tag }}</text>
        </view>
        <view class="hero-action" @click="openChat">
          <text>在线咨询</text>
        </view>
      </view>

      <view class="stats-card slide-up delay-1">
        <view class="stat-item">
          <text class="stat-value">{{ profile.completedOrders || 0 }}</text>
          <text class="stat-label">已完成订单</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ profile.praiseRate || 0 }}%</text>
          <text class="stat-label">好评率</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ profile.experienceYears || 0 }}年</text>
          <text class="stat-label">从业经验</text>
        </view>
      </view>

      <view class="section-card slide-up delay-2">
        <view class="section-head">
          <text class="section-title">个人简介</text>
        </view>
        <text class="bio-text">{{ profile.introduction || defaultIntroduction }}</text>
        <view class="meta-grid">
          <view class="meta-item">
            <text class="meta-label">常驻医院</text>
            <text class="meta-value">{{ profile.hospitalName || '资料完善中' }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">擅长方向</text>
            <text class="meta-value">{{ profile.professionalField || '综合陪诊服务' }}</text>
          </view>
          <view class="meta-item" v-if="profile.certificate">
            <text class="meta-label">执业证书</text>
            <text class="meta-value">{{ profile.certificate }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">资质状态</text>
            <text class="meta-value">{{ profile.qualificationStatusText || '待审核' }}</text>
          </view>
        </view>
      </view>

      <view class="section-card slide-up delay-2">
        <view class="section-head">
          <text class="section-title">资质一览</text>
          <text class="section-link" @click="previewQualificationImages">查看完整资质</text>
        </view>
        <view class="qualification-list">
          <view class="qualification-item" v-for="item in qualificationItems" :key="item.label">
            <view class="qualification-copy">
              <text class="qualification-label">{{ item.label }}</text>
              <text class="qualification-tip">{{ item.tip }}</text>
            </view>
            <text class="qualification-badge" :class="{ ready: item.ready }">{{ item.ready ? '已上传' : '未上传' }}</text>
          </view>
        </view>
      </view>

      <view class="section-card slide-up delay-3">
        <view class="section-head">
          <text class="section-title">用户评价</text>
          <text class="section-subtitle" v-if="profile.evaluationCount">最近 {{ reviewList.length }} 条</text>
        </view>
        <view v-if="reviewList.length === 0" class="empty-box">
          <text>暂未展示评价内容</text>
        </view>
        <scroll-view v-else class="review-scroll" scroll-x show-scrollbar>
          <view class="review-list">
            <view class="review-card" v-for="item in reviewList" :key="item.orderId">
              <view class="review-head">
                <view class="review-user">
                  <view class="review-avatar">{{ getReviewerInitial(item.reviewerName) }}</view>
                  <view class="review-meta">
                    <text class="review-name">{{ item.reviewerName || '用户**' }}</text>
                    <text class="review-date">{{ item.serviceDate || formatReviewDate(item.createTime) }}</text>
                  </view>
                </view>
                <view class="review-rating">
                  <text v-for="n in 5" :key="n" class="review-star" :class="{ active: n <= Number(item.rating || 0) }">★</text>
                </view>
              </view>
              <view class="review-tags" v-if="normalizeTags(item.tags).length">
                <text class="review-tag" v-for="tag in normalizeTags(item.tags)" :key="tag">{{ tag }}</text>
              </view>
              <text class="review-content">{{ item.content || '用户给出了好评。' }}</text>
              <view v-if="item.attendantReply" class="reply-box">
                <text class="reply-label">陪诊师回复</text>
                <text class="reply-text">{{ item.attendantReply }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="section-card slide-up delay-3">
        <view class="section-head">
          <text class="section-title">服务范围</text>
        </view>
        <view class="scope-grid">
          <view class="scope-item">
            <text class="scope-key">累计接单</text>
            <text class="scope-value">{{ profile.totalOrders || 0 }} 单</text>
          </view>
          <view class="scope-item">
            <text class="scope-key">本月服务</text>
            <text class="scope-value">{{ profile.monthService || 0 }} 次</text>
          </view>
          <view class="scope-item">
            <text class="scope-key">今日服务</text>
            <text class="scope-value">{{ profile.todayService || 0 }} 次</text>
          </view>
          <view class="scope-item">
            <text class="scope-key">常驻院区</text>
            <text class="scope-value">{{ profile.hospitalName || '待补充' }}</text>
          </view>
        </view>
      </view>
    </template>

    <view v-if="!loading && !loadError" class="bottom-bar">
      <button class="ghost-btn" @click="openChat">联系陪诊师</button>
      <button class="primary-btn" @click="bookService">预约服务</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAttendantById, getAttendantPublicReviews } from '@/api/attendant.js'
import { defaultAvatar } from '@/utils/assets.js'
import { resolveAvatarUrl, resolveImageUrl } from '@/utils/media.js'

const loading = ref(true)
const loadError = ref('')
const attendantId = ref('')
const profile = ref({})
const reviewList = ref([])

const defaultIntroduction = '该陪诊师已完成实名认证与资料补充，可提供院内陪诊、就诊流程协助与基础沟通支持。'

const avatarUrl = computed(() => resolveAvatarUrl(profile.value.avatarUrl || '', defaultAvatar))
const roundedScore = computed(() => Math.max(0, Math.min(5, Math.round(Number(profile.value.score || 0)))))
const displayScore = computed(() => Number(profile.value.score || 0).toFixed(1))

const specialtyTags = computed(() => {
  const raw = String(profile.value.professionalField || '')
  return raw
    .split(/[，,、/｜|\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 4)
})

const qualificationItems = computed(() => ([
  {
    label: '身份证明',
    ready: !!profile.value.idCardUploaded,
    tip: '实名认证与身份核验资料'
  },
  {
    label: '执业证书',
    ready: !!profile.value.practiceCertUploaded,
    tip: '专业资质与从业能力证明'
  },
  {
    label: '健康证明',
    ready: !!profile.value.healthCertUploaded,
    tip: '服务期间健康状态说明'
  }
]))

const normalizeTags = (raw) => String(raw || '')
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const formatReviewDate = (value) => {
  if (!value) return '最近评价'
  const date = new Date(String(value).replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) return '最近评价'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const getReviewerInitial = (name = '') => {
  const raw = String(name || '').trim()
  return raw ? raw.slice(0, 1) : '匿'
}

const normalizeProfile = (payload = {}) => ({
  ...payload,
  score: Number(payload.score || 0),
  experienceYears: Number(payload.experienceYears || 0),
  todayService: Number(payload.todayService || 0),
  monthService: Number(payload.monthService || 0),
  praiseRate: Number(payload.praiseRate || 0),
  totalOrders: Number(payload.totalOrders || 0),
  completedOrders: Number(payload.completedOrders || 0),
  evaluationCount: Number(payload.evaluationCount || 0)
})

const ensureLoggedIn = () => {
  if (uni.getStorageSync('token')) return true
  uni.navigateTo({ url: '/pages/auth/login?role=user' })
  return false
}

const openChat = () => {
  if (!ensureLoggedIn()) return
  if (!attendantId.value) {
    uni.showToast({ title: '陪诊师资料暂不可用', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/subpkg/chat/chat?userId=${encodeURIComponent(attendantId.value)}&name=${encodeURIComponent(profile.value.name || '陪诊师')}&avatar=${encodeURIComponent(profile.value.avatarUrl || '')}`
  })
}

const bookService = () => {
  uni.switchTab({ url: '/pages/ai-triage/01-appointment-selection' })
}

const previewQualificationImages = () => {
  const images = [
    profile.value.idCardFrontFileUrl,
    profile.value.idCardBackFileUrl,
    profile.value.practiceCertFileUrl,
    profile.value.healthCertFileUrl
  ]
    .map((item) => resolveImageUrl(item, ''))
    .filter(Boolean)

  if (!images.length) {
    uni.showToast({ title: '暂未上传完整资质资料', icon: 'none' })
    return
  }

  uni.previewImage({
    urls: images,
    current: images[0]
  })
}

const loadAttendantDetail = async () => {
  if (!attendantId.value) {
    loadError.value = '未找到陪诊师信息'
    loading.value = false
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const [profileRes, reviewRes] = await Promise.all([
      getAttendantById(attendantId.value),
      getAttendantPublicReviews(attendantId.value, 6)
    ])

    if (profileRes.code !== 200 || !profileRes.data) {
      throw new Error(profileRes.message || '陪诊师资料加载失败')
    }

    profile.value = normalizeProfile(profileRes.data)
    reviewList.value = reviewRes.code === 200 && Array.isArray(reviewRes.data) ? reviewRes.data : []
  } catch (error) {
    loadError.value = error?.message || '陪诊师资料加载失败'
  } finally {
    loading.value = false
  }
}

onLoad((options = {}) => {
  attendantId.value = options.id ? decodeURIComponent(options.id) : ''
  loadAttendantDetail()
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';

.page {
  @include user-page;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(180rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.state-card {
  @include user-card(36rpx);
  margin-top: 24rpx;
  text-align: center;
}

.state-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: $user-color-text-main;
}

.state-desc,
.state-link {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: $user-color-text-sub;
}

.state-link {
  color: $user-color-primary;
}

.hero-card {
  @include user-card(34rpx 30rpx);
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
}

.avatar-shell {
  width: 172rpx;
  height: 172rpx;
  padding: 8rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.18), rgba(37, 99, 235, 0.06));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.avatar {
  width: 156rpx;
  height: 156rpx;
  border-radius: 50%;
  background: #eef4fc;
}

.hero-name {
  font-size: 46rpx;
  font-weight: 700;
  color: $user-color-text-main;
}

.rating-row {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 14rpx;
}

.stars {
  display: flex;
  gap: 4rpx;
}

.star,
.review-star {
  color: #d7e0ec;
  font-size: 28rpx;
}

.star.active,
.review-star.active {
  color: #ffb200;
}

.rating-score,
.rating-count,
.section-subtitle {
  font-size: 24rpx;
  color: $user-color-text-sub;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12rpx;
  margin-top: 24rpx;
}

.tag-chip,
.review-tag {
  @include user-tag(false);
  background: #eef5ff;
  color: $user-color-primary;
  padding: 10rpx 22rpx;
}

.hero-action {
  margin-top: 28rpx;
  min-width: 188rpx;
  height: 92rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, $user-color-primary, $user-color-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  box-shadow: $user-shadow-primary;
}

.stats-card,
.section-card {
  @include user-card(28rpx);
  margin-top: 20rpx;
}

.stats-card {
  display: flex;
  gap: 12rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: $user-color-text-main;
}

.stat-label {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $user-color-text-sub;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $user-color-text-main;
}

.section-link {
  font-size: 24rpx;
  color: $user-color-primary;
}

.bio-text {
  font-size: 28rpx;
  line-height: 1.8;
  color: $user-color-text-sub;
}

.meta-grid,
.scope-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 22rpx;
}

.meta-item,
.scope-item {
  background: #f7faff;
  border-radius: 20rpx;
  padding: 20rpx;
  border: 1rpx solid rgba(0, 122, 255, 0.08);
}

.meta-label,
.scope-key,
.qualification-tip,
.reply-label {
  display: block;
  font-size: 22rpx;
  color: #7b8ea3;
}

.meta-value,
.scope-value {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  line-height: 1.5;
  color: $user-color-text-main;
  font-weight: 600;
}

.qualification-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.qualification-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid rgba(22, 50, 79, 0.06);
}

.qualification-item:last-child {
  border-bottom: none;
}

.qualification-label {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $user-color-text-main;
}

.qualification-badge {
  min-width: 116rpx;
  padding: 10rpx 0;
  border-radius: 999rpx;
  text-align: center;
  font-size: 22rpx;
  color: #95a3b8;
  background: #eef2f7;
}

.qualification-badge.ready {
  color: $user-color-primary;
  background: rgba(0, 122, 255, 0.12);
}

.empty-box {
  padding: 26rpx;
  border-radius: 20rpx;
  background: #f7faff;
  text-align: center;
  font-size: 24rpx;
  color: $user-color-text-sub;
}

.review-scroll {
  white-space: nowrap;
}

.review-list {
  display: flex;
  gap: 18rpx;
}

.review-card {
  width: 560rpx;
  background: #f8fbff;
  border-radius: 24rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(0, 122, 255, 0.08);
  box-sizing: border-box;
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.review-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #d8e6fb, #edf4ff);
  color: $user-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.review-meta {
  display: flex;
  flex-direction: column;
}

.review-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $user-color-text-main;
}

.review-date {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8ea0b2;
}

.review-rating {
  display: flex;
  gap: 2rpx;
}

.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 18rpx;
}

.review-content {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: $user-color-text-main;
  white-space: normal;
}

.reply-box {
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 18rpx;
  background: #ffffff;
}

.reply-text {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
}

.bottom-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 18rpx;
  padding: 20rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(18rpx);
  box-shadow: 0 18rpx 38rpx rgba(18, 56, 109, 0.12);
}

.ghost-btn,
.primary-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.ghost-btn {
  @include user-ghost-btn(92rpx);
}

.primary-btn {
  @include user-primary-btn;
}
</style>
