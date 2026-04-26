import { defineStore } from 'pinia'

import { get, post } from '@/utils/api.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'
import { addOrderListener, connectOrderSocket } from '@/utils/order-websocket.js'
import {
  buildExclusiveDispatchKey,
  formatExclusiveDispatchCountdown,
  getExclusiveDispatchRemainingMs,
  shouldOpenExclusiveDispatchPopup,
} from '@/utils/exclusive-dispatch.mjs'
import { calculateEstimatedAttendantIncome } from '@/utils/settlement.mjs'

let listenerBound = false
let countdownTimer = null
let refreshPromise = null
const pendingOrderIds = new Set()

const readEscortSession = () => {
  const role = uni.getStorageSync('role') || ''
  const token = uni.getStorageSync('token') || ''
  const isLoggedIn = !!uni.getStorageSync('isLoggedIn')
  const userInfo = uni.getStorageSync('userInfo') || {}
  const userId = Number(userInfo.id || userInfo.userId || 0)
  const isEscort = role === 'escort' && !!token && isLoggedIn && userId > 0

  return {
    isEscort,
    role,
    userId,
    userInfo,
  }
}

const getCurrentPageContext = () => {
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const currentPage = pages[pages.length - 1] || {}
  const route = String(currentPage.route || currentPage.$page?.route || currentPage.$page?.fullPath || '')
  const options = currentPage.options || currentPage.$page?.options || {}
  const orderId = Number(options.orderId || 0)
  return { route, orderId }
}

const formatAppointmentTime = (order = {}) => {
  const date = order.serviceDate || ''
  const slot = formatServiceTimeSlot(order.serviceTimeSlot || '')
  return [date, slot].filter(Boolean).join(' ').trim() || '时间待定'
}

const formatIncome = (amount) => {
  return calculateEstimatedAttendantIncome(amount).toFixed(2)
}

const buildPopupPayload = (order = {}, userId) => {
  const orderId = Number(order.orderId || order.id || 0)
  const remainingMs = getExclusiveDispatchRemainingMs(order)
  return {
    key: buildExclusiveDispatchKey(userId, orderId),
    userId: Number(userId || 0),
    orderId,
    orderNo: order.orderNo || '',
    hospital: order.hospital || '医院待确认',
    appointmentTime: formatAppointmentTime(order),
    patientName: order.contactPerson || order.patientName || order.userName || '患者',
    patientAge: order.patientAge || '--',
    patientSex: order.patientSex || '未知',
    serviceType: order.serviceContent || order.serviceTypeName || '陪诊服务',
    incomeText: formatIncome(order.orderAmount),
    paymentTime: order.paymentTime || order.createTime || order.updateTime || '',
    createTime: order.createTime || '',
    updateTime: order.updateTime || '',
    countdownText: formatExclusiveDispatchCountdown(remainingMs),
    remainingMs,
  }
}

const isTerminalAcceptError = (error) => {
  const code = Number(error?.code || error?.statusCode || 0)
  if ([401, 403, 404].includes(code)) return true
  const message = String(error?.message || error?.errMsg || '')
  return /已被|已接单|已释放|不存在|无权限|已取消|不可|失效/.test(message)
}

export const useExclusiveDispatchStore = defineStore('exclusiveDispatch', {
  state: () => ({
    initialized: false,
    activeUserId: 0,
    visible: false,
    actionLoading: false,
    popup: null,
    queue: [],
    ignoredKeyMap: {},
  }),

  getters: {
    ignoredKeysSet: (state) => new Set(Object.keys(state.ignoredKeyMap || {})),
  },

  actions: {
    ensureInitialized() {
      this.syncSession()
      if (this.initialized) return

      connectOrderSocket()
      addOrderListener((message) => {
        this.handleOrderMessage(message)
      })
      uni.$on('session:changed', () => {
        this.syncSession(true)
      })

      this.initialized = true
      listenerBound = true
      this.refreshPendingExclusiveOrders()
    },

    syncSession(forceReset = false) {
      const session = readEscortSession()
      if (!session.isEscort) {
        this.resetAllState()
        return null
      }

      if (forceReset || (this.activeUserId && this.activeUserId !== session.userId)) {
        this.resetAllState()
      }

      this.activeUserId = session.userId
      return session
    },

    resetAllState() {
      this.stopCountdown()
      this.activeUserId = 0
      this.visible = false
      this.actionLoading = false
      this.popup = null
      this.queue = []
      this.ignoredKeyMap = {}
      pendingOrderIds.clear()
    },

    stopCountdown() {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    },

    startCountdown() {
      this.stopCountdown()
      if (!this.popup) return

      const tick = () => {
        if (!this.popup) {
          this.stopCountdown()
          return
        }
        const remainingMs = getExclusiveDispatchRemainingMs({
          paymentTime: this.popup.paymentTime,
          createTime: this.popup.createTime,
          updateTime: this.popup.updateTime,
        })

        if (remainingMs <= 0) {
          this.clearOrderState(this.popup.orderId)
          return
        }

        this.popup = {
          ...this.popup,
          remainingMs,
          countdownText: formatExclusiveDispatchCountdown(remainingMs),
        }
      }

      tick()
      countdownTimer = setInterval(tick, 1000)
    },

    setIgnored(key, ignored = true) {
      if (!key) return
      const nextMap = { ...(this.ignoredKeyMap || {}) }
      if (ignored) nextMap[key] = Date.now()
      else delete nextMap[key]
      this.ignoredKeyMap = nextMap
    },

    removeQueuedPopup(key) {
      if (!key) return
      this.queue = (this.queue || []).filter((item) => item?.key !== key)
    },

    showPopup(popup) {
      if (!popup?.key) return
      this.popup = popup
      this.visible = true
      this.actionLoading = false
      this.startCountdown()
    },

    showNextPopup() {
      if (this.visible || this.popup || !this.queue.length) return

      while (this.queue.length > 0) {
        const nextPopup = this.queue.shift()
        if (!nextPopup?.key) continue

        const { route, orderId } = getCurrentPageContext()
        const remainingMs = getExclusiveDispatchRemainingMs(nextPopup)
        if (!shouldOpenExclusiveDispatchPopup({
          userId: nextPopup.userId,
          orderId: nextPopup.orderId,
          orderStatus: 8,
          remainingMs,
          ignoredKeys: this.ignoredKeysSet,
          currentRoute: route,
          currentOrderId: orderId,
        })) {
          continue
        }

        this.showPopup({
          ...nextPopup,
          remainingMs,
          countdownText: formatExclusiveDispatchCountdown(remainingMs),
        })
        break
      }
    },

    closePopup({ ignore = false } = {}) {
      if (ignore && this.popup?.key) {
        this.setIgnored(this.popup.key, true)
      }
      this.stopCountdown()
      this.visible = false
      this.actionLoading = false
      this.popup = null
      this.showNextPopup()
    },

    clearOrderState(orderId) {
      const nextOrderId = Number(orderId || 0)
      if (!nextOrderId || !this.activeUserId) return

      const key = buildExclusiveDispatchKey(this.activeUserId, nextOrderId)
      this.setIgnored(key, false)
      this.removeQueuedPopup(key)

      if (this.popup?.key === key) {
        this.stopCountdown()
        this.visible = false
        this.popup = null
        this.actionLoading = false
      }

      this.showNextPopup()
    },

    enqueuePopup(popup) {
      if (!popup?.key) return

      if (this.popup?.key === popup.key) {
        this.popup = popup
        this.visible = true
        this.startCountdown()
        return
      }

      const existingIndex = this.queue.findIndex((item) => item?.key === popup.key)
      if (existingIndex >= 0) {
        this.queue.splice(existingIndex, 1, popup)
      } else {
        this.queue.push(popup)
      }

      this.showNextPopup()
    },

    async refreshPendingExclusiveOrders() {
      const session = this.syncSession()
      if (!session) return
      if (refreshPromise) return refreshPromise

      refreshPromise = (async () => {
        try {
          const res = await get('/attendant/orders', {
            attendantId: session.userId,
            orderStatus: 8,
            page: 0,
            size: 20,
          })
          const orders = Array.isArray(res?.data?.content) ? res.data.content : []
          orders.forEach((order) => {
            if (Number(order?.orderStatus) !== 8) return
            const popup = buildPopupPayload(order, session.userId)
            const { route, orderId: currentOrderId } = getCurrentPageContext()
            if (!shouldOpenExclusiveDispatchPopup({
              userId: session.userId,
              orderId: popup.orderId,
              orderStatus: 8,
              remainingMs: popup.remainingMs,
              ignoredKeys: this.ignoredKeysSet,
              currentRoute: route,
              currentOrderId,
            })) {
              return
            }
            this.enqueuePopup(popup)
          })
        } catch (error) {
          console.error('刷新专属派单失败:', error)
        } finally {
          refreshPromise = null
        }
      })()

      return refreshPromise
    },

    async fetchExclusiveOrder(orderId) {
      const nextOrderId = Number(orderId || 0)
      if (!nextOrderId || pendingOrderIds.has(nextOrderId)) return
      const session = this.syncSession()
      if (!session) return

      pendingOrderIds.add(nextOrderId)
      try {
        const res = await get(`/attendant/orders/${nextOrderId}`)
        const order = res?.data || {}
        const latestStatus = Number(order.orderStatus || 0)
        if (latestStatus !== 8) {
          this.clearOrderState(nextOrderId)
          return
        }

        const popup = buildPopupPayload(order, session.userId)
        const { route, orderId: currentOrderId } = getCurrentPageContext()
        if (!shouldOpenExclusiveDispatchPopup({
          userId: session.userId,
          orderId: popup.orderId,
          orderStatus: latestStatus,
          remainingMs: popup.remainingMs,
          ignoredKeys: this.ignoredKeysSet,
          currentRoute: route,
          currentOrderId,
        })) {
          return
        }

        this.enqueuePopup(popup)
      } catch (error) {
        console.error('加载专属派单摘要失败:', error)
      } finally {
        pendingOrderIds.delete(nextOrderId)
      }
    },

    handleOrderMessage(message) {
      const session = this.syncSession()
      if (!session || !message) return

      const payload = message.data && typeof message.data === 'object' ? message.data : {}
      const type = String(message.type || message.eventType || payload.type || '').toUpperCase()
      if (type !== 'ORDER_STATUS_CHANGED') return

      const orderId = Number(payload.orderId ?? message.orderId ?? 0)
      if (!orderId) return

      const orderStatus = Number(payload.orderStatus ?? message.orderStatus ?? 0)
      if (orderStatus === 8) {
        this.fetchExclusiveOrder(orderId)
        return
      }

      this.clearOrderState(orderId)
    },

    async viewOrderDetail(orderId = this.popup?.orderId) {
      const nextOrderId = Number(orderId || 0)
      if (!nextOrderId) return
      this.closePopup()
      uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${nextOrderId}` })
    },

    async acceptExclusiveOrder(orderId = this.popup?.orderId, { redirectToDetail = true } = {}) {
      const nextOrderId = Number(orderId || 0)
      const session = this.syncSession()
      if (!session || !nextOrderId) {
        return { ok: false, message: '订单信息有误' }
      }

      const isCurrentPopupOrder = this.popup?.orderId === nextOrderId
      if (isCurrentPopupOrder && this.actionLoading) {
        return { ok: false, message: '接单中' }
      }

      if (isCurrentPopupOrder) this.actionLoading = true
      try {
        const res = await post(`/attendant/orders/${nextOrderId}/accept?attendantId=${session.userId}`)
        const acceptedOrderId = Number(res?.data?.orderId || nextOrderId)
        this.clearOrderState(acceptedOrderId)
        uni.$emit('escort-order-updated', {
          action: 'accepted',
          orderId: acceptedOrderId,
          orderNo: res?.data?.orderNo || this.popup?.orderNo || '',
        })
        uni.showToast({ title: '接单成功', icon: 'success' })
        if (redirectToDetail) {
          setTimeout(() => {
            uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${acceptedOrderId}` })
          }, 220)
        }
        return { ok: true, orderId: acceptedOrderId }
      } catch (error) {
        const message = error?.message || error?.errMsg || '接单失败，请稍后重试'
        const terminal = isTerminalAcceptError(error)
        if (terminal) {
          this.clearOrderState(nextOrderId)
          uni.$emit('escort-order-updated', {
            action: 'refresh',
            orderId: nextOrderId,
          })
        }
        uni.showToast({ title: message, icon: 'none' })
        return { ok: false, terminal, message }
      } finally {
        if (isCurrentPopupOrder) {
          this.actionLoading = false
        }
      }
    },

    async rejectExclusiveOrder(orderId = this.popup?.orderId, reason = '当前时段无法接单') {
      const nextOrderId = Number(orderId || 0)
      const session = this.syncSession()
      if (!session || !nextOrderId) {
        return { ok: false, message: '订单信息有误' }
      }

      const isCurrentPopupOrder = this.popup?.orderId === nextOrderId
      if (isCurrentPopupOrder && this.actionLoading) {
        return { ok: false, message: '处理中' }
      }

      if (isCurrentPopupOrder) this.actionLoading = true
      try {
        const encodedReason = encodeURIComponent(reason || '当前时段无法接单')
        const res = await post(`/attendant/orders/${nextOrderId}/reject-assigned?reason=${encodedReason}`)
        this.clearOrderState(nextOrderId)
        uni.$emit('escort-order-updated', {
          action: 'rejected-assigned',
          orderId: nextOrderId,
        })
        uni.showToast({ title: res?.data || '已拒绝派单', icon: 'none' })
        return { ok: true }
      } catch (error) {
        const message = error?.message || error?.errMsg || '拒绝失败，请稍后重试'
        const terminal = isTerminalAcceptError(error)
        if (terminal) {
          this.clearOrderState(nextOrderId)
          uni.$emit('escort-order-updated', {
            action: 'refresh',
            orderId: nextOrderId,
          })
        }
        uni.showToast({ title: message, icon: 'none' })
        return { ok: false, terminal, message }
      } finally {
        if (isCurrentPopupOrder) {
          this.actionLoading = false
        }
      }
    },
  },
})

export const hasExclusiveDispatchListener = () => listenerBound
