<template>
  <span class="rank-badge">
    #{{ rank }}
  </span>
</template>

<script setup lang="ts">
defineProps<{
  rank: number
}>()
</script>

<style scoped>
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  padding: 0 8px;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  color: white;
  font-size: 0.75rem;
  font-weight: 800;
  border-radius: var(--radius-pill);
  letter-spacing: 0.02em;
}
</style>
