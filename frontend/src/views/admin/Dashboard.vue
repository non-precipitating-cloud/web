<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="stat in stats" :key="stat.label" :class="['stat-card', stat.theme]">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <el-row :gutter="20" style="margin-top:20px">
      <!-- 热门菜品 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <h4>🔥 热门菜品 TOP5</h4>
          </div>
          <div class="panel-body">
            <div v-for="(d, i) in topDishes" :key="d.id" class="top-item">
              <span :class="['rank', 'rank-' + (i + 1)]">{{ i + 1 }}</span>
              <span class="top-name">{{ d.name }}</span>
              <span class="top-sales">销量 {{ d.sales }}</span>
            </div>
            <el-empty v-if="topDishes.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </el-col>

      <!-- 情感分布 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <h4>📊 评价情感分布</h4>
          </div>
          <div class="panel-body" v-if="sentimentStats">
            <div class="sentiment-item">
              <div class="si-header">
                <span>😊 好评</span>
                <span class="si-pct">{{ sentimentStats.positive }}%</span>
              </div>
              <el-progress
                :percentage="sentimentStats.positive"
                color="#27ae60"
                :stroke-width="10"
                :show-text="false"
              />
            </div>
            <div class="sentiment-item">
              <div class="si-header">
                <span>😐 中评</span>
                <span class="si-pct">{{ sentimentStats.neutral }}%</span>
              </div>
              <el-progress
                :percentage="sentimentStats.neutral"
                color="#f39c12"
                :stroke-width="10"
                :show-text="false"
              />
            </div>
            <div class="sentiment-item">
              <div class="si-header">
                <span>😞 差评</span>
                <span class="si-pct">{{ sentimentStats.negative }}%</span>
              </div>
              <el-progress
                :percentage="sentimentStats.negative"
                color="#e74c3c"
                :stroke-width="10"
                :show-text="false"
              />
            </div>
          </div>
          <el-empty v-else description="暂无评价数据" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminGetDishList } from '@/api/modules/dish'
import { adminGetOrderList } from '@/api/modules/order'
import { adminGetReviewList } from '@/api/modules/review'

const stats = ref([
  { label: '菜品总数', value: 0, icon: '🍜', theme: 'theme-orange' },
  { label: '订单总数', value: 0, icon: '📋', theme: 'theme-blue' },
  { label: '评价总数', value: 0, icon: '💬', theme: 'theme-green' },
  { label: '好评率', value: '0%', icon: '👍', theme: 'theme-purple' }
])
const topDishes = ref([])
const sentimentStats = ref(null)

onMounted(async () => {
  try {
    const [dishRes, orderRes, reviewRes] = await Promise.all([
      adminGetDishList({ pageNum: 1, pageSize: 1000 }),
      adminGetOrderList({ pageNum: 1, pageSize: 1000 }),
      adminGetReviewList({ pageNum: 1, pageSize: 1000 })
    ])

    stats.value[0].value = dishRes.data.total
    stats.value[1].value = orderRes.data.total
    stats.value[2].value = reviewRes.data.total

    const dishes = dishRes.data.records
    dishes.sort((a, b) => b.sales - a.sales)
    topDishes.value = dishes.slice(0, 5)

    const reviews = reviewRes.data.records
    if (reviews.length > 0) {
      const pos = reviews.filter(r => r.sentiment === 'positive').length
      const neu = reviews.filter(r => r.sentiment === 'neutral').length
      const neg = reviews.filter(r => r.sentiment === 'negative').length
      const total = reviews.length
      sentimentStats.value = {
        positive: Math.round(pos / total * 100),
        neutral: Math.round(neu / total * 100),
        negative: Math.round(neg / total * 100)
      }
      stats.value[3].value = Math.round(pos / total * 100) + '%'
    }
  } catch {}
})
</script>

<style scoped>
/* 统计卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
  border-top: 3px solid transparent;
}
.stat-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-hover); }
.stat-card.theme-orange { border-top-color: #e67e22; }
.stat-card.theme-blue   { border-top-color: #3498db; }
.stat-card.theme-green  { border-top-color: #27ae60; }
.stat-card.theme-purple { border-top-color: #8e44ad; }

.stat-icon { font-size: 36px; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 800; color: var(--color-text); }
.stat-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

/* 面板 */
.panel {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0ebe3;
}
.panel-header h4 { margin: 0; font-size: 15px; }
.panel-body { padding: 12px 20px 20px; }

/* TOP5 */
.top-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f8f4f0;
}
.top-item:last-child { border-bottom: none; }
.rank {
  width: 26px; height: 26px; line-height: 26px; text-align: center;
  border-radius: 50%; font-size: 12px; font-weight: 700; color: #fff;
  margin-right: 12px; flex-shrink: 0;
  background: #b0b0b0;
}
.rank-1 { background: linear-gradient(135deg, #f39c12, #e67e22); }
.rank-2 { background: linear-gradient(135deg, #bdc3c7, #95a5a6); }
.rank-3 { background: linear-gradient(135deg, #e67e22, #d35400); }
.top-name { flex: 1; font-size: 14px; color: var(--color-text); }
.top-sales { font-size: 13px; color: var(--color-text-secondary); }

/* 情感 */
.sentiment-item { margin-bottom: 18px; }
.sentiment-item:last-child { margin-bottom: 0; }
.si-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}
.si-pct { font-weight: 600; color: var(--color-text); }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
