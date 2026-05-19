<template>
  <div class="history-section">
    <div class="section-header">
      <h2 class="section-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="#8b5cf6" stroke-width="2" width="22" height="22">
          <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
        </svg>
        浏览记录
      </h2>
      <span v-if="total > 0" class="section-count">共 {{ total }} 条</span>
    </div>

    <!-- Empty -->
    <div v-if="!loading && history.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
          <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
        </svg>
      </div>
      <p>还没有浏览记录</p>
    </div>

    <!-- Timeline -->
    <div v-else class="timeline">
      <div
        v-for="(group, date) in groupedHistory"
        :key="date"
        class="timeline-group"
      >
        <div class="timeline-date">
          <span class="date-dot" />
          <span class="date-text">{{ date }}</span>
        </div>
        <div class="timeline-items">
          <router-link
            v-for="item in group"
            :key="item.id"
            :to="`/movies/${item.movieId}`"
            class="timeline-item"
          >
            <div class="item-poster">
              <PosterImage
                :src="resolveHistoryPoster(item)"
                :alt="item.title"
                ratio="2-3"
              />
            </div>
            <div class="item-info">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-time">{{ formatTime(item.viewedAt) }}</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载历史中..." />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="loadPage(currentPage - 1)">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="loadPage(currentPage + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getUserHistory, type UserHistoryItem } from '../../api/movies'
import { formatTime } from '../../utils/date'
import PosterImage from '../common/PosterImage.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const history = ref<UserHistoryItem[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)

function resolveHistoryPoster(item: UserHistoryItem) {
  return item.posterLocalPath || item.posterUrl || undefined
}

const groupedHistory = computed(() => {
  const groups: Record<string, UserHistoryItem[]> = {}
  for (const item of history.value) {
    const d = new Date(item.viewedAt)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    if (!groups[key]) groups[key] = []
    groups[key].push(item)
  }
  return groups
})

async function loadPage(page: number) {
  loading.value = true
  try {
    const res = await getUserHistory(page, 5)
    history.value = res.records
    total.value = res.total
    totalPages.value = res.pages
    currentPage.value = res.current
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
}

onMounted(() => loadPage(1))
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text-primary);
}

.section-count {
  font-size: 0.8rem;
  color: var(--text-muted);
}

/* Empty */
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-state p {
  font-size: 0.9rem;
}

/* Timeline */
.timeline {
  position: relative;
}

.timeline-group {
  margin-bottom: 28px;
}

.timeline-group:last-child {
  margin-bottom: 0;
}

.timeline-date {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  position: relative;
}

.date-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-red);
  flex-shrink: 0;
}

.date-text {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.timeline-items {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-left: 18px;
  padding-left: 18px;
  border-left: 2px solid var(--glass-border);
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: inherit;
  transition: background var(--duration-fast) var(--ease-out);
}

.timeline-item:hover {
  background: var(--glass);
}

.item-poster {
  width: 36px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
}

.item-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-time {
  font-size: 0.75rem;
  color: var(--text-muted);
  flex-shrink: 0;
  margin-left: 12px;
}

/* Loading */
.loading-state {
  padding: 40px 0;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 20px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.page-btn:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: var(--glass-border-hover);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.8rem;
  color: var(--text-muted);
}
</style>
