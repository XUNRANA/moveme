<template>
  <div class="system-panel">
    <div class="panel-header">
      <h3 class="panel-title">数据库概览</h3>
      <p class="panel-desc">各表记录数统计</p>
    </div>

    <div class="table-grid">
      <div
        v-for="(table, i) in tables"
        :key="table.name"
        class="table-card"
        :style="{ animationDelay: `${i * 40}ms` }"
      >
        <div class="table-name">{{ table.name }}</div>
        <div class="table-count">{{ table.count }}</div>
        <div class="table-label">{{ table.label }}</div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载系统数据..." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminStats } from '../../api/movies'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const loading = ref(false)

const tables = ref([
  { name: 'movies', count: 0, label: '电影' },
  { name: 'users', count: 0, label: '用户' },
  { name: 'ratings', count: 0, label: '评分' },
  { name: 'favorites', count: 0, label: '收藏' },
  { name: 'view_history', count: 0, label: '观影记录' },
  { name: 'search_history', count: 0, label: '搜索记录' },
  { name: 'persons', count: 0, label: '影人' },
  { name: 'genres', count: 0, label: '类型' },
  { name: 'crawl_logs', count: 0, label: '爬虫日志' },
  { name: 'import_logs', count: 0, label: '导入日志' },
  { name: 'recommendation_logs', count: 0, label: '推荐日志' },
  { name: 'movie_comments', count: 0, label: '评论' },
])

onMounted(async () => {
  loading.value = true
  try {
    const stats = await getAdminStats()
    tables.value[0].count = stats.movieCount
    tables.value[1].count = stats.userCount
    tables.value[2].count = stats.ratingCount
    tables.value[3].count = stats.favoriteCount
    tables.value[4].count = stats.viewHistoryCount
    tables.value[5].count = stats.searchHistoryCount
    tables.value[6].count = stats.personCount
    tables.value[7].count = stats.genreCount
    tables.value[8].count = stats.crawlLogCount
    tables.value[9].count = stats.importLogCount
    tables.value[10].count = stats.recoLogCount
    tables.value[11].count = stats.movieCommentCount
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.system-panel {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 24px;
}

.panel-header {
  margin-bottom: 24px;
}

.panel-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.panel-desc {
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-top: 4px;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.table-card {
  padding: 20px;
  background: var(--bg-primary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  text-align: center;
  transition: all var(--duration-normal) var(--ease-out);
  animation: slide-up 0.4s var(--ease-out) forwards;
  opacity: 0;
}

.table-card:hover {
  border-color: var(--glass-border-hover);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.table-name {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.table-count {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
}

.table-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-top: 6px;
}

.loading-state {
  padding: 40px 0;
}
</style>
