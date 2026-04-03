<template>
  <view class="order-confirm-page">
    <view class="confirm-hero-card">
      <view class="confirm-hero-top">
        <view class="confirm-hero-badge">最后一步</view>
        <view class="confirm-hero-status">待支付</view>
      </view>
      <text class="confirm-hero-title">确认本次陪诊服务订单</text>
      <text class="confirm-hero-subtitle">请核对预约信息与预付款金额，确认无误后完成支付。</text>
      <view class="confirm-hero-progress">
        <view
          v-for="step in flowSteps"
          :key="step.label"
          class="confirm-progress-item"
          :class="step.state"
        >
          <view class="confirm-progress-dot">
            <text v-if="step.state === 'done'" class="confirm-progress-check">✓</text>
          </view>
          <text class="confirm-progress-text">{{ step.label }}</text>
        </view>
      </view>
    </view>

    <!-- 加载指示器 -->
    <view v-if="isLoading" class="loading-container">
      <text>加载中...</text>
    </view>

    <view v-else class="confirm-page-content">
      <view class="confirm-summary-card">
        <view class="summary-card-head">
          <view>
            <text class="summary-card-eyebrow">订单摘要</text>
            <text class="summary-card-title">{{ orderData.serviceTypeName || '陪诊服务' }}</text>
          </view>
          <view class="summary-card-icon">
            <text class="summary-card-icon-text">✓</text>
          </view>
        </view>

        <view class="summary-focus-row">
          <view class="summary-focus-chip">
            <text class="summary-focus-label">就诊时间</text>
            <text class="summary-focus-value">{{ formatDate(orderData.serviceDate) }} {{ formatServiceTime(orderData.serviceTime) || '未知' }}</text>
          </view>
          <view class="summary-focus-chip light">
            <text class="summary-focus-label">医院</text>
            <text class="summary-focus-value">{{ orderData.hospital || '未知' }}</text>
          </view>
        </view>

        <view class="summary-detail-grid">
          <view class="summary-detail-item">
            <text class="summary-detail-label">就诊人</text>
            <text class="summary-detail-value">{{ orderData.patientName || '未知' }}</text>
          </view>
          <view class="summary-detail-item">
            <text class="summary-detail-label">订单编号</text>
            <text class="summary-detail-value mono">{{ orderData.orderNo || '未知' }}</text>
          </view>
        </view>

        <view class="summary-note-card">
          <text class="summary-note-title">症状描述</text>
          <text class="summary-note-text">{{ getSymptomDescription() }}</text>
        </view>

        <view class="summary-note-card muted">
          <text class="summary-note-title">其他需求</text>
          <text class="summary-note-text">{{ orderData.otherRequirement || '暂无补充要求' }}</text>
        </view>
      </view>


      <!-- 费用明细（预付款说明） -->
      <view class="card fee-detail-card">
        <view class="title fee-detail-head">
          <view class="section-title-stack">
            <text class="section-eyebrow">金额确认</text>
            <text class="section-title-main">费用明细</text>
          </view>
          <view class="rule-entry-pill" @click="showBillingRules = true">
            <text class="rule-entry-text">查看计费规则</text>
            <text class="rule-entry-arrow">›</text>
          </view>
        </view>
        <view class="fee-tip">
          <view class="fee-tip-tag">预付款说明</view>
          <view class="fee-tip-text">
            本次为服务预付款，服务结束后按实际时长结算，多退少补。
          </view>
        </view>
        <view class="fee-list-shell">
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
      </view>

      <!-- 支付方式 -->
      <view class="card payment-method-card">
        <view class="title payment-method-title">
          <view class="section-title-stack">
            <text class="section-eyebrow">支付方式</text>
            <text class="section-title-main">选择支付渠道</text>
          </view>
          <text class="payment-method-hint">可随时切换</text>
        </view>
        <view class="payment-method-list">
          <view
            v-for="method in paymentMethods"
            :key="method.value"
            class="payment-option-card"
            :class="{ selected: payMethod === method.value }"
            @click="selectPayMethod(method.value)"
          >
            <view class="payment-option-main">
              <view class="payment-option-icon" :class="method.value">
                <text class="payment-option-icon-text">{{ method.short }}</text>
              </view>
              <view class="payment-option-copy">
                <text class="payment-option-name">{{ method.label }}</text>
                <text class="payment-option-desc">{{ method.desc }}</text>
              </view>
            </view>
            <view class="payment-option-indicator" :class="{ selected: payMethod === method.value }">
              <view class="payment-option-indicator-inner"></view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部支付栏 -->
    <view v-if="!isLoading" class="footer">
      <view class="footer-price-block">
        <text class="footer-price-label">实付款</text>
        <view class="footer-price-line">
          <text class="footer-price-sign">¥</text>
          <text class="footer-price-value">{{ formatAmount(orderData.totalPrice) }}</text>
        </view>
        <text class="footer-price-hint">支付后锁定当前服务订单</text>
      </view>
      <button class="confirm-btn" @click="confirmPay">确认支付</button>
    </view>

    <!-- 计费规则底部抽屉 -->
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

    <!-- 支付弹窗 -->
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
import { get, post } from '@/utils/api.js';
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'

const orderData = ref({});
const payMethod = ref('wechat'); // 默认支付方式
const isLoading = ref(true);
const showBillingRules = ref(false); // 控制模态框显示
const showPaymentModal = ref(false); // 控制支付弹窗显示
const paymentMethods = [
  { value: 'wechat', label: '微信支付', desc: '推荐使用，支付体验更顺畅', short: '微' },
  { value: 'alipay', label: '支付宝支付', desc: '适合常用支付宝的用户', short: '支' },
  { value: 'unionpay', label: '银联支付', desc: '支持银行卡渠道支付', short: '银' }
]
const flowSteps = [
  { label: '选择服务', state: 'done' },
  { label: '填写需求', state: 'done' },
  { label: '提交预约', state: 'done' },
  { label: '确认支付', state: 'active' }
]

// 页面加载时获取 orderNo
const orderNo = ref('');

onLoad((options) => {
  if (redirectPublicSafeToHome()) return
  console.log('【OrderConfirmPage】页面加载参数:', options);
  if (options && options.orderNo) {
    orderNo.value = options.orderNo;
    console.log('【OrderConfirmPage】订单号:', orderNo.value);
    fetchOrderDetail(orderNo.value);
  } else {
    console.error('【OrderConfirmPage】缺少订单号参数');
    uni.showToast({
      title: '订单号错误',
      icon: 'none'
    });
    uni.redirectTo({
      url: '/pages/role-user/home'
    });
  }
});

/**
 * 调用后端接口获取订单详情
 * @param {string} orderNo - 订单号
 */
const fetchOrderDetail = async (orderNo) => {
  isLoading.value = true;
  try {
    console.log('【OrderConfirmPage】正在调用后端接口获取订单详情:', orderNo);
    const response = await get(`/ai/guide/orders/${orderNo}/complete-info`);

    console.log('【OrderConfirmPage】后端返回的订单数据:', response);

    if (response && response.code === 200 && response.data) {
      orderData.value = response.data;
    } else {
      console.warn('【OrderConfirmPage】后端返回数据为空或格式不正确:', response);
      uni.showToast({
        title: '获取订单信息失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('【OrderConfirmPage】获取订单详情失败:', error);
    uni.showToast({
      title: '网络错误，获取订单信息失败',
      icon: 'none'
    });
  } finally {
    isLoading.value = false;
  }
};

const selectPayMethod = (value) => {
  payMethod.value = value
}

/**
 * 确认支付 (触发弹窗)
 */
const confirmPay = () => {
  showPaymentModal.value = true;
};

/**
 * 处理支付结果
 */
const handlePaymentResult = async (isPaid) => {
  showPaymentModal.value = false;

  const paymentStatus = isPaid ? 1 : 0;

  try {
    // 无论成功或失败，都先通知后端
    const response = await post('/ai/guide/payments/status', {
      orderNo: orderNo.value,
      paymentStatus: paymentStatus
    });

    console.log('【OrderConfirmPage】支付状态更新响应:', response);

    if (isPaid) {
      uni.showToast({ title: '支付成功，跳转中...', icon: 'none' });
      setTimeout(() => {
        // 先重置到订单列表作为栈底，避免支付成功页返回到订单确认页
        const o = encodeURIComponent(orderNo.value)
        uni.reLaunch({
          url: '/pages/role-user/order',
          success() {
            setTimeout(() => {
              uni.navigateTo({
                url: `/subpkg/appointment-flow/05-payment-success-page?orderNo=${o}`
              })
            }, 150)
          }
        })
      }, 1000);
    } else {
      uni.showToast({ title: '支付未完成', icon: 'none' });
      setTimeout(() => {
        uni.navigateTo({
          url: `/subpkg/appointment-flow/06-payment-failed-page?orderNo=${encodeURIComponent(orderNo.value)}`
        });
      }, 1000);
    }
  } catch (error) {
    console.error('【调用支付状态接口失败】', error);
    // 即使接口报错，也根据用户点击的结果进行跳转
    if (isPaid) {
        const o = encodeURIComponent(orderNo.value)
        uni.reLaunch({
          url: '/pages/role-user/order',
          success() {
            setTimeout(() => {
              uni.navigateTo({
                url: `/subpkg/appointment-flow/05-payment-success-page?orderNo=${o}`
              })
            }, 150)
          }
        })
    } else {
        uni.navigateTo({
          url: `/subpkg/appointment-flow/06-payment-failed-page?orderNo=${encodeURIComponent(orderNo.value)}`
        });
    }
  }
};


/**
 * 格式化金额 (元)
 */
const formatAmount = (amount) => {
  const num = Number(amount);
  if (isNaN(num)) {
    return '0.00';
  }
  return num.toFixed(2);
};


/**
 * 获取症状描述文本
 */
const getSymptomDescription = () => {
  const { symptoms } = orderData.value;
  if (!symptoms) {
    return '未提供症状信息';
  }
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(s => s && s.trim() && s !== '无' && s !== 'null');
    return validSymptoms.length > 0 ? validSymptoms.join(', ') : '未提供症状信息';
  }
  return symptoms;
};

/**
 * 格式化日期
 */
const formatDate = (dateStr) => {
  if (!dateStr || typeof dateStr !== 'string') return '未知';
  return dateStr; // 后端已返回格式化好的日期
};

const formatServiceTime = (serviceTime) => {
  return formatServiceTimeSlot(serviceTime || '')
}

</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
/* 保持原有样式不变 */
.order-confirm-page {
  @include user-page;
  padding: 28rpx 24rpx calc(180rpx + env(safe-area-inset-bottom));
  font-size: 28rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

.loading-container {
  text-align: center;
  padding: 80rpx 20rpx;
  color: $user-color-text-sub;
}

.confirm-page-content {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.confirm-hero-card,
.confirm-summary-card,
.card {
  @include user-card(28rpx);
}

.confirm-hero-card {
  padding: 28rpx 28rpx 30rpx;
  background: linear-gradient(145deg, #ffffff 0%, #eff6ff 100%);
}

.confirm-hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.confirm-hero-badge,
.confirm-hero-status {
  min-height: 46rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
}

.confirm-hero-badge {
  background: rgba(0, 122, 255, 0.10);
  color: $user-color-primary;
}

.confirm-hero-status {
  background: rgba(250, 173, 20, 0.14);
  color: #d97706;
}

.confirm-hero-title {
  display: block;
  font-size: 42rpx;
  line-height: 1.18;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.confirm-hero-subtitle {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
  margin-bottom: 26rpx;
}

.confirm-hero-progress {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.confirm-progress-item {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  min-height: 52rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(220, 232, 248, 0.95);
}

.confirm-progress-item.done {
  background: rgba(82, 196, 26, 0.10);
  border-color: rgba(82, 196, 26, 0.18);
}

.confirm-progress-item.active {
  background: rgba(0, 122, 255, 0.12);
  border-color: rgba(0, 122, 255, 0.22);
}

.confirm-progress-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-progress-item.done .confirm-progress-dot {
  background: #52c41a;
}

.confirm-progress-item.active .confirm-progress-dot {
  background: $user-color-primary;
}

.confirm-progress-check {
  color: #ffffff;
  font-size: 18rpx;
  line-height: 1;
}

.confirm-progress-text {
  font-size: 22rpx;
  color: $user-color-text-main;
  font-weight: 600;
}

.summary-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.summary-card-eyebrow {
  display: block;
  font-size: 22rpx;
  line-height: 1;
  color: $user-color-primary;
  font-weight: 700;
  letter-spacing: 2rpx;
  margin-bottom: 12rpx;
}

.summary-card-title {
  display: block;
  font-size: 36rpx;
  line-height: 1.18;
  color: $user-color-text-main;
  font-weight: 700;
}

.summary-card-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.12) 0%, rgba(37, 99, 235, 0.18) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-card-icon-text {
  font-size: 34rpx;
  color: $user-color-primary;
  font-weight: 700;
}

.summary-focus-row {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.summary-focus-chip {
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #edf6ff 0%, #f7fbff 100%);
  border: 1rpx solid rgba(214, 234, 255, 0.95);
}

.summary-focus-chip.light {
  background: linear-gradient(145deg, #ffffff 0%, #f7fbff 100%);
}

.summary-focus-label,
.summary-detail-label,
.summary-note-title,
.section-eyebrow {
  display: block;
  font-size: 22rpx;
  line-height: 1.2;
  color: $user-color-text-sub;
  margin-bottom: 10rpx;
}

.summary-focus-value,
.summary-detail-value {
  display: block;
  font-size: 28rpx;
  line-height: 1.55;
  color: $user-color-text-main;
  font-weight: 600;
  word-break: break-word;
}

.summary-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.summary-detail-item,
.summary-note-card,
.fee-list-shell,
.payment-option-card {
  border-radius: 22rpx;
  background: #ffffff;
  border: 1rpx solid rgba(220, 232, 248, 0.9);
  box-shadow: 0 10rpx 22rpx rgba(18, 56, 109, 0.05);
}

.summary-detail-item {
  padding: 20rpx;
}

.summary-detail-value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 24rpx;
}

.summary-note-card {
  padding: 20rpx 22rpx;
  margin-bottom: 14rpx;
}

.summary-note-card.muted {
  margin-bottom: 0;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
}

.summary-note-text {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-main;
}

.section-title-stack {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.section-title-main {
  font-size: 32rpx;
  line-height: 1.18;
  color: $user-color-text-main;
  font-weight: 700;
}

.fee-detail-head,
.payment-method-title {
  margin-bottom: 22rpx;
  gap: 20rpx;
}

.fee-list-shell {
  padding: 8rpx 22rpx;
}

.label,
.value {
  color: inherit;
}

.fee-item,
.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 0;
  font-size: 24rpx;
  color: $user-color-text-main;
  border-bottom: 1rpx solid rgba(220, 232, 248, 0.8);
}

.fee-item:last-of-type {
  border-bottom: none;
}

.discount {
  color: $user-color-primary;
  font-weight: 500;
}

.total {
  padding-bottom: 8rpx;
  font-weight: 700;
  border-bottom: none;
  color: $user-color-primary;
}

.total-price {
  font-size: 36rpx;
}

.fee-tip {
  margin-bottom: 16rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #f0f7ff;
  border: 1rpx solid #d6eaff;
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.fee-tip-tag {
  font-size: 22rpx;
  color: #007AFF;
  font-weight: 600;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: #e6f3ff;
  flex-shrink: 0;
}

.fee-tip-text {
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.6;
}

.fee-detail-head {
  align-items: flex-start;
}

.payment-method-hint {
  font-size: 22rpx;
  color: $user-color-text-sub;
}

.payment-method-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.payment-option-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 20rpx 22rpx;
}

.payment-option-card.selected {
  border-color: rgba(0, 122, 255, 0.34);
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.06) 0%, rgba(37, 99, 235, 0.10) 100%);
  box-shadow: 0 14rpx 28rpx rgba(0, 122, 255, 0.12);
}

.payment-option-main {
  display: flex;
  align-items: center;
  gap: 18rpx;
  flex: 1;
}

.payment-option-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(0, 122, 255, 0.10);
}

.payment-option-icon.alipay {
  background: rgba(37, 99, 235, 0.10);
}

.payment-option-icon.unionpay {
  background: rgba(22, 50, 79, 0.08);
}

.payment-option-icon-text {
  font-size: 28rpx;
  font-weight: 700;
  color: $user-color-primary;
}

.payment-option-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  min-width: 0;
}

.payment-option-name {
  font-size: 28rpx;
  color: $user-color-text-main;
  font-weight: 600;
}

.payment-option-desc {
  font-size: 22rpx;
  color: $user-color-text-sub;
  line-height: 1.5;
}

.payment-option-indicator {
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(148, 163, 184, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.payment-option-indicator.selected {
  border-color: $user-color-primary;
}

.payment-option-indicator-inner {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: transparent;
}

.payment-option-indicator.selected .payment-option-indicator-inner {
  background: $user-color-primary;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  border-top: 1rpx solid rgba(220, 232, 248, 0.9);
  position: sticky;
  bottom: 0;
  z-index: 20;
  margin-top: 28rpx;
  backdrop-filter: blur(18rpx);
}

.footer-price-block {
  min-width: 0;
}

.footer-price-label {
  display: block;
  font-size: 22rpx;
  color: $user-color-text-sub;
  margin-bottom: 8rpx;
}

.footer-price-line {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
  margin-bottom: 6rpx;
}

.footer-price-sign {
  font-size: 24rpx;
  color: $user-color-primary;
  font-weight: 700;
}

.footer-price-value {
  font-size: 42rpx;
  line-height: 1;
  color: $user-color-primary;
  font-weight: 700;
}

.footer-price-hint {
  display: block;
  font-size: 20rpx;
  color: #8aa0b8;
}

.price {
  color: $user-color-text-main;
  font-weight: 600;
}

.confirm-btn {
  min-width: 230rpx;
  height: 88rpx;
  background: linear-gradient(135deg, #007aff 0%, #2563eb 100%);
  color: white;
  border-radius: 999rpx;
  font-size: 28rpx;
  line-height: 88rpx;
  font-weight: 700;
  border: none;
  box-shadow: 0 16rpx 30rpx rgba(37, 99, 235, 0.24);
}

.rule-entry-pill {
  margin-left: auto;
  min-height: 58rpx;
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
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
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
  line-height: 1;
  color: #2563eb;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.rules-sheet-title {
  font-size: 40rpx;
  line-height: 1.12;
  color: #0f172a;
  font-weight: 700;
}

.rules-sheet-subtitle {
  font-size: 24rpx;
  line-height: 1.65;
  color: #475569;
}

.rules-sheet-close {
  width: 64rpx;
  height: 64rpx;
  border-radius: 999rpx;
  background: rgba(148, 163, 184, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rules-sheet-close-icon {
  font-size: 40rpx;
  color: #64748b;
  line-height: 1;
}

.rules-sheet-body {
  flex: 1;
  padding: 4rpx 30rpx 16rpx;
  overflow-y: auto;
  box-sizing: border-box;
  scrollbar-width: none;
}

.rules-sheet-body::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.rules-hero-card {
  padding: 26rpx 24rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.10) 0%, rgba(14, 165, 233, 0.08) 100%);
  border: 1rpx solid rgba(147, 197, 253, 0.5);
  margin-bottom: 24rpx;
  box-sizing: border-box;
}

.rules-hero-badge {
  display: inline-flex;
  min-height: 42rpx;
  padding: 0 16rpx;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.68);
  color: #1d4ed8;
  font-size: 22rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
}

.rules-hero-title {
  display: block;
  font-size: 30rpx;
  line-height: 1.45;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12rpx;
}

.rules-hero-desc {
  display: block;
  font-size: 24rpx;
  color: #475569;
  line-height: 1.7;
}

.rules-block {
  margin-bottom: 24rpx;
}

.rules-block-title {
  display: block;
  font-size: 28rpx;
  line-height: 1.3;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 16rpx;
}

.rules-rate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.rules-rate-card {
  padding: 22rpx 20rpx;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 8rpx 20rpx rgba(148, 163, 184, 0.08);
  box-sizing: border-box;
}

.rules-rate-card.accent {
  background: linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  border-color: rgba(147, 197, 253, 0.55);
}

.rules-rate-name {
  display: block;
  font-size: 26rpx;
  line-height: 1.3;
  color: #0f172a;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.rules-rate-price {
  display: block;
  font-size: 26rpx;
  line-height: 1.2;
  color: #2563eb;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.rules-rate-desc {
  display: block;
  font-size: 24rpx;
  color: #475569;
  line-height: 1.65;
}

.rules-example-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.rules-example-item {
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: #ffffff;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 6rpx 18rpx rgba(148, 163, 184, 0.06);
}

.rules-example-name {
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
  color: #0f172a;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.rules-example-value {
  display: block;
  font-size: 24rpx;
  line-height: 1.6;
  color: #2563eb;
}

.rules-footnote {
  padding: 22rpx 22rpx 26rpx;
  border-radius: 22rpx;
  background: rgba(248, 250, 252, 0.92);
  border: 1rpx solid rgba(226, 232, 240, 0.75);
  margin-bottom: 10rpx;
}

.rules-footnote-title {
  display: block;
  font-size: 24rpx;
  line-height: 1.3;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.rules-footnote-text {
  display: block;
  font-size: 24rpx;
  line-height: 1.65;
  color: #6b7280;
}

.rules-sheet-footer {
  padding: 18rpx 30rpx calc(22rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(226, 232, 240, 0.9);
  display: flex;
  justify-content: center;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16rpx);
}

.rules-sheet-primary-btn {
  width: 100%;
  min-height: 88rpx;
  padding: 0 32rpx;
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 999rpx;
  font-size: 28rpx;
  line-height: 88rpx;
  box-shadow: 0 16rpx 32rpx rgba(37, 99, 235, 0.24);
}

@keyframes rulesOverlayFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes rulesSheetRiseUp {
  from {
    opacity: 0;
    transform: translateY(48rpx) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 760px) {
  .rules-rate-grid {
    grid-template-columns: 1fr;
  }
}

.payment-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.44);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20rpx;
}

.payment-modal-content {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 30rpx;
  width: 100%;
  max-width: 560rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24rpx 48rpx rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.payment-modal-header {
  padding: 28rpx 24rpx 20rpx;
  text-align: center;
}

.payment-modal-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $user-color-text-main;
}

.payment-modal-body {
  padding: 10rpx 28rpx 34rpx;
  text-align: center;
}

.payment-modal-text {
  font-size: 28rpx;
  color: $user-color-text-sub;
  line-height: 1.65;
}

.payment-modal-footer {
  display: flex;
  gap: 18rpx;
  padding: 20rpx 24rpx 26rpx;
  border-top: 1rpx solid rgba(220, 232, 248, 0.85);
}

.payment-modal-btn {
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  flex: 1;
}

.success-btn {
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 12rpx 24rpx rgba(37, 99, 235, 0.2);
}

.fail-btn {
  background: #ffffff;
  color: #ef4444;
  border: 1rpx solid rgba(239, 68, 68, 0.22);
}

@media (max-width: 680px) {
  .summary-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
