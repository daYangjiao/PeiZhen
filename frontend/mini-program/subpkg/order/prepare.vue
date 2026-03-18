<template>
	<view class="container">
		<scroll-view class="content" scroll-y>
			<!-- 通用必备 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/requirement.svg" mode="aspectFit"></image>
					<text>通用必备</text>
				</view>
				<view class="check-list">
					<view class="check-item" v-for="(item, idx) in generalItems" :key="'g'+idx" @click="toggleCheck('general', idx)">
						<view class="checkbox" :class="{ checked: item.checked }">{{ item.checked ? '✓' : '' }}</view>
						<text class="check-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<!-- 订单专属·患者需求 -->
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/symptom-modern.svg" mode="aspectFit"></image>
					<text>订单专属·患者需求</text>
				</view>
				<view class="check-list">
					<view class="check-item" v-for="(item, idx) in orderItems" :key="'o'+idx" @click="toggleCheck('order', idx)">
						<view class="checkbox" :class="{ checked: item.checked }">{{ item.checked ? '✓' : '' }}</view>
						<text class="check-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="btn-wrap">
				<button class="confirm-btn" @click="markPrepared">标记为已准备</button>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			orderId: '',
			generalItems: [
				{ label: '工作证/资质证明', checked: false },
				{ label: '手机(扫码功能)', checked: false },
				{ label: '口罩/手套', checked: false },
				{ label: '签字笔/便签本', checked: false }
			],
			orderItems: []
		}
	},
	onLoad(options) {
		this.orderId = options.orderId || ''
		const symptom = decodeURIComponent(options.symptom || '')
		const other = decodeURIComponent(options.other || '')
		const hospital = decodeURIComponent(options.hospital || '')

		// 根据患者需求动态生成订单专属项
		const items = []
		if (symptom && symptom !== '无') {
			// 尝试解析过敏等关键信息
			if (symptom.includes('青霉素') || symptom.toLowerCase().includes('过敏')) {
				items.push({ label: '对青霉素过敏', checked: false })
			}
			items.push({ label: symptom, checked: false })
		}
		items.push({ label: '医院科室导航图', checked: false })
		items.push({ label: '取药袋/病历夹', checked: false })
		items.push({ label: '轮椅(根据需求)', checked: false })
		if (other && other !== '无') {
			items.push({ label: other, checked: false })
		}
		// 去重并保留顺序
		const seen = new Set()
		this.orderItems = items.filter(i => {
			if (seen.has(i.label)) return false
			seen.add(i.label)
			return true
		})
		if (this.orderItems.length === 0) {
			this.orderItems = [
				{ label: '医院科室导航图', checked: false },
				{ label: '取药袋/病历夹', checked: false },
				{ label: '轮椅(根据需求)', checked: false }
			]
		}
	},
	methods: {
		toggleCheck(type, idx) {
			const arr = type === 'general' ? this.generalItems : this.orderItems
			arr[idx].checked = !arr[idx].checked
			this.$forceUpdate()
		},
		markPrepared() {
			if (!this.orderId) {
				uni.showToast({ title: '订单信息异常', icon: 'none' })
				return
			}
			uni.setStorageSync(`order_prepared_${this.orderId}`, '1')
			uni.showToast({ title: '已标记准备完成', icon: 'success' })
			setTimeout(() => {
				uni.navigateBack()
			}, 800)
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.container {
	min-height: 100vh;
	background: #f5f7fa;
	--text-main: #1f2937;
	--text-sub: #667085;
	--primary: #66a6ff;
	--primary-deep: #4f95f0;
}
.content {
	min-height: 100vh;
	padding: 24rpx 0 16rpx;
}
.info-card {
	background: $escort-color-surface;
	border-radius: $escort-radius-card;
	padding: 28rpx 24rpx;
	margin: 0 24rpx 20rpx;
	box-shadow: $escort-shadow-card;
	border: 1rpx solid #e7edf5;
}
.card-title {
	display: flex;
	align-items: center;
	margin-bottom: 18rpx;
	font-size: 30rpx;
	font-weight: 600;
	color: var(--text-main);
}
.title-icon {
	width: 34rpx;
	height: 34rpx;
	margin-right: 10rpx;
}
.check-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}
.check-item {
	display: flex;
	align-items: center;
	padding: 10rpx 0;
}
.checkbox {
	width: 36rpx;
	height: 36rpx;
	border: 2rpx solid #d8e0ec;
	border-radius: 8rpx;
	margin-right: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24rpx;
	color: #fff;
	flex-shrink: 0;
}
.checkbox.checked {
	background: $escort-color-primary;
	border-color: $escort-color-primary;
}
.check-label {
	font-size: 28rpx;
	color: var(--text-main);
	line-height: 1.4;
}
.btn-wrap {
	padding: 28rpx 24rpx 72rpx;
}
.confirm-btn {
	width: 100%;
	height: 88rpx;
	line-height: 88rpx;
	background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
	color: #fff;
	border: none;
	border-radius: 44rpx;
	font-size: 28rpx;
	font-weight: 600;
	box-shadow: $escort-shadow-primary;
}
</style>
