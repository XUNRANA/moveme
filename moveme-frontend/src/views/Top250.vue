<template>
  <div class="top250-page">
    <div class="page-header">
      <h1 class="page-title">豆瓣 Top 250</h1>
      <p class="page-desc">被时间验证的经典之作</p>
    </div>

    <div class="page-content">
      <LoadingSpinner v-if="loading" text="加载中..." />
      <MovieGrid
        v-else
        :movies="movies"
        :cols="5"
        show-rank
        :rank-offset="(currentPage - 1) * pageSize + 1"
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { MovieItem } from '../api/movies'
import { getTop250 } from '../api/movies'
import { chartItemToMovie } from '../utils/movie'
import MovieGrid from '../components/movie/MovieGrid.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'

const movies = ref<MovieItem[]>([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = 20
const allMovies = ref<MovieItem[]>([])

const totalPages = computed(() => Math.ceil(allMovies.value.length / pageSize))

onMounted(async () => {
  try {
    const data = await getTop250()
    allMovies.value = data.movies.map(chartItemToMovie)
    movies.value = allMovies.value.slice(0, pageSize)
  } catch (e) {
    console.error('Failed to load Top250:', e)
  } finally {
    loading.value = false
  }
})

function onPageChange(page: number) {
  currentPage.value = page
  const start = (page - 1) * pageSize
  movies.value = allMovies.value.slice(start, start + pageSize)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.top250-page { min-height: 100vh; }

.page-header {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 48px 32px 0;
}

.page-title {
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 900;
  color: var(--text-primary);
  letter-spacing: -0.03em;
}

.page-desc {
  font-size: 1rem;
  color: var(--text-muted);
  margin-top: 8px;
}

.page-content {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 32px;
}

@media (max-width: 768px) {
  .page-header { padding: 32px 16px 0; }
  .page-title { font-size: 1.8rem; }
  .page-content { padding: 24px 16px; }
}
</style>
