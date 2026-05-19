<template>
  <section class="hero" v-if="movie">
    <!-- Background -->
    <div class="hero-bg">
      <img v-if="posterSrc" :src="posterSrc" alt="" class="hero-bg-img" />
      <div class="hero-gradient" />
    </div>

    <!-- Content -->
    <div class="hero-content">
      <span class="hero-badge">今日推荐</span>
      <h1 class="hero-title">{{ movie.title }}</h1>
      <p v-if="movie.titleEn" class="hero-title-en">{{ movie.titleEn }}</p>

      <div class="hero-meta">
        <RatingBadge v-if="movie.doubanRating" :value="movie.doubanRating" size="lg" glow />
        <span v-if="movie.year" class="meta-item">{{ movie.year }}</span>
        <span v-if="movie.genres?.length" class="meta-item">{{ movie.genres.join(' / ') }}</span>
      </div>

      <p v-if="movie.summary" class="hero-summary">{{ movie.summary }}</p>

      <div class="hero-actions">
        <router-link :to="`/movies/${movie.id}`" class="btn-hero-primary">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8" fill="currentColor"/>
          </svg>
          查看详情
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MovieItem } from '../../api/movies'
import { resolvePoster } from '../../api/movies'
import RatingBadge from '../common/RatingBadge.vue'

const props = defineProps<{
  movie: MovieItem
}>()

const posterSrc = computed(() => resolvePoster(props.movie))
</script>

<style scoped>
.hero {
  position: relative;
  height: 85vh;
  min-height: 500px;
  max-height: 900px;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
}

.hero-bg {
  position: absolute;
  inset: 0;
}

.hero-bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 20%;
  filter: blur(2px) brightness(0.5);
  transform: scale(1.05);
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(to top, var(--bg-primary) 0%, transparent 50%),
    linear-gradient(to right, rgba(0,0,0,0.75) 0%, transparent 60%),
    linear-gradient(to bottom, rgba(0,0,0,0.3) 0%, transparent 30%);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 0 48px 80px;
  width: 100%;
  animation: hero-enter 0.8s var(--ease-out) forwards;
}

@keyframes hero-enter {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  background: var(--accent-red);
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: var(--radius-pill);
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-bottom: 20px;
}

.hero-title {
  font-family: var(--font-display);
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 900;
  color: #ffffff;
  line-height: 1.1;
  letter-spacing: -0.03em;
  margin-bottom: 8px;
  text-shadow: 0 2px 20px rgba(0,0,0,0.5);
}

.hero-title-en {
  font-size: 1.1rem;
  color: rgba(255,255,255,0.7);
  font-weight: 400;
  letter-spacing: 0.02em;
  margin-bottom: 20px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 0.9rem;
  color: rgba(255,255,255,0.8);
}

.hero-summary {
  max-width: 560px;
  font-size: 0.95rem;
  color: rgba(255,255,255,0.8);
  line-height: 1.7;
  margin-bottom: 32px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.btn-hero-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: var(--accent-red);
  color: white;
  font-size: 0.9rem;
  font-weight: 700;
  border-radius: var(--radius-sm);
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-hero-primary:hover {
  background: var(--accent-red-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

@media (max-width: 768px) {
  .hero { height: 70vh; min-height: 400px; }
  .hero-content { padding: 0 24px 48px; }
  .hero-summary { display: none; }
}
</style>
