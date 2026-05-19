<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div v-if="visible" class="search-overlay" @click.self="$emit('close')">
        <div class="search-container">
          <div class="search-input-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
            </svg>
            <input
              ref="inputRef"
              v-model="query"
              type="text"
              placeholder="搜索电影、导演、演员..."
              class="search-input"
              @keyup.esc="$emit('close')"
              @keyup.enter="onSearch"
            />
            <button class="search-close" @click="$emit('close')">
              <span>ESC</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  search: [query: string]
}>()

const query = ref('')
const inputRef = ref<HTMLInputElement>()

watch(() => props.visible, (v) => {
  if (v) {
    query.value = ''
    nextTick(() => inputRef.value?.focus())
  }
})

function onSearch() {
  if (query.value.trim()) {
    emit('search', query.value.trim())
  }
}
</script>

<style scoped>
.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 20vh;
}

.search-container {
  width: 100%;
  max-width: 640px;
  padding: 0 24px;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--bg-elevated);
  border: 1px solid var(--glass-border-hover);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  gap: 12px;
}

.search-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  font-size: 1.125rem;
  background: none;
  color: var(--text-primary);
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-close {
  padding: 4px 10px;
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  color: var(--text-muted);
  flex-shrink: 0;
}

/* Transition */
.overlay-enter-active,
.overlay-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}
</style>
