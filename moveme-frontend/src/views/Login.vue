<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="auth-bg-posters" />
      <div class="auth-bg-gradient" />
    </div>

    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-icon">M</span>
        <h1 class="brand-title">MovieMe</h1>
        <p class="brand-sub">登录你的电影世界</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <div class="form-field">
          <label class="field-label">用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="field-input"
            placeholder="输入用户名"
            autocomplete="username"
          />
        </div>

        <div class="form-field">
          <label class="field-label">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="field-input"
            placeholder="输入密码"
            autocomplete="current-password"
          />
        </div>

        <p v-if="error" class="form-error">{{ error }}</p>

        <button type="submit" class="auth-btn" :disabled="loading">
          <span v-if="loading" class="btn-spinner" />
          <span v-else>登 录</span>
        </button>
      </form>

      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  password: '',
})

async function handleLogin() {
  error.value = ''
  if (!form.username.trim()) { error.value = '请输入用户名'; return }
  if (!form.password) { error.value = '请输入密码'; return }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    router.push('/')
  } catch {
    error.value = '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* Blurred movie poster collage background */
.auth-bg {
  position: absolute; inset: 0;
  background: var(--bg-primary);
}

.auth-bg-posters {
  position: absolute; inset: -20px;
  background:
    url('https://picsum.photos/seed/bg1/600/900') center/cover no-repeat,
    url('https://picsum.photos/seed/bg2/600/900') center/cover no-repeat,
    url('https://picsum.photos/seed/bg3/600/900') center/cover no-repeat;
  filter: blur(40px) brightness(0.8) saturate(1.3);
  opacity: 0.3;
}

.auth-bg-gradient {
  position: absolute; inset: 0;
  background: radial-gradient(ellipse at center, transparent 0%, var(--bg-primary) 70%);
}

.auth-card {
  position: relative; z-index: 2;
  width: 100%;
  max-width: 400px;
  margin: 0 24px;
  padding: 48px 40px;
  background: var(--bg-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.08);
  animation: card-enter 0.6s var(--ease-out) both;
}

.auth-brand { text-align: center; margin-bottom: 36px; }

.brand-icon {
  display: inline-flex;
  align-items: center; justify-content: center;
  width: 56px; height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--accent-red), #b20710);
  color: white;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  box-shadow: 0 4px 20px var(--accent-glow);
}

.brand-title {
  margin: 16px 0 4px;
  font-family: var(--font-display);
  font-size: 1.8rem;
  font-weight: 900;
  color: var(--text-primary);
}

.brand-sub {
  font-size: 0.9rem;
  color: var(--text-muted);
}

/* Form */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field { display: flex; flex-direction: column; gap: 8px; }

.field-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.field-input {
  padding: 14px 16px;
  font-size: 0.95rem;
  color: var(--text-primary);
  background: var(--bg-elevated);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  outline: none;
  transition: border-color var(--duration-fast) var(--ease-out);
}

.field-input::placeholder { color: var(--text-muted); }
.field-input:focus { border-color: var(--accent-red); }

.form-error {
  margin: -8px 0 0;
  font-size: 0.8rem;
  color: var(--accent-red);
}

.auth-btn {
  margin-top: 4px;
  padding: 14px;
  font-size: 1rem;
  font-weight: 700;
  color: white;
  background: var(--accent-red);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-btn:hover { background: #c40810; box-shadow: 0 4px 24px var(--accent-glow); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-spinner {
  width: 20px; height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.auth-footer a {
  color: var(--accent-red);
  text-decoration: none;
  font-weight: 600;
  transition: color var(--duration-fast) var(--ease-out);
}

.auth-footer a:hover { color: #ff3a45; }

@media (max-width: 480px) {
  .auth-card { padding: 32px 24px; }
  .brand-title { font-size: 1.5rem; }
}
</style>
