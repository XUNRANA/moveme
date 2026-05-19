<template>
  <div class="app-root">
    <div class="film-grain" />
    <AppNavbar @search="showSearch = true" />
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </main>
    <AppFooter />
    <SearchOverlay :visible="showSearch" @close="showSearch = false" @search="onSearch" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppNavbar from './components/layout/AppNavbar.vue'
import AppFooter from './components/layout/AppFooter.vue'
import SearchOverlay from './components/common/SearchOverlay.vue'

const router = useRouter()
const showSearch = ref(false)

function onSearch(query: string) {
  showSearch.value = false
  router.push({ path: '/', query: { q: query } })
}
</script>

<style>
.app-root {
  min-height: 100vh;
  position: relative;
}

.app-main {
  padding-top: var(--navbar-height);
}

/* Page transition */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
