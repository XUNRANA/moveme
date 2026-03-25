<template>
  <div class="home">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <h1 class="logo">MovieMe</h1>
      </div>
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <span class="welcome">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <!-- 主内容 -->
    <el-main class="main">
      <div class="hero">
        <h2>发现你的下一部好电影</h2>
        <p>基于智能算法和 AI 大模型，为你推荐最适合的电影</p>
        <el-input v-model="searchKeyword" placeholder="搜索电影名称..." size="large" class="search-input"
          @keyup.enter="handleSearch">
          <template #append>
            <el-button :icon="'Search'" @click="handleSearch" />
          </template>
        </el-input>
      </div>

      <div class="section">
        <h3>系统状态</h3>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="电影数据" :value="0" suffix="部">
                <template #prefix>
                  <el-icon><Film /></el-icon>
                </template>
              </el-statistic>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="注册用户" :value="0" suffix="人">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-statistic>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="用户评分" :value="0" suffix="条">
                <template #prefix>
                  <el-icon><Star /></el-icon>
                </template>
              </el-statistic>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-empty description="电影数据将在爬虫模块完成后展示" />
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Film, User, Star } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const searchKeyword = ref('')

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // token 可能过期
    }
  }
})

function handleSearch() {
  // Phase 3 实现
}

function handleLogout() {
  userStore.logout()
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.logo {
  font-size: 24px;
  color: #409eff;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome {
  color: #606266;
  font-size: 14px;
}

.main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.hero {
  text-align: center;
  padding: 60px 0 40px;
}

.hero h2 {
  font-size: 32px;
  color: #303133;
  margin-bottom: 12px;
}

.hero p {
  color: #909399;
  font-size: 16px;
  margin-bottom: 32px;
}

.search-input {
  max-width: 600px;
}

.section {
  margin: 40px 0;
}

.section h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 20px;
}
</style>
