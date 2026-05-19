<template>
  <div class="crawler-panel">
    <!-- Header -->
    <div class="panel-header">
      <h3 class="panel-title">爬虫任务日志</h3>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>任务类型</th>
            <th>状态</th>
            <th>总数</th>
            <th>成功</th>
            <th>失败</th>
            <th>开始时间</th>
            <th>结束时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td class="td-id">{{ log.id }}</td>
            <td>{{ log.taskType }}</td>
            <td>
              <span class="status-badge" :class="statusClass(log.status)">{{ log.status }}</span>
            </td>
            <td>{{ log.totalCount }}</td>
            <td class="td-success">{{ log.successCount }}</td>
            <td class="td-fail">{{ log.failCount }}</td>
            <td class="td-time">{{ formatDateTime(log.startedAt) }}</td>
            <td class="td-time">{{ log.finishedAt ? formatDateTime(log.finishedAt) : '--' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Error Message -->
    <div v-for="log in failedLogs" :key="`err-${log.id}`" class="error-banner">
      <span class="error-label">任务 #{{ log.id }} 错误:</span>
      {{ log.errorMessage }}
    </div>

    <!-- Empty -->
    <div v-if="!loading && logs.length === 0" class="empty-state">
      <p>暂无爬虫日志</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载爬虫日志..." />
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="loadPage(currentPage - 1)">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="loadPage(currentPage + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAdminCrawlLogs, type CrawlLogItem } from '../../api/movies'
import { formatDateTime } from '../../utils/date'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const logs = ref<CrawlLogItem[]>([])
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)

const failedLogs = computed(() => logs.value.filter(l => l.status === 'FAILED' && l.errorMessage))

function statusClass(status: string) {
  if (status === 'SUCCESS') return 'status-success'
  if (status === 'FAILED') return 'status-fail'
  return 'status-running'
}

async function loadPage(page: number) {
  loading.value = true
  try {
    const res = await getAdminCrawlLogs(page, 20)
    logs.value = res.records
    totalPages.value = res.pages
    currentPage.value = res.current
  } catch {
    // API not implemented yet
  } finally {
    loading.value = false
  }
}

onMounted(() => loadPage(1))
</script>

<style scoped>
.crawler-panel {
  background: var(--glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 24px;
}

.panel-header {
  margin-bottom: 20px;
}

.panel-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

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
  padding: 10px 14px;
  font-weight: 600;
  color: var(--text-muted);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--glass-border);
  white-space: nowrap;
}

.data-table td {
  padding: 10px 14px;
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

.td-success {
  color: #059669;
  font-weight: 600;
}

.td-fail {
  color: #dc2626;
  font-weight: 600;
}

.td-time {
  font-size: 0.8rem;
}

.status-badge {
  display: inline-block;
  padding: 3px 10px;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: var(--radius-pill);
}

.status-success {
  color: #059669;
  background: rgba(16, 185, 129, 0.1);
}

.status-fail {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.1);
}

.status-running {
  color: #d97706;
  background: rgba(245, 158, 11, 0.1);
}

.error-banner {
  margin-top: 12px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  color: #dc2626;
}

.error-label {
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted);
}

.loading-state {
  padding: 40px 0;
}

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
