<template>
  <div class="poster-image" :class="[`ratio-${ratio}`]">
    <img
      v-if="loaded"
      :src="actualSrc"
      :alt="alt"
      loading="lazy"
      @load="onLoad"
      @error="onError"
    />
    <div v-else class="poster-fallback">
      <span class="fallback-icon">M</span>
      <span v-if="alt" class="fallback-text">{{ alt }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
  ratio?: '2-3' | '16-9' | '1-1' | '21-9'
}>(), {
  ratio: '2-3',
})

const loaded = ref(false)
const errored = ref(false)

const actualSrc = computed(() => props.src || '')

watch(actualSrc, () => {
  loaded.value = false
  errored.value = false
  if (actualSrc.value) {
    const img = new Image()
    img.onload = () => { loaded.value = true }
    img.onerror = () => { errored.value = true }
    img.src = actualSrc.value
  }
}, { immediate: true })

function onLoad() {
  loaded.value = true
}

function onError() {
  errored.value = true
  loaded.value = false
}
</script>

<style scoped>
.poster-image {
  position: relative;
  overflow: hidden;
  background: var(--bg-surface);
  border-radius: var(--radius-sm);
}

.ratio-2-3 { aspect-ratio: 2 / 3; }
.ratio-16-9 { aspect-ratio: 16 / 9; }
.ratio-1-1 { aspect-ratio: 1 / 1; }
.ratio-21-9 { aspect-ratio: 21 / 9; }

.poster-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-normal) var(--ease-out);
}

.poster-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, var(--bg-surface), var(--bg-elevated));
}

.fallback-icon {
  font-size: 2rem;
  font-weight: 900;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.fallback-text {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-align: center;
  padding: 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
</style>
