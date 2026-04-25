<template>
  <Teleport to="body">
    <div v-if="modelValue" class="drawer-mask" @click.self="close">
      <aside class="drawer-panel" :style="width ? { maxWidth: width } : undefined">
        <header class="drawer-header">
          <div>
            <p v-if="eyebrow" class="drawer-eyebrow">{{ eyebrow }}</p>
            <h3 class="drawer-title">{{ title }}</h3>
            <p v-if="description" class="drawer-description">{{ description }}</p>
          </div>
          <button class="drawer-close" type="button" aria-label="关闭详情" @click="close">
            <span></span>
            <span></span>
          </button>
        </header>
        <div class="drawer-body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="drawer-footer">
          <slot name="footer" />
        </footer>
      </aside>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const close = () => {
  emit('update:modelValue', false)
}
</script>
