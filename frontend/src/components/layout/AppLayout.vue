<template>
  <div class="app-shell">
    <!-- ===== 顶部导航 ===== -->
    <header class="app-header">
      <div class="header-inner">
        <!-- Logo -->
        <div class="header-logo" @click="$router.push('/home')">
          <span class="logo-icon">🍽️</span>
          <span class="logo-text">美味餐厅</span>
        </div>

        <!-- 导航链接 -->
        <nav class="header-nav">
          <router-link to="/home" class="nav-item" active-class="nav-active">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </router-link>
          <router-link to="/cart" class="nav-item" active-class="nav-active">
            <el-icon><ShoppingCart /></el-icon>
            <span>点餐车</span>
            <span v-if="cartCount > 0" class="nav-badge">{{ cartCount }}</span>
          </router-link>
          <router-link to="/orders" class="nav-item" active-class="nav-active">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </router-link>
          <router-link to="/ai" class="nav-item" active-class="nav-active">
            <el-icon><MagicStick /></el-icon>
            <span>AI 助手</span>
          </router-link>
        </nav>

        <!-- 用户区域 -->
        <div class="header-user">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-trigger">
                <el-avatar :size="34" :src="userStore.avatar" />
                <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                    <el-icon><Setting /></el-icon> 管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button class="btn-login" round @click="$router.push('/login')">登录</el-button>
            <el-button class="btn-register" round plain @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- ===== 主体内容 ===== -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- ===== AI 客服悬浮按钮 ===== -->
    <div class="ai-fab" @click="showService = true" title="AI 智能客服">
      <div class="fab-icon"><el-icon :size="22"><ChatDotRound /></el-icon></div>
      <span class="ai-fab-label">AI 客服</span>
    </div>

    <!-- ===== AI 客服弹窗 ===== -->
    <el-dialog
      v-model="showService"
      title="🤖 AI 智能客服"
      width="420px"
      :close-on-click-modal="false"
      destroy-on-close
      class="service-dialog"
    >
      <div class="chat-window" ref="chatWindowRef">
        <div v-if="chatMessages.length === 0" class="chat-empty">
          <span class="chat-empty-icon">💬</span>
          <p>有什么可以帮您的？</p>
          <div class="chat-hints">
            <span v-for="h in hints" :key="h" class="hint-chip" @click="sendHint(h)">{{ h }}</span>
          </div>
        </div>
        <div v-for="(msg, i) in chatMessages" :key="i"
             :class="['chat-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
          <el-avatar v-if="msg.role === 'ai'" :size="30" class="bubble-avatar">🤖</el-avatar>
          <div class="bubble-content">
            <span v-if="msg.role === 'ai'" class="bubble-ai-label">AI 客服</span>
            {{ msg.content }}
          </div>
          <el-avatar v-if="msg.role === 'user'" :size="30" class="bubble-avatar" :src="userStore.avatar" />
        </div>
      </div>
      <div class="chat-input-row">
        <el-input
          v-model="chatInput"
          placeholder="输入问题..."
          @keyup.enter="sendChat"
          :disabled="chatLoading"
          class="chat-input"
        />
        <el-button type="primary" :loading="chatLoading" circle @click="sendChat">
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, useCartStore } from '@/store'
import { getCartList } from '@/api/modules/cart'
import { getUserInfo } from '@/api/modules/user'
import { aiCustomerService } from '@/api/modules/ai'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const cartCount = ref(0)
const showService = ref(false)
const chatInput = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)
const chatWindowRef = ref(null)

const hints = ['有什么好吃的？', '推荐几个辣菜', '有什么优惠活动？', '营业时间到几点？', '怎么配送？', '帮我推荐两人套餐']

const loadCartCount = async () => {
  if (!userStore.isLoggedIn) {
    cartCount.value = 0
    return
  }
  try {
    const res = await getCartList()
    cartCount.value = res.data.length
    cartStore.setCount(res.data.length)
  } catch { /* ignore */ }
}

const syncUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.data) {
      userStore.updateProfile({
        nickname: res.data.nickname,
        phone: res.data.phone || '',
        avatar: res.data.avatar || ''
      })
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  loadCartCount()
  syncUserInfo()
})
watch(() => route.path, loadCartCount)

const handleUserCommand = (cmd) => {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatWindowRef.value) {
    chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight
  }
}

const sendChat = async () => {
  const q = chatInput.value.trim()
  if (!q) return
  chatMessages.value.push({ role: 'user', content: q })
  chatInput.value = ''
  chatLoading.value = true
  await scrollToBottom()
  try {
    const res = await aiCustomerService({ question: q })
    chatMessages.value.push({ role: 'ai', content: res.data })
  } catch {
    chatMessages.value.push({ role: 'ai', content: '抱歉，服务暂不可用，请稍后重试。' })
  } finally {
    chatLoading.value = false
    await scrollToBottom()
  }
}

const sendHint = (text) => {
  chatInput.value = text
  sendChat()
}
</script>

<style scoped>
/* ===== 整体布局 ===== */
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* ===== Header ===== */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
  height: 62px;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
}

/* Logo */
.header-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}
.logo-icon { font-size: 26px; }
.logo-text {
  font-size: 19px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 1px;
}

/* Nav */
.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  justify-content: center;
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
}
.nav-item:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}
.nav-item.nav-active {
  color: var(--color-primary);
  font-weight: 600;
}
.nav-item.nav-active::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: 2px;
  background: var(--color-primary);
}
.nav-badge {
  position: absolute;
  top: 2px;
  right: 6px;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  line-height: 17px;
  text-align: center;
  color: #fff;
  background: var(--color-primary);
  border-radius: 9px;
}

/* User */
.header-user { flex-shrink: 0; }
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}
.user-trigger:hover { background: #f5f5f5; }
.user-name { font-size: 13px; color: var(--color-text); max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.arrow { font-size: 12px; color: var(--color-text-muted); transition: transform var(--transition-fast); }
.user-trigger:hover .arrow { transform: rotate(180deg); }

.btn-login {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-text-color: #fff;
}
.btn-register {
  --el-button-text-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
}

/* ===== Main ===== */
.app-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}

@media (max-width: 768px) {
  .header-nav .nav-item span { display: none; }
  .header-inner { gap: 12px; padding: 0 12px; }
  .app-main { padding: 16px; }
}

/* ===== AI 悬浮按钮 ===== */
.ai-fab {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 54px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  z-index: 999;
}
.ai-fab .fab-icon {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(231, 76, 60, 0.35);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
  animation: fab-pulse 2.5s ease-in-out infinite;
}
.ai-fab:hover .fab-icon {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(231, 76, 60, 0.5);
}
.ai-fab-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary);
  background: #fff;
  padding: 2px 8px;
  border-radius: 8px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
@keyframes fab-pulse {
  0%, 100% { box-shadow: 0 4px 20px rgba(231, 76, 60, 0.35); }
  50% { box-shadow: 0 4px 32px rgba(231, 76, 60, 0.55); }
}

/* ===== 客服弹窗 ===== */
.chat-window {
  height: 320px;
  overflow-y: auto;
  padding: 12px 8px;
  background: #f8f9fa;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
}
.chat-empty-icon { font-size: 42px; margin-bottom: 8px; }
.chat-empty p { font-size: 14px; margin: 0 0 12px; }
.chat-hints { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.hint-chip {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--color-text-secondary);
}
.hint-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.chat-bubble {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  max-width: 85%;
}
.bubble-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.bubble-avatar { flex-shrink: 0; }
.bubble-content {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}
.bubble-user .bubble-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble-ai .bubble-content {
  background: #fff;
  color: var(--color-text);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}
.bubble-ai-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.chat-input-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  align-items: center;
}
.chat-input { flex: 1; }
</style>
