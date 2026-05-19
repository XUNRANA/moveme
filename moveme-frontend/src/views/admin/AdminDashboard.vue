<template>
  <div class="admin-dashboard">
    <!-- Header -->
    <div class="admin-header">
      <div class="header-left">
        <h1 class="header-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28">
            <rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4" />
          </svg>
          管理控制台
        </h1>
        <p class="header-subtitle">系统运行状态总览</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <AdminStatsCards />

    <!-- Tab Navigation -->
    <div class="tab-nav">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <component :is="tab.icon" />
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab Content -->
    <div class="tab-content">
      <Transition name="tab" mode="out-in">
        <AdminUserTable v-if="activeTab === 'users'" />
        <AdminCrawlerPanel v-else-if="activeTab === 'crawler'" />
        <AdminImportLogs v-else-if="activeTab === 'import'" />
        <AdminRecoLogs v-else-if="activeTab === 'reco'" />
        <AdminSystemOverview v-else-if="activeTab === 'system'" />
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import AdminStatsCards from '../../components/admin/AdminStatsCards.vue'
import AdminUserTable from '../../components/admin/AdminUserTable.vue'
import AdminCrawlerPanel from '../../components/admin/AdminCrawlerPanel.vue'
import AdminImportLogs from '../../components/admin/AdminImportLogs.vue'
import AdminRecoLogs from '../../components/admin/AdminRecoLogs.vue'
import AdminSystemOverview from '../../components/admin/AdminSystemOverview.vue'

const activeTab = ref('users')

const UsersIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 16, height: 16 }, [
  h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: 9, cy: 7, r: 4 }),
  h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75' })
])

const SpiderIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 16, height: 16 }, [
  h('circle', { cx: 12, cy: 12, r: 3 }),
  h('path', { d: 'M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83' })
])

const ImportIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 16, height: 16 }, [
  h('path', { d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' }),
  h('polyline', { points: '7 10 12 15 17 10' }),
  h('line', { x1: 12, y1: 15, x2: 12, y2: 3 })
])

const ChartIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 16, height: 16 }, [
  h('path', { d: 'M12 20V10M18 20V4M6 20v-4' })
])

const SettingsIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 16, height: 16 }, [
  h('path', { d: 'M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z' }),
  h('circle', { cx: 12, cy: 12, r: 3 })
])

const tabs = [
  { key: 'users', label: '用户管理', icon: UsersIcon },
  { key: 'crawler', label: '爬虫管理', icon: SpiderIcon },
  { key: 'import', label: '数据导入', icon: ImportIcon },
  { key: 'reco', label: '推荐日志', icon: ChartIcon },
  { key: 'system', label: '系统概览', icon: SettingsIcon },
]
</script>

<style scoped>
.admin-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

/* Header */
.admin-header {
  padding: 48px 0 32px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-primary);
}

.header-subtitle {
  margin-top: 6px;
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-left: 40px;
}

/* Tab Nav */
.tab-nav {
  display: flex;
  gap: 4px;
  margin-top: 32px;
  padding: 4px;
  background: var(--glass);
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  overflow-x: auto;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
}

.tab-btn:hover {
  color: var(--text-secondary);
  background: var(--glass-hover);
}

.tab-btn.active {
  color: var(--text-primary);
  background: var(--bg-elevated);
  box-shadow: var(--shadow-card);
  font-weight: 600;
}

/* Tab Content */
.tab-content {
  margin-top: 24px;
}

/* Tab transition */
.tab-enter-active,
.tab-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.tab-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.tab-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .tab-nav {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .tab-btn {
    padding: 10px 14px;
    font-size: 0.8rem;
  }
}
</style>
