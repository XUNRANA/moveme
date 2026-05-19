<template>
  <div class="person-page">
    <div v-if="loading" class="person-loading">
      <LoadingSpinner text="加载中..." />
    </div>

    <template v-else-if="person">
      <!-- Person Header -->
      <div class="person-hero">
        <div class="person-hero-bg" />
        <div class="person-hero-content">
          <div class="person-avatar">
            <img v-if="person.avatarLocalPath || person.avatarUrl" :src="person.avatarLocalPath || person.avatarUrl" alt="" class="avatar-img" />
            <div v-else class="avatar-fallback">{{ person.name?.[0] }}</div>
          </div>
          <div class="person-info">
            <h1 class="person-name">{{ person.name }}</h1>
            <p v-if="person.nameEn" class="person-name-en">{{ person.nameEn }}</p>
            <div class="person-meta">
              <span v-if="person.gender && person.gender !== '未知'" class="meta-tag">{{ person.gender }}</span>
              <span v-if="person.birthDate" class="meta-tag">{{ person.birthDate }}</span>
              <span v-if="person.birthPlace" class="meta-tag">{{ person.birthPlace }}</span>
            </div>
            <div v-if="person.movieCount" class="person-stats">
              <div class="stat-item">
                <span class="stat-val">{{ person.movieCount }}</span>
                <span class="stat-label">作品</span>
              </div>
              <div v-if="person.avgMovieRating" class="stat-item">
                <span class="stat-val">{{ Number(person.avgMovieRating).toFixed(1) }}</span>
                <span class="stat-label">均分</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="person-body">
        <!-- Bio -->
        <section v-if="person.bio" class="person-section">
          <h2 class="section-heading">人物简介</h2>
          <p class="bio-text" :class="{ collapsed: bioCollapsed }">{{ person.bio }}</p>
          <button v-if="person.bio.length > 200" class="toggle-btn" @click="bioCollapsed = !bioCollapsed">
            {{ bioCollapsed ? '展开全部' : '收起' }}
          </button>
        </section>

        <!-- Filmography -->
        <section class="person-section">
          <div class="film-tabs">
            <button
              v-for="tab in availableTabs"
              :key="tab.key"
              class="film-tab"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
              <span class="tab-count">{{ tab.count }}</span>
            </button>
          </div>

          <div v-if="currentFilms.length" class="film-grid">
            <router-link
              v-for="item in currentFilms"
              :key="item.movieId"
              :to="`/movies/${item.movieId}`"
              class="film-card"
            >
              <PosterImage :src="item.posterUrl" :alt="item.title" />
              <div class="film-meta">
                <span class="film-title">{{ item.title }}</span>
                <div class="film-sub">
                  <span v-if="item.rating" class="film-rating">{{ Number(item.rating).toFixed(1) }}</span>
                  <span v-if="item.year" class="film-year">{{ item.year }}</span>
                  <span v-if="item.roleName" class="film-role">{{ item.roleName }}</span>
                </div>
              </div>
            </router-link>
          </div>

          <div v-else class="empty-state">暂无作品</div>
        </section>
      </div>
    </template>

    <div v-else class="empty-state" style="min-height: 60vh;">
      <p>人物不存在</p>
      <router-link to="/" class="back-link">回到首页</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPersonDetail, type PersonDetail, type FilmographyItem } from '../api/movies'
import PosterImage from '../components/common/PosterImage.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'

const route = useRoute()

const person = ref<PersonDetail | null>(null)
const loading = ref(false)
const bioCollapsed = ref(true)
const activeTab = ref<'acted' | 'directed' | 'written'>('acted')

const availableTabs = computed(() => {
  if (!person.value) return []
  const tabs = []
  if (person.value.acted?.length) tabs.push({ key: 'acted' as const, label: '演出', count: person.value.acted.length })
  if (person.value.directed?.length) tabs.push({ key: 'directed' as const, label: '导演', count: person.value.directed.length })
  if (person.value.written?.length) tabs.push({ key: 'written' as const, label: '编剧', count: person.value.written.length })
  return tabs
})

const currentFilms = computed<FilmographyItem[]>(() => {
  if (!person.value) return []
  return person.value[activeTab.value] || []
})

async function loadPerson(id: number) {
  loading.value = true
  try {
    person.value = await getPersonDetail(id)
    if (person.value?.acted?.length) activeTab.value = 'acted'
    else if (person.value?.directed?.length) activeTab.value = 'directed'
    else if (person.value?.written?.length) activeTab.value = 'written'
  } catch {
    person.value = null
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, (id) => {
  const pid = Number(id)
  if (Number.isFinite(pid) && pid > 0) void loadPerson(pid)
  else person.value = null
}, { immediate: true })
</script>

<style scoped>
.person-page { min-height: 100vh; }

.person-loading { display: flex; align-items: center; justify-content: center; min-height: 60vh; }

/* Hero */
.person-hero {
  position: relative;
  padding: 80px 0 48px;
  overflow: hidden;
}

.person-hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, var(--bg-elevated) 0%, var(--bg-primary) 100%);
}

.person-hero-content {
  position: relative; z-index: 2;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.person-avatar {
  flex-shrink: 0;
  width: 140px; height: 140px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
}

.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 3.5rem; font-weight: 900; color: var(--text-muted);
  background: var(--bg-elevated);
}

.person-info { flex: 1; }

.person-name {
  font-family: var(--font-display);
  font-size: 2.5rem; font-weight: 900;
  color: var(--text-primary);
  line-height: 1.2;
}

.person-name-en {
  font-size: 1rem; color: var(--text-muted); margin-top: 4px;
}

.person-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.meta-tag {
  padding: 6px 14px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill);
}

.person-stats { display: flex; gap: 32px; margin-top: 20px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-val {
  font-family: var(--font-display);
  font-size: 2rem; font-weight: 900;
  color: var(--accent-gold);
}
.stat-label { font-size: 0.75rem; color: var(--text-muted); margin-top: 2px; }

/* Body */
.person-body {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 32px;
}

.person-section { margin-bottom: 40px; }

.section-heading {
  font-family: var(--font-display);
  font-size: 1.3rem; font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 16px;
}

/* Bio */
.bio-text {
  font-size: 0.95rem;
  color: var(--text-secondary);
  line-height: 1.9;
  white-space: pre-line;
}

.bio-text.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.toggle-btn {
  margin-top: 8px;
  background: none; border: none;
  color: var(--accent-red);
  font-size: 0.85rem; font-weight: 600;
  cursor: pointer;
}

.toggle-btn:hover { text-decoration: underline; }

/* Film Tabs */
.film-tabs { display: flex; gap: 8px; margin-bottom: 24px; }
.film-tab {
  padding: 10px 20px;
  font-size: 0.85rem; font-weight: 600;
  color: var(--text-muted);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill);
  cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  transition: all var(--duration-fast) var(--ease-out);
}
.film-tab:hover { color: var(--text-primary); background: var(--glass-hover); }
.film-tab.active { color: white; background: var(--accent-red); border-color: var(--accent-red); }

.tab-count {
  font-size: 0.7rem;
  padding: 2px 7px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.15);
}
.film-tab.active .tab-count { background: rgba(255, 255, 255, 0.25); }

/* Film Grid */
.film-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 20px;
}

.film-card {
  text-decoration: none;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-surface);
  transition: transform var(--duration-fast) var(--ease-out), box-shadow var(--duration-fast) var(--ease-out);
}

.film-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card);
}

.film-meta { padding: 12px; }
.film-title {
  font-size: 0.85rem; font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  display: block;
}
.film-card:hover .film-title { color: var(--accent-gold); }

.film-sub { display: flex; gap: 8px; margin-top: 4px; font-size: 0.75rem; }
.film-rating { color: var(--accent-gold); font-weight: 600; }
.film-year { color: var(--text-muted); }
.film-role { color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 16px; min-height: 200px;
  font-size: 0.95rem; color: var(--text-muted);
}

.back-link {
  color: var(--accent-red); text-decoration: none; font-weight: 600;
}

@media (max-width: 768px) {
  .person-hero-content { flex-direction: column; align-items: center; text-align: center; }
  .person-avatar { width: 100px; height: 100px; }
  .person-name { font-size: 1.8rem; }
  .person-meta { justify-content: center; }
  .person-stats { justify-content: center; }
  .person-body { padding: 24px 16px; }
  .film-grid { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 12px; }
}
</style>
