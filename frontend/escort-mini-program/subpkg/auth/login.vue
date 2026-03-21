<template>
	<view class="container">
		<!-- 顶部装饰 -->
		<view class="header-section">
			<view class="circle-1"></view>
			<view class="circle-2"></view>
			<image class="logo" src="/static/brand-logo.png" mode="aspectFit"></image>
			<text class="welcome-text">欢迎回来，陪诊师</text>
		</view>
		
		<!-- 登录卡片 -->
		<view class="login-card">
			<view class="tab-header">
				<text class="tab-item active">账号登录</text>
			</view>

			<view class="input-group">
				<view class="input-item">
					<text class="iconfont">👤</text>
					<input class="input" v-model="form.username" placeholder="请输入用户名" />
				</view>
				<view class="input-item">
					<text class="iconfont">🔒</text>
					<input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
				</view>
			</view>

			<view class="agreement-row">
				<checkbox-group @change="onCheckChange">
					<label class="checkbox-label">
						<checkbox :checked="agreed" color="#4A90E2" style="transform:scale(0.7)" />
						<text class="agreement-text">我已阅读并同意<text class="link">《服务协议》</text></text>
					</label>
				</checkbox-group>
			</view>

			<button class="login-btn" @click="handleLogin">立即登录</button>

			<view class="footer-links">
				<text @click="goRegister">陪诊师入驻</text>
				<text class="divider">|</text>
				<text>找回密码</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { post, setToken } from '@/utils/api.js'

const form = ref({
	username: '',
	password: ''
})
const agreed = ref(false)

const onCheckChange = (e) => {
	agreed.value = e.detail.value.length > 0
}

const goRegister = () => {
	uni.navigateTo({ url: '/subpkg/auth/register' })
}

const handleLogin = async () => {
	if (!agreed.value) {
		uni.showToast({ title: '请先同意协议', icon: 'none' })
		return
	}
	if (!form.value.username || !form.value.password) {
		uni.showToast({ title: '请输入账号密码', icon: 'none' })
		return
	}

	uni.showLoading({ title: '登录中...' })
	try {
		const res = await post('/api/users/login', form.value)
		uni.hideLoading()

		if (res.code === 200 && res.data) {
			const { token, userInfo } = res.data
			if (userInfo.userType !== 1) {
				uni.showToast({ title: '非陪诊师账号', icon: 'none' })
				return
			}

			setToken(token)
			// 确保存储完整的 userInfo 对象，包括 id
			uni.setStorageSync('userInfo', userInfo)
			uni.setStorageSync('isLoggedIn', true)

			uni.showToast({ title: '登录成功', icon: 'success' })
			setTimeout(() => {
				uni.reLaunch({ url: '/pages/index/index' })
			}, 1500)
		} else {
            uni.showToast({ title: res.message || '登录失败', icon: 'none' })
        }
	} catch (e) {
		uni.hideLoading()
		uni.showToast({ title: e.message || '登录失败', icon: 'none' })
	}
}
</script>

<style lang="scss" scoped>
.container {
	min-height: 100vh;
	background-color: #f5f7fa;
	position: relative;
	overflow: hidden;
}

.header-section {
	height: 300px;
	background: linear-gradient(135deg, #4A90E2, #357ABD);
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 80px;
	position: relative;

	.circle-1 {
		position: absolute;
		width: 200px;
		height: 200px;
		background: rgba(255,255,255,0.1);
		border-radius: 50%;
		top: -50px;
		right: -50px;
	}
	
	.logo {
		width: 80px;
		height: 80px;
		background: #fff;
		border-radius: 20px;
		margin-bottom: 16px;
		box-shadow: 0 4px 12px rgba(0,0,0,0.1);
	}
	
	.welcome-text {
		color: #fff;
		font-size: 20px;
		font-weight: 500;
	}
}

.login-card {
	margin: -60px 24px 0;
	background: #fff;
	border-radius: 24px;
	padding: 30px 24px;
	box-shadow: 0 8px 24px rgba(0,0,0,0.08);
	position: relative;
	z-index: 10;

	.tab-header {
		margin-bottom: 30px;
		.tab-item {
			font-size: 18px;
			font-weight: bold;
			color: #333;
			position: relative;
			&::after {
				content: '';
				position: absolute;
				bottom: -8px;
				left: 0;
				width: 30px;
				height: 4px;
				background: #4A90E2;
				border-radius: 2px;
			}
		}
	}
	
	.input-item {
		display: flex;
		align-items: center;
		height: 54px;
		background: #f8f9fa;
		border-radius: 12px;
		margin-bottom: 16px;
		padding: 0 16px;
		
		.iconfont {
			font-size: 20px;
			color: #999;
			margin-right: 12px;
		}

		.input {
			flex: 1;
			font-size: 16px;
		}
	}
}

.agreement-row {
	margin: 20px 0;
	.agreement-text {
		font-size: 13px;
		color: #999;
		.link { color: #4A90E2; }
	}
}

.login-btn {
	background: linear-gradient(135deg, #4A90E2, #357ABD);
	color: #fff;
	height: 50px;
	border-radius: 25px;
	font-size: 17px;
	font-weight: bold;
	box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
}

.footer-links {
	display: flex;
	justify-content: center;
	align-items: center;
	margin-top: 24px;
	font-size: 14px;
	color: #666;
	.divider { margin: 0 15px; color: #eee; }
}
</style>