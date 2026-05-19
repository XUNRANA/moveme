<template>
  <div class="stats-grid">
    <div
      v-for="(card, i) in cards"
      :key="card.label"
      class="stat-card"
      :style="{ animationDelay: `${i * 60}ms` }"
    >
      <div class="card-icon" :style="{ background: card.bg, color: card.color }">
        <component :is="card.icon" />
      </div>
      <div class="card-body">
        <div class="card-value">{{ card.value }}</div>
        <div class="card-label">{{ card.label }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h, watch } from 'vue'
import { getAdminStats, type AdminStats } from '../../api/movies'

const data = ref<AdminStats | null>(null)

onMounted(async () => {
  try {
    data.value = await getAdminStats()
  } catch {
    // API not implemented yet
  }
})

const FilmIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('rect', { x: 2, y: 2, width: 20, height: 20, rx: 2.18, ry: 2.18 }),
  h('line', { x1: 7, y1: 2, x2: 7, y2: 22 }),
  h('line', { x1: 17, y1: 2, x2: 17, y2: 22 }),
  h('line', { x1: 2, y1: 12, x2: 22, y2: 12 }),
  h('line', { x1: 2, y1: 7, x2: 7, y2: 7 }),
  h('line', { x1: 2, y1: 17, x2: 7, y2: 17 }),
  h('line', { x1: 17, y1: 7, x2: 22, y2: 7 }),
  h('line', { x1: 17, y1: 17, x2: 22, y2: 17 })
])

const UsersIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: 9, cy: 7, r: 4 }),
  h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75' })
])

const UserPlusIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: 9, cy: 7, r: 4 }),
  h('line', { x1: 19, y1: 8, x2: 19, y2: 14 }),
  h('line', { x1: 22, y1: 11, x2: 16, y2: 11 })
])

const StarIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('path', { d: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z' })
])

const SpiderIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('circle', { cx: 12, cy: 12, r: 3 }),
  h('path', { d: 'M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83' })
])

const ChartIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 20, height: 20 }, [
  h('path', { d: 'M12 20V10M18 20V4M6 20v-4' })
])

const cards = ref([
  { label: '电影总数', value: 0, icon: FilmIcon, color: '#3b82f6', bg: 'rgba(59,130,246,0.1)' },
  { label: '用户总数', value: 0, icon: UsersIcon, color: '#8b5cf6', bg: 'rgba(139,92,246,0.1)' },
  { label: '今日新增', value: 0, icon: UserPlusIcon, color: '#10b981', bg: 'rgba(16,185,129,0.1)' },
  { label: '评分总数', value: 0, icon: StarIcon, color: '#f59e0b', bg: 'rgba(245,158,11,0.1)' },
  { label: '爬虫状态', value: '--', icon: SpiderIcon, color: '#ef4444', bg: 'rgba(239,68,68,0.1)' },
  { label: '推荐日志', value: 0, icon: ChartIcon, color: '#06b6d4', bg: 'rgba(6,182,212,0.1)' },
])

watch(data, (d) => {
  if (d) {
    cards.value[0].value = d.movieCount
    cards.value[1].value = d.userCount
    cards.value[2].value = d.todayNewUsers
    cards.value[3].value = d.ratingCount
    cards.value[4].value = d.lastCrawlStatus || '--'
    cards.value[5].value = d.recoLogCount
  }
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.stat-card {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  backdrop-filter: blur(12px);
  transition: all var(--duration-normal) var(--ease-out);
  animation: slide-up 0.4s var(--ease-out) forwards;
  opacity: 0;
}

.stat-card:hover {
  border-color: var(--glass-border-hover);
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-value {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}

.card-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 2px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
