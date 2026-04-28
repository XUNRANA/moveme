<template>
  <div class="detail-page">
    <header class="detail-header">
      <div class="header-brand" @click="router.push('/')">
        <span class="brand-mark">M</span>
        <div>
          <div class="brand-title">MovieMe</div>
          <div class="brand-subtitle">Movie detail</div>
        </div>
      </div>

      <div class="header-actions">
        <el-button text @click="router.push('/')">返回电影列表</el-button>
        <template v-if="userStore.isLoggedIn">
          <span class="welcome-text">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>

    <main class="detail-main">
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="detail-skeleton">
            <div class="poster-skeleton" />
            <div class="content-skeleton">
              <el-skeleton-item variant="h1" style="width: 50%; height: 32px" />
              <el-skeleton-item variant="text" style="width: 70%" />
              <el-skeleton-item variant="text" style="width: 90%" />
              <el-skeleton-item variant="text" style="width: 80%" />
            </div>
          </div>
        </template>

        <template #default>
          <el-empty v-if="loadFailed || !movie" description="电影不存在，或者数据还没导入">
            <el-button type="primary" @click="router.push('/')">回到电影列表</el-button>
          </el-empty>

          <template v-else>
            <section class="hero-card">
              <div class="poster-wrap">
                <el-image v-if="resolvePoster(movie)" :src="resolvePoster(movie)" fit="cover" class="poster-image">
                  <template #error>
                    <div class="poster-fallback">{{ movie.title.slice(0, 1) }}</div>
                  </template>
                </el-image>
                <div v-else class="poster-fallback">{{ movie.title.slice(0, 1) }}</div>
              </div>

              <div class="hero-content">
                <div class="eyebrow">
                  <span>{{ movie.year || '未知年份' }}</span>
                  <span v-if="movie.countries?.length">· {{ movie.countries.join(' / ') }}</span>
                  <span v-if="movie.durationText">· {{ movie.durationText }}</span>
                </div>

                <h1>{{ movie.title }}</h1>
                <p v-if="movie.titleEn && movie.titleEn !== movie.title" class="original-title">
                  {{ movie.titleEn }}
                </p>

                <div class="rating-row">
                  <div class="rating-chip primary">
                    <span class="chip-label">豆瓣</span>
                    <strong>{{ formatRating(movie.doubanRating) }}</strong>
                    <span>{{ movie.doubanVotes || 0 }} 人评分</span>
                  </div>
                  <div class="rating-chip">
                    <span class="chip-label">站内</span>
                    <strong>{{ formatRating(movie.localRating) }}</strong>
                    <span>{{ movie.localVotes || 0 }} 条记录</span>
                  </div>
                </div>

                <div class="tag-row">
                  <span v-for="genre in movie.genres" :key="genre" class="genre-tag">{{ genre }}</span>
                </div>

                <div class="meta-grid">
                  <div class="meta-card">
                    <span class="meta-label">导演</span>
                    <strong>{{ joinPersons(movie.directors) }}</strong>
                  </div>
                  <div class="meta-card">
                    <span class="meta-label">主演</span>
                    <strong>{{ joinPersons(movie.actors) }}</strong>
                  </div>
                  <div class="meta-card" v-if="movie.writers?.length">
                    <span class="meta-label">编剧</span>
                    <strong>{{ joinPersons(movie.writers) }}</strong>
                  </div>
                  <div class="meta-card">
                    <span class="meta-label">语言</span>
                    <strong>{{ movie.languages?.length ? movie.languages.join(' / ') : '暂无数据' }}</strong>
                  </div>
                  <div class="meta-card">
                    <span class="meta-label">上映日期</span>
                    <strong>{{ movie.releaseDate || '暂无数据' }}</strong>
                  </div>
                  <div class="meta-card">
                    <span class="meta-label">IMDb</span>
                    <strong>{{ movie.imdbId || '暂无数据' }}</strong>
                  </div>
                </div>
              </div>
            </section>

            <section class="summary-card">
              <div class="section-title">剧情简介</div>
              <p>{{ movie.summary || '这部电影还没有简介。' }}</p>
            </section>
          </template>
        </template>
      </el-skeleton>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMovieDetail, resolvePoster, type MovieDetail, type PersonBrief } from '../api/movies'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const movie = ref<MovieDetail | null>(null)
const loading = ref(false)
const loadFailed = ref(false)

async function loadMovie(id: number) {
  loading.value = true
  loadFailed.value = false

  try {
    movie.value = await getMovieDetail(id)
  } catch {
    movie.value = null
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function formatRating(value?: number) {
  if (value === undefined || value === null || value === 0) return '暂无'
  return value.toFixed(1)
}

function joinPersons(items?: PersonBrief[]) {
  if (!items?.length) return '暂无数据'
  return items.map((p) => p.name).join(' / ')
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

watch(
  () => route.params.id,
  (id) => {
    const movieId = Number(id)
    if (Number.isFinite(movieId) && movieId > 0) {
      void loadMovie(movieId)
    } else {
      movie.value = null
      loadFailed.value = true
    }
  },
  { immediate: true },
)

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // Ignore expired token here and let request interceptor handle future failures.
    }
  }
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(249, 192, 106, 0.16), transparent 30%),
    linear-gradient(180deg, #0f172a 0%, #111827 42%, #f4efe6 42%, #f4efe6 100%);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 24px 40px;
  color: #f8fafc;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #111827;
  font-size: 20px;
  font-weight: 800;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
}

.brand-subtitle,
.welcome-text {
  color: rgba(248, 250, 252, 0.72);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.detail-skeleton {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 28px;
  margin-top: 24px;
}

.poster-skeleton {
  height: 460px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.16);
}

.content-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 20px;
}

.hero-card {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 32px;
  margin-top: 12px;
  padding: 28px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.poster-wrap {
  min-height: 460px;
}

.poster-image,
.poster-fallback {
  width: 100%;
  height: 100%;
  min-height: 460px;
  border-radius: 24px;
}

.poster-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  color: #f8fafc;
  font-size: 88px;
  font-weight: 700;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.eyebrow {
  color: #92400e;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.hero-content h1 {
  margin: 0;
  color: #111827;
  font-size: 42px;
  line-height: 1.05;
}

.original-title {
  margin: -6px 0 0;
  color: #64748b;
  font-size: 18px;
}

.rating-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.rating-chip {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #eef2ff;
  color: #312e81;
}

.rating-chip.primary {
  background: #fef3c7;
  color: #92400e;
}

.chip-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.rating-chip strong {
  font-size: 26px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.genre-tag {
  padding: 8px 12px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.meta-label {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.meta-card strong {
  color: #0f172a;
  line-height: 1.6;
}

.summary-card {
  margin-top: 24px;
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.section-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.summary-card p {
  margin: 0;
  color: #334155;
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-line;
}

@media (max-width: 960px) {
  .detail-header,
  .header-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-card,
  .detail-skeleton {
    grid-template-columns: 1fr;
  }

  .poster-wrap {
    max-width: 380px;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-header {
    padding: 20px 20px 12px;
  }

  .detail-main {
    padding: 0 16px 32px;
  }

  .hero-card,
  .summary-card {
    padding: 20px;
    border-radius: 24px;
  }

  .hero-content h1 {
    font-size: 32px;
  }

  .poster-image,
  .poster-fallback,
  .poster-wrap {
    min-height: 360px;
  }
}
</style>
