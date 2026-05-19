<template>
  <div class="user-panel">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
        </svg>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索用户名/昵称..."
          @keyup.enter="loadUsers(1)"
        />
      </div>
      <div class="filter-group">
        <select v-model="roleFilter" @change="loadUsers(1)">
          <option value="">全部角色</option>
          <option value="0">普通用户</option>
          <option value="1">管理员</option>
        </select>
        <select v-model="statusFilter" @change="loadUsers(1)">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td class="td-id">{{ user.id }}</td>
            <td class="td-username">{{ user.username }}</td>
            <td>{{ user.nickname || '--' }}</td>
            <td>{{ user.email || '--' }}</td>
            <td>
              <span class="role-badge" :class="user.role === 1 ? 'role-admin' : 'role-user'">
                {{ user.role === 1 ? '管理员' : '用户' }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="user.status === 1 ? 'status-active' : 'status-disabled'">
                {{ user.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="td-time">{{ formatDate(user.createdAt) }}</td>
            <td class="td-actions">
              <button
                class="action-btn"
                :class="user.status === 1 ? 'btn-disable' : 'btn-enable'"
                @click="toggleStatus(user)"
              >
                {{ user.status === 1 ? '禁用' : '启用' }}
              </button>
              <button
                class="action-btn btn-role"
                @click="toggleRole(user)"
              >
                {{ user.role === 1 ? '降为用户' : '升为管理员' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty -->
    <div v-if="!loading && users.length === 0" class="empty-state">
      <p>没有找到用户</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载用户列表..." />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="loadUsers(currentPage - 1)">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }} (共 {{ total }} 条)</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="loadUsers(currentPage + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminUsers, updateUserStatus, updateUserRole, type AdminUserItem } from '../../api/movies'
import { formatDate } from '../../utils/date'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const users = ref<AdminUserItem[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const statusFilter = ref('')

async function loadUsers(page: number) {
  loading.value = true
  try {
    const params: Record<string, any> = { page, size: 15 }
    if (keyword.value) params.keyword = keyword.value
    if (roleFilter.value !== '') params.role = Number(roleFilter.value)
    if (statusFilter.value !== '') params.status = Number(statusFilter.value)
    const res = await getAdminUsers(params as any)
    users.value = res.records
    total.value = res.total
    totalPages.value = res.pages
    currentPage.value = res.current
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
}

async function toggleStatus(user: AdminUserItem) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await updateUserStatus(user.id, newStatus)
    user.status = newStatus
  } catch {
    // API not implemented yet
  }
}

async function toggleRole(user: AdminUserItem) {
  const newRole = user.role === 1 ? 0 : 1
  try {
    await updateUserRole(user.id, newRole)
    user.role = newRole
  } catch {
    // API not implemented yet
  }
}

onMounted(() => loadUsers(1))
</script>

<style scoped>
.user-panel {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 24px;
}

/* Toolbar */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  flex: 1;
  min-width: 200px;
  color: var(--text-muted);
}

.search-box input {
  flex: 1;
  font-size: 0.85rem;
}

.search-box input::placeholder {
  color: var(--text-muted);
}

.filter-group {
  display: flex;
  gap: 8px;
}

.filter-group select {
  padding: 8px 14px;
  font-size: 0.8rem;
  background: var(--bg-primary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
}

/* Table */
.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.data-table th {
  text-align: left;
  padding: 12px 14px;
  font-weight: 600;
  color: var(--text-muted);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--glass-border);
  white-space: nowrap;
}

.data-table td {
  padding: 12px 14px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--glass-border);
  white-space: nowrap;
}

.data-table tr:hover td {
  background: var(--glass-hover);
}

.td-id {
  font-family: var(--font-mono);
  font-size: 0.8rem;
  color: var(--text-muted);
}

.td-username {
  font-weight: 600;
  color: var(--text-primary);
}

.td-time {
  font-size: 0.8rem;
}

/* Badges */
.role-badge,
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: var(--radius-pill);
}

.role-admin {
  color: #7c3aed;
  background: rgba(139, 92, 246, 0.1);
}

.role-user {
  color: var(--text-muted);
  background: var(--glass);
}

.status-active {
  color: #059669;
  background: rgba(16, 185, 129, 0.1);
}

.status-disabled {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.1);
}

/* Actions */
.td-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  padding: 5px 12px;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-disable {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.08);
}

.btn-disable:hover {
  background: rgba(239, 68, 68, 0.15);
}

.btn-enable {
  color: #059669;
  background: rgba(16, 185, 129, 0.08);
}

.btn-enable:hover {
  background: rgba(16, 185, 129, 0.15);
}

.btn-role {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}

.btn-role:hover {
  background: rgba(59, 130, 246, 0.15);
}

/* Empty / Loading */
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.loading-state {
  padding: 40px 0;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--glass-border);
}

.page-btn {
  padding: 8px 20px;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-primary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--glass-border-hover);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.8rem;
  color: var(--text-muted);
}
</style>
