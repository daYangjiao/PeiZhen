<template>
  <view class="order-confirm-page">
    <view class="step-bar">
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">选择服务</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">描述症状</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">提交需求</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item active">
        <text class="step-dot"></text>
        <text class="step-text">确认订单</text>
      </view>
    </view>

    <view v-if="isLoading" class="loading-container">
      <text>加载中...</text>
    </view>

    <template v-else>
      <view class="card order-info">
        <view class="title">
          <text>订单信息</text>
        </view>
        <view class="info-item">
          <text class="label">就诊医院</text>
          <text class="value">{{ getDisplayHospital() }}</text>
        </view>
        <view class="info-item">
          <text class="label">就诊时间</text>
          <text class="value">{{ getDisplayServiceTime() }}</text>
        </view>
        <view class="info-item">
          <text class="label">就诊人</text>
          <text class="value">{{ getDisplayPatientName() }}</text>
        </view>
        <view class="info-item multiline-item">
          <text class="label">症状描述</text>
          <text class="value multiline-value">{{ getSymptomDescription() }}</text>
        </view>
        <view class="info-item last multiline-item">
          <text class="label">其他需求</text>
          <text class="value multiline-value">{{ getDisplayOtherRequirement() }}</text>
        </view>
      </view>

      <view class="card fee-detail">
        <view class="title fee-detail-head">
          <text>费用明细</text>
          <view class="rule-entry-pill" @click="showBillingRules = true">
            <text class="rule-entry-text">查看计费规则</text>
            <text class="rule-entry-arrow">›</text>
          </view>
        </view>
        <view class="fee-tip">
          <view class="fee-tip-tag">预付款说明</view>
          <view class="fee-tip-text">本次为服务预付款，服务结束后按实际时长结算，多退少补。</view>
        </view>
        <view class="fee-item">
          <text>陪诊服务预付款</text>
          <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
        </view>
        <view class="fee-item">
          <text>优惠券</text>
          <text class="discount">-¥0.00</text>
        </view>
        <view class="total">
          <text>预付款合计</text>
          <text class="total-price">¥{{ formatAmount(orderData.totalPrice) }}</text>
        </view>
      </view>

      <view class="card payment-method">
        <view class="title">
          <text>选择支付方式</text>
        </view>
        <view
          v-for="method in paymentMethods"
          :key="method.value"
          class="payment-option"
          :class="{ selected: payMethod === method.value }"
          @click="selectPayMethod(method.value)"
        >
          <view class="payment-left">
            <view class="payment-icon-shell">
              <image class="payment-icon" :src="method.icon" mode="aspectFit" />
            </view>
            <text class="payment-name">{{ method.label }}</text>
          </view>
          <view class="payment-check" :class="{ selected: payMethod === method.value }">
            <view class="payment-check-inner"></view>
          </view>
        </view>
      </view>
    </template>

    <view v-if="!isLoading" class="footer-spacer"></view>
    <view v-if="!isLoading" class="footer">
      <view class="footer-content">
        <view class="real-price">
          <text class="real-price-label">实付款：</text>
          <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
        </view>
        <button class="confirm-btn" @click="confirmPay">确认支付</button>
      </view>
    </view>

    <view v-if="showBillingRules" class="rules-sheet-overlay" @click="showBillingRules = false">
      <view class="rules-sheet" @click.stop>
        <view class="rules-sheet-handle"></view>
        <view class="rules-sheet-header">
          <view class="rules-sheet-header-copy">
            <text class="rules-sheet-eyebrow">费用说明</text>
            <text class="rules-sheet-title">计费规则</text>
            <text class="rules-sheet-subtitle">预付款先行锁单，服务结束后按实际时长结算，多退少补。</text>
          </view>
          <view class="rules-sheet-close" @click="showBillingRules = false">
            <text class="rules-sheet-close-icon">×</text>
          </view>
        </view>
        <view class="rules-sheet-body">
          <view class="rules-hero-card">
            <view class="rules-hero-badge">结算原则</view>
            <text class="rules-hero-title">先支付预付款，服务完成后自动结算差额</text>
            <text class="rules-hero-desc">如果实际服务时长高于预估，将补差价；若低于预估，系统会自动退回剩余费用。</text>
          </view>

          <view class="rules-block">
            <text class="rules-block-title">服务类型与单价</text>
            <view class="rules-rate-grid">
              <view class="rules-rate-card">
                <text class="rules-rate-name">普通陪诊</text>
                <text class="rules-rate-price">¥50 起</text>
                <text class="rules-rate-desc">含 2 小时，超出部分 ¥30/小时，不足 1 小时按 1 小时计。</text>
              </view>
              <view class="rules-rate-card">
                <text class="rules-rate-name">术后护理</text>
                <text class="rules-rate-price">¥45 / 小时</text>
                <text class="rules-rate-desc">按实际时长计费，不足 1 小时按 1 小时计。</text>
              </view>
              <view class="rules-rate-card accent">
                <text class="rules-rate-name">急诊陪同</text>
                <text class="rules-rate-price">+¥100</text>
                <text class="rules-rate-desc">在普通陪诊基础上加收加急服务费。</text>
              </view>
              <view class="rules-rate-card accent">
                <text class="rules-rate-name">上门陪诊</text>
                <text class="rules-rate-price">+¥30</text>
                <text class="rules-rate-desc">在普通陪诊基础上加收上门服务费。</text>
              </view>
            </view>
          </view>

          <view class="rules-block">
            <text class="rules-block-title">常见示例</text>
            <view class="rules-example-list">
              <view class="rules-example-item">
                <text class="rules-example-name">普通陪诊 3 小时</text>
                <text class="rules-example-value">¥50 + ¥30 × 1 = ¥80</text>
              </view>
              <view class="rules-example-item">
                <text class="rules-example-name">急诊陪同 3 小时</text>
                <text class="rules-example-value">普通陪诊 ¥80 + 加急费 ¥100 = ¥180</text>
              </view>
              <view class="rules-example-item">
                <text class="rules-example-name">术后护理 1.5 小时</text>
                <text class="rules-example-value">按 2 小时计：¥45 × 2 = ¥90</text>
              </view>
            </view>
          </view>

          <view class="rules-footnote">
            <text class="rules-footnote-title">温馨提示</text>
            <text class="rules-footnote-text">下单金额仅用于锁定陪诊服务。最终费用会在服务完成后自动按实际时长结算，多退少补。</text>
          </view>
        </view>
        <view class="rules-sheet-footer">
          <button class="rules-sheet-primary-btn" @click="showBillingRules = false">我知道了</button>
        </view>
      </view>
    </view>

    <view v-if="showPaymentModal" class="payment-modal-overlay" @click="showPaymentModal = false">
      <view class="payment-modal-content" @click.stop>
        <view class="payment-modal-header">
          <text class="payment-modal-title">支付确认</text>
        </view>
        <view class="payment-modal-body">
          <text class="payment-modal-text">请确认您的支付状态：</text>
        </view>
        <view class="payment-modal-footer">
          <button class="payment-modal-btn success-btn" @click="handlePaymentResult(true)">已支付</button>
          <button class="payment-modal-btn fail-btn" @click="handlePaymentResult(false)">未支付</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'

const orderData = ref({})
const payMethod = ref('wechat')
const isLoading = ref(true)
const showBillingRules = ref(false)
const showPaymentModal = ref(false)
const orderNo = ref('')
const paymentMethods = [
  { value: 'wechat', label: '微信支付', icon: '/static/wechat-pay-icon.png' },
  { value: 'alipay', label: '支付宝支付', icon: '/static/alipay-pay-icon.png' },
  { value: 'unionpay', label: '银联支付', icon: '/static/unionpay-pay-icon.png' }
]

onLoad((options) => {
  if (redirectPublicSafeToHome()) return
  if (options && options.orderNo) {
    orderNo.value = options.orderNo
    fetchOrderDetail(orderNo.value)
  } else {
    uni.showToast({ title: '订单号错误', icon: 'none' })
    uni.redirectTo({ url: '/pages/role-user/home' })
  }
})

const fetchOrderDetail = async (currentOrderNo) => {
  isLoading.value = true
  try {
    const response = await get(`/ai/guide/orders/${currentOrderNo}/complete-info`)
    if (response && response.code === 200 && response.data) {
      orderData.value = response.data
    } else {
      uni.showToast({ title: '获取订单信息失败', icon: 'none' })
    }
  } catch (error) {
    console.error('【OrderConfirmPage】获取订单详情失败:', error)
    uni.showToast({ title: '网络错误，获取订单信息失败', icon: 'none' })
  } finally {
    isLoading.value = false
  }
}

const selectPayMethod = (value) => {
  payMethod.value = value
}

const confirmPay = () => {
  showPaymentModal.value = true
}

const handlePaymentResult = async (isPaid) => {
  showPaymentModal.value = false
  const paymentStatus = isPaid ? 1 : 0

  try {
    await post('/ai/guide/payments/status', {
      orderNo: orderNo.value,
      paymentStatus
    })

    if (isPaid) {
      uni.showToast({ title: '支付成功，跳转中...', icon: 'none' })
      setTimeout(() => {
        const encoded = encodeURIComponent(orderNo.value)
        uni.reLaunch({
          url: '/pages/role-user/order',
          success() {
            setTimeout(() => {
              uni.navigateTo({
                url: `/subpkg/appointment-flow/05-payment-success-page?orderNo=${encoded}`
              })
            }, 150)
          }
        })
      }, 1000)
    } else {
      uni.showToast({ title: '支付未完成', icon: 'none' })
      setTimeout(() => {
        uni.navigateTo({
          url: `/subpkg/appointment-flow/06-payment-failed-page?orderNo=${encodeURIComponent(orderNo.value)}`
        })
      }, 1000)
    }
  } catch (error) {
    console.error('【调用支付状态接口失败】', error)
    if (isPaid) {
      const encoded = encodeURIComponent(orderNo.value)
      uni.reLaunch({
        url: '/pages/role-user/order',
        success() {
          setTimeout(() => {
            uni.navigateTo({
              url: `/subpkg/appointment-flow/05-payment-success-page?orderNo=${encoded}`
            })
          }, 150)
        }
      })
    } else {
      uni.navigateTo({
        url: `/subpkg/appointment-flow/06-payment-failed-page?orderNo=${encodeURIComponent(orderNo.value)}`
      })
    }
  }
}

const formatAmount = (amount) => {
  const num = Number(amount)
  if (Number.isNaN(num)) return '0.00'
  return num.toFixed(2)
}

const normalizeDisplayText = (value, fallback = '未知') => {
  if (Array.isArray(value)) {
    const items = value
      .map(item => (item == null ? '' : String(item).trim()))
      .filter(item => item && item !== 'null' && item !== 'undefined' && item !== '无')
    return items.length > 0 ? items.join('，') : fallback
  }

  if (value == null) return fallback
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return fallback
  return text
}

const normalizeTimePart = (value) => {
  if (value == null) return ''
  let text = String(value).trim()
  if (!text) return ''

  text = text
    .replace(/[：]/g, ':')
    .replace(/[．。]/g, '.')
    .replace(/\s+/g, '')
    .replace(/到|至/g, '-')
    .replace(/点半/g, ':30')
    .replace(/点/g, ':00')

  if (text.includes('-')) {
    return text.split('-').map(part => normalizeTimePart(part)).join('-')
  }

  const dotMatch = text.match(/^(\d{1,2})\.(\d{1,2})$/)
  if (dotMatch) {
    return `${dotMatch[1].padStart(2, '0')}:${dotMatch[2].padStart(2, '0')}`
  }

  const colonMatch = text.match(/^(\d{1,2}):(\d{1,2})$/)
  if (colonMatch) {
    return `${colonMatch[1].padStart(2, '0')}:${colonMatch[2].padStart(2, '0')}`
  }

  return text
}

const getDisplayHospital = () => normalizeDisplayText(
  orderData.value.hospital || orderData.value.hospitalName || orderData.value.medicalInstitution,
  '未知'
)

const getDisplayPatientName = () => normalizeDisplayText(
  orderData.value.patientName || orderData.value.contactPerson || orderData.value.patientRealName,
  '未知'
)

const getDisplayOtherRequirement = () => normalizeDisplayText(
  orderData.value.otherRequirement || orderData.value.customRequirement || orderData.value.requirement,
  '无'
)

const getSymptomDescription = () => {
  const preferred = normalizeDisplayText(orderData.value.symptomDescription, '')
  if (preferred && preferred !== '未知') return preferred

  const { symptoms } = orderData.value
  if (!symptoms) return '无'
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms
      .map(item => (item == null ? '' : String(item).trim()))
      .filter(item => item && item !== '无' && item !== 'null' && item !== 'undefined')
    return validSymptoms.length > 0 ? validSymptoms.join('，') : '无'
  }
  const text = String(symptoms).trim()
  if (!text || text === '无' || text === 'null' || text === 'undefined') return '无'
  return text
}

const formatDate = (dateStr) => {
  if (!dateStr || typeof dateStr !== 'string') return '未知'
  return dateStr
}

const getDisplayServiceTime = () => {
  const dateText = normalizeDisplayText(orderData.value.serviceDate, '')
  const startText = normalizeTimePart(orderData.value.serviceStartTime)
  const endText = normalizeTimePart(orderData.value.serviceEndTime)

  if (dateText && startText && endText) {
    return `${dateText} ${formatServiceTimeSlot(`${startText}-${endText}`)}`
  }

  const directTime = orderData.value.serviceTimeSlot || orderData.value.serviceTime || ''
  if (dateText && directTime) {
    const normalized = normalizeTimePart(directTime)
    if (normalized.includes('-')) {
      return `${dateText} ${formatServiceTimeSlot(normalized)}`
    }
    return `${dateText} ${normalized || '未知'}`
  }

  if (dateText) return dateText
  if (startText && endText) return formatServiceTimeSlot(`${startText}-${endText}`)

  return '未知'
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';

.order-confirm-page {
  @include user-page;
  min-height: 100vh;
  padding: 28rpx 24rpx 24rpx;
  box-sizing: border-box;
}

.loading-container {
  @include user-card(64rpx 24rpx);
  text-align: center;
  color: $user-color-text-sub;
  font-size: 26rpx;
}

.step-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8rpx;
  margin-bottom: 24rpx;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 15%;
  text-align: center;
  flex-shrink: 0;
}

.step-dot {
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
  background: #dce8f8;
  color: #ffffff;
  font-size: 22rpx;
  box-sizing: border-box;
}

.step-item.completed .step-dot {
  background: $user-color-success;
}

.step-item.active .step-dot {
  background: $user-color-primary;
  box-shadow: 0 0 0 8rpx rgba(0, 122, 255, 0.12);
}

.step-text {
  font-size: 22rpx;
  line-height: 1.35;
  color: $user-color-text-sub;
  white-space: nowrap;
}

.step-item.completed .step-text {
  color: $user-color-success;
}

.step-item.active .step-text {
  color: $user-color-primary;
  font-weight: 600;
}

.progress-line {
  flex: 1;
  height: 4rpx;
  background: rgba(220, 232, 248, 0.95);
  border-radius: 999rpx;
  margin: 0 12rpx;
}

.progress-line.green {
  background: rgba(82, 196, 26, 0.7);
}

.card {
  @include user-card(26rpx 24rpx);
  margin-bottom: 18rpx;
}

.title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  margin-bottom: 18rpx;
  font-size: 30rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
}

.info-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid rgba(220, 232, 248, 0.92);
}

.info-item.last {
  border-bottom: none;
  padding-bottom: 0;
}

.label {
  width: 144rpx;
  flex-shrink: 0;
  font-size: 24rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.value {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  line-height: 1.6;
  color: $user-color-text-main;
  font-weight: 600;
  text-align: right;
  word-break: break-word;
}

.multiline-item {
  align-items: flex-start;
}

.multiline-value {
  white-space: normal;
}

.fee-detail-head {
  margin-bottom: 16rpx;
}

.fee-tip {
  border-radius: 18rpx;
  padding: 18rpx 20rpx;
  background: linear-gradient(135deg, #f3f8ff 0%, #eef6ff 100%);
  margin-bottom: 14rpx;
}

.fee-tip-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 122, 255, 0.12);
  color: $user-color-primary;
  font-size: 20rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.fee-tip-text {
  font-size: 22rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.fee-item,
.total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx 0;
  font-size: 24rpx;
  color: $user-color-text-main;
}

.fee-item {
  border-bottom: 1rpx solid rgba(220, 232, 248, 0.92);
}

.price {
  color: $user-color-text-main;
  font-weight: 600;
}

.discount {
  color: $user-color-primary;
  font-weight: 600;
}

.total {
  color: $user-color-primary;
  font-weight: 700;
}

.total-price {
  font-size: 34rpx;
}

.payment-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid rgba(220, 232, 248, 0.92);
}

.payment-option:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.payment-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.payment-icon-shell {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: #f6faff;
  border: 1rpx solid rgba(220, 232, 248, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.payment-icon {
  width: 44rpx;
  height: 44rpx;
}

.payment-name {
  font-size: 28rpx;
  line-height: 1.4;
  color: $user-color-text-main;
  font-weight: 600;
}

.payment-check {
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(148, 163, 184, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.payment-check.selected {
  border-color: $user-color-primary;
}

.payment-check-inner {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: transparent;
}

.payment-check.selected .payment-check-inner {
  background: $user-color-primary;
}

.footer-spacer {
  height: calc(132rpx + env(safe-area-inset-bottom));
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 40;
  padding: 14rpx 24rpx calc(14rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20rpx);
  border-top: 1rpx solid rgba(220, 232, 248, 0.92);
  box-sizing: border-box;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  width: 100%;
}

.real-price {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  min-width: 0;
}

.real-price-label {
  font-size: 24rpx;
  color: $user-color-text-main;
  font-weight: 600;
}

.real-price .price {
  color: $user-color-primary;
  font-size: 42rpx;
  line-height: 1;
  font-weight: 700;
}

.confirm-btn {
  width: 260rpx;
  margin: 0 0 0 auto;
  @include user-primary-btn;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  height: 88rpx;
  line-height: 88rpx;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.confirm-btn::after {
  border: none;
}

.rule-entry-pill {
  margin-left: auto;
  min-height: 56rpx;
  padding: 0 22rpx 0 24rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1rpx solid rgba(37, 99, 235, 0.18);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  box-shadow: 0 10rpx 24rpx rgba(37, 99, 235, 0.10);
  flex-shrink: 0;
  box-sizing: border-box;
}

.rule-entry-text {
  font-size: 24rpx;
  line-height: 1.4;
  color: #1d4ed8;
  font-weight: 600;
}

.rule-entry-arrow {
  font-size: 26rpx;
  line-height: 1;
  color: #2563eb;
  margin-top: -2rpx;
}

.rules-sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  display: flex;
  justify-content: center;
  align-items: flex-end;
  z-index: 9999;
  padding: 32rpx 20rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  animation: rulesOverlayFadeIn 0.24s ease;
}

.rules-sheet {
  width: 100%;
  max-width: 720rpx;
  max-height: 78vh;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 36rpx 36rpx 28rpx 28rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -12rpx 40rpx rgba(15, 23, 42, 0.12), 0 16rpx 48rpx rgba(15, 23, 42, 0.18);
  overflow: hidden;
  box-sizing: border-box;
  animation: rulesSheetRiseUp 0.28s cubic-bezier(0.2, 0.9, 0.2, 1);
}

.rules-sheet-handle {
  width: 88rpx;
  height: 10rpx;
  border-radius: 999rpx;
  background: rgba(148, 163, 184, 0.35);
  margin: 16rpx auto 0;
  flex-shrink: 0;
}

.rules-sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  padding: 22rpx 30rpx 18rpx;
}

.rules-sheet-header-copy {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  flex: 1;
  min-width: 0;
}

.rules-sheet-eyebrow {
  font-size: 22rpx;
  color: $user-color-primary;
  font-weight: 700;
}

.rules-sheet-title {
  font-size: 38rpx;
  line-height: 1.18;
  color: $user-color-text-main;
  font-weight: 700;
}

.rules-sheet-subtitle {
  font-size: 22rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.rules-sheet-close {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  background: rgba(148, 163, 184, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rules-sheet-close-icon {
  font-size: 34rpx;
  color: $user-color-text-sub;
  line-height: 1;
}

.rules-sheet-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 6rpx 30rpx 12rpx;
}

.rules-hero-card {
  padding: 24rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, #edf6ff 0%, #ffffff 100%);
  box-shadow: 0 14rpx 30rpx rgba(37, 99, 235, 0.08);
  margin-bottom: 20rpx;
}

.rules-hero-badge {
  display: inline-flex;
  min-height: 44rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  align-items: center;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-size: 22rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.rules-hero-title {
  display: block;
  font-size: 30rpx;
  line-height: 1.45;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.rules-hero-desc {
  display: block;
  font-size: 22rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
}

.rules-block {
  margin-bottom: 20rpx;
}

.rules-block-title {
  display: block;
  font-size: 26rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.rules-rate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.rules-rate-card,
.rules-example-item,
.rules-footnote {
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid rgba(220, 232, 248, 0.85);
  box-shadow: 0 10rpx 24rpx rgba(18, 56, 109, 0.05);
}

.rules-rate-card {
  padding: 22rpx 20rpx;
}

.rules-rate-card.accent {
  background: linear-gradient(145deg, #f7fbff 0%, #eef5ff 100%);
}

.rules-rate-name,
.rules-example-name,
.rules-footnote-title {
  display: block;
  font-size: 24rpx;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.rules-rate-price {
  display: block;
  font-size: 28rpx;
  color: $user-color-primary;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.rules-rate-desc,
.rules-example-value,
.rules-footnote-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.rules-example-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.rules-example-item {
  padding: 20rpx 22rpx;
}

.rules-footnote {
  padding: 22rpx;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
}

.rules-sheet-footer {
  padding: 18rpx 30rpx calc(22rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(220, 232, 248, 0.9);
}

.rules-sheet-primary-btn {
  width: 100%;
  @include user-primary-btn;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.payment-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.46);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 24rpx;
  box-sizing: border-box;
}

.payment-modal-content {
  width: 100%;
  max-width: 700rpx;
  min-height: 396rpx;
  background: #ffffff;
  border-radius: 36rpx;
  overflow: hidden;
  box-shadow: 0 28rpx 56rpx rgba(15, 23, 42, 0.22);
}

.payment-modal-header {
  padding: 42rpx 40rpx 16rpx;
}

.payment-modal-title {
  font-size: 34rpx;
  color: $user-color-text-main;
  font-weight: 700;
  text-align: center;
}

.payment-modal-body {
  padding: 20rpx 40rpx 42rpx;
}

.payment-modal-text {
  font-size: 28rpx;
  line-height: 1.8;
  color: $user-color-text-sub;
  text-align: center;
}

.payment-modal-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  padding: 0 40rpx 40rpx;
  align-items: stretch;
}

.payment-modal-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  border-radius: 26rpx;
  border: none;
  font-size: 30rpx;
  font-weight: 700;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.payment-modal-btn::after {
  border: none;
}

.success-btn {
  background: linear-gradient(135deg, #007aff 0%, #2563eb 100%);
  color: #ffffff;
}

.fail-btn {
  background: #ffffff;
  color: $user-color-primary;
  border: 1rpx solid rgba(0, 122, 255, 0.22);
}

@keyframes rulesOverlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes rulesSheetRiseUp {
  from {
    transform: translateY(32rpx);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>
