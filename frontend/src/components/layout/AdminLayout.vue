<template>
  <el-container class="admin-layout">
    <!-- 侧栏 -->
    <el-aside width="220px" class="admin-aside">
      <div class="aside-brand" @click="$router.push('/admin/dashboard')">
        <span class="brand-icon">🍽️</span>
        <div>
          <h3>餐厅后台</h3>
          <p>管理中心</p>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
        class="aside-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/dishes">
          <el-icon><Food /></el-icon>
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/reviews">
          <el-icon><ChatLineRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>AI 经营分析</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧 -->
    <el-container class="admin-right">
      <el-header class="admin-header">
        <div class="header-left">
          <span class="page-title">{{ $route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-avatar :size="30" :src="userStore.avatar" />
          <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
          <el-button size="small" plain round @click="$router.push('/home')">前台</el-button>
          <el-button size="small" type="danger" plain round @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { height: 100vh; }

/* ===== 侧栏 ===== */
.admin-aside {
  background: linear-gradient(180deg, #1a1d2e 0%, #222640 100%);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.aside-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.brand-icon { font-size: 28px; }
.aside-brand h3 {
  margin: 0;
  font-size: 16px;
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
}
.aside-brand p {
  margin: 0;
  font-size: 11px;
  color: rgba(255,255,255,0.4);
}

.aside-menu {
  flex: 1;
  border-right: none !important;
  padding: 8px 0;
}
.aside-menu .el-menu-item {
  margin: 2px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  transition: all var(--transition-fast);
}
.aside-menu .el-menu-item:hover {
  background: rgba(255,255,255,0.06);
  color: #fff;
}
.aside-menu .el-menu-item.is-active {
  background: rgba(231, 76, 60, 0.18);
  color: #fff;
  font-weight: 600;
  border-left: 3px solid var(--color-primary);
}

/* ===== 右侧 ===== */
.admin-right { background: var(--bg-page); }
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
  padding: 0 24px;
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.page-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-main { padding: 20px; min-height: 0; }
</style>
