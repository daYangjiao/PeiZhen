<template>
  <view class="page">
    <view class="status-card slide-up delay-1" :class="overallState">
      <view class="status-top">
        <view class="status-mark">{{ statusMark }}</view>
        <view class="status-copy">
          <text class="status-title">{{ statusTitle }}</text>
          <text class="status-desc">{{ statusDesc }}</text>
        </view>
      </view>
      <view class="progress-card">
        <view class="progress-meta">
          <text>材料完整度</text>
          <text>{{ qualificationCompleteness }}%</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: `${qualificationCompleteness}%` }"></view>
        </view>
      </view>
      <view v-if="showFailReason || displayBlockReason" class="reason-card">
        <text class="reason-title">{{ showFailReason ? '驳回原因' : '当前阻断' }}</text>
        <text class="reason-text">{{ showFailReason ? failReasonText : displayBlockReason }}</text>
      </view>
    </view>

    <view class="section-card slide-up delay-2">
      <view class="section-head">
        <text class="section-title">资质材料</text>
        <text class="section-note">{{ materialReadyCount }}/4 已完成</text>
      </view>
      <view class="material-list">
        <view class="material-card" v-for="item in materialItems" :key="item.key" @click="goUpload(item.key)">
          <view class="material-preview" :class="{ empty: !item.preview }">
            <image v-if="item.preview" :src="toFullUrl(item.preview)" mode="aspectFill"></image>
            <text v-else>{{ item.short }}</text>
          </view>
          <view class="material-copy">
            <view class="material-title-row">
              <text class="material-title">{{ item.label }}</text>
              <text class="material-badge" :class="item.ready ? 'ready' : 'missing'">{{ item.ready ? '已上传' : '待补充' }}</text>
            </view>
            <text class="material-desc">{{ item.desc }}</text>
            <text v-if="item.expireDate" class="expire-text" :class="{ expired: item.expired }">
              有效期 {{ item.expireDate }}{{ item.expired ? ' 已过期' : '' }}
            </text>
            <text v-else-if="item.needExpire" class="expire-text expired">待填写有效期</text>
          </view>
          <text class="material-action">{{ item.ready ? '管理' : '上传' }}</text>
        </view>
      </view>
    </view>

    <view class="section-card slide-up delay-3">
      <view class="section-head">
        <text class="section-title">审核记录</text>
        <text class="section-note">{{ auditLogs.length ? '最近记录' : '暂无记录' }}</text>
      </view>
      <view v-if="auditLogs.length" class="log-list">
        <view class="log-row" v-for="(log, index) in auditLogs" :key="`${log.action}-${index}`">
          <view class="log-dot"></view>
          <view class="log-copy">
            <text class="log-action">{{ mapLogAction(log.action) }}</text>
            <text class="log-time">{{ formatLogTime(log.createTime) }}</text>
            <text v-if="log.reason" class="log-reason">{{ log.reason }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-log">材料提交后，这里会显示平台审核进度。</view>
    </view>

    <view v-if="showSubmitButton" class="submit-btn slide-up delay-3" :class="{ disabled: submitDisabled }" @click="submitForReview">
      <text>{{ submitting ? '提交中...' : '提交审核' }}</text>
    </view>
    <view v-if="showPassedFooter" class="passed-footer slide-up delay-3">资质已通过，证件到期前请及时更新。</view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { post } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { getQualificationImageFields } from '@/utils/qualification.mjs'
import { resolveImageUrl } from '@/utils/media.js'

const userStore = useUserStore()
const { attendantInfo } = storeToRefs(userStore)

const submitting = ref(false)

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const statusCode = computed(() => Number(attendantInfo.value.qualificationStatusCode || 0))

const showFailReason = computed(() => statusCode.value === 2)
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
const allMaterialsReady = computed(() => idCardReady.value && practiceReady.value && healthReady.value && practiceExpireReady.value && healthExpireReady.value)

const overallState = computed(() => {
  if (attendantInfo.value.practiceCertExpired || attendantInfo.value.healthCertExpired) return 'expired'
  if (statusCode.value === 1 && allMaterialsReady.value) return 'passed'
  if (statusCode.value === 2) return 'rejected'
  if (!allMaterialsReady.value) return 'incomplete'
  return 'pending'
})

const statusMark = computed(() => {
  const map = { passed: '通', rejected: '驳', expired: '期', incomplete: '补', pending: '审' }
  return map[overallState.value] || '审'
})

const statusTitle = computed(() => {
  const map = {
    passed: '资质已通过',
    rejected: '资质审核未通过',
    expired: '证件已过期',
    incomplete: '资质待补充',
    pending: '资质审核中'
  }
  return map[overallState.value] || '资质审核中'
})

const statusDesc = computed(() => {
  const map = {
    passed: '你可以正常进入接单大厅并接单。',
    rejected: '请根据驳回原因修改材料，重新提交平台审核。',
    expired: '请更新过期证件和有效期后重新提交审核。',
    incomplete: '补全身份证正反面、执业证书、健康证和有效期后提交审核。',
    pending: '平台正在审核你的入驻资料，审核通过后即可接单。'
  }
  return map[overallState.value] || '平台正在审核你的入驻资料。'
})

const displayBlockReason = computed(() => {
  if (!blockReason.value || showFailReason.value) return ''
  if (overallState.value === 'incomplete' || overallState.value === 'pending') return ''
  return blockReason.value
})

const materialItems = computed(() => [
  {
    key: 'idCardFront',
    label: '身份证正面',
    short: '正',
    desc: '姓名、证件号需清晰无遮挡',
    ready: !!idCardFront.value,
    preview: idCardFront.value
  },
  {
    key: 'idCardBack',
    label: '身份证反面',
    short: '反',
    desc: '有效期和签发机关需清晰',
    ready: !!idCardBack.value,
    preview: idCardBack.value
  },
  {
    key: 'practiceCert',
    label: '执业证书',
    short: '执',
    desc: '证书清晰可见，需填写有效期',
    ready: practiceReady.value && !!attendantInfo.value.practiceCertExpireDate && !attendantInfo.value.practiceCertExpired,
    preview: qualificationImages.value.practiceCert,
    needExpire: true,
    expireDate: attendantInfo.value.practiceCertExpireDate || '',
    expired: attendantInfo.value.practiceCertExpired
  },
  {
    key: 'healthCert',
    label: '健康证',
    short: '康',
    desc: '需在有效期内',
    ready: healthReady.value && !!attendantInfo.value.healthCertExpireDate && !attendantInfo.value.healthCertExpired,
    preview: qualificationImages.value.healthCert,
    needExpire: true,
    expireDate: attendantInfo.value.healthCertExpireDate || '',
    expired: attendantInfo.value.healthCertExpired
  }
])

const materialReadyCount = computed(() => materialItems.value.filter((item) => item.ready).length)
const showSubmitButton = computed(() => overallState.value !== 'passed')
const showPassedFooter = computed(() => overallState.value === 'passed')
const submitDisabled = computed(() => submitting.value || !allMaterialsReady.value)

const toFullUrl = (url) => resolveImageUrl(url, '')

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
    BAN: '账号状态变更',
    RESTORE: '账号状态恢复'
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
  padding: 24rpx 24rpx calc(36rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.status-card,
.section-card {
  @include escort-card(30rpx);
  border: 1rpx solid #e4eefb;
}

.section-card {
  margin-top: 24rpx;
}

.status-card {
  background: linear-gradient(135deg, #ffffff 0%, #f5f9ff 100%);
}

.status-top {
  display: flex;
  align-items: flex-start;
  gap: 22rpx;
}

.status-mark {
  width: 84rpx;
  height: 84rpx;
  border-radius: 30rpx;
  background: #eaf4ff;
  color: $escort-color-primary;
  font-size: 34rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-card.passed .status-mark {
  background: #e9f9f0;
  color: #16a35a;
}

.status-card.rejected .status-mark,
.status-card.expired .status-mark {
  background: #fff0f0;
  color: #ef4444;
}

.status-copy {
  flex: 1;
  min-width: 0;
}

.status-title {
  display: block;
  font-size: 38rpx;
  line-height: 1.3;
  font-weight: 900;
  color: #172033;
}

.status-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  line-height: 1.55;
  color: #60738d;
}

.progress-card {
  margin-top: 26rpx;
  padding: 22rpx;
  border-radius: 26rpx;
  background: #f7fbff;
  border: 1rpx solid #e1ecfa;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #4d617a;
  font-size: 24rpx;
  font-weight: 800;
}

.progress-track {
  margin-top: 16rpx;
  height: 16rpx;
  border-radius: 999rpx;
  background: #e8f0fa;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $escort-color-primary 0%, #27b89a 100%);
}

.reason-card {
  margin-top: 22rpx;
  padding: 22rpx;
  border-radius: 26rpx;
  background: #fff5f5;
  border: 1rpx solid #ffd7d7;
}

.reason-title {
  display: block;
  font-size: 24rpx;
  color: #c24141;
  font-weight: 900;
}

.reason-text {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #7f1d1d;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 31rpx;
  color: #172033;
  font-weight: 900;
}

.section-note {
  font-size: 24rpx;
  color: #7d8ea2;
}

.material-list {
  display: grid;
  gap: 18rpx;
}

.material-card {
  min-height: 132rpx;
  padding: 16rpx;
  border-radius: 28rpx;
  background: #f8fbff;
  border: 1rpx solid #e0ebf8;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.material-preview {
  width: 110rpx;
  height: 86rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: #eaf4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $escort-color-primary;
  font-size: 30rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.material-preview image {
  width: 100%;
  height: 100%;
}

.material-preview.empty {
  border: 1rpx dashed #b8cff0;
}

.material-copy {
  flex: 1;
  min-width: 0;
}

.material-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.material-title {
  font-size: 28rpx;
  font-weight: 900;
  color: #172033;
}

.material-badge {
  height: 38rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
}

.material-badge.ready {
  background: #e9f9f0;
  color: #16a35a;
}

.material-badge.missing {
  background: #fff4e5;
  color: #c06d00;
}

.material-desc,
.expire-text {
  display: block;
  margin-top: 7rpx;
  font-size: 23rpx;
  line-height: 1.35;
  color: #7d8ea2;
}

.expire-text.expired {
  color: #ef4444;
  font-weight: 800;
}

.material-action {
  color: $escort-color-primary;
  font-size: 25rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.log-list {
  display: grid;
  gap: 18rpx;
}

.log-row {
  display: flex;
  gap: 16rpx;
}

.log-dot {
  width: 18rpx;
  height: 18rpx;
  margin-top: 10rpx;
  border-radius: 50%;
  background: $escort-color-primary;
  flex-shrink: 0;
}

.log-copy {
  flex: 1;
  min-width: 0;
}

.log-action {
  display: block;
  font-size: 27rpx;
  color: #172033;
  font-weight: 900;
}

.log-time,
.log-reason {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  line-height: 1.45;
  color: #6b7890;
}

.empty-log {
  padding: 28rpx;
  border-radius: 24rpx;
  background: #f7fbff;
  color: #7d8ea2;
  font-size: 25rpx;
  text-align: center;
}

.submit-btn {
  height: 88rpx;
  border-radius: 60rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
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

.passed-footer {
  margin-top: 28rpx;
  padding: 24rpx;
  border-radius: 26rpx;
  background: #e9f9f0;
  color: #16724a;
  font-size: 25rpx;
  text-align: center;
  font-weight: 800;
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
