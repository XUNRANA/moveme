<template>
  <div class="profile-card">
    <div class="profile-main">
      <!-- Avatar -->
      <div class="avatar-wrapper">
        <div class="avatar-ring" @click="triggerUpload" title="点击更换头像">
          <div class="avatar">
            <img v-if="userStore.userInfo?.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="avatar" class="avatar-img" />
            <span v-else class="avatar-text">{{ initial }}</span>
          </div>
          <div class="avatar-overlay">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
              <circle cx="12" cy="13" r="4" />
            </svg>
          </div>
        </div>
        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="file-input" @change="handleFileChange" />
        <p v-if="uploading" class="upload-hint">上传中...</p>
      </div>

      <!-- Info -->
      <div class="profile-info">
        <h1 class="profile-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</h1>
        <p v-if="userStore.userInfo?.nickname" class="profile-username">@{{ userStore.userInfo?.username }}</p>
        <p class="profile-bio">{{ userStore.userInfo?.bio || '这个人很懒，什么都没写~' }}</p>
        <div class="profile-meta">
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
            </svg>
            {{ joinDate }} 加入
          </span>
          <span v-if="userStore.userInfo?.email" class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
            </svg>
            {{ userStore.userInfo.email }}
          </span>
        </div>
      </div>

      <!-- Edit Button -->
      <button class="edit-btn" @click="showEdit = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M17 3a2.85 2.85 0 0 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
        </svg>
        编辑资料
      </button>
    </div>

    <!-- Edit Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
          <div class="modal-card">
            <h3 class="modal-title">编辑资料</h3>
            <div class="form-group">
              <label>昵称</label>
              <input v-model="editForm.nickname" type="text" placeholder="输入昵称" />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input v-model="editForm.email" type="email" placeholder="输入邮箱" />
            </div>
            <div class="modal-actions">
              <button class="btn-cancel" @click="showEdit = false">取消</button>
              <button class="btn-save" :disabled="saving" @click="handleSave">
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue'
import { useUserStore } from '../../stores/user'
import { uploadAvatar } from '../../api/movies'
import request from '../../utils/request'

const userStore = useUserStore()
const showEdit = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const initial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const joinDate = computed(() => {
  if (!userStore.userInfo?.createdAt) return ''
  const d = new Date(userStore.userInfo.createdAt)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const editForm = reactive({
  nickname: '',
  email: '',
})

watch(showEdit, (val) => {
  if (val) {
    editForm.nickname = userStore.userInfo?.nickname || ''
    editForm.email = userStore.userInfo?.email || ''
  }
})

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过 5MB')
    input.value = ''
    return
  }

  uploading.value = true
  try {
    await uploadAvatar(file)
    await userStore.fetchUserInfo()
  } catch (err: any) {
    alert(err?.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    await request.put('/users/me', {
      nickname: editForm.nickname,
      email: editForm.email,
    })
    await userStore.fetchUserInfo()
    showEdit.value = false
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-card {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 40px;
  backdrop-filter: blur(12px);
}

.profile-main {
  display: flex;
  align-items: flex-start;
  gap: 32px;
}

/* Avatar */
.avatar-wrapper {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, var(--accent-gold), #f0c040, var(--accent-gold-dim));
  box-shadow: 0 0 24px var(--accent-gold-glow);
  cursor: pointer;
  position: relative;
  transition: transform var(--duration-fast) var(--ease-out);
}

.avatar-ring:hover {
  transform: scale(1.05);
}

.avatar-ring:hover .avatar-overlay {
  opacity: 1;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--bg-primary);
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-size: 2rem;
  font-weight: 800;
  color: white;
  font-family: var(--font-display);
}

.avatar-overlay {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-out);
  color: white;
}

.file-input {
  display: none;
}

.upload-hint {
  font-size: 0.7rem;
  color: var(--accent-gold);
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Info */
.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.2;
}

.profile-username {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 2px;
}

.profile-bio {
  margin-top: 12px;
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.profile-meta {
  margin-top: 16px;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.meta-item svg {
  opacity: 0.6;
}

/* Edit Button */
.edit-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-pill);
  transition: all var(--duration-fast) var(--ease-out);
}

.edit-btn:hover {
  color: var(--accent-gold);
  border-color: var(--accent-gold);
  box-shadow: 0 0 16px var(--accent-gold-glow);
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  background: var(--bg-elevated);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  width: 400px;
  max-width: 90vw;
  box-shadow: var(--shadow-hover);
}

.modal-title {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  padding: 10px 14px;
  background: var(--bg-surface);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  transition: border-color var(--duration-fast) var(--ease-out);
}

.form-group input:focus {
  border-color: var(--accent-gold);
  outline: none;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
}

.btn-cancel {
  padding: 10px 20px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-cancel:hover {
  background: var(--glass);
}

.btn-save {
  padding: 10px 24px;
  font-size: 0.8rem;
  font-weight: 600;
  color: white;
  background: var(--accent-gold);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-save:hover {
  background: var(--accent-gold-dim);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Modal transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform 0.2s var(--ease-out), opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-card {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}

.modal-leave-to .modal-card {
  transform: scale(0.95);
  opacity: 0;
}

@media (max-width: 640px) {
  .profile-main {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-meta {
    justify-content: center;
  }

  .edit-btn {
    align-self: center;
  }
}
</style>
