<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-mark">资</view>
      <view class="hero-copy">
        <text class="hero-title">上传资质材料</text>
        <text class="hero-desc">按步骤补齐材料，外层显示便于审核的预览图，点击可查看原图。</text>
      </view>
    </view>

    <view class="card step-card slide-up delay-1">
      <view class="section-head">
        <view>
          <text class="section-kicker">步骤 1</text>
          <text class="section-title">身份证正反面</text>
        </view>
        <text class="section-status" :class="idCardReady ? 'status-pass' : 'status-warn'">
          {{ idCardReady ? '已完成' : '未完成' }}
        </text>
      </view>
      <view class="id-grid">
        <view class="id-slot" :class="{ focused: isFocused('idCardFront') }" @click="uploadByKey('idCardFront')">
          <image v-if="idCardFrontDisplay" class="preview" :src="toFullUrl(idCardFrontDisplay)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传身份证正面</text>
          </view>
          <view class="slot-foot">
            <text class="slot-label">身份证正面</text>
            <text v-if="idCardFrontOriginal" class="slot-link" @click.stop="previewCert(idCardFrontOriginal)">原图</text>
          </view>
        </view>
        <view class="id-slot" :class="{ focused: isFocused('idCardBack') }" @click="uploadByKey('idCardBack')">
          <image v-if="idCardBackDisplay" class="preview" :src="toFullUrl(idCardBackDisplay)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传身份证背面</text>
          </view>
          <view class="slot-foot">
            <text class="slot-label">身份证背面</text>
            <text v-if="idCardBackOriginal" class="slot-link" @click.stop="previewCert(idCardBackOriginal)">原图</text>
          </view>
        </view>
      </view>
      <text class="tip">上传前会先进入框选页，请让证件四边完整落入框内。</text>
    </view>

    <view
      class="card cert-step slide-up"
      :class="section.delay"
      v-for="section in certSections"
      :key="section.key"
    >
      <view class="section-head">
        <view>
          <text class="section-kicker">{{ section.step }}</text>
          <text class="section-title">{{ section.title }}</text>
        </view>
        <text class="section-status" :class="section.ready ? 'status-pass' : 'status-warn'">
          {{ section.ready ? '已完成' : '未完成' }}
        </text>
      </view>
      <view class="cert-layout">
        <view class="cert-preview" :class="{ focused: isFocused(section.key), empty: !section.displayUrl }" @click="onCertCardTap(section.key, section.originalUrl)">
          <image v-if="section.displayUrl" class="preview" :src="toFullUrl(section.displayUrl)" mode="aspectFill"></image>
          <view v-else class="placeholder">
            <text class="plus">+</text>
            <text class="placeholder-text">上传{{ section.title }}</text>
          </view>
        </view>
        <view class="cert-panel">
          <text class="cert-desc">{{ section.desc }}</text>
          <view class="date-picker" :class="{ empty: !section.expireDate, expired: section.expired }" @click="openDatePicker(section.key)">
            <text>{{ section.expireDate || '选择有效期' }}</text>
            <text v-if="section.expired" class="date-badge">已过期</text>
          </view>
          <view class="cert-actions">
            <text v-if="section.originalUrl" class="cert-action" @click.stop="previewCert(section.originalUrl)">查看原图</text>
            <text class="cert-action primary" @click.stop="uploadByKey(section.key)">{{ section.originalUrl ? '修改材料' : '上传材料' }}</text>
          </view>
          <text v-if="section.missingText" class="inline-error">{{ section.missingText }}</text>
        </view>
      </view>
    </view>

    <view class="bottom-panel slide-up delay-3">
      <text class="validation-text" :class="{ ok: allMaterialsReady }">{{ validationText }}</text>
      <view class="bottom-actions">
        <button class="bottom-btn secondary" :disabled="saving || submitting" @click="goBack">保存材料</button>
        <button class="bottom-btn primary" :disabled="submitDisabled" @click="submitForReview">
          {{ submitting ? '提交中...' : '提交审核' }}
        </button>
      </view>
    </view>

    <view v-if="datePickerVisible" class="sheet-mask" @click="closeDatePicker">
      <view class="date-sheet" @click.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="sheet-kicker">证件有效期</text>
            <text class="sheet-title">{{ datePickerTitle }}</text>
          </view>
          <text class="sheet-close" @click="closeDatePicker">关闭</text>
        </view>
        <picker-view class="date-wheel" :value="datePickerValue" @change="onDateWheelChange">
          <picker-view-column>
            <view class="wheel-item" v-for="year in yearOptions" :key="year">{{ year }}年</view>
          </picker-view-column>
          <picker-view-column>
            <view class="wheel-item" v-for="month in monthOptions" :key="month">{{ pad(month) }}月</view>
          </picker-view-column>
          <picker-view-column>
            <view class="wheel-item" v-for="day in dayOptions" :key="day">{{ pad(day) }}日</view>
          </picker-view-column>
        </picker-view>
        <button class="sheet-confirm" :disabled="saving || submitting" @click="confirmDatePicker">确认有效期</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { post, put, upload } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { resolveImageUrl } from '@/utils/media.js'
import { pickFrameIdCardFile } from '@/utils/id-card-upload.js'
import { getQualificationImageFields } from '@/utils/qualification.mjs'

const userStore = useUserStore()
const { attendantInfo } = storeToRefs(userStore)

const focusType = ref('')
const saving = ref(false)
const submitting = ref(false)
const practiceCertExpireDate = ref('')
const healthCertExpireDate = ref('')
const datePickerVisible = ref(false)
const datePickerTarget = ref('')
const datePickerValue = ref([0, 0, 0])
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 31 }, (_, index) => currentYear + index)
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const qualificationImages = computed(() => getQualificationImageFields(attendantInfo.value))
const idCardFront = computed(() => qualificationImages.value.idCardFront)
const idCardBack = computed(() => qualificationImages.value.idCardBack)
const practiceCertUrl = computed(() => qualificationImages.value.practiceCert)
const healthCertUrl = computed(() => qualificationImages.value.healthCert)
const idCardReady = computed(() => !!idCardFront.value && !!idCardBack.value)
const idCardFrontOriginal = computed(() => attendantInfo.value.idCardFrontFileUrl || attendantInfo.value.idCardFileUrl || idCardFront.value)
const idCardFrontDisplay = computed(() => attendantInfo.value.idCardFrontScanFileUrl || idCardFrontOriginal.value)
const idCardBackOriginal = computed(() => attendantInfo.value.idCardBackFileUrl || idCardBack.value)
const idCardBackDisplay = computed(() => attendantInfo.value.idCardBackScanFileUrl || idCardBackOriginal.value)
const practiceCertOriginal = computed(() => attendantInfo.value.practiceCertFileUrl || practiceCertUrl.value)
const practiceCertDisplay = computed(() => attendantInfo.value.practiceCertScanFileUrl || practiceCertOriginal.value)
const healthCertOriginal = computed(() => attendantInfo.value.healthCertFileUrl || healthCertUrl.value)
const healthCertDisplay = computed(() => attendantInfo.value.healthCertScanFileUrl || healthCertOriginal.value)
const practiceCertReady = computed(() => !!practiceCertOriginal.value && !!practiceCertExpireDate.value && !attendantInfo.value.practiceCertExpired)
const healthCertReady = computed(() => !!healthCertOriginal.value && !!healthCertExpireDate.value && !attendantInfo.value.healthCertExpired)
const allMaterialsReady = computed(() => idCardReady.value && practiceCertReady.value && healthCertReady.value)
const submitDisabled = computed(() => saving.value || submitting.value || !allMaterialsReady.value)
const selectedYear = computed(() => yearOptions[datePickerValue.value[0]] || currentYear)
const selectedMonth = computed(() => monthOptions[datePickerValue.value[1]] || 1)
const dayOptions = computed(() => {
  const count = new Date(selectedYear.value, selectedMonth.value, 0).getDate()
  return Array.from({ length: count }, (_, index) => index + 1)
})
const datePickerTitle = computed(() => datePickerTarget.value === 'healthCert' ? '选择健康证到期日期' : '选择执业证书到期日期')

const isFocused = (key) => focusType.value === key || (focusType.value === 'idCard' && (key === 'idCardFront' || key === 'idCardBack'))

const certSections = computed(() => [
  {
    key: 'practiceCert',
    step: '步骤 2',
    title: '执业证书',
    desc: '上传清晰证书照片，并填写有效期。到期后需要重新提交审核。',
    originalUrl: practiceCertOriginal.value,
    displayUrl: practiceCertDisplay.value,
    expireDate: practiceCertExpireDate.value,
    expired: attendantInfo.value.practiceCertExpired,
    ready: practiceCertReady.value,
    missingText: !practiceCertOriginal.value
      ? '请上传执业证书'
      : !practiceCertExpireDate.value
        ? '请填写执业证书有效期'
        : attendantInfo.value.practiceCertExpired
          ? '执业证书已过期，请更新'
          : '',
    delay: 'delay-2',
  },
  {
    key: 'healthCert',
    step: '步骤 3',
    title: '健康证',
    desc: '健康证需清晰可识别，并处于有效期内。',
    originalUrl: healthCertOriginal.value,
    displayUrl: healthCertDisplay.value,
    expireDate: healthCertExpireDate.value,
    expired: attendantInfo.value.healthCertExpired,
    ready: healthCertReady.value,
    missingText: !healthCertOriginal.value
      ? '请上传健康证'
      : !healthCertExpireDate.value
        ? '请填写健康证有效期'
        : attendantInfo.value.healthCertExpired
          ? '健康证已过期，请更新'
          : '',
    delay: 'delay-3',
  },
])

const validationText = computed(() => {
  if (allMaterialsReady.value) return '材料已完整，可以提交审核'
  if (!idCardFront.value) return '请先上传身份证正面'
  if (!idCardBack.value) return '请先上传身份证背面'
  const firstMissingCert = certSections.value.find((item) => item.missingText)
  return firstMissingCert?.missingText || '请补全资质材料'
})

const toFullUrl = (url) => {
  return resolveImageUrl(url, '')
}

const pad = (value) => String(value).padStart(2, '0')

const parseDateToPickerValue = (dateText) => {
  const fallback = new Date()
  fallback.setFullYear(currentYear + 1)
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateText || '')
  const year = match ? Number(match[1]) : fallback.getFullYear()
  const month = match ? Number(match[2]) : fallback.getMonth() + 1
  const day = match ? Number(match[3]) : fallback.getDate()
  const yearIndex = Math.max(0, yearOptions.findIndex((item) => item === year))
  const monthIndex = Math.max(0, Math.min(11, month - 1))
  const dayCount = new Date(yearOptions[yearIndex] || currentYear, monthIndex + 1, 0).getDate()
  return [yearIndex, monthIndex, Math.max(0, Math.min(dayCount - 1, day - 1))]
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

const normalizeUploadResult = (uploadRes = {}) => {
  const data = uploadRes.data && typeof uploadRes.data === 'object' ? uploadRes.data : {}
  const originalUrl = data.originalUrl || data.url || uploadRes.url || uploadRes.data || ''
  const scanUrl = data.scanUrl || originalUrl
  return {
    originalUrl,
    scanUrl,
  }
}

const chooseCertFile = () =>
  new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFilePaths && res.tempFilePaths[0]
        filePath ? resolve(filePath) : reject(new Error('未选择图片'))
      },
      fail: reject
    })
  })

const compressSelectedImage = (filePath) =>
  new Promise((resolve) => {
    if (!filePath) {
      resolve('')
      return
    }
    uni.compressImage({
      src: filePath,
      quality: 86,
      compressedWidth: 1280,
      compressedHeight: 1280,
      success: (result) => resolve(result?.tempFilePath || filePath),
      fail: () => resolve(filePath)
    })
  })

const uploadByKey = async (key) => {
  if (saving.value || submitting.value) return
  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  try {
    const filePath = key === 'idCardFront'
      ? await pickFrameIdCardFile('front')
      : key === 'idCardBack'
        ? await pickFrameIdCardFile('back')
        : await chooseCertFile()
    if (!filePath) return
    const preparedFilePath = key === 'idCardFront' || key === 'idCardBack'
      ? filePath
      : await compressSelectedImage(filePath)

    saving.value = true
    const uploadRes = await upload('/api/common/upload-image', preparedFilePath || filePath)
    const { originalUrl, scanUrl } = normalizeUploadResult(uploadRes)
    if (!originalUrl) {
      uni.showToast({ title: '上传返回异常', icon: 'none' })
      return
    }

    const payload = {}
    if (key === 'idCardFront') {
      payload.idCardFrontFileUrl = originalUrl
      payload.idCardFrontScanFileUrl = scanUrl
      payload.idCardFileUrl = originalUrl
    } else if (key === 'idCardBack') {
      payload.idCardBackFileUrl = originalUrl
      payload.idCardBackScanFileUrl = scanUrl
    } else if (key === 'practiceCert') {
      payload.practiceCertFileUrl = originalUrl
      payload.practiceCertScanFileUrl = scanUrl
      payload.practiceCertUploaded = 1
      if (practiceCertExpireDate.value) payload.practiceCertExpireDate = practiceCertExpireDate.value
    } else if (key === 'healthCert') {
      payload.healthCertFileUrl = originalUrl
      payload.healthCertScanFileUrl = scanUrl
      payload.healthCertUploaded = 1
      if (healthCertExpireDate.value) payload.healthCertExpireDate = healthCertExpireDate.value
    }

    await put(`/attendant/qualification/${uid}`, payload)
    await userStore.fetchAttendantProfile(uid)
    uni.showToast({ title: '上传成功', icon: 'success' })
  } catch (error) {
    if (error?.message !== 'cancel' && !String(error?.errMsg || '').includes('cancel')) {
      uni.showToast({ title: error?.message || error?.errMsg || error?.data?.message || '上传失败', icon: 'none' })
    }
  } finally {
    saving.value = false
  }
}

const saveExpireDate = async (type, value) => {
  if (!value || saving.value || submitting.value) return
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

const openDatePicker = (type) => {
  if (saving.value || submitting.value) return
  datePickerTarget.value = type
  const currentValue = type === 'practiceCert' ? practiceCertExpireDate.value : healthCertExpireDate.value
  datePickerValue.value = parseDateToPickerValue(currentValue)
  datePickerVisible.value = true
}

const closeDatePicker = () => {
  datePickerVisible.value = false
  datePickerTarget.value = ''
}

const onDateWheelChange = (event) => {
  const value = Array.isArray(event?.detail?.value) ? event.detail.value : datePickerValue.value
  const yearIndex = Math.max(0, Math.min(yearOptions.length - 1, value[0] || 0))
  const monthIndex = Math.max(0, Math.min(monthOptions.length - 1, value[1] || 0))
  const dayCount = new Date(yearOptions[yearIndex], monthIndex + 1, 0).getDate()
  const dayIndex = Math.max(0, Math.min(dayCount - 1, value[2] || 0))
  datePickerValue.value = [yearIndex, monthIndex, dayIndex]
}

const confirmDatePicker = async () => {
  const target = datePickerTarget.value
  if (!target) return
  const year = yearOptions[datePickerValue.value[0]] || currentYear
  const month = monthOptions[datePickerValue.value[1]] || 1
  const day = dayOptions.value[datePickerValue.value[2]] || 1
  closeDatePicker()
  await saveExpireDate(target, `${year}-${pad(month)}-${pad(day)}`)
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
  if (saving.value || submitting.value) return
  uni.navigateBack()
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
      setTimeout(() => {
        uni.redirectTo({ url: '/subpkg/profile/qualification' })
      }, 500)
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
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
  padding: 24rpx 24rpx calc(190rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.hero-card {
  @include escort-card(30rpx);
  border: 1rpx solid #e5eefb;
  background: linear-gradient(135deg, #ffffff 0%, #f3f8ff 100%);
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.hero-mark {
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

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-title {
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  color: #172033;
}

.hero-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  line-height: 1.55;
  color: #61738a;
}

.card {
  @include escort-card(30rpx);
  margin-top: 24rpx;
  border: 1rpx solid #e3edf9;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-kicker {
  display: block;
  margin-bottom: 4rpx;
  font-size: 22rpx;
  font-weight: 900;
  color: $escort-color-primary;
}

.section-title {
  display: block;
  font-size: 31rpx;
  font-weight: 900;
  color: #172033;
}

.section-status {
  font-size: 24rpx;
  font-weight: 900;
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
  gap: 18rpx;
}

.id-slot {
  border-radius: 28rpx;
  border: 1rpx solid #dfeaf7;
  overflow: hidden;
  background: #f8fafc;
}

.id-slot.focused {
  border-color: $escort-color-primary;
  box-shadow: 0 0 0 2rpx rgba(102, 166, 255, 0.14);
}

.preview {
  width: 100%;
  height: 210rpx;
  background: #ffffff;
}

.placeholder {
  width: 100%;
  height: 210rpx;
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
  font-size: 23rpx;
}

.slot-foot {
  min-height: 64rpx;
  padding: 0 18rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.slot-label {
  color: #172033;
  font-size: 24rpx;
  font-weight: 800;
}

.slot-link {
  color: $escort-color-primary;
  font-size: 23rpx;
  font-weight: 900;
}

.tip {
  display: block;
  margin-top: 16rpx;
  color: #9ca3af;
  font-size: 23rpx;
}

.cert-layout {
  display: flex;
  align-items: stretch;
  gap: 18rpx;
}

.cert-preview {
  width: 230rpx;
  min-height: 260rpx;
  border-radius: 28rpx;
  border: 1rpx solid #dfeaf7;
  overflow: hidden;
  background: #f8fafc;
  flex-shrink: 0;
}

.cert-preview.focused {
  border-color: $escort-color-primary;
  box-shadow: 0 0 0 2rpx rgba(102, 166, 255, 0.14);
}

.cert-preview .preview {
  height: 260rpx;
}

.cert-preview .placeholder {
  height: 260rpx;
}

.cert-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.cert-desc {
  display: block;
  color: #5d738b;
  font-size: 25rpx;
  line-height: 1.55;
}

.date-picker {
  margin-top: 18rpx;
  min-height: 72rpx;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid #d8e6f5;
  color: #1f2937;
  font-size: 25rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.date-picker.empty {
  color: $escort-color-primary;
  font-weight: 900;
}

.date-picker.expired {
  border-color: #ffd1d1;
  background: #fff5f5;
}

.date-badge {
  height: 38rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: #ffe4e4;
  color: #ef4444;
  font-size: 21rpx;
  display: flex;
  align-items: center;
}

.cert-actions {
  margin-top: 18rpx;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
}

.cert-action {
  height: 54rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid #dbe4f0;
  background: #f8fbff;
  color: #4b5563;
  font-size: 23rpx;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cert-action.primary {
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.1);
  color: $escort-color-primary;
}

.inline-error {
  display: block;
  margin-top: 14rpx;
  color: #ef4444;
  font-size: 23rpx;
  line-height: 1.4;
}

.bottom-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 900;
  padding: 18rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));
  background: rgba(246, 249, 255, 0.96);
  border-top: 1rpx solid #dce8f8;
  box-shadow: 0 -12rpx 30rpx rgba(18, 56, 109, 0.08);
  box-sizing: border-box;
}

.validation-text {
  display: block;
  margin-bottom: 14rpx;
  color: #ef4444;
  font-size: 24rpx;
  font-weight: 800;
}

.validation-text.ok {
  color: #16a35a;
}

.bottom-actions {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 16rpx;
}

.bottom-btn {
  height: 88rpx;
  border-radius: 60rpx;
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 29rpx;
  font-weight: 900;
}

.bottom-btn.secondary {
  background: #ffffff;
  color: #4d617a;
  border: 1rpx solid #d8e6f5;
}

.bottom-btn.primary {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  color: #ffffff;
  box-shadow: $escort-shadow-primary;
}

.bottom-btn[disabled],
.bottom-btn.primary[disabled] {
  background: #c0c4cc;
  color: #ffffff;
  border: none;
  box-shadow: none;
}

.sheet-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 1200;
  background: rgba(10, 24, 45, 0.38);
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.18s ease forwards;
}

.date-sheet {
  width: 100%;
  padding: 18rpx 28rpx calc(32rpx + env(safe-area-inset-bottom));
  border-radius: 36rpx 36rpx 0 0;
  background: #ffffff;
  box-shadow: 0 -24rpx 60rpx rgba(17, 35, 64, 0.16);
  box-sizing: border-box;
  animation: sheetUp 0.24s ease forwards;
}

.sheet-handle {
  width: 76rpx;
  height: 8rpx;
  margin: 0 auto 22rpx;
  border-radius: 999rpx;
  background: #d8e4f2;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
}

.sheet-kicker {
  display: block;
  color: $escort-color-primary;
  font-size: 23rpx;
  font-weight: 900;
}

.sheet-title {
  display: block;
  margin-top: 4rpx;
  color: #172033;
  font-size: 32rpx;
  font-weight: 900;
}

.sheet-close {
  height: 60rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #f3f7fc;
  color: #5d738b;
  font-size: 24rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.date-wheel {
  height: 300rpx;
  margin-top: 20rpx;
  border-radius: 28rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 48%, #f8fbff 100%);
  overflow: hidden;
}

.wheel-item {
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  color: #172033;
  font-size: 30rpx;
  font-weight: 800;
}

.sheet-confirm {
  height: 88rpx;
  margin: 22rpx 0 0;
  border: none;
  border-radius: 999rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  color: #ffffff;
  font-size: 29rpx;
  font-weight: 900;
  box-shadow: $escort-shadow-primary;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sheet-confirm[disabled] {
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

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes sheetUp {
  from {
    transform: translateY(36rpx);
  }
  to {
    transform: translateY(0);
  }
}
</style>
