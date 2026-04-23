<template>
  <article class="overview-card">
    <div class="overview-meta">
      <p class="overview-label">{{ label }}</p>
      <span v-if="badge" class="overview-badge" :class="badgeClass">{{ badge }}</span>
    </div>
    <div class="overview-value">{{ value }}</div>
    <p v-if="hint" class="overview-hint">{{ hint }}</p>
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

<style scoped>
.overview-card {
  border-radius: 8px;
  padding: 12px;
  min-height: 108px;
}

.overview-meta {
  gap: 8px;
}

.overview-label {
  font-size: 0.8rem;
  line-height: 1.2;
}

.overview-value {
  margin-top: 10px;
  font-size: 1.62rem;
}

.overview-hint {
  margin-top: 6px;
  font-size: 0.76rem;
  line-height: 1.3;
}

.overview-badge {
  font-size: 0.68rem;
}
</style>
