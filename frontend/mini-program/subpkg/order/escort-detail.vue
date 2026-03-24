<template>
	<view class="container">

		
		<view class="loading" v-if="isLoading">
			<text>加载中...</text>
		</view>
		
		<scroll-view class="content" scroll-y v-else>
			<view class="overview-card">
				<view class="overview-top">
					<view class="overview-status-wrap">
						<text class="overview-status">{{ statusText }}</text>
						<text class="overview-desc">{{ statusDesc || '请查看详情后进行下一步操作' }}</text>
						<text class="overview-hint">{{ overviewHintText }}</text>
					</view>
					<view class="overview-income" v-if="orderInfo.status !== 'completed'">
						<text class="overview-income-label">本单可得</text>
						<text class="overview-income-value">¥{{ feeInfoAttendantIncomeText }}</text>
					</view>
				</view>
				<text class="overview-order-no">订单号：{{ orderInfo.orderNo }}</text>
			</view>

			<!-- 顶部：订单整体状态进度条（待核销 / 服务中 / 待确认） -->
			<view class="status-card top-progress-card" v-if="['accepted','in_progress','waiting_confirm'].includes(orderInfo.status)">
				<view class="progress-title-row">
					<text class="progress-title">服务进度</text>
					<text class="progress-step-text">当前：{{ serviceProgressText }}</text>
				</view>
				<view class="service-progress-bar">
					<view class="progress-step" :class="{ active: serviceProgressStep >= 1, current: serviceProgressStep === 1 }">
						<view class="step-dot"></view>
						<text class="step-label">待核销</text>
					</view>
					<view class="progress-line" :class="{ active: serviceProgressStep >= 2 }"></view>
					<view class="progress-step" :class="{ active: serviceProgressStep >= 2, current: serviceProgressStep === 2 }">
						<view class="step-dot"></view>
						<text class="step-label">服务中</text>
					</view>
					<view class="progress-line" :class="{ active: serviceProgressStep >= 3 }"></view>
					<view class="progress-step" :class="{ active: serviceProgressStep >= 3, current: serviceProgressStep === 3 }">
						<view class="step-dot"></view>
						<text class="step-label">待确认</text>
					</view>
				</view>
				<!-- 待服务时：服务准备状态 + 前往准备 -->
				<view class="prep-section" v-if="orderInfo.status === 'accepted'">
					<view class="prep-status">
						<text class="prep-label">服务准备：</text>
						<text class="prep-value" :class="{ done: isPrepared }">{{ isPrepared ? '已准备' : '未完成' }}</text>
					</view>
					<button v-if="!isPrepared" class="prep-btn action-style" @click="goToPrepare">前往准备</button>
				</view>
			</view>

			<!-- 订单完成：顶部直接展示结算信息（替代流程进度） -->
			<view v-else-if="orderInfo.status === 'completed'">
				<view class="settlement-card">
					<view class="settlement-header">
						<text class="settlement-title">本单结算</text>
						<text class="settlement-status-tag">订单完成</text>
					</view>
					<view class="settlement-amount">¥{{ attendantIncomeText }}</view>

					<view class="settlement-breakdown">
						<view class="settlement-row">
							<text class="settlement-label">患者支付总额</text>
							<text class="settlement-value">¥{{ settlementTotalText }}</text>
						</view>
						<view class="settlement-row">
							<text class="settlement-label">平台服务费（10%）</text>
							<text class="settlement-value minus">-¥{{ platformServiceFeeText }}</text>
						</view>
					</view>
				</view>

				<!-- 患者评价（紧接结算卡片下方） -->
				<view class="info-card evaluation-card">
					<view class="card-title evaluation-header">
						<text class="evaluation-title">患者评价</text>
						<view class="evaluation-rating" v-if="evaluation">
							<view class="stars">
								<text v-for="i in 5" :key="i" class="star" :class="{ filled: i <= (evaluation.rating || 0) }">★</text>
							</view>
							<text class="rating-num">{{ (evaluation.rating || 0).toFixed(1) }}</text>
						</view>
					</view>
					<view v-if="!evaluation" class="evaluation-empty">
						<text>该订单暂无患者评价</text>
					</view>
					<template v-else>
						<view class="evaluation-content" v-if="evaluation.content">
							<text>{{ evaluation.content }}</text>
						</view>
						<view class="evaluation-tags" v-if="evaluation.tags">
							<text v-for="(tag, i) in (evaluation.tags || '').split(',')" :key="i" class="tag" v-show="tag">{{ tag.trim() }}</text>
						</view>
						<view class="reply-status" :class="{ hasReply: evaluation.attendantReply }">
							<text class="reply-label">我的回复：</text>
							<text class="reply-text">{{ evaluation.attendantReply || '暂无回复' }}</text>
						</view>
						<view class="reply-input-row">
							<input class="reply-input" v-model="replyInput" placeholder="输入回复..." maxlength="500" />
							<button class="reply-btn" @click="submitReply">回复</button>
						</view>
					</template>
				</view>
			</view>

			<!-- 患者信息（接单前隐藏姓名/头像/电话，仅展示年龄和性别） -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/ren_1.svg" mode="aspectFit"></image>
					<text>患者信息</text>
				</view>
				<view class="patient-info">
					<view class="patient-avatar">
						<image :src="displayPatientAvatar" mode="aspectFill" @error="onPatientAvatarError"></image>
					</view>
					<view class="patient-details">
						<view class="patient-name">{{ orderInfo.patientName }}</view>
						<view class="patient-meta">
							<text>{{ orderInfo.patientAge }}岁</text>
							<text>{{ orderInfo.patientGender }}</text>
							<text v-if="canViewPatientContact">{{ orderInfo.patientPhone }}</text>
						</view>
					</view>
					<view class="contact-btn" v-if="canViewPatientContact" @click="openContactPatientModal">
						<image src="/static/xiaoxi_2.png" mode="aspectFit"></image>
					</view>
				</view>
			</view>

			<!-- 时长费用确认（待患者确认阶段）- 紧跟在患者信息之后 -->
			<view class="info-card confirm-card" v-if="orderInfo.status === 'waiting_confirm'">
				<view class="confirm-header">
					<text class="confirm-title">时长费用确认</text>
					<text class="confirm-status">患者未确认</text>
				</view>
				<view class="confirm-table">
					<view class="row">
						<text class="label">预计时长</text>
						<text class="value">{{ orderInfo.duration || '—' }}</text>
					</view>
					<view class="row">
						<text class="label">实际时长</text>
						<text class="value">{{ orderInfo.actualDuration ? orderInfo.actualDuration + '小时' : '—' }}</text>
					</view>
					<view class="row">
						<text class="label">费用差异</text>
						<text class="value">
							{{ orderInfo.balanceAmount == null ? '—' : (orderInfo.balanceAmount >= 0 ? '需补付¥' + Number(orderInfo.balanceAmount).toFixed(2) : '自动退款¥' + Number(-orderInfo.balanceAmount).toFixed(2)) }}
						</text>
					</view>
				</view>
				<view class="confirm-desc-title">时长说明：</view>
				<view class="confirm-desc">陪诊师可在与患者沟通后，及时提示患者确认时长；如长时间未确认，可申请平台介入。</view>

				<view class="confirm-btn-row">
					<button class="confirm-btn outline" @click="contactPatient">联系患者</button>
					<button class="confirm-btn danger" @click="applyIntervention">申请介入</button>
				</view>
				<button class="confirm-btn secondary" @click="goToSubmitTime">修改时长</button>

				<view class="confirm-warning">
					<text>⚠ 患者正在确认，请稍候。若24小时未确认，可联系客服。</text>
				</view>
			</view>
			
			<!-- 服务信息 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/service.svg" mode="aspectFit"></image>
					<text>服务信息</text>
				</view>
				<view class="service-info">
					<view class="info-row">
						<text class="label">服务类型</text>
						<text class="value">{{ orderInfo.serviceType }}</text>
					</view>
					<view class="info-row">
						<text class="label">医院</text>
						<text class="value">{{ orderInfo.hospital }}</text>
					</view>
					<view class="info-row">
						<text class="label">预约时间</text>
						<text class="value">{{ formatAppointmentTime() }}</text>
					</view>
					<view class="info-row" v-if="orderInfo.appointmentEndTime">
						<text class="label">预约结束时间</text>
						<text class="value">{{ orderInfo.appointmentEndTime }}</text>
					</view>
					<view class="info-row" v-if="orderInfo.duration">
						<text class="label">服务时长</text>
						<text class="value">{{ orderInfo.duration }}</text>
					</view>
				</view>
			</view>
			
			<!-- 服务进度：已到院 / 候诊中 / 检查中 / 就诊完成（已完成订单不显示） -->
			<view class="info-card service-flow-card" v-if="orderInfo.status === 'in_progress'">
				<view class="card-title service-flow-header">
					<image class="title-icon" src="/static/service-progress.svg" mode="aspectFit"></image>
					<text>服务进度</text>
				</view>
				<view class="flow-list">
					<view
						v-for="(step, index) in serviceFlowSteps"
						:key="step.key"
						class="flow-item"
						:class="flowItemClass(index + 1)"
					>
						<view class="flow-left">
							<text class="flow-label">{{ step.label }}</text>
						</view>
						<view class="flow-right">
							<button
								v-if="orderInfo.status === 'completed'"
								class="flow-btn done"
							>已完成</button>
							<button
								v-else-if="currentFlowStep === index + 1 && index + 1 !== serviceFlowSteps.length"
								class="flow-btn doing"
							>进行中</button>
							<button
								v-else-if="currentFlowStep === index + 1 && index + 1 === serviceFlowSteps.length"
								class="flow-btn done"
							>已完成</button>
							<button
								v-else-if="currentFlowStep < index + 1"
								class="flow-btn update"
								@click="updateFlowStep(index + 1)"
							>更新至此</button>
							<button
								v-else
								class="flow-btn done"
							>已完成</button>
						</view>
					</view>
				</view>
			</view>
			
			<!-- 症状描述 -->
			<view class="info-card" v-if="orderInfo.symptomDescription">
				<view class="card-title">
					<image class="title-icon" src="/static/symptom-modern.svg" mode="aspectFit"></image>
					<text>症状描述</text>
				</view>
				<view class="special-requests">
					<text>{{ orderInfo.symptomDescription }}</text>
				</view>
			</view>
			<!-- 其他需求 -->
			<view class="info-card" v-if="orderInfo.otherRequirement">
				<view class="card-title">
					<image class="title-icon" src="/static/requirement.svg" mode="aspectFit"></image>
					<text>其他需求</text>
				</view>
				<view class="special-requests">
					<text>{{ orderInfo.otherRequirement }}</text>
				</view>
			</view>
			<!-- 特殊需求（兼容旧数据） -->
			<view class="info-card" v-else-if="orderInfo.specialRequests">
				<view class="card-title">
					<image class="title-icon" src="/static/note.png" mode="aspectFit"></image>
					<text>特殊需求</text>
				</view>
				<view class="special-requests">
					<text>{{ orderInfo.specialRequests }}</text>
				</view>
			</view>
			
			<!-- 费用信息（已完成订单不显示） -->
			<view class="info-card" v-if="orderInfo.status !== 'completed'">
				<view class="card-title">
					<image class="title-icon" src="/static/money.svg" mode="aspectFit"></image>
					<text>费用信息</text>
				</view>
				<view class="fee-info">
					<view class="fee-row total">
						<text class="fee-label">金额</text>
						<text class="fee-value total-price">¥{{ feeInfoAttendantIncomeText }}</text>
					</view>
				</view>
			</view>

			<!-- 取消信息 -->
			<view class="info-card" v-if="orderInfo.status === 'cancelled'">
				<view class="card-title">
					<image class="title-icon" src="/static/cancel.svg" mode="aspectFit"></image>
					<text>取消信息</text>
				</view>
				<view class="fee-info">
					<view class="fee-row">
						<text class="fee-label">取消原因</text>
						<text class="fee-value">{{ orderInfo.cancelReason || '未填写' }}</text>
					</view>
					<view class="fee-row">
						<text class="fee-label">取消方</text>
						<text class="fee-value">{{ getCancelByText(orderInfo.cancelBy) }}</text>
					</view>
					<view class="fee-row">
						<text class="fee-label">取消时间</text>
						<text class="fee-value">{{ formatCancelTime(orderInfo.cancelTime) || '未知' }}</text>
					</view>
					<view class="fee-row">
						<text class="fee-label">违约金（从您账户扣除）</text>
						<text class="fee-value">¥{{ Number(orderInfo.penaltyAmount || 0).toFixed(2) }}</text>
					</view>
				</view>
			</view>
						
			<!-- 服务记录 -->
			<view class="info-card" v-if="orderInfo.serviceRecords && orderInfo.serviceRecords.length > 0">
				<view class="card-title">
					<image class="title-icon" src="/static/record.svg" mode="aspectFit"></image>
					<text>服务记录</text>
				</view>
				<view class="service-records">
					<view class="record-item" v-for="(record, index) in orderInfo.serviceRecords" :key="index">
						<view class="record-time">{{ record.time }}</view>
						<view class="record-content">{{ record.content }}</view>
					</view>
				</view>
			</view>
			<!-- 待核销时页面底部取消订单（在滚动区域最底部，非底栏） -->
			<view class="cancel-order-area" v-if="orderInfo.status === 'accepted'">
				<view class="tips-text">实际费用按服务时长多退少补</view>
				<button class="cancel-order-btn" :disabled="cancelSubmitting" @click="openCancelModal">取消订单</button>
			</view>
		</scroll-view>
		
		<!-- 底部操作按钮 -->
		<view class="bottom-actions" v-if="showActions">
			<button class="action-btn secondary" @click="contactPatient">联系患者</button>
			<!-- 待服务状态：扫码核销（未准备时点弹窗）、模拟扫码 -->
			<template v-if="orderInfo.status === 'accepted'">
				<button class="action-btn" :class="isPrepared ? 'primary' : 'disabled'" @click="onScanCodeClick">扫码核销</button>
				<button class="action-btn" :class="isPrepared ? 'warning' : 'disabled'" @click="onSimulateScanClick">模拟扫码</button>
			</template>
			<!-- 服务中状态：显示结束服务 -->
			<button v-else-if="orderInfo.status === 'in_progress'" class="action-btn primary" @click="handleEndServiceClick">结束服务</button>
		</view>

		<!-- 去准备弹窗 -->
		<view class="modal-overlay" v-if="showPrepareModal" @click="showPrepareModal = false">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">去准备</text>
				</view>
				<view class="modal-body">
					<text class="modal-desc">请先完成服务准备，完成后即可扫码核销</text>
				</view>
				<view class="modal-footer">
					<button class="modal-btn cancel" @click="showPrepareModal = false">取消</button>
					<button class="modal-btn confirm" @click="showPrepareModal = false; goToPrepare()">前往准备</button>
				</view>
			</view>
		</view>

		<!-- 模拟扫码弹窗 -->
		<view class="modal-overlay" v-if="showSimulateModal" @click="showSimulateModal = false">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">模拟扫码</text>
				</view>
				<view class="modal-body">
					<input class="modal-input" v-model="simulateQrContent" placeholder="请输入二维码内容" />
					<view class="qr-hint">提示：输入 SERVICE_CONFIRM_{{orderInfo.id}}</view>
				</view>
				<view class="modal-footer">
					<button class="modal-btn cancel" @click="showSimulateModal = false">取消</button>
					<button class="modal-btn confirm" @click="handleSimulateScan">确定</button>
				</view>
			</view>
		</view>

		<!-- 扫码核销确认弹窗 -->
		<view class="modal-overlay" v-if="showVerifyConfirmModal" @click="closeVerifyConfirmModal">
			<view class="modal-content verify-modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">确认核销订单</text>
				</view>
				<view class="modal-body">
					<view class="verify-modal-tip">
						<text>请确认当前扫码订单与患者现场订单一致，再执行核销并开始服务。</text>
					</view>
					<view class="verify-summary">
						<view class="verify-row">
							<text class="verify-label">订单号</text>
							<text class="verify-value">{{ orderInfo.orderNo || '—' }}</text>
						</view>
						<view class="verify-row">
							<text class="verify-label">患者</text>
							<text class="verify-value">{{ orderInfo.patientName || '—' }}</text>
						</view>
						<view class="verify-row">
							<text class="verify-label">服务类型</text>
							<text class="verify-value">{{ orderInfo.serviceType || '—' }}</text>
						</view>
						<view class="verify-row">
							<text class="verify-label">医院</text>
							<text class="verify-value">{{ orderInfo.hospital || '—' }}</text>
						</view>
						<view class="verify-row">
							<text class="verify-label">预约时间</text>
							<text class="verify-value">{{ formatAppointmentTime() }}</text>
						</view>
					</view>
					<view class="verify-code-preview">
						<text class="verify-code-label">扫码内容</text>
						<text class="verify-code-text">{{ pendingQrContent || '—' }}</text>
					</view>
				</view>
				<view class="modal-footer">
					<button class="modal-btn cancel" @click="closeVerifyConfirmModal">取消</button>
					<button class="modal-btn confirm" :disabled="verifySubmitting" @click="confirmVerifyQrCode">确认核销</button>
				</view>
			</view>
		</view>

		<!-- 取消订单弹窗 -->
		<view class="modal-overlay" v-if="showCancelModal" @click="showCancelModal = false">
			<view class="modal-content cancel-modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">取消订单</text>
				</view>
				<view class="modal-body">
					<input class="modal-input" v-model="cancelReason" placeholder="请输入取消原因" />
					<view class="cancel-calc">
						<view class="calc-row"><text>距离开始时间：</text><text>{{ cancelLeadTimeText }}</text></view>
						<view class="calc-row" v-if="cancelPenaltyRate > 0"><text>违约金比例：</text><text>{{ cancelPenaltyRateText }}</text></view>
						<view class="calc-row" v-if="cancelPenaltyRate > 0"><text>违约金：</text><text>¥{{ cancelPenaltyAmount }}</text></view>
						<view class="calc-row" v-if="cancelPenaltyRate > 0"><text>患者退款：</text><text>¥{{ cancelRefundAmount }}</text></view>
					</view>
					<view class="cancel-warning" v-if="cancelPenaltyRate > 0">陪诊师主动取消，将从您的账户扣除违约金，患者全额退款。</view>
					<view class="cancel-warning cancel-info" v-else>预约开始前取消，您无需支付违约金，订单将重新开放给其他陪诊师接单。</view>
				</view>
				<view class="modal-footer">
					<button class="modal-btn cancel" @click="showCancelModal = false">再想想</button>
					<button class="modal-btn danger" :disabled="cancelSubmitting" @click="handleCancelOrder">确认取消</button>
				</view>
			</view>
		</view>

		<!-- 联系患者方式选择弹窗 -->
		<view class="contact-modal-overlay" v-if="showContactPatientModal" @click="showContactPatientModal = false">
			<view class="contact-modal" @click.stop>
				<view class="contact-header">
					<view class="contact-title-wrap">
						<text class="contact-title">联系患者</text>
						<text class="contact-subtitle">请选择联系方式</text>
					</view>
					<text class="contact-close" @click="showContactPatientModal = false">×</text>
				</view>
				<view class="contact-options">
					<view class="contact-option" @click="handleContactPatientChoice('phone')">
						<view class="contact-icon phone">电</view>
						<view class="contact-text">
							<text class="contact-main">拨打电话</text>
							<text class="contact-desc">{{ orderInfo.patientPhone || '未提供电话' }}</text>
						</view>
						<text class="contact-arrow">›</text>
					</view>
					<view class="contact-option" @click="handleContactPatientChoice('chat')">
						<view class="contact-icon chat">聊</view>
						<view class="contact-text">
							<text class="contact-main">发送消息</text>
							<text class="contact-desc">进入聊天界面与患者沟通</text>
						</view>
						<text class="contact-arrow">›</text>
					</view>
				</view>
				<view class="contact-footer">
					<button class="contact-cancel-btn" @click="showContactPatientModal = false">取消</button>
				</view>
			</view>
		</view>

		<!-- 结束服务前确认弹窗 -->
		<view class="modal-overlay" v-if="showEndServiceModal" @click="showEndServiceModal = false">
			<view class="modal-content end-service-modal" @click.stop>
				<view class="modal-header">
					<text class="modal-title">确认结束服务</text>
				</view>
				<view class="modal-body">
					<view class="end-service-tip">
						<text class="end-service-title">当前服务进度尚未标记为「就诊完成」。</text>
						<text class="end-service-desc">
							请确认本次陪诊服务的全部流程（到院、候诊、检查、就诊等）是否已经完成，再结束服务并提交时长与费用。
						</text>
					</view>
				</view>
				<view class="modal-footer end-service-footer">
					<button class="modal-btn cancel" @click="showEndServiceModal = false">返回修改服务进度</button>
					<button class="modal-btn confirm" @click="confirmEndServiceWithProgress">已全部完成，一键标记并结束服务</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { get, post, config } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'
import { makePhoneCallWithGuard, scanCodeWithGuard } from '@/subpkg/common/runtime.js'
import placeholderImg from '../../static/user-placeholder.png'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

function fullAvatarUrl(path) {
	if (!path || path.startsWith('http') || path.startsWith('/static')) return path
	const base = (config.baseURL || '').replace(/\/$/, '')
	return base + (path.startsWith('/') ? path : '/' + path)
}

export default {
	data() {
		return {
			isLoading: true,
			showSimulateModal: false,
			showVerifyConfirmModal: false,
			showCancelModal: false,
			showEndServiceModal: false,
			showPrepareModal: false,
			showContactPatientModal: false,
			simulateQrContent: '',
			pendingQrContent: '',
			verifySubmitting: false,
			cancelReason: '',
			cancelPenaltyRate: 0,
			cancelPenaltyAmount: '0.00',
			cancelRefundAmount: '0.00',
			cancelLeadTimeText: '',
			cancelPenaltyRateText: '0%',
			cancelSubmitting: false,
			socketListener: null,
			isPrepared: false,
			patientAvatarError: false,
			orderInfo: {
				id: '',
				orderNo: '',
				status: 'accepted',
				patientAvatar: '',
				patientName: '',
				patientAge: 0,
				patientGender: '',
				patientPhone: '',
				serviceType: '',
				hospital: '',
				appointmentTime: '',
				appointmentEndTime: '',
				duration: '',
				specialRequests: '',
				symptomDescription: '',
				otherRequirement: '',
				serviceFee: 0,
				platformFee: 0,
				totalFee: 0,
				cancelReason: '',
				cancelTime: '',
				cancelBy: null,
				penaltyAmount: 0,
				refundAmount: 0,
				serviceRecords: []
			},
			serviceFlowSteps: [
				{ key: 'arrived', label: '已到院' },
				{ key: 'waiting', label: '候诊中' },
				{ key: 'exam', label: '检查中' },
				{ key: 'finished', label: '就诊完成' }
			],
			currentFlowStep: 1,
			evaluation: null,
			replyInput: '',
		}
	},
	
	computed: {
		displayPatientAvatar() {
			if (this.patientAvatarError) return placeholderImg
			const a = this.orderInfo.patientAvatar
			if (a && (a.startsWith('http') || a.startsWith('data:') || a.startsWith('blob:'))) return a
			if (a && typeof a === 'string' && a.trim() && !a.includes('user-placeholder')) return a
			return placeholderImg
		},
		canViewPatientContact() {
			const status = this.orderInfo.status
			return status && status !== 'pending'
		},
		statusText() {
			const texts = {
				pending: '待接单',
				accepted: '待服务',
				in_progress: '服务中',
				waiting_confirm: '待患者确认',
				disputed: '时长有争议',
				completed: '已完成',
				cancelled: '已取消'
			}
			return texts[this.orderInfo.status] || '未知状态'
		},
		statusDesc() {
			const descs = {
				pending: '等待陪诊师接单',
				accepted: '请按时到达指定地点',
				in_progress: '正在为患者提供陪诊服务',
				waiting_confirm: '已提交服务时长与费用，等待患者确认',
				disputed: '患者对本次时长与费用有异议，等待平台处理',
				completed: '服务已完成，感谢您的专业服务',
				cancelled: '订单已取消'
			}
			return descs[this.orderInfo.status] || ''
		},
		showActions() {
			return ['accepted', 'in_progress'].includes(this.orderInfo.status)
		},
		serviceProgressStep() {
			if (this.orderInfo.status === 'accepted') return 1
			if (this.orderInfo.status === 'in_progress') return 2
			if (this.orderInfo.status === 'waiting_confirm') return 3
			if (this.orderInfo.status === 'completed') return 4
			return 1
		},
		serviceProgressText() {
			const map = {
				accepted: '待核销',
				in_progress: '服务中',
				waiting_confirm: '待确认'
			}
			return map[this.orderInfo.status] || '待接单'
		},
		overviewHintText() {
			const status = this.orderInfo.status
			if (status === 'accepted') {
				return this.isPrepared ? '准备已完成，可扫码核销开始服务' : '请先完成服务准备，再进行扫码核销'
			}
			if (status === 'in_progress') {
				return `当前服务流程进度：第${this.currentFlowStep}步 / 共4步`
			}
			if (status === 'waiting_confirm') {
				return '您已提交时长与费用，正在等待患者确认'
			}
			if (status === 'completed') {
				return '订单已完成，结算金额已生成'
			}
			if (status === 'cancelled') {
				return '订单已取消，可在下方查看取消信息'
			}
			return '等待接单后即可查看完整服务流程'
		},
		settlementTotal() {
			const raw = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0)
			return isNaN(raw) ? 0 : raw
		},
		settlementTotalText() {
			return this.settlementTotal.toFixed(2)
		},
		platformServiceFeeText() {
			const fee = this.settlementTotal * 0.10
			return fee.toFixed(2)
		},
		attendantIncomeText() {
			const income = this.settlementTotal * 0.90
			return income.toFixed(2)
		},
		feeInfoAttendantIncomeText() {
			const raw = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0)
			return (raw * 0.9).toFixed(2)
		}
	},
	
	onLoad(options) {
		if (redirectPublicSafeToHome()) return
		if (options.orderId) {
			this.loadOrderDetail(options.orderId)
		}
		this.setupWebSocketListener()
	},
	onShow() {
		if (redirectPublicSafeToHome()) return
		this.refreshPreparedState()
		if (this.orderInfo && this.orderInfo.id) {
			this.loadOrderDetail(this.orderInfo.id)
		}
	},
	beforeDestroy() {
		if (this.socketListener) {
			removeChatListener(this.socketListener)
		}
	},
	
	methods: {
		refreshPreparedState() {
			if (!this.orderInfo.id) return
			this.isPrepared = uni.getStorageSync(`order_prepared_${this.orderInfo.id}`) === '1'
		},
		async loadEvaluation(orderId) {
			if (!orderId) return
			try {
				const res = await get(`/attendant/orders/${orderId}/evaluation`)
				if (res.code === 200 && res.data) {
					this.evaluation = res.data
					this.replyInput = res.data.attendantReply || ''
				} else {
					this.evaluation = null
				}
			} catch (e) {
				console.error('加载评价失败', e)
				this.evaluation = null
			}
		},
		async submitReply() {
			const content = (this.replyInput || '').trim()
			if (!content) {
				uni.showToast({ title: '请输入回复内容', icon: 'none' })
				return
			}
			if (!this.orderInfo.id) return
			try {
				uni.showLoading({ title: '提交中...' })
				const res = await post(`/attendant/orders/${this.orderInfo.id}/evaluation/reply`, { reply: content })
				uni.hideLoading()
				if (res.code === 200) {
					uni.showToast({ title: '回复成功', icon: 'success' })
					this.evaluation = { ...this.evaluation, attendantReply: content, replyTime: new Date().toISOString() }
					this.replyInput = ''
				} else {
					uni.showToast({ title: res.message || '回复失败', icon: 'none' })
				}
			} catch (e) {
				uni.hideLoading()
				console.error('回复失败', e)
				uni.showToast({ title: '回复失败', icon: 'none' })
			}
		},
		flowItemClass(stepIndex) {
			if (this.orderInfo.status === 'completed') return 'completed'
			if (this.currentFlowStep === stepIndex) return 'current'
			if (this.currentFlowStep > stepIndex) return 'past'
			return 'future'
		},
		updateFlowStep(stepIndex) {
			if (this.orderInfo.status !== 'in_progress') return
			this.currentFlowStep = stepIndex
			if (!this.orderInfo.id) return
			uni.setStorageSync(`order_flow_step_${this.orderInfo.id}`, stepIndex)
			post(`/attendant/orders/${this.orderInfo.id}/service-progress?step=${stepIndex}`)
				.then(res => {
					if (res.code !== 200) {
						console.warn('更新服务进度失败:', res.message)
					}
				})
				.catch(err => {
					console.error('更新服务进度异常:', err)
				})
		},
		applyIntervention() {
			uni.showToast({
				title: '申请介入功能暂未开通，可先联系客服',
				icon: 'none'
			})
		},
		goBack() {
			uni.navigateBack()
		},
		async loadOrderDetail(orderId) {
			this.isLoading = true
			try {
				const res = await get(`/attendant/orders/${orderId}`)
				if (res.code === 200 && res.data) {
					const order = res.data
					let status = 'pending'
					if (order.orderStatus === 2) status = 'accepted'
					else if (order.orderStatus === 3) status = 'in_progress'
					else if (order.orderStatus === 4) status = 'waiting_confirm'
					else if (order.orderStatus === 5) status = 'disputed'
					else if (order.orderStatus === 6) status = 'completed'
					else if (order.orderStatus === 7) status = 'cancelled'

					const showUserAvatar = order.orderStatus !== 1 && order.userAvatar
					this.patientAvatarError = false
					this.orderInfo = {
						id: order.orderId,
						orderNo: order.orderNo,
						userId: order.userId,
						status: status,
						patientName: order.patientName || order.contactPerson,
						patientAge: order.patientAge || '--',
						patientGender: order.patientSex || '未知',
						patientPhone: order.contactPhone || order.userPhone,
						patientAvatar: showUserAvatar ? fullAvatarUrl(order.userAvatar) : '',
						serviceType: order.serviceContent || order.serviceTypeName,
						hospital: order.hospital,
						appointmentTime: (order.serviceDate || '') + ' ' + (order.serviceTimeSlot || ''),
						duration: order.consultationDuration ? order.consultationDuration + '小时' : '2小时',
						symptomDescription: order.specialRequirements || '',
						otherRequirement: (order.customRequirement && order.customRequirement !== '无') ? order.customRequirement : '',
						specialRequests: (!order.specialRequirements && (!order.customRequirement || order.customRequirement === '无')) ? '无特殊要求' : '',
						serviceFee: order.orderAmount,
						totalFee: order.orderAmount,
						cancelReason: order.cancelReason || '',
						cancelTime: order.cancelTime || '',
						cancelBy: order.cancelBy,
						penaltyAmount: order.penaltyAmount || 0,
						refundAmount: order.refundAmount || 0,
						estimatedDuration: order.estimatedDuration,
						actualDuration: order.actualDuration,
						balanceAmount: order.balanceAmount,
						serviceRecords: []
					}
					const stored = uni.getStorageSync(`order_flow_step_${order.orderId}`)
					if (stored) {
						this.currentFlowStep = Number(stored) || 1
					} else {
						if (order.orderStatus === 3) this.currentFlowStep = 2
						else if (order.orderStatus === 6) this.currentFlowStep = 4
						else this.currentFlowStep = 1
					}
					this.simulateQrContent = `SERVICE_CONFIRM_${order.orderId}`
					this.refreshPreparedState()
					if (status === 'completed') {
						this.loadEvaluation(order.orderId)
					} else {
						this.evaluation = null
					}
				} else {
					const msg = res.message || '订单不存在';
					uni.showToast({ title: msg, icon: 'none' })
					setTimeout(() => uni.navigateBack(), 1500)
				}
			} catch (error) {
				console.error('加载订单详情异常:', error)
				uni.showToast({ title: '网络错误', icon: 'none' })
			} finally {
				this.isLoading = false
			}
		},
		goToPrepare() {
			uni.navigateTo({
				url: `/subpkg/order/prepare?orderId=${this.orderInfo.id}&hospital=${encodeURIComponent(this.orderInfo.hospital || '')}&symptom=${encodeURIComponent(this.orderInfo.symptomDescription || '')}&other=${encodeURIComponent(this.orderInfo.otherRequirement || '')}`
			})
		},
		onPatientAvatarError() {
			this.patientAvatarError = true
		},
		openContactPatientModal() {
			this.showContactPatientModal = true
		},
		handleContactPatientChoice(type) {
			this.showContactPatientModal = false
			if (type === 'phone') {
				if (this.orderInfo.patientPhone) {
					makePhoneCallWithGuard(this.orderInfo.patientPhone)
				} else {
					uni.showToast({ title: '暂无患者电话', icon: 'none' })
				}
			} else if (type === 'chat') {
				uni.navigateTo({
					url: `/subpkg/chat/chat?userId=${this.orderInfo.userId}&name=${encodeURIComponent(this.orderInfo.patientName || '患者')}`
				})
			}
		},
		contactPatient() {
			this.openContactPatientModal()
		},
		onScanCodeClick() {
			if (this.orderInfo.status !== 'accepted') return
			if (!this.isPrepared) {
				this.showPrepareModal = true
				return
			}
			this.scanCode()
		},
		onSimulateScanClick() {
			if (this.orderInfo.status !== 'accepted') return
			if (!this.isPrepared) {
				this.showPrepareModal = true
				return
			}
			this.showSimulateModal = true
		},
		scanCode() {
			scanCodeWithGuard({
				success: (res) => {
					this.openVerifyConfirmModal(res.result)
				},
				fail: (err) => {
					if (err?.message === 'unsupported') return
					console.error('扫码失败', err)
					uni.showToast({ title: '扫码失败', icon: 'none' })
				}
			})
		},
		handleSimulateScan() {
			if (!this.simulateQrContent) {
				uni.showToast({ title: '请输入内容', icon: 'none' })
				return
			}
			this.showSimulateModal = false
			this.openVerifyConfirmModal(this.simulateQrContent)
		},
		openVerifyConfirmModal(content) {
			const scannedText = (content || '').trim()
			if (!scannedText) {
				uni.showToast({ title: '未识别到二维码内容', icon: 'none' })
				return
			}
			const validationError = this.getQrValidationError(scannedText)
			if (validationError) {
				uni.showModal({
					title: '扫码内容不匹配',
					content: validationError,
					showCancel: false,
					confirmText: '我知道了'
				})
				return
			}
			this.pendingQrContent = scannedText
			this.showVerifyConfirmModal = true
		},
		parseOrderIdFromQrContent(content) {
			const match = /^SERVICE_CONFIRM_(\d+)$/.exec((content || '').trim())
			if (!match) return null
			return Number(match[1])
		},
		getQrValidationError(content) {
			const scannedText = (content || '').trim()
			if (!scannedText) return '未识别到二维码内容，请重新扫码。'
			const parsedOrderId = this.parseOrderIdFromQrContent(scannedText)
			if (parsedOrderId == null) {
				return '当前二维码不是有效的服务确认码，请确认扫描的是用户订单详情页中的服务二维码。'
			}
			if (Number(this.orderInfo.id) !== parsedOrderId) {
				return `当前扫码对应订单ID为 ${parsedOrderId}，与本订单不匹配。请核对患者现场订单后重新扫码。`
			}
			return ''
		},
		closeVerifyConfirmModal() {
			if (this.verifySubmitting) return
			this.showVerifyConfirmModal = false
			this.pendingQrContent = ''
		},
		confirmVerifyQrCode() {
			if (!this.pendingQrContent) {
				uni.showToast({ title: '未识别到二维码内容', icon: 'none' })
				return
			}
			this.verifyQrCode(this.pendingQrContent)
		},
		async verifyQrCode(content) {
			this.verifySubmitting = true
			uni.showLoading({ title: '核销中...' })
			try {
				const res = await post(`/attendant/orders/${this.orderInfo.id}/scan-qr?qrCodeContent=${encodeURIComponent(content)}`)
				uni.hideLoading()
				if (res.code === 200) {
					this.showVerifyConfirmModal = false
					this.pendingQrContent = ''
					uni.showToast({ title: '核销成功', icon: 'success' })
					this.orderInfo.status = 'in_progress'
					this.loadOrderDetail(this.orderInfo.id)
				} else {
					uni.showToast({ title: res.message || '核销失败', icon: 'none' })
				}
			} catch (e) {
				uni.hideLoading()
				console.error('核销异常:', e)
				uni.showToast({ title: '核销异常', icon: 'none' })
			} finally {
				this.verifySubmitting = false
			}
		},
		handleMainAction() {
			if (this.orderInfo.status === 'accepted') {
				uni.showToast({ title: '请点击扫码核销', icon: 'none' })
			} else if (this.orderInfo.status === 'in_progress') {
				this.handleEndServiceClick()
			}
		},
		openCancelModal() {
			this.cancelReason = ''
			this.calculateCancelPenalty()
			this.showCancelModal = true
		},
		calculateCancelPenalty() {
			const amount = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0)
			const startAt = this.parseServiceStartTime()
			const now = new Date()
			let rate = 1
			let diffMinutes = -1

			if (startAt) {
				diffMinutes = Math.floor((startAt.getTime() - now.getTime()) / 60000)
				if (diffMinutes > 120) {
					rate = 0
				} else if (diffMinutes > 60) {
					rate = 0.2
				} else if (diffMinutes > 30) {
					rate = 0.3
				} else if (diffMinutes >= 0) {
					rate = 0.5
				} else {
					rate = 1
				}
			}

			const penalty = amount * rate
			this.cancelPenaltyRate = rate
			this.cancelPenaltyRateText = `${Math.round(rate * 100)}%`
			this.cancelPenaltyAmount = penalty.toFixed(2)
			this.cancelRefundAmount = amount.toFixed(2)
			this.cancelLeadTimeText = this.formatLeadTime(diffMinutes)
		},
		parseServiceStartTime() {
			const text = this.orderInfo.appointmentTime || ''
			const match = text.match(/(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/)
			if (!match) return null
			return new Date(`${match[1]}T${match[2]}:00`)
		},
		formatLeadTime(diffMinutes) {
			if (diffMinutes < 0) return '已超过开始时间'
			const h = Math.floor(diffMinutes / 60)
			const m = diffMinutes % 60
			if (h > 0) return `${h}小时${m}分钟`
			return `${m}分钟`
		},
		getCancelByText(cancelBy) {
			if (cancelBy === 1) return '陪诊师'
			if (cancelBy === 0) return '用户'
			return '系统'
		},
		formatCancelTime(val) {
			if (!val) return ''
			if (typeof val === 'string' && val.length >= 16) {
				if (/^\d{4}-\d{2}-\d{2}/.test(val)) return val.substring(0, 19).replace('T', ' ')
				const i = val.indexOf('T')
				if (i !== -1) return val.substring(0, 19).replace('T', ' ')
			}
			if (typeof val === 'number' && val > 0) {
				const d = new Date(val)
				return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') + ' ' +
					String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0') + ':' + String(d.getSeconds()).padStart(2, '0')
			}
			return val
		},
		handleCancelOrder() {
			if (this.cancelSubmitting) return
			if (!this.cancelReason || !this.cancelReason.trim()) {
				uni.showToast({ title: '请输入取消原因', icon: 'none' })
				return
			}
			this.cancelSubmitting = true
			// 这里按你后端实际的取消接口来调用
			post(`/attendant/orders/${this.orderInfo.id}/cancel`, {
				reason: this.cancelReason.trim(),
				penaltyAmount: this.cancelPenaltyAmount,
				refundAmount: this.cancelRefundAmount,
				penaltyRate: this.cancelPenaltyRate
			}).then((apiRes) => {
				if (apiRes.code === 200) {
					this.showCancelModal = false
					uni.showToast({ title: '订单已取消', icon: 'success' })
					setTimeout(() => uni.navigateBack(), 1500)
				} else {
					uni.showToast({ title: apiRes.message || '取消失败', icon: 'none' })
				}
			}).catch((e) => {
				console.error('取消订单异常:', e)
				uni.showToast({ title: '取消订单失败', icon: 'none' })
			}).finally(() => {
				this.cancelSubmitting = false
			})
		},
		goToSubmitTime() {
			uni.navigateTo({
				url: `/subpkg/order/submit-time-fee?orderId=${this.orderInfo.id}`
			})
		},
		handleEndServiceClick() {
			if (this.orderInfo.status !== 'in_progress') {
				return
			}
			if (this.currentFlowStep >= 4) {
				this.goToSubmitTime()
				return
			}
			this.showEndServiceModal = true
		},
		async confirmEndServiceWithProgress() {
			this.showEndServiceModal = false
			const finalStep = 4
			this.currentFlowStep = finalStep
			if (this.orderInfo && this.orderInfo.id) {
				uni.setStorageSync(`order_flow_step_${this.orderInfo.id}`, finalStep)
				try {
					const res = await post(`/attendant/orders/${this.orderInfo.id}/service-progress?step=${finalStep}`)
					if (res && res.code !== 200) {
						console.warn('一键更新服务进度为就诊完成失败:', res.message)
					}
				} catch (e) {
					console.error('一键更新服务进度为就诊完成异常:', e)
				}
			}
			this.goToSubmitTime()
		},
		formatAppointmentTime() {
			return this.orderInfo.appointmentTime || '时间待定'
		},
		handleSocketMessage(message) {
			const isCurrentOrder = message.orderId === this.orderInfo.id || 
			                      message.orderNo === this.orderInfo.orderNo ||
			                      (message.data && (message.data.orderId === this.orderInfo.id || message.data.orderNo === this.orderInfo.orderNo))
			if (isCurrentOrder && (message.type === 'SERVICE_STARTED' || 
			                       message.type === 'SERVICE_COMPLETED' ||
			                       message.type === 'ORDER_STATUS_CHANGED')) {
				setTimeout(() => {
					this.loadOrderDetail(this.orderInfo.id)
				}, 1000)
			}
		},
		setupWebSocketListener() {
			this.socketListener = this.handleSocketMessage.bind(this)
			addChatListener(this.socketListener)
		}
	}
}
</script>

<style lang="scss">
@import '@/styles/escort-ui.scss';
.container {
	background-color: #f5f7fa;
	min-height: 100vh;
	--primary: #007AFF;
	--primary-deep: #2563EB;
	--primary-soft: #eaf3ff;
	--text-main: #1f2937;
	--text-sub: #667085;
	--card-shadow: 0 10rpx 24rpx rgba(31, 41, 55, 0.08);
}

.custom-navbar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 1000;
	padding-top: var(--status-bar-height);
	background-color: var(--primary);
	
	.navbar-content {
		height: 44px;
		display: flex;
		align-items: center;
		padding: 0 15px;
		
		.nav-left, .nav-right {
			width: 60px;
			height: 44px;
			display: flex;
			align-items: center;
		}
		
		.back-icon {
			width: 24px;
			height: 24px;
			filter: brightness(0) invert(1);
		}
		
		.nav-title {
			flex: 1;
			text-align: center;
			font-size: 18px;
			font-weight: 600;
			color: #ffffff;
		}
	}
}

.content {
	padding: 18rpx 0 128rpx;
}

.overview-card {
	position: relative;
	overflow: hidden;
	background: linear-gradient(135deg, #69B2FF, #007AFF 56%, #5b9df2);
	margin: 0 24rpx 22rpx;
	padding: 28rpx 26rpx 24rpx;
	border-radius: 20rpx;
	box-shadow: 0 14rpx 28rpx rgba(86, 157, 247, 0.3);
	color: #ffffff;
	animation: slideUp 260ms ease both;
}

.overview-card::before {
	content: '';
	position: absolute;
	width: 280rpx;
	height: 280rpx;
	right: -86rpx;
	top: -130rpx;
	background: radial-gradient(circle, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0));
}

.overview-card::after {
	content: '';
	position: absolute;
	width: 180rpx;
	height: 180rpx;
	right: 132rpx;
	bottom: -116rpx;
	background: radial-gradient(circle, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0));
}

.overview-top {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 18rpx;
	position: relative;
	z-index: 1;
}

.overview-status-wrap {
	flex: 1;
	min-width: 0;
}

.overview-status {
	display: block;
	font-size: 36rpx;
	font-weight: 700;
	line-height: 1.2;
	letter-spacing: 1rpx;
}

.overview-desc {
	display: block;
	font-size: 23rpx;
	opacity: 0.94;
	margin-top: 10rpx;
	line-height: 1.4;
}

.overview-hint {
	display: inline-flex;
	margin-top: 12rpx;
	padding: 8rpx 14rpx;
	border-radius: 999rpx;
	font-size: 21rpx;
	line-height: 1.3;
	background: rgba(255, 255, 255, 0.2);
	border: 1rpx solid rgba(255, 255, 255, 0.28);
}

.overview-income {
	text-align: center;
	flex-shrink: 0;
	background: rgba(255, 255, 255, 0.18);
	border: 1rpx solid rgba(255, 255, 255, 0.3);
	border-radius: 14rpx;
	padding: 10rpx 14rpx;
	min-width: 182rpx;
	backdrop-filter: blur(4rpx);
}

.overview-income-label {
	display: block;
	font-size: 22rpx;
	opacity: 0.9;
}

.overview-income-value {
	display: block;
	font-size: 40rpx;
	font-weight: 700;
	margin-top: 6rpx;
}

.overview-order-no {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	margin-top: 16rpx;
	padding: 8rpx 14rpx;
	font-size: 22rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.16);
	border: 1rpx solid rgba(255, 255, 255, 0.25);
	opacity: 0.95;
	position: relative;
	z-index: 1;
}

.loading {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 200px;
	color: #666;
	font-size: 16px;
}

.status-card {
	background-color: #ffffff;
	margin: 0 24rpx 20rpx;
	border-radius: 18rpx;
	padding: 26rpx 24rpx;
	box-shadow: var(--card-shadow);
	animation: slideUp 280ms ease both;
	
	&.top-progress-card {
		padding-top: 24rpx;
		padding-bottom: 24rpx;
		.service-progress-bar {
			margin-bottom: 0;
		}
	}
	
	.progress-title-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;

		.progress-title {
			font-size: 28rpx;
			font-weight: 600;
			color: var(--text-main);
		}

		.progress-step-text {
			font-size: 23rpx;
			color: var(--primary);
			background: var(--primary-soft);
			border-radius: 999rpx;
			padding: 6rpx 14rpx;
		}
	}
}

.info-card {
	background-color: #ffffff;
	margin: 0 24rpx 20rpx;
	border-radius: 18rpx;
	padding: 26rpx 24rpx;
	box-shadow: var(--card-shadow);
	animation: slideUp 280ms ease both;
	
	.card-title {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
		
		.title-icon {
			width: 34rpx;
			height: 34rpx;
			margin-right: 10rpx;
			opacity: 0.92;
		}
		
		text {
			font-size: 30rpx;
			font-weight: 600;
			color: var(--text-main);
		}
	}
}

.patient-info {
	display: flex;
	align-items: center;
	padding: 8rpx 0 4rpx;
	
	.patient-avatar {
		width: 92rpx;
		height: 92rpx;
		border-radius: 50%;
		overflow: hidden;
		margin-right: 18rpx;
		background: #eef3fb;
		border: 2rpx solid #f0f5ff;
		
		image {
			width: 100%;
			height: 100%;
		}
	}
	
	.patient-details {
		flex: 1;
		
		.patient-name {
			font-size: 30rpx;
			font-weight: 600;
			color: var(--text-main);
			margin-bottom: 10rpx;
		}
		
		.patient-meta {
			font-size: 24rpx;
			color: var(--text-sub);
			
			text {
				margin-right: 16rpx;
			}
		}
	}
	
	.contact-btn {
		width: 72rpx;
		height: 72rpx;
		background: linear-gradient(180deg, #79b5ff, var(--primary));
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 10rpx 20rpx rgba(102, 166, 255, 0.3);
		
		image {
			width: 34rpx;
			height: 34rpx;
			filter: brightness(0) invert(1);
		}
	}
}

.service-info {
	.info-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 18rpx 0;
		border-bottom: 1rpx solid #edf2f8;
		
		&:last-child {
			border-bottom: none;
		}
		
		.label {
			font-size: 24rpx;
			color: #7b8798;
		}
		
		.value {
			font-size: 25rpx;
			color: var(--text-main);
			font-weight: 500;
			text-align: right;
			max-width: 420rpx;
		}
	}
}

.special-requests {
	padding: 22rpx 22rpx;
	background-color: #f8fafc;
	border: 1rpx solid #e6edf8;
	border-radius: 14rpx;
	font-size: 24rpx;
	color: #334155;
	line-height: 1.6;
}

.fee-info {
	.fee-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 18rpx 0;
		
		.fee-label {
			font-size: 24rpx;
			color: #7b8798;
		}
		
		.fee-value {
			font-size: 24rpx;
			color: var(--text-main);
			font-weight: 500;
		}
		
		&.total {
			border-top: 1rpx solid #edf2f8;
			padding-top: 20rpx;
			margin-top: 6rpx;
			
			.fee-label {
				font-size: 30rpx;
				font-weight: 600;
				color: var(--text-main);
			}
			
			.total-price {
				font-size: 36rpx;
				font-weight: 600;
				color: #ff5b4d;
			}
		}
	}
}

.settlement-card {
	margin: 0 24rpx 20rpx;
	padding: 26rpx 24rpx;
	border-radius: 18rpx;
	background-color: #ffffff;
	box-shadow: var(--card-shadow);
	animation: slideUp 280ms ease both;
}

.settlement-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 10px;
}

.settlement-title {
	font-size: 30rpx;
	font-weight: 600;
	color: var(--text-main);
}

.settlement-status-tag {
	font-size: 22rpx;
	padding: 4rpx 14rpx;
	border-radius: 999px;
	background: var(--primary-soft);
	color: var(--primary);
	border: 1rpx solid #cde2ff;
}

.settlement-amount {
	/* 强调陪诊师实收金额 */
	font-size: 40rpx;
	font-weight: 700;
	margin-top: 8px;
	color: #ff5b4d;
}

.settlement-subtitle {
	font-size: 22rpx;
	color: #999;
	margin-top: 4px;
}

.settlement-breakdown {
	margin-top: 18px;
	padding: 12px 0 4px;
	border-top: 1px solid #f0f0f0;
}

.settlement-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 8px;
}

.settlement-label {
	font-size: 24rpx;
	color: #7b8798;
}

.settlement-value {
	font-size: 26rpx;
	color: var(--text-main);
}

.settlement-value.minus {
	opacity: 0.95;
}

.settlement-label.highlight {
	font-weight: 600;
	color: var(--text-main);
}

.settlement-value.highlight {
	font-size: 32rpx;
	font-weight: 700;
	color: #ff5b4d;
}

.settlement-divider {
	height: 1px;
	background: #f0f0f0;
	margin: 10px 0 8px;
}

/* 患者评价卡片 */
.evaluation-card {
	margin-top: 0;
}
.evaluation-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}
.evaluation-title {
	font-size: 16px;
	font-weight: 600;
	color: #333;
}
.evaluation-rating {
	display: flex;
	align-items: center;
	gap: 8px;
}
.evaluation-rating .stars {
	display: flex;
	gap: 2px;
}
.evaluation-rating .star {
	font-size: 18px;
	color: #e0e0e0;
}
.evaluation-rating .star.filled {
	color: #faad14;
}
.rating-num {
	font-size: 15px;
	font-weight: 600;
	color: #333;
}
.evaluation-empty {
	padding: 24px 0;
	text-align: center;
	font-size: 14px;
	color: #999;
}
.evaluation-content {
	padding: 14px 16px;
	background: #f8f9fa;
	border-radius: 12px;
	font-size: 14px;
	color: #333;
	line-height: 1.6;
	margin-bottom: 12px;
}
.evaluation-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-bottom: 12px;
}
.evaluation-tags .tag {
	font-size: 12px;
	padding: 4px 10px;
	background: var(--primary-soft);
	color: var(--primary);
	border-radius: 999px;
}
.reply-status {
	padding: 12px 16px;
	background: #eef5ff;
	border-radius: 12px;
	margin-bottom: 12px;
}
.reply-status.hasReply {
	background: #f6ffed;
}
.reply-label {
	font-size: 13px;
	color: #666;
	margin-right: 4px;
}
.reply-text {
	font-size: 14px;
	color: #333;
}
.reply-input-row {
	display: flex;
	align-items: center;
	gap: 10px;
}
.reply-input {
	flex: 1;
	height: 40px;
	padding: 0 14px;
	border: 1px solid #e0e0e0;
	border-radius: 20px;
	font-size: 14px;
	background: #f8f9fa;
}
.reply-btn {
	flex-shrink: 0;
	height: 40px;
	padding: 0 20px;
	line-height: 40px;
	font-size: 14px;
	color: var(--primary);
	background: #fff;
	border: 1px solid var(--primary);
	border-radius: 20px;
}
.reply-btn:active {
	background: var(--primary-soft);
}

.service-progress-bar {
	display: flex;
	align-items: center;
	margin-bottom: 0;
	.progress-step {
		display: flex;
		flex-direction: column;
		align-items: center;
		flex: 0 0 auto;
		.step-dot {
			width: 24rpx;
			height: 24rpx;
			border-radius: 50%;
			background: #e0e0e0;
			margin-bottom: 8rpx;
			border: 3rpx solid #fff;
			box-shadow: 0 0 0 2rpx #e0e0e0;
		}
		&.active .step-dot {
			background: var(--primary);
			box-shadow: 0 0 0 2rpx var(--primary);
		}
		&.current .step-dot {
			background: var(--primary);
			box-shadow: 0 0 0 2rpx var(--primary), 0 0 0 8rpx rgba(102, 166, 255, 0.2);
		}
		.step-label {
			font-size: 24rpx;
			color: #999;
		}
		&.active .step-label, &.current .step-label {
			color: var(--primary);
			font-weight: 500;
		}
	}
	.progress-line {
		flex: 1;
		height: 4rpx;
		background: #e0e0e0;
		margin: 0 8rpx;
		margin-bottom: 28rpx;
		&.active {
			background: var(--primary);
		}
	}
}
.prep-section {
	margin-top: 20px;
	padding-top: 20px;
	border-top: 1px solid #f0f0f0;
	display: flex;
	align-items: center;
	.prep-status {
		flex: 1;
		min-width: 0;
		.prep-label { font-size: 15px; color: #666; }
		.prep-value { font-size: 15px; color: #ff9500; }
		.prep-value.done { color: var(--primary); }
	}
	.prep-btn.action-style {
		flex-shrink: 0;
		margin-left: auto;
		height: 44px;
		line-height: 44px;
		padding: 0 24px;
		background: var(--primary);
		color: #fff;
		border: none;
		border-radius: 22px;
		font-size: 14px;
		font-weight: 600;
	}
	.prep-btn.action-style:active {
		background: var(--primary-deep);
		transform: scale(0.98);
	}
}
.action-btn.disabled {
	background: #ccc !important;
	color: #999 !important;
}

.process-timeline {
	.timeline-item {
		display: flex;
		position: relative;
		padding-bottom: 20px;
		
		&:not(:last-child)::after {
			content: '';
			position: absolute;
			left: 8px;
			top: 20px;
			width: 2px;
			height: calc(100% - 10px);
			background-color: #e0e0e0;
		}
		
		&.active::after {
			background-color: var(--primary);
		}
		
		.timeline-dot {
			width: 16px;
			height: 16px;
			border-radius: 50%;
			background-color: #e0e0e0;
			border: 3px solid #ffffff;
			box-shadow: 0 0 0 2px #e0e0e0;
			margin-right: 15px;
			flex-shrink: 0;
			margin-top: 2px;
		}
		
		&.active .timeline-dot {
			background-color: var(--primary);
			box-shadow: 0 0 0 2px var(--primary);
		}
		
		&.current .timeline-dot {
			background-color: #ff9500;
			box-shadow: 0 0 0 2px #ff9500;
			animation: pulse 2s infinite;
		}
		
		.timeline-content {
			flex: 1;
			
			.step-title {
				font-size: 15px;
				font-weight: 600;
				color: #333;
				margin-bottom: 4px;
			}
			
			.step-time {
				font-size: 12px;
				color: var(--primary);
				margin-bottom: 4px;
			}
			
			.step-desc {
				font-size: 13px;
				color: #666;
				line-height: 1.4;
			}
		}
		
		&.active .timeline-content .step-title {
			color: var(--primary);
		}
		
		&.current .timeline-content .step-title {
			color: #ff9500;
		}
	}
}

@keyframes pulse {
	0% {
		box-shadow: 0 0 0 2px #ff9500, 0 0 0 4px rgba(255, 149, 0, 0.3);
	}
	50% {
		box-shadow: 0 0 0 2px #ff9500, 0 0 0 8px rgba(255, 149, 0, 0.1);
	}
	100% {
		box-shadow: 0 0 0 2px #ff9500, 0 0 0 4px rgba(255, 149, 0, 0.3);
	}
}

.service-records {
	.record-item {
		padding: 15px 0;
		border-bottom: 1px solid #f0f0f0;
		
		&:last-child {
			border-bottom: none;
		}
		
		.record-time {
			font-size: 12px;
			color: #999;
			margin-bottom: 5px;
		}
		
		.record-content {
			font-size: 14px;
			color: #333;
			line-height: 1.5;
		}
	}
}

.bottom-actions {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background-color: #ffffff;
	padding: 18rpx 24rpx;
	padding-bottom: calc(18rpx + env(safe-area-inset-bottom));
	border-top: 1rpx solid #e7edf5;
	display: flex;
	gap: 12rpx;
	box-shadow: 0 -6rpx 18rpx rgba(31, 41, 55, 0.06);
	
	.action-btn {
		flex: 1;
		height: 84rpx;
		border-radius: 44rpx;
		font-size: 27rpx;
		font-weight: 600;
		border: none;
		transition: all 0.3s ease;
		
		&.secondary {
			background-color: #f8f9fa;
			color: #666;
			border: 1px solid #e0e0e0;
			
			&:active {
				background-color: #e9ecef;
				transform: scale(0.98);
			}
		}
		
		&.primary {
			background-color: var(--primary);
			color: #ffffff;
			flex: 1.2;
			
			&:active {
				background-color: var(--primary-deep);
				transform: scale(0.98);
			}
		}

		&.warning {
			background-color: #FF9800;
			color: #ffffff;
			flex: 1;

			&:active {
				background-color: #F57C00;
				transform: scale(0.98);
			}
		}

	}
}

/* 页面底部取消订单（滚动区域内） */
.cancel-order-area {
	margin: 0 15px 24px;
	padding-bottom: env(safe-area-inset-bottom);
}
.cancel-order-area .tips-text {
	margin-bottom: 8px;
	padding: 10px 16px;
	text-align: center;
	font-size: 13px;
	color: #ff4d4f;
	background: #ffecec;
	border-radius: 999px;
}

.service-flow-card .service-flow-header {
	display: flex;
	align-items: center;
	justify-content: flex-start;
}
.service-flow-card .service-flow-header .title-icon {
	margin-right: 8px;
}
.flow-list {
	margin-top: 8px;
}
.flow-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 12px 14px;
	margin-bottom: 10px;
	border-radius: 18px;
	background: #f7f8fa;
	position: relative;
}
.flow-item::before {
	content: '';
	position: absolute;
	left: 0;
	top: 8px;
	bottom: 8px;
	width: 4px;
	border-radius: 4px;
	background: #e0e0e0;
}
.flow-item.current {
	background: #e8f3ff;
}
.flow-item.current::before {
	background: var(--primary);
}
.flow-item.past::before,
.flow-item.completed::before {
	background: #52c41a;
}
.flow-left .flow-label {
	font-size: 14px;
	color: #333;
	font-weight: 500;
	margin-left: 8px;
}
.flow-right .flow-btn {
	min-width: 80px;
	padding: 0 12px;
	height: 30px;
	line-height: 30px;
	border-radius: 15px;
	font-size: 12px;
	border: 1px solid #d9d9d9;
	background: #fff;
	color: #666;
}
.flow-btn.doing {
	background: var(--primary);
	border-color: var(--primary);
	color: #fff;
}
.flow-btn.update {
	background: #fff;
	border-color: var(--primary);
	color: var(--primary);
}
.flow-btn.done {
	background: #52c41a;
	border-color: #52c41a;
	color: #fff;
}

.confirm-card {
	margin-top: 0;
}
.confirm-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.confirm-title {
	font-size: 16px;
	font-weight: 600;
	color: #333;
}
.confirm-status {
	font-size: 14px;
	color: #ff4d4f;
}
.confirm-table .row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 6px 0;
}
.confirm-table .label {
	font-size: 14px;
	color: #666;
}
.confirm-table .value {
	font-size: 14px;
	color: #333;
	font-weight: 500;
}
.confirm-desc-title {
	margin-top: 10px;
	font-size: 14px;
	color: #333;
	font-weight: 500;
}
.confirm-desc {
	margin-top: 4px;
	font-size: 13px;
	color: #666;
	background: #f8f9fa;
	border-radius: 8px;
	padding: 10px;
	line-height: 1.5;
}
.confirm-btn-row {
	display: flex;
	gap: 10px;
	margin-top: 14px;
}
.confirm-btn {
	height: 36px;
	border-radius: 18px;
	font-size: 14px;
	border: none;
}
.confirm-btn.outline {
	flex: 1;
	background: #fff;
	color: var(--primary);
	border: 1px solid var(--primary);
}
.confirm-btn.danger {
	flex: 1;
	background: #fff0f0;
	color: #ff4d4f;
	border: 1px solid #ff4d4f;
}
.confirm-btn.secondary {
	width: 100%;
	margin-top: 10px;
	background: #f8f9fa;
	color: #333;
	border: 1px solid #e0e0e0;
}
.confirm-warning {
	margin-top: 10px;
	padding: 8px 10px;
	border-radius: 8px;
	background: #fff7e6;
	font-size: 12px;
	color: #fa8c16;
}
.cancel-order-btn {
	width: 100%;
	height: 44px;
	line-height: 44px;
	font-size: 15px;
	color: #fff;
	background-color: #ff4d4f;
	border: 1px solid #ff4d4f;
	border-radius: 8px;
}
.cancel-order-btn:not([disabled]):active {
	background-color: #e53935;
}
.cancel-order-btn[disabled] {
	opacity: 0.6;
}
.cancel-modal-content {
	width: 86%;
}
.cancel-calc {
	margin-top: 12px;
	padding: 12px;
	border-radius: 8px;
	background: #fafafa;
}
.calc-row {
	display: flex;
	justify-content: space-between;
	font-size: 13px;
	color: #666;
	margin-bottom: 6px;
}
.calc-row:last-child {
	margin-bottom: 0;
}
.cancel-warning {
	margin-top: 10px;
	font-size: 13px;
	color: #ff4d4f;
	line-height: 1.5;
}

.cancel-warning.cancel-info {
	color: #666;
}

/* 联系患者弹窗（与用户端联系弹窗一致风格） */
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
	padding-top: 120rpx;
	padding-left: 20rpx;
	padding-right: 20rpx;
}
.contact-modal {
	width: 90%;
	max-width: 640rpx;
	background: #fff;
	border-radius: 24rpx;
	box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.18);
	overflow: hidden;
	animation: contactIn 180ms ease-out;
}
@keyframes contactIn {
	from { transform: translateY(-40rpx); opacity: 0; }
	to { transform: translateY(0); opacity: 1; }
}
.contact-modal .contact-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 22rpx 22rpx 18rpx;
	background: #f8f9fa;
	border-bottom: 1rpx solid #eee;
}
.contact-modal .contact-title-wrap {
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}
.contact-modal .contact-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #111827;
}
.contact-modal .contact-subtitle {
	font-size: 24rpx;
	color: #6b7280;
}
.contact-modal .contact-close {
	font-size: 44rpx;
	color: #999;
	line-height: 1;
	padding: 0 8rpx;
}
.contact-modal .contact-options {
	padding: 10rpx 18rpx 6rpx;
}
.contact-modal .contact-option {
	display: flex;
	align-items: center;
	padding: 18rpx 10rpx;
	border-radius: 16rpx;
}
.contact-modal .contact-option + .contact-option {
	margin-top: 10rpx;
}
.contact-modal .contact-option:active {
	background: #f5f7ff;
}
.contact-modal .contact-icon {
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
.contact-modal .contact-icon.phone {
	background: #f0f7ff;
	color: var(--primary);
}
.contact-modal .contact-icon.chat {
	background: #f6ffed;
	color: #52c41a;
}
.contact-modal .contact-text {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}
.contact-modal .contact-main {
	font-size: 28rpx;
	font-weight: 600;
	color: #111827;
}
.contact-modal .contact-desc {
	font-size: 24rpx;
	color: #6b7280;
}
.contact-modal .contact-arrow {
	font-size: 40rpx;
	color: #cbd5e1;
	margin-left: 10rpx;
}
.contact-modal .contact-footer {
	padding: 18rpx 22rpx 22rpx;
	border-top: 1rpx solid #f0f0f0;
}
.contact-modal .contact-cancel-btn {
	width: 100%;
	height: 80rpx;
	line-height: 80rpx;
	background: #f5f7fa;
	color: #666;
	border: none;
	border-radius: 40rpx;
	font-size: 28rpx;
}

/* 弹窗样式 */
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1001;
}

.modal-content {
	background-color: #ffffff;
	border-radius: 12px;
	width: 80%;
	padding: 20px;
}

.modal-header {
	text-align: center;
	margin-bottom: 20px;
}

.modal-title {
	font-size: 18px;
	font-weight: 600;
	color: #333;
}

.modal-body {
	margin-bottom: 20px;
}
.modal-desc {
	font-size: 14px;
	color: #666;
	line-height: 1.5;
}

.modal-input {
	width: 100%;
	height: 40px;
	border: 1px solid #e0e0e0;
	border-radius: 4px;
	padding: 0 10px;
	box-sizing: border-box;
	font-size: 14px;
}

.qr-hint {
	font-size: 12px;
	color: #999;
	margin-top: 8px;
	text-align: center;
}

.verify-modal-content {
	width: 84%;
	max-width: 680rpx;
}

.verify-modal-tip {
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
	background: #f3f8ff;
	color: #4b5563;
	font-size: 24rpx;
	line-height: 1.6;
	margin-bottom: 18rpx;
}

.verify-summary {
	border: 1rpx solid #e6edf8;
	border-radius: 16rpx;
	background: #f8fbff;
	padding: 6rpx 20rpx;
}

.verify-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 24rpx;
	padding: 18rpx 0;
	border-bottom: 1rpx solid #e6edf8;
}

.verify-row:last-child {
	border-bottom: none;
}

.verify-label {
	flex-shrink: 0;
	font-size: 24rpx;
	color: #6b7280;
}

.verify-value {
	flex: 1;
	text-align: right;
	font-size: 25rpx;
	font-weight: 500;
	color: var(--text-main);
	line-height: 1.5;
}

.verify-code-preview {
	margin-top: 18rpx;
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
	background: #f5f7fa;
}

.verify-code-label {
	display: block;
	font-size: 23rpx;
	color: #6b7280;
	margin-bottom: 8rpx;
}

.verify-code-text {
	display: block;
	word-break: break-all;
	font-size: 24rpx;
	line-height: 1.5;
	color: var(--text-main);
}

.modal-footer {
	display: flex;
	gap: 10px;
}

.modal-btn {
	flex: 1;
	height: 40px;
	border-radius: 20px;
	font-size: 14px;
	border: none;

	&.cancel {
		background-color: #f5f7fa;
		color: #666;
	}

	&.confirm {
		background-color: var(--primary);
		color: #ffffff;
	}
	&.danger {
		background-color: #ff4d4f;
		color: #ffffff;
	}
}

@keyframes slideUp {
	from {
		opacity: 0;
		transform: translateY(12rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>
