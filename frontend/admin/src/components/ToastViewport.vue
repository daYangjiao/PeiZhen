<template>
  <teleport to="body">
    <div class="toast-viewport">
      <div
        v-for="toast in uiStore.toasts"
        :key="toast.id"
        class="toast-item"
        :class="`toast-${toast.type}`"
      >
        <div class="toast-dot"></div>
        <span>{{ toast.message }}</span>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { useUiStore } from '../stores/ui'

const uiStore = useUiStore()
</script>
