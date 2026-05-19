<template>
  <div class="movie-detail" v-if="movie">
    <!-- Immersive Header -->
    <div class="detail-hero">
      <div class="hero-bg">
        <img v-if="posterSrc" :src="posterSrc" alt="" class="hero-bg-img" />
        <div class="hero-gradient" />
      </div>
      <div class="hero-content">
        <div class="hero-poster">
          <PosterImage :src="posterSrc" :alt="movie.title" />
          <RankBadge v-if="movie.top250?.rank" :rank="movie.top250.rank" class="poster-rank" />
        </div>
        <div class="hero-info">
          <h1 class="detail-title">{{ movie.title }}</h1>
          <p v-if="movie.titleEn" class="detail-title-en">{{ movie.titleEn }}</p>

          <div class="detail-meta">
            <RatingBadge v-if="movie.doubanRating" :value="movie.doubanRating" size="lg" glow />
            <span v-if="movie.doubanVotes" class="votes-text">{{ (movie.doubanVotes / 10000).toFixed(1) }}万人评分</span>
            <span v-if="movie.year" class="meta-dot">{{ movie.year }}</span>
            <span v-if="movie.countries?.length" class="meta-dot">{{ movie.countries.join(' / ') }}</span>
            <span v-if="movie.durationText" class="meta-dot">{{ movie.durationText }}</span>
            <span v-if="movie.wishCount" class="meta-dot">{{ movie.wishCount }}人想看</span>
            <span v-if="movie.collectCount" class="meta-dot">{{ movie.collectCount }}人看过</span>
          </div>

          <!-- Genres -->
          <div v-if="movie.genres?.length" class="detail-genres">
            <span v-for="g in movie.genres" :key="g" class="genre-tag">{{ g }}</span>
          </div>

          <!-- Want to Watch / Watched Buttons -->
          <div v-if="userStore.isLoggedIn" class="action-bar">
            <button
              class="action-btn"
              :class="{ active: favStatus === 0 }"
              @click="toggleFavorite(0)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
              </svg>
              {{ favStatus === 0 ? '已想看' : '想看' }}
            </button>
            <button
              class="action-btn"
              :class="{ active: favStatus === 1 }"
              @click="toggleFavorite(1)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              {{ favStatus === 1 ? '已看过' : '看过' }}
            </button>
            <div v-if="userRating" class="user-rating-badge" @click="showRatingModal = true">
              <span class="rating-stars-mini">
                <span v-for="i in 5" :key="i" class="star-mini" :class="{ filled: i <= Math.round(userRating.score / 2), half: userRating.score / 2 >= i - 0.5 && userRating.score / 2 < i }">★</span>
              </span>
              <span class="rating-score-text">{{ (userRating.score / 2).toFixed(1) }}</span>
            </div>
          </div>

          <!-- Summary + Play Links side by side -->
          <div class="summary-play-row">
            <p v-if="movie.summary" class="detail-summary">{{ movie.summary }}</p>
            <div v-if="movie.playLinks?.length" class="play-links-sidebar">
              <a
                v-for="(link, i) in sortedPlayLinks"
                :key="i"
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
                class="play-link-chip"
              >{{ link.platform }}</a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Sections -->
    <div class="detail-body">
      <!-- Three columns: Rating+GenreRanks | ReleaseDates | Akas -->
      <div v-if="movie.ratingDist?.length || movie.genreRanks?.length || movie.releaseDates?.length || movie.akas?.length" class="info-three-col" v-animate>
        <!-- Col 1: Rating Distribution + Genre Ranks inline -->
        <div class="info-col">
          <section v-if="movie.ratingDist?.length || movie.genreRanks?.length" class="detail-section">
            <h2 class="section-heading">评分分布</h2>
            <div class="rating-dist-row">
              <div class="rating-bars">
                <div v-for="r in movie.ratingDist" :key="r.star" class="rating-bar-row">
                  <span class="bar-label">{{ r.label }}</span>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: `${r.percentage || 0}%` }" />
                  </div>
                  <span class="bar-pct">{{ (r.percentage || 0).toFixed(1) }}%</span>
                </div>
              </div>
              <div v-if="movie.genreRanks?.length" class="rank-inline">
                <span v-for="r in movie.genreRanks" :key="r.genre" class="rank-inline-item">
                  超越 {{ (r.percentile || 0).toFixed(0) }}% 的{{ r.genre }}电影
                </span>
              </div>
            </div>
          </section>
        </div>

        <!-- Col 2: Release Dates -->
        <div class="info-col">
          <section v-if="movie.releaseDates?.length" class="detail-section">
            <h2 class="section-heading">上映日期</h2>
            <div class="release-list">
              <div v-for="(r, i) in movie.releaseDates" :key="i" class="release-item">
                <span class="release-region">{{ r.region }}</span>
                <span class="release-date">{{ r.date || r.rawText }}</span>
              </div>
            </div>
          </section>
        </div>

        <!-- Col 3: Akas -->
        <div class="info-col">
          <section v-if="movie.akas?.length" class="detail-section">
            <h2 class="section-heading">别名</h2>
            <div class="akas-list" :class="{ 'akas-two-col': movie.akas.length > 8 }">
              <span v-for="(a, i) in movie.akas" :key="i" class="aka-item">{{ a }}</span>
            </div>
          </section>
        </div>
      </div>

      <!-- Directors / Writers / Cast / Awards / Comments -->
      <template v-if="movie.directors?.length || movie.writers?.length || movie.actors?.length || movie.awards?.length">

      <section v-if="movie.directors?.length" v-animate class="detail-section">
        <h2 class="section-heading">导演</h2>
        <div class="crew-row">
          <router-link v-for="d in movie.directors.slice(0, 11)" :key="d.id" :to="`/persons/${d.id}`" class="crew-person">
            <img v-if="d.avatarLocalPath || d.avatarUrl" :src="d.avatarLocalPath || d.avatarUrl" alt="" class="crew-img" />
            <div v-else class="crew-img-placeholder">{{ d.name?.[0] }}</div>
            <span class="crew-name">{{ d.name }}</span>
          </router-link>
        </div>
      </section>

      <section v-if="movie.writers?.length" v-animate class="detail-section">
        <h2 class="section-heading">编剧</h2>
        <div class="crew-row">
          <router-link v-for="w in movie.writers.slice(0, 11)" :key="w.id" :to="`/persons/${w.id}`" class="crew-person">
            <img v-if="w.avatarLocalPath || w.avatarUrl" :src="w.avatarLocalPath || w.avatarUrl" alt="" class="crew-img" />
            <div v-else class="crew-img-placeholder">{{ w.name?.[0] }}</div>
            <span class="crew-name">{{ w.name }}</span>
          </router-link>
        </div>
      </section>

      <section v-if="movie.actors?.length" v-animate class="detail-section">
        <h2 class="section-heading">演职人员</h2>
        <div class="cast-scroll">
          <router-link v-for="p in movie.actors.slice(0, 11)" :key="p.id" :to="`/persons/${p.id}`" class="cast-card">
            <div class="cast-avatar">
              <img v-if="p.avatarLocalPath || p.avatarUrl" :src="p.avatarLocalPath || p.avatarUrl" alt="" />
              <div v-else class="avatar-placeholder">{{ p.name?.[0] }}</div>
            </div>
            <span class="cast-name">{{ p.name }}</span>
            <span v-if="p.roleName" class="cast-role">{{ p.roleName }}</span>
          </router-link>
        </div>
      </section>

      <section v-if="movie.awards?.length" v-animate class="detail-section">
        <h2 class="section-heading">获奖</h2>
        <div class="awards-list">
          <div v-for="(a, i) in movie.awards" :key="i" class="award-item">
            <span class="award-ceremony">{{ a.ceremony }}</span>
            <span class="award-category">{{ a.category }}</span>
            <span class="award-status" :class="{ won: a.status === 'won', nominated: a.status === 'nominated' }">{{ awardStatusLabel(a.status) }}</span>
          </div>
        </div>
      </section>

      <section ref="commentSectionRef" v-animate class="detail-section">
        <div class="comments-header">
          <h2 class="section-heading">评论</h2>
          <button v-if="userStore.isLoggedIn" class="write-comment-btn" @click="openWriteComment">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
            写评论
          </button>
        </div>

        <div class="comments-sort-bar">
          <button class="sort-btn" :class="{ active: commentSort === 'hot' }" @click="toggleSort('hot')">最热</button>
          <button class="sort-btn" :class="{ active: commentSort === 'new' }" @click="toggleSort('new')">最新</button>
        </div>

        <LoadingSpinner v-if="commentLoading" text="加载评论..." />

        <template v-else-if="comments.length">
          <div class="comments-list">
            <div v-for="c in comments" :key="c.id" class="comment-card">
              <div class="comment-header">
                <span class="comment-author">{{ c.authorName }}</span>
                <RatingBadge v-if="c.rating" :value="c.rating * 2" size="sm" />
                <span class="comment-date">{{ c.postedAt }}</span>
              </div>
              <p class="comment-text">{{ c.content }}</p>
              <div class="comment-footer">
                <button
                  class="comment-like-btn"
                  :class="{ liked: c.liked }"
                  @click="toggleLike(c)"
                >
                  <svg viewBox="0 0 24 24" :fill="c.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" width="16" height="16">
                    <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
                  </svg>
                  {{ c.votes || 0 }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="commentTotalPages > 1" class="comments-pagination">
            <button class="page-btn" :disabled="commentPage <= 1" @click="loadComments(commentPage - 1)">上一页</button>
            <template v-for="p in visibleCommentPages" :key="p">
              <span v-if="p === '...'" class="page-dots">...</span>
              <button v-else class="page-btn" :class="{ active: p === commentPage }" @click="loadComments(p as number)">{{ p }}</button>
            </template>
            <button class="page-btn" :disabled="commentPage >= commentTotalPages" @click="loadComments(commentPage + 1)">下一页</button>
          </div>
        </template>

        <div v-else class="comments-empty">暂无评论</div>
      </section>

      </template>

      <section v-if="movie.relatedMovies?.length" class="detail-section">
        <h2 class="section-heading">相关电影</h2>
        <div class="related-scroll">
          <router-link v-for="r in movie.relatedMovies" :key="r.movieId" :to="`/movies/${r.movieId}`" class="related-card">
            <PosterImage :src="r.coverUrl" :alt="r.title" />
            <span class="related-title">{{ r.title }}</span>
            <RatingBadge v-if="r.rating" :value="r.rating" size="sm" />
          </router-link>
        </div>
      </section>
    </div>

    <!-- Rating Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showRatingModal" class="modal-overlay" @click.self="showRatingModal = false">
          <div class="modal-card">
            <h3 class="modal-title">为「{{ movie.title }}」评分</h3>
            <div class="star-picker">
              <div class="star-row">
                <span
                  v-for="i in 5"
                  :key="i"
                  class="star-pick"
                  :class="{
                    filled: tempRating >= i,
                    half: tempRating >= i - 0.5 && tempRating < i,
                  }"
                  @mousemove="handleStarHover($event, i)"
                  @mouseleave="tempRating = committedRating"
                  @click="handleStarClick($event, i)"
                >★</span>
              </div>
              <p class="rating-display">{{ tempRating > 0 ? tempRating.toFixed(1) + ' 分' : '点击星星评分' }}</p>
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" @click="skipRating">跳过</button>
              <button class="btn-save" :disabled="tempRating === 0" @click="confirmRating">确认评分</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Comment Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showCommentModal" class="modal-overlay" @click.self="showCommentModal = false">
          <div class="modal-card">
            <h3 class="modal-title">写评论</h3>
            <div class="comment-modal-rating">
              <span class="comment-rating-label">评分（可选）</span>
              <div class="star-row">
                <span
                  v-for="i in 5"
                  :key="i"
                  class="star-pick"
                  :class="{ filled: writeCommentRating >= i }"
                  @click="writeCommentRating = writeCommentRating === i ? 0 : i"
                >★</span>
              </div>
            </div>
            <div class="form-group">
              <textarea v-model="tempComment" placeholder="写下你对这部电影的看法..." rows="5" />
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" @click="showCommentModal = false">取消</button>
              <button class="btn-save" :disabled="!tempComment.trim()" @click="confirmComment">发布</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>

  <div v-else class="detail-loading">
    <LoadingSpinner text="加载中..." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { MovieDetail as MovieDetailType, CommentVO } from '../api/movies'
import {
  getMovieDetail, resolvePoster,
  checkFavorite, addFavorite, removeFavorite,
  checkRating, saveRating,
  recordView,
  getMovieComments, submitMovieComment, likeComment, unlikeComment,
} from '../api/movies'
import { useUserStore } from '../stores/user'
import PosterImage from '../components/common/PosterImage.vue'
import RatingBadge from '../components/common/RatingBadge.vue'
import RankBadge from '../components/common/RankBadge.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'

const route = useRoute()
const userStore = useUserStore()
const movie = ref<MovieDetailType | null>(null)

// ─── Favorite state ───
const favStatus = ref<number | null>(null) // null=none, 0=wish, 1=watched
const favId = ref<number | null>(null)

// ─── Rating state ───
const userRating = ref<{ id: number; score: number; comment: string } | null>(null)
const showRatingModal = ref(false)
const tempRating = ref(0)
const committedRating = ref(0)
const showCommentModal = ref(false)
const tempComment = ref('')
const pendingScore = ref(0)

// ─── Comments state ───
const comments = ref<CommentVO[]>([])
const commentPage = ref(1)
const commentTotalPages = ref(0)
const commentSort = ref('hot')
const commentLoading = ref(false)
const commentSectionRef = ref<HTMLElement | null>(null)
const writeCommentRating = ref(0)

const posterSrc = computed(() => movie.value ? resolvePoster(movie.value) : undefined)

const PLATFORM_ORDER = ['爱奇艺', '优酷', '腾讯视频', '芒果TV', '哔哩哔哩']

const sortedPlayLinks = computed(() => {
  if (!movie.value?.playLinks) return []
  return [...movie.value.playLinks].sort((a, b) => {
    const ai = PLATFORM_ORDER.findIndex(p => (a.platform ?? '').includes(p))
    const bi = PLATFORM_ORDER.findIndex(p => (b.platform ?? '').includes(p))
    const ar = ai === -1 ? PLATFORM_ORDER.length : ai
    const br = bi === -1 ? PLATFORM_ORDER.length : bi
    return ar - br
  })
})

function awardStatusLabel(status?: string) {
  if (status === 'won') return '获奖'
  if (status === 'nominated') return '提名'
  return '入围'
}

// ─── Comments logic ───
const visibleCommentPages = computed(() => {
  const total = commentTotalPages.value
  const current = commentPage.value
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

async function loadComments(page: number) {
  const movieId = movie.value?.id
  if (!movieId) return
  commentLoading.value = true
  try {
    const res = await getMovieComments(movieId, page, 10, commentSort.value)
    comments.value = res.records || []
    commentTotalPages.value = res.pages || 0
    commentPage.value = res.current || 1
  } catch {
    comments.value = []
    commentTotalPages.value = 0
  } finally {
    commentLoading.value = false
  }
}

function toggleSort(sort: string) {
  commentSort.value = sort
  loadComments(1)
}

async function toggleLike(c: CommentVO) {
  if (!c.id) return
  try {
    if (c.liked) {
      await unlikeComment(c.id)
      c.liked = false
      c.votes = Math.max((c.votes || 1) - 1, 0)
    } else {
      await likeComment(c.id)
      c.liked = true
      c.votes = (c.votes || 0) + 1
    }
  } catch {}
}

function openWriteComment() {
  tempComment.value = ''
  writeCommentRating.value = 0
  showCommentModal.value = true
}

// ─── Favorite logic ───
async function toggleFavorite(status: number) {
  const movieId = movie.value?.id
  if (!movieId) return

  if (favStatus.value === status) {
    // Already in this state → remove
    await removeFavorite(movieId)
    favStatus.value = null
    favId.value = null
  } else {
    await addFavorite(movieId, status)
    favStatus.value = status
    // If switching to watched, show rating modal
    if (status === 1) {
      if (!userRating.value) {
        showRatingModal.value = true
      }
    }
  }
}

// ─── Rating logic ───
function handleStarHover(e: MouseEvent, starIndex: number) {
  const rect = (e.target as HTMLElement).getBoundingClientRect()
  const isLeft = e.clientX - rect.left < rect.width / 2
  tempRating.value = isLeft ? starIndex - 0.5 : starIndex
}

function handleStarClick(e: MouseEvent, starIndex: number) {
  const rect = (e.target as HTMLElement).getBoundingClientRect()
  const isLeft = e.clientX - rect.left < rect.width / 2
  committedRating.value = isLeft ? starIndex - 0.5 : starIndex
  tempRating.value = committedRating.value
}

async function skipRating() {
  showRatingModal.value = false
  await doSaveRating()
}

async function confirmRating() {
  if (tempRating.value === 0) return
  pendingScore.value = Math.round(tempRating.value * 2) // 0-5 → 1-10
  showRatingModal.value = false
  await doSaveRating()
  writeCommentRating.value = pendingScore.value
  tempComment.value = ''
  showCommentModal.value = true
}

async function doSaveRating() {
  const movieId = movie.value?.id
  if (!movieId || pendingScore.value === 0) return
  await saveRating(movieId, pendingScore.value)
  userRating.value = { id: 0, score: pendingScore.value, comment: '' }
}

async function confirmComment() {
  showCommentModal.value = false
  const movieId = movie.value?.id
  if (!movieId || !tempComment.value.trim()) return
  try {
    await submitMovieComment(movieId, tempComment.value.trim(), writeCommentRating.value || undefined)
    loadComments(1)
  } catch {}
}

// ─── Load ───
async function loadMovie(id: number) {
  try {
    movie.value = await getMovieDetail(id)
  } catch {
    movie.value = null
    return
  }

  // Load comments separately
  loadComments(1)

  if (!userStore.isLoggedIn) return

  // Record view
  recordView(id).catch(() => {})

  // Check favorite status
  checkFavorite(id).then(fav => {
    if (fav) {
      favStatus.value = fav.status
      favId.value = fav.id
    } else {
      favStatus.value = null
      favId.value = null
    }
  }).catch(() => {})

  // Check rating status
  checkRating(id).then(r => {
    if (r) {
      userRating.value = { id: r.id, score: r.score, comment: r.comment }
    } else {
      userRating.value = null
    }
  }).catch(() => {})
}

onMounted(() => loadMovie(Number(route.params.id)))
watch(() => route.params.id, (id) => { if (id) loadMovie(Number(id)) })
</script>

<style scoped>
.movie-detail { min-height: 100vh; }

/* Hero */
.detail-hero {
  position: relative;
  min-height: 500px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.hero-bg { position: absolute; inset: 0; }

.hero-bg-img {
  width: 100%; height: 100%;
  object-fit: cover;
  filter: blur(30px) brightness(0.4) saturate(1.2);
  transform: scale(1.2);
}

.hero-gradient {
  position: absolute; inset: 0;
  background:
    linear-gradient(to top, var(--bg-primary) 0%, var(--bg-primary) 10%, transparent 75%),
    linear-gradient(to right, rgba(0,0,0,0.7) 0%, transparent 50%);
}

.hero-content {
  position: relative; z-index: 2;
  max-width: var(--content-max);
  margin: 0 auto; padding: 80px 48px 48px; width: 100%;
  display: flex; gap: 40px; align-items: flex-start;
}

.hero-poster {
  flex-shrink: 0; width: 240px;
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.poster-rank { position: absolute; top: 12px; left: 12px; }

.hero-info { flex: 1; min-width: 0; padding-bottom: 8px; }

.detail-title {
  font-family: var(--font-display);
  font-size: 3rem; font-weight: 900;
  color: #ffffff;
  line-height: 1.2; letter-spacing: -0.02em;
  text-shadow: 0 2px 12px rgba(0,0,0,0.5);
}

.detail-title-en {
  font-size: 1.1rem; color: rgba(255,255,255,0.6); margin-top: 6px;
}

.detail-meta {
  display: flex; align-items: center; gap: 12px;
  margin-top: 20px; flex-wrap: wrap;
}

.meta-dot { font-size: 0.9rem; color: rgba(255,255,255,0.8); }
.meta-dot::before { content: '·'; margin-right: 12px; color: rgba(255,255,255,0.5); }
.votes-text { font-size: 0.85rem; color: rgba(255,255,255,0.6); }

.detail-genres { display: flex; gap: 8px; margin-top: 16px; flex-wrap: wrap; }

.genre-tag {
  padding: 6px 16px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: var(--radius-pill);
  font-size: 0.8rem; color: rgba(255,255,255,0.9);
}

/* Action Bar */
.action-bar {
  display: flex; align-items: center; gap: 12px;
  margin-top: 20px; flex-wrap: wrap;
}

.action-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 24px;
  font-size: 0.85rem; font-weight: 600;
  color: rgba(255,255,255,0.9);
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.action-btn:hover {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.4);
  color: #ffffff;
}

.action-btn.active {
  background: var(--accent-gold);
  border-color: var(--accent-gold);
  color: #ffffff;
  box-shadow: 0 0 20px var(--accent-gold-glow);
}

.user-rating-badge {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.user-rating-badge:hover {
  background: rgba(255,255,255,0.2);
  border-color: var(--accent-gold);
}

.rating-stars-mini { display: flex; gap: 1px; }

.star-mini {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.3);
  line-height: 1;
}

.star-mini.filled { color: var(--accent-gold); }

.rating-score-text {
  font-size: 0.8rem; font-weight: 700;
  color: var(--accent-gold);
}

/* Summary + Play Links row */
.summary-play-row {
  display: flex; gap: 32px; margin-top: 20px; align-items: flex-start;
}

.detail-summary {
  flex: 1;
  font-size: 0.95rem; color: rgba(255,255,255,0.8);
  line-height: 1.8;
}

.play-links-sidebar {
  flex-shrink: 0;
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  width: 320px;
}

.play-link-chip {
  display: flex; align-items: center; justify-content: center;
  padding: 8px 16px;
  font-size: 0.8rem; font-weight: 600;
  color: rgba(255,255,255,0.9);
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: var(--radius-sm);
  text-decoration: none;
  width: 100%;
  transition: all var(--duration-fast) var(--ease-out);
}
.play-link-chip:hover {
  color: #ffffff;
  border-color: rgba(255,255,255,0.4);
  background: rgba(255,255,255,0.2);
}

/* Three columns */
.info-three-col {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  align-items: start;
  margin-bottom: 48px;
}

.info-col { margin-bottom: 0; }

/* Crew row */
.crew-row { display: flex; flex-wrap: wrap; gap: 12px; }
.crew-person {
  display: flex; align-items: center; gap: 12px;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out);
}
.crew-person:hover { background: var(--glass-hover); }
.crew-person:hover .crew-name { color: var(--accent-gold); }
.crew-img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.crew-img-placeholder {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: var(--glass);
  display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; font-weight: 700;
  color: var(--text-muted);
  flex-shrink: 0;
}
.crew-name {
  font-size: 0.9rem; font-weight: 600;
  color: var(--text-primary);
  transition: color var(--duration-fast) var(--ease-out);
}

/* Body */
.detail-body {
  max-width: var(--content-max);
  margin: 0 auto; padding: 48px 48px 0;
}

.detail-section { margin-bottom: 48px; }

.section-heading {
  font-family: var(--font-display);
  font-size: 1.3rem; font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 20px; letter-spacing: -0.01em;
}

/* Rating Bars */
.rating-dist-row { display: flex; align-items: flex-start; gap: 24px; }
.rating-bars { display: flex; flex-direction: column; gap: 8px; flex: 1; }
.rank-inline { display: flex; flex-direction: column; gap: 8px; justify-content: center; padding-top: 2px; flex-shrink: 0; }
.rank-inline-item { font-size: 0.78rem; color: var(--text-muted); white-space: nowrap; }
.rating-bar-row { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 32px; font-size: 0.8rem; color: var(--text-muted); text-align: right; }
.bar-track { flex: 1; height: 6px; background: var(--glass); border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, var(--accent-gold), var(--accent-gold-dim)); border-radius: 3px; transition: width 0.6s var(--ease-out); }
.bar-pct { width: 48px; font-size: 0.75rem; color: var(--text-muted); }

/* Cast */
.cast-scroll { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 16px; }
.cast-card { display: flex; flex-direction: column; align-items: center; gap: 8px; text-decoration: none; }
.cast-avatar { width: 80px; height: 80px; border-radius: 50%; overflow: hidden; background: var(--bg-surface); }
.cast-avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; font-weight: 700; color: var(--text-muted); background: var(--bg-elevated); }
.cast-name { font-size: 0.8rem; font-weight: 600; color: var(--text-primary); text-align: center; }
.cast-role { font-size: 0.7rem; color: var(--text-muted); text-align: center; }

/* Awards */
.awards-list { display: flex; flex-direction: column; gap: 12px; }
.award-item { display: flex; align-items: center; gap: 16px; padding: 12px 16px; background: var(--glass); border-radius: var(--radius-sm); }
.award-ceremony { font-size: 0.85rem; font-weight: 600; color: var(--text-primary); }
.award-category { font-size: 0.85rem; color: var(--text-secondary); flex: 1; }
.award-status { font-size: 0.75rem; color: var(--text-muted); padding: 4px 12px; background: var(--glass); border-radius: var(--radius-pill); }
.award-status.won { color: var(--accent-gold); background: var(--accent-gold-glow); }
.award-status.nominated { color: var(--accent-red); background: var(--accent-glow); }

/* Release Dates */
.release-list { display: flex; flex-direction: column; gap: 8px; }
.release-item { display: flex; align-items: center; gap: 12px; }
.release-region { font-size: 0.8rem; font-weight: 600; color: var(--text-primary); min-width: 60px; }
.release-date { font-size: 0.8rem; color: var(--text-muted); }

/* Akas */
.akas-list { display: flex; flex-direction: column; gap: 6px; }
.akas-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 16px; }
.aka-item { font-size: 0.85rem; color: var(--text-secondary); }

/* Comments */
.comments-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.write-comment-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; font-size: 0.8rem; font-weight: 600;
  color: var(--text-primary); background: var(--glass);
  border: 1px solid var(--glass-border); border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}
.write-comment-btn:hover { background: var(--glass-hover); border-color: var(--glass-border-hover); }

.comments-sort-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.sort-btn {
  padding: 6px 16px; font-size: 0.8rem; font-weight: 600;
  color: var(--text-muted); background: transparent;
  border: 1px solid var(--glass-border); border-radius: var(--radius-pill);
  transition: all var(--duration-fast) var(--ease-out);
}
.sort-btn:hover { color: var(--text-primary); }
.sort-btn.active { color: white; background: var(--accent-red); border-color: var(--accent-red); }

.comments-list { display: flex; flex-direction: column; gap: 16px; }
.comment-card { padding: 20px; background: var(--glass); border: 1px solid var(--glass-border); border-radius: var(--radius-md); }
.comment-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.comment-author { font-size: 0.85rem; font-weight: 600; color: var(--text-primary); }
.comment-date { font-size: 0.75rem; color: var(--text-muted); margin-left: auto; }
.comment-text { font-size: 0.9rem; color: var(--text-secondary); line-height: 1.7; }
.comment-footer { display: flex; align-items: center; margin-top: 12px; }
.comment-like-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; font-size: 0.8rem; color: var(--text-muted);
  background: transparent; border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.comment-like-btn:hover { color: var(--text-primary); border-color: var(--glass-border-hover); }
.comment-like-btn.liked { color: var(--accent-red); border-color: var(--accent-red); }

.comments-empty { text-align: center; padding: 40px 0; font-size: 0.9rem; color: var(--text-muted); }

.comments-pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 8px; margin-top: 32px;
}
.page-btn {
  padding: 8px 16px; font-size: 0.8rem;
  color: var(--text-secondary); background: var(--glass);
  border: 1px solid var(--glass-border); border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}
.page-btn:hover:not(:disabled) { color: var(--text-primary); background: var(--glass-hover); }
.page-btn.active { color: white; background: var(--accent-red); border-color: var(--accent-red); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-dots { color: var(--text-muted); padding: 0 4px; }

.comment-modal-rating { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.comment-rating-label { font-size: 0.85rem; color: var(--text-muted); }

/* Related */
.related-scroll { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 16px; }
.related-card { display: flex; flex-direction: column; gap: 8px; text-decoration: none; }
.related-title { font-size: 0.8rem; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Loading */
.detail-loading { display: flex; align-items: center; justify-content: center; min-height: 60vh; }

/* ─── Modals ─── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}

.modal-card {
  background: var(--bg-elevated);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  width: 420px; max-width: 90vw;
  box-shadow: var(--shadow-hover);
}

.modal-title {
  font-family: var(--font-display);
  font-size: 1.2rem; font-weight: 700;
  margin-bottom: 24px; color: var(--text-primary);
}

/* Star Picker */
.star-picker { text-align: center; margin-bottom: 24px; }
.star-row { display: inline-flex; gap: 4px; }
.star-pick {
  font-size: 2.5rem; cursor: pointer;
  color: var(--glass);
  transition: color 0.15s, transform 0.15s;
  line-height: 1;
  user-select: none;
}
.star-pick:hover { transform: scale(1.15); }
.star-pick.filled { color: var(--accent-gold); }
.star-pick.half { color: var(--accent-gold-dim); }
.rating-display {
  margin-top: 12px;
  font-size: 0.9rem; color: var(--text-secondary);
}

/* Form */
.form-group { margin-bottom: 20px; }
.form-group textarea {
  width: 100%;
  padding: 12px 14px;
  background: var(--bg-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  color: var(--text-primary);
  resize: vertical;
  font-family: inherit;
  transition: border-color var(--duration-fast) var(--ease-out);
}
.form-group textarea:focus {
  border-color: var(--accent-gold);
  outline: none;
}

.modal-actions {
  display: flex; justify-content: flex-end; gap: 12px; margin-top: 28px;
}

.btn-cancel {
  padding: 10px 20px;
  font-size: 0.8rem; font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}
.btn-cancel:hover { background: var(--glass); }

.btn-save {
  padding: 10px 24px;
  font-size: 0.8rem; font-weight: 600;
  color: white;
  background: var(--accent-gold);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}
.btn-save:hover { background: var(--accent-gold-dim); }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

/* Modal transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-active .modal-card, .modal-leave-active .modal-card {
  transition: transform 0.2s var(--ease-out), opacity 0.2s ease;
}
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card { transform: scale(0.95) translateY(8px); opacity: 0; }
.modal-leave-to .modal-card { transform: scale(0.95); opacity: 0; }

@media (max-width: 768px) {
  .hero-content { flex-direction: column; align-items: flex-start; padding: 0 24px 32px; gap: 24px; }
  .hero-poster { width: 160px; }
  .detail-title { font-size: 1.8rem; }
  .detail-body { padding: 32px 24px 0; }
  .info-three-col { grid-template-columns: 1fr 1fr; gap: 32px; }
  .akas-two-col { grid-template-columns: 1fr; }
  .summary-play-row { flex-direction: column; gap: 20px; }
  .play-links-sidebar { width: 100%; }
  .action-bar { gap: 8px; }
  .action-btn { padding: 8px 16px; font-size: 0.8rem; }
}
</style>
