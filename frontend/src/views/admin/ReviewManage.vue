<template>
  <div class="review-manage">
    <div class="page-header">
      <h3>💬 评价管理</h3>
      <p>管理用户评价，维护社区氛围</p>
    </div>

    <div class="table-card">
      <el-table :data="reviews" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="菜品" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">#{{ row.dishId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="100">
          <template #default="{ row }">
            {{ row.nickname || '用户' + row.userId }}
          </template>
        </el-table-column>
        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">
            <span :class="['rating-stars', ratingClass(row.rating)]">
              ⭐ {{ row.rating }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="review-content">
              <span class="review-quote">"</span>
              {{ row.content || '(无文字评价)' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="情感关键词" width="160">
          <template #default="{ row }">
            <span
              v-if="row.sentimentKeywords"
              v-for="(kw, i) in parseKeywords(row.sentimentKeywords)"
              :key="i"
              class="kw-tag"
            >{{ kw }}</span>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
              effect="dark"
              round
            >
              {{ row.status === 1 ? '正常' : '屏蔽' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              plain
              round
              @click="toggle(row)"
            >
              {{ row.status === 1 ? '屏蔽' : '恢复' }}
            </el-button>
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
import { adminGetReviewList, toggleReviewStatus } from '@/api/modules/review'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const reviews = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminGetReviewList({ pageNum: pageNum.value, pageSize: pageSize.value })
    reviews.value = res.data.records
    total.value = res.data.total
  } catch {} finally { loading.value = false }
}
loadData()

const toggle = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await toggleReviewStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已恢复显示' : '已屏蔽')
    loadData()
  } catch {}
}

const ratingClass = (r) => {
  if (r >= 4) return 'high'
  if (r >= 3) return 'mid'
  return 'low'
}

const parseKeywords = (val) => {
  if (!val) return []
  try { return JSON.parse(val) } catch { return [val] }
}
</script>

<style scoped>
.page-header { margin-bottom: 18px; }
.page-header h3 { margin: 0 0 4px; font-size: 20px; }
.page-header p { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

.table-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-card);
}

/* 评分颜色 */
.rating-stars { font-weight: 600; font-size: 13px; }
.rating-stars.high { color: #27ae60; }
.rating-stars.mid { color: #f39c12; }
.rating-stars.low { color: #e74c3c; }

/* 评价内容 */
.review-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.review-quote {
  font-size: 18px;
  color: var(--color-text-muted);
  font-family: Georgia, serif;
  margin-right: 2px;
}

/* 关键词 */
.kw-tag {
  display: inline-block;
  padding: 2px 8px;
  margin: 2px 4px 2px 0;
  background: #fef0e8;
  color: var(--color-primary);
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}
.no-data { color: var(--color-text-muted); }

.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
