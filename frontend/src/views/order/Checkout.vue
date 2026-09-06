<template>
  <div class="checkout-page">
    <h2>💳 确认支付</h2>

    <div v-if="order" class="checkout-card">
      <!-- 订单信息 -->
      <div class="order-summary">
        <div class="summary-row">
          <span class="sr-label">订单编号</span>
          <span class="sr-value">{{ order.orderNo }}</span>
        </div>
        <div class="summary-row">
          <span class="sr-label">下单时间</span>
          <span class="sr-value">{{ order.createdAt }}</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-row total-row">
          <span class="sr-label">应付金额</span>
          <span class="sr-price">¥{{ order.totalPrice }}</span>
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="pay-section">
        <h4>选择支付方式</h4>
        <div class="pay-options">
          <div
            v-for="method in payMethods"
            :key="method.value"
            :class="['pay-card', { selected: payMethod === method.value }]"
            @click="payMethod = method.value"
          >
            <span class="pay-icon">{{ method.icon }}</span>
            <div class="pay-info">
              <span class="pay-name">{{ method.label }}</span>
              <span class="pay-desc">{{ method.desc }}</span>
            </div>
            <span v-if="payMethod === method.value" class="pay-check">✓</span>
          </div>
        </div>
      </div>

      <!-- 操作 -->
      <div class="pay-actions">
        <el-button size="large" round @click="$router.back()">返回修改</el-button>
        <el-button
          type="primary"
          size="large"
          round
          :loading="paying"
          :disabled="!payMethod"
          @click="handlePay"
          class="pay-btn"
        >
          💰 确认支付 ¥{{ order.totalPrice }}
        </el-button>
      </div>
    </div>

    <div v-else-if="!loading" class="not-found">
      <el-empty description="订单不存在" />
      <el-button type="primary" @click="$router.push('/orders')">查看我的订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder } from '@/api/modules/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(true)
const paying = ref(false)
const payMethod = ref('微信支付')

const payMethods = [
  { label: '微信支付', value: '微信支付', icon: '💚', desc: '支持微信扫码支付' },
  { label: '支付宝', value: '支付宝', icon: '💙', desc: '支持支付宝扫码支付' },
  { label: '银行支付', value: '银行支付', icon: '🏦', desc: '支持主流银行储蓄卡/信用卡' }
]

onMounted(async () => {
  try {
    const res = await getOrderDetail(route.params.orderId)
    order.value = res.data
    if (order.value.status !== 0) {
      ElMessage.warning('该订单已支付或已取消')
      router.replace('/orders')
    }
  } catch {} finally {
    loading.value = false
  }
})

const handlePay = async () => {
  paying.value = true
  try {
    await payOrder(order.value.orderId, { method: payMethod.value })
    ElMessage.success('支付成功！商家正在备餐...')
    router.replace('/orders')
  } catch {} finally {
    paying.value = false
  }
}
</script>

<style scoped>
.checkout-page { max-width: 680px; margin: 0 auto; }
.checkout-page h2 { font-size: 22px; margin: 0 0 20px; }

.checkout-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-card);
}

/* 订单信息 */
.order-summary {
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px dashed #f0ebe3;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
}
.sr-label { color: var(--color-text-secondary); }
.sr-value { color: var(--color-text); font-weight: 500; }
.summary-divider { margin: 8px 0; border-top: 1px dotted #f0ebe3; }
.total-row { padding-top: 14px; }
.sr-price {
  font-size: 30px;
  font-weight: 800;
  color: var(--color-primary);
}

/* 支付选择 */
.pay-section { margin-bottom: 24px; }
.pay-section h4 {
  font-size: 15px;
  margin: 0 0 14px;
  color: var(--color-text);
}
.pay-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pay-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 2px solid #f0ebe3;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}
.pay-card:hover {
  border-color: var(--color-primary);
  background: #fefaf7;
}
.pay-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
.pay-icon { font-size: 28px; flex-shrink: 0; }
.pay-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.pay-name { font-size: 15px; font-weight: 600; color: var(--color-text); }
.pay-desc { font-size: 12px; color: var(--color-text-secondary); }
.pay-check {
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  background: var(--color-primary);
  color: #fff;
  border-radius: 50%;
  font-size: 13px;
  font-weight: bold;
}

/* 操作 */
.pay-actions {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #f0ebe3;
}
.pay-btn {
  min-width: 220px;
  font-size: 16px;
  letter-spacing: 1px;
}

.not-found { text-align: center; margin-top: 40px; }
</style>
