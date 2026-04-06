<template>
	<view class="container">
		<view class="form-section">
			<!-- 头像上传 -->
			<view class="avatar-upload">
				<image 
					class="avatar-img" 
					:src="displayAvatarUrl" 
					mode="aspectFit"
					@click="isEditing ? chooseAvatar : null"
				></image>
				<view v-if="isEditing" class="upload-text" @click="chooseAvatar">点击更换头像</view>
			</view>
			
			<!-- 信息展示/编辑表单 -->
			<view class="form-group">
				<!-- 姓名 -->
				<view class="form-item">
					<text class="form-label">姓名</text>
					<view v-if="!isEditing" class="info-display">{{originalUserData.name || '未设置'}}</view>
					<input v-else class="form-input" type="text" v-model="userForm.name" :placeholder="originalUserData.name || '请输入姓名'" />
				</view>
				
				<!-- 性别 -->
				<view class="form-item">
					<text class="form-label">性别</text>
					<view v-if="!isEditing" class="info-display">{{originalUserData.sex || '未设置'}}</view>
					<view v-else class="radio-group">
						<view class="radio-item" @click="userForm.sex = '男'">
							<view class="radio-circle" :class="{ 'checked': userForm.sex === '男' || (!userForm.sex && originalUserData.sex === '男') }"></view>
							<text class="radio-text">男</text>
						</view>
						<view class="radio-item" @click="userForm.sex = '女'">
							<view class="radio-circle" :class="{ 'checked': userForm.sex === '女' || (!userForm.sex && originalUserData.sex === '女') }"></view>
							<text class="radio-text">女</text>
						</view>
					</view>
				</view>
				
				<!-- 年龄 -->
				<view class="form-item">
					<text class="form-label">年龄</text>
					<view v-if="!isEditing" class="info-display">{{originalUserData.age || '未设置'}}</view>
					<input v-else class="form-input" type="number" v-model="userForm.age" :placeholder="originalUserData.age ? originalUserData.age + '' : '请输入年龄'" />
				</view>
				
				<!-- 手机号 -->
				<view class="form-item">
					<text class="form-label">手机号</text>
					<view v-if="!isEditing" class="info-display">{{originalUserData.phone || '未设置'}}</view>
					<input v-else class="form-input" type="number" v-model="userForm.phone" :placeholder="originalUserData.phone || '请输入手机号'" maxlength="11" />
				</view>
				
				<!-- 密码 -->
				<view class="form-item" v-if="isEditing">
					<text class="form-label">密码</text>
					<input class="form-input" type="password" v-model="userForm.password" placeholder="请输入新密码（不修改请留空）" />
				</view>
			</view>
		</view>
		
		<!-- 按钮区域 -->
		<view class="btn-section">
			<button v-if="!isEditing" class="edit-btn" @click="startEditing">编辑资料</button>
			<view v-else class="btn-group">
				<button class="cancel-btn" @click="cancelEditing">取消</button>
				<button class="save-btn" @click="saveProfile">保存</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user.js'
import { userPlaceholder } from '@/utils/assets.js'
import { pickCropAndUploadAvatar } from '@/utils/avatar-upload.js'
import { resolveAvatarUrl } from '@/utils/media.js'

// 响应式数据
const userStore = ref(null)
// 编辑模式状态
const isEditing = ref(false)
// 存储原始用户数据，用于显示当前值
const originalUserData = ref({
	avatar: '',
	name: '',
	sex: '', // 男/女
	age: null,
	phone: '',
	password: ''
})
// 表单数据，只包含用户修改的字段
const userForm = ref({
	avatar: '',
	name: '',
	sex: '',
	age: null,
	phone: '',
	password: ''
})
const localAvatarPreview = ref('')
const avatarUploadedThisEdit = ref(false)

const displayAvatarUrl = computed(() =>
	resolveAvatarUrl(localAvatarPreview.value || userForm.value.avatar || originalUserData.value.avatar, userPlaceholder)
)

// 生命周期
onMounted(() => {
	// 初始化用户store
	userStore.value = useUserStore()
	// 从本地存储恢复状态
	userStore.value.restoreFromStorage()
	// 获取用户信息
	loadUserInfo()
})

// 方法
// 加载用户信息
const loadUserInfo = async () => {
	// 从本地存储获取用户信息
	const localUserInfo = uni.getStorageSync('userInfo')
	
	if (!localUserInfo || !localUserInfo.id) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		uni.navigateTo({ url: '/pages/auth/login?role=user' })
		return
	}
	
	try {
		// 直接调用API获取用户信息
		const response = await getUserInfo(localUserInfo.id)
		
		// 获取用户数据
		const userData = response.data || response
		
		if (userData) {
			// 直接设置用户数据 - 注意后端返回的是userId而不是id
			originalUserData.value = {
				id: userData.userId || userData.id || localUserInfo.id,
				userId: userData.userId || userData.id || localUserInfo.id,
				avatar: userData.avatar || '',
				name: userData.name || '',
				sex: userData.sex || '',
				age: userData.age || null,
				phone: userData.phone || '',
				password: ''
			}
			
			// 初始化表单
			userForm.value = { ...originalUserData.value, password: '' }
		} else {
			uni.showToast({ title: '获取用户信息失败', icon: 'none' })
		}
	} catch (error) {
		console.error('获取用户信息失败:', error)
		uni.showToast({ title: '网络错误', icon: 'none' })
	}
}

// 选择头像
const chooseAvatar = () => {
	uploadUserAvatar()
}

// 上传头像（上传后后端已更新用户 avatar，并已同步到 store）
const uploadUserAvatar = async () => {
	try {
		uni.showLoading({ title: '处理中...' })
		const { avatarUrl, localFilePath } = await pickCropAndUploadAvatar(uploadAvatar)
		uni.hideLoading()
		if (avatarUrl) {
			localAvatarPreview.value = localFilePath || ''
			userForm.value.avatar = avatarUrl
			originalUserData.value.avatar = avatarUrl
			avatarUploadedThisEdit.value = true
			uni.showToast({ title: '头像上传成功', icon: 'success' })
		}
	} catch (error) {
		uni.hideLoading()
		if (!/cancel/i.test(error?.message || error?.errMsg || '')) {
			uni.showToast({ title: '头像上传失败', icon: 'none' })
		}
	}
}



// 开始编辑模式
const startEditing = () => {
	isEditing.value = true
	// 初始化表单数据为当前用户数据
	userForm.value = {
		avatar: originalUserData.value.avatar || '',
		name: originalUserData.value.name || '',
		sex: originalUserData.value.sex || '',
		age: originalUserData.value.age || null,
		phone: originalUserData.value.phone || '',
		password: ''
	}
	localAvatarPreview.value = ''
	avatarUploadedThisEdit.value = false
}

// 取消编辑
const cancelEditing = () => {
	isEditing.value = false
	// 重置表单数据
	userForm.value = {
		avatar: '',
		name: '',
		sex: '',
		age: null,
		phone: '',
		password: ''
	}
	localAvatarPreview.value = ''
	avatarUploadedThisEdit.value = false
}

// 保存个人资料
const saveProfile = async () => {
	if (!userStore.value || !userStore.value.isLoggedIn) {
		uni.showToast({
			title: '请先登录',
			icon: 'none'
		})
		return
	}
	
	try {
		uni.showLoading({ title: '保存中...' })
		
		// 创建一个只包含必要字段的对象
		const updatedFields = {}
		
		// 确保包含用户ID（尝试多种可能的字段名）
		let userId = null
		
		// 从userStore获取
		if (userStore.value && userStore.value.userInfo) {
			userId = userStore.value.userInfo.userId || userStore.value.userInfo.id
		}
		
		// 从originalUserData获取
		if (!userId && originalUserData.value) {
			userId = originalUserData.value.userId || originalUserData.value.id
		}
		
		if (userId) {
			// 只设置一个ID字段，避免冗余
			updatedFields.id = userId
		} else {
			uni.hideLoading()
			uni.showToast({
				title: '无法获取用户ID，请重新登录',
				icon: 'none'
			})
			return
		}
		
		// 检查哪些字段被修改了，只包含实际修改的字段
		Object.keys(userForm.value).forEach(key => {
			const newValue = userForm.value[key]
			const originalValue = originalUserData.value[key]
			
			// 跳过密码字段（如果为空）和avatar字段（单独处理）
			if (key === 'password' && (!newValue || newValue === '')) {
				return
			}
			
			// 跳过avatar字段，单独处理
			if (key === 'avatar') {
				return
			}
			
			// 确保两个值都转换为字符串进行比较（除了age）
			let normalizedNewValue = newValue
			let normalizedOriginalValue = originalValue
			
			if (key !== 'age') {
				normalizedNewValue = String(newValue || '')
				normalizedOriginalValue = String(originalValue || '')
			}
			
			// 只有当值确实发生变化时才包含在更新中
			if (normalizedNewValue !== normalizedOriginalValue && normalizedNewValue !== '') {
				// 对于age字段，确保是数字类型
				if (key === 'age' && newValue) {
					updatedFields[key] = parseInt(newValue)
				} else {
					updatedFields[key] = newValue
				}
			}
		})

		if (Object.keys(updatedFields).length === 1) {
			uni.hideLoading()
			if (avatarUploadedThisEdit.value) {
				await loadUserInfo()
				localAvatarPreview.value = ''
				avatarUploadedThisEdit.value = false
				isEditing.value = false
				uni.showToast({
					title: '保存成功',
					icon: 'success'
				})
				return
			}
			isEditing.value = false
			uni.showToast({
				title: '没有需要保存的修改',
				icon: 'none'
			})
			return
		}
		
		// 调用更新用户信息API，只传递已修改的字段
		const response = await updateUserInfo(updatedFields)
		
		uni.hideLoading()
		
		if (response.code === 200) {
            // 更新store中的用户信息
            const updatedUserInfo = {
                ...userStore.value.userInfo,
                ...updatedFields
            }
            userStore.value.setUserInfo(updatedUserInfo)
            
            // 更新成功后，更新原始数据
            await loadUserInfo()
            localAvatarPreview.value = ''
            avatarUploadedThisEdit.value = false
            // 退出编辑模式
            isEditing.value = false
            
            uni.showToast({
                title: '保存成功',
                icon: 'success'
            })
		} else {
			uni.showToast({
				title: response.message || '保存失败',
				icon: 'none'
			})
		}
	} catch (error) {
		uni.hideLoading()
		console.error('更新用户信息失败:', error)
		uni.showToast({
			title: '保存失败，请重试',
			icon: 'none'
		})
	}
}
</script>

<style lang="scss">
@import '@/styles/user-ui.scss';
.container {
    min-height: 100vh;
    background: linear-gradient(180deg, #f4f8ff 0%, #f8fbff 240rpx, #f5f7fa 100%);
    padding-bottom: 120rpx;
}

.form-section {
    background-color: #ffffff;
    padding: 32rpx 28rpx;
    margin: 20rpx;
    border-radius: 28rpx;
    border: 1rpx solid rgba(0, 122, 255, 0.08);
    box-shadow: 0 18rpx 42rpx rgba(18, 56, 109, 0.08);
}

/* 头像上传 */
.avatar-upload {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin: 24rpx 0 40rpx;
}

.avatar-img {
    width: 168rpx;
    height: 168rpx;
    border-radius: 42rpx;
    border: 4rpx solid rgba(0, 122, 255, 0.1);
    background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
    box-shadow: 0 18rpx 38rpx rgba(0, 122, 255, 0.12);
    margin-bottom: 18rpx;
}

.upload-text {
    padding: 10rpx 20rpx;
    border-radius: 999rpx;
    background: rgba(0, 122, 255, 0.08);
    font-size: 24rpx;
    color: #007AFF;
    font-weight: 600;
}

/* 表单样式 */
.form-group {
    margin-top: 30rpx;
}

.form-item {
    margin-bottom: 30rpx;
}

.form-label {
    display: block;
    font-size: 28rpx;
    color: #333333;
    margin-bottom: 15rpx;
    font-weight: bold;
}

.form-input {
    width: 100%;
    height: 80rpx;
    border: 1px solid #ddd;
    background-color: #f8f8f8;
    border-radius: 8rpx;
    padding: 0 20rpx;
    font-size: 28rpx;
    color: #333333;
}

.form-hint {
    font-size: 24rpx;
    color: #666666;
    margin-top: 8rpx;
    padding-left: 10rpx;
}

/* 文本域和选择器 */
.form-textarea {
    width: 100%;
    height: 160rpx;
    background-color: #f8f8f8;
    border-radius: 8rpx;
    padding: 20rpx;
    font-size: 28rpx;
    color: #333333;
}

.form-picker {
    width: 100%;
    height: 80rpx;
    background-color: #f8f8f8;
    border-radius: 8rpx;
    padding: 0 20rpx;
    display: flex;
    align-items: center;
}

.picker-value {
    font-size: 28rpx;
    color: #333333;
}

/* 单选按钮 */
.radio-group {
    display: flex;
    flex-direction: row;
}

.radio-item {
    display: flex;
    flex-direction: row;
    align-items: center;
    margin-right: 40rpx;
}

.radio-circle {
    width: 40rpx;
    height: 40rpx;
    border-radius: 20rpx;
    border: 2rpx solid #cccccc;
    margin-right: 10rpx;
    position: relative;
}

.radio-circle.checked {
    border-color: #007AFF;
    background-color: #ffffff;
}

.radio-circle.checked::after {
    content: '';
    position: absolute;
    width: 24rpx;
    height: 24rpx;
    border-radius: 12rpx;
    background-color: #007AFF;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

.radio-text {
    font-size: 28rpx;
    color: #333333;
}

/* 按钮区域 */
.btn-section {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 30rpx;
    background-color: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(8px);
    box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.btn-group {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
}

.edit-btn {
    width: 100%;
    height: 80rpx;
    line-height: 80rpx;
    background-color: #007AFF;
    color: #fff;
    border-radius: 40rpx;
    font-size: 30rpx;
    text-align: center;
}

.save-btn {
    width: 45%;
    height: 90rpx;
    background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
    color: #ffffff;
    border-radius: 45rpx;
    font-size: 32rpx;
    font-weight: bold;
    display: flex;
    justify-content: center;
    align-items: center;
}

.cancel-btn {
    width: 45%;
    height: 80rpx;
    line-height: 80rpx;
    background-color: #f5f7fa;
    color: #666;
    border-radius: 40rpx;
    font-size: 30rpx;
    text-align: center;
}

/* 信息展示模式 */
.info-display {
    font-size: 28rpx;
    color: #333;
    padding: 20rpx 0;
    border-bottom: 1px solid #eee;
    margin-bottom: 30rpx;
}
</style>
