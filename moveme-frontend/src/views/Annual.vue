<template>
  <div class="annual-page">
    <div class="page-header">
      <h1 class="page-title">年度榜单</h1>
      <p class="page-desc">回顾每一年的电影记忆</p>
    </div>

    <div class="page-content">
      <!-- Year Timeline -->
      <div class="year-timeline">
        <button
          v-for="y in years"
          :key="y"
          class="year-btn"
          :class="{ active: selectedYear === y }"
          @click="selectedYear = y"
        >{{ y }}</button>
      </div>

      <!-- Year Hero -->
      <div v-if="heroMovie" class="year-hero">
        <router-link :to="`/movies/${heroMovie.movieId}`" class="year-hero-link">
          <PosterImage :src="heroMovie.posterUrl" :alt="heroMovie.title" ratio="21-9" class="year-hero-poster" />
          <div class="year-hero-overlay" />
          <div class="year-hero-content">
            <span class="year-label">{{ selectedYear }} 年度热门</span>
            <h2 class="year-movie-title">{{ heroMovie.title }}</h2>
            <RatingBadge v-if="heroMovie.rating" :value="heroMovie.rating" size="lg" glow />
          </div>
        </router-link>
      </div>

      <!-- Charts -->
      <div v-for="chart in charts" :key="chart.genreName" class="chart-section">
        <h2 class="chart-title">{{ chart.boardTitle }}</h2>
        <MovieGrid :movies="chartMoviesMap[chart.genreName] || []" :cols="5" show-rank />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MovieChart, MovieItem } from '../api/movies'
import { listAnnualYears, getAnnualByYear } from '../api/movies'
import { chartItemToMovie } from '../utils/movie'
import MovieGrid from '../components/movie/MovieGrid.vue'
import PosterImage from '../components/common/PosterImage.vue'
import RatingBadge from '../components/common/RatingBadge.vue'

const route = useRoute()
const router = useRouter()
const years = ref<number[]>([])
const selectedYear = ref(0)
const charts = ref<MovieChart[]>([])
const loading = ref(true)

const heroMovie = computed(() => charts.value[0]?.movies?.[0])

const chartMoviesMap = computed(() => {
  const map: Record<string, MovieItem[]> = {}
  for (const chart of charts.value) {
    map[chart.genreName] = chart.movies.map(chartItemToMovie)
  }
  return map
})

async function loadYear(year: number) {
  loading.value = true
  try {
    charts.value = await getAnnualByYear(year)
  } catch (e) {
    console.error('Failed to load annual data:', e)
    charts.value = []
  } finally {
    loading.value = false
  }
}

watch(selectedYear, (y) => {
  if (y) {
    loadYear(y)
    router.replace(`/annual/${y}`)
  }
})

onMounted(async () => {
  try {
    years.value = await listAnnualYears()
  } catch (e) {
    console.error('Failed to load annual years:', e)
  }
  const initialYear = Number(route.params.year) || years.value[0] || 2025
  selectedYear.value = initialYear
})
</script>

<style scoped>
.annual-page { min-height: 100vh; }

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

.page-desc { font-size: 1rem; color: var(--text-muted); margin-top: 8px; }

.page-content {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 32px;
}

/* Year Timeline */
.year-timeline {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 4px 0 24px;
}

.year-timeline::-webkit-scrollbar { display: none; }

.year-btn {
  padding: 10px 20px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill);
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out);
}

.year-btn:hover { color: var(--text-primary); background: var(--glass-hover); }
.year-btn.active { color: white; background: var(--accent-red); border-color: var(--accent-red); }

/* Year Hero */
.year-hero {
  border-radius: var(--radius-lg);
  overflow: hidden;
  aspect-ratio: 21 / 9;
  margin-bottom: 48px;
}

.year-hero-link { display: block; width: 100%; height: 100%; position: relative; text-decoration: none; }
.year-hero-poster { width: 100%; height: 100%; }
.year-hero-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to right, rgba(9,9,9,0.9) 0%, rgba(9,9,9,0.3) 60%, transparent 100%);
}
.year-hero-content { position: absolute; bottom: 0; left: 0; padding: 40px; z-index: 2; }
.year-label { font-size: 0.8rem; font-weight: 700; color: var(--accent-red); text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 8px; display: block; }
.year-movie-title { font-family: var(--font-display); font-size: 2rem; font-weight: 900; color: #ffffff; margin-bottom: 12px; text-shadow: 0 2px 12px rgba(0,0,0,0.5); }

.chart-section { margin-bottom: 48px; }
.chart-title { font-family: var(--font-display); font-size: 1.3rem; font-weight: 800; color: var(--text-primary); margin-bottom: 20px; }

@media (max-width: 768px) {
  .page-header { padding: 32px 16px 0; }
  .page-content { padding: 24px 16px; }
  .year-hero { aspect-ratio: 16 / 9; }
  .year-hero-content { padding: 24px; }
  .year-movie-title { font-size: 1.4rem; }
}
</style>
