<template>
  <div ref="rootRef" class="select-control" :class="{ 'is-open': open, 'is-disabled': disabled }">
    <button class="select-trigger" type="button" :disabled="disabled" @click="toggle" @keydown.escape="close">
      <span>{{ selectedLabel }}</span>
      <span class="select-caret"></span>
    </button>
    <div v-if="open" class="select-menu">
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

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
const open = ref(false)

const isSameValue = (left, right) => String(left ?? '') === String(right ?? '')

const selectedLabel = computed(() => {
  const option = props.options.find((item) => isSameValue(item.value, props.modelValue))
  return option?.label || props.placeholder
})

const isSelected = (value) => isSameValue(value, props.modelValue)

const close = () => {
  open.value = false
}

const toggle = () => {
  if (props.disabled) return
  open.value = !open.value
}

const choose = (value) => {
  emit('update:modelValue', value)
  emit('change', value)
  close()
}

const handleDocumentClick = (event) => {
  if (!rootRef.value || rootRef.value.contains(event.target)) return
  close()
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentClick)
})
</script>
