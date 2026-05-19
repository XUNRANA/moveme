<template>
  <div class="charts-page">
    <div class="page-header">
      <h1 class="page-title">分类排行榜</h1>
      <p class="page-desc">探索不同类型的电影宇宙</p>
    </div>

    <!-- Bento Grid Genre Entry -->
    <div v-if="!selectedGenre" class="page-content">
      <GenreBentoGrid :genres="genres" />
    </div>

    <!-- Selected Genre Grid -->
    <div v-else class="page-content">
      <div class="genre-header">
        <button class="back-btn" @click="selectedGenre = ''">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="m15 18-6-6 6-6"/></svg>
          返回分类
        </button>
        <h2 class="genre-title">{{ selectedGenre }}</h2>
      </div>
      <LoadingSpinner v-if="genreLoading" text="加载中..." />
      <MovieGrid v-else :movies="genreMovies" :cols="5" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { MovieItem } from '../api/movies'
import { listChartGenres, getChartByGenre } from '../api/movies'
import { chartItemToMovie } from '../utils/movie'
import type { GenreCard } from '../api/movies'
import GenreBentoGrid from '../components/movie/GenreBentoGrid.vue'
import MovieGrid from '../components/movie/MovieGrid.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'

const route = useRoute()
const genres = ref<GenreCard[]>([])
const selectedGenre = ref('')
const genreMovies = ref<MovieItem[]>([])
const genreLoading = ref(false)

const sizes: GenreCard['size'][] = ['large', 'medium', 'medium', 'small', 'small', 'large']


async function loadGenre(genre: string) {
  if (!genre) return
  genreLoading.value = true
  try {
    const data = await getChartByGenre(genre)
    genreMovies.value = data.movies.map(chartItemToMovie)
  } catch (e) {
    console.error('Failed to load genre chart:', e)
    genreMovies.value = []
  } finally {
    genreLoading.value = false
  }
}

watch(selectedGenre, (g) => {
  if (g) loadGenre(g)
})

watch(() => route.query.genre, (genre) => {
  if (genre) {
    selectedGenre.value = String(genre)
  }
}, { immediate: true })

onMounted(async () => {
  try {
    const genreNames = await listChartGenres()
    // Fetch each genre's chart to get the first movie's poster
    const charts = await Promise.allSettled(
      genreNames.map(name => getChartByGenre(name))
    )
    genres.value = genreNames.map((name, i) => {
      const result = charts[i]
      const firstMovie = result.status === 'fulfilled' ? result.value.movies[0] : null
      return {
        name,
        posterUrl: firstMovie?.posterUrl || '',
        movieCount: result.status === 'fulfilled' ? result.value.movies.length : 0,
        size: sizes[i % sizes.length],
      }
    })
  } catch (e) {
    console.error('Failed to load chart genres:', e)
  }
})
</script>

<style scoped>
.charts-page { min-height: 100vh; }

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

.genre-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.back-btn:hover {
  color: var(--text-primary);
  background: var(--glass-hover);
}

.genre-title {
  font-family: var(--font-display);
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .page-header { padding: 32px 16px 0; }
  .page-content { padding: 24px 16px; }
}
</style>
