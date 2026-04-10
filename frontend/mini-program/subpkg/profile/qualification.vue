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
        </view>

        <view class="ghost-btn" @click="goUpload(item.key)">
          <text>{{ item.uploaded ? '管理' : '上传' }}</text>
        </view>
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

const idCardFront = computed(() => attendantInfo.value.idCardFrontFileUrl || attendantInfo.value.idCardFileUrl || '')
const idCardBack = computed(() => attendantInfo.value.idCardBackFileUrl || '')
const idCardReady = computed(() => !!idCardFront.value && !!idCardBack.value)
const practiceReady = computed(() => !!attendantInfo.value.practiceCertFileUrl || !!attendantInfo.value.practiceCertUploaded)
const healthReady = computed(() => !!attendantInfo.value.healthCertFileUrl || !!attendantInfo.value.healthCertUploaded)

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
    desc: '证书清晰可见',
    uploaded: practiceReady.value
  },
  {
    key: 'healthCert',
    label: '健康证',
    desc: '需在有效期内',
    uploaded: healthReady.value
  }
])

const submitDisabled = computed(() => submitting.value || !(idCardReady.value && practiceReady.value && healthReady.value))

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
  align-items: center;
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
