<template>
  <div class="browse-page">
    <header class="hero-shell">
      <div class="hero-topbar">
        <div class="brand-block">
          <span class="brand-mark">M</span>
          <div>
            <div class="brand-title">MovieMe</div>
            <div class="brand-subtitle">Browse, filter and search real movie data</div>
          </div>
        </div>

        <div class="account-actions">
          <template v-if="userStore.isLoggedIn">
            <div class="profile-chip">
              <span class="profile-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <span class="profile-role">{{ userStore.isAdmin ? '管理员' : '已登录' }}</span>
            </div>
            <el-button type="danger" text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="router.push('/login')">登录</el-button>
            <el-button @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>

      <section class="hero-content">
        <div class="hero-copy">
          <span class="hero-kicker">Phase 3</span>
          <h1>电影浏览和搜索已经接上真实接口</h1>
          <p>
            现在首页不再只是登录后的空壳。你可以直接浏览电影列表、按类型和评分筛选，或者用关键词搜索数据库中的电影。
          </p>
        </div>

        <div class="hero-search-card">
          <div class="search-label">快速搜索</div>
          <el-input
            v-model="searchInput"
            size="large"
            placeholder="输入电影名、原名或剧情关键词"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>

          <div class="hero-stats">
            <div class="stat-card accent">
              <span class="stat-label">当前结果</span>
              <strong>{{ total }}</strong>
            </div>
            <div class="stat-card">
              <span class="stat-label">浏览模式</span>
              <strong>{{ isSearchMode ? '搜索结果' : '筛选浏览' }}</strong>
            </div>
          </div>
        </div>
      </section>
    </header>

    <main class="content-shell">
      <section class="toolbar-shell">
        <div class="toolbar-head">
          <div>
            <div class="toolbar-title">{{ headingTitle }}</div>
            <div class="toolbar-subtitle">{{ headingSubtitle }}</div>
          </div>
          <div class="toolbar-actions">
            <el-button v-if="isSearchMode" @click="clearSearch">清空搜索</el-button>
            <el-button :icon="RefreshRight" @click="resetFilters" :disabled="!hasFilters && !isSearchMode">
              重置
            </el-button>
          </div>
        </div>

        <div class="filter-row">
          <el-select
            v-model="filters.genre"
            clearable
            filterable
            placeholder="类型"
            :disabled="isSearchMode"
            @change="handleFilterChange"
          >
            <el-option v-for="genre in genres" :key="genre" :label="genre" :value="genre" />
          </el-select>

          <el-select
            v-model="filters.year"
            clearable
            placeholder="年份"
            :disabled="isSearchMode"
            @change="handleFilterChange"
          >
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}`" :value="year" />
          </el-select>

          <el-select
            v-model="filters.ratingMin"
            clearable
            placeholder="最低评分"
            :disabled="isSearchMode"
            @change="handleFilterChange"
          >
            <el-option v-for="rating in ratingOptions" :key="rating" :label="`${rating} 分以上`" :value="rating" />
          </el-select>
        </div>
      </section>

      <section v-loading="loading" class="grid-shell">
        <template v-if="movies.length">
          <article
            v-for="movie in movies"
            :key="movie.id"
            class="movie-card"
            @click="openMovie(movie.id)"
          >
            <div class="poster-area">
              <el-image v-if="resolvePoster(movie)" :src="resolvePoster(movie)" fit="cover" class="poster-image">
                <template #error>
                  <div class="poster-fallback">{{ movie.title.slice(0, 1) }}</div>
                </template>
              </el-image>
              <div v-else class="poster-fallback">{{ movie.title.slice(0, 1) }}</div>

              <div class="rating-badge">
                <el-icon><Star /></el-icon>
                <span>{{ formatRating(movie.doubanRating) }}</span>
              </div>
            </div>

            <div class="movie-body">
              <div class="movie-header">
                <h3>{{ movie.title }}</h3>
                <span class="movie-year">{{ movie.year || '未知年份' }}</span>
              </div>

              <p v-if="movie.titleEn && movie.titleEn !== movie.title" class="original-name">
                {{ movie.titleEn }}
              </p>

              <div class="meta-line">
                <span v-if="movie.durationText">{{ movie.durationText }}</span>
                <span>· {{ movie.doubanVotes || 0 }} 人评分</span>
              </div>

              <div class="genre-row">
                <span v-for="genre in movie.genres.slice(0, 3)" :key="genre" class="genre-pill">{{ genre }}</span>
              </div>

              <p class="summary-text">{{ movie.summary || '暂无简介' }}</p>

              <div class="card-footer">
                <span>查看详情</span>
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </article>
        </template>

        <el-empty v-else :description="emptyDescription">
          <template #image>
            <div class="empty-illustration">
              <el-icon><Film /></el-icon>
            </div>
          </template>
          <el-button v-if="isSearchMode" type="primary" @click="clearSearch">返回浏览模式</el-button>
          <el-button v-else-if="hasFilters" type="primary" @click="resetFilters">清空筛选</el-button>
        </el-empty>
      </section>

      <div v-if="total > 0" class="pagination-shell">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Film, RefreshRight, Search, Star } from '@element-plus/icons-vue'
import { listMovieGenres, listMovies, resolvePoster, searchMovies, type MovieItem } from '../api/movies'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const genres = ref<string[]>([])
const movies = ref<MovieItem[]>([])
const total = ref(0)
const loading = ref(false)
const searchInput = ref('')

const filters = reactive({
  genre: '',
  year: undefined as number | undefined,
  ratingMin: undefined as number | undefined,
})

const pagination = reactive({
  page: 1,
  size: 12,
})

const keyword = ref('')

const ratingOptions = [9, 8, 7, 6]
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 70 }, (_, index) => currentYear - index)

const isSearchMode = computed(() => keyword.value.trim().length > 0)
const hasFilters = computed(() => Boolean(filters.genre || filters.year || filters.ratingMin))
const headingTitle = computed(() => (isSearchMode.value ? `搜索“${keyword.value}”` : '电影列表'))
const headingSubtitle = computed(() => {
  if (isSearchMode.value) {
    return `共找到 ${total.value} 部相关电影`
  }
  if (hasFilters.value) {
    return `筛选后共有 ${total.value} 部电影`
  }
  return `数据库中当前可浏览 ${total.value} 部电影`
})
const emptyDescription = computed(() => {
  if (isSearchMode.value) {
    return `没有找到和“${keyword.value}”相关的电影`
  }
  if (hasFilters.value) {
    return '当前筛选条件下没有电影'
  }
  return '数据库里还没有电影数据，请先触发爬虫导入。'
})

type QueryPatch = {
  q?: string | null
  genre?: string | null
  year?: number | null
  ratingMin?: number | null
  page?: number | null
  size?: number | null
}

function parseInteger(value: unknown, fallback: number) {
  const parsed = Number(value)
  if (Number.isFinite(parsed) && parsed > 0) {
    return Math.floor(parsed)
  }
  return fallback
}

function parseOptionalInteger(value: unknown) {
  const parsed = Number(value)
  if (Number.isFinite(parsed) && parsed > 0) {
    return Math.floor(parsed)
  }
  return undefined
}

function formatRating(value?: number) {
  if (value === undefined || value === null || value === 0) return '暂无'
  return value.toFixed(1)
}

function syncStateFromRoute() {
  keyword.value = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  searchInput.value = keyword.value
  filters.genre = typeof route.query.genre === 'string' ? route.query.genre : ''
  filters.year = parseOptionalInteger(route.query.year)
  filters.ratingMin = parseOptionalInteger(route.query.ratingMin)
  pagination.page = parseInteger(route.query.page, 1)
  pagination.size = parseInteger(route.query.size, 12)
}

async function fetchMovies() {
  loading.value = true

  try {
    if (isSearchMode.value) {
      const data = await searchMovies(keyword.value, pagination.page, pagination.size)
      movies.value = data.records
      total.value = data.total
      return
    }

    const data = await listMovies({
      page: pagination.page,
      size: pagination.size,
      genre: filters.genre || undefined,
      year: filters.year,
      ratingMin: filters.ratingMin,
    })
    movies.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function updateQuery(patch: QueryPatch) {
  const next = {
    q: keyword.value || null,
    genre: filters.genre || null,
    year: filters.year ?? null,
    ratingMin: filters.ratingMin ?? null,
    page: pagination.page > 1 ? pagination.page : null,
    size: pagination.size !== 12 ? pagination.size : null,
  }

  if ('q' in patch) next.q = patch.q ?? null
  if ('genre' in patch) next.genre = patch.genre ?? null
  if ('year' in patch) next.year = patch.year ?? null
  if ('ratingMin' in patch) next.ratingMin = patch.ratingMin ?? null
  if ('page' in patch) next.page = patch.page ?? null
  if ('size' in patch) next.size = patch.size ?? null

  const query: Record<string, string> = {}
  if (next.q) {
    query.q = next.q
    if (next.page && next.page > 1) query.page = `${next.page}`
    if (next.size && next.size !== 12) query.size = `${next.size}`
  } else {
    if (next.genre) query.genre = next.genre
    if (next.year) query.year = `${next.year}`
    if (next.ratingMin) query.ratingMin = `${next.ratingMin}`
    if (next.page && next.page > 1) query.page = `${next.page}`
    if (next.size && next.size !== 12) query.size = `${next.size}`
  }

  router.push({
    path: '/',
    query,
  })
}

function handleSearch() {
  const value = searchInput.value.trim()
  updateQuery({
    q: value || null,
    genre: null,
    year: null,
    ratingMin: null,
    page: null,
  })
}

function handleFilterChange() {
  updateQuery({
    q: null,
    genre: filters.genre || null,
    year: filters.year ?? null,
    ratingMin: filters.ratingMin ?? null,
    page: null,
  })
}

function handlePageChange(page: number) {
  updateQuery({
    page: page > 1 ? page : null,
  })
}

function clearSearch() {
  searchInput.value = ''
  updateQuery({
    q: null,
    page: null,
  })
}

function resetFilters() {
  searchInput.value = ''
  updateQuery({
    q: null,
    genre: null,
    year: null,
    ratingMin: null,
    page: null,
  })
}

function openMovie(id: number) {
  router.push(`/movies/${id}`)
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

watch(
  () => route.query,
  () => {
    syncStateFromRoute()
    void fetchMovies()
  },
  { immediate: true },
)

onMounted(async () => {
  try {
    genres.value = await listMovieGenres()
  } catch {
    genres.value = []
  }

  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // Ignore expired token here and let the request interceptor handle future failures.
    }
  }
})
</script>

<style scoped>
.browse-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.18), transparent 24%),
    linear-gradient(180deg, #0f172a 0%, #111827 36%, #f8fafc 36%, #f8fafc 100%);
}

.hero-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 24px 34px;
  color: #f8fafc;
}

.hero-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #111827;
  font-size: 22px;
  font-weight: 800;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
}

.brand-subtitle,
.profile-role {
  color: rgba(248, 250, 252, 0.72);
  font-size: 13px;
}

.account-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.profile-name {
  font-size: 14px;
  font-weight: 600;
}

.hero-content {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr;
  gap: 28px;
  margin-top: 44px;
  align-items: end;
}

.hero-kicker {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.12);
  color: #fde68a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 18px 0 14px;
  font-size: 52px;
  line-height: 1.05;
}

.hero-copy p {
  max-width: 720px;
  margin: 0;
  color: rgba(248, 250, 252, 0.74);
  font-size: 17px;
  line-height: 1.75;
}

.hero-search-card {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(18px);
}

.search-label {
  margin-bottom: 12px;
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.stat-card {
  padding: 16px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.38);
}

.stat-card.accent {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.92), rgba(245, 158, 11, 0.88));
  color: #111827;
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stat-card strong {
  font-size: 24px;
}

.content-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.toolbar-shell {
  margin-top: -6px;
  padding: 24px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.toolbar-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.toolbar-title {
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.toolbar-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 14px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 220px));
  gap: 14px;
  margin-top: 18px;
}

.grid-shell {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  min-height: 320px;
  margin-top: 24px;
}

.movie-card {
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.movie-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 55px rgba(15, 23, 42, 0.14);
}

.poster-area {
  position: relative;
  height: 340px;
  background: #e2e8f0;
}

.poster-image,
.poster-fallback {
  width: 100%;
  height: 100%;
}

.poster-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a, #1d4ed8);
  color: #f8fafc;
  font-size: 68px;
  font-weight: 700;
}

.rating-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #f8fafc;
  font-size: 13px;
  font-weight: 700;
}

.movie-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.movie-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.movie-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
}

.movie-year,
.original-name,
.meta-line {
  color: #64748b;
  font-size: 13px;
}

.original-name,
.meta-line {
  margin: 0;
}

.genre-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.genre-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.summary-text {
  margin: 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.75;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
  color: #f97316;
  font-size: 13px;
  font-weight: 700;
}

.empty-illustration {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 110px;
  height: 110px;
  border-radius: 28px;
  background: linear-gradient(135deg, #dbeafe, #fde68a);
  color: #1e293b;
  font-size: 44px;
}

.pagination-shell {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 1180px) {
  .grid-shell {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-topbar,
  .toolbar-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-row,
  .grid-shell {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-shell,
  .content-shell {
    padding-left: 16px;
    padding-right: 16px;
  }

  .hero-copy h1 {
    font-size: 38px;
  }

  .toolbar-shell {
    padding: 20px;
  }

  .filter-row,
  .grid-shell,
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .poster-area {
    height: 300px;
  }
}
</style>
