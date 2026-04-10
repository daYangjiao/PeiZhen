<template>
	<view class="container">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<text class="navbar-title">陪诊接单大厅</text>
				<view class="navbar-right">
					<text class="refresh-icon" @click="refreshOrders">🔄</text>
				</view>
			</view>
		</view>
		
		<!-- 订单列表 -->
		<scroll-view 
			class="order-list" 
			scroll-y="true"
			@scrolltolower="loadMore"
			refresher-enabled="true"
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
		>
			<view v-if="orderList.length === 0 && !isLoading" class="empty-state">
				<text class="empty-icon">📭</text>
				<text class="empty-text">暂无待接订单，稍后再来看看吧</text>
			</view>

			<OrderCard
				v-for="(order, index) in orderList" 
				:key="order.orderId"
				:order-data="formatOrderData(order)"
				:show-actions="true"
				:action-type="'accept'"
				@contact="contactPatient"
				@main-action="handleAccept"
			/>

			<view class="loading-more" v-if="isLoading">
				<text>加载中...</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import OrderCard from '@/components/OrderCard.vue'
import { get, post } from '@/utils/api.js'

const orderList = ref([])
const isLoading = ref(false)
const isRefreshing = ref(false)
const page = ref(0)

// 格式化后端数据以适配组件
const formatOrderData = (raw) => {
	return {
		id: raw.orderId,
		orderId: raw.orderId,
		userName: raw.patientName || raw.contactPerson || '匿名患者',
		userAge: raw.patientAge || '--', // 假设后端返回了 patientAge
		userGender: raw.patientSex || '未知', // 假设后端返回了 patientSex
		userAvatar: '/static/user-placeholder.png',
		serviceType: raw.serviceContent || raw.serviceTypeName || '陪诊服务',
		hospitalName: raw.hospital || '未知医院',
		appointmentTime: (raw.serviceDate || '') + ' ' + (raw.serviceTimeSlot || ''),
		price: raw.orderAmount || 0,
		specialNote: raw.customRequirement || raw.otherRequirement || '无特殊要求',
		phone: raw.contactPhone || raw.userPhone,
		orderStatus: raw.orderStatus // 确保传递 orderStatus
	}
}

// 加载订单列表
const loadOrders = async (reset = false) => {
	if (isLoading.value) return
	if (reset) {
		page.value = 0
		orderList.value = []
	}

	isLoading.value = true
	try {
		// 调用后端 AttendantController 的待接单接口
		const res = await get('/attendant/orders/waiting', {
			page: page.value,
			size: 10
		})

		if (res.code === 200 && res.data) {
			const newOrders = res.data.content || [] // 修正：PagedResponse 的字段是 content
			if (reset) {
				orderList.value = newOrders
			} else {
				orderList.value = [...orderList.value, ...newOrders]
			}
			if (newOrders.length > 0) page.value++
		}
	} catch (e) {
		console.error('加载订单失败:', e)
	} finally {
		isLoading.value = false
		isRefreshing.value = false
	}
}

const onRefresh = () => {
	isRefreshing.value = true
	loadOrders(true)
}

const refreshOrders = () => {
	onRefresh()
}

const loadMore = () => {
	loadOrders()
}

// 接单处理
const handleAccept = (actionData) => {
	// actionData 是子组件传递过来的对象，包含 { type: 'accept', data: orderData }
	// 这里我们需要的是 orderData
	const order = actionData.data || actionData // 兼容直接传递 order 对象的情况
	const attendantInfo = uni.getStorageSync('userInfo')

	if (!attendantInfo || !attendantInfo.id) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}

	uni.showModal({
		title: '确认接单',
		content: `确定要接受该订单吗？`,
		success: async (res) => {
			if (res.confirm) {
				try {
					const response = await post(`/attendant/orders/${order.id}/accept?attendantId=${attendantInfo.id}`)
					if (response.code === 200) {
						uni.showToast({ title: '接单成功', icon: 'success' })
						// 刷新列表
						loadOrders(true)
						// 跳转到我的订单
						setTimeout(() => {
							uni.switchTab({ url: '/pages/order/order' })
						}, 1500)
					} else {
						// 处理非 200 的情况
						let errorMsg = response.message || '接单失败'
						
						// 根据不同错误类型提供更友好的提示
						if (errorMsg.includes('订单不存在')) {
							errorMsg = '订单已失效，请刷新页面'
						} else if (errorMsg.includes('无效的陪诊师ID')) {
							errorMsg = '账号信息异常，请重新登录'
						} else if (errorMsg.includes('订单当前状态无法接单')) {
							errorMsg = '订单状态异常，请稍后再试'
						} else if (errorMsg.includes('用户类型不支持接单')) {
							errorMsg = '账号权限不足，请联系管理员'
						}
						
						uni.showToast({ title: errorMsg, icon: 'none' })
					}
				} catch (e) {
					console.error('接单失败:', e)
					// 如果是 API 拦截器抛出的错误，通常已经包含了 message
					const msg = e.message || '接单失败，请稍后重试'
					uni.showToast({ title: msg, icon: 'none' })
				}
			}
		}
	})
}

const contactPatient = (order) => {
	if (order.phone) {
		uni.makePhoneCall({ phoneNumber: order.phone })
	} else {
		uni.showToast({ title: '暂无联系电话', icon: 'none' })
	}
}

onMounted(() => {
	loadOrders(true)
})
</script>

<style lang="scss" scoped>
.container {
	background-color: #f5f5f5;
	min-height: 100vh;
}

.custom-navbar {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 999;
	background-color: #ffffff;
	padding-top: var(--status-bar-height);
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
	
	.navbar-content {
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 16px;
		
		.navbar-title {
			font-size: 18px;
			font-weight: 600;
			color: #333333;
		}
		
		.refresh-icon {
			font-size: 20px;
			padding: 8px;
		}
	}
}

.order-list {
	height: 100vh;
	padding: calc(var(--status-bar-height) + 54px) 16px 20px;
	box-sizing: border-box;
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding-top: 100px;

	.empty-icon {
		font-size: 60px;
		margin-bottom: 20px;
	}

	.empty-text {
		color: #999;
		font-size: 14px;
	}
}

.loading-more {
	text-align: center;
	padding: 20px;
	color: #999;
	font-size: 14px;
}
</style>
