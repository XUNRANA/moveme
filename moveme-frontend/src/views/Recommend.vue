<template>
  <div class="recommend-page">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <span class="sidebar-title">对话历史</span>
        <button class="btn-new" @click="newConversation" title="新对话">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <path d="M12 5v14M5 12h14"/>
          </svg>
        </button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeId }"
          @click="switchConversation(conv.id)"
        >
          <span class="conv-title">{{ conv.title }}</span>
          <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
          <button class="btn-delete" @click.stop="deleteConversation(conv.id)" title="删除">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>
            </svg>
          </button>
        </div>
        <div v-if="conversations.length === 0" class="conv-empty">暂无对话</div>
      </div>
    </aside>

    <!-- Main -->
    <div class="main-area">
      <!-- Toggle sidebar (mobile) -->
      <button class="btn-sidebar-toggle" @click="sidebarOpen = !sidebarOpen">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
          <path d="M3 12h18M3 6h18M3 18h18"/>
        </svg>
      </button>

      <!-- Hero -->
      <section class="hero" v-if="activeMessages.length === 0">
        <div class="hero-content">
          <h1 class="hero-title">AI 电影推荐</h1>
          <p class="hero-subtitle">基于小米 MiMO 大模型，为你量身定制观影指南</p>
          <button class="btn-quick" :disabled="loading" @click="handleQuickRecommend">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
            </svg>
            {{ loading && mode === 'quick' ? '生成中...' : '一键推荐' }}
          </button>
        </div>
      </section>

      <!-- Chat Area -->
      <section class="chat-section">
        <div class="chat-container">
          <!-- Messages -->
          <div ref="messagesRef" class="messages">
            <div v-if="activeMessages.length === 0" class="empty-state">
              <div class="empty-icon">🎬</div>
              <p>告诉我你想看什么类型的电影，或者直接点击「一键推荐」</p>
              <div class="quick-prompts">
                <button v-for="p in quickPrompts" :key="p" class="chip" @click="sendQuickPrompt(p)">{{ p }}</button>
              </div>
            </div>

            <div v-for="(msg, i) in activeMessages" :key="i" class="message" :class="msg.role">
              <div class="message-avatar">
                <span v-if="msg.role === 'user'">你</span>
                <span v-else>AI</span>
              </div>
              <div class="message-body">
                <div class="message-content" v-html="renderMarkdown(msg.content)" @click="handleLinkClick"></div>
              </div>
            </div>

            <!-- Streaming indicator -->
            <div v-if="streaming" class="message assistant">
              <div class="message-avatar"><span>AI</span></div>
              <div class="message-body">
                <div class="message-content streaming-cursor" v-html="renderMarkdown(streamBuffer)"></div>
              </div>
            </div>
          </div>

          <!-- Input -->
          <div class="input-bar">
            <textarea
              v-model="inputText"
              class="input-field"
              placeholder="说说你想看什么..."
              rows="1"
              :disabled="loading"
              @keydown.enter.exact.prevent="handleSend"
              @input="autoResize"
            ></textarea>
            <button class="btn-send" :disabled="!inputText.trim() || loading" @click="handleSend">
              <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { marked } from 'marked'
import { chatRecommend, quickRecommend, type ChatMessage } from '../api/movies'

const router = useRouter()
const route = useRoute()

interface Conversation {
  id: string
  title: string
  messages: ChatMessage[]
  createdAt: number
  updatedAt: number
}

const STORAGE_KEY = 'moveme_conversations'
const ACTIVE_KEY = 'moveme_active_conversation'

const conversations = ref<Conversation[]>([])
const activeId = ref('')
const inputText = ref('')
const loading = ref(false)
const streaming = ref(false)
const streamBuffer = ref('')
const mode = ref<'chat' | 'quick'>('chat')
const messagesRef = ref<HTMLElement>()
const sidebarOpen = ref(false)

const quickPrompts = [
  '推荐几部高分悬疑片',
  '适合周末看的轻松喜剧',
  '最近有什么好电影？',
  '类似《盗梦空间》的烧脑片'
]

const activeMessages = computed(() => {
  const conv = conversations.value.find(c => c.id === activeId.value)
  return conv ? conv.messages : []
})

// --- Persistence ---
function loadConversations() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) conversations.value = JSON.parse(raw)
  } catch {}
  // 优先用 URL 里的 id，其次用 localStorage 记录的
  const urlId = route.params.id as string
  if (urlId && conversations.value.some(c => c.id === urlId)) {
    activeId.value = urlId
  } else {
    const savedActive = localStorage.getItem(ACTIVE_KEY)
    if (savedActive && conversations.value.some(c => c.id === savedActive)) {
      activeId.value = savedActive
    } else if (conversations.value.length > 0) {
      activeId.value = conversations.value[0].id
    }
  }
}

function saveConversations() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations.value))
  localStorage.setItem(ACTIVE_KEY, activeId.value)
}

// --- Conversation management ---
function navigateToConv(id: string) {
  router.push(`/recommend/${id}`)
}

function newConversation() {
  const conv: Conversation = {
    id: crypto.randomUUID(),
    title: '新对话',
    messages: [],
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
  conversations.value.unshift(conv)
  activeId.value = conv.id
  sidebarOpen.value = false
  saveConversations()
  navigateToConv(conv.id)
}

function switchConversation(id: string) {
  activeId.value = id
  sidebarOpen.value = false
  saveConversations()
  navigateToConv(id)
  scrollToBottom()
}

function deleteConversation(id: string) {
  conversations.value = conversations.value.filter(c => c.id !== id)
  if (activeId.value === id) {
    activeId.value = conversations.value.length > 0 ? conversations.value[0].id : ''
  }
  saveConversations()
}

function ensureActiveConversation(): Conversation {
  if (!activeId.value || !conversations.value.some(c => c.id === activeId.value)) {
    newConversation()
  }
  const conv = conversations.value.find(c => c.id === activeId.value)!
  // 确保 URL 包含当前对话 ID
  if (route.params.id !== conv.id) {
    navigateToConv(conv.id)
  }
  return conv
}

// --- Helpers ---
function formatTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function renderMarkdown(text: string): string {
  return marked.parse(text) as string
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

async function typewriterEffect(conv: Conversation, fullText: string) {
  streaming.value = true
  streamBuffer.value = ''
  const chars = fullText.split('')
  for (let i = 0; i < chars.length; i++) {
    streamBuffer.value += chars[i]
    if (i % 3 === 0) scrollToBottom()
    await new Promise(r => setTimeout(r, 15))
  }
  conv.messages.push({ role: 'assistant', content: fullText })
  streamBuffer.value = ''
  streaming.value = false
}

function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function handleLinkClick(e: Event) {
  const target = e.target as HTMLElement
  if (target.tagName === 'A') {
    const href = target.getAttribute('href')
    if (href && href.startsWith('/')) {
      e.preventDefault()
      router.push(href)
    }
  }
}

// --- Actions ---
function resetState() {
  loading.value = false
  streaming.value = false
  streamBuffer.value = ''
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  const conv = ensureActiveConversation()
  conv.messages.push({ role: 'user', content: text })
  if (conv.messages.length === 1) {
    conv.title = text.length > 20 ? text.slice(0, 20) + '...' : text
  }
  inputText.value = ''
  loading.value = true
  streaming.value = true
  streamBuffer.value = ''
  mode.value = 'chat'
  saveConversations() // 用户消息立即保存，不等 AI 回完
  scrollToBottom()

  try {
    const fullText = await chatRecommend(conv.messages)
    // 模拟打字效果
    await typewriterEffect(conv, fullText)
  } catch (e: any) {
    conv.messages.push({ role: 'assistant', content: `抱歉，出现了错误：${e.message}` })
  } finally {
    resetState()
    saveConversations()
    scrollToBottom()
  }
}

async function handleQuickRecommend() {
  if (loading.value) return
  const conv = ensureActiveConversation()
  conv.messages.push({ role: 'user', content: '请根据我的口味推荐几部电影' })
  if (conv.messages.length === 1) conv.title = '一键推荐'
  saveConversations() // 用户消息立即保存
  loading.value = true
  mode.value = 'quick'
  scrollToBottom()

  try {
    const reply = await quickRecommend()
    await typewriterEffect(conv, reply)
  } catch (e: any) {
    conv.messages.push({ role: 'assistant', content: `抱歉，推荐失败：${e.message}` })
  } finally {
    resetState()
    saveConversations()
    scrollToBottom()
  }
}

function sendQuickPrompt(prompt: string) {
  inputText.value = prompt
  handleSend()
}

// --- Init ---
onMounted(() => {
  marked.setOptions({ breaks: true, gfm: true })
  loadConversations()
  if (conversations.value.length === 0) newConversation()
  // URL 有 id 但没匹配到对话 → 导航到当前活跃对话
  else if (route.params.id && !conversations.value.some(c => c.id === route.params.id)) {
    navigateToConv(activeId.value)
  }
})

// 浏览器前进/后退时同步对话
watch(() => route.params.id, (newId) => {
  if (newId && conversations.value.some(c => c.id === newId)) {
    activeId.value = newId as string
    saveConversations()
    scrollToBottom()
  }
})
</script>

<style scoped>
.recommend-page {
  height: calc(100vh - var(--navbar-height));
  display: flex;
  background: var(--bg);
  overflow: hidden;
}

/* Sidebar */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--bg-elevated);
  border-right: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--glass-border);
}

.sidebar-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.btn-new {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  border: 1px solid var(--glass-border);
  background: var(--glass);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-new:hover {
  color: var(--accent-red);
  border-color: var(--accent-red);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  position: relative;
}

.conv-item:hover {
  background: var(--glass-hover);
}

.conv-item.active {
  background: var(--glass);
  border: 1px solid var(--glass-border);
}

.conv-title {
  flex: 1;
  font-size: 0.8rem;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 0.7rem;
  color: var(--text-muted);
  flex-shrink: 0;
}

.btn-delete {
  opacity: 0;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.conv-item:hover .btn-delete {
  opacity: 1;
}

.btn-delete:hover {
  color: var(--accent-red);
  background: rgba(255, 77, 85, 0.1);
}

.conv-empty {
  text-align: center;
  padding: 24px;
  font-size: 0.8rem;
  color: var(--text-muted);
}

/* Main */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.btn-sidebar-toggle {
  display: none;
  position: fixed;
  bottom: 80px;
  left: 16px;
  z-index: 50;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-red);
  color: white;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
}

/* Hero */
.hero {
  padding: 48px 32px 32px;
  text-align: center;
  background: linear-gradient(180deg, var(--bg-elevated) 0%, var(--bg) 100%);
}

.hero-content {
  max-width: 600px;
  margin: 0 auto;
}

.hero-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 0.95rem;
  color: var(--text-muted);
  margin-bottom: 24px;
}

.btn-quick {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  color: white;
  font-size: 0.9rem;
  font-weight: 600;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-quick:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(255, 77, 85, 0.3);
}

.btn-quick:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Chat */
.chat-section {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 16px 32px;
  overflow: hidden;
}

.chat-container {
  width: 100%;
  max-width: 720px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 0;
  overflow-y: auto;
  min-height: 0;
}

.empty-state {
  text-align: center;
  padding: 48px 16px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 12px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
}

.chip {
  padding: 8px 16px;
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: 999px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.chip:hover {
  background: var(--glass-hover);
  color: var(--text-primary);
  border-color: var(--accent-red);
}

/* Message bubble */
.message {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: var(--accent-red);
  color: white;
}

.message.assistant .message-avatar {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  color: var(--text-primary);
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-content {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  line-height: 1.7;
  word-break: break-word;
}

.message.user .message-content {
  background: var(--accent-red);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-content {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}

.message-content :deep(p) { margin: 0 0 8px; }
.message-content :deep(p:last-child) { margin-bottom: 0; }
.message-content :deep(strong) { font-weight: 700; color: var(--accent-gold); }
.message.user .message-content :deep(strong) { color: white; }
.message-content :deep(ul), .message-content :deep(ol) { margin: 4px 0; padding-left: 20px; }
.message-content :deep(li) { margin: 2px 0; }
.message-content :deep(code) { background: rgba(0,0,0,0.1); padding: 1px 4px; border-radius: 3px; font-size: 0.85em; }
.message-content :deep(a) { color: var(--accent-red); text-decoration: underline; }
.message.user .message-content :deep(a) { color: #ffcdd2; }

.streaming-cursor::after {
  content: '\25CA';
  animation: blink 0.8s infinite;
  color: var(--accent-red);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Input */
.input-bar {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  transition: border-color var(--duration-fast) var(--ease-out);
  flex-shrink: 0;
}

.input-bar:focus-within { border-color: var(--accent-red); }

.input-field {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  font-size: 0.9rem;
  color: var(--text-primary);
  resize: none;
  line-height: 1.5;
  max-height: 120px;
  font-family: inherit;
}

.input-field::placeholder { color: var(--text-muted); }

.btn-send {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--accent-red);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  flex-shrink: 0;
}

.btn-send:hover:not(:disabled) { background: var(--accent-red-hover); }
.btn-send:disabled { opacity: 0.4; cursor: not-allowed; }

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: var(--navbar-height);
    bottom: 0;
    z-index: 90;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }
  .sidebar.open { transform: translateX(0); }
  .btn-sidebar-toggle { display: flex; }
  .hero { padding: 32px 16px 24px; }
  .hero-title { font-size: 1.5rem; }
  .message { max-width: 92%; }
}
</style>
