<template>
  <div class="home-page">
    <!-- Search Results -->
    <div v-if="searchQuery" class="home-section search-results-section">
      <div class="search-results-header">
        <h2 class="search-results-title">搜索：{{ searchQuery }}</h2>
        <p class="search-results-count" v-if="searchDone">找到 {{ searchResults.length }} 部电影</p>
      </div>
      <LoadingSpinner v-if="searchLoading" text="搜索中..." />
      <MovieGrid v-else-if="searchResults.length" :movies="searchResults" :cols="5" />
      <div v-else-if="searchDone" class="search-empty">
        <p>没有找到与「{{ searchQuery }}」相关的电影</p>
      </div>
    </div>

    <!-- Normal Homepage (hidden during search) -->
    <template v-if="!searchQuery">
    <!-- Hero -->
    <MovieHero v-if="heroMovie" :movie="heroMovie" />

    <!-- Top 250 Horizontal -->
    <div v-animate class="home-section">
      <MovieRow title="豆瓣 Top 250" :movies="top250Movies" view-all-to="/top250" show-rank />
    </div>

    <!-- Genre Bento Grid -->
    <div v-animate class="home-section">
      <GenreBentoGrid :genres="genres" to="/charts" />
    </div>

    <!-- Annual Timeline -->
    <div v-animate class="home-section">
      <SectionTitle title="年度榜单" />
      <div class="annual-timeline">
        <button
          v-for="y in annualYears"
          :key="y"
          class="year-btn"
          :class="{ active: selectedYear === y }"
          @click="selectedYear = y"
        >{{ y }}</button>
      </div>
      <div v-if="annualMovies.length" class="annual-hero">
        <router-link :to="`/movies/${annualMovies[0].movieId}`" class="annual-hero-link">
          <PosterImage :src="annualMovies[0].posterUrl" :alt="annualMovies[0].title" ratio="16-9" class="annual-hero-poster" />
          <div class="annual-hero-overlay" />
          <div class="annual-hero-content">
            <span class="annual-hero-year">{{ selectedYear }} 年度热门</span>
            <h2 class="annual-hero-title">{{ annualMovies[0].title }}</h2>
            <RatingBadge v-if="annualMovies[0].rating" :value="annualMovies[0].rating" size="lg" glow />
          </div>
        </router-link>
      </div>
    </div>

    <!-- Discover Sections -->
    <div v-for="section in discoverSections" :key="section.key" v-animate class="home-section">
      <MovieRow :title="section.title" :movies="section.movies" />
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { MovieItem, ChartMovieItem, DiscoverSection, MovieChart } from '../api/movies'
import { getDiscover, getTop250, listAnnualYears, getAnnualByYear, searchMovies } from '../api/movies'
import { chartItemToMovie } from '../utils/movie'
import type { GenreCard } from '../api/movies'
import MovieHero from '../components/movie/MovieHero.vue'
import MovieRow from '../components/movie/MovieRow.vue'
import MovieGrid from '../components/movie/MovieGrid.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import GenreBentoGrid from '../components/movie/GenreBentoGrid.vue'
import SectionTitle from '../components/common/SectionTitle.vue'
import PosterImage from '../components/common/PosterImage.vue'
import RatingBadge from '../components/common/RatingBadge.vue'

const route = useRoute()
const heroMovie = ref<MovieItem | null>(null)
const top250Movies = ref<MovieItem[]>([])
const genres = ref<GenreCard[]>([])
const annualYears = ref<number[]>([])
const selectedYear = ref(0)
const discoverSections = ref<DiscoverSection[]>([])
const annualCharts = ref<MovieChart[]>([])

const annualMovies = ref<ChartMovieItem[]>([])

// Search state
const searchQuery = ref('')
const searchResults = ref<MovieItem[]>([])
const searchLoading = ref(false)
const searchDone = ref(false)

async function doSearch(q: string) {
  if (!q) {
    searchQuery.value = ''
    searchResults.value = []
    searchDone.value = false
    return
  }
  searchQuery.value = q
  searchLoading.value = true
  searchDone.value = false
  try {
    const data = await searchMovies(q, 1, 50)
    searchResults.value = data.records || []
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
    searchDone.value = true
  }
}

let homeLoaded = false

async function loadHomeData() {
  if (homeLoaded) return
  homeLoaded = true
  const [discover, top250, years] = await Promise.allSettled([
    getDiscover(),
    getTop250(),
    listAnnualYears(),
  ])
  if (discover.status === 'fulfilled') {
    const sections = discover.value
    discoverSections.value = sections
    genres.value = buildGenresFromDiscover(sections)
  }
  if (top250.status === 'fulfilled') {
    const all250 = top250.value.movies
    top250Movies.value = all250.slice(0, 20).map(chartItemToMovie)
    // 每日随机一部 Top250 作为今日推荐
    const today = new Date()
    const daySeed = today.getFullYear() * 10000 + (today.getMonth() + 1) * 100 + today.getDate()
    const idx = daySeed % all250.length
    heroMovie.value = chartItemToMovie(all250[idx])
  }
  if (years.status === 'fulfilled') {
    annualYears.value = years.value
    if (years.value.length) selectedYear.value = years.value[0]
  }
}

watch(() => route.query.q, (q) => {
  doSearch(q ? String(q) : '')
  if (!q) loadHomeData()
}, { immediate: true })


const sizes: GenreCard['size'][] = ['large', 'medium', 'medium', 'small', 'small', 'large']

function buildGenresFromDiscover(sections: DiscoverSection[]): GenreCard[] {
  return sections
    .filter(s => s.key.startsWith('genre_'))
    .map((s, i) => ({
      name: s.title.replace(' · 精选', ''),
      posterUrl: s.movies[0]?.posterLocalPath || s.movies[0]?.posterUrl || '',
      movieCount: s.movies.length,
      size: sizes[i % sizes.length],
    }))
}

async function loadAnnual(year: number) {
  try {
    const charts = await getAnnualByYear(year)
    annualCharts.value = charts
    annualMovies.value = charts[0]?.movies || []
  } catch {
    annualCharts.value = []
    annualMovies.value = []
  }
}

watch(selectedYear, (y) => {
  if (y) loadAnnual(y)
})

onMounted(() => {
  if (!route.query.q) loadHomeData()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
}

.home-section {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 32px 32px 0;
}

/* Annual Timeline */
.annual-timeline {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 4px 0 20px;
  -ms-overflow-style: none;
}

.annual-timeline::-webkit-scrollbar { display: none; }

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

.year-btn:hover {
  color: var(--text-primary);
  background: var(--glass-hover);
}

.year-btn.active {
  color: white;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

/* Annual Hero */
.annual-hero {
  margin-top: 16px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  aspect-ratio: 21 / 9;
}

.annual-hero-link {
  display: block;
  width: 100%;
  height: 100%;
  position: relative;
  text-decoration: none;
}

.annual-hero-poster {
  width: 100%;
  height: 100%;
}

.annual-hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0.3) 60%, transparent 100%);
}

.annual-hero-content {
  position: absolute;
  bottom: 0;
  left: 0;
  padding: 40px;
  z-index: 2;
}

.annual-hero-year {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--accent-red);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 8px;
  display: block;
}

.annual-hero-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 900;
  color: #ffffff;
  margin-bottom: 12px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.5);
}

@media (max-width: 768px) {
  .home-section { padding: 24px 16px 0; }
  .annual-hero { aspect-ratio: 16 / 9; }
  .annual-hero-content { padding: 24px; }
  .annual-hero-title { font-size: 1.4rem; }
}

/* Search Results */
.search-results-section {
  padding-top: 48px;
  min-height: 60vh;
}

.search-results-header {
  margin-bottom: 32px;
}

.search-results-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 900;
  color: var(--text-primary);
}

.search-results-count {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-top: 8px;
}

.search-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 30vh;
  font-size: 1.1rem;
  color: var(--text-muted);
}
</style>
