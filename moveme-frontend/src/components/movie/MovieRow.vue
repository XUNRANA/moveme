<template>
  <section class="movie-row">
    <SectionTitle :title="title" :to="viewAllTo" />
    <div class="row-scroll" ref="scrollRef">
      <MovieCard
        v-for="(movie, i) in movies"
        :key="movie.id"
        :movie="movie"
        :rank="showRank ? i + 1 : undefined"
        class="row-item"
        :style="{ animationDelay: `${i * 50}ms` }"
      />
    </div>
    <!-- Scroll buttons -->
    <button v-if="canScrollLeft" class="scroll-btn scroll-left" @click="scroll(-1)">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6"/></svg>
    </button>
    <button v-if="canScrollRight" class="scroll-btn scroll-right" @click="scroll(1)">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
    </button>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { MovieItem } from '../../api/movies'
import MovieCard from './MovieCard.vue'
import SectionTitle from '../common/SectionTitle.vue'

withDefaults(defineProps<{
  title: string
  movies: MovieItem[]
  viewAllTo?: string
  showRank?: boolean
}>(), {
  showRank: false,
})

const scrollRef = ref<HTMLElement>()
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

function updateScrollState() {
  const el = scrollRef.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 10
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 10
}

function scroll(dir: number) {
  const el = scrollRef.value
  if (!el) return
  el.scrollBy({ left: dir * 800, behavior: 'smooth' })
}

onMounted(() => {
  scrollRef.value?.addEventListener('scroll', updateScrollState, { passive: true })
  updateScrollState()
  window.addEventListener('resize', updateScrollState)
})

onUnmounted(() => {
  scrollRef.value?.removeEventListener('scroll', updateScrollState)
  window.removeEventListener('resize', updateScrollState)
})
</script>

<style scoped>
.movie-row {
  position: relative;
  margin-bottom: 48px;
}

.row-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  padding: 8px 4px 16px;
  -ms-overflow-style: none;
}

.row-scroll::-webkit-scrollbar {
  display: none;
}

.row-item {
  scroll-snap-align: start;
  animation: card-fade-in 0.5s var(--ease-out) forwards;
  opacity: 0;
}

/* Scroll buttons */
.scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid var(--glass-border);
  border-radius: 50%;
  color: var(--text-primary);
  backdrop-filter: blur(8px);
  transition: all var(--duration-fast) var(--ease-out);
  z-index: 5;
  opacity: 0;
}

.movie-row:hover .scroll-btn {
  opacity: 1;
}

.scroll-btn:hover {
  background: rgba(0, 0, 0, 0.9);
  transform: translateY(-50%) scale(1.1);
}

.scroll-btn svg {
  width: 20px;
  height: 20px;
}

.scroll-left { left: -8px; }
.scroll-right { right: -8px; }
</style>
