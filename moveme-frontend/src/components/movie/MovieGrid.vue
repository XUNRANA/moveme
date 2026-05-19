<template>
  <div class="movie-grid-wrap">
    <div class="movie-grid" :class="[`cols-${cols}`]">
      <MovieCard
        v-for="(movie, i) in movies"
        :key="movie.id"
        :movie="movie"
        :rank="showRank ? (rankOffset + i) : undefined"
        class="grid-item"
        :style="{ animationDelay: `${i * 40}ms` }"
      />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="grid-pagination">
      <button
        class="page-btn"
        :disabled="currentPage <= 1"
        @click="$emit('page-change', currentPage - 1)"
      >上一页</button>

      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="page-dots">...</span>
        <button
          v-else
          class="page-btn"
          :class="{ active: p === currentPage }"
          @click="$emit('page-change', p as number)"
        >{{ p }}</button>
      </template>

      <button
        class="page-btn"
        :disabled="currentPage >= totalPages"
        @click="$emit('page-change', currentPage + 1)"
      >下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MovieItem } from '../../api/movies'
import MovieCard from './MovieCard.vue'

const props = withDefaults(defineProps<{
  movies: MovieItem[]
  cols?: number
  showRank?: boolean
  rankOffset?: number
  currentPage?: number
  totalPages?: number
}>(), {
  cols: 5,
  showRank: false,
  rankOffset: 1,
  currentPage: 1,
  totalPages: 1,
})

defineEmits<{
  'page-change': [page: number]
}>()

const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.currentPage
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages: (number | string)[] = [1]
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})
</script>

<style scoped>
.movie-grid-wrap {
  overflow: hidden;
}

.movie-grid {
  display: grid;
  gap: 24px 16px;
  overflow: hidden;
}

.cols-4 { grid-template-columns: repeat(4, 1fr); }
.cols-5 { grid-template-columns: repeat(5, 1fr); }
.cols-6 { grid-template-columns: repeat(6, 1fr); }

.grid-item {
  animation: card-fade-in 0.4s var(--ease-out) forwards;
  opacity: 0;
  width: 100% !important;
  min-width: 0;
  max-width: 100%;
  flex-shrink: 1 !important;
}

/* Pagination */
.grid-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 48px;
}

.page-btn {
  padding: 8px 16px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.page-btn:hover:not(:disabled) {
  color: var(--text-primary);
  background: var(--glass-hover);
}

.page-btn.active {
  color: white;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-dots {
  color: var(--text-muted);
  padding: 0 4px;
}

@media (max-width: 1200px) { .cols-5 { grid-template-columns: repeat(4, 1fr); } .cols-6 { grid-template-columns: repeat(5, 1fr); } }
@media (max-width: 900px)  { .cols-4, .cols-5, .cols-6 { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 600px)  { .cols-4, .cols-5, .cols-6 { grid-template-columns: repeat(2, 1fr); } }
</style>
