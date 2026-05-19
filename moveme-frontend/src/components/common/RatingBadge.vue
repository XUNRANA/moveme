<template>
  <span class="rating-badge" :class="[size, { glow }]">
    <span class="star">★</span>
    <span class="value">{{ formatted }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  value?: number | null
  size?: 'sm' | 'md' | 'lg'
  glow?: boolean
}>(), {
  size: 'md',
  glow: false,
})

const formatted = computed(() => {
  if (props.value == null) return '-'
  return props.value.toFixed(1)
})
</script>

<style scoped>
.rating-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
  color: var(--accent-gold);
  white-space: nowrap;
}

.rating-badge.glow {
  text-shadow: 0 0 12px var(--accent-gold-glow);
}

.star {
  font-size: 0.85em;
}

.sm { font-size: 0.75rem; }
.md { font-size: 0.875rem; }
.lg { font-size: 1.25rem; }
</style>
