<template>
  <div class="dish-detail" v-loading="loading">
    <template v-if="!loading && dish.id">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <router-link to="/home">← 返回首页</router-link>
      </div>

      <!-- 主内容 -->
      <div class="detail-main">
        <!-- 左侧图片 -->
        <div class="detail-image">
          <div class="image-wrapper">
            <el-image :src="dish.image || ''" fit="cover" class="main-img">
              <template #error>
                <div class="img-fallback">🍽️</div>
              </template>
            </el-image>
            <span v-if="dish.stock === 0" class="badge sold-out">已售罄</span>
            <span v-if="dish.sales > 100" class="badge hot">🔥 热销 {{ dish.sales }}+</span>
          </div>
        </div>

        <!-- 右侧信息 -->
        <div class="detail-info">
          <h1 class="dish-title">{{ dish.name }}</h1>

          <div class="price-row">
            <span class="price">¥{{ dish.price }}</span>
            <span class="unit">/ 份</span>
          </div>

          <div class="stats-row">
            <span class="stat">⭐ {{ dish.score }} 评分</span>
            <span class="stat">📦 月销 {{ dish.sales }}</span>
            <span class="stat">📋 库存 {{ dish.stock }} 份</span>
          </div>

          <div class="ingredients-section" v-if="dish.ingredients">
            <h4>🥬 食材</h4>
            <p>{{ dish.ingredients }}</p>
          </div>

          <div class="desc-section" v-if="dish.description">
            <h4>📝 介绍</h4>
            <p>{{ dish.description }}</p>
          </div>

          <div class="add-cart-section">
            <div class="qty-control">
              <span class="qty-label">数量</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="dish.stock || 1"
                :disabled="dish.stock === 0"
                size="large"
                controls-position="right"
              />
            </div>
            <el-button
              type="primary"
              size="large"
              :disabled="dish.stock === 0 || dish.status === 0"
              @click="addToCartHandler"
              class="cart-btn"
            >
              <el-icon><ShoppingCart /></el-icon>
              {{ dish.stock === 0 ? '已售罄' : '加入点餐车' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 评价区域 -->
      <div class="review-section">
        <h3>📊 用户评价 ({{ reviews.length }})</h3>
        <div v-if="reviews.length === 0" class="review-empty">
          <span>💬</span>
          <p>暂无评价，快来第一个评价吧</p>
        </div>
        <div v-for="rv in reviews" :key="rv.id" :class="['review-card', sentimentClass(rv.rating)]">
          <div class="review-left">
            <el-avatar :size="40" class="review-avatar">
              {{ (rv.nickname || '用户')[0] }}
            </el-avatar>
          </div>
          <div class="review-right">
            <div class="review-header">
              <span class="review-user">{{ rv.nickname || '用户' + rv.userId }}</span>
              <span class="review-rating">⭐ {{ rv.rating }}</span>
              <span class="review-time">{{ formatDate(rv.createdAt) }}</span>
            </div>
            <p class="review-content">{{ rv.content || '(无文字评价)' }}</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDishDetail } from '@/api/modules/dish'
import { addToCart } from '@/api/modules/cart'
import { getDishReviews } from '@/api/modules/review'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const dish = ref({})
const quantity = ref(1)
const reviews = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const [dishRes, reviewRes] = await Promise.all([
      getDishDetail(route.params.id),
      getDishReviews(route.params.id, { pageNum: 1, pageSize: 50 })
    ])
    dish.value = dishRes.data
    reviews.value = reviewRes.data.records
  } catch {} finally {
    loading.value = false
  }
})

const addToCartHandler = async () => {
  try {
    await addToCart({ dishId: dish.value.id, quantity: quantity.value })
    ElMessage.success('已加入点餐车')
  } catch {}
}

const sentimentClass = (rating) => {
  if (rating >= 4) return 'positive'
  if (rating === 3) return 'neutral'
  return 'negative'
}

const formatDate = (d) => {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.dish-detail { max-width: 1000px; margin: 0 auto; }

/* 面包屑 */
.breadcrumb { margin-bottom: 20px; }
.breadcrumb a {
  color: var(--color-text-secondary);
  font-size: 14px;
  transition: color var(--transition-fast);
}
.breadcrumb a:hover { color: var(--color-primary); }

/* 主内容 */
.detail-main {
  display: flex;
  gap: 32px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-card);
}

/* 图片 */
.detail-image { flex: 0 0 42%; }
.image-wrapper {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
}
.main-img {
  width: 100%;
  height: 340px;
  display: block;
  transition: transform var(--transition-slow);
}
.main-img:hover { transform: scale(1.03); }
.img-fallback {
  width: 100%;
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f4f0;
  font-size: 72px;
  border-radius: var(--radius-md);
}
.badge {
  position: absolute;
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
.badge.sold-out {
  top: 12px; right: 12px;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(4px);
}
.badge.hot {
  top: 12px; left: 12px;
  background: var(--color-accent);
}

/* 信息 */
.detail-info { flex: 1; }
.dish-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--color-text);
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 16px;
}
.price {
  font-size: 32px;
  font-weight: 800;
  color: var(--color-primary);
}
.unit { font-size: 14px; color: var(--color-text-secondary); }

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0ebe3;
}
.stat { font-size: 13px; color: var(--color-text-secondary); }

.ingredients-section, .desc-section {
  margin-bottom: 16px;
}
.ingredients-section h4, .desc-section h4 {
  font-size: 14px;
  margin: 0 0 6px;
  color: var(--color-text);
}
.ingredients-section p, .desc-section p {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* 加入点餐车 */
.add-cart-section {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  margin-top: 24px;
  padding: 20px;
  background: #fef8f6;
  border-radius: var(--radius-md);
}
.qty-control {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.qty-label { font-size: 12px; color: var(--color-text-secondary); }
.cart-btn { flex: 1; height: 44px; font-size: 16px; }

/* 评价 */
.review-section {
  margin-top: 32px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-card);
}
.review-section h3 { margin: 0 0 16px; }
.review-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--color-text-secondary);
}
.review-empty span { font-size: 36px; display: block; margin-bottom: 8px; }

.review-card {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f0eb;
  border-left: 3px solid transparent;
  padding-left: 16px;
  transition: border-color var(--transition-fast);
}
.review-card.positive { border-left-color: #27ae60; }
.review-card.neutral  { border-left-color: #f39c12; }
.review-card.negative { border-left-color: #e74c3c; }
.review-avatar {
  flex-shrink: 0;
  background: var(--color-primary-light) !important;
  color: var(--color-primary) !important;
  font-weight: 600;
}
.review-right { flex: 1; }
.review-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 6px;
}
.review-user { font-size: 14px; font-weight: 500; color: var(--color-text); }
.review-rating { font-size: 13px; }
.review-time { font-size: 12px; color: var(--color-text-muted); margin-left: auto; }
.review-content { font-size: 14px; color: var(--color-text-secondary); line-height: 1.6; margin: 0; }

@media (max-width: 768px) {
  .detail-main { flex-direction: column; }
  .detail-image { flex: none; }
  .main-img, .img-fallback { height: 240px; }
}
</style>
