<template>
  <div class="favorites-section">
    <div class="section-header">
      <h2 class="section-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="var(--accent-red)" stroke-width="2" width="22" height="22">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
        </svg>
        我的收藏
      </h2>

      <!-- Tabs -->
      <div class="tab-bar">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 0 }"
          @click="switchTab(0)"
        >想看 ({{ wishCount }})</button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 1 }"
          @click="switchTab(1)"
        >看过 ({{ watchedCount }})</button>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && favorites.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
        </svg>
      </div>
      <p>{{ activeTab === 0 ? '还没有想看的电影' : '还没有看过的电影' }}</p>
      <router-link to="/" class="empty-link">去发现好电影</router-link>
    </div>

    <!-- Grid -->
    <div v-else class="favorites-grid">
      <MovieCard
        v-for="item in favorites"
        :key="item.id"
        :movie="{ id: item.movieId, doubanId: '', title: item.title, posterUrl: item.posterUrl, posterLocalPath: item.posterLocalPath, doubanRating: item.doubanRating, year: item.year, genres: item.genres }"
      />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载收藏中..." />
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
import { ref, onMounted } from 'vue'
import { getUserFavorites, type UserFavoriteItem } from '../../api/movies'
import MovieCard from '../movie/MovieCard.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const activeTab = ref(0) // 0=wish, 1=watched
const favorites = ref<UserFavoriteItem[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const wishCount = ref(0)
const watchedCount = ref(0)

async function loadPage(page: number) {
  loading.value = true
  try {
    const status = activeTab.value // 0=wish, 1=watched
    const res = await getUserFavorites(status, page, 5)
    favorites.value = res.records
    total.value = res.total
    totalPages.value = res.pages
    currentPage.value = res.current
    if (status === 0) wishCount.value = res.total
    else watchedCount.value = res.total
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
}

function switchTab(tab: number) {
  activeTab.value = tab
  loadPage(1)
}

onMounted(() => loadPage(1))
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
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

/* Tabs */
.tab-bar {
  display: flex;
  gap: 4px;
  background: var(--glass);
  border-radius: var(--radius-pill);
  padding: 3px;
}

.tab-btn {
  padding: 8px 20px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-muted);
  border-radius: var(--radius-pill);
  transition: all var(--duration-fast) var(--ease-out);
}

.tab-btn.active {
  color: var(--text-primary);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  font-weight: 600;
}

.tab-btn:hover:not(.active) {
  color: var(--text-secondary);
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
  margin-bottom: 12px;
}

.empty-link {
  font-size: 0.8rem;
  color: var(--accent-red);
  text-decoration: none;
  font-weight: 500;
}

.empty-link:hover {
  text-decoration: underline;
}

/* Grid */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 20px;
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
