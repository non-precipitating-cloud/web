<template>
  <div class="ai-page">
    <!-- 顶部横幅 -->
    <div class="ai-banner">
      <span class="banner-icon">🤖</span>
      <div>
        <h2>AI 智能点餐助手</h2>
        <p>智能推荐、菜单查询、问题解答，一站式搞定</p>
      </div>
    </div>

    <!-- 快捷提问 -->
    <div class="quick-ask">
      <span class="quick-label">💬 快速提问：</span>
      <button
        v-for="q in quickQuestions"
        :key="q.label"
        class="quick-chip"
        @click="handleQuick(q)"
      >
        {{ q.label }}
      </button>
    </div>

    <!-- 对话区域 -->
    <div class="chat-area" ref="chatArea">
      <div v-if="chatHistory.length === 0 && !lastRecommend" class="chat-welcome">
        <span class="welcome-emoji">🍽️</span>
        <p>试试下面的快捷提问，或者使用底部面板智能推荐~</p>
      </div>

      <div v-for="(msg, i) in chatHistory" :key="i" :class="['chat-msg', msg.role]">
        <template v-if="msg.role === 'user'">
          <div class="msg-content user-content">{{ msg.content }}</div>
          <el-avatar :size="32" class="msg-avatar user-avatar">👤</el-avatar>
        </template>
        <template v-else>
          <el-avatar :size="32" class="msg-avatar ai-avatar">🤖</el-avatar>
          <div class="msg-content ai-content" v-html="formatMsg(msg.content)"></div>
        </template>
      </div>

      <!-- 推荐结果卡片 -->
      <div v-if="lastRecommend && lastRecommend.dishes && lastRecommend.dishes.length > 0" class="recommend-inline">
        <div class="rec-reason">💡 {{ lastRecommend.reason || '为您推荐以下菜品' }}</div>
        <div class="rec-dishes">
          <div v-for="dish in lastRecommend.dishes" :key="dish.dishId || dish.id || Math.random()" class="rec-dish-card">
            <!-- 图片：有图则显示，无图显示占位 -->
            <div class="rec-img-box">
              <img v-if="dish.image" :src="dish.image" class="rec-img" />
              <div v-else class="rec-img-fb">🍽️</div>
            </div>
            <div class="rec-info">
              <div class="rec-name">{{ dish.dishName || dish.name || dish.dish_name || '(菜品#' + (dish.dishId || dish.id) + ')' }}</div>
              <div class="rec-price">¥{{ dish.price }}</div>
              <el-tag v-if="dish.reason" size="small" type="warning" effect="dark" round>
                {{ dish.reason }}
              </el-tag>
            </div>
            <el-button type="primary" size="small" circle @click="quickAdd(dish)" title="加入点餐车">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="rec-total">
          合计 <b>¥{{ lastRecommend.totalPrice }}</b>
          <el-button size="small" type="primary" round @click="addAllRec">
            📥 全部加入点餐车
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部输入 -->
    <div class="chat-input-bar">
      <el-input
        v-model="chatInput"
        placeholder="输入您的问题，如：帮我推荐几道辣菜..."
        size="large"
        clearable
        @keyup.enter="sendMessage"
        class="input-field"
      >
        <template #prefix>
          <el-icon><ChatDotRound /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="large" :loading="loading" circle @click="sendMessage">
        <el-icon><Promotion /></el-icon>
      </el-button>
    </div>

    <!-- 底部推荐面板 -->
    <div class="config-row">
      <div class="config-field">
        <label class="config-label">😋 口味</label>
        <div class="taste-pills">
          <button
            v-for="t in tastes"
            :key="t.value"
            :class="['taste-chip', { active: form.taste === t.value }]"
            @click="form.taste = t.value"
          >
            {{ t.icon }} {{ t.label }}
          </button>
        </div>
      </div>
      <div class="config-field">
        <label class="config-label">💰 预算</label>
        <el-input v-model="form.budget" placeholder="如: 30" size="small" style="width:120px">
          <template #append>元</template>
        </el-input>
      </div>
      <div class="config-field">
        <label class="config-label">👥 人数</label>
        <el-input-number v-model="form.personCount" :min="1" :max="10" size="small" controls-position="right" />
      </div>
      <el-button type="primary" round @click="doRecommend">
        🚀 智能推荐
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { aiRecommend, aiCustomerService } from '@/api/modules/ai'
import { addToCart } from '@/api/modules/cart'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const chatInput = ref('')
const chatHistory = ref([])
const lastRecommend = ref(null)
const chatArea = ref(null)

const form = reactive({ budget: '', taste: '辣', personCount: 1 })

const tastes = [
  { label: '辣', value: '辣', icon: '🌶️' },
  { label: '清淡', value: '清淡', icon: '🥬' },
  { label: '酸甜', value: '酸甜', icon: '🍋' },
  { label: '不忌口', value: '不忌口', icon: '🎲' }
]

// 快捷提问：纯文本快捷入口，与底部口味/预算面板完全独立
const quickQuestions = [
  { label: '有什么好吃的？' },
  { label: '推荐几个辣菜' },
  { label: '帮我搭配两人餐' },
  { label: '人均 ¥20 吃什么' },
  { label: '有什么优惠？' },
  { label: '营业时间' }
]

const scrollDown = async () => {
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}

const addChat = (role, content) => {
  chatHistory.value.push({ role, content })
  scrollDown()
}

const formatMsg = (text) => {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

// 快捷提问：统一走文本输入 → sendMessage（与底部口味面板完全独立）
const handleQuick = (q) => {
  chatInput.value = q.label
  sendMessage()
}

// 自由输入 → 走客服 API
const sendMessage = async () => {
  const q = chatInput.value.trim()
  if (!q || loading.value) return

  addChat('user', q)
  chatInput.value = ''
  lastRecommend.value = null
  loading.value = true

  try {
    // 判断是否带有推荐意图
    if (isRecommendIntent(q)) {
      const taste = detectTaste(q)
      const budget = detectBudget(q)
      const persons = detectPersons(q)
      const res = await aiRecommend({
        taste: taste,
        budget: budget || undefined,
        personCount: persons || 1
      })
      console.log('[AI] sendMessage recommend res.data:', res.data)
      lastRecommend.value = res.data
      addChat('ai', (res.data && res.data.reason) || '为您找到以下推荐：')
    } else {
      const res = await aiCustomerService({ question: q })
      addChat('ai', res.data)
    }
  } catch {
    addChat('ai', '抱歉，服务暂不可用，请稍后重试 😥')
  } finally {
    loading.value = false
    scrollDown()
  }
}

// 判断是否是推荐意图
const isRecommendIntent = (q) => {
  const recWords = ['推荐', '搭配', '帮我', '吃什么', '点菜', '点餐', '选菜', '介绍几个', '来几个']
  return recWords.some(w => q.includes(w))
}

const detectTaste = (q) => {
  if (q.includes('辣')) return '辣'
  if (q.includes('清淡') || q.includes('素')) return '清淡'
  if (q.includes('酸甜') || q.includes('甜')) return '酸甜'
  return '不忌口'
}

const detectBudget = (q) => {
  const m = q.match(/(\d+)\s*(元|块|¥)/)
  return m ? m[1] : null
}

const detectPersons = (q) => {
  const m = q.match(/(\d+)\s*(人|位)/)
  return m ? parseInt(m[1]) : null
}

// 底部面板推荐
const doRecommend = async () => {
  const prompt = `帮我推荐：${form.taste}口味，预算${form.budget || '不限'}，${form.personCount}人`
  addChat('user', prompt)
  lastRecommend.value = null
  loading.value = true

  try {
    const res = await aiRecommend({
      taste: form.taste,
      budget: form.budget || undefined,
      personCount: form.personCount
    })
    console.log('[AI] doRecommend res.data:', res.data)
    lastRecommend.value = res.data
    addChat('ai', (res.data && res.data.reason) || '为您找到以下推荐：')
  } catch {
    addChat('ai', '推荐服务暂不可用 😥')
  } finally {
    loading.value = false
    scrollDown()
  }
}

const quickAdd = async (dish) => {
  const dishId = dish.dishId || dish.id
  const dishName = dish.dishName || dish.name || '菜品'
  try {
    await addToCart({ dishId, quantity: 1 })
    ElMessage.success(`已加入: ${dishName}`)
  } catch {}
}

const addAllRec = async () => {
  if (!lastRecommend.value?.dishes) return
  for (const d of lastRecommend.value.dishes) {
    const dishId = d.dishId || d.id
    try { await addToCart({ dishId, quantity: 1 }) } catch {}
  }
  ElMessage.success('已全部加入点餐车')
}
</script>

<style scoped>
.ai-page { max-width: 900px; margin: 0 auto; }

/* 横幅 */
.ai-banner {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8e0 100%);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}
.banner-icon { font-size: 44px; }
.ai-banner h2 { margin: 0 0 2px; font-size: 19px; }
.ai-banner p { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

/* 快捷提问 */
.quick-ask {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.quick-label { font-size: 13px; color: var(--color-text-secondary); white-space: nowrap; }
.quick-chip {
  padding: 6px 15px;
  border: 1px solid #f0ebe3;
  border-radius: 18px;
  background: #fff;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
  white-space: nowrap;
}
.quick-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fefaf7;
}

/* 对话区 */
.chat-area {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-card);
  min-height: 280px;
  max-height: 480px;
  overflow-y: auto;
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chat-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  padding: 40px 0;
}
.welcome-emoji { font-size: 48px; margin-bottom: 8px; }
.chat-welcome p { font-size: 14px; margin: 0; }

.chat-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  max-width: 90%;
}
.chat-msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.msg-avatar { flex-shrink: 0; }
.user-avatar { background: var(--color-primary-light) !important; color: var(--color-primary) !important; }
.ai-avatar { background: #fff5f0 !important; }
.msg-content {
  padding: 10px 15px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.6;
}
.user-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-content {
  background: #f8f9fa;
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}
.ai-content :deep(strong) { color: var(--color-primary); }

/* 推荐卡片 */
.recommend-inline {
  background: #fefaf7;
  border: 1px solid #fde8e0;
  border-radius: var(--radius-md);
  padding: 16px;
  margin-top: 4px;
}
.rec-reason { font-size: 13px; color: var(--color-text); margin-bottom: 12px; }
.rec-dishes { display: flex; flex-direction: column; gap: 8px; }
.rec-dish-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  box-shadow: var(--shadow-sm);
}
.rec-img-box {
  width: 56px; height: 56px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.rec-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}
.rec-img-fb {
  width: 100%; height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f4f0;
  font-size: 24px;
}
.rec-info { flex: 1; min-width: 0; }
.rec-name { font-size: 14px; font-weight: 600; margin-bottom: 2px; color: var(--color-text); }
.rec-price { font-size: 14px; color: var(--color-primary); font-weight: 700; margin-bottom: 4px; }
.rec-total {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #fde8e0;
  font-size: 14px;
}
.rec-total b { color: var(--color-primary); font-size: 20px; }

/* 底部输入 */
.chat-input-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}
.input-field { flex: 1; }

/* 配置行 */
.config-row {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}
.config-field { display: flex; flex-direction: column; gap: 5px; }
.config-label { font-size: 12px; font-weight: 600; color: var(--color-text-secondary); }
.taste-pills { display: flex; gap: 6px; }
.taste-chip {
  padding: 5px 14px;
  border: 1.5px solid #f0ebe3;
  border-radius: 18px;
  background: #fff;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
}
.taste-chip:hover { border-color: var(--color-primary); }
.taste-chip.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .config-row { flex-direction: column; align-items: stretch; }
  .quick-ask { gap: 6px; }
  .quick-chip { font-size: 11px; padding: 4px 10px; }
}
</style>
