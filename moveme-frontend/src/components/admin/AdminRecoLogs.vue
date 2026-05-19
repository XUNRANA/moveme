<template>
  <div class="reco-panel">
    <div class="panel-header">
      <h3 class="panel-title">推荐系统日志</h3>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户ID</th>
            <th>策略类型</th>
            <th>LLM 供应商</th>
            <th>延迟(ms)</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td class="td-id">{{ log.id }}</td>
            <td>{{ log.userId }}</td>
            <td>
              <span class="strategy-badge">{{ log.strategyType }}</span>
            </td>
            <td>{{ log.llmProvider || '--' }}</td>
            <td>{{ log.latencyMs ?? '--' }}</td>
            <td class="td-time">{{ formatDateTime(log.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty -->
    <div v-if="!loading && logs.length === 0" class="empty-state">
      <p>暂无推荐日志</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <LoadingSpinner text="加载推荐日志..." />
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
import { ref, onMounted } from 'vue'
import { getAdminRecoLogs, type RecoLogItem } from '../../api/movies'
import { formatDateTime } from '../../utils/date'
import LoadingSpinner from '../common/LoadingSpinner.vue'

const logs = ref<RecoLogItem[]>([])
const totalPages = ref(0)
const currentPage = ref(1)
const loading = ref(false)

async function loadPage(page: number) {
  loading.value = true
  try {
    const res = await getAdminRecoLogs(page, 20)
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
.reco-panel {
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

.td-time {
  font-size: 0.8rem;
}

.strategy-badge {
  display: inline-block;
  padding: 3px 10px;
  font-size: 0.7rem;
  font-weight: 600;
  border-radius: var(--radius-pill);
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
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
