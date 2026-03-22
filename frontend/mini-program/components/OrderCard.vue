<template>
	<view class="order-card" @click="handleCardClick">
		<view class="card-header">
			<view class="user-info">
				<image class="avatar" :src="displayAvatar" mode="aspectFill" @error="onAvatarError"></image>
				<view class="user-details">
					<view class="name-row">
						<text class="display-name">{{ orderData.userName }}</text>
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
			<view class="income-chip">
				<text class="income-label">预计收入</text>
				<text class="income-value">¥{{ orderData.price }}</text>
			</view>
		</view>

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

		<view class="status-tag" :class="statusClass" v-if="showStatus">
			<text>{{ statusText }}</text>
		</view>

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
import { computed, ref } from 'vue'
// 通过 import 让构建时解析路径，避免 H5/小程序里 /static/ 路径不生效
import placeholderImg from '../static/user-placeholder.png'

const props = defineProps({
	orderData: { type: Object, required: true },
	showActions: { type: Boolean, default: true },
	showStatus: { type: Boolean, default: false },
	actionType: { type: String, default: 'accept' }
})

const emit = defineEmits(['card-click', 'view-detail', 'main-action'])

const avatarError = ref(false)
const displayAvatar = computed(() => {
	if (avatarError.value) return placeholderImg
	const url = props.orderData.userAvatar
	if (url && (url.startsWith('http') || url.startsWith('data:') || url.startsWith('blob:'))) return url
	// 无头像或为占位图路径时，使用 import 的占位图（构建时解析，避免 /static/ 不生效）
	if (!url || !String(url).trim() || String(url).includes('user-placeholder')) return placeholderImg
	return url
})
const onAvatarError = () => {
	avatarError.value = true
}

const statusClass = computed(() => {
	const statusMap = { 1: 'status-pending', 2: 'status-accepted', 3: 'status-progress', 6: 'status-completed', 7: 'status-cancelled' }
	if (typeof props.orderData.status === 'string') {
		const strMap = { pending: 'status-pending', accepted: 'status-accepted', in_progress: 'status-progress', completed: 'status-completed', cancelled: 'status-cancelled' }
		return strMap[props.orderData.status] || 'status-pending'
	}
	return statusMap[props.orderData.orderStatus] || 'status-pending'
})

const statusText = computed(() => {
	const statusMap = { 1: '待接单', 2: '待服务', 3: '服务中', 6: '已完成', 7: '已取消' }
	if (typeof props.orderData.status === 'string') {
		const strMap = { pending: '待接单', accepted: '待服务', in_progress: '服务中', completed: '已完成', cancelled: '已取消' }
		return strMap[props.orderData.status] || '待接单'
	}
	return statusMap[props.orderData.orderStatus] || '待接单'
})

const mainActionText = computed(() => {
	const actionMap = { accept: '接单', start: '开始服务', complete: '完成服务', view: '查看详情' }
	return actionMap[props.actionType] || '接单'
})

const serviceTypeColorClass = computed(() => {
	const t = (props.orderData.serviceType || '').trim()
	if (t.includes('普通')) return 'type-normal'
	if (t.includes('术后')) return 'type-postop'
	if (t.includes('急诊')) return 'type-emergency'
	if (t.includes('上门')) return 'type-home'
	return 'type-default'
})

const handleCardClick = () => emit('card-click', props.orderData)

const handleViewDetail = () => {
	// 交给父组件决定如何跳转（大厅、订单列表等场景可能不同）
	emit('view-detail', props.orderData)
}

const handleMainAction = () => {
	emit('main-action', { type: props.actionType, data: props.orderData })
}
</script>

<style lang="scss" scoped>
.order-card {
	background: #ffffff;
	border-radius: 16rpx;
	padding: 22rpx;
	margin-bottom: 18rpx;
	box-shadow: 0 10rpx 22rpx rgba(31, 41, 55, 0.08);
	position: relative;
	border: 1rpx solid #e8edf4;
	transition: transform 0.2s ease, box-shadow 0.2s ease;
	animation: cardIn 0.24s ease both;
	&:active {
		transform: scale(0.986);
		box-shadow: 0 12rpx 28rpx rgba(31, 41, 55, 0.12);
	}
}

.card-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 14rpx;
	.user-info {
		display: flex;
		align-items: center;
		flex: 1;
		.avatar {
			width: 78rpx;
			height: 78rpx;
			border-radius: 39rpx;
			margin-right: 14rpx;
			border: none;
			overflow: hidden;
		}
		.user-details {
			flex: 1;
			min-width: 0;
			.name-row {
				display: flex;
				align-items: center;
				margin-bottom: 8rpx;
				.display-name {
					font-size: 28rpx;
					font-weight: 600;
					color: #1f2937;
					margin-right: 8rpx;
					max-width: 210rpx;
					overflow: hidden;
					text-overflow: ellipsis;
					white-space: nowrap;
				}
				.tags {
					display: flex;
					gap: 6rpx;
					.tag {
						font-size: 19rpx;
						padding: 3rpx 9rpx;
						border-radius: 999rpx;
						&.age {
							background-color: #eef5ff;
							color: #007AFF;
						}
						&.gender {
							background-color: #fff1f5;
							color: #e85d88;
						}
					}
				}
			}
			.service-type-box {
				display: inline-flex;
				align-items: center;
				border-radius: 999rpx;
				padding: 5rpx 12rpx;
				border: 1px solid transparent;
				.service-type {
					font-size: 21rpx;
					font-weight: 500;
				}
				&.type-normal {
					background: #eef5ff;
					border-color: #b6d4ff;
					.service-type { color: #007AFF; }
				}
				&.type-postop {
					background: #f5f0ff;
					border-color: #d8c4ff;
					.service-type { color: #6f4acc; }
				}
				&.type-emergency {
					background: #fff3e8;
					border-color: #ffd3ad;
					.service-type { color: #f08a1f; }
				}
				&.type-home {
					background: #e8fbf6;
					border-color: #a8eddc;
					.service-type { color: #18a17f; }
				}
				&.type-default {
					background: #f2f4f7;
					border-color: #dbe1ea;
					.service-type { color: #6b7280; }
				}
			}
		}
	}
}

.income-chip {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 3rpx;
	margin-left: 10rpx;
	background: #f3f8ff;
	border: 1rpx solid #d9e9ff;
	border-radius: 10rpx;
	padding: 9rpx 11rpx;
	flex-shrink: 0;
}

.income-label {
	font-size: 19rpx;
	color: #6c7e96;
	line-height: 1;
}

.income-value {
	font-size: 31rpx;
	font-weight: 700;
	color: #2f74d7;
	line-height: 1.1;
}

.service-info {
	margin-bottom: 12rpx;
	.info-row {
		display: flex;
		align-items: center;
		margin-bottom: 8rpx;
		&:last-child {
			margin-bottom: 0;
		}
		.info-icon {
			width: 24rpx;
			height: 24rpx;
			margin-right: 8rpx;
			opacity: 0.8;
		}
		.info-text {
			flex: 1;
			font-size: 23rpx;
			color: #334155;
			line-height: 1.4;
		}
	}
}

.special-needs {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
	margin-bottom: 12rpx;
}

.needs-item {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 9rpx 10rpx;
	border-radius: 10rpx;
	background: #f7fbff;
	border: 1rpx solid #e2eefb;
}

.needs-icon {
	width: 24rpx;
	height: 24rpx;
	opacity: 0.75;
	flex-shrink: 0;
}

.needs-content {
	flex: 1;
	min-width: 0;
}

.needs-label {
	display: block;
	font-size: 18rpx;
	color: #8090a8;
	margin-bottom: 2rpx;
}

.needs-value {
	flex: 1;
	font-size: 22rpx;
	color: #4b5a70;
	line-height: 1.35;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-actions {
	display: flex;
	gap: 10rpx;
	margin-top: 10rpx;
	padding-top: 12rpx;
	border-top: 1rpx solid #edf2f8;
	.action-btn {
		flex: 1;
		height: 64rpx;
		border-radius: 32rpx;
		font-size: 24rpx;
		font-weight: 500;
		border: none;
		transition: all 0.2s ease;
		position: relative;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8rpx;
		&::after {
			border: none;
		}
		&:active {
			transform: scale(0.97);
		}
		&.secondary {
			background: #f3f6fb;
			color: #5b6575;
			border: 1rpx solid #dfe7f1;
			&:active {
				background: #e8eef7;
			}
		}
		&.primary {
			background: linear-gradient(135deg, #69B2FF, #007AFF);
			color: #ffffff;
			box-shadow: 0 6rpx 16rpx rgba(102, 166, 255, 0.25);
			&:active {
				background: linear-gradient(135deg, #6eaefb, #5e9ff2);
			}
		}
		.btn-icon {
			width: 22rpx;
			height: 22rpx;
		}
	}
}

.status-tag {
	position: absolute;
	top: 18rpx;
	right: 18rpx;
	border-radius: 999rpx;
	padding: 6rpx 14rpx;
	font-size: 20rpx;
	background: #edf2f7;
	color: #64748b;
}

@keyframes cardIn {
	from {
		opacity: 0;
		transform: translateY(10rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}
</style>
