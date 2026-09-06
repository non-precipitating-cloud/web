<template>
  <div class="cart-page">
    <div class="cart-header">
      <h2>🛒 我的点餐车</h2>
      <span v-if="cartList.length > 0" class="item-count">{{ cartList.length }} 种菜品</span>
    </div>

    <!-- 空状态 -->
    <div v-if="cartList.length === 0" class="empty-state">
      <span class="empty-icon">🛒</span>
      <h3>点餐车空空如也</h3>
      <p>快去挑选心仪的菜品吧~</p>
      <el-button type="primary" round size="large" @click="$router.push('/home')">
        🍽️ 去点餐
      </el-button>
    </div>

    <!-- 有内容 -->
    <template v-else>
      <div class="cart-items">
        <div
          v-for="item in cartList"
          :key="item.cartId"
          :class="['cart-item', { 'item-off': item.dishStatus === 0 }]"
        >
          <div class="item-image">
            <el-image :src="item.dishImage || ''" fit="cover">
              <template #error><span class="img-placeholder">🍽️</span></template>
            </el-image>
          </div>
          <div class="item-body">
            <div class="item-top">
              <h4 class="item-name">{{ item.dishName }}</h4>
              <span v-if="item.dishStatus === 0" class="off-tag">已下架</span>
            </div>
            <span class="item-price">¥{{ item.dishPrice }}</span>
          </div>
          <div class="item-qty">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.dishStock"
              size="small"
              @change="updateQty(item)"
              controls-position="right"
            />
          </div>
          <div class="item-subtotal">
            ¥{{ (item.dishPrice * item.quantity).toFixed(2) }}
          </div>
          <div class="item-delete">
            <el-button type="danger" plain circle size="small" @click="removeItem(item)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 底部合计 -->
      <div class="cart-footer">
        <div class="footer-info">
          <span class="total-label">合计金额</span>
          <span class="total-price">¥{{ totalPrice }}</span>
        </div>
        <el-button type="primary" size="large" @click="submitOrderHandler" class="submit-btn">
          📋 提交订单
        </el-button>
      </div>
    </template>

    <!-- 待支付订单 -->
    <div v-if="pendingOrders.length > 0" class="pending-section">
      <div class="pending-header" @click="pendingExpanded = !pendingExpanded">
        <span>📋 待支付订单 ({{ pendingOrders.length }})</span>
        <el-icon :class="{ 'arrow-up': pendingExpanded }"><ArrowDown /></el-icon>
      </div>
      <template v-if="pendingExpanded">
        <div v-for="order in pendingOrders" :key="order.orderId" class="pending-order">
          <div class="pending-order-header">
            <span class="order-no">#{{ order.orderNo }}</span>
            <span class="order-total">¥{{ order.totalPrice }}</span>
          </div>
          <div
            v-for="item in order.items"
            :key="item.itemId"
            class="pending-item"
          >
            <div class="pending-item-info">
              <span class="pending-item-name">{{ item.dishName }}</span>
              <span class="pending-item-price">¥{{ item.dishPrice }}</span>
            </div>
            <div class="pending-item-qty">
              <el-input-number
                v-model="item.quantity"
                :min="0"
                :max="99"
                size="small"
                controls-position="right"
                @change="updatePendingQty(order.orderId, item)"
              />
            </div>
            <div class="pending-item-subtotal">¥{{ (item.dishPrice * item.quantity).toFixed(2) }}</div>
          </div>
          <div class="pending-order-actions">
            <el-button size="small" type="primary" plain @click="$router.push('/checkout/' + order.orderId)">
              去支付
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeFromCart } from '@/api/modules/cart'
import { submitOrder, getPendingOrders, updateOrderItemQuantity } from '@/api/modules/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const cartList = ref([])
const pendingOrders = ref([])
const pendingExpanded = ref(false)

const totalPrice = computed(() =>
  cartList.value.reduce((sum, item) => sum + item.dishPrice * item.quantity, 0).toFixed(2)
)

onMounted(() => {
  loadCart()
  loadPendingOrders()
})

async function loadCart() {
  try {
    const res = await getCartList()
    cartList.value = res.data
  } catch {}
}

async function loadPendingOrders() {
  try {
    const res = await getPendingOrders()
    pendingOrders.value = res.data
    if (res.data.length > 0) pendingExpanded.value = true
  } catch {}
}

async function updateQty(row) {
  try { await updateCartQuantity(row.cartId, row.quantity) } catch { loadCart() }
}

async function removeItem(row) {
  try {
    await ElMessageBox.confirm('确定移除该菜品？', '提示', { type: 'warning' })
    await removeFromCart(row.cartId)
    ElMessage.success('已移除')
    loadCart()
  } catch {}
}

async function submitOrderHandler() {
  const hasOff = cartList.value.some(item => item.dishStatus === 0)
  if (hasOff) {
    ElMessage.warning('点餐车中有已下架菜品，请先移除')
    return
  }
  try {
    const res = await submitOrder({})
    ElMessage.success('下单成功！')
    loadCart()
    loadPendingOrders()
  } catch {}
}

async function updatePendingQty(orderId, item) {
  if (item.quantity === 0) {
    try {
      await ElMessageBox.confirm('数量设为 0 将移除该菜品，确认？', '提示', { type: 'warning' })
      await updateOrderItemQuantity(orderId, item.itemId, 0)
      ElMessage.success('已移除')
      loadPendingOrders()
    } catch {
      item.quantity = 1
    }
    return
  }
  try {
    await updateOrderItemQuantity(orderId, item.itemId, item.quantity)
    loadPendingOrders()
  } catch {
    loadPendingOrders()
  }
}
</script>

<style scoped>
.cart-page { max-width: 800px; margin: 0 auto; }

.cart-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.cart-header h2 { margin: 0; font-size: 22px; }
.item-count { font-size: 13px; color: var(--color-text-secondary); }

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
.empty-icon { font-size: 64px; display: block; margin-bottom: 12px; }
.empty-state h3 { margin: 0 0 6px; color: var(--color-text); }
.empty-state p { margin: 0 0 20px; color: var(--color-text-secondary); font-size: 14px; }

/* 菜品列表 */
.cart-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-card);
  transition: all var(--transition-fast);
}
.cart-item:hover { box-shadow: var(--shadow-hover); }
.cart-item.item-off { opacity: 0.6; background: #fef5f5; }

.item-image {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
}
.item-image .el-image {
  width: 100%;
  height: 100%;
}
.img-placeholder {
  width: 100%;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f4f0;
  font-size: 28px;
}

.item-body { flex: 1; min-width: 0; }
.item-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.item-name { margin: 0; font-size: 15px; font-weight: 600; color: var(--color-text); }
.off-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fde8e8;
  color: #e74c3c;
  font-weight: 500;
}
.item-price { font-size: 14px; color: var(--color-primary); font-weight: 600; }

.item-qty { flex-shrink: 0; }

.item-subtotal {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
  min-width: 72px;
  text-align: right;
  flex-shrink: 0;
}

.item-delete { flex-shrink: 0; }

/* 底部 */
.cart-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px 24px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: 0 -2px 16px rgba(0,0,0,0.06);
}
.total-label { font-size: 14px; color: var(--color-text-secondary); }
.total-price {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-primary);
  margin-left: 12px;
}
.submit-btn { height: 46px; padding: 0 32px; font-size: 16px; }

/* 待支付订单 */
.pending-section {
  margin-top: 24px;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.pending-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  background: #fafafc;
  user-select: none;
}
.pending-header .el-icon {
  transition: transform 0.2s;
}
.pending-header .arrow-up {
  transform: rotate(180deg);
}
.pending-order {
  border-top: 1px solid #f0f0f0;
  padding: 12px 18px;
}
.pending-order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.order-total {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 15px;
}
.pending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}
.pending-item-info {
  flex: 1;
  min-width: 0;
}
.pending-item-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
}
.pending-item-price {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.pending-item-qty { flex-shrink: 0; }
.pending-item-subtotal {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  min-width: 64px;
  text-align: right;
}
.pending-order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #eee;
}

@media (max-width: 600px) {
  .cart-item { flex-wrap: wrap; gap: 10px; }
  .item-subtotal { text-align: left; }
}
</style>
