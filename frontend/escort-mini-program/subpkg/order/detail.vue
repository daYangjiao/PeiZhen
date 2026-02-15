<template>
	<view class="container">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="nav-left" @click="goBack">
					<image class="back-icon" src="/static/back.svg" mode="aspectFit"></image>
				</view>
				<view class="nav-title">订单详情</view>
				<view class="nav-right"></view>
			</view>
		</view>
		
		<!-- 加载状态 -->
		<view class="loading" v-if="isLoading">
			<text>加载中...</text>
		</view>
		
		<scroll-view class="content" scroll-y v-else>
			<!-- 订单状态 -->
			<view class="status-card">
				<view class="status-info">
					<image class="status-icon" :src="statusIcon" mode="aspectFit"></image>
					<view class="status-text">
						<text class="status-title">{{ statusText }}</text>
						<text class="status-desc">{{ statusDesc }}</text>
					</view>
				</view>
				<view class="order-no">订单号：{{ orderInfo.orderNo }}</view>
			</view>
			
			<!-- 患者信息 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/ren_1.png" mode="aspectFit"></image>
					<text>患者信息</text>
				</view>
				<view class="patient-info">
					<view class="patient-avatar">
						<image :src="orderInfo.patientAvatar" mode="aspectFill"></image>
					</view>
					<view class="patient-details">
						<view class="patient-name">{{ orderInfo.patientName }}</view>
						<view class="patient-meta">
							<text>{{ orderInfo.patientAge }}岁</text>
							<text>{{ orderInfo.patientGender }}</text>
							<text>{{ orderInfo.patientPhone }}</text>
						</view>
					</view>
					<view class="contact-btn" @click="contactPatient">
						<image src="/static/phone.png" mode="aspectFit"></image>
					</view>
				</view>
			</view>
			
			<!-- 服务信息 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/service.png" mode="aspectFit"></image>
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
			
			<!-- 特殊需求 -->
			<view class="info-card" v-if="orderInfo.specialRequests">
				<view class="card-title">
					<image class="title-icon" src="/static/note.png" mode="aspectFit"></image>
					<text>特殊需求</text>
				</view>
				<view class="special-requests">
					<text>{{ orderInfo.specialRequests }}</text>
				</view>
			</view>
			
			<!-- 费用信息 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/money.png" mode="aspectFit"></image>
					<text>费用信息</text>
				</view>
				<view class="fee-info">
					<view class="fee-row">
						<text class="fee-label">服务费</text>
						<text class="fee-value">¥{{ orderInfo.serviceFee }}</text>
					</view>
					<view class="fee-row" v-if="orderInfo.platformFee">
						<text class="fee-label">平台服务费</text>
						<text class="fee-value">¥{{ orderInfo.platformFee }}</text>
					</view>
					<view class="fee-row total">
						<text class="fee-label">总计</text>
						<text class="fee-value total-price">¥{{ orderInfo.totalFee }}</text>
					</view>
				</view>
			</view>
			
			<!-- 陪诊流程 -->
			<view class="info-card" v-if="orderInfo.status === 'in_progress' || orderInfo.status === 'completed'">
				<view class="card-title">
					<image class="title-icon" src="/static/process.png" mode="aspectFit"></image>
					<text>陪诊流程</text>
				</view>
				<view class="process-timeline">
					<view class="timeline-item" v-for="(step, index) in processSteps" :key="index" :class="{ active: step.completed, current: step.current }">
						<view class="timeline-dot"></view>
						<view class="timeline-content">
							<view class="step-title">{{ step.title }}</view>
							<view class="step-time" v-if="step.time">{{ step.time }}</view>
							<view class="step-desc" v-if="step.desc">{{ step.desc }}</view>
						</view>
					</view>
				</view>
			</view>
			
			<!-- 服务记录 -->
			<view class="info-card" v-if="orderInfo.serviceRecords && orderInfo.serviceRecords.length > 0">
				<view class="card-title">
					<image class="title-icon" src="/static/record.png" mode="aspectFit"></image>
					<text>服务记录</text>
				</view>
				<view class="service-records">
					<view class="record-item" v-for="(record, index) in orderInfo.serviceRecords" :key="index">
						<view class="record-time">{{ record.time }}</view>
						<view class="record-content">{{ record.content }}</view>
					</view>
				</view>
			</view>
		</scroll-view>
		
		<!-- 底部操作按钮 -->
		<view class="bottom-actions" v-if="showActions">
			<button class="action-btn secondary" @click="contactPatient">联系患者</button>

			<!-- 待服务状态：显示扫码核销和模拟扫码 -->
			<template v-if="orderInfo.status === 'accepted'">
				<button class="action-btn primary" @click="scanCode">扫码核销</button>
				<button class="action-btn warning" @click="showSimulateModal = true">模拟扫码</button>
			</template>

			<!-- 服务中状态：显示结束服务 -->
			<button v-else-if="orderInfo.status === 'in_progress'" class="action-btn primary" @click="completeService">结束服务</button>
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
	</view>
</template>

<script>
import { get, post } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'

export default {
	data() {
		return {
			isLoading: true,
			showSimulateModal: false,
			simulateQrContent: '',
			socketListener: null,
			orderInfo: {
				id: '',
				orderNo: '',
				status: 'accepted',
				patientAvatar: '/static/user-placeholder.png',
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
				serviceFee: 0,
				platformFee: 0,
				totalFee: 0,
				serviceRecords: []
			},
			processSteps: [
				{
					title: '前往医院',
					time: '09:00',
					desc: '到达指定地点与患者会合',
					completed: true,
					current: false
				},
				{
					title: '协助挂号',
					time: '09:30',
					desc: '帮助患者完成挂号手续',
					completed: true,
					current: false
				},
				{
					title: '陪同就诊',
					time: '10:00',
					desc: '陪同患者前往科室就诊',
					completed: true,
					current: true
				},
				{
					title: '协助检查',
					desc: '陪同患者完成各项检查',
					completed: false,
					current: false
				},
				{
					title: '取药/报告',
					desc: '协助患者取药或取检查报告',
					completed: false,
					current: false
				},
				{
					title: '服务完成',
					desc: '确认患者安全离开医院',
					completed: false,
					current: false
				}
			]
		}
	},
	
	computed: {
		statusIcon() {
			const icons = {
				pending: '/static/clock.png',
				accepted: '/static/check.png',
				in_progress: '/static/progress.png',
				completed: '/static/success.png',
				cancelled: '/static/cancel.png'
			}
			return icons[this.orderInfo.status] || '/static/clock.png'
		},
		
		statusText() {
			const texts = {
				pending: '待接单',
				accepted: '待服务',
				in_progress: '服务中',
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
				completed: '服务已完成，感谢您的专业服务',
				cancelled: '订单已取消'
			}
			return descs[this.orderInfo.status] || ''
		},
		
		showActions() {
			return ['accepted', 'in_progress'].includes(this.orderInfo.status)
		},
		
		mainActionText() {
			const texts = {
				accepted: '开始服务',
				in_progress: '结束服务' // 修改文案
			}
			return texts[this.orderInfo.status] || ''
		}
	},
	
	onLoad(options) {
		if (options.orderId) {
			this.loadOrderDetail(options.orderId)
		}
		// 添加WebSocket监听
		this.setupWebSocketListener()
	},
	
	beforeDestroy() {
		// 页面销毁时移除监听
		if (this.socketListener) {
			removeChatListener(this.socketListener)
		}
	},
	
	methods: {
		// 返回
		goBack() {
			uni.navigateBack()
		},
		
		// 加载订单详情
		async loadOrderDetail(orderId) {
			this.isLoading = true
			try {
				console.log('正在加载订单详情, ID:', orderId);
				const res = await get(`/attendant/orders/${orderId}`)
				console.log('订单详情响应:', res);

				if (res.code === 200 && res.data) {
					const order = res.data

					// 状态映射
					let status = 'pending'
					if (order.orderStatus === 2) status = 'accepted'
					else if (order.orderStatus === 3) status = 'in_progress'
					else if (order.orderStatus === 6) status = 'completed'
					else if (order.orderStatus === 7) status = 'cancelled'

					this.orderInfo = {
						id: order.orderId,
						orderNo: order.orderNo,
						status: status,
						patientName: order.patientName || order.contactPerson,
						patientAge: order.patientAge || '--',
						patientGender: order.patientSex || '未知',
						patientPhone: order.contactPhone || order.userPhone,
						patientAvatar: '/static/user-placeholder.png',
						serviceType: order.serviceContent || order.serviceTypeName,
						hospital: order.hospital,
						appointmentTime: (order.serviceDate || '') + ' ' + (order.serviceTimeSlot || ''),
						duration: order.consultationDuration ? order.consultationDuration + '小时' : '2小时',
						specialRequests: order.specialRequirements || order.customRequirement || '无特殊要求',
						serviceFee: order.orderAmount,
						totalFee: order.orderAmount,
						serviceRecords: [] // 暂时为空，后续可从后端获取
					}
					// 预填充模拟扫码内容
					this.simulateQrContent = `SERVICE_CONFIRM_${order.orderId}`
				} else {
					const msg = res.message || '订单不存在';
					console.error('加载失败:', msg);
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
		
		// 联系患者
		contactPatient() {
			uni.showActionSheet({
				itemList: ['拨打电话', '发送消息'],
				success: (res) => {
					if (res.tapIndex === 0) {
						// 拨打电话
						uni.makePhoneCall({
							phoneNumber: this.orderInfo.patientPhone
						})
					} else if (res.tapIndex === 1) {
						// 发送消息
						uni.navigateTo({
							url: `/subpkg/chat/chat?userId=${this.orderInfo.userId}&name=${this.orderInfo.patientName}`
						})
					}
				}
			})
		},
		
		// 扫码核销
		scanCode() {
			uni.scanCode({
				success: (res) => {
					this.verifyQrCode(res.result)
				},
				fail: (err) => {
					console.error('扫码失败', err)
					uni.showToast({ title: '扫码失败', icon: 'none' })
				}
			})
		},

		// 模拟扫码
		handleSimulateScan() {
			if (!this.simulateQrContent) {
				uni.showToast({ title: '请输入内容', icon: 'none' })
				return
			}
			this.showSimulateModal = false
			this.verifyQrCode(this.simulateQrContent)
		},

		// 验证二维码并开始服务
		async verifyQrCode(content) {
			uni.showLoading({ title: '核销中...' })
			try {
				// 修复：将 qrCodeContent 作为 URL 参数传递，避免 Content-Type 问题
				const res = await post(`/attendant/orders/${this.orderInfo.id}/scan-qr?qrCodeContent=${encodeURIComponent(content)}`)

				uni.hideLoading()
				if (res.code === 200) {
					uni.showToast({ title: '核销成功', icon: 'success' })
					this.orderInfo.status = 'in_progress'
					// 刷新页面
					this.loadOrderDetail(this.orderInfo.id)
				} else {
					uni.showToast({ title: res.message || '核销失败', icon: 'none' })
				}
			} catch (e) {
				uni.hideLoading()
				console.error('核销异常:', e)
				uni.showToast({ title: '核销异常', icon: 'none' })
			}
		},

		// 主要操作 (保留兼容性)
		handleMainAction() {
			if (this.orderInfo.status === 'accepted') {
				// 引导去扫码
				uni.showToast({ title: '请点击扫码核销', icon: 'none' })
			} else if (this.orderInfo.status === 'in_progress') {
				this.completeService()
			}
		},
		
		// 完成服务
		completeService() {
			uni.showModal({
				title: '确认结束服务',
				content: `确认已完成${this.orderInfo.patientName}的所有陪诊服务？`,
				success: async (res) => {
					if (res.confirm) {
						try {
							const response = await post(`/attendant/orders/${this.orderInfo.id}/end?actualDuration=2`)
							if (response.code === 200) {
								this.orderInfo.status = 'completed'
								uni.showToast({ title: '服务已结束', icon: 'success' })
								setTimeout(() => {
									uni.switchTab({ url: '/pages/order/order' })
								}, 1500)
							}
						} catch (e) {
							console.error('结束服务失败:', e)
						}
					}
				}
			})
		},

		// 格式化预约时间为时间段
		formatAppointmentTime() {
			return this.orderInfo.appointmentTime || '时间待定'
		},
		
		// WebSocket消息处理
		handleSocketMessage(message) {
			console.log('订单详情页收到WebSocket消息:', message)
			
			// 检查是否是当前订单的消息
			const isCurrentOrder = message.orderId === this.orderInfo.id || 
			                      message.orderNo === this.orderInfo.orderNo ||
			                      (message.data && (message.data.orderId === this.orderInfo.id || message.data.orderNo === this.orderInfo.orderNo))
			
			// 处理订单状态变更消息
			if (isCurrentOrder && (message.type === 'SERVICE_STARTED' || 
			                       message.type === 'SERVICE_COMPLETED' ||
			                       message.type === 'ORDER_STATUS_CHANGED')) {
				
				console.log('收到当前订单状态更新，刷新详情')
				// 延迟刷新确保数据库更新
				setTimeout(() => {
					this.loadOrderDetail(this.orderInfo.id)
				}, 1000)
			}
		},
		
		// 设置WebSocket监听
		setupWebSocketListener() {
			this.socketListener = this.handleSocketMessage.bind(this)
			addChatListener(this.socketListener)
		}
	}
}
</script>

<style lang="scss">
.container {
	background-color: #f5f5f5;
	min-height: 100vh;
}

.custom-navbar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 1000;
	padding-top: var(--status-bar-height);
	background-color: #4A90E2;
	
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
	padding-top: calc(var(--status-bar-height) + 44px);
	padding-bottom: 80px;
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
	margin: 15px;
	border-radius: 12px;
	padding: 20px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	
	.status-info {
		display: flex;
		align-items: center;
		margin-bottom: 15px;
		
		.status-icon {
			width: 40px;
			height: 40px;
			margin-right: 15px;
		}
		
		.status-text {
			flex: 1;
			
			.status-title {
				font-size: 18px;
				font-weight: 600;
				color: #333;
				display: block;
				margin-bottom: 5px;
			}
			
			.status-desc {
				font-size: 14px;
				color: #666;
				display: block;
			}
		}
	}
	
	.order-no {
		font-size: 14px;
		color: #999;
	}
}

.info-card {
	background-color: #ffffff;
	margin: 0 15px 15px;
	border-radius: 12px;
	padding: 20px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	
	.card-title {
		display: flex;
		align-items: center;
		margin-bottom: 15px;
		
		.title-icon {
			width: 20px;
			height: 20px;
			margin-right: 8px;
			opacity: 0.8;
		}
		
		text {
			font-size: 16px;
			font-weight: 600;
			color: #333;
		}
	}
}

.patient-info {
	display: flex;
	align-items: center;
	
	.patient-avatar {
		width: 50px;
		height: 50px;
		border-radius: 25px;
		overflow: hidden;
		margin-right: 15px;
		
		image {
			width: 100%;
			height: 100%;
		}
	}
	
	.patient-details {
		flex: 1;
		
		.patient-name {
			font-size: 16px;
			font-weight: 600;
			color: #333;
			margin-bottom: 5px;
		}
		
		.patient-meta {
			font-size: 14px;
			color: #666;
			
			text {
				margin-right: 15px;
			}
		}
	}
	
	.contact-btn {
		width: 40px;
		height: 40px;
		background-color: #4A90E2;
		border-radius: 20px;
		display: flex;
		align-items: center;
		justify-content: center;
		
		image {
			width: 20px;
			height: 20px;
			filter: brightness(0) invert(1);
		}
	}
}

.service-info {
	.info-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 0;
		border-bottom: 1px solid #f0f0f0;
		
		&:last-child {
			border-bottom: none;
		}
		
		.label {
			font-size: 14px;
			color: #666;
		}
		
		.value {
			font-size: 14px;
			color: #333;
			font-weight: 500;
		}
	}
}

.special-requests {
	padding: 15px;
	background-color: #f8f9fa;
	border-radius: 8px;
	font-size: 14px;
	color: #666;
	line-height: 1.5;
}

.fee-info {
	.fee-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 0;
		
		.fee-label {
			font-size: 14px;
			color: #666;
		}
		
		.fee-value {
			font-size: 14px;
			color: #333;
			font-weight: 500;
		}
		
		&.total {
			border-top: 1px solid #f0f0f0;
			padding-top: 15px;
			margin-top: 5px;
			
			.fee-label {
				font-size: 16px;
				font-weight: 600;
				color: #333;
			}
			
			.total-price {
				font-size: 18px;
				font-weight: 600;
				color: #ff6b6b;
			}
		}
	}
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
			background-color: #4A90E2;
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
			background-color: #4A90E2;
			box-shadow: 0 0 0 2px #4A90E2;
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
				color: #4A90E2;
				margin-bottom: 4px;
			}
			
			.step-desc {
				font-size: 13px;
				color: #666;
				line-height: 1.4;
			}
		}
		
		&.active .timeline-content .step-title {
			color: #4A90E2;
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
	padding: 15px;
	border-top: 1px solid #f0f0f0;
	display: flex;
	gap: 10px;
	
	.action-btn {
		flex: 1;
		height: 44px;
		border-radius: 22px;
		font-size: 14px;
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
			background-color: #4A90E2;
			color: #ffffff;
			flex: 1.2;
			
			&:active {
				background-color: #3a7bc8;
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
		background-color: #f5f5f5;
		color: #666;
	}

	&.confirm {
		background-color: #4A90E2;
		color: #ffffff;
	}
}
</style>