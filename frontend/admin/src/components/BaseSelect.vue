<template>
  <div ref="rootRef" class="select-control" :class="{ 'is-open': open, 'is-disabled': disabled }">
    <button class="select-trigger" type="button" :disabled="disabled" @click="toggle" @keydown.escape="close">
      <span>{{ selectedLabel }}</span>
      <span class="select-caret"></span>
    </button>
    <Teleport to="body">
      <div v-if="open" ref="menuRef" class="select-menu select-menu-floating" :style="floatingStyle">
        <button
          v-for="option in options"
          :key="`${option.value}-${option.label}`"
          class="select-option"
          :class="{ 'is-selected': isSelected(option.value) }"
          type="button"
          @click="choose(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    required: true
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const rootRef = ref(null)
const menuRef = ref(null)
const open = ref(false)
const floatingStyle = ref({})

const isSameValue = (left, right) => String(left ?? '') === String(right ?? '')

const selectedLabel = computed(() => {
  const option = props.options.find((item) => isSameValue(item.value, props.modelValue))
  return option?.label || props.placeholder
})

const isSelected = (value) => isSameValue(value, props.modelValue)

const close = () => {
  open.value = false
}

const updateFloatingPosition = () => {
  const root = rootRef.value
  if (!root) return
  const rect = root.getBoundingClientRect()
  const viewportGap = 12
  const menuMaxHeight = 260
  const spaceBelow = window.innerHeight - rect.bottom - viewportGap
  const spaceAbove = rect.top - viewportGap
  const shouldOpenUp = spaceBelow < 160 && spaceAbove > spaceBelow
  const maxHeight = Math.max(140, Math.min(menuMaxHeight, shouldOpenUp ? spaceAbove - 8 : spaceBelow - 8))

  floatingStyle.value = {
    position: 'fixed',
    top: shouldOpenUp ? 'auto' : `${rect.bottom + 8}px`,
    bottom: shouldOpenUp ? `${window.innerHeight - rect.top + 8}px` : 'auto',
    left: `${rect.left}px`,
    width: `${rect.width}px`,
    maxHeight: `${maxHeight}px`
  }
}

const toggle = () => {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) {
    nextTick(updateFloatingPosition)
  }
}

const choose = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
  close()
}

const handleDocumentClick = (event) => {
  if (rootRef.value?.contains(event.target) || menuRef.value?.contains(event.target)) return
  close()
}

const handleViewportChange = () => {
  if (open.value) updateFloatingPosition()
}

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
