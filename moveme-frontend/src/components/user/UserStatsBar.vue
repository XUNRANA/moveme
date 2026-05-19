<template>
  <div class="stats-bar">
    <div
      v-for="(stat, i) in stats"
      :key="stat.label"
      class="stat-card"
      :style="{ animationDelay: `${i * 80}ms` }"
    >
      <div class="stat-icon" :style="{ color: stat.color }">
        <component :is="stat.icon" />
      </div>
      <div class="stat-value">{{ stat.value }}</div>
      <div class="stat-label">{{ stat.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { getUserStats, type UserStats } from '../../api/movies'

const data = ref<UserStats | null>(null)

onMounted(async () => {
  try {
    data.value = await getUserStats()
  } catch {
    // API not implemented yet
  }
})

const StarIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'currentColor', width: 24, height: 24 }, [
  h('path', { d: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z' })
])

const HeartIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 24, height: 24 }, [
  h('path', { d: 'M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z' })
])

const EyeIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 24, height: 24 }, [
  h('path', { d: 'M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z' }),
  h('circle', { cx: 12, cy: 12, r: 3 })
])

const ClockIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 24, height: 24 }, [
  h('circle', { cx: 12, cy: 12, r: 10 }),
  h('path', { d: 'M12 6v6l4 2' })
])

const stats = ref([
  { label: '评分', value: 0, icon: StarIcon, color: 'var(--accent-gold)' },
  { label: '想看', value: 0, icon: HeartIcon, color: 'var(--accent-red)' },
  { label: '看过', value: 0, icon: EyeIcon, color: '#3b82f6' },
  { label: '浏览记录', value: 0, icon: ClockIcon, color: '#8b5cf6' },
])

import { watch } from 'vue'

watch(data, (d) => {
  if (d) {
    stats.value[0].value = d.ratingCount
    stats.value[1].value = d.wishCount
    stats.value[2].value = d.watchedCount
    stats.value[3].value = d.historyCount
  }
})
</script>

<style scoped>
.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 24px;
  text-align: center;
  backdrop-filter: blur(12px);
  transition: all var(--duration-normal) var(--ease-out);
  animation: slide-up 0.5s var(--ease-out) forwards;
  opacity: 0;
}

.stat-card:hover {
  border-color: var(--glass-border-hover);
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.stat-icon {
  margin-bottom: 12px;
  display: flex;
  justify-content: center;
  opacity: 0.8;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 0.8rem;
  color: var(--text-muted);
  font-weight: 500;
}

@media (max-width: 640px) {
  .stats-bar {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
