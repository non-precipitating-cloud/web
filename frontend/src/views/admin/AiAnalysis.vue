<template>
  <div class="ai-analysis">
    <div class="page-header">
      <h3>📊 AI 经营数据分析</h3>
      <p>基于近期订单与评价数据，AI 自动生成经营洞察与建议</p>
    </div>

    <!-- 生成按钮 -->
    <div class="generate-area" v-if="!report && !loading">
      <div class="generate-icon">🤖</div>
      <p>点击下方按钮，AI 将分析您的经营数据并给出专业建议</p>
      <el-button type="primary" size="large" @click="runAnalysis" round>
        🚀 生成经营分析报告
      </el-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-area">
      <div class="loading-card">
        <div class="pulse-ring"></div>
        <span class="pulse-icon">🤖</span>
        <p>AI 正在分析中，请稍候...</p>
      </div>
    </div>

    <!-- 报告 -->
    <div v-if="report" class="report-card">
      <div class="report-header">
        <span>📋 经营分析报告</span>
        <el-button size="small" plain round @click="report='';runAnalysis()">
          🔄 重新生成
        </el-button>
      </div>
      <div class="report-body" v-html="formattedReport"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { aiBusinessAnalysis } from '@/api/modules/ai'
import { adminGetDishList } from '@/api/modules/dish'
import { adminGetOrderList } from '@/api/modules/order'

const loading = ref(false)
const report = ref('')

const runAnalysis = async () => {
  loading.value = true
  report.value = ''
  try {
    const [dishRes, orderRes] = await Promise.all([
      adminGetDishList({ pageNum: 1, pageSize: 1000 }),
      adminGetOrderList({ pageNum: 1, pageSize: 1000 })
    ])

    const dishes = dishRes.data.records
    const orders = orderRes.data.records
    const hotDishes = [...dishes].sort((a, b) => b.sales - a.sales).slice(0, 5)
    const coldDishes = [...dishes].sort((a, b) => a.sales - b.sales).slice(0, 5)

    const stats = {
      菜品总数: dishes.length,
      订单总数: orders.length,
      热销TOP5: hotDishes.map(d => `${d.name}(销量${d.sales})`),
      滞销菜品: coldDishes.map(d => `${d.name}(销量${d.sales})`),
      平均评分: (dishes.reduce((s, d) => s + (d.score || 5), 0) / dishes.length).toFixed(1)
    }

    const res = await aiBusinessAnalysis({ stats })
    report.value = res.data
  } catch {} finally {
    loading.value = false
  }
}

const formattedReport = computed(() => {
  if (!report.value) return ''
  return report.value
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/(\d+\.\s)/g, '<br>$1')
})
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h3 { margin: 0 0 4px; font-size: 20px; }
.page-header p { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

/* 未开始状态 */
.generate-area {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
.generate-icon { font-size: 64px; margin-bottom: 12px; }
.generate-area p {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin: 0 0 20px;
}

/* Loading */
.loading-area {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}
.loading-card {
  text-align: center;
  position: relative;
}
.pulse-ring {
  width: 80px; height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  border: 3px solid var(--color-primary);
  animation: pulse-ring 1.5s ease-out infinite;
}
@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}
.pulse-icon {
  position: absolute;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 36px;
}
.loading-card p {
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 报告 */
.report-card {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fefaf7;
  border-bottom: 1px solid #f5f0eb;
  font-weight: 600;
  font-size: 15px;
}
.report-body {
  padding: 28px;
  line-height: 2;
  font-size: 15px;
  color: var(--color-text);
}
.report-body :deep(strong) {
  color: var(--color-primary);
  font-size: 16px;
}
</style>
