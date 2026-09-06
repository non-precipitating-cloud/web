<template>
  <div class="order-page">
    <h2>📋 我的订单</h2>

    <!-- 状态 Tab -->
    <div class="order-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value; loadOrders()"
      >
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      <div v-for="order in orders" :key="order.orderId" :class="['order-card', statusBar(order.status)]">
        <!-- 状态条 -->
        <div class="card-status-bar"></div>
        <div class="card-body">
          <!-- 头部 -->
          <div class="card-header">
            <span class="order-no"># {{ order.orderNo }}</span>
            <span class="order-time">{{ order.createdAt }}</span>
            <el-tag :type="statusType(order.status)" size="small" effect="dark" round>
              {{ order.statusText }}
            </el-tag>
          </div>

          <!-- 菜品 -->
          <div class="order-items">
            <div v-for="item in order.items" :key="item.itemId" class="order-item">
              <span class="oi-name">{{ item.dishName }}</span>
              <span class="oi-qty">×{{ item.quantity }}</span>
              <span class="oi-price">¥{{ item.subtotal }}</span>
            </div>
          </div>

          <!-- 底部操作 -->
          <div class="card-footer">
            <span class="total">
              合计 <b>¥{{ order.totalPrice }}</b>
            </span>
            <div class="actions">
              <el-button
                v-if="order.status === 0"
                type="primary" size="small" round
                @click="$router.push('/checkout/' + order.orderId)"
              >
                去支付
              </el-button>
              <el-button
                v-if="order.status === 0"
                type="danger" size="small" plain round
                @click="cancelOrder(order.orderId)"
              >
                取消
              </el-button>
              <el-button
                v-if="order.status >= 1 && order.status <= 3"
                type="danger" size="small" plain round
                @click="applyRefund(order.orderId)"
              >
                申请退款
              </el-button>
              <el-button
                v-if="order.status === 3 && !order.reviewed"
                type="primary" size="small" round
                @click="$router.push(`/review/${order.orderId}`)"
              >
                ✍️ 去评价
              </el-button>
              <el-tag v-if="order.status === 3 && order.reviewed" type="success" round>
                ✓ 已评价
              </el-tag>
              <el-button
                v-if="order.status === 3 && order.reviewed"
                type="primary" size="small" plain round
                @click="viewReview(order.orderId)"
              >
                查看评价
              </el-button>
              <el-button
                v-if="order.status === 3 || order.status === 4 || order.status === 5"
                type="default" size="small" plain round
                @click="deleteOrderHandler(order.orderId)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="pageNum"
        @change="loadOrders"
      />
    </div>

    <!-- 查看评价弹窗 -->
    <el-dialog v-model="reviewDialogVisible" title="📝 我的评价" width="480px" destroy-on-close>
      <div v-loading="reviewLoading" class="review-dialog-body">
        <div v-for="rv in orderReviews" :key="rv.id" class="review-item">
          <div class="review-dish">{{ rv.dishName }}</div>
          <div class="review-stars">
            <el-rate :model-value="rv.rating" disabled show-score :score-template="rv.rating + ' 分'" />
          </div>
          <div class="review-content">{{ rv.content || '（无文字评价）' }}</div>
          <div class="review-sentiment" v-if="rv.sentiment">
            <el-tag v-if="rv.sentiment === 'positive'" type="success" size="small" round>❤️ 好评</el-tag>
            <el-tag v-else-if="rv.sentiment === 'neutral'" type="info" size="small" round>😐 中性</el-tag>
            <el-tag v-else-if="rv.sentiment === 'negative'" type="danger" size="small" round>💔 差评</el-tag>
          </div>
          <div class="review-time">{{ rv.createdAt }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserOrderList, cancelOrder as cancelOrderApi, deleteOrder, refundOrder } from '@/api/modules/order'
import { getOrderReviews } from '@/api/modules/review'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const orders = ref([])
const activeTab = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: '0' },
  { label: '已接单', value: '1' },
  { label: '出餐中', value: '2' },
  { label: '已完成', value: '3' },
  { label: '退款审核中', value: '6' },
  { label: '已退款', value: '5' },
  { label: '已取消', value: '4' }
]

const reviewDialogVisible = ref(false)
const reviewLoading = ref(false)
const orderReviews = ref([])

async function viewReview(orderId) {
  reviewDialogVisible.value = true
  reviewLoading.value = true
  try {
    const res = await getOrderReviews(orderId)
    orderReviews.value = res.data
  } catch {} finally {
    reviewLoading.value = false
  }
}

onMounted(loadOrders)

async function loadOrders() {
  loading.value = true
  try {
    const res = await getUserOrderList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeTab.value || undefined
    })
    orders.value = res.data.records
    total.value = res.data.total
  } catch {} finally {
    loading.value = false
  }
}

async function cancelOrder(orderId) {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
    await cancelOrderApi(orderId)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch {}
}

async function applyRefund(orderId) {
  try {
    await ElMessageBox.confirm('确定申请退款？退款申请将提交给管理员审核。', '提示', { type: 'warning' })
    await refundOrder(orderId)
    ElMessage.success('退款申请已提交，等待管理员审核')
    loadOrders()
  } catch {}
}

async function deleteOrderHandler(orderId) {
  try {
    await ElMessageBox.confirm('确定删除该订单？', '提示', { type: 'warning' })
    await deleteOrder(orderId)
    ElMessage.success('订单已删除')
    loadOrders()
  } catch {}
}

const statusBar = (s) => {
  const map = { 0: 'bar-pending', 1: 'bar-accepted', 2: 'bar-preparing', 3: 'bar-done', 4: 'bar-cancelled', 5: 'bar-refunded', 6: 'bar-refunding' }
  return map[s] || ''
}
const statusType = (s) => {
  const map = { 0: 'warning', 1: 'info', 2: '', 3: 'success', 4: 'danger', 5: 'danger', 6: 'warning' }
  return map[s] || 'info'
}
</script>

<style scoped>
.order-page { max-width: 860px; margin: 0 auto; }
.order-page h2 { font-size: 22px; margin: 0 0 20px; }

/* Tabs */
.order-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 6px;
  box-shadow: var(--shadow-sm);
}
.tab-btn {
  flex: 1;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
  white-space: nowrap;
}
.tab-btn:hover { color: var(--color-primary); }
.tab-btn.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(231,76,60,0.25);
}
.tab-count {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.8;
}

/* 订单卡片 */
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card {
  display: flex;
  background: #fff;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
}
.order-card:hover { box-shadow: var(--shadow-hover); }

.card-status-bar {
  width: 4px;
  flex-shrink: 0;
  background: #e0e0e0;
}
.bar-pending .card-status-bar  { background: #f39c12; }
.bar-accepted .card-status-bar  { background: #3498db; }
.bar-preparing .card-status-bar { background: #e67e22; }
.bar-done .card-status-bar      { background: #27ae60; }
.bar-cancelled .card-status-bar { background: #bbb; }
.bar-refunded .card-status-bar  { background: #e74c3c; }
.bar-refunding .card-status-bar  { background: #e67e22; }

.card-body { flex: 1; padding: 16px 20px; }
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f5f0eb;
}
.order-no { font-weight: 600; font-size: 14px; color: var(--color-text); }
.order-time { font-size: 12px; color: var(--color-text-muted); margin-left: auto; }

.order-items { margin-bottom: 12px; }
.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 5px 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.oi-name { flex: 1; }
.oi-qty { color: var(--color-text-muted); font-size: 13px; }
.oi-price { font-weight: 500; min-width: 60px; text-align: right; }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f0eb;
}
.total { font-size: 14px; color: var(--color-text-secondary); }
.total b { color: var(--color-primary); font-size: 18px; margin-left: 2px; }
.actions { display: flex; gap: 8px; align-items: center; }

.pagination { margin-top: 24px; display: flex; justify-content: center; }

/* 查看评价弹窗 */
.review-dialog-body { display: flex; flex-direction: column; gap: 16px; }
.review-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: var(--radius-md);
}
.review-dish { font-weight: 600; font-size: 14px; margin-bottom: 6px; }
.review-stars { margin-bottom: 6px; }
.review-content { font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; line-height: 1.6; }
.review-sentiment { margin-bottom: 4px; }
.review-time { font-size: 12px; color: var(--color-text-muted); }
</style>
