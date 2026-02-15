<template>
	<view class="register-container">
		<!-- 顶部装饰 -->
		<view class="header-decoration">
			<view class="decoration-circle circle1"></view>
			<view class="decoration-circle circle2"></view>
			<view class="decoration-circle circle3"></view>
		</view>
		
		<!-- Logo和标题 -->
		<view class="logo-section">
			<image class="logo" src="/static/mynewlogo.png" mode="aspectFit"></image>
			<text class="app-name">愈安伴</text>
			<text class="app-desc">专业贴心的医疗陪护服务</text>
		</view>
		
		<!-- 注册区域 -->
		<view class="register-section">
			<view class="register-card">
				<text class="register-title">用户注册</text>
				<text class="register-subtitle">创建您的账号，开始使用陪诊服务</text>
				
				<!-- 注册表单 -->
				<view class="form-item">
					<text class="form-label">用户名</text>
					<input 
						class="form-input" 
						type="text" 
						placeholder="请输入用户名" 
						v-model="formData.username"
						@blur="validateUsername"
					/>
					<text v-if="errors.username" class="error-text">{{ errors.username }}</text>
				</view>
				
				<view class="form-item">
					<text class="form-label">密码</text>
					<input 
						class="form-input" 
						type="password" 
						placeholder="请设置密码" 
						v-model="formData.password"
						@blur="validatePassword"
					/>
					<text v-if="errors.password" class="error-text">{{ errors.password }}</text>
				</view>
				
				<view class="form-item">
					<text class="form-label">确认密码</text>
					<input 
						class="form-input" 
						type="password" 
						placeholder="请再次输入密码" 
						v-model="formData.confirmPassword"
						@blur="validateConfirmPassword"
					/>
					<text v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</text>
				</view>
				
				<view class="form-item">
					<text class="form-label">姓名</text>
					<input 
						class="form-input" 
						type="text" 
						placeholder="请输入您的真实姓名" 
						v-model="formData.name"
						@blur="validateName"
					/>
					<text v-if="errors.name" class="error-text">{{ errors.name }}</text>
				</view>
				
				<!-- 注册按钮 -->
				<button 
					class="register-btn" 
					@click="handleRegister"
					:disabled="isSubmitting"
				>
					{{ isSubmitting ? '注册中...' : '立即注册' }}
				</button>
				
				<!-- 已有账号提示 -->
				<view class="login-tips">
					<text class="tips-text">已有账号？</text>
					<text class="link-text" @click="goToLogin">立即登录</text>
				</view>
			</view>
		</view>
		
		<!-- 底部说明 -->
		<view class="footer-info">
			<view class="agreement-checkbox">
				<checkbox :checked="agreedToTerms" @tap="toggleAgreement" />
				<text class="agreement-text">我已阅读并同意</text>
				<text class="link-text" @click="viewPrivacyPolicy">《隐私政策》</text>
				<text class="agreement-text">和</text>
				<text class="link-text" @click="viewTermsOfService">《服务条款》</text>
			</view>
			<text v-if="errors.agreement" class="error-text agreement-error">{{ errors.agreement }}</text>
		</view>
	</view>
</template>

<script>
import { register } from '@/api/auth'

export default {
	data() {
		return {
			// 表单数据
			formData: {
				username: '',
				password: '',
				confirmPassword: '',
				name: ''
			},
			// 错误信息
			errors: {
				username: '',
				password: '',
				confirmPassword: '',
				name: '',
				agreement: ''
			},
			// 是否同意条款
			agreedToTerms: false,
			// 提交状态
			isSubmitting: false,
			// 重定向URL
			redirectUrl: '/pages/index/index'
		}
	},
	
	onLoad(options) {
		// 获取重定向URL
		if (options.redirect) {
			this.redirectUrl = decodeURIComponent(options.redirect)
		}
	},
	
	methods: {
		// 验证用户名
		validateUsername() {
			if (!this.formData.username) {
				this.errors.username = '用户名不能为空'
				return false
			}
			if (this.formData.username.length < 3) {
				this.errors.username = '用户名长度不能少于3个字符'
				return false
			}
			this.errors.username = ''
			return true
		},
		
		// 验证密码
		validatePassword() {
			if (!this.formData.password) {
				this.errors.password = '密码不能为空'
				return false
			}
			if (this.formData.password.length < 6) {
				this.errors.password = '密码长度不能少于6个字符'
				return false
			}
			this.errors.password = ''
			return true
		},
		
		// 验证确认密码
		validateConfirmPassword() {
			if (!this.formData.confirmPassword) {
				this.errors.confirmPassword = '请确认密码'
				return false
			}
			if (this.formData.confirmPassword !== this.formData.password) {
				this.errors.confirmPassword = '两次输入的密码不一致'
				return false
			}
			this.errors.confirmPassword = ''
			return true
		},
		
		// 验证姓名
		validateName() {
			if (!this.formData.name) {
				this.errors.name = '姓名不能为空'
				return false
			}
			this.errors.name = ''
			return true
		},
		
		// 验证所有表单项
		validateForm() {
			const usernameValid = this.validateUsername()
			const passwordValid = this.validatePassword()
			const confirmPasswordValid = this.validateConfirmPassword()
			const nameValid = this.validateName()
			
			// 验证是否同意条款
			if (!this.agreedToTerms) {
				this.errors.agreement = '请阅读并同意隐私政策和服务条款'
				return false
			} else {
				this.errors.agreement = ''
			}
			
			return usernameValid && passwordValid && confirmPasswordValid && nameValid && this.agreedToTerms
		},
		
		// 切换同意条款状态
		toggleAgreement() {
			this.agreedToTerms = !this.agreedToTerms
			if (this.agreedToTerms) {
				this.errors.agreement = ''
			}
		},
		
		// 处理注册
		async handleRegister() {
			// 表单验证
			if (!this.validateForm()) {
				uni.showToast({
					title: '请完善表单信息',
					icon: 'none'
				})
				return
			}
			
			// 设置提交状态
			this.isSubmitting = true
			
			try {
				// 准备注册数据
				const registerData = {
					username: this.formData.username,
					password: this.formData.password,
					name: this.formData.name,
					openid: '' // 添加openid字段，后端要求不能为null
				}
				
				// 调用注册接口
				const response = await register(registerData)
				
				// 注册成功
				uni.showToast({
					title: '注册成功',
					icon: 'success'
				})
				
				// 如果注册接口返回了用户ID，尝试获取详细信息
				if (response.data && response.data.userId) {
					try {
						// 导入getUserById方法
						const { getUserById } = require('@/api/user.js');
						
						// 获取详细用户信息
						const userDetailResponse = await getUserById(response.data.userId);
						
						if (userDetailResponse.data) {
							console.log('获取到用户详细信息:', userDetailResponse.data);
							
							// 保存用户信息到本地，方便下次登录时使用
							const userInfo = {
								userId: response.data.userId,
								name: userDetailResponse.data.name,
								username: userDetailResponse.data.username,
								nickName: userDetailResponse.data.username
							};
							
							// 存储到本地，但不设置为已登录状态
							uni.setStorageSync('lastRegisteredUser', userInfo);
						}
					} catch (detailError) {
						console.error('获取用户详细信息失败:', detailError);
					}
				}
				
				// 延迟跳转到登录页
				setTimeout(() => {
					uni.redirectTo({
						url: '/subpkg/auth/login'
					})
				}, 1500)
			} catch (error) {
				console.error('注册失败:', error)
				
				// 显示错误信息
				uni.showToast({
					title: error.message || '注册失败，请重试',
					icon: 'none'
				})
			} finally {
				// 重置提交状态
				this.isSubmitting = false
			}
		},
		
		// 跳转到登录页
		goToLogin() {
			uni.redirectTo({
				url: '/subpkg/auth/login'
			})
		},
		
		// 查看隐私政策
		viewPrivacyPolicy() {
			uni.showModal({
				title: '隐私政策',
				content: '这里是隐私政策的内容...',
				showCancel: false
			})
		},
		
		// 查看服务条款
		viewTermsOfService() {
			uni.showModal({
				title: '服务条款',
				content: '这里是服务条款的内容...',
				showCancel: false
			})
		}
	}
}
</script>

<style>
.register-container {
	min-height: 100vh;
	background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 40rpx;
	box-sizing: border-box;
}

/* 顶部装饰 */
.header-decoration {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	height: 300rpx;
	overflow: hidden;
}

.decoration-circle {
	position: absolute;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.1);
}

.circle1 {
	width: 200rpx;
	height: 200rpx;
	top: -100rpx;
	right: -50rpx;
}

.circle2 {
	width: 150rpx;
	height: 150rpx;
	top: 50rpx;
	left: -75rpx;
}

.circle3 {
	width: 100rpx;
	height: 100rpx;
	top: 150rpx;
	right: 100rpx;
}

/* Logo区域 */
.logo-section {
	margin-top: 50rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	z-index: 10;
}

.logo {
	width: 200rpx;
	height: 200rpx;
	margin-bottom: 10rpx;
}

.app-name {
	font-size: 48rpx;
	font-weight: bold;
	color: white;
	margin-bottom: 30rpx;
}

.app-desc {
	font-size: 28rpx;
	color: rgba(255, 255, 255, 0.8);
	margin-bottom: 60rpx;
}

/* 注册区域 */
.register-section {
	flex: 1;
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.register-card {
	background: white;
	border-radius: 30rpx;
	padding: 60rpx 40rpx;
	width: 100%;
	max-width: 600rpx;
	box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
	display: flex;
	flex-direction: column;
	align-items: center;
}

.register-title {
	font-size: 36rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 20rpx;
	text-align: center;
}

.register-subtitle {
	font-size: 28rpx;
	color: #666;
	text-align: center;
	line-height: 1.5;
	margin-bottom: 60rpx;
}

.form-item {
	width: 100%;
	margin-bottom: 20rpx;
}

.form-label {
	font-size: 28rpx;
	color: #666666;
	margin-bottom: 10rpx;
	display: block;
}

.form-input {
	width: 100%;
	height: 90rpx;
	padding: 0 30rpx;
	border: 2rpx solid #e0e0e0;
	border-radius: 12rpx;
	background-color: #fff;
	font-size: 28rpx;
	color: #333;
	box-sizing: border-box;
}

.form-input:focus {
	border-color: #007aff;
}

.error-text {
	font-size: 24rpx;
	color: #FF5151;
	margin-top: 10rpx;
	display: block;
}

.register-btn {
	width: 100%;
	height: 90rpx;
	background-color: #007aff;
	color: #fff;
	border: none;
	border-radius: 12rpx;
	font-size: 32rpx;
	font-weight: 500;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-top: 20rpx;
}

.register-btn[disabled] {
	background-color: #c7c7cc;
	color: #fff;
}

.btn-text {
	font-size: 32rpx;
	font-weight: 500;
	color: #FFFFFF;
}

.login-tips {
	display: flex;
	justify-content: center;
	margin-top: 30rpx;
}

.tips-text {
	font-size: 28rpx;
	color: #666;
}

.link-text {
	font-size: 28rpx;
	color: #007aff;
	margin-left: 10rpx;
}

/* 底部信息 */
.footer-info {
	padding: 40rpx 0;
	display: flex;
	flex-direction: column;
	flex-wrap: wrap;
	justify-content: center;
	align-items: center;
}

.info-text {
	color: rgba(255, 255, 255, 0.7);
	font-size: 24rpx;
}

.footer-info .link-text {
	color: rgba(255, 255, 255, 0.9);
	font-size: 24rpx;
	text-decoration: underline;
	margin: 0 5rpx;
}

.agreement-checkbox {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: center;
	margin-bottom: 10rpx;
}

.agreement-text {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.8);
	margin: 0 4rpx;
}

.agreement-error {
	text-align: center;
	margin-top: 10rpx;
}
</style>