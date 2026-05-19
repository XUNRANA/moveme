<template>
  <div class="section-title">
    <h2 class="title-text">{{ title }}</h2>
    <router-link v-if="to" :to="to" class="view-all">
      查看全部
      <span class="arrow">→</span>
    </router-link>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  to?: string
}>()
</script>

<style scoped>
.section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0 4px;
  margin-bottom: 20px;
}

.title-text {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.view-all {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.875rem;
  color: var(--text-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.view-all:hover {
  color: var(--accent-red);
}

.arrow {
  transition: transform var(--duration-fast) var(--ease-out);
}

.view-all:hover .arrow {
  transform: translateX(4px);
}
</style>
