<template>
	<view class="order-card" @click="handleCardClick">
		<!-- 订单头部 -->
		<view class="card-header">
			<view class="user-info">
				<image class="avatar" :src="displayAvatar" mode="aspectFill"></image>
				<view class="user-details">
					<view class="name-row">
						<text class="username">{{ orderData.userName }}</text>
						<view class="tags">
							<text class="tag age" v-if="orderData.userAge">{{ orderData.userAge }}岁</text>
							<text class="tag gender" v-if="orderData.userGender">{{ orderData.userGender }}</text>
						</view>
					</view>
					<view class="service-type-box" :class="serviceTypeColorClass">
						<text class="service-type">{{ orderData.serviceType }}</text>
					</view>
				</view>
			</view>
			<text class="price">¥{{ orderData.price }}</text>
		</view>
		
		<!-- 服务信息 -->
		<view class="service-info">
			<view class="info-row">
				<image class="info-icon" src="/static/location.svg" mode="aspectFit"></image>
				<text class="info-text">{{ orderData.hospitalName || '未知医院' }}</text>
			</view>
			
			<view class="info-row">
				<image class="info-icon" src="/static/clock.svg" mode="aspectFit"></image>
				<text class="info-text">{{ orderData.appointmentTime || '时间待定' }}</text>
			</view>
			
			<view class="info-row" v-if="orderData.phone">
				<image class="info-icon" src="/static/phone.png" mode="aspectFit"></image>
				<text class="info-text">{{ orderData.phone }}</text>
			</view>
		</view>
		
		<!-- 症状 & 其他需求（现代化卡片展示） -->
		<view class="special-needs" v-if="orderData.symptomDescription || orderData.otherRequirement">
			<view class="needs-item" v-if="orderData.symptomDescription">
				<image class="needs-icon" src="/static/symptom-modern.svg" mode="aspectFit"></image>
				<view class="needs-content">
					<text class="needs-label">症状</text>
					<text class="needs-value">{{ orderData.symptomDescription }}</text>
				</view>
			</view>
			<view class="needs-item" v-if="orderData.otherRequirement">
				<image class="needs-icon" src="/static/requirement.svg" mode="aspectFit"></image>
				<view class="needs-content">
					<text class="needs-label">其他需求</text>
					<text class="needs-value">{{ orderData.otherRequirement }}</text>
				</view>
			</view>
		</view>
		
		<!-- 订单状态标签 -->
		<view class="status-tag" :class="statusClass" v-if="showStatus">
			<text>{{ statusText }}</text>
		</view>
		
		<!-- 操作按钮 -->
		<view class="card-actions" v-if="showActions">
			<button class="action-btn secondary" @click.stop="handleViewDetail">
				<image class="btn-icon" src="/static/detail.svg" mode="aspectFit"></image>
				<text>查看详情</text>
			</button>
			<button class="action-btn primary" @click.stop="handleMainAction">
				<image class="btn-icon" src="/static/accept.svg" mode="aspectFit"></image>
				<text>{{ mainActionText }}</text>
			</button>
		</view>
	</view>
</template>

<script setup>
import { computed, defineProps, defineEmits } from 'vue'

// 定义props
const props = defineProps({
	orderData: {
		type: Object,
		required: true
	},
	showActions: {
		type: Boolean,
		default: true
	},
	showStatus: {
		type: Boolean,
		default: false
	},
	actionType: {
		type: String,
		default: 'accept' // accept, start, complete, view
	}
})

// 定义emits
const emit = defineEmits(['card-click', 'view-detail', 'main-action'])

// 始终显示用户头像（无头像时使用占位图）
const displayAvatar = computed(() => {
	const placeholder = '/static/user-placeholder.png'
	return props.orderData.userAvatar || placeholder
})

// 计算属性
const statusClass = computed(() => {
	const statusMap = {
		1: 'status-pending',
		2: 'status-accepted',
		3: 'status-progress',
		6: 'status-completed',
		7: 'status-cancelled'
	}
	// 兼容旧的字符串状态
	if (typeof props.orderData.status === 'string') {
		const strMap = {
			pending: 'status-pending',
			accepted: 'status-accepted',
			in_progress: 'status-progress',
			completed: 'status-completed',
			cancelled: 'status-cancelled'
		}
		return strMap[props.orderData.status] || 'status-pending'
	}
	return statusMap[props.orderData.orderStatus] || 'status-pending'
})

const statusText = computed(() => {
	const statusMap = {
		1: '待接单',
		2: '待服务',
		3: '服务中',
		6: '已完成',
		7: '已取消'
	}
	// 兼容旧的字符串状态
	if (typeof props.orderData.status === 'string') {
		const strMap = {
			pending: '待接单',
			accepted: '待服务',
			in_progress: '服务中',
			completed: '已完成',
			cancelled: '已取消'
		}
		return strMap[props.orderData.status] || '待接单'
	}
	return statusMap[props.orderData.orderStatus] || '待接单'
})

const mainActionText = computed(() => {
	const actionMap = {
		accept: '接单',
		start: '开始服务',
		complete: '完成服务',
		view: '查看详情'
	}
	return actionMap[props.actionType] || '接单'
})

// 陪诊类型颜色：根据服务类型文本匹配
const serviceTypeColorClass = computed(() => {
	const t = (props.orderData.serviceType || '').trim()
	if (t.includes('普通')) return 'type-normal'
	if (t.includes('术后')) return 'type-postop'
	if (t.includes('急诊')) return 'type-emergency'
	if (t.includes('上门')) return 'type-home'
	return 'type-default'
})

// 方法定义
// 卡片点击
const handleCardClick = () => {
	emit('card-click', props.orderData)
}

// 查看详情
const handleViewDetail = () => {
	// 如果父组件没有监听 view-detail，则默认跳转
	// 这里我们触发事件，让父组件处理跳转逻辑
	// 或者直接在这里跳转：
	uni.navigateTo({
		url: `/subpkg/order/detail?orderId=${props.orderData.orderId || props.orderData.id}`
	})
	emit('view-detail', props.orderData)
}

// 主要操作
const handleMainAction = () => {
	emit('main-action', {
		type: props.actionType,
		data: props.orderData
	})
}
</script>

<style lang="scss" scoped>
.order-card {
	background: #ffffff;
	border-radius: 20rpx;
	padding: 30rpx;
	margin-bottom: 24rpx;
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
	position: relative;
	transition: all 0.3s ease;
	border: 1rpx solid #f0f0f0;
	
	&:active {
		transform: scale(0.98);
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
	}
}

.card-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
	
	.user-info {
		display: flex;
		align-items: center;
		flex: 1;
		
		.avatar {
			width: 48px;
			height: 48px;
			border-radius: 24px;
			margin-right: 12px;
			border: none;
			overflow: hidden;
		}
		
		.user-details {
			flex: 1;
			
			.name-row {
				display: flex;
				align-items: center;
				margin-bottom: 6px;

				.username {
					font-size: 16px;
					font-weight: 600;
					color: #333333;
					margin-right: 8px;
				}

				.tags {
					display: flex;
					gap: 4px;

					.tag {
						font-size: 10px;
						padding: 1px 4px;
						border-radius: 4px;

						&.age {
							background-color: #e6f7ff;
							color: #1890ff;
						}

						&.gender {
							background-color: #fff0f6;
							color: #eb2f96;
						}
					}
				}
			}
			
			.service-type-box {
				display: inline-block;
				border-radius: 6px;
				padding: 3px 10px;
				border: 1px solid transparent;

				.service-type {
					font-size: 12px;
					font-weight: 500;
				}

				&.type-normal {
					background: #e6f7ff;
					border-color: #91d5ff;
					.service-type { color: #1890ff; }
				}
				&.type-postop {
					background: #f9f0ff;
					border-color: #d3adf7;
					.service-type { color: #722ed1; }
				}
				&.type-emergency {
					background: #fff7e6;
					border-color: #ffd591;
					.service-type { color: #fa8c16; }
				}
				&.type-home {
					background: #e6fffb;
					border-color: #87e8de;
					.service-type { color: #13c2c2; }
				}
				&.type-default {
					background: #f5f5f5;
					border-color: #d9d9d9;
					.service-type { color: #8c8c8c; }
				}
			}
		}
	}
	
	.price {
		font-size: 18px;
		font-weight: 700;
		color: #ff4d4f;
	}
}

.service-info {
	margin-bottom: 12px;
	
	.info-row {
		display: flex;
		align-items: flex-start;
		margin-bottom: 8px;
		
		&:last-child {
			margin-bottom: 0;
		}
		
		.info-icon {
			width: 16px;
			height: 16px;
			margin-right: 8px;
			opacity: 0.8;
		}
		
		.info-text {
			flex: 1;
			font-size: 14px;
			color: #334155;
			line-height: 1.4;
		}

		&:first-child .info-text {
			font-size: 15px;
			font-weight: 600;
			color: #111827;
		}

		&:nth-child(2) .info-text {
			color: #4A90E2;
		}
	}
}

.special-needs {
	display: flex;
	flex-direction: column;
	gap: 10px;
	margin-bottom: 12px;
	
	.needs-item {
		display: flex;
		align-items: flex-start;
		gap: 10px;
		background: #f8fafc;
		border: 1px solid #e2e8f0;
		border-radius: 10px;
		padding: 10px 12px;
	}
	
	.needs-icon {
		width: 18px;
		height: 18px;
		flex-shrink: 0;
		margin-top: 2px;
	}
	
	.needs-content {
		flex: 1;
		min-width: 0;
	}
	
	.needs-label {
		display: block;
		font-size: 11px;
		color: #94a3b8;
		margin-bottom: 2px;
	}
	
	.needs-value {
		font-size: 13px;
		color: #334155;
		line-height: 1.4;
		word-break: break-all;
	}
}

.status-tag {
	position: absolute;
	top: var(--spacing-md);
	right: var(--spacing-md);
	padding: var(--spacing-xs) var(--spacing-sm);
	border-radius: var(--radius-medium);
	font-size: 10px;
	font-weight: 600;
	
	&.status-pending {
		background-color: rgba(25, 118, 210, 0.1);
		color: #1976d2;
	}
	
	&.status-accepted {
		background-color: rgba(46, 125, 50, 0.1);
		color: #2e7d32;
	}
	
	&.status-progress {
		background-color: rgba(245, 124, 0, 0.1);
		color: #f57c00;
	}
	
	&.status-completed {
		background-color: rgba(46, 125, 50, 0.1);
		color: #2e7d32;
	}
	
	&.status-cancelled {
		background-color: rgba(198, 40, 40, 0.1);
		color: #c62828;
	}
}

.card-actions {
	display: flex;
	gap: 12px;
	margin-top: 12px;
	padding-top: 12px;
	border-top: 1px solid #f0f0f0;
	
	.action-btn {
		flex: 1;
		height: 36px;
		border-radius: 18px;
		font-size: 14px;
		font-weight: 500;
		border: none;
		transition: all 0.3s ease;
		cursor: pointer;
		position: relative;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		
		&:active {
			transform: scale(0.95);
		}
		
		&.secondary {
			background: #f8f9fa;
			color: #666666;
			border: 1px solid #e9ecef;
			
			&:active {
				background: #e9ecef;
			}
		}
		
		&.primary {
			background: #4A90E2;
			color: #ffffff;
			
			&:active {
				background: #357abd;
			}
		}
		
		&:disabled {
			background: #f5f5f5;
			color: #cccccc;
			cursor: not-allowed;
			transform: none;
		}
		
		.btn-icon {
			width: 16px;
			height: 16px;
		}
	}
}

// 响应式适配
@media (max-width: 375px) {
	.order-card {
		padding: var(--spacing-md);
		margin-bottom: 10px;
	}
	
	.card-header {
		.user-info .avatar {
			width: 40px;
			height: 40px;
			border-radius: 20px;
		}
		
		.price {
			font-size: 18px;
		}
	}
	
	.service-info .info-row {
		.info-text {
			font-size: 13px;
		}
	}
	
	.card-actions .action-btn {
		height: 32px;
		font-size: 13px;
	}
}

// 动画效果
.order-card {
	animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
	from {
		opacity: 0;
		transform: translateY(10px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>