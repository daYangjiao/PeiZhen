<template>
	<view class="container">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="nav-left"></view>
				<view class="nav-title">我的订单</view>
				<view class="nav-right"></view>
			</view>
		</view>
		
		<!-- 状态筛选 -->
		<view class="status-tabs">
			<view 
				class="tab-item" 
				:class="{ active: currentTab === index }"
				v-for="(tab, index) in tabs" 
				:key="index"
				@click="switchTab(index)"
			>
				<text>{{ tab.name }}</text>
			</view>
		</view>
		
		<!-- 订单列表 -->
		<scroll-view
			class="order-list"
			scroll-y
			refresher-enabled
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
		>
			<view class="order-wrapper" v-for="order in orderList" :key="order.orderId" @click="goToDetail(order.orderId)">
				<!-- 订单头部 -->
				<view class="order-header">
					<text class="order-no">订单号：{{ order.orderNo }}</text>
					<view class="status-tag" :class="'status-' + order.orderStatus">
						<text>{{ getStatusText(order.orderStatus) }}</text>
					</view>
				</view>
				
				<view class="order-content">
					<view class="user-info">
						<image class="user-avatar" src="/static/user-placeholder.png" mode="aspectFill"></image>
						<view class="user-details">
							<text class="user-name">{{ order.patientName || order.contactPerson }}</text>
							<text class="service-type">{{ order.serviceContent || order.serviceTypeName }}</text>
						</view>
					</view>
					<view class="order-info-grid">
						<view class="info-item">
							<text class="info-label">医院</text>
							<text class="info-value">{{ order.hospital }}</text>
						</view>
						<view class="info-item">
							<text class="info-label">报酬</text>
							<text class="info-value price">¥{{ order.orderAmount }}</text>
						</view>
					</view>
					<view class="appointment-time">
						<text class="time-label">预约时间：</text>
						<text class="time-value">{{ order.serviceDate }} {{ order.serviceTimeSlot || '' }}</text>
					</view>
				</view>

				<!-- 操作按钮 -->
				<view class="card-actions" v-if="order.orderStatus === 1 || order.orderStatus === 2">
					<button class="action-btn primary" @click.stop="handleAction(order)">
						{{ getActionBtnText(order.orderStatus) }}
					</button>
				</view>
			</view>
			
			<!-- 暂无数据 -->
			<view class="empty-state" v-if="orderList.length === 0 && !isLoading">
				<text class="empty-icon">📭</text>
				<text class="empty-text">暂无相关订单</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { get, post } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'

const currentTab = ref(0)
const tabs = ref([
	{ name: '全部', status: null },
	{ name: '待服务', status: 2 }, // 修正：待服务对应 orderStatus=2 (已接单)
	{ name: '进行中', status: 3 }, // 修正：进行中对应 orderStatus=3
	{ name: '已完成', status: 6 }  // 修正：已完成对应 orderStatus=6
])

const orderList = ref([])
const isLoading = ref(false)
const isRefreshing = ref(false)
let socketListener = null
let pollTimer = null

const fetchOrders = async () => {
	const userInfo = uni.getStorageSync('userInfo')
	if (!userInfo || !userInfo.id) return

	isLoading.value = true
	try {
		const res = await get('/attendant/orders', {
			attendantId: userInfo.id,
			orderStatus: tabs.value[currentTab.value].status,
			page: 0,
			size: 50
		})
		if (res.code === 200 && res.data) {
			orderList.value = res.data.content || [] // 修正：PagedResponse 的字段是 content
		}
	} catch (e) {
		console.error('获取订单失败:', e)
	} finally {
		isLoading.value = false
		isRefreshing.value = false
	}
}

const switchTab = (index) => {
	currentTab.value = index
	fetchOrders()
}

const onRefresh = () => {
	isRefreshing.value = true
	fetchOrders()
}

const getStatusText = (status) => {
	const map = { 1: '待接单', 2: '待服务', 3: '服务中', 4: '待确认', 5: '待补款', 6: '已完成', 7: '已取消' }
	return map[status] || '未知'
}

const getActionBtnText = (status) => {
	if (status === 2) return '开始服务'
	if (status === 3) return '完成服务'
	return ''
}

const handleAction = async (order) => {
	const title = order.orderStatus === 2 ? '确认开始服务？' : '确认完成服务？'
	const url = order.orderStatus === 2 ? `/attendant/orders/${order.orderId}/start` : `/attendant/orders/${order.orderId}/end?actualDuration=2`

	uni.showModal({
		title: '提示',
		content: title,
		success: async (res) => {
			if (res.confirm) {
				try {
					const response = await post(url)
					if (response.code === 200) {
						uni.showToast({ title: '操作成功', icon: 'success' })
						fetchOrders()
					}
				} catch (e) {}
			}
		}
	})
}

const goToDetail = (id) => {
	uni.navigateTo({ url: `/subpkg/order/detail?orderId=${id}` })
}

// WebSocket消息处理
const handleSocketMessage = (message) => {
	console.log('陪诊师订单页收到WebSocket消息:', message)
	
	// 处理订单状态变更消息
	if (message.type === 'NEW_ORDER' || 
	    message.type === 'ORDER_ASSIGNED' ||
	    message.type === 'SERVICE_STARTED' || 
	    message.type === 'SERVICE_COMPLETED') {
		
		console.log('检测到订单状态变更，刷新订单列表')
		// 延迟刷新确保数据库更新
		setTimeout(() => {
			fetchOrders()
		}, 1000)
	}
}

// 设置WebSocket监听
const setupWebSocketListener = () => {
	if (socketListener) {
		removeChatListener(socketListener)
	}
	socketListener = handleSocketMessage
	addChatListener(socketListener)
}

// 开始轮询
const startPolling = () => {
	// 清除之前的轮询
	if (pollTimer) {
		clearInterval(pollTimer)
	}
	
	// 每15秒轮询一次
	pollTimer = setInterval(() => {
		fetchOrders()
	}, 15000)
}

// 页面卸载时清理
onUnmounted(() => {
	if (socketListener) {
		removeChatListener(socketListener)
		socketListener = null
	}
	if (pollTimer) {
		clearInterval(pollTimer)
		pollTimer = null
	}
})

onMounted(() => {
	fetchOrders()
	setupWebSocketListener()
	startPolling()
})
</script>

<style lang="scss" scoped>
.container {
	min-height: 100vh;
	background-color: #f5f5f5;
}

.custom-navbar {
	position: sticky;
	top: 0;
	z-index: 100;
	background: #ffffff;
	padding-top: var(--status-bar-height);
	
	.navbar-content {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 44px;
		.nav-title { font-size: 18px; font-weight: bold; }
	}
}

.status-tabs {
	display: flex;
	background: #ffffff;
	position: sticky;
	top: calc(var(--status-bar-height) + 44px);
	z-index: 99;
	border-bottom: 1px solid #eee;

	.tab-item {
		flex: 1;
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 14px;
		color: #666;

		&.active {
			color: #4A90E2;
			font-weight: bold;
			position: relative;
			&::after {
				content: '';
				position: absolute;
				bottom: 0;
				width: 40%;
				height: 3px;
				background: #4A90E2;
				border-radius: 2px;
			}
		}
	}
}

.order-list {
	height: calc(100vh - var(--status-bar-height) - 88px);
	padding: 12px;
	box-sizing: border-box;
}

.order-wrapper {
	background: #ffffff;
	margin-bottom: 12px;
	border-radius: 12px;
	padding: 16px;

	.order-header {
		display: flex;
		justify-content: space-between;
		margin-bottom: 15px;
		.order-no { font-size: 13px; color: #999; }
		.status-tag {
			font-size: 12px;
			padding: 2px 8px;
			border-radius: 4px;
			&.status-1 { background: #e3f2fd; color: #4A90E2; }
			&.status-2 { background: #fff3e0; color: #ff9800; }
			&.status-3 { background: #e8f5e9; color: #4caf50; }
		}
	}

	.user-info {
		display: flex;
		align-items: center;
		margin-bottom: 15px;
		.user-avatar { width: 40px; height: 40px; border-radius: 20px; margin-right: 10px; }
		.user-name { font-size: 16px; font-weight: bold; display: block; }
		.service-type { font-size: 13px; color: #666; }
	}
	
	.order-info-grid {
		display: flex;
		background: #f8f9fa;
		padding: 12px;
		border-radius: 8px;
		margin-bottom: 12px;
		.info-item {
			flex: 1;
			.info-label { font-size: 12px; color: #999; display: block; }
			.info-value { font-size: 14px; color: #333; font-weight: 500; }
			.price { color: #f44336; font-size: 16px; }
		}
	}
	
	.appointment-time {
		font-size: 13px;
		color: #666;
	}
	
	.card-actions {
		margin-top: 15px;
		padding-top: 15px;
		border-top: 1px solid #eee;
		display: flex;
		justify-content: flex-end;
		.action-btn {
			margin: 0;
			height: 32px;
			line-height: 32px;
			font-size: 13px;
			border-radius: 16px;
			background: #4A90E2;
			color: #fff;
		}
	}
}

.empty-state {
	text-align: center;
	padding-top: 100px;
	.empty-icon { font-size: 50px; display: block; margin-bottom: 10px; }
	.empty-text { color: #999; font-size: 14px; }
}
</style>
