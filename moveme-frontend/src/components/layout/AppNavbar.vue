<template>
  <nav class="navbar" :class="{ scrolled: isScrolled, hidden: isHidden }">
    <div class="navbar-inner">
      <!-- Brand -->
      <router-link to="/" class="brand">
        <span class="brand-icon">M</span>
        <span class="brand-text">MovieMe</span>
      </router-link>

      <!-- Nav Links -->
      <div class="nav-links">
        <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">发现</router-link>
        <router-link to="/top250" class="nav-link" :class="{ active: $route.path === '/top250' }">TOP250</router-link>
        <router-link to="/boards" class="nav-link" :class="{ active: $route.path === '/boards' }">榜单</router-link>
        <router-link to="/charts" class="nav-link" :class="{ active: $route.path === '/charts' }">分类</router-link>
        <div class="nav-link-dropdown" @mouseenter="showAnnual = true" @mouseleave="showAnnual = false">
          <span class="nav-link">年度</span>
          <Transition name="dropdown">
            <div v-if="showAnnual" class="dropdown-menu">
              <router-link
                v-for="y in annualYears"
                :key="y"
                :to="`/annual/${y}`"
                class="dropdown-item"
              >{{ y }}</router-link>
            </div>
          </Transition>
        </div>
      </div>

      <!-- Right -->
      <div class="nav-right">
        <button class="icon-btn" @click="$emit('search')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
          </svg>
        </button>

        <template v-if="userStore.isLoggedIn">
          <router-link to="/user/dashboard" class="nav-link" :class="{ active: $route.path.startsWith('/user') }">个人中心</router-link>
          <router-link v-if="userStore.isAdmin" to="/admin/dashboard" class="nav-link" :class="{ active: $route.path.startsWith('/admin') }">管理后台</router-link>
          <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <button class="btn-ghost" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-primary">登录</router-link>
          <router-link to="/register" class="btn-ghost">注册</router-link>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

defineEmits<{
  search: []
}>()

const router = useRouter()
const userStore = useUserStore()
const showAnnual = ref(false)
const annualYears = [2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014]

// Scroll behavior
const isScrolled = ref(false)
const isHidden = ref(false)
let lastScrollY = 0

function onScroll() {
  const y = window.scrollY
  isScrolled.value = y > 20
  isHidden.value = y > 300 && y > lastScrollY
  lastScrollY = y
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))

function handleLogout() {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--navbar-height);
  transition: all var(--duration-normal) var(--ease-out);
}

.navbar.scrolled {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--glass-border);
}

.navbar.hidden {
  transform: translateY(-100%);
}

.navbar-inner {
  max-width: var(--content-max);
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 32px;
  gap: 32px;
}

/* Brand */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.brand-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  border-radius: 8px;
  font-weight: 900;
  font-size: 1.1rem;
  color: white;
}

.brand-text {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 1.2rem;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* Nav Links */
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 8px 16px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
}

.nav-link:hover,
.nav-link.active {
  color: var(--text-primary);
  background: var(--glass);
}

/* Dropdown */
.nav-link-dropdown {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  background: var(--bg-elevated);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 8px;
  min-width: 120px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  box-shadow: var(--shadow-card);
}

.dropdown-item {
  padding: 8px 12px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  text-decoration: none;
  text-align: center;
  transition: all var(--duration-fast) var(--ease-out);
}

.dropdown-item:hover {
  color: var(--text-primary);
  background: var(--glass-hover);
}

/* Right */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  transition: all var(--duration-fast) var(--ease-out);
}

.icon-btn svg {
  width: 18px;
  height: 18px;
}

.icon-btn:hover {
  color: var(--text-primary);
  background: var(--glass);
}

.user-name {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.btn-primary {
  padding: 8px 20px;
  background: var(--accent-red);
  color: white;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: var(--radius-sm);
  text-decoration: none;
  transition: background var(--duration-fast) var(--ease-out);
}

.btn-primary:hover {
  background: var(--accent-red-hover);
}

.btn-ghost {
  padding: 8px 16px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-ghost:hover {
  color: var(--text-primary);
  background: var(--glass);
}

/* Dropdown transition */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 768px) {
  .nav-links { display: none; }
  .brand-text { display: none; }
  .navbar-inner { padding: 0 16px; }
}
</style>
