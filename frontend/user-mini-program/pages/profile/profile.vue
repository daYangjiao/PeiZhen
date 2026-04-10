<template>
	<view class="container">
		<!-- 用户信息区域 -->
		<view class="user-section">
			<!-- 已登录状态 -->
			<view v-if="userStore && userStore.isLoggedIn" class="user-info">
				<view class="user-avatar">
					<image 
						class="avatar-img" 
						:src="userStore.avatar || '/static/user-placeholder.png'" 
						mode="aspectFill"
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
          <view class="menu-item" @click="goToRules">
            <view class="item-icon">
              <image src="/static/settings.png" mode="aspectFit"></image>
            </view>
            <text class="item-title">平台规则</text>
            <text class="item-arrow">〉</text>
          </view>

				</view>
			</view>
			
			<view class="menu-group">
				<text class="group-title">更多功能</text>
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
					<view v-if="userStore && userStore.isLoggedIn" class="menu-item" @click="handleLogout">
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
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

// 响应式数据
const userStore = ref(null)

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
	if (!userStore.value.checkLoginStatus('/pages/order/order')) {
		return
	}
	uni.switchTab({
		url: '/pages/order/order'
	});
}

// 跳转到预约页面
const goToAppointments = () => {
	if (!userStore.value.checkLoginStatus('/pages/appointment/appointment')) {
		return
	}
	uni.switchTab({
		url: '/pages/appointment/appointment'
	});
}

// 跳转到消息页面
const goToMessages = () => {
	if (!userStore.value.checkLoginStatus('/pages/message/message')) {
		return
	}
	uni.switchTab({
		url: '/pages/message/message'
	});
}
//跳转到平台规则
// 跳转到平台规则页面
const goToRules = () => {
  uni.navigateTo({
    url: '/pages/rules/rules'
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
				userStore.value.logout()
			}
		}
	})
}

// 获取用户详细信息
const fetchUserDetail = async () => {
	if (userStore.value && userStore.value.isLoggedIn && userStore.value.userInfo && userStore.value.userInfo.userId) {
		try {
			// 导入getUserById方法
			const { getUserById } = require('@/api/user.js');
			
			// 获取详细用户信息
			const userDetailResponse = await getUserById(userStore.value.userInfo.userId);
			
			if (userDetailResponse.data) {
				// 更新用户信息
				const updatedUserInfo = {
					...userStore.value.userInfo,
					name: userDetailResponse.data.name,
					username: userDetailResponse.data.username
					// 可以根据需要更新更多字段
				};
				
				// 更新store中的用户信息
				userStore.value.setUserInfo(updatedUserInfo);
			}
		} catch (error) {
			console.error('获取用户详细信息失败:', error);
		}
	}
};

// 生命周期
onMounted(() => {
	// 初始化用户store
	userStore.value = useUserStore()
	// 从本地存储恢复状态
	userStore.value.restoreFromStorage()
	// 获取用户详细信息
	fetchUserDetail()
})

// 页面显示时检查登录状态并更新用户信息
const onShow = () => {
	if (userStore.value) {
		userStore.value.restoreFromStorage()
		// 每次页面显示时获取最新用户信息
		fetchUserDetail()
	}
}
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