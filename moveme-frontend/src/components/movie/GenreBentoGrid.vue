<template>
  <section class="bento-section">
    <SectionTitle title="分类宇宙" :to="to" />
    <div class="bento-grid">
      <router-link
        v-for="(genre, i) in genres"
        :key="genre.name"
        :to="`/charts?genre=${genre.name}`"
        class="bento-card"
        :class="[`size-${genre.size}`]"
        :style="{ animationDelay: `${i * 60}ms` }"
      >
        <img v-if="genre.posterUrl" :src="genre.posterUrl" alt="" class="bento-bg" />
        <div class="bento-overlay" />
        <div class="bento-content">
          <h3 class="bento-name">{{ genre.name }}</h3>
          <span class="bento-count">{{ genre.movieCount }} 部</span>
        </div>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { GenreCard } from '../../api/movies'
import SectionTitle from '../common/SectionTitle.vue'

defineProps<{
  genres: GenreCard[]
  to?: string
}>()
</script>

<style scoped>
.bento-section {
  margin-bottom: 48px;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 160px;
  gap: 12px;
}

.bento-card {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  text-decoration: none;
  animation: card-fade-in 0.5s var(--ease-out) forwards;
  opacity: 0;
  cursor: pointer;
}

@keyframes card-fade-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

/* Sizes */
.size-large {
  grid-column: span 2;
  grid-row: span 2;
}

.size-medium {
  grid-column: span 2;
  grid-row: span 1;
}

.size-small {
  grid-column: span 1;
  grid-row: span 1;
}

.bento-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s var(--ease-out);
}

.bento-card:hover .bento-bg {
  transform: scale(1.08);
}

.bento-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.3) 60%, rgba(0,0,0,0.1) 100%);
  transition: background var(--duration-normal) var(--ease-out);
}

.bento-card:hover .bento-overlay {
  background: linear-gradient(to top, rgba(229,9,20,0.4) 0%, rgba(0,0,0,0.3) 60%, rgba(0,0,0,0.1) 100%);
}

.bento-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
}

.bento-name {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.02em;
  text-shadow: 0 2px 8px rgba(0,0,0,0.5);
}

.size-large .bento-name {
  font-size: 2rem;
}

.size-small .bento-name {
  font-size: 1.1rem;
}

.bento-count {
  font-size: 0.75rem;
  color: rgba(255,255,255,0.7);
  margin-top: 4px;
  display: block;
}

@keyframes card-fade-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@media (max-width: 900px) {
  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: 140px;
  }
  .size-large { grid-column: span 2; grid-row: span 1; }
  .size-medium { grid-column: span 1; }
}

@media (max-width: 600px) {
  .bento-grid {
    grid-template-columns: 1fr 1fr;
    grid-auto-rows: 120px;
  }
  .size-large { grid-column: span 2; }
  .size-medium { grid-column: span 1; }
}
</style>
