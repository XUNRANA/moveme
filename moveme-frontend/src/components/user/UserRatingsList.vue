<template>
  <div class="ratings-section">
    <div class="section-header">
      <h2 class="section-title">
        <svg viewBox="0 0 24 24" fill="var(--accent-gold)" width="22" height="22">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
        我的评分
      </h2>
      <span v-if="total > 0" class="section-count">共 {{ total }} 条</span>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && ratings.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
      </div>
      <p>还没有评分记录</p>
      <router-link to="/" class="empty-link">去发现好电影</router-link>
    </div>

    <!-- Rating List -->
    <div v-else class="rating-list">
      <router-link
        v-for="item in ratings"
        :key="item.id"
        :to="`/movies/${item.movieId}`"
        class="rating-item"
      >
        <div class="rating-poster">
          <PosterImage
            :src="resolvePoster(item)"
            :alt="item.title"
            ratio="2-3"
          />
        </div>
        <div class="rating-info">
          <h4 class="rating-title">{{ item.title }}</h4>
          <div class="rating-stars">
            <span v-for="s in 5" :key="s" class="star" :class="{ filled: s <= Math.round(item.score / 2) }">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
              </svg>
            </span>
            <span class="score-text">{{ item.score }}分</span>
          </div>
          <p v-if="item.comment" class="rating-comment">{{ item.comment }}</p>
          <span class="rating-time">{{ formatDate(item.createdAt) }}</span>
        </div>
      </router-link>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载评分中..." />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage <= 1"
        @click="loadPage(currentPage - 1)"
      >上一页</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages"
        @click="loadPage(currentPage + 1)"
      >下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserRatings, resolvePoster, type UserRatingItem } from '../../api/movies'
import { formatDate } from '../../utils/date'
import PosterImage from '../common/PosterImage.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const ratings = ref<UserRatingItem[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function loadPage(page: number) {
  loading.value = true
  try {
    const res = await getUserRatings(page, 5)
    ratings.value = res.records
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

/* Rating List */
.rating-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--glass-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.rating-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-primary);
  text-decoration: none;
  color: inherit;
  transition: background var(--duration-fast) var(--ease-out);
}

.rating-item:hover {
  background: var(--glass);
}

.rating-poster {
  width: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.rating-info {
  flex: 1;
  min-width: 0;
}

.rating-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 6px;
}

.star {
  color: var(--glass-border);
  display: flex;
}

.star.filled {
  color: var(--accent-gold);
}

.score-text {
  margin-left: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--accent-gold);
}

.rating-comment {
  font-size: 0.8rem;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rating-time {
  font-size: 0.75rem;
  color: var(--text-muted);
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
