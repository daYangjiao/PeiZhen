<template>
	<view class="container">
		<!-- 用户信息区域 -->
		<view class="user-section">
			<!-- 已登录状态 -->
			<view v-if="userStore.isLoggedIn" class="user-info">
				<view class="user-avatar">
					<image 
						class="avatar-img" 
						:src="avatarDisplayUrl" 
						mode="aspectFill"
						@error="onAvatarError"
					></image>
				</view>
				<view class="user-details">
					<text class="user-name">{{ userStore.displayName }}</text>
					<text class="user-desc">{{ userStore.userInfo && userStore.userInfo.gender === 1 ? '先生' : userStore.userInfo && userStore.userInfo.gender === 2 ? '女士' : '' }}</text>
				</view>
				<view class="user-actions">
					<view class="action-btn" @click="editProfile">
						<text class="action-text">编辑</text>
					</view>
				</view>
			</view>
			
			<!-- 未登录状态 -->
			<view v-else class="login-prompt">
				<view class="prompt-icon">
					<image class="icon-img" src="/static/user-placeholder.png" mode="aspectFit"></image>
				</view>
				<text class="prompt-title">您还未登录</text>
				<text class="prompt-desc">登录后可享受更多个性化服务</text>
				<button class="login-btn" @click="goToLogin">
					<text class="btn-text">立即登录</text>
				</button>
			</view>
		</view>
		
		<!-- 功能菜单 -->
		<view class="menu-section">
			<view class="menu-group">
				<text class="group-title">我的服务</text>
				<view class="menu-list">
					<view class="menu-item" @click="goToOrders">
						<view class="item-icon">
							<image src="/static/order.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">我的订单</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToAppointments">
						<view class="item-icon">
							<image src="/static/yvyue_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">我的预约</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToMessages">
						<view class="item-icon">
							<image src="/static/xiaoxi_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">消息中心</text>
						<text class="item-arrow">〉</text>
					</view>
				</view>
			</view>
			
			<view class="menu-group">
				<text class="group-title">设置</text>
				<view class="menu-list">
					<view class="menu-item" @click="goToSettings">
						<view class="item-icon">
							<image src="/static/settings.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">设置</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToHelp">
						<view class="item-icon">
							<image src="/static/help.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">帮助与反馈</text>
						<text class="item-arrow">〉</text>
					</view>
					<view v-if="userStore.isLoggedIn" class="menu-item" @click="handleLogout">
						<view class="item-icon">
							<image src="/static/wode_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title logout-text">退出登录</text>
						<text class="item-arrow">〉</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { config } from '@/utils/api.js'
import { getUserById } from '@/api/user.js'

// 占位图（本地静态资源，一定可显示）
const PLACEHOLDER_AVATAR = '/static/user-placeholder.png'

// 直接使用 store，保证头像等字段响应式更新
const userStore = useUserStore()
const avatarLoadFailed = ref(false)

// 与陪诊师端一致：数据库存 /uploads/xxx.jpg → 完整 URL http://localhost:8080/uploads/xxx.jpg
const getFullAvatarUrl = (relativePath) => {
	if (!relativePath || typeof relativePath !== 'string') return PLACEHOLDER_AVATAR
	const path = String(relativePath).trim()
	if (!path) return PLACEHOLDER_AVATAR
	if (path.startsWith('http')) return path
	const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/'
	const normalized = path.startsWith('/') ? path.substring(1) : path
	return baseUrl + normalized
}

// 实际展示的头像地址：有头像且未加载失败时用完整 URL，否则用占位图
const avatarDisplayUrl = computed(() => {
	if (avatarLoadFailed.value) return PLACEHOLDER_AVATAR
	const avatar = userStore.avatar || userStore.userInfo?.avatar || ''
	return getFullAvatarUrl(avatar)
})

const onAvatarError = () => {
	avatarLoadFailed.value = true
}

// 头像来源变化时重置加载失败状态（例如上传新头像或从接口拉取到头像后）
watch(() => userStore.avatar, () => {
	avatarLoadFailed.value = false
})

// 方法
// 跳转到登录页面
const goToLogin = () => {
	uni.navigateTo({
		url: '/subpkg/auth/login?redirect=' + encodeURIComponent('/pages/profile/profile')
	})
}

// 编辑个人资料
const editProfile = () => {
	console.log('🚀🚀🚀 点击了编辑按钮，准备跳转到编辑页面 🚀🚀🚀');
	uni.navigateTo({
		url: '/subpkg/profile/edit-profile'
	});
}

// 跳转到订单页面
const goToOrders = () => {
	if (!userStore.checkLoginStatus('/pages/order/order')) {
		return
	}
	uni.switchTab({
		url: '/pages/order/order'
	});
}

// 跳转到预约页面
const goToAppointments = () => {
	if (!userStore.checkLoginStatus('/pages/appointment/appointment')) {
		return
	}
	uni.switchTab({
		url: '/pages/appointment/appointment'
	});
}

// 跳转到消息页面
const goToMessages = () => {
	if (!userStore.checkLoginStatus('/pages/message/message')) {
		return
	}
	uni.switchTab({
		url: '/pages/message/message'
	});
}

// 跳转到设置页面
const goToSettings = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	});
}

// 跳转到帮助页面
const goToHelp = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	});
}

// 退出登录
const handleLogout = () => {
	uni.showModal({
		title: '提示',
		content: '确定要退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				userStore.logout()
			}
		}
	})
}

// 获取用户详细信息（含头像），拉取后 store 会更新，头像会自动刷新
const fetchUserDetail = async () => {
	const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
	if (!userStore.isLoggedIn || !uid) return
	try {
		const userDetailResponse = await getUserById(uid)
		if (userDetailResponse.data) {
			const updatedUserInfo = {
				...userStore.userInfo,
				name: userDetailResponse.data.name,
				username: userDetailResponse.data.username,
				avatar: userDetailResponse.data.avatar
			}
			userStore.setUserInfo(updatedUserInfo)
		}
	} catch (error) {
		console.error('获取用户详细信息失败:', error)
	}
}

// 生命周期
onMounted(() => {
	userStore.restoreFromStorage()
	fetchUserDetail()
})

// 页面显示时刷新用户信息（从编辑页返回或切 Tab 回来时能看到最新头像）
onShow(() => {
	userStore.restoreFromStorage()
	avatarLoadFailed.value = false
	fetchUserDetail()
})
</script>

<style>
.container {
	min-height: 100vh;
	background-color: #f5f5f5;
	padding: 20rpx;
}

/* 用户信息区域 */
.user-section {
	background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
	border-radius: 20rpx;
	padding: 40rpx;
	margin-bottom: 30rpx;
	color: white;
}

/* 已登录用户信息 */
.user-info {
	display: flex;
	align-items: center;
}

.user-avatar {
	margin-right: 30rpx;
}

.avatar-img {
	width: 120rpx;
	height: 120rpx;
	border-radius: 60rpx;
	border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.user-details {
	flex: 1;
}

.user-name {
	font-size: 36rpx;
	font-weight: bold;
	color: white;
	display: block;
	margin-bottom: 10rpx;
}

.user-desc {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.8);
	display: block;
}

.user-actions {
	margin-left: 20rpx;
}

.action-btn {
	background: rgba(255, 255, 255, 0.2);
	border-radius: 30rpx;
	padding: 15rpx 30rpx;
	border: 2rpx solid rgba(255, 255, 255, 0.3);
}

.action-text {
	color: white;
	font-size: 24rpx;
}

/* 未登录提示 */
.login-prompt {
	text-align: center;
	padding: 40rpx 0;
}

.prompt-icon {
	margin-bottom: 30rpx;
}

.icon-img {
	width: 120rpx;
	height: 120rpx;
	opacity: 0.8;
}

.prompt-title {
	font-size: 32rpx;
	font-weight: bold;
	color: white;
	display: block;
	margin-bottom: 15rpx;
}

.prompt-desc {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.8);
	display: block;
	margin-bottom: 40rpx;
}

.login-btn {
	background: white;
	border-radius: 50rpx;
	padding: 20rpx 60rpx;
	border: none;
	margin: 0;
}

.login-btn::after {
	border: none;
}

.btn-text {
	color: #4A90E2;
	font-size: 28rpx;
	font-weight: bold;
}

/* 功能菜单 */
.menu-section {
	background: white;
	border-radius: 20rpx;
	padding: 30rpx;
}

.menu-group {
	margin-bottom: 40rpx;
}

.menu-group:last-child {
	margin-bottom: 0;
}

.group-title {
	font-size: 28rpx;
	font-weight: bold;
	color: #333;
	display: block;
	margin-bottom: 20rpx;
	padding-left: 10rpx;
}

.menu-list {
	background: #f8f9fa;
	border-radius: 15rpx;
	overflow: hidden;
}

.menu-item {
	display: flex;
	align-items: center;
	padding: 30rpx 20rpx;
	background: white;
	margin-bottom: 2rpx;
	position: relative;
}

.menu-item:last-child {
	margin-bottom: 0;
}

.item-icon {
	width: 50rpx;
	height: 50rpx;
	margin-right: 30rpx;
}

.item-icon image {
	width: 100%;
	height: 100%;
}

.item-title {
	flex: 1;
	font-size: 28rpx;
	color: #333;
}

.logout-text {
	color: #ff4757;
}

.item-arrow {
	font-size: 24rpx;
	color: #999;
	font-weight: bold;
}
</style>