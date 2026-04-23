<template>
  <teleport to="body">
    <div v-if="modelValue" class="dialog-mask" @click.self="emit('update:modelValue', false)">
      <section class="dialog-card" :style="width ? { maxWidth: width } : undefined">
        <header class="dialog-header">
          <div>
            <p v-if="eyebrow" class="dialog-eyebrow">{{ eyebrow }}</p>
            <h3 class="dialog-title">{{ title }}</h3>
            <p v-if="description" class="dialog-description">{{ description }}</p>
          </div>
          <button class="dialog-close" type="button" @click="emit('update:modelValue', false)">
            ×
          </button>
        </header>

        <div class="dialog-body">
          <slot />
        </div>

        <footer v-if="$slots.footer" class="dialog-footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </teleport>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  eyebrow: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '720px'
  }
})

const emit = defineEmits(['update:modelValue'])
</script>
