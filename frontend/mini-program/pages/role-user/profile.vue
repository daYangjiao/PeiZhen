<template>
	<view class="container">
		<view class="user-section">
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
					<view class="action-btn" @click="handleEditProfile">
						<text class="action-text">编辑</text>
					</view>
				</view>
			</view>
			
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
import { ensureRole } from '@/utils/auth-guard.js'

const PLACEHOLDER_AVATAR = '/static/user-placeholder.png'
const userStore = useUserStore()
const avatarLoadFailed = ref(false)

const getFullAvatarUrl = (relativePath) => {
	if (!relativePath || typeof relativePath !== 'string') return PLACEHOLDER_AVATAR
	const path = String(relativePath).trim()
	if (!path) return PLACEHOLDER_AVATAR
	if (path.startsWith('http')) return path
	const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/'
	const normalized = path.startsWith('/') ? path.substring(1) : path
	return baseUrl + normalized
}

const avatarDisplayUrl = computed(() => {
	if (avatarLoadFailed.value) return PLACEHOLDER_AVATAR
	const avatar = userStore.avatar || userStore.userInfo?.avatar || ''
	return getFullAvatarUrl(avatar)
})

const onAvatarError = () => {
	avatarLoadFailed.value = true
}

watch(() => userStore.avatar, () => {
	avatarLoadFailed.value = false
})

const goToLogin = () => {
	uni.navigateTo({ url: '/pages/auth/login?role=user' })
}

const handleEditProfile = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/profile')) return
	uni.navigateTo({ url: '/subpkg/profile/edit-profile' })
}

const goToOrders = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/order')) return
	uni.switchTab({
		url: '/pages/role-user/order'
	})
}

const goToAppointments = () => {
	uni.showToast({ title: '预约列表待接入', icon: 'none' })
}

const goToMessages = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/message')) return
	uni.switchTab({
		url: '/pages/role-user/message'
	})
}

const goToSettings = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	})
}

const goToHelp = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	})
}

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

onMounted(() => {
	userStore.restoreFromStorage()
	fetchUserDetail()
})

onShow(() => {
	const fromRoute = uni.getStorageSync('guard_from_route')
	if (!userStore.isLoggedIn && (fromRoute === 'pages/role-user/profile' || fromRoute === '/pages/role-user/profile')) {
		uni.removeStorageSync('guard_from_route')
		uni.switchTab({ url: '/pages/role-user/home' })
		return
	}
	if (!ensureRole('user')) return
	userStore.restoreFromStorage()
	avatarLoadFailed.value = false
	fetchUserDetail()
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.container {
	min-height: 100vh;
	background-color: #f5f7fa;
	padding: 20rpx;
}
.user-section {
	background: linear-gradient(135deg, #66A6FF 0%, #4F95F0 100%);
	border-radius: 20rpx;
	padding: 40rpx;
	margin-bottom: 30rpx;
	color: white;
}
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
	color: #66A6FF;
	font-size: 28rpx;
	font-weight: bold;
}
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

