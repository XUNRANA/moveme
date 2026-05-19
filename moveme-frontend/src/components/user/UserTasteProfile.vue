<template>
  <div class="taste-section">
    <div class="section-header">
      <h2 class="section-title">
        <svg viewBox="0 0 24 24" fill="none" stroke="var(--accent-gold)" stroke-width="2" width="22" height="22">
          <path d="M12 20V10M18 20V4M6 20v-4" />
        </svg>
        我的品味画像
      </h2>
    </div>

    <!-- Empty -->
    <div v-if="!loading && !taste" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
          <path d="M12 20V10M18 20V4M6 20v-4" />
        </svg>
      </div>
      <p>评分更多电影后生成品味画像</p>
    </div>

    <div v-else-if="taste" class="taste-content">
      <!-- Summary Stats -->
      <div class="taste-summary">
        <div class="summary-item">
          <span class="summary-value">{{ taste.avgRatingGiven.toFixed(1) }}</span>
          <span class="summary-label">平均评分</span>
        </div>
        <div class="summary-divider" />
        <div class="summary-item">
          <span class="summary-value">{{ taste.ratingCount }}</span>
          <span class="summary-label">评分次数</span>
        </div>
      </div>

      <!-- Genre Preferences -->
      <div v-if="taste.genrePrefs.length > 0" class="pref-section">
        <h3 class="pref-title">类型偏好</h3>
        <div class="genre-bars">
          <div
            v-for="genre in topGenres"
            :key="genre.genreId"
            class="genre-bar-item"
          >
            <div class="bar-label">
              <span class="bar-name">{{ genre.genreName }}</span>
              <span class="bar-score">{{ genre.score.toFixed(1) }}</span>
            </div>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ width: `${(genre.score / maxGenreScore) * 100}%` }"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Person Preferences -->
      <div v-if="taste.personPrefs.length > 0" class="pref-section">
        <h3 class="pref-title">偏好影人</h3>
        <div class="person-list">
          <div
            v-for="person in topPersons"
            :key="person.personId"
            class="person-item"
          >
            <div class="person-avatar">
              <span>{{ person.personName.charAt(0) }}</span>
            </div>
            <div class="person-info">
              <span class="person-name">{{ person.personName }}</span>
              <span class="person-role">{{ roleLabel(person.roleKind) }}</span>
            </div>
            <span class="person-score">{{ person.score.toFixed(1) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载品味画像..." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getUserTaste, type UserTaste } from '../../api/movies'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const taste = ref<UserTaste | null>(null)
const loading = ref(false)

const topGenres = computed(() => {
  if (!taste.value) return []
  return [...taste.value.genrePrefs].sort((a, b) => b.score - a.score).slice(0, 8)
})

const maxGenreScore = computed(() => {
  if (topGenres.value.length === 0) return 1
  return topGenres.value[0].score
})

const topPersons = computed(() => {
  if (!taste.value) return []
  return [...taste.value.personPrefs].sort((a, b) => b.score - a.score).slice(0, 10)
})

function roleLabel(kind: string) {
  const map: Record<string, string> = { director: '导演', actor: '演员', writer: '编剧' }
  return map[kind] || kind
}

onMounted(async () => {
  loading.value = true
  try {
    taste.value = await getUserTaste()
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.section-header {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text-primary);
}

/* Empty */
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-state p {
  font-size: 0.9rem;
}

/* Taste Content */
.taste-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* Summary */
.taste-summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  padding: 28px;
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
}

.summary-item {
  text-align: center;
}

.summary-value {
  display: block;
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 800;
  color: var(--accent-gold);
  line-height: 1;
}

.summary-label {
  display: block;
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-top: 6px;
}

.summary-divider {
  width: 1px;
  height: 40px;
  background: var(--glass-border);
}

/* Genre Bars */
.pref-section {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 24px;
}

.pref-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.genre-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.genre-bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-name {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-primary);
}

.bar-score {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--accent-gold);
}

.bar-track {
  height: 6px;
  background: var(--glass-hover);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-gold), #f0c040);
  border-radius: 3px;
  transition: width 0.8s var(--ease-out);
}

/* Person List */
.person-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.person-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out);
}

.person-item:hover {
  background: var(--glass-hover);
}

.person-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.person-avatar span {
  font-size: 1rem;
  font-weight: 700;
  color: white;
}

.person-info {
  flex: 1;
  min-width: 0;
}

.person-name {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.person-role {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.person-score {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--accent-gold);
  flex-shrink: 0;
}

/* Loading */
.loading-state {
  padding: 40px 0;
}
</style>
