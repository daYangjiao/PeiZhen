<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/ren_1.png" mode="aspectFit"></image>
        <view>
          <text class="hero-title">资质管理</text>
          <text class="hero-desc">证件分步上传，审核状态实时同步</text>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-1">
      <view class="status-head">
        <text class="section-title">资质状态</text>
        <text class="status-text" :class="statusClass">{{ statusText }}</text>
      </view>
      <view class="progress-line">
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: `${qualificationCompleteness}%` }"></view>
        </view>
        <text class="progress-text">{{ qualificationCompleteness }}%</text>
      </view>
      <text v-if="blockReason" class="fail-reason">{{ blockReason }}</text>
      <text v-if="showFailReason" class="fail-reason">失败原因：{{ failReasonText }}</text>
      <text v-else-if="isBlocked" class="blocked-tip">账号已封禁，请联系平台客服处理。</text>
    </view>

    <view class="card slide-up delay-2">
      <view class="row" v-for="item in qualificationItems" :key="item.key">
        <view class="left">
          <text class="name">{{ item.label }}</text>
          <text class="desc">{{ item.desc }}</text>
        </view>

        <view class="middle">
          <view class="dot" :class="item.uploaded ? 'dot-pass' : 'dot-warn'"></view>
          <text :class="item.uploaded ? 'text-pass' : 'text-warn'">
            {{ item.uploaded ? '已上传' : '未上传' }}
          </text>
          <text v-if="item.expireDate" class="expire-text" :class="item.expired ? 'expired' : ''">
            {{ item.expireDate }}{{ item.expired ? ' 已过期' : '' }}
          </text>
        </view>

        <view class="ghost-btn" @click="goUpload(item.key)">
          <text>{{ item.uploaded ? '管理' : '上传' }}</text>
        </view>
      </view>
    </view>

    <view v-if="auditLogs.length" class="card slide-up delay-3">
      <text class="section-title">审核记录</text>
      <view class="log-row" v-for="(log, index) in auditLogs" :key="`${log.action}-${index}`">
        <text class="log-action">{{ mapLogAction(log.action) }}</text>
        <text class="log-time">{{ formatLogTime(log.createTime) }}</text>
        <text v-if="log.reason" class="log-reason">{{ log.reason }}</text>
      </view>
    </view>

    <view class="submit-btn slide-up delay-3" :class="{ disabled: submitDisabled }" @click="submitForReview">
      <text>{{ submitting ? '提交中...' : '提交审核' }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { post } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { getQualificationImageFields } from '@/utils/qualification.mjs'

const userStore = useUserStore()
const { attendantInfo } = storeToRefs(userStore)

const submitting = ref(false)

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const statusCode = computed(() => Number(attendantInfo.value.qualificationStatusCode || 0))

const statusText = computed(() => {
  if (statusCode.value === 1) return '已审核'
  if (statusCode.value === 3) return '审核失败'
  if (statusCode.value === 2) return '封禁'
  return '待审核'
})

const statusClass = computed(() => {
  if (statusCode.value === 1) return 'status-pass'
  if (statusCode.value === 3) return 'status-fail'
  if (statusCode.value === 2) return 'status-blocked'
  return 'status-pending'
})

const showFailReason = computed(() => statusCode.value === 3)
const isBlocked = computed(() => statusCode.value === 2)
const failReasonText = computed(() => attendantInfo.value.qualificationFailReason || '资质资料不完整')
const blockReason = computed(() => attendantInfo.value.qualificationBlockReason || '')
const qualificationCompleteness = computed(() => Math.max(0, Math.min(100, Number(attendantInfo.value.qualificationCompleteness || 0))))
const auditLogs = computed(() => attendantInfo.value.recentQualificationLogs || [])

const qualificationImages = computed(() => getQualificationImageFields(attendantInfo.value))
const idCardFront = computed(() => qualificationImages.value.idCardFront)
const idCardBack = computed(() => qualificationImages.value.idCardBack)
const idCardReady = computed(() => !!idCardFront.value && !!idCardBack.value)
const practiceReady = computed(() => !!qualificationImages.value.practiceCert)
const healthReady = computed(() => !!qualificationImages.value.healthCert)
const practiceExpireReady = computed(() => !!attendantInfo.value.practiceCertExpireDate && !attendantInfo.value.practiceCertExpired)
const healthExpireReady = computed(() => !!attendantInfo.value.healthCertExpireDate && !attendantInfo.value.healthCertExpired)

const qualificationItems = computed(() => [
  {
    key: 'idCard',
    label: '身份证',
    desc: '需上传正反面',
    uploaded: idCardReady.value
  },
  {
    key: 'practiceCert',
    label: '执业证书',
    desc: '证书清晰可见，需填写有效期',
    uploaded: practiceReady.value && !!attendantInfo.value.practiceCertExpireDate,
    expireDate: attendantInfo.value.practiceCertExpireDate || '',
    expired: attendantInfo.value.practiceCertExpired
  },
  {
    key: 'healthCert',
    label: '健康证',
    desc: '需在有效期内',
    uploaded: healthReady.value && !!attendantInfo.value.healthCertExpireDate,
    expireDate: attendantInfo.value.healthCertExpireDate || '',
    expired: attendantInfo.value.healthCertExpired
  }
])

const submitDisabled = computed(() => submitting.value || !(idCardReady.value && practiceReady.value && healthReady.value && practiceExpireReady.value && healthExpireReady.value))

const loadProfile = async () => {
  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  await userStore.fetchAttendantProfile(uid)
}

const goUpload = (type) => {
  uni.navigateTo({
    url: `/subpkg/profile/qualification-upload?type=${type}`
  })
}

const mapLogAction = (action = '') => {
  const map = {
    UPLOAD: '更新资料',
    SUBMIT: '提交审核',
    APPROVE: '审核通过',
    REJECT: '审核驳回',
    BAN: '账号封禁',
    RESTORE: '恢复通过'
  }
  return map[String(action).toUpperCase()] || action || '审核记录'
}

const formatLogTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const submitForReview = async () => {
  if (submitDisabled.value) return

  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const res = await post(`/attendant/qualification/${uid}/submit`)
    if (res.code === 200) {
      await userStore.fetchAttendantProfile(uid)
      uni.showToast({ title: '提交成功', icon: 'success' })
    }
  } catch (error) {
    const message = error && error.message ? error.message : '提交失败'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onMounted(loadProfile)
onShow(loadProfile)
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding-bottom: 24rpx;
}

.hero-card {
  @include escort-card(26rpx);
  margin: 24rpx 24rpx 0;
  border: 1rpx solid #e5eefb;
  background: linear-gradient(135deg, #ffffff 0%, #f3f8ff 100%);
}

.hero-left {
  display: flex;
  align-items: center;
}

.hero-icon {
  width: 56rpx;
  height: 56rpx;
  margin-right: 14rpx;
}

.hero-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.card {
  @include escort-card(32rpx);
  margin: 24rpx;
}

.status-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 700;
}

.status-text {
  font-size: 28rpx;
  font-weight: 700;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 22rpx;
}

.progress-track {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  background: #eef3f8;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $escort-color-primary 0%, #43c3a4 100%);
}

.progress-text {
  font-size: 24rpx;
  font-weight: 700;
  color: $escort-color-primary;
}

.status-pass {
  color: #52c41a;
}

.status-pending {
  color: #faad14;
}

.status-fail,
.status-blocked {
  color: #ff4d4f;
}

.fail-reason,
.blocked-tip {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  color: #ff4d4f;
}

.row {
  min-height: 112rpx;
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid #eef2f7;
}

.row:last-child {
  border-bottom: none;
}

.left {
  width: 240rpx;
}

.name {
  display: block;
  font-size: 28rpx;
  color: #1f2937;
  font-weight: 600;
}

.desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #9ca3af;
}

.middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6rpx;
}

.middle .dot {
  margin-bottom: 2rpx;
}

.expire-text {
  font-size: 22rpx;
  color: #667085;
}

.expire-text.expired {
  color: #ff4d4f;
  font-weight: 700;
}

.log-row {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.log-row:last-child {
  border-bottom: none;
}

.log-action {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  color: #1f2937;
}

.log-time,
.log-reason {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #667085;
}

.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-right: 8rpx;
}

.dot-pass {
  background: #52c41a;
}

.dot-warn {
  background: #ff4d4f;
}

.text-pass {
  color: #52c41a;
  font-size: 25rpx;
}

.text-warn {
  color: #ff4d4f;
  font-size: 25rpx;
}

.ghost-btn {
  min-width: 108rpx;
  height: 54rpx;
  border-radius: 30rpx;
  border: 1rpx solid $escort-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: $escort-color-primary;
    font-size: 24rpx;
  }
}

.submit-btn {
  height: 88rpx;
  border-radius: 60rpx;
  background: $escort-color-primary;
  margin: 28rpx 24rpx 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;

  text {
    color: #ffffff;
    font-size: 30rpx;
    font-weight: 700;
  }
}

.submit-btn.disabled {
  background: #c0c4cc;
  box-shadow: none;
}

.slide-up {
  opacity: 0;
  transform: translateY(20rpx);
  animation: slideUp 0.42s ease forwards;
}

.delay-1 {
  animation-delay: 0.02s;
}

.delay-2 {
  animation-delay: 0.08s;
}

.delay-3 {
  animation-delay: 0.14s;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
