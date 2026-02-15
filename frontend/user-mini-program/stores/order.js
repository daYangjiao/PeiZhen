import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useOrderStore = defineStore('order', () => {
	// 订单列表
	const orders = ref([])
	
	// 计算属性 - 按状态分类的订单
	const ongoingOrders = computed(() => 
		orders.value.filter(order => order.status === 'ongoing')
	)
	
	const completedOrders = computed(() => 
		orders.value.filter(order => order.status === 'completed')
	)
	
	const pendingOrders = computed(() => 
		orders.value.filter(order => order.status === 'pending')
	)
	
	const historyOrders = computed(() => 
		orders.value.filter(order => order.status === 'completed' || order.status === 'cancelled')
	)
	
	// 添加新订单
	const addOrder = (orderData) => {
		const newOrder = {
			id: Date.now().toString(),
			status: 'ongoing',
			createdAt: new Date().toISOString(),
			...orderData
		}
		orders.value.unshift(newOrder)
		
		// 保存到本地存储
		saveToStorage()
	}

	// 取消订单
	const cancelOrder = (orderId) => {
		// 从订单列表中找到要取消的订单
		const orderIndex = orders.value.findIndex(order => order.id === orderId)
		if (orderIndex !== -1) {
			const canceledOrder = orders.value[orderIndex]
			// 更新订单状态为已取消
			canceledOrder.status = 'cancelled'
			canceledOrder.cancelTime = new Date().toISOString()
			// 保存到本地存储
			saveToStorage()
			return true
		}
		return false
	}
	const updateOrderStatus = (orderId, newStatus) => {
		const order = orders.value.find(o => o.id === orderId)
		if (order) {
			order.status = newStatus
			order.updatedAt = new Date().toISOString()
			saveToStorage()
		}
	}
	
	// 删除订单
	const removeOrder = (orderId) => {
		const index = orders.value.findIndex(o => o.id === orderId)
		if (index > -1) {
			orders.value.splice(index, 1)
			saveToStorage()
		}
	}
	
	// 获取订单详情
	const getOrderById = (orderId) => {
		return orders.value.find(o => o.id === orderId)
	}
	
	// 保存到本地存储
	const saveToStorage = () => {
		try {
			uni.setStorageSync('orders', JSON.stringify(orders.value))
		} catch (e) {
			console.error('保存订单数据失败:', e)
		}
	}
	
	// 从本地存储加载
	const loadFromStorage = () => {
		try {
			const storedOrders = uni.getStorageSync('orders')
			if (storedOrders) {
				orders.value = JSON.parse(storedOrders)
			}
		} catch (e) {
			console.error('加载订单数据失败:', e)
		}
	}
	
	// 清空所有订单
	const clearAllOrders = () => {
		orders.value = []
		uni.removeStorageSync('orders')
	}
	
	// 获取订单统计信息
	const getOrderStats = computed(() => {
		return {
			total: orders.value.length,
			ongoing: ongoingOrders.value.length,
			completed: completedOrders.value.length,
			pending: pendingOrders.value.length
		}
	})
	
	return {
			// 状态
			orders,
			
			// 计算属性
			ongoingOrders,
			completedOrders,
			pendingOrders,
			historyOrders,
			getOrderStats,
			
			// 方法
			addOrder,
			cancelOrder,
			updateOrderStatus,
			removeOrder,
			getOrderById,
			loadFromStorage,
			clearAllOrders
		}
})