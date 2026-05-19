<template>
  <div class="boards-page">
    <div class="page-header">
      <h1 class="page-title">热门榜单</h1>
      <p class="page-desc">各大榜单实时更新</p>
    </div>

    <div class="page-content">
      <!-- Board Tabs -->
      <div class="board-tabs">
        <button
          v-for="b in boards"
          :key="b.boardName"
          class="tab-btn"
          :class="{ active: activeBoard === b.boardName }"
          @click="activeBoard = b.boardName"
        >{{ b.displayName }}</button>
      </div>

      <!-- Movie Grid -->
      <LoadingSpinner v-if="loading" text="加载中..." />
      <MovieGrid v-else :movies="boardMovies" :cols="5" show-rank />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { BoardVO, MovieItem } from '../api/movies'
import { listBoards, getBoardMovies } from '../api/movies'
import { chartItemToMovie } from '../utils/movie'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import MovieGrid from '../components/movie/MovieGrid.vue'

const boards = ref<BoardVO[]>([])
const activeBoard = ref('')
const boardMovies = ref<MovieItem[]>([])
const loading = ref(true)

async function loadBoard(boardName: string) {
  if (!boardName) return
  loading.value = true
  try {
    const data = await getBoardMovies(boardName)
    boardMovies.value = data.movies.map(chartItemToMovie)
  } catch (e) {
    console.error('Failed to load board movies:', e)
    boardMovies.value = []
  } finally {
    loading.value = false
  }
}

watch(activeBoard, (name) => {
  if (name) loadBoard(name)
})

onMounted(async () => {
  try {
    boards.value = await listBoards()
    if (boards.value.length) {
      activeBoard.value = boards.value[0].boardName
    }
  } catch (e) {
    console.error('Failed to load boards:', e)
  }
})
</script>

<style scoped>
.boards-page { min-height: 100vh; }

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

.board-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 24px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill);
  transition: all var(--duration-fast) var(--ease-out);
}

.tab-btn:hover {
  color: var(--text-primary);
  background: var(--glass-hover);
}

.tab-btn.active {
  color: white;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

@media (max-width: 768px) {
  .page-header { padding: 32px 16px 0; }
  .page-content { padding: 24px 16px; }
}
</style>
