<template>
  <div class="review-page">
    <h2>✍️ 发布评价</h2>

    <div class="review-card">
      <!-- 订单信息 -->
      <div class="order-ref">
        <span class="ref-label">评价订单</span>
        <el-tag round effect="plain"># {{ orderNo }}</el-tag>
      </div>

      <!-- 评分 -->
      <div class="rating-section">
        <label class="section-label">⭐ 整体评分</label>
        <div class="rating-stars">
          <el-rate
            v-model="form.rating"
            :max="5"
            size="large"
            show-score
            :texts="['很差', '较差', '一般', '满意', '超赞']"
            show-text
          />
        </div>
        <div class="rating-emoji">
          {{ ratingEmoji }}
        </div>
      </div>

      <!-- 评价内容 -->
      <div class="content-section">
        <label class="section-label">📝 评价内容</label>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          placeholder="分享您的用餐体验... 菜品口味如何？分量够吗？有什么建议？"
          maxlength="500"
          show-word-limit
        />
      </div>

      <!-- 快捷标签 -->
      <div class="quick-tags">
        <span
          v-for="tag in quickTags"
          :key="tag"
          class="quick-tag"
          @click="appendTag(tag)"
        >
          {{ tag }}
        </span>
      </div>

      <!-- 提交 -->
      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        @click="submit"
        class="submit-btn"
        round
      >
        📤 提交评价
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/modules/order'
import { submitReview } from '@/api/modules/review'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderNo = ref('')
const submitting = ref(false)
const form = reactive({ rating: 5, content: '' })

const quickTags = ['味道很棒', '分量足', '性价比高', '包装精致', '配送快', '还会回购']

const ratingEmoji = computed(() => {
  const map = { 1: '😞', 2: '😐', 3: '🙂', 4: '😊', 5: '🤩' }
  return map[form.rating] || '🙂'
})

onMounted(async () => {
  try {
    const res = await getOrderDetail(route.params.orderId)
    orderNo.value = res.data.orderNo
  } catch {}
})

const appendTag = (tag) => {
  form.content = form.content ? form.content + '，' + tag : tag
}

const submit = async () => {
  submitting.value = true
  try {
    await submitReview({
      orderId: route.params.orderId,
      rating: form.rating,
      content: form.content
    })
    ElMessage.success('评价提交成功！AI 正在分析中...')
    router.push('/orders')
  } catch {} finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-page { max-width: 600px; margin: 0 auto; }
.review-page h2 { font-size: 22px; margin: 0 0 20px; }

.review-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-card);
  border-top: 4px solid var(--color-primary);
}

/* 订单引用 */
.order-ref {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0ebe3;
}
.ref-label { font-size: 14px; color: var(--color-text-secondary); }

/* 评分 */
.rating-section { margin-bottom: 24px; text-align: center; }
.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 10px;
}
.rating-stars { margin-bottom: 8px; }
.rating-emoji { font-size: 40px; }

/* 评价内容 */
.content-section { margin-bottom: 16px; }

/* 快捷标签 */
.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}
.quick-tag {
  padding: 6px 14px;
  border: 1px solid #f0ebe3;
  border-radius: 18px;
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: #fafafa;
}
.quick-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.submit-btn { width: 100%; height: 46px; font-size: 16px; letter-spacing: 2px; }
</style>
