<template>
  <router-link :to="`/movies/${movie.id}`" class="movie-card" :class="{ 'show-info': showInfo }">
    <div class="card-poster">
      <PosterImage :src="posterSrc" :alt="movie.title" />
      <div class="card-overlay">
        <RatingBadge v-if="movie.doubanRating" :value="movie.doubanRating" size="md" glow />
        <p v-if="movie.summary" class="card-summary">{{ movie.summary }}</p>
      </div>
      <RankBadge v-if="rank" :rank="rank" class="card-rank" />
    </div>
    <div class="card-meta">
      <h3 class="card-title">{{ movie.title }}</h3>
      <div class="card-info">
        <span v-if="movie.year" class="card-year">{{ movie.year }}</span>
        <span v-if="movie.genres?.length" class="card-genre">{{ movie.genres[0] }}</span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MovieItem } from '../../api/movies'
import { resolvePoster } from '../../api/movies'
import PosterImage from '../common/PosterImage.vue'
import RatingBadge from '../common/RatingBadge.vue'
import RankBadge from '../common/RankBadge.vue'

const props = withDefaults(defineProps<{
  movie: MovieItem
  rank?: number | null
  showInfo?: boolean
}>(), {
  rank: null,
  showInfo: false,
})

const posterSrc = computed(() => resolvePoster(props.movie))
</script>

<style scoped>
.movie-card {
  display: block;
  text-decoration: none;
  flex-shrink: 0;
  width: 180px;
}

.card-poster {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  aspect-ratio: 2 / 3;
  transition: transform var(--duration-normal) var(--ease-out);
}

.movie-card:hover .card-poster {
  transform: translateY(-8px);
}

.card-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  background: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.3) 50%, transparent 100%);
  opacity: 0;
  transition: opacity var(--duration-normal) var(--ease-out);
}

.movie-card:hover .card-overlay {
  opacity: 1;
}

.card-summary {
  font-size: 0.75rem;
  color: rgba(255,255,255,0.8);
  line-height: 1.5;
  margin-top: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-rank {
  position: absolute;
  top: 8px;
  left: 8px;
}

.card-meta {
  padding: 12px 4px 0;
}

.card-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--duration-fast) var(--ease-out);
}

.movie-card:hover .card-title {
  color: var(--accent-gold);
}

.card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.card-year {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.card-genre {
  font-size: 0.7rem;
  color: var(--text-muted);
  padding: 2px 8px;
  background: var(--glass);
  border-radius: var(--radius-pill);
}

/* Responsive */
@media (max-width: 640px) {
  .movie-card { width: 140px; }
  .card-title { font-size: 0.8rem; }
}
</style>
