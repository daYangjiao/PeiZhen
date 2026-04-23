<template>
  <article class="overview-card">
    <div class="overview-meta">
      <p class="overview-label">{{ label }}</p>
      <span class="overview-badge" :class="badgeClass">{{ badge }}</span>
    </div>
    <div class="overview-value">{{ value }}</div>
    <p class="overview-hint">{{ hint }}</p>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  hint: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'neutral'
  }
})

const badgeClass = computed(() => `tone-${props.tone}`)
</script>
