<template>
  <div ref="rootRef" class="date-control" :class="{ 'is-open': open, 'is-disabled': disabled }">
    <button class="date-trigger" type="button" :disabled="disabled" @click="toggle" @keydown.escape="close">
      <span :class="{ 'is-placeholder': !displayValue }">{{ displayValue || placeholder }}</span>
      <span class="date-trigger-icon">日</span>
    </button>

    <Teleport to="body">
      <div v-if="open" ref="panelRef" class="date-menu date-menu-floating" :style="floatingStyle">
        <div class="date-calendar-header">
          <button class="date-nav" type="button" aria-label="上个月" @click="moveMonth(-1)">‹</button>
          <strong>{{ monthLabel }}</strong>
          <button class="date-nav" type="button" aria-label="下个月" @click="moveMonth(1)">›</button>
        </div>

        <div class="date-weekdays">
          <span v-for="weekday in weekdays" :key="weekday">{{ weekday }}</span>
        </div>

        <div class="date-grid">
          <button
            v-for="day in calendarDays"
            :key="day.key"
            class="date-day"
            :class="{
              'is-muted': !day.inMonth,
              'is-today': day.isToday,
              'is-selected': day.isSelected
            }"
            type="button"
            @click="selectDay(day.date)"
          >
            {{ day.label }}
          </button>
        </div>

        <div v-if="mode === 'datetime'" class="date-time-row">
          <label>
            <span>小时</span>
            <input v-model="hourText" class="date-time-input" inputmode="numeric" maxlength="2" @blur="normalizeTime" />
          </label>
          <span class="date-time-divider">:</span>
          <label>
            <span>分钟</span>
            <input v-model="minuteText" class="date-time-input" inputmode="numeric" maxlength="2" @blur="normalizeTime" />
          </label>
        </div>

        <div class="date-actions">
          <button class="button button-ghost" type="button" @click="clearValue">清空</button>
          <button class="button button-secondary" type="button" @click="chooseToday">今天</button>
          <button v-if="mode === 'datetime'" class="button button-primary" type="button" @click="confirmDateTime">确定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  mode: {
    type: String,
    default: 'date',
    validator: (value) => ['date', 'datetime'].includes(value)
  },
  placeholder: {
    type: String,
    default: '请选择日期'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const rootRef = ref(null)
const panelRef = ref(null)
const open = ref(false)
const floatingStyle = ref({})
const selectedDate = ref(null)
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth())
const hourText = ref('00')
const minuteText = ref('00')

const pad = (value) => String(value).padStart(2, '0')

const parseDate = (value) => {
  if (!value) return null
  const datePart = String(value).slice(0, 10)
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(datePart)
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!year || month < 1 || month > 12 || day < 1 || day > 31) return null
  return new Date(year, month - 1, day)
}

const formatDate = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`

const parseTime = (value) => {
  const match = /T(\d{2}):(\d{2})/.exec(String(value || ''))
  return {
    hour: match ? match[1] : '00',
    minute: match ? match[2] : '00'
  }
}

const sameDay = (left, right) => Boolean(left && right && formatDate(left) === formatDate(right))

const displayValue = computed(() => {
  if (!props.modelValue) return ''
  if (props.mode === 'datetime') {
    const [datePart, timePart = '00:00'] = props.modelValue.split('T')
    return `${datePart} ${timePart}`
  }
  return props.modelValue
})

const monthLabel = computed(() => `${viewYear.value}年${pad(viewMonth.value + 1)}月`)

const calendarDays = computed(() => {
  const firstDay = new Date(viewYear.value, viewMonth.value, 1)
  const mondayOffset = (firstDay.getDay() + 6) % 7
  const startDate = new Date(viewYear.value, viewMonth.value, 1 - mondayOffset)
  const today = new Date()

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)
    return {
      key: formatDate(date),
      date,
      label: date.getDate(),
      inMonth: date.getMonth() === viewMonth.value,
      isToday: sameDay(date, today),
      isSelected: sameDay(date, selectedDate.value)
    }
  })
})

const syncFromModel = () => {
  const parsedDate = parseDate(props.modelValue) || new Date()
  selectedDate.value = parseDate(props.modelValue)
  viewYear.value = parsedDate.getFullYear()
  viewMonth.value = parsedDate.getMonth()
  const time = parseTime(props.modelValue)
  hourText.value = time.hour
  minuteText.value = time.minute
}

const updateFloatingPosition = () => {
  const root = rootRef.value
  if (!root) return
  const rect = root.getBoundingClientRect()
  const viewportGap = 12
  const panelMaxHeight = props.mode === 'datetime' ? 440 : 380
  const spaceBelow = window.innerHeight - rect.bottom - viewportGap
  const spaceAbove = rect.top - viewportGap
  const shouldOpenUp = spaceBelow < 280 && spaceAbove > spaceBelow
  const maxHeight = Math.max(260, Math.min(panelMaxHeight, shouldOpenUp ? spaceAbove - 8 : spaceBelow - 8))

  floatingStyle.value = {
    position: 'fixed',
    top: shouldOpenUp ? 'auto' : `${rect.bottom + 8}px`,
    bottom: shouldOpenUp ? `${window.innerHeight - rect.top + 8}px` : 'auto',
    left: `${Math.max(12, Math.min(rect.left, window.innerWidth - 344))}px`,
    width: `${Math.max(320, Math.min(340, window.innerWidth - 24))}px`,
    maxHeight: `${maxHeight}px`
  }
}

const close = () => {
  open.value = false
}

const toggle = () => {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) {
    syncFromModel()
    nextTick(updateFloatingPosition)
  }
}

const emitValue = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
}

const normalizeNumber = (value, max) => {
  const numberValue = Number(String(value).replace(/\D/g, ''))
  if (Number.isNaN(numberValue)) return '00'
  return pad(Math.min(max, Math.max(0, numberValue)))
}

const normalizeTime = () => {
  hourText.value = normalizeNumber(hourText.value, 23)
  minuteText.value = normalizeNumber(minuteText.value, 59)
}

const selectDay = (date) => {
  selectedDate.value = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  viewYear.value = date.getFullYear()
  viewMonth.value = date.getMonth()
  if (props.mode === 'date') {
    emitValue(formatDate(selectedDate.value))
    close()
  }
}

const confirmDateTime = () => {
  if (!selectedDate.value) {
    selectedDate.value = new Date()
  }
  normalizeTime()
  emitValue(`${formatDate(selectedDate.value)}T${hourText.value}:${minuteText.value}`)
  close()
}

const chooseToday = () => {
  const today = new Date()
  selectDay(today)
  if (props.mode === 'datetime') {
    selectedDate.value = today
    viewYear.value = today.getFullYear()
    viewMonth.value = today.getMonth()
    hourText.value = pad(today.getHours())
    minuteText.value = pad(today.getMinutes())
  }
}

const clearValue = () => {
  emitValue('')
  close()
}

const moveMonth = (step) => {
  const next = new Date(viewYear.value, viewMonth.value + step, 1)
  viewYear.value = next.getFullYear()
  viewMonth.value = next.getMonth()
}

const handleDocumentClick = (event) => {
  if (rootRef.value?.contains(event.target) || panelRef.value?.contains(event.target)) return
  close()
}

const handleViewportChange = () => {
  if (open.value) updateFloatingPosition()
}

watch(
  () => props.modelValue,
  () => {
    if (open.value) syncFromModel()
  }
)

watch(open, async (value) => {
  if (!value) return
  await nextTick()
  updateFloatingPosition()
})

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentClick)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentClick)
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
})
</script>
