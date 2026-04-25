<template>
  <view class="container">


    <!-- 订单号 + 状态 -->
    <view class="order-info">
      <view class="header-row">
        <text class="label">订单号</text>
        <view class="status-tag" :class="getStatusClass(order)">
          {{ getOrderStatusText(order) }}
        </view>
      </view>
      <text class="order-number">{{ order.orderNo }}</text>
    </view>

    <!-- 待支付倒计时 -->
    <view v-if="showPayCountdown" class="pay-countdown">
      <text class="countdown-text">
        请在
        <text class="time">{{ payCountdown }}</text>
        内完成支付，超时订单将自动取消
      </text>
    </view>

    <!-- 专属派单待确认倒计时 -->
    <view v-if="showAssignedCountdown" class="assigned-countdown">
      <text class="countdown-text">
        专属派单待确认，剩余
        <text class="time">{{ assignedCountdown }}</text>
        ，超时后将自动进入公共派单
      </text>
    </view>

    <!-- 服务进度条（与陪诊师端四步保持一致） -->
    <view
      v-if="order.orderStatus === 3 || order.orderStatus === 4 || order.orderStatus === 6"
      class="service-progress-card"
    >
      <view class="service-progress-title">服务进度</view>
      <view class="service-progress-bar">
        <view
          class="progress-step"
          :class="{ active: currentServiceProgressStep >= 1, current: currentServiceProgressStep === 1 }"
        >
          <view class="step-dot"></view>
          <text class="step-label">{{ userServiceFlowSteps[0].label }}</text>
        </view>
        <view class="progress-line" :class="{ active: currentServiceProgressStep >= 2 }"></view>
        <view
          class="progress-step"
          :class="{ active: currentServiceProgressStep >= 2, current: currentServiceProgressStep === 2 }"
        >
          <view class="step-dot"></view>
          <text class="step-label">{{ userServiceFlowSteps[1].label }}</text>
        </view>
        <view class="progress-line" :class="{ active: currentServiceProgressStep >= 3 }"></view>
        <view
          class="progress-step"
          :class="{ active: currentServiceProgressStep >= 3, current: currentServiceProgressStep === 3 }"
        >
          <view class="step-dot"></view>
          <text class="step-label">{{ userServiceFlowSteps[2].label }}</text>
        </view>
        <view class="progress-line" :class="{ active: currentServiceProgressStep >= 4 }"></view>
        <view
          class="progress-step"
          :class="{ active: currentServiceProgressStep >= 4, current: currentServiceProgressStep === 4 }"
        >
          <view class="step-dot"></view>
          <text class="step-label">{{ userServiceFlowSteps[3].label }}</text>
        </view>
      </view>
    </view>

    <!-- 服务信息 -->
    <view class="service-info">
      <view class="info-item">
        <text class="label">服务类型：</text>
        <text class="value">{{ order.serviceTypeName || getServiceTypeName(order.clinicType) }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务时间：</text>
        <text class="value">{{ order.serviceDate }} {{ formatServiceTimeSlot(order.serviceTime || '') }}</text>
      </view>
      <view v-if="order.actualDuration != null" class="info-item">
        <text class="label">实际服务时长：</text>
        <text class="value">{{ getActualDurationDisplay(order) }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务医院：</text>
        <text class="value">{{ order.hospital }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人：</text>
        <text class="value">{{ order.patientName }}</text>
      </view>
      <view class="info-item">
        <text class="label">联系电话：</text>
        <text class="value">{{ order.patientPhone || order.contactPhone }}</text>
      </view>
      <!-- 新增：症状描述 -->
      <view class="info-item">
        <text class="label">症状描述：</text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <!-- 新增：其他需求 -->
      <view class="info-item">
        <text class="label">其他需求：</text>
        <text class="value">{{ order.otherRequirement || '无' }}</text>
      </view>
    </view>

    <!-- 陪诊师信息 -->
    <view class="companion-info companion-info-clickable" v-if="order.attendantName && order.attendantName !== '待分配陪诊师'" @click="openAttendantDetail">
      <image
        :src="orderAttendantAvatar"
        class="avatar"
        mode="aspectFill"
        @error="handleImageError"
      ></image>
      <view class="companion-details">
        <text class="name">{{ order.attendantName }}</text>
        <view class="phone" v-if="order.attendantPhone">
          <image src="/static/contact.png" class="phone-icon"></image>
          <text class="phone-number">{{ order.attendantPhone }}</text>
        </view>
        <view class="rating">
          <text class="star">⭐</text>
          <text class="score">{{ formatRatingScore(order.attendantScore, order.attendantEvaluationCount) }}</text>
        </view>
      </view>
      <text class="companion-entry-arrow">›</text>
    </view>
    <view class="companion-info" v-else-if="(order.orderStatus === 0 || order.orderStatus === 1) && order.paymentStatus === 1">
      <view class="companion-details">
        <text class="name">待分配陪诊师</text>
        <text class="desc">系统正在为您匹配合适的陪诊师...</text>
        <text class="desc release-hint" v-if="order.orderStatus === 1 && order.cancelReason">原陪诊师取消原因：{{ order.cancelReason }}</text>
      </view>
    </view>
    <view class="companion-info" v-else-if="order.paymentStatus === 0 && order.orderStatus !== 7">
      <view class="companion-details">
        <text class="name">待支付</text>
        <text class="desc">请尽快完成支付以确认订单</text>
      </view>
    </view>
    <view class="companion-info" v-else-if="timeoutClosedOrder">
      <view class="companion-details">
        <text class="name">订单已超时关闭</text>
        <text class="desc timeout-close-text">{{ getTimeoutClosedMessage(order) }}</text>
      </view>
    </view>
    <view class="companion-info" v-else-if="order.orderStatus === 7">
      <view class="companion-details">
        <text class="name">订单已取消</text>
        <text class="desc">此订单已取消，无法分配陪诊师</text>
      </view>
    </view>

    <!-- 费用信息 -->
    <view class="payment-info">
      <view class="payment-row">
        <text class="label">订单金额</text>
        <text class="price">¥{{ formatAmount(finalAmount) }}</text>
      </view>
      <view class="payment-sub-row">
        <text class="sub-label">预付款</text>
        <text class="sub-value">¥{{ formatAmount(prepayAmount) }}</text>
      </view>
      <view class="payment-sub-row" v-if="hasDiff">
        <text class="sub-label">{{ diffLabel }}</text>
        <text class="sub-value">¥{{ formatAmount(Math.abs(diffAmount)) }}</text>
      </view>
      <view class="payment-row" v-if="order.priceCalculation">
        <text class="label">费用说明</text>
        <text class="value">{{ order.priceCalculation }}</text>
      </view>
    </view>

    <!-- 取消信息 -->
    <view class="payment-info" v-if="timeoutClosedOrder">
      <text class="timeout-close-message">{{ getTimeoutClosedMessage(order) }}</text>
    </view>
    <view class="payment-info" v-else-if="order.orderStatus === 7">
      <view class="payment-row">
        <text class="label">取消原因</text>
        <text class="value">{{ order.cancelReason || '未填写' }}</text>
      </view>
      <view class="payment-row">
        <text class="label">取消方</text>
        <text class="value">{{ getCancelByText(order.cancelBy) }}</text>
      </view>
      <view class="payment-row">
        <text class="label">取消时间</text>
        <text class="value">{{ formatOrderDateTime(order.cancelTime) || '未知' }}</text>
      </view>
      <view class="payment-row">
        <text class="label">违约金</text>
        <text class="price">¥{{ formatAmount(order.penaltyAmount) }}</text>
      </view>
      <view class="payment-row">
        <text class="label">您的退款金额</text>
        <text class="price">¥{{ formatAmount(order.refundAmount) }}</text>
      </view>
    </view>

    <!-- 二维码区域 -->
    <view class="qr-section" v-if="showQRCode">
      <view class="qr-title">服务确认二维码</view>
      <view class="qr-container">
        <image
          v-if="!qrLoadFailed"
          :src="currentQrCodeUrl"
          class="qr-image"
          mode="aspectFit"
          @click="previewQrCode"
          @error="handleQrError"
        ></image>
        <view v-else class="qr-error-box">
          <text class="qr-error-text">加载失败，点击重试</text>
          <button class="qr-retry-btn" @click="retryQrCode">重新加载</button>
        </view>
        <text v-if="!qrLoadFailed" class="qr-preview-hint" @click="previewQrCode">点按放大预览</text>
        <br>
        <text class="qr-desc">请陪诊师扫描此二维码确认开始服务</text>
      </view>
    </view>

    <!-- 服务时长确认弹窗（三种状态：需补付 / 自动退款 / 无差异） -->
    <view class="duration-modal-overlay" v-if="showDurationConfirm">
      <view class="duration-modal">
        <view class="duration-confirm-section">
          <view class="confirm-title">服务时长确认</view>
          <view class="duration-card">
            <view class="row">
              <text class="label">预计时长</text>
              <text class="value">
                {{ getEstimatedDurationDisplay(order) }}（{{ order.serviceTypeName || getServiceTypeName(order.clinicType) }})
              </text>
            </view>
            <view class="row">
              <text class="label">实际时长</text>
              <text class="value highlight">
                {{ getActualDurationDisplay(order) }}（陪诊师提交）
              </text>
            </view>
            <view class="row">
              <text class="label">费用差异</text>
              <text
                class="value diff"
                :class="{
                  needPay: order.balanceAmount && order.balanceAmount > 0,
                  autoRefund: order.balanceAmount && order.balanceAmount < 0
                }"
              >
                {{ diffDesc }}
              </text>
            </view>
            <view class="rule-text">
              <text class="rule-main-line">
                计费规则：不足半小时按半小时计，不足一小时按一小时计；普通陪诊起步价50元（含2小时），超出部分30元/小时。
              </text>
              <view class="rule-extra-tip">
                <text class="rule-extra-tag">预付款说明</text>
                <text class="rule-extra-text">
                  前期支付为预付款，服务结束后按实际时长结算，多退少补；如需退款，将自动退款（原路退回），预计 24 小时内到账。
                </text>
              </view>
            </view>
          </view>

          <!-- 差额支付方式（仅需补付时显示） -->
          <view
            v-if="order.balanceAmount && order.balanceAmount > 0"
            class="duration-pay-section"
          >
            <view class="pay-section-header">
              <text class="pay-title">支付差额</text>
              <text class="pay-amount">¥{{ formatAmount(order.balanceAmount) }}</text>
            </view>
            <view class="pay-section-subtitle">请选择支付方式完成本次补付</view>
            <radio-group
              class="pay-options"
              @change="onBalancePayMethodChange"
              :value="balancePayMethod"
            >
              <view class="pay-option">
                <radio value="wechat" :checked="balancePayMethod === 'wechat'" />
                <view class="pay-option-text">
                  <text class="pay-option-main">微信支付</text>
                  <text class="pay-option-sub">推荐，优先使用微信钱包</text>
                </view>
              </view>
              <view class="pay-option">
                <radio value="alipay" :checked="balancePayMethod === 'alipay'" />
                <view class="pay-option-text">
                  <text class="pay-option-main">支付宝</text>
                  <text class="pay-option-sub">使用支付宝账户付款</text>
                </view>
              </view>
            </radio-group>
          </view>

          <view class="btn-group">
            <button class="disagree-btn" @click="openDisputeModal">不认可，申诉</button>
            <button class="confirm-btn" @click="confirmDuration">
              {{ confirmBtnText }}
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- 补付结果模拟弹窗 -->
    <view v-if="showBalancePayResultModal" class="payment-modal-overlay" @click="showBalancePayResultModal = false">
      <view class="payment-modal-content" @click.stop>
        <view class="payment-modal-header">
          <text class="payment-modal-title">补付结果确认</text>
        </view>
        <view class="payment-modal-body">
          <text class="payment-modal-text">请确认本次补付的支付结果：</text>
        </view>
        <view class="payment-modal-footer">
          <button class="payment-modal-btn success-btn" @click="handleBalancePayResult(true)">已支付</button>
          <button class="payment-modal-btn fail-btn" @click="handleBalancePayResult(false)">未支付</button>
        </view>
      </view>
    </view>

    <!-- 联系陪诊师方式选择弹窗（参考补付弹窗风格） -->
    <view v-if="showContactModal" class="contact-modal-overlay" @click="showContactModal = false">
      <view class="contact-modal" @click.stop>
        <view class="contact-header">
          <view class="contact-title-wrap">
            <text class="contact-title">联系陪诊师</text>
            <text class="contact-subtitle">请选择联系方式</text>
          </view>
          <text class="contact-close" @click="showContactModal = false">×</text>
        </view>

        <view class="contact-options">
          <view class="contact-option" @click="handleContactChoice('phone')">
            <view class="contact-icon phone">☎</view>
            <view class="contact-text">
              <text class="contact-main">拨打电话</text>
              <text class="contact-desc">{{ order.attendantPhone || '未提供电话' }}</text>
            </view>
            <text class="contact-arrow">›</text>
          </view>

          <view class="contact-option" @click="handleContactChoice('chat')">
            <view class="contact-icon chat">💬</view>
            <view class="contact-text">
              <text class="contact-main">在线联系</text>
              <text class="contact-desc">进入聊天界面与陪诊师沟通</text>
            </view>
            <text class="contact-arrow">›</text>
          </view>
        </view>

        <view class="contact-footer">
          <button class="contact-cancel-btn" @click="showContactModal = false">取消</button>
        </view>
      </view>
    </view>

    <!-- 我的评价（已完成且已评价时展示，含陪诊师回复） -->
    <view v-if="order.orderStatus === 6 && userEvaluation" class="evaluation-section">
      <view class="evaluation-title">我的评价</view>
      <view class="evaluation-rating-row">
        <view class="evaluation-stars">
          <text v-for="i in 5" :key="i" class="eval-star" :class="{ filled: i <= (userEvaluation.rating || 0) }">★</text>
        </view>
        <text class="eval-rating-num">{{ (userEvaluation.rating || 0).toFixed(1) }}</text>
      </view>
      <view v-if="userEvaluation.content" class="evaluation-content">{{ userEvaluation.content }}</view>
      <view v-if="userEvaluation.tags" class="evaluation-tags">
        <text v-for="(tag, i) in (userEvaluation.tags || '').split(',')" :key="i" class="eval-tag" v-show="tag">{{ tag.trim() }}</text>
      </view>
      <view class="attendant-reply-block" :class="{ hasReply: userEvaluation.attendantReply }">
        <text class="reply-label">陪诊师回复：</text>
        <text class="reply-text">{{ userEvaluation.attendantReply || '暂未回复' }}</text>
      </view>
    </view>

    <!-- 服务记录（时间轴样式） -->
    <view class="record-section">
      <view class="record-title">服务记录</view>
      <view class="record-list">
        <view
          v-for="(item, index) in serviceSteps"
          :key="index"
          :class="['step-item', { 'step-item-last': index === serviceSteps.length - 1 }]"
        >
          <view class="step-axis">
            <view class="step-dot"></view>
            <view
              class="step-line"
              v-if="index !== serviceSteps.length - 1"
            ></view>
          </view>
          <view class="step-body">
            <view class="step-header">
              <text class="step-title">{{ item.title }}</text>
              <text class="step-time">{{ item.time || '--' }}</text>
            </view>
            <text class="step-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部固定导航栏：左侧辅助操作 + 右侧主操作按钮 -->
    <view class="bottom-nav">
      <view class="bottom-left">
        <view class="nav-item" @click="callCompanion" v-if="order.attendantPhone">
          <image src="/static/contact.png" class="nav-icon"></image>
          <text>联系</text>
        </view>
        <view class="nav-item" @click="consult">
          <image src="/static/advisory.png" class="nav-icon"></image>
          <text>咨询</text>
        </view>
        <view class="nav-item" @click="share">
          <image src="/static/share.png" class="nav-icon"></image>
          <text>分享</text>
        </view>
        <view
          class="nav-item cancel-nav-item"
          v-if="order.paymentStatus === 0 && order.orderStatus !== 7"
          @click="openCancelModal"
        >
          <image src="/static/icons/emergency.png" class="nav-icon"></image>
          <text>取消</text>
        </view>
      </view>
      <view class="bottom-right" v-if="primaryActionText">
        <button class="primary-action-btn" @click="handlePrimaryAction">
          {{ primaryActionText }}
        </button>
      </view>
    </view>
  </view>

  <!-- 不认可申诉弹窗 -->
  <view class="dispute-modal-overlay" v-if="showDisputeModal">
    <view class="dispute-modal">
      <view class="dispute-title">不认可本次时长</view>
      <view class="dispute-row">
        <text class="dispute-label">我认为的实际时长（小时）</text>
        <input
          class="dispute-input"
          v-model="disputeDuration"
          placeholder="例如：3 或 3.5"
          type="digit"
        />
      </view>
      <view class="dispute-row">
        <text class="dispute-label">说明</text>
        <textarea
          class="dispute-textarea"
          v-model="disputeReason"
          placeholder="请说明不认可的原因，便于平台介入处理"
        />
      </view>
      <view class="dispute-actions">
        <button class="dispute-cancel" @click="showDisputeModal = false">取消</button>
        <button class="dispute-submit" @click="submitDispute">提交申诉</button>
      </view>
    </view>
  </view>

  <!-- 取消订单弹窗 -->
  <view v-if="showCancelModal" class="dispute-modal-overlay">
    <view class="dispute-modal">
      <view class="dispute-title">取消订单</view>
      <view class="dispute-row">
        <text class="dispute-label">请选择取消原因</text>
        <view class="cancel-reason-list">
          <view
            v-for="reason in cancelReasons"
            :key="reason"
            class="cancel-reason-item"
            :class="{ active: selectedCancelReason === reason }"
            @click="selectedCancelReason = reason"
          >
            {{ reason }}
          </view>
        </view>
      </view>
      <view class="dispute-row">
        <text class="dispute-label">补充说明（选填）</text>
        <textarea
          class="dispute-textarea"
          v-model="cancelRemark"
          placeholder="如有其他情况，可在此简单说明"
        />
      </view>
      <view class="dispute-actions">
        <button class="dispute-cancel" @click="showCancelModal = false">再想想</button>
        <button class="dispute-submit" @click="submitCancelOrder">确认取消</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app';
import { get, post, put, config } from '@/utils/api.js';
import { addOrderListener, removeOrderListener, connectOrderSocket, isOrderSocketOpen } from '@/utils/order-websocket.js';
import { makePhoneCallWithGuard } from '@/subpkg/common/runtime.js';
import { userPlaceholder } from '@/utils/assets.js';
import { navigateToAttendantDetail } from '@/utils/attendant-detail.js';
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { formatRatingScore } from '@/utils/rating.js'
import { formatOrderDateTime, formatServiceTimeSlot, getOrderDurationLabel } from '@/utils/order-display.js'
import { getTimeoutClosedMessage, isTimeoutClosedOrder, TIMEOUT_CLOSE_STATUS_TEXT } from '@/utils/order-timeout.js'

// 使用 ref 定义响应式变量
const order = ref({});
const userEvaluation = ref(null); // 我的评价（含陪诊师回复）
const balancePayMethod = ref('wechat'); // 差额支付方式，默认微信
const showBalancePayResultModal = ref(false); // 补付结果弹窗
const showContactModal = ref(false); // 联系陪诊师方式弹窗
let pollTimer = null; // 订单实时同步轮询定时器
let payCountdownTimer = null; // 待支付倒计时定时器
let assignedCountdownTimer = null; // 专属派单待确认倒计时定时器
let socketRefreshTimer = null; // 消息刷新防抖定时器
let currentOrderKey = ''; // 当前订单号（优先）
let isFetchingOrder = false; // 防止并发请求
let queuedOrderKey = ''; // 并发请求期间记录下一次刷新目标
let pageActive = false; // 页面可见态
const REALTIME_SYNC_INTERVAL = 30000;

const payCountdown = ref('');
const assignedCountdown = ref('');
const qrLoadFailed = ref(false);
const qrRefreshToken = ref(Date.now());
const currentQrSourceKey = ref('');
const showCancelModal = ref(false);
const cancelReasons = [
  '计划有变，暂不就诊',
  '信息填写有误，重新下单',
  '价格原因，暂不接受',
  '通过其他渠道已就诊',
  '其他原因'
];
const selectedCancelReason = ref(cancelReasons[0]);
const cancelRemark = ref('');

const showPayCountdown = computed(() => {
  return (
    order.value &&
    order.value.paymentStatus === 0 &&
    order.value.orderStatus !== 7
  );
});

const showAssignedCountdown = computed(() => {
  return (
    order.value &&
    Number(order.value.paymentStatus) === 1 &&
    Number(order.value.orderStatus) === 8
  );
});

const timeoutClosedOrder = computed(() => isTimeoutClosedOrder(order.value));

// 计算属性
const showQRCode = computed(() => {
  return order.value.orderStatus === 2
    && order.value.paymentStatus === 1
    && !!order.value.orderId
    && !!order.value.attendantId;
});

const currentQrCodeUrl = computed(() => getQrCodeUrl(order.value?.qrCodeUrl, order.value?.orderId));

const showDurationConfirm = computed(() => {
  return order.value.orderStatus === 4; // 待确认时长费用
});

const diffDesc = computed(() => {
  if (!order.value || order.value.balanceAmount == null) {
    return '无费用差异';
  }
  const b = Number(order.value.balanceAmount);
  if (b === 0) return '无费用差异';
  if (b > 0) return `需补付¥${formatAmount(b)}`;
  return `自动退款¥${formatAmount(Math.abs(b))}`;
});

const confirmBtnText = computed(() => {
  if (!order.value || order.value.balanceAmount == null) {
    return '确认时长，完成订单';
  }
  const b = Number(order.value.balanceAmount);
  if (b === 0) return '确认时长，完成订单';
  if (b > 0) return `确认并补付¥${formatAmount(b)}`;
  // 自动退款场景：按钮文案尽量简短，避免在小屏幕被截断
  return '确认并自动退款';
});

// 是否已评价（前端本地标记，后端接入后可替换）
const hasEvaluated = computed(() => {
  if (!order.value || !order.value.orderNo) return false;
  const key = `order_evaluated_${order.value.orderNo}`;
  return uni.getStorageSync(key) === '1';
});

// 底部主操作按钮文案：根据订单状态和支付状态动态切换
const primaryActionText = computed(() => {
  if (!order.value || order.value.orderStatus === undefined || order.value.orderStatus === null) {
    return '';
  }
  const status = order.value.orderStatus;
  const paymentStatus = order.value.paymentStatus;

  // 待支付：引导去支付确认页
  if (paymentStatus === 0 && status !== 7) {
    return '去支付';
  }

  // 待接单：允许用户主动取消订单
  if (status === 1) {
    return '取消订单';
  }

  // 服务中：引导查看服务进度
  if (status === 3) {
    return '查看服务进度';
  }

  // 时长费用有争议：查看申诉进度
  if (status === 5) {
    return '查看申诉进度';
  }

  if (status === 9) {
    return '支付差额';
  }

  // 已完成：优先评价，评价后展示再次下单
  if (status === 6) {
    return hasEvaluated.value ? '再次下单' : '去评价';
  }

  // 已取消：支持再次下单
  if (status === 7) {
    return '再次下单';
  }

  // 其他状态（待接单/待服务/待确认等）底部不再额外给主按钮
  return '';
});

// 差额支付方式切换
const onBalancePayMethodChange = (e) => {
  balancePayMethod.value = e.detail.value;
};

// 订单金额拆分：预付款 + 差额（补付/退款） + 最终金额
const finalAmount = computed(() => {
  if (!order.value) return 0;
  const base = order.value.totalPrice || order.value.orderAmount;
  return Number(base || 0);
});

const diffAmount = computed(() => {
  if (!order.value || order.value.balanceAmount == null) return 0;
  return Number(order.value.balanceAmount || 0);
});

const hasDiff = computed(() => {
  return diffAmount.value !== 0;
});

const prepayAmount = computed(() => {
  // 若有差额，则预付款 = 最终金额 - 差额；否则预付款 = 当前金额
  if (!order.value) return 0;
  if (order.value.balanceAmount != null && diffAmount.value !== 0) {
    return finalAmount.value - diffAmount.value;
  }
  return finalAmount.value;
});

const diffLabel = computed(() => {
  if (!hasDiff.value) return '无差额';
  if (diffAmount.value > 0) return '实际补付';
  return '实际退款';
});

// 服务进度（用户端进度条，来源于后端 serviceProgressStep）
const userServiceFlowSteps = [
  { key: 'arrived', label: '已到院' },
  { key: 'waiting', label: '候诊中' },
  { key: 'exam', label: '检查中' },
  { key: 'finished', label: '就诊完成' }
];

const currentServiceProgressStep = computed(() => {
  // 后端字段：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成
  const step = order.value.serviceProgressStep;
  if (!step || step < 1) return 1;
  if (step > 4) return 4;
  return step;
});

// 启动或更新待支付倒计时
const setupPayCountdown = () => {
  // 清理旧定时器
  if (payCountdownTimer) {
    clearInterval(payCountdownTimer);
    payCountdownTimer = null;
  }

  if (!showPayCountdown.value) {
    payCountdown.value = '';
    return;
  }

  const createTimeStr = order.value.createTime || order.value.orderDate;
  if (!createTimeStr) {
    payCountdown.value = '';
    return;
  }

  // 兼容 iOS 的日期解析
  const baseTime = Date.parse(createTimeStr.replace(/-/g, '/'));
  if (isNaN(baseTime)) {
    payCountdown.value = '';
    return;
  }

  const deadline = baseTime + 15 * 60 * 1000; // 下单后15分钟

  const tick = async () => {
    const now = Date.now();
    const diff = deadline - now;

    if (diff <= 0) {
      payCountdown.value = '00:00';
      if (payCountdownTimer) {
        clearInterval(payCountdownTimer);
        payCountdownTimer = null;
      }

      // 超时仍未支付且未取消，则自动取消订单
      if (order.value && order.value.paymentStatus === 0 && order.value.orderStatus !== 7) {
        try {
          // 同样使用 query 参数方式传递取消原因
          await put(`/api/orders/${order.value.orderId}/cancel?reason=${encodeURIComponent('超时未支付自动取消')}`);
          uni.showToast({ title: '超时未支付，订单已自动取消', icon: 'none' });
          await fetchOrderDetail(order.value.orderNo);
        } catch (e) {
          console.error('自动取消超时未支付订单失败:', e);
        }
      }
      return;
    }

    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    const mm = minutes.toString().padStart(2, '0');
    const ss = seconds.toString().padStart(2, '0');
    payCountdown.value = `${mm}:${ss}`;
  };

  tick();
  payCountdownTimer = setInterval(tick, 1000);
};

const setupAssignedCountdown = () => {
  if (assignedCountdownTimer) {
    clearInterval(assignedCountdownTimer);
    assignedCountdownTimer = null;
  }

  if (!showAssignedCountdown.value) {
    assignedCountdown.value = '';
    return;
  }

  const paymentTimeStr = order.value.paymentTime || order.value.createTime;
  if (!paymentTimeStr) {
    assignedCountdown.value = '';
    return;
  }

  const baseTime = Date.parse(String(paymentTimeStr).replace(/-/g, '/'));
  if (Number.isNaN(baseTime)) {
    assignedCountdown.value = '';
    return;
  }

  const deadline = baseTime + 15 * 60 * 1000;

  const tick = () => {
    const diff = deadline - Date.now();
    if (diff <= 0) {
      assignedCountdown.value = '00:00';
      if (assignedCountdownTimer) {
        clearInterval(assignedCountdownTimer);
        assignedCountdownTimer = null;
      }
      return;
    }

    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    assignedCountdown.value = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  };

  tick();
  assignedCountdownTimer = setInterval(tick, 1000);
};

// 服务记录 (根据订单状态动态生成)
const serviceSteps = computed(() => {
  if (!order.value || !order.value.orderNo) return [];

  const steps = [];
  const status = Number(order.value.orderStatus);
  const paymentStatus = order.value.paymentStatus;

  steps.push({
    title: '订单创建',
    desc: '您已成功提交订单',
    time: formatOrderDateTime(order.value.createTime || order.value.orderDate)
  });

  // 如果订单已取消，单独处理，避免展示未发生的流程
  if (status === 7) {
    if (paymentStatus === 1) {
      steps.push({ title: '已支付', desc: '订单费用已支付', time: formatOrderDateTime(order.value.paymentTime) });
    }
    steps.push({
      title: '订单取消',
      desc: order.value.cancelReason || '订单已被取消',
      time: formatOrderDateTime(order.value.cancelTime)
    });
    return steps;
  }

  if (paymentStatus === 1) {
    steps.push({
      title: '已支付',
      desc: '订单费用已支付',
      time: formatOrderDateTime(order.value.paymentTime)
    });
  }

  const acceptTime = formatOrderDateTime(order.value.acceptTime || order.value.paymentTime || order.value.createTime);
  const startTime = formatOrderDateTime(order.value.serviceStartTime || order.value.acceptTime || order.value.paymentTime || order.value.createTime);
  const endTime = formatOrderDateTime(order.value.serviceEndTime || order.value.updateTime || order.value.serviceStartTime);

  switch (status) {
    case 0:
      break;
    case 1:
      steps.push({
        title: '待接单',
        desc: '订单已进入接单大厅，等待陪诊师接单',
        time: acceptTime
      });
      break;
    case 8:
      steps.push({
        title: '专属派单待确认',
        desc: '已优先派给指定陪诊师，等待其确认接单',
        time: acceptTime
      });
      break;
    case 2:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      break;
    case 3:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      steps.push({
        title: '服务开始',
        desc: '陪诊师已开始服务',
        time: startTime
      });
      break;
    case 4:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      steps.push({
        title: '服务开始',
        desc: '陪诊师已开始服务',
        time: startTime
      });
      steps.push({ title: '服务结束', desc: '陪诊师已结束服务', time: endTime });
      steps.push({ title: '待确认时长', desc: '请确认实际服务时长与费用', time: endTime });
      break;
    case 5:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      steps.push({
        title: '服务开始',
        desc: '陪诊师已开始服务',
        time: startTime
      });
      steps.push({ title: '服务结束', desc: '陪诊师已结束服务', time: endTime });
      steps.push({ title: '待确认时长', desc: '请确认实际服务时长与费用', time: endTime });
      steps.push({ title: '争议处理中', desc: '平台正在处理时长费用申诉', time: formatOrderDateTime(order.value.updateTime || order.value.serviceEndTime) });
      break;
    case 9:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      steps.push({
        title: '服务开始',
        desc: '陪诊师已开始服务',
        time: startTime
      });
      steps.push({ title: '服务结束', desc: '陪诊师已结束服务', time: endTime });
      steps.push({ title: '待补差额', desc: '请完成平台核定的差额支付', time: formatOrderDateTime(order.value.updateTime || order.value.serviceEndTime) });
      break;
    case 6:
      steps.push({
        title: '陪诊师已接单',
        desc: '陪诊师已接单，准备为您服务',
        time: acceptTime
      });
      steps.push({
        title: '服务开始',
        desc: '陪诊师已开始服务',
        time: startTime
      });
      steps.push({ title: '服务结束', desc: '陪诊师已结束服务', time: endTime });
      steps.push({ title: '待确认时长', desc: '请确认实际服务时长与费用', time: endTime });
      steps.push({ title: '订单完成', desc: '订单已完成', time: formatOrderDateTime(order.value.updateTime || order.value.serviceEndTime) });
      break;
    default:
      break;
  }

  return steps;
});

// 状态文本映射
const getOrderStatusText = (order) => {
  if (order?.orderStatusDesc) {
    return order.orderStatusDesc;
  }
  if (isTimeoutClosedOrder(order)) {
    return TIMEOUT_CLOSE_STATUS_TEXT;
  }
  if (!order) return '未知状态';

  if (order.paymentStatus === 0 && order.orderStatus !== 7) {
    return '待支付';
  }

  const statusMap = {
    0: '待接单',
    1: '待接单',
    8: '专属派单待确认',
    2: '待服务', // 修改：将“已接单”改为“待服务”
    3: '服务中',
    4: '待确认时长',
    5: '待支付差价',
    6: '已完成',
    7: '已取消'
  };
  return statusMap[order.orderStatus] || '未知状态';
};

const getStatusClass = (order) => {
  if (!order) return '';

  if (order.paymentStatus === 0 && order.orderStatus !== 7) {
    return 'status-deposit';
  }

  const classMap = {
    0: 'status-waiting',
    1: 'status-waiting',
    8: 'status-waiting',
    2: 'status-accepted',
    3: 'status-service',
    4: 'status-confirm',
    5: 'status-balance',
    6: 'status-completed',
    7: 'status-cancelled'
  };
  return classMap[order.orderStatus] || 'status-default';
};

// 服务类型映射
const getServiceTypeName = (type) => {
  const typeMap = {
    1: '普通陪诊',
    2: '术后护理',
    3: '急诊陪同',
    4: '上门陪诊'
  };
  return typeMap[type] || '未知类型';
};

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return '0.00';
  return Number(amount).toFixed(2);
};

const getCancelByText = (cancelBy) => {
  if (cancelBy === 1) return '陪诊师';
  if (cancelBy === 0) return '用户';
  return '系统';
};

const openCancelModal = () => {
  if (!order.value) return;
  const status = order.value.orderStatus;
  // 仅允许待支付/待接单/待服务阶段取消，服务中及之后不允许
  if (status >= 3) {
    return;
  }
  showCancelModal.value = true;
};

const submitCancelOrder = async () => {
  if (!order.value || !order.value.orderId) {
    uni.showToast({ title: '订单信息有误', icon: 'none' });
    return;
  }

  const reasonText = selectedCancelReason.value || '用户主动取消';
  const fullReason = cancelRemark.value
    ? `${reasonText}（${cancelRemark.value}）`
    : reasonText;

  try {
    uni.showLoading({ title: '正在取消...', mask: true });
    // 后端使用 @RequestParam 接收 reason，这里改为 query 参数传递
    await put(`/api/orders/${order.value.orderId}/cancel?reason=${encodeURIComponent(fullReason)}`);
    uni.hideLoading();
    uni.showToast({ title: '订单已取消', icon: 'none' });
    showCancelModal.value = false;
    await fetchOrderDetail(order.value.orderNo);
  } catch (e) {
    uni.hideLoading();
    console.error('取消订单失败:', e);
    uni.showToast({ title: '取消失败，请稍后重试', icon: 'none' });
  }
};

const orderAttendantAvatar = computed(() =>
  resolveAvatarUrl(order.value?.attendantAvatar || '', userPlaceholder)
);

const getQrCodeUrl = (qrCodeUrl, orderId) => {
  const base = config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL;
  let finalUrl = '';
  if (qrCodeUrl) {
    if (qrCodeUrl.startsWith('http')) {
      finalUrl = qrCodeUrl;
    } else {
      const path = qrCodeUrl.startsWith('/') ? qrCodeUrl : '/' + qrCodeUrl;
      finalUrl = base + path;
    }
  } else if (orderId) {
    finalUrl = `${base}/order-qr/${orderId}.png`;
  }
  if (!finalUrl) return '';
  const joiner = finalUrl.includes('?') ? '&' : '?';
  return `${finalUrl}${joiner}t=${qrRefreshToken.value}`;
};

const getQrSourceKey = (detail = {}) => {
  if (!detail?.orderId) return '';
  return `${detail.orderId}|${detail.qrCodeUrl || ''}|${detail.orderStatus || ''}|${detail.paymentStatus || ''}|${detail.attendantId || ''}`;
};

const previewQrCode = () => {
  if (qrLoadFailed.value) {
    uni.showToast({ title: '二维码加载失败，请先重试', icon: 'none' });
    return;
  }
  const qrUrl = getQrCodeUrl(order.value?.qrCodeUrl, order.value?.orderId);
  if (!qrUrl) {
    uni.showToast({ title: '二维码暂未生成', icon: 'none' });
    return;
  }
  uni.previewImage({
    urls: [qrUrl],
    current: qrUrl
  });
};

const handleQrError = () => {
  qrLoadFailed.value = true;
};

const retryQrCode = () => {
  qrLoadFailed.value = false;
  qrRefreshToken.value = Date.now();
};

// 图片加载错误处理
const handleImageError = (e) => {
  console.error('头像加载失败:', e);
};

// 返回订单列表
const returnToOrders = () => {
  uni.reLaunch({
    url: '/pages/role-user/order',
  });
};

// 联系陪诊师：弹出自定义弹窗，让用户选择拨打电话或在线联系
const callCompanion = () => {
  if (!order.value) {
    uni.showToast({ title: '订单信息有误', icon: 'none' });
    return;
  }

  const phone = order.value.attendantPhone;
  const targetId = order.value.attendantId;

  if (!phone && !targetId) {
    uni.showToast({ title: '当前暂无陪诊师联系方式', icon: 'none' });
    return;
  }

  showContactModal.value = true;
};

const openAttendantDetail = () => {
  navigateToAttendantDetail(order.value);
};

// 处理联系陪诊师方式选择
const handleContactChoice = (type) => {
  showContactModal.value = false;
  if (!order.value) return;

  const phone = order.value.attendantPhone;
  const targetId = order.value.attendantId;
  const targetName = order.value.attendantName || '陪诊师';

  if (type === 'phone') {
    if (phone) {
      makePhoneCallWithGuard(phone);
    } else {
      uni.showToast({ title: '暂未提供陪诊师电话', icon: 'none' });
    }
  } else if (type === 'chat') {
    if (targetId) {
      uni.navigateTo({
        url: `/subpkg/chat/chat?userId=${encodeURIComponent(targetId)}&name=${encodeURIComponent(targetName)}&avatar=${encodeURIComponent(order.value.attendantAvatar || '')}`
      });
    } else {
      uni.showToast({ title: '暂未提供在线联系方式', icon: 'none' });
    }
  }
};

// 咨询
const consult = () => {
  uni.showToast({
    title: '暂未开放咨询功能',
  });
};

// 分享
const share = () => {
  uni.showToast({
    title: '暂未开放分享功能',
  });
};

// 申诉弹窗
const showDisputeModal = ref(false);
const disputeDuration = ref('');
const disputeReason = ref('');

// 评价
const evaluate = () => {
  uni.showToast({
    title: '暂未开放评价功能',
  });
};

const formatDuration = (val) => {
  if (val == null) return '--';
  const num = Number(val);
  if (Number.isNaN(num)) return '--';
  return num.toFixed(1).replace(/\.0$/, '');
};

const getActualDurationDisplay = (detailOrder) => {
  if (!detailOrder || detailOrder.actualDuration == null) return '—';
  return `${formatDuration(detailOrder.actualDuration)}小时`;
};

const getEstimatedDurationDisplay = (detailOrder) => {
  const label = getOrderDurationLabel(detailOrder, '—');
  return label === '—' ? label : label;
};

// 打开申诉弹窗
const openDisputeModal = () => {
  disputeDuration.value = formatDuration(order.value.actualDuration);
  disputeReason.value = '';
  showDisputeModal.value = true;
};

// 提交申诉
const submitDispute = async () => {
  try {
    const dur = disputeDuration.value ? Number(disputeDuration.value) : null;
    if (Number.isNaN(dur) || dur <= 0) {
      uni.showToast({ title: '请输入有效时长', icon: 'none' });
      return;
    }
    if (!String(disputeReason.value || '').trim()) {
      uni.showToast({ title: '请填写申诉原因', icon: 'none' });
      return;
    }
    const query = [];
    query.push(`userDuration=${dur}`);
    query.push(`reason=${encodeURIComponent(disputeReason.value.trim())}`);
    const qs = query.length ? `?${query.join('&')}` : '';
    const response = await post(`/api/orders/${order.value.orderId}/dispute-time-fee${qs}`);
    if (response && response.code === 200) {
      uni.showToast({ title: '申诉已提交', icon: 'success' });
      showDisputeModal.value = false;
      await fetchOrderDetail(order.value.orderNo);
    } else {
      uni.showToast({ title: response.message || '提交申诉失败', icon: 'none' });
    }
  } catch (error) {
    console.error('提交申诉失败:', error);
    uni.showToast({ title: '提交申诉失败', icon: 'none' });
  }
};

// 实际调用后端确认时长与费用
const doConfirmTimeAndFee = async () => {
  try {
    uni.showLoading({ title: '提交中...' });
    const response = await post(`/api/orders/${order.value.orderId}/confirm-time-fee`);
    uni.hideLoading();
    if (response && response.code === 200) {
      uni.showToast({ title: '确认成功', icon: 'success' });
      await fetchOrderDetail(order.value.orderNo);
    } else {
      uni.showToast({ title: response.message || '确认失败', icon: 'none' });
    }
  } catch (error) {
    uni.hideLoading();
    console.error('确认时长失败:', error);
    uni.showToast({ title: '确认失败', icon: 'none' });
  }
};

// 确认服务时长与费用：有补付时先弹出补付结果选择
const confirmDuration = async () => {
  if (!order.value) return;
  await doConfirmTimeAndFee();
  if (Number(order.value.orderStatus) === 9 && Number(order.value.balanceAmount || 0) > 0) {
    showBalancePayResultModal.value = true;
  }
};

// 处理补付结果：已支付 -> 调用补差额支付接口；未支付 -> 跳转支付失败页
const handleBalancePayResult = async (isPaid) => {
  showBalancePayResultModal.value = false;
  if (!order.value) return;
  if (isPaid) {
    await payBalance();
  } else {
    uni.navigateTo({
      url: `/subpkg/appointment-flow/06-payment-failed-page?orderNo=${encodeURIComponent(order.value.orderNo)}&scene=balance`
    });
  }
};

// 底部主操作按钮行为
const handlePrimaryAction = () => {
  if (!order.value) return;
  const status = order.value.orderStatus;
  const paymentStatus = order.value.paymentStatus;
  const orderNo = order.value.orderNo;

  // 待接单：主按钮执行取消订单逻辑（弹出原因选择弹窗）
  if (status === 1) {
    openCancelModal();
    return;
  }

  // 待支付：回到订单确认页
  if (paymentStatus === 0 && status !== 7) {
    if (orderNo) {
      uni.navigateTo({
        url: `/subpkg/appointment-flow/04-order-confirm-page?orderNo=${encodeURIComponent(orderNo)}`
      });
    }
    return;
  }

  // 服务中：滚动到服务进度区域
  if (status === 3) {
    uni.pageScrollTo({
      selector: '.service-progress-card',
      duration: 300
    });
    return;
  }

  // 时长费用有争议：滚动到服务记录
  if (status === 5) {
    uni.pageScrollTo({
      selector: '.record-section',
      duration: 300
    });
    return;
  }

  if (status === 9) {
    showBalancePayResultModal.value = true;
    return;
  }

  // 已完成：去评价或再次下单
  if (status === 6) {
    if (!hasEvaluated.value) {
      if (orderNo) {
        uni.navigateTo({
          url: `/subpkg/evaluate/evaluate?orderNo=${encodeURIComponent(orderNo)}`
        });
      }
    } else {
      uni.navigateTo({
        url: '/subpkg/appointment-flow/01-appointment-selection'
      });
    }
    return;
  }

  // 已取消：再次下单
  if (status === 7) {
    uni.navigateTo({
      url: '/subpkg/appointment-flow/01-appointment-selection'
    });
  }
};

// 支付差价
const payBalance = async () => {
  try {
    const response = await post(`/api/orders/${order.value.orderId}/pay-balance`);
    
    if (response && response.code === 200) {
      uni.showToast({ title: '支付成功', icon: 'success' });
      await fetchOrderDetail(order.value.orderNo);
    } else {
      uni.showToast({ title: response.message || response.data || '支付失败', icon: 'none' });
    }
  } catch (error) {
    console.error('支付差价失败:', error);
    uni.showToast({ title: '支付失败', icon: 'none' });
  }
};

/**
 * 获取症状描述文本
 */
const getSymptomDescription = () => {
  const preferred = String(order.value.symptomDescription || '').trim();
  if (preferred && preferred !== '无' && preferred !== 'null') {
    return preferred;
  }
  const { symptoms } = order.value;
  if (!symptoms) {
    return '无';
  }
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(s => s && s.trim() && s !== '无' && s !== 'null');
    return validSymptoms.length > 0 ? validSymptoms.join('，') : '无';
  }
  return symptoms;
};

// 加载用户评价（含陪诊师回复）
const loadUserEvaluation = async (orderId) => {
  if (!orderId) return;
  try {
    const res = await get(`/api/orders/${orderId}/evaluation`);
    if (res && res.code === 200 && res.data) {
      userEvaluation.value = res.data;
    } else {
      userEvaluation.value = null;
    }
  } catch (e) {
    console.error('加载评价失败', e);
    userEvaluation.value = null;
  }
};

const stopRealtimeSync = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

const shouldRealtimeSync = (orderData) => {
  if (!orderData) return false;
  const status = Number(orderData.orderStatus);
  // 仅终态（已完成/已取消）停止自动同步，其余状态保持实时更新
  return status !== 6 && status !== 7;
};

const startRealtimeSync = (orderKey) => {
  if (!orderKey) return;
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(() => {
    if (!pageActive || isFetchingOrder || isOrderSocketOpen()) return;
    fetchOrderDetail(orderKey);
  }, REALTIME_SYNC_INTERVAL);
};

const updateRealtimeSyncState = (orderKey, orderData = order.value) => {
  if (orderKey) currentOrderKey = orderKey;
  if (!shouldRealtimeSync(orderData)) {
    stopRealtimeSync();
    return;
  }
  startRealtimeSync(currentOrderKey || orderData?.orderNo || orderData?.orderId);
};

const scheduleOrderRefresh = (delay = 700) => {
  if (!pageActive) return;
  const key = currentOrderKey || order.value?.orderNo || order.value?.orderId;
  if (!key) return;
  if (socketRefreshTimer) clearTimeout(socketRefreshTimer);
  socketRefreshTimer = setTimeout(() => {
    fetchOrderDetail(key);
  }, delay);
};

// 获取订单详情（同时兼容用户端和陪诊师端）
const fetchOrderDetail = async (orderKey) => {
  if (!orderKey) return;

  if (isFetchingOrder) {
    queuedOrderKey = orderKey;
    return;
  }

  isFetchingOrder = true;
  try {
    console.log('正在获取订单详情，订单标识:', orderKey);

    let data = null;
    const isNumericOrderId = /^\d+$/.test(String(orderKey || ''));

    // 1. 先尝试用户端统一详情接口（根据订单号）
    try {
      const res = await get(`/ai/guide/orders/${orderKey}/complete-info`);
      console.log('订单详情响应(用户端接口):', res);
      if (res && res.code === 200 && res.data) {
        data = res.data;
      }
    } catch (e) {
      console.warn('用户端订单详情接口调用失败，尝试陪诊师端接口:', e);
    }

    // 2. 如果按订单号查不到，再尝试陪诊师端接口（根据订单 ID）
    if (!data && isNumericOrderId) {
      try {
        const byUserOrderId = await get(`/api/orders/${orderKey}`);
        console.log('订单详情响应(用户ID接口):', byUserOrderId);
        if (byUserOrderId && byUserOrderId.code === 200 && byUserOrderId.data) {
          data = byUserOrderId.data;
        }
      } catch (e) {
        console.warn('用户ID订单详情接口调用失败:', e);
      }
    }

    if (!data) {
      try {
        const byId = await get(`/attendant/orders/${orderKey}`);
        console.log('订单详情响应(陪诊师端接口):', byId);
        if (byId && byId.code === 200 && byId.data) {
          data = byId.data;
        }
      } catch (e) {
        console.warn('陪诊师端订单详情接口调用失败:', e);
      }
    }

    if (!data) {
      uni.showToast({ title: '获取订单详情失败', icon: 'none' });
      return;
    }

    const nextQrSourceKey = getQrSourceKey(data);
    const qrSourceChanged = nextQrSourceKey !== currentQrSourceKey.value;

    order.value = {
      ...data
    };

    if (qrSourceChanged) {
      currentQrSourceKey.value = nextQrSourceKey;
      qrLoadFailed.value = false;
      qrRefreshToken.value = Date.now();
    }

    currentOrderKey = data.orderNo || orderKey;

    // 更新待支付倒计时（仅对有支付倒计时的用户端订单生效）
    setupPayCountdown();
    setupAssignedCountdown();

    // 已完成订单且可能已评价时，加载评价（含陪诊师回复）
    if (order.value.orderStatus === 6 && order.value.orderId) {
      loadUserEvaluation(order.value.orderId);
    } else {
      userEvaluation.value = null;
    }

    updateRealtimeSyncState(currentOrderKey, order.value);
  } catch (error) {
    console.error('获取订单详情失败:', error);
    uni.showToast({ title: '网络错误', icon: 'none' });
  } finally {
    isFetchingOrder = false;
    if (queuedOrderKey) {
      const nextKey = queuedOrderKey;
      queuedOrderKey = '';
      fetchOrderDetail(nextKey);
    }
  }
};

const isOrderRelatedMessage = (messageType) => {
  const type = String(messageType || '').toUpperCase();
  const orderEventTypes = [
    'ORDER_ACCEPTED',
    'SERVICE_STARTED',
    'SERVICE_COMPLETED',
    'ORDER_STATUS_CHANGED',
    'ORDER_RELEASED_BY_ATTENDANT',
    'SERVICE_PROGRESS_UPDATED',
    'SERVICE_PROGRESS_CHANGED',
    'ORDER_UPDATED',
    'ORDER_CANCELLED',
    'ORDER_FINISHED',
    'TIME_FEE_CONFIRMED',
    'TIME_FEE_DISPUTED',
    'BALANCE_PAYMENT_REQUIRED'
  ];
  if (orderEventTypes.includes(type)) return true;
  return false;
};

const applyOrderEventPatch = (payload = {}, messageType = '') => {
  if (!order.value) return;
  const nextStatus = Number(payload.orderStatus);
  if (Number.isFinite(nextStatus) && Number(order.value.orderStatus) !== nextStatus) {
    order.value = {
      ...order.value,
      orderStatus: nextStatus
    };
    setupAssignedCountdown();
  }

  if (typeof payload.reason === 'string' && payload.reason.trim()) {
    order.value = {
      ...order.value,
      cancelReason: payload.reason
    };
    if (nextStatus === 7 && payload.reason.includes('超时未匹配到陪诊师')) {
      order.value = {
        ...order.value,
        orderStatusDesc: TIMEOUT_CLOSE_STATUS_TEXT
      };
    }
  }

  const nextStep = Number(payload.step);
  const normalizedType = String(messageType || '').toUpperCase();
  if (Number.isFinite(nextStep) && nextStep >= 1 && nextStep <= 4) {
    order.value = {
      ...order.value,
      serviceProgressStep: nextStep
    };
  } else if ((normalizedType === 'SERVICE_COMPLETED' || nextStatus >= 4) && Number(order.value.serviceProgressStep || 0) < 4) {
    order.value = {
      ...order.value,
      serviceProgressStep: 4
    };
  }
};

// WebSocket 消息处理
const handleSocketMessage = (message) => {
  if (!pageActive || !message || !order.value) return;

  let payload = {};
  if (message.data && typeof message.data === 'object') {
    payload = message.data;
  } else if (typeof message.data === 'string') {
    try {
      payload = JSON.parse(message.data);
    } catch (e) {
      payload = {};
    }
  }
  const messageOrderId = Number(message.orderId || payload.orderId || 0);
  const currentOrderId = Number(order.value.orderId || 0);
  const messageOrderNo = String(message.orderNo || payload.orderNo || '');
  const currentOrderNo = String(order.value.orderNo || '');

  const isCurrentOrder = (currentOrderId && messageOrderId && messageOrderId === currentOrderId) ||
    (currentOrderNo && messageOrderNo && messageOrderNo === currentOrderNo) ||
    (typeof message.content === 'string' && currentOrderNo && message.content.includes(currentOrderNo));

  const messageType = message.type || message.eventType || payload.type || '';
  const related = isOrderRelatedMessage(messageType);
  if (!isCurrentOrder || !related) return;

  applyOrderEventPatch({
    orderStatus: payload.orderStatus ?? message.orderStatus,
    step: payload.step ?? message.step,
    reason: payload.reason ?? message.reason
  }, messageType);

  if (String(messageType).toUpperCase() === 'ORDER_RELEASED_BY_ATTENDANT') {
    uni.showToast({ title: '订单已重新进入接单大厅，将为您重新匹配陪诊师', icon: 'none', duration: 2500 });
  }

  if (typeof payload.reason === 'string' && payload.reason.includes('超时未匹配到陪诊师')) {
    uni.showToast({ title: '订单已超时关闭', icon: 'none', duration: 2500 });
  }

  scheduleOrderRefresh();
};

const cleanupRealtimeResources = () => {
  removeOrderListener(handleSocketMessage);
  stopRealtimeSync();
  if (socketRefreshTimer) {
    clearTimeout(socketRefreshTimer);
    socketRefreshTimer = null;
  }
  if (payCountdownTimer) {
    clearInterval(payCountdownTimer);
    payCountdownTimer = null;
  }
  if (assignedCountdownTimer) {
    clearInterval(assignedCountdownTimer);
    assignedCountdownTimer = null;
  }
};

// 页面加载
onLoad(async (options) => {
  if (redirectPublicSafeToHome()) return
  const orderNo = options.orderNo || options.orderId;

  console.log('页面加载参数:', options);
  console.log('解析出的订单号:', orderNo);

  if (!orderNo) {
    uni.showToast({ title: '订单号错误', icon: 'none' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/role-user/order' });
    }, 1500);
    return;
  }

  currentOrderKey = orderNo;
  await fetchOrderDetail(orderNo);

  connectOrderSocket();
  addOrderListener(handleSocketMessage);
});

onShow(() => {
  if (redirectPublicSafeToHome()) return
  pageActive = true;
  const key = currentOrderKey || order.value?.orderNo || order.value?.orderId;
  if (!key) return;
  connectOrderSocket();
  updateRealtimeSyncState(key, order.value);
  fetchOrderDetail(key);
});

onHide(() => {
  pageActive = false;
  stopRealtimeSync();
  if (socketRefreshTimer) {
    clearTimeout(socketRefreshTimer);
    socketRefreshTimer = null;
  }
});

onUnload(() => {
  cleanupRealtimeResources();
});

// 页面卸载（兜底）
onUnmounted(() => {
  cleanupRealtimeResources();
});
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
/* 状态标签样式 */
.status-deposit {
  background-color: #FFF3E0;
  color: #F57C00;
}
.status-waiting {
  background-color: #E3F2FD;
  color: #1976D2;
}
.status-accepted {
  background-color: #E8F5E8;
  color: #4CAF50;
}
.status-service {
  background-color: #F3E5F5;
  color: #9C27B0;
}
.status-confirm {
  background-color: #FFF8E1;
  color: #FF8F00;
}
.status-balance {
  background-color: #FFEBEE;
  color: #D32F2F;
}
.status-completed {
  background-color: #E0E0E0;
  color: #616161;
}
.status-cancelled {
  background-color: #FFEBEE;
  color: #D32F2F;
}

/* 邀请陪诊师按钮 */
.invite-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  text-align: center;
}

.invite-btn {
  width: 100%;
  height: 90rpx;
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
  border: none;
  border-radius: 45rpx;
  font-size: 32rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.invite-icon {
  font-size: 36rpx;
}

.invite-text {
  font-size: 32rpx;
}

/* 二维码区域 */
.qr-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}
.qr-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.qr-container {
  text-align: center;
}
.qr-image {
  width: 300rpx;
  height: 300rpx;
  margin: 0 auto 20rpx;
}
.qr-error-box {
  width: 300rpx;
  min-height: 300rpx;
  margin: 0 auto 20rpx;
  border-radius: 20rpx;
  background: #f8fafc;
  border: 2rpx dashed #cbd5e1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  box-sizing: border-box;
}
.qr-error-text {
  font-size: 26rpx;
  color: #475569;
  margin-bottom: 18rpx;
}
.qr-retry-btn {
  min-width: 180rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  font-size: 24rpx;
  border: none;
}
.qr-preview-hint {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #007AFF;
  text-align: center;
}
.qr-desc {
  font-size: 26rpx;
  color: #666;
  text-align: center;
}

/* 时长确认弹窗 */
.duration-modal-overlay {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.50);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 1000;
  padding: 80rpx 20rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.duration-modal {
  width: 100%;
  max-width: 640rpx;
  max-height: calc(100dvh - 80rpx - env(safe-area-inset-bottom));
  transform: translateY(0);
  animation: modalIn 180ms ease-out;
}
@keyframes modalIn {
  from { transform: translateY(-40rpx); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* 时长确认内容卡片 */
.duration-confirm-section {
  background: white;
  border-radius: 34rpx;
  padding: 36rpx;
  box-shadow: 0 24rpx 70rpx rgba(25, 66, 128, 0.18);
  max-height: calc(100dvh - 80rpx - env(safe-area-inset-bottom));
  overflow-y: auto;
  box-sizing: border-box;
}
.confirm-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.duration-card {
  background: #f5f9ff;
  border-radius: 20rpx;
  padding: 24rpx;
}
.duration-card .row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.duration-card .label {
  font-size: 28rpx;
  color: #666;
}
.duration-card .value {
  font-size: 28rpx;
  color: #333;
}
.duration-card .value.highlight {
  color: #007AFF;
  font-weight: 500;
}
.duration-card .value.diff {
  font-weight: 600;
}
.duration-card .value.needPay {
  color: #FF4D4F;
}
.duration-card .value.autoRefund {
  color: #52c41a;
}
.rule-text {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
  line-height: 1.4;
}
.rule-main-line {
  display: block;
}
.rule-extra-tip {
  margin-top: 10rpx;
  padding: 12rpx 14rpx;
  border-radius: 14rpx;
  background: #fff7f7;
  border: 1rpx solid #ffd6d6;
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
}
.rule-extra-tag {
  font-size: 22rpx;
  color: #ff4d4f;
  font-weight: 600;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: #ffecec;
  flex-shrink: 0;
}
.rule-extra-text {
  font-size: 24rpx;
  color: #666;
  line-height: 1.6;
}

/* 差额支付方式区域 */
.duration-pay-section {
  margin-top: 24rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #f9fafb;
  border: 1rpx solid #e5e7eb;
}
.pay-section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8rpx;
}
.pay-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #111827;
}
.pay-amount {
  font-size: 30rpx;
  font-weight: 700;
  color: #ff4d4f;
}
.pay-section-subtitle {
  font-size: 22rpx;
  color: #6b7280;
  margin-bottom: 12rpx;
}
.pay-options {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.pay-option {
  display: flex;
  align-items: center;
  padding: 10rpx 4rpx;
}
.pay-option-text {
  margin-left: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}
.pay-option-main {
  font-size: 24rpx;
  color: #111827;
}
.pay-option-sub {
  font-size: 22rpx;
  color: #9ca3af;
}
.btn-group {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
}
.disagree-btn,
.confirm-btn {
  width: 100%;
  height: 80rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}
.disagree-btn {
  flex: 1;
  background: #ffffff;
  color: #007AFF;
  border: 2rpx solid rgba(24, 144, 255, 0.35);
}
.confirm-btn {
  flex: 1.2;
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
}

/* 申诉弹窗 */
.dispute-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.44);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 48rpx;
  box-sizing: border-box;
}
.dispute-modal {
  width: 100%;
  max-width: 630rpx;
  background: #fff;
  border-radius: 40rpx;
  padding: 42rpx 34rpx 32rpx;
  box-shadow: 0 30rpx 90rpx rgba(25, 66, 128, 0.22);
  box-sizing: border-box;
  animation: userModalIn 180ms ease-out;
}
.dispute-title {
  font-size: 36rpx;
  font-weight: 800;
  margin-bottom: 24rpx;
  text-align: center;
  color: #172033;
}
.dispute-row {
  margin-bottom: 22rpx;
}
.dispute-label {
  font-size: 27rpx;
  color: #53627a;
  margin-bottom: 12rpx;
  display: block;
  font-weight: 700;
}
.dispute-input {
  width: 100%;
  height: 82rpx;
  border-radius: 24rpx;
  border: 1rpx solid #dce8f6;
  padding: 0 22rpx;
  font-size: 28rpx;
  color: #172033;
  background: #f8fbff;
  box-sizing: border-box;
}
.dispute-textarea {
  width: 100%;
  min-height: 170rpx;
  border-radius: 24rpx;
  border: 1rpx solid #dce8f6;
  padding: 20rpx 22rpx;
  font-size: 28rpx;
  color: #172033;
  background: #f8fbff;
  box-sizing: border-box;
}
.dispute-actions {
  margin-top: 28rpx;
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 18rpx;
}
.dispute-cancel,
.dispute-submit {
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
}
.dispute-cancel {
  background: #f2f6fb;
  color: #53627a;
}
.dispute-submit {
  background: linear-gradient(135deg, #1777ff, #0f9ed8);
  color: #fff;
  box-shadow: 0 14rpx 30rpx rgba(23, 119, 255, 0.22);
}

/* 差价支付区域 */
.balance-payment-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}
.payment-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.payment-amount {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 32rpx;
}
.amount-label {
  font-size: 28rpx;
  color: #666;
  margin-right: 16rpx;
}
.amount-price {
  font-size: 36rpx;
  color: #FF4D4F;
  font-weight: bold;
}
.pay-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #52C41A, #389E0D);
  color: white;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}

/* 模拟按钮区域 */
.simulate-section {
  padding: 0 32rpx 20rpx 32rpx;
  margin-bottom: 20rpx;
}
.simulate-btn {
  width: 100%;
  height: 80rpx;
  background-color: #FF7F00;
  color: white;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(255, 127, 0, 0.2);
}

/* 保持原有样式 */
.container {
  padding: 20rpx 32rpx 160rpx;
  background-color: #f5f7fa;
  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  overflow-x: clip;
  box-sizing: border-box;
}

.order-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 28rpx;
  color: #666;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.status-tag {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.order-number {
  font-size: 26rpx;
  color: #333;
}

.pay-countdown {
  margin: 0 32rpx 16rpx;
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background-color: #fff7e6;
  border: 1rpx solid #ffe7ba;
}

.assigned-countdown {
  margin: 0 32rpx 16rpx;
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background-color: #eef5ff;
  border: 1rpx solid #d7e8ff;
}

.countdown-text {
  font-size: 24rpx;
  color: #fa8c16;
}

.assigned-countdown .countdown-text {
  color: #007AFF;
}

.countdown-text .time {
  font-weight: 700;
  font-size: 26rpx;
}

.service-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.service-progress-card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.service-progress-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.service-progress-bar {
  display: flex;
  align-items: center;
  margin-bottom: 0;
}

.service-progress-bar .progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 0 auto;
}

.service-progress-bar .step-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: #e0e0e0;
  margin-bottom: 8rpx;
  border: 3rpx solid #fff;
  box-shadow: 0 0 0 2rpx #e0e0e0;
}

.service-progress-bar .progress-step.active .step-dot {
  background: #007AFF;
  box-shadow: 0 0 0 2rpx #007AFF;
}

.service-progress-bar .progress-step.current .step-dot {
  background: #007AFF;
  box-shadow: 0 0 0 2rpx #007AFF, 0 0 0 8rpx rgba(24, 144, 255, 0.2);
}

.service-progress-bar .step-label {
  font-size: 24rpx;
  color: #999;
}

.service-progress-bar .progress-step.active .step-label,
.service-progress-bar .progress-step.current .step-label {
  color: #007AFF;
  font-weight: 500;
}

.service-progress-bar .progress-line {
  flex: 1;
  height: 4rpx;
  background: #e0e0e0;
  margin: 0 8rpx;
  margin-bottom: 28rpx;
}

.service-progress-bar .progress-line.active {
  background: linear-gradient(90deg, #007AFF, #007AFF);
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
}

.label {
  color: #666;
  width: 180rpx;
}

.value {
  color: #333;
  flex: 1;
  text-align: right;
}

.companion-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.companion-info-clickable {
  cursor: pointer;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.companion-details .desc {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-top: 4rpx;
}

.companion-details .timeout-close-text {
  white-space: pre-wrap;
  line-height: 1.7;
}

.companion-details .release-hint {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-top: 8rpx;
}

.companion-details {
  flex: 1;
}

.companion-entry-arrow {
  font-size: 38rpx;
  color: #b8c4d3;
  line-height: 1;
}

.name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  display: block;
  margin-bottom: 8rpx;
}

.phone {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.phone-icon {
  width: 24rpx;
  height: 24rpx;
}

.phone-number {
  font-size: 24rpx;
  color: #666;
}

.rating {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.star {
  font-size: 20rpx;
}

.score {
  font-size: 24rpx;
  color: #FFB300;
}

.payment-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.timeout-close-message {
  display: block;
  font-size: 26rpx;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
}

.price {
  color: #FF4D4F;
  font-weight: bold;
}

.payment-sub-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.payment-sub-row .sub-label {
  font-size: 24rpx;
  color: #666;
}

.payment-sub-row .sub-value {
  font-size: 24rpx;
  color: #333;
}

/* 我的评价（含陪诊师回复） */
.evaluation-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(15, 23, 42, 0.06);
}

.evaluation-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}

.evaluation-rating-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.evaluation-stars {
  display: flex;
  gap: 4rpx;
}

.eval-star {
  font-size: 32rpx;
  color: #e0e0e0;
}

.eval-star.filled {
  color: #faad14;
}

.eval-rating-num {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.evaluation-content {
  padding: 20rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  margin-bottom: 16rpx;
}

.evaluation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.eval-tag {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  background: #e6f7ff;
  color: #007AFF;
  border-radius: 999rpx;
}

.attendant-reply-block {
  padding: 20rpx;
  background: #e6f7ff;
  border-radius: 12rpx;
}

.attendant-reply-block.hasReply {
  background: #f6ffed;
}

.reply-label {
  font-size: 26rpx;
  color: #666;
  margin-right: 8rpx;
}

.reply-text {
  font-size: 28rpx;
  color: #333;
}

.record-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(15, 23, 42, 0.06);
}

.record-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 32rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.record-title::before {
  content: '';
  width: 6rpx;
  height: 30rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #007AFF, #007AFF);
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.step-item {
  display: flex;
  align-items: stretch;
  gap: 20rpx;
}

.step-axis {
  width: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.step-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #007AFF;
  box-shadow: 0 0 0 6rpx rgba(24, 144, 255, 0.12);
  margin-top: 6rpx;
}

.step-line {
  flex: 1;
  width: 4rpx;
  margin-top: 6rpx;
  background: linear-gradient(180deg, rgba(24, 144, 255, 0.16), rgba(24, 144, 255, 0.02));
}

.step-body {
  flex: 1;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: #f9fafb;
  border: 1rpx solid #eef2ff;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8rpx;
}

.step-title {
  font-size: 28rpx;
  color: #111827;
  font-weight: 600;
}

.step-time {
  font-size: 22rpx;
  color: #6b7280;
}

.step-desc {
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.4;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1rpx solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 120rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  z-index: 999;
  padding: 0 32rpx calc(env(safe-area-inset-bottom));
  box-sizing: border-box;
  width: auto;
}

.bottom-left {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.bottom-right {
  flex-shrink: 0;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: #007AFF;
}

.cancel-nav-item text {
  color: #ff4d4f;
}

.cancel-reason-list {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.cancel-reason-item {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid #ddd;
  font-size: 24rpx;
  color: #555;
}

.cancel-reason-item.active {
  border-color: #ff4d4f;
  background-color: #fff1f0;
  color: #ff4d4f;
}

.nav-icon {
  width: 40rpx;
  height: 40rpx;
}

.primary-action-btn {
  min-width: 260rpx;
  padding: 0 40rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.return-to-orders-section {
  padding: 40rpx 32rpx 20rpx 32rpx;
}

.return-to-orders-btn {
  width: 100%;
  height: 80rpx;
  background-color: #007AFF;
  color: white;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(24, 144, 255, 0.2);
}

/* 补付结果弹窗（沿用下单页样式） */
.payment-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.44);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
  padding: 48rpx;
  box-sizing: border-box;
}

.payment-modal-content {
  background: #ffffff;
  border-radius: 40rpx;
  width: 100%;
  max-width: 620rpx;
  max-height: calc(100dvh - 96rpx - env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  box-shadow: 0 30rpx 90rpx rgba(25, 66, 128, 0.22);
  overflow: hidden;
  box-sizing: border-box;
  animation: userModalIn 180ms ease-out;
}

.payment-modal-header {
  padding: 40rpx 32rpx 8rpx;
  text-align: center;
}

.payment-modal-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #172033;
}

.payment-modal-body {
  padding: 20rpx 34rpx 8rpx;
  overflow-y: auto;
  min-height: 0;
  text-align: center;
}

.payment-modal-text {
  display: block;
  padding: 22rpx 24rpx;
  border-radius: 26rpx;
  background: #f7faff;
  font-size: 28rpx;
  color: #53627a;
  line-height: 1.6;
}

.payment-modal-footer {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  padding: 30rpx 34rpx 34rpx;
  gap: 18rpx;
}

.payment-modal-btn {
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
}

.payment-modal-btn.success-btn {
  background: linear-gradient(135deg, #1777ff, #0f9ed8);
  color: #ffffff;
  box-shadow: 0 14rpx 30rpx rgba(23, 119, 255, 0.22);
}

.payment-modal-btn.fail-btn {
  background: #f2f6fb;
  color: #53627a;
}

@keyframes userModalIn {
  from {
    opacity: 0;
    transform: translateY(28rpx) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 联系陪诊师弹窗（与补付弹窗同一套现代风格） */
.contact-modal-overlay {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.50);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 1100;
  padding: 120rpx 20rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.contact-modal {
  width: 100%;
  max-width: 640rpx;
  max-height: calc(100dvh - 120rpx - env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: contactIn 180ms ease-out;
  display: flex;
  flex-direction: column;
}

@keyframes contactIn {
  from { transform: translateY(-40rpx); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22rpx 22rpx 18rpx;
  background: #f8f9fa;
  border-bottom: 1rpx solid #eee;
}

.contact-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.contact-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
}

.contact-subtitle {
  font-size: 24rpx;
  color: #6b7280;
}

.contact-close {
  font-size: 44rpx;
  color: #999;
  line-height: 1;
  padding: 0 8rpx;
}

.contact-options {
  padding: 10rpx 18rpx 6rpx;
  overflow-y: auto;
  min-height: 0;
}

.contact-option {
  display: flex;
  align-items: center;
  padding: 18rpx 10rpx;
  border-radius: 16rpx;
}

.contact-option + .contact-option {
  margin-top: 10rpx;
}

.contact-option:active {
  background: #f5f7ff;
}

.contact-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  margin-right: 14rpx;
  flex-shrink: 0;
}

.contact-icon.phone {
  background: #f0f7ff;
  color: #007AFF;
}

.contact-icon.chat {
  background: #f6ffed;
  color: #52c41a;
}

.contact-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.contact-main {
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.contact-desc {
  font-size: 24rpx;
  color: #6b7280;
}

.contact-arrow {
  font-size: 40rpx;
  color: #cbd5e1;
  margin-left: 10rpx;
}

.contact-footer {
  padding: 18rpx 22rpx 22rpx;
  border-top: 1rpx solid #f0f0f0;
}

.contact-cancel-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #f5f7fa;
  color: #666;
  border: none;
  border-radius: 40rpx;
  font-size: 28rpx;
}
</style>
