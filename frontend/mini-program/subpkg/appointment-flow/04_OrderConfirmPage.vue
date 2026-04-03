<template>
  <view class="order-confirm-page">

    <!-- 进度条 -->
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

    <!-- 加载指示器 -->
    <view v-if="isLoading" class="loading-container">
      <text>加载中...</text>
    </view>

    <!-- 订单信息 -->
    <view v-else class="card order-info">
      <view class="title">
        <text class="iconfont icon-info"> 订单信息</text>
      </view>
      <view class="info-item">
        <text class="label"><text class="iconfont icon-hospital"> 就诊医院</text></text>
        <text class="value">{{ orderData.hospital || '未知' }}</text>
      </view>
      <!-- 合并就诊日期和时间 -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-calendar"> 就诊时间</text></text>
        <text class="value">{{ formatDate(orderData.serviceDate) }} {{ orderData.serviceTime || '未知' }}</text>
      </view>
      <view class="info-item">
        <text class="label"><text class="iconfont icon-user"> 就诊人</text></text>
        <text class="value">{{ orderData.patientName || '未知' }}</text>
      </view>
      <!-- 显示后端返回的 symptoms 和 otherRequirement -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-note"> 症状描述</text></text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <!-- 新增：其他需求 -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-note"> 其他需求</text></text>
        <text class="value">{{ orderData.otherRequirement || '无' }}</text>
      </view>

    </view>


    <!-- 费用明细（预付款说明） -->
    <view v-if="!isLoading" class="card fee-detail">
      <view class="title">
        <text class="iconfont icon-list"> 费用明细</text>
        <!-- 新增：右上角的小字“查看计费规则”按钮 -->
        <button class="rule-link-button" @click="showBillingRules = true">查看计费规则</button>
      </view>
      <view class="fee-tip">
        <view class="fee-tip-tag">预付款说明</view>
        <view class="fee-tip-text">
          本次为服务预付款，服务结束后按实际时长结算，多退少补。
        </view>
      </view>
      <view class="fee-item">
        陪诊服务预付款
        <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
      <view class="fee-item">优惠券
        <text class="discount">-¥0.00</text>
      </view>
      <view class="total">
        预付款合计
        <text class="total-price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view v-if="!isLoading" class="card payment-method">
      <view class="title">
        <text class="iconfont icon-pay"> 选择支付方式</text>
      </view>
      <radio-group @change="onPaymentChange" :value="payMethod">
        <view class="payment-option">
          <radio value="wechat" :checked="payMethod === 'wechat'" />
          <view class="pay-icon">
            <text class="iconfont icon-wechat"> 微信支付</text>
          </view>
        </view>
        <view class="payment-option">
          <radio value="alipay" :checked="payMethod === 'alipay'" />
          <view class="pay-icon">
            <text class="iconfont icon-alipay"> 支付宝支付</text>
          </view>
        </view>
        <view class="payment-option">
          <radio value="unionpay" :checked="payMethod === 'unionpay'" />
          <view class="pay-icon">
            <text class="iconfont icon-unionpay"> 银联支付</text>
          </view>
        </view>
      </radio-group>
    </view>

    <!-- 底部支付栏 -->
    <view v-if="!isLoading" class="footer">
      <view class="real-price">实付款：
        <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
      <button class="confirm-btn" @click="confirmPay">确认支付</button>
    </view>

    <!-- 计费规则模态框 -->
    <view v-if="showBillingRules" class="modal-overlay" @click="showBillingRules = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <view class="modal-header-left">
            <text class="modal-title">计费规则</text>
            <text class="modal-subtitle">按实际服务时长结算，多退少补</text>
          </view>
          <text class="close-btn" @click="showBillingRules = false">×</text>
        </view>
        <scroll-view class="modal-body" scroll-y="true">
          <view class="rules-card">
            <view class="rules-banner">
              <text class="rules-banner-title">费用如何计算？</text>
              <text class="rules-banner-desc">下单时先支付预付款，服务结束后按实际时长结算，多退少补。</text>
            </view>

            <view class="rules-section">
              <view class="rules-section-title">服务类型与单价</view>

              <view class="rule-item">
                <view class="rule-dot"></view>
                <view class="rule-main">
                  <text class="rule-name">普通陪诊</text>
                  <text class="rule-desc">起步价 ¥50（含 2 小时），超出部分 ¥30/小时；不足 1 小时按 1 小时计。</text>
                </view>
              </view>

              <view class="rule-item">
                <view class="rule-dot"></view>
                <view class="rule-main">
                  <text class="rule-name">术后护理</text>
                  <text class="rule-desc">¥45/小时，不足 1 小时按 1 小时计。</text>
                </view>
              </view>

              <view class="rule-item">
                <view class="rule-dot"></view>
                <view class="rule-main">
                  <text class="rule-name">急诊陪同</text>
                  <text class="rule-desc">在普通陪诊基础上加收加急费 ¥100。</text>
                </view>
              </view>

              <view class="rule-item">
                <view class="rule-dot"></view>
                <view class="rule-main">
                  <text class="rule-name">上门陪诊</text>
                  <text class="rule-desc">在普通陪诊基础上加收上门费 ¥30。</text>
                </view>
              </view>
            </view>

            <view class="rules-section">
              <view class="rules-section-title">示例（便于理解）</view>
              <view class="example-item">
                <text class="example-name">普通陪诊 3 小时</text>
                <text class="example-desc">¥50 + ¥30 × 1 = ¥80</text>
              </view>
              <view class="example-item">
                <text class="example-name">急诊陪同 3 小时</text>
                <text class="example-desc">普通陪诊 ¥80 + 加急费 ¥100 = ¥180</text>
              </view>
              <view class="example-item">
                <text class="example-name">上门陪诊 3 小时</text>
                <text class="example-desc">普通陪诊 ¥80 + 上门费 ¥30 = ¥110</text>
              </view>
              <view class="example-item">
                <text class="example-name">术后护理 1.5 小时</text>
                <text class="example-desc">按 2 小时计：¥45 × 2 = ¥90</text>
              </view>
            </view>

            <view class="rules-note">
              <text class="note-title">温馨提示</text>
              <text class="note-desc">若实际服务时长与预估不一致，将在服务结束后进行差额补付或自动退款。</text>
            </view>
          </view>
        </scroll-view>
        <view class="modal-footer">
          <button class="close-modal-btn" @click="showBillingRules = false">我知道了</button>
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
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post, config } from '@/utils/api.js';
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

const orderData = ref({});
const showInvoice = ref(false);
const payMethod = ref('wechat'); // 默认支付方式
const isLoading = ref(true);
const showBillingRules = ref(false); // 控制模态框显示
const showPaymentModal = ref(false); // 控制支付弹窗显示

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

/**
 * 处理支付方式变更
 */
const onPaymentChange = (e) => {
  payMethod.value = e.detail.value;
};

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
                url: `/subpkg/appointment-flow/05_PaymentSuccessPage?orderNo=${o}`
              })
            }, 150)
          }
        })
      }, 1000);
    } else {
      uni.showToast({ title: '支付未完成', icon: 'none' });
      setTimeout(() => {
        uni.navigateTo({
          url: `/subpkg/appointment-flow/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
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
                url: `/subpkg/appointment-flow/05_PaymentSuccessPage?orderNo=${o}`
              })
            }, 150)
          }
        })
    } else {
        uni.navigateTo({
          url: `/subpkg/appointment-flow/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
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

</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
/* 保持原有样式不变 */
.order-confirm-page {
  background-color: #f5f7fa;
  padding: 40rpx;
  font-size: 28rpx;
  min-height: 100vh;
}

.loading-container {
  text-align: center;
  padding: 40rpx;
  color: #999;
}

.back-btn {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  color: #333;
  font-size: 32rpx;
}

.step-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40px;
  padding: 0 20rpx;
  position: relative;
  align-items: center;
}

.step-dot {
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  border-radius: 50%;
  color: white;
  font-size: 24rpx;
  text-align: center;
  margin-bottom: 10rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
}

.step-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 28rpx;
  color: #333;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 10%;
  margin: 0 10px -25px 10px;
  text-align: center;
  font-size: 28rpx;
}

.step-item.completed .step-dot {
  background-color: #4caf50;
  color: white;
}

.step-item.active .step-dot {
  background-color: #007AFF;
  color: white;
}

.step-item.completed .step-text {
  color: #4caf50;
}

.step-item.active .step-text {
  color: #007AFF;
}

.progress-line {
  width: 60rpx;
  height: 2rpx;
  background-color: #ddd;
  margin: 8px 10rpx;
  flex-shrink: 0;
}

.progress-line.green {
  background-color: #4caf50;
}

.card {
  background-color: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 40px;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.title {
  font-weight: bold;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title .iconfont {
  margin-right: 10rpx;
  color: #007AFF;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  font-size: 24rpx;
  color: #666;
  font-weight: 800;
}

.value {
  color: #333;
  font-weight: 500;
  font-size: 24rpx;
  text-align: right;
  max-width: 60%;
  word-break: break-all;
}

.fee-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
  font-size: 24rpx;
}

.price {
  color: #333;
  font-weight: 500;
}

.discount {
  color: #007AFF;
  font-weight: 500;
}

.total {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  font-weight: bold;
  color: #007AFF;
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

.payment-option {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eee;
}

.pay-icon {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 24rpx;
}

.pay-icon .iconfont {
  margin-right: 10rpx;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background-color: white;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  margin-top: 20rpx;
  position: sticky;
  bottom: 0;
  z-index: 10;
}

.real-price {
  font-size: 28rpx;
  color: #333;
}

.price {
  color: #007AFF;
  font-weight: bold;
  font-size: 32rpx;
}

.confirm-btn {
  width: 200rpx;
  height: 60rpx;
  background-color: #007AFF;
  color: white;
  border-radius: 30rpx;
  font-size: 28rpx;
  line-height: 60rpx;
  margin-right: -5px;
  border: none;
}

.rule-link-button {
  display: inline-block;
  margin-left: auto;
  padding: 0;
  border: none;
  background: none;
  color: #007AFF;
  font-size: 24rpx;
  text-decoration: underline;
  cursor: pointer;
  line-height: 1;
  margin-top: -10rpx;
  margin-right: 10rpx;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 32rpx;
  box-sizing: border-box;
}

.modal-content {
  background-color: white;
  border-radius: 28rpx;
  width: calc(100vw - 64rpx);
  max-width: 720rpx;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  overflow: hidden;
  box-sizing: border-box;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx 22rpx;
  border-bottom: 1rpx solid #eee;
  background-color: #f8f9fa;
}

.modal-header-left {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-subtitle {
  font-size: 24rpx;
  color: #666;
}

.close-btn {
  font-size: 40rpx;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0 10rpx;
}

.modal-body {
  flex: 1;
  padding: 28rpx 30rpx 24rpx;
  max-height: 60vh;
  overflow-y: auto;
  font-size: 24rpx;
  line-height: 1.5;
  color: #333;
  box-sizing: border-box;
}

.rule-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.rules-card {
  width: 100%;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  box-sizing: border-box;
}

.rules-banner {
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: #f0f7ff;
  border: 1rpx solid #d6eaff;
  margin-bottom: 22rpx;
}

.rules-banner-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #007AFF;
  margin-bottom: 8rpx;
}

.rules-banner-desc {
  display: block;
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.6;
}

.rules-section {
  margin-top: 18rpx;
}

.rules-section-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #333;
  margin: 6rpx 0 14rpx;
}

.rule-item {
  display: flex;
  gap: 14rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.rule-item:last-child {
  border-bottom: none;
}

.rule-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #007AFF;
  margin-top: 10rpx;
  flex-shrink: 0;
}

.rule-main {
  flex: 1;
  min-width: 0;
}

.rule-name {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #111827;
  margin-bottom: 6rpx;
}

.rule-desc {
  display: block;
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.6;
}

.example-item {
  padding: 12rpx 0;
  border-bottom: 1rpx dashed #eef2f7;
}

.example-item:last-child {
  border-bottom: none;
}

.example-name {
  display: block;
  font-size: 24rpx;
  color: #111827;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.example-desc {
  display: block;
  font-size: 24rpx;
  color: #4b5563;
}

.rules-note {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f0f0f0;
}

.note-title {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8rpx;
}

.note-desc {
  display: block;
  font-size: 24rpx;
  color: #6b7280;
  line-height: 1.6;
}

.modal-footer {
  padding: 24rpx 30rpx 28rpx;
  border-top: 1rpx solid #eee;
  display: flex;
  justify-content: center;
  box-sizing: border-box;
}

.close-modal-btn {
  min-width: 220rpx;
  padding: 12rpx 32rpx;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 999rpx;
  font-size: 28rpx;
  cursor: pointer;
}

.payment-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20rpx;
}

.payment-modal-content {
  background-color: white;
  border-radius: 16rpx;
  width: 90%;
  max-width: 500rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.payment-modal-header {
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
  background-color: #f8f9fa;
  text-align: center;
}

.payment-modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.payment-modal-body {
  padding: 40rpx 20rpx;
  text-align: center;
}

.payment-modal-text {
  font-size: 28rpx;
  color: #333;
}

.payment-modal-footer {
  display: flex;
  justify-content: space-around;
  padding: 20rpx;
  border-top: 1rpx solid #eee;
}

.payment-modal-btn {
  padding: 16rpx 30rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  cursor: pointer;
  border: none;
  flex: 1;
  margin: 0 10rpx;
}

.success-btn {
  background-color: #4caf50;
  color: white;
}

.fail-btn {
  background-color: #ff5252;
  color: white;
}
</style>
