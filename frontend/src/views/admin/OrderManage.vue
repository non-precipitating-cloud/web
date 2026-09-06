<template>
  <div class="order-manage">
    <!-- 页面头 -->
    <div class="page-header">
      <h3>📋 订单管理</h3>
      <p>查看和处理客户订单</p>
    </div>

    <!-- Tab 筛选 -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value; loadData()"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="username" label="用户" width="90" />
        <el-table-column prop="totalPrice" label="金额" width="80">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="dark" round>
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="菜品明细" min-width="200">
          <template #default="{ row }">
            <span v-for="item in row.items" :key="item.itemId" class="dish-tag">
              {{ item.dishName }}×{{ item.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              size="small" type="primary" round
              @click="updateStatus(row, 1)"
            >
              接单
            </el-button>
            <el-button
              v-if="row.status === 1"
              size="small" type="warning" round
              @click="updateStatus(row, 2)"
            >
              出餐
            </el-button>
            <el-button
              v-if="row.status === 2"
              size="small" type="success" round
              @click="updateStatus(row, 3)"
            >
              完成
            </el-button>
            <el-button
              v-if="row.status >= 1 && row.status <= 3"
              size="small" type="danger" round
              @click="handleRefund(row)"
            >
              退款
            </el-button>
            <template v-else-if="row.status === 6">
              <el-button size="small" type="success" round @click="handleApproveRefund(row)">
                同意
              </el-button>
              <el-button size="small" type="info" plain round @click="handleRejectRefund(row)">
                拒绝
              </el-button>
            </template>
            <span v-else class="no-action">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="pageNum"
          @change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { adminGetOrderList, updateOrderStatus, adminRefundOrder, approveRefund, rejectRefund } from '@/api/modules/order'
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminGetOrderList({
      pageNum: pageNum.value, pageSize: pageSize.value,
      status: activeTab.value || undefined
    })
    orders.value = res.data.records
    total.value = res.data.total
  } catch {} finally { loading.value = false }
}
loadData()

const updateStatus = async (row, status) => {
  try {
    await updateOrderStatus(row.orderId, status)
    ElMessage.success('订单状态已更新')
    loadData()
  } catch {}
}

const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`确认对订单 #${row.orderNo} 进行退款？`, '退款确认', { type: 'warning' })
    await adminRefundOrder(row.orderId)
    ElMessage.success('退款成功')
    loadData()
  } catch {}
}

const handleApproveRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`同意订单 #${row.orderNo} 的退款申请？`, '审核确认', { type: 'warning' })
    await approveRefund(row.orderId)
    ElMessage.success('已同意退款')
    loadData()
  } catch {}
}

const handleRejectRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`拒绝订单 #${row.orderNo} 的退款申请？订单将恢复原状态。`, '审核确认', { type: 'warning' })
    await rejectRefund(row.orderId)
    ElMessage.success('已拒绝退款')
    loadData()
  } catch {}
}

const statusType = (s) => ({ 0: 'warning', 1: 'info', 2: '', 3: 'success', 4: 'danger', 5: 'danger', 6: 'warning' }[s] || 'info')
</script>

<style scoped>
/* 页面头 */
.page-header { margin-bottom: 18px; }
.page-header h3 { margin: 0 0 4px; font-size: 20px; }
.page-header p { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

/* Tab */
.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: var(--radius-md);
  padding: 5px;
  box-shadow: var(--shadow-sm);
  width: fit-content;
}
.tab-btn {
  padding: 8px 18px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
}
.tab-btn:hover { color: var(--color-primary); }
.tab-btn.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-card);
}
.dish-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px 4px 2px 0;
  background: #f5f0eb;
  border-radius: 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.no-action { color: var(--color-text-muted); font-size: 13px; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
