<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/ren_1.png" mode="aspectFit"></image>
        <view>
          <text class="hero-title">上传资质材料</text>
          <text class="hero-desc">身份证需上传正反面，其他证件单独上传</text>
        </view>
      </view>

      <view class="expire-form">
        <view class="expire-row">
          <view>
            <text class="expire-label">执业证书有效期</text>
            <text class="expire-desc">到期后需要重新提交审核</text>
          </view>
          <picker mode="date" :value="practiceCertExpireDate" @change="onExpireDateChange('practiceCert', $event)">
            <view class="date-picker" :class="{ empty: !practiceCertExpireDate }">
              {{ practiceCertExpireDate || '选择日期' }}
            </view>
          </picker>
        </view>
        <view class="expire-row">
          <view>
            <text class="expire-label">健康证有效期</text>
            <text class="expire-desc">健康证必须在有效期内</text>
          </view>
          <picker mode="date" :value="healthCertExpireDate" @change="onExpireDateChange('healthCert', $event)">
            <view class="date-picker" :class="{ empty: !healthCertExpireDate }">
              {{ healthCertExpireDate || '选择日期' }}
            </view>
          </picker>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-1">
      <view class="section-head">
        <text class="section-title">身份证（正反面）</text>
        <text class="section-status" :class="idCardReady ? 'status-pass' : 'status-warn'">
          {{ idCardReady ? '已完成' : '未完成' }}
        </text>
      </view>
      <view class="id-grid">
        <view class="id-slot" :class="{ focused: focusType === 'idCard' }" @click="uploadByKey('idCardFront')">
          <image v-if="idCardFront" class="preview" :src="toFullUrl(idCardFront)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传身份证正面</text>
          </view>
          <text class="slot-label">正面</text>
        </view>
        <view class="id-slot" :class="{ focused: focusType === 'idCard' }" @click="uploadByKey('idCardBack')">
          <image v-if="idCardBack" class="preview" :src="toFullUrl(idCardBack)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传身份证背面</text>
          </view>
          <text class="slot-label">背面</text>
        </view>
      </view>
      <text class="tip">仅支持 JPG/PNG，建议文字清晰无遮挡</text>
    </view>

    <view class="card slide-up delay-2">
      <view class="section-head">
        <text class="section-title">其他证件</text>
        <text class="section-status" :class="practiceCertUrl && healthCertUrl ? 'status-pass' : 'status-warn'">
          {{ practiceCertUrl && healthCertUrl ? '已完成' : '未完成' }}
        </text>
      </view>

      <view class="cert-grid">
        <view class="cert-slot" :class="{ focused: focusType === 'practiceCert' }" @click="onCertCardTap('practiceCert', practiceCertUrl)">
          <image v-if="practiceCertUrl" class="preview" :src="toFullUrl(practiceCertUrl)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传执业证书</text>
          </view>
          <view class="cert-foot">
            <view class="cert-meta">
              <text class="cert-name">执业证书</text>
              <text class="cert-state" :class="practiceCertUrl ? 'pass' : 'warn'">
                {{ practiceCertUrl ? '已上传' : '未上传' }}
              </text>
            </view>
            <view class="cert-actions">
              <text
                v-if="practiceCertUrl"
                class="cert-action"
                @click.stop="previewCert(practiceCertUrl)"
              >预览</text>
              <text
                class="cert-action primary"
                @click.stop="uploadByKey('practiceCert')"
              >{{ practiceCertUrl ? '修改' : '上传' }}</text>
            </view>
          </view>
        </view>

        <view class="cert-slot" :class="{ focused: focusType === 'healthCert' }" @click="onCertCardTap('healthCert', healthCertUrl)">
          <image v-if="healthCertUrl" class="preview" :src="toFullUrl(healthCertUrl)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传健康证</text>
          </view>
          <view class="cert-foot">
            <view class="cert-meta">
              <text class="cert-name">健康证</text>
              <text class="cert-state" :class="healthCertUrl ? 'pass' : 'warn'">
                {{ healthCertUrl ? '已上传' : '未上传' }}
              </text>
            </view>
            <view class="cert-actions">
              <text
                v-if="healthCertUrl"
                class="cert-action"
                @click.stop="previewCert(healthCertUrl)"
              >预览</text>
              <text
                class="cert-action primary"
                @click.stop="uploadByKey('healthCert')"
              >{{ healthCertUrl ? '修改' : '上传' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-btn slide-up delay-3" :class="{ disabled: saving }" @click="goBack">
      <text>{{ saving ? '处理中...' : '完成并返回' }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { put, upload } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { resolveImageUrl } from '@/utils/media.js'

const userStore = useUserStore()
const { attendantInfo } = storeToRefs(userStore)

const focusType = ref('')
const saving = ref(false)
const practiceCertExpireDate = ref('')
const healthCertExpireDate = ref('')

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const idCardFront = computed(() => attendantInfo.value.idCardFrontFileUrl || attendantInfo.value.idCardFileUrl || '')
const idCardBack = computed(() => attendantInfo.value.idCardBackFileUrl || '')
const practiceCertUrl = computed(() => attendantInfo.value.practiceCertFileUrl || '')
const healthCertUrl = computed(() => attendantInfo.value.healthCertFileUrl || '')
const idCardReady = computed(() => !!idCardFront.value && !!idCardBack.value)

const toFullUrl = (url) => {
  return resolveImageUrl(url, '')
}

const loadProfile = async () => {
  const uid = userId()
  if (!uid) return
  await userStore.fetchAttendantProfile(uid)
  practiceCertExpireDate.value = attendantInfo.value.practiceCertExpireDate || ''
  healthCertExpireDate.value = attendantInfo.value.healthCertExpireDate || ''
}

const previewImage = (url) => {
  const full = toFullUrl(url)
  if (!full) return
  uni.previewImage({
    urls: [full],
    current: full
  })
}

const uploadByKey = (key) => {
  if (saving.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const uid = userId()
      if (!uid) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      const filePath = res.tempFilePaths && res.tempFilePaths[0]
      if (!filePath) return

      saving.value = true
      try {
        const uploadRes = await upload('/api/common/upload-image', filePath)
        const fileUrl = uploadRes.url || uploadRes.data || ''
        if (!fileUrl) {
          uni.showToast({ title: '上传返回异常', icon: 'none' })
          return
        }

        const payload = {}
        if (key === 'idCardFront') {
          payload.idCardFrontFileUrl = fileUrl
          payload.idCardFileUrl = fileUrl
        } else if (key === 'idCardBack') {
          payload.idCardBackFileUrl = fileUrl
        } else if (key === 'practiceCert') {
          payload.practiceCertFileUrl = fileUrl
          payload.practiceCertUploaded = 1
          if (practiceCertExpireDate.value) payload.practiceCertExpireDate = practiceCertExpireDate.value
        } else if (key === 'healthCert') {
          payload.healthCertFileUrl = fileUrl
          payload.healthCertUploaded = 1
          if (healthCertExpireDate.value) payload.healthCertExpireDate = healthCertExpireDate.value
        }

        await put(`/attendant/qualification/${uid}`, payload)
        await userStore.fetchAttendantProfile(uid)
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        saving.value = false
      }
    }
  })
}

const onExpireDateChange = async (type, event) => {
  const value = event?.detail?.value || ''
  if (!value || saving.value) return
  if (type === 'practiceCert') practiceCertExpireDate.value = value
  else healthCertExpireDate.value = value

  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload = {}
    if (type === 'practiceCert') payload.practiceCertExpireDate = value
    else payload.healthCertExpireDate = value
    await put(`/attendant/qualification/${uid}`, payload)
    await userStore.fetchAttendantProfile(uid)
    uni.showToast({ title: '有效期已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error?.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const onCertCardTap = (key, url) => {
  if (url) {
    previewImage(url)
    return
  }
  uploadByKey(key)
}

const previewCert = (url) => {
  previewImage(url)
}

const goBack = () => {
  if (saving.value) return
  uni.navigateBack()
}

onLoad((options) => {
  focusType.value = options && options.type ? String(options.type) : ''
})

onMounted(loadProfile)
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
  @include escort-card(28rpx);
  margin: 24rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.section-status {
  font-size: 24rpx;
  font-weight: 600;
}

.status-pass {
  color: #52c41a;
}

.status-warn {
  color: #ff4d4f;
}

.id-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.id-slot {
  border-radius: 14rpx;
  border: 1rpx solid #e5eaf2;
  overflow: hidden;
  background: #f8fafc;
}

.id-slot.focused {
  border-color: $escort-color-primary;
  box-shadow: 0 0 0 2rpx rgba(102, 166, 255, 0.14);
}

.preview {
  width: 100%;
  height: 190rpx;
}

.placeholder {
  width: 100%;
  height: 190rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.plus {
  color: $escort-color-primary;
  font-size: 42rpx;
  line-height: 1;
}

.placeholder-text {
  margin-top: 8rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.slot-label {
  display: block;
  text-align: center;
  color: #4b5563;
  font-size: 24rpx;
  line-height: 58rpx;
  background: #fff;
}

.tip {
  display: block;
  margin-top: 10rpx;
  color: #9ca3af;
  font-size: 22rpx;
}

.cert-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.expire-form {
  margin-top: 24rpx;
  display: grid;
  gap: 16rpx;
}

.expire-row {
  min-height: 88rpx;
  padding: 18rpx 20rpx;
  border: 1rpx solid #e5eefb;
  border-radius: 24rpx;
  background: #f8fbff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.expire-label {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #1f2937;
}

.expire-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a97aa;
}

.date-picker {
  min-width: 190rpx;
  height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 1rpx solid #d8e6f5;
  color: #1f2937;
  font-size: 25rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.date-picker.empty {
  color: $escort-color-primary;
  font-weight: 700;
}

.cert-slot {
  border-radius: 14rpx;
  border: 1rpx solid #e5eaf2;
  overflow: hidden;
  background: #f8fafc;
}

.cert-slot.focused {
  border-color: $escort-color-primary;
  box-shadow: 0 0 0 2rpx rgba(102, 166, 255, 0.14);
}

.cert-foot {
  min-height: 96rpx;
  background: #fff;
  padding: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.cert-meta {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.cert-name {
  font-size: 24rpx;
  color: #1f2937;
  font-weight: 600;
}

.cert-state {
  font-size: 22rpx;
}

.cert-state.pass {
  color: #52c41a;
}

.cert-state.warn {
  color: #ff4d4f;
}

.cert-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.cert-action {
  height: 48rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid #dbe4f0;
  background: #f8fbff;
  color: #4b5563;
  font-size: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cert-action.primary {
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.1);
  color: $escort-color-primary;
  font-weight: 600;
}

.bottom-btn {
  height: 88rpx;
  border-radius: 60rpx;
  background: $escort-color-primary;
  margin: 16rpx 24rpx 0;
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

.bottom-btn.disabled {
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
